package io.github.breakerchap.ctf.player;

import org.bukkit.Material;

public enum TeamSide {
  RED("Red", Material.RED_TERRACOTTA, Material.RED_STAINED_GLASS),
  BLUE("Blue", Material.BLUE_TERRACOTTA, Material.BLUE_STAINED_GLASS);

  private final String displayName;
  private final Material buildMaterial;
  private final Material flagMaterial;

  TeamSide(String displayName, Material buildMaterial, Material flagMaterial) {
    this.displayName = displayName;
    this.buildMaterial = buildMaterial;
    this.flagMaterial = flagMaterial;
  }

  public String displayName() {
    return displayName;
  }

  public Material buildMaterial() {
    return buildMaterial;
  }

  public Material flagMaterial() {
    return flagMaterial;
  }

  public static TeamSide parse(String value) {
    if (value == null) return null;
    return switch (value.toLowerCase()) {
      case "red", "r" -> RED;
      case "blue", "b" -> BLUE;
      default -> null;
    };
  }
}
