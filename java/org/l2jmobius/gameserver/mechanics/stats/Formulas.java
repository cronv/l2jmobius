/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.mechanics.stats;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.data.xml.HitConditionBonusData;
import org.l2jmobius.gameserver.data.xml.KarmaLossData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Position;
import org.l2jmobius.gameserver.entity.actor.instance.SiegeFlag;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.BasicProperty;
import org.l2jmobius.gameserver.mechanics.skill.enums.DispelSlotType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExMagicAttackInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.MathUtil;

/**
 * This class provides global calculation logic for various game mechanics.<br>
 * It contains utility methods used to determine stats, damage, and other numerical values across the server.
 */
public class Formulas
{
	/** Regeneration Task period. */
	private static final int HP_REGENERATE_PERIOD = 3000; // 3 secs
	
	public static final byte SHIELD_DEFENSE_FAILED = 0; // no shield defense
	public static final byte SHIELD_DEFENSE_SUCCEED = 1; // normal shield defense
	public static final byte SHIELD_DEFENSE_PERFECT_BLOCK = 2; // perfect block
	
	public static final int SKILL_LAUNCH_TIME = 500; // The time to pass after the skill launching until the skill to affect targets. In milliseconds.
	private static final byte MELEE_ATTACK_RANGE = 40;
	
	/**
	 * Retrieves the health regeneration period for a specific {@code Creature}.<br>
	 * This method returns a modified value if the creature is a door.
	 * @param creature The {@code Creature} to check.
	 * @return The regeneration period as an {@code int}.
	 */
	public static int getRegeneratePeriod(Creature creature)
	{
		return creature.isDoor() ? HP_REGENERATE_PERIOD * 100 : HP_REGENERATE_PERIOD;
	}
	
	/**
	 * Calculates the final physical damage dealt by an attacker to a target.<br>
	 * This method accounts for defense, critical hits, traits, and class balances.<br>
	 * It also applies modifiers for backstabs, skill power, and special statuses.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} being used to perform the attack.
	 * @param backstab A boolean indicating if the attack is a backstab.
	 * @param power The base power value of the skill.
	 * @param shld The shield defense status of the target.
	 * @param ss A boolean indicating if the special shot bonus applies.
	 * @return The final calculated damage as a {@code double}.
	 */
	public static double calcBlowDamage(Creature attacker, Creature target, Skill skill, boolean backstab, double power, byte shld, boolean ss)
	{
		double defence = target.getPDef();
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				defence += target.getShldDef();
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1;
			}
		}
		
		final CreatureStat attackerStat = attacker.getStat();
		final CreatureStat targetStat = target.getStat();
		
		// Critical
		final double criticalMod = attackerStat.getValue(Stat.CRITICAL_DAMAGE, 1);
		final double criticalPositionMod = attackerStat.getPositionTypeValue(Stat.CRITICAL_DAMAGE, Position.getPosition(attacker, target));
		final double criticalVulnMod = Math.max(0.2, 2 - targetStat.getValue(Stat.DEFENCE_CRITICAL_DAMAGE, 1)); // The limit for reducing critical damage is 80% (0.2).
		final double criticalAddMod = attackerStat.getValue(Stat.CRITICAL_DAMAGE_ADD, 0);
		final double criticalAddVuln = targetStat.getValue(Stat.DEFENCE_CRITICAL_DAMAGE_ADD, 0);
		final double criticalSkillMod = calcCritDamage(attacker, target, skill) / 2;
		
		// Trait, elements
		final double weaponTraitMod = calcWeaponTraitBonus(attacker, target);
		final double generalTraitMod = calcGeneralTraitBonus(attacker, target, skill.getTraitType(), true);
		final double weaknessMod = calcWeaknessBonus(attacker, target, skill.getTraitType());
		final double attributeMod = calcAttributeBonus(attacker, target, skill);
		final double randomMod = attacker.getRandomDamageMultiplier();
		final double pvpPveMod = calculatePvpPveBonus(attacker, target, skill, true);
		
		// Initial damage
		final double ssmod = ss ? (2 * attackerStat.getValue(Stat.SHOTS_BONUS)) - (targetStat.getValue(Stat.SPIRITSHOT_RESISTANCE, 0) / 100) : 1; // 2.04 for dual weapon?
		final double cdMult = criticalMod * (((criticalPositionMod - 1) / 2) + 1) * (((criticalVulnMod - 1) / 2) + 1);
		final double cdPatk = (criticalAddMod + criticalAddVuln) * criticalSkillMod;
		final Position position = Position.getPosition(attacker, target);
		final double isPosition = position == Position.BACK ? 0.2 : position == Position.SIDE ? 0.05 : 0;
		
		// Mobius: Manage level difference.
		// if (attacker.getLevel() < target.getLevel())
		// {
		// power *= 1 - (Math.min(target.getLevel() - attacker.getLevel(), 9) / 10);
		// }
		
		double balanceMod = 1;
		if (attacker.isPlayable())
		{
			final int playerId = attacker.asPlayer().getPlayerClass().getId();
			balanceMod = target.isPlayable() ? ClassBalanceConfig.PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS[playerId] : ClassBalanceConfig.PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS[playerId];
		}
		
		if (target.isPlayable())
		{
			final int playerId = target.asPlayer().getPlayerClass().getId();
			defence *= attacker.isPlayable() ? ClassBalanceConfig.PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS[playerId] : ClassBalanceConfig.PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS[playerId];
		}
		
		final double skillPower = power + attackerStat.getValue(Stat.SKILL_POWER_ADD, 0);
		
		// Calculate the attack value by multiplying the sum of skill power and patk by 0.666, then applying various bonuses and penalties before dividing by pdef.
		// ````````````````````````^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^```^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^```^^^^^^^^^^^^
		final int patk = attacker.getPAtk();
		final double baseMod = (77 * (((skillPower + patk) * 0.666) + (isPosition * (skillPower + patk) * randomMod) + (6 * cdPatk))) / defence;
		final double damage = baseMod * ssmod * cdMult * weaponTraitMod * generalTraitMod * weaknessMod * attributeMod * randomMod * pvpPveMod * balanceMod;
		
		return damage;
	}
	
	/**
	 * Calculates the magic damage dealt by an attacker to a target.<br>
	 * This method applies various modifiers including skill power, defense, and critical hits.<br>
	 * It also handles special conditions like spirit shots and magic failure mechanics.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} being used for the attack.
	 * @param mAtk The magic attack value of the attacker.
	 * @param power The base power of the skill.
	 * @param mDef The magic defense value of the target.
	 * @param sps Whether the attack is a spirit shot.
	 * @param bss Whether the attack is a bonus spirit shot.
	 * @param mcrit Whether the attack is a critical hit.
	 * @return The final calculated damage as a {@code double}.
	 */
	public static double calcMagicDam(Creature attacker, Creature target, Skill skill, double mAtk, double power, double mDef, boolean sps, boolean bss, boolean mcrit)
	{
		final CreatureStat attackerStat = attacker.getStat();
		
		// Bonus Spirit shot.
		final double shotsBonus;
		if (bss)
		{
			shotsBonus = (4 * attackerStat.getValue(Stat.SHOTS_BONUS)) - (target.getStat().getValue(Stat.SPIRITSHOT_RESISTANCE, 1) / 100);
		}
		else if (sps)
		{
			shotsBonus = (2 * attackerStat.getValue(Stat.SHOTS_BONUS)) - (target.getStat().getValue(Stat.SPIRITSHOT_RESISTANCE, 1) / 100);
		}
		else
		{
			shotsBonus = 1;
		}
		
		final double critMod = mcrit ? calcCritDamage(attacker, target, skill) : 1;
		final double critMagicAdd = mcrit ? calcCritDamageAdd(attacker, target, skill) : 0;
		
		// Trait, elements
		final double generalTraitMod = calcGeneralTraitBonus(attacker, target, skill.getTraitType(), true);
		final double weaknessMod = calcWeaknessBonus(attacker, target, skill.getTraitType());
		final double attributeMod = calcAttributeBonus(attacker, target, skill);
		final double randomMod = attacker.getRandomDamageMultiplier();
		final double pvpPveMod = calculatePvpPveBonus(attacker, target, skill, mcrit);
		
		// MDAM Formula.
		double damage = ((77 * (power + attackerStat.getValue(Stat.SKILL_POWER_ADD, 0)) * Math.sqrt(mAtk)) / mDef) * shotsBonus;
		
		// Failure calculation
		if (PlayerConfig.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			if (attacker.isPlayer())
			{
				if (calcMagicSuccess(attacker, target, skill))
				{
					if (skill.hasEffectType(EffectType.HP_DRAIN))
					{
						attacker.sendPacket(SystemMessageId.DRAIN_WAS_ONLY_50_SUCCESSFUL);
					}
					else
					{
						attacker.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
					}
					
					damage /= 2;
				}
				else
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
					sm.addString(target.getName());
					sm.addSkillName(skill);
					attacker.sendPacket(sm);
					damage = 1;
				}
			}
			
			if (target.isPlayer())
			{
				final SystemMessage sm = (skill.hasEffectType(EffectType.HP_DRAIN)) ? new SystemMessage(SystemMessageId.YOU_RESISTED_C1_S_DRAIN) : new SystemMessage(SystemMessageId.YOU_RESISTED_C1_S_MAGIC);
				sm.addString(attacker.getName());
				target.sendPacket(sm);
			}
		}
		
		// NasSeKa rev. 10196: generalTraitMod == 0 ? 1 : generalTraitMod (no invulnerable traits).
		damage = damage * critMod * (generalTraitMod == 0 ? 1 : generalTraitMod) * weaknessMod * attributeMod * randomMod * pvpPveMod;
		damage *= attackerStat.getValue(Stat.MAGICAL_SKILL_POWER, 1);
		damage += critMagicAdd;
		
		// AoE modifiers.
		if (skill.hasNegativeEffect() && (skill.getAffectLimit() > 0))
		{
			final CreatureStat targetStat = target.getStat();
			damage *= Math.max((attackerStat.getMul(Stat.AREA_OF_EFFECT_DAMAGE_MODIFY, 1) - targetStat.getValue(Stat.AREA_OF_EFFECT_DAMAGE_DEFENCE, 0)), 0.01);
			damage -= targetStat.getValue(Stat.AREA_OF_EFFECT_DAMAGE_DEFENCE_ADD, 0);
		}
		
		return Math.max(1, damage);
	}
	
	/**
	 * Determines if an attack results in a critical hit.<br>
	 * This method calculates the probability based on whether the action is a magical skill, physical skill, or auto-attack.<br>
	 * It accounts for creature stats, target defenses, level differences, and class balance multipliers.
	 * @param rateValue The base critical rate value used for calculations.
	 * @param creature The entity performing the attack.
	 * @param target The entity receiving the attack.
	 * @param skill The {@code Skill} being executed; if {@code null}, it is treated as an auto-attack.
	 * @return {@code true} if the attack is a critical hit, {@code false} otherwise.
	 */
	public static boolean calcCrit(double rateValue, Creature creature, Creature target, Skill skill)
	{
		// Base crit rate for autoattack \ physical skill hit.
		double baseRate = rateValue;
		
		if (skill != null)
		{
			// Magical skill critical rate.
			if (skill.isMagic())
			{
				final double magicRate = creature.getStat().getValue(Stat.MAGIC_CRITICAL_RATE);
				final double magicSkillBaseRateMod = (baseRate / 100) + 1;
				if ((target == null) || !skill.hasNegativeEffect())
				{
					return Math.min(magicRate * magicSkillBaseRateMod, 320) > Rnd.get(1000);
				}
				
				final CreatureStat targetStat = target.getStat();
				final double defenceMagicCriticalRate = targetStat.getValue(Stat.DEFENCE_MAGIC_CRITICAL_RATE, 1) - 1;
				double magicRateFinal = (magicRate - targetStat.getValue(Stat.DEFENCE_MAGIC_CRITICAL_RATE_ADD, 0)) * (1 - Math.min(0.8, defenceMagicCriticalRate));
				if ((creature.getLevel() >= 78) || (target.getLevel() >= 78))
				{
					final int creatureLevel = creature.getLevel();
					magicRateFinal += Math.sqrt(creatureLevel) + ((creatureLevel - target.getLevel()) / 25);
				}
				
				double balanceMod = 1;
				if (creature.isPlayable())
				{
					final int playerClassId = creature.asPlayer().getPlayerClass().getId();
					balanceMod = target.isPlayable() ? ClassBalanceConfig.PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[playerClassId] : ClassBalanceConfig.PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[playerClassId];
				}
				
				return Math.clamp((magicSkillBaseRateMod * magicRateFinal) * balanceMod, 30, PlayerConfig.MAX_MCRIT_RATE) > Rnd.get(1000);
			}
			
			// Physical skill critical rate.
			final double statBonus;
			
			// There is a chance that activeChar has altered base stat for skill critical.
			final byte skillCritRateStat = (byte) creature.getStat().getValue(Stat.STAT_BONUS_SKILL_CRITICAL);
			if ((skillCritRateStat >= 0) && (skillCritRateStat < BaseStat.values().length))
			{
				// Best tested.
				statBonus = BaseStat.values()[skillCritRateStat].calcBonus(creature);
			}
			else
			{
				// Default base stat used for skill critical formula is STR.
				statBonus = BaseStat.STR.calcBonus(creature);
			}
			
			final double physicalSkillRate = creature.getStat().getValue(Stat.CRITICAL_RATE_SKILL);
			final CreatureStat targetStat = target.getStat();
			final double defencePhysicalSkillCriticalRate = targetStat.getValue(Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_RATE, 1) - 1;
			final double physicalSkillRateFinal = ((physicalSkillRate - targetStat.getValue(Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_RATE_ADD, 0)) * statBonus) * (1 - Math.min(0.8, defencePhysicalSkillCriticalRate));
			
			double balanceMod = 1;
			if (creature.isPlayable())
			{
				final int playerClassId = creature.asPlayer().getPlayerClass().getId();
				balanceMod = target.isPlayable() ? ClassBalanceConfig.PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[playerClassId] : ClassBalanceConfig.PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[playerClassId];
			}
			
			return Math.clamp((((baseRate / 100) + 1) * physicalSkillRateFinal) * balanceMod, 30, PlayerConfig.MAX_PSKILLCRIT_RATE) > Rnd.get(1000);
		}
		
		// Autoattack critical rate.
		final CreatureStat targetStat = target.getStat();
		final double defenceCriticalRate = targetStat.getValue(Stat.DEFENCE_CRITICAL_RATE, 1) - 1;
		final double attackRateMod = (baseRate - targetStat.getValue(Stat.DEFENCE_CRITICAL_RATE_ADD, 0)) * (1 - Math.min(0.8, defenceCriticalRate));
		final double criticalPositionBonus = calcCriticalPositionBonus(creature, target);
		final double criticalHeightBonus = calcCriticalHeightBonus(creature, target);
		double attackRate = attackRateMod * criticalPositionBonus * criticalHeightBonus;
		
		// Autoattack critical depends on level difference at high levels as well.
		if ((creature.getLevel() >= 78) || (target.getLevel() >= 78))
		{
			final int creatureLevel = creature.getLevel();
			attackRate += (Math.sqrt(creatureLevel) * (creatureLevel - target.getLevel()) * 0.125);
		}
		
		double balanceMod = 1;
		if (creature.isPlayable())
		{
			final int playerClassId = creature.asPlayer().getPlayerClass().getId();
			balanceMod = target.isPlayable() ? ClassBalanceConfig.PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[playerClassId] : ClassBalanceConfig.PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[playerClassId];
		}
		
		return Math.clamp(attackRate * balanceMod, 30, PlayerConfig.MAX_PCRIT_RATE) > Rnd.get(1000);
	}
	
	/**
	 * Calculates the critical rate bonus based on the relative position of the attacker to the target.<br>
	 * It applies a multiplier depending on whether the attack comes from the side or the back.<br>
	 * Attacks from the front use the base critical rate value.
	 * @param creature The {@code Creature} performing the attack.
	 * @param target The {@code Creature} being attacked.
	 * @return The calculated critical rate as a {@code double}.
	 */
	public static double calcCriticalPositionBonus(Creature creature, Creature target)
	{
		// final Position position = activeChar.getStat().has(Stats.ATTACK_BEHIND) ? Position.BACK : Position.getPosition(activeChar, target);
		switch (Position.getPosition(creature, target))
		{
			case SIDE: // 10% Critical Chance bonus when attacking from side.
			{
				return 1.1 * creature.getStat().getPositionTypeValue(Stat.CRITICAL_RATE, Position.SIDE);
			}
			case BACK: // 30% Critical Chance bonus when attacking from back.
			{
				return 1.3 * creature.getStat().getPositionTypeValue(Stat.CRITICAL_RATE, Position.BACK);
			}
			default: // No Critical Chance bonus when attacking from front.
			{
				return creature.getStat().getPositionTypeValue(Stat.CRITICAL_RATE, Position.FRONT);
			}
		}
	}
	
	/**
	 * Calculates the critical height bonus based on the vertical distance between two locations.<br>
	 * This method compares the Z coordinates of the source and target positions.<br>
	 * It applies a multiplier to the difference and adds a base value.
	 * @param from The starting location of the action.
	 * @param target The destination location of the action.
	 * @return The calculated critical height bonus as a double.
	 */
	public static double calcCriticalHeightBonus(ILocational from, ILocational target)
	{
		return ((((Math.clamp(from.getZ() - target.getZ(), -25, 25) * 4) / 5) + 10) / 100) + 1;
	}
	
	/**
	 * Calculates the final critical damage multiplier for an attack.<br>
	 * This method determines if the skill is magic or physical to select the correct stats.<br>
	 * It also applies class balance multipliers and target defense reductions.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} being used, or {@code null} for auto-attacks.
	 * @return The calculated critical damage multiplier as a {@code double}.
	 */
	public static double calcCritDamage(Creature attacker, Creature target, Skill skill)
	{
		final double criticalDamage;
		final double defenceCriticalDamage;
		double balanceMod = 1;
		
		if (skill != null)
		{
			if (skill.isMagic())
			{
				// Magic critical damage.
				criticalDamage = attacker.getStat().getValue(Stat.MAGIC_CRITICAL_DAMAGE, 1);
				defenceCriticalDamage = target.getStat().getValue(Stat.DEFENCE_MAGIC_CRITICAL_DAMAGE, 1);
				if (attacker.isPlayable())
				{
					final int playerId = attacker.asPlayer().getPlayerClass().getId();
					balanceMod = target.isPlayable() ? ClassBalanceConfig.PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[playerId] : ClassBalanceConfig.PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[playerId];
				}
			}
			else
			{
				criticalDamage = attacker.getStat().getValue(Stat.PHYSICAL_SKILL_CRITICAL_DAMAGE, 1);
				defenceCriticalDamage = target.getStat().getValue(Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_DAMAGE, 1);
				if (attacker.isPlayable())
				{
					final int playerId = attacker.asPlayer().getPlayerClass().getId();
					balanceMod = target.isPlayable() ? ClassBalanceConfig.PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[playerId] : ClassBalanceConfig.PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[playerId];
				}
			}
		}
		else
		{
			// Autoattack critical damage.
			final CreatureStat attackerStat = attacker.getStat();
			criticalDamage = attackerStat.getValue(Stat.CRITICAL_DAMAGE, 1) * attackerStat.getPositionTypeValue(Stat.CRITICAL_DAMAGE, Position.getPosition(attacker, target));
			defenceCriticalDamage = target.getStat().getValue(Stat.DEFENCE_CRITICAL_DAMAGE, 1);
			if (attacker.isPlayable())
			{
				final int playerId = attacker.asPlayer().getPlayerClass().getId();
				balanceMod = target.isPlayable() ? ClassBalanceConfig.PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[playerId] : ClassBalanceConfig.PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[playerId];
			}
		}
		
		// The limit for reducing critical damage is 80% (0.2).
		return 1 + ((1 * criticalDamage * balanceMod) * Math.max(0.2, (2 - defenceCriticalDamage)));
	}
	
	/**
	 * Calculates the additional damage from a critical hit.<br>
	 * This method determines the difference between the attacker's bonus and the target's defense.<br>
	 * It distinguishes between magic, physical skills, and autoattacks based on the provided {@code Skill}.<br>
	 * The result is clamped to a minimum value of 0.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} being used, or {@code null} for autoattacks.
	 * @return The final calculated critical damage bonus as a {@code double}.
	 */
	public static double calcCritDamageAdd(Creature attacker, Creature target, Skill skill)
	{
		final double criticalDamageAdd;
		final double defenceCriticalDamageAdd;
		
		if (skill != null)
		{
			if (skill.isMagic())
			{
				// Magic critical damage.
				criticalDamageAdd = attacker.getStat().getValue(Stat.MAGIC_CRITICAL_DAMAGE_ADD, 0);
				defenceCriticalDamageAdd = target.getStat().getValue(Stat.DEFENCE_MAGIC_CRITICAL_DAMAGE_ADD, 0);
			}
			else
			{
				criticalDamageAdd = attacker.getStat().getValue(Stat.PHYSICAL_SKILL_CRITICAL_DAMAGE_ADD, 0);
				defenceCriticalDamageAdd = target.getStat().getValue(Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_DAMAGE_ADD, 0);
			}
		}
		else
		{
			// Autoattack critical damage.
			criticalDamageAdd = attacker.getStat().getValue(Stat.CRITICAL_DAMAGE_ADD, 0);
			defenceCriticalDamageAdd = target.getStat().getValue(Stat.DEFENCE_CRITICAL_DAMAGE_ADD, 0);
		}
		
		return Math.max(0, criticalDamageAdd + defenceCriticalDamageAdd);
	}
	
	/**
	 * Determines if an attack successfully breaks the target's current action.<br>
	 * This method checks conditions like channeling and specific status effects.<br>
	 * It calculates a success rate based on damage and the target's stats.
	 * @param target The {@code Creature} being attacked.
	 * @param dmg The amount of damage dealt by the attack.
	 * @return {@code true} if the attack break succeeds, {@code false} otherwise.
	 */
	public static boolean calcAtkBreak(Creature target, double dmg)
	{
		if (target.isChanneling())
		{
			return false;
		}
		
		// Cannot interrupt targets affected by Burst or Superior Burst Casting.
		if (target.hasAbnormalType(AbnormalType.DC_MOD))
		{
			return false;
		}
		
		double init = 0;
		
		if (PlayerConfig.ALT_GAME_CANCEL_CAST && target.isCastingNow(SkillCaster::canAbortCast))
		{
			init = 15;
		}
		
		if (PlayerConfig.ALT_GAME_CANCEL_BOW && target.isAttackingNow())
		{
			final Weapon wpn = target.getActiveWeaponItem();
			if ((wpn != null) && (wpn.getItemType() == WeaponType.BOW))
			{
				init = 15;
			}
		}
		
		if (target.isRaid() || target.isHpBlocked() || (init <= 0))
		{
			return false; // No attack break
		}
		
		// Chance of break is higher with higher dmg.
		init += Math.sqrt(13 * dmg);
		
		// Chance is affected by target MEN.
		init -= ((BaseStat.MEN.calcBonus(target) * 100) - 100);
		
		// Calculate all modifiers for ATTACK_CANCEL.
		double rate = target.getStat().getValue(Stat.ATTACK_CANCEL, init);
		
		// Adjust the rate to be between 1 and 99.
		rate = Math.clamp(rate, 1, 99);
		
		return Rnd.get(100) < rate;
	}
	
	/**
	 * Calculates the attack speed for a specific skill.<br>
	 * This method determines the speed based on whether the skill is magic or physical.<br>
	 * It uses the attacker's stats and the provided skill time to return an integer value.
	 * @param attacker The {@code Creature} performing the action.
	 * @param skill The {@code Skill} being used.
	 * @param skillTime The base time of the {@code Skill}.
	 * @return The calculated attack speed as an {@code int}.
	 */
	public static int calcAtkSpd(Creature attacker, Skill skill, double skillTime)
	{
		if (skill.isMagic())
		{
			return (int) ((skillTime / attacker.getMAtkSpd()) * 333);
		}
		
		return (int) ((skillTime / attacker.getPAtkSpd()) * 300);
	}
	
	/**
	 * Calculates the attack speed multiplier for a specific {@code Creature}.<br>
	 * This calculation considers dexterity bonuses and weapon base values.<br>
	 * It also applies additional multipliers from creature stats.
	 * @param creature The {@code Creature} to calculate the multiplier for.
	 * @return The final calculated attack speed multiplier as a {@code double}.
	 */
	public static double calcAtkSpdMultiplier(Creature creature)
	{
		final double armorBonus = 1; // EquipedArmorSpeedByCrystal TODO: Implement me!
		final double dexBonus = BaseStat.DEX.calcBonus(creature);
		final double weaponAttackSpeed = Stat.weaponBaseValue(creature, Stat.PHYSICAL_ATTACK_SPEED) / armorBonus; // unk868
		final CreatureStat creatureStat = creature.getStat();
		final double attackSpeedPerBonus = creatureStat.getMul(Stat.PHYSICAL_ATTACK_SPEED);
		final double attackSpeedDiffBonus = creatureStat.getAdd(Stat.PHYSICAL_ATTACK_SPEED);
		return (dexBonus * (weaponAttackSpeed / 333) * attackSpeedPerBonus) + (attackSpeedDiffBonus / 333);
	}
	
	/**
	 * Calculates the magic attack speed multiplier for a given {@code Creature}.<br>
	 * This method combines bonuses from wisdom and specific stat multipliers.<br>
	 * It also accounts for flat casting speed differences.
	 * @param creature The {@code Creature} to calculate the multiplier for.
	 * @return The calculated magic attack speed multiplier as a {@code double}.
	 */
	public static double calcMAtkSpdMultiplier(Creature creature)
	{
		final double armorBonus = 1; // TODO: Implement me!
		final double witBonus = BaseStat.WIT.calcBonus(creature);
		final CreatureStat creatureStat = creature.getStat();
		final double castingSpeedPerBonus = creatureStat.getMul(Stat.MAGIC_ATTACK_SPEED);
		final double castingSpeedDiffBonus = creatureStat.getAdd(Stat.MAGIC_ATTACK_SPEED);
		return ((1 / armorBonus) * witBonus * castingSpeedPerBonus) + (castingSpeedDiffBonus / 333);
	}
	
	/**
	 * Calculates the time factor for a specific skill based on the creature's stats.<br>
	 * This method determines how fast a skill can be executed.<br>
	 * It accounts for attack speed multipliers and special magic types.<br>
	 * The result is adjusted by NPC hit time factors if applicable.
	 * @param creature The {@code Creature} performing the action.
	 * @param skill The {@code Skill} being used to calculate the factor.
	 * @return The calculated time factor as a {@code double}.
	 */
	public static double calcSkillTimeFactor(Creature creature, Skill skill)
	{
		final int magicType = skill.getMagicType();
		if (skill.getOperateType().isChanneling() || (magicType == 2) || (magicType == 4) || (magicType == 21))
		{
			return 1.0d;
		}
		
		double factor = 0.0;
		if (magicType == 1)
		{
			final double spiritshotHitTime = (creature.isChargedShot(ShotType.SPIRITSHOTS) || creature.isChargedShot(ShotType.BLESSED_SPIRITSHOTS)) ? 0.4 : 0; // TODO: Implement propper values
			final double matkspdmul = creature.getStat().getMAttackSpeedMultiplier();
			factor = matkspdmul + (matkspdmul * spiritshotHitTime); // matkspdmul + (matkspdmul * spiritshot_hit_time)
		}
		else
		{
			factor = creature.getAttackSpeedMultiplier();
		}
		
		if (creature.isNpc())
		{
			final double npcFactor = creature.asNpc().getTemplate().getHitTimeFactorSkill();
			if (npcFactor > 0)
			{
				factor /= npcFactor;
			}
		}
		
		return Math.max(0.01, factor);
	}
	
	/**
	 * Calculates the time required to cancel a skill for a specific creature.<br>
	 * This method takes into account the skill's base hit cancel time and the creature's unique factors.<br>
	 * It ensures that the result is never lower than the {@code SKILL_LAUNCH_TIME}.
	 * @param creature The {@code Creature} performing the action.
	 * @param skill The {@code Skill} being used.
	 * @return The calculated cancel time as a double value.
	 */
	public static double calcSkillCancelTime(Creature creature, Skill skill)
	{
		return Math.max((skill.getHitCancelTime() * 1000) / calcSkillTimeFactor(creature, skill), SKILL_LAUNCH_TIME);
	}
	
	/**
	 * Determines if an attack from one creature successfully hits another.<br>
	 * This method calculates the hit probability based on accuracy and evasion rates.<br>
	 * It also applies bonuses from specific combat conditions.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} being attacked.
	 * @return {@code true} if the attack hits, and {@code false} if it misses.
	 */
	public static boolean calcHitMiss(Creature attacker, Creature target)
	{
		int chance = (80 + (2 * (attacker.getAccuracy() - target.getEvasionRate()))) * 10;
		
		// Get additional bonus from the conditions when you are attacking.
		chance *= HitConditionBonusData.getInstance().getConditionBonus(attacker, target);
		
		chance = Math.max(chance, 200);
		chance = Math.min(chance, 980);
		
		return chance < Rnd.get(1000);
	}
	
	/**
	 * Calculates the shield defense result for a target creature.<br>
	 * It checks if the target is wearing a valid shield and determines the success of the block.<br>
	 * The calculation considers the attacker's range, the target's stats, and their relative positions.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} being attacked.
	 * @param sendSysMsg Whether to send a system message to the player if they succeed in blocking.
	 * @param crit Whether the attack is a critical hit, which triples the block rate.
	 * @return A {@code byte} representing the shield defense result.
	 */
	public static byte calcShldUse(Creature attacker, Creature target, boolean sendSysMsg, boolean crit)
	{
		final ItemTemplate item = target.getSecondaryWeaponItem();
		if (!(item instanceof Armor) || (((Armor) item).getItemType() == ArmorType.SIGIL))
		{
			return 0;
		}
		
		double shldRate = target.getStat().getValue(Stat.SHIELD_DEFENCE_RATE) * BaseStat.CON.calcBonus(target);
		
		// If attacker use bow and target wear shield, shield block rate is multiplied by 1.3 (30%).
		if (attacker.getAttackType().isRanged())
		{
			shldRate *= 1.3;
		}
		
		// Critical hits are blocked three times as often.
		if (crit)
		{
			shldRate *= 3;
		}
		
		final int degreeside = target.isAffected(EffectFlag.PHYSICAL_SHIELD_ANGLE_ALL) ? 360 : 120;
		if ((degreeside < 360) && (Math.abs(target.calculateDirectionTo(attacker) - LocationUtil.convertHeadingToDegree(target.getHeading())) > (degreeside / 2)))
		{
			return 0;
		}
		
		byte shldSuccess = SHIELD_DEFENSE_FAILED;
		
		// Check shield success.
		if (shldRate > Rnd.get(100))
		{
			// If shield succeed, check perfect block.
			if (((100 - (2 * BaseStat.CON.calcBonus(target))) < Rnd.get(100)))
			{
				shldSuccess = SHIELD_DEFENSE_PERFECT_BLOCK;
			}
			else
			{
				shldSuccess = SHIELD_DEFENSE_SUCCEED;
			}
		}
		
		if (sendSysMsg && target.isPlayer())
		{
			final Player enemy = target.asPlayer();
			
			switch (shldSuccess)
			{
				case SHIELD_DEFENSE_SUCCEED:
				{
					enemy.sendPacket(SystemMessageId.YOU_VE_BLOCKED_THE_ATTACK);
					break;
				}
				case SHIELD_DEFENSE_PERFECT_BLOCK:
				{
					enemy.sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
					break;
				}
			}
		}
		
		return shldSuccess;
	}
	
	/**
	 * Calculates the result of a shield block attempt against a non-critical attack.
	 * @param attacker The creature performing the attack.
	 * @param target The creature attempting to block with a shield.
	 * @param sendSysMsg Whether to send a system message to the player if they succeed.
	 * @return A byte representing the result of the shield defense.
	 */
	public static byte calcShldUse(Creature attacker, Creature target, boolean sendSysMsg)
	{
		return calcShldUse(attacker, target, sendSysMsg, false);
	}
	
	/**
	 * Calculates the shield usage value for a specific interaction.<br>
	 * This method determines how much of the {@code Creature} shield is consumed.
	 * @param attacker The {@code Creature} performing the action.
	 * @param target The {@code Creature} receiving the action.
	 * @return The calculated shield usage as a {@code byte}.
	 */
	public static byte calcShldUse(Creature attacker, Creature target)
	{
		return calcShldUse(attacker, target, true);
	}
	
	/**
	 * Determines if a magic skill successfully hits the target.<br>
	 * This method checks for debuff blocks and calculates hit probability based on magic attack and defense.
	 * @param actor The creature performing the action.
	 * @param target The creature receiving the action.
	 * @param skill The skill being used.
	 * @return {@code true} if the hit is successful, {@code false} otherwise.
	 */
	public static boolean calcMagicAffected(Creature actor, Creature target, Skill skill)
	{
		double defence = 0;
		if (skill.isActive() && skill.hasNegativeEffect())
		{
			defence = target.getMDef();
		}
		
		if (skill.isDebuff())
		{
			if (target.getAbnormalShieldBlocks() > 0)
			{
				target.decrementAbnormalShieldBlocks();
				return false;
			}
			else if (target.isAffected(EffectFlag.DEBUFF_BLOCK))
			{
				return false;
			}
		}
		
		final double attack = 2 * actor.getMAtk() * calcGeneralTraitBonus(actor, target, skill.getTraitType(), false);
		double d = (attack - defence) / (attack + defence);
		d += 0.5 * Rnd.nextGaussian();
		return d > 0;
	}
	
	/**
	 * Calculates the damage modifier based on level differences.<br>
	 * This method considers both the {@code Skill} level bonus and the difference between the attacker and target levels.
	 * @param attacker The {@code Creature} performing the action.
	 * @param target The {@code Creature} receiving the action.
	 * @param skill The {@code Skill} being used to calculate the modifier.
	 * @return The final calculated level bonus multiplier as a {@code double}.
	 */
	public static double calcLvlBonusMod(Creature attacker, Creature target, Skill skill)
	{
		final int attackerLvl = skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel();
		final double skillLevelBonusRateMod = 1 + (skill.getLvlBonusRate() / 100.);
		final double lvlMod = 1 + ((attackerLvl - target.getLevel()) / 100.);
		return skillLevelBonusRateMod * lvlMod;
	}
	
	/**
	 * Determines if a skill effect successfully applies to a target.<br>
	 * This method checks for object types, debuff resistances, and activation rates.<br>
	 * It calculates the final success chance based on various modifiers.
	 * @param attacker The {@code Creature} performing the action.
	 * @param target The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the effect is successfully applied, {@code false} otherwise.
	 */
	public static boolean calcEffectSuccess(Creature attacker, Creature target, Skill skill)
	{
		// Dead targets, such as the corpse of Corpse Plague, and static objects can not receive continuous effects.
		if (target.isDead() || target.isDoor() || (target instanceof SiegeFlag) || (target instanceof StaticObject))
		{
			return false;
		}
		
		if (skill.isDebuff())
		{
			boolean resisted = target.isCastingNow(s -> s.getSkill().getAbnormalResists().contains(skill.getAbnormalType()));
			if (!resisted)
			{
				if (target.getAbnormalShieldBlocks() > 0)
				{
					target.decrementAbnormalShieldBlocks();
					resisted = true;
				}
			}
			
			if (!resisted)
			{
				final double sphericBarrierRange = target.getStat().getValue(Stat.SPHERIC_BARRIER_RANGE, 0);
				if (sphericBarrierRange > 0)
				{
					resisted = attacker.calculateDistance3D(target) > sphericBarrierRange;
				}
			}
			
			if (resisted)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
				sm.addString(target.getName());
				sm.addSkillName(skill);
				attacker.sendPacket(sm);
				attacker.sendPacket(new ExMagicAttackInfo(attacker.getObjectId(), target.getObjectId(), ExMagicAttackInfo.RESISTED));
				return false;
			}
		}
		
		final int activateRate = skill.getActivateRate();
		final double traitMod = calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
		
		// A target invulnerable to the trait falls through to the resist below, even for effects that always land.
		if ((traitMod > 0) && ((activateRate == -1) || (activateRate > 99)))
		{
			// Debuff Resistance still applies to debuffs that always land (Fluffy Reinforcement notes), unless the skill ignores it.
			if (skill.isDebuff() && !skill.isIgnoreDebuffResist() && (target != attacker))
			{
				final double debuffResist = target.getStat().getValue(Stat.RESIST_ABNORMAL_DEBUFF, 1);
				if ((debuffResist < 1) && ((debuffResist * 100) <= Rnd.get(100)))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
					sm.addString(target.getName());
					sm.addSkillName(skill);
					attacker.sendPacket(sm);
					attacker.sendPacket(new ExMagicAttackInfo(attacker.getObjectId(), target.getObjectId(), ExMagicAttackInfo.RESISTED));
					return false;
				}
			}
			
			return true;
		}
		
		int magicLevel = skill.getMagicLevel();
		final int targetEffectLevel = target.getLevel();
		if (magicLevel <= -1)
		{
			magicLevel = targetEffectLevel + 3;
		}
		
		final double targetBasicProperty = getAbnormalResist(skill.getBasicProperty(), target);
		final double baseMod = ((((((magicLevel - targetEffectLevel) + 3) * skill.getLvlBonusRate()) + activateRate) + 30.0) - targetBasicProperty);
		final double elementMod = calcAttributeBonus(attacker, target, skill);
		final double basicPropertyResist = getBasicPropertyResistBonus(skill.getBasicProperty(), target);
		final double buffDebuffMod = skill.isDebuff() ? target.getStat().getValue(Stat.RESIST_ABNORMAL_DEBUFF, 1) : 1;
		final double rate = baseMod * elementMod * traitMod * buffDebuffMod;
		final double finalRate = traitMod > 0 ? Math.clamp(rate, skill.getMinChance(), skill.getMaxChance()) * basicPropertyResist : 0;
		
		if ((finalRate <= Rnd.get(100)) && (target != attacker))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
			sm.addString(target.getName());
			sm.addSkillName(skill);
			attacker.sendPacket(sm);
			attacker.sendPacket(new ExMagicAttackInfo(attacker.getObjectId(), target.getObjectId(), traitMod > 0 ? ExMagicAttackInfo.RESISTED : ExMagicAttackInfo.IMMUNE));
			return false;
		}
		
		return true;
	}
	
	/**
	 * Determines if a magic skill successfully hits the target.<br>
	 * This method calculates success based on level differences, accuracy, and resistance.<br>
	 * It returns {@code true} if the hit is successful and {@code false} otherwise.
	 * @param attacker The creature performing the attack.
	 * @param target The creature receiving the attack.
	 * @param skill The skill being used for the calculation.
	 * @return {@code true} if the magic hit succeeds, {@code false} otherwise.
	 */
	public static boolean calcMagicSuccess(Creature attacker, Creature target, Skill skill)
	{
		double lvlModifier = 1;
		float targetModifier = 1;
		int mAccModifier = 1;
		if (attacker.isAttackable() || target.isAttackable())
		{
			final int targetMagicSuccessLevel = target.getLevel();
			lvlModifier = MathUtil.pow(1.3, targetMagicSuccessLevel - (PlayerConfig.CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL && (skill.getMagicLevel() > 0) ? skill.getMagicLevel() : attacker.getLevel()));
			
			final Player attackerMagicPlayer = attacker.asPlayer();
			if ((attackerMagicPlayer != null) && !target.isRaid() && !target.isRaidMinion() && (targetMagicSuccessLevel >= NpcConfig.MIN_NPC_LEVEL_MAGIC_PENALTY) && ((targetMagicSuccessLevel - attackerMagicPlayer.getLevel()) >= 3))
			{
				final int levelDiff = targetMagicSuccessLevel - attackerMagicPlayer.getLevel() - 2;
				if (levelDiff >= NpcConfig.NPC_SKILL_CHANCE_PENALTY.length)
				{
					targetModifier = NpcConfig.NPC_SKILL_CHANCE_PENALTY[NpcConfig.NPC_SKILL_CHANCE_PENALTY.length - 1];
				}
				else
				{
					targetModifier = NpcConfig.NPC_SKILL_CHANCE_PENALTY[levelDiff];
				}
			}
		}
		else
		{
			final int mAccDiff = attacker.getMagicAccuracy() - target.getMagicEvasionRate();
			mAccModifier = 100;
			if (mAccDiff > -20)
			{
				mAccModifier = 2;
			}
			else if (mAccDiff > -25)
			{
				mAccModifier = 30;
			}
			else if (mAccDiff > -30)
			{
				mAccModifier = 60;
			}
			else if (mAccDiff > -35)
			{
				mAccModifier = 90;
			}
		}
		
		// General magic resist.
		final double resModifier = target.getStat().getMul(Stat.MAGIC_SUCCESS_RES, 1);
		final int rate = 100 - Math.round((float) (mAccModifier * lvlModifier * targetModifier * resModifier));
		
		return (Rnd.get(100) < rate);
	}
	
	/**
	 * Calculates the magic damage dealt by an attacker to a target.<br>
	 * This method accounts for stats, skills, and various combat modifiers.<br>
	 * It handles shield defense, spiritshot bonuses, and critical hits.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} being used for the attack.
	 * @param power The base power multiplier of the skill.
	 * @param shld The shield defense status of the target.
	 * @param sps Whether the attack is a spiritshot.
	 * @param bss Whether the attack is a backstab.
	 * @param mcrit Whether the attack is a magic critical hit.
	 * @param critLimit The maximum damage allowed for a magic critical hit.
	 * @return The final calculated magic damage as a {@code double}.
	 */
	public static double calcManaDam(Creature attacker, Creature target, Skill skill, double power, byte shld, boolean sps, boolean bss, boolean mcrit, double critLimit)
	{
		// Formula: (SQR(M.Atk)*Power*(Target Max MP/97))/M.Def
		double mAtk = attacker.getMAtk();
		double mDef = target.getMDef();
		final double mp = target.getMaxMp();
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				mDef += target.getShldDef();
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1;
			}
		}
		
		// Bonus Spiritshot
		final double shotsBonus = attacker.getStat().getValue(Stat.SHOTS_BONUS);
		final double shotsResist = target.getStat().getValue(Stat.SPIRITSHOT_RESISTANCE, 0) / 100;
		mAtk *= bss ? (4 * shotsBonus) - shotsResist : sps ? (2 * shotsBonus) - shotsResist : 1;
		
		double damage = (Math.sqrt(mAtk) * power * (mp / 97)) / mDef;
		damage *= calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
		damage *= calculatePvpPveBonus(attacker, target, skill, mcrit);
		
		// Failure calculation
		if (PlayerConfig.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			if (attacker.isPlayer())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_C2_S_MAGIC_DAMAGE_IS_DECREASED);
				sm.addString(target.getName());
				sm.addString(attacker.getName());
				attacker.sendPacket(sm);
				damage /= 2;
			}
			
			if (target.isPlayer())
			{
				final SystemMessage sm2 = new SystemMessage(SystemMessageId.C1_WEAKLY_RESISTED_C2_S_MAGIC);
				sm2.addString(target.getName());
				sm2.addString(attacker.getName());
				target.sendPacket(sm2);
			}
		}
		
		if (mcrit)
		{
			damage *= 3;
			damage = Math.min(damage, critLimit);
			attacker.sendPacket(SystemMessageId.M_CRITICAL);
		}
		
		return damage;
	}
	
	/**
	 * Calculates the final percentage of health restored by a skill.<br>
	 * This method applies bonuses based on the {@code caster}'s Wisdom stat.<br>
	 * It ensures the result stays within specific bounds and limits extra bonuses.
	 * @param baseRestorePercent The initial restoration percentage provided by the skill.
	 * @param caster The {@code Creature} performing the action.
	 * @return The final calculated restoration percentage as a {@code double}.
	 */
	public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, Creature caster)
	{
		if ((baseRestorePercent == 0) || (baseRestorePercent == 100))
		{
			return baseRestorePercent;
		}
		
		double restorePercent = baseRestorePercent * BaseStat.WIT.calcBonus(caster);
		if ((restorePercent - baseRestorePercent) > 20.0)
		{
			restorePercent += 20.0;
		}
		
		restorePercent = Math.max(restorePercent, baseRestorePercent);
		restorePercent = Math.min(restorePercent, 90.0);
		
		return restorePercent;
	}
	
	/**
	 * Determines if a {@code Creature} successfully evades an attack from another {@code Creature}.<br>
	 * This method checks the target's skill evasion value against a random roll.<br>
	 * It also sends notification messages to players if either party is a {@code Player}.
	 * @param creature The entity performing the attack.
	 * @param target The entity attempting to evade the attack.
	 * @param skill The {@code Skill} being used for the attack.
	 * @return {@code true} if the attack was evaded, {@code false} otherwise.
	 */
	public static boolean calcSkillEvasion(Creature creature, Creature target, Skill skill)
	{
		if (Rnd.get(100) < target.getStat().getSkillEvasionTypeValue(skill.getMagicType()))
		{
			if (creature.isPlayer())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_DODGED_THE_ATTACK);
				sm.addString(target.getName());
				creature.asPlayer().sendPacket(sm);
			}
			
			if (target.isPlayer())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_DODGED_C1_S_ATTACK);
				sm.addString(creature.getName());
				target.asPlayer().sendPacket(sm);
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if the {@code actor} successfully triggers a Skill Mastery effect.<br>
	 * This calculation only applies to {@code Player} entities.<br>
	 * It determines the success based on the player's stats and class multipliers.
	 * @param actor The {@code Creature} performing the action.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the mastery effect is triggered, {@code false} otherwise.
	 */
	public static boolean calcSkillMastery(Creature actor, Skill skill)
	{
		// Non players are not affected by Skill Mastery.
		if (!actor.isPlayer())
		{
			return false;
		}
		
		final int val = (int) actor.getStat().getAdd(Stat.SKILL_MASTERY, -1);
		if (val == -1)
		{
			return false;
		}
		
		final double chance = BaseStat.values()[val].calcBonus(actor) * actor.getStat().getMul(Stat.SKILL_MASTERY_RATE, 1);
		
		return ((Rnd.nextDouble() * 100.) < (chance * ClassBalanceConfig.SKILL_MASTERY_CHANCE_MULTIPLIERS[actor.asPlayer().getPlayerClass().getId()]));
	}
	
	/**
	 * Calculates the damage multiplier based on elemental attributes.<br>
	 * It compares the attacker's element value against the target's defense.<br>
	 * The result is a multiplier between {@code 0.75} and {@code 1.25}.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} receiving the attack.
	 * @param skill The {@code Skill} being used to perform the attack.
	 * @return The calculated damage multiplier as a {@code double}.
	 */
	public static double calcAttributeBonus(Creature attacker, Creature target, Skill skill)
	{
		int attackAttribute;
		int defenceAttribute;
		
		if ((skill != null) && (skill.getAttributeType() != AttributeType.NONE))
		{
			final AttributeType attrType = skill.getAttributeType();
			attackAttribute = attacker.getAttackElementValue(attrType) + skill.getAttributeValue();
			defenceAttribute = target.getDefenseElementValue(attrType);
		}
		else
		{
			final AttributeType attackElem = attacker.getAttackElement();
			attackAttribute = attacker.getAttackElementValue(attackElem);
			defenceAttribute = target.getDefenseElementValue(attackElem);
		}
		
		final int diff = attackAttribute - defenceAttribute;
		if (diff > 0)
		{
			return Math.min(1.025 + (Math.sqrt(MathUtil.pow(diff, 3) / 2) * 0.0001), 1.25);
		}
		else if (diff < 0)
		{
			return Math.max(0.975 - (Math.sqrt(MathUtil.pow(-diff, 3) / 2) * 0.0001), 0.75);
		}
		
		return 1;
	}
	
	/**
	 * Calculates and applies a counter-attack when a creature is hit.<br>
	 * This only occurs for melee skills within the correct range.<br>
	 * It checks the target's vengeance chance before applying damage.<br>
	 * If successful, it reduces the health of the attacker based on specific formulas.
	 * @param attacker The creature performing the initial action.
	 * @param target The creature receiving the hit and potentially counter-attacking.
	 * @param skill The {@code Skill} being used for the attack.
	 * @param crit Whether the initial attack was a critical hit.
	 */
	public static void calcCounterAttack(Creature attacker, Creature target, Skill skill, boolean crit)
	{
		// Only melee skills can be reflected.
		if (skill.isMagic() || (skill.getCastRange() > MELEE_ATTACK_RANGE))
		{
			return;
		}
		
		final double chance = target.getStat().getValue(Stat.VENGEANCE_SKILL_PHYSICAL_DAMAGE, 0);
		if (Rnd.get(100) < chance)
		{
			if (target.isPlayer())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_COUNTERED_C1_S_ATTACK);
				sm.addString(attacker.getName());
				target.sendPacket(sm);
			}
			
			if (attacker.isPlayer())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_PERFORMING_A_COUNTERATTACK);
				sm.addString(target.getName());
				attacker.sendPacket(sm);
			}
			
			double counterdmg = ((target.getPAtk() * 873) / attacker.getPDef()); // Old: (((target.getPAtk(attacker) * 10.0) * 70.0) / attacker.getPDef(target));
			counterdmg *= calcWeaponTraitBonus(attacker, target);
			counterdmg *= calcGeneralTraitBonus(attacker, target, skill.getTraitType(), true);
			counterdmg *= calcAttributeBonus(attacker, target, skill);
			
			attacker.reduceCurrentHp(counterdmg, target, skill);
		}
	}
	
	/**
	 * Determines if a debuff reflection occurs on a target.<br>
	 * This check only applies to skills that are debuffs and have an activation rate.<br>
	 * It compares the target's reflection stat against a random value between 0 and 100.
	 * @param target The {@code Creature} being affected by the skill.
	 * @param skill The {@code Skill} being used to apply the effect.
	 * @return {@code true} if the debuff is reflected, {@code false} otherwise.
	 */
	public static boolean calcBuffDebuffReflection(Creature target, Skill skill)
	{
		if (!skill.isDebuff() || (skill.getActivateRate() == -1))
		{
			return false;
		}
		
		return target.getStat().getValue(skill.isMagic() ? Stat.REFLECT_SKILL_MAGIC : Stat.REFLECT_SKILL_PHYSIC, 0) > Rnd.get(100);
	}
	
	/**
	 * Calculates the damage taken by a {@code Creature} from falling.<br>
	 * This method returns {@code 0} if falling damage is disabled or height is negative.<br>
	 * The result depends on the creature's fall stat and maximum health.
	 * @param creature The {@code Creature} receiving the damage.
	 * @param fallHeight The distance of the fall in units.
	 * @return The calculated amount of fall damage as a {@code double}.
	 */
	public static double calcFallDam(Creature creature, int fallHeight)
	{
		if (!GeneralConfig.ENABLE_FALLING_DAMAGE || (fallHeight < 0))
		{
			return 0;
		}
		
		return creature.getStat().getValue(Stat.FALL, (fallHeight * creature.getMaxHp()) / 1000.0);
	}
	
	/**
	 * Determines if a physical blow successfully hits the target.<br>
	 * This method calculates the hit chance based on weapon stats and positional bonuses.<br>
	 * It also considers the creature's blow rate and the target's defense.
	 * @param creature The {@code Creature} performing the attack.
	 * @param target The {@code Creature} receiving the attack.
	 * @param skill The {@code Skill} being used for the attack.
	 * @param chanceBoost A percentage value to increase the hit chance.
	 * @return {@code true} if the blow hits, {@code false} otherwise.
	 */
	public static boolean calcBlowSuccess(Creature creature, Creature target, Skill skill, double chanceBoost)
	{
		final Weapon weapon = creature.getActiveWeaponItem();
		final int baseCritRate = creature.getTemplate().getBaseCritRate();
		final double weaponCritical = weapon != null ? weapon.getStats(Stat.CRITICAL_RATE, baseCritRate) : baseCritRate;
		
		// double dexBonus = BaseStats.DEX.calcBonus(activeChar); Not used in GOD
		final double critHeightBonus = calcCriticalHeightBonus(creature, target);
		final double criticalPosition = calcCriticalPositionBonus(creature, target); // 30% chance from back, 10% chance from side. Include buffs that give positional crit rate.
		final double chanceBoostMod = (100 + chanceBoost) / 100;
		final double blowRateMod = creature.getStat().getValue(Stat.BLOW_RATE, 1);
		final double blowRateDefenseMod = target.getStat().getValue(Stat.BLOW_RATE_DEFENCE, 1);
		
		final double rate = criticalPosition * critHeightBonus * weaponCritical * chanceBoostMod * blowRateMod * blowRateDefenseMod;
		
		// Blow rate change is limited (%).
		return Rnd.get(100) < Math.min(rate, PlayerConfig.BLOW_RATE_CHANCE_LIMIT);
	}
	
	/**
	 * Calculates which buffs or debuffs are removed from a target based on a skill effect.<br>
	 * It checks the {@code slot} type to determine if it should process buffs or debuffs.<br>
	 * The method respects the {@code rate} and {@code max} limits for removal.
	 * @param creature The creature performing the action.
	 * @param target The creature receiving the effect.
	 * @param skill The skill being used to trigger the cancellation.
	 * @param slot The type of effect to target, such as {@code BUFF} or {@code DEBUFF}.
	 * @param rate The success chance for removing an effect.
	 * @param max The maximum number of effects that can be removed at once.
	 * @return A list of {@code BuffInfo} objects representing the canceled effects.
	 */
	public static List<BuffInfo> calcCancelStealEffects(Creature creature, Creature target, Skill skill, DispelSlotType slot, int rate, int max)
	{
		final List<BuffInfo> canceled = new ArrayList<>(max);
		switch (slot)
		{
			case BUFF:
			{
				// Resist Modifier.
				final int cancelMagicLvl = skill == null ? creature.getLevel() : skill.getMagicLevel();
				
				// Prevent initialization.
				final List<BuffInfo> dances = target.getEffectList().getDances();
				for (int i = dances.size() - 1; i >= 0; i--) // reverse order
				{
					final BuffInfo info = dances.get(i);
					if (!info.getSkill().canBeStolen() || ((rate < 100) && !calcCancelSuccess(info, cancelMagicLvl, rate, skill, target)))
					{
						continue;
					}
					
					canceled.add(info);
					if (canceled.size() >= max)
					{
						break;
					}
				}
				
				if (canceled.size() < max)
				{
					// Prevent initialization.
					final List<BuffInfo> buffs = target.getEffectList().getBuffs();
					for (int i = buffs.size() - 1; i >= 0; i--) // reverse order
					{
						final BuffInfo info = buffs.get(i);
						if (!info.getSkill().canBeStolen() || ((rate < 100) && !calcCancelSuccess(info, cancelMagicLvl, rate, skill, target)))
						{
							continue;
						}
						
						canceled.add(info);
						if (canceled.size() >= max)
						{
							break;
						}
					}
				}
				break;
			}
			case DEBUFF:
			{
				final List<BuffInfo> debuffs = target.getEffectList().getDebuffs();
				for (int i = debuffs.size() - 1; i >= 0; i--)
				{
					final BuffInfo info = debuffs.get(i);
					if (info.getSkill().canBeDispelled() && (Rnd.get(100) <= rate))
					{
						canceled.add(info);
						if (canceled.size() >= max)
						{
							break;
						}
					}
				}
				break;
			}
		}
		
		return canceled;
	}
	
	/**
	 * Determines if a buff cancellation succeeds based on various factors.<br>
	 * It calculates the success chance using the base rate, magic level difference, and dispel resistance.<br>
	 * The final result is clamped between {@code 25} and {@code 75}.
	 * @param info The {@code BuffInfo} containing details about the buff to be cancelled.
	 * @param cancelMagicLvl The magic level of the source performing the cancellation.
	 * @param rate The base success rate for the cancellation.
	 * @param skill The {@code Skill} being used to attempt the cancellation.
	 * @param target The {@code Creature} that currently has the buff.
	 * @return {@code true} if the cancellation is successful, {@code false} otherwise.
	 */
	public static boolean calcCancelSuccess(BuffInfo info, int cancelMagicLvl, int rate, Skill skill, Creature target)
	{
		final int chance = (int) (rate + ((cancelMagicLvl - info.getSkill().getMagicLevel()) * 2) + ((info.getAbnormalTime() / 120) * target.getStat().getValue(Stat.RESIST_DISPEL_BUFF, 1)));
		return Rnd.get(100) < Math.clamp(chance, 25, 75); // TODO: i_dispel_by_slot_probability min = 40, max = 95.
	}
	
	/**
	 * Determines if a dispel buff cancellation succeeds.<br>
	 * This calculation considers the base rate, level difference, and target resistance.
	 * @param rate The base success rate for the cancellation.
	 * @param caster The {@code Creature} performing the action.
	 * @param target The {@code Creature} being affected by the buff.
	 * @return {@code true} if the cancellation is successful, {@code false} otherwise.
	 */
	public static boolean calcCancelSuccess(int rate, Creature caster, Creature target)
	{
		final int chance = (int) (rate + ((caster.getLevel() - target.getLevel()) * 2) + (target.getStat().getValue(Stat.RESIST_DISPEL_BUFF, 1)));
		return Rnd.get(100) < chance;
	}
	
	/**
	 * Calculates the duration of an abnormal effect for a specific skill.<br>
	 * It returns -1 if the skill is null, passive, or a toggle.<br>
	 * The duration is adjusted based on target multipliers and additions.<br>
	 * If the caster has mastered the skill, the duration is doubled.
	 * @param caster The {@code Creature} casting the skill.
	 * @param target The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used to calculate the time.
	 * @return The calculated duration as an {@code int}.
	 */
	public static int calcEffectAbnormalTime(Creature caster, Creature target, Skill skill)
	{
		int time = (skill == null) || skill.isPassive() || skill.isToggle() ? -1 : skill.getAbnormalTime();
		
		if ((skill != null) && !skill.isStatic())
		{
			// AbnormalTimeChangeBySkillId effect.
			if (time > 0)
			{
				final int skillId = skill.getId();
				final double multipliedAbnormalTime = target.getMultipliedAbnormalTime(skillId);
				if (multipliedAbnormalTime != 0d)
				{
					time *= multipliedAbnormalTime;
				}
				
				final int addedAbnormalTime = target.getAddedAbnormalTime(skillId);
				if (addedAbnormalTime != 0)
				{
					time += addedAbnormalTime;
				}
			}
			
			// If the skill is a mastery skill, the effect will last twice the default time.
			if (calcSkillMastery(caster, skill))
			{
				time *= 2;
			}
		}
		
		return time;
	}
	
	/**
	 * Calculates the success probability of a skill execution.<br>
	 * This method determines if an effect occurs based on various stats and traits.<br>
	 * It handles cases where {@code baseChance} is not a valid number by checking trait bonuses.
	 * @param baseChance The base probability value for the skill.
	 * @param attacker The {@code Creature} performing the action.
	 * @param target The {@code Creature} receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the probability check succeeds, {@code false} otherwise.
	 */
	public static boolean calcProbability(double baseChance, Creature attacker, Creature target, Skill skill)
	{
		// Skills without set probability should only test against trait invulnerability.
		if (Double.isNaN(baseChance))
		{
			return calcGeneralTraitBonus(attacker, target, skill.getTraitType(), true) > 0;
		}
		
		// Outdated formula: return Rnd.get(100) < ((((((skill.getMagicLevel() + baseChance) - target.getLevel()) + 30) - target.getINT()) * calcAttributeBonus(attacker, target, skill)) * calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false));
		// TODO: Find more retail-like formula
		return Rnd.get(100) < (((((skill.getMagicLevel() + baseChance) - target.getLevel()) - getAbnormalResist(skill.getBasicProperty(), target)) * calcAttributeBonus(attacker, target, skill)) * calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false));
	}
	
	/**
	 * Calculates the amount of karma lost based on experience gained.<br>
	 * This method uses the player level to determine the modifier.<br>
	 * It also accounts for the global karma loss rate.
	 * @param player The {@code Player} object used to check the current level.
	 * @param finalExp The amount of experience received by the player.
	 * @return The total integer value of karma lost.
	 */
	public static int calculateKarmaLost(Player player, double finalExp)
	{
		final double karmaLossMod = KarmaLossData.getInstance().getModifier(player.getLevel());
		if (finalExp > 0) // Received experience.
		{
			return (int) ((Math.abs(finalExp / RatesConfig.RATE_KARMA_LOST) / karmaLossMod) / 30);
		}
		
		return (int) ((Math.abs(finalExp) / karmaLossMod) / 30);
	}
	
	/**
	 * Calculates the amount of karma gained by a player.<br>
	 * The calculation depends on the number of kills and whether the target was a summon.<br>
	 * It applies different multipliers based on specific kill thresholds.
	 * @param pkCount The total number of kills performed.
	 * @param isSummon A boolean indicating if the target was a summon.
	 * @return The final calculated karma gain as an {@code int}.
	 */
	public static int calculateKarmaGain(int pkCount, boolean isSummon)
	{
		int result = 43200;
		
		if (isSummon)
		{
			result = (int) ((((pkCount * 0.375) + 1) * 60) * 4) - 150;
			
			if (result > 10800)
			{
				return 10800;
			}
		}
		
		if (pkCount < 99)
		{
			result = (int) ((((pkCount * 0.5) + 1) * 60) * 12);
		}
		else if (pkCount < 180)
		{
			result = (int) ((((pkCount * 0.125) + 37.75) * 60) * 12);
		}
		
		return result;
	}
	
	/**
	 * Calculates the damage multiplier based on specific creature traits.<br>
	 * It checks for invulnerability and applies trait bonuses between the attacker and target.<br>
	 * The result is a double value representing the final modifier.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} receiving the attack.
	 * @param traitType The specific {@code TraitType} to evaluate.
	 * @param ignoreResistance A boolean indicating if resistance should be ignored.
	 * @return The calculated damage multiplier as a {@code double}.
	 */
	public static double calcGeneralTraitBonus(Creature attacker, Creature target, TraitType traitType, boolean ignoreResistance)
	{
		if (traitType == TraitType.NONE)
		{
			return 1.0;
		}
		
		if (target.getStat().isInvulnerableTrait(traitType))
		{
			return 0;
		}
		
		switch (traitType.getType())
		{
			case 2:
			{
				if (!attacker.getStat().hasAttackTrait(traitType) || !target.getStat().hasDefenceTrait(traitType))
				{
					return 1.0;
				}
				break;
			}
			case 3:
			{
				if (ignoreResistance)
				{
					return 1.0;
				}
				break;
			}
			default:
			{
				return 1.0;
			}
		}
		
		return Math.max(attacker.getStat().getAttackTrait(traitType) - target.getStat().getDefenceTrait(traitType), 0.05);
	}
	
	/**
	 * Calculates the damage multiplier based on creature weaknesses.<br>
	 * This method checks for traits that provide a bonus to the attacker.<br>
	 * It compares attack and defense traits between both creatures.
	 * @param attacker The {@code Creature} performing the action.
	 * @param target The {@code Creature} receiving the action.
	 * @param traitType The specific {@code TraitType} being evaluated.
	 * @return The calculated damage multiplier as a {@code double}.
	 */
	public static double calcWeaknessBonus(Creature attacker, Creature target, TraitType traitType)
	{
		double result = 1;
		final CreatureStat attackerStat = attacker.getStat();
		final CreatureStat targetStat = target.getStat();
		for (TraitType trait : TraitType.getAllWeakness())
		{
			if ((traitType != trait) && targetStat.hasDefenceTrait(trait) && attackerStat.hasAttackTrait(trait) && !targetStat.isInvulnerableTrait(traitType))
			{
				result *= Math.max(attackerStat.getAttackTrait(trait) - targetStat.getDefenceTrait(trait), 0.05);
			}
		}
		
		return result;
	}
	
	/**
	 * Calculates the weapon trait bonus based on the defender's traits.<br>
	 * This method determines how much damage is mitigated by a specific defense trait.<br>
	 * It returns a value between 0.22 and 1.0.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} receiving the attack.
	 * @return The calculated weapon trait bonus as a {@code double}.
	 */
	public static double calcWeaponTraitBonus(Creature attacker, Creature target)
	{
		return Math.max(0.22, 1.0 - target.getStat().getDefenceTrait(attacker.getAttackType().getTraitType()));
	}
	
	/**
	 * Calculates the attack trait bonus between two creatures.<br>
	 * This method considers weapon traits and general weakness bonuses.<br>
	 * It ensures the final result is at least {@code 0.05}.
	 * @param attacker The creature performing the attack.
	 * @param target The creature receiving the attack.
	 * @return The calculated trait bonus as a double.
	 */
	public static double calcAttackTraitBonus(Creature attacker, Creature target)
	{
		final double weaponTraitBonus = calcWeaponTraitBonus(attacker, target);
		if (weaponTraitBonus == 0)
		{
			return 0;
		}
		
		double weaknessBonus = 1.0;
		for (TraitType traitType : TraitType.values())
		{
			if (traitType.getType() == 2)
			{
				weaknessBonus *= calcGeneralTraitBonus(attacker, target, traitType, true);
				if (weaknessBonus == 0)
				{
					return 0;
				}
			}
		}
		
		return Math.max(weaponTraitBonus * weaknessBonus, 0.05);
	}
	
	/**
	 * Calculates the damage reduction multiplier based on a creature's basic property resistance.<br>
	 * This method checks if the {@code target} has a specific {@code BasicProperty} resistance.<br>
	 * It returns a value between 0.0 and 1.0 depending on the resistance level.
	 * @param basicProperty The type of basic property to check.
	 * @param target The creature whose resistance is being evaluated.
	 * @return A double representing the damage multiplier.
	 */
	public static double getBasicPropertyResistBonus(BasicProperty basicProperty, Creature target)
	{
		if ((basicProperty == BasicProperty.NONE) || !target.hasBasicPropertyResist())
		{
			return 1.0;
		}
		
		final BasicPropertyResist resist = target.getBasicPropertyResist(basicProperty);
		switch (resist.getResistLevel())
		{
			case 0:
			{
				return 1.0;
			}
			case 1:
			{
				return 0.6;
			}
			case 2:
			{
				return 0.3;
			}
			default:
			{
				return 0;
			}
		}
	}
	
	/**
	 * Calculates the final damage for an auto-attack between two creatures.<br>
	 * This method accounts for defense, critical hits, and special skills.<br>
	 * It also applies weapon modifiers and various trait bonuses.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} receiving the damage.
	 * @param shld The shield defense status of the target.
	 * @param crit Whether the attack is a critical hit.
	 * @param ss Whether the special skill bonus is active.
	 * @param ssBlessed Whether the special skill is blessed.
	 * @return The final calculated damage as a {@code double}.
	 */
	public static double calcAutoAttackDamage(Creature attacker, Creature target, byte shld, boolean crit, boolean ss, boolean ssBlessed)
	{
		// DEFENCE CALCULATION (pDef + sDef).
		double defence = target.getPDef();
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				defence += target.getShldDef();
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK:
			{
				return 1;
			}
		}
		
		final Weapon weapon = attacker.getActiveWeaponItem();
		final boolean isRanged = (weapon != null) && weapon.getItemType().isRanged();
		final CreatureStat attackerStat = attacker.getStat();
		final double shotsBonus = attackerStat.getValue(Stat.SHOTS_BONUS);
		final double shotsResist = target.getStat().getValue(Stat.SOULSHOT_RESISTANCE, 0) / 100;
		
		final double cAtk = crit ? calcCritDamage(attacker, target, null) : 1;
		final double cAtkAdd = crit ? calcCritDamageAdd(attacker, target, null) : 0;
		final double critMod = crit ? (isRanged ? 0.5 : 1) : 0;
		final double ssBonus = ss ? ((ssBlessed ? 4 : 2) * shotsBonus) - shotsResist : 1;
		final double randomDamage = attacker.getRandomDamageMultiplier();
		final double proxBonus = (attacker.isInFrontOf(target) ? 0 : (attacker.isBehind(target) ? 0.2 : 0.05)) * attacker.getPAtk();
		double attack = (attacker.getPAtk() * randomDamage) + proxBonus;
		
		// The attack calculation is (pAtk * cAtk * ss + cAtkAdd) * crit * weaponMod + pAtk * (1 - crit) * ss * weaponMod.
		// ````````````````````^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^````^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
		final double weaponMod = isRanged ? 154 : 77;
		attack = ((((attack * cAtk * ssBonus) + cAtkAdd) * critMod) * weaponMod) + (attack * (1 - critMod) * ssBonus * weaponMod);
		
		// DAMAGE CALCULATION (ATTACK / DEFENCE) * trait bonus * attr bonus * pvp bonus * pve bonus
		double damage = attack / defence;
		damage *= calcAttackTraitBonus(attacker, target);
		damage *= calcAttributeBonus(attacker, target, null);
		damage *= calculatePvpPveBonus(attacker, target, null, crit);
		damage *= attackerStat.getMul(Stat.AUTO_ATTACK_DAMAGE_BONUS);
		
		return Math.max(1, damage);
	}
	
	/**
	 * Retrieves the abnormal resistance value for a specific property.<br>
	 * It checks if the property is {@code PHYSICAL} or {@code MAGIC}.<br>
	 * If no matching property is found, it returns {@code 0}.
	 * @param basicProperty The type of resistance to retrieve.
	 * @param target The creature whose stats are being checked.
	 * @return The numerical value of the abnormal resistance.
	 */
	public static double getAbnormalResist(BasicProperty basicProperty, Creature target)
	{
		switch (basicProperty)
		{
			case PHYSICAL:
			{
				return target.getStat().getValue(Stat.ABNORMAL_RESIST_PHYSICAL);
			}
			case MAGIC:
			{
				return target.getStat().getValue(Stat.ABNORMAL_RESIST_MAGICAL);
			}
			default:
			{
				return 0;
			}
		}
	}
	
	/**
	 * Checks if a creature can break out of a stun effect.<br>
	 * This method evaluates the chance to remove a stun based on configuration and random values.<br>
	 * It ensures that stuns with double duration from skill mastery are not prematurely removed.
	 * @param creature The {@code Creature} to check for stun status.
	 * @return {@code true} if the stun is successfully broken, {@code false} otherwise.
	 */
	public static boolean calcStunBreak(Creature creature)
	{
		// Check if target is stunned and break it with 14% chance. (retail is 14% and 35% on crit?).
		if (PlayerConfig.ALT_GAME_STUN_BREAK && creature.hasBlockActions() && (Rnd.get(14) == 0))
		{
			// Any stun that has double duration due to skill mastery, doesn't get removed until its time reaches the usual abnormal time.
			return creature.getEffectList().hasAbnormalType(AbnormalType.STUN, info -> info.getTime() <= info.getSkill().getAbnormalTime());
		}
		
		return false;
	}
	
	/**
	 * Determines if a real target break occurs.<br>
	 * This method uses a random chance of {@code 3}.<br>
	 * It returns {@code true} if the roll is successful.
	 * @return {@code true} if the target breaks, {@code false} otherwise.
	 */
	public static boolean calcRealTargetBreak()
	{
		// Real Target breaks at 3% (Rnd > 3.0 doesn't break) probability.
		return Rnd.get(100) <= 3;
	}
	
	/**
	 * Calculates the time interval between consecutive attacks.<br>
	 * This method uses the {@code attackSpeed} to determine the delay.<br>
	 * The result is clamped to a minimum value of {@code 50}.
	 * @param attackSpeed The speed at which an entity performs attacks.
	 * @return The calculated time between attacks as an {@code int}.
	 */
	public static int calculateTimeBetweenAttacks(int attackSpeed)
	{
		// Measured Nov 2015 by Nik. Formula: atk.spd/500 = hits per second.
		return Math.max(50, (500000 / attackSpeed));
	}
	
	/**
	 * Calculates the time required to land a hit based on weapon mechanics.<br>
	 * This method applies specific multipliers for different {@code WeaponType} categories.<br>
	 * It accounts for whether the attack is {@code twoHanded} or a {@code secondHit}.
	 * @param totalAttackTime The base time of the attack.
	 * @param attackType The type of weapon being used.
	 * @param twoHanded Whether the weapon is held with two hands.
	 * @param secondHit Whether this is the second hit in a sequence.
	 * @return The calculated time to hit as an {@code int}.
	 */
	public static int calculateTimeToHit(int totalAttackTime, WeaponType attackType, boolean twoHanded, boolean secondHit)
	{
		// Gracia Final Retail confirmed:
		// The time to damage for one-handed attacks is TotalBasicAttackTime * 0.644, two-handed single hits are TotalBasicAttackTime * 0.735, two-handed double hits are TotalBasicAttackTime * 0.2726 and TotalBasicAttackTime * 0.6, and bow or crossbow attacks are TotalBasicAttackTime * 0.978.
		
		// Measured July 2016 by Nik.
		// Due to retail packet delay, we are unable to gather too accurate results. Therefore the below formulas are based on original Gracia Final values.
		// Any original values that appear higher than tested have been replaced with the tested values, because even with packet delay it is obvious they are wrong.
		// All other original values are compared with the test results and differences are considered to be too insignificant and mostly caused due to packet delay.
		switch (attackType)
		{
			case BOW:
			case CROSSBOW:
			case TWOHANDCROSSBOW:
			{
				return (int) (totalAttackTime * 0.95);
			}
			case DUALBLUNT:
			case DUALDAGGER:
			case DUAL:
			case DUALFIST:
			{
				if (secondHit)
				{
					return (int) (totalAttackTime * 0.6);
				}
				
				return (int) (totalAttackTime * 0.2726);
			}
			default:
			{
				if (twoHanded)
				{
					return (int) (totalAttackTime * 0.735);
				}
				
				return (int) (totalAttackTime * 0.644);
			}
		}
	}
	
	/**
	 * Calculates the reuse time for a ranged weapon attack.<br>
	 * This method returns {@code 0} if no weapon is provided or if the weapon is not of a ranged type.<br>
	 * The result is based on the creature's attack speed stat.
	 * @param creature The {@code Creature} performing the action.
	 * @param weapon The {@code Weapon} being used for the attack.
	 * @return The calculated reuse time in milliseconds.
	 */
	public static int calculateReuseTime(Creature creature, Weapon weapon)
	{
		if (weapon == null)
		{
			return 0;
		}
		
		final WeaponType defaultAttackType = weapon.getItemType();
		final Transform transform = creature.getTransformation();
		final WeaponType weaponType = transform == null ? defaultAttackType : transform.getBaseAttackType(creature, defaultAttackType);
		
		// Only ranged weapons should continue for now.
		if (!weaponType.isRanged())
		{
			return 0;
		}
		
		return 900000 / creature.getStat().getPAtkSpd();
	}
	
	/**
	 * Calculates the damage multiplier based on PvP or PvE scenarios.<br>
	 * It determines if the interaction is between players or against monsters.<br>
	 * The result accounts for class multipliers, skill types, and raid bonuses.<br>
	 * It ensures the final bonus value is never lower than 0.05.
	 * @param attacker The {@code Creature} performing the action.
	 * @param target The {@code Creature} receiving the action.
	 * @param skill The {@code Skill} being used, or {@code null} for auto-attack.
	 * @param crit A boolean indicating if the hit is a critical strike.
	 * @return The calculated damage multiplier as a {@code double}.
	 */
	public static double calculatePvpPveBonus(Creature attacker, Creature target, Skill skill, boolean crit)
	{
		final Player attackerPlayer = attacker.asPlayer();
		final Player targetPlayer = target.asPlayer();
		
		// PvP bonus
		if (attacker.isPlayable() && target.isPlayable())
		{
			final double pvpAttack;
			final double pvpDefense;
			if (skill != null)
			{
				if (skill.isMagic())
				{
					// Magical Skill PvP
					pvpAttack = attacker.getStat().getMul(Stat.PVP_MAGICAL_SKILL_DAMAGE, 1) * ClassBalanceConfig.PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[attackerPlayer.getPlayerClass().getId()];
					pvpDefense = target.getStat().getMul(Stat.PVP_MAGICAL_SKILL_DEFENCE, 1) * ClassBalanceConfig.PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS[targetPlayer.getPlayerClass().getId()];
				}
				else
				{
					// Physical Skill PvP
					pvpAttack = attacker.getStat().getMul(Stat.PVP_PHYSICAL_SKILL_DAMAGE, 1) * ClassBalanceConfig.PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[attackerPlayer.getPlayerClass().getId()];
					pvpDefense = target.getStat().getMul(Stat.PVP_PHYSICAL_SKILL_DEFENCE, 1) * ClassBalanceConfig.PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[targetPlayer.getPlayerClass().getId()];
				}
			}
			else
			{
				// Autoattack PvP
				pvpAttack = attacker.getStat().getMul(Stat.PVP_PHYSICAL_ATTACK_DAMAGE, 1) * ClassBalanceConfig.PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[attackerPlayer.getPlayerClass().getId()];
				pvpDefense = target.getStat().getMul(Stat.PVP_PHYSICAL_ATTACK_DEFENCE, 1) * ClassBalanceConfig.PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[targetPlayer.getPlayerClass().getId()];
			}
			
			// Dragon weapon defence stat.
			final double dragonDefense;
			final Weapon weapon = attacker.getActiveWeaponItem();
			if ((weapon != null) && weapon.isDragonWeapon())
			{
				dragonDefense = target.getStat().getMul(Stat.DRAGON_WEAPON_DEFENCE, 1);
			}
			else
			{
				dragonDefense = 1;
			}
			
			return Math.max(0.05, dragonDefense * (1 + (pvpAttack - pvpDefense))); // Bonus should not be negative.
		}
		
		// PvE Bonus
		if (target.isAttackable() || attacker.isAttackable())
		{
			final double pveAttack;
			final double pveDefense;
			final double pveRaidAttack;
			final double pveRaidDefense;
			final CreatureStat attackerStat = attacker.getStat();
			final boolean attackerPveIsRaid = attacker.isRaid();
			
			double pvePenalty = 1;
			if (!target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= NpcConfig.MIN_NPC_LEVEL_DMG_PENALTY) && (attackerPlayer != null) && ((target.getLevel() - attackerPlayer.getLevel()) >= 2))
			{
				final int levelDiff = target.getLevel() - attackerPlayer.getLevel() - 1;
				if (levelDiff >= NpcConfig.NPC_SKILL_DMG_PENALTY.length)
				{
					pvePenalty = NpcConfig.NPC_SKILL_DMG_PENALTY[NpcConfig.NPC_SKILL_DMG_PENALTY.length - 1];
				}
				else
				{
					pvePenalty = NpcConfig.NPC_SKILL_DMG_PENALTY[levelDiff];
				}
			}
			
			if (skill != null)
			{
				if (skill.isMagic())
				{
					// Magical Skill PvE
					pveAttack = attackerStat.getMul(Stat.PVE_MAGICAL_SKILL_DAMAGE, 1) * (attackerPlayer == null ? 1 : ClassBalanceConfig.PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[attackerPlayer.getPlayerClass().getId()]);
					pveDefense = target.getStat().getMul(Stat.PVE_MAGICAL_SKILL_DEFENCE, 1) * (targetPlayer == null ? 1 : ClassBalanceConfig.PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS[targetPlayer.getPlayerClass().getId()]);
					pveRaidAttack = attackerPveIsRaid ? attackerStat.getMul(Stat.PVE_RAID_MAGICAL_SKILL_DAMAGE, 1) : 1;
					pveRaidDefense = attackerPveIsRaid ? attackerStat.getMul(Stat.PVE_RAID_MAGICAL_SKILL_DEFENCE, 1) : 1;
				}
				else
				{
					// Physical Skill PvE
					pveAttack = attackerStat.getMul(Stat.PVE_PHYSICAL_SKILL_DAMAGE, 1) * (attackerPlayer == null ? 1 : ClassBalanceConfig.PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[attackerPlayer.getPlayerClass().getId()]);
					pveDefense = target.getStat().getMul(Stat.PVE_PHYSICAL_SKILL_DEFENCE, 1) * (targetPlayer == null ? 1 : ClassBalanceConfig.PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[targetPlayer.getPlayerClass().getId()]);
					pveRaidAttack = attackerPveIsRaid ? attackerStat.getMul(Stat.PVE_RAID_PHYSICAL_SKILL_DAMAGE, 1) : 1;
					pveRaidDefense = attackerPveIsRaid ? attackerStat.getMul(Stat.PVE_RAID_PHYSICAL_SKILL_DEFENCE, 1) : 1;
				}
			}
			else
			{
				// Autoattack PvE
				pveAttack = attackerStat.getMul(Stat.PVE_PHYSICAL_ATTACK_DAMAGE, 1) * (attackerPlayer == null ? 1 : ClassBalanceConfig.PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[attackerPlayer.getPlayerClass().getId()]);
				pveDefense = target.getStat().getMul(Stat.PVE_PHYSICAL_ATTACK_DEFENCE, 1) * (targetPlayer == null ? 1 : ClassBalanceConfig.PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[targetPlayer.getPlayerClass().getId()]);
				pveRaidAttack = attackerPveIsRaid ? attackerStat.getMul(Stat.PVE_RAID_PHYSICAL_ATTACK_DAMAGE, 1) : 1;
				pveRaidDefense = attackerPveIsRaid ? attackerStat.getMul(Stat.PVE_RAID_PHYSICAL_ATTACK_DEFENCE, 1) : 1;
			}
			
			return Math.max(0.05, (1 + ((pveAttack * pveRaidAttack) - (pveDefense * pveRaidDefense))) * pvePenalty); // Bonus should not be negative.
		}
		
		return 1;
	}
}
