package com.jht.nvntry.domain.inventory;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class InventoryKey implements Serializable {
    private UUID productId;
    private UUID locationId;

    public InventoryKey() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryKey that)) return false;
        return Objects.equals(productId, that.productId) && Objects.equals(locationId, that.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, locationId);
    }
}