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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.Couple;

/**
 * Manages the lifecycle and data of {@code Couple} entities within the game world.<br>
 * It handles loading, saving, and updating couple information in the database.
 * @author evill33t
 */
public class CoupleManager
{
	private static final Logger LOGGER = Logger.getLogger(CoupleManager.class.getName());
	
	private final List<Couple> _couples = new CopyOnWriteArrayList<>();
	
	/**
	 * Initializes a new instance of the {@code CoupleManager}.<br>
	 * This constructor calls the internal {@code load()} method.<br>
	 * It prepares the manager to handle {@code Couple} data.
	 */
	protected CoupleManager()
	{
		load();
	}
	
	/**
	 * Refreshes the list of couples from the database.<br>
	 * This method clears all current data and calls {@code load()}.
	 */
	public void reload()
	{
		_couples.clear();
		load();
	}
	
	/**
	 * Loads all couple data from the database.<br>
	 * It populates the {@code _couples} list with entries from the {@code mods_wedding} table.<br>
	 * Any errors during the loading process are logged as severe errors.
	 */
	private void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement ps = con.createStatement();
			ResultSet rs = ps.executeQuery("SELECT id FROM mods_wedding ORDER BY id"))
		{
			while (rs.next())
			{
				_couples.add(new Couple(rs.getInt("id")));
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _couples.size() + " couples(s)");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception: CoupleManager.load(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves a {@code Couple} object based on its unique ID.<br>
	 * This method searches the internal list for the matching index.<br>
	 * It returns {@code null} if no couple is found.
	 * @param coupleId The unique identifier of the couple to retrieve.
	 * @return The {@code Couple} object, or {@code null} if it does not exist.
	 */
	public Couple getCouple(int coupleId)
	{
		final int index = getCoupleIndex(coupleId);
		return index >= 0 ? _couples.get(index) : null;
	}
	
	/**
	 * Creates a new {@code Couple} between two players.<br>
	 * This method checks if both players are available for coupling.<br>
	 * It updates the partner and couple IDs for both {@code Player} objects.
	 * @param player1 The first player to join the couple.
	 * @param player2 The second player to join the couple.
	 */
	public void createCouple(Player player1, Player player2)
	{
		if ((player1 == null) || (player2 == null) || (player1.getPartnerId() != 0) || (player2.getPartnerId() != 0))
		{
			return;
		}
		
		final int player1id = player1.getObjectId();
		final int player2id = player2.getObjectId();
		final Couple couple = new Couple(player1, player2);
		_couples.add(couple);
		player1.setPartnerId(player2id);
		player2.setPartnerId(player1id);
		final int coupleId = couple.getId();
		player1.setCoupleId(coupleId);
		player2.setCoupleId(coupleId);
	}
	
	/**
	 * Removes a couple from the system based on their unique ID.<br>
	 * This method resets the marriage status for both players involved.<br>
	 * It also removes the {@code Couple} object from the internal list.
	 * @param coupleId The unique identifier of the couple to be deleted.
	 */
	public void deleteCouple(int coupleId)
	{
		final int index = getCoupleIndex(coupleId);
		final Couple couple = _couples.get(index);
		if (couple == null)
		{
			return;
		}
		
		final Player player1 = World.getPlayer(couple.getPlayer1Id());
		final Player player2 = World.getPlayer(couple.getPlayer2Id());
		if (player1 != null)
		{
			player1.setPartnerId(0);
			player1.setMarried(false);
			player1.setCoupleId(0);
		}
		
		if (player2 != null)
		{
			player2.setPartnerId(0);
			player2.setMarried(false);
			player2.setCoupleId(0);
		}
		
		couple.divorce();
		_couples.remove(index);
	}
	
	/**
	 * Finds the position of a {@code Couple} in the internal list.<br>
	 * It searches for the couple that matches the provided {@code coupleId}.
	 * @param coupleId The unique identifier of the couple to find.
	 * @return The zero-based index of the couple, or -1 if it is not found.
	 */
	public int getCoupleIndex(int coupleId)
	{
		int i = 0;
		for (Couple temp : _couples)
		{
			if ((temp != null) && (temp.getId() == coupleId))
			{
				return i;
			}
			
			i++;
		}
		
		return -1;
	}
	
	/**
	 * Retrieves all current couples.<br>
	 * This method returns the internal list of {@code Couple} objects.
	 * @return a {@code List} containing all {@code Couple} instances.
	 */
	public List<Couple> getCouples()
	{
		return _couples;
	}
	
	/**
	 * Retrieves the singleton instance of the {@code CoupleManager}.<br>
	 * This method provides a global point of access to the manager.
	 * @return The single instance of {@code CoupleManager}.
	 */
	public static CoupleManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CoupleManager INSTANCE = new CoupleManager();
	}
}
