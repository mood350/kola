package com.dogaa.backend.modules.admin.dto;

/** Dashboard alert, also feeding the header bell (BACKEND.md 5). */
public record AlertResponse(String title, String detail, String severity) {
}
