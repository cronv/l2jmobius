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
package org.l2jmobius.gameserver.entity.item.enchant;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.EnchantItemData;
import org.l2jmobius.gameserver.data.xml.EnchantItemGroupsData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents an item used to apply enchantments to other equipment.<br>
 * This class handles the logic for processing enchantment scrolls within the game.
 * @author UnAfraid, Mobius
 */
public class EnchantScroll extends AbstractEnchantItem
{
	private final boolean _isWeapon;
	private final boolean _isBlessed;
	private final boolean _isBlessedDown;
	private final boolean _isSafe;
	private final boolean _isGiant;
	private final int _scrollGroupId;
	private final Map<Integer, Integer> _items = new HashMap<>();
	
	/**
	 * Creates a new {@code EnchantScroll} instance using the provided stats.<br>
	 * This constructor initializes specific scroll properties based on the item type.<br>
	 * It sets flags for weapon, blessed, safe, and giant attributes.
	 * @param set The {@code StatSet} containing the scroll data.
	 */
	public EnchantScroll(StatSet set)
	{
		super(set);
		_scrollGroupId = set.getInt("scrollGroupId", 0);
		
		final ItemType type = getItem().getItemType();
		_isWeapon = (type == EtcItemType.ENCHT_ATTR_ANCIENT_CRYSTAL_ENCHANT_WP) || (type == EtcItemType.BLESS_ENCHT_WP) || (type == EtcItemType.ENCHT_WP) || (type == EtcItemType.GIANT_ENCHT_WP);
		_isBlessed = (type == EtcItemType.BLESS_ENCHT_AM) || (type == EtcItemType.BLESS_ENCHT_WP) || (type == EtcItemType.BLESSED_ENCHT_ATTR_INC_PROP_ENCHT_WP) || (type == EtcItemType.BLESSED_ENCHT_ATTR_INC_PROP_ENCHT_AM) || (type == EtcItemType.BLESSED_GIANT_ENCHT_ATTR_INC_PROP_ENCHT_AM) || (type == EtcItemType.BLESSED_GIANT_ENCHT_ATTR_INC_PROP_ENCHT_WP);
		_isBlessedDown = (type == EtcItemType.BLESS_ENCHT_AM_DOWN);
		_isSafe = (type == EtcItemType.ENCHT_ATTR_ANCIENT_CRYSTAL_ENCHANT_AM) || (type == EtcItemType.ENCHT_ATTR_ANCIENT_CRYSTAL_ENCHANT_WP) || (type == EtcItemType.ENCHT_ATTR_CRYSTAL_ENCHANT_AM) || (type == EtcItemType.ENCHT_ATTR_CRYSTAL_ENCHANT_WP);
		_isGiant = (type == EtcItemType.GIANT_ENCHT_AM) || (type == EtcItemType.GIANT_ENCHT_WP);
	}
	
	/**
	 * Checks if this scroll is intended for weapons.
	 * @return {@code true} if the scroll is a weapon enchant, {@code false} otherwise.
	 */
	@Override
	public boolean isWeapon()
	{
		return _isWeapon;
	}
	
	/**
	 * Checks if this scroll is a blessed type.
	 * @return {@code true} if the scroll is blessed, {@code false} otherwise.
	 */
	public boolean isBlessed()
	{
		return _isBlessed;
	}
	
	/**
	 * Checks if the scroll is a Blessed Down type.<br>
	 * This indicates whether the enchantment reduces the item's level.
	 * @return {@code true} if it is a Blessed Down scroll, {@code false} otherwise.
	 */
	public boolean isBlessedDown()
	{
		return _isBlessedDown;
	}
	
	/**
	 * Checks if the scroll is a safe enchantment.<br>
	 * This method returns {@code true} if the scroll is marked as safe.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if the scroll is safe, {@code false} otherwise.
	 */
	public boolean isSafe()
	{
		return _isSafe;
	}
	
	/**
	 * Checks if this scroll is a giant type.
	 * @return {@code true} if the scroll is a giant type, {@code false} otherwise.
	 */
	public boolean isGiant()
	{
		return _isGiant;
	}
	
	/**
	 * Adds a specific item to the scroll's list.<br>
	 * It maps the {@code itemId} to a group ID.<br>
	 * If the provided {@code scrollGroupId} is less than 0, it uses the default value.
	 * @param itemId The unique identifier of the item to add.
	 * @param scrollGroupId The group ID for the item, or -1 to use the default.
	 */
	public void addItem(int itemId, int scrollGroupId)
	{
		_items.put(itemId, scrollGroupId > -1 ? scrollGroupId : _scrollGroupId);
	}
	
	/**
	 * Retrieves the list of item IDs associated with this scroll.<br>
	 * This method returns all keys from the internal {@code _items} map.
	 * @return a {@code Collection<Integer>} containing the item IDs.
	 */
	public Collection<Integer> getItems()
	{
		return _items.keySet();
	}
	
	/**
	 * Checks if an item can be enchanted using this scroll.<br>
	 * It verifies compatibility between the items and any support objects.<br>
	 * The method ensures that specific requirements like blessed or giant status match.
	 * @param itemToEnchant The {@code Item} that is being enchanted.
	 * @param supportItem The {@code EnchantSupportItem} providing extra effects.
	 * @return {@code true} if the enchantment is valid, {@code false} otherwise.
	 */
	@Override
	public boolean isValid(Item itemToEnchant, EnchantSupportItem supportItem)
	{
		if (!_items.isEmpty() && !_items.containsKey(itemToEnchant.getId()))
		{
			return false;
		}
		else if (supportItem != null)
		{
			final boolean blessed = isBlessed();
			if ((blessed && !supportItem.isBlessed()) || (!blessed && supportItem.isBlessed()))
			{
				return false;
			}
			else if ((isGiant() && !supportItem.isGiant()) || (!isGiant() && supportItem.isGiant()))
			{
				return false;
			}
			else if (!supportItem.isValid(itemToEnchant, supportItem))
			{
				return false;
			}
			else if (supportItem.isWeapon() != isWeapon())
			{
				return false;
			}
		}
		
		if (_items.isEmpty())
		{
			for (EnchantScroll scroll : EnchantItemData.getInstance().getScrolls())
			{
				if (scroll.getId() == getId())
				{
					continue;
				}
				
				final Collection<Integer> scrollItems = scroll.getItems();
				if (!scrollItems.isEmpty() && scrollItems.contains(itemToEnchant.getId()))
				{
					return false;
				}
			}
		}
		
		return super.isValid(itemToEnchant, supportItem);
	}
	
	/**
	 * Calculates the success chance for enchanting a specific item.<br>
	 * This method checks the scroll group and the current enchant level of the item.<br>
	 * It returns {@code 100} if the enchantment is safe.
	 * @param player The {@code Player} attempting the enchantment.
	 * @param enchantItem The {@code Item} being enchanted.
	 * @return The success chance as a double, or {@code -1} if an error occurs.
	 */
	public double getChance(Player player, Item enchantItem)
	{
		if (enchantItem == null)
		{
			return -1;
		}
		
		final int scrollGroupId = _items.getOrDefault(enchantItem.getId(), _scrollGroupId);
		if (EnchantItemGroupsData.getInstance().getScrollGroup(scrollGroupId) == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Unexistent enchant scroll group specified for enchant scroll: " + getId());
			return -1;
		}
		
		final EnchantItemGroup group = EnchantItemGroupsData.getInstance().getItemGroup(enchantItem.getTemplate(), scrollGroupId);
		if (group == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Couldn't find enchant item group for scroll: " + getId() + " requested by: " + player);
			return -1;
		}
		
		final int safeEnchant = getSafeEnchant();
		if ((safeEnchant > 0) && (enchantItem.getEnchantLevel() < safeEnchant))
		{
			return 100;
		}
		
		return group.getChance(enchantItem.getEnchantLevel());
	}
	
	/**
	 * Determines the outcome of an enchantment attempt.<br>
	 * It checks if the items are valid and calculates the final success chance.<br>
	 * The result considers player stats, scroll bonuses, and support items.
	 * @param player The {@code Player} attempting the enchantment.
	 * @param enchantItem The {@code Item} being enchanted.
	 * @param supportItem The optional {@code EnchantSupportItem} used to boost success rates.
	 * @return The {@code EnchantResultType} of the operation.
	 */
	public EnchantResultType calculateSuccess(Player player, Item enchantItem, EnchantSupportItem supportItem)
	{
		if (!isValid(enchantItem, supportItem))
		{
			return EnchantResultType.ERROR;
		}
		
		final double chance = getChance(player, enchantItem);
		if (chance == -1)
		{
			return EnchantResultType.ERROR;
		}
		
		final int crystalLevel = enchantItem.getTemplate().getCrystalType().getLevel();
		final double enchantRateStat = (crystalLevel > CrystalType.NONE.getLevel()) && (crystalLevel < CrystalType.EVENT.getLevel()) ? player.getStat().getValue(Stat.ENCHANT_RATE) : 0;
		final double bonusRate = getBonusRate();
		final double supportBonusRate = (supportItem != null) ? supportItem.getBonusRate() : 0;
		final double finalChance = Math.min(chance + bonusRate + supportBonusRate + enchantRateStat, 100);
		final double random = 100 * Rnd.nextDouble();
		final boolean success = (random < finalChance) || player.tryLuck();
		return success ? EnchantResultType.SUCCESS : EnchantResultType.FAILURE;
	}
}
