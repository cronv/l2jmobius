```
██╗     ██████╗      ██╗    ███╗   ███╗ ██████╗ ██████╗ ██╗██╗   ██╗███████╗
██║     ╚════██╗     ██║    ████╗ ████║██╔═══██╗██╔══██╗██║██║   ██║██╔════╝
██║      █████╔╝     ██║    ██╔████╔██║██║   ██║██████╔╝██║██║   ██║███████╗
██║     ██╔═══╝ ██   ██║    ██║╚██╔╝██║██║   ██║██╔══██╗██║██║   ██║╚════██║
███████╗███████╗╚█████╔╝    ██║ ╚═╝ ██║╚██████╔╝██████╔╝██║╚██████╔╝███████║
╚══════╝╚══════╝ ╚════╝     ╚═╝     ╚═╝ ╚═════╝ ╚═════╝ ╚═╝ ╚═════╝ ╚══════╝
                        _____ _
                       |_   _| |_  _ _ ___ _ _  ___
                         | | | ' \| '_/ _ \ ' \/ -_)
                         |_| |_||_|_| \___/_||_\___|


[root@aden ~]# cat /etc/l2j-release
-----------------------------------------------------------------------------
 PROJECT    :: L2J_Mobius_14.3_Throne
 CHRONICLE  :: Throne
 RELEASE    :: 15.07.2026
 PROTOCOL   :: 563
 RUNTIME    :: Java - MySQL - LoginServer + GameServer
 LICENSE    :: GPLv3 inherited - MIT for L2JMobius development
 SOURCE     :: https://gitlab.com/MobiusDevelopment/L2J_Mobius
 WEBSITE    :: https://l2jmobius.org
 WIKI       :: https://l2jmobius.org/wiki/
 FORUM      :: https://l2jmobius.org/forum/
 DISCORD    :: https://discord.gg/bGeNBqv
-----------------------------------------------------------------------------
[root@aden ~]# ./gameserver --status
 >> server-side only. no client modifications. no client distribution.
 >> the goal: an original, unmodified game experience. actually playable.
```

# Throne Patch Notes Checklist

Everything the official Throne patch notes introduced or changed.  
Items marked *(client)* are handled by the game client and need no server work.

## Skills

### Othell Rogue

- ⬜ Enhanced the Blind Spot skill, which is an effect of the Shadow Flash skill
- ⬜ To reduce the number of self-cast buffs, the similar buffs have been merged
- ⬜ When Heart Breaker is used, the Heartbeat buff of a higher level is applied
- ⬜ Cannot be used simultaneously with the Heartbeat
- ⬜ The Blind Spot buff effect has changed

| Skill Name | Before | After |
|---|---|---|
| Blind Spot | For 5 sec., Critical Damage for basic attacks +30%, received PvP damage -75%. | For 5 sec., Critical Damage for basic attacks +40%, received PvP damage -75%. |

- ⬜ Improved the Angel of Death and Amazing Angel of Death skills
- ⬜ Removed   skill, Its effects have been transferred to the Angel of Death and Amazing Angel of Death skills

| Skill Name | Before | After |
|---|---|---|
| Angel of Death Lv. 1 | For 2 min., Perfection trigger rate +4, Perfection power +20, P. Skill Power +5%, P. Accuracy +8, P. Evasion +40. | For 2 min., Perfection trigger rate +4, Perfection power +20, P. Skill Power +5%, P. Accuracy +8, P. Evasion +50, Speed +90, P. Atk. +3%. |
| Angel of Death Lv. 2 | For 2 min., Perfection trigger rate +4, Perfection power +50, P. Skill Power +10%, P. Accuracy +8, P. Evasion +40. | For 2 min., Perfection trigger rate +4, Perfection power +50, P. Skill Power +10%, P. Accuracy +8, P. Evasion +55, Speed +90, P. Atk. +5%. |
| Amazing Angel of Death Lv. 1 | For 5 min., Perfection trigger rate +10, Perfection power +100, P. Skill Power +15%, P. Accuracy +10, P. Evasion +45, P. Atk. +5%. | For 5 min., Perfection trigger rate +10, Perfection power +100, P. Skill Power +15%, P. Accuracy +10, P. Evasion +60, P. Atk. +15%, Speed +90. |
| Amazing Angel of Death Lv. 2 | For 5 min., Perfection trigger rate +10, Perfection power +150, P. Skill Power +25%, P. Accuracy +10, P. Evasion +45, P. Atk. +10%. | For 5 min., Perfection trigger rate +10, Perfection power +150, P. Skill Power +25%, P. Accuracy +10, P. Evasion +70, P. Atk. +20%, Speed +90. |

- ⬜ Improved the Final Ultimate Evasion skill
- ⬜ Removed the /main/skills/othel_adventurer/19296_1_0.html skill. Its effects have been transferred to the Final Ultimate Evasion skill
- ⬜ The Cheating Death buff is now triggered starting from the first level of Final Ultimate Evasion

| Skill Name | Before | After |
|---|---|---|
| Final Ultimate Evasion Lv. 1 | When hit with a P./ M. skill, cancels damage and increases Debilitation/ Confusion/ Obstruction/ Stupor/ Infection/ Petrification/ Pull/ Danger Zone Resistance by 110 for 30 sec. with a 45% chance.; Protects from 5 debuffs. With a certain chance, cancels the enemy's target when hit.; Type: Debuff barrier.; Complete defense against debuff attacks a certain number of times.; Doesn't stack. | For 30 sec., P./ M. Evasion +400, Debilitation/ Confusion/ Obstruction/ Stupor/ Infection/ Petrification/ Pull/ Danger Zone Resistance +110.; Protects from debuffs. With a 50% chance, cancels the enemy's target when hit. On receiving fatal damage, triggers the Cheating Death effect that makes you invincible for 10 sec.; Type: Immunity to debuffs; Debuff attack immunity.; Doesn't stack. |
| Final Ultimate Evasion Lv. 2 | When hit with a P./ M. skill, cancels damage and increases Debilitation/ Confusion/ Obstruction/ Stupor/ Infection/ Petrification/ Pull/ Danger Zone Resistance by 130 for 30 sec. with a 50% chance.; Protects from 6 debuffs. With a certain chance, cancels the enemy's target when hit.; Type: Debuff barrier.; Complete defense against debuff attacks a certain number of times.; Doesn't stack. | For 30 sec., P./ M. Evasion +500, Debilitation/ Confusion/ Obstruction/ Stupor/ Infection/ Petrification/ Pull/ Danger Zone Resistance +130.; Protects from debuffs. With a 50% chance, cancels the enemy's target when hit. On receiving fatal damage, triggers the Cheating Death effect that makes you invincible for 10 sec.; Type: Immunity to debuffs; Debuff attack immunity.; Doesn't stack. |
| Final Ultimate Evasion Lv. 3 | When hit with a P./ M. skill, cancels damage and increases Debilitation/ Confusion/ Obstruction/ Stupor/ Infection/ Petrification/ Pull/ Danger Zone Resistance by 150 for 30 sec. with a 60% chance.; Protects from 7 debuffs. With a certain chance, cancels the enemy's target when hit.; Type: Debuff barrier.; Complete defense against debuff attacks a certain number of times.; Doesn't stack. | For 30 sec., P./ M. Evasion +600, Debilitation/ Confusion/ Obstruction/ Stupor/ Infection/ Petrification/ Pull/ Danger Zone Resistance +150.; Protects from debuffs. With a 50% chance, cancels the enemy's target when hit. On receiving fatal damage, triggers the Cheating Death effect that makes you invincible for 10 sec.; Type: Immunity to debuffs; Debuff attack immunity.; Doesn't stack. |
| Final Ultimate Evasion Lv. 4 | When hit with a P./ M. skill, cancels damage and increases Debilitation/ Confusion/ Obstruction/ Stupor/ Infection/ Petrification/ Pull/ Danger Zone Resistance by 160 for 30 sec. with a 75% chance.; Protects from 8 debuffs. With a certain chance, cancels the enemy's target when hit.; On receiving fatal damage, triggers the Cheating Death effect that makes you invincible for 5 sec.; Type: Debuff barrier.; Complete defense against debuff attacks a certain number of times.; Doesn't stack. | For 30 sec., P./ M. Evasion +700, Debilitation/ Confusion/ Obstruction/ Stupor/ Infection/ Petrification/ Pull/ Danger Zone Resistance +160.; Protects from debuffs. With a 50% chance, cancels the enemy's target when hit. On receiving fatal damage, triggers the Cheating Death effect that makes you invincible for 10 sec.; Type: Immunity to debuffs; Debuff attack immunity.; Doesn't stack. |
| Cheating Death | Allows to avoid death. Removes all debuffs. Grants invincibility for 5 sec.; Type: Invincibility; Damage and debuff immunity. | Allows to avoid death. For 10 sec., HP does not decrease below 1.; Type: Immortality; HP doesn't go below 1.; Doesn't stack. |

- ⬜ Some skills have been deleted
- ⬜ The items that had been used for enchanting these skills will be refunded to your private warehouse

| Name | Description |
|---|---|
| Shadow Dash | For 120 sec., Speed +66, P. Evasion +4, HP Recovery Rate +10. |
| Counter Flip | Knocks down the attacking opponent for 15 sec. and teleports you behind their back. After that the Counter Flip effect is cancelled. Requires a dagger or a dual dagger. |
| Shadow Fake Death | The player feigns death to prevent monsters from attacking. The player is not detected by the target until further movement. Consumes 100 MP per second. |

- ⬜ Modified the descriptions of some skills
- ⬜ Added a clarification regarding some skills that cannot be used simultaneously

| Skill Name | Before | After |
|---|---|---|
| Gust Riding | For 5 sec., Critical Damage for basic attacks +30%, Perfection power+100, Speed +40, P. Evasion +5.; Does not stack with Heartbeat. | For 5 sec., Critical Damage for basic attacks +30%, Perfection power+100, Speed +40, P. Evasion +5.; Does not stack with Heartbeat and Blind Spot. |
| Heartbeat | For 5 sec., Critical Damage for basic attacks +30%, Speed +40, P. Evasion +5. | For 5 sec., Critical Damage for basic attacks +30%, Speed +40, P. Evasion +5.; Does not stack with Blind Spot. |

- ⬜ Removed all mentions of the deleted skills from the description of Othell Adventurer's System
- ⬜ Removed all mentions of the deleted skills from the description of Superior Critical Rate's Othell Wind Rider
- ⬜ Modified the Soulstone consumption of some skills

| Class | Skill Name | Soulstone consumption |
|---|---|---|
| Othell Adventurer | Dexterous Strike | Soulstone x3 to x2 |
| Othell Wind Rider | Wind Sting | Soulstone x3 to x2 |

- ⬜ Modified the effect of the Superior Light Armor Mastery skill. Now the P. Skill Critical Rate increase effect is determined by the equipped armor

### Yul Archer

- ⬜ Expanded the levels of some skills
- ⬜ The Mind's Eye skill has been expanded to Lv. 6

| Skill Name | Required Level | Skill Effect | Required Items |
|---|---|---|---|
| Mind's Eye Lv. 6 New level | 115 | For 60 sec., opens your mind's eye. P. Accuracy +40, P. Atk. +15%, P. Skill Power +15%, Critical Rate for basic attacks +25%, Critical Damage for basic attacks +130%, P. Skill Critical Damage +20%.; Cannot be used simultaneously with the following skills: Survival Instinct/ Hunting Instinct/ Counter Instinct/ Bloody Instinct. | Rune Stone x16 |

- ⬜ Yul Sagittarius's Sniping skill has been expanded to level 6

| Skill name | Required level | Skill effect | Required items |
|---|---|---|---|
| Sniping Lv. 5 New level | 110 | For 30 min., when using a bow or a crossbow, P. Atk. +10%, P. Skill Power +6%, Critical Rate for basic attacks and P. skills +20%, P./ M. Def. +10%. | Rune Stone x16 |
| Sniping Lv. 6 New level | 115 | For 30 min., when using a bow or a crossbow, P. Atk. +10%, P. Skill Power +10%, Critical Rate for basic attacks and P. skills +25%, P./ M. Def. +10%. | Rune Stone x32 |

- ✅ Yul Sagittarius's Sagittarius' Shooting Star and Sagittarius' Falling Star skills have been expanded to Lv. 2. Also, the range of these skills has been increased and their visual effect is modified *(client)*

| Skill name | Required level | Skill effect | Required items |
|---|---|---|---|
| Sagittarius' Shooting Star Lv. 2 New level | 110 | Summons starlight to attack the target at a distance.; Attacks enemies in the skill range dealing damage equal to 134% of P. Atk.; High Critical rate.; Consumes 2 Soulstones.; Requires a bow or a crossbow. | Enhanced Rune Stone x1 |
| Sagittarius' Falling Star Lv. 2 New level | 120 | Attacks the target in front of you with the energy of a falling star.; Attacks enemies in the skill range dealing damage equal to 123% of P. Atk.; Decreases Sagittarius' Shooting Star's cooldown by 5 sec.; High Critical rate.; Consumes 2 Soulstones.; Requires a bow or a crossbow. | Enhanced Rune Stone x1 |

- ⬜ Yul Moonlight Sentinel's Rapid Fire Position skill has been expanded to level 6

| Skill name | Required level | Skill effect | Required items |
|---|---|---|---|
| Rapid Fire Position Lv. 5 New level | 110 | For 30 min., when using a bow or a crossbow, P. Atk. +10%, P. Skill Power +6%, P. Skill Critical Rate +10%.; Received M. Skill Critical Rate -15%, P. Skill Cooldown -10%. | Rune Stone x16 |
| Rapid Fire Position Lv. 6 New level | 115 | For 30 min., when using a bow or a crossbow, P. Atk. +10%, P. Skill Power +10%, P. Skill Critical Rate +15%.; Received M. Skill Critical Rate -15%, P. Skill Cooldown -10%. | Rune Stone x32 |

- ✅ Yul Moonlight Sentinel's Multiple Moonlit Arrows and Moon Shot skills have been expanded to Lv. 2. Also, increased the range of Moon Shot and modified its visual effect *(client)*

| Skill name | Required level | Skill effect | Required items |
|---|---|---|---|
| Multiple Moonlit Arrows Lv. 2 New level | 110 | Summons moonlight to attack the target at a distance.; Attacks enemies in the skill range dealing damage equal to 134% of P. Atk.; High Critical rate.; Consumes 2 Soulstones.; Requires a bow or a crossbow. | Enhanced Rune Stone x1 |
| Moon Shot Lv. 2 New level | 120 | Shoots an arrow imbued with the moon's power.; Deals damage to the target and all enemies along the way equal to 125% of P. Atk.; Decreases the cooldown of Multiple Moonlit Arrows by 5 sec.; High Critical rate.; Consumes 2 Soulstones.; Requires a bow or a crossbow. | Enhanced Rune Stone x1 |

- ⬜ Yul Ghost Sentinel's Hawk Eye skill has been expanded to level 6

| Skill name | Required level | Skill effect | Required items |
|---|---|---|---|
| Hawk Eye Lv. 5 New level | 110 | For 30 min., when using a bow or a crossbow, P. Atk. +10%, P. Skill Power +16%, P. Skill Critical Rate +10%.; Received M. Skill Critical Damage -15% | Rune Stone x16 |
| Hawk Eye Lv. 6 New level | 115 | For 30 min., when using a bow or a crossbow, P. Atk. +10%, P. Skill Power +20%, P. Skill Critical Rate +15%.; Received M. Skill Critical Damage -15% | Rune Stone x32 |

- ✅ Yul Ghost Sentinel's Shadow Piercing and Arrow Rain of Abyss skills have been expanded to Lv. 2. The visual effect of the Arrow Rain of Abyss skill has been changed *(client)*

| Skill name | Required level | Skill effect | Required items |
|---|---|---|---|
| Shadow Piercing Lv. 2 New level | 110 | The Ghost Sentinel unleashes their inner powers enhancing their combat abilities.; For 30 sec., P. Skill Critical Rate +50%. When killing an enemy, cooldown of the following skills is reduced by 1 sec.: Quick Shot, Pinpoint Shot, Impact Shot, Recoil Shot, Slow Shot, Tornado Shot, Multiple Arrows, Heavy Arrow Rain, Arrow Rain of Abyss.; Effect cooldown: 300 sec.; Consumes 15 Soulstones. | Enhanced Rune Stone x1 |
| Arrow Rain of Abyss Lv. 2 New level | 120 | Showers enemies with abysmal arrows.; Deals damage equal to 130% of P. Atk. to all enemies in the area of effect.; High Critical rate.; Consumes 2 Soulstones.; Requires a bow or a crossbow. | Enhanced Rune Stone x1 |

- ⬜ Yul Trickster's Vengeful Sniper skill has been expanded to level 6

| Skill name | Required level | Skill effect | Required items |
|---|---|---|---|
| Vengeful Sniper Lv. 5 New level | 110 | For 30 min., when using a bow or a crossbow, P. Atk. +10%, P. Skill Power +6%, P. Skill Critical Rate +10%, PvP damage +10%.; All received Critical Rate -10%, received M. Skill Damage in PvP -10%. | Rune Stone x16 |
| Vengeful Sniper Lv. 6 New level | 115 | For 30 min., when using a bow or a crossbow, P. Atk. +10%, P. Skill Power +10%, P. Skill Critical Rate +15%, PvP damage +10%.; All received Critical Rate -10%, received M. Skill Damage in PvP -10%. | Rune Stone x32 |

- ⬜ Yul Trickster's Expert Rapid Shot and Devastating Shot skills have been expanded to Lv. 2

| Skill name | Required level | Skill effect | Required items |
|---|---|---|---|
| Expert Rapid Shot Lv. 2 New level | 110 | Unleashes a volley of arrows at a single target.; Deals damage equal to 115% of P. Atk.; Ignores 10% of the target's P. Def.; High Critical rate.; Requires a bow or a crossbow. | Enhanced Rune Stone x1 |
| Devastating Shot Lv. 2 New level | 120 | Shoots an arrow at the target knocking them down.; Deals damage equal to 122% of P. Atk. to the target and nearby enemies.; In PvP, prevents the target from receiving buffs for 9 sec.; Consumes 10 Soulstones.; Requires a bow or a crossbow.; Base success rate: 70%; Inflicted status: Debilitation. | Enhanced Rune Stone x1 |

- ⬜ Modified some skill effects

| Class | Skill | Changes |
|---|---|---|
| All Archers | Tornado Shot Lv. 1~15 | Increased range and modified the visual effect. |
| All Archers | Heavy Arrow Rain Lv. 1~15 | Increased range and modified the visual effect. |
| All Archers | Area Flames Lv. 1~8 | Increased Critical Rate.; Increased MP Consumption for Lv. 2 and higher.; Increased the Critical Rate increase value when the Focus enchant path is chosen.; Note: The max value has been increased, but the increase when enchanting is reduced. |
| All Archers | Superior Light Armor Mastery Lv. 8~15 | Modified the P./ M. Def. increase.; <At max level>; P. Def. +7% to P. Def. +17%, M. Def. +10% |
| All Archers | Dead Eye Stance Lv. 1~4 | The Dead Eye Stance, Rapid Fire and Sniper Stance skills have been merged.; The effects have been modified to account for the merger.; <Max. level effect>; When using a bow or a crossbow: P. Atk. +20%; Atk. Spd. +20%; P. Skill Power +10%; P. Skill Critical Rate +10%; P. Skill Cooldown -10% |
| All Archers | Last Stance Lv. 1~2 | Due to the changes made to the Dead Eye Stance, the effects of Last Stance have also been modified.; <Max. level effect>; When using a bow or a crossbow: P. Atk. +30%; Atk. Spd. +20%; P. Skill Power +15%; P. Skill Critical Rate +25%; P./ M. Def. +10%; P. Skill Cooldown -10% |
| All Archers | Final Ultimate Escape Lv. 1~4 | <Max. level effect>; P./ M. Evasion +700; AoE Damage Resistance +30%; Buff Cancel Resistance +90% |
| Yul Sagittarius | Survival Instinct Lv. 1~3 | <Max. level balance changes>; Cooldown 600 sec. to 300 sec.; When the Time enchant path is chosen, max duration: +15 sec. to +20 sec.; P. Skill Power +25% to +40% |
| Yul Moonlight Sentinel | Hunting Instinct Lv. 1~3 | <Max. level balance changes>; Cooldown 600 sec. to 300 sec.; When the Time enchant path is chosen, max duration: +15 sec. to +20 sec.; P. Skill Power +25% to +40% |
| Yul Ghost Sentinel | Counter Instinct Lv. 1~3 | <Max. level balance changes>; Cooldown 600 sec. to 300 sec.; When the Time enchant path is chosen, max duration: +15 sec. to +20 sec.; P. Skill Power +25% to +40% |
| Yul Trickster | Bloody Instinct Lv. 1~3 | <Max. level balance changes>; Cooldown 600 sec. to 300 sec.; When the Time enchant path is chosen, max duration: +15 sec. to +20 sec.; P. Skill Power +25% to +40% |

- ⬜ Some skills have been removed
- ⬜ The items spent on learning and enchanting these skills will be returned to your private warehouse

| Name | Description |
|---|---|
| Rapid Fire Stance | For 30 min., P. Atk. +10%, P. Skill Cooldown -5%. Requires a bow or a crossbow. |
| Sniper Stance | When using a bow or a crossbow, P. Atk. +10%, Atk. Spd. +5% for 30 min. |
| Quick Fire | When using a bow or a crossbow, Atk. Spd. +20% for 30 sec. |

- ⬜ Modified the effect of the Superior Light Armor Mastery skill. Now the P. Skill Critical Rate increase effect is determined by the equipped armor

### Feoh Wizard

- ⬜ Modified some skill effects for all Wizards

| Skill | Changes |
|---|---|
| Elemental Burst Lv. 1~11 | [Balance changes]; Increased M. Critical trigger rate for all Elemental Burst skills. |
| Superior Burst Casting Lv. 1~2 | Modified the casting visual effect. |

- ⬜ Expanded the levels of some skills
- ⬜ Feoh Archmage's Volcanic Blast and Volcanic Division skills have been expanded to Lv. 2

| Skill name | Required level | Skill effect | Required items |
|---|---|---|---|
| Volcanic Blast Lv. 2 New level | 110 | A powerful elemental explosion that affects enemies at the front.; Deals magic damage to the target and nearby enemies. Power 850. The damage increases in accordance with the number of affected enemies.; High Critical rate.; Also, applies the Sun Dominance effect on you: For 15 sec., Attribute Attack +100.; Consumes 2 Spirit Ore.; Over-hit. | Enhanced Rune Stone x1 |
| Volcanic Division Lv. 2 New level | 120 | Upgrades Volcanic Blast to Volcanic Blast - Division. Burst Casting and Superior Burst Casting cooldown is reduced to 180 sec.; When using M. attack skills, Volcanic Blast cooldown -1 sec.; <Volcanic Blast - Division>; Volcanic Blast also applies the Volcanic Destruction debuff. | Enhanced Rune Stone x1 |

- ⬜ Feoh Soultaker's Black Blast and Black Division skills have been expanded to Lv. 2

| Skill name | Required level | Skill effect | Required items |
|---|---|---|---|
| Black Blast Lv. 2 New level | 110 | A powerful elemental explosion that affects enemies at the front.; Deals magic damage to the target and nearby enemies. Power 850. The damage increases in accordance with the number of affected enemies.; High Critical rate.; Also, the damage is increased if the target is debuffed.; Consumes 2 Spirit Ore.; Over-hit. | Enhanced Rune Stone x1 |
| Black Division Lv. 2 New level | 120 | Upgrades Black Blast to Black Blast - Division. Burst Casting and Superior Burst Casting cooldown is reduced to 180 sec.; When using Elemental Burst, Black Blast cooldown -2 sec.; <Black Blast - Division>; Black Blast also applies the Black Destruction debuff. | Enhanced Rune Stone x1 |

- ⬜ Feoh Mystic Muse's Frost Blast and Frost Division skills have been expanded to Lv. 2

| Skill name | Required level | Skill effect | Required items |
|---|---|---|---|
| Frost Blast Lv. 2 New level | 110 | A powerful elemental explosion that affects enemies at the front.; Deals magic damage to the target and nearby enemies. Power 850. The damage increases in accordance with the number of affected enemies.; High Critical rate.; Additionally, applies Frost Supremacy on the target in PvP (skill cooldown +7%).; The skill has the shortest cooldown among the Explosion-type skills.; Consumes 2 Spirit Ore.; Over-hit. | Enhanced Rune Stone x1 |
| Frost Division Lv. 2 New level | 120 | Upgrades Frost Blast to Frost Blast - Division. Burst Casting and Superior Burst Casting cooldown is reduced to 180 sec.; When using Elemental Burst, Frost Blast cooldown -1 sec.; <Frost Blast - Division>; Frost Blast also applies the Frost Destruction debuff. | Enhanced Rune Stone x1 |

- ⬜ Feoh Storm Screamer's Storm Blast and Storm Division skills have been expanded to Lv. 2

| Skill name | Required level | Skill effect | Required items |
|---|---|---|---|
| Storm Blast Lv. 2 New level | 110 | A powerful elemental explosion that affects enemies at the front.; Deals magic damage to the target and nearby enemies. Power 850. The damage increases in accordance with the number of affected enemies.; High Critical rate.; A high chance to hit enemies in the skill range.; Consumes 2 Spirit Ore.; Over-hit. | Enhanced Rune Stone x1 |
| Storm Division Lv. 2 New level | 120 | Upgrades Storm Blast to Storm Blast - Division. Burst Casting and Superior Burst Casting cooldown is reduced to 180 sec.; When using Storm Burst, Frost Blast cooldown -2 sec.; <Storm Blast - Division>; Storm Blast also applies the Storm Destruction debuff. | Enhanced Rune Stone x1 |

- ⬜ Feoh Soul Hound's Sacred Blast and Sacred Division skills have been expanded to Lv. 2

| Skill name | Required level | Skill effect | Required items |
|---|---|---|---|
| Sacred Blast Lv. 2 New level | 110 | A powerful elemental explosion that affects enemies at the front.; Deals magic damage to the target and nearby enemies. Power 850. The damage increases in accordance with the number of affected enemies.; High Critical rate.; Also, applies the Sacred Dominance effect on you: For 15 sec., PvP damage +10%.; Consumes 2 Spirit Ore.; Over-hit. | Enhanced Rune Stone x1 |
| Sacred Division Lv. 2 New level | 120 | Upgrades Sacred Blast to Sacred Blast - Division. Burst Casting and Superior Burst Casting cooldown is reduced to 180 sec.; When using M. attack skills, Sacred Blast cooldown -1 sec.; <Sacred Blast - Division>; Sacred Blast also applies the Sacred Destruction debuff. | Enhanced Rune Stone x1 |

### Sigel Knight

- ⬜ Modified the Soulstone consumption for some skills

| Class | Skill name | Soulstone consumption |
|---|---|---|
| Sigel Death Knight | Hell Rift | Soulstone x3 to x2 |
| Sigel Shillien Templar | Templar's Spirited Cry | Soulstone x3 to x2 |
| Sigel Eva's Templar | Guardian's Thunderous Cry | Soulstone x3 to x2 |
| Sigel Phoenix Knight | Paladin's Fervent Cry | Soulstone x3 to x2 |
| Sigel Hell Knight | Avenger's Bitter Cry | Soulstone x3 to x2 |

### Eviscerator

- ⬜ Fixed the issue that caused the range of the Flying Knee Kick skill to malfunction when the skill was enchanted
- ⬜ Fixed the issue that prevented the Footwork Lv. 4 buff from triggering when Fluid Weave Lv. 4 effect was active, and an enhancing skill was used
- ⬜ Fixed the issue that caused the P. Atk. increase value to be displayed incorrectly in the description of the Steel Mind enchanted skill

### Other Skill Changes

- ⬜ Fixed the issue that caused the Funky Star and Fantasia Ring debuffs to not be applied when using Shine Maker's Sparkling Touch skill
- ⬜ Corrected the errors in the descriptions of the Sword Muse's Glory and Spectral Dancer's Glory skills (Iss Sword Muse and Iss Spectral Dancer classes)
- ⬜ Fixed the issue that sometimes caused Aeore Healer's Dark Rain skill's debuff to be removed
- ⬜ Fixed the issue that caused the effect direction and the character's direction to be different when using Samurai's Advent skill
- ⬜ Fixed the issue that caused the effect duration stated in the skill description to be different from the actual duration when enchanting Warg's Warg's Energy Surge skill along the Time path

## Items

### Agathion Changes

- ⬜ Modified the effects of 26 agathions

| Item name | Effect change | Effect change |
|---|---|---|
| Item name | Standard skill | Additional skill |
| Agathion Charm: Virgo | Increases acquired XP.; to; Enhances Vampiric Rage.; When enchanted to +9, CHA +1.; When enchanted to +10, damage +1%. | Increases Critical Rate for basic attacks; when enchanted to +9, CHA +1; when enchanted to +10, damage +1%.; to; Increases acquired XP/ SP. |
| Greater Agathion Charm: Virgo | Increases acquired XP.; to; Enhances Vampiric Rage. | Increases Critical Rate for basic attacks, CHA, inflicted damage, P. Skill Power, decreases received damage.; to; Increases acquired XP/ SP.; When enchanted to +5, decreases cooldown.; When enchanted to +9, CHA +1.; When enchanted to +10, received damage -1%. |
| Agathion Charm: Capricorn | Increases Vitality bonus.; to; Increases Atk. Spd.; When enchanted to +9, CHA +1.; When enchanted to +10, damage +1%. | Increases M. Skill Critical Rate; when enchanted to +9, CHA +1; when enchanted to +10, inflicted damage +1%.; to; Increases P. Skill Critical Rate. |
| Greater Agathion Charm: Capricorn | Increases Vitality bonus.; to; Increases Atk. Spd. | Increases M. Skill Critical Rate, CHA, inflicted damage, Skill Mastery Rate, reduces received damage.; to; Increases P. Critical Rate.; When enchanted to +5, increases P. Atk.; When enchanted to +9, CHA +1.; When enchanted to +10, received damage -1%. |
| Agathion Charm: Libra | Decreases Skill MP Consumption.; to; Increases Atk. Spd.; When enchanted to +9, CHA +1.; When enchanted to +10, damage +1%. | Increases M. Skill Critical Damage; when enchanted to +9, CHA +1; when enchanted to +10, inflicted damage +1%.; to; P. Skill Critical Damage Boost. |
| Greater Agathion Charm: Libra | Decreases Skill MP Consumption.; to; Increases Atk. Spd. | Increases M. Skill Critical Damage, CHA, inflicted damage, Skill Mastery Rate, reduces received damage.; to; Increases P. Critical Damage.; When enchanted to +5, increases P. Def.; When enchanted to +9, CHA +1.; When enchanted to +10, received damage -1%. |
| Agathion Charm: Aries | Increases Damage Reflection Resistance.; to; Increases Vitality bonus.; When enchanted to +9, CHA +1.; When enchanted to +10, damage +1%. | Increases P. Skill Critical Damage; when enchanted to +9, CHA +1; when enchanted to +10, inflicted damage +1%.; to; Increases Weapon P./ M. Atk. |
| Greater Agathion Charm: Aries | Increases Damage Reflection Resistance.; to; Increases Vitality bonus. | Increases P. Skill Critical Damage, CHA, inflicted damage, Skill Mastery Rate, reduces received damage.; to; Increases Weapon P./ M. Atk.; When enchanted to +5, increases Max HP/ MP.; When enchanted to +9, CHA +1.; When enchanted to +10, received damage -1%. |
| Agathion Charm: Taurus | Increases Max HP.; to; Increases Vitality bonus.; When enchanted to +9, CHA +1.; When enchanted to +10, damage +1%. | Increases P. Atk.; when enchanted to +9, CHA +1; when enchanted to +10, inflicted damage +1%.; to; Increases Armor P. Def. |
| Greater Agathion Charm: Taurus | Increases Max HP.; to; Increases Vitality bonus. | Increases P. Def., CHA, inflicted damage, reduces received damage when immobilized, received damage.; to; Increases Armor P. Def.; When enchanted to +5, increases Max HP/ MP.; When enchanted to +9, CHA +1.; When enchanted to +10, received damage -1%. |
| Agathion Charm: Leo | Increases Max MP.; to; Increases Atk. Spd.; When enchanted to +9, CHA +1.; When enchanted to +10, damage +1%. | Increases P. Atk.; when enchanted to +9, CHA +1; when enchanted to +10, inflicted damage +1%.; to; Increases Basic Attack Power. |
| Greater Agathion Charm: Leo | Increases Max MP.; to; Increases Atk. Spd. | Increases P. Atk., CHA, inflicted damage, damage to immobilized targets, reduces received damage.; to; Increases Basic Attack Power.; When enchanted to +5, increases M. Def.; When enchanted to +9, CHA +1.; When enchanted to +10, received damage -1%. |
| Agathion Charm: Gemini | Increases Debuff Resistance.; to; Increases Atk. Spd.; When enchanted to +9, CHA +1.; When enchanted to +10, damage +1%. | Increases M. Def.; when enchanted to +9, CHA +1; when enchanted to +10, inflicted damage +1%.; to; Increases Critical Rate for basic attacks. |
| Greater Agathion Charm: Gemini | Increases Debuff Resistance.; to; Increases Atk. Spd. | Increases M. Def., CHA, inflicted damage, reduces received damage when immobilized, received damage.; to; Increases Critical Rate for basic attacks.; When enchanted to +5, increases P. Atk.; When enchanted to +9, CHA +1.; When enchanted to +10, received damage -1%. |
| Agathion Charm: Scorpio | Enhances Vampiric Rage.; to; Increases Casting Spd.; When enchanted to +9, CHA +1.; When enchanted to +10, damage +1%. | Increases Critical Damage for basic attacks; when enchanted to +9, CHA +1; when enchanted to +10, inflicted damage +1%.; to; Increases M. Skill Critical Damage. |
| Greater Agathion Charm: Scorpio | Enhances Vampiric Rage.; to; Increases Casting Spd. | Increases Critical Damage for basic attacks, CHA, inflicted damage, damage to immobilized targets, reduces received damage.; to; Increases M. Skill Critical Damage.; When enchanted to +5, increases P. Def.; When enchanted to +9, CHA +1.; When enchanted to +10, received damage -1%. |
| Agathion Charm: Aquarius | Increases Max CP.; to; Increases Atk. Spd.; When enchanted to +9, CHA +1.; When enchanted to +10, damage +1%. | Increases M. Atk.; when enchanted to +9, CHA +1; when enchanted to +10, inflicted damage +1%.; to; P. Skill Power is increased. |
| Greater Agathion Charm: Aquarius | Increases Max CP.; to; Increases Atk. Spd. | Increases M. Atk., CHA, inflicted damage, M. Skill Power, reduces received damage.; to; Increases P. Power.; When enchanted to +5, increases M. Def.; When enchanted to +9, CHA +1.; When enchanted to +10, received damage -1%. |
| Agathion Charm: Pisces | Increases Bow/ Crossbow Resistance.; to; Increases Casting Spd.; When enchanted to +9, CHA +1.; When enchanted to +10, damage +1%. | Increases Casting Spd.; when enchanted to +9, CHA +1; when enchanted to +10, inflicted damage +1%.; to; Increases M. Skill Power. |
| Greater Agathion Charm: Pisces | Increases Bow/ Crossbow Resistance.; to; Increases Casting Spd. | Increases Casting Spd., CHA, inflicted damage, skill cooldown, reduces received damage.; to; Increases M. Power.; When enchanted to +5, increases M. Def.; When enchanted to +9, CHA +1.; When enchanted to +10, received damage -1%. |
| Agathion Charm: Sagittarius | Increases Dagger Resistance.; to; Increases Atk. Spd.; When enchanted to +9, CHA +1.; When enchanted to +10, damage +1%. | Increases Atk. Spd.; when enchanted to +9, CHA +1; when enchanted to +10, inflicted damage +1%.; to; Increases Basic Critical Damage. |
| Greater Agathion Charm: Sagittarius | Increases Dagger Resistance.; to; Increases Atk. Spd. | Increases Atk. Spd., CHA, inflicted damage, reduces skill cooldown, received damage.; to; Increases Basic Critical Damage.; When enchanted to +5, increases P. Def.; When enchanted to +9, CHA +1.; When enchanted to +10, received damage -1%. |
| Agathion Charm: Cancer | Decreases Vitality Consumption.; to; Increases Casting Spd.; When enchanted to +9, CHA +1.; When enchanted to +10, damage +1%. | Increases P. Skill Critical Rate; when enchanted to +9, CHA +1; when enchanted to +10, inflicted damage +1%.; to; Increases M. Skill Critical Rate |
| Greater Agathion Charm: Cancer | Decreases Vitality Consumption.; to; Increases Casting Spd. | Increases P. Skill Critical Rate, CHA, inflicted damage, P. Skill Power, reduces received damage.; to; Increases M. Skill Critical Rate; When enchanted to +5, increases M. Atk.; When enchanted to +9, CHA +1.; When enchanted to +10, received damage -1%. |
| Agathion Charm: Fire Dragon | Increases Weapon P./ M. Atk.; to; Increases Vitality bonus. | Increases P./ M. Atk., P./ M. Def.; to; Increases Weapon P./ M. Atk., P./ M. Atk., P./ M. Def. |
| Greater Agathion Charm: Fire Dragon | Increases Weapon P./ M. Atk.; to; Increases Vitality bonus. | Increases P./ M. Atk., P./ M. Def., Critical Damage for basic attacks and P./ M. skills.; to; Increases Weapon P./ M. Atk., P./ M. Atk., P./ M. Def., all Critical Damage. |

- ⬜ Updated the description of Divine Book of Growth: Agathion Dragon to reflect the agathion changes

| Before | Now |
|---|---|
| Required for enchanting Agathion Fire Dragon.; In case of failure, the enchant value remains the same. | Required for enchanting Agathion Dragon.; <Enchantment items>; Greater Agathion Charm: Fire Dragon; Agathion Charm: Fire Dragon; In case of failure, the enchant value remains the same. |

### Dim Starlight Agathion

- ⬜ Obsolete Starlight Agathions have been updated
- ⬜ The "Dim" prefix has been added to the names of obsolete Starlight Agathions

| Before | After |
|---|---|
| Dim Starlight Agathion Charm: Virgo Lv. 1~12 | Dim Starlight Agathion Charm: Virgo Lv. 1~12 |
| Dim Starlight Agathion Charm: Capricorn Lv. 1~12 | Dim Starlight Agathion Charm: Capricorn Lv. 1~12 |
| Dim Starlight Agathion Charm: Libra Lv. 1~12 | Dim Starlight Agathion Charm: Libra Lv. 1~12 |
| Dim Starlight Agathion Charm: Aries Lv. 1~12 | Dim Starlight Agathion Charm: Aries Lv. 1~12 |
| Dim Starlight Agathion Charm: Taurus Lv. 1~12 | Dim Starlight Agathion Charm: Taurus Lv. 1~12 |
| Dim Starlight Agathion Charm: Leo Lv. 1~12 | Dim Starlight Agathion Charm: Leo Lv. 1~12 |
| Dim Starlight Agathion Charm: Gemini Lv. 1~12 | Dim Starlight Agathion Charm: Gemini Lv. 1~12 |
| Dim Starlight Agathion Charm: Scorpio Lv. 1~12 | Dim Starlight Agathion Charm: Scorpio Lv. 1~12 |
| Dim Starlight Agathion Charm: Aquarius Lv. 1~12 | Dim Starlight Agathion Charm: Aquarius Lv. 1~12 |
| Dim Starlight Agathion Charm: Pisces Lv. 1~12 | Dim Starlight Agathion Charm: Pisces Lv. 1~12 |
| Dim Starlight Agathion Charm: Sagittarius Lv. 1~12 | Dim Starlight Agathion Charm: Sagittarius Lv. 1~12 |
| Dim Starlight Agathion Charm: Cancer Lv. 1~12 | Dim Starlight Agathion Charm: Cancer Lv. 1~12 |
| Dim Agathion Bracelet: Virgo Lv. 1~12 | Dim Agathion Bracelet: Virgo Lv. 1~12 |
| Dim Agathion Bracelet: Capricorn Lv. 1~12 | Dim Agathion Bracelet: Capricorn Lv. 1~12 |
| Dim Agathion Bracelet: Libra Lv. 1~12 | Dim Agathion Bracelet: Libra Lv. 1~12 |
| Dim Agathion Bracelet: Aries Lv. 1~12 | Dim Agathion Bracelet: Aries Lv. 1~12 |
| Dim Agathion Bracelet: Taurus Lv. 1~12 | Dim Agathion Bracelet: Taurus Lv. 1~12 |
| Dim Agathion Bracelet: Leo Lv. 1~12 | Dim Agathion Bracelet: Leo Lv. 1~12 |
| Dim Agathion Bracelet: Gemini Lv. 1~12 | Dim Agathion Bracelet: Gemini Lv. 1~12 |
| Dim Agathion Bracelet: Scorpio Lv. 1~12 | Dim Agathion Bracelet: Scorpio Lv. 1~12 |
| Dim Agathion Bracelet: Aquarius Lv. 1~12 | Dim Agathion Bracelet: Aquarius Lv. 1~12 |
| Dim Agathion Bracelet: Pisces Lv. 1~12 | Dim Agathion Bracelet: Pisces Lv. 1~12 |
| Dim Agathion Bracelet: Sagittarius Lv. 1~12 | Dim Agathion Bracelet: Sagittarius Lv. 1~12 |
| Dim Agathion Bracelet: Cancer Lv. 1~12 | Dim Agathion Bracelet: Cancer Lv. 1~12 |
| Dim High-grade Agathion Bracelet: Virgo Lv. 1~5 | Dim High-grade Agathion Bracelet: Virgo Lv. 1~5 |
| Dim High-grade Agathion Bracelet: Capricorn Lv. 1~5 | Dim High-grade Agathion Bracelet: Capricorn Lv. 1~5 |
| Dim High-grade Agathion Bracelet: Libra Lv. 1~5 | Dim High-grade Agathion Bracelet: Libra Lv. 1~5 |
| Dim High-grade Agathion Bracelet: Aries Lv. 1~5 | Dim High-grade Agathion Bracelet: Aries Lv. 1~5 |
| Dim High-grade Agathion Bracelet: Taurus Lv. 1~5 | Dim High-grade Agathion Bracelet: Taurus Lv. 1~5 |
| Dim High-grade Agathion Bracelet: Leo Lv. 1~5 | Dim High-grade Agathion Bracelet: Leo Lv. 1~5 |
| Dim High-grade Agathion Bracelet: Gemini Lv. 1~5 | Dim High-grade Agathion Bracelet: Gemini Lv. 1~5 |
| Dim High-grade Agathion Bracelet: Scorpio Lv. 1~5 | Dim High-grade Agathion Bracelet: Scorpio Lv. 1~5 |
| Dim High-grade Agathion Bracelet: Aquarius Lv. 1~5 | Dim High-grade Agathion Bracelet: Aquarius Lv. 1~5 |
| Dim High-grade Agathion Bracelet: Pisces Lv. 1~5 | Dim High-grade Agathion Bracelet: Pisces Lv. 1~5 |
| Dim High-grade Agathion Bracelet: Sagittarius Lv. 1~5 | Dim High-grade Agathion Bracelet: Sagittarius Lv. 1~5 |
| Dim High-grade Agathion Bracelet: Cancer Lv. 1~5 | Dim High-grade Agathion Bracelet: Cancer Lv. 1~5 |

- ⬜ Some Dim Starlight Agathions are now incompatible with their regular counterparts and can no longer be equipped at the same time

| Current Agathions | Cannot be equipped together with Dim Agathions. |
|---|---|
| Agathion Charm: Virgo; Greater Agathion Charm: Virgo | No restrictions |
| Agathion Charm: Capricorn; Greater Agathion Charm: Capricorn | Dim Starlight Agathion Charm: Taurus Lv. 1~12; Dim Starlight Agathion Charm: Pisces Lv. 1~12; Dim Starlight Agathion Charm: Capricorn Lv. 1~12; Dim Agathion Bracelet: Taurus Lv. 1~12; Dim Agathion Bracelet: Pisces Lv. 1~12; Dim Agathion Bracelet: Capricorn Lv. 1~12 |
| Agathion Charm: Libra; Greater Agathion Charm: Libra | Dim Starlight Agathion Charm: Aries Lv. 1~12; Dim Starlight Agathion Charm: Taurus Lv. 1~12; Dim Starlight Agathion Charm: Cancer Lv. 1~12; Dim Agathion Bracelet: Aries Lv. 1~12; Dim Agathion Bracelet: Taurus Lv. 1~12; Dim Agathion Bracelet: Cancer Lv. 1~12 |
| Agathion Charm: Aries; Greater Agathion Charm: Aries | Dim Starlight Agathion Charm: Aquarius Lv. 1~12; Dim Starlight Agathion Charm: Pisces Lv. 1~12; Dim Agathion Bracelet: Aquarius Lv. 1~12; Dim Agathion Bracelet: Pisces Lv. 1~12 |
| Agathion Charm: Taurus; Greater Agathion Charm: Taurus | Dim Starlight Agathion Charm: Cancer Lv. 1~12; Dim Agathion Bracelet: Cancer Lv. 1~12 |
| Agathion Charm: Leo; Greater Agathion Charm: Leo | Dim Starlight Agathion Charm: Taurus Lv. 1~12; Dim Starlight Agathion Charm: Gemini Lv. 1~12; Dim Agathion Bracelet: Taurus Lv. 1~12; Dim Agathion Bracelet: Gemini Lv. 1~12 |
| Agathion Charm: Gemini; Greater Agathion Charm: Gemini | Dim Starlight Agathion Charm: Taurus Lv. 1~12; Dim Starlight Agathion Charm: Leo Lv. 1~12; Dim Starlight Agathion Charm: Pisces Lv. 1~12; Dim Agathion Bracelet: Taurus Lv. 1~12; Dim Agathion Bracelet: Leo Lv. 1~12; Dim Agathion Bracelet: Pisces Lv. 1~12 |
| Agathion Charm: Scorpio; Greater Agathion Charm: Scorpio | Dim Starlight Agathion Charm: Virgo Lv. 1~12; Dim Starlight Agathion Charm: Capricorn Lv. 1~12; Dim Starlight Agathion Charm: Cancer Lv. 1~12; Dim Agathion Bracelet: Virgo Lv. 1~12; Dim Agathion Bracelet: Capricorn Lv. 1~12; Dim Agathion Bracelet: Cancer Lv. 1~12 |
| Agathion Charm: Aquarius; Greater Agathion Charm: Aquarius | Dim Starlight Agathion Charm: Taurus Lv. 1~12; Dim Starlight Agathion Charm: Gemini Lv. 1~12; Dim Agathion Bracelet: Taurus Lv. 1~12; Dim Agathion Bracelet: Gemini Lv. 1~12 |
| Agathion Charm: Pisces; Greater Agathion Charm: Pisces | Dim Starlight Agathion Charm: Virgo Lv. 1~12; Dim Starlight Agathion Charm: Gemini Lv. 1~12; Dim Agathion Bracelet: Virgo Lv. 1~12; Dim Agathion Bracelet: Gemini Lv. 1~12 |
| Agathion Charm: Sagittarius; Greater Agathion Charm: Sagittarius | Dim Starlight Agathion Charm: Taurus Lv. 1~12; Dim Starlight Agathion Charm: Scorpio Lv. 1~12; Dim Starlight Agathion Charm: Cancer Lv. 1~12; Dim Agathion Bracelet: Taurus Lv. 1~12; Dim Agathion Bracelet: Scorpio Lv. 1~12; Dim Agathion Bracelet: Cancer Lv. 1~12 |
| Agathion Charm: Cancer; Greater Agathion Charm: Cancer | Dim Starlight Agathion Charm: Virgo Lv. 1~12; Dim Starlight Agathion Charm: Libra Lv. 1~12; Dim Starlight Agathion Charm: Aquarius Lv. 1~12; Dim Agathion Bracelet: Virgo Lv. 1~12; Dim Agathion Bracelet: Libra Lv. 1~12; Dim Agathion Bracelet: Aquarius Lv. 1~12 |
| Agathion Charm: Fire Dragon; Greater Agathion Charm: Fire Dragon | Dim Starlight Agathion Charm: Virgo Lv. 1~12; Dim Starlight Agathion Charm: Capricorn Lv. 1~12; Dim Starlight Agathion Charm: Libra Lv. 1~12; Dim Starlight Agathion Charm: Aries Lv. 1~12; Dim Starlight Agathion Charm: Taurus Lv. 1~12; Dim Starlight Agathion Charm: Leo Lv. 1~12; Dim Starlight Agathion Charm: Gemini Lv. 1~12; Dim Starlight Agathion Charm: Scorpio Lv. 1~12; Dim Starlight Agathion Charm: Aquarius Lv. 1~12; Dim Starlight Agathion Charm: Pisces Lv. 1~12; Dim Starlight Agathion Charm: Capricorn Lv. 1~12; Dim Starlight Agathion Charm: Cancer Lv. 1~12; Dim Agathion Bracelet: Virgo Lv. 1~12; Dim Agathion Bracelet: Capricorn Lv. 1~12; Dim Agathion Bracelet: Libra Lv. 1~12; Dim Agathion Bracelet: Aries Lv. 1~12; Dim Agathion Bracelet: Taurus Lv. 1~12; Dim Agathion Bracelet: Leo Lv. 1~12; Dim Agathion Bracelet: Gemini Lv. 1~12; Dim Agathion Bracelet: Scorpio Lv. 1~12; Dim Agathion Bracelet: Aquarius Lv. 1~12; Dim Agathion Bracelet: Pisces Lv. 1~12; Dim Agathion Bracelet: Capricorn Lv. 1~12; Dim Agathion Bracelet: Cancer Lv. 1~12 |

- ⬜ After equipping a Dim Starlight Agathion, its active skill becomes available only after a 30-second delay
- ⬜ Dim Agathion Charms can be exchanged with a certain chance for regular ones through the Game Assistant Special in any major town

| Item (one selected at random) | Chance | Required for Exchange (one of any type, any level) |
|---|---|---|
| Agathion Charm: Virgo x1 | 8.3% | Dim Starlight Agathion Charm: Virgo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Libra Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aries Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Taurus Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Leo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Gemini Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Scorpio Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aquarius Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Pisces Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Cancer Lv. 1~12 x1 |
| Agathion Charm: Capricorn x1 | 8.3% | Dim Starlight Agathion Charm: Virgo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Libra Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aries Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Taurus Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Leo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Gemini Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Scorpio Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aquarius Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Pisces Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Cancer Lv. 1~12 x1 |
| Agathion Charm: Libra x1 | 8.3% | Dim Starlight Agathion Charm: Virgo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Libra Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aries Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Taurus Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Leo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Gemini Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Scorpio Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aquarius Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Pisces Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Cancer Lv. 1~12 x1 |
| Agathion Charm: Aries x1 | 8.3% | Dim Starlight Agathion Charm: Virgo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Libra Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aries Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Taurus Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Leo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Gemini Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Scorpio Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aquarius Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Pisces Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Cancer Lv. 1~12 x1 |
| Agathion Charm: Taurus x1 | 8.3% | Dim Starlight Agathion Charm: Virgo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Libra Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aries Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Taurus Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Leo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Gemini Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Scorpio Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aquarius Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Pisces Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Cancer Lv. 1~12 x1 |
| Agathion Charm: Leo x1 | 8.3% | Dim Starlight Agathion Charm: Virgo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Libra Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aries Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Taurus Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Leo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Gemini Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Scorpio Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aquarius Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Pisces Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Cancer Lv. 1~12 x1 |
| Agathion Charm: Gemini x1 | 8.3% | Dim Starlight Agathion Charm: Virgo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Libra Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aries Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Taurus Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Leo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Gemini Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Scorpio Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aquarius Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Pisces Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Cancer Lv. 1~12 x1 |
| Agathion Charm: Scorpio x1 | 8.3% | Dim Starlight Agathion Charm: Virgo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Libra Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aries Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Taurus Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Leo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Gemini Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Scorpio Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aquarius Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Pisces Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Cancer Lv. 1~12 x1 |
| Agathion Charm: Aquarius x1 | 8.4% | Dim Starlight Agathion Charm: Virgo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Libra Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aries Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Taurus Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Leo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Gemini Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Scorpio Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aquarius Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Pisces Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Cancer Lv. 1~12 x1 |
| Agathion Charm: Pisces x1 | 8.4% | Dim Starlight Agathion Charm: Virgo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Libra Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aries Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Taurus Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Leo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Gemini Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Scorpio Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aquarius Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Pisces Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Cancer Lv. 1~12 x1 |
| Agathion Charm: Sagittarius x1 | 8.4% | Dim Starlight Agathion Charm: Virgo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Libra Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aries Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Taurus Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Leo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Gemini Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Scorpio Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aquarius Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Pisces Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Cancer Lv. 1~12 x1 |
| Agathion Charm: Cancer x1 | 8.4% | Dim Starlight Agathion Charm: Virgo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Libra Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aries Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Taurus Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Leo Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Gemini Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Scorpio Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Aquarius Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Pisces Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Capricorn Lv. 1~12 x1; OR; Dim Starlight Agathion Charm: Cancer Lv. 1~12 x1 |

- ⬜ Dim Starlight Agathions can no longer be synthesized to increase their level or upgraded to the Enhanced grade through the Master Blacksmith
- ⬜ Some items used with agathions have become outdated. The names of these items have been changed

| Before | After |
|---|---|
| Damaged Starlight Agathion Evolution Stone | Damaged Starlight Agathion Evolution Stone |
| Damaged Starlight Jar | Damaged Starlight Jar |
| Damaged Red Starlight Jar | Damaged Red Starlight Jar |
| Damaged Book of Growth: Starlight Agathion Charm | Damaged Book of Growth: Starlight Agathion Charm |
| Damaged Book of Growth: Starlight Agathion Charm (Ancient) | Damaged Book of Growth: Starlight Agathion Charm (Ancient) |
| Damaged Book of Growth: Starlight Agathion Charm (Stable) | Damaged Book of Growth: Starlight Agathion Charm (Stable) |
| Damaged Red Star Solvent | Damaged Red Star Solvent |

- ⬜ All no-longer-used items can be exchanged with the Game Assistant Special on the side panel for the new item, Starlight Agathion Mark, at the following exchange rates:

| Item | Required |
|---|---|
| Damaged Starlight Agathion Evolution Stone | Starlight Agathion Mark x1 |
| Damaged Starlight Jar | Starlight Agathion Mark x10 |
| Damaged Starlight Jar | Starlight Agathion Mark x1 |
| Damaged Red Starlight Jar | Starlight Agathion Mark x2 |
| Damaged Book of Growth: Starlight Agathion Charm | Starlight Agathion Mark x1 |
| Damaged Book of Growth: Starlight Agathion Charm (Ancient) | Starlight Agathion Mark x1 |
| Damaged Book of Growth: Starlight Agathion Charm (Stable) | Starlight Agathion Mark x3 |
| Damaged Red Star Solvent | Starlight Agathion Mark x1 |

- ⬜ Double click on the Starlight Agathion Mark item in your inventory to exchange it for the following items

| Item | Required |
|---|---|
| Divine Book of Growth: Zodiac Agathion x1 | Starlight Agathion Mark x100 |
| Greater Zodiac Agathion's Book of Growth x1 | Starlight Agathion Mark x20 |
| Greater Zodiac Agathion's Sacred Book of Growth x1 | Starlight Agathion Mark x200 |
| Greater Zodiac Agathion Cube x1 | Starlight Agathion Mark x400 |
| Zodiac Agathion Cube x1 | Starlight Agathion Mark x10 |
| Beora's Ancient Energy x1 | Starlight Agathion Mark x1 |

- ⬜ Due to the change in agathions, the enchantment properties of the Augmenting Stone: Superior Seed Bracelet have been redesigned
- ⬜ The dependence of effects on the equipped type of starlight agathion has been removed

### Heir's Radiance Shard

- ⬜ The list of items available for exchange through the Game Assistant on the side panel to obtain Heir's Radiance Shard has been updated
- ⬜ The old exchanges have been replaced with new ones

| Item | Required |
|---|---|
| Heir's Radiance Shard x1 | Forgotten Spellbook - Chapter 1 x1 |
| Heir's Radiance Shard x1 | Artifact Crystal x1 |
| Heir's Radiance Shard x1 | Artifact Fragment x1 |
| Heir's Radiance Shard x1 | Crystal of Dyes x1 |
| Heir's Radiance Shard x1 | Jewel Energy x1 |
| Heir's Radiance Shard x1 | Energy of Protection x1 |

## Hunting Zones

### Throne of Heroes

- ⬜ Added two new raid bosses to the Throne of Heroes clan instance zone

| Instance zone name | Minimum clan level | Minimum character level |
|---|---|---|
| Throne of Heroes - Ferion | Lv. 12+ | Lv. 100+ |
| Throne of Heroes - Toros | Lv. 13+ | Lv. 100+ |

- ⬜ To enter Throne of Heroes, talk to Roiental Throne of Heroes' Manager in Rune
- ⬜ The entrance requirements, bound time and number of participants are the same as before
- ⬜ Added a new item to the clan shop. It is needed for the Ferion boss raid

| Item | Description | Conditions for unlocking | Price |
|---|---|---|---|
| Reverence Veil | A special veil for those who dare challenge the Archangel. The veil allows to seal Ferion's Praetorians and disrupt the Ritual of Light.; Note!; Can be used only in the Throne of Heroes - Ferion clan instance zone against Ferion's Praetorians who lost Ferion's Protection. | Clan Lv. 12+; Specialties: no requirements | Personal Reputation x1,000 |

- ⬜ Ferion is an archangel that punishes sinners with the power of light. He summons Praetorians, performs the Ritual of Light and clears the battlefield with AoE attacks
- ⬜ When Ferion's HP goes below a certain threshold, he summons Praetorians and starts the Ritual of Light
- ⬜ When the Ritual begins, Praetorians get the Blessing of Ferion that sets a limit for the received damage

| Skill Name | Description |
|---|---|
| Ferion's Protection Lv. 1 | During the Ritual of Light, Ferion's Praetorians are under his protection. Yet, if their HP gets lower than 20%%, they will lose Ferion's Protection.; Received damage limit: 200,000. Received damage limit from basic attacks with dual swords/ dual daggers/ dual blunt weapons/ fist weapons: 100,000. |
| Ferion's Protection Lv. 2 | During the Ritual of Light, Ferion's Praetorians are under his protection. Yet, if their HP gets lower than 20%%, they will lose Ferion's Protection.; Received damage limit: 111,111. Received damage limit from basic attacks with dual swords/ dual daggers/ dual blunt weapons/ fist weapons: 55,555. |
| Ferion's Protection Lv. 3 | During the Ritual of Light, Ferion's Praetorians are under his protection. Yet, if their HP gets lower than 20%%, they will lose Ferion's Protection.; Received damage limit: 33,333. Received damage limit from basic attacks with dual swords/ dual daggers/ dual blunt weapons/ fist weapons: 16,666. |

- ⬜ When Praetorians' HP goes below 20%, Blessing of Ferion is lifted and they can be sealed with the Reverence Veil
- ⬜ To stop the ritual, you need to seal all the Praetorians
- ⬜ If you don't stop the ritual, Ferion will recover the lost health and receive a powerful buff
- ⬜ The following items can be received as rewards for defeating Ferion

| Items rewarded for completing Throne of Heroes - Ferion | Items rewarded for completing Throne of Heroes - Ferion | Items rewarded for completing Throne of Heroes - Ferion | Items rewarded for completing Throne of Heroes - Ferion | Items rewarded for completing Throne of Heroes - Ferion | Items rewarded for completing Throne of Heroes - Ferion |
|---|---|---|---|---|---|
| Enhanced Rune Stone | Lindvior's Earring | Valakas' Blessed Necklace | Antharas' Blessed Earring | Wisdom Crystal | Crystal of Dawn |
| Tersi's Soul Crystal Lv. 3/4/5/6 | Masia's Soul Crystal Lv. 3/4/5/6 | Pa'agrio's Soul Crystal Lv. 3/4/5/6 | Eva's Soul Crystal - Lv. 3/4/5/6 | +1/3/5 Attack Artifact Pack | +1/3/5 Defense Artifact Pack |
| +1/3/5 Support Artifact Pack | +1/3/5 Balance Artifact Pack | Krishna Weapon | Leviathan Armor | Rune Stone | Selenite |
| Fioren's Crystal Prison's Time Stone | Jamoa's Camp's Time Stone | Pantheon's Museum's Time Stone | Crystal of Dyes | Artifact Fragment | Artifact Crystal |

- ⬜ Toros is a demon that uses blood to create clones of himself and controls the battlefield with AoE attacks
- ⬜ Toros summons three clones made from blood
- ⬜ Each clone has specific passive skills and a certain number of HP
- ⬜ For the first two clones, the received damage is limited. The first one has a lot of HP but low defense, the second has relatively little HP but is immune to ranged attacks
- ⬜ The last clone has little HP but great defense. He casts Toros' Clone Barrier that makes him invincible when he takes damage
- ⬜ The following items can be received as rewards for defeating Toros

| Items rewarded for completing Throne of Heroes - Toros | Items rewarded for completing Throne of Heroes - Toros | Items rewarded for completing Throne of Heroes - Toros | Items rewarded for completing Throne of Heroes - Toros | Items rewarded for completing Throne of Heroes - Toros | Items rewarded for completing Throne of Heroes - Toros |
|---|---|---|---|---|---|
| Enhanced Rune Stone | Lindvior's Earring | Valakas' Blessed Necklace | Antharas' Blessed Earring | Wisdom Crystal | Crystal of Dawn |
| Tersi's Soul Crystal Lv. 4/5/6/7 | Masia's Soul Crystal Lv. 4/5/6/7 | Pa'agrio's Soul Crystal Lv. 4/5/6/7 | Eva's Soul Crystal - Lv. 4/5/6/7 | +1/3/5/7 Attack Artifact Pack | +1/3/5/7 Defense Artifact Pack |
| +1/3/5/7 Support Artifact Pack | +1/3/5/7 Balance Artifact Pack | Krishna Weapon | Leviathan Armor | Rune Stone | Selenite |
| Fioren's Crystal Prison's Time Stone | Jamoa's Camp's Time Stone | Pantheon's Museum's Time Stone | Crystal of Dyes | Artifact Fragment | Artifact Crystal |

- ⬜ Changed the minimum level required to enter the Throne of Heroes - Tauti zone

| Before |  | Now |
|---|---|---|
| Lv. 13+ | to | Lv. 11+ |

- ⬜ Modified the balance and rewards of the current Throne of Heroes: Goldberg, Mary Reed and Tauti raids
- ⬜ Boss levels and their characteristics have been increased to reflect the updated stats

| Name | Level | Changes |
|---|---|---|
| Goldberg Ancient Rogue | Lv. 116 to Lv. 122 | Throne's Treasure Chest; R99-grade equipment no longer drops.; The following items have been added: Lv. 3 Soul Crystals (Tersi, Masia, Pa'agrio and Eva); Artifact Fragment; +1 Artifact Chests |
| Mary Reed Pirate Queen | Lv. 120 to Lv. 124 | Throne's Treasure Chest; R99-grade equipment no longer drops.; The following items have been added: Lv. 3 - 4 Soul Crystals (Tersi, Masia, Pa'agrio and Eva); Artifact Fragment; +1 - +3 Artifact Chests |
| Tauti Hellfire Monarch | Lv. 125 to Lv. 126 | Throne's Treasure Chest; R99-grade equipment no longer drops.; The following items have been added: Lv. 3 - 5 Soul Crystals (Tersi, Masia, Pa'agrio and Eva); Crystal of Dawn; Wisdom Crystal; Artifact Fragment; +1 - +5 Artifact Chests |

### Gainak Arena

- ⬜ Added a new daily party instance zone Gainak Arena

| Entry level | Lv. 105+ |
|---|---|
| Number of participants | 2~7 characters |
| Entrance NPC | Benusta Manager of Party Instance Zones |
| Entry | Once a day |
| Reset time | 06:30 GMT+3 |

- ⬜ After entering, talk to Benusta Manager of Party Instance Zones to choose a difficulty level
- ⬜ After choosing the difficulty, the character is bound to the instance zone and cannot alter their choice
- ⬜ Gainak Arena consists of three battles with bosses. After you defeat the final boss, you will receive rewards
- ⬜ The rewards for every difficulty level are listed below

| Difficulty | Boss monster | Victory Reward |
|---|---|---|
| Lv. 110 | Plague Executioner Kajan; Sinister Messenger Grabol; Ghost Knight Vexus | Benusta's Reward Pack Lv. 1 x1; Novice Wanderer's Tales x2 |
| Lv. 120 | Plague Executioner Lidakin; Sinister Messenger Talgor; Ghost Knight Sindrick | Benusta's Reward Pack Lv. 3 x1; Novice Wanderer's Tales x2 |
| Lv. 130 | Plague Executioner Belfus; Sinister Messenger Galatan; Ghost Knight Godiak | Benusta's Reward Pack Lv. 5 x1; Novice Wanderer's Tales x2 |

- ⬜ Due to the addition of the new Gainak Arena zone, you can no longer enter the daily party instance zone Nightmare Kamaloka
- ⬜ Removed the entry button from Benusta Manager of Party Instance Zones's dialog
- ⬜ Replaced the Nightmare Kamaloka monsters with the Gainak Arena monsters in the target list of the Hunt in a Party Instance Zone clan mission

### Canon Cathedral

- ⬜ Added a new daily party instance zone Canon Cathedral

| Entry level | Lv. 105+ |
|---|---|
| Number of participants | 2~7 characters |
| Entrance NPC | Benusta Manager of Party Instance Zones |
| Entry | Once a day |
| Reset time | 06:30 GMT+3 |

- ⬜ After entering, talk to Benusta Manager of Party Instance Zones to choose a difficulty level
- ⬜ After choosing the difficulty, the character is bound to the instance zone and cannot alter their choice
- ⬜ The Canon Cathedral consists of 4 sealed rooms. To move to the next room, you need to destroy the stone statue in the center of the room you are in
- ⬜ When you attack the statue, minion monsters appear. When you encounter a boss in spirit state (can't be attacked), the difficulty of the sealed room sharply increases
- ⬜ After you destroy all statues, a boss will appear in the final room. Defeat them to obtain rewards
- ⬜ The rewards for every difficulty level are listed below

| Difficulty | Boss monster | Victory Reward |
|---|---|---|
| Lv. 110 | Corrupted High Priest Velkir | Benusta's Reward Pack Lv. 1 x1; Novice Wanderer's Tales x2 |
| Lv. 120 | Corrupted High Priest Radiges | Benusta's Reward Pack Lv. 3 x1; Novice Wanderer's Tales x2 |
| Lv. 130 | Corrupted High Priest Aquilon | Benusta's Reward Pack Lv. 5 x1; Novice Wanderer's Tales x2 |

- ⬜ Due to the addition of the new Canon Cathedral zone, you can no longer enter the daily party instance zone Ashen Shadow Camp
- ⬜ Removed the entry button from Benusta Manager of Party Instance Zones's dialog
- ⬜ Replaced the Ashen Shadow Camp monsters with the Canon Cathedral monsters in the target list of the Hunt in a Party Instance Zone clan mission

### Raid Bosses

- ⬜ Added new raid boss Baluan to the Baluan Battleground
- ⬜ Baluan appears every Thursday at 21:00 (server time)
- ⬜ My Teleport, summoning party members and restarting are prohibited on Baluan Battleground
- ⬜ Baluan disappears after 30 min.
- ⬜ The conditions for receiving Baluan raid rewards are listed below

| Available items | Requirements |
|---|---|
| Pack of Triumph over Demon of Dissent Baluan | Up to 200 characters who dealt the most damage |
| Demon of Dissent Baluan's Last Hit Reward Pack | The character who delivered the final blow among those who have dealt the most damage |
| Demon of Dissent Baluan's Damage Reward Pack - 1st Place | The character who has dealt the most damage |
| Demon of Dissent Baluan's Damage Reward Pack - 2nd Place | The character that dealt the second-most damage |
| Demon of Dissent Baluan's Damage Reward Pack - 3rd~10th Place | The character that dealt from third- to tenth-most damage |

- ⬜ The command channel or party that contributed the most to defeating Baluan will be given priority in looting the trophies for 15 sec.
- ⬜ Trophies for defeating Baluan are listed below

| List of items that you can receive | List of items that you can receive | List of items that you can receive | List of items that you can receive |
|---|---|---|---|
| Heavenly Talisman | Epic Talisman | Gran Kain's Scroll: Enchant L-grade Weapon | Gran Kain's Scroll: Enchant L-grade Armor |
| Orb of Fire | Orb of Water | Orb of Air | Orb of Earth |
| Venir's; Talisman Lv. 24 | Sayha's Enhanced; Talisman Lv. 10 | Enhanced Talisman of; Protection Lv. 10 | Eraton's Dye Lv. 5; (M. Critical Damage) |
| Eraton's Dye Lv. 5; (P. Atk.) | Eraton's Dye Lv. 5; (M. Atk.) | Eraton's Dye Lv. 5 (Critical Damage for Basic Attacks) | Eraton's Dye Lv. 5 (P. Skill Critical Damage) |
| Eraton's Dye Lv. 5 (Basic Attack Power) | Eraton's Dye Lv. 5 (M. Skill Power) | Eraton's Dye Lv. 5 (P. Skill Power) | Shillien's Soul Crystal; Lv. 10 |
| Tersi's Soul; Crystal Lv. 10 | Masia's Soul; Crystal Lv. 10 | Pa'agrio's Soul; Crystal - Lv. 10 | Eva's Soul; Crystal - Lv. 10 |

- ⬜ Due to the introduction of the new field boss Baluan, the appearance timetable of the existing field boss Atrus has been changed

| Before |  | After |
|---|---|---|
| Appears every Tuesday and Thursday at 21:00 (server time). | to | Appears every Tuesday at 21:00 (server time). |
| Disappears after 20 min. | to | Disappears after 30 min. |

- ⬜ Modified field boss Atrus' stats and trophies
- ⬜ Increased P. Atk. and Skill Power. Added debuff immunity
- ⬜ Rewards for defeating the bosses have been changed accordingly
- ⬜ The command channel or party that contributed the most to defeating Atrus will be given priority in looting the trophies for 15 sec.
- ⬜ Trophies for defeating Atrus are listed below

| List of items that you can receive | List of items that you can receive | List of items that you can receive | List of items that you can receive |
|---|---|---|---|
| Heavenly Talisman | Epic Talisman | Gran Kain's Scroll: Enchant L-grade Weapon | Gran Kain's Scroll: Enchant L-grade Armor |
| Orb of Fire | Orb of Water | Orb of Air | Orb of Earth |
| Venir's; Talisman Lv. 24 | Sayha's Enhanced; Talisman Lv. 10 | Enhanced Talisman of; Protection Lv. 10 | Eraton's Dye Lv. 5; (M. Critical Damage) |
| Eraton's Dye Lv. 5; (P. Atk.) | Eraton's Dye Lv. 5; (M. Atk.) | Eraton's Dye Lv. 5 (Critical Damage for Basic Attacks) | Eraton's Dye Lv. 5 (P. Skill Critical Damage) |
| Eraton's Dye Lv. 5 (Basic Attack Power) | Eraton's Dye Lv. 5 (M. Skill Power) | Eraton's Dye Lv. 5 (P. Skill Power) | Shillien's Soul Crystal; Lv. 10 |
| Tersi's Soul; Crystal Lv. 10 | Masia's Soul; Crystal Lv. 10 | Pa'agrio's Soul; Crystal - Lv. 10 | Eva's Soul; Crystal - Lv. 10 |

- ⬜ Modified the balance of some raid bosses in the field hunting zones

| Boss | Changes |
|---|---|
| Baluan | Increased stats and modified the combat rules. |
| Atrus | Increased stats and modified the combat rules. |
| Valakas | Increased stats. |

- ⬜ Corrected the pattern and balance of the Limit Barrier skill used by the Valakas Fire Dragon boss
- ⬜ Changed the items awarded after a successful raid against Valakas Fire Dragon

| Newly available items | Newly available items | Newly available items |
|---|---|---|
| Dark Dragon's Necklace | Dark Dragon's Earring | Dark Dragon's Ring |
| Eraton's Dye Lv. 5 | Shillien's Soul Crystal Lv. 10 | Tersi's Soul Crystal Lv. 10 |
| Masia's Soul Crystal Lv. 10 | Pa'agrio's Soul Crystal Lv. 10 | Eva's Soul Crystal Lv. 10 |

- ⬜ Fixed the issue that sometimes prevented the Chaos Golems' War Machine Guardian monster from appearing during the raid in the Plunderous Plains

### Instance Zone Rewards

- ⬜ The reward system of party instance zones Sakura Isle and Chaos' Air Fortress has been altered
- ⬜ The reward system has been improved. From now on, party members who haven't dealt any damage will still receive rewards
- ⬜ Now, even if the group is disbanded during the battle with the final boss, its members still receive rewards if they are within the zone boundaries when the boss dies
- ⬜ If the character is not in the instance zone when the reward is issued, they will be unable to get it

### Other Hunting Zone Changes

- ⬜ Added additional hunting zone categorization to the Info menu on the map

| Hunting zone type | Icon appearance |
|---|---|
| Solo Hunting Zone | Blue |
| Party hunting zone | Yellow |
| Improved hunting zone | Red |
| Conquest | Purple |

- ⬜ Reorganized the levels and types of monsters in the party hunting zones
- ⬜ The existing party hunting zones have been separated into regular and improved
- ⬜ Monsters in the improved hunting zones are stronger, and the rewards are better than in regular hunting zones
- ⬜ Monster levels and the balance of each hunting zone have been modified according to its new classification
- ⬜ Switch between the tabs below to see the distribution by type
- ⬜ Party Hunting Zones

**Improved Hunting Zones**

| Hunting Zone | Level |
|---|---|
| Neutral Zone | 112 |
| Wasteland | 114 |
| Cruma Marshlands | 116 |
| Valley of Saints | 118 |
| Ketra Orc Outpost | 120 |
| Varka Silenos Barracks | 120 |
| Dragon Valley - West | 122 |
| Dragon Valley - East | 124 |
| Execution Grounds | 126 |
| Langk Lizardmen Barracks | 128 |
| Langk Lizardmen Temple | 130 |
| Hunting Zone | Level |
|---|---|
| Sea of Spores | 122 |
| Beast Farm | 124 |
| Fafurion Temple | 126 |
| Immortal Plateau | 126 |
| Snowy Volcano | 128 |
| Kasha Orcs' Logging Site | 128 |
| Baluan Battleground | 130 |
| Kasha Orc Fortress | 130 |

- ⬜ You may receive Chaos Aether with a certain chance for killing monsters with the Chaos Power buff in the improved hunting zones

| Name | Description |
|---|---|
| Chaos Power | A mysterious energy of Chaos. |

- ⬜ The placement of monsters in the following hunting zones has been changed according to the zone's new type and level

| Hunting Zone |
|---|
| Snowy Volcano |
| Baluan Battleground |
| Langk Lizardmen Barracks |
| Langk Lizardmen Temple |

- ⬜ Some Defender NPCs will be removed from hunting zones
- ⬜ Removed the passive skill that increased the combat stats of monsters in party hunting zones during the night
- ⬜ Modified the amount of Adena awarded for killing monsters in some field hunting zones according to their level and type

| Party Hunting Zones | Improved Hunting Zones |
|---|---|
| Langk Lizardmen Barracks (Lv. 128) | Sea of Spores (Lv. 122) |
| Langk Lizardmen Temple (Lv. 130) | Beast Farm (Lv. 124) |
|  | Fafurion Temple (Lv. 126) |
|  | Immortal Plateau (Lv. 126) |
|  | Kasha Orcs' Logging Site (Lv. 128) |
|  | Snowy Volcano (Lv. 128) |
|  | Baluan Battleground (Lv. 130) |

- ⬜ You can now leave session zones instantly, pressing the exit button in the peaceful zone even if the character is in combat state
- ⬜ In all session zones, the starting point is now peaceful
- ⬜ Teleport and summon restrictions have been added to certain areas of the Langk Lizardmen Temple zone to prevent characters from climbing onto objects
- ⬜ The drop rates of Pantheon's Museum's Time Stone, Jamoa's Camp's Time Stone and Fioren's Crystal Prison's Time Stone from monsters in the instanced zones Pantheon's Museum, Jamoa's Camp (Lv. 120+), and Fioren's Crystal Prison (Lv. 120+) have been adjusted
- ⬜ Evasion Reduction has been removed from all hunting zones

## System Changes

### UI

- ⬜ Modified the Contacts menu UI
- ✅ Improved the design of the Contacts window to make the icons and buttons more readable *(client)*
- ⬜ To send a private message through Chat with Friends, double-click the character's name in the friend list

### Craft

- ⬜ Added scrolls for enchanting exalted equipment to the smithy

| Craftable Item | Crafting Fee |
|---|---|
| Blessed Scroll: Enchant Exalted Weapon x1 | Aether x1,500; Black Mithril x500 |
| Scroll: Enchant Blessed Exalted Armor x1 | Aether x500; Oriharukon Ore x250; Dragon Scale x250; Angel's Feather x250 |

- ⬜ Changed the materials needed for crafting Orb of Water Elysia

| Before | After |
|---|---|
| Superior Red Cat's Eye Lv. 10 x2; Superior Blue Cat's Eye Lv. 10 x2; Wisdom Crystal x370 | Superior Red Cat's Eye Lv. 10 x1; Superior Blue Cat's Eye Lv. 10 x1; Wisdom Crystal x370 |

- ⬜ Characters who already own an Orb of Water Elysia will receive Superior Red Cat's Eye Lv. 10 x1 and Superior Blue Cat's Eye Lv. 10 x1

### Einhasad's Store

- ⬜ Added new items to Einhasad's Store

| Item Name | Purchase Limit | Fee |
|---|---|---|
| Navari's Mask |  | Einhasad's Golden Coin x2,000 |
| Master Fishing Rod Pack Lv. 5 30-day |  | Einhasad's Golden Coin x1,500 |
| Jewel Energy |  | Einhasad's Golden Coin x120 |
| Sayha's Energy |  | Einhasad's Golden Coin x300 |
| Artifact Fragment |  | Einhasad's Golden Coin x300 |
| Rune Stone |  | Einhasad's Golden Coin x150 |
| Supply Pack (2-hour) | 12 pcs. per account; (per day) | Einhasad's Golden Coin x100 |
| Wisdom Crystal |  | Chaos Aether x66 |
| Solaris | 2 pc. per account (per day) | Adena x20,000,000 |

- ⬜ The prices of some items in Einhasad's Store have been changed

| Item Name | Before |  | After |
|---|---|---|---|
| Energy of Protection | Einhasad's Golden Coin x180 | to | Einhasad's Golden Coin x140 |
| Artifact Crystal | Einhasad's Golden Coin x315 | to | Einhasad's Golden Coin x300 |

- ⬜ The purchase limit of some items in Einhasad's Store has been changed

| Item Name | Before |  | After |
|---|---|---|---|
| Sealed Fire Source Pack (25 pcs.) | 1 pc. per account (per day) | to | 2 pcs. per account (per day) |
| Divine Fire Pack (10 pcs.) | 1 pc. per account (per day) | to | 2 pcs. per account (per day) |
| Fire Orb Pack (100 pcs.) | 1 pc. per account (per day) | to | 2 pcs. per account (per day) |

## Changes

- ⬜ Fixed the issue that prevented the Samurai's appearance from changing when equipping different weapons
- ⬜ Fixed the issue that prevented Samurai class characters from seeing the movement direction when using a Sayune
- ✅ Fixed the issue that prevented the visual effect from being displayed when Samurai and Warg class characters used Dragon Weapons *(client)*
- ⬜ Fixed the issue that made it possible to move objects in some areas of Langk Lizardmen Temple
- ⬜ Fixed the issue that caused the location name to be displayed as Snowy Volcano in some areas of Baluan Battleground
- ⬜ Fixed the issue that sometimes made it impossible to accept the quest after entering the Imperial Tomb session zone
- ✅ Fixed the issue that caused the character to move after clicking the Potion Auto-use icon *(client)*
- ✅ Fixed the issue that prevented skill icons from being displayed as unavailable after transformation *(client)*
- ✅ Fixed the issue that caused the quantity of items in the Play Report to be different from the number displayed in the tooltip *(client)*
- ⬜ Fixed the issue that made it possible to obtain an Augmenting Stone Pack from Baluan's Reward Pack
- ⬜ Fixed the issue that made it impossible to zoom in or out of the radar in cities
- ⬜ Fixed the issue that caused the damage text to not be displayed correctly on Perfect hit
- ⬜ Fixed the issue that prevented Ramona Empress's health bar from being visible after the cutscene during the fight in the Empress' Palace
- ⬜ Fixed an issue that caused the item that is needed to use the Build Headquarters skill to be displayed incorrectly
- ⬜ Fixed the issue that caused the Reset button to be available in the item enchantment window even if no items had been selected
- ✅ Fixed the issue that caused the Giant's Critical Weakness effect description in the item tooltips to be different from the actual effect *(client)*
- ⬜ Fixed the issue that caused "Skill Power" bonus to be displayed in the descriptions of items augmented with the Chaos Augmenting Stone: Weapon/ Armor instead of "all attack power" bonus
- ⬜ Corrected the exchange NPC in the description of Agathion Guardian's Fragment
- ⬜ Fixed the issue that sometimes caused the Relics window to lag when opening
- ✅ Changed the tooltip that appeared when you enter the Conquest character name. It now reads "Cannot be changed" instead of "Cannot be used" *(client)*
- ⬜ Fixed the issue that caused the Settings button to be duplicated in the menu
- ⬜ Fixed the issue that prevented the highest artifact enhancement effect from being displayed in the detailed info window
- ⬜ Fixed the issue that caused attacks from behind and from the side to deal excessive damage in certain conditions
- ⬜ When attacking from the back or from the side, the base damage bonus is now applied equally
- ⬜ The effects that increase the damage when attacking from behind and from the side are temporarily limited
- ⬜ These effects will be reworked in the future updates
