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

import java.awt.Color;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.ExServerPrimitive;

/**
 * This class handles administrative commands for controlling doors.<br>
 * It allows administrators to open or close specific doors by ID, all doors at once, or the currently selected door.<br>
 * It also draws the collision volume of nearby doors, so wrong door data can be seen in the world.
 */
public class AdminDoorControl implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminDoorControl.class.getName());
	
	private static final Map<Player, Set<Integer>> PLAYER_SHOWN_DOORS = new ConcurrentHashMap<>();
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_open",
		"admin_close",
		"admin_openall",
		"admin_closeall",
		"admin_showdoors",
	};
	
	/**
	 * Processes admin commands related to door manipulation.<br>
	 * This method handles opening, closing, and showing doors based on the provided command string.<br>
	 * It interacts with {@code DoorData}, {@code CastleManager}, and the target of the {@code Player}.
	 * @param command The command string entered by the player.
	 * @param activeChar The {@code Player} who executed the command.
	 * @return Always returns {@code true} after attempting to process the command.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		try
		{
			if (command.startsWith("admin_open "))
			{
				final int doorId = Integer.parseInt(command.substring(11));
				final Door door = DoorData.getInstance().getDoor(doorId);
				if (door != null)
				{
					door.openMe();
				}
				else
				{
					for (Castle castle : CastleManager.getInstance().getCastles())
					{
						final Door castleDoor = castle.getDoor(doorId);
						if (castleDoor != null)
						{
							castleDoor.openMe();
						}
					}
				}
			}
			else if (command.startsWith("admin_close "))
			{
				final int doorId = Integer.parseInt(command.substring(12));
				final Door door = DoorData.getInstance().getDoor(doorId);
				if (door != null)
				{
					door.closeMe();
				}
				else
				{
					for (Castle castle : CastleManager.getInstance().getCastles())
					{
						final Door castleDoor = castle.getDoor(doorId);
						if (castleDoor != null)
						{
							castleDoor.closeMe();
						}
					}
				}
			}
			else if (command.equals("admin_closeall"))
			{
				for (Door door : DoorData.getInstance().getDoors())
				{
					door.closeMe();
				}
				
				for (Castle castle : CastleManager.getInstance().getCastles())
				{
					for (Door door : castle.getDoors())
					{
						door.closeMe();
					}
				}
			}
			else if (command.equals("admin_openall"))
			{
				for (Door door : DoorData.getInstance().getDoors())
				{
					door.openMe();
				}
				
				for (Castle castle : CastleManager.getInstance().getCastles())
				{
					for (Door door : castle.getDoors())
					{
						door.openMe();
					}
				}
			}
			else if (command.equals("admin_open"))
			{
				final WorldObject target = activeChar.getTarget();
				if ((target != null) && target.isDoor())
				{
					target.asDoor().openMe();
				}
				else
				{
					activeChar.sendSysMessage("Incorrect target.");
				}
			}
			else if (command.equals("admin_close"))
			{
				final WorldObject target = activeChar.getTarget();
				if ((target != null) && target.isDoor())
				{
					target.asDoor().closeMe();
				}
				else
				{
					activeChar.sendSysMessage("Incorrect target.");
				}
			}
			else if (command.contains("admin_showdoors"))
			{
				if (command.contains("off"))
				{
					final Set<Integer> doorIds = PLAYER_SHOWN_DOORS.get(activeChar);
					if (doorIds == null)
					{
						return true;
					}
					
					for (int doorId : doorIds)
					{
						final int x = activeChar.getX();
						final int y = activeChar.getY();
						final ExServerPrimitive exsp = new ExServerPrimitive("Door" + doorId, x, y, -16000);
						exsp.addLine(Color.BLACK, x, y, -16000, x, y, -16000);
						activeChar.sendPacket(exsp);
					}
					
					doorIds.clear();
					PLAYER_SHOWN_DOORS.remove(activeChar);
				}
				else
				{
					final Set<Integer> doorIds = PLAYER_SHOWN_DOORS.computeIfAbsent(activeChar, _ -> new HashSet<>());
					
					World.forEachVisibleObject(activeChar, Door.class, door ->
					{
						final int dId = door.getId();
						final boolean firstSighting = doorIds.add(dId);
						
						final ExServerPrimitive packet = new ExServerPrimitive("Door" + dId, activeChar.getX(), activeChar.getY(), -16000);
						final Color color = door.isOpen() ? Color.GREEN : Color.RED;
						final int zMin = door.getZMin();
						final int zMax = door.getZMax();
						
						// Floor and ceiling outlines.
						for (int i = 0; i < 4; i++)
						{
							final int j = (i + 1) < 4 ? i + 1 : 0;
							packet.addLine(color, door.getX(i), door.getY(i), zMin, door.getX(j), door.getY(j), zMin);
							packet.addLine(color, door.getX(i), door.getY(i), zMax, door.getX(j), door.getY(j), zMax);
						}
						
						// Corner posts, numbered so a scrambled node order shows up.
						for (int i = 0; i < 4; i++)
						{
							packet.addLine(color, door.getX(i), door.getY(i), zMin, door.getX(i), door.getY(i), zMax);
							packet.addPoint(String.valueOf(i + 1), color, true, door.getX(i), door.getY(i), zMin);
						}
						
						// The door's own data, written above it.
						packet.addPoint(dId + " h=" + (zMax - zMin) + " z=" + zMin + ".." + zMax, color, true, door.getX(), door.getY(), zMax);
						activeChar.sendPacket(packet);
						
						// Announce each door once, not on every refresh.
						if (firstSighting)
						{
							activeChar.sendSysMessage("Found door " + dId);
						}
					});
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Problem with AdminDoorControl: " + e.getMessage());
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return An array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
