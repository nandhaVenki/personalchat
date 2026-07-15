package com.personalchat.app.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.personalchat.app.data.model.UserProfile;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.UUID;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String PREFS_PROFILE = "profile_prefs";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_PHONE_HASH = "phone_hash";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_PUBLIC_KEY = "public_key";
    
    private static final String KEYSTORE_ALIAS = "PersonalChat_Identity_Key";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    private final Context context;

    public UserRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean hasProfile() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE);
        return prefs.contains(KEY_DEVICE_ID) && prefs.contains(KEY_PHONE_NUMBER);
    }

    public UserProfile getProfile() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_DEVICE_ID)) {
            return null;
        }
        return new UserProfile(
                prefs.getString(KEY_DISPLAY_NAME, ""),
                prefs.getString(KEY_PHONE_NUMBER, ""),
                prefs.getString(KEY_PHONE_HASH, ""),
                prefs.getString(KEY_DEVICE_ID, ""),
                prefs.getString(KEY_PUBLIC_KEY, "")
        );
    }

    public void saveProfile(String displayName, String rawPhoneNumber) {
        String normalizedPhone = normalizePhoneNumber(rawPhoneNumber);
        String phoneHash = sha256(normalizedPhone);
        String deviceId = UUID.randomUUID().toString();
        
        String publicKeyBase64 = "";
        try {
            generateIdentityKeyPair();
            publicKeyBase64 = getIdentityPublicKeyBase64();
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate identity key pair", e);
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_DISPLAY_NAME, displayName)
                .putString(KEY_PHONE_NUMBER, normalizedPhone)
                .putString(KEY_PHONE_HASH, phoneHash)
                .putString(KEY_DEVICE_ID, deviceId)
                .putString(KEY_PUBLIC_KEY, publicKeyBase64)
                .apply();
        Log.d(TAG, "Profile saved: deviceId=" + deviceId + ", phoneHash=" + phoneHash);
    }

    // Phone number normalization
    public static String normalizePhoneNumber(String raw) {
        if (raw == null) return "";
        // Strip everything except digits and plus sign
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.isEmpty()) {
            return "";
        }
        // If it starts with + we keep it as is
        if (digits.startsWith("+")) {
            return digits;
        }
        // Else, let's prepend a default code (e.g. +1 for US/Canada) if it's 10 digits
        if (digits.length() == 10) {
            return "+1" + digits;
        }
        return "+" + digits; // fallback
    }

    // SHA-256 hash generator for phone number privacy
    public static String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            Log.e(TAG, "SHA-256 hashing failed", ex);
            return "";
        }
    }

    // Keystore integration: generates standard RSA Keypair inside Android Keystore
    private void generateIdentityKeyPair() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER);
            
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY | KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .build();
            
            kpg.initialize(spec);
            kpg.generateKeyPair();
            Log.d(TAG, "Identity RSA keypair generated in Android Keystore.");
        }
    }

    private String getIdentityPublicKeyBase64() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        Certificate cert = keyStore.getCertificate(KEYSTORE_ALIAS);
        if (cert == null) return "";
        PublicKey publicKey = cert.getPublicKey();
        return Base64.encodeToString(publicKey.getEncoded(), Base64.NO_WRAP);
    }
}
