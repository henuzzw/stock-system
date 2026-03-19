package ai.openclaw.stockweb.account;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DailyPlanView(
        Long id,
        Long userId,
        Long strategyRunId,
        Long symbolId,
        LocalDate planDate,
        String action,
        String side,
        BigDecimal quantity,
        BigDecimal priceTarget,
        String matchedMinute,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
