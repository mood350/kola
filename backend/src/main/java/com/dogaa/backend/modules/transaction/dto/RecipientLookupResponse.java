package com.dogaa.backend.modules.transaction.dto;

/**
 * Who a phone number belongs to, shown on the confirmation screen before money moves.
 *
 * <p>The whole point is that a mistyped digit becomes visible <em>before</em> the transfer, not
 * after. A P2P transfer is irreversible without a dispute, so the last chance to catch a wrong
 * number is this screen.
 *
 * @param phone         the number normalised to E.164 — send this back on the transfer, not what
 *                      the user typed, so both calls address the same account
 * @param phoneMasked   safe to display and to log
 * @param registered    false means the number has no Dogaa account; the transfer still works but
 *                      leaves through Mobile Money, and there is no name to confirm
 * @param name          the account holder, null when {@code registered} is false
 * @param self          true when the caller looked up their own number — a transfer to oneself is
 *                      refused, and saying so here beats a failure after confirmation
 */
public record RecipientLookupResponse(String phone,
                                      String phoneMasked,
                                      boolean registered,
                                      String name,
                                      boolean self) {
}
