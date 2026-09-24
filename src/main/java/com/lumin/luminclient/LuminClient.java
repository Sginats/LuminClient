package com.lumin.luminclient;

import com.lumin.luminclient.auto.AutomationEngine;
import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.core.ChatCommandHandler;
import com.lumin.luminclient.core.Debug;
import com.lumin.luminclient.core.Keybinds;
import com.lumin.luminclient.core.LuminCommand;
import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.gui.GuiManager;
import com.lumin.luminclient.notify.Notifier;
import com.lumin.luminclient.stats.SessionAnalytics;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
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
    private SessionAnalytics analytics;

    @Override
    public void onInitializeClient() {
        instance = this;

        config = new LuminConfig();
        config.load();
        Debug.init(config);
        Debug.log(Debug.Category.CONFIG, "Config loaded. debugMode=" + config.debugMode + " debugToChat=" + config.debugToChat);

        analytics = new SessionAnalytics();
        flipEngine = new FlipEngine(config);
        guiManager = new GuiManager(config, flipEngine);
        automationEngine = new AutomationEngine(config, flipEngine, analytics);

        Keybinds.register(config, guiManager, automationEngine);
        LuminCommand.register(config, flipEngine, guiManager, automationEngine, analytics);
        ChatCommandHandler.register(config, flipEngine, guiManager, automationEngine, analytics);
        Notifier.register(config, flipEngine);

        flipEngine.start();
        automationEngine.start();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());

        LOGGER.info("{} initialized (automation enabled={})", MOD_NAME, config.automationEnabled);
    }

    private void shutdown() {
        if (automationEngine != null) {
            automationEngine.stop();
        }
        if (flipEngine != null) {
            flipEngine.stop();
        }
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
