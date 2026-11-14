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
package ai.areas.AteliaRefinery;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * Defines the AI behavior and logic for the {@code AteliaRefinery} area.<br>
 * This class handles specific script interactions within this location.
 * @author NviX
 */
public class AteliaRefinery extends Script
{
	// NPC
	private static final int ATELIA_REFINERY_TELEPORT_DEVICE = 34441;
	
	// Teleport Locations
	private static final Location[] TELE_LOCATIONS =
	{
		new Location(-251728, 178576, -8928), // Atelia Outlet
		new Location(-59493, 52620, -8610), // Entrance
		new Location(-56096, 49688, -8729), // First Area
		new Location(-56160, 45406, -8847), // Second Area
		new Location(-56140, 41067, -8965), // Third Area
		new Location(-51716, 60243, -3344), // Exit
	};
	
	// Special Mobs
	private static final int HARKE = 24161;
	private static final int ERGALION = 24162;
	private static final int SPIRA = 24163;
	
	// Mobs
	private static final int[] MOBS =
	{
		24144, // Death Rogue
		24145, // Death Shooter
		24146, // Death Warrior
		24147, // Death Sorcerer
		24148, // Death Pondus
		24149, // Devil Nightmare
		24150, // Devil Warrior
		24151, // Devil Guardian
		24152, // Devil Sinist
		24153, // Devil Varos
		24154, // Demonic Wizard
		24155, // Demonic Warrior
		24156, // Demonic Archer
		24157, // Demonic Keras
		24158, // Demonic Weiss
		24159, // Atelia Yuyurina
		24160 // Atelia Popobena
	};
	
	/**
	 * Private constructor for the {@code AteliaRefinery} class.<br>
	 * This constructor initializes the script by registering required IDs.<br>
	 * It sets up talk and kill events for specific NPCs and mobs.
	 */
	private AteliaRefinery()
	{
		addTalkId(ATELIA_REFINERY_TELEPORT_DEVICE);
		addFirstTalkId(ATELIA_REFINERY_TELEPORT_DEVICE);
		addKillId(MOBS);
	}
	
	/**
	 * Handles specific events for the refinery area.<br>
	 * It teleports the {@code Player} to different locations based on the provided event name.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} involved in the event.
	 * @param player The {@code Player} who triggered the event.
	 * @return A {@code null} value as no HTML text is returned.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "outlet":
			{
				player.teleToLocation(TELE_LOCATIONS[0]);
				htmltext = "34441-01.html";
				break;
			}
			case "entrance":
			{
				player.teleToLocation(TELE_LOCATIONS[1]);
				htmltext = "34441-01.html";
				break;
			}
			case "first_area":
			{
				player.teleToLocation(TELE_LOCATIONS[2]);
				htmltext = "34441-01.html";
				break;
			}
			case "second_area":
			{
				player.teleToLocation(TELE_LOCATIONS[3]);
				htmltext = "34441-01.html";
				break;
			}
			case "third_area":
			{
				player.teleToLocation(TELE_LOCATIONS[4]);
				htmltext = "34441-01.html";
				break;
			}
			case "exit":
			{
				player.teleToLocation(TELE_LOCATIONS[5]);
				htmltext = "34441-01.html";
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles special logic for rewards or spawns based on the kill event.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who performed the kill.
	 * @param isSummon Indicates if the killed entity was a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		int chance = 1;
		if (getRandom(10000) < chance)
		{
			addSpawn(HARKE, npc, false, 300000);
		}
		else if (getRandom(10000) < chance)
		{
			addSpawn(ERGALION, npc, false, 300000);
		}
		else if (getRandom(100000) < chance)
		{
			addSpawn(SPIRA, npc, false, 300000);
		}
	}
	
	/**
	 * Handles the dialogue when a {@code Player} speaks to an {@code Npc} for the first time.<br>
	 * It returns the filename of the specific NPC's dialogue script.
	 * @param npc The {@code Npc} that is being spoken to.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		Location npcLoc = npc.getLocation();
		// final QuestState qs = player.getQuestState(Q10890_SaviorsPathHallOfEtina.class.getSimpleName());
		// if (((qs != null) && qs.isCompleted()) && ((npcLoc.getX() == -59891) && (npcLoc.getY() == 52625)))
		// {
		// htmltext = "34441-03.html";
		// }
		// else if (((qs != null) && qs.isCompleted()))
		// {
		// htmltext = "34441-00.html";
		// }
		// else
		if ((npcLoc.getX() == -59891) && (npcLoc.getY() == 52625))
		{
			htmltext = "34441-02.html";
		}
		else
		{
			htmltext = "34441.html";
		}
		
		return htmltext;
	}
	
	/**
	 * This is the entry point for the {@code AteliaRefinery} class.<br>
	 * It initializes a new instance of the script to start the logic.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new AteliaRefinery();
	}
}
