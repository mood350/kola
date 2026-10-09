package com.kola.backend.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumbersTest {

    @ParameterizedTest
    @ValueSource(strings = {"90123456", "090123456", "90 12 34 56", "+228 90123456", "0022890123456"})
    void normalizesEveryCommonFormatToTheSameE164Number(String input) {
        assertThat(PhoneNumbers.normalize(input, "228")).isEqualTo("+22890123456");
    }

    @Test
    void keepsForeignNumbersOnTheirOwnCallingCode() {
        assertThat(PhoneNumbers.normalize("+221771234567", "228")).isEqualTo("+221771234567");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "12", "abcdefgh", "+2281234567890123456"})
    void rejectsWhatIsNotAPhoneNumber(String input) {
        assertThatThrownBy(() -> PhoneNumbers.normalize(input, "228"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void masksAllButTheLastTwoDigits() {
        assertThat(PhoneNumbers.mask("+22890123456")).isEqualTo("+228******56");
    }
}
