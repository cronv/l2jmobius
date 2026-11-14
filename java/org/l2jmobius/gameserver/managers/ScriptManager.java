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
package org.l2jmobius.gameserver.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.scripting.ScriptEngine;

/**
 * Manages the loading and execution of game scripts.<br>
 * It handles {@code Quest} objects and interacts with the {@code ScriptEngine}.
 */
public class ScriptManager
{
	private static final Logger LOGGER = Logger.getLogger(ScriptManager.class.getName());
	
	/** Map containing all the quests. */
	private final Map<String, Quest> _quests = new ConcurrentHashMap<>();
	/** Map containing all the scripts. */
	private final Map<String, Quest> _scripts = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code ScriptManager}.<br>
	 * This constructor is protected to prevent direct instantiation from outside the package.
	 */
	protected ScriptManager()
	{
	}
	
	/**
	 * Reloads a specific script from the provided folder.<br>
	 * This method checks if the script exists before calling its {@code reload()} method.
	 * @param questFolder The name of the folder containing the script to reload.
	 */
	public void reload(String questFolder)
	{
		final Quest q = getScript(questFolder);
		if (q != null)
		{
			q.reload();
		}
	}
	
	/**
	 * Reloads a specific quest based on its unique identifier.<br>
	 * This method finds the {@code Quest} object associated with the provided ID.<br>
	 * If the quest exists, it triggers the internal reload process.
	 * @param questId The unique integer ID of the quest to reload.
	 */
	public void reload(int questId)
	{
		final Quest q = getQuest(questId);
		if (q != null)
		{
			q.reload();
		}
	}
	
	/**
	 * This method reloads all scripts currently registered in the system.<br>
	 * It first clears all existing scripts from memory.<br>
	 * Then it executes the script list using {@code ScriptEngine}.<br>
	 * Finally, it generates a report of the current status.
	 */
	public void reloadAllScripts()
	{
		unloadAllScripts();
		
		LOGGER.info("Reloading all server scripts.");
		try
		{
			ScriptEngine.getInstance().executeScriptList();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed executing script list!", e);
		}
		
		getInstance().report();
	}
	
	/**
	 * Removes all active scripts and quests from the server.<br>
	 * This method calls {@code unload(false)} on every {@code Quest} object.<br>
	 * It then clears both the quest and script maps.
	 */
	public void unloadAllScripts()
	{
		LOGGER.info("Unloading all server scripts.");
		
		// Unload quests.
		for (Quest quest : _quests.values())
		{
			if (quest != null)
			{
				quest.unload(false);
			}
		}
		
		_quests.clear();
		
		// Unload scripts.
		for (Quest script : _scripts.values())
		{
			if (script != null)
			{
				script.unload(false);
			}
		}
		
		_scripts.clear();
	}
	
	/**
	 * Logs the current status of loaded content.<br>
	 * This method prints the total count of {@code Quest} objects in both the quests and scripts maps.<br>
	 * It uses the {@code LOGGER} to output these counts to the server logs.
	 */
	public void report()
	{
		final String simpleName = getClass().getSimpleName();
		LOGGER.info(simpleName + ": Loaded " + _quests.size() + " quests.");
		LOGGER.info(simpleName + ": Loaded " + _scripts.size() + " scripts.");
	}
	
	/**
	 * Saves all current quests to the database.<br>
	 * Saves all current scripts to the database.<br>
	 * This method iterates through both {@code _quests} and {@code _scripts} collections.
	 */
	public void save()
	{
		// Save quests.
		for (Quest quest : _quests.values())
		{
			quest.onSave();
		}
		
		// Save scripts.
		for (Quest script : _scripts.values())
		{
			script.onSave();
		}
	}
	
	/**
	 * Retrieves a {@code Quest} object based on its name.<br>
	 * This method first searches in the quests map.<br>
	 * If not found, it searches in the scripts map.
	 * @param name The unique name of the quest or script to find.
	 * @return The matching {@code Quest} object, or {@code null} if no match exists.
	 */
	public Quest getScript(String name)
	{
		final Quest quest = _quests.get(name);
		return (quest != null) ? quest : _scripts.get(name);
	}
	
	/**
	 * Retrieves a {@code Quest} object based on its unique identifier.<br>
	 * This method searches through all loaded quests.<br>
	 * It returns {@code null} if no quest matches the provided ID.
	 * @param questId The unique integer ID of the quest to find.
	 * @return The matching {@code Quest} object or {@code null}.
	 */
	public Quest getQuest(int questId)
	{
		for (Quest q : _quests.values())
		{
			if (q.getId() == questId)
			{
				return q;
			}
		}
		
		return null;
	}
	
	/**
	 * Adds a new {@code Quest} to the manager.<br>
	 * If a quest with the same name exists, it will be replaced.<br>
	 * The old quest is automatically unloaded if it is overwritten.
	 * @param quest The {@code Quest} object to add to the system.
	 */
	public void addQuest(Quest quest)
	{
		if (quest == null)
		{
			throw new IllegalArgumentException("Quest argument cannot be null");
		}
		
		// FIXME: unloading the old quest at this point is a tad too late.
		// The new quest has initialized and read the data to start an unpredictable number of tasks, while the old quest will save data that will never be read.
		// Requesting the newQuest to re-read the data is not necessarily ideal because it might cause the quest to repeat actions like starting timers or spawning NPCs.
		// The current solution properly closes the running tasks of the old quest but ignores the data, which is perhaps the least of all evils.
		final String name = quest.getName();
		final Quest old = _quests.put(name, quest);
		if (old != null)
		{
			old.unload();
			LOGGER.info("Replaced quest " + old.getName() + " (" + old.getId() + ") with a new version!");
		}
		
		if (DevelopmentConfig.SHOW_QUEST_LOAD_IN_LOGS)
		{
			final String questName = name.contains("_") ? name.substring(name.indexOf('_') + 1) : name;
			LOGGER.info("Loaded quest " + StringUtil.separateWords(questName) + ".");
		}
	}
	
	/**
	 * Removes a specific script from the manager.<br>
	 * It checks both the quests and scripts collections.<br>
	 * Returns {@code true} if the script was successfully removed.<br>
	 * Returns {@code false} if the script was not found.
	 * @param script The {@code Quest} object to be removed.
	 * @return A boolean indicating success.
	 */
	public boolean removeScript(Quest script)
	{
		final String scriptName = script.getName();
		if (_quests.containsKey(scriptName))
		{
			_quests.remove(scriptName);
			return true;
		}
		else if (_scripts.containsKey(scriptName))
		{
			_scripts.remove(scriptName);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Retrieves all currently loaded quests.<br>
	 * This method returns a map of quest identifiers to {@code Quest} objects.
	 * @return A {@code Map} containing all active quests.
	 */
	public Map<String, Quest> getQuests()
	{
		return _quests;
	}
	
	/**
	 * Unloads a specific quest from the system.<br>
	 * This method saves the quest data before removing it.
	 * @param ms The {@code Quest} object to be unloaded.
	 * @return {@code true} if the script was successfully removed, {@code false} otherwise.
	 */
	public boolean unload(Quest ms)
	{
		ms.onSave();
		return removeScript(ms);
	}
	
	/**
	 * Retrieves all registered scripts.<br>
	 * This method returns a map of script names to {@code Quest} objects.
	 * @return A {@code Map} containing all available scripts.
	 */
	public Map<String, Quest> getScripts()
	{
		return _scripts;
	}
	
	/**
	 * Adds a new {@code Quest} script to the manager.<br>
	 * If a script with the same name already exists, it will be unloaded and replaced.<br>
	 * This method logs the loading status if enabled in the configuration.
	 * @param script The {@code Quest} object to register as a script.
	 */
	public void addScript(Quest script)
	{
		final Quest old = _scripts.put(script.getClass().getSimpleName(), script);
		if (old != null)
		{
			old.unload();
			LOGGER.info("Replaced script " + old.getName() + " with a new version!");
		}
		
		if (DevelopmentConfig.SHOW_SCRIPT_LOAD_IN_LOGS)
		{
			LOGGER.info("Loaded script " + StringUtil.separateWords(script.getClass().getSimpleName()) + ".");
		}
	}
	
	/**
	 * Retrieves the global instance of the {@code ScriptManager}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access script and quest management features.
	 * @return The single shared instance of {@code ScriptManager}.
	 */
	public static ScriptManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ScriptManager INSTANCE = new ScriptManager();
	}
}
