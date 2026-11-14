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

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the target of a skill is a member of the caster's party.<br>
 * This condition ensures that the {@code Skill} can only be applied to friendly party members.
 * @author UnAfraid
 */
public class TargetMyPartySkillCondition implements ISkillCondition
{
	private final boolean _includeMe;
	
	/**
	 * Creates a new condition to check if a target is in the same party.<br>
	 * It uses the provided {@code StatSet} to configure settings.<br>
	 * The {@code includeMe} flag determines if the caster is included.
	 * @param params The {@code StatSet} containing configuration values.
	 */
	public TargetMyPartySkillCondition(StatSet params)
	{
		_includeMe = params.getBoolean("includeMe");
	}
	
	/**
	 * Checks if the {@code caster} can use a specific {@code skill} on the {@code target}.<br>
	 * This method verifies if the target belongs to the same party as the caster.<br>
	 * It also handles special cases for summons and self-targeting based on configuration.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object that is being targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((target == null) || !target.isPlayable())
		{
			return false;
		}
		
		final Party party = caster.getParty();
		if (target.isPlayer())
		{
			final Party targetParty = target.asPlayer().getParty();
			return ((party == null) ? (_includeMe && (caster == target)) : (_includeMe ? party == targetParty : (party == targetParty) && (caster != target)));
		}
		else if (target.isSummon())
		{
			final Summon summon = target.asSummon();
			final Player summonOwner = summon.getOwner();
			if (summonOwner != null)
			{
				final Party targetParty = summonOwner.getParty();
				return ((party == null) ? (_includeMe && (caster == summonOwner)) : (_includeMe ? party == targetParty : (party == targetParty) && (caster != summonOwner)));
			}
		}
		
		return false;
	}
}
