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
package org.l2jmobius.gameserver.entity.clan;

/**
 * This class manages the {@code ClanAccess} permissions for clan members.<br>
 * It uses a bitmask representation to efficiently store and check various privileges.
 * @author Mobius
 */
public class ClanPrivileges
{
	private int _mask;
	
	/**
	 * Creates a new instance of {@code ClanPrivileges}.<br>
	 * This constructor initializes the privilege mask to {@code 0}.<br>
	 * All privileges are disabled by default.
	 */
	public ClanPrivileges()
	{
		_mask = 0;
	}
	
	/**
	 * Creates a new {@code ClanPrivileges} instance with a specific bitmask.<br>
	 * The {@code mask} determines which permissions are active for the clan.
	 * @param mask The integer value representing the set of privileges.
	 */
	public ClanPrivileges(int mask)
	{
		_mask = mask;
	}
	
	/**
	 * Retrieves the current privilege mask.<br>
	 * This value represents all enabled permissions for the clan.
	 * @return The current {@code int} mask.
	 */
	public int getMask()
	{
		return _mask;
	}
	
	/**
	 * Updates the internal bitmask for clan privileges.<br>
	 * This method sets the current privilege state to the provided value.
	 * @param mask The new {@code int} value representing the set of privileges.
	 */
	public void setMask(int mask)
	{
		_mask = mask;
	}
	
	/**
	 * Disables all current privileges.<br>
	 * This method sets the internal mask to {@code 0}.
	 */
	public void disableAll()
	{
		_mask = 0;
	}
	
	/**
	 * Enables all available privileges for the clan.<br>
	 * This method sets the internal mask to the value returned by {@code getCompleteMask()}.
	 */
	public void enableAll()
	{
		_mask = getCompleteMask();
	}
	
	/**
	 * Returns a bitmask representing all possible privileges.<br>
	 * This method iterates through every value in {@code ClanAccess}.<br>
	 * It combines them into a single integer mask.
	 * @return The complete bitmask as an {@code int}.
	 */
	public static int getCompleteMask()
	{
		int mask = 0;
		for (ClanAccess access : ClanAccess.values())
		{
			mask |= 1 << access.ordinal();
		}
		
		return mask;
	}
	
	/**
	 * Updates the set of allowed privileges for this clan.<br>
	 * This method first clears all existing permissions.<br>
	 * It then enables only the specific permissions provided in the arguments.
	 * @param privileges The list of {@code ClanAccess} types to enable.
	 */
	public void setPrivileges(ClanAccess... privileges)
	{
		disableAll();
		for (ClanAccess access : privileges)
		{
			_mask |= 1 << access.ordinal();
		}
	}
	
	/**
	 * Sets the base requirements for clan access.<br>
	 * This method clears all existing permissions first.<br>
	 * It then applies a set of required {@code ClanAccess} levels.
	 * @param minimum The primary {@code ClanAccess} level required.
	 * @param privileges Additional {@code ClanAccess} levels to include.
	 */
	public void setMinimumPrivileges(ClanAccess minimum, ClanAccess... privileges)
	{
		disableAll();
		addMinimumPrivileges(minimum, privileges);
	}
	
	/**
	 * Adds specific permissions to the current set of clan privileges.<br>
	 * This method updates the internal mask by including the {@code minimum} privilege and any additional {@code privileges}.<br>
	 * It uses bitwise operations to ensure that these permissions are enabled.
	 * @param minimum The base {@code ClanAccess} permission to add.
	 * @param privileges A variable number of additional {@code ClanAccess} permissions to add.
	 */
	public void addMinimumPrivileges(ClanAccess minimum, ClanAccess... privileges)
	{
		_mask |= 1 << minimum.ordinal();
		for (ClanAccess access : privileges)
		{
			_mask |= 1 << access.ordinal();
		}
	}
	
	/**
	 * Removes specific permissions from the current set of clan privileges.<br>
	 * This method updates the internal mask by disabling the specified rights.
	 * @param minimum The first {@code ClanAccess} privilege to remove.
	 * @param privileges A variable number of additional {@code ClanAccess} privileges to remove.
	 */
	public void removeMinimumPrivileges(ClanAccess minimum, ClanAccess... privileges)
	{
		_mask &= ~(1 << minimum.ordinal());
		for (ClanAccess access : privileges)
		{
			_mask &= ~(1 << access.ordinal());
		}
	}
	
	/**
	 * Checks if the current privileges include a specific set of requirements.<br>
	 * It verifies that both the {@code minimum} privilege and all additional {@code privileges} are active.
	 * @param minimum The base {@code ClanAccess} required to pass the check.
	 * @param privileges A variable number of extra {@code ClanAccess} types that must also be present.
	 * @return {@code true} if all specified privileges are held, otherwise {@code false}.
	 */
	public boolean hasMinimumPrivileges(ClanAccess minimum, ClanAccess... privileges)
	{
		if ((_mask & (1 << minimum.ordinal())) == 0)
		{
			return false;
		}
		
		for (ClanAccess access : privileges)
		{
			if ((_mask & (1 << access.ordinal())) == 0)
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Creates a copy of the current {@code ClanPrivileges} object.<br>
	 * This method returns a new instance with the same mask value.
	 * @return A new {@code ClanPrivileges} instance.
	 */
	@Override
	public ClanPrivileges clone()
	{
		return new ClanPrivileges(_mask);
	}
}
