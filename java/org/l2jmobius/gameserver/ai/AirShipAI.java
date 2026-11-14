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
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExMoveToLocationAirShip;
import org.l2jmobius.gameserver.network.serverpackets.ExStopMoveAirShip;

/**
 * Handles the artificial intelligence logic for {@code AirShip} entities.<br>
 * It manages movement patterns and interactions for ships in the game world.
 * @author DS, Mobius
 */
public class AirShipAI extends CreatureAI
{
	/**
	 * Creates a new instance of {@code AirShipAI}.<br>
	 * This constructor initializes the AI for a specific {@code AirShip}.
	 * @param airShip The {@code AirShip} object that this AI will control.
	 */
	public AirShipAI(AirShip airShip)
	{
		super(airShip);
	}
	
	/**
	 * Moves the actor to a specific set of coordinates.<br>
	 * This method checks if movement is enabled before updating the position.<br>
	 * It also sends a network packet to update the client.
	 * @param x The target x-coordinate.
	 * @param y The target y-coordinate.
	 * @param z The target z-coordinate.
	 */
	@Override
	protected void moveTo(int x, int y, int z)
	{
		if (_actor.isMovementDisabled())
		{
			return;
		}
		
		_actor.moveToLocation(x, y, z, 0);
		_actor.broadcastPacket(new ExMoveToLocationAirShip(getActor()));
	}
	
	/**
	 * Stops the movement of the airship.<br>
	 * This method updates the actor state and sends a packet to the client.
	 * @param loc The {@code Location} where the movement should stop.
	 */
	@Override
	public void clientStopMoving(Location loc)
	{
		if (_actor.isMoving())
		{
			_actor.stopMove(loc);
			_actor.broadcastPacket(new ExStopMoveAirShip(getActor()));
			return;
		}
		
		if (loc != null)
		{
			_actor.broadcastPacket(new ExStopMoveAirShip(getActor()));
		}
	}
	
	/**
	 * Sends the current movement status of the airship to a specific player.<br>
	 * This method checks if the actor is currently moving before sending the packet.<br>
	 * It uses the {@code ExMoveToLocationAirShip} packet to update the client.
	 * @param player The {@code Player} who will receive the movement information.
	 */
	@Override
	public void describeStateToPlayer(Player player)
	{
		if (!_actor.isMoving())
		{
			return;
		}
		
		player.sendPacket(new ExMoveToLocationAirShip(getActor()));
	}
	
	/**
	 * Sets the current attack target for the air ship.<br>
	 * This method tells the AI to focus on a specific {@code WorldObject}.
	 * @param target The {@code WorldObject} that the air ship will attack.
	 */
	@Override
	public void setIntentionAttack(WorldObject target)
	{
	}
	
	/**
	 * Sets the current action of the AI to casting a skill.<br>
	 * This method defines how the AI interacts with a target and an item.<br>
	 * It determines if the AI should force the use or stay in place.
	 * @param skill The {@code Skill} that the AI will cast.
	 * @param target The {@code WorldObject} that is being targeted by the skill.
	 * @param item The {@code Item} required for the skill, if any.
	 * @param forceUse Set to {@code true} to force the use of the item.
	 * @param dontMove Set to {@code true} to prevent the AI from moving while casting.
	 */
	@Override
	public void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
	}
	
	/**
	 * Sets the current intention of the AI to follow a specific target.<br>
	 * This updates the behavior state for the {@code AirShipAI}.
	 * @param target The {@code WorldObject} that the AI should follow.
	 */
	@Override
	public void setIntentionFollow(WorldObject target)
	{
	}
	
	/**
	 * Sets the current intention of the AI to pick up an item.<br>
	 * This updates the internal state for the next action cycle.
	 * @param item The {@code WorldObject} that represents the item to be picked up.
	 */
	@Override
	public void setIntentionPickUp(WorldObject item)
	{
	}
	
	/**
	 * Sets the current interaction target for the AI.<br>
	 * This method tells the AI to interact with a specific {@code WorldObject}.
	 * @param object The {@code WorldObject} that the AI will interact with.
	 */
	@Override
	public void setIntentionInteract(WorldObject object)
	{
	}
	
	/**
	 * This method is called when an entity attacks the air ship.<br>
	 * It allows the AI to react to being attacked by a specific target.
	 * @param attacker The {@code WorldObject} that initiated the attack.
	 */
	@Override
	public void notifyActionAttacked(WorldObject attacker)
	{
	}
	
	/**
	 * Notifies the system that an aggression action has occurred.<br>
	 * This method updates the state of the AI regarding a specific target.
	 * @param target The {@code WorldObject} being targeted by the aggression.
	 * @param aggro The amount of aggression value to apply.
	 */
	@Override
	public void notifyActionAggression(WorldObject target, int aggro)
	{
	}
	
	/**
	 * Notifies the system that an action has been blocked.<br>
	 * This method is used to handle interruptions in current behaviors.
	 */
	@Override
	public void notifyActionBlocked()
	{
	}
	
	/**
	 * Notifies the system that the entity is currently rooted.<br>
	 * This method signals that movement is restricted.
	 */
	@Override
	public void notifyActionRooted()
	{
	}
	
	/**
	 * Tells the AI to stop tracking a specific target.<br>
	 * This method is used when an object is no longer relevant to the current action.
	 * @param object The {@code WorldObject} that should be forgotten.
	 */
	@Override
	public void notifyActionForgetObject(WorldObject object)
	{
	}
	
	/**
	 * Notifies the system that the current action has been cancelled.<br>
	 * This method updates the internal state of the {@code AirShipAI}.<br>
	 * It is used to stop any ongoing behaviors or movements.
	 */
	@Override
	public void notifyActionCancel()
	{
	}
	
	/**
	 * Notifies the system that an action has ended due to death.<br>
	 * This method is called when the entity dies while performing a task.
	 */
	@Override
	public void notifyActionDeath()
	{
	}
	
	/**
	 * This method notifies the system that a fake death action has occurred.<br>
	 * It is used to trigger specific animations or effects without actually killing the entity.
	 */
	@Override
	public void notifyActionFakeDeath()
	{
	}
	
	/**
	 * Notifies the system that a casting action has finished.<br>
	 * This method is called when an entity completes its current skill cast.
	 */
	@Override
	public void notifyActionFinishCasting()
	{
	}
	
	/**
	 * Handles the logic for when a client action fails.<br>
	 * This method is called to notify the system of an interrupted or failed task.
	 */
	@Override
	protected void clientActionFailed()
	{
	}
	
	/**
	 * Moves the {@code WorldObject} to a new position.<br>
	 * The new position is calculated based on the {@code pawn} and the provided {@code offset}.
	 * @param pawn The target object used as a reference point for movement.
	 * @param offset The distance or coordinate adjustment to apply to the {@code pawn}.
	 */
	@Override
	public void moveToPawn(WorldObject pawn, int offset)
	{
	}
	
	/**
	 * This method is called when the airship arrives at the end of its path.<br>
	 * It synchronizes the stop position and heading to the clients.<br>
	 * Sends an {@code ExStopMoveAirShip} packet to all nearby players.
	 */
	@Override
	protected void clientStoppedMoving()
	{
		_actor.broadcastPacket(new ExStopMoveAirShip(getActor()));
	}
	
	/**
	 * Retrieves the {@code AirShip} associated with this AI instance.<br>
	 * This method casts the internal actor to an {@code AirShip} type.
	 * @return The {@code AirShip} entity.
	 */
	@Override
	public AirShip getActor()
	{
		return (AirShip) _actor;
	}
}
