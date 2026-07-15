package com.personalchat.app.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.personalchat.app.R;
import com.personalchat.app.data.repository.UserRepository;
import com.personalchat.app.ui.chats.ChatsActivity;
import com.personalchat.app.ui.onboarding.OnboardingActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Simple artificial delay for branding feel
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            UserRepository userRepository = new UserRepository(SplashActivity.this);
            Intent intent;
            if (userRepository.hasProfile()) {
                intent = new Intent(SplashActivity.this, ChatsActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            }
            startActivity(intent);
            finish();
        }, 1500);
    }
}
