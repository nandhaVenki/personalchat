package com.personalchat.app.ui.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.personalchat.app.R;
import com.personalchat.app.data.model.UserProfile;
import com.personalchat.app.data.repository.UserRepository;
import com.personalchat.app.network.SocketManager;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText inputSignalingUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        UserRepository userRepository = new UserRepository(this);

        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView tvName = findViewById(R.id.tv_profile_name);
        TextView tvPhone = findViewById(R.id.tv_profile_phone);
        TextView tvDeviceId = findViewById(R.id.tv_profile_device_id);
        TextView tvPubKey = findViewById(R.id.tv_profile_pubkey);
        inputSignalingUrl = findViewById(R.id.input_signaling_url);
        MaterialButton btnSave = findViewById(R.id.btn_save_settings);

        btnBack.setOnClickListener(v -> finish());

        // Load profile values
        UserProfile profile = userRepository.getProfile();
        if (profile != null) {
            tvName.setText(profile.getDisplayName());
            tvPhone.setText(profile.getPhoneNumber());
            tvDeviceId.setText(profile.getDeviceId());
            tvPubKey.setText(profile.getPublicKey());
        }

        // Load saved signaling URL from profile prefs
        SharedPreferences prefs = getSharedPreferences("profile_prefs", MODE_PRIVATE);
        String savedUrl = prefs.getString("signaling_url", "https://personalchat-signaling.onrender.com");
        inputSignalingUrl.setText(savedUrl);

        btnSave.setOnClickListener(v -> {
            if (inputSignalingUrl.getText() != null) {
                String newUrl = inputSignalingUrl.getText().toString().trim();
                if (newUrl.isEmpty()) {
                    inputSignalingUrl.setError("Signaling URL cannot be empty");
                    return;
                }

                // Save setting
                prefs.edit().putString("signaling_url", newUrl).apply();
                Toast.makeText(ProfileActivity.this, "Settings saved!", Toast.LENGTH_SHORT).show();

                // Reconnect signaling socket with new URL
                SocketManager.getInstance(ProfileActivity.this).disconnect();
                SocketManager.getInstance(ProfileActivity.this).connect(newUrl);

                finish();
            }
        });
    }
}
