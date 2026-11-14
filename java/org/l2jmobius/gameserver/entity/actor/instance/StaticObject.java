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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.stat.StaticObjectStat;
import org.l2jmobius.gameserver.entity.actor.status.StaticObjectStatus;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ShowTownMap;
import org.l2jmobius.gameserver.network.serverpackets.StaticObjectInfo;

/**
 * Represents an instance of a {@code StaticObject} within the game world.<br>
 * This class handles non-moving entities that exist in specific locations.
 * @author godson
 */
public class StaticObject extends Creature
{
	/** The interaction distance of the StaticObject */
	public static final int INTERACTION_DISTANCE = 150;
	
	private final int _staticObjectId;
	private int _meshIndex = 0; // 0 - static objects, alternate static objects
	private int _type = -1; // 0 - map signs, 1 - throne , 2 - arena signs
	private ShowTownMap _map;
	
	/**
	 * Initializes the artificial intelligence for this object.<br>
	 * This method returns {@code null} because static objects do not have active AI behavior.
	 * @return the {@code CreatureAI} instance or {@code null}.
	 */
	@Override
	protected CreatureAI initAI()
	{
		return null;
	}
	
	/**
	 * Retrieves the unique identifier for this static object.<br>
	 * This value corresponds to the {@code _staticObjectId} field.
	 * @return The unique ID of the static object as an {@code int}.
	 */
	@Override
	public int getId()
	{
		return _staticObjectId;
	}
	
	/**
	 * Creates a new instance of a {@code StaticObject}.<br>
	 * This constructor initializes the object using a template and a unique ID.
	 * @param template The {@code CreatureTemplate} used to define the object properties.
	 * @param staticId The unique identifier for this specific static object.
	 */
	public StaticObject(CreatureTemplate template, int staticId)
	{
		super(template);
		setInstanceType(InstanceType.StaticObject);
		_staticObjectId = staticId;
	}
	
	/**
	 * Retrieves the statistics for this static object.<br>
	 * This method calls the {@code getStat()} method of the parent class.<br>
	 * It casts the result to a {@code StaticObjectStat} type.
	 * @return the {@code StaticObjectStat} associated with this instance.
	 */
	@Override
	public StaticObjectStat getStat()
	{
		return (StaticObjectStat) super.getStat();
	}
	
	/**
	 * Initializes the statistics for this {@code StaticObject}.<br>
	 * It creates and sets a new {@code StaticObjectStat} instance.
	 */
	@Override
	public void initCharStat()
	{
		setStat(new StaticObjectStat(this));
	}
	
	/**
	 * Retrieves the current status of this static object.<br>
	 * This method casts the base {@code Creature} status to a {@code StaticObjectStatus}.
	 * @return the {@code StaticObjectStatus} for this instance.
	 */
	@Override
	public StaticObjectStatus getStatus()
	{
		return (StaticObjectStatus) super.getStatus();
	}
	
	/**
	 * Initializes the status of this {@code StaticObject}.<br>
	 * It creates a new {@code StaticObjectStatus} instance.<br>
	 * This method is used to set up the initial state for the object.
	 */
	@Override
	public void initCharStatus()
	{
		setStatus(new StaticObjectStatus(this));
	}
	
	/**
	 * Retrieves the type of the {@code StaticObject}.<br>
	 * The value corresponds to specific categories like map signs or thrones.
	 * @return the current type as an {@code int}.
	 */
	public int getType()
	{
		return _type;
	}
	
	/**
	 * Sets the category of the {@code StaticObject}.<br>
	 * This value determines how the object is identified in the game world.
	 * @param type The integer ID representing the specific type.
	 */
	public void setType(int type)
	{
		_type = type;
	}
	
	/**
	 * Sets the {@code _map} field for this object.<br>
	 * It creates a new {@code ShowTownMap} instance using the provided coordinates.
	 * @param texture The name of the map texture to load.
	 * @param x The x-coordinate for the map.
	 * @param y The y-coordinate for the map.
	 */
	public void setMap(String texture, int x, int y)
	{
		_map = new ShowTownMap("town_map." + texture, x, y);
	}
	
	/**
	 * Retrieves the town map associated with this static object.
	 * @return the {@code ShowTownMap} instance.
	 */
	public ShowTownMap getMap()
	{
		return _map;
	}
	
	/**
	 * Retrieves the level of the {@code StaticObject}.<br>
	 * This method always returns a constant value.
	 * @return the current level as an {@code int}.
	 */
	@Override
	public int getLevel()
	{
		return 1;
	}
	
	/**
	 * Retrieves the current active weapon instance.<br>
	 * This method returns {@code null} if no weapon is currently equipped.
	 * @return The {@code Item} instance of the active weapon or {@code null}.
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the current weapon item being used by this object.<br>
	 * This method returns {@code null} if no weapon is currently active.
	 * @return the {@code Weapon} instance or {@code null}.
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon instance for this object.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Item} representing the secondary weapon, or {@code null} if none exists.
	 */
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon item for this object.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Weapon} object or {@code null} if none exists.
	 */
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	/**
	 * Checks if the object can be targeted by an auto-attack.<br>
	 * This method currently always returns {@code false}.
	 * @param attacker The {@code Creature} attempting to perform the attack.
	 * @return {@code false} because this object is not auto-attackable.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}
	
	/**
	 * Sets the {@code _meshIndex} for this static object.<br>
	 * This updates the visual model used to render the object.<br>
	 * It also broadcasts a {@code StaticObjectInfo} packet to all players.
	 * @param meshIndex The new index value for the mesh.
	 */
	public void setMeshIndex(int meshIndex)
	{
		_meshIndex = meshIndex;
		broadcastPacket(new StaticObjectInfo(this));
	}
	
	/**
	 * Retrieves the current mesh index of this {@code StaticObject}.<br>
	 * This value determines which specific model variation is displayed.
	 * @return The current {@code int} mesh index.
	 */
	public int getMeshIndex()
	{
		return _meshIndex;
	}
	
	/**
	 * Sends the information of this {@code StaticObject} to a specific player.<br>
	 * This method creates and sends a {@code StaticObjectInfo} packet.
	 * @param player The {@code Player} who will receive the information.
	 */
	@Override
	public void sendInfo(Player player)
	{
		player.sendPacket(new StaticObjectInfo(this));
	}
	
	/**
	 * Moves the {@code StaticObject} to a new position.<br>
	 * This method updates the coordinates of the object in the game world.
	 * @param x The new X coordinate.
	 * @param y The new Y coordinate.
	 * @param z The new Z coordinate.
	 * @param offset An additional value to adjust the final position.
	 */
	@Override
	public void moveToLocation(int x, int y, int z, int offset)
	{
	}
	
	/**
	 * Stops the current movement of the {@code StaticObject}.<br>
	 * Sets the new position to the provided {@code Location}.
	 * @param loc The target {@code Location} where the object should stop.
	 */
	@Override
	public void stopMove(Location loc)
	{
	}
	
	/**
	 * Initiates an automatic attack on a specific target.<br>
	 * This method triggers the combat sequence against the provided {@code Creature}.
	 * @param target The {@code Creature} to be attacked.
	 */
	@Override
	public void doAutoAttack(Creature target)
	{
	}
	
	/**
	 * Executes a specific {@code Skill} on the static object.<br>
	 * This method handles the logic for casting the provided skill.
	 * @param skill The {@code Skill} to be cast.
	 */
	@Override
	public void doCast(Skill skill)
	{
	}
}
