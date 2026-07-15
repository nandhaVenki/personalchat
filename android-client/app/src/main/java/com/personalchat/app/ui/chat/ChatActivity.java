package com.personalchat.app.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.personalchat.app.R;
import com.personalchat.app.data.model.PeerConnectionState;
import com.personalchat.app.network.SocketManager;
import com.personalchat.app.network.WebRtcManager;

import org.json.JSONObject;

public class ChatActivity extends AppCompatActivity implements SocketManager.SocketListener, WebRtcManager.WebRtcStateListener {
    private static final String TAG = "ChatActivity";

    private ChatViewModel viewModel;
    private MessageAdapter adapter;
    
    private View connectionDot;
    private TextView tvConnectionState;
    private TextView tvWarningBanner;
    private EditText inputMessage;
    private RecyclerView recyclerView;

    private String conversationId;
    private SocketManager socketManager;
    private WebRtcManager webRtcManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        conversationId = getIntent().getStringExtra("conversation_id");
        String peerName = getIntent().getStringExtra("peer_name");
        String peerPhone = getIntent().getStringExtra("peer_phone");

        socketManager = SocketManager.getInstance(this);
        webRtcManager = WebRtcManager.getInstance(this);

        // Bind views
        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView tvPeerName = findViewById(R.id.tv_peer_name);
        TextView tvPeerPhone = findViewById(R.id.tv_peer_number);
        connectionDot = findViewById(R.id.connection_indicator_dot);
        tvConnectionState = findViewById(R.id.tv_connection_state);
        tvWarningBanner = findViewById(R.id.tv_warning_banner);
        recyclerView = findViewById(R.id.recycler_messages);
        inputMessage = findViewById(R.id.input_message);
        ImageButton btnSend = findViewById(R.id.btn_send);

        tvPeerName.setText(peerName);
        tvPeerPhone.setText(peerPhone);

        btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new MessageAdapter();
        recyclerView.setAdapter(adapter);

        // Bind ViewModel
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        viewModel.init(conversationId);

        viewModel.getMessages().observe(this, messages -> {
            adapter.submitList(messages, () -> {
                if (messages != null && !messages.isEmpty()) {
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            });
        });

        viewModel.getPeerConnectionState().observe(this, this::updateConnectionUi);

        // Send listener
        btnSend.setOnClickListener(v -> {
            String text = inputMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                viewModel.sendMessage(text);
                inputMessage.setText("");
            }
        });

        // Trigger connection check / initiation
        triggerConnectionCheck();
    }

    @Override
    protected void onResume() {
        super.onResume();
        socketManager.addListener(this);
        webRtcManager.addStateListener(this);
        
        // Refresh connection state in VM
        viewModel.updateConnectionState(webRtcManager.getConnectionState(conversationId));
    }

    @Override
    protected void onPause() {
        super.onPause();
        socketManager.removeListener(this);
        webRtcManager.removeStateListener(this);
    }

    private void triggerConnectionCheck() {
        // If offline/unavailable, request connection over signaling server
        PeerConnectionState currentState = webRtcManager.getConnectionState(conversationId);
        if (currentState != PeerConnectionState.CONNECTED && socketManager.isConnected()) {
            viewModel.updateConnectionState(PeerConnectionState.CONNECTING);
            socketManager.sendConnectionRequest(conversationId);
        }
    }

    private void updateConnectionUi(PeerConnectionState state) {
        if (state == PeerConnectionState.CONNECTED) {
            connectionDot.setBackgroundResource(R.drawable.status_dot_green);
            tvConnectionState.setText("Direct Connection established");
            tvWarningBanner.setVisibility(View.GONE);
        } else if (state == PeerConnectionState.CONNECTING) {
            connectionDot.setBackgroundResource(R.drawable.status_dot_yellow);
            tvConnectionState.setText("Connecting...");
            tvWarningBanner.setVisibility(View.GONE);
        } else {
            connectionDot.setBackgroundResource(R.drawable.status_dot_red);
            tvConnectionState.setText("Unavailable");
            tvWarningBanner.setVisibility(View.VISIBLE);
        }
    }

    // SocketManager.SocketListener delegates
    @Override
    public void onConnected() {
        triggerConnectionCheck();
    }

    @Override
    public void onDisconnected() {
        viewModel.updateConnectionState(PeerConnectionState.UNAVAILABLE);
    }

    // WebRtcManager.WebRtcStateListener delegate
    @Override
    public void onConnectionStateChanged(String peerDeviceId, PeerConnectionState state) {
        if (conversationId.equals(peerDeviceId)) {
            viewModel.updateConnectionState(state);
        }
    }
}
