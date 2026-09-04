tag @s remove leaf_walk_active
scoreboard players reset @s leafWalkTime

execute as @s at @s run summon armor_stand ~ ~-1 ~ {Small:1b,Tags:["leaf_walk"],NoGravity:1b,Invisible:1b}

scoreboard players set @s leaf_walk_cooldown 600

schedule function ctf:hunter/leaf_walk/remove_last_leaves 3s replace