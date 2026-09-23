package com.lumin.luminclient.mixin;

import com.lumin.luminclient.core.Debug;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook into Hypixel's inventory GUIs (bazaar, AH) so we can observe which screen
 * is open and dump slots in debug mode.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    private static boolean lumin$loggedScreen = false;

    @Inject(method = "render", at = @At("TAIL"))
    private void lumin$renderOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!Debug.isEnabled()) {
            lumin$loggedScreen = false;
            return;
        }
        try {
            HandledScreen<?> self = (HandledScreen<?>) (Object) this;
            if (!lumin$loggedScreen) {
                Debug.log(Debug.Category.GUI, "HandledScreen opened: " + self.getTitle().getString()
                        + " (" + self.getClass().getSimpleName() + ")");
                lumin$loggedScreen = true;
            }
        } catch (Throwable ignored) {
        }
    }
}
