package com.lumin.luminclient.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lumin.luminclient.core.Debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ThreadLocalRandom;
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
    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 500L;
    private static final Gson GSON = new Gson();

    private final String apiKey;
    private volatile long cooldownUntilMs = 0L;

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
        IOException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                long inLoopNow = System.currentTimeMillis();
                if (inLoopNow < cooldownUntilMs) {
                    sleep(cooldownUntilMs - inLoopNow);
                }
                return getOnce(urlStr);
            } catch (RateLimitedException e) {
                throw new IOException(e.getMessage(), e);
            } catch (IOException e) {
                last = e;
                long backoff = jitteredBackoffMs(attempt);
                Debug.log(Debug.Category.API, "Request failed (attempt " + attempt + "/" + MAX_ATTEMPTS + "): " + e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    sleep(backoff);
                }
            }
        }
        throw last == null ? new IOException("Unknown HTTP failure") : last;
    }

    private JsonObject getOnce(String urlStr) throws IOException {
        HttpURLConnection conn = null;
        long startNs = System.nanoTime();
        try {
            Debug.log(Debug.Category.API, "GET " + urlStr);
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
                Debug.log(Debug.Category.ERROR, "HTTP " + code + " with empty body for " + urlStr);
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

            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
            Debug.log(Debug.Category.API, "HTTP " + code + " in " + elapsedMs + "ms, body=" + sb.length() + " bytes");

            JsonObject json = GSON.fromJson(sb.toString(), JsonObject.class);
            if (json == null) {
                Debug.log(Debug.Category.ERROR, "Empty/invalid JSON (HTTP " + code + ") from " + urlStr);
                throw new IOException("Empty/invalid JSON (HTTP " + code + ")");
            }
            if (code == 429) {
                long retryAfterMs = parseRetryAfterMs(conn);
                cooldownUntilMs = System.currentTimeMillis() + retryAfterMs;
                Debug.log(Debug.Category.ERROR, "Rate limited (429). Cooling down for " + retryAfterMs + "ms");
                throw new RateLimitedException("Rate limited (429)");
            }
            if (code >= 500) {
                throw new IOException("HTTP " + code + " server error");
            }
            if (code >= 400) {
                String cause = json.has("cause") ? json.get("cause").getAsString() : "unknown";
                Debug.log(Debug.Category.ERROR, "HTTP " + code + " from " + urlStr + ": " + cause);
                throw new IOException("HTTP " + code + ": " + cause);
            }
            if (json.has("success") && !json.get("success").getAsBoolean()) {
                String cause = json.has("cause") ? json.get("cause").getAsString() : "unknown";
                Debug.log(Debug.Category.ERROR, "Hypixel API error from " + urlStr + ": " + cause);
                throw new IOException("Hypixel API error: " + cause);
            }
            return json;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static long parseRetryAfterMs(HttpURLConnection conn) {
        String retryAfter = conn.getHeaderField("Retry-After");
        if (retryAfter != null) {
            try {
                long sec = Long.parseLong(retryAfter.trim());
                return Math.max(1_000L, sec * 1_000L);
            } catch (NumberFormatException ignored) {
            }
            try {
                ZonedDateTime when = ZonedDateTime.parse(retryAfter.trim(), DateTimeFormatter.RFC_1123_DATE_TIME);
                long delta = when.toInstant().toEpochMilli() - System.currentTimeMillis();
                return Math.max(1_000L, delta);
            } catch (DateTimeParseException ignored) {
            }
        }
        return 10_000L;
    }

    private static long jitteredBackoffMs(int attempt) {
        long cap = BASE_BACKOFF_MS * (1L << Math.max(0, attempt - 1));
        return cap + ThreadLocalRandom.current().nextLong(150L, 401L);
    }

    private static void sleep(long ms) throws IOException {
        try {
            Thread.sleep(Math.max(0L, ms));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for retry", e);
        }
    }

    private static final class RateLimitedException extends IOException {
        private RateLimitedException(String message) {
            super(message);
        }
    }
}
