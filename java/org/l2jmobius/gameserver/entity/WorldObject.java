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
package org.l2jmobius.gameserver.entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.instance.Servitor;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.ActionClickHandler;
import org.l2jmobius.gameserver.handler.ActionShiftHandler;
import org.l2jmobius.gameserver.handler.IActionClickHandler;
import org.l2jmobius.gameserver.handler.IActionShiftHandler;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.interfaces.IPositionable;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.TriggerCastInfo;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Base type for every interactive object in the game world, providing identity, position and instance state.
 */
public abstract class WorldObject extends ListenersContainer implements IPositionable
{
	/** Name */
	protected String _name;
	/** Object ID */
	private int _objectId;
	/** World Region */
	private WorldRegion _worldRegion;
	/** Location */
	private final Location _location = new Location(0, 0, -10000);
	/** Instance */
	private Instance _instance;
	/** Instance type */
	private InstanceType _instanceType;
	private boolean _isSpawned;
	private boolean _isInvisible;
	private boolean _isTargetable = true;
	private Map<String, Object> _scripts;
	
	/** TriggerCast Queue */
	private final ConcurrentLinkedQueue<TriggerCastInfo> _triggerCastQueue = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean _triggerCasting = new AtomicBoolean();
	
	/**
	 * Creates a {@code WorldObject} with the given id, defaulting its {@code InstanceType} to {@code WorldObject}.
	 * @param objectId The unique object id.
	 */
	public WorldObject(int objectId)
	{
		setInstanceType(InstanceType.WorldObject);
		_objectId = objectId;
	}
	
	/**
	 * Retrieves this object's {@code InstanceType}.
	 * @return The {@code InstanceType} of this object.
	 */
	public InstanceType getInstanceType()
	{
		return _instanceType;
	}
	
	/**
	 * Sets this object's {@code InstanceType}.
	 * @param newInstanceType The {@code InstanceType} to assign.
	 */
	protected void setInstanceType(InstanceType newInstanceType)
	{
		_instanceType = newInstanceType;
	}
	
	/**
	 * Checks whether this object's {@code InstanceType} matches any of the given types.
	 * @param instanceTypes The {@code InstanceType}s to test against.
	 * @return {@code true} if any matches, {@code false} otherwise.
	 */
	public boolean isInstanceTypes(InstanceType... instanceTypes)
	{
		return _instanceType.isTypes(instanceTypes);
	}
	
	/**
	 * Handles a player's click action on this object, defaulting to an interaction.
	 * @param player The acting {@code Player}.
	 */
	public void onAction(Player player)
	{
		onAction(player, true);
	}
	
	/**
	 * Handles a player's click action on this object via the registered handler, then sends an action-failed packet.
	 * @param player The acting {@code Player}.
	 * @param interact {@code true} if the player is interacting with the object.
	 */
	public void onAction(Player player, boolean interact)
	{
		final IActionClickHandler handler = ActionClickHandler.getInstance().getHandler(getInstanceType());
		if (handler != null)
		{
			handler.onAction(player, this, interact);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Handles a player's shift-click action on this object via the registered handler, then sends an action-failed packet.
	 * @param player The acting {@code Player}.
	 */
	public void onActionShift(Player player)
	{
		final IActionShiftHandler handler = ActionShiftHandler.getInstance().getHandler(getInstanceType());
		if (handler != null)
		{
			handler.onAction(player, this, true);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Handles a player's forced-attack action on this object by sending an action-failed packet.
	 * @param player The acting {@code Player}.
	 */
	public void onForcedAttack(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Called once this object has been spawned into the world.
	 */
	public void onSpawn()
	{
	}
	
	/**
	 * Removes this object from the world and clears its spawned state.
	 * @return {@code true} once removed.
	 */
	public boolean decayMe()
	{
		_isSpawned = false;
		World.removeVisibleObject(this, _worldRegion);
		World.removeObject(this);
		return true;
	}
	
	/**
	 * Removes this object from the world and assigns it a freshly allocated object id.
	 */
	public void refreshId()
	{
		World.removeObject(this);
		IdManager.getInstance().releaseId(getObjectId());
		_objectId = IdManager.getInstance().getNextId();
	}
	
	/**
	 * Spawns this object into the world at its current position and runs {@code onSpawn()}.
	 * @return {@code true} once spawned, {@code false} when the position is out of the world.
	 */
	public boolean spawnMe()
	{
		synchronized (this)
		{
			// An object out of the world has no region, it stays unspawned.
			final WorldRegion region = World.getRegion(this);
			if (region == null)
			{
				return false;
			}
			
			// Set the x,y,z position of the WorldObject spawn and update its _worldregion.
			_isSpawned = true;
			setWorldRegion(region);
			
			// Add the WorldObject spawn in the _allobjects of World.
			World.addObject(this);
			
			// Add the WorldObject spawn to _visibleObjects and if necessary to _allplayers of its WorldRegion.
			_worldRegion.addVisibleObject(this);
		}
		
		// This can synchronize on others instances, so it's out of synchronized, to avoid deadlocks.
		// Add the WorldObject spawn in the world as a visible object.
		World.addVisibleObject(this, getWorldRegion());
		
		onSpawn();
		
		return true;
	}
	
	/**
	 * Spawns this object at the given coordinates, clamped to the world bounds.
	 * @param x The world X coordinate.
	 * @param y The world Y coordinate.
	 * @param z The world Z coordinate.
	 * @return {@code true} once spawned, {@code false} when the position is out of the world.
	 */
	public boolean spawnMe(int x, int y, int z)
	{
		synchronized (this)
		{
			int spawnX = x;
			if (spawnX > World.WORLD_X_MAX)
			{
				spawnX = World.WORLD_X_MAX - 5000;
			}
			
			if (spawnX < World.WORLD_X_MIN)
			{
				spawnX = World.WORLD_X_MIN + 5000;
			}
			
			int spawnY = y;
			if (spawnY > World.WORLD_Y_MAX)
			{
				spawnY = World.WORLD_Y_MAX - 5000;
			}
			
			if (spawnY < World.WORLD_Y_MIN)
			{
				spawnY = World.WORLD_Y_MIN + 5000;
			}
			
			// Set the x,y,z position of the WorldObject. If flagged with _isSpawned, setXYZ will automatically update world region, so avoid that.
			setXYZ(spawnX, spawnY, z);
		}
		
		// Spawn and update its _worldregion.
		return spawnMe();
	}
	
	/**
	 * Indicates whether this object can be attacked.
	 * @return {@code true} if it can be attacked, {@code false} otherwise.
	 */
	public boolean canBeAttacked()
	{
		return false;
	}
	
	public abstract boolean isAutoAttackable(Creature attacker);
	
	/**
	 * Indicates whether this object is currently spawned in the world.
	 * @return {@code true} if spawned, {@code false} otherwise.
	 */
	public boolean isSpawned()
	{
		return _isSpawned;
	}
	
	/**
	 * Sets whether this object is spawned in the world.
	 * @param value The spawned state to apply.
	 */
	public void setSpawned(boolean value)
	{
		_isSpawned = value;
	}
	
	/**
	 * Retrieves this object's name.
	 * @return The name, or {@code null} if unset.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Sets this object's name.
	 * @param value The name to assign.
	 */
	public void setName(String value)
	{
		_name = value;
	}
	
	/**
	 * Retrieves this object's template id; {@code 0} for a plain {@code WorldObject}.
	 * @return The template id as an {@code int}.
	 */
	public int getId()
	{
		return 0;
	}
	
	/**
	 * Retrieves this object's unique object id.
	 * @return The object id as an {@code int}.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	public abstract void sendInfo(Player player);
	
	/**
	 * Sends a {@code ServerPacket} to this object; no-op unless overridden.
	 * @param packet The {@code ServerPacket} to send.
	 */
	public void sendPacket(ServerPacket packet)
	{
	}
	
	/**
	 * Sends a system message to this object; no-op unless overridden.
	 * @param id The {@code SystemMessageId} of the message.
	 */
	public void sendPacket(SystemMessageId id)
	{
	}
	
	/**
	 * Returns this object as an {@code Attackable}, or {@code null} if it is not one.
	 * @return This object as an {@code Attackable}, or {@code null}.
	 */
	public Attackable asAttackable()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Creature}, or {@code null} if it is not one.
	 * @return This object as a {@code Creature}, or {@code null}.
	 */
	public Creature asCreature()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Door}, or {@code null} if it is not one.
	 * @return This object as a {@code Door}, or {@code null}.
	 */
	public Door asDoor()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Monster}, or {@code null} if it is not one.
	 * @return This object as a {@code Monster}, or {@code null}.
	 */
	public Monster asMonster()
	{
		return null;
	}
	
	/**
	 * Returns this object as an {@code Npc}, or {@code null} if it is not one.
	 * @return This object as an {@code Npc}, or {@code null}.
	 */
	public Npc asNpc()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Pet}, or {@code null} if it is not one.
	 * @return This object as a {@code Pet}, or {@code null}.
	 */
	public Pet asPet()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Playable}, or {@code null} if it is not one.
	 * @return This object as a {@code Playable}, or {@code null}.
	 */
	public Playable asPlayable()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Player}, or {@code null} if it is not one.
	 * @return This object as a {@code Player}, or {@code null}.
	 */
	public Player asPlayer()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Servitor}, or {@code null} if it is not one.
	 * @return This object as a {@code Servitor}, or {@code null}.
	 */
	public Servitor asServitor()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Summon}, or {@code null} if it is not one.
	 * @return This object as a {@code Summon}, or {@code null}.
	 */
	public Summon asSummon()
	{
		return null;
	}
	
	/**
	 * Indicates whether this object is an artefact.
	 * @return {@code true} if this object is an artefact, {@code false} otherwise.
	 */
	public boolean isArtefact()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is attackable.
	 * @return {@code true} if this object is attackable, {@code false} otherwise.
	 */
	public boolean isAttackable()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a {@code Creature}.
	 * @return {@code true} if this object is a {@code Creature}, {@code false} otherwise.
	 */
	public boolean isCreature()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a cubic.
	 * @return {@code true} if this object is a cubic, {@code false} otherwise.
	 */
	public boolean isCubic()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a {@code Door}.
	 * @return {@code true} if this object is a {@code Door}, {@code false} otherwise.
	 */
	public boolean isDoor()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a fake player.
	 * @return {@code true} if this object is a fake player, {@code false} otherwise.
	 */
	public boolean isFakePlayer()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a {@code Fence}.
	 * @return {@code true} if this object is a {@code Fence}, {@code false} otherwise.
	 */
	public boolean isFence()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is an item.
	 * @return {@code true} if this object is an item, {@code false} otherwise.
	 */
	public boolean isItem()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a {@code Monster}.
	 * @return {@code true} if this object is a {@code Monster}, {@code false} otherwise.
	 */
	public boolean isMonster()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is an {@code Npc}.
	 * @return {@code true} if this object is an {@code Npc}, {@code false} otherwise.
	 */
	public boolean isNpc()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a {@code Pet}.
	 * @return {@code true} if this object is a {@code Pet}, {@code false} otherwise.
	 */
	public boolean isPet()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a {@code Playable}.
	 * @return {@code true} if this object is a {@code Playable}, {@code false} otherwise.
	 */
	public boolean isPlayable()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a {@code Player}.
	 * @return {@code true} if this object is a {@code Player}, {@code false} otherwise.
	 */
	public boolean isPlayer()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a {@code Servitor}.
	 * @return {@code true} if this object is a {@code Servitor}, {@code false} otherwise.
	 */
	public boolean isServitor()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a {@code Summon}.
	 * @return {@code true} if this object is a {@code Summon}, {@code false} otherwise.
	 */
	public boolean isSummon()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a trap.
	 * @return {@code true} if this object is a trap, {@code false} otherwise.
	 */
	public boolean isTrap()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a vehicle.
	 * @return {@code true} if this object is a vehicle, {@code false} otherwise.
	 */
	public boolean isVehicle()
	{
		return false;
	}
	
	/**
	 * Indicates whether this object is a walker.
	 * @return {@code true} if this object is a walker, {@code false} otherwise.
	 */
	public boolean isWalker()
	{
		return false;
	}
	
	/**
	 * Sets whether this object can be targeted; clearing it makes nearby creatures drop it as a target.
	 * @param targetable The targetable state to apply.
	 */
	public void setTargetable(boolean targetable)
	{
		if (_isTargetable != targetable)
		{
			_isTargetable = targetable;
			if (!targetable)
			{
				World.forEachVisibleObject(this, Creature.class, creature ->
				{
					if (creature.getTarget() == this)
					{
						creature.setTarget(null);
						creature.abortAttack();
						creature.abortCast();
					}
				});
			}
		}
	}
	
	/**
	 * Indicates whether this object can be targeted.
	 * @return {@code true} if targetable, {@code false} otherwise.
	 */
	public boolean isTargetable()
	{
		return _isTargetable;
	}
	
	/**
	 * Indicates whether this object is inside the given zone.
	 * @param zone The {@code ZoneId} to test.
	 * @return {@code true} if inside the zone, {@code false} otherwise.
	 */
	public boolean isInsideZone(ZoneId zone)
	{
		return false;
	}
	
	/**
	 * Stores a script on this object, keyed by its class name.
	 * @param <T> The script type.
	 * @param script The script to add.
	 * @return The added script.
	 */
	public <T> T addScript(T script)
	{
		if (_scripts == null)
		{
			// Double-checked locking
			synchronized (this)
			{
				if (_scripts == null)
				{
					_scripts = new ConcurrentHashMap<>();
				}
			}
		}
		
		_scripts.put(script.getClass().getName(), script);
		return script;
	}
	
	/**
	 * Removes the script of the given type from this object.
	 * @param <T> The script type.
	 * @param script The script class to remove.
	 * @return The removed script, or {@code null} if absent.
	 */
	@SuppressWarnings("unchecked")
	public <T> T removeScript(Class<T> script)
	{
		if (_scripts == null)
		{
			return null;
		}
		
		return (T) _scripts.remove(script.getName());
	}
	
	/**
	 * Retrieves the script of the given type from this object.
	 * @param <T> The script type.
	 * @param script The script class to retrieve.
	 * @return The script, or {@code null} if absent.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getScript(Class<T> script)
	{
		if (_scripts == null)
		{
			return null;
		}
		
		return (T) _scripts.get(script.getName());
	}
	
	/**
	 * Removes a status listener; no-op unless overridden.
	 * @param object The {@code Creature} to remove.
	 */
	public void removeStatusListener(Creature object)
	{
	}
	
	/**
	 * Moves this object to the given coordinates clamped to the world bounds, and clears its spawned state.
	 * @param x The world X coordinate.
	 * @param y The world Y coordinate.
	 * @param z The world Z coordinate.
	 */
	public void setXYZInvisible(int x, int y, int z)
	{
		int correctX = x;
		if (correctX > World.WORLD_X_MAX)
		{
			correctX = World.WORLD_X_MAX - 5000;
		}
		
		if (correctX < World.WORLD_X_MIN)
		{
			correctX = World.WORLD_X_MIN + 5000;
		}
		
		int correctY = y;
		if (correctY > World.WORLD_Y_MAX)
		{
			correctY = World.WORLD_Y_MAX - 5000;
		}
		
		if (correctY < World.WORLD_Y_MIN)
		{
			correctY = World.WORLD_Y_MIN + 5000;
		}
		
		setXYZ(correctX, correctY, z);
		setSpawned(false);
	}
	
	/**
	 * Moves this object to the given location clamped to the world bounds, and clears its spawned state.
	 * @param loc The target {@code ILocational}.
	 */
	public void setLocationInvisible(ILocational loc)
	{
		setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Retrieves this object's {@code WorldRegion}.
	 * @return The {@code WorldRegion}, or {@code null} if none.
	 */
	public WorldRegion getWorldRegion()
	{
		return _worldRegion;
	}
	
	/**
	 * Sets this object's {@code WorldRegion}, removing it from its current region when set to {@code null}.
	 * @param region The {@code WorldRegion} to assign.
	 */
	public void setWorldRegion(WorldRegion region)
	{
		if ((region == null) && (_worldRegion != null))
		{
			_worldRegion.removeVisibleObject(this);
		}
		
		_worldRegion = region;
	}
	
	/**
	 * Retrieves this object's X coordinate.
	 * @return The X coordinate as an {@code int}.
	 */
	@Override
	public int getX()
	{
		return _location.getX();
	}
	
	/**
	 * Retrieves this object's Y coordinate.
	 * @return The Y coordinate as an {@code int}.
	 */
	@Override
	public int getY()
	{
		return _location.getY();
	}
	
	/**
	 * Retrieves this object's Z coordinate.
	 * @return The Z coordinate as an {@code int}.
	 */
	@Override
	public int getZ()
	{
		return _location.getZ();
	}
	
	/**
	 * Retrieves this object's heading.
	 * @return The heading as an {@code int}.
	 */
	@Override
	public int getHeading()
	{
		return _location.getHeading();
	}
	
	/**
	 * Retrieves the id of this object's {@code Instance}; {@code 0} if it is not in one.
	 * @return The instance id as an {@code int}.
	 */
	public int getInstanceId()
	{
		final Instance instance = _instance;
		return (instance != null) ? instance.getId() : 0;
	}
	
	/**
	 * Indicates whether this object is inside an {@code Instance}.
	 * @return {@code true} if in an instance, {@code false} otherwise.
	 */
	public boolean isInInstance()
	{
		return _instance != null;
	}
	
	/**
	 * Retrieves the {@code Instance} this object belongs to.
	 * @return The {@code Instance}, or {@code null} if none.
	 */
	public Instance getInstanceWorld()
	{
		return _instance;
	}
	
	/**
	 * Retrieves this object's {@code Location}.
	 * @return The {@code Location}.
	 */
	@Override
	public Location getLocation()
	{
		return _location;
	}
	
	/**
	 * Sets this object's coordinates, switching world region when it is spawned.
	 * @param newX The world X coordinate.
	 * @param newY The world Y coordinate.
	 * @param newZ The world Z coordinate.
	 */
	@Override
	public void setXYZ(int newX, int newY, int newZ)
	{
		_location.setXYZ(newX, newY, newZ);
		
		if (_isSpawned)
		{
			final WorldRegion newRegion = World.getRegion(this);
			if ((newRegion != null) && (newRegion != _worldRegion))
			{
				if (_worldRegion != null)
				{
					_worldRegion.removeVisibleObject(this);
				}
				
				newRegion.addVisibleObject(this);
				World.switchRegion(this, newRegion);
				setWorldRegion(newRegion);
				
				// Doors and fences are known per region, so a move aimed through one is caught upon reaching its region.
				if (isCreature() && (!newRegion.getDoors().isEmpty() || !newRegion.getFences().isEmpty()))
				{
					asCreature().revalidateMoveDestination();
				}
			}
		}
	}
	
	/**
	 * Sets this object's coordinates from the given location.
	 * @param loc The source {@code ILocational}.
	 */
	@Override
	public void setXYZ(ILocational loc)
	{
		setXYZ(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Sets this object's heading.
	 * @param newHeading The heading to assign.
	 */
	@Override
	public void setHeading(int newHeading)
	{
		_location.setHeading(newHeading);
	}
	
	/**
	 * Sets this object's {@code Instance} by id, ignoring non-zero ids that resolve to no instance.
	 * @param id The instance id, or {@code 0} for none.
	 */
	public void setInstanceById(int id)
	{
		final Instance instance = InstanceManager.getInstance().getInstance(id);
		if ((id != 0) && (instance == null))
		{
			return;
		}
		
		setInstance(instance);
	}
	
	/**
	 * Moves this object into the given {@code Instance}, notifying the old and new instances of the change.
	 * @param newInstance The {@code Instance} to enter, or {@code null} for none.
	 */
	public synchronized void setInstance(Instance newInstance)
	{
		// Check if new and old instances are identical.
		if (_instance == newInstance)
		{
			return;
		}
		
		// Leave old instance.
		if (_instance != null)
		{
			_instance.onInstanceChange(this, false);
		}
		
		// Set new instance.
		_instance = newInstance;
		
		// Enter into new instance.
		if (newInstance != null)
		{
			newInstance.onInstanceChange(this, true);
		}
	}
	
	/**
	 * Sets this object's position and heading from the given {@code Location}.
	 * @param loc The source {@code Location}.
	 */
	@Override
	public void setLocation(Location loc)
	{
		_location.setXYZ(loc.getX(), loc.getY(), loc.getZ());
		_location.setHeading(loc.getHeading());
	}
	
	/**
	 * Calculates the 2D distance from this object to the given coordinate.
	 * @param x The target X coordinate.
	 * @param y The target Y coordinate.
	 * @param z The target Z coordinate (ignored).
	 * @return The 2D distance as a {@code double}.
	 */
	public double calculateDistance2D(int x, int y, int z)
	{
		final double dx = x - getX();
		final double dy = y - getY();
		return Math.sqrt((dx * dx) + (dy * dy));
	}
	
	/**
	 * Calculates the 2D distance from this object to the given location.
	 * @param loc The target {@code ILocational}.
	 * @return The 2D distance, or {@code Double.MAX_VALUE} if {@code loc} is {@code null}.
	 */
	public double calculateDistance2D(ILocational loc)
	{
		if (loc == null)
		{
			return Double.MAX_VALUE;
		}
		
		return calculateDistance2D(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Calculates the 3D distance from this object to the given coordinate.
	 * @param x The target X coordinate.
	 * @param y The target Y coordinate.
	 * @param z The target Z coordinate.
	 * @return The 3D distance as a {@code double}.
	 */
	public double calculateDistance3D(int x, int y, int z)
	{
		final double dx = x - getX();
		final double dy = y - getY();
		final double dz = z - getZ();
		return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
	}
	
	/**
	 * Calculates the 3D distance from this object to the given location.
	 * @param loc The target {@code ILocational}.
	 * @return The 3D distance, or {@code Double.MAX_VALUE} if {@code loc} is {@code null}.
	 */
	public double calculateDistance3D(ILocational loc)
	{
		if (loc == null)
		{
			return Double.MAX_VALUE;
		}
		
		return calculateDistance3D(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Calculates the heading angle from this object toward the given location.
	 * @param target The target {@code ILocational}.
	 * @return The angle as a {@code double}.
	 */
	public double calculateDirectionTo(ILocational target)
	{
		return LocationUtil.calculateAngleFrom(this, target);
	}
	
	/**
	 * Indicates whether this object is invisible.
	 * @return {@code true} if invisible, {@code false} otherwise.
	 */
	public boolean isInvisible()
	{
		return _isInvisible;
	}
	
	/**
	 * Sets this object's invisibility, hiding it from players who cannot see it and broadcasting its state.
	 * @param invisible The invisibility state to apply.
	 */
	public void setInvisible(boolean invisible)
	{
		_isInvisible = invisible;
		
		if (invisible)
		{
			final DeleteObject deletePacket = new DeleteObject(this);
			World.forEachVisibleObject(this, Player.class, player ->
			{
				if (!isVisibleFor(player))
				{
					player.sendPacket(deletePacket);
				}
			});
		}
		
		// Broadcast information regarding the object to those which are suppose to see.
		broadcastInfo();
	}
	
	/**
	 * Indicates whether the given player can see this object; always visible to a GM.
	 * @param player The observing {@code Player}.
	 * @return {@code true} if visible to the player, {@code false} otherwise.
	 */
	public boolean isVisibleFor(Player player)
	{
		return !_isInvisible || player.isGM();
	}
	
	/**
	 * Sends this object's info to every nearby player that can see it.
	 */
	public void broadcastInfo()
	{
		World.forEachVisibleObject(this, Player.class, player ->
		{
			if (isVisibleFor(player))
			{
				sendInfo(player);
			}
		});
	}
	
	/**
	 * Indicates whether this object is invulnerable.
	 * @return {@code true} if invulnerable, {@code false} otherwise.
	 */
	public boolean isInvul()
	{
		return false;
	}
	
	/**
	 * Indicates whether the given object's region surrounds this object's region.
	 * @param worldObject The {@code WorldObject} to test.
	 * @return {@code true} if its region is surrounding, {@code false} otherwise or if either region is {@code null}.
	 */
	public boolean isInSurroundingRegion(WorldObject worldObject)
	{
		if (worldObject == null)
		{
			return false;
		}
		
		final WorldRegion worldRegion = worldObject.getWorldRegion();
		if (worldRegion == null)
		{
			return false;
		}
		
		if (_worldRegion == null)
		{
			return false;
		}
		
		return worldRegion.isSurroundingRegion(_worldRegion);
	}
	
	/**
	 * Queues a trigger cast, starting processing if none is in progress.
	 * @param info The {@code TriggerCastInfo} to queue.
	 */
	public void addTriggerCast(TriggerCastInfo info)
	{
		_triggerCastQueue.offer(info);
		if (_triggerCasting.compareAndSet(false, true))
		{
			processTriggerCastQueue();
		}
	}
	
	/**
	 * Drains the trigger-cast queue, executing each queued cast in order.
	 */
	private void processTriggerCastQueue()
	{
		try
		{
			while (!_triggerCastQueue.isEmpty())
			{
				final TriggerCastInfo info = _triggerCastQueue.poll();
				if (info != null)
				{
					SkillCaster.triggerCast(info);
				}
			}
		}
		finally
		{
			_triggerCasting.set(false);
			if (!_triggerCastQueue.isEmpty() && _triggerCasting.compareAndSet(false, true))
			{
				processTriggerCastQueue();
			}
		}
	}
	
	/**
	 * Compares this object with another for equal object id.
	 * @param obj The object to compare against.
	 * @return {@code true} if both are {@code WorldObject}s with the same object id, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof WorldObject) && (((WorldObject) obj).getObjectId() == getObjectId());
	}
	
	/**
	 * Returns this object as {@code ClassName:name[objectId]}.
	 * @return A string representation of this object.
	 */
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(':');
		sb.append(_name);
		sb.append('[');
		sb.append(_objectId);
		sb.append(']');
		return sb.toString();
	}
}
