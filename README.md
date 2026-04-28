# 1MB Boosters

`Boosters` is a helper plugin for 1MoreBlock.com that tracks server-wide boosters from [mcMMO](https://github.com/mcMMO-Dev/mcMMO), [Jobs Reborn](https://github.com/Zrips/Jobs), and an experimental points integration for `PyroWelcomesPro`, restores tracked boosters after restart, and gives players a clean `/rate` command to check the current status.

Version: `1.3.0`  
Build: `042`  
Updated: `2026-04-28`

## What it does

- Tracks native mcMMO `/xprate` commands from players and console.
- Tracks native Jobs `/jobs boost ...` commands from players and console.
- Restores tracked boosters a few seconds after startup.
- Adds `/rate`, `/rate info`, `/rate start`, `/rate stop`, `/rate reload`, and `/rate debug`.
- Supports an experimental hidden/admin-visible `points` booster for `PyroWelcomesPro`.
- Exposes PlaceholderAPI placeholders for mcMMO, Jobs, and points.
- Uses MiniMessage for plugin output and configurable broadcast messages.
- Supports configurable console command hooks for booster start and stop events.
- Loads player-facing wording from `Locale_EN.yml`.

## Server target

- Built with Java `25`.
- Compiled against the Paper API for `26.1.2`.
- The generated `plugin.yml` declares `api-version: 1.21.11` so the same jar can be tested on Paper `1.21.11` and Paper `26.1.2`.
- Intended for Paper `1.21.11` and newer Paper `26.x` servers that are running on Java `25`.
- The built jar is named `1MB-Boosters-v1.3.0-042-j25-26.1.2.jar`.
- The plugin data folder is `plugins/1MB-Boosters/`.
- Repo-local `/servers/` is not used by this project; testing should go through `/Users/floris/Projects/Codex/servers/run-test-server`.

If you run this exact build on a server that still uses Java `21`, the plugin will not load because the bytecode target is Java `25`.

## Supported integrations

### mcMMO

Native mcMMO commands that Boosters tracks:

- `/xprate <rate> <true|false>`
- `/xprate reset`

Boosters can also manage timed mcMMO boosters itself with `/rate start mcmmo ...`, even though native mcMMO `/xprate` does not include a timer.

### Jobs Reborn

Native Jobs commands that Boosters tracks:

- `/jobs boost <job|all> <exp|money|points|all> <time> <rate>`
- `/jobs boost all reset`

Boosters stores the remaining time and restores the Jobs booster after restart.

### PyroWelcomesPro points

Boosters includes an experimental points integration for `PyroWelcomesPro`.

- It edits `plugins/PyroWelcomesPro/config.yml`
- It updates:
  - `Settings.EarnablePoints`
  - `Settings.DiscordSRV.EarnablePoints`
- It runs the configured reload command after saving
- It tracks the multiplier and time like the other booster types

Current assumptions:

- Base in-game points: `2`
- Base Discord points: `1`
- Points multipliers must be whole numbers like `2`, `3`, or `4`

By default, points can be hidden from normal players while still being visible to admins in `/rate`, `/rate debug`, and direct admin commands like `/rate start points ...`.
When `features.points.visible: false`, `points` is also excluded from `/rate start all`, `/rate stop all`, and public PlaceholderAPI output.

## Commands

### Player command

- `/rate`
  Shows the current tracked booster status. Experimental points can stay hidden from normal players.
  Players with only `onemb.boosters.rate` do not get admin or debug tab completions.
- `/rate info`
  Shows a short introduction to the plugin, the current build information, and a clickable GitHub repository URL.

### Admin commands

- `/rate start <mcmmo|jobs|points|all> <time> <rate>`
  Starts tracked boosters. `all` starts every enabled/admin-available booster using the same time and rate.
- `/rate stop <mcmmo|jobs|points|all>`
  Stops one tracked booster or all tracked boosters.
- `/rate reload`
  Reloads this plugin's `config.yml` and locale file.
- `/rate debug`
  Shows a summary debug view with build/runtime information, including the compile Paper API version and declared compatibility floor.
- `/rate debug reference`
  Shows commands, permissions, and placeholders.
- `/rate debug integrations`
  Shows integration status for mcMMO, Jobs, and points.
- `/rate debug state`
  Shows the tracked runtime state.
- `/rate debug raw`
  Shows raw stored values and important file paths.
- `/rate debug config`
  Shows an expanded grouped config summary.
- `/rate debug config <path>`
  Reads a config value.
- `/rate debug config <path> <value>`
  Sets a simple config value and reloads this plugin.
- `/rate debug toggle <path> [true|false]`
  Flips a boolean true/false config setting, or explicitly sets it if you provide `true` or `false`, then reloads this plugin.
- `/rate debug all`
  Console-only full debug dump with summary, reference, integrations, state, raw, config, and logs.
- `/rate debug logs`
  Shows recent audit log entries.
- `/rate debug clean logs`
  Clears the recent audit log entries.
- `/rate debug cleanlogs`
  Alias for clearing the recent audit log entries.

If `/rate start` or `/rate stop` is used without enough arguments, the plugin shows the correct command synopsis.

## Command examples

- `/rate`
- `/rate info`
- `/rate start mcmmo 1h 2`
- `/rate start jobs 30m 2`
- `/rate start points 1h 2`
- `/rate start all 1h 2`
- `/rate stop points`
- `/rate stop all`
- `/rate reload`
- `/rate debug`
- `/rate debug reference`
- `/rate debug raw`
- `/rate debug all`
- `/rate debug config features.points.visible true`
- `/rate debug toggle features.points.visible`
- `/rate debug toggle features.points.visible true`
- `/rate debug logs`
- `/rate debug clean logs`

## Permissions

- `onemb.boosters.rate`
  Allows players to use `/rate`.
- `onemb.boosters.admin`
  Allows staff to use `/rate start`, `/rate stop`, and `/rate reload`.
- `onemb.boosters.debug`
  Allows staff to use `/rate debug`.

### LuckPerms examples

- `lp group admin permission set onemb.boosters.admin true`
- `lp group admin permission set onemb.boosters.debug true`
- `lp group default permission set onemb.boosters.rate true`

## PlaceholderAPI

Boosters registers the PlaceholderAPI identifier `onembboosters`.

### mcMMO placeholders

- `%onembboosters_mcmmo_active%`
  Returns `Yes` or `No`.
- `%onembboosters_mcmmo_rate%`
  Returns the tracked mcMMO rate without the `x`.
- `%onembboosters_mcmmo_time%`
  Returns the original tracked mcMMO duration, or `Manual` for a native `/xprate`.
- `%onembboosters_mcmmo_timeleft%`
  Returns the remaining tracked mcMMO duration, or `Manual` for a native `/xprate`.

### Jobs placeholders

- `%onembboosters_jobs_active%`
  Returns `Yes` or `No`.
- `%onembboosters_jobs_rate%`
  Returns the tracked Jobs rate without the `x`.
- `%onembboosters_jobs_time%`
  Returns the original tracked Jobs duration.
- `%onembboosters_jobs_timeleft%`
  Returns the remaining tracked Jobs duration.

### Points placeholders

- `%onembboosters_points_active%`
  Returns `Yes` or `No`.
- `%onembboosters_points_rate%`
  Returns the tracked points multiplier, or `1` while points are hidden.
- `%onembboosters_points_time%`
  Returns the original tracked points duration, or `None` while points are hidden.
- `%onembboosters_points_timeleft%`
  Returns the remaining tracked points duration, or `None` while points are hidden.

When a booster is not active:

- `rate` returns `1`
- `time` returns `None`
- `timeleft` returns `None`

## Config and locale

- `config.yml`
  Structural settings, feature toggles, tab completions, display options, integration paths, broadcasts, and lifecycle command hooks.
- `Locale_EN.yml`
  Player-facing wording and MiniMessage styling for `/rate` output.
- `booster-state.yml`
  Automatically managed runtime state for tracked boosters.

### Command hooks

You can run your own console commands whenever a tracked booster starts or stops.

- Hook lists live in `config.yml` under `commandHooks.start.*` and `commandHooks.stop.*`
- `global` runs for every booster type
- `mcmmo`, `jobs`, and `points` only run for that booster type
- Hooks also run for tracked native mcMMO/Jobs commands and when a timed booster expires
- Commands are sent to the console raw, so the target plugin can handle its own formatting, MiniMessage, hex colors, sounds, bossbars, particles, and so on
- Leading `/` is optional

Available hook placeholders:

- The common ones you will probably use most are `{booster}`, `{rate}`, `{duration}`, `{remaining}`, `{sender}`, `{jobs_target}`, `{jobs_scope}`, `{ingame}`, and `{discord}`.
- `{phase}` returns `start` or `stop`
- `{booster}` returns `mcMMO`, `Jobs`, or `Points`
- `{booster_key}` returns `mcmmo`, `jobs`, or `points`
- `{booster_label}` returns the configured display label from `config.yml`
- `{rate}` returns the tracked multiplier
- `{duration}` and `{duration_compact}` return the booster duration
- `{remaining}` and `{remaining_compact}` return the remaining time at the moment the hook runs
- `{sender}` returns the player name or `Console`
- `{jobs_target}` and `{jobs_scope}` return Jobs target/scope values
- `{ingame}` and `{discord}` return the current points values for the points integration

Example hook config:

```yml
commandHooks:
  start:
    global:
      - "cmi broadcast Booster {booster} {rate}x started for {duration} by {sender}"
      - "cmi titlemsg all Booster Started \n {booster} {rate}x for {duration}"
  stop:
    global:
      - "cmi broadcast Booster {booster} stopped by {sender}"
      - "cmi titlemsg all Booster Stopped \n {booster} ended after {duration}"
```

The expected folder layout is:

- `plugins/1MB-Boosters/config.yml`
- `plugins/1MB-Boosters/Locale_EN.yml`
- `plugins/1MB-Boosters/booster-state.yml`

If you upgrade from an older release that used `plugins/Boosters/` or `plugins/boosters/`, this build attempts to migrate that data folder on startup.

## Build notes

- Gradle targets Java `25`.
- The plugin is compiled against `io.papermc.paper:paper-api:26.1.2.build.+`.
- The generated `plugin.yml` declares `api-version: 1.21.11`.
- Current release metadata: version `1.3.0`, build `042`.
- PlaceholderAPI support is optional.

## Credits

- [nossr50](https://github.com/nossr50) and the mcMMO team for [mcMMO](https://github.com/mcMMO-Dev/mcMMO)
- [Zrips](https://github.com/Zrips) for [Jobs Reborn](https://github.com/Zrips/Jobs)
- [xsmeths](https://github.com/xsmeths) for logic fixes and helpful review on booster output behavior
- [The456gamer](https://github.com/the456gamer) for additional contributions and improvements
- Everyone who helped shape the original idea and later fixes for this plugin
