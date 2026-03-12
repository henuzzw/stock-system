package ai.openclaw.stockweb.account;

import java.math.BigDecimal;

public record AccountSummaryView(
        BigDecimal initialCash,
        BigDecimal cashBalance,
        int positionsCount,
        BigDecimal positionsMarketValue
) {
}
