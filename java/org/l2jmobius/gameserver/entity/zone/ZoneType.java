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
package org.l2jmobius.gameserver.entity.zone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureZoneEnter;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureZoneExit;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * Abstract base for all zone types, handling membership, affect-checks and enter/exit events.
 * @author durgus
 */
public abstract class ZoneType extends ListenersContainer
{
	public static final Logger LOGGER = Logger.getLogger(ZoneType.class.getName());
	
	private final int _id;
	protected ZoneForm _zone;
	protected List<ZoneForm> _blockedZones;
	private final Map<Integer, Creature> _characterList = new ConcurrentHashMap<>();
	
	/** Parameters to affect specific characters */
	private boolean _checkAffected = false;
	private String _name = null;
	private int _minLevel;
	private int _maxLevel;
	private int[] _race;
	private int[] _class;
	private char _classType;
	private InstanceType _target = InstanceType.Creature; // Default all chars.
	private boolean _allowStore;
	private boolean _enabled;
	private AbstractZoneSettings _settings;
	private int _instanceTemplateId;
	private Map<Integer, Boolean> _enabledInInstance;
	
	/**
	 * Creates a {@code ZoneType} with the given id and default level, race and class limits.
	 * @param id The unique zone id.
	 */
	protected ZoneType(int id)
	{
		_id = id;
		_minLevel = 0;
		_maxLevel = 0xFF;
		_classType = 0;
		_race = null;
		_class = null;
		_allowStore = true;
		_enabled = true;
	}
	
	/**
	 * Retrieves this zone's id.
	 * @return The zone id as an {@code int}.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Applies a named configuration parameter parsed from the zone definition, enabling affect-checking.
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public void setParameter(String name, String value)
	{
		_checkAffected = true;
		
		// Zone name
		if (name.equals("name"))
		{
			_name = value;
		}
		// Minimum level
		else if (name.equals("affectedLvlMin"))
		{
			_minLevel = Integer.parseInt(value);
		}
		// Maximum level
		else if (name.equals("affectedLvlMax"))
		{
			_maxLevel = Integer.parseInt(value);
		}
		// Affected Races
		else if (name.equals("affectedRace"))
		{
			// Create a new array holding the affected race.
			if (_race == null)
			{
				_race = new int[1];
				_race[0] = Integer.parseInt(value);
			}
			else
			{
				final int[] temp = new int[_race.length + 1];
				int i = 0;
				for (; i < _race.length; i++)
				{
					temp[i] = _race[i];
				}
				
				temp[i] = Integer.parseInt(value);
				_race = temp;
			}
		}
		// Affected classes
		else if (name.equals("affectedClassId"))
		{
			// Create a new array holding the affected classIds.
			if (_class == null)
			{
				_class = new int[1];
				_class[0] = Integer.parseInt(value);
			}
			else
			{
				final int[] temp = new int[_class.length + 1];
				int i = 0;
				for (; i < _class.length; i++)
				{
					temp[i] = _class[i];
				}
				
				temp[i] = Integer.parseInt(value);
				_class = temp;
			}
		}
		// Affected class type
		else if (name.equals("affectedClassType"))
		{
			if (value.equals("Fighter"))
			{
				_classType = 1;
			}
			else
			{
				_classType = 2;
			}
		}
		else if (name.equals("targetClass"))
		{
			_target = Enum.valueOf(InstanceType.class, value);
		}
		else if (name.equals("allowStore"))
		{
			_allowStore = Boolean.parseBoolean(value);
		}
		else if (name.equals("default_enabled"))
		{
			_enabled = Boolean.parseBoolean(value);
		}
		else if (name.equals("instanceId"))
		{
			_instanceTemplateId = Integer.parseInt(value);
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": Unknown parameter - " + name + " in zone: " + _id);
		}
	}
	
	/**
	 * Checks whether the given creature meets this zone's instance, level, type, race and class requirements.
	 * @param creature The {@code Creature} to test.
	 * @return {@code true} if the creature is affected, {@code false} otherwise.
	 */
	private boolean isAffected(Creature creature)
	{
		// Check instance
		final Instance world = creature.getInstanceWorld();
		if (world != null)
		{
			if (world.getTemplateId() != _instanceTemplateId)
			{
				return false;
			}
			
			if (!isEnabled(creature.getInstanceId()))
			{
				return false;
			}
		}
		else if (_instanceTemplateId > 0)
		{
			return false;
		}
		else if (!isEnabled())
		{
			return false;
		}
		
		// Check level
		final int level = creature.getLevel();
		if ((level < _minLevel) || (level > _maxLevel))
		{
			if (creature.isPlayer())
			{
				creature.asPlayer().sendPacket(new ExShowScreenMessage(SystemMessageId.YOU_CANNOT_ENTER_AS_YOUR_LEVEL_DOES_NOT_MEET_THE_REQUIREMENTS, ExShowScreenMessage.TOP_CENTER, 10000));
			}
			
			return false;
		}
		
		// Check obj class.
		if (!creature.isInstanceTypes(_target))
		{
			return false;
		}
		
		if (creature.isPlayer())
		{
			// Check class type.
			if (_classType != 0)
			{
				if (creature.asPlayer().isMageClass())
				{
					if (_classType == 1)
					{
						return false;
					}
				}
				else if (_classType == 2)
				{
					return false;
				}
			}
			
			// Check race
			if (_race != null)
			{
				boolean ok = false;
				for (int element : _race)
				{
					if (creature.getRace().ordinal() == element)
					{
						ok = true;
						break;
					}
				}
				
				if (!ok)
				{
					return false;
				}
			}
			
			// Check class
			if (_class != null)
			{
				boolean ok = false;
				for (int _clas : _class)
				{
					if (creature.asPlayer().getPlayerClass().getId() == _clas)
					{
						ok = true;
						break;
					}
				}
				
				if (!ok)
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Sets this zone's {@code ZoneForm}, throwing if one is already set.
	 * @param zone The {@code ZoneForm} to assign.
	 */
	public void setZone(ZoneForm zone)
	{
		if (_zone != null)
		{
			throw new IllegalStateException("Zone already set");
		}
		
		_zone = zone;
	}
	
	/**
	 * Retrieves this zone's {@code ZoneForm}.
	 * @return The {@code ZoneForm}.
	 */
	public ZoneForm getZone()
	{
		return _zone;
	}
	
	/**
	 * Sets this zone's blocked {@code ZoneForm}s, throwing if they are already set.
	 * @param blockedZones The blocked {@code ZoneForm}s to assign.
	 */
	public void setBlockedZones(List<ZoneForm> blockedZones)
	{
		if (_blockedZones != null)
		{
			throw new IllegalStateException("Blocked zone already set");
		}
		
		_blockedZones = blockedZones;
	}
	
	/**
	 * Retrieves this zone's blocked {@code ZoneForm}s.
	 * @return The blocked {@code ZoneForm}s, or {@code null} if none.
	 */
	public List<ZoneForm> getBlockedZones()
	{
		return _blockedZones;
	}
	
	/**
	 * Sets this zone's name.
	 * @param name The name to assign.
	 */
	public void setName(String name)
	{
		_name = name;
	}
	
	/**
	 * Retrieves this zone's name.
	 * @return The name, or {@code null} if unset.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Checks whether the given coordinate is inside this zone and not in a blocked zone.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @param z The Z coordinate.
	 * @return {@code true} if inside the zone, {@code false} otherwise.
	 */
	public boolean isInsideZone(int x, int y, int z)
	{
		return (_zone != null) && _zone.isInsideZone(x, y, z) && !isInsideBlockedZone(x, y, z);
	}
	
	/**
	 * Checks whether the given coordinate is inside any blocked zone.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @param z The Z coordinate.
	 * @return {@code true} if inside a blocked zone, {@code false} otherwise.
	 */
	public boolean isInsideBlockedZone(int x, int y, int z)
	{
		if ((_blockedZones == null) || _blockedZones.isEmpty())
		{
			return false;
		}
		
		for (ZoneForm zone : _blockedZones)
		{
			if (zone.isInsideZone(x, y, z))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks whether the given coordinate is inside this zone, using the zone's high Z.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @return {@code true} if inside the zone, {@code false} otherwise.
	 */
	public boolean isInsideZone(int x, int y)
	{
		return isInsideZone(x, y, _zone.getHighZ());
	}
	
	/**
	 * Checks whether the given location is inside this zone.
	 * @param loc The {@code ILocational} to test.
	 * @return {@code true} if inside the zone, {@code false} otherwise.
	 */
	public boolean isInsideZone(ILocational loc)
	{
		return isInsideZone(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Checks whether the given object is inside this zone.
	 * @param object The {@code WorldObject} to test.
	 * @return {@code true} if inside the zone, {@code false} otherwise.
	 */
	public boolean isInsideZone(WorldObject object)
	{
		return isInsideZone(object.getX(), object.getY(), object.getZ());
	}
	
	/**
	 * Calculates the distance from the given coordinate to this zone.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @return The distance as a {@code double}.
	 */
	public double getDistanceToZone(int x, int y)
	{
		return _zone.getDistanceToZone(x, y);
	}
	
	/**
	 * Calculates the distance from the given object to this zone.
	 * @param object The {@code WorldObject} to measure from.
	 * @return The distance as a {@code double}.
	 */
	public double getDistanceToZone(WorldObject object)
	{
		return _zone.getDistanceToZone(object.getX(), object.getY());
	}
	
	/**
	 * Re-evaluates the creature's membership, firing enter events on entry or removing it on exit.
	 * @param creature The {@code Creature} to revalidate.
	 */
	public void revalidateInZone(Creature creature)
	{
		// If the object is inside the zone...
		if (isInsideZone(creature))
		{
			// If the character can't be affected by this zone return.
			if (_checkAffected && !isAffected(creature))
			{
				return;
			}
			
			if (_characterList.putIfAbsent(creature.getObjectId(), creature) == null)
			{
				// Notify to scripts.
				if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_ZONE_ENTER, this))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnCreatureZoneEnter(creature, this), this);
				}
				
				// Notify Zone implementation.
				onEnter(creature);
			}
		}
		else
		{
			removeCharacter(creature);
		}
	}
	
	/**
	 * Removes the creature from this zone, firing an exit event if it was a member.
	 * @param creature The {@code Creature} to remove.
	 */
	public void removeCharacter(Creature creature)
	{
		// Was the character inside this zone?
		final int objectId = creature.getObjectId();
		if (_characterList.containsKey(objectId))
		{
			// Notify to scripts.
			if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_ZONE_EXIT, this))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnCreatureZoneExit(creature, this), this);
			}
			
			// Unregister player.
			_characterList.remove(objectId);
			
			// Notify Zone implementation.
			onExit(creature);
		}
	}
	
	/**
	 * Checks whether the given creature is currently a member of this zone.
	 * @param creature The {@code Creature} to test.
	 * @return {@code true} if in the zone, {@code false} otherwise.
	 */
	public boolean isCharacterInZone(Creature creature)
	{
		return _characterList.containsKey(creature.getObjectId());
	}
	
	/**
	 * Retrieves this zone's {@code AbstractZoneSettings}.
	 * @return The {@code AbstractZoneSettings}.
	 */
	public AbstractZoneSettings getSettings()
	{
		return _settings;
	}
	
	/**
	 * Sets this zone's {@code AbstractZoneSettings}, clearing any previous settings.
	 * @param settings The {@code AbstractZoneSettings} to apply.
	 */
	public void setSettings(AbstractZoneSettings settings)
	{
		if (_settings != null)
		{
			_settings.clear();
		}
		
		_settings = settings;
	}
	
	protected abstract void onEnter(Creature creature);
	
	protected abstract void onExit(Creature creature);
	
	/**
	 * Called when a creature dies inside this zone; no-op unless overridden.
	 * @param creature The {@code Creature} that died.
	 */
	public void onDieInside(Creature creature)
	{
	}
	
	/**
	 * Called when a creature revives inside this zone; no-op unless overridden.
	 * @param creature The {@code Creature} that revived.
	 */
	public void onReviveInside(Creature creature)
	{
	}
	
	/**
	 * Called when a player logs in inside this zone; no-op unless overridden.
	 * @param player The {@code Player} that logged in.
	 */
	public void onPlayerLoginInside(Player player)
	{
	}
	
	/**
	 * Called when a player logs out inside this zone; no-op unless overridden.
	 * @param player The {@code Player} that logged out.
	 */
	public void onPlayerLogoutInside(Player player)
	{
	}
	
	/**
	 * Retrieves the members of this zone keyed by object id.
	 * @return A {@code Map} of object id to {@code Creature}.
	 */
	public Map<Integer, Creature> getCharacters()
	{
		return _characterList;
	}
	
	/**
	 * Retrieves the creatures currently inside this zone.
	 * @return A {@code Collection} of {@code Creature}.
	 */
	public Collection<Creature> getCharactersInside()
	{
		return _characterList.values();
	}
	
	/**
	 * Retrieves the players currently inside this zone.
	 * @return A {@code List} of {@code Player}.
	 */
	public List<Player> getPlayersInside()
	{
		final List<Player> players = new ArrayList<>();
		for (Creature ch : _characterList.values())
		{
			if ((ch != null) && ch.isPlayer())
			{
				players.add(ch.asPlayer());
			}
		}
		
		return players;
	}
	
	/**
	 * Sends a packet to every player inside this zone.
	 * @param packet The {@code ServerPacket} to send.
	 */
	public void broadcastPacket(ServerPacket packet)
	{
		if (_characterList.isEmpty())
		{
			return;
		}
		
		for (Creature creature : _characterList.values())
		{
			if ((creature != null) && creature.isPlayer())
			{
				creature.sendPacket(packet);
			}
		}
	}
	
	/**
	 * Retrieves the {@code InstanceType} this zone affects.
	 * @return The target {@code InstanceType}.
	 */
	public InstanceType getTargetType()
	{
		return _target;
	}
	
	/**
	 * Sets the {@code InstanceType} this zone affects, enabling affect-checking.
	 * @param type The target {@code InstanceType}.
	 */
	public void setTargetType(InstanceType type)
	{
		_target = type;
		_checkAffected = true;
	}
	
	/**
	 * Indicates whether private stores are allowed in this zone.
	 * @return {@code true} if storing is allowed, {@code false} otherwise.
	 */
	public boolean getAllowStore()
	{
		return _allowStore;
	}
	
	/**
	 * Retrieves the instance template id this zone is bound to.
	 * @return The instance template id as an {@code int}.
	 */
	public int getInstanceTemplateId()
	{
		return _instanceTemplateId;
	}
	
	/**
	 * Returns this zone as {@code ClassName[id]}.
	 * @return A string representation of this zone.
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + _id + "]";
	}
	
	/**
	 * Visualizes this zone's form at the given height.
	 * @param z The height to visualize at.
	 */
	public void visualizeZone(int z)
	{
		_zone.visualizeZone(z);
	}
	
	/**
	 * Sets whether this zone is enabled.
	 * @param value The enabled state to apply.
	 */
	public void setEnabled(boolean value)
	{
		_enabled = value;
	}
	
	/**
	 * Indicates whether this zone is enabled.
	 * @return {@code true} if enabled, {@code false} otherwise.
	 */
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	/**
	 * Sets whether this zone is enabled within the given instance.
	 * @param state The enabled state to apply.
	 * @param instanceId The instance id.
	 */
	public void setEnabled(boolean state, int instanceId)
	{
		if (_enabledInInstance == null)
		{
			synchronized (this)
			{
				if (_enabledInInstance == null)
				{
					_enabledInInstance = new ConcurrentHashMap<>();
				}
			}
		}
		
		_enabledInInstance.put(instanceId, state);
	}
	
	/**
	 * Indicates whether this zone is enabled within the given instance, falling back to the default state.
	 * @param instanceId The instance id.
	 * @return {@code true} if enabled, {@code false} otherwise.
	 */
	public boolean isEnabled(int instanceId)
	{
		if (_enabledInInstance != null)
		{
			return _enabledInInstance.getOrDefault(instanceId, _enabled);
		}
		
		return _enabled;
	}
	
	/**
	 * Teleports every online player in this zone to the nearest town.
	 */
	public void oustAllPlayers()
	{
		for (Creature obj : _characterList.values())
		{
			if ((obj != null) && obj.isPlayer())
			{
				final Player player = obj.asPlayer();
				if (player.isOnline())
				{
					player.teleToLocation(TeleportWhereType.TOWN);
				}
			}
		}
	}
	
	/**
	 * Teleports every online player in this zone to the given location.
	 * @param loc The destination {@code Location}.
	 */
	public void movePlayersTo(Location loc)
	{
		if (_characterList.isEmpty())
		{
			return;
		}
		
		for (Creature creature : _characterList.values())
		{
			if ((creature != null) && creature.isPlayer())
			{
				final Player player = creature.asPlayer();
				if (player.isOnline())
				{
					player.teleToLocation(loc);
				}
			}
		}
	}
}
