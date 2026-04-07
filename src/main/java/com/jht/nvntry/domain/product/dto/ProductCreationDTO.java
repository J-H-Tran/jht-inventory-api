package com.jht.nvntry.domain.product.dto;

// Complete minus server-managed fields (ID, timestamps)
public record ProductCreationDTO(
        String sku,
        String name
) {}