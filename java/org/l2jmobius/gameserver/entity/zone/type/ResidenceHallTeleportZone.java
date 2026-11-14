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

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * This class defines a teleportation zone specifically for residence halls during clan hall sieges.<br>
 * It handles the logic required to move players into the siege area.
 * @author BiggBoss, Stayway
 */
public class ResidenceHallTeleportZone extends ResidenceTeleportZone
{
	private int _id;
	private ScheduledFuture<?> _teleTask;
	
	/**
	 * Creates a new {@code ResidenceHallTeleportZone} instance.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this teleport zone.
	 */
	public ResidenceHallTeleportZone(int id)
	{
		super(id);
	}
	
	/**
	 * Sets a specific configuration parameter for this zone.<br>
	 * It updates the {@code _id} if the name is {@code residenceZoneId}.<br>
	 * Otherwise, it passes the values to the parent class.
	 * @param name The name of the parameter to set.
	 * @param value The string value to assign to the parameter.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("residenceZoneId"))
		{
			_id = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	/**
	 * Retrieves the unique identifier for this residence zone.<br>
	 * This ID is used to identify specific teleport zones in the game world.
	 * @return The {@code int} value of the zone ID.
	 */
	public int getResidenceZoneId()
	{
		return _id;
	}
	
	/**
	 * Checks if the teleport task needs to be restarted.<br>
	 * It verifies if {@code _teleTask} is {@code null} or finished.<br>
	 * If needed, it schedules a new {@code TeleportTask} using {@code ThreadPool}.
	 */
	public synchronized void checkTeleportTask()
	{
		if ((_teleTask == null) || _teleTask.isDone())
		{
			_teleTask = ThreadPool.schedule(new TeleportTask(), 30000);
		}
	}
	
	protected class TeleportTask implements Runnable
	{
		@Override
		public void run()
		{
			final List<Location> spawns = getSpawns();
			if (spawns.isEmpty())
			{
				LOGGER.warning(ResidenceHallTeleportZone.class.getSimpleName() + ": No teleport spawn configured for zone " + getId() + ".");
				return;
			}
			
			final Location loc = spawns.get(spawns.size() > 1 ? Rnd.get(spawns.size()) : 0);
			if (loc == null)
			{
				LOGGER.warning(ResidenceHallTeleportZone.class.getSimpleName() + ": Null teleport spawn configured for zone " + getId() + ".");
				return;
			}
			
			for (Player pc : getPlayersInside())
			{
				if (pc != null)
				{
					pc.teleToLocation(loc, false);
				}
			}
		}
	}
}
