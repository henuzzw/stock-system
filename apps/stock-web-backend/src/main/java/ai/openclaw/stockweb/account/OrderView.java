package ai.openclaw.stockweb.account;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderView(
        long id,
        long userId,
        long symbolId,
        String code,
        String name,
        String side,
        String orderType,
        String status,
        BigDecimal requestedQuantity,
        BigDecimal filledQuantity,
        BigDecimal limitPrice,
        BigDecimal avgFillPrice,
        String source,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
}
