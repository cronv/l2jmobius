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
package org.l2jmobius.gameserver.entity.item.type;

import java.util.HashMap;
import java.util.Map;

/**
 * This enum defines the different types of crystals available in the game.<br>
 * It is used to categorize crystal items throughout the server.
 * @author Adry_85, Mobius
 */
public enum CrystalType
{
	NONE(0, 0, 0, 0),
	D(1, 1458, 11, 90),
	C(2, 1459, 6, 45),
	B(3, 1460, 11, 67),
	A(4, 1461, 20, 145),
	S(5, 1462, 25, 250),
	S80(6, 1462, 25, 250),
	S84(7, 1462, 25, 250),
	R(8, 17371, 30, 500),
	R95(9, 17371, 30, 500),
	R99(10, 17371, 30, 500),
	R110(11, 17371, 30, 500),
	L(12, 17371, 30, 500),
	EVENT(13, 0, 0, 0);
	
	private final int _level;
	private final int _crystalId;
	private final int _crystalEnchantBonusArmor;
	private final int _crystalEnchantBonusWeapon;
	
	/**
	 * Creates a new {@code CrystalType} instance.<br>
	 * This constructor initializes the properties of the crystal.
	 * @param level The numerical level of the crystal.
	 * @param crystalId The unique identification number for the crystal.
	 * @param crystalEnchantBonusArmor The amount of armor bonus provided by the enchantment.
	 * @param crystalEnchantBonusWeapon The amount of weapon bonus provided by the enchantment.
	 */
	CrystalType(int level, int crystalId, int crystalEnchantBonusArmor, int crystalEnchantBonusWeapon)
	{
		_level = level;
		_crystalId = crystalId;
		_crystalEnchantBonusArmor = crystalEnchantBonusArmor;
		_crystalEnchantBonusWeapon = crystalEnchantBonusWeapon;
	}
	
	/**
	 * Retrieves the level of this {@code CrystalType}.<br>
	 * This value represents the numerical rank of the crystal.
	 * @return The integer level associated with this crystal.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the unique identification number for this {@code CrystalType}.<br>
	 * This value corresponds to the internal ID used by the game engine.
	 * @return The integer ID of the crystal.
	 */
	public int getCrystalId()
	{
		return _crystalId;
	}
	
	/**
	 * Retrieves the armor bonus provided by this crystal type.<br>
	 * This value is used during the enchantment process.
	 * @return the amount of bonus armor as an {@code int}.
	 */
	public int getCrystalEnchantBonusArmor()
	{
		return _crystalEnchantBonusArmor;
	}
	
	/**
	 * Retrieves the weapon enchantment bonus for this {@code CrystalType}.<br>
	 * This value represents the specific power boost applied to weapons.
	 * @return The integer value of the weapon enchantment bonus.
	 */
	public int getCrystalEnchantBonusWeapon()
	{
		return _crystalEnchantBonusWeapon;
	}
	
	/**
	 * Checks if this crystal has a higher level than another.<br>
	 * It compares the {@code _level} value of two {@code CrystalType} objects.
	 * @param crystalType The other crystal to compare against.
	 * @return {@code true} if this crystal's level is strictly greater than the provided crystal, otherwise {@code false}.
	 */
	public boolean isGreater(CrystalType crystalType)
	{
		return getLevel() > crystalType.getLevel();
	}
	
	/**
	 * Checks if this crystal type has a lower level than another.<br>
	 * It compares the {@code _level} value of two {@code CrystalType} constants.
	 * @param crystalType The other {@code CrystalType} to compare against.
	 * @return {@code true} if this crystal's level is strictly less than the provided type, {@code false} otherwise.
	 */
	public boolean isLesser(CrystalType crystalType)
	{
		return getLevel() < crystalType.getLevel();
	}
	
	/**
	 * Calculates a new {@code CrystalType} based on an added level.<br>
	 * This method adds {@code levelValue} to the current crystal's level.<br>
	 * It ensures the result stays within the valid range of {@code NONE} and {@code R110}.
	 * @param levelValue The amount to add to the current level.
	 * @return The resulting {@code CrystalType} after adding the level.
	 */
	public CrystalType plusLevel(int levelValue)
	{
		final int level = levelValue + _level;
		if (level >= CrystalType.R110.getLevel())
		{
			return CrystalType.R110;
		}
		
		if (level <= CrystalType.NONE.getLevel())
		{
			return CrystalType.NONE;
		}
		
		return getByLevel(level);
	}
	
	private static final Map<Integer, CrystalType> BY_LEVEL = new HashMap<>(values().length);
	static
	{
		for (CrystalType crystalType : values())
		{
			BY_LEVEL.put(crystalType.getLevel(), crystalType);
		}
	}
	
	/**
	 * Retrieves a {@code CrystalType} based on its numerical level.<br>
	 * This method looks up the value in the internal mapping.
	 * @param level The integer level to search for.
	 * @return The corresponding {@code CrystalType} or {@code null} if not found.
	 */
	public static CrystalType getByLevel(int level)
	{
		return BY_LEVEL.get(level);
	}
}
