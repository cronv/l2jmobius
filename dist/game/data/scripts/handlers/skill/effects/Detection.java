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
package handlers.skill.effects;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for detection effects in the game.<br>
 * It manages how entities perceive and interact with each other based on specific skill mechanics.
 * @author UnAfraid
 */
public class Detection extends AbstractEffect
{
	/**
	 * Creates a new {@code Detection} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public Detection(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect occurs instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect for a specific skill.<br>
	 * This method checks if both creatures are players.<br>
	 * It removes the {@code HIDE} status from an invisible target if they are not allies.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer() || !effected.isPlayer())
		{
			return;
		}
		
		final Player player = effector.asPlayer();
		final Player target = effected.asPlayer();
		final boolean hasParty = player.isInParty();
		final int clanId = player.getClanId();
		final boolean hasClan = clanId > 0;
		final int allyId = player.getAllyId();
		final boolean hasAlly = allyId > 0;
		
		if (target.isInvisible())
		{
			if (hasParty && (target.isInParty()) && (player.getParty().getLeaderObjectId() == target.getParty().getLeaderObjectId()))
			{
				return;
			}
			else if (hasClan && (clanId == target.getClanId()))
			{
				return;
			}
			else if (hasAlly && (allyId == target.getAllyId()))
			{
				return;
			}
			
			// Remove Hide.
			target.getEffectList().stopEffects(AbnormalType.HIDE);
		}
	}
}
