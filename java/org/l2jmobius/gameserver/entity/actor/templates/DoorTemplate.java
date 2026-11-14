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
package org.l2jmobius.gameserver.entity.actor.templates;

import org.l2jmobius.gameserver.entity.actor.enums.creature.DoorOpenType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class serves as a template for door entities in the game world.<br>
 * It defines the base properties and behaviors shared by all types of doors.
 * @author JIV
 */
public class DoorTemplate extends CreatureTemplate
{
	private final int _doorId;
	private final int[] _nodeX;
	private final int[] _nodeY;
	private final int _nodeZ;
	private final int _height;
	private final int _posX;
	private final int _posY;
	private final int _posZ;
	private final int _emmiter;
	private final int _childDoorId;
	private final String _name;
	private final String _groupName;
	private final boolean _showHp;
	private final boolean _isWall;
	
	// -1 close, 0 nothing, 1 open
	private final byte _masterDoorClose;
	private final byte _masterDoorOpen;
	
	private final boolean _isTargetable;
	private final boolean _default_status;
	
	private int _openTime;
	private int _randomTime;
	private final int _closeTime;
	private final int _level;
	private final DoorOpenType _openType;
	private final boolean _checkCollision;
	private final boolean _isAttackableDoor;
	private final boolean _stealth;
	private final boolean _isInverted;
	
	/**
	 * Creates a new {@code DoorTemplate} using the provided statistics.<br>
	 * This constructor initializes all door properties from the {@code StatSet}.<br>
	 * It sets values for position, dimensions, and behavior flags.
	 * @param set The {@code StatSet} containing the configuration data.
	 */
	public DoorTemplate(StatSet set)
	{
		super(set);
		_doorId = set.getInt("id");
		_name = set.getString("name");
		
		// position
		_height = set.getInt("height", 150);
		_nodeZ = set.getInt("nodeZ");
		_nodeX = new int[4]; // 4 * x
		_nodeY = new int[4]; // 4 * y
		for (int i = 0; i < 4; i++)
		{
			_nodeX[i] = set.getInt("nodeX_" + i);
			_nodeY[i] = set.getInt("nodeY_" + i);
		}
		
		_posX = set.getInt("x");
		_posY = set.getInt("y");
		_posZ = Math.min(set.getInt("z"), _nodeZ);
		
		// optional
		_emmiter = set.getInt("emmiterId", 0);
		_showHp = set.getBoolean("showHp", true);
		_isWall = set.getBoolean("isWall", false);
		_groupName = set.getString("group", null);
		_childDoorId = set.getInt("childId", -1);
		
		// true if door is opening
		String masterevent = set.getString("masterClose", "act_nothing");
		_masterDoorClose = (byte) (masterevent.equals("act_open") ? 1 : masterevent.equals("act_close") ? -1 : 0);
		masterevent = set.getString("masterOpen", "act_nothing");
		_masterDoorOpen = (byte) (masterevent.equals("act_open") ? 1 : masterevent.equals("act_close") ? -1 : 0);
		_isTargetable = set.getBoolean("targetable", true);
		_default_status = set.getString("default", "close").equals("open");
		_closeTime = set.getInt("closeTime", -1);
		_level = set.getInt("level", 0);
		_openType = set.getEnum("openMethod", DoorOpenType.class, DoorOpenType.NONE);
		_checkCollision = set.getBoolean("isCheckCollision", true);
		if (_openType == DoorOpenType.BY_TIME)
		{
			_openTime = set.getInt("openTime");
			_randomTime = set.getInt("randomTime", -1);
		}
		
		_isAttackableDoor = set.getBoolean("attackable", false);
		_stealth = set.getBoolean("stealth", false);
		_isInverted = set.getBoolean("isInverted", false);
	}
	
	/**
	 * Retrieves the unique identifier for this door.<br>
	 * This value is used to identify the specific {@code DoorTemplate}.
	 * @return The unique {@code int} ID of the door.
	 */
	public int getId()
	{
		return _doorId;
	}
	
	/**
	 * Retrieves the display name of the door.<br>
	 * This value is used to identify the door in the game world.
	 * @return The {@code String} name of the door template.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the X coordinates of the nodes.<br>
	 * This returns an array of integers representing the horizontal positions.
	 * @return an {@code int[]} containing the node X values.
	 */
	public int[] getNodeX()
	{
		return _nodeX;
	}
	
	/**
	 * Retrieves the Y coordinates of the nodes.<br>
	 * This returns an array of integers representing vertical positions.
	 * @return an {@code int[]} containing the node Y values.
	 */
	public int[] getNodeY()
	{
		return _nodeY;
	}
	
	/**
	 * Retrieves the Z coordinate of a node.<br>
	 * This value is part of the door's spatial data.
	 * @return The integer value of the {@code _nodeZ} coordinate.
	 */
	public int getNodeZ()
	{
		return _nodeZ;
	}
	
	/**
	 * Returns the height of the door.<br>
	 * This value represents the vertical size of the {@code DoorTemplate}.
	 * @return The height as an {@code int}.
	 */
	public int getHeight()
	{
		return _height;
	}
	
	/**
	 * Returns the X coordinate of the door.<br>
	 * This value represents the horizontal position in the world.
	 * @return The {@code int} value of the X coordinate.
	 */
	public int getX()
	{
		return _posX;
	}
	
	/**
	 * Returns the vertical position of the door.<br>
	 * This value corresponds to the {@code _posY} field.
	 * @return The current Y coordinate as an {@code int}.
	 */
	public int getY()
	{
		return _posY;
	}
	
	/**
	 * Retrieves the Z coordinate of the door.<br>
	 * This value represents the depth position in the 3D space.
	 * @return The {@code int} value of the Z coordinate.
	 */
	public int getZ()
	{
		return _posZ;
	}
	
	/**
	 * Retrieves the emitter ID for this door.<br>
	 * This value identifies the specific effect or particle associated with the door.
	 * @return The {@code int} value of the emitter.
	 */
	public int getEmmiter()
	{
		return _emmiter;
	}
	
	/**
	 * Retrieves the unique identifier for a child door.<br>
	 * This value links this door to another specific door template.
	 * @return The {@code int} ID of the child door.
	 */
	public int getChildDoorId()
	{
		return _childDoorId;
	}
	
	/**
	 * Retrieves the name of the group associated with this door.<br>
	 * This value is used to identify which group a door belongs to.
	 * @return The {@code String} name of the group.
	 */
	public String getGroupName()
	{
		return _groupName;
	}
	
	/**
	 * Checks if the health points of this door are visible.<br>
	 * Returns {@code true} if health is shown.<br>
	 * Returns {@code false} if health is hidden.
	 * @return The visibility status of the health points.
	 */
	public boolean isShowHp()
	{
		return _showHp;
	}
	
	/**
	 * Checks if this door acts as a wall.<br>
	 * This determines if the object blocks movement or vision.
	 * @return {@code true} if it is a wall, {@code false} otherwise.
	 */
	public boolean isWall()
	{
		return _isWall;
	}
	
	/**
	 * Retrieves the master door open state.<br>
	 * This value represents the default opening status for a master door.
	 * @return The {@code byte} value representing the master door open state.
	 */
	public byte getMasterDoorOpen()
	{
		return _masterDoorOpen;
	}
	
	/**
	 * Retrieves the master door close state.<br>
	 * This value determines if the door is closed, open, or has no specific state.
	 * @return The {@code byte} value representing the master door close status.
	 */
	public byte getMasterDoorClose()
	{
		return _masterDoorClose;
	}
	
	/**
	 * Checks if this door can be targeted by other entities.<br>
	 * Returns {@code true} if the door is targetable.<br>
	 * Returns {@code false} if it cannot be targeted.
	 * @return a boolean indicating whether the door is targetable.
	 */
	public boolean isTargetable()
	{
		return _isTargetable;
	}
	
	/**
	 * Checks if the door starts in an open state.<br>
	 * This method returns the default status of the door.
	 * @return {@code true} if the door is open by default, {@code false} otherwise.
	 */
	public boolean isOpenByDefault()
	{
		return _default_status;
	}
	
	/**
	 * Retrieves the time it takes for the door to open.<br>
	 * This value is stored in the {@code _openTime} field.
	 * @return The opening time as an {@code int}.
	 */
	public int getOpenTime()
	{
		return _openTime;
	}
	
	/**
	 * Retrieves the random time value for this door.<br>
	 * This value is used to determine timing behaviors.
	 * @return the {@code int} value of the random time.
	 */
	public int getRandomTime()
	{
		return _randomTime;
	}
	
	/**
	 * Returns the time it takes for a door to close.<br>
	 * This value is retrieved from the {@code _closeTime} field.
	 * @return The closing time as an {@code int}.
	 */
	public int getCloseTime()
	{
		return _closeTime;
	}
	
	/**
	 * Retrieves the level of the door.<br>
	 * This value represents the difficulty or rank of the template.
	 * @return The current level as an {@code int}.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the opening type of the door.<br>
	 * This value determines how the door behaves when it opens.
	 * @return the {@code DoorOpenType} of this door.
	 */
	public DoorOpenType getOpenType()
	{
		return _openType;
	}
	
	/**
	 * Checks if the door has collision enabled.<br>
	 * This determines if entities can physically interact with the door.
	 * @return {@code true} if collisions are checked, {@code false} otherwise.
	 */
	public boolean isCheckCollision()
	{
		return _checkCollision;
	}
	
	/**
	 * Checks if the door can be attacked by other entities.<br>
	 * This returns {@code true} if the door is marked as attackable.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if the door is attackable, {@code false} otherwise.
	 */
	public boolean isAttackable()
	{
		return _isAttackableDoor;
	}
	
	/**
	 * Checks if the door is set to stealth mode.<br>
	 * Returns {@code true} if stealth is enabled.<br>
	 * Returns {@code false} if stealth is disabled.
	 * @return The stealth status of the door.
	 */
	public boolean isStealth()
	{
		return _stealth;
	}
	
	/**
	 * Checks if the door's state logic is inverted.<br>
	 * Returns {@code true} if the inversion flag is set.<br>
	 * Returns {@code false} otherwise.
	 * @return The current inversion status of the door.
	 */
	public boolean isInverted()
	{
		return _isInverted;
	}
}
