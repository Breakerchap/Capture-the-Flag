say "Capture The Flag Loaded"

scoreboard objectives add recentHit dummy
scoreboard objectives add leafWalkTime dummy
scoreboard objectives add randomEffect dummy
scoreboard objectives add tempHealth dummy
scoreboard objectives add invisState dummy

scoreboard objectives add great_feast_cooldown dummy

scoreboard objectives add leaf_walk_cooldown dummy

scoreboard objectives add raise_dead_cooldown dummy
scoreboard objectives add raisen_dead_lifespan dummy

scoreboard objectives add elytra_cooldown dummy
scoreboard objectives add elytra_duration dummy

scoreboard objectives add pearl_cooldown dummy
scoreboard objectives add use_pearl minecraft.used:minecraft.ender_pearl

scoreboard objectives add windburst_cooldown dummy
scoreboard objectives add use_windburst minecraft.used:minecraft.wind_charge

scoreboard objectives add water_bucket_cooldown dummy
scoreboard objectives add use_water_bucket minecraft.used:minecraft.water_bucket

scoreboard objectives add dead_timer dummy

advancement revoke @a only ctf:use_great_feast
advancement revoke @a only ctf:use_leaf_walk
advancement revoke @a only ctf:hit_player
advancement revoke @a only ctf:use_elytra
advancement revoke @a only ctf:use_raise_dead

team add Red
team modify Red friendlyFire false

team add Blue
team modify Blue friendlyFire false
# !Flags

# setup scoreboards
scoreboard objectives add numofredflags dummy
scoreboard objectives add numofblueflags dummy
scoreboard objectives add redtimer dummy
scoreboard objectives add bluetimer dummy
scoreboard objectives add flagtick dummy

# setup bossbars
bossbar add ctf:redflags "Red Team Countdown"
bossbar add ctf:blueflags "Blue Team Countdown"

bossbar set ctf:redflags max 60
bossbar set ctf:blueflags max 60

bossbar set ctf:redflags color red
bossbar set ctf:blueflags color blue

bossbar set ctf:redflags style notched_20
bossbar set ctf:blueflags style notched_20

bossbar set ctf:redflags visible true
bossbar set ctf:blueflags visible true

bossbar set ctf:redflags players @a
bossbar set ctf:blueflags players @a

bossbar set ctf:redflags value 60
bossbar set ctf:blueflags value 60

# ensure scoreboard players exist
scoreboard players set global numofredflags 0
scoreboard players set global numofblueflags 0
scoreboard players set global redtimer 60
scoreboard players set global bluetimer 60
scoreboard players set global flagtick 0


