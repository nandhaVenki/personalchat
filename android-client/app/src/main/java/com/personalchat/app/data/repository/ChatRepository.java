package com.personalchat.app.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.personalchat.app.data.database.AppDatabase;
import com.personalchat.app.data.database.ConversationDao;
import com.personalchat.app.data.database.MessageDao;
import com.personalchat.app.data.model.Conversation;
import com.personalchat.app.data.model.DeliveryStatus;
import com.personalchat.app.data.model.Message;
import com.personalchat.app.data.model.PeerConnectionState;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatRepository {
    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final ExecutorService executorService;

    public ChatRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.conversationDao = db.conversationDao();
        this.messageDao = db.messageDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Conversation>> getAllConversations() {
        return conversationDao.getAllConversations();
    }

    public List<Conversation> getAllConversationsSync() {
        return conversationDao.getAllConversationsSync();
    }

    public LiveData<List<Message>> getMessagesForConversation(String convId) {
        return messageDao.getMessagesForConversation(convId);
    }

    public void insertConversation(Conversation conversation) {
        executorService.execute(() -> conversationDao.insert(conversation));
    }

    public Conversation getConversationByIdSync(String id) {
        return conversationDao.getConversationById(id);
    }

    public void insertMessage(Message message) {
        executorService.execute(() -> {
            messageDao.insert(message);
            // Update last message preview in the parent conversation
            conversationDao.updateLastMessage(message.getConversationId(), message.getContent(), message.getTimestamp());
        });
    }

    public void updateMessageStatus(String msgId, DeliveryStatus status) {
        executorService.execute(() -> messageDao.updateMessageStatus(msgId, status.name()));
    }

    public void updateConversationConnectionState(String convId, PeerConnectionState state) {
        executorService.execute(() -> conversationDao.updateConnectionState(convId, state.name()));
    }

    public List<Message> getUnsentMessagesSync(String convId) {
        return messageDao.getUnsentMessages(convId);
    }
    
    public Message getMessageByIdSync(String msgId) {
        return messageDao.getMessageById(msgId);
    }
}
