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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.CastleData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Defender;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.interfaces.IPositionable;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.SiegeGuardHolder;

/**
 * Manages the spawning and behavior of siege guards within castles.<br>
 * This class handles the lifecycle of {@code Defender} entities during siege events.
 * @author St3eT
 */
public class SiegeGuardManager
{
	private static final Logger LOGGER = Logger.getLogger(SiegeGuardManager.class.getName());
	private static final Set<Item> _droppedTickets = ConcurrentHashMap.newKeySet();
	private static final Map<Integer, Set<Spawn>> _siegeGuardSpawn = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code SiegeGuardManager}.<br>
	 * This constructor clears all dropped tickets and loads the initial data.
	 */
	protected SiegeGuardManager()
	{
		_droppedTickets.clear();
		load();
	}
	
	/**
	 * Loads hired siege guard tickets from the database.<br>
	 * It populates the {@code _droppedTickets} set with items.<br>
	 * This method handles errors by logging a warning message.
	 */
	private void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			ResultSet rs = con.createStatement().executeQuery("SELECT * FROM castle_siege_guards Where isHired = 1"))
		{
			while (rs.next())
			{
				final int x = rs.getInt("x");
				final int y = rs.getInt("y");
				final int z = rs.getInt("z");
				final Castle castle = CastleManager.getInstance().getCastle(x, y, z);
				if (castle == null)
				{
					LOGGER.warning("Siege guard ticket cannot be placed! Castle is null at X: " + x + ", Y: " + y + ", Z: " + z);
					continue;
				}
				
				final int npcId = rs.getInt("npcId");
				final SiegeGuardHolder holder = getSiegeGuardByNpc(castle.getResidenceId(), npcId);
				if ((holder != null) && !castle.getSiege().isInProgress())
				{
					final Item dropticket = new Item(holder.getItemId());
					dropticket.setItemLocation(ItemLocation.VOID);
					dropticket.dropMe(null, x, y, z);
					World.addObject(dropticket);
					_droppedTickets.add(dropticket);
				}
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _droppedTickets.size() + " siege guards tickets.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves a {@code SiegeGuardHolder} based on the castle and item ID.<br>
	 * This method searches through all guards for a specific castle.<br>
	 * It returns the first holder that matches the provided {@code itemId}.
	 * @param castleId The unique identifier of the castle.
	 * @param itemId The unique identifier of the item.
	 * @return The matching {@code SiegeGuardHolder} or {@code null} if not found.
	 */
	public SiegeGuardHolder getSiegeGuardByItem(int castleId, int itemId)
	{
		for (SiegeGuardHolder holder : CastleData.getInstance().getSiegeGuardsForCastle(castleId))
		{
			if (holder.getItemId() == itemId)
			{
				return holder;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a {@code SiegeGuardHolder} based on the castle and NPC identifiers.<br>
	 * It searches through all guards associated with the specified {@code castleId}.<br>
	 * Returns {@code null} if no matching guard is found.
	 * @param castleId The unique identifier of the castle.
	 * @param npcId The unique identifier of the NPC.
	 * @return The matching {@code SiegeGuardHolder} or {@code null}.
	 */
	public SiegeGuardHolder getSiegeGuardByNpc(int castleId, int npcId)
	{
		for (SiegeGuardHolder holder : CastleData.getInstance().getSiegeGuardsForCastle(castleId))
		{
			if (holder.getNpcId() == npcId)
			{
				return holder;
			}
		}
		
		return null;
	}
	
	/**
	 * Checks if the player is standing too close to any dropped ticket.<br>
	 * It returns {@code true} if a ticket is within 25 units of the player.<br>
	 * Otherwise, it returns {@code false}.
	 * @param player The {@code Player} to check the distance for.
	 * @return {@code true} if the player is too close to a ticket, {@code false} otherwise.
	 */
	public boolean isTooCloseToAnotherTicket(Player player)
	{
		for (Item ticket : _droppedTickets)
		{
			if (ticket.calculateDistance3D(player) < 25)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the maximum number of NPCs for a specific item has been reached.<br>
	 * This method compares the current ticket count against the limit defined in {@code SiegeGuardHolder}.
	 * @param castleId The unique identifier for the castle.
	 * @param itemId The unique identifier for the item being checked.
	 * @return {@code true} if the count is equal to or exceeds the maximum amount, {@code false} otherwise.
	 */
	public boolean isAtNpcLimit(int castleId, int itemId)
	{
		final SiegeGuardHolder holder = getSiegeGuardByItem(castleId, itemId);
		long count = 0;
		for (Item ticket : _droppedTickets)
		{
			if (ticket.getId() == itemId)
			{
				count++;
			}
		}
		
		return count >= holder.getMaxNpcAmout();
	}
	
	/**
	 * Adds a siege guard for a {@code Player} based on the provided {@code itemId}.<br>
	 * This method checks if the player is in a castle and if the NPC limit has not been reached.<br>
	 * It saves the new guard to the database and spawns it at the player's location.<br>
	 * A ticket item is also dropped for the player.
	 * @param itemId The unique identifier of the siege guard ticket.
	 * @param player The {@code Player} who is hiring the siege guard.
	 */
	public void addTicket(int itemId, Player player)
	{
		final Castle castle = CastleManager.getInstance().getCastle(player);
		if (castle == null)
		{
			return;
		}
		
		final int castleId = castle.getResidenceId();
		if (isAtNpcLimit(castleId, itemId))
		{
			return;
		}
		
		final SiegeGuardHolder holder = getSiegeGuardByItem(castleId, itemId);
		if (holder != null)
		{
			final int playerX = player.getX();
			final int playerY = player.getY();
			final int playerZ = player.getZ();
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement("Insert Into castle_siege_guards (castleId, npcId, x, y, z, heading, respawnDelay, isHired) Values (?, ?, ?, ?, ?, ?, ?, ?)"))
			{
				statement.setInt(1, castleId);
				statement.setInt(2, holder.getNpcId());
				statement.setInt(3, playerX);
				statement.setInt(4, playerY);
				statement.setInt(5, playerZ);
				statement.setInt(6, player.getHeading());
				statement.setInt(7, 0);
				statement.setInt(8, 1);
				statement.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Error adding siege guard for castle " + castle.getName() + ": " + e.getMessage(), e);
			}
			
			spawnMercenary(player, holder);
			final Item dropticket = new Item(itemId);
			dropticket.setItemLocation(ItemLocation.VOID);
			dropticket.dropMe(null, playerX, playerY, playerZ);
			World.addObject(dropticket);
			_droppedTickets.add(dropticket);
		}
	}
	
	/**
	 * Spawns a mercenary NPC at a specific location.<br>
	 * This method creates a new {@code Defender} based on the provided holder's template.<br>
	 * It sets the initial health, mana, and position of the spawned entity.
	 * @param pos The location where the mercenary will be spawned.
	 * @param holder The holder object containing the NPC data for the mercenary.
	 */
	private void spawnMercenary(IPositionable pos, SiegeGuardHolder holder)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(holder.getNpcId());
		if (template != null)
		{
			final Defender npc = new Defender(template);
			npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
			npc.setDecayed(false);
			npc.setHeading(pos.getHeading());
			npc.spawnMe(pos.getX(), pos.getY(), (pos.getZ() + 20));
			npc.scheduleDespawn(3000);
			npc.setImmobilized(holder.isStationary());
		}
	}
	
	/**
	 * Removes all active tickets associated with a specific castle.<br>
	 * This method checks the {@code _droppedTickets} set for valid items.<br>
	 * It decays and removes tickets that belong to the provided {@code castleId}.
	 * @param castleId The unique identifier of the castle.
	 */
	public void deleteTickets(int castleId)
	{
		for (Item ticket : _droppedTickets)
		{
			if ((ticket != null) && (getSiegeGuardByItem(castleId, ticket.getId()) != null))
			{
				ticket.decayMe();
				_droppedTickets.remove(ticket);
			}
		}
	}
	
	/**
	 * Removes a siege guard ticket from the game world.<br>
	 * This method identifies the correct castle and holder associated with the {@code Item}.<br>
	 * It then removes the corresponding npc and deletes the item from the dropped tickets set.
	 * @param item The {@code Item} instance to be removed.
	 */
	public void removeTicket(Item item)
	{
		final Castle castle = CastleManager.getInstance().getCastle(item);
		if (castle == null)
		{
			return;
		}
		
		final SiegeGuardHolder holder = getSiegeGuardByItem(castle.getResidenceId(), item.getId());
		if (holder == null)
		{
			return;
		}
		
		removeSiegeGuard(holder.getNpcId(), item);
		_droppedTickets.remove(item);
	}
	
	/**
	 * Loads the siege guards from the database for a specific {@code Castle}.<br>
	 * It checks if the castle is owned to determine the hiring status.<br>
	 * The loaded spawns are added to the internal tracking set.
	 * @param castle The {@code Castle} object used to identify the residence and owner ID.
	 */
	private void loadSiegeGuard(Castle castle)
	{
		final int castleId = castle.getResidenceId();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM castle_siege_guards Where castleId = ? And isHired = ?"))
		{
			ps.setInt(1, castleId);
			ps.setInt(2, castle.getOwnerId() > 0 ? 1 : 0);
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Spawn spawn = new Spawn(rs.getInt("npcId"));
					spawn.setAmount(1);
					spawn.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
					spawn.setHeading(rs.getInt("heading"));
					spawn.setRespawnDelay(rs.getInt("respawnDelay"));
					spawn.setLocationId(0);
					
					getSpawnedGuards(castleId).add(spawn);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error loading siege guard for castle " + castle.getName() + ": " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes a hired siege guard from the database.<br>
	 * This method deletes the record based on the provided {@code npcId} and coordinates.
	 * @param npcId The unique identifier of the NPC to remove.
	 * @param pos The {@code IPositionable} location where the guard is located.
	 */
	public void removeSiegeGuard(int npcId, IPositionable pos)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("Delete From castle_siege_guards Where npcId = ? And x = ? AND y = ? AND z = ? AND isHired = 1"))
		{
			ps.setInt(1, npcId);
			ps.setInt(2, pos.getX());
			ps.setInt(3, pos.getY());
			ps.setInt(4, pos.getZ());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error deleting hired siege guard at " + pos + " : " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes all hired siege guards from a specific castle.<br>
	 * This method updates the database to clear active guards.
	 * @param castle The {@code Castle} object representing the target location.
	 */
	public void removeSiegeGuards(Castle castle)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("Delete From castle_siege_guards Where castleId = ? And isHired = 1"))
		{
			ps.setInt(1, castle.getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error deleting hired siege guard for castle " + castle.getName() + ": " + e.getMessage(), e);
		}
	}
	
	/**
	 * Spawns the siege guards for a specific {@code Castle}.<br>
	 * This method initializes all valid spawns and handles their respawn logic.<br>
	 * It also updates the immobilization state of the last spawned NPC.
	 * @param castle The {@code Castle} object for which to spawn guards.
	 */
	public void spawnSiegeGuard(Castle castle)
	{
		try
		{
			final boolean isHired = (castle.getOwnerId() > 0);
			loadSiegeGuard(castle);
			
			final int castleId = castle.getResidenceId();
			for (Spawn spawn : getSpawnedGuards(castleId))
			{
				if (spawn != null)
				{
					spawn.init();
					if (isHired || (spawn.getRespawnDelay() == 0))
					{
						spawn.stopRespawn();
					}
					
					final Npc lastSpawn = spawn.getLastSpawn();
					final SiegeGuardHolder holder = getSiegeGuardByNpc(castleId, lastSpawn.getId());
					if (holder == null)
					{
						continue;
					}
					
					lastSpawn.setImmobilized(holder.isStationary());
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error spawning siege guards for castle " + castle.getName(), e);
		}
	}
	
	/**
	 * Removes all active siege guards from a specific castle.<br>
	 * This method stops the respawn logic for each spawn point.<br>
	 * It also deletes any existing {@code Npc} instances currently spawned.<br>
	 * Finally, it clears the list of spawned guards for that castle.
	 * @param castle The {@code Castle} object to clear guards from.
	 */
	public void unspawnSiegeGuard(Castle castle)
	{
		final int castleId = castle.getResidenceId();
		for (Spawn spawn : getSpawnedGuards(castleId))
		{
			if (spawn == null)
			{
				continue;
			}
			
			final Npc lastSpawn = spawn.getLastSpawn();
			if (lastSpawn != null)
			{
				spawn.stopRespawn();
				lastSpawn.deleteMe();
			}
		}
		
		getSpawnedGuards(castleId).clear();
	}
	
	/**
	 * Retrieves the set of active guard spawns for a specific castle.<br>
	 * If no guards exist for the given {@code castleId}, an empty set is returned.
	 * @param castleId The unique identifier of the castle.
	 * @return A {@code Set} of {@code Spawn} objects associated with the castle.
	 */
	public Set<Spawn> getSpawnedGuards(int castleId)
	{
		return _siegeGuardSpawn.computeIfAbsent(castleId, _ -> ConcurrentHashMap.newKeySet());
	}
	
	/**
	 * Retrieves the singleton instance of the {@code SiegeGuardManager}.<br>
	 * Use this method to access the manager from anywhere in the code.
	 * @return The active {@code SiegeGuardManager} instance.
	 */
	public static SiegeGuardManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SiegeGuardManager INSTANCE = new SiegeGuardManager();
	}
}
