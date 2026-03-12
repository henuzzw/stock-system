package ai.openclaw.stockweb.strategy;

import ai.openclaw.stockweb.account.DailyPlanView;
import ai.openclaw.stockweb.account.StrategyRunView;

import java.util.List;

public record LatestStrategyExecutionResponse(
        StrategyRunView run,
        List<DailyPlanView> plans
) {
}
