package org.example.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductResponse {
    public String id;
    public String type;
    public java.math.BigDecimal price;
    public java.time.LocalDate startDate;
    public java.time.LocalDate endDate;
    public String eventId;

}
