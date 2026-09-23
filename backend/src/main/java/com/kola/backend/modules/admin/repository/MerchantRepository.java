package com.kola.backend.modules.admin.repository;

import com.kola.backend.modules.admin.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    List<Merchant> findAllByOrderByNameAsc();
}
