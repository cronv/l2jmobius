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
 * Handles the application of effects that modify a creature's maximum health points.<br>
 * This class manages how {@code Stat.MAX_HP} is adjusted by various skills or buffs.
 * @author NosBit
 */
public class MaxHp extends AbstractStatEffect
{
	private final boolean _heal;
	
	/**
	 * Creates a new {@code MaxHp} effect.<br>
	 * This constructor initializes the effect using the provided stats.<br>
	 * It sets the target stat to {@code Stat.MAX_HP}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public MaxHp(StatSet params)
	{
		super(params, Stat.MAX_HP);
		
		_heal = params.getBoolean("heal", false);
	}
	
	/**
	 * Applies a continuous instant effect to a target.<br>
	 * This method handles the logic for healing or modifying health based on the current mode.<br>
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
				if (!effected.isHpBlocked())
				{
					switch (_mode)
					{
						case DIFF:
						{
							effected.setCurrentHp(effected.getCurrentHp() + _amount);
							break;
						}
						case PER:
						{
							effected.setCurrentHp(effected.getCurrentHp() + (effected.getMaxHp() * (_amount / 100)));
							break;
						}
					}
				}
			}, 100);
		}
	}
}
