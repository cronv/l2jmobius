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
package org.l2jmobius.tools.dashboard.tools;

import org.l2jmobius.tools.dashboard.runtime.ServiceRole;

/**
 * Describes official L2J Mobius utility applications exposed as dedicated Dashboard pages.
 * @author BazookaRpm
 */
public enum OfficialTool
{
	MAP_EXTRACTOR("Map Extractor", "Generate geodata, doors and water zones from supported client map data.", ServiceRole.GAME, "org.l2jmobius.tools.MapExtractor", "GameServer.jar", "MapExtractor.vbs", "MapExtractor.sh", false),
	DROP_EDITOR("Drop List Editor", "Edit NPC drop lists directly in the source datapack with XML backup and conflict protection.", ServiceRole.GAME, "org.l2jmobius.tools.editor.DropEditor", "GameServer.jar", "DropEditor.vbs", "DropEditor.sh", true),
	SEARCH("Search", "Search the Game Server distribution for text or regular expressions across supported data files.", ServiceRole.GAME, "org.l2jmobius.tools.Search", "GameServer.jar", "Search.vbs", "Search.sh", false),
	ACCOUNT_MANAGER("Account Manager", "Create, inspect, update and remove game accounts through the official Login Server utility.", ServiceRole.LOGIN, "org.l2jmobius.tools.AccountManager", "LoginServer.jar", "AccountManager.vbs", "AccountManager.sh", false),
	GAME_SERVER_REGISTER("GameServer Register", "Register or unregister Game Server IDs and generate the official authentication HexID.", ServiceRole.LOGIN, "org.l2jmobius.tools.GameServerRegister", "LoginServer.jar", "GameServerRegister.vbs", "GameServerRegister.sh", true);
	
	private final String _label;
	private final String _description;
	private final ServiceRole _workingRole;
	private final String _mainClass;
	private final String _jarName;
	private final String _windowsScript;
	private final String _unixScript;
	private final boolean _servicesMustBeOffline;
	
	OfficialTool(String label, String description, ServiceRole workingRole, String mainClass, String jarName, String windowsScript, String unixScript, boolean servicesMustBeOffline)
	{
		_label = label;
		_description = description;
		_workingRole = workingRole;
		_mainClass = mainClass;
		_jarName = jarName;
		_windowsScript = windowsScript;
		_unixScript = unixScript;
		_servicesMustBeOffline = servicesMustBeOffline;
	}
	
	public String label()
	{
		return _label;
	}
	
	public String description()
	{
		return _description;
	}
	
	public ServiceRole workingRole()
	{
		return _workingRole;
	}
	
	public String mainClass()
	{
		return _mainClass;
	}
	
	public String jarName()
	{
		return _jarName;
	}
	
	public String windowsScript()
	{
		return _windowsScript;
	}
	
	public String unixScript()
	{
		return _unixScript;
	}
	
	public boolean servicesMustBeOffline()
	{
		return _servicesMustBeOffline;
	}
	
	public String classEntry()
	{
		return _mainClass.replace('.', '/') + ".class";
	}
}
