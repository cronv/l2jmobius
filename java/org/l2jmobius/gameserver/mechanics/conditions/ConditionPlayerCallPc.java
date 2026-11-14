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
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class implements the {@code PlayerCallPc} condition.<br>
 * It checks if a player has successfully called another player character.<br>
 * Use this condition to verify specific player-to-player interaction requirements.
 * @author Adry_85
 */
public class ConditionPlayerCallPc extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new instance of {@code ConditionPlayerCallPc}.<br>
	 * This constructor sets the required condition state.
	 * @param value The boolean value to be used for this condition.
	 */
	public ConditionPlayerCallPc(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the {@code effector} is allowed to call a player.<br>
	 * This method validates various restrictions like death, olympiad mode, and zone limits.
	 * @param effector The creature performing the action.
	 * @param effected The target creature being affected.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		boolean canCallPlayer = true;
		final Player player = effector.asPlayer();
		if (player == null)
		{
			canCallPlayer = false;
		}
		else if ((effected != null) && effected.isPlayer() && effected.isDead())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED_OR_TELEPORTED);
			sm.addPcName(effected.asPlayer());
			player.sendPacket(sm);
			canCallPlayer = false;
		}
		else if (player.isInOlympiadMode())
		{
			player.sendPacket(SystemMessageId.CANNOT_BE_SUMMONED_IN_THIS_LOCATION);
			canCallPlayer = false;
		}
		else if (player.getObserverMode().isActive())
		{
			canCallPlayer = false;
		}
		else if (player.isOnEvent())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_SUMMONING_OR_TELEPORTING_IN_THIS_AREA);
			canCallPlayer = false;
		}
		else if (player.isInsideZone(ZoneId.NO_SUMMON_FRIEND) || player.isInsideZone(ZoneId.JAIL) || player.isFlyingMounted())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_SUMMONING_OR_TELEPORTING_IN_THIS_AREA);
			canCallPlayer = false;
		}
		
		return _value == canCallPlayer;
	}
}
