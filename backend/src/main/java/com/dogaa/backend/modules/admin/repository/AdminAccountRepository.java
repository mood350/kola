package com.dogaa.backend.modules.admin.repository;

import com.dogaa.backend.modules.admin.entity.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminAccountRepository extends JpaRepository<AdminAccount, UUID> {

    Optional<AdminAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<AdminAccount> findAllByOrderByNameAsc();
}
