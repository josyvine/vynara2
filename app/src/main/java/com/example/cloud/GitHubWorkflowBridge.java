package com.example.cloud;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.utils.VynaraLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GitHubWorkflowBridge {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    public interface WorkflowDispatchCallback {
        void onDispatched(String eventType, String assetId);
        void onError(String errorMessage);
    }

    public interface ArtifactDownloadCallback {
        void onProgress(int percentage, long bytesRead, long totalBytes);
        void onSuccess(File downloadedFile);
        void onError(String errorMessage);
    }

    public interface ConnectionTestCallback {
        void onSuccess(String repoFullName, boolean hasWorkflowAccess);
        void onError(String errorMessage);
    }

    public GitHubWorkflowBridge() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    // --- Overloaded Context-Aware Methods (Auto-fetch Stored Token) ---

    public void testConnection(Context context, String repository, ConnectionTestCallback callback) {
        String token = GitHubOAuthService.getAccessToken(context);
        testConnection(repository, token, callback);
    }

    public void dispatchGenerationWorkflow(Context context,
                                           String repository,
                                           String eventType,
                                           String assetId,
                                           String bpyScript,
                                           WorkflowDispatchCallback callback) {
        String token = GitHubOAuthService.getAccessToken(context);
        dispatchGenerationWorkflow(repository, token, eventType, assetId, bpyScript, callback);
    }

    public void downloadWorkflowArtifact(Context context,
                                         String repository,
                                         String assetId,
                                         File destinationFile,
                                         ArtifactDownloadCallback callback) {
        String token = GitHubOAuthService.getAccessToken(context);
        downloadWorkflowArtifact(repository, token, assetId, destinationFile, callback);
    }

    // --- Standard Methods ---

    public void testConnection(String repository, String personalAccessToken, ConnectionTestCallback callback) {
        if (repository == null || repository.trim().isEmpty()) {
            callback.onError("Repository cannot be empty. Format: owner/repo");
            return;
        }
        if (personalAccessToken == null || personalAccessToken.trim().isEmpty()) {
            callback.onError("GitHub Access Token is empty. Please sign in or provide a token.");
            return;
        }

        String targetUrl = "https://api.github.com/repos/" + repository.trim();

        Request request = new Request.Builder()
                .url(targetUrl)
                .header("Authorization", "Bearer " + personalAccessToken.trim())
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network connection failure: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        String errorMsg = "GitHub API Error [" + response.code() + "]: " + response.message();
                        mainHandler.post(() -> callback.onError(errorMsg));
                        return;
                    }

                    String jsonStr = responseBody.string();
                    JSONObject repoObj = new JSONObject(jsonStr);
                    String fullName = repoObj.optString("full_name", repository);
                    JSONObject permissions = repoObj.optJSONObject("permissions");
                    boolean hasPushAccess = permissions != null && (permissions.optBoolean("push", false) || permissions.optBoolean("admin", false));

                    mainHandler.post(() -> callback.onSuccess(fullName, hasPushAccess));
                } catch (Exception ex) {
                    mainHandler.post(() -> callback.onError("Failed to parse repository response: " + ex.getMessage()));
                }
            }
        });
    }

    public void dispatchGenerationWorkflow(String repository,
                                           String personalAccessToken,
                                           String eventType,
                                           String assetId,
                                           String bpyScript,
                                           WorkflowDispatchCallback callback) {
        if (repository == null || repository.trim().isEmpty() || personalAccessToken == null || personalAccessToken.trim().isEmpty()) {
            callback.onError("GitHub credentials are not properly configured.");
            return;
        }

        String dispatchUrl = "https://api.github.com/repos/" + repository.trim() + "/dispatches";

        try {
            JSONObject clientPayload = new JSONObject();
            clientPayload.put("asset_id", assetId);
            clientPayload.put("bpy_script", bpyScript);
            clientPayload.put("timestamp", System.currentTimeMillis());

            JSONObject rootPayload = new JSONObject();
            rootPayload.put("event_type", (eventType != null && !eventType.trim().isEmpty()) ? eventType : "vynara_generate");
            rootPayload.put("client_payload", clientPayload);

            RequestBody body = RequestBody.create(rootPayload.toString(), JSON_MEDIA_TYPE);

            Request request = new Request.Builder()
                    .url(dispatchUrl)
                    .header("Authorization", "Bearer " + personalAccessToken.trim())
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Vynara-3D-Studio-Android")
                    .post(body)
                    .build();

            VynaraLogger.system("GitHubWorkflowBridge: Dispatching workflow to: " + dispatchUrl + " with assetId: " + assetId);

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    VynaraLogger.e("Workflow dispatch failed: " + e.getMessage(), e);
                    mainHandler.post(() -> callback.onError("Failed to dispatch workflow: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        if (response.code() == 204 || response.isSuccessful()) {
                            VynaraLogger.system("GitHubWorkflowBridge: Workflow dispatched successfully (HTTP " + response.code() + ")");
                            mainHandler.post(() -> callback.onDispatched(eventType, assetId));
                        } else {
                            String err = "GitHub returned HTTP " + response.code() + " (" + response.message() + ")";
                            VynaraLogger.e(err);
                            mainHandler.post(() -> callback.onError(err));
                        }
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception ex) {
            callback.onError("Failed to assemble dispatch payload: " + ex.getMessage());
        }
    }

    public void downloadWorkflowArtifact(String repository,
                                         String personalAccessToken,
                                         String assetId,
                                         File destinationFile,
                                         ArtifactDownloadCallback callback) {
        String artifactsUrl = "https://api.github.com/repos/" + repository.trim() + "/actions/artifacts";

        Request request = new Request.Builder()
                .url(artifactsUrl)
                .header("Authorization", "Bearer " + personalAccessToken.trim())
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Artifact search failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        mainHandler.post(() -> callback.onError("Failed to list artifacts: HTTP " + response.code()));
                        return;
                    }

                    String json = responseBody.string();
                    JSONObject root = new JSONObject(json);
                    JSONArray artifacts = root.optJSONArray("artifacts");

                    if (artifacts == null || artifacts.length() == 0) {
                        mainHandler.post(() -> callback.onError("No build artifacts found in repository."));
                        return;
                    }

                    String downloadLocationUrl = null;
                    for (int i = 0; i < artifacts.length(); i++) {
                        JSONObject artifact = artifacts.getJSONObject(i);
                        String name = artifact.optString("name", "");
                        if (name.equalsIgnoreCase(assetId) || name.contains(assetId)) {
                            downloadLocationUrl = artifact.optString("archive_download_url", null);
                            break;
                        }
                    }

                    if (downloadLocationUrl == null) {
                        mainHandler.post(() -> callback.onError("Artifact matching assetId '" + assetId + "' not ready yet."));
                        return;
                    }

                    executeBinaryDownload(downloadLocationUrl, personalAccessToken, destinationFile, callback);
                } catch (Exception ex) {
                    mainHandler.post(() -> callback.onError("Failed to parse artifacts list: " + ex.getMessage()));
                }
            }
        });
    }

    private void executeBinaryDownload(String downloadUrl,
                                       String token,
                                       File destinationFile,
                                       ArtifactDownloadCallback callback) {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .header("Authorization", "Bearer " + token.trim())
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Failed to download artifact binary: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    mainHandler.post(() -> callback.onError("Artifact download failed: HTTP " + response.code()));
                    return;
                }

                ResponseBody body = response.body();
                long totalBytes = body.contentLength();

                File tempZipFile = new File(destinationFile.getParentFile(), destinationFile.getName() + ".zip");

                try (InputStream inputStream = body.byteStream();
                     FileOutputStream outputStream = new FileOutputStream(tempZipFile)) {

                    byte[] buffer = new byte[8192];
                    long totalBytesRead = 0;
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        if (totalBytes > 0) {
                            int progress = (int) ((totalBytesRead * 100) / totalBytes);
                            long currentRead = totalBytesRead;
                            mainHandler.post(() -> callback.onProgress(progress, currentRead, totalBytes));
                        }
                    }
                    outputStream.flush();

                    boolean extracted = extractGlbFromZip(tempZipFile, destinationFile);
                    if (tempZipFile.exists()) {
                        tempZipFile.delete();
                    }

                    if (extracted && destinationFile.exists() && destinationFile.length() > 0) {
                        mainHandler.post(() -> callback.onSuccess(destinationFile));
                    } else {
                        mainHandler.post(() -> callback.onError("Extracted file is missing or invalid."));
                    }

                } catch (Exception ex) {
                    if (tempZipFile.exists()) {
                        tempZipFile.delete();
                    }
                    mainHandler.post(() -> callback.onError("Error saving artifact: " + ex.getMessage()));
                }
            }
        });
    }

    private boolean extractGlbFromZip(File zipFile, File destinationGlbFile) {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new java.io.FileInputStream(zipFile)))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName().toLowerCase();
                if (fileName.endsWith(".glb") || fileName.endsWith(".gltf")) {
                    if (destinationGlbFile.getParentFile() != null && !destinationGlbFile.getParentFile().exists()) {
                        destinationGlbFile.getParentFile().mkdirs();
                    }

                    try (FileOutputStream fos = new FileOutputStream(destinationGlbFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.flush();
                    }
                    zis.closeEntry();
                    return true;
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            VynaraLogger.e("ZIP extraction error: " + e.getMessage(), e);
        }
        return false;
    }
}