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
package org.l2jmobius.tools.dashboard.telemetry;

/**
 * Captures one point-in-time view of server and JVM telemetry.
 * @param available whether runtime telemetry is currently available
 * @param serviceName managed service display name
 * @param javaVersion managed JVM version
 * @param onlinePlayers current online player count
 * @param maximumPlayers configured maximum player count
 * @param connectedGameServers connected GameServer count
 * @param authenticatedClients authenticated LoginServer client count
 * @param liveThreads current live JVM thread count
 * @param peakThreads peak JVM thread count
 * @param daemonThreads current daemon thread count
 * @param loadedClasses currently loaded JVM class count
 * @param heapUsedBytes used JVM heap bytes
 * @param heapMaximumBytes maximum JVM heap bytes
 * @param nonHeapUsedBytes used JVM non-heap bytes
 * @param metaspaceUsedBytes used JVM metaspace bytes
 * @param garbageCollectionCount cumulative garbage collection count
 * @param garbageCollectionMillis cumulative garbage collection time in milliseconds
 * @param uptimeMillis managed JVM uptime in milliseconds
 * @param processCpuRatio process CPU utilization ratio
 * @param systemCpuRatio system CPU utilization ratio
 * @param systemMemoryUsedBytes used physical system memory bytes
 * @param systemMemoryTotalBytes total physical system memory bytes
 * @author BazookaRpm
 */
public record RuntimeSnapshot(boolean available, String serviceName, String javaVersion, int onlinePlayers, int maximumPlayers, int connectedGameServers, int authenticatedClients, int liveThreads, int peakThreads, int daemonThreads, int loadedClasses, long heapUsedBytes, long heapMaximumBytes, long nonHeapUsedBytes, long metaspaceUsedBytes, long garbageCollectionCount, long garbageCollectionMillis, long uptimeMillis, double processCpuRatio, double systemCpuRatio, long systemMemoryUsedBytes, long systemMemoryTotalBytes)
{
	public static RuntimeSnapshot unavailable()
	{
		return new RuntimeSnapshot(false, "Unavailable", "Unknown", -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1);
	}
	
	public double heapRatio()
	{
		if (heapMaximumBytes <= 0)
		{
			return -1;
		}
		
		return Math.min(1.0, Math.max(0.0, (double) heapUsedBytes / heapMaximumBytes));
	}
}
