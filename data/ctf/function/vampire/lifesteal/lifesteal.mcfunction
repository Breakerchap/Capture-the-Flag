execute as @a if items entity @s weapon.mainhand *[minecraft:custom_data~{vampire:tooth}] run effect give @s regeneration 1 2 true
advancement revoke @a only ctf:hit_player