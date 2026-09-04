gamemode spectator

kill @s
effect clear @s
clear @s

tag @s remove leaf_walk_active
tag @s remove leaf_walk

advancement revoke @s only ctf:death

scoreboard players set @s dead_timer 60

title @s title {"text":"3","color":"red","bold":true}

tp @s -6.0 -48 -21.0 0 0