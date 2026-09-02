package com.example.possystem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.possystem.helper.LicenseManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1800; // 1.8 seconds delay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Check if activated
        if (!LicenseManager.isActivated(this)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(SplashActivity.this, ActivationActivity.class));
                finish();
            }, SPLASH_DELAY_MS);
            return;
        }

        // 🌐 Background Sync with XAMPP Server to check if Admin marked store as Inactive or Extended days
        LicenseManager.syncLicenseWithServer(this, null);

        // Delayed Handler to navigate based on validated License State
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (!LicenseManager.isLicenseValid(SplashActivity.this)) {
                // Suspended / Inactive / Expired -> Subscription Lock Screen
                intent = new Intent(SplashActivity.this, SubscriptionLockedActivity.class);
            } else {
                // Valid Active License -> Login Activity
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish(); // Close SplashActivity
        }, SPLASH_DELAY_MS);
    }
}
