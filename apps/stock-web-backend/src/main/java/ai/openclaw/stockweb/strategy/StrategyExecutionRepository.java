package ai.openclaw.stockweb.strategy;

import ai.openclaw.stockweb.account.DailyPlanView;
import ai.openclaw.stockweb.account.OrderView;
import ai.openclaw.stockweb.account.PositionView;
import ai.openclaw.stockweb.account.StrategyRunView;
import ai.openclaw.stockweb.account.TradeView;
import ai.openclaw.stockweb.mapper.StrategyExecutionMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class StrategyExecutionRepository {
    private final StrategyExecutionMapper mapper;

    public StrategyExecutionRepository(StrategyExecutionMapper mapper) {
        this.mapper = mapper;
    }

    public BigDecimal findCashBalanceByUserId(long userId) {
        BigDecimal cashBalance = mapper.findCashBalanceByUserId(userId);
        return cashBalance == null ? BigDecimal.ZERO : cashBalance;
    }

    public List<ExecutionCandidate> findExecutionCandidates(LocalDate runDate) {
        List<ExecutionCandidate> ranked = mapper.findExecutionCandidates(runDate);
        Map<Long, ExecutionCandidate> deduped = new LinkedHashMap<>();
        for (ExecutionCandidate candidate : ranked) {
            if (candidate.trendOk() != null && candidate.trendOk() != 1) {
                continue;
            }
            deduped.putIfAbsent(candidate.symbolId(), candidate);
        }
        return deduped.values().stream().toList();
    }

    public Optional<MinutePriceMatch> findNearestMinuteClosePrice(
            long symbolId, LocalDate runDate,
            int targetHour, int targetMinute,
            int windowBeforeMinutes, int windowAfterMinutes) {
        return mapper.findNearestMinuteClosePrice(symbolId, runDate, targetHour, targetMinute, windowBeforeMinutes, windowAfterMinutes);
    }

    public List<PositionSnapshot> findOpenPositions(long userId) {
        return mapper.findOpenPositions(userId);
    }

    public Optional<BigDecimal> findLatestClosePrice(long symbolId, LocalDate tradeDate) {
        return mapper.findLatestClosePrice(symbolId, tradeDate);
    }

    public Optional<Integer> findLatestTrendOk(long symbolId, LocalDate runDate) {
        return mapper.findLatestTrendOk(symbolId, runDate);
    }

    public boolean isOutOfTopKForConsecutiveDays(long symbolId, String poolName, LocalDate runDate, int topK, int consecutiveDays) {
        return mapper.isOutOfTopKForConsecutiveDays(symbolId, poolName, runDate, topK, consecutiveDays);
    }

    // --- Strategy Run ---

    public StrategyRunView findLatestStrategyRunByUserId(long userId) {
        return mapper.findLatestStrategyRunByUserId(userId).orElse(null);
    }

    public List<DailyPlanView> findDailyPlansByStrategyRunId(long strategyRunId) {
        return mapper.findDailyPlansByStrategyRunId(strategyRunId);
    }

    @Transactional
    public long createStrategyRun(long userId, String strategyKey, LocalDate runDate, String runType, String status, String message) {
        StrategyRunView strategyRun = new StrategyRunView(
                null, userId, strategyKey, runDate, runType, status, message, LocalDateTime.now(), null
        );
        mapper.insertStrategyRun(strategyRun);
        return strategyRun.id();
    }

    // --- Daily Plan ---
    @Transactional
    public long createDailyPlanSell(long strategyRunId, long userId, long symbolId, LocalDate planDate,
                                    BigDecimal quantity, BigDecimal priceTarget,
                                    String reason, String status) {
        return mapper.insertDailyPlan(userId, strategyRunId, symbolId, planDate, "SELL", null,
                quantity, priceTarget, reason, status, LocalDateTime.now());
    }

    @Transactional
    public long createDailyPlanBuy(long strategyRunId, long userId, long symbolId, LocalDate planDate,
                                   String poolName, Integer rankValue, BigDecimal totalScore,
                                   Integer trendOk, BigDecimal weight, BigDecimal priceTarget,
                                   String status) {
        return mapper.insertDailyPlan(userId, strategyRunId, symbolId, planDate, "BUY", poolName,
                null, priceTarget, null, status, LocalDateTime.now());
    }

    // --- Orders ---
    @Transactional
    public long createOrder(long userId, long strategyRunId, long symbolId, String orderType, String side,
                            BigDecimal quantity, BigDecimal price, String status) {
        return mapper.insertOrder(userId, strategyRunId, symbolId, orderType, side, quantity, price, status, LocalDateTime.now());
    }

    @Transactional
    public long createOrderWithInfo(long userId, long symbolId, String orderType, String side, String status,
                                    BigDecimal quantity, BigDecimal price, String execType, String execInfo) {
        return mapper.insertOrder(userId, 0L, symbolId, orderType, side, quantity, price, status, LocalDateTime.now());
    }

    // --- Trades ---
    @Transactional
    public long createTrade(long userId, long strategyRunId, long symbolId, String side,
                            BigDecimal quantity, BigDecimal price, BigDecimal amount) {
        return mapper.insertTrade(userId, strategyRunId, symbolId, side, quantity, price, amount, LocalDateTime.now());
    }

    @Transactional
    public long createTradeWithFilled(long orderId, long userId, long symbolId, String side,
                                     BigDecimal quantity, BigDecimal price, BigDecimal amount,
                                     BigDecimal fee, Instant filledAt) {
        LocalDateTime filledAtLd = filledAt.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        return mapper.insertTrade(userId, 0L, symbolId, side, quantity, price, amount, filledAtLd);
    }

    // --- Positions ---
    @Transactional
    public void upsertPosition(long userId, long symbolId, BigDecimal quantity, BigDecimal availableQuantity, BigDecimal avgCost) {
        LocalDateTime now = LocalDateTime.now();
        mapper.upsertPosition(userId, symbolId, quantity, availableQuantity, avgCost, now, now);
    }

    @Transactional
    public void upsertPositionBySide(long userId, long symbolId, String side, BigDecimal quantity, BigDecimal avgCost) {
        LocalDateTime now = LocalDateTime.now();
        if ("BUY".equals(side)) {
            mapper.upsertPosition(userId, symbolId, quantity, quantity, avgCost, now, now);
        } else {
            mapper.upsertPosition(userId, symbolId, quantity.negate(), quantity.negate(), avgCost, now, now);
        }
    }

    // --- Status updates ---
    @Transactional
    public void updateDailyPlanStatus(long planId, String status) {
        mapper.updateDailyPlanStatus(planId, status, LocalDateTime.now());
    }

    @Transactional
    public void updateDailyPlanStatus(long planId, String status, String message) {
        mapper.updateDailyPlanStatus(planId, status, LocalDateTime.now());
    }

    @Transactional
    public void updateAccountCashBalance(long userId, BigDecimal delta) {
        mapper.updateAccountCashBalance(userId, delta);
    }

    @Transactional
    public void updateStrategyRun(long strategyRunId, String status, String message, LocalDateTime endedAt) {
        mapper.updateStrategyRun(strategyRunId, status, message, endedAt);
    }

    public Optional<BigDecimal> findLatestClosePriceAnyDate(long symbolId) {
        return mapper.findLatestClosePriceAnyDate(symbolId);
    }
}
