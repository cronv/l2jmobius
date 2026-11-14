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
package org.l2jmobius.gameserver.mechanics.stats.functions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class serves as a base template for defining various functions.<br>
 * It provides a structured way to handle logic within the stats system.
 * @author mkizub, Zoey76
 */
public class FuncTemplate
{
	private final Class<?> _functionClass;
	private final Condition _attachCond;
	private final Condition _applyCond;
	private final Stat _stat;
	private final int _order;
	private final double _value;
	
	/**
	 * Creates a new instance of a {@code FuncTemplate}.<br>
	 * This constructor initializes the function with specific conditions, stats, and values.<br>
	 * It also determines the correct class to load based on the provided name.
	 * @param attachCond The condition required to attach this template.
	 * @param applyCond The condition required to apply this template.
	 * @param functionName The name of the function used to identify the class.
	 * @param order The priority order for this function.
	 * @param stat The {@code Stat} type associated with this template.
	 * @param value The numerical value assigned to this template.
	 */
	public FuncTemplate(Condition attachCond, Condition applyCond, String functionName, int order, Stat stat, double value)
	{
		final StatFunction function = StatFunction.valueOf(functionName.toUpperCase());
		if (order >= 0)
		{
			_order = order;
		}
		else
		{
			_order = function.getOrder();
		}
		
		_attachCond = attachCond;
		_applyCond = applyCond;
		_stat = stat;
		_value = value;
		
		try
		{
			_functionClass = Class.forName("org.l2jmobius.gameserver.mechanics.stats.functions.Func" + function.getName());
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Retrieves the {@code Class} type of the function.<br>
	 * This is used to identify which logic should be executed.
	 * @return The {@code Class<?>} associated with this template.
	 */
	public Class<?> getFunctionClass()
	{
		return _functionClass;
	}
	
	/**
	 * Retrieves the {@code Stat} associated with this template.
	 * @return the {@code Stat} object.
	 */
	public Stat getStat()
	{
		return _stat;
	}
	
	/**
	 * Retrieves the execution order of this template.<br>
	 * This value determines the sequence in which functions are processed.
	 * @return The integer representing the order.
	 */
	public int getOrder()
	{
		return _order;
	}
	
	/**
	 * Retrieves the numerical value associated with this template.<br>
	 * This value is used during stat calculations.
	 * @return The {@code double} value of the function.
	 */
	public double getValue()
	{
		return _value;
	}
	
	/**
	 * Checks if the {@code Creature} and {@code Skill} meet specific requirements.<br>
	 * It validates both the attachment and application conditions.
	 * @param effected The {@code Creature} being affected by the skill.
	 * @param skill The {@code Skill} currently being used.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	public boolean meetCondition(Creature effected, Skill skill)
	{
		if ((_attachCond != null) && !_attachCond.test(effected, effected, skill))
		{
			return false;
		}
		
		if ((_applyCond != null) && !_applyCond.test(effected, effected, skill))
		{
			return false;
		}
		
		return true;
	}
}
