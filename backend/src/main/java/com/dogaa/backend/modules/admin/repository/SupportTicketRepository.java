package com.dogaa.backend.modules.admin.repository;

import com.dogaa.backend.modules.admin.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    Optional<SupportTicket> findByReference(String reference);

    /** Oldest first: a support queue is worked in the order people asked. */
    List<SupportTicket> findAllByOrderByCreatedAtAsc();
}
