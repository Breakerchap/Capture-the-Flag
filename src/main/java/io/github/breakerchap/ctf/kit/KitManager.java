package io.github.breakerchap.ctf.kit;

import io.github.breakerchap.ctf.CaptureTheFlagPlugin;
import io.github.breakerchap.ctf.player.CtfClass;
import io.github.breakerchap.ctf.player.TeamSide;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class KitManager {
  public static final String GREAT_FEAST = "great_feast";
  public static final String LEAF_WALK = "leaf_walk";
  public static final String RAISE_DEAD = "raise_dead";
  public static final String BATWINGS = "batwings";
  public static final String WIND_BURST = "wind_burst";
  public static final String ASSASSIN_PEARL = "assassin_pearl";
  public static final String WATER_BUCKET = "water_bucket";

  private final CaptureTheFlagPlugin plugin;
  private final NamespacedKey abilityKey;
  private final NamespacedKey weaponKey;

  public KitManager(CaptureTheFlagPlugin plugin) {
    this.plugin = plugin;
    this.abilityKey = new NamespacedKey(plugin, "ability");
    this.weaponKey = new NamespacedKey(plugin, "weapon");
  }

  public void giveKit(Player player, TeamSide team, CtfClass ctfClass) {
    player.getInventory().clear();
    player.getInventory().setArmorContents(new ItemStack[4]);
    player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

    switch (ctfClass) {
      case ASSASSIN -> assassin(player);
      case CHEF -> chef(player);
      case HUNTER -> hunter(player);
      case MACE_BEARER -> maceBearer(player);
      case NECROMANCER -> necromancer(player);
      case PRO -> pro(player);
      case SWIMMER -> swimmer(player);
      case VAMPIRE -> vampire(player);
    }
    giveTeamBlocks(player, team);
  }

  public String ability(ItemStack item) {
    if (item == null || item.getType().isAir()) return null;
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return null;
    return meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING);
  }

  public boolean isVampireTooth(ItemStack item) {
    if (item == null || item.getType().isAir()) return false;
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return false;
    return "vampire_tooth".equals(meta.getPersistentDataContainer().get(weaponKey, PersistentDataType.STRING));
  }

  public ItemStack abilityItem(String ability) {
    return switch (ability) {
      case GREAT_FEAST -> special(Material.SUSPICIOUS_STEW, "Great Feast", NamedTextColor.GOLD, ability);
      case LEAF_WALK -> special(Material.DARK_OAK_LEAVES, "Leaf Walk", TextColor.color(0x00CC00), ability);
      case RAISE_DEAD -> special(Material.SOUL_LANTERN, "Raise Dead", NamedTextColor.DARK_PURPLE, ability);
      case BATWINGS -> special(Material.FIREWORK_STAR, "Batwings", NamedTextColor.DARK_RED, ability);
      case WIND_BURST -> special(Material.WIND_CHARGE, "Windburst", NamedTextColor.BLUE, ability);
      case ASSASSIN_PEARL -> special(Material.ENDER_PEARL, "Assassin Pearl", NamedTextColor.DARK_GRAY, ability);
      case WATER_BUCKET -> special(Material.WATER_BUCKET, "Water Bucket", NamedTextColor.AQUA, ability);
      default -> throw new IllegalArgumentException("Unknown ability " + ability);
    };
  }

  private void assassin(Player player) {
    player.getInventory().setHelmet(leather(
        Material.LEATHER_HELMET, "Assassin's Cap", NamedTextColor.DARK_GRAY,
        Color.fromRGB(0x1D1D21), Enchantment.PROTECTION, 0
    ));
    player.getInventory().setChestplate(leather(
        Material.LEATHER_CHESTPLATE, "Assassin's Tunic", NamedTextColor.DARK_GRAY,
        Color.fromRGB(0x1D1D21), Enchantment.PROTECTION, 1
    ));
    player.getInventory().setLeggings(leather(
        Material.LEATHER_LEGGINGS, "Assassin's Leggings", NamedTextColor.DARK_GRAY,
        Color.fromRGB(0x1D1D21), Enchantment.PROTECTION, 1
    ));
    player.getInventory().setBoots(leather(
        Material.LEATHER_BOOTS, "Assassin's Boots", NamedTextColor.DARK_GRAY,
        Color.fromRGB(0x1D1D21), Enchantment.PROTECTION, 0
    ));
    player.getInventory().addItem(namedEnchanted(
        Material.WOODEN_SWORD, "Assassin's Dagger", NamedTextColor.DARK_GRAY, Enchantment.SHARPNESS, 3
    ));
    player.getInventory().addItem(tool(Material.STONE_PICKAXE, Enchantment.EFFICIENCY, 2));
    player.getInventory().addItem(new ItemStack(Material.DRIED_KELP, 64));
    player.getInventory().addItem(customPotion(
        Material.POTION,
        "Assassin's Syrup",
        NamedTextColor.DARK_GRAY,
        List.of(
            new PotionEffect(PotionEffectType.INVISIBILITY, 300, 0, false, false),
            new PotionEffect(PotionEffectType.SPEED, 300, 2, false, false),
            new PotionEffect(PotionEffectType.JUMP_BOOST, 300, 1, false, false)
        )
    ));
  }

  private void chef(Player player) {
    player.getInventory().setHelmet(leather(
        Material.LEATHER_HELMET, "Chef's Hat", NamedTextColor.GOLD,
        Color.fromRGB(0xFAFAFA), Enchantment.PROTECTION, 2
    ));
    player.getInventory().setChestplate(leather(
        Material.LEATHER_CHESTPLATE, "Chef's Apron", NamedTextColor.GOLD,
        Color.fromRGB(0xF0F0F0), Enchantment.PROTECTION, 2
    ));
    player.getInventory().setLeggings(leather(
        Material.LEATHER_LEGGINGS, "Chef's Chaps", NamedTextColor.GOLD,
        Color.fromRGB(0xF0F0F0), Enchantment.PROTECTION, 2
    ));
    player.getInventory().setBoots(leather(
        Material.LEATHER_BOOTS, "Chef's Boots", NamedTextColor.GOLD,
        Color.fromRGB(0xF0F0F0), Enchantment.PROTECTION, 2
    ));
    player.getInventory().addItem(unbreakable(Material.GOLDEN_SWORD));
    player.getInventory().addItem(tool(Material.STONE_PICKAXE, Enchantment.EFFICIENCY, 2));
    player.getInventory().addItem(new ItemStack(Material.GOLDEN_CARROT, 64));
    player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
    player.getInventory().addItem(customPotion(
        Material.SPLASH_POTION,
        "Health Potion",
        NamedTextColor.RED,
        List.of(
            new PotionEffect(PotionEffectType.REGENERATION, 120, 2),
            new PotionEffect(PotionEffectType.ABSORPTION, 6000, 1),
            new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1)
        )
    ));
    player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 20 * 60 * 60, 1, false, false));
  }

  private void hunter(Player player) {
    TextColor green = TextColor.color(0x00CC00);
    player.getInventory().setHelmet(leather(
        Material.LEATHER_HELMET, "Hunter's Cap", green,
        Color.fromRGB(0x5E963C), Enchantment.PROTECTION, 1
    ));
    player.getInventory().setChestplate(leather(
        Material.LEATHER_CHESTPLATE, "Hunter's Tunic", green,
        Color.fromRGB(0x5E963C), Enchantment.PROTECTION, 2
    ));
    player.getInventory().setLeggings(leather(
        Material.LEATHER_LEGGINGS, "Hunter's Pants", green,
        Color.fromRGB(0x5E963C), Enchantment.PROTECTION, 2
    ));
    player.getInventory().setBoots(leather(
        Material.LEATHER_BOOTS, "Hunter's Boots", green,
        Color.fromRGB(0x5E963C), Enchantment.PROTECTION, 1
    ));
    player.getInventory().addItem(named(Material.BOW, "Hunter's Bow", green));
    player.getInventory().addItem(namedEnchanted(
        Material.WOODEN_SWORD, "Hunter's Shank", green, Enchantment.SHARPNESS, 3
    ));
    player.getInventory().addItem(tool(Material.WOODEN_PICKAXE, Enchantment.EFFICIENCY, 3));
    player.getInventory().addItem(new ItemStack(Material.COOKED_RABBIT, 64));
    player.getInventory().addItem(new ItemStack(Material.ARROW, 64), new ItemStack(Material.ARROW, 64));
  }

  private void maceBearer(Player player) {
    player.getInventory().setHelmet(armour(
        Material.CHAINMAIL_HELMET, "MaceBearer's Helmet", NamedTextColor.BLUE, Enchantment.PROTECTION, 2
    ));
    player.getInventory().setChestplate(armour(
        Material.IRON_CHESTPLATE, "MaceBearer's Chestplate", NamedTextColor.BLUE, Enchantment.PROTECTION, 2
    ));
    player.getInventory().setLeggings(armour(
        Material.IRON_LEGGINGS, "MaceBearer's Leggings", NamedTextColor.BLUE, Enchantment.PROTECTION, 2
    ));
    ItemStack boots = armour(
        Material.CHAINMAIL_BOOTS, "MaceBearer's Boots", NamedTextColor.BLUE, Enchantment.PROTECTION, 2
    );
    boots.addUnsafeEnchantment(Enchantment.FEATHER_FALLING, 3);
    player.getInventory().setBoots(boots);
    ItemStack mace = named(Material.MACE, "MaceBearer's Mace", NamedTextColor.BLUE);
    mace.addUnsafeEnchantment(Enchantment.WIND_BURST, 1);
    player.getInventory().addItem(mace);
    player.getInventory().addItem(tool(Material.IRON_PICKAXE, null, 0));
    player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 64));
    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 60 * 60, 0, false, false));
  }

  private void necromancer(Player player) {
    player.getInventory().setHelmet(armour(
        Material.SKELETON_SKULL, "Necromancer's Skull", NamedTextColor.DARK_PURPLE, Enchantment.PROTECTION, 2
    ));
    player.getInventory().setChestplate(leather(
        Material.LEATHER_CHESTPLATE, "Necromancer's Robe", NamedTextColor.DARK_PURPLE,
        Color.fromRGB(0x8238B1), Enchantment.PROTECTION, 2
    ));
    player.getInventory().setLeggings(leather(
        Material.LEATHER_LEGGINGS, "Necromancer's Breeches", NamedTextColor.DARK_PURPLE,
        Color.fromRGB(0x8238B1), Enchantment.PROTECTION, 2
    ));
    player.getInventory().setBoots(leather(
        Material.LEATHER_BOOTS, "Necromancer's Shoes", NamedTextColor.DARK_PURPLE,
        Color.fromRGB(0x8238B1), Enchantment.PROTECTION, 2
    ));
    player.getInventory().addItem(namedEnchanted(
        Material.STICK, "Necromancer's Magic(ish) Wand", NamedTextColor.DARK_PURPLE, Enchantment.SHARPNESS, 1
    ));
    player.getInventory().addItem(tool(Material.GOLDEN_PICKAXE, null, 0));
    player.getInventory().addItem(new ItemStack(Material.BONE, 64));
  }

  private void pro(Player player) {
    player.getInventory().setBoots(unbreakable(Material.IRON_BOOTS));
    player.getInventory().addItem(unbreakable(Material.WOODEN_AXE));
    player.getInventory().addItem(unbreakable(Material.FISHING_ROD));
    player.getInventory().addItem(customPotion(
        Material.POTION,
        "Potion of TurtleMaster",
        NamedTextColor.WHITE,
        List.of(
            new PotionEffect(PotionEffectType.RESISTANCE, 100, 4),
            new PotionEffect(PotionEffectType.SLOWNESS, 100, 4)
        )
    ));
    player.getInventory().addItem(tool(Material.WOODEN_HOE, null, 0));
    player.getInventory().addItem(new ItemStack(Material.HONEY_BOTTLE, 16));
  }

  private void swimmer(Player player) {
    ItemStack helmet = armour(
        Material.DIAMOND_HELMET, "Swimmer's Cap", NamedTextColor.AQUA, Enchantment.PROTECTION, 2
    );
    helmet.addUnsafeEnchantment(Enchantment.AQUA_AFFINITY, 1);
    helmet.addUnsafeEnchantment(Enchantment.RESPIRATION, 3);
    player.getInventory().setHelmet(helmet);
    player.getInventory().setChestplate(leather(
        Material.LEATHER_CHESTPLATE, "Swimmer's Rash Vest", NamedTextColor.AQUA,
        Color.fromRGB(0x3AB3DA), Enchantment.PROTECTION, 1
    ));
    player.getInventory().setLeggings(leather(
        Material.LEATHER_LEGGINGS, "Swimmer's Boardies", NamedTextColor.AQUA,
        Color.fromRGB(0x3AB3DA), Enchantment.PROTECTION, 1
    ));
    ItemStack boots = armour(
        Material.DIAMOND_BOOTS, "Swimmer's Aquatic Socks", NamedTextColor.AQUA, Enchantment.PROTECTION, 2
    );
    boots.addUnsafeEnchantment(Enchantment.DEPTH_STRIDER, 2);
    boots.addUnsafeEnchantment(Enchantment.FEATHER_FALLING, 4);
    player.getInventory().setBoots(boots);
    ItemStack trident = named(Material.TRIDENT, "Swimmer's Trident", NamedTextColor.AQUA);
    trident.addUnsafeEnchantment(Enchantment.RIPTIDE, 2);
    player.getInventory().addItem(trident);
    player.getInventory().addItem(tool(Material.DIAMOND_PICKAXE, null, 0));
    player.getInventory().addItem(new ItemStack(Material.COOKED_SALMON, 64));
  }

  private void vampire(Player player) {
    player.getInventory().setHelmet(leather(
        Material.LEATHER_HELMET, "Vampire's Collar", NamedTextColor.DARK_RED,
        Color.fromRGB(0x1D1D21), Enchantment.PROTECTION, 2
    ));
    player.getInventory().setChestplate(vampireRobe());
    player.getInventory().setLeggings(leather(
        Material.LEATHER_LEGGINGS, "Vampire's Trousers", NamedTextColor.DARK_RED,
        Color.fromRGB(0x1D1D21), Enchantment.PROTECTION, 2
    ));
    player.getInventory().setBoots(leather(
        Material.LEATHER_BOOTS, "Vampire's Shoes", NamedTextColor.DARK_RED,
        Color.fromRGB(0x1D1D21), Enchantment.PROTECTION, 1
    ));
    ItemStack tooth = namedEnchanted(
        Material.GHAST_TEAR, "Vampire's Tooth", NamedTextColor.DARK_RED, Enchantment.SHARPNESS, 5
    );
    ItemMeta toothMeta = tooth.getItemMeta();
    toothMeta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, "vampire_tooth");
    tooth.setItemMeta(toothMeta);
    player.getInventory().addItem(tooth);
    player.getInventory().addItem(tool(Material.IRON_PICKAXE, null, 0));
    ItemStack blood = named(Material.RABBIT, "Bowl of Blood", NamedTextColor.DARK_RED);
    blood.setAmount(64);
    player.getInventory().addItem(blood);
  }

  public ItemStack vampireRobe() {
    return leather(
        Material.LEATHER_CHESTPLATE, "Vampire's Robe", NamedTextColor.DARK_RED,
        Color.fromRGB(0x1D1D21), Enchantment.PROTECTION, 2
    );
  }

  public ItemStack batwingElytra() {
    return named(Material.ELYTRA, "Batwings", NamedTextColor.DARK_RED);
  }

  private void giveTeamBlocks(Player player, TeamSide team) {
    player.getInventory().addItem(
        new ItemStack(team.buildMaterial(), plugin.getConfig().getInt("game.build-block-count", 128)),
        new ItemStack(team.flagMaterial(), plugin.getConfig().getInt("game.flag-block-count", 64))
    );
  }

  private ItemStack special(Material material, String name, TextColor colour, String ability) {
    ItemStack item = named(material, name, colour);
    ItemMeta meta = item.getItemMeta();
    meta.getPersistentDataContainer().set(abilityKey, PersistentDataType.STRING, ability);
    meta.setEnchantmentGlintOverride(true);
    item.setItemMeta(meta);
    return item;
  }

  private static ItemStack named(Material material, String name, TextColor colour) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(Component.text(name, colour));
    meta.setUnbreakable(true);
    item.setItemMeta(meta);
    return item;
  }

  private static ItemStack namedEnchanted(
      Material material,
      String name,
      TextColor colour,
      Enchantment enchantment,
      int level
  ) {
    ItemStack item = named(material, name, colour);
    if (enchantment != null && level > 0) item.addUnsafeEnchantment(enchantment, level);
    return item;
  }

  private static ItemStack unbreakable(Material material) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    meta.setUnbreakable(true);
    item.setItemMeta(meta);
    return item;
  }

  private static ItemStack tool(Material material, Enchantment enchantment, int level) {
    ItemStack item = unbreakable(material);
    if (enchantment != null && level > 0) item.addUnsafeEnchantment(enchantment, level);
    return item;
  }

  private static ItemStack armour(
      Material material,
      String name,
      TextColor colour,
      Enchantment enchantment,
      int level
  ) {
    return namedEnchanted(material, name, colour, enchantment, level);
  }

  private static ItemStack leather(
      Material material,
      String name,
      TextColor colour,
      Color dye,
      Enchantment enchantment,
      int level
  ) {
    ItemStack item = named(material, name, colour);
    ItemMeta raw = item.getItemMeta();
    if (raw instanceof LeatherArmorMeta meta) {
      meta.setColor(dye);
      item.setItemMeta(meta);
    }
    if (enchantment != null && level > 0) item.addUnsafeEnchantment(enchantment, level);
    return item;
  }

  private static ItemStack customPotion(
      Material material,
      String name,
      TextColor colour,
      List<PotionEffect> effects
  ) {
    ItemStack item = new ItemStack(material);
    ItemMeta raw = item.getItemMeta();
    if (raw instanceof PotionMeta meta) {
      meta.displayName(Component.text(name, colour));
      for (PotionEffect effect : effects) meta.addCustomEffect(effect, true);
      item.setItemMeta(meta);
    }
    return item;
  }
}
