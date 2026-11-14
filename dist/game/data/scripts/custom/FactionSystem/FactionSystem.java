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
package custom.FactionSystem;

import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Manages the core logic for the faction system within the game.<br>
 * This class handles interactions between players and factions based on {@code FactionSystemConfig}. It provides functionality for tracking affiliations and related world events.
 * @author Mobius
 */
public class FactionSystem extends Script
{
	// NPCs
	private static final int MANAGER = 500;
	
	// Other
	private static final String[] TEXTS =
	{
		FactionSystemConfig.FACTION_GOOD_TEAM_NAME + " or " + FactionSystemConfig.FACTION_EVIL_TEAM_NAME + "?",
		"Select your faction!",
		"The choice is yours!"
	};
	
	/**
	 * Private constructor for the {@code FactionSystem} class.<br>
	 * This constructor initializes the required NPC IDs and spawn locations.<br>
	 * It ensures that the faction manager is registered correctly in the game world.
	 */
	private FactionSystem()
	{
		addSpawnId(MANAGER);
		addStartNpc(MANAGER);
		addTalkId(MANAGER);
		addFirstTalkId(MANAGER);
		
		if (FactionSystemConfig.FACTION_SYSTEM_ENABLED)
		{
			addSpawn(MANAGER, FactionSystemConfig.FACTION_MANAGER_LOCATION, false, 0);
		}
	}
	
	/**
	 * Handles specific events for the faction system.<br>
	 * This method processes actions like joining a faction or NPC speech.<br>
	 * It updates player status and sends messages based on the event type.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the event processing or the superclass return value.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "selectGoodFaction":
			{
				if (FactionSystemConfig.FACTION_BALANCE_ONLINE_PLAYERS && (World.getAllGoodPlayers().size() >= (World.getAllEvilPlayers().size() + FactionSystemConfig.FACTION_BALANCE_PLAYER_EXCEED_LIMIT)))
				{
					final String htmltext = null;
					final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
					packet.setHtml(getHtm(player, "onlinelimit.html"));
					packet.replace("%name%", player.getName());
					packet.replace("%more%", FactionSystemConfig.FACTION_GOOD_TEAM_NAME);
					packet.replace("%less%", FactionSystemConfig.FACTION_EVIL_TEAM_NAME);
					player.sendPacket(packet);
					return htmltext;
				}
				
				if (FactionSystemConfig.FACTION_AUTO_NOBLESS)
				{
					player.setNobleLevel(1);
				}
				
				player.setGood();
				final PlayerAppearance appearance = player.getAppearance();
				appearance.setNameColor(FactionSystemConfig.FACTION_GOOD_NAME_COLOR);
				appearance.setTitleColor(FactionSystemConfig.FACTION_GOOD_NAME_COLOR);
				player.setTitle(FactionSystemConfig.FACTION_GOOD_TEAM_NAME);
				player.sendMessage("You are now fighting for the " + FactionSystemConfig.FACTION_GOOD_TEAM_NAME + " faction.");
				player.teleToLocation(FactionSystemConfig.FACTION_GOOD_BASE_LOCATION);
				broadcastMessageToFaction(FactionSystemConfig.FACTION_GOOD_TEAM_NAME, FactionSystemConfig.FACTION_GOOD_TEAM_NAME + " faction grows stronger with the arrival of " + player.getName() + ".");
				World.addFactionPlayerToWorld(player);
				break;
			}
			case "selectEvilFaction":
			{
				if (FactionSystemConfig.FACTION_BALANCE_ONLINE_PLAYERS && (World.getAllEvilPlayers().size() >= (World.getAllGoodPlayers().size() + FactionSystemConfig.FACTION_BALANCE_PLAYER_EXCEED_LIMIT)))
				{
					final String htmltext = null;
					final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
					packet.setHtml(getHtm(player, "onlinelimit.html"));
					packet.replace("%name%", player.getName());
					packet.replace("%more%", FactionSystemConfig.FACTION_EVIL_TEAM_NAME);
					packet.replace("%less%", FactionSystemConfig.FACTION_GOOD_TEAM_NAME);
					player.sendPacket(packet);
					return htmltext;
				}
				
				if (FactionSystemConfig.FACTION_AUTO_NOBLESS)
				{
					player.setNobleLevel(1);
				}
				
				player.setEvil();
				final PlayerAppearance appearance = player.getAppearance();
				appearance.setNameColor(FactionSystemConfig.FACTION_EVIL_NAME_COLOR);
				appearance.setTitleColor(FactionSystemConfig.FACTION_EVIL_NAME_COLOR);
				player.setTitle(FactionSystemConfig.FACTION_EVIL_TEAM_NAME);
				player.sendMessage("You are now fighting for the " + FactionSystemConfig.FACTION_EVIL_TEAM_NAME + " faction.");
				player.teleToLocation(FactionSystemConfig.FACTION_EVIL_BASE_LOCATION);
				broadcastMessageToFaction(FactionSystemConfig.FACTION_EVIL_TEAM_NAME, FactionSystemConfig.FACTION_EVIL_TEAM_NAME + " faction grows stronger with the arrival of " + player.getName() + ".");
				World.addFactionPlayerToWorld(player);
				break;
			}
			case "SPEAK":
			{
				if (npc != null)
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(TEXTS), 1500);
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the initial interaction between a {@code Player} and an {@code Npc}.<br>
	 * This method sends a custom HTML message to the player.<br>
	 * The message allows the player to choose their faction.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return Always returns {@code null}.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final String htmltext = null;
		final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
		packet.setHtml(getHtm(player, "manager.html"));
		packet.replace("%name%", player.getName());
		packet.replace("%good%", FactionSystemConfig.FACTION_GOOD_TEAM_NAME);
		packet.replace("%evil%", FactionSystemConfig.FACTION_EVIL_TEAM_NAME);
		player.sendPacket(packet);
		return htmltext;
	}
	
	/**
	 * This method is called when an {@code Npc} spawns in the world.<br>
	 * It checks if the {@code Npc} ID matches the {@code MANAGER} constant.<br>
	 * If it matches, it starts a quest timer for the NPC.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == MANAGER)
		{
			startQuestTimer("SPEAK", 10000, npc, null, true);
		}
	}
	
	/**
	 * Sends a message to all players in a specific faction.<br>
	 * It checks the {@code factionName} to determine which group to notify.<br>
	 * The message is sent to every player found in that group.
	 * @param factionName The name of the faction to receive the message.
	 * @param message The text content to be broadcasted.
	 */
	private void broadcastMessageToFaction(String factionName, String message)
	{
		if (factionName.equals(FactionSystemConfig.FACTION_GOOD_TEAM_NAME))
		{
			for (Player player : World.getAllGoodPlayers())
			{
				player.sendMessage(message);
			}
		}
		else
		{
			for (Player player : World.getAllEvilPlayers())
			{
				player.sendMessage(message);
			}
		}
	}
	
	/**
	 * This is the entry point for the {@code FactionSystem} class.<br>
	 * It initializes a new instance of the system to start the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new FactionSystem();
	}
}
