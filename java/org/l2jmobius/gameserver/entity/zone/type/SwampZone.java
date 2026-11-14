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
package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

/**
 * Represents a specific type of zone that modifies the movement speed of entities.<br>
 * This class defines the behavior for swamp-like environments within the game world.
 * @author Kerberos, Mobius
 */
public class SwampZone extends ZoneType
{
	private double _move_bonus;
	private int _castleId;
	private Castle _castle;
	private int _eventId;
	
	/**
	 * Creates a new {@code SwampZone} instance.<br>
	 * This constructor initializes the zone with default values.<br>
	 * The move bonus is set to {@code 0.5}.<br>
	 * The castle and event IDs are initialized to {@code null} or {@code 0}.
	 * @param id The unique identifier for this zone.
	 */
	public SwampZone(int id)
	{
		super(id);
		
		// Setup default speed reduce (in %).
		_move_bonus = 0.5;
		
		// no castle by default
		_castleId = 0;
		_castle = null;
		
		// no event by default
		_eventId = 0;
	}
	
	/**
	 * Updates a specific property of the {@code SwampZone}.<br>
	 * This method handles values for {@code move_bonus}, {@code castleId}, and {@code eventId}.<br>
	 * If the name does not match these, it calls the superclass method.
	 * @param name The name of the parameter to set.
	 * @param value The string representation of the value to assign.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("move_bonus"))
		{
			_move_bonus = Double.parseDouble(value);
		}
		else if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
		}
		else if (name.equals("eventId"))
		{
			_eventId = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	/**
	 * Retrieves the {@code Castle} associated with this zone.<br>
	 * It fetches the castle from {@code CastleManager} if it is not already loaded.
	 * @return the {@code Castle} object or {@code null} if no castle exists.
	 */
	private Castle getCastle()
	{
		if ((_castleId > 0) && (_castle == null))
		{
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		}
		
		return _castle;
	}
	
	/**
	 * This method is called when a {@code Creature} enters the swamp zone.<br>
	 * It checks if siege conditions apply before updating the creature's status.<br>
	 * If the creature is a player, it may trigger an event and broadcast their info.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		final Castle castle = getCastle();
		if (castle != null)
		{
			// castle zones active only during siege
			if (!castle.getSiege().isInProgress())
			{
				return;
			}
			
			// defenders not affected
			final Player player = creature.asPlayer();
			if ((player != null) && player.isInSiege() && (player.getSiegeState() == 2))
			{
				return;
			}
		}
		
		creature.setInsideZone(ZoneId.SWAMP, true);
		if (creature.isPlayer())
		{
			if (_eventId > 0)
			{
				creature.sendPacket(new OnEventTrigger(_eventId, true));
			}
			
			creature.asPlayer().broadcastUserInfo();
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the swamp zone.<br>
	 * It updates the creature's internal state regarding its presence in the zone.<br>
	 * If the creature is a {@code Player}, it may send an event packet or broadcast information.
	 * @param creature The {@code Creature} that is exiting the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		// don't broadcast info if not needed
		if (creature.isInsideZone(ZoneId.SWAMP))
		{
			creature.setInsideZone(ZoneId.SWAMP, false);
			if (creature.isPlayer())
			{
				if (_eventId > 0)
				{
					creature.sendPacket(new OnEventTrigger(_eventId, false));
				}
				
				if (!creature.isTeleporting())
				{
					creature.asPlayer().broadcastUserInfo();
				}
			}
		}
	}
	
	/**
	 * Retrieves the movement speed bonus for this zone.<br>
	 * This value is used to modify the speed of creatures entering the area.
	 * @return The {@code double} value representing the move bonus.
	 */
	public double getMoveBonus()
	{
		return _move_bonus;
	}
}
