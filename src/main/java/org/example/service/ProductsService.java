package org.example.service;

import lombok.AllArgsConstructor;
import org.example.domain.CompanyGroup;
import org.example.domain.InsuredProductsResponse;
import org.example.domain.ProductResponse;
import org.example.dto.EventDto;
import org.example.dto.ProductDto;
import org.example.entities.EventEntity;
import org.example.entities.ProductEntity;
import org.example.repository.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductsService {
    private final EventRepository eventRepository;


    public ResponseEntity<?> getProductsByInsured(String insuredId) {
        List<EventEntity> events = eventRepository.findByInsuredId(insuredId);
        if (events.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

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


    public List<ProductEntity> getProductEntities(EventDto ed, EventEntity e) {
        List<ProductEntity> products = new ArrayList<>();
        if (ed.getProducts() != null) {
            for (ProductDto pd : ed.getProducts()) {
                ProductEntity p = getProductEntity(e, pd);
                products.add(p);
            }
        }
        return products;
    }

    private ProductEntity getProductEntity(EventEntity e, ProductDto pd) {
        ProductEntity p = new ProductEntity();
        p.setId(UUID.randomUUID().toString());
        p.setType(pd.getType());
        try {
            p.setPrice(new BigDecimal(pd.getPrice()));
        } catch (Exception ex) {
            p.setPrice(null);
        }
        try {
            p.setStartDate(LocalDate.parse(pd.getStartDate()));
        } catch (Exception ex) {
            p.setStartDate(null);
        }
        try {
            p.setEndDate(LocalDate.parse(pd.getEndDate()));
        } catch (Exception ex) {
            p.setEndDate(null);
        }
        p.setEvent(e);
        return p;
    }

}
