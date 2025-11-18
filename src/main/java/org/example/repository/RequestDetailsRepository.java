package org.example.repository;

import org.example.entities.RequestDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestDetailsRepository extends JpaRepository<RequestDetailsEntity, String> {
}

