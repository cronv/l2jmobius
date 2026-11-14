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
package ai.areas.StakatoNest.StakatoNestTeleporter;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * Handles the teleportation logic for the {@code StakatoNest} area.<br>
 * This script manages how NPCs or players are moved between specific locations.
 * @author Charus, Mobius
 */
public class StakatoNestTeleporter extends Script
{
	// Locations
	private static final Location[] LOCS =
	{
		new Location(80456, -52322, -5640),
		new Location(88718, -46214, -4640),
		new Location(87464, -54221, -5120),
		new Location(80848, -49426, -5128),
		new Location(87682, -43291, -4128)
	};
	
	// NPC
	private static final int KINTAIJIN = 32640;
	private static final int PARTY_TELEPORT_RANGE = 2000;
	
	/**
	 * Private constructor for the {@code StakatoNestTeleporter} class.<br>
	 * This constructor initializes the required NPC and talk IDs.<br>
	 * It is not intended to be called from outside this class.
	 */
	private StakatoNestTeleporter()
	{
		addStartNpc(KINTAIJIN);
		addTalkId(KINTAIJIN);
	}
	
	/**
	 * Handles specific events for the teleporter.<br>
	 * It moves the {@code Player} and their nearby party members to a location based on an index.
	 * @param event The string index of the destination location.
	 * @param npc The {@code Npc} triggering the event.
	 * @param player The {@code Player} who triggered the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final int index = Integer.parseInt(event) - 1;
		if (LOCS.length > index)
		{
			final Location loc = LOCS[index];
			final Party party = player.getParty();
			if (party != null)
			{
				for (Player member : party.getMembers())
				{
					if ((member != player) && member.isInsideRadius3D(npc, PARTY_TELEPORT_RANGE))
					{
						member.teleToLocation(loc, true);
					}
				}
			}
			
			player.teleToLocation(loc, false);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method determines which dialogue file to show based on a quest.<br>
	 * It checks if the {@code Player} has completed the specific quest.<br>
	 * If the quest is finished, it returns the success dialog.<br>
	 * Otherwise, it returns the failure dialog.
	 * @param npc The {@code Npc} that is being spoken to.
	 * @param player The {@code Player} who is talking to the NPC.
	 * @return A {@code String} representing the name of the HTML file to display.
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		// TODO: Pre-Quest removed with Etina's Fate.
		// final QuestState accessQuest = player.getQuestState(Q00240_ImTheOnlyOneYouCanTrust.class.getSimpleName());
		// return (((accessQuest != null) && accessQuest.isCompleted()) ? "32640.htm" : "32640-no.htm");
		return "32640.htm";
	}
	
	/**
	 * This is the entry point for the {@code StakatoNestTeleporter} class.<br>
	 * It initializes a new instance of the teleporter script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new StakatoNestTeleporter();
	}
}
