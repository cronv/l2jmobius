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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client when a player uses an extra magic skill on the ground.<br>
 * It provides the necessary data for the client to display the visual effects of the skill.
 * @author Mobius
 */
public class ExMagicSkillUseGround extends ServerPacket
{
	private final int _playerObjectId;
	private final int _skillId;
	private final Location _location;
	
	/**
	 * This constructor creates a new {@code ExMagicSkillUseGround} packet.<br>
	 * It is used to notify the client about a magic skill cast on the ground.
	 * @param playerObjectId The unique ID of the player using the skill.
	 * @param skillId The unique identifier for the specific skill being used.
	 * @param location The {@code Location} where the skill was cast.
	 */
	public ExMagicSkillUseGround(int playerObjectId, int skillId, Location location)
	{
		_playerObjectId = playerObjectId;
		_skillId = skillId;
		_location = location;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_MAGIC_SKILL_USE_GROUND.writeId(this, buffer);
		buffer.writeInt(_playerObjectId);
		buffer.writeInt(_skillId);
		buffer.writeInt(_location.getX());
		buffer.writeInt(_location.getY());
		buffer.writeInt(_location.getZ());
	}
}
