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
package org.l2jmobius.gameserver.util;

import org.l2jmobius.gameserver.config.FloodProtectorConfig;
import org.l2jmobius.gameserver.network.GameClient;

/**
 * Holds the per-player collection of flood protectors guarding individual client actions.
 * @author fordfrog, Mobius
 */
public class FloodProtectors
{
	private final FloodProtectorAction _useItem;
	private final FloodProtectorAction _rollDice;
	private final FloodProtectorAction _itemPetSummon;
	private final FloodProtectorAction _heroVoice;
	private final FloodProtectorAction _globalChat;
	private final FloodProtectorAction _subclass;
	private final FloodProtectorAction _dropItem;
	private final FloodProtectorAction _serverBypass;
	private final FloodProtectorAction _multiSell;
	private final FloodProtectorAction _transaction;
	private final FloodProtectorAction _manufacture;
	private final FloodProtectorAction _sendMail;
	private final FloodProtectorAction _characterSelect;
	private final FloodProtectorAction _itemAuction;
	private final FloodProtectorAction _playerAction;
	
	/**
	 * Creates flood protector actions for a client.
	 * @param client client these protectors guard
	 */
	public FloodProtectors(GameClient client)
	{
		_useItem = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_USE_ITEM);
		_rollDice = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_ROLL_DICE);
		_itemPetSummon = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_ITEM_PET_SUMMON);
		_heroVoice = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_HERO_VOICE);
		_globalChat = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_GLOBAL_CHAT);
		_subclass = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_SUBCLASS);
		_dropItem = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_DROP_ITEM);
		_serverBypass = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_SERVER_BYPASS);
		_multiSell = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_MULTISELL);
		_transaction = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_TRANSACTION);
		_manufacture = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_MANUFACTURE);
		_sendMail = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_SENDMAIL);
		_characterSelect = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_CHARACTER_SELECT);
		_itemAuction = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_ITEM_AUCTION);
		_playerAction = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_PLAYER_ACTION);
	}
	
	/**
	 * Indicates whether using an item is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canUseItem()
	{
		return _useItem.canPerformAction();
	}
	
	/**
	 * Indicates whether rolling dice is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canRollDice()
	{
		return _rollDice.canPerformAction();
	}
	
	/**
	 * Indicates whether using a pet summon item is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canUsePetSummonItem()
	{
		return _itemPetSummon.canPerformAction();
	}
	
	/**
	 * Indicates whether using hero voice is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canUseHeroVoice()
	{
		return _heroVoice.canPerformAction();
	}
	
	/**
	 * Indicates whether using global chat is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canUseGlobalChat()
	{
		return _globalChat.canPerformAction();
	}
	
	/**
	 * Indicates whether changing subclass is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canChangeSubclass()
	{
		return _subclass.canPerformAction();
	}
	
	/**
	 * Indicates whether dropping an item is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canDropItem()
	{
		return _dropItem.canPerformAction();
	}
	
	/**
	 * Indicates whether using a server bypass is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canUseServerBypass()
	{
		return _serverBypass.canPerformAction();
	}
	
	/**
	 * Indicates whether using multi-sell is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canUseMultiSell()
	{
		return _multiSell.canPerformAction();
	}
	
	/**
	 * Indicates whether performing a transaction is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canPerformTransaction()
	{
		return _transaction.canPerformAction();
	}
	
	/**
	 * Indicates whether manufacturing is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canManufacture()
	{
		return _manufacture.canPerformAction();
	}
	
	/**
	 * Indicates whether sending mail is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canSendMail()
	{
		return _sendMail.canPerformAction();
	}
	
	/**
	 * Indicates whether selecting a character is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canSelectCharacter()
	{
		return _characterSelect.canPerformAction();
	}
	
	/**
	 * Indicates whether using the item auction is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canUseItemAuction()
	{
		return _itemAuction.canPerformAction();
	}
	
	/**
	 * Indicates whether performing a general player action is currently permitted.
	 * @return {@code true} if permitted, {@code false} otherwise
	 */
	public boolean canPerformPlayerAction()
	{
		return _playerAction.canPerformAction();
	}
}
