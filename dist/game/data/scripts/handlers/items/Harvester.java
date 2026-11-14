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

import java.util.List;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * Handles the logic for harvesting items from world objects.<br>
 * This class manages how {@code Player} entities interact with harvestable resources.
 * @author l3x
 */
public class Harvester implements IItemHandler
{
	/**
	 * Handles the logic for using an item on a target.<br>
	 * This method checks if the user is allowed to use the item.<br>
	 * It also verifies that the target is a dead monster.
	 * @param playable The entity attempting to use the item.
	 * @param item The item being used.
	 * @param forceUse A flag indicating if the use should be forced.
	 * @return {@code true} if the item was used successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!GeneralConfig.ALLOW_MANOR)
		{
			return false;
		}
		else if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_PET);
			return false;
		}
		
		final List<ItemSkillHolder> skills = item.getTemplate().getSkills(ItemSkillType.NORMAL);
		if (skills == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": is missing skills!");
			return false;
		}
		
		final Player player = playable.asPlayer();
		final WorldObject target = player.getTarget();
		if ((target == null) || !target.isMonster() || !target.asCreature().isDead())
		{
			player.sendPacket(SystemMessageId.INVALID_TARGET);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		skills.forEach(holder -> player.useMagic(holder.getSkill(), item, false, false));
		return true;
	}
}
