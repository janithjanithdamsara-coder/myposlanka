package com.example.possystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiptActivity extends AppCompatActivity {

    private Button btnDone, btnPrintBottom;
    private ImageView btnBack, btnPrintTop;
    private TextView tvInvoiceNo, tvDate, tvGrandTotal, tvCashGiven, tvChangeReturned;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        btnDone = findViewById(R.id.btnDoneReceipt);
        btnPrintBottom = findViewById(R.id.btnPrintReceiptBottom);
        btnBack = findViewById(R.id.btnBackReceipt);
        btnPrintTop = findViewById(R.id.btnPrintReceipt);

        tvInvoiceNo = findViewById(R.id.tvReceiptInvoiceNo);
        tvDate = findViewById(R.id.tvReceiptDate);
        tvGrandTotal = findViewById(R.id.tvReceiptGrandTotal);
        tvCashGiven = findViewById(R.id.tvReceiptCashGiven);
        tvChangeReturned = findViewById(R.id.tvReceiptChangeReturned);

        // Populate intent data
        Intent intent = getIntent();
        if (intent != null) {
            String invoiceNo = intent.getStringExtra("INVOICE_NO");
            double grandTotal = intent.getDoubleExtra("GRAND_TOTAL", 0.0);
            double paidAmount = intent.getDoubleExtra("PAID_AMOUNT", 0.0);
            double changeAmount = intent.getDoubleExtra("CHANGE_AMOUNT", 0.0);

            if (tvInvoiceNo != null && invoiceNo != null) tvInvoiceNo.setText("Invoice: #" + invoiceNo);
            if (tvGrandTotal != null) tvGrandTotal.setText(String.format("LKR %.2f", grandTotal));
            if (tvCashGiven != null) tvCashGiven.setText(String.format("LKR %.2f", paidAmount));
            if (tvChangeReturned != null) tvChangeReturned.setText(String.format("LKR %.2f", changeAmount));
        }

        if (tvDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            tvDate.setText("Date: " + sdf.format(new Date()));
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Complete sale without printing
        if (btnDone != null) {
            btnDone.setOnClickListener(v -> {
                Toast.makeText(this, "Sale Completed Successfully! / ගනුදෙනුව අවසන්!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        // Print receipt
        if (btnPrintBottom != null) {
            btnPrintBottom.setOnClickListener(v -> printReceiptAction());
        }

        if (btnPrintTop != null) {
            btnPrintTop.setOnClickListener(v -> printReceiptAction());
        }
    }

    private void printReceiptAction() {
        Toast.makeText(this, "🖨️ Printing Receipt... / බිල්පත මුද්‍රණය වේ", Toast.LENGTH_SHORT).show();
        finish();
    }
}
