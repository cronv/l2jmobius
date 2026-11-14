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

import org.l2jmobius.gameserver.entity.actor.enums.player.DailyMissionStatus;

/**
 * This class represents a player's progress and status for daily missions.<br>
 * It stores the specific data required to track individual mission completion.<br>
 * Use this class to manage {@code DailyMissionStatus} for each player.
 * @author UnAfraid
 */
public class DailyMissionPlayerEntry
{
	private final int _objectId;
	private final int _rewardId;
	private DailyMissionStatus _status = DailyMissionStatus.NOT_AVAILABLE;
	private int _progress;
	private long _lastCompleted;
	private boolean _recentlyCompleted;
	
	/**
	 * Creates a new entry for a player's daily mission.<br>
	 * This constructor initializes the basic requirements for a mission.
	 * @param objectId The unique identifier for the mission object.
	 * @param rewardId The identifier for the reward given upon completion.
	 */
	public DailyMissionPlayerEntry(int objectId, int rewardId)
	{
		_objectId = objectId;
		_rewardId = rewardId;
	}
	
	/**
	 * Creates a new {@code DailyMissionPlayerEntry} with specific initial values.<br>
	 * This constructor sets the mission status, current progress, and completion timestamp.
	 * @param objectId The unique identifier for the mission object.
	 * @param rewardId The identifier for the reward granted upon completion.
	 * @param status The integer value representing the {@code DailyMissionStatus}.
	 * @param progress The current amount of progress made toward the mission.
	 * @param lastCompleted The timestamp of when the mission was last finished.
	 */
	public DailyMissionPlayerEntry(int objectId, int rewardId, int status, int progress, long lastCompleted)
	{
		this(objectId, rewardId);
		_status = DailyMissionStatus.valueOf(status);
		_progress = progress;
		_lastCompleted = lastCompleted;
	}
	
	/**
	 * Retrieves the unique identifier for this entry.<br>
	 * This ID is used to identify the specific object associated with the mission.
	 * @return The {@code int} value of the object ID.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Retrieves the unique identifier for the reward.<br>
	 * This ID corresponds to the item or prize granted upon completion.
	 * @return The {@code int} value of the reward ID.
	 */
	public int getRewardId()
	{
		return _rewardId;
	}
	
	/**
	 * Retrieves the current status of the daily mission.<br>
	 * This value indicates whether the mission is available, in progress, or completed.
	 * @return The {@code DailyMissionStatus} of this entry.
	 */
	public DailyMissionStatus getStatus()
	{
		return _status;
	}
	
	/**
	 * Updates the current status of the daily mission.<br>
	 * This method sets the {@code _status} field to the provided value.
	 * @param status The new {@code DailyMissionStatus} to assign.
	 */
	public void setStatus(DailyMissionStatus status)
	{
		_status = status;
	}
	
	/**
	 * Returns the current progress of the daily mission.<br>
	 * The value represents how much of the task has been completed.
	 * @return The current progress as an {@code int}.
	 */
	public int getProgress()
	{
		return _progress;
	}
	
	/**
	 * Updates the current progress of a daily mission.<br>
	 * This method sets the internal progress value to the provided amount.
	 * @param progress The new progress value to set.
	 */
	public void setProgress(int progress)
	{
		_progress = progress;
	}
	
	/**
	 * Increases the current progress of the daily mission by 1.<br>
	 * Updates the internal {@code _progress} value.
	 * @return The new progress value after the increase.
	 */
	public int increaseProgress()
	{
		_progress++;
		return _progress;
	}
	
	/**
	 * Returns the timestamp of when the mission was last finished.<br>
	 * This value is stored as a {@code long}.
	 * @return The last completion time.
	 */
	public long getLastCompleted()
	{
		return _lastCompleted;
	}
	
	/**
	 * Updates the timestamp of when the mission was last finished.<br>
	 * This value is stored as a {@code long}.
	 * @param lastCompleted The new timestamp to set.
	 */
	public void setLastCompleted(long lastCompleted)
	{
		_lastCompleted = lastCompleted;
	}
	
	/**
	 * Checks if the daily mission was finished recently.<br>
	 * This helps determine if a reward should be hidden or shown.
	 * @return {@code true} if completed recently, {@code false} otherwise.
	 */
	public boolean isRecentlyCompleted()
	{
		return _recentlyCompleted;
	}
	
	/**
	 * Updates the completion status of a daily mission.<br>
	 * This method sets whether the mission was finished recently.
	 * @param recentlyCompleted The new status to set as {@code true} or {@code false}.
	 */
	public void setRecentlyCompleted(boolean recentlyCompleted)
	{
		_recentlyCompleted = recentlyCompleted;
	}
}
