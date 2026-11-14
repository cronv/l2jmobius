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
package org.l2jmobius.gameserver.config.custom;

import java.util.Arrays;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;

/**
 * This class loads all the custom class balance related configurations.<br>
 * It provides access to settings that adjust the stats and mechanics of different character classes.
 * @author Mobius
 */
public class ClassBalanceConfig
{
	// File
	private static final String CLASS_BALANCE_CONFIG_FILE = "./config/Custom/ClassBalance.ini";
	
	// Array size based on the highest PlayerClass id.
	private static final int ARRAY_SIZE = Arrays.stream(PlayerClass.values()).mapToInt(PlayerClass::getId).max().getAsInt() + 1;
	
	// Constants
	public static float[] PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_ENERGY_SKILL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_ENERGY_SKILL_DAMAGE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVE_ENERGY_SKILL_DEFENCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PVP_ENERGY_SKILL_DEFENCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] PLAYER_HEALING_SKILL_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] SKILL_MASTERY_CHANCE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] SKILL_REUSE_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] EXP_AMOUNT_MULTIPLIERS = new float[ARRAY_SIZE];
	public static float[] SP_AMOUNT_MULTIPLIERS = new float[ARRAY_SIZE];
	
	/**
	 * Loads all class balance configurations from the configuration file.<br>
	 * This method populates various multiplier arrays for skills and attributes.
	 */
	public static void load()
	{
		final ConfigReader config = new ConfigReader(CLASS_BALANCE_CONFIG_FILE);
		
		parseClassMultipliers(config, "PveMagicalSkillDamageMultipliers", PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpMagicalSkillDamageMultipliers", PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PveMagicalSkillDefenceMultipliers", PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpMagicalSkillDefenceMultipliers", PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS);
		parseClassMultipliers(config, "PveMagicalSkillCriticalChanceMultipliers", PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpMagicalSkillCriticalChanceMultipliers", PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS);
		parseClassMultipliers(config, "PveMagicalSkillCriticalDamageMultipliers", PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpMagicalSkillCriticalDamageMultipliers", PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PvePhysicalSkillDamageMultipliers", PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpPhysicalSkillDamageMultipliers", PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PvePhysicalSkillDefenceMultipliers", PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpPhysicalSkillDefenceMultipliers", PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS);
		parseClassMultipliers(config, "PvePhysicalSkillCriticalChanceMultipliers", PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpPhysicalSkillCriticalChanceMultipliers", PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS);
		parseClassMultipliers(config, "PvePhysicalSkillCriticalDamageMultipliers", PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpPhysicalSkillCriticalDamageMultipliers", PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PvePhysicalAttackDamageMultipliers", PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpPhysicalAttackDamageMultipliers", PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PvePhysicalAttackDefenceMultipliers", PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpPhysicalAttackDefenceMultipliers", PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS);
		parseClassMultipliers(config, "PvePhysicalAttackCriticalChanceMultipliers", PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpPhysicalAttackCriticalChanceMultipliers", PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS);
		parseClassMultipliers(config, "PvePhysicalAttackCriticalDamageMultipliers", PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpPhysicalAttackCriticalDamageMultipliers", PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PveBlowSkillDamageMultipliers", PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpBlowSkillDamageMultipliers", PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PveBlowSkillDefenceMultipliers", PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpBlowSkillDefenceMultipliers", PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS);
		parseClassMultipliers(config, "PveEnergySkillDamageMultipliers", PVE_ENERGY_SKILL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpEnergySkillDamageMultipliers", PVP_ENERGY_SKILL_DAMAGE_MULTIPLIERS);
		parseClassMultipliers(config, "PveEnergySkillDefenceMultipliers", PVE_ENERGY_SKILL_DEFENCE_MULTIPLIERS);
		parseClassMultipliers(config, "PvpEnergySkillDefenceMultipliers", PVP_ENERGY_SKILL_DEFENCE_MULTIPLIERS);
		parseClassMultipliers(config, "PlayerHealingSkillMultipliers", PLAYER_HEALING_SKILL_MULTIPLIERS);
		parseClassMultipliers(config, "SkillMasteryChanceMultipliers", SKILL_MASTERY_CHANCE_MULTIPLIERS);
		parseClassMultipliers(config, "SkillReuseMultipliers", SKILL_REUSE_MULTIPLIERS);
		parseClassMultipliers(config, "ExpAmountMultipliers", EXP_AMOUNT_MULTIPLIERS);
		parseClassMultipliers(config, "SpAmountMultipliers", SP_AMOUNT_MULTIPLIERS);
	}
	
	/**
	 * Parses class multipliers from a configuration file.<br>
	 * It fills the {@code multipliers} array with default values of 1f.<br>
	 * The method identifies classes by ID or name and applies specific values.
	 * @param config The {@code ConfigReader} used to fetch the configuration string.
	 * @param key The configuration key to look up.
	 * @param multipliers The array where parsed float values will be stored.
	 */
	private static void parseClassMultipliers(ConfigReader config, String key, float[] multipliers)
	{
		Arrays.fill(multipliers, 1f);
		for (String info : config.getString(key, "").trim().split(";"))
		{
			final String[] classInfo = info.trim().split("[*]");
			if (classInfo.length == 2)
			{
				final String id = classInfo[0].trim();
				multipliers[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
			}
		}
	}
}
