/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.entity.residences;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.enums.ClanHallGrade;
import org.l2jmobius.gameserver.entity.clan.enums.ClanHallType;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a clan-owned residence and headquarters in the game world.
 * @author St3eT
 */
public class ClanHall extends AbstractResidence
{
	private static final Logger LOGGER = Logger.getLogger(ClanHall.class.getName());
	
	// Static parameters
	private final ClanHallType _type;
	private final int _minBid;
	final int _lease;
	private final int _deposit;
	private final Set<Integer> _npcs = new HashSet<>();
	private final Set<Door> _doors = new HashSet<>();
	private final Set<ClanHallTeleportHolder> _teleports = new HashSet<>();
	private final Location _ownerLocation;
	private final Location _banishLocation;
	
	// Dynamic parameters
	Clan _owner = null;
	long _paidUntil = 0;
	protected ScheduledFuture<?> _checkPaymentTask = null;
	
	// Other
	private static final String INSERT_CLANHALL = "INSERT INTO clanhall (id, ownerId, paidUntil) VALUES (?,?,?)";
	private static final String LOAD_CLANHALL = "SELECT * FROM clanhall WHERE id=?";
	private static final String UPDATE_CLANHALL = "UPDATE clanhall SET ownerId=?,paidUntil=? WHERE id=?";
	
	/**
	 * Creates a clan hall from the given configuration, loading data and initializing its zone and functions.
	 * @param params The {@code StatSet} with the hall configuration.
	 */
	public ClanHall(StatSet params)
	{
		super(params.getInt("id"));
		
		// Set static parameters
		setName(params.getString("name"));
		_grade = params.getEnum("grade", ClanHallGrade.class);
		_type = params.getEnum("type", ClanHallType.class);
		_minBid = params.getInt("minBid");
		_lease = params.getInt("lease");
		_deposit = params.getInt("deposit");
		final List<Integer> npcs = params.getList("npcList", Integer.class);
		if (npcs != null)
		{
			_npcs.addAll(npcs);
		}
		
		final List<Door> doors = params.getList("doorList", Door.class);
		if (doors != null)
		{
			_doors.addAll(doors);
		}
		
		final List<ClanHallTeleportHolder> teleports = params.getList("teleportList", ClanHallTeleportHolder.class);
		if (teleports != null)
		{
			_teleports.addAll(teleports);
		}
		
		_ownerLocation = params.getLocation("owner_loc");
		_banishLocation = params.getLocation("banish_loc");
		
		// Set dynamic parameters (from DB).
		load();
		
		// Init Clan Hall zone and Functions.
		initResidenceZone();
		initFunctions();
	}
	
	/**
	 * Loads owner and payment data from the database, creating a record if none exists.
	 */
	@Override
	protected void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement loadStatement = con.prepareStatement(LOAD_CLANHALL);
			PreparedStatement insertStatement = con.prepareStatement(INSERT_CLANHALL))
		{
			final int residenceId = getResidenceId();
			loadStatement.setInt(1, residenceId);
			
			try (ResultSet rset = loadStatement.executeQuery())
			{
				if (rset.next())
				{
					setPaidUntil(rset.getLong("paidUntil"));
					setOwner(rset.getInt("ownerId"));
				}
				else
				{
					insertStatement.setInt(1, residenceId);
					insertStatement.setInt(2, 0); // New clanhall should not have owner.
					insertStatement.setInt(3, 0); // New clanhall should not have paid until.
					if (insertStatement.execute())
					{
						LOGGER.info("Clan Hall " + getName() + " (" + residenceId + ") was sucessfully created.");
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.INFO, "Failed loading clan hall", e);
		}
	}
	
	/**
	 * Persists the owner id and payment expiration to the database.
	 */
	public void updateDB()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_CLANHALL))
		{
			statement.setInt(1, getOwnerId());
			statement.setLong(2, _paidUntil);
			statement.setInt(3, getResidenceId());
			statement.execute();
		}
		catch (SQLException e)
		{
			LOGGER.warning(e.toString());
		}
	}
	
	/**
	 * Locates and assigns the {@code ClanHallZone} matching this residence id.
	 */
	@Override
	protected void initResidenceZone()
	{
		for (ClanHallZone zone : ZoneManager.getInstance().getAllZones(ClanHallZone.class))
		{
			if (zone.getResidenceId() == getResidenceId())
			{
				setResidenceZone(zone);
				break;
			}
		}
	}
	
	/**
	 * Retrieves the number of days past the payment deadline.
	 * @return The days overdue, or {@code 0} if not expired.
	 */
	public int getCostFailDay()
	{
		final Duration failDay = Duration.between(Instant.ofEpochMilli(_paidUntil), Instant.now());
		return failDay.isNegative() ? 0 : (int) failDay.toDays();
	}
	
	/**
	 * Banishes all non-owner players from the residence zone.
	 */
	public void banishOthers()
	{
		getResidenceZone().banishForeigners(getOwnerId());
	}
	
	/**
	 * Opens or closes every door in the clan hall.
	 * @param open {@code true} to open the doors, {@code false} to close them.
	 */
	public void openCloseDoors(boolean open)
	{
		_doors.forEach(door -> door.openCloseMe(open));
	}
	
	/**
	 * Retrieves the doors of this clan hall.
	 * @return A {@code Set} of {@code Door} objects.
	 */
	public Set<Door> getDoors()
	{
		return _doors;
	}
	
	/**
	 * Retrieves the NPC ids associated with this clan hall.
	 * @return A {@code Set} of NPC ids.
	 */
	public Set<Integer> getNpcs()
	{
		return _npcs;
	}
	
	/**
	 * Retrieves the type of this clan hall.
	 * @return The {@code ClanHallType}.
	 */
	public ClanHallType getType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the clan that owns this hall.
	 * @return The owning {@code Clan}, or {@code null} if none.
	 */
	public Clan getOwner()
	{
		return _owner;
	}
	
	/**
	 * Retrieves the id of the owning clan.
	 * @return The owner id, or {@code 0} if none.
	 */
	@Override
	public int getOwnerId()
	{
		final Clan owner = _owner;
		return (owner != null) ? owner.getId() : 0;
	}
	
	/**
	 * Sets the owner by resolving the clan with the given id.
	 * @param clanId The id of the clan to set as owner.
	 */
	public void setOwner(int clanId)
	{
		setOwner(ClanTable.getInstance().getClan(clanId));
	}
	
	/**
	 * Assigns or clears the owner, scheduling payment checks when set or resetting hall state when cleared.
	 * @param clan The {@code Clan} to set as owner, or {@code null} to clear it.
	 */
	public void setOwner(Clan clan)
	{
		if (clan != null)
		{
			_owner = clan;
			clan.setHideoutId(getResidenceId());
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
			if (_paidUntil == 0)
			{
				setPaidUntil(Instant.now().plus(Duration.ofDays(7)).toEpochMilli());
			}
			
			final int failDays = getCostFailDay();
			final long time = failDays > 0 ? (failDays > 8 ? Instant.now().toEpochMilli() : Instant.ofEpochMilli(_paidUntil).plus(Duration.ofDays(failDays + 1)).toEpochMilli()) : _paidUntil;
			_checkPaymentTask = ThreadPool.schedule(new CheckPaymentTask(), Math.max(0, time - System.currentTimeMillis()));
		}
		else
		{
			if (_owner != null)
			{
				_owner.setHideoutId(0);
				_owner.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_owner));
				removeFunctions();
			}
			
			_owner = null;
			setPaidUntil(0);
			if (_checkPaymentTask != null)
			{
				_checkPaymentTask.cancel(true);
				_checkPaymentTask = null;
			}
		}
		
		updateDB();
	}
	
	/**
	 * Retrieves the timestamp when the current lease payment expires.
	 * @return The expiration timestamp.
	 */
	public long getPaidUntil()
	{
		return _paidUntil;
	}
	
	/**
	 * Sets the payment expiration timestamp.
	 * @param paidUntil The expiration timestamp.
	 */
	public void setPaidUntil(long paidUntil)
	{
		_paidUntil = paidUntil;
	}
	
	/**
	 * Retrieves the timestamp when the next payment is due.
	 * @return The next payment time in milliseconds, or {@code 0} if none scheduled.
	 */
	public long getNextPayment()
	{
		return (_checkPaymentTask != null) ? System.currentTimeMillis() + _checkPaymentTask.getDelay(TimeUnit.MILLISECONDS) : 0;
	}
	
	/**
	 * Retrieves the owner location.
	 * @return The owner {@code Location}.
	 */
	public Location getOwnerLocation()
	{
		return _ownerLocation;
	}
	
	/**
	 * Retrieves the location where banished players are sent.
	 * @return The banishment {@code Location}.
	 */
	public Location getBanishLocation()
	{
		return _banishLocation;
	}
	
	/**
	 * Retrieves all teleports of this clan hall.
	 * @return A {@code Set} of {@code ClanHallTeleportHolder} objects.
	 */
	public Set<ClanHallTeleportHolder> getTeleportList()
	{
		return _teleports;
	}
	
	/**
	 * Retrieves the teleports available at or below the given function level.
	 * @param functionLevel The required function level.
	 * @return A {@code List} of matching {@code ClanHallTeleportHolder} objects.
	 */
	public List<ClanHallTeleportHolder> getTeleportList(int functionLevel)
	{
		final List<ClanHallTeleportHolder> result = new ArrayList<>();
		for (ClanHallTeleportHolder holder : _teleports)
		{
			if (holder.getMinFunctionLevel() <= functionLevel)
			{
				result.add(holder);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the minimum bid required for this residence.
	 * @return The minimum bid.
	 */
	public int getMinBid()
	{
		return _minBid;
	}
	
	/**
	 * Retrieves the lease amount for this clan hall.
	 * @return The lease value.
	 */
	public int getLease()
	{
		return _lease;
	}
	
	/**
	 * Retrieves the deposit amount for this clan hall.
	 * @return The deposit value.
	 */
	public int getDeposit()
	{
		return _deposit;
	}
	
	class CheckPaymentTask implements Runnable
	{
		@Override
		public void run()
		{
			if (_owner != null)
			{
				if (_owner.getWarehouse().getAdena() < _lease)
				{
					if (getCostFailDay() > 8)
					{
						_owner.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.THE_CLAN_HALL_FEE_IS_ONE_WEEK_OVERDUE_THEREFORE_THE_CLAN_HALL_OWNERSHIP_HAS_BEEN_REVOKED));
						setOwner(null);
					}
					else
					{
						_checkPaymentTask = ThreadPool.schedule(new CheckPaymentTask(), 24 * 60 * 60 * 1000); // 1 day
						final SystemMessage sm = new SystemMessage(SystemMessageId.THE_PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_DEPOSIT_THE_NECESSARY_AMOUNT_OF_ADENA_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
						sm.addInt(_lease);
						_owner.broadcastToOnlineMembers(sm);
					}
				}
				else
				{
					_owner.getWarehouse().destroyItem(ItemProcessType.FEE, Inventory.ADENA_ID, _lease, null, null);
					setPaidUntil(Instant.ofEpochMilli(_paidUntil).plus(Duration.ofDays(7)).toEpochMilli());
					_checkPaymentTask = ThreadPool.schedule(new CheckPaymentTask(), _paidUntil - System.currentTimeMillis());
					updateDB();
				}
			}
		}
	}
	
	/**
	 * Returns the class name, residence name and id.
	 * @return A string describing this clan hall.
	 */
	@Override
	public String toString()
	{
		return (getClass().getSimpleName() + ":" + getName() + "[" + getResidenceId() + "]");
	}
}
