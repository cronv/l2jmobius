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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.tools.dashboard.runtime.ServicePhase;
import org.l2jmobius.tools.dashboard.telemetry.RuntimeSnapshot;

/**
 * Evaluates deterministic warning thresholds without performing automatic remediation.
 * @author BazookaRpm
 */
public final class HealthPolicy
{
	private HealthPolicy()
	{
	}
	
	public static List<HealthFinding> evaluate(ServicePhase phase, RuntimeSnapshot snapshot, int cpuWarningPercent, int heapWarningPercent)
	{
		final List<HealthFinding> findings = new ArrayList<>();
		if ((phase == ServicePhase.FAILED) || (phase == ServicePhase.OFFLINE))
		{
			findings.add(new HealthFinding(HealthFinding.Level.WARNING, "The service is not online."));
			return findings;
		}
		
		if ((snapshot == null) || !snapshot.available())
		{
			findings.add(new HealthFinding(HealthFinding.Level.INFORMATION, "JMX telemetry is not available yet."));
			return findings;
		}
		
		final double cpuPercent = snapshot.processCpuRatio() * 100.0;
		if ((snapshot.processCpuRatio() >= 0) && (cpuPercent >= cpuWarningPercent))
		{
			findings.add(new HealthFinding(HealthFinding.Level.WARNING, "Process CPU is above " + cpuWarningPercent + "% (" + Math.round(cpuPercent) + "%)."));
		}
		
		final double heapPercent = snapshot.heapRatio() * 100.0;
		if ((snapshot.heapRatio() >= 0) && (heapPercent >= heapWarningPercent))
		{
			findings.add(new HealthFinding(HealthFinding.Level.WARNING, "JVM heap usage is above " + heapWarningPercent + "% (" + Math.round(heapPercent) + "%)."));
		}
		
		if (snapshot.liveThreads() > 2000)
		{
			findings.add(new HealthFinding(HealthFinding.Level.WARNING, "The JVM currently has more than 2,000 live threads."));
		}
		
		if (findings.isEmpty())
		{
			findings.add(new HealthFinding(HealthFinding.Level.INFORMATION, "No configured health threshold is currently exceeded."));
		}
		
		return findings;
	}
}
