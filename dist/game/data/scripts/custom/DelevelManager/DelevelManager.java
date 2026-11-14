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
package custom.DelevelManager;

import org.l2jmobius.gameserver.config.custom.DelevelManagerConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * Manages the logic for reducing a player's level within the game.<br>
 * This {@code DelevelManager} handles the necessary calculations and state changes when a character is deleveled.
 * @author Mobius
 */
public class DelevelManager extends Script
{
	/**
	 * Private constructor for the {@code DelevelManager} class.<br>
	 * This constructor initializes the required NPC IDs and talk IDs from the configuration.<br>
	 * It prevents other classes from creating new instances of this manager.
	 */
	private DelevelManager()
	{
		addStartNpc(DelevelManagerConfig.DELEVEL_MANAGER_NPCID);
		addTalkId(DelevelManagerConfig.DELEVEL_MANAGER_NPCID);
		addFirstTalkId(DelevelManagerConfig.DELEVEL_MANAGER_NPCID);
	}
	
	/**
	 * Handles specific events triggered by the {@code DelevelManager}.<br>
	 * It checks if the player meets the requirements to decrease their level.<br>
	 * The method processes the "delevel" event and returns a response message.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} that initiated the event.
	 * @param player The {@code Player} who is interacting with the system.
	 * @return A {@code String} containing the HTML file path or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!DelevelManagerConfig.DELEVEL_MANAGER_ENABLED)
		{
			return null;
		}
		
		switch (event)
		{
			case "delevel":
			{
				final int playerLevel = player.getLevel();
				if (playerLevel <= DelevelManagerConfig.DELEVEL_MANAGER_MINIMUM_DELEVEL)
				{
					return "1002000-2.htm";
				}
				
				if (getQuestItemsCount(player, DelevelManagerConfig.DELEVEL_MANAGER_ITEMID) >= DelevelManagerConfig.DELEVEL_MANAGER_ITEMCOUNT)
				{
					takeItems(player, DelevelManagerConfig.DELEVEL_MANAGER_ITEMID, DelevelManagerConfig.DELEVEL_MANAGER_ITEMCOUNT);
					player.getStat().removeExpAndSp((player.getExp() - ExperienceData.getInstance().getExpForLevel(playerLevel - 1)), 0);
					player.broadcastUserInfo();
					return "1002000.htm";
				}
				
				return "1002000-1.htm";
			}
		}
		
		return null;
	}
	
	/**
	 * Handles the initial interaction between a {@code Player} and an {@code Npc}.<br>
	 * This method determines which dialog file to show first.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return The name of the HTML file to display.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "1002000.htm";
	}
	
	/**
	 * This is the entry point for the {@code DelevelManager} class.<br>
	 * It initializes a new instance of the manager to start the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new DelevelManager();
	}
}
