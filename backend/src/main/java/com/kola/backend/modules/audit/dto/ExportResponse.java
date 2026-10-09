package com.kola.backend.modules.audit.dto;

/** @param url signed download link, or null while generation is not implemented */
public record ExportResponse(String url) {
}
