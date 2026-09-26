package io.github.breakerchap.ctf;

import io.github.breakerchap.ctf.arena.ArenaManager;
import io.github.breakerchap.ctf.command.CtfCommand;
import io.github.breakerchap.ctf.game.GameListener;
import io.github.breakerchap.ctf.game.GameManager;
import io.github.breakerchap.ctf.kit.KitManager;
import io.github.breakerchap.ctf.marker.MarkerManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CaptureTheFlagPlugin extends JavaPlugin {
  private ArenaManager arenaManager;
  private MarkerManager markerManager;
  private KitManager kitManager;
  private GameManager gameManager;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    arenaManager = new ArenaManager(this);
    arenaManager.load();
    markerManager = new MarkerManager(this, arenaManager);
    kitManager = new KitManager(this);
    gameManager = new GameManager(this, arenaManager, markerManager, kitManager);
    gameManager.recoverInterruptedGames();

    CtfCommand ctfCommand = new CtfCommand(this, arenaManager, markerManager, gameManager, kitManager);
    PluginCommand command = getCommand("ctf");
    if (command == null) {
      throw new IllegalStateException("ctf command is missing from plugin.yml");
    }
    command.setExecutor(ctfCommand);
    command.setTabCompleter(ctfCommand);

    getServer().getPluginManager().registerEvents(markerManager, this);
    getServer().getPluginManager().registerEvents(new GameListener(this, arenaManager, gameManager, kitManager), this);

    gameManager.startTicker();
    getLogger().info("CaptureTheFlag enabled with " + arenaManager.all().size() + " configured arena(s).");
  }

  @Override
  public void onDisable() {
    if (gameManager != null) gameManager.shutdown();
    if (markerManager != null) markerManager.removeAllMarkerEntities();
    if (arenaManager != null) arenaManager.save();
  }

  public ArenaManager arenas() { return arenaManager; }
  public MarkerManager markers() { return markerManager; }
  public KitManager kits() { return kitManager; }
  public GameManager games() { return gameManager; }
}
