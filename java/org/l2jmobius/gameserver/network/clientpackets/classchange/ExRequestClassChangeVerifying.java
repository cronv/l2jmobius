/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.network.clientpackets.classchange;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExClassChangeSetAlarm;

/**
 * Handles the client request to verify a character's class change.<br>
 * This packet ensures that all requirements for changing a {@code PlayerClass} are met.<br>
 * It validates the current state of the player and any associated quest progress.
 * @author Mobius
 */
public class ExRequestClassChangeVerifying extends ClientPacket
{
	// @formatter:off
	private static final int[] FIRST_CLASS_QUESTS = {10007, 10207, 10307, 10407, 11007}; // Path of Destiny - Beginning
	private static final int[] SECOND_CLASS_QUESTS = {10015, 10215, 10315, 10415, 11015}; // Path of Destiny - Proving
	private static final int[] THIRD_CLASS_QUESTS = {10024, 10124, 10224, 10324, 10424, 11024}; // Path of Destiny - Conviction
	private static final int[] FOURTH_CLASS_QUESTS = {10036, 10236, 10336, 10436, 11036}; // Path of Destiny - Overcome
	// @formatter:on
	
	private int _classId;
	
	@Override
	protected void readImpl()
	{
		_classId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_classId != player.getPlayerClass().getId())
		{
			return;
		}
		
		if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
		{
			return;
		}
		
		if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			if (!fourthClassCheck(player))
			{
				return;
			}
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
		{
			if (!thirdClassCheck(player))
			{
				return;
			}
		}
		else if (player.isInCategory(CategoryType.SECOND_CLASS_GROUP))
		{
			if (!secondClassCheck(player))
			{
				return;
			}
		}
		else if (player.isInCategory(CategoryType.FIRST_CLASS_GROUP))
		{
			if (!firstClassCheck(player))
			{
				return;
			}
		}
		
		player.sendPacket(ExClassChangeSetAlarm.STATIC_PACKET);
	}
	
	/**
	 * Verifies if the {@code Player} has completed the required tutorial.<br>
	 * This check is skipped if tutorials are disabled in the configuration.<br>
	 * It checks for the completion of a specific quest state.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the requirements are met, {@code false} otherwise.
	 */
	private boolean firstClassCheck(Player player)
	{
		if (PlayerConfig.DISABLE_TUTORIAL)
		{
			return true;
		}
		
		return isAnyQuestStarted(player, FIRST_CLASS_QUESTS);
	}
	
	/**
	 * Performs the second validation check for a class change.<br>
	 * It checks if the player has completed the required quest.<br>
	 * This check is skipped if tutorials are disabled in {@code PlayerConfig}.
	 * @param player The {@code Player} object to validate.
	 * @return {@code true} if the requirements are met, {@code false} otherwise.
	 */
	private boolean secondClassCheck(Player player)
	{
		if (PlayerConfig.DISABLE_TUTORIAL)
		{
			return true;
		}
		
		return isAnyQuestStarted(player, SECOND_CLASS_QUESTS);
	}
	
	/**
	 * Verifies if the {@code Player} has completed a specific quest.<br>
	 * This check is skipped if tutorials are disabled in {@code PlayerConfig}.<br>
	 * It checks for the completion of the "Q11026_PathOfDestinyConviction" quest.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the requirements are met, otherwise {@code false}.
	 */
	private boolean thirdClassCheck(Player player)
	{
		if (PlayerConfig.DISABLE_TUTORIAL)
		{
			return true;
		}
		
		return isAnyQuestStarted(player, THIRD_CLASS_QUESTS);
	}
	
	/**
	 * Verifies if the {@code Player} has completed a specific quest.<br>
	 * This check is skipped if tutorials are disabled in {@code PlayerConfig}.<br>
	 * It checks for the completion of the "Q11027_PathOfDestinyOvercome" quest.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the requirements are met, otherwise {@code false}.
	 */
	private boolean fourthClassCheck(Player player)
	{
		if (PlayerConfig.DISABLE_TUTORIAL)
		{
			return true;
		}
		
		return isAnyQuestStarted(player, FOURTH_CLASS_QUESTS);
	}
	
	/**
	 * Checks if the {@code player} has started any of the provided quests.<br>
	 * It iterates through the array of quest IDs and verifies their status.
	 * @param player The {@code Player} object to check.
	 * @param questIds An array of integers representing the unique quest IDs.
	 * @return {@code true} if at least one quest is started, otherwise {@code false}.
	 */
	private boolean isAnyQuestStarted(Player player, int[] questIds)
	{
		for (int questId : questIds)
		{
			final Quest quest = ScriptManager.getInstance().getQuest(questId);
			if (quest != null)
			{
				final QuestState qs = player.getQuestState(quest.getName());
				if ((qs != null) && qs.isStarted())
				{
					return true;
				}
			}
		}
		
		return false;
	}
}
