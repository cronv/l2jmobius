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
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to synchronize riding information.<br>
 * It informs the player about their current mount status and position.
 */
public class Ride extends ServerPacket
{
	private final int _objectId;
	private final Location _loc;
	private final boolean _mounted;
	private final int _rideType;
	private final int _rideNpcId;
	
	/**
	 * Creates a new {@code Ride} packet for a creature.<br>
	 * For a {@code Player} it uses the real mount state; any other creature (e.g. a fake player npc) is treated as dismounting.
	 * @param creature The {@code Creature} whose ride information will be packaged.
	 */
	public Ride(Creature creature)
	{
		_objectId = creature.getObjectId();
		_loc = creature.getLocation();
		
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			_mounted = player.isMounted();
			_rideType = player.getMountType().ordinal();
			_rideNpcId = player.getMountNpcId() + 1000000;
		}
		else if (creature.isFakePlayer())
		{
			// Read the fake player mount from its template holder and current dismount state.
			final Npc npc = creature.asNpc();
			final FakePlayerHolder holder = npc.getTemplate().getFakePlayerInfo();
			if ((holder != null) && (holder.getMountType() > 0) && !npc.isDismounted())
			{
				_mounted = true;
				_rideType = holder.getMountType();
				_rideNpcId = holder.getMountNpcId() + 1000000;
			}
			else
			{
				_mounted = false;
				_rideType = 0;
				_rideNpcId = 1000000;
			}
		}
		else
		{
			_mounted = false;
			_rideType = 0;
			_rideNpcId = 1000000;
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.RIDE.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_mounted);
		buffer.writeInt(_rideType);
		buffer.writeInt(_rideNpcId);
		buffer.writeInt(_loc.getX());
		buffer.writeInt(_loc.getY());
		buffer.writeInt(_loc.getZ());
	}
}
