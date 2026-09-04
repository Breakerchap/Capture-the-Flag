package io.github.breakerchap.ctf.arena;

import io.github.breakerchap.ctf.CaptureTheFlagPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ArenaManager {
  private final CaptureTheFlagPlugin plugin;
  private final Map<String, Arena> arenas = new LinkedHashMap<>();
  private final File file;

  public ArenaManager(CaptureTheFlagPlugin plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "arenas.yml");
  }

  public void load() {
    arenas.clear();
    if (!file.exists()) return;
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
    ConfigurationSection root = yaml.getConfigurationSection("arenas");
    if (root == null) return;

    for (String key : root.getKeys(false)) {
      ConfigurationSection section = root.getConfigurationSection(key);
      if (section == null) continue;
      String worldName = section.getString("world-name");
      World world = worldName == null ? null : Bukkit.getWorld(worldName);
      if (world == null) {
        plugin.getLogger().warning("Skipping arena '" + key + "': world '" + worldName + "' is not loaded.");
        continue;
      }

      Arena arena = new Arena(section.getString("name", key), world);
      arena.setCornerA(readLocation(section.getConfigurationSection("corner-a"), world));
      arena.setCornerB(readLocation(section.getConfigurationSection("corner-b"), world));
      arena.setRedBase(readLocation(section.getConfigurationSection("red-base"), world));
      arena.setBlueBase(readLocation(section.getConfigurationSection("blue-base"), world));
      arena.setSpectator(readLocation(section.getConfigurationSection("spectator"), world));
      arena.setRequiredFlags(section.getInt("required-flags", 4));
      arena.setWinSeconds(section.getInt("win-seconds", 60));
      Material neutral = Material.matchMaterial(section.getString("neutral-flag-material", "GLASS"));
      arena.setNeutralFlagMaterial(neutral == null ? Material.GLASS : neutral);

      List<?> rawFlags = section.getList("flags", List.of());
      for (Object raw : rawFlags) {
        if (!(raw instanceof Map<?, ?> map)) continue;
        int x = intValue(map.get("x"));
        int y = intValue(map.get("y"));
        int z = intValue(map.get("z"));
        int size = Math.max(1, Math.min(2, intValue(map.get("size"))));
        arena.addFlag(new FlagPoint(x, y, z, size));
      }
      arenas.put(normalise(arena.name()), arena);
    }
  }

  public void save() {
    if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
      plugin.getLogger().warning("Could not create plugin data directory.");
      return;
    }
    YamlConfiguration yaml = new YamlConfiguration();
    ConfigurationSection root = yaml.createSection("arenas");
    for (Arena arena : arenas.values()) {
      ConfigurationSection section = root.createSection(normalise(arena.name()));
      section.set("name", arena.name());
      section.set("world-name", arena.worldName());
      writeLocation(section, "corner-a", arena.cornerA());
      writeLocation(section, "corner-b", arena.cornerB());
      writeLocation(section, "red-base", arena.redBase());
      writeLocation(section, "blue-base", arena.blueBase());
      writeLocation(section, "spectator", arena.spectator());
      section.set("required-flags", arena.requiredFlags());
      section.set("win-seconds", arena.winSeconds());
      section.set("neutral-flag-material", arena.neutralFlagMaterial().name());
      List<Map<String, Object>> flags = new ArrayList<>();
      for (FlagPoint flag : arena.flags()) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("x", flag.x());
        entry.put("y", flag.y());
        entry.put("z", flag.z());
        entry.put("size", flag.size());
        flags.add(entry);
      }
      section.set("flags", flags);
    }
    try {
      yaml.save(file);
    } catch (IOException exception) {
      plugin.getLogger().severe("Could not save arenas.yml: " + exception.getMessage());
    }
  }

  public Arena create(String name, World world) {
    String key = normalise(name);
    if (arenas.containsKey(key)) return null;
    Arena arena = new Arena(name, world);
    arena.setNeutralFlagMaterial(Material.matchMaterial(plugin.getConfig().getString("game.neutral-flag-material", "GLASS")));
    arena.setRequiredFlags(plugin.getConfig().getInt("game.default-required-flags", 4));
    arena.setWinSeconds(plugin.getConfig().getInt("game.default-win-seconds", 60));
    arenas.put(key, arena);
    save();
    return arena;
  }

  public Arena get(String name) {
    return name == null ? null : arenas.get(normalise(name));
  }

  public boolean delete(String name) {
    boolean removed = arenas.remove(normalise(name)) != null;
    if (removed) save();
    return removed;
  }

  public Collection<Arena> all() {
    return List.copyOf(arenas.values());
  }

  public List<String> names() {
    return arenas.values().stream().map(Arena::name).toList();
  }

  private static String normalise(String value) {
    return value.toLowerCase(Locale.ROOT);
  }

  private static int intValue(Object value) {
    return value instanceof Number number ? number.intValue() : 0;
  }

  private static void writeLocation(ConfigurationSection parent, String key, Location location) {
    if (location == null) return;
    ConfigurationSection section = parent.createSection(key);
    section.set("x", location.getX());
    section.set("y", location.getY());
    section.set("z", location.getZ());
    section.set("yaw", location.getYaw());
    section.set("pitch", location.getPitch());
  }

  private static Location readLocation(ConfigurationSection section, World world) {
    if (section == null) return null;
    return new Location(
        world,
        section.getDouble("x"),
        section.getDouble("y"),
        section.getDouble("z"),
        (float) section.getDouble("yaw"),
        (float) section.getDouble("pitch")
    );
  }
}
