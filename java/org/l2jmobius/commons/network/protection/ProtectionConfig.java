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

import org.l2jmobius.commons.util.ConfigReader;

/**
 * Holds the connection protection settings shared by the game and login servers.
 * @author Mobius
 */
public class ProtectionConfig
{
	// File
	private static final String NETWORK_CONFIG_FILE = "config/Network.ini";
	
	// Constants
	public static boolean PACKET_RATE_LIMIT_ENABLED;
	public static int PACKET_RATE_LIMIT;
	public static int PACKET_RATE_BURST;
	public static int PACKET_QUEUE_LIMIT;
	
	/**
	 * Loads the protection settings from the already opened network configuration.
	 * @param config The {@code ConfigReader} holding the network properties.
	 */
	public static void load(ConfigReader config)
	{
		loadRateLimit(config);
		
		// The execution queue is sized once at startup, so this value cannot change while the server runs.
		PACKET_QUEUE_LIMIT = Math.max(1, config.getInt("PacketQueueLimit", 100000));
	}
	
	/**
	 * Re-reads the settings that take effect without a restart, leaving the queue limit at its startup value.
	 */
	public static void reload()
	{
		loadRateLimit(new ConfigReader(NETWORK_CONFIG_FILE));
	}
	
	/**
	 * Reads the rate limit applied to every incoming packet.
	 * @param config The {@code ConfigReader} holding the network properties.
	 */
	private static void loadRateLimit(ConfigReader config)
	{
		// Values are clamped because a zero or negative limit would reject every packet and lock all clients out.
		PACKET_RATE_LIMIT_ENABLED = config.getBoolean("PacketRateLimitEnabled", true);
		PACKET_RATE_LIMIT = Math.max(1, config.getInt("PacketRateLimit", 250));
		PACKET_RATE_BURST = Math.max(PACKET_RATE_LIMIT, config.getInt("PacketRateBurst", 500));
	}
}
