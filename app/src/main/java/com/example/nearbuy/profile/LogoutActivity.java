package com.example.nearbuy.profile;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;

/**
 * LogoutActivity – Shown when the user taps "Log Out" in ProfileActivity.
 * Clears the back stack and redirects to WelcomeActivity,
 * or immediately when the user taps "Go to Login".
 */
public class LogoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        findViewById(R.id.btnYes).setOnClickListener(v -> goToWelcome());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }

    private void goToWelcome() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
