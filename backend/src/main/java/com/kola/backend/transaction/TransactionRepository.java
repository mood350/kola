package com.kola.backend.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Historique complet d'un wallet (toutes transactions)
    List<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    Page<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    // Historique des envois d'un utilisateur
    List<Transaction> findBySenderIdOrderByCreatedAtDesc(Long senderId);

    // Historique des réceptions d'un utilisateur
    List<Transaction> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    // Récupérer une transaction par sa référence unique
    Optional<Transaction> findByReference(String reference);

    // Toutes les transactions en attente (pour un job de vérification)
    List<Transaction> findByStatus(TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t")
    BigDecimal sumTotalAmount();

    @Query("SELECT COALESCE(SUM(t.fee), 0) FROM Transaction t")
    BigDecimal sumTotalFees();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.createdAt >= :start AND t.createdAt < :end")
    BigDecimal sumAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT t.status, COUNT(t), COALESCE(SUM(t.amount), 0) FROM Transaction t GROUP BY t.status ORDER BY COUNT(t) DESC")
    List<Object[]> summarizeByStatus();

    @Query("SELECT t.type, COUNT(t), COALESCE(SUM(t.amount), 0) FROM Transaction t GROUP BY t.type ORDER BY COUNT(t) DESC")
    List<Object[]> summarizeByType();

    @Query("SELECT t.currency, COUNT(t), COALESCE(SUM(t.amount), 0) FROM Transaction t GROUP BY t.currency ORDER BY SUM(t.amount) DESC")
    List<Object[]> summarizeByCurrency();

    @Query("SELECT COALESCE(t.receiverCountryCode, COALESCE(t.sender.countryCode, 'UN')), COUNT(t), COALESCE(SUM(t.amount), 0) FROM Transaction t GROUP BY COALESCE(t.receiverCountryCode, COALESCE(t.sender.countryCode, 'UN')) ORDER BY SUM(t.amount) DESC")
    List<Object[]> summarizeByCountry();

    @Query("SELECT YEAR(t.createdAt), MONTH(t.createdAt), COUNT(t), COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.createdAt >= :start GROUP BY YEAR(t.createdAt), MONTH(t.createdAt) ORDER BY YEAR(t.createdAt), MONTH(t.createdAt)")
    List<Object[]> summarizeMonthlySince(@Param("start") LocalDateTime start);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.sender.id = :senderId " +
            "AND t.status = com.kola.backend.transaction.TransactionStatus.SUCCESS " +
            "AND t.type IN (" +
            "  com.kola.backend.transaction.TransactionType.TRANSFER_OUT, " +
            "  com.kola.backend.transaction.TransactionType.WITHDRAWAL" +
            ") " +
            "AND t.createdAt >= :startOfDay")
    BigDecimal sumSpentTodayBySender(@Param("senderId") Long senderId, @Param("startOfDay") LocalDateTime startOfDay);
}
