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

/**
 * This class checks if a {@code Player} belongs to a specific pledge class.<br>
 * It is used as a condition to verify the player's current pledge status.
 * @author MrPoke
 */
public class ConditionPlayerPledgeClass extends Condition
{
	private final int _pledgeClass;
	
	/**
	 * Creates a new instance of {@code ConditionPlayerPledgeClass}.<br>
	 * This constructor sets the specific pledge class for the condition.
	 * @param pledgeClass The integer ID representing the pledge class.
	 */
	public ConditionPlayerPledgeClass(int pledgeClass)
	{
		_pledgeClass = pledgeClass;
	}
	
	/**
	 * Checks if the {@code effector} meets the required pledge class requirements.<br>
	 * This method verifies the player's clan status and pledge level.<br>
	 * It returns {@code true} if the conditions are met.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} involved in the action.
	 * @return {@code true} if the requirements are satisfied, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Player player = effector.asPlayer();
		if ((player == null) || (player.getClan() == null))
		{
			return false;
		}
		
		final boolean isClanLeader = player.isClanLeader();
		if ((_pledgeClass == -1) && !isClanLeader)
		{
			return false;
		}
		
		return isClanLeader || (player.getPledgeClass() >= _pledgeClass);
	}
}
