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
package org.l2jmobius.gameserver.mechanics.siege.manor;

/**
 * Handles the logic for procuring crops within the manor siege system.<br>
 * This class manages how seeds are processed into harvestable crops.
 * @author malyelfik
 */
public class CropProcure extends SeedProduction
{
	private final int _rewardType;
	
	/**
	 * Creates a new {@code CropProcure} instance.<br>
	 * This constructor initializes the reward data for crop procurement.
	 * @param id The unique identifier for the crop.
	 * @param amount The quantity of the crop to be produced.
	 * @param type The specific category or type of the reward.
	 * @param startAmount The initial amount available for production.
	 * @param price The cost associated with the procurement.
	 */
	public CropProcure(int id, long amount, int type, long startAmount, long price)
	{
		super(id, amount, price, startAmount);
		_rewardType = type;
	}
	
	/**
	 * Retrieves the reward type associated with this {@code CropProcure}.<br>
	 * This value is used to determine what kind of reward is given.
	 * @return The integer value representing the reward type.
	 */
	public int getReward()
	{
		return _rewardType;
	}
}
