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
package org.l2jmobius.gameserver.data.holders;

/**
 * This class serves as a simple data holder for the Selling Buffs system.<br>
 * It stores relevant information required to manage and process these buffs.
 * @author St3eT
 */
public class SellBuffHolder
{
	private final int _skillId;
	private long _price;
	
	/**
	 * Creates a new {@code SellBuffHolder} instance.<br>
	 * This constructor initializes the buff data with a specific ID and cost.
	 * @param skillId The unique identifier for the skill.
	 * @param price The amount of currency required to sell the buff.
	 */
	public SellBuffHolder(int skillId, long price)
	{
		_skillId = skillId;
		_price = price;
	}
	
	/**
	 * Retrieves the unique identifier for the skill.<br>
	 * This value is used to identify which buff is being sold.
	 * @return The {@code int} ID of the skill.
	 */
	public int getSkillId()
	{
		return _skillId;
	}
	
	/**
	 * Updates the price of the selling buff.<br>
	 * This method sets the {@code _price} field to the provided value.
	 * @param price The new price to set.
	 */
	public void setPrice(int price)
	{
		_price = price;
	}
	
	/**
	 * Retrieves the current price of the selling buff.<br>
	 * This value is stored as a {@code long}.
	 * @return The price of the buff.
	 */
	public long getPrice()
	{
		return _price;
	}
}
