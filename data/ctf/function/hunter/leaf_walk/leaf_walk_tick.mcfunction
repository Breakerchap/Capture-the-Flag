execute at @s run fill ~-3 ~-3 ~-3 ~3 ~3 ~3 air replace dark_oak_leaves

# Place dark oak leaves under the player
execute at @s run fill ~-1 ~-1 ~-1 ~1 ~-1 ~1 minecraft:dark_oak_leaves replace air

# Increment timer
scoreboard players add @s leafWalkTime 1

# Keep looping if under 5 seconds
execute if score @s leafWalkTime matches ..99 run schedule function ctf:hunter/leaf_walk/leaf_walk_loop 1t

# Stop after 5 seconds
execute if score @s leafWalkTime matches 100.. run function ctf:hunter/leaf_walk/end_leaf_walk