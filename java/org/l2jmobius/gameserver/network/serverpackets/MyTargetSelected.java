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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.ControllableAirShip;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to notify them that a new target has been selected.<br>
 * It informs the {@code GameClient} about the specific entity being targeted.
 * @author Mobius
 */
public class MyTargetSelected extends ServerPacket
{
	private final int _objectId;
	private final int _color;
	private final int _isDead;
	
	/**
	 * Creates a new {@code MyTargetSelected} packet.<br>
	 * This is used to notify the client about a selected target.<br>
	 * It calculates the object ID and color based on the player and target.
	 * @param player The {@code Player} who selected the target.
	 * @param target The {@code Creature} that was selected.
	 */
	public MyTargetSelected(Player player, Creature target)
	{
		_objectId = (target instanceof ControllableAirShip) ? ((ControllableAirShip) target).getHelmObjectId() : target.getObjectId();
		_color = target.isAutoAttackable(player) ? (player.getLevel() - target.getLevel()) : 0;
		_isDead = target.isDead() ? 1 : 0;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.MY_TARGET_SELECTED.writeId(this, buffer);
		buffer.writeInt(1); // Grand Crusade
		buffer.writeInt(_objectId);
		buffer.writeShort(_color);
		buffer.writeInt(0);
		buffer.writeByte(_isDead);
	}
}
