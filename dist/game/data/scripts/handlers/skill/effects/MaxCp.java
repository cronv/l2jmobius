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
package handlers.skill.effects;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the modification of a creature's maximum CP stat.<br>
 * This effect allows for dynamic changes to the {@code Stat.MAX_CP} value during gameplay.
 * @author Nik
 */
public class MaxCp extends AbstractStatEffect
{
	private final boolean _heal;
	
	/**
	 * Creates a new {@code MaxCp} effect.<br>
	 * This constructor initializes the effect using the provided {@code StatSet}.<br>
	 * It sets the target stat to {@code Stat.MAX_CP}.
	 * @param params The set of parameters used to configure the effect.
	 */
	public MaxCp(StatSet params)
	{
		super(params, Stat.MAX_CP);
		
		_heal = params.getBoolean("heal", false);
	}
	
	/**
	 * Applies a continuous instant effect to a target creature.<br>
	 * This method handles the logic for updating CP values based on the defined mode.<br>
	 * It schedules the update to occur after a short delay.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void continuousInstant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (_heal)
		{
			ThreadPool.schedule(() ->
			{
				switch (_mode)
				{
					case DIFF:
					{
						effected.setCurrentCp(effected.getCurrentCp() + _amount);
						break;
					}
					case PER:
					{
						effected.setCurrentCp(effected.getCurrentCp() + (effected.getMaxCp() * (_amount / 100)));
						break;
					}
				}
			}, 100);
		}
	}
}
