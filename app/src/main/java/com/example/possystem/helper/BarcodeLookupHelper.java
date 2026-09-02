package com.example.possystem.helper;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class BarcodeLookupHelper {

    public interface LookupCallback {
        void onResult(String productName, String brand);
    }

    // Pre-loaded Sri Lankan Master Dictionary (0ms Instant Offline Response)
    private static final Map<String, String[]> LOCAL_DICTIONARY = new HashMap<>();

    static {
        LOCAL_DICTIONARY.put("4791066000863", new String[]{"Elephant House Double Delight Ice Cream 2L", "Elephant House"});
        LOCAL_DICTIONARY.put("4791066101072", new String[]{"Elephant House Soda 1L", "Elephant House"});
        LOCAL_DICTIONARY.put("4791066100013", new String[]{"Elephant House Cream Soda 1.5L", "Elephant House"});
        LOCAL_DICTIONARY.put("4791066100020", new String[]{"Elephant House Necto 1.5L", "Elephant House"});
        LOCAL_DICTIONARY.put("4791066100037", new String[]{"Elephant House Orange Barley 1.5L", "Elephant House"});
        
        LOCAL_DICTIONARY.put("4792018000010", new String[]{"Anchor Milk Powder 400g", "Fonterra"});
        LOCAL_DICTIONARY.put("4792018000027", new String[]{"Anchor Milk Powder 1kg", "Fonterra"});

        LOCAL_DICTIONARY.put("4791017000011", new String[]{"Munchee Super Cream Cracker 190g", "CBL"});
        LOCAL_DICTIONARY.put("4791017000028", new String[]{"Munchee Hawaii Biscuits 200g", "CBL"});

        LOCAL_DICTIONARY.put("4791008000015", new String[]{"Maliban Gold Marie 80g", "Maliban"});
        LOCAL_DICTIONARY.put("4791008000022", new String[]{"Maliban Cream Cracker 190g", "Maliban"});

        LOCAL_DICTIONARY.put("4793001169901", new String[]{"Harischandra Coffee Powder 100g", "Harischandra"});
        LOCAL_DICTIONARY.put("4793001169918", new String[]{"Harischandra Kurakkan Flour 400g", "Harischandra"});

        LOCAL_DICTIONARY.put("4792003000018", new String[]{"Highland Fresh Milk 1L", "Highland"});
        LOCAL_DICTIONARY.put("4792003000025", new String[]{"Highland Salted Butter 200g", "Highland"});
    }

    public static void lookupBarcode(String barcode, LookupCallback callback) {
        if (barcode == null || barcode.trim().isEmpty()) {
            callback.onResult(null, null);
            return;
        }

        String cleanBarcode = barcode.trim();
        Log.d("POS_BARCODE", "Starting multi-tier lookup for: " + cleanBarcode);

        // 1. Tier 1: Check Local Sri Lanka Master Dictionary (0ms Instant Offline Response)
        if (LOCAL_DICTIONARY.containsKey(cleanBarcode)) {
            String[] data = LOCAL_DICTIONARY.get(cleanBarcode);
            Log.d("POS_BARCODE", "Tier 1 Match: " + data[0]);
            callback.onResult(data[0], data[1]);
            return;
        }

        // Run Multi-tier Network APIs on Background Thread
        Executors.newSingleThreadExecutor().execute(() -> {

            // 2. Tier 2: Query UPCitemdb API (https://api.upcitemdb.com/prod/trial/lookup?upc=...)
            String[] upcResult = fetchFromUPCitemdb(cleanBarcode);
            if (upcResult != null && upcResult[0] != null && !upcResult[0].isEmpty()) {
                Log.d("POS_BARCODE", "Tier 2 Match (UPCitemdb): " + upcResult[0]);
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(upcResult[0], upcResult[1]));
                return;
            }

            // 3. Tier 3: Query Open Food Facts API (https://world.openfoodfacts.org/api/v2/product/...)
            String foodProductName = fetchFromOpenFoodFacts(cleanBarcode);
            if (foodProductName != null && !foodProductName.isEmpty()) {
                Log.d("POS_BARCODE", "Tier 3 Match (OpenFoodFacts): " + foodProductName);
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(foodProductName, "Grocery"));
                return;
            }

            // 4. Tier 4: Query Google Books API (For Books / ISBN Barcodes)
            String bookTitle = fetchFromGoogleBooks(cleanBarcode);
            if (bookTitle != null && !bookTitle.isEmpty()) {
                Log.d("POS_BARCODE", "Tier 4 Match (Google Books): " + bookTitle);
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(bookTitle, "Book"));
                return;
            }

            // 5. Tier 5: GS1 Sri Lanka Manufacturer Brand Detection
            String manufacturer = detectSriLankanManufacturerPrefix(cleanBarcode);
            if (manufacturer != null) {
                String suggestion = manufacturer + " (" + cleanBarcode + ")";
                Log.d("POS_BARCODE", "Tier 5 Match (GS1 Manufacturer): " + suggestion);
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(suggestion, "Sri Lanka"));
                return;
            }

            // 6. Tier 6: Manual Entry Fallback ("Item-[Barcode]")
            String defaultName = "Item-" + cleanBarcode;
            Log.d("POS_BARCODE", "Tier 6 Fallback: " + defaultName);
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(defaultName, "General"));
        });
    }

    private static String[] fetchFromUPCitemdb(String barcode) {
        try {
            String apiUrl = "https://api.upcitemdb.com/prod/trial/lookup?upc=" + barcode;
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "POSSystem-AndroidApp/1.0");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject response = new JSONObject(sb.toString());
                if (response.optString("code", "").equalsIgnoreCase("OK") && response.optInt("total", 0) > 0) {
                    JSONArray items = response.optJSONArray("items");
                    if (items != null && items.length() > 0) {
                        JSONObject item = items.getJSONObject(0);
                        String title = item.optString("title", null);
                        String brand = item.optString("brand", "");
                        if (title != null && !title.isEmpty()) {
                            return new String[]{title, brand};
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("POS_BARCODE", "UPCitemdb lookup error: " + e.getMessage());
        }
        return null;
    }

    private static String fetchFromOpenFoodFacts(String barcode) {
        try {
            String apiUrl = "https://world.openfoodfacts.org/api/v2/product/" + barcode + ".json";
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "POSSystem-AndroidApp/1.0");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject response = new JSONObject(sb.toString());
                if (response.optInt("status") == 1 && response.has("product")) {
                    JSONObject product = response.getJSONObject("product");
                    String name = product.optString("product_name", null);
                    if (name == null || name.isEmpty() || name.equalsIgnoreCase("null")) {
                        name = product.optString("product_name_en", null);
                    }
                    if (name == null || name.isEmpty() || name.equalsIgnoreCase("null")) {
                        name = product.optString("generic_name", null);
                    }
                    return name;
                }
            }
        } catch (Exception e) {
            Log.e("POS_BARCODE", "OpenFoodFacts lookup error: " + e.getMessage());
        }
        return null;
    }

    private static String fetchFromGoogleBooks(String barcode) {
        try {
            String apiUrl = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + barcode;
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject response = new JSONObject(sb.toString());
                if (response.optInt("totalItems", 0) > 0 && response.has("items")) {
                    JSONArray items = response.getJSONArray("items");
                    if (items.length() > 0) {
                        JSONObject volumeInfo = items.getJSONObject(0).getJSONObject("volumeInfo");
                        return volumeInfo.optString("title", null);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("POS_BARCODE", "Google Books lookup error: " + e.getMessage());
        }
        return null;
    }

    private static String detectSriLankanManufacturerPrefix(String barcode) {
        if (!barcode.startsWith("479")) return null;

        if (barcode.startsWith("4791066")) return "Elephant House Product";
        if (barcode.startsWith("4793001")) return "Harischandra Product";
        if (barcode.startsWith("4791017")) return "Munchee CBL Product";
        if (barcode.startsWith("4791008")) return "Maliban Product";
        if (barcode.startsWith("4792018")) return "Anchor Fonterra Product";
        if (barcode.startsWith("4792003")) return "Highland Dairy Product";
        if (barcode.startsWith("4792004")) return "Pelwatte Milk Product";
        if (barcode.startsWith("4791022")) return "Unilever Product";
        if (barcode.startsWith("4791033")) return "Nestle Product";
        if (barcode.startsWith("4791005")) return "Cargills Kist Product";

        return "Sri Lanka Product";
    }
}
