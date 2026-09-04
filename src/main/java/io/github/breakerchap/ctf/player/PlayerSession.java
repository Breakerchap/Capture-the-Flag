package io.github.breakerchap.ctf.player;

public final class PlayerSession {
  private String arenaName;
  private TeamSide team;
  private CtfClass ctfClass;

  public PlayerSession(String arenaName, TeamSide team) {
    this.arenaName = arenaName;
    this.team = team;
    this.ctfClass = CtfClass.PRO;
  }

  public String arenaName() {
    return arenaName;
  }

  public TeamSide team() {
    return team;
  }

  public CtfClass ctfClass() {
    return ctfClass;
  }

  public void setArenaName(String arenaName) {
    this.arenaName = arenaName;
  }

  public void setTeam(TeamSide team) {
    this.team = team;
  }

  public void setCtfClass(CtfClass ctfClass) {
    this.ctfClass = ctfClass;
  }
}
