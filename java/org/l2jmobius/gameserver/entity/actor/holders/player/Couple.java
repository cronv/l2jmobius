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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.IdManager;

/**
 * Represents a romantic relationship between two {@code Player} entities.<br>
 * This class manages the data and logic associated with player couples in the game.
 * @author evill33t
 */
public class Couple
{
	private static final Logger LOGGER = Logger.getLogger(Couple.class.getName());
	
	private int _id = 0;
	private int _player1Id = 0;
	private int _player2Id = 0;
	private boolean _maried = false;
	private Calendar _affiancedDate;
	private Calendar _weddingDate;
	
	/**
	 * Creates a new {@code Couple} instance using a specific ID.<br>
	 * This constructor loads the couple data from the database.<br>
	 * It populates fields like player IDs and marriage status.
	 * @param coupleId The unique identifier for the couple in the database.
	 */
	public Couple(int coupleId)
	{
		_id = coupleId;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM mods_wedding WHERE id = ?"))
		{
			ps.setInt(1, _id);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					_player1Id = rs.getInt("player1Id");
					_player2Id = rs.getInt("player2Id");
					_maried = Boolean.parseBoolean(rs.getString("married"));
					_affiancedDate = Calendar.getInstance();
					_affiancedDate.setTimeInMillis(rs.getLong("affianceDate"));
					
					_weddingDate = Calendar.getInstance();
					_weddingDate.setTimeInMillis(rs.getLong("weddingDate"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception: Couple.load(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Creates a new {@code Couple} instance using two players.<br>
	 * This method initializes the IDs and dates for both players.<br>
	 * It also saves the new couple record into the database.
	 * @param player1 The first {@code Player} in the couple.
	 * @param player2 The second {@code Player} in the couple.
	 */
	public Couple(Player player1, Player player2)
	{
		final long currentTime = System.currentTimeMillis();
		_player1Id = player1.getObjectId();
		_player2Id = player2.getObjectId();
		
		_affiancedDate = Calendar.getInstance();
		_affiancedDate.setTimeInMillis(currentTime);
		
		_weddingDate = Calendar.getInstance();
		_weddingDate.setTimeInMillis(currentTime);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO mods_wedding (id, player1Id, player2Id, married, affianceDate, weddingDate) VALUES (?, ?, ?, ?, ?, ?)"))
		{
			_id = IdManager.getInstance().getNextId();
			ps.setInt(1, _id);
			ps.setInt(2, _player1Id);
			ps.setInt(3, _player2Id);
			ps.setBoolean(4, false);
			ps.setLong(5, _affiancedDate.getTimeInMillis());
			ps.setLong(6, _weddingDate.getTimeInMillis());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not create couple: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Updates the marriage status of a {@code Couple}.<br>
	 * Sets the {@code _maried} flag to {@code true}.<br>
	 * Records the current date as the wedding date in the database.
	 */
	public void marry()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE mods_wedding set married = ?, weddingDate = ? where id = ?"))
		{
			ps.setBoolean(1, true);
			_weddingDate = Calendar.getInstance();
			ps.setLong(2, _weddingDate.getTimeInMillis());
			ps.setInt(3, _id);
			ps.execute();
			_maried = true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not marry: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Ends the marriage between two players.<br>
	 * Removes the couple record from the database.<br>
	 * Logs an error if the database operation fails.
	 */
	public void divorce()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM mods_wedding WHERE id=?"))
		{
			ps.setInt(1, _id);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception: Couple.divorce(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the unique identifier for this {@code Couple}.<br>
	 * This ID is used to distinguish between different couples in the system.
	 * @return The unique integer ID of the couple.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the unique identifier for the first player in the couple.<br>
	 * This value is stored as an {@code int}.
	 * @return The {@code int} ID of the first player.
	 */
	public int getPlayer1Id()
	{
		return _player1Id;
	}
	
	/**
	 * Retrieves the unique identifier for the second player in the couple.<br>
	 * This value is stored as an {@code int}.
	 * @return The {@code int} ID of the second player.
	 */
	public int getPlayer2Id()
	{
		return _player2Id;
	}
	
	/**
	 * Checks if the couple is currently married.
	 * @return {@code true} if the couple is married, {@code false} otherwise.
	 */
	public boolean getMaried()
	{
		return _maried;
	}
	
	/**
	 * Retrieves the date when the couple became engaged.
	 * @return the {@code Calendar} object representing the engagement date.
	 */
	public Calendar getAffiancedDate()
	{
		return _affiancedDate;
	}
	
	/**
	 * Retrieves the date of the wedding.<br>
	 * This method returns the {@code Calendar} object stored in the private field.
	 * @return the {@code Calendar} representing the wedding date.
	 */
	public Calendar getWeddingDate()
	{
		return _weddingDate;
	}
}
