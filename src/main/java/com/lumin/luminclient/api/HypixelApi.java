package com.lumin.luminclient.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * Minimal HTTP client for the public Hypixel API.
 *
 * Endpoints used (all public, no key required):
 *  - GET /v2/skyblock/bazaar   -> full bazaar product snapshot
 *  - GET /v2/skyblock/auctions -> paginated live auctions (for BIN snipe detection)
 */
public class HypixelApi {

    private static final String BASE = "https://api.hypixel.net";
    private static final int TIMEOUT_MS = 10_000;
    private static final Gson GSON = new Gson();

    private final String apiKey;

    public HypixelApi(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public JsonObject getBazaar() throws IOException {
        return get(BASE + "/v2/skyblock/bazaar");
    }

    public JsonObject getAuctions(int page) throws IOException {
        return get(BASE + "/v2/skyblock/auctions?page=" + page);
    }

    private JsonObject get(String urlStr) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "LuminClient/1.0");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Accept-Encoding", "gzip");
            if (!apiKey.isEmpty()) {
                conn.setRequestProperty("API-Key", apiKey);
            }

            int code = conn.getResponseCode();
            InputStream raw = code >= 200 && code < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();
            if (raw == null) {
                throw new IOException("HTTP " + code + " with empty body");
            }

            InputStream in = raw;
            String encoding = conn.getContentEncoding();
            if ("gzip".equalsIgnoreCase(encoding)) {
                in = new GZIPInputStream(raw);
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }

            JsonObject json = GSON.fromJson(sb.toString(), JsonObject.class);
            if (json == null) {
                throw new IOException("Empty/invalid JSON (HTTP " + code + ")");
            }
            if (code >= 400) {
                String cause = json.has("cause") ? json.get("cause").getAsString() : "unknown";
                throw new IOException("HTTP " + code + ": " + cause);
            }
            if (json.has("success") && !json.get("success").getAsBoolean()) {
                String cause = json.has("cause") ? json.get("cause").getAsString() : "unknown";
                throw new IOException("Hypixel API error: " + cause);
            }
            return json;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
