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
package org.l2jmobius.gameserver.entity.instancezone.conditions;

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class checks if the current number of players in a group exceeds the maximum allowed limit.<br>
 * It is used to validate instance entry conditions based on group size.
 * @author malyelfik
 */
public class ConditionGroupMax extends Condition
{
	/**
	 * Creates a new {@code ConditionGroupMax} instance.<br>
	 * This condition checks if a group has reached its maximum size limit.<br>
	 * It sets the system message for when the limit is exceeded.
	 * @param template The {@code InstanceTemplate} associated with this condition.
	 * @param parameters The {@code StatSet} containing the configuration values.
	 * @param onlyLeader Determines if the check applies only to the group leader.
	 * @param showMessageAndHtml Determines if a message and HTML should be displayed.
	 */
	public ConditionGroupMax(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, true, showMessageAndHtml);
		setSystemMessage(SystemMessageId.YOU_CANNOT_ENTER_DUE_TO_THE_PARTY_HAVING_EXCEEDED_THE_LIMIT);
	}
	
	/**
	 * Checks if the current group size is within the allowed limit.<br>
	 * This method validates the number of players in a {@code List}.<br>
	 * It returns {@code true} if the count is less than or equal to the limit.
	 * @param player The {@code Player} who triggered the check.
	 * @param npc The {@code Npc} associated with the instance.
	 * @param group The {@code List} of players currently in the group.
	 * @return {@code true} if the group size is valid, otherwise {@code false}.
	 */
	@Override
	protected boolean test(Player player, Npc npc, List<Player> group)
	{
		return group.size() <= getLimit();
	}
	
	/**
	 * Retrieves the maximum size limit for a group.<br>
	 * This value is stored in the {@code parameters} set.
	 * @return The integer limit value.
	 */
	public int getLimit()
	{
		return getParameters().getInt("limit");
	}
}
