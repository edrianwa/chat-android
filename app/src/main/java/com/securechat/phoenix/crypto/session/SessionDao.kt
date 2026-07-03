package com.securechat.phoenix.crypto.session

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Room entity for Signal Protocol sessions.
 * Stores the serialized session state for the Double Ratchet.
 */
@Entity(tableName = "signal_sessions", primaryKeys = ["recipient_id", "device_id"])
data class SessionEntity(
    @ColumnInfo(name = "recipient_id") val recipientId: String,
    @ColumnInfo(name = "device_id") val deviceId: Int,
    val record: String // Base64-encoded serialized SessionRecord
)

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("SELECT * FROM signal_sessions WHERE recipient_id = :recipientId AND device_id = :deviceId")
    suspend fun getSession(recipientId: String, deviceId: Int): SessionEntity?

    @Query("SELECT device_id FROM signal_sessions WHERE recipient_id = :recipientId")
    suspend fun getSubDeviceSessions(recipientId: String): List<Int>

    @Query("DELETE FROM signal_sessions WHERE recipient_id = :recipientId AND device_id = :deviceId")
    suspend fun deleteSession(recipientId: String, deviceId: Int)

    @Query("DELETE FROM signal_sessions WHERE recipient_id = :recipientId")
    suspend fun deleteAllSessions(recipientId: String)
}
