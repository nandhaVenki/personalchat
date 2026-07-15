package com.personalchat.app.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.personalchat.app.data.model.Conversation;
import com.personalchat.app.data.model.DeliveryStatus;
import com.personalchat.app.data.model.Message;
import com.personalchat.app.data.model.PeerConnectionState;
import com.personalchat.app.data.model.UserProfile;
import com.personalchat.app.data.repository.ChatRepository;
import com.personalchat.app.data.repository.UserRepository;
import com.personalchat.app.network.WebRtcManager;

import java.util.List;
import java.util.UUID;

public class ChatViewModel extends AndroidViewModel {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final WebRtcManager webRtcManager;
    private LiveData<List<Message>> messages;
    private final MutableLiveData<PeerConnectionState> peerConnectionState = new MutableLiveData<>(PeerConnectionState.UNAVAILABLE);
    private String conversationId;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        this.chatRepository = new ChatRepository(application);
        this.userRepository = new UserRepository(application);
        this.webRtcManager = WebRtcManager.getInstance(application);
    }

    public void init(String conversationId) {
        this.conversationId = conversationId;
        this.messages = chatRepository.getMessagesForConversation(conversationId);
        
        // Load initial state
        new Thread(() -> {
            Conversation conv = chatRepository.getConversationByIdSync(conversationId);
            if (conv != null) {
                PeerConnectionState state = PeerConnectionState.valueOf(conv.getConnectionState());
                peerConnectionState.postValue(state);
            }
        }).start();
    }

    public LiveData<List<Message>> getMessages() {
        return messages;
    }

    public LiveData<PeerConnectionState> getPeerConnectionState() {
        return peerConnectionState;
    }

    public void updateConnectionState(PeerConnectionState state) {
        peerConnectionState.setValue(state);
    }

    public void sendMessage(String content) {
        if (conversationId == null || content.trim().isEmpty()) return;

        UserProfile profile = userRepository.getProfile();
        if (profile == null) return;

        String messageId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        // 1. Create message locally in SENDING state
        Message message = new Message(
                messageId,
                conversationId,
                profile.getDeviceId(),
                conversationId,
                content,
                now,
                DeliveryStatus.SENDING.name(),
                false
        );

        // Save locally
        chatRepository.insertMessage(message);

        // 2. Attempt WebRTC direct transmission
        new Thread(() -> {
            boolean sent = webRtcManager.sendMessage(conversationId, message);
            if (sent) {
                // Sent successfully over wire (WebRtcManager updates database status to SENT)
                // Note: The UI updates automatically via Room LiveData
            } else {
                // Connection offline, stays in SENDING in database. Will auto-retry on reconnect.
            }
        }).start();
    }
}
