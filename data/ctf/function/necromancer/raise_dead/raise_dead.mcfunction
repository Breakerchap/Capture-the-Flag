clear @s soul_lantern

scoreboard players set @s raise_dead_cooldown 1800

execute as @s[team=Red] at @s run summon zombie ^ ^1.5 ^1.5 {CustomName:'{"text":"Necromancer\'s Zombie"}',CustomNameVisible:1b,Team:"Red",ArmorItems:[{},{},{},{id:"minecraft:leather_helmet",Count:1b}],Tags:["just_raised","necromancer_mobs"]}
execute as @s[team=Red] at @s run summon zombie ^ ^1.5 ^-1.5 {CustomName:'{"text":"Necromancer\'s Zombie"}',CustomNameVisible:1b,Team:"Red",ArmorItems:[{},{},{},{id:"minecraft:leather_helmet",Count:1b}],Tags:["just_raised","necromancer_mobs"]}
execute as @s[team=Red] at @s run summon skeleton ^1.5 ^1.5 ^ {CustomName:'{"text":"Necromancer\'s Skeleton"}',CustomNameVisible:1b,Team:"Red",ArmorItems:[{},{},{},{id:"minecraft:leather_helmet",Count:1b}],Tags:["just_raised","necromancer_mobs"]}
execute as @s[team=Red] at @s run summon skeleton ^-1.5 ^1.5 ^ {CustomName:'{"text":"Necromancer\'s Skeleton"}',CustomNameVisible:1b,Team:"Red",ArmorItems:[{},{},{},{id:"minecraft:leather_helmet",Count:1b}],Tags:["just_raised","necromancer_mobs"]}

execute as @s[team=Blue] at @s run summon zombie ^ ^1.5 ^1.5 {CustomName:'{"text":"Necromancer\'s Zombie"}',CustomNameVisible:1b,Team:"Blue",ArmorItems:[{},{},{},{id:"minecraft:leather_helmet",Count:1b}],Tags:["just_raised","necromancer_mobs"]}
execute as @s[team=Blue] at @s run summon zombie ^ ^1.5 ^-1.5 {CustomName:'{"text":"Necromancer\'s Zombie"}',CustomNameVisible:1b,Team:"Blue",ArmorItems:[{},{},{},{id:"minecraft:leather_helmet",Count:1b}],Tags:["just_raised","necromancer_mobs"]}
execute as @s[team=Blue] at @s run summon skeleton ^1.5 ^1.5 ^ {CustomName:'{"text":"Necromancer\'s Skeleton"}',CustomNameVisible:1b,Team:"Blue",ArmorItems:[{},{},{},{id:"minecraft:leather_helmet",Count:1b}],Tags:["just_raised","necromancer_mobs"]}
execute as @s[team=Blue] at @s run summon skeleton ^-1.5 ^1.5 ^ {CustomName:'{"text":"Necromancer\'s Skeleton"}',CustomNameVisible:1b,Team:"Blue",ArmorItems:[{},{},{},{id:"minecraft:leather_helmet",Count:1b}],Tags:["just_raised","necromancer_mobs"]}

scoreboard players set @e[tag=just_raised] raisen_dead_lifespan 600
execute as @e[tag=just_raised] run tag @s remove just_raised

advancement revoke @s only ctf:use_raise_dead