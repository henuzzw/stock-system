package ai.openclaw.stockweb.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StrategyExecutionResult(
        long strategyRunId,
        LocalDate runDate,
        BigDecimal budget,
        int plannedCount,
        int filledCount,
        int skippedCount,
        BigDecimal spentAmount,
        BigDecimal remainingCash,
        String status,
        String summary
) {
}
