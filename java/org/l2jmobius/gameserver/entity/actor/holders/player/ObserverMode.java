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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.cubic.Cubic;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyMessageType;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameManager;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadMode;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoCubic;
import org.l2jmobius.gameserver.network.serverpackets.ObservationMode;
import org.l2jmobius.gameserver.network.serverpackets.ObservationReturn;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMode;

/**
 * Manages the observer mode of a {@code Player}.<br>
 * It drives entering and leaving spectator state, both the free form and the olympiad form.
 * @author Mobius
 */
public class ObserverMode
{
	private final Player _player;
	private boolean _active = false;
	private ScheduledFuture<?> _leaveTask = null;
	
	/**
	 * Creates a new {@code ObserverMode} for a specific player.
	 * @param player The {@code Player} object associated with this instance.
	 */
	public ObserverMode(Player player)
	{
		_player = player;
	}
	
	/**
	 * Sets whether the actor is in observing mode.<br>
	 * This method updates several status flags based on the provided {@code value}.<br>
	 * It also clears the current target and adjusts AI intentions if necessary.
	 * @param value The boolean state to set for observation mode.
	 */
	public void setObserving(boolean value)
	{
		_active = value;
		_player.setTarget(null);
		_player.setBlockActions(value);
		_player.setInvul(value);
		_player.setInvisible(value);
		if (_player.hasAI() && !value)
		{
			_player.getAI().setIntentionIdle();
		}
	}
	
	/**
	 * Switches the player into observer mode.<br>
	 * This method removes any active hide effects and updates the player's status.<br>
	 * It also teleports the player to a new location and broadcasts their information.
	 * @param loc The {@code Location} where the player will be moved.
	 */
	public void enter(Location loc)
	{
		if (!_player.isInOlympiadMode())
		{
			_player.setLastLocation();
		}
		
		// Remove Hide.
		_player.getEffectList().stopEffects(AbnormalType.HIDE);
		
		setObserving(true);
		_player.sendPacket(new ObservationMode(loc));
		_player.teleToLocation(loc, false);
		_player.broadcastUserInfo();
		
		// Observation lasts one hour per use.
		_leaveTask = ThreadPool.schedule(() ->
		{
			if (_active && _player.isOnline())
			{
				leave();
			}
		}, 3600000);
	}
	
	/**
	 * Enters the observer mode for an olympiad.<br>
	 * This method handles all necessary state changes for the player.<br>
	 * It removes active summons and effects while setting invulnerability.<br>
	 * The player is then teleported to the specified location.
	 * @param loc The {@code Location} where the player will be moved.
	 * @param id The unique identifier of the olympiad game.
	 */
	public void enterOlympiad(Location loc, int id)
	{
		final Pet pet = _player.getPet();
		if (pet != null)
		{
			pet.unSummon(_player);
		}
		
		if (_player.hasServitors())
		{
			_player.getServitors().values().forEach(s -> s.unSummon(_player));
		}
		
		// Remove Hide.
		_player.getEffectList().stopEffects(AbnormalType.HIDE);
		
		final CubicList cubicList = _player.getCubicList();
		if (!cubicList.isEmpty())
		{
			cubicList.getCubics().values().forEach(Cubic::deactivate);
			cubicList.clear();
			_player.sendPacket(new ExUserInfoCubic(_player));
		}
		
		final Party party = _player.getParty();
		if (party != null)
		{
			party.removePartyMember(_player, PartyMessageType.EXPELLED);
		}
		
		_player.setOlympiadGameId(id);
		if (_player.isSitting())
		{
			_player.standUp();
		}
		
		if (!_active && !_player.isInOlympiadMode())
		{
			_player.setLastLocation();
		}
		
		_active = true;
		_player.setTarget(null);
		_player.setInvul(true);
		_player.setInvisible(true);
		_player.setInstance(OlympiadGameManager.getInstance().getOlympiadTask(id).getStadium().getInstance());
		_player.teleToLocation(loc, false);
		_player.sendPacket(new ExOlympiadMode(OlympiadMode.SPECTATOR));
		_player.broadcastUserInfo();
	}
	
	/**
	 * Exits the observer mode for the player.<br>
	 * Resets target and instance information.<br>
	 * Teleports the player to their last known location.<br>
	 * Restores normal visibility, invulnerability, and AI behavior.
	 */
	public void leave()
	{
		if (_leaveTask != null)
		{
			_leaveTask.cancel(false);
			_leaveTask = null;
		}
		
		_player.setTarget(null);
		_player.setInstance(null);
		_player.teleToLocation(_player.getLastLocation(), false);
		_player.unsetLastLocation();
		_player.sendPacket(new ObservationReturn(_player.getLocation()));
		_player.setBlockActions(false);
		if (!_player.isGM())
		{
			_player.setInvisible(false);
			_player.setInvul(false);
		}
		
		if (_player.hasAI())
		{
			_player.getAI().setIntentionIdle();
		}
		
		_player.setFalling(); // Prevent receive falling damage.
		_active = false;
		_player.broadcastUserInfo();
	}
	
	/**
	 * Exits the olympiad observer mode for the actor.<br>
	 * Resets the {@code _olympiadGameId} and {@code _active} variables to their default values.<br>
	 * Clears the target and instance information.<br>
	 * Teleports the actor back to their last known location.
	 */
	public void leaveOlympiad()
	{
		if (_player.getOlympiadGameId() == -1)
		{
			return;
		}
		
		_player.setOlympiadGameId(-1);
		_active = false;
		_player.setTarget(null);
		_player.sendPacket(new ExOlympiadMode(OlympiadMode.NONE));
		_player.setInstance(null);
		_player.teleToLocation(_player.getLastLocation(), true);
		if (!_player.isGM())
		{
			_player.setInvisible(false);
			_player.setInvul(false);
		}
		
		if (_player.hasAI())
		{
			_player.getAI().setIntentionIdle();
		}
		
		_player.unsetLastLocation();
		_player.broadcastUserInfo();
	}
	
	/**
	 * Checks if the actor is currently in observer mode.<br>
	 * This state determines how the entity interacts with the game world.
	 * @return {@code true} if the actor is an observer, {@code false} otherwise.
	 */
	public boolean isActive()
	{
		return _active;
	}
}
