clear @a[tag=mace_bearer] wind_charge
execute as @a[tag=mace_bearer] unless data entity @s Inventory[{id:"minecraft:wind_charge", tag:{wind:burst}}] run item replace entity @s weapon.offhand with wind_charge[custom_name='["",{"text":"Windburst","italic":false,"color":"blue"}]',custom_data={wind:burst},enchantment_glint_override=true]
schedule clear ctf:mace_bearer/windburst/give_windburst