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
package handlers.bypass.npc;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.network.serverpackets.equipmentupgrade.ExShowUpgradeSystem;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * Handles the bypass logic for the equipment upgrade system.<br>
 * This class manages interactions when a player triggers an upgrade-related NPC action.<br>
 * It ensures that the correct {@code ExShowUpgradeSystem} packets are sent to the client.
 * @author Mobius
 */
public class UpgradeEquipment implements IBypassHandler
{
	private static final int[] HEAD_BLACKSMITH =
	{
		30847, // Ferris (Aden)
		31272, // Noel (Goddard)
		30512, // Kusto (Giran)
		30595, // Opix (Dion)
		31317, // Lombert (Rune)
		30897, // Roman (Heine)
		31961, // Newyear (Schuttgart)
		30677, // Flutter (Oren)
		30499, // Tapoy (Gludin)
		30687, // Vergara (Hunters)
	};
	
	private static final String[] COMMANDS =
	{
		"UpgradeEquipment"
	};
	
	/**
	 * Handles the command to show the equipment upgrade system.<br>
	 * This method checks if the {@code target} is the correct NPC.<br>
	 * If valid, it sends a packet to the {@code player}.
	 * @param command The name of the command being executed.
	 * @param player The {@code Player} who sent the command.
	 * @param target The {@code Creature} being interacted with.
	 * @return {@code true} if the command was successful, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if ((target == null) || !target.isNpc() || !ArrayUtil.contains(HEAD_BLACKSMITH, target.asNpc().getId()))
		{
			return false;
		}
		
		player.sendPacket(new ExShowUpgradeSystem());
		return true;
	}
	
	/**
	 * Retrieves the list of valid commands for this handler.<br>
	 * This method returns the {@code COMMANDS} array.
	 * @return an array of {@code String} containing all supported commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
