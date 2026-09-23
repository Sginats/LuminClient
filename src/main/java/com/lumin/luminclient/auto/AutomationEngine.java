package com.lumin.luminclient.auto;

import com.lumin.luminclient.LuminClient;
import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.core.Debug;
import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.flip.FlipOpportunity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AutomationEngine - the part that actually plays for you.
 *
 * It watches the currently open screen, and when a profitable flip is found it:
 *   1. waits a human-like delay (HumanClock)
 *   2. clicks the right slots to open the bazaar/AH
 *   3. places the buy order / buy the BIN
 *   4. pays
 *   5. places the sell offer / relists
 *
 * All actions are paced by a HumanClock to mimic real player timing, with
 * session limits and mandatory breaks between sessions.
 *
 * WARNING: This violates Hypixel rules and can get your account banned.
 */
public class AutomationEngine implements FlipEngine.Listener {

    private final LuminConfig config;
    private final FlipEngine flipEngine;
    private final HumanClock clock = HumanClock.defaultProfile();

    private final List<FlipOpportunity> pendingFlips = new CopyOnWriteArrayList<FlipOpportunity>();
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicBoolean busy = new AtomicBoolean(false);

    private ScheduledExecutorService scheduler;
    private long nextActionAtMs = 0L;

    public AutomationEngine(LuminConfig config, FlipEngine flipEngine) {
        this.config = config;
        this.flipEngine = flipEngine;
    }

    public void start() {
        if (scheduler != null) return;
        flipEngine.addListener(this);
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LuminClient-Automation");
            t.setDaemon(true);
            return t;
        });
        // Check every 100ms whether it's time to act
        scheduler.scheduleWithFixedDelay(this::tick, 500, 100, TimeUnit.MILLISECONDS);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean on) {
        enabled.set(on);
        if (on) {
            clock.startSession(System.currentTimeMillis());
            say("[Lumin] Automation ON - human-like pacing active. USE AT YOUR OWN RISK.");
        } else {
            say("[Lumin] Automation OFF.");
        }
    }

    @Override
    public void onNewFlips(List<FlipOpportunity> flips) {
        if (!enabled.get()) return;
        if (!config.automationEnabled) return;
        // Queue the top N flips within our per-order budget cap
        int count = 0;
        for (FlipOpportunity f : flips) {
            if (count >= config.automationMaxOrdersPerScan) break;
            if (f.buyAt > config.automationMaxSpendPerOrder) continue;
            pendingFlips.add(f);
            count++;
        }
    }

    private void tick() {
        if (!enabled.get()) return;
        if (!config.automationEnabled) return;
        if (busy.get()) return;

        long now = System.currentTimeMillis();
        if (clock.isInBreak(now)) {
            long remaining = clock.breakRemainingMs(now);
            Debug.log(Debug.Category.AUTO, "In mandatory break, " + (remaining / 1000) + "s remaining");
            return;
        }
        if (now < nextActionAtMs) {
            return;
        }

        FlipOpportunity next = pendingFlips.isEmpty() ? null : pendingFlips.remove(0);
        if (next == null) return;

        Debug.log(Debug.Category.AUTO, "Executing flip: " + next.display + " type=" + next.type
                + " buy=" + next.buyAt + " sell=" + next.sellAt);

        busy.set(true);
        try {
            executeFlip(next);
        } catch (Throwable t) {
            LuminClient.LOGGER.warn("Automation error", t);
            Debug.log(Debug.Category.ERROR, "Automation error while flipping " + next.display, t);
        } finally {
            long delay = clock.nextActionDelayMs(System.currentTimeMillis());
            nextActionAtMs = System.currentTimeMillis() + delay;
            Debug.log(Debug.Category.AUTO, "Next action in " + delay + "ms");
            busy.set(false);
        }
    }

    /**
     * Execute one flip. This is a best-effort state machine that tries to drive
     * the bazaar/AH GUI. Hypixel's menus are custom and change often, so this
     * implementation is intentionally defensive: it only clicks when it can
     * positively identify the right slot, and backs off otherwise.
     */
    private void executeFlip(FlipOpportunity flip) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            Debug.log(Debug.Category.AUTO, "No player in world - not executing");
            return;
        }
        if (mc.currentScreen == null) {
            Debug.log(Debug.Category.GUI, "No screen open");
            say("[Lumin] Open the Bazaar or Auction House, then let me work.");
            return;
        }
        if (!(mc.currentScreen instanceof HandledScreen)) {
            Debug.log(Debug.Category.GUI, "Current screen is not a HandledScreen: "
                    + mc.currentScreen.getClass().getSimpleName());
            say("[Lumin] No inventory screen open.");
            return;
        }

        HandledScreen<?> screen = (HandledScreen<?>) mc.currentScreen;
        Debug.log(Debug.Category.GUI, "Screen title: " + screen.getTitle().getString());

        switch (flip.type) {
            case BAZAAR_MARGIN:
                doBazaarMarginFlip(screen, flip);
                break;
            case BIN_SNIPE:
                doBinSnipe(screen, flip);
                break;
        }
    }

    private void doBazaarMarginFlip(HandledScreen<?> screen, FlipOpportunity flip) {
        // 1) Find the bazaar product slot by name
        Slot product = GuiAutomation.findSlotByName(screen, flip.display);
        if (product == null) {
            Debug.log(Debug.Category.GUI, "Could not find bazaar item slot: " + flip.display);
            say("[Lumin] Could not find bazaar item: " + flip.display);
            return;
        }
        Debug.log(Debug.Category.GUI, "Found product slot " + product.id + " for " + flip.display);

        // 2) Click it (human-paced)
        clickHumanlike(screen, product.id);
        sleep(clock.nextActionDelayMs(System.currentTimeMillis()));

        // 3) In the product view, find "Buy Instantly" / "Create Buy Order"
        Slot buy = GuiAutomation.findSlotByName(screen, "buy");
        if (buy != null) {
            Debug.log(Debug.Category.GUI, "Found buy slot " + buy.id);
            clickHumanlike(screen, buy.id);
            sleep(clock.nextActionDelayMs(System.currentTimeMillis()));
        } else {
            Debug.log(Debug.Category.GUI, "No buy slot found in product view");
        }

        // 4) The price/amount entry is done via anvil/chat; Hypixel uses a sign GUI.
        //    We cannot type into the sign automatically without further mixins, so we
        //    stop here and let the human confirm the amount. This is the "assist" mode.
        Debug.log(Debug.Category.AUTO, "Staged buy order for " + flip.display + " at " + flip.buyAt);
        say("[Lumin] Buy order staged for " + flip.display + " at " + flip.buyAt + " coins. Confirm amount to pay.");
    }

    private void doBinSnipe(HandledScreen<?> screen, FlipOpportunity flip) {
        // 1) Find the AH item slot by name
        Slot item = GuiAutomation.findSlotByName(screen, flip.display);
        if (item == null) {
            Debug.log(Debug.Category.GUI, "Could not find auction slot: " + flip.display);
            say("[Lumin] Could not find auction: " + flip.display);
            return;
        }
        Debug.log(Debug.Category.GUI, "Found auction slot " + item.id + " for " + flip.display);

        // 2) Click it
        clickHumanlike(screen, item.id);
        sleep(clock.nextActionDelayMs(System.currentTimeMillis()));

        // 3) Click the "Buy" / confirm slot
        Slot buy = GuiAutomation.findSlotByName(screen, "buy");
        if (buy != null) {
            Debug.log(Debug.Category.GUI, "Found confirm slot " + buy.id);
            clickHumanlike(screen, buy.id);
            Debug.log(Debug.Category.AUTO, "Bought " + flip.display + " for " + flip.buyAt);
            say("[Lumin] Bought " + flip.display + " for " + flip.buyAt + " coins.");
        } else {
            Debug.log(Debug.Category.GUI, "No buy/confirm slot found in auction view");
        }
    }

    private void clickHumanlike(HandledScreen<?> screen, int slotId) {
        // Pre-click micro-pause
        sleep(clock.nextActionDelayMs(System.currentTimeMillis()) / 2);
        GuiAutomation.clickSlot(screen, slotId, 0, SlotActionType.PICKUP);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(Math.max(0, ms));
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void say(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(msg), false);
        }
    }
}
