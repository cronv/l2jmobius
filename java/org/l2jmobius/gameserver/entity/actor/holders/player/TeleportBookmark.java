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

import org.l2jmobius.gameserver.entity.Location;

/**
 * Represents a saved location a player can use to teleport.
 * @author UnAfraid, Mobius
 */
public class TeleportBookmark extends Location
{
	private final int _id;
	private int _icon;
	private String _name;
	private String _tag;
	
	/**
	 * Creates a teleport bookmark with the given location and details.
	 * @param id The bookmark id.
	 * @param x The destination X coordinate.
	 * @param y The destination Y coordinate.
	 * @param z The destination Z coordinate.
	 * @param icon The icon id.
	 * @param tag A descriptive label.
	 * @param name The display name.
	 */
	public TeleportBookmark(int id, int x, int y, int z, int icon, String tag, String name)
	{
		super(x, y, z);
		_id = id;
		_icon = icon;
		_name = name;
		_tag = tag;
	}
	
	/**
	 * Retrieves the bookmark name.
	 * @return The name as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Sets the bookmark name.
	 * @param name The new name.
	 */
	public void setName(String name)
	{
		_name = name;
	}
	
	/**
	 * Retrieves the bookmark id.
	 * @return The id as an {@code int}.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the icon id.
	 * @return The icon id as an {@code int}.
	 */
	public int getIcon()
	{
		return _icon;
	}
	
	/**
	 * Sets the icon id.
	 * @param icon The new icon id.
	 */
	public void setIcon(int icon)
	{
		_icon = icon;
	}
	
	/**
	 * Retrieves the bookmark tag.
	 * @return The tag as a {@code String}.
	 */
	public String getTag()
	{
		return _tag;
	}
	
	/**
	 * Sets the bookmark tag.
	 * @param tag The new tag.
	 */
	public void setTag(String tag)
	{
		_tag = tag;
	}
}
