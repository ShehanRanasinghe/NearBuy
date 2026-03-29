package com.example.nearbuy.app.startup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nearbuy.R;
import com.example.nearbuy.auth.LoginActivity;
import com.example.nearbuy.auth.RegisterActivity;

/**
 * WelcomeActivity – Shown after splash for first-time users.
 * Displays app features and routes to Register or Login.
 */
public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button btnGetStarted = findViewById(R.id.btnGetStarted);
        TextView tvAlreadyHaveAccount = findViewById(R.id.tvAlreadyHaveAccount);

        btnGetStarted.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        tvAlreadyHaveAccount.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }
}

