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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.network.pool.ResourcePool;

/**
 * A buffer used for writing outbound network data using a pooled direct {@code ByteBuffer}.<br>
 * It supports sequential and absolute writes with automatic growth and size hinting.<br>
 * Multi-byte values are written in the byte order of the underlying buffer; the protocol convention is little-endian.<br>
 * This class also provides mechanisms for efficient broadcasting by snapshotting written content.
 * @author Mobius
 */
public class WriteBuffer
{
	/** Per-packet-class largest observed packet size, used to right-size the initial pooled buffer. */
	private static final Map<Class<?>, Integer> MAXIMUM_PACKET_SIZE = new ConcurrentHashMap<>();
	
	private final ResourcePool _resourcePool;
	private final Class<?> _packetClass;
	private final int _initialSize;
	
	private ByteBuffer _buf;
	private int _position;
	private int _limit;
	
	/**
	 * Creates a new {@code WriteBuffer} for a specific packet type.<br>
	 * This constructor acquires a buffer from the {@code ResourcePool}, sized using a hint recorded for the provided {@code packetClass}.
	 * @param resourcePool The pool used to acquire and release buffers.
	 * @param packetClass The class type of the packet being written.
	 */
	public WriteBuffer(ResourcePool resourcePool, Class<?> packetClass)
	{
		_resourcePool = resourcePool;
		_packetClass = packetClass;
		_initialSize = MAXIMUM_PACKET_SIZE.getOrDefault(packetClass, resourcePool.getSegmentSize());
		_buf = resourcePool.getBuffer(_initialSize);
		_limit = _buf.capacity();
	}
	
	/**
	 * Creates a new {@code WriteBuffer} seeded with data from a byte array.<br>
	 * This constructor is used to create a per-recipient copy of a broadcast packet.<br>
	 * It copies the provided {@code cached} content into the buffer and sets the write position past it.
	 * @param cached The source byte array containing the initial data.
	 * @param cachedLength The number of bytes to copy from the {@code cached} array.
	 * @param resourcePool The pool used to acquire the underlying buffer.
	 * @param packetClass The class type used to determine the optimal initial buffer size.
	 */
	public WriteBuffer(byte[] cached, int cachedLength, ResourcePool resourcePool, Class<?> packetClass)
	{
		_resourcePool = resourcePool;
		_packetClass = packetClass;
		_initialSize = MAXIMUM_PACKET_SIZE.getOrDefault(packetClass, Math.max(cachedLength, resourcePool.getSegmentSize()));
		_buf = resourcePool.getBuffer(Math.max(_initialSize, cachedLength));
		_buf.position(0);
		_buf.put(cached, 0, cachedLength);
		_buf.position(0);
		_position = cachedLength;
		_limit = cachedLength;
	}
	
	/**
	 * Checks if the current buffer has enough space for the requested size.<br>
	 * If the capacity is too small, it allocates a larger buffer from the pool.<br>
	 * The existing data is copied to the new buffer before recycling the old one.
	 * @param required The minimum capacity needed for the operation.
	 */
	private void ensureSize(int required)
	{
		final int capacity = _buf.capacity();
		if (capacity < required)
		{
			final int newSize = Math.max(required, (int) (capacity * 1.5));
			final ByteBuffer newBuf = _resourcePool.getBuffer(newSize);
			newBuf.position(0);
			_buf.position(0).limit(Math.min(_position, capacity));
			newBuf.put(_buf);
			newBuf.position(0);
			_resourcePool.recycleBuffer(_buf);
			_buf = newBuf;
		}
	}
	
	// -------------------------------------------------------------------------
	// Sequential writes (cursor-based).
	// -------------------------------------------------------------------------
	
	/**
	 * Writes a {@code byte} value to the buffer.<br>
	 * This method ensures there is enough space, then advances the write position by 1 byte.
	 * @param value The {@code byte} value to write.
	 */
	public void writeByte(byte value)
	{
		ensureSize(_position + 1);
		_buf.put(_position++, value);
	}
	
	/**
	 * Writes a single byte to the buffer.<br>
	 * This method casts the provided {@code int} to a {@code byte} before writing.
	 * @param value The {@code int} value to be cast and written as a {@code byte}.
	 */
	public void writeByte(int value)
	{
		writeByte((byte) value);
	}
	
	/**
	 * Writes a {@code boolean} value to the buffer as a {@code byte}.<br>
	 * {@code true} is written as {@code 1} and {@code false} as {@code 0}.
	 * @param value The {@code boolean} value to write.
	 */
	public void writeByte(boolean value)
	{
		writeByte((byte) (value ? 1 : 0));
	}
	
	/**
	 * Writes a sequence of bytes to the buffer.<br>
	 * This method ensures there is enough space, then advances the write position by the array length.<br>
	 * If the array is {@code null} or empty, nothing is written.
	 * @param value The array of {@code byte} values to write.
	 */
	public void writeBytes(byte... value)
	{
		if ((value == null) || (value.length == 0))
		{
			return;
		}
		
		ensureSize(_position + value.length);
		_buf.put(_position, value, 0, value.length);
		_position += value.length;
	}
	
	/**
	 * Writes a {@code short} value to the buffer.<br>
	 * This method ensures there is enough space, then advances the write position by 2 bytes.
	 * @param value The {@code short} value to write.
	 */
	public void writeShort(short value)
	{
		ensureSize(_position + 2);
		_buf.putShort(_position, value);
		_position += 2;
	}
	
	/**
	 * Writes a {@code short} value to the buffer.<br>
	 * This method casts the provided {@code int} to a {@code short} before writing.
	 * @param value The {@code int} value to be cast and written as a {@code short}.
	 */
	public void writeShort(int value)
	{
		writeShort((short) value);
	}
	
	/**
	 * Writes a {@code boolean} value to the buffer as a {@code short}.<br>
	 * {@code true} is written as {@code 1} and {@code false} as {@code 0}.
	 * @param value The {@code boolean} value to write.
	 */
	public void writeShort(boolean value)
	{
		writeShort((short) (value ? 1 : 0));
	}
	
	/**
	 * Writes an {@code int} value to the buffer.<br>
	 * This method ensures there is enough space, then advances the write position by 4 bytes.
	 * @param value The {@code int} value to write.
	 */
	public void writeInt(int value)
	{
		ensureSize(_position + 4);
		_buf.putInt(_position, value);
		_position += 4;
	}
	
	/**
	 * Writes a {@code boolean} value to the buffer as an {@code int}.<br>
	 * {@code true} is written as {@code 1} and {@code false} as {@code 0}.
	 * @param value The {@code boolean} value to write.
	 */
	public void writeInt(boolean value)
	{
		writeInt(value ? 1 : 0);
	}
	
	/**
	 * Writes a {@code long} value to the buffer.<br>
	 * This method ensures there is enough space, then advances the write position by 8 bytes.
	 * @param value The {@code long} value to write.
	 */
	public void writeLong(long value)
	{
		ensureSize(_position + 8);
		_buf.putLong(_position, value);
		_position += 8;
	}
	
	/**
	 * Writes a {@code float} value to the buffer.<br>
	 * This method converts the {@code float} to its raw {@code int} bits and writes them.
	 * @param value The {@code float} value to write.
	 */
	public void writeFloat(float value)
	{
		writeInt(Float.floatToRawIntBits(value));
	}
	
	/**
	 * Writes a {@code double} value to the buffer.<br>
	 * This method converts the {@code double} to its raw {@code long} bits and writes them.
	 * @param value The {@code double} value to write.
	 */
	public void writeDouble(double value)
	{
		writeLong(Double.doubleToRawLongBits(value));
	}
	
	/**
	 * Writes a single {@code char} to the buffer.<br>
	 * This method writes the character as a 2-byte {@code short}.
	 * @param value The {@code char} value to write.
	 */
	public void writeChar(char value)
	{
		writeShort((short) value);
	}
	
	/**
	 * Writes a {@code CharSequence} to the buffer as null-terminated text.<br>
	 * Each character is written as a 2-byte {@code short}, followed by a {@code short} null terminator.<br>
	 * If the input is {@code null}, only the null terminator is written.
	 * @param text The sequence of characters to write.
	 */
	public void writeString(CharSequence text)
	{
		if (text != null)
		{
			final int len = text.length();
			ensureSize(_position + (len << 1) + 2);
			for (int i = 0; i < len; i++)
			{
				_buf.putShort(_position, (short) text.charAt(i));
				_position += 2;
			}
			
			// Null terminator - capacity already reserved above.
			_buf.putShort(_position, (short) 0);
			_position += 2;
			return;
		}
		
		writeChar('\000');
	}
	
	/**
	 * Writes a {@code CharSequence} to the buffer with a length prefix.<br>
	 * The character count is written first as a {@code short}, then each character as a 2-byte {@code short}.<br>
	 * If the input is {@code null} or empty, a length of {@code 0} is written.
	 * @param text The {@code CharSequence} to write.
	 */
	public void writeSizedString(CharSequence text)
	{
		if ((text != null) && !text.isEmpty())
		{
			final int len = text.length();
			ensureSize(_position + 2 + (len << 1));
			_buf.putShort(_position, (short) len);
			_position += 2;
			for (int i = 0; i < len; i++)
			{
				_buf.putShort(_position, (short) text.charAt(i));
				_position += 2;
			}
			return;
		}
		
		writeShort(0);
	}
	
	// -------------------------------------------------------------------------
	// Indexed (absolute) writes and reads - used for header patching and encryption.
	// -------------------------------------------------------------------------
	
	/**
	 * Writes a {@code byte} to the buffer at an absolute index.<br>
	 * This method ensures there is enough space and does not change the current write position.
	 * @param index The absolute index where the value will be written.
	 * @param value The {@code byte} value to write into the buffer.
	 */
	public void writeByte(int index, byte value)
	{
		ensureSize(index + 1);
		_buf.put(index, value);
	}
	
	/**
	 * Writes a {@code short} value to the buffer at an absolute index.<br>
	 * This method ensures there is enough space and does not change the current write position.
	 * @param index The absolute index where the value will be written.
	 * @param value The {@code short} value to write into the buffer.
	 */
	public void writeShort(int index, short value)
	{
		ensureSize(index + 2);
		_buf.putShort(index, value);
	}
	
	/**
	 * Writes an {@code int} value to the buffer at an absolute index.<br>
	 * This method ensures there is enough space and does not change the current write position.
	 * @param index The absolute index where the value will be written.
	 * @param value The {@code int} value to write into the buffer.
	 */
	public void writeInt(int index, int value)
	{
		ensureSize(index + 4);
		_buf.putInt(index, value);
	}
	
	/**
	 * Reads a {@code byte} from the buffer at an absolute index.<br>
	 * This method does not change the current write position.
	 * @param index The absolute index of the byte to read.
	 * @return The {@code byte} value found at the specified {@code index}.
	 */
	public byte readByte(int index)
	{
		return _buf.get(index);
	}
	
	/**
	 * Reads a {@code short} value from the buffer at an absolute index.<br>
	 * This method does not change the current write position.
	 * @param index The absolute index of the first byte to read.
	 * @return The {@code short} value found at the specified {@code index}.
	 */
	public short readShort(int index)
	{
		return _buf.getShort(index);
	}
	
	/**
	 * Reads an {@code int} value from the buffer at an absolute index.<br>
	 * This method does not change the current write position.
	 * @param index The absolute index of the first byte to read.
	 * @return The {@code int} value found at the specified {@code index}.
	 */
	public int readInt(int index)
	{
		return _buf.getInt(index);
	}
	
	// -------------------------------------------------------------------------
	// Position / limit / mark.
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the current write position in the buffer.<br>
	 * This value indicates where the next byte will be written.
	 * @return The current write position as an {@code int}.
	 */
	public int position()
	{
		return _position;
	}
	
	/**
	 * Sets the current write position of the buffer.<br>
	 * This method updates the write position to the specified value.
	 * @param pos The new write position.
	 */
	public void position(int pos)
	{
		_position = pos;
	}
	
	/**
	 * Returns the current limit of the buffer.<br>
	 * The limit marks the end of the buffer's logical content for export operations.
	 * @return The current limit as an {@code int}.
	 */
	public int limit()
	{
		return _limit;
	}
	
	/**
	 * Sets the buffer's limit to a new value.<br>
	 * This method grows the underlying buffer if needed so it can hold {@code newLimit} bytes, then updates the limit.
	 * @param newLimit The new limit for the buffer.
	 */
	public void limit(int newLimit)
	{
		ensureSize(newLimit);
		_limit = newLimit;
	}
	
	/**
	 * Sets the limit to match the current write position.<br>
	 * This effectively truncates the buffer's content to what has been written so far.
	 */
	public void mark()
	{
		_limit = _position;
	}
	
	// -------------------------------------------------------------------------
	// Export / recycle.
	// -------------------------------------------------------------------------
	
	/**
	 * Exposes the current buffer content as an array of {@code ByteBuffer}.<br>
	 * This method rewinds the underlying buffer to position {@code 0} and sets its limit to the current content size.<br>
	 * It also records the content size to refine future size hints for this packet class.
	 * @return An array containing the internal {@code ByteBuffer}.
	 */
	public ByteBuffer[] toByteBuffers()
	{
		if (_limit > _initialSize)
		{
			MAXIMUM_PACKET_SIZE.put(_packetClass, Math.min(_limit, 65535));
		}
		
		_buf.position(0);
		_buf.limit(_limit);
		return new ByteBuffer[]
		{
			_buf
		};
	}
	
	/**
	 * Copies the current buffer content into a new {@code byte[]}.<br>
	 * This method creates a snapshot of all bytes up to the current limit.
	 * @return A new {@code byte[]} containing the buffer's contents.
	 */
	public byte[] toByteArray()
	{
		final byte[] snapshot = new byte[_limit];
		_buf.get(0, snapshot, 0, _limit);
		return snapshot;
	}
	
	/**
	 * Releases the internal buffer back to the {@code ResourcePool}.<br>
	 * This method clears the internal buffer reference and resets the write position and limit to {@code 0}.
	 */
	public void releaseResources()
	{
		if (_buf != null)
		{
			_resourcePool.recycleBuffer(_buf);
			_buf = null;
		}
		
		_position = 0;
		_limit = 0;
	}
}
