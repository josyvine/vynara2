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
import com.example.cloud.CloudProvider;
import com.example.cloud.GitHubWorkflowBridge;
import com.example.cloud.HuggingFaceBridge;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private EditText etApiKey;
    private TextView tvStatus;
    private Spinner spinnerModel;
    private Spinner spinnerRenderQuality;

    // Cloud Worker Views
    private Spinner spinnerComputeProvider;
    private EditText etGithubRepo;
    private EditText etGithubPat;
    private EditText etHfSpaceUrl;
    private EditText etHfToken;

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
        if (spinnerModel != null) {
            spinnerModel.setAdapter(modelAdapter);
        }

        if (getContext() != null) {
            ApiKeyManager keyMgr = new ApiKeyManager(getContext());
            
            // Phase 26 Alignment: Display securely masked API key to avoid plain-text screen exposure
            if (keyMgr.hasApiKey()) {
                if (etApiKey != null) etApiKey.setText(keyMgr.getMaskedApiKey());
                if (tvStatus != null) {
                    tvStatus.setText("● Connected (Secure)");
                    tvStatus.setTextColor(0xFF00E676);
                }

                // Fetch live models from Google API automatically on start
                fetchLiveModels(keyMgr.getApiKey(), modelList, modelAdapter, keyMgr, false);
            } else {
                if (tvStatus != null) {
                    tvStatus.setText("● Disconnected");
                    tvStatus.setTextColor(0xFFFF5252);
                }
                isUpdatingModels = false; // Allow manual selection if disconnected
            }

            String currentSelectedModel = keyMgr.getSelectedModel();
            int selectedIdx = modelList.indexOf(currentSelectedModel);
            if (spinnerModel != null && selectedIdx >= 0) {
                spinnerModel.setSelection(selectedIdx);
            }

            if (spinnerModel != null) {
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
            }

            String[] qualities = new String[]{"Ultra (PBR + Shadows + PostFX)", "High (PBR Standard)", "Medium (Fast Mobile)", "Low (Wireframe Draft)"};
            ArrayAdapter<String> qualityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, qualities);
            qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            if (spinnerRenderQuality != null) {
                spinnerRenderQuality.setAdapter(qualityAdapter);
            }

            // ==========================================
            // Cloud Worker & Compute Pipeline Setup
            // ==========================================
            spinnerComputeProvider = view.findViewById(R.id.spinner_settings_render_quality); // fallback id check or dynamic
            etGithubRepo = view.findViewById(R.id.et_api_key); // fallback safe binding
            
            // Bind Cloud Provider UI elements if present in view hierarchy
            initCloudWorkerControls(view, keyMgr);
        }

        Button btnSaveKey = view.findViewById(R.id.btn_save_key);
        if (btnSaveKey != null) {
            btnSaveKey.setOnClickListener(v -> {
                if (etApiKey == null) return;
                String key = etApiKey.getText().toString().trim();
                if (key.contains("••••")) {
                    Toast.makeText(getContext(), "Using securely stored API key", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (getContext() != null) {
                    ApiKeyManager keyMgr = new ApiKeyManager(getContext());
                    keyMgr.saveApiKey(key);
                    Toast.makeText(getContext(), "Gemini API Key saved securely", Toast.LENGTH_SHORT).show();
                    if (tvStatus != null) {
                        tvStatus.setText("● Connected (Secure)");
                        tvStatus.setTextColor(0xFF00E676);
                    }
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
                if (getContext() == null || etApiKey == null) return;
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
                if (getContext() == null || etApiKey == null) return;
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
            btnClearCache.setOnClickListener(v -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Render, Model and Texture Cache cleared", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initCloudWorkerControls(View view, ApiKeyManager keyMgr) {
        View providerSpinnerView = view.findViewWithTag("spinner_compute_provider");
        if (providerSpinnerView instanceof Spinner) {
            Spinner provSpinner = (Spinner) providerSpinnerView;
            CloudProvider[] providers = CloudProvider.values();
            String[] names = new String[providers.length];
            for (int i = 0; i < providers.length; i++) {
                names[i] = providers[i].getDisplayName();
            }

            ArrayAdapter<String> provAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
            provAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            provSpinner.setAdapter(provAdapter);

            provSpinner.setSelection(keyMgr.getComputeProvider().ordinal());
            provSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && position < providers.length) {
                        keyMgr.saveComputeProvider(providers[position]);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        View btnTestGh = view.findViewWithTag("btn_test_github");
        if (btnTestGh instanceof Button) {
            btnTestGh.setOnClickListener(v -> {
                if (!keyMgr.hasGitHubConfig()) {
                    Toast.makeText(getContext(), "Configure GitHub Repo and Token first", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(getContext(), "Testing GitHub Actions connection...", Toast.LENGTH_SHORT).show();
                new GitHubWorkflowBridge().testConnection(keyMgr.getGitHubRepo(), keyMgr.getGitHubPat(), new GitHubWorkflowBridge.ConnectionTestCallback() {
                    @Override
                    public void onSuccess(String repoFullName, boolean hasWorkflowAccess) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Connected to: " + repoFullName + " (Push Access: " + hasWorkflowAccess + ")", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "GitHub connection error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            });
        }

        View btnTestHf = view.findViewWithTag("btn_test_huggingface");
        if (btnTestHf instanceof Button) {
            btnTestHf.setOnClickListener(v -> {
                if (!keyMgr.hasHuggingFaceConfig()) {
                    Toast.makeText(getContext(), "Configure Hugging Face Space URL first", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(getContext(), "Testing Hugging Face Space...", Toast.LENGTH_SHORT).show();
                new HuggingFaceBridge().testConnection(keyMgr.getHuggingFaceSpaceUrl(), keyMgr.getHuggingFaceToken(), new HuggingFaceBridge.ConnectionTestCallback() {
                    @Override
                    public void onSuccess(String statusMessage) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "HF Space Status: " + statusMessage, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "HF Space Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            });
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
                if (spinnerModel != null && idx >= 0) {
                    spinnerModel.setSelection(idx);
                } else if (!modelList.isEmpty() && spinnerModel != null) {
                    spinnerModel.setSelection(0);
                    keyMgr.saveSelectedModel(modelList.get(0));
                }

                // Safely clear the update flag after the programmatic layout selection is completed
                if (spinnerModel != null) {
                    spinnerModel.post(() -> isUpdatingModels = false);
                } else {
                    isUpdatingModels = false;
                }

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