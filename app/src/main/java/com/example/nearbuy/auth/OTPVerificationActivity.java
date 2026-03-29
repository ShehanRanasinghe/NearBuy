package com.example.nearbuy.auth;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.nearbuy.R;
import com.example.nearbuy.dashboard.DashboardActivity;
public class OTPVerificationActivity extends AppCompatActivity {
    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private Button btnVerify;
    private TextView tvPhoneNumber, tvResendOtp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);
        String phone = getIntent().getStringExtra("phone");
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);
        btnVerify   = findViewById(R.id.btnVerify);
        tvResendOtp = findViewById(R.id.tvResendOtp);
        if (phone != null) tvPhoneNumber.setText(phone);
        setupOtpAutoFocus();
        btnVerify.setOnClickListener(v -> verifyOtp());
        tvResendOtp.setOnClickListener(v ->
                Toast.makeText(this, "OTP resent!", Toast.LENGTH_SHORT).show());
    }
    private void setupOtpAutoFocus() {
        addWatcher(otp1, null, otp2);
        addWatcher(otp2, otp1, otp3);
        addWatcher(otp3, otp2, otp4);
        addWatcher(otp4, otp3, otp5);
        addWatcher(otp5, otp4, otp6);
        addWatcher(otp6, otp5, null);
    }
    private void addWatcher(EditText current, EditText prev, EditText next) {
        current.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count == 1 && next != null) next.requestFocus();
                if (count == 0 && prev != null) prev.requestFocus();
            }
        });
    }
    private void verifyOtp() {
        String code = otp1.getText().toString() + otp2.getText() + otp3.getText()
                + otp4.getText() + otp5.getText() + otp6.getText();
        if (code.length() < 6) {
            Toast.makeText(this, "Enter all 6 digits", Toast.LENGTH_SHORT).show();
            return;
        }
        // Sample: any 6-digit code works
        Toast.makeText(this, "Verified! Welcome to NearBuy", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, DashboardActivity.class));
        finishAffinity();
    }
}
