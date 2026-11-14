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
package org.l2jmobius.gameserver.network.serverpackets.luckygame;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.LuckyGameBettingType;
import org.l2jmobius.gameserver.network.enums.LuckyGameRewardType;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet sends the result of an {@code LuckyGame} to the client.<br>
 * It contains information about the won items and the specific type of result achieved.
 * @author Sdw
 */
public class ExBettingLuckyGameResult extends ServerPacket
{
	public static final int RESULT_INVALID_CAPACITY = -2;
	public static final int RESULT_INVALID_ITEM_COUNT = -1;
	public static final int RESULT_SUCCESS = 1;
	
	public static final ExBettingLuckyGameResult NORMAL_INVALID_ITEM_COUNT = new ExBettingLuckyGameResult(RESULT_INVALID_ITEM_COUNT, LuckyGameBettingType.NORMAL);
	public static final ExBettingLuckyGameResult PREMIUM_INVALID_ITEM_COUNT = new ExBettingLuckyGameResult(RESULT_INVALID_ITEM_COUNT, LuckyGameBettingType.PREMIUM);
	public static final ExBettingLuckyGameResult NORMAL_INVALID_CAPACITY = new ExBettingLuckyGameResult(RESULT_INVALID_CAPACITY, LuckyGameBettingType.NORMAL);
	public static final ExBettingLuckyGameResult PREMIUM_INVALID_CAPACITY = new ExBettingLuckyGameResult(RESULT_INVALID_CAPACITY, LuckyGameBettingType.PREMIUM);
	
	private final int _result;
	private final LuckyGameBettingType _type;
	private final Map<LuckyGameRewardType, List<ItemHolder>> _rewards;
	private final int _ticketCount;
	private final int _size;
	
	/**
	 * Creates a new {@code ExBettingLuckyGameResult} with default values.<br>
	 * This constructor initializes the result and game type.<br>
	 * It sets the reward map, ticket count, and size to their default states.
	 * @param result The result code, one of the {@code RESULT_} constants.
	 * @param type The {@code LuckyGameBettingType} of the game.
	 */
	public ExBettingLuckyGameResult(int result, LuckyGameBettingType type)
	{
		_result = result;
		_type = type;
		_rewards = new EnumMap<>(LuckyGameRewardType.class);
		_ticketCount = 0;
		_size = 0;
	}
	
	/**
	 * Creates a new {@code ExBettingLuckyGameResult} packet.<br>
	 * This constructor initializes the result type, game type, rewards, and ticket count.<br>
	 * It also calculates the total size of all items in the rewards map.
	 * @param result The result code, one of the {@code RESULT_} constants.
	 * @param type The {@code LuckyGameBettingType} of the game played.
	 * @param rewards A map of {@code LuckyGameRewardType} groups to won item holders.
	 * @param ticketCount The number of tickets used for this result.
	 */
	public ExBettingLuckyGameResult(int result, LuckyGameBettingType type, Map<LuckyGameRewardType, List<ItemHolder>> rewards, int ticketCount)
	{
		_result = result;
		_type = type;
		_rewards = rewards;
		_ticketCount = ticketCount;
		_size = rewards.values().stream().mapToInt(List::size).sum();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BETTING_LUCKY_GAME_RESULT.writeId(this, buffer);
		buffer.writeInt(_result);
		buffer.writeInt(_type.getClientId());
		buffer.writeInt(_ticketCount);
		buffer.writeInt(_size);
		for (Entry<LuckyGameRewardType, List<ItemHolder>> reward : _rewards.entrySet())
		{
			for (ItemHolder item : reward.getValue())
			{
				buffer.writeInt(reward.getKey().getClientId());
				buffer.writeInt(item.getId());
				buffer.writeInt((int) item.getCount());
			}
		}
	}
}
