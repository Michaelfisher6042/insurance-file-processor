package org.example.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "product")
@Data
public class ProductEntity {
    @Id
    private String id;

    private String type;

    private BigDecimal price;

    private LocalDate startDate;

    private LocalDate endDate;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private EventEntity event;

}

