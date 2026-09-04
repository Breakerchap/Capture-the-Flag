execute as @s run function ctf:remove_classes
tag @s add mace_bearer

clear @s

item replace entity @s armor.head with chainmail_helmet[trim={pattern:silence,material:netherite},custom_name='["",{"text":"MaceBearer\'s Helmet","italic":false,"color":"blue"}]',enchantment_glint_override=false,enchantments={levels:{protection:2}},unbreakable={}]
item replace entity @s armor.chest with iron_chestplate[trim={pattern:silence,material:netherite},custom_name='["",{"text":"MaceBearer\'s Chestplate","italic":false,"color":"blue"}]',enchantment_glint_override=false,enchantments={protection:2},unbreakable={}]
item replace entity @s armor.legs with iron_leggings[trim={pattern:silence,material:netherite},custom_name='["",{"text":"MaceBearer\'s Leggings","italic":false,"color":"blue"}]',enchantment_glint_override=false,enchantments={protection:2},unbreakable={}]
item replace entity @s armor.feet with chainmail_boots[trim={pattern:silence,material:netherite},custom_name='["",{"text":"MaceBearer\'s Boots","italic":false,"color":"blue"}]',enchantment_glint_override=false,enchantments={protection:2,feather_falling:3},unbreakable={}]

give @s mace[custom_name='["",{"text":"MaceBearer\'s Mace","italic":false,"color":"blue"}]',enchantments={wind_burst:1},unbreakable={}]
execute as @s run function ctf:give_terracotta
give @s iron_pickaxe[unbreakable={},can_break={predicates:[{blocks:red_terracotta},{blocks:blue_terracotta},{blocks:red_stained_glass},{blocks:blue_stained_glass},{blocks:glass}]}]
give @s cooked_beef 64

scoreboard players set @s windburst_cooldown 200