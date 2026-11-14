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
package handlers.chat.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.FakePlayerChatManager;

import custom.FakePlayers.RandomFakePlayerGenerator;

/**
 * Handles administrative commands for managing fake players in the game world.<br>
 * {@code //fakechat} sends a message from a fake player, {@code //spawnfake} spawns a fresh random fake player at the admin position, and {@code //fakeinfo} shows the targeted fake player's class, level and worn equipment, highlighting any missing armor part.
 * @author Mobius
 */
public class AdminFakePlayers implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminFakePlayers.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_fakechat",
		"admin_spawnfake",
		"admin_fakeinfo"
	};
	
	/**
	 * Processes administrative fake player commands.
	 * @param command The raw string containing the command and its arguments.
	 * @param activeChar The {@code Player} object who is executing the command.
	 * @return {@code true} if the command was processed successfully, or {@code false} if it failed.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_fakechat"))
		{
			final String[] words = command.substring(15).split(" ");
			if (words.length < 3)
			{
				activeChar.sendSysMessage("Usage: //fakechat playername fpcname message");
				return false;
			}
			
			final Player player = World.getPlayer(words[0]);
			if (player == null)
			{
				activeChar.sendSysMessage("Player not found.");
				return false;
			}
			
			final String fpcName = FakePlayerData.getInstance().getProperName(words[1]);
			if (fpcName == null)
			{
				activeChar.sendSysMessage("Fake player not found.");
				return false;
			}
			
			String message = "";
			for (int i = 0; i < words.length; i++)
			{
				if (i < 2)
				{
					continue;
				}
				
				message += (words[i] + " ");
			}
			
			FakePlayerChatManager.getInstance().sendChat(player, fpcName, message);
			activeChar.sendSysMessage("Your message has been sent.");
			return true;
		}
		else if (command.equals("admin_spawnfake"))
		{
			try
			{
				final int npcId = RandomFakePlayerGenerator.generate();
				final Spawn spawn = new Spawn(npcId);
				spawn.setXYZ(activeChar.getX(), activeChar.getY(), activeChar.getZ());
				spawn.setHeading(activeChar.getHeading());
				spawn.setAmount(1);
				if (activeChar.isInInstance())
				{
					spawn.setInstanceId(activeChar.getInstanceId());
				}
				
				SpawnTable.getInstance().addSpawn(spawn);
				spawn.init();
				spawn.stopRespawn();
				
				final Npc npc = spawn.getLastSpawn();
				if (npc != null)
				{
					npc.broadcastInfo();
					activeChar.sendSysMessage("Spawned fake player " + npc.getName() + " (npcId " + npcId + ").");
				}
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed to spawn fake player: " + e.getMessage());
				LOGGER.warning("admin_spawnfake: " + e.getMessage());
			}
			return true;
		}
		else if (command.equals("admin_fakeinfo"))
		{
			final WorldObject target = activeChar.getTarget();
			if (!(target instanceof Npc npc) || !npc.getTemplate().isFakePlayer())
			{
				activeChar.sendSysMessage("Target a fake player first.");
				return true;
			}
			
			final NpcTemplate template = npc.getTemplate();
			final FakePlayerHolder holder = template.getFakePlayerInfo();
			activeChar.sendSysMessage("=== Fake Player: " + npc.getName() + " ===");
			activeChar.sendSysMessage("Class: " + holder.getPlayerClass() + " | Level: " + template.getLevel() + " | Race: " + template.getRace() + " | Sex: " + template.getSex());
			activeChar.sendSysMessage(itemLine("Weapon", holder.getEquipRHand()));
			activeChar.sendSysMessage(itemLine("Shield", holder.getEquipLHand()));
			activeChar.sendSysMessage(itemLine("Head", holder.getEquipHead()));
			activeChar.sendSysMessage(itemLine("Chest", holder.getEquipChest()));
			
			final boolean fullArmor = isFullArmor(holder.getEquipChest());
			activeChar.sendSysMessage(fullArmor ? "Legs: covered by full armor" : itemLine("Legs", holder.getEquipLegs()));
			activeChar.sendSysMessage(itemLine("Gloves", holder.getEquipGloves()));
			activeChar.sendSysMessage(itemLine("Feet", holder.getEquipFeet()));
			
			final List<String> missing = new ArrayList<>();
			if (holder.getEquipRHand() <= 0)
			{
				missing.add("Weapon");
			}
			
			if (holder.getEquipHead() <= 0)
			{
				missing.add("Head");
			}
			
			if (holder.getEquipChest() <= 0)
			{
				missing.add("Chest");
			}
			
			if (!fullArmor && (holder.getEquipLegs() <= 0))
			{
				missing.add("Legs");
			}
			
			if (holder.getEquipGloves() <= 0)
			{
				missing.add("Gloves");
			}
			
			if (holder.getEquipFeet() <= 0)
			{
				missing.add("Feet");
			}
			
			activeChar.sendSysMessage(missing.isEmpty() ? "Missing parts: none" : "Missing parts: " + String.join(", ", missing));
			return true;
		}
		
		return true;
	}
	
	/**
	 * Formats a slot line with the item name and id, or a missing marker.
	 * @param slot the slot label
	 * @param itemId the equipped item id
	 * @return the display line
	 */
	private static String itemLine(String slot, int itemId)
	{
		if (itemId <= 0)
		{
			return slot + ": -- MISSING --";
		}
		
		final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
		return slot + ": " + (item != null ? item.getName() : "?") + " (" + itemId + ")";
	}
	
	/**
	 * Checks whether the chest item is a full armor, which also covers the legs.
	 * @param chestId the chest item id
	 * @return {@code true} if the chest is a full armor
	 */
	private static boolean isFullArmor(int chestId)
	{
		if (chestId <= 0)
		{
			return false;
		}
		
		final ItemTemplate chest = ItemData.getInstance().getTemplate(chestId);
		return (chest != null) && (chest.getBodyPart() == BodyPart.FULL_ARMOR);
	}
	
	/**
	 * Retrieves the list of available admin commands.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
