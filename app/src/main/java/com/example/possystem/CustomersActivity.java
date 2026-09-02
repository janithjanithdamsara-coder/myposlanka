package com.example.possystem;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class CustomersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers);

        // Back Button
        if (findViewById(R.id.btnBackCustomers) != null) {
            findViewById(R.id.btnBackCustomers).setOnClickListener(v -> finish());
        }
    }
}
