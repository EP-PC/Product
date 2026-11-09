package store.product;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record ProductOut(
    String id,
    String name,
    BigDecimal price,
    String unit
) {}
