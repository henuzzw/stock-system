package ai.openclaw.stockweb.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DailyPlanView(
        long id,
        long strategyRunId,
        long userId,
        long symbolId,
        String code,
        String name,
        LocalDate runDate,
        String planType,
        String poolName,
        Integer rankValue,
        BigDecimal totalScore,
        Integer trendOk,
        BigDecimal targetWeight,
        BigDecimal targetAmount,
        String actionReason,
        String status,
        Instant createdAt
) {
}
