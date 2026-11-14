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
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This class checks if a {@code Player} meets the requirements to create an outpost.<br>
 * It validates specific game mechanics and conditions for the creation process.
 * @author Adry_85
 */
public class ConditionPlayerCanCreateOutpost extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new instance of the {@code ConditionPlayerCanCreateOutpost} condition.<br>
	 * This sets whether a player is allowed to create an outpost.
	 * @param value The boolean state determining if creation is permitted.
	 */
	public ConditionPlayerCanCreateOutpost(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the {@code effector} meets all requirements to create an outpost.<br>
	 * This method validates player status, clan leadership, and location constraints.
	 * @param effector The {@code Creature} attempting the action.
	 * @param effected The {@code Creature} receiving the effect.
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
		boolean canCreateOutpost = true;
		if (player.isAlikeDead() || player.isCursedWeaponEquipped() || (player.getClan() == null))
		{
			canCreateOutpost = false;
		}
		
		final Castle castle = CastleManager.getInstance().getCastle(player);
		final Fort fort = FortManager.getInstance().getFort(player);
		if ((castle == null) && (fort == null))
		{
			canCreateOutpost = false;
		}
		
		if (((fort != null) && (fort.getResidenceId() == 0)) || ((castle != null) && (castle.getResidenceId() == 0)))
		{
			player.sendMessage("You must be on fort or castle ground to construct an outpost or flag.");
			canCreateOutpost = false;
		}
		else if (((fort != null) && !fort.getZone().isActive()) || ((castle != null) && !castle.getZone().isActive()))
		{
			player.sendMessage("You can only construct an outpost or flag on siege field.");
			canCreateOutpost = false;
		}
		else if (!player.isClanLeader())
		{
			player.sendMessage("You must be a clan leader to construct an outpost or flag.");
			canCreateOutpost = false;
		}
		else if (!player.isInsideZone(ZoneId.HQ))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SET_UP_A_BASE_HERE);
			canCreateOutpost = false;
		}
		
		return _value == canCreateOutpost;
	}
}
