package com.lumin.luminclient.core;

import com.lumin.luminclient.auto.AutomationEngine;
import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.flip.FlipOpportunity;
import com.lumin.luminclient.gui.GuiManager;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

/**
 * Handles ".lumin ..." typed in chat.
 *
 * Why "." instead of "/"? Because "/" is intercepted by the vanilla command
 * system and only works for commands that are actually registered on the
 * client dispatcher. "." is sent as a normal chat message, which we can
 * intercept reliably on any server (including Hypixel).
 */
public final class ChatCommandHandler {

    private static LuminConfig config;
    private static FlipEngine flipEngine;
    private static GuiManager guiManager;
    private static AutomationEngine automation;

    private ChatCommandHandler() {}

    public static void register(LuminConfig cfg,
                                FlipEngine fe,
                                GuiManager gm,
                                AutomationEngine auto) {
        config = cfg;
        flipEngine = fe;
        guiManager = gm;
        automation = auto;

        ClientSendMessageEvents.CHAT.register(ChatCommandHandler::onChat);
    }

    private static void onChat(String message) {
        if (message == null) return;
        String trimmed = message.trim();
        if (!trimmed.toLowerCase(Locale.ROOT).startsWith(".lumin")) return;

        // Parse args
        String[] parts = trimmed.split("\\s+");
        // parts[0] == ".lumin"
        String sub = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "";

        switch (sub) {
            case "gui":
                guiManager.openFlipsScreen();
                break;
            case "refresh":
                flipEngine.refreshNow();
                say("[Lumin] Scanning market...");
                break;
            case "top":
                top();
                break;
            case "auto":
                auto(parts);
                break;
            case "set":
                set(parts);
                break;
            case "debug":
                debug(parts);
                break;
            case "":
            case "help":
            default:
                say("[Lumin] .lumin gui|refresh|top|auto|set|debug");
                break;
        }
    }

    private static void top() {
        List<FlipOpportunity> flips = flipEngine.getLatestFlips();
        if (flips.isEmpty()) {
            say("[Lumin] No flips found yet. Try .lumin refresh");
            return;
        }
        say("[Lumin] Top flips:");
        int i = 0;
        for (FlipOpportunity f : flips) {
            if (i++ >= 10) break;
            say(String.format("  %s | buy %.0f sell %.0f | +%.0f (%.1f%%)",
                    f.display, f.buyAt, f.sellAt, f.profitPerUnit, f.profitPercent));
        }
    }

    private static void auto(String[] parts) {
        if (parts.length < 3) {
            say("[Lumin] automation is " + (automation.isEnabled() ? "ON" : "OFF") + ". Use .lumin auto on|off");
            return;
        }
        boolean on = parts[2].equalsIgnoreCase("on") || parts[2].equalsIgnoreCase("true");
        automation.setEnabled(on);
        config.automationEnabled = on;
        config.save();
        say("[Lumin] Automation " + (on ? "ON" : "OFF"));
    }

    private static void set(String[] parts) {
        if (parts.length < 4) {
            say("[Lumin] .lumin set minmargin|budget|maxspend <value>");
            return;
        }
        String key = parts[2].toLowerCase(Locale.ROOT);
        try {
            double val = Double.parseDouble(parts[3]);
            switch (key) {
                case "minmargin":
                    config.minMarginPercent = val;
                    say("[Lumin] minMarginPercent=" + val);
                    break;
                case "budget":
                    config.maxBudget = val;
                    say("[Lumin] maxBudget=" + val);
                    break;
                case "maxspend":
                    config.automationMaxSpendPerOrder = val;
                    say("[Lumin] automationMaxSpendPerOrder=" + val);
                    break;
                default:
                    say("[Lumin] Unknown key: " + key);
                    return;
            }
            config.save();
        } catch (NumberFormatException e) {
            say("[Lumin] Invalid number: " + parts[3]);
        }
    }

    private static void debug(String[] parts) {
        if (parts.length < 3) {
            say("[Lumin] .lumin debug on|off|chat <on|off>|tail");
            return;
        }
        String sub = parts[2].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "on":
                config.debugMode = true;
                config.save();
                say("[Lumin] Debug mode ON. Log: " + Debug.getLogFilePath());
                break;
            case "off":
                config.debugMode = false;
                config.save();
                say("[Lumin] Debug mode OFF");
                break;
            case "chat":
                if (parts.length < 4) {
                    say("[Lumin] debugToChat=" + config.debugToChat);
                    return;
                }
                config.debugToChat = parts[3].equalsIgnoreCase("on") || parts[3].equalsIgnoreCase("true");
                config.save();
                say("[Lumin] debugToChat=" + config.debugToChat);
                break;
            case "tail":
                String[] lines = Debug.tail(10);
                if (lines.length == 0) {
                    say("[Lumin] No debug lines yet.");
                    return;
                }
                say("[Lumin] Last " + lines.length + " debug lines:");
                for (String l : lines) {
                    say("  " + l);
                }
                break;
            default:
                say("[Lumin] .lumin debug on|off|chat <on|off>|tail");
                break;
        }
    }

    private static void say(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(msg), false);
        }
    }
}
