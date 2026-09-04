execute as @s run function ctf:remove_classes
tag @s add assassin

clear @s

item replace entity @s armor.head with leather_helmet[trim={pattern:silence,material:netherite},dyed_color={rgb:1908001},custom_name='["",{"text":"Assassin\'s Cap","italic":false,"color":"dark_gray"}]',unbreakable={}]
item replace entity @s armor.chest with leather_chestplate[trim={pattern:silence,material:netherite},dyed_color={rgb:1908001},custom_name='["",{"text":"Assassin\'s Cap","italic":false,"color":"dark_gray"}]',enchantments={levels:{protection:1}},enchantment_glint_override=false,unbreakable={}]
item replace entity @s armor.legs with leather_leggings[trim={pattern:silence,material:netherite},dyed_color={rgb:1908001},custom_name='["",{"text":"Assassin\'s Cap","italic":false,"color":"dark_gray"}]',enchantments={levels:{protection:1}},enchantment_glint_override=false,unbreakable={}]
item replace entity @s armor.feet with leather_boots[trim={pattern:silence,material:netherite},dyed_color={rgb:1908001},custom_name='["",{"text":"Assassin\'s Cap","italic":false,"color":"dark_gray"}]',unbreakable={}]

give @s wooden_sword[minecraft:custom_model_data={strings:['dagger']},custom_name='["",{"text":"Assassin\'s Dagger","italic":false,"color":"dark_gray"}]',attribute_modifiers={modifiers:[{type:attack_speed,amount:6,operation:add_value,id:1744234593860}]},enchantments={levels:{sharpness:3}},unbreakable={}]
execute as @s run function ctf:give_terracotta
give @s stone_pickaxe[enchantments={levels:{efficiency:2}},unbreakable={},can_break={predicates:[{blocks:red_terracotta},{blocks:blue_terracotta},{blocks:red_stained_glass},{blocks:blue_stained_glass},{blocks:glass}]}]
give @s dried_kelp[food={nutrition:3,saturation:2}] 64
give @s potion[potion_contents={custom_color:12641022,custom_effects:[{id:invisibility,duration:300,amplifier:0,show_particles:0b},{id:speed,duration:300,amplifier:2,show_particles:0b},{id:jump_boost,duration:300,amplifier:1,show_particles:0b}]},custom_name='["",{"text":"Assassin\'s Syrup","italic":false,"color":"dark_gray"}]']

scoreboard players set @s pearl_cooldown 200