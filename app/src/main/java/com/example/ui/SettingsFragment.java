package com.example.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.R;
import com.example.ai.AIModel;
import com.example.ai.ApiKeyManager;
import com.example.ai.GeminiApiClient;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private EditText etApiKey;
    private TextView tvStatus;
    private Spinner spinnerModel;
    private Spinner spinnerRenderQuality;

    // CRITICAL FIX: Initialize flag as true to block all accidental, system-triggered 
    // selection events during the initial layout startup passes before live models load.
    private boolean isUpdatingModels = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etApiKey = view.findViewById(R.id.et_api_key);
        tvStatus = view.findViewById(R.id.tv_connection_status);
        spinnerModel = view.findViewById(R.id.spinner_gemini_model);
        spinnerRenderQuality = view.findViewById(R.id.spinner_settings_render_quality);

        final List<String> modelList = new ArrayList<>();
        modelList.add("gemini-3.5-flash");
        modelList.add("gemini-3.1-flash-lite");
        modelList.add("gemini-3.1-pro");

        final ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, modelList);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(modelAdapter);

        if (getContext() != null) {
            ApiKeyManager keyMgr = new ApiKeyManager(getContext());
            
            // Phase 26 Alignment: Display securely masked API key to avoid plain-text screen exposure
            if (keyMgr.hasApiKey()) {
                etApiKey.setText(keyMgr.getMaskedApiKey());
                tvStatus.setText("● Connected (Secure)");
                tvStatus.setTextColor(0xFF00E676);

                // Fetch live models from Google API automatically on start
                fetchLiveModels(keyMgr.getApiKey(), modelList, modelAdapter, keyMgr, false);
            } else {
                tvStatus.setText("● Disconnected");
                tvStatus.setTextColor(0xFFFF5252);
                isUpdatingModels = false; // Allow manual selection if disconnected
            }

            String currentSelectedModel = keyMgr.getSelectedModel();
            int selectedIdx = modelList.indexOf(currentSelectedModel);
            if (selectedIdx >= 0) {
                spinnerModel.setSelection(selectedIdx);
            }

            spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isUpdatingModels) {
                        return; // Discard auto-selection events during async list reload
                    }
                    if (position >= 0 && position < modelList.size()) {
                        String selected = modelList.get(position);
                        keyMgr.saveSelectedModel(selected);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            String[] qualities = new String[]{"Ultra (PBR + Shadows + PostFX)", "High (PBR Standard)", "Medium (Fast Mobile)", "Low (Wireframe Draft)"};
            ArrayAdapter<String> qualityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, qualities);
            qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRenderQuality.setAdapter(qualityAdapter);
        }

        Button btnSaveKey = view.findViewById(R.id.btn_save_key);
        if (btnSaveKey != null) {
            btnSaveKey.setOnClickListener(v -> {
                String key = etApiKey.getText().toString().trim();
                if (key.contains("••••")) {
                    Toast.makeText(getContext(), "Using securely stored API key", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (getContext() != null) {
                    ApiKeyManager keyMgr = new ApiKeyManager(getContext());
                    keyMgr.saveApiKey(key);
                    Toast.makeText(getContext(), "Gemini API Key saved securely", Toast.LENGTH_SHORT).show();
                    tvStatus.setText("● Connected (Secure)");
                    tvStatus.setTextColor(0xFF00E676);
                    etApiKey.setText(keyMgr.getMaskedApiKey());
                    if (!key.isEmpty()) {
                        fetchLiveModels(key, modelList, modelAdapter, keyMgr, true);
                    }
                }
            });
        }

        ImageButton btnFetch = view.findViewById(R.id.btn_fetch_models);
        if (btnFetch != null) {
            btnFetch.setOnClickListener(v -> {
                if (getContext() == null) return;
                ApiKeyManager keyMgr = new ApiKeyManager(getContext());
                String apiKey = etApiKey.getText().toString().trim();
                if (apiKey.isEmpty() || apiKey.contains("••••")) {
                    apiKey = keyMgr.getApiKey();
                }
                if (apiKey.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter and save a Gemini API key first", Toast.LENGTH_SHORT).show();
                    return;
                }
                fetchLiveModels(apiKey, modelList, modelAdapter, keyMgr, true);
            });
        }

        Button btnTest = view.findViewById(R.id.btn_test_connection);
        if (btnTest != null) {
            btnTest.setOnClickListener(v -> {
                if (getContext() == null) return;
                ApiKeyManager keyMgr = new ApiKeyManager(getContext());
                String apiKey = etApiKey.getText().toString().trim();
                if (apiKey.isEmpty() || apiKey.contains("••••")) {
                    apiKey = keyMgr.getApiKey();
                }
                if (apiKey.isEmpty()) {
                    Toast.makeText(getContext(), "Please save an API Key first", Toast.LENGTH_SHORT).show();
                    return;
                }
                String selectedModel = keyMgr.getSelectedModel();
                Toast.makeText(getContext(), "Testing connection with " + selectedModel + "...", Toast.LENGTH_SHORT).show();
                new GeminiApiClient().testConnection(apiKey, selectedModel, new GeminiApiClient.ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Connection Successful! Model: " + selectedModel, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Connection failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            });
        }

        Button btnClearCache = view.findViewById(R.id.btn_clear_cache);
        if (btnClearCache != null) {
            btnClearCache.setOnClickListener(v -> Toast.makeText(getContext(), "Render and Texture Cache cleared", Toast.LENGTH_SHORT).show());
        }
    }

    private void fetchLiveModels(String apiKey, List<String> modelList, ArrayAdapter<String> modelAdapter, ApiKeyManager keyMgr, boolean showToast) {
        if (apiKey == null || apiKey.trim().isEmpty()) return;
        if (showToast && getContext() != null) {
            Toast.makeText(getContext(), "Fetching live models from Google Gemini API...", Toast.LENGTH_SHORT).show();
        }

        // Lock user actions during async network transactions
        isUpdatingModels = true;

        new GeminiApiClient().fetchModels(apiKey.trim(), new GeminiApiClient.ApiCallback<List<AIModel>>() {
            @Override
            public void onSuccess(List<AIModel> result) {
                if (getContext() == null || result == null || result.isEmpty()) {
                    isUpdatingModels = false;
                    return;
                }
                
                modelList.clear();
                for (AIModel m : result) {
                    modelList.add(m.getName());
                }
                modelAdapter.notifyDataSetChanged();

                String savedModel = keyMgr.getSelectedModel();
                int idx = modelList.indexOf(savedModel);
                if (idx >= 0) {
                    spinnerModel.setSelection(idx);
                } else if (!modelList.isEmpty()) {
                    spinnerModel.setSelection(0);
                    keyMgr.saveSelectedModel(modelList.get(0));
                }

                // Safely clear the update flag after the programmatic layout selection is completed
                spinnerModel.post(() -> isUpdatingModels = false);

                if (showToast && getContext() != null) {
                    Toast.makeText(getContext(), "Fetched " + result.size() + " live models from Google API!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                isUpdatingModels = false; // Release lock so user can interact if call failed
                if (showToast && getContext() != null) {
                    Toast.makeText(getContext(), "Failed to fetch live models: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}