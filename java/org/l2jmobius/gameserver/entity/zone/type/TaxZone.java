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
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;

/**
 * Represents a specific type of zone designated for taxation.<br>
 * This class defines the behavior and properties associated with {@code TaxZone} areas.
 * @author malyelfik
 */
public class TaxZone extends ZoneType
{
	private int _domainId;
	private Castle _castle;
	
	/**
	 * Creates a new instance of {@code TaxZone}.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for the tax zone.
	 */
	public TaxZone(int id)
	{
		super(id);
	}
	
	/**
	 * Sets a specific configuration parameter for this zone.<br>
	 * It handles the {@code domainId} property specifically.<br>
	 * Other parameters are passed to the parent class.
	 * @param name The name of the parameter to update.
	 * @param value The string value to assign to the parameter.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equalsIgnoreCase("domainId"))
		{
			_domainId = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It updates the creature's status to indicate it is inside the tax zone.<br>
	 * If the creature is an NPC, it sets its specific tax zone reference.
	 * @param creature The {@code Creature} that entered the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.TAX, true);
		if (creature.isNpc())
		{
			creature.asNpc().setTaxZone(this);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the tax zone.<br>
	 * It updates the creature's status to indicate it is no longer inside the zone.<br>
	 * If the creature is an NPC, its tax zone reference is cleared.
	 * @param creature The {@code Creature} that is leaving the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.TAX, false);
		if (creature.isNpc())
		{
			creature.asNpc().setTaxZone(null);
		}
	}
	
	/**
	 * Retrieves the {@code Castle} associated with this zone.<br>
	 * This method uses lazy loading to fetch the castle if it is not already set.<br>
	 * It uses the internal {@code _domainId} to find the correct instance.
	 * @return the {@code Castle} object, or {@code null} if no castle exists for this ID.
	 */
	public Castle getCastle()
	{
		// Lazy loading is used because zone is loaded before residence.
		if (_castle == null)
		{
			_castle = CastleManager.getInstance().getCastleById(_domainId);
		}
		
		return _castle;
	}
}
