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
package org.l2jmobius.gameserver.mechanics.events;

/**
 * This class handles events related to {@code Container} objects.<br>
 * It provides a centralized way to manage interactions with various types of containers in the game world.
 * @author UnAfraid
 */
public class Containers
{
	private static final ListenersContainer _globalContainer = new ListenersContainer();
	private static final ListenersContainer _globalNpcsContainer = new ListenersContainer();
	private static final ListenersContainer _globalMonstersContainer = new ListenersContainer();
	private static final ListenersContainer _globalPlayersContainer = new ListenersContainer();
	
	/**
	 * Protected constructor for the {@code Containers} class.<br>
	 * This prevents direct instantiation of this utility class.
	 */
	protected Containers()
	{
	}
	
	/**
	 * Provides access to the global listener container.<br>
	 * This container holds listeners that apply to all game entities.
	 * @return The {@code ListenersContainer} instance for global events.
	 */
	public static ListenersContainer Global()
	{
		return _globalContainer;
	}
	
	/**
	 * Retrieves the container for NPC related events.<br>
	 * This provides access to listeners specifically for non-player characters.
	 * @return The {@code ListenersContainer} for NPCs.
	 */
	public static ListenersContainer Npcs()
	{
		return _globalNpcsContainer;
	}
	
	/**
	 * Retrieves the global container for monster-related events.<br>
	 * This allows you to register listeners specifically for monsters.
	 * @return a {@code ListenersContainer} instance for monsters.
	 */
	public static ListenersContainer Monsters()
	{
		return _globalMonstersContainer;
	}
	
	/**
	 * Returns the container for player-related listeners.<br>
	 * This is used to manage events specifically for {@code Players}.
	 * @return a {@code ListenersContainer} containing all player event listeners.
	 */
	public static ListenersContainer Players()
	{
		return _globalPlayersContainer;
	}
}
