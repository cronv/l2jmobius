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
package org.l2jmobius.gameserver.entity.actor.tasks.npc.trap;

import org.l2jmobius.gameserver.entity.actor.instance.Trap;

/**
 * Handles the logic for unsummoning a {@code Trap} entity.<br>
 * This task is executed to remove the trap from the game world.
 * @author Zoey76
 */
public class TrapUnsummonTask implements Runnable
{
	private final Trap _trap;
	
	/**
	 * Creates a new task to unsummon a specific trap.<br>
	 * This constructor initializes the task with the provided {@code Trap} object.
	 * @param trap The {@code Trap} instance to be processed by this task.
	 */
	public TrapUnsummonTask(Trap trap)
	{
		_trap = trap;
	}
	
	@Override
	public void run()
	{
		_trap.unSummon();
	}
}
