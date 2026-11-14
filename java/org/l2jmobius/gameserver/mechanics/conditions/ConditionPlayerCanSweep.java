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
package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Validates whether a player meets all requirements to perform a sweep action.<br>
 * It checks for valid targets, party membership, corpse age, and inventory capacity.<br>
 * If multiple conditions fail, it provides specific feedback messages for each failure.
 * @author Zoey76
 */
public class ConditionPlayerCanSweep extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new instance of {@code ConditionPlayerCanSweep}.<br>
	 * This constructor sets the internal state for the sweep condition.
	 * @param value The boolean value to be used by the condition.
	 */
	public ConditionPlayerCanSweep(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Validates if a player can perform the sweep action on a target.<br>
	 * This method checks for ownership, corpse age, and inventory capacity.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} being targeted by the action.
	 * @param skill The {@code Skill} used to perform the sweep.
	 * @param item The {@code ItemTemplate} associated with the action.
	 * @return {@code true} if all conditions are met, {@code false} otherwise.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		boolean canSweep = false;
		if (effector.isPlayer() && (skill != null))
		{
			final Player sweeper = effector.asPlayer();
			for (WorldObject wo : skill.getTargetsAffected(sweeper, effected))
			{
				if ((wo != null) && wo.isAttackable())
				{
					final Attackable attackable = wo.asAttackable();
					if (attackable.isDead())
					{
						if (attackable.isSpoiled())
						{
							canSweep = attackable.checkSpoilOwner(sweeper, true);
							if (canSweep)
							{
								canSweep = !attackable.isOldCorpse(sweeper, NpcConfig.CORPSE_CONSUME_SKILL_ALLOWED_TIME_BEFORE_DECAY, true);
							}
							
							if (canSweep)
							{
								canSweep = sweeper.getInventory().checkInventorySlotsAndWeight(attackable.getSpoilLootItems(), true, true);
							}
						}
						else
						{
							sweeper.sendPacket(SystemMessageId.THE_SWEEPER_HAS_FAILED_AS_THE_TARGET_IS_NOT_SPOILED);
						}
					}
				}
			}
		}
		
		return _value == canSweep;
	}
}
