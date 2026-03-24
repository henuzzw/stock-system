package ai.openclaw.stockweb.api;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ApiController {

    private final NamedParameterJdbcTemplate jdbc;

    public ApiController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("ok", true);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        var technical = normalizeMap(jdbc.queryForMap(
                "SELECT COUNT(*) AS cnt, MAX(run_date) AS latest FROM technical_low_daily",
                Map.of()
        ));
        var valuation = normalizeMap(jdbc.queryForMap(
                "SELECT COUNT(*) AS cnt, MAX(run_date) AS latest FROM valuation_low_daily",
                Map.of()
        ));
        var candidates = normalizeMap(jdbc.queryForMap(
                "SELECT COUNT(*) AS cnt, MAX(run_date) AS latest FROM candidates_daily",
                Map.of()
        ));
        var scores = normalizeMap(jdbc.queryForMap(
                "SELECT COUNT(*) AS cnt, MAX(run_date) AS latest FROM scores_daily",
                Map.of()
        ));
        var minutePrices = normalizeMap(jdbc.queryForMap(
                "SELECT COUNT(*) AS cnt, MAX(ts) AS latest FROM minute_prices",
                Map.of()
        ));
        var symbols = normalizeMap(jdbc.queryForMap(
                "SELECT COUNT(*) AS cnt FROM symbols WHERE is_active = 1",
                Map.of()
        ));

        return Map.of(
                "ok", true,
                "symbols", symbols,
                "technical", technical,
                "valuation", valuation,
                "candidates", candidates,
                "scores", scores,
                "minute_prices", minutePrices
        );
    }

    @GetMapping("/ranks/technical")
    public List<Map<String, Object>> technical(@RequestParam(required = false) String date) {
        String d = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
        return normalizeRows(jdbc.queryForList(
                """
                SELECT s.code, s.name, r.score, r.`rank`, r.reasons, r.run_date
                FROM technical_low_daily r
                JOIN symbols s ON s.id = r.symbol_id
                WHERE r.run_date = :d
                ORDER BY r.`rank` ASC
                LIMIT 20
                """,
                Map.of("d", d)
        ));
    }

    @GetMapping("/ranks/valuation")
    public List<Map<String, Object>> valuation(@RequestParam(required = false) String date) {
        String d = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
        return normalizeRows(jdbc.queryForList(
                """
                SELECT s.code, s.name, r.score, r.`rank`, r.reasons, r.run_date
                FROM valuation_low_daily r
                JOIN symbols s ON s.id = r.symbol_id
                WHERE r.run_date = :d
                ORDER BY r.`rank` ASC
                LIMIT 20
                """,
                Map.of("d", d)
        ));
    }

    @GetMapping("/candidates")
    public List<Map<String, Object>> candidates(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "left") String side,
            @RequestParam(required = false) Integer triggered,
            @RequestParam(required = false) Integer intersection,
            @RequestParam(defaultValue = "rank") String sort,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        String d = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
        boolean right = side != null && side.equalsIgnoreCase("right");

        String orderBy;
        if ("score".equalsIgnoreCase(sort)) {
            orderBy = right ? "c.right_signal_score DESC" : "c.left_signal_score DESC";
        } else {
            orderBy = right ? "c.rank_right ASC" : "c.rank_left ASC";
        }

        String where = "WHERE c.run_date = :d";
        if (triggered != null && triggered == 1) {
            where += right ? " AND c.right_triggered = 1" : " AND c.left_triggered = 1";
        }
        if (intersection != null && intersection == 1) {
            where += " AND c.in_technical_top20 = 1 AND c.in_valuation_top20 = 1";
        }

        String sql = "SELECT s.code, s.name, "
                + "c.in_technical_top20, c.in_valuation_top20, "
                + "c.left_signal_score, c.right_signal_score, "
                + "c.left_triggered, c.right_triggered, "
                + "c.rank_left, c.rank_right, "
                + "c.snapshot, c.run_date "
                + "FROM candidates_daily c JOIN symbols s ON s.id = c.symbol_id "
                + where + " "
                + "ORDER BY " + orderBy + " "
                + "LIMIT :limit";

        return normalizeRows(jdbc.queryForList(sql, Map.of("d", d, "limit", limit == null ? 20 : limit)));
    }

    @GetMapping("/symbol/{code}")
    public Map<String, Object> symbolDetail(
            @PathVariable String code,
            @RequestParam(required = false, defaultValue = "180") Integer days
    ) {
        String norm = code.matches("\\d+") ? String.format("%06d", Integer.parseInt(code)) : code.toUpperCase();
        var symbol = normalizeMap(jdbc.queryForMap(
                "SELECT id, code, name, market, exchange FROM symbols WHERE code = :c LIMIT 1",
                Map.of("c", norm)
        ));
        long symbolId = ((Number) symbol.get("id")).longValue();

        var prices = normalizeRows(jdbc.queryForList(
                """
                SELECT trade_date, open, high, low, close, volume, turnover
                FROM daily_prices
                WHERE symbol_id = :sid
                ORDER BY trade_date DESC
                LIMIT :n
                """,
                Map.of("sid", symbolId, "n", days)
        ));

        var technicals = normalizeRows(jdbc.queryForList(
                """
                SELECT trade_date, sma_10, sma_20, rsi_14, macd, macd_signal
                FROM technicals
                WHERE symbol_id = :sid
                ORDER BY trade_date DESC
                LIMIT :n
                """,
                Map.of("sid", symbolId, "n", days)
        ));

        var fundamentals = normalizeRows(jdbc.queryForList(
                """
                SELECT trade_date, pe_ttm, pb, ps, dividend_yield, total_market_cap, float_market_cap
                FROM fundamentals
                WHERE symbol_id = :sid
                ORDER BY trade_date DESC
                LIMIT 60
                """,
                Map.of("sid", symbolId)
        ));

        var candidates = normalizeRows(jdbc.queryForList(
                """
                SELECT run_date, left_signal_score, right_signal_score, left_triggered, right_triggered,
                       rank_left, rank_right, snapshot
                FROM candidates_daily
                WHERE symbol_id = :sid
                ORDER BY run_date DESC
                LIMIT 30
                """,
                Map.of("sid", symbolId)
        ));

        var intraday = normalizeRows(jdbc.queryForList(
                """
                SELECT ts, open, high, low, close, volume, amount, avg_price
                FROM minute_prices
                WHERE symbol_id = :sid
                  AND DATE(ts) = (
                    SELECT DATE(MAX(ts))
                    FROM minute_prices
                    WHERE symbol_id = :sid
                  )
                ORDER BY ts ASC
                """,
                Map.of("sid", symbolId)
        ));

        return Map.of(
                "symbol", symbol,
                "prices", prices,
                "technicals", technicals,
                "fundamentals", fundamentals,
                "candidates", candidates,
                "intraday", intraday
        );
    }

    private List<Map<String, Object>> normalizeRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> normalized = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            normalized.add(normalizeMap(row));
        }
        return normalized;
    }

    private Map<String, Object> normalizeMap(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            normalized.put(entry.getKey(), normalizeValue(entry.getValue()));
        }
        return normalized;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate.toString();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toString();
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().toString();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toString();
        }
        return value;
    }
}
