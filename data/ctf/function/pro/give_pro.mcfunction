execute as @s run function ctf:remove_classes
tag @s add pro

clear @s

item replace entity @s armor.feet with iron_boots

give @s wooden_axe[unbreakable={}]
give @s fishing_rod[unbreakable={}]
give @s potion[potion_contents={custom_color:10329495,custom_effects:[{id:resistance,duration:100,amplifier:4},{id:slowness,duration:100,amplifier:4}]},custom_name='["",{"text":"Potion of TurtleMaster","italic":false}]']
execute as @s run function ctf:give_terracotta
give @s wooden_hoe[unbreakable={},can_break={predicates:[{blocks:red_terracotta},{blocks:blue_terracotta},{blocks:red_stained_glass},{blocks:blue_stained_glass},{blocks:glass}]}]
give @s honey_bottle 16