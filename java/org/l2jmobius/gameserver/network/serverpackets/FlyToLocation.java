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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to teleport a creature to a specific location.<br>
 * It is typically used when an entity gains the ability to fly or moves via special mechanics.
 * @author Mobius
 */
public class FlyToLocation extends ServerPacket
{
	private final int _destX;
	private final int _destY;
	private final int _destZ;
	private final int _chaObjId;
	private final int _orgX;
	private final int _orgY;
	private final int _orgZ;
	private final FlyType _type;
	private int _flySpeed;
	private int _flyDelay;
	private int _animationSpeed;
	
	/**
	 * Creates a packet to move a creature to a specific location.<br>
	 * This method sets the destination coordinates and flight type.<br>
	 * It also enables blinking for player characters.
	 * @param creature The {@code Creature} that will perform the movement.
	 * @param destX The target X coordinate.
	 * @param destY The target Y coordinate.
	 * @param destZ The target Z coordinate.
	 * @param type The {@code FlyType} used for the flight animation.
	 */
	public FlyToLocation(Creature creature, int destX, int destY, int destZ, FlyType type)
	{
		_chaObjId = creature.getObjectId();
		_orgX = creature.getX();
		_orgY = creature.getY();
		_orgZ = creature.getZ();
		_destX = destX;
		_destY = destY;
		_destZ = destZ;
		_type = type;
		if (creature.isPlayer())
		{
			creature.asPlayer().setBlinkActive(true);
		}
	}
	
	/**
	 * Creates a new {@code FlyToLocation} packet for a specific creature.<br>
	 * This packet handles the movement of an entity to a target destination.<br>
	 * It also enables blinking for players if applicable.
	 * @param creature The {@code Creature} that will perform the flight.
	 * @param destX The target X coordinate.
	 * @param destY The target Y coordinate.
	 * @param destZ The target Z coordinate.
	 * @param type The {@code FlyType} of the movement.
	 * @param flySpeed The speed at which the creature flies.
	 * @param flyDelay The delay before flight begins.
	 * @param animationSpeed The speed of the flight animation.
	 */
	public FlyToLocation(Creature creature, int destX, int destY, int destZ, FlyType type, int flySpeed, int flyDelay, int animationSpeed)
	{
		_chaObjId = creature.getObjectId();
		_orgX = creature.getX();
		_orgY = creature.getY();
		_orgZ = creature.getZ();
		_destX = destX;
		_destY = destY;
		_destZ = destZ;
		_type = type;
		_flySpeed = flySpeed;
		_flyDelay = flyDelay;
		_animationSpeed = animationSpeed;
		if (creature.isPlayer())
		{
			creature.asPlayer().setBlinkActive(true);
		}
	}
	
	/**
	 * Creates a new {@code FlyToLocation} packet for a specific creature.<br>
	 * This method uses the coordinates from an {@code ILocational} object.<br>
	 * It simplifies creating a flight movement request.
	 * @param creature The {@code Creature} that will perform the flight.
	 * @param dest The destination location of the flight.
	 * @param type The {@code FlyType} used for this movement.
	 */
	public FlyToLocation(Creature creature, ILocational dest, FlyType type)
	{
		this(creature, dest.getX(), dest.getY(), dest.getZ(), type);
	}
	
	/**
	 * Creates a new {@code FlyToLocation} packet for a specific creature.<br>
	 * This constructor uses an {@code ILocational} object to define the destination coordinates.<br>
	 * It sets all necessary flight parameters including speed and animation details.
	 * @param creature The {@code Creature} that is performing the flight action.
	 * @param dest The {@code ILocational} destination where the creature will fly.
	 * @param type The {@code FlyType} representing the style of flight.
	 * @param flySpeed The speed at which the creature flies.
	 * @param flyDelay The delay before the flight begins.
	 * @param animationSpeed The speed of the flight animation.
	 */
	public FlyToLocation(Creature creature, ILocational dest, FlyType type, int flySpeed, int flyDelay, int animationSpeed)
	{
		this(creature, dest.getX(), dest.getY(), dest.getZ(), type, flySpeed, flyDelay, animationSpeed);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.FLY_TO_LOCATION.writeId(this, buffer);
		buffer.writeInt(_chaObjId);
		buffer.writeInt(_destX);
		buffer.writeInt(_destY);
		buffer.writeInt(_destZ);
		buffer.writeInt(_orgX);
		buffer.writeInt(_orgY);
		buffer.writeInt(_orgZ);
		buffer.writeInt(_type.ordinal());
		buffer.writeInt(_flySpeed);
		buffer.writeInt(_flyDelay);
		buffer.writeInt(_animationSpeed);
	}
}
