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
package org.l2jmobius.commons.network.protection;

/**
 * Meters the incoming packet rate of a single connection using a token bucket.<br>
 * Each connection owns one guard, so clients behind a shared address are metered independently.
 * @author Mobius
 */
public class FloodGuard
{
	private static final double NANOS_IN_SECOND = 1000000000.0;
	
	// Accessed only on the single async-read thread serving this connection.
	private double _tokens = -1;
	private long _lastRefill = System.nanoTime();
	
	/**
	 * Refills the bucket for the elapsed time and consumes a token for one incoming packet.
	 * @return {@code true} if the packet is within the configured rate, {@code false} if the connection is flooding.
	 */
	public boolean allow()
	{
		if (!ProtectionConfig.PACKET_RATE_LIMIT_ENABLED)
		{
			return true;
		}
		
		// Seeded on first use because the guard may be constructed before the configuration is loaded.
		if (_tokens < 0)
		{
			_tokens = ProtectionConfig.PACKET_RATE_BURST;
		}
		
		final long now = System.nanoTime();
		final double refill = ((now - _lastRefill) / NANOS_IN_SECOND) * ProtectionConfig.PACKET_RATE_LIMIT;
		_lastRefill = now;
		
		_tokens = Math.min(_tokens + refill, ProtectionConfig.PACKET_RATE_BURST);
		if (_tokens < 1)
		{
			return false;
		}
		
		_tokens--;
		return true;
	}
}
