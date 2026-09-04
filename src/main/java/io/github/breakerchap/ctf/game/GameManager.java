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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
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

public final class GameManager {
  private final CaptureTheFlagPlugin plugin;
  private final ArenaManager arenas;
  private final MarkerManager markers;
  private final KitManager kits;
  private final Map<UUID, PlayerSession> sessions = new HashMap<>();
  private final Map<String, RuntimeArena> runtimes = new HashMap<>();
  private final Map<UUID, Map<String, Integer>> cooldowns = new HashMap<>();
  private final Map<UUID, ItemStack[]> hiddenAssassinArmour = new HashMap<>();
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

  public void shutdown() {
    if (ticker != null) ticker.cancel();
    for (RuntimeArena runtime : runtimes.values()) hideBossbars(runtime.arena);
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
    runtime.state = ArenaState.WAITING;
    runtime.redSeconds = arena.winSeconds();
    runtime.blueSeconds = arena.winSeconds();
    resetFlags(arena);
    hideBossbars(arena);

    for (Player player : players(arena)) {
      cooldowns.remove(player.getUniqueId());
      restoreAssassinArmour(player);
      player.getInventory().clear();
      player.getInventory().setArmorContents(new ItemStack[4]);
      player.setGameMode(GameMode.ADVENTURE);
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
    if (arena == null || !isRunning(arena) || cooldown(player, ability) > 0) return false;

    removeAbility(player, ability);
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
      case KitManager.LEAF_WALK -> startLeafWalk(player, arena);
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

    if (session.ctfClass() == CtfClass.ASSASSIN && "ENDER_PEARL".equals(entityTypeName)) {
      removeAbility(player, KitManager.ASSASSIN_PEARL);
      setCooldown(player, KitManager.ASSASSIN_PEARL, 400);
    } else if (session.ctfClass() == CtfClass.MACE_BEARER && "WIND_CHARGE".equals(entityTypeName)) {
      removeAbility(player, KitManager.WIND_BURST);
      setCooldown(player, KitManager.WIND_BURST, 200);
    }
  }

  public void waterBucketUsed(Player player, Location placedAt) {
    PlayerSession session = session(player);
    if (session == null || session.ctfClass() != CtfClass.SWIMMER) return;
    Arena arena = arenas.get(session.arenaName());
    if (arena == null || !isRunning(arena) || cooldown(player, KitManager.WATER_BUCKET) > 0) return;

    removeAbility(player, KitManager.WATER_BUCKET);
    setCooldown(player, KitManager.WATER_BUCKET, 800);
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> clearWater(arena, placedAt), 100L);
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
      if (ability != null && playerCooldowns.getOrDefault(ability, 0) <= 0) {
        ensureAbility(player, ability);
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
      restoreAssassinArmour(player);
      player.setGameMode(GameMode.SPECTATOR);
      player.getInventory().clear();
      player.getInventory().setArmorContents(new ItemStack[4]);
      cooldowns.remove(player.getUniqueId());
    }

    hideBossbars(runtime.arena);
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      resetFlags(runtime.arena);
      runtime.redSeconds = runtime.arena.winSeconds();
      runtime.blueSeconds = runtime.arena.winSeconds();
      runtime.state = ArenaState.WAITING;
    }, 60L);
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
    if (KitManager.WIND_BURST.equals(ability)) {
      player.getInventory().setItemInOffHand(item);
    } else {
      player.getInventory().setItem(8, item);
    }
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
  }

  private void startLeafWalk(Player player, Arena arena) {
    Set<Block> placed = new HashSet<>();

    new BukkitRunnable() {
      private int age;

      @Override
      public void run() {
        PlayerSession current = session(player);
        if (!sameRunningArena(player, arena)
            || current == null
            || current.ctfClass() != CtfClass.HUNTER) {
          finishTrail();
          cancel();
          return;
        }

        int y = player.getLocation().getBlockY() - 1;
        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
          for (int dz = -1; dz <= 1; dz++) {
            Block block = arena.world().getBlockAt(x + dx, y, z + dz);
            if (arena.contains(block.getLocation()) && block.getType().isAir()) {
              block.setType(Material.DARK_OAK_LEAVES, false);
              placed.add(block);
            }
          }
        }

        age++;
        if (age >= 100) {
          setCooldown(player, KitManager.LEAF_WALK, 600);
          finishTrail();
          cancel();
        }
      }

      private void finishTrail() {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
          for (Block block : placed) {
            if (block.getType() == Material.DARK_OAK_LEAVES) {
              block.setType(Material.AIR, false);
            }
          }
        }, 60L);
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  private void raiseDead(Player player, Arena arena, TeamSide team) {
    Vector forward = player.getLocation().getDirection().setY(0);
    if (forward.lengthSquared() < 0.01) forward = new Vector(0, 0, 1);
    forward.normalize();
    Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
    Location origin = player.getLocation().clone().add(0, 1.0, 0);

    spawnZombie(arena, team, origin.clone().add(forward.clone().multiply(1.5)));
    spawnZombie(arena, team, origin.clone().subtract(forward.clone().multiply(1.5)));
    spawnSkeleton(arena, team, origin.clone().add(right.clone().multiply(1.5)));
    spawnSkeleton(arena, team, origin.clone().subtract(right.clone().multiply(1.5)));
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

  private void clearWater(Arena arena, Location source) {
    if (source == null
        || source.getWorld() == null
        || !source.getWorld().equals(arena.world())) return;

    int baseX = source.getBlockX();
    int baseY = source.getBlockY();
    int baseZ = source.getBlockZ();
    for (int x = baseX - 6; x <= baseX + 6; x++) {
      for (int y = baseY - 4; y <= baseY + 7; y++) {
        for (int z = baseZ - 6; z <= baseZ + 6; z++) {
          Block block = arena.world().getBlockAt(x, y, z);
          if (arena.contains(block.getLocation()) && block.getType() == Material.WATER) {
            block.setType(Material.AIR, false);
          }
        }
      }
    }
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

  private static final class RuntimeArena {
    private final Arena arena;
    private ArenaState state = ArenaState.WAITING;
    private int redSeconds;
    private int blueSeconds;
    private final BossBar redBar;
    private final BossBar blueBar;

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
