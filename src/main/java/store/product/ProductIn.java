package store.product;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record ProductIn(
    String name,
    BigDecimal price,
    String unit
) {}
