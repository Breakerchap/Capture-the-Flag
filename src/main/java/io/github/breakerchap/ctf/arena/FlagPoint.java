package io.github.breakerchap.ctf.arena;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class FlagPoint {
  private final int x;
  private final int y;
  private final int z;
  private final int size;

  public FlagPoint(int x, int y, int z, int size) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.size = Math.max(1, Math.min(2, size));
  }

  public int x() {
    return x;
  }

  public int y() {
    return y;
  }

  public int z() {
    return z;
  }

  public int size() {
    return size;
  }

  public List<Block> blocks(World world) {
    List<Block> blocks = new ArrayList<>(size * size);
    for (int dx = 0; dx < size; dx++) {
      for (int dz = 0; dz < size; dz++) {
        blocks.add(world.getBlockAt(x + dx, y, z + dz));
      }
    }
    return blocks;
  }

  public boolean contains(Location location) {
    if (location == null) return false;
    int bx = location.getBlockX();
    int by = location.getBlockY();
    int bz = location.getBlockZ();
    return by == y && bx >= x && bx < x + size && bz >= z && bz < z + size;
  }

  public double distanceSquared(Location location) {
    double cx = x + (size / 2.0);
    double cz = z + (size / 2.0);
    double dx = location.getX() - cx;
    double dy = location.getY() - y;
    double dz = location.getZ() - cz;
    return dx * dx + dy * dy + dz * dz;
  }
}
