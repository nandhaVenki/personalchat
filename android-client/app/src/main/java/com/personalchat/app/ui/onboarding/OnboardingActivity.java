package com.personalchat.app.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.personalchat.app.R;
import com.personalchat.app.data.repository.UserRepository;
import com.personalchat.app.ui.chats.ChatsActivity;

public class OnboardingActivity extends AppCompatActivity {

    private TextInputEditText inputName;
    private TextInputEditText inputPhone;
    private MaterialButton btnContinue;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        userRepository = new UserRepository(this);

        inputName = findViewById(R.id.input_display_name);
        inputPhone = findViewById(R.id.input_phone_number);
        btnContinue = findViewById(R.id.btn_continue);

        btnContinue.setOnClickListener(v -> handleContinue());
    }

    private void handleContinue() {
        String name = inputName.getText() != null ? inputName.getText().toString().trim() : "";
        String phoneInput = inputPhone.getText() != null ? inputPhone.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            inputName.setError("Display name is required");
            return;
        }

        if (TextUtils.isEmpty(phoneInput)) {
            inputPhone.setError("Mobile number is required");
            return;
        }

        String normalizedPhone = UserRepository.normalizePhoneNumber(phoneInput);
        if (normalizedPhone.length() != 10) {
            inputPhone.setError("Mobile number must be exactly 10 digits");
            return;
        }

        // Save profile locally (hashes phone, generates RSA KeyPair in Android Keystore, stores values)
        userRepository.saveProfile(name, normalizedPhone);

        Toast.makeText(this, "Profile created successfully!", Toast.LENGTH_SHORT).show();

        // Launch Chats list
        Intent intent = new Intent(OnboardingActivity.this, ChatsActivity.class);
        startActivity(intent);
        finish();
    }
}
