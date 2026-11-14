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
package org.l2jmobius.tools.dashboard.health;

import org.l2jmobius.tools.dashboard.telemetry.RuntimeSnapshot;

/**
 * Stores the reduced metric set used by history charts.
 * @param timestampMillis sample timestamp in milliseconds
 * @param processCpuRatio process CPU utilization ratio
 * @param systemCpuRatio system CPU utilization ratio
 * @param heapRatio JVM heap utilization ratio
 * @param threadCount live JVM thread count
 * @param onlinePlayers current online player count
 * @param maximumPlayers configured maximum player count
 * @param authenticatedClients authenticated LoginServer client count
 * @author BazookaRpm
 */
public record RuntimeSample(long timestampMillis, double processCpuRatio, double systemCpuRatio, double heapRatio, int threadCount, int onlinePlayers, int maximumPlayers, int authenticatedClients)
{
	public static RuntimeSample from(long timestampMillis, RuntimeSnapshot snapshot)
	{
		return new RuntimeSample(timestampMillis, snapshot.processCpuRatio(), snapshot.systemCpuRatio(), snapshot.heapRatio(), snapshot.liveThreads(), snapshot.onlinePlayers(), snapshot.maximumPlayers(), snapshot.authenticatedClients());
	}
}
