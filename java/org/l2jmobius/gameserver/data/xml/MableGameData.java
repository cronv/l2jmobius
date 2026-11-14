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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.serverpackets.mablegame.ExMableGameMinigame;
import org.l2jmobius.gameserver.network.serverpackets.mablegame.ExMableGameMove;
import org.l2jmobius.gameserver.network.serverpackets.mablegame.ExMableGamePrison;
import org.l2jmobius.gameserver.network.serverpackets.mablegame.ExMableGameRewardItem;

/**
 * This class handles the loading and management of data for the Mable Game minigame.<br>
 * It parses configuration from XML files to initialize game mechanics, rewards, and skills.
 */
public class MableGameData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(MableGameData.class.getName());
	
	public static final int COMMON_DICE_ITEM_ID = 81461;
	public static final int ENHANCED_DICE_ITEM_ID = 81462;
	public static final int MIN_PRISON_DICE = 5;
	public static final int MAX_PRISON_DICE = 6;
	
	private final List<ItemHolder> _resetItems = new ArrayList<>();
	private final Map<Integer, MableGameCell> _cells = new HashMap<>();
	private final Map<String, MableGamePlayerState> _playerStates = new HashMap<>();
	private boolean _isEnabled;
	private int _dailyAvailableRounds;
	private int _commonDiceLimit;
	private ItemHolder _roundReward;
	
	/**
	 * Creates a new instance of {@code MableGameData}.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It initializes the game data from the configuration files.
	 */
	public MableGameData()
	{
		load();
	}
	
	/**
	 * Loads the game data from the XML configuration file.<br>
	 * It also populates player states from the database if the feature is enabled.
	 */
	@Override
	public void load()
	{
		_roundReward = null;
		_resetItems.clear();
		_cells.clear();
		parseDatapackFile("data/MableGameData.xml");
		
		if (_isEnabled)
		{
			final String simpleName = getClass().getSimpleName();
			LOGGER.info(simpleName + ": Loaded (" + _cells.size() + " cells)");
			
			if (_playerStates.isEmpty())
			{
				try (Connection conn = DatabaseFactory.getConnection();
					PreparedStatement ps = conn.prepareStatement("SELECT * FROM mable_game");
					ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						final String accountName = rs.getString("account_name");
						final int round = rs.getInt("round");
						final int currentCellId = rs.getInt("current_cell_id");
						final int remainCommonDice = rs.getInt("remain_common_dice");
						final int remainPrisonRolls = rs.getInt("remain_prison_rolls");
						_playerStates.put(accountName, new MableGamePlayerState(round, currentCellId, remainCommonDice, remainPrisonRolls));
					}
				}
				catch (Exception e)
				{
					LOGGER.warning(simpleName + ": Failed loading player states. " + e.getMessage());
				}
			}
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": Disabled");
		}
	}
	
	/**
	 * Saves the current state of all players to the database.<br>
	 * This method updates the {@code mable_game} table with round progress and dice counts.<br>
	 * It only executes if the game is currently enabled.
	 */
	public void save()
	{
		if (!_isEnabled)
		{
			return;
		}
		
		try (Connection conn = DatabaseFactory.getConnection();
			PreparedStatement ps = conn.prepareStatement("UPDATE mable_game SET round=?, current_cell_id=?, remain_common_dice=?, remain_prison_rolls=? WHERE account_name=?"))
		{
			for (Entry<String, MableGamePlayerState> entry : _playerStates.entrySet())
			{
				final MableGamePlayerState state = entry.getValue();
				ps.setInt(1, state.getRound());
				ps.setInt(2, state.getCurrentCellId());
				ps.setInt(3, state.getRemainCommonDice());
				ps.setInt(4, state.getRemainingPrisonRolls());
				ps.setString(5, entry.getKey());
				ps.addBatch();
			}
			
			ps.executeBatch();
			LOGGER.warning(getClass().getSimpleName() + ": Saved player states.");
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed saving player states. " + e.getMessage());
		}
	}
	
	/**
	 * Parses the configuration data from an XML document.<br>
	 * This method populates the game settings and cell data.<br>
	 * It checks if the feature is enabled before processing.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the configuration.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
		{
			_isEnabled = true;
			final NamedNodeMap at = listNode.getAttributes();
			final Node attribute = at.getNamedItem("enabled");
			if ((attribute != null) && Boolean.parseBoolean(attribute.getNodeValue()))
			{
				// Parse dailyAvailableRounds.
				forEach(listNode, "dailyAvailableRounds", dailyAvailableRounds ->
				{
					_dailyAvailableRounds = Integer.parseInt(dailyAvailableRounds.getTextContent());
				});
				
				// Parse commonDiceLimit.
				forEach(listNode, "commonDiceLimit", commonDiceLimit ->
				{
					_commonDiceLimit = Integer.parseInt(commonDiceLimit.getTextContent());
				});
				
				// Parse roundReward.
				forEach(listNode, "roundReward", roundReward ->
				{
					final NamedNodeMap attrs = roundReward.getAttributes();
					final int itemId = parseInteger(attrs, "id");
					final int itemCount = parseInteger(attrs, "count");
					_roundReward = new ItemHolder(itemId, itemCount);
				});
				
				// Parse resetItems.
				forEach(listNode, "resetItems", resetItems ->
				{
					forEach(resetItems, "item", item ->
					{
						final NamedNodeMap attrs = item.getAttributes();
						final int itemId = parseInteger(attrs, "id");
						final int itemCount = parseInteger(attrs, "count");
						_resetItems.add(new ItemHolder(itemId, itemCount));
					});
				});
				
				// Parse cells.
				forEach(listNode, "cells", cells ->
				{
					forEach(cells, "cell", cell ->
					{
						final NamedNodeMap attrs = cell.getAttributes();
						final int id = parseInteger(attrs, "id");
						final int color = parseInteger(attrs, "color");
						final String name = parseString(attrs, "name");
						final String[] paramsStr = parseString(attrs, "params", "").split(";");
						final int[] params = new int[paramsStr.length];
						if (!paramsStr[0].isBlank())
						{
							for (int i = 0; i < paramsStr.length; i++)
							{
								params[i] = Integer.parseInt(paramsStr[i]);
							}
						}
						
						final List<ItemHolder> rewards = new ArrayList<>();
						forEach(cell, "rewards", rewardsNode ->
						{
							forEach(rewardsNode, "item", item ->
							{
								final NamedNodeMap itemAttrs = item.getAttributes();
								final int itemId = parseInteger(itemAttrs, "id");
								final int itemCount = parseInteger(itemAttrs, "count");
								rewards.add(new ItemHolder(itemId, itemCount));
							});
						});
						
						final MableGameCellColor cellColor = MableGameCellColor.getByClientId(color);
						if (cellColor == null)
						{
							LOGGER.warning(getClass().getSimpleName() + ": Missing color: " + color + " for cell id: " + id);
						}
						else
						{
							_cells.put(id, new MableGameCell(id, cellColor, name, params, rewards));
						}
					});
				});
			}
			else
			{
				_isEnabled = false;
				_dailyAvailableRounds = 0;
				_commonDiceLimit = 0;
			}
		});
	}
	
	/**
	 * Checks if the game feature is currently active.
	 * @return {@code true} if enabled, or {@code false} if disabled.
	 */
	public boolean isEnabled()
	{
		return _isEnabled;
	}
	
	/**
	 * Retrieves the total number of rounds available for the day.<br>
	 * This value is used to limit how many times a player can participate.
	 * @return the current count of daily available rounds as an {@code int}.
	 */
	public int getDailyAvailableRounds()
	{
		return _dailyAvailableRounds;
	}
	
	/**
	 * Retrieves the maximum number of common dice allowed.<br>
	 * This value is used to limit how many items a player can use.
	 * @return The current common dice limit as an {@code int}.
	 */
	public int getCommonDiceLimit()
	{
		return _commonDiceLimit;
	}
	
	/**
	 * Retrieves the total number of cells available.<br>
	 * This value corresponds to the size of the internal cell collection.
	 * @return The highest {@code int} ID for a cell.
	 */
	public int getHighestCellId()
	{
		return _cells.size();
	}
	
	/**
	 * Retrieves the reward given for completing a round.
	 * @return the {@code ItemHolder} object containing the reward items.
	 */
	public ItemHolder getRoundReward()
	{
		return _roundReward;
	}
	
	/**
	 * Retrieves the list of items used for resetting.
	 * @return a {@code List} of {@code ItemHolder} objects.
	 */
	public List<ItemHolder> getResetItems()
	{
		return _resetItems;
	}
	
	/**
	 * Retrieves a specific cell from the data map.<br>
	 * This method looks up the cell using its unique identifier.
	 * @param cellId The unique ID of the cell to find.
	 * @return The {@code MableGameCell} object associated with the given ID, or {@code null} if not found.
	 */
	public MableGameCell getCellById(int cellId)
	{
		return _cells.get(cellId);
	}
	
	/**
	 * Retrieves the game state for a specific player.<br>
	 * If no state exists, it creates a new one and saves it to the database.
	 * @param accountName The unique name of the player account.
	 * @return The {@code MableGamePlayerState} associated with the provided account name.
	 */
	public MableGamePlayerState getPlayerState(String accountName)
	{
		MableGamePlayerState state = _playerStates.get(accountName);
		if (state == null)
		{
			state = new MableGamePlayerState(1, 1, _commonDiceLimit, 0);
			_playerStates.put(accountName, state);
			try (Connection conn = DatabaseFactory.getConnection();
				PreparedStatement ps = conn.prepareStatement("INSERT INTO mable_game VALUES (?,?,?,?,?)"))
			{
				ps.setString(1, accountName);
				ps.setInt(2, state.getRound());
				ps.setInt(3, state.getCurrentCellId());
				ps.setInt(4, state.getRemainCommonDice());
				ps.setInt(5, state.getRemainingPrisonRolls());
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Failed inserting player state for account: " + accountName + ". " + e.getMessage());
			}
		}
		
		return state;
	}
	
	public enum MableGameCellColor
	{
		LIGHT_BLUE(1),
		YELLOW(2),
		PURPLE(3),
		RED(4),
		DARK_RED(5),
		GREEN(7),
		BURNING_RED(8),
		DARK_PURPLE(9);
		
		private final int _clientId;
		
		MableGameCellColor(int clientId)
		{
			_clientId = clientId;
		}
		
		public static MableGameCellColor getByClientId(int id)
		{
			for (MableGameCellColor color : MableGameCellColor.values())
			{
				if (color.getClientId() == id)
				{
					return color;
				}
			}
			
			return null;
		}
		
		public int getClientId()
		{
			return _clientId;
		}
	}
	
	public static class MableGamePlayerState
	{
		private int _round;
		private int _currentCellId;
		private int _remainCommonDice;
		private int _remainingPrisonRolls;
		private int _pendingCellIdPopup = -1;
		private ItemHolder _pendingReward = null;
		private boolean _isMoved = false;
		
		private MableGamePlayerState(int round, int currentCellId, int remainCommonDice, int remainingPrisonRolls)
		{
			_round = round;
			_currentCellId = currentCellId;
			_remainCommonDice = remainCommonDice;
			_remainingPrisonRolls = remainingPrisonRolls;
		}
		
		public int getRound()
		{
			return _round;
		}
		
		public void setRound(int round)
		{
			_round = round;
		}
		
		public int getCurrentCellId()
		{
			return _currentCellId;
		}
		
		public void setCurrentCellId(int currentCellId)
		{
			_currentCellId = currentCellId;
		}
		
		public int getRemainCommonDice()
		{
			return _remainCommonDice;
		}
		
		public void setRemainCommonDice(int remainCommonDice)
		{
			_remainCommonDice = remainCommonDice;
		}
		
		public int getRemainingPrisonRolls()
		{
			return _remainingPrisonRolls;
		}
		
		public void setRemainingPrisonRolls(int count)
		{
			_remainingPrisonRolls = count;
		}
		
		public int getPendingCellIdPopup()
		{
			return _pendingCellIdPopup;
		}
		
		public void setPendingCellIdPopup(int cellId)
		{
			_pendingCellIdPopup = cellId;
		}
		
		public ItemHolder getPendingReward()
		{
			return _pendingReward;
		}
		
		public void setPendingReward(ItemHolder reward)
		{
			_pendingReward = reward;
		}
		
		public boolean isMoved()
		{
			return _isMoved;
		}
		
		public void setMoved(boolean val)
		{
			_isMoved = val;
		}
		
		public void handleCell(Player player, MableGameCell cell)
		{
			switch (cell.getColor())
			{
				case LIGHT_BLUE:
				{
					final int cellId = cell.getId();
					if (cellId == MableGameData.getInstance().getHighestCellId())
					{
						final ItemHolder roundReward = MableGameData.getInstance().getRoundReward();
						if (roundReward != null)
						{
							setPendingCellIdPopup(cellId);
							setPendingReward(roundReward);
							player.sendPacket(new ExMableGameRewardItem(roundReward.getId(), roundReward.getCount()));
						}
					}
					break;
				}
				case YELLOW:
				{
					final int cellId = cell.getId();
					final int[] params = cell.getParams();
					int newCellId = cellId;
					if (Rnd.nextBoolean())
					{
						newCellId -= params[Rnd.get(params.length)];
						newCellId = Math.max(newCellId, 1);
					}
					else
					{
						newCellId += params[Rnd.get(params.length)];
						newCellId = Math.min(newCellId, MableGameData.getInstance().getHighestCellId());
					}
					
					setPendingCellIdPopup(newCellId);
					setCurrentCellId(newCellId);
					setMoved(true);
					final MableGameCell newCell = MableGameData.getInstance().getCellById(newCellId);
					player.sendPacket(new ExMableGameMove(newCellId - cellId, newCellId, newCell.getColor().getClientId()));
					break;
				}
				case RED:
				case BURNING_RED:
				{
					final int luckyNumber = Rnd.get(1, 6);
					final int dice = Rnd.get(1, 6);
					final int bossDice = Rnd.get(1, 6);
					final int result = dice < bossDice ? 0 : dice == bossDice ? 2 : 1;
					final boolean isLuckyNumber = luckyNumber == dice;
					final int rewardIndex = dice < bossDice ? 0 : dice == bossDice ? 1 : 2;
					final List<ItemHolder> rewards = cell.getRewards();
					final ItemHolder reward = (rewards.size() - 1) < rewardIndex ? new ItemHolder(Inventory.ADENA_ID, 1) : rewards.get(rewardIndex);
					final int itemId = reward.getId();
					final long itemCount = reward.getCount() * (isLuckyNumber && (itemId != Inventory.ADENA_ID) ? 2 : 1);
					setPendingCellIdPopup(cell.getId());
					setPendingReward(new ItemHolder(itemId, itemCount));
					player.sendPacket(new ExMableGameMinigame(cell.getParams()[0], luckyNumber, dice, bossDice, result, isLuckyNumber, itemId, itemCount));
					break;
				}
				case DARK_RED:
				{
					setRemainingPrisonRolls(3);
					player.sendPacket(new ExMableGamePrison(MIN_PRISON_DICE, MAX_PRISON_DICE, getRemainingPrisonRolls()));
					break;
				}
				case PURPLE:
				case GREEN:
				{
					final List<ItemHolder> greenRewards = cell.getRewards();
					if (greenRewards != null)
					{
						for (ItemHolder reward : greenRewards)
						{
							setPendingCellIdPopup(cell.getId());
							setPendingReward(reward);
							player.sendPacket(new ExMableGameRewardItem(reward.getId(), reward.getCount()));
							break;
						}
					}
					break;
				}
				case DARK_PURPLE:
				{
					break;
				}
				default:
				{
					LOGGER.warning(getClass().getSimpleName() + ": Unhandled Cell Id:" + cell.getId() + " Color:" + cell.getColor());
					break;
				}
			}
		}
	}
	
	public static class MableGameCell
	{
		private final int _id;
		private final MableGameCellColor _color;
		private final String _name;
		private final int[] _params;
		private final List<ItemHolder> _rewards;
		
		protected MableGameCell(int id, MableGameCellColor color, String name, int[] params, List<ItemHolder> rewards)
		{
			_id = id;
			_color = color;
			_name = name;
			_params = params;
			_rewards = rewards;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public MableGameCellColor getColor()
		{
			return _color;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public int[] getParams()
		{
			return _params;
		}
		
		public List<ItemHolder> getRewards()
		{
			return _rewards;
		}
	}
	
	/**
	 * Retrieves the singleton instance of {@code MableGameData}.<br>
	 * This method provides global access to the game data.
	 * @return The active {@code MableGameData} instance.
	 */
	public static MableGameData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MableGameData INSTANCE = new MableGameData();
	}
}
