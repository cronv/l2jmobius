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
package org.l2jmobius.gameserver.mechanics.events.returns;

/**
 * This class serves as the base for all event return mechanics.<br>
 * It provides common functionality for handling rewards or results triggered by game events.
 * @author UnAfraid
 */
public abstract class AbstractEventReturn
{
	private final boolean _override;
	private final boolean _abort;
	
	/**
	 * Creates a new instance of {@code AbstractEventReturn}.<br>
	 * This constructor sets the behavior for overriding and aborting events.
	 * @param override Set to {@code true} if this event should override others.
	 * @param abort Set to {@code true} if this event should stop further processing.
	 */
	public AbstractEventReturn(boolean override, boolean abort)
	{
		_override = override;
		_abort = abort;
	}
	
	/**
	 * Checks if this event is allowed to override other events.<br>
	 * Returns the current status of the {@code _override} flag.
	 * @return {@code true} if the event can override others, {@code false} otherwise.
	 */
	public boolean override()
	{
		return _override;
	}
	
	/**
	 * Checks if the event should be cancelled.<br>
	 * This method returns the value of the internal {@code _abort} flag.
	 * @return {@code true} if the event is aborted, {@code false} otherwise.
	 */
	public boolean abort()
	{
		return _abort;
	}
}
