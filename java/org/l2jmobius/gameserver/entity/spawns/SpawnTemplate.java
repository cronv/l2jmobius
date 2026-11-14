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
package org.l2jmobius.gameserver.entity.spawns;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.zone.type.BannedSpawnTerritory;
import org.l2jmobius.gameserver.entity.zone.type.SpawnTerritory;
import org.l2jmobius.gameserver.interfaces.IParameterized;
import org.l2jmobius.gameserver.interfaces.ITerritorized;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a blueprint for spawning entities within the game world.<br>
 * It defines the properties and constraints required to create new spawns.<br>
 * This class is used by the server to manage how and where mobs appear.
 * @author UnAfraid, Mobius
 */
public class SpawnTemplate implements Cloneable, ITerritorized, IParameterized<StatSet>
{
	private final String _name;
	private final String _ai;
	private final boolean _spawnByDefault;
	private final File _file;
	private List<SpawnTerritory> _territories;
	private List<BannedSpawnTerritory> _bannedTerritories;
	private final List<SpawnGroup> _groups = new ArrayList<>();
	private StatSet _parameters;
	
	/**
	 * Creates a new {@code SpawnTemplate} using data from a configuration file.<br>
	 * This constructor extracts the name, AI, and default spawn status from a {@code StatSet}.
	 * @param set The {@code StatSet} containing the template properties.
	 * @param file The {@code File} object representing the configuration source.
	 */
	public SpawnTemplate(StatSet set, File file)
	{
		this(set.getString("name", null), set.getString("ai", null), set.getBoolean("spawnByDefault", true), file);
	}
	
	/**
	 * Creates a new {@code SpawnTemplate} with specific configuration.<br>
	 * This constructor initializes the basic properties of the spawn.
	 * @param name The display name of the template.
	 * @param ai The artificial intelligence type to use.
	 * @param spawnByDefault Determines if this template spawns by default. Use {@code true} or {@code false}.
	 * @param file The configuration file associated with this template.
	 */
	private SpawnTemplate(String name, String ai, boolean spawnByDefault, File file)
	{
		_name = name;
		_ai = ai;
		_spawnByDefault = spawnByDefault;
		_file = file;
	}
	
	/**
	 * Retrieves the name of the {@code SpawnTemplate}.
	 * @return The name as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the artificial intelligence identifier for this spawn template.<br>
	 * This value determines which AI behavior is used by the spawned entities.
	 * @return The {@code String} representing the AI type.
	 */
	public String getAI()
	{
		return _ai;
	}
	
	/**
	 * Checks if this spawn template is enabled by default.<br>
	 * Returns {@code true} if it should spawn automatically.<br>
	 * Returns {@code false} otherwise.
	 * @return the default spawning status of the template.
	 */
	public boolean isSpawningByDefault()
	{
		return _spawnByDefault;
	}
	
	/**
	 * Retrieves the {@code File} associated with this template.
	 * @return The {@code File} object.
	 */
	public File getFile()
	{
		return _file;
	}
	
	/**
	 * Adds a new territory to the list of spawn territories.<br>
	 * This method initializes the internal list if it is currently {@code null}.
	 * @param territory The {@code SpawnTerritory} object to be added.
	 */
	@Override
	public void addTerritory(SpawnTerritory territory)
	{
		if (_territories == null)
		{
			_territories = new ArrayList<>();
		}
		
		_territories.add(territory);
	}
	
	/**
	 * Retrieves the list of territories associated with this template.<br>
	 * Returns an empty {@code List} if no territories exist.
	 * @return a {@code List} of {@code SpawnTerritory} objects.
	 */
	@Override
	public List<SpawnTerritory> getTerritories()
	{
		return _territories != null ? _territories : Collections.emptyList();
	}
	
	/**
	 * Adds a new territory to the list of banned spawn areas.<br>
	 * This method ensures the internal list is initialized before adding the data.
	 * @param territory The {@code BannedSpawnTerritory} object to be added.
	 */
	@Override
	public void addBannedTerritory(BannedSpawnTerritory territory)
	{
		if (_bannedTerritories == null)
		{
			_bannedTerritories = new ArrayList<>();
		}
		
		_bannedTerritories.add(territory);
	}
	
	/**
	 * Retrieves the list of territories where spawning is prohibited.<br>
	 * Returns an empty {@code List} if no banned territories exist.
	 * @return a {@code List} of {@code BannedSpawnTerritory} objects.
	 */
	@Override
	public List<BannedSpawnTerritory> getBannedTerritories()
	{
		return _bannedTerritories != null ? _bannedTerritories : Collections.emptyList();
	}
	
	/**
	 * Adds a new {@code SpawnGroup} to the template.<br>
	 * This allows for organized spawning of entities within this template.
	 * @param group The {@code SpawnGroup} to be added.
	 */
	public void addGroup(SpawnGroup group)
	{
		_groups.add(group);
	}
	
	/**
	 * Retrieves the list of all spawn groups associated with this template.
	 * @return a {@code List} containing all {@code SpawnGroup} objects.
	 */
	public List<SpawnGroup> getGroups()
	{
		return _groups;
	}
	
	/**
	 * Retrieves a list of {@code SpawnGroup} objects that match the given name.<br>
	 * The search is case-insensitive.<br>
	 * If no matches are found, an empty list is returned.
	 * @param name The name to search for in the spawn groups.
	 * @return A list of matching {@code SpawnGroup} objects.
	 */
	public List<SpawnGroup> getGroupsByName(String name)
	{
		final List<SpawnGroup> result = new ArrayList<>();
		for (SpawnGroup group : _groups)
		{
			final String groupName = group.getName();
			if ((groupName != null) && groupName.equalsIgnoreCase(name))
			{
				result.add(group);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the current set of parameters for this spawn template.
	 * @return the {@code StatSet} containing the configuration data.
	 */
	@Override
	public StatSet getParameters()
	{
		return _parameters;
	}
	
	/**
	 * Updates the {@code StatSet} for this spawn template.<br>
	 * This method replaces the current parameters with a new set.
	 * @param parameters The new {@code StatSet} to apply.
	 */
	@Override
	public void setParameters(StatSet parameters)
	{
		_parameters = parameters;
	}
	
	/**
	 * Notifies an event based on the AI script associated with this template.<br>
	 * It retrieves the {@code Quest} from the {@code ScriptManager}.<br>
	 * If the script exists, it is passed to the provided consumer.
	 * @param event The {@code Consumer} used to handle the {@code Quest} object.
	 */
	public void notifyEvent(Consumer<Quest> event)
	{
		if (_ai != null)
		{
			final Quest script = ScriptManager.getInstance().getScript(_ai);
			if (script != null)
			{
				event.accept(script);
			}
		}
	}
	
	/**
	 * Spawns specific groups of entities into an instance.<br>
	 * This method iterates through all available {@code SpawnGroup} objects.<br>
	 * It only spawns the groups that match the provided filter.
	 * @param groupFilter A {@code Predicate} used to determine which {@code SpawnGroup} objects should be spawned.
	 * @param instance The {@code Instance} where the entities will be created.
	 */
	public void spawn(Predicate<SpawnGroup> groupFilter, Instance instance)
	{
		for (SpawnGroup group : _groups)
		{
			if (groupFilter.test(group))
			{
				group.spawnAll(instance);
			}
		}
	}
	
	/**
	 * Spawns all entities defined in this template.<br>
	 * This method calls {@code spawnAll(null)} to initiate the process.
	 */
	public void spawnAll()
	{
		spawnAll(null);
	}
	
	/**
	 * Spawns all default groups within this template.<br>
	 * This method uses the {@code SpawnGroup#isSpawningByDefault} predicate to filter which groups to create.
	 * @param instance The {@code Instance} where the entities will be spawned.
	 */
	public void spawnAll(Instance instance)
	{
		spawn(SpawnGroup::isSpawningByDefault, instance);
	}
	
	/**
	 * Notifies all active scripts that this spawn has been activated.<br>
	 * This method triggers the {@code onSpawnActivate} logic for associated quests.
	 */
	public void notifyActivate()
	{
		notifyEvent(script -> script.onSpawnActivate(this));
	}
	
	/**
	 * Spawns all groups associated with this template.<br>
	 * This includes groups that are not set to spawn by default.
	 * @param instance The {@code Instance} where the entities will be spawned.
	 */
	public void spawnAllIncludingNotDefault(Instance instance)
	{
		_groups.forEach(group -> group.spawnAll(instance));
	}
	
	/**
	 * Removes active entities from specific spawn groups.<br>
	 * This method checks each group against the provided filter.<br>
	 * If a group matches, all its members are despawned.<br>
	 * Finally, it notifies any active scripts of the deactivation.
	 * @param groupFilter A {@code Predicate} used to determine which {@code SpawnGroup} objects should be despawned.
	 */
	public void despawn(Predicate<SpawnGroup> groupFilter)
	{
		for (SpawnGroup group : _groups)
		{
			if (groupFilter.test(group))
			{
				group.despawnAll();
			}
		}
		
		notifyEvent(script -> script.onSpawnDeactivate(this));
	}
	
	/**
	 * Removes all active entities from every {@code SpawnGroup}.<br>
	 * This method also triggers the deactivation event for scripts.
	 */
	public void despawnAll()
	{
		_groups.forEach(SpawnGroup::despawnAll);
		notifyEvent(script -> script.onSpawnDeactivate(this));
	}
	
	/**
	 * Creates a deep copy of this {@code SpawnTemplate}.<br>
	 * This method copies all internal data including territories and groups.
	 * @return A new {@code SpawnTemplate} instance with the same properties.
	 */
	@Override
	public SpawnTemplate clone()
	{
		final SpawnTemplate template = new SpawnTemplate(_name, _ai, _spawnByDefault, _file);
		
		// Clone parameters
		template.setParameters(_parameters);
		
		// Clone banned territories
		for (BannedSpawnTerritory territory : getBannedTerritories())
		{
			template.addBannedTerritory(territory);
		}
		
		// Clone territories
		for (SpawnTerritory territory : getTerritories())
		{
			template.addTerritory(territory);
		}
		
		// Clone groups
		for (SpawnGroup group : _groups)
		{
			template.addGroup(group.clone());
		}
		
		return template;
	}
}
