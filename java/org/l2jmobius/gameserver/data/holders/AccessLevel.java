/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class defines the permission levels for different types of users.<br>
 * It is used to control access to specific game features and administrative commands.
 * @author HorridoJoho
 */
public class AccessLevel
{
	/** The access level. */
	private int _accessLevel = 0;
	/** The access level name. */
	private String _name = null;
	/** Child access levels. */
	AccessLevel _childsAccessLevel = null;
	/** Child access levels. */
	private int _child = 0;
	/** The name color for the access level. */
	private int _nameColor = 0;
	/** The title color for the access level. */
	private int _titleColor = 0;
	/** Flag to determine if the access level has GM access. */
	private boolean _isGm = false;
	/** Flag for peace zone attack */
	private boolean _allowPeaceAttack = false;
	/** Flag for fixed res */
	private boolean _allowFixedRes = false;
	/** Flag for transactions */
	private boolean _allowTransaction = false;
	/** Flag for AltG commands */
	private boolean _allowAltG = false;
	/** Flag to give damage */
	private boolean _giveDamage = false;
	/** Flag to take aggro */
	private boolean _takeAggro = false;
	/** Flag to gain exp in party */
	private boolean _gainExp = false;
	
	/**
	 * Creates a new {@code AccessLevel} instance using data from a {@code StatSet}.<br>
	 * This constructor initializes all properties like level, colors, and permissions.
	 * @param set The {@code StatSet} containing the configuration values.
	 */
	public AccessLevel(StatSet set)
	{
		_accessLevel = set.getInt("level");
		_name = set.getString("name");
		_nameColor = Integer.decode("0x" + set.getString("nameColor", "FFFFFF"));
		_titleColor = Integer.decode("0x" + set.getString("titleColor", "FFFFFF"));
		_child = set.getInt("childAccess", 0);
		_isGm = set.getBoolean("isGM", false);
		_allowPeaceAttack = set.getBoolean("allowPeaceAttack", false);
		_allowFixedRes = set.getBoolean("allowFixedRes", false);
		_allowTransaction = set.getBoolean("allowTransaction", true);
		_allowAltG = set.getBoolean("allowAltg", false);
		_giveDamage = set.getBoolean("giveDamage", true);
		_takeAggro = set.getBoolean("takeAggro", true);
		_gainExp = set.getBoolean("gainExp", true);
	}
	
	/**
	 * Creates a new {@code AccessLevel} object with default values.<br>
	 * The default name is set to {@code User}.<br>
	 * All permissions are initialized to their standard starting states.
	 */
	public AccessLevel()
	{
		_accessLevel = 0;
		_name = "User";
		_nameColor = Integer.decode("0xFFFFFF");
		_titleColor = Integer.decode("0xFFFFFF");
		_child = 0;
		_isGm = false;
		_allowPeaceAttack = false;
		_allowFixedRes = false;
		_allowTransaction = true;
		_allowAltG = false;
		_giveDamage = true;
		_takeAggro = true;
		_gainExp = true;
	}
	
	/**
	 * Retrieves the current access level.<br>
	 * This value represents the numerical rank of the {@code AccessLevel}.
	 * @return The integer value of the access level.
	 */
	public int getLevel()
	{
		return _accessLevel;
	}
	
	/**
	 * Retrieves the name of the {@code AccessLevel}.<br>
	 * This method returns the string identifier for this specific level.
	 * @return The name of the access level as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Returns the color code for the name of this access level.<br>
	 * This value is used to determine how the name appears in the game.
	 * @return The {@code int} color value.
	 */
	public int getNameColor()
	{
		return _nameColor;
	}
	
	/**
	 * Retrieves the color value for the title of this {@code AccessLevel}.<br>
	 * This value is used to determine how the title text is displayed.
	 * @return The integer color code for the title.
	 */
	public int getTitleColor()
	{
		return _titleColor;
	}
	
	/**
	 * Checks if the current {@code AccessLevel} has Game Master privileges.<br>
	 * This method returns the value of the internal GM flag.
	 * @return {@code true} if the access level is a Game Master, {@code false} otherwise.
	 */
	public boolean isGm()
	{
		return _isGm;
	}
	
	/**
	 * Checks if the current access level permits attacking in peace zones.<br>
	 * This method returns the value of the {@code _allowPeaceAttack} flag.
	 * @return {@code true} if attacks are allowed, {@code false} otherwise.
	 */
	public boolean allowPeaceAttack()
	{
		return _allowPeaceAttack;
	}
	
	/**
	 * Checks if the current access level allows fixed resistance.<br>
	 * This determines if specific resistance values can be set manually.
	 * @return {@code true} if fixed resistance is allowed, {@code false} otherwise.
	 */
	public boolean allowFixedRes()
	{
		return _allowFixedRes;
	}
	
	/**
	 * Checks if the current access level allows transactions.<br>
	 * This method returns the value of the {@code _allowTransaction} flag.
	 * @return {@code true} if transactions are allowed, {@code false} otherwise.
	 */
	public boolean allowTransaction()
	{
		return _allowTransaction;
	}
	
	/**
	 * Checks if the current access level allows AltG commands.<br>
	 * This is used to determine if specific gold-related actions are permitted.
	 * @return {@code true} if AltG is allowed, {@code false} otherwise.
	 */
	public boolean allowAltG()
	{
		return _allowAltG;
	}
	
	/**
	 * Checks if this access level is allowed to deal damage.<br>
	 * This returns the value of the {@code _giveDamage} flag.
	 * @return {@code true} if damage can be dealt, {@code false} otherwise.
	 */
	public boolean canGiveDamage()
	{
		return _giveDamage;
	}
	
	/**
	 * Checks if this access level is allowed to take aggro.<br>
	 * This returns the value of the {@code _takeAggro} flag.
	 * @return {@code true} if aggro can be taken, {@code false} otherwise.
	 */
	public boolean canTakeAggro()
	{
		return _takeAggro;
	}
	
	/**
	 * Checks if the current access level is allowed to gain experience.<br>
	 * This method returns the value of the {@code _gainExp} flag.
	 * @return {@code true} if experience can be gained, {@code false} otherwise.
	 */
	public boolean canGainExp()
	{
		return _gainExp;
	}
	
	/**
	 * Checks if the current level has permission to access a specific child level.<br>
	 * This method recursively checks the hierarchy of {@code AccessLevel}.
	 * @param accessLevel The {@code AccessLevel} to check for permissions.
	 * @return {@code true} if the access is granted, {@code false} otherwise.
	 */
	public boolean hasChildAccess(AccessLevel accessLevel)
	{
		if (_childsAccessLevel == null)
		{
			if (_child <= 0)
			{
				return false;
			}
			
			_childsAccessLevel = AdminData.getInstance().getAccessLevel(_child);
		}
		
		return (_childsAccessLevel != null) && ((_childsAccessLevel.getLevel() == accessLevel.getLevel()) || _childsAccessLevel.hasChildAccess(accessLevel));
	}
}
