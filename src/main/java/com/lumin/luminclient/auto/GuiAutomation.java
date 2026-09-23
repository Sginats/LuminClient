package com.lumin.luminclient.auto;

import com.lumin.luminclient.core.Debug;
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
        if (mc.interactionManager == null || mc.player == null) {
            Debug.log(Debug.Category.ERROR, "clickSlot: interactionManager or player is null");
            return;
        }
        Debug.log(Debug.Category.GUI, "Clicking slot " + slotId + " button=" + button + " action=" + action);
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
            if (name.contains(n)) {
                Debug.log(Debug.Category.GUI, "findSlotByName(\"" + needle + "\") -> slot " + slot.id
                        + " name=\"" + slot.getStack().getName().getString() + "\"");
                return slot;
            }
        }
        Debug.log(Debug.Category.GUI, "findSlotByName(\"" + needle + "\") -> not found");
        return null;
    }

    /** Find a slot whose item name exactly matches (case-insensitive). */
    public static Slot findSlotByExactName(HandledScreen<?> screen, String name) {
        for (Slot slot : screen.getScreenHandler().slots) {
            if (!slot.hasStack()) continue;
            if (slot.getStack().getName().getString().equalsIgnoreCase(name)) {
                Debug.log(Debug.Category.GUI, "findSlotByExactName(\"" + name + "\") -> slot " + slot.id);
                return slot;
            }
        }
        Debug.log(Debug.Category.GUI, "findSlotByExactName(\"" + name + "\") -> not found");
        return null;
    }

    /** Dump all non-empty slots in the current screen to the debug log (for troubleshooting). */
    public static void dumpSlots(HandledScreen<?> screen) {
        if (!Debug.isEnabled()) return;
        Debug.log(Debug.Category.GUI, "=== Slot dump (" + screen.getScreenHandler().slots.size() + " slots) ===");
        for (Slot slot : screen.getScreenHandler().slots) {
            if (!slot.hasStack()) continue;
            Debug.log(Debug.Category.GUI, "  slot " + slot.id + ": " + slot.getStack().getName().getString());
        }
    }
}
