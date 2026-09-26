# Paper 26.2 plugin branch

This branch ports the original datapack to a standalone Paper **26.2** plugin. The old datapack files remain in the repository as a reference, but the plugin does not load or require them.

## What changed

- No fixed world coordinates.
- Multiple independent arenas can exist in the same Minecraft world.
- Each arena stores its own two cuboid corners, red and blue bases, spectator point, flag points, required flag count and win countdown.
- Setup uses placeable marker items rather than editing coordinates in functions.
- Flag points can be either 1x1 or 2x2, so the original map's four-block flag pads can be represented without hard-coding them.
- Arena configuration is saved to `plugins/CaptureTheFlag/arenas.yml`.
- Team building blocks can be placed anywhere inside an arena except in a vertical column above a beacon/capture point; breakable materials remain configurable in `config.yml`.
- Ability items remain visible during cooldown and use Minecraft's normal item cooldown overlay instead of disappearing/replacing inventory contents.
- Hunter Leaf Walk removes old leaves immediately, follows normal jumps upward, and drops downward while sneaking.
- Red/blue timers, bossbars and flag counts are per arena, rather than global scoreboards.
- Team membership is per arena, so two games in one world do not share scores or teams.
- Temporary Hunter leaves are reference-counted per block, so overlapping Leaf Walk users cannot delete each other's platform.
- Swimmer water is ownership-tracked as it flows and only ability-created water is removed after 5.5 seconds; existing map water is left alone.
- Abilities that depend on facing use the player's full 3D look direction, including up/down.
- Each game captures a pre-game block snapshot of the arena. Stop, finish and plugin shutdown restore it automatically.
- Active-match snapshots are also written to `plugins/CaptureTheFlag/snapshots/`, so an interrupted server process is recovered on the next startup. Container contents are included.
- Necromancer mobs are tagged to an arena/team and respect friendly-fire rules.
- Disconnecting no longer removes a player's arena/team/class session; reconnecting during the same server run restores them to the current match state without resetting long cooldowns.
- The post-game results phase shows the winner, final flag totals and each player's kills/deaths before restoring the arena and returning players to their team base.
- Class kits now mirror the datapack's gameplay-relevant item details more closely, including armour trims, the Assassin dagger model/attack speed, Necromancer wand knockback, Swimmer trident attributes and custom Assassin/Necromancer food values.

## Build

Paper 26.2 requires Java 25.

```bash
mvn package
```

The JAR is written to `target/CaptureTheFlag-2.0.0-SNAPSHOT.jar`.

## Install

1. Build the JAR or download the GitHub Actions artifact.
2. Put the JAR in the Paper server's `plugins/` directory.
3. Remove/disable the old CTF datapack for worlds where this plugin will run.
4. Start the server.

## Arena setup

Create an arena in the world you are standing in:

```text
/ctf arena create <name>
/ctf arena markers <name>
```

The marker command gives seven setup items:

- Corner A
- Corner B
- Red Base
- Blue Base
- Spectator
- Flag 1x1
- Flag 2x2

Place a marker item where the point belongs. The marker block itself is cancelled; the plugin stores the location and shows a floating labelled setup marker. You can place as many flag markers as you need.

Useful setup commands:

```text
/ctf arena info <name>
/ctf arena showmarkers <name>
/ctf arena hidemarkers <name>
/ctf arena removeflag <name>
/ctf arena clearflags <name>
/ctf arena setrequired <name> <flags>
/ctf arena setwinseconds <name> <seconds>
/ctf arena setneutral <name> <material>
```

`removeflag` removes the nearest flag point within eight blocks.

## Playing

```text
/ctf join <arena> <red|blue>
/ctf class <class>
/ctf start <arena>
```

Classes:

- assassin
- chef
- hunter
- mace_bearer
- necromancer
- pro
- swimmer
- vampire

Players default to `pro` if they do not choose a class.

Stop a game with:

```text
/ctf stop <arena>
```

Stopping or finishing a game automatically restores the arena to the block state captured immediately before that game started. To force the last snapshot to be applied again:

```text
/ctf reset <arena>
```

## Ability behaviour

Ability cooldowns keep their item in the inventory and show the normal Minecraft cooldown sweep. Ability lore shows `Ready` or the remaining cooldown, and trying to use an ability early gives a brief actionbar timer. If hotbar slot 9 is occupied when an ability becomes available, the plugin uses another free inventory slot instead of overwriting the existing item.

Leaf Walk lasts 5 seconds, matching the datapack. Its current 3x3 leaf platform is the only temporary platform kept: leaves behind the player disappear on the next tick. Jumping raises the platform as the player rises; holding sneak removes the platform beneath the player so they can descend, and releasing sneak recreates it at the new height. Overlapping Hunter platforms share ownership safely.

The Swimmer's temporary water remains for 5.5 seconds. Flowing blocks inherit ownership from the ability source, and cleanup restores only blocks changed by that ability rather than scanning and deleting all nearby water.

## Flag behaviour

At game start every configured flag cell is reset to the arena's neutral flag material (glass by default). A flag counts as red or blue only when every cell in that flag point is the team's stained glass. When a team controls at least `required-flags`, its countdown decreases once per second. The first countdown to reach zero wins.

## Permissions

- `ctf.play` — join, leave and choose a class; granted by default.
- `ctf.admin` — arena setup/start/stop/reset/reload; op by default.
