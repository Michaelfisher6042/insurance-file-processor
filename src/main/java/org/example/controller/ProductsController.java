package org.example.controller;

import org.example.domain.CompanyGroup;
import org.example.domain.InsuredProductsResponse;
import org.example.domain.ProductResponse;
import org.example.entities.EventEntity;
import org.example.entities.ProductEntity;
import org.example.repository.EventRepository;
import org.example.service.ProductsService;
import org.springframework.http.MediaType;
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
    private final ProductsService productsService;

    public ProductsController(ProductsService productsService) {
        this.productsService = productsService;
    }

    @GetMapping(value = "/{insuredId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProductsByInsured(@PathVariable String insuredId) {
        return productsService.getProductsByInsured(insuredId);
    }




}
