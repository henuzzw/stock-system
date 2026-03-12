package ai.openclaw.stockweb.strategy;

import ai.openclaw.stockweb.account.DailyPlanView;
import ai.openclaw.stockweb.account.StrategyRunView;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class StrategyExecutionRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public StrategyExecutionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public BigDecimal findCashBalanceByUserId(long userId) {
        BigDecimal cashBalance = jdbc.queryForObject(
                """
                SELECT cash_balance
                FROM accounts
                WHERE user_id = :userId
                """,
                Map.of("userId", userId),
                BigDecimal.class
        );
        return cashBalance == null ? BigDecimal.ZERO : cashBalance;
    }

    public List<ExecutionCandidate> findExecutionCandidates(LocalDate runDate) {
        List<ExecutionCandidate> ranked = jdbc.query(
                """
                SELECT c.symbol_id,
                       picked.pool_name,
                       picked.rank_value,
                       s.total_score,
                       s.trend_ok
                FROM (
                    SELECT symbol_id, 'left' AS pool_name, rank_left AS rank_value
                    FROM candidates_daily
                    WHERE run_date = :runDate
                      AND rank_left IS NOT NULL
                      AND rank_left <= 3
                    UNION ALL
                    SELECT symbol_id, 'right' AS pool_name, rank_right AS rank_value
                    FROM candidates_daily
                    WHERE run_date = :runDate
                      AND rank_right IS NOT NULL
                      AND rank_right <= 3
                ) picked
                JOIN candidates_daily c
                  ON c.run_date = :runDate
                 AND c.symbol_id = picked.symbol_id
                LEFT JOIN scores_daily s
                  ON s.run_date = :runDate
                 AND s.symbol_id = picked.symbol_id
                ORDER BY picked.rank_value ASC, picked.pool_name ASC, c.symbol_id ASC
                """,
                Map.of("runDate", runDate),
                (rs, rowNum) -> new ExecutionCandidate(
                        rs.getLong("symbol_id"),
                        rs.getString("pool_name"),
                        (Integer) rs.getObject("rank_value"),
                        rs.getBigDecimal("total_score"),
                        (Integer) rs.getObject("trend_ok")
                )
        );

        Map<Long, ExecutionCandidate> deduped = new LinkedHashMap<>();
        for (ExecutionCandidate candidate : ranked) {
            if (candidate.trendOk() != null && candidate.trendOk() != 1) {
                continue;
            }
            deduped.putIfAbsent(candidate.symbolId(), candidate);
        }
        return new ArrayList<>(deduped.values());
    }

    public Optional<BigDecimal> findMinuteClosePrice(long symbolId, LocalDate runDate, int hour, int minute) {
        LocalDateTime ts = runDate.atTime(hour, minute);
        List<BigDecimal> prices = jdbc.query(
                """
                SELECT close
                FROM minute_prices
                WHERE symbol_id = :symbolId
                  AND ts = :ts
                LIMIT 1
                """,
                new MapSqlParameterSource()
                        .addValue("symbolId", symbolId)
                        .addValue("ts", ts),
                (rs, rowNum) -> rs.getBigDecimal("close")
        );
        return prices.stream().findFirst();
    }

    public long createStrategyRun(long userId, String strategyKey, LocalDate runDate, String stage, String status, String summary) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO strategy_runs (user_id, strategy_key, run_date, stage, status, summary)
                VALUES (:userId, :strategyKey, :runDate, :stage, :status, :summary)
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("strategyKey", strategyKey)
                        .addValue("runDate", runDate)
                        .addValue("stage", stage)
                        .addValue("status", status)
                        .addValue("summary", summary),
                keyHolder,
                new String[]{"id"}
        );
        return requireKey(keyHolder);
    }

    public void updateStrategyRun(long strategyRunId, String stage, String status, String summary) {
        jdbc.update(
                """
                UPDATE strategy_runs
                SET stage = :stage,
                    status = :status,
                    summary = :summary
                WHERE id = :strategyRunId
                """,
                Map.of(
                        "strategyRunId", strategyRunId,
                        "stage", stage,
                        "status", status,
                        "summary", summary
                )
        );
    }

    public long createDailyPlan(
            long strategyRunId,
            long userId,
            long symbolId,
            LocalDate runDate,
            String planType,
            String poolName,
            Integer rankValue,
            BigDecimal totalScore,
            Integer trendOk,
            BigDecimal targetWeight,
            BigDecimal targetAmount,
            String actionReason,
            String status
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO daily_plan (
                    strategy_run_id, user_id, symbol_id, run_date, plan_type, pool_name, rank_value,
                    total_score, trend_ok, target_weight, target_amount, action_reason, status
                )
                VALUES (
                    :strategyRunId, :userId, :symbolId, :runDate, :planType, :poolName, :rankValue,
                    :totalScore, :trendOk, :targetWeight, :targetAmount, :actionReason, :status
                )
                """,
                new MapSqlParameterSource()
                        .addValue("strategyRunId", strategyRunId)
                        .addValue("userId", userId)
                        .addValue("symbolId", symbolId)
                        .addValue("runDate", runDate)
                        .addValue("planType", planType)
                        .addValue("poolName", poolName)
                        .addValue("rankValue", rankValue)
                        .addValue("totalScore", totalScore)
                        .addValue("trendOk", trendOk)
                        .addValue("targetWeight", targetWeight)
                        .addValue("targetAmount", targetAmount)
                        .addValue("actionReason", actionReason)
                        .addValue("status", status),
                keyHolder,
                new String[]{"id"}
        );
        return requireKey(keyHolder);
    }

    public void updateDailyPlanStatus(long planId, String status, String actionReason) {
        jdbc.update(
                """
                UPDATE daily_plan
                SET status = :status,
                    action_reason = :actionReason
                WHERE id = :planId
                """,
                Map.of(
                        "planId", planId,
                        "status", status,
                        "actionReason", actionReason
                )
        );
    }

    public long createOrder(
            long userId,
            long symbolId,
            String side,
            String orderType,
            String status,
            BigDecimal requestedQuantity,
            BigDecimal filledQuantity,
            BigDecimal avgFillPrice,
            String source,
            String note
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO orders (
                    user_id, symbol_id, side, order_type, status, requested_quantity, filled_quantity,
                    avg_fill_price, source, note
                )
                VALUES (
                    :userId, :symbolId, :side, :orderType, :status, :requestedQuantity, :filledQuantity,
                    :avgFillPrice, :source, :note
                )
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("symbolId", symbolId)
                        .addValue("side", side)
                        .addValue("orderType", orderType)
                        .addValue("status", status)
                        .addValue("requestedQuantity", requestedQuantity)
                        .addValue("filledQuantity", filledQuantity)
                        .addValue("avgFillPrice", avgFillPrice)
                        .addValue("source", source)
                        .addValue("note", note),
                keyHolder,
                new String[]{"id"}
        );
        return requireKey(keyHolder);
    }

    public long createTrade(
            long orderId,
            long userId,
            long symbolId,
            String side,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal amount,
            BigDecimal fee,
            Instant executedAt
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO trades (order_id, user_id, symbol_id, side, quantity, price, amount, fee, executed_at)
                VALUES (:orderId, :userId, :symbolId, :side, :quantity, :price, :amount, :fee, :executedAt)
                """,
                new MapSqlParameterSource()
                        .addValue("orderId", orderId)
                        .addValue("userId", userId)
                        .addValue("symbolId", symbolId)
                        .addValue("side", side)
                        .addValue("quantity", quantity)
                        .addValue("price", price)
                        .addValue("amount", amount)
                        .addValue("fee", fee)
                        .addValue("executedAt", Timestamp.from(executedAt)),
                keyHolder,
                new String[]{"id"}
        );
        return requireKey(keyHolder);
    }

    public void updateAccountCashBalance(long userId, BigDecimal delta) {
        jdbc.update(
                """
                UPDATE accounts
                SET cash_balance = cash_balance + :delta
                WHERE user_id = :userId
                """,
                Map.of(
                        "userId", userId,
                        "delta", delta
                )
        );
    }

    public void upsertPosition(long userId, long symbolId, String side, BigDecimal quantity, BigDecimal price) {
        if ("BUY".equalsIgnoreCase(side)) {
            jdbc.update(
                    """
                    INSERT INTO positions (user_id, symbol_id, quantity, available_quantity, avg_cost)
                    VALUES (:userId, :symbolId, :quantity, :quantity, :price)
                    ON DUPLICATE KEY UPDATE
                        avg_cost = CASE
                            WHEN quantity + VALUES(quantity) = 0 THEN 0
                            ELSE ((quantity * avg_cost) + (VALUES(quantity) * VALUES(avg_cost))) / (quantity + VALUES(quantity))
                        END,
                        quantity = quantity + VALUES(quantity),
                        available_quantity = available_quantity + VALUES(available_quantity)
                    """,
                    Map.of(
                            "userId", userId,
                            "symbolId", symbolId,
                            "quantity", quantity,
                            "price", price
                    )
            );
            return;
        }

        jdbc.update(
                """
                UPDATE positions
                SET quantity = GREATEST(quantity - :quantity, 0),
                    available_quantity = GREATEST(available_quantity - :quantity, 0),
                    avg_cost = CASE
                        WHEN GREATEST(quantity - :quantity, 0) = 0 THEN 0
                        ELSE avg_cost
                    END
                WHERE user_id = :userId
                  AND symbol_id = :symbolId
                """,
                Map.of(
                        "userId", userId,
                        "symbolId", symbolId,
                        "quantity", quantity
                )
        );
    }

    public StrategyRunView findLatestStrategyRunByUserId(long userId) {
        List<StrategyRunView> runs = jdbc.query(
                """
                SELECT id, user_id, strategy_key, run_date, stage, status, summary, created_at
                FROM strategy_runs
                WHERE user_id = :userId
                ORDER BY run_date DESC, id DESC
                LIMIT 1
                """,
                Map.of("userId", userId),
                (rs, rowNum) -> new StrategyRunView(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("strategy_key"),
                        rs.getDate("run_date").toLocalDate(),
                        rs.getString("stage"),
                        rs.getString("status"),
                        rs.getString("summary"),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
        return runs.isEmpty() ? null : runs.get(0);
    }

    public List<DailyPlanView> findDailyPlansByStrategyRunId(long strategyRunId) {
        return jdbc.query(
                """
                SELECT dp.id, dp.strategy_run_id, dp.user_id, dp.symbol_id, s.code, s.name,
                       dp.run_date, dp.plan_type, dp.pool_name, dp.rank_value,
                       dp.total_score, dp.trend_ok, dp.target_weight, dp.target_amount,
                       dp.action_reason, dp.status, dp.created_at
                FROM daily_plan dp
                JOIN symbols s ON s.id = dp.symbol_id
                WHERE dp.strategy_run_id = :strategyRunId
                ORDER BY dp.id ASC
                """,
                Map.of("strategyRunId", strategyRunId),
                (rs, rowNum) -> new DailyPlanView(
                        rs.getLong("id"),
                        rs.getLong("strategy_run_id"),
                        rs.getLong("user_id"),
                        rs.getLong("symbol_id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getDate("run_date").toLocalDate(),
                        rs.getString("plan_type"),
                        rs.getString("pool_name"),
                        (Integer) rs.getObject("rank_value"),
                        rs.getBigDecimal("total_score"),
                        (Integer) rs.getObject("trend_ok"),
                        rs.getBigDecimal("target_weight"),
                        rs.getBigDecimal("target_amount"),
                        rs.getString("action_reason"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
    }

    private long requireKey(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Missing generated key");
        }
        return key.longValue();
    }
}
