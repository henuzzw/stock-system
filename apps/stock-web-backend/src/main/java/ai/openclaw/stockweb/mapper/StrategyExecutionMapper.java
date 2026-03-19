package ai.openclaw.stockweb.mapper;

import ai.openclaw.stockweb.account.DailyPlanView;
import ai.openclaw.stockweb.account.OrderView;
import ai.openclaw.stockweb.account.PositionView;
import ai.openclaw.stockweb.account.StrategyRunView;
import ai.openclaw.stockweb.account.TradeView;
import ai.openclaw.stockweb.strategy.ExecutionCandidate;
import ai.openclaw.stockweb.strategy.MinutePriceMatch;
import ai.openclaw.stockweb.strategy.PositionSnapshot;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface StrategyExecutionMapper {

    @Select("""
            SELECT cash_balance
            FROM accounts
            WHERE user_id = #{userId}
            """)
    BigDecimal findCashBalanceByUserId(@Param("userId") long userId);

    @Select("""
            SELECT c.symbol_id,
                   picked.pool_name,
                   picked.rank_value,
                   s.total_score,
                   s.trend_ok
            FROM (
                SELECT symbol_id, 'left' AS pool_name, rank_left AS rank_value
                FROM candidates_daily
                WHERE run_date = #{runDate}
                  AND rank_left IS NOT NULL
                  AND rank_left <= 3
                UNION ALL
                SELECT symbol_id, 'right' AS pool_name, rank_right AS rank_value
                FROM candidates_daily
                WHERE run_date = #{runDate}
                  AND rank_right IS NOT NULL
                  AND rank_right <= 3
            ) picked
            JOIN candidates_daily c
              ON c.run_date = #{runDate}
             AND c.symbol_id = picked.symbol_id
            LEFT JOIN scores_daily s
              ON s.run_date = #{runDate}
             AND s.symbol_id = picked.symbol_id
            ORDER BY picked.rank_value ASC, picked.pool_name ASC, c.symbol_id ASC
            """)
    List<ExecutionCandidate> findExecutionCandidates(@Param("runDate") LocalDate runDate);

    @Select("""
            SELECT symbol_id, close_price AS closePrice, ts
            FROM minute_prices
            WHERE symbol_id = #{symbolId}
              AND DATE(ts) = #{runDate}
              AND TIME(ts) BETWEEN 
                  TIME(DATE_SUB(MAKETIME(#{targetHour}, #{targetMinute}, 0), INTERVAL #{windowBeforeMinutes} MINUTE))
                  AND TIME(DATE_ADD(MAKETIME(#{targetHour}, #{targetMinute}, 0), INTERVAL #{windowAfterMinutes} MINUTE))
            ORDER BY ABS(TIMESTAMPDIFF(MINUTE, ts, MAKETIME(#{targetHour}, #{targetMinute}, 0))) ASC
            LIMIT 1
            """)
    Optional<MinutePriceMatch> findNearestMinuteClosePrice(
            @Param("symbolId") long symbolId,
            @Param("runDate") LocalDate runDate,
            @Param("targetHour") int targetHour,
            @Param("targetMinute") int targetMinute,
            @Param("windowBeforeMinutes") int windowBeforeMinutes,
            @Param("windowAfterMinutes") int windowAfterMinutes
    );

    @Select("""
            SELECT p.symbol_id,
                   p.quantity,
                   p.available_quantity,
                   p.avg_cost
            FROM positions p
            WHERE p.user_id = #{userId}
              AND p.quantity > 0
            ORDER BY p.symbol_id ASC
            """)
    List<PositionSnapshot> findOpenPositions(@Param("userId") long userId);

    @Select("""
            SELECT close
            FROM daily_prices
            WHERE symbol_id = #{symbolId}
              AND trade_date = #{tradeDate}
            LIMIT 1
            """)
    Optional<BigDecimal> findLatestClosePrice(@Param("symbolId") long symbolId, @Param("tradeDate") LocalDate tradeDate);

    @Select("""
            SELECT trend_ok
            FROM scores_daily
            WHERE symbol_id = #{symbolId}
              AND run_date = #{runDate}
            LIMIT 1
            """)
    Optional<Integer> findLatestTrendOk(@Param("symbolId") long symbolId, @Param("runDate") LocalDate runDate);

    @Select("""
            SELECT COUNT(*) > 0
            FROM candidates_daily
            WHERE symbol_id = #{symbolId}
              AND run_date BETWEEN DATE_SUB(#{runDate}, INTERVAL #{consecutiveDays} DAY) AND DATE_SUB(#{runDate}, INTERVAL 1 DAY)
              AND (
                  (#{poolName} = 'left' AND (rank_left IS NULL OR rank_left > #{topK}))
                  OR (#{poolName} = 'right' AND (rank_right IS NULL OR rank_right > #{topK}))
              )
            """)
    boolean isOutOfTopKForConsecutiveDays(
            @Param("symbolId") long symbolId,
            @Param("poolName") String poolName,
            @Param("runDate") LocalDate runDate,
            @Param("topK") int topK,
            @Param("consecutiveDays") int consecutiveDays
    );

    @Insert("""
            INSERT INTO strategy_runs (user_id, strategy_key, run_date, run_type, status, message, created_at)
            VALUES (#{userId}, #{strategyKey}, #{runDate}, #{runType}, #{status}, #{message}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertStrategyRun(StrategyRunView strategyRun);

    @Insert("""
            INSERT INTO daily_plan (user_id, strategy_run_id, symbol_id, plan_date, action, side, quantity, price_target, 
                                    matched_minute, status, created_at)
            VALUES (#{userId}, #{strategyRunId}, #{symbolId}, #{planDate}, #{action}, #{side}, #{quantity}, #{priceTarget},
                    #{matchedMinute}, #{status}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDailyPlan(DailyPlanView dailyPlan);

    @Insert("""
            INSERT INTO orders (user_id, strategy_run_id, symbol_id, order_type, side, quantity, price, status, created_at)
            VALUES (#{userId}, #{strategyRunId}, #{symbolId}, #{orderType}, #{side}, #{quantity}, #{price}, #{status}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(ai.openclaw.stockweb.account.OrderView order);

    @Insert("""
            INSERT INTO trades (user_id, strategy_run_id, symbol_id, side, quantity, price, amount, created_at)
            VALUES (#{userId}, #{strategyRunId}, #{symbolId}, #{side}, #{quantity}, #{price}, #{amount}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTrade(TradeView trade);

    @Update("""
            UPDATE accounts
            SET cash_balance = cash_balance + #{delta}
            WHERE user_id = #{userId}
            """)
    int updateAccountCashBalance(@Param("userId") long userId, @Param("delta") BigDecimal delta);

    @Insert("""
            INSERT INTO positions (user_id, symbol_id, quantity, available_quantity, avg_cost, created_at, updated_at)
            VALUES (#{userId}, #{symbolId}, #{quantity}, #{availableQuantity}, #{avgCost}, #{createdAt}, #{updatedAt})
            ON DUPLICATE KEY UPDATE
                quantity = quantity + VALUES(quantity),
                available_quantity = available_quantity + VALUES(available_quantity),
                avg_cost = CASE
                    WHEN quantity + VALUES(quantity) = 0 THEN 0
                    ELSE ((quantity * avg_cost) + (VALUES(quantity) * VALUES(avg_cost))) / (quantity + VALUES(quantity))
                END,
                updated_at = VALUES(updated_at)
            """)
    int upsertPosition(ai.openclaw.stockweb.account.PositionView position);

    @Update("""
            UPDATE daily_plan
            SET status = #{status}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateDailyPlanStatus(@Param("id") long id, @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE strategy_runs
            SET status = #{status}, message = #{message}, ended_at = #{endedAt}
            WHERE id = #{id}
            """)
    int updateStrategyRun(@Param("id") long id, @Param("status") String status, @Param("message") String message, @Param("endedAt") LocalDateTime endedAt);

    @Select("""
            SELECT close
            FROM daily_prices
            WHERE symbol_id = #{symbolId}
            ORDER BY trade_date DESC
            LIMIT 1
            """)
    Optional<BigDecimal> findLatestClosePriceAnyDate(@Param("symbolId") long symbolId);
}
