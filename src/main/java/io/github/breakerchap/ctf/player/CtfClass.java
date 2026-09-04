package io.github.breakerchap.ctf.player;

public enum CtfClass {
  ASSASSIN,
  CHEF,
  HUNTER,
  MACE_BEARER,
  NECROMANCER,
  PRO,
  SWIMMER,
  VAMPIRE;

  public static CtfClass parse(String value) {
    if (value == null) return null;
    String normalised = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
    try {
      return valueOf(normalised);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  public String commandName() {
    return name().toLowerCase();
  }
}
