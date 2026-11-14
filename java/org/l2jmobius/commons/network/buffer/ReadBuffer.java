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
package org.l2jmobius.commons.network.buffer;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class provides a wrapper for an inbound network {@code ByteBuffer}.<br>
 * It supports sequential cursor-based reads and absolute indexed access.<br>
 * Multi-byte values are read in the byte order configured on the wrapped {@code ByteBuffer}; the protocol convention is little-endian, so the supplied buffer is expected to be ordered accordingly.
 * @author Mobius
 */
public class ReadBuffer
{
	private final ByteBuffer _buf;
	
	/**
	 * Creates a new {@code ReadBuffer} instance.<br>
	 * This constructor wraps the provided {@code ByteBuffer}.<br>
	 * The buffer's byte order and position are used as-is.
	 * @param buffer The {@code ByteBuffer} to wrap. It must not be {@code null}.
	 * @throws NullPointerException If {@code buffer} is {@code null}.
	 */
	public ReadBuffer(ByteBuffer buffer)
	{
		_buf = Objects.requireNonNull(buffer);
	}
	
	/**
	 * Creates a new {@code ReadBuffer} instance.<br>
	 * This method wraps the provided {@code ByteBuffer}.<br>
	 * The buffer's byte order and position are used as-is.
	 * @param buffer The {@code ByteBuffer} to wrap. It must not be {@code null}.
	 * @return A new {@code ReadBuffer} wrapping the provided buffer.
	 * @throws NullPointerException If {@code buffer} is {@code null}.
	 */
	public static ReadBuffer of(ByteBuffer buffer)
	{
		return new ReadBuffer(buffer);
	}
	
	// -------------------------------------------------------------------------
	// Sequential reads (cursor-based).
	// -------------------------------------------------------------------------
	
	/**
	 * Reads the next {@code char} value from the buffer.<br>
	 * This method advances the buffer's current position.
	 * @return The {@code char} value read from the buffer.
	 */
	public char readChar()
	{
		return _buf.getChar();
	}
	
	/**
	 * Reads the next {@code byte} value from the buffer.<br>
	 * This method advances the buffer's current position.
	 * @return The {@code byte} value read from the buffer.
	 */
	public byte readByte()
	{
		return _buf.get();
	}
	
	/**
	 * Reads the next {@code short} value from the buffer.<br>
	 * This method advances the buffer's current position.
	 * @return The {@code short} value read from the buffer.
	 */
	public short readShort()
	{
		return _buf.getShort();
	}
	
	/**
	 * Reads the next {@code int} value from the buffer.<br>
	 * This method advances the buffer's current position.
	 * @return The {@code int} value read from the buffer.
	 */
	public int readInt()
	{
		return _buf.getInt();
	}
	
	/**
	 * Reads the next {@code long} value from the buffer.<br>
	 * This method advances the buffer's current position.
	 * @return The {@code long} value read from the buffer.
	 */
	public long readLong()
	{
		return _buf.getLong();
	}
	
	/**
	 * Reads the next {@code float} value from the buffer.<br>
	 * This method advances the buffer's current position.
	 * @return The {@code float} value read from the buffer.
	 */
	public float readFloat()
	{
		return _buf.getFloat();
	}
	
	/**
	 * Reads the next {@code double} value from the buffer.<br>
	 * This method advances the buffer's current position.
	 * @return The {@code double} value read from the buffer.
	 */
	public double readDouble()
	{
		return _buf.getDouble();
	}
	
	/**
	 * Reads a specific number of bytes from the buffer.<br>
	 * This method advances the buffer's current position by {@code length}.
	 * @param length The number of bytes to read.
	 * @return A new {@code byte[]} containing the requested data.
	 * @throws BufferUnderflowException If {@code length} is negative.
	 * @throws BufferOverflowException If {@code length} is greater than the number of remaining bytes.
	 */
	public byte[] readBytes(int length)
	{
		// Validate underflow before allocating.
		if (length < 0)
		{
			throw new BufferUnderflowException();
		}
		
		// Validate overflow before allocating, so a forged length cannot exhaust the heap.
		if (length > _buf.remaining())
		{
			throw new BufferOverflowException();
		}
		
		final byte[] dst = new byte[length];
		_buf.get(dst);
		return dst;
	}
	
	/**
	 * Reads the next sequence of bytes from the buffer into a destination array.<br>
	 * This method advances the buffer's current position by the length of {@code dst}.
	 * @param dst The byte array where the data will be stored.
	 */
	public void readBytes(byte[] dst)
	{
		_buf.get(dst);
	}
	
	/**
	 * Reads a sequence of bytes from the buffer into a destination array.<br>
	 * This method copies data into the provided {@code byte[]} starting at {@code offset}.<br>
	 * It advances the buffer's current position by {@code length}.
	 * @param dst The destination array where the bytes will be stored.
	 * @param offset The starting index in the {@code dst} array where writing begins.
	 * @param length The number of bytes to read from the buffer.
	 */
	public void readBytes(byte[] dst, int offset, int length)
	{
		_buf.get(dst, offset, length);
	}
	
	/**
	 * Returns the number of bytes left in the buffer.<br>
	 * This value represents the space between the current position and the limit.
	 * @return The number of remaining bytes as an {@code int}.
	 */
	public int remaining()
	{
		return _buf.remaining();
	}
	
	// -------------------------------------------------------------------------
	// Indexed (absolute) access - used during decryption.
	// -------------------------------------------------------------------------
	
	/**
	 * Reads a {@code byte} from the buffer at an absolute index.<br>
	 * This method does not change the buffer's current position.
	 * @param index The absolute index of the byte to read.
	 * @return The {@code byte} value found at the specified {@code index}.
	 */
	public byte readByte(int index)
	{
		return _buf.get(index);
	}
	
	/**
	 * Reads a {@code short} value from the buffer at an absolute index.<br>
	 * This method does not change the buffer's current position.
	 * @param index The absolute index of the first byte to read.
	 * @return The {@code short} value found at the specified {@code index}.
	 */
	public short readShort(int index)
	{
		return _buf.getShort(index);
	}
	
	/**
	 * Reads an {@code int} value from the buffer at an absolute index.<br>
	 * This method does not change the buffer's current position.
	 * @param index The absolute index of the first byte to read.
	 * @return The {@code int} value found at the specified {@code index}.
	 */
	public int readInt(int index)
	{
		return _buf.getInt(index);
	}
	
	/**
	 * Writes a {@code byte} to the buffer at an absolute index.<br>
	 * This method does not change the buffer's current position.
	 * @param index The absolute index where the value will be written.
	 * @param value The {@code byte} value to write into the buffer.
	 */
	public void writeByte(int index, byte value)
	{
		_buf.put(index, value);
	}
	
	/**
	 * Writes a {@code short} value to the buffer at an absolute index.<br>
	 * This method does not change the buffer's current position.
	 * @param index The absolute index where the value will be written.
	 * @param value The {@code short} value to write into the buffer.
	 */
	public void writeShort(int index, short value)
	{
		_buf.putShort(index, value);
	}
	
	/**
	 * Writes an {@code int} value to the buffer at an absolute index.<br>
	 * This method does not change the buffer's current position.
	 * @param index The absolute index where the value will be written.
	 * @param value The {@code int} value to write into the buffer.
	 */
	public void writeInt(int index, int value)
	{
		_buf.putInt(index, value);
	}
	
	/**
	 * Retrieves the current limit of the underlying buffer.<br>
	 * The limit defines the end of the readable data.
	 * @return The current limit as an {@code int}.
	 */
	public int limit()
	{
		return _buf.limit();
	}
	
	/**
	 * Updates the buffer's limit to a new value.<br>
	 * This sets the boundary for reading operations.
	 * @param newLimit The new limit to set for the underlying {@code ByteBuffer}.
	 */
	public void limit(int newLimit)
	{
		_buf.limit(newLimit);
	}
}
