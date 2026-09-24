# LuminClient Project Structure

This document is a map of the repository for maintainers and coding agents. It describes where behavior lives, which files are safe starting points for a change, and how the project is built and tested.

## Repository tree

```text
.
├── .github/
│   └── workflows/
│       └── ci.yml                         # GitHub Actions: Java 21, clean test build
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/com/lumin/luminclient/
│   │   │   ├── LuminClient.java           # Fabric client entrypoint and dependency wiring
│   │   │   ├── api/
│   │   │   │   ├── HypixelApi.java         # HTTP client, retries, rate-limit handling
│   │   │   │   └── Models.java              # JSON-to-domain parsing
│   │   │   ├── auto/
│   │   │   │   ├── AutomationEngine.java   # Automation queue, budgets, GUI actions
│   │   │   │   ├── GuiAutomation.java      # Defensive inventory slot lookup/click helpers
│   │   │   │   └── HumanClock.java          # Action pacing, fatigue, and breaks
│   │   │   ├── config/
│   │   │   │   └── LuminConfig.java         # JSON config, defaults, migration, validation
│   │   │   ├── core/
│   │   │   │   ├── ChatCommandHandler.java # Dot-prefixed chat command adapter
│   │   │   │   ├── CommandActions.java     # Shared command behavior and output
│   │   │   │   ├── Debug.java              # File/console/chat diagnostics and tail buffer
│   │   │   │   ├── Keybinds.java            # K/J client keybinds
│   │   │   │   └── LuminCommand.java       # Brigadier `/lumin` command adapter
│   │   │   ├── flip/
│   │   │   │   ├── BazaarMarginStrategy.java
│   │   │   │   ├── BinSnipeStrategy.java
│   │   │   │   ├── FlipEngine.java          # Scheduled scans and listener publication
│   │   │   │   └── FlipOpportunity.java     # Immutable strategy output
│   │   │   ├── gui/
│   │   │   │   ├── FlipsScreen.java         # Paginated in-game flip screen
│   │   │   │   └── GuiManager.java          # Screen opening and HUD registration
│   │   │   ├── mixin/
│   │   │   │   ├── HandledScreenMixin.java  # Screen overlay hook
│   │   │   │   └── MinecraftClientMixin.java # Client lifecycle/input hooks
│   │   │   ├── notify/
│   │   │   │   └── Notifier.java             # New-flip chat and sound notifications
│   │   │   └── stats/
│   │   │       └── SessionAnalytics.java     # Daily persisted automation metrics
│   │   ├── resources/
│   │   │   ├── assets/luminclient/icon.png
│   │   │   ├── fabric.mod.json              # Fabric metadata and client entrypoint
│   │   │   └── luminclient.mixins.json      # Mixin declarations
│   │   └── test/java/com/lumin/luminclient/
│   │       ├── api/ModelsTest.java
│   │       ├── flip/BazaarMarginStrategyTest.java
│   │       ├── flip/BinSnipeStrategyTest.java
│   │       └── stats/SessionAnalyticsTest.java
├── build.gradle                              # Loom, Java 21, JUnit configuration
├── gradle.properties                         # Minecraft/Fabric/version coordinates
├── gradlew / gradlew.bat                     # Gradle wrapper entrypoints
├── README.MD                                 # User-facing setup and usage guide
└── docs/
    ├── PROJECT_STRUCTURE.md                  # This repository map
    └── ARCHITECTURE.md                       # Runtime flow and extension guidance
```

## Ownership map

| Concern | Primary owner | Related code |
| --- | --- | --- |
| Mod startup/shutdown | `LuminClient` | `Keybinds`, `LuminCommand`, `ChatCommandHandler`, `Notifier` |
| Market data | `HypixelApi`, `Models` | `FlipEngine`, strategy classes |
| Flip calculations | Strategy classes | `FlipOpportunity`, `FlipEngine` |
| Presentation | `FlipsScreen`, `GuiManager`, `Notifier` | `HandledScreenMixin` |
| Automation safety | `AutomationEngine`, `LuminConfig` | `HumanClock`, `SessionAnalytics` |
| Commands | `CommandActions` | Two adapters: Brigadier and chat |
| Diagnostics | `Debug` | All subsystems log by category |
| Persistence | `LuminConfig`, `SessionAnalytics` | Fabric config directory |

When adding a command, put behavior and response formatting in `CommandActions` first, then expose it through both command adapters. When adding a flip source, return `FlipOpportunity` values from a strategy and register it in `FlipEngine`; do not make the GUI or automation layer understand API response shapes.

## Build and validation

The project targets Java 21, Minecraft 1.21.4, Fabric Loader 0.16.9, Yarn `1.21.4+build.8`, and Fabric API `0.114.0+1.21.4`.

```bash
./gradlew --no-daemon clean test build
```

Use `./gradlew test` for focused Java tests and `./gradlew runClient` for a local client session. CI runs the full clean test/build command on every push and pull request.

## Runtime-generated files

These files are not repository inputs and must not be committed:

- `config/luminclient.json`: user settings and optional API key.
- `config/luminclient-stats.json`: UTC-day automation counters.
- `config/luminclient-debug.log`: rolling diagnostics output.
- `build/`: Gradle and Loom outputs.

The test fallback paths under `build/tmp/` are used when Fabric's runtime config directory is unavailable.
