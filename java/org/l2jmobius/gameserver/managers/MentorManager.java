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
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.Mentee;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;

/**
 * Manages the mentor and mentee relationships between players.<br>
 * This class handles the registration, tracking, and lifecycle of {@code Mentee} objects.
 * @author UnAfraid
 */
public class MentorManager
{
	private static final Logger LOGGER = Logger.getLogger(MentorManager.class.getName());
	
	private final Map<Integer, Map<Integer, Mentee>> _menteeData = new ConcurrentHashMap<>();
	private final Map<Integer, Mentee> _mentors = new ConcurrentHashMap<>();
	
	/**
	 * Constructs a new {@code MentorManager} instance.<br>
	 * This constructor initializes the manager by loading data from the database.
	 */
	protected MentorManager()
	{
		load();
	}
	
	/**
	 * Loads mentor and mentee data from the database.<br>
	 * It populates the internal maps using the {@code character_mentees} table.<br>
	 * Any errors during the loading process are logged as a warning.
	 */
	private void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement statement = con.createStatement();
			ResultSet rset = statement.executeQuery("SELECT * FROM character_mentees"))
		{
			while (rset.next())
			{
				addMentor(rset.getInt("mentorId"), rset.getInt("charId"));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Removes a specific mentee from a mentor's list in the database.<br>
	 * This method deletes the record where both {@code mentorId} and {@code menteeId} match.<br>
	 * If an error occurs during the deletion, it logs a warning message.
	 * @param mentorId The unique identifier of the mentor.
	 * @param menteeId The unique identifier of the mentee to be removed.
	 */
	public void deleteMentee(int mentorId, int menteeId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_mentees WHERE mentorId = ? AND charId = ?"))
		{
			statement.setInt(1, mentorId);
			statement.setInt(2, menteeId);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Removes a specific mentoring relationship from the database.<br>
	 * This method deletes the record for the given mentor and mentee.<br>
	 * It also updates the internal cache by calling {@code removeMentor}.
	 * @param mentorId The unique identifier of the mentor.
	 * @param menteeId The unique identifier of the mentee.
	 */
	public void deleteMentor(int mentorId, int menteeId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_mentees WHERE mentorId = ? AND charId = ?"))
		{
			statement.setInt(1, mentorId);
			statement.setInt(2, menteeId);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		finally
		{
			removeMentor(mentorId, menteeId);
		}
	}
	
	/**
	 * Checks if a specific object is currently acting as a mentor.<br>
	 * This method looks up the {@code objectId} in the internal mentor data map.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if the object is a mentor, otherwise {@code false}.
	 */
	public boolean isMentor(int objectId)
	{
		return _menteeData.containsKey(objectId);
	}
	
	/**
	 * Checks if a specific object is currently registered as a mentee.<br>
	 * It searches through all mentor data to find the given ID.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if the object is a mentee, otherwise {@code false}.
	 */
	public boolean isMentee(int objectId)
	{
		for (Map<Integer, Mentee> map : _menteeData.values())
		{
			if (map.containsKey(objectId))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves the full mapping of mentor and mentee data.<br>
	 * The outer map uses the mentor ID as the key.<br>
	 * The inner map uses the mentee ID as the key.
	 * @return a {@code Map} containing all mentoring relationships.
	 */
	public Map<Integer, Map<Integer, Mentee>> getMentorData()
	{
		return _menteeData;
	}
	
	/**
	 * Removes all active mentoring buffs from a specific player.<br>
	 * This method checks the {@code Player} effect list for any skills marked as mentoring.<br>
	 * It stops those effects if they are found.
	 * @param player The {@code Player} object to clear buffs from.
	 */
	public void cancelAllMentoringBuffs(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		for (BuffInfo info : player.getEffectList().getEffects())
		{
			if (info.getSkill().isMentoring())
			{
				player.stopSkillEffects(info.getSkill());
			}
		}
	}
	
	/**
	 * Sets a penalty for a specific mentor.<br>
	 * This method updates the expiration time of the mentor's penalty in their variables.
	 * @param mentorId The unique identifier of the mentor.
	 * @param penalty The amount of time to add to the current system time.
	 */
	public void setPenalty(int mentorId, long penalty)
	{
		final Player player = World.getPlayer(mentorId);
		final PlayerVariables vars = player != null ? player.getVariables() : new PlayerVariables(mentorId);
		vars.set("Mentor-Penalty-" + mentorId, String.valueOf(System.currentTimeMillis() + penalty));
	}
	
	/**
	 * Retrieves the current penalty value for a specific mentor.<br>
	 * This method looks up the value from the player's variables.<br>
	 * It returns {@code 0} if no penalty is found.
	 * @param mentorId The unique identifier of the mentor.
	 * @return The penalty amount as a {@code long}.
	 */
	public long getMentorPenalty(int mentorId)
	{
		final Player player = World.getPlayer(mentorId);
		final PlayerVariables vars = player != null ? player.getVariables() : new PlayerVariables(mentorId);
		return vars.getLong("Mentor-Penalty-" + mentorId, 0);
	}
	
	/**
	 * Assigns a mentee to a specific mentor.<br>
	 * This method adds the {@code menteeId} to the list of mentees for {@code mentorId}.<br>
	 * If the mentee already exists, their data is reloaded.<br>
	 * Otherwise, a new {@code Mentee} object is created and stored.
	 * @param mentorId The unique identifier of the mentor.
	 * @param menteeId The unique identifier of the mentee.
	 */
	public void addMentor(int mentorId, int menteeId)
	{
		final Map<Integer, Mentee> mentees = _menteeData.computeIfAbsent(mentorId, _ -> new ConcurrentHashMap<>());
		final Mentee mentee = mentees.get(menteeId);
		if (mentee != null)
		{
			mentee.load(); // Just reloading data if is already there.
		}
		else
		{
			mentees.put(menteeId, new Mentee(menteeId));
		}
	}
	
	/**
	 * Removes a specific mentee from a mentor's list.<br>
	 * If the mentor has no more mentees, they are removed from the system.
	 * @param mentorId The unique identifier of the mentor.
	 * @param menteeId The unique identifier of the mentee.
	 */
	public void removeMentor(int mentorId, int menteeId)
	{
		final Map<Integer, Mentee> mentees = _menteeData.get(mentorId);
		if (mentees != null)
		{
			mentees.remove(menteeId);
			if (mentees.isEmpty())
			{
				_menteeData.remove(mentorId);
				_mentors.remove(mentorId);
			}
		}
	}
	
	/**
	 * Finds the mentor associated with a specific mentee.<br>
	 * It searches through all active mentoring data.<br>
	 * Returns {@code null} if no mentor is found.
	 * @param menteeId The unique identifier of the mentee to look up.
	 * @return The {@code Mentee} object representing the mentor, or {@code null}.
	 */
	public Mentee getMentor(int menteeId)
	{
		for (Entry<Integer, Map<Integer, Mentee>> map : _menteeData.entrySet())
		{
			if (map.getValue().containsKey(menteeId))
			{
				return _mentors.computeIfAbsent(map.getKey(), Mentee::new);
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves all {@code Mentee} objects associated with a specific mentor.<br>
	 * This method looks up the data using the provided {@code mentorId}.<br>
	 * It returns an empty list if no mentees are found.
	 * @param mentorId The unique identifier of the mentor.
	 * @return A {@code Collection} of all mentees belonging to the mentor.
	 */
	public Collection<Mentee> getMentees(int mentorId)
	{
		final Map<Integer, Mentee> mentees = _menteeData.get(mentorId);
		return (mentees != null) ? mentees.values() : Collections.emptyList();
	}
	
	/**
	 * Retrieves a specific {@code Mentee} based on the mentor and mentee IDs.<br>
	 * This method looks up the data in the internal mentoring records.<br>
	 * It returns {@code null} if no such relationship exists.
	 * @param mentorId The unique identifier of the mentor.
	 * @param menteeId The unique identifier of the mentee.
	 * @return The {@code Mentee} object, or {@code null} if not found.
	 */
	public Mentee getMentee(int mentorId, int menteeId)
	{
		return _menteeData.getOrDefault(mentorId, Collections.emptyMap()).get(menteeId);
	}
	
	/**
	 * Checks if all mentees of a specific mentor are currently offline.<br>
	 * This method ignores the status of the mentee specified by {@code menteeId}.<br>
	 * It returns {@code true} if no other mentees are online.
	 * @param menteorId The unique identifier for the mentor.
	 * @param menteeId The unique identifier for the specific mentee to exclude from the check.
	 * @return {@code true} if all other mentees are offline, otherwise {@code false}.
	 */
	public boolean isAllMenteesOffline(int menteorId, int menteeId)
	{
		boolean isAllMenteesOffline = true;
		for (Mentee men : getMentees(menteorId))
		{
			if (men.isOnline() && (men.getObjectId() != menteeId))
			{
				isAllMenteesOffline = false;
				break;
			}
		}
		
		return isAllMenteesOffline;
	}
	
	/**
	 * Checks if a mentor has any mentees currently online.<br>
	 * It iterates through all mentees associated with the given ID.<br>
	 * Returns {@code true} if at least one mentee is online.
	 * @param menteorId The unique identifier of the mentor to check.
	 * @return {@code true} if there are online mentees, otherwise {@code false}.
	 */
	public boolean hasOnlineMentees(int menteorId)
	{
		for (Mentee mentee : getMentees(menteorId))
		{
			if ((mentee != null) && mentee.isOnline())
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Provides access to the singleton instance of {@code MentorManager}.<br>
	 * Use this method to get the global manager for mentor and mentee data.
	 * @return The single instance of {@code MentorManager}.
	 */
	public static MentorManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MentorManager INSTANCE = new MentorManager();
	}
}
