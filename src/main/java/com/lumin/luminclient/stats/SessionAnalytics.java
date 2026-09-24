package com.lumin.luminclient.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lumin.luminclient.LuminClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SessionAnalytics {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int VERSION = 1;

    private final File file;

    private long attempted;
    private long succeeded;
    private long failed;
    private double estimatedPnl;
    private double totalSpend;
    private long totalLatencyMs;
    private final Map<String, Integer> failureReasons = new HashMap<String, Integer>();
    private String day = utcDay();

    public SessionAnalytics() {
        this.file = FabricLoader.getInstance().getConfigDir().resolve("luminclient-stats.json").toFile();
        load();
    }

    public synchronized void recordAttempt(double spend, long latencyMs, boolean success, double pnlDelta, String failureReason) {
        rollDayIfNeeded();
        attempted++;
        totalSpend += Math.max(0.0, spend);
        totalLatencyMs += Math.max(0L, latencyMs);
        estimatedPnl += pnlDelta;
        if (success) {
            succeeded++;
        } else {
            failed++;
            String reason = (failureReason == null || failureReason.trim().isEmpty()) ? "unknown" : failureReason.trim();
            failureReasons.put(reason, failureReasons.getOrDefault(reason, 0) + 1);
        }
        save();
    }

    public synchronized Summary getSummary() {
        rollDayIfNeeded();
        double avgLatencyMs = attempted <= 0 ? 0.0 : (double) totalLatencyMs / (double) attempted;
        return new Summary(attempted, succeeded, failed, estimatedPnl, totalSpend, avgLatencyMs, new HashMap<String, Integer>(failureReasons));
    }

    private synchronized void rollDayIfNeeded() {
        String today = utcDay();
        if (!today.equals(day)) {
            day = today;
            attempted = 0;
            succeeded = 0;
            failed = 0;
            estimatedPnl = 0.0;
            totalSpend = 0.0;
            totalLatencyMs = 0L;
            failureReasons.clear();
            save();
        }
    }

    private void load() {
        if (!file.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
            attempted = o.has("attempted") ? o.get("attempted").getAsLong() : 0L;
            succeeded = o.has("succeeded") ? o.get("succeeded").getAsLong() : 0L;
            failed = o.has("failed") ? o.get("failed").getAsLong() : 0L;
            estimatedPnl = o.has("estimatedPnl") ? o.get("estimatedPnl").getAsDouble() : 0.0;
            totalSpend = o.has("totalSpend") ? o.get("totalSpend").getAsDouble() : 0.0;
            totalLatencyMs = o.has("totalLatencyMs") ? o.get("totalLatencyMs").getAsLong() : 0L;
            day = o.has("day") ? o.get("day").getAsString() : utcDay();
            failureReasons.clear();
            if (o.has("failureReasons") && o.get("failureReasons").isJsonObject()) {
                JsonObject fr = o.getAsJsonObject("failureReasons");
                for (String k : fr.keySet()) {
                    failureReasons.put(k, fr.get(k).getAsInt());
                }
            }
        } catch (Exception e) {
            LuminClient.LOGGER.warn("Failed to load luminclient stats: {}", e.getMessage());
        }
    }

    private synchronized void save() {
        JsonObject o = new JsonObject();
        o.addProperty("version", VERSION);
        o.addProperty("day", day);
        o.addProperty("attempted", attempted);
        o.addProperty("succeeded", succeeded);
        o.addProperty("failed", failed);
        o.addProperty("estimatedPnl", estimatedPnl);
        o.addProperty("totalSpend", totalSpend);
        o.addProperty("totalLatencyMs", totalLatencyMs);
        JsonObject fr = new JsonObject();
        for (Map.Entry<String, Integer> e : failureReasons.entrySet()) {
            fr.addProperty(e.getKey(), e.getValue());
        }
        o.add("failureReasons", fr);

        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(o, writer);
            }
        } catch (Exception e) {
            LuminClient.LOGGER.warn("Failed to save luminclient stats: {}", e.getMessage());
        }
    }

    private static String utcDay() {
        return LocalDate.now(ZoneOffset.UTC).toString();
    }

    public static final class Summary {
        public final long attempted;
        public final long succeeded;
        public final long failed;
        public final double estimatedPnl;
        public final double totalSpend;
        public final double avgLatencyMs;
        public final Map<String, Integer> failureReasons;

        public Summary(long attempted, long succeeded, long failed, double estimatedPnl, double totalSpend,
                       double avgLatencyMs, Map<String, Integer> failureReasons) {
            this.attempted = attempted;
            this.succeeded = succeeded;
            this.failed = failed;
            this.estimatedPnl = estimatedPnl;
            this.totalSpend = totalSpend;
            this.avgLatencyMs = avgLatencyMs;
            this.failureReasons = Collections.unmodifiableMap(failureReasons);
        }
    }
}
