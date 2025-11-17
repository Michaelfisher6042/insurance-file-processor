package org.example.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "event")
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

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getInsuredId() { return insuredId; }
    public void setInsuredId(String insuredId) { this.insuredId = insuredId; }
    public RequestDetailsEntity getRequestDetails() { return requestDetails; }
    public void setRequestDetails(RequestDetailsEntity requestDetails) { this.requestDetails = requestDetails; }
    public List<ProductEntity> getProducts() { return products; }
    public void setProducts(List<ProductEntity> products) { this.products = products; }
}

