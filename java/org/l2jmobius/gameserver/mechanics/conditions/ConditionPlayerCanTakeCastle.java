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
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class implements the condition to check if a {@code Player} is eligible to take over a castle.<br>
 * It validates whether the player meets the necessary requirements defined by the game mechanics.
 * @author Adry_85
 */
public class ConditionPlayerCanTakeCastle extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new condition to check if a player can take a castle.<br>
	 * This constructor sets the required state for the condition.
	 * @param value The boolean value used to determine if the action is allowed.
	 */
	public ConditionPlayerCanTakeCastle(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the {@code effector} is allowed to take a castle.<br>
	 * This method validates player status, clan leadership, and siege conditions.<br>
	 * It also verifies that the target is within range and part of the correct castle.
	 * @param effector The creature performing the action.
	 * @param effected The creature being targeted by the action.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} associated with the action.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if ((effector == null) || !effector.isPlayer())
		{
			return !_value;
		}
		
		final Player player = effector.asPlayer();
		boolean canTakeCastle = true;
		if (player.isAlikeDead() || player.isCursedWeaponEquipped() || !player.isClanLeader())
		{
			canTakeCastle = false;
		}
		
		final Castle castle = CastleManager.getInstance().getCastle(player);
		SystemMessage sm;
		if ((castle == null) || (castle.getResidenceId() <= 0) || !castle.getSiege().isInProgress() || (castle.getSiege().getAttackerClan(player.getClan()) == null))
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
			sm.addSkillName(skill);
			player.sendPacket(sm);
			canTakeCastle = false;
		}
		else if (!castle.getArtefacts().contains(effected))
		{
			player.sendPacket(SystemMessageId.INVALID_TARGET);
			canTakeCastle = false;
		}
		else if (!LocationUtil.checkIfInRange(200, player, effected, true) || (player.getZ() < effected.getZ()) || (Math.abs(player.getZ() - effected.getZ()) > 40))
		{
			player.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_CANCELLED);
			canTakeCastle = false;
		}
		
		return _value == canTakeCastle;
	}
}
