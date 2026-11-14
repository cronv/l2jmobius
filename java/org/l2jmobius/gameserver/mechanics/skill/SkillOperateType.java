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
 * This enum defines the different types of skill operations available in the game.<br>
 * It categorizes skills into specific identifiers such as {@code A1}, {@code CA1}, and {@code P}.<br>
 * Use this class to distinguish between various skill execution modes.
 * @author Zoey76
 */
public enum SkillOperateType
{
	/**
	 * Active Skill with "Instant Effect" (for example damage skills heal/pdam/mdam/cpdam skills).
	 */
	A1,
	
	/**
	 * Active Skill with "Continuous effect + Instant effect" (for example buff/debuff or damage/heal over time skills).
	 */
	A2,
	
	/**
	 * Active Skill with "Instant effect for target + Continuous effect + Continuous effect for self"
	 */
	A3,
	
	/**
	 * Active Skill with "Instant effect + ?" used for special event herb.
	 */
	A4,
	
	/**
	 * Aura Active Skill
	 */
	A5,
	
	/**
	 * Synergy Active Skill
	 */
	A6,
	
	/**
	 * Continuous Active Skill with "instant effect" (instant effect casted by ticks).
	 */
	CA1,
	
	/**
	 * ?
	 */
	CA2,
	
	/**
	 * Continuous Active Skill with "continuous effect" (continuous effect casted by ticks).
	 */
	CA5,
	
	/**
	 * Directional Active Skill with "Charge/Rush instant effect".
	 */
	DA1,
	
	/**
	 * Directional Active Skill with "Charge/Rush Continuous effect".
	 */
	DA2,
	
	/**
	 * Directional Active Skill with Blink effect
	 */
	DA3,
	
	/**
	 * Directional Active Skill with "Left Continuous effect".
	 */
	DA4,
	
	/**
	 * Directional Active Skill with "Right Continuous effect".
	 */
	DA5,
	
	/**
	 * Directional Active Skill with "Charge/Rush instant effect based on skill cast range".
	 */
	DA6,
	
	/**
	 * Passive Skill.
	 */
	P,
	
	/**
	 * Toggle Skill.
	 */
	T,
	
	/**
	 * Toggle Skill with Group.
	 */
	TG,
	
	/**
	 * Aura Skill.
	 */
	AU;
	
	/**
	 * Checks if the current skill type is an active skill.<br>
	 * This method returns {@code true} for various active types like A1, A2, or DA1.<br>
	 * It returns {@code false} for passive, toggle, or aura skills.
	 * @return {@code true} if the skill is active, {@code false} otherwise
	 */
	public boolean isActive()
	{
		switch (this)
		{
			case A1:
			case A2:
			case A3:
			case A4:
			case A5:
			case A6:
			case CA1:
			case CA5:
			case DA1:
			case DA2:
			case DA4:
			case DA5:
			case DA6:
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	/**
	 * Checks if the skill type has a continuous effect.<br>
	 * This method returns {@code true} for types that apply effects over time.
	 * @return {@code true} if the skill is continuous, {@code false} otherwise.
	 */
	public boolean isContinuous()
	{
		switch (this)
		{
			case A2:
			case A3:
			case A4:
			case A5:
			case A6:
			case DA2:
			case DA4:
			case DA5:
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	/**
	 * Checks if the skill type has a continuous effect on itself.<br>
	 * This specifically identifies the {@code A3} operative type.
	 * @return {@code true} if the skill is self-continuous, {@code false} otherwise
	 */
	public boolean isSelfContinuous()
	{
		return (this == A3);
	}
	
	/**
	 * Checks if the skill type is a passive skill.<br>
	 * Returns {@code true} if the type is {@code P}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if this is a passive skill, {@code false} otherwise
	 */
	public boolean isPassive()
	{
		return (this == P);
	}
	
	/**
	 * Checks if the skill type is a toggle-based operative type.<br>
	 * This includes types like {@code T}, {@code TG}, and {@code AU}.
	 * @return {@code true} if it is a toggle type, {@code false} otherwise
	 */
	public boolean isToggle()
	{
		return (this == T) || (this == TG) || (this == AU);
	}
	
	/**
	 * Checks if the skill type is an aura.<br>
	 * This returns {@code true} for types {@code A5}, {@code A6}, or {@code AU}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the skill is an aura, {@code false} otherwise.
	 */
	public boolean isAura()
	{
		return (this == A5) || (this == A6) || (this == AU);
	}
	
	/**
	 * Checks if the skill type should hide its messages.<br>
	 * This is used to determine if certain actions are not broadcasted to other players.
	 * @return {@code true} if messages are hidden, {@code false} otherwise
	 */
	public boolean isHidingMessages()
	{
		return (this == P) || (this == A5) || (this == A6) || (this == TG);
	}
	
	/**
	 * Checks if the skill type should be hidden from public broadcasts.<br>
	 * This is used for skills that do not need to be announced to other players.
	 * @return {@code true} if the skill is not broadcastable, {@code false} otherwise
	 */
	public boolean isNotBroadcastable()
	{
		return (this == AU) || (this == A5) || (this == A6) || (this == TG) || (this == T);
	}
	
	/**
	 * Checks if the skill type is a channeling skill.<br>
	 * This returns {@code true} for types like {@code CA1}, {@code CA2}, and {@code CA5}.
	 * @return {@code true} if it is a channeling skill, {@code false} otherwise.
	 */
	public boolean isChanneling()
	{
		switch (this)
		{
			case CA1:
			case CA2:
			case CA5:
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	/**
	 * Checks if the skill operative type is a synergy skill.<br>
	 * This method returns {@code true} only for the {@code A6} type.
	 * @return {@code true} if the skill is a synergy type, {@code false} otherwise
	 */
	public boolean isSynergy()
	{
		return (this == A6);
	}
	
	/**
	 * Checks if the skill is a directional type.<br>
	 * This method returns {@code true} for all types from {@code DA1} to {@code DA5}.<br>
	 * It returns {@code false} for all other types.
	 * @return {@code true} if the skill is a directional type, {@code false} otherwise
	 */
	public boolean isFlyType()
	{
		switch (this)
		{
			case DA1:
			case DA2:
			case DA3:
			case DA4:
			case DA5:
			case DA6:
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
}
