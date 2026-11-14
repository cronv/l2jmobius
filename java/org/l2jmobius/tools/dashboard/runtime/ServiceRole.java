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

import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;

/**
 * Defines immutable launch and health-monitoring metadata for one managed server process.
 * @param roleId stable role identifier
 * @param label human-readable service name
 * @param workingFolder distribution working-folder name
 * @param artifactName executable server JAR name
 * @param configPortKey Server.ini key containing the service port
 * @param servicePortSetting Control Center setting containing the service port fallback
 * @param servicePortDefault default client-facing service port
 * @author BazookaRpm
 */
public record ServiceRole(String roleId, String label, String workingFolder, String artifactName, String configPortKey, String servicePortSetting, int servicePortDefault)
{
	public static final ServiceRole LOGIN = new ServiceRole("LOGIN", "Login Server", "login", "LoginServer.jar", "LoginserverPort", ControlCenterSettings.LOGIN_PORT, 2106);
	public static final ServiceRole GAME = new ServiceRole("GAME", "Game Server", "game", "GameServer.jar", "GameserverPort", ControlCenterSettings.GAME_PORT, 7777);
}
