package io.github.breakerchap.ctf.game;

import io.github.breakerchap.ctf.CaptureTheFlagPlugin;
import io.github.breakerchap.ctf.arena.Arena;
import io.github.breakerchap.ctf.arena.ArenaManager;
import io.github.breakerchap.ctf.kit.KitManager;
import io.github.breakerchap.ctf.player.CtfClass;
import io.github.breakerchap.ctf.player.PlayerSession;
import io.github.breakerchap.ctf.player.TeamSide;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class GameListener implements Listener {
  private final CaptureTheFlagPlugin plugin;
  private final ArenaManager arenas;
  private final GameManager games;
  private final KitManager kits;
  private final Set<Material> allowedBuildOn;
  private final Set<Material> breakable;

  public GameListener(CaptureTheFlagPlugin plugin, ArenaManager arenas, GameManager games, KitManager kits) {
    this.plugin = plugin;
    this.arenas = arenas;
    this.games = games;
    this.kits = kits;
    this.allowedBuildOn = materialSet(plugin.getConfig().getStringList("game.allowed-build-on"));
    this.breakable = materialSet(plugin.getConfig().getStringList("game.breakable-blocks"));
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    PlayerSession session = games.session(player);
    if (session == null) return;
    Arena arena = arenas.get(session.arenaName());
    if (arena == null || !games.isRunning(arena)) {
      event.setCancelled(true);
      return;
    }
    if (!arena.contains(event.getBlockPlaced().getLocation())) {
      event.setCancelled(true);
      return;
    }

    Material placed = event.getBlockPlaced().getType();
    TeamSide team = session.team();
    if (placed == team.buildMaterial()) {
      if (!allowedBuildOn.contains(event.getBlockAgainst().getType())) event.setCancelled(true);
      return;
    }
    if (placed == team.flagMaterial()) {
      if (!arena.isFlagCell(event.getBlockPlaced().getLocation())) event.setCancelled(true);
      return;
    }
    event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    PlayerSession session = games.session(player);
    if (session == null) return;
    Arena arena = arenas.get(session.arenaName());
    if (arena == null || !games.isRunning(arena) || !arena.contains(event.getBlock().getLocation())) {
      event.setCancelled(true);
      return;
    }

    Material type = event.getBlock().getType();
    if (!breakable.contains(type)) {
      event.setCancelled(true);
      return;
    }
    boolean flagLike = type == Material.GLASS || type == Material.RED_STAINED_GLASS || type == Material.BLUE_STAINED_GLASS;
    if (flagLike && !arena.isFlagCell(event.getBlock().getLocation())) {
      event.setCancelled(true);
      return;
    }
    event.setDropItems(false);
  }

  @EventHandler(ignoreCancelled = true)
  public void onInteract(PlayerInteractEvent event) {
    if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;
    ItemStack item = event.getItem();
    String ability = kits.ability(item);
    if (ability == null) return;
    if (ability.equals(KitManager.ASSASSIN_PEARL)
        || ability.equals(KitManager.WIND_BURST)
        || ability.equals(KitManager.WATER_BUCKET)) return;
    event.setCancelled(true);
    games.useAbility(event.getPlayer(), ability);
  }

  @EventHandler(ignoreCancelled = true)
  public void onProjectileLaunch(ProjectileLaunchEvent event) {
    Projectile projectile = event.getEntity();
    ProjectileSource source = projectile.getShooter();
    if (source instanceof Player player) {
      games.projectileUsed(player, projectile.getType().name());
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onWaterBucket(PlayerBucketEmptyEvent event) {
    Player player = event.getPlayer();
    PlayerSession session = games.session(player);
    if (session == null || session.ctfClass() != CtfClass.SWIMMER) return;
    Arena arena = arenas.get(session.arenaName());
    if (arena == null || !games.isRunning(arena)) return;
    Location water = event.getBlockClicked().getRelative(event.getBlockFace()).getLocation();
    if (!arena.contains(water)) {
      event.setCancelled(true);
      return;
    }
    games.waterBucketUsed(player, water);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onDamage(EntityDamageByEntityEvent event) {
    Entity rawAttacker = resolveAttacker(event.getDamager());
    Entity victim = event.getEntity();

    TeamContext attacker = context(rawAttacker);
    TeamContext target = context(victim);
    if (attacker != null && target != null
        && attacker.arena.name().equalsIgnoreCase(target.arena.name())
        && attacker.team == target.team) {
      event.setCancelled(true);
      return;
    }

    if (rawAttacker instanceof Player player) {
      PlayerSession session = games.session(player);
      if (session != null && session.ctfClass() == CtfClass.VAMPIRE
          && kits.isVampireTooth(player.getInventory().getItemInMainHand())) {
        Arena arena = arenas.get(session.arenaName());
        if (arena != null && games.isRunning(arena)) {
          player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, 2, false, false));
        }
      }
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onTarget(EntityTargetLivingEntityEvent event) {
    TeamSide mobTeam = games.mobTeam(event.getEntity());
    Arena mobArena = games.mobArena(event.getEntity());
    if (mobTeam == null || mobArena == null || !(event.getTarget() instanceof Player target)) return;
    PlayerSession targetSession = games.session(target);
    if (targetSession != null
        && targetSession.team() == mobTeam
        && targetSession.arenaName().equalsIgnoreCase(mobArena.name())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    Arena arena = games.arenaFor(player);
    if (arena == null || !games.isRunning(arena)) return;
    event.getDrops().clear();
    event.setDroppedExp(0);
    event.setKeepInventory(false);
    event.setKeepLevel(true);
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent event) {
    Player player = event.getPlayer();
    Arena arena = games.arenaFor(player);
    if (arena == null || !games.isRunning(arena)) return;
    Location spectator = arena.spectatorOrCentre();
    if (spectator != null) event.setRespawnLocation(spectator);
    plugin.getServer().getScheduler().runTask(plugin, () -> games.respawnAfterDelay(player));
  }

  @EventHandler(ignoreCancelled = true)
  public void onDrop(PlayerDropItemEvent event) {
    Arena arena = games.arenaFor(event.getPlayer());
    if (arena != null && games.isRunning(arena)) event.setCancelled(true);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    if (games.session(event.getPlayer()) != null) games.leave(event.getPlayer());
  }

  private Entity resolveAttacker(Entity entity) {
    if (entity instanceof Projectile projectile) {
      ProjectileSource shooter = projectile.getShooter();
      if (shooter instanceof Entity shooterEntity) return shooterEntity;
    }
    return entity;
  }

  private TeamContext context(Entity entity) {
    if (entity instanceof Player player) {
      PlayerSession session = games.session(player);
      if (session == null) return null;
      Arena arena = arenas.get(session.arenaName());
      if (arena == null) return null;
      return new TeamContext(arena, session.team());
    }
    Arena arena = games.mobArena(entity);
    TeamSide team = games.mobTeam(entity);
    return arena == null || team == null ? null : new TeamContext(arena, team);
  }

  private static Set<Material> materialSet(List<String> values) {
    Set<Material> materials = new HashSet<>();
    for (String value : values) {
      Material material = Material.matchMaterial(value);
      if (material != null) materials.add(material);
    }
    return materials;
  }

  private static final class TeamContext {
    private final Arena arena;
    private final TeamSide team;

    private TeamContext(Arena arena, TeamSide team) {
      this.arena = arena;
      this.team = team;
    }
  }
}
