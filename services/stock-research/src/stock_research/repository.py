from __future__ import annotations

from stock_research.db import Database


A_SHARE_FILTER = """
(
    s.market IN ('CN', 'A', 'ASHARE', 'A_SHARE')
    OR s.code REGEXP '^(SH|SZ)?(60|68|00|30)[0-9]{4}$'
)
"""


class ResearchRepository:
    def __init__(self, db: Database) -> None:
        self.db = db

    def fetch_technical_source_rows(self) -> list[dict]:
        return self.db.fetchall(
            f"""
            SELECT
                s.id AS symbol_id,
                s.code,
                s.name,
                dp.trade_date,
                dp.close,
                t.sma_20
            FROM symbols s
            JOIN daily_prices dp ON dp.symbol_id = s.id
            LEFT JOIN technicals t
                ON t.symbol_id = dp.symbol_id
               AND t.trade_date = dp.trade_date
            WHERE s.is_active = 1
              AND {A_SHARE_FILTER}
            ORDER BY s.id, dp.trade_date
            """
        )

    def fetch_valuation_source_rows(self) -> list[dict]:
        return self.db.fetchall(
            f"""
            SELECT
                s.id AS symbol_id,
                s.code,
                s.name,
                f.trade_date,
                f.pe_ttm,
                f.pb,
                f.ps,
                f.dividend_yield
            FROM symbols s
            JOIN fundamentals f ON f.symbol_id = s.id
            WHERE s.is_active = 1
              AND {A_SHARE_FILTER}
            ORDER BY s.id, f.trade_date
            """
        )

    def fetch_ranked_rows(self, table: str, run_date: str) -> list[dict]:
        return self.db.fetchall(
            f"""
            SELECT
                r.run_date,
                r.symbol_id,
                s.code,
                s.name,
                r.score,
                r.`rank`,
                r.reasons,
                r.created_at
            FROM {table} r
            JOIN symbols s ON s.id = r.symbol_id
            WHERE r.run_date = %s
            ORDER BY r.`rank`
            LIMIT 20
            """,
            (run_date,),
        )

    def fetch_latest_technical_components(self) -> list[dict]:
        return self.db.fetchall(
            f"""
            WITH latest AS (
                SELECT symbol_id, MAX(trade_date) AS trade_date
                FROM daily_prices
                GROUP BY symbol_id
            ), recent_10 AS (
                SELECT dp.symbol_id,
                       MIN(dp.low) AS low_10,
                       MIN(dp.trade_date) AS min_dt,
                       MAX(dp.trade_date) AS max_dt
                FROM daily_prices dp
                JOIN latest l ON l.symbol_id = dp.symbol_id
                WHERE dp.trade_date >= DATE_SUB(l.trade_date, INTERVAL 20 DAY)
                GROUP BY dp.symbol_id
            ), recent_120 AS (
                SELECT dp.symbol_id,
                       MIN(dp.close) AS close_low_120,
                       MAX(dp.close) AS close_high_120
                FROM daily_prices dp
                JOIN latest l ON l.symbol_id = dp.symbol_id
                WHERE dp.trade_date >= DATE_SUB(l.trade_date, INTERVAL 200 DAY)
                GROUP BY dp.symbol_id
            )
            SELECT
                s.id AS symbol_id,
                s.code,
                s.name,
                dp.trade_date,
                dp.close,
                dp.low,
                t.sma_20 AS sma20,
                t.sma_10,
                t.sma_5,
                t.sma_20,
                t.sma_10,
                t.rsi_14,
                (CASE WHEN t.sma_20 IS NULL THEN NULL
                      WHEN t_prev.sma_20 IS NULL THEN NULL
                      WHEN t.sma_20 >= t_prev.sma_20 THEN 1 ELSE 0 END) AS sma20_up,
                (CASE WHEN r10.low_10 IS NULL THEN NULL
                      WHEN dp.low IS NULL THEN NULL
                      WHEN dp.low > r10.low_10 THEN 1 ELSE 0 END) AS no_new_low_10,
                (CASE WHEN r120.close_high_120 IS NULL OR r120.close_low_120 IS NULL OR r120.close_high_120 = r120.close_low_120 THEN NULL
                      ELSE ((dp.close - r120.close_low_120) / (r120.close_high_120 - r120.close_low_120)) * 100 END) AS price_percentile_120,
                (CASE WHEN r120.close_high_120 IS NULL OR r120.close_high_120 = 0 THEN NULL
                      ELSE ((r120.close_high_120 - dp.close) / r120.close_high_120) * 100 END) AS drawdown_120,
                (CASE WHEN dp.close IS NULL OR t.sma_20 IS NULL OR t.sma_20 = 0 THEN NULL
                      ELSE ((dp.close - t.sma_20) / t.sma_20) * 100 END) AS distance_to_sma20
            FROM symbols s
            JOIN latest l ON l.symbol_id = s.id
            JOIN daily_prices dp ON dp.symbol_id = s.id AND dp.trade_date = l.trade_date
            LEFT JOIN technicals t ON t.symbol_id = s.id AND t.trade_date = l.trade_date
            LEFT JOIN technicals t_prev ON t_prev.symbol_id = s.id AND t_prev.trade_date = DATE_SUB(l.trade_date, INTERVAL 1 DAY)
            LEFT JOIN recent_10 r10 ON r10.symbol_id = s.id
            LEFT JOIN recent_120 r120 ON r120.symbol_id = s.id
            WHERE s.is_active = 1 AND {A_SHARE_FILTER}
            """
        )

    def fetch_latest_valuation_components(self) -> list[dict]:
        return self.db.fetchall(
            f"""
            WITH latest AS (
                SELECT symbol_id, MAX(trade_date) AS trade_date
                FROM fundamentals
                GROUP BY symbol_id
            )
            SELECT
                s.id AS symbol_id,
                s.code,
                s.name,
                f.trade_date,
                f.pe_ttm,
                f.pb,
                f.ps,
                f.dividend_yield
            FROM symbols s
            JOIN latest l ON l.symbol_id = s.id
            JOIN fundamentals f ON f.symbol_id = s.id AND f.trade_date = l.trade_date
            WHERE s.is_active = 1 AND {A_SHARE_FILTER}
            """
        )
