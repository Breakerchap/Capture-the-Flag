# red team countdown
execute if score global numofredflags matches 4.. run scoreboard players remove global redtimer 1
execute store result bossbar ctf:redflags value run scoreboard players get global redtimer

# blue team countdown
execute if score global numofblueflags matches 4.. run scoreboard players remove global bluetimer 1
execute store result bossbar ctf:blueflags value run scoreboard players get global bluetimer
