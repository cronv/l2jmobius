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
package org.l2jmobius.gameserver.entity.actor.request;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * This class serves as the base for all request objects handled by actors.<br>
 * It provides common functionality and structure for processing asynchronous tasks.<br>
 * Subclasses should implement specific logic to handle unique game actions.
 * @author UnAfraid
 */
public abstract class AbstractRequest
{
	private final Player _player;
	private volatile long _timestamp = 0;
	private volatile boolean _isProcessing;
	private ScheduledFuture<?> _timeOutTask;
	
	/**
	 * Creates a new instance of an {@code AbstractRequest}.<br>
	 * This constructor assigns the provided {@code Player} to the request.<br>
	 * The {@code player} parameter must not be {@code null}.
	 * @param player The {@code Player} who initiated this request.
	 */
	public AbstractRequest(Player player)
	{
		Objects.requireNonNull(player);
		_player = player;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this request.<br>
	 * This method returns the owner of the current action.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Returns the time when this request was created.<br>
	 * The value is stored as a {@code long}.
	 * @return The timestamp of the request.
	 */
	public long getTimestamp()
	{
		return _timestamp;
	}
	
	/**
	 * Sets the internal time for this request.<br>
	 * The value is stored as a {@code long}.
	 * @param timestamp The new time to assign.
	 */
	public void setTimestamp(long timestamp)
	{
		_timestamp = timestamp;
	}
	
	/**
	 * Schedules a timeout task for this request.<br>
	 * The {@code onTimeout} method will be called after the specified time.
	 * @param delay The amount of time to wait before triggering the timeout.
	 */
	public void scheduleTimeout(long delay)
	{
		_timeOutTask = ThreadPool.schedule(this::onTimeout, delay);
	}
	
	/**
	 * Checks if the request has currently timed out.<br>
	 * It returns {@code true} if a timeout task is active.<br>
	 * It returns {@code false} if no task exists or it is finished.
	 * @return {@code true} if the request is still within its timeout period, {@code false} otherwise.
	 */
	public boolean isTimeout()
	{
		return (_timeOutTask != null) && !_timeOutTask.isDone();
	}
	
	/**
	 * Cancels the scheduled timeout task for this request.<br>
	 * This method checks if a {@code _timeOutTask} exists.<br>
	 * If it exists, it calls {@code cancel(false)} on that task.
	 */
	public void cancelTimeout()
	{
		if (_timeOutTask != null)
		{
			_timeOutTask.cancel(false);
		}
	}
	
	/**
	 * Checks if the request is currently being processed.<br>
	 * Returns {@code true} if processing is active.<br>
	 * Returns {@code false} otherwise.
	 * @return The current processing status of this request.
	 */
	public boolean isProcessing()
	{
		return _isProcessing;
	}
	
	/**
	 * Updates the processing status of this request.<br>
	 * This method sets the internal {@code _isProcessing} flag.
	 * @param isProcessing The new status to set.
	 * @return The value that was assigned to the status.
	 */
	public boolean setProcessing(boolean isProcessing)
	{
		return _isProcessing = isProcessing;
	}
	
	/**
	 * Checks if this request can be processed alongside another request.<br>
	 * This method currently always returns {@code true}.
	 * @param request The other {@code AbstractRequest} to check against.
	 * @return {@code true} if the requests are compatible, otherwise {@code false}.
	 */
	public boolean canWorkWith(AbstractRequest request)
	{
		return true;
	}
	
	/**
	 * Checks if the current request involves an item.<br>
	 * This method returns {@code false} by default for all requests.
	 * @return {@code true} if the request is for an item, otherwise {@code false}.
	 */
	public boolean isItemRequest()
	{
		return false;
	}
	
	public abstract boolean isUsing(int objectId);
	
	/**
	 * This method is called when a request takes too long to complete.<br>
	 * It handles the logic for cleaning up or canceling the task.<br>
	 * Use this to ensure the system stays responsive.
	 */
	public void onTimeout()
	{
	}
}
