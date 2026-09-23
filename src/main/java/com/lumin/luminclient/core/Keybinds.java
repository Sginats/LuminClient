package com.lumin.luminclient.core;

import com.lumin.luminclient.auto.AutomationEngine;
import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.gui.GuiManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Keybinds for opening the GUI and toggling automation.
 */
public final class Keybinds {

    private static KeyBinding openGui;
    private static KeyBinding toggleAutomation;

    private Keybinds() {}

    public static void register(LuminConfig config, GuiManager guiManager, AutomationEngine automation) {
        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.luminclient.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "key.categories.luminclient"
        ));

        toggleAutomation = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.luminclient.toggle_automation",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "key.categories.luminclient"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGui.wasPressed()) {
                guiManager.openFlipsScreen();
            }
            while (toggleAutomation.wasPressed()) {
                boolean on = !automation.isEnabled();
                automation.setEnabled(on);
                config.automationEnabled = on;
                config.save();
            }
        });
    }
}
