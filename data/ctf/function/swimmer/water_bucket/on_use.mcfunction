scoreboard players set @s use_water_bucket 0
scoreboard players set @s water_bucket_cooldown 800

execute as @s at @s run summon armor_stand ~ ~ ~ {Tags:["water_scan"],NoGravity:1b,Invisible:1b}
schedule function ctf:swimmer/water_bucket/remove_water 5s
