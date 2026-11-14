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
package org.l2jmobius.loginserver.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * This class provides utility methods for handling and validating IP subnets.<br>
 * It helps determine if a specific {@code InetAddress} belongs to a defined range.
 * @author Mobius
 */
public class Subnet
{
	private final byte[] _address;
	private final byte[] _subnetMask;
	private final boolean _v4;
	
	/**
	 * Creates a new {@code Subnet} instance from a string.<br>
	 * The input should be in CIDR notation, such as "192.168.1.0/24".<br>
	 * It parses the IP address and calculates the subnet mask automatically.
	 * @param input The network address with prefix length to parse.
	 * @throws UnknownHostException If the IP address in the string is invalid.
	 */
	public Subnet(String input) throws UnknownHostException
	{
		final String[] parts = input.split("/");
		final InetAddress inetAddress = InetAddress.getByName(parts[0]);
		_address = inetAddress.getAddress();
		_v4 = _address.length == 4;
		
		// Determine prefix length, with defaults for IPv4 (32) and IPv6 (128).
		int prefixLength = _v4 ? 32 : 128;
		if (parts.length > 1)
		{
			prefixLength = Integer.parseInt(parts[1]);
		}
		
		// Validate prefix length.
		if ((prefixLength < 0) || (prefixLength > (_address.length * 8)))
		{
			throw new IllegalArgumentException("Invalid prefix length: " + prefixLength);
		}
		
		// Create mask based on prefix length.
		final int fullBytes = prefixLength / 8;
		final int remainingBits = prefixLength % 8;
		_subnetMask = new byte[_address.length];
		Arrays.fill(_subnetMask, 0, fullBytes, (byte) 0xFF);
		if (remainingBits > 0)
		{
			_subnetMask[fullBytes] = (byte) (0xFF << (8 - remainingBits));
		}
		
		// Apply mask to address.
		for (int i = 0; i < _address.length; i++)
		{
			_address[i] &= _subnetMask[i];
		}
	}
	
	/**
	 * Checks if the provided IP address belongs to this subnet.<br>
	 * This method handles both IPv4 and IPv6 addresses.<br>
	 * It also supports checking IPv4 addresses embedded within IPv6 formats.
	 * @param address The {@code byte[]} array representing the IP address to check.
	 * @return {@code true} if the address is in the subnet, otherwise {@code false}.
	 */
	public boolean isInSubnet(byte[] address)
	{
		// IPv4 and IPv6 address length mismatch.
		if (address.length != _address.length)
		{
			if (!_v4 || (address.length != 16))
			{
				return false;
			}
			
			// Check for IPv4-in-IPv6 address format (::ffff:192.168.x.x).
			// @formatter:off
			final byte[] ipv4In6Prefix = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xFF, (byte) 0xFF };
			// @formatter:on
			for (int i = 0; i < ipv4In6Prefix.length; i++)
			{
				if (address[i] != ipv4In6Prefix[i])
				{
					return false;
				}
			}
			
			// Extract IPv4 portion.
			address = Arrays.copyOfRange(address, 12, 16);
		}
		
		// Apply mask to check subnet match.
		for (int i = 0; i < address.length; i++)
		{
			if ((address[i] & _subnetMask[i]) != _address[i])
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the IP address associated with this {@code Subnet}.<br>
	 * This method returns a copy of the internal byte array.
	 * @return a {@code byte[]} containing the address.
	 */
	public byte[] getAddress()
	{
		return _address.clone();
	}
	
	/**
	 * Compares this {@code Subnet} to the specified object.<br>
	 * It checks if the provided object falls within this subnet's range.<br>
	 * This method returns {@code true} if the objects are equal or related.
	 * @param obj The object to compare with this one.
	 * @return {@code true} if the object is part of this subnet, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		
		if (obj instanceof Subnet)
		{
			return isInSubnet(((Subnet) obj)._address);
		}
		
		if (obj instanceof InetAddress)
		{
			return isInSubnet(((InetAddress) obj).getAddress());
		}
		
		return false;
	}
	
	/**
	 * Returns a string representation of the subnet.<br>
	 * It displays the IP address followed by the prefix length.<br>
	 * If the address is invalid, it returns {@code Invalid address}.
	 * @return A string describing the subnet.
	 */
	@Override
	public String toString()
	{
		int prefixLength = 0;
		for (byte b : _subnetMask)
		{
			prefixLength += Integer.bitCount(b & 0xFF);
		}
		
		try
		{
			return InetAddress.getByAddress(_address).getHostAddress() + "/" + prefixLength;
		}
		catch (UnknownHostException e)
		{
			return "Invalid address";
		}
	}
}
