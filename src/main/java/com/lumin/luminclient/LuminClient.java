package com.lumin.luminclient;

import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.core.CommandLumin;
import com.lumin.luminclient.core.Keybinds;
import com.lumin.luminclient.core.TickHandler;
import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.gui.GuiManager;
import com.lumin.luminclient.notify.Notifier;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * LuminClient - Hypixel SkyBlock flip assistant.
 *
 * Market analytics only. This mod reads the public Hypixel API and shows
 * profitable bazaar / auction flips. It never sends input, never clicks,
 * and never plays the game for the user.
 */
@Mod(
        modid = LuminClient.MOD_ID,
        name = LuminClient.MOD_NAME,
        version = LuminClient.MOD_VERSION,
        clientSideOnly = true,
        acceptedMinecraftVersions = "[1.8.9]"
)
public class LuminClient {

    public static final String MOD_ID = "luminclient";
    public static final String MOD_NAME = "LuminClient";
    public static final String MOD_VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @Mod.Instance(MOD_ID)
    public static LuminClient instance;

    private LuminConfig config;
    private FlipEngine flipEngine;
    private GuiManager guiManager;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new LuminConfig(event.getSuggestedConfigurationFile());
        config.load();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        flipEngine = new FlipEngine(config);
        guiManager = new GuiManager(config, flipEngine);

        MinecraftForge.EVENT_BUS.register(new TickHandler(config, flipEngine, guiManager));
        MinecraftForge.EVENT_BUS.register(guiManager);
        MinecraftForge.EVENT_BUS.register(new Notifier(config, flipEngine));

        Keybinds.register();
        ClientCommandHandler.instance.registerCommand(new CommandLumin(config, flipEngine, guiManager));

        flipEngine.start();
        LOGGER.info("{} {} initialized", MOD_NAME, MOD_VERSION);
    }

    public LuminConfig getConfig() {
        return config;
    }

    public FlipEngine getFlipEngine() {
        return flipEngine;
    }
}
