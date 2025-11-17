package org.example.controller;

import org.example.model.EventEntity;
import org.example.model.ProductEntity;
import org.example.repository.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductsController {
    private final EventRepository eventRepository;

    public ProductsController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping("/{insuredId}")
    public ResponseEntity<?> getProductsByInsured(@PathVariable String insuredId) {
        List<EventEntity> events = eventRepository.findByInsuredId(insuredId);
        if (events.isEmpty()) return ResponseEntity.notFound().build();

        // group by sourceCompany from requestDetails
        Map<String, List<ProductResponse>> grouped = new HashMap<>();
        for (EventEntity e : events) {
            String source = e.getRequestDetails() != null ? e.getRequestDetails().getSourceCompany() : "unknown";
            List<ProductResponse> prs = grouped.computeIfAbsent(source, k -> new ArrayList<>());
            if (e.getProducts() != null) {
                for (ProductEntity p : e.getProducts()) {
                    prs.add(new ProductResponse(p.getId(), p.getType(), p.getPrice(), p.getStartDate(), p.getEndDate(), e.getId()));
                }
            }
        }

        // Build response
        List<CompanyGroup> response = grouped.entrySet().stream()
                .map(en -> new CompanyGroup(en.getKey(), en.getValue()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new InsuredProductsResponse(insuredId, response));
    }

    public static class InsuredProductsResponse {
        public String insuredId;
        public List<CompanyGroup> groups;
        public InsuredProductsResponse(String insuredId, List<CompanyGroup> groups) { this.insuredId = insuredId; this.groups = groups; }
    }

    public static class CompanyGroup {
        public String sourceCompany;
        public List<ProductResponse> products;
        public CompanyGroup(String sourceCompany, List<ProductResponse> products) { this.sourceCompany = sourceCompany; this.products = products; }
    }

    public static class ProductResponse {
        public String id;
        public String type;
        public java.math.BigDecimal price;
        public java.time.LocalDate startDate;
        public java.time.LocalDate endDate;
        public String eventId;

        public ProductResponse(String id, String type, java.math.BigDecimal price, java.time.LocalDate startDate, java.time.LocalDate endDate, String eventId) {
            this.id = id; this.type = type; this.price = price; this.startDate = startDate; this.endDate = endDate; this.eventId = eventId;
        }
    }
}

