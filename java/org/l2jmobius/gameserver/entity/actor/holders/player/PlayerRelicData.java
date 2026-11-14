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
package org.l2jmobius.gameserver.entity.actor.holders.player;

/**
 * This class holds the data associated with a player's relics.<br>
 * It stores persistent information used to manage relic properties for an individual character.
 * @author CostyKiller
 */
public class PlayerRelicData
{
	private final int _relicId;
	private int _relicLevel;
	private int _relicCount;
	private int _relicIndex;
	private long _relicSummonTime;
	
	/**
	 * Creates a new {@code PlayerRelicData} object.<br>
	 * This constructor initializes all fields for a player's relic.
	 * @param relicId The unique identifier for the relic type.
	 * @param relicLevel The current level of the relic.
	 * @param relicCount The total number of this relic owned by the player.
	 * @param relicIndex The specific index of the relic in the collection.
	 * @param relicSummonTime The timestamp when the relic was summoned.
	 */
	public PlayerRelicData(int relicId, int relicLevel, int relicCount, int relicIndex, long relicSummonTime)
	{
		_relicId = relicId;
		_relicLevel = relicLevel;
		_relicCount = relicCount;
		_relicIndex = relicIndex;
		_relicSummonTime = relicSummonTime;
	}
	
	/**
	 * Retrieves the unique identifier for the player's relic.<br>
	 * This value is used to identify which specific relic type is being referenced.
	 * @return The {@code int} ID of the relic.
	 */
	public int getRelicId()
	{
		return _relicId;
	}
	
	/**
	 * Retrieves the current level of the player's relic.<br>
	 * This value is stored in the {@code _relicLevel} field.
	 * @return The integer level of the relic.
	 */
	public int getRelicLevel()
	{
		return _relicLevel;
	}
	
	/**
	 * Retrieves the current number of relics.<br>
	 * This value represents the total count stored in {@code _relicCount}.
	 * @return The total number of relics as an {@code int}.
	 */
	public int getRelicCount()
	{
		return _relicCount;
	}
	
	/**
	 * Retrieves the current index of the relic.<br>
	 * This value identifies the position of the relic in the collection.
	 * @return The {@code int} representing the relic index.
	 */
	public int getRelicIndex()
	{
		return _relicIndex;
	}
	
	/**
	 * Retrieves the timestamp when the relic was summoned.<br>
	 * This value is stored in milliseconds.
	 * @return The {@code long} value representing the summon time.
	 */
	public long getRelicSummonTime()
	{
		return _relicSummonTime;
	}
	
	/**
	 * Updates the current level of the player's relic.<br>
	 * This method sets the {@code _relicLevel} field to a new value.
	 * @param level The new level to assign to the relic.
	 */
	public void setRelicLevel(int level)
	{
		_relicLevel = level;
	}
	
	/**
	 * Updates the total number of relics for the player.<br>
	 * This method sets the {@code _relicCount} field to a new value.
	 * @param count The new quantity of relics to set.
	 */
	public void setRelicCount(int count)
	{
		_relicCount = count;
	}
	
	/**
	 * Updates the current index of the relic.<br>
	 * This value determines the position of the relic in the collection.
	 * @param index The new {@code int} value to set for the relic index.
	 */
	public void setRelicIndex(int index)
	{
		_relicIndex = index;
	}
	
	/**
	 * Sets the time when a relic was summoned.<br>
	 * This updates the {@code _relicSummonTime} field.
	 * @param relicSummonTime The timestamp of the summon.
	 */
	public void setRelicSummonTime(long relicSummonTime)
	{
		_relicSummonTime = relicSummonTime;
	}
}
