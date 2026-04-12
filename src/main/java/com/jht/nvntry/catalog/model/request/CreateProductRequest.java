package com.jht.nvntry.catalog.model.request;

import com.jht.nvntry.catalog.model.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a new product in the catalog")
public record CreateProductRequest(

        @NotBlank
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "SKU must be alphanumeric with hyphens or underscores only")
        @Schema(description = "Unique product identifier", example = "WIDGET-001")
        String sku,

        @NotBlank
        @Size(max = 255, message = "Name must not exceed 255 characters")
        @Schema(description = "Human-readable product name", example = "Blue Widget")
        String name,

        @NotNull(message = "Unit of measure is required")
        @Schema(description = "Unit in which this product is counted", example = "EACH")
        Product.UnitOfMeasure unitOfMeasure
) {}
//    @NotBlank(message = "SKU is required")
//    @Size(max = 100, message = "SKU must not exceed 100 characters")
//    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "SKU must be alphanumeric with hyphens or underscores only")
//    @Schema(description = "Unique product identifier", example = "WIDGET-001")
//    String sku,
//
//    @NotBlank(message = "Name is required")
//    @Size(max = 255, message = "Name must not exceed 255 characters")
//    @Schema(description = "Human-readable product name", example = "Blue Widget")
//    String name,
//
//    @NotNull(message = "Unit of measure is required")
//    @Schema(description = "Unit in which this product is counted", example = "EACH")
//    Product.UnitOfMeasure unitOfMeasure