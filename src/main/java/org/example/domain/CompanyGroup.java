package org.example.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
@Data
@AllArgsConstructor
public class CompanyGroup {
    public String sourceCompany;
    public List<ProductResponse> products;
}