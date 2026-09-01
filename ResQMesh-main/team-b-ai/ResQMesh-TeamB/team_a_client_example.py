"""
Give this file to Team A.
They can copy the send_to_ai() function into their backend.
"""

import requests

TEAM_B = "http://127.0.0.1:8000"  # change to Team B laptop IP if needed


def send_to_ai(payload: dict) -> dict:
    response = requests.post(f"{TEAM_B}/api/analyze", json=payload, timeout=5)
    response.raise_for_status()
    return response.json()


if __name__ == "__main__":
    print("health:", requests.get(f"{TEAM_B}/api/health", timeout=5).json())

    result = send_to_ai(
        {
            "sos": [
                {
                    "id": "SOS-1",
                    "zone": "A",
                    "type": "collapse",
                    "label": "From Team A mesh",
                }
            ],
            "exits": {"EXIT_B": {"blocked": True}},
            "zones": {
                "B": {
                    "confirmed_safe": 18,
                    "rescued": 2,
                    "devices_alive": 5,
                }
            },
            "nodes": {
                "node-4": {"online": False, "zone": "B"},
                "node-5": {"online": True, "zone": "B"},
            },
        }
    )

    print("Building B route:", result["routes"]["B"]["path"])
    print("Top ZeroSignal:", result["zerosignal"]["zones"][0]["name"],
          result["zerosignal"]["zones"][0]["probability"], "%")
    print("First rescue:", result["priority"][0]["title"])
