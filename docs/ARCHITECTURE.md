# LuminClient Runtime Architecture

## System boundary

LuminClient is a client-only Fabric mod. It reads public Hypixel market endpoints, computes candidate flips, renders them in Minecraft, and optionally performs limited GUI interaction. It does not provide a server component or a persistent remote service.

Automation is disabled by default. Hypixel rules may prohibit automated clicking, trading, buying, or selling; changes to automation must preserve the explicit warning, default-off behavior, and spend/loss safeguards.

## Startup and shutdown

`LuminClient.onInitializeClient()` is the composition root:

1. Construct and load `LuminConfig`.
2. Initialize `Debug`.
3. Construct `SessionAnalytics`, `FlipEngine`, `GuiManager`, and `AutomationEngine`.
4. Register keybinds, both command adapters, and notifications.
5. Start the flip scheduler and automation scheduler.
6. Register the Fabric client-stopping callback.

Shutdown stops automation first and then the flip engine. New services should be owned and stopped from this lifecycle rather than creating unmanaged executors.

## Data flow

```text
Hypixel HTTP API
        │
        ▼
HypixelApi ──► Models ──► Flip strategies
                              │
                              ▼
                         FlipEngine
                  (latest snapshot + listeners)
                    ┌─────────┼──────────┐
                    ▼         ▼          ▼
              FlipsScreen  Notifier  AutomationEngine
                                      │
                                      ▼
                              Minecraft client thread
```

### Market scanning

`FlipEngine` owns a single daemon scheduled executor. It periodically calls `HypixelApi`, parses JSON through `Models` into immutable `market` records, runs enabled strategies, replaces the latest immutable snapshot, and notifies listeners. API failures are logged and do not replace the last successful snapshot.

`HypixelApi` is the only HTTP boundary. Keep retries, `Retry-After`, backoff, timeouts, response validation, and API-key header behavior there. Strategies should receive parsed domain data or an API abstraction, not open connections themselves.

### Strategy outputs

Both strategies produce `FlipOpportunity`:

- `BazaarMarginStrategy` evaluates executable Bazaar `buy_summary`/`sell_summary` levels with weighted acquisition/liquidation prices, a 1% rounded-up liquidation fee, slippage, freshness, minimum margin/profit, budget, and liquidity. `quick_status` prices are references, not executable bids or asks.
- `BinSnipeStrategy` evaluates live BIN auctions against a robust reference price and configured percentage threshold.

The GUI and automation layers consume the common opportunity type. A new strategy should not add strategy-specific branches outside the flip package unless execution genuinely differs.

## Threading model

| Thread/context | Responsibilities | Rules |
| --- | --- | --- |
| Minecraft client thread | Screens, player messages, inventory clicks, keybind callbacks | All Minecraft state and GUI mutation must happen here |
| Flip executor | HTTP calls, parsing, strategy calculations, listener dispatch | Do not access `MinecraftClient` state directly |
| Automation executor | Queue selection, pacing, budgets, analytics bookkeeping | Use `callOnClientThread` for screen inspection/clicks |
| Command callback | Parse command input and request actions | Keep output in `CommandActions`; avoid blocking network work |

`AutomationEngine` bridges its executor to the client thread with a `FutureTask`. Be careful with deadlocks: code running on the client thread must not wait for the automation executor, and long sleeps/network calls must not run on the client thread.

Shared snapshots use copy-on-write collections and are exposed as defensive/unmodifiable lists. Preserve that property when changing publication or caching.

## Automation state machine

`AutomationEngine` is a `FlipEngine.Listener`. Its high-level state is:

1. **Disabled**: no queueing or execution.
2. **Enabled and waiting**: accepts opportunities that pass item filters, per-order cap, daily spend/loss reservations, duplicate suppression, and per-scan count.
3. **Paced**: `HumanClock` determines the next action delay and may enter fatigue or a mandatory break.
4. **Executing**: inspect the current `HandledScreen`, identify a slot defensively, and click through the relevant flow.
5. **Recorded**: update daily counters, duplicate cache, and `SessionAnalytics`; on failure, apply the configured cooldown.

Current Bazaar execution is assistive at the sign/anvil amount-entry boundary: it stages the order and asks the player to confirm. GUI recognition is intentionally best-effort because Hypixel menus can change.

Safety checks are layered rather than interchangeable:

- `automationEnabled` is persisted and defaults to false.
- Runtime enablement is separately controlled by `AutomationEngine`.
- Per-order spend and per-scan order limits cap individual bursts.
- Daily spend and estimated-loss reservations prevent queued work from oversubscribing limits.
- Whitelist/blacklist filters restrict item scope.
- Duplicate suppression prevents repeated orders for the same item/type within the cooldown window.
- Failure cooldown and mandatory breaks reduce repeated action loops.

## Commands and control surfaces

`CommandActions` is the shared command service. It returns chat-ready lines and owns behavior for:

- help, GUI opening, refresh, and top flips;
- automation on/off;
- `minmargin`, `budget`, and `maxspend` settings;
- debug mode/chat/tail;
- session statistics.

`LuminCommand` exposes the Brigadier `/lumin` command. `ChatCommandHandler` exposes the dot-prefixed `.lumin` form by intercepting matching chat messages. Keep the two adapters thin and update both when adding a command. Keybinds call managers directly for GUI and automation toggling.

## Configuration and persistence

`LuminConfig` loads from Fabric's config directory, applies field-level defaults, validates/clamps values, and writes a versioned JSON object. It currently uses config version 1. The optional Hypixel API key is user data; never log its value or include it in tests, examples, or diagnostics.

`SessionAnalytics` stores daily UTC counters in `luminclient-stats.json`. It rolls counters at the UTC date boundary and records attempts, success/failure counts, estimated PnL, spend, latency, and categorized failure reasons. Analytics must remain observational: it must not determine whether an order is allowed.

## Diagnostics

Use `Debug.Category` consistently:

- `API`: requests, status, latency, and response sizes.
- `SCAN`: scan lifecycle and result counts.
- `FLIP`: strategy decisions.
- `AUTO`: queueing, pacing, and execution decisions.
- `GUI`: screens, slots, and clicks.
- `CONFIG`: load/save behavior.
- `ERROR`: failures that require attention.

Errors should be logged even when verbose debug mode is off. Avoid broad catches that hide failures or convert an error into a success-shaped result.

## Extension and change guidance

1. Read the owning package and its tests before changing behavior.
2. Keep HTTP, parsing, strategy, presentation, and execution responsibilities separated.
3. Preserve client-thread boundaries and defensive collection exposure.
4. Add or update focused JUnit tests for pure parsing, strategy, config, or analytics behavior.
5. Run `./gradlew --no-daemon clean test build`.
6. Update `README.MD` when user-visible commands, configuration, installation, or safety behavior changes.

There are no tests that safely exercise a live Minecraft GUI or Hypixel network. Those paths require manual client testing and careful logging; do not make network calls from unit tests.
