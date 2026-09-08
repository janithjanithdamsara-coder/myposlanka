package com.example.possystem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.possystem.helper.LicenseManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiptActivity extends AppCompatActivity {

    private Button btnDone, btnPrintBottom;
    private ImageView btnBack, btnPrintTop;
    private TextView tvInvoiceNo, tvDate, tvGrandTotal, tvCashGiven, tvChangeReturned, tvReceiptShopName;

    private String currentInvoiceNo = "";
    private double currentGrandTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        btnDone = findViewById(R.id.btnDoneReceipt);
        btnPrintBottom = findViewById(R.id.btnPrintReceiptBottom);
        btnBack = findViewById(R.id.btnBackReceipt);
        btnPrintTop = findViewById(R.id.btnPrintReceipt);

        tvReceiptShopName = findViewById(R.id.tvReceiptShopName);
        tvInvoiceNo = findViewById(R.id.tvReceiptInvoiceNo);
        tvDate = findViewById(R.id.tvReceiptDate);
        tvGrandTotal = findViewById(R.id.tvReceiptGrandTotal);
        tvCashGiven = findViewById(R.id.tvReceiptCashGiven);
        tvChangeReturned = findViewById(R.id.tvReceiptChangeReturned);

        if (tvReceiptShopName != null) {
            tvReceiptShopName.setText(LicenseManager.getShopName(this));
        }

        // Populate intent data
        Intent intent = getIntent();
        if (intent != null) {
            currentInvoiceNo = intent.getStringExtra("INVOICE_NO");
            if (currentInvoiceNo == null) currentInvoiceNo = "INV-" + (System.currentTimeMillis() % 100000);
            currentGrandTotal = intent.getDoubleExtra("GRAND_TOTAL", 0.0);
            double paidAmount = intent.getDoubleExtra("PAID_AMOUNT", 0.0);
            double changeAmount = intent.getDoubleExtra("CHANGE_AMOUNT", 0.0);

            if (tvInvoiceNo != null) tvInvoiceNo.setText("Invoice: #" + currentInvoiceNo);
            if (tvGrandTotal != null) tvGrandTotal.setText(String.format(Locale.US, "LKR %.2f", currentGrandTotal));
            if (tvCashGiven != null) tvCashGiven.setText(String.format(Locale.US, "LKR %.2f", paidAmount));
            if (tvChangeReturned != null) tvChangeReturned.setText(String.format(Locale.US, "LKR %.2f", changeAmount));
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
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Receipt Action / බිල්පත");
        builder.setItems(new CharSequence[]{"🖨️ Thermal Bluetooth Print", "📲 Send Digital Bill via WhatsApp", "✓ Done"}, (d, which) -> {
            if (which == 0) {
                Toast.makeText(this, "🖨️ Sending to Thermal Bluetooth Printer...", Toast.LENGTH_SHORT).show();
                finish();
            } else if (which == 1) {
                shareReceiptViaWhatsApp();
            } else {
                finish();
            }
        });
        builder.show();
    }

    private void shareReceiptViaWhatsApp() {
        String shopName = LicenseManager.getShopName(this);
        String message = "🧾 *" + shopName + " - Digital Receipt*\n" +
                "• Invoice No: #" + currentInvoiceNo + "\n" +
                "• Total Amount: LKR " + String.format(Locale.US, "%.2f", currentGrandTotal) + "\n" +
                "• Status: PAID ✅\n\n" +
                "Thank you for shopping with us! 🙏";
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(message)));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp is not installed.", Toast.LENGTH_SHORT).show();
        }
    }
}
