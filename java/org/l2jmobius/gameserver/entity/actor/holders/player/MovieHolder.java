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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.Movie;

/**
 * This class manages the collection of movies associated with a {@code Player}.<br>
 * It provides a way to track and access active movie data for the player entity.
 * @author St3eT
 */
public class MovieHolder
{
	private final Movie _movie;
	private final List<Player> _players;
	private final Collection<Player> _votedPlayers = ConcurrentHashMap.newKeySet();
	
	/**
	 * Creates a new {@code MovieHolder} instance.<br>
	 * This constructor initializes the list of {@code Player} objects and the {@code Movie}.<br>
	 * It also triggers the {@code playMovie} method for every player in the list.
	 * @param players The list of {@code Player} objects who will watch the movie.
	 * @param movie The {@code Movie} object to be played.
	 */
	public MovieHolder(List<Player> players, Movie movie)
	{
		_players = players;
		_movie = movie;
		_players.forEach(p -> p.playMovie(this));
	}
	
	/**
	 * Retrieves the {@code Movie} associated with this holder.<br>
	 * This method returns the current movie object.
	 * @return The {@code Movie} instance.
	 */
	public Movie getMovie()
	{
		return _movie;
	}
	
	/**
	 * Registers a vote for a player to escape the current movie.<br>
	 * This method checks if the {@code Player} is eligible to vote.<br>
	 * If enough players vote, the movie will stop for everyone.
	 * @param player The {@code Player} who is casting the vote.
	 */
	public void playerEscapeVote(Player player)
	{
		if (_votedPlayers.contains(player) || !_players.contains(player) || !_movie.isEscapable())
		{
			return;
		}
		
		_votedPlayers.add(player);
		
		if (((_votedPlayers.size() * 100) / _players.size()) >= 50)
		{
			_players.forEach(Player::stopMovie);
		}
	}
	
	/**
	 * Retrieves the list of players associated with this movie.<br>
	 * This method returns all {@code Player} objects currently in the holder.
	 * @return a {@code List} containing all {@code Player} objects.
	 */
	public List<Player> getPlayers()
	{
		return _players;
	}
	
	/**
	 * Retrieves the list of players who have voted. <br>
	 * This method returns a {@code Collection} of {@code Player} objects.
	 * @return A collection containing all players who cast a vote.
	 */
	public Collection<Player> getVotedPlayers()
	{
		return _votedPlayers;
	}
}
