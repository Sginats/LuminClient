package com.lumin.luminclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Simple JSON-backed config. Stored in the Fabric config dir.
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
    public double minBinFlipPercent = 8.0;     // BIN vs lowest BIN gap %

    // Notifications
    public boolean chatNotifications = true;
    public boolean soundAlerts = true;

    // Automation
    // WARNING: enabling this makes the mod click/place orders/pay for you.
    // This violates Hypixel rules and can get the account banned. Default OFF.
    public boolean automationEnabled = false;
    public int clickDelayMs = 250;             // delay between automated clicks
    public double automationMaxSpendPerOrder = 250_000; // per-order spend cap
    public int automationMaxOrdersPerScan = 3;          // safety limit

    // Debug / diagnostics
    public boolean debugMode = false;      // verbose logging to console + file
    public boolean debugToChat = false;    // also echo debug lines to in-game chat

    // Optional Hypixel API key. Public endpoints used by default do NOT need one.
    public String apiKey = "";

    public LuminConfig() {
        this.file = FabricLoader.getInstance().getConfigDir().resolve("luminclient.json").toFile();
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
            if (o.has("automationEnabled")) automationEnabled = o.get("automationEnabled").getAsBoolean();
            if (o.has("clickDelayMs")) clickDelayMs = o.get("clickDelayMs").getAsInt();
            if (o.has("automationMaxSpendPerOrder")) automationMaxSpendPerOrder = o.get("automationMaxSpendPerOrder").getAsDouble();
            if (o.has("automationMaxOrdersPerScan")) automationMaxOrdersPerScan = o.get("automationMaxOrdersPerScan").getAsInt();
            if (o.has("debugMode")) debugMode = o.get("debugMode").getAsBoolean();
            if (o.has("debugToChat")) debugToChat = o.get("debugToChat").getAsBoolean();
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
        o.addProperty("automationEnabled", automationEnabled);
        o.addProperty("clickDelayMs", clickDelayMs);
        o.addProperty("automationMaxSpendPerOrder", automationMaxSpendPerOrder);
        o.addProperty("automationMaxOrdersPerScan", automationMaxOrdersPerScan);
        o.addProperty("debugMode", debugMode);
        o.addProperty("debugToChat", debugToChat);
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
