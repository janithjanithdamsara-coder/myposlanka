package com.example.possystem;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.possystem.helper.BackupHelper;

public class SettingsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Button btnSaveSettings, btnExportBackup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        btnBack = findViewById(R.id.btnBackSettings);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnExportBackup = findViewById(R.id.btnExportJsonBackup);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSaveSettings != null) {
            btnSaveSettings.setOnClickListener(v -> {
                Toast.makeText(this, "✓ Settings Saved Successfully! / සැකසීම් සුරකින ලදී", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnExportBackup != null) {
            btnExportBackup.setOnClickListener(v -> {
                BackupHelper.exportDatabaseToJson(SettingsActivity.this);
            });
        }
    }
}
