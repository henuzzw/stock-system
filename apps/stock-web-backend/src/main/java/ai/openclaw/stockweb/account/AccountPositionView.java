package ai.openclaw.stockweb.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountPositionView(
        long id,
        long userId,
        long symbolId,
        String code,
        String name,
        BigDecimal quantity,
        BigDecimal availableQuantity,
        BigDecimal avgCost,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
