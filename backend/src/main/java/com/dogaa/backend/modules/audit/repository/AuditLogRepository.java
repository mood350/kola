package com.dogaa.backend.modules.audit.repository;

import com.dogaa.backend.modules.audit.entity.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

    /**
     * Newest first, with every filter optional. The admin UI announces filtering by admin, action
     * and period, so the query supports all three even though the current screen sends none.
     */
    @Query("""
            select e from AuditLogEntry e
            where (:actorName is null or lower(e.actorName) like lower(concat('%', :actorName, '%')))
              and (:module is null or e.module = :module)
              and (:from is null or e.createdAt >= :from)
              and (:to is null or e.createdAt <= :to)
            order by e.createdAt desc
            """)
    List<AuditLogEntry> search(@Param("actorName") String actorName,
                               @Param("module") String module,
                               @Param("from") Instant from,
                               @Param("to") Instant to);
}
