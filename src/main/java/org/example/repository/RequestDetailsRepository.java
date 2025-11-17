package org.example.repository;

import org.example.model.RequestDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestDetailsRepository extends JpaRepository<RequestDetailsEntity, String> {
}

