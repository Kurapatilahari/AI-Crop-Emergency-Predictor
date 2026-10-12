package com.agri.cropemergency.repository;

import com.agri.cropemergency.model.SensorReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {
    List<SensorReading> findByFieldIdOrderByRecordedAtDesc(Long fieldId, Pageable pageable);
    long countByFieldId(Long fieldId);
    void deleteByFieldId(Long fieldId);
    void deleteByFieldIdAndSource(Long fieldId, String source);
}
