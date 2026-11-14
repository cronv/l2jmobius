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

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

/**
 * Represents a zone where player-versus-player combat is disabled.<br>
 * This {@code NoPvPZone} type ensures that players cannot attack each other within its boundaries.
 * @author Edoo
 */
public class NoPvPZone extends ZoneType
{
	/**
	 * Creates a new {@code NoPvPZone} instance.<br>
	 * This zone prevents players from attacking each other.
	 * @param id The unique identifier for this zone.
	 */
	public NoPvPZone(int id)
	{
		super(id);
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It updates the creature's status based on the current configuration.<br>
	 * It also broadcasts information if the creature is a {@code Player}.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			
			// PVP possible during siege, now for siege participants only.
			// Could also check if this town is in siege, or if any siege is going on.
			if ((player.getSiegeState() != 0) && (GeneralConfig.PEACE_ZONE_MODE == 1))
			{
				return;
			}
		}
		
		if (GeneralConfig.PEACE_ZONE_MODE != 2)
		{
			creature.setInsideZone(ZoneId.NO_PVP, true);
		}
		
		// Send player info to nearby players.
		if (creature.isPlayer())
		{
			creature.broadcastInfo();
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It updates the creature's status regarding the {@code ZoneId.NO_PVP} area.<br>
	 * If the entity is a {@code Player}, it broadcasts their information to nearby players.
	 * @param creature The {@code Creature} that is exiting the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (GeneralConfig.PEACE_ZONE_MODE != 2)
		{
			creature.setInsideZone(ZoneId.NO_PVP, false);
		}
		
		// Send player info to nearby players.
		if (creature.isPlayer() && !creature.isTeleporting())
		{
			creature.broadcastInfo();
		}
	}
	
	/**
	 * Enables or disables this zone.<br>
	 * If set to {@code true}, it updates the status of players and their summons inside the zone.<br>
	 * If set to {@code false}, it removes all characters currently inside the zone.
	 * @param value The new enabled state to apply.
	 */
	@Override
	public void setEnabled(boolean value)
	{
		super.setEnabled(value);
		if (value)
		{
			for (Player player : World.getPlayers())
			{
				if ((player != null) && isInsideZone(player))
				{
					revalidateInZone(player);
					
					final Pet pet = player.getPet();
					if (pet != null)
					{
						revalidateInZone(pet);
					}
					
					for (Summon summon : player.getServitors().values())
					{
						revalidateInZone(summon);
					}
				}
			}
		}
		else
		{
			for (Creature creature : getCharactersInside())
			{
				if (creature != null)
				{
					removeCharacter(creature);
				}
			}
		}
	}
}
