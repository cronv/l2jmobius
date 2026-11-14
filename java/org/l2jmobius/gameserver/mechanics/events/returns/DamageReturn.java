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
package org.l2jmobius.gameserver.mechanics.events.returns;

/**
 * This class handles the logic for damage reflection mechanics.<br>
 * It determines how much damage is returned to an attacker when a target is hit.
 * @author Sdw
 */
public class DamageReturn extends TerminateReturn
{
	private final double _damage;
	
	/**
	 * Creates a new {@code DamageReturn} instance.<br>
	 * This constructor initializes the return state and sets the damage amount.
	 * @param terminate Determines if the action should be terminated.
	 * @param override Determines if this return overrides previous ones.
	 * @param abort Determines if the current action should be aborted.
	 * @param damage The amount of damage to be returned.
	 */
	public DamageReturn(boolean terminate, boolean override, boolean abort, double damage)
	{
		super(terminate, override, abort);
		_damage = damage;
	}
	
	/**
	 * Retrieves the amount of damage associated with this event.<br>
	 * This value is stored when the {@code DamageReturn} object is created.
	 * @return The total damage as a {@code double}.
	 */
	public double getDamage()
	{
		return _damage;
	}
}
