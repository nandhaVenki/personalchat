package com.personalchat.app.ui.chats;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.personalchat.app.data.model.Conversation;
import com.personalchat.app.data.repository.ChatRepository;
import com.personalchat.app.network.SocketManager;

import java.util.List;

public class ChatsViewModel extends AndroidViewModel {

    private final ChatRepository chatRepository;
    private final LiveData<List<Conversation>> conversations;
    private final MutableLiveData<Boolean> isSignalingConnected = new MutableLiveData<>(false);

    public ChatsViewModel(@NonNull Application application) {
        super(application);
        this.chatRepository = new ChatRepository(application);
        this.conversations = chatRepository.getAllConversations();
    }

    public LiveData<List<Conversation>> getConversations() {
        return conversations;
    }

    public LiveData<Boolean> getIsSignalingConnected() {
        return isSignalingConnected;
    }

    public void updateSignalingConnected(boolean connected) {
        isSignalingConnected.setValue(connected);
    }
}
