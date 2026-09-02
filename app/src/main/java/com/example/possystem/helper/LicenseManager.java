package com.example.possystem.helper;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LicenseManager {

    private static final String PREF_NAME = "POS_LICENSE_PREFS";

    // Keys
    private static final String KEY_IS_ACTIVATED = "IS_ACTIVATED";
    private static final String KEY_SHOP_ID = "SHOP_ID";
    private static final String KEY_SHOP_NAME = "SHOP_NAME";
    private static final String KEY_BUSINESS_TYPE = "BUSINESS_TYPE"; // RETAIL, PHARMACY, RESTAURANT, FASHION, HARDWARE
    private static final String KEY_SUPPORT_PIN = "SUPPORT_PIN";
    private static final String KEY_PLAN_TYPE = "PLAN_TYPE"; // "TRIAL" or "PAID"
    private static final String KEY_EXPIRY_TIMESTAMP = "EXPIRY_TIMESTAMP";
    private static final String KEY_LAST_KNOWN_TIMESTAMP = "LAST_KNOWN_TIMESTAMP";
    private static final String KEY_IS_SUSPENDED = "IS_SUSPENDED";
    private static final String KEY_ADDRESS = "ADDRESS";
    private static final String KEY_PHONE = "PHONE";
    private static final String KEY_RECEIPT_HEADER = "RECEIPT_HEADER";
    private static final String KEY_RECEIPT_FOOTER = "RECEIPT_FOOTER";
    private static final String KEY_LOGO_URL = "LOGO_URL";
    private static final String KEY_FEATURES_JSON = "FEATURES_JSON";

    // Secret Salt for Offline Cryptographic PIN Generation
    public static final String SECRET_SALT = "POS_LANKA_SEC_2026";
    public static final String SUPPORT_PHONE = "077 123 4567";
    public static final String SUPPORT_WHATSAPP = "+94771234567";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Check if app has been activated at least once
     */
    public static boolean isActivated(Context context) {
        return getPrefs(context).getBoolean(KEY_IS_ACTIVATED, false);
    }

    /**
     * Check if the shop's license is currently active and not expired
     */
    public static boolean isLicenseValid(Context context) {
        if (!isActivated(context)) {
            return false;
        }

        if (isSuspended(context)) {
            return false;
        }

        // Clock Tamper Protection Check
        if (isClockTampered(context)) {
            return false;
        }

        long expiryTime = getExpiryTimestamp(context);
        long currentTime = System.currentTimeMillis();

        // Update last known timestamp if current time is progressing normally
        if (currentTime >= getLastKnownTimestamp(context)) {
            updateLastKnownTimestamp(context, currentTime);
        }

        return currentTime <= expiryTime;
    }

    /**
     * Checks if user has intentionally set the phone's system time backwards
     */
    public static boolean isClockTampered(Context context) {
        long lastKnown = getLastKnownTimestamp(context);
        long current = System.currentTimeMillis();
        // Allow a small 5 minute grace margin in case of minor NTP sync shifts
        return lastKnown > 0 && current < (lastKnown - (5 * 60 * 1000L));
    }

    public static long getExpiryTimestamp(Context context) {
        return getPrefs(context).getLong(KEY_EXPIRY_TIMESTAMP, 0L);
    }

    public static long getLastKnownTimestamp(Context context) {
        return getPrefs(context).getLong(KEY_LAST_KNOWN_TIMESTAMP, 0L);
    }

    public static void updateLastKnownTimestamp(Context context, long timestamp) {
        getPrefs(context).edit().putLong(KEY_LAST_KNOWN_TIMESTAMP, timestamp).apply();
    }

    public static boolean isSuspended(Context context) {
        return getPrefs(context).getBoolean(KEY_IS_SUSPENDED, false);
    }

    public static void setSuspended(Context context, boolean suspended) {
        getPrefs(context).edit().putBoolean(KEY_IS_SUSPENDED, suspended).apply();
    }

    public static String getShopId(Context context) {
        return getPrefs(context).getString(KEY_SHOP_ID, "NOT_SET");
    }

    public static String getShopName(Context context) {
        return getPrefs(context).getString(KEY_SHOP_NAME, "My Retail Shop");
    }

    public static String getBusinessType(Context context) {
        return getPrefs(context).getString(KEY_BUSINESS_TYPE, "RETAIL");
    }

    public static void setBusinessType(Context context, String businessType) {
        getPrefs(context).edit().putString(KEY_BUSINESS_TYPE, businessType != null ? businessType.toUpperCase() : "RETAIL").apply();
    }

    public static String getAddress(Context context) {
        return getPrefs(context).getString(KEY_ADDRESS, "No. 45, Main Street");
    }

    public static String getPhone(Context context) {
        return getPrefs(context).getString(KEY_PHONE, "077 123 4567");
    }

    public static String getReceiptHeader(Context context) {
        return getPrefs(context).getString(KEY_RECEIPT_HEADER, "Welcome to " + getShopName(context));
    }

    public static String getReceiptFooter(Context context) {
        return getPrefs(context).getString(KEY_RECEIPT_FOOTER, "Thank you for shopping with us! • Exchange within 7 days");
    }

    public static String getLogoUrl(Context context) {
        return getPrefs(context).getString(KEY_LOGO_URL, "");
    }

    /**
     * Check if a specific modular feature is enabled in store hub
     */
    public static boolean isFeatureEnabled(Context context, String featureKey) {
        String featuresJson = getPrefs(context).getString(KEY_FEATURES_JSON, null);
        if (featuresJson == null || featuresJson.trim().isEmpty()) {
            return true; // Default enabled
        }
        try {
            org.json.JSONObject obj = new org.json.JSONObject(featuresJson);
            return obj.optBoolean(featureKey, true);
        } catch (Exception e) {
            return true;
        }
    }

    public static String getSupportPin(Context context) {
        return getPrefs(context).getString(KEY_SUPPORT_PIN, "000000");
    }

    public static String getPlanType(Context context) {
        return getPrefs(context).getString(KEY_PLAN_TYPE, "TRIAL");
    }

    public static boolean isTrial(Context context) {
        return "TRIAL".equalsIgnoreCase(getPlanType(context));
    }

    /**
     * Get remaining days in current subscription / trial
     */
    public static long getDaysRemaining(Context context) {
        long expiry = getExpiryTimestamp(context);
        long current = System.currentTimeMillis();
        if (expiry <= current) return 0;
        return (expiry - current) / (1000L * 60 * 60 * 24) + 1;
    }

    /**
     * Formatted string of Expiry Date e.g. "15-Oct-2026"
     */
    public static String getFormattedExpiryDate(Context context) {
        long expiry = getExpiryTimestamp(context);
        if (expiry == 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        return sdf.format(new Date(expiry));
    }

    /**
     * Activate the App with initial configuration
     */
    public static void activateShop(Context context, String shopId, String shopName, String supportPin, int durationDays, String planType) {
        activateShop(context, shopId, shopName, "RETAIL", supportPin, durationDays, planType);
    }

    public static void activateShop(Context context, String shopId, String shopName, String businessType, String supportPin, int durationDays, String planType) {
        long now = System.currentTimeMillis();
        long expiry = now + (durationDays * 24L * 60 * 60 * 1000L);

        getPrefs(context).edit()
                .putBoolean(KEY_IS_ACTIVATED, true)
                .putString(KEY_SHOP_ID, shopId.trim().toUpperCase())
                .putString(KEY_SHOP_NAME, shopName.trim())
                .putString(KEY_BUSINESS_TYPE, businessType != null ? businessType.toUpperCase() : "RETAIL")
                .putString(KEY_SUPPORT_PIN, supportPin.trim())
                .putString(KEY_PLAN_TYPE, planType != null ? planType.toUpperCase() : "TRIAL")
                .putLong(KEY_EXPIRY_TIMESTAMP, expiry)
                .putLong(KEY_LAST_KNOWN_TIMESTAMP, now)
                .putBoolean(KEY_IS_SUSPENDED, false)
                .apply();
    }

    /**
     * Extend subscription validity by additional days
     */
    public static void extendSubscription(Context context, int additionalDays, String newPlanType) {
        long currentExpiry = getExpiryTimestamp(context);
        long now = System.currentTimeMillis();
        long baseTime = Math.max(currentExpiry, now);
        long newExpiry = baseTime + (additionalDays * 24L * 60 * 60 * 1000L);

        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putLong(KEY_EXPIRY_TIMESTAMP, newExpiry);
        editor.putLong(KEY_LAST_KNOWN_TIMESTAMP, now);
        editor.putBoolean(KEY_IS_SUSPENDED, false);
        if (newPlanType != null) {
            editor.putString(KEY_PLAN_TYPE, newPlanType.toUpperCase());
        }
        editor.apply();
    }

    // =========================================================================
    // 🔐 OFFLINE CRYPTOGRAPHIC PIN VALIDATION
    // =========================================================================

    /**
     * Generates a 6-digit verification code based on parameters and Secret Salt.
     * Matches the PHP unpack('N') algorithm in XAMPP and JavaScript Web Panel.
     */
    public static String generateOfflineCode(String shopId, String actionKey) {
        try {
            String raw = shopId.trim().toUpperCase() + ":" + actionKey + ":" + SECRET_SALT;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            
            // Extract unsigned 32-bit big-endian integer (Exact match with PHP unpack('N'))
            long unsignedNumber = (((long)(hash[0] & 0xFF)) << 24)
                                | (((long)(hash[1] & 0xFF)) << 16)
                                | (((long)(hash[2] & 0xFF)) << 8)
                                | ((long)(hash[3] & 0xFF));

            // Return 6-digit numeric PIN
            return String.format(Locale.US, "%06d", (int)(unsignedNumber % 1000000L));
        } catch (Exception e) {
            return "999999";
        }
    }

    /**
     * Verify an entered offline renewal / unlock PIN and apply appropriate extension
     * @return true if PIN was valid and applied, false otherwise
     */
    public static boolean applyOfflineRenewalPin(Context context, String enteredPin) {
        if (enteredPin == null || enteredPin.trim().isEmpty()) return false;
        String cleanPin = enteredPin.trim();
        String shopId = getShopId(context);

        // 1. Check for +30 Days Paid Renewal PIN
        String pinPaid30 = generateOfflineCode(shopId, "RENEW_PAID_30");
        if (cleanPin.equals(pinPaid30)) {
            extendSubscription(context, 30, "PAID");
            return true;
        }

        // 2. Check for +14 Days Trial Renewal PIN
        String pinTrial14 = generateOfflineCode(shopId, "RENEW_TRIAL_14");
        if (cleanPin.equals(pinTrial14)) {
            extendSubscription(context, 14, "TRIAL");
            return true;
        }

        // 3. Check for +7 Days Trial Renewal PIN
        String pinTrial7 = generateOfflineCode(shopId, "RENEW_TRIAL_7");
        if (cleanPin.equals(pinTrial7)) {
            extendSubscription(context, 7, "TRIAL");
            return true;
        }

        // 4. Check for Emergency Unlock / Clock Reset PIN
        String pinEmergency = generateOfflineCode(shopId, "EMERGENCY_UNLOCK");
        if (cleanPin.equals(pinEmergency)) {
            // Reset clock guard to current time and add 3 days grace
            updateLastKnownTimestamp(context, System.currentTimeMillis());
            extendSubscription(context, 3, "PAID");
            return true;
        }

        return false;
    }

    // =========================================================================
    // 🌐 ONLINE LICENSE SYNC (AUTO-KILL SWITCH & REMOTE SUSPENSION)
    // =========================================================================

    public interface LicenseSyncListener {
        void onSyncComplete(boolean isBlocked, String message);
    }

    // =========================================================================
    // 🌐 PRODUCTION CLOUD / LIVE HOSTED DOMAIN URL
    // When hosting on cPanel / Cloud VPS, simply enter your live API URL below:
    // Example: "https://yourdomain.com/api/check_license.php"
    // =========================================================================
    public static final String PRODUCTION_SERVER_URL = "http://myposlanka.sahashrajanith.site/api/check_license.php"; 

    private static final String[] SERVER_ENDPOINTS = {
            PRODUCTION_SERVER_URL,
            "https://myposlanka.sahashrajanith.site/api/check_license.php",
            "http://myposlanka.sahashrajanith.site/api/check_license.php",
            "http://10.0.2.2/possystem/api/check_license.php",       // Android Emulator Loopback
            "http://10.190.222.16/possystem/api/check_license.php",  // Current Local Wi-Fi IP
            "http://192.168.8.162/possystem/api/check_license.php",  // Local Wi-Fi / LAN IP
            "http://localhost/possystem/api/check_license.php"       // Local Device
    };

    /**
     * Asynchronously pings the server to verify if admin has marked the store as inactive/suspended or extended the license.
     */
    public static void syncLicenseWithServer(Context context, LicenseSyncListener listener) {
        if (!isActivated(context)) {
            if (listener != null) listener.onSyncComplete(true, "Not activated");
            return;
        }
        String shopId = getShopId(context);

        new Thread(() -> {
            boolean isSuspendedOnServer = false;
            String serverMsg = "";

            for (String endpoint : SERVER_ENDPOINTS) {
                if (endpoint == null || endpoint.trim().isEmpty()) continue;
                try {
                    String fullUrl = endpoint + "?shop_id=" + shopId;
                    java.net.URL url = new java.net.URL(fullUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(2000);
                    conn.setReadTimeout(2000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();

                        org.json.JSONObject json = new org.json.JSONObject(response.toString());
                        if ("success".equalsIgnoreCase(json.optString("status"))) {
                            boolean isActive = json.optBoolean("is_active", true);
                            boolean isExpired = json.optBoolean("is_expired", false);
                            long serverExpiryMs = json.optLong("expiry_timestamp", 0);
                            String planType = json.optString("plan_type", "TRIAL");
                            String shopName = json.optString("shop_name", "");
                            String businessType = json.optString("business_type", "RETAIL");
                            String address = json.optString("address", "");
                            String phone = json.optString("phone", "");
                            String header = json.optString("receipt_header", "");
                            String footer = json.optString("receipt_footer", "");
                            String logoUrl = json.optString("logo_url", "");
                            org.json.JSONObject featuresObj = json.optJSONObject("features");

                            SharedPreferences.Editor editor = getPrefs(context).edit();
                            if (!shopName.isEmpty()) editor.putString(KEY_SHOP_NAME, shopName);
                            if (!businessType.isEmpty()) editor.putString(KEY_BUSINESS_TYPE, businessType);
                            if (!address.isEmpty()) editor.putString(KEY_ADDRESS, address);
                            if (!phone.isEmpty()) editor.putString(KEY_PHONE, phone);
                            if (!header.isEmpty()) editor.putString(KEY_RECEIPT_HEADER, header);
                            if (!footer.isEmpty()) editor.putString(KEY_RECEIPT_FOOTER, footer);
                            if (!logoUrl.isEmpty()) editor.putString(KEY_LOGO_URL, logoUrl);
                            if (featuresObj != null) editor.putString(KEY_FEATURES_JSON, featuresObj.toString());

                            if (!isActive) {
                                // 🚫 Admin marked as INACTIVE / SUSPENDED!
                                isSuspendedOnServer = true;
                                editor.putBoolean(KEY_IS_SUSPENDED, true);
                                editor.apply();
                                serverMsg = "Your store access has been disabled by Admin.";
                            } else {
                                // ✅ Active on Server
                                editor.putBoolean(KEY_IS_SUSPENDED, false);
                                if (serverExpiryMs > 0) {
                                    editor.putLong(KEY_EXPIRY_TIMESTAMP, serverExpiryMs);
                                    editor.putString(KEY_PLAN_TYPE, planType);
                                }
                                editor.apply();

                                if (isExpired) {
                                    isSuspendedOnServer = true;
                                    serverMsg = "Subscription has expired.";
                                }
                            }
                            break; // Successfully queried
                        }
                    }
                } catch (Exception ignored) {
                    // Try next endpoint if this one fails
                }
            }

            final boolean finalBlocked = isSuspendedOnServer;
            final String finalMsg = serverMsg;

            if (listener != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    listener.onSyncComplete(finalBlocked, finalMsg);
                });
            }
        }).start();
    }
}
