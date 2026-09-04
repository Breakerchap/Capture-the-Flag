# Reset scores
scoreboard players set global numofredflags 0
scoreboard players set global numofblueflags 0

# --- Red Flags (Only count if not mixed with blue) ---

# Single-block red flags
execute if block -22 -59 -3 red_stained_glass unless block -22 -59 -3 blue_stained_glass run scoreboard players add global numofredflags 1
execute if block 9 -59 -3 red_stained_glass unless block 9 -59 -3 blue_stained_glass run scoreboard players add global numofredflags 1
execute if block -22 -59 -40 red_stained_glass unless block -22 -59 -40 blue_stained_glass run scoreboard players add global numofredflags 1
execute if block 9 -59 -40 red_stained_glass unless block 9 -59 -40 blue_stained_glass run scoreboard players add global numofredflags 1

# 4-block red flags
execute if block -36 -60 -21 red_stained_glass if block -36 -60 -22 red_stained_glass if block -37 -60 -21 red_stained_glass if block -37 -60 -22 red_stained_glass unless block -36 -60 -21 blue_stained_glass unless block -36 -60 -22 blue_stained_glass unless block -37 -60 -21 blue_stained_glass unless block -37 -60 -22 blue_stained_glass run scoreboard players add global numofredflags 1

execute if block -6 -57 -21 red_stained_glass if block -6 -57 -22 red_stained_glass if block -7 -57 -21 red_stained_glass if block -7 -57 -22 red_stained_glass unless block -6 -57 -21 blue_stained_glass unless block -6 -57 -22 blue_stained_glass unless block -7 -57 -21 blue_stained_glass unless block -7 -57 -22 blue_stained_glass run scoreboard players add global numofredflags 1

execute if block 26 -60 -21 red_stained_glass if block 26 -60 -22 red_stained_glass if block 25 -60 -21 red_stained_glass if block 25 -60 -22 red_stained_glass unless block 26 -60 -21 blue_stained_glass unless block 26 -60 -22 blue_stained_glass unless block 25 -60 -21 blue_stained_glass unless block 25 -60 -22 blue_stained_glass run scoreboard players add global numofredflags 1

# --- Blue Flags (Only count if not mixed with red) ---

# Single-block blue flags
execute if block -22 -59 -3 blue_stained_glass unless block -22 -59 -3 red_stained_glass run scoreboard players add global numofblueflags 1
execute if block 9 -59 -3 blue_stained_glass unless block 9 -59 -3 red_stained_glass run scoreboard players add global numofblueflags 1
execute if block -22 -59 -40 blue_stained_glass unless block -22 -59 -40 red_stained_glass run scoreboard players add global numofblueflags 1
execute if block 9 -59 -40 blue_stained_glass unless block 9 -59 -40 red_stained_glass run scoreboard players add global numofblueflags 1

# 4-block blue flags
execute if block -36 -60 -21 blue_stained_glass if block -36 -60 -22 blue_stained_glass if block -37 -60 -21 blue_stained_glass if block -37 -60 -22 blue_stained_glass unless block -36 -60 -21 red_stained_glass unless block -36 -60 -22 red_stained_glass unless block -37 -60 -21 red_stained_glass unless block -37 -60 -22 red_stained_glass run scoreboard players add global numofblueflags 1

execute if block -6 -57 -21 blue_stained_glass if block -6 -57 -22 blue_stained_glass if block -7 -57 -21 blue_stained_glass if block -7 -57 -22 blue_stained_glass unless block -6 -57 -21 red_stained_glass unless block -6 -57 -22 red_stained_glass unless block -7 -57 -21 red_stained_glass unless block -7 -57 -22 red_stained_glass run scoreboard players add global numofblueflags 1

execute if block 26 -60 -21 blue_stained_glass if block 26 -60 -22 blue_stained_glass if block 25 -60 -21 blue_stained_glass if block 25 -60 -22 blue_stained_glass unless block 26 -60 -21 red_stained_glass unless block 26 -60 -22 red_stained_glass unless block 25 -60 -21 red_stained_glass unless block 25 -60 -22 red_stained_glass run scoreboard players add global numofblueflags 1
