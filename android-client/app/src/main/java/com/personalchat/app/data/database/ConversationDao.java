package com.personalchat.app.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.personalchat.app.data.model.Conversation;

import java.util.List;

@Dao
public interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    LiveData<List<Conversation>> getAllConversations();

    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    List<Conversation> getAllConversationsSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Conversation conversation);

    @Update
    void update(Conversation conversation);

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    Conversation getConversationById(String id);

    @Query("UPDATE conversations SET connectionState = :state WHERE id = :id")
    void updateConnectionState(String id, String state);

    @Query("UPDATE conversations SET lastMessage = :lastMsg, lastMessageTimestamp = :timestamp WHERE id = :id")
    void updateLastMessage(String id, String lastMsg, long timestamp);

    @Query("DELETE FROM conversations WHERE id = :id")
    void deleteConversation(String id);
}
