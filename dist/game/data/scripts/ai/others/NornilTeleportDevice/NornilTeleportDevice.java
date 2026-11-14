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
package ai.others.NornilTeleportDevice;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * Handles the artificial intelligence for the {@code NornilTeleportDevice}.<br>
 * This class manages the behavior of the teleportation device script.
 * @author St3eT
 */
public class NornilTeleportDevice extends Script
{
	// NPCs
	private static final int DEVICE = 33790; // Nornil Teleport Device
	
	// Locations
	private static final Location[] LOCATIONS =
	{
		new Location(-79667, 54028, -4824),
		new Location(-87483, 54024, -4440),
		new Location(-87839, 49803, -4344),
		new Location(-84995, 50974, -4600),
		new Location(-86945, 42814, -2656),
	};
	
	/**
	 * Initializes the {@code NornilTeleportDevice} script.<br>
	 * This constructor registers the required NPC IDs and talk events.
	 */
	public NornilTeleportDevice()
	{
		addStartNpc(DEVICE);
		addFirstTalkId(DEVICE);
		addTalkId(DEVICE);
	}
	
	/**
	 * Handles specific events triggered by the teleport device.<br>
	 * It checks if the {@code event} string starts with {@code teleport_}.<br>
	 * If it does, it moves the {@code player} to a predefined location.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} entity that triggered the event.
	 * @param player The {@code Player} entity who will be moved.
	 * @return The result of the super class event handler.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("teleport_"))
		{
			final int locId = Integer.parseInt(event.replace("teleport_", ""));
			player.teleToLocation(LOCATIONS[locId - 1]);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * It determines which HTML file to display based on the device location.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} entity initiating the conversation.
	 * @return A {@code String} representing the path to the HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "NornilTeleportDevice-" + npc.getParameters().getInt("device_place", 0) + ".html";
	}
	
	/**
	 * This is the entry point for the {@code NornilTeleportDevice} class.<br>
	 * It initializes a new instance of the script to start the program.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new NornilTeleportDevice();
	}
}
