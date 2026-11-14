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
package org.l2jmobius.gameserver.mechanics.announce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;

/**
 * Represents a system-wide announcement message to be displayed to players.<br>
 * This class handles the data and logic for broadcasting information across the game server.
 * @author UnAfraid
 */
public class Announcement implements IAnnouncement
{
	protected static final Logger LOGGER = Logger.getLogger(Announcement.class.getName());
	
	private static final String INSERT_QUERY = "INSERT INTO announcements (type, content, author) VALUES (?, ?, ?)";
	private static final String UPDATE_QUERY = "UPDATE announcements SET type = ?, content = ?, author = ? WHERE id = ?";
	private static final String DELETE_QUERY = "DELETE FROM announcements WHERE id = ?";
	
	protected int _id;
	private AnnouncementType _type;
	private String _content;
	private String _author;
	
	/**
	 * Creates a new {@code Announcement} object.<br>
	 * This constructor initializes the announcement with specific details.
	 * @param type The {@code AnnouncementType} of this message.
	 * @param content The text body of the announcement.
	 * @param author The name of the person who created it.
	 */
	public Announcement(AnnouncementType type, String content, String author)
	{
		_type = type;
		_content = content;
		_author = author;
	}
	
	/**
	 * Creates a new {@code Announcement} object from a database result set.<br>
	 * This constructor populates the fields using data from the provided {@code ResultSet}.
	 * @param rset The {@code ResultSet} containing the announcement data.
	 * @throws SQLException If an error occurs while accessing the database results.
	 */
	public Announcement(ResultSet rset) throws SQLException
	{
		_id = rset.getInt("id");
		_type = AnnouncementType.findById(rset.getInt("type"));
		_content = rset.getString("content");
		_author = rset.getString("author");
	}
	
	/**
	 * Retrieves the unique identifier for this {@code Announcement}.<br>
	 * This value is used to identify the record in the database.
	 * @return The unique integer ID of the announcement.
	 */
	@Override
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the type of this announcement.<br>
	 * This method returns the {@code AnnouncementType} assigned to the object.
	 * @return The {@code AnnouncementType} of the current announcement.
	 */
	@Override
	public AnnouncementType getType()
	{
		return _type;
	}
	
	/**
	 * Sets the category of the announcement.<br>
	 * This updates the {@code _type} field with the provided value.
	 * @param type The new {@code AnnouncementType} to assign.
	 */
	@Override
	public void setType(AnnouncementType type)
	{
		_type = type;
	}
	
	/**
	 * Retrieves the text content of this announcement.<br>
	 * This method returns the string stored in the {@code _content} field.
	 * @return The content as a {@code String}.
	 */
	@Override
	public String getContent()
	{
		return _content;
	}
	
	/**
	 * Sets the text content for this {@code Announcement}.<br>
	 * This updates the internal {@code _content} field.
	 * @param content The new string to set as the announcement content.
	 */
	@Override
	public void setContent(String content)
	{
		_content = content;
	}
	
	/**
	 * Retrieves the name of the person who created this announcement.<br>
	 * This method returns the value stored in the {@code _author} field.
	 * @return The author's name as a {@code String}.
	 */
	@Override
	public String getAuthor()
	{
		return _author;
	}
	
	/**
	 * Sets the name of the person who created the announcement.<br>
	 * This updates the {@code _author} field.
	 * @param author The name of the author to set.
	 */
	@Override
	public void setAuthor(String author)
	{
		_author = author;
	}
	
	/**
	 * Checks if the current {@code Announcement} object is valid.<br>
	 * This method currently always returns {@code true}.
	 * @return {@code true} if the announcement is valid.
	 */
	@Override
	public boolean isValid()
	{
		return true;
	}
	
	/**
	 * Saves the current announcement data into the database.<br>
	 * This method uses an {@code INSERT} query to create a new record.<br>
	 * It updates the internal {@code _id} field with the generated key.
	 * @return {@code true} if the save was successful, or {@code false} if an error occurred.
	 */
	@Override
	public boolean storeMe()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS))
		{
			ps.setInt(1, _type.ordinal());
			ps.setString(2, _content);
			ps.setString(3, _author);
			ps.execute();
			try (ResultSet rset = ps.getGeneratedKeys())
			{
				if (rset.next())
				{
					_id = rset.getInt(1);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't store announcement: ", e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Updates the current announcement data in the database.<br>
	 * This method uses the {@code UPDATE_QUERY} to save changes.
	 * @return {@code true} if the update was successful, or {@code false} if an error occurred.
	 */
	@Override
	public boolean updateMe()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_QUERY))
		{
			ps.setInt(1, _type.ordinal());
			ps.setString(2, _content);
			ps.setString(3, _author);
			ps.setInt(4, _id);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't store announcement: ", e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Removes this announcement from the database.<br>
	 * It uses the unique {@code _id} to identify which record to delete.
	 * @return {@code true} if the deletion was successful, or {@code false} if an error occurred.
	 */
	@Override
	public boolean deleteMe()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_QUERY))
		{
			ps.setInt(1, _id);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't remove announcement: ", e);
			return false;
		}
		
		return true;
	}
}
