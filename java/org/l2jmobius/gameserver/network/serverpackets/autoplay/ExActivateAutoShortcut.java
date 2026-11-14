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
package org.l2jmobius.gameserver.network.serverpackets.autoplay;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.entity.actor.holders.player.Shortcuts;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to activate an autoplay shortcut.<br>
 * It informs the player that a specific {@code Shortcut} has been triggered by the autoplay system.
 * @author Mobius
 */
public class ExActivateAutoShortcut extends ServerPacket
{
	private final int _position;
	private final boolean _active;
	
	/**
	 * This method creates a new {@code ExActivateAutoShortcut} packet.<br>
	 * It sets the shortcut position and its active state.
	 * @param shortcut The {@code Shortcut} object to use for calculating the position.
	 * @param active The {@code boolean} value indicating if the shortcut is enabled.
	 */
	public ExActivateAutoShortcut(Shortcut shortcut, boolean active)
	{
		_position = shortcut.getSlot() + (shortcut.getPage() * Shortcuts.MAX_SHORTCUTS_PER_BAR);
		_active = active;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ACTIVATE_AUTO_SHORTCUT.writeId(this, buffer);
		buffer.writeShort(_position);
		buffer.writeByte(_active);
	}
}
