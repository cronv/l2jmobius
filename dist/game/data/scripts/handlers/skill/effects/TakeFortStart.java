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
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code TakeFortStart} skill effect.<br>
 * It manages the initial actions triggered when a player begins capturing a fort.
 * @author UnAfraid
 */
public class TakeFortStart extends AbstractEffect
{
	/**
	 * Initializes a new {@code TakeFortStart} effect.<br>
	 * This constructor sets up the initial state using provided statistics.
	 * @param params The {@code StatSet} containing the necessary parameters for this effect.
	 */
	public TakeFortStart(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method returns {@code true} for this specific effect.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect for a specific skill action.<br>
	 * This method handles logic such as announcing messages to players.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who is targeted by the action.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} involved in the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector.isPlayer())
		{
			final Fort fort = FortManager.getInstance().getFort(effector);
			final Clan clan = effector.getClan();
			if ((fort != null) && (clan != null))
			{
				fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.S1_CLAN_IS_TRYING_TO_DISPLAY_A_FLAG), clan.getName());
			}
		}
	}
}
