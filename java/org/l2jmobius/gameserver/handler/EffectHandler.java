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

import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.scripting.ScriptEngine;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class manages the processing and application of various game effects.<br>
 * It handles how {@code AbstractEffect} instances interact with players and entities.
 * @author BiggBoss
 */
public class EffectHandler
{
	private final Map<String, Function<StatSet, AbstractEffect>> _effectHandlerFactories = new HashMap<>();
	
	/**
	 * Registers a new effect handler factory with a unique name.<br>
	 * This allows the system to create {@code AbstractEffect} objects based on a {@code StatSet}.<br>
	 * Use this method to map specific names to their corresponding logic.
	 * @param name The unique identifier for the handler.
	 * @param handlerFactory A function that takes a {@code StatSet} and returns an {@code AbstractEffect}.
	 */
	public void registerHandler(String name, Function<StatSet, AbstractEffect> handlerFactory)
	{
		_effectHandlerFactories.put(name, handlerFactory);
	}
	
	/**
	 * Retrieves a factory function for a specific effect handler.<br>
	 * This method looks up the handler based on the provided {@code name}.<br>
	 * It returns a {@code Function} that creates an {@code AbstractEffect} from a {@code StatSet}.
	 * @param name The unique identifier for the effect handler.
	 * @return A {@code Function} to create the effect, or {@code null} if no handler is found.
	 */
	public Function<StatSet, AbstractEffect> getHandlerFactory(String name)
	{
		return _effectHandlerFactories.get(name);
	}
	
	/**
	 * Returns the total number of registered effect handlers.<br>
	 * This count includes all entries in the internal map.
	 * @return The number of handlers currently stored in the {@code EffectHandler}.
	 */
	public int size()
	{
		return _effectHandlerFactories.size();
	}
	
	/**
	 * Runs the master script for effect handlers.<br>
	 * This method calls {@code ScriptEngine.getInstance().executeScript()}.<br>
	 * It uses the constant {@code ScriptEngine.EFFECT_MASTER_HANDLER_FILE}.<br>
	 * If an error occurs, it throws a new {@code Error}.
	 */
	public void executeScript()
	{
		try
		{
			ScriptEngine.getInstance().executeScript(ScriptEngine.EFFECT_MASTER_HANDLER_FILE);
		}
		catch (Exception e)
		{
			throw new Error("Problems while running EffectMasterHandler", e);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final EffectHandler INSTANCE = new EffectHandler();
	}
	
	/**
	 * Provides the global instance of the {@code EffectHandler}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the shared handler for all effects.
	 * @return The single instance of {@code EffectHandler}.
	 */
	public static EffectHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
}
