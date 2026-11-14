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

import org.l2jmobius.gameserver.mechanics.conditions.ICondition;
import org.l2jmobius.gameserver.scripting.ScriptEngine;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class manages the processing and evaluation of game conditions.<br>
 * It handles how {@code ICondition} objects are applied to entities during gameplay.
 * @author Sdw
 */
public class ConditionHandler
{
	private final Map<String, Function<StatSet, ICondition>> _conditionHandlerFactories = new HashMap<>();
	
	/**
	 * Registers a new factory for creating conditions.<br>
	 * This maps a unique name to a specific logic provider.<br>
	 * Use this to define how {@code ICondition} objects are generated from a {@code StatSet}.
	 * @param name The unique identifier for the handler.
	 * @param handlerFactory A function that creates an {@code ICondition} based on a {@code StatSet}.
	 */
	public void registerHandler(String name, Function<StatSet, ICondition> handlerFactory)
	{
		_conditionHandlerFactories.put(name, handlerFactory);
	}
	
	/**
	 * Retrieves a factory for creating a specific condition.<br>
	 * This method looks up the handler by its registered name.
	 * @param name The unique identifier for the condition handler.
	 * @return A {@code Function} that creates an {@code ICondition}, or {@code null} if not found.
	 */
	public Function<StatSet, ICondition> getHandlerFactory(String name)
	{
		return _conditionHandlerFactories.get(name);
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count includes all entries in the internal map.
	 * @return The current size of the handler collection.
	 */
	public int size()
	{
		return _conditionHandlerFactories.size();
	}
	
	/**
	 * Runs the script associated with the condition handler.<br>
	 * This method calls {@code ScriptEngine.getInstance().executeScript()}.<br>
	 * It uses the constant {@code ScriptEngine.CONDITION_HANDLER_FILE}.<br>
	 * If an error occurs, it throws a new {@code Error}.
	 */
	public void executeScript()
	{
		try
		{
			ScriptEngine.getInstance().executeScript(ScriptEngine.CONDITION_HANDLER_FILE);
		}
		catch (Exception e)
		{
			throw new Error("Problems while running ConditionMasterHandler", e);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final ConditionHandler INSTANCE = new ConditionHandler();
	}
	
	/**
	 * Retrieves the singleton instance of the {@code ConditionHandler}.<br>
	 * Use this method to access the global handler for condition logic.
	 * @return The single shared instance of {@code ConditionHandler}.
	 */
	public static ConditionHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
}
