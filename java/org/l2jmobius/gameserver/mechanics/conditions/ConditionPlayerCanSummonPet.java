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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.SummonInfoTable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This class checks if a {@code Player} is allowed to summon a pet.<br>
 * It validates the requirements for summoning based on the current game state.
 * @author Zoey76
 */
public class ConditionPlayerCanSummonPet extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new condition to check if a player can summon a pet.<br>
	 * This constructor sets the required state for the condition.
	 * @param value The boolean value used to determine if summoning is allowed.
	 */
	public ConditionPlayerCanSummonPet(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if a player is allowed to summon a pet.<br>
	 * This method validates the current state of the {@code effector}.<br>
	 * It returns {@code true} only if all conditions are met.
	 * @param effector The {@code Creature} attempting to perform the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} associated with the action.
	 * @return {@code true} if the player can summon a pet, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Player player = effector.asPlayer();
		if (player == null)
		{
			return false;
		}
		
		boolean canSummon = true;
		if (PlayerConfig.RESTORE_PET_ON_RECONNECT && SummonInfoTable.getInstance().getPets().containsKey(player.getObjectId()))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_MULTIPLE_PETS_AT_THE_SAME_TIME);
			canSummon = false;
		}
		else if (player.hasPet())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_MULTIPLE_PETS_AT_THE_SAME_TIME);
			canSummon = false;
		}
		else if (player.isFlyingMounted() || player.isMounted() || player.getObserverMode().isActive() || player.isTeleporting())
		{
			canSummon = false;
		}
		
		return (_value == canSummon);
	}
}
