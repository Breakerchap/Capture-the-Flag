clear @s firework_star
item replace entity @s armor.chest with elytra[enchantment_glint_override=true,custom_name='["",{"text":"Batwings","italic":false,"color":"dark_red"}]']

scoreboard players set @s elytra_cooldown 740
scoreboard players set @s elytra_duration 140

advancement revoke @s only ctf:use_elytra