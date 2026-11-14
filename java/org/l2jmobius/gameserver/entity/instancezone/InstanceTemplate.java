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
package org.l2jmobius.gameserver.entity.instancezone;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.holders.InstanceReenterTimeHolder;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.GroupType;
import org.l2jmobius.gameserver.entity.actor.templates.DoorTemplate;
import org.l2jmobius.gameserver.entity.groups.AbstractPlayerGroup;
import org.l2jmobius.gameserver.entity.instancezone.conditions.Condition;
import org.l2jmobius.gameserver.entity.instancezone.conditions.ConditionCommandChannel;
import org.l2jmobius.gameserver.entity.instancezone.conditions.ConditionGroupMax;
import org.l2jmobius.gameserver.entity.instancezone.conditions.ConditionGroupMin;
import org.l2jmobius.gameserver.entity.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class serves as a template holder for instance zones.<br>
 * It stores the configuration and static data required to initialize an instance.
 * @author malyelfik
 */
public class InstanceTemplate extends ListenersContainer
{
	// Basic instance parameters
	private int _templateId = -1;
	private String _name = "UnknownInstance";
	private int _duration = -1;
	private long _emptyDestroyTime = -1;
	private int _ejectTime = GeneralConfig.EJECT_DEAD_PLAYER_TIME;
	private int _maxWorldCount = -1;
	private boolean _isPvP = false;
	private boolean _allowPlayerSummon = false;
	private float _expRate = RatesConfig.RATE_INSTANCE_XP;
	private float _spRate = RatesConfig.RATE_INSTANCE_SP;
	private float _expPartyRate = RatesConfig.RATE_INSTANCE_PARTY_XP;
	private float _spPartyRate = RatesConfig.RATE_INSTANCE_PARTY_SP;
	private StatSet _parameters = StatSet.EMPTY_STATSET;
	private final Map<Integer, DoorTemplate> _doors = new HashMap<>();
	private final List<SpawnTemplate> _spawns = new ArrayList<>();
	
	// Locations
	private InstanceTeleportType _enterLocationType = InstanceTeleportType.NONE;
	private List<Location> _enterLocations = null;
	private InstanceTeleportType _exitLocationType = InstanceTeleportType.NONE;
	private List<Location> _exitLocations = null;
	
	// Reenter data
	private InstanceReenterType _reenterType = InstanceReenterType.NONE;
	private List<InstanceReenterTimeHolder> _reenterData = Collections.emptyList();
	
	// Buff remove data
	private InstanceRemoveBuffType _removeBuffType = InstanceRemoveBuffType.NONE;
	private List<Integer> _removeBuffExceptions = Collections.emptyList();
	
	// Conditions
	private List<Condition> _conditions = Collections.emptyList();
	private int _groupMask = GroupType.NONE.getMask();
	
	/**
	 * Creates a new {@code InstanceTemplate} using the provided data.<br>
	 * This constructor initializes the template ID, name, and maximum world count.
	 * @param set The {@code StatSet} containing the configuration values.
	 */
	public InstanceTemplate(StatSet set)
	{
		_templateId = set.getInt("id", 0);
		_name = set.getString("name", null);
		_maxWorldCount = set.getInt("maxWorlds", -1);
	}
	
	// -------------------------------------------------------------
	// Setters
	// -------------------------------------------------------------
	
	/**
	 * Sets the display name for this instance.<br>
	 * The name is only updated if it is not {@code null} or empty.
	 * @param name The new name to assign to the instance.
	 */
	public void setName(String name)
	{
		if ((name != null) && !name.isEmpty())
		{
			_name = name;
		}
	}
	
	/**
	 * Sets the total duration for this instance.<br>
	 * The value must be greater than {@code 0}.<br>
	 * If a non-positive value is provided, the duration remains unchanged.
	 * @param duration The time in seconds to set for the instance.
	 */
	public void setDuration(int duration)
	{
		if (duration > 0)
		{
			_duration = duration;
		}
	}
	
	/**
	 * Sets the time until an empty instance is destroyed.<br>
	 * The input value is converted from minutes to milliseconds.<br>
	 * If the provided value is less than {@code 0}, no change occurs.
	 * @param emptyDestroyTime The duration in minutes before an empty instance is removed.
	 */
	public void setEmptyDestroyTime(long emptyDestroyTime)
	{
		if (emptyDestroyTime >= 0)
		{
			_emptyDestroyTime = TimeUnit.MINUTES.toMillis(emptyDestroyTime);
		}
	}
	
	/**
	 * Sets the time limit for players to remain in the instance.<br>
	 * This value is only updated if {@code ejectTime} is greater than or equal to {@code 0}.
	 * @param ejectTime The duration in seconds before players are ejected.
	 */
	public void setEjectTime(int ejectTime)
	{
		if (ejectTime >= 0)
		{
			_ejectTime = ejectTime;
		}
	}
	
	/**
	 * Sets whether players are allowed to summon others into this instance.<br>
	 * This updates the {@code _allowPlayerSummon} field.
	 * @param value The boolean value to set for summoning permission.
	 */
	public void allowPlayerSummon(boolean value)
	{
		_allowPlayerSummon = value;
	}
	
	/**
	 * Sets whether Player vs Player combat is enabled in this instance.<br>
	 * Use {@code true} to allow players to attack each other.<br>
	 * Use {@code false} to disable player combat.
	 * @param value The boolean state of PvP status.
	 */
	public void setPvP(boolean value)
	{
		_isPvP = value;
	}
	
	/**
	 * Updates the instance parameters using a provided map.<br>
	 * This method initializes the {@code _parameters} field with the values from the input map.<br>
	 * If the map is empty, no changes are made.
	 * @param set A {@code Map<String, Object>} containing the parameter keys and values.
	 */
	public void setParameters(Map<String, Object> set)
	{
		if (!set.isEmpty())
		{
			_parameters = new StatSet(Collections.unmodifiableMap(set));
		}
	}
	
	/**
	 * Adds a door to the instance configuration.<br>
	 * This method maps a {@code DoorTemplate} to a specific {@code templateId}.
	 * @param templateId The unique identifier for the door.
	 * @param template The {@code DoorTemplate} object to be added.
	 */
	public void addDoor(int templateId, DoorTemplate template)
	{
		_doors.put(templateId, template);
	}
	
	/**
	 * Adds a list of spawn templates to the instance.<br>
	 * This method updates the internal collection of spawns for this {@code InstanceTemplate}.
	 * @param spawns The list of {@code SpawnTemplate} objects to add.
	 */
	public void addSpawns(List<SpawnTemplate> spawns)
	{
		_spawns.addAll(spawns);
	}
	
	/**
	 * Sets the teleportation data for entering an instance.<br>
	 * This defines where players are sent when they enter the zone.
	 * @param type The {@code InstanceTeleportType} used to determine how the teleport occurs.
	 * @param locations A {@code List} of {@code Location} objects representing possible entry points.
	 */
	public void setEnterLocation(InstanceTeleportType type, List<Location> locations)
	{
		_enterLocationType = type;
		_enterLocations = locations;
	}
	
	/**
	 * Sets the teleportation details for leaving an instance.<br>
	 * This defines where players are sent when they exit.
	 * @param type The {@code InstanceTeleportType} used to determine the exit behavior.
	 * @param locations A {@code List} of {@code Location} objects representing possible exit points.
	 */
	public void setExitLocation(InstanceTeleportType type, List<Location> locations)
	{
		_exitLocationType = type;
		_exitLocations = locations;
	}
	
	/**
	 * Sets the re-entry data for this instance.<br>
	 * This method defines how players can re-enter the zone based on specific types and time holders.
	 * @param type The {@code InstanceReenterType} to be applied.
	 * @param holder A {@code List} of {@code InstanceReenterTimeHolder} objects containing timing data.
	 */
	public void setReenterData(InstanceReenterType type, List<InstanceReenterTimeHolder> holder)
	{
		_reenterType = type;
		_reenterData = holder;
	}
	
	/**
	 * Configures which buffs should be removed when entering an instance.<br>
	 * This method sets the buff removal type and a list of exceptions.
	 * @param type The {@code InstanceRemoveBuffType} to apply.
	 * @param exceptionList A {@code List} of IDs that should not be removed.
	 */
	public void setRemoveBuff(InstanceRemoveBuffType type, List<Integer> exceptionList)
	{
		_removeBuffType = type;
		_removeBuffExceptions = exceptionList;
	}
	
	/**
	 * Sets the entry conditions for this instance.<br>
	 * This method updates the internal condition list and recalculates the allowed group masks.<br>
	 * It determines if players can enter alone, in a party, or via command channels based on the provided {@code Condition} objects.
	 * @param conditions The list of {@code Condition} objects to apply to this instance.
	 */
	public void setConditions(List<Condition> conditions)
	{
		// Set conditions
		_conditions = conditions;
		
		// Now iterate over conditions and determine enter group data.
		boolean onlyCC = false;
		int min = 1;
		int max = 1;
		for (Condition cond : _conditions)
		{
			if (cond instanceof ConditionCommandChannel)
			{
				onlyCC = true;
			}
			else if (cond instanceof ConditionGroupMin)
			{
				min = ((ConditionGroupMin) cond).getLimit();
			}
			else if (cond instanceof ConditionGroupMax)
			{
				max = ((ConditionGroupMax) cond).getLimit();
			}
		}
		
		// Reset group mask before setting new group.
		_groupMask = 0;
		
		// Check if player can enter in other group then Command channel.
		if (!onlyCC)
		{
			// Player
			if (min == 1)
			{
				_groupMask |= GroupType.NONE.getMask();
			}
			
			// Party
			final int partySize = PlayerConfig.ALT_PARTY_MAX_MEMBERS;
			if (((max > 1) && (max <= partySize)) || ((min <= partySize) && (max > partySize)))
			{
				_groupMask |= GroupType.PARTY.getMask();
			}
		}
		
		// Command channel
		if (onlyCC || (max > 7))
		{
			_groupMask |= GroupType.COMMAND_CHANNEL.getMask();
		}
	}
	
	// -------------------------------------------------------------
	// Getters
	// -------------------------------------------------------------
	/**
	 * Retrieves the unique identifier for this instance template.
	 * @return The {@code int} ID of the template.
	 */
	public int getId()
	{
		return _templateId;
	}
	
	/**
	 * Retrieves the name of the instance.
	 * @return The {@code String} name of this instance.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the list of entry points for this instance.<br>
	 * Each {@code Location} in the list represents a valid spot where players can enter.
	 * @return A {@code List} of {@code Location} objects.
	 */
	public List<Location> getEnterLocations()
	{
		return _enterLocations;
	}
	
	/**
	 * Retrieves the entry location for this instance.<br>
	 * The result depends on whether the entrance type is {@code FIXED} or {@code RANDOM}.
	 * @return the {@code Location} where players will appear.
	 */
	public Location getEnterLocation()
	{
		Location loc = null;
		switch (_enterLocationType)
		{
			case RANDOM:
			{
				loc = _enterLocations.get(Rnd.get(_enterLocations.size()));
				break;
			}
			case FIXED:
			{
				loc = _enterLocations.get(0);
				break;
			}
		}
		
		return loc;
	}
	
	/**
	 * Retrieves the teleport type used when exiting this instance.<br>
	 * This value determines how players are moved when they leave the zone.
	 * @return the {@code InstanceTeleportType} for the exit location.
	 */
	public InstanceTeleportType getExitLocationType()
	{
		return _exitLocationType;
	}
	
	/**
	 * Determines the exit {@code Location} for a given {@code Player}.<br>
	 * The result depends on the configured {@code _exitLocationType}.<br>
	 * It can return a random, fixed, or origin location.
	 * @param player The {@code Player} who is exiting the instance.
	 * @return The calculated {@code Location} for the exit.
	 */
	public Location getExitLocation(Player player)
	{
		Location location = null;
		switch (_exitLocationType)
		{
			case RANDOM:
			{
				location = _exitLocations.get(Rnd.get(_exitLocations.size()));
				break;
			}
			case FIXED:
			{
				location = _exitLocations.get(0);
				break;
			}
			case ORIGIN:
			{
				final PlayerVariables vars = player.getVariables();
				if (vars.contains(PlayerVariables.INSTANCE_ORIGIN))
				{
					final int[] loc = vars.getIntArray(PlayerVariables.INSTANCE_ORIGIN, ";");
					if ((loc != null) && (loc.length == 3))
					{
						location = new Location(loc[0], loc[1], loc[2]);
					}
					
					vars.remove(PlayerVariables.INSTANCE_ORIGIN);
				}
				break;
			}
			case TOWN:
			{
				if (player.getReputation() < 0)
				{
					location = MapRegionData.getInstance().getNearestKarmaRespawn(player);
				}
				else
				{
					location = MapRegionData.getInstance().getNearestTownRespawn(player);
				}
				break;
			}
		}
		
		return location;
	}
	
	/**
	 * Retrieves the time until an empty instance is destroyed.<br>
	 * This value is returned as a {@code long}.
	 * @return The destruction time for an empty instance.
	 */
	public long getEmptyDestroyTime()
	{
		return _emptyDestroyTime;
	}
	
	/**
	 * Retrieves the total duration of the instance.
	 * @return The duration value as an {@code int}.
	 */
	public int getDuration()
	{
		return _duration;
	}
	
	/**
	 * Retrieves the time limit for players to remain in the instance.<br>
	 * This value is used to determine when a player should be removed.
	 * @return the {@code ejectTime} as an {@code int}.
	 */
	public int getEjectTime()
	{
		return _ejectTime;
	}
	
	/**
	 * Checks if players are allowed to be summoned into this instance.<br>
	 * This value is determined by the {@code allowPlayerSummon} setting.
	 * @return {@code true} if summoning is enabled, {@code false} otherwise.
	 */
	public boolean isPlayerSummonAllowed()
	{
		return _allowPlayerSummon;
	}
	
	/**
	 * Checks if Player vs Player (PvP) is enabled in this instance.
	 * @return {@code true} if PvP is allowed, {@code false} otherwise.
	 */
	public boolean isPvP()
	{
		return _isPvP;
	}
	
	/**
	 * Retrieves all doors associated with this instance.<br>
	 * The map uses the door ID as the key.
	 * @return A {@code Map} containing the door IDs and their corresponding {@code DoorTemplate} objects.
	 */
	public Map<Integer, DoorTemplate> getDoors()
	{
		return _doors;
	}
	
	/**
	 * Retrieves the list of spawn templates for this instance.
	 * @return a {@code List} containing all {@code SpawnTemplate} objects.
	 */
	public List<SpawnTemplate> getSpawns()
	{
		return _spawns;
	}
	
	/**
	 * Retrieves the maximum number of worlds allowed.<br>
	 * This value is used to limit world creation.
	 * @return the maximum world count as an {@code int}.
	 */
	public int getMaxWorlds()
	{
		return _maxWorldCount;
	}
	
	/**
	 * Retrieves the configuration parameters for this instance.<br>
	 * These values are stored in a {@code Map}.
	 * @return the {@code StatSet} containing all instance parameters.
	 */
	public StatSet getParameters()
	{
		return _parameters;
	}
	
	/**
	 * Checks if the buff removal feature is currently enabled.<br>
	 * It returns {@code true} if the type is not set to {@code NONE}.
	 * @return {@code true} if buffs are removed, {@code false} otherwise.
	 */
	public boolean isRemoveBuffEnabled()
	{
		return _removeBuffType != InstanceRemoveBuffType.NONE;
	}
	
	/**
	 * Removes specific buffs from a {@code Player} and their associated entities.<br>
	 * This method affects the player, their servitors, and their pet if one exists.<br>
	 * It checks the configured {@code InstanceRemoveBuffType} to determine which effects to stop.
	 * @param player The {@code Player} whose buffs will be removed.
	 */
	public void removePlayerBuff(Player player)
	{
		// Make list of affected playable objects.
		final List<Playable> affected = new ArrayList<>();
		affected.add(player);
		player.getServitors().values().forEach(affected::add);
		if (player.hasPet())
		{
			affected.add(player.getPet());
		}
		
		// Now remove buffs by type.
		if (_removeBuffType == InstanceRemoveBuffType.ALL)
		{
			for (Playable playable : affected)
			{
				playable.stopAllEffectsExceptThoseThatLastThroughDeath();
			}
		}
		else
		{
			for (Playable playable : affected)
			{
				// Stop all buffs.
				playable.getEffectList().stopEffects(info -> !info.getSkill().isIrreplaceableBuff() && info.getSkill().getBuffType().isBuff() && hasRemoveBuffException(info.getSkill()), true, true);
			}
		}
	}
	
	/**
	 * Checks if a specific skill is an exception for buff removal.<br>
	 * It evaluates the {@code _removeBuffExceptions} list based on the current {@code _removeBuffType}.
	 * @param skill The {@code Skill} object to check.
	 * @return {@code true} if the skill should be excluded from the removal logic, {@code false} otherwise.
	 */
	private boolean hasRemoveBuffException(Skill skill)
	{
		final boolean containsSkill = _removeBuffExceptions.contains(skill.getId());
		return (_removeBuffType == InstanceRemoveBuffType.BLACKLIST) ? containsSkill : !containsSkill;
	}
	
	/**
	 * Retrieves the reenter type for this instance.<br>
	 * This value determines how players can rejoin the instance.
	 * @return the {@code InstanceReenterType} of this instance.
	 */
	public InstanceReenterType getReenterType()
	{
		return _reenterType;
	}
	
	/**
	 * Calculates the next available time a player can re-enter this instance.<br>
	 * It checks all {@code InstanceReenterTimeHolder} objects in the data list.<br>
	 * The method returns the earliest valid timestamp found.
	 * @return The next allowed re-entry time in milliseconds, or -1 if no data is available.
	 */
	public long calculateReenterTime()
	{
		long time = -1;
		for (InstanceReenterTimeHolder data : _reenterData)
		{
			if (data.getTime() > 0)
			{
				time = System.currentTimeMillis() + data.getTime();
				break;
			}
			
			final Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, data.getHour());
			calendar.set(Calendar.MINUTE, data.getMinute());
			calendar.set(Calendar.SECOND, 0);
			
			// If calendar time is lower than current, add one more day.
			final long currentTime = System.currentTimeMillis();
			if (calendar.getTimeInMillis() <= currentTime)
			{
				calendar.add(Calendar.DAY_OF_MONTH, 1);
			}
			
			// Modify calendar day
			if (data.getDay() != null)
			{
				// DayOfWeek starts with Monday(1) but Calendar starts with Sunday(1).
				int day = data.getDay().getValue() + 1;
				if (day > 7)
				{
					day = 1;
				}
				
				// Set exact day. If modified date is before current, add one more week.
				calendar.set(Calendar.DAY_OF_WEEK, day);
				if (calendar.getTimeInMillis() <= currentTime)
				{
					calendar.add(Calendar.WEEK_OF_MONTH, 1);
				}
			}
			
			final long calendarTime = calendar.getTimeInMillis();
			if ((time == -1) || (calendarTime < time))
			{
				time = calendarTime;
			}
		}
		
		return time;
	}
	
	/**
	 * Checks if the current group mask includes a specific group type.<br>
	 * It compares the bitmask of the provided {@code GroupType} against the internal mask.
	 * @param type The {@code GroupType} to check.
	 * @return {@code true} if all bits for the given type are set, otherwise {@code false}.
	 */
	private boolean groupMaskContains(GroupType type)
	{
		final int flag = type.getMask();
		return (_groupMask & flag) == flag;
	}
	
	/**
	 * Determines the appropriate {@code GroupType} for a player to enter an instance.<br>
	 * It checks if the player's group matches the allowed mask.<br>
	 * If no direct match is found, it tries to find the nearest compatible group type.<br>
	 * Returns {@code null} if the player does not meet any entry requirements.
	 * @param player The {@code Player} attempting to enter the instance.
	 * @return The determined {@code GroupType} or {@code null} if entry is denied.
	 */
	private GroupType getEnterGroupType(Player player)
	{
		// If mask doesn't contain any group.
		if (_groupMask == 0)
		{
			return null;
		}
		
		// If player can override instance conditions then he can enter alone.
		if (player.isGM())
		{
			return GroupType.NONE;
		}
		
		// Check if mask contains player's group.
		final GroupType playerGroup = player.getGroupType();
		if (groupMaskContains(playerGroup))
		{
			return playerGroup;
		}
		
		// Check if mask contains only one group.
		final GroupType type = GroupType.getByMask(_groupMask);
		if (type != null)
		{
			return type;
		}
		
		// When mask contains more group types but without player's group, choose nearest one.
		// player < party < command channel
		for (GroupType t : GroupType.values())
		{
			if ((t != playerGroup) && groupMaskContains(t))
			{
				return t;
			}
		}
		
		// Nothing found? then player cannot enter.
		return null;
	}
	
	/**
	 * Retrieves the list of players allowed to enter an instance.<br>
	 * This method identifies the correct group based on the player's requirements.<br>
	 * It includes the requesting {@code Player} and all other members of their party or command channel.
	 * @param player The {@code Player} requesting to enter the instance.
	 * @return A {@code List} of {@code Player} objects that can enter, or {@code null} if no group type is found.
	 */
	public List<Player> getEnterGroup(Player player)
	{
		final GroupType type = getEnterGroupType(player);
		if (type == null)
		{
			return null;
		}
		
		// Make list of players which can enter into instance world.
		final List<Player> group = new ArrayList<>();
		group.add(player); // Put player who made request at first position inside list.
		
		// Check if player has group in which he can enter.
		AbstractPlayerGroup pGroup = null;
		if (type == GroupType.PARTY)
		{
			pGroup = player.getParty();
		}
		else if (type == GroupType.COMMAND_CHANNEL)
		{
			pGroup = player.getCommandChannel();
		}
		
		// If any group found then put them into enter group list.
		if (pGroup != null)
		{
			for (Player member : pGroup.getMembers())
			{
				if (!member.equals(player))
				{
					group.add(member);
				}
			}
		}
		
		return group;
	}
	
	/**
	 * Checks if all instance conditions are met.<br>
	 * It iterates through every {@code Condition} in the template.<br>
	 * If any condition fails, it returns {@code false}.<br>
	 * Otherwise, it returns {@code true}.
	 * @param group The list of {@code Player} objects to check.
	 * @param npc The {@code Npc} triggering the validation.
	 * @param htmlCallback A {@code BiConsumer} used to send HTML messages to players.
	 * @return {@code true} if all conditions pass, otherwise {@code false}.
	 */
	public boolean validateConditions(List<Player> group, Npc npc, BiConsumer<Player, String> htmlCallback)
	{
		for (Condition cond : _conditions)
		{
			if (!cond.validate(npc, group, htmlCallback))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Applies all active conditions to a specific group of players.<br>
	 * This method iterates through the internal condition list and triggers their effects.
	 * @param group The {@code List} of {@code Player} objects to receive the effects.
	 */
	public void applyConditionEffects(List<Player> group)
	{
		_conditions.forEach(c -> c.applyEffect(group));
	}
	
	/**
	 * Retrieves the experience rate for this instance.
	 * @return The current {@code float} value of the experience rate.
	 */
	public float getExpRate()
	{
		return _expRate;
	}
	
	/**
	 * Sets the experience rate for this instance.<br>
	 * This value determines how much experience players receive.
	 * @param expRate The new experience rate to apply.
	 */
	public void setExpRate(float expRate)
	{
		_expRate = expRate;
	}
	
	/**
	 * Retrieves the spawn rate for this instance.
	 * @return The current {@code float} value of the spawn rate.
	 */
	public float getSPRate()
	{
		return _spRate;
	}
	
	/**
	 * Sets the spawn rate for this instance.<br>
	 * This value determines how frequently entities appear.
	 * @param spRate The new spawn rate to apply.
	 */
	public void setSPRate(float spRate)
	{
		_spRate = spRate;
	}
	
	/**
	 * Retrieves the experience rate for parties.<br>
	 * This value is used to calculate how much experience players receive when grouped.
	 * @return The party experience rate as a {@code float}.
	 */
	public float getExpPartyRate()
	{
		return _expPartyRate;
	}
	
	/**
	 * Sets the experience rate for parties within this instance.<br>
	 * This value determines how much extra experience party members receive.
	 * @param expRate The new experience rate to apply.
	 */
	public void setExpPartyRate(float expRate)
	{
		_expPartyRate = expRate;
	}
	
	/**
	 * Retrieves the party rate for this instance.<br>
	 * This value determines the multiplier applied to group actions.
	 * @return The current {@code float} party rate.
	 */
	public float getSPPartyRate()
	{
		return _spPartyRate;
	}
	
	/**
	 * Sets the special party rate for this instance.<br>
	 * This value determines the multiplier applied to party rewards.
	 * @param spRate The new {@code float} value for the party rate.
	 */
	public void setSPPartyRate(float spRate)
	{
		_spPartyRate = spRate;
	}
	
	/**
	 * Retrieves the number of worlds where this instance exists.<br>
	 * It uses the unique ID of the current {@code InstanceTemplate}.
	 * @return The total count of worlds for this instance.
	 */
	public long getWorldCount()
	{
		return InstanceManager.getInstance().getWorldCount(getId());
	}
	
	/**
	 * Returns a string representation of the instance template.<br>
	 * This includes the {@code _templateId} and the {@code _name}.
	 * @return A formatted string containing the ID and Name.
	 */
	@Override
	public String toString()
	{
		return "ID: " + _templateId + " Name: " + _name;
	}
}
