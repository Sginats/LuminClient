# LuminClient

A Minecraft mod (Fabric, 1.21.x) that helps you flip items on Hypixel SkyBlock — and, if you enable it, does the clicking and paying for you.

It reads the **public Hypixel API** to find profitable flips and shows them in-game. With automation enabled, it drives the Bazaar / Auction House GUI for you with human-like pacing.

---

## ⚠️ Important: Hypixel rules

Automating gameplay (clicking, trading, buying/selling) on Hypixel **violates their rules** and can get your account **permanently banned**. This mod is provided for educational purposes. Use it at your own risk.

The "humanization" features (variable delays, thinking pauses, session breaks, fatigue) are designed to make the automation look more like a real player, but **there is no guarantee** it will avoid detection.

---

## Features

- **Bazaar margin flips**: scans `/v2/skyblock/bazaar` and finds items where the spread between best sell and best buy is profitable after tax.
- **BIN snipe flips**: scans live auctions for underpriced Buy-It-Now listings.
- **In-game GUI** (`.lumin gui` or keybind `K`) showing the top flips.
- **HUD overlay** showing how many flips are available.
- **Automation engine** (`.lumin auto on` or keybind `J`) that clicks the GUI, places orders, and pays.
- **Humanization**: reaction delays, thinking pauses, micro-jitter, fatigue, and mandatory session breaks.

---

## Minecraft & toolchain

- **Minecraft**: `1.21.4` (Hypixel SkyBlock runs on modern MC; 1.8.9 is no longer supported for SkyBlock)
- **Mod loader**: Fabric `0.16.9`
- **Mappings**: Yarn `1.21.4+build.8`
- **Fabric API**: `0.114.0+1.21.4`
- **Java**: 21+

---

## Building

### Prerequisites
- JDK 21
- Gradle (or use the wrapper)

### Build the mod

```bash
./gradlew build
```

The output JAR is in `build/libs/luminclient-1.0.0.jar`.

### Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft `1.21.4`.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) in your `mods` folder.
3. Put `luminclient-1.0.0.jar` in your `mods` folder.
4. Launch the game and join Hypixel.

### Run in a dev environment

```bash
./gradlew runClient
```

---

## Usage

| Command | What it does |
|---|---|
| `.lumin` | help |
| `.lumin gui` | open the flips GUI |
| `.lumin refresh` | force a market scan |
| `.lumin top` | print top flips in chat |
| `.lumin auto on\|off` | toggle automation |
| `.lumin set minmargin <pct>` | min bazaar margin % |
| `.lumin set budget <coins>` | total budget |
| `.lumin set maxspend <coins>` | max coins per automated order |
| `.lumin debug on\|off` | toggle verbose debug logging |
| `.lumin debug chat <on\|off>` | echo debug lines to chat |
| `.lumin debug tail` | show last 10 debug lines |
| `.lumin stats` | show daily automation analytics |

**Keybinds** (configurable in Controls):
- `K` — open the flips GUI
- `J` — toggle automation

---

## Automation & humanization

When automation is ON, the mod will:
1. Wait a **human-like delay** (log-normal reaction time + micro-jitter).
2. Occasionally add a **thinking pause** before acting.
3. Click the right slots in the Bazaar/AH GUI to stage a buy/sell.
4. Respect a **per-order spend cap** and a **max orders per scan** limit.
5. **Fatigue**: reaction time slowly increases the longer the session runs.
6. After a max session length, it takes a **mandatory break** (45–90 minutes) before resuming.

These are controlled in `config/luminclient.json`:

```json
{
  "configVersion": 1,
  "automationEnabled": false,
  "clickDelayMs": 250,
  "automationMaxSpendPerOrder": 250000,
  "automationMaxOrdersPerScan": 3,
  "automationMaxDailySpend": 5000000,
  "automationMaxDailyLoss": 500000,
  "automationFailureCooldownSeconds": 60,
  "automationPreventDuplicateOrders": true,
  "debugMode": false,
  "debugToChat": false
}
```

> The full humanization model lives in `HumanClock.java`. The defaults aim for believable pacing, but you can tune it in code.

---

## Debug mode

When something isn't working, turn on debug mode to see exactly what the mod is doing:

```bash
.lumin debug on          # enable verbose logging
.lumin debug chat on     # also echo debug lines to in-game chat
.lumin debug tail        # show the last 10 debug lines
.lumin debug off         # turn it off
```

Debug output is written to:
- the **game console** (via the mod logger)
- `config/luminclient-debug.log` (rolling file)
- **in-game chat** (if `debugToChat` is on)

What it logs:
- **API** — every HTTP request/response to Hypixel (URL, status, latency, body size)
- **SCAN** — market scan start/end, how many products/auctions were parsed, how many flips were found
- **FLIP** — flip detection decisions (which items matched, why they were included/excluded)
- **AUTO** — automation decisions (which flip it picked, human-like delays, break status)
- **GUI** — which screen is open, which slots were found/clicked, slot dumps
- **CONFIG** — config load/save
- **ERROR** — anything that failed (always logged, even if debug mode is off)

This is the first thing to enable if flips aren't showing up, automation isn't clicking, or you suspect an API problem.

---

## How the flip detection works

### Bazaar margin flip
Hypixel's bazaar has a **buy side** (people placing buy orders) and a **sell side** (people placing sell offers). If the best sell offer is higher than the best buy order, the difference is the margin. The mod computes:

```
net_profit = (best_buy - best_sell) - 1% bazaar tax
margin_%   = net_profit / best_sell
```

and surfaces items above your thresholds.

### BIN snipe
It scans the first few pages of live auctions, groups BINs by item, and flags any listing priced well below the next-cheapest BIN for the same item.

---

## API usage

Uses only **public** Hypixel endpoints (no API key required):

- `GET https://api.hypixel.net/v2/skyblock/bazaar`
- `GET https://api.hypixel.net/v2/skyblock/auctions?page=N`

If you add an API key in config, it will be sent as the `API-Key` header, but the current features don't need one.

---

## Project layout

```
src/main/java/com.lumin.luminclient/
├── LuminClient.java          # mod entrypoint
├── api/                      # Hypixel API client + models
├── config/                   # JSON config
├── core/                     # commands + keybinds + Debug
│   └── Debug.java            # debug/diagnostics logger
├── flip/                     # flip detection engine + strategies
├── gui/                      # screens + HUD
├── notify/                   # chat + sound alerts
└── auto/                     # automation engine + HumanClock + GUI helpers
    └── HumanClock.java       # human-behavior timing model
```

---

## Limitations & TODO

- **Sign GUI entry**: Hypixel uses a sign/anvil GUI to enter order amounts/prices. Fully automating that needs an additional mixin to type into the sign — currently the mod stages the order and asks you to confirm the amount.
- **GUI brittleness**: Hypixel's menus are custom and change often; slot detection is best-effort.
- **Build**: verified end-to-end — `./gradlew build` produces a working `build/libs/luminclient-1.0.0.jar`.

---

## License

MIT
