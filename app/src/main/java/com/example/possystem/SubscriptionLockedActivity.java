package com.example.possystem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.possystem.helper.LicenseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SubscriptionLockedActivity extends AppCompatActivity {

    private TextView tvLockTitle, tvLockSubtitle, tvLockedShopName, tvLockedShopDetails;
    private TextInputEditText etRenewalPin;
    private MaterialButton btnApplyRenewalPin, btnCallAdmin, btnWhatsAppAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription_locked);

        tvLockTitle = findViewById(R.id.tvLockTitle);
        tvLockSubtitle = findViewById(R.id.tvLockSubtitle);
        tvLockedShopName = findViewById(R.id.tvLockedShopName);
        tvLockedShopDetails = findViewById(R.id.tvLockedShopDetails);
        etRenewalPin = findViewById(R.id.etRenewalPin);
        btnApplyRenewalPin = findViewById(R.id.btnApplyRenewalPin);
        btnCallAdmin = findViewById(R.id.btnCallAdmin);
        btnWhatsAppAdmin = findViewById(R.id.btnWhatsAppAdmin);

        // Load Shop License State
        String shopName = LicenseManager.getShopName(this);
        String shopId = LicenseManager.getShopId(this);
        String expiry = LicenseManager.getFormattedExpiryDate(this);

        tvLockedShopName.setText(shopName);
        tvLockedShopDetails.setText("Shop ID: " + shopId + " • Expired on: " + expiry);

        // Check if locked due to Clock Tampering
        if (LicenseManager.isClockTampered(this)) {
            tvLockTitle.setText("SECURITY TAMPER ALERT");
            tvLockSubtitle.setText("Device clock has been altered. Please restore system time.");
        }

        btnApplyRenewalPin.setOnClickListener(v -> handleOfflineRenewal());

        btnCallAdmin.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + LicenseManager.SUPPORT_PHONE.replace(" ", "")));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Admin Call: " + LicenseManager.SUPPORT_PHONE, Toast.LENGTH_LONG).show();
            }
        });

        btnWhatsAppAdmin.setOnClickListener(v -> {
            try {
                String message = "Hello Admin, I have made payment to renew POS subscription for Shop: " + shopName + " (ID: " + shopId + "). Please provide renewal PIN.";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://wa.me/" + LicenseManager.SUPPORT_WHATSAPP.replace("+", "") + "?text=" + Uri.encode(message)));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Admin WhatsApp: " + LicenseManager.SUPPORT_WHATSAPP, Toast.LENGTH_LONG).show();
            }
        });

        MaterialButton btnReverifyOnline = findViewById(R.id.btnReverifyOnline);
        if (btnReverifyOnline != null) {
            btnReverifyOnline.setOnClickListener(v -> {
                Toast.makeText(this, "Checking server status...", Toast.LENGTH_SHORT).show();
                LicenseManager.syncLicenseWithServer(this, (isBlocked, message) -> {
                    if (!isBlocked && LicenseManager.isLicenseValid(this)) {
                        Toast.makeText(this, "🎉 Store Active! Access Restored.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(SubscriptionLockedActivity.this, SplashActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "❌ Store is still Inactive / Suspended on Admin Server.", Toast.LENGTH_LONG).show();
                    }
                });
            });
        }
    }

    private void handleOfflineRenewal() {
        String pin = etRenewalPin.getText() != null ? etRenewalPin.getText().toString().trim() : "";
        if (TextUtils.isEmpty(pin)) {
            etRenewalPin.setError("Enter 6-digit PIN");
            return;
        }

        boolean success = LicenseManager.applyOfflineRenewalPin(this, pin);
        if (success) {
            Toast.makeText(this, "🎉 Subscription Renewed! Expiry: " + LicenseManager.getFormattedExpiryDate(this), Toast.LENGTH_LONG).show();

            // Resume to Splash -> Login / Main
            Intent intent = new Intent(SubscriptionLockedActivity.this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "❌ Invalid Renewal PIN. Please contact Admin.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent back press from bypassing lock screen
        finishAffinity();
    }
}
