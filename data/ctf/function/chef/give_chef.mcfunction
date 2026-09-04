execute as @s run function ctf:remove_classes
tag @s add chef

clear @s

item replace entity @s armor.head with leather_helmet[trim={pattern:vex,material:resin},dyed_color={rgb:16383998},custom_name='["",{"text":"Chef\'s Hat","italic":false,"color":"gold"}]',enchantment_glint_override=false,enchantments={levels:{protection:2}},unbreakable={}]
item replace entity @s armor.chest with leather_chestplate[trim={pattern:vex,material:quartz},dyed_color={rgb:15768605},custom_name='["",{"text":"Chef\'s Apron","italic":false,"color":"gold"}]',enchantment_glint_override=false,enchantments={levels:{protection:2}},unbreakable={}]
item replace entity @s armor.legs with leather_leggings[trim={pattern:tide,material:quartz},dyed_color={rgb:15768605},custom_name='["",{"text":"Chef\'s Chaps","italic":false,"color":"gold"}]',enchantment_glint_override=false,enchantments={levels:{protection:2}},unbreakable={}]
item replace entity @s armor.feet with leather_boots[trim={pattern:snout,material:quartz},dyed_color={rgb:15768605},custom_name='["",{"text":"Chef\'s Boots","italic":false,"color":"gold"}]',enchantment_glint_override=false,enchantments={levels:{protection:2}},unbreakable={}]

give @s golden_sword[unbreakable={}]
give @s stone_pickaxe[enchantments={levels:{efficiency:2}},unbreakable={},can_break={predicates:[{blocks:red_terracotta},{blocks:blue_terracotta},{blocks:red_stained_glass},{blocks:blue_stained_glass},{blocks:glass}]}]
execute as @s run function ctf:give_terracotta
give @s golden_carrot 64
give @s golden_apple
give @s splash_potion[potion_contents={custom_color:16733268,custom_effects:[{id:regeneration,duration:120,amplifier:2},{id:absorption,duration:6000,amplifier:1},{id:instant_health,duration:1,amplifier:1}]},custom_name='["",{"text":"Health Potion","italic":false,"color":"red"}]']

scoreboard players set @s great_feast_cooldown 200