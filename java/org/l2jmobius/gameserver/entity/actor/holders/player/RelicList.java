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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.data.holders.RelicCollectionDataHolder;
import org.l2jmobius.gameserver.data.holders.RelicCollectionInfoHolder;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.data.xml.RelicCollectionData;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.mechanics.options.Options;
import org.l2jmobius.gameserver.mechanics.variables.AccountVariables;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsActiveInfo;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsAnnounce;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsCollectionCompleteAnnounce;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsCollectionInfo;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsCollectionUpdate;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsExchangeList;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsList;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsUpdateList;

/**
 * Manages the relics of a {@code Player}.<br>
 * It handles the owned relics, their collections and the bonuses they grant.<br>
 * The rows are keyed by account name, so the data is shared across the account.
 * @author Mobius
 */
public class RelicList
{
	private static final Logger LOGGER = Logger.getLogger(RelicList.class.getName());
	
	private static final String INSERT_RELICS = "REPLACE INTO relics (`accountName`, `relicId`, `relicLevel`, `relicCount`, `relicIndex`, `relicSummonTime`) VALUES (?, ?, ?, ?, ?, ?)";
	private static final String RESTORE_RELICS = "SELECT * FROM relics WHERE accountName=? ORDER BY `relicId`";
	private static final String DELETE_RELICS = "DELETE FROM relics WHERE accountName=? AND relicId=? AND relicLevel=? AND relicCount=? AND relicIndex=? AND relicSummonTime=?";
	private static final String INSERT_RELICS_COLLECTION = "REPLACE INTO relic_collections (`accountName`, `relicId`, `relicLevel`, `relicCollectionId`, `index`) VALUES (?, ?, ?, ?, ?)";
	private static final String RESTORE_RELICS_COLLECTION = "SELECT * FROM relic_collections WHERE accountName=? ORDER BY `relicCollectionId`";
	
	private final Player _player;
	private final List<PlayerRelicCollectionData> _collections = new ArrayList<>();
	private final List<PlayerRelicData> _entries = Collections.synchronizedList(new ArrayList<>());
	
	/**
	 * Creates a new {@code RelicList} for a specific player.
	 * @param player The {@code Player} object associated with this instance.
	 */
	public RelicList(Player player)
	{
		_player = player;
	}
	
	/**
	 * Loads the player's relics from the database.<br>
	 * This method clears the current {@code _entries} collection.<br>
	 * It then populates it with data for each relic associated with the account name.
	 */
	public void restore()
	{
		_entries.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_RELICS))
		{
			statement.setString(1, _player.getAccountName());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final int relicId = rset.getInt("relicId");
					final int relicLevel = rset.getInt("relicLevel");
					final int relicCount = rset.getInt("relicCount");
					final int relicIndex = rset.getInt("relicIndex");
					final Long relicSummonTime = rset.getLong("relicSummonTime");
					
					final RelicDataHolder holder = RelicData.getInstance().getRelic(relicId);
					if (holder != null)
					{
						_entries.add(new PlayerRelicData(relicId, relicLevel, relicCount, relicIndex, relicSummonTime));
						// giveRelicSkill(holder);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore relic data for playerId: " + _player.getObjectId(), e);
		}
	}
	
	/**
	 * This method retrieves the active relic information for the player.<br>
	 * It identifies the current relic ID and its level from the player's data.<br>
	 * A packet containing this info is sent to the client after a short delay.
	 */
	public void restoreActiveInfo()
	{
		final int relicId = _player.getVariables().getInt(PlayerVariables.ACTIVE_RELIC, 0);
		int relicLevel1 = 0;
		for (PlayerRelicData relic : getRelics())
		{
			if (relic.getRelicId() == relicId)
			{
				relicLevel1 = relic.getRelicLevel();
				break;
			}
		}
		
		final int relicLevel = relicLevel1;
		ThreadPool.schedule(() -> _player.sendPacket(new ExRelicsActiveInfo(relicId, relicLevel)), 50);
	}
	
	/**
	 * Clears the current relic collections from memory.<br>
	 * Fetches all saved relic collection data from the database for the current account.<br>
	 * Populates the internal list with valid {@code PlayerRelicCollectionData} objects.
	 */
	public void restoreCollections()
	{
		_collections.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_RELICS_COLLECTION))
		{
			statement.setString(1, _player.getAccountName());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final int relicCollectionId = rset.getInt("relicCollectionId");
					if (RelicCollectionData.getInstance().getRelicCollection(relicCollectionId) != null)
					{
						_collections.add(new PlayerRelicCollectionData(relicCollectionId, rset.getInt("relicId"), rset.getInt("relicLevel"), rset.getInt("index")));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore relics collection list data for playerId: " + _player.getObjectId(), e);
		}
	}
	
	/**
	 * This method updates the player's active bonuses based on their relic collections.<br>
	 * It checks if each collection meets the required completion count.<br>
	 * If a collection is complete, it applies the corresponding options to the player.
	 */
	public void restoreCollectionBonuses()
	{
		final Set<Integer> relicCollectionIds = new HashSet<>();
		for (PlayerRelicCollectionData relicCollection : _collections)
		{
			relicCollectionIds.add(relicCollection.getRelicCollectionId());
		}
		
		for (int relicCollectionId : relicCollectionIds)
		{
			final RelicCollectionDataHolder relicCollection = RelicCollectionData.getInstance().getRelicCollection(relicCollectionId);
			int count = 0;
			for (PlayerRelicCollectionData data : _collections)
			{
				if (data.getRelicCollectionId() == relicCollectionId)
				{
					count++;
				}
			}
			
			if (count >= relicCollection.getCompleteCount())
			{
				final Options options = OptionData.getInstance().getOptions(relicCollection.getOptionId());
				if (options != null)
				{
					options.apply(_player);
				}
			}
		}
	}
	
	/**
	 * Saves the player's relic data to the database.<br>
	 * This method iterates through all items in {@code _entries}.<br>
	 * It uses a batch insert to store account information and relic details.
	 */
	public void store()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(INSERT_RELICS))
		{
			_entries.forEach(data ->
			{
				try
				{
					st.setString(1, _player.getAccountName());
					st.setInt(2, data.getRelicId());
					st.setInt(3, data.getRelicLevel());
					st.setInt(4, data.getRelicCount());
					st.setInt(5, data.getRelicIndex());
					st.setLong(6, data.getRelicSummonTime());
					st.addBatch();
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, "Could not store relics for playerId " + _player.getObjectId() + ": ", e);
				}
			});
			st.executeBatch();
			// No need to call con.commit() as HikariCP autocommit is true.
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not store relics for playerId " + _player.getObjectId() + ": ", e);
		}
	}
	
	/**
	 * Saves the current relic collections to the database.<br>
	 * This method iterates through all stored data and executes a batch insert.<br>
	 * It uses {@code DatabaseFactory} to manage the connection.
	 */
	public void storeCollections()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(INSERT_RELICS_COLLECTION))
		{
			_collections.forEach(data ->
			{
				try
				{
					st.setString(1, _player.getAccountName());
					st.setInt(2, data.getRelicId());
					st.setInt(3, data.getRelicLevel());
					st.setInt(4, data.getRelicCollectionId());
					st.setInt(5, data.getIndex());
					st.addBatch();
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, "Could not store relics collection for playerId " + _player.getObjectId() + ": ", e);
				}
			});
			st.executeBatch();
			// No need to call con.commit() as HikariCP autocommit is true.
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not store relics collection for playerId " + _player.getObjectId() + ": ", e);
		}
	}
	
	/**
	 * Removes a specific relic from the database for the current account.<br>
	 * This method executes a SQL delete query using the provided relic details.
	 * @param relicId The unique identifier of the relic to be deleted.
	 * @param relicLevel The level of the relic being removed.
	 * @param relicCount The quantity of the relic in the inventory.
	 * @param relicIndex The specific index position of the relic.
	 * @param relicSummonTime The timestamp when the relic was summoned.
	 */
	public void delete(int relicId, int relicLevel, int relicCount, int relicIndex, long relicSummonTime)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_RELICS))
		{
			statement.setString(1, _player.getAccountName());
			statement.setInt(2, relicId);
			statement.setInt(3, relicLevel);
			statement.setInt(4, relicCount);
			statement.setInt(5, relicIndex);
			statement.setLong(6, relicSummonTime);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not delete relics data for playerId: " + _player.getObjectId(), e);
		}
	}
	
	/**
	 * Retrieves the collection of relics owned by the player.
	 * @return a {@code Collection} of {@code PlayerRelicData} objects.
	 */
	public Collection<PlayerRelicData> getRelics()
	{
		return _entries;
	}
	
	/**
	 * Retrieves the collection of relic data for the player.
	 * @return a {@code Collection} of {@code PlayerRelicCollectionData} objects.
	 */
	public Collection<PlayerRelicCollectionData> getCollections()
	{
		return _collections;
	}
	
	/**
	 * Checks if a specific relic is registered in the player's collection.<br>
	 * It verifies both the {@code relicId} and the {@code relicLevel}.
	 * @param relicId The unique identifier of the relic.
	 * @param relicLevel The current level of the relic.
	 * @return {@code true} if the relic is found in any collection, {@code false} otherwise.
	 */
	public boolean isRegistered(int relicId, int relicLevel)
	{
		final Set<Integer> result = new HashSet<>();
		for (PlayerRelicCollectionData collectionData : _collections)
		{
			if ((collectionData.getRelicId() == relicId) && (collectionData.getRelicLevel() == relicLevel))
			{
				result.add(collectionData.getRelicCollectionId());
			}
			continue;
		}
		
		return result.contains(relicId);
	}
	
	/**
	 * Checks if a specific relic is part of a collection.<br>
	 * This method searches through all registered collections for a match.
	 * @param relicId The unique identifier of the relic to find.
	 * @param relicCollectionId The unique identifier of the collection to check.
	 * @return {@code true} if the relic is found in the specified collection, otherwise {@code false}.
	 */
	public boolean isRegisteredInCollection(int relicId, int relicCollectionId)
	{
		for (PlayerRelicCollectionData collectionData : _collections)
		{
			if ((collectionData.getRelicId() == relicId) && (collectionData.getRelicCollectionId() == relicCollectionId))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if a specific relic collection is finished.<br>
	 * It compares the number of owned relics against the total required for the given ID.
	 * @param relicCollectionId The unique identifier of the relic collection to check.
	 * @return {@code true} if all required relics are collected, otherwise {@code false}.
	 */
	public boolean isCompleteCollection(int relicCollectionId)
	{
		final RelicCollectionDataHolder collectionHolder = RelicCollectionData.getInstance().getRelicCollection(relicCollectionId);
		if (collectionHolder == null)
		{
			return false;
		}
		
		final int completeRelicsInCollection = collectionHolder.getCompleteCount();
		final Set<Integer> result = new HashSet<>();
		for (PlayerRelicCollectionData collectionData : _collections)
		{
			if ((collectionData.getRelicCollectionId() == relicCollectionId))
			{
				result.add(collectionData.getRelicId());
			}
		}
		
		if (completeRelicsInCollection == result.size())
		{
			if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				_player.sendMessage("Collection id: " + relicCollectionId + " is complete.");
				_player.sendMessage("Relics in collection id: " + relicCollectionId + " : " + result);
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Processes the acquisition of a new relic for the player.<br>
	 * It checks if the relic exists in {@code RelicData}.<br>
	 * High grade relics are added to an unconfirmed list and broadcast to all players.<br>
	 * Lower grade relics increment their count or create a new entry in the player's collection.
	 * @param obtainedRelicId The unique identifier of the relic being acquired.
	 */
	public void handleAcquisition(int obtainedRelicId)
	{
		final RelicDataHolder relicTemplate = RelicData.getInstance().getRelic(obtainedRelicId);
		if (relicTemplate == null)
		{
			PacketLogger.warning("handleAcquisition: Relic ID " + obtainedRelicId + " is not registered in RelicData.");
			return;
		}
		
		final boolean isHighGrade = (relicTemplate.getGrade() == RelicGrade.BGRADE) || (relicTemplate.getGrade() == RelicGrade.AGRADE);
		final Collection<PlayerRelicData> storedRelics = getRelics();
		final PlayerRelicData existingRelic = storedRelics.stream().filter(relic -> relic.getRelicId() == obtainedRelicId).findFirst().orElse(null);
		final PlayerRelicData newRelic = new PlayerRelicData(obtainedRelicId, 0, 0, 0, 0);
		
		if (isHighGrade)
		{
			// B/A grade relics go into confirmation list (index >= 300) before being confirmed.
			final long unconfirmedCount = storedRelics.stream().filter(r -> (r.getRelicIndex() >= 300) && (r.getRelicId() == obtainedRelicId)).count();
			newRelic.setRelicCount(1);
			newRelic.setRelicIndex(300 + (int) unconfirmedCount);
			newRelic.setRelicSummonTime(System.currentTimeMillis());
			storedRelics.add(newRelic);
			store();
			final AccountVariables accountVars = _player.getAccountVariables();
			accountVars.set(AccountVariables.UNCONFIRMED_RELICS_COUNT, accountVars.getInt(AccountVariables.UNCONFIRMED_RELICS_COUNT, 0) + 1);
			accountVars.storeMe();
			_player.sendPacket(new ExRelicsExchangeList(_player));
			World.broadcastToAllOnlinePlayers(new ExRelicsAnnounce(_player, obtainedRelicId, ExRelicsAnnounce.TYPE_SUMMON));
		}
		else if (existingRelic != null)
		{
			// Existing No/D/C grade relic - increment count.
			existingRelic.setRelicCount(existingRelic.getRelicCount() + 1);
			store();
			_player.sendPacket(new ExRelicsUpdateList(1, existingRelic.getRelicId(), 0, 1));
			handle(existingRelic);
		}
		else
		{
			// New No/D/C grade relic - add fresh.
			newRelic.setRelicCount(1);
			storedRelics.add(newRelic);
			store();
			_player.sendPacket(new ExRelicsUpdateList(1, newRelic.getRelicId(), 0, 0));
			handle(newRelic);
		}
		
		// giveRelicSkill(relicTemplate, 0);
	}
	
	/**
	 * Registers an obtained relic into every collection that wants it, and applies the collection option once one completes.<br>
	 * The relic list, the database and the player's options are all touched here, on the thread that obtained the relic, so the packet that follows only writes what this returns.
	 * @param obtainedRelicId The relic that was obtained.
	 * @return The collection that completed, or {@code 0} when none did.
	 */
	public int updateCollection(int obtainedRelicId)
	{
		PlayerRelicData existingRelic = null;
		
		// Check if the relic with the same ID exists.
		for (PlayerRelicData relic : getRelics())
		{
			final int relicId = relic.getRelicId();
			if ((relicId == obtainedRelicId) && (relic.getRelicIndex() == 0)) // Only relics with index 0 can be added to collection.
			{
				existingRelic = relic;
				break;
			}
		}
		
		// Check if obtained relic is required in some collection.
		if (existingRelic != null)
		{
			final int relicId = existingRelic.getRelicId();
			int neededRelicCollectionId = 0;
			int neededRelicIndex = 0;
			int neededRelicLevel = 0;
			for (RelicCollectionDataHolder cRelicCollectionHolder : RelicCollectionData.getInstance().getRelicCollections())
			{
				// Find the relicId into collections.
				for (RelicCollectionInfoHolder relicData : cRelicCollectionHolder.getRelics())
				{
					// Relic id found.
					if ((relicData.getRelicId() == relicId) && (!isRegisteredInCollection(relicId, cRelicCollectionHolder.getCollectionId())))
					{
						for (int i = 0; i < cRelicCollectionHolder.getRelics().size(); i++)
						{
							final RelicCollectionInfoHolder relic = cRelicCollectionHolder.getRelic(i);
							if (relic.getRelicId() == relicId)
							{
								neededRelicCollectionId = cRelicCollectionHolder.getCollectionId();
								neededRelicIndex = i; // Position found.
								neededRelicLevel = cRelicCollectionHolder.getRelic(i).getEnchantLevel();
								
								// Add relic to collection if matches the level needed.
								if (existingRelic.getRelicLevel() >= neededRelicLevel)
								{
									if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
									{
										_player.sendMessage("2.Relic id: " + relicId + " with level: " + existingRelic.getRelicLevel() + " needed in collection: " + neededRelicCollectionId);
									}
									
									// Update Relic Collections from db.
									getCollections().add(new PlayerRelicCollectionData(neededRelicCollectionId, relicId, neededRelicLevel, neededRelicIndex));
									storeCollections();
									_player.sendPacket(new ExRelicsCollectionInfo(_player));
									if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
									{
										_player.sendMessage("Added Relic Id: " + relicId + " into Collection Id: " + neededRelicCollectionId);
									}
								}
							}
						}
					}
				}
			}
			
			if ((neededRelicCollectionId != 0) && !isRegistered(relicId, existingRelic.getRelicLevel()))
			{
				// Check if collection is complete and give skills.
				final boolean isComplete = isCompleteCollection(neededRelicCollectionId);
				if (isComplete)
				{
					// Announce Collection Complete.
					_player.sendPacket(new ExRelicsCollectionCompleteAnnounce(neededRelicCollectionId));
					
					// Apply collection option if all requirements are met.
					final Options options = OptionData.getInstance().getOptions(RelicCollectionData.getInstance().getRelicCollection(neededRelicCollectionId).getOptionId());
					if (options != null)
					{
						options.apply(_player);
						if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
						{
							_player.sendMessage("Added Skill for complete collection: " + options.getPassiveSkills());
						}
					}
					
					return neededRelicCollectionId;
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Confirms one unconfirmed relic, moving it into the player's own list.<br>
	 * The relic list, the database and the account variables are all touched here, on the thread that confirmed the relic, so the packet that follows only writes what it was given.
	 * @param relicId The relic being confirmed.
	 */
	public void confirmExchange(int relicId)
	{
		final PlayerRelicData newRelic = new PlayerRelicData(relicId, 0, 0, 0, 0);
		
		// Add to database table the confirmed relics.
		PlayerRelicData existingRelic = null;
		PlayerRelicData existingUnconfirmedRelic = null;
		
		// Check if the relic with the same ID exists.
		for (PlayerRelicData relic : getRelics())
		{
			if (relic.getRelicId() == relicId)
			{
				if ((relic.getRelicIndex() >= 300) && (relic.getRelicCount() > 0))
				{
					existingUnconfirmedRelic = relic;
				}
				else if (relic.getRelicIndex() == 0)
				{
					existingRelic = relic;
				}
			}
		}
		
		if ((existingUnconfirmedRelic != null) && (existingRelic != null))
		{
			// Decrease the count of the existing relic with index >= 300.
			final int unconfirmedIndex = existingUnconfirmedRelic.getRelicIndex();
			if (unconfirmedIndex >= 300)
			{
				existingUnconfirmedRelic.setRelicCount(existingUnconfirmedRelic.getRelicCount() - 1);
				_player.getAccountVariables().set(AccountVariables.UNCONFIRMED_RELICS_COUNT, _player.getAccountVariables().getInt(AccountVariables.UNCONFIRMED_RELICS_COUNT, 0) - 1);
				_player.getAccountVariables().storeMe();
				store();
				if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
				{
					_player.sendMessage("1.Existing unconfirmed Relic Id: " + existingUnconfirmedRelic.getRelicId() + " Index: " + unconfirmedIndex + " count decreased.");
				}
			}
			
			// Increase the count of the existing relic with index == 0.
			final int confirmedIndex = existingRelic.getRelicIndex();
			if (confirmedIndex == 0)
			{
				existingRelic.setRelicCount(existingRelic.getRelicCount() + 1);
				store();
				_player.sendPacket(new ExRelicsList(_player)); // Update confirmed relic list relics count.
				_player.sendPacket(new ExRelicsUpdateList(1, existingRelic.getRelicId(), 0, existingRelic.getRelicCount() + 1)); // Update confirmed relic list with new relic.
				if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
				{
					_player.sendMessage("2.Existing Relic Id: " + existingRelic.getRelicId() + " Index: " + confirmedIndex + " count increased.");
				}
				
				final int relicLevel = existingRelic.getRelicLevel();
				if (!isRegistered(existingRelic.getRelicId(), relicLevel))
				{
					// Auto-Add to relic collections on summon.
					_player.sendPacket(new ExRelicsCollectionUpdate(_player, existingRelic.getRelicId(), relicLevel)); // Update collection list.
				}
			}
		}
		else if ((existingUnconfirmedRelic != null) && (existingRelic == null))
		{
			// Decrease the count of the existing relic with index >= 300.
			final int unconfirmedIndex = existingUnconfirmedRelic.getRelicIndex();
			if (unconfirmedIndex >= 300)
			{
				existingUnconfirmedRelic.setRelicCount(existingUnconfirmedRelic.getRelicCount() - 1);
				_player.getAccountVariables().set(AccountVariables.UNCONFIRMED_RELICS_COUNT, _player.getAccountVariables().getInt(AccountVariables.UNCONFIRMED_RELICS_COUNT, 0) - 1);
				_player.getAccountVariables().storeMe();
				if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
				{
					_player.sendMessage("1.Existing unconfirmed Relic Id: " + existingUnconfirmedRelic.getRelicId() + " Index: " + unconfirmedIndex + " count decreased.");
				}
			}
			
			// Add the new relic with index == 0 if it doesn't exist.
			add(newRelic);
			store();
			_player.sendPacket(new ExRelicsUpdateList(1, newRelic.getRelicId(), 0, 0)); // Update confirmed relic list with new relic.
			if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				_player.sendMessage("3.New Relic Id: " + newRelic.getRelicId() + " Index: " + newRelic.getRelicIndex() + " was added.");
			}
			
			if (!isRegistered(newRelic.getRelicId(), newRelic.getRelicLevel()))
			{
				// Auto-Add to relic collections on summon.
				_player.sendPacket(new ExRelicsCollectionUpdate(_player, newRelic.getRelicId(), newRelic.getRelicLevel())); // Update collection list.
			}
		}
		
		_player.sendPacket(new ExRelicsList(_player));// Update confirmed relic list.
		_player.sendPacket(new ExRelicsExchangeList(_player)); // Update relic exchange/confirm list.
		
		// Cleanup (remove confirmed relics with index 300+ and count 0 from db).
		if ((existingUnconfirmedRelic != null) && (existingUnconfirmedRelic.getRelicCount() == 0))
		{
			final int unconfirmedIndex = existingUnconfirmedRelic.getRelicIndex();
			delete(existingUnconfirmedRelic.getRelicId(), existingUnconfirmedRelic.getRelicLevel(), existingUnconfirmedRelic.getRelicCount(), unconfirmedIndex, existingUnconfirmedRelic.getRelicSummonTime());
			remove(existingUnconfirmedRelic);
			if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				_player.sendMessage("4.Cleanup Relic Id: " + existingUnconfirmedRelic.getRelicId() + " Index: " + unconfirmedIndex + " was removed from db.");
			}
		}
	}
	
	/**
	 * Processes the logic for a player's relic.<br>
	 * This method broadcasts an announcement to all online players.<br>
	 * It also updates the collection if the relic is not yet registered.
	 * @param relic The {@code PlayerRelicData} object containing the relic information.
	 */
	public void handle(PlayerRelicData relic)
	{
		final int relicId = relic.getRelicId();
		World.broadcastToAllOnlinePlayers(new ExRelicsAnnounce(_player, relicId, ExRelicsAnnounce.TYPE_SUMMON));
		
		if (!isRegistered(relicId, relic.getRelicLevel()))
		{
			_player.sendPacket(new ExRelicsCollectionUpdate(_player, relicId, relic.getRelicLevel()));
		}
	}
	
	/**
	 * Adds a relic to the list.
	 * @param relic The {@code PlayerRelicData} to add.
	 */
	public void add(PlayerRelicData relic)
	{
		_entries.add(relic);
	}
	
	/**
	 * Removes a relic from the list.
	 * @param relic The {@code PlayerRelicData} to remove.
	 */
	public void remove(PlayerRelicData relic)
	{
		_entries.remove(relic);
	}
	
	/**
	 * Removes every relic collection entry belonging to a collection.
	 * @param relicCollectionId The identifier of the relic collection to clear.
	 */
	public void removeCollectionById(int relicCollectionId)
	{
		final Iterator<PlayerRelicCollectionData> iterator = _collections.iterator();
		while (iterator.hasNext())
		{
			if (iterator.next().getRelicCollectionId() == relicCollectionId)
			{
				iterator.remove();
			}
		}
	}
}
