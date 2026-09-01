"""
ZeroSignal AI — find possible victims even when nobody pressed SOS.

Line for judges:
"No SOS received does not mean no victim."
"AI generates probability-ranked possible missing-person zones
 from indirect evidence. It does not claim exact GPS of a person."

missing = occupancy_before - confirmed_safe - rescued

score =
  0.35 * missing_share
+ 0.25 * lost_device_ratio
+ 0.20 * damage
+ 0.10 * silent_zone (1 if no SOS from this zone)
+ 0.10 * last_known_device_density
"""

import math


def softmax(values):
    if not values:
        return []
    biggest = max(values)
    exps = [math.exp(v - biggest) for v in values]
    total = sum(exps) or 1.0
    return [e / total for e in exps]


def analyze(campus):
    zones = campus["zones"]
    sos_zones = {item["zone"] for item in campus.get("sos", [])}

    rows = []
    total_missing = 0

    for zone_id, zone in zones.items():
        missing = max(
            0,
            zone["occupancy_before"] - zone["confirmed_safe"] - zone["rescued"],
        )
        total_missing += missing
        last_seen = max(1, zone["devices_last_seen"])
        lost_ratio = max(0.0, (last_seen - zone["devices_alive"]) / last_seen)
        silent = 1.0 if zone_id not in sos_zones else 0.0
        density = min(1.0, zone["devices_last_seen"] / 20.0)

        rows.append(
            {
                "id": zone_id,
                "name": zone["name"],
                "missing": missing,
                "lost_ratio": lost_ratio,
                "damage": zone["damage"],
                "silent": silent,
                "density": density,
                "occupancy_before": zone["occupancy_before"],
                "confirmed_safe": zone["confirmed_safe"],
                "rescued": zone["rescued"],
                "devices_last_seen": zone["devices_last_seen"],
                "devices_alive": zone["devices_alive"],
                "had_sos": zone_id in sos_zones,
            }
        )

    missing_total = max(1, total_missing)
    raw_scores = []
    for row in rows:
        missing_share = row["missing"] / missing_total
        score = (
            0.35 * missing_share
            + 0.25 * row["lost_ratio"]
            + 0.20 * row["damage"]
            + 0.10 * row["silent"]
            + 0.10 * row["density"]
        )
        raw_scores.append(score)

    probs = softmax([s * 4 for s in raw_scores])

    results = []
    for row, score, prob in zip(rows, raw_scores, probs):
        results.append(
            {
                "id": row["id"],
                "name": row["name"],
                "missing": row["missing"],
                "probability": round(prob * 100),
                "score": round(score, 3),
                "had_sos": row["had_sos"],
                "occupancy_before": row["occupancy_before"],
                "confirmed_safe": row["confirmed_safe"],
                "rescued": row["rescued"],
                "devices_lost": row["devices_last_seen"] - row["devices_alive"],
                "reason": _reason(row),
            }
        )

    results.sort(key=lambda item: item["probability"], reverse=True)
    return {
        "total_unaccounted": total_missing,
        "zones": results,
        "headline": "No SOS received ≠ No victim",
    }


def _reason(row):
    bits = []
    if row["missing"] > 0:
        bits.append(f"{row['missing']} people unaccounted")
    if row["lost_ratio"] >= 0.3:
        bits.append("many devices went silent")
    if row["damage"] >= 0.6:
        bits.append("high building damage")
    if row["silent"]:
        bits.append("no SOS from this zone")
    if not bits:
        bits.append("low residual risk")
    return ", ".join(bits)
