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
package org.l2jmobius.gameserver.mechanics.skill;

import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This {@code Enum} provides a central location for common skill references.<br>
 * It allows developers to easily access frequently used skills throughout the codebase.
 * @author DrHouse, Mobius
 */
public enum CommonSkill
{
	CREATE_DWARVEN(172, 1),
	LUCKY(194, 1),
	EXPERTISE(239, 1),
	SEAL_OF_RULER(246, 1),
	BUILD_HEADQUARTERS(247, 1),
	CRYSTALLIZE(248, 1),
	STRIDER_SIEGE_ASSAULT(325, 1),
	WYVERN_AEGIS(327, 1),
	ONYX_BEAST_TRANSFORMATION(617, 1),
	CREATE_COMMON(1320, 1),
	DIVINE_INSPIRATION(1405, 1),
	LARGE_FIREWORK(2025, 1),
	SPECIAL_TREE_RECOVERY_BONUS(2139, 1),
	CARAVANS_SECRET_MEDICINE(2341, 1),
	VOID_BURST(3630, 1),
	VOID_FLOW(3631, 1),
	RAID_CURSE(4215, 1),
	HINDER_STRIDER(4258, 1),
	WEIGHT_PENALTY(4270, 1),
	WYVERN_BREATH(4289, 1),
	RAID_CURSE2(4515, 1),
	NPC_ULTIMATE_DEFENSE(5044, 3),
	THE_VICTOR_OF_WAR(5074, 1),
	THE_VANQUISHED_OF_WAR(5075, 1),
	BLESSING_OF_PROTECTION(5182, 1),
	FIREWORK(5965, 1),
	PET_SWITCH_STANCE(6054, 1),
	FROG_TRANSFORM(6201, 1),
	CHILD_TRANSFORM(6202, 1),
	NATIVE_TRANSFORM(6203, 1),
	WEAPON_GRADE_PENALTY(6209, 1),
	ARMOR_GRADE_PENALTY(6213, 1),
	FEATHER_OF_BLESSING(7008, 1),
	SHILENS_BREATH(14571, 1),
	HAIR_ACCESSORY_SET(17192, 1),
	ALCHEMY_CUBE(17943, 1),
	ALCHEMY_CUBE_RANDOM_SUCCESS(17966, 1),
	LUCKY_CLOVER(18103, 1),
	IMPRIT_OF_LIGHT(19034, 1),
	IMPRIT_OF_DARKNESS(19035, 1),
	EINHASAD_OVERSEEING_1(29413, 1),
	EINHASAD_OVERSEEING_2(29413, 2),
	EINHASAD_OVERSEEING_3(29413, 3),
	EINHASAD_OVERSEEING_4(29413, 4),
	EINHASAD_CHAINS(29414, 1),
	KNIGHT_OF_SPLENDOR(32117, 1),
	PROTECTION_OF_LIGHT(32119, 1),
	KNIGHT_OF_DARKNESS(32120, 1),
	PROTECTION_OF_DARKNESS(32122, 1),
	OLYMPIAD_WIN(32417, 1),
	TRANQUIL_SOUL(32935, 1),
	TELEPORTATION_CUBE(39723, 1),
	BOT_REPORT_STATUS(55031, 1),
	OLYMPIAD_FEVER_MELODY(62078, 1),
	OLYMPIAD_DRUMBEAT_OF_SERENITY(62079, 1),
	OLYMPIAD_FANTASIA_HARMONY(62080, 1);
	
	private final SkillHolder _holder;
	
	/**
	 * Creates a new instance of {@code CommonSkill}.<br>
	 * This constructor initializes the internal {@code SkillHolder}.
	 * @param id The unique identifier for the skill.
	 * @param level The level associated with the skill.
	 */
	CommonSkill(int id, int level)
	{
		_holder = new SkillHolder(id, level);
	}
	
	/**
	 * Retrieves the unique identifier for this skill.<br>
	 * This value is stored within the {@code SkillHolder}.
	 * @return The {@code int} ID of the skill.
	 */
	public int getId()
	{
		return _holder.getSkillId();
	}
	
	/**
	 * Retrieves the skill level for this common skill.<br>
	 * This value is fetched from the internal {@code SkillHolder}.
	 * @return The integer level of the skill.
	 */
	public int getLevel()
	{
		return _holder.getSkillLevel();
	}
	
	/**
	 * Retrieves the {@code Skill} object associated with this common skill.<br>
	 * This method fetches the data from the internal {@code SkillHolder}.
	 * @return the {@code Skill} instance for this entry.
	 */
	public Skill getSkill()
	{
		return _holder.getSkill();
	}
}
