function ctf:chef/chef_health_boost
effect give @a[tag=mace_bearer] jump_boost 1 0 true
function ctf:stop_place

execute as @a[tag=assassin] if score @s use_pearl matches 1 run function ctf:assassin/pearl/on_throw
execute as @a[tag=mace_bearer] if score @s use_windburst matches 1 run function ctf:mace_bearer/windburst/on_throw
execute as @a[tag=swimmer] if score @s use_water_bucket matches 1 run function ctf:swimmer/water_bucket/on_use

# !True Invis
# Update invisState score
execute as @a[tag=assassin] at @s if predicate {"condition":"minecraft:entity_properties","entity":"this","predicate":{"effects":{"minecraft:invisibility":{}}}} run scoreboard players set @s invisState 1
execute as @a[tag=assassin] at @s unless predicate {"condition":"minecraft:entity_properties","entity":"this","predicate":{"effects":{"minecraft:invisibility":{}}}} run scoreboard players set @s invisState 0

# Strip gear if invisible and not yet cleared
execute as @a[tag=assassin,scores={invisState=1..}] unless entity @s[tag=invis_cleared] run function ctf:assassin/true_invis

# Detect invis ended
execute as @a[tag=assassin,tag=invis_cleared,scores={invisState=0}] run function ctf:assassin/invis_ended

# !Death
execute as @e at @s if entity @s[y=-140,dy=20] run advancement grant @s only ctf:death
kill @e[type=item]

scoreboard players remove @a dead_timer 1

execute as @a[scores={dead_timer=40}] run title @s title {"text":"2","color":"gold","bold":true}
execute as @a[scores={dead_timer=20}] run title @s title {"text":"1","color":"yellow","bold":true}
execute as @a[scores={dead_timer=0}] run function ctf:death/respawn

# !Cooldowns
# Chef
scoreboard players remove @a[tag=chef] great_feast_cooldown 1
execute as @a[tag=chef] at @s if score @s great_feast_cooldown matches 0 run function ctf:chef/great_feast/give_great_feast

# Hunter
scoreboard players remove @a[tag=hunter] leaf_walk_cooldown 1
execute as @a[tag=hunter] at @s if score @s leaf_walk_cooldown matches 0 run function ctf:hunter/leaf_walk/give_leaf_walk

# Assassin
scoreboard players remove @a[tag=assassin] pearl_cooldown 1
execute as @a[tag=assassin] at @s if score @s pearl_cooldown matches 0 run function ctf:assassin/pearl/give_pearl

# Mace Bearer
scoreboard players remove @a[tag=mace_bearer] windburst_cooldown 1
execute as @a[tag=mace_bearer] at @s if score @s windburst_cooldown matches 0 run function ctf:mace_bearer/windburst/give_windburst

# Swimmer
scoreboard players remove @a[tag=swimmer] water_bucket_cooldown 1
execute as @a[tag=swimmer] at @s if score @s water_bucket_cooldown matches 0 run function ctf:swimmer/water_bucket/give_water_bucket
clear @a[tag=swimmer] bucket

# Vampire
scoreboard players remove @a[tag=vampire] elytra_cooldown 1
execute as @a[tag=vampire] at @s if score @s elytra_cooldown matches 0 run function ctf:vampire/batwings/give_batwings
scoreboard players remove @a[tag=vampire] elytra_duration 1
execute as @a[tag=vampire] at @s if score @s elytra_duration matches 0 run function ctf:vampire/batwings/clear_batwings

# Necromancer
scoreboard players remove @a[tag=necromancer] raise_dead_cooldown 1
execute as @a[tag=necromancer] at @s if score @s raise_dead_cooldown matches 0 run function ctf:necromancer/raise_dead/give_raise_dead
scoreboard players remove @e[tag=necromancer_mobs] raisen_dead_lifespan 1
execute as @e[tag=necromancer_mobs] at @s if score @s raisen_dead_lifespan matches 0 run kill @s

# !FLags

function ctf:flags/count_flags

# tick timer
scoreboard players add global flagtick 1
execute if score global flagtick matches 20.. run function ctf:flags/tick_bossbars
execute if score global flagtick matches 20.. run scoreboard players set global flagtick 0

# show flag counts in actionbar with blue score colored
execute as @a run title @s actionbar [{"text":"Red Flags: ","color":"red"},{"score":{"name":"global","objective":"numofredflags"}},{"text":" | Blue Flags: ","color":"blue"},{"score":{"name":"global","objective":"numofblueflags"},"color":"blue"}]

# !Winning
execute if score global redtimer matches 0 run function ctf:win
execute if score global bluetimer matches 0 run function ctf:win