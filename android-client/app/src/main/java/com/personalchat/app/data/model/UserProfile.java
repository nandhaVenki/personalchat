package com.personalchat.app.data.model;

public class UserProfile {
    private String displayName;
    private String phoneNumber;
    private String phoneHash;
    private String deviceId;
    private String publicKey;

    public UserProfile() {
    }

    public UserProfile(String displayName, String phoneNumber, String phoneHash, String deviceId, String publicKey) {
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.phoneHash = phoneHash;
        this.deviceId = deviceId;
        this.publicKey = publicKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneHash() {
        return phoneHash;
    }

    public void setPhoneHash(String phoneHash) {
        this.phoneHash = phoneHash;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
