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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the initial effects triggered when a castle is taken.<br>
 * It manages the logic for starting the {@code Castle} state transition.
 * @author St3eT
 */
public class TakeCastleStart extends AbstractEffect
{
	/**
	 * Initializes a new {@code TakeCastleStart} effect.<br>
	 * This constructor sets up the initial state using provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public TakeCastleStart(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method returns {@code true} for this specific effect.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect for a specific skill.<br>
	 * This method checks if the {@code effector} is a player.<br>
	 * It announces the siege start to players if the conditions are met.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer())
		{
			return;
		}
		
		final Castle castle = CastleManager.getInstance().getCastle(effected);
		if ((castle != null) && castle.getSiege().isInProgress())
		{
			castle.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.THE_OPPOSING_CLAN_HAS_STARTED_S1).addSkillName(skill.getId()), false);
		}
	}
}
