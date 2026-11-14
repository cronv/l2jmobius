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
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class implements a condition to target all members of the player's party.<br>
 * It specifically excludes the player who cast the action from being targeted by {@code ConditionTargetMyPartyExceptMe}.
 * @author Adry_85
 */
public class ConditionTargetMyPartyExceptMe extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new instance of {@code ConditionTargetMyPartyExceptMe}.<br>
	 * This constructor sets the internal state for the condition.
	 * @param value The boolean value to initialize the condition with.
	 */
	public ConditionTargetMyPartyExceptMe(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the target meets the party membership requirements.<br>
	 * It verifies that both creatures are players and belong to the same party.<br>
	 * The method ensures the effector does not target themselves.
	 * @param effector The creature performing the action.
	 * @param effected The creature being targeted by the action.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} involved in the action.
	 * @return {@code true} if the target is a valid party member, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		boolean isPartyMember = true;
		final Player player = effector.asPlayer();
		if ((player == null) || (effected == null) || !effected.isPlayer())
		{
			isPartyMember = false;
		}
		else if (player == effected)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_ON_YOURSELF);
			isPartyMember = false;
		}
		else if (!player.isInParty() || !player.getParty().equals(effected.getParty()))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
			sm.addSkillName(skill);
			player.sendPacket(sm);
			isPartyMember = false;
		}
		
		return _value == isPartyMember;
	}
}
