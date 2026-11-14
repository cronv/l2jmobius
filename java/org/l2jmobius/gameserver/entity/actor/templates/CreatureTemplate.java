/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.entity.actor.templates;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class serves as a data template for creatures in the game.<br>
 * It defines the base attributes and properties used to initialize {@code Creature} instances.
 * @author Zoey76
 */
public class CreatureTemplate extends ListenersContainer
{
	// BaseStats
	private WeaponType _baseAttackType;
	
	/** For client info use {@link #_fCollisionRadius} */
	private int _collisionRadius;
	/** For client info use {@link #_fCollisionHeight} */
	private int _collisionHeight;
	
	private float _fCollisionRadius;
	private float _fCollisionHeight;
	
	protected final Map<Stat, Double> _baseValues = new EnumMap<>(Stat.class);
	
	/** The creature's race. */
	private Race _race;
	
	/**
	 * Creates a new {@code CreatureTemplate} using the provided statistics.<br>
	 * This constructor initializes the template by applying all values from the {@code StatSet}.
	 * @param set The {@code StatSet} containing the base stats for this creature.
	 */
	public CreatureTemplate(StatSet set)
	{
		set(set);
	}
	
	/**
	 * Populates the base statistics of this template from a {@code StatSet}.<br>
	 * This method maps values like strength, defense, and speeds to internal variables.<br>
	 * It also initializes collision geometry and attack types.
	 * @param set The {@code StatSet} containing the data to apply.
	 */
	public void set(StatSet set)
	{
		// Base stats
		_baseValues.put(Stat.STAT_STR, set.getDouble("baseSTR", 0));
		_baseValues.put(Stat.STAT_CON, set.getDouble("baseCON", 0));
		_baseValues.put(Stat.STAT_DEX, set.getDouble("baseDEX", 0));
		_baseValues.put(Stat.STAT_INT, set.getDouble("baseINT", 0));
		_baseValues.put(Stat.STAT_WIT, set.getDouble("baseWIT", 0));
		_baseValues.put(Stat.STAT_MEN, set.getDouble("baseMEN", 0));
		_baseValues.put(Stat.STAT_LUC, set.getDouble("baseLUC", 0));
		_baseValues.put(Stat.STAT_CHA, set.getDouble("baseCHA", 0));
		
		// Max HP/MP/CP
		_baseValues.put(Stat.MAX_HP, set.getDouble("baseHpMax", 0));
		_baseValues.put(Stat.MAX_MP, set.getDouble("baseMpMax", 0));
		_baseValues.put(Stat.MAX_CP, set.getDouble("baseCpMax", 0));
		
		// Regenerate HP/MP/CP
		_baseValues.put(Stat.REGENERATE_HP_RATE, set.getDouble("baseHpReg", 0));
		_baseValues.put(Stat.REGENERATE_MP_RATE, set.getDouble("baseMpReg", 0));
		_baseValues.put(Stat.REGENERATE_CP_RATE, set.getDouble("baseCpReg", 0));
		
		// Attack and Defense
		_baseValues.put(Stat.PHYSICAL_ATTACK, set.getDouble("basePAtk", 0));
		_baseValues.put(Stat.MAGIC_ATTACK, set.getDouble("baseMAtk", 0));
		_baseValues.put(Stat.PHYSICAL_DEFENCE, set.getDouble("basePDef", 0));
		_baseValues.put(Stat.MAGICAL_DEFENCE, set.getDouble("baseMDef", 0));
		
		// Attack speed
		_baseValues.put(Stat.PHYSICAL_ATTACK_SPEED, set.getDouble("basePAtkSpd", 300));
		_baseValues.put(Stat.MAGIC_ATTACK_SPEED, set.getDouble("baseMAtkSpd", 333));
		
		// Misc
		_baseValues.put(Stat.SHIELD_DEFENCE, set.getDouble("baseShldDef", 0));
		_baseValues.put(Stat.PHYSICAL_ATTACK_RANGE, set.getDouble("baseAtkRange", 40));
		_baseValues.put(Stat.RANDOM_DAMAGE, set.getDouble("baseRndDam", 0));
		
		// Shield and critical rates
		_baseValues.put(Stat.SHIELD_DEFENCE_RATE, set.getDouble("baseShldRate", 0));
		_baseValues.put(Stat.CRITICAL_RATE, set.getDouble("baseCritRate", 4));
		_baseValues.put(Stat.MAGIC_CRITICAL_RATE, set.getDouble("baseMCritRate", 5));
		_baseValues.put(Stat.ACCURACY_COMBAT, set.getDouble("accuracy", 0));
		_baseValues.put(Stat.EVASION_RATE, set.getDouble("evasion", 0));
		_baseValues.put(Stat.CRITICAL_RATE_SKILL, set.getDouble("basePSkillCritRate", 5));
		
		// Breath under water
		_baseValues.put(Stat.BREATH, set.getDouble("baseBreath", 100));
		
		// Elemental attributes attack.
		_baseValues.put(Stat.FIRE_POWER, set.getDouble("baseFire", 0));
		_baseValues.put(Stat.WIND_POWER, set.getDouble("baseWind", 0));
		_baseValues.put(Stat.WATER_POWER, set.getDouble("baseWater", 0));
		_baseValues.put(Stat.EARTH_POWER, set.getDouble("baseEarth", 0));
		_baseValues.put(Stat.HOLY_POWER, set.getDouble("baseHoly", 0));
		_baseValues.put(Stat.DARK_POWER, set.getDouble("baseDark", 0));
		
		// Defense
		_baseValues.put(Stat.FIRE_RES, set.getDouble("baseFireRes", 0));
		_baseValues.put(Stat.WIND_RES, set.getDouble("baseWindRes", 0));
		_baseValues.put(Stat.WATER_RES, set.getDouble("baseWaterRes", 0));
		_baseValues.put(Stat.EARTH_RES, set.getDouble("baseEarthRes", 0));
		_baseValues.put(Stat.HOLY_RES, set.getDouble("baseHolyRes", 0));
		_baseValues.put(Stat.DARK_RES, set.getDouble("baseDarkRes", 0));
		_baseValues.put(Stat.BASE_ATTRIBUTE_RES, set.getDouble("baseElementRes", 0));
		
		// Geometry
		_fCollisionHeight = set.getFloat("collision_height", 0);
		_fCollisionRadius = set.getFloat("collision_radius", 0);
		_collisionRadius = (int) _fCollisionRadius;
		_collisionHeight = (int) _fCollisionHeight;
		
		// Speed
		_baseValues.put(Stat.RUN_SPEED, set.getDouble("baseRunSpd", 120));
		_baseValues.put(Stat.WALK_SPEED, set.getDouble("baseWalkSpd", 50));
		
		// Swimming
		_baseValues.put(Stat.SWIM_RUN_SPEED, set.getDouble("baseSwimRunSpd", 120));
		_baseValues.put(Stat.SWIM_WALK_SPEED, set.getDouble("baseSwimWalkSpd", 50));
		
		// Flying
		_baseValues.put(Stat.FLY_RUN_SPEED, set.getDouble("baseFlyRunSpd", 120));
		_baseValues.put(Stat.FLY_WALK_SPEED, set.getDouble("baseFlyWalkSpd", 50));
		
		// Attack type
		_baseAttackType = set.getEnum("baseAtkType", WeaponType.class, WeaponType.FIST);
		
		// Basic property
		_baseValues.put(Stat.ABNORMAL_RESIST_PHYSICAL, set.getDouble("physicalAbnormalResist", 10));
		_baseValues.put(Stat.ABNORMAL_RESIST_MAGICAL, set.getDouble("magicAbnormalResist", 10));
	}
	
	/**
	 * Retrieves the base Strength value for this creature.<br>
	 * It looks up the {@code Stat.STAT_STR} in the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base Strength stat.
	 */
	public int getBaseSTR()
	{
		final Double val = _baseValues.get(Stat.STAT_STR);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base Constitution value for this creature.<br>
	 * This value is stored in the {@code _baseValues} map.<br>
	 * It returns 0 if no value is found.
	 * @return The integer value of the base Constitution stat.
	 */
	public int getBaseCON()
	{
		final Double val = _baseValues.get(Stat.STAT_CON);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base Dexterity value for this creature.<br>
	 * It looks up the {@code Stat.STAT_DEX} in the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base Dexterity stat.
	 */
	public int getBaseDEX()
	{
		final Double val = _baseValues.get(Stat.STAT_DEX);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base intelligence value for this creature.<br>
	 * It looks up the {@code Stat.STAT_INT} in the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base intelligence stat.
	 */
	public int getBaseINT()
	{
		final Double val = _baseValues.get(Stat.STAT_INT);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base Wisdom value for this creature.<br>
	 * It looks up the {@code Stat.STAT_WIT} in the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base Wisdom stat.
	 */
	public int getBaseWIT()
	{
		final Double val = _baseValues.get(Stat.STAT_WIT);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base Menas (MEN) stat for this creature.<br>
	 * It looks up the value in the {@code _baseValues} map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base MEN stat.
	 */
	public int getBaseMEN()
	{
		final Double val = _baseValues.get(Stat.STAT_MEN);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base Luck value for this creature.<br>
	 * It looks up the {@code Stat.STAT_LUC} in the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base Luck stat.
	 */
	public int getBaseLUC()
	{
		final Double val = _baseValues.get(Stat.STAT_LUC);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base Charisma value for this creature.<br>
	 * It looks up the value in the {@code _baseValues} map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base Charisma stat.
	 */
	public int getBaseCHA()
	{
		final Double val = _baseValues.get(Stat.STAT_CHA);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base maximum health of the creature.<br>
	 * This value is fetched from the {@code Stat.MAX_HP} entry in the internal stats map.<br>
	 * It returns {@code 0} if no value is defined.
	 * @return The base maximum health as a {@code float}.
	 */
	public float getBaseHpMax()
	{
		final Double val = _baseValues.get(Stat.MAX_HP);
		return val != null ? val.floatValue() : 0;
	}
	
	/**
	 * Retrieves the base maximum CP for this creature.<br>
	 * This value is fetched from the {@code Stat.MAX_CP} entry in the internal stats map.<br>
	 * It returns {@code 0} if no value is defined.
	 * @return The base maximum CP as a {@code float}.
	 */
	public float getBaseCpMax()
	{
		final Double val = _baseValues.get(Stat.MAX_CP);
		return val != null ? val.floatValue() : 0;
	}
	
	/**
	 * Retrieves the base maximum mana for this creature.<br>
	 * It looks up the {@code Stat.MAX_MP} value from the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The base maximum mana as a {@code float}.
	 */
	public float getBaseMpMax()
	{
		final Double val = _baseValues.get(Stat.MAX_MP);
		return val != null ? val.floatValue() : 0;
	}
	
	/**
	 * Retrieves the base health regeneration rate.<br>
	 * This value is fetched from the {@code Stat.REGENERATE_HP_RATE} entry.<br>
	 * It returns {@code 0} if no value is defined.
	 * @return The base health regeneration rate as a {@code float}.
	 */
	public float getBaseHpReg()
	{
		final Double val = _baseValues.get(Stat.REGENERATE_HP_RATE);
		return val != null ? val.floatValue() : 0;
	}
	
	/**
	 * Retrieves the base Mana Point regeneration rate.<br>
	 * This value is fetched from the {@code Stat.REGENERATE_MP_RATE} entry.<br>
	 * It returns {@code 0} if no value is defined.
	 * @return The base MP regeneration rate as a {@code float}.
	 */
	public float getBaseMpReg()
	{
		final Double val = _baseValues.get(Stat.REGENERATE_MP_RATE);
		return val != null ? val.floatValue() : 0;
	}
	
	/**
	 * Retrieves the base fire power of the creature.<br>
	 * This value is fetched from the {@code _baseValues} map using the {@code Stat.FIRE_POWER} key.<br>
	 * If no value exists, it returns {@code 0}.
	 * @return The integer value of the base fire power.
	 */
	public int getBaseFire()
	{
		final Double val = _baseValues.get(Stat.FIRE_POWER);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base wind power of the creature.<br>
	 * This value is taken from the {@code Stat.WIND_POWER} entry in the internal stats map.<br>
	 * It returns {@code 0} if no value is defined.
	 * @return The integer value of the base wind power.
	 */
	public int getBaseWind()
	{
		final Double val = _baseValues.get(Stat.WIND_POWER);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base water power of the creature.<br>
	 * This value is taken from the {@code Stat.WATER_POWER} entry in the internal stats map.<br>
	 * If no value exists, it returns 0.
	 * @return The integer value of the base water power.
	 */
	public int getBaseWater()
	{
		final Double val = _baseValues.get(Stat.WATER_POWER);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base Earth power value for this creature.<br>
	 * It looks up the {@code Stat.EARTH_POWER} in the internal stats map.<br>
	 * If no value is found, it returns 0.
	 * @return The integer value of the base Earth power.
	 */
	public int getBaseEarth()
	{
		final Double val = _baseValues.get(Stat.EARTH_POWER);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base holy power value for this creature.<br>
	 * It looks up the {@code Stat.HOLY_POWER} in the internal stats map.<br>
	 * If no value is found, it returns 0.
	 * @return The integer value of the base holy power.
	 */
	public int getBaseHoly()
	{
		final Double val = _baseValues.get(Stat.HOLY_POWER);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base dark power value for this creature.<br>
	 * It looks up the {@code Stat.DARK_POWER} in the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base dark power.
	 */
	public int getBaseDark()
	{
		final Double val = _baseValues.get(Stat.DARK_POWER);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base fire resistance value.<br>
	 * This value is stored in the {@code _baseValues} map.<br>
	 * It returns {@code 0} if no value is defined.
	 * @return The base fire resistance as a {@code double}.
	 */
	public double getBaseFireRes()
	{
		final Double val = _baseValues.get(Stat.FIRE_RES);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base wind resistance value for this creature.<br>
	 * It looks up the {@code Stat.WIND_RES} in the internal stats map.<br>
	 * Returns 0 if no value is found.
	 * @return The base wind resistance as a {@code double}.
	 */
	public double getBaseWindRes()
	{
		final Double val = _baseValues.get(Stat.WIND_RES);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base water resistance value for this creature.<br>
	 * It looks up the {@code Stat.WATER_RES} in the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The base water resistance as a {@code double}.
	 */
	public double getBaseWaterRes()
	{
		final Double val = _baseValues.get(Stat.WATER_RES);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base earth resistance value for this creature.<br>
	 * It looks up the {@code Stat.EARTH_RES} in the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The base earth resistance as a {@code double}.
	 */
	public double getBaseEarthRes()
	{
		final Double val = _baseValues.get(Stat.EARTH_RES);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base holy resistance value.<br>
	 * This value is fetched from the {@code Stat} map.<br>
	 * It returns {@code 0} if no value is found.
	 * @return The base holy resistance as a {@code double}.
	 */
	public double getBaseHolyRes()
	{
		final Double val = _baseValues.get(Stat.HOLY_RES);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base dark resistance value for this creature.<br>
	 * It looks up the {@code DARK_RES} stat in the internal map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The base dark resistance as a {@code double}.
	 */
	public double getBaseDarkRes()
	{
		final Double val = _baseValues.get(Stat.DARK_RES);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base element resistance value.<br>
	 * This method looks up the {@code Stat.BASE_ATTRIBUTE_RES} from the internal map.<br>
	 * It returns a default value of {@code 0} if no value is found.
	 * @return The base element resistance as a {@code double}.
	 */
	public double getBaseElementRes()
	{
		final Double val = _baseValues.get(Stat.BASE_ATTRIBUTE_RES);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base physical attack value.<br>
	 * This method looks up the {@code Stat.PHYSICAL_ATTACK} in the internal stats map.<br>
	 * It returns 0 if no value is found.
	 * @return The integer value of the base physical attack.
	 */
	public int getBasePAtk()
	{
		final Double val = _baseValues.get(Stat.PHYSICAL_ATTACK);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base magic attack value for this creature.<br>
	 * It looks up the {@code Stat.MAGIC_ATTACK} in the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base magic attack.
	 */
	public int getBaseMAtk()
	{
		final Double val = _baseValues.get(Stat.MAGIC_ATTACK);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base physical defense value for this creature.<br>
	 * It looks up the {@code Stat.PHYSICAL_DEFENCE} in the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base physical defense.
	 */
	public int getBasePDef()
	{
		final Double val = _baseValues.get(Stat.PHYSICAL_DEFENCE);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base magical defense value for this creature.<br>
	 * It looks up the {@code Stat.MAGICAL_DEFENCE} in the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base magical defense.
	 */
	public int getBaseMDef()
	{
		final Double val = _baseValues.get(Stat.MAGICAL_DEFENCE);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base physical attack speed of the creature.<br>
	 * This value is pulled from the {@code Stat.PHYSICAL_ATTACK_SPEED} entry in the internal stats map.<br>
	 * It returns 0 if no value is defined for this stat.
	 * @return The integer value of the base physical attack speed.
	 */
	public int getBasePAtkSpd()
	{
		final Double val = _baseValues.get(Stat.PHYSICAL_ATTACK_SPEED);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base magic attack speed of the creature.<br>
	 * This value is fetched from the {@code Stat.MAGIC_ATTACK_SPEED} entry.<br>
	 * It returns {@code 0} if no value is defined.
	 * @return The integer value of the base magic attack speed.
	 */
	public int getBaseMAtkSpd()
	{
		final Double val = _baseValues.get(Stat.MAGIC_ATTACK_SPEED);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Calculates the random damage value for this creature.<br>
	 * It retrieves the value from the {@code Stat.RANDOM_DAMAGE} entry.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the random damage or {@code 0} if not set.
	 */
	public int getRandomDamage()
	{
		final Double val = _baseValues.get(Stat.RANDOM_DAMAGE);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base shield defense value.<br>
	 * This method looks up the {@code SHIELD_DEFENCE} stat in the internal map.<br>
	 * It returns 0 if no value is found.
	 * @return The integer value of the base shield defense.
	 */
	public int getBaseShldDef()
	{
		final Double val = _baseValues.get(Stat.SHIELD_DEFENCE);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base shield defense rate.<br>
	 * This value is stored in the {@code _baseValues} map.<br>
	 * It returns {@code 0} if no value is found.
	 * @return The integer value of the base shield defense rate.
	 */
	public int getBaseShldRate()
	{
		final Double val = _baseValues.get(Stat.SHIELD_DEFENCE_RATE);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base critical rate for this creature.<br>
	 * This value is fetched from the {@code _baseValues} map.<br>
	 * It returns {@code 0} if no value is defined.
	 * @return The integer value of the base critical rate.
	 */
	public int getBaseCritRate()
	{
		final Double val = _baseValues.get(Stat.CRITICAL_RATE);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base magic critical rate for this creature.<br>
	 * It looks up the {@code MAGIC_CRITICAL_RATE} value from the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base magic critical rate.
	 */
	public int getBaseMCritRate()
	{
		final Double val = _baseValues.get(Stat.MAGIC_CRITICAL_RATE);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base critical rate for skills.<br>
	 * This value is fetched from the {@code Stat.CRITICAL_RATE_SKILL} entry.<br>
	 * It returns {@code 0} if no value is defined.
	 * @return The integer value of the skill critical rate.
	 */
	public int getPSkillCriticalRate()
	{
		final Double val = _baseValues.get(Stat.CRITICAL_RATE_SKILL);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base breath value for this creature.<br>
	 * It looks up the {@code Stat.BREATH} in the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base breath stat.
	 */
	public int getBaseBreath()
	{
		final Double val = _baseValues.get(Stat.BREATH);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base physical abnormal resistance value.<br>
	 * This method looks up the {@code Stat.ABNORMAL_RESIST_PHYSICAL} in the base values map.<br>
	 * It returns 0 if no value is defined for this stat.
	 * @return The integer value of the base physical abnormal resistance.
	 */
	public int getBaseAbnormalResistPhysical()
	{
		final Double val = _baseValues.get(Stat.ABNORMAL_RESIST_PHYSICAL);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the base magical abnormal resistance value.<br>
	 * This value is fetched from the {@code _baseValues} map.<br>
	 * It returns {@code 0} if no value is defined.
	 * @return The integer value of the magical abnormal resistance.
	 */
	public int getBaseAbnormalResistMagical()
	{
		final Double val = _baseValues.get(Stat.ABNORMAL_RESIST_MAGICAL);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Returns the collision radius of the creature.<br>
	 * This value is used to determine the physical size for collisions.
	 * @return The integer value of {@code _collisionRadius}.
	 */
	public int getCollisionRadius()
	{
		return _collisionRadius;
	}
	
	/**
	 * Retrieves the collision height of the creature.<br>
	 * This value is used for physical interactions in the game world.
	 * @return The {@code int} value representing the collision height.
	 */
	public int getCollisionHeight()
	{
		return _collisionHeight;
	}
	
	/**
	 * Returns the collision radius of the creature.<br>
	 * This value is used for client-side information.
	 * @return The {@code float} value of the collision radius.
	 */
	public float getFCollisionRadius()
	{
		return _fCollisionRadius;
	}
	
	/**
	 * Returns the collision height of the creature.<br>
	 * This value is used for physical interactions in the game world.
	 * @return The {@code float} value representing the collision height.
	 */
	public float getFCollisionHeight()
	{
		return _fCollisionHeight;
	}
	
	/**
	 * Retrieves the primary attack category for this creature.<br>
	 * This value determines how the creature interacts with different weapon types.
	 * @return the {@code WeaponType} of the base attack.
	 */
	public WeaponType getBaseAttackType()
	{
		return _baseAttackType;
	}
	
	/**
	 * Sets the primary attack category for this creature.<br>
	 * This value determines how the creature interacts with different weapon types.
	 * @param type The {@code WeaponType} to assign as the base attack type.
	 */
	public void setBaseAttackType(WeaponType type)
	{
		_baseAttackType = type;
	}
	
	/**
	 * Retrieves the base physical attack range for this creature.<br>
	 * It looks up the {@code Stat.PHYSICAL_ATTACK_RANGE} value from the internal stats map.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The integer value of the base physical attack range.
	 */
	public int getBaseAttackRange()
	{
		final Double val = _baseValues.get(Stat.PHYSICAL_ATTACK_RANGE);
		return val != null ? val.intValue() : 0;
	}
	
	/**
	 * Retrieves the skills associated with this template.<br>
	 * This method returns an empty map if no skills are defined.
	 * @return A {@code Map} where the key is an {@code Integer} and the value is a {@code Skill}.
	 */
	public Map<Integer, Skill> getSkills()
	{
		return Collections.emptyMap();
	}
	
	/**
	 * Retrieves the race of the creature.<br>
	 * This method returns the {@code Race} associated with this template.
	 * @return the {@code Race} of the creature.
	 */
	public Race getRace()
	{
		return _race;
	}
	
	/**
	 * Sets the race of the creature.<br>
	 * This updates the {@code _race} field with a new value.
	 * @param race The {@code Race} to assign to this template.
	 */
	public void setRace(Race race)
	{
		_race = race;
	}
	
	/**
	 * Retrieves the base value for a specific {@code Stat}.<br>
	 * If no value is found, it returns the provided {@code defaultValue}.
	 * @param stat The {@code Stat} type to look up.
	 * @param defaultValue The value to return if the {@code Stat} is not present.
	 * @return The base value of the requested {@code Stat} or the {@code defaultValue}.
	 */
	public double getBaseValue(Stat stat, double defaultValue)
	{
		final Double val = _baseValues.get(stat);
		return val != null ? val.doubleValue() : defaultValue;
	}
}
