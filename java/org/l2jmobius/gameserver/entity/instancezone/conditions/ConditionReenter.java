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
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Defines the requirements for a player to re-enter an instance.<br>
 * This class checks if specific criteria are met before allowing access.
 * @author malyelfik
 */
public class ConditionReenter extends Condition
{
	/**
	 * Creates a new {@code ConditionReenter} object.<br>
	 * This constructor initializes the re-entry rules for an instance.<br>
	 * It sets the system message to notify players when they cannot re-enter yet.
	 * @param template The {@code InstanceTemplate} associated with this condition.
	 * @param parameters The {@code StatSet} containing specific configuration values.
	 * @param onlyLeader Determines if the rule applies only to the party leader.
	 * @param showMessageAndHtml Determines if a message and HTML should be displayed to the player.
	 */
	public ConditionReenter(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, onlyLeader, showMessageAndHtml);
		setSystemMessage(SystemMessageId.C1_CANNOT_ENTER_YET, (message, player) -> message.addString(player.getName()));
	}
	
	/**
	 * Checks if a player is allowed to re-enter an instance.<br>
	 * It compares the current system time against the instance's cooldown.
	 * @param player The {@code Player} attempting to enter.
	 * @param npc The {@code Npc} associated with the check.
	 * @return {@code true} if the cooldown has expired, {@code false} otherwise.
	 */
	@Override
	protected boolean test(Player player, Npc npc)
	{
		final int instanceId = getParameters().getInt("instanceId", getInstanceTemplate().getId());
		return System.currentTimeMillis() > InstanceManager.getInstance().getInstanceTime(player, instanceId);
	}
}
