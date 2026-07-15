package com.personalchat.app.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.personalchat.app.data.model.UserProfile;
import com.personalchat.app.data.repository.UserRepository;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static final String DEFAULT_SERVER_URL = "http://10.0.2.2:3000"; // Emulator localhost mapping

    private static SocketManager instance;
    private Socket socket;
    private final Context context;
    private final UserRepository userRepository;
    private final Handler mainHandler;
    private SocketListener listener;
    private boolean isRegistered = false;

    public interface SocketListener {
        void onConnected();
        void onDisconnected();
        void onPeerOnline(String deviceId, String phoneHash);
        void onPeerOffline(String deviceId, String phoneHash);
        void onConnectionRequest(String senderDeviceId);
        void onWebRtcOffer(String senderDeviceId, String sdp);
        void onWebRtcAnswer(String senderDeviceId, String sdp);
        void onIceCandidate(String senderDeviceId, JSONObject candidate);
        void onConnectionError(String senderDeviceId, String reason);
    }

    public static synchronized SocketManager getInstance(Context context) {
        if (instance == null) {
            instance = new SocketManager(context);
        }
        return instance;
    }

    private SocketManager(Context context) {
        this.context = context.getApplicationContext();
        this.userRepository = new UserRepository(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setListener(SocketListener listener) {
        this.listener = listener;
    }

    public void connect(String serverUrl) {
        if (socket != null && socket.connected()) {
            return;
        }

        String url = (serverUrl == null || serverUrl.isEmpty()) ? DEFAULT_SERVER_URL : serverUrl;
        try {
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;
            socket = IO.socket(url, options);

            setupSocketListeners();
            socket.connect();
            Log.d(TAG, "Connecting to signaling server at " + url);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid signaling server URI", e);
        }
    }

    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.off();
            socket = null;
            isRegistered = false;
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    private void setupSocketListeners() {
        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d(TAG, "Socket connected. Attempting registration...");
            mainHandler.post(() -> {
                if (listener != null) listener.onConnected();
            });
            registerDevice();
        });

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.d(TAG, "Socket disconnected.");
            isRegistered = false;
            mainHandler.post(() -> {
                if (listener != null) listener.onDisconnected();
            });
        });

        socket.on("register-response", args -> {
            Log.d(TAG, "Registration confirmation received.");
            isRegistered = true;
        });

        socket.on("peer-online", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String deviceId = data.getString("deviceId");
                String phoneHash = data.getString("phoneHash");
                mainHandler.post(() -> {
                    if (listener != null) listener.onPeerOnline(deviceId, phoneHash);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error parsing peer-online", e);
            }
        });

        socket.on("peer-offline", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String deviceId = data.getString("deviceId");
                String phoneHash = data.getString("phoneHash");
                mainHandler.post(() -> {
                    if (listener != null) listener.onPeerOffline(deviceId, phoneHash);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error parsing peer-offline", e);
            }
        });

        socket.on("connection-request", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String senderDeviceId = data.getString("senderDeviceId");
                mainHandler.post(() -> {
                    if (listener != null) listener.onConnectionRequest(senderDeviceId);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error parsing connection-request", e);
            }
        });

        socket.on("webrtc-offer", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String senderDeviceId = data.getString("senderDeviceId");
                String sdp = data.getString("sdp");
                mainHandler.post(() -> {
                    if (listener != null) listener.onWebRtcOffer(senderDeviceId, sdp);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error parsing webrtc-offer", e);
            }
        });

        socket.on("webrtc-answer", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String senderDeviceId = data.getString("senderDeviceId");
                String sdp = data.getString("sdp");
                mainHandler.post(() -> {
                    if (listener != null) listener.onWebRtcAnswer(senderDeviceId, sdp);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error parsing webrtc-answer", e);
            }
        });

        socket.on("ice-candidate", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String senderDeviceId = data.getString("senderDeviceId");
                JSONObject candidate = data.getJSONObject("candidate");
                mainHandler.post(() -> {
                    if (listener != null) listener.onIceCandidate(senderDeviceId, candidate);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error parsing ice-candidate", e);
            }
        });

        socket.on("connection-error", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String senderDeviceId = data.getString("senderDeviceId");
                String reason = data.getString("reason");
                mainHandler.post(() -> {
                    if (listener != null) listener.onConnectionError(senderDeviceId, reason);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error parsing connection-error", e);
            }
        });
    }

    private void registerDevice() {
        if (!userRepository.hasProfile()) {
            Log.w(TAG, "Cannot register: Profile does not exist yet.");
            return;
        }

        UserProfile profile = userRepository.getProfile();
        try {
            JSONObject payload = new JSONObject();
            payload.put("phoneHash", profile.getPhoneHash());
            payload.put("deviceId", profile.getDeviceId());
            payload.put("publicKey", profile.getPublicKey());
            
            socket.emit("register-device", payload);
            Log.d(TAG, "Sent register-device payload.");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build registration payload", e);
        }
    }

    public void checkContact(String phoneHash, Emitter.Listener callback) {
        if (!isConnected()) {
            Log.w(TAG, "Cannot check contact: socket offline.");
            return;
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put("phoneHash", phoneHash);
            socket.emit("check-contact", payload, callback);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build check-contact payload", e);
        }
    }

    public void sendConnectionRequest(String targetDeviceId) {
        if (!isConnected()) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("targetDeviceId", targetDeviceId);
            socket.emit("connection-request", payload);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build connection-request payload", e);
        }
    }

    public void sendWebRtcOffer(String targetDeviceId, String sdp) {
        if (!isConnected()) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("targetDeviceId", targetDeviceId);
            payload.put("sdp", sdp);
            socket.emit("webrtc-offer", payload);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build webrtc-offer payload", e);
        }
    }

    public void sendWebRtcAnswer(String targetDeviceId, String sdp) {
        if (!isConnected()) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("targetDeviceId", targetDeviceId);
            payload.put("sdp", sdp);
            socket.emit("webrtc-answer", payload);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build webrtc-answer payload", e);
        }
    }

    public void sendIceCandidate(String targetDeviceId, JSONObject candidate) {
        if (!isConnected()) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("targetDeviceId", targetDeviceId);
            payload.put("candidate", candidate);
            socket.emit("ice-candidate", payload);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build ice-candidate payload", e);
        }
    }

    public void sendConnectionError(String targetDeviceId, String reason) {
        if (!isConnected()) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("targetDeviceId", targetDeviceId);
            payload.put("reason", reason);
            socket.emit("connection-error", payload);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build connection-error payload", e);
        }
    }
}
