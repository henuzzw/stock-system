from stock_research.candidates_sql import INIT_CANDIDATES_SQL

INIT_SQL = """
CREATE TABLE IF NOT EXISTS technical_low_daily (
    run_date DATE NOT NULL,
    symbol_id BIGINT NOT NULL,
    score DECIMAL(10, 4) NOT NULL,
    `rank` INT NOT NULL,
    reasons JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (run_date, symbol_id),
    KEY idx_technical_low_daily_run_date_rank (run_date, `rank`),
    CONSTRAINT fk_technical_low_daily_symbol FOREIGN KEY (symbol_id) REFERENCES symbols (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS valuation_low_daily (
    run_date DATE NOT NULL,
    symbol_id BIGINT NOT NULL,
    score DECIMAL(10, 4) NOT NULL,
    `rank` INT NOT NULL,
    reasons JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (run_date, symbol_id),
    KEY idx_valuation_low_daily_run_date_rank (run_date, `rank`),
    CONSTRAINT fk_valuation_low_daily_symbol FOREIGN KEY (symbol_id) REFERENCES symbols (id)
) ENGINE=InnoDB;
""" + INIT_CANDIDATES_SQL
