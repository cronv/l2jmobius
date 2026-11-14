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
package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a specific zone type associated with Mother Trees.<br>
 * This zone provides basic {@code HP} and {@code MP} regeneration for entities within its boundaries.
 * @author durgus
 */
public class MotherTreeZone extends ZoneType
{
	private int _enterMsg;
	private int _leaveMsg;
	private int _mpRegen;
	private int _hpRegen;
	
	/**
	 * Creates a new {@code MotherTreeZone} instance.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this zone.
	 */
	public MotherTreeZone(int id)
	{
		super(id);
	}
	
	/**
	 * Sets a specific configuration parameter for this zone.<br>
	 * This method updates values like message IDs or regeneration bonuses.<br>
	 * If the name is not recognized, it passes the value to the parent class.
	 * @param name The name of the parameter to set.
	 * @param value The string value to assign to the parameter.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("enterMsgId"))
		{
			_enterMsg = Integer.parseInt(value);
		}
		else if (name.equals("leaveMsgId"))
		{
			_leaveMsg = Integer.parseInt(value);
		}
		else if (name.equals("MpRegenBonus"))
		{
			_mpRegen = Integer.parseInt(value);
		}
		else if (name.equals("HpRegenBonus"))
		{
			_hpRegen = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It checks if the creature is a {@code Player}.<br>
	 * If it is a player, it updates their zone status and sends a system message.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			creature.setInsideZone(ZoneId.MOTHER_TREE, true);
			if (_enterMsg != 0)
			{
				player.sendPacket(new SystemMessage(_enterMsg));
			}
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It updates the zone status for players.<br>
	 * It sends a system message if one is configured.
	 * @param creature The {@code Creature} that is exiting the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			player.setInsideZone(ZoneId.MOTHER_TREE, false);
			if (_leaveMsg != 0)
			{
				player.sendPacket(new SystemMessage(_leaveMsg));
			}
		}
	}
	
	/**
	 * Retrieves the MP regeneration bonus for this zone.<br>
	 * This value is used to calculate how much extra mana a creature recovers.
	 * @return The integer value of the MP regeneration bonus.
	 */
	public int getMpRegenBonus()
	{
		return _mpRegen;
	}
	
	/**
	 * Returns the health point regeneration bonus for this zone.<br>
	 * This value is used to calculate how much HP a creature recovers over time.
	 * @return The {@code int} value of the health regeneration bonus.
	 */
	public int getHpRegenBonus()
	{
		return _hpRegen;
	}
}
