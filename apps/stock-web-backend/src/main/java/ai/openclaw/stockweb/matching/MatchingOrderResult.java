package ai.openclaw.stockweb.matching;

import java.math.BigDecimal;

public record MatchingOrderResult(
        boolean filled,
        BigDecimal amount
) {
    public static MatchingOrderResult filled(BigDecimal amount) {
        return new MatchingOrderResult(true, amount);
    }

    public static MatchingOrderResult skipped() {
        return new MatchingOrderResult(false, BigDecimal.ZERO);
    }
}
