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
package org.l2jmobius.gameserver.entity.actor.holders.npc;

/**
 * This class serves as a container for the {@code schemes} buffer.<br>
 * It is used to manage and store specific buff-related data for NPCs.
 */
public class BuffSkillHolder
{
	private final int _id;
	private final int _level;
	private final int _price;
	private final String _type;
	private final String _description;
	
	/**
	 * Creates a new instance of {@code BuffSkillHolder}.<br>
	 * This constructor initializes all fields for a specific buff skill.
	 * @param id The unique identifier for the skill.
	 * @param level The current level of the skill.
	 * @param price The cost required to obtain the skill.
	 * @param type The category or classification of the skill.
	 * @param description A text summary explaining what the skill does.
	 */
	public BuffSkillHolder(int id, int level, int price, String type, String description)
	{
		_id = id;
		_level = level;
		_price = price;
		_type = type;
		_description = description;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code BuffSkillHolder}.<br>
	 * This value is used to identify a specific skill in the system.
	 * @return The unique integer ID of the buff skill.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the current level of the buff skill.<br>
	 * This value is stored in the {@code BuffSkillHolder} instance.
	 * @return The integer level of the skill.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the cost of the buff skill.<br>
	 * This value is returned as an {@code int}.
	 * @return The price of the skill.
	 */
	public int getPrice()
	{
		return _price;
	}
	
	/**
	 * Retrieves the type of the buff skill.<br>
	 * This value identifies the category of the skill.
	 * @return The {@code String} representing the skill type.
	 */
	public String getType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the descriptive text for this buff skill.<br>
	 * This string provides details about what the skill does.
	 * @return The description of the buff skill as a {@code String}.
	 */
	public String getDescription()
	{
		return _description;
	}
}
