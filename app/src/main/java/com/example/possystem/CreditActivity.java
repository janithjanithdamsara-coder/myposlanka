package com.example.possystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class CreditActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit_drawer);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        // Back Button
        if (findViewById(R.id.btnBackCredit) != null) {
            findViewById(R.id.btnBackCredit).setOnClickListener(v -> finish());
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(CreditActivity.this, MainActivity.class));
            finish();
        } else if (id == R.id.nav_pos) {
            startActivity(new Intent(CreditActivity.this, PosActivity.class));
            finish();
        } else if (id == R.id.nav_products) {
            startActivity(new Intent(CreditActivity.this, ProductsActivity.class));
            finish();
        } else if (id == R.id.nav_inventory) {
            startActivity(new Intent(CreditActivity.this, InventoryActivity.class));
            finish();
        } else if (id == R.id.nav_credit) {
            // Already on Credit
        } else if (id == R.id.nav_logout) {
            startActivity(new Intent(CreditActivity.this, LoginActivity.class));
            finish();
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
