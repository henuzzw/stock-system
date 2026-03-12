from __future__ import annotations

import csv
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class WatchlistSymbol:
    code: str
    name: str
    market: str
    exchange: str | None
    source: str


def classify_symbol(code: str) -> tuple[str, str | None]:
    normalized = code.strip().upper()
    if normalized.isdigit():
        if normalized.startswith(("430", "830", "831", "832", "833", "834", "835", "836", "837", "838", "839", "870", "871", "872", "873", "874", "875", "876", "877", "878", "879")):
            return "CN", "BJ"
        if normalized.startswith(("600", "601", "603", "605", "688", "689")):
            return "CN", "SH"
        return "CN", "SZ"
    return "US", None


def load_watchlist(path: Path) -> list[WatchlistSymbol]:
    items: list[WatchlistSymbol] = []
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            code = (row.get("code") or "").strip()
            name = (row.get("name") or code).strip()
            if not code:
                continue
            market, exchange = classify_symbol(code)
            items.append(
                WatchlistSymbol(
                    code=code.upper() if market == "US" else code.zfill(6),
                    name=name,
                    market=market,
                    exchange=exchange,
                    source="watchlist.csv",
                )
            )
    return items
