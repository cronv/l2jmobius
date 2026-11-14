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
package org.l2jmobius.gameserver.mechanics.variables;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class manages custom variables specifically for {@code Summon} and NPC entities.<br>
 * It extends {@code StatSet} to provide a structured way to store and retrieve dynamic data.
 * @author GKR, Mobius
 */
public class NpcVariables extends StatSet
{
	/**
	 * Creates a new instance of {@code NpcVariables}.<br>
	 * This class stores variables in memory for NPCs.<br>
	 * It does not perform any database operations.
	 */
	public NpcVariables()
	{
	}
	
	/**
	 * Retrieves an integer value associated with a specific key.<br>
	 * If the key does not exist, it returns {@code 0}.
	 * @param key The unique identifier for the variable.
	 * @return The integer value of the variable.
	 */
	@Override
	public int getInt(String key)
	{
		return super.getInt(key, 0);
	}
	
	/**
	 * Checks if a variable exists with the given name.<br>
	 * This method looks into the internal set of variables.
	 * @param name The name of the variable to check.
	 * @return {@code true} if the variable exists, otherwise {@code false}.
	 */
	public boolean hasVariable(String name)
	{
		return getSet().containsKey(name);
	}
	
	/**
	 * Retrieves a {@code Player} object associated with the given name.<br>
	 * This method looks up the variable in the current set.<br>
	 * It returns {@code null} if no player is found for that name.
	 * @param name The unique name of the player to retrieve.
	 * @return The {@code Player} object, or {@code null} if not found.
	 */
	public Player getPlayer(String name)
	{
		return getObject(name, Player.class);
	}
	
	/**
	 * Retrieves a {@code Summon} object associated with the given name.<br>
	 * This method looks up the variable in the internal storage.<br>
	 * It returns {@code null} if no summon is found for that name.
	 * @param name The unique identifier of the summon to retrieve.
	 * @return The {@code Summon} object, or {@code null} if it does not exist.
	 */
	public Summon getSummon(String name)
	{
		return getObject(name, Summon.class);
	}
}
