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

import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.TeleporterData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.teleporter.TeleportHolder;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Represents a doorman NPC that handles teleportation requests.<br>
 * This class manages the interaction between players and teleporters within an instance.
 */
public class Doorman extends Folk
{
	/**
	 * Creates a new {@code Doorman} instance.<br>
	 * This constructor initializes the actor using a provided template.<br>
	 * It sets the {@code InstanceType} to {@code Doorman}.
	 * @param template The {@code NpcTemplate} used to create this doorman.
	 */
	public Doorman(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Doorman);
	}
	
	/**
	 * Checks if the {@code Doorman} can be attacked by a specific creature.<br>
	 * This method allows monsters to attack this entity.<br>
	 * It falls back to the parent class logic for other types of attackers.
	 * @param attacker The {@code Creature} attempting to perform an auto-attack.
	 * @return {@code true} if the attacker is allowed to hit the doorman, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker.isMonster())
		{
			return true;
		}
		
		return super.isAutoAttackable(attacker);
	}
	
	/**
	 * Handles specific commands sent by a {@code Player} to the doorman.<br>
	 * This method processes actions like opening doors, closing doors, or teleporting.<br>
	 * If no specific command is recognized, it passes the request to the superclass.
	 * @param player The {@code Player} who sent the command.
	 * @param command The string content of the command received.
	 */
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("Chat"))
		{
			showChatWindow(player);
			return;
		}
		else if (command.startsWith("open_doors"))
		{
			if (isOwnerClan(player))
			{
				if (isUnderSiege())
				{
					cannotManageDoors(player);
				}
				else
				{
					openDoors(player, command);
				}
			}
			return;
		}
		else if (command.startsWith("close_doors"))
		{
			if (isOwnerClan(player))
			{
				if (isUnderSiege())
				{
					cannotManageDoors(player);
				}
				else
				{
					closeDoors(player, command);
				}
			}
			return;
		}
		else if (command.startsWith("tele"))
		{
			if (isOwnerClan(player))
			{
				final TeleportHolder holder = TeleporterData.getInstance().getHolder(getId(), TeleportType.OTHER.name());
				if (holder != null)
				{
					final int locId = Integer.parseInt(command.substring(5).trim());
					holder.doTeleport(player, this, locId);
				}
			}
			return;
		}
		
		super.onBypassFeedback(player, command);
	}
	
	/**
	 * Displays a chat window to the specified player.<br>
	 * This method sends an {@code ActionFailed} packet first.<br>
	 * It then determines which HTML file to show based on clan ownership.<br>
	 * Finally, it sends the {@code NpcHtmlMessage} to the player.
	 * @param player The {@code Player} who will see the chat window.
	 */
	@Override
	public void showChatWindow(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		final int objectId = getObjectId();
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		if (!isOwnerClan(player))
		{
			html.setFile(player, "data/html/doorman/" + getTemplate().getId() + "-no.htm");
		}
		else
		{
			html.setFile(player, "data/html/doorman/" + getTemplate().getId() + ".htm");
		}
		
		html.replace("%objectId%", String.valueOf(objectId));
		player.sendPacket(html);
	}
	
	/**
	 * Opens specific doors based on the provided command.<br>
	 * This method parses a string to find door IDs.<br>
	 * It then calls {@code openMe()} on each matching door.
	 * @param player The {@code Player} who is interacting with the doorman.
	 * @param command The command string containing the door identifiers.
	 */
	protected void openDoors(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
		st.nextToken();
		
		while (st.hasMoreTokens())
		{
			DoorData.getInstance().getDoor(Integer.parseInt(st.nextToken())).openMe();
		}
	}
	
	/**
	 * Closes specific doors based on the provided command.<br>
	 * This method parses a list of door IDs from the {@code command} string.<br>
	 * It iterates through each ID and calls {@code closeMe()} on the corresponding {@code DoorData}.
	 * @param player The {@code Player} who issued the command.
	 * @param command The full command string containing the door IDs to be closed.
	 */
	protected void closeDoors(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
		st.nextToken();
		
		while (st.hasMoreTokens())
		{
			DoorData.getInstance().getDoor(Integer.parseInt(st.nextToken())).closeMe();
		}
	}
	
	/**
	 * This method prevents a {@code Player} from interacting with doors.<br>
	 * It sends an {@code ActionFailed} packet to the player.<br>
	 * It also displays a busy status message via {@code NpcHtmlMessage}.
	 * @param player The {@code Player} who tried to manage the doors.
	 */
	protected void cannotManageDoors(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, "data/html/doorman/" + getTemplate().getId() + "-busy.htm");
		player.sendPacket(html);
	}
	
	/**
	 * Checks if the specified {@code Player} belongs to the owner clan.<br>
	 * This method currently always returns {@code true}.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is an owner, otherwise {@code false}.
	 */
	protected boolean isOwnerClan(Player player)
	{
		return true;
	}
	
	/**
	 * Checks if the doorman is currently under siege.<br>
	 * This method returns {@code false} by default.
	 * @return {@code true} if the doorman is under siege, otherwise {@code false}.
	 */
	protected boolean isUnderSiege()
	{
		return false;
	}
}
