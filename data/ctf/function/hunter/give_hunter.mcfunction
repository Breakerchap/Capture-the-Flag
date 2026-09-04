execute as @s run function ctf:remove_classes
tag @s add hunter

clear @s

item replace entity @s armor.head with leather_helmet[trim={pattern:tide,material:emerald},dyed_color={rgb:6198028,show_in_tooltip:false},custom_name='["",{"text":"Hunter\'s Cap","italic":false,"color":"#00cc00"}]',enchantments={levels:{protection:1}},unbreakable={show_in_tooltip:false},enchantment_glint_override=false]
item replace entity @s armor.chest with leather_chestplate[trim={pattern:tide,material:emerald},dyed_color={rgb:6198028,show_in_tooltip:false},custom_name='["",{"text":"Hunter\'s Tunic","italic":false,"color":"#00cc00"}]',enchantments={levels:{protection:2}},unbreakable={show_in_tooltip:false},enchantment_glint_override=false]
item replace entity @s armor.legs with leather_leggings[trim={pattern:tide,material:emerald},dyed_color={rgb:6198028,show_in_tooltip:false},custom_name='["",{"text":"Hunter\'s Pants","italic":false,"color":"#00cc00"}]',enchantments={levels:{protection:2}},unbreakable={show_in_tooltip:false},enchantment_glint_override=false]
item replace entity @s armor.feet with leather_boots[trim={pattern:tide,material:emerald},dyed_color={rgb:6198028,show_in_tooltip:false},custom_name='["",{"text":"Hunter\'s Boots","italic":false,"color":"#00cc00"}]',enchantments={levels:{protection:1}},unbreakable={show_in_tooltip:false},enchantment_glint_override=false]

give @s bow[custom_name='["",{"text":"Hunter\'s Bow","italic":false,"color":"#00cc00"}]',unbreakable={}]
give @s wooden_sword[custom_name='["",{"text":"Hunter\'s Shank","italic":false,"color":"#00cc00"}]',enchantments={levels:{sharpness:3}},unbreakable={}]
execute as @s run function ctf:give_terracotta
give @s wooden_pickaxe[enchantments={levels:{efficiency:3}},unbreakable={},can_break={predicates:[{blocks:red_terracotta},{blocks:blue_terracotta},{blocks:red_stained_glass},{blocks:blue_stained_glass},{blocks:glass}]}]
give @s cooked_rabbit 64

scoreboard players set @s leaf_walk_cooldown 200

item replace entity @s inventory.0 with arrow 64
item replace entity @s inventory.1 with arrow 64