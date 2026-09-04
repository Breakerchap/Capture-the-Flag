package io.github.breakerchap.ctf.marker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public enum MarkerType {
  CORNER_A(Material.LIME_CONCRETE, "Corner A", NamedTextColor.GREEN),
  CORNER_B(Material.YELLOW_CONCRETE, "Corner B", NamedTextColor.YELLOW),
  RED_BASE(Material.RED_CONCRETE, "Red Base", NamedTextColor.RED),
  BLUE_BASE(Material.BLUE_CONCRETE, "Blue Base", NamedTextColor.BLUE),
  SPECTATOR(Material.PURPLE_CONCRETE, "Spectator", NamedTextColor.LIGHT_PURPLE),
  FLAG_SINGLE(Material.BEACON, "Flag 1x1", NamedTextColor.GOLD),
  FLAG_QUAD(Material.SEA_LANTERN, "Flag 2x2", NamedTextColor.AQUA);

  private final Material material;
  private final String label;
  private final NamedTextColor colour;

  MarkerType(Material material, String label, NamedTextColor colour) {
    this.material = material;
    this.label = label;
    this.colour = colour;
  }

  public Material material() { return material; }
  public String label() { return label; }
  public NamedTextColor colour() { return colour; }
  public Component component() { return Component.text(label, colour); }
}
