from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import mysql.connector
from mysql.connector import Error

app = FastAPI()

# Database Configuration
db_config = {
    'host': 'localhost',
    'user': 'root',  # Default XAMPP/WAMP user
    'password': '',  # Default password
    'database': 'pheonix_db'
}

class MeshPacket(BaseModel):
    uuid: str
    senderName: str
    timestamp: int
    lat: float
    lon: float
    payload: str
    priority: int

@app.post("/api/v1/mesh/sync")
async def sync_packets(packets: List[MeshPacket]):
    try:
        connection = mysql.connector.connect(**db_config)
        cursor = connection.cursor()

        insert_query = """
        INSERT IGNORE INTO mesh_packets (uuid, senderName, timestamp, lat, lon, payload, priority)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
        """

        data_to_insert = [
            (p.uuid, p.senderName, p.timestamp, p.lat, p.lon, p.payload, p.priority)
            for p in packets
        ]

        cursor.executemany(insert_query, data_to_insert)
        connection.commit()

        inserted_count = cursor.rowcount
        cursor.close()
        connection.close()

        return {"status": "success", "synced": len(packets), "new_entries": inserted_count}

    except Error as e:
        print(f"Error: {e}")
        raise HTTPException(status_code=500, detail="Database connection failed")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
