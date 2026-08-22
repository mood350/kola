package com.kola.backend.aml;

import com.kola.backend.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AmlAlertService {

    private final AmlAlertRepository alertRepository;

    @Transactional(readOnly = true)
    public Page<AmlAlertResponse> search(AmlAlertStatus status, AmlRiskLevel riskLevel, Pageable pageable) {
        return alertRepository.search(status, riskLevel, pageable).map(AmlAlertResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public AmlAlertResponse getById(Long id) {
        return AmlAlertResponse.fromEntity(
                alertRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Alerte introuvable")));
    }

    /** Tableau de bord conformité : volumétrie par statut et par niveau. */
    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (AmlAlertStatus s : AmlAlertStatus.values()) {
            byStatus.put(s.name(), alertRepository.countByStatus(s));
        }

        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (AmlRiskLevel l : AmlRiskLevel.values()) {
            byLevel.put(l.name(), alertRepository.countByRiskLevel(l));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", alertRepository.count());
        result.put("byStatus", byStatus);
        result.put("byRiskLevel", byLevel);
        return result;
    }

    @Transactional
    public AmlAlertResponse review(Long id, User analyst, AmlAlertStatus newStatus, String notes) {
        AmlAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alerte introuvable"));

        alert.setStatus(newStatus);
        alert.setReviewedBy(analyst.getEmail());
        if (notes != null && !notes.isBlank()) {
            alert.setReviewNotes(notes);
        }
        return AmlAlertResponse.fromEntity(alertRepository.save(alert));
    }
}
