from __future__ import annotations

import json
from contextlib import contextmanager
from datetime import datetime
from decimal import Decimal
from typing import Any, Iterable
import logging

from stock_pipeline.config import Settings


logger = logging.getLogger(__name__)


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
    if hasattr(value, "item"):
        return value.item()
    if isinstance(value, datetime):
        return value.replace(tzinfo=None)
    try:
        if value != value:
            return None
    except Exception:
        pass
    return value


class Database:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._session_connection = None
        self._session_include_database = True
        self._session_depth = 0

    def _connect_once(self, include_database: bool = True):
        import mysql.connector

        kwargs = dict(
            host=self.settings.mysql_host,
            port=self.settings.mysql_port,
            user=self.settings.mysql_user,
            password=self.settings.mysql_password,
            autocommit=False,
        )
        if include_database:
            kwargs["database"] = self.settings.mysql_database
        return mysql.connector.connect(**kwargs, use_pure=True)

    @contextmanager
    def session(self, include_database: bool = True):
        if self._session_connection is not None:
            self._session_depth += 1
            try:
                yield self._session_connection
            finally:
                self._session_depth -= 1
            return

        connection = self._connect_once(include_database=include_database)
        self._session_connection = connection
        self._session_include_database = include_database
        self._session_depth = 1
        logger.info("Opened MySQL session for batch DB work")
        try:
            yield connection
        finally:
            try:
                connection.close()
            finally:
                self._session_connection = None
                self._session_include_database = True
                self._session_depth = 0
                logger.info("Closed MySQL session for batch DB work")

    @contextmanager
    def connect(self, include_database: bool = True):
        if self._session_connection is not None:
            if include_database and not self._session_include_database:
                raise RuntimeError("Active DB session was opened without selecting a database")
            try:
                self._session_connection.ping(reconnect=True, attempts=1, delay=0)
            except Exception:
                logger.warning("MySQL session ping failed; reconnecting batch session", exc_info=True)
                self._session_connection.reconnect(attempts=1, delay=0)
            try:
                yield self._session_connection
                self._session_connection.commit()
            except Exception:
                self._session_connection.rollback()
                raise
            return

        connection = self._connect_once(include_database=include_database)
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
            statements = [stmt.strip() for stmt in sql.split(";") if stmt.strip()]
            for stmt in statements:
                cursor.execute(stmt)
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

        columns = list(prepared[0].keys())
        update_columns = update_columns or [column for column in columns if column not in key_columns]
        placeholders = ", ".join(["%s"] * len(columns))
        column_sql = ", ".join(columns)
        update_sql = ", ".join([f"{column}=VALUES({column})" for column in update_columns])
        query = f"INSERT INTO {table} ({column_sql}) VALUES ({placeholders}) ON DUPLICATE KEY UPDATE {update_sql}"

        values = [tuple(_normalize_value(row[column]) for column in columns) for row in prepared]
        with self.connect() as connection:
            cursor = connection.cursor()
            cursor.executemany(query, values)
            affected = cursor.rowcount
            cursor.close()
        return affected

    def insert_ingestion_log(
        self,
        run_type: str,
        source: str,
        status: str,
        started_at: datetime,
        finished_at: datetime | None,
        rows_written: int,
        message: str | None,
        symbol_id: int | None = None,
    ) -> None:
        self.execute(
            """
            INSERT INTO ingestion_log
                (run_type, symbol_id, source, status, started_at, finished_at, rows_written, message)
            VALUES
                (%s, %s, %s, %s, %s, %s, %s, %s)
            """,
            (run_type, symbol_id, source, status, started_at, finished_at, rows_written, message),
        )
