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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.HomunculusData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.homunculus.Homunculus;
import org.l2jmobius.gameserver.mechanics.homunculus.HomunculusTemplate;

/**
 * Manages the lifecycle and data of {@code Homunculus} entities.<br>
 * This class handles loading {@code HomunculusTemplate} data and coordinating homunculus actions.
 * @author nexvill, Mobius
 */
public class HomunculusManager
{
	private static final Logger LOGGER = Logger.getLogger(HomunculusManager.class.getName());
	
	private static final String SELECT_QUERY = "SELECT slot, id, level, exp, skillLevel1, skillLevel2, skillLevel3, skillLevel4, skillLevel5, active FROM character_homunculus WHERE ownerId=? ORDER by slot ASC";
	private static final String INSERT_QUERY = "INSERT INTO `character_homunculus` (ownerId, slot, id, level, exp, skillLevel1, skillLevel2, skillLevel3, skillLevel4, skillLevel5, active) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
	private static final String DELETE_QUERY = "DELETE FROM character_homunculus WHERE ownerId=? AND slot=? AND id=? AND level=? AND exp=? AND skillLevel1=? AND skillLevel2=? AND skillLevel3=? AND skillLevel4=? AND skillLevel5=? AND active=?";
	private static final String UPDATE_QUERY = "UPDATE character_homunculus SET level=?, exp=?, skillLevel1=?, skillLevel2=?, skillLevel3=?, skillLevel4=?, skillLevel5=?, active=?, slot=? WHERE id=? AND ownerId=?";
	
	/**
	 * Retrieves all active homunculi belonging to a specific player.<br>
	 * This method fetches data from the database and creates {@code Homunculus} objects.<br>
	 * It also removes any invalid templates found in the database.
	 * @param owner The {@code Player} who owns the homunculi.
	 * @return A {@code List} of {@code Homunculus} objects belonging to the player.
	 */
	public List<Homunculus> select(Player owner)
	{
		final List<Homunculus> list = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_QUERY))
		{
			statement.setInt(1, owner.getObjectId());
			final ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				final int id = rset.getInt("id");
				final int slot = rset.getInt("slot");
				final int level = rset.getInt("level");
				final int exp = rset.getInt("exp");
				final int skillLevel1 = rset.getInt("skillLevel1");
				final int skillLevel2 = rset.getInt("skillLevel2");
				final int skillLevel3 = rset.getInt("skillLevel3");
				final int skillLevel4 = rset.getInt("skillLevel4");
				final int skillLevel5 = rset.getInt("skillLevel5");
				final boolean isActive = rset.getInt("active") > 0;
				
				final HomunculusTemplate template = HomunculusData.getInstance().getTemplate(id);
				Homunculus homunculus = null;
				boolean remove = template == null;
				if (!remove)
				{
					homunculus = new Homunculus(template, slot, level, exp, skillLevel1, skillLevel2, skillLevel3, skillLevel4, skillLevel5, isActive);
				}
				
				if (remove)
				{
					delete(owner, slot, id, level, exp, skillLevel1, skillLevel2, skillLevel3, skillLevel4, skillLevel5, isActive);
				}
				else
				{
					list.add(homunculus);
				}
			}
			
			rset.close();
		}
		catch (Exception e)
		{
			LOGGER.warning("HomunculusManager.select(Player): " + e);
		}
		
		return list;
	}
	
	/**
	 * Saves a new {@code Homunculus} to the database for a specific {@code Player}.<br>
	 * This method handles the SQL insertion process.
	 * @param owner The {@code Player} who owns the homunculus.
	 * @param homunculus The {@code Homunculus} object to be saved.
	 * @return {@code true} if the save was successful, or {@code false} if an error occurred.
	 */
	public boolean insert(Player owner, Homunculus homunculus)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_QUERY))
		{
			statement.setInt(1, owner.getObjectId());
			statement.setInt(2, homunculus.getSlot());
			statement.setInt(3, homunculus.getId());
			statement.setInt(4, homunculus.getLevel());
			statement.setInt(5, homunculus.getExp());
			statement.setInt(6, homunculus.getSkillLevel1());
			statement.setInt(7, homunculus.getSkillLevel2());
			statement.setInt(8, homunculus.getSkillLevel3());
			statement.setInt(9, homunculus.getSkillLevel4());
			statement.setInt(10, homunculus.getSkillLevel5());
			statement.setInt(11, homunculus.isActive() ? 1 : 0);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.warning(owner.getHomunculusList() + " could not add homunculus to homunculus list: " + homunculus);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Removes a specific {@code Homunculus} from the database.<br>
	 * This method checks if the {@code owner} is the correct owner of the entity.<br>
	 * It deletes the record based on all unique attributes of the {@code homunculus}.
	 * @param owner The {@code Player} who owns the homunculus.
	 * @param homunculus The {@code Homunculus} object to be removed.
	 * @return {@code true} if the deletion was successful, or {@code false} otherwise.
	 */
	public boolean delete(Player owner, Homunculus homunculus)
	{
		return delete(owner, homunculus.getSlot(), homunculus.getId(), homunculus.getLevel(), homunculus.getExp(), homunculus.getSkillLevel1(), homunculus.getSkillLevel2(), homunculus.getSkillLevel3(), homunculus.getSkillLevel4(), homunculus.getSkillLevel5(), homunculus.isActive());
	}
	
	/**
	 * Deletes a specific homunculus from the database based on its unique attributes.<br>
	 * This method removes the record associated with the provided owner and slot information.
	 * @param owner The {@code Player} who owns the homunculus.
	 * @param slot The inventory slot of the homunculus.
	 * @param id The unique identifier of the homunculus.
	 * @param level The current level of the homunculus.
	 * @param exp The current experience points of the homunculus.
	 * @param skillLevel1 The first skill level of the homunculus.
	 * @param skillLevel2 The second skill level of the homunculus.
	 * @param skillLevel3 The third skill level of the homunculus.
	 * @param skillLevel4 The fourth skill level of the homunculus.
	 * @param skillLevel5 The fifth skill level of the homunculus.
	 * @param active The active status of the homunculus.
	 * @return {@code true} if the deletion was successful, or {@code false} if an error occurred.
	 */
	private boolean delete(Player owner, int slot, int id, int level, int exp, int skillLevel1, int skillLevel2, int skillLevel3, int skillLevel4, int skillLevel5, boolean active)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_QUERY))
		{
			statement.setInt(1, owner.getObjectId());
			statement.setInt(2, slot);
			statement.setInt(3, id);
			statement.setInt(4, level);
			statement.setInt(5, exp);
			statement.setInt(6, skillLevel1);
			statement.setInt(7, skillLevel2);
			statement.setInt(8, skillLevel3);
			statement.setInt(9, skillLevel4);
			statement.setInt(10, skillLevel5);
			statement.setInt(11, active ? 1 : 0);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.warning(owner.getHomunculusList() + " could not delete homunculus: " + id + " " + active + " ownerId: " + owner.getObjectId());
			return false;
		}
		
		return true;
	}
	
	/**
	 * Updates the database record for a specific {@code Homunculus}.<br>
	 * This method saves the current stats and slot information of the homunculus.
	 * @param owner The {@code Player} who owns the homunculus.
	 * @param homunculus The {@code Homunculus} object containing the updated data.
	 * @return {@code true} if the update was successful, or {@code false} if an error occurred.
	 */
	public boolean update(Player owner, Homunculus homunculus)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_QUERY))
		{
			statement.setInt(1, homunculus.getLevel());
			statement.setInt(2, homunculus.getExp());
			statement.setInt(3, homunculus.getSkillLevel1());
			statement.setInt(4, homunculus.getSkillLevel2());
			statement.setInt(5, homunculus.getSkillLevel3());
			statement.setInt(6, homunculus.getSkillLevel4());
			statement.setInt(7, homunculus.getSkillLevel5());
			statement.setInt(8, homunculus.isActive() ? 1 : 0);
			statement.setInt(9, homunculus.getSlot());
			statement.setInt(10, homunculus.getId());
			statement.setInt(11, owner.getObjectId());
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.warning(owner.getHomunculusList() + " could not update homunculus list on owner id: " + owner.getObjectId());
			return false;
		}
		
		return true;
	}
	
	private static class SingletonHolder
	{
		protected static final HomunculusManager INSTANCE = new HomunculusManager();
	}
	
	/**
	 * Retrieves the singleton instance of the {@code HomunculusManager}.<br>
	 * This provides a global point of access to manage homunculus data.
	 * @return The single active instance of {@code HomunculusManager}.
	 */
	public static HomunculusManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
}
