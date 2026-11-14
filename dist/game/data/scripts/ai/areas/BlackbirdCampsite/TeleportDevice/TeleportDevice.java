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
package ai.areas.BlackbirdCampsite.TeleportDevice;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * Represents the Kingdom's Royal Guard Teleport Device.<br>
 * This {@code Script} handles the teleportation logic for players interacting with the device.
 * @author Gigi
 * @date 2018-04-30 - [23:32:48]
 */
public class TeleportDevice extends Script
{
	// NPC
	private static final int TELEPORT_DEVICE = 34242;
	
	// Teleports
	private static final Location LOCATION1 = new Location(-46335, 59575, -2960);
	private static final Location LOCATION2 = new Location(-42307, 51232, -2032);
	private static final Location LOCATION3 = new Location(-44060, 40139, -1432);
	private static final Location LOCATION4 = new Location(-57242, 43811, -1552);
	
	/**
	 * Private constructor for the {@code TeleportDevice} class.<br>
	 * This constructor initializes the required talk IDs.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private TeleportDevice()
	{
		addFirstTalkId(TELEPORT_DEVICE);
		addTalkId(TELEPORT_DEVICE);
	}
	
	/**
	 * Handles specific events for the teleport device.<br>
	 * It moves a {@code Player} to different locations based on the event name.
	 * @param event The name of the teleport action to perform.
	 * @param npc The {@code Npc} that triggered the event.
	 * @param player The {@code Player} who will be teleported.
	 * @return Always returns {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "teleport1":
			{
				player.teleToLocation(LOCATION1);
				break;
			}
			case "teleport2":
			{
				player.teleToLocation(LOCATION2);
				break;
			}
			case "teleport3":
			{
				player.teleToLocation(LOCATION3);
				break;
			}
			case "teleport4":
			{
				player.teleToLocation(LOCATION4);
				break;
			}
		}
		
		return null;
	}
	
	/**
	 * Handles the initial interaction between a {@code Player} and an {@code Npc}.<br>
	 * This method returns the name of the HTML file to display.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the interaction.
	 * @return A {@code String} representing the filename of the dialogue UI.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "34242.html";
	}
	
	/**
	 * This is the entry point for the {@code TeleportDevice} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new TeleportDevice();
	}
}
