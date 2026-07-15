package com.personalchat.app.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations")
public class Conversation {
    
    @PrimaryKey
    @NonNull
    private String id; // Typically peerDeviceId
    
    private String peerName;
    private String peerPhone;
    private String peerPhoneHash;
    private String peerPublicKey;
    private String lastMessage;
    private long lastMessageTimestamp;
    
    private String connectionState; // Mapping to PeerConnectionState name
    private int unreadCount;

    public Conversation() {
    }

    @Ignore
    public Conversation(@NonNull String id, String peerName, String peerPhone, String peerPhoneHash, String peerPublicKey) {
        this.id = id;
        this.peerName = peerName;
        this.peerPhone = peerPhone;
        this.peerPhoneHash = peerPhoneHash;
        this.peerPublicKey = peerPublicKey;
        this.connectionState = PeerConnectionState.UNAVAILABLE.name();
        this.lastMessage = "";
        this.lastMessageTimestamp = System.currentTimeMillis();
        this.unreadCount = 0;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getPeerName() {
        return peerName;
    }

    public void setPeerName(String peerName) {
        this.peerName = peerName;
    }

    public String getPeerPhone() {
        return peerPhone;
    }

    public void setPeerPhone(String peerPhone) {
        this.peerPhone = peerPhone;
    }

    public String getPeerPhoneHash() {
        return peerPhoneHash;
    }

    public void setPeerPhoneHash(String peerPhoneHash) {
        this.peerPhoneHash = peerPhoneHash;
    }

    public String getPeerPublicKey() {
        return peerPublicKey;
    }

    public void setPeerPublicKey(String peerPublicKey) {
        this.peerPublicKey = peerPublicKey;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(String connectionState) {
        this.connectionState = connectionState;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
