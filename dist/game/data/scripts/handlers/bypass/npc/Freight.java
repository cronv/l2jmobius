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
package handlers.bypass.npc;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerFreight;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PackageToList;
import org.l2jmobius.gameserver.network.serverpackets.WareHouseWithdrawalList;

/**
 * Handles the bypass logic for {@code PlayerFreight} interactions.<br>
 * This class manages how players interact with freight containers in the game world.
 * @author UnAfraid
 */
public class Freight implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"package_withdraw",
		"package_deposit"
	};
	
	/**
	 * Handles specific commands for the freight system.<br>
	 * This method processes requests from a {@code Player}.<br>
	 * It checks if the target is an NPC before executing logic.
	 * @param command The name of the command to execute.
	 * @param player The {@code Player} who sent the command.
	 * @param target The {@code Creature} being interacted with.
	 * @return Always returns {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		if (command.equalsIgnoreCase(COMMANDS[0]))
		{
			final PlayerFreight freight = player.getFreight();
			if (freight != null)
			{
				if (freight.getSize() > 0)
				{
					player.setActiveWarehouse(freight);
					for (Item i : player.getActiveWarehouse().getItems())
					{
						if (i.isTimeLimitedItem() && (i.getRemainingTime() <= 0))
						{
							player.getActiveWarehouse().destroyItem(ItemProcessType.DESTROY, i, player, null);
						}
					}
					
					player.sendPacket(new WareHouseWithdrawalList(1, player, WareHouseWithdrawalList.FREIGHT));
					player.sendPacket(new WareHouseWithdrawalList(2, player, WareHouseWithdrawalList.FREIGHT));
				}
				else
				{
					player.sendPacket(SystemMessageId.YOU_HAVE_NOT_DEPOSITED_ANY_ITEMS_IN_YOUR_WAREHOUSE);
				}
			}
		}
		else if (command.equalsIgnoreCase(COMMANDS[1]))
		{
			if (player.getAccountChars().isEmpty())
			{
				player.sendPacket(SystemMessageId.THAT_CHARACTER_DOES_NOT_EXIST);
			}
			else
			{
				player.sendPacket(new PackageToList(player.getAccountChars()));
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves the list of available commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the allowed commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
