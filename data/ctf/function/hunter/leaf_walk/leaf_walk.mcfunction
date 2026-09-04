# Start the ability
tag @s add leaf_walk_active
scoreboard players set @s leafWalkTime 0

# Remove the special leaf item
clear @s minecraft:dark_oak_leaves[custom_data={leaf_walk:true}] 1

# Start placing loop
function ctf:hunter/leaf_walk/leaf_walk_loop

# Revoke advancement to allow retriggering
advancement revoke @s only ctf:use_leaf_walk