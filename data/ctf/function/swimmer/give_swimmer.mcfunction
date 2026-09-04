execute as @s run function ctf:remove_classes
tag @s add swimmer

clear @s

item replace entity @s armor.head with diamond_helmet[trim={pattern:wayfinder,material:amethyst},custom_name='["",{"text":"Swimmer\'s Cap","italic":false,"color":"aqua"}]',enchantments={levels:{aqua_affinity:1,protection:2,respiration:3}},unbreakable={},enchantment_glint_override=false]
item replace entity @s armor.chest with leather_chestplate[trim={pattern:wayfinder,material:amethyst},dyed_color={rgb:3847130},custom_name='["",{"text":"Swimmer\'s Rash Vest","italic":false,"color":"aqua"}]',enchantments={levels:{protection:1}},unbreakable={},enchantment_glint_override=false]
item replace entity @s armor.legs with leather_leggings[trim={pattern:wayfinder,material:amethyst},dyed_color={rgb:3847130},custom_name='["",{"text":"Swimmer\'s Boardies","italic":false,"color":"aqua"}]',enchantments={levels:{protection:1}},unbreakable={},enchantment_glint_override=false]
item replace entity @s armor.feet with diamond_boots[trim={pattern:wayfinder,material:amethyst},custom_name='["",{"text":"Swimmer\'s Aquatic Socks","italic":false,"color":"aqua"}]',enchantments={levels:{depth_strider:2,feather_falling:4,protection:2}},unbreakable={},enchantment_glint_override=false]

give @s trident[custom_name='["",{"text":"Swimmer\'s Trident","italic":false,"color":"aqua"}]',enchantments={levels:{riptide:2}},unbreakable={},attribute_modifiers={modifiers:[{type:attack_damage,amount:6.5,operation:add_value,id:1745499488704},{type:attack_speed,amount:-2.5,operation:add_value,id:1745499488704}]}]
execute as @s run function ctf:give_terracotta
give @s diamond_pickaxe[unbreakable={},can_break={predicates:[{blocks:red_terracotta},{blocks:blue_terracotta},{blocks:red_stained_glass},{blocks:blue_stained_glass},{blocks:glass}]}]
give @s cooked_salmon 64

scoreboard players set @s water_bucket_cooldown 200