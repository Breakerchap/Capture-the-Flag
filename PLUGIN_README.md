# Paper 26.2 plugin branch

This branch ports the original datapack to a standalone Paper **26.2** plugin. The old datapack files remain in the repository as a reference, but the plugin does not load or require them.

## What changed

- No fixed world coordinates.
- Multiple independent arenas can exist in the same Minecraft world.
- Each arena stores its own two cuboid corners, red and blue bases, spectator point, flag points, required flag count and win countdown.
- Setup uses placeable marker items rather than editing coordinates in functions.
- Flag points can be either 1x1 or 2x2, so the original map's four-block flag pads can be represented without hard-coding them.
- Arena configuration is saved to `plugins/CaptureTheFlag/arenas.yml`.
- Build and break material lists are configurable in `config.yml`.
- Red/blue timers, bossbars and flag counts are per arena, rather than global scoreboards.
- Team membership is per arena, so two games in one world do not share scores or teams.
- Temporary Hunter leaves are tracked and cleaned up without deleting unrelated map leaves.
- Necromancer mobs are tagged to an arena/team and respect friendly-fire rules.

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

## Flag behaviour

At game start every configured flag cell is reset to the arena's neutral flag material (glass by default). A flag counts as red or blue only when every cell in that flag point is the team's stained glass. When a team controls at least `required-flags`, its countdown decreases once per second. The first countdown to reach zero wins.

## Permissions

- `ctf.play` — join, leave and choose a class; granted by default.
- `ctf.admin` — arena setup/start/stop/reload; op by default.
