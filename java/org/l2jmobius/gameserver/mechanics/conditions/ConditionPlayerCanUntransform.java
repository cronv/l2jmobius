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
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This class checks if a {@code Player} is currently in a state that allows them to untransform.<br>
 * It serves as a condition for mechanics requiring the player to be able to revert their current form.
 * @author Adry_85
 */
public class ConditionPlayerCanUntransform extends Condition
{
	private final boolean _value;
	
	/**
	 * This condition checks if a player is allowed to untransform.<br>
	 * It stores the required state as a boolean value.
	 * @param value The required state for the transformation check.
	 */
	public ConditionPlayerCanUntransform(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the {@code effector} is allowed to untransform.<br>
	 * This method validates the player's state and current location.<br>
	 * It returns {@code true} if all conditions are met.
	 * @param effector The creature attempting the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the player can untransform, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		boolean canUntransform = true;
		final Player player = effector.asPlayer();
		if (player == null)
		{
			canUntransform = false;
		}
		else if (player.isAlikeDead() || player.isCursedWeaponEquipped())
		{
			canUntransform = false;
		}
		else if (player.isFlyingMounted() && !player.isInsideZone(ZoneId.LANDING))
		{
			player.sendPacket(SystemMessageId.YOU_ARE_TOO_HIGH_TO_PERFORM_THIS_ACTION_PLEASE_LOWER_YOUR_ALTITUDE_AND_TRY_AGAIN); // TODO: check if message is retail like.
			canUntransform = false;
		}
		
		return _value == canUntransform;
	}
}
