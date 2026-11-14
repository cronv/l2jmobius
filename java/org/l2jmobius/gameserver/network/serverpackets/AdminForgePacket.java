/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.network.serverpackets;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;

/**
 * This class is used to construct custom server packets with arbitrary formats.<br>
 * It allows administrators to forge and send various types of data to the client.
 * @author Maktakien
 */
public class AdminForgePacket extends ServerPacket
{
	private final List<Part> _parts = new ArrayList<>();
	
	private static class Part
	{
		public byte b;
		public String str;
		
		public Part(byte bb, String string)
		{
			b = bb;
			str = string;
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		for (Part p : _parts)
		{
			generate(p.b, p.str, buffer);
		}
	}
	
	/**
	 * This method writes a value to the provided {@code WriteBuffer}.<br>
	 * It determines the data type based on the {@code type} byte.<br>
	 * The value is parsed from a {@code String} and written in the correct format.
	 * @param type The character code representing the data type.
	 * @param value The string representation of the value to write.
	 * @param buffer The {@code WriteBuffer} where the data will be stored.
	 * @return {@code true} if the value was successfully written, otherwise {@code false}.
	 */
	public boolean generate(byte type, String value, WriteBuffer buffer)
	{
		switch (type)
		{
			case 'C':
			case 'c':
			{
				buffer.writeByte(Integer.decode(value));
				return true;
			}
			case 'D':
			case 'd':
			{
				buffer.writeInt(Integer.decode(value));
				return true;
			}
			case 'H':
			case 'h':
			{
				buffer.writeShort(Integer.decode(value));
				return true;
			}
			case 'F':
			case 'f':
			{
				buffer.writeDouble(Double.parseDouble(value));
				return true;
			}
			case 'S':
			case 's':
			{
				buffer.writeString(value);
				return true;
			}
			case 'B':
			case 'b':
			case 'X':
			case 'x':
			{
				buffer.writeBytes(new BigInteger(value).toByteArray());
				return true;
			}
			case 'Q':
			case 'q':
			{
				buffer.writeLong(Long.decode(value));
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	/**
	 * Adds a new part to the packet.<br>
	 * This method stores a {@code byte} and a {@code String} together.<br>
	 * The data is saved into an internal list for later use.
	 * @param b The byte value to store.
	 * @param string The string value to store.
	 */
	public void addPart(byte b, String string)
	{
		_parts.add(new Part(b, string));
	}
}
