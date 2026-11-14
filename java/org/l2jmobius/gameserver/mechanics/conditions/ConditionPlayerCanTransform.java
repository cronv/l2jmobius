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
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This class checks if a player is allowed to perform a transformation.<br>
 * It verifies the necessary requirements for the {@code Player} to change their form.
 * @author Adry_85
 */
public class ConditionPlayerCanTransform extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new {@code ConditionPlayerCanTransform} instance.<br>
	 * This condition checks if a player is allowed to transform based on a fixed state.
	 * @param value The boolean state that determines if transformation is permitted.
	 */
	public ConditionPlayerCanTransform(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if a player is allowed to perform a transformation.<br>
	 * This method validates the current state of the {@code effector}.<br>
	 * It returns {@code true} only if all conditions are met.
	 * @param effector The creature attempting the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the transformation is allowed, {@code false} otherwise.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		boolean canTransform = true;
		final Player player = effector.asPlayer();
		if ((player == null) || player.isAlikeDead() || player.isCursedWeaponEquipped())
		{
			canTransform = false;
		}
		else if (player.isSitting())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TRANSFORM_WHILE_SITTING);
			canTransform = false;
		}
		else if (player.isTransformed())
		{
			player.sendPacket(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN);
			canTransform = false;
		}
		else if (player.isInWater())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TRANSFORM_WHILE_IN_THE_WATER);
			canTransform = false;
		}
		else if (player.isFlyingMounted() || player.isMounted())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TRANSFORM_WHILE_RIDING_A_PET);
			canTransform = false;
		}
		else if (player.isRegisteredOnEvent())
		{
			player.sendMessage("You cannot transform while registered on an event.");
			canTransform = false;
		}
		
		return _value == canTransform;
	}
}
