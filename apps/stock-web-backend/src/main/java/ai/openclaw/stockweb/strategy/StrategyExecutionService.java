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
                userId, STRATEGY_KEY, runDate, "EXECUTE_BUY_SELL", "RUNNING", "Building daily buy/sell plan"
        );

        int plannedCount = 0;
        int filledCount = 0;
        int skippedCount = 0;
        BigDecimal spentAmount = ZERO;

        // --- SELL pass ---
        for (PositionSnapshot position : repository.findOpenPositions(userId)) {
            BigDecimal latestClose = repository.findLatestClosePrice(position.symbolId(), runDate).orElse(null);
            Integer latestTrendOk = repository.findLatestTrendOk(position.symbolId(), runDate).orElse(null);
            if (latestClose == null || position.avgCost() == null || position.avgCost().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            boolean stopLoss = latestClose.subtract(position.avgCost())
                    .divide(position.avgCost(), 6, RoundingMode.HALF_UP)
                    .compareTo(new BigDecimal("-0.08")) <= 0;
            boolean trendFailed = latestTrendOk != null && latestTrendOk == 0;
            boolean rankingFailed =
                    repository.isOutOfTopKForConsecutiveDays(position.symbolId(), "right", runDate, 10, 2)
                    || repository.isOutOfTopKForConsecutiveDays(position.symbolId(), "left", runDate, 10, 2);

            if (!(stopLoss || trendFailed || rankingFailed)) {
                continue;
            }

            MinutePriceMatch sellMatch = repository.findNearestMinuteClosePrice(
                    position.symbolId(), runDate, 14, 30, 120, 30).orElse(null);
            if (sellMatch == null || sellMatch.closePrice() == null || sellMatch.closePrice().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal quantity = position.availableQuantity() == null ? position.quantity() : position.availableQuantity();
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            long sellPlanId = repository.createDailyPlanSell(
                    strategyRunId, userId, position.symbolId(), runDate,
                    quantity, quantity.multiply(sellMatch.closePrice()).setScale(2, RoundingMode.HALF_UP),
                    buildSellReason(stopLoss, trendFailed, rankingFailed), "PLANNED"
            );
            plannedCount++;

            BigDecimal amount = quantity.multiply(sellMatch.closePrice()).setScale(4, RoundingMode.HALF_UP);
            String matchedTime = formatMinuteTimestamp(sellMatch.matchedAt());
            long orderId = repository.createOrderWithInfo(
                    userId, position.symbolId(), "SELL", "MARKET", "FILLED",
                    quantity, sellMatch.closePrice(), "strategy-exec",
                    "strategyRunId=" + strategyRunId + ",planId=" + sellPlanId + ",matchedMinute=" + matchedTime
            );
            repository.createTradeWithFilled(
                    orderId, userId, position.symbolId(), "SELL",
                    quantity, sellMatch.closePrice(), amount,
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    sellMatch.matchedAt().atZone(MARKET_ZONE).toInstant()
            );
            repository.updateAccountCashBalance(userId, amount);
            repository.upsertPositionBySide(userId, position.symbolId(), "SELL", quantity, sellMatch.closePrice());
            repository.updateDailyPlanStatus(sellPlanId, "FILLED", "Sell filled at " + matchedTime);
            filledCount++;
        }

        // --- Check if any candidates ---
        if (candidates.isEmpty() || budget.compareTo(BigDecimal.ZERO) <= 0) {
            String summary = candidates.isEmpty()
                    ? "No eligible candidates after rank/trend filtering"
                    : "Cash balance below execution budget threshold";
            repository.updateStrategyRun(strategyRunId, "SUCCESS", summary, LocalDateTime.now());
            return new StrategyExecutionResult(strategyRunId, runDate, budget, plannedCount, filledCount, skippedCount, spentAmount, cashBalance, "SUCCESS", summary);
        }

        List<BigDecimal> weights = computeWeights(candidates);

        // --- BUY pass ---
        for (int i = 0; i < candidates.size(); i++) {
            ExecutionCandidate candidate = candidates.get(i);
            BigDecimal targetWeight = weights.get(i);
            BigDecimal targetAmount = budget.multiply(targetWeight).setScale(2, RoundingMode.HALF_UP);

            long planId = repository.createDailyPlanBuy(
                    strategyRunId, userId, candidate.symbolId(), runDate,
                    candidate.poolName(), candidate.rankValue(), candidate.totalScore(),
                    candidate.trendOk(), targetWeight, targetAmount, "PLANNED"
            );
            plannedCount++;

            if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
                repository.updateDailyPlanStatus(planId, "SKIPPED", "Target amount rounded to zero");
                skippedCount++;
                continue;
            }

            MinutePriceMatch minuteMatch = repository.findNearestMinuteClosePrice(
                    candidate.symbolId(), runDate, 10, 30, 60, 120).orElse(null);
            if (minuteMatch == null || minuteMatch.closePrice() == null || minuteMatch.closePrice().compareTo(BigDecimal.ZERO) <= 0) {
                repository.updateDailyPlanStatus(planId, "SKIPPED", "Missing intraday minute price near buy trigger");
                skippedCount++;
                continue;
            }

            BigDecimal latestClose = repository.findLatestClosePrice(candidate.symbolId(), runDate).orElse(minuteMatch.closePrice());
            if (minuteMatch.closePrice().compareTo(latestClose) > 0 && latestClose.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal gainPct = minuteMatch.closePrice().subtract(latestClose).divide(latestClose, 6, RoundingMode.HALF_UP);
                if (gainPct.compareTo(new BigDecimal("0.05")) > 0) {
                    repository.updateDailyPlanStatus(planId, "SKIPPED", "Intraday gain too hot for buy trigger");
                    skippedCount++;
                    continue;
                }
            }

            BigDecimal minutePrice = minuteMatch.closePrice();
            String matchedTime = formatMinuteTimestamp(minuteMatch.matchedAt());
            BigDecimal quantity = targetAmount.divide(minutePrice, 4, RoundingMode.DOWN);
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                repository.updateDailyPlanStatus(planId, "SKIPPED", "Quantity rounded to zero at " + matchedTime);
                skippedCount++;
                continue;
            }

            BigDecimal amount = quantity.multiply(minutePrice).setScale(4, RoundingMode.HALF_UP);
            long orderId = repository.createOrderWithInfo(
                    userId, candidate.symbolId(), "BUY", "MARKET", "FILLED",
                    quantity, minutePrice, "strategy-exec",
                    "strategyRunId=" + strategyRunId + ",planId=" + planId + ",matchedMinute=" + matchedTime
            );
            repository.createTradeWithFilled(
                    orderId, userId, candidate.symbolId(), "BUY",
                    quantity, minutePrice, amount,
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    minuteMatch.matchedAt().atZone(MARKET_ZONE).toInstant()
            );
            repository.updateAccountCashBalance(userId, amount.negate());
            repository.upsertPositionBySide(userId, candidate.symbolId(), "BUY", quantity, minutePrice);
            repository.updateDailyPlanStatus(planId, "FILLED", "Filled at " + matchedTime);

            filledCount++;
            spentAmount = spentAmount.add(amount).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal remainingCash = repository.findCashBalanceByUserId(userId).setScale(2, RoundingMode.HALF_UP);
        String status = skippedCount > 0 ? "PARTIAL" : "SUCCESS";
        String summary = "planned=" + plannedCount + ", filled=" + filledCount + ", skipped=" + skippedCount + ", spent=" + spentAmount;
        repository.updateStrategyRun(strategyRunId, status, summary, LocalDateTime.now());

        return new StrategyExecutionResult(
                strategyRunId, runDate, budget,
                plannedCount, filledCount, skippedCount, spentAmount, remainingCash,
                status, summary
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

    private String buildSellReason(boolean stopLoss, boolean trendFailed, boolean rankingFailed) {
        List<String> reasons = new ArrayList<>();
        if (stopLoss) reasons.add("hard_stop");
        if (trendFailed) reasons.add("trend_failure");
        if (rankingFailed) reasons.add("ranking_failure");
        return String.join(",", reasons);
    }
}
