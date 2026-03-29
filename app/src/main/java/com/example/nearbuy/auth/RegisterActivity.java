package com.example.nearbuy.auth;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.nearbuy.R;
public class RegisterActivity extends AppCompatActivity {
    private EditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        etFullName       = findViewById(R.id.etFullName);
        etEmail          = findViewById(R.id.etEmail);
        etPhone          = findViewById(R.id.etPhone);
        etPassword       = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister      = findViewById(R.id.btnRegister);
        tvLogin          = findViewById(R.id.tvLogin);
        btnRegister.setOnClickListener(v -> attemptRegister());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
    private void attemptRegister() {
        String name  = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();
        String conf  = etConfirmPassword.getText().toString().trim();
        if (TextUtils.isEmpty(name)) { etFullName.setError("Name required"); return; }
        if (TextUtils.isEmpty(email)) { etEmail.setError("Email required"); return; }
        if (TextUtils.isEmpty(phone)) { etPhone.setError("Phone required"); return; }
        if (TextUtils.isEmpty(pass))  { etPassword.setError("Password required"); return; }
        if (!pass.equals(conf)) { etConfirmPassword.setError("Passwords do not match"); return; }
        // Navigate to OTP screen
        Toast.makeText(this, "OTP sent to " + phone, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, OTPVerificationActivity.class);
        intent.putExtra("phone", phone);
        intent.putExtra("name", name);
        startActivity(intent);
    }
}
