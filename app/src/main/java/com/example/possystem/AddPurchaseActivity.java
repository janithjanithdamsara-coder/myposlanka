package com.example.possystem;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.possystem.data.AppDatabase;
import com.example.possystem.data.entity.ProductEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class AddPurchaseActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spnSuppliers;
    private EditText etInvoiceNo, etPurchaseDate, etSearchProduct, etPaidAmount;
    private Button btnAddItem, btnSavePurchase;
    private TextView tvGrandTotal, tvDueBalance;

    private List<ProductEntity> dbProducts = new ArrayList<>();
    private ProductEntity selectedProduct;
    private double currentGrandTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_purchase);

        btnBack = findViewById(R.id.btnBackAddPurchase);
        spnSuppliers = findViewById(R.id.spnSuppliers);
        etInvoiceNo = findViewById(R.id.etPurchaseInvoiceNo);
        etPurchaseDate = findViewById(R.id.etPurchaseDate);
        etSearchProduct = findViewById(R.id.etSearchPurchaseItem);
        etPaidAmount = findViewById(R.id.etPurchasePaidAmount);
        btnAddItem = findViewById(R.id.btnAddPurchaseItem);
        btnSavePurchase = findViewById(R.id.btnSavePurchase);
        tvGrandTotal = findViewById(R.id.tvPurchaseGrandTotal);
        tvDueBalance = findViewById(R.id.tvPurchaseDueBalance);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Set default invoice no & date
        if (etInvoiceNo != null) etInvoiceNo.setText("PUR-" + (System.currentTimeMillis() % 100000));
        if (etPurchaseDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etPurchaseDate.setText(sdf.format(new Date()));
        }

        // Populate Suppliers Spinner
        if (spnSuppliers != null) {
            String[] defaultSuppliers = {"Ceylon Grain Traders", "Elephant House Wholesalers", "Maliban Distributers", "General Supplier"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, defaultSuppliers);
            spnSuppliers.setAdapter(adapter);
        }

        // Load Products for selection
        loadProductsFromDb();

        if (btnAddItem != null) {
            btnAddItem.setOnClickListener(v -> addSelectedProductToPurchase());
        }

        if (etPaidAmount != null) {
            etPaidAmount.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    recalculateDueBalance();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnSavePurchase != null) {
            btnSavePurchase.setOnClickListener(v -> savePurchaseAndUpdateStock());
        }
    }

    private void loadProductsFromDb() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            dbProducts = db.productDao().getAllProducts();
        });
    }

    private void addSelectedProductToPurchase() {
        String query = etSearchProduct.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Please type or scan a product name/barcode!", Toast.LENGTH_SHORT).show();
            return;
        }

        for (ProductEntity p : dbProducts) {
            if ((p.name != null && p.name.toLowerCase().contains(query.toLowerCase())) ||
                (p.barcode != null && p.barcode.contains(query))) {
                selectedProduct = p;
                currentGrandTotal += (p.purchasePrice * 10.0); // Demo 10 qty purchase
                if (tvGrandTotal != null) tvGrandTotal.setText(String.format("LKR %.2f", currentGrandTotal));
                recalculateDueBalance();
                Toast.makeText(this, "Added 10x '" + p.name + "' to Purchase list!", Toast.LENGTH_SHORT).show();
                etSearchProduct.setText("");
                return;
            }
        }

        Toast.makeText(this, "No matching product found in shop catalog!", Toast.LENGTH_SHORT).show();
    }

    private void recalculateDueBalance() {
        double paid = 0.0;
        if (etPaidAmount != null && !TextUtils.isEmpty(etPaidAmount.getText())) {
            try {
                paid = Double.parseDouble(etPaidAmount.getText().toString());
            } catch (Exception ignored) {}
        }

        double due = Math.max(0.0, currentGrandTotal - paid);
        if (tvDueBalance != null) tvDueBalance.setText(String.format("LKR %.2f", due));
    }

    private void savePurchaseAndUpdateStock() {
        if (selectedProduct == null && currentGrandTotal <= 0) {
            Toast.makeText(this, "Please add at least 1 product to purchase!", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            if (selectedProduct != null) {
                // Product Stock + 10 and update stock in Room DB
                double newStock = selectedProduct.stockQuantity + 10.0;
                db.productDao().updateStockQuantity(selectedProduct.id, newStock);
            }

            runOnUiThread(() -> {
                Toast.makeText(AddPurchaseActivity.this, "✓ Purchase Saved! Product Stock increased by +10!", Toast.LENGTH_LONG).show();
                finish();
            });
        });
    }
}
