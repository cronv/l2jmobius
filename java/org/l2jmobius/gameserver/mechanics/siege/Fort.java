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
package org.l2jmobius.gameserver.mechanics.siege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.StaticObjectData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.residences.AbstractResidence;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.type.FortZone;
import org.l2jmobius.gameserver.entity.zone.type.SiegeZone;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a fort structure within the game world.<br>
 * This class handles the logic for fort ownership, residence properties, and siege mechanics.<br>
 * It extends {@code AbstractResidence} to provide base functionality for player-owned territories.
 */
public class Fort extends AbstractResidence
{
	protected static final Logger LOGGER = Logger.getLogger(Fort.class.getName());
	
	private final List<Door> _doors = new ArrayList<>();
	private StaticObject _flagPole = null;
	private FortSiege _siege = null;
	private Calendar _siegeDate;
	private Calendar _lastOwnedTime;
	private SiegeZone _zone;
	private Clan _fortOwner = null;
	private int _fortType = 0;
	private int _state = 0;
	private int _castleId = 0;
	private int _supplyLeveL = 0;
	private final Map<Integer, FortFunction> _function = new ConcurrentHashMap<>();
	private final ScheduledFuture<?>[] _fortUpdater = new ScheduledFuture<?>[2];
	
	// Spawn Data
	private boolean _isSuspiciousMerchantSpawned = false;
	private final Set<Spawn> _siegeNpcs = ConcurrentHashMap.newKeySet();
	private final Set<Spawn> _npcCommanders = ConcurrentHashMap.newKeySet();
	private final Set<Spawn> _specialEnvoys = ConcurrentHashMap.newKeySet();
	
	private final Map<Integer, Integer> _envoyCastles = new HashMap<>(2);
	private final Set<Integer> _availableCastles = new HashSet<>(1);
	
	/** Fortress Functions */
	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_RESTORE_HP = 2;
	public static final int FUNC_RESTORE_MP = 3;
	public static final int FUNC_RESTORE_EXP = 4;
	public static final int FUNC_SUPPORT = 5;
	
	public class FortFunction
	{
		final int _type;
		private int _level;
		protected int _fee;
		protected int _tempFee;
		final long _rate;
		long _endDate;
		protected boolean _inDebt;
		public boolean _cwh;
		
		public FortFunction(int type, int level, int lease, int tempLease, long rate, long time, boolean cwh)
		{
			_type = type;
			_level = level;
			_fee = lease;
			_tempFee = tempLease;
			_rate = rate;
			_endDate = time;
			initializeTask(cwh);
		}
		
		public int getType()
		{
			return _type;
		}
		
		public int getLevel()
		{
			return _level;
		}
		
		public int getLease()
		{
			return _fee;
		}
		
		public long getRate()
		{
			return _rate;
		}
		
		public long getEndTime()
		{
			return _endDate;
		}
		
		public void setLevel(int level)
		{
			_level = level;
		}
		
		public void setLease(int lease)
		{
			_fee = lease;
		}
		
		public void setEndTime(long time)
		{
			_endDate = time;
		}
		
		private void initializeTask(boolean cwh)
		{
			if (_fortOwner == null)
			{
				return;
			}
			
			final long currentTime = System.currentTimeMillis();
			if (_endDate > currentTime)
			{
				ThreadPool.schedule(new FunctionTask(cwh), _endDate - currentTime);
			}
			else
			{
				ThreadPool.schedule(new FunctionTask(cwh), 0);
			}
		}
		
		private class FunctionTask implements Runnable
		{
			public FunctionTask(boolean cwh)
			{
				_cwh = cwh;
			}
			
			@Override
			public void run()
			{
				try
				{
					if (_fortOwner == null)
					{
						return;
					}
					
					if ((_fortOwner.getWarehouse().getAdena() >= _fee) || !_cwh)
					{
						final int fee = _endDate == -1 ? _tempFee : _fee;
						setEndTime(System.currentTimeMillis() + _rate);
						dbSave();
						if (_cwh)
						{
							_fortOwner.getWarehouse().destroyItemByItemId(ItemProcessType.FEE, Inventory.ADENA_ID, fee, null, null);
						}
						
						ThreadPool.schedule(new FunctionTask(true), _rate);
					}
					else
					{
						removeFunction(_type);
					}
				}
				catch (Throwable t)
				{
					// Ignore.
				}
			}
		}
		
		public void dbSave()
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("REPLACE INTO fort_functions (fort_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)"))
			{
				ps.setInt(1, getResidenceId());
				ps.setInt(2, _type);
				ps.setInt(3, _level);
				ps.setInt(4, _fee);
				ps.setLong(5, _rate);
				ps.setLong(6, _endDate);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Exception: Fort.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Creates a new {@code Fort} instance using the provided unique identifier.<br>
	 * This constructor initializes all necessary zones, NPCs, and siege functions.<br>
	 * It also handles the loading of fort-specific data from the database.
	 * @param fortId The unique ID of the fort to be loaded.
	 */
	public Fort(int fortId)
	{
		super(fortId);
		load();
		loadFlagPoles();
		if (_fortOwner != null)
		{
			setVisibleFlag(true);
			loadFunctions();
		}
		
		initResidenceZone();
		
		// initFunctions();
		initNpcs(); // load and spawn npcs (Always spawned)
		initSiegeNpcs(); // load suspicious merchants (Despawned 10mins before siege)
		
		// spawnSuspiciousMerchant(); // spawn suspicious merchants
		initNpcCommanders(); // npc Commanders (not monsters) (Spawned during siege)
		spawnNpcCommanders(); // spawn npc Commanders
		initSpecialEnvoys(); // envoys from castles (Spawned after fort taken)
		if ((_fortOwner != null) && (_state == 0))
		{
			spawnSpecialEnvoys();
		}
	}
	
	/**
	 * Retrieves a specific fort function based on its type.
	 * @param type The unique identifier for the fort function.
	 * @return The {@code FortFunction} associated with the given {@code type}.
	 */
	public FortFunction getFortFunction(int type)
	{
		return _function.get(type);
	}
	
	/**
	 * Ends the siege for a specific {@code Clan}.<br>
	 * This method executes the cleanup process in a separate thread.
	 * @param clan The {@code Clan} that is finishing its siege.
	 */
	public void endOfSiege(Clan clan)
	{
		ThreadPool.execute(new endFortressSiege(this, clan));
	}
	
	/**
	 * Removes all players from the residence zone who are not part of the owner clan.<br>
	 * This method ensures that only authorized members can remain in the private area.<br>
	 * It uses the {@code _fortOwner} ID to identify valid members.
	 */
	public void banishForeigners()
	{
		getResidenceZone().banishForeigners(_fortOwner.getId());
	}
	
	/**
	 * Checks if the given coordinates are inside the siege zone.<br>
	 * This method retrieves the current {@code SiegeZone}.<br>
	 * It returns {@code true} if the point is within the boundaries.
	 * @param x The x coordinate to check.
	 * @param y The y coordinate to check.
	 * @param z The z coordinate to check.
	 * @return {@code true} if the coordinates are inside the zone, otherwise {@code false}.
	 */
	public boolean checkIfInZone(int x, int y, int z)
	{
		final SiegeZone zone = getZone();
		return (zone != null) && zone.isInsideZone(x, y, z);
	}
	
	/**
	 * Retrieves the {@code SiegeZone} associated with this fort.<br>
	 * It searches for a zone that matches the current residence ID.<br>
	 * If the zone is not cached, it fetches it from the {@code ZoneManager}.
	 * @return The {@code SiegeZone} object or {@code null} if no matching zone exists.
	 */
	public SiegeZone getZone()
	{
		if (_zone == null)
		{
			for (SiegeZone zone : ZoneManager.getInstance().getAllZones(SiegeZone.class))
			{
				if (zone.getSiegeObjectId() == getResidenceId())
				{
					_zone = zone;
					break;
				}
			}
		}
		
		return _zone;
	}
	
	/**
	 * Retrieves the residence zone for this fort.<br>
	 * This method returns a {@code FortZone} object.
	 * @return the {@code FortZone} associated with the residence area
	 */
	@Override
	public FortZone getResidenceZone()
	{
		return (FortZone) super.getResidenceZone();
	}
	
	/**
	 * Calculates the distance between this zone and a specific object.<br>
	 * It uses the {@code SiegeZone} to determine the value.
	 * @param obj The {@code WorldObject} to measure the distance to.
	 * @return The distance as a {@code double}.
	 */
	public double getDistance(WorldObject obj)
	{
		return getZone().getDistanceToZone(obj);
	}
	
	/**
	 * Closes a specific door for a player.<br>
	 * This method sets the state of the door to closed.
	 * @param player The {@code Player} who is interacting with the door.
	 * @param doorId The unique identifier of the door to be closed.
	 */
	public void closeDoor(Player player, int doorId)
	{
		openCloseDoor(player, doorId, false);
	}
	
	/**
	 * Opens a specific door for a player.<br>
	 * This method sets the door state to {@code true}.
	 * @param player The {@code Player} who is interacting with the door.
	 * @param doorId The unique identifier of the door to open.
	 */
	public void openDoor(Player player, int doorId)
	{
		openCloseDoor(player, doorId, true);
	}
	
	/**
	 * Changes the state of a specific door based on the provided boolean.<br>
	 * This method only works if the {@code Player} belongs to the fort owner.<br>
	 * It calls {@code openMe()} or {@code closeMe()} on the target {@code Door}.
	 * @param player The {@code Player} attempting to interact with the door.
	 * @param doorId The unique identifier of the door to modify.
	 * @param open Set to {@code true} to open the door, or {@code false} to close it.
	 */
	public void openCloseDoor(Player player, int doorId, boolean open)
	{
		if (player.getClan() != _fortOwner)
		{
			return;
		}
		
		final Door door = getDoor(doorId);
		if (door != null)
		{
			if (open)
			{
				door.openMe();
			}
			else
			{
				door.closeMe();
			}
		}
	}
	
	// This method is used to begin removing all fort upgrades.
	/**
	 * Removes the upgrades from all doors in the fort.<br>
	 * This method calls {@code removeDoorUpgrade()}.
	 */
	public void removeUpgrade()
	{
		removeDoorUpgrade();
	}
	
	/**
	 * Sets the owner of the fortress to a specific {@code Clan}.<br>
	 * This method handles reputation updates, siege state changes, and skill distribution.<br>
	 * It returns {@code false} if the provided clan is {@code null} or already owns a castle.
	 * @param clan The {@code Clan} that will become the new owner.
	 * @param updateClansReputation Whether to update the reputation of the clans involved.
	 * @return {@code true} if the ownership was successfully updated, {@code false} otherwise.
	 */
	public boolean setOwner(Clan clan, boolean updateClansReputation)
	{
		if (clan == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Updating Fort owner with null clan!!!");
			return false;
		}
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_OF_S1_HAS_FINISHED);
		sm.addCastleId(getResidenceId());
		final FortSiege siege = getSiege();
		siege.announceToPlayer(sm);
		
		final Clan oldowner = _fortOwner;
		if ((oldowner != null) && (clan != oldowner))
		{
			// Remove points from old owner.
			updateClansReputation(oldowner, true);
			try
			{
				final Player oldleader = oldowner.getLeader().getPlayer();
				if ((oldleader != null) && (oldleader.getMountType() == MountType.WYVERN))
				{
					oldleader.dismount();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception in setOwner: " + e.getMessage(), e);
			}
			
			if (siege.isInProgress())
			{
				siege.updatePlayerSiegeStateFlags(true);
			}
			
			removeOwner(true);
		}
		
		setFortState(0, 0); // initialize fort state
		
		// if clan already have castle, don't store him in fortress
		if (clan.getCastleId() > 0)
		{
			siege.announceToPlayer(new SystemMessage(SystemMessageId.THE_REBEL_ARMY_RECAPTURED_THE_FORTRESS));
			
			// The siege is over, the fortress went back to the rebels instead of to the castle owner.
			if (siege.isInProgress())
			{
				siege.endSiege();
			}
			
			return false;
		}
		
		// Give points to new owner.
		if (updateClansReputation)
		{
			updateClansReputation(clan, false);
		}
		
		spawnSpecialEnvoys();
		
		// If clan have already fortress, remove it.
		if (clan.getFortId() > 0)
		{
			FortManager.getInstance().getFortByOwner(clan).removeOwner(true);
		}
		
		setSupplyLeveL(0);
		setOwnerClan(clan);
		updateOwnerInDB(); // Update in database
		saveFortVariables();
		
		if (siege.isInProgress())
		{
			siege.endSiege();
		}
		
		for (Player member : clan.getOnlineMembers(0))
		{
			giveResidentialSkills(member);
			member.sendSkillList();
		}
		
		return true;
	}
	
	/**
	 * Removes the current owner of the fort.<br>
	 * This method clears ownership and resets associated skills and functions.<br>
	 * It also updates the supply level to {@code 0}.
	 * @param updateDB Set to {@code true} to persist the change in the database.
	 */
	public void removeOwner(boolean updateDB)
	{
		final Clan clan = _fortOwner;
		if (clan != null)
		{
			for (Player member : clan.getOnlineMembers(0))
			{
				removeResidentialSkills(member);
				member.sendSkillList();
			}
			
			clan.setFortId(0);
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
			setOwnerClan(null);
			setSupplyLeveL(0);
			saveFortVariables();
			removeAllFunctions();
			if (updateDB)
			{
				updateOwnerInDB();
			}
		}
	}
	
	/**
	 * Increases the current supply level of the fort.<br>
	 * This method ensures the level does not exceed {@code FeatureConfig.FS_MAX_SUPPLY_LEVEL}.
	 */
	public void raiseSupplyLeveL()
	{
		_supplyLeveL++;
		if (_supplyLeveL > FeatureConfig.FS_MAX_SUPPLY_LEVEL)
		{
			_supplyLeveL = FeatureConfig.FS_MAX_SUPPLY_LEVEL;
		}
	}
	
	/**
	 * Sets the current supply level of the fort.<br>
	 * The new value must be less than or equal to {@code FeatureConfig.FS_MAX_SUPPLY_LEVEL}.
	 * @param value The new supply level to set.
	 */
	public void setSupplyLeveL(int value)
	{
		if (value <= FeatureConfig.FS_MAX_SUPPLY_LEVEL)
		{
			_supplyLeveL = value;
		}
	}
	
	/**
	 * Retrieves the current supply level of the fort.
	 * @return The current {@code int} value of the supply level.
	 */
	public int getSupplyLeveL()
	{
		return _supplyLeveL;
	}
	
	/**
	 * Saves the current fort variables to the database.<br>
	 * This method updates the {@code supplyLvL} for the specific residence.<br>
	 * It handles any database exceptions by logging a warning.
	 */
	public void saveFortVariables()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE fort SET supplyLvL=? WHERE id = ?"))
		{
			ps.setInt(1, _supplyLeveL);
			ps.setInt(2, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: saveFortVariables(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Updates the visibility of the fort's flag pole.<br>
	 * This method changes the mesh index based on the provided {@code boolean}.<br>
	 * If {@code value} is {@code true}, it uses mesh index 1.<br>
	 * If {@code value} is {@code false}, it uses mesh index 0.
	 * @param value The visibility state to set for the flag pole.
	 */
	public void setVisibleFlag(boolean value)
	{
		final StaticObject flagPole = _flagPole;
		if (flagPole != null)
		{
			flagPole.setMeshIndex(value ? 1 : 0);
		}
	}
	
	/**
	 * Resets all doors in the fort to their default state.<br>
	 * This method closes open doors and revives dead ones.<br>
	 * It also restores the health of each door to its maximum value.<br>
	 * Finally, it reloads any existing door upgrades.
	 */
	public void resetDoors()
	{
		for (Door door : _doors)
		{
			if (door.isOpen())
			{
				door.closeMe();
			}
			
			if (door.isDead())
			{
				door.doRevive();
			}
			
			final long maxHp = door.getMaxHp();
			if (door.getCurrentHp() < maxHp)
			{
				door.setCurrentHp(maxHp);
			}
		}
		
		loadDoorUpgrade(); // Check for any upgrade the doors may have.
	}
	
	// This method upgrade door.
	/**
	 * Updates the statistics for a specific door.<br>
	 * This method increases the current health of the door and saves new defense values.
	 * @param doorId The unique identifier of the door to upgrade.
	 * @param hp The amount of additional health to add to the door.
	 * @param pDef The new physical defense value for the door.
	 * @param mDef The new magical defense value for the door.
	 */
	public void upgradeDoor(int doorId, int hp, int pDef, int mDef)
	{
		final Door door = getDoor(doorId);
		if (door != null)
		{
			door.setCurrentHp(door.getMaxHp() + hp);
			saveDoorUpgrade(doorId, hp, pDef, mDef);
		}
	}
	
	// This method loads fort.
	/**
	 * Loads the fort data from the database.<br>
	 * This method retrieves information such as name, owner, and state.<br>
	 * It also initializes the owner clan and schedules necessary update tasks.
	 */
	@Override
	protected void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM fort WHERE id = ?"))
		{
			final int residenceId = getResidenceId();
			ps.setInt(1, residenceId);
			int ownerId = 0;
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					setName(rs.getString("name"));
					
					_siegeDate = Calendar.getInstance();
					_lastOwnedTime = Calendar.getInstance();
					_siegeDate.setTimeInMillis(rs.getLong("siegeDate"));
					_lastOwnedTime.setTimeInMillis(rs.getLong("lastOwnedTime"));
					ownerId = rs.getInt("owner");
					_fortType = rs.getInt("fortType");
					_state = rs.getInt("state");
					_castleId = rs.getInt("castleId");
					_supplyLeveL = rs.getInt("supplyLvL");
				}
			}
			
			if (ownerId > 0)
			{
				final Clan clan = ClanTable.getInstance().getClan(ownerId); // Try to find clan instance.
				clan.setFortId(residenceId);
				setOwnerClan(clan);
				final int ownedTime = getOwnedTime();
				final int runCount = ownedTime / (FeatureConfig.FS_UPDATE_FRQ * 60);
				long initial = System.currentTimeMillis() - _lastOwnedTime.getTimeInMillis();
				while (initial > (FeatureConfig.FS_UPDATE_FRQ * 60000))
				{
					initial -= FeatureConfig.FS_UPDATE_FRQ * 60000;
				}
				
				initial = (FeatureConfig.FS_UPDATE_FRQ * 60000) - initial;
				if ((FeatureConfig.FS_MAX_OWN_TIME <= 0) || (ownedTime < (FeatureConfig.FS_MAX_OWN_TIME * 3600)))
				{
					_fortUpdater[0] = ThreadPool.scheduleAtFixedRate(new FortUpdater(this, clan, runCount, FortUpdaterType.PERIODIC_UPDATE), initial, FeatureConfig.FS_UPDATE_FRQ * 60000); // Schedule owner tasks to start running.
					if (FeatureConfig.FS_MAX_OWN_TIME > 0)
					{
						_fortUpdater[1] = ThreadPool.scheduleAtFixedRate(new FortUpdater(this, clan, runCount, FortUpdaterType.MAX_OWN_TIME), 3600000, 3600000); // Schedule owner tasks to remove owener.
					}
				}
				else
				{
					_fortUpdater[1] = ThreadPool.schedule(new FortUpdater(this, clan, 0, FortUpdaterType.MAX_OWN_TIME), 60000); // Schedule owner tasks to remove owner.
				}
			}
			else
			{
				setOwnerClan(null);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadFortData(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Loads the fort functions from the database.<br>
	 * It retrieves all entries for the current residence ID.<br>
	 * The results are stored in the {@code _function} map.
	 */
	private void loadFunctions()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM fort_functions WHERE fort_id = ?"))
		{
			ps.setInt(1, getResidenceId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					_function.put(rs.getInt("type"), new FortFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime"), true));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception: Fort.loadFunctions(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes a specific function from the fort.<br>
	 * This method deletes the function from the internal list and the database.
	 * @param functionType The unique identifier of the function to remove.
	 */
	public void removeFunction(int functionType)
	{
		_function.remove(functionType);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM fort_functions WHERE fort_id=? AND type=?"))
		{
			ps.setInt(1, getResidenceId());
			ps.setInt(2, functionType);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception: Fort.removeFunctions(int functionType): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Clears all registered functions from the fort.<br>
	 * This method iterates through every key in the {@code _function} map.<br>
	 * It calls {@code removeFunction} for each individual ID.
	 */
	private void removeAllFunctions()
	{
		for (int id : _function.keySet())
		{
			removeFunction(id);
		}
	}
	
	/**
	 * Updates or adds a fort function for a specific player.<br>
	 * This method handles the logic for creating, modifying, or removing functions based on provided parameters.
	 * @param player The {@code Player} who is performing the action.
	 * @param type The unique identifier for the function type.
	 * @param level The new level to set for the function.
	 * @param lease The amount of currency required for the lease.
	 * @param rate The multiplier or rate associated with the function.
	 * @param addNew A boolean flag; if {@code true}, a new function is created.
	 * @return {@code true} if the operation was successful, {@code false} otherwise.
	 */
	public boolean updateFunctions(Player player, int type, int level, int lease, long rate, boolean addNew)
	{
		if (player == null)
		{
			return false;
		}
		
		if ((lease > 0) && !player.destroyItemByItemId(null, Inventory.ADENA_ID, lease, null, true))
		{
			return false;
		}
		
		if (addNew)
		{
			_function.put(type, new FortFunction(type, level, lease, 0, rate, 0, false));
		}
		else if ((level == 0) && (lease == 0))
		{
			removeFunction(type);
		}
		else if ((lease - _function.get(type).getLease()) > 0)
		{
			_function.remove(type);
			_function.put(type, new FortFunction(type, level, lease, 0, rate, -1, false));
		}
		else
		{
			_function.get(type).setLease(lease);
			_function.get(type).setLevel(level);
			_function.get(type).dbSave();
		}
		
		return true;
	}
	
	/**
	 * Activates the instance by loading all associated doors.<br>
	 * This method ensures that door data is populated from the database.
	 */
	public void activateInstance()
	{
		loadDoor();
	}
	
	// This method loads fort door data from database.
	/**
	 * Loads the doors associated with this residence.<br>
	 * It iterates through all {@code DoorData}.<br>
	 * Only doors belonging to the current residence are added to the internal list.
	 */
	private void loadDoor()
	{
		for (Door door : DoorData.getInstance().getDoors())
		{
			if ((door.getFort() != null) && (door.getFort().getResidenceId() == getResidenceId()))
			{
				_doors.add(door);
			}
		}
	}
	
	/**
	 * Loads the flag pole object for the current fort.<br>
	 * It searches through all {@code StaticObjectData} to find a matching object.<br>
	 * If no flag pole is found, it throws a {@code NullPointerException}.
	 */
	private void loadFlagPoles()
	{
		for (StaticObject obj : StaticObjectData.getInstance().getStaticObjects())
		{
			if ((obj.getType() == 3) && obj.getName().startsWith(getName()))
			{
				_flagPole = obj;
				break;
			}
		}
		
		if (_flagPole == null)
		{
			throw new NullPointerException("Can't find flagpole for Fort " + this);
		}
	}
	
	// This method loads fort door upgrade data from database.
	/**
	 * Loads door upgrades from the database for the current residence.<br>
	 * It fetches data from the {@code fort_doorupgrade} table.<br>
	 * Each record is used to call the {@code upgradeDoor} method.
	 */
	private void loadDoorUpgrade()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM fort_doorupgrade WHERE fortId = ?"))
		{
			ps.setInt(1, getResidenceId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					upgradeDoor(rs.getInt("id"), rs.getInt("hp"), rs.getInt("pDef"), rs.getInt("mDef"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadFortDoorUpgrade(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes the door upgrade data from the database.<br>
	 * This method deletes all entries associated with the current residence ID.<br>
	 * It handles potential SQL exceptions by logging a warning message.
	 */
	private void removeDoorUpgrade()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM fort_doorupgrade WHERE fortId = ?"))
		{
			ps.setInt(1, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: removeDoorUpgrade(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Saves the upgrade statistics for a specific door to the database.<br>
	 * This method inserts new records into the {@code fort_doorupgrade} table.
	 * @param doorId The unique identifier of the door.
	 * @param hp The health points of the upgraded door.
	 * @param pDef The physical defense value of the upgraded door.
	 * @param mDef The magical defense value of the upgraded door.
	 */
	private void saveDoorUpgrade(int doorId, int hp, int pDef, int mDef)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO fort_doorupgrade (doorId, hp, pDef, mDef) VALUES (?,?,?,?)"))
		{
			ps.setInt(1, doorId);
			ps.setInt(2, hp);
			ps.setInt(3, pDef);
			ps.setInt(4, mDef);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: saveDoorUpgrade(int doorId, int hp, int pDef, int mDef): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Updates the fort owner information in the database.<br>
	 * This method synchronizes the current owner and last owned time with the SQL table.<br>
	 * It also handles victory announcements and schedules periodic tasks for the new owner.
	 */
	private void updateOwnerInDB()
	{
		final Clan clan = _fortOwner;
		int clanId = 0;
		if (clan != null)
		{
			clanId = clan.getId();
			_lastOwnedTime.setTimeInMillis(System.currentTimeMillis());
		}
		else
		{
			_lastOwnedTime.setTimeInMillis(0);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE fort SET owner=?,lastOwnedTime=?,state=?,castleId=? WHERE id = ?"))
		{
			ps.setInt(1, clanId);
			ps.setLong(2, _lastOwnedTime.getTimeInMillis());
			ps.setInt(3, 0);
			ps.setInt(4, 0);
			
			final int residenceId = getResidenceId();
			ps.setInt(5, residenceId);
			ps.execute();
			
			// Announce to clan members.
			if (clan != null)
			{
				clan.setFortId(residenceId); // Set has fort flag for new owner.
				SystemMessage sm;
				sm = new SystemMessage(SystemMessageId.S1_IS_VICTORIOUS_IN_THE_FORTRESS_BATTLE_OF_S2);
				sm.addString(clan.getName());
				sm.addCastleId(residenceId);
				World.getPlayers().forEach(p -> p.sendPacket(sm));
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				clan.broadcastToOnlineMembers(new PlaySound(1, "Siege_Victory", 0, 0, 0, 0, 0));
				if (_fortUpdater[0] != null)
				{
					_fortUpdater[0].cancel(false);
				}
				
				if (_fortUpdater[1] != null)
				{
					_fortUpdater[1].cancel(false);
				}
				
				_fortUpdater[0] = ThreadPool.scheduleAtFixedRate(new FortUpdater(this, clan, 0, FortUpdaterType.PERIODIC_UPDATE), FeatureConfig.FS_UPDATE_FRQ * 60000, FeatureConfig.FS_UPDATE_FRQ * 60000); // Schedule owner tasks to start running.
				if (FeatureConfig.FS_MAX_OWN_TIME > 0)
				{
					_fortUpdater[1] = ThreadPool.scheduleAtFixedRate(new FortUpdater(this, clan, 0, FortUpdaterType.MAX_OWN_TIME), 3600000, 3600000); // Schedule owner tasks to remove owner.
				}
			}
			else
			{
				if (_fortUpdater[0] != null)
				{
					_fortUpdater[0].cancel(false);
				}
				
				_fortUpdater[0] = null;
				if (_fortUpdater[1] != null)
				{
					_fortUpdater[1].cancel(false);
				}
				
				_fortUpdater[1] = null;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: updateOwnerInDB(Pledge clan): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the unique identifier of the current fort owner.<br>
	 * This method checks if a {@code Clan} is currently assigned to this fort.<br>
	 * If no owner exists, it returns -1.
	 * @return The {@code int} ID of the owning clan, or -1 if there is no owner.
	 */
	@Override
	public int getOwnerId()
	{
		final Clan clan = _fortOwner;
		return clan != null ? clan.getId() : -1;
	}
	
	/**
	 * Retrieves the {@code Clan} that currently owns this fort.
	 * @return the {@code Clan} object representing the owner, or {@code null} if no owner exists.
	 */
	public Clan getOwnerClan()
	{
		return _fortOwner;
	}
	
	/**
	 * Sets the owner of the fort.<br>
	 * This method updates the {@code _fortOwner} field.<br>
	 * It also updates the visibility flag based on whether the provided {@code Clan} is {@code null}.
	 * @param clan The {@code Clan} that will own the fort.
	 */
	public void setOwnerClan(Clan clan)
	{
		setVisibleFlag(clan != null);
		_fortOwner = clan;
	}
	
	/**
	 * Retrieves a {@code Door} object based on its unique identifier.<br>
	 * This method searches through the internal list of doors.<br>
	 * It returns {@code null} if the ID is invalid or not found.
	 * @param doorId The unique identification number of the door to find.
	 * @return The corresponding {@code Door} object, or {@code null} if no match exists.
	 */
	public Door getDoor(int doorId)
	{
		if (doorId <= 0)
		{
			return null;
		}
		
		for (Door door : _doors)
		{
			if (door.getId() == doorId)
			{
				return door;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the list of all doors associated with this fort.
	 * @return a {@code List} containing all {@code Door} objects.
	 */
	public List<Door> getDoors()
	{
		return _doors;
	}
	
	/**
	 * Retrieves the current flag pole object.
	 * @return the {@code StaticObject} representing the flag pole.
	 */
	public StaticObject getFlagPole()
	{
		return _flagPole;
	}
	
	/**
	 * Retrieves the {@code FortSiege} instance associated with this fort.<br>
	 * This method uses double-checked locking to ensure thread safety during initialization.<br>
	 * If the siege object does not exist, it creates a new one.
	 * @return The current {@code FortSiege} object.
	 */
	public FortSiege getSiege()
	{
		if (_siege == null)
		{
			synchronized (this)
			{
				if (_siege == null)
				{
					_siege = new FortSiege(this);
				}
			}
		}
		
		return _siege;
	}
	
	/**
	 * Retrieves the current date for the siege event.
	 * @return the {@code Calendar} object representing the siege date.
	 */
	public Calendar getSiegeDate()
	{
		return _siegeDate;
	}
	
	/**
	 * Sets the date for the siege event.<br>
	 * This method updates the {@code _siegeDate} field with the provided value.
	 * @param siegeDate The {@code Calendar} object representing the siege date.
	 */
	public void setSiegeDate(Calendar siegeDate)
	{
		_siegeDate = siegeDate;
	}
	
	/**
	 * Calculates the time elapsed since the fort was last owned.<br>
	 * It returns the duration in seconds.<br>
	 * If the fort has never been owned, it returns {@code 0}.
	 * @return The number of seconds since ownership changed.
	 */
	public int getOwnedTime()
	{
		return _lastOwnedTime.getTimeInMillis() == 0 ? 0 : (int) ((System.currentTimeMillis() - _lastOwnedTime.getTimeInMillis()) / 1000);
	}
	
	/**
	 * Calculates the remaining time until the rebel army takes over.<br>
	 * It checks the last ownership timestamp and adds the maximum ownership duration.<br>
	 * The result is returned in seconds.
	 * @return The number of seconds remaining until the next takeover.
	 */
	public int getTimeTillRebelArmy()
	{
		return _lastOwnedTime.getTimeInMillis() == 0 ? 0 : (int) (((_lastOwnedTime.getTimeInMillis() + (FeatureConfig.FS_MAX_OWN_TIME * 3600000)) - System.currentTimeMillis()) / 1000);
	}
	
	/**
	 * Calculates the time remaining until the next fort update.<br>
	 * It checks if a scheduled task exists in {@code _fortUpdater}.<br>
	 * If no task is found, it returns {@code 0}.
	 * @return The delay in seconds until the next update.
	 */
	public long getTimeTillNextFortUpdate()
	{
		return _fortUpdater[0] == null ? 0 : _fortUpdater[0].getDelay(TimeUnit.SECONDS);
	}
	
	/**
	 * Updates the reputation score of a specific clan.<br>
	 * This method modifies the points based on whether the fort was lost or taken.
	 * @param owner The {@code Clan} object that owns the fort.
	 * @param removePoints If {@code true}, subtracts points from the reputation. If {@code false}, adds points to the reputation.
	 */
	public void updateClansReputation(Clan owner, boolean removePoints)
	{
		if (owner != null)
		{
			if (removePoints)
			{
				owner.takeReputationScore(FeatureConfig.LOOSE_FORT_POINTS);
			}
			else
			{
				owner.addReputationScore(FeatureConfig.TAKE_FORT_POINTS);
			}
		}
	}
	
	private static class endFortressSiege implements Runnable
	{
		private final Fort _f;
		private final Clan _clan;
		
		public endFortressSiege(Fort f, Clan clan)
		{
			_f = f;
			_clan = clan;
		}
		
		@Override
		public void run()
		{
			try
			{
				_f.setOwner(_clan, true);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception in endFortressSiege " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Retrieves the current state of the fort.
	 * @return The integer value representing the current fort state.
	 */
	public int getFortState()
	{
		return _state;
	}
	
	/**
	 * Updates the current state and castle identifier for this fort.<br>
	 * This method saves the new values directly to the database.
	 * @param state The new status value for the fort.
	 * @param castleId The unique identifier of the associated castle.
	 */
	public void setFortState(int state, int castleId)
	{
		_state = state;
		_castleId = castleId;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE fort SET state=?,castleId=? WHERE id = ?"))
		{
			ps.setInt(1, _state);
			ps.setInt(2, _castleId);
			ps.setInt(3, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: setFortState(int state, int castleId): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the type of the fort.<br>
	 * This value identifies the specific category or configuration of the fort.
	 * @return The integer representing the fort type.
	 */
	public int getFortType()
	{
		return _fortType;
	}
	
	/**
	 * Retrieves the castle ID associated with a specific ambassador.<br>
	 * This method checks if the {@code npcId} exists in the envoy castles map.
	 * @param npcId The unique identifier of the ambassador NPC.
	 * @return The castle ID as an {@code int}, or -1 if no castle is found.
	 */
	public int getCastleIdByAmbassador(int npcId)
	{
		if ((_envoyCastles == null) || !_envoyCastles.containsKey(npcId))
		{
			return -1;
		}
		
		return _envoyCastles.get(npcId);
	}
	
	/**
	 * Retrieves a {@code Castle} object based on the provided ambassador NPC ID.<br>
	 * This method first finds the castle ID associated with the {@code npcId}.<br>
	 * It then fetches the corresponding {@code Castle} instance from the manager.
	 * @param npcId The unique identifier of the ambassador NPC.
	 * @return The {@code Castle} object associated with the ambassador, or {@code null} if not found.
	 */
	public Castle getCastleByAmbassador(int npcId)
	{
		return CastleManager.getInstance().getCastleById(getCastleIdByAmbassador(npcId));
	}
	
	/**
	 * Retrieves the unique identifier for the contracted castle.<br>
	 * This value is used to identify which specific castle this object belongs to.
	 * @return The {@code int} ID of the contracted castle.
	 */
	public int getContractedCastleId()
	{
		return _castleId;
	}
	
	/**
	 * Retrieves the {@code Castle} associated with this fort.<br>
	 * It uses the internal castle ID to fetch the data from the {@code CastleManager}.
	 * @return The {@code Castle} object corresponding to the contracted castle.
	 */
	public Castle getContractedCastle()
	{
		return CastleManager.getInstance().getCastleById(getContractedCastleId());
	}
	
	/**
	 * Checks if the current fort is a border fortress.<br>
	 * This method returns {@code true} if there is more than one available castle.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if multiple castles are available, {@code false} otherwise.
	 */
	public boolean isBorderFortress()
	{
		return _availableCastles.size() > 1;
	}
	
	/**
	 * Retrieves the size of the fort based on its type.<br>
	 * Returns {@code 3} if the fort type is {@code 0}.<br>
	 * Otherwise, it returns {@code 5}.
	 * @return The integer size of the fort.
	 */
	public int getFortSize()
	{
		return _fortType == 0 ? 3 : 5;
	}
	
	/**
	 * Spawns the suspicious merchant NPCs for the siege.<br>
	 * This method ensures that merchants are only spawned once.<br>
	 * It iterates through all registered {@code Spawn} objects in the siege and triggers their spawn logic.
	 */
	public void spawnSuspiciousMerchant()
	{
		if (_isSuspiciousMerchantSpawned)
		{
			return;
		}
		
		_isSuspiciousMerchantSpawned = true;
		for (Spawn spawnDat : _siegeNpcs)
		{
			spawnDat.doSpawn(false);
			spawnDat.startRespawn();
		}
	}
	
	/**
	 * Removes the suspicious merchant from the game world.<br>
	 * This method stops all respawns for siege NPCs.<br>
	 * It also deletes any currently active instances of these NPCs.
	 */
	public void despawnSuspiciousMerchant()
	{
		if (!_isSuspiciousMerchantSpawned)
		{
			return;
		}
		
		_isSuspiciousMerchantSpawned = false;
		for (Spawn spawnDat : _siegeNpcs)
		{
			spawnDat.stopRespawn();
			spawnDat.getLastSpawn().deleteMe();
		}
	}
	
	/**
	 * Spawns all NPC commanders defined in the configuration.<br>
	 * This method iterates through the {@code _npcCommanders} list.<br>
	 * It triggers the initial spawn and starts the respawn logic for each commander.
	 */
	public void spawnNpcCommanders()
	{
		for (Spawn spawnDat : _npcCommanders)
		{
			spawnDat.doSpawn(false);
			spawnDat.startRespawn();
		}
	}
	
	/**
	 * Removes all active NPC commanders from the world.<br>
	 * This method stops the respawn cycle for each commander.<br>
	 * It also deletes the current instance of every commander.
	 */
	public void despawnNpcCommanders()
	{
		for (Spawn spawnDat : _npcCommanders)
		{
			spawnDat.stopRespawn();
			spawnDat.getLastSpawn().deleteMe();
		}
	}
	
	/**
	 * This method handles the spawning of special envoys.<br>
	 * It iterates through all entries in the {@code _specialEnvoys} list.<br>
	 * Each envoy is spawned and its respawn timer is started.
	 */
	public void spawnSpecialEnvoys()
	{
		for (Spawn spawnDat : _specialEnvoys)
		{
			spawnDat.doSpawn(false);
			spawnDat.startRespawn();
		}
	}
	
	/**
	 * Initializes the NPCs for the residence zone.<br>
	 * This method fetches spawn data from the database based on the current fort ID.<br>
	 * It adds each spawn to the {@code SpawnTable} and starts their respawn cycle.
	 */
	private void initNpcs()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM fort_spawnlist WHERE fortId = ? AND spawnType = ?"))
		{
			ps.setInt(1, getResidenceId());
			ps.setInt(2, 0);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Spawn spawnDat = new Spawn(rs.getInt("npcId"));
					spawnDat.setAmount(1);
					spawnDat.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
					spawnDat.setHeading(rs.getInt("heading"));
					spawnDat.setRespawnDelay(60);
					SpawnTable.getInstance().addSpawn(spawnDat);
					spawnDat.doSpawn(false);
					spawnDat.startRespawn();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Fort " + getResidenceId() + " initNpcs: Spawn could not be initialized: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Initializes the siege NPCs for the residence.<br>
	 * This method clears existing data and loads new spawns from the database.<br>
	 * It fetches records from {@code fort_spawnlist} based on the current residence ID.<br>
	 * If an error occurs during loading, a warning is logged to the logger.
	 */
	private void initSiegeNpcs()
	{
		_siegeNpcs.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT id, npcId, x, y, z, heading FROM fort_spawnlist WHERE fortId = ? AND spawnType = ? ORDER BY id"))
		{
			ps.setInt(1, getResidenceId());
			ps.setInt(2, 2);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Spawn spawnDat = new Spawn(rs.getInt("npcId"));
					spawnDat.setAmount(1);
					spawnDat.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
					spawnDat.setHeading(rs.getInt("heading"));
					spawnDat.setRespawnDelay(60);
					_siegeNpcs.add(spawnDat);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Fort " + getResidenceId() + " initSiegeNpcs: Spawn could not be initialized: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Initializes the list of NPC commanders for the fort.<br>
	 * This method fetches spawn data from the database based on the residence ID.<br>
	 * It populates the {@code _npcCommanders} collection with new {@code Spawn} objects.
	 */
	private void initNpcCommanders()
	{
		_npcCommanders.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT id, npcId, x, y, z, heading FROM fort_spawnlist WHERE fortId = ? AND spawnType = ? ORDER BY id"))
		{
			ps.setInt(1, getResidenceId());
			ps.setInt(2, 1);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Spawn spawnDat = new Spawn(rs.getInt("npcId"));
					spawnDat.setAmount(1);
					spawnDat.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
					spawnDat.setHeading(rs.getInt("heading"));
					spawnDat.setRespawnDelay(60);
					_npcCommanders.add(spawnDat);
				}
			}
		}
		catch (Exception e)
		{
			// Problem with initializing spawn, go to next one.
			LOGGER.log(Level.WARNING, "Fort " + getResidenceId() + " initNpcCommanders: Spawn could not be initialized: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Initializes the special envoys for the current fort.<br>
	 * This method clears existing data and loads new spawn information from the database.<br>
	 * It populates the {@code _specialEnvoys}, {@code _envoyCastles}, and {@code _availableCastles} collections.
	 */
	private void initSpecialEnvoys()
	{
		_specialEnvoys.clear();
		_envoyCastles.clear();
		_availableCastles.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT id, npcId, x, y, z, heading, castleId FROM fort_spawnlist WHERE fortId = ? AND spawnType = ? ORDER BY id"))
		{
			ps.setInt(1, getResidenceId());
			ps.setInt(2, 3);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int castleId = rs.getInt("castleId");
					final int npcId = rs.getInt("npcId");
					final Spawn spawnDat = new Spawn(npcId);
					spawnDat.setAmount(1);
					spawnDat.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
					spawnDat.setHeading(rs.getInt("heading"));
					spawnDat.setRespawnDelay(60);
					_specialEnvoys.add(spawnDat);
					_envoyCastles.put(npcId, castleId);
					_availableCastles.add(castleId);
				}
			}
		}
		catch (Exception e)
		{
			// Problem with initializing spawn, go to next one.
			LOGGER.log(Level.WARNING, "Fort " + getResidenceId() + " initSpecialEnvoys: Spawn could not be initialized: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Initializes the residence zone for this fort.<br>
	 * It searches through all {@code FortZone} objects in the {@code ZoneManager}.<br>
	 * The method sets the current zone if the residence ID matches.
	 */
	@Override
	protected void initResidenceZone()
	{
		for (FortZone zone : ZoneManager.getInstance().getAllZones(FortZone.class))
		{
			if (zone.getResidenceId() == getResidenceId())
			{
				setResidenceZone(zone);
				break;
			}
		}
	}
}
