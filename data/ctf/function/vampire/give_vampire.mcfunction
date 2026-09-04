execute as @s run function ctf:remove_classes
tag @s add vampire

clear @s

item replace entity @s armor.head with leather_helmet[trim={pattern:dune,material:redstone},dyed_color={rgb:1908001},custom_name='["",{"text":"Vampire\'s Collar","italic":false,"color":"dark_red"}]',enchantment_glint_override=false,enchantments={levels:{protection:2}},unbreakable={}]
item replace entity @s armor.chest with elytra[trim={pattern:dune,material:redstone},dyed_color={rgb:1908001},custom_name='["",{"text":"Vampire\'s Robe","italic":false,"color":"dark_red"}]',enchantment_glint_override=false,enchantments={levels:{protection:2}},unbreakable={}]
item replace entity @s armor.legs with leather_leggings[trim={pattern:dune,material:redstone},dyed_color={rgb:1908001},custom_name='["",{"text":"Vampire\'s Trousers","italic":false,"color":"dark_red"}]',enchantment_glint_override=false,enchantments={levels:{protection:2}},unbreakable={}]
item replace entity @s armor.feet with leather_boots[trim={pattern:dune,material:redstone},dyed_color={rgb:1908001},custom_name='["",{"text":"Vampire\'s Shoes","italic":false,"color":"dark_red"}]',enchantment_glint_override=false,enchantments={levels:{protection:1}},unbreakable={}]

give @s ghast_tear[custom_name='["",{"text":"Vampire\'s Tooth","italic":false,"color":"dark_red"}]',enchantments={levels:{sharpness:5}},custom_data={vampire:tooth},unbreakable={}]
execute as @s run function ctf:give_terracotta
give @s iron_pickaxe[unbreakable={},can_break={predicates:[{blocks:red_terracotta},{blocks:blue_terracotta},{blocks:red_stained_glass},{blocks:blue_stained_glass},{blocks:glass}]}]
give @s rabbit[custom_name='["",{"text":"Bowl of Blood","italic":false,"color":"dark_red"}]'] 64

scoreboard players set @s elytra_cooldown 300