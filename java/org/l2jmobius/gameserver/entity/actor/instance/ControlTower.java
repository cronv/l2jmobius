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
package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Tower;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;

/**
 * Represents a {@code ControlTower} instance within the game world.<br>
 * This class handles the specific logic and behavior for this type of tower entity.
 * @author Mobius
 */
public class ControlTower extends Tower
{
	private static final int LIFE_CONTROL_TOWER = 13002;
	private static final int LIFE_CONTROL_TOWER_WRECK = 13003;
	
	private Set<Spawn> _guards;
	
	/**
	 * Creates a new {@code ControlTower} instance.<br>
	 * This constructor initializes the tower using a provided template.<br>
	 * It sets the {@code InstanceType} to {@code ControlTower}.
	 * @param template The {@code NpcTemplate} used to define the tower properties.
	 */
	public ControlTower(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.ControlTower);
	}
	
	/**
	 * Handles the logic when this tower is destroyed.<br>
	 * It notifies the siege system and stops guard respawns if a siege is active.
	 * @param killer The {@code Creature} that killed the tower.
	 * @return {@code true} if the death was successful, {@code false} otherwise.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		if (getCastle().getSiege().isInProgress())
		{
			getCastle().getSiege().killedCT(this);
			
			// A destroyed Life Control Tower leaves its wreck behind.
			if (getId() == LIFE_CONTROL_TOWER)
			{
				getCastle().getSiege().spawnTowerWreck(this, LIFE_CONTROL_TOWER_WRECK);
			}
			
			if ((_guards != null) && !_guards.isEmpty())
			{
				for (Spawn spawn : _guards)
				{
					if (spawn == null)
					{
						continue;
					}
					
					try
					{
						spawn.stopRespawn();
						// spawn.getLastSpawn().doDie(spawn.getLastSpawn());
					}
					catch (Exception e)
					{
						LOGGER.log(Level.WARNING, "Error at ControlTower", e);
					}
				}
				
				_guards.clear();
			}
		}
		
		return super.doDie(killer);
	}
	
	/**
	 * Adds a new {@code Spawn} to the list of guards.<br>
	 * This method links a specific guard to this {@code ControlTower}.
	 * @param guard The {@code Spawn} object to register.
	 */
	public void registerGuard(Spawn guard)
	{
		getGuards().add(guard);
	}
	
	/**
	 * Retrieves the set of guards associated with this tower.<br>
	 * This method ensures that the {@code _guards} collection is initialized before returning it.
	 * @return a {@code Set} of {@code Spawn} objects representing the guards.
	 */
	private Set<Spawn> getGuards()
	{
		if (_guards == null)
		{
			synchronized (this)
			{
				if (_guards == null)
				{
					_guards = ConcurrentHashMap.newKeySet();
				}
			}
		}
		
		return _guards;
	}
}
