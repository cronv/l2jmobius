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
package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Represents a specific type of doorman NPC for fort instances.<br>
 * This class handles the logic for players entering or interacting with fort areas.
 */
public class FortDoorman extends Doorman
{
	/**
	 * Creates a new {@code FortDoorman} instance.<br>
	 * This constructor initializes the doorman using a specific template.<br>
	 * It sets the entity type to {@code InstanceType.FortDoorman}.
	 * @param template The {@code NpcTemplate} used to create this doorman.
	 */
	public FortDoorman(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.FortDoorman);
	}
	
	/**
	 * Displays a chat window to the specified player.<br>
	 * The content of the window depends on the player's clan and siege status.<br>
	 * It sends an {@code ActionFailed} packet before showing the HTML message.
	 * @param player The {@code Player} who will see the chat window.
	 */
	@Override
	public void showChatWindow(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		if (!isOwnerClan(player))
		{
			html.setFile(player, "data/html/doorman/" + getTemplate().getId() + "-no.htm");
		}
		else if (isUnderSiege())
		{
			html.setFile(player, "data/html/doorman/" + getTemplate().getId() + "-busy.htm");
		}
		else
		{
			html.setFile(player, "data/html/doorman/" + getTemplate().getId() + ".htm");
		}
		
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	/**
	 * Opens specific doors for a player based on a command string.<br>
	 * This method parses the {@code command} to identify door IDs.<br>
	 * It then calls the open method on the fort for each ID found.
	 * @param player The {@code Player} who is performing the action.
	 * @param command A {@code String} containing the instructions to open doors.
	 */
	@Override
	protected void openDoors(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
		st.nextToken();
		
		while (st.hasMoreTokens())
		{
			getFort().openDoor(player, Integer.parseInt(st.nextToken()));
		}
	}
	
	/**
	 * Closes specific doors for a player based on a command.<br>
	 * This method parses the {@code command} string to identify door IDs.<br>
	 * It then calls the close method on each identified door.
	 * @param player The {@code Player} who is performing the action.
	 * @param command The raw command string containing the door identifiers.
	 */
	@Override
	protected void closeDoors(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
		st.nextToken();
		
		while (st.hasMoreTokens())
		{
			getFort().closeDoor(player, Integer.parseInt(st.nextToken()));
		}
	}
	
	/**
	 * Checks if a {@code Player} belongs to the clan that owns the fort.<br>
	 * This method verifies that both the player and the fort have valid owners.<br>
	 * It compares the unique IDs of the clans.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is in the owner clan, {@code false} otherwise.
	 */
	@Override
	protected boolean isOwnerClan(Player player)
	{
		final Fort fort = getFort();
		if ((player.getClan() != null) && (fort != null))
		{
			final Clan ownerClan = fort.getOwnerClan();
			if (ownerClan != null)
			{
				return player.getClanId() == ownerClan.getId();
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the current fort is currently under siege.<br>
	 * This method retrieves the status from the associated zone.
	 * @return {@code true} if the zone is active, {@code false} otherwise.
	 */
	@Override
	protected boolean isUnderSiege()
	{
		return getFort().getZone().isActive();
	}
}
