-- Create Database
CREATE DATABASE IF NOT EXISTS pheonix_db;
USE pheonix_db;

-- Create Mesh Packets Table
CREATE TABLE IF NOT EXISTS mesh_packets (
    uuid VARCHAR(64) PRIMARY KEY,
    senderName VARCHAR(128),
    timestamp BIGINT,
    hopCount INT DEFAULT 0,
    lat DOUBLE,
    lon DOUBLE,
    payload TEXT,
    priority INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
