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
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the physical damage mute effect.<br>
 * It prevents a {@code Creature} from dealing physical damage while the effect is active.
 * @author -Rnn-
 */
public class PhysicalDamageMute extends AbstractEffect
{
	/**
	 * Creates a new instance of {@code PhysicalDamageMute}.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public PhysicalDamageMute(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for this effect's flags.<br>
	 * It returns the value associated with {@code EffectFlag.PSYCHICAL_ATTACK_MUTED}.
	 * @return The long value representing the effect flags.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.PSYCHICAL_ATTACK_MUTED.getMask();
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It stops the target from performing an attack.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to apply this effect.
	 * @param item The {@code Item} used to apply this effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.abortAttack();
	}
}
