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
import org.l2jmobius.gameserver.entity.actor.instance.Trap;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code TrapDetect} skill effect.<br>
 * It allows players to identify hidden traps in the game world.
 * @author UnAfraid
 */
public class TrapDetect extends AbstractEffect
{
	private final int _power;
	
	/**
	 * Creates a new instance of the {@code TrapDetect} effect.<br>
	 * This constructor initializes the effect using provided statistics.<br>
	 * It requires the {@code power} value to be present in the input set.
	 * @param params The {@code StatSet} containing the configuration values for this effect.
	 */
	public TrapDetect(StatSet params)
	{
		if (params.isEmpty())
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + ": effect without power!");
		}
		
		_power = params.getInt("power");
	}
	
	/**
	 * Checks if the effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * This method handles the immediate application of a trap detection effect.<br>
	 * It checks if the target is a valid trap and determines if it can be detected based on power.<br>
	 * If successful, it marks the trap as detected by the effector.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} being acted upon.
	 * @param skill The {@code Skill} used to trigger the effect.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isTrap() || effected.isAlikeDead())
		{
			return;
		}
		
		final Trap trap = (Trap) effected;
		if (trap.getLevel() <= _power)
		{
			trap.setDetected(effector);
		}
	}
}
