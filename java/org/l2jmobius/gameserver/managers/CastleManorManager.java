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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.manor.CropProcure;
import org.l2jmobius.gameserver.mechanics.siege.manor.ManorMode;
import org.l2jmobius.gameserver.mechanics.siege.manor.Seed;
import org.l2jmobius.gameserver.mechanics.siege.manor.SeedProduction;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the castle manor system within the game server.<br>
 * This class handles the logic for manor mechanics such as crop production and seed management.
 * @author malyelfik, Stayway
 */
public class CastleManorManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CastleManorManager.class.getName());
	
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");
	
	// SQL queries
	private static final String INSERT_PRODUCT = "INSERT INTO castle_manor_production VALUES (?, ?, ?, ?, ?, ?)";
	private static final String INSERT_CROP = "INSERT INTO castle_manor_procure VALUES (?, ?, ?, ?, ?, ?, ?)";
	
	// Current manor status
	private ManorMode _mode = ManorMode.APPROVED;
	
	// Temporary date
	private Calendar _nextModeChange = null;
	
	// Seeds holder
	private static final Map<Integer, Seed> _seeds = new HashMap<>();
	
	// Manor period settings
	private final Map<Integer, List<CropProcure>> _procure = new HashMap<>();
	private final Map<Integer, List<CropProcure>> _procureNext = new HashMap<>();
	private final Map<Integer, List<SeedProduction>> _production = new HashMap<>();
	private final Map<Integer, List<SeedProduction>> _productionNext = new HashMap<>();
	
	/**
	 * Initializes the {@code CastleManorManager} and sets up the manor system.<br>
	 * This constructor checks if manors are enabled in the configuration.<br>
	 * It loads data from files and databases to prepare the game state.<br>
	 * It also determines the initial {@code ManorMode} based on the current server time.
	 */
	public CastleManorManager()
	{
		if (GeneralConfig.ALLOW_MANOR)
		{
			// Load seed data from XML and castle manor data from the database.
			load();
			loadDb();
			
			// Set the initial manor mode based on the current time.
			final Calendar currentTime = Calendar.getInstance();
			final int hour = currentTime.get(Calendar.HOUR_OF_DAY);
			final int min = currentTime.get(Calendar.MINUTE);
			final int maintenanceMin = GeneralConfig.ALT_MANOR_REFRESH_MIN + GeneralConfig.ALT_MANOR_MAINTENANCE_MIN;
			if (((hour >= GeneralConfig.ALT_MANOR_REFRESH_TIME) && (min >= maintenanceMin)) || (hour < GeneralConfig.ALT_MANOR_APPROVE_TIME) || ((hour == GeneralConfig.ALT_MANOR_APPROVE_TIME) && (min <= GeneralConfig.ALT_MANOR_APPROVE_MIN)))
			{
				_mode = ManorMode.MODIFIABLE;
			}
			else if ((hour == GeneralConfig.ALT_MANOR_REFRESH_TIME) && (min >= GeneralConfig.ALT_MANOR_REFRESH_MIN) && (min < maintenanceMin))
			{
				_mode = ManorMode.MAINTENANCE;
			}
			else
			{
				_mode = ManorMode.APPROVED;
			}
			
			// Schedule the mode change task.
			scheduleModeChange();
			
			// Schedule auto-save if not saving all actions.
			if (!GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS)
			{
				ThreadPool.scheduleAtFixedRate(this::storeMe, GeneralConfig.ALT_MANOR_SAVE_PERIOD_RATE * 60 * 60 * 1000, GeneralConfig.ALT_MANOR_SAVE_PERIOD_RATE * 60 * 60 * 1000);
			}
		}
		else
		{
			_mode = ManorMode.DISABLED;
			_nextModeChange = null;
			LOGGER.info(getClass().getSimpleName() + ": Manor system is deactivated.");
		}
	}
	
	/**
	 * Loads the seed data from the configuration files.<br>
	 * This method parses the {@code Seeds.xml} file and logs the total count of loaded seeds.
	 */
	@Override
	public void load()
	{
		parseDatapackFile("data/Seeds.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _seeds.size() + " seeds.");
	}
	
	/**
	 * Parses the provided {@code Document} to load seed data.<br>
	 * It iterates through the XML structure to find castle and crop information.<br>
	 * The extracted values are used to populate the internal seeds collection.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		StatSet set;
		NamedNodeMap attrs;
		Node att;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("castle".equalsIgnoreCase(d.getNodeName()))
					{
						final int castleId = parseInteger(d.getAttributes(), "id");
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("crop".equalsIgnoreCase(c.getNodeName()))
							{
								set = new StatSet();
								set.set("castleId", castleId);
								attrs = c.getAttributes();
								for (int i = 0; i < attrs.getLength(); i++)
								{
									att = attrs.item(i);
									set.set(att.getNodeName(), att.getNodeValue());
								}
								
								_seeds.put(set.getInt("seedId"), new Seed(set));
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Loads the castle manor production and procure data from the database.<br>
	 * This method iterates through all castles to populate internal maps with current and next period data.<br>
	 * It handles both {@code SeedProduction} and {@code CropProcure} records.
	 */
	private void loadDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement stProduction = con.prepareStatement("SELECT * FROM castle_manor_production WHERE castle_id=?");
			PreparedStatement stProcure = con.prepareStatement("SELECT * FROM castle_manor_procure WHERE castle_id=?"))
		{
			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				final int castleId = castle.getResidenceId();
				
				// Clear parameters for reuse.
				stProduction.clearParameters();
				stProcure.clearParameters();
				
				// Load seed production data for the castle.
				final List<SeedProduction> currentProduction = new ArrayList<>();
				final List<SeedProduction> nextProduction = new ArrayList<>();
				stProduction.setInt(1, castleId);
				
				try (ResultSet rs = stProduction.executeQuery())
				{
					while (rs.next())
					{
						final int seedId = rs.getInt("seed_id");
						if (_seeds.containsKey(seedId))
						{
							final SeedProduction seedProduction = new SeedProduction(seedId, rs.getLong("amount"), rs.getLong("price"), rs.getInt("start_amount"));
							
							// Separate current and next period productions.
							if (rs.getBoolean("next_period"))
							{
								nextProduction.add(seedProduction);
							}
							else
							{
								currentProduction.add(seedProduction);
							}
						}
						else
						{
							LOGGER.warning(getClass().getSimpleName() + ": Unknown seed ID: " + seedId);
						}
					}
				}
				
				_production.put(castleId, currentProduction);
				_productionNext.put(castleId, nextProduction);
				
				// Load crop procure data for the castle.
				final List<CropProcure> currentProcure = new ArrayList<>();
				final List<CropProcure> nextProcure = new ArrayList<>();
				stProcure.setInt(1, castleId);
				
				try (ResultSet rs = stProcure.executeQuery())
				{
					final Set<Integer> knownCropIds = getCropIds();
					while (rs.next())
					{
						final int cropId = rs.getInt("crop_id");
						if (knownCropIds.contains(cropId))
						{
							final CropProcure cropProcure = new CropProcure(cropId, rs.getLong("amount"), rs.getInt("reward_type"), rs.getLong("start_amount"), rs.getLong("price"));
							
							// Separate current and next period procurements.
							if (rs.getBoolean("next_period"))
							{
								nextProcure.add(cropProcure);
							}
							else
							{
								currentProcure.add(cropProcure);
							}
						}
						else
						{
							LOGGER.warning(getClass().getSimpleName() + ": Unknown crop ID: " + cropId);
						}
					}
				}
				
				_procure.put(castleId, currentProcure);
				_procureNext.put(castleId, nextProcure);
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Manor data loaded.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Unable to load manor data!", e);
		}
	}
	
	/**
	 * Calculates and schedules the next transition for the manor mode.<br>
	 * It determines the target time based on the current {@code _mode}.<br>
	 * The method uses values from {@code GeneralConfig} to set the hour and minute.<br>
	 * If the calculated time has passed, it schedules the change for the next day.<br>
	 * Finally, it uses {@code ThreadPool} to execute {@code changeMode} after the delay.
	 */
	private void scheduleModeChange()
	{
		// Initialize next mode change time to the current date with seconds reset to zero.
		_nextModeChange = Calendar.getInstance();
		_nextModeChange.set(Calendar.SECOND, 0);
		
		// Determine mode-specific time for the next change.
		switch (_mode)
		{
			case MODIFIABLE:
			{
				_nextModeChange.set(Calendar.HOUR_OF_DAY, GeneralConfig.ALT_MANOR_APPROVE_TIME);
				_nextModeChange.set(Calendar.MINUTE, GeneralConfig.ALT_MANOR_APPROVE_MIN);
				
				// If the time has already passed today, schedule for tomorrow.
				if (_nextModeChange.before(Calendar.getInstance()))
				{
					_nextModeChange.add(Calendar.DATE, 1);
				}
				break;
			}
			case MAINTENANCE:
			{
				_nextModeChange.set(Calendar.HOUR_OF_DAY, GeneralConfig.ALT_MANOR_REFRESH_TIME);
				_nextModeChange.set(Calendar.MINUTE, GeneralConfig.ALT_MANOR_REFRESH_MIN + GeneralConfig.ALT_MANOR_MAINTENANCE_MIN);
				
				// If the time has already passed today, schedule for tomorrow.
				if (_nextModeChange.before(Calendar.getInstance()))
				{
					_nextModeChange.add(Calendar.DATE, 1);
				}
				break;
			}
			case APPROVED:
			{
				_nextModeChange.set(Calendar.HOUR_OF_DAY, GeneralConfig.ALT_MANOR_REFRESH_TIME);
				_nextModeChange.set(Calendar.MINUTE, GeneralConfig.ALT_MANOR_REFRESH_MIN);
				
				// If the time has already passed today, schedule for tomorrow.
				if (_nextModeChange.before(Calendar.getInstance()))
				{
					_nextModeChange.add(Calendar.DATE, 1);
				}
				break;
			}
			case DISABLED:
			{
				// If mode is DISABLED, don't schedule.
				return;
			}
			default:
			{
				// For any unexpected mode, don't schedule.
				return;
			}
		}
		
		// Schedule mode change.
		long delay = _nextModeChange.getTimeInMillis() - System.currentTimeMillis();
		
		// Ensure delay is not negative (minimum 1 second).
		if (delay < 1000)
		{
			delay = 1000; // Schedule for at least 1 second from now.
			_nextModeChange.setTimeInMillis(System.currentTimeMillis() + delay);
		}
		
		ThreadPool.schedule(this::changeMode, delay);
	}
	
	/**
	 * Transitions the manor system to its next operational state.<br>
	 * This method handles logic for moving between {@code APPROVED}, {@code MAINTENANCE}, and {@code MODIFIABLE} modes.<br>
	 * It updates castle production data, processes crop rewards, and validates clan resources based on the current mode.
	 */
	public void changeMode()
	{
		switch (_mode)
		{
			case APPROVED:
			{
				// Transition to maintenance mode.
				_mode = ManorMode.MAINTENANCE;
				
				// Update manor period for each castle.
				for (Castle castle : CastleManager.getInstance().getCastles())
				{
					final Clan owner = castle.getOwner();
					if (owner == null)
					{
						continue;
					}
					
					final int castleId = castle.getResidenceId();
					final ItemContainer cwh = owner.getWarehouse();
					
					// Process crop procurement and treasury updates.
					for (CropProcure crop : _procure.get(castleId))
					{
						final long cropStartAmount = crop.getStartAmount();
						if (cropStartAmount > 0)
						{
							final long cropAmount = crop.getAmount();
							long harvestedAmount = (long) ((cropStartAmount - cropAmount) * 0.9);
							if ((harvestedAmount < 1) && (Rnd.get(99) < 90))
							{
								harvestedAmount = 1;
							}
							
							// Add harvested crops to the clan warehouse.
							if (harvestedAmount > 0)
							{
								cwh.addItem(ItemProcessType.REWARD, getSeedByCrop(crop.getId()).getMatureId(), harvestedAmount, null, null);
							}
							
							// Add unspent amount to castle treasury.
							if (cropAmount > 0)
							{
								castle.addToTreasuryNoTax(cropAmount * crop.getPrice());
							}
						}
					}
					
					// Update current production and procurements to the next period's data.
					_production.put(castleId, _productionNext.get(castleId));
					_procure.put(castleId, _procureNext.get(castleId));
					
					// Prepare data for the upcoming period, depending on treasury funds.
					if (castle.getTreasury() < getManorCost(castleId, false))
					{
						_productionNext.put(castleId, Collections.emptyList());
						_procureNext.put(castleId, Collections.emptyList());
					}
					else
					{
						// Reset production and procurement amounts for the next period.
						final List<SeedProduction> productionList = new ArrayList<>(_productionNext.get(castleId));
						for (SeedProduction seed : productionList)
						{
							seed.setAmount(seed.getStartAmount());
						}
						
						_productionNext.put(castleId, productionList);
						
						final List<CropProcure> procureList = new ArrayList<>(_procureNext.get(castleId));
						for (CropProcure crop : procureList)
						{
							crop.setAmount(crop.getStartAmount());
						}
						
						_procureNext.put(castleId, procureList);
					}
				}
				
				// Save the updated manor data.
				storeMe();
				break;
			}
			case MAINTENANCE:
			{
				// Notify clan leaders of the manor mode change.
				for (Castle castle : CastleManager.getInstance().getCastles())
				{
					final Clan owner = castle.getOwner();
					if (owner != null)
					{
						final ClanMember clanLeader = owner.getLeader();
						if ((clanLeader != null) && clanLeader.isOnline())
						{
							clanLeader.getPlayer().sendPacket(SystemMessageId.THE_MANOR_INFORMATION_HAS_BEEN_UPDATED);
						}
					}
				}
				
				// Transition to modifiable mode.
				_mode = ManorMode.MODIFIABLE;
				break;
			}
			case MODIFIABLE:
			{
				// Transition to approved mode.
				_mode = ManorMode.APPROVED;
				
				// Validate each castle's funds and warehouse capacity.
				for (Castle castle : CastleManager.getInstance().getCastles())
				{
					final Clan owner = castle.getOwner();
					if (owner == null)
					{
						continue;
					}
					
					final int castleId = castle.getResidenceId();
					final ItemContainer cwh = owner.getWarehouse();
					
					// Calculate the necessary slots in the warehouse for new crops.
					int requiredSlots = 0;
					for (CropProcure crop : _procureNext.get(castleId))
					{
						if ((crop.getStartAmount() > 0) && (cwh.getAllItemsByItemId(getSeedByCrop(crop.getId()).getMatureId()) == null))
						{
							requiredSlots++;
						}
					}
					
					final long manorCost = getManorCost(castleId, true);
					
					// Check if there's enough capacity and funds.
					if (!cwh.validateCapacity(requiredSlots) && (castle.getTreasury() < manorCost))
					{
						// Clear next period data if insufficient resources.
						_productionNext.get(castleId).clear();
						_procureNext.get(castleId).clear();
						
						// Notify clan leader.
						final ClanMember clanLeader = owner.getLeader();
						if ((clanLeader != null) && clanLeader.isOnline())
						{
							clanLeader.getPlayer().sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_FUNDS_IN_THE_CLAN_WAREHOUSE_FOR_THE_MANOR_TO_OPERATE);
						}
					}
					else // Deduct manor cost from treasury.
					{
						castle.addToTreasuryNoTax(-manorCost);
					}
				}
				
				// Store changes if configured to save all actions.
				if (GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS)
				{
					storeMe();
				}
				break;
			}
			case DISABLED:
			{
				// Do nothing if disabled.
				return;
			}
		}
		
		// Schedule the next mode change.
		scheduleModeChange();
	}
	
	/**
	 * Sets the production data for the next period for a specific castle.<br>
	 * This method updates the internal map and saves the data to the database if enabled in {@code GeneralConfig}.
	 * @param list The list of {@code SeedProduction} objects to set.
	 * @param castleId The unique identifier of the castle.
	 */
	public void setNextSeedProduction(List<SeedProduction> list, int castleId)
	{
		_productionNext.put(castleId, list);
		
		// Save actions to the database if configured to do so.
		if (GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement deleteStmt = con.prepareStatement("DELETE FROM castle_manor_production WHERE castle_id = ? AND next_period = 1");
				PreparedStatement insertStmt = con.prepareStatement(INSERT_PRODUCT))
			{
				// Delete existing production data for the next period.
				deleteStmt.setInt(1, castleId);
				deleteStmt.executeUpdate();
				
				// Insert new production data if list is not empty.
				for (SeedProduction sp : list)
				{
					insertStmt.setInt(1, castleId);
					insertStmt.setInt(2, sp.getId());
					insertStmt.setLong(3, sp.getAmount());
					insertStmt.setLong(4, sp.getStartAmount());
					insertStmt.setLong(5, sp.getPrice());
					insertStmt.setBoolean(6, true);
					insertStmt.addBatch();
				}
				
				// Execute batch insert for new production data.
				if (!list.isEmpty())
				{
					insertStmt.executeBatch();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Unable to store manor data!", e);
			}
		}
	}
	
	/**
	 * Sets the crop procurement data for the next period for a specific castle.<br>
	 * This method updates the internal map and saves the data to the database if enabled in {@code GeneralConfig}.
	 * @param list The list of {@code CropProcure} objects to set.
	 * @param castleId The unique identifier of the castle.
	 */
	public void setNextCropProcure(List<CropProcure> list, int castleId)
	{
		_procureNext.put(castleId, list);
		
		// Save actions to the database if configured to do so.
		if (GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement deleteStmt = con.prepareStatement("DELETE FROM castle_manor_procure WHERE castle_id = ? AND next_period = 1");
				PreparedStatement insertStmt = con.prepareStatement(INSERT_CROP))
			{
				// Delete existing procure data for the next period.
				deleteStmt.setInt(1, castleId);
				deleteStmt.executeUpdate();
				
				// Insert new procure data if list is not empty.
				for (CropProcure cp : list)
				{
					insertStmt.setInt(1, castleId);
					insertStmt.setInt(2, cp.getId());
					insertStmt.setLong(3, cp.getAmount());
					insertStmt.setLong(4, cp.getStartAmount());
					insertStmt.setLong(5, cp.getPrice());
					insertStmt.setInt(6, cp.getReward());
					insertStmt.setBoolean(7, true);
					insertStmt.addBatch();
				}
				
				// Execute batch insert for new procure data.
				if (!list.isEmpty())
				{
					insertStmt.executeBatch();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Unable to store manor data!", e);
			}
		}
	}
	
	/**
	 * Updates the current production amounts for a specific castle.<br>
	 * This method saves the provided {@code SeedProduction} items into the database.<br>
	 * It targets records where {@code next_period} is {@code 0}.
	 * @param castleId The unique identifier of the castle to update.
	 * @param items A collection of {@code SeedProduction} objects containing the new production data.
	 */
	public void updateCurrentProduction(int castleId, Collection<SeedProduction> items)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle_manor_production SET amount = ? WHERE castle_id = ? AND seed_id = ? AND next_period = 0"))
		{
			for (SeedProduction sp : items)
			{
				ps.setLong(1, sp.getAmount());
				ps.setInt(2, castleId);
				ps.setInt(3, sp.getId());
				ps.addBatch();
			}
			
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Unable to update current production data!", e);
		}
	}
	
	/**
	 * Updates the current procurement amounts for a specific castle.<br>
	 * This method saves the provided {@code CropProcure} items into the database.<br>
	 * It targets records where the {@code next_period} is set to 0.
	 * @param castleId The unique identifier of the castle to update.
	 * @param items A collection of {@code CropProcure} objects containing the new data.
	 */
	public void updateCurrentProcure(int castleId, Collection<CropProcure> items)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle_manor_procure SET amount = ? WHERE castle_id = ? AND crop_id = ? AND next_period = 0"))
		{
			for (CropProcure cp : items)
			{
				ps.setLong(1, cp.getAmount());
				ps.setInt(2, castleId);
				ps.setInt(3, cp.getId());
				ps.addBatch();
			}
			
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Unable to update current procure data!", e);
		}
	}
	
	/**
	 * Retrieves the seed production data for a specific castle.<br>
	 * It returns either current or future production based on the provided flag.
	 * @param castleId The unique identifier of the castle.
	 * @param nextPeriod Set to {@code true} to get the next period, or {@code false} for the current period.
	 * @return A {@code List} of {@code SeedProduction} objects for the requested period.
	 */
	public List<SeedProduction> getSeedProduction(int castleId, boolean nextPeriod)
	{
		return nextPeriod ? _productionNext.get(castleId) : _production.get(castleId);
	}
	
	/**
	 * Retrieves a specific {@code SeedProduction} object based on the provided IDs.<br>
	 * This method searches through all productions for a given castle and period.<br>
	 * It returns the first matching product or {@code null} if no match is found.
	 * @param castleId The unique identifier of the castle.
	 * @param seedId The unique identifier of the specific seed product.
	 * @param nextPeriod Determines whether to fetch data for the current or next period.
	 * @return The matching {@code SeedProduction} object, or {@code null}.
	 */
	public SeedProduction getSeedProduct(int castleId, int seedId, boolean nextPeriod)
	{
		for (SeedProduction sp : getSeedProduction(castleId, nextPeriod))
		{
			if (sp.getId() == seedId)
			{
				return sp;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the list of crop procurements for a specific castle.<br>
	 * It selects data based on whether the requested period is the next one or the current one.
	 * @param castleId The unique identifier of the castle.
	 * @param nextPeriod Set to {@code true} to get the next period, or {@code false} for the current period.
	 * @return A {@code List} of {@code CropProcure} objects for the specified castle and period.
	 */
	public List<CropProcure> getCropProcure(int castleId, boolean nextPeriod)
	{
		return nextPeriod ? _procureNext.get(castleId) : _procure.get(castleId);
	}
	
	/**
	 * Retrieves a specific {@code CropProcure} object based on the castle and crop ID.<br>
	 * This method searches through all procures for the given period to find a match.
	 * @param castleId The unique identifier of the castle.
	 * @param cropId The unique identifier of the crop.
	 * @param nextPeriod Set to {@code true} to fetch data for the next period, or {@code false} for the current one.
	 * @return The matching {@code CropProcure} object, or {@code null} if no match is found.
	 */
	public CropProcure getCropProcure(int castleId, int cropId, boolean nextPeriod)
	{
		for (CropProcure cp : getCropProcure(castleId, nextPeriod))
		{
			if (cp.getId() == cropId)
			{
				return cp;
			}
		}
		
		return null;
	}
	
	/**
	 * Calculates the total cost for a manor based on production and procurement.<br>
	 * This includes the price of all seeds and crops for the specified period.
	 * @param castleId The unique identifier of the castle.
	 * @param nextPeriod Whether to calculate costs for the upcoming period or the current one.
	 * @return The total calculated cost as a {@code long}.
	 */
	public long getManorCost(int castleId, boolean nextPeriod)
	{
		final List<CropProcure> procureList = getCropProcure(castleId, nextPeriod);
		final List<SeedProduction> productionList = getSeedProduction(castleId, nextPeriod);
		long totalCost = 0;
		
		for (SeedProduction seed : productionList)
		{
			Seed s = getSeed(seed.getId());
			totalCost += (s != null) ? (s.getSeedReferencePrice() * seed.getStartAmount()) : 1;
		}
		
		for (CropProcure crop : procureList)
		{
			totalCost += crop.getPrice() * crop.getStartAmount();
		}
		
		return totalCost;
	}
	
	/**
	 * Saves the current and next period manor production and procure data to the database.<br>
	 * This method clears existing records before inserting new batch data.
	 * @return {@code true} if the data was saved successfully, or {@code false} if an error occurred.
	 */
	public boolean storeMe()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement deleteProductionStmt = con.prepareStatement("DELETE FROM castle_manor_production");
			PreparedStatement insertProductionStmt = con.prepareStatement(INSERT_PRODUCT);
			PreparedStatement deleteProcureStmt = con.prepareStatement("DELETE FROM castle_manor_procure");
			PreparedStatement insertProcureStmt = con.prepareStatement(INSERT_CROP))
		{
			// Delete old production data.
			deleteProductionStmt.executeUpdate();
			
			// Insert current production data.
			for (Entry<Integer, List<SeedProduction>> entry : _production.entrySet())
			{
				final int castleIdKey = entry.getKey();
				for (SeedProduction sp : entry.getValue())
				{
					insertProductionStmt.setInt(1, castleIdKey);
					insertProductionStmt.setInt(2, sp.getId());
					insertProductionStmt.setLong(3, sp.getAmount());
					insertProductionStmt.setLong(4, sp.getStartAmount());
					insertProductionStmt.setLong(5, sp.getPrice());
					insertProductionStmt.setBoolean(6, false);
					insertProductionStmt.addBatch();
				}
			}
			
			// Insert next period production data.
			for (Entry<Integer, List<SeedProduction>> entry : _productionNext.entrySet())
			{
				final int castleIdKey = entry.getKey();
				for (SeedProduction sp : entry.getValue())
				{
					insertProductionStmt.setInt(1, castleIdKey);
					insertProductionStmt.setInt(2, sp.getId());
					insertProductionStmt.setLong(3, sp.getAmount());
					insertProductionStmt.setLong(4, sp.getStartAmount());
					insertProductionStmt.setLong(5, sp.getPrice());
					insertProductionStmt.setBoolean(6, true);
					insertProductionStmt.addBatch();
				}
			}
			
			// Execute batch insert for production data.
			insertProductionStmt.executeBatch();
			
			// Delete old procure data.
			deleteProcureStmt.executeUpdate();
			
			// Insert current procure data.
			for (Entry<Integer, List<CropProcure>> entry : _procure.entrySet())
			{
				final int castleIdKey = entry.getKey();
				for (CropProcure cp : entry.getValue())
				{
					insertProcureStmt.setInt(1, castleIdKey);
					insertProcureStmt.setInt(2, cp.getId());
					insertProcureStmt.setLong(3, cp.getAmount());
					insertProcureStmt.setLong(4, cp.getStartAmount());
					insertProcureStmt.setLong(5, cp.getPrice());
					insertProcureStmt.setInt(6, cp.getReward());
					insertProcureStmt.setBoolean(7, false);
					insertProcureStmt.addBatch();
				}
			}
			
			// Insert next period procure data.
			for (Entry<Integer, List<CropProcure>> entry : _procureNext.entrySet())
			{
				final int castleIdKey = entry.getKey();
				for (CropProcure cp : entry.getValue())
				{
					insertProcureStmt.setInt(1, castleIdKey);
					insertProcureStmt.setInt(2, cp.getId());
					insertProcureStmt.setLong(3, cp.getAmount());
					insertProcureStmt.setLong(4, cp.getStartAmount());
					insertProcureStmt.setLong(5, cp.getPrice());
					insertProcureStmt.setInt(6, cp.getReward());
					insertProcureStmt.setBoolean(7, true);
					insertProcureStmt.addBatch();
				}
			}
			
			// Execute batch insert for procure data.
			insertProcureStmt.executeBatch();
			
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Unable to store manor data!", e);
			return false;
		}
	}
	
	/**
	 * Resets the production and procurement data for a specific castle.<br>
	 * This method clears all internal lists associated with the given {@code castleId}.<br>
	 * It also deletes the corresponding records from the database if configured in {@code GeneralConfig}.
	 * @param castleId The unique identifier of the castle to reset.
	 */
	public void resetManorData(int castleId)
	{
		if (!GeneralConfig.ALLOW_MANOR)
		{
			return;
		}
		
		// Clear existing production and procurement data for the specified castle.
		_procure.get(castleId).clear();
		_procureNext.get(castleId).clear();
		_production.get(castleId).clear();
		_productionNext.get(castleId).clear();
		
		// Save changes to the database if configured to do so.
		if (GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement deleteProductionStmt = con.prepareStatement("DELETE FROM castle_manor_production WHERE castle_id = ?");
				PreparedStatement deleteProcureStmt = con.prepareStatement("DELETE FROM castle_manor_procure WHERE castle_id = ?"))
			{
				// Delete seed production data.
				deleteProductionStmt.setInt(1, castleId);
				deleteProductionStmt.executeUpdate();
				
				// Delete procurement data.
				deleteProcureStmt.setInt(1, castleId);
				deleteProcureStmt.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Unable to store manor data!", e);
			}
		}
	}
	
	/**
	 * Checks if the manor system is currently in maintenance mode.
	 * @return {@code true} if the current mode is {@code ManorMode.MAINTENANCE}, {@code false} otherwise.
	 */
	public boolean isUnderMaintenance()
	{
		return _mode == ManorMode.MAINTENANCE;
	}
	
	/**
	 * Checks if the manor mode is currently set to approved.
	 * @return {@code true} if the mode is {@code ManorMode.APPROVED}, otherwise {@code false}.
	 */
	public boolean isManorApproved()
	{
		return _mode == ManorMode.APPROVED;
	}
	
	/**
	 * Checks if the current manor mode allows for modifications.<br>
	 * Returns {@code true} if the mode is set to {@code ManorMode.MODIFIABLE}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the period can be modified, {@code false} otherwise.
	 */
	public boolean isModifiablePeriod()
	{
		return _mode == ManorMode.MODIFIABLE;
	}
	
	/**
	 * Retrieves the name of the current manor mode.<br>
	 * This method returns the string representation of the internal {@code _mode} variable.
	 * @return The name of the current mode as a {@code String}.
	 */
	public String getCurrentModeName()
	{
		return _mode.toString();
	}
	
	/**
	 * Retrieves the date and time for the next scheduled mode change.<br>
	 * If no change is scheduled, it returns a default value.
	 * @return A formatted string representing the date or {@code Disabled}.
	 */
	public String getNextModeChange()
	{
		if (_nextModeChange == null)
		{
			return "Disabled";
		}
		
		return DATE_FORMAT.format(Instant.ofEpochMilli(_nextModeChange.getTimeInMillis()).atZone(ZoneId.systemDefault()));
	}
	
	/**
	 * Retrieves a list of unique {@code Seed} objects.<br>
	 * This method filters the internal collection to ensure no duplicate crop IDs are included.
	 * @return A {@code List} containing unique {@code Seed} instances.
	 */
	public List<Seed> getCrops()
	{
		final List<Seed> seeds = new ArrayList<>();
		final List<Integer> cropIds = new ArrayList<>();
		for (Seed seed : _seeds.values())
		{
			final Integer cropId = seed.getCropId();
			if (!cropIds.contains(cropId))
			{
				seeds.add(seed);
				cropIds.add(cropId);
			}
		}
		
		cropIds.clear();
		return seeds;
	}
	
	/**
	 * Retrieves all {@code Seed} objects associated with a specific castle.<br>
	 * This method filters the internal seed collection by the provided ID.
	 * @param castleId The unique identifier of the castle to search for.
	 * @return A {@code Set} containing the seeds found for the given castle.
	 */
	public Set<Seed> getSeedsForCastle(int castleId)
	{
		Set<Seed> result = new HashSet<>();
		for (Seed seed : _seeds.values())
		{
			if (seed.getCastleId() == castleId)
			{
				result.add(seed);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves all unique seed identifiers.<br>
	 * This method returns the set of keys from the internal seeds map.
	 * @return a {@code Set<Integer>} containing all seed IDs.
	 */
	public Set<Integer> getSeedIds()
	{
		return _seeds.keySet();
	}
	
	/**
	 * Retrieves all unique crop identifiers from the available seeds.<br>
	 * This method iterates through the internal seed collection to gather IDs.
	 * @return A {@code Set<Integer>} containing all crop IDs.
	 */
	public Set<Integer> getCropIds()
	{
		final Set<Integer> result = new HashSet<>();
		for (Seed seed : _seeds.values())
		{
			result.add(seed.getCropId());
		}
		
		return result;
	}
	
	/**
	 * Retrieves a {@code Seed} object based on the provided unique identifier.
	 * @param seedId The unique ID of the seed to retrieve.
	 * @return The {@code Seed} associated with the given {@code seedId}.
	 */
	public Seed getSeed(int seedId)
	{
		return _seeds.get(seedId);
	}
	
	/**
	 * Retrieves a specific {@code Seed} based on the provided crop and castle identifiers.<br>
	 * This method searches through all seeds associated with a castle to find a match.
	 * @param cropId The unique identifier for the crop.
	 * @param castleId The unique identifier for the castle.
	 * @return The matching {@code Seed} object, or {@code null} if no match is found.
	 */
	public Seed getSeedByCrop(int cropId, int castleId)
	{
		for (Seed seed : getSeedsForCastle(castleId))
		{
			if (seed.getCropId() == cropId)
			{
				return seed;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a {@code Seed} object based on the provided crop identifier.<br>
	 * It searches through all registered seeds to find a match.
	 * @param cropId The unique identifier for the crop.
	 * @return The matching {@code Seed} object, or {@code null} if no match is found.
	 */
	public Seed getSeedByCrop(int cropId)
	{
		for (Seed seed : _seeds.values())
		{
			if (seed.getCropId() == cropId)
			{
				return seed;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the singleton instance of the {@code CastleManorManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The active {@code CastleManorManager} instance.
	 */
	public static CastleManorManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CastleManorManager INSTANCE = new CastleManorManager();
	}
}
