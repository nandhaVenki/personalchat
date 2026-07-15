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
    private static final String DEFAULT_SERVER_URL = "https://personalchat-signaling.onrender.com";

    private static SocketManager instance;
    private Socket socket;
    private final Context context;
    private final UserRepository userRepository;
    private final Handler mainHandler;
    private final java.util.List<SocketListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private boolean isRegistered = false;

    public interface SocketListener {
        default void onConnected() {}
        default void onDisconnected() {}
        default void onPeerOnline(String deviceId, String phoneHash) {}
        default void onPeerOffline(String deviceId, String phoneHash) {}
        default void onConnectionRequest(String senderDeviceId) {}
        default void onWebRtcOffer(String senderDeviceId, String sdp) {}
        default void onWebRtcAnswer(String senderDeviceId, String sdp) {}
        default void onIceCandidate(String senderDeviceId, JSONObject candidate) {}
        default void onConnectionError(String senderDeviceId, String reason) {}
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

    public void addListener(SocketListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(SocketListener listener) {
        listeners.remove(listener);
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
            options.transports = new String[]{"websocket"};
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
                for (SocketListener listener : listeners) {
                    listener.onConnected();
                }
            });
            registerDevice();
        });

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.d(TAG, "Socket disconnected.");
            isRegistered = false;
            mainHandler.post(() -> {
                for (SocketListener listener : listeners) {
                    listener.onDisconnected();
                }
            });
        });

        socket.on("register-response", args -> {
            Log.d(TAG, "Registration confirmation received.");
            isRegistered = true;
            try {
                if (args != null && args.length > 0) {
                    JSONObject data = (JSONObject) args[0];
                    if (data.has("stunServers")) {
                        org.json.JSONArray array = data.getJSONArray("stunServers");
                        java.util.List<String> servers = new java.util.ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            servers.add(array.getString(i));
                        }
                        WebRtcManager.getInstance(context).setIceServers(servers);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing register-response", e);
            }
        });

        socket.on("peer-online", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String deviceId = data.getString("deviceId");
                String phoneHash = data.getString("phoneHash");
                mainHandler.post(() -> {
                    for (SocketListener listener : listeners) {
                        listener.onPeerOnline(deviceId, phoneHash);
                    }
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
                    for (SocketListener listener : listeners) {
                        listener.onPeerOffline(deviceId, phoneHash);
                    }
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
                    for (SocketListener listener : listeners) {
                        listener.onConnectionRequest(senderDeviceId);
                    }
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
                    for (SocketListener listener : listeners) {
                        listener.onWebRtcOffer(senderDeviceId, sdp);
                    }
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
                    for (SocketListener listener : listeners) {
                        listener.onWebRtcAnswer(senderDeviceId, sdp);
                    }
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
                    for (SocketListener listener : listeners) {
                        listener.onIceCandidate(senderDeviceId, candidate);
                    }
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
                    for (SocketListener listener : listeners) {
                        listener.onConnectionError(senderDeviceId, reason);
                    }
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
