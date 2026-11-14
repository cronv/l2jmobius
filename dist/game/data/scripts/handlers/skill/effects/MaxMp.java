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
 * Handles the modification of a creature's maximum MP stat.<br>
 * This effect applies changes to the {@code Stat} value associated with maximum mana.
 * @author Sdw
 */
public class MaxMp extends AbstractStatEffect
{
	private final boolean _heal;
	
	/**
	 * Creates a new {@code MaxMp} effect.<br>
	 * This constructor initializes the effect using the provided statistics.<br>
	 * It sets the target stat to {@code Stat.MAX_MP}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public MaxMp(StatSet params)
	{
		super(params, Stat.MAX_MP);
		
		_heal = params.getBoolean("heal", false);
	}
	
	/**
	 * Applies a continuous instant effect to a target creature.<br>
	 * This method handles the logic for healing or modifying MP based on the defined mode.<br>
	 * The action is scheduled to occur after a short delay.
	 * @param effector The {@code Creature} who performs the skill.
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
						effected.setCurrentMp(effected.getCurrentMp() + _amount);
						break;
					}
					case PER:
					{
						effected.setCurrentMp(effected.getCurrentMp() + (effected.getMaxMp() * (_amount / 100)));
						break;
					}
				}
			}, 100);
		}
	}
}
