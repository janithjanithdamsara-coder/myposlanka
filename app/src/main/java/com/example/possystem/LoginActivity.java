package com.example.possystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.possystem.helper.UserSessionManager;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private ImageView btnToggleEye;
    private Button btnLogin, btnFingerprint;

    private boolean isPasswordVisible = false;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI Elements
        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);
        btnToggleEye = findViewById(R.id.btnTogglePasswordEye);
        btnLogin = findViewById(R.id.btnLoginSubmit);
        btnFingerprint = findViewById(R.id.btnFingerprintLogin);

        // Safe Setup for Biometric Fingerprint Prompt
        setupBiometricPrompt();

        // Eye Icon Click Handler to Toggle Password Visibility
        if (btnToggleEye != null && etPassword != null) {
            btnToggleEye.setOnClickListener(v -> {
                int selection = etPassword.getSelectionEnd();
                if (isPasswordVisible) {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    isPasswordVisible = false;
                } else {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    isPasswordVisible = true;
                }
                etPassword.setSelection(selection);
            });
        }

        // Login Button Click Handler
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                String username = etUsername != null ? etUsername.getText().toString().trim() : "";
                String password = etPassword != null ? etPassword.getText().toString().trim() : "";

                if (TextUtils.isEmpty(username)) {
                    if (etUsername != null) etUsername.setError("Please enter username");
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    if (etPassword != null) etPassword.setError("Please enter password");
                    return;
                }

                // Default Login Credentials Check (admin / admin or cashier / cashier)
                if ((username.equalsIgnoreCase("admin") && password.equals("admin")) ||
                    (username.equalsIgnoreCase("cashier") && password.equals("cashier"))) {
                    
                    String role = username.equalsIgnoreCase("admin") ? "ADMIN" : "CASHIER";
                    UserSessionManager.saveSession(LoginActivity.this, username, role);

                    Toast.makeText(LoginActivity.this, "Login Successful! Welcome " + username, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid Username or Password!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Fingerprint Login Click Handler
        if (btnFingerprint != null) {
            btnFingerprint.setOnClickListener(v -> checkAndLaunchBiometric());
        }
    }

    private void setupBiometricPrompt() {
        try {
            executor = ContextCompat.getMainExecutor(this);

            biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Log.d("POS_BIOMETRIC", "Biometric Error: " + errString);
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    UserSessionManager.saveSession(LoginActivity.this, "admin", "ADMIN");
                    Toast.makeText(getApplicationContext(), "Fingerprint Login Successful! Welcome Admin", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(getApplicationContext(), "Fingerprint not recognized. Try again.", Toast.LENGTH_SHORT).show();
                }
            });

            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Fingerprint Quick Login")
                    .setSubtitle("Scan registered fingerprint to access POS Counter")
                    .setNegativeButtonText("Use Password Instead")
                    .build();
        } catch (Exception e) {
            Log.e("POS_BIOMETRIC", "Biometric setup failed safely", e);
        }
    }

    private void checkAndLaunchBiometric() {
        try {
            if (biometricPrompt == null || promptInfo == null) {
                Toast.makeText(this, "Fingerprint login not supported on this device.", Toast.LENGTH_SHORT).show();
                return;
            }
            BiometricManager biometricManager = BiometricManager.from(this);
            int canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK);
            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                biometricPrompt.authenticate(promptInfo);
            } else {
                Toast.makeText(this, "Fingerprint sensor unavailable or not registered.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Fingerprint sensor unavailable.", Toast.LENGTH_SHORT).show();
        }
    }
}
