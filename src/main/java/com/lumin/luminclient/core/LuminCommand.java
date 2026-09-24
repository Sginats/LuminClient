package com.lumin.luminclient.core;

import com.lumin.luminclient.auto.AutomationEngine;
import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.gui.GuiManager;
import com.lumin.luminclient.stats.SessionAnalytics;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

/**
 * /lumin command tree.
 */
public final class LuminCommand {

    private LuminCommand() {}

    public static void register(LuminConfig config,
                                FlipEngine flipEngine,
                                GuiManager guiManager,
                                AutomationEngine automation,
                                SessionAnalytics analytics) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("lumin")
                .then(ClientCommandManager.literal("gui")
                    .executes(ctx -> {
                        sendAll(ctx, CommandActions.openGui(guiManager));
                        return 1;
                    }))
                .then(ClientCommandManager.literal("refresh")
                    .executes(ctx -> {
                        sendAll(ctx, CommandActions.refresh(flipEngine));
                        return 1;
                    }))
                .then(ClientCommandManager.literal("auto")
                    .then(ClientCommandManager.argument("on", BoolArgumentType.bool())
                        .executes(ctx -> {
                            sendAll(ctx, CommandActions.auto(String.valueOf(BoolArgumentType.getBool(ctx, "on")), config, automation));
                            return 1;
                        })))
                .then(ClientCommandManager.literal("set")
                    .then(ClientCommandManager.literal("minmargin")
                        .then(ClientCommandManager.argument("pct", DoubleArgumentType.doubleArg(0))
                            .executes(ctx -> {
                                sendAll(ctx, CommandActions.set("minmargin", String.valueOf(DoubleArgumentType.getDouble(ctx, "pct")), config));
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("budget")
                        .then(ClientCommandManager.argument("coins", DoubleArgumentType.doubleArg(0))
                            .executes(ctx -> {
                                sendAll(ctx, CommandActions.set("budget", String.valueOf(DoubleArgumentType.getDouble(ctx, "coins")), config));
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("maxspend")
                        .then(ClientCommandManager.argument("coins", DoubleArgumentType.doubleArg(0))
                            .executes(ctx -> {
                                sendAll(ctx, CommandActions.set("maxspend", String.valueOf(DoubleArgumentType.getDouble(ctx, "coins")), config));
                                return 1;
                            }))))
                .then(ClientCommandManager.literal("top")
                    .executes(ctx -> {
                        sendAll(ctx, CommandActions.top(flipEngine, true));
                        return 1;
                    }))
                .then(ClientCommandManager.literal("debug")
                    .then(ClientCommandManager.literal("on")
                        .executes(ctx -> {
                            sendAll(ctx, CommandActions.debug(new String[]{"on"}, config));
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("off")
                        .executes(ctx -> {
                            sendAll(ctx, CommandActions.debug(new String[]{"off"}, config));
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("chat")
                        .then(ClientCommandManager.argument("on", BoolArgumentType.bool())
                            .executes(ctx -> {
                                sendAll(ctx, CommandActions.debug(new String[]{"chat", String.valueOf(BoolArgumentType.getBool(ctx, "on"))}, config));
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("tail")
                        .executes(ctx -> {
                            sendAll(ctx, CommandActions.debug(new String[]{"tail"}, config));
                            return 1;
                        }))
                    .executes(ctx -> {
                        sendAll(ctx, CommandActions.debug(new String[0], config));
                        return 1;
                    }))
                .then(ClientCommandManager.literal("stats")
                    .executes(ctx -> {
                        sendAll(ctx, CommandActions.stats(analytics));
                        return 1;
                    }))
                .executes(ctx -> {
                    sendAll(ctx, CommandActions.help(true));
                    return 1;
                })
            );
        });
    }

    private static void sendAll(com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> ctx,
                                java.util.List<String> lines) {
        for (String line : lines) {
            ctx.getSource().sendFeedback(Text.literal(line));
        }
    }
}
