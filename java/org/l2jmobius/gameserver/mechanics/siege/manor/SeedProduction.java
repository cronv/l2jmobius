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
package org.l2jmobius.gameserver.mechanics.siege.manor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles the logic for seed production within the manor siege mechanics.<br>
 * It manages how seeds are generated and distributed during gameplay.
 * @author xban1x
 */
public class SeedProduction
{
	private final int _seedId;
	private final long _price;
	private final long _startAmount;
	private final AtomicLong _amount;
	
	/**
	 * Creates a new {@code SeedProduction} instance.<br>
	 * Initializes the seed data with specific values.
	 * @param id The unique identifier for the seed.
	 * @param amount The initial quantity of the seed.
	 * @param price The cost associated with the seed.
	 * @param startAmount The original amount at the beginning.
	 */
	public SeedProduction(int id, long amount, long price, long startAmount)
	{
		_seedId = id;
		_amount = new AtomicLong(amount);
		_price = price;
		_startAmount = startAmount;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code SeedProduction}.<br>
	 * This ID is used to distinguish different seed types.
	 * @return The integer value of the {@code _seedId}.
	 */
	public int getId()
	{
		return _seedId;
	}
	
	/**
	 * Retrieves the current quantity of seeds produced.<br>
	 * This method returns the value stored in the {@code _amount} field.
	 * @return The current amount as a {@code long}.
	 */
	public long getAmount()
	{
		return _amount.get();
	}
	
	/**
	 * Retrieves the current price of the seed.<br>
	 * This value is stored as a {@code long}.
	 * @return The price of the seed.
	 */
	public long getPrice()
	{
		return _price;
	}
	
	/**
	 * Returns the initial amount of seeds.<br>
	 * This value is used to reset the current amount.
	 * @return The starting amount as a {@code long}.
	 */
	public long getStartAmount()
	{
		return _startAmount;
	}
	
	/**
	 * Updates the current quantity of seeds.<br>
	 * This method sets the {@code _amount} field to a new value.
	 * @param amount The new total amount to set.
	 */
	public void setAmount(long amount)
	{
		_amount.set(amount);
	}
	
	/**
	 * Reduces the current amount by a specific value.<br>
	 * This method ensures the result is not less than {@code 0}.<br>
	 * It uses an atomic operation to update the value safely.
	 * @param value The amount to subtract from the total.
	 * @return {@code true} if the reduction was successful, or {@code false} if it would result in a negative value.
	 */
	public boolean decreaseAmount(long value)
	{
		long current;
		long next;
		do
		{
			current = _amount.get();
			next = current - value;
			if (next < 0)
			{
				return false;
			}
		}
		while (!_amount.compareAndSet(current, next));
		return true;
	}
}
