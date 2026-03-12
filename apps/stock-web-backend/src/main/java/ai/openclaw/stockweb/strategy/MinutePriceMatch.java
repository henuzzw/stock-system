package ai.openclaw.stockweb.strategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MinutePriceMatch(
        LocalDateTime matchedAt,
        BigDecimal closePrice
) {
}
