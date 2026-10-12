package com.agri.cropemergency.repository;

import com.agri.cropemergency.model.Alert;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Alert> findByUserIdAndSeenFalse(Long userId);
    long countByUserIdAndSeenFalse(Long userId);
    void deleteByFieldId(Long fieldId);
}
