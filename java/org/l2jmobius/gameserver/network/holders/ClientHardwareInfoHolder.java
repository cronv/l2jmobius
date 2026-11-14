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
package org.l2jmobius.gameserver.network.holders;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.variables.AccountVariables;

/**
 * Holds the client OS, CPU and graphics hardware specifications reported on login.
 * @author Mobius
 */
public class ClientHardwareInfoHolder
{
	private final String _macAddress;
	private final int _windowsPlatformId;
	private final int _windowsMajorVersion;
	private final int _windowsMinorVersion;
	private final int _windowsBuildNumber;
	private final int _directxVersion;
	private final int _directxRevision;
	private final String _cpuName;
	private final int _cpuSpeed;
	private final int _cpuCoreCount;
	private final int _vgaCount;
	private final int _vgaPcxSpeed;
	private final int _physMemorySlot1;
	private final int _physMemorySlot2;
	private final int _physMemorySlot3;
	private final int _videoMemory;
	private final int _vgaVersion;
	private final String _vgaName;
	private final String _vgaDriverVersion;
	
	/**
	 * Constructs a holder capturing the client OS, CPU and graphics card hardware specifications.
	 * @param macAddress MAC address of the network interface
	 * @param windowsPlatformId Windows platform identifier
	 * @param windowsMajorVersion Windows major version number
	 * @param windowsMinorVersion Windows minor version number
	 * @param windowsBuildNumber Windows build number
	 * @param directxVersion installed DirectX version
	 * @param directxRevision DirectX revision number
	 * @param cpuName processor model name
	 * @param cpuSpeed CPU clock speed
	 * @param cpuCoreCount total number of CPU cores
	 * @param vgaCount number of graphics cards detected
	 * @param vgaPcxSpeed PCX speed of the video card
	 * @param physMemorySlot1 capacity of the first physical memory slot
	 * @param physMemorySlot2 capacity of the second physical memory slot
	 * @param physMemorySlot3 capacity of the third physical memory slot
	 * @param videoMemory amount of video memory
	 * @param vgaVersion VGA hardware version
	 * @param vgaName graphics card name
	 * @param vgaDriverVersion VGA driver version
	 */
	public ClientHardwareInfoHolder(String macAddress, int windowsPlatformId, int windowsMajorVersion, int windowsMinorVersion, int windowsBuildNumber, int directxVersion, int directxRevision, String cpuName, int cpuSpeed, int cpuCoreCount, int vgaCount, int vgaPcxSpeed, int physMemorySlot1, int physMemorySlot2, int physMemorySlot3, int videoMemory, int vgaVersion, String vgaName, String vgaDriverVersion)
	{
		_macAddress = macAddress;
		_windowsPlatformId = windowsPlatformId;
		_windowsMajorVersion = windowsMajorVersion;
		_windowsMinorVersion = windowsMinorVersion;
		_windowsBuildNumber = windowsBuildNumber;
		_directxVersion = directxVersion;
		_directxRevision = directxRevision;
		_cpuName = cpuName;
		_cpuSpeed = cpuSpeed;
		_cpuCoreCount = cpuCoreCount;
		_vgaCount = vgaCount;
		_vgaPcxSpeed = vgaPcxSpeed;
		_physMemorySlot1 = physMemorySlot1;
		_physMemorySlot2 = physMemorySlot2;
		_physMemorySlot3 = physMemorySlot3;
		_videoMemory = videoMemory;
		_vgaVersion = vgaVersion;
		_vgaName = vgaName;
		_vgaDriverVersion = vgaDriverVersion;
	}
	
	/**
	 * Constructs a holder by parsing a delimited raw hardware string.
	 * @param info raw hardware information string to parse
	 */
	public ClientHardwareInfoHolder(String info)
	{
		final String[] split = info.split(AccountVariables.HWIDSLIT_VAR);
		_macAddress = split[0];
		_windowsPlatformId = Integer.valueOf(split[1]);
		_windowsMajorVersion = Integer.valueOf(split[2]);
		_windowsMinorVersion = Integer.valueOf(split[3]);
		_windowsBuildNumber = Integer.valueOf(split[4]);
		_directxVersion = Integer.valueOf(split[5]);
		_directxRevision = Integer.valueOf(split[6]);
		_cpuName = split[7];
		_cpuSpeed = Integer.valueOf(split[8]);
		_cpuCoreCount = Integer.valueOf(split[9]);
		_vgaCount = Integer.valueOf(split[10]);
		_vgaPcxSpeed = Integer.valueOf(split[11]);
		_physMemorySlot1 = Integer.valueOf(split[12]);
		_physMemorySlot2 = Integer.valueOf(split[13]);
		_physMemorySlot3 = Integer.valueOf(split[14]);
		_videoMemory = Integer.valueOf(split[15]);
		_vgaVersion = Integer.valueOf(split[16]);
		_vgaName = split[17];
		_vgaDriverVersion = split[18];
	}
	
	/**
	 * Builds a delimited hardware id string from all fields and saves it to the player account variable.
	 * @param player player whose account variables are updated
	 */
	public void store(Player player)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(_macAddress);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_windowsPlatformId);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_windowsMajorVersion);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_windowsMinorVersion);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_windowsBuildNumber);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_directxVersion);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_directxRevision);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_cpuName);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_cpuSpeed);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_cpuCoreCount);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaCount);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaPcxSpeed);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_physMemorySlot1);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_physMemorySlot2);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_physMemorySlot3);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_videoMemory);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaVersion);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaName);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaDriverVersion);
		player.getAccountVariables().set(AccountVariables.HWID, sb.toString());
	}
	
	/**
	 * Returns the client network interface MAC address.
	 * @return the MAC address
	 */
	public String getMacAddress()
	{
		return _macAddress;
	}
	
	/**
	 * Returns the Windows platform identifier.
	 * @return the Windows platform id
	 */
	public int getWindowsPlatformId()
	{
		return _windowsPlatformId;
	}
	
	/**
	 * Returns the Windows major version number.
	 * @return the Windows major version
	 */
	public int getWindowsMajorVersion()
	{
		return _windowsMajorVersion;
	}
	
	/**
	 * Returns the Windows minor version number.
	 * @return the Windows minor version
	 */
	public int getWindowsMinorVersion()
	{
		return _windowsMinorVersion;
	}
	
	/**
	 * Returns the Windows build number.
	 * @return the Windows build number
	 */
	public int getWindowsBuildNumber()
	{
		return _windowsBuildNumber;
	}
	
	/**
	 * Returns the installed DirectX version.
	 * @return the DirectX version
	 */
	public int getDirectxVersion()
	{
		return _directxVersion;
	}
	
	/**
	 * Returns the DirectX revision number.
	 * @return the DirectX revision
	 */
	public int getDirectxRevision()
	{
		return _directxRevision;
	}
	
	/**
	 * Returns the processor model name.
	 * @return the CPU name
	 */
	public String getCpuName()
	{
		return _cpuName;
	}
	
	/**
	 * Returns the CPU clock speed.
	 * @return the CPU speed
	 */
	public int getCpuSpeed()
	{
		return _cpuSpeed;
	}
	
	/**
	 * Returns the total number of CPU cores.
	 * @return the CPU core count
	 */
	public int getCpuCoreCount()
	{
		return _cpuCoreCount;
	}
	
	/**
	 * Returns the number of VGA cards detected.
	 * @return the VGA card count
	 */
	public int getVgaCount()
	{
		return _vgaCount;
	}
	
	/**
	 * Returns the VGA PCX speed.
	 * @return the VGA PCX speed
	 */
	public int getVgaPcxSpeed()
	{
		return _vgaPcxSpeed;
	}
	
	/**
	 * Returns the capacity of the first physical memory slot.
	 * @return the first physical memory slot value
	 */
	public int getPhysMemorySlot1()
	{
		return _physMemorySlot1;
	}
	
	/**
	 * Returns the capacity of the second physical memory slot.
	 * @return the second physical memory slot value
	 */
	public int getPhysMemorySlot2()
	{
		return _physMemorySlot2;
	}
	
	/**
	 * Returns the capacity of the third physical memory slot.
	 * @return the third physical memory slot value
	 */
	public int getPhysMemorySlot3()
	{
		return _physMemorySlot3;
	}
	
	/**
	 * Returns the amount of video memory.
	 * @return the video memory
	 */
	public int getVideoMemory()
	{
		return _videoMemory;
	}
	
	/**
	 * Returns the VGA hardware version.
	 * @return the VGA version
	 */
	public int getVgaVersion()
	{
		return _vgaVersion;
	}
	
	/**
	 * Returns the graphics card name.
	 * @return the VGA name
	 */
	public String getVgaName()
	{
		return _vgaName;
	}
	
	/**
	 * Returns the VGA driver version.
	 * @return the VGA driver version
	 */
	public String getVgaDriverVersion()
	{
		return _vgaDriverVersion;
	}
	
	/**
	 * Compares this holder to another by MAC address equality after a type check.
	 * @param obj object to compare against
	 * @return {@code true} if both are holders with equal MAC addresses, {@code false} otherwise
	 */
	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof ClientHardwareInfoHolder) && _macAddress.equals(((ClientHardwareInfoHolder) obj).getMacAddress());
	}
}
