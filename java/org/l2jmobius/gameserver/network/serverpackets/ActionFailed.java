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

import java.util.EnumMap;
import java.util.Map;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.skill.SkillCastingType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client when an action or skill fails to execute.<br>
 * It informs the user that their requested interaction was unsuccessful.
 * @author Mobius
 */
public class ActionFailed extends ServerPacket
{
	public static final ActionFailed STATIC_PACKET = new ActionFailed();
	
	private static final Map<SkillCastingType, ActionFailed> STATIC_PACKET_BY_CASTING_TYPE = new EnumMap<>(SkillCastingType.class);
	static
	{
		for (SkillCastingType castingType : SkillCastingType.values())
		{
			STATIC_PACKET_BY_CASTING_TYPE.put(castingType, new ActionFailed(castingType.getClientBarId()));
		}
	}
	
	private final int _castingType;
	
	/**
	 * Private constructor for the {@code ActionFailed} class.<br>
	 * This constructor initializes a default instance with a casting type of {@code 0}.<br>
	 * It is intended for internal use only.
	 */
	private ActionFailed()
	{
		_castingType = 0;
	}
	
	/**
	 * Creates a new {@code ActionFailed} instance.<br>
	 * This constructor sets the internal casting type value.
	 * @param castingType The integer ID of the skill casting type.
	 */
	private ActionFailed(int castingType)
	{
		_castingType = castingType;
	}
	
	/**
	 * Retrieves a pre-instantiated {@code ActionFailed} packet for a specific casting type.<br>
	 * This method uses an internal cache to provide the correct packet based on the provided enum.
	 * @param castingType The {@code SkillCastingType} used to identify the failure.
	 * @return The corresponding {@code ActionFailed} packet instance.
	 */
	public static ActionFailed get(SkillCastingType castingType)
	{
		return STATIC_PACKET_BY_CASTING_TYPE.getOrDefault(castingType, STATIC_PACKET);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.ACTION_FAIL.writeId(this, buffer);
		buffer.writeInt(_castingType); // MagicSkillUse castingType
	}
	
	// Checks if the item can be dropped by the game client.
	// {
	// return true;
	// }
}
