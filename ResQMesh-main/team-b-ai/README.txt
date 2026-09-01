ResQMesh TEAM B — put this folder in Git as team-b-ai/

START
  python -m pip install -r requirements.txt
  python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000
  Chrome: http://127.0.0.1:8000

TEAM A CONNECTION
  Read CONTRACT.md
  Main URL: POST http://127.0.0.1:8000/api/analyze
  Example code: team_a_client_example.py

DEMO
  Click "Block Exit B" — route from Building B must change to EXIT_C
  Building B has no SOS but high ZeroSignal probability
