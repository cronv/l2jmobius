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
package ai.others.Spawns;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.OnDayNightChange;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Manages the spawning of entities based on the current time of day.<br>
 * This {@code Class} listens for {@code OnDayNightChange} events to update active spawns.<br>
 * It ensures that specific mobs appear only during appropriate daylight or nighttime cycles.
 * @author UnAfraid
 */
public class DayNightSpawns extends Script
{
	private static final String NIGHT_GROUP_NAME = "nightTime";
	private static final String DAY_GROUP_NAME = "dayTime";
	private final Set<SpawnTemplate> _templates = ConcurrentHashMap.newKeySet();
	
	/**
	 * Private constructor for the {@code DayNightSpawns} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private DayNightSpawns()
	{
	}
	
	/**
	 * This method is called when a new spawn is activated.<br>
	 * It adds the {@code template} to the internal collection.<br>
	 * It then triggers the initial spawn management based on the current time.
	 * @param template The {@code SpawnTemplate} being activated.
	 */
	@Override
	public void onSpawnActivate(SpawnTemplate template)
	{
		if (_templates.add(template))
		{
			manageSpawns(template, GameTimeTaskManager.getInstance().isNight());
		}
	}
	
	/**
	 * This method handles the deactivation of a specific spawn.<br>
	 * It removes the provided {@code SpawnTemplate} from the active tracking set.
	 * @param template The {@code SpawnTemplate} to be deactivated.
	 */
	@Override
	public void onSpawnDeactivate(SpawnTemplate template)
	{
		_templates.remove(template);
	}
	
	/**
	 * Handles the logic when the game time switches between day and night.<br>
	 * This method updates all registered spawn templates based on the new time state.
	 * @param event The {@code OnDayNightChange} object containing the current time status.
	 */
	@RegisterEvent(EventType.ON_DAY_NIGHT_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onDayNightChange(OnDayNightChange event)
	{
		_templates.forEach(template -> manageSpawns(template, event.isNight()));
	}
	
	/**
	 * This method updates the visibility of spawns based on the time of day.<br>
	 * It checks if a group belongs to the night or day category.<br>
	 * Spawns are activated or deactivated depending on the {@code isNight} value.
	 * @param template The {@code SpawnTemplate} containing the groups to manage.
	 * @param isNight A boolean indicating if it is currently night time.
	 */
	private void manageSpawns(SpawnTemplate template, boolean isNight)
	{
		template.getGroups().forEach(group ->
		{
			final String groupName = group.getName();
			if (NIGHT_GROUP_NAME.equalsIgnoreCase(groupName))
			{
				if (!isNight)
				{
					group.despawnAll();
				}
				else
				{
					group.spawnAll();
				}
			}
			else if (DAY_GROUP_NAME.equalsIgnoreCase(groupName))
			{
				if (isNight)
				{
					group.despawnAll();
				}
				else
				{
					group.spawnAll();
				}
			}
		});
	}
	
	/**
	 * This is the entry point for the {@code DayNightSpawns} class.<br>
	 * It initializes a new instance of the script to start the application.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new DayNightSpawns();
	}
}
