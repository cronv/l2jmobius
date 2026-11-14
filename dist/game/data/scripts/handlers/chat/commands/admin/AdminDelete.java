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

import java.util.Collections;
import java.util.List;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.spawns.SpawnGroup;
import org.l2jmobius.gameserver.entity.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.entity.zone.type.SpawnTerritory;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.DatabaseSpawnManager;

/**
 * Handles the admin command to delete objects from the game world.<br>
 * This class allows administrators to remove {@code WorldObject} instances, such as NPCs or other entities.<br>
 * It interacts with the {@code World} and {@code DatabaseSpawnManager} to ensure proper cleanup.
 * @author Mobius
 */
public class AdminDelete implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_delete", // supports range parameter
		"admin_delete_group" // for territory spawns
	};
	
	/**
	 * Processes an admin command to delete NPCs or spawn groups.<br>
	 * This method identifies the specific action based on the {@code command} string.<br>
	 * It handles both individual deletions and group deletions.
	 * @param command The text input provided by the administrator.
	 * @param activeChar The {@code Player} who is executing the command.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.contains("group"))
		{
			handleDeleteGroup(activeChar);
		}
		else if (command.startsWith("admin_delete"))
		{
			final String[] split = command.split(" ");
			handleDelete(activeChar, (split.length > 1) && StringUtil.isNumeric(split[1]) ? Integer.parseInt(split[1]) : 0);
		}
		
		return true;
	}
	
	/**
	 * Deletes an {@code Npc} based on the provided range.<br>
	 * If {@code range} is greater than {@code 0}, it deletes all visible NPCs within that distance.<br>
	 * If {@code range} is {@code 0} or less, it attempts to delete the player's current target.
	 * @param player The {@code Player} executing the command.
	 * @param range The radius used to find targets if greater than {@code 0}.
	 */
	private void handleDelete(Player player, int range)
	{
		if (range > 0)
		{
			World.forEachVisibleObjectInRange(player, Npc.class, range, target -> deleteNpc(player, target));
			return;
		}
		
		final WorldObject obj = player.getTarget();
		if (obj instanceof Npc)
		{
			deleteNpc(player, obj.asNpc());
		}
		else
		{
			player.sendSysMessage("Incorrect target.");
		}
	}
	
	/**
	 * Deletes the group associated with a targeted {@code Npc}.<br>
	 * Checks if the current target is an instance of {@code Npc}.<br>
	 * Sends a system message to the {@code Player} if the target is invalid.
	 * @param player The {@code Player} executing the command.
	 */
	private void handleDeleteGroup(Player player)
	{
		final WorldObject obj = player.getTarget();
		if (obj instanceof Npc)
		{
			deleteGroup(player, obj.asNpc());
		}
		else
		{
			player.sendSysMessage("Incorrect target.");
		}
	}
	
	/**
	 * Removes a specific {@code Npc} from the world and its spawn data.<br>
	 * A territory spawn keeps its other npcs: only this one goes.
	 * @param player The {@code Player} who executed the command.
	 * @param target The {@code Npc} to be deleted.
	 */
	private void deleteNpc(Player player, Npc target)
	{
		final Spawn spawn = target.getSpawn();
		if (spawn != null)
		{
			target.deleteMe();
			spawn.stopRespawn();
			if (DatabaseSpawnManager.getInstance().isDefined(spawn.getId()))
			{
				DatabaseSpawnManager.getInstance().deleteSpawn(spawn, true);
			}
			else
			{
				SpawnData.getInstance().deleteSpawn(spawn);
			}
			
			player.sendSysMessage("Deleted " + target.getName() + " from " + target.getObjectId() + ".");
		}
	}
	
	/**
	 * Deletes the spawn group associated with a specific {@code Npc}.<br>
	 * This method removes the {@code Npc}, stops its respawn, and deletes it from the database.<br>
	 * It also despawns all other templates within the same group.
	 * @param player The {@code Player} who executed the command.
	 * @param target The {@code Npc} target to be deleted.
	 */
	private void deleteGroup(Player player, Npc target)
	{
		final Spawn spawn = target.getSpawn();
		if (spawn != null)
		{
			final NpcSpawnTemplate npcSpawnTemplate = spawn.getNpcSpawnTemplate();
			final SpawnGroup group = npcSpawnTemplate != null ? npcSpawnTemplate.getGroup() : null;
			List<SpawnTerritory> territories = group != null ? group.getTerritories() : Collections.emptyList();
			boolean simpleTerritory = false;
			if (territories.isEmpty())
			{
				final SpawnTemplate spawnTemplate = npcSpawnTemplate != null ? npcSpawnTemplate.getSpawnTemplate() : null;
				if (spawnTemplate != null)
				{
					territories = spawnTemplate.getTerritories();
					simpleTerritory = true;
				}
			}
			
			if (territories.isEmpty())
			{
				player.sendSysMessage("Incorrect target.");
			}
			else
			{
				target.deleteMe();
				spawn.stopRespawn();
				if (DatabaseSpawnManager.getInstance().isDefined(spawn.getId()))
				{
					DatabaseSpawnManager.getInstance().deleteSpawn(spawn, true);
				}
				else
				{
					SpawnData.getInstance().deleteGroup(spawn);
				}
				
				if (group != null)
				{
					for (NpcSpawnTemplate template : group.getSpawns())
					{
						template.despawn();
					}
				}
				else if (simpleTerritory && (npcSpawnTemplate != null))
				{
					npcSpawnTemplate.despawn();
				}
				
				player.sendSysMessage("Deleted " + target.getName() + " group from " + target.getObjectId() + ".");
			}
		}
	}
	
	/**
	 * Returns the list of available admin commands.<br>
	 * This method provides the strings used to identify this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
