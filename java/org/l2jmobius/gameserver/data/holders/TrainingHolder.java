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
package org.l2jmobius.gameserver.data.holders;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.gameserver.config.TrainingCampConfig;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * This class serves as a data container for training camp information.<br>
 * It stores the necessary state and configuration for players participating in training sessions.
 * @author Sdw
 */
public class TrainingHolder implements Serializable
{
	private final int _objectId;
	private final int _classIndex;
	private final int _level;
	private final long _startTime;
	private long _endTime = -1;
	private static final long TRAINING_DIVIDER = TimeUnit.SECONDS.toMinutes(TrainingCampConfig.TRAINING_CAMP_MAX_DURATION);
	
	/**
	 * Creates a new instance of {@code TrainingHolder}.<br>
	 * This constructor initializes the training data for a specific object.
	 * @param objectId The unique identifier for the object.
	 * @param classIndex The index representing the character class.
	 * @param level The level of the character during training.
	 * @param startTime The timestamp when the training began.
	 * @param endTime The timestamp when the training is scheduled to end.
	 */
	public TrainingHolder(int objectId, int classIndex, int level, long startTime, long endTime)
	{
		_objectId = objectId;
		_classIndex = classIndex;
		_level = level;
		_startTime = startTime;
		_endTime = endTime;
	}
	
	/**
	 * Retrieves the end time of the training session.<br>
	 * Returns the value stored in {@code _endTime}.
	 * @return The end time as a {@code long}.
	 */
	public long getEndTime()
	{
		return _endTime;
	}
	
	/**
	 * Sets the end time for the training session.<br>
	 * This updates the {@code _endTime} field with a new value.
	 * @param endTime The new end time as a {@code long}.
	 */
	public void setEndTime(long endTime)
	{
		_endTime = endTime;
	}
	
	/**
	 * Retrieves the unique identifier for this training object.<br>
	 * This value is used to identify specific instances in the system.
	 * @return The {@code int} ID of the object.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Retrieves the index of the class for this training session.<br>
	 * This value is stored in the {@code _classIndex} field.
	 * @return The integer index representing the class.
	 */
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	/**
	 * Retrieves the current level of the training holder.<br>
	 * This value is stored during the initialization of the {@code TrainingHolder}.
	 * @return The integer value representing the level.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the time when the training session began.<br>
	 * This value is stored as a {@code long}.
	 * @return The start time of the training.
	 */
	public long getStartTime()
	{
		return _startTime;
	}
	
	/**
	 * Checks if the training session is currently active.<br>
	 * It returns {@code true} if the end time has not been set.<br>
	 * It returns {@code false} if the training has finished.
	 * @return {@code true} if the holder is still in a training state, otherwise {@code false}.
	 */
	public boolean isTraining()
	{
		return _endTime == -1;
	}
	
	/**
	 * Checks if the training session is valid for a specific player.<br>
	 * It verifies that training is enabled in the configuration.<br>
	 * It also ensures the {@code Player} matches the required object ID and class index.
	 * @param player The {@code Player} to validate.
	 * @return {@code true} if the session is valid for the player, otherwise {@code false}.
	 */
	public boolean isValid(Player player)
	{
		return TrainingCampConfig.TRAINING_CAMP_ENABLE && (player.getObjectId() == _objectId) && (player.getClassIndex() == _classIndex);
	}
	
	/**
	 * Calculates the total time passed since training started.<br>
	 * It returns the duration in seconds.
	 * @return The elapsed time in seconds as a {@code long}.
	 */
	public long getElapsedTime()
	{
		return TimeUnit.SECONDS.convert(System.currentTimeMillis() - _startTime, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Calculates the time left until training is finished.<br>
	 * This value is based on the maximum duration defined in {@code TrainingCampConfig}.<br>
	 * It subtracts the elapsed time from the total allowed limit.
	 * @return The remaining training time in minutes as a {@code long}.
	 */
	public long getRemainingTime()
	{
		return TimeUnit.SECONDS.toMinutes(TrainingCampConfig.TRAINING_CAMP_MAX_DURATION - getElapsedTime());
	}
	
	/**
	 * Calculates the training time based on the provided {@code TimeUnit}.<br>
	 * This method returns the smaller value between the maximum allowed duration and the actual elapsed time.
	 * @param unit The {@code TimeUnit} to use for the conversion.
	 * @return The calculated training time as a {@code long}.
	 */
	public long getTrainingTime(TimeUnit unit)
	{
		return Math.min(unit.convert(TrainingCampConfig.TRAINING_CAMP_MAX_DURATION, TimeUnit.SECONDS), unit.convert(_endTime - _startTime, TimeUnit.MILLISECONDS));
	}
	
	/**
	 * Returns the constant value used to divide training time.<br>
	 * This value is based on the maximum duration defined in {@code TrainingCampConfig}.
	 * @return The training divider as a {@code long}.
	 */
	public static long getTrainingDivider()
	{
		return TRAINING_DIVIDER;
	}
}
