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
package org.l2jmobius.gameserver.data.holders;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemPointHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class serves as a data container for lucky game mechanics.<br>
 * It stores various statistics and configurations related to random loot or chance-based events.
 * @author Sdw
 */
public class LuckyGameDataHolder
{
	private final int _index;
	private final int _turningPoints;
	private final List<ItemChanceHolder> _commonRewards = new ArrayList<>();
	private final List<ItemPointHolder> _uniqueRewards = new ArrayList<>();
	private final List<ItemChanceHolder> _modifyRewards = new ArrayList<>();
	private int _minModifyRewardGame;
	private int _maxModifyRewardGame;
	
	/**
	 * Creates a new instance of {@code LuckyGameDataHolder}.<br>
	 * This constructor initializes the internal data from a {@code StatSet}.
	 * @param params The {@code StatSet} containing the required configuration values.
	 */
	public LuckyGameDataHolder(StatSet params)
	{
		_index = params.getInt("index");
		_turningPoints = params.getInt("turning_point");
	}
	
	/**
	 * Adds a new item to the common rewards list.<br>
	 * This method stores the provided {@code ItemChanceHolder} object.
	 * @param item The {@code ItemChanceHolder} to be added.
	 */
	public void addCommonReward(ItemChanceHolder item)
	{
		_commonRewards.add(item);
	}
	
	/**
	 * Adds a new unique reward to the list.<br>
	 * This method stores the provided {@code ItemPointHolder} object.
	 * @param item The {@code ItemPointHolder} to be added.
	 */
	public void addUniqueReward(ItemPointHolder item)
	{
		_uniqueRewards.add(item);
	}
	
	/**
	 * Adds a new reward to the modify rewards list.<br>
	 * This method stores an {@code ItemChanceHolder} object.
	 * @param item The {@code ItemChanceHolder} to be added.
	 */
	public void addModifyReward(ItemChanceHolder item)
	{
		_modifyRewards.add(item);
	}
	
	/**
	 * Retrieves the list of common rewards.<br>
	 * This method returns all items associated with common rewards for this game data.
	 * @return a {@code List} of {@code ItemChanceHolder} objects.
	 */
	public List<ItemChanceHolder> getCommonReward()
	{
		return _commonRewards;
	}
	
	/**
	 * Retrieves the list of unique rewards.<br>
	 * This method returns all {@code ItemPointHolder} objects associated with this game data.
	 * @return a {@code List} containing the unique rewards.
	 */
	public List<ItemPointHolder> getUniqueReward()
	{
		return _uniqueRewards;
	}
	
	/**
	 * Retrieves the list of rewards for modification.<br>
	 * This method returns all {@code ItemChanceHolder} objects stored in the modify rewards list.
	 * @return a {@code List} containing the {@code ItemChanceHolder} rewards.
	 */
	public List<ItemChanceHolder> getModifyReward()
	{
		return _modifyRewards;
	}
	
	/**
	 * Sets the minimum game count for modify rewards.<br>
	 * This value determines when a player can start receiving these rewards.
	 * @param minModifyRewardGame The minimum number of games required to receive a modify reward.
	 */
	public void setMinModifyRewardGame(int minModifyRewardGame)
	{
		_minModifyRewardGame = minModifyRewardGame;
	}
	
	/**
	 * Sets the maximum game number for modify rewards.<br>
	 * This value determines the upper limit of games allowed to receive a reward.
	 * @param maxModifyRewardGame The maximum game count to set.
	 */
	public void setMaxModifyRewardGame(int maxModifyRewardGame)
	{
		_maxModifyRewardGame = maxModifyRewardGame;
	}
	
	/**
	 * Retrieves the minimum game count for modify rewards.<br>
	 * This value determines when a player can start receiving these rewards.
	 * @return the minimum number of games required for a modify reward.
	 */
	public int getMinModifyRewardGame()
	{
		return _minModifyRewardGame;
	}
	
	/**
	 * Retrieves the maximum number of games allowed for modify rewards.<br>
	 * This value is stored in the {@code _maxModifyRewardGame} field.
	 * @return The maximum limit for modify reward games as an {@code int}.
	 */
	public int getMaxModifyRewardGame()
	{
		return _maxModifyRewardGame;
	}
	
	/**
	 * Retrieves the unique index of this data holder.<br>
	 * This value is used to identify the specific game entry.
	 * @return The current {@code int} index.
	 */
	public int getIndex()
	{
		return _index;
	}
	
	/**
	 * Returns the number of turning points for this game.<br>
	 * This value determines how many times the rewards can change.
	 * @return The total count of {@code turningPoints}.
	 */
	public int getTurningPoints()
	{
		return _turningPoints;
	}
}
