package com.example.nearbuy.auth;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.nearbuy.R;
public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText etEmail;
    private Button btnSendReset;
    private TextView tvBackToLogin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        etEmail       = findViewById(R.id.etEmail);
        btnSendReset  = findViewById(R.id.btnSendReset);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        btnSendReset.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Enter your email");
                return;
            }
            Toast.makeText(this, "Reset link sent to " + email, Toast.LENGTH_LONG).show();
            finish();
        });
        tvBackToLogin.setOnClickListener(v -> finish());
    }
}
