# 1MB Boosters

`Boosters` is a helper plugin for 1MoreBlock.com that tracks server-wide boosters from [mcMMO](https://github.com/mcMMO-Dev/mcMMO) and [Jobs Reborn](https://github.com/Zrips/Jobs), restores them after a restart, and gives players a clean `/rate` command to check the current status.

Version: `1.2.1`  
Build: `023`  
Updated: `2026-04-19`

## What it does

- Tracks native mcMMO `/xprate` commands from players and console.
- Tracks native Jobs `/jobs boost ...` commands from players and console.
- Restores tracked boosters a few seconds after startup.
- Adds `/rate` for friendly player-facing status output.
- Adds `/rate start ...` and `/rate stop ...` so staff can run timed boosters from one command.
- Adds `/rate debug` for quick diagnostics about versions, permissions, dependencies, and tracked state.
- Exposes PlaceholderAPI placeholders for holograms, scoreboards, and chat.
- Uses MiniMessage components for this plugin's own console and in-game messages.

## Server target

- Built with Java `25`.
- Compiled against the Paper API for `1.21.11`.
- Intended for Paper `1.21.11` and newer Paper `26.x` servers that are running on Java `25`.
- The built jar is named `1MB-Boosters-v1.2.1-023-j25-1.21.11.jar`.
- The plugin data folder is `plugins/1MB-Boosters/`.

If you run this exact build on a server that still uses Java `21`, the plugin will not load because the bytecode target is Java `25`.

## Supported booster flows

### mcMMO

Native mcMMO commands that Boosters tracks:

- `/xprate <rate> <true|false>`
- `/xprate reset`

Example:

- `/xprate 2 true`

mcMMO does not provide its own timer for `/xprate`, so Boosters can only track a native `/xprate` command as a manual booster with no known end time.

If you want Boosters to manage the timer for mcMMO, use:

- `/rate start mcmmo <time> <rate>`

That starts the mcMMO booster now and lets Boosters stop it later with `/xprate reset`.

### Jobs Reborn

Native Jobs commands that Boosters tracks:

- `/jobs boost <job|all> <exp|money|points|all> <time> <rate>`
- `/jobs boost all reset`

Examples:

- `/jobs boost all all 1h 2`
- `/jobs boost Miner exp 30m 2`
- `/jobs boost all reset`

Jobs already has a timer, so Boosters stores the remaining time and reapplies the booster after restart with the correct time left.

The built-in admin shortcut uses a global Jobs booster:

- `/rate start jobs <time> <rate>`

Internally, that runs `/jobs boost all all <time> <rate>`.

## Commands

### Player command

- `/rate`
  Shows the current tracked booster status for mcMMO and Jobs.

### Admin commands

- `/rate start <mcmmo|jobs> <time> <rate>`
  Starts a tracked booster. For mcMMO this creates a timed booster managed by Boosters. For Jobs this starts a global all/all booster.
- `/rate stop <mcmmo|jobs|all>`
  Stops one tracked booster or both.
- `/rate debug`
  Shows plugin version, build number, target versions, commands, permissions, dependency status, and tracked booster state details.

If `/rate start` or `/rate stop` is used without enough arguments, the plugin shows the correct command synopsis.

## Command examples

- `/rate`
- `/rate start mcmmo 1h 2`
- `/rate start mcmmo 2h30m 3`
- `/rate start jobs 30m 2`
- `/rate start jobs 1h 2.5`
- `/rate stop mcmmo`
- `/rate stop jobs`
- `/rate stop all`
- `/rate debug`

## Permissions

- `onemb.booster.rate`
  Allows players to use `/rate`.
- `onemb.booster.admin`
  Allows staff to use `/rate start ...` and `/rate stop ...`.
- `onemb.booster.debug`
  Allows staff to use `/rate debug`.
- `boosters.rate`
  Legacy compatibility node for older permission setups.
- `boosters.admin`
  Legacy compatibility node for older permission setups.
- `boosters.debug`
  Legacy compatibility node for older permission setups.

## PlaceholderAPI

Boosters registers the PlaceholderAPI identifier:

- `%onemb_boosters_mcmmo_active%`
- `%onemb_boosters_mcmmo_rate%`
- `%onemb_boosters_mcmmo_time%`
- `%onemb_boosters_mcmmo_timeleft%`
- `%onemb_boosters_jobs_active%`
- `%onemb_boosters_jobs_rate%`
- `%onemb_boosters_jobs_time%`
- `%onemb_boosters_jobs_timeleft%`

Returned values:

- `active` returns `Yes` or `No`
- `rate` returns the multiplier without the `x`, for example `2` or `2.5`
- `time` returns the original tracked duration, for example `2h`
- `timeleft` returns the remaining tracked duration, for example `1h 3m`

When a booster is not active:

- `rate` returns `1`
- `time` returns `None`
- `timeleft` returns `None`

When mcMMO was started directly with `/xprate` and no timer is known:

- `time` returns `Manual`
- `timeleft` returns `Manual`

## Files and storage

- `config.yml`
  Settings such as restore delay and mcMMO announcement behavior.
- `booster-state.yml`
  Automatically managed runtime state for tracked boosters.

The expected folder layout is:

- `plugins/1MB-Boosters/config.yml`
- `plugins/1MB-Boosters/booster-state.yml`

If you upgrade from an older release that used `plugins/Boosters/` or `plugins/boosters/`, this build attempts to migrate that data folder on startup.

## Build notes

- Gradle targets Java `25`.
- The plugin is compiled against `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT`.
- Current release metadata: version `1.2.1`, build `023`.
- PlaceholderAPI support is included as an optional dependency and is intended to work with newer PlaceholderAPI builds, including the `2.12.3-DEV-265` line you referenced for your server.

## Credits

- [nossr50](https://github.com/nossr50) and the mcMMO team for [mcMMO](https://github.com/mcMMO-Dev/mcMMO)
- [Zrips](https://github.com/Zrips) for [Jobs Reborn](https://github.com/Zrips/Jobs)
- Everyone who helped shape the original idea and later fixes for this plugin
