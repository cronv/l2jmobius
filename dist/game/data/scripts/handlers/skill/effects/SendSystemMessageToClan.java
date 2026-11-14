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
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for sending a system message to all members of a {@code Clan}.<br>
 * It is triggered as an effect when a skill or item interaction occurs.
 * @author Nik
 */
public class SendSystemMessageToClan extends AbstractEffect
{
	private final SystemMessage _message;
	
	/**
	 * Sends a system message to all members of a clan.<br>
	 * This method initializes the message using an ID from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public SendSystemMessageToClan(StatSet params)
	{
		final int id = params.getInt("id", 0);
		_message = new SystemMessage(id);
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the effect immediately.<br>
	 * This method sends a system message to all online members of the target's clan.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with this effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if ((player == null) || (_message == null))
		{
			return;
		}
		
		final Clan clan = player.getClan();
		if (clan != null)
		{
			clan.broadcastToOnlineMembers(_message);
		}
	}
}
