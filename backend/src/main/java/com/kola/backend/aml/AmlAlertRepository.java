package com.kola.backend.aml;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AmlAlertRepository extends JpaRepository<AmlAlert, Long> {

    @Query("""
            SELECT a FROM AmlAlert a
            WHERE (:status IS NULL OR a.status = :status)
              AND (:riskLevel IS NULL OR a.riskLevel = :riskLevel)
            ORDER BY a.createdAt DESC
            """)
    Page<AmlAlert> search(@Param("status") AmlAlertStatus status,
                          @Param("riskLevel") AmlRiskLevel riskLevel,
                          Pageable pageable);

    long countByStatus(AmlAlertStatus status);

    long countByRiskLevel(AmlRiskLevel riskLevel);
}
