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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.EnchantChallengePointData;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * This class manages the challenge points associated with a {@code Player}.<br>
 * It handles the persistence and retrieval of these points from the database.<br>
 * Use this class to track progress for specific game mechanics like enchanting.
 * @author Serenitty
 */
public class ChallengePoint
{
	private static final Logger LOGGER = Logger.getLogger(ChallengePoint.class.getName());
	
	// Character enchant challenge points.
	private static final String INSERT_CHALLENGE_POINTS = "REPLACE INTO enchant_challenge_points (`charId`, `groupId`, `points`) VALUES (?, ?, ?)";
	private static final String RESTORE_CHALLENGE_POINTS = "SELECT * FROM enchant_challenge_points WHERE charId=?";
	private static final String INSERT_CHALLENGE_POINTS_RECHARGES = "REPLACE INTO enchant_challenge_points_recharges (`charId`, `groupId`, `optionIndex`, `count`) VALUES (?, ?, ?, ?)";
	private static final String RESTORE_CHALLENGE_POINTS_RECHARGES = "SELECT * FROM enchant_challenge_points_recharges WHERE charId=?";
	
	private final Player _owner;
	private int _nowGroup;
	private int _nowPoint;
	private final Map<Integer, Integer> _challengePoints = new HashMap<>();
	private final Map<Integer, Map<Integer, Integer>> _challengePointsRecharges = new HashMap<>();
	private final int[] _challengePointsPendingRecharge =
	{
		-1,
		-1,
	};
	
	/**
	 * Creates a new {@code ChallengePoint} instance for a specific player.<br>
	 * This constructor initializes the owner and sets default values for points.
	 * @param owner The {@code Player} who owns these challenge points.
	 */
	public ChallengePoint(Player owner)
	{
		_owner = owner;
		_nowGroup = 0;
		_nowPoint = 0;
	}
	
	/**
	 * Saves the current challenge points to the database.<br>
	 * This method updates both standard points and recharge values for the {@code Player}.<br>
	 * It uses batch processing to improve performance during the save operation.
	 */
	public void storeChallengePoints()
	{
		// LOGGER.info("Storing Challenge Points for " + _owner);
		
		if (_challengePoints.isEmpty())
		{
			return;
		}
		
		try (Connection conn = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps1 = conn.prepareStatement(INSERT_CHALLENGE_POINTS))
			{
				for (Entry<Integer, Integer> entry : _challengePoints.entrySet())
				{
					ps1.setInt(1, _owner.getObjectId());
					ps1.setInt(2, entry.getKey());
					ps1.setInt(3, entry.getValue());
					ps1.addBatch();
				}
				
				ps1.executeBatch();
			}
			
			try (PreparedStatement ps2 = conn.prepareStatement(INSERT_CHALLENGE_POINTS_RECHARGES))
			{
				for (Entry<Integer, Map<Integer, Integer>> entry : _challengePointsRecharges.entrySet())
				{
					for (Entry<Integer, Integer> entry2 : entry.getValue().entrySet())
					{
						ps2.setInt(1, _owner.getObjectId());
						ps2.setInt(2, entry.getKey());
						ps2.setInt(3, entry2.getKey());
						ps2.setInt(4, entry2.getValue());
						ps2.addBatch();
					}
				}
				
				ps2.executeBatch();
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Could not store Challenge Points for " + _owner + " " + e);
		}
	}
	
	/**
	 * Loads the challenge points and recharge data from the database.<br>
	 * This method clears existing local maps and populates them with values<br>
	 * associated with the {@code Player} owner.
	 */
	public void restoreChallengePoints()
	{
		_challengePoints.clear();
		try (Connection con = DatabaseFactory.getConnection())
		{
			final int objectId = _owner.getObjectId();
			try (PreparedStatement ps = con.prepareStatement(RESTORE_CHALLENGE_POINTS))
			{
				ps.setInt(1, objectId);
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						final int groupId = rs.getInt("groupId");
						final int points = rs.getInt("points");
						_challengePoints.put(groupId, points);
					}
				}
			}
			
			_challengePointsRecharges.clear();
			try (PreparedStatement ps = con.prepareStatement(RESTORE_CHALLENGE_POINTS_RECHARGES))
			{
				ps.setInt(1, objectId);
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						final int groupId = rs.getInt("groupId");
						final int optionIndex = rs.getInt("optionIndex");
						final int count = rs.getInt("count");
						Map<Integer, Integer> options = _challengePointsRecharges.get(groupId);
						if (options == null)
						{
							options = new HashMap<>();
							_challengePointsRecharges.put(groupId, options);
						}
						
						options.put(optionIndex, count);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Could not restore Challenge Points for " + _owner + " " + e);
		}
		
		// LOGGER.info("Restored Challenge Points recharges for " + _owner);
		// LOGGER.info("Restored Challenge Points for " + _owner);
	}
	
	/**
	 * Retrieves the current number of challenge points.<br>
	 * This method resets the internal {@code _nowPoint} value to {@code 0} after retrieval.
	 * @return The number of points retrieved before resetting.
	 */
	public int getNowPoint()
	{
		final int nowPoint = _nowPoint;
		_nowPoint = 0;
		return nowPoint;
	}
	
	/**
	 * Retrieves the current group value.<br>
	 * This method resets {@code _nowGroup} to {@code 0} after retrieval.
	 * @return The previous value of {@code _nowGroup}.
	 */
	public int getNowGroup()
	{
		final int nowGroup = _nowGroup;
		_nowGroup = 0;
		return nowGroup;
	}
	
	/**
	 * Updates the current group identifier.<br>
	 * This method sets the value of {@code _nowGroup}.
	 * @param val The new group value to assign.
	 */
	public void setNowGroup(int val)
	{
		_nowGroup = val;
	}
	
	/**
	 * Updates the current number of challenge points.<br>
	 * This method sets the {@code _nowPoint} field to a new value.
	 * @param val The new point value to set.
	 */
	public void setNowPoint(int val)
	{
		_nowPoint = val;
	}
	
	/**
	 * Retrieves the map of all challenge points.<br>
	 * The keys represent group IDs and the values represent point amounts.
	 * @return a {@code Map<Integer, Integer>} containing the challenge points.
	 */
	public Map<Integer, Integer> getChallengePoints()
	{
		return _challengePoints;
	}
	
	/**
	 * Retrieves the number of recharge points for a specific group and option.<br>
	 * It returns {@code 0} if no recharges are found or the group does not exist.
	 * @param groupId The unique identifier for the challenge point group.
	 * @param optionIndex The index of the specific recharge option.
	 * @return The total count of available recharge points.
	 */
	public int getChallengePointsRecharges(int groupId, int optionIndex)
	{
		final Map<Integer, Integer> options = _challengePointsRecharges.get(groupId);
		if (options != null)
		{
			return options.getOrDefault(optionIndex, 0);
		}
		
		return 0;
	}
	
	/**
	 * Adds a specific amount of recharge points to a group and option.<br>
	 * This method updates the internal map for the given {@code groupId}.<br>
	 * If the {@code optionIndex} does not exist, it is created with the provided {@code amount}.
	 * @param groupId The unique identifier for the challenge point group.
	 * @param optionIndex The specific index of the recharge option.
	 * @param amount The number of points to add to the current total.
	 */
	public void addChallengePointsRecharge(int groupId, int optionIndex, int amount)
	{
		Map<Integer, Integer> options = _challengePointsRecharges.get(groupId);
		if (options == null)
		{
			options = new HashMap<>();
			_challengePointsRecharges.put(groupId, options);
		}
		
		options.compute(optionIndex, (_, v) -> v == null ? amount : v + amount);
	}
	
	/**
	 * Updates the pending recharge data for a specific group and option.<br>
	 * This method stores the provided values into the internal pending recharge array.
	 * @param groupId The unique identifier for the challenge point group.
	 * @param optionIndex The index of the specific option to be recharged.
	 */
	public void setChallengePointsPendingRecharge(int groupId, int optionIndex)
	{
		_challengePointsPendingRecharge[0] = groupId;
		_challengePointsPendingRecharge[1] = optionIndex;
	}
	
	/**
	 * Retrieves the list of pending challenge point recharges.<br>
	 * This method returns an array containing the data for all pending recharges.
	 * @return an {@code int[]} array representing the pending recharge values.
	 */
	public int[] getChallengePointsPendingRecharge()
	{
		return _challengePointsPendingRecharge;
	}
	
	/**
	 * This method prepares the challenge point data for a player.<br>
	 * It creates an array of {@code ChallengePointInfoHolder} objects based on the current points.<br>
	 * Each object contains the group ID and recharge counts for different options.
	 * @return An array of {@code ChallengePointInfoHolder} containing all initialized challenge point information.
	 */
	public ChallengePointInfoHolder[] initializeChallengePoints()
	{
		final Map<Integer, Integer> challengePoints = getChallengePoints();
		final ChallengePointInfoHolder[] info = new ChallengePointInfoHolder[challengePoints.size()];
		int i = 0;
		for (Entry<Integer, Integer> entry : challengePoints.entrySet())
		{
			final int groupId = entry.getKey();
			info[i] = new ChallengePointInfoHolder(groupId, entry.getValue(), //
				getChallengePointsRecharges(groupId, 0), //
				getChallengePointsRecharges(groupId, 1), //
				getChallengePointsRecharges(groupId, 2), //
				getChallengePointsRecharges(groupId, 3), //
				getChallengePointsRecharges(groupId, 4), //
				getChallengePointsRecharges(groupId, 5));
			i++;
		}
		
		return info;
	}
	
	/**
	 * Checks if adding a specific amount of points is allowed.<br>
	 * This method verifies that the new total does not exceed the maximum limit.
	 * @param categoryId The unique identifier for the point category.
	 * @param points The number of points to be added.
	 * @return {@code true} if the addition is valid, {@code false} otherwise.
	 */
	public boolean canAddPoints(int categoryId, int points)
	{
		final int totalPoints = _challengePoints.getOrDefault(categoryId, 0) + points;
		final int maxPoints = EnchantChallengePointData.getInstance().getMaxPoints();
		return maxPoints > totalPoints;
	}
}
