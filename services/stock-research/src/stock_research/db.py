from __future__ import annotations

import json
from contextlib import contextmanager
from datetime import date, datetime
from decimal import Decimal
from typing import Any, Iterable

from stock_research.config import Settings


def _normalize_value(value: Any) -> Any:
    if value is None:
        return None
    if isinstance(value, float):
        if value != value:
            return None
        return value
    if isinstance(value, Decimal):
        return value
    if isinstance(value, (dict, list)):
        return json.dumps(value, ensure_ascii=False, default=str)
    if isinstance(value, (date, datetime)):
        return value
    if hasattr(value, "item"):
        return value.item()
    try:
        if value != value:
            return None
    except Exception:
        pass
    return value


class Database:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    @contextmanager
    def connect(self, include_database: bool = True):
        import mysql.connector

        kwargs: dict[str, Any] = {
            "host": self.settings.mysql_host,
            "port": self.settings.mysql_port,
            "user": self.settings.mysql_user,
            "password": self.settings.mysql_password,
            "autocommit": False,
            "use_pure": True,
        }
        if include_database:
            kwargs["database"] = self.settings.mysql_database
        connection = mysql.connector.connect(**kwargs)
        try:
            yield connection
            connection.commit()
        except Exception:
            connection.rollback()
            raise
        finally:
            connection.close()

    def execute_script(self, sql: str, include_database: bool = True) -> None:
        with self.connect(include_database=include_database) as connection:
            cursor = connection.cursor()
            for statement in [part.strip() for part in sql.split(";") if part.strip()]:
                cursor.execute(statement)
            cursor.close()

    def fetchall(self, query: str, params: tuple[Any, ...] = ()) -> list[dict[str, Any]]:
        with self.connect() as connection:
            cursor = connection.cursor(dictionary=True)
            cursor.execute(query, params)
            rows = cursor.fetchall()
            cursor.close()
            return rows

    def fetchone(self, query: str, params: tuple[Any, ...] = ()) -> dict[str, Any] | None:
        rows = self.fetchall(query, params)
        return rows[0] if rows else None

    def execute(self, query: str, params: tuple[Any, ...] = ()) -> None:
        with self.connect() as connection:
            cursor = connection.cursor()
            cursor.execute(query, params)
            cursor.close()

    def upsert_rows(
        self,
        table: str,
        rows: Iterable[dict[str, Any]],
        key_columns: list[str],
        update_columns: list[str] | None = None,
    ) -> int:
        prepared = list(rows)
        if not prepared:
            return 0

        # Ensure stable column set across rows (some rows may omit optional cols)
        columns = sorted({key for row in prepared for key in row.keys()})
        update_columns = update_columns or [column for column in columns if column not in key_columns]
        placeholders = ", ".join(["%s"] * len(columns))
        update_sql = ", ".join(f"{column}=VALUES({column})" for column in update_columns)
        column_sql = ", ".join(f"`{column}`" for column in columns)
        update_sql = ", ".join(f"`{column}`=VALUES(`{column}`)" for column in update_columns)
        query = (
            f"INSERT INTO {table} ({column_sql}) VALUES ({placeholders}) "
            f"ON DUPLICATE KEY UPDATE {update_sql}"
        )
        values = [tuple(_normalize_value(row[column]) for column in columns) for row in prepared]

        with self.connect() as connection:
            cursor = connection.cursor()
            cursor.executemany(query, values)
            affected = cursor.rowcount
            cursor.close()
        return affected
