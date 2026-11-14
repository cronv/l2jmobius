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

import java.util.concurrent.Future;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.stat.ControllableAirShipStat;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents an airship entity that can be controlled by a player.<br>
 * This class handles the specific logic for player-driven aerial transportation.
 */
public class ControllableAirShip extends AirShip
{
	private static final int HELM = 13556;
	private static final int LOW_FUEL = 40;
	
	private int _fuel = 0;
	private int _maxFuel = 0;
	
	private final int _ownerId;
	private int _helmId;
	private Player _captain = null;
	
	private Future<?> _consumeFuelTask;
	private Future<?> _checkTask;
	
	/**
	 * Creates a new instance of a {@code ControllableAirShip}.<br>
	 * This constructor initializes the ship using a specific template and owner.<br>
	 * It also assigns a unique ID for the helm.
	 * @param template The {@code CreatureTemplate} used to define the ship's properties.
	 * @param ownerId The unique identifier of the player who owns this ship.
	 */
	public ControllableAirShip(CreatureTemplate template, int ownerId)
	{
		super(template);
		setInstanceType(InstanceType.ControllableAirShip);
		_ownerId = ownerId;
		_helmId = IdManager.getInstance().getNextId(); // not forget to release !
	}
	
	/**
	 * Retrieves the statistics for this airship.<br>
	 * This method returns a {@code ControllableAirShipStat} object.
	 * @return the current {@code ControllableAirShipStat} of the ship.
	 */
	@Override
	public ControllableAirShipStat getStat()
	{
		return (ControllableAirShipStat) super.getStat();
	}
	
	/**
	 * Initializes the statistics for this {@code ControllableAirShip}.<br>
	 * It creates a new {@code ControllableAirShipStat} object.<br>
	 * This method sets the stats using the {@code setStat} method.
	 */
	@Override
	public void initCharStat()
	{
		setStat(new ControllableAirShipStat(this));
	}
	
	/**
	 * Checks if the airship is currently available for control.<br>
	 * It verifies that the base conditions are met and the ship is not in a dock.
	 * @return {@code true} if the ship can be controlled, {@code false} otherwise.
	 */
	@Override
	public boolean canBeControlled()
	{
		return super.canBeControlled() && !isInDock();
	}
	
	/**
	 * Checks if the given {@code Player} is the owner of this airship.<br>
	 * It returns {@code true} if the player's clan ID or object ID matches the owner ID.<br>
	 * If no owner exists, it returns {@code false}.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is the owner, {@code false} otherwise.
	 */
	@Override
	public boolean isOwner(Player player)
	{
		if (_ownerId == 0)
		{
			return false;
		}
		
		return (player.getClanId() == _ownerId) || (player.getObjectId() == _ownerId);
	}
	
	/**
	 * Retrieves the unique identifier of the owner.<br>
	 * This ID is assigned when the {@code ControllableAirShip} is created.
	 * @return The {@code int} value representing the owner's ID.
	 */
	@Override
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	/**
	 * Checks if the given {@code Player} is the captain of this ship.<br>
	 * It returns {@code true} only if the player matches the current captain.<br>
	 * Returns {@code false} if there is no captain or the player is not the captain.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is the captain, otherwise {@code false}.
	 */
	@Override
	public boolean isCaptain(Player player)
	{
		return (_captain != null) && (player == _captain);
	}
	
	/**
	 * Retrieves the unique identifier of the current captain.<br>
	 * Returns {@code 0} if no captain is assigned to this ship.
	 * @return The {@code int} object ID of the captain or {@code 0}.
	 */
	@Override
	public int getCaptainId()
	{
		return _captain != null ? _captain.getObjectId() : 0;
	}
	
	/**
	 * Retrieves the unique identifier for the helm.<br>
	 * This ID is used to identify the specific helm object associated with this air ship.
	 * @return The {@code int} value of the helm object ID.
	 */
	@Override
	public int getHelmObjectId()
	{
		return _helmId;
	}
	
	/**
	 * Retrieves the unique identifier for the helm item.<br>
	 * This value is used to identify the steering component of the airship.
	 * @return The {@code int} ID of the helm item.
	 */
	@Override
	public int getHelmItemId()
	{
		return HELM;
	}
	
	/**
	 * Sets the current captain of the airship.<br>
	 * This method checks if the {@code Player} meets all requirements to control the helm.<br>
	 * It validates conditions such as distance, combat status, and active skills.
	 * @param player The {@code Player} attempting to become the captain.
	 * @return {@code true} if the captain was successfully set, otherwise {@code false}.
	 */
	@Override
	public boolean setCaptain(Player player)
	{
		if (player == null)
		{
			_captain = null;
		}
		else
		{
			if ((_captain == null) && (player.getAirShip() == this))
			{
				final int x = player.getInVehiclePosition().getX() - 0x16e;
				final int y = player.getInVehiclePosition().getY();
				final int z = player.getInVehiclePosition().getZ() - 0x6b;
				if (((x * x) + (y * y) + (z * z)) > 2500)
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_BECAUSE_YOU_ARE_TOO_FAR);
					return false;
				}
				else if (player.isInCombat())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_HELM_WHILE_IN_A_BATTLE);
					return false;
				}
				else if (player.isSitting())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_HELM_WHILE_IN_A_SITTING_POSITION);
					return false;
				}
				else if (player.hasBlockActions() && player.hasAbnormalType(AbnormalType.PARALYZE))
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_HELM_WHILE_YOU_ARE_PETRIFIED);
					return false;
				}
				else if (player.isCursedWeaponEquipped())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_HELM_WHEN_A_CURSED_WEAPON_IS_EQUIPPED);
					return false;
				}
				else if (player.isFishing())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_HELM_WHILE_FISHING);
					return false;
				}
				else if (player.isDead() || player.isFakeDeath())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_HELM_WHEN_YOU_ARE_DEAD);
					return false;
				}
				else if (player.isCastingNow())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_HELM_WHILE_USING_A_SKILL);
					return false;
				}
				else if (player.isTransformed())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_HELM_WHILE_TRANSFORMED);
					return false;
				}
				else if (player.isCombatFlagEquipped())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_HELM_WHILE_HOLDING_A_FLAG);
					return false;
				}
				else if (player.isInDuel())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_HELM_WHILE_IN_A_DUEL);
					return false;
				}
				
				_captain = player;
				player.broadcastUserInfo();
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_HELM_BECAUSE_YOU_DO_NOT_MEET_THE_REQUIREMENTS);
				return false;
			}
		}
		
		updateAbnormalVisualEffects();
		return true;
	}
	
	/**
	 * Retrieves the current amount of fuel.<br>
	 * This value represents how much fuel is currently available in the {@code ControllableAirShip}.
	 * @return The current fuel level as an {@code int}.
	 */
	@Override
	public int getFuel()
	{
		return _fuel;
	}
	
	/**
	 * Sets the current fuel level of the airship.<br>
	 * The value is clamped between {@code 0} and {@code _maxFuel}.<br>
	 * This method also triggers system messages for low or empty fuel.
	 * @param f The new fuel amount to set.
	 */
	@Override
	public void setFuel(int f)
	{
		final int old = _fuel;
		if (f < 0)
		{
			_fuel = 0;
		}
		else if (f > _maxFuel)
		{
			_fuel = _maxFuel;
		}
		else
		{
			_fuel = f;
		}
		
		if ((_fuel == 0) && (old > 0))
		{
			broadcastToPassengers(new SystemMessage(SystemMessageId.THE_AIRSHIP_S_FUEL_EP_HAS_RUN_OUT_THE_AIRSHIP_S_SPEED_IS_GREATLY_DECREASED));
		}
		else if (_fuel < LOW_FUEL)
		{
			broadcastToPassengers(new SystemMessage(SystemMessageId.THE_AIRSHIP_S_FUEL_EP_WILL_SOON_RUN_OUT));
		}
	}
	
	/**
	 * Returns the maximum fuel capacity of the air ship.<br>
	 * This value is retrieved from the internal {@code _maxFuel} field.
	 * @return The maximum amount of fuel the ship can hold as an {@code int}.
	 */
	@Override
	public int getMaxFuel()
	{
		return _maxFuel;
	}
	
	/**
	 * Sets the maximum fuel capacity for this air ship.<br>
	 * This updates the internal {@code _maxFuel} value.
	 * @param mf The new maximum fuel amount.
	 */
	@Override
	public void setMaxFuel(int mf)
	{
		_maxFuel = mf;
	}
	
	/**
	 * Removes a {@code Player} from the airship.<br>
	 * If the player is the current captain, their role is cleared.<br>
	 * This method calls the superclass implementation to handle removal.
	 * @param player The {@code Player} to be removed from the ship.
	 */
	@Override
	public void oustPlayer(Player player)
	{
		if (player == _captain)
		{
			setCaptain(null); // no need to broadcast userinfo here
		}
		
		super.oustPlayer(player);
	}
	
	/**
	 * This method is called when the {@code ControllableAirShip} instance is created.<br>
	 * It initializes the background tasks for fuel consumption and status checks.<br>
	 * It also calls the parent class {@code onSpawn} method.
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_checkTask = ThreadPool.scheduleAtFixedRate(new CheckTask(), 60000, 10000);
		_consumeFuelTask = ThreadPool.scheduleAtFixedRate(new ConsumeFuelTask(), 60000, 60000);
	}
	
	/**
	 * Removes this airship from the game world.<br>
	 * This method cancels all active tasks and broadcasts a deletion packet.
	 * @return {@code true} if the object was successfully deleted, otherwise {@code false}.
	 */
	@Override
	public boolean deleteMe()
	{
		if (!super.deleteMe())
		{
			return false;
		}
		
		if (_checkTask != null)
		{
			_checkTask.cancel(false);
			_checkTask = null;
		}
		
		if (_consumeFuelTask != null)
		{
			_consumeFuelTask.cancel(false);
			_consumeFuelTask = null;
		}
		
		broadcastPacket(new DeleteObject(_helmId));
		return true;
	}
	
	/**
	 * Updates the unique identifier for this object.<br>
	 * This method calls {@code super.refreshId()} to update the base ID.<br>
	 * It also releases the old {@code _helmId} and assigns a new one from {@code IdManager}.
	 */
	@Override
	public void refreshId()
	{
		super.refreshId();
		IdManager.getInstance().releaseId(_helmId);
		_helmId = IdManager.getInstance().getNextId();
	}
	
	/**
	 * Sends the ship information to a specific player.<br>
	 * This method also sends the information to the captain if one exists.
	 * @param player The {@code Player} who will receive the information.
	 */
	@Override
	public void sendInfo(Player player)
	{
		super.sendInfo(player);
		if (_captain != null)
		{
			_captain.sendInfo(player);
		}
	}
	
	protected class ConsumeFuelTask implements Runnable
	{
		@Override
		public void run()
		{
			int fuel = getFuel();
			if (fuel > 0)
			{
				fuel -= 10;
				if (fuel < 0)
				{
					fuel = 0;
				}
				
				setFuel(fuel);
				updateAbnormalVisualEffects();
			}
		}
	}
	
	protected class CheckTask implements Runnable
	{
		@Override
		public void run()
		{
			if (isSpawned() && isEmpty() && !isInDock())
			{
				// deleteMe() can't be called from CheckTask because task should not cancel itself
				ThreadPool.execute(new DecayTask());
			}
		}
	}
	
	protected class DecayTask implements Runnable
	{
		@Override
		public void run()
		{
			deleteMe();
		}
	}
}
