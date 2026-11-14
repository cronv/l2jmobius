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

import java.util.Calendar;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GraciaSeedsConfig;
import org.l2jmobius.gameserver.mechanics.script.Quest;

/**
 * Manages the logic and data for {@code GraciaSeeds} within the game world.<br>
 * This class handles seed-related mechanics and interactions as defined in the configuration.
 */
public class GraciaSeedsManager
{
	private static final Logger LOGGER = Logger.getLogger(GraciaSeedsManager.class.getName());
	
	public static final String ENERGY_SEEDS = "EnergySeeds";
	
	private static final byte SOITYPE = 2;
	private static final byte SOATYPE = 3;
	
	// Seed of Destruction
	private static final byte SODTYPE = 1;
	private int _SoDTiatKilled = 0;
	private int _SoDState = 1;
	private final Calendar _SoDLastStateChangeDate;
	
	/**
	 * Initializes the {@code GraciaSeedsManager}.<br>
	 * Sets the initial date for Seed of Destruction.<br>
	 * Loads existing data from storage.<br>
	 * Processes the current stages of the event.
	 */
	protected GraciaSeedsManager()
	{
		_SoDLastStateChangeDate = Calendar.getInstance();
		loadData();
		handleSodStages();
	}
	
	/**
	 * Saves the current state of a specific seed to the global variables.<br>
	 * This method updates values based on the provided {@code seedType}.<br>
	 * It handles data for Seed of Destruction, Seed of Infinity, and Seed of Annihilation.
	 * @param seedType The type of seed to save.
	 */
	public void saveData(byte seedType)
	{
		switch (seedType)
		{
			case SODTYPE:
			{
				// Seed of Destruction.
				GlobalVariablesManager.getInstance().set("SoDState", _SoDState);
				GlobalVariablesManager.getInstance().set("SoDTiatKilled", _SoDTiatKilled);
				GlobalVariablesManager.getInstance().set("SoDLSCDate", _SoDLastStateChangeDate.getTimeInMillis());
				break;
			}
			case SOITYPE:
			{
				// Seed of Infinity.
				break;
			}
			case SOATYPE:
			{
				// Seed of Annihilation.
				break;
			}
			default:
			{
				LOGGER.warning(getClass().getSimpleName() + ": Unknown SeedType in SaveData: " + seedType);
				break;
			}
		}
	}
	
	/**
	 * Loads the Seed of Destruction data from global variables.<br>
	 * It checks if the variable {@code SoDState} exists in {@code GlobalVariablesManager}.<br>
	 * If it does not exist, it saves the initial values using {@code saveData}.
	 */
	public void loadData()
	{
		// Seed of Destruction variables
		if (GlobalVariablesManager.getInstance().hasVariable("SoDState"))
		{
			_SoDState = GlobalVariablesManager.getInstance().getInt("SoDState", 0);
			_SoDTiatKilled = GlobalVariablesManager.getInstance().getInt("SoDTiatKilled", 0);
			_SoDLastStateChangeDate.setTimeInMillis(GlobalVariablesManager.getInstance().getLong("SoDLSCDate", 0));
		}
		else
		{
			// save Initial values
			saveData(SODTYPE);
		}
	}
	
	/**
	 * Manages the progression logic for the Seed of Destruction event.<br>
	 * It checks the current {@code _SoDState} and updates it based on elapsed time or completion status.<br>
	 * This method handles transitions between different stages of the quest.
	 */
	private void handleSodStages()
	{
		switch (_SoDState)
		{
			case 1:
			{
				// Do nothing players should kill Tiat a few times.
				break;
			}
			case 2:
			{
				// Conquest Complete state, if too much time is passed than change to defense state.
				final long timePast = System.currentTimeMillis() - _SoDLastStateChangeDate.getTimeInMillis();
				if (timePast >= GraciaSeedsConfig.SOD_STAGE_2_LENGTH)
				{
					// Change to Attack state because Defend statet is not implemented.
					setSoDState(1, true);
				}
				else
				{
					ThreadPool.schedule(() ->
					{
						setSoDState(1, true);
						updateSodState();
					}, GraciaSeedsConfig.SOD_STAGE_2_LENGTH - timePast);
				}
				break;
			}
			case 3:
			{
				// Not implemented.
				setSoDState(1, true);
				break;
			}
			default:
			{
				LOGGER.warning(getClass().getSimpleName() + ": Unknown Seed of Destruction state(" + _SoDState + ")! ");
				break;
			}
		}
	}
	
	/**
	 * Updates the current state of the Seed of Destruction.<br>
	 * This method checks for the {@code ENERGY_SEEDS} quest.<br>
	 * It notifies the quest to stop the {@code StopSoDAi} event if it exists.
	 */
	public void updateSodState()
	{
		final Quest quest = ScriptManager.getInstance().getScript(ENERGY_SEEDS);
		if (quest == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": missing EnergySeeds Quest!");
		}
		else
		{
			quest.notifyEvent("StopSoDAi", null, null);
		}
	}
	
	/**
	 * Increments the count of Seed of Destruction Tiats killed.<br>
	 * Checks if the kill threshold from {@code GraciaSeedsConfig} is met.<br>
	 * Updates the current state and saves the data.<br>
	 * Notifies the {@code EnergySeeds} quest if it exists.
	 */
	public void increaseSoDTiatKilled()
	{
		if (_SoDState == 1)
		{
			_SoDTiatKilled++;
			if (_SoDTiatKilled >= GraciaSeedsConfig.SOD_TIAT_KILL_COUNT)
			{
				setSoDState(2, false);
			}
			
			saveData(SODTYPE);
			final Quest esQuest = ScriptManager.getInstance().getScript(ENERGY_SEEDS);
			if (esQuest == null)
			{
				LOGGER.warning(getClass().getSimpleName() + ": missing EnergySeeds Quest!");
			}
			else
			{
				esQuest.notifyEvent("StartSoDAi", null, null);
			}
		}
	}
	
	/**
	 * Retrieves the total number of {@code SoDTiat} killed.<br>
	 * This value is used to track progress in the Seed of Destruction event.
	 * @return The current count of {@code SoDTiat} killed.
	 */
	public int getSoDTiatKilled()
	{
		return _SoDTiatKilled;
	}
	
	/**
	 * Updates the current state of the Seed of Destruction.<br>
	 * This method also updates the last change timestamp and resets Tiat kills if the new state is {@code 1}.<br>
	 * It triggers a stage handling update and optionally saves the data to the database.
	 * @param value The new state integer to set for the Seed of Destruction.
	 * @param doSave A boolean flag indicating whether to save the change to the database.
	 */
	public void setSoDState(int value, boolean doSave)
	{
		LOGGER.info(getClass().getSimpleName() + ": New Seed of Destruction state -> " + value + ".");
		_SoDLastStateChangeDate.setTimeInMillis(System.currentTimeMillis());
		_SoDState = value;
		
		// reset number of Tiat kills
		if (_SoDState == 1)
		{
			_SoDTiatKilled = 0;
		}
		
		handleSodStages();
		
		if (doSave)
		{
			saveData(SODTYPE);
		}
	}
	
	/**
	 * Calculates the remaining time until the next Seed of Destruction state change.<br>
	 * It checks the current {@code _SoDState} to determine the countdown.<br>
	 * If the state is not currently active, it returns -1.
	 * @return The remaining time in milliseconds or -1 if no timer is active.
	 */
	public long getSoDTimeForNextStateChange()
	{
		switch (_SoDState)
		{
			case 1:
			{
				return -1;
			}
			case 2:
			{
				return ((_SoDLastStateChangeDate.getTimeInMillis() + GraciaSeedsConfig.SOD_STAGE_2_LENGTH) - System.currentTimeMillis());
			}
			case 3:
			{
				// Not implemented yet.
				return -1;
			}
			default:
			{
				// This should not happen!.
				return -1;
			}
		}
	}
	
	/**
	 * Retrieves the date when the Seed of Destruction last changed its state.<br>
	 * This method returns a {@code Calendar} object containing that timestamp.
	 * @return The {@code Calendar} object representing the last state change date.
	 */
	public Calendar getSoDLastStateChangeDate()
	{
		return _SoDLastStateChangeDate;
	}
	
	/**
	 * Retrieves the current state of the Seed of Destruction.<br>
	 * This value is used to determine which stage the event is in.
	 * @return The current {@code int} state of the Seed of Destruction.
	 */
	public int getSoDState()
	{
		return _SoDState;
	}
	
	/**
	 * Retrieves the singleton instance of {@code GraciaSeedsManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The active {@code GraciaSeedsManager} instance.
	 */
	public static GraciaSeedsManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GraciaSeedsManager INSTANCE = new GraciaSeedsManager();
	}
}
