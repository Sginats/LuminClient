package com.lumin.luminclient.core;

import com.lumin.luminclient.LuminClient;
import com.lumin.luminclient.config.LuminConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Debug / diagnostics mode.
 *
 * When enabled, LuminClient logs verbose, timestamped diagnostics to:
 *  - the game console (via the mod logger)
 *  - the in-game chat (optional)
 *  - a rolling debug log file: config/luminclient-debug.log
 *
 * This is what you turn on when something goes wrong so you can see exactly
 * what the mod saw, what it decided, and where it failed.
 */
public final class Debug {

    public enum Category {
        API,        // HTTP requests/responses to Hypixel
        SCAN,       // market scan results
        FLIP,       // flip detection decisions
        AUTO,       // automation actions
        GUI,        // screen/slot detection
        CONFIG,     // config load/save
        ERROR       // errors (always logged)
    }

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int MAX_LOG_LINES = 2000;

    private static LuminConfig config;
    private static File logFile;
    private static final CopyOnWriteArrayList<String> recent = new CopyOnWriteArrayList<String>();

    private Debug() {}

    public static void init(LuminConfig cfg) {
        config = cfg;
        try {
            File dir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().toFile();
            logFile = new File(dir, "luminclient-debug.log");
        } catch (Throwable t) {
            logFile = null;
        }
    }

    public static boolean isEnabled() {
        return config != null && config.debugMode;
    }

    public static void log(Category cat, String msg) {
        log(cat, msg, null);
    }

    public static void log(Category cat, String msg, Throwable t) {
        boolean errorsOnly = (cat == Category.ERROR);
        if (!isEnabled() && !errorsOnly) return;

        String line = String.format("[%s] [%-5s] %s", TS.format(new Date()), cat.name(), msg);

        // 1) mod logger (console)
        if (cat == Category.ERROR) {
            LuminClient.LOGGER.error(line, t);
        } else {
            LuminClient.LOGGER.info(line, t);
        }

        // 2) file
        writeToFile(line, t);

        // 3) in-game chat (optional, only if enabled)
        if (config != null && config.debugToChat) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§7[Lumin:" + cat.name() + "]§r " + msg), false);
            }
        }
    }

    /** Record a recent debug line in memory (for /lumin debug tail). */
    private static void remember(String line) {
        recent.add(line);
        while (recent.size() > MAX_LOG_LINES) {
            recent.remove(0);
        }
    }

    private static synchronized void writeToFile(String line, Throwable t) {
        remember(line);
        if (logFile == null) return;
        try {
            logFile.getParentFile().mkdirs();
            try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
                pw.println(line);
                if (t != null) {
                    t.printStackTrace(pw);
                }
            }
        } catch (IOException ignored) {
        }
    }

    /** Last N debug lines (for /lumin debug tail). */
    public static String[] tail(int n) {
        int size = recent.size();
        int count = Math.min(n, size);
        String[] out = new String[count];
        for (int i = 0; i < count; i++) {
            out[i] = recent.get(size - count + i);
        }
        return out;
    }

    public static String getLogFilePath() {
        return logFile == null ? "(unavailable)" : logFile.getAbsolutePath();
    }
}
