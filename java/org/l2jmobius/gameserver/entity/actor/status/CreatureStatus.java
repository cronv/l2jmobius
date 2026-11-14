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
package org.l2jmobius.gameserver.entity.actor.status;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureHpChange;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * Represents the current status effects and conditions applied to a {@code Creature}.<br>
 * This class manages various buffs, debuffs, and active mechanics for entities in the game world.
 */
public class CreatureStatus
{
	protected static final Logger LOGGER = Logger.getLogger(CreatureStatus.class.getName());
	
	private final Creature _creature;
	
	private double _currentHp = 0; // Current HP of the Creature
	private double _currentMp = 0; // Current MP of the Creature
	
	/** Array containing all clients that need to be notified about hp/mp updates of the Creature */
	private Set<Creature> _StatusListener;
	
	private Future<?> _regTask;
	
	protected byte _flagsRegenActive = 0;
	
	protected static final byte REGEN_FLAG_CP = 4;
	private static final byte REGEN_FLAG_HP = 1;
	private static final byte REGEN_FLAG_MP = 2;
	
	/**
	 * Creates a new {@code CreatureStatus} instance for a specific creature.<br>
	 * This object tracks the health and mana of the provided {@code Creature}.
	 * @param creature The {@code Creature} to associate with this status object.
	 */
	public CreatureStatus(Creature creature)
	{
		_creature = creature;
	}
	
	/**
	 * Registers a new listener to receive status updates.<br>
	 * This method adds the specified {@code Creature} to the internal listener set.<br>
	 * It prevents adding the primary {@code _creature} as its own listener.
	 * @param object The {@code Creature} to be added as a listener.
	 */
	public void addStatusListener(Creature object)
	{
		if (object == _creature)
		{
			return;
		}
		
		getStatusListener().add(object);
	}
	
	/**
	 * Removes a specific listener from the status updates.<br>
	 * This stops the {@code Creature} from receiving notifications about HP or MP changes.
	 * @param object The {@code Creature} to remove as a listener.
	 */
	public void removeStatusListener(Creature object)
	{
		getStatusListener().remove(object);
	}
	
	/**
	 * Retrieves the set of creatures listening for status updates.<br>
	 * This method initializes the internal set if it is currently {@code null}.
	 * @return a {@code Set} of {@code Creature} objects that receive notifications.
	 */
	public Set<Creature> getStatusListener()
	{
		if (_StatusListener == null)
		{
			_StatusListener = ConcurrentHashMap.newKeySet();
		}
		
		return _StatusListener;
	}
	
	// place holder, only PcStatus has CP
	/**
	 * Reduces the current CP of the creature.<br>
	 * The amount to subtract is provided by the {@code value} parameter.
	 * @param value The amount of CP to remove.
	 */
	public void reduceCp(int value)
	{
	}
	
	/**
	 * Reduces the current health points of the creature.<br>
	 * This method updates the {@code _currentHp} value based on the provided amount.<br>
	 * It also records the {@code Creature} responsible for the damage.
	 * @param value The amount of health to subtract from the creature.
	 * @param attacker The {@code Creature} that performed the attack.
	 */
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	/**
	 * Reduces the health points of a {@code Creature}.<br>
	 * This method handles damage calculation and updates.
	 * @param value The amount of health to subtract.
	 * @param attacker The {@code Creature} that caused the reduction.
	 * @param isHpConsumption Whether this reduction counts as an HP consumption action.
	 */
	public void reduceHp(double value, Creature attacker, boolean isHpConsumption)
	{
		reduceHp(value, attacker, true, false, isHpConsumption);
	}
	
	/**
	 * Reduces the current HP of the creature.<br>
	 * This method handles damage logic including invulnerability checks and status effect removals.<br>
	 * It also triggers death if the HP falls below a certain threshold.
	 * @param value The amount of HP to subtract from the creature.
	 * @param attacker The {@code Creature} that initiated the damage.
	 * @param awake Whether the creature is currently awake.
	 * @param isDOT Whether the damage is from a Damage Over Time effect.
	 * @param isHPConsumption Whether the damage is caused by HP consumption.
	 */
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption)
	{
		final Creature creature = _creature;
		if (creature.isDead())
		{
			return;
		}
		
		// invul handling
		if (creature.isHpBlocked() && !(isDOT || isHPConsumption))
		{
			return;
		}
		
		if (attacker != null)
		{
			final Player attackerPlayer = attacker.asPlayer();
			if ((attackerPlayer != null) && attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage())
			{
				return;
			}
		}
		
		if (!isDOT && !isHPConsumption)
		{
			if (awake)
			{
				creature.stopEffectsOnDamage();
			}
			
			if (Formulas.calcStunBreak(creature))
			{
				creature.stopStunning(true);
			}
			
			if (Formulas.calcRealTargetBreak())
			{
				_creature.getEffectList().stopEffects(AbnormalType.REAL_TARGET);
			}
		}
		
		if (value > 0)
		{
			setCurrentHp(Math.max(_currentHp - value, creature.isUndying() ? 1 : 0));
		}
		
		if ((creature.getCurrentHp() < 0.5)) // Die
		{
			creature.doDie(attacker);
		}
	}
	
	/**
	 * Reduces the current MP of the creature.<br>
	 * The value cannot drop below {@code 0}.
	 * @param value The amount of MP to subtract.
	 */
	public void reduceMp(double value)
	{
		setCurrentMp(Math.max(_currentMp - value, 0));
	}
	
	/**
	 * Starts the periodic regeneration of health and mana for the creature.<br>
	 * This method schedules a background task to update stats based on the regeneration period.<br>
	 * It only starts if the creature is alive and no task is currently running.
	 */
	public synchronized void startHpMpRegeneration()
	{
		if ((_regTask == null) && !_creature.isDead())
		{
			// Get the Regeneration period.
			final int period = Formulas.getRegeneratePeriod(_creature);
			
			// Create the HP/MP/CP Regeneration task.
			_regTask = ThreadPool.scheduleAtFixedRate(this::doRegeneration, period, period);
		}
	}
	
	/**
	 * Stops the active regeneration of HP and MP for the creature.<br>
	 * This method cancels the running {@code Future} task if it exists.<br>
	 * It also resets the {@code _flagsRegenActive} flag to {@code 0}.
	 */
	public synchronized void stopHpMpRegeneration()
	{
		if (_regTask != null)
		{
			// Stop the HP/MP/CP Regeneration task.
			_regTask.cancel(false);
			_regTask = null;
			
			// Set the RegenActive flag to false.
			_flagsRegenActive = 0;
		}
	}
	
	// place holder, only PcStatus has CP
	/**
	 * Retrieves the current CP value of the creature.<br>
	 * This method currently returns a default value of {@code 0}.
	 * @return The current CP as a {@code double}.
	 */
	public double getCurrentCp()
	{
		return 0;
	}
	
	// place holder, only PcStatus has CP
	/**
	 * Updates the current CP value of the creature.<br>
	 * This method sets the internal {@code _currentCp} field to a new value.
	 * @param newCp The new CP value to set.
	 */
	public void setCurrentCp(double newCp)
	{
	}
	
	// place holder, only PcStatus has CP
	/**
	 * Updates the current CP of the creature.<br>
	 * This method allows for an optional network broadcast to other clients.
	 * @param newCp The new CP value to set.
	 * @param broadcastPacket Set to {@code true} to send a packet update, or {@code false} to skip it.
	 */
	public void setCurrentCp(double newCp, boolean broadcastPacket)
	{
	}
	
	/**
	 * Retrieves the current health points of the creature.
	 * @return The current {@code double} value of HP.
	 */
	public double getCurrentHp()
	{
		return _currentHp;
	}
	
	/**
	 * Updates the current health points of the creature.<br>
	 * This method sets the internal {@code _currentHp} value to a new amount.
	 * @param newHp The new health point value to set.
	 */
	public void setCurrentHp(double newHp)
	{
		setCurrentHp(newHp, true);
	}
	
	/**
	 * Updates the current health points of the {@code Creature}.<br>
	 * This method manages regeneration flags based on the new value.<br>
	 * It also handles optional network broadcasting and event notifications.
	 * @param newHp The new health point value to set.
	 * @param broadcastPacket Set to {@code true} to send a status update packet to all clients.
	 * @return {@code true} if the health value was actually changed, otherwise {@code false}.
	 */
	public boolean setCurrentHp(double newHp, boolean broadcastPacket)
	{
		// Get the Max HP of the Creature.
		final long oldHp = (long) _currentHp;
		final double maxHp = _creature.getStat().getMaxHp();
		
		synchronized (this)
		{
			if (_creature.isDead())
			{
				return false;
			}
			
			if (newHp >= maxHp)
			{
				// Set the RegenActive flag to false.
				_currentHp = maxHp;
				_flagsRegenActive &= ~REGEN_FLAG_HP;
				
				// Stop the HP/MP/CP Regeneration task.
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true.
				_currentHp = newHp;
				_flagsRegenActive |= REGEN_FLAG_HP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority.
				startHpMpRegeneration();
			}
		}
		
		final boolean hpWasChanged = oldHp != _currentHp;
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform.
		if (hpWasChanged)
		{
			if (broadcastPacket)
			{
				_creature.broadcastStatusUpdate();
			}
			
			final Creature creature = getActiveChar();
			if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_HP_CHANGE, creature))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnCreatureHpChange(creature, oldHp, _currentHp), creature);
			}
		}
		
		return hpWasChanged;
	}
	
	/**
	 * Updates both the current HP and MP of the {@code Creature}.<br>
	 * This method broadcasts a status update if either value changes.
	 * @param newHp The new health points to set.
	 * @param newMp The new mana points to set.
	 */
	public void setCurrentHpMp(double newHp, double newMp)
	{
		boolean hpOrMpWasChanged = setCurrentHp(newHp, false);
		hpOrMpWasChanged |= setCurrentMp(newMp, false);
		if (hpOrMpWasChanged)
		{
			_creature.broadcastStatusUpdate();
		}
	}
	
	/**
	 * Retrieves the current mana points of the creature.
	 * @return The current {@code double} value of {@code _currentMp}.
	 */
	public double getCurrentMp()
	{
		return _currentMp;
	}
	
	/**
	 * Updates the current mana points of the creature.<br>
	 * This method sets the {@code _currentMp} value to a new amount.
	 * @param newMp The new mana point value to set.
	 */
	public void setCurrentMp(double newMp)
	{
		setCurrentMp(newMp, true);
	}
	
	/**
	 * Updates the current MP of the {@code Creature}.<br>
	 * This method manages regeneration flags based on the new value.<br>
	 * It also handles broadcasting updates to other players if requested.
	 * @param newMp The new MP value to set for the creature.
	 * @param broadcastPacket Set to {@code true} to notify all clients of the change.
	 * @return {@code true} if the MP value was actually changed, otherwise {@code false}.
	 */
	public boolean setCurrentMp(double newMp, boolean broadcastPacket)
	{
		// Get the Max MP of the Creature.
		final int currentMp = (int) _currentMp;
		final int maxMp = _creature.getStat().getMaxMp();
		
		synchronized (this)
		{
			if (_creature.isDead())
			{
				return false;
			}
			
			if (newMp >= maxMp)
			{
				// Set the RegenActive flag to false.
				_currentMp = maxMp;
				_flagsRegenActive &= ~REGEN_FLAG_MP;
				
				// Stop the HP/MP/CP Regeneration task.
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true.
				_currentMp = newMp;
				_flagsRegenActive |= REGEN_FLAG_MP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority.
				startHpMpRegeneration();
			}
		}
		
		final boolean mpWasChanged = currentMp != _currentMp;
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform.
		if (mpWasChanged && broadcastPacket)
		{
			_creature.broadcastStatusUpdate();
		}
		
		return mpWasChanged;
	}
	
	/**
	 * Updates the current HP and MP of the {@code Creature}.<br>
	 * This method calculates the new values based on regeneration rates.<br>
	 * It broadcasts the updated status to all listeners.<br>
	 * If the creature is dead or at full health, it stops the regeneration process.
	 */
	protected void doRegeneration()
	{
		// Modify the current HP/MP of the Creature and broadcast Server->Client packet StatusUpdate.
		if (!_creature.isDead() && ((_currentHp < _creature.getMaxRecoverableHp()) || (_currentMp < _creature.getMaxRecoverableMp())))
		{
			final double newHp = _currentHp + _creature.getStat().getValue(Stat.REGENERATE_HP_RATE);
			final double newMp = _currentMp + _creature.getStat().getValue(Stat.REGENERATE_MP_RATE);
			setCurrentHpMp(newHp, newMp);
		}
		else
		{
			stopHpMpRegeneration();
		}
	}
	
	/**
	 * Retrieves the {@code Creature} associated with this status.
	 * @return the {@code Creature} object.
	 */
	public Creature getActiveChar()
	{
		return _creature;
	}
}
