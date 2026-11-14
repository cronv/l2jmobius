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
package ai.areas.Gracia.SeedOfAnnihilation.Nemo;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;

import ai.areas.Gracia.SeedOfAnnihilation.Maguen;

/**
 * This class defines the artificial intelligence behavior for the {@code Nemo} NPC.<br>
 * It handles the logic and actions performed by this entity within its designated area.
 * @author St3eT
 */
public class Nemo extends Script
{
	// NPCs
	private static final int NEMO = 32735; // Nemo
	private static final int MAGUEN = 18839; // Wild Maguen
	
	// Items
	private static final int COLLECTOR = 15487; // Maguen Plasma Collector
	
	// Misc
	private static final int MAXIMUM_MAGUEN = 18; // Maximum maguens in one time
	
	/**
	 * Initializes the {@code Nemo} script.<br>
	 * Registers the starting NPC and talk IDs for this entity.
	 */
	public Nemo()
	{
		addStartNpc(NEMO);
		addFirstTalkId(NEMO);
		addTalkId(NEMO);
	}
	
	/**
	 * Handles specific events for the Nemo NPC.<br>
	 * This method processes logic for dialogue, item rewards, and summoning maguens.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} interacting with the NPC.
	 * @return The HTML text to display or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32735-01.html":
			{
				htmltext = event;
				break;
			}
			case "giveCollector":
			{
				if (hasQuestItems(player, COLLECTOR))
				{
					htmltext = "32735-03.html";
				}
				else if (!player.isInventoryUnder80(false))
				{
					htmltext = "32735-04.html";
				}
				else
				{
					htmltext = "32735-02.html";
					giveItems(player, COLLECTOR, 1);
				}
				break;
			}
			case "summonMaguen":
			{
				final int scriptValue = npc.getScriptValue();
				if ((player.getVariables().getInt("TEST_MAGUEN", 0) == 0) && (scriptValue < MAXIMUM_MAGUEN))
				{
					final Npc maguen = addSpawn(MAGUEN, npc.getLocation(), true, 60000, true);
					maguen.getVariables().set("SUMMON_PLAYER", player);
					maguen.getVariables().set("SPAWNED_NPC", npc);
					maguen.getVariables().set("TEST_MAGUEN", 1);
					player.getVariables().set("TEST_MAGUEN", 1);
					maguen.setTitle(player.getName());
					maguen.setRunning();
					maguen.getAI().setIntentionFollow(player);
					maguen.broadcastStatusUpdate();
					showOnScreenMsg(player, NpcStringId.MAGUEN, 2, 4000);
					maguenAi().startQuestTimer("DIST_CHECK_TIMER", 1000, maguen, player);
					npc.setScriptValue(scriptValue + 1);
					htmltext = "32735-05.html";
				}
				else
				{
					htmltext = "32735-06.html";
				}
				break;
			}
			case "DECREASE_COUNT":
			{
				final Npc spawnedNpc = npc.getVariables().getObject("SPAWNED_NPC", Npc.class);
				if ((spawnedNpc != null) && (spawnedNpc.getScriptValue() > 0))
				{
					player.getVariables().remove("TEST_MAGUEN");
					spawnedNpc.setScriptValue(spawnedNpc.getScriptValue() - 1);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Retrieves the quest script for the Maguen AI.<br>
	 * This method fetches the {@code Quest} object from the {@code ScriptManager}.
	 * @return The {@code Quest} instance associated with the Maguen class.
	 */
	private Quest maguenAi()
	{
		return ScriptManager.getInstance().getScript(Maguen.class.getSimpleName());
	}
	
	/**
	 * Handles the initial interaction when a {@code Player} speaks to an {@code Npc}.<br>
	 * This method determines which dialogue file to display.
	 * @param npc The {@code Npc} being spoken to.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A string representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code Nemo} class.<br>
	 * It initializes a new instance of the {@code Nemo} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Nemo();
	}
}
