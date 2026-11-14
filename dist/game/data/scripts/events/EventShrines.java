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
package events;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.events.EventShrineManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

/**
 * This class handles the logic for {@code EventShrine} objects within game events.<br>
 * It manages interactions and behaviors triggered by these shrines during active events.
 * @author hlwrave, Mobius
 * @Add in event config.xml enableShrines="true" after event name to enable them.
 */
public class EventShrines extends Script
{
	private static final Map<Integer, Integer> ZONE_TRIGGERS = new HashMap<>();
	static
	{
		ZONE_TRIGGERS.put(11030, 23206292); // Hunter
		ZONE_TRIGGERS.put(11031, 24186292); // Aden
		ZONE_TRIGGERS.put(11032, 24166292); // Goddard
		ZONE_TRIGGERS.put(11035, 22136292); // Shuttgard
		ZONE_TRIGGERS.put(11028, 20226292); // Dion
		ZONE_TRIGGERS.put(11029, 22196292); // Oren
		ZONE_TRIGGERS.put(11020, 22226292); // Giran
		ZONE_TRIGGERS.put(11027, 19216292); // Gludio
		ZONE_TRIGGERS.put(11034, 23246292); // Heine
		ZONE_TRIGGERS.put(11025, 17226292); // Gluddin
		ZONE_TRIGGERS.put(11033, 21166292); // Rune
		ZONE_TRIGGERS.put(11042, 17256292); // Faeron
		ZONE_TRIGGERS.put(11043, 26206292); // Arcan
		ZONE_TRIGGERS.put(11022, 16256292); // Talking Island
	}
	
	/**
	 * Initializes the {@code EventShrines} script.<br>
	 * This constructor registers all required zone IDs from the {@code ZONE_TRIGGERS} map.
	 */
	public EventShrines()
	{
		addEnterZoneId(ZONE_TRIGGERS.keySet());
	}
	
	/**
	 * Handles the logic when a creature enters a specific zone.<br>
	 * This method checks if the {@code creature} is a player and sends an {@code OnEventTrigger} packet based on the {@code ZoneType}.<br>
	 * It determines whether to activate or deactivate shrines based on the current server configuration.
	 * @param creature The {@code Creature} that entered the zone.
	 * @param zone The {@code ZoneType} of the area entered.
	 */
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer())
		{
			final int zoneId = zone.getId();
			if (EventShrineManager.getInstance().areShrinesEnabled())
			{
				creature.sendPacket(new OnEventTrigger(ZONE_TRIGGERS.get(zoneId), true));
			}
			else // Deactivate shrine.
			{
				creature.sendPacket(new OnEventTrigger(ZONE_TRIGGERS.get(zoneId) + 2, true));
			}
		}
	}
	
	/**
	 * This is the entry point for the {@code EventShrines} class.<br>
	 * It initializes a new instance of the script.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new EventShrines();
	}
}
