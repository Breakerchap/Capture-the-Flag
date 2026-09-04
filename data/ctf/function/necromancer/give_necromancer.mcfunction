execute as @s run function ctf:remove_classes
tag @s add necromancer

clear @s

item replace entity @s armor.head with skeleton_skull[custom_name='["",{"text":"Necromancer\'s Skull","italic":false,"color":"dark_purple"}]',enchantments={protection:2},unbreakable={}]
item replace entity @s armor.chest with leather_chestplate[trim={pattern:dune,material:netherite},dyed_color={rgb:8530865},custom_name='["",{"text":"Necromancer\'s Robe","italic":false,"color":"dark_purple"}]',enchantments={levels:{protection:2}},unbreakable={}]
item replace entity @s armor.legs with leather_leggings[trim={pattern:dune,material:netherite},dyed_color={rgb:8530865},custom_name='["",{"text":"Necromancer\'s Breeches","italic":false,"color":"dark_purple"}]',enchantments={levels:{protection:2}},unbreakable={}]
item replace entity @s armor.feet with leather_boots[trim={pattern:dune,material:netherite},dyed_color={rgb:8530865},custom_name='["",{"text":"Necromancer\'s Shoes","italic":false,"color":"dark_purple"}]',enchantments={levels:{protection:2}},unbreakable={}]

give @s stick[enchantments={levels:{sharpness:1}},custom_name='["",{"text":"Necromancer\'s Magic(ish) Wand","italic":false,"color":"dark_purple"}]',custom_data={magic:stick},attribute_modifiers={modifiers:[{type:attack_knockback,amount:0.7,operation:add_value,id:1745498558239}]}]
execute as @s run function ctf:give_terracotta
give @s golden_pickaxe[unbreakable={},can_break={predicates:[{blocks:red_terracotta},{blocks:blue_terracotta},{blocks:red_stained_glass},{blocks:blue_stained_glass},{blocks:glass}]}]
give @s bone[consumable={consume_seconds:1.61},food={nutrition:5,saturation:3}] 64

scoreboard players set @s raise_dead_cooldown 300