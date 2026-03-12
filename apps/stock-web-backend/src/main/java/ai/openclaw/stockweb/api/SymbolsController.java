package ai.openclaw.stockweb.api;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class SymbolsController {
    private final NamedParameterJdbcTemplate jdbc;
    private final SymbolInitializationService symbolInitializationService;

    public SymbolsController(
            NamedParameterJdbcTemplate jdbc,
            SymbolInitializationService symbolInitializationService
    ) {
        this.jdbc = jdbc;
        this.symbolInitializationService = symbolInitializationService;
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

        String fuzzy = buildOrderedLikePattern(query);
        return jdbc.queryForList(
                """
                SELECT code, name, market, exchange
                FROM symbols
                WHERE is_active = 1
                  AND (
                    code LIKE CONCAT('%',:q,'%')
                    OR name LIKE CONCAT('%',:q,'%')
                    OR name LIKE :fuzzy
                  )
                ORDER BY
                  CASE
                    WHEN code = :q THEN 0
                    WHEN name = :q THEN 1
                    WHEN name LIKE CONCAT('%',:q,'%') THEN 2
                    WHEN name LIKE :fuzzy THEN 3
                    ELSE 4
                  END,
                  market, code
                LIMIT :limit
                """,
                Map.of("q", query, "fuzzy", fuzzy, "limit", limit)
        );
    }

    @PostMapping("/symbols")
    public ResponseEntity<?> addSymbol(@RequestBody SymbolCreateRequest request) {
        String code = normalizeCode(request.code());
        String name = request.name() == null ? "" : request.name().trim();
        String market = request.market() == null ? "CN" : request.market().trim().toUpperCase();
        String exchange = request.exchange() == null ? null : request.exchange().trim().toUpperCase();
        String source = request.source() == null || request.source().isBlank() ? "manual" : request.source().trim();
        boolean initializeData = request.initializeData() != null && request.initializeData();

        if (code.isBlank() || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "code and name are required"
            ));
        }

        List<Map<String, Object>> existing = jdbc.queryForList(
                "SELECT id, code, name, market, exchange, source, is_active FROM symbols WHERE code = :code LIMIT 1",
                Map.of("code", code)
        );
        if (!existing.isEmpty()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("created", false);
            body.put("message", "Symbol already exists");
            body.put("symbol", existing.get(0));
            if (initializeData) {
                body.put("initialization", symbolInitializationService.initialize(code, market));
            }
            return ResponseEntity.ok(body);
        }

        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(
                    """
                    INSERT INTO symbols (code, name, market, exchange, source, is_active)
                    VALUES (:code, :name, :market, :exchange, :source, 1)
                    """,
                    new MapSqlParameterSource()
                            .addValue("code", code)
                            .addValue("name", name)
                            .addValue("market", market)
                            .addValue("exchange", exchange)
                            .addValue("source", source),
                    keyHolder,
                    new String[]{"id"}
            );
            Number key = keyHolder.getKey();
            Map<String, Object> symbol = new LinkedHashMap<>();
            symbol.put("id", key == null ? null : key.longValue());
            symbol.put("code", code);
            symbol.put("name", name);
            symbol.put("market", market);
            symbol.put("exchange", exchange);
            symbol.put("source", source);
            symbol.put("is_active", 1);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("created", true);
            body.put("message", initializeData ? "Symbol created and initialization started" : "Symbol created");
            body.put("symbol", symbol);
            if (initializeData) {
                body.put("initialization", symbolInitializationService.initialize(code, market));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (DuplicateKeyException ex) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "created", false,
                    "message", "Symbol already exists"
            ));
        }
    }

    private String buildOrderedLikePattern(String query) {
        StringBuilder pattern = new StringBuilder("%");
        query.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .filter(ch -> !ch.isBlank())
                .forEach(ch -> pattern.append(ch).append('%'));
        return pattern.toString();
    }

    private String normalizeCode(String raw) {
        String code = raw == null ? "" : raw.trim();
        if (code.matches("\\d+")) {
            return String.format("%06d", Integer.parseInt(code));
        }
        return code.toUpperCase();
    }

    public record SymbolCreateRequest(
            String code,
            String name,
            String market,
            String exchange,
            String source,
            Boolean initializeData
    ) {
    }
}
