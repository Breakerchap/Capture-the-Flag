clear @s suspicious_stew

scoreboard players set @s great_feast_cooldown 1800

# For team "Red"
execute if entity @s[team=Red] run effect give @a[team=Red] minecraft:speed 20 2
execute if entity @s[team=Red] run effect give @a[team=Red] minecraft:regeneration 20 0
execute if entity @s[team=Red] run effect give @a[team=Red] minecraft:haste 20 0
execute if entity @s[team=Red] run effect give @a[team=Red] minecraft:jump_boost 20 1
execute if entity @s[team=Red] run title @a[team=Red] title {"text":"Great Feast Used","color":"gold","bold":true}


# For team "Blue"
execute if entity @s[team=Blue] run effect give @a[team=Blue] minecraft:speed 20 1
execute if entity @s[team=Blue] run effect give @a[team=Blue] minecraft:regeneration 20 0
execute if entity @s[team=Blue] run effect give @a[team=Blue] minecraft:haste 20 0
execute if entity @s[team=Blue] run effect give @a[team=Blue] minecraft:jump_boost 20 1
execute if entity @s[team=Blue] run title @a[team=Blue] title {"text":"Great Feast Used","color":"gold","bold":true}

advancement revoke @s only ctf:use_great_feast