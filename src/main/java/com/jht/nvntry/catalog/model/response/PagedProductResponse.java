package com.jht.nvntry.catalog.model.response;

import java.util.List;

public record PagedProductResponse(
        List<ProductResponse> content,
        int size,
        boolean hasNext
) {
}