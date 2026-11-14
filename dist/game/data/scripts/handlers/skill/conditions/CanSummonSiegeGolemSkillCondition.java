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

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Skill condition allowing a Siege Golem summon only during a siege the casters clan is attacking.
 * @author Sdw, Mobius
 */
public class CanSummonSiegeGolemSkillCondition implements ISkillCondition
{
	/**
	 * Creates a {@code CanSummonSiegeGolemSkillCondition} from the given parameters.
	 * @param params The {@code StatSet} of condition parameters.
	 */
	public CanSummonSiegeGolemSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks whether the caster may summon a Siege Golem, requiring an alive clan member at a castle or fort siege their clan is attacking.
	 * @param caster The {@code Creature} casting the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} targeted.
	 * @return {@code true} if the summon is allowed, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((caster == null) || !caster.isPlayer())
		{
			return false;
		}
		
		final Player player = caster.asPlayer();
		boolean canSummonSiegeGolem = true;
		if (player.isAlikeDead() || player.isCursedWeaponEquipped() || (player.getClan() == null))
		{
			canSummonSiegeGolem = false;
		}
		
		final Castle castle = CastleManager.getInstance().getCastle(player);
		final Fort fort = FortManager.getInstance().getFort(player);
		if ((castle == null) && (fort == null))
		{
			canSummonSiegeGolem = false;
		}
		
		if (((fort != null) && (fort.getResidenceId() == 0)) || ((castle != null) && (castle.getResidenceId() == 0)))
		{
			player.sendPacket(SystemMessageId.INVALID_TARGET);
			canSummonSiegeGolem = false;
		}
		else if (((castle != null) && !castle.getSiege().isInProgress()) || ((fort != null) && !fort.getSiege().isInProgress()))
		{
			player.sendPacket(SystemMessageId.INVALID_TARGET);
			canSummonSiegeGolem = false;
		}
		else
		{
			final int clanId = player.getClanId();
			if ((clanId != 0) && (((castle != null) && (castle.getSiege().getAttackerClan(clanId) == null)) || ((fort != null) && (fort.getSiege().getAttackerClan(clanId) == null))))
			{
				player.sendPacket(SystemMessageId.INVALID_TARGET);
				canSummonSiegeGolem = false;
			}
		}
		
		// Siege weapons are summoned outside the castle walls.
		if (canSummonSiegeGolem && player.isInsideZone(ZoneId.CASTLE))
		{
			player.sendPacket(SystemMessageId.CANNOT_BE_SUMMONED_IN_THIS_LOCATION);
			canSummonSiegeGolem = false;
		}
		
		return canSummonSiegeGolem;
	}
}
