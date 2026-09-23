package com.kola.backend.modules.admin.dto;

/**
 * One bar of the volume chart (BACKEND.md 5).
 *
 * @param h    bar height as a percentage of the tallest bar, 0-100
 * @param last marks the current day, which the UI highlights
 */
public record ChartPointResponse(String day, int h, boolean last) {
}
