package com.jht.nvntry.catalog.model.response;

import com.jht.nvntry.catalog.model.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Product details")
public record ProductResponse(

        @Schema(description = "Unique product ID")
        UUID id,

        @Schema(description = "Unique SKU - normalised to uppercase on creation")
        String sku,

        String name,

        Product.UnitOfMeasure unitOfMeasure,

        boolean active,

        OffsetDateTime createdAt
) {
    /* Static Factory
    * Mapping lives here, not in the service.
    * The service should not know about field names on the response object.
    * */
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getUnitOfMeasure(),
                p.isActive(),
                p.getCreatedAt()
        );
    }
}
//    @Schema(description = "Unique product ID")
//    UUID id,
//
//    @Schema(description = "Unique SKU - normalised to uppercase on creation")
//    String sku,
//
//    String name,
//
//    Product.UnitOfMeasure unitOfMeasure,
//
//    boolean active,
//
//    OffsetDateTime createdAt
//    public static ProductResponse from(Product p) {
//        return new ProductResponse(
//                p.getId(),
//                p.getSku(),
//                p.getName(),
//                p.getUnitOfMeasure(),
//                p.isActive(),
//                p.getCreatedAt()
//        );
//    }