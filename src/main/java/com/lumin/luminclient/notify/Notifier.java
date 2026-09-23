package com.lumin.luminclient.notify;

import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.flip.FlipOpportunity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Chat + sound notifications when new flips are found.
 */
public final class Notifier {

    private Notifier() {}

    public static void register(LuminConfig config, FlipEngine flipEngine) {
        flipEngine.addListener((List<FlipOpportunity> flips) -> {
            if (!config.chatNotifications && !config.soundAlerts) return;
            if (flips == null || flips.isEmpty()) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            FlipOpportunity best = flips.get(0);
            if (config.chatNotifications) {
                mc.player.sendMessage(Text.literal(String.format(
                        "[Lumin] Best flip: %s +%.0f (%.1f%%)",
                        best.display, best.profitPerUnit, best.profitPercent)), false);
            }
            if (config.soundAlerts && mc.player != null) {
                mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        });
    }
}
