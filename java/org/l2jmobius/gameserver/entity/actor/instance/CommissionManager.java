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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExShowCommission;

/**
 * Manages the logic and interactions for commission-related NPCs.<br>
 * This class handles how players interact with commissions within an instance.<br>
 * It serves as a specialized {@code Npc} for processing quest-like tasks.
 * @author NosBit
 */
public class CommissionManager extends Npc
{
	/**
	 * Creates a new {@code CommissionManager} instance.<br>
	 * This constructor initializes the manager using the provided template.<br>
	 * It also sets the {@code InstanceType} to {@code CommissionManager}.
	 * @param template The {@code NpcTemplate} used to create this NPC.
	 */
	public CommissionManager(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.CommissionManager);
	}
	
	/**
	 * Checks if a specific creature can attack this NPC automatically.<br>
	 * This method returns {@code true} if the attacker is a monster.<br>
	 * Otherwise, it uses the default behavior from the parent class.
	 * @param attacker The {@code Creature} attempting to perform an auto-attack.
	 * @return {@code true} if the attack is allowed, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker.isMonster())
		{
			return true;
		}
		
		return super.isAutoAttackable(attacker);
	}
	
	/**
	 * Handles feedback when a player uses a bypass command.<br>
	 * It checks if the {@code command} is {@code show_commission}.<br>
	 * If it matches, it sends a specific packet to the {@code player}.<br>
	 * Otherwise, it calls the parent method.
	 * @param player The {@code Player} who sent the command.
	 * @param command The string content of the bypass command.
	 */
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.equalsIgnoreCase("show_commission"))
		{
			player.sendPacket(ExShowCommission.STATIC_PACKET);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}
