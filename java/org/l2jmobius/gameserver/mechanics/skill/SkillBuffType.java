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
package org.l2jmobius.gameserver.mechanics.skill;

/**
 * This enum represents the different categories for skill buffs.<br>
 * It is used to classify how a skill affects the target.<br>
 * Use {@code SkillBuffType} to determine the specific buff type of a skill.
 * @author Nik
 */
public enum SkillBuffType
{
	NONE,
	BUFF,
	DEBUFF,
	DANCE,
	TOGGLE,
	TRIGGER;
	
	/**
	 * Checks if the current skill buff type is {@code NONE}.<br>
	 * This method returns {@code true} if the type matches {@code SkillBuffType.NONE}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the type is {@code NONE}, otherwise {@code false}.
	 */
	public boolean isNone()
	{
		return this == NONE;
	}
	
	/**
	 * Checks if the current skill type is a buff.<br>
	 * This method returns {@code true} if the type is {@code BUFF}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if this is a buff, {@code false} otherwise.
	 */
	public boolean isBuff()
	{
		return this == BUFF;
	}
	
	/**
	 * Checks if the skill type is a debuff.<br>
	 * This method compares the current enum value to {@code SkillBuffType.DEBUFF}.
	 * @return {@code true} if the skill is a debuff, otherwise {@code false}.
	 */
	public boolean isDebuff()
	{
		return this == DEBUFF;
	}
	
	/**
	 * Checks if the current skill buff type is a dance.<br>
	 * This method compares the enum constant to {@code DANCE}.
	 * @return {@code true} if the type is {@code DANCE}, otherwise {@code false}.
	 */
	public boolean isDance()
	{
		return this == DANCE;
	}
	
	/**
	 * Checks if the skill buff type is a {@code TOGGLE}.<br>
	 * This method returns {@code true} if it matches.
	 * @return {@code true} if the type is {@code TOGGLE}, otherwise {@code false}.
	 */
	public boolean isToggle()
	{
		return this == TOGGLE;
	}
	
	/**
	 * Checks if the skill buff type is a {@code TRIGGER}.<br>
	 * This method returns {@code true} if it matches.
	 * @return {@code true} if the type is {@code TRIGGER}, otherwise {@code false}.
	 */
	public boolean isTrigger()
	{
		return this == TRIGGER;
	}
}
