package com.example.possystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.possystem.data.AppDatabase;
import com.example.possystem.data.entity.ProductEntity;
import com.example.possystem.data.entity.ReturnEntity;
import com.example.possystem.data.entity.ReturnItemEntity;
import com.example.possystem.data.entity.SaleEntity;
import com.example.possystem.data.entity.SaleItemEntity;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class ReturnsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etInvoiceNo;
    private Button btnSearch, btnConfirmReturn;
    private LinearLayout layoutFoundInvoice, containerReturnItems;
    private TextView tvFoundInvoiceHeader, tvTotalRefund;

    private SaleEntity activeSale;
    private List<SaleItemEntity> activeSaleItems = new ArrayList<>();
    private Map<Integer, Double> returnQtyMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_returns);

        btnBack = findViewById(R.id.btnBackReturns);
        etInvoiceNo = findViewById(R.id.etSearchInvoiceNo);
        btnSearch = findViewById(R.id.btnSearchInvoice);
        btnConfirmReturn = findViewById(R.id.btnConfirmReturn);

        layoutFoundInvoice = findViewById(R.id.layoutFoundInvoice);
        containerReturnItems = findViewById(R.id.containerReturnItems);
        tvFoundInvoiceHeader = findViewById(R.id.tvFoundInvoiceHeader);
        tvTotalRefund = findViewById(R.id.tvTotalRefundAmount);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> performInvoiceSearch());
        }

        if (btnConfirmReturn != null) {
            btnConfirmReturn.setOnClickListener(v -> processReturnTransaction());
        }
    }

    private void performInvoiceSearch() {
        String query = etInvoiceNo.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Please enter an invoice number!", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<SaleEntity> allSales = db.saleDao().getAllSales();
            SaleEntity targetSale = null;

            if (allSales != null) {
                for (SaleEntity s : allSales) {
                    if (s.invoiceNumber != null && s.invoiceNumber.equalsIgnoreCase(query)) {
                        targetSale = s;
                        break;
                    }
                }
            }

            if (targetSale != null) {
                activeSale = targetSale;
                activeSaleItems = db.saleDao().getItemsForSale(targetSale.id);

                runOnUiThread(() -> renderInvoiceReturnItems());
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(ReturnsActivity.this, "⚠️ Invoice '" + query + "' not found!", Toast.LENGTH_LONG).show();
                    layoutFoundInvoice.setVisibility(View.GONE);
                });
            }
        });
    }

    private void renderInvoiceReturnItems() {
        if (activeSale == null || activeSaleItems == null || activeSaleItems.isEmpty()) {
            layoutFoundInvoice.setVisibility(View.GONE);
            return;
        }

        layoutFoundInvoice.setVisibility(View.VISIBLE);
        returnQtyMap.clear();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dateStr = sdf.format(new Date(activeSale.timestamp));
        tvFoundInvoiceHeader.setText("Invoice: #" + activeSale.invoiceNumber + " • Date: " + dateStr);

        containerReturnItems.removeAllViews();

        for (SaleItemEntity item : activeSaleItems) {
            returnQtyMap.put(item.id, 0.0);

            MaterialCardView card = new MaterialCardView(this);
            card.setCardBackgroundColor(getColor(R.color.white));
            card.setRadius(24f);
            card.setStrokeColor(getColor(R.color.card_stroke));
            card.setStrokeWidth(2);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 8, 0, 8);
            card.setLayoutParams(cardParams);

            LinearLayout itemRow = new LinearLayout(this);
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setPadding(24, 20, 24, 20);

            // Item Name & Sold Qty Info
            LinearLayout infoCol = new LinearLayout(this);
            infoCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            infoCol.setLayoutParams(infoParams);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(item.productName);
            tvTitle.setTextSize(15f);
            tvTitle.setTextColor(getColor(R.color.black));
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvSub = new TextView(this);
            tvSub.setText(String.format(Locale.getDefault(), "Sold: %.0f • Price: LKR %.2f", item.quantity, item.unitPrice));
            tvSub.setTextSize(12f);
            tvSub.setTextColor(getColor(R.color.text_secondary));

            infoCol.addView(tvTitle);
            infoCol.addView(tvSub);

            // Stepper Controls [- 0 +]
            LinearLayout stepperRow = new LinearLayout(this);
            stepperRow.setOrientation(LinearLayout.HORIZONTAL);
            stepperRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            Button btnMinus = new Button(this);
            btnMinus.setText("-");
            btnMinus.setTextSize(14f);
            btnMinus.setLayoutParams(new LinearLayout.LayoutParams(90, 90));

            TextView tvQty = new TextView(this);
            tvQty.setText("0");
            tvQty.setTextSize(15f);
            tvQty.setTextColor(getColor(R.color.black));
            tvQty.setTypeface(null, android.graphics.Typeface.BOLD);
            tvQty.setPadding(16, 0, 16, 0);

            Button btnPlus = new Button(this);
            btnPlus.setText("+");
            btnPlus.setTextSize(14f);
            btnPlus.setLayoutParams(new LinearLayout.LayoutParams(90, 90));

            btnMinus.setOnClickListener(v -> {
                double current = returnQtyMap.get(item.id);
                if (current > 0) {
                    current--;
                    returnQtyMap.put(item.id, current);
                    tvQty.setText(String.format(Locale.getDefault(), "%.0f", current));
                    recalculateTotalRefund();
                }
            });

            btnPlus.setOnClickListener(v -> {
                double current = returnQtyMap.get(item.id);
                if (current < item.quantity) {
                    current++;
                    returnQtyMap.put(item.id, current);
                    tvQty.setText(String.format(Locale.getDefault(), "%.0f", current));
                    recalculateTotalRefund();
                } else {
                    Toast.makeText(ReturnsActivity.this, "Cannot return more than sold quantity!", Toast.LENGTH_SHORT).show();
                }
            });

            stepperRow.addView(btnMinus);
            stepperRow.addView(tvQty);
            stepperRow.addView(btnPlus);

            itemRow.addView(infoCol);
            itemRow.addView(stepperRow);
            card.addView(itemRow);

            containerReturnItems.addView(card);
        }

        recalculateTotalRefund();
    }

    private void recalculateTotalRefund() {
        double refundTotal = 0.0;
        for (SaleItemEntity item : activeSaleItems) {
            double retQty = returnQtyMap.containsKey(item.id) ? returnQtyMap.get(item.id) : 0.0;
            refundTotal += (retQty * item.unitPrice);
        }

        tvTotalRefund.setText(String.format(Locale.getDefault(), "LKR %.2f", refundTotal));
    }

    private void processReturnTransaction() {
        double totalRefund = 0.0;
        List<ReturnItemEntity> returnItemsToInsert = new ArrayList<>();

        for (SaleItemEntity item : activeSaleItems) {
            double retQty = returnQtyMap.containsKey(item.id) ? returnQtyMap.get(item.id) : 0.0;
            if (retQty > 0) {
                double lineRefund = retQty * item.unitPrice;
                totalRefund += lineRefund;
                returnItemsToInsert.add(new ReturnItemEntity(0, item.productId, item.productName, retQty, item.unitPrice, lineRefund));
            }
        }

        if (returnItemsToInsert.isEmpty()) {
            Toast.makeText(this, "Please select at least 1 item quantity to return!", Toast.LENGTH_SHORT).show();
            return;
        }

        final double finalRefund = totalRefund;
        String returnInvNo = "RET-" + System.currentTimeMillis() % 100000;

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            // 1. Save Master Return Entity
            ReturnEntity returnMaster = new ReturnEntity(returnInvNo, activeSale.invoiceNumber, finalRefund, "Customer Refund");
            long returnId = db.returnDao().insertReturn(returnMaster);

            // 2. Save Return Items & Re-increase Product Stock in DB
            for (ReturnItemEntity ri : returnItemsToInsert) {
                ri.returnId = (int) returnId;

                ProductEntity p = db.productDao().getProductByBarcode(String.valueOf(ri.productId));
                if (p != null) {
                    db.productDao().updateStockQuantity(p.id, p.stockQuantity + ri.returnedQuantity);
                }
            }
            db.returnDao().insertReturnItems(returnItemsToInsert);

            runOnUiThread(() -> {
                Toast.makeText(ReturnsActivity.this, "✓ Return Processed! Refund: LKR " + String.format(Locale.getDefault(), "%.2f", finalRefund), Toast.LENGTH_LONG).show();
                finish();
            });
        });
    }
}
