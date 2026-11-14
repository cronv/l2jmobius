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

import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;

/**
 * This class serves as a data container for information related to a player's class.<br>
 * It stores specific attributes and properties associated with the {@code PlayerClass}.
 * @author Mobius
 */
public class ClassInfoHolder
{
	private final PlayerClass _playerClass;
	private final PlayerClass _parentClass;
	private final String _className;
	
	/**
	 * Creates a new instance of {@code ClassInfoHolder}.<br>
	 * This constructor initializes the class data for a specific player type.
	 * @param playerClass The {@code PlayerClass} enum representing the current class.
	 * @param parentClass The {@code PlayerClass} that this class evolves from, or {@code null} if it is a base class.
	 * @param className The display name of the class as a {@code String}.
	 */
	public ClassInfoHolder(PlayerClass playerClass, PlayerClass parentClass, String className)
	{
		_playerClass = playerClass;
		_parentClass = parentClass;
		_className = className;
	}
	
	/**
	 * Retrieves the {@code PlayerClass} associated with this holder.<br>
	 * This method returns the primary class type for the player.
	 * @return the {@code PlayerClass} of the player.
	 */
	public PlayerClass getPlayerClass()
	{
		return _playerClass;
	}
	
	/**
	 * Retrieves the parent class of the current player class.<br>
	 * This is used to determine which class a player evolved from.<br>
	 * It returns {@code null} if the current class is a base class.
	 * @return the {@code PlayerClass} of the parent, or {@code null}
	 */
	public PlayerClass getParentClass()
	{
		return _parentClass;
	}
	
	/**
	 * Retrieves the display name of the player class.<br>
	 * This method returns the {@code String} value stored in the holder.
	 * @return the name of the class as a {@code String}.
	 */
	public String getClassName()
	{
		return _className;
	}
}
