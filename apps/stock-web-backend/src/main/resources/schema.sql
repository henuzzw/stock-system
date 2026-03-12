CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    initial_cash DECIMAL(18, 2) NOT NULL,
    cash_balance DECIMAL(18, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_accounts_user_id UNIQUE (user_id),
    CONSTRAINT fk_accounts_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS positions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    symbol_id BIGINT NOT NULL,
    quantity DECIMAL(18, 4) NOT NULL DEFAULT 0,
    available_quantity DECIMAL(18, 4) NOT NULL DEFAULT 0,
    avg_cost DECIMAL(18, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_positions_user_symbol UNIQUE (user_id, symbol_id),
    CONSTRAINT fk_positions_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_positions_symbol_id FOREIGN KEY (symbol_id) REFERENCES symbols (id)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    symbol_id BIGINT NOT NULL,
    side VARCHAR(8) NOT NULL,
    order_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    requested_quantity DECIMAL(18, 4) NOT NULL,
    filled_quantity DECIMAL(18, 4) NOT NULL DEFAULT 0,
    limit_price DECIMAL(18, 4) NULL,
    avg_fill_price DECIMAL(18, 4) NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'manual',
    note VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_symbol_id FOREIGN KEY (symbol_id) REFERENCES symbols (id)
);

CREATE TABLE IF NOT EXISTS trades (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    symbol_id BIGINT NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity DECIMAL(18, 4) NOT NULL,
    price DECIMAL(18, 4) NOT NULL,
    amount DECIMAL(18, 4) NOT NULL,
    fee DECIMAL(18, 4) NOT NULL DEFAULT 0,
    executed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trades_order_id FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_trades_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_trades_symbol_id FOREIGN KEY (symbol_id) REFERENCES symbols (id)
);

CREATE TABLE IF NOT EXISTS strategy_runs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    strategy_key VARCHAR(64) NOT NULL,
    run_date DATE NOT NULL,
    stage VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    summary VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_strategy_runs_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_strategy_runs_user_date (user_id, run_date, id)
);

CREATE TABLE IF NOT EXISTS daily_plan (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    strategy_run_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    symbol_id BIGINT NOT NULL,
    run_date DATE NOT NULL,
    plan_type VARCHAR(16) NOT NULL,
    pool_name VARCHAR(16) NULL,
    rank_value INT NULL,
    total_score DECIMAL(18, 4) NULL,
    trend_ok TINYINT NULL,
    target_weight DECIMAL(18, 8) NULL,
    target_amount DECIMAL(18, 4) NULL,
    action_reason VARCHAR(500) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_daily_plan_strategy_run_id FOREIGN KEY (strategy_run_id) REFERENCES strategy_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_daily_plan_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_daily_plan_symbol_id FOREIGN KEY (symbol_id) REFERENCES symbols (id),
    INDEX idx_daily_plan_user_date (user_id, run_date, id)
);
