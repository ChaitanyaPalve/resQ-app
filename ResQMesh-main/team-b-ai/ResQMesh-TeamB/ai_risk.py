"""
AI Disaster Commander — Risk Map
Simple formula (easy to explain to judges):

risk = 0.4*sensor + 0.3*damage + 0.2*sos + 0.1*node_failures

HIGH    >= 0.70  red
MEDIUM  >= 0.40  orange
LOW     <  0.40  green
"""


def zone_risk(zone):
    sos_score = min(1.0, zone.get("sos_count", 0) / 3.0)
    risk = (
        0.4 * zone.get("sensor_score", 0)
        + 0.3 * zone.get("damage", 0)
        + 0.2 * sos_score
        + 0.1 * zone.get("node_failures", 0)
    )
    risk = round(min(1.0, max(0.0, risk)), 2)

    if risk >= 0.70:
        level = "HIGH"
        color = "#ff3b3b"
    elif risk >= 0.40:
        level = "MEDIUM"
        color = "#ff9f1c"
    else:
        level = "LOW"
        color = "#22c55e"

    return {"risk": risk, "level": level, "color": color}


def build_risk_map(campus):
    result = {}
    for zone_id, zone in campus["zones"].items():
        info = zone_risk(zone)
        info["id"] = zone_id
        info["name"] = zone["name"]
        result[zone_id] = info
    return result
