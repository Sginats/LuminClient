package com.lumin.luminclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Simple JSON-backed config. Stored in the Forge config dir.
 * Never stores secrets - the public Hypixel endpoints used here need no key.
 */
public class LuminConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final File file;

    // Market scan settings
    public int scanIntervalSeconds = 30;
    public double minMarginPercent = 3.0;      // bazaar min margin %
    public double minProfitPerFlip = 10_000;   // coins
    public double maxBudget = 5_000_000;       // total coins willing to commit
    public int maxResults = 15;                // rows shown in GUI

    // Flip strategy
    public boolean enableBazaarMarginFlips = true;
    public boolean enableBinSnipeFlips = true;
    public double minBinFlipPercent = 8.0;     // BIN vs lowest BIN / lbin gap %

    // Notifications
    public boolean chatNotifications = true;
    public boolean soundAlerts = true;

    // Optional Hypixel API key. Public endpoints used by default do NOT need one.
    // If the user sets one it is stored in the separate -local file that is gitignored.
    public String apiKey = "";

    public LuminConfig(File suggestedFile) {
        File dir = suggestedFile.getParentFile();
        this.file = new File(dir, "luminclient.json");
    }

    public void load() {
        if (!file.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            JsonObject o = new JsonParser().parse(reader).getAsJsonObject();
            if (o.has("scanIntervalSeconds")) scanIntervalSeconds = o.get("scanIntervalSeconds").getAsInt();
            if (o.has("minMarginPercent")) minMarginPercent = o.get("minMarginPercent").getAsDouble();
            if (o.has("minProfitPerFlip")) minProfitPerFlip = o.get("minProfitPerFlip").getAsDouble();
            if (o.has("maxBudget")) maxBudget = o.get("maxBudget").getAsDouble();
            if (o.has("maxResults")) maxResults = o.get("maxResults").getAsInt();
            if (o.has("enableBazaarMarginFlips")) enableBazaarMarginFlips = o.get("enableBazaarMarginFlips").getAsBoolean();
            if (o.has("enableBinSnipeFlips")) enableBinSnipeFlips = o.get("enableBinSnipeFlips").getAsBoolean();
            if (o.has("minBinFlipPercent")) minBinFlipPercent = o.get("minBinFlipPercent").getAsDouble();
            if (o.has("chatNotifications")) chatNotifications = o.get("chatNotifications").getAsBoolean();
            if (o.has("soundAlerts")) soundAlerts = o.get("soundAlerts").getAsBoolean();
            if (o.has("apiKey")) apiKey = o.get("apiKey").getAsString();
        } catch (Exception e) {
            // keep defaults on malformed config
        }
    }

    public void save() {
        JsonObject o = new JsonObject();
        o.addProperty("scanIntervalSeconds", scanIntervalSeconds);
        o.addProperty("minMarginPercent", minMarginPercent);
        o.addProperty("minProfitPerFlip", minProfitPerFlip);
        o.addProperty("maxBudget", maxBudget);
        o.addProperty("maxResults", maxResults);
        o.addProperty("enableBazaarMarginFlips", enableBazaarMarginFlips);
        o.addProperty("enableBinSnipeFlips", enableBinSnipeFlips);
        o.addProperty("minBinFlipPercent", minBinFlipPercent);
        o.addProperty("chatNotifications", chatNotifications);
        o.addProperty("soundAlerts", soundAlerts);
        o.addProperty("apiKey", apiKey);
        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(o, writer);
            }
        } catch (IOException e) {
            // non-fatal
        }
    }
}
