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
package handlers.skill.effects;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PetItemList;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the restoration effect logic.<br>
 * It manages how health or other resources are recovered for a {@code Creature}.
 * @author Zoey76, Mobius
 */
public class Restoration extends AbstractEffect
{
	private final int _itemId;
	private final long _itemCount;
	private final int _itemEnchantmentLevel;
	
	/**
	 * Creates a new {@code Restoration} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It extracts the item ID, count, and enchantment level.
	 * @param params The {@code StatSet} containing the required configuration data.
	 */
	public Restoration(StatSet params)
	{
		_itemId = params.getInt("itemId", 0);
		_itemCount = params.getLong("itemCount", 0);
		_itemEnchantmentLevel = params.getInt("itemEnchantmentLevel", 0);
	}
	
	/**
	 * Checks if the effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant restoration of an item to a target.<br>
	 * This method checks if the target is playable and contains valid item data.<br>
	 * It adds the specified {@code Item} to either a player or their pet.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the restoration.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} being processed.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayable())
		{
			return;
		}
		
		if ((_itemId <= 0) || (_itemCount <= 0))
		{
			effected.sendPacket(SystemMessageId.FAILED_TO_CHANGE_THE_ITEM);
			LOGGER.warning(Restoration.class.getSimpleName() + " effect with wrong item Id/count: " + _itemId + "/" + _itemCount + "!");
			return;
		}
		
		if (effected.isPlayer())
		{
			final Item newItem = effected.asPlayer().addItem(ItemProcessType.REWARD, _itemId, _itemCount, effector, true);
			if (_itemEnchantmentLevel > 0)
			{
				newItem.setEnchantLevel(_itemEnchantmentLevel);
			}
		}
		else if (effected.isPet())
		{
			final Player target = effected.asPlayer();
			final Item newItem = effected.getInventory().addItem(ItemProcessType.REWARD, _itemId, _itemCount, target, effector);
			if (_itemEnchantmentLevel > 0)
			{
				newItem.setEnchantLevel(_itemEnchantmentLevel);
			}
			
			target.sendPacket(new PetItemList(effected.getInventory().getItems()));
		}
	}
	
	/**
	 * Retrieves the type of effect associated with this restoration.<br>
	 * This method returns a constant value representing item extraction.
	 * @return the {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.EXTRACT_ITEM;
	}
}
