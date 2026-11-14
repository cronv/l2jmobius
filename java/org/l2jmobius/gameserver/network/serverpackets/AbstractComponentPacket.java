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

import org.l2jmobius.gameserver.interfaces.MaskComponent;

/**
 * Base class for server packets whose body is assembled from selectable components.<br>
 * Each component is tracked as a single bit inside a mask byte array, most significant bit first,<br>
 * and only the data blocks of added components are written to the client.
 * @author Mobius
 * @param <T>
 */
public abstract class AbstractComponentPacket<T extends MaskComponent>extends ServerPacket
{
	protected abstract byte[] getMasks();
	
	/**
	 * Called the first time a component is added to this packet.
	 * @param component the component that was added
	 */
	protected void onComponentAdded(T component)
	{
	}
	
	/**
	 * Adds the given components to this packet, ignoring components already present.
	 * @param components the components to add
	 */
	@SafeVarargs
	public final void addComponentType(T... components)
	{
		for (T component : components)
		{
			if (!containsMask(component))
			{
				addMask(component.getMask());
				onComponentAdded(component);
			}
		}
	}
	
	/**
	 * Sets the mask bit that corresponds to the given mask value.
	 * @param mask the mask value
	 */
	protected void addMask(int mask)
	{
		getMasks()[mask >> 3] |= (byte) (0x80 >>> (mask & 7));
	}
	
	/**
	 * Checks if the given component has already been added.
	 * @param component the component to check
	 * @return {@code true} if the component bit is set
	 */
	public boolean containsMask(T component)
	{
		return containsMask(component.getMask());
	}
	
	/**
	 * Checks if the mask bit that corresponds to the given mask value is set.
	 * @param mask the mask value
	 * @return {@code true} if the bit is set
	 */
	public boolean containsMask(int mask)
	{
		return (getMasks()[mask >> 3] & (0x80 >>> (mask & 7))) != 0;
	}
	
	/**
	 * Checks if the given component flag is contained in a flag set.
	 * @param masks the flag set
	 * @param type the component to check
	 * @return {@code true} if every bit of the component mask is present in {@code masks}
	 */
	public boolean containsMask(int masks, T type)
	{
		return (masks & type.getMask()) == type.getMask();
	}
}
