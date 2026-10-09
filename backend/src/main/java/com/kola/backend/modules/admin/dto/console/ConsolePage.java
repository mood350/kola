package com.kola.backend.modules.admin.dto.console;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * One page of a console list. Our own shape rather than a serialised Spring {@code Page}, whose
 * JSON layout is an implementation detail Spring itself warns against exposing.
 *
 * @param page zero-based
 */
public record ConsolePage<T>(List<T> items, int page, int size, long total, int totalPages) {

    public static <T> ConsolePage<T> of(Page<?> source, List<T> items) {
        return new ConsolePage<>(items, source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages());
    }
}
