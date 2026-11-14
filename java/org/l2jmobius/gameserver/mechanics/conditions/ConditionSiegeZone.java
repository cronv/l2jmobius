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
package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if a creature is currently located within a designated siege zone.<br>
 * This condition is used to verify proximity to {@code Castle} or {@code Fort} areas.
 * @author Gigiikun
 */
public class ConditionSiegeZone extends Condition
{
	// conditional values
	public static final int COND_NOT_ZONE = 0x0001;
	public static final int COND_CAST_ATTACK = 0x0002;
	public static final int COND_CAST_DEFEND = 0x0004;
	public static final int COND_CAST_NEUTRAL = 0x0008;
	public static final int COND_FORT_ATTACK = 0x0010;
	public static final int COND_FORT_DEFEND = 0x0020;
	public static final int COND_FORT_NEUTRAL = 0x0040;
	
	private final int _value;
	private final boolean _self;
	
	/**
	 * Creates a new instance of {@code ConditionSiegeZone}.<br>
	 * This constructor initializes the zone condition with specific settings.
	 * @param value The integer value representing the siege zone type.
	 * @param self A boolean indicating if the condition applies to the source entity.
	 */
	public ConditionSiegeZone(int value, boolean self)
	{
		_value = value;
		_self = self;
	}
	
	/**
	 * Checks if the siege zone condition is met for a specific action.<br>
	 * It determines the target based on whether the condition is self-oriented.<br>
	 * The result depends on the {@code Castle} or {@code Fort} status of that target.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the condition is satisfied, {@code false} otherwise.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Creature target = _self ? effector : effected;
		final Castle castle = CastleManager.getInstance().getCastle(target);
		final Fort fort = FortManager.getInstance().getFort(target);
		if ((castle == null) && (fort == null))
		{
			return (_value & COND_NOT_ZONE) != 0;
		}
		
		if (castle != null)
		{
			return checkIfOk(target, castle, _value);
		}
		
		return checkIfOk(target, fort, _value);
	}
	
	/**
	 * Checks if a player meets the requirements for a specific siege zone condition.<br>
	 * This method validates the {@code creature} status and compares it against the {@code castle} state.<br>
	 * It evaluates the provided {@code value} bitmask to determine the allowed action.
	 * @param creature The {@code Creature} being checked.
	 * @param castle The {@code Castle} associated with the siege zone.
	 * @param value The integer bitmask representing the required condition.
	 * @return {@code true} if the conditions are met, otherwise {@code false}.
	 */
	public static boolean checkIfOk(Creature creature, Castle castle, int value)
	{
		if ((creature == null) || !creature.isPlayer())
		{
			return false;
		}
		
		final Player player = creature.asPlayer();
		if ((castle == null) || (castle.getResidenceId() <= 0))
		{
			if ((value & COND_NOT_ZONE) != 0)
			{
				return true;
			}
		}
		else if (!castle.getZone().isActive())
		{
			if ((value & COND_NOT_ZONE) != 0)
			{
				return true;
			}
		}
		else if (((value & COND_CAST_ATTACK) != 0) && player.isRegisteredOnThisSiegeField(castle.getResidenceId()) && (player.getSiegeState() == 1))
		{
			return true;
		}
		else if (((value & COND_CAST_DEFEND) != 0) && player.isRegisteredOnThisSiegeField(castle.getResidenceId()) && (player.getSiegeState() == 2))
		{
			return true;
		}
		else if (((value & COND_CAST_NEUTRAL) != 0) && (player.getSiegeState() == 0))
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if a player meets the requirements for a specific siege zone condition.<br>
	 * This method validates the {@code creature} status and compares it against the {@code fort} state.<br>
	 * It evaluates the provided {@code value} bitmask to determine the required action.
	 * @param creature The {@code Creature} being checked. Must be a valid {@code Player}.
	 * @param fort The {@code Fort} associated with the siege zone.
	 * @param value The integer bitmask representing the condition type.
	 * @return {@code true} if the conditions are met, otherwise {@code false}.
	 */
	public static boolean checkIfOk(Creature creature, Fort fort, int value)
	{
		if ((creature == null) || !creature.isPlayer())
		{
			return false;
		}
		
		final Player player = creature.asPlayer();
		if ((fort == null) || (fort.getResidenceId() <= 0))
		{
			if ((value & COND_NOT_ZONE) != 0)
			{
				return true;
			}
		}
		else if (!fort.getZone().isActive())
		{
			if ((value & COND_NOT_ZONE) != 0)
			{
				return true;
			}
		}
		else if (((value & COND_FORT_ATTACK) != 0) && player.isRegisteredOnThisSiegeField(fort.getResidenceId()) && (player.getSiegeState() == 1))
		{
			return true;
		}
		else if (((value & COND_FORT_DEFEND) != 0) && player.isRegisteredOnThisSiegeField(fort.getResidenceId()) && (player.getSiegeState() == 2))
		{
			return true;
		}
		else if (((value & COND_FORT_NEUTRAL) != 0) && (player.getSiegeState() == 0))
		{
			return true;
		}
		
		return false;
	}
}
