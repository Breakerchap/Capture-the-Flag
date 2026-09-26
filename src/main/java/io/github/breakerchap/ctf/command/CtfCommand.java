package io.github.breakerchap.ctf.command;

import io.github.breakerchap.ctf.CaptureTheFlagPlugin;
import io.github.breakerchap.ctf.arena.Arena;
import io.github.breakerchap.ctf.arena.ArenaManager;
import io.github.breakerchap.ctf.arena.ArenaState;
import io.github.breakerchap.ctf.game.GameManager;
import io.github.breakerchap.ctf.kit.KitManager;
import io.github.breakerchap.ctf.marker.MarkerManager;
import io.github.breakerchap.ctf.player.CtfClass;
import io.github.breakerchap.ctf.player.PlayerSession;
import io.github.breakerchap.ctf.player.TeamSide;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class CtfCommand implements TabExecutor {
  private final CaptureTheFlagPlugin plugin;
  private final ArenaManager arenas;
  private final MarkerManager markers;
  private final GameManager games;
  @SuppressWarnings("unused")
  private final KitManager kits;

  public CtfCommand(CaptureTheFlagPlugin plugin, ArenaManager arenas, MarkerManager markers, GameManager games, KitManager kits) {
    this.plugin = plugin;
    this.arenas = arenas;
    this.markers = markers;
    this.games = games;
    this.kits = kits;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
      help(sender);
      return true;
    }

    switch (args[0].toLowerCase(Locale.ROOT)) {
      case "arena" -> arenaCommand(sender, args);
      case "join" -> join(sender, args);
      case "leave" -> leave(sender);
      case "class" -> classCommand(sender, args);
      case "start" -> start(sender, args);
      case "stop" -> stop(sender, args);
      case "reset" -> reset(sender, args);
      case "reload" -> reload(sender);
      default -> help(sender);
    }
    return true;
  }

  private void arenaCommand(CommandSender sender, String[] args) {
    if (!admin(sender)) return;
    if (args.length < 2) {
      arenaHelp(sender);
      return;
    }
    String sub = args[1].toLowerCase(Locale.ROOT);
    switch (sub) {
      case "list" -> {
        if (arenas.all().isEmpty()) sender.sendMessage(Component.text("No arenas configured.", NamedTextColor.YELLOW));
        else sender.sendMessage(Component.text("Arenas: " + String.join(", ", arenas.names()), NamedTextColor.AQUA));
      }
      case "create" -> {
        if (!(sender instanceof Player player)) {
          sender.sendMessage(Component.text("Only a player can create an arena because the world is taken from your location.", NamedTextColor.RED));
          return;
        }
        if (args.length < 3) {
          sender.sendMessage(Component.text("Usage: /ctf arena create <name>", NamedTextColor.RED));
          return;
        }
        Arena created = arenas.create(args[2], player.getWorld());
        if (created == null) {
          sender.sendMessage(Component.text("An arena with that name already exists.", NamedTextColor.RED));
          return;
        }
        sender.sendMessage(Component.text("Created arena '" + created.name() + "' in world '" + created.worldName() + "'.", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Run /ctf arena markers " + created.name() + " to get the setup marker kit.", NamedTextColor.GRAY));
      }
      case "delete" -> {
        if (args.length < 3) {
          sender.sendMessage(Component.text("Usage: /ctf arena delete <name>", NamedTextColor.RED));
          return;
        }
        Arena arena = arenas.get(args[2]);
        if (arena != null) {
          games.stopGame(arena);
          markers.hideMarkers(arena);
        }
        boolean removed = arenas.delete(args[2]);
        sender.sendMessage(Component.text(removed ? "Arena deleted." : "Arena not found.", removed ? NamedTextColor.GREEN : NamedTextColor.RED));
      }
      case "markers" -> {
        if (!(sender instanceof Player player)) {
          sender.sendMessage(Component.text("Only players can receive marker items.", NamedTextColor.RED));
          return;
        }
        Arena arena = requireArena(sender, args, 2);
        if (arena == null) return;
        markers.giveMarkerKit(player, arena);
        markers.showMarkers(arena);
      }
      case "showmarkers" -> {
        Arena arena = requireArena(sender, args, 2);
        if (arena == null) return;
        markers.showMarkers(arena);
        sender.sendMessage(Component.text("Setup markers shown.", NamedTextColor.GREEN));
      }
      case "hidemarkers" -> {
        Arena arena = requireArena(sender, args, 2);
        if (arena == null) return;
        markers.hideMarkers(arena);
        sender.sendMessage(Component.text("Setup markers hidden.", NamedTextColor.GREEN));
      }
      case "info", "validate" -> {
        Arena arena = requireArena(sender, args, 2);
        if (arena == null) return;
        info(sender, arena);
      }
      case "clearflags" -> {
        Arena arena = requireArena(sender, args, 2);
        if (arena == null) return;
        arena.clearFlags();
        arenas.save();
        markers.showMarkers(arena);
        sender.sendMessage(Component.text("All flag points removed from " + arena.name() + ".", NamedTextColor.YELLOW));
      }
      case "removeflag" -> {
        if (!(sender instanceof Player player)) {
          sender.sendMessage(Component.text("Only a player can remove the nearest flag.", NamedTextColor.RED));
          return;
        }
        Arena arena = requireArena(sender, args, 2);
        if (arena == null) return;
        boolean removed = arena.removeNearestFlag(player.getLocation(), 8.0);
        if (removed) {
          arenas.save();
          markers.showMarkers(arena);
        }
        sender.sendMessage(Component.text(removed ? "Nearest flag removed." : "No flag point within 8 blocks.", removed ? NamedTextColor.GREEN : NamedTextColor.RED));
      }
      case "setrequired" -> {
        Arena arena = requireArena(sender, args, 2);
        if (arena == null) return;
        if (args.length < 4) {
          sender.sendMessage(Component.text("Usage: /ctf arena setrequired <name> <flags>", NamedTextColor.RED));
          return;
        }
        Integer value = positiveInt(args[3]);
        if (value == null) {
          sender.sendMessage(Component.text("Flags must be a positive integer.", NamedTextColor.RED));
          return;
        }
        arena.setRequiredFlags(value);
        arenas.save();
        sender.sendMessage(Component.text("Required controlled flags set to " + value + ".", NamedTextColor.GREEN));
      }
      case "setwinseconds" -> {
        Arena arena = requireArena(sender, args, 2);
        if (arena == null) return;
        if (args.length < 4) {
          sender.sendMessage(Component.text("Usage: /ctf arena setwinseconds <name> <seconds>", NamedTextColor.RED));
          return;
        }
        Integer value = positiveInt(args[3]);
        if (value == null) {
          sender.sendMessage(Component.text("Seconds must be a positive integer.", NamedTextColor.RED));
          return;
        }
        arena.setWinSeconds(value);
        arenas.save();
        sender.sendMessage(Component.text("Win countdown set to " + value + " seconds.", NamedTextColor.GREEN));
      }
      case "setneutral" -> {
        Arena arena = requireArena(sender, args, 2);
        if (arena == null) return;
        if (args.length < 4) {
          sender.sendMessage(Component.text("Usage: /ctf arena setneutral <name> <material>", NamedTextColor.RED));
          return;
        }
        Material material = Material.matchMaterial(args[3]);
        if (material == null || !material.isBlock()) {
          sender.sendMessage(Component.text("Unknown block material.", NamedTextColor.RED));
          return;
        }
        arena.setNeutralFlagMaterial(material);
        arenas.save();
        sender.sendMessage(Component.text("Neutral flag material set to " + material.name() + ".", NamedTextColor.GREEN));
      }
      default -> arenaHelp(sender);
    }
  }

  private void join(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(Component.text("Only players can join an arena.", NamedTextColor.RED));
      return;
    }
    if (!sender.hasPermission("ctf.play")) {
      sender.sendMessage(Component.text("You do not have permission to play CTF.", NamedTextColor.RED));
      return;
    }
    if (args.length < 3) {
      sender.sendMessage(Component.text("Usage: /ctf join <arena> <red|blue>", NamedTextColor.RED));
      return;
    }
    Arena arena = arenas.get(args[1]);
    TeamSide team = TeamSide.parse(args[2]);
    if (arena == null) {
      sender.sendMessage(Component.text("Arena not found.", NamedTextColor.RED));
      return;
    }
    if (team == null) {
      sender.sendMessage(Component.text("Team must be red or blue.", NamedTextColor.RED));
      return;
    }
    games.join(player, arena, team);
  }

  private void leave(CommandSender sender) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(Component.text("Only players can leave an arena.", NamedTextColor.RED));
      return;
    }
    if (games.session(player) == null) {
      sender.sendMessage(Component.text("You are not in a CTF arena.", NamedTextColor.YELLOW));
      return;
    }
    games.leave(player);
  }

  private void classCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(Component.text("Only players can choose a class.", NamedTextColor.RED));
      return;
    }
    PlayerSession session = games.session(player);
    if (session == null) {
      sender.sendMessage(Component.text("Join an arena first.", NamedTextColor.RED));
      return;
    }
    if (args.length < 2) {
      sender.sendMessage(Component.text("Classes: " + String.join(", ", classNames()), NamedTextColor.AQUA));
      return;
    }
    CtfClass ctfClass = CtfClass.parse(args[1]);
    if (ctfClass == null) {
      sender.sendMessage(Component.text("Unknown class. Classes: " + String.join(", ", classNames()), NamedTextColor.RED));
      return;
    }
    games.setClass(player, ctfClass);
  }

  private void start(CommandSender sender, String[] args) {
    if (!admin(sender)) return;
    if (args.length < 2) {
      sender.sendMessage(Component.text("Usage: /ctf start <arena>", NamedTextColor.RED));
      return;
    }
    Arena arena = arenas.get(args[1]);
    if (arena == null) {
      sender.sendMessage(Component.text("Arena not found.", NamedTextColor.RED));
      return;
    }
    List<String> errors = arena.validationErrors();
    if (!errors.isEmpty()) {
      sender.sendMessage(Component.text("Arena is not ready: " + String.join("; ", errors), NamedTextColor.RED));
      return;
    }
    if (!games.startGame(arena)) {
      String reason = games.state(arena) == ArenaState.WAITING
          ? "Could not start the arena. The pre-game backup failed; check the server console."
          : "Arena is already active.";
      sender.sendMessage(Component.text(reason, NamedTextColor.RED));
      return;
    }
    sender.sendMessage(Component.text("Starting " + arena.name() + ".", NamedTextColor.GREEN));
  }

  private void stop(CommandSender sender, String[] args) {
    if (!admin(sender)) return;
    if (args.length < 2) {
      sender.sendMessage(Component.text("Usage: /ctf stop <arena>", NamedTextColor.RED));
      return;
    }
    Arena arena = arenas.get(args[1]);
    if (arena == null) {
      sender.sendMessage(Component.text("Arena not found.", NamedTextColor.RED));
      return;
    }
    games.stopGame(arena);
    sender.sendMessage(Component.text("Stopped " + arena.name() + ".", NamedTextColor.YELLOW));
  }

  private void reset(CommandSender sender, String[] args) {
    if (!admin(sender)) return;
    if (args.length < 2) {
      sender.sendMessage(Component.text("Usage: /ctf reset <arena>", NamedTextColor.RED));
      return;
    }
    Arena arena = arenas.get(args[1]);
    if (arena == null) {
      sender.sendMessage(Component.text("Arena not found.", NamedTextColor.RED));
      return;
    }

    if (games.state(arena) != ArenaState.WAITING) games.stopGame(arena);
    boolean restored = games.resetArena(arena);
    sender.sendMessage(Component.text(
        restored
            ? "Restored " + arena.name() + " to its pre-game snapshot."
            : "No pre-game snapshot exists yet. Start a game once to create one.",
        restored ? NamedTextColor.GREEN : NamedTextColor.YELLOW
    ));
  }

  private void reload(CommandSender sender) {
    if (!admin(sender)) return;
    plugin.reloadConfig();
    sender.sendMessage(Component.text("CTF config reloaded. Arena changes are saved live; restart the server to reload build/break material lists.", NamedTextColor.GREEN));
  }

  private void info(CommandSender sender, Arena arena) {
    ArenaState state = games.state(arena);
    sender.sendMessage(Component.text("Arena: " + arena.name() + "  [" + state.name().toLowerCase(Locale.ROOT) + "]", NamedTextColor.AQUA));
    sender.sendMessage(Component.text("World: " + arena.worldName() + " | flags: " + arena.flags().size() + " | required: " + arena.requiredFlags() + " | win: " + arena.winSeconds() + "s", NamedTextColor.GRAY));
    List<String> errors = arena.validationErrors();
    if (errors.isEmpty()) sender.sendMessage(Component.text("Validation: ready to play.", NamedTextColor.GREEN));
    else sender.sendMessage(Component.text("Validation: " + String.join("; ", errors), NamedTextColor.RED));
  }

  private Arena requireArena(CommandSender sender, String[] args, int index) {
    if (args.length <= index) {
      sender.sendMessage(Component.text("Arena name required.", NamedTextColor.RED));
      return null;
    }
    Arena arena = arenas.get(args[index]);
    if (arena == null) sender.sendMessage(Component.text("Arena not found.", NamedTextColor.RED));
    return arena;
  }

  private boolean admin(CommandSender sender) {
    if (sender.hasPermission("ctf.admin")) return true;
    sender.sendMessage(Component.text("You do not have permission to administer CTF.", NamedTextColor.RED));
    return false;
  }

  private void help(CommandSender sender) {
    sender.sendMessage(Component.text("Capture the Flag", NamedTextColor.GOLD));
    sender.sendMessage(Component.text("/ctf join <arena> <red|blue> - join a team", NamedTextColor.GRAY));
    sender.sendMessage(Component.text("/ctf class <class> - choose a class", NamedTextColor.GRAY));
    sender.sendMessage(Component.text("/ctf leave - leave your arena", NamedTextColor.GRAY));
    if (sender.hasPermission("ctf.admin")) {
      sender.sendMessage(Component.text("/ctf arena ... - configure arenas", NamedTextColor.GRAY));
      sender.sendMessage(Component.text("/ctf start <arena> | /ctf stop <arena> | /ctf reset <arena>", NamedTextColor.GRAY));
    }
  }

  private void arenaHelp(CommandSender sender) {
    sender.sendMessage(Component.text("Arena commands:", NamedTextColor.GOLD));
    sender.sendMessage(Component.text("/ctf arena create <name> | delete <name> | list | info <name>", NamedTextColor.GRAY));
    sender.sendMessage(Component.text("/ctf arena markers <name> | showmarkers <name> | hidemarkers <name>", NamedTextColor.GRAY));
    sender.sendMessage(Component.text("/ctf arena removeflag <name> | clearflags <name>", NamedTextColor.GRAY));
    sender.sendMessage(Component.text("/ctf arena setrequired <name> <flags> | setwinseconds <name> <seconds>", NamedTextColor.GRAY));
    sender.sendMessage(Component.text("/ctf arena setneutral <name> <material>", NamedTextColor.GRAY));
  }

  private static Integer positiveInt(String raw) {
    try {
      int value = Integer.parseInt(raw);
      return value > 0 ? value : null;
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private static List<String> classNames() {
    return List.of(CtfClass.values()).stream().map(CtfClass::commandName).toList();
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) return filter(List.of("help", "arena", "join", "leave", "class", "start", "stop", "reset", "reload"), args[0]);
    if (args[0].equalsIgnoreCase("join")) {
      if (args.length == 2) return filter(arenas.names(), args[1]);
      if (args.length == 3) return filter(List.of("red", "blue"), args[2]);
    }
    if (args[0].equalsIgnoreCase("class") && args.length == 2) return filter(classNames(), args[1]);
    if ((args[0].equalsIgnoreCase("start")
        || args[0].equalsIgnoreCase("stop")
        || args[0].equalsIgnoreCase("reset")) && args.length == 2) {
      return filter(arenas.names(), args[1]);
    }
    if (args[0].equalsIgnoreCase("arena")) {
      if (args.length == 2) return filter(List.of("create", "delete", "list", "info", "validate", "markers", "showmarkers", "hidemarkers", "removeflag", "clearflags", "setrequired", "setwinseconds", "setneutral"), args[1]);
      if (args.length == 3 && !args[1].equalsIgnoreCase("create") && !args[1].equalsIgnoreCase("list")) return filter(arenas.names(), args[2]);
      if (args.length == 4 && args[1].equalsIgnoreCase("setneutral")) return filter(List.of("GLASS", "WHITE_STAINED_GLASS", "BEACON"), args[3]);
    }
    return List.of();
  }

  private static List<String> filter(List<String> values, String prefix) {
    String lower = prefix.toLowerCase(Locale.ROOT);
    List<String> result = new ArrayList<>();
    for (String value : values) {
      if (value.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(value);
    }
    return result;
  }
}
