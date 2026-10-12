package com.agri.cropemergency.repository;

import com.agri.cropemergency.model.FarmField;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FarmFieldRepository extends JpaRepository<FarmField, Long> {
    List<FarmField> findByOwnerIdOrderByIdDesc(Long ownerId);
    long countByOwnerId(Long ownerId);
}
