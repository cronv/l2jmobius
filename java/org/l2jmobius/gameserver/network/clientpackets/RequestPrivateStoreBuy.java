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
package org.l2jmobius.gameserver.network.clientpackets;

import static org.l2jmobius.gameserver.entity.actor.Npc.INTERACTION_DISTANCE;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.holders.RequestTrade;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * Handles the request from a client to purchase an item from a private store.<br>
 * This packet processes the transaction and updates the player's inventory accordingly.
 */
public class RequestPrivateStoreBuy extends ClientPacket
{
	private static final int BATCH_LENGTH = 20; // length of the one item
	
	private int _storePlayerId;
	private Set<RequestTrade> _items = null;
	
	@Override
	protected void readImpl()
	{
		_storePlayerId = readInt();
		final int count = readInt();
		if ((count <= 0) || (count > PlayerConfig.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != remaining()))
		{
			return;
		}
		
		_items = new HashSet<>();
		for (int i = 0; i < count; i++)
		{
			final int objectId = readInt();
			final long cnt = readLong();
			final long price = readLong();
			if ((objectId < 1) || (cnt < 1) || (price < 0))
			{
				_items = null;
				return;
			}
			
			_items.add(new RequestTrade(objectId, cnt, price));
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_items == null)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isRegisteredOnEvent())
		{
			player.sendMessage("You cannot open a private store while participating in an event.");
			return;
		}
		
		if (!getClient().getFloodProtectors().canPerformTransaction())
		{
			player.sendMessage("You are buying items too fast.");
			return;
		}
		
		final WorldObject object = World.getPlayer(_storePlayerId);
		if ((object == null) || player.isCursedWeaponEquipped())
		{
			return;
		}
		
		final Player storePlayer = object.asPlayer();
		if (!player.isInsideRadius3D(storePlayer, INTERACTION_DISTANCE))
		{
			return;
		}
		
		if (player.getInstanceWorld() != storePlayer.getInstanceWorld())
		{
			return;
		}
		
		final PrivateStoreType privateStoreType = storePlayer.getPrivateStoreType();
		if (!((privateStoreType == PrivateStoreType.SELL) || (privateStoreType == PrivateStoreType.PACKAGE_SELL)))
		{
			return;
		}
		
		final TradeList storeList = storePlayer.getSellList();
		if (storeList == null)
		{
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your Access Level.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final int itemCount = storeList.getItemCount();
		if ((privateStoreType == PrivateStoreType.PACKAGE_SELL) && (itemCount > _items.size()))
		{
			final String msgErr = "[RequestPrivateStoreBuy] " + player + " tried to buy less items than sold by package-sell, ban this player for bot usage!";
			PunishmentManager.handleIllegalPlayerAction(player, msgErr, GeneralConfig.DEFAULT_PUNISH);
			return;
		}
		
		final int result = storeList.privateStoreBuy(player, _items);
		if (result > 0)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			if (result > 1)
			{
				PacketLogger.warning("PrivateStore buy has failed due to invalid list or request. Player: " + player.getName() + ", Private store of: " + storePlayer.getName());
			}
			return;
		}
		
		final boolean storeEmpty = storeList.getItemCount() == 0;
		
		// Update offline trade record, if realtime saving is enabled.
		if (OfflineTradeConfig.OFFLINE_TRADE_ENABLE && OfflineTradeConfig.STORE_OFFLINE_TRADE_IN_REALTIME && ((storePlayer.getClient() == null) || storePlayer.getClient().isDetached()))
		{
			OfflineTraderTable.getInstance().onTransaction(storePlayer, storeEmpty, false);
		}
		
		if (storeEmpty)
		{
			storePlayer.setPrivateStoreType(PrivateStoreType.NONE);
			storePlayer.broadcastUserInfo();
		}
	}
}
