package com.example.ai;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

public class ApiKeyManager {
    private static final String PREF_NAME = "vynara_secure_prefs";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";
    private static final String KEY_SELECTED_MODEL = "selected_gemini_model";

    private SharedPreferences prefs;

    public ApiKeyManager(Context context) {
        Context appContext = context.getApplicationContext();
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            this.prefs = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    appContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Phase 26 Alignment: Fallback to private SharedPreferences if Android Keystore is unavailable
            this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public void saveApiKey(String apiKey) {
        if (apiKey != null) {
            prefs.edit().putString(KEY_GEMINI_API_KEY, apiKey.trim()).apply();
        }
    }

    public String getApiKey() {
        return prefs.getString(KEY_GEMINI_API_KEY, "");
    }

    public boolean hasApiKey() {
        String key = getApiKey();
        return key != null && !key.trim().isEmpty();
    }

    public String getMaskedApiKey() {
        String key = getApiKey();
        if (key == null || key.length() < 8) {
            return "••••••••••••••••";
        }
        return key.substring(0, 4) + "••••••••••••" + key.substring(key.length() - 3);
    }

    public void saveSelectedModel(String modelId) {
        if (modelId != null) {
            prefs.edit().putString(KEY_SELECTED_MODEL, modelId.trim()).apply();
        }
    }

    public String getSelectedModel() {
        // Return blank by default, forcing the app to dynamically register and bind 
        // to a valid model returned from the Google server instead of guessing a deprecated default
        return prefs.getString(KEY_SELECTED_MODEL, "");
    }

    public void clearApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply();
    }
}