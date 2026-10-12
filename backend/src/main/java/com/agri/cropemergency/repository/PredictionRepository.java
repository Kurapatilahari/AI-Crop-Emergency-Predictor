package com.agri.cropemergency.repository;

import com.agri.cropemergency.model.Outcome;
import com.agri.cropemergency.model.Prediction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    List<Prediction> findByFieldIdOrderByCreatedAtDesc(Long fieldId, Pageable pageable);
    Optional<Prediction> findFirstByFieldIdOrderByCreatedAtDesc(Long fieldId);
    List<Prediction> findByOutcomeNot(Outcome outcome);
    List<Prediction> findByFieldOwnerIdAndOutcomeNot(Long ownerId, Outcome outcome);
    void deleteByFieldId(Long fieldId);
}
