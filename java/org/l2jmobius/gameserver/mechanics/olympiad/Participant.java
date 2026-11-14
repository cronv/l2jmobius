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
package org.l2jmobius.gameserver.mechanics.olympiad;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a player participating in an Olympiad event.<br>
 * This class manages the state and statistics of a {@code Player} during competition.
 * @author DS, Zoey76
 */
public class Participant
{
	private final int _objectId;
	private Player _player;
	private final String _name;
	private final String _className;
	private final int _side;
	private final int _baseClass;
	private boolean _disconnected = false;
	private boolean _defaulted = false;
	private final StatSet _stats;
	private final int _level;
	private final String _clanName;
	private final int _clanId;
	
	/**
	 * Creates a new {@code Participant} instance for an olympiad.<br>
	 * This constructor initializes the participant using data from a {@code Player}.<br>
	 * It sets up the required stats and clan information.
	 * @param plr The {@code Player} object to associate with this participant.
	 * @param olympiadSide The side of the olympiad assigned to this participant.
	 */
	public Participant(Player plr, int olympiadSide)
	{
		_objectId = plr.getObjectId();
		_player = plr;
		_name = plr.getName();
		_className = ClassListData.getInstance().getClass(plr.getPlayerClass()).getClassName();
		_side = olympiadSide;
		_baseClass = plr.getBaseClass();
		_stats = Olympiad.getNobleStats(_objectId);
		_clanName = plr.getClan() != null ? plr.getClan().getName() : "";
		_clanId = plr.getClanId();
		_level = plr.getLevel();
	}
	
	/**
	 * Creates a new {@code Participant} instance using an object identifier.<br>
	 * This constructor initializes the participant with default values.
	 * @param objId The unique identifier for the object.
	 * @param olympiadSide The side assigned to the participant in the olympiad.
	 */
	public Participant(int objId, int olympiadSide)
	{
		_objectId = objId;
		_player = null;
		_name = "-";
		_className = "-";
		_side = olympiadSide;
		_baseClass = 0;
		_stats = null;
		_clanName = "";
		_clanId = 0;
		_level = 0;
	}
	
	/**
	 * Retrieves the current level of the participant.<br>
	 * This value is stored as an {@code int}.
	 * @return The level of the participant.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Refreshes the {@code Player} object associated with this participant.<br>
	 * It checks if the current player is offline or {@code null}.<br>
	 * If so, it attempts to fetch the player from {@code World} using the object ID.
	 * @return {@code true} if a valid {@code Player} was found, otherwise {@code false}.
	 */
	public boolean updatePlayer()
	{
		if ((_player == null) || !_player.isOnline())
		{
			_player = World.getPlayer(getObjectId());
		}
		
		return (_player != null);
	}
	
	/**
	 * Updates a specific statistic for the participant.<br>
	 * This method adds the {@code increment} value to the existing stat.<br>
	 * The resulting value will never be less than {@code 0}.
	 * @param statName The name of the statistic to update.
	 * @param increment The amount to add to the current statistic.
	 */
	public void updateStat(String statName, int increment)
	{
		_stats.set(statName, Math.max(_stats.getInt(statName) + increment, 0));
	}
	
	/**
	 * Retrieves the name of the participant.<br>
	 * If names are hidden in the configuration, it returns the class name instead.
	 * @return The name or class name of the participant.
	 */
	public String getName()
	{
		if (OlympiadConfig.OLYMPIAD_HIDE_NAMES)
		{
			return _className;
		}
		
		return _name;
	}
	
	/**
	 * Retrieves the name of the player's clan.<br>
	 * This method returns the {@code clanName} field.
	 * @return The name of the clan as a {@code String}.
	 */
	public String getClanName()
	{
		return _clanName;
	}
	
	/**
	 * Retrieves the unique identifier for the participant's clan.<br>
	 * This value corresponds to the {@code clanId} field.
	 * @return The integer ID of the clan.
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this participant.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the unique identifier for this participant.<br>
	 * This ID is used to distinguish between different participants in an olympiad.
	 * @return The {@code int} value of the object ID.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Retrieves the statistics for this participant.<br>
	 * This method returns the current {@code StatSet} object.
	 * @return the {@code StatSet} containing the participant's stats.
	 */
	public StatSet getStats()
	{
		return _stats;
	}
	
	/**
	 * Sets the {@code Player} associated with this participant.<br>
	 * This method updates the internal player reference to the provided {@code noble} object.
	 * @param noble The {@code Player} instance to assign to this participant.
	 */
	public void setPlayer(Player noble)
	{
		_player = noble;
	}
	
	/**
	 * Retrieves the team side of the participant.<br>
	 * This identifies which group the player belongs to in an olympiad.
	 * @return The integer value representing the side.
	 */
	public int getSide()
	{
		return _side;
	}
	
	/**
	 * Retrieves the base class of the participant.<br>
	 * This value represents the primary character type.
	 * @return The {@code int} value of the base class.
	 */
	public int getBaseClass()
	{
		return _baseClass;
	}
	
	/**
	 * Checks if the participant has been disconnected.<br>
	 * This method returns the current status of the {@code disconnected} flag.
	 * @return {@code true} if the participant is disconnected, {@code false} otherwise.
	 */
	public boolean isDisconnected()
	{
		return _disconnected;
	}
	
	/**
	 * Updates the connection status of the participant.<br>
	 * Sets the {@code disconnected} flag to the specified value.
	 * @param value The new connection status to set. Use {@code true} if the player is disconnected and {@code false} otherwise.
	 */
	public void setDisconnected(boolean value)
	{
		_disconnected = value;
	}
	
	/**
	 * Checks if the participant has been defaulted.<br>
	 * This method returns {@code true} if the {@code defaulted} flag is set.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if defaulted, {@code false} otherwise.
	 */
	public boolean isDefaulted()
	{
		return _defaulted;
	}
	
	/**
	 * Sets whether the participant has defaulted.<br>
	 * This updates the {@code defaulted} status of the {@code Participant}.
	 * @param value The new status to set. Use {@code true} if they have defaulted, or {@code false} otherwise.
	 */
	public void setDefaulted(boolean value)
	{
		_defaulted = value;
	}
}
