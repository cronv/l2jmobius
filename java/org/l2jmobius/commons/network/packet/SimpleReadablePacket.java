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
package org.l2jmobius.commons.network.packet;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;

/**
 * A simple class for packets that can be read from client data.<br>
 * It provides methods to read various data types from the packet's byte array.<br>
 * Multi-byte values are decoded in little-endian order.
 * @author Mobius
 * @since October 29th 2020
 */
public class SimpleReadablePacket
{
	private final byte[] _bytes;
	private int _position = 0;
	
	/**
	 * Creates a new {@code SimpleReadablePacket} from a byte array.<br>
	 * This constructor initializes the packet with the provided data for reading.
	 * @param bytes The raw byte array to be stored in the packet.
	 */
	public SimpleReadablePacket(byte[] bytes)
	{
		_bytes = bytes;
	}
	
	/**
	 * Reads a {@code boolean} value from the packet data.<br>
	 * This method advances the position by 1 byte.<br>
	 * It returns {@code true} if the byte is not {@code 0}, otherwise {@code false}.
	 * @return The {@code boolean} value read from the packet.
	 */
	public boolean readBoolean()
	{
		return readByte() != 0;
	}
	
	/**
	 * Reads a null-terminated string from the packet data.<br>
	 * This method reads 2-byte characters until it encounters a {@code 0} value.<br>
	 * An unterminated string ends at the last whole character, returning what was read so far.
	 * @return The resulting {@code String}.
	 */
	public String readString()
	{
		final StringBuilder result = new StringBuilder();
		int charId;
		while ((getRemainingLength() > 1) && ((charId = readShort()) != 0))
		{
			result.append((char) charId);
		}
		
		return result.toString();
	}
	
	/**
	 * Reads a length-prefixed string from the packet data.<br>
	 * The character count is read first as a {@code short}, then the characters are decoded using {@code UTF_16LE}.<br>
	 * Returns an empty string if an error occurs.
	 * @return The decoded {@code String}, or an empty string if an error occurs.
	 */
	public String readSizedString()
	{
		String result = "";
		try
		{
			result = new String(readBytes(readShort() * 2), StandardCharsets.UTF_16LE);
		}
		catch (Exception ignored)
		{
		}
		
		return result;
	}
	
	/**
	 * Reads a specific number of bytes from the packet.<br>
	 * This method advances the position by {@code length}.
	 * @param length The number of bytes to read.
	 * @return A new {@code byte[]} containing the requested data.
	 */
	public byte[] readBytes(int length)
	{
		// Validate underflow before allocating.
		if (length < 0)
		{
			throw new BufferUnderflowException();
		}
		
		// Validate overflow before allocating, so a forged length cannot exhaust the heap.
		if (length > getRemainingLength())
		{
			throw new BufferOverflowException();
		}
		
		final byte[] result = new byte[length];
		for (int i = 0; i < length; i++)
		{
			result[i] = _bytes[_position++];
		}
		
		return result;
	}
	
	/**
	 * Reads bytes from the packet into a provided array.<br>
	 * This method reads exactly {@code array.length} bytes and advances the position accordingly.
	 * @param array The {@code byte[]} to be populated with data.
	 * @return The same {@code byte[]} instance after being filled.
	 */
	public byte[] readBytes(byte[] array)
	{
		// Validate underflow before allocating.
		final int length = array.length;
		if (length < 0)
		{
			throw new BufferUnderflowException();
		}
		
		// Validate overflow before allocating, so a forged length cannot exhaust the heap.
		if (length > getRemainingLength())
		{
			throw new BufferOverflowException();
		}
		
		for (int i = 0; i < length; i++)
		{
			array[i] = _bytes[_position++];
		}
		
		return array;
	}
	
	/**
	 * Reads the next byte from the packet data as an unsigned {@code int}.<br>
	 * This method advances the position by 1 byte and maps the byte to the range {@code 0} to {@code 255}.
	 * @return The value of the read byte as an unsigned {@code int}.
	 */
	public int readByte()
	{
		return _bytes[_position++] & 0xff;
	}
	
	/**
	 * Reads the next short from the packet data as an unsigned {@code int}.<br>
	 * This method combines two bytes and advances the position by 2 bytes.<br>
	 * The value is in the range {@code 0} to {@code 65535}.
	 * @return The value of the read short as an unsigned {@code int}.
	 */
	public int readShort()
	{
		return (_bytes[_position++] & 0xff) //
			| ((_bytes[_position++] & 0xff) << 8);
	}
	
	/**
	 * Reads the next {@code int} value from the packet data.<br>
	 * This method advances the position by 4 bytes.
	 * @return The {@code int} value read from the packet.
	 */
	public int readInt()
	{
		return (_bytes[_position++] & 0xff) //
			| ((_bytes[_position++] & 0xff) << 8) //
			| ((_bytes[_position++] & 0xff) << 16) //
			| ((_bytes[_position++] & 0xff) << 24);
	}
	
	/**
	 * Reads the next {@code long} value from the packet data.<br>
	 * This method advances the position by 8 bytes.
	 * @return The {@code long} value read from the packet.
	 */
	public long readLong()
	{
		return (_bytes[_position++] & 0xff) //
			| ((_bytes[_position++] & 0xffL) << 8) //
			| ((_bytes[_position++] & 0xffL) << 16) //
			| ((_bytes[_position++] & 0xffL) << 24) //
			| ((_bytes[_position++] & 0xffL) << 32) //
			| ((_bytes[_position++] & 0xffL) << 40) //
			| ((_bytes[_position++] & 0xffL) << 48) //
			| ((_bytes[_position++] & 0xffL) << 56);
	}
	
	/**
	 * Reads the next {@code float} value from the packet data.<br>
	 * This method reads 4 bytes and converts the underlying {@code int} bits into a floating-point number.
	 * @return The {@code float} value read from the packet.
	 */
	public float readFloat()
	{
		return Float.intBitsToFloat(readInt());
	}
	
	/**
	 * Reads the next {@code double} value from the packet data.<br>
	 * This method reads 8 bytes and converts the underlying {@code long} bits into a floating-point number.
	 * @return The {@code double} value read from the packet.
	 */
	public double readDouble()
	{
		return Double.longBitsToDouble(readLong());
	}
	
	/**
	 * Calculates the number of bytes left to be read in the packet.<br>
	 * This value is the difference between the total length and the current read position.
	 * @return The number of remaining bytes as an {@code int}.
	 */
	public int getRemainingLength()
	{
		return _bytes.length - _position;
	}
	
	/**
	 * Returns the total size of the packet data.<br>
	 * This value represents the length of the internal byte array.
	 * @return The total number of bytes in the packet.
	 */
	public int getLength()
	{
		return _bytes.length;
	}
}
