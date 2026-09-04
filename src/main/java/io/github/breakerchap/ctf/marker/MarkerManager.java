package io.github.breakerchap.ctf.marker;

import io.github.breakerchap.ctf.CaptureTheFlagPlugin;
import io.github.breakerchap.ctf.arena.Arena;
import io.github.breakerchap.ctf.arena.ArenaManager;
import io.github.breakerchap.ctf.arena.FlagPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class MarkerManager implements Listener {
  private final CaptureTheFlagPlugin plugin;
  private final ArenaManager arenas;
  private final NamespacedKey itemTypeKey;
  private final NamespacedKey itemArenaKey;
  private final NamespacedKey entityMarkerKey;
  private final NamespacedKey entityArenaKey;

  public MarkerManager(CaptureTheFlagPlugin plugin, ArenaManager arenas) {
    this.plugin = plugin;
    this.arenas = arenas;
    this.itemTypeKey = new NamespacedKey(plugin, "marker_type");
    this.itemArenaKey = new NamespacedKey(plugin, "marker_arena");
    this.entityMarkerKey = new NamespacedKey(plugin, "setup_marker");
    this.entityArenaKey = new NamespacedKey(plugin, "setup_marker_arena");
  }

  public void giveMarkerKit(Player player, Arena arena) {
    for (MarkerType type : MarkerType.values()) {
      player.getInventory().addItem(markerItem(arena, type));
    }
    player.sendMessage(Component.text("Marker kit for " + arena.name() + " added to your inventory.", NamedTextColor.GREEN));
    player.sendMessage(Component.text(
        "Place a marker item where that point belongs. Marker blocks are not actually placed; a labelled setup marker is created instead.",
        NamedTextColor.GRAY
    ));
  }

  public ItemStack markerItem(Arena arena, MarkerType type) {
    ItemStack item = new ItemStack(type.material());
    ItemMeta meta = item.getItemMeta();
    meta.displayName(type.component().append(Component.text(" Marker", NamedTextColor.WHITE)));
    meta.lore(List.of(
        Component.text("Arena: " + arena.name(), NamedTextColor.GRAY),
        Component.text("Place to set this point", NamedTextColor.DARK_GRAY)
    ));
    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    PersistentDataContainer pdc = meta.getPersistentDataContainer();
    pdc.set(itemTypeKey, PersistentDataType.STRING, type.name());
    pdc.set(itemArenaKey, PersistentDataType.STRING, arena.name());
    item.setItemMeta(meta);
    return item;
  }

  @EventHandler(ignoreCancelled = true)
  public void onMarkerPlace(BlockPlaceEvent event) {
    ItemStack item = event.getItemInHand();
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return;
    PersistentDataContainer pdc = meta.getPersistentDataContainer();
    String typeName = pdc.get(itemTypeKey, PersistentDataType.STRING);
    String arenaName = pdc.get(itemArenaKey, PersistentDataType.STRING);
    if (typeName == null || arenaName == null) return;

    event.setCancelled(true);
    Player player = event.getPlayer();
    if (!player.hasPermission("ctf.admin")) {
      player.sendMessage(Component.text("You do not have permission to place CTF setup markers.", NamedTextColor.RED));
      return;
    }

    Arena arena = arenas.get(arenaName);
    if (arena == null) {
      player.sendMessage(Component.text("That arena no longer exists.", NamedTextColor.RED));
      return;
    }
    if (!player.getWorld().equals(arena.world())) {
      player.sendMessage(Component.text(
          "This marker belongs to arena '" + arena.name() + "' in world '" + arena.worldName() + "'.",
          NamedTextColor.RED
      ));
      return;
    }

    MarkerType type;
    try {
      type = MarkerType.valueOf(typeName);
    } catch (IllegalArgumentException exception) {
      return;
    }

    Location blockLocation = event.getBlockPlaced().getLocation();
    Location precise = blockLocation.clone().add(0.5, 0.0, 0.5);
    precise.setYaw(player.getLocation().getYaw());
    precise.setPitch(0.0f);

    switch (type) {
      case CORNER_A -> arena.setCornerA(blockLocation);
      case CORNER_B -> arena.setCornerB(blockLocation);
      case RED_BASE -> arena.setRedBase(precise);
      case BLUE_BASE -> arena.setBlueBase(precise);
      case SPECTATOR -> arena.setSpectator(precise);
      case FLAG_SINGLE -> arena.addFlag(new FlagPoint(
          blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ(), 1
      ));
      case FLAG_QUAD -> arena.addFlag(new FlagPoint(
          blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ(), 2
      ));
    }

    arenas.save();
    showMarkers(arena);
    player.sendMessage(Component.text(type.label() + " set for arena " + arena.name() + ".", type.colour()));
  }

  public void showMarkers(Arena arena) {
    hideMarkers(arena);
    if (arena.world() == null) return;
    addMarker(arena, arena.cornerA(), MarkerType.CORNER_A, null);
    addMarker(arena, arena.cornerB(), MarkerType.CORNER_B, null);
    addMarker(arena, arena.redBase(), MarkerType.RED_BASE, null);
    addMarker(arena, arena.blueBase(), MarkerType.BLUE_BASE, null);
    addMarker(arena, arena.spectator(), MarkerType.SPECTATOR, null);
    int index = 1;
    for (FlagPoint flag : arena.flags()) {
      Location location = new Location(
          arena.world(),
          flag.x() + flag.size() / 2.0,
          flag.y() + 0.3,
          flag.z() + flag.size() / 2.0
      );
      addMarker(
          arena,
          location,
          flag.size() == 1 ? MarkerType.FLAG_SINGLE : MarkerType.FLAG_QUAD,
          " #" + index++
      );
    }
  }

  public void hideMarkers(Arena arena) {
    if (arena.world() == null) return;
    String wanted = arena.name().toLowerCase(Locale.ROOT);
    for (Entity entity : new ArrayList<>(arena.world().getEntities())) {
      PersistentDataContainer pdc = entity.getPersistentDataContainer();
      Byte marker = pdc.get(entityMarkerKey, PersistentDataType.BYTE);
      String arenaName = pdc.get(entityArenaKey, PersistentDataType.STRING);
      if (marker != null
          && marker == (byte) 1
          && arenaName != null
          && arenaName.toLowerCase(Locale.ROOT).equals(wanted)) {
        entity.remove();
      }
    }
  }

  public void removeAllMarkerEntities() {
    for (Arena arena : arenas.all()) hideMarkers(arena);
  }

  private void addMarker(Arena arena, Location location, MarkerType type, String suffix) {
    if (location == null || location.getWorld() == null) return;
    Location standLocation = location.clone().add(0.0, 0.3, 0.0);
    ArmorStand stand = location.getWorld().spawn(standLocation, ArmorStand.class, entity -> {
      entity.setInvisible(true);
      entity.setGravity(false);
      entity.setMarker(true);
      entity.setInvulnerable(true);
      entity.setSilent(true);
      entity.setPersistent(false);
      entity.setCustomNameVisible(true);
      entity.customName(type.component().append(Component.text(
          suffix == null ? "" : suffix,
          NamedTextColor.WHITE
      )));
      entity.getPersistentDataContainer().set(entityMarkerKey, PersistentDataType.BYTE, (byte) 1);
      entity.getPersistentDataContainer().set(entityArenaKey, PersistentDataType.STRING, arena.name());
    });
    stand.setGlowing(true);
  }
}
