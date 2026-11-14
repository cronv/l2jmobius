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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.CastleData;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.instance.Artefact;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.residences.AbstractResidence;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.type.CastleZone;
import org.l2jmobius.gameserver.entity.zone.type.ResidenceTeleportZone;
import org.l2jmobius.gameserver.entity.zone.type.SiegeZone;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExCastleState;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a castle structure within the game world.<br>
 * This class handles the core logic for castle ownership, residence mechanics, and siege interactions.<br>
 * It serves as the primary data model for {@code CastleZone} areas.
 */
public class Castle extends AbstractResidence
{
	protected static final Logger LOGGER = Logger.getLogger(Castle.class.getName());
	
	private final List<Door> _doors = new ArrayList<>();
	private final List<Npc> _sideNpcs = new ArrayList<>();
	int _ownerId = 0;
	private Siege _siege = null;
	private Calendar _siegeDate;
	private boolean _isTimeRegistrationOver = true; // True if Castle Lords set the time, or 24h is elapsed after the siege.
	private Calendar _siegeTimeRegistrationEndDate; // Last siege end date + 1 day.
	private CastleSide _castleSide = null;
	private long _treasury = 0;
	private boolean _showNpcCrest = false;
	private SiegeZone _zone = null;
	private ResidenceTeleportZone _teleZone;
	private Clan _formerOwner = null;
	private final Set<Artefact> _artefacts = new HashSet<>(1);
	private final Map<Integer, Integer> _engravedArtefacts = new ConcurrentHashMap<>();
	private final Map<Integer, CastleFunction> _function = new ConcurrentHashMap<>();
	private int _ticketBuyCount = 0;
	private boolean _isFirstMidVictory = false;
	
	/** Castle Functions */
	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_RESTORE_HP = 2;
	public static final int FUNC_RESTORE_MP = 3;
	public static final int FUNC_RESTORE_EXP = 4;
	public static final int FUNC_SUPPORT = 5;
	
	public class CastleFunction
	{
		final int _type;
		private int _lvl;
		protected int _fee;
		protected int _tempFee;
		final long _rate;
		long _endDate;
		protected boolean _inDebt;
		public boolean _cwh;
		
		public CastleFunction(int type, int lvl, int lease, int tempLease, long rate, long time, boolean cwh)
		{
			_type = type;
			_lvl = lvl;
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
		
		public int getLvl()
		{
			return _lvl;
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
		
		public void setLvl(int lvl)
		{
			_lvl = lvl;
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
			if (_ownerId <= 0)
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
					if (_ownerId <= 0)
					{
						return;
					}
					
					if ((ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().getAdena() >= _fee) || !_cwh)
					{
						int fee = _fee;
						if (_endDate == -1)
						{
							fee = _tempFee;
						}
						
						setEndTime(System.currentTimeMillis() + _rate);
						dbSave();
						if (_cwh)
						{
							ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().destroyItemByItemId(ItemProcessType.FEE, Inventory.ADENA_ID, fee, null, null);
						}
						
						ThreadPool.schedule(new FunctionTask(true), _rate);
					}
					else
					{
						removeFunction(_type);
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, "", e);
				}
			}
		}
		
		public void dbSave()
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("REPLACE INTO castle_functions (castle_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)"))
			{
				ps.setInt(1, getResidenceId());
				ps.setInt(2, _type);
				ps.setInt(3, _lvl);
				ps.setInt(4, _fee);
				ps.setLong(5, _rate);
				ps.setLong(6, _endDate);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Exception: Castle.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Creates a new {@code Castle} instance using the provided unique identifier.<br>
	 * This constructor initializes the castle data and spawns necessary NPCs.<br>
	 * It also sets up residence zones and loads specific functions based on ownership.
	 * @param castleId The unique ID of the castle to load.
	 */
	public Castle(int castleId)
	{
		super(castleId);
		load();
		initResidenceZone();
		
		// initFunctions();
		spawnSideNpcs();
		if (_ownerId != 0)
		{
			loadFunctions();
			loadDoorUpgrade();
		}
	}
	
	/**
	 * Retrieves a specific function associated with the castle based on its type.
	 * @param type The unique identifier for the desired castle function.
	 * @return The {@code CastleFunction} corresponding to the provided {@code type}.
	 */
	public CastleFunction getCastleFunction(int type)
	{
		return _function.get(type);
	}
	
	/**
	 * Forgets the artifacts engraved during a previous siege.
	 */
	public void clearEngravedArtefacts()
	{
		_engravedArtefacts.clear();
	}
	
	/**
	 * Marks a specific object as belonging to a clan.<br>
	 * This method updates the owner and side of the target.<br>
	 * It also broadcasts a success message to all online players.
	 * @param clan The {@code Clan} that will own the target.
	 * @param target The {@code WorldObject} being engraved.
	 * @param side The {@code CastleSide} assigned to this action.
	 */
	public synchronized void engrave(Clan clan, WorldObject target, CastleSide side)
	{
		if (!_artefacts.contains(target))
		{
			return;
		}
		
		final SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_S1_HAS_SUCCEEDED_IN_S2);
		msg.addString(clan.getName());
		msg.addString(getName());
		
		// Every artifact of the castle has to carry the seal of the same clan before the castle changes hands.
		_engravedArtefacts.put(target.getObjectId(), clan.getId());
		for (Artefact artefact : _artefacts)
		{
			final Integer engraver = _engravedArtefacts.get(artefact.getObjectId());
			if ((engraver == null) || (engraver.intValue() != clan.getId()))
			{
				// The siege participants learn of each engraved artifact, the last one changes the owner.
				getSiege().announceToPlayer(msg, true);
				return;
			}
		}
		
		_engravedArtefacts.clear();
		setSide(side);
		setOwner(clan);
		World.broadcastToAllOnlinePlayers(msg);
	}
	
	// This method add to the treasury.
	/**
	 * Adds a specific amount of currency to the treasury.<br>
	 * This method calculates and applies taxes based on the castle name.<br>
	 * It handles special tax logic for castles like Schuttgart or Dion.<br>
	 * The final remaining amount is added using {@code addToTreasuryNoTax}.
	 * @param amountValue The total amount of currency to be processed.
	 */
	public void addToTreasury(long amountValue)
	{
		// check if owned
		if (_ownerId <= 0)
		{
			return;
		}
		
		long amount = amountValue;
		switch (getName().toLowerCase())
		{
			case "schuttgart":
			case "goddard":
			{
				final Castle rune = CastleManager.getInstance().getCastle("rune");
				if (rune != null)
				{
					final long runeTax = (long) (amount * rune.getTaxRate(CastleTaxType.BUY));
					if (rune.getOwnerId() > 0)
					{
						rune.addToTreasury(runeTax);
					}
					
					amount -= runeTax;
				}
				break;
			}
			case "dion":
			case "giran":
			case "gludio":
			case "innadril":
			case "oren":
			{
				final Castle aden = CastleManager.getInstance().getCastle("aden");
				if (aden != null)
				{
					final long adenTax = (long) (amount * aden.getTaxRate(CastleTaxType.BUY)); // Find out what Aden gets from the current castle instance's income.
					if (aden.getOwnerId() > 0)
					{
						aden.addToTreasury(adenTax); // Only bother to really add the tax to the treasury if not npc owned.
					}
					
					amount -= adenTax; // Subtract Aden's income from current castle instance's income.
				}
				break;
			}
		}
		
		addToTreasuryNoTax(amount);
	}
	
	/**
	 * Adds a specific amount of currency to the castle treasury without applying taxes.<br>
	 * This method handles both positive additions and negative deductions.<br>
	 * It ensures the total does not exceed {@code Inventory.MAX_ADENA}.<br>
	 * Returns {@code true} if the operation succeeded, or {@code false} otherwise.
	 * @param amountValue The amount of currency to add or subtract from the treasury.
	 * @return {@code true} if the update was successful; {@code false} if the castle has no owner or insufficient funds for deduction.
	 */
	public boolean addToTreasuryNoTax(long amountValue)
	{
		if (_ownerId <= 0)
		{
			return false;
		}
		
		long amount = amountValue;
		if (amount < 0)
		{
			amount *= -1;
			if (_treasury < amount)
			{
				return false;
			}
			
			_treasury -= amount;
		}
		else if ((_treasury + amount) > Inventory.MAX_ADENA)
		{
			_treasury = Inventory.MAX_ADENA;
		}
		else
		{
			_treasury += amount;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle SET treasury = ? WHERE id = ?"))
		{
			ps.setLong(1, _treasury);
			ps.setInt(2, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		return true;
	}
	
	/**
	 * Removes all players from the residence zone who do not belong to the owner.<br>
	 * This method triggers a banishment action for foreigners.
	 */
	public void banishForeigners()
	{
		getResidenceZone().banishForeigners(_ownerId);
	}
	
	/**
	 * Checks if the given coordinates are inside the current siege zone.<br>
	 * This method retrieves the {@code SiegeZone} and verifies the position.
	 * @param x The x coordinate to check.
	 * @param y The y coordinate to check.
	 * @param z The z coordinate to check.
	 * @return {@code true} if the coordinates are within the zone, otherwise {@code false}.
	 */
	public boolean checkIfInZone(int x, int y, int z)
	{
		final SiegeZone zone = getZone();
		return (zone != null) && zone.isInsideZone(x, y, z);
	}
	
	/**
	 * Retrieves the {@code SiegeZone} associated with this castle.<br>
	 * It searches for a zone that matches the current residence ID.<br>
	 * The result is cached after the first lookup.
	 * @return the {@code SiegeZone} object or {@code null} if not found.
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
	 * Retrieves the residence zone for this castle.<br>
	 * This method returns a {@code CastleZone} object.
	 * @return the {@code CastleZone} associated with the residence area.
	 */
	@Override
	public CastleZone getResidenceZone()
	{
		return (CastleZone) super.getResidenceZone();
	}
	
	/**
	 * Retrieves the teleport zone associated with this residence.<br>
	 * It searches for a {@code ResidenceTeleportZone} that matches the current residence ID.<br>
	 * If the zone is not already cached, it fetches it from the {@code ZoneManager}.
	 * @return the {@code ResidenceTeleportZone} for this residence or {@code null} if none exists.
	 */
	public ResidenceTeleportZone getTeleZone()
	{
		if (_teleZone == null)
		{
			for (ResidenceTeleportZone zone : ZoneManager.getInstance().getAllZones(ResidenceTeleportZone.class))
			{
				if (zone.getResidenceId() == getResidenceId())
				{
					_teleZone = zone;
					break;
				}
			}
		}
		
		return _teleZone;
	}
	
	/**
	 * Removes all players from the teleport zone.<br>
	 * This method calls {@code oustAllPlayers()} on the {@code ResidenceTeleportZone}.
	 */
	public void oustAllPlayers()
	{
		getTeleZone().oustAllPlayers();
	}
	
	/**
	 * Calculates the distance between this castle and a specific object.<br>
	 * It uses the {@code SiegeZone} to determine the measurement.
	 * @param obj The {@code WorldObject} to measure the distance to.
	 * @return The distance as a {@code double}.
	 */
	public double getDistance(WorldObject obj)
	{
		return getZone().getDistanceToZone(obj);
	}
	
	/**
	 * Closes a specific door for a player.<br>
	 * This method updates the state of the door to closed.
	 * @param player The {@code Player} who is interacting with the door.
	 * @param doorId The unique identifier of the door to be closed.
	 */
	public void closeDoor(Player player, int doorId)
	{
		openCloseDoor(player, doorId, false);
	}
	
	/**
	 * Opens a specific door for the given player.<br>
	 * This method sets the door state to {@code true}.
	 * @param player The {@code Player} who is interacting with the door.
	 * @param doorId The unique identifier of the door to open.
	 */
	public void openDoor(Player player, int doorId)
	{
		openCloseDoor(player, doorId, true);
	}
	
	/**
	 * Opens or closes a specific door based on the provided state.<br>
	 * This action is only allowed for the castle owner or a GM.
	 * @param player The {@code Player} attempting to interact with the door.
	 * @param doorId The unique identifier of the door.
	 * @param open Set to {@code true} to open the door, or {@code false} to close it.
	 */
	public void openCloseDoor(Player player, int doorId, boolean open)
	{
		if ((player.getClanId() != _ownerId) && !player.isGM())
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
	
	/**
	 * Changes the state of a door based on its name.<br>
	 * This method checks if the {@code Player} is the owner or a GM.<br>
	 * It opens or closes the door depending on the {@code open} boolean value.
	 * @param player The {@code Player} attempting to interact with the door.
	 * @param doorName The unique name of the door to be toggled.
	 * @param open Set to {@code true} to open the door, or {@code false} to close it.
	 */
	public void openCloseDoor(Player player, String doorName, boolean open)
	{
		if ((player.getClanId() != _ownerId) && !player.isGM())
		{
			return;
		}
		
		final Door door = getDoor(doorName);
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
	
	// This method is used to begin removing all castle upgrades.
	/**
	 * Removes all upgrades from the castle.<br>
	 * This method clears door and trap upgrades.<br>
	 * It also removes all registered functions from the internal map.
	 */
	public void removeUpgrade()
	{
		removeDoorUpgrade();
		removeTrapUpgrade();
		for (Integer fc : _function.keySet())
		{
			removeFunction(fc);
		}
		
		_function.clear();
	}
	
	// This method updates the castle tax rate.
	/**
	 * Sets the owner of this castle to a specific {@code Clan}.<br>
	 * This method handles removing the previous owner and updating database records.<br>
	 * It also manages residential skills for members and updates siege status.
	 * @param clan The {@code Clan} that will become the new owner. Can be {@code null}.
	 */
	public void setOwner(Clan clan)
	{
		// Remove old owner
		if ((_ownerId > 0) && ((clan == null) || (clan.getId() != _ownerId)))
		{
			final Clan oldOwner = ClanTable.getInstance().getClan(getOwnerId()); // Try to find clan instance.
			if (oldOwner != null)
			{
				if (_formerOwner == null)
				{
					_formerOwner = oldOwner;
					if (PlayerConfig.REMOVE_CASTLE_CIRCLETS)
					{
						CastleManager.getInstance().removeCirclet(_formerOwner, getResidenceId());
					}
				}
				
				try
				{
					final Player oldleader = oldOwner.getLeader().getPlayer();
					if ((oldleader != null) && (oldleader.getMountType() == MountType.WYVERN))
					{
						oldleader.dismount();
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "Exception in setOwner: " + e.getMessage(), e);
				}
				
				oldOwner.setCastleId(0); // Unset has castle flag for old owner.
				for (Player member : oldOwner.getOnlineMembers(0))
				{
					removeResidentialSkills(member);
					member.sendSkillList();
					member.broadcastUserInfo();
				}
			}
		}
		
		updateOwnerInDB(clan); // Update in database
		setShowNpcCrest(false);
		
		// if clan have fortress, remove it
		if ((clan != null) && (clan.getFortId() > 0))
		{
			FortManager.getInstance().getFortByOwner(clan).removeOwner(true);
		}
		
		final Siege siege = getSiege();
		if (siege.isInProgress())
		{
			siege.midVictory(); // Mid victory phase of siege.
		}
		
		if (clan != null)
		{
			for (Player member : clan.getOnlineMembers(0))
			{
				giveResidentialSkills(member);
				member.sendSkillList();
			}
		}
	}
	
	/**
	 * Removes the current owner of the castle.<br>
	 * This method clears all ownership data and resets castle functions.<br>
	 * It also removes residential skills from the former owner's members.
	 * @param clan The {@code Clan} that currently owns the castle.
	 */
	public void removeOwner(Clan clan)
	{
		if (clan != null)
		{
			_formerOwner = clan;
			if (PlayerConfig.REMOVE_CASTLE_CIRCLETS)
			{
				CastleManager.getInstance().removeCirclet(_formerOwner, getResidenceId());
			}
			
			for (Player member : clan.getOnlineMembers(0))
			{
				removeResidentialSkills(member);
				member.sendSkillList();
			}
			
			clan.setCastleId(0);
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		}
		
		setSide(CastleSide.NEUTRAL);
		updateOwnerInDB(null);
		if (getSiege().isInProgress())
		{
			getSiege().midVictory();
		}
		
		for (Integer fc : _function.keySet())
		{
			removeFunction(fc);
		}
		
		_function.clear();
	}
	
	/**
	 * Restores the doors of this castle to their maximum health.
	 */
	public void restoreDoors()
	{
		restoreDoors(false);
	}
	
	/**
	 * Restores the doors of this castle.<br>
	 * Dead doors are revived, open doors are closed, and the health of every door is set according to the {@code isDoorWeak} flag.
	 * @param isDoorWeak Set to {@code true} to restore doors with half of their maximum health.
	 */
	public void restoreDoors(boolean isDoorWeak)
	{
		for (Door door : _doors)
		{
			if (door.isDead())
			{
				door.doRevive();
			}
			
			final long maxHp = door.getMaxHp();
			door.setCurrentHp((isDoorWeak) ? (maxHp / 2) : (maxHp));
			
			if (door.isOpen())
			{
				door.closeMe();
			}
		}
	}
	
	/**
	 * Opens the doors of this castle that stay open outside sieges.
	 */
	public void openDefaultDoors()
	{
		for (Door door : _doors)
		{
			if (door.getTemplate().isOpenByDefault() && !door.isOpen())
			{
				door.openMe();
			}
		}
	}
	
	// This method loads castle.
	/**
	 * Loads the castle data from the database.<br>
	 * This method retrieves information such as the name, siege date, and treasury amount.<br>
	 * It also identifies the owner of the residence.
	 */
	@Override
	protected void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement("SELECT * FROM castle WHERE id = ?");
			PreparedStatement ps2 = con.prepareStatement("SELECT clan_id FROM clan_data WHERE hasCastle = ?"))
		{
			final int residenceId = getResidenceId();
			ps1.setInt(1, residenceId);
			try (ResultSet rs = ps1.executeQuery())
			{
				while (rs.next())
				{
					setName(rs.getString("name"));
					// _OwnerId = rs.getInt("ownerId");
					_siegeDate = Calendar.getInstance();
					_siegeDate.setTimeInMillis(rs.getLong("siegeDate"));
					_siegeTimeRegistrationEndDate = Calendar.getInstance();
					_siegeTimeRegistrationEndDate.setTimeInMillis(rs.getLong("regTimeEnd"));
					_isTimeRegistrationOver = Boolean.parseBoolean(rs.getString("regTimeOver"));
					_castleSide = Enum.valueOf(CastleSide.class, rs.getString("side"));
					_treasury = rs.getLong("treasury");
					_showNpcCrest = Boolean.parseBoolean(rs.getString("showNpcCrest"));
					_ticketBuyCount = rs.getInt("ticketBuyCount");
				}
			}
			
			ps2.setInt(1, residenceId);
			try (ResultSet rs = ps2.executeQuery())
			{
				while (rs.next())
				{
					_ownerId = rs.getInt("clan_id");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadCastleData(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Loads the castle functions from the database.<br>
	 * It retrieves all entries for the current residence ID.<br>
	 * The results are stored in the {@code _function} map.
	 */
	private void loadFunctions()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM castle_functions WHERE castle_id = ?"))
		{
			ps.setInt(1, getResidenceId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					_function.put(rs.getInt("type"), new CastleFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime"), true));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception: Castle.loadFunctions(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes a specific function from the castle.<br>
	 * This method deletes the function from the internal list and the database.
	 * @param functionType The unique identifier of the function to remove.
	 */
	public void removeFunction(int functionType)
	{
		_function.remove(functionType);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM castle_functions WHERE castle_id=? AND type=?"))
		{
			ps.setInt(1, getResidenceId());
			ps.setInt(2, functionType);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception: Castle.removeFunctions(int functionType): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Updates or adds a castle function for a specific player.<br>
	 * This method handles the logic for leasing, leveling, and rates of functions.<br>
	 * It checks if the player has enough {@code Inventory.ADENA_ID} to cover the lease cost.
	 * @param player The {@code Player} performing the action.
	 * @param type The unique identifier for the function type.
	 * @param lvl The level of the function.
	 * @param lease The amount of adena required for the lease.
	 * @param rate The multiplier rate for the function.
	 * @param addNew Whether to create a new {@code CastleFunction} or update an existing one.
	 * @return {@code true} if the operation was successful, {@code false} otherwise.
	 */
	public boolean updateFunctions(Player player, int type, int lvl, int lease, long rate, boolean addNew)
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
			_function.put(type, new CastleFunction(type, lvl, lease, 0, rate, 0, false));
		}
		else if ((lvl == 0) && (lease == 0))
		{
			removeFunction(type);
		}
		else
		{
			final int diffLease = lease - _function.get(type).getLease();
			if (diffLease > 0)
			{
				_function.remove(type);
				_function.put(type, new CastleFunction(type, lvl, lease, 0, rate, -1, false));
			}
			else
			{
				_function.get(type).setLease(lease);
				_function.get(type).setLvl(lvl);
				_function.get(type).dbSave();
			}
		}
		
		return true;
	}
	
	/**
	 * Activates the instance by loading all doors.<br>
	 * This method ensures that door data is initialized for the castle.
	 */
	public void activateInstance()
	{
		loadDoor();
	}
	
	// This method loads castle door data from database.
	/**
	 * Loads the doors associated with this residence.<br>
	 * It iterates through all {@code DoorData}.<br>
	 * Only doors belonging to the current {@code residenceId} are added to the internal list.
	 */
	private void loadDoor()
	{
		for (Door door : DoorData.getInstance().getDoors())
		{
			if ((door.getCastle() != null) && (door.getCastle().getResidenceId() == getResidenceId()))
			{
				_doors.add(door);
			}
		}
	}
	
	// This method loads castle door upgrade data from database.
	/**
	 * Loads door upgrade data from the database.<br>
	 * This method fetches all records for the current residence ID.<br>
	 * It populates the internal state using {@code setDoorUpgrade}.
	 */
	private void loadDoorUpgrade()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM castle_doorupgrade WHERE castleId=?"))
		{
			ps.setInt(1, getResidenceId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					setDoorUpgrade(rs.getInt("doorId"), rs.getInt("ratio"), false);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadCastleDoorUpgrade(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes the upgrade status from all doors in the castle.<br>
	 * This method resets the {@code UpgradeHpRatio} to {@code 1} for every {@code Door}.<br>
	 * It also deletes the corresponding records from the {@code castle_doorupgrade} database table.
	 */
	private void removeDoorUpgrade()
	{
		for (Door door : _doors)
		{
			door.getStat().setUpgradeHpRatio(1);
			door.setCurrentHp(door.getCurrentHp());
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM castle_doorupgrade WHERE castleId=?"))
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
	 * Updates the health ratio and current health for a specific door.<br>
	 * If {@code save} is {@code true}, it persists the change to the database.
	 * @param doorId The unique identifier of the door to update.
	 * @param ratio The new health ratio value to apply.
	 * @param save Whether to save the changes to the database.
	 */
	public void setDoorUpgrade(int doorId, int ratio, boolean save)
	{
		final Door door = (getDoors().isEmpty()) ? DoorData.getInstance().getDoor(doorId) : getDoor(doorId);
		if (door == null)
		{
			return;
		}
		
		door.getStat().setUpgradeHpRatio(ratio);
		door.setCurrentHp(door.getMaxHp());
		
		if (save)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("REPLACE INTO castle_doorupgrade (doorId, ratio, castleId) values (?,?,?)"))
			{
				ps.setInt(1, doorId);
				ps.setInt(2, ratio);
				ps.setInt(3, getResidenceId());
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception: setDoorUpgrade(int doorId, int ratio, int castleId): " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Updates the owner of the residence in the database.<br>
	 * This method handles both assigning a new {@code Clan} and removing an existing one.<br>
	 * It also updates the {@code hasCastle} flag for all relevant clans.<br>
	 * If a new owner is assigned, it broadcasts a victory message to their members.
	 * @param clan The {@code Clan} that will own the residence, or {@code null} to remove ownership.
	 */
	private void updateOwnerInDB(Clan clan)
	{
		if (clan != null)
		{
			_ownerId = clan.getId(); // Update owner id property.
		}
		else
		{
			_ownerId = 0; // Remove owner
			CastleManorManager.getInstance().resetManorData(getResidenceId());
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Need to remove has castle flag from clan_data, should be checked from castle table.
			final int residenceId = getResidenceId();
			try (PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET hasCastle = 0 WHERE hasCastle = ?"))
			{
				ps.setInt(1, residenceId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET hasCastle = ? WHERE clan_id = ?"))
			{
				ps.setInt(1, residenceId);
				ps.setInt(2, _ownerId);
				ps.execute();
			}
			
			// Announce to clan members.
			if (clan != null)
			{
				clan.setCastleId(residenceId); // Set has castle flag for new owner.
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				clan.broadcastToOnlineMembers(new PlaySound(1, "Siege_Victory", 0, 0, 0, 0, 0));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: updateOwnerInDB(Pledge clan): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves a {@code Door} object based on its unique identifier.<br>
	 * This method searches through the internal list of doors.<br>
	 * It returns {@code null} if no door matches the provided ID.
	 * @param doorId The unique identification number of the door to find.
	 * @return The matching {@code Door} object, or {@code null} if not found.
	 */
	public Door getDoor(int doorId)
	{
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
	 * Retrieves a {@code Door} object based on its template name.<br>
	 * It searches through all doors in the castle.<br>
	 * Returns {@code null} if no matching door is found.
	 * @param doorName The unique name of the door to find.
	 * @return The matching {@code Door} instance or {@code null}.
	 */
	public Door getDoor(String doorName)
	{
		for (Door door : _doors)
		{
			if (door.getTemplate().getName().equals(doorName))
			{
				return door;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the list of all doors associated with this castle.
	 * @return a {@code List} containing all {@code Door} objects.
	 */
	public List<Door> getDoors()
	{
		return _doors;
	}
	
	/**
	 * Checks if the current siege is a first mid victory.<br>
	 * This method returns the value of the internal {@code _isFirstMidVictory} flag.
	 * @return {@code true} if it is a first mid victory, {@code false} otherwise.
	 */
	public boolean isFirstMidVictory()
	{
		return _isFirstMidVictory;
	}
	
	/**
	 * Sets whether the first mid victory is enabled.<br>
	 * This determines if the first mid victory condition is active.
	 * @param value The boolean state to set for the first mid victory.
	 */
	public void setFirstMidVictory(boolean value)
	{
		_isFirstMidVictory = value;
	}
	
	/**
	 * Retrieves the unique identifier of the owner.<br>
	 * This value corresponds to the {@code Clan} that owns this castle.
	 * @return The {@code int} ID of the current owner.
	 */
	@Override
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	/**
	 * Retrieves the {@code Clan} that owns this castle.<br>
	 * It returns {@code null} if no owner is assigned.
	 * @return the {@code Clan} object of the owner or {@code null}.
	 */
	public Clan getOwner()
	{
		return (_ownerId != 0) ? ClanTable.getInstance().getClan(_ownerId) : null;
	}
	
	/**
	 * Retrieves the {@code Siege} object associated with this castle.<br>
	 * If the object does not exist, it creates a new one.
	 * @return The current {@code Siege} instance.
	 */
	public Siege getSiege()
	{
		if (_siege == null)
		{
			_siege = new Siege(this);
		}
		
		return _siege;
	}
	
	/**
	 * Retrieves the scheduled date for the siege.
	 * @return the {@code Calendar} object representing the siege date.
	 */
	public Calendar getSiegeDate()
	{
		return _siegeDate;
	}
	
	/**
	 * Checks if the registration period for the siege has ended.
	 * @return {@code true} if the time is over, {@code false} otherwise.
	 */
	public boolean isTimeRegistrationOver()
	{
		return _isTimeRegistrationOver;
	}
	
	/**
	 * Sets whether the time registration period has ended.<br>
	 * This updates the {@code _isTimeRegistrationOver} flag.
	 * @param value The new status of the time registration.
	 */
	public void setTimeRegistrationOver(boolean value)
	{
		_isTimeRegistrationOver = value;
	}
	
	/**
	 * Retrieves the end date for siege time registration.<br>
	 * If no date is set, it initializes and returns the current system time.
	 * @return a {@code Calendar} object representing the registration deadline.
	 */
	public Calendar getTimeRegistrationOverDate()
	{
		if (_siegeTimeRegistrationEndDate == null)
		{
			_siegeTimeRegistrationEndDate = Calendar.getInstance();
		}
		
		return _siegeTimeRegistrationEndDate;
	}
	
	/**
	 * Retrieves the tax percentage based on the castle side and transaction type.<br>
	 * This method checks whether the action is a purchase or a sale.<br>
	 * It returns the value defined in {@code FeatureConfig}.
	 * @param type The type of tax to calculate, such as buy or sell.
	 * @return The integer percentage for the specified tax.
	 */
	public int getTaxPercent(CastleTaxType type)
	{
		final int taxPercent;
		switch (_castleSide)
		{
			case LIGHT:
			{
				taxPercent = type == CastleTaxType.BUY ? FeatureConfig.CASTLE_BUY_TAX_LIGHT : FeatureConfig.CASTLE_SELL_TAX_LIGHT;
				break;
			}
			case DARK:
			{
				taxPercent = type == CastleTaxType.BUY ? FeatureConfig.CASTLE_BUY_TAX_DARK : FeatureConfig.CASTLE_SELL_TAX_DARK;
				break;
			}
			default:
			{
				taxPercent = type == CastleTaxType.BUY ? FeatureConfig.CASTLE_BUY_TAX_NEUTRAL : FeatureConfig.CASTLE_SELL_TAX_NEUTRAL;
				break;
			}
		}
		
		return taxPercent;
	}
	
	/**
	 * Retrieves the tax rate for a specific type.<br>
	 * This method converts the percentage value into a decimal format.
	 * @param taxType The {@code CastleTaxType} to check.
	 * @return The tax rate as a {@code double}.
	 */
	public double getTaxRate(CastleTaxType taxType)
	{
		return getTaxPercent(taxType) / 100.0;
	}
	
	/**
	 * Retrieves the current amount of gold in the treasury.
	 * @return The total value stored in the {@code _treasury}.
	 */
	public long getTreasury()
	{
		return _treasury;
	}
	
	/**
	 * Checks if the NPC crest is currently visible.
	 * @return {@code true} if the crest should be shown, {@code false} otherwise.
	 */
	public boolean getShowNpcCrest()
	{
		return _showNpcCrest;
	}
	
	/**
	 * Updates the visibility of the NPC crest.<br>
	 * This method sets whether the crest should be displayed for NPCs.<br>
	 * It triggers an update if the new value differs from the current one.
	 * @param showNpcCrest The boolean value to set for showing the NPC crest.
	 */
	public void setShowNpcCrest(boolean showNpcCrest)
	{
		if (_showNpcCrest != showNpcCrest)
		{
			_showNpcCrest = showNpcCrest;
			updateShowNpcCrest();
		}
	}
	
	/**
	 * Updates the reputation scores for clans involved in a siege.<br>
	 * This method calculates points based on whether a clan won, lost, or defended a castle.<br>
	 * It also sends system messages to online members regarding their new reputation totals.
	 */
	public void updateClansReputation()
	{
		final Clan owner = ClanTable.getInstance().getClan(getOwnerId());
		if (_formerOwner != null)
		{
			if (_formerOwner != owner)
			{
				final int maxreward = Math.max(0, _formerOwner.getReputationScore());
				_formerOwner.takeReputationScore(FeatureConfig.LOOSE_CASTLE_POINTS);
				if (owner != null)
				{
					owner.addReputationScore(Math.min(FeatureConfig.TAKE_CASTLE_POINTS, maxreward));
					owner.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.YOUR_CLAN_HAS_WON_THE_SIEGE_CLAN_REPUTATION_POINTS_S1).addInt(Math.min(FeatureConfig.TAKE_CASTLE_POINTS, maxreward)));
				}
				
				_formerOwner.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.YOUR_CLAN_HAS_LOST_THE_SIEGE_CLAN_REPUTATION_POINTS_S1).addInt(FeatureConfig.LOOSE_CASTLE_POINTS));
			}
			else
			{
				_formerOwner.addReputationScore(FeatureConfig.CASTLE_DEFENDED_POINTS);
				_formerOwner.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.YOUR_CLAN_HAS_WON_THE_SIEGE_CLAN_REPUTATION_POINTS_S1).addInt(FeatureConfig.CASTLE_DEFENDED_POINTS));
			}
		}
		else if (owner != null)
		{
			owner.addReputationScore(FeatureConfig.TAKE_CASTLE_POINTS);
			owner.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.YOUR_CLAN_HAS_WON_THE_SIEGE_CLAN_REPUTATION_POINTS_S1).addInt(FeatureConfig.TAKE_CASTLE_POINTS));
		}
	}
	
	/**
	 * Updates the {@code showNpcCrest} value in the database.<br>
	 * This method saves the current state of the crest visibility for this castle.<br>
	 * It uses the unique residence ID to identify which record to update.
	 */
	public void updateShowNpcCrest()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle SET showNpcCrest = ? WHERE id = ?"))
		{
			ps.setString(1, String.valueOf(_showNpcCrest));
			ps.setInt(2, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.info("Error saving showNpcCrest for castle " + getName() + ": " + e.getMessage());
		}
	}
	
	/**
	 * Adds a new {@code Artefact} to the internal collection.<br>
	 * This method registers the provided object for use in siege mechanics.
	 * @param artefact The {@code Artefact} instance to register.
	 */
	public void registerArtefact(Artefact artefact)
	{
		_artefacts.add(artefact);
	}
	
	/**
	 * Retrieves the set of artefacts associated with this castle.
	 * @return a {@code Set} containing all {@code Artefact} objects.
	 */
	public Set<Artefact> getArtefacts()
	{
		return _artefacts;
	}
	
	/**
	 * Retrieves the total number of tickets purchased.
	 * @return The current count of tickets bought as an {@code int}.
	 */
	public int getTicketBuyCount()
	{
		return _ticketBuyCount;
	}
	
	/**
	 * Updates the number of tickets purchased for this castle.<br>
	 * This method saves the new value to the database.
	 * @param count The new ticket purchase count to set.
	 */
	public void setTicketBuyCount(int count)
	{
		_ticketBuyCount = count;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle SET ticketBuyCount = ? WHERE id = ?"))
		{
			ps.setInt(1, _ticketBuyCount);
			ps.setInt(2, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the current upgrade level of a specific flame tower.<br>
	 * This method looks up the tower based on its position in the residence area.<br>
	 * If the tower does not exist, it returns {@code 0}.
	 * @param towerIndex The index of the tower to check.
	 * @return The current upgrade level of the tower or {@code 0} if none is found.
	 */
	public int getTrapUpgradeLevel(int towerIndex)
	{
		final TowerSpawn spawn = SiegeManager.getInstance().getFlameTowers(getResidenceId()).get(towerIndex);
		return (spawn != null) ? spawn.getUpgradeLevel() : 0;
	}
	
	/**
	 * Updates the upgrade level of a specific tower.<br>
	 * This method modifies the level in memory and optionally saves it to the database.
	 * @param towerIndex The index of the tower to update.
	 * @param level The new upgrade level for the tower.
	 * @param save If {@code true}, the change is persisted to the database.
	 */
	public void setTrapUpgrade(int towerIndex, int level, boolean save)
	{
		if (save)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("REPLACE INTO castle_trapupgrade (castleId, towerIndex, level) values (?,?,?)"))
			{
				ps.setInt(1, getResidenceId());
				ps.setInt(2, towerIndex);
				ps.setInt(3, level);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception: setTrapUpgradeLevel(int towerIndex, int level, int castleId): " + e.getMessage(), e);
			}
		}
		
		final TowerSpawn spawn = SiegeManager.getInstance().getFlameTowers(getResidenceId()).get(towerIndex);
		if (spawn != null)
		{
			spawn.setUpgradeLevel(level);
		}
	}
	
	/**
	 * Resets the upgrade levels of all flame towers for this residence.<br>
	 * Removes the trap upgrade records from the database for this castle.<br>
	 * Handles any potential SQL exceptions by logging a warning.
	 */
	private void removeTrapUpgrade()
	{
		final int residenceId = getResidenceId();
		for (TowerSpawn ts : SiegeManager.getInstance().getFlameTowers(residenceId))
		{
			ts.setUpgradeLevel(0);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM castle_trapupgrade WHERE castleId=?"))
		{
			ps.setInt(1, residenceId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: removeDoorUpgrade(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Initializes the residence zone for this castle.<br>
	 * It searches all {@code CastleZone} objects in the {@code ZoneManager}.<br>
	 * The method sets the correct zone if the {@code residenceId} matches.
	 */
	@Override
	protected void initResidenceZone()
	{
		for (CastleZone zone : ZoneManager.getInstance().getAllZones(CastleZone.class))
		{
			if (zone.getResidenceId() == getResidenceId())
			{
				setResidenceZone(zone);
				break;
			}
		}
	}
	
	/**
	 * Grants residential skills to the specified player.<br>
	 * This method also checks if the {@code Player} has a high enough pledge class.<br>
	 * If they are a Marquis or higher, it adds a specific side-based skill.
	 * @param player The {@code Player} who will receive the skills.
	 */
	@Override
	public void giveResidentialSkills(Player player)
	{
		super.giveResidentialSkills(player);
		if ((player.isClanLeader()) || (player.getPledgeType() == 100)) // Leader or Elite
		{
			player.addSkill(_castleSide == CastleSide.DARK ? CommonSkill.KNIGHT_OF_DARKNESS.getSkill() : CommonSkill.KNIGHT_OF_SPLENDOR.getSkill());
			player.addSkill(_castleSide == CastleSide.DARK ? CommonSkill.PROTECTION_OF_DARKNESS.getSkill() : CommonSkill.PROTECTION_OF_LIGHT.getSkill());
		}
		else // Non Elite members
		{
			player.addSkill(_castleSide == CastleSide.DARK ? CommonSkill.PROTECTION_OF_DARKNESS.getSkill() : CommonSkill.PROTECTION_OF_LIGHT.getSkill());
		}
	}
	
	/**
	 * Removes specific residential skills from a player.<br>
	 * This method clears the {@code KNIGHT_OF_SPLENDOR}, {@code PROTECTION_OF_LIGHT}, {@code KNIGHT_OF_DARKNESS}, and {@code PROTECTION_OF_DARKNESS} skills.<br>
	 * It also calls the parent class implementation to ensure all residential skills are removed.
	 * @param player The {@code Player} object to modify.
	 */
	@Override
	public void removeResidentialSkills(Player player)
	{
		super.removeResidentialSkills(player);
		player.removeSkill(CommonSkill.KNIGHT_OF_SPLENDOR.getId());
		player.removeSkill(CommonSkill.PROTECTION_OF_LIGHT.getId());
		player.removeSkill(CommonSkill.KNIGHT_OF_DARKNESS.getId());
		player.removeSkill(CommonSkill.PROTECTION_OF_DARKNESS.getId());
	}
	
	/**
	 * Removes all existing side NPCs from the world.<br>
	 * Clears the internal list of side NPCs.<br>
	 * Spawns new NPCs based on the configured spawn holders.<br>
	 * Adds the newly spawned NPCs to the internal tracking list.
	 */
	public void spawnSideNpcs()
	{
		for (Npc npc : _sideNpcs)
		{
			if (npc != null)
			{
				npc.deleteMe();
			}
		}
		
		_sideNpcs.clear();
		
		for (CastleSpawnHolder holder : getSideSpawns())
		{
			if (holder != null)
			{
				Spawn spawn;
				try
				{
					spawn = new Spawn(holder.getNpcId());
				}
				catch (Exception e)
				{
					LOGGER.warning(Castle.class.getSimpleName() + ": " + e.getMessage());
					return;
				}
				
				spawn.setXYZ(holder);
				spawn.setHeading(holder.getHeading());
				final Npc npc = spawn.doSpawn(false);
				spawn.stopRespawn();
				npc.broadcastInfo();
				_sideNpcs.add(npc);
			}
		}
	}
	
	/**
	 * Retrieves the list of spawn points for the current castle side.<br>
	 * This method fetches data from {@code CastleData}.
	 * @return a {@code List} of {@code CastleSpawnHolder} objects.
	 */
	public List<CastleSpawnHolder> getSideSpawns()
	{
		return CastleData.getInstance().getSpawnsForSide(getResidenceId(), getSide());
	}
	
	/**
	 * Updates the current side of the castle.<br>
	 * This method updates the database and broadcasts the change to all players.<br>
	 * It also triggers the spawning of new NPCs based on the updated side.
	 * @param side The new {@code CastleSide} to assign to this castle.
	 */
	public void setSide(CastleSide side)
	{
		if (_castleSide == side)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle SET side = ? WHERE id = ?"))
		{
			ps.setString(1, side.toString());
			ps.setInt(2, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		_castleSide = side;
		World.broadcastToAllOnlinePlayers(new ExCastleState(this));
		spawnSideNpcs();
	}
	
	/**
	 * Retrieves the side of the castle.<br>
	 * This method returns the current {@code CastleSide} value.
	 * @return The current {@code CastleSide}.
	 */
	public CastleSide getSide()
	{
		return _castleSide;
	}
}
