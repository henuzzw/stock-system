package ai.openclaw.stockweb.strategy;

import java.math.BigDecimal;

public record ExecutionCandidate(
        long symbolId,
        String poolName,
        Integer rankValue,
        BigDecimal totalScore,
        Integer trendOk
) {
}
