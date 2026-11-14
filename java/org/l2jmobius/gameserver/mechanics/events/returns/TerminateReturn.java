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
 * This class represents an event that signals the termination of a return process.<br>
 * It is used to stop ongoing return mechanics within the game engine.
 * @author UnAfraid
 */
public class TerminateReturn extends AbstractEventReturn
{
	private final boolean _terminate;
	
	/**
	 * Creates a new instance of {@code TerminateReturn}.<br>
	 * This constructor initializes the termination settings for an event.
	 * @param terminate Set to {@code true} if the process should be terminated.
	 * @param override Set to {@code true} if this action should override existing ones.
	 * @param abort Set to {@code true} if the action should trigger an immediate abort.
	 */
	public TerminateReturn(boolean terminate, boolean override, boolean abort)
	{
		super(override, abort);
		_terminate = terminate;
	}
	
	/**
	 * Checks if the return process should be stopped.<br>
	 * This method retrieves the value of the {@code _terminate} flag.
	 * @return {@code true} if the process is terminated, {@code false} otherwise.
	 */
	public boolean terminate()
	{
		return _terminate;
	}
}
