package com.lumin.luminclient;

import com.lumin.luminclient.auto.AutomationEngine;
import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.core.Keybinds;
import com.lumin.luminclient.core.LuminCommand;
import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.gui.GuiManager;
import com.lumin.luminclient.notify.Notifier;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LuminClient - Hypixel SkyBlock bazaar/auction flip assistant with full trade automation.
 *
 * The automation layer clicks GUI buttons, places orders, and pays for you.
 * USE AT YOUR OWN RISK. Automating gameplay on Hypixel violates their rules
 * and can result in a permanent account ban.
 */
public class LuminClient implements ClientModInitializer {

    public static final String MOD_ID = "luminclient";
    public static final String MOD_NAME = "LuminClient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static LuminClient instance;

    private LuminConfig config;
    private FlipEngine flipEngine;
    private GuiManager guiManager;
    private AutomationEngine automationEngine;

    @Override
    public void onInitializeClient() {
        instance = this;

        config = new LuminConfig();
        config.load();

        flipEngine = new FlipEngine(config);
        guiManager = new GuiManager(config, flipEngine);
        automationEngine = new AutomationEngine(config, flipEngine);

        Keybinds.register(config, guiManager, automationEngine);
        LuminCommand.register(config, flipEngine, guiManager, automationEngine);
        Notifier.register(config, flipEngine);

        flipEngine.start();
        automationEngine.start();

        LOGGER.info("{} initialized (automation enabled={})", MOD_NAME, config.automationEnabled);
    }

    public static LuminClient getInstance() {
        return instance;
    }

    public LuminConfig getConfig() {
        return config;
    }

    public FlipEngine getFlipEngine() {
        return flipEngine;
    }

    public AutomationEngine getAutomationEngine() {
        return automationEngine;
    }
}
