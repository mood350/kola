package com.kola.backend.modules.admin.dto.console;

import java.util.List;

/**
 * Everything the mobile app shows the customer, seen from the back-office: accounts, vaults,
 * loans, scheduled payments and identity documents. Nothing the app does not have — a support
 * agent answering a call should see what the caller sees. The history is paginated separately,
 * on {@code /console/transactions?userId=}, so opening a profile never loads it whole.
 */
public record ConsoleUserDetail(ConsoleUserProfile profile,
                                List<ConsoleWallet> wallets,
                                List<ConsoleVault> vaults,
                                List<ConsoleLoan> loans,
                                List<ConsoleScheduledTask> scheduled,
                                List<ConsoleKycDocument> documents) {
}
