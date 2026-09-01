"""
ResQMesh Team B — AI Command Center

Run:
  python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000

Open:
  http://127.0.0.1:8000

Team A main hook:
  POST /api/analyze
"""

from __future__ import annotations

import copy
import json
from pathlib import Path
from typing import Any, Optional

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from pydantic import BaseModel

from ai_risk import build_risk_map
from ai_route import all_routes
from priority import build_priority
from zerosignal import analyze as zerosignal_analyze

ROOT = Path(__file__).parent
ORIGINAL = json.loads((ROOT / "campus.json").read_text(encoding="utf-8"))
campus = copy.deepcopy(ORIGINAL)

app = FastAPI(title="ResQMesh Team B — AI Brain")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class BlockBody(BaseModel):
    exit_id: str
    blocked: bool


class SosItem(BaseModel):
    id: str
    zone: str
    type: str
    label: str = "SOS"


class AnalyzeBody(BaseModel):
    sos: Optional[list[SosItem]] = None
    exits: Optional[dict[str, dict[str, Any]]] = None
    zones: Optional[dict[str, dict[str, Any]]] = None
    nodes: Optional[dict[str, dict[str, Any]]] = None


def apply_team_a(body: AnalyzeBody) -> None:
    if body.sos is not None:
        campus["sos"] = [item.model_dump() for item in body.sos]
        counts: dict[str, int] = {}
        for item in campus["sos"]:
            counts[item["zone"]] = counts.get(item["zone"], 0) + 1
        for zone_id, zone in campus["zones"].items():
            zone["sos_count"] = counts.get(zone_id, 0)

    if body.exits:
        for exit_id, info in body.exits.items():
            if exit_id in campus["exits"]:
                campus["exits"][exit_id].update(info)

    if body.zones:
        for zone_id, info in body.zones.items():
            if zone_id in campus["zones"]:
                campus["zones"][zone_id].update(info)

    if body.nodes:
        failed: dict[str, int] = {}
        total: dict[str, int] = {}
        for node in body.nodes.values():
            zone_id = node.get("zone")
            if zone_id not in campus["zones"]:
                continue
            total[zone_id] = total.get(zone_id, 0) + 1
            if not node.get("online", True):
                failed[zone_id] = failed.get(zone_id, 0) + 1
        for zone_id, count in total.items():
            campus["zones"][zone_id]["node_failures"] = round(
                failed.get(zone_id, 0) / count, 2
            )


def snapshot() -> dict[str, Any]:
    risk_map = build_risk_map(campus)
    zerosignal = zerosignal_analyze(campus)
    routes = all_routes(campus, risk_map)
    priority = build_priority(campus, zerosignal)
    return {
        "ok": True,
        "zones": campus["zones"],
        "exits": campus["exits"],
        "sos": campus["sos"],
        "risk_map": risk_map,
        "routes": routes,
        "zerosignal": zerosignal,
        "priority": priority,
        "headline": "No SOS received ≠ No victim",
    }


@app.get("/")
def home():
    return FileResponse(ROOT / "dashboard.html")


@app.get("/api/health")
def health():
    return {"ok": True, "team": "B", "service": "resqmesh-ai"}


@app.get("/api/state")
def state():
    return snapshot()


@app.post("/api/analyze")
def analyze(body: AnalyzeBody):
    apply_team_a(body)
    return snapshot()


@app.post("/api/ingest")
def ingest(body: AnalyzeBody):
    """Same as /api/analyze. Kept so older Team A notes still work."""
    apply_team_a(body)
    return snapshot()



@app.post("/api/block-exit")
def block_exit(body: BlockBody):
    if body.exit_id in campus["exits"]:
        campus["exits"][body.exit_id]["blocked"] = body.blocked
    return snapshot()


@app.post("/api/reset")
def reset():
    global campus
    campus = copy.deepcopy(ORIGINAL)
    return snapshot()
