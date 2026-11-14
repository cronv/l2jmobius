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

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClan;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a character possesses the {@code Holy Thing} skill.<br>
 * This condition is used to restrict certain skills or actions based on player abilities.
 * @author UnAfraid, Mobius
 */
public class PossessHolythingSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new condition to check for the Holy Thing skill.<br>
	 * It uses the provided {@code StatSet} to define the requirements.
	 * @param params The set of stats used to initialize the condition.
	 */
	public PossessHolythingSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the caster meets all requirements to use a specific skill.<br>
	 * This method validates player status, clan leadership, and target proximity.<br>
	 * It also ensures the target is a valid castle artefact during an active siege.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((caster == null) || !caster.isPlayer())
		{
			return false;
		}
		
		final Player player = caster.asPlayer();
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
		else if (isAlliedWithDefender(castle, player.getClan()))
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
			sm.addSkillName(skill);
			player.sendPacket(sm);
			canTakeCastle = false;
		}
		else if (!castle.getArtefacts().contains(target))
		{
			player.sendPacket(SystemMessageId.INVALID_TARGET);
			canTakeCastle = false;
		}
		else if (!LocationUtil.checkIfInRange(skill.getCastRange(), player, target, true) || (Math.abs(player.getZ() - target.getZ()) > 45))
		{
			player.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_CANCELLED);
			canTakeCastle = false;
		}
		
		return canTakeCastle;
	}
	
	/**
	 * Checks if the clan belongs to the same alliance as any defending clan of the castle siege.
	 * @param castle The {@code Castle} under siege.
	 * @param clan The {@code Clan} of the caster.
	 * @return {@code true} if the clan is allied with a defender, {@code false} otherwise.
	 */
	private boolean isAlliedWithDefender(Castle castle, Clan clan)
	{
		final int allyId = clan.getAllyId();
		if (allyId == 0)
		{
			return false;
		}
		
		for (SiegeClan siegeClan : castle.getSiege().getDefenderClans())
		{
			final Clan defender = ClanTable.getInstance().getClan(siegeClan.getClanId());
			if ((defender != null) && (defender.getAllyId() == allyId))
			{
				return true;
			}
		}
		
		return false;
	}
}
