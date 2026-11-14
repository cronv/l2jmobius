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

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.AbstractPlayerGroup;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the entity is the leader of a command channel.<br>
 * This condition is used to verify leadership status within specific instance zones.
 * @author malyelfik
 */
public class ConditionCommandChannelLeader extends Condition
{
	/**
	 * Creates a new condition to check if the player is the leader of a channel.<br>
	 * This constructor initializes the requirements for entering an instance zone.<br>
	 * It sets the specific system message for unauthorized requests.
	 * @param template The {@code InstanceTemplate} for the zone.
	 * @param parameters The {@code StatSet} containing required values.
	 * @param onlyLeader Determines if only the leader can perform the action.
	 * @param showMessageAndHtml Determines if messages should display HTML formatting.
	 */
	public ConditionCommandChannelLeader(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, true, showMessageAndHtml);
		setSystemMessage(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_MAKE_THE_REQUEST_TO_ENTER);
	}
	
	/**
	 * Checks if the {@code Player} is the leader of their command channel.<br>
	 * This method returns {@code true} if the player is a leader.<br>
	 * It returns {@code false} if the group is {@code null} or the player is not the leader.
	 * @param player The {@code Player} to check.
	 * @param npc The {@code Npc} involved in the action.
	 * @return {@code true} if the player is the leader, otherwise {@code false}.
	 */
	@Override
	public boolean test(Player player, Npc npc)
	{
		final AbstractPlayerGroup group = player.getCommandChannel();
		return (group != null) && group.isLeader(player);
	}
}
