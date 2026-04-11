package com.jht.nvntry.catalog.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update an existing product in the catalog")
public record PatchProductRequest(
        @NotBlank
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name
) {}