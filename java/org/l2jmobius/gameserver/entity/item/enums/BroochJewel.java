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
package org.l2jmobius.gameserver.entity.item.enums;

/**
 * Defines the types of jewels that can be socketed into a brooch.<br>
 * This enum is used to identify specific jewel variants within the game world.
 * @author Mobius
 */
public enum BroochJewel
{
	RUBY_LV1(38855, 17814, 1, 0.01, true, false),
	RUBY_LV2(38856, 17814, 1, 0.035, true, false),
	RUBY_LV3(38857, 17815, 1, 0.075, true, false),
	RUBY_LV4(38858, 17816, 1, 0.125, true, false),
	RUBY_LV5(38859, 17817, 1, 0.2, true, false),
	
	GREATER_RUBY_LV1(47688, 18715, 1, 0.2, true, false),
	GREATER_RUBY_LV1_EVENT(80473, 18715, 1, 0.2, true, false),
	GREATER_RUBY_LV2(48771, 18715, 1, 0.2, true, false),
	GREATER_RUBY_LV3(48772, 18715, 1, 0.2, true, false),
	GREATER_RUBY_LV4(48773, 18715, 1, 0.2, true, false),
	GREATER_RUBY_LV5(48774, 18715, 1, 0.2, true, false),
	
	SUPERIOR_RUBY_LV1(81505, 39658, 1, 0.23, true, false),
	SUPERIOR_RUBY_LV2(81506, 39658, 1, 0.23, true, false),
	SUPERIOR_RUBY_LV3(81507, 39658, 1, 0.23, true, false),
	SUPERIOR_RUBY_LV4(81508, 39658, 1, 0.23, true, false),
	SUPERIOR_RUBY_LV5(81509, 39658, 1, 0.23, true, false),
	SUPERIOR_RUBY_LV6(81510, 39658, 1, 0.23, true, false),
	SUPERIOR_RUBY_LV7(81511, 39658, 1, 0.23, true, false),
	SUPERIOR_RUBY_LV8(81512, 39658, 1, 0.23, true, false),
	SUPERIOR_RUBY_LV9(81513, 39658, 1, 0.23, true, false),
	SUPERIOR_RUBY_LV10(81514, 39658, 1, 0.23, true, false),
	SUPERIOR_RUBY_LV10_EVENT(81869, 39658, 1, 0.23, true, false),
	
	SAPPHIRE_LV1(38927, 17818, 1, 0.01, false, true),
	SAPPHIRE_LV2(38928, 17818, 1, 0.035, false, true),
	SAPPHIRE_LV3(38929, 17819, 1, 0.075, false, true),
	SAPPHIRE_LV4(38930, 17820, 1, 0.125, false, true),
	SAPPHIRE_LV5(38931, 17821, 1, 0.2, false, true),
	
	GREATER_SAPPHIRE_LV1(47689, 18718, 1, 0.2, false, true),
	GREATER_SAPPHIRE_LV1_EVENT(80474, 18718, 1, 0.2, false, true),
	GREATER_SAPPHIRE_LV2(48775, 18718, 1, 0.2, false, true),
	GREATER_SAPPHIRE_LV3(48776, 18718, 1, 0.2, false, true),
	GREATER_SAPPHIRE_LV4(48777, 18718, 1, 0.2, false, true),
	GREATER_SAPPHIRE_LV5(48778, 18718, 1, 0.2, false, true),
	
	SUPERIOR_SAPPHIRE_LV1(81515, 39660, 1, 0.23, false, true),
	SUPERIOR_SAPPHIRE_LV2(81516, 39660, 1, 0.23, false, true),
	SUPERIOR_SAPPHIRE_LV3(81517, 39660, 1, 0.23, false, true),
	SUPERIOR_SAPPHIRE_LV4(81518, 39660, 1, 0.23, false, true),
	SUPERIOR_SAPPHIRE_LV5(81519, 39660, 1, 0.23, false, true),
	SUPERIOR_SAPPHIRE_LV6(81520, 39660, 1, 0.23, false, true),
	SUPERIOR_SAPPHIRE_LV7(81521, 39660, 1, 0.23, false, true),
	SUPERIOR_SAPPHIRE_LV8(81522, 39660, 1, 0.23, false, true),
	SUPERIOR_SAPPHIRE_LV9(81523, 39660, 1, 0.23, false, true),
	SUPERIOR_SAPPHIRE_LV10(81524, 39660, 1, 0.23, false, true),
	SUPERIOR_SAPPHIRE_LV10_EVENT(81870, 39660, 1, 0.23, false, true),
	
	ORB_OF_FIRE_RUBY(83358, 39658, 1, 0.23, true, false),
	ORB_OF_FIRE_SAPPHIRE(83358, 39660, 1, 0.23, false, true);
	
	private final int _itemId;
	private final int _skillId;
	private final int _skillLevel;
	private final double _bonus;
	private final boolean _isRuby;
	private final boolean _isSapphire;
	
	/**
	 * Initializes a new {@code BroochJewel} instance.<br>
	 * Sets the internal properties for the jewel type.
	 * @param itemId The unique identifier for the item.
	 * @param skillId The ID of the skill associated with this jewel.
	 * @param skillLevel The level of the skill provided by the jewel.
	 * @param bonus The numerical bonus value applied to the skill.
	 * @param isRuby Set to {@code true} if the jewel is a Ruby type.
	 * @param isSapphire Set to {@code true} if the jewel is a Sapphire type.
	 */
	private BroochJewel(int itemId, int skillId, int skillLevel, double bonus, boolean isRuby, boolean isSapphire)
	{
		_itemId = itemId;
		_skillId = skillId;
		_skillLevel = skillLevel;
		_bonus = bonus;
		_isRuby = isRuby;
		_isSapphire = isSapphire;
	}
	
	/**
	 * Retrieves the unique identifier for this jewel.<br>
	 * This value corresponds to the internal item ID used by the game engine.
	 * @return The {@code int} representation of the item ID.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the unique identifier for the skill associated with this jewel.<br>
	 * This value corresponds to the {@code _skillId} field.
	 * @return The integer ID of the skill.
	 */
	public int getSkillId()
	{
		return _skillId;
	}
	
	/**
	 * Retrieves the skill level associated with this {@code BroochJewel}.<br>
	 * This value represents the specific tier of the jewel.
	 * @return The integer value of the skill level.
	 */
	public int getSkillLevel()
	{
		return _skillLevel;
	}
	
	/**
	 * Retrieves the bonus value for this {@code BroochJewel}.<br>
	 * This value represents the specific multiplier assigned to the jewel.
	 * @return The bonus as a {@code double}.
	 */
	public double getBonus()
	{
		return _bonus;
	}
	
	/**
	 * Checks if the jewel type is a ruby.
	 * @return {@code true} if it is a ruby, {@code false} otherwise.
	 */
	public boolean isRuby()
	{
		return _isRuby;
	}
	
	/**
	 * Checks if the jewel type is a sapphire.<br>
	 * This method returns {@code true} for any sapphire variant.<br>
	 * It returns {@code false} for all other types.
	 * @return {@code true} if this is a sapphire, otherwise {@code false}.
	 */
	public boolean isSapphire()
	{
		return _isSapphire;
	}
}
