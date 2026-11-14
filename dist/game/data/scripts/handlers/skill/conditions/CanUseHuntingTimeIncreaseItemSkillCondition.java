/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.skill.conditions;

import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.data.xml.TimedHuntingZoneData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a player is eligible to use an item that increases hunting time.<br>
 * This condition verifies the requirements for skills associated with hunting zone extensions.
 * @author CostyKiller
 */
public class CanUseHuntingTimeIncreaseItemSkillCondition implements ISkillCondition
{
	private final int _zoneId;
	private final long _time;
	
	/**
	 * Creates a new condition to check if a hunting time increase skill is valid.<br>
	 * It initializes the required zone ID and duration from the provided stats.
	 * @param params The {@code StatSet} containing the {@code zoneId} and {@code time} values.
	 */
	public CanUseHuntingTimeIncreaseItemSkillCondition(StatSet params)
	{
		_zoneId = params.getInt("zoneId", 0);
		_time = params.getLong("time", 3600000);
	}
	
	/**
	 * Checks if a player can use a skill to increase hunting zone time.<br>
	 * It verifies the remaining time and refill limits of the current zone.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final Player player = caster.asPlayer();
		if (player == null)
		{
			return false;
		}
		
		final TimedHuntingZoneHolder holder = TimedHuntingZoneData.getInstance().getHuntingZone(_zoneId);
		if (holder == null)
		{
			return false;
		}
		
		final long currentTime = System.currentTimeMillis();
		final long endTime = currentTime + player.getTimedHuntingZoneRemainingTime(_zoneId);
		if ((endTime > currentTime) && (((endTime - currentTime) + _time) > holder.getMaximumAddedTime()))
		{
			return false;
		}
		
		final long remainRefill = player.getVariables().getInt(PlayerVariables.HUNTING_ZONE_REMAIN_REFILL + _zoneId, holder.getRemainRefillTime());
		if ((_time < remainRefill) || (remainRefill == 0))
		{
			return false;
		}
		
		return true;
	}
}
