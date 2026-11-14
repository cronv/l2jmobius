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
package handlers.skill.conditions;

import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the skill can be cast based on specific "Op Sweeper" requirements.<br>
 * This condition validates whether the target and caster meet the necessary criteria for this unique skill.
 * @author Zoey76
 */
public class OpSweeperSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new {@code OpSweeperSkillCondition} instance.<br>
	 * This constructor initializes the condition using specific statistics.
	 * @param params The {@code StatSet} containing the required values.
	 */
	public OpSweeperSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use the specified {@code skill} on a {@code target}.<br>
	 * This method validates conditions for sweeping corpses.<br>
	 * It ensures the target is spoiled and that the player has enough inventory space.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		boolean canSweep = false;
		if (caster.isPlayer() && (skill != null))
		{
			final Player sweeper = caster.asPlayer();
			for (WorldObject wo : skill.getTargetsAffected(sweeper, target))
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
		
		return canSweep;
	}
}
