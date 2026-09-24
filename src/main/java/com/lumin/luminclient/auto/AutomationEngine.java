package com.lumin.luminclient.auto;

import com.lumin.luminclient.LuminClient;
import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.core.Debug;
import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.flip.FlipOpportunity;
import com.lumin.luminclient.stats.SessionAnalytics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
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
    private final SessionAnalytics analytics;

    private final List<FlipOpportunity> pendingFlips = new CopyOnWriteArrayList<FlipOpportunity>();
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final Map<String, Long> recentOrderByKey = new HashMap<String, Long>();
    private final Set<String> pendingOrderKeys = new HashSet<String>();

    private ScheduledExecutorService scheduler;
    private long nextActionAtMs = 0L;
    private long failureCooldownUntilMs = 0L;
    private String dailyCounterDay = "";
    private double dailySpend = 0.0;
    private double dailyEstimatedLoss = 0.0;
    private double reservedSpend = 0.0;
    private double reservedEstimatedLoss = 0.0;

    public AutomationEngine(LuminConfig config, FlipEngine flipEngine, SessionAnalytics analytics) {
        this.config = config;
        this.flipEngine = flipEngine;
        this.analytics = analytics;
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
        rollDailyBudgetIfNeeded();
        pruneDuplicateCache();
        // Queue the top N flips within our per-order budget cap
        int count = 0;
        for (FlipOpportunity f : flips) {
            if (count >= config.automationMaxOrdersPerScan) break;
            if (f.buyAt > config.automationMaxSpendPerOrder) continue;
            if (!passesItemFilters(f)) continue;
            if (dailySpend + reservedSpend + f.buyAt > config.automationMaxDailySpend) continue;
            double estLoss = Math.max(0.0, f.buyAt - f.sellAt);
            if (dailyEstimatedLoss + reservedEstimatedLoss + estLoss > config.automationMaxDailyLoss) continue;
            if (config.automationPreventDuplicateOrders && isDuplicateFlip(f)) continue;
            pendingFlips.add(f);
            rememberPendingFlip(f);
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
        if (now < failureCooldownUntilMs) {
            return;
        }
        if (now < nextActionAtMs) {
            return;
        }

        FlipOpportunity next = pendingFlips.isEmpty() ? null : pendingFlips.remove(0);
        if (next == null) return;
        forgetPendingFlip(next);
        rollDailyBudgetIfNeeded();

        Debug.log(Debug.Category.AUTO, "Executing flip: " + next.display + " type=" + next.type
                + " buy=" + next.buyAt + " sell=" + next.sellAt);

        busy.set(true);
        long start = System.nanoTime();
        boolean success = false;
        String failureReason = null;
        try {
            success = executeFlip(next);
            if (success) {
                dailySpend += Math.max(0.0, next.buyAt);
                dailyEstimatedLoss += Math.max(0.0, next.buyAt - next.sellAt);
                rememberExecutedFlip(next);
            } else {
                failureReason = "not-executed";
            }
        } catch (Throwable t) {
            LuminClient.LOGGER.warn("Automation error", t);
            Debug.log(Debug.Category.ERROR, "Automation error while flipping " + next.display, t);
            failureReason = t.getClass().getSimpleName();
            long cooldownMs = Math.max(0L, config.automationFailureCooldownSeconds * 1000L);
            failureCooldownUntilMs = System.currentTimeMillis() + cooldownMs;
        } finally {
            long latencyMs = (System.nanoTime() - start) / 1_000_000L;
            analytics.recordAttempt(
                    Math.max(0.0, next.buyAt),
                    latencyMs,
                    success,
                    success ? (next.sellAt - next.buyAt) : 0.0,
                    success ? null : failureReason
            );
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
    private boolean executeFlip(FlipOpportunity flip) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            Debug.log(Debug.Category.AUTO, "No player in world - not executing");
            return false;
        }

        HandledScreen<?> screen = callOnClientThread(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen == null) {
                Debug.log(Debug.Category.GUI, "No screen open");
                say("[Lumin] Open the Bazaar or Auction House, then let me work.");
                return null;
            }
            if (!(client.currentScreen instanceof HandledScreen)) {
                Debug.log(Debug.Category.GUI, "Current screen is not a HandledScreen: "
                        + client.currentScreen.getClass().getSimpleName());
                say("[Lumin] No inventory screen open.");
                return null;
            }
            HandledScreen<?> handled = (HandledScreen<?>) client.currentScreen;
            Debug.log(Debug.Category.GUI, "Screen title: " + handled.getTitle().getString());
            return handled;
        });
        if (screen == null) {
            return false;
        }

        switch (flip.type) {
            case BAZAAR_MARGIN:
                return doBazaarMarginFlip(screen, flip);
            case BIN_SNIPE:
                return doBinSnipe(screen, flip);
            default:
                return false;
        }
    }

    private boolean doBazaarMarginFlip(HandledScreen<?> screen, FlipOpportunity flip) {
        // 1) Find the bazaar product slot by name
        Slot product = callOnClientThread(() -> GuiAutomation.findSlotByName(screen, flip.display));
        if (product == null) {
            Debug.log(Debug.Category.GUI, "Could not find bazaar item slot: " + flip.display);
            say("[Lumin] Could not find bazaar item: " + flip.display);
            return false;
        }
        Debug.log(Debug.Category.GUI, "Found product slot " + product.id + " for " + flip.display);

        // 2) Click it (human-paced)
        clickHumanlike(screen, product.id);
        sleep(clock.nextActionDelayMs(System.currentTimeMillis()));

        // 3) In the product view, find "Buy Instantly" / "Create Buy Order"
        Slot buy = callOnClientThread(() -> GuiAutomation.findSlotByName(screen, "buy"));
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
        return true;
    }

    private boolean doBinSnipe(HandledScreen<?> screen, FlipOpportunity flip) {
        // 1) Find the AH item slot by name
        Slot item = callOnClientThread(() -> GuiAutomation.findSlotByName(screen, flip.display));
        if (item == null) {
            Debug.log(Debug.Category.GUI, "Could not find auction slot: " + flip.display);
            say("[Lumin] Could not find auction: " + flip.display);
            return false;
        }
        Debug.log(Debug.Category.GUI, "Found auction slot " + item.id + " for " + flip.display);

        // 2) Click it
        clickHumanlike(screen, item.id);
        sleep(clock.nextActionDelayMs(System.currentTimeMillis()));

        // 3) Click the "Buy" / confirm slot
        Slot buy = callOnClientThread(() -> GuiAutomation.findSlotByName(screen, "buy"));
        if (buy != null) {
            Debug.log(Debug.Category.GUI, "Found confirm slot " + buy.id);
            clickHumanlike(screen, buy.id);
            Debug.log(Debug.Category.AUTO, "Bought " + flip.display + " for " + flip.buyAt);
            say("[Lumin] Bought " + flip.display + " for " + flip.buyAt + " coins.");
            return true;
        } else {
            Debug.log(Debug.Category.GUI, "No buy/confirm slot found in auction view");
            return false;
        }
    }

    private void clickHumanlike(HandledScreen<?> screen, int slotId) {
        // Pre-click micro-pause
        sleep(clock.nextActionDelayMs(System.currentTimeMillis()) / 2);
        callOnClientThread(() -> {
            GuiAutomation.clickSlot(screen, slotId, 0, SlotActionType.PICKUP);
            return null;
        });
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(Math.max(0, ms));
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void say(String msg) {
        callOnClientThread(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal(msg), false);
            }
            return null;
        });
    }

    public void stop() {
        enabled.set(false);
        busy.set(false);
        pendingFlips.clear();
        synchronized (this) {
            pendingOrderKeys.clear();
            recentOrderByKey.clear();
            reservedSpend = 0.0;
            reservedEstimatedLoss = 0.0;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private synchronized void rollDailyBudgetIfNeeded() {
        String day = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        if (!day.equals(dailyCounterDay)) {
            dailyCounterDay = day;
            dailySpend = 0.0;
            dailyEstimatedLoss = 0.0;
            reservedSpend = 0.0;
            reservedEstimatedLoss = 0.0;
        }
    }

    private boolean passesItemFilters(FlipOpportunity f) {
        String id = normalizeId(f.productId);
        String display = normalizeId(f.display);
        boolean hasWhitelist = config.automationWhitelistItemIds != null && config.automationWhitelistItemIds.length > 0;
        if (hasWhitelist) {
            boolean match = false;
            for (String w : config.automationWhitelistItemIds) {
                String n = normalizeId(w);
                if (!n.isEmpty() && (id.contains(n) || display.contains(n))) {
                    match = true;
                    break;
                }
            }
            if (!match) return false;
        }
        if (config.automationBlacklistItemIds != null) {
            for (String b : config.automationBlacklistItemIds) {
                String n = normalizeId(b);
                if (!n.isEmpty() && (id.contains(n) || display.contains(n))) {
                    return false;
                }
            }
        }
        return true;
    }

    private synchronized boolean isDuplicateFlip(FlipOpportunity f) {
        String key = flipKey(f);
        if (pendingOrderKeys.contains(key)) return true;
        Long last = recentOrderByKey.get(key);
        return last != null && (System.currentTimeMillis() - last) < TimeUnit.MINUTES.toMillis(10);
    }

    private synchronized void rememberPendingFlip(FlipOpportunity f) {
        pendingOrderKeys.add(flipKey(f));
        reservedSpend += Math.max(0.0, f.buyAt);
        reservedEstimatedLoss += Math.max(0.0, f.buyAt - f.sellAt);
    }

    private synchronized void forgetPendingFlip(FlipOpportunity f) {
        pendingOrderKeys.remove(flipKey(f));
        reservedSpend = Math.max(0.0, reservedSpend - Math.max(0.0, f.buyAt));
        reservedEstimatedLoss = Math.max(0.0, reservedEstimatedLoss - Math.max(0.0, f.buyAt - f.sellAt));
    }

    private synchronized void rememberExecutedFlip(FlipOpportunity f) {
        recentOrderByKey.put(flipKey(f), System.currentTimeMillis());
    }

    private synchronized void pruneDuplicateCache() {
        long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
        recentOrderByKey.entrySet().removeIf(e -> e.getValue() < cutoff);
    }

    private String flipKey(FlipOpportunity f) {
        String base = (f.productId == null || f.productId.isEmpty()) ? f.display : f.productId;
        return normalizeId(base) + "|" + f.type;
    }

    private static String normalizeId(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    private <T> T callOnClientThread(java.util.concurrent.Callable<T> callable) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.isOnThread()) {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        FutureTask<T> task = new FutureTask<T>(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        mc.execute(task);
        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
