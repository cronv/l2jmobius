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

/**
 * Represents the lifecycle phase observed by the dashboard.
 * @author BazookaRpm
 */
public enum ServicePhase
{
	OFFLINE(0, "Offline", false, false),
	LAUNCHING(10, "Launching", true, false),
	ONLINE(20, "Online", true, true),
	STOPPING(30, "Stopping", true, false),
	FAILED(40, "Failed", false, false),
	EXTERNAL(50, "External process", true, true);
	
	private final int _code;
	private final String _label;
	private final boolean _processExpected;
	private final boolean _acceptingConnections;
	
	ServicePhase(int code, String label, boolean processExpected, boolean acceptingConnections)
	{
		_code = code;
		_label = label;
		_processExpected = processExpected;
		_acceptingConnections = acceptingConnections;
	}
	
	public int getCode()
	{
		return _code;
	}
	
	public String getLabel()
	{
		return _label;
	}
	
	public boolean isProcessExpected()
	{
		return _processExpected;
	}
	
	public boolean isAcceptingConnections()
	{
		return _acceptingConnections;
	}
	
	public boolean isTerminal()
	{
		return (this == OFFLINE) || (this == FAILED);
	}
}
