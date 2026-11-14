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

import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.scripting.ScriptEngine;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class manages the evaluation of conditions for skills.<br>
 * It processes {@code ISkillCondition} objects to determine if a skill can be executed.<br>
 * It acts as a central handler for checking various game mechanics and requirements.
 * @author NosBit
 */
public class SkillConditionHandler
{
	private final Map<String, Function<StatSet, ISkillCondition>> _skillConditionHandlerFactories = new HashMap<>();
	
	/**
	 * Registers a new factory for creating skill conditions.<br>
	 * This maps a unique name to a specific logic provider.<br>
	 * Use this to extend how the system handles different skills.
	 * @param name The unique identifier for the handler.
	 * @param handlerFactory A function that creates an {@code ISkillCondition} from a {@code StatSet}.
	 */
	public void registerHandler(String name, Function<StatSet, ISkillCondition> handlerFactory)
	{
		_skillConditionHandlerFactories.put(name, handlerFactory);
	}
	
	/**
	 * Retrieves a factory for creating a skill condition based on its name.<br>
	 * This method looks up the registered handler in the internal map.<br>
	 * It returns {@code null} if no handler is found for the given name.
	 * @param name The unique identifier of the skill condition handler.
	 * @return A {@code Function} that creates an {@code ISkillCondition}, or {@code null}.
	 */
	public Function<StatSet, ISkillCondition> getHandlerFactory(String name)
	{
		return _skillConditionHandlerFactories.get(name);
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count includes all entries in the internal map.
	 * @return The number of registered handlers as an {@code int}.
	 */
	public int size()
	{
		return _skillConditionHandlerFactories.size();
	}
	
	/**
	 * Runs the script for skill conditions.<br>
	 * This method calls {@code ScriptEngine.getInstance().executeScript()}.<br>
	 * It uses the constant {@code ScriptEngine.SKILL_CONDITION_HANDLER_FILE}.<br>
	 * If an error occurs, it throws a new {@code Error}.
	 */
	public void executeScript()
	{
		try
		{
			ScriptEngine.getInstance().executeScript(ScriptEngine.SKILL_CONDITION_HANDLER_FILE);
		}
		catch (Exception e)
		{
			throw new Error("Problems while running SkillMasterHandler", e);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final SkillConditionHandler INSTANCE = new SkillConditionHandler();
	}
	
	/**
	 * Gets the singleton instance of the {@code SkillConditionHandler}.<br>
	 * Use this method to access the global handler for skill conditions.
	 * @return The single instance of {@code SkillConditionHandler}.
	 */
	public static SkillConditionHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
}
