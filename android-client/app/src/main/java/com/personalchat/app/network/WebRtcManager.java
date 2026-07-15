package com.personalchat.app.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.personalchat.app.data.model.Conversation;
import com.personalchat.app.data.model.DeliveryStatus;
import com.personalchat.app.data.model.Message;
import com.personalchat.app.data.model.PeerConnectionState;
import com.personalchat.app.data.model.UserProfile;
import com.personalchat.app.data.repository.ChatRepository;
import com.personalchat.app.data.repository.UserRepository;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebRtcManager implements SocketManager.SocketListener {
    private static final String TAG = "WebRtcManager";

    private static WebRtcManager instance;
    private final Context context;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final Handler mainHandler;
    private final ExecutorService dbExecutor;

    private PeerConnectionFactory factory;
    private final Map<String, PeerConnection> peerConnections = new ConcurrentHashMap<>();
    private final Map<String, DataChannel> dataChannels = new ConcurrentHashMap<>();
    private final Map<String, PeerConnectionState> connectionStates = new ConcurrentHashMap<>();

    private List<PeerConnection.IceServer> iceServersList = new ArrayList<>();
    private final List<WebRtcStateListener> stateListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public interface WebRtcStateListener {
        void onConnectionStateChanged(String peerDeviceId, PeerConnectionState state);
    }

    public static synchronized WebRtcManager getInstance(Context context) {
        if (instance == null) {
            instance = new WebRtcManager(context);
        }
        return instance;
    }

    private WebRtcManager(Context context) {
        this.context = context.getApplicationContext();
        this.userRepository = new UserRepository(context);
        this.chatRepository = new ChatRepository(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.dbExecutor = Executors.newSingleThreadExecutor();

        initializeWebRtc();
        SocketManager.getInstance(context).addListener(this);
    }

    public void addStateListener(WebRtcStateListener listener) {
        if (listener != null && !stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
    }

    public void removeStateListener(WebRtcStateListener listener) {
        stateListeners.remove(listener);
    }

    private void initializeWebRtc() {
        try {
            PeerConnectionFactory.InitializationOptions initializationOptions =
                    PeerConnectionFactory.InitializationOptions.builder(context)
                            .createInitializationOptions();
            PeerConnectionFactory.initialize(initializationOptions);

            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            factory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .createPeerConnectionFactory();

            // Default fallback STUN server
            iceServersList.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
            Log.d(TAG, "WebRTC Factory initialized successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize WebRTC", e);
        }
    }

    public void setIceServers(List<String> stunServers) {
        if (stunServers == null || stunServers.isEmpty()) return;
        List<PeerConnection.IceServer> newServers = new ArrayList<>();
        for (String url : stunServers) {
            newServers.add(PeerConnection.IceServer.builder(url).createIceServer());
        }
        this.iceServersList = newServers;
        Log.d(TAG, "WebRTC configured with " + stunServers.size() + " STUN servers.");
    }

    public PeerConnectionState getConnectionState(String peerDeviceId) {
        PeerConnectionState state = connectionStates.get(peerDeviceId);
        return state != null ? state : PeerConnectionState.UNAVAILABLE;
    }

    private void updateConnectionState(String peerDeviceId, PeerConnectionState state) {
        connectionStates.put(peerDeviceId, state);
        chatRepository.updateConversationConnectionState(peerDeviceId, state);
        mainHandler.post(() -> {
            for (WebRtcStateListener listener : stateListeners) {
                listener.onConnectionStateChanged(peerDeviceId, state);
            }
        });
    }

    // Initiate WebRTC connection (Offerer role)
    public void initiateConnection(String peerDeviceId) {
        Log.d(TAG, "Initiating WebRTC connection to peer: " + peerDeviceId);
        updateConnectionState(peerDeviceId, PeerConnectionState.CONNECTING);
        
        PeerConnection pc = getOrCreatePeerConnection(peerDeviceId);
        if (pc == null) {
            updateConnectionState(peerDeviceId, PeerConnectionState.UNAVAILABLE);
            return;
        }

        // Create the DataChannel for messaging (Negotiated = false implies default protocol handshake)
        DataChannel.Init init = new DataChannel.Init();
        init.ordered = true;
        
        DataChannel dc = pc.createDataChannel("chat_channel", init);
        setupDataChannel(peerDeviceId, dc);

        // Generate SDP Offer
        MediaConstraints constraints = new MediaConstraints();
        pc.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                pc.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "Set local offer SDP success for " + peerDeviceId);
                        // Relay SDP Offer over signaling socket
                        SocketManager.getInstance(context).sendWebRtcOffer(peerDeviceId, sessionDescription.description);
                    }
                }, sessionDescription);
            }

            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "Failed to create offer: " + s);
                updateConnectionState(peerDeviceId, PeerConnectionState.UNAVAILABLE);
            }
        }, constraints);
    }

    // Receive incoming offer and return answer (Answerer role)
    public void handleIncomingOffer(String peerDeviceId, String sdpOffer) {
        Log.d(TAG, "Received incoming offer from " + peerDeviceId);
        updateConnectionState(peerDeviceId, PeerConnectionState.CONNECTING);

        PeerConnection pc = getOrCreatePeerConnection(peerDeviceId);
        if (pc == null) {
            updateConnectionState(peerDeviceId, PeerConnectionState.UNAVAILABLE);
            return;
        }

        SessionDescription remoteSdp = new SessionDescription(SessionDescription.Type.OFFER, sdpOffer);
        pc.setRemoteDescription(new SimpleSdpObserver() {
            @Override
            public void onSetSuccess() {
                Log.d(TAG, "Set remote offer SDP success. Creating answer...");
                pc.createAnswer(new SimpleSdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        pc.setLocalDescription(new SimpleSdpObserver() {
                            @Override
                            public void onSetSuccess() {
                                Log.d(TAG, "Set local answer SDP success. Relaying answer...");
                                SocketManager.getInstance(context).sendWebRtcAnswer(peerDeviceId, sessionDescription.description);
                            }
                        }, sessionDescription);
                    }

                    @Override
                    public void onCreateFailure(String s) {
                        Log.e(TAG, "Failed to create answer: " + s);
                        updateConnectionState(peerDeviceId, PeerConnectionState.UNAVAILABLE);
                    }
                }, new MediaConstraints());
            }

            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "Failed to set remote offer: " + s);
                updateConnectionState(peerDeviceId, PeerConnectionState.UNAVAILABLE);
            }
        }, remoteSdp);
    }

    // Process Answer SDP on dialing device
    public void handleIncomingAnswer(String peerDeviceId, String sdpAnswer) {
        Log.d(TAG, "Received answer from " + peerDeviceId);
        PeerConnection pc = peerConnections.get(peerDeviceId);
        if (pc == null) return;

        SessionDescription remoteSdp = new SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer);
        pc.setRemoteDescription(new SimpleSdpObserver() {
            @Override
            public void onSetSuccess() {
                Log.d(TAG, "Set remote answer SDP success for " + peerDeviceId);
            }

            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "Failed to set remote answer: " + s);
                updateConnectionState(peerDeviceId, PeerConnectionState.UNAVAILABLE);
            }
        }, remoteSdp);
    }

    // Add candidate received from socket to PeerConnection
    public void handleIncomingIceCandidate(String peerDeviceId, JSONObject jsonCandidate) {
        PeerConnection pc = peerConnections.get(peerDeviceId);
        if (pc == null) return;

        try {
            IceCandidate candidate = new IceCandidate(
                    jsonCandidate.getString("sdpMid"),
                    jsonCandidate.getInt("sdpMLineIndex"),
                    jsonCandidate.getString("candidate")
            );
            pc.addIceCandidate(candidate);
            Log.d(TAG, "Successfully added remote ICE Candidate for " + peerDeviceId);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing incoming ICE candidate", e);
        }
    }

    public void handleConnectionError(String peerDeviceId, String reason) {
        Log.w(TAG, "Peer " + peerDeviceId + " connection error: " + reason);
        updateConnectionState(peerDeviceId, PeerConnectionState.UNAVAILABLE);
        disconnectPeer(peerDeviceId);
    }

    private PeerConnection getOrCreatePeerConnection(String peerDeviceId) {
        if (peerConnections.containsKey(peerDeviceId)) {
            return peerConnections.get(peerDeviceId);
        }

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServersList);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;

        PeerConnection pc = factory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "SignalingState change for " + peerDeviceId + ": " + signalingState);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "IceConnectionState change for " + peerDeviceId + ": " + iceConnectionState);
                if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED ||
                    iceConnectionState == PeerConnection.IceConnectionState.COMPLETED) {
                    // Connection completed
                } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED ||
                           iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                    updateConnectionState(peerDeviceId, PeerConnectionState.UNAVAILABLE);
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {}

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "IceGatheringState change for " + peerDeviceId + ": " + iceGatheringState);
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                // Secure practice: never log the raw ICE candidate details
                Log.d(TAG, "Local ICE Candidate gathered for " + peerDeviceId + ". Sending to remote...");
                try {
                    JSONObject json = new JSONObject();
                    json.put("sdpMid", iceCandidate.sdpMid);
                    json.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                    json.put("candidate", iceCandidate.sdp);
                    
                    SocketManager.getInstance(context).sendIceCandidate(peerDeviceId, json);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to serialize local ICE Candidate", e);
                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}

            @Override
            public void onAddStream(MediaStream mediaStream) {}

            @Override
            public void onRemoveStream(MediaStream mediaStream) {}

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(TAG, "Incoming DataChannel received from " + peerDeviceId);
                setupDataChannel(peerDeviceId, dataChannel);
            }

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {}
        });

        if (pc != null) {
            peerConnections.put(peerDeviceId, pc);
        }
        return pc;
    }

    private void setupDataChannel(String peerDeviceId, DataChannel dc) {
        dataChannels.put(peerDeviceId, dc);
        dc.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {}

            @Override
            public void onStateChange() {
                DataChannel.State state = dc.state();
                Log.d(TAG, "DataChannel with " + peerDeviceId + " state: " + state);
                if (state == DataChannel.State.OPEN) {
                    updateConnectionState(peerDeviceId, PeerConnectionState.CONNECTED);
                    
                    // Send local profile metadata directly to peer over WebRTC
                    try {
                        UserProfile profile = userRepository.getProfile();
                        if (profile != null) {
                            JSONObject initPayload = new JSONObject();
                            initPayload.put("type", "init");
                            initPayload.put("displayName", profile.getDisplayName());
                            initPayload.put("phoneNumber", profile.getPhoneNumber());
                            byte[] bytes = initPayload.toString().getBytes(StandardCharsets.UTF_8);
                            dc.send(new DataChannel.Buffer(ByteBuffer.wrap(bytes), false));
                            Log.d(TAG, "Sent P2P init payload to " + peerDeviceId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to send P2P init payload", e);
                    }
                    
                    retryPendingMessages(peerDeviceId);
                } else if (state == DataChannel.State.CLOSED) {
                    updateConnectionState(peerDeviceId, PeerConnectionState.UNAVAILABLE);
                }
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                ByteBuffer byteBuffer = buffer.data;
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                String rawMsg = new String(bytes, StandardCharsets.UTF_8);
                handleReceivedPayload(peerDeviceId, rawMsg);
            }
        });
    }

    // Send a text message over the DataChannel if open
    public boolean sendMessage(String peerDeviceId, Message message) {
        DataChannel dc = dataChannels.get(peerDeviceId);
        if (dc != null && dc.state() == DataChannel.State.OPEN) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("type", "msg");
                payload.put("id", message.getId());
                payload.put("content", message.getContent());
                payload.put("timestamp", message.getTimestamp());

                byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                dc.send(new DataChannel.Buffer(buffer, false));
                
                // Set locally to SENT (meaning transmitted over wire)
                chatRepository.updateMessageStatus(message.getId(), DeliveryStatus.SENT);
                Log.d(TAG, "Sent message over DataChannel: " + message.getId());
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error sending message payload over WebRTC", e);
            }
        }
        return false;
    }

    private void handleReceivedPayload(String peerDeviceId, String rawPayload) {
        try {
            JSONObject payload = new JSONObject();
            try {
                payload = new JSONObject(rawPayload);
            } catch (JSONException je) {
                // fallback
                return;
            }
            String type = payload.optString("type", "");

            if ("init".equals(type)) {
                String displayName = payload.getString("displayName");
                String phoneNumber = payload.getString("phoneNumber");
                dbExecutor.execute(() -> {
                    Conversation conv = chatRepository.getConversationByIdSync(peerDeviceId);
                    if (conv == null) {
                        conv = new Conversation(peerDeviceId, displayName, phoneNumber, UserRepository.sha256(phoneNumber), "");
                        chatRepository.insertConversation(conv);
                    } else {
                        conv.setPeerName(displayName);
                        conv.setPeerPhone(phoneNumber);
                        chatRepository.insertConversation(conv);
                    }
                });
            } else if ("msg".equals(type)) {
                String id = payload.getString("id");
                String content = payload.getString("content");
                long timestamp = payload.getLong("timestamp");

                UserProfile profile = userRepository.getProfile();
                String myDeviceId = profile != null ? profile.getDeviceId() : "";

                // Create the incoming message with DELIVERED status
                Message message = new Message(id, peerDeviceId, peerDeviceId, myDeviceId, content, timestamp, DeliveryStatus.DELIVERED.name(), true);
                chatRepository.insertMessage(message);

                // Auto-respond with delivery acknowledgement directly back
                JSONObject ack = new JSONObject();
                ack.put("type", "ack");
                ack.put("messageId", id);
                
                DataChannel dc = dataChannels.get(peerDeviceId);
                if (dc != null && dc.state() == DataChannel.State.OPEN) {
                    ByteBuffer buffer = ByteBuffer.wrap(ack.toString().getBytes(StandardCharsets.UTF_8));
                    dc.send(new DataChannel.Buffer(buffer, false));
                    Log.d(TAG, "Sent Delivery Acknowledgement for " + id);
                }

            } else if ("ack".equals(type)) {
                String messageId = payload.getString("messageId");
                Log.d(TAG, "Received Delivery Acknowledgement for message: " + messageId);
                chatRepository.updateMessageStatus(messageId, DeliveryStatus.DELIVERED);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling incoming payload", e);
        }
    }

    // Automatically retries sending unsent messages once direct connection recovers
    private void retryPendingMessages(String peerDeviceId) {
        dbExecutor.execute(() -> {
            List<Message> unsentList = chatRepository.getUnsentMessagesSync(peerDeviceId);
            if (!unsentList.isEmpty()) {
                Log.d(TAG, "Retrying " + unsentList.size() + " unsent messages for " + peerDeviceId);
                for (Message message : unsentList) {
                    sendMessage(peerDeviceId, message);
                }
            }
        });
    }

    public void disconnectPeer(String peerDeviceId) {
        DataChannel dc = dataChannels.remove(peerDeviceId);
        if (dc != null) {
            try {
                dc.close();
                dc.dispose();
            } catch (Exception ignored) {}
        }

        PeerConnection pc = peerConnections.remove(peerDeviceId);
        if (pc != null) {
            try {
                pc.close();
                pc.dispose();
            } catch (Exception ignored) {}
        }
        updateConnectionState(peerDeviceId, PeerConnectionState.UNAVAILABLE);
        Log.d(TAG, "Disconnected and cleaned up WebRTC peer: " + peerDeviceId);
    }

    public void disconnectAll() {
        for (String peerDeviceId : new ArrayList<>(peerConnections.keySet())) {
            disconnectPeer(peerDeviceId);
        }
    }

    // SocketManager.SocketListener implementation
    @Override
    public void onConnected() {
        dbExecutor.execute(() -> {
            for (Conversation conversation : chatRepository.getAllConversationsSync()) {
                SocketManager.getInstance(context).sendConnectionRequest(conversation.getId());
            }
        });
    }

    @Override
    public void onDisconnected() {
        disconnectAll();
    }

    @Override
    public void onPeerOnline(String deviceId, String phoneHash) {
        dbExecutor.execute(() -> {
            Conversation conv = chatRepository.getConversationByIdSync(deviceId);
            if (conv != null) {
                SocketManager.getInstance(context).sendConnectionRequest(deviceId);
            }
        });
    }

    @Override
    public void onPeerOffline(String deviceId, String phoneHash) {
        disconnectPeer(deviceId);
    }

    @Override
    public void onConnectionRequest(String senderDeviceId) {
        initiateConnection(senderDeviceId);
    }

    @Override
    public void onWebRtcOffer(String senderDeviceId, String sdp) {
        handleIncomingOffer(senderDeviceId, sdp);
    }

    @Override
    public void onWebRtcAnswer(String senderDeviceId, String sdp) {
        handleIncomingAnswer(senderDeviceId, sdp);
    }

    @Override
    public void onIceCandidate(String senderDeviceId, JSONObject candidate) {
        handleIncomingIceCandidate(senderDeviceId, candidate);
    }

    @Override
    public void onConnectionError(String senderDeviceId, String reason) {
        handleConnectionError(senderDeviceId, reason);
    }

    // Helper classes to reduce boilerplate
    private static class SimpleSdpObserver implements SdpObserver {
        @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
        @Override public void onSetSuccess() {}
        @Override public void onCreateFailure(String s) {}
        @Override public void onSetFailure(String s) {}
    }
}
