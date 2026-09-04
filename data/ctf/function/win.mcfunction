execute if score global redtimer matches 0 run title @a title {"text":"Red Wins!","color":"red","bold":true}
execute if score global bluetimer matches 0 run title @a title {"text":"Blue Wins!","color":"blue","bold":true}

gamemode spectator @a
clear @a

schedule function ctf:flags/reset_flags 3s