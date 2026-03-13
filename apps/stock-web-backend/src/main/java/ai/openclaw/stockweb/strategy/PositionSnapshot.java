package ai.openclaw.stockweb.strategy;

import java.math.BigDecimal;

public record PositionSnapshot(
        long symbolId,
        BigDecimal quantity,
        BigDecimal availableQuantity,
        BigDecimal avgCost
) {
}
