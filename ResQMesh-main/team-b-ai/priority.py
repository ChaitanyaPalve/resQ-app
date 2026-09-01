"""
Rescue priority list for the command center.

Order:
1. Fire SOS
2. Building collapse SOS
3. High ZeroSignal missing-person zone
4. Minor injury SOS
"""

TYPE_RANK = {
    "fire": 1,
    "collapse": 2,
    "zerosignal": 3,
    "minor": 4,
}


def build_priority(campus, zerosignal):
    items = []

    for sos in campus.get("sos", []):
        items.append(
            {
                "id": sos["id"],
                "kind": "SOS",
                "type": sos["type"],
                "title": sos["label"],
                "zone": sos["zone"],
                "rank": TYPE_RANK.get(sos["type"], 9),
            }
        )

    top = None
    for zone in zerosignal.get("zones", []):
        if zone["missing"] > 0 and (top is None or zone["probability"] > top["probability"]):
            top = zone

    if top:
        items.append(
            {
                "id": "ZS-" + top["id"],
                "kind": "ZeroSignal",
                "type": "zerosignal",
                "title": f"{top['name']} — {top['probability']}% possible missing-person zone ({top['missing']} unaccounted)",
                "zone": top["id"],
                "rank": TYPE_RANK["zerosignal"],
            }
        )

    items.sort(key=lambda item: item["rank"])
    for index, item in enumerate(items, start=1):
        item["priority"] = index
    return items
