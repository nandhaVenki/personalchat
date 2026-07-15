package com.personalchat.app.ui.chats;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.personalchat.app.R;
import com.personalchat.app.data.model.Conversation;
import com.personalchat.app.data.model.PeerConnectionState;
import com.personalchat.app.data.model.UserProfile;
import com.personalchat.app.data.repository.ChatRepository;
import com.personalchat.app.data.repository.UserRepository;
import com.personalchat.app.network.SocketManager;
import com.personalchat.app.network.WebRtcManager;
import com.personalchat.app.ui.chat.ChatActivity;
import com.personalchat.app.ui.newchat.NewChatActivity;
import com.personalchat.app.ui.profile.ProfileActivity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class ChatsActivity extends AppCompatActivity implements SocketManager.SocketListener, WebRtcManager.WebRtcStateListener {
    private static final String TAG = "ChatsActivity";

    private ChatsViewModel viewModel;
    private ChatsAdapter adapter;
    private View statusDot;
    private TextView statusText;
    private LinearLayout emptyState;

    private SocketManager socketManager;
    private WebRtcManager webRtcManager;
    private UserRepository userRepository;
    private ChatRepository chatRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);

        userRepository = new UserRepository(this);
        chatRepository = new ChatRepository(this);
        socketManager = SocketManager.getInstance(this);
        webRtcManager = WebRtcManager.getInstance(this);

        // Initialize UI components
        statusDot = findViewById(R.id.signaling_status_dot);
        statusText = findViewById(R.id.signaling_status_text);
        emptyState = findViewById(R.id.empty_state);
        RecyclerView recyclerView = findViewById(R.id.recycler_conversations);
        FloatingActionButton fabNewChat = findViewById(R.id.fab_new_chat);
        ImageButton btnSettings = findViewById(R.id.btn_settings);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatsAdapter(conversation -> {
            Intent intent = new Intent(ChatsActivity.this, ChatActivity.class);
            intent.putExtra("conversation_id", conversation.getId());
            intent.putExtra("peer_name", conversation.getPeerName());
            intent.putExtra("peer_phone", conversation.getPeerPhone());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // Bind ViewModel
        viewModel = new ViewModelProvider(this).get(ChatsViewModel.class);
        viewModel.getConversations().observe(this, conversations -> {
            if (conversations == null || conversations.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.submitList(conversations);
            }
        });

        viewModel.getIsSignalingConnected().observe(this, connected -> {
            if (connected) {
                statusDot.setBackgroundResource(R.drawable.status_dot_green);
                statusText.setText("Online");
            } else {
                statusDot.setBackgroundResource(R.drawable.status_dot_red);
                statusText.setText("Offline");
            }
        });

        // Click listeners
        fabNewChat.setOnClickListener(v -> {
            Intent intent = new Intent(ChatsActivity.this, NewChatActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(ChatsActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Initialize network and signaling layers
        setupNetwork();
    }

    @Override
    protected void onResume() {
        super.onResume();
        socketManager.setListener(this);
        webRtcManager.setStateListener(this);
        viewModel.updateSignalingConnected(socketManager.isConnected());
        
        // Auto-negotiate WebRTC for conversations marked as CONNECTING or CONNECTED on startup
        tryConnectToAllPeers();
    }

    private void setupNetwork() {
        // Fetch customized signaling server URL from profile preferences
        SharedPreferences settingsPrefs = getSharedPreferences("profile_prefs", MODE_PRIVATE);
        String savedUrl = settingsPrefs.getString("signaling_url", "http://10.0.2.2:3000");

        socketManager.setListener(this);
        socketManager.connect(savedUrl);
    }

    private void tryConnectToAllPeers() {
        new Thread(() -> {
            for (Conversation conversation : chatRepository.getAllConversationsSync()) {
                // If we were previously communicating, trigger signaling request to reconnect
                if (socketManager.isConnected()) {
                    socketManager.sendConnectionRequest(conversation.getId());
                }
            }
        }).start();
    }

    // SocketManager.SocketListener callbacks
    @Override
    public void onConnected() {
        Log.d(TAG, "Signaling connected.");
        viewModel.updateSignalingConnected(true);
        // Automatically check presence and ping open chats
        tryConnectToAllPeers();
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "Signaling disconnected.");
        viewModel.updateSignalingConnected(false);
    }

    @Override
    public void onPeerOnline(String deviceId, String phoneHash) {
        Log.d(TAG, "Peer online: " + deviceId);
        // Try connecting WebRTC
        socketManager.sendConnectionRequest(deviceId);
    }

    @Override
    public void onPeerOffline(String deviceId, String phoneHash) {
        Log.d(TAG, "Peer offline: " + deviceId);
        webRtcManager.disconnectPeer(deviceId);
    }

    @Override
    public void onConnectionRequest(String senderDeviceId) {
        Log.d(TAG, "Incoming WebRTC connection request from: " + senderDeviceId);
        // We act as offerer upon request
        webRtcManager.initiateConnection(senderDeviceId);
    }

    @Override
    public void onWebRtcOffer(String senderDeviceId, String sdp) {
        webRtcManager.handleIncomingOffer(senderDeviceId, sdp);
    }

    @Override
    public void onWebRtcAnswer(String senderDeviceId, String sdp) {
        webRtcManager.handleIncomingAnswer(senderDeviceId, sdp);
    }

    @Override
    public void onIceCandidate(String senderDeviceId, JSONObject candidate) {
        webRtcManager.handleIncomingIceCandidate(senderDeviceId, candidate);
    }

    @Override
    public void onConnectionError(String senderDeviceId, String reason) {
        webRtcManager.handleConnectionError(senderDeviceId, reason);
    }

    // WebRtcManager.WebRtcStateListener callbacks
    @Override
    public void onConnectionStateChanged(String peerDeviceId, PeerConnectionState state) {
        // Redundant trigger, Room LiveData observes connection status updates
        Log.d(TAG, "P2P Connection state changed for " + peerDeviceId + " -> " + state.name());
    }
}
