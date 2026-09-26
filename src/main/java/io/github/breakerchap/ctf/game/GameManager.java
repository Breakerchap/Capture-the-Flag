package io.github.breakerchap.ctf.game;

import io.github.breakerchap.ctf.CaptureTheFlagPlugin;
import io.github.breakerchap.ctf.arena.Arena;
import io.github.breakerchap.ctf.arena.ArenaManager;
import io.github.breakerchap.ctf.arena.ArenaState;
import io.github.breakerchap.ctf.arena.FlagPoint;
import io.github.breakerchap.ctf.kit.KitManager;
import io.github.breakerchap.ctf.marker.MarkerManager;
import io.github.breakerchap.ctf.player.CtfClass;
import io.github.breakerchap.ctf.player.PlayerSession;
import io.github.breakerchap.ctf.player.TeamSide;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.WindCharge;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public final class GameManager {
  private final CaptureTheFlagPlugin plugin;
  private final ArenaManager arenas;
  private final MarkerManager markers;
  private final KitManager kits;
  private final Map<UUID, PlayerSession> sessions = new HashMap<>();
  private final Map<String, RuntimeArena> runtimes = new HashMap<>();
  private static final int SNAPSHOT_MAGIC = 0x43544653;
  private static final int SNAPSHOT_VERSION = 1;

  private final Map<UUID, Map<String, Integer>> cooldowns = new HashMap<>();
  private final Map<UUID, ItemStack[]> hiddenAssassinArmour = new HashMap<>();
  private final Map<BlockKey, WaterCell> abilityWater = new HashMap<>();
  private final Map<UUID, Set<BlockKey>> waterUses = new HashMap<>();
  private final Map<BlockKey, LeafCell> leafWalkLeaves = new HashMap<>();
  private final Map<UUID, MatchStats> matchStats = new HashMap<>();
  private final NamespacedKey mobArenaKey;
  private final NamespacedKey mobTeamKey;
  private BukkitTask ticker;
  private long ticks;

  public GameManager(
      CaptureTheFlagPlugin plugin,
      ArenaManager arenas,
      MarkerManager markers,
      KitManager kits
  ) {
    this.plugin = plugin;
    this.arenas = arenas;
    this.markers = markers;
    this.kits = kits;
    this.mobArenaKey = new NamespacedKey(plugin, "mob_arena");
    this.mobTeamKey = new NamespacedKey(plugin, "mob_team");
  }

  public void startTicker() {
    if (ticker != null) ticker.cancel();
    ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
  }

  public void recoverInterruptedGames() {
    for (Arena arena : arenas.all()) {
      File file = snapshotFile(arena);
      if (!file.exists()) continue;

      ArenaSnapshot snapshot = loadSnapshot(file);
      if (snapshot == null) {
        plugin.getLogger().severe(
            "Could not recover interrupted CTF arena '" + arena.name()
                + "'. Snapshot kept at " + file.getAbsolutePath()
        );
        continue;
      }

      RuntimeArena runtime = runtime(arena);
      runtime.snapshot = snapshot;
      clearTransientBlocks(arena);
      restoreArena(runtime);
      clearTemporaryEntities(arena);
      deleteSnapshotFile(arena);
      plugin.getLogger().warning(
          "Recovered arena '" + arena.name() + "' from an interrupted match snapshot."
      );
    }
  }

  public void shutdown() {
    if (ticker != null) ticker.cancel();
    for (RuntimeArena runtime : runtimes.values()) {
      hideBossbars(runtime.arena);
      if (runtime.state != ArenaState.WAITING) {
        clearTransientBlocks(runtime.arena);
        restoreArena(runtime);
        clearTemporaryEntities(runtime.arena);
        deleteSnapshotFile(runtime.arena);
      }
    }
    abilityWater.clear();
    waterUses.clear();
    leafWalkLeaves.clear();
    runtimes.clear();
  }

  public PlayerSession session(Player player) {
    return sessions.get(player.getUniqueId());
  }

  public PlayerSession session(UUID uuid) {
    return sessions.get(uuid);
  }

  public Arena arenaFor(Player player) {
    PlayerSession session = session(player);
    return session == null ? null : arenas.get(session.arenaName());
  }

  public ArenaState state(Arena arena) {
    return runtime(arena).state;
  }

  public boolean isRunning(Arena arena) {
    return state(arena) == ArenaState.RUNNING;
  }

  public List<Player> players(Arena arena) {
    List<Player> players = new ArrayList<>();
    for (Player player : plugin.getServer().getOnlinePlayers()) {
      PlayerSession session = session(player);
      if (session != null && session.arenaName().equalsIgnoreCase(arena.name())) players.add(player);
    }
    return players;
  }

  public List<Player> teamPlayers(Arena arena, TeamSide team) {
    return players(arena).stream()
        .filter(player -> session(player).team() == team)
        .toList();
  }

  public void join(Player player, Arena arena, TeamSide team) {
    PlayerSession previous = session(player);
    if (previous != null) leave(player, false);

    PlayerSession session = new PlayerSession(arena.name(), team);
    sessions.put(player.getUniqueId(), session);
    player.sendMessage(Component.text(
        "Joined " + arena.name() + " on the " + team.displayName() + " team.",
        team == TeamSide.RED ? NamedTextColor.RED : NamedTextColor.BLUE
    ));

    RuntimeArena runtime = runtime(arena);
    if (runtime.state != ArenaState.WAITING) showBossbars(player, runtime);
    if (runtime.state == ArenaState.RUNNING) spawnAndKit(player);
  }

  public void leave(Player player) {
    leave(player, true);
  }

  private void leave(Player player, boolean message) {
    PlayerSession removed = sessions.remove(player.getUniqueId());
    cooldowns.remove(player.getUniqueId());
    restoreAssassinArmour(player);
    if (removed == null) return;

    Arena arena = arenas.get(removed.arenaName());
    if (arena != null) hideBossbars(player, runtime(arena));
    player.getInventory().clear();
    player.getInventory().setArmorContents(new ItemStack[4]);
    player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    if (message) player.sendMessage(Component.text("Left Capture the Flag.", NamedTextColor.YELLOW));
  }

  public void disconnect(Player player) {
    PlayerSession session = session(player);
    if (session == null) return;
    Arena arena = arenas.get(session.arenaName());
    restoreAssassinArmour(player);
    if (arena != null) hideBossbars(player, runtime(arena));
  }

  public void reconnect(Player player) {
    PlayerSession session = session(player);
    if (session == null) return;
    Arena arena = arenas.get(session.arenaName());
    if (arena == null) {
      sessions.remove(player.getUniqueId());
      cooldowns.remove(player.getUniqueId());
      return;
    }

    RuntimeArena runtime = runtime(arena);
    player.sendMessage(Component.text(
        "Reconnected to " + arena.name() + " on the " + session.team().displayName() + " team.",
        NamedTextColor.GRAY
    ));

    switch (runtime.state) {
      case COUNTDOWN -> {
        showBossbars(player, runtime);
        prepareForCountdown(player);
      }
      case RUNNING -> {
        showBossbars(player, runtime);
        player.setGameMode(GameMode.SURVIVAL);
        String ability = classAbility(session.ctfClass());
        if (ability != null) {
          ensureAbility(player, ability);
          applyVisualCooldown(player, ability, cooldown(player, ability));
          updateAbilityLore(player, ability);
        }
      }
      case FINISHED -> {
        player.setGameMode(GameMode.SPECTATOR);
        showPersonalResults(player);
      }
      case WAITING -> returnPlayerToBase(player, arena);
    }
  }

  public void setClass(Player player, CtfClass ctfClass) {
    PlayerSession session = session(player);
    if (session == null) return;
    session.setCtfClass(ctfClass);
    player.sendMessage(Component.text(
        "Class set to " + ctfClass.commandName().replace('_', ' ') + ".",
        NamedTextColor.GREEN
    ));
    Arena arena = arenas.get(session.arenaName());
    if (arena != null && isRunning(arena)) givePlayerKit(player);
  }

  public boolean startGame(Arena arena) {
    List<String> errors = arena.validationErrors();
    if (!errors.isEmpty()) return false;

    RuntimeArena runtime = runtime(arena);
    if (runtime.state != ArenaState.WAITING) return false;

    clearTransientBlocks(arena);
    ArenaSnapshot snapshot = captureArena(arena);
    if (snapshot == null || !persistSnapshot(arena, snapshot)) return false;
    runtime.snapshot = snapshot;

    clearMatchStats(arena);
    markers.hideMarkers(arena);
    resetFlags(arena);
    runtime.redSeconds = arena.winSeconds();
    runtime.blueSeconds = arena.winSeconds();
    runtime.state = ArenaState.COUNTDOWN;
    updateBossbars(runtime);

    for (Player player : players(arena)) {
      showBossbars(player, runtime);
      prepareForCountdown(player);
    }

    new BukkitRunnable() {
      private int count = 3;

      @Override
      public void run() {
        if (runtime.state != ArenaState.COUNTDOWN) {
          cancel();
          return;
        }
        if (count > 0) {
          NamedTextColor colour = count == 3
              ? NamedTextColor.RED
              : count == 2 ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
          for (Player player : players(arena)) {
            showTitle(player, Component.text(Integer.toString(count), colour));
          }
          count--;
          return;
        }

        runtime.state = ArenaState.RUNNING;
        for (Player player : players(arena)) {
          spawnAndKit(player);
          showTitle(player, Component.text("Go!", NamedTextColor.GREEN));
        }
        cancel();
      }
    }.runTaskTimer(plugin, 0L, 20L);

    return true;
  }

  public void stopGame(Arena arena) {
    RuntimeArena runtime = runtime(arena);
    boolean wasActive = runtime.state != ArenaState.WAITING;

    runtime.state = ArenaState.WAITING;
    runtime.redSeconds = arena.winSeconds();
    runtime.blueSeconds = arena.winSeconds();

    if (wasActive) {
      clearTransientBlocks(arena);
      restoreArena(runtime);
      clearTemporaryEntities(arena);
      deleteSnapshotFile(arena);
    }
    hideBossbars(arena);

    for (Player player : players(arena)) {
      cooldowns.remove(player.getUniqueId());
      clearAbilityCooldowns(player);
      restoreAssassinArmour(player);
      returnPlayerToBase(player, arena);
      player.sendMessage(Component.text("The CTF game was stopped.", NamedTextColor.YELLOW));
    }
  }

  public void respawnAfterDelay(Player player) {
    PlayerSession session = session(player);
    if (session == null) return;
    Arena arena = arenas.get(session.arenaName());
    if (arena == null || !isRunning(arena)) return;

    player.setGameMode(GameMode.SPECTATOR);
    Location spectator = arena.spectatorOrCentre();
    if (spectator != null) player.teleport(spectator);
    showTitle(player, Component.text("3", NamedTextColor.RED));

    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      if (sameRunningArena(player, arena)) {
        showTitle(player, Component.text("2", NamedTextColor.GOLD));
      }
    }, 20L);

    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      if (sameRunningArena(player, arena)) {
        showTitle(player, Component.text("1", NamedTextColor.YELLOW));
      }
    }, 40L);

    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      if (!sameRunningArena(player, arena)) return;
      spawnAndKit(player);
      showTitle(player, Component.text("Go!", NamedTextColor.GREEN));
    }, 60L);
  }

  public boolean useAbility(Player player, String ability) {
    PlayerSession session = session(player);
    if (session == null) return false;
    Arena arena = arenas.get(session.arenaName());
    if (arena == null || !isRunning(arena)) return false;

    int remaining = cooldown(player, ability);
    if (remaining > 0) {
      sendCooldownFeedback(player, remaining);
      return false;
    }

    switch (ability) {
      case KitManager.GREAT_FEAST -> {
        setCooldown(player, ability, 1800);
        int speedAmplifier = session.team() == TeamSide.RED ? 2 : 1;
        for (Player teammate : teamPlayers(arena, session.team())) {
          teammate.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, speedAmplifier));
          teammate.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 0));
          teammate.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 400, 0));
          teammate.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 400, 1));
          showTitle(teammate, Component.text("Great Feast Used", NamedTextColor.GOLD));
        }
      }
      case KitManager.LEAF_WALK -> {
        // Datapack: 100 ticks active, then 600 ticks before it can be used again.
        // Keep one continuous visible cooldown so the item never needs to disappear.
        setCooldown(player, ability, 700);
        startLeafWalk(player, arena);
      }
      case KitManager.ASSASSIN_PEARL -> {
        setCooldown(player, ability, 400);
        player.launchProjectile(EnderPearl.class);
      }
      case KitManager.WIND_BURST -> {
        setCooldown(player, ability, 200);
        player.launchProjectile(WindCharge.class);
      }
      case KitManager.RAISE_DEAD -> {
        setCooldown(player, ability, 1800);
        raiseDead(player, arena, session.team());
      }
      case KitManager.BATWINGS -> {
        setCooldown(player, ability, 740);
        activateBatwings(player, arena);
      }
      default -> {
        return false;
      }
    }
    return true;
  }

  public void projectileUsed(Player player, String entityTypeName) {
    PlayerSession session = session(player);
    if (session == null) return;
    Arena arena = arenas.get(session.arenaName());
    if (arena == null || !isRunning(arena)) return;

    if (session.ctfClass() == CtfClass.ASSASSIN
        && "ENDER_PEARL".equals(entityTypeName)
        && cooldown(player, KitManager.ASSASSIN_PEARL) <= 0) {
      setCooldown(player, KitManager.ASSASSIN_PEARL, 400);
    } else if (session.ctfClass() == CtfClass.MACE_BEARER
        && "WIND_CHARGE".equals(entityTypeName)
        && cooldown(player, KitManager.WIND_BURST) <= 0) {
      setCooldown(player, KitManager.WIND_BURST, 200);
    }
  }

  public void waterBucketUsed(Player player, Location placedAt) {
    PlayerSession session = session(player);
    if (session == null || session.ctfClass() != CtfClass.SWIMMER) return;
    Arena arena = arenas.get(session.arenaName());
    if (arena == null || !isRunning(arena)) return;

    int remaining = cooldown(player, KitManager.WATER_BUCKET);
    if (remaining > 0) {
      sendCooldownFeedback(player, remaining);
      return;
    }

    Block target = placedAt.getBlock();
    if (!target.getType().isAir() && !abilityWater.containsKey(blockKey(target))) return;

    UUID useId = UUID.randomUUID();
    WaterCell cell = abilityWater.get(blockKey(target));
    if (cell == null) {
      cell = new WaterCell(arena.name(), target.getBlockData().clone());
      abilityWater.put(blockKey(target), cell);
    } else if (!cell.arenaName.equalsIgnoreCase(arena.name())) {
      return;
    }

    claimWater(useId, blockKey(target), cell);
    target.setType(Material.WATER, false);
    setCooldown(player, KitManager.WATER_BUCKET, 800);

    UUID scheduledUse = useId;
    plugin.getServer().getScheduler().runTaskLater(
        plugin,
        () -> cleanupWaterUse(scheduledUse),
        110L
    );
  }

  public void waterFlow(Block from, Block to) {
    WaterCell source = abilityWater.get(blockKey(from));
    if (source == null || from.getType() != Material.WATER) return;

    Arena arena = arenas.get(source.arenaName);
    if (arena == null || !isRunning(arena) || !arena.contains(to.getLocation())) return;

    BlockKey destinationKey = blockKey(to);
    WaterCell destination = abilityWater.get(destinationKey);

    // Never claim decorative/pre-existing water. We only propagate into blocks that
    // this ability is actually causing to become water, or water already owned by it.
    if (destination == null) {
      if (!to.getType().isAir()) return;
      destination = new WaterCell(arena.name(), to.getBlockData().clone());
      abilityWater.put(destinationKey, destination);
    }

    if (!destination.arenaName.equalsIgnoreCase(arena.name())) return;
    for (UUID owner : new HashSet<>(source.owners)) {
      claimWater(owner, destinationKey, destination);
    }
  }

  private void claimWater(UUID useId, BlockKey key, WaterCell cell) {
    cell.owners.add(useId);
    waterUses.computeIfAbsent(useId, ignored -> new HashSet<>()).add(key);
  }

  private void cleanupWaterUse(UUID useId) {
    Set<BlockKey> cells = waterUses.remove(useId);
    if (cells == null) return;

    for (BlockKey key : cells) {
      WaterCell cell = abilityWater.get(key);
      if (cell == null) continue;
      cell.owners.remove(useId);
      if (!cell.owners.isEmpty()) continue;

      Block block = blockForKey(key);
      if (block != null && block.getType() == Material.WATER) {
        block.setBlockData(cell.original.clone(), false);
      }
      abilityWater.remove(key);
    }
  }

  private void clearAbilityWater(Arena arena) {
    Set<UUID> uses = new HashSet<>();
    for (WaterCell cell : abilityWater.values()) {
      if (cell.arenaName.equalsIgnoreCase(arena.name())) uses.addAll(cell.owners);
    }
    for (UUID use : uses) cleanupWaterUse(use);
  }

  public TeamSide mobTeam(Entity entity) {
    String raw = entity.getPersistentDataContainer().get(mobTeamKey, PersistentDataType.STRING);
    return TeamSide.parse(raw);
  }

  public Arena mobArena(Entity entity) {
    String raw = entity.getPersistentDataContainer().get(mobArenaKey, PersistentDataType.STRING);
    return raw == null ? null : arenas.get(raw);
  }

  public int cooldown(Player player, String ability) {
    return cooldowns.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(ability, 0);
  }

  public void givePlayerKit(Player player) {
    PlayerSession session = session(player);
    if (session == null) return;
    restoreAssassinArmour(player);
    kits.giveKit(player, session.team(), session.ctfClass());
    initialiseCooldowns(player, session.ctfClass());
    String ability = classAbility(session.ctfClass());
    if (ability != null) ensureAbility(player, ability);
  }

  private void tick() {
    ticks++;
    tickCooldownsAndClassStates();
    if (ticks % 20L == 0L) tickArenas();
  }

  private void tickCooldownsAndClassStates() {
    for (Player player : plugin.getServer().getOnlinePlayers()) {
      PlayerSession session = session(player);
      if (session == null) continue;
      Arena arena = arenas.get(session.arenaName());
      if (arena == null || !isRunning(arena)) continue;

      Map<String, Integer> playerCooldowns = cooldowns.computeIfAbsent(
          player.getUniqueId(),
          ignored -> new HashMap<>()
      );
      for (Map.Entry<String, Integer> entry : new ArrayList<>(playerCooldowns.entrySet())) {
        if (entry.getValue() > 0) entry.setValue(entry.getValue() - 1);
      }

      String ability = classAbility(session.ctfClass());
      if (ability != null) {
        ensureAbility(player, ability);
        if (ticks % 20L == 0L) updateAbilityLore(player, ability);
      }

      if (session.ctfClass() == CtfClass.ASSASSIN) tickAssassinInvisibility(player);
      else restoreAssassinArmour(player);
    }
  }

  private void tickArenas() {
    for (RuntimeArena runtime : runtimes.values()) {
      if (runtime.state != ArenaState.RUNNING) continue;

      int redFlags = countFlags(runtime.arena, TeamSide.RED);
      int blueFlags = countFlags(runtime.arena, TeamSide.BLUE);
      if (redFlags >= runtime.arena.requiredFlags()) runtime.redSeconds--;
      if (blueFlags >= runtime.arena.requiredFlags()) runtime.blueSeconds--;
      updateBossbars(runtime);

      for (Player player : players(runtime.arena)) {
        player.sendActionBar(Component.text("Red Flags: ", NamedTextColor.RED)
            .append(Component.text(redFlags, NamedTextColor.WHITE))
            .append(Component.text(" | Blue Flags: ", NamedTextColor.BLUE))
            .append(Component.text(blueFlags, NamedTextColor.WHITE))
            .append(Component.text("   Red: ", NamedTextColor.DARK_RED))
            .append(Component.text(Math.max(0, runtime.redSeconds) + "s", NamedTextColor.WHITE))
            .append(Component.text(" | Blue: ", NamedTextColor.DARK_BLUE))
            .append(Component.text(Math.max(0, runtime.blueSeconds) + "s", NamedTextColor.WHITE)));
      }

      if (runtime.redSeconds <= 0 || runtime.blueSeconds <= 0) finish(runtime);
    }
  }

  private void finish(RuntimeArena runtime) {
    if (runtime.state != ArenaState.RUNNING) return;
    runtime.state = ArenaState.FINISHED;

    int redFlags = countFlags(runtime.arena, TeamSide.RED);
    int blueFlags = countFlags(runtime.arena, TeamSide.BLUE);

    Component title;
    if (runtime.redSeconds <= 0 && runtime.blueSeconds <= 0) {
      title = Component.text("Draw!", NamedTextColor.GOLD);
    } else if (runtime.redSeconds <= 0) {
      title = Component.text("Red Wins!", NamedTextColor.RED);
    } else {
      title = Component.text("Blue Wins!", NamedTextColor.BLUE);
    }

    for (Player player : players(runtime.arena)) {
      showTitle(player, title);
      player.sendMessage(Component.text("Match Results", NamedTextColor.GOLD)
          .append(Component.text("  Red flags: ", NamedTextColor.GRAY))
          .append(Component.text(redFlags, NamedTextColor.RED))
          .append(Component.text(" | Blue flags: ", NamedTextColor.GRAY))
          .append(Component.text(blueFlags, NamedTextColor.BLUE)));
      showPersonalResults(player);

      restoreAssassinArmour(player);
      player.setGameMode(GameMode.SPECTATOR);
      player.getInventory().clear();
      player.getInventory().setArmorContents(new ItemStack[4]);
      cooldowns.remove(player.getUniqueId());
      clearAbilityCooldowns(player);
    }

    hideBossbars(runtime.arena);
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      clearTransientBlocks(runtime.arena);
      restoreArena(runtime);
      clearTemporaryEntities(runtime.arena);
      deleteSnapshotFile(runtime.arena);
      runtime.redSeconds = runtime.arena.winSeconds();
      runtime.blueSeconds = runtime.arena.winSeconds();
      runtime.state = ArenaState.WAITING;

      for (Player player : players(runtime.arena)) {
        returnPlayerToBase(player, runtime.arena);
      }
    }, 100L);
  }

  private int countFlags(Arena arena, TeamSide team) {
    World world = arena.world();
    if (world == null) return 0;

    int count = 0;
    for (FlagPoint flag : arena.flags()) {
      boolean all = true;
      for (Block block : flag.blocks(world)) {
        if (block.getType() != team.flagMaterial()) {
          all = false;
          break;
        }
      }
      if (all) count++;
    }
    return count;
  }

  public void resetFlags(Arena arena) {
    World world = arena.world();
    if (world == null) return;
    for (FlagPoint flag : arena.flags()) {
      for (Block block : flag.blocks(world)) {
        block.setType(arena.neutralFlagMaterial(), false);
      }
    }
  }

  private void prepareForCountdown(Player player) {
    player.getInventory().clear();
    player.getInventory().setArmorContents(new ItemStack[4]);
    player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    player.setGameMode(GameMode.ADVENTURE);

    Arena arena = arenaFor(player);
    PlayerSession session = session(player);
    if (arena != null && session != null) {
      Location base = arena.base(session.team());
      if (base != null) player.teleport(base);
    }
  }

  private void spawnAndKit(Player player) {
    PlayerSession session = session(player);
    if (session == null) return;
    Arena arena = arenas.get(session.arenaName());
    if (arena == null) return;

    Location base = arena.base(session.team());
    if (base != null) player.teleport(base);
    player.setGameMode(GameMode.SURVIVAL);
    player.setHealth(player.getMaxHealth());
    player.setFoodLevel(20);
    player.setSaturation(20.0f);
    givePlayerKit(player);
  }

  private void initialiseCooldowns(Player player, CtfClass ctfClass) {
    Map<String, Integer> map = new HashMap<>();
    switch (ctfClass) {
      case CHEF -> map.put(KitManager.GREAT_FEAST, 200);
      case HUNTER -> map.put(KitManager.LEAF_WALK, 200);
      case ASSASSIN -> map.put(KitManager.ASSASSIN_PEARL, 200);
      case MACE_BEARER -> map.put(KitManager.WIND_BURST, 200);
      case SWIMMER -> map.put(KitManager.WATER_BUCKET, 200);
      case VAMPIRE -> map.put(KitManager.BATWINGS, 300);
      case NECROMANCER -> map.put(KitManager.RAISE_DEAD, 300);
      case PRO -> { }
    }
    cooldowns.put(player.getUniqueId(), map);
    for (Map.Entry<String, Integer> entry : map.entrySet()) {
      applyVisualCooldown(player, entry.getKey(), entry.getValue());
    }
  }

  private String classAbility(CtfClass ctfClass) {
    return switch (ctfClass) {
      case CHEF -> KitManager.GREAT_FEAST;
      case HUNTER -> KitManager.LEAF_WALK;
      case ASSASSIN -> KitManager.ASSASSIN_PEARL;
      case MACE_BEARER -> KitManager.WIND_BURST;
      case SWIMMER -> KitManager.WATER_BUCKET;
      case VAMPIRE -> KitManager.BATWINGS;
      case NECROMANCER -> KitManager.RAISE_DEAD;
      case PRO -> null;
    };
  }

  private void ensureAbility(Player player, String ability) {
    if (hasAbility(player, ability)) return;
    ItemStack item = kits.abilityItem(ability);
    applyAbilityLore(item, cooldown(player, ability));

    // Preserve the datapack's convenient last-hotbar-slot placement when it is free,
    // but never destroy whatever the player already has there.
    ItemStack preferred = player.getInventory().getItem(8);
    if (preferred == null || preferred.getType().isAir()) {
      player.getInventory().setItem(8, item);
      return;
    }

    // addItem only uses empty/mergeable slots and therefore cannot overwrite an item.
    player.getInventory().addItem(item);
  }

  private void updateAbilityLore(Player player, String ability) {
    for (ItemStack item : player.getInventory().getContents()) {
      if (ability.equals(kits.ability(item))) applyAbilityLore(item, cooldown(player, ability));
    }
    ItemStack offhand = player.getInventory().getItemInOffHand();
    if (ability.equals(kits.ability(offhand))) applyAbilityLore(offhand, cooldown(player, ability));
  }

  private void applyAbilityLore(ItemStack item, int remainingTicks) {
    if (item == null || item.getType().isAir()) return;
    ItemMeta meta = item.getItemMeta();
    if (remainingTicks <= 0) {
      meta.lore(List.of(Component.text("Ready", NamedTextColor.GREEN)));
    } else {
      String seconds = String.format(Locale.ROOT, "%.1f", remainingTicks / 20.0);
      meta.lore(List.of(Component.text("Cooldown: " + seconds + "s", NamedTextColor.GRAY)));
    }
    item.setItemMeta(meta);
  }

  private void sendCooldownFeedback(Player player, int remainingTicks) {
    String seconds = String.format(Locale.ROOT, "%.1f", remainingTicks / 20.0);
    player.sendActionBar(Component.text("Ability ready in " + seconds + "s", NamedTextColor.YELLOW));
  }

  private boolean hasAbility(Player player, String ability) {
    for (ItemStack item : player.getInventory().getContents()) {
      if (ability.equals(kits.ability(item))) return true;
    }
    return ability.equals(kits.ability(player.getInventory().getItemInOffHand()));
  }

  private void removeAbility(Player player, String ability) {
    ItemStack[] contents = player.getInventory().getContents();
    for (int i = 0; i < contents.length; i++) {
      if (ability.equals(kits.ability(contents[i]))) player.getInventory().setItem(i, null);
    }
    if (ability.equals(kits.ability(player.getInventory().getItemInOffHand()))) {
      player.getInventory().setItemInOffHand(null);
    }
  }

  private void setCooldown(Player player, String ability, int ticks) {
    cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>()).put(ability, ticks);
    applyVisualCooldown(player, ability, ticks);
    updateAbilityLore(player, ability);
  }

  private void applyVisualCooldown(Player player, String ability, int ticks) {
    Material material = kits.abilityItem(ability).getType();
    player.setCooldown(material, Math.max(0, ticks));
  }

  private void clearAbilityCooldowns(Player player) {
    for (String ability : List.of(
        KitManager.GREAT_FEAST,
        KitManager.LEAF_WALK,
        KitManager.RAISE_DEAD,
        KitManager.BATWINGS,
        KitManager.WIND_BURST,
        KitManager.ASSASSIN_PEARL,
        KitManager.WATER_BUCKET
    )) {
      player.setCooldown(kits.abilityItem(ability).getType(), 0);
    }
  }

  private void startLeafWalk(Player player, Arena arena) {
    Set<BlockKey> platform = new HashSet<>();

    new BukkitRunnable() {
      private int age;

      @Override
      public void run() {
        PlayerSession current = session(player);
        if (!sameRunningArena(player, arena)
            || current == null
            || current.ctfClass() != CtfClass.HUNTER) {
          releaseLeafPlatform(platform);
          cancel();
          return;
        }

        // Leaves cease to exist as soon as they are no longer the current platform.
        releaseLeafPlatform(platform);

        if (player.isSneaking()) {
          Vector velocity = player.getVelocity();
          if (velocity.getY() > -0.35) {
            velocity.setY(-0.35);
            player.setVelocity(velocity);
          }
        } else {
          int y = player.getLocation().getBlockY() - 1;
          int x = player.getLocation().getBlockX();
          int z = player.getLocation().getBlockZ();
          for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
              Block block = arena.world().getBlockAt(x + dx, y, z + dz);
              if (arena.contains(block.getLocation())) claimLeaf(arena, block, platform);
            }
          }
        }

        age++;
        if (age >= 100) {
          releaseLeafPlatform(platform);
          cancel();
        }
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  private void claimLeaf(Arena arena, Block block, Set<BlockKey> platform) {
    BlockKey key = blockKey(block);
    LeafCell existing = leafWalkLeaves.get(key);

    if (existing != null) {
      if (!existing.arenaName.equalsIgnoreCase(arena.name())
          || block.getType() != Material.DARK_OAK_LEAVES) return;
      existing.references++;
      platform.add(key);
      return;
    }

    if (!block.getType().isAir()) return;
    block.setType(Material.DARK_OAK_LEAVES, false);
    leafWalkLeaves.put(key, new LeafCell(arena.name()));
    platform.add(key);
  }

  private void releaseLeafPlatform(Set<BlockKey> platform) {
    for (BlockKey key : new HashSet<>(platform)) {
      LeafCell cell = leafWalkLeaves.get(key);
      if (cell == null) continue;
      cell.references--;
      if (cell.references > 0) continue;

      Block block = blockForKey(key);
      if (block != null && block.getType() == Material.DARK_OAK_LEAVES) {
        block.setType(Material.AIR, false);
      }
      leafWalkLeaves.remove(key);
    }
    platform.clear();
  }

  private void clearLeafWalkArena(Arena arena) {
    for (Map.Entry<BlockKey, LeafCell> entry : new ArrayList<>(leafWalkLeaves.entrySet())) {
      if (!entry.getValue().arenaName.equalsIgnoreCase(arena.name())) continue;
      Block block = blockForKey(entry.getKey());
      if (block != null && block.getType() == Material.DARK_OAK_LEAVES) {
        block.setType(Material.AIR, false);
      }
      leafWalkLeaves.remove(entry.getKey());
    }
  }

  private void raiseDead(Player player, Arena arena, TeamSide team) {
    // Match the datapack's local (^) coordinates, including pitch, so looking
    // upwards or downwards works just as reliably as looking horizontally.
    Vector forward = player.getEyeLocation().getDirection().normalize();
    double yaw = Math.toRadians(player.getLocation().getYaw());
    Vector left = new Vector(Math.cos(yaw), 0, Math.sin(yaw)).normalize();
    Vector up = forward.clone().crossProduct(left).normalize();
    Location origin = player.getLocation();

    spawnZombie(arena, team, localOffset(origin, left, up, forward, 0, 1.5, 1.5));
    spawnZombie(arena, team, localOffset(origin, left, up, forward, 0, 1.5, -1.5));
    spawnSkeleton(arena, team, localOffset(origin, left, up, forward, 1.5, 1.5, 0));
    spawnSkeleton(arena, team, localOffset(origin, left, up, forward, -1.5, 1.5, 0));
  }

  private static Location localOffset(
      Location origin,
      Vector left,
      Vector up,
      Vector forward,
      double localX,
      double localY,
      double localZ
  ) {
    return origin.clone()
        .add(left.clone().multiply(localX))
        .add(up.clone().multiply(localY))
        .add(forward.clone().multiply(localZ));
  }

  private void spawnZombie(Arena arena, TeamSide team, Location location) {
    Zombie mob = arena.world().spawn(location, Zombie.class, zombie -> {
      zombie.customName(Component.text("Necromancer's Zombie", NamedTextColor.DARK_PURPLE));
      zombie.setCustomNameVisible(true);
      tagMob(zombie, arena, team);
      EntityEquipment equipment = zombie.getEquipment();
      if (equipment != null) equipment.setHelmet(new ItemStack(Material.LEATHER_HELMET));
    });
    plugin.getServer().getScheduler().runTaskLater(plugin, mob::remove, 600L);
  }

  private void spawnSkeleton(Arena arena, TeamSide team, Location location) {
    Skeleton mob = arena.world().spawn(location, Skeleton.class, skeleton -> {
      skeleton.customName(Component.text("Necromancer's Skeleton", NamedTextColor.DARK_PURPLE));
      skeleton.setCustomNameVisible(true);
      tagMob(skeleton, arena, team);
      EntityEquipment equipment = skeleton.getEquipment();
      if (equipment != null) equipment.setHelmet(new ItemStack(Material.LEATHER_HELMET));
    });
    plugin.getServer().getScheduler().runTaskLater(plugin, mob::remove, 600L);
  }

  private void tagMob(Entity entity, Arena arena, TeamSide team) {
    PersistentDataContainer pdc = entity.getPersistentDataContainer();
    pdc.set(mobArenaKey, PersistentDataType.STRING, arena.name());
    pdc.set(mobTeamKey, PersistentDataType.STRING, team.name());
  }

  private void activateBatwings(Player player, Arena arena) {
    player.getInventory().setChestplate(kits.batwingElytra());
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      if (!sameRunningArena(player, arena)) return;
      PlayerSession session = session(player);
      if (session != null && session.ctfClass() == CtfClass.VAMPIRE) {
        player.getInventory().setChestplate(kits.vampireRobe());
      }
    }, 140L);
  }

  private void clearTransientBlocks(Arena arena) {
    clearAbilityWater(arena);
    clearLeafWalkArena(arena);
  }

  public void recordDeath(Player victim, Player killer) {
    Arena arena = arenaFor(victim);
    if (arena == null || !isRunning(arena)) return;

    stats(victim.getUniqueId()).deaths++;
    if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) return;

    PlayerSession killerSession = session(killer);
    PlayerSession victimSession = session(victim);
    if (killerSession == null || victimSession == null) return;
    if (!killerSession.arenaName().equalsIgnoreCase(victimSession.arenaName())) return;
    if (killerSession.team() == victimSession.team()) return;
    stats(killer.getUniqueId()).kills++;
  }

  private MatchStats stats(UUID playerId) {
    return matchStats.computeIfAbsent(playerId, ignored -> new MatchStats());
  }

  private void clearMatchStats(Arena arena) {
    for (Map.Entry<UUID, PlayerSession> entry : sessions.entrySet()) {
      if (entry.getValue().arenaName().equalsIgnoreCase(arena.name())) {
        matchStats.remove(entry.getKey());
      }
    }
  }

  private void showPersonalResults(Player player) {
    MatchStats stats = this.stats(player.getUniqueId());
    player.sendMessage(Component.text(
        "Your match: " + stats.kills + " kills | " + stats.deaths + " deaths",
        NamedTextColor.GRAY
    ));
  }

  private void returnPlayerToBase(Player player, Arena arena) {
    PlayerSession session = session(player);
    if (session == null) return;
    restoreAssassinArmour(player);
    clearAbilityCooldowns(player);
    player.getInventory().clear();
    player.getInventory().setArmorContents(new ItemStack[4]);
    player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    player.setGameMode(GameMode.ADVENTURE);
    Location base = arena.base(session.team());
    if (base != null) player.teleport(base);
  }

  private void tickAssassinInvisibility(Player player) {
    boolean invisible = player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    if (invisible && !hiddenAssassinArmour.containsKey(player.getUniqueId())) {
      ItemStack[] armour = player.getInventory().getArmorContents();
      hiddenAssassinArmour.put(player.getUniqueId(), cloneArmour(armour));
      player.getInventory().setArmorContents(new ItemStack[4]);
      removeMaterial(player, Material.GLASS_BOTTLE);
    } else if (!invisible) {
      restoreAssassinArmour(player);
    }
  }

  private void restoreAssassinArmour(Player player) {
    ItemStack[] armour = hiddenAssassinArmour.remove(player.getUniqueId());
    if (armour != null) player.getInventory().setArmorContents(armour);
  }

  private static ItemStack[] cloneArmour(ItemStack[] source) {
    ItemStack[] clone = new ItemStack[source.length];
    for (int i = 0; i < source.length; i++) {
      clone[i] = source[i] == null ? null : source[i].clone();
    }
    return clone;
  }

  private static void removeMaterial(Player player, Material material) {
    for (int i = 0; i < player.getInventory().getSize(); i++) {
      ItemStack item = player.getInventory().getItem(i);
      if (item != null && item.getType() == material) {
        player.getInventory().setItem(i, null);
      }
    }
  }

  public boolean resetArena(Arena arena) {
    RuntimeArena runtime = runtime(arena);
    if (runtime.snapshot == null && snapshotFile(arena).exists()) {
      runtime.snapshot = loadSnapshot(snapshotFile(arena));
    }
    if (runtime.snapshot == null) return false;

    clearTransientBlocks(arena);
    restoreArena(runtime);
    clearTemporaryEntities(arena);
    deleteSnapshotFile(arena);
    return true;
  }

  private ArenaSnapshot captureArena(Arena arena) {
    World world = arena.world();
    Location a = arena.cornerA();
    Location b = arena.cornerB();
    if (world == null || a == null || b == null) return null;

    int minX = Math.min(a.getBlockX(), b.getBlockX());
    int minY = Math.min(a.getBlockY(), b.getBlockY());
    int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
    int maxX = Math.max(a.getBlockX(), b.getBlockX());
    int maxY = Math.max(a.getBlockY(), b.getBlockY());
    int maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

    long volume = (long) (maxX - minX + 1)
        * (maxY - minY + 1)
        * (maxZ - minZ + 1);
    if (volume > Integer.MAX_VALUE) {
      plugin.getLogger().severe("Arena '" + arena.name() + "' is too large to snapshot safely.");
      return null;
    }

    Map<String, Integer> paletteIds = new HashMap<>();
    List<BlockData> palette = new ArrayList<>();
    int[] blocks = new int[(int) volume];
    List<BlockState> tileStates = new ArrayList<>();
    List<ContainerSnapshot> containers = new ArrayList<>();
    int index = 0;

    for (int y = minY; y <= maxY; y++) {
      for (int z = minZ; z <= maxZ; z++) {
        for (int x = minX; x <= maxX; x++) {
          Block block = world.getBlockAt(x, y, z);
          BlockData data = block.getBlockData();
          String key = data.getAsString();
          Integer paletteIndex = paletteIds.get(key);
          if (paletteIndex == null) {
            paletteIndex = palette.size();
            paletteIds.put(key, paletteIndex);
            palette.add(data.clone());
          }
          blocks[index++] = paletteIndex;

          BlockState state = block.getState();
          if (state instanceof TileState) tileStates.add(state);
          if (state instanceof Container container) {
            containers.add(new ContainerSnapshot(
                x,
                y,
                z,
                cloneItems(container.getInventory().getContents())
            ));
          }
        }
      }
    }

    return new ArenaSnapshot(
        world,
        minX,
        minY,
        minZ,
        maxX,
        maxY,
        maxZ,
        palette,
        blocks,
        tileStates,
        containers
    );
  }

  private void restoreArena(RuntimeArena runtime) {
    ArenaSnapshot snapshot = runtime.snapshot;
    if (snapshot == null) return;

    int index = 0;
    for (int y = snapshot.minY; y <= snapshot.maxY; y++) {
      for (int z = snapshot.minZ; z <= snapshot.maxZ; z++) {
        for (int x = snapshot.minX; x <= snapshot.maxX; x++) {
          BlockData data = snapshot.palette.get(snapshot.blocks[index++]);
          snapshot.world.getBlockAt(x, y, z).setBlockData(data, false);
        }
      }
    }

    // Restore captured tile state when this snapshot was made in this JVM.
    for (BlockState state : snapshot.tileStates) state.update(true, false);

    // Container contents are also serialized to disk so crash recovery keeps chests,
    // barrels, etc. intact after a hard restart.
    for (ContainerSnapshot saved : snapshot.containers) {
      BlockState state = snapshot.world.getBlockAt(saved.x, saved.y, saved.z).getState();
      if (state instanceof Container container) {
        container.getInventory().setContents(cloneItems(saved.contents));
        container.update(true, false);
      }
    }
  }

  private boolean persistSnapshot(Arena arena, ArenaSnapshot snapshot) {
    File file = snapshotFile(arena);
    File directory = file.getParentFile();
    if (!directory.exists() && !directory.mkdirs()) {
      plugin.getLogger().severe("Could not create CTF snapshot directory: " + directory);
      return false;
    }

    File temporary = new File(directory, file.getName() + ".tmp");
    try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(
        new GZIPOutputStream(new FileOutputStream(temporary))
    )) {
      out.writeInt(SNAPSHOT_MAGIC);
      out.writeInt(SNAPSHOT_VERSION);
      out.writeUTF(snapshot.world.getUID().toString());
      out.writeUTF(snapshot.world.getName());
      out.writeInt(snapshot.minX);
      out.writeInt(snapshot.minY);
      out.writeInt(snapshot.minZ);
      out.writeInt(snapshot.maxX);
      out.writeInt(snapshot.maxY);
      out.writeInt(snapshot.maxZ);

      out.writeInt(snapshot.palette.size());
      for (BlockData data : snapshot.palette) out.writeUTF(data.getAsString());

      out.writeInt(snapshot.blocks.length);
      for (int block : snapshot.blocks) out.writeInt(block);

      out.writeInt(snapshot.containers.size());
      for (ContainerSnapshot container : snapshot.containers) {
        out.writeInt(container.x);
        out.writeInt(container.y);
        out.writeInt(container.z);
        out.writeObject(container.contents);
      }
    } catch (IOException exception) {
      plugin.getLogger().severe(
          "Could not write pre-game snapshot for '" + arena.name() + "': " + exception.getMessage()
      );
      temporary.delete();
      return false;
    }

    try {
      try {
        Files.move(
            temporary.toPath(),
            file.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        );
      } catch (IOException atomicFailure) {
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
      return true;
    } catch (IOException exception) {
      plugin.getLogger().severe(
          "Could not install pre-game snapshot for '" + arena.name() + "': " + exception.getMessage()
      );
      temporary.delete();
      return false;
    }
  }

  private ArenaSnapshot loadSnapshot(File file) {
    try (BukkitObjectInputStream in = new BukkitObjectInputStream(
        new GZIPInputStream(new FileInputStream(file))
    )) {
      if (in.readInt() != SNAPSHOT_MAGIC) throw new IOException("invalid snapshot header");
      int version = in.readInt();
      if (version != SNAPSHOT_VERSION) throw new IOException("unsupported snapshot version " + version);

      UUID worldId = UUID.fromString(in.readUTF());
      String worldName = in.readUTF();
      World world = Bukkit.getWorld(worldId);
      if (world == null) world = Bukkit.getWorld(worldName);
      if (world == null) throw new IOException("snapshot world is not loaded: " + worldName);

      int minX = in.readInt();
      int minY = in.readInt();
      int minZ = in.readInt();
      int maxX = in.readInt();
      int maxY = in.readInt();
      int maxZ = in.readInt();

      int paletteSize = in.readInt();
      List<BlockData> palette = new ArrayList<>(paletteSize);
      for (int i = 0; i < paletteSize; i++) palette.add(Bukkit.createBlockData(in.readUTF()));

      int blockCount = in.readInt();
      int[] blocks = new int[blockCount];
      for (int i = 0; i < blockCount; i++) blocks[i] = in.readInt();

      int containerCount = in.readInt();
      List<ContainerSnapshot> containers = new ArrayList<>(containerCount);
      for (int i = 0; i < containerCount; i++) {
        int x = in.readInt();
        int y = in.readInt();
        int z = in.readInt();
        Object raw = in.readObject();
        if (!(raw instanceof ItemStack[] contents)) {
          throw new IOException("invalid container data in snapshot");
        }
        containers.add(new ContainerSnapshot(x, y, z, cloneItems(contents)));
      }

      return new ArenaSnapshot(
          world,
          minX,
          minY,
          minZ,
          maxX,
          maxY,
          maxZ,
          palette,
          blocks,
          List.of(),
          containers
      );
    } catch (IOException | ClassNotFoundException | IllegalArgumentException exception) {
      plugin.getLogger().severe(
          "Could not read CTF snapshot '" + file.getName() + "': " + exception.getMessage()
      );
      return null;
    }
  }

  private File snapshotFile(Arena arena) {
    String safeName = arena.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    return new File(new File(plugin.getDataFolder(), "snapshots"), safeName + ".ctfsnap.gz");
  }

  private void deleteSnapshotFile(Arena arena) {
    File file = snapshotFile(arena);
    try {
      Files.deleteIfExists(file.toPath());
    } catch (IOException exception) {
      plugin.getLogger().warning(
          "Could not delete completed snapshot for '" + arena.name() + "': " + exception.getMessage()
      );
    }
  }

  private static ItemStack[] cloneItems(ItemStack[] source) {
    ItemStack[] clone = new ItemStack[source.length];
    for (int i = 0; i < source.length; i++) {
      clone[i] = source[i] == null ? null : source[i].clone();
    }
    return clone;
  }

  private static BlockKey blockKey(Block block) {
    return new BlockKey(
        block.getWorld().getUID(),
        block.getX(),
        block.getY(),
        block.getZ()
    );
  }

  private static Block blockForKey(BlockKey key) {
    World world = Bukkit.getWorld(key.worldId);
    return world == null ? null : world.getBlockAt(key.x, key.y, key.z);
  }

  private void clearTemporaryEntities(Arena arena) {
    World world = arena.world();
    if (world == null) return;
    for (Entity entity : new ArrayList<>(world.getEntities())) {
      String arenaName = entity.getPersistentDataContainer().get(mobArenaKey, PersistentDataType.STRING);
      if (arena.name().equalsIgnoreCase(arenaName)) entity.remove();
    }
  }

  private RuntimeArena runtime(Arena arena) {
    return runtimes.computeIfAbsent(
        arena.name().toLowerCase(Locale.ROOT),
        ignored -> new RuntimeArena(arena)
    );
  }

  private void updateBossbars(RuntimeArena runtime) {
    float redProgress = Math.max(
        0.0f,
        Math.min(1.0f, runtime.redSeconds / (float) runtime.arena.winSeconds())
    );
    float blueProgress = Math.max(
        0.0f,
        Math.min(1.0f, runtime.blueSeconds / (float) runtime.arena.winSeconds())
    );
    runtime.redBar.progress(redProgress);
    runtime.blueBar.progress(blueProgress);
    runtime.redBar.name(Component.text(
        "Red Team Countdown: " + Math.max(0, runtime.redSeconds) + "s",
        NamedTextColor.RED
    ));
    runtime.blueBar.name(Component.text(
        "Blue Team Countdown: " + Math.max(0, runtime.blueSeconds) + "s",
        NamedTextColor.BLUE
    ));
  }

  private void showBossbars(Player player, RuntimeArena runtime) {
    player.showBossBar(runtime.redBar);
    player.showBossBar(runtime.blueBar);
  }

  private void hideBossbars(Player player, RuntimeArena runtime) {
    player.hideBossBar(runtime.redBar);
    player.hideBossBar(runtime.blueBar);
  }

  private void hideBossbars(Arena arena) {
    RuntimeArena runtime = runtime(arena);
    for (Player player : players(arena)) hideBossbars(player, runtime);
  }

  private static void showTitle(Player player, Component title) {
    player.showTitle(Title.title(title, Component.empty()));
  }

  private boolean sameRunningArena(Player player, Arena arena) {
    PlayerSession session = session(player);
    return session != null
        && session.arenaName().equalsIgnoreCase(arena.name())
        && isRunning(arena)
        && player.isOnline();
  }

  private static final class ArenaSnapshot {
    private final World world;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final List<BlockData> palette;
    private final int[] blocks;
    private final List<BlockState> tileStates;
    private final List<ContainerSnapshot> containers;

    private ArenaSnapshot(
        World world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        List<BlockData> palette,
        int[] blocks,
        List<BlockState> tileStates,
        List<ContainerSnapshot> containers
    ) {
      this.world = world;
      this.minX = minX;
      this.minY = minY;
      this.minZ = minZ;
      this.maxX = maxX;
      this.maxY = maxY;
      this.maxZ = maxZ;
      this.palette = List.copyOf(palette);
      this.blocks = blocks;
      this.tileStates = List.copyOf(tileStates);
      this.containers = List.copyOf(containers);
    }
  }

  private record BlockKey(UUID worldId, int x, int y, int z) { }

  private static final class WaterCell {
    private final String arenaName;
    private final BlockData original;
    private final Set<UUID> owners = new HashSet<>();

    private WaterCell(String arenaName, BlockData original) {
      this.arenaName = arenaName;
      this.original = original;
    }
  }

  private static final class LeafCell {
    private final String arenaName;
    private int references = 1;

    private LeafCell(String arenaName) {
      this.arenaName = arenaName;
    }
  }

  private static final class MatchStats {
    private int kills;
    private int deaths;
  }

  private static final class ContainerSnapshot {
    private final int x;
    private final int y;
    private final int z;
    private final ItemStack[] contents;

    private ContainerSnapshot(int x, int y, int z, ItemStack[] contents) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.contents = contents;
    }
  }

  private static final class RuntimeArena {
    private final Arena arena;
    private ArenaState state = ArenaState.WAITING;
    private int redSeconds;
    private int blueSeconds;
    private final BossBar redBar;
    private final BossBar blueBar;
    private ArenaSnapshot snapshot;

    private RuntimeArena(Arena arena) {
      this.arena = arena;
      this.redSeconds = arena.winSeconds();
      this.blueSeconds = arena.winSeconds();
      this.redBar = BossBar.bossBar(
          Component.text("Red Team Countdown", NamedTextColor.RED),
          1.0f,
          BossBar.Color.RED,
          BossBar.Overlay.NOTCHED_20
      );
      this.blueBar = BossBar.bossBar(
          Component.text("Blue Team Countdown", NamedTextColor.BLUE),
          1.0f,
          BossBar.Color.BLUE,
          BossBar.Overlay.NOTCHED_20
      );
    }
  }
}
