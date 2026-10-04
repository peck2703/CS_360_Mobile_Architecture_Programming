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
                .url(SUPABASE_URL + tableName)
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

        Request request = new Request.Builder()
                .url(SUPABASE_URL + tableName)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", authHeaderValue)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public String authenticateUser(String email, String password) throws IOException {
        //Build the target authentication endpoint URL
        //Strip "rest/v1/" from the baseline setup to target the system auth route
        String authUrl = SUPABASE_URL.replace("rest/v1/", "auth/v1/token?grant_type=password");

        //Build the JSON request payload
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
            if (response.isSuccessful() && response.body() != null) {
                String responseString = response.body().string();
                // Parse out the nested access_token string from the response payload
                JsonObject jsonObject = JsonParser.parseString(responseString).getAsJsonObject();
                String jwtToken = jsonObject.get("access_token").getAsString();

                // Set the token inside the client instance to clear future RLS headers automatically
                setUserAccessToken(jwtToken);
                return jwtToken;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean registerUserAccount(String email, String password) throws IOException {
        //Build the signup endpoint target
        String signupUrl = SUPABASE_URL.replace("rest/v1/", "auth/v1/signup");

        //Wrap inputs into the exact structural JSON payload Supabase expects
        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("email", email);
        jsonPayload.addProperty("password", password);

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(signupUrl)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            // Returns true if the request completes successfully (HTTP status codes 200-299)
            return response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
