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
package org.l2jmobius.gameserver.managers.events;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.itemcontainer.Mail;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.balthusevent.ExBalthusEvent;
import org.l2jmobius.gameserver.network.serverpackets.balthusevent.ExBalthusEventJackpotUser;

/**
 * Manages the logic and lifecycle of Balthus events within the game world.<br>
 * This class handles event scheduling, rewards, and player notifications.
 * @author Index
 */
public class BalthusEventManager
{
	protected static final Logger LOGGER = Logger.getLogger(BalthusEventManager.class.getName());
	
	private final Set<Player> _players = ConcurrentHashMap.newKeySet();
	private final Map<Integer, BalthusEventHolder> _templates = new HashMap<>();
	private boolean _isEasyMode;
	private int _minLevel = 0;
	private ItemHolder _consolation = null;
	private String _mailSubject = null;
	private String _mailContent = null;
	private boolean _isRunning = false;
	private int _currProgress;
	private int _currState;
	private int _avoidMinutesIssue;
	private Player _winner = null;
	private ItemChanceHolder _rewardItem;
	
	/**
	 * Constructs a new instance of the {@code BalthusEventManager}.<br>
	 * This constructor is protected to restrict instantiation.
	 */
	protected BalthusEventManager()
	{
	}
	
	/**
	 * Initializes the event manager and sets up the initial state.<br>
	 * It calculates the starting delay based on the current time.<br>
	 * This method also schedules the winner selection task using {@code ThreadPool}.
	 */
	public void init()
	{
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _templates.size() + " rewards.");
		
		final Calendar calendar = Calendar.getInstance();
		final long currentTime = System.currentTimeMillis();
		final long hours = calendar.get(Calendar.HOUR_OF_DAY);
		final long mins = calendar.get(Calendar.MINUTE);
		long startDelay;
		if (mins <= 11)
		{
			calendar.set(Calendar.MINUTE, 12);
			calendar.set(Calendar.SECOND, 0);
			startDelay = calendar.getTimeInMillis() - currentTime;
			_currProgress = 20;
			_avoidMinutesIssue = 1;
		}
		else if (mins <= 23)
		{
			calendar.set(Calendar.MINUTE, 24);
			calendar.set(Calendar.SECOND, 0);
			startDelay = calendar.getTimeInMillis() - currentTime;
			_currProgress = 40;
			_avoidMinutesIssue = 2;
		}
		else if (mins <= 35)
		{
			calendar.set(Calendar.MINUTE, 36);
			calendar.set(Calendar.SECOND, 0);
			startDelay = calendar.getTimeInMillis() - currentTime;
			_currProgress = 60;
			_avoidMinutesIssue = 3;
		}
		else if (mins <= 47)
		{
			calendar.set(Calendar.MINUTE, 48);
			calendar.set(Calendar.SECOND, 0);
			startDelay = calendar.getTimeInMillis() - currentTime;
			_currProgress = 80;
			_avoidMinutesIssue = 4;
		}
		else
		{
			calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + 1);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			startDelay = calendar.getTimeInMillis() - currentTime;
			_currProgress = 100;
			_avoidMinutesIssue = 5;
		}
		
		if (((hours > 11) && (hours < 23)) || (hours == 23))
		{
			_currState = (int) hours - 11;
		}
		else
		{
			_currState = (int) hours + 13;
		}
		
		getNewRewardItem();
		ThreadPool.scheduleAtFixedRate(this::tryToGetWinner, startDelay, 720000);
	}
	
	/**
	 * Adds a {@code Player} to the list of active participants.<br>
	 * This method registers the player for event tracking.
	 * @param player The {@code Player} object to be added.
	 */
	public void addPlayer(Player player)
	{
		_players.add(player);
	}
	
	/**
	 * Removes a specific player from the active list.<br>
	 * This method updates the internal collection of players.
	 * @param player The {@code Player} object to be removed.
	 */
	public void removePlayer(Player player)
	{
		_players.remove(player);
	}
	
	/**
	 * Retrieves all players currently involved in the event.
	 * @return a {@code Set} of {@code Player} objects.
	 */
	public Set<Player> getPlayers()
	{
		return _players;
	}
	
	/**
	 * Removes a specific player from the list of participants.<br>
	 * This method updates the internal collection to exclude the provided {@code Player}.
	 * @param player The {@code Player} object to be removed.
	 */
	public void removeParticipant(Player player)
	{
		_players.remove(player);
	}
	
	/**
	 * Checks if a specific player is currently participating in the event.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is a participant, {@code false} otherwise.
	 */
	public boolean isPlayerParticipant(Player player)
	{
		return _players.contains(player);
	}
	
	/**
	 * Enables or disables the easy mode for the event.<br>
	 * This setting adjusts the difficulty of the game mechanics.
	 * @param isEasyMode The boolean value to set for easy mode.
	 */
	public void setEasyMode(boolean isEasyMode)
	{
		_isEasyMode = isEasyMode;
	}
	
	/**
	 * Retrieves the minimum level required for this event.
	 * @return the current {@code minLevel} value.
	 */
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	/**
	 * Sets the minimum level required to participate in the event.<br>
	 * This value is stored in the {@code _minLevel} field.
	 * @param minLevel The minimum level requirement for players.
	 */
	public void setMinLevel(int minLevel)
	{
		_minLevel = minLevel;
	}
	
	/**
	 * Retrieves the consolation item for the event.
	 * @return the {@code ItemHolder} representing the consolation prize.
	 */
	public ItemHolder getConsolation()
	{
		return _consolation;
	}
	
	/**
	 * Sets the consolation item for the event.<br>
	 * This method updates the {@code _consolation} field with the provided value.
	 * @param consolation The {@code ItemHolder} to be set as the consolation prize.
	 */
	public void setConsolation(ItemHolder consolation)
	{
		_consolation = consolation;
	}
	
	/**
	 * Sets the subject text for the outgoing mail.<br>
	 * This value will be used when sending notifications to players.
	 * @param mailSubject The text to use as the mail subject.
	 */
	public void setMailSubject(String mailSubject)
	{
		_mailSubject = mailSubject;
	}
	
	/**
	 * Sets the main body text for the outgoing mail.<br>
	 * This content will be displayed to players when they receive a message.
	 * @param mailContent The {@code String} containing the text to be sent.
	 */
	public void setMailContent(String mailContent)
	{
		_mailContent = mailContent;
	}
	
	/**
	 * Retrieves the current progress of the event.<br>
	 * This value represents how far the event has advanced.
	 * @return The current progress as an {@code int}.
	 */
	public int getCurrentProgress()
	{
		return _currProgress;
	}
	
	/**
	 * Retrieves the current state of the event.<br>
	 * This value represents the progress stage within the {@code BalthusEventManager}.
	 * @return The current state as an {@code int}.
	 */
	public int getCurrentState()
	{
		return _currState;
	}
	
	/**
	 * Retrieves the unique identifier of the current reward item.<br>
	 * This value is used to identify which item is currently being awarded.
	 * @return The {@code int} ID of the current reward item.
	 */
	public int getCurrRewardItem()
	{
		return _rewardItem.getId();
	}
	
	/**
	 * Checks if the event is currently active.
	 * @return {@code true} if the event is running, {@code false} otherwise.
	 */
	public boolean isRunning()
	{
		return _isRunning;
	}
	
	/**
	 * Retrieves the current event time in seconds.<br>
	 * This value is calculated based on the current system minute.
	 * @return The current time as an {@code int}.
	 */
	public int getTime()
	{
		return Calendar.getInstance().get(Calendar.MINUTE) * 60; // client makes 3600 - time
	}
	
	/**
	 * Attempts to determine and award a winner for the current event stage.<br>
	 * This method checks if the event is finished and rolls for a reward based on the item chance.<br>
	 * If a valid winner is found, it broadcasts the result and distributes rewards.<br>
	 * It also updates the progress counter regardless of whether a winner was selected.
	 */
	private void tryToGetWinner()
	{
		_avoidMinutesIssue++;
		if (_isRunning && (_avoidMinutesIssue != 6))
		{
			return;
		}
		
		if (!_isRunning && !_players.isEmpty() && (Rnd.get(100) <= _rewardItem.getChance()))
		{
			final List<Player> playerList = new ArrayList<>(_players);
			Collections.shuffle(playerList);
			for (Player player : playerList)
			{
				if (player.getLevel() >= _minLevel)
				{
					_winner = player;
				}
			}
			
			if (_winner != null)
			{
				final int _rewardItemId = _rewardItem.getId();
				LOGGER.info(getClass().getSimpleName() + ": New winner for " + _currState + " of Balthus Event is " + _winner.getName() + " - " + _winner.getObjectId() + ". Player win " + ItemData.getInstance().getTemplate(_rewardItemId).getName() + " - " + _rewardItemId + " count: " + _rewardItem.getCount() + ".");
				World.broadcastToAllOnlinePlayers(new ExBalthusEventJackpotUser());
				World.broadcastToAllOnlinePlayers(new SystemMessage(SystemMessageId.S1_HAS_OBTAINED_S2_FROM_THE_FESTIVAL_FAIRY).addPcName(_winner).addItemName(_rewardItemId));
				_isRunning = true;
				sendConsolationItemsToAll();
				sendRewardToPlayer(_winner);
			}
			else
			{
				_currProgress += 20;
			}
		}
		else
		{
			_currProgress += 20;
		}
		
		if (_avoidMinutesIssue == 6)
		{
			resetCurrentStage();
		}
		
		for (Player player : World.getPlayers())
		{
			player.sendPacket(new ExBalthusEvent(player));
		}
	}
	
	/**
	 * Resets the current stage of the event.<br>
	 * Increments the {@code _currState} and resets progress variables.<br>
	 * Sets {@code _isRunning} to {@code false}.<br>
	 * Triggers the selection of a new reward item.
	 */
	private void resetCurrentStage()
	{
		_currState += 1;
		_winner = null;
		if (_currState == 25)
		{
			_currState = 1;
		}
		
		_currProgress = 20;
		if (!_isRunning && _isEasyMode)
		{
			sendConsolationItemsToAll();
		}
		
		_isRunning = false;
		_rewardItem = null;
		_avoidMinutesIssue = 1;
		getNewRewardItem();
	}
	
	/**
	 * Distributes consolation items to all participants.<br>
	 * This method skips the winner and updates the reward for everyone else.<br>
	 * It adds the count from {@code _consolation} to each player's variables.<br>
	 * A system message is sent to notify players of their new rewards.
	 */
	private void sendConsolationItemsToAll()
	{
		for (Player player : _players)
		{
			if (player == _winner)
			{
				continue;
			}
			
			player.getVariables().set(PlayerVariables.BALTHUS_REWARD, player.getVariables().getInt(PlayerVariables.BALTHUS_REWARD, 0) + _consolation.getCount());
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_LUCKY_COIN_X_S1).addInt((int) _consolation.getCount()));
		}
	}
	
	/**
	 * Determines the next reward item for the current stage.<br>
	 * It checks all available templates to find a valid reward based on the current time.<br>
	 * A random selection is made from the possible items in that template.<br>
	 * The selected item is stored in {@code _rewardItem}.
	 */
	private void getNewRewardItem()
	{
		while (_rewardItem == null)
		{
			for (BalthusEventHolder holder : _templates.values())
			{
				if (holder.getRewardTime() == null)
				{
					continue;
				}
				
				final Map<Integer, Integer> rewardTime = holder.getRewardTime();
				final int firstParam = rewardTime.keySet().iterator().hasNext() ? rewardTime.keySet().iterator().next() : 0;
				final Calendar calendar = Calendar.getInstance();
				final int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
				if ((firstParam <= hourOfDay) && (hourOfDay <= rewardTime.get(firstParam)))
				{
					final double random = Rnd.get(100d);
					double chance = 0;
					for (Map<ItemChanceHolder, Double> map : holder.getRewardList().values())
					{
						for (Entry<ItemChanceHolder, Double> entry : map.entrySet())
						{
							chance += entry.getValue();
							if (chance >= random)
							{
								_rewardItem = entry.getKey();
								LOGGER.info(getClass().getSimpleName() + ": Reward for " + _currState + " stage set. Next reward item is " + ItemData.getInstance().getTemplate(_rewardItem.getId()).getName() + " - " + _rewardItem.getId() + " count: " + _rewardItem.getCount() + ".");
								break;
							}
						}
						
						if (_rewardItem != null)
						{
							break;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Sends a reward to the specified player.<br>
	 * This method creates a new {@code MailMessage} with the current reward item.<br>
	 * It then uses {@code MailManager} to deliver the mail to the player.
	 * @param player The {@code Player} who will receive the reward.
	 */
	private void sendRewardToPlayer(Player player)
	{
		final MailMessage msg = new MailMessage(player.getObjectId(), _mailSubject, _mailContent, MailType.NEWS_INFORMER);
		final Mail attachments = msg.createAttachments();
		attachments.addItem(ItemProcessType.REWARD, _rewardItem.getId(), _rewardItem.getCount(), null, null);
		MailManager.getInstance().sendMessage(msg);
	}
	
	/**
	 * Adds a new template to the internal collection.<br>
	 * This method maps a specific value to its corresponding {@code BalthusEventHolder}.
	 * @param value The unique identifier for the template.
	 * @param holder The {@code BalthusEventHolder} associated with this value.
	 */
	public void addTemplate(int value, BalthusEventHolder holder)
	{
		_templates.put(value, holder);
	}
	
	public static class BalthusEventHolder
	{
		// From hour to hour.
		private final Map<Integer, Integer> _rewardTime;
		
		// Time reward - Reward item, chance to put in lottery.
		private final Map<Integer, Map<ItemChanceHolder, Double>> _rewardList;
		
		public BalthusEventHolder(Map<Integer, Integer> rewardTime, Map<Integer, Map<ItemChanceHolder, Double>> rewardList)
		{
			_rewardTime = rewardTime;
			_rewardList = rewardList;
		}
		
		public Map<Integer, Integer> getRewardTime()
		{
			return _rewardTime;
		}
		
		public Map<Integer, Map<ItemChanceHolder, Double>> getRewardList()
		{
			return _rewardList;
		}
	}
	
	/**
	 * Retrieves the singleton instance of the {@code BalthusEventManager}.<br>
	 * Use this method to access the global manager for balthus events.
	 * @return The active {@code BalthusEventManager} instance.
	 */
	public static BalthusEventManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BalthusEventManager INSTANCE = new BalthusEventManager();
	}
}
