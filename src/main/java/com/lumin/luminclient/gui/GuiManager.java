package com.lumin.luminclient.gui;

import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.flip.FlipEngine;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Manages the HUD overlay and the FlipsScreen.
 */
public class GuiManager {

    private final LuminConfig config;
    private final FlipEngine flipEngine;

    public GuiManager(LuminConfig config, FlipEngine flipEngine) {
        this.config = config;
        this.flipEngine = flipEngine;

        // Draw a tiny HUD with the current flip count
        HudRenderCallback.EVENT.register((DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;
            int count = flipEngine.getLatestFlips().size();
            if (count <= 0) return;
            String text = "[Lumin] " + count + " flips";
            context.drawTextWithShadow(mc.textRenderer, text, 6, 6, 0x55FFFF);
        });
    }

    public void openFlipsScreen() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setScreen(new FlipsScreen(flipEngine));
    }
}
