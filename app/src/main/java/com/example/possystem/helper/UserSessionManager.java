package com.example.possystem.helper;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSessionManager {

    private static final String PREF_NAME = "POS_USER_SESSION";
    private static final String KEY_ROLE = "USER_ROLE";
    private static final String KEY_USERNAME = "USERNAME";

    public static void saveSession(Context context, String username, String role) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_ROLE, role != null ? role.toUpperCase() : "CASHIER")
                .apply();
    }

    public static String getRole(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString(KEY_ROLE, "ADMIN"); // Default to ADMIN for convenience
    }

    public static boolean isCashier(Context context) {
        return "CASHIER".equalsIgnoreCase(getRole(context));
    }

    public static boolean isAdmin(Context context) {
        return "ADMIN".equalsIgnoreCase(getRole(context));
    }

    public static void clearSession(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().clear().apply();
    }
}
