/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.entity.clan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;

/**
 * This class represents an individual member within a {@code Clan}.<br>
 * It stores all relevant data and properties associated with that specific membership.
 */
public class ClanMember
{
	private static final Logger LOGGER = Logger.getLogger(ClanMember.class.getName());
	
	private final Clan _clan;
	private int _objectId;
	private String _name;
	private String _title;
	private int _powerGrade;
	private int _level;
	private int _classId;
	private boolean _sex;
	private int _raceOrdinal;
	private Player _player;
	private int _pledgeType;
	private int _apprentice;
	private int _sponsor;
	private int _clanContribution = -1;
	private int _clanContributionPrevious = -1;
	private int _clanContributionTotal = -1;
	private int _clanContributionTotalPrevious = -1;
	
	/**
	 * Creates a new {@code ClanMember} instance using data from a database result set.<br>
	 * This constructor populates the member's attributes like name, level, and power grade.
	 * @param clan The {@code Clan} object that this member belongs to.
	 * @param clanMember The {@code ResultSet} containing the member's database information.
	 * @throws SQLException If a database error occurs while reading the result set.
	 */
	public ClanMember(Clan clan, ResultSet clanMember) throws SQLException
	{
		if (clan == null)
		{
			throw new IllegalArgumentException("Cannot create a Clan Member with a null clan.");
		}
		
		_clan = clan;
		_name = clanMember.getString("char_name");
		_level = clanMember.getInt("level");
		_classId = clanMember.getInt("classid");
		_objectId = clanMember.getInt("charId");
		_pledgeType = clanMember.getInt("subpledge");
		_title = clanMember.getString("title");
		_powerGrade = clanMember.getInt("power_grade");
		_apprentice = clanMember.getInt("apprentice");
		_sponsor = clanMember.getInt("sponsor");
		_sex = clanMember.getInt("sex") != 0;
		_raceOrdinal = clanMember.getInt("race");
	}
	
	/**
	 * Creates a new {@code ClanMember} instance.<br>
	 * This constructor initializes the member data using information from a {@code Player}.<br>
	 * It sets properties like name, level, and class ID based on the provided {@code Player} object.
	 * @param clan The {@code Clan} that this member belongs to.
	 * @param player The {@code Player} whose data will be used to populate the member details.
	 */
	public ClanMember(Clan clan, Player player)
	{
		if (clan == null)
		{
			throw new IllegalArgumentException("Cannot create a Clan Member if player has a null clan.");
		}
		
		_player = player;
		_clan = clan;
		_name = player.getName();
		_level = player.getLevel();
		_classId = player.getPlayerClass().getId();
		_objectId = player.getObjectId();
		_pledgeType = player.getPledgeType();
		_powerGrade = player.getPowerGrade();
		_title = player.getTitle();
		_sponsor = 0;
		_apprentice = 0;
		_sex = player.getAppearance().isFemale();
		_raceOrdinal = player.getRace().ordinal();
	}
	
	/**
	 * Sets the {@code Player} associated with this clan member.<br>
	 * If the new {@code Player} is {@code null}, it saves the current player data.<br>
	 * If a valid {@code Player} is provided, it updates clan skills and leader status.
	 * @param player the {@code Player} to set for this member
	 */
	public void setPlayer(Player player)
	{
		if ((player == null) && (_player != null))
		{
			// This is here to keep the data when the player logs off.
			_name = _player.getName();
			_level = _player.getLevel();
			_classId = _player.getPlayerClass().getId();
			_objectId = _player.getObjectId();
			_powerGrade = _player.getPowerGrade();
			_pledgeType = _player.getPledgeType();
			_title = _player.getTitle();
			_apprentice = _player.getApprentice();
			_sponsor = _player.getSponsor();
			_sex = _player.getAppearance().isFemale();
			_raceOrdinal = _player.getRace().ordinal();
		}
		
		if (player != null)
		{
			_clan.addSkillEffects(player);
			final boolean isClanLeader = player.isClanLeader();
			if ((_clan.getLevel() >= 5) && isClanLeader)
			{
				SiegeManager.getInstance().addSiegeSkills(player);
			}
			
			if (isClanLeader)
			{
				_clan.setLeader(this);
			}
		}
		
		_player = player;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this clan member.
	 * @return the {@code Player} object or {@code null} if no player is assigned.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Checks if the clan member is currently online.<br>
	 * This method verifies that the associated {@code Player} is active and connected.
	 * @return {@code true} if the player is online and not detached, {@code false} otherwise.
	 */
	public boolean isOnline()
	{
		if ((_player == null) || !_player.isOnline())
		{
			return false;
		}
		
		if ((_player.getClient() == null) || _player.getClient().isDetached())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Retrieves the class identifier for this clan member.<br>
	 * It returns the ID from the {@code Player} object if it exists.<br>
	 * Otherwise, it returns the stored {@code _classId}.
	 * @return the integer ID of the character's class.
	 */
	public int getClassId()
	{
		return _player != null ? _player.getPlayerClass().getId() : _classId;
	}
	
	/**
	 * Retrieves the current level of the clan member.<br>
	 * It returns the level from the {@code Player} object if it is online.<br>
	 * Otherwise, it returns the stored level value.
	 * @return the current level as an {@code int}.
	 */
	public int getLevel()
	{
		return _player != null ? _player.getLevel() : _level;
	}
	
	/**
	 * Retrieves the name of the clan member.<br>
	 * It returns the name from the {@code Player} object if it exists.<br>
	 * Otherwise, it returns the stored {@code _name} field.
	 * @return The name of the clan member as a {@code String}.
	 */
	public String getName()
	{
		return _player != null ? _player.getName() : _name;
	}
	
	/**
	 * Retrieves the unique identifier for this clan member.<br>
	 * It returns the {@code Player} object ID if a player is linked.<br>
	 * Otherwise, it returns the internal {@code _objectId}.
	 * @return the unique integer ID of the member.
	 */
	public int getObjectId()
	{
		return _player != null ? _player.getObjectId() : _objectId;
	}
	
	/**
	 * Retrieves the title of the clan member.<br>
	 * It returns the title from the {@code Player} object if it exists.<br>
	 * Otherwise, it returns the stored {@code _title}.
	 * @return the title as a {@code String}.
	 */
	public String getTitle()
	{
		return _player != null ? _player.getTitle() : _title;
	}
	
	/**
	 * Retrieves the pledge type for this clan member.<br>
	 * It returns the value from the {@code Player} object if it exists.<br>
	 * Otherwise, it returns the local {@code _pledgeType} value.
	 * @return the current pledge type as an {@code int}.
	 */
	public int getPledgeType()
	{
		return _player != null ? _player.getPledgeType() : _pledgeType;
	}
	
	/**
	 * Sets the pledge type for this clan member.<br>
	 * This method updates the internal value and syncs it with the {@code Player} object if they are online.<br>
	 * If the player is offline, it triggers a database update.
	 * @param pledgeType The new pledge type to assign.
	 */
	public void setPledgeType(int pledgeType)
	{
		_pledgeType = pledgeType;
		if (_player != null)
		{
			_player.setPledgeType(pledgeType);
		}
		else
		{
			// Db save if char not logged in.
			updatePledgeType();
		}
	}
	
	/**
	 * Updates the {@code subpledge} value in the database.<br>
	 * This method saves the current {@code _pledgeType} for the character.<br>
	 * It uses the unique object ID to identify the correct record.
	 */
	public void updatePledgeType()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET subpledge=? WHERE charId=?"))
		{
			ps.setLong(1, _pledgeType);
			ps.setInt(2, getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not update pledge type: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the power grade of the clan member.<br>
	 * It returns the value from the {@code Player} object if it is not {@code null}.<br>
	 * Otherwise, it returns the local {@code _powerGrade} value.
	 * @return the current power grade as an {@code int}.
	 */
	public int getPowerGrade()
	{
		return _player != null ? _player.getPowerGrade() : _powerGrade;
	}
	
	/**
	 * Sets the power grade for this clan member.<br>
	 * This method updates the internal value and syncs it with the {@code Player} object if they are online.<br>
	 * If the player is offline, it triggers a database update.
	 * @param powerGrade The new power grade to assign.
	 */
	public void setPowerGrade(int powerGrade)
	{
		_powerGrade = powerGrade;
		if (_player != null)
		{
			_player.setPowerGrade(powerGrade);
		}
		else
		{
			// Db save if char not logged in.
			updatePowerGrade();
		}
	}
	
	/**
	 * Updates the {@code power_grade} in the database.<br>
	 * This method saves the current value of {@code _powerGrade} for the character.<br>
	 * It uses the unique identifier provided by {@code getObjectId()}.
	 */
	public void updatePowerGrade()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET power_grade=? WHERE charId=?"))
		{
			ps.setLong(1, _powerGrade);
			ps.setInt(2, getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not update power _grade: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Sets the IDs for both the apprentice and the sponsor.<br>
	 * This method updates the internal fields for these roles.
	 * @param apprenticeID The unique identifier for the apprentice.
	 * @param sponsorID The unique identifier for the sponsor.
	 */
	public void setApprenticeAndSponsor(int apprenticeID, int sponsorID)
	{
		_apprentice = apprenticeID;
		_sponsor = sponsorID;
	}
	
	/**
	 * Retrieves the ordinal value of the player's race.<br>
	 * It returns the ordinal from the {@code Player} object if it exists.<br>
	 * Otherwise, it returns the stored {@code _raceOrdinal} value.
	 * @return the integer ordinal of the race.
	 */
	public int getRaceOrdinal()
	{
		return _player != null ? _player.getRace().ordinal() : _raceOrdinal;
	}
	
	/**
	 * Checks if the clan member is female.<br>
	 * This method returns {@code true} if the player is female.<br>
	 * It falls back to the internal {@code _sex} value if the player is null.
	 * @return {@code true} if female, {@code false} otherwise.
	 */
	public boolean getSex()
	{
		return _player != null ? _player.getAppearance().isFemale() : _sex;
	}
	
	/**
	 * Retrieves the sponsor ID for this clan member.<br>
	 * It returns the value from the {@code Player} object if it exists.<br>
	 * Otherwise, it returns the local {@code _sponsor} value.
	 * @return the sponsor ID as an {@code int}.
	 */
	public int getSponsor()
	{
		return _player != null ? _player.getSponsor() : _sponsor;
	}
	
	/**
	 * Retrieves the apprentice ID for this clan member.<br>
	 * It returns the value from the {@code Player} object if it exists.<br>
	 * Otherwise, it returns the stored {@code _apprentice} value.
	 * @return the apprentice ID as an {@code int}.
	 */
	public int getApprentice()
	{
		return _player != null ? _player.getApprentice() : _apprentice;
	}
	
	/**
	 * Retrieves the name of the apprentice or the sponsor.<br>
	 * It checks for an apprentice first, then looks for a sponsor.<br>
	 * If neither is found, it returns an empty string.
	 * @return The name of the related clan member or {@code "Error"} if not found.
	 */
	public String getApprenticeOrSponsorName()
	{
		if (_player != null)
		{
			_apprentice = _player.getApprentice();
			_sponsor = _player.getSponsor();
		}
		
		if (_apprentice != 0)
		{
			final ClanMember apprentice = _clan.getClanMember(_apprentice);
			if (apprentice != null)
			{
				return apprentice.getName();
			}
			
			return "Error";
		}
		
		if (_sponsor != 0)
		{
			final ClanMember sponsor = _clan.getClanMember(_sponsor);
			if (sponsor != null)
			{
				return sponsor.getName();
			}
			
			return "Error";
		}
		
		return "";
	}
	
	/**
	 * Retrieves the {@code Clan} associated with this member.
	 * @return the {@code Clan} object.
	 */
	public Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * Calculates the pledge class for a given {@code Player}.<br>
	 * This method determines the rank based on clan level, leadership status, and pledge type.<br>
	 * It also applies special overrides for noble or hero statuses.
	 * @param player The {@code Player} to calculate the pledge class for.
	 * @return The calculated pledge class as an {@code int}.
	 */
	public static int calculatePledgeClass(Player player)
	{
		int pledgeClass = 0;
		if (player == null)
		{
			return pledgeClass;
		}
		
		final Clan clan = player.getClan();
		if (clan != null)
		{
			switch (clan.getLevel())
			{
				case 4:
				{
					if (player.isClanLeader())
					{
						pledgeClass = 3;
					}
					break;
				}
				case 5:
				{
					if (player.isClanLeader())
					{
						pledgeClass = 4;
					}
					else
					{
						pledgeClass = 2;
					}
					break;
				}
				case 6:
				{
					switch (player.getPledgeType())
					{
						case -1:
						{
							pledgeClass = 1;
							break;
						}
						case 100:
						case 200:
						{
							pledgeClass = 2;
							break;
						}
						case 0:
						{
							if (player.isClanLeader())
							{
								pledgeClass = 5;
							}
							else
							{
								switch (clan.getLeaderSubPledge(player.getObjectId()))
								{
									case 100:
									case 200:
									{
										pledgeClass = 4;
										break;
									}
									case -1:
									default:
									{
										pledgeClass = 3;
										break;
									}
								}
							}
							break;
						}
					}
					break;
				}
				case 7:
				{
					switch (player.getPledgeType())
					{
						case -1:
						{
							pledgeClass = 1;
							break;
						}
						case 100:
						case 200:
						{
							pledgeClass = 3;
							break;
						}
						case 1001:
						case 1002:
						case 2001:
						case 2002:
						{
							pledgeClass = 2;
							break;
						}
						case 0:
						{
							if (player.isClanLeader())
							{
								pledgeClass = 7;
							}
							else
							{
								switch (clan.getLeaderSubPledge(player.getObjectId()))
								{
									case 100:
									case 200:
									{
										pledgeClass = 6;
										break;
									}
									case 1001:
									case 1002:
									case 2001:
									case 2002:
									{
										pledgeClass = 5;
										break;
									}
									case -1:
									default:
									{
										pledgeClass = 4;
										break;
									}
								}
							}
							break;
						}
					}
					break;
				}
				case 8:
				{
					switch (player.getPledgeType())
					{
						case -1:
						{
							pledgeClass = 1;
							break;
						}
						case 100:
						case 200:
						{
							pledgeClass = 4;
							break;
						}
						case 1001:
						case 1002:
						case 2001:
						case 2002:
						{
							pledgeClass = 3;
							break;
						}
						case 0:
						{
							if (player.isClanLeader())
							{
								pledgeClass = 8;
							}
							else
							{
								switch (clan.getLeaderSubPledge(player.getObjectId()))
								{
									case 100:
									case 200:
									{
										pledgeClass = 7;
										break;
									}
									case 1001:
									case 1002:
									case 2001:
									case 2002:
									{
										pledgeClass = 6;
										break;
									}
									case -1:
									default:
									{
										pledgeClass = 5;
										break;
									}
								}
							}
							break;
						}
					}
					break;
				}
				case 9:
				{
					switch (player.getPledgeType())
					{
						case -1:
						{
							pledgeClass = 1;
							break;
						}
						case 100:
						case 200:
						{
							pledgeClass = 5;
							break;
						}
						case 1001:
						case 1002:
						case 2001:
						case 2002:
						{
							pledgeClass = 4;
							break;
						}
						case 0:
						{
							if (player.isClanLeader())
							{
								pledgeClass = 9;
							}
							else
							{
								switch (clan.getLeaderSubPledge(player.getObjectId()))
								{
									case 100:
									case 200:
									{
										pledgeClass = 8;
										break;
									}
									case 1001:
									case 1002:
									case 2001:
									case 2002:
									{
										pledgeClass = 7;
										break;
									}
									case -1:
									default:
									{
										pledgeClass = 6;
										break;
									}
								}
							}
							break;
						}
					}
					break;
				}
				case 10:
				{
					switch (player.getPledgeType())
					{
						case -1:
						{
							pledgeClass = 1;
							break;
						}
						case 100:
						case 200:
						{
							pledgeClass = 6;
							break;
						}
						case 1001:
						case 1002:
						case 2001:
						case 2002:
						{
							pledgeClass = 5;
							break;
						}
						case 0:
						{
							if (player.isClanLeader())
							{
								pledgeClass = 10;
							}
							else
							{
								switch (clan.getLeaderSubPledge(player.getObjectId()))
								{
									case 100:
									case 200:
									{
										pledgeClass = 9;
										break;
									}
									case 1001:
									case 1002:
									case 2001:
									case 2002:
									{
										pledgeClass = 8;
										break;
									}
									case -1:
									default:
									{
										pledgeClass = 7;
										break;
									}
								}
							}
							break;
						}
					}
					break;
				}
				case 11:
				case 12:
				case 13:
				case 14:
				case 15:
				{
					switch (player.getPledgeType())
					{
						case -1:
						{
							pledgeClass = 1;
							break;
						}
						case 100:
						case 200:
						{
							pledgeClass = 7;
							break;
						}
						case 1001:
						case 1002:
						case 2001:
						case 2002:
						{
							pledgeClass = 6;
							break;
						}
						case 0:
						{
							if (player.isClanLeader())
							{
								pledgeClass = 11;
							}
							else
							{
								switch (clan.getLeaderSubPledge(player.getObjectId()))
								{
									case 100:
									case 200:
									{
										pledgeClass = 10;
										break;
									}
									case 1001:
									case 1002:
									case 2001:
									case 2002:
									{
										pledgeClass = 9;
										break;
									}
									case -1:
									default:
									{
										pledgeClass = 8;
										break;
									}
								}
							}
							break;
						}
					}
					break;
				}
				default:
				{
					pledgeClass = 1;
					break;
				}
			}
		}
		
		if ((player.getNobleLevel() > 0) && (pledgeClass < 5))
		{
			pledgeClass = 5;
		}
		
		if (player.isHero() && (pledgeClass < 8))
		{
			pledgeClass = 8;
		}
		
		return pledgeClass;
	}
	
	/**
	 * Saves the apprentice and sponsor IDs to the database.<br>
	 * This method updates the character record for this member.
	 * @param apprentice The ID of the apprentice.
	 * @param sponsor The ID of the sponsor.
	 */
	public void saveApprenticeAndSponsor(int apprentice, int sponsor)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET apprentice=?,sponsor=? WHERE charId=?"))
		{
			ps.setInt(1, apprentice);
			ps.setInt(2, sponsor);
			ps.setInt(3, getObjectId());
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Could not save apprentice/sponsor: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Updates the clan contribution points for this member.<br>
	 * The new value is stored in the {@code _clanContribution} field.
	 * @param value The new contribution amount to set.
	 */
	public void setClanContribution(int value)
	{
		_clanContribution = value;
	}
	
	/**
	 * Retrieves the clan contribution value for this member.<br>
	 * It fetches the value from the database if it has not been loaded yet.<br>
	 * If no value is found in the database, it returns 0.
	 * @return the current clan contribution as an {@code int}.
	 */
	public int getClanContribution()
	{
		if (_clanContribution == -1)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT * FROM character_variables WHERE charId=? AND var=?"))
			{
				ps.setInt(1, _objectId);
				ps.setString(2, PlayerVariables.CLAN_CONTRIBUTION);
				try (ResultSet result = ps.executeQuery())
				{
					if (result.next())
					{
						_clanContribution = result.getInt("val");
					}
				}
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, "Could not load Clan Contribution: " + e.getMessage(), e);
			}
			
			// Avoid further queries.
			if (_clanContribution == -1)
			{
				_clanContribution = 0;
			}
		}
		
		return _clanContribution;
	}
	
	/**
	 * Retrieves the previous clan contribution value for this member.<br>
	 * It loads the value from the database if it is not already set.<br>
	 * If no value is found in the database, it returns 0.
	 * @return the previous clan contribution as an {@code int}.
	 */
	public int getClanContributionPrevious()
	{
		if (_clanContributionPrevious == -1)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT * FROM character_variables WHERE charId=? AND var=?"))
			{
				ps.setInt(1, _objectId);
				ps.setString(2, PlayerVariables.CLAN_CONTRIBUTION_PREVIOUS);
				try (ResultSet result = ps.executeQuery())
				{
					if (result.next())
					{
						_clanContributionPrevious = result.getInt("val");
					}
				}
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, "Could not load Previous Clan Contribution: " + e.getMessage(), e);
			}
			
			// Avoid further queries.
			if (_clanContributionPrevious == -1)
			{
				_clanContributionPrevious = 0;
			}
		}
		
		return _clanContributionPrevious;
	}
	
	/**
	 * Retrieves the previous total clan contribution for this member.<br>
	 * This value is loaded from the database if it has not been cached yet.<br>
	 * If no value exists in the database, it defaults to {@code 0}.
	 * @return the previous total clan contribution as an {@code int}.
	 */
	public int getClanContributionTotalPrevious()
	{
		if (_clanContributionTotalPrevious == -1)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT * FROM character_variables WHERE charId=? AND var=?"))
			{
				ps.setInt(1, _objectId);
				ps.setString(2, PlayerVariables.CLAN_CONTRIBUTION_TOTAL_PREVIOUS);
				try (ResultSet result = ps.executeQuery())
				{
					if (result.next())
					{
						_clanContributionTotalPrevious = result.getInt("val");
					}
				}
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, "Could not load Previous Clan Contribution Total: " + e.getMessage(), e);
			}
			
			// Avoid further queries.
			if (_clanContributionTotalPrevious == -1)
			{
				_clanContributionTotalPrevious = 0;
			}
		}
		
		return _clanContributionTotalPrevious;
	}
	
	/**
	 * Updates the total contribution amount for this clan member.<br>
	 * The new value is stored in the {@code _clanContributionTotal} field.
	 * @param value The new total contribution value to set.
	 */
	public void setClanContributionTotal(int value)
	{
		_clanContributionTotal = value;
	}
	
	/**
	 * Retrieves the total clan contribution for this member.<br>
	 * This method loads the value from the database if it is not already cached.<br>
	 * It returns 0 if no data is found or an error occurs.
	 * @return the total clan contribution as an {@code int}.
	 */
	public int getClanContributionTotal()
	{
		if (_clanContributionTotal == -1)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT * FROM character_variables WHERE charId=? AND var=?"))
			{
				ps.setInt(1, _objectId);
				ps.setString(2, PlayerVariables.CLAN_CONTRIBUTION_TOTAL);
				try (ResultSet result = ps.executeQuery())
				{
					if (result.next())
					{
						_clanContributionTotal = result.getInt("val");
					}
				}
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, "Could not load Clan Contribution total: " + e.getMessage(), e);
			}
			
			// Avoid further queries.
			if (_clanContributionTotal == -1)
			{
				_clanContributionTotal = 0;
			}
		}
		
		return _clanContributionTotal;
	}
}
