package ai.openclaw.stockweb.account;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StrategyRunView(
        Long id,
        long userId,
        String strategyKey,
        LocalDate runDate,
        String runType,
        String status,
        String message,
        LocalDateTime createdAt,
        LocalDateTime endedAt
) {
}
