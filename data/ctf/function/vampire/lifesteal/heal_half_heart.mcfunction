# Store current health into score (1 point = 0.5 hearts)
execute store result score @s tempHealth run data get entity @s Health 1

# Add 1 point (0.5 hearts)
scoreboard players add @s tempHealth 1

# Clamp to 20 (full health = 10 hearts = 20 points)
execute if score @s tempHealth matches 21.. run scoreboard players set @s tempHealth 20

# Apply health back to player
execute store result entity @s Health float 1 run scoreboard players get @s tempHealth
