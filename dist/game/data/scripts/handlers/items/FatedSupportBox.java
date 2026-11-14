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
package handlers.items;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles the logic for the {@code FatedSupportBox} item.<br>
 * This class manages what happens when a player interacts with or uses this specific box.<br>
 * It ensures that rewards are distributed correctly to the {@code Player}.
 * @author Mobius
 */
public class FatedSupportBox implements IItemHandler
{
	// Items
	private static final int FATED_BOX_ERTHEIA_WIZARD = 26229;
	private static final int FATED_BOX_ERTHEIA_FIGHTER = 26230;
	private static final int FATED_BOX_FIGHTER = 37315;
	private static final int FATED_BOX_WIZARD = 37316;
	private static final int FATED_BOX_WARRIOR = 37317;
	private static final int FATED_BOX_ROGUE = 37318;
	private static final int FATED_BOX_KAMAEL = 37319;
	private static final int FATED_BOX_ORC_FIGHTER = 37320;
	private static final int FATED_BOX_ORC_WIZARD = 37321;
	
	/**
	 * Handles the logic for using a Fated Support Box.<br>
	 * Checks if the user is a {@code Player} and meets level or race requirements.<br>
	 * Rewards the player with a specific box based on their class and race.
	 * @param playable The entity attempting to use the item.
	 * @param item The item being used.
	 * @param forceUse Whether the use is forced by the system.
	 * @return {@code true} if the item was successfully processed, {@code false} otherwise.
	 */
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_PET);
			return false;
		}
		
		final Player player = playable.asPlayer();
		if (!player.isInventoryUnder80(false))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_ITEM_OWNERSHIP_LIMIT_AND_YOU_CANNOT_TAKE_THE_ITEM_CHECK_ITEM_OWNERSHIP_TIME_LIMITS_FOR_THE_INVENTORY_PLEASE);
			return false;
		}
		
		final Race race = player.getRace();
		// Characters that have gone through their 2nd class transfer/1st liberation will be able to open the Fated Support Box at level 40.
		final int playerLevel = player.getLevel();
		if ((playerLevel < 40) || player.isInCategory(CategoryType.FIRST_CLASS_GROUP) || ((race != Race.ERTHEIA) && player.isInCategory(CategoryType.SECOND_CLASS_GROUP)))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET).addItemName(item));
			return false;
		}
		
		player.getInventory().destroyItem(ItemProcessType.FEE, item, 1, player, null);
		player.sendInventoryUpdate(new InventoryUpdate(item));
		
		// It will stay in your inventory after use until you reach level 84.
		if (playerLevel > 84)
		{
			player.sendMessage("Fated Support Box was removed because your level has exceeded the maximum requirement."); // custom message
			return true;
		}
		
		final PlayerClass playerClass = player.getPlayerClass();
		switch (race)
		{
			case HUMAN:
			case ELF:
			case DARK_ELF:
			case DWARF:
			{
				final int playerClassId = playerClass.getId();
				if (player.isMageClass())
				{
					player.addItem(ItemProcessType.REWARD, FATED_BOX_WIZARD, 1, player, true);
				}
				else if (CategoryData.getInstance().isInCategory(CategoryType.SUB_GROUP_ROGUE, playerClassId))
				{
					player.addItem(ItemProcessType.REWARD, FATED_BOX_ROGUE, 1, player, true);
				}
				else if (CategoryData.getInstance().isInCategory(CategoryType.SUB_GROUP_KNIGHT, playerClassId))
				{
					player.addItem(ItemProcessType.REWARD, FATED_BOX_FIGHTER, 1, player, true);
				}
				else
				{
					player.addItem(ItemProcessType.REWARD, FATED_BOX_WARRIOR, 1, player, true);
				}
				break;
			}
			case ORC:
			{
				if (player.isMageClass())
				{
					player.addItem(ItemProcessType.REWARD, FATED_BOX_ORC_WIZARD, 1, player, true);
				}
				else
				{
					player.addItem(ItemProcessType.REWARD, FATED_BOX_ORC_FIGHTER, 1, player, true);
				}
				break;
			}
			case KAMAEL:
			{
				player.addItem(ItemProcessType.REWARD, FATED_BOX_KAMAEL, 1, player, true);
				break;
			}
			case ERTHEIA:
			{
				if (player.isMageClass())
				{
					player.addItem(ItemProcessType.REWARD, FATED_BOX_ERTHEIA_WIZARD, 1, player, true);
				}
				else
				{
					player.addItem(ItemProcessType.REWARD, FATED_BOX_ERTHEIA_FIGHTER, 1, player, true);
				}
				break;
			}
		}
		
		return true;
	}
}
