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
package org.l2jmobius.gameserver.entity.actor.tasks.attackable;

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Quest;

/**
 * This task handles the notification logic when an {@code Attackable} entity is killed.<br>
 * It checks for relevant quest progress and notifies players based on specific kill conditions.
 * @author xban1x
 */
public class OnKillNotifyTask implements Runnable
{
	private final Attackable _attackable;
	private final Quest _quest;
	private final Player _killer;
	private final boolean _isSummon;
	
	/**
	 * Creates a new task to notify the system when an entity is killed.<br>
	 * This task handles specific logic for quest progression and summon status.
	 * @param attackable The {@code Attackable} entity that was defeated.
	 * @param quest The {@code Quest} associated with this kill event.
	 * @param killer The {@code Player} who performed the kill.
	 * @param isSummon A boolean indicating if the killed entity was a summon.
	 */
	public OnKillNotifyTask(Attackable attackable, Quest quest, Player killer, boolean isSummon)
	{
		_attackable = attackable;
		_quest = quest;
		_killer = killer;
		_isSummon = isSummon;
	}
	
	@Override
	public void run()
	{
		if ((_quest != null) && (_attackable != null) && (_killer != null))
		{
			_quest.onKill(_attackable, _killer, _isSummon);
		}
	}
}
