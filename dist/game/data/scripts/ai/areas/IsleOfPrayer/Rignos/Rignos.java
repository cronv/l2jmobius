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
package ai.areas.IsleOfPrayer.Rignos;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class defines the artificial intelligence behavior for the {@code Rignos} NPC.<br>
 * It handles the logic for how this entity interacts with players and other actors in its area.
 * @author St3eT
 */
public class Rignos extends Script
{
	// NPC
	private static final int RIGNOS = 32349; // Rignos
	
	// Item
	private static final int STAMP = 10013; // Race Stamp
	private static final int KEY = 9694; // Secret Key
	
	// Skill
	private static final SkillHolder TIMER = new SkillHolder(5239, 5); // Event Timer
	// Misc
	private static final int MIN_LV = 78;
	
	/**
	 * Private constructor for the {@code Rignos} class.<br>
	 * This constructor initializes the NPC IDs and talk IDs.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private Rignos()
	{
		addStartNpc(RIGNOS);
		addTalkId(RIGNOS);
		addFirstTalkId(RIGNOS);
	}
	
	/**
	 * Handles specific events for the Rignos NPC.<br>
	 * This method processes logic for starting races and exchanging items.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return A {@code String} representing the result of the event or the original event name.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "32349-03.html":
			{
				return event;
			}
			case "startRace":
			{
				if (npc.isScriptValue(0))
				{
					npc.setScriptValue(1);
					startQuestTimer("TIME_OUT", 1800000, npc, null);
					
					final Skill skill = TIMER.getSkill();
					skill.applyEffects(player, player);
					final Summon pet = player.getPet();
					if (pet != null)
					{
						skill.applyEffects(pet, pet);
					}
					
					player.getServitors().values().forEach(s -> skill.applyEffects(s, s));
					if (hasQuestItems(player, STAMP))
					{
						takeItems(player, STAMP, -1);
					}
				}
				break;
			}
			case "exchange":
			{
				if (getQuestItemsCount(player, STAMP) >= 4)
				{
					giveItems(player, KEY, 3);
					takeItems(player, STAMP, -1);
				}
				break;
			}
			case "TIME_OUT":
			{
				npc.setScriptValue(0);
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Determines the correct HTML text to display when a player talks to this NPC for the first time.<br>
	 * It checks the player's level and quest items to decide which dialogue file to load.
	 * @param npc The {@code Npc} instance that is being interacted with.
	 * @param player The {@code Player} instance who initiated the conversation.
	 * @return A {@code String} representing the name of the HTML file to be displayed.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = (npc.isScriptValue(0) && (player.getLevel() >= MIN_LV)) ? "32349.html" : "32349-02.html";
		if (getQuestItemsCount(player, STAMP) >= 4)
		{
			htmltext = "32349-01.html";
		}
		
		return htmltext;
	}
	
	/**
	 * This is the main entry point for the {@code Rignos} class.<br>
	 * It initializes a new instance of the {@code Rignos} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Rignos();
	}
}
