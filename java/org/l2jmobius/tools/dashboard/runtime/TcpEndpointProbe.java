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
package org.l2jmobius.tools.dashboard.runtime;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;

/**
 * Tests a loopback endpoint through the asynchronous channel API with an explicit timeout.
 * @author BazookaRpm
 */
public final class TcpEndpointProbe
{
	private TcpEndpointProbe()
	{
	}
	
	public static boolean acceptsConnections(int port, int timeoutMillis)
	{
		if ((port <= 0) || (port > 65535))
		{
			return false;
		}
		
		AsynchronousSocketChannel channel = null;
		try
		{
			channel = AsynchronousSocketChannel.open();
			channel.connect(new InetSocketAddress("127.0.0.1", port)).get(Math.max(50, timeoutMillis), TimeUnit.MILLISECONDS);
			return true;
		}
		catch (Exception exception)
		{
			return false;
		}
		finally
		{
			if (channel != null)
			{
				try
				{
					channel.close();
				}
				catch (Exception ignored)
				{
					// The probe result has already been determined.
				}
			}
		}
	}
}
