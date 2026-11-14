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
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class implements the {@code ConditionPlayerCanTakeFort} logic.<br>
 * It checks if a player meets the requirements to capture a fort.<br>
 * It ensures that only eligible players can perform this action.
 * @author Adry_85
 */
public class ConditionPlayerCanTakeFort extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new condition to check if a player can take a fort.<br>
	 * This constructor sets the required state for the condition.
	 * @param value The boolean value used to determine if the action is allowed.
	 */
	public ConditionPlayerCanTakeFort(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the {@code effector} is allowed to take a fort.<br>
	 * This method validates player status, clan leadership, and siege conditions.<br>
	 * It also verifies the target location and distance requirements.
	 * @param effector The creature performing the action.
	 * @param effected The target creature or object being interacted with.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} involved in the action.
	 * @return {@code true} if all conditions are met, {@code false} otherwise.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if ((effector == null) || !effector.isPlayer())
		{
			return !_value;
		}
		
		final Player player = effector.asPlayer();
		boolean canTakeFort = true;
		if (player.isAlikeDead() || player.isCursedWeaponEquipped() || !player.isClanLeader())
		{
			canTakeFort = false;
		}
		
		final Fort fort = FortManager.getInstance().getFort(player);
		final SystemMessage sm;
		if ((fort == null) || (fort.getResidenceId() <= 0) || !fort.getSiege().isInProgress() || (fort.getSiege().getAttackerClan(player.getClan()) == null))
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
			sm.addSkillName(skill);
			player.sendPacket(sm);
			canTakeFort = false;
		}
		else if (fort.getFlagPole() != effected)
		{
			player.sendPacket(SystemMessageId.INVALID_TARGET);
			canTakeFort = false;
		}
		else if (!LocationUtil.checkIfInRange(200, player, effected, true))
		{
			player.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_CANCELLED);
			canTakeFort = false;
		}
		
		return _value == canTakeFort;
	}
}
