package com.phoenix.phoenixnet.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE user_id = :userId LIMIT 1")
    fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun getActiveUser(): UserEntity?

    @Query("""
        SELECT * FROM users 
        WHERE first_name = :first 
        AND middle_name = :middle 
        AND last_name = :last 
        AND trigger_word_hash = :hash 
        LIMIT 1
    """)
    fun verifyUser(first: String, middle: String, last: String, hash: String): UserEntity?

    @Query("""
        SELECT * FROM users 
        WHERE first_name = :first 
        AND middle_name = :middle 
        AND last_name = :last 
        LIMIT 1
    """)
    fun getUserByNames(first: String, middle: String, last: String): UserEntity?

    @Query("DELETE FROM users")
    fun clearAllUsers()
}
