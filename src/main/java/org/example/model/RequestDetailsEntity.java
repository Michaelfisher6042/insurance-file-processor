package org.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "request_details")
public class RequestDetailsEntity {
    @Id
    private String id;

    private LocalDateTime acceptDate;

    private String sourceCompany;

    @OneToMany(mappedBy = "requestDetails", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventEntity> events;

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public LocalDateTime getAcceptDate() { return acceptDate; }
    public void setAcceptDate(LocalDateTime acceptDate) { this.acceptDate = acceptDate; }
    public String getSourceCompany() { return sourceCompany; }
    public void setSourceCompany(String sourceCompany) { this.sourceCompany = sourceCompany; }
    public List<EventEntity> getEvents() { return events; }
    public void setEvents(List<EventEntity> events) { this.events = events; }
}

