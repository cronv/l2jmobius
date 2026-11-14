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
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the player is not currently in a party.<br>
 * This {@code ConditionNoParty} class ensures that an action can only be performed by solo players.
 * @author St3eT
 */
public class ConditionNoParty extends Condition
{
	/**
	 * Creates a new {@code ConditionNoParty} instance.<br>
	 * This condition checks if a player is not in a party.<br>
	 * It uses the provided template and parameters to define the logic.
	 * @param template The {@code InstanceTemplate} for this condition.
	 * @param parameters The {@code StatSet} containing specific values.
	 * @param onlyLeader Determines if the check applies only to the party leader.
	 * @param showMessageAndHtml Determines if a message and HTML should be displayed.
	 */
	public ConditionNoParty(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, true, showMessageAndHtml);
	}
	
	/**
	 * Checks if the {@code Player} is currently in a party.<br>
	 * This method returns {@code true} if the player is not in any party.<br>
	 * It ignores the {@code Npc} parameter during the check.
	 * @param player The {@code Player} to check.
	 * @param npc The {@code Npc} involved in the interaction.
	 * @return {@code true} if the player is not in a party, otherwise {@code false}.
	 */
	@Override
	public boolean test(Player player, Npc npc)
	{
		return !player.isInParty();
	}
}
