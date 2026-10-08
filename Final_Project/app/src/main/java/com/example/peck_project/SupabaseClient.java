package com.example.peck_project;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SupabaseClient {
    private static final String SUPABASE_URL = "https://zyefbqfrmqnegwteyicx.supabase.co";
    private static final String SUPABASE_ANON_KEY = "sb_publishable_s0ie9g_yBCys3khaP54-SQ_fmEF9eeT";

    private static SupabaseClient instance;
    private final OkHttpClient httpClient;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private String userAccessToken = null;

    private SupabaseClient() {
        this.httpClient = new OkHttpClient();
    }

    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    /**
     * Call this method immediately after a successful user login/authentication
     */
    public void setUserAccessToken(String token) {
        this.userAccessToken = token;
    }

    /**
     * Pulls data from a targeted Supabase PostgreSQL table as a JSON String
     */
    public String fetchTableData(String tableName) throws IOException {
        String authHeaderValue = (userAccessToken != null) ? "Bearer " + userAccessToken : "Bearer " + SUPABASE_ANON_KEY;

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/" + tableName)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", authHeaderValue)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected network code " + response);
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * Upserts (inserts or updates) a JSON payload directly into your Supabase table
     */
    public boolean upsertData(String tableName, String jsonPayload) throws IOException {
        String authHeaderValue = (userAccessToken != null) ? "Bearer " + userAccessToken : "Bearer " + SUPABASE_ANON_KEY;
        RequestBody body = RequestBody.create(jsonPayload, JSON);

        String fullTargetUrl = SUPABASE_URL + "/rest/v1/" + tableName;

        Request request = new Request.Builder()
                .url(fullTargetUrl)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", authHeaderValue)
                .addHeader("Prefer", "return=representation")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(body)
                .build();


        try (Response response = httpClient.newCall(request).execute()) {
            // Enable diagnostic visibility checks natively in the data stream loop
            android.util.Log.e("SUPABASE_SYNC_DEBUG", "Destination HTTP Endpoint: " + fullTargetUrl);
            android.util.Log.e("SUPABASE_SYNC_DEBUG", "HTTP Response Code: " + response.code());

            if (response.body() != null) {
                String errorBodyOutput = response.body().string();
                android.util.Log.e("SUPABASE_SYNC_DEBUG", "Server Output Error Payload: " + errorBodyOutput);
            }

            return response.isSuccessful();
        }
    }

    /**
     * Authenticates a user and returns a comma-separated string containing [jwtToken],[userUuid]
     */
    public String authenticateUser(String email, String password) throws IOException {
        String authUrl = SUPABASE_URL + "/auth/v1/token?grant_type=password";

        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("email", email);
        jsonPayload.addProperty("password", password);

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(authUrl)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() != null) {
                String responseString = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(responseString).getAsJsonObject();

                if (!response.isSuccessful()) {
                    return null;
                }

                // FIXED: Extract the actual 36-character user account UUID from the response body!
                if (jsonObject.has("access_token") && jsonObject.has("user")) {
                    String jwtToken = jsonObject.get("access_token").getAsString();

                    JsonObject userObj = jsonObject.getAsJsonObject("user");
                    String userUuid = userObj.get("id").getAsString();

                    setUserAccessToken(jwtToken);

                    // Return BOTH values packed together cleanly as a split-ready string
                    return jwtToken + "," + userUuid;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String registerUserAccount(String email, String password) {
        try {
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

            com.google.gson.JsonObject jsonPayload = new com.google.gson.JsonObject();
            jsonPayload.addProperty("email", email);
            jsonPayload.addProperty("password", password);

            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    jsonPayload.toString(),
                    okhttp3.MediaType.parse("application/json; charset=utf-8")
            );

            String targetUrl = SUPABASE_URL + "/auth/v1/signup";

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(targetUrl)
                    .post(body)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                // DIAGNOSTIC CORE: Log the network status immediately
                android.util.Log.e("REGISTRATION_GATE", "Supabase SignUp HTTP Status Code: " + response.code());

                if (response.body() != null) {
                    String responseBody = response.body().string();
                    android.util.Log.e("REGISTRATION_GATE", "Supabase SignUp Raw Body Payload: " + responseBody);

                    if (response.isSuccessful()) {
                        com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(responseBody).getAsJsonObject();

                        if (obj.has("user") && !obj.get("user").isJsonNull()) {
                            com.google.gson.JsonObject userObj = obj.getAsJsonObject("user");
                            if (userObj.has("id")) {
                                return userObj.get("id").getAsString();
                            }
                        }
                        if (obj.has("id")) {
                            return obj.get("id").getAsString();
                        }
                    }

                }
                return null;
            }
        } catch (Exception e) {
            // !!! CRITICAL DIAGNOSTIC: Force Logcat to print the exact crash line number and message !!!
            android.util.Log.e("REGISTRATION_GATE", "CRITICAL NETWORK THREAD EXCEPTION DISCOVERED:", e);
            return null;
        }
    }

}