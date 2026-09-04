package com.example.cloud;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.example.cloud.models.DeviceCodeResponse;
import com.example.utils.VynaraLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GitHubOAuthService {
    private static final String OAUTH_AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String OAUTH_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String DEVICE_CODE_URL = "https://github.com/login/device/code";
    private static final String USER_API_URL = "https://api.github.com/user";
    private static final String USER_REPOS_URL = "https://api.github.com/user/repos?per_page=100&sort=updated";

    public static final String DEFAULT_REDIRECT_URI = "vynara://oauth-callback";
    public static final String DEFAULT_SCOPES = "repo,workflow,user";

    private final OkHttpClient httpClient;
    private final Handler mainHandler;
    private volatile boolean isPollingCancelled = false;

    public interface OAuthTokenCallback {
        void onSuccess(String accessToken, String tokenType, String scope);
        void onError(String errorMessage);
    }

    public interface DeviceCodeCallback {
        void onDeviceCodeReceived(DeviceCodeResponse response);
        void onError(String errorMessage);
    }

    public interface DevicePollingCallback {
        void onTokenReceived(String accessToken);
        void onPending(String status);
        void onError(String errorMessage);
    }

    public interface UserProfileCallback {
        void onSuccess(String login, String name, String avatarUrl);
        void onError(String errorMessage);
    }

    public interface UserReposCallback {
        void onSuccess(List<String> repoFullNames);
        void onError(String errorMessage);
    }

    public GitHubOAuthService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static String buildWebAuthorizeUrl(String clientId, String redirectUri, String state) {
        Uri.Builder builder = Uri.parse(OAUTH_AUTHORIZE_URL).buildUpon();
        builder.appendQueryParameter("client_id", clientId != null ? clientId.trim() : "");
        builder.appendQueryParameter("redirect_uri", redirectUri != null ? redirectUri.trim() : DEFAULT_REDIRECT_URI);
        builder.appendQueryParameter("scope", DEFAULT_SCOPES);
        if (state != null && !state.trim().isEmpty()) {
            builder.appendQueryParameter("state", state.trim());
        }
        return builder.build().toString();
    }

    public void exchangeCodeForToken(String clientId,
                                    String clientSecret,
                                    String code,
                                    String redirectUri,
                                    OAuthTokenCallback callback) {
        if (clientId == null || clientId.trim().isEmpty()) {
            callback.onError("GitHub Client ID is missing.");
            return;
        }
        if (code == null || code.trim().isEmpty()) {
            callback.onError("Authorization code is empty.");
            return;
        }

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("client_id", clientId.trim())
                .add("code", code.trim())
                .add("redirect_uri", redirectUri != null ? redirectUri.trim() : DEFAULT_REDIRECT_URI);

        if (clientSecret != null && !clientSecret.trim().isEmpty()) {
            formBuilder.add("client_secret", clientSecret.trim());
        }

        Request request = new Request.Builder()
                .url(OAUTH_TOKEN_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .post(formBuilder.build())
                .build();

        VynaraLogger.system("GitHubOAuthService: Exchanging OAuth code for Access Token...");

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                VynaraLogger.e("OAuth token exchange network failure: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Network error during token exchange: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        String err = "GitHub returned HTTP " + response.code();
                        mainHandler.post(() -> callback.onError(err));
                        return;
                    }

                    String jsonStr = body.string();
                    JSONObject json = new JSONObject(jsonStr);

                    if (json.has("error")) {
                        String errorDesc = json.optString("error_description", json.optString("error", "Unknown OAuth error"));
                        mainHandler.post(() -> callback.onError(errorDesc));
                        return;
                    }

                    String accessToken = json.optString("access_token", "");
                    String tokenType = json.optString("token_type", "bearer");
                    String scope = json.optString("scope", "");

                    if (!accessToken.isEmpty()) {
                        VynaraLogger.system("GitHubOAuthService: Token exchange successful.");
                        mainHandler.post(() -> callback.onSuccess(accessToken, tokenType, scope));
                    } else {
                        mainHandler.post(() -> callback.onError("Response did not contain an access_token."));
                    }
                } catch (Exception e) {
                    VynaraLogger.e("Failed to parse token response: " + e.getMessage(), e);
                    mainHandler.post(() -> callback.onError("Parse error: " + e.getMessage()));
                }
            }
        });
    }

    public void requestDeviceCode(String clientId, DeviceCodeCallback callback) {
        if (clientId == null || clientId.trim().isEmpty()) {
            callback.onError("GitHub Client ID is required for Device Flow.");
            return;
        }

        FormBody formBody = new FormBody.Builder()
                .add("client_id", clientId.trim())
                .add("scope", DEFAULT_SCOPES)
                .build();

        Request request = new Request.Builder()
                .url(DEVICE_CODE_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .post(formBody)
                .build();

        VynaraLogger.system("GitHubOAuthService: Requesting Device Flow code...");

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                VynaraLogger.e("Device code request failed: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Network failure: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        mainHandler.post(() -> callback.onError("GitHub returned HTTP " + response.code()));
                        return;
                    }

                    String jsonStr = body.string();
                    JSONObject json = new JSONObject(jsonStr);

                    if (json.has("error")) {
                        String errorDesc = json.optString("error_description", json.optString("error", "Failed to start Device Flow"));
                        mainHandler.post(() -> callback.onError(errorDesc));
                        return;
                    }

                    DeviceCodeResponse codeResponse = DeviceCodeResponse.fromJson(json);
                    if (codeResponse.isValid()) {
                        VynaraLogger.system("GitHubOAuthService: Received User Code: " + codeResponse.getUserCode());
                        mainHandler.post(() -> callback.onDeviceCodeReceived(codeResponse));
                    } else {
                        mainHandler.post(() -> callback.onError("Received invalid Device Code payload from GitHub."));
                    }
                } catch (Exception e) {
                    VynaraLogger.e("Error parsing Device Code response: " + e.getMessage(), e);
                    mainHandler.post(() -> callback.onError("Parse error: " + e.getMessage()));
                }
            }
        });
    }

    public void startDeviceFlowPolling(String clientId,
                                       String deviceCode,
                                       int intervalSeconds,
                                       int expiresInSeconds,
                                       DevicePollingCallback callback) {
        isPollingCancelled = false;
        final long startTime = System.currentTimeMillis();
        final long maxDurationMs = (expiresInSeconds > 0 ? expiresInSeconds : 900) * 1000L;
        final int intervalMs = Math.max(intervalSeconds, 5) * 1000;

        Runnable pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPollingCancelled) {
                    mainHandler.post(() -> callback.onError("Device login cancelled."));
                    return;
                }

                if (System.currentTimeMillis() - startTime > maxDurationMs) {
                    mainHandler.post(() -> callback.onError("Device code has expired. Please try again."));
                    return;
                }

                pollDeviceTokenOnce(clientId, deviceCode, new DevicePollingCallback() {
                    @Override
                    public void onTokenReceived(String accessToken) {
                        mainHandler.post(() -> callback.onTokenReceived(accessToken));
                    }

                    @Override
                    public void onPending(String status) {
                        mainHandler.post(() -> callback.onPending(status));
                        if (!isPollingCancelled) {
                            mainHandler.postDelayed(pollRunnable, intervalMs);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        mainHandler.post(() -> callback.onError(errorMessage));
                    }
                });
            }
        };

        mainHandler.postDelayed(pollRunnable, intervalMs);
    }

    public void cancelDeviceFlowPolling() {
        this.isPollingCancelled = true;
    }

    private void pollDeviceTokenOnce(String clientId, String deviceCode, DevicePollingCallback callback) {
        FormBody formBody = new FormBody.Builder()
                .add("client_id", clientId.trim())
                .add("device_code", deviceCode.trim())
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .build();

        Request request = new Request.Builder()
                .url(OAUTH_TOKEN_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .post(formBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onPending("Connecting to GitHub...");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (body == null) {
                        callback.onPending("Waiting for user authorization...");
                        return;
                    }

                    String jsonStr = body.string();
                    JSONObject json = new JSONObject(jsonStr);

                    if (json.has("access_token")) {
                        String token = json.getString("access_token");
                        callback.onTokenReceived(token);
                        return;
                    }

                    String error = json.optString("error", "");
                    if ("authorization_pending".equalsIgnoreCase(error)) {
                        callback.onPending("Waiting for authorization on GitHub...");
                    } else if ("slow_down".equalsIgnoreCase(error)) {
                        callback.onPending("Slow down requested by GitHub...");
                    } else if ("expired_token".equalsIgnoreCase(error)) {
                        callback.onError("Device code has expired.");
                    } else if ("access_denied".equalsIgnoreCase(error)) {
                        callback.onError("Access was denied by the user.");
                    } else {
                        String errorDesc = json.optString("error_description", error);
                        callback.onError(errorDesc.isEmpty() ? "Unknown authorization error." : errorDesc);
                    }
                } catch (Exception e) {
                    callback.onPending("Processing...");
                }
            }
        });
    }

    public void fetchUserProfile(String accessToken, UserProfileCallback callback) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onError("Access token is empty.");
            return;
        }

        Request request = new Request.Builder()
                .url(USER_API_URL)
                .header("Authorization", "Bearer " + accessToken.trim())
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Failed to fetch user profile: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        mainHandler.post(() -> callback.onError("HTTP " + response.code() + " fetching profile"));
                        return;
                    }

                    String jsonStr = body.string();
                    JSONObject json = new JSONObject(jsonStr);

                    String login = json.optString("login", "");
                    String name = json.optString("name", login);
                    String avatarUrl = json.optString("avatar_url", "");

                    mainHandler.post(() -> callback.onSuccess(login, name, avatarUrl));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Error parsing profile: " + e.getMessage()));
                }
            }
        });
    }

    public void fetchUserRepositories(String accessToken, UserReposCallback callback) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onError("Access token is empty.");
            return;
        }

        Request request = new Request.Builder()
                .url(USER_REPOS_URL)
                .header("Authorization", "Bearer " + accessToken.trim())
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Failed to fetch repositories: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        mainHandler.post(() -> callback.onError("HTTP " + response.code() + " fetching repos"));
                        return;
                    }

                    String jsonStr = body.string();
                    JSONArray array = new JSONArray(jsonStr);
                    List<String> repoNames = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject repo = array.getJSONObject(i);
                        String fullName = repo.optString("full_name", "");
                        if (!fullName.isEmpty()) {
                            repoNames.add(fullName);
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(repoNames));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Error parsing repositories: " + e.getMessage()));
                }
            }
        });
    }
}