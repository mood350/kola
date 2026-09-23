package com.kola.backend.modules.admin.dto;

/**
 * One dashboard tile (BACKEND.md 5). Values are formatted strings: the front-end displays them
 * as they arrive and has no formatting logic of its own.
 *
 * @param up whether {@code delta} is an improvement — drives the arrow and its colour
 */
public record MetricResponse(String label, String value, String delta, boolean up) {
}
