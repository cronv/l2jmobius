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
package handlers.items;

import org.l2jmobius.gameserver.data.holders.PetDataHolder;
import org.l2jmobius.gameserver.data.xml.PetData;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.PetItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the logic for items that can summon pets or other entities.<br>
 * This class manages how {@code SummonItems} interact with players and the game world.
 * @author HorridoJoho, UnAfraid
 */
public class SummonItems extends ItemSkillsTemplate
{
	/**
	 * Handles the logic for using a summon item.<br>
	 * This method checks if the player is allowed to use the item based on their current state.<br>
	 * It validates conditions like combat status, pet ownership, and trade status.
	 * @param playable The {@code Playable} entity attempting to use the item.
	 * @param item The {@code Item} being used.
	 * @param forceUse A boolean indicating if the use is forced.
	 * @return {@code true} if the item was successfully used, {@code false} otherwise.
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
		if (!player.getClient().getFloodProtectors().canUsePetSummonItem() || (player.getBlockCheckerArena() != -1) || player.getObserverMode().isActive() || player.isAllSkillsDisabled() || player.isCastingNow())
		{
			return false;
		}
		
		if (player.isSitting())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_MOVE_WHILE_SITTING);
			return false;
		}
		
		if (player.hasPet() || player.isMounted())
		{
			player.sendPacket(SystemMessageId.YOU_ALREADY_HAVE_A_PET);
			return false;
		}
		
		if (player.isAttackingNow())
		{
			player.sendPacket(SystemMessageId.CANNOT_BE_SUMMONED_WHILE_IN_COMBAT);
			return false;
		}
		
		if ((player.getActiveTradeList() != null) || player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.CANNOT_BE_SUMMONED_WHILE_TRADING);
			return false;
		}
		
		final PetDataHolder petData = PetData.getInstance().getPetDataHolderByItemId(item.getId());
		if ((petData == null) || (petData.getNpcId() == -1))
		{
			return false;
		}
		
		player.addScript(new PetItemHolder(item));
		return super.onItemUse(playable, item, forceUse);
	}
}
