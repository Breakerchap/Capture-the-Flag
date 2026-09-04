package io.github.breakerchap.ctf.arena;

import io.github.breakerchap.ctf.player.TeamSide;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public final class Arena {
  private final String name;
  private UUID worldId;
  private String worldName;
  private Location cornerA;
  private Location cornerB;
  private Location redBase;
  private Location blueBase;
  private Location spectator;
  private final List<FlagPoint> flags = new ArrayList<>();
  private int requiredFlags = 4;
  private int winSeconds = 60;
  private Material neutralFlagMaterial = Material.GLASS;

  public Arena(String name, World world) {
    this.name = name;
    setWorld(world);
  }

  public String name() { return name; }
  public UUID worldId() { return worldId; }
  public String worldName() { return worldName; }
  public Location cornerA() { return cloneOrNull(cornerA); }
  public Location cornerB() { return cloneOrNull(cornerB); }
  public Location redBase() { return cloneOrNull(redBase); }
  public Location blueBase() { return cloneOrNull(blueBase); }
  public Location spectator() { return cloneOrNull(spectator); }
  public List<FlagPoint> flags() { return Collections.unmodifiableList(flags); }
  public int requiredFlags() { return requiredFlags; }
  public int winSeconds() { return winSeconds; }
  public Material neutralFlagMaterial() { return neutralFlagMaterial; }

  public void setWorld(World world) {
    this.worldId = world.getUID();
    this.worldName = world.getName();
  }

  public World world() {
    World byId = worldId == null ? null : Bukkit.getWorld(worldId);
    return byId != null ? byId : Bukkit.getWorld(worldName);
  }

  public void setCornerA(Location cornerA) { this.cornerA = blockLocation(cornerA); }
  public void setCornerB(Location cornerB) { this.cornerB = blockLocation(cornerB); }
  public void setRedBase(Location redBase) { this.redBase = cloneOrNull(redBase); }
  public void setBlueBase(Location blueBase) { this.blueBase = cloneOrNull(blueBase); }
  public void setSpectator(Location spectator) { this.spectator = cloneOrNull(spectator); }
  public void setRequiredFlags(int requiredFlags) { this.requiredFlags = Math.max(1, requiredFlags); }
  public void setWinSeconds(int winSeconds) { this.winSeconds = Math.max(1, winSeconds); }

  public void setNeutralFlagMaterial(Material material) {
    this.neutralFlagMaterial = material == null ? Material.GLASS : material;
  }

  public void addFlag(FlagPoint flag) {
    for (FlagPoint existing : flags) {
      if (existing.x() == flag.x() && existing.y() == flag.y() && existing.z() == flag.z()) return;
    }
    flags.add(flag);
  }

  public boolean removeNearestFlag(Location location, double maxDistance) {
    FlagPoint nearest = null;
    double best = maxDistance * maxDistance;
    for (FlagPoint flag : flags) {
      double distance = flag.distanceSquared(location);
      if (distance <= best) {
        best = distance;
        nearest = flag;
      }
    }
    return nearest != null && flags.remove(nearest);
  }

  public void clearFlags() {
    flags.clear();
  }

  public boolean contains(Location location) {
    if (location == null || world() == null || !world().equals(location.getWorld())) return false;
    if (cornerA == null || cornerB == null) return false;
    int minX = Math.min(cornerA.getBlockX(), cornerB.getBlockX());
    int minY = Math.min(cornerA.getBlockY(), cornerB.getBlockY());
    int minZ = Math.min(cornerA.getBlockZ(), cornerB.getBlockZ());
    int maxX = Math.max(cornerA.getBlockX(), cornerB.getBlockX());
    int maxY = Math.max(cornerA.getBlockY(), cornerB.getBlockY());
    int maxZ = Math.max(cornerA.getBlockZ(), cornerB.getBlockZ());
    return location.getX() >= minX && location.getX() <= maxX + 1
        && location.getY() >= minY && location.getY() <= maxY + 1
        && location.getZ() >= minZ && location.getZ() <= maxZ + 1;
  }

  public boolean isFlagCell(Location location) {
    for (FlagPoint flag : flags) {
      if (flag.contains(location)) return true;
    }
    return false;
  }

  public Location base(TeamSide team) {
    return team == TeamSide.RED ? redBase() : blueBase();
  }

  public Location spectatorOrCentre() {
    if (spectator != null) return spectator();
    World world = world();
    if (world == null || cornerA == null || cornerB == null) return null;
    double x = (cornerA.getBlockX() + cornerB.getBlockX()) / 2.0 + 0.5;
    double y = Math.max(cornerA.getBlockY(), cornerB.getBlockY()) + 5.0;
    double z = (cornerA.getBlockZ() + cornerB.getBlockZ()) / 2.0 + 0.5;
    return new Location(world, x, y, z);
  }

  public List<String> validationErrors() {
    List<String> errors = new ArrayList<>();
    World world = world();
    if (world == null) errors.add("world is not loaded");
    if (cornerA == null || cornerB == null) errors.add("both arena corners are required");
    if (redBase == null) errors.add("red base is missing");
    if (blueBase == null) errors.add("blue base is missing");
    if (flags.isEmpty()) errors.add("at least one flag point is required");
    if (requiredFlags > flags.size()) errors.add("required-flags is greater than the number of flag points");
    if (world != null && cornerA != null && cornerB != null) {
      if (redBase != null && !contains(redBase)) errors.add("red base is outside the arena bounds");
      if (blueBase != null && !contains(blueBase)) errors.add("blue base is outside the arena bounds");
      if (spectator != null && !contains(spectator)) errors.add("spectator point is outside the arena bounds");
      for (FlagPoint flag : flags) {
        Location flagLocation = new Location(world, flag.x(), flag.y(), flag.z());
        if (!contains(flagLocation)) {
          errors.add("a flag point is outside the arena bounds");
          break;
        }
      }
    }
    return errors;
  }

  private static Location blockLocation(Location source) {
    if (source == null) return null;
    return new Location(source.getWorld(), source.getBlockX(), source.getBlockY(), source.getBlockZ());
  }

  private static Location cloneOrNull(Location location) {
    return location == null ? null : location.clone();
  }
}
