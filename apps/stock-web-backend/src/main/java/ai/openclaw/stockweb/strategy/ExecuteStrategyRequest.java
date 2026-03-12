package ai.openclaw.stockweb.strategy;

import java.time.LocalDate;

public record ExecuteStrategyRequest(
        LocalDate runDate
) {
}
