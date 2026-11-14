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

import java.util.concurrent.Future;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * Represents a specific area that deals damage to entities within its boundaries.<br>
 * This {@code DamageZone} class handles the logic for applying periodic or instant damage effects.
 * @author durgus
 */
public class DamageZone extends ZoneType
{
	private int _damageHPPerSec;
	private int _damageMPPerSec;
	
	private int _castleId;
	private Castle _castle;
	
	private int _startTask;
	private int _reuseTask;
	protected volatile Future<?> _task;
	
	/**
	 * Creates a new {@code DamageZone} instance.<br>
	 * This constructor initializes the zone with default damage values.<br>
	 * It sets the target type to {@code InstanceType.Playable}.
	 * @param id The unique identifier for this zone.
	 */
	public DamageZone(int id)
	{
		super(id);
		
		// Setup default damage
		_damageHPPerSec = 200;
		_damageMPPerSec = 0;
		
		// Setup default start / reuse time.
		_startTask = 10;
		_reuseTask = 5000;
		
		// no castle by default
		_castleId = 0;
		_castle = null;
		
		setTargetType(InstanceType.Playable); // default only playabale
	}
	
	/**
	 * Updates a specific property of the {@code DamageZone}.<br>
	 * This method parses the {@code value} based on the provided {@code name}.<br>
	 * It handles properties like damage rates and castle IDs.<br>
	 * If the name is unknown, it calls the parent class method.
	 * @param name The name of the property to update.
	 * @param value The string representation of the new value.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("dmgHPSec"))
		{
			_damageHPPerSec = Integer.parseInt(value);
		}
		else if (name.equals("dmgMPSec"))
		{
			_damageMPPerSec = Integer.parseInt(value);
		}
		else if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
		}
		else if (name.equalsIgnoreCase("initialDelay"))
		{
			_startTask = Integer.parseInt(value);
		}
		else if (name.equalsIgnoreCase("reuse"))
		{
			_reuseTask = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It starts a periodic task to apply damage if applicable.<br>
	 * The task only runs if the zone has active damage parameters.<br>
	 * It checks for specific siege conditions if the zone belongs to a {@code Castle}.
	 * @param creature The {@code Creature} that entered the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		Future<?> task = _task;
		if ((task == null) && ((_damageHPPerSec != 0) || (_damageMPPerSec != 0)))
		{
			final Player player = creature.asPlayer();
			final Castle castle = getCastle();
			if (castle != null) // Castle zone
			{
				if (!(castle.getSiege().isInProgress() && (player != null) && (player.getSiegeState() != 2))) // Siege and no defender
				{
					return;
				}
			}
			
			synchronized (this)
			{
				task = _task;
				if (task == null)
				{
					_task = task = ThreadPool.scheduleAtFixedRate(new ApplyDamage(), _startTask, _reuseTask);
				}
			}
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It checks if any creatures remain inside the area.<br>
	 * If the area is empty, it cancels the active background task.
	 * @param creature The {@code Creature} that is exiting the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (getCharactersInside().isEmpty() && (_task != null))
		{
			_task.cancel(true);
			_task = null;
		}
	}
	
	/**
	 * Retrieves the amount of health points lost per second. <br>
	 * This value is specific to the {@code DamageZone}.
	 * @return the current {@code _damageHPPerSec} value.
	 */
	protected int getHPDamagePerSecond()
	{
		return _damageHPPerSec;
	}
	
	/**
	 * Returns the amount of mana damage dealt per second.<br>
	 * This value is used to calculate how much {@code MP} a creature loses over time.
	 * @return the current {@code _damageMPPerSec} value.
	 */
	protected int getMPDamagePerSecond()
	{
		return _damageMPPerSec;
	}
	
	/**
	 * Retrieves the {@code Castle} associated with this damage zone.<br>
	 * It fetches the castle from {@code CastleManager} if it is not already loaded.
	 * @return the {@code Castle} object, or {@code null} if no castle exists.
	 */
	protected Castle getCastle()
	{
		if ((_castleId > 0) && (_castle == null))
		{
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		}
		
		return _castle;
	}
	
	private class ApplyDamage implements Runnable
	{
		private final Castle _castle;
		
		protected ApplyDamage()
		{
			_castle = getCastle();
		}
		
		@Override
		public void run()
		{
			if (getCharactersInside().isEmpty())
			{
				if (_task != null)
				{
					_task.cancel(false);
					_task = null;
				}
				return;
			}
			
			if (!isEnabled())
			{
				return;
			}
			
			boolean siege = false;
			if (_castle != null)
			{
				siege = _castle.getSiege().isInProgress();
				
				// Castle zones active only during siege.
				if (!siege)
				{
					_task.cancel(false);
					_task = null;
					return;
				}
			}
			
			for (Creature character : getCharactersInside())
			{
				if ((character != null) && character.isPlayer() && !character.isDead())
				{
					if (siege)
					{
						// During siege defenders not affected.
						final Player player = character.asPlayer();
						if ((player != null) && player.isInSiege() && (player.getSiegeState() == 2))
						{
							continue;
						}
					}
					
					final double multiplier = 1 + (character.getStat().getValue(Stat.DAMAGE_ZONE_VULN, 0) / 100);
					if (getHPDamagePerSecond() != 0)
					{
						character.reduceCurrentHp(getHPDamagePerSecond() * multiplier, character, null);
					}
					
					if (getMPDamagePerSecond() != 0)
					{
						character.reduceCurrentMp(getMPDamagePerSecond() * multiplier);
					}
				}
			}
		}
	}
}
