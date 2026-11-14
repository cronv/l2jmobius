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
package ai.areas.MithrilMines.MithrilMinesTeleporter;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * Handles the teleportation logic for NPCs in the Mithril Mines area.<br>
 * This {@code MithrilMinesTeleporter} script manages how entities move between specific locations.
 * @author Charus
 */
public class MithrilMinesTeleporter extends Script
{
	// NPC
	private static final int TELEPORT_CRYSTAL = 32652;
	
	// Location
	private static final Location[] LOCS =
	{
		new Location(171946, -173352, 3440),
		new Location(175499, -181586, -904),
		new Location(173462, -174011, 3480),
		new Location(179299, -182831, -224),
		new Location(178591, -184615, -360),
		new Location(175499, -181586, -904)
	};
	
	/**
	 * Private constructor for the {@code MithrilMinesTeleporter} class.<br>
	 * It initializes the required NPC IDs and talk events.<br>
	 * This method is not intended to be called from outside this class.
	 */
	private MithrilMinesTeleporter()
	{
		addStartNpc(TELEPORT_CRYSTAL);
		addFirstTalkId(TELEPORT_CRYSTAL);
		addTalkId(TELEPORT_CRYSTAL);
	}
	
	/**
	 * Handles specific events for the teleportation script.<br>
	 * It teleports a {@code Player} to a location based on an index provided in the {@code event} string.
	 * @param event The index of the destination location as a string.
	 * @param npc The {@code Npc} that triggered the event.
	 * @param player The {@code Player} who will be teleported.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final int index = Integer.parseInt(event) - 1;
		if (LOCS.length > index)
		{
			final Location loc = LOCS[index];
			player.teleToLocation(loc, false);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Determines the correct dialogue file to show when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method checks the current location of the {@code Npc} to return specific HTML files.<br>
	 * If no specific location matches, it falls back to the default behavior.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} entity initiating the conversation.
	 * @return A {@code String} representing the name of the HTML dialogue file to display.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.isInsideRadius2D(173147, -173762, 0, Npc.INTERACTION_DISTANCE))
		{
			return "32652-01.htm";
		}
		
		if (npc.isInsideRadius2D(181941, -174614, 0, Npc.INTERACTION_DISTANCE))
		{
			return "32652-02.htm";
		}
		
		if (npc.isInsideRadius2D(179560, -182956, 0, Npc.INTERACTION_DISTANCE))
		{
			return "32652-03.htm";
		}
		
		return super.onFirstTalk(npc, player);
	}
	
	/**
	 * This is the entry point for the {@code MithrilMinesTeleporter} class.<br>
	 * It initializes a new instance of the teleporter script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new MithrilMinesTeleporter();
	}
}
