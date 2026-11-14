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
package org.l2jmobius.gameserver.mechanics.conditions;

/**
 * This interface allows classes to listen for and process condition events.<br>
 * Classes implementing {@code ConditionListener} are registered via a component's {@code addConditionListener} method.<br>
 * The appropriate methods will be invoked automatically when a condition event occurs.
 * @author mkizub
 */
public interface ConditionListener
{
	/**
	 * Notify changed.
	 */
	void notifyChanged();
}
