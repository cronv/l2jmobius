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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.SocialClass;
import org.l2jmobius.gameserver.entity.clan.enums.ClanHallGrade;
import org.l2jmobius.gameserver.entity.zone.type.ResidenceZone;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;

/**
 * Serves as the base class for all residence types in the game.<br>
 * It provides core functionality and common logic shared by different residential areas.
 * @author xban1x
 */
public abstract class AbstractResidence extends ListenersContainer
{
	private static final Logger LOGGER = Logger.getLogger(AbstractResidence.class.getName());
	
	protected ClanHallGrade _grade = ClanHallGrade.GRADE_NONE;
	
	private final int _residenceId;
	private String _name;
	private ResidenceZone _zone = null;
	private final Map<Integer, ResidenceFunction> _functions = new ConcurrentHashMap<>();
	private List<SkillLearn> _residentialSkills = new ArrayList<>();
	
	/**
	 * Creates a new instance of an {@code AbstractResidence}.<br>
	 * This constructor sets the unique identifier for the residence.<br>
	 * It also triggers the initialization of residential skills.
	 * @param residenceId The unique ID assigned to this residence.
	 */
	public AbstractResidence(int residenceId)
	{
		_residenceId = residenceId;
		initResidentialSkills();
	}
	
	protected abstract void load();
	
	protected abstract void initResidenceZone();
	
	public abstract int getOwnerId();
	
	/**
	 * Initializes the list of residential skills for this residence.<br>
	 * It fetches available skills from {@code SkillTreeData} based on the current residence ID.
	 */
	protected void initResidentialSkills()
	{
		_residentialSkills = SkillTreeData.getInstance().getAvailableResidentialSkills(getResidenceId());
	}
	
	/**
	 * Retrieves the current grade of the residence.<br>
	 * This value is stored in the {@code _grade} field.
	 * @return The {@code ClanHallGrade} of this residence.
	 */
	public ClanHallGrade getGrade()
	{
		return _grade;
	}
	
	/**
	 * Retrieves the unique identifier for this residence.<br>
	 * This value is assigned when the {@code AbstractResidence} is created.
	 * @return The {@code int} ID of the residence.
	 */
	public int getResidenceId()
	{
		return _residenceId;
	}
	
	/**
	 * Retrieves the name of the residence.<br>
	 * This method returns the current {@code String} value stored in the private field.
	 * @return The name of the residence.
	 */
	public String getName()
	{
		return _name;
	}
	
	// TODO: Remove it later when both castles and forts are loaded from same table.
	/**
	 * Sets the display name for this residence.<br>
	 * The provided {@code String} is stored in the internal field.
	 * @param name The new name to assign to the residence.
	 */
	public void setName(String name)
	{
		_name = name;
	}
	
	/**
	 * Retrieves the current {@code ResidenceZone} associated with this residence.<br>
	 * This method returns the zone object or {@code null} if no zone is assigned.
	 * @return The {@code ResidenceZone} of this residence.
	 */
	public ResidenceZone getResidenceZone()
	{
		return _zone;
	}
	
	/**
	 * Sets the {@code ResidenceZone} for this residence.<br>
	 * This method updates the internal {@code _zone} field.
	 * @param zone The new {@code ResidenceZone} to assign.
	 */
	protected void setResidenceZone(ResidenceZone zone)
	{
		_zone = zone;
	}
	
	/**
	 * Grants residential skills to a specific player.<br>
	 * This method checks the player's social class against the required requirements.<br>
	 * If the player meets the criteria, the skill is added to their character.
	 * @param player The {@code Player} who will receive the skills.
	 */
	public void giveResidentialSkills(Player player)
	{
		if ((_residentialSkills != null) && !_residentialSkills.isEmpty())
		{
			final int playerSocialClass = player.getPledgeClass() + 1;
			for (SkillLearn skill : _residentialSkills)
			{
				final SocialClass skillSocialClass = skill.getSocialClass();
				if ((skillSocialClass == null) || (playerSocialClass >= skillSocialClass.ordinal()))
				{
					player.addSkill(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()), false);
				}
			}
		}
	}
	
	/**
	 * Removes all residential skills from the specified player.<br>
	 * This method iterates through the {@code _residentialSkills} list.<br>
	 * It calls {@code player.removeSkill} for each skill found.
	 * @param player The {@code Player} object to modify.
	 */
	public void removeResidentialSkills(Player player)
	{
		if ((_residentialSkills != null) && !_residentialSkills.isEmpty())
		{
			for (SkillLearn skill : _residentialSkills)
			{
				player.removeSkill(skill.getSkillId(), false);
			}
		}
	}
	
	/**
	 * Initializes the residence functions from the database.<br>
	 * This method loads all active functions associated with the current {@code _residenceId}.<br>
	 * It removes any expired functions that cannot be reactivated.
	 */
	protected void initFunctions()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM residence_functions WHERE residenceId = ?"))
		{
			ps.setInt(1, _residenceId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int id = rs.getInt("id");
					final int level = rs.getInt("level");
					final long expiration = rs.getLong("expiration");
					final ResidenceFunction func = new ResidenceFunction(id, level, expiration, this);
					if ((expiration <= System.currentTimeMillis()) && !func.reactivate())
					{
						removeFunction(func);
						continue;
					}
					
					_functions.put(id, func);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to initialize functions for residence: " + _residenceId, e);
		}
	}
	
	/**
	 * Adds a new function to the residence.<br>
	 * This method creates a {@code ResidenceFunction} object using the provided values.
	 * @param id The unique identifier for the function.
	 * @param level The level of the function to be added.
	 */
	public void addFunction(int id, int level)
	{
		addFunction(new ResidenceFunction(id, level, this));
	}
	
	/**
	 * Adds a new function to the residence.<br>
	 * This method saves the function data to the database.<br>
	 * It also updates the internal {@code _functions} map.
	 * @param func The {@code ResidenceFunction} to be added.
	 */
	public void addFunction(ResidenceFunction func)
	{
		final int funcId = func.getId();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO residence_functions (id, level, expiration, residenceId) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE level = ?, expiration = ?"))
		{
			final int funcLevel = func.getLevel();
			final long funcExpiration = func.getExpiration();
			
			ps.setInt(1, funcId);
			ps.setInt(2, funcLevel);
			ps.setLong(3, funcExpiration);
			ps.setInt(4, _residenceId);
			ps.setInt(5, funcLevel);
			ps.setLong(6, funcExpiration);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to add function: " + funcId + " for residence: " + _residenceId, e);
		}
		finally
		{
			if (_functions.containsKey(funcId))
			{
				removeFunction(_functions.get(funcId));
			}
			
			_functions.put(funcId, func);
		}
	}
	
	/**
	 * Removes a specific function from the residence.<br>
	 * This method deletes the entry from the database and cancels its expiration.<br>
	 * It also removes the function from the internal {@code _functions} map.
	 * @param func The {@code ResidenceFunction} to be removed.
	 */
	public void removeFunction(ResidenceFunction func)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM residence_functions WHERE residenceId = ? and id = ?"))
		{
			ps.setInt(1, _residenceId);
			ps.setInt(2, func.getId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to remove function: " + func.getId() + " residence: " + _residenceId, e);
		}
		finally
		{
			func.cancelExpiration();
			_functions.remove(func.getId());
		}
	}
	
	/**
	 * Removes all functions associated with this residence.<br>
	 * This method deletes the records from the database for the current {@code _residenceId}.<br>
	 * It also cancels any active expirations and clears the internal {@code _functions} map.
	 */
	public void removeFunctions()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM residence_functions WHERE residenceId = ?"))
		{
			ps.setInt(1, _residenceId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to remove functions for residence: " + _residenceId, e);
		}
		finally
		{
			_functions.values().forEach(ResidenceFunction::cancelExpiration);
			_functions.clear();
		}
	}
	
	/**
	 * Checks if the residence contains a specific function type.<br>
	 * It iterates through all active functions to find a match.
	 * @param type The {@code ResidenceFunctionType} to search for.
	 * @return {@code true} if the function exists, otherwise {@code false}.
	 */
	public boolean hasFunction(ResidenceFunctionType type)
	{
		for (ResidenceFunction function : _functions.values())
		{
			final ResidenceFunctionTemplate template = function.getTemplate();
			if ((template != null) && (template.getType() == type))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves a {@code ResidenceFunction} based on its specific type.<br>
	 * This method searches through all available functions in the residence.<br>
	 * It returns the first matching function found or {@code null} if no match exists.
	 * @param type The {@code ResidenceFunctionType} to search for.
	 * @return The corresponding {@code ResidenceFunction} or {@code null}.
	 */
	public ResidenceFunction getFunction(ResidenceFunctionType type)
	{
		for (ResidenceFunction function : _functions.values())
		{
			if (function.getType() == type)
			{
				return function;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a specific {@code ResidenceFunction} based on its unique identifiers.<br>
	 * This method searches through all available functions in the residence.<br>
	 * It returns {@code null} if no matching function is found.
	 * @param id The unique identifier of the function.
	 * @param level The current level of the function.
	 * @return The matching {@code ResidenceFunction} object or {@code null}.
	 */
	public ResidenceFunction getFunction(int id, int level)
	{
		for (ResidenceFunction func : _functions.values())
		{
			if ((func.getId() == id) && (func.getLevel() == level))
			{
				return func;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a {@code ResidenceFunction} based on its unique identifier.<br>
	 * This method searches through all registered functions in the residence.<br>
	 * It returns {@code null} if no function matches the provided ID.
	 * @param id The unique identifier of the function to find.
	 * @return The matching {@code ResidenceFunction} object or {@code null}.
	 */
	public ResidenceFunction getFunction(int id)
	{
		for (ResidenceFunction func : _functions.values())
		{
			if (func.getId() == id)
			{
				return func;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the current level of a specific residence function.<br>
	 * It looks up the {@code ResidenceFunction} based on the provided type.<br>
	 * If the function does not exist, it returns {@code 0}.
	 * @param type The {@code ResidenceFunctionType} to check.
	 * @return The integer level of the function or {@code 0} if none is found.
	 */
	public int getFunctionLevel(ResidenceFunctionType type)
	{
		final ResidenceFunction func = getFunction(type);
		return func != null ? func.getLevel() : 0;
	}
	
	/**
	 * Retrieves the expiration time for a specific residence function.<br>
	 * It searches through all active functions to find a match for the given type.
	 * @param type The {@code ResidenceFunctionType} to check.
	 * @return The expiration timestamp as a {@code long}, or -1 if no such function exists.
	 */
	public long getFunctionExpiration(ResidenceFunctionType type)
	{
		ResidenceFunction function = null;
		for (ResidenceFunction func : _functions.values())
		{
			if (func.getTemplate().getType() == type)
			{
				function = func;
				break;
			}
		}
		
		return function != null ? function.getExpiration() : -1;
	}
	
	/**
	 * Retrieves all functions associated with this residence.
	 * @return a {@code Collection} of {@code ResidenceFunction} objects.
	 */
	public Collection<ResidenceFunction> getFunctions()
	{
		return _functions.values();
	}
	
	/**
	 * Compares this residence to the specified object.<br>
	 * Two residences are considered equal if they have the same {@code residenceId}.
	 * @param obj The object to compare with this one.
	 * @return {@code true} if the objects are equal, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof AbstractResidence) && (((AbstractResidence) obj).getResidenceId() == getResidenceId());
	}
	
	/**
	 * Returns a string representation of the residence.<br>
	 * This includes the name and the unique identifier.
	 * @return A {@code String} containing the name and ID.
	 */
	@Override
	public String toString()
	{
		return _name + " (" + _residenceId + ")";
	}
}
