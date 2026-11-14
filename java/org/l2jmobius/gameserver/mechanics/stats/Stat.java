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

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.function.DoubleBinaryOperator;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.AttributeFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.BaseStatFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.MAccuracyFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.MAttackFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.MAttackSpeedFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.MCritRateFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.MDefenseFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.MEvasionRateFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.MaxCpFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.MaxHpFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.MaxMpFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.MpVampiricChanceFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.PAccuracyFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.PAttackFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.PAttackSpeedFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.PCriticalRateFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.PDefenseFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.PEvasionRateFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.PRangeFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.PSkillCritRateFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.RandomDamageFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.RegenCPFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.RegenHPFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.RegenMPFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.ShieldDefenceFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.ShieldDefenceRateFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.ShotsBonusFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.SpeedFinalizer;
import org.l2jmobius.gameserver.mechanics.stats.finalizers.VampiricChanceFinalizer;
import org.l2jmobius.gameserver.util.MathUtil;

/**
 * Basic in-game statistics registry identifying attribute and mechanic types.
 * @author mkizub, Mobius
 */
public enum Stat
{
	// HP, MP & CP
	HP_LIMIT("hpLimit"),
	MAX_HP("maxHp", new MaxHpFinalizer()),
	MAX_MP("maxMp", new MaxMpFinalizer()),
	MAX_CP("maxCp", new MaxCpFinalizer()),
	MAX_RECOVERABLE_HP("maxRecoverableHp"), // The maximum HP that is able to be recovered trough heals.
	MAX_RECOVERABLE_MP("maxRecoverableMp"),
	MAX_RECOVERABLE_CP("maxRecoverableCp"),
	REGENERATE_HP_RATE("regHp", new RegenHPFinalizer()),
	REGENERATE_CP_RATE("regCp", new RegenCPFinalizer()),
	REGENERATE_MP_RATE("regMp", new RegenMPFinalizer()),
	ADDITIONAL_POTION_HP("addPotionHp"),
	ADDITIONAL_POTION_MP("addPotionMp"),
	ADDITIONAL_POTION_CP("addPotionCp"),
	RESURRECTION_HP_BONUS("resurrectionHpBonus"), // Extra percentage of Max HP restored when resurrected.
	MANA_CHARGE("manaCharge"),
	HEAL_EFFECT("healEffect"),
	HEAL_EFFECT_ADD("healEffectAdd"),
	
	// ATTACK & DEFENCE
	PHYSICAL_DEFENCE("pDef", new PDefenseFinalizer()),
	MAGICAL_DEFENCE("mDef", new MDefenseFinalizer()),
	PHYSICAL_ATTACK("pAtk", new PAttackFinalizer()),
	MAGIC_ATTACK("mAtk", new MAttackFinalizer()),
	WEAPON_BONUS_PHYSICAL_ATTACK("weaponBonusPAtk"),
	WEAPON_BONUS_PHYSICAL_ATTACK_MULTIPIER("weaponBonusPAtkMultiplier"),
	WEAPON_BONUS_MAGIC_ATTACK("weaponBonusMAtk"),
	WEAPON_BONUS_MAGIC_ATTACK_MULTIPIER("weaponBonusMAtkMultiplier"),
	PHYSICAL_ATTACK_SPEED("pAtkSpd", new PAttackSpeedFinalizer()),
	MAGIC_ATTACK_SPEED("mAtkSpd", new MAttackSpeedFinalizer()), // Magic Skill Casting Time Rate
	ATK_REUSE("atkReuse"), // Bows Hits Reuse Rate
	SHIELD_DEFENCE("sDef", new ShieldDefenceFinalizer()),
	SHIELD_DEFENCE_IGNORE_REMOVAL("shieldDefIgnoreRemoval"),
	SHIELD_DEFENCE_IGNORE_REMOVAL_ADD("shieldDefIgnoreRemovalAdd"),
	CRITICAL_DAMAGE("cAtk"),
	CRITICAL_DAMAGE_ADD("cAtkAdd"), // this is another type for special critical damage mods - vicious stance, critical power and critical damage SA
	HATE_ATTACK("attackHate"),
	REAR_DAMAGE_RATE("rearDamage"),
	DRAGON_WEAPON_DEFENCE("dragonWeaponDefence"),
	AUTO_ATTACK_DAMAGE_BONUS("autoAttackDamageBonus"),
	IGNORE_REDUCE_DAMAGE("ignoreReduceDamage"),
	STATIC_PHYSICAL_ATTACK_SPEED("staticPAtkSpeed"),
	STATIC_MAGICAL_ATTACK_SPEED("staticMAtkSpeed"),
	
	// PVP BONUS
	PVP_PHYSICAL_ATTACK_DAMAGE("pvpPhysDmg"),
	PVP_MAGICAL_SKILL_DAMAGE("pvpMagicalDmg"),
	PVP_PHYSICAL_SKILL_DAMAGE("pvpPhysSkillsDmg"),
	PVP_PHYSICAL_ATTACK_DEFENCE("pvpPhysDef"),
	PVP_MAGICAL_SKILL_DEFENCE("pvpMagicalDef"),
	PVP_PHYSICAL_SKILL_DEFENCE("pvpPhysSkillsDef"),
	
	// PVE BONUS
	PVE_PHYSICAL_ATTACK_DAMAGE("pvePhysDmg"),
	PVE_PHYSICAL_SKILL_DAMAGE("pvePhysSkillDmg"),
	PVE_MAGICAL_SKILL_DAMAGE("pveMagicalDmg"),
	PVE_PHYSICAL_ATTACK_DEFENCE("pvePhysDef"),
	PVE_PHYSICAL_SKILL_DEFENCE("pvePhysSkillDef"),
	PVE_MAGICAL_SKILL_DEFENCE("pveMagicalDef"),
	PVE_RAID_PHYSICAL_ATTACK_DAMAGE("pveRaidPhysDmg"),
	PVE_RAID_PHYSICAL_SKILL_DAMAGE("pveRaidPhysSkillDmg"),
	PVE_RAID_MAGICAL_SKILL_DAMAGE("pveRaidMagicalDmg"),
	PVE_RAID_PHYSICAL_ATTACK_DEFENCE("pveRaidPhysDef"),
	PVE_RAID_PHYSICAL_SKILL_DEFENCE("pveRaidPhysSkillDef"),
	PVE_RAID_MAGICAL_SKILL_DEFENCE("pveRaidMagicalDef"),
	
	// FIXED BONUS
	PVP_DAMAGE_TAKEN("pvpDamageTaken"),
	PVE_DAMAGE_TAKEN("pveDamageTaken"),
	PVE_DAMAGE_TAKEN_MONSTER("pveDamageTakenMonster"),
	PVE_DAMAGE_TAKEN_RAID("pveDamageTakenRaid"),
	
	// ATTACK & DEFENCE RATES
	MAGIC_CRITICAL_DAMAGE("mCritPower"),
	SKILL_POWER_ADD("skillPowerAdd"),
	PHYSICAL_SKILL_POWER("physicalSkillPower"),
	MAGICAL_SKILL_POWER("magicalSkillPower"),
	CHARGING_SKILL_POWER("chargingSkillPower"),
	PHYSICAL_SKILL_CRITICAL_DAMAGE("cAtkSkill"),
	PHYSICAL_SKILL_CRITICAL_DAMAGE_ADD("cAtkSkillAdd"),
	MAGIC_CRITICAL_DAMAGE_ADD("mCritPowerAdd"),
	SHIELD_DEFENCE_RATE("rShld", new ShieldDefenceRateFinalizer()),
	CRITICAL_RATE("critRate", new PCriticalRateFinalizer(), MathUtil::add, MathUtil::add, 0, 1),
	CRITICAL_RATE_SKILL("physicalSkillCriticalRate", new PSkillCritRateFinalizer()),
	ADD_MAX_MAGIC_CRITICAL_RATE("addMaxMagicCritRate"),
	ADD_MAX_PHYSICAL_CRITICAL_RATE("addMaxPhysicalCritRate"),
	ADD_MAX_PHYSICAL_SKILL_CRITICAL_RATE("addMaxPhysicalSkillCritRate"),
	MAGIC_CRITICAL_RATE("mCritRate", new MCritRateFinalizer()),
	DEFENCE_CRITICAL_RATE("defCritRate"),
	DEFENCE_CRITICAL_RATE_ADD("defCritRateAdd"),
	DEFENCE_MAGIC_CRITICAL_RATE("defMCritRate"),
	DEFENCE_MAGIC_CRITICAL_RATE_ADD("defMCritRateAdd"),
	DEFENCE_CRITICAL_DAMAGE("defCritDamage"),
	DEFENCE_MAGIC_CRITICAL_DAMAGE("defMCritDamage"),
	DEFENCE_MAGIC_CRITICAL_DAMAGE_ADD("defMCritDamageAdd"),
	DEFENCE_CRITICAL_DAMAGE_ADD("defCritDamageAdd"), // Resistance to critical damage in value (Example: +100 will be 100 more critical damage, NOT 100% more).
	DEFENCE_PHYSICAL_SKILL_CRITICAL_DAMAGE("defCAtkSkill"),
	DEFENCE_PHYSICAL_SKILL_CRITICAL_DAMAGE_ADD("defCAtkSkillAdd"),
	DEFENCE_PHYSICAL_SKILL_CRITICAL_RATE("defPhysSkillCritRate"),
	DEFENCE_PHYSICAL_SKILL_CRITICAL_RATE_ADD("defPhysSkillCritRateAdd"),
	DEFENCE_IGNORE_REMOVAL("defIgnoreRemoval"),
	DEFENCE_IGNORE_REMOVAL_ADD("defIgnoreRemovalAdd"),
	AREA_OF_EFFECT_DAMAGE_DEFENCE("aoeDamageDefence"),
	AREA_OF_EFFECT_DAMAGE_DEFENCE_ADD("aoeDamageDefenceAdd"),
	AREA_OF_EFFECT_DAMAGE_MODIFY("aoeDamageModify"),
	BLOW_RATE("blowRate"),
	BLOW_RATE_DEFENCE("blowRateDefence"),
	INSTANT_KILL_RESIST("instantKillResist"),
	EXPSP_RATE("rExp"),
	ACTIVE_BONUS_EXP("activeBonusExp"), // Used to measure active skill bonus exp.
	BONUS_EXP_BUFFS("bonusExpBuffs"), // Used to count active skill exp.
	BONUS_EXP_PASSIVES("bonusExpPassives"), // Used to count passive skill exp.
	BONUS_EXP("bonusExp"),
	BONUS_SP("bonusSp"),
	BONUS_DROP_ADENA("bonusDropAdena"),
	BONUS_DROP_AMOUNT("bonusDropAmount"),
	BONUS_DROP_RATE("bonusDropRate"),
	BONUS_SPOIL_RATE("bonusSpoilRate"),
	BONUS_RAID_POINTS("bonusRaidPoints"),
	ATTACK_CANCEL("cancel"),
	ARMOR_BONUS_MAGICAL_DEFENCE("armorBonusMagicalDefence"),
	ARMOR_BONUS_PHYSICAL_DEFENCE("armorBonusPhysicalDefence"),
	
	// ACCURACY & RANGE
	ACCURACY_COMBAT("accCombat", new PAccuracyFinalizer()),
	ACCURACY_MAGIC("accMagic", new MAccuracyFinalizer()),
	EVASION_RATE("rEvas", new PEvasionRateFinalizer()),
	MAGIC_EVASION_RATE("mEvas", new MEvasionRateFinalizer()),
	ACCURACY_BONUS("accBonus"),
	PHYSICAL_ATTACK_RANGE("pAtkRange", new PRangeFinalizer()),
	MAGIC_ATTACK_RANGE("mAtkRange"),
	ATTACK_COUNT_MAX("atkCountMax"),
	PHYSICAL_POLEARM_TARGET_SINGLE("polearmSingleTarget"),
	WEAPON_ATTACK_ANGLE_BONUS("weaponAttackAngleBonus"),
	
	// Run speed, walk & escape speed are calculated proportionally, magic speed is a buff.
	MOVE_SPEED("moveSpeed"),
	SPEED_LIMIT("speedLimit"),
	RUN_SPEED("runSpd", new SpeedFinalizer()),
	WALK_SPEED("walkSpd", new SpeedFinalizer()),
	SWIM_RUN_SPEED("fastSwimSpd", new SpeedFinalizer()),
	SWIM_WALK_SPEED("slowSimSpd", new SpeedFinalizer()),
	FLY_RUN_SPEED("fastFlySpd", new SpeedFinalizer()),
	FLY_WALK_SPEED("slowFlySpd", new SpeedFinalizer()),
	STATIC_SPEED("staticSpeed"),
	
	// BASIC STATS
	STAT_STR("STR", new BaseStatFinalizer()),
	STAT_CON("CON", new BaseStatFinalizer()),
	STAT_DEX("DEX", new BaseStatFinalizer()),
	STAT_INT("INT", new BaseStatFinalizer()),
	STAT_WIT("WIT", new BaseStatFinalizer()),
	STAT_MEN("MEN", new BaseStatFinalizer()),
	STAT_LUC("LUC", new BaseStatFinalizer()),
	STAT_CHA("CHA", new BaseStatFinalizer()),
	
	// Special stats, share one slot in Calculator.
	
	// VARIOUS
	BREATH("breath"),
	FALL("fall"),
	FISHING_EXP_SP_BONUS("fishingExpSpBonus"),
	ENCHANT_RATE("enchantRate"),
	
	// VULNERABILITIES
	DAMAGE_ZONE_VULN("damageZoneVuln"),
	RESIST_DISPEL_BUFF("cancelVuln"), // Resistance for cancel type skills
	RESIST_ABNORMAL_DEBUFF("debuffVuln"),
	
	// RESISTANCES
	FIRE_RES("fireRes", new AttributeFinalizer(AttributeType.FIRE, false)),
	WIND_RES("windRes", new AttributeFinalizer(AttributeType.WIND, false)),
	WATER_RES("waterRes", new AttributeFinalizer(AttributeType.WATER, false)),
	EARTH_RES("earthRes", new AttributeFinalizer(AttributeType.EARTH, false)),
	HOLY_RES("holyRes", new AttributeFinalizer(AttributeType.HOLY, false)),
	DARK_RES("darkRes", new AttributeFinalizer(AttributeType.DARK, false)),
	BASE_ATTRIBUTE_RES("baseAttrRes"),
	MAGIC_SUCCESS_RES("magicSuccRes"),
	
	// BUFF_IMMUNITY("buffImmunity"), // TODO: Implement me
	ABNORMAL_RESIST_PHYSICAL("abnormalResPhysical"),
	ABNORMAL_RESIST_MAGICAL("abnormalResMagical"),
	REAL_DAMAGE_RESIST("realDamageResist"),
	
	// ELEMENT POWER
	FIRE_POWER("firePower", new AttributeFinalizer(AttributeType.FIRE, true)),
	WATER_POWER("waterPower", new AttributeFinalizer(AttributeType.WATER, true)),
	WIND_POWER("windPower", new AttributeFinalizer(AttributeType.WIND, true)),
	EARTH_POWER("earthPower", new AttributeFinalizer(AttributeType.EARTH, true)),
	HOLY_POWER("holyPower", new AttributeFinalizer(AttributeType.HOLY, true)),
	DARK_POWER("darkPower", new AttributeFinalizer(AttributeType.DARK, true)),
	
	// PROFICIENCY
	REFLECT_DAMAGE_PERCENT("reflectDam"),
	REFLECT_DAMAGE_PERCENT_DEFENSE("reflectDamDef"),
	REFLECT_SKILL_MAGIC("reflectSkillMagic"), // Need rework
	REFLECT_SKILL_PHYSIC("reflectSkillPhysic"), // Need rework
	VENGEANCE_SKILL_MAGIC_DAMAGE("vengeanceMdam"),
	VENGEANCE_SKILL_PHYSICAL_DAMAGE("vengeancePdam"),
	ABSORB_DAMAGE_PERCENT("absorbDam"),
	ABSORB_DAMAGE_CHANCE("absorbDamChance", new VampiricChanceFinalizer()),
	ABSORB_DAMAGE_DEFENCE("absorbDamDefence"),
	TRANSFER_DAMAGE_SUMMON_PERCENT("transDam"),
	MANA_SHIELD_PERCENT("manaShield"),
	TRANSFER_DAMAGE_TO_PLAYER("transDamToPlayer"),
	ABSORB_MANA_DAMAGE_PERCENT("absorbDamMana"),
	ABSORB_MANA_DAMAGE_CHANCE("absorbDamManaChance", new MpVampiricChanceFinalizer()),
	
	WEIGHT_LIMIT("weightLimit"),
	WEIGHT_PENALTY("weightPenalty"),
	
	// ExSkill
	INVENTORY_NORMAL("inventoryLimit"),
	STORAGE_PRIVATE("whLimit"),
	TRADE_SELL("PrivateSellLimit"),
	TRADE_BUY("PrivateBuyLimit"),
	RECIPE_DWARVEN("DwarfRecipeLimit"),
	RECIPE_COMMON("CommonRecipeLimit"),
	
	// Skill mastery
	SKILL_MASTERY("skillMastery"),
	SKILL_MASTERY_RATE("skillMasteryRate"),
	
	// Vitality
	VITALITY_CONSUME_RATE("vitalityConsumeRate"),
	VITALITY_EXP_RATE("vitalityExpRate"),
	VITALITY_SKILLS("vitalitySkills"), // Used to count vitality skill bonuses.
	
	// Souls
	MAX_SOULS("maxSouls"),
	
	REDUCE_EXP_LOST_BY_PVP("reduceExpLostByPvp"),
	REDUCE_EXP_LOST_BY_MOB("reduceExpLostByMob"),
	REDUCE_EXP_LOST_BY_RAID("reduceExpLostByRaid"),
	
	REDUCE_DEATH_PENALTY_BY_PVP("reduceDeathPenaltyByPvp"),
	REDUCE_DEATH_PENALTY_BY_MOB("reduceDeathPenaltyByMob"),
	REDUCE_DEATH_PENALTY_BY_RAID("reduceDeathPenaltyByRaid"),
	
	// Brooches
	BROOCH_JEWELS("broochJewels"),
	
	// Agathions
	AGATHION_SLOTS("agathionSlots"),
	
	// Artifacts
	ARTIFACT_SLOTS("artifactSlots"),
	
	// Summon Points
	MAX_SUMMON_POINTS("summonPoints"),
	
	// Cubic Count
	MAX_CUBIC("cubicCount"),
	
	// The maximum allowed range to be damaged/debuffed from.
	SPHERIC_BARRIER_RANGE("sphericBarrier"),
	
	// Blocks given amount of debuffs.
	DEBUFF_BLOCK("debuffBlock"),
	
	// Affects the random weapon damage.
	RANDOM_DAMAGE("randomDamage", new RandomDamageFinalizer()),
	
	// Affects the random weapon damage.
	DAMAGE_LIMIT("damageCap"),
	
	// Maximun momentum one can charge.
	MAX_MOMENTUM("maxMomentum"),
	
	// Which base stat ordinal should alter skill critical formula.
	STAT_BONUS_SKILL_CRITICAL("statSkillCritical"),
	STAT_BONUS_SPEED("statSpeed"),
	CRAFTING_CRITICAL("craftingCritical"),
	SHOTS_BONUS("shotBonus", new ShotsBonusFinalizer()),
	SOULSHOT_RESISTANCE("soulshotResistance"),
	SPIRITSHOT_RESISTANCE("spiritshotResistance"),
	WORLD_CHAT_POINTS("worldChatPoints"),
	ATTACK_DAMAGE("attackDamage"),
	
	IMMOBILE_DAMAGE_BONUS("immobileBonus"),
	IMMOBILE_DAMAGE_RESIST("immobileResist"),
	
	CRAFT_RATE("CraftRate"),
	
	// Othell Perfection
	PERFECTION_RATE("perfectionRate"),
	PERFECTION_POWER("perfectionPower"),
	PERFECTION_RESIST("perfectionResist");
	
	public static final int NUM_STATS = values().length;
	
	private final String _value;
	private final IStatFunction _valueFinalizer;
	private final DoubleBinaryOperator _addFunction;
	private final DoubleBinaryOperator _mulFunction;
	private final Double _resetAddValue;
	private final Double _resetMulValue;
	
	/**
	 * Returns the XML name of this stat.
	 * @return the XML name string
	 */
	public String getValue()
	{
		return _value;
	}
	
	/**
	 * Constructs a stat backed by the given XML name using default finalizer and math functions.
	 * @param xmlString XML name identifying this stat
	 */
	Stat(String xmlString)
	{
		this(xmlString, Stat::defaultValue, MathUtil::add, MathUtil::mul, 0, 1);
	}
	
	/**
	 * Constructs a stat with the given XML name and value finalizer using default math functions.
	 * @param xmlString XML name identifying this stat
	 * @param valueFinalizer function that finalizes this stat value
	 */
	Stat(String xmlString, IStatFunction valueFinalizer)
	{
		this(xmlString, valueFinalizer, MathUtil::add, MathUtil::mul, 0, 1);
	}
	
	/**
	 * Constructs a stat with the given XML name, finalizer, add/multiply operators and reset baselines.
	 * @param xmlString XML name identifying this stat
	 * @param valueFinalizer function that finalizes this stat value
	 * @param addFunction operator combining additive modifiers
	 * @param mulFunction operator combining multiplicative modifiers
	 * @param resetAddValue additive baseline applied on reset
	 * @param resetMulValue multiplicative baseline applied on reset
	 */
	Stat(String xmlString, IStatFunction valueFinalizer, DoubleBinaryOperator addFunction, DoubleBinaryOperator mulFunction, double resetAddValue, double resetMulValue)
	{
		_value = xmlString;
		_valueFinalizer = valueFinalizer;
		_addFunction = addFunction;
		_mulFunction = mulFunction;
		_resetAddValue = resetAddValue;
		_resetMulValue = resetMulValue;
	}
	
	private static final Map<String, Stat> BY_XML_NAME = new HashMap<>(values().length);
	static
	{
		for (Stat s : values())
		{
			BY_XML_NAME.put(s.getValue(), s);
		}
	}
	
	/**
	 * Returns the stat registered under the given XML name.
	 * @param name XML name to look up
	 * @return the matching stat
	 * @throws NoSuchElementException if no stat matches the name
	 */
	public static Stat valueOfXml(String name)
	{
		final String internName = name.intern();
		final Stat stat = BY_XML_NAME.get(internName);
		if (stat == null)
		{
			throw new NoSuchElementException("Unknown name '" + internName + "' for enum " + Stat.class.getSimpleName());
		}
		return stat;
	}
	
	/**
	 * Finalizes this stat for the given creature, falling back to the default value on error.
	 * @param creature creature to compute the stat for
	 * @param baseValue optional base value
	 * @return the finalized stat value
	 */
	public double finalize(Creature creature, OptionalDouble baseValue)
	{
		try
		{
			return _valueFinalizer.calc(creature, baseValue, this);
		}
		catch (Exception e)
		{
			// LOGGER.log(Level.WARNING, "Exception during finalization for : " + creature + " stat: " + toString() + " : ", e);
			return defaultValue(creature, baseValue, this);
		}
	}
	
	/**
	 * Applies this stat add operator to combine the given values.
	 * @param oldValue current accumulated value
	 * @param value value to add
	 * @return the combined result
	 */
	public double functionAdd(double oldValue, double value)
	{
		return _addFunction.applyAsDouble(oldValue, value);
	}
	
	/**
	 * Applies this stat multiply operator to combine the given values.
	 * @param oldValue current accumulated value
	 * @param value multiplier to apply
	 * @return the combined result
	 */
	public double functionMul(double oldValue, double value)
	{
		return _mulFunction.applyAsDouble(oldValue, value);
	}
	
	/**
	 * Returns the additive baseline applied on reset.
	 * @return the reset add value
	 */
	public Double getResetAddValue()
	{
		return _resetAddValue;
	}
	
	/**
	 * Returns the multiplicative baseline applied on reset.
	 * @return the reset multiply value
	 */
	public Double getResetMulValue()
	{
		return _resetMulValue;
	}
	
	/**
	 * Computes the weapon base value of the given stat for the creature via its finalizer.
	 * @param creature creature to compute the value for
	 * @param stat stat whose weapon base value is computed
	 * @return the weapon base value
	 */
	public static double weaponBaseValue(Creature creature, Stat stat)
	{
		return stat._valueFinalizer.calcWeaponBaseValue(creature, stat);
	}
	
	/**
	 * Computes the default stat value, using the base when present or move-type contribution otherwise.
	 * @param creature creature to compute the stat for
	 * @param base optional base value
	 * @param stat stat being computed
	 * @return the default stat value
	 */
	public static double defaultValue(Creature creature, OptionalDouble base, Stat stat)
	{
		final double mul = creature.getStat().getMulValue(stat);
		final double add = creature.getStat().getAddValue(stat);
		return base.isPresent() ? defaultValue(creature, stat, base.getAsDouble()) : mul * (add + creature.getStat().getMoveTypeValue(stat, creature.getMoveType()));
	}
	
	/**
	 * Computes the default stat value by applying multiplier, addition and move-type contribution to the base.
	 * @param creature creature to compute the stat for
	 * @param stat stat being computed
	 * @param baseValue base value before modifiers
	 * @return the default stat value
	 */
	public static double defaultValue(Creature creature, Stat stat, double baseValue)
	{
		final double mul = creature.getStat().getMulValue(stat);
		final double add = creature.getStat().getAddValue(stat);
		return (mul * baseValue) + add + creature.getStat().getMoveTypeValue(stat, creature.getMoveType());
	}
}
