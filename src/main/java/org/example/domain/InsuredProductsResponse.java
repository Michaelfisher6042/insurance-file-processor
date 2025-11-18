package org.example.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
@Data
@AllArgsConstructor
public  class InsuredProductsResponse {
    public String insuredId;
    public List<CompanyGroup> groups;
}