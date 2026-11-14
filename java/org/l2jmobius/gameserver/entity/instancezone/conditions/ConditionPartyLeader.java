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
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the entity is the leader of a party.<br>
 * This condition is used to verify leadership status within an instance zone.
 * @author malyelfik
 */
public class ConditionPartyLeader extends Condition
{
	/**
	 * Creates a new condition to check if the player is a party leader.<br>
	 * This constructor sets up specific requirements for entering an instance.<br>
	 * It automatically assigns a system message for restricted access.
	 * @param template The {@code InstanceTemplate} associated with this condition.
	 * @param parameters The {@code StatSet} containing the required values.
	 * @param onlyLeader Set to {@code true} if only the leader can perform the action.
	 * @param showMessageAndHtml Set to {@code true} to display a message and HTML formatting.
	 */
	public ConditionPartyLeader(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, true, showMessageAndHtml);
		setSystemMessage(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_MAKE_THE_REQUEST_TO_ENTER);
	}
	
	/**
	 * Checks if the {@code Player} is the leader of their party.<br>
	 * This method returns {@code true} only if the player belongs to a party and holds the leader role.<br>
	 * The {@code Npc} parameter is currently not used in this check.
	 * @param player The {@code Player} to check.
	 * @param npc The {@code Npc} involved in the condition.
	 * @return {@code true} if the player is a party leader, otherwise {@code false}.
	 */
	@Override
	public boolean test(Player player, Npc npc)
	{
		return player.isInParty() && player.getParty().isLeader(player);
	}
}
