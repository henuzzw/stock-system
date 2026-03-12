package ai.openclaw.stockweb.account;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
public class AccountRepository {
    private static final BigDecimal INITIAL_CASH = new BigDecimal("10000000.00");

    private final NamedParameterJdbcTemplate jdbc;

    public AccountRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void createDefaultAccount(long userId) {
        jdbc.update(
                """
                INSERT INTO accounts (user_id, initial_cash, cash_balance)
                VALUES (:userId, :initialCash, :cashBalance)
                """,
                Map.of(
                        "userId", userId,
                        "initialCash", INITIAL_CASH,
                        "cashBalance", INITIAL_CASH
                )
        );
    }

    public List<AccountPositionView> findPositionsByUserId(long userId) {
        return jdbc.query(
                """
                SELECT p.id, p.user_id, p.symbol_id, s.code, s.name,
                       p.quantity, p.available_quantity, p.avg_cost,
                       p.created_at, p.updated_at
                FROM positions p
                JOIN symbols s ON s.id = p.symbol_id
                WHERE p.user_id = :userId
                ORDER BY s.code ASC
                """,
                Map.of("userId", userId),
                (rs, rowNum) -> new AccountPositionView(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getLong("symbol_id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("available_quantity"),
                        rs.getBigDecimal("avg_cost"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()
                )
        );
    }

    public AccountSummaryView findSummaryByUserId(long userId) {
        return jdbc.queryForObject(
                """
                SELECT a.initial_cash,
                       a.cash_balance,
                       COUNT(p.id) AS positions_count,
                       COALESCE(SUM(p.quantity * latest.close), 0) AS positions_market_value
                FROM accounts a
                LEFT JOIN positions p ON p.user_id = a.user_id
                LEFT JOIN (
                    SELECT dp.symbol_id, dp.close
                    FROM daily_prices dp
                    JOIN (
                        SELECT symbol_id, MAX(trade_date) AS max_trade_date
                        FROM daily_prices
                        GROUP BY symbol_id
                    ) latest_dp
                      ON latest_dp.symbol_id = dp.symbol_id
                     AND latest_dp.max_trade_date = dp.trade_date
                ) latest ON latest.symbol_id = p.symbol_id
                WHERE a.user_id = :userId
                GROUP BY a.initial_cash, a.cash_balance
                """,
                Map.of("userId", userId),
                (rs, rowNum) -> new AccountSummaryView(
                        rs.getBigDecimal("initial_cash"),
                        rs.getBigDecimal("cash_balance"),
                        rs.getInt("positions_count"),
                        rs.getBigDecimal("positions_market_value")
                )
        );
    }

    public List<OrderView> findOrdersByUserId(long userId) {
        return jdbc.query(
                """
                SELECT o.id, o.user_id, o.symbol_id, s.code, s.name,
                       o.side, o.order_type, o.status,
                       o.requested_quantity, o.filled_quantity,
                       o.limit_price, o.avg_fill_price,
                       o.source, o.note,
                       o.created_at, o.updated_at
                FROM orders o
                JOIN symbols s ON s.id = o.symbol_id
                WHERE o.user_id = :userId
                ORDER BY o.created_at DESC, o.id DESC
                LIMIT 200
                """,
                Map.of("userId", userId),
                (rs, rowNum) -> new OrderView(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getLong("symbol_id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("side"),
                        rs.getString("order_type"),
                        rs.getString("status"),
                        rs.getBigDecimal("requested_quantity"),
                        rs.getBigDecimal("filled_quantity"),
                        rs.getBigDecimal("limit_price"),
                        rs.getBigDecimal("avg_fill_price"),
                        rs.getString("source"),
                        rs.getString("note"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()
                )
        );
    }

    public List<TradeView> findTradesByUserId(long userId) {
        return jdbc.query(
                """
                SELECT t.id, t.order_id, t.user_id, t.symbol_id, s.code, s.name,
                       t.side, t.quantity, t.price, t.amount, t.fee, t.executed_at
                FROM trades t
                JOIN symbols s ON s.id = t.symbol_id
                WHERE t.user_id = :userId
                ORDER BY t.executed_at DESC, t.id DESC
                LIMIT 300
                """,
                Map.of("userId", userId),
                (rs, rowNum) -> new TradeView(
                        rs.getLong("id"),
                        rs.getLong("order_id"),
                        rs.getLong("user_id"),
                        rs.getLong("symbol_id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("side"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("price"),
                        rs.getBigDecimal("amount"),
                        rs.getBigDecimal("fee"),
                        rs.getTimestamp("executed_at").toInstant()
                )
        );
    }

    public List<StrategyRunView> findStrategyRunsByUserId(long userId) {
        return jdbc.query(
                """
                SELECT id, user_id, strategy_key, run_date, stage, status, summary, created_at
                FROM strategy_runs
                WHERE user_id = :userId
                ORDER BY run_date DESC, id DESC
                LIMIT 120
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
    }

    public List<DailyPlanView> findDailyPlansByUserId(long userId) {
        return jdbc.query(
                """
                SELECT dp.id, dp.strategy_run_id, dp.user_id, dp.symbol_id, s.code, s.name,
                       dp.run_date, dp.plan_type, dp.pool_name, dp.rank_value,
                       dp.total_score, dp.trend_ok, dp.target_weight, dp.target_amount,
                       dp.action_reason, dp.status, dp.created_at
                FROM daily_plan dp
                JOIN symbols s ON s.id = dp.symbol_id
                WHERE dp.user_id = :userId
                ORDER BY dp.run_date DESC, dp.id DESC
                LIMIT 300
                """,
                Map.of("userId", userId),
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
}
