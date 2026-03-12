package ai.openclaw.stockweb.account;

import java.time.Instant;
import java.time.LocalDate;

public record StrategyRunView(
        long id,
        long userId,
        String strategyKey,
        LocalDate runDate,
        String stage,
        String status,
        String summary,
        Instant createdAt
) {
}
