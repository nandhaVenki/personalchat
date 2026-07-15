package com.personalchat.app.data.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.personalchat.app.data.keystore.KeyStoreHelper;
import com.personalchat.app.data.model.Conversation;
import com.personalchat.app.data.model.Message;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

import java.security.SecureRandom;

@Database(entities = {Conversation.class, Message.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    private static final String DATABASE_NAME = "personal_chat_secure.db";
    private static final String PREFS_NAME = "secure_db_prefs";
    private static final String KEY_ENCRYPTED_PASS = "encrypted_db_pass";
    private static final String KEY_IV = "db_pass_iv";

    private static volatile AppDatabase instance;

    public abstract ConversationDao conversationDao();
    public abstract MessageDao messageDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = buildDatabase(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private static AppDatabase buildDatabase(Context context) {
        try {
            // 1. Initialize SQLCipher libraries
            System.loadLibrary("sqlcipher");
            Log.d(TAG, "SQLCipher libraries loaded successfully.");

            // 2. Retrieve or generate database passphrase
            String dbPassphrase = getOrCreateDatabasePassphrase(context);
            if (dbPassphrase == null) {
                throw new RuntimeException("Could not retrieve or generate DB passphrase.");
            }

            // 3. Create SQLCipher SupportOpenHelperFactory
            byte[] passphraseBytes = dbPassphrase.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            SupportOpenHelperFactory supportFactory = new SupportOpenHelperFactory(passphraseBytes);

            // 4. Build Room Encrypted Database
            return Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME)
                    .openHelperFactory(supportFactory)
                    .fallbackToDestructiveMigration() // Simple PoC strategy
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "Error building encrypted database", e);
            throw new RuntimeException("Failed to initialize encrypted database", e);
        }
    }

    private static String getOrCreateDatabasePassphrase(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String encryptedPass = prefs.getString(KEY_ENCRYPTED_PASS, null);
        String ivStr = prefs.getString(KEY_IV, null);

        KeyStoreHelper keyStoreHelper = new KeyStoreHelper();

        if (encryptedPass != null && ivStr != null) {
            // Decrypt existing passphrase
            Log.d(TAG, "Decrypting existing database passphrase.");
            return keyStoreHelper.decrypt(encryptedPass, ivStr);
        } else {
            // Generate a secure random passphrase
            Log.d(TAG, "Generating new database passphrase.");
            byte[] randomBytes = new byte[32];
            new SecureRandom().nextBytes(randomBytes);
            String rawPassphrase = Base64.encodeToString(randomBytes, Base64.NO_WRAP);

            // Encrypt and store it
            StringBuilder outIv = new StringBuilder();
            String encrypted = keyStoreHelper.encrypt(rawPassphrase, outIv);

            if (encrypted != null) {
                prefs.edit()
                        .putString(KEY_ENCRYPTED_PASS, encrypted)
                        .putString(KEY_IV, outIv.toString())
                        .apply();
                return rawPassphrase;
            }
            return null;
        }
    }
}
