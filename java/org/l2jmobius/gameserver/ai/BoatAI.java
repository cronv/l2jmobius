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
package org.l2jmobius.gameserver.ai;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Boat;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.VehicleDeparture;
import org.l2jmobius.gameserver.network.serverpackets.VehicleInfo;
import org.l2jmobius.gameserver.network.serverpackets.VehicleStarted;

/**
 * Handles the artificial intelligence for {@code Boat} entities.<br>
 * This class manages how boats navigate and interact within the game world.
 * @author DS, Mobius
 */
public class BoatAI extends CreatureAI
{
	/**
	 * Creates a new {@code BoatAI} instance for a specific boat.<br>
	 * This constructor initializes the AI behavior for the provided {@code Boat}.
	 * @param boat The {@code Boat} object that this AI will control.
	 */
	public BoatAI(Boat boat)
	{
		super(boat);
	}
	
	/**
	 * Moves the boat to a specific set of coordinates.<br>
	 * This method checks if movement is enabled before proceeding.<br>
	 * It broadcasts the necessary packets to notify clients of the movement.
	 * @param x The target x coordinate.
	 * @param y The target y coordinate.
	 * @param z The target z coordinate.
	 */
	@Override
	protected void moveTo(int x, int y, int z)
	{
		if (_actor.isMovementDisabled())
		{
			return;
		}
		
		if (!_actor.isMoving())
		{
			_actor.broadcastPacket(new VehicleStarted(getActor(), 1));
		}
		
		_actor.moveToLocation(x, y, z, 0);
		_actor.broadcastPacket(new VehicleDeparture(getActor()));
	}
	
	/**
	 * Stops the movement of the boat at a specific location.<br>
	 * This method updates the actor's state and sends necessary packets to the client.<br>
	 * It ensures the vehicle is correctly synchronized if it was already moving.
	 * @param loc The {@code Location} where the boat should stop moving.
	 */
	@Override
	public void clientStopMoving(Location loc)
	{
		if (_actor.isMoving())
		{
			_actor.stopMove(loc);
			_actor.broadcastPacket(new VehicleStarted(getActor(), 0));
			_actor.broadcastPacket(new VehicleInfo(getActor()));
			return;
		}
		
		if (loc != null)
		{
			_actor.broadcastPacket(new VehicleStarted(getActor(), 0));
			_actor.broadcastPacket(new VehicleInfo(getActor()));
		}
	}
	
	/**
	 * Sends a notification to the player about the boat's movement.<br>
	 * This method checks if the actor is currently moving.<br>
	 * If it is moving, it sends a {@code VehicleDeparture} packet to the {@code Player}.
	 * @param player The {@code Player} who will receive the state update.
	 */
	@Override
	public void describeStateToPlayer(Player player)
	{
		if (!_actor.isMoving())
		{
			return;
		}
		
		player.sendPacket(new VehicleDeparture(getActor()));
	}
	
	/**
	 * Sets the AI's current goal to attack a specific target.<br>
	 * This method updates the behavior state of the {@code BoatAI}.
	 * @param target The {@code WorldObject} that the AI will attack.
	 */
	@Override
	public void setIntentionAttack(WorldObject target)
	{
	}
	
	/**
	 * Sets the current action of the boat to casting a skill.<br>
	 * This method defines how the boat interacts with a target and an item.
	 * @param skill The {@code Skill} that the boat will cast.
	 * @param target The {@code WorldObject} that is being targeted by the skill.
	 * @param item The {@code Item} required for the skill, if any.
	 * @param forceUse Whether to force the use of the skill even if conditions are not met.
	 * @param dontMove Whether the boat should remain stationary while casting.
	 */
	@Override
	public void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
	}
	
	/**
	 * Sets the AI's current goal to follow a specific target.<br>
	 * This updates the behavior of the {@code BoatAI}.
	 * @param target The {@code WorldObject} that the boat should follow.
	 */
	@Override
	public void setIntentionFollow(WorldObject target)
	{
	}
	
	/**
	 * Sets the current intention of the boat to pick up an item.<br>
	 * This updates the AI behavior to target a specific {@code WorldObject}.
	 * @param item The {@code WorldObject} that the boat should attempt to pick up.
	 */
	@Override
	public void setIntentionPickUp(WorldObject item)
	{
	}
	
	/**
	 * Sets the current interaction target for the boat.<br>
	 * This method tells the AI to interact with a specific {@code WorldObject}.
	 * @param object The {@code WorldObject} that the boat should interact with.
	 */
	@Override
	public void setIntentionInteract(WorldObject object)
	{
	}
	
	/**
	 * This method is called when an entity attacks the boat.<br>
	 * It allows the AI to react to being attacked by a {@code WorldObject}.
	 * @param attacker The {@code WorldObject} that initiated the attack.
	 */
	@Override
	public void notifyActionAttacked(WorldObject attacker)
	{
	}
	
	/**
	 * Notifies the AI that a target has become aggressive.<br>
	 * This method updates the internal state regarding an enemy.
	 * @param target The {@code WorldObject} that is showing aggression.
	 * @param aggro The level of aggression for the {@code target}.
	 */
	@Override
	public void notifyActionAggression(WorldObject target, int aggro)
	{
	}
	
	/**
	 * Notifies the system that the current action has been blocked.<br>
	 * This method is used to handle interruptions in the boat's behavior.
	 */
	@Override
	public void notifyActionBlocked()
	{
	}
	
	/**
	 * Notifies the system that the boat has been rooted.<br>
	 * This method is called when the entity cannot move due to a rooting effect.
	 */
	@Override
	public void notifyActionRooted()
	{
	}
	
	/**
	 * Tells the AI to stop tracking a specific target.<br>
	 * This method clears the current objective related to the provided {@code WorldObject}.<br>
	 * Use this when an entity is no longer relevant to the boat's actions.
	 * @param object The {@code WorldObject} that the AI should forget.
	 */
	@Override
	public void notifyActionForgetObject(WorldObject object)
	{
	}
	
	/**
	 * Notifies the system that the current action has been cancelled.<br>
	 * This method updates the internal state of the {@code BoatAI}.<br>
	 * It is used to stop any ongoing behaviors or movements.
	 */
	@Override
	public void notifyActionCancel()
	{
	}
	
	/**
	 * Notifies the system that an action has been completed due to death.<br>
	 * This method handles the cleanup of current actions when the entity dies.
	 */
	@Override
	public void notifyActionDeath()
	{
	}
	
	/**
	 * Notifies the system that a fake death action has occurred.<br>
	 * This method is used to trigger specific behaviors related to faking death.
	 */
	@Override
	public void notifyActionFakeDeath()
	{
	}
	
	/**
	 * Notifies the system that a casting action has finished.<br>
	 * This method is used to update the state of the boat after completing a skill.
	 */
	@Override
	public void notifyActionFinishCasting()
	{
	}
	
	/**
	 * Handles the logic for when a client action fails.<br>
	 * This method is called to notify the system that an intended action could not be completed.
	 */
	@Override
	protected void clientActionFailed()
	{
	}
	
	/**
	 * Moves a {@code WorldObject} to a new position.<br>
	 * The new position is calculated based on the target pawn and an offset.
	 * @param pawn The target {@code WorldObject} to move towards.
	 * @param offset The distance to move from the target.
	 */
	@Override
	public void moveToPawn(WorldObject pawn, int offset)
	{
	}
	
	/**
	 * This method is called when the boat arrives at the end of its path.<br>
	 * It synchronizes the stop position and heading to the clients.<br>
	 * Sends a {@code VehicleStarted} and {@code VehicleInfo} packet to all nearby players.
	 */
	@Override
	protected void clientStoppedMoving()
	{
		_actor.broadcastPacket(new VehicleStarted(getActor(), 0));
		_actor.broadcastPacket(new VehicleInfo(getActor()));
	}
	
	/**
	 * Retrieves the {@code Boat} associated with this AI.<br>
	 * This method casts the internal actor to a {@code Boat} object.
	 * @return The {@code Boat} instance.
	 */
	@Override
	public Boat getActor()
	{
		return (Boat) _actor;
	}
}
