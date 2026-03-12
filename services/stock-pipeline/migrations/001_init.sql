CREATE DATABASE IF NOT EXISTS stock_pipeline CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE stock_pipeline;

CREATE TABLE IF NOT EXISTS symbols (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    market VARCHAR(16) NOT NULL,
    exchange VARCHAR(16) NULL,
    source VARCHAR(32) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_symbols_code_market (code, market)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS daily_prices (
    symbol_id BIGINT NOT NULL,
    trade_date DATE NOT NULL,
    open DECIMAL(18, 4) NULL,
    high DECIMAL(18, 4) NULL,
    low DECIMAL(18, 4) NULL,
    close DECIMAL(18, 4) NULL,
    adj_close DECIMAL(18, 4) NULL,
    volume BIGINT NULL,
    turnover DECIMAL(20, 4) NULL,
    amplitude DECIMAL(10, 4) NULL,
    pct_change DECIMAL(10, 4) NULL,
    change_amount DECIMAL(18, 4) NULL,
    source VARCHAR(32) NOT NULL,
    raw_payload JSON NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (symbol_id, trade_date),
    CONSTRAINT fk_daily_prices_symbol FOREIGN KEY (symbol_id) REFERENCES symbols (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fundamentals (
    symbol_id BIGINT NOT NULL,
    trade_date DATE NOT NULL,
    pe_ttm DECIMAL(18, 4) NULL,
    pb DECIMAL(18, 4) NULL,
    ps DECIMAL(18, 4) NULL,
    dividend_yield DECIMAL(18, 4) NULL,
    total_market_cap DECIMAL(20, 4) NULL,
    float_market_cap DECIMAL(20, 4) NULL,
    revenue DECIMAL(20, 4) NULL,
    net_profit DECIMAL(20, 4) NULL,
    roe DECIMAL(18, 4) NULL,
    debt_ratio DECIMAL(18, 4) NULL,
    source VARCHAR(32) NOT NULL,
    raw_payload JSON NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (symbol_id, trade_date),
    CONSTRAINT fk_fundamentals_symbol FOREIGN KEY (symbol_id) REFERENCES symbols (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS news (
    id BIGINT NOT NULL AUTO_INCREMENT,
    symbol_id BIGINT NOT NULL,
    published_at DATETIME NOT NULL,
    title VARCHAR(512) NOT NULL,
    url VARCHAR(1024) NULL,
    summary TEXT NULL,
    source VARCHAR(32) NOT NULL,
    raw_payload JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_news_symbol_time_title (symbol_id, published_at, title(191)),
    CONSTRAINT fk_news_symbol FOREIGN KEY (symbol_id) REFERENCES symbols (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS technicals (
    symbol_id BIGINT NOT NULL,
    trade_date DATE NOT NULL,
    sma_5 DECIMAL(18, 4) NULL,
    sma_10 DECIMAL(18, 4) NULL,
    sma_20 DECIMAL(18, 4) NULL,
    ema_12 DECIMAL(18, 4) NULL,
    ema_26 DECIMAL(18, 4) NULL,
    rsi_14 DECIMAL(18, 4) NULL,
    macd DECIMAL(18, 4) NULL,
    macd_signal DECIMAL(18, 4) NULL,
    macd_hist DECIMAL(18, 4) NULL,
    source VARCHAR(32) NOT NULL,
    raw_payload JSON NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (symbol_id, trade_date),
    CONSTRAINT fk_technicals_symbol FOREIGN KEY (symbol_id) REFERENCES symbols (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS snapshots (
    symbol_id BIGINT NOT NULL,
    snapshot_time DATETIME NOT NULL,
    price DECIMAL(18, 4) NULL,
    change_pct DECIMAL(18, 4) NULL,
    volume BIGINT NULL,
    amount DECIMAL(20, 4) NULL,
    bid DECIMAL(18, 4) NULL,
    ask DECIMAL(18, 4) NULL,
    source VARCHAR(32) NOT NULL,
    raw_payload JSON NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (symbol_id, snapshot_time),
    CONSTRAINT fk_snapshots_symbol FOREIGN KEY (symbol_id) REFERENCES symbols (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS minute_prices (
    symbol_id BIGINT NOT NULL,
    ts DATETIME NOT NULL,
    open DECIMAL(20, 6) NULL,
    high DECIMAL(20, 6) NULL,
    low DECIMAL(20, 6) NULL,
    close DECIMAL(20, 6) NULL,
    volume BIGINT NULL,
    amount DECIMAL(20, 4) NULL,
    avg_price DECIMAL(20, 6) NULL,
    source VARCHAR(32) NOT NULL,
    raw_payload JSON NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (symbol_id, ts),
    KEY idx_minute_prices_ts (ts),
    CONSTRAINT fk_minute_prices_symbol FOREIGN KEY (symbol_id) REFERENCES symbols (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ingestion_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    run_type VARCHAR(64) NOT NULL,
    symbol_id BIGINT NULL,
    source VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at DATETIME NOT NULL,
    finished_at DATETIME NULL,
    rows_written INT NOT NULL DEFAULT 0,
    message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ingestion_log_run_type_started_at (run_type, started_at),
    CONSTRAINT fk_ingestion_log_symbol FOREIGN KEY (symbol_id) REFERENCES symbols (id)
) ENGINE=InnoDB;
