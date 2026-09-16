package com.kola.backend.modules.audit.repository;

import com.kola.backend.modules.audit.entity.AuditLogEntry;
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
     *
     * <p>Each filter neutralises itself through {@code coalesce} rather than a {@code :param is null}
     * test: PostgreSQL cannot infer the type of a standalone null bind parameter and rejects the
     * statement ({@code lower(bytea) does not exist}). Inside {@code coalesce} it takes the type from
     * the column it sits next to.
     */
    @Query("""
            select e from AuditLogEntry e
            where lower(e.actorName) like lower(concat('%', coalesce(:actorName, e.actorName), '%'))
              and e.module = coalesce(:module, e.module)
              and e.createdAt >= coalesce(:from, e.createdAt)
              and e.createdAt <= coalesce(:to, e.createdAt)
            order by e.createdAt desc
            """)
    List<AuditLogEntry> search(@Param("actorName") String actorName,
                               @Param("module") String module,
                               @Param("from") Instant from,
                               @Param("to") Instant to);
}
