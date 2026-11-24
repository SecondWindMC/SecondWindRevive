# SecondWind

Downed-but-not-out (DBNO) revive system for Paper servers. Lethal damage puts players into a crawl + bleed-out state; teammates revive them, enemies finish them.

## Install
1. Drop `SecondWind-1.0.0.jar` into `plugins/`
2. Restart (or `/reload` not needed — just start the server)
3. Optional: edit `plugins/SecondWind/config.yml`, then `/sw reload`

## Requirements
- Paper 1.21.x, Java 21
- No dependencies

## How it plays
- Downed players crawl (15% speed), can't fight or use items, and bleed out after a timer (default 60s).
- A teammate holds **sneak** within 3 blocks for ~8s to revive them at 3 hearts + brief invulnerability.
- An enemy does the same to execute them.
- Logging out while downed = death.

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

## Build from source
```
mvn package   # JDK 21
```
