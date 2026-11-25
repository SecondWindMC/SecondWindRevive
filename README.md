# SecondWind

Downed-but-not-out (DBNO) revive system for Paper servers. Lethal damage puts players into a crawl + bleed-out state; teammates revive them, enemies finish them.

**Download:** see [Releases](../../releases) — or on [Hangar](https://hangar.papermc.io/SecondWindMC/SecondWindRevive).
**Bundle / support:** https://secondwindmc.github.io — $5.99 standalone or $8.99 with GraveGuard.
**Support:** secondwindmc@proton.me

## Install
1. Drop `SecondWind-1.1.0.jar` into `plugins/`
2. Restart (or `/reload` not needed — just start the server)
3. Optional: edit `plugins/SecondWind/config.yml`, then `/sw reload`

## Requirements
- Paper 1.21.x, Java 21
- No dependencies

## How it plays
- Downed players crawl (15% speed), can't fight or use items, and bleed out after a timer (default 60s).
- A teammate holds **sneak** within 3 blocks for ~8s to revive them at 3 hearts + brief invulnerability.
- An enemy does the same to execute them.
- Logging out while downed = death (configurable).

## Commands (`/sw` or `/secondwind`)
| Command | Permission | What it does |
|---|---|---|
| `/sw reload` | secondwind.admin | Reload config + messages |
| `/sw status [player]` | secondwind.admin | Show a player's downed state |
| `/sw down <player>` | secondwind.admin | Force-down a player |
| `/sw revive <player>` | secondwind.admin | Revive a downed player |
| `/sw execute <player>` | secondwind.admin | Kill a downed player |

Permission `secondwind.bypass` — never goes downed (default: op).

## Config highlights
`bleed-out-seconds`, `revive-seconds`, `execute-seconds`, `interact-radius`, `movement-multiplier`, `revived-health`, `post-revive-invulnerable-seconds`, `revivers: ANYONE|TEAM`, `executors: ENEMY|ANYONE|NONE`, `exempt-causes`, `disabled-worlds`, `blocked-commands`.

All player-facing text lives in `messages.yml` (legacy `&` colors, `{placeholders}`).

## Validation performed

Validated on Paper 1.21.11 build 132 with Java 21 using automated offline-mode Mineflayer clients:

- lethal player damage put the victim into the downed state at 4 health instead of causing immediate death;
- the victim remained in place instead of respawning;
- a nearby sneaking teammate completed the revive channel;
- revived health matched the configured value of 6.

Additional restart validation for v1.1.0:

- forced a player into the downed state, waited for periodic persistence, then stopped Paper with `kill -9`;
- after restart and rejoin, the same player was still downed with the bleed-out timer running (46 seconds remained in the test);
- admin revive cleared the persisted `downed.yml` state and restored normal revived health.

## Build from source
```
mvn package   # JDK 21
```
