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
 * This class handles the logic for granting resistance against abnormal status effects based on a specific category.<br>
 * It allows characters to mitigate or ignore certain types of debuffs defined by the game's mechanics.
 * @author Sdw
 */
public class ResistAbnormalByCategory extends AbstractEffect
{
	private final DispelSlotType _slot;
	private final double _amount;
	
	/**
	 * Creates a new {@code ResistAbnormalByCategory} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the resistance amount and the specific dispel slot type.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public ResistAbnormalByCategory(StatSet params)
	{
		_amount = params.getDouble("amount", 0);
		_slot = params.getEnum("slot", DispelSlotType.class, DispelSlotType.DEBUFF);
	}
	
	/**
	 * Applies the effect to a specific creature.<br>
	 * This method updates the stats of the {@code effected} target based on the provided {@code skill}.<br>
	 * It calculates the new stat value using the internal amount and applies it to the correct slot.
	 * @param effected The {@code Creature} that will receive the effect.
	 * @param skill The {@code Skill} being used to trigger this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		switch (_slot)
		{
			// Only this one is in use it seems.
			case DEBUFF:
			{
				effected.getStat().mergeMul(Stat.RESIST_ABNORMAL_DEBUFF, 1 + (_amount / 100));
				break;
			}
		}
	}
}
