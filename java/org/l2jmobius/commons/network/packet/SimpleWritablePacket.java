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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is an abstract class for packets backed by a {@code byte[]} array.<br>
 * It provides methods to write various types of data into the packet with a maximum size of {@code 65533} bytes.
 * @author Mobius
 * @since October 29th 2020
 */
public abstract class SimpleWritablePacket
{
	private static final Map<Class<?>, Integer> MAXIMUM_PACKET_SIZE = new ConcurrentHashMap<>();
	
	private final int _initialSize = MAXIMUM_PACKET_SIZE.getOrDefault(getClass(), 8);
	
	private byte[] _data;
	private byte[] _sendableBytes;
	private int _position = 2; // Allocate space for size (max length 65535 - size header).
	
	/**
	 * Initializes a new instance of this packet.<br>
	 * It allocates the initial internal buffer based on the class type.
	 */
	protected SimpleWritablePacket()
	{
		_data = new byte[_initialSize];
	}
	
	/**
	 * Writes a single {@code byte} to the packet.<br>
	 * This method automatically expands the internal buffer if needed.<br>
	 * It throws a {@code BufferOverflowException} if the data exceeds the maximum size limit.
	 * @param value The {@code byte} value to write.
	 */
	public void write(byte value)
	{
		// Check current size.
		if (_position < 65535)
		{
			// Check capacity.
			if (_position == _data.length)
			{
				_data = Arrays.copyOf(_data, _data.length * 2); // Double the capacity.
			}
			
			// Set value.
			_data[_position++] = value;
			return;
		}
		
		throw new BufferOverflowException();
	}
	
	/**
	 * Writes a {@code boolean} value to the packet as a {@code byte}.<br>
	 * {@code true} is written as {@code 1} and {@code false} as {@code 0}.
	 * @param value The {@code boolean} value to write.
	 */
	public void writeBoolean(boolean value)
	{
		writeByte(value ? 1 : 0);
	}
	
	/**
	 * Writes a {@code String} to the packet.<br>
	 * The text is encoded using {@code UTF_16LE} and followed by a {@code short} terminator of {@code 0}.<br>
	 * If the input is {@code null}, only the terminator is written.
	 * @param text The string to write to the packet.
	 */
	public void writeString(String text)
	{
		if (text != null)
		{
			writeBytes(text.getBytes(StandardCharsets.UTF_16LE));
		}
		
		writeShort(0);
	}
	
	/**
	 * Writes a {@code String} to the packet with a length prefix.<br>
	 * The character count is written first as a {@code short}, then the text is encoded using {@code UTF_16LE}.<br>
	 * If the input is {@code null}, a length of {@code 0} is written.
	 * @param text The string to write to the packet.
	 */
	public void writeSizedString(String text)
	{
		if (text != null)
		{
			writeShort(text.length());
			writeBytes(text.getBytes(StandardCharsets.UTF_16LE));
		}
		else
		{
			writeShort(0);
		}
	}
	
	/**
	 * Writes the contents of a byte array to the packet.<br>
	 * This method writes each byte of the {@code array} in order.
	 * @param array The array of bytes to write to the packet.
	 */
	public void writeBytes(byte[] array)
	{
		for (int i = 0; i < array.length; i++)
		{
			write(array[i]);
		}
	}
	
	/**
	 * Writes a single byte to the packet.<br>
	 * This method casts the provided {@code int} to a {@code byte}, keeping only the lowest 8 bits.
	 * @param value The {@code int} value to be cast and written as a {@code byte}.
	 */
	public void writeByte(int value)
	{
		write((byte) (value & 0xff));
	}
	
	/**
	 * Writes a {@code boolean} value to the packet as a {@code byte}.<br>
	 * {@code true} is written as {@code 1} and {@code false} as {@code 0}.
	 * @param value The {@code boolean} value to write.
	 */
	public void writeByte(boolean value)
	{
		writeByte(value ? 1 : 0);
	}
	
	/**
	 * Writes a {@code short} value to the packet.<br>
	 * This method writes the value as 2 bytes in little-endian order.
	 * @param value The value to be written as a {@code short}.
	 */
	public void writeShort(int value)
	{
		write((byte) (value & 0xff));
		write((byte) ((value >> 8) & 0xff));
	}
	
	/**
	 * Writes a {@code boolean} value to the packet as a {@code short}.<br>
	 * {@code true} is written as {@code 1} and {@code false} as {@code 0}.
	 * @param value The {@code boolean} value to write.
	 */
	public void writeShort(boolean value)
	{
		writeShort(value ? 1 : 0);
	}
	
	/**
	 * Writes an {@code int} value to the packet.<br>
	 * This method writes the value as 4 bytes in little-endian order.
	 * @param value The {@code int} value to write.
	 */
	public void writeInt(int value)
	{
		write((byte) (value & 0xff));
		write((byte) ((value >> 8) & 0xff));
		write((byte) ((value >> 16) & 0xff));
		write((byte) ((value >> 24) & 0xff));
	}
	
	/**
	 * Writes a {@code boolean} value to the packet as an {@code int}.<br>
	 * {@code true} is written as {@code 1} and {@code false} as {@code 0}.
	 * @param value The {@code boolean} value to write.
	 */
	public void writeInt(boolean value)
	{
		writeInt(value ? 1 : 0);
	}
	
	/**
	 * Writes a {@code long} value to the packet.<br>
	 * This method writes the value as 8 bytes in little-endian order.
	 * @param value The {@code long} value to write.
	 */
	public void writeLong(long value)
	{
		write((byte) (value & 0xff));
		write((byte) ((value >> 8) & 0xff));
		write((byte) ((value >> 16) & 0xff));
		write((byte) ((value >> 24) & 0xff));
		write((byte) ((value >> 32) & 0xff));
		write((byte) ((value >> 40) & 0xff));
		write((byte) ((value >> 48) & 0xff));
		write((byte) ((value >> 56) & 0xff));
	}
	
	/**
	 * Writes a {@code boolean} value to the packet as a {@code long}.<br>
	 * {@code true} is written as {@code 1} and {@code false} as {@code 0}.
	 * @param value The {@code boolean} value to write.
	 */
	public void writeLong(boolean value)
	{
		writeLong(value ? 1 : 0);
	}
	
	/**
	 * Writes a {@code float} value to the packet.<br>
	 * This method converts the {@code float} to its raw {@code int} bits and writes them.
	 * @param value The {@code float} value to write.
	 */
	public void writeFloat(float value)
	{
		writeInt(Float.floatToRawIntBits(value));
	}
	
	/**
	 * Writes a {@code double} value to the packet.<br>
	 * This method converts the {@code double} to its raw {@code long} bits and writes them.
	 * @param value The {@code double} value to write.
	 */
	public void writeDouble(double value)
	{
		writeLong(Double.doubleToRawLongBits(value));
	}
	
	/**
	 * This method is intended to be overridden by the server implementation.<br>
	 * It handles the final processing of the packet data.
	 */
	public void write()
	{
		// Overridden by server implementation.
	}
	
	/**
	 * Retrieves the final byte array ready to be sent over the network.<br>
	 * This method ensures the packet is fully written and formatted with its size header.<br>
	 * It caches the result in {@code _sendableBytes} after the first call.
	 * @return A {@code byte[]} containing the processed packet data.
	 */
	public synchronized byte[] getSendableBytes()
	{
		// Generate sendable byte array.
		if (_sendableBytes == null /* Not processed */)
		{
			// Write packet implementation (only once).
			if (_position == 2)
			{
				write();
				
				// Update maximum packet size if needed.
				if (_position > _initialSize)
				{
					MAXIMUM_PACKET_SIZE.put(getClass(), Math.min(_position, 65535));
				}
			}
			
			// Check if data was written.
			if (_position > 2)
			{
				// Trim array of data.
				_sendableBytes = Arrays.copyOf(_data, _position);
				
				// Add size info at start (unsigned short - max size 65535).
				_sendableBytes[0] = (byte) (_position & 0xff);
				_sendableBytes[1] = (byte) ((_position >> 8) & 0xffff);
			}
		}
		
		// Return the data.
		return _sendableBytes;
	}
	
	/**
	 * Returns the current length of the packet data.<br>
	 * This value represents the current position in the internal buffer.
	 * @return The current size of the packet as an {@code int}.
	 */
	public int getLength()
	{
		return _position;
	}
}
