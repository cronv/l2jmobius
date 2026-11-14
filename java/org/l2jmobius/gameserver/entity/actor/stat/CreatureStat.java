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
package org.l2jmobius.gameserver.entity.actor.stat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.ArmorMagicalDefenceType;
import org.l2jmobius.gameserver.data.enums.ArmorPhysicalDefenceType;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Position;
import org.l2jmobius.gameserver.entity.actor.holders.creature.DelayedPumpHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillConditionScope;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.MoveType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.TraitType;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoAddMDefend;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoAddPDefend;
import org.l2jmobius.gameserver.util.MathUtil;

/**
 * Represents the statistics and attributes of a {@code Creature}.<br>
 * This class manages various stats like health, mana, and defense for entities in the game.<br>
 * It handles calculations for modifiers, buffs, and base values.
 */
public class CreatureStat
{
	private final Creature _creature;
	private long _exp = 0;
	private long _sp = 0;
	private int _level = 1;
	/** Creature's maximum buff count. */
	private int _maxBuffCount = PlayerConfig.BUFFS_MAX_AMOUNT;
	private double _vampiricSum = 0;
	private double _mpVampiricSum = 0;
	
	private final Map<Stat, Double> _statsAdd = new EnumMap<>(Stat.class);
	private final Map<Stat, Double> _statsMul = new EnumMap<>(Stat.class);
	private final Map<Stat, Map<MoveType, Double>> _moveTypeStats = new ConcurrentHashMap<>();
	private final Map<Integer, Double> _reuseStat = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _changedSkillBaseReuseTime = new ConcurrentHashMap<>();
	private final Map<Integer, Double> _mpConsumeStat = new ConcurrentHashMap<>();
	private final Map<Integer, Deque<Double>> _skillEvasionStat = new ConcurrentHashMap<>();
	private final Map<Stat, Map<Position, Double>> _positionStats = new ConcurrentHashMap<>();
	private final Map<Stat, Double> _fixedValue = new ConcurrentHashMap<>();
	
	/**
	 * Tracks the part added by {@code StatAddForLevel}.<br>
	 * Used for client packets that must exclude that specific amount.
	 */
	private double _weaponPAtkLevelBonus = 0;
	private double _weaponMAtkLevelBonus = 0;
	
	private final float[] _attackTraitValues = new float[TraitType.values().length];
	private final float[] _defenceTraitValues = new float[TraitType.values().length];
	private final Set<TraitType> _attackTraits = EnumSet.noneOf(TraitType.class);
	private final Set<TraitType> _defenceTraits = EnumSet.noneOf(TraitType.class);
	private final Set<TraitType> _invulnerableTraits = EnumSet.noneOf(TraitType.class);
	
	/** Values to be recalculated after every stat update */
	private double _attackSpeedMultiplier = 1;
	private double _mAttackSpeedMultiplier = 1;
	
	private ScheduledFuture<?> _recalculateStatsTask = null;
	private final AtomicBoolean _broadcast = new AtomicBoolean();
	private final ReentrantReadWriteLock _lock = new ReentrantReadWriteLock();
	
	private final Map<ArmorMagicalDefenceType, Integer> _magicalDefenceTypes = new EnumMap<>(ArmorMagicalDefenceType.class);
	private final Map<ArmorPhysicalDefenceType, Integer> _physicalDefenceTypes = new EnumMap<>(ArmorPhysicalDefenceType.class);
	
	/**
	 * Creates a new {@code CreatureStat} instance for a specific creature.<br>
	 * This constructor initializes the internal trait values to their default states.
	 * @param creature The {@code Creature} object associated with these statistics.
	 */
	public CreatureStat(Creature creature)
	{
		_creature = creature;
		for (int i = 0; i < TraitType.values().length; i++)
		{
			_attackTraitValues[i] = 1;
			_defenceTraitValues[i] = 0;
		}
	}
	
	/**
	 * Retrieves the current accuracy value of the creature.<br>
	 * This value is used to determine hit success in combat.
	 * @return the accuracy as an {@code int}
	 */
	public int getAccuracy()
	{
		return (int) getValue(Stat.ACCURACY_COMBAT);
	}
	
	/**
	 * Retrieves the current CP regeneration rate.<br>
	 * This value is used to determine how much CP a creature recovers over time.
	 * @return The current CP regeneration rate as an {@code int}.
	 */
	public int getCpRegen()
	{
		return (int) getValue(Stat.REGENERATE_CP_RATE);
	}
	
	/**
	 * Retrieves the current health regeneration rate.<br>
	 * This value is used to determine how much HP a creature recovers over time.
	 * @return The health regeneration rate as an {@code int}.
	 */
	public int getHpRegen()
	{
		return (int) getValue(Stat.REGENERATE_HP_RATE);
	}
	
	/**
	 * Retrieves the current Mana Point regeneration rate.<br>
	 * This value is used to determine how much mana a creature recovers over time.
	 * @return The current {@code int} value of the MP regeneration rate.
	 */
	public int getMpRegen()
	{
		return (int) getValue(Stat.REGENERATE_MP_RATE);
	}
	
	/**
	 * Retrieves the current magic accuracy value.<br>
	 * This value is used to determine how well a creature resists magical attacks.
	 * @return The magic accuracy as an {@code int}.
	 */
	public int getMagicAccuracy()
	{
		return (int) getValue(Stat.ACCURACY_MAGIC);
	}
	
	/**
	 * Retrieves the {@code Creature} associated with these statistics.<br>
	 * This method returns the primary entity owner of the stats.
	 * @return The {@code Creature} instance.
	 */
	public Creature getActiveChar()
	{
		return _creature;
	}
	
	/**
	 * Retrieves the current attack speed multiplier.<br>
	 * This value determines how fast a creature can perform physical attacks.
	 * @return The current {@code double} value of the attack speed multiplier.
	 */
	public double getAttackSpeedMultiplier()
	{
		return _attackSpeedMultiplier;
	}
	
	/**
	 * Retrieves the magical attack speed multiplier.<br>
	 * This value determines how much the magical attack speed is scaled.
	 * @return The current {@code double} value of the magical attack speed multiplier.
	 */
	public double getMAttackSpeedMultiplier()
	{
		return _mAttackSpeedMultiplier;
	}
	
	/**
	 * Retrieves the current Constitution value.<br>
	 * This value represents the character's physical endurance.
	 * @return The integer value of the {@code CON} stat.
	 */
	public int getCON()
	{
		return (int) getValue(Stat.STAT_CON);
	}
	
	/**
	 * Calculates the final critical damage value.<br>
	 * This method takes a base value and applies relevant modifiers.
	 * @param init The initial critical damage value to use for calculation.
	 * @return The calculated critical damage as a {@code double}.
	 */
	public double getCriticalDmg(double init)
	{
		return getValue(Stat.CRITICAL_DAMAGE, init);
	}
	
	/**
	 * Retrieves the current critical hit rate.<br>
	 * This value is returned as an {@code int}.
	 * @return The character's critical hit rate.
	 */
	public int getCriticalHit()
	{
		return (int) getValue(Stat.CRITICAL_RATE);
	}
	
	/**
	 * Retrieves the skill critical rate for the creature.<br>
	 * This value is used to determine how often skills trigger a critical hit.
	 * @return The current skill critical rate as an {@code int}.
	 */
	public int getPSkillCriticalRate()
	{
		return (int) getValue(Stat.CRITICAL_RATE_SKILL);
	}
	
	/**
	 * Retrieves the current Dexterity value.<br>
	 * This method returns the {@code int} value for the {@code STAT_DEX} statistic.
	 * @return The current Dexterity value as an {@code int}.
	 */
	public int getDEX()
	{
		return (int) getValue(Stat.STAT_DEX);
	}
	
	/**
	 * Retrieves the current evasion rate of the creature.<br>
	 * This value is used to determine how often attacks are avoided.
	 * @return the evasion rate as an {@code int}
	 */
	public int getEvasionRate()
	{
		return (int) getValue(Stat.EVASION_RATE);
	}
	
	/**
	 * Retrieves the current magic evasion rate for the creature.<br>
	 * This value is used to determine how often magical attacks are avoided.
	 * @return The magic evasion rate as an {@code int}.
	 */
	public int getMagicEvasionRate()
	{
		return (int) getValue(Stat.MAGIC_EVASION_RATE);
	}
	
	/**
	 * Retrieves the current experience points of the creature.
	 * @return the total amount of {@code exp} as a {@code long}.
	 */
	public long getExp()
	{
		return _exp;
	}
	
	/**
	 * Sets the experience points for the creature.<br>
	 * This updates the internal {@code _exp} field with a new value.
	 * @param value The new experience value to set.
	 */
	public void setExp(long value)
	{
		_exp = value;
	}
	
	/**
	 * Retrieves the current {@code INT} stat value.<br>
	 * This method returns the integer representation of the {@code STAT_INT} attribute.
	 * @return The current {@code INT} value.
	 */
	public int getINT()
	{
		return (int) getValue(Stat.STAT_INT);
	}
	
	/**
	 * Retrieves the current level of the creature.
	 * @return The current level as an {@code int}.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Sets the current level of the creature.<br>
	 * This updates the internal {@code _level} field with the provided value.
	 * @param value The new level to assign.
	 */
	public void setLevel(int value)
	{
		_level = value;
	}
	
	/**
	 * Calculates the magical attack range for a specific skill.<br>
	 * If the {@code Skill} is provided, it uses the magic attack range formula based on that skill's cast range.<br>
	 * If the {@code Skill} is {@code null}, it returns the base attack range of the creature template.
	 * @param skill The {@code Skill} to use for the calculation.
	 * @return The calculated magical attack range as an {@code int}.
	 */
	public int getMagicalAttackRange(Skill skill)
	{
		if (skill != null)
		{
			final int range = skill.getCastRange();
			if (range > 0)
			{
				return range + (int) getValue(Stat.MAGIC_ATTACK_RANGE, 0);
			}
			
			return range;
		}
		
		return _creature.getTemplate().getBaseAttackRange();
	}
	
	/**
	 * Retrieves the maximum CP value for the creature.<br>
	 * This method returns the current {@code MAX_CP} stat as an {@code int}.
	 * @return The maximum CP of the creature.
	 */
	public int getMaxCp()
	{
		return (int) getValue(Stat.MAX_CP);
	}
	
	/**
	 * Calculates the maximum amount of {@code CP} that can be recovered.<br>
	 * This value is based on the {@code MAX_RECOVERABLE_CP} stat and the current {@code getMaxCp()} value.
	 * @return The maximum recoverable {@code CP} as an {@code int}.
	 */
	public int getMaxRecoverableCp()
	{
		return (int) getValue(Stat.MAX_RECOVERABLE_CP, getMaxCp());
	}
	
	/**
	 * Retrieves the maximum health points of the creature.<br>
	 * This value is used to determine the upper limit of current HP.
	 * @return The maximum health as a {@code long}.
	 */
	public long getMaxHp()
	{
		return (long) getValue(Stat.MAX_HP);
	}
	
	/**
	 * Calculates the maximum amount of health that can be recovered.<br>
	 * This value is based on the {@code MAX_RECOVERABLE_HP} stat multiplier.<br>
	 * It uses the current {@code getMaxHp()} as the base value.
	 * @return The total maximum recoverable health as a {@code long}.
	 */
	public long getMaxRecoverableHp()
	{
		return (long) getValue(Stat.MAX_RECOVERABLE_HP, getMaxHp());
	}
	
	/**
	 * Retrieves the maximum mana points for the creature.<br>
	 * This value is calculated based on the {@code Stat.MAX_MP} statistic.
	 * @return The current maximum mana as an {@code int}.
	 */
	public int getMaxMp()
	{
		return (int) getValue(Stat.MAX_MP);
	}
	
	/**
	 * Calculates the maximum amount of {@code MP} that can be recovered.<br>
	 * This value is based on the {@code MAX_RECOVERABLE_MP} stat multiplier.<br>
	 * It uses the current {@code getMaxMp()} as a base for the calculation.
	 * @return The total maximum recoverable {@code MP}.
	 */
	public int getMaxRecoverableMp()
	{
		return (int) getValue(Stat.MAX_RECOVERABLE_MP, getMaxMp());
	}
	
	/**
	 * Retrieves the current magical attack value.<br>
	 * This method returns the {@code int} value for {@code Stat.MAGIC_ATTACK}.
	 * @return The magical attack amount.
	 */
	public int getMAtk()
	{
		if (_creature.isServitor())
		{
			final Player owner = _creature.asServitor().getOwner();
			if (owner != null)
			{
				return (int) (1.1 * owner.getStat().getValue(Stat.MAGIC_ATTACK));
			}
		}
		
		return (int) getValue(Stat.MAGIC_ATTACK);
	}
	
	/**
	 * Retrieves the magic attack bonus provided by the equipped weapon.<br>
	 * This value is fetched from the {@code Stat.WEAPON_BONUS_MAGIC_ATTACK} stat.
	 * @return The integer value of the weapon's magic attack bonus.
	 */
	public int getWeaponBonusMAtk()
	{
		return (int) getValue(Stat.WEAPON_BONUS_MAGIC_ATTACK);
	}
	
	/**
	 * Retrieves the magic attack speed of the creature.<br>
	 * This value is used to determine how fast magical attacks are performed.
	 * @return The current magic attack speed as an {@code int}.
	 */
	public int getMAtkSpd()
	{
		return (int) getValue(Stat.MAGIC_ATTACK_SPEED);
	}
	
	/**
	 * Retrieves the magic critical hit rate.<br>
	 * This value is used to determine how often a character lands a critical magical attack.
	 * @return The current magic critical hit rate as an {@code int}.
	 */
	public int getMCriticalHit()
	{
		return (int) getValue(Stat.MAGIC_CRITICAL_RATE);
	}
	
	/**
	 * Retrieves the magical defense value of the creature.<br>
	 * This method returns the current {@code MAGICAL_DEFENCE} stat as an {@code int}.
	 * @return The magical defense value.
	 */
	public int getMDef()
	{
		if (_creature.isServitor())
		{
			final Player owner = _creature.asServitor().getOwner();
			if (owner != null)
			{
				return (int) (0.7 * owner.getStat().getValue(Stat.MAGICAL_DEFENCE));
			}
		}
		
		return (int) getValue(Stat.MAGICAL_DEFENCE);
	}
	
	/**
	 * Retrieves the current value of the MEN stat.<br>
	 * This method returns the integer value associated with {@code Stat.STAT_MEN}.
	 * @return the current MEN stat value as an {@code int}
	 */
	public int getMEN()
	{
		return (int) getValue(Stat.STAT_MEN);
	}
	
	/**
	 * Retrieves the current Luck value for the creature.<br>
	 * This method returns the {@code int} representation of the {@code STAT_LUC} stat.
	 * @return The current Luck value.
	 */
	public int getLUC()
	{
		return (int) getValue(Stat.STAT_LUC);
	}
	
	/**
	 * Retrieves the current Charisma value.<br>
	 * This method returns the integer value of the {@code STAT_CHA} statistic.
	 * @return The current Charisma value as an {@code int}.
	 */
	public int getCHA()
	{
		return (int) getValue(Stat.STAT_CHA);
	}
	
	/**
	 * Calculates the movement speed multiplier for the creature.<br>
	 * This value is used to normalize movement speed based on the environment.<br>
	 * It compares the current move speed against the base template speed.<br>
	 * The result accounts for whether the creature is swimming or walking.
	 * @return the calculated movement speed multiplier as a {@code double}.
	 */
	public double getMovementSpeedMultiplier()
	{
		double baseSpeed;
		if (_creature.isInsideZone(ZoneId.WATER))
		{
			baseSpeed = _creature.getTemplate().getBaseValue(_creature.isRunning() ? Stat.SWIM_RUN_SPEED : Stat.SWIM_WALK_SPEED, 0);
		}
		else
		{
			baseSpeed = _creature.getTemplate().getBaseValue(_creature.isRunning() ? Stat.RUN_SPEED : Stat.WALK_SPEED, 0);
		}
		
		return getMoveSpeed() * (1. / baseSpeed);
	}
	
	/**
	 * Retrieves the current movement speed of the creature.<br>
	 * It checks if the creature is in water to determine which stat to use.<br>
	 * If the creature is swimming, it returns the swim speed.<br>
	 * Otherwise, it returns the standard run speed.
	 * @return the current movement speed as a {@code double}.
	 */
	public double getRunSpeed()
	{
		return getValue(_creature.isInsideZone(ZoneId.WATER) ? Stat.SWIM_RUN_SPEED : Stat.RUN_SPEED);
	}
	
	/**
	 * Retrieves the current walking speed of the creature.<br>
	 * This value changes depending on whether the creature is in water or on land.
	 * @return The walk speed as a {@code double}.
	 */
	public double getWalkSpeed()
	{
		return getValue(_creature.isInsideZone(ZoneId.WATER) ? Stat.SWIM_WALK_SPEED : Stat.WALK_SPEED);
	}
	
	/**
	 * Retrieves the current swim run speed value.<br>
	 * This method returns the value associated with {@code Stat.SWIM_RUN_SPEED}.
	 * @return the swim run speed as a {@code double}.
	 */
	public double getSwimRunSpeed()
	{
		return getValue(Stat.SWIM_RUN_SPEED);
	}
	
	/**
	 * Retrieves the current swim and walk speed.<br>
	 * This value is used to determine how fast a creature moves on land or in water.
	 * @return The movement speed as a {@code double}.
	 */
	public double getSwimWalkSpeed()
	{
		return getValue(Stat.SWIM_WALK_SPEED);
	}
	
	/**
	 * Retrieves the current movement speed of the creature.<br>
	 * This value depends on whether the creature is running or walking.<br>
	 * It also accounts for whether the creature is currently in water.
	 * @return the calculated movement speed as a {@code double}
	 */
	public double getMoveSpeed()
	{
		if (_creature.isInsideZone(ZoneId.WATER))
		{
			return _creature.isRunning() ? getSwimRunSpeed() : getSwimWalkSpeed();
		}
		
		return _creature.isRunning() ? getRunSpeed() : getWalkSpeed();
	}
	
	/**
	 * Retrieves the physical attack value of the creature.<br>
	 * This method returns the current {@code Stat.PHYSICAL_ATTACK} as an {@code int}.
	 * @return The physical attack value.
	 */
	public int getPAtk()
	{
		if (_creature.isServitor())
		{
			final Player owner = _creature.asServitor().getOwner();
			if (owner != null)
			{
				return (int) (1.1 * owner.getStat().getValue(Stat.PHYSICAL_ATTACK));
			}
		}
		
		return (int) getValue(Stat.PHYSICAL_ATTACK);
	}
	
	/**
	 * Retrieves the physical attack bonus provided by the weapon.<br>
	 * This value is fetched from the {@code Stat.WEAPON_BONUS_PHYSICAL_ATTACK} stat.
	 * @return The integer value of the weapon's physical attack bonus.
	 */
	public int getWeaponBonusPAtk()
	{
		return (int) getValue(Stat.WEAPON_BONUS_PHYSICAL_ATTACK);
	}
	
	/**
	 * Retrieves the physical attack speed of the creature.<br>
	 * This value is obtained from the {@code Stat.PHYSICAL_ATTACK_SPEED} stat.
	 * @return The current physical attack speed as an {@code int}.
	 */
	public int getPAtkSpd()
	{
		return (int) getValue(Stat.PHYSICAL_ATTACK_SPEED);
	}
	
	/**
	 * Retrieves the physical defense value of the creature.<br>
	 * This method returns the current {@code Stat.PHYSICAL_DEFENCE} as an {@code int}.
	 * @return The physical defense value.
	 */
	public int getPDef()
	{
		if (_creature.isServitor())
		{
			final Player owner = _creature.asServitor().getOwner();
			if (owner != null)
			{
				return (int) (0.7 * owner.getStat().getValue(Stat.PHYSICAL_DEFENCE));
			}
		}
		
		return (int) getValue(Stat.PHYSICAL_DEFENCE);
	}
	
	/**
	 * Retrieves the physical attack range of the creature.<br>
	 * This value is used to determine how far a character can strike enemies.
	 * @return The physical attack range as an {@code int}.
	 */
	public int getPhysicalAttackRange()
	{
		return (int) getValue(Stat.PHYSICAL_ATTACK_RANGE);
	}
	
	/**
	 * Retrieves the physical attack radius of the creature.<br>
	 * This value determines how far a physical attack can reach.
	 * @return the physical attack radius as an {@code int}.
	 */
	public int getPhysicalAttackRadius()
	{
		return 40;
	}
	
	/**
	 * Retrieves the physical attack angle of the creature.<br>
	 * This value is currently returned as a default of {@code 0}.
	 * @return The current physical attack angle.
	 */
	public int getPhysicalAttackAngle()
	{
		return 0;
	}
	
	/**
	 * Retrieves the weapon reuse modifier for the creature.<br>
	 * This value is used to calculate how quickly a weapon can be reused.<br>
	 * It returns the value of {@code Stat.ATK_REUSE} or a default of {@code 1}.
	 * @return The current weapon reuse modifier as a {@code double}.
	 */
	public double getWeaponReuseModifier()
	{
		return getValue(Stat.ATK_REUSE, 1);
	}
	
	/**
	 * Retrieves the shield defense value.<br>
	 * This method returns the current value for {@code Stat.SHIELD_DEFENCE}.
	 * @return The integer value of the shield defense stat.
	 */
	public int getShldDef()
	{
		return (int) getValue(Stat.SHIELD_DEFENCE);
	}
	
	/**
	 * Retrieves the current stamina points.
	 * @return the current amount of {@code sp}
	 */
	public long getSp()
	{
		return _sp;
	}
	
	/**
	 * Sets the current special points of the creature.<br>
	 * This method updates the {@code _sp} field with a new value.
	 * @param value The new amount of special points to set.
	 */
	public void setSp(long value)
	{
		_sp = value;
	}
	
	/**
	 * Retrieves the current Strength value.<br>
	 * This method returns the {@code int} value associated with {@code Stat.STAT_STR}.
	 * @return The current Strength stat as an {@code int}.
	 */
	public int getSTR()
	{
		return (int) getValue(Stat.STAT_STR);
	}
	
	/**
	 * Retrieves the current {@code WIT} stat value.<br>
	 * This method returns the integer value of the {@code STAT_WIT} attribute.
	 * @return The current {@code WIT} stat as an {@code int}.
	 */
	public int getWIT()
	{
		return (int) getValue(Stat.STAT_WIT);
	}
	
	/**
	 * Calculates the amount of MP consumed by a specific skill.<br>
	 * This method accounts for dance penalties and magic type multipliers.<br>
	 * It returns 1 if the provided {@code Skill} is {@code null}.
	 * @param skill The {@code Skill} to calculate the cost for.
	 * @return The total MP cost as an {@code int}.
	 */
	public int getMpConsume(Skill skill)
	{
		if (skill == null)
		{
			return 1;
		}
		
		final int skillMpConsume = skill.getMpConsume();
		double mpConsume = skillMpConsume;
		final double nextDanceMpCost = Math.ceil(skillMpConsume / 2.);
		if (skill.isDance() && PlayerConfig.DANCE_CONSUME_ADDITIONAL_MP && (_creature != null) && (_creature.getDanceCount() > 0))
		{
			mpConsume += _creature.getDanceCount() * nextDanceMpCost;
		}
		
		return (int) (mpConsume * getMpConsumeTypeValue(skill.getMagicType()));
	}
	
	/**
	 * Retrieves the initial MP cost for a specific skill.<br>
	 * If the provided {@code Skill} is {@code null}, it returns a default value of 1.
	 * @param skill The {@code Skill} to check for its MP consumption.
	 * @return The amount of MP consumed initially by the skill.
	 */
	public int getMpInitialConsume(Skill skill)
	{
		if (skill == null)
		{
			return 1;
		}
		
		return skill.getMpInitialConsume();
	}
	
	/**
	 * Retrieves the primary attack element of the creature.<br>
	 * It first checks the active weapon for an attribute type.<br>
	 * If no weapon attribute exists, it finds the highest value among all elements.
	 * @return the {@code AttributeType} of the strongest attack element.
	 */
	public AttributeType getAttackElement()
	{
		final Item weaponInstance = _creature.getActiveWeaponInstance();
		
		// 1st order - weapon element
		if ((weaponInstance != null) && (weaponInstance.getAttackAttributeType() != AttributeType.NONE))
		{
			return weaponInstance.getAttackAttributeType();
		}
		
		// temp fix starts
		int tempVal = 0;
		final int[] stats =
		{
			getAttackElementValue(AttributeType.FIRE),
			getAttackElementValue(AttributeType.WATER),
			getAttackElementValue(AttributeType.WIND),
			getAttackElementValue(AttributeType.EARTH),
			getAttackElementValue(AttributeType.HOLY),
			getAttackElementValue(AttributeType.DARK)
		};
		
		AttributeType returnVal = AttributeType.NONE;
		for (byte x = 0; x < stats.length; x++)
		{
			if (stats[x] > tempVal)
			{
				returnVal = AttributeType.findByClientId(x);
				tempVal = stats[x];
			}
		}
		
		return returnVal;
	}
	
	/**
	 * Retrieves the numerical value for a specific attack attribute.<br>
	 * This method maps an {@code AttributeType} to its corresponding power stat.<br>
	 * It returns 0 if the provided attribute is not recognized.
	 * @param attackAttribute The type of attack attribute to retrieve.
	 * @return The integer value of the requested attack attribute.
	 */
	public int getAttackElementValue(AttributeType attackAttribute)
	{
		switch (attackAttribute)
		{
			case FIRE:
			{
				return (int) getValue(Stat.FIRE_POWER);
			}
			case WATER:
			{
				return (int) getValue(Stat.WATER_POWER);
			}
			case WIND:
			{
				return (int) getValue(Stat.WIND_POWER);
			}
			case EARTH:
			{
				return (int) getValue(Stat.EARTH_POWER);
			}
			case HOLY:
			{
				return (int) getValue(Stat.HOLY_POWER);
			}
			case DARK:
			{
				return (int) getValue(Stat.DARK_POWER);
			}
			default:
			{
				return 0;
			}
		}
	}
	
	/**
	 * Retrieves the value of a specific defense attribute.<br>
	 * This method returns the resistance value based on the provided {@code AttributeType}.<br>
	 * If the type is not recognized, it returns the base attribute resistance.
	 * @param defenseAttribute The type of defense attribute to retrieve.
	 * @return The integer value of the requested defense attribute.
	 */
	public int getDefenseElementValue(AttributeType defenseAttribute)
	{
		switch (defenseAttribute)
		{
			case FIRE:
			{
				return (int) getValue(Stat.FIRE_RES);
			}
			case WATER:
			{
				return (int) getValue(Stat.WATER_RES);
			}
			case WIND:
			{
				return (int) getValue(Stat.WIND_RES);
			}
			case EARTH:
			{
				return (int) getValue(Stat.EARTH_RES);
			}
			case HOLY:
			{
				return (int) getValue(Stat.HOLY_RES);
			}
			case DARK:
			{
				return (int) getValue(Stat.DARK_RES);
			}
			default:
			{
				return (int) getValue(Stat.BASE_ATTRIBUTE_RES);
			}
		}
	}
	
	/**
	 * Adds a specific value to an attack trait.<br>
	 * This method updates the total value for the given {@code TraitType}.<br>
	 * It also ensures the trait is added to the set of active traits.
	 * @param traitType The type of the attack trait to modify.
	 * @param value The amount to add to the current trait value.
	 */
	public void mergeAttackTrait(TraitType traitType, float value)
	{
		_lock.readLock().lock();
		try
		{
			_attackTraitValues[traitType.ordinal()] += value;
			_attackTraits.add(traitType);
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Removes a specific amount from an attack trait.<br>
	 * If the resulting value reaches 1, the trait is removed from the set.
	 * @param traitType The {@code TraitType} to modify.
	 * @param value The amount to subtract from the current trait value.
	 */
	public void removeAttackTrait(TraitType traitType, float value)
	{
		_lock.readLock().lock();
		try
		{
			final int traitOrdinal = traitType.ordinal();
			_attackTraitValues[traitOrdinal] -= value;
			if (_attackTraitValues[traitOrdinal] == 1)
			{
				_attackTraits.remove(traitType);
			}
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Retrieves the value of a specific attack trait.<br>
	 * This method returns the float value associated with the provided {@code TraitType}.
	 * @param traitType The type of attack trait to retrieve.
	 * @return The float value of the requested attack trait.
	 */
	public float getAttackTrait(TraitType traitType)
	{
		_lock.readLock().lock();
		try
		{
			return _attackTraitValues[traitType.ordinal()];
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Checks if the actor possesses a specific attack trait.<br>
	 * This method verifies if the provided {@code TraitType} exists in the current set of traits.
	 * @param traitType The type of attack trait to check for.
	 * @return {@code true} if the actor has the specified trait, otherwise {@code false}.
	 */
	public boolean hasAttackTrait(TraitType traitType)
	{
		_lock.readLock().lock();
		try
		{
			return _attackTraits.contains(traitType);
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Adds a specific defense trait to the creature.<br>
	 * This method updates the total value for the given {@code TraitType}.<br>
	 * It also ensures the trait is added to the set of active traits.
	 * @param traitType The type of defense trait to modify.
	 * @param value The amount to add to the current trait value.
	 */
	public void mergeDefenceTrait(TraitType traitType, float value)
	{
		_lock.readLock().lock();
		try
		{
			_defenceTraitValues[traitType.ordinal()] += value;
			_defenceTraits.add(traitType);
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Removes a specific amount from a defense trait.<br>
	 * If the resulting value reaches {@code 0}, the trait is removed from the set.
	 * @param traitType The type of the defense trait to modify.
	 * @param value The amount to subtract from the current trait value.
	 */
	public void removeDefenceTrait(TraitType traitType, float value)
	{
		_lock.readLock().lock();
		try
		{
			final int traitOrdinal = traitType.ordinal();
			_defenceTraitValues[traitOrdinal] -= value;
			if (_defenceTraitValues[traitOrdinal] == 0)
			{
				_defenceTraits.remove(traitType);
			}
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Retrieves the defense value for a specific trait type.<br>
	 * This method returns the numerical value associated with the provided {@code TraitType}.
	 * @param traitType The type of defense trait to query.
	 * @return The float value of the requested defense trait.
	 */
	public float getDefenceTrait(TraitType traitType)
	{
		_lock.readLock().lock();
		try
		{
			return _defenceTraitValues[traitType.ordinal()];
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Checks if the actor possesses a specific defense trait.<br>
	 * This method returns {@code true} if the trait is present.
	 * @param traitType The type of defense trait to check.
	 * @return {@code true} if the trait exists, otherwise {@code false}.
	 */
	public boolean hasDefenceTrait(TraitType traitType)
	{
		_lock.readLock().lock();
		try
		{
			return _defenceTraits.contains(traitType);
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Adds a specific trait to the set of invulnerable traits.<br>
	 * This method updates the internal collection of {@code TraitType}.
	 * @param traitType The type of trait to be merged into the invulnerability list.
	 */
	public void mergeInvulnerableTrait(TraitType traitType)
	{
		_lock.readLock().lock();
		try
		{
			_invulnerableTraits.add(traitType);
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Removes a specific invulnerability trait from the actor.<br>
	 * This method updates the internal set of active traits.
	 * @param traitType The {@code TraitType} to be removed.
	 */
	public void removeInvulnerableTrait(TraitType traitType)
	{
		_lock.readLock().lock();
		try
		{
			_invulnerableTraits.remove(traitType);
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Checks if a specific trait type provides invulnerability.<br>
	 * This method returns {@code true} if the given {@code traitType} is in the invulnerable traits set.
	 * @param traitType The type of trait to check.
	 * @return {@code true} if the trait makes the actor invulnerable, otherwise {@code false}.
	 */
	public boolean isInvulnerableTrait(TraitType traitType)
	{
		_lock.readLock().lock();
		try
		{
			return _invulnerableTraits.contains(traitType);
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Retrieves the maximum number of buffs allowed.<br>
	 * This value limits how many active effects a creature can have.
	 * @return the maximum buff count as an {@code int}.
	 */
	public int getMaxBuffCount()
	{
		return _maxBuffCount;
	}
	
	/**
	 * Sets the maximum number of active buffs allowed.<br>
	 * This updates the {@code _maxBuffCount} field.
	 * @param value The new maximum buff count.
	 */
	public void setMaxBuffCount(int value)
	{
		_maxBuffCount = value;
	}
	
	/**
	 * Adds a specific value to an existing {@code Stat}.<br>
	 * This method updates the internal collection of stats.<br>
	 * It uses the addition function defined within the {@code Stat} object.
	 * @param stat The {@code Stat} object to be updated.
	 * @param value The numeric amount to add to the statistic.
	 */
	public void mergeAdd(Stat stat, Double value)
	{
		_statsAdd.merge(stat, value, stat::functionAdd);
	}
	
	/**
	 * Multiplies a specific statistic by a given value.<br>
	 * This method updates the multiplier for the provided {@code Stat}.<br>
	 * It uses the multiplication function defined within the {@code Stat} object.
	 * @param stat The {@code Stat} to be modified.
	 * @param value The {@code Double} value to multiply by.
	 */
	public void mergeMul(Stat stat, Double value)
	{
		_statsMul.merge(stat, value, stat::functionMul);
	}
	
	/**
	 * Retrieves the additive value for a specific statistic.<br>
	 * This method calculates the total bonus without applying any multipliers.
	 * @param stat The {@code Stat} type to retrieve.
	 * @return The total additive value as a {@code double}.
	 */
	public double getAdd(Stat stat)
	{
		return getAdd(stat, 0d);
	}
	
	/**
	 * Retrieves the added value for a specific {@code Stat}.<br>
	 * If no value is found, it returns the provided default.
	 * @param stat The {@code Stat} type to look up.
	 * @param defaultValue The value to return if the stat is not present.
	 * @return The double value of the added stat or the default value.
	 */
	public double getAdd(Stat stat, double defaultValue)
	{
		_lock.readLock().lock();
		try
		{
			final Double val = _statsAdd.get(stat);
			return val != null ? val.doubleValue() : defaultValue;
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Retrieves the additional value for a specific {@code Stat}.<br>
	 * This method calculates the bonus amount based on the provided {@code Stat} object.
	 * @param stat The {@code Stat} to retrieve the value from.
	 * @return The calculated double value of the statistic.
	 */
	public double getAddValue(Stat stat)
	{
		return getAddValue(stat, 0d);
	}
	
	/**
	 * Retrieves the additional value for a specific {@code Stat}.<br>
	 * If no value is found, it returns the provided default.
	 * @param stat The {@code Stat} type to look up.
	 * @param defaultValue The value to return if the {@code Stat} is not present.
	 * @return The additional value for the {@code Stat} or the {@code defaultValue}.
	 */
	public double getAddValue(Stat stat, double defaultValue)
	{
		final Double val = _statsAdd.get(stat);
		return val != null ? val.doubleValue() : defaultValue;
	}
	
	/**
	 * Calculates the multiplier for a specific statistic.<br>
	 * This method retrieves the value of the {@code stat} modified by any active effects.
	 * @param stat The {@code Stat} object to calculate the multiplier for.
	 * @return The resulting multiplier as a {@code double}.
	 */
	public double getMul(Stat stat)
	{
		return getMul(stat, 1d);
	}
	
	/**
	 * Retrieves the multiplier for a specific {@code Stat}.<br>
	 * If no multiplier is found, it returns the provided default value.
	 * @param stat The {@code Stat} type to look up.
	 * @param defaultValue The value to return if the stat is not mapped.
	 * @return The multiplier as a {@code double}.
	 */
	public double getMul(Stat stat, double defaultValue)
	{
		_lock.readLock().lock();
		try
		{
			final Double val = _statsMul.get(stat);
			return val != null ? val.doubleValue() : defaultValue;
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Retrieves the multiplier value for a specific {@code Stat}.<br>
	 * This method calculates the final multiplier based on the provided stat.
	 * @param stat The {@code Stat} object to evaluate.
	 * @return The calculated multiplier as a {@code double}.
	 */
	public double getMulValue(Stat stat)
	{
		return getMulValue(stat, 1d);
	}
	
	/**
	 * Retrieves the multiplier value for a specific {@code Stat}.<br>
	 * If no multiplier is found, it returns the provided default value.
	 * @param stat The {@code Stat} type to look up.
	 * @param defaultValue The value to return if the multiplier is missing.
	 * @return The multiplier as a {@code double}.
	 */
	public double getMulValue(Stat stat, double defaultValue)
	{
		final Double val = _statsMul.get(stat);
		return val != null ? val.doubleValue() : defaultValue;
	}
	
	/**
	 * Calculates the final value for a specific {@code Stat}.<br>
	 * It first checks if there is a fixed value assigned to the stat.<br>
	 * If no fixed value exists, it uses the stat's own calculation logic.
	 * @param stat The {@code Stat} type to calculate.
	 * @param baseValue The initial value used for calculations.
	 * @return The final calculated double value.
	 */
	public double getValue(Stat stat, double baseValue)
	{
		final Double val = _fixedValue.get(stat);
		return val != null ? val.doubleValue() : stat.finalize(_creature, OptionalDouble.of(baseValue));
	}
	
	/**
	 * Retrieves the calculated value for a specific {@code Stat}.<br>
	 * It first checks if a fixed value exists in the internal cache.<br>
	 * If no fixed value is found, it calculates the value using the stat's finalization logic.
	 * @param stat The {@code Stat} to retrieve the value for.
	 * @return The calculated double value of the requested {@code Stat}.
	 */
	public double getValue(Stat stat)
	{
		final Double val = _fixedValue.get(stat);
		return val != null ? val.doubleValue() : stat.finalize(_creature, OptionalDouble.empty());
	}
	
	/**
	 * Resets all statistics to their default values.<br>
	 * This method clears the current additions and multipliers.<br>
	 * It then populates them with the base reset values defined in {@code Stat}.
	 */
	protected void resetStats()
	{
		_statsAdd.clear();
		_statsMul.clear();
		_vampiricSum = 0;
		_mpVampiricSum = 0;
		_weaponPAtkLevelBonus = 0;
		_weaponMAtkLevelBonus = 0;
		
		// Initialize default values
		for (Stat stat : Stat.values())
		{
			final Double resetAddValue = stat.getResetAddValue();
			if (resetAddValue != 0)
			{
				_statsAdd.put(stat, resetAddValue);
			}
			
			final Double resetMulValue = stat.getResetMulValue();
			if (resetMulValue != 0)
			{
				_statsMul.put(stat, resetMulValue);
			}
		}
	}
	
	/**
	 * Adds a bonus to the weapon physical attack level.<br>
	 * This method updates the {@code _weaponPAtkLevelBonus} field.
	 * @param value The amount to add to the current bonus.
	 */
	public void addWeaponPAtkLevelBonus(double value)
	{
		_weaponPAtkLevelBonus += value;
	}
	
	/**
	 * Adds a bonus to the weapon magical attack level.<br>
	 * This updates the {@code _weaponMAtkLevelBonus} field.
	 * @param value The amount to add to the bonus.
	 */
	public void addWeaponMAtkLevelBonus(double value)
	{
		_weaponMAtkLevelBonus += value;
	}
	
	/**
	 * Retrieves the physical attack bonus based on weapon level.<br>
	 * This value is cast to an {@code int}.
	 * @return The weapon physical attack level bonus.
	 */
	public int getWeaponPAtkLevelBonus()
	{
		return (int) _weaponPAtkLevelBonus;
	}
	
	/**
	 * Retrieves the magical attack bonus based on weapon level.<br>
	 * This value is cast to an {@code int}.
	 * @return the weapon magical attack level bonus.
	 */
	public int getWeaponMAtkLevelBonus()
	{
		return (int) _weaponMAtkLevelBonus;
	}
	
	/**
	 * This method schedules a task to update the creature's statistics.<br>
	 * It processes all active effects and recalculates multipliers.<br>
	 * If {@code broadcast} is {@code true}, it notifies other clients of the changes.
	 * @param broadcast Determines if the updated stats should be sent to other players.
	 */
	public void recalculateStats(boolean broadcast)
	{
		if (broadcast)
		{
			synchronized (_broadcast)
			{
				_broadcast.compareAndSet(false, true);
			}
		}
		
		synchronized (this)
		{
			if (_recalculateStatsTask != null)
			{
				return;
			}
			
			_recalculateStatsTask = ThreadPool.schedule(() ->
			{
				pumpStats();
				_recalculateStatsTask = null;
			}, 50);
		}
	}
	
	/**
	 * Runs a stat recalculation that is still waiting on its task, for callers that read the final values right away.
	 */
	public void runPendingRecalculation()
	{
		synchronized (this)
		{
			if (_recalculateStatsTask == null)
			{
				return;
			}
			
			_recalculateStatsTask.cancel(false);
			_recalculateStatsTask = null;
		}
		
		pumpStats();
	}
	
	/**
	 * Wipes the stats and pumps every passive, option and active effect back into them, then hands the final values to {@code onRecalculateStats}.
	 */
	private void pumpStats()
	{
		synchronized (_broadcast)
		{
			final boolean broadcastChanges = _broadcast.get();
			_broadcast.compareAndSet(true, false);
			
			_lock.writeLock().lock();
			
			// Copy old data before wiping it out.
			final Map<Stat, Double> adds = !broadcastChanges ? Collections.emptyMap() : new EnumMap<>(_statsAdd);
			final Map<Stat, Double> muls = !broadcastChanges ? Collections.emptyMap() : new EnumMap<>(_statsMul);
			
			final Map<ArmorMagicalDefenceType, Integer> previousMagicalDefenceTypes = new EnumMap<>(_magicalDefenceTypes);
			final Map<ArmorPhysicalDefenceType, Integer> previousPhysicalDefenceTypes = new EnumMap<>(_physicalDefenceTypes);
			_magicalDefenceTypes.clear();
			_physicalDefenceTypes.clear();
			
			try
			{
				// Wipe all the data.
				resetStats();
				
				// Delayed pump effects.
				final List<DelayedPumpHolder> delayedPumps = new ArrayList<>();
				
				// Call pump to each effect.
				for (BuffInfo info : _creature.getEffectList().getPassives())
				{
					if (info.isInUse() && info.getSkill().checkConditions(SkillConditionScope.PASSIVE, _creature, _creature.getTarget()))
					{
						for (AbstractEffect effect : info.getEffects())
						{
							if (effect.canStart(info.getEffector(), info.getEffected(), info.getSkill()) && effect.canPump(info.getEffector(), info.getEffected(), info.getSkill()))
							{
								if (effect.delayPump())
								{
									delayedPumps.add(new DelayedPumpHolder(effect, info.getEffected(), info.getSkill()));
								}
								else
								{
									effect.pump(info.getEffected(), info.getSkill());
								}
							}
						}
					}
				}
				
				for (BuffInfo info : _creature.getEffectList().getOptions())
				{
					if (info.isInUse())
					{
						for (AbstractEffect effect : info.getEffects())
						{
							if (effect.canStart(info.getEffector(), info.getEffected(), info.getSkill()) && effect.canPump(info.getEffector(), info.getEffected(), info.getSkill()))
							{
								if (effect.delayPump())
								{
									delayedPumps.add(new DelayedPumpHolder(effect, info.getEffected(), info.getSkill()));
								}
								else
								{
									effect.pump(info.getEffected(), info.getSkill());
								}
							}
						}
					}
				}
				
				for (BuffInfo info : _creature.getEffectList().getEffects())
				{
					if (info.isInUse())
					{
						for (AbstractEffect effect : info.getEffects())
						{
							if (effect.canStart(info.getEffector(), info.getEffected(), info.getSkill()) && effect.canPump(info.getEffector(), info.getEffected(), info.getSkill()))
							{
								if (effect.delayPump())
								{
									delayedPumps.add(new DelayedPumpHolder(effect, info.getEffected(), info.getSkill()));
								}
								else
								{
									effect.pump(info.getEffected(), info.getSkill());
								}
							}
						}
					}
				}
				
				// Call delayed effect pumps.
				if (!delayedPumps.isEmpty())
				{
					for (DelayedPumpHolder holder : delayedPumps)
					{
						holder.getEffect().pump(holder.getEffected(), holder.getSkill());
					}
				}
				
				// Pump for summon ABILITY_CHANGE abnormal type.
				if (_creature.isSummon())
				{
					final Player player = _creature.asPlayer();
					if ((player != null) && player.hasAbnormalType(AbnormalType.ABILITY_CHANGE))
					{
						for (BuffInfo info : player.getEffectList().getEffects())
						{
							if (info.isInUse() && info.isAbnormalType(AbnormalType.ABILITY_CHANGE))
							{
								for (AbstractEffect effect : info.getEffects())
								{
									if (effect.canStart(info.getEffector(), info.getEffected(), info.getSkill()) && effect.canPump(_creature, _creature, info.getSkill()))
									{
										effect.pump(_creature, info.getSkill());
									}
								}
							}
						}
					}
				}
				
				_attackSpeedMultiplier = Formulas.calcAtkSpdMultiplier(_creature);
				_mAttackSpeedMultiplier = Formulas.calcMAtkSpdMultiplier(_creature);
			}
			finally
			{
				_lock.writeLock().unlock();
			}
			
			// Notify recalculation to child classes.
			onRecalculateStats(broadcastChanges);
			
			if (broadcastChanges)
			{
				// Calculate the difference between old and new stats.
				final Set<Stat> changed = EnumSet.noneOf(Stat.class);
				Double statAddResetValue;
				Double statMulResetValue;
				Double statAddValue;
				Double statMulValue;
				Double addsValue;
				Double mulsValue;
				for (Stat stat : Stat.values())
				{
					statAddResetValue = stat.getResetAddValue();
					statMulResetValue = stat.getResetMulValue();
					addsValue = adds.getOrDefault(stat, statAddResetValue);
					mulsValue = muls.getOrDefault(stat, statMulResetValue);
					statAddValue = _statsAdd.getOrDefault(stat, statAddResetValue);
					statMulValue = _statsMul.getOrDefault(stat, statMulResetValue);
					if (addsValue.equals(statAddResetValue) || mulsValue.equals(statMulResetValue) || !addsValue.equals(statAddValue) || !mulsValue.equals(statMulValue))
					{
						changed.add(stat);
					}
				}
				
				_creature.broadcastModifiedStats(changed);
				
				if (_creature.isPlayer())
				{
					final Player creaturePlayer = _creature.asPlayer();
					if (!previousPhysicalDefenceTypes.equals(_physicalDefenceTypes))
					{
						_creature.sendPacket(new ExUserInfoAddPDefend(creaturePlayer));
					}
					
					if (!previousMagicalDefenceTypes.equals(_magicalDefenceTypes))
					{
						_creature.sendPacket(new ExUserInfoAddMDefend(creaturePlayer));
					}
				}
			}
		}
	}
	
	/**
	 * Recalculates the creature statistics.<br>
	 * This method ensures that current CP, HP, and MP do not exceed their maximum values.
	 * @param broadcast Whether to send a network packet to notify other clients of the change.
	 */
	protected void onRecalculateStats(boolean broadcast)
	{
		// Check if Max HP/MP/CP is lower than current due to new stats.
		final int maxCp = getMaxCp();
		if (_creature.getCurrentCp() > maxCp)
		{
			_creature.setCurrentCp(maxCp);
		}
		
		final long maxHp = getMaxHp();
		if (_creature.getCurrentHp() > maxHp)
		{
			_creature.setCurrentHp(maxHp);
		}
		
		final int maxMp = getMaxMp();
		if (_creature.getCurrentMp() > maxMp)
		{
			_creature.setCurrentMp(maxMp);
		}
	}
	
	/**
	 * Retrieves the value of a specific {@code Stat} based on a {@code Position}.<br>
	 * This method checks if there is a custom multiplier for the given position.<br>
	 * If no specific value is found, it returns {@code 1d}.
	 * @param stat The {@code Stat} type to look up.
	 * @param position The {@code Position} used to determine the modifier.
	 * @return The double value associated with the position or {@code 1d} if none exists.
	 */
	public double getPositionTypeValue(Stat stat, Position position)
	{
		final Map<Position, Double> map = _positionStats.get(stat);
		if (map != null)
		{
			final Double val = map.get(position);
			if (val != null)
			{
				return val.doubleValue();
			}
		}
		
		return 1d;
	}
	
	/**
	 * Merges a value into the specific position for a given stat.<br>
	 * This method updates the internal map of stats associated with positions.<br>
	 * If the value already exists for that position, it applies the provided function to combine them.
	 * @param stat The {@code Stat} type to update.
	 * @param position The {@code Position} where the value is applied.
	 * @param value The new numeric value to merge.
	 * @param func The {@code BiFunction} used to combine existing and new values.
	 */
	public void mergePositionTypeValue(Stat stat, Position position, double value, BiFunction<? super Double, ? super Double, ? extends Double> func)
	{
		_positionStats.computeIfAbsent(stat, _ -> new ConcurrentHashMap<>()).merge(position, value, func);
	}
	
	/**
	 * Retrieves the specific value for a move type associated with a given stat.<br>
	 * This method checks if a mapping exists for the provided {@code Stat}.<br>
	 * It returns the corresponding double value or 0d if no value is found.
	 * @param stat The {@code Stat} to check for values.
	 * @param type The {@code MoveType} to retrieve the value for.
	 * @return The numeric value of the move type, or 0d if it does not exist.
	 */
	public double getMoveTypeValue(Stat stat, MoveType type)
	{
		final Map<MoveType, Double> map = _moveTypeStats.get(stat);
		if (map != null)
		{
			final Double val = map.get(type);
			if (val != null)
			{
				return val.doubleValue();
			}
		}
		
		return 0d;
	}
	
	/**
	 * Adds a specific value to a {@code MoveType} for a given {@code Stat}.<br>
	 * This method updates the existing total if the type already exists.<br>
	 * It uses addition to merge the new {@code value} into the current amount.
	 * @param stat The {@code Stat} object to modify.
	 * @param type The {@code MoveType} category for the modification.
	 * @param value The numeric amount to add to the stat.
	 */
	public void mergeMoveTypeValue(Stat stat, MoveType type, double value)
	{
		_moveTypeStats.computeIfAbsent(stat, _ -> new ConcurrentHashMap<>()).merge(type, value, MathUtil::add);
	}
	
	/**
	 * Retrieves the reuse value for a specific magic type.<br>
	 * It returns the stored value if it exists.<br>
	 * If no value is found, it returns {@code 1.0}.
	 * @param magicType The identifier for the magic type to check.
	 * @return The reuse value as a {@code double}.
	 */
	public double getReuseTypeValue(int magicType)
	{
		final Double val = _reuseStat.get(magicType);
		return val != null ? val.doubleValue() : 1d;
	}
	
	/**
	 * Merges a new value into the reuse statistics for a specific magic type.<br>
	 * This method uses the provided {@code BiFunction} to determine how to combine the values.
	 * @param magicType The identifier for the magic type being updated.
	 * @param value The numeric value to be merged.
	 * @param func The function used to merge the existing value with the new value.
	 */
	public void mergeReuseTypeValue(int magicType, double value, BiFunction<? super Double, ? super Double, ? extends Double> func)
	{
		_reuseStat.merge(magicType, value, func);
	}
	
	/**
	 * Changes a specific skill's base reuse time, keeping the lowest value if more than one source targets the same skill.
	 * @param skillId The identifier of the skill whose base reuse time should be changed.
	 * @param reuseTime The new base reuse time in milliseconds.
	 */
	public void addChangedSkillBaseReuseTime(int skillId, int reuseTime)
	{
		_changedSkillBaseReuseTime.merge(skillId, reuseTime, Math::min);
	}
	
	/**
	 * Restores the base reuse time of a specific skill.
	 * @param skillId The identifier of the skill to restore.
	 */
	public void removeChangedSkillBaseReuseTime(int skillId)
	{
		_changedSkillBaseReuseTime.remove(skillId);
	}
	
	/**
	 * Retrieves the MP consumption value for a specific magic type.<br>
	 * It returns the configured value from the internal map.<br>
	 * If no specific value is found, it defaults to {@code 1.0}.
	 * @param magicType The identifier of the magic type to check.
	 * @return The MP consumption multiplier as a {@code double}.
	 */
	public double getMpConsumeTypeValue(int magicType)
	{
		final Double val = _mpConsumeStat.get(magicType);
		return val != null ? val.doubleValue() : 1d;
	}
	
	/**
	 * Merges a specific magic type consumption value into the existing stats.<br>
	 * This method uses a provided function to combine the new value with the current one.
	 * @param magicType The identifier for the type of magic being processed.
	 * @param value The numerical value to be merged.
	 * @param func The logic used to merge two {@code Double} values into a single result.
	 */
	public void mergeMpConsumeTypeValue(int magicType, double value, BiFunction<? super Double, ? super Double, ? extends Double> func)
	{
		_mpConsumeStat.merge(magicType, value, func);
	}
	
	/**
	 * Retrieves the evasion value for a specific magic type.<br>
	 * This method checks if any evasion values exist for the provided {@code magicType}.<br>
	 * It returns the most recent value from the stack or {@code 0d} if none are found.
	 * @param magicType The type of magic to check for evasion.
	 * @return The last recorded skill evasion value as a {@code double}.
	 */
	public double getSkillEvasionTypeValue(int magicType)
	{
		final Deque<Double> skillEvasions = _skillEvasionStat.get(magicType);
		if ((skillEvasions != null) && !skillEvasions.isEmpty())
		{
			return skillEvasions.peekLast().doubleValue();
		}
		
		return 0d;
	}
	
	/**
	 * Adds a specific evasion value for a given magic type.<br>
	 * This method stores the value in a list associated with the {@code magicType}.
	 * @param magicType The type of magic to apply the evasion to.
	 * @param value The numerical amount of evasion to add.
	 */
	public void addSkillEvasionTypeValue(int magicType, double value)
	{
		_skillEvasionStat.computeIfAbsent(magicType, _ -> new ArrayDeque<>()).add(value);
	}
	
	/**
	 * Removes a specific value from the skill evasion stats for a given magic type.<br>
	 * This method updates the internal collection of values associated with {@code magicType}.<br>
	 * If no values remain after removal, the entry is removed from the map.
	 * @param magicType The type of magic to modify.
	 * @param value The specific numerical value to remove from the list.
	 */
	public void removeSkillEvasionTypeValue(int magicType, double value)
	{
		_skillEvasionStat.computeIfPresent(magicType, (_, v) ->
		{
			v.remove(value);
			return !v.isEmpty() ? v : null;
		});
	}
	
	/**
	 * Adds a value to the total vampiric sum.<br>
	 * This method updates the internal {@code _vampiricSum} field.
	 * @param sum The amount to add to the current sum.
	 */
	public void addToVampiricSum(double sum)
	{
		_vampiricSum += sum;
	}
	
	/**
	 * Calculates the total sum of vampiric effects.<br>
	 * This value represents the combined multiplier for life steal.
	 * @return The total vampiric sum as a {@code double}.
	 */
	public double getVampiricSum()
	{
		_lock.readLock().lock();
		try
		{
			return _vampiricSum;
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Adds a value to the total MP vampire sum.<br>
	 * This method updates the internal {@code _mpVampiricSum} field.
	 * @param sum The amount to add to the current sum.
	 */
	public void addToMpVampiricSum(double sum)
	{
		_mpVampiricSum += sum;
	}
	
	/**
	 * Retrieves the total sum of MP vampiric effects.<br>
	 * This value represents the combined multiplier for mana recovery from attacks.
	 * @return The current {@code double} sum of all MP vampiric values.
	 */
	public double getMpVampiricSum()
	{
		_lock.readLock().lock();
		try
		{
			return _mpVampiricSum;
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Calculates the reuse time for a specific {@code Skill}.<br>
	 * It checks if the skill has static properties.<br>
	 * If not, it scales the delay based on the magic type.
	 * @param skill The {@code Skill} to check.
	 * @return The calculated reuse time as an {@code int}.
	 */
	public int getReuseTime(Skill skill)
	{
		int reuseDelay = skill.getReuseDelay();
		final Integer changedReuseDelay = _changedSkillBaseReuseTime.get(skill.getId());
		if (changedReuseDelay != null)
		{
			reuseDelay = Math.min(reuseDelay, changedReuseDelay.intValue());
		}
		
		return (skill.isStaticReuse() || skill.isStatic()) ? reuseDelay : (int) (reuseDelay * getReuseTypeValue(skill.getMagicType()));
	}
	
	/**
	 * Adds a fixed value to a specific {@code Stat}.<br>
	 * This method updates the internal mapping of stats to their values.
	 * @param stat The {@code Stat} type to modify.
	 * @param value The numeric {@code Double} value to assign.
	 * @return {@code true} if the value was successfully added, or {@code false} if it already existed.
	 */
	public boolean addFixedValue(Stat stat, Double value)
	{
		return _fixedValue.put(stat, value) == null;
	}
	
	/**
	 * Removes a specific {@code Stat} from the fixed values collection.<br>
	 * Returns {@code true} if the value was successfully removed.<br>
	 * Returns {@code false} if the value was not found or already removed.
	 * @param stat The {@code Stat} object to be removed.
	 * @return {@code true} if the removal was successful, otherwise {@code false}.
	 */
	public boolean removeFixedValue(Stat stat)
	{
		return _fixedValue.remove(stat) != null;
	}
	
	/**
	 * Adds a physical defense bonus to the actor.<br>
	 * This method updates the value for a specific {@code ArmorPhysicalDefenceType}.<br>
	 * If the type already exists, the new {@code amount} is added to it.
	 * @param defenceType The type of physical defense to modify.
	 * @param amount The numerical value to add to the defense bonus.
	 */
	public void addPhysicalDefenceBonus(ArmorPhysicalDefenceType defenceType, int amount)
	{
		_physicalDefenceTypes.merge(defenceType, amount, Integer::sum);
	}
	
	/**
	 * Adds a magical defense bonus to the specified type.<br>
	 * This method updates the internal map of defenses.<br>
	 * It adds the provided {@code amount} to the existing value for the given {@code defenceType}.
	 * @param defenceType The category of magical defense to modify.
	 * @param amount The numerical value to add to the bonus.
	 */
	public void addMagicalDefenceBonus(ArmorMagicalDefenceType defenceType, int amount)
	{
		_magicalDefenceTypes.merge(defenceType, amount, Integer::sum);
	}
	
	/**
	 * Retrieves the physical defense types for this creature.<br>
	 * It returns a map where each {@code ArmorPhysicalDefenceType} is associated with its value.
	 * @return A {@code Map} containing the physical defense types and their corresponding values.
	 */
	public Map<ArmorPhysicalDefenceType, Integer> getPhysicalDefenceTypes()
	{
		return _physicalDefenceTypes;
	}
	
	/**
	 * Retrieves the magical defense values for different types.<br>
	 * This method returns a map where each {@code ArmorMagicalDefenceType} is linked to its corresponding integer value.
	 * @return A {@code Map} containing the magical defense types and their values.
	 */
	public Map<ArmorMagicalDefenceType, Integer> getMagicalDefenceTypes()
	{
		return _magicalDefenceTypes;
	}
}
