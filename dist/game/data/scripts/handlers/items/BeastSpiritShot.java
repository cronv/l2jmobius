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
 * Handles the logic for the Beast SpiritShot item.<br>
 * This class manages how the item interacts with players and summons in the game world.
 * @author Tempy, Mobius
 */
public class BeastSpiritShot implements IItemHandler
{
	/**
	 * Handles the logic for using a SpiritShot item on a pet or servitor.<br>
	 * This method checks if the user has an active summon and enough shots to proceed.<br>
	 * It consumes the required amount of items and charges the shot for all eligible summons.
	 * @param playable The entity attempting to use the item.
	 * @param item The specific item instance being used.
	 * @param forceUse A flag indicating if the use should be forced.
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
		final boolean isBlessed = ((itemId == 6647) || (itemId == 20334) || (itemId == 35671)); // TODO: Unhardcode these!
		final List<ItemSkillHolder> skills = item.getTemplate().getSkills(ItemSkillType.NORMAL);
		final ShotType shotType = isBlessed ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS;
		short shotConsumption = 0;
		if ((pet != null) && !pet.isChargedShot(shotType))
		{
			shotConsumption += pet.getSpiritShotsPerHit();
		}
		
		for (Summon servitors : aliveServitor)
		{
			if (!servitors.isChargedShot(shotType))
			{
				shotConsumption += servitors.getSpiritShotsPerHit();
			}
		}
		
		if (skills == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": is missing skills!");
			return false;
		}
		
		final long shotCount = item.getCount();
		if (shotCount < shotConsumption)
		{
			// Not enough SpiritShots to use.
			if (!activeOwner.disableAutoShot(itemId))
			{
				activeOwner.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_SPIRITSHOTS_NEEDED_FOR_A_PET_SERVITOR);
			}
			
			return false;
		}
		
		if (!activeOwner.destroyItem(ItemProcessType.NONE, item.getObjectId(), shotConsumption, null, false))
		{
			if (!activeOwner.disableAutoShot(itemId))
			{
				activeOwner.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_SPIRITSHOTS_NEEDED_FOR_A_PET_SERVITOR);
			}
			
			return false;
		}
		
		// Pet uses the power of spirit.
		if ((pet != null) && !pet.isChargedShot(shotType))
		{
			activeOwner.sendMessage(isBlessed ? "Your pet uses blessed spiritshot." : "Your pet uses spiritshot."); // activeOwner.sendPacket(SystemMessageId.YOUR_PET_USES_SPIRITSHOT);
			pet.chargeShot(shotType);
			
			// Visual effect change if player has equipped Sapphire level 3 or higher.
			final BroochJewel sapphire = activeOwner.getActiveSapphireJewel();
			if (sapphire != null)
			{
				activeOwner.broadcastSkillPacket(new MagicSkillUse(pet, pet, sapphire.getSkillId(), sapphire.getSkillLevel(), 0, 0), pet);
			}
			else
			{
				skills.forEach(holder -> activeOwner.broadcastSkillPacket(new MagicSkillUse(pet, pet, holder.getSkillId(), holder.getSkillLevel(), 0, 0), pet));
			}
		}
		
		aliveServitor.forEach(s ->
		{
			if (!s.isChargedShot(shotType))
			{
				activeOwner.sendMessage(isBlessed ? "Your servitor uses blessed spiritshot." : "Your servitor uses spiritshot."); // activeOwner.sendPacket(SystemMessageId.YOUR_PET_USES_SPIRITSHOT);
				s.chargeShot(shotType);
				
				// Visual effect change if player has equipped Sapphire level 3 or higher.
				final BroochJewel sapphire = activeOwner.getActiveSapphireJewel();
				if (sapphire != null)
				{
					activeOwner.broadcastSkillPacket(new MagicSkillUse(s, s, sapphire.getSkillId(), sapphire.getSkillLevel(), 0, 0), s);
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
