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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.DispelSlotType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for resisting dispel effects based on a specific category.<br>
 * It allows {@code Creature} objects to ignore certain types of dispels defined by {@code DispelSlotType}.<br>
 * It works by checking if an effect belongs to a restricted category before applying it.
 * @author Sdw
 */
public class ResistDispelByCategory extends AbstractEffect
{
	private final DispelSlotType _slot;
	private final double _amount;
	
	/**
	 * Creates a new {@code ResistDispelByCategory} effect.<br>
	 * This constructor initializes the dispel amount and slot type from a set of parameters.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public ResistDispelByCategory(StatSet params)
	{
		_amount = params.getDouble("amount", 0);
		_slot = params.getEnum("slot", DispelSlotType.class, DispelSlotType.BUFF);
	}
	
	/**
	 * Applies a multiplier to the target's resistance based on the effect amount.<br>
	 * This method updates the stats of the {@code effected} creature.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} that triggered this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		switch (_slot)
		{
			// Only this one is in use it seems.
			case BUFF:
			{
				effected.getStat().mergeMul(Stat.RESIST_DISPEL_BUFF, 1 + (_amount / 100));
				break;
			}
		}
	}
}
