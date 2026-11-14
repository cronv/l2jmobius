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
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class defines a condition for instance zones based on quest progress.<br>
 * It checks if a {@code Player} has completed or reached a specific state in a {@code Quest}.
 * @author malyelfik
 */
public class ConditionQuest extends Condition
{
	/**
	 * Creates a new quest condition for an instance.<br>
	 * This constructor initializes the requirements needed to enter or progress in an instance.<br>
	 * It sets a default system message if the player does not meet the requirements.
	 * @param template The {@code InstanceTemplate} associated with this condition.
	 * @param parameters The {@code StatSet} containing specific values for the quest.
	 * @param onlyLeader Determines if the condition applies only to the party leader.
	 * @param showMessageAndHtml Determines if a message and HTML should be displayed to the player.
	 */
	public ConditionQuest(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, onlyLeader, showMessageAndHtml);
		
		// Set message
		setSystemMessage(SystemMessageId.C1_DOES_NOT_MEET_QUEST_REQUIREMENTS_AND_CANNOT_ENTER, (message, player) -> message.addString(player.getName()));
	}
	
	/**
	 * Checks if a {@code Player} meets the required quest conditions.<br>
	 * It verifies that the player has a specific progress state in a quest.
	 * @param player The {@code Player} to check.
	 * @param npc The {@code Npc} associated with this condition.
	 * @return {@code true} if the conditions are met, otherwise {@code false}.
	 */
	@Override
	protected boolean test(Player player, Npc npc)
	{
		final int id = getParameters().getInt("id");
		final Quest q = ScriptManager.getInstance().getQuest(id);
		if (q == null)
		{
			return false;
		}
		
		final QuestState qs = player.getQuestState(q.getName());
		if (qs == null)
		{
			return false;
		}
		
		final int cond = getParameters().getInt("cond", -1);
		return (cond == -1) || qs.isCond(cond);
	}
}
