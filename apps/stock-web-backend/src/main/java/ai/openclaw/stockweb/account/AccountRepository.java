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
}
