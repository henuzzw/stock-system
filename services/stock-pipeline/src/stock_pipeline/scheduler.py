from __future__ import annotations

import time
from dataclasses import dataclass
from datetime import datetime, time as dt_time
from zoneinfo import ZoneInfo

from stock_pipeline.config import Settings
from stock_pipeline.ingest import DataIngestor


@dataclass(frozen=True)
class SessionWindow:
    timezone: str
    start: dt_time
    end: dt_time


CN_MORNING = SessionWindow("Asia/Shanghai", dt_time(9, 30), dt_time(11, 30))
CN_AFTERNOON = SessionWindow("Asia/Shanghai", dt_time(13, 0), dt_time(15, 0))
US_REGULAR = SessionWindow("America/New_York", dt_time(9, 30), dt_time(16, 0))


def _in_window(now: datetime, window: SessionWindow) -> bool:
    local = now.astimezone(ZoneInfo(window.timezone))
    if local.weekday() >= 5:
        return False
    return window.start <= local.time() <= window.end


def any_market_open(now: datetime | None = None) -> bool:
    now = now or datetime.now().astimezone()
    return any(_in_window(now, window) for window in (CN_MORNING, CN_AFTERNOON, US_REGULAR))


def run_loop(settings: Settings, poll_minutes: int | None = None) -> None:
    ingestor = DataIngestor(settings)
    interval = max(15, poll_minutes or settings.intraday_interval_minutes)
    last_cn_close_recap_day: str | None = None
    last_us_close_recap_day: str | None = None

    while True:
        now = datetime.now().astimezone()
        if any_market_open(now):
            ingestor.intraday_update()

        shanghai_now = now.astimezone(ZoneInfo("Asia/Shanghai"))
        cn_recap_key = shanghai_now.strftime("%Y-%m-%d")
        if shanghai_now.weekday() < 5 and shanghai_now.time() >= dt_time(15, 10) and last_cn_close_recap_day != cn_recap_key:
            ingestor.daily_close_recap()
            last_cn_close_recap_day = cn_recap_key

        new_york_now = now.astimezone(ZoneInfo("America/New_York"))
        us_recap_key = new_york_now.strftime("%Y-%m-%d")
        if new_york_now.weekday() < 5 and new_york_now.time() >= dt_time(16, 10) and last_us_close_recap_day != us_recap_key:
            ingestor.daily_close_recap()
            last_us_close_recap_day = us_recap_key

        time.sleep(interval * 60)
