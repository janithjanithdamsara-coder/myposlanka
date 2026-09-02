package com.example.possystem;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class ExpensesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses);

        // Back Button
        if (findViewById(R.id.btnBackExpenses) != null) {
            findViewById(R.id.btnBackExpenses).setOnClickListener(v -> finish());
        }
    }
}
