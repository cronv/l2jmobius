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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.holders.CollectionDataHolder;
import org.l2jmobius.gameserver.data.xml.CollectionData;
import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.options.Options;

/**
 * Manages the collections of a {@code Player}.<br>
 * It handles the registered collection entries, their bonuses and the favourite list.<br>
 * The rows are keyed by account name, so the data is shared across the account.
 * @author Mobius
 */
public class CollectionList
{
	private static final Logger LOGGER = Logger.getLogger(CollectionList.class.getName());
	
	private static final String INSERT_COLLECTION = "REPLACE INTO collections (`accountName`, `itemId`, `collectionId`, `index`) VALUES (?, ?, ?, ?)";
	private static final String RESTORE_COLLECTION = "SELECT * FROM collections WHERE accountName=? ORDER BY `index`";
	private static final String DELETE_COLLECTION_FAVORITE = "DELETE FROM collection_favorites WHERE accountName=?";
	private static final String INSERT_COLLECTION_FAVORITE = "REPLACE INTO collection_favorites (`accountName`, `collectionId`) VALUES (?, ?)";
	private static final String RESTORE_COLLECTION_FAVORITE = "SELECT * FROM collection_favorites WHERE accountName=?";
	
	private final Player _player;
	private final List<PlayerCollectionData> _entries = new ArrayList<>();
	private final List<Integer> _favorites = new ArrayList<>();
	
	/**
	 * Creates a new {@code CollectionList} for a specific player.
	 * @param player The {@code Player} object associated with this instance.
	 */
	public CollectionList(Player player)
	{
		_player = player;
	}
	
	/**
	 * This method restores the player's collection data from the database.<br>
	 * It clears the current {@code _entries} list before loading new entries.<br>
	 * The process uses a {@code PreparedStatement} to fetch records for the current account.<br>
	 * If an error occurs, it logs a severe message with the player's object ID.
	 */
	public void restore()
	{
		_entries.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_COLLECTION))
		{
			statement.setString(1, _player.getAccountName());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final int collectionId = rset.getInt("collectionId");
					if (CollectionData.getInstance().getCollection(collectionId) != null)
					{
						_entries.add(new PlayerCollectionData(collectionId, rset.getInt("itemId"), rset.getInt("index")));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore collection list data for playerId: " + _player.getObjectId(), e);
		}
	}
	
	/**
	 * This method updates the player's bonuses based on completed collections.<br>
	 * It checks every collection owned by the player.<br>
	 * If a collection meets the required completion count, its associated options are applied to the player.
	 */
	public void restoreBonuses()
	{
		final Set<Integer> collectionIds = new HashSet<>();
		for (PlayerCollectionData collection : _entries)
		{
			collectionIds.add(collection.getCollectionId());
		}
		
		for (int collectionId : collectionIds)
		{
			final CollectionDataHolder collection = CollectionData.getInstance().getCollection(collectionId);
			int count = 0;
			for (PlayerCollectionData data : _entries)
			{
				if (data.getCollectionId() == collectionId)
				{
					count++;
				}
			}
			
			if (count >= collection.getCompleteCount())
			{
				final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
				if (options != null)
				{
					options.apply(_player);
				}
			}
		}
	}
	
	/**
	 * Loads the player's favorite collections from the database.<br>
	 * This method clears the current {@code _favorites} list before populating it.<br>
	 * It uses the account name to fetch data from the database.
	 */
	public void restoreFavorites()
	{
		_favorites.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_COLLECTION_FAVORITE))
		{
			statement.setString(1, _player.getAccountName());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					_favorites.add(rset.getInt("collectionId"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore collection favorite list data for playerId: " + _player.getObjectId(), e);
		}
	}
	
	/**
	 * Saves the player's collection data to the database.<br>
	 * This method checks if any collections exist before starting the save process.<br>
	 * It uses a batch statement to insert all collection items for the current account.
	 */
	public void store()
	{
		if (_entries.isEmpty())
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(INSERT_COLLECTION))
		{
			_entries.forEach(data ->
			{
				try
				{
					st.setString(1, _player.getAccountName());
					st.setInt(2, data.getItemId());
					st.setInt(3, data.getCollectionId());
					st.setInt(4, data.getIndex());
					st.addBatch();
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, "Could not store collection for playerId " + _player.getObjectId() + ": ", e);
				}
			});
			st.executeBatch();
			// No need to call con.commit() as HikariCP autocommit is true.
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not store collection for playerId " + _player.getObjectId() + ": ", e);
		}
	}
	
	/**
	 * Saves the current collection favorites to the database.<br>
	 * This method deletes existing records for the account and inserts new ones.<br>
	 * It iterates through the {@code _favorites} collection to perform a batch update.
	 */
	public void storeFavorites()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement st = con.prepareStatement(DELETE_COLLECTION_FAVORITE))
			{
				st.setString(1, _player.getAccountName());
				st.execute();
			}
			
			try (PreparedStatement st = con.prepareStatement(INSERT_COLLECTION_FAVORITE))
			{
				_favorites.forEach(data ->
				{
					try
					{
						st.setString(1, _player.getAccountName());
						st.setInt(2, data);
						st.addBatch();
					}
					catch (Exception e)
					{
						LOGGER.log(Level.SEVERE, "Could not store collection favorite for playerId " + _player.getObjectId() + ": ", e);
					}
				});
				st.executeBatch();
				// No need to call con.commit() as HikariCP autocommit is true.
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not store collection favorite for playerId " + _player.getObjectId() + ": ", e);
		}
	}
	
	/**
	 * Retrieves the list of collections for the player.
	 * @return a {@code List} of {@code PlayerCollectionData} objects.
	 */
	public List<PlayerCollectionData> getCollections()
	{
		return _entries;
	}
	
	/**
	 * Retrieves the list of favorite collections for the player.
	 * @return a {@code List<Integer>} containing the IDs of the favorite collections.
	 */
	public List<Integer> getFavorites()
	{
		return _favorites;
	}
	
	/**
	 * Adds a specific collection to the player's favorites.<br>
	 * This method updates the internal list of favorite IDs.
	 * @param id The unique identifier of the collection to add.
	 */
	public void addFavorite(Integer id)
	{
		_favorites.add(id);
	}
	
	/**
	 * Removes a specific collection from the favorites list.<br>
	 * This method updates the internal favorite collection by removing the entry with the given {@code id}.
	 * @param id The unique identifier of the collection to remove.
	 */
	public void removeFavorite(Integer id)
	{
		_favorites.remove(id);
	}
	
	/**
	 * Checks if a specific collection is finished.<br>
	 * It compares the number of items collected against the required amount.
	 * @param collectionId The unique identifier for the collection to check.
	 * @return {@code true} if the collection is complete, otherwise {@code false}.
	 */
	public boolean isComplete(int collectionId)
	{
		final CollectionDataHolder collectionHolder = CollectionData.getInstance().getCollection(collectionId);
		int count = 0;
		for (PlayerCollectionData data : _entries)
		{
			if (data.getCollectionId() == collectionId)
			{
				count++;
			}
		}
		
		if (count >= collectionHolder.getCompleteCount())
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Adds an entry to the collection list.
	 * @param entry The {@code PlayerCollectionData} to add.
	 */
	public void add(PlayerCollectionData entry)
	{
		_entries.add(entry);
	}
	
	/**
	 * Removes every entry belonging to a collection.
	 * @param collectionId The identifier of the collection to clear.
	 */
	public void removeById(int collectionId)
	{
		final Iterator<PlayerCollectionData> iterator = _entries.iterator();
		while (iterator.hasNext())
		{
			if (iterator.next().getCollectionId() == collectionId)
			{
				iterator.remove();
			}
		}
	}
}
