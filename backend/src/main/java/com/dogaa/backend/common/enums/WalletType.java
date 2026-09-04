package com.dogaa.backend.common.enums;

/**
 * The two accounts every user gets at sign-up.
 *
 * <p>{@link #CURRENT} carries day-to-day money: transfers, merchant payments, bills, cash-in and
 * cash-out. {@link #SAVINGS} is the collateral account: it is what a loan is secured against, and
 * while a loan is outstanding its whole balance sits in {@code lockedBalance} — deposits still land
 * there, withdrawals are refused because the available balance is zero. The freeze needs no special
 * rule, it is the wallet's own available/locked split doing its job.
 */
public enum WalletType {
    CURRENT,
    SAVINGS
}
