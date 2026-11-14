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
package org.l2jmobius.gameserver.mechanics.script.timers;

/**
 * Functional interface invoked when an event timer fires.
 * @author UnAfraid
 * @param <T> The event type.
 */
@FunctionalInterface
public interface IEventTimerEvent<T>
{
	/**
	 * Invoked when the timer fires.
	 * @param holder The {@code TimerHolder} of the fired timer.
	 */
	void onTimerEvent(TimerHolder<T> holder);
}
