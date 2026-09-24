package com.lumin.luminclient.flip;

import com.lumin.luminclient.LuminClient;
import com.lumin.luminclient.api.HypixelApi;
import com.lumin.luminclient.api.Models;
import com.lumin.luminclient.market.BazaarSnapshot;
import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.core.Debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodically polls the public Hypixel API and produces flip opportunities.
 * Runs fully off-thread; the game thread only reads the immutable results.
 */
public class FlipEngine {

    public interface Listener {
        void onNewFlips(List<FlipOpportunity> flips);
    }

    private final LuminConfig config;
    private final HypixelApi api;
    private final BazaarMarginStrategy bazaarStrategy = new BazaarMarginStrategy();
    private final BinSnipeStrategy binStrategy = new BinSnipeStrategy();

    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    private final List<FlipOpportunity> latestFlips = new CopyOnWriteArrayList<FlipOpportunity>();

    private ScheduledExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public FlipEngine(LuminConfig config) {
        this.config = config;
        this.api = new HypixelApi(config.apiKey);
    }

    public void start() {
        if (running.getAndSet(true)) return;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LuminClient-FlipEngine");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(this::scanOnceSafe,
                2, config.scanIntervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        running.set(false);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public List<FlipOpportunity> getLatestFlips() {
        return Collections.unmodifiableList(new ArrayList<FlipOpportunity>(latestFlips));
    }

    /** Trigger a scan immediately (async). */
    public void refreshNow() {
        if (executor != null) {
            executor.execute(this::scanOnceSafe);
        }
    }

    private void scanOnceSafe() {
        long startNs = System.nanoTime();
        try {
            Debug.log(Debug.Category.SCAN, "Starting market scan (bazaar=" + config.enableBazaarMarginFlips
                    + ", binsnipe=" + config.enableBinSnipeFlips + ")");
            List<FlipOpportunity> flips = new ArrayList<FlipOpportunity>();

            if (config.enableBazaarMarginFlips) {
                BazaarSnapshot snapshot = Models.parseBazaar(api.getBazaar());
                Debug.log(Debug.Category.SCAN, "Bazaar snapshot: " + snapshot.products().size() + " products");
                List<FlipOpportunity> bz = bazaarStrategy.findFlips(snapshot, config);
                Debug.log(Debug.Category.SCAN, "Bazaar margin flips found: " + bz.size());
                flips.addAll(bz);
            }

            if (config.enableBinSnipeFlips) {
                List<FlipOpportunity> bs = binStrategy.findFlips(api, config);
                Debug.log(Debug.Category.SCAN, "BIN snipe flips found: " + bs.size());
                flips.addAll(bs);
            }

            latestFlips.clear();
            latestFlips.addAll(flips);

            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
            Debug.log(Debug.Category.SCAN, "Scan complete in " + elapsedMs + "ms, total flips=" + flips.size());

            for (Listener l : listeners) {
                try {
                    l.onNewFlips(Collections.unmodifiableList(flips));
                } catch (Throwable t) {
                    LuminClient.LOGGER.warn("Flip listener error", t);
                    Debug.log(Debug.Category.ERROR, "Flip listener error", t);
                }
            }
        } catch (Exception e) {
            LuminClient.LOGGER.warn("Flip scan failed: {}", e.getMessage());
            Debug.log(Debug.Category.ERROR, "Flip scan failed: " + e.getMessage(), e);
        }
    }
}
