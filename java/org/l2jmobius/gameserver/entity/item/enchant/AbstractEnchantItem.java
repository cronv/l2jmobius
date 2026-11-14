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
package org.l2jmobius.gameserver.entity.item.enchant;

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class serves as a base for items that can be used to apply enchantments.<br>
 * It provides common functionality for handling enchantment logic and data.<br>
 * Subclasses should extend this class to define specific enchantment behaviors.
 * @author UnAfraid
 */
public abstract class AbstractEnchantItem
{
	protected static final Logger LOGGER = Logger.getLogger(AbstractEnchantItem.class.getName());
	
	private static final Set<EtcItemType> ENCHANT_TYPES = EnumSet.noneOf(EtcItemType.class);
	static
	{
		ENCHANT_TYPES.add(EtcItemType.ENCHT_ATTR_ANCIENT_CRYSTAL_ENCHANT_AM);
		ENCHANT_TYPES.add(EtcItemType.ENCHT_ATTR_ANCIENT_CRYSTAL_ENCHANT_WP);
		ENCHANT_TYPES.add(EtcItemType.BLESS_ENCHT_AM);
		ENCHANT_TYPES.add(EtcItemType.BLESS_ENCHT_AM_DOWN);
		ENCHANT_TYPES.add(EtcItemType.BLESS_ENCHT_WP);
		ENCHANT_TYPES.add(EtcItemType.ENCHT_AM);
		ENCHANT_TYPES.add(EtcItemType.ENCHT_WP);
		ENCHANT_TYPES.add(EtcItemType.ENCHT_WP_DOWN);
		ENCHANT_TYPES.add(EtcItemType.ENCHT_AM_DOWN);
		ENCHANT_TYPES.add(EtcItemType.GIANT_ENCHT_AM);
		ENCHANT_TYPES.add(EtcItemType.GIANT_ENCHT_WP);
		ENCHANT_TYPES.add(EtcItemType.ENCHT_ATTR_INC_PROP_ENCHT_AM);
		ENCHANT_TYPES.add(EtcItemType.ENCHT_ATTR_INC_PROP_ENCHT_WP);
		ENCHANT_TYPES.add(EtcItemType.GIANT_ENCHT_ATTR_INC_PROP_ENCHT_AM);
		ENCHANT_TYPES.add(EtcItemType.GIANT_ENCHT_ATTR_INC_PROP_ENCHT_WP);
		ENCHANT_TYPES.add(EtcItemType.BLESSED_ENCHT_ATTR_INC_PROP_ENCHT_AM);
		ENCHANT_TYPES.add(EtcItemType.BLESSED_ENCHT_ATTR_INC_PROP_ENCHT_WP);
		ENCHANT_TYPES.add(EtcItemType.BLESSED_GIANT_ENCHT_ATTR_INC_PROP_ENCHT_AM);
		ENCHANT_TYPES.add(EtcItemType.BLESSED_GIANT_ENCHT_ATTR_INC_PROP_ENCHT_WP);
	}
	
	private final int _id;
	private final CrystalType _grade;
	private final int _minEnchantLevel;
	private final int _maxEnchantLevel;
	private final int _safeEnchantLevel;
	private final int _randomEnchantMin;
	private final int _randomEnchantMax;
	private final int _randomEnchantChance;
	private final int[] _randomEnchantChances;
	private final double _bonusRate;
	
	/**
	 * Creates a new instance of an enchantment item.<br>
	 * This constructor initializes the internal fields from a {@code StatSet}.<br>
	 * It validates that the associated item belongs to a supported enchantment type.
	 * @param set The {@code StatSet} containing all required configuration data.
	 */
	public AbstractEnchantItem(StatSet set)
	{
		_id = set.getInt("id");
		final ItemTemplate item = getItem();
		if (item == null)
		{
			throw new NullPointerException();
		}
		else if (!ENCHANT_TYPES.contains(item.getItemType()))
		{
			throw new IllegalAccessError();
		}
		
		_grade = set.getEnum("targetGrade", CrystalType.class, CrystalType.NONE);
		_minEnchantLevel = set.getInt("minEnchant", 0);
		_maxEnchantLevel = set.getInt("maxEnchant", 127);
		_safeEnchantLevel = set.getInt("safeEnchant", 0);
		_randomEnchantMin = set.getInt("randomEnchantMin", 1);
		_randomEnchantMax = set.getInt("randomEnchantMax", _randomEnchantMin);
		_randomEnchantChance = set.getInt("randomEnchantChance", 50);
		
		// One chance per value of the randomEnchantMin to randomEnchantMax range, for example 70;20;10 over 1 to 3.
		_randomEnchantChances = set.getString("randomEnchantChances", null) == null ? null : set.getIntArray("randomEnchantChances", ";");
		_bonusRate = set.getDouble("bonusRate", 0);
	}
	
	/**
	 * Retrieves the unique identifier for this enchantment item.
	 * @return The {@code int} ID of the enchantment item.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the bonus rate for this enchantment item.<br>
	 * This value is used to calculate specific bonuses during the enchanting process.
	 * @return The current {@code double} value of the bonus rate.
	 */
	public double getBonusRate()
	{
		return _bonusRate;
	}
	
	/**
	 * Retrieves the {@code ItemTemplate} associated with this item.<br>
	 * It uses the internal ID to fetch data from {@code ItemData}.
	 * @return The {@code ItemTemplate} for this enchantment.
	 */
	public ItemTemplate getItem()
	{
		return ItemData.getInstance().getTemplate(_id);
	}
	
	/**
	 * Retrieves the grade of the enchantment crystal.<br>
	 * This value determines the quality level of the item.
	 * @return the {@code CrystalType} representing the current grade.
	 */
	public CrystalType getGrade()
	{
		return _grade;
	}
	
	/**
	 * @return {@code true} if scroll is for weapon, {@code false} for armor
	 */
	public abstract boolean isWeapon();
	
	/**
	 * Retrieves the minimum enchantment level for this item.<br>
	 * This value is used to determine the lowest possible level an item can reach.
	 * @return The minimum enchantment level as an {@code int}.
	 */
	public int getMinEnchantLevel()
	{
		return _minEnchantLevel;
	}
	
	/**
	 * Retrieves the maximum enchantment level allowed for this item.<br>
	 * This value is used to cap the enhancement process.
	 * @return The maximum enchantment level as an {@code int}.
	 */
	public int getMaxEnchantLevel()
	{
		return _maxEnchantLevel;
	}
	
	/**
	 * Retrieves the safe enchantment level for this item.<br>
	 * This value represents the maximum level that can be reached without risk of failure.
	 * @return The {@code int} value of the safe enchantment level.
	 */
	public int getSafeEnchant()
	{
		return _safeEnchantLevel;
	}
	
	/**
	 * Retrieves the minimum level for a random enchantment.<br>
	 * This value is used to determine the lower bound of an enchantment range.
	 * @return The minimum enchantment level as an {@code int}.
	 */
	public int getRandomEnchantMin()
	{
		return _randomEnchantMin;
	}
	
	/**
	 * Retrieves the maximum level for a random enchantment.<br>
	 * This value is used to determine the upper limit of the enchantment range.
	 * @return the maximum enchantment level as an {@code int}.
	 */
	public int getRandomEnchantMax()
	{
		return _randomEnchantMax;
	}
	
	/**
	 * Retrieves the random enchantment chance for this item.<br>
	 * This value is used to determine the success rate of an enchantment.
	 * @return The integer value representing the enchantment chance.
	 */
	public int getRandomEnchantChance()
	{
		return _randomEnchantChance;
	}
	
	/**
	 * Rolls the amount of enchant levels granted by a successful enchant.<br>
	 * When randomEnchantChances is set, each value of the random range is rolled with its own chance.<br>
	 * Otherwise randomEnchantChance decides between the minimum value and the whole range.
	 * @return The {@code int} amount of levels to add.
	 */
	public int getRandomEnchantValue()
	{
		if (_randomEnchantChances == null)
		{
			return Rnd.get(1, 100) >= _randomEnchantChance ? _randomEnchantMin : Rnd.get(_randomEnchantMin, _randomEnchantMax);
		}
		
		final int random = Rnd.get(100);
		int chanceSum = 0;
		for (int i = 0; i < _randomEnchantChances.length; i++)
		{
			chanceSum += _randomEnchantChances[i];
			if (random < chanceSum)
			{
				return Math.min(_randomEnchantMin + i, _randomEnchantMax);
			}
		}
		
		return _randomEnchantMin;
	}
	
	/**
	 * Checks if an item can be enchanted using the current support item.<br>
	 * This method verifies the enchantment level, type, and grade requirements.
	 * @param itemToEnchant The {@code Item} that the player wants to enchant.
	 * @param supportItem The {@code EnchantSupportItem} being used for the process.
	 * @return {@code true} if the item meets all requirements; {@code false} otherwise.
	 */
	public boolean isValid(Item itemToEnchant, EnchantSupportItem supportItem)
	{
		if (itemToEnchant == null)
		{
			return false;
		}
		else if (!itemToEnchant.isEnchantable() || ((itemToEnchant.getTemplate().getEnchantLimit() != 0) && (itemToEnchant.getEnchantLevel() == itemToEnchant.getTemplate().getEnchantLimit())))
		{
			return false;
		}
		else if (!isValidItemType(itemToEnchant.getTemplate().getType2()))
		{
			return false;
		}
		else if (((_minEnchantLevel != 0) && (itemToEnchant.getEnchantLevel() < _minEnchantLevel)) || ((_maxEnchantLevel != 0) && (itemToEnchant.getEnchantLevel() >= _maxEnchantLevel)))
		{
			return false;
		}
		else if (_grade != itemToEnchant.getTemplate().getCrystalTypePlus())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if the provided item type is valid for enchantment.<br>
	 * It verifies if a weapon type matches the {@code isWeapon()} check.<br>
	 * It also checks if shield, armor, or accessory types are not weapons.
	 * @param type2 The item type identifier from {@code ItemTemplate}.
	 * @return {@code true} if the item type is valid for this enchantment, {@code false} otherwise.
	 */
	private boolean isValidItemType(int type2)
	{
		if (type2 == ItemTemplate.TYPE2_WEAPON)
		{
			return isWeapon();
		}
		else if ((type2 == ItemTemplate.TYPE2_SHIELD_ARMOR) || (type2 == ItemTemplate.TYPE2_ACCESSORY))
		{
			return !isWeapon();
		}
		
		return false;
	}
}
