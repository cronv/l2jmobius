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
package org.l2jmobius.gameserver.data.holders;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.mechanics.script.Faction;

/**
 * This class acts as a data container for monster book cards.<br>
 * It stores the collection of cards associated with specific monsters in the game world.
 * @author Mobius
 */
public class MonsterBookCardHolder
{
	private final int _id;
	private final int _monsterId;
	private final Faction _faction;
	private final List<MonsterBookRewardHolder> _rewards = new ArrayList<>(4);
	
	/**
	 * Creates a new instance of {@code MonsterBookCardHolder}.<br>
	 * This constructor initializes the card with its unique ID, associated monster, and faction.
	 * @param id The unique identifier for this card.
	 * @param monsterId The ID of the monster associated with this card.
	 * @param faction The {@code Faction} to which this card belongs.
	 */
	public MonsterBookCardHolder(int id, int monsterId, Faction faction)
	{
		_id = id;
		_monsterId = monsterId;
		_faction = faction;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code MonsterBookCardHolder}.<br>
	 * This value is used to distinguish between different cards.
	 * @return The unique integer ID of the card.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the unique identifier for the monster.<br>
	 * This value corresponds to the {@code monsterId} field.
	 * @return The integer ID of the monster.
	 */
	public int getMonsterId()
	{
		return _monsterId;
	}
	
	/**
	 * Retrieves the faction associated with this monster book card.<br>
	 * This method returns the {@code Faction} object assigned to the holder.
	 * @return the {@code Faction} of the monster book card.
	 */
	public Faction getFaction()
	{
		return _faction;
	}
	
	/**
	 * Retrieves the reward for a specific monster book level.<br>
	 * This method looks up the reward based on the provided index.
	 * @param level The zero-based index of the reward to retrieve.
	 * @return The {@code MonsterBookRewardHolder} corresponding to the given level.
	 */
	public MonsterBookRewardHolder getReward(int level)
	{
		return _rewards.get(level);
	}
	
	/**
	 * Adds a new reward to the list of rewards.<br>
	 * This method updates the {@code MonsterBookCardHolder} with the provided data.
	 * @param reward The {@code MonsterBookRewardHolder} object to be added.
	 */
	public void addReward(MonsterBookRewardHolder reward)
	{
		_rewards.add(reward);
	}
}
