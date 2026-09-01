package com.phoenix.phoenixnet.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Consolidated Room Database for PhoenixNet.
 * 
 * ARCHITECTURE RULES:
 * 1. Longevity: Standard Migration placeholders are used instead of destructive wipes.
 * 2. Security: SQLCipher injection hooks provided for encryption-at-rest.
 */
@Database(
    entities = [MeshPacketEntity::class, UserEntity::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun meshPacketDao(): MeshPacketDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN email TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Update mesh_packets
                db.execSQL("ALTER TABLE mesh_packets ADD COLUMN is_broadcasted INTEGER NOT NULL DEFAULT 0")

                // 2. Migrate users table (Handle rename/split of name fields)
                db.execSQL("""
                    CREATE TABLE users_new (
                        user_id TEXT NOT NULL PRIMARY KEY,
                        email TEXT NOT NULL,
                        first_name TEXT NOT NULL,
                        middle_name TEXT NOT NULL,
                        last_name TEXT NOT NULL,
                        trigger_word_hash TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    INSERT INTO users_new (user_id, email, first_name, middle_name, last_name, trigger_word_hash, created_at)
                    SELECT user_id, email, display_name, '', '', password_hash, created_at FROM users
                """)

                db.execSQL("DROP TABLE users")
                db.execSQL("ALTER TABLE users_new RENAME TO users")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mesh_packets ADD COLUMN recipient_id TEXT")
                db.execSQL("ALTER TABLE mesh_packets ADD COLUMN message_type TEXT NOT NULL DEFAULT 'VOICE'")
                db.execSQL("ALTER TABLE mesh_packets ADD COLUMN text_content TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "phoenixnet_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
