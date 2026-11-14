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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.enums.BroochJewel;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Handles the logic for the {@code BeastSoulShot} item.<br>
 * This class manages how the item interacts with players and summons when used.
 * @author Tempy
 */
public class BeastSoulShot implements IItemHandler
{
	/**
	 * Handles the logic for using a Soulshot item on a pet or servitor.<br>
	 * Validates if the user is a {@code Player} with an active, living summon.<br>
	 * Checks if there are enough shots available to charge the required skills.<br>
	 * Consumes the items and triggers the visual effects for the soulshot.
	 * @param playable The entity attempting to use the item.
	 * @param item The {@code Item} being used.
	 * @param forceUse A boolean flag indicating if the use is forced.
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
		
		final Player activeOwner = playable.asPlayer();
		if (!activeOwner.hasSummon())
		{
			activeOwner.sendPacket(SystemMessageId.SERVITORS_ARE_NOT_AVAILABLE_AT_THIS_TIME);
			return false;
		}
		
		final Summon pet = playable.getPet();
		if ((pet != null) && pet.isDead())
		{
			activeOwner.sendPacket(SystemMessageId.A_DEAD_PET_SERVITOR_CANNOT_USE_SOULSHOTS_SPIRITSHOTS);
			return false;
		}
		
		final List<Summon> aliveServitor = new ArrayList<>();
		for (Summon s : playable.getServitors().values())
		{
			if (!s.isDead())
			{
				aliveServitor.add(s);
			}
		}
		
		if ((pet == null) && aliveServitor.isEmpty())
		{
			activeOwner.sendPacket(SystemMessageId.A_DEAD_PET_SERVITOR_CANNOT_USE_SOULSHOTS_SPIRITSHOTS);
			return false;
		}
		
		final int itemId = item.getId();
		final long shotCount = item.getCount();
		final List<ItemSkillHolder> skills = item.getTemplate().getSkills(ItemSkillType.NORMAL);
		short shotConsumption = 0;
		if ((pet != null) && !pet.isChargedShot(ShotType.SOULSHOTS))
		{
			shotConsumption += pet.getSoulShotsPerHit();
		}
		
		for (Summon servitors : aliveServitor)
		{
			if (!servitors.isChargedShot(ShotType.SOULSHOTS))
			{
				shotConsumption += servitors.getSoulShotsPerHit();
			}
		}
		
		if (skills == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": is missing skills!");
			return false;
		}
		
		if (shotCount < shotConsumption)
		{
			// Not enough Soulshots to use.
			if (!activeOwner.disableAutoShot(itemId))
			{
				activeOwner.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_SOULSHOTS_NEEDED_FOR_A_SERVITOR);
			}
			
			return false;
		}
		
		// If the player doesn't have enough beast soulshot remaining, remove any auto soulshot task.
		if (!activeOwner.destroyItem(ItemProcessType.NONE, item.getObjectId(), shotConsumption, null, false))
		{
			if (!activeOwner.disableAutoShot(itemId))
			{
				activeOwner.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_SOULSHOTS_NEEDED_FOR_A_SERVITOR);
			}
			
			return false;
		}
		
		// Pet uses the power of spirit.
		if ((pet != null) && !pet.isChargedShot(ShotType.SOULSHOTS))
		{
			activeOwner.sendMessage("Your pet uses soulshot."); // activeOwner.sendPacket(SystemMessageId.YOUR_PET_USES_SPIRITSHOT);
			pet.chargeShot(ShotType.SOULSHOTS);
			
			// Visual effect change if player has equipped Ruby level 3 or higher.
			final BroochJewel ruby = activeOwner.getActiveRubyJewel();
			if (ruby != null)
			{
				activeOwner.broadcastSkillPacket(new MagicSkillUse(pet, pet, ruby.getSkillId(), ruby.getSkillLevel(), 0, 0), pet);
			}
			else
			{
				skills.forEach(holder -> activeOwner.broadcastSkillPacket(new MagicSkillUse(pet, pet, holder.getSkillId(), holder.getSkillLevel(), 0, 0), pet));
			}
		}
		
		aliveServitor.forEach(s ->
		{
			if (!s.isChargedShot(ShotType.SOULSHOTS))
			{
				activeOwner.sendMessage("Your servitor uses soulshot."); // activeOwner.sendPacket(SystemMessageId.YOUR_PET_USES_SPIRITSHOT);
				s.chargeShot(ShotType.SOULSHOTS);
				
				// Visual effect change if player has equipped Ruby level 3 or higher.
				final BroochJewel ruby = activeOwner.getActiveRubyJewel();
				if (ruby != null)
				{
					activeOwner.broadcastSkillPacket(new MagicSkillUse(s, s, ruby.getSkillId(), ruby.getSkillLevel(), 0, 0), s);
				}
				else
				{
					skills.forEach(holder -> activeOwner.broadcastSkillPacket(new MagicSkillUse(s, s, holder.getSkillId(), holder.getSkillLevel(), 0, 0), s));
				}
			}
		});
		
		return true;
	}
}
