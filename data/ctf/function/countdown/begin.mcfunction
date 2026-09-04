gamemode adventure @a

reload

function ctf:flags/reset_flags

kill @a
clear @a

execute as @a run function ctf:remove_classes

execute as @a[team=Red] run spawnpoint @s -6 -58 21 180
execute as @a[team=Blue] run spawnpoint @s -6 -58 -66 0

gamemode adventure @a
execute as @a[team=Red] at @s run tp @s -6 -58 21 180 0
execute as @a[team=Blue] at @s run tp @s -6 -58 -66 0 0