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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.PremiumItem;

/**
 * Manages the premium items of a {@code Player}.<br>
 * It holds the items waiting in the premium item list and persists them in the {@code character_premium_items} table.
 * @author Mobius
 */
public class PremiumItemList
{
	private static final Logger LOGGER = Logger.getLogger(PremiumItemList.class.getName());
	
	private final Player _player;
	private final Map<Integer, PremiumItem> _items = new ConcurrentSkipListMap<>();
	
	/**
	 * Creates a new {@code PremiumItemList} for a specific player.
	 * @param player The {@code Player} object associated with this instance.
	 */
	public PremiumItemList(Player player)
	{
		_player = player;
	}
	
	/**
	 * Loads the list of premium items for the current character from the database.<br>
	 * It retrieves data from the {@code character_premium_items} table.<br>
	 * The results are stored in the internal {@code _items} map.
	 */
	public void restore()
	{
		final String sql = "SELECT itemNum, itemId, itemCount, itemSender FROM character_premium_items WHERE charId=?";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(sql))
		{
			statement.setInt(1, _player.getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final int itemNum = rset.getInt("itemNum");
					final int itemId = rset.getInt("itemId");
					final long itemCount = rset.getLong("itemCount");
					final String itemSender = rset.getString("itemSender");
					_items.put(itemNum, new PremiumItem(itemId, itemCount, itemSender));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore premium items: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the list of premium items.<br>
	 * The map uses an {@code Integer} ID as the key.
	 * @return A {@code Map} containing all {@code PremiumItem} objects.
	 */
	public Map<Integer, PremiumItem> getItems()
	{
		return _items;
	}
	
	/**
	 * Updates the count of a specific premium item for the character.<br>
	 * This method modifies the {@code itemCount} in the database.
	 * @param itemNum The unique identifier for the premium item.
	 * @param newcount The new quantity to set for the item.
	 */
	public void update(int itemNum, long newcount)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE character_premium_items SET itemCount=? WHERE charId=? AND itemNum=? "))
		{
			statement.setLong(1, newcount);
			statement.setInt(2, _player.getObjectId());
			statement.setInt(3, itemNum);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not update premium items: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes a specific premium item from the character database.<br>
	 * This method uses the unique identifier of the character and the item number.<br>
	 * If an error occurs during the deletion, it logs a severe message.
	 * @param itemNum The unique identification number of the premium item to delete.
	 */
	public void delete(int itemNum)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_premium_items WHERE charId=? AND itemNum=? "))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, itemNum);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.severe("Could not delete premium item: " + e);
		}
	}
}
