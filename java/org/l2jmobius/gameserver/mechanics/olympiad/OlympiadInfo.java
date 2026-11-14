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

/**
 * This class holds the configuration and metadata for {@code Olympiad} events.<br>
 * It provides easy access to global settings such as duration, rewards, and requirements.
 * @author JIV
 */
public class OlympiadInfo
{
	private final String _name;
	private final String _clan;
	private final int _clanId;
	private final int _classId;
	private final int _dmg;
	private final int _curPoints;
	private final int _diffPoints;
	
	/**
	 * Creates a new {@code OlympiadInfo} object.<br>
	 * This constructor initializes all fields for an olympiad participant.
	 * @param name The name of the player.
	 * @param clan The name of the player's clan.
	 * @param clanId The unique identifier for the clan.
	 * @param classId The unique identifier for the character class.
	 * @param dmg The damage value of the participant.
	 * @param curPoints The current points earned by the participant.
	 * @param diffPoints The difference in points.
	 */
	public OlympiadInfo(String name, String clan, int clanId, int classId, int dmg, int curPoints, int diffPoints)
	{
		_name = name;
		_clan = clan;
		_clanId = clanId;
		_classId = classId;
		_dmg = dmg;
		_curPoints = curPoints;
		_diffPoints = diffPoints;
	}
	
	/**
	 * Retrieves the name of the olympiad.<br>
	 * This method returns the {@code String} value stored in the private field.
	 * @return The name of the olympiad.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the name of the clan.<br>
	 * This method returns the {@code String} value stored in the private field.
	 * @return The name of the clan as a {@code String}.
	 */
	public String getClanName()
	{
		return _clan;
	}
	
	/**
	 * Retrieves the unique identifier for the clan.<br>
	 * This value is used to identify which clan a player belongs to.
	 * @return The {@code int} ID of the clan.
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * Retrieves the unique identifier for the class. <br>
	 * This value is stored in the {@code _classId} field.
	 * @return The integer ID of the class.
	 */
	public int getClassId()
	{
		return _classId;
	}
	
	/**
	 * Retrieves the damage value for this {@code OlympiadInfo}.<br>
	 * This value is stored in the internal field {@code _dmg}.
	 * @return The total damage as an {@code int}.
	 */
	public int getDamage()
	{
		return _dmg;
	}
	
	/**
	 * Retrieves the current points for the olympiad.<br>
	 * This value is stored in the {@code _curPoints} field.
	 * @return The current number of points as an {@code int}.
	 */
	public int getCurrentPoints()
	{
		return _curPoints;
	}
	
	/**
	 * Retrieves the difference points for this olympiad entry.<br>
	 * This value represents the point gap calculated during the event.
	 * @return The current {@code int} value of difference points.
	 */
	public int getDiffPoints()
	{
		return _diffPoints;
	}
}
