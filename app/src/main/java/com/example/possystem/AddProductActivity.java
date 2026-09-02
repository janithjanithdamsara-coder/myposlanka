package com.example.possystem;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.example.possystem.data.AppDatabase;
import com.example.possystem.data.entity.ProductEntity;
import com.example.possystem.helper.BarcodeLookupHelper;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.concurrent.Executors;

public class AddProductActivity extends AppCompatActivity {

    private EditText etBarcode, etName, etPurchasePrice, etSellingPrice, etWholesalePrice, etStockQty, etMinStockAlert, etExpiryDate, etBatchNo;
    private CheckBox cbIsWeightBased;
    private Button btnSave, btnScan, btnAutoFill;
    private ImageView btnBack;

    // ZXing Barcode Scanner Activity Result Launcher
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            String scannedBarcode = result.getContents().trim();
            Log.d("POS_BARCODE", "Camera Scanned Barcode: " + scannedBarcode);
            if (etBarcode != null) {
                etBarcode.setText(scannedBarcode);
            }
            performBarcodeAutoFill(scannedBarcode);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        // Bind Views
        btnBack = findViewById(R.id.btnBackAddProduct);
        etBarcode = findViewById(R.id.etAddProductBarcode);
        btnScan = findViewById(R.id.btnScanProductBarcode);
        btnAutoFill = findViewById(R.id.btnAutoFillBarcode);
        etName = findViewById(R.id.etAddProductName);
        etPurchasePrice = findViewById(R.id.etAddPurchasePrice);
        etSellingPrice = findViewById(R.id.etAddSellingPrice);
        etWholesalePrice = findViewById(R.id.etAddWholesalePrice);
        cbIsWeightBased = findViewById(R.id.cbIsWeightBased);
        etStockQty = findViewById(R.id.etAddStockQty);
        etMinStockAlert = findViewById(R.id.etAddMinStockAlert);
        etExpiryDate = findViewById(R.id.etAddExpiryDate);
        etBatchNo = findViewById(R.id.etAddBatchNo);
        btnSave = findViewById(R.id.btnSaveProduct);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnScan != null) {
            btnScan.setOnClickListener(v -> launchCameraBarcodeScanner());
        }

        if (btnAutoFill != null) {
            btnAutoFill.setOnClickListener(v -> {
                String barcode = etBarcode != null ? etBarcode.getText().toString().trim() : "";
                if (TextUtils.isEmpty(barcode)) {
                    Toast.makeText(AddProductActivity.this, "Please enter or scan a barcode first!", Toast.LENGTH_SHORT).show();
                } else {
                    performBarcodeAutoFill(barcode);
                }
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveProductToDatabase());
        }

        // Check if barcode was passed from POS Screen
        String passedBarcode = getIntent().getStringExtra("BARCODE");
        if (!TextUtils.isEmpty(passedBarcode)) {
            if (etBarcode != null) etBarcode.setText(passedBarcode);
            performBarcodeAutoFill(passedBarcode);
        }

        // Auto-fill product details when barcode length is valid
        if (etBarcode != null) {
            etBarcode.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() >= 4) {
                        performBarcodeAutoFill(s.toString().trim());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void performBarcodeAutoFill(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) return;

        Log.d("POS_BARCODE", "Performing lookup for barcode: " + barcode);
        Toast.makeText(AddProductActivity.this, "🔍 Searching Product Database...", Toast.LENGTH_SHORT).show();

        BarcodeLookupHelper.lookupBarcode(barcode, (name, brand) -> {
            Log.d("POS_BARCODE", "Lookup result for " + barcode + ": " + name);
            if (name != null && !name.trim().isEmpty() && !name.equalsIgnoreCase("null")) {
                if (etName != null) {
                    etName.setText(name); // Always force overwrite name field!
                    Toast.makeText(AddProductActivity.this, "✨ Auto-filled Product Name: " + name, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(AddProductActivity.this, "⚠️ No auto-fill info for barcode: " + barcode + ". Type name manually.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void launchCameraBarcodeScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan Product Barcode / බාර්කෝඩ් එක Scan කරන්න");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setCaptureActivity(CaptureActivity.class);
        barcodeLauncher.launch(options);
    }

    private void saveProductToDatabase() {
        String name = etName.getText().toString().trim();
        String barcode = etBarcode.getText().toString().trim();
        String purchasePriceStr = etPurchasePrice.getText().toString().trim();
        String sellingPriceStr = etSellingPrice.getText().toString().trim();
        String stockQtyStr = etStockQty.getText().toString().trim();
        String minStockStr = etMinStockAlert.getText().toString().trim();
        String expiryDate = etExpiryDate.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Product Name is required!");
            return;
        }

        if (TextUtils.isEmpty(sellingPriceStr)) {
            etSellingPrice.setError("Selling Price is required!");
            return;
        }

        double purchasePrice = TextUtils.isEmpty(purchasePriceStr) ? 0.0 : Double.parseDouble(purchasePriceStr);
        double sellingPrice = Double.parseDouble(sellingPriceStr);
        double stockQty = TextUtils.isEmpty(stockQtyStr) ? 0.0 : Double.parseDouble(stockQtyStr);
        double minStock = TextUtils.isEmpty(minStockStr) ? 5.0 : Double.parseDouble(minStockStr);
        boolean isWeightBased = cbIsWeightBased != null && cbIsWeightBased.isChecked();

        ProductEntity newProduct = new ProductEntity(
                barcode,
                name,
                purchasePrice,
                sellingPrice,
                isWeightBased ? "kg" : "pcs",
                isWeightBased,
                stockQty,
                minStock,
                expiryDate
        );

        // Execute Room DB Insertion with Duplicate Barcode Prevention
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            
            if (!TextUtils.isEmpty(barcode)) {
                ProductEntity existing = db.productDao().getProductByBarcode(barcode);
                if (existing != null) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddProductActivity.this, "⚠️ Barcode already exists for '" + existing.name + "'!", Toast.LENGTH_LONG).show();
                    });
                    return;
                }
            }

            db.productDao().insert(newProduct);

            runOnUiThread(() -> {
                Toast.makeText(AddProductActivity.this, "Product Saved Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
