package com.securechat.phoenix.chat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY timestamp ASC")
    fun getMessages(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE message_id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("UPDATE messages SET status = :status WHERE message_id = :messageId")
    suspend fun updateStatus(messageId: String, status: MessageStatus)

    @Query("UPDATE messages SET status = :status WHERE message_id IN (:messageIds)")
    suspend fun updateStatusBatch(messageIds: List<String>, status: MessageStatus)

    /**
     * Get distinct chats with the latest message for each.
     */
    @Query("""
        SELECT * FROM messages 
        WHERE id IN (SELECT MAX(id) FROM messages GROUP BY chat_id)
        ORDER BY timestamp DESC
    """)
    fun getConversations(): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE chat_id = :chatId AND is_outgoing = 0 AND status != 'READ'")
    fun getUnreadCount(chatId: String): Flow<Int>

    // --- Pending messages (offline queue) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPending(message: PendingMessageEntity)

    @Query("SELECT * FROM pending_messages ORDER BY timestamp ASC")
    suspend fun getPendingMessages(): List<PendingMessageEntity>

    @Query("DELETE FROM pending_messages WHERE message_id = :messageId")
    suspend fun removePending(messageId: String)

    @Query("SELECT COUNT(*) FROM pending_messages")
    suspend fun getPendingCount(): Int
}
