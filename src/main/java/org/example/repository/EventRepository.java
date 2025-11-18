package org.example.repository;

import org.example.entities.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<EventEntity, String> {
    List<EventEntity> findByInsuredId(String insuredId);
}
