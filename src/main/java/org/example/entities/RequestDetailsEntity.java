package org.example.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "request_details")
@Data
public class RequestDetailsEntity {
    @Id
    private String id;

    private LocalDateTime acceptDate;

    private String sourceCompany;

    @OneToMany(mappedBy = "requestDetails", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventEntity> events;
}

