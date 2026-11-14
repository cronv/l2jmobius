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
package org.l2jmobius.gameserver.entity.item.holders;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.groups.Party;

/**
 * This class handles the logic for protecting items dropped by entities.<br>
 * It ensures that items are not lost or stolen under specific conditions.<br>
 * The {@code DropProtection} task is executed periodically to manage these protections.
 * @author DrHouse
 */
public class DropProtection implements Runnable
{
	private volatile boolean _isProtected = false;
	private Creature _owner = null;
	private ScheduledFuture<?> _task = null;
	
	private static final long PROTECTED_MILLIS_TIME = 15000;
	
	@Override
	public synchronized void run()
	{
		_isProtected = false;
		_owner = null;
		_task = null;
	}
	
	/**
	 * Checks if the item is currently protected.<br>
	 * This method returns {@code true} if protection is active.<br>
	 * It returns {@code false} otherwise.
	 * @return The current protection status of the item.
	 */
	public boolean isProtected()
	{
		return _isProtected;
	}
	
	/**
	 * Returns the {@code Creature} that owns this protection.<br>
	 * This is the entity currently associated with the drop protection.
	 * @return the {@code Creature} owner, or {@code null} if no owner exists.
	 */
	public Creature getOwner()
	{
		return _owner;
	}
	
	/**
	 * Checks if a {@code Player} is allowed to pick up an item.<br>
	 * This method returns {@code true} if the player is not blocked by protection rules.<br>
	 * It allows the owner, party members, or unprotected players to succeed.
	 * @param actor The {@code Player} attempting to pick up the item.
	 * @return {@code true} if the pickup is successful, {@code false} otherwise.
	 */
	public synchronized boolean tryPickUp(Player actor)
	{
		if (!_isProtected)
		{
			return true;
		}
		
		if (_owner == actor)
		{
			return true;
		}
		
		final Party ownerParty = _owner.getParty();
		if ((ownerParty != null) && (ownerParty == actor.getParty()))
		{
			return true;
		}
		
		/*
		 * if (_owner.getClan() != null && _owner.getClan() == actor.getClan()) return true;
		 */
		
		return false;
	}
	
	/**
	 * Attempts to pick up an item for a {@code Pet}.<br>
	 * This method delegates the action to the owner of the {@code Pet}.
	 * @param pet The {@code Pet} trying to pick up the item.
	 * @return {@code true} if the pickup was successful, {@code false} otherwise.
	 */
	public boolean tryPickUp(Pet pet)
	{
		return tryPickUp(pet.getOwner());
	}
	
	/**
	 * Removes the protection status from this item.<br>
	 * This method cancels any active scheduled tasks.<br>
	 * It resets the {@code _isProtected} flag to {@code false}.<br>
	 * The owner and task references are set to {@code null}.
	 */
	public synchronized void unprotect()
	{
		if (_task != null)
		{
			_task.cancel(false);
		}
		
		_isProtected = false;
		_owner = null;
		_task = null;
	}
	
	/**
	 * Sets the current owner of the protected item.<br>
	 * This method enables protection for a specific {@code Creature}.<br>
	 * It also schedules an automatic unprotection after a delay.
	 * @param creature The {@code Creature} that will own the protected item.
	 */
	public synchronized void protect(Creature creature)
	{
		unprotect();
		
		_isProtected = true;
		_owner = creature;
		if (_owner == null)
		{
			throw new NullPointerException("Trying to protect dropped item to null owner");
		}
		
		_task = ThreadPool.schedule(this, PROTECTED_MILLIS_TIME);
	}
}
