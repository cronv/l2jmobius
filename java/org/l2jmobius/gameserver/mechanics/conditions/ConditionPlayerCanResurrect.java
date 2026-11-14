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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class checks if a player is eligible to be resurrected.<br>
 * It validates the current state of the {@code Player} and surrounding mechanics.
 * @author UnAfraid
 */
public class ConditionPlayerCanResurrect extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new condition to check if a player can be resurrected.<br>
	 * This constructor sets the allowed state for resurrection.
	 * @param value The boolean value determining if resurrection is permitted.
	 */
	public ConditionPlayerCanResurrect(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the resurrection condition is met for a specific action.<br>
	 * This method validates whether the {@code effected} creature can be resurrected based on its state and current game rules.<br>
	 * It handles checks for player status, summon ownership, and siege restrictions.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} being targeted by the action.
	 * @param skill The {@code Skill} being used to perform the resurrection.
	 * @param item The {@code ItemTemplate} associated with the action.
	 * @return {@code true} if the resurrection is allowed, {@code false} otherwise.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		// Need skill rework for fix that properly.
		if (skill.getAffectRange() > 0)
		{
			return true;
		}
		
		if (effected == null)
		{
			return false;
		}
		
		boolean canResurrect = true;
		if (effected.isPlayer())
		{
			final Player player = effected.asPlayer();
			if (!player.isDead())
			{
				canResurrect = false;
				if (effector.isPlayer())
				{
					final SystemMessage msg = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
					msg.addSkillName(skill);
					effector.sendPacket(msg);
				}
			}
			else if (player.isResurrectionBlocked())
			{
				canResurrect = false;
				if (effector.isPlayer())
				{
					effector.sendPacket(SystemMessageId.REJECT_RESURRECTION);
				}
			}
			else if (player.isReviveRequested())
			{
				canResurrect = false;
				if (effector.isPlayer())
				{
					effector.sendPacket(SystemMessageId.RESURRECTION_HAS_ALREADY_BEEN_PROPOSED);
				}
			}
			else if (skill.getId() != 2393) // Blessed Scroll of Battlefield Resurrection
			{
				final Siege siege = SiegeManager.getInstance().getSiege(player);
				final SystemMessageId refusal = (siege != null) && siege.isInProgress() ? siege.getResurrectionRefusal(player) : null;
				if (refusal != null)
				{
					canResurrect = false;
					if (effector.isPlayer())
					{
						effector.sendPacket(refusal);
					}
				}
			}
		}
		else if (effected.isSummon())
		{
			final Summon summon = effected.asSummon();
			final Player player = summon.getOwner();
			if (!summon.isDead())
			{
				canResurrect = false;
				if (effector.isPlayer())
				{
					final SystemMessage msg = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
					msg.addSkillName(skill);
					effector.sendPacket(msg);
				}
			}
			else if (summon.isResurrectionBlocked())
			{
				canResurrect = false;
				if (effector.isPlayer())
				{
					effector.sendPacket(SystemMessageId.REJECT_RESURRECTION);
				}
			}
			else if ((player != null) && player.isRevivingPet())
			{
				canResurrect = false;
				if (effector.isPlayer())
				{
					effector.sendPacket(SystemMessageId.RESURRECTION_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
				}
			}
		}
		
		return _value == canResurrect;
	}
}
