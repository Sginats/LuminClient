package com.lumin.luminclient.core;

import com.lumin.luminclient.auto.AutomationEngine;
import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.flip.FlipOpportunity;
import com.lumin.luminclient.gui.GuiManager;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

import java.util.List;

/**
 * /lumin command tree.
 */
public final class LuminCommand {

    private LuminCommand() {}

    public static void register(LuminConfig config,
                                FlipEngine flipEngine,
                                GuiManager guiManager,
                                AutomationEngine automation) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("lumin")
                .then(ClientCommandManager.literal("gui")
                    .executes(ctx -> {
                        guiManager.openFlipsScreen();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("refresh")
                    .executes(ctx -> {
                        flipEngine.refreshNow();
                        ctx.getSource().sendFeedback(Text.literal("[Lumin] Scanning market..."));
                        return 1;
                    }))
                .then(ClientCommandManager.literal("auto")
                    .then(ClientCommandManager.argument("on", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean on = BoolArgumentType.getBool(ctx, "on");
                            automation.setEnabled(on);
                            config.automationEnabled = on;
                            config.save();
                            ctx.getSource().sendFeedback(Text.literal("[Lumin] Automation " + (on ? "ON" : "OFF")));
                            return 1;
                        })))
                .then(ClientCommandManager.literal("set")
                    .then(ClientCommandManager.literal("minmargin")
                        .then(ClientCommandManager.argument("pct", DoubleArgumentType.doubleArg(0))
                            .executes(ctx -> {
                                config.minMarginPercent = DoubleArgumentType.getDouble(ctx, "pct");
                                config.save();
                                ctx.getSource().sendFeedback(Text.literal("[Lumin] minMarginPercent=" + config.minMarginPercent));
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("budget")
                        .then(ClientCommandManager.argument("coins", DoubleArgumentType.doubleArg(0))
                            .executes(ctx -> {
                                config.maxBudget = DoubleArgumentType.getDouble(ctx, "coins");
                                config.save();
                                ctx.getSource().sendFeedback(Text.literal("[Lumin] maxBudget=" + config.maxBudget));
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("maxspend")
                        .then(ClientCommandManager.argument("coins", DoubleArgumentType.doubleArg(0))
                            .executes(ctx -> {
                                config.automationMaxSpendPerOrder = DoubleArgumentType.getDouble(ctx, "coins");
                                config.save();
                                ctx.getSource().sendFeedback(Text.literal("[Lumin] automationMaxSpendPerOrder=" + config.automationMaxSpendPerOrder));
                                return 1;
                            }))))
                .then(ClientCommandManager.literal("top")
                    .executes(ctx -> {
                        List<FlipOpportunity> flips = flipEngine.getLatestFlips();
                        if (flips.isEmpty()) {
                            ctx.getSource().sendFeedback(Text.literal("[Lumin] No flips found yet. Try /lumin refresh"));
                            return 0;
                        }
                        ctx.getSource().sendFeedback(Text.literal("[Lumin] Top flips:"));
                        int i = 0;
                        for (FlipOpportunity f : flips) {
                            if (i++ >= 10) break;
                            ctx.getSource().sendFeedback(Text.literal(String.format(
                                    "  %s | buy %.0f sell %.0f | +%.0f (%.1f%%)",
                                    f.display, f.buyAt, f.sellAt, f.profitPerUnit, f.profitPercent)));
                        }
                        return 1;
                    }))
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Text.literal("[Lumin] /lumin gui|refresh|auto|set|top"));
                    return 1;
                })
            );
        });
    }
}
