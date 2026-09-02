package com.example.possystem;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class PurchasesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchases);

        // Back Button
        if (findViewById(R.id.btnBackPurchases) != null) {
            findViewById(R.id.btnBackPurchases).setOnClickListener(v -> finish());
        }

        // FAB Add Purchase
        if (findViewById(R.id.fabAddPurchase) != null) {
            findViewById(R.id.fabAddPurchase).setOnClickListener(v -> {
                startActivity(new Intent(PurchasesActivity.this, AddPurchaseActivity.class));
            });
        }
    }
}
