INIT_CANDIDATES_SQL = """
CREATE TABLE IF NOT EXISTS candidates_daily (
    run_date DATE NOT NULL,
    symbol_id BIGINT NOT NULL,
    in_technical_top20 TINYINT NOT NULL DEFAULT 0,
    in_valuation_top20 TINYINT NOT NULL DEFAULT 0,
    left_signal_score DECIMAL(10, 4) NOT NULL,
    right_signal_score DECIMAL(10, 4) NOT NULL,
    left_triggered TINYINT NOT NULL DEFAULT 0,
    right_triggered TINYINT NOT NULL DEFAULT 0,
    rank_left INT NULL,
    rank_right INT NULL,
    snapshot JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (run_date, symbol_id),
    KEY idx_candidates_daily_run_date_left (run_date, left_triggered, left_signal_score),
    KEY idx_candidates_daily_run_date_right (run_date, right_triggered, right_signal_score),
    KEY idx_candidates_daily_run_date_rank_left (run_date, rank_left),
    KEY idx_candidates_daily_run_date_rank_right (run_date, rank_right),
    CONSTRAINT fk_candidates_daily_symbol FOREIGN KEY (symbol_id) REFERENCES symbols (id)
) ENGINE=InnoDB;
"""
