package ai.openclaw.stockweb.account;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeView(
        long id,
        long orderId,
        long userId,
        long symbolId,
        String code,
        String name,
        String side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal amount,
        BigDecimal fee,
        Instant executedAt
) {
}
