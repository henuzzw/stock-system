from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    mysql_host: str
    mysql_port: int
    mysql_user: str
    mysql_password: str
    mysql_database: str


def load_settings() -> Settings:
    try:
        from dotenv import load_dotenv

        load_dotenv()
    except ImportError:
        pass

    return Settings(
        mysql_host=os.getenv("MYSQL_HOST", "127.0.0.1"),
        mysql_port=int(os.getenv("MYSQL_PORT", "3310")),
        mysql_user=os.getenv("MYSQL_USER", "flowing"),
        mysql_password=os.getenv("MYSQL_PASSWORD", "flowing"),
        mysql_database=os.getenv("MYSQL_DATABASE", "stock_pipeline"),
    )
