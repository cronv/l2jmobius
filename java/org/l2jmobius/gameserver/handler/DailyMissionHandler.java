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
package org.l2jmobius.gameserver.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.scripting.ScriptEngine;

/**
 * This class manages the logic for daily missions within the game.<br>
 * It handles tracking progress and updating {@code DailyMissionDataHolder} records.
 * @author Sdw
 */
public class DailyMissionHandler
{
	private final Map<String, Function<DailyMissionDataHolder, AbstractDailyMissionHandler>> _handlerFactories = new HashMap<>();
	
	/**
	 * Registers a new mission handler factory with a specific name.<br>
	 * This allows the system to create handlers dynamically based on their unique keys.
	 * @param name The unique identifier for the handler.
	 * @param handlerFactory A function that creates an {@code AbstractDailyMissionHandler} from {@code DailyMissionDataHolder}.
	 */
	public void registerHandler(String name, Function<DailyMissionDataHolder, AbstractDailyMissionHandler> handlerFactory)
	{
		_handlerFactories.put(name, handlerFactory);
	}
	
	/**
	 * Retrieves a factory function for a specific daily mission.<br>
	 * This method looks up the handler based on the provided name.<br>
	 * It returns {@code null} if no handler is registered for that name.
	 * @param name The unique identifier of the daily mission.
	 * @return A {@code Function} that creates an {@code AbstractDailyMissionHandler}, or {@code null}.
	 */
	public Function<DailyMissionDataHolder, AbstractDailyMissionHandler> getHandler(String name)
	{
		return _handlerFactories.get(name);
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count includes all entries in the internal map.
	 * @return The number of active handlers.
	 */
	public int size()
	{
		return _handlerFactories.size();
	}
	
	/**
	 * Runs the daily mission reward script.<br>
	 * This method calls {@code ScriptEngine.getInstance().executeScript()}.<br>
	 * It uses the {@code ONE_DAY_REWARD_MASTER_HANDLER} constant.<br>
	 * If an error occurs, it throws a new {@code Error}.
	 */
	public void executeScript()
	{
		try
		{
			ScriptEngine.getInstance().executeScript(ScriptEngine.ONE_DAY_REWARD_MASTER_HANDLER);
		}
		catch (Exception e)
		{
			throw new Error("Problems while running DailyMissionMasterHandler", e);
		}
	}
	
	/**
	 * Provides access to the singleton instance of {@code DailyMissionHandler}.<br>
	 * Use this method to get the global handler for daily missions.
	 * @return The single instance of {@code DailyMissionHandler}.
	 */
	public static DailyMissionHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DailyMissionHandler INSTANCE = new DailyMissionHandler();
	}
}
