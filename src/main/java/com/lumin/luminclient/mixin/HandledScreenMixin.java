package com.lumin.luminclient.mixin;

import com.lumin.luminclient.LuminClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook into Hypixel's inventory GUIs (bazaar, AH) so we can draw overlays and
 * drive clicks.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void lumin$renderOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            if (LuminClient.getInstance() != null) {
                LuminClient.getInstance().getAutomationEngine();
                // Overlay rendering is handled by GuiManager via Fabric's HudRenderCallback.
            }
        } catch (Throwable ignored) {
        }
    }
}
