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
package org.l2jmobius.gameserver.entity.clan.entry;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.clan.Clan;

/**
 * This class holds the data required for a player to join a {@code Clan}.<br>
 * It stores information related to the recruitment process of a pledge.<br>
 * Use this object to manage requirements and status during clan entry.
 * @author Sdw
 */
public class PledgeRecruitInfo
{
	private int _clanId;
	private int _karma;
	private String _information;
	private String _detailedInformation;
	private final Clan _clan;
	private final int _applicationType;
	private final int _recruitType;
	
	/**
	 * Creates a new instance of {@code PledgeRecruitInfo}.<br>
	 * This constructor initializes the recruitment data for a specific clan.<br>
	 * It also fetches the corresponding {@code Clan} object from the database.
	 * @param clanId The unique identifier for the clan.
	 * @param karma The current karma value of the clan.
	 * @param information A brief description of the recruitment.
	 * @param detailedInformation A more in-depth explanation of the recruitment.
	 * @param applicationType The category type of the application.
	 * @param recruitType The specific type of recruitment being performed.
	 */
	public PledgeRecruitInfo(int clanId, int karma, String information, String detailedInformation, int applicationType, int recruitType)
	{
		_clanId = clanId;
		_karma = karma;
		_information = information;
		_detailedInformation = detailedInformation;
		_clan = ClanTable.getInstance().getClan(clanId);
		_applicationType = applicationType;
		_recruitType = recruitType;
	}
	
	/**
	 * Retrieves the unique identifier for the clan.<br>
	 * This value corresponds to the {@code _clanId} field.
	 * @return The {@code int} ID of the clan.
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * Sets the unique identifier for the clan.<br>
	 * This value is stored in the {@code _clanId} field.
	 * @param clanId The unique ID of the clan to assign.
	 */
	public void setClanId(int clanId)
	{
		_clanId = clanId;
	}
	
	/**
	 * Retrieves the name of the clan associated with this recruitment info.<br>
	 * This method calls {@code getName()} on the internal {@code Clan} object.
	 * @return The name of the clan as a {@code String}.
	 */
	public String getClanName()
	{
		return _clan.getName();
	}
	
	/**
	 * Retrieves the name of the leader for the associated clan.<br>
	 * This method fetches the data from the {@code Clan} object.
	 * @return The name of the clan leader as a {@code String}.
	 */
	public String getClanLeaderName()
	{
		return _clan.getLeaderName();
	}
	
	/**
	 * Retrieves the current level of the clan.<br>
	 * This value is fetched from the associated {@code Clan} object.
	 * @return The integer level of the clan.
	 */
	public int getClanLevel()
	{
		return _clan.getLevel();
	}
	
	/**
	 * Retrieves the karma value of the pledge recruit.<br>
	 * This value represents the current karma score associated with the recruitment info.
	 * @return The current {@code int} karma value.
	 */
	public int getKarma()
	{
		return _karma;
	}
	
	/**
	 * Updates the karma value for this recruitment info.<br>
	 * This method sets the internal {@code _karma} field.
	 * @param karma The new karma value to assign.
	 */
	public void setKarma(int karma)
	{
		_karma = karma;
	}
	
	/**
	 * Retrieves the basic information for this pledge recruitment.<br>
	 * This method returns the {@code _information} string field.
	 * @return The recruitment information as a {@code String}.
	 */
	public String getInformation()
	{
		return _information;
	}
	
	/**
	 * Updates the general recruitment information.<br>
	 * This method sets the {@code _information} field to a new value.
	 * @param information The new string to store as information.
	 */
	public void setInformation(String information)
	{
		_information = information;
	}
	
	/**
	 * Retrieves the full description of the pledge recruitment.<br>
	 * This method returns the {@code _detailedInformation} string.<br>
	 * It provides more context than the standard information field.
	 * @return The detailed recruitment information as a {@code String}.
	 */
	public String getDetailedInformation()
	{
		return _detailedInformation;
	}
	
	/**
	 * Updates the detailed information for a pledge recruitment.<br>
	 * This method sets the {@code _detailedInformation} field.
	 * @param detailedInformation The new string containing the details.
	 */
	public void setDetailedInformation(String detailedInformation)
	{
		_detailedInformation = detailedInformation;
	}
	
	/**
	 * Retrieves the type of application for this recruitment.<br>
	 * This value is stored as an {@code int}.
	 * @return The application type.
	 */
	public int getApplicationType()
	{
		return _applicationType;
	}
	
	/**
	 * Retrieves the recruitment type associated with this pledge.<br>
	 * This value identifies how the recruit was processed.
	 * @return the {@code int} value of the recruit type.
	 */
	public int getRecruitType()
	{
		return _recruitType;
	}
	
	/**
	 * Retrieves the {@code Clan} associated with this recruitment information.
	 * @return the {@code Clan} object.
	 */
	public Clan getClan()
	{
		return _clan;
	}
}
