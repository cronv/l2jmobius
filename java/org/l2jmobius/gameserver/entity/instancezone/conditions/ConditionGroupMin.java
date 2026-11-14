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
 * This class checks if the minimum required number of players is present to enter an instance.<br>
 * It ensures that a group meets the {@code minSize} requirement before allowing entry.
 * @author malyelfik
 */
public class ConditionGroupMin extends Condition
{
	/**
	 * Creates a new {@code ConditionGroupMin} instance.<br>
	 * This condition checks if a group meets the minimum size requirement to enter an instance.
	 * @param template The {@code InstanceTemplate} for the zone.
	 * @param parameters The {@code StatSet} containing configuration values.
	 * @param onlyLeader Determines if this check applies only to the group leader.
	 * @param showMessageAndHtml Determines if a message and HTML should be displayed.
	 */
	public ConditionGroupMin(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, true, showMessageAndHtml);
		setSystemMessage(SystemMessageId.YOU_MUST_HAVE_A_MINIMUM_OF_S1_PEOPLE_TO_ENTER_THIS_INSTANCE_ZONE, (msg, _) -> msg.addInt(getLimit()));
	}
	
	/**
	 * Checks if the current group size meets the minimum requirement.<br>
	 * This method compares the size of the {@code List} against a predefined limit.
	 * @param player The {@code Player} who is attempting to perform the action.
	 * @param npc The {@code Npc} involved in the interaction.
	 * @param group The {@code List} of players currently in the group.
	 * @return {@code true} if the group size is greater than or equal to the limit, otherwise {@code false}.
	 */
	@Override
	protected boolean test(Player player, Npc npc, List<Player> group)
	{
		return group.size() >= getLimit();
	}
	
	/**
	 * Retrieves the maximum limit for the group size.<br>
	 * This value is stored in the {@code parameters} set.
	 * @return The integer value of the limit.
	 */
	public int getLimit()
	{
		return getParameters().getInt("limit");
	}
}
