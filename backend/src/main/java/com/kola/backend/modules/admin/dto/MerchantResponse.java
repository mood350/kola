package com.kola.backend.modules.admin.dto;

/**
 * A partner merchant (BACKEND.md 10).
 *
 * @param status {@code active}, {@code suspended} or {@code pending}
 */
public record MerchantResponse(String id, String name, String category, String status) {
}
