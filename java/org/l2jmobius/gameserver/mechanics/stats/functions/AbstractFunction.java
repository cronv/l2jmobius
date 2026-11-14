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

import java.util.logging.Logger;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * Represents a mathematical component used by a {@code Calculator} to dynamically compute character properties.<br>
 * Each instance defines a specific calculation logic that is executed based on its priority order.<br>
 * This allows for modular and extensible stat calculations such as maximum health or attack accuracy.
 * @author Zoey76
 */
public abstract class AbstractFunction
{
	/** Logger. */
	protected static final Logger LOG = Logger.getLogger(AbstractFunction.class.getName());
	/** Statistics, that is affected by this function (See Creature.CALCULATOR_XXX constants) */
	private final Stat _stat;
	/**
	 * Order of functions calculation.<br>
	 * Functions with lower order are executed first.<br>
	 * Functions with the same order are executed in unspecified order.<br>
	 * Usually add/subtract functions has lowest order,<br>
	 * then bonus/penalty functions (multiply/divide) are applied, then functions that do more complex<br>
	 * calculations (non-linear functions).
	 */
	private final int _order;
	/**
	 * Owner can be an armor, weapon, skill, system event, quest, etc.<br>
	 * Used to remove all functions added by this owner.
	 */
	private final Object _funcOwner;
	/** Function may be disabled by attached condition. */
	private final Condition _applyCond;
	/** The value. */
	private final double _value;
	
	/**
	 * Creates a new instance of an {@code AbstractFunction}.<br>
	 * This constructor initializes the basic properties for a stat calculation.
	 * @param stat The {@code Stat} type that this function affects.
	 * @param order The priority level for calculation execution.
	 * @param owner The object that owns this specific function.
	 * @param value The numerical value associated with the function.
	 * @param applyCond The {@code Condition} required to activate this function.
	 */
	public AbstractFunction(Stat stat, int order, Object owner, double value, Condition applyCond)
	{
		_stat = stat;
		_order = order;
		_funcOwner = owner;
		_value = value;
		_applyCond = applyCond;
	}
	
	/**
	 * Retrieves the condition required to apply this function.<br>
	 * This is used to check if the effect should be active.
	 * @return the {@code Condition} associated with this function.
	 */
	public Condition getApplyCond()
	{
		return _applyCond;
	}
	
	/**
	 * Retrieves the owner of this function.<br>
	 * The owner can be an armor, weapon, skill, or other object.<br>
	 * This is used to identify which source added the function.
	 * @return the {@code Object} that owns this function.
	 */
	public Object getFuncOwner()
	{
		return _funcOwner;
	}
	
	/**
	 * Retrieves the calculation priority of this function.<br>
	 * Functions with a lower {@code int} value are executed first.
	 * @return The order value of the function.
	 */
	public int getOrder()
	{
		return _order;
	}
	
	/**
	 * Retrieves the {@code Stat} associated with this function.<br>
	 * This identifies which character property is being modified.
	 * @return the {@code Stat} object.
	 */
	public Stat getStat()
	{
		return _stat;
	}
	
	/**
	 * Retrieves the numerical value of this function.<br>
	 * This value is used during the calculation process.
	 * @return The current {@code double} value.
	 */
	public double getValue()
	{
		return _value;
	}
	
	/**
	 * Run the mathematics function of the Func.
	 * @param effector the effector
	 * @param effected the effected
	 * @param skill the skill
	 * @param initVal the initial value
	 * @return the calculated value
	 */
	public abstract double calc(Creature effector, Creature effected, Skill skill, double initVal);
}
