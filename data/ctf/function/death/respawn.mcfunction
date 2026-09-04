gamemode adventure @s
execute as @s[team=Red] at @s run tp @s -6 -58 21 180 0
execute as @s[team=Blue] at @s run tp @s -6 -58 -66 0 0

execute as @s if entity @s[tag=hunter] run function ctf:hunter/give_hunter
execute as @s if entity @s[tag=chef] run function ctf:chef/give_chef
execute as @s if entity @s[tag=swimmer] run function ctf:swimmer/give_swimmer
execute as @s if entity @s[tag=vampire] run function ctf:vampire/give_vampire
execute as @s if entity @s[tag=mace_bearer] run function ctf:mace_bearer/give_mace_bearer
execute as @s if entity @s[tag=assassin] run function ctf:assassin/give_assassin
execute as @s if entity @s[tag=necromancer] run function ctf:necromancer/give_necromancer
execute as @s if entity @s[tag=pro] run function ctf:pro/give_pro

title @s title {"text":"Go!","color":"green","bold":true}