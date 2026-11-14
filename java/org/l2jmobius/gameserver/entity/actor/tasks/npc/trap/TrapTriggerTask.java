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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.instance.Trap;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class handles the logic for triggering a {@code Trap} instance.<br>
 * It is executed as a task to process interactions when an entity enters a trap.
 * @author Zoey76
 */
public class TrapTriggerTask implements Runnable
{
	private final Trap _trap;
	
	/**
	 * Creates a new task to handle a specific trap trigger.<br>
	 * This constructor initializes the task with the provided {@code Trap} object.
	 * @param trap The {@code Trap} instance associated with this task.
	 */
	public TrapTriggerTask(Trap trap)
	{
		_trap = trap;
	}
	
	@Override
	public void run()
	{
		try
		{
			final Skill trapSkill = _trap.getSkill();
			_trap.doCast(trapSkill);
			ThreadPool.schedule(new TrapUnsummonTask(_trap), trapSkill.getHitTime() + 300);
		}
		catch (Exception e)
		{
			_trap.unSummon();
		}
	}
}
