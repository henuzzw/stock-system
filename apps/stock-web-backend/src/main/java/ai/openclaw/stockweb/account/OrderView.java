package ai.openclaw.stockweb.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record OrderView(
        Long id,
        Long userId,
        Long strategyRunId,
        Long symbolId,
        String orderType,
        String side,
        BigDecimal quantity,
        BigDecimal price,
        String status,
        LocalDateTime createdAt,
        Instant fillUpdatedAt,
        String code,
        String name
) {
    public OrderView(Long id, Long userId, Long strategyRunId, Long symbolId, String orderType, String side,
                     BigDecimal quantity, BigDecimal price, String status, LocalDateTime createdAt) {
        this(id, userId, strategyRunId, symbolId, orderType, side, quantity, price, status, createdAt, null, null, null);
    }
}
