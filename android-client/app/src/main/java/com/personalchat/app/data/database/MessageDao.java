package com.personalchat.app.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.personalchat.app.data.model.Message;

import java.util.List;

@Dao
public interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    LiveData<List<Message>> getMessagesForConversation(String convId);

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    List<Message> getMessagesForConversationSync(String convId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Message message);

    @Update
    void update(Message message);

    @Query("UPDATE messages SET status = :status WHERE id = :msgId")
    void updateMessageStatus(String msgId, String status);

    @Query("SELECT * FROM messages WHERE status = 'SENDING' AND conversationId = :convId")
    List<Message> getUnsentMessages(String convId);

    @Query("SELECT * FROM messages WHERE id = :msgId LIMIT 1")
    Message getMessageById(String msgId);
}
