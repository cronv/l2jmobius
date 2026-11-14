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
 * This class represents a condition related to an instance party.<br>
 * It is used to check if specific requirements are met regarding the party status within an {@code InstanceTemplate}.
 * @author malyelfik
 */
public class ConditionParty extends Condition
{
	/**
	 * Creates a new {@code ConditionParty} instance.<br>
	 * This condition checks if a player is in a party to enter an instance.<br>
	 * It sets the default system message for players who are not in a party.
	 * @param template The {@code InstanceTemplate} associated with this condition.
	 * @param parameters The {@code StatSet} containing specific configuration values.
	 * @param onlyLeader Determines if only the party leader can trigger the condition.
	 * @param showMessageAndHtml Determines if the system message and HTML formatting should be displayed.
	 */
	public ConditionParty(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, true, showMessageAndHtml);
		setSystemMessage(SystemMessageId.YOU_ARE_NOT_IN_A_PARTY_SO_YOU_CANNOT_ENTER);
	}
	
	/**
	 * Checks if the {@code Player} is currently in a party.<br>
	 * This method returns {@code true} if they are, and {@code false} otherwise.
	 * @param player The {@code Player} to check.
	 * @param npc The {@code Npc} involved in the condition.
	 * @return {@code true} if the player is in a party, {@code false} otherwise.
	 */
	@Override
	public boolean test(Player player, Npc npc)
	{
		return player.isInParty();
	}
}
