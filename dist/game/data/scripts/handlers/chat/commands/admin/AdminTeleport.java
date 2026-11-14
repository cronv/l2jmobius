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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.AdminTeleportType;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.DatabaseSpawnManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class handles various administrative teleportation commands.<br>
 * It manages actions such as {@code show_moves}, {@code show_teleport}, {@code teleport_to_character}, {@code move_to}, and {@code teleport_character}.
 */
public class AdminTeleport implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminTeleport.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_show_moves",
		"admin_show_moves_other",
		"admin_show_teleport",
		"admin_teleport_to_character",
		"admin_teleportto",
		"admin_teleport",
		"admin_move_to",
		"admin_teleport_character",
		"admin_recall",
		"admin_walk",
		"teleportto",
		"recall",
		"admin_recall_npc",
		"admin_gonorth",
		"admin_gosouth",
		"admin_goeast",
		"admin_gowest",
		"admin_goup",
		"admin_godown",
		"admin_tele",
		"admin_teleto",
		"admin_instant_move",
		"admin_sendhome"
	};
	
	/**
	 * Processes administrative commands for the active player.<br>
	 * It handles teleportation, NPC recall, and movement actions.
	 * @param command The string content of the command sent by the user.
	 * @param activeChar The {@code Player} who is executing the command.
	 * @return {@code true} if the command was recognized and processed, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_instant_move"))
		{
			activeChar.sendSysMessage("Instant move ready. Click where you want to go.");
			activeChar.setTeleMode(AdminTeleportType.DEMONIC);
		}
		else if (command.equals("admin_teleto sayune"))
		{
			activeChar.sendSysMessage("Sayune move ready. Click where you want to go.");
			activeChar.setTeleMode(AdminTeleportType.SAYUNE);
		}
		else if (command.equals("admin_teleto charge"))
		{
			activeChar.sendSysMessage("Charge move ready. Click where you want to go.");
			activeChar.setTeleMode(AdminTeleportType.CHARGE);
		}
		else if (command.equals("admin_teleto end"))
		{
			activeChar.setTeleMode(AdminTeleportType.NORMAL);
		}
		else if (command.equals("admin_show_moves"))
		{
			AdminHtml.showAdminHtml(activeChar, "teleports.htm");
		}
		else if (command.equals("admin_show_moves_other"))
		{
			AdminHtml.showAdminHtml(activeChar, "teleports/OtherLocations.htm");
		}
		else if (command.equals("admin_show_teleport"))
		{
			showTeleportCharWindow(activeChar);
		}
		else if (command.equals("admin_recall_npc"))
		{
			recallNPC(activeChar);
		}
		else if (command.equals("admin_teleport_to_character"))
		{
			teleportToCharacter(activeChar, activeChar.getTarget());
		}
		else if (command.startsWith("admin_walk"))
		{
			try
			{
				final String val = command.substring(11);
				final StringTokenizer st = new StringTokenizer(val);
				final int x = Integer.parseInt(st.nextToken());
				final int y = Integer.parseInt(st.nextToken());
				final int z = Integer.parseInt(st.nextToken());
				activeChar.getAI().setIntentionMoveTo(new Location(x, y, z, 0));
			}
			catch (Exception e)
			{
			}
		}
		else if (command.startsWith("admin_move_to"))
		{
			try
			{
				final String val = command.substring(14);
				teleportTo(activeChar, val);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Case of empty or missing coordinates.
				AdminHtml.showAdminHtml(activeChar, "teleports.htm");
			}
			catch (NumberFormatException nfe)
			{
				activeChar.sendSysMessage("Usage: //move_to <x> <y> <z>");
				AdminHtml.showAdminHtml(activeChar, "teleports.htm");
			}
		}
		else if (command.startsWith("admin_teleport_character"))
		{
			try
			{
				final String val = command.substring(25);
				teleportCharacter(activeChar, val);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Case of empty coordinates.
				activeChar.sendSysMessage("Wrong or no Coordinates given.");
				showTeleportCharWindow(activeChar); // back to character teleport
			}
		}
		else if (command.startsWith("admin_teleportto "))
		{
			try
			{
				final String targetName = command.substring(17);
				final Player player = World.getPlayer(targetName);
				teleportToCharacter(activeChar, player);
			}
			catch (StringIndexOutOfBoundsException e)
			{
			}
		}
		else if (command.startsWith("admin_teleport"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int x = (int) Float.parseFloat(st.nextToken());
				final int y = (int) Float.parseFloat(st.nextToken());
				final int z = st.hasMoreTokens() ? ((int) Float.parseFloat(st.nextToken())) : GeoEngine.getInstance().getHeight(x, y, 10000);
				activeChar.teleToLocation(x, y, z);
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Wrong coordinates!");
			}
		}
		else if (command.startsWith("admin_recall "))
		{
			try
			{
				final String[] param = command.split(" ");
				if (param.length != 2)
				{
					activeChar.sendSysMessage("Usage: //recall <playername>");
					return false;
				}
				
				final String targetName = param[1];
				final Player player = World.getPlayer(targetName);
				if (player != null)
				{
					teleportCharacter(player, activeChar.getLocation(), activeChar);
				}
				else
				{
					changeCharacterPosition(activeChar, targetName);
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
			}
		}
		else if (command.equals("admin_tele"))
		{
			showTeleportWindow(activeChar);
		}
		else if (command.startsWith("admin_go"))
		{
			int intVal = 150;
			int x = activeChar.getX();
			int y = activeChar.getY();
			int z = activeChar.getZ();
			try
			{
				final String val = command.substring(8);
				final StringTokenizer st = new StringTokenizer(val);
				final String dir = st.nextToken();
				if (st.hasMoreTokens())
				{
					intVal = Integer.parseInt(st.nextToken());
				}
				
				if (dir.equals("east"))
				{
					x += intVal;
				}
				else if (dir.equals("west"))
				{
					x -= intVal;
				}
				else if (dir.equals("north"))
				{
					y -= intVal;
				}
				else if (dir.equals("south"))
				{
					y += intVal;
				}
				else if (dir.equals("up"))
				{
					z += intVal;
				}
				else if (dir.equals("down"))
				{
					z -= intVal;
				}
				
				activeChar.teleToLocation(new Location(x, y, z));
				showTeleportWindow(activeChar);
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //go<north|south|east|west|up|down> [offset] (default 150)");
			}
		}
		else if (command.startsWith("admin_sendhome"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // Skip command.
			if (st.countTokens() > 1)
			{
				activeChar.sendSysMessage("Usage: //sendhome <playername>");
			}
			else if (st.countTokens() == 1)
			{
				final String name = st.nextToken();
				final Player player = World.getPlayer(name);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
					return false;
				}
				
				teleportHome(player);
			}
			else
			{
				final WorldObject target = activeChar.getTarget();
				if ((target != null) && target.isPlayer())
				{
					teleportHome(target.asPlayer());
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns a static array of strings representing all supported commands.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Moves the player to their respective starting town.<br>
	 * The destination is determined by the {@code Player} race.
	 * @param player The {@code Player} to teleport.
	 */
	private void teleportHome(Player player)
	{
		String regionName;
		switch (player.getRace())
		{
			case ELF:
			{
				regionName = "elf_town";
				break;
			}
			case DARK_ELF:
			{
				regionName = "darkelf_town";
				break;
			}
			case ORC:
			{
				regionName = "orc_town";
				break;
			}
			case DWARF:
			{
				regionName = "dwarf_town";
				break;
			}
			case KAMAEL:
			{
				regionName = "kamael_town";
				break;
			}
			case HUMAN:
			default:
			{
				regionName = "talking_island_town";
				break;
			}
		}
		
		player.teleToLocation(MapRegionData.getInstance().getSpawnLocByRegionName(regionName), true, null);
	}
	
	/**
	 * Moves a player to specific coordinates.<br>
	 * The {@code coords} string must contain three integers for x, y, and z.<br>
	 * Sends a system message to the player upon success.
	 * @param activeChar The {@code Player} who will be moved.
	 * @param coords A {@code String} containing the target coordinates.
	 */
	private void teleportTo(Player activeChar, String coords)
	{
		try
		{
			final StringTokenizer st = new StringTokenizer(coords);
			final int x = Integer.parseInt(st.nextToken());
			final int y = Integer.parseInt(st.nextToken());
			final int z = Integer.parseInt(st.nextToken());
			activeChar.getAI().setIntentionIdle();
			activeChar.teleToLocation(x, y, z, null);
			activeChar.sendSysMessage("You have been teleported to " + coords);
		}
		catch (NoSuchElementException nsee)
		{
			activeChar.sendSysMessage("Wrong or no Coordinates given.");
		}
	}
	
	/**
	 * Opens the teleportation menu for an administrator.<br>
	 * This method displays the {@code move.htm} interface to the user.
	 * @param activeChar The {@code Player} who will see the window.
	 */
	private void showTeleportWindow(Player activeChar)
	{
		AdminHtml.showAdminHtml(activeChar, "move.htm");
	}
	
	/**
	 * Opens a teleport window for the target character.<br>
	 * It checks if the current target is a {@code Player}.<br>
	 * If valid, it sends an HTML message with coordinate inputs to the admin.<br>
	 * If invalid, it sends an {@code INVALID_TARGET} system message.
	 * @param activeChar The {@code Player} who is executing the command.
	 */
	private void showTeleportCharWindow(Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		Player player = null;
		if ((target != null) && target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0, 1);
		final String replyMSG = "<html><title>Teleport Character</title><body>The character you will teleport is " + player.getName() + ".<br>Co-ordinate x<edit var=\"char_cord_x\" width=110>Co-ordinate y<edit var=\"char_cord_y\" width=110>Co-ordinate z<edit var=\"char_cord_z\" width=110><button value=\"Teleport\" action=\"bypass -h admin_teleport_character $char_cord_x $char_cord_y $char_cord_z\" width=60 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><button value=\"Teleport near you\" action=\"bypass -h admin_teleport_character " + activeChar.getX() + " " + activeChar.getY() + " " + activeChar.getZ() + "\" width=115 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><center><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>";
		adminReply.setHtml(replyMSG);
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * Teleports a targeted player to specific coordinates.<br>
	 * This method validates the target and parses the coordinate string.<br>
	 * It prevents teleporting a character to themselves.
	 * @param activeChar The {@code Player} executing the command.
	 * @param coords A {@code String} containing the x, y, and z coordinates.
	 */
	private void teleportCharacter(Player activeChar, String coords)
	{
		final WorldObject target = activeChar.getTarget();
		Player player = null;
		if ((target != null) && target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		if (player.getObjectId() == activeChar.getObjectId())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_ON_YOURSELF);
		}
		else
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(coords);
				final String x1 = st.nextToken();
				final int x = Integer.parseInt(x1);
				final String y1 = st.nextToken();
				final int y = Integer.parseInt(y1);
				final String z1 = st.nextToken();
				final int z = Integer.parseInt(z1);
				teleportCharacter(player, new Location(x, y, z), null);
			}
			catch (NoSuchElementException nsee)
			{
			}
		}
	}
	
	/**
	 * Teleports a specific player to a given location.<br>
	 * This method checks if the target is in jail before moving them.<br>
	 * It sends feedback messages to both the admin and the player.
	 * @param player The {@code Player} object to be teleported.
	 * @param loc The {@code Location} where the player will move.
	 * @param activeChar The {@code Player} performing the teleport action.
	 */
	private void teleportCharacter(Player player, Location loc, Player activeChar)
	{
		if (player != null)
		{
			// Check for jail
			if (player.isJailed())
			{
				activeChar.sendSysMessage("Sorry, player " + player.getName() + " is in Jail.");
			}
			else
			{
				activeChar.sendSysMessage("You have recalled " + player.getName());
				player.sendMessage("Admin is teleporting you.");
				player.getAI().setIntentionIdle();
				player.teleToLocation(loc, true, activeChar.getInstanceWorld());
			}
		}
	}
	
	/**
	 * Moves the {@code activeChar} to the location of a specific {@code WorldObject}.<br>
	 * This method checks if the target is a valid {@code Player}.<br>
	 * It prevents teleporting to the same character.
	 * @param activeChar The player who will perform the teleport.
	 * @param target The world object that serves as the destination.
	 */
	private void teleportToCharacter(Player activeChar, WorldObject target)
	{
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final Player player = target.asPlayer();
		if (player.getObjectId() == activeChar.getObjectId())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_ON_YOURSELF);
		}
		else
		{
			activeChar.getAI().setIntentionIdle();
			activeChar.teleToLocation(player, true, player.getInstanceWorld());
			activeChar.sendSysMessage("You have teleported to character " + player.getName() + ".");
		}
	}
	
	/**
	 * Updates the coordinates of a specific character in the database.<br>
	 * This method uses the current location of {@code activeChar}.<br>
	 * It sends a confirmation or error message to the player.
	 * @param activeChar The {@code Player} who is executing the command.
	 * @param name The name of the character whose position will be updated.
	 */
	private void changeCharacterPosition(Player activeChar, String name)
	{
		final int x = activeChar.getX();
		final int y = activeChar.getY();
		final int z = activeChar.getZ();
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=? WHERE char_name=?");
			statement.setInt(1, x);
			statement.setInt(2, y);
			statement.setInt(3, z);
			statement.setString(4, name);
			statement.execute();
			final int count = statement.getUpdateCount();
			statement.close();
			if (count == 0)
			{
				activeChar.sendSysMessage("Character not found or position unaltered.");
			}
			else
			{
				activeChar.sendSysMessage("Player's [" + name + "] position is now set to (" + x + "," + y + "," + z + ").");
			}
		}
		catch (SQLException se)
		{
			activeChar.sendSysMessage("SQLException while changing offline character's position");
		}
	}
	
	/**
	 * Recalls a targeted NPC to the current player's location.<br>
	 * This method handles standard NPCs, RaidBosses, and GrandBosses differently.<br>
	 * It deletes the existing spawn and creates a new one at the player's position.
	 * @param activeChar The {@code Player} who is executing the command.
	 */
	private void recallNPC(Player activeChar)
	{
		final WorldObject obj = activeChar.getTarget();
		if ((obj instanceof Npc) && !obj.asNpc().isMinion() && !(obj instanceof RaidBoss) && !(obj instanceof GrandBoss))
		{
			final Npc target = obj.asNpc();
			final int monsterTemplate = target.getTemplate().getId();
			final NpcTemplate template1 = NpcData.getInstance().getTemplate(monsterTemplate);
			if (template1 == null)
			{
				activeChar.sendSysMessage("Incorrect monster template.");
				LOGGER.warning("ERROR: NPC " + target.getObjectId() + " has a 'null' template.");
				return;
			}
			
			Spawn spawn = target.getSpawn();
			if (spawn == null)
			{
				activeChar.sendSysMessage("Incorrect monster spawn.");
				LOGGER.warning("ERROR: NPC " + target.getObjectId() + " has a 'null' spawn.");
				return;
			}
			
			final int respawnTime = spawn.getRespawnDelay() / 1000;
			target.deleteMe();
			spawn.stopRespawn();
			SpawnData.getInstance().deleteSpawn(spawn);
			
			try
			{
				spawn = new Spawn(template1);
				spawn.setXYZ(activeChar);
				spawn.setAmount(1);
				spawn.setHeading(activeChar.getHeading());
				spawn.setRespawnDelay(respawnTime);
				if (activeChar.isInInstance())
				{
					spawn.setInstanceId(activeChar.getInstanceId());
				}
				
				SpawnData.getInstance().addNewSpawn(spawn);
				spawn.init();
				if (respawnTime <= 0)
				{
					spawn.stopRespawn();
				}
				
				activeChar.sendSysMessage("Created " + template1.getName() + " on " + target.getObjectId() + ".");
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Target is not in game.");
			}
		}
		else if (obj instanceof RaidBoss)
		{
			final RaidBoss target = (RaidBoss) obj;
			final Spawn spawn = target.getSpawn();
			final double curHP = target.getCurrentHp();
			final double curMP = target.getCurrentMp();
			if (spawn == null)
			{
				activeChar.sendSysMessage("Incorrect raid spawn.");
				LOGGER.warning("ERROR: NPC Id" + target.getId() + " has a 'null' spawn.");
				return;
			}
			
			DatabaseSpawnManager.getInstance().deleteSpawn(spawn, true);
			try
			{
				final Spawn spawnDat = new Spawn(target.getId());
				spawnDat.setXYZ(activeChar);
				spawnDat.setAmount(1);
				spawnDat.setHeading(activeChar.getHeading());
				spawnDat.setRespawnMinDelay(43200);
				spawnDat.setRespawnMaxDelay(129600);
				
				DatabaseSpawnManager.getInstance().addNewSpawn(spawnDat, 0, curHP, curMP, true);
			}
			catch (Exception e)
			{
				activeChar.sendPacket(SystemMessageId.YOUR_TARGET_CANNOT_BE_FOUND);
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
		}
	}
}
