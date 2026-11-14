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

import java.nio.charset.StandardCharsets;

import org.l2jmobius.commons.network.Client;
import org.l2jmobius.commons.network.Connection;
import org.l2jmobius.commons.network.buffer.ReadBuffer;

/**
 * This is the base class for all packets received from a client.<br>
 * Subclasses must implement {@code read()} to parse data and implement {@code run()} to execute logic.<br>
 * The network layer handles the lifecycle by initializing, parsing, and executing these packets.
 * @param <T> The client type associated with this packet.
 * @author JoeAlisson, Mobius
 */
public abstract class ReadablePacket<T extends Client<Connection<T>>> implements Runnable
{
	private ReadBuffer _buffer;
	private T _client;
	
	/**
	 * Creates a new instance of a {@code ReadablePacket}.<br>
	 * This is the default constructor for all packet types.
	 */
	protected ReadablePacket()
	{
	}
	
	/**
	 * Initializes the packet with the required data.<br>
	 * This method assigns the {@code client} and {@code buffer} to internal fields.
	 * @param client The {@code Client} instance associated with this packet.
	 * @param buffer The {@code ReadBuffer} containing the packet data.
	 */
	public void init(T client, ReadBuffer buffer)
	{
		_client = client;
		_buffer = buffer;
	}
	
	// -------------------------------------------------------------------------
	// Delegating read helpers - all delegate to the bound buffer.
	// -------------------------------------------------------------------------
	
	/**
	 * Reads a {@code char} value from the current buffer.<br>
	 * This method advances the buffer position by 2 bytes.
	 * @return The {@code char} value read from the buffer.
	 */
	protected char readChar()
	{
		return _buffer.readChar();
	}
	
	/**
	 * Reads a {@code byte} value from the current buffer.<br>
	 * This method advances the buffer position by 1 byte.
	 * @return The {@code byte} value read from the buffer.
	 */
	protected byte readByte()
	{
		return _buffer.readByte();
	}
	
	/**
	 * Reads the next byte from the current buffer as an unsigned {@code int}.<br>
	 * This method advances the buffer position by 1 byte and maps the {@code byte} to the range {@code 0} to {@code 255}.
	 * @return The value of the read byte as an unsigned {@code int}.
	 */
	protected int readUnsignedByte()
	{
		return Byte.toUnsignedInt(_buffer.readByte());
	}
	
	/**
	 * Reads a {@code boolean} value from the current buffer.<br>
	 * This method advances the buffer position by 1 byte.<br>
	 * It returns {@code true} if the byte is not {@code 0}, otherwise {@code false}.
	 * @return The {@code boolean} value read from the buffer.
	 */
	protected boolean readBoolean()
	{
		return _buffer.readByte() != 0;
	}
	
	/**
	 * Reads a specific number of bytes from the current buffer.<br>
	 * This method creates a new {@code byte[]} of the given {@code length} to store the data.
	 * @param length The number of bytes to read.
	 * @return A new {@code byte[]} containing the requested data.
	 */
	protected byte[] readBytes(int length)
	{
		return _buffer.readBytes(length);
	}
	
	/**
	 * Reads the next sequence of bytes from the current buffer into a destination array.<br>
	 * This method reads exactly {@code dst.length} bytes and stores them in the provided array.
	 * @param dst The destination array where the bytes will be stored.
	 */
	protected void readBytes(byte[] dst)
	{
		_buffer.readBytes(dst, 0, dst.length);
	}
	
	/**
	 * Reads a specific number of bytes from the current buffer into a destination array.<br>
	 * This method stores the data in the {@code dst} array starting at the given {@code offset}.<br>
	 * It reads exactly {@code length} bytes from the current position.
	 * @param dst The destination array where the bytes will be stored.
	 * @param offset The starting index in the {@code dst} array.
	 * @param length The number of bytes to read from the buffer.
	 */
	protected void readBytes(byte[] dst, int offset, int length)
	{
		_buffer.readBytes(dst, offset, length);
	}
	
	/**
	 * Reads a {@code short} value from the current buffer.<br>
	 * This method advances the buffer position by 2 bytes.
	 * @return The {@code short} value read from the buffer.
	 */
	protected short readShort()
	{
		return _buffer.readShort();
	}
	
	/**
	 * Reads an {@code int} value from the current buffer.<br>
	 * This method advances the buffer position by 4 bytes.
	 * @return The {@code int} value read from the buffer.
	 */
	protected int readInt()
	{
		return _buffer.readInt();
	}
	
	/**
	 * Reads a {@code long} value from the current buffer.<br>
	 * This method advances the buffer position by 8 bytes.
	 * @return The {@code long} value read from the buffer.
	 */
	protected long readLong()
	{
		return _buffer.readLong();
	}
	
	/**
	 * Reads a {@code float} value from the current buffer.<br>
	 * This method advances the buffer position by 4 bytes.
	 * @return The {@code float} value read from the buffer.
	 */
	protected float readFloat()
	{
		return _buffer.readFloat();
	}
	
	/**
	 * Reads a {@code double} value from the current buffer.<br>
	 * This method advances the buffer position by 8 bytes.
	 * @return The {@code double} value read from the buffer.
	 */
	protected double readDouble()
	{
		return _buffer.readDouble();
	}
	
	/**
	 * Reads a null-terminated string from the current buffer.<br>
	 * This method reads 2-byte characters until it encounters a {@code 0} value.<br>
	 * An unterminated string ends at the last whole character, returning what was read so far.
	 * @return The resulting {@code String}.
	 */
	protected String readString()
	{
		final StringBuilder result = new StringBuilder();
		int charId;
		while ((remaining() > 1) && ((charId = readShort()) != 0))
		{
			result.append((char) charId);
		}
		
		return result.toString();
	}
	
	/**
	 * Reads a length-prefixed {@code String} from the current buffer.<br>
	 * The character count is read first as a {@code short}, then the characters are decoded using {@code UTF_16LE}.<br>
	 * Returns an empty string if an error occurs or the character count is not positive.
	 * @return The decoded {@code String} from the buffer.
	 */
	protected String readSizedString()
	{
		try
		{
			final int charCount = readShort();
			if (charCount > 0)
			{
				return new String(readBytes(charCount * 2), StandardCharsets.UTF_16LE);
			}
		}
		catch (Exception ignored)
		{
		}
		
		return "";
	}
	
	/**
	 * Returns the number of bytes left in the buffer.<br>
	 * This value indicates how much data is still available to read.
	 * @return The number of remaining bytes as an {@code int}.
	 */
	protected int remaining()
	{
		return _buffer.remaining();
	}
	
	/**
	 * Retrieves the {@code Client} associated with this packet.<br>
	 * This method returns the client instance that sent the packet.
	 * @return The {@code Client} associated with this packet.
	 */
	public T getClient()
	{
		return _client;
	}
	
	/**
	 * Parses the packet's fields from the bound buffer.<br>
	 * Called on the I/O thread; must not block or perform heavy work.
	 * @return {@code true} if parsing succeeded and the packet should be executed; {@code false} to discard.
	 */
	public abstract boolean read();
}
