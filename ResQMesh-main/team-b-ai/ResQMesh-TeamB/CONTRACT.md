# Team A ↔ Team B contract (LOCKED)

Team B is the AI brain. It does not talk to ESP32.
Team A owns live disaster data. Team B only computes.

## How they connect

```
Team A backend  --HTTP POST JSON-->  Team B AI :8000
Team A backend  <--JSON AI result--  Team B AI
Team C dashboard reads Team A  (or Team B /api/state if A is late)
```

Default URL (same laptop):

```
http://127.0.0.1:8000
```

Different laptops, same WiFi:

```
http://<TEAM_B_IPV4>:8000
```

Team B must be started with:

```
python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

---

## Endpoints Team A may call

| Method | Path | Who | Purpose |
|--------|------|-----|---------|
| GET | `/api/health` | A, C | Is AI online? |
| GET | `/api/state` | A, C | Last campus + latest AI result |
| POST | `/api/analyze` | **A (main)** | Send full/partial live data, get AI result |
| POST | `/api/block-exit` | demo / C | `{ "exit_id": "EXIT_B", "blocked": true }` |
| POST | `/api/reset` | demo | Restore `campus.json` |

**Main integration endpoint = `POST /api/analyze`**

---

## Minimum JSON Team A must send

First working test (enough for SIH integration):

```json
{
  "sos": [
    {
      "id": "SOS-1",
      "zone": "A",
      "type": "collapse",
      "label": "Collapse reported by mesh"
    }
  ],
  "exits": {
    "EXIT_B": { "blocked": true }
  }
}
```

`type` must be one of: `fire` | `collapse` | `minor`

`zone` must be one of: `A` | `B` | `C` | `HALL`

`exit` ids: `EXIT_A` | `EXIT_B` | `EXIT_C`

---

## Full JSON Team A should send (best)

```json
{
  "sos": [
    {
      "id": "SOS-1",
      "zone": "HALL",
      "type": "fire",
      "label": "Fire in hall"
    }
  ],
  "exits": {
    "EXIT_A": { "blocked": false, "crowd": 12, "capacity": 30 },
    "EXIT_B": { "blocked": false, "crowd": 70, "capacity": 40 },
    "EXIT_C": { "blocked": false, "crowd": 8, "capacity": 35 }
  },
  "zones": {
    "A": {
      "occupancy_before": 42,
      "confirmed_safe": 31,
      "rescued": 4,
      "damage": 0.78,
      "devices_last_seen": 18,
      "devices_alive": 12,
      "sensor_score": 0.72,
      "sos_count": 1,
      "node_failures": 0.2
    },
    "B": {
      "occupancy_before": 38,
      "confirmed_safe": 18,
      "rescued": 2,
      "damage": 0.92,
      "devices_last_seen": 16,
      "devices_alive": 5,
      "sensor_score": 0.95,
      "sos_count": 0,
      "node_failures": 0.6
    }
  },
  "nodes": {
    "node-4": { "online": false, "zone": "B" }
  }
}
```

### Field meaning (Team A fills these)

| Field | From | Notes |
|-------|------|--------|
| `sos[]` | mesh SOS packets | required for live SOS demo |
| `exits.*.blocked` | dashboard / sensor / button | required for dynamic route |
| `exits.*.crowd` | crowd estimate, or 0 | optional |
| `zones.*.occupancy_before` | known campus occupancy | needed for ZeroSignal |
| `zones.*.confirmed_safe` | check-in / rescue reports | needed for ZeroSignal |
| `zones.*.rescued` | rescue team reports | needed for ZeroSignal |
| `zones.*.devices_last_seen` | devices seen before disaster | needed for ZeroSignal |
| `zones.*.devices_alive` | devices still on mesh | needed for ZeroSignal |
| `zones.*.damage` | 0 to 1 | optional, default keep old |
| `zones.*.sensor_score` | 0 to 1 from MPU/smoke/etc | optional |
| `zones.*.node_failures` | 0 to 1 | optional |
| `nodes` | mesh heartbeat | optional; offline nodes raise `node_failures` |

If Team A omits a field, Team B keeps the value from `campus.json`.

Team A does **not** send routes, risk, or ZeroSignal. Team B calculates those.

---

## What Team B returns (always)

```json
{
  "ok": true,
  "headline": "No SOS received ≠ No victim",
  "risk_map": {
    "B": { "id": "B", "name": "Building B", "risk": 0.72, "level": "HIGH", "color": "#ff3b3b" }
  },
  "routes": {
    "B": { "from": "B", "exit": "EXIT_C", "path": ["B", "HALL", "EXIT_C"], "cost": 349.0 }
  },
  "zerosignal": {
    "total_unaccounted": 28,
    "headline": "No SOS received ≠ No victim",
    "zones": [
      {
        "id": "B",
        "name": "Building B",
        "probability": 68,
        "missing": 18,
        "had_sos": false,
        "reason": "18 people unaccounted, many devices went silent, high building damage, no SOS from this zone"
      }
    ]
  },
  "priority": [
    { "priority": 1, "kind": "SOS", "type": "fire", "title": "...", "zone": "HALL" }
  ],
  "zones": {},
  "exits": {},
  "sos": []
}
```

Team C / Team A should display:

- `risk_map[zone].level` + `color`
- `routes[zone].path`
- `zerosignal.zones` (purple missing-person zones)
- `priority` (rescue order)

---

## Team A Python example

```python
import requests

TEAM_B = "http://127.0.0.1:8000"

def send_to_ai(payload):
    r = requests.post(f"{TEAM_B}/api/analyze", json=payload, timeout=5)
    r.raise_for_status()
    return r.json()

ai = send_to_ai({
    "sos": [{"id": "SOS-1", "zone": "A", "type": "collapse", "label": "From mesh"}],
    "exits": {"EXIT_B": {"blocked": True}},
})
print(ai["routes"]["B"]["path"])
print(ai["zerosignal"]["zones"][0])
```

---

## Rules so A and B do not break each other

1. Zone ids stay `A`, `B`, `C`, `HALL`. Do not rename.
2. Exit ids stay `EXIT_A`, `EXIT_B`, `EXIT_C`.
3. Team A never overwrites Team B files (`ai_risk.py`, `zerosignal.py`, ...).
4. Team B never talks to ESP32 or the database.
5. If Team A is down, Team B still demos from `campus.json`.
6. If Team B is down, Team A still stores SOS; AI can be filled later.
