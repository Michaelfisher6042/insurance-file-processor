package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entities.EventEntity;
import org.example.entities.ProductEntity;
import org.example.entities.RequestDetailsEntity;
import org.example.xml.EventDto;
import org.example.xml.RootRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
@Service
@Slf4j
@RequiredArgsConstructor
public class RequestDetailsService {

    private final ProductsService productsService;
    private final DateTimeFormatter acceptDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");

    public RequestDetailsEntity getRequestDetailsEntity(RootRequest root) {
        RequestDetailsEntity rd = new RequestDetailsEntity();
        rd.setId(root.getRequestDetails().getId());
        // parse acceptDate tolerant
        try {
            rd.setAcceptDate(LocalDateTime.parse(root.getRequestDetails().getAcceptDate(), acceptDateFormatter));
        } catch (Exception ex) {
            try {
                rd.setAcceptDate(LocalDateTime.parse(root.getRequestDetails().getAcceptDate()));
            } catch (Exception ex2) {
                rd.setAcceptDate(null);
            }
        }
        rd.setSourceCompany(root.getRequestDetails().getSourceCompany());

        List<EventEntity> events = new ArrayList<>();
        if (root.getEvents() != null) {
            for (EventDto ed : root.getEvents()) {
                EventEntity e = getEventEntity(ed, rd);
                List<ProductEntity> products = productsService.getProductEntities(ed, e);
                e.setProducts(products);
                events.add(e);
            }
        }
        rd.setEvents(events);
        return rd;
    }

    private EventEntity getEventEntity(EventDto ed, RequestDetailsEntity rd) {
        EventEntity e = new EventEntity();
        e.setId(ed.getId());
        e.setType(ed.getType());
        e.setInsuredId(ed.getInsuredId());
        e.setRequestDetails(rd);
        return e;
    }

}
