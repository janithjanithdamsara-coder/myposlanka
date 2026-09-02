package com.example.possystem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.possystem.helper.LicenseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ActivationActivity extends AppCompatActivity {

    private TextInputEditText etShopName, etShopId, etActivationPin, etReferralCode;
    private MaterialButton btnActivateLicense, btnCallSupport, btnWhatsAppSupport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation);

        etShopName = findViewById(R.id.etActivationShopName);
        etShopId = findViewById(R.id.etActivationShopId);
        etActivationPin = findViewById(R.id.etActivationPin);
        etReferralCode = findViewById(R.id.etReferralCode);
        btnActivateLicense = findViewById(R.id.btnActivateLicense);
        btnCallSupport = findViewById(R.id.btnCallSupport);
        btnWhatsAppSupport = findViewById(R.id.btnWhatsAppSupport);

        // Pre-fill existing shop name if available
        if (LicenseManager.isActivated(this)) {
            etShopName.setText(LicenseManager.getShopName(this));
            etShopId.setText(LicenseManager.getShopId(this));
        }

        btnActivateLicense.setOnClickListener(v -> handleActivation());

        btnCallSupport.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + LicenseManager.SUPPORT_PHONE.replace(" ", "")));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Support: " + LicenseManager.SUPPORT_PHONE, Toast.LENGTH_LONG).show();
            }
        });

        btnWhatsAppSupport.setOnClickListener(v -> {
            try {
                String message = "Hello Admin, I need an activation PIN for POS System.";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://wa.me/" + LicenseManager.SUPPORT_WHATSAPP.replace("+", "") + "?text=" + Uri.encode(message)));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp: " + LicenseManager.SUPPORT_WHATSAPP, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleActivation() {
        String shopName = etShopName.getText() != null ? etShopName.getText().toString().trim() : "";
        String shopId = etShopId.getText() != null ? etShopId.getText().toString().trim().toUpperCase() : "";
        String pin = etActivationPin.getText() != null ? etActivationPin.getText().toString().trim() : "";

        if (TextUtils.isEmpty(shopName)) {
            etShopName.setError("Enter shop name");
            return;
        }

        if (TextUtils.isEmpty(shopId)) {
            etShopId.setError("Enter Shop ID (e.g. SHP-101)");
            return;
        }

        if (TextUtils.isEmpty(pin)) {
            etActivationPin.setError("Enter 6-digit PIN");
            return;
        }

        // Check if PIN matches standard trial or offline hash
        String trial14Hash = LicenseManager.generateOfflineCode(shopId, "RENEW_TRIAL_14");
        String trial30Hash = LicenseManager.generateOfflineCode(shopId, "RENEW_TRIAL_30");
        String trial7Hash = LicenseManager.generateOfflineCode(shopId, "RENEW_TRIAL_7");
        String paid30Hash = LicenseManager.generateOfflineCode(shopId, "RENEW_PAID_30");
        String emergencyHash = LicenseManager.generateOfflineCode(shopId, "EMERGENCY_UNLOCK");

        int durationDays = 14; // Default to 14 days trial
        String planType = "TRIAL";

        if (pin.equals(paid30Hash)) {
            durationDays = 30;
            planType = "PAID";
        } else if (pin.equals(trial30Hash)) {
            durationDays = 30;
            planType = "TRIAL";
        } else if (pin.equals(trial7Hash)) {
            durationDays = 7;
            planType = "TRIAL";
        } else if (pin.equals(trial14Hash)) {
            durationDays = 14;
            planType = "TRIAL";
        } else if (pin.equals(emergencyHash)) {
            durationDays = 3;
            planType = "PAID";
        } else {
            // Check default master demo PIN "849201"
            if ("849201".equals(pin) || "123456".equals(pin)) {
                durationDays = 14;
                planType = "TRIAL";
            } else {
                Toast.makeText(this, "❌ Invalid Activation PIN for " + shopId + ". Contact Admin.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Activate License
        LicenseManager.activateShop(this, shopId, shopName, pin, durationDays, planType);

        // Immediate background sync to pull exact business_type, features & receipt branding
        LicenseManager.syncLicenseWithServer(this, null);

        Toast.makeText(this, "🎉 Store Activated Successfully! " + durationDays + " Days " + planType, Toast.LENGTH_LONG).show();

        // Navigate to Login Activity
        Intent intent = new Intent(ActivationActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
