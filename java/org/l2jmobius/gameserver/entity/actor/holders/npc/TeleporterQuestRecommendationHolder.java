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
package org.l2jmobius.gameserver.entity.actor.holders.npc;

/**
 * This class holds data for recommending specific quests to players based on teleporter interactions.<br>
 * It helps the game server determine which quest suggestions should be displayed to an actor.
 * @author Mobius
 */
public class TeleporterQuestRecommendationHolder
{
	private final int _npcId;
	private final String _questName;
	private final int[] _conditions; // -1 = all conditions
	private final String _html;
	
	/**
	 * Creates a new instance of {@code TeleporterQuestRecommendationHolder}.<br>
	 * This constructor initializes the NPC ID, quest name, requirements, and HTML content.
	 * @param npcId The unique identifier for the NPC.
	 * @param questName The display name of the quest.
	 * @param conditions An array of integer requirements where -1 represents all conditions.
	 * @param html The HTML string to be displayed to the player.
	 */
	public TeleporterQuestRecommendationHolder(int npcId, String questName, int[] conditions, String html)
	{
		_npcId = npcId;
		_questName = questName;
		_conditions = conditions;
		_html = html;
	}
	
	/**
	 * Retrieves the unique identifier for the NPC.<br>
	 * This value is used to identify which NPC provides the quest recommendation.
	 * @return The {@code int} ID of the NPC.
	 */
	public int getNpcId()
	{
		return _npcId;
	}
	
	/**
	 * Retrieves the name of the quest associated with this recommendation.<br>
	 * This value is stored in the {@code _questName} field.
	 * @return The name of the quest as a {@code String}.
	 */
	public String getQuestName()
	{
		return _questName;
	}
	
	/**
	 * Retrieves the list of conditions for this recommendation.<br>
	 * Returns an array where {@code -1} represents all conditions.
	 * @return an {@code int[]} containing the condition IDs.
	 */
	public int[] getConditions()
	{
		return _conditions;
	}
	
	/**
	 * Retrieves the HTML content for this recommendation.<br>
	 * This string is used to display information in the game interface.
	 * @return The {@code String} containing the HTML markup.
	 */
	public String getHtml()
	{
		return _html;
	}
}
