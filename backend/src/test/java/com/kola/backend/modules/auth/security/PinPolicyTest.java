package com.kola.backend.modules.auth.security;

import com.kola.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PinPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"1937", "80412", "570913"})
    void acceptsANonTrivialPin(String pin) {
        assertThatCode(() -> PinPolicy.validate(pin)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000", "1111", "999999"})
    void rejectsARepeatedDigit(String pin) {
        assertThatThrownBy(() -> PinPolicy.validate(pin)).isInstanceOf(BadRequestException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234", "4321", "456789"})
    void rejectsAConsecutiveSequence(String pin) {
        assertThatThrownBy(() -> PinPolicy.validate(pin)).isInstanceOf(BadRequestException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "1234567", "12a4"})
    void rejectsAnythingThatIsNotFourToSixDigits(String pin) {
        assertThatThrownBy(() -> PinPolicy.validate(pin)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> PinPolicy.validate(null)).isInstanceOf(BadRequestException.class);
    }
}
