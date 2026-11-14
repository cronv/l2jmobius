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
package org.l2jmobius.gameserver.cache;

/**
 * This class manages the caching of relationships between entities.<br>
 * It provides fast access to pre-calculated data to improve performance.<br>
 * Use {@code RelationCache} to retrieve or store specific relational links.
 * @author Sahar
 */
public class RelationCache
{
	private final long _relation;
	private final boolean _isAutoAttackable;
	
	/**
	 * Creates a new instance of {@code RelationCache}.<br>
	 * This constructor initializes the relation and attack status.
	 * @param relation The unique identifier for the relation.
	 * @param isAutoAttackable Whether the entity can be attacked automatically.
	 */
	public RelationCache(long relation, boolean isAutoAttackable)
	{
		_relation = relation;
		_isAutoAttackable = isAutoAttackable;
	}
	
	/**
	 * Retrieves the unique identifier for the relation.<br>
	 * This value was provided during the creation of the {@code RelationCache}.
	 * @return The {@code long} value representing the relation.
	 */
	public long getRelation()
	{
		return _relation;
	}
	
	/**
	 * Checks if the entity can be targeted by an auto-attack.<br>
	 * This method returns the current status of the auto-attack flag.
	 * @return {@code true} if the entity is auto-attackable, {@code false} otherwise.
	 */
	public boolean isAutoAttackable()
	{
		return _isAutoAttackable;
	}
}
