from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Settings:
    mysql_host: str
    mysql_port: int
    mysql_user: str
    mysql_password: str
    mysql_database: str
    watchlist_path: Path
    intraday_interval_minutes: int
    default_lookback_days: int


def load_settings() -> Settings:
    try:
        from dotenv import load_dotenv

        load_dotenv()
    except ImportError:
        pass

    return Settings(
        mysql_host=os.getenv("MYSQL_HOST", "127.0.0.1"),
        mysql_port=int(os.getenv("MYSQL_PORT", "3306")),
        mysql_user=os.getenv("MYSQL_USER", "flowing"),
        mysql_password=os.getenv("MYSQL_PASSWORD", "flowing"),
        mysql_database=os.getenv("MYSQL_DATABASE", "stock_pipeline"),
        watchlist_path=Path(
            os.getenv("WATCHLIST_PATH", "/home/openclaw/.openclaw/workspace/data/watchlist.csv")
        ),
        intraday_interval_minutes=int(os.getenv("INTRADAY_INTERVAL_MINUTES", "15")),
        default_lookback_days=int(os.getenv("DEFAULT_LOOKBACK_DAYS", "180")),
    )
