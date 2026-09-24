package com.lumin.luminclient.core;

import com.lumin.luminclient.auto.AutomationEngine;
import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.flip.FlipOpportunity;
import com.lumin.luminclient.gui.GuiManager;
import com.lumin.luminclient.stats.SessionAnalytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CommandActions {
    private CommandActions() {}

    public static List<String> help(boolean slashStyle) {
        String prefix = slashStyle ? "/lumin" : ".lumin";
        return lines("[Lumin] " + prefix + " gui|refresh|auto|set|top|debug|stats");
    }

    public static List<String> openGui(GuiManager guiManager) {
        guiManager.openFlipsScreen();
        return lines("[Lumin] Opened flips screen.");
    }

    public static List<String> refresh(FlipEngine flipEngine) {
        flipEngine.refreshNow();
        return lines("[Lumin] Scanning market...");
    }

    public static List<String> top(FlipEngine flipEngine, boolean slashStyle) {
        List<FlipOpportunity> flips = flipEngine.getLatestFlips();
        if (flips.isEmpty()) {
            String hint = slashStyle ? "/lumin refresh" : ".lumin refresh";
            return lines("[Lumin] No flips found yet. Try " + hint);
        }
        List<String> out = new ArrayList<String>();
        out.add("[Lumin] Top flips:");
        int i = 0;
        for (FlipOpportunity f : flips) {
            if (i++ >= 10) break;
            out.add(String.format("  %s | buy %.0f sell %.0f | +%.0f (%.1f%%)",
                    f.display, f.buyAt, f.sellAt, f.profitPerUnit, f.profitPercent));
        }
        return out;
    }

    public static List<String> auto(String value, LuminConfig config, AutomationEngine automation) {
        if (value == null || value.isEmpty()) {
            return lines("[Lumin] automation is " + (automation.isEnabled() ? "ON" : "OFF") + ". Use auto on|off");
        }
        boolean on = value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true");
        automation.setEnabled(on);
        config.automationEnabled = on;
        config.save();
        return lines("[Lumin] Automation " + (on ? "ON" : "OFF"));
    }

    public static List<String> set(String key, String value, LuminConfig config) {
        if (key == null || value == null) {
            return lines("[Lumin] set minmargin|budget|maxspend <value>");
        }
        try {
            double val = Double.parseDouble(value);
            switch (key.toLowerCase(Locale.ROOT)) {
                case "minmargin":
                    config.minMarginPercent = val;
                    config.save();
                    return lines("[Lumin] minMarginPercent=" + config.minMarginPercent);
                case "budget":
                    config.maxBudget = val;
                    config.save();
                    return lines("[Lumin] maxBudget=" + config.maxBudget);
                case "maxspend":
                    config.automationMaxSpendPerOrder = val;
                    config.save();
                    return lines("[Lumin] automationMaxSpendPerOrder=" + config.automationMaxSpendPerOrder);
                default:
                    return lines("[Lumin] Unknown key: " + key);
            }
        } catch (NumberFormatException e) {
            return lines("[Lumin] Invalid number: " + value);
        }
    }

    public static List<String> debug(String[] args, LuminConfig config) {
        if (args == null || args.length == 0) {
            return lines("[Lumin] debug on|off|chat <on|off>|tail");
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "on":
                config.debugMode = true;
                config.save();
                return lines("[Lumin] Debug mode ON. Log: " + Debug.getLogFilePath());
            case "off":
                config.debugMode = false;
                config.save();
                return lines("[Lumin] Debug mode OFF");
            case "chat":
                if (args.length < 2) {
                    return lines("[Lumin] debugToChat=" + config.debugToChat);
                }
                config.debugToChat = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
                config.save();
                return lines("[Lumin] debugToChat=" + config.debugToChat);
            case "tail":
                String[] lines = Debug.tail(10);
                if (lines.length == 0) {
                    return lines("[Lumin] No debug lines yet.");
                }
                List<String> out = new ArrayList<String>();
                out.add("[Lumin] Last " + lines.length + " debug lines:");
                for (String l : lines) out.add("  " + l);
                return out;
            default:
                return lines("[Lumin] debug on|off|chat <on|off>|tail");
        }
    }

    public static List<String> stats(SessionAnalytics analytics) {
        SessionAnalytics.Summary s = analytics.getSummary();
        return lines(
                "[Lumin] Session stats:",
                "  attempted=" + s.attempted + " success=" + s.succeeded + " failed=" + s.failed,
                String.format("  estPnL=%.0f spend=%.0f avgLatencyMs=%.1f", s.estimatedPnl, s.totalSpend, s.avgLatencyMs),
                "  failures=" + (s.failureReasons.isEmpty() ? "{}" : s.failureReasons.toString())
        );
    }

    private static List<String> lines(String... values) {
        List<String> out = new ArrayList<String>(values.length);
        for (String v : values) out.add(v);
        return out;
    }
}
