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

/**
 * This class holds action data related to skills, handlers, and option identifiers.<br>
 * It is used for mapping player actions to their appropriate handlers.
 * @author UnAfraid, Mobius
 */
public class ActionDataHolder
{
	private final int _id;
	private final String _handler;
	private final int _optionId;
	
	/**
	 * Creates a new instance of {@code ActionDataHolder}.<br>
	 * This constructor initializes the action data with specific identifiers.
	 * @param id The unique identifier for the action.
	 * @param handler The name of the handler associated with this action.
	 * @param optionId The specific option identifier for the action.
	 */
	public ActionDataHolder(int id, String handler, int optionId)
	{
		_id = id;
		_handler = handler;
		_optionId = optionId;
	}
	
	/**
	 * Retrieves the unique identifier for this action.<br>
	 * This value is used to distinguish between different actions.
	 * @return the {@code int} id of the action.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the name of the handler associated with this action.<br>
	 * This value is used to map actions to their specific logic.
	 * @return the {@code String} representing the handler name.
	 */
	public String getHandler()
	{
		return _handler;
	}
	
	/**
	 * Retrieves the unique identifier for this specific action option.<br>
	 * This value is used to distinguish between different variations of an action.
	 * @return the {@code int} value representing the option ID.
	 */
	public int getOptionId()
	{
		return _optionId;
	}
}
