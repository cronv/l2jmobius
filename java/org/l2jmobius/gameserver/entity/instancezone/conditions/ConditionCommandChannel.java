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
 * This class represents a condition that checks the current command channel of an actor.<br>
 * It is used to restrict access or actions based on which channel a {@code Player} or {@code Npc} is currently using.
 * @author malyelfik
 */
public class ConditionCommandChannel extends Condition
{
	/**
	 * Creates a new {@code ConditionCommandChannel} instance.<br>
	 * This condition checks if a player is associated with the correct command channel.<br>
	 * It sets a specific system message for failed attempts.
	 * @param template The {@code InstanceTemplate} for this condition.
	 * @param parameters The {@code StatSet} containing required values.
	 * @param onlyLeader Determines if only the leader can pass the check.
	 * @param showMessageAndHtml Determines if the error message and HTML should be displayed.
	 */
	public ConditionCommandChannel(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, true, showMessageAndHtml);
		setSystemMessage(SystemMessageId.YOU_CANNOT_ENTER_BECAUSE_YOU_ARE_NOT_ASSOCIATED_WITH_THE_CURRENT_COMMAND_CHANNEL);
	}
	
	/**
	 * Checks if the {@code Player} is currently in a command channel.<br>
	 * This method ignores the {@code Npc} parameter.
	 * @param player The {@code Player} to check.
	 * @param npc The {@code Npc} involved in the action.
	 * @return {@code true} if the player is in a command channel, {@code false} otherwise.
	 */
	@Override
	public boolean test(Player player, Npc npc)
	{
		return player.isInCommandChannel();
	}
}
