package ai.openclaw.stockweb.strategy;

import ai.openclaw.stockweb.account.DailyPlanView;
import ai.openclaw.stockweb.account.StrategyRunView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class StrategyExecutionService {
    private static final String STRATEGY_KEY = "d7-mvp";
    private static final BigDecimal EXECUTION_BUDGET = new BigDecimal("100000.00");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter MINUTE_TS_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final StrategyExecutionRepository repository;

    public StrategyExecutionService(StrategyExecutionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public StrategyExecutionResult execute(long userId, LocalDate requestedRunDate) {
        LocalDate runDate = requestedRunDate == null ? LocalDate.now(MARKET_ZONE) : requestedRunDate;
        BigDecimal cashBalance = repository.findCashBalanceByUserId(userId).setScale(2, RoundingMode.HALF_UP);
        BigDecimal budget = cashBalance.min(EXECUTION_BUDGET).setScale(2, RoundingMode.HALF_UP);
        List<ExecutionCandidate> candidates = repository.findExecutionCandidates(runDate);

        long strategyRunId = repository.createStrategyRun(
                userId,
                STRATEGY_KEY,
                runDate,
                "EXECUTE_BUY",
                "RUNNING",
                "Building daily buy plan"
        );

        if (candidates.isEmpty() || budget.compareTo(BigDecimal.ZERO) <= 0) {
            String summary = candidates.isEmpty()
                    ? "No eligible candidates after rank/trend filtering"
                    : "Cash balance below execution budget threshold";
            repository.updateStrategyRun(strategyRunId, "EXECUTE_BUY", "SUCCESS", summary);
            return new StrategyExecutionResult(strategyRunId, runDate, budget, 0, 0, 0, ZERO, cashBalance, "SUCCESS", summary);
        }

        List<BigDecimal> weights = computeWeights(candidates);
        int plannedCount = 0;
        int filledCount = 0;
        int skippedCount = 0;
        BigDecimal spentAmount = ZERO;

        for (int i = 0; i < candidates.size(); i++) {
            ExecutionCandidate candidate = candidates.get(i);
            BigDecimal targetWeight = weights.get(i);
            BigDecimal targetAmount = budget.multiply(targetWeight).setScale(2, RoundingMode.HALF_UP);
            long planId = repository.createDailyPlan(
                    strategyRunId,
                    userId,
                    candidate.symbolId(),
                    runDate,
                    "BUY",
                    candidate.poolName(),
                    candidate.rankValue(),
                    candidate.totalScore(),
                    candidate.trendOk(),
                    targetWeight,
                    targetAmount,
                    "Planned from " + candidate.poolName() + " pool",
                    "PLANNED"
            );
            plannedCount++;

            if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
                repository.updateDailyPlanStatus(planId, "SKIPPED", "Target amount rounded to zero");
                skippedCount++;
                continue;
            }

            MinutePriceMatch minuteMatch = repository.findNearestMinuteClosePrice(candidate.symbolId(), runDate, 10, 10, 2, 2).orElse(null);
            if (minuteMatch == null || minuteMatch.closePrice() == null || minuteMatch.closePrice().compareTo(BigDecimal.ZERO) <= 0) {
                repository.updateDailyPlanStatus(planId, "SKIPPED", "Missing minute price near 10:10 (window 10:08-10:12)");
                skippedCount++;
                continue;
            }

            BigDecimal minutePrice = minuteMatch.closePrice();
            String matchedTime = formatMinuteTimestamp(minuteMatch.matchedAt());
            BigDecimal quantity = targetAmount.divide(minutePrice, 4, RoundingMode.DOWN);
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                repository.updateDailyPlanStatus(planId, "SKIPPED", "Quantity rounded to zero at matched minute price " + matchedTime);
                skippedCount++;
                continue;
            }

            BigDecimal amount = quantity.multiply(minutePrice).setScale(4, RoundingMode.HALF_UP);
            long orderId = repository.createOrder(
                    userId,
                    candidate.symbolId(),
                    "BUY",
                    "MARKET",
                    "FILLED",
                    quantity,
                    quantity,
                    minutePrice,
                    "strategy-exec",
                    "strategyRunId=" + strategyRunId + ",planId=" + planId + ",matchedMinute=" + matchedTime
            );
            repository.createTrade(
                    orderId,
                    userId,
                    candidate.symbolId(),
                    "BUY",
                    quantity,
                    minutePrice,
                    amount,
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    minuteMatch.matchedAt().atZone(MARKET_ZONE).toInstant()
            );
            repository.updateAccountCashBalance(userId, amount.negate());
            repository.upsertPosition(userId, candidate.symbolId(), "BUY", quantity, minutePrice);
            repository.updateDailyPlanStatus(planId, "FILLED", "Filled using minute_prices " + matchedTime + " close");

            filledCount++;
            spentAmount = spentAmount.add(amount).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal remainingCash = repository.findCashBalanceByUserId(userId).setScale(2, RoundingMode.HALF_UP);
        String status = skippedCount > 0 ? "PARTIAL" : "SUCCESS";
        String summary = "planned=" + plannedCount + ", filled=" + filledCount + ", skipped=" + skippedCount + ", spent=" + spentAmount;
        repository.updateStrategyRun(strategyRunId, "EXECUTE_BUY", status, summary);

        return new StrategyExecutionResult(
                strategyRunId,
                runDate,
                budget,
                plannedCount,
                filledCount,
                skippedCount,
                spentAmount,
                remainingCash,
                status,
                summary
        );
    }

    public LatestStrategyExecutionResponse latest(long userId) {
        StrategyRunView run = repository.findLatestStrategyRunByUserId(userId);
        if (run == null) {
            return new LatestStrategyExecutionResponse(null, List.of());
        }
        List<DailyPlanView> plans = repository.findDailyPlansByStrategyRunId(run.id());
        return new LatestStrategyExecutionResponse(run, plans);
    }

    private List<BigDecimal> computeWeights(List<ExecutionCandidate> candidates) {
        List<BigDecimal> scores = new ArrayList<>(candidates.size());
        BigDecimal total = BigDecimal.ZERO;
        for (ExecutionCandidate candidate : candidates) {
            BigDecimal score = candidate.totalScore() == null ? BigDecimal.ZERO : candidate.totalScore().max(BigDecimal.ZERO);
            scores.add(score);
            total = total.add(score);
        }

        List<BigDecimal> weights = new ArrayList<>(candidates.size());
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal running = BigDecimal.ZERO;
            for (int i = 0; i < scores.size(); i++) {
                BigDecimal weight = i == scores.size() - 1
                        ? BigDecimal.ONE.subtract(running)
                        : scores.get(i).divide(total, 8, RoundingMode.HALF_UP);
                weights.add(weight);
                running = running.add(weight);
            }
            return weights;
        }

        BigDecimal equalWeight = BigDecimal.ONE.divide(BigDecimal.valueOf(candidates.size()), 8, RoundingMode.HALF_UP);
        BigDecimal running = BigDecimal.ZERO;
        for (int i = 0; i < candidates.size(); i++) {
            BigDecimal weight = i == candidates.size() - 1
                    ? BigDecimal.ONE.subtract(running)
                    : equalWeight;
            weights.add(weight);
            running = running.add(weight);
        }
        return weights;
    }

    private String formatMinuteTimestamp(LocalDateTime matchedAt) {
        return matchedAt.format(MINUTE_TS_FORMATTER);
    }
}
