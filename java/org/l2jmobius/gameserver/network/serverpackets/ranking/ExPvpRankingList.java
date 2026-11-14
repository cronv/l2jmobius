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
package org.l2jmobius.gameserver.network.serverpackets.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.enums.player.RankingCategory;
import org.l2jmobius.gameserver.entity.actor.enums.player.RankingScope;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This packet sends the PvP ranking list to a client.<br>
 * It contains data for players categorized by {@code RankingCategory} and filtered by {@code RankingScope}.<br>
 * The list includes details such as player names, ranks, and associated statistics.
 * @author Berezkin Nikolay
 */
public class ExPvpRankingList extends ServerPacket
{
	private final Player _player;
	private final int _season;
	private final int _tabId;
	private final int _type;
	private final int _race;
	private final int _class;
	private final Map<Integer, StatSet> _playerList;
	private final Map<Integer, StatSet> _snapshotList;
	
	/**
	 * Creates a new {@code ExPvpRankingList} packet for a specific player.<br>
	 * This constructor initializes the ranking data based on the provided filters.
	 * @param player The {@code Player} who will receive this packet.
	 * @param season The current ranking season number.
	 * @param tabId The identifier for the UI tab.
	 * @param type The category type of the ranking.
	 * @param race The specific character race filter.
	 * @param baseclass The specific base class filter.
	 */
	public ExPvpRankingList(Player player, int season, int tabId, int type, int race, int baseclass)
	{
		_player = player;
		_season = season;
		_tabId = tabId;
		_type = type;
		_race = race;
		_class = baseclass;
		_playerList = RankManager.getInstance().getPvpRankList();
		_snapshotList = RankManager.getInstance().getSnapshotPvpRankList();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_PVP_RANKING_LIST.writeId(this, buffer);
		buffer.writeByte(_season);
		buffer.writeByte(_tabId);
		buffer.writeByte(_type);
		buffer.writeInt(_race);
		if (!_playerList.isEmpty() && (_type != 255) && (_race != 255))
		{
			final RankingCategory category = RankingCategory.values()[_tabId];
			writeFilteredRankingData(category, category.getScopeByGroup(_type), Race.values()[_race], PlayerClass.getPlayerClass(_class), buffer);
		}
		else
		{
			buffer.writeInt(0);
		}
	}
	
	/**
	 * Writes the ranking data to the {@code WriteBuffer} based on specific filters.<br>
	 * This method handles different categories like server, race, clan, friend, and class.<br>
	 * It selects the correct subset of players before writing the final data.
	 * @param category The type of ranking category to filter by.
	 * @param scope The scope of the ranking data to include.
	 * @param race The specific {@code Race} to filter for when using the RACE category.
	 * @param baseclass The specific {@code PlayerClass} to filter for when using the CLASS category.
	 * @param buffer The {@code WriteBuffer} used to store the outgoing data.
	 */
	private void writeFilteredRankingData(RankingCategory category, RankingScope scope, Race race, PlayerClass baseclass, WriteBuffer buffer)
	{
		switch (category)
		{
			case SERVER:
			{
				writeScopeData(scope, new ArrayList<>(_playerList.entrySet()), new ArrayList<>(_snapshotList.entrySet()), buffer);
				break;
			}
			case RACE:
			{
				writeScopeData(scope, _playerList.entrySet().stream().filter(it -> it.getValue().getInt("race") == race.ordinal()).toList(), _snapshotList.entrySet().stream().filter(it -> it.getValue().getInt("race") == race.ordinal()).toList(), buffer);
				break;
			}
			case CLAN:
			{
				final Clan playerClan = _player.getClan();
				writeScopeData(scope, playerClan == null ? Collections.emptyList() : _playerList.entrySet().stream().filter(it -> it.getValue().getString("clanName").equals(playerClan.getName())).toList(), playerClan == null ? Collections.emptyList() : _snapshotList.entrySet().stream().filter(it -> it.getValue().getString("clanName").equals(playerClan.getName())).toList(), buffer);
				break;
			}
			case FRIEND:
			{
				final Set<Integer> friendList = _player.getFriends().getIds();
				writeScopeData(scope, _playerList.entrySet().stream().filter(it -> friendList.contains(it.getValue().getInt("charId"))).toList(), _snapshotList.entrySet().stream().filter(it -> friendList.contains(it.getValue().getInt("charId"))).toList(), buffer);
				break;
			}
			case CLASS:
			{
				final int baseclassId = baseclass.getId();
				writeScopeData(scope, _playerList.entrySet().stream().filter(it -> it.getValue().getInt("classId") == baseclassId).toList(), _snapshotList.entrySet().stream().filter(it -> it.getValue().getInt("classId") == baseclassId).toList(), buffer);
				break;
			}
		}
	}
	
	/**
	 * Writes the ranking data to the provided buffer based on the specified scope.<br>
	 * It filters the list of entries and writes player statistics like name, level, and rank.
	 * @param scope The {@code RankingScope} used to determine how many players to include.
	 * @param list The full list of ranking entries.
	 * @param snapshot A list of snapshot data for additional stats.
	 * @param buffer The {@code WriteBuffer} where the data will be written.
	 */
	private void writeScopeData(RankingScope scope, List<Entry<Integer, StatSet>> list, List<Entry<Integer, StatSet>> snapshot, WriteBuffer buffer)
	{
		Entry<Integer, StatSet> playerData = list.stream().filter(it -> it.getValue().getInt("charId", 0) == _player.getObjectId()).findFirst().orElse(null);
		final int indexOf = list.indexOf(playerData);
		final List<Entry<Integer, StatSet>> limited;
		switch (scope)
		{
			case TOP_100:
			{
				limited = list.stream().limit(100).toList();
				break;
			}
			case ALL:
			{
				limited = list;
				break;
			}
			case TOP_150:
			{
				limited = list.stream().limit(150).toList();
				break;
			}
			case SELF:
			{
				limited = playerData == null ? Collections.emptyList() : list.subList(Math.max(0, indexOf - 10), Math.min(list.size(), indexOf + 10));
				break;
			}
			default:
			{
				limited = Collections.emptyList();
				break;
			}
		}
		
		buffer.writeInt(limited.size());
		int rank = 1;
		for (Entry<Integer, StatSet> data : limited.stream().sorted(Entry.comparingByKey()).toList())
		{
			int curRank = rank++;
			final StatSet player = data.getValue();
			buffer.writeSizedString(player.getString("name"));
			buffer.writeSizedString(player.getString("clanName"));
			buffer.writeInt(player.getInt("level"));
			buffer.writeInt(player.getInt("race"));
			buffer.writeInt(player.getInt("classId"));
			buffer.writeLong(player.getInt("points")); // server rank
			if (!snapshot.isEmpty())
			{
				for (Entry<Integer, StatSet> ssData : snapshot.stream().sorted(Entry.comparingByKey()).toList())
				{
					final StatSet snapshotData = ssData.getValue();
					if (player.getInt("charId") == snapshotData.getInt("charId"))
					{
						buffer.writeInt(scope == RankingScope.SELF ? ssData.getKey() : curRank); // server rank snapshot
						buffer.writeInt(snapshotData.getInt("raceRank", 0)); // race rank snapshot
						buffer.writeInt(player.getInt("kills"));
						buffer.writeInt(player.getInt("deaths"));
					}
				}
			}
			else
			{
				buffer.writeInt(scope == RankingScope.SELF ? data.getKey() : curRank);
				buffer.writeInt(0);
				buffer.writeInt(0);
				buffer.writeInt(0);
			}
		}
	}
}
