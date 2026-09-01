"""
AI Disaster Commander — Safest route (not shortest).

edge_cost = distance + (risk * 200) + (crowd_penalty * 50)

If an exit is blocked, that exit is removed from the map
and the path is calculated again. This is the live demo:
Exit B blocked → system switches to Exit C.
"""

import networkx as nx


def _crowd_penalty(campus, node_id):
    exits = campus.get("exits", {})
    if node_id not in exits:
        return 0.0
    exit_info = exits[node_id]
    capacity = max(1, exit_info.get("capacity", 1))
    crowd = exit_info.get("crowd", 0)
    extra = max(0.0, crowd / capacity - 1.0)
    return extra


def build_graph(campus, risk_map):
    graph = nx.Graph()
    blocked = {
        exit_id
        for exit_id, exit_info in campus["exits"].items()
        if exit_info.get("blocked")
    }

    for node in campus["graph"]["nodes"]:
        if node not in blocked:
            graph.add_node(node)

    for edge in campus["graph"]["edges"]:
        start = edge["from"]
        end = edge["to"]
        if start in blocked or end in blocked:
            continue
        if start not in graph or end not in graph:
            continue

        risk_start = risk_map.get(start, {}).get("risk", 0)
        risk_end = risk_map.get(end, {}).get("risk", 0)
        risk = max(risk_start, risk_end)
        crowd = max(_crowd_penalty(campus, start), _crowd_penalty(campus, end))
        cost = edge["distance"] + (risk * 200) + (crowd * 50)

        graph.add_edge(
            start,
            end,
            weight=cost,
            distance=edge["distance"],
        )

    return graph


def safest_route(graph, start, exit_ids):
    best = None
    for exit_id in exit_ids:
        if exit_id not in graph or start not in graph:
            continue
        try:
            path = nx.shortest_path(graph, start, exit_id, weight="weight")
            cost = nx.shortest_path_length(graph, start, exit_id, weight="weight")
        except nx.NetworkXNoPath:
            continue
        if best is None or cost < best["cost"]:
            best = {
                "from": start,
                "exit": exit_id,
                "path": path,
                "cost": round(float(cost), 1),
            }
    return best


def all_routes(campus, risk_map):
    graph = build_graph(campus, risk_map)
    open_exits = [
        exit_id
        for exit_id, exit_info in campus["exits"].items()
        if not exit_info.get("blocked")
    ]
    starts = list(campus["zones"].keys())
    routes = {}
    for start in starts:
        routes[start] = safest_route(graph, start, open_exits)
    return routes
