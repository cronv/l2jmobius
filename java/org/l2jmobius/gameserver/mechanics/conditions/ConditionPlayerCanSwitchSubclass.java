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
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * Checks if a {@code Player} is allowed to change their current subclass.<br>
 * This condition validates the requirements for switching character classes.
 * @author Sdw
 */
public class ConditionPlayerCanSwitchSubclass extends Condition
{
	private final int _subIndex;
	
	/**
	 * Checks if a player is allowed to switch to a specific subclass.<br>
	 * This condition uses the provided index to identify the target subclass.
	 * @param subIndex The index of the subclass to check against.
	 */
	public ConditionPlayerCanSwitchSubclass(int subIndex)
	{
		_subIndex = subIndex;
	}
	
	/**
	 * Checks if a {@code Player} is allowed to switch their subclass.<br>
	 * This method validates several conditions such as inventory space, weight limits, and active status.<br>
	 * It returns {@code false} if any restriction prevents the change.
	 * @param effector The creature attempting to perform the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item involved in the transaction.
	 * @return {@code true} if the subclass can be switched, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		boolean canSwitchSub = true;
		
		final Player player = effector.asPlayer();
		if ((player == null) || player.isAlikeDead())
		{
			canSwitchSub = false;
		}
		else if (((_subIndex != 0) && (player.getSubClasses().get(_subIndex) == null)) || (player.getClassIndex() == _subIndex))
		{
			canSwitchSub = false;
		}
		else if (!player.isInventoryUnder90(true))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_CREATE_OR_CHANGE_A_SUBCLASS_WHILE_YOU_HAVE_NO_FREE_SPACE_IN_YOUR_INVENTORY);
			canSwitchSub = false;
		}
		else if (player.getWeightPenalty() >= 2)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_CREATE_OR_CHANGE_A_DUAL_CLASS_WHILE_YOU_HAVE_OVERWEIGHT);
			canSwitchSub = false;
		}
		else if (player.isRegisteredOnEvent())
		{
			player.sendMessage("You cannot change your subclass while registered in an event.");
			canSwitchSub = false;
		}
		else if (player.isAllSkillsDisabled())
		{
			canSwitchSub = false;
		}
		else if (player.isAffected(EffectFlag.MUTED))
		{
			canSwitchSub = false;
			player.sendPacket(SystemMessageId.YOU_CANNOT_CHANGE_THE_CLASS_BECAUSE_OF_IDENTITY_CRISIS);
		}
		else if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) || (player.getPvpFlag() > 0) || player.isInInstance() || player.isTransformed() || player.isMounted())
		{
			canSwitchSub = false;
		}
		
		return canSwitchSub;
	}
}
