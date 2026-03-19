package ai.openclaw.stockweb.mapper;

import ai.openclaw.stockweb.account.DailyPlanView;
import ai.openclaw.stockweb.account.PositionView;
import ai.openclaw.stockweb.account.StrategyRunView;
import ai.openclaw.stockweb.account.TradeView;
import ai.openclaw.stockweb.strategy.ExecutionCandidate;
import ai.openclaw.stockweb.strategy.MinutePriceMatch;
import ai.openclaw.stockweb.strategy.PositionSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface StrategyExecutionMapper {

    BigDecimal findCashBalanceByUserId(@Param("userId") long userId);

    List<ExecutionCandidate> findExecutionCandidates(@Param("runDate") LocalDate runDate);

    Optional<MinutePriceMatch> findNearestMinuteClosePrice(
            @Param("symbolId") long symbolId,
            @Param("runDate") LocalDate runDate,
            @Param("targetHour") int targetHour,
            @Param("targetMinute") int targetMinute,
            @Param("windowBeforeMinutes") int windowBeforeMinutes,
            @Param("windowAfterMinutes") int windowAfterMinutes
    );

    List<PositionSnapshot> findOpenPositions(@Param("userId") long userId);

    Optional<BigDecimal> findLatestClosePrice(@Param("symbolId") long symbolId, @Param("tradeDate") LocalDate tradeDate);

    Optional<Integer> findLatestTrendOk(@Param("symbolId") long symbolId, @Param("runDate") LocalDate runDate);

    boolean isOutOfTopKForConsecutiveDays(
            @Param("symbolId") long symbolId,
            @Param("poolName") String poolName,
            @Param("runDate") LocalDate runDate,
            @Param("topK") int topK,
            @Param("consecutiveDays") int consecutiveDays
    );

    int insertStrategyRun(StrategyRunView strategyRun);

    Optional<StrategyRunView> findLatestStrategyRunByUserId(@Param("userId") long userId);

    List<DailyPlanView> findDailyPlansByStrategyRunId(@Param("strategyRunId") long strategyRunId);

    int insertDailyPlan(@Param("userId") long userId,
                        @Param("strategyRunId") long strategyRunId,
                        @Param("symbolId") long symbolId,
                        @Param("planDate") LocalDate planDate,
                        @Param("action") String action,
                        @Param("side") String side,
                        @Param("quantity") BigDecimal quantity,
                        @Param("priceTarget") BigDecimal priceTarget,
                        @Param("matchedMinute") String matchedMinute,
                        @Param("status") String status,
                        @Param("createdAt") LocalDateTime createdAt);

    int insertOrder(@Param("userId") long userId,
                    @Param("strategyRunId") long strategyRunId,
                    @Param("symbolId") long symbolId,
                    @Param("orderType") String orderType,
                    @Param("side") String side,
                    @Param("quantity") BigDecimal quantity,
                    @Param("price") BigDecimal price,
                    @Param("status") String status,
                    @Param("createdAt") LocalDateTime createdAt);

    int insertTrade(@Param("userId") long userId,
                    @Param("strategyRunId") long strategyRunId,
                    @Param("symbolId") long symbolId,
                    @Param("side") String side,
                    @Param("quantity") BigDecimal quantity,
                    @Param("price") BigDecimal price,
                    @Param("amount") BigDecimal amount,
                    @Param("createdAt") LocalDateTime createdAt);

    int updateAccountCashBalance(@Param("userId") long userId, @Param("delta") BigDecimal delta);

    int upsertPosition(@Param("userId") long userId,
                        @Param("symbolId") long symbolId,
                        @Param("quantity") BigDecimal quantity,
                        @Param("availableQuantity") BigDecimal availableQuantity,
                        @Param("avgCost") BigDecimal avgCost,
                        @Param("createdAt") LocalDateTime createdAt,
                        @Param("updatedAt") LocalDateTime updatedAt);

    int updateDailyPlanStatus(@Param("id") long id, @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt);

    int updateStrategyRun(@Param("id") long id, @Param("status") String status, @Param("message") String message, @Param("endedAt") LocalDateTime endedAt);

    Optional<BigDecimal> findLatestClosePriceAnyDate(@Param("symbolId") long symbolId);
}
