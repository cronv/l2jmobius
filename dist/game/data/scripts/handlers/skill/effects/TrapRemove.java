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
import org.l2jmobius.gameserver.entity.actor.enums.creature.TrapAction;
import org.l2jmobius.gameserver.entity.actor.instance.Trap;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnTrapAction;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for removing traps from a target area.<br>
 * It implements the {@code TrapRemove} effect to clear active traps.<br>
 * Use this class when a skill or action needs to eliminate nearby {@code Trap} instances.
 * @author UnAfraid
 */
public class TrapRemove extends AbstractEffect
{
	private final int _power;
	
	/**
	 * Creates a new instance of the {@code TrapRemove} effect.<br>
	 * This constructor initializes the effect using provided statistics.<br>
	 * It requires the {@code power} value to be present in the input set.
	 * @param params The {@code StatSet} containing the required configuration values.
	 */
	public TrapRemove(StatSet params)
	{
		if (params.isEmpty())
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + ": effect without power!");
		}
		
		_power = params.getInt("power");
	}
	
	/**
	 * Checks if the effect occurs instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of a trap removal.<br>
	 * This method checks if the target is a valid trap and disarms it.<br>
	 * It also handles script notifications and player messages.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} being affected by the action.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isTrap())
		{
			return;
		}
		
		if (effected.isAlikeDead())
		{
			return;
		}
		
		final Trap trap = (Trap) effected;
		if (!trap.canBeSeen(effector))
		{
			if (effector.isPlayer())
			{
				effector.sendPacket(SystemMessageId.INVALID_TARGET);
			}
			return;
		}
		
		if (trap.getLevel() > _power)
		{
			return;
		}
		
		// Notify to scripts
		if (EventDispatcher.getInstance().hasListener(EventType.ON_TRAP_ACTION, trap))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnTrapAction(trap, effector, TrapAction.TRAP_DISARMED), trap);
		}
		
		trap.unSummon();
		if (effector.isPlayer())
		{
			effector.sendPacket(SystemMessageId.THE_TRAP_DEVICE_HAS_BEEN_STOPPED);
		}
	}
}
