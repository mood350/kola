package com.dogaa.backend.modules.scoring.service;

import java.math.BigDecimal;

/**
 * Raw behavioural measurements over the observation window, already filtered for materiality.
 *
 * <p>Deliberately a plain record of numbers: gathering them touches five modules, but turning them
 * into a score is arithmetic, and keeping the two apart is what lets the rules be tested without a
 * database. Every ratio the calculator needs is derivable from these fields.
 *
 * @param totalInflow            money received over the window
 * @param totalOutflow           money sent over the window
 * @param netSavingsIn           savings deposits minus savings withdrawals
 * @param savingsDeposits        gross savings deposits, used for the persistence ratio
 * @param savingsWithdrawals     gross savings withdrawals
 * @param scheduledExecuted      programmed transfers that ran successfully
 * @param scheduledFailed        programmed transfers that failed
 * @param averageBalance         mean daily total balance
 * @param balanceStdDev          standard deviation of the daily total balance
 * @param averageDailySpend      mean daily outgoing amount
 * @param insufficientFundsFails operations refused for lack of funds: attempted overdrafts
 * @param distinctInflowDays     days with at least one material inflow
 * @param inflowIntervalCv       coefficient of variation of the gaps between inflows
 * @param inflowTrend            this window's inflow against the previous ones
 * @param distinctOutgoingTypes  how many of merchant / P2P / bill were used
 * @param distinctCounterparties distinct people or merchants dealt with
 * @param loansRepaidOnTime      loans paid back in full by the due date
 * @param loansPaidLate          loans paid back after the grace period
 * @param loansDefaulted         loans that had to be recovered from the collateral
 */
public record ScoringInputs(
        BigDecimal totalInflow,
        BigDecimal totalOutflow,
        BigDecimal netSavingsIn,
        BigDecimal savingsDeposits,
        BigDecimal savingsWithdrawals,
        int scheduledExecuted,
        int scheduledFailed,
        BigDecimal averageBalance,
        BigDecimal balanceStdDev,
        BigDecimal averageDailySpend,
        int insufficientFundsFails,
        int distinctInflowDays,
        double inflowIntervalCv,
        Trend inflowTrend,
        int distinctOutgoingTypes,
        int distinctCounterparties,
        int loansRepaidOnTime,
        int loansPaidLate,
        int loansDefaulted) {

    public enum Trend {
        GROWING,
        STABLE,
        DECLINING
    }

    /** A brand-new account with no activity at all: everything at zero, trend neutral. */
    public static ScoringInputs empty() {
        return new ScoringInputs(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0,
                0, 1.0, Trend.STABLE,
                0, 0,
                0, 0, 0);
    }
}
