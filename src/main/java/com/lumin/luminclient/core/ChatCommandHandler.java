package com.lumin.luminclient.core;

import com.lumin.luminclient.auto.AutomationEngine;
import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.gui.GuiManager;
import com.lumin.luminclient.stats.SessionAnalytics;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

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

    private static Context ctx;
    private static boolean registered = false;

    public static final class Context {
        public final LuminConfig config;
        public final FlipEngine flipEngine;
        public final GuiManager guiManager;
        public final AutomationEngine automation;
        public final SessionAnalytics analytics;

        public Context(LuminConfig config, FlipEngine flipEngine, GuiManager guiManager, AutomationEngine automation, SessionAnalytics analytics) {
            this.config = config;
            this.flipEngine = flipEngine;
            this.guiManager = guiManager;
            this.automation = automation;
            this.analytics = analytics;
        }
    }

    private ChatCommandHandler() {}

    public static void register(LuminConfig cfg,
                                FlipEngine fe,
                                GuiManager gm,
                                AutomationEngine auto,
                                SessionAnalytics analytics) {
        ctx = new Context(cfg, fe, gm, auto, analytics);
        if (!registered) {
            ClientSendMessageEvents.CHAT.register(ChatCommandHandler::onChat);
            registered = true;
        }
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
                emit(CommandActions.openGui(ctx.guiManager));
                break;
            case "refresh":
                emit(CommandActions.refresh(ctx.flipEngine));
                break;
            case "top":
                emit(CommandActions.top(ctx.flipEngine, false));
                break;
            case "auto":
                emit(CommandActions.auto(parts.length > 2 ? parts[2] : "", ctx.config, ctx.automation));
                break;
            case "set":
                emit(CommandActions.set(parts.length > 2 ? parts[2] : null, parts.length > 3 ? parts[3] : null, ctx.config));
                break;
            case "debug":
                emit(CommandActions.debug(parts.length > 2
                        ? java.util.Arrays.copyOfRange(parts, 2, parts.length)
                        : new String[0], ctx.config));
                break;
            case "stats":
                emit(CommandActions.stats(ctx.analytics));
                break;
            case "":
            case "help":
            default:
                emit(CommandActions.help(false));
                break;
        }
    }

    private static void emit(java.util.List<String> lines) { for (String line : lines) say(line); }

    private static void say(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(msg), false);
        }
    }
}
