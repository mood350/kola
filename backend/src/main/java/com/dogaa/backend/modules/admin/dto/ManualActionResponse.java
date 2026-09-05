package com.dogaa.backend.modules.admin.dto;

/**
 * One manual intervention by an agent (BACKEND.md 13).
 *
 * @param by   the agent's name
 * @param time pre-formatted age, e.g. "Il y a 2 h"
 */
public record ManualActionResponse(String id, String action, String by, String time) {
}
