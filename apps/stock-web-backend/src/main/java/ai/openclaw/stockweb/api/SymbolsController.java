package ai.openclaw.stockweb.api;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class SymbolsController {
    private final NamedParameterJdbcTemplate jdbc;

    public SymbolsController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/symbols")
    public List<Map<String, Object>> symbols(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "200") Integer limit
    ) {
        String query = (q == null) ? "" : q.trim();
        if (query.isBlank()) {
            return jdbc.queryForList(
                    "SELECT code, name, market, exchange FROM symbols WHERE is_active = 1 ORDER BY market, code LIMIT :limit",
                    Map.of("limit", limit)
            );
        }
        return jdbc.queryForList(
                """
                SELECT code, name, market, exchange
                FROM symbols
                WHERE is_active = 1
                  AND (code LIKE CONCAT('%',:q,'%') OR name LIKE CONCAT('%',:q,'%'))
                ORDER BY market, code
                LIMIT :limit
                """,
                Map.of("q", query, "limit", limit)
        );
    }
}
