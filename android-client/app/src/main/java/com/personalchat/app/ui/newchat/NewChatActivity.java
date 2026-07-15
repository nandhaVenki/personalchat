package com.personalchat.app.ui.newchat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.personalchat.app.R;
import com.personalchat.app.data.model.Conversation;
import com.personalchat.app.data.repository.ChatRepository;
import com.personalchat.app.data.repository.UserRepository;
import com.personalchat.app.network.SocketManager;
import com.personalchat.app.ui.chat.ChatActivity;

import org.json.JSONObject;

public class NewChatActivity extends AppCompatActivity {
    private static final String TAG = "NewChatActivity";
    private static final int REQUEST_CODE_CONTACT_PERMISSION = 100;
    private static final int REQUEST_CODE_PICK_CONTACT = 101;

    private TextInputEditText inputPhoneManual;
    private TextView tvFeedback;
    private ProgressBar progressLookup;
    private MaterialButton btnStartChat;

    private SocketManager socketManager;
    private ChatRepository chatRepository;
    private String tempPeerName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);

        socketManager = SocketManager.getInstance(this);
        chatRepository = new ChatRepository(this);

        ImageButton btnBack = findViewById(R.id.btn_back);
        MaterialButton btnContactPicker = findViewById(R.id.btn_contact_picker);
        inputPhoneManual = findViewById(R.id.input_phone_manual);
        tvFeedback = findViewById(R.id.tv_feedback);
        progressLookup = findViewById(R.id.progress_lookup);
        btnStartChat = findViewById(R.id.btn_start_chat);

        btnBack.setOnClickListener(v -> finish());
        
        btnContactPicker.setOnClickListener(v -> {
            if (checkContactsPermission()) {
                launchContactPicker();
            } else {
                requestContactsPermission();
            }
        });

        btnStartChat.setOnClickListener(v -> {
            if (inputPhoneManual.getText() != null) {
                String manualNum = inputPhoneManual.getText().toString().trim();
                if (TextUtils.isEmpty(manualNum)) {
                    inputPhoneManual.setError("Please enter a phone number");
                    return;
                }
                // Clear temporary picked contact name since they entered manual text
                tempPeerName = "";
                performContactLookup(manualNum);
            }
        });
    }

    private boolean checkContactsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestContactsPermission() {
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.READ_CONTACTS}, 
                REQUEST_CODE_CONTACT_PERMISSION);
    }

    private void launchContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CONTACT_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchContactPicker();
            } else {
                Toast.makeText(this, "Permission denied. Cannot access system contacts.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_CONTACT && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            if (contactUri == null) return;

            String[] projection = new String[]{
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            };

            try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    
                    String rawNumber = cursor.getString(phoneIdx);
                    tempPeerName = cursor.getString(nameIdx);
                    
                    inputPhoneManual.setText(rawNumber);
                    Log.d(TAG, "Selected contact: " + tempPeerName + " (" + rawNumber + ")");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to read contact data", e);
            }
        }
    }

    private void performContactLookup(String rawNumber) {
        if (!socketManager.isConnected()) {
            Toast.makeText(this, "Cannot lookup: signaling offline", Toast.LENGTH_SHORT).show();
            return;
        }

        String normalized = UserRepository.normalizePhoneNumber(rawNumber);
        String hash = UserRepository.sha256(normalized);

        tvFeedback.setVisibility(View.GONE);
        progressLookup.setVisibility(View.VISIBLE);
        btnStartChat.setEnabled(false);

        socketManager.checkContact(hash, args -> runOnUiThread(() -> {
            progressLookup.setVisibility(View.GONE);
            btnStartChat.setEnabled(true);

            if (args == null || args.length == 0) {
                tvFeedback.setText("Server error during verification.");
                tvFeedback.setVisibility(View.VISIBLE);
                return;
            }

            try {
                JSONObject response = (JSONObject) args[0];
                if (response.has("error")) {
                    tvFeedback.setText("Error: " + response.getString("error"));
                    tvFeedback.setVisibility(View.VISIBLE);
                    return;
                }

                boolean registered = response.getBoolean("registered");
                if (registered) {
                    String peerDeviceId = response.getString("deviceId");
                    String peerPublicKey = response.getString("publicKey");

                    // Run Room inserts on DB thread
                    new Thread(() -> {
                        Conversation conv = chatRepository.getConversationByIdSync(peerDeviceId);
                        if (conv == null) {
                            String name = TextUtils.isEmpty(tempPeerName) ? normalized : tempPeerName;
                            conv = new Conversation(peerDeviceId, name, normalized, hash, peerPublicKey);
                            chatRepository.insertConversation(conv);
                        }

                        final Conversation finalConv = conv;
                        // Open chat session
                        runOnUiThread(() -> {
                            Intent intent = new Intent(NewChatActivity.this, ChatActivity.class);
                            intent.putExtra("conversation_id", peerDeviceId);
                            intent.putExtra("peer_name", finalConv.getPeerName());
                            intent.putExtra("peer_phone", finalConv.getPeerPhone());
                            startActivity(intent);
                            finish();
                        });
                    }).start();

                } else {
                    tvFeedback.setText(R.string.contact_not_registered);
                    tvFeedback.setVisibility(View.VISIBLE);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed parsing lookup response", e);
                tvFeedback.setText("Parse error verifying registry.");
                tvFeedback.setVisibility(View.VISIBLE);
            }
        }));
    }
}
