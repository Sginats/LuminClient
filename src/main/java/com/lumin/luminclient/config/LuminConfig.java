package com.lumin.luminclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lumin.luminclient.LuminClient;
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
    private static final int CONFIG_VERSION = 1;

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

    // Trade safety controls
    public String[] automationWhitelistItemIds = new String[0];
    public String[] automationBlacklistItemIds = new String[0];
    public double automationMaxDailySpend = 5_000_000;
    public double automationMaxDailyLoss = 500_000;
    public int automationFailureCooldownSeconds = 60;
    public boolean automationPreventDuplicateOrders = true;

    public LuminConfig() {
        File f;
        try {
            f = FabricLoader.getInstance().getConfigDir().resolve("luminclient.json").toFile();
        } catch (Throwable t) {
            f = new File("build/tmp/luminclient-test-config.json");
        }
        this.file = f;
    }

    public void load() {
        if (!file.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
            int version = o.has("configVersion") ? o.get("configVersion").getAsInt() : 0;
            if (version < CONFIG_VERSION) {
                LuminClient.LOGGER.info("Migrating luminclient config from version {} to {}", version, CONFIG_VERSION);
            }
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
            if (o.has("automationWhitelistItemIds") && o.get("automationWhitelistItemIds").isJsonArray()) {
                automationWhitelistItemIds = GSON.fromJson(o.get("automationWhitelistItemIds"), String[].class);
            }
            if (o.has("automationBlacklistItemIds") && o.get("automationBlacklistItemIds").isJsonArray()) {
                automationBlacklistItemIds = GSON.fromJson(o.get("automationBlacklistItemIds"), String[].class);
            }
            if (o.has("automationMaxDailySpend")) automationMaxDailySpend = o.get("automationMaxDailySpend").getAsDouble();
            if (o.has("automationMaxDailyLoss")) automationMaxDailyLoss = o.get("automationMaxDailyLoss").getAsDouble();
            if (o.has("automationFailureCooldownSeconds")) automationFailureCooldownSeconds = o.get("automationFailureCooldownSeconds").getAsInt();
            if (o.has("automationPreventDuplicateOrders")) automationPreventDuplicateOrders = o.get("automationPreventDuplicateOrders").getAsBoolean();
            validate();
        } catch (Exception e) {
            LuminClient.LOGGER.warn("Failed to parse luminclient config (keeping defaults): {}", e.getMessage());
            validate();
        }
    }

    public void save() {
        validate();
        JsonObject o = new JsonObject();
        o.addProperty("configVersion", CONFIG_VERSION);
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
        o.add("automationWhitelistItemIds", GSON.toJsonTree(automationWhitelistItemIds == null ? new String[0] : automationWhitelistItemIds));
        o.add("automationBlacklistItemIds", GSON.toJsonTree(automationBlacklistItemIds == null ? new String[0] : automationBlacklistItemIds));
        o.addProperty("automationMaxDailySpend", automationMaxDailySpend);
        o.addProperty("automationMaxDailyLoss", automationMaxDailyLoss);
        o.addProperty("automationFailureCooldownSeconds", automationFailureCooldownSeconds);
        o.addProperty("automationPreventDuplicateOrders", automationPreventDuplicateOrders);
        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(o, writer);
            }
        } catch (IOException e) {
            LuminClient.LOGGER.warn("Failed to save luminclient config: {}", e.getMessage());
        }
    }

    private void validate() {
        scanIntervalSeconds = clampInt(scanIntervalSeconds, 5, 3600);
        minMarginPercent = clampDouble(minMarginPercent, 0.0, 200.0);
        minProfitPerFlip = clampDouble(minProfitPerFlip, 0.0, 1_000_000_000.0);
        maxBudget = clampDouble(maxBudget, 0.0, 10_000_000_000.0);
        maxResults = clampInt(maxResults, 1, 200);
        minBinFlipPercent = clampDouble(minBinFlipPercent, 0.0, 500.0);
        clickDelayMs = clampInt(clickDelayMs, 50, 5_000);
        automationMaxSpendPerOrder = clampDouble(automationMaxSpendPerOrder, 0.0, 10_000_000_000.0);
        automationMaxOrdersPerScan = clampInt(automationMaxOrdersPerScan, 1, 100);
        automationMaxDailySpend = clampDouble(automationMaxDailySpend, 0.0, 10_000_000_000.0);
        automationMaxDailyLoss = clampDouble(automationMaxDailyLoss, 0.0, 10_000_000_000.0);
        automationFailureCooldownSeconds = clampInt(automationFailureCooldownSeconds, 0, 3600);
        if (automationWhitelistItemIds == null) automationWhitelistItemIds = new String[0];
        if (automationBlacklistItemIds == null) automationBlacklistItemIds = new String[0];
        if (apiKey == null) apiKey = "";
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
