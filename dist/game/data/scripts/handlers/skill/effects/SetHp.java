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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This effect sets a creature's current HP to a specific value.<br>
 * It is used by {@code Skill} mechanics to modify health directly.
 * @author Nik
 */
public class SetHp extends AbstractEffect
{
	private final double _amount;
	private final StatModifierType _mode;
	
	/**
	 * Creates a new {@code SetHp} effect instance.<br>
	 * This constructor initializes the amount and mode from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public SetHp(StatSet params)
	{
		_amount = params.getDouble("amount", 0);
		_mode = params.getEnum("mode", StatModifierType.class, StatModifierType.DIFF);
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the effect to a target creature immediately.<br>
	 * This method updates the current HP of the {@code effected} creature.<br>
	 * It ignores targets that are already dead or are doors.
	 * @param effector The creature who performs the action.
	 * @param effected The creature who receives the effect.
	 * @param skill The {@code Skill} being used to trigger this effect.
	 * @param item The {@code Item} associated with the skill, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead() || effected.isDoor())
		{
			return;
		}
		
		final boolean full = (_mode == StatModifierType.PER) && (_amount == 100.0);
		final long maxHp = effected.getMaxHp();
		final double amount = full ? maxHp : (_mode == StatModifierType.PER) ? ((maxHp * _amount) / 100.0) : _amount;
		effected.setCurrentHp(amount);
	}
}
