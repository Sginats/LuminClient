package com.lumin.luminclient.auto;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Low-level GUI helpers for driving Hypixel's bazaar/AH screens.
 *
 * Hypixel uses custom inventory GUIs (HandledScreen with a slot grid). We interact
 * with them by clicking slots, exactly like a player would with a mouse.
 */
public final class GuiAutomation {

    private GuiAutomation() {}

    /** Click a slot in the currently open handled screen. */
    public static void clickSlot(HandledScreen<?> screen, int slotId, int button, SlotActionType action) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(
                screen.getScreenHandler().syncId,
                slotId,
                button,
                action,
                mc.player
        );
    }

    /** Find a slot by matching text in the item name or lore (case-insensitive). */
    public static Slot findSlotByName(HandledScreen<?> screen, String needle) {
        String n = needle.toLowerCase();
        for (Slot slot : screen.getScreenHandler().slots) {
            if (!slot.hasStack()) continue;
            String name = slot.getStack().getName().getString().toLowerCase();
            if (name.contains(n)) return slot;
        }
        return null;
    }

    /** Find a slot whose item name exactly matches (case-insensitive). */
    public static Slot findSlotByExactName(HandledScreen<?> screen, String name) {
        for (Slot slot : screen.getScreenHandler().slots) {
            if (!slot.hasStack()) continue;
            if (slot.getStack().getName().getString().equalsIgnoreCase(name)) return slot;
        }
        return null;
    }
}
