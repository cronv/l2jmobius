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
 * This class checks if an entity is within a specific distance from a target.<br>
 * It is used to evaluate spatial requirements for {@code InstanceTemplate} conditions.
 * @author malyelfik
 */
public class ConditionDistance extends Condition
{
	/**
	 * Creates a new {@code ConditionDistance} instance.<br>
	 * This constructor initializes the distance check requirements.<br>
	 * It sets a default system message for invalid locations.
	 * @param template The {@code InstanceTemplate} associated with this condition.
	 * @param parameters The {@code StatSet} containing specific configuration values.
	 * @param onlyLeader Determines if the condition applies only to the party leader.
	 * @param showMessageAndHtml Determines if a message and HTML should be displayed.
	 */
	public ConditionDistance(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, onlyLeader, showMessageAndHtml);
		setSystemMessage(SystemMessageId.C1_IS_TOO_FAR_FROM_THE_INSTANCE_ZONE_ENTRANCE, (message, player) -> message.addString(player.getName()));
	}
	
	/**
	 * Checks if a {@code Player} is within a specific distance of an {@code Npc}.<br>
	 * The distance value is retrieved from the configuration parameters.
	 * @param player The {@code Player} to check.
	 * @param npc The {@code Npc} used as the center point.
	 * @return {@code true} if the player is inside the radius, otherwise {@code false}.
	 */
	@Override
	public boolean test(Player player, Npc npc)
	{
		final int distance = getParameters().getInt("distance", 1000);
		return player.isInsideRadius3D(npc, distance);
	}
}
