package org.example.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "event")
@Data
public class EventEntity {
    @Id
    private String id;

    private String type;

    private String insuredId;

    @ManyToOne
    @JoinColumn(name = "request_id")
    private RequestDetailsEntity requestDetails;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductEntity> products;
}

