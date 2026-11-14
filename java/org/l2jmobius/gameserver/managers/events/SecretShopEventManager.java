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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Mail;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExItemAnnounce;
import org.l2jmobius.gameserver.network.serverpackets.secretshop.ExFestivalBmAllItemInfo;
import org.l2jmobius.gameserver.network.serverpackets.secretshop.ExFestivalBmGame;
import org.l2jmobius.gameserver.network.serverpackets.secretshop.ExFestivalBmInfo;
import org.l2jmobius.gameserver.network.serverpackets.secretshop.ExFestivalBmTopItemInfo;

/**
 * Manages the logic and lifecycle of secret shop events within the game world.<br>
 * This class handles event scheduling, item distribution, and client notifications for secret shop activities.
 * @author Serenitty
 */
public class SecretShopEventManager
{
	private static final Logger LOGGER = Logger.getLogger(SecretShopEventManager.class.getName());
	
	private static final int UPDATE_INTERVAL = 5000;
	private static final int CLAN_REWARD_ITEM_ID = 94834;
	private static final int CLAN_REWARD_ITEM_COUNT = 1000;
	
	private static final int TICKET_AMOUNT_PER_GAME = 1;
	
	private static final ConcurrentHashMap<Player, PlayerTicketData> PLAYER_REWARD_QUEUES = new ConcurrentHashMap<>();
	private static final Set<Player> LIST_TO_UPDATE = ConcurrentHashMap.newKeySet();
	
	private static Map<Integer, List<SecretShopRewardHolder>> _rewardData = new HashMap<>();
	private static List<SecretShopRewardHolder> _activeRewards;
	private static boolean _isEventPeriod;
	private static boolean _exchangeEnabled;
	private static long _startTime;
	private static long _endTime;
	private static int _ticketId;
	private static int _startHour;
	private static int _startMinute;
	private static int _endHour;
	private static int _endMinute;
	
	private ScheduledFuture<?> _updateTask = null;
	private ScheduledFuture<?> _startTask = null;
	private ScheduledFuture<?> _endTask = null;
	
	/**
	 * Constructs a new instance of the {@code SecretShopEventManager}.<br>
	 * This constructor initializes the manager for handling secret shop events.
	 */
	protected SecretShopEventManager()
	{
	}
	
	/**
	 * Initializes the event with reward data and time constraints.<br>
	 * Sets up the required parameters for the secret shop manager.
	 * @param rewardData A map containing the rewards associated with each game ID.
	 * @param ticketId The unique identifier for the event ticket.
	 * @param startHour The hour when the event begins.
	 * @param startMinute The minute when the event begins.
	 * @param endHour The hour when the event ends.
	 * @param endMinute The minute when the event ends.
	 */
	public void init(Map<Integer, List<SecretShopRewardHolder>> rewardData, int ticketId, int startHour, int startMinute, int endHour, int endMinute)
	{
		_rewardData = rewardData;
		_ticketId = ticketId;
		_startHour = startHour;
		_startMinute = startMinute;
		_endHour = endHour;
		_endMinute = endMinute;
	}
	
	/**
	 * Initializes the event logic and starts background tasks.<br>
	 * This method sets the exchange status to {@code true}.<br>
	 * It schedules periodic updates and a final stop task.
	 */
	private void start()
	{
		if (!_isEventPeriod)
		{
			return;
		}
		
		final String simpleName = getClass().getSimpleName();
		LOGGER.warning(simpleName + ": Started!");
		_exchangeEnabled = true;
		_endTime = getNextEndTime();
		_startTime = 0;
		
		initRewards(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)); // 1 (Sunday) to 7 (Saturday).
		broadcastInfo();
		
		if (_updateTask != null)
		{
			_updateTask.cancel(true);
			_updateTask = null;
		}
		
		_updateTask = ThreadPool.scheduleAtFixedRate(this::updateInfo, 500, UPDATE_INTERVAL);
		
		if (_endTask != null)
		{
			_endTask.cancel(false);
			_endTask = null;
		}
		
		LOGGER.warning(simpleName + ": Scheduled end in " + _endTime + " ms.");
		ThreadPool.schedule(this::stop, _endTime);
	}
	
	/**
	 * Stops the current event and prepares for the next scheduled session.<br>
	 * It resets the exchange status and clears active reward amounts.<br>
	 * This method also cancels any active update or end tasks.<br>
	 * Finally, it schedules the {@code start} method to run after a delay.
	 */
	private void stop()
	{
		_exchangeEnabled = false;
		_startTime = getNextStartTime();
		_endTime = 0;
		
		for (SecretShopRewardHolder reward : _activeRewards)
		{
			reward._currentAmount = 0;
		}
		
		if (_updateTask != null)
		{
			_updateTask.cancel(true);
			_updateTask = null;
		}
		
		if (_endTask != null)
		{
			_endTask.cancel(false);
			_endTask = null;
		}
		
		updateInfo();
		LIST_TO_UPDATE.clear();
		broadcastInfo();
		
		LOGGER.warning(getClass().getSimpleName() + ": Scheduled start in " + _startTime + " ms.");
		ThreadPool.schedule(this::start, _startTime);
	}
	
	/**
	 * Activates the secret shop event.<br>
	 * Checks if rewards are available before starting.<br>
	 * Cancels any existing scheduled tasks for this manager.<br>
	 * Starts the event immediately or schedules it for the next period.
	 */
	public void startEvent()
	{
		if (_rewardData.isEmpty())
		{
			return;
		}
		
		_isEventPeriod = true;
		final String className = getClass().getSimpleName();
		LOGGER.warning(className + ": Activated");
		if (_startTask != null)
		{
			_startTask.cancel(false);
			_startTask = null;
		}
		
		if (_endTask != null)
		{
			_endTask.cancel(false);
			_endTask = null;
		}
		
		if (_updateTask != null)
		{
			_updateTask.cancel(true);
			_updateTask = null;
		}
		
		LIST_TO_UPDATE.clear();
		
		if (isExchangePeriod())
		{
			start();
		}
		else
		{
			_startTime = getNextStartTime();
			LOGGER.warning(className + ": Scheduled start in " + _startTime + " ms.");
			_startTask = ThreadPool.schedule(this::start, _startTime);
		}
	}
	
	/**
	 * Stops the current event. <br>
	 * Sets the {@code _isEventPeriod} flag to {@code false}.
	 */
	public void stopEvent()
	{
		_isEventPeriod = false;
	}
	
	/**
	 * Allows a {@code Player} to exchange tickets for rewards.<br>
	 * This method checks if the event is active and if the player has enough tickets.<br>
	 * It then selects a reward based on random chances and updates the inventory.<br>
	 * If a top grade reward is won, it broadcasts an announcement to all players.
	 * @param player The {@code Player} attempting to exchange tickets.
	 */
	public synchronized void exchange(Player player)
	{
		if (!_isEventPeriod || !hasAvailableRewards())
		{
			return;
		}
		
		int ticketAmount = (int) player.getInventory().getInventoryItemCount(_ticketId, -1);
		if (ticketAmount < TICKET_AMOUNT_PER_GAME)
		{
			return;
		}
		
		double totalChance = 0;
		double totalPossibleChance = 0;
		for (SecretShopRewardHolder reward : _activeRewards)
		{
			if (reward._currentAmount <= 0)
			{
				continue;
			}
			
			totalPossibleChance += reward._chance;
		}
		
		final double chance = Rnd.get(0, totalPossibleChance);
		for (SecretShopRewardHolder reward : _activeRewards)
		{
			if (reward._currentAmount <= 0)
			{
				continue;
			}
			
			totalChance += reward._chance;
			if (totalChance >= chance)
			{
				reward._currentAmount--;
				
				player.destroyItemByItemId(ItemProcessType.FEE, _ticketId, TICKET_AMOUNT_PER_GAME, player, true);
				ticketAmount -= TICKET_AMOUNT_PER_GAME;
				
				final Item item = player.addItem(ItemProcessType.REWARD, reward._id, reward._count, player, true);
				player.sendPacket(new ExFestivalBmGame(_ticketId, ticketAmount, TICKET_AMOUNT_PER_GAME, reward._grade, reward._id, (int) reward._count, 1));
				
				if (reward.isTopGrade())
				{
					World.broadcastToAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.EVENT_PARTICIPATE));
					sendClanReward(player); // Optional
					if (noTopGradeRewardsRemaining())
					{
						show(player, true);
						updateInfo();
						stop();
						return;
					}
					
					broadcastInfo();
				}
				break;
			}
		}
		
		show(player, true);
	}
	
	/**
	 * Updates the event information for a specific player.<br>
	 * This method sends packets to show or hide rewards based on the {@code show} flag.<br>
	 * It also manages which players are currently being tracked for updates.
	 * @param player The {@code Player} who will receive the update.
	 * @param show Set to {@code true} to display information, or {@code false} to hide it.
	 */
	public void show(Player player, boolean show)
	{
		if (!_isEventPeriod)
		{
			return;
		}
		
		if (show)
		{
			if (!LIST_TO_UPDATE.contains(player))
			{
				LIST_TO_UPDATE.add(player);
			}
			
			final int ticketAmount = (int) player.getInventory().getInventoryItemCount(_ticketId, -1);
			player.sendPacket(new ExFestivalBmInfo(_ticketId, ticketAmount, TICKET_AMOUNT_PER_GAME));
			player.sendPacket(new ExFestivalBmAllItemInfo(getSortedRewards()));
		}
		else
		{
			LIST_TO_UPDATE.remove(player);
		}
	}
	
	/**
	 * Sends a reward mail to all online members of the player's clan.<br>
	 * This method checks if the {@code Player} and their {@code Clan} are not null.<br>
	 * It then iterates through all online members to trigger the {@code sendMail} method.
	 * @param player The {@code Player} whose clan rewards should be distributed.
	 */
	private void sendClanReward(Player player)
	{
		if (player != null)
		{
			final Clan clan = player.getClan();
			if (clan == null)
			{
				return;
			}
			
			LOGGER.warning(getClass().getSimpleName() + ": Give Clan Reward->: clan=" + clan + " player=" + player);
			clan.getOnlineMembers(player.getObjectId()).forEach(this::sendMail);
		}
		else
		{
			LOGGER.warning(getClass().getSimpleName() + ": Player is Null!");
		}
	}
	
	/**
	 * Sends a reward mail to the specified player.<br>
	 * This method notifies clan members about a shared prize.<br>
	 * It includes an additional item as an attachment.
	 * @param player The {@code Player} who will receive the mail.
	 */
	private void sendMail(Player player)
	{
		final MailMessage message = new MailMessage(player.getObjectId(), "Secret Shop Clan Reward", "Your clan member won one of the main prize of the event. All (online) clan members receive an additional reward.", MailType.REGULAR);
		final Mail attachment = message.createAttachments();
		attachment.addItem(ItemProcessType.REWARD, CLAN_REWARD_ITEM_ID, CLAN_REWARD_ITEM_COUNT, null, null);
		MailManager.getInstance().sendMessage(message);
	}
	
	/**
	 * Initializes the active rewards for a specific game.<br>
	 * This method separates rewards into top and standard categories.<br>
	 * It shuffles the top rewards and selects 3 to be active.
	 * @param currentGame The unique identifier for the current game.
	 */
	private void initRewards(int currentGame)
	{
		_activeRewards = new ArrayList<>();
		final List<SecretShopRewardHolder> topRewards = new ArrayList<>();
		final List<SecretShopRewardHolder> rewards = new ArrayList<>();
		for (SecretShopRewardHolder eventReward : _rewardData.get(currentGame))
		{
			final SecretShopRewardHolder reward = new SecretShopRewardHolder(eventReward);
			switch (reward._grade)
			{
				case 3: // 1 Star
				case 2: // 2 Star
				{
					rewards.add(reward);
					break;
				}
				case 1: // 3 Star
				{
					topRewards.add(reward);
					break;
				}
				default:
				{
					LOGGER.warning(getClass().getSimpleName() + ": Incorrect reward grade " + reward._grade);
					break;
				}
			}
		}
		
		_activeRewards.clear();
		Collections.shuffle(topRewards);
		_activeRewards.addAll(topRewards.stream().limit(3).toList());
		_activeRewards.addAll(rewards);
	}
	
	/**
	 * Updates the event information for all players in the update list.<br>
	 * It removes offline or detached players from {@code LIST_TO_UPDATE}.<br>
	 * It sends the latest rewards to online players using {@code sendInfo}.<br>
	 * It also sends a packet containing sorted rewards via {@code ExFestivalBmAllItemInfo}.
	 */
	private void updateInfo()
	{
		final Iterator<Player> iterator = LIST_TO_UPDATE.iterator();
		while (iterator.hasNext())
		{
			final Player player = iterator.next();
			final GameClient client = player.getClient();
			if (!player.isOnline() || (client == null) || client.isDetached())
			{
				LIST_TO_UPDATE.remove(player);
			}
			else
			{
				sendInfo(player);
				player.sendPacket(new ExFestivalBmAllItemInfo(getSortedRewards()));
			}
		}
	}
	
	/**
	 * Sends the current event information to a specific player.<br>
	 * This method checks if the event is active before sending the packet.<br>
	 * It provides details about rewards and timing to the {@code Player}.
	 * @param player The {@code Player} who will receive the information.
	 */
	public void sendInfo(Player player)
	{
		if (!_isEventPeriod || (player == null))
		{
			return;
		}
		
		player.sendPacket(new ExFestivalBmTopItemInfo(_exchangeEnabled ? getNextEndTime() : getNextStartTime(), _exchangeEnabled, _activeRewards));
	}
	
	/**
	 * Sends the event information to all online players.<br>
	 * This method checks if the event is currently active before broadcasting.<br>
	 * It uses {@code World.broadcastToAllOnlinePlayers} to send the data.
	 */
	private void broadcastInfo()
	{
		if (!_isEventPeriod)
		{
			return;
		}
		
		World.broadcastToAllOnlinePlayers(new ExFestivalBmTopItemInfo(_exchangeEnabled ? getNextEndTime() : getNextStartTime(), _exchangeEnabled, _activeRewards));
	}
	
	/**
	 * Retrieves a sorted list of active rewards.<br>
	 * The rewards are ordered by their unique ID.
	 * @return A {@code List} of {@code SecretShopRewardHolder} objects sorted by ID.
	 */
	private List<SecretShopRewardHolder> getSortedRewards()
	{
		final List<SecretShopRewardHolder> showRewards = new ArrayList<>(_activeRewards);
		showRewards.sort(Comparator.comparingInt(SecretShopRewardHolder::getId));
		return showRewards;
	}
	
	/**
	 * Calculates the time remaining until the next event start.<br>
	 * It checks if the current time is past the scheduled start hour and minute.<br>
	 * If it is, it calculates the time for the following day.
	 * @return The number of milliseconds until the event starts.
	 */
	private long getNextStartTime()
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, _startHour);
		calendar.set(Calendar.MINUTE, _startMinute);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		if (System.currentTimeMillis() > calendar.getTimeInMillis())
		{
			// Schedule for the next day.
			calendar.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		return Math.max(0, calendar.getTimeInMillis() - System.currentTimeMillis());
	}
	
	/**
	 * Calculates the remaining time until the event ends.<br>
	 * It returns the difference in milliseconds between the current time and the scheduled end time.<br>
	 * If the exchange is disabled, it returns 0.
	 * @return The number of milliseconds remaining until the event ends.
	 */
	private long getNextEndTime()
	{
		if (!_exchangeEnabled)
		{
			return 0; // No end time if exchange is not enabled.
		}
		
		final Calendar endTime = Calendar.getInstance();
		endTime.set(Calendar.HOUR_OF_DAY, _endHour);
		endTime.set(Calendar.MINUTE, _endMinute);
		endTime.set(Calendar.SECOND, 0);
		endTime.set(Calendar.MILLISECOND, 0);
		
		return Math.max(0, endTime.getTimeInMillis() - System.currentTimeMillis());
	}
	
	/**
	 * Checks if the current time falls within the active exchange window.<br>
	 * This method verifies that the event is active and the current hour and minute are between the start and end times.
	 * @return {@code true} if the current time is within the allowed period, {@code false} otherwise.
	 */
	private boolean isExchangePeriod()
	{
		if (!_isEventPeriod)
		{
			return false;
		}
		
		final Calendar calendar = Calendar.getInstance();
		final int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
		final int currentMinute = calendar.get(Calendar.MINUTE);
		return ((currentHour > _startHour) || ((currentHour == _startHour) && (currentMinute >= _startMinute))) && ((currentHour < _endHour) || ((currentHour == _endHour) && (currentMinute < _endMinute)));
	}
	
	/**
	 * Checks if there are any rewards currently available for exchange.<br>
	 * It scans the active reward list to see if at least one item is ready.
	 * @return {@code true} if any rewards can be claimed, {@code false} otherwise.
	 */
	private boolean hasAvailableRewards()
	{
		return _activeRewards.stream().anyMatch(SecretShopRewardHolder::hasAvailableReward);
	}
	
	/**
	 * Checks if all top-grade rewards have been claimed.<br>
	 * It iterates through the active rewards to see if any top-grade items still have a positive amount.
	 * @return {@code true} if no top-grade rewards remain, {@code false} otherwise.
	 */
	private boolean noTopGradeRewardsRemaining()
	{
		for (SecretShopRewardHolder reward : _activeRewards)
		{
			if (reward.isTopGrade() && (reward._currentAmount > 0))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if the current time falls within the active event period.
	 * @return {@code true} if the event is currently active, {@code false} otherwise.
	 */
	public boolean isEventPeriod()
	{
		return _isEventPeriod;
	}
	
	/**
	 * Adds a ticket to the queue for a specific player.<br>
	 * This method ensures that the {@code PlayerTicketData} is initialized.<br>
	 * It triggers the processing of the player's queue if it is not already running.
	 * @param player The {@code Player} who will receive the tickets.
	 */
	public void addTickets(Player player)
	{
		PLAYER_REWARD_QUEUES.computeIfAbsent(player, _ -> new PlayerTicketData());
		
		final PlayerTicketData playerData = PLAYER_REWARD_QUEUES.get(player);
		final Queue<TicketTask> queue = playerData.getQueue();
		queue.add(new TicketTask(player));
		
		if (playerData.getProcessingFlag().compareAndSet(false, true))
		{
			processPlayerQueue(playerData);
		}
	}
	
	/**
	 * Processes the queue of ticket tasks for a specific player.<br>
	 * This method runs asynchronously in a separate thread.<br>
	 * It checks if the inventory has enough space before calling {@code exchange}.<br>
	 * The processing flag is reset to {@code false} once all tasks are finished.
	 * @param playerData The data object containing the player's ticket queue and status flags.
	 */
	private void processPlayerQueue(PlayerTicketData playerData)
	{
		ThreadPool.execute(() ->
		{
			final Queue<TicketTask> queue = playerData.getQueue();
			while (!queue.isEmpty())
			{
				final TicketTask task = queue.poll();
				if (task != null)
				{
					final Player player = task.getPlayer();
					if (!player.isInventoryUnder80(false))
					{
						player.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
						continue;
					}
					
					exchange(player);
				}
				
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					break;
				}
			}
			
			playerData.getProcessingFlag().set(false);
		});
	}
	
	private static class PlayerTicketData
	{
		private final ConcurrentLinkedQueue<TicketTask> _queue = new ConcurrentLinkedQueue<>();
		private final AtomicBoolean _processingFlag = new AtomicBoolean();
		
		public Queue<TicketTask> getQueue()
		{
			return _queue;
		}
		
		public AtomicBoolean getProcessingFlag()
		{
			return _processingFlag;
		}
	}
	
	private static class TicketTask
	{
		private final Player _player;
		
		public TicketTask(Player player)
		{
			_player = player;
		}
		
		public Player getPlayer()
		{
			return _player;
		}
	}
	
	public static class SecretShopRewardHolder
	{
		private static final int TOP_GRADE = 1;
		private static final int MIDDLE_GRADE = 2;
		private static final int LOW_GRADE = 3;
		
		private final int _id;
		private final long _count;
		private final int _grade;
		private final long _totalAmount;
		private long _currentAmount;
		private final double _chance;
		
		public SecretShopRewardHolder(int id, long count, int grade, long totalAmount, long currentAmount, double chance)
		{
			_id = id;
			_count = count;
			_grade = grade;
			_totalAmount = totalAmount;
			_currentAmount = currentAmount;
			_chance = chance;
		}
		
		public SecretShopRewardHolder(SecretShopRewardHolder reward)
		{
			_id = reward._id;
			_count = reward._count;
			_grade = reward._grade;
			_totalAmount = reward._totalAmount;
			_currentAmount = reward._currentAmount;
			_chance = reward._chance;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public long getCount()
		{
			return _count;
		}
		
		public int getGrade()
		{
			return _grade;
		}
		
		public long getTotalAmount()
		{
			return _totalAmount;
		}
		
		public long getCurrentAmount()
		{
			return _currentAmount;
		}
		
		public double getChance()
		{
			return _chance;
		}
		
		public boolean hasAvailableReward()
		{
			return _totalAmount > 0;
		}
		
		public boolean isTopGrade()
		{
			return _grade == TOP_GRADE;
		}
		
		public boolean isMiddleGrade()
		{
			return _grade == MIDDLE_GRADE;
		}
		
		public boolean isLowGrade()
		{
			return _grade == LOW_GRADE;
		}
	}
	
	/**
	 * Retrieves the singleton instance of the {@code SecretShopEventManager}.<br>
	 * This provides a global access point to manage secret shop events.
	 * @return The active {@code SecretShopEventManager} instance.
	 */
	public static SecretShopEventManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SecretShopEventManager INSTANCE = new SecretShopEventManager();
	}
}
