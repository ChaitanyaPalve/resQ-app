package com.phoenix.phoenixnet.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Modern User Entity for local authentication and profile management.
 * Enforces offline-first storage for local verification.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String = "",
    
    @ColumnInfo(name = "email")
    val email: String = "",
    
    @ColumnInfo(name = "first_name")
    val firstName: String = "",
    
    @ColumnInfo(name = "middle_name")
    val middleName: String = "",
    
    @ColumnInfo(name = "last_name")
    val lastName: String = "",
    
    /**
     * SHA-256 hash of the user's secret trigger word.
     */
    @ColumnInfo(name = "trigger_word_hash")
    val triggerWordHash: String = "",
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
