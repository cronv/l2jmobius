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
package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.Set;
import java.util.concurrent.Future;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.DoorAI;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.DoorOpenType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.stat.DoorStat;
import org.l2jmobius.gameserver.entity.actor.status.DoorStatus;
import org.l2jmobius.gameserver.entity.actor.templates.DoorTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.DoorStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;
import org.l2jmobius.gameserver.network.serverpackets.StaticObjectInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a door entity within the game world.<br>
 * This class handles the logic for opening, closing, and interacting with doors.<br>
 * It inherits from {@code Creature} to manage its presence in the game instance.
 */
public class Door extends Creature
{
	boolean _open = false;
	private boolean _isAttackableDoor = false;
	private boolean _isInverted = false;
	private int _meshindex = 1;
	private Future<?> _autoCloseTask;
	
	/**
	 * Creates a new {@code Door} instance using the provided template.<br>
	 * This constructor initializes the door's state based on its configuration.<br>
	 * It sets up properties like open status and attackability.<br>
	 * If the door is time-based, it starts the opening timer.
	 * @param template The {@code DoorTemplate} used to define this door.
	 */
	public Door(DoorTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Door);
		setInvul(false);
		setLethalable(false);
		_open = template.isOpenByDefault();
		_isAttackableDoor = template.isAttackable();
		_isInverted = template.isInverted();
		super.setTargetable(template.isTargetable());
		
		if (isOpenableByTime())
		{
			startTimerOpen();
		}
	}
	
	/**
	 * Initializes the artificial intelligence for this door.<br>
	 * This method creates and returns a new {@code DoorAI} instance.
	 * @return The initialized {@code CreatureAI} for this door.
	 */
	@Override
	protected CreatureAI initAI()
	{
		return new DoorAI(this);
	}
	
	/**
	 * Moves the entity to a specific set of coordinates.<br>
	 * The {@code offset} value determines the final position adjustment.
	 * @param x The target X coordinate.
	 * @param y The target Y coordinate.
	 * @param z The target Z coordinate.
	 * @param offset The additional offset to apply to the location.
	 */
	@Override
	public void moveToLocation(int x, int y, int z, int offset)
	{
	}
	
	/**
	 * Stops the current movement of the door.<br>
	 * This method updates the internal state to halt any active pathfinding.
	 * @param loc The {@code Location} where the movement should stop.
	 */
	@Override
	public void stopMove(Location loc)
	{
	}
	
	/**
	 * Initiates an automatic attack against a specific target.<br>
	 * This method handles the logic for attacking the provided {@code Creature}.
	 * @param target The {@code Creature} to be attacked.
	 */
	@Override
	public void doAutoAttack(Creature target)
	{
	}
	
	/**
	 * Executes a specific skill on the door.<br>
	 * This method handles the logic for when a {@code Skill} is cast by an actor.
	 * @param skill The {@code Skill} to be performed.
	 */
	@Override
	public void doCast(Skill skill)
	{
	}
	
	/**
	 * Starts a timer to open or close the door.<br>
	 * It calculates the required delay based on the {@code DoorTemplate}.<br>
	 * A random time is added if specified in the template.<br>
	 * The action is scheduled using {@code ThreadPool}.
	 */
	private void startTimerOpen()
	{
		int delay = _open ? getTemplate().getOpenTime() : getTemplate().getCloseTime();
		if (getTemplate().getRandomTime() > 0)
		{
			delay += Rnd.get(getTemplate().getRandomTime());
		}
		
		ThreadPool.schedule(new TimerOpen(), delay * 1000);
	}
	
	/**
	 * Retrieves the {@code DoorTemplate} associated with this door.<br>
	 * This template contains the base data for the door entity.
	 * @return the {@code DoorTemplate} of this door.
	 */
	@Override
	public DoorTemplate getTemplate()
	{
		return (DoorTemplate) super.getTemplate();
	}
	
	/**
	 * Retrieves the current status of the door.<br>
	 * This method returns a {@code DoorStatus} object representing the state.
	 * @return The current {@code DoorStatus} of this entity.
	 */
	@Override
	public DoorStatus getStatus()
	{
		return (DoorStatus) super.getStatus();
	}
	
	/**
	 * Initializes the status of this door.<br>
	 * This method creates a new {@code DoorStatus} object for the current instance.
	 */
	@Override
	public void initCharStatus()
	{
		setStatus(new DoorStatus(this));
	}
	
	/**
	 * Initializes the {@code DoorStat} for this door.<br>
	 * This method creates and sets a new {@code DoorStat} instance.
	 */
	@Override
	public void initCharStat()
	{
		setStat(new DoorStat(this));
	}
	
	/**
	 * Retrieves the current statistics for this door.<br>
	 * This method returns a {@code DoorStat} object containing relevant data.
	 * @return the {@code DoorStat} associated with this instance.
	 */
	@Override
	public DoorStat getStat()
	{
		return (DoorStat) super.getStat();
	}
	
	/**
	 * Checks if the door can be opened using a skill.<br>
	 * This method returns {@code true} if the open type is set to {@code BY_SKILL}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the door is openable by a skill, {@code false} otherwise.
	 */
	public boolean isOpenableBySkill()
	{
		return (getTemplate().getOpenType()) == DoorOpenType.BY_SKILL;
	}
	
	/**
	 * Checks if the door can be opened using an item.<br>
	 * This method returns {@code true} if the open type is set to {@code BY_ITEM}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the door is openable by an item, {@code false} otherwise.
	 */
	public boolean isOpenableByItem()
	{
		return (getTemplate().getOpenType()) == DoorOpenType.BY_ITEM;
	}
	
	/**
	 * Checks if the door can be opened by clicking on it.<br>
	 * This method returns {@code true} if the open type is set to {@code BY_CLICK}.
	 * @return {@code true} if clickable, {@code false} otherwise.
	 */
	public boolean isOpenableByClick()
	{
		return (getTemplate().getOpenType()) == DoorOpenType.BY_CLICK;
	}
	
	/**
	 * Checks if the door can be opened using a time-based mechanism.<br>
	 * This method returns {@code true} if the open type is set to {@code BY_TIME}.
	 * @return {@code true} if the door opens by time, {@code false} otherwise.
	 */
	public boolean isOpenableByTime()
	{
		return (getTemplate().getOpenType()) == DoorOpenType.BY_TIME;
	}
	
	/**
	 * Checks if the door can be opened by a cycle.<br>
	 * This method returns {@code true} if the open type is set to {@code BY_CYCLE}.
	 * @return {@code true} if the door is openable by cycle, {@code false} otherwise.
	 */
	public boolean isOpenableByCycle()
	{
		return (getTemplate().getOpenType()) == DoorOpenType.BY_CYCLE;
	}
	
	/**
	 * Retrieves the level of this door.<br>
	 * This value is obtained from the associated {@code DoorTemplate}.
	 * @return the current level as an {@code int}
	 */
	@Override
	public int getLevel()
	{
		return getTemplate().getLevel();
	}
	
	/**
	 * Retrieves the unique identifier for this door.<br>
	 * This value is obtained from the associated {@code DoorTemplate}.
	 * @return the integer ID of the door.
	 */
	@Override
	public int getId()
	{
		return getTemplate().getId();
	}
	
	/**
	 * Checks if the door is currently open.
	 * @return {@code true} if the door is open, {@code false} otherwise.
	 */
	public boolean isOpen()
	{
		return _open;
	}
	
	/**
	 * Sets the open state of the door.<br>
	 * This method updates the {@code _open} field.<br>
	 * It also notifies any sibling doors if a child ID is present.
	 * @param open The new open state to set. Use {@code true} for open and {@code false} for closed.
	 */
	public void setOpen(boolean open)
	{
		final boolean wasOpen = _open;
		_open = open;
		final int childId = getChildId();
		if (childId > 0)
		{
			final Door sibling = getSiblingDoor(childId);
			if (sibling != null)
			{
				sibling.notifyChildEvent(open);
			}
			else
			{
				LOGGER.warning(getClass().getSimpleName() + ": cannot find child id: " + childId);
			}
		}
		
		// Pathfinding rejects steps through a closed door, but a door can also close while someone walks through it.
		// Movement already under way is stopped here, since every close path leads to this method.
		if (wasOpen && !open && checkCollision())
		{
			for (Creature creature : World.getVisibleObjects(this, Creature.class))
			{
				if (creature.isMoving() && DoorData.getInstance().checkIfDoorsBetween(creature.getX(), creature.getY(), creature.getZ(), creature.getXdestination(), creature.getYdestination(), creature.getZdestination(), creature.getInstanceWorld(), false))
				{
					creature.stopMove(null);
				}
			}
		}
	}
	
	/**
	 * Checks if the door can be attacked by players or creatures.<br>
	 * This value is determined by the door's configuration.
	 * @return {@code true} if the door is attackable, {@code false} otherwise.
	 */
	public boolean isAttackableDoor()
	{
		return _isAttackableDoor;
	}
	
	/**
	 * Checks if the door state is inverted.<br>
	 * This determines if the logic for opening and closing is flipped.
	 * @return {@code true} if the door is inverted, {@code false} otherwise.
	 */
	public boolean isInverted()
	{
		return _isInverted;
	}
	
	/**
	 * Checks if the health points of this door should be displayed.<br>
	 * This value is retrieved from the {@code DoorTemplate}.
	 * @return {@code true} if health points are visible, {@code false} otherwise.
	 */
	public boolean isShowHp()
	{
		return getTemplate().isShowHp();
	}
	
	/**
	 * Sets whether this door can be attacked by other entities.<br>
	 * Use {@code true} to allow attacks and {@code false} to prevent them.
	 * @param value The boolean state for attackability.
	 */
	public void setIsAttackableDoor(boolean value)
	{
		_isAttackableDoor = value;
	}
	
	/**
	 * Retrieves the current damage value of this door.<br>
	 * The damage is calculated based on the remaining health percentage.<br>
	 * If the door does not belong to a {@code Castle} or a {@code Fort}, it returns 0.
	 * @return the calculated damage as an {@code int}.
	 */
	public int getDamage()
	{
		if ((getCastle() == null) && (getFort() == null))
		{
			return 0;
		}
		
		final int dmg = 6 - (int) Math.ceil((getCurrentHp() / getMaxHp()) * 6);
		if (dmg > 6)
		{
			return 6;
		}
		
		if (dmg < 0)
		{
			return 0;
		}
		
		return dmg;
	}
	
	/**
	 * Retrieves the {@code Castle} associated with this door.<br>
	 * This method uses the {@code CastleManager} to find the correct instance.
	 * @return the {@code Castle} object or {@code null} if none is found.
	 */
	public Castle getCastle()
	{
		return CastleManager.getInstance().getCastle(this);
	}
	
	/**
	 * Retrieves the {@code Fort} associated with this door.<br>
	 * It uses the {@code FortManager} to find the correct instance.
	 * @return the {@code Fort} object or {@code null} if none exists.
	 */
	public Fort getFort()
	{
		return FortManager.getInstance().getFort(this);
	}
	
	/**
	 * Checks if this door is considered an enemy.<br>
	 * This is determined by the status of the associated castle or fort.<br>
	 * It also requires that health display is enabled.
	 * @return {@code true} if the door is an active enemy, {@code false} otherwise.
	 */
	public boolean isEnemy()
	{
		if ((getCastle() != null) && (getCastle().getResidenceId() > 0) && getCastle().getZone().isActive() && isShowHp())
		{
			return true;
		}
		else if ((getFort() != null) && (getFort().getResidenceId() > 0) && getFort().getZone().isActive() && isShowHp())
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if the door can be targeted by an auto-attack from a specific creature.<br>
	 * Only playable creatures can attack this door.<br>
	 * The result depends on whether the door is marked as attackable or active in a siege zone.<br>
	 * Owners of castles and forts cannot attack their own structures.
	 * @param attacker The {@code Creature} attempting to perform the auto-attack.
	 * @return {@code true} if the door can be attacked by the given creature, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		// Doors can`t be attacked by NPCs.
		if (!attacker.isPlayable())
		{
			return false;
		}
		else if (_isAttackableDoor)
		{
			return true;
		}
		else if (!isShowHp())
		{
			return false;
		}
		
		final Player actingPlayer = attacker.asPlayer();
		
		// Attackable only during siege by everyone except the defending side.
		final boolean isCastle = ((getCastle() != null) && (getCastle().getResidenceId() > 0) && getCastle().getZone().isActive());
		final boolean isFort = ((getFort() != null) && (getFort().getResidenceId() > 0) && getFort().getZone().isActive());
		if (isFort)
		{
			final Clan clan = actingPlayer.getClan();
			if ((clan != null) && (clan == getFort().getOwnerClan()))
			{
				return false;
			}
		}
		else if (isCastle)
		{
			final Clan clan = actingPlayer.getClan();
			if ((clan != null) && ((clan.getId() == getCastle().getOwnerId()) || getCastle().getSiege().checkIsDefender(clan)))
			{
				return false;
			}
		}
		
		return (isCastle || isFort);
	}
	
	/**
	 * Retrieves the current weapon instance being used by the actor.<br>
	 * This method returns {@code null} if no weapon is currently active.
	 * @return the active {@code Item} instance or {@code null}.
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the weapon currently being used by the actor.<br>
	 * This method returns {@code null} if no weapon is active.
	 * @return the current {@code Weapon} object or {@code null}.
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon instance for this entity.<br>
	 * This method currently returns {@code null}.
	 * @return The {@code Item} representing the secondary weapon, or {@code null}.
	 */
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon item for the actor.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Weapon} object if it exists, or {@code null} otherwise.
	 */
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	/**
	 * Sends the current status of this door to all nearby players.<br>
	 * It determines which packet to send based on visibility and permissions.<br>
	 * This method updates the visual state for everyone in range.
	 * @param caster The {@code Creature} that triggered the update.
	 */
	@Override
	public void broadcastStatusUpdate(Creature caster)
	{
		final StaticObjectInfo su = new StaticObjectInfo(this, false);
		final StaticObjectInfo targetableSu = new StaticObjectInfo(this, true);
		final DoorStatusUpdate dsu = new DoorStatusUpdate(this);
		final OnEventTrigger oe = (getEmitter() > 0) ? new OnEventTrigger(getEmitter(), _isInverted ? !_open : _open) : null;
		final boolean useTargetable = ((getCastle() != null) && (getCastle().getResidenceId() > 0)) || ((getFort() != null) && (getFort().getResidenceId() > 0));
		
		World.forEachVisibleObject(this, Player.class, player ->
		{
			if (!isVisibleFor(player))
			{
				return;
			}
			
			if (player.isGM() || useTargetable)
			{
				player.sendPacket(targetableSu);
			}
			else
			{
				player.sendPacket(su);
			}
			
			player.sendPacket(dsu);
			if (oe != null)
			{
				player.sendPacket(oe);
			}
		});
	}
	
	/**
	 * Opens or closes the door based on the provided boolean value.<br>
	 * If {@code true}, it calls the {@code openMe()} method.<br>
	 * If {@code false}, it calls the {@code closeMe()} method.
	 * @param open The state to set for the door.
	 */
	public void openCloseMe(boolean open)
	{
		if (open)
		{
			openMe();
		}
		else
		{
			closeMe();
		}
	}
	
	/**
	 * Opens the door for all players.<br>
	 * This method checks if the door belongs to a group and manages it accordingly.<br>
	 * If the door is currently closed, it sets the state to {@code true}.<br>
	 * It also broadcasts the status update and starts the auto-close task.
	 */
	public void openMe()
	{
		final String groupName = getGroupName();
		if (groupName != null)
		{
			manageGroupOpen(true, groupName);
			return;
		}
		
		if (!isOpen())
		{
			setOpen(true);
			broadcastStatusUpdate();
			startAutoCloseTask();
		}
	}
	
	/**
	 * Closes the door and cancels any pending auto-close tasks.<br>
	 * If the door belongs to a group, it updates the group status.<br>
	 * Otherwise, it sets the open state to {@code false} and broadcasts the update.
	 */
	public void closeMe()
	{
		// Remove close task.
		final Future<?> oldTask = _autoCloseTask;
		if (oldTask != null)
		{
			_autoCloseTask = null;
			oldTask.cancel(false);
		}
		
		final String groupName = getGroupName();
		if (groupName != null)
		{
			manageGroupOpen(false, groupName);
			return;
		}
		
		if (isOpen())
		{
			setOpen(false);
			broadcastStatusUpdate();
		}
	}
	
	/**
	 * Updates the open state for all doors belonging to a specific group.<br>
	 * This method synchronizes the {@code open} status across multiple doors.<br>
	 * It also starts an auto-close task on the first door if it is being opened.
	 * @param open The desired open state to set.
	 * @param groupName The name of the group identifying the doors.
	 */
	private void manageGroupOpen(boolean open, String groupName)
	{
		final Set<Integer> set = DoorData.getInstance().getDoorsByGroup(groupName);
		Door first = null;
		for (Integer id : set)
		{
			final Door door = getSiblingDoor(id);
			if (first == null)
			{
				first = door;
			}
			
			if (door.isOpen() != open)
			{
				door.setOpen(open);
				door.broadcastStatusUpdate();
			}
		}
		
		if ((first != null) && open)
		{
			first.startAutoCloseTask(); // only one from group
		}
	}
	
	/**
	 * Notifies the child entity of a door state change.<br>
	 * This method checks the master door open or close values from the template.<br>
	 * It triggers {@code openMe()} or {@code closeMe()} based on those values.
	 * @param open The desired state of the door to notify.
	 */
	private void notifyChildEvent(boolean open)
	{
		final byte openThis = open ? getTemplate().getMasterDoorOpen() : getTemplate().getMasterDoorClose();
		if (openThis == 1)
		{
			openMe();
		}
		else if (openThis == -1)
		{
			closeMe();
		}
	}
	
	/**
	 * Retrieves the name of the door.<br>
	 * This value is obtained from the associated {@code DoorTemplate}.
	 * @return The name as a {@code String}.
	 */
	@Override
	public String getName()
	{
		return getTemplate().getName();
	}
	
	/**
	 * Retrieves the X coordinate of a specific node from the door template.
	 * @param i The index of the node to retrieve.
	 * @return The X coordinate as an {@code int}.
	 */
	public int getX(int i)
	{
		return getTemplate().getNodeX()[i];
	}
	
	/**
	 * Retrieves the Y coordinate from the template at a specific index.<br>
	 * This method accesses the {@code nodeY} array of the door's template.
	 * @param i The index of the node to retrieve.
	 * @return The Y coordinate as an {@code int}.
	 */
	public int getY(int i)
	{
		return getTemplate().getNodeY()[i];
	}
	
	/**
	 * Retrieves the minimum Z coordinate of the door.<br>
	 * This value is obtained from the {@code DoorTemplate}.
	 * @return The minimum Z coordinate as an {@code int}.
	 */
	public int getZMin()
	{
		// The declared node height can sit above the ground the door stands on, which lets a character walk underneath the collision band.
		// The band is lowered to the terrain, so it starts at walking height.
		// The node height is the reference for the lookup, which keeps doors on upper floors on their own layer.
		final int nodeZ = getTemplate().getNodeZ();
		final int groundZ = GeoEngine.getInstance().getHeight(getX(), getY(), nodeZ);
		
		// One below the terrain, since the collision test wants the crossing height above this value.
		return Math.min(nodeZ, groundZ) - 1;
	}
	
	/**
	 * Retrieves the maximum height of the door.<br>
	 * This value is calculated by adding the node Z coordinate and the height from the {@code DoorTemplate}.
	 * @return The maximum Z coordinate for this door.
	 */
	public int getZMax()
	{
		return getTemplate().getNodeZ() + getTemplate().getHeight();
	}
	
	/**
	 * Retrieves the vertical size of the door.
	 * @return The height defined by the {@code DoorTemplate}.
	 */
	public int getHeight()
	{
		return getTemplate().getHeight();
	}
	
	/**
	 * Sets the visual mesh index for this door.<br>
	 * This value determines which 3D model is displayed.
	 * @param mesh The index of the mesh to use.
	 */
	public void setMeshIndex(int mesh)
	{
		_meshindex = mesh;
	}
	
	/**
	 * Retrieves the mesh index for this door.<br>
	 * This value is used to identify which 3D model to display.
	 * @return The current {@code int} mesh index.
	 */
	public int getMeshIndex()
	{
		return _meshindex;
	}
	
	/**
	 * Retrieves the emitter value from the door template.<br>
	 * This value is used to determine the emission properties of the door.
	 * @return the emitter value as an {@code int}.
	 */
	public int getEmitter()
	{
		return getTemplate().getEmmiter();
	}
	
	/**
	 * Checks if the door functions as a wall.<br>
	 * This method returns {@code true} if the door is considered a wall based on its template.
	 * @return {@code true} if it is a wall, {@code false} otherwise.
	 */
	public boolean isWall()
	{
		return getTemplate().isWall();
	}
	
	/**
	 * Retrieves the group name associated with this door.<br>
	 * This value is fetched from the {@code DoorTemplate}.
	 * @return The group name as a {@code String}.
	 */
	public String getGroupName()
	{
		return getTemplate().getGroupName();
	}
	
	/**
	 * Retrieves the unique identifier for the child door.<br>
	 * This value is obtained from the associated {@code DoorTemplate}.
	 * @return the integer ID of the child door.
	 */
	public int getChildId()
	{
		return getTemplate().getChildDoorId();
	}
	
	/**
	 * Reduces the current health points of this entity based on an attack.<br>
	 * It checks if the damage should be applied based on the environment and attacker type.<br>
	 * If conditions are met, it calls the superclass method to apply the reduction.
	 * @param value The amount of damage to subtract from the health.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param skill The {@code Skill} used to perform the action.
	 * @param isDOT Whether this damage is a Damage Over Time effect.
	 * @param directlyToHp Whether the damage bypasses armor and goes directly to health.
	 * @param critical Whether the attack is a critical hit.
	 * @param reflect Whether the damage should be reflected back to the attacker.
	 */
	@Override
	public void reduceCurrentHp(double value, Creature attacker, Skill skill, boolean isDOT, boolean directlyToHp, boolean critical, boolean reflect)
	{
		if (isWall() && !isInInstance())
		{
			if (!attacker.isServitor())
			{
				return;
			}
			
			if (attacker.getTemplate().getRace() != Race.SIEGE_WEAPON)
			{
				return;
			}
		}
		
		// Beasts cannot attack castle gates.
		if (attacker.isPet() && (getCastle() != null) && (getCastle().getResidenceId() > 0))
		{
			return;
		}
		
		super.reduceCurrentHp(value, attacker, skill, isDOT, directlyToHp, critical, reflect);
	}
	
	/**
	 * Handles the logic for when this door is destroyed.<br>
	 * It checks if the door belongs to a castle or fort during a siege.<br>
	 * If it is part of a siege, it broadcasts a destruction message.
	 * @param killer The {@code Creature} that killed this door.
	 * @return {@code true} if the death logic was successful, {@code false} otherwise.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		final boolean isFort = ((getFort() != null) && (getFort().getResidenceId() > 0) && getFort().getSiege().isInProgress());
		final boolean isCastle = ((getCastle() != null) && (getCastle().getResidenceId() > 0) && getCastle().getSiege().isInProgress());
		if (isFort || isCastle)
		{
			broadcastPacket(new SystemMessage(SystemMessageId.THE_CASTLE_GATE_HAS_BEEN_DESTROYED));
		}
		else
		{
			openMe();
		}
		
		return true;
	}
	
	/**
	 * Sends the door information to a specific player.<br>
	 * This method checks if the {@code Player} can see this object.<br>
	 * It sends packets for static object info and status updates.<br>
	 * It also handles event triggers based on the emitter state.
	 * @param player The {@code Player} who will receive the information.
	 */
	@Override
	public void sendInfo(Player player)
	{
		if (isVisibleFor(player))
		{
			player.sendPacket(new StaticObjectInfo(this, player.isGM()));
			player.sendPacket(new DoorStatusUpdate(this));
			final int emitter = getEmitter();
			if (emitter > 0)
			{
				if (_isInverted)
				{
					player.sendPacket(new OnEventTrigger(emitter, !_open));
				}
				else
				{
					player.sendPacket(new OnEventTrigger(emitter, _open));
				}
			}
		}
	}
	
	/**
	 * Sets whether this entity can be targeted by other actors.<br>
	 * This method also broadcasts the updated status to all nearby players.
	 * @param targetable The new targetable state. Use {@code true} to allow targeting or {@code false} to prevent it.
	 */
	@Override
	public void setTargetable(boolean targetable)
	{
		super.setTargetable(targetable);
		broadcastStatusUpdate();
	}
	
	/**
	 * Checks if the door has collision enabled.<br>
	 * This method retrieves the collision status from the {@code DoorTemplate}.
	 * @return {@code true} if collisions are checked, {@code false} otherwise.
	 */
	public boolean checkCollision()
	{
		return getTemplate().isCheckCollision();
	}
	
	/**
	 * Finds the {@code Door} associated with a specific ID.<br>
	 * It first checks the current instance for the door.<br>
	 * If not found, it retrieves the door from the global data.
	 * @param doorId The unique identifier of the door to find.
	 * @return The {@code Door} object matching the provided ID.
	 */
	private Door getSiblingDoor(int doorId)
	{
		final Instance inst = getInstanceWorld();
		return (inst != null) ? inst.getDoor(doorId) : DoorData.getInstance().getDoor(doorId);
	}
	
	/**
	 * Starts a background task to close the door automatically.<br>
	 * This method checks if the door is eligible for auto-closing based on its template and status.<br>
	 * It cancels any existing {@code _autoCloseTask} before scheduling a new one using {@code ThreadPool}.<br>
	 * The delay is calculated by multiplying the template close time by {@code 1000}.
	 */
	private void startAutoCloseTask()
	{
		final int closeTime = getTemplate().getCloseTime();
		if ((closeTime < 0) || isOpenableByTime())
		{
			return;
		}
		
		final Future<?> oldTask = _autoCloseTask;
		if (oldTask != null)
		{
			_autoCloseTask = null;
			oldTask.cancel(false);
		}
		
		_autoCloseTask = ThreadPool.schedule(new AutoClose(), closeTime * 1000);
	}
	
	class AutoClose implements Runnable
	{
		@Override
		public void run()
		{
			if (_open)
			{
				closeMe();
			}
		}
	}
	
	class TimerOpen implements Runnable
	{
		@Override
		public void run()
		{
			if (_open)
			{
				closeMe();
			}
			else
			{
				openMe();
			}
			
			int delay = _open ? getTemplate().getCloseTime() : getTemplate().getOpenTime();
			if (getTemplate().getRandomTime() > 0)
			{
				delay += Rnd.get(getTemplate().getRandomTime());
			}
			
			ThreadPool.schedule(this, delay * 1000);
		}
	}
	
	/**
	 * Checks if the current entity is a door.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the entity is a door.
	 */
	@Override
	public boolean isDoor()
	{
		return true;
	}
	
	/**
	 * Returns the current {@code Door} instance.<br>
	 * This method allows for method chaining.
	 * @return the current {@code Door} object.
	 */
	@Override
	public Door asDoor()
	{
		return this;
	}
	
	/**
	 * Returns a string representation of the {@code Door} object.<br>
	 * This includes the class name, template ID, and object ID.
	 * @return A formatted string describing the current instance.
	 */
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append('[');
		sb.append(getTemplate().getId());
		sb.append("](");
		sb.append(getObjectId());
		sb.append(')');
		return sb.toString();
	}
}
