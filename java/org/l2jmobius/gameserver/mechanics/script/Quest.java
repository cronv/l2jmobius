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
package org.l2jmobius.gameserver.mechanics.script;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.config.custom.RandomSpawnsConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NewQuestData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.creature.TrapAction;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.holders.npc.MinionList;
import org.l2jmobius.gameserver.entity.actor.holders.player.MovieHolder;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.instance.Trap;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enchant.attribute.AttributeHolder;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.Mail;
import org.l2jmobius.gameserver.entity.itemcontainer.PetInventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerWarehouse;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.spawns.SpawnGroup;
import org.l2jmobius.gameserver.entity.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.interfaces.IPositionable;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.ItemCommissionManager;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.annotations.Id;
import org.l2jmobius.gameserver.mechanics.events.annotations.Ids;
import org.l2jmobius.gameserver.mechanics.events.annotations.NpcLevelRange;
import org.l2jmobius.gameserver.mechanics.events.annotations.NpcLevelRanges;
import org.l2jmobius.gameserver.mechanics.events.annotations.Priority;
import org.l2jmobius.gameserver.mechanics.events.annotations.Range;
import org.l2jmobius.gameserver.mechanics.events.annotations.Ranges;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttacked;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSee;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureZoneEnter;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureZoneExit;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableAggroRangeEnter;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableAttack;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableFactionCall;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableHate;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableKill;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcCanBeSeen;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcDespawn;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcEventReceived;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcFirstTalk;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMoveFinished;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMoveNodeArrived;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMoveRouteFinished;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSkillFinished;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSkillSee;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSpawn;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcTeleport;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerProfessionCancel;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerProfessionChange;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSkillLearn;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSummonSpawn;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSummonTalk;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnTrapAction;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceCreated;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceDestroy;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceEnter;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceLeave;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceStatusChange;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemBypassEvent;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemTalk;
import org.l2jmobius.gameserver.mechanics.events.holders.olympiad.OnOlympiadMatchResult;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.OnCastleSiegeFinish;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.OnCastleSiegeOwnerChange;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.OnCastleSiegeStart;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.AnnotationEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.DummyEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.RunnableEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.AbstractEventReturn;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.olympiad.CompetitionType;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.olympiad.Participant;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.NewQuest;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.NewQuestCondition;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.NewQuestReward;
import org.l2jmobius.gameserver.mechanics.script.timers.IEventTimerCancel;
import org.l2jmobius.gameserver.mechanics.script.timers.IEventTimerEvent;
import org.l2jmobius.gameserver.mechanics.script.timers.TimerExecutor;
import org.l2jmobius.gameserver.mechanics.script.timers.TimerHolder;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.Movie;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExAdenaInvenCount;
import org.l2jmobius.gameserver.network.serverpackets.ExQuestNpcLogList;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoInvenWeight;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcQuestHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.SpecialCamera;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestDialog;
import org.l2jmobius.gameserver.scripting.ScriptEngine;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Main handler for the quest system: loading, execution and progression of all quests.
 * @author Luis Arias, Mobius
 */
public class Quest implements IEventTimerEvent<String>, IEventTimerCancel<String>
{
	public static final Logger LOGGER = Logger.getLogger(Quest.class.getName());
	
	private static final String DEFAULT_NO_QUEST_MSG = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	private static final String QUEST_DELETE_FROM_CHAR_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=?";
	private static final String QUEST_DELETE_FROM_CHAR_QUERY_NON_REPEATABLE_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=? AND var!=?";
	private static final SkillHolder[] STORY_QUEST_BUFFS =
	{
		new SkillHolder(62074, 1), // Fantasia Harmony - Adventurer
		new SkillHolder(62072, 1), // Fever Melody - Adventurer
		new SkillHolder(62073, 1), // Drumbeat of Serenity - Adventurer
	};
	
	private final Map<ListenerRegisterType, Set<Integer>> _registeredIds = new ConcurrentHashMap<>();
	private final Queue<AbstractEventListener> _listeners = new PriorityBlockingQueue<>();
	private final Set<QuestCondition> _startCondition = ConcurrentHashMap.newKeySet(1);
	private final Map<String, List<QuestTimer>> _questTimers = new HashMap<>();
	private TimerExecutor<String> _timerExecutor;
	
	private final int _questId;
	private final Path _scriptFile;
	private boolean _isCustom = false;
	private NpcStringId _questNameNpcStringId;
	private int[] _questItemIds = null;
	
	private final NewQuest _questData;
	
	/**
	 * Creates a new quest, registering it as a quest when questId is greater than 0 or as a general script otherwise.
	 * @param questId the unique identifier for the quest
	 */
	public Quest(int questId)
	{
		_scriptFile = Path.of(ScriptEngine.getInstance().getCurrentLoadingScript().toUri());
		initializeAnnotationListeners();
		
		_questId = questId;
		if (questId > 0)
		{
			ScriptManager.getInstance().addQuest(this);
		}
		else
		{
			ScriptManager.getInstance().addScript(this);
		}
		
		_questData = NewQuestData.getInstance().getQuestById(questId);
		if (_questData != null)
		{
			addNewQuestConditions(_questData.getConditions(), null);
			
			final int questType = _questData.getQuestType();
			if (questType == 1)
			{
				final int startNpcId = _questData.getStartNpcId();
				if (startNpcId > 0)
				{
					addFirstTalkId(startNpcId);
				}
				
				final int endNpcId = _questData.getEndNpcId();
				if ((endNpcId > 0) && (endNpcId != startNpcId))
				{
					addFirstTalkId(endNpcId);
				}
			}
			else if (questType == 4)
			{
				final int startItemId = _questData.getStartItemId();
				if (startItemId > 0)
				{
					addItemTalkId(startItemId);
				}
			}
			
			final int itemId = _questData.getGoal().getItemId();
			if (itemId > 0)
			{
				registerQuestItems(itemId);
			}
		}
		
		onLoad();
	}
	
	/**
	 * Handles a timer event by extracting its data and dispatching to the specific event handler.
	 * @param holder the holder containing the event details
	 */
	@Override
	public void onTimerEvent(TimerHolder<String> holder)
	{
		onTimerEvent(holder.getEvent(), holder.getParams(), holder.getNpc(), holder.getPlayer());
	}
	
	/**
	 * Handles cancellation of a timer event by dispatching to the specific cancel logic.
	 * @param holder the holder containing the cancelled timer details
	 */
	@Override
	public void onTimerCancel(TimerHolder<String> holder)
	{
		onTimerCancel(holder.getEvent(), holder.getParams(), holder.getNpc(), holder.getPlayer());
	}
	
	/**
	 * Handles a specific timer event when a scheduled timer expires.
	 * @param event the unique identifier for the timer event
	 * @param params the parameters associated with the event
	 * @param npc the involved NPC
	 * @param player the involved player
	 */
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		LOGGER.warning("[" + getClass().getSimpleName() + "]: Timer event arrived at non overriden onTimerEvent method event: " + event + " npc: " + npc + " player: " + player);
	}
	
	/**
	 * Called when a specific timer event is cancelled to allow custom logic.
	 * @param event the identifier of the cancelled timer event
	 * @param params the parameters associated with the timer
	 * @param npc the involved NPC
	 * @param player the involved player
	 */
	public void onTimerCancel(String event, StatSet params, Npc npc, Player player)
	{
	}
	
	/**
	 * Retrieves the timer executor for this quest, creating one if none exists.
	 * @return the timer executor for managing quest timers
	 */
	public TimerExecutor<String> getTimers()
	{
		if (_timerExecutor == null)
		{
			synchronized (this)
			{
				if (_timerExecutor == null)
				{
					_timerExecutor = new TimerExecutor<>(this, this);
				}
			}
		}
		
		return _timerExecutor;
	}
	
	/**
	 * Checks whether this quest has any active timers.
	 * @return {@code true} if there are timers, {@code false} otherwise
	 */
	public boolean hasTimers()
	{
		return _timerExecutor != null;
	}
	
	/**
	 * Retrieves the unique identifier for this quest.
	 * @return the quest ID
	 */
	public int getId()
	{
		return _questId;
	}
	
	/**
	 * Returns the simple class name used to identify the quest type.
	 * @return the class name
	 */
	public String getName()
	{
		return getClass().getSimpleName();
	}
	
	/**
	 * Retrieves the file path for this script.
	 * @return the script file path
	 */
	public Path getScriptFile()
	{
		return _scriptFile;
	}
	
	/**
	 * Retrieves the internal package path of this class as a directory structure.
	 * @return the string representation of the class path
	 */
	public String getPath()
	{
		final String path = getClass().getName().replace('.', '/');
		return path.substring(0, path.lastIndexOf('/' + getClass().getSimpleName()));
	}
	
	/**
	 * Checks whether this quest is a custom quest.
	 * @return {@code true} if the quest is custom, {@code false} otherwise
	 */
	public boolean isCustomQuest()
	{
		return _isCustom;
	}
	
	/**
	 * Sets whether this quest is considered a custom quest.
	 * @param value the custom status to set
	 */
	public void setCustom(boolean value)
	{
		_isCustom = value;
	}
	
	/**
	 * Retrieves the string identifier for this quest, falling back to a value derived from the quest ID.
	 * @return the quest string ID
	 */
	public int getNpcStringId()
	{
		return _questNameNpcStringId != null ? _questNameNpcStringId.getId() / 100 : (_questId > 10000 ? _questId - 5000 : _questId);
	}
	
	/**
	 * Retrieves the NPC string ID associated with the quest name.
	 * @return the NPC string ID of the quest name
	 */
	public NpcStringId getQuestNameNpcStringId()
	{
		return _questNameNpcStringId;
	}
	
	/**
	 * Sets the NPC string ID used as the quest name.
	 * @param npcStringId the NPC string ID to assign
	 */
	public void setQuestNameNpcStringId(NpcStringId npcStringId)
	{
		_questNameNpcStringId = npcStringId;
	}
	
	/**
	 * Reloads the quest script from the file system, unloading it first.
	 */
	public void reload()
	{
		unload();
		
		try
		{
			ScriptEngine.getInstance().executeScript(_scriptFile);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to reload script!", e);
		}
	}
	
	/**
	 * Unloads the quest, removing all active timers and associated data.
	 */
	public void unload()
	{
		unload(true);
	}
	
	/**
	 * Unloads the quest, saving state, cancelling timers, and removing registered listeners.
	 * @param removeFromList set to {@code true} to remove this script from the script manager list
	 */
	public void unload(boolean removeFromList)
	{
		onSave();
		
		// Cancel all pending timers before reloading.
		// If timers ought to be restarted, the quest can take care of it with its code (example: save global data indicating what timer must be restarted).
		for (List<QuestTimer> timers : _questTimers.values())
		{
			for (QuestTimer timer : timers)
			{
				timer.cancel();
			}
			
			timers.clear();
		}
		
		_questTimers.clear();
		
		if (removeFromList)
		{
			ScriptManager.getInstance().removeScript(this);
		}
		
		_listeners.forEach(AbstractEventListener::unregisterMe);
		_listeners.clear();
		if (_timerExecutor != null)
		{
			_timerExecutor.cancelAllTimers();
		}
	}
	
	/**
	 * Called when the quest script is loaded to initialize necessary data or resources.
	 */
	protected void onLoad()
	{
	}
	
	/**
	 * Called when the quest data needs to be saved to handle persistence logic.
	 */
	public void onSave()
	{
	}
	
	/**
	 * Creates a new quest state for the given player initialized with the CREATED status.
	 * @param player the player who will start the quest
	 * @return a new quest state instance
	 */
	public QuestState newQuestState(Player player)
	{
		return new QuestState(this, player, State.CREATED);
	}
	
	/**
	 * Retrieves the current quest state for a player, optionally initializing a new one if none exists.
	 * @param player the player whose quest state is retrieved
	 * @param initIfNone whether to initialize a new state if none is found
	 * @return the quest state, or {@code null} if not initialized
	 */
	public QuestState getQuestState(Player player, boolean initIfNone)
	{
		final QuestState qs = player.getQuestState(getName());
		if ((qs != null) || !initIfNone)
		{
			return qs;
		}
		
		return newQuestState(player);
	}
	
	/**
	 * Starts a non-repeating quest timer for a specific event.
	 * @param name the unique identifier for the timer event
	 * @param time the duration of the timer in milliseconds
	 * @param npc the associated NPC
	 * @param player the associated player
	 */
	public void startQuestTimer(String name, long time, Npc npc, Player player)
	{
		startQuestTimer(name, time, npc, player, false);
	}
	
	/**
	 * Retrieves all active timers for this quest keyed by timer name.
	 * @return a map containing the quest timers
	 */
	public Map<String, List<QuestTimer>> getQuestTimers()
	{
		return _questTimers;
	}
	
	/**
	 * Starts a quest timer, adding it only if no matching timer already exists for the given entities.
	 * @param name the unique identifier for the timer event
	 * @param time the duration of the timer in milliseconds
	 * @param npc the associated NPC, or {@code null} as a wildcard
	 * @param player the associated player, or {@code null} as a wildcard
	 * @param repeating whether the timer should restart automatically after finishing
	 */
	public void startQuestTimer(String name, long time, Npc npc, Player player, boolean repeating)
	{
		if (name == null)
		{
			return;
		}
		
		synchronized (_questTimers)
		{
			_questTimers.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>());
			
			// If there exists a timer with this name, allow the timer only if the [npc, player] set is unique nulls act as wildcards.
			if (getQuestTimer(name, npc, player) == null)
			{
				_questTimers.get(name).add(new QuestTimer(this, name, time, npc, player, repeating));
			}
		}
	}
	
	/**
	 * Retrieves the quest timer matching the given name, NPC, and player.
	 * @param name the unique identifier for the quest timer
	 * @param npc the associated NPC
	 * @param player the associated player
	 * @return the matching quest timer, or {@code null}
	 */
	public QuestTimer getQuestTimer(String name, Npc npc, Player player)
	{
		if (name == null)
		{
			return null;
		}
		
		final List<QuestTimer> timers = _questTimers.get(name);
		if ((timers == null) || timers.isEmpty())
		{
			return null;
		}
		
		for (QuestTimer timer : timers)
		{
			if ((timer != null) && timer.equals(this, name, npc, player))
			{
				return timer;
			}
		}
		
		return null;
	}
	
	/**
	 * Stops and clears all active timers associated with the given name.
	 * @param name the unique identifier of the quest timers to cancel
	 */
	public void cancelQuestTimers(String name)
	{
		if (name == null)
		{
			return;
		}
		
		final List<QuestTimer> timers = _questTimers.get(name);
		if ((timers == null) || timers.isEmpty())
		{
			return;
		}
		
		for (QuestTimer timer : timers)
		{
			if (timer != null)
			{
				timer.cancel();
			}
		}
		
		timers.clear();
	}
	
	/**
	 * Cancels the quest timers matching the given name, NPC, and player context.
	 * @param name the unique identifier of the quest timer to cancel
	 * @param npc the associated NPC
	 * @param player the associated player
	 */
	public void cancelQuestTimer(String name, Npc npc, Player player)
	{
		if (name == null)
		{
			return;
		}
		
		final List<QuestTimer> timers = _questTimers.get(name);
		if ((timers == null) || timers.isEmpty())
		{
			return;
		}
		
		for (QuestTimer timer : timers)
		{
			if ((timer != null) && timer.equals(this, name, npc, player))
			{
				timer.cancel();
			}
		}
	}
	
	/**
	 * Removes a specific quest timer from the active list.
	 * @param timer the quest timer to remove
	 */
	public void removeQuestTimer(QuestTimer timer)
	{
		if (timer == null)
		{
			return;
		}
		
		final List<QuestTimer> timers = _questTimers.get(timer.toString());
		if (timers != null)
		{
			timers.remove(timer);
		}
	}
	
	// These are methods to call within the core to call the quest events.
	
	/**
	 * Notifies the system that an NPC has teleported, logging any exception.
	 * @param npc the NPC that teleported
	 */
	public void notifyTeleport(Npc npc)
	{
		try
		{
			onTeleport(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onTeleport() in notifyTeleport(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Triggers a script event for an NPC and player, disabling simulated talking before execution.
	 * @param event the name of the event to trigger
	 * @param npc the involved NPC
	 * @param player the interacting player
	 */
	public void notifyEvent(String event, Npc npc, Player player)
	{
		String res = null;
		try
		{
			// Simulated talk should not exist when event runs.
			if (player != null)
			{
				player.setSimulatedTalking(false);
			}
			
			res = onEvent(event, npc, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		
		showResult(player, res, npc);
	}
	
	/**
	 * Handles a player talking to an NPC, displaying start conditions or default dialogue as appropriate.
	 * @param npc the NPC being interacted with
	 * @param player the player initiating the conversation
	 */
	public void notifyTalk(Npc npc, Player player)
	{
		String res = null;
		try
		{
			final Set<Quest> startingQuests = new HashSet<>();
			for (AbstractEventListener listener : npc.getListeners(EventType.ON_NPC_QUEST_START))
			{
				final Object owner = listener.getOwner();
				if (owner instanceof Quest)
				{
					startingQuests.add((Quest) owner);
				}
			}
			
			final String startConditionHtml = getStartConditionHtml(player, npc);
			if (startingQuests.contains(this) && (startConditionHtml != null))
			{
				res = startConditionHtml;
			}
			else
			{
				res = onTalk(npc, player, false);
			}
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		
		player.setLastQuestNpcObject(npc.getObjectId());
		showResult(player, res, npc);
	}
	
	/**
	 * Triggers the first interaction between an NPC and player and displays the result.
	 * @param npc the involved NPC
	 * @param player the player initiating the conversation
	 */
	public void notifyFirstTalk(Npc npc, Player player)
	{
		String res = null;
		try
		{
			res = onFirstTalk(npc, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		
		showResult(player, res, npc);
	}
	
	/**
	 * Notifies the player of an interaction with an item and displays the result.
	 * @param item the item being interacted with
	 * @param player the player who initiated the interaction
	 */
	public void notifyItemTalk(Item item, Player player)
	{
		String res = null;
		try
		{
			res = onItemTalk(item, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		
		showResult(player, res);
	}
	
	/**
	 * Handles dialogue triggered when a player interacts with an item.
	 * @param item the item being interacted with
	 * @param player the player who initiated the interaction
	 * @return the dialogue text, or {@code null} if none exists
	 */
	public String onItemTalk(Item item, Player player)
	{
		return null;
	}
	
	/**
	 * Notifies the script that an event has been received, logging any exception.
	 * @param eventName the name of the event being triggered
	 * @param sender the NPC that sent the event
	 * @param receiver the NPC that received the event
	 * @param reference a world object associated with the event
	 */
	public void notifyEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		try
		{
			onEventReceived(eventName, sender, receiver, reference);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onEventReceived() in notifyEventReceived(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Notifies the script that an Olympiad match has ended, logging any exception.
	 * @param winner the participant who won the match
	 * @param looser the participant who lost the match
	 * @param type the competition type of the finished match
	 */
	public void notifyOlympiadMatch(Participant winner, Participant looser, CompetitionType type)
	{
		try
		{
			onOlympiadMatchFinish(winner, looser, type);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Execution on onOlympiadMatchFinish() in notifyOlympiadMatch(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Notifies the system that an NPC has finished moving, logging any exception.
	 * @param npc the NPC that completed its movement
	 */
	public void notifyMoveFinished(Npc npc)
	{
		try
		{
			onMoveFinished(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onMoveFinished() in notifyMoveFinished(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Notifies the system that an NPC has arrived at a specific node, logging any exception.
	 * @param npc the NPC that reached the destination
	 */
	public void notifyNodeArrived(Npc npc)
	{
		try
		{
			onNodeArrived(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onNodeArrived() in notifyNodeArrived(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Notifies the system that an NPC has finished its route, logging any exception.
	 * @param npc the NPC that completed the route
	 */
	public void notifyRouteFinished(Npc npc)
	{
		try
		{
			onRouteFinished(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onRouteFinished() in notifyRouteFinished(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Notifies the script that a player can see an NPC, returning {@code false} on exception.
	 * @param npc the NPC being seen
	 * @param player the player who can see the NPC
	 * @return {@code true} if the script logic succeeds, otherwise {@code false}
	 */
	public boolean notifyOnCanSeeMe(Npc npc, Player player)
	{
		try
		{
			return onCanSeeMe(npc, player);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onCanSeeMe() in notifyOnCanSeeMe(): " + e.getMessage(), e);
		}
		
		return false;
	}
	
	// These are methods that java calls to invoke scripts.
	
	/**
	 * Handles the logic when an NPC is attacked by a player.
	 * @param npc the target NPC being attacked
	 * @param attacker the player performing the attack
	 * @param damage the amount of damage dealt
	 * @param isSummon whether the attack is a summon action
	 */
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
	}
	
	/**
	 * Handles the logic when an NPC is attacked by a player using a skill.
	 * @param npc the target NPC being attacked
	 * @param attacker the player performing the attack
	 * @param damage the amount of damage dealt
	 * @param isSummon whether the attack comes from a summon
	 * @param skill the skill used to perform the attack
	 */
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		onAttack(npc, attacker, damage, isSummon);
	}
	
	/**
	 * Handles the death event by triggering an event with the killer and the quest state player.
	 * @param killer the creature that caused the death
	 * @param victim the creature that was killed
	 * @param qs the current quest state
	 */
	public void onDeath(Creature killer, Creature victim, QuestState qs)
	{
		onEvent("", (killer instanceof Npc) ? killer.asNpc() : null, qs.getPlayer());
	}
	
	/**
	 * Handles a specific event triggered by an NPC and player.
	 * @param event the name of the event to handle
	 * @param npc the involved NPC
	 * @param player the involved player
	 * @return {@code null}
	 */
	public String onEvent(String event, Npc npc, Player player)
	{
		return null;
	}
	
	/**
	 * Called when an NPC is killed.
	 * @param npc the NPC that was killed
	 * @param killer the player who performed the kill
	 * @param isSummon whether the kill was from a summon
	 */
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
	}
	
	/**
	 * Handles a player talking to an NPC, updating quest state and simulated talking before dispatching.
	 * @param npc the NPC being spoken to
	 * @param talker the player initiating the conversation
	 * @param simulated whether the interaction is a simulation
	 * @return the result of the internal onTalk call
	 */
	public String onTalk(Npc npc, Player talker, boolean simulated)
	{
		final QuestState qs = talker.getQuestState(getName());
		if (qs != null)
		{
			qs.setSimulated(simulated);
		}
		
		talker.setSimulatedTalking(simulated);
		return onTalk(npc, talker);
	}
	
	/**
	 * Handles the interaction when a player talks to an NPC.
	 * @param npc the NPC being interacted with
	 * @param talker the player who initiated the conversation
	 * @return the response, or {@code null}
	 */
	public String onTalk(Npc npc, Player talker)
	{
		return null;
	}
	
	/**
	 * Handles the logic when a player talks to an NPC for the first time.
	 * @param npc the NPC being interacted with
	 * @param player the player who initiated the conversation
	 * @return the dialogue or response, or {@code null}
	 */
	public String onFirstTalk(Npc npc, Player player)
	{
		return null;
	}
	
	/**
	 * Handles an event triggered by a specific item interacting with a player.
	 * @param item the item that triggered the event
	 * @param player the player who interacted with the item
	 * @param event the name of the specific event to handle
	 */
	public void onItemEvent(Item item, Player player, String event)
	{
	}
	
	/**
	 * Called when an NPC acquires a list of skills for a player.
	 * @param npc the NPC acquiring the skills
	 * @param player the player interacting with the NPC
	 */
	public void onAcquireSkillList(Npc npc, Player player)
	{
	}
	
	/**
	 * Called when a player acquires information about a skill from an NPC.
	 * @param npc the involved NPC
	 * @param player the player acquiring the skill info
	 * @param skill the skill being processed
	 */
	public void onAcquireSkillInfo(Npc npc, Player player, Skill skill)
	{
	}
	
	/**
	 * Handles the logic when a player acquires a skill from an NPC.
	 * @param npc the NPC that provides the skill
	 * @param player the player who receives the skill
	 * @param skill the skill being acquired
	 * @param type the acquire skill type defining how the skill is obtained
	 */
	public void onAcquireSkill(Npc npc, Player player, Skill skill, AcquireSkillType type)
	{
	}
	
	/**
	 * Handles the logic when an NPC casts a skill that is seen.
	 * @param npc the NPC casting the skill
	 * @param caster the player who initiated the action
	 * @param skill the skill being used
	 * @param targets the world objects targeted by the skill
	 * @param isSummon whether the skill is a summon
	 */
	public void onSkillSee(Npc npc, Player caster, Skill skill, Collection<WorldObject> targets, boolean isSummon)
	{
	}
	
	/**
	 * Called when a spell has finished executing.
	 * @param npc the NPC who cast the spell
	 * @param player the player who was targeted by the spell
	 * @param skill the skill representing the spell used
	 */
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
	}
	
	/**
	 * Handles the logic for a specific trap action when a creature interacts with a trap.
	 * @param trap the trap that was triggered
	 * @param trigger the creature that activated the trap
	 * @param action the trap action being performed
	 */
	public void onTrapAction(Trap trap, Creature trigger, TrapAction action)
	{
	}
	
	/**
	 * Called when an NPC spawns.
	 * @param npc the NPC that spawned
	 */
	public void onSpawn(Npc npc)
	{
	}
	
	/**
	 * Called when an NPC teleports.
	 * @param npc the NPC that teleported
	 */
	protected void onTeleport(Npc npc)
	{
	}
	
	/**
	 * Called when an NPC receives a faction call.
	 * @param npc the NPC receiving the call
	 * @param caller the NPC initiating the call
	 * @param attacker the attacking player, if any
	 * @param isSummon whether triggered by a summon
	 */
	public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
	}
	
	/**
	 * Called when a player enters an NPC aggro range.
	 * @param npc the NPC whose aggro range was entered
	 * @param player the player who entered
	 * @param isSummon whether the entity is a summon
	 */
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
	}
	
	/**
	 * Called when an NPC sees a creature.
	 * @param npc the observing NPC
	 * @param creature the spotted creature
	 */
	public void onCreatureSee(Npc npc, Creature creature)
	{
	}
	
	/**
	 * Called when a player enters the world.
	 * @param player the player who entered
	 */
	public void onEnterWorld(Player player)
	{
	}
	
	/**
	 * Called when a creature enters a zone.
	 * @param creature the creature that entered
	 * @param zone the entered zone type
	 */
	public void onEnterZone(Creature creature, ZoneType zone)
	{
	}
	
	/**
	 * Called when a creature leaves a zone.
	 * @param creature the creature that exited
	 * @param zone the exited zone type
	 */
	public void onExitZone(Creature creature, ZoneType zone)
	{
	}
	
	/**
	 * Handles an event received by the quest script.
	 * @param eventName the name of the event
	 * @param sender the NPC that sent the event
	 * @param receiver the NPC that received the event
	 * @param reference the world object associated with the event
	 * @return {@code null}
	 */
	public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		return null;
	}
	
	/**
	 * Called when an Olympiad match finishes.
	 * @param winner the winning participant
	 * @param looser the losing participant
	 * @param type the competition type
	 */
	public void onOlympiadMatchFinish(Participant winner, Participant looser, CompetitionType type)
	{
	}
	
	/**
	 * Called when a player loses an Olympiad match.
	 * @param loser the player who lost
	 * @param type the competition type
	 */
	public void onOlympiadLose(Player loser, CompetitionType type)
	{
	}
	
	/**
	 * Called when an NPC finishes moving.
	 * @param npc the NPC that completed its move
	 */
	public void onMoveFinished(Npc npc)
	{
	}
	
	/**
	 * Called when an NPC arrives at a node.
	 * @param npc the NPC that arrived
	 */
	public void onNodeArrived(Npc npc)
	{
	}
	
	/**
	 * Called when an NPC finishes its route.
	 * @param npc the NPC that completed its route
	 */
	public void onRouteFinished(Npc npc)
	{
	}
	
	/**
	 * Determines whether a mob should hate a player.
	 * @param mob the attackable entity feeling hate
	 * @param player the player causing the hate
	 * @param isSummon whether the target is a summon
	 * @return {@code true} by default
	 */
	public boolean onNpcHate(Attackable mob, Player player, boolean isSummon)
	{
		return true;
	}
	
	/**
	 * Called when a summon is spawned.
	 * @param summon the summon that spawned
	 */
	public void onSummonSpawn(Summon summon)
	{
	}
	
	/**
	 * Called when a summon is talked to.
	 * @param summon the summon being processed
	 */
	public void onSummonTalk(Summon summon)
	{
	}
	
	/**
	 * Called when an instance is created.
	 * @param instance the created instance
	 * @param player the player who initiated the creation
	 */
	public void onInstanceCreated(Instance instance, Player player)
	{
	}
	
	/**
	 * Called when an instance is destroyed.
	 * @param instance the instance being destroyed
	 */
	public void onInstanceDestroy(Instance instance)
	{
	}
	
	/**
	 * Called when a player enters an instance.
	 * @param player the player who entered
	 * @param instance the instance being entered
	 */
	public void onInstanceEnter(Player player, Instance instance)
	{
	}
	
	/**
	 * Called when a player leaves an instance.
	 * @param player the player who left
	 * @param instance the instance being left
	 */
	public void onInstanceLeave(Player player, Instance instance)
	{
	}
	
	/**
	 * Called when an NPC despawns.
	 * @param npc the NPC that despawned
	 */
	public void onNpcDespawn(Npc npc)
	{
	}
	
	/**
	 * Checks whether a player can see an NPC.
	 * @param npc the NPC to check visibility for
	 * @param player the player attempting to see the NPC
	 * @return {@code false} by default
	 */
	public boolean onCanSeeMe(Npc npc, Player player)
	{
		return false;
	}
	
	/**
	 * Logs a script error and shows the stack trace to the player if a GM.
	 * @param player the player who triggered the error
	 * @param t the throwable containing the error details
	 * @return {@code true} if the error was shown to the player, {@code false} otherwise
	 */
	public boolean showError(Player player, Throwable t)
	{
		LOGGER.log(Level.WARNING, getScriptFile().toAbsolutePath().toString(), t);
		if (t.getMessage() == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + t.getMessage());
		}
		
		if ((player != null) && player.getAccessLevel().isGm())
		{
			final String res = "<html><body><title>Script error</title>" + TraceUtil.getStackTrace(t) + "</body></html>";
			return showResult(player, res);
		}
		
		return false;
	}
	
	/**
	 * Displays a result message to the player.
	 * @param player the player who will see the message
	 * @param res the result text to display
	 * @return {@code true} if the input is invalid and nothing was shown, {@code false} otherwise
	 */
	public boolean showResult(Player player, String res)
	{
		return showResult(player, res, null);
	}
	
	/**
	 * Displays a result as an HTML file, raw HTML, or plain text based on its content.
	 * @param player the player who will receive the message
	 * @param res the content to display
	 * @param npc the associated NPC, if any
	 * @return {@code true} if the input is invalid and nothing was shown, {@code false} otherwise
	 */
	public boolean showResult(Player player, String res, Npc npc)
	{
		if ((res == null) || res.isEmpty() || (player == null))
		{
			return true;
		}
		
		if (res.endsWith(".htm") || res.endsWith(".html"))
		{
			showHtmlFile(player, res, npc);
		}
		else if (res.startsWith("<html>"))
		{
			final int objectId = npc != null ? npc.getObjectId() : 0;
			final NpcHtmlMessage npcReply = new NpcHtmlMessage(objectId, res);
			if (npc != null)
			{
				npcReply.replace("%objectId%", npc.getObjectId());
			}
			
			npcReply.replace("%playername%", player.getName());
			player.sendPacket(npcReply);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			player.sendMessage(res);
		}
		
		return false;
	}
	
	/**
	 * Loads a player quests and quest variables from the database, removing invalid data when autodelete is enabled.
	 * @param player the player whose quest data is loaded
	 */
	public static void playerEnter(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ?");
			PreparedStatement invalidQuestDataVar = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ? AND var = ?");
			PreparedStatement ps1 = con.prepareStatement("SELECT name, value FROM character_quests WHERE charId = ? AND var = ?"))
		{
			// Get list of quests owned by the player from database.
			final int objectId = player.getObjectId();
			ps1.setInt(1, objectId);
			ps1.setString(2, "<state>");
			try (ResultSet rs = ps1.executeQuery())
			{
				while (rs.next())
				{
					// Get the ID of the quest and its state.
					final String questId = rs.getString("name");
					final String statename = rs.getString("value");
					
					// Search quest associated with the ID.
					final Quest q = ScriptManager.getInstance().getScript(questId);
					if (q == null)
					{
						LOGGER.finer("Unknown quest " + questId + " for " + player);
						if (GeneralConfig.AUTODELETE_INVALID_QUEST_DATA)
						{
							invalidQuestData.setInt(1, objectId);
							invalidQuestData.setString(2, questId);
							invalidQuestData.executeUpdate();
						}
						continue;
					}
					
					// Create a new QuestState for the player that will be added to the player's list of quests.
					new QuestState(q, player, State.getStateId(statename));
				}
			}
			
			// Get list of quests owned by the player from the DB in order to add variables used in the quest.
			try (PreparedStatement ps2 = con.prepareStatement("SELECT name, var, value FROM character_quests WHERE charId = ? AND var <> ?"))
			{
				ps2.setInt(1, objectId);
				ps2.setString(2, "<state>");
				try (ResultSet rs = ps2.executeQuery())
				{
					while (rs.next())
					{
						final String questId = rs.getString("name");
						final String var = rs.getString("var");
						final String value = rs.getString("value");
						
						// Get the QuestState saved in the loop before.
						final Quest quest = ScriptManager.getInstance().getScript(questId);
						if (quest != null)
						{
							final QuestState qs = quest.getQuestState(player, true);
							if (qs == null)
							{
								LOGGER.finer("Lost variable " + var + " in quest " + questId + " for " + player);
								
								if (GeneralConfig.AUTODELETE_INVALID_QUEST_DATA)
								{
									invalidQuestDataVar.setInt(1, objectId);
									invalidQuestDataVar.setString(2, questId);
									invalidQuestDataVar.setString(3, var);
									invalidQuestDataVar.executeUpdate();
								}
								continue;
							}
							
							// Add parameter to the quest.
							qs.setInternal(var, value);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not insert char quest:", e);
		}
	}
	
	/**
	 * Inserts or updates a quest variable in the database.
	 * @param qs the quest state holding player information
	 * @param var the variable name to store
	 * @param value the value to assign
	 */
	public static void createQuestVarInDb(QuestState qs, String var, String value)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_quests (charId,name,var,value) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE value=?"))
		{
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.setString(4, value);
			statement.setString(5, value);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not insert char quest:", e);
		}
	}
	
	/**
	 * Updates a quest variable value in the database.
	 * @param qs the quest state holding the quest information
	 * @param var the variable name to update
	 * @param value the new value to assign
	 */
	public static void updateQuestVarInDb(QuestState qs, String var, String value)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE character_quests SET value=? WHERE charId=? AND name=? AND var = ?"))
		{
			statement.setString(1, value);
			statement.setInt(2, qs.getPlayer().getObjectId());
			statement.setString(3, qs.getQuestName());
			statement.setString(4, var);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not update char quest:", e);
		}
	}
	
	/**
	 * Deletes a quest variable from the database.
	 * @param qs the quest state holding player and quest information
	 * @param var the variable name to delete
	 */
	public static void deleteQuestVarInDb(QuestState qs, String var)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=? AND name=? AND var=?"))
		{
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Unable to delete char quest!", e);
		}
	}
	
	/**
	 * Removes a quest from a character database record, handling repeatable and non-repeatable quests.
	 * @param qs the quest state holding player and quest details
	 * @param repeatable whether the quest is repeatable
	 */
	public static void deleteQuestInDb(QuestState qs, boolean repeatable)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(repeatable ? QUEST_DELETE_FROM_CHAR_QUERY : QUEST_DELETE_FROM_CHAR_QUERY_NON_REPEATABLE_QUERY))
		{
			ps.setInt(1, qs.getPlayer().getObjectId());
			ps.setString(2, qs.getQuestName());
			if (!repeatable)
			{
				ps.setString(3, "<state>");
			}
			
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not delete char quest:", e);
		}
	}
	
	/**
	 * Saves the current quest state to the database.
	 * @param qs the quest state to save
	 */
	public static void createQuestInDb(QuestState qs)
	{
		createQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
	}
	
	/**
	 * Updates the quest state in the database.
	 * @param qs the quest state to update
	 */
	public static void updateQuestInDb(QuestState qs)
	{
		updateQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
	}
	
	/**
	 * Returns the message shown when a player has no active quests, falling back to a default.
	 * @param player the player used to fetch cached content
	 * @return the quest notification message
	 */
	public static String getNoQuestMsg(Player player)
	{
		final String result = HtmCache.getInstance().getHtm(player, "data/html/noquest.htm");
		if ((result != null) && !result.isEmpty())
		{
			return result;
		}
		
		return DEFAULT_NO_QUEST_MSG;
	}
	
	/**
	 * Returns the HTML message for a player with no quest level reward.
	 * @param player the player to check for rewards
	 * @return the HTML message
	 */
	public static String getNoQuestLevelRewardMsg(Player player)
	{
		return HtmCache.getInstance().getHtm(player, "data/html/noquestlevelreward.html");
	}
	
	/**
	 * Returns the message for a one-time quest already completed.
	 * @param player the player attempting to access the quest
	 * @return the completion message
	 */
	public static String getAlreadyCompletedMsg(Player player)
	{
		return getAlreadyCompletedMsg(player, QuestFinishType.ONE_TIME);
	}
	
	/**
	 * Returns the HTML message for an already completed quest, depending on quest type.
	 * @param player the player attempting to access the quest
	 * @param type the quest type being checked
	 * @return the cached HTML content
	 */
	public static String getAlreadyCompletedMsg(Player player, QuestFinishType type)
	{
		return HtmCache.getInstance().getHtm(player, (type == QuestFinishType.ONE_TIME ? "data/html/alreadyCompleted.html" : "data/html/alreadyCompletedDaily.html"));
	}
	
	// TODO: Clean up these methods
	/**
	 * Sets the NPC ID that starts this quest.
	 * @param npcId the ID of the starting NPC
	 */
	public void addStartNpc(int npcId)
	{
		setNpcQuestStartId(npcId);
	}
	
	/**
	 * Registers an NPC ID for the first talk event.
	 * @param npcId the ID of the NPC to register
	 */
	public void addFirstTalkId(int npcId)
	{
		setNpcFirstTalkId(event -> notifyFirstTalk(event.getNpc(), event.getPlayer()), npcId);
	}
	
	/**
	 * Registers an NPC ID for the talk event.
	 * @param npcId the ID of the NPC to set
	 */
	public void addTalkId(int npcId)
	{
		setNpcTalkId(npcId);
	}
	
	/**
	 * Registers an NPC ID to trigger the kill event and update the player log.
	 * @param npcId the ID of the NPC tracked for kills
	 */
	public void addKillId(int npcId)
	{
		setAttackableKillId(kill ->
		{
			final Player player = kill.getAttacker();
			onKill(kill.getTarget(), player, kill.isSummon());
			
			if (!getNpcLogList(player).isEmpty())
			{
				sendNpcLogList(player);
			}
		}, npcId);
	}
	
	/**
	 * Registers an NPC ID to trigger the attack event.
	 * @param npcId the ID of the NPC
	 */
	public void addAttackId(int npcId)
	{
		setAttackableAttackId(attack -> onAttack(attack.getTarget(), attack.getAttacker(), attack.getDamage(), attack.isSummon(), attack.getSkill()), npcId);
	}
	
	/**
	 * Sets the NPC IDs that start this quest.
	 * @param npcIds the IDs of the starting NPCs
	 */
	public void addStartNpc(int... npcIds)
	{
		setNpcQuestStartId(npcIds);
	}
	
	/**
	 * Sets the NPC IDs that start this quest.
	 * @param npcIds the IDs of the starting NPCs
	 */
	public void addStartNpc(Collection<Integer> npcIds)
	{
		setNpcQuestStartId(npcIds);
	}
	
	/**
	 * Registers NPC IDs for the first talk event.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFirstTalkId(int... npcIds)
	{
		setNpcFirstTalkId(event -> notifyFirstTalk(event.getNpc(), event.getPlayer()), npcIds);
	}
	
	/**
	 * Registers NPC IDs for the first talk event.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFirstTalkId(Collection<Integer> npcIds)
	{
		setNpcFirstTalkId(event -> notifyFirstTalk(event.getNpc(), event.getPlayer()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger skill acquisition events.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addAcquireSkillId(int... npcIds)
	{
		setPlayerSkillLearnId(event -> onAcquireSkill(event.getTrainer(), event.getPlayer(), event.getSkill(), event.getAcquireType()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger skill acquisition events.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addAcquireSkillId(Collection<Integer> npcIds)
	{
		setPlayerSkillLearnId(event -> onAcquireSkill(event.getTrainer(), event.getPlayer(), event.getSkill(), event.getAcquireType()), npcIds);
	}
	
	/**
	 * Registers item IDs that bypass standard event logic.
	 * @param itemIds the IDs of the items to bypass
	 */
	public void addItemBypassEventId(int... itemIds)
	{
		setItemBypassEventId(event -> onItemEvent(event.getItem(), event.getPlayer(), event.getEvent()), itemIds);
	}
	
	/**
	 * Registers item IDs that bypass standard event logic.
	 * @param itemIds the IDs of the items to bypass
	 */
	public void addItemBypassEventId(Collection<Integer> itemIds)
	{
		setItemBypassEventId(event -> onItemEvent(event.getItem(), event.getPlayer(), event.getEvent()), itemIds);
	}
	
	/**
	 * Registers item IDs that trigger an item talk event.
	 * @param itemIds the IDs of the items
	 */
	public void addItemTalkId(int... itemIds)
	{
		setItemTalkId(event -> notifyItemTalk(event.getItem(), event.getPlayer()), itemIds);
	}
	
	/**
	 * Registers item IDs that trigger an item talk event.
	 * @param itemIds the IDs of the items
	 */
	public void addItemTalkId(Collection<Integer> itemIds)
	{
		setItemTalkId(event -> notifyItemTalk(event.getItem(), event.getPlayer()), itemIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the attack event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addAttackId(int... npcIds)
	{
		setAttackableAttackId(attack -> onAttack(attack.getTarget(), attack.getAttacker(), attack.getDamage(), attack.isSummon(), attack.getSkill()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the attack event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addAttackId(Collection<Integer> npcIds)
	{
		setAttackableAttackId(attack -> onAttack(attack.getTarget(), attack.getAttacker(), attack.getDamage(), attack.isSummon(), attack.getSkill()), npcIds);
	}
	
	/**
	 * Registers NPC IDs to trigger the kill event and update the player log.
	 * @param npcIds the IDs of the NPCs to include
	 */
	public void addKillId(int... npcIds)
	{
		setAttackableKillId(kill ->
		{
			final Player player = kill.getAttacker();
			onKill(kill.getTarget(), player, kill.isSummon());
			
			if (!getNpcLogList(player).isEmpty())
			{
				sendNpcLogList(player);
			}
		}, npcIds);
	}
	
	/**
	 * Registers NPC IDs to trigger the kill event and update the player log.
	 * @param npcIds the IDs of the NPCs to track
	 */
	public void addKillId(Collection<Integer> npcIds)
	{
		setAttackableKillId(kill ->
		{
			final Player player = kill.getAttacker();
			onKill(kill.getTarget(), player, kill.isSummon());
			
			if (!getNpcLogList(player).isEmpty())
			{
				sendNpcLogList(player);
			}
		}, npcIds);
	}
	
	/**
	 * Registers NPC IDs for the talk event.
	 * @param npcIds the IDs to add
	 */
	public void addTalkId(int... npcIds)
	{
		setNpcTalkId(npcIds);
	}
	
	/**
	 * Registers NPC IDs for the talk event.
	 * @param npcIds the IDs to add
	 */
	public void addTalkId(Collection<Integer> npcIds)
	{
		setNpcTalkId(npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger a teleport event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addTeleportId(int... npcIds)
	{
		setNpcTeleportId(event -> notifyTeleport(event.getNpc()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger a teleport event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addTeleportId(Collection<Integer> npcIds)
	{
		setNpcTeleportId(event -> notifyTeleport(event.getNpc()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the spawn event.
	 * @param npcIds the IDs to add
	 */
	public void addSpawnId(int... npcIds)
	{
		setNpcSpawnId(event -> onSpawn(event.getNpc()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the spawn event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addSpawnId(Collection<Integer> npcIds)
	{
		setNpcSpawnId(event -> onSpawn(event.getNpc()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the despawn event.
	 * @param npcIds the IDs of the NPCs to monitor
	 */
	public void addDespawnId(int... npcIds)
	{
		setNpcDespawnId(event -> onNpcDespawn(event.getNpc()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the despawn event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addDespawnId(Collection<Integer> npcIds)
	{
		setNpcDespawnId(event -> onNpcDespawn(event.getNpc()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the skill see event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addSkillSeeId(int... npcIds)
	{
		setNpcSkillSeeId(event -> onSkillSee(event.getTarget(), event.getCaster(), event.getSkill(), event.getTargets(), event.isSummon()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the skill see event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addSkillSeeId(Collection<Integer> npcIds)
	{
		setNpcSkillSeeId(event -> onSkillSee(event.getTarget(), event.getCaster(), event.getSkill(), event.getTargets(), event.isSummon()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the spell finished event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addSpellFinishedId(int... npcIds)
	{
		setNpcSkillFinishedId(event -> onSpellFinished(event.getCaster(), event.getTarget(), event.getSkill()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the spell finished event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addSpellFinishedId(Collection<Integer> npcIds)
	{
		setNpcSkillFinishedId(event -> onSpellFinished(event.getCaster(), event.getTarget(), event.getSkill()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger a trap action.
	 * @param npcIds the IDs to apply this action to
	 */
	public void addTrapActionId(int... npcIds)
	{
		setTrapActionId(event -> onTrapAction(event.getTrap(), event.getTrigger(), event.getAction()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger a trap action.
	 * @param npcIds the IDs to apply this action to
	 */
	public void addTrapActionId(Collection<Integer> npcIds)
	{
		setTrapActionId(event -> onTrapAction(event.getTrap(), event.getTrigger(), event.getAction()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger faction calls.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addFactionCallId(int... npcIds)
	{
		setAttackableFactionCallId(event -> onFactionCall(event.getNpc(), event.getCaller(), event.getAttacker(), event.isSummon()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger faction calls.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addFactionCallId(Collection<Integer> npcIds)
	{
		setAttackableFactionCallId(event -> onFactionCall(event.getNpc(), event.getCaller(), event.getAttacker(), event.isSummon()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger an aggro range entry event.
	 * @param npcIds the IDs of the NPCs to monitor
	 */
	public void addAggroRangeEnterId(int... npcIds)
	{
		setAttackableAggroRangeEnterId(event -> onAggroRangeEnter(event.getNpc(), event.getPlayer(), event.isSummon()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger an aggro range entry event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addAggroRangeEnterId(Collection<Integer> npcIds)
	{
		setAttackableAggroRangeEnterId(event -> onAggroRangeEnter(event.getNpc(), event.getPlayer(), event.isSummon()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the creature see event.
	 * @param npcIds the IDs of the NPCs to track
	 */
	public void addCreatureSeeId(int... npcIds)
	{
		setCreatureSeeId(event -> onCreatureSee(event.getCreature().asNpc(), event.getSeen()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the creature see event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addCreatureSeeId(Collection<Integer> npcIds)
	{
		setCreatureSeeId(event -> onCreatureSee(event.getCreature().asNpc(), event.getSeen()), npcIds);
	}
	
	/**
	 * Registers a zone ID that triggers the creature enter zone event.
	 * @param zoneId the ID of the zone to monitor
	 */
	public void addEnterZoneId(int zoneId)
	{
		setCreatureZoneEnterId(event -> onEnterZone(event.getCreature(), event.getZone()), zoneId);
	}
	
	/**
	 * Registers zone IDs that trigger the enter zone event.
	 * @param zoneIds the IDs of the zones
	 */
	public void addEnterZoneId(int... zoneIds)
	{
		setCreatureZoneEnterId(event -> onEnterZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Registers zone IDs that trigger the enter zone event.
	 * @param zoneIds the IDs of the zones
	 */
	public void addEnterZoneId(Collection<Integer> zoneIds)
	{
		setCreatureZoneEnterId(event -> onEnterZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Registers a zone ID that triggers the exit zone event.
	 * @param zoneId the ID of the zone to monitor
	 */
	public void addExitZoneId(int zoneId)
	{
		setCreatureZoneExitId(event -> onExitZone(event.getCreature(), event.getZone()), zoneId);
	}
	
	/**
	 * Registers zone IDs that trigger the exit zone event.
	 * @param zoneIds the IDs of the zones
	 */
	public void addExitZoneId(int... zoneIds)
	{
		setCreatureZoneExitId(event -> onExitZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Registers zone IDs that trigger the exit zone event.
	 * @param zoneIds the IDs of the zones
	 */
	public void addExitZoneId(Collection<Integer> zoneIds)
	{
		setCreatureZoneExitId(event -> onExitZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Registers NPC IDs that receive event notifications.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addEventReceivedId(int... npcIds)
	{
		setNpcEventReceivedId(event -> notifyEventReceived(event.getEventName(), event.getSender(), event.getReceiver(), event.getReference()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that receive event notifications.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addEventReceivedId(Collection<Integer> npcIds)
	{
		setNpcEventReceivedId(event -> notifyEventReceived(event.getEventName(), event.getSender(), event.getReceiver(), event.getReference()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the move finished event.
	 * @param npcIds the IDs of the NPCs to monitor
	 */
	public void addMoveFinishedId(int... npcIds)
	{
		setNpcMoveFinishedId(event -> notifyMoveFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the move finished event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addMoveFinishedId(Collection<Integer> npcIds)
	{
		setNpcMoveFinishedId(event -> notifyMoveFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the node arrived event.
	 * @param npcIds the IDs of the NPCs to monitor
	 */
	public void addNodeArrivedId(int... npcIds)
	{
		setNpcMoveNodeArrivedId(event -> notifyNodeArrived(event.getNpc()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the node arrived event.
	 * @param npcIds the IDs of the affected NPCs
	 */
	public void addNodeArrivedId(Collection<Integer> npcIds)
	{
		setNpcMoveNodeArrivedId(event -> notifyNodeArrived(event.getNpc()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the route finished event.
	 * @param npcIds the IDs of the NPCs to monitor
	 */
	public void addRouteFinishedId(int... npcIds)
	{
		setNpcMoveRouteFinishedId(event -> notifyRouteFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger the route finished event.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addRouteFinishedId(Collection<Integer> npcIds)
	{
		setNpcMoveRouteFinishedId(event -> notifyRouteFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger a hate response.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addNpcHateId(int... npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!onNpcHate(event.getNpc(), event.getPlayer(), event.isSummon()), false, false), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger a hate response.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addNpcHateId(Collection<Integer> npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!onNpcHate(event.getNpc(), event.getPlayer(), event.isSummon()), false, false), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger summon spawn events.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addSummonSpawnId(int... npcIds)
	{
		setPlayerSummonSpawnId(event -> onSummonSpawn(event.getSummon()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger summon spawn events.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addSummonSpawnId(Collection<Integer> npcIds)
	{
		setPlayerSummonSpawnId(event -> onSummonSpawn(event.getSummon()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger summon talk events.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addSummonTalkId(int... npcIds)
	{
		setPlayerSummonTalkId(event -> onSummonTalk(event.getSummon()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger summon talk events.
	 * @param npcIds the IDs of the NPCs
	 */
	public void addSummonTalkId(Collection<Integer> npcIds)
	{
		setPlayerSummonTalkId(event -> onSummonTalk(event.getSummon()), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger a can-see-me check.
	 * @param npcIds the IDs of the NPCs to add
	 */
	public void addCanSeeMeId(int... npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!notifyOnCanSeeMe(event.getNpc(), event.getPlayer()), false, false), npcIds);
	}
	
	/**
	 * Registers NPC IDs that trigger a can-see-me check.
	 * @param npcIds the IDs of the NPCs to add
	 */
	public void addCanSeeMeId(Collection<Integer> npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!notifyOnCanSeeMe(event.getNpc(), event.getPlayer()), false, false), npcIds);
	}
	
	/**
	 * Registers a listener notifying the quest when an Olympiad match finishes.
	 */
	public void addOlympiadMatchFinishId()
	{
		setOlympiadMatchResult(event -> notifyOlympiadMatch(event.getWinner(), event.getLoser(), event.getCompetitionType()));
	}
	
	/**
	 * Registers template IDs that trigger the instance-created event.
	 * @param templateIds the template IDs to monitor
	 */
	public void addInstanceCreatedId(int... templateIds)
	{
		setInstanceCreatedId(event -> onInstanceCreated(event.getInstanceWorld(), event.getCreator()), templateIds);
	}
	
	/**
	 * Registers template IDs that trigger the instance-created event.
	 * @param templateIds the template IDs to monitor
	 */
	public void addInstanceCreatedId(Collection<Integer> templateIds)
	{
		setInstanceCreatedId(event -> onInstanceCreated(event.getInstanceWorld(), event.getCreator()), templateIds);
	}
	
	/**
	 * Registers template IDs that trigger the instance-destroy event.
	 * @param templateIds the template IDs to monitor
	 */
	public void addInstanceDestroyId(int... templateIds)
	{
		setInstanceDestroyId(event -> onInstanceDestroy(event.getInstanceWorld()), templateIds);
	}
	
	/**
	 * Registers template IDs that trigger the instance-destroy event.
	 * @param templateIds the template IDs to monitor
	 */
	public void addInstanceDestroyId(Collection<Integer> templateIds)
	{
		setInstanceDestroyId(event -> onInstanceDestroy(event.getInstanceWorld()), templateIds);
	}
	
	/**
	 * Registers template IDs that trigger the instance-enter event.
	 * @param templateIds the template IDs to monitor
	 */
	public void addInstanceEnterId(int... templateIds)
	{
		setInstanceEnterId(event -> onInstanceEnter(event.getPlayer(), event.getInstanceWorld()), templateIds);
	}
	
	/**
	 * Registers template IDs that trigger the instance-enter event.
	 * @param templateIds the template IDs to monitor
	 */
	public void addInstanceEnterId(Collection<Integer> templateIds)
	{
		setInstanceEnterId(event -> onInstanceEnter(event.getPlayer(), event.getInstanceWorld()), templateIds);
	}
	
	/**
	 * Registers template IDs that trigger the instance-leave event.
	 * @param templateIds the template IDs to monitor
	 */
	public void addInstanceLeaveId(int... templateIds)
	{
		setInstanceLeaveId(event -> onInstanceLeave(event.getPlayer(), event.getInstanceWorld()), templateIds);
	}
	
	/**
	 * Registers template IDs that trigger the instance-leave event.
	 * @param templateIds the template IDs to monitor
	 */
	public void addInstanceLeaveId(Collection<Integer> templateIds)
	{
		setInstanceLeaveId(event -> onInstanceLeave(event.getPlayer(), event.getInstanceWorld()), templateIds);
	}
	
	/**
	 * Returns a random party member sharing the player instance, or the player itself when not in a party.
	 * @param player the reference player
	 * @return a random party member, the player when partyless, or {@code null} when player is null
	 */
	public static Player getRandomPartyMember(Player player)
	{
		if (player == null)
		{
			return null;
		}
		
		final Party party = player.getParty();
		if (party == null)
		{
			return player;
		}
		
		final List<Player> members = party.getMembers();
		if (members.isEmpty())
		{
			return player;
		}
		
		final List<Player> sameInstance = new ArrayList<>();
		for (Player partyMember : members)
		{
			if (partyMember.getInstanceId() == player.getInstanceId())
			{
				sameInstance.add(partyMember);
			}
		}
		if (sameInstance.isEmpty())
		{
			return player;
		}
		
		final Player member = sameInstance.get(Rnd.get(sameInstance.size()));
		
		return member;
	}
	
	/**
	 * Returns a random party member whose cond quest variable equals the given value.
	 * @param player the reference player
	 * @param cond the required cond value
	 * @return a matching random party member, or {@code null} when none match
	 */
	public Player getRandomPartyMember(Player player, int cond)
	{
		return getRandomPartyMember(player, "cond", String.valueOf(cond));
	}
	
	/**
	 * Returns a random party member within party range whose quest variable equals the given value.
	 * @param player the reference player
	 * @param var the quest state variable to test; null returns any random member
	 * @param value the required variable value
	 * @return a matching random party member, or {@code null} when none match
	 */
	public Player getRandomPartyMember(Player player, String var, String value)
	{
		// if no valid player instance is passed, there is nothing to check...
		if (player == null)
		{
			return null;
		}
		
		// For null var condition, return any random party member.
		if (var == null)
		{
			return getRandomPartyMember(player);
		}
		
		// normal cases...if the player is not in a party, check the player's state
		QuestState temp = null;
		final Party party = player.getParty();
		
		// if this player is not in a party, just check if this player instance matches the conditions itself
		if ((party == null) || party.getMembers().isEmpty())
		{
			temp = player.getQuestState(getName());
			if ((temp != null) && temp.isSet(var) && temp.get(var).equalsIgnoreCase(value))
			{
				return player; // match
			}
			
			return null; // no match
		}
		
		// if the player is in a party, gather a list of all matching party members (possibly including this player)
		final List<Player> candidates = new ArrayList<>();
		
		// get the target for enforcing distance limitations.
		WorldObject target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (Player partyMember : party.getMembers())
		{
			if (partyMember == null)
			{
				continue;
			}
			
			temp = partyMember.getQuestState(getName());
			if ((temp != null) && (temp.get(var) != null) && (temp.get(var)).equalsIgnoreCase(value) && partyMember.isInsideRadius3D(target, PlayerConfig.ALT_PARTY_RANGE) && (player.getInstanceId() == partyMember.getInstanceId()))
			{
				candidates.add(partyMember);
			}
		}
		
		// if there was no match, return null...
		if (candidates.isEmpty())
		{
			return null;
		}
		
		// If a match was found from the party, return one of them at random.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Returns a random party member within party range whose quest state equals the given state.
	 * @param player the reference player
	 * @param state the required quest state value
	 * @return a matching random party member, or {@code null} when none match
	 */
	public Player getRandomPartyMemberState(Player player, byte state)
	{
		// if no valid player instance is passed, there is nothing to check...
		if (player == null)
		{
			return null;
		}
		
		// normal cases...if the player is not in a party check the player's state
		QuestState temp = null;
		final Party party = player.getParty();
		
		// if this player is not in a party, just check if this player instance matches the conditions itself
		if ((party == null) || party.getMembers().isEmpty())
		{
			temp = player.getQuestState(getName());
			if ((temp != null) && (temp.getState() == state))
			{
				return player; // match
			}
			
			return null; // no match
		}
		
		// if the player is in a party, gather a list of all matching party members (possibly
		// including this player)
		final List<Player> candidates = new ArrayList<>();
		
		// get the target for enforcing distance limitations.
		WorldObject target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (Player partyMember : party.getMembers())
		{
			if (partyMember == null)
			{
				continue;
			}
			
			temp = partyMember.getQuestState(getName());
			if ((temp != null) && (temp.getState() == state) && partyMember.isInsideRadius3D(target, PlayerConfig.ALT_PARTY_RANGE) && (player.getInstanceId() == partyMember.getInstanceId()))
			{
				candidates.add(partyMember);
			}
		}
		
		// if there was no match, return null...
		if (candidates.isEmpty())
		{
			return null;
		}
		
		// If a match was found from the party, return one of them at random.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Selects an eligible party member near the NPC by highest random roll.
	 * @param player the initiating player
	 * @param npc the target NPC for distance checks
	 * @return the selected player, or {@code null} when none qualify
	 */
	public Player getRandomPartyMember(Player player, Npc npc)
	{
		if ((player == null) || !checkDistanceToTarget(player, npc))
		{
			return null;
		}
		
		final Party party = player.getParty();
		Player luckyPlayer = null;
		if (party == null)
		{
			if (checkPartyMember(player, npc))
			{
				luckyPlayer = player;
			}
		}
		else
		{
			int highestRoll = 0;
			for (Player member : party.getMembers())
			{
				final int rnd = getRandom(1000);
				if ((rnd > highestRoll) && checkPartyMember(member, npc))
				{
					highestRoll = rnd;
					luckyPlayer = member;
				}
			}
		}
		
		return (luckyPlayer != null) && checkDistanceToTarget(luckyPlayer, npc) ? luckyPlayer : null;
	}
	
	/**
	 * Hook for overriding party-member eligibility against an NPC; always true by default.
	 * @param player the player to check
	 * @param npc the NPC to check against
	 * @return {@code true} by default
	 */
	public boolean checkPartyMember(Player player, Npc npc)
	{
		return true;
	}
	
	/**
	 * Returns a random eligible party member quest state matching a condition, weighting the initiating player by chance.
	 * @param player the reference player
	 * @param condition the required cond, or -1 to require the quest be started
	 * @param playerChance the number of pool entries granted to the initiating player
	 * @param target the NPC target for distance and condition checks
	 * @return a matching random quest state, or {@code null} when none qualify
	 */
	public QuestState getRandomPartyMemberState(Player player, int condition, int playerChance, Npc target)
	{
		if ((player == null) || (playerChance < 1))
		{
			return null;
		}
		
		final String name = getName();
		QuestState qs = player.getQuestState(name);
		if (!player.isInParty())
		{
			return !checkPartyMemberConditions(qs, condition, target) || !checkDistanceToTarget(player, target) ? null : qs;
		}
		
		final List<QuestState> candidates = new ArrayList<>();
		if (checkPartyMemberConditions(qs, condition, target) && (playerChance > 0))
		{
			for (int i = 0; i < playerChance; i++)
			{
				candidates.add(qs);
			}
		}
		
		for (Player member : player.getParty().getMembers())
		{
			if (member == player)
			{
				continue;
			}
			
			qs = member.getQuestState(name);
			if (checkPartyMemberConditions(qs, condition, target))
			{
				candidates.add(qs);
			}
		}
		
		if (candidates.isEmpty())
		{
			return null;
		}
		
		qs = candidates.get(getRandom(candidates.size()));
		return !checkDistanceToTarget(qs.getPlayer(), target) ? null : qs;
	}
	
	/**
	 * Tests whether a quest state is started or matches a cond and passes the party-member check.
	 * @param qs the quest state to test
	 * @param condition the required cond, or -1 to require the quest be started
	 * @param npc the NPC involved
	 * @return {@code true} when all conditions are met
	 */
	private boolean checkPartyMemberConditions(QuestState qs, int condition, Npc npc)
	{
		return (qs != null) && ((condition == -1) ? qs.isStarted() : qs.isCond(condition)) && checkPartyMember(qs, npc);
	}
	
	/**
	 * Tests whether the player is within party range of the target, or the target is null.
	 * @param player the player to check
	 * @param target the NPC target
	 * @return {@code true} when in range or target is null
	 */
	private static boolean checkDistanceToTarget(Player player, Npc target)
	{
		return (target == null) || LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, player, target, true);
	}
	
	/**
	 * Hook for overriding party-member eligibility against an NPC; always true by default.
	 * @param qs the quest state to check
	 * @param npc the NPC to check against
	 * @return {@code true} by default
	 */
	public boolean checkPartyMember(QuestState qs, Npc npc)
	{
		return true;
	}
	
	/**
	 * Sends an HTML file to the player and returns its content.
	 * @param player the recipient player
	 * @param filename the file to display
	 * @return the file content, or {@code null} when not found
	 */
	public String showHtmlFile(Player player, String filename)
	{
		return showHtmlFile(player, filename, null);
	}
	
	/**
	 * Sends an HTML file to the player as a quest or NPC HTML message and returns its content.
	 * @param player the recipient player
	 * @param filename the file to display; a non-.html name uses a quest window
	 * @param npc the associated NPC, or null
	 * @return the file content, or {@code null} when not found
	 */
	public String showHtmlFile(Player player, String filename, Npc npc)
	{
		final boolean questwindow = !filename.endsWith(".html");
		
		// Create handler to file linked to the quest.
		String content = getHtm(player, filename);
		
		// Send message to client if message not empty.
		if (content != null)
		{
			if (npc != null)
			{
				content = content.replace("%objectId%", String.valueOf(npc.getObjectId()));
			}
			
			if (questwindow && (_questId > 0) && (_questId < 20000) && (_questId != 999))
			{
				final NpcQuestHtmlMessage npcReply = new NpcQuestHtmlMessage(npc != null ? npc.getObjectId() : 0, _questId);
				npcReply.setHtml(content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
			else
			{
				final NpcHtmlMessage npcReply = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0, content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		
		return content;
	}
	
	/**
	 * Resolves HTML content by searching the script path, then the quest path.
	 * @param player the player for context-specific content
	 * @param fileName the file to retrieve
	 * @return the HTML content, or {@code null} when not found
	 */
	public String getHtm(Player player, String fileName)
	{
		final HtmCache hc = HtmCache.getInstance();
		String content = hc.getHtm(player, fileName.startsWith("data/") ? fileName : "data/scripts/" + getPath() + "/" + fileName);
		if (content == null)
		{
			content = hc.getHtm(player, "data/scripts/" + getPath() + "/" + fileName);
			if (content == null)
			{
				content = hc.getHtm(player, "data/scripts/quests/" + getName() + "/" + fileName);
			}
		}
		
		return content;
	}
	
	/**
	 * Returns the registered quest item IDs.
	 * @return the registered item IDs
	 */
	public int[] getRegisteredItemIds()
	{
		return _questItemIds;
	}
	
	/**
	 * Registers the quest item IDs, logging a severe error for any non-existing item.
	 * @param items the quest item IDs
	 */
	public void registerQuestItems(int... items)
	{
		for (int id : items)
		{
			if ((id != 0) && (ItemData.getInstance().getTemplate(id) == null))
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found registerQuestItems for non existing item: " + id + "!");
			}
		}
		
		_questItemIds = items;
	}
	
	/**
	 * Removes all registered quest items from the player.
	 * @param player the player whose items are removed
	 */
	public void removeRegisteredQuestItems(Player player)
	{
		takeItems(player, -1, _questItemIds);
	}
	
	/**
	 * Enables or disables the login listener that triggers enter-world handling.
	 * @param value true to register the listener, false to remove existing login listeners
	 */
	public void setOnEnterWorld(boolean value)
	{
		if (value)
		{
			setPlayerLoginId(event -> onEnterWorld(event.getPlayer()));
		}
		else
		{
			for (AbstractEventListener listener : getListeners())
			{
				if (listener.getType() == EventType.ON_PLAYER_LOGIN)
				{
					listener.unregisterMe();
				}
			}
		}
	}
	
	/**
	 * Returns the NPC log entries for the player; empty by default.
	 * @param player the player to check
	 * @return the NPC log entries
	 */
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		return Collections.emptySet();
	}
	
	/**
	 * Tests whether the target is an instance of the class and its ID is in the array.
	 * @param <T> the required target type
	 * @param ids the IDs to match against
	 * @param target the object to validate
	 * @param clazz the required type
	 * @return {@code true} when the target matches the type and its ID is listed
	 */
	public <T> boolean isTarget(int[] ids, WorldObject target, Class<T> clazz)
	{
		if ((target != null) && clazz.isInstance(target))
		{
			return ArrayUtil.contains(ids, target.getId());
		}
		
		return false;
	}
	
	/**
	 * Sends the NPC log list packet when the player has a quest state for this quest.
	 * @param player the recipient player
	 */
	public void sendNpcLogList(Player player)
	{
		if (player.getQuestState(getName()) != null)
		{
			final ExQuestNpcLogList packet = new ExQuestNpcLogList(_questId);
			getNpcLogList(player).forEach(packet::add);
			player.sendPacket(packet);
		}
	}
	
	/**
	 * Returns the quest start conditions.
	 * @return the start conditions
	 */
	private Set<QuestCondition> getStartConditions()
	{
		return _startCondition;
	}
	
	/**
	 * Tests whether the player satisfies every start condition.
	 * @param player the player to check
	 * @return {@code true} when all start conditions pass
	 */
	public boolean canStartQuest(Player player)
	{
		for (QuestCondition cond : _startCondition)
		{
			if (!cond.test(player))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Returns the HTML for the first failed start condition, or null when all pass or the quest is past creation.
	 * @param player the player to check
	 * @param npc the associated NPC
	 * @return the failed condition HTML, or {@code null}
	 */
	public String getStartConditionHtml(Player player, Npc npc)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && !qs.isCreated())
		{
			return null;
		}
		
		for (QuestCondition cond : _startCondition)
		{
			if (!cond.test(player))
			{
				return cond.getHtml(npc);
			}
		}
		
		return null;
	}
	
	/**
	 * Adds a start condition with its failure HTML.
	 * @param questStartRequirement the predicate the player must satisfy
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondStart(Predicate<Player> questStartRequirement, String html)
	{
		getStartConditions().add(new QuestCondition(questStartRequirement, html));
	}
	
	/**
	 * Adds a start condition requiring the player level within an inclusive range.
	 * @param minLevel the minimum level
	 * @param maxLevel the maximum level
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondLevel(int minLevel, int maxLevel, String html)
	{
		addCondStart(p -> (p.getLevel() >= minLevel) && (p.getLevel() <= maxLevel), html);
	}
	
	/**
	 * Adds a start condition requiring at least the given level.
	 * @param minLevel the minimum level
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondMinLevel(int minLevel, String html)
	{
		addCondStart(p -> p.getLevel() >= minLevel, html);
	}
	
	/**
	 * Adds a start condition requiring at most the given level.
	 * @param maxLevel the maximum level
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondMaxLevel(int maxLevel, String html)
	{
		addCondStart(p -> p.getLevel() <= maxLevel, html);
	}
	
	/**
	 * Adds a start condition requiring the given race.
	 * @param race the required race
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondRace(Race race, String html)
	{
		addCondStart(p -> p.getRace() == race, html);
	}
	
	/**
	 * Adds a start condition excluding the given race.
	 * @param race the excluded race
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondNotRace(Race race, String html)
	{
		addCondStart(p -> p.getRace() != race, html);
	}
	
	/**
	 * Adds a start condition requiring the named quest be completed.
	 * @param name the quest state name
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondCompletedQuest(String name, String html)
	{
		addCondStart(p -> p.hasQuestState(name) && p.getQuestState(name).isCompleted(), html);
	}
	
	/**
	 * Adds a start condition requiring the named quest be started.
	 * @param name the quest state name
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondStartedQuest(String name, String html)
	{
		addCondStart(p -> p.hasQuestState(name) && p.getQuestState(name).isStarted(), html);
	}
	
	/**
	 * Adds a start condition requiring the given player class.
	 * @param playerClass the required class
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondClassId(PlayerClass playerClass, String html)
	{
		addCondStart(p -> p.getPlayerClass() == playerClass, html);
	}
	
	/**
	 * Adds a start condition requiring one of the given player classes.
	 * @param playerClasss the allowed classes
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondClassIds(List<PlayerClass> playerClasss, String html)
	{
		addCondStart(p -> playerClasss.contains(p.getPlayerClass()), html);
	}
	
	/**
	 * Adds a start condition requiring at least one of the given item in inventory.
	 * @param itemId the required item ID
	 */
	public void addCondItemId(int itemId)
	{
		addCondStart(p -> !p.getInventory().getAllItemsByItemId(itemId).isEmpty(), null);
	}
	
	/**
	 * Adds a start condition requiring negative reputation.
	 */
	public void addCondPlayerKarma()
	{
		addCondStart(p -> p.getReputation() < 0, null);
	}
	
	/**
	 * Adds a start condition excluding the given player class.
	 * @param playerClass the excluded class
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondNotClassId(PlayerClass playerClass, String html)
	{
		addCondStart(p -> p.getPlayerClass() != playerClass, html);
	}
	
	/**
	 * Adds a start condition requiring an active subclass.
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondIsSubClassActive(String html)
	{
		addCondStart(p -> p.isSubClassActive(), html);
	}
	
	/**
	 * Adds a start condition requiring no active subclass or dual class.
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondIsNotSubClassActive(String html)
	{
		addCondStart(p -> !p.isSubClassActive() && !p.isDualClassActive(), html);
	}
	
	/**
	 * Adds a start condition requiring membership in the given category.
	 * @param categoryType the required category
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondInCategory(CategoryType categoryType, String html)
	{
		addCondStart(p -> p.isInCategory(categoryType), html);
	}
	
	/**
	 * Adds a start condition requiring a clan level above the given value.
	 * @param clanLevel the threshold clan level
	 * @param html the HTML shown when the condition fails
	 */
	public void addCondClanLevel(int clanLevel, String html)
	{
		addCondStart(p -> (p.getClan() != null) && (p.getClan().getLevel() > clanLevel), html);
	}
	
	/**
	 * Adds a start condition requiring at least the given level with a faction.
	 * @param faction the faction to check
	 * @param factionLevel the minimum faction level
	 * @param html the HTML shown when the condition fails
	 */
	public void addFactionLevel(Faction faction, int factionLevel, String html)
	{
		addCondStart(p -> p.getFactionLevel(faction) >= factionLevel, html);
	}
	
	/**
	 * Adds start conditions for allowed classes, previous quests, and level range from the given condition.
	 * @param condition the source quest conditions
	 * @param html the HTML shown when a condition fails
	 */
	public void addNewQuestConditions(NewQuestCondition condition, String html)
	{
		final List<PlayerClass> allowedClassIds = condition.getAllowedClassIds();
		if (!allowedClassIds.isEmpty())
		{
			addCondStart(p -> allowedClassIds.contains(p.getPlayerClass()), html);
		}
		
		final List<Integer> previousQuestIds = condition.getPreviousQuestIds();
		if (!previousQuestIds.isEmpty())
		{
			if (condition.getOneOfPreQuests())
			{
				addCondStart(p -> p.hasAnyCompletedQuestStates(previousQuestIds), html);
			}
			else
			{
				for (Integer questId : previousQuestIds)
				{
					final Quest quest = ScriptManager.getInstance().getQuest(questId);
					if (quest != null)
					{
						final String name = quest.getName();
						addCondStart(p -> p.hasQuestState(name) && p.getQuestState(name).isCompleted(), html);
					}
				}
			}
		}
		
		addCondMinLevel(condition.getMinLevel(), html);
		addCondMaxLevel(condition.getMaxLevel(), html);
	}
	
	/**
	 * Hook invoked when the quest is aborted; empty by default.
	 * @param player the player who aborted the quest
	 */
	public void onQuestAborted(Player player)
	{
	}
	
	/**
	 * Scans public methods for RegisterEvent and RegisterType annotations and registers valid listeners with their ID filters and priority.
	 */
	private void initializeAnnotationListeners()
	{
		final List<Integer> ids = new ArrayList<>();
		for (Method method : getClass().getMethods())
		{
			if (method.isAnnotationPresent(RegisterEvent.class) && method.isAnnotationPresent(RegisterType.class))
			{
				final RegisterEvent listener = method.getAnnotation(RegisterEvent.class);
				final EventType eventType = listener.value();
				final int paramCount = method.getParameterCount();
				if (paramCount != 1)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Non properly defined annotation listener on method: " + method.getName() + " expected parameter count is 1 but found: " + paramCount);
					continue;
				}
				else if (!eventType.isEventClass(method.getParameterTypes()[0]))
				{
					LOGGER.warning(getClass().getSimpleName() + ": Non properly defined annotation listener on method: " + method.getName() + " expected parameter to be type of: " + eventType.getEventClass().getSimpleName() + " but found: " + method.getParameterTypes()[0].getSimpleName());
					continue;
				}
				else if (!eventType.isReturnClass(method.getReturnType()))
				{
					LOGGER.warning(getClass().getSimpleName() + ": Non properly defined annotation listener on method: " + method.getName() + " expected return type to be one of: " + Arrays.toString(eventType.getReturnClasses()) + " but found: " + method.getReturnType().getSimpleName());
					continue;
				}
				
				final RegisterType regType = method.getAnnotation(RegisterType.class);
				final ListenerRegisterType type = regType.value();
				int priority = 0;
				
				// Clear the list
				ids.clear();
				
				// Scan for possible Id filters.
				for (Annotation annotation : method.getAnnotations())
				{
					if (annotation instanceof Id)
					{
						final Id npc = (Id) annotation;
						for (int id : npc.value())
						{
							ids.add(id);
						}
					}
					else if (annotation instanceof Ids)
					{
						final Ids npcs = (Ids) annotation;
						for (Id npc : npcs.value())
						{
							for (int id : npc.value())
							{
								ids.add(id);
							}
						}
					}
					else if (annotation instanceof Range)
					{
						final Range range = (Range) annotation;
						if (range.from() > range.to())
						{
							LOGGER.warning(getClass().getSimpleName() + ": Wrong " + annotation.getClass().getSimpleName() + " from is higher then to!");
							continue;
						}
						
						for (int id = range.from(); id <= range.to(); id++)
						{
							ids.add(id);
						}
					}
					else if (annotation instanceof Ranges)
					{
						final Ranges ranges = (Ranges) annotation;
						for (Range range : ranges.value())
						{
							if (range.from() > range.to())
							{
								LOGGER.warning(getClass().getSimpleName() + ": Wrong " + annotation.getClass().getSimpleName() + " from is higher then to!");
								continue;
							}
							
							for (int id = range.from(); id <= range.to(); id++)
							{
								ids.add(id);
							}
						}
					}
					else if (annotation instanceof NpcLevelRange)
					{
						final NpcLevelRange range = (NpcLevelRange) annotation;
						if (range.from() > range.to())
						{
							LOGGER.warning(getClass().getSimpleName() + ": Wrong " + annotation.getClass().getSimpleName() + " from is higher then to!");
							continue;
						}
						else if (type != ListenerRegisterType.NPC)
						{
							LOGGER.warning(getClass().getSimpleName() + ": ListenerRegisterType " + type + " for " + annotation.getClass().getSimpleName() + " NPC is expected!");
							continue;
						}
						
						for (int level = range.from(); level <= range.to(); level++)
						{
							final List<NpcTemplate> templates = NpcData.getInstance().getAllOfLevel(level);
							templates.forEach(template -> ids.add(template.getId()));
						}
					}
					else if (annotation instanceof NpcLevelRanges)
					{
						final NpcLevelRanges ranges = (NpcLevelRanges) annotation;
						for (NpcLevelRange range : ranges.value())
						{
							if (range.from() > range.to())
							{
								LOGGER.warning(getClass().getSimpleName() + ": Wrong " + annotation.getClass().getSimpleName() + " from is higher then to!");
								continue;
							}
							else if (type != ListenerRegisterType.NPC)
							{
								LOGGER.warning(getClass().getSimpleName() + ": ListenerRegisterType " + type + " for " + annotation.getClass().getSimpleName() + " NPC is expected!");
								continue;
							}
							
							for (int level = range.from(); level <= range.to(); level++)
							{
								final List<NpcTemplate> templates = NpcData.getInstance().getAllOfLevel(level);
								templates.forEach(template -> ids.add(template.getId()));
							}
						}
					}
					else if (annotation instanceof Priority)
					{
						final Priority p = (Priority) annotation;
						priority = p.value();
					}
				}
				
				if (!ids.isEmpty())
				{
					_registeredIds.computeIfAbsent(type, _ -> ConcurrentHashMap.newKeySet()).addAll(ids);
				}
				
				registerAnnotation(method, eventType, type, priority, ids);
			}
		}
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an attackable-kill consumer for the given NPC IDs, logging a severe error for any non-existing NPC.
	 * @param callback the consumer run on kill
	 * @param npcIds the NPC IDs to monitor
	 * @return the registered listeners
	 */
	protected List<AbstractEventListener> setAttackableKillId(Consumer<OnAttackableKill> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addKillId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_ATTACKABLE_KILL, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an attackable-kill consumer for the given NPC IDs, logging a severe error for any non-existing NPC.
	 * @param callback the consumer run on kill
	 * @param npcIds the NPC IDs to monitor
	 * @return the registered listeners
	 */
	protected List<AbstractEventListener> setAttackableKillId(Consumer<OnAttackableKill> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addKillId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_ATTACKABLE_KILL, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers a creature-death function for the given NPC IDs.
	 * @param callback the function run on death
	 * @param npcIds the NPC IDs to monitor
	 * @return the registered listeners
	 */
	protected List<AbstractEventListener> addCreatureKillId(Function<OnCreatureDeath, ? extends AbstractEventReturn> callback, int... npcIds)
	{
		return registerFunction(callback, EventType.ON_CREATURE_DEATH, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers a creature-death consumer for the given NPC IDs.
	 * @param callback the consumer run on death
	 * @param npcIds the NPC IDs to monitor
	 * @return the registered listeners
	 */
	protected List<AbstractEventListener> setCreatureKillId(Consumer<OnCreatureDeath> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_DEATH, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers a creature-death consumer for the given NPC IDs.
	 * @param callback the consumer run on death
	 * @param npcIds the NPC IDs to monitor
	 * @return the registered listeners
	 */
	protected List<AbstractEventListener> setCreatureKillId(Consumer<OnCreatureDeath> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_DEATH, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers a creature-attacked function for the given NPC IDs.
	 * @param callback the function run on attack
	 * @param npcIds the NPC IDs to monitor
	 * @return the registered listeners
	 */
	protected List<AbstractEventListener> addCreatureAttackedId(Function<OnCreatureAttacked, ? extends AbstractEventReturn> callback, int... npcIds)
	{
		return registerFunction(callback, EventType.ON_CREATURE_ATTACKED, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers a creature-attacked consumer for the given NPC IDs.
	 * @param callback the consumer run on attack
	 * @param npcIds the NPC IDs to monitor
	 * @return the registered listeners
	 */
	protected List<AbstractEventListener> setCreatureAttackedId(Consumer<OnCreatureAttacked> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_ATTACKED, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_CREATURE_ATTACKED callback for the given NPCs.
	 * @param callback action run when a creature is attacked
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setCreatureAttackedId(Consumer<OnCreatureAttacked> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_ATTACKED, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_NPC_FIRST_TALK callback for the given NPCs, logging any that do not exist.
	 * @param callback action run on first talk
	 * @param npcIds NPC identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcFirstTalkId(Consumer<OnNpcFirstTalk> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addFirstTalkId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_FIRST_TALK, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_FIRST_TALK callback for the given NPCs, logging any that do not exist.
	 * @param callback action run on first talk
	 * @param npcIds NPC identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcFirstTalkId(Consumer<OnNpcFirstTalk> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addFirstTalkId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_FIRST_TALK, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers a dummy ON_NPC_TALK listener for the given NPCs, logging any that do not exist.
	 * @param npcIds NPC identifiers to register
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcTalkId(Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addTalkId for non existing NPC: " + id + "!");
			}
		}
		
		return registerDummy(EventType.ON_NPC_TALK, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers a dummy ON_NPC_TALK listener for the given NPCs, logging any that do not exist.
	 * @param npcIds NPC identifiers to register
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcTalkId(int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addTalkId for non existing NPC: " + id + "!");
			}
		}
		
		return registerDummy(EventType.ON_NPC_TALK, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_NPC_TELEPORT callback for the given NPCs.
	 * @param callback action run when an NPC teleports
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcTeleportId(Consumer<OnNpcTeleport> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_TELEPORT, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_TELEPORT callback for the given NPCs.
	 * @param callback action run when an NPC teleports
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcTeleportId(Consumer<OnNpcTeleport> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_TELEPORT, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers a dummy ON_NPC_QUEST_START listener for the given NPCs, logging any that do not exist.
	 * @param npcIds NPC identifiers to register as quest starters
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcQuestStartId(int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addStartNpc for non existing NPC: " + id + "!");
			}
		}
		
		return registerDummy(EventType.ON_NPC_QUEST_START, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers a dummy ON_NPC_QUEST_START listener for the given NPCs, logging any that do not exist.
	 * @param npcIds NPC identifiers to register as quest starters
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcQuestStartId(Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addStartNpc for non existing NPC: " + id + "!");
			}
		}
		
		return registerDummy(EventType.ON_NPC_QUEST_START, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_NPC_SKILL_SEE callback for the given NPCs, logging any that do not exist.
	 * @param callback action run on the skill-see event
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcSkillSeeId(Consumer<OnNpcSkillSee> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addSkillSeeId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_SKILL_SEE, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_SKILL_SEE callback for the given NPCs, logging any that do not exist.
	 * @param callback action run on the skill-see event
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcSkillSeeId(Consumer<OnNpcSkillSee> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addSkillSeeId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_SKILL_SEE, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_NPC_SKILL_FINISHED callback for the given NPCs, logging any that do not exist.
	 * @param callback action run when the skill finishes
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcSkillFinishedId(Consumer<OnNpcSkillFinished> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addSpellFinishedId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_SKILL_FINISHED, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_SKILL_FINISHED callback for the given NPCs, logging any that do not exist.
	 * @param callback action run when the skill finishes
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcSkillFinishedId(Consumer<OnNpcSkillFinished> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addSpellFinishedId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_SKILL_FINISHED, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_NPC_SPAWN callback for the given NPCs, logging any that do not exist.
	 * @param callback action run when an NPC spawns
	 * @param npcIds NPC identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcSpawnId(Consumer<OnNpcSpawn> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addSpawnId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_SPAWN, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_SPAWN callback for the given NPCs, logging any that do not exist.
	 * @param callback action run when an NPC spawns
	 * @param npcIds NPC identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcSpawnId(Consumer<OnNpcSpawn> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addSpawnId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_SPAWN, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_NPC_DESPAWN callback for the given NPCs.
	 * @param callback action run when an NPC despawns
	 * @param npcIds NPC identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcDespawnId(Consumer<OnNpcDespawn> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_DESPAWN, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_DESPAWN callback for the given NPCs.
	 * @param callback action run when an NPC despawns
	 * @param npcIds NPC identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcDespawnId(Consumer<OnNpcDespawn> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_DESPAWN, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_NPC_EVENT_RECEIVED callback for the given NPCs.
	 * @param callback action run when the event is received
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcEventReceivedId(Consumer<OnNpcEventReceived> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_EVENT_RECEIVED, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_EVENT_RECEIVED callback for the given NPCs.
	 * @param callback action run when the event is received
	 * @param npcIds NPC identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcEventReceivedId(Consumer<OnNpcEventReceived> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_EVENT_RECEIVED, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_NPC_MOVE_FINISHED callback for the given NPCs.
	 * @param callback action run when movement finishes
	 * @param npcIds NPC identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcMoveFinishedId(Consumer<OnNpcMoveFinished> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_MOVE_FINISHED, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_MOVE_FINISHED callback for the given NPCs.
	 * @param callback action run when movement finishes
	 * @param npcIds NPC identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcMoveFinishedId(Consumer<OnNpcMoveFinished> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_MOVE_FINISHED, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_NPC_MOVE_NODE_ARRIVED callback for the given NPCs.
	 * @param callback action run when an NPC arrives at a node
	 * @param npcIds NPC identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcMoveNodeArrivedId(Consumer<OnNpcMoveNodeArrived> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_MOVE_NODE_ARRIVED, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_MOVE_NODE_ARRIVED callback for the given NPCs.
	 * @param callback action run when an NPC arrives at a node
	 * @param npcIds NPC identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcMoveNodeArrivedId(Consumer<OnNpcMoveNodeArrived> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_MOVE_NODE_ARRIVED, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_NPC_MOVE_ROUTE_FINISHED callback for the given NPCs.
	 * @param callback action run when the move route finishes
	 * @param npcIds NPC identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcMoveRouteFinishedId(Consumer<OnNpcMoveRouteFinished> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_MOVE_ROUTE_FINISHED, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_MOVE_ROUTE_FINISHED callback for the given NPCs.
	 * @param callback action run when the move route finishes
	 * @param npcIds NPC identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcMoveRouteFinishedId(Consumer<OnNpcMoveRouteFinished> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_MOVE_ROUTE_FINISHED, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_NPC_HATE consumer callback for the given NPCs.
	 * @param callback action run on the hate event
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcHateId(Consumer<OnAttackableHate> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_HATE, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_HATE consumer callback for the given NPCs.
	 * @param callback action run on the hate event
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcHateId(Consumer<OnAttackableHate> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_HATE, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_HATE function callback for the given NPCs.
	 * @param callback function run on the hate event
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> addNpcHateId(Function<OnAttackableHate, TerminateReturn> callback, int... npcIds)
	{
		return registerFunction(callback, EventType.ON_NPC_HATE, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_HATE function callback for the given NPCs.
	 * @param callback function run on the hate event
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> addNpcHateId(Function<OnAttackableHate, TerminateReturn> callback, Collection<Integer> npcIds)
	{
		return registerFunction(callback, EventType.ON_NPC_HATE, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_NPC_CAN_BE_SEEN consumer callback for the given NPCs.
	 * @param callback action run when an NPC can be seen
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcCanBeSeenId(Consumer<OnNpcCanBeSeen> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_CAN_BE_SEEN, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_CAN_BE_SEEN consumer callback for the given NPCs.
	 * @param callback action run when an NPC can be seen
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcCanBeSeenId(Consumer<OnNpcCanBeSeen> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_CAN_BE_SEEN, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_CAN_BE_SEEN function callback for the given NPCs.
	 * @param callback function run when an NPC can be seen
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcCanBeSeenId(Function<OnNpcCanBeSeen, TerminateReturn> callback, int... npcIds)
	{
		return registerFunction(callback, EventType.ON_NPC_CAN_BE_SEEN, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_NPC_CAN_BE_SEEN function callback for the given NPCs.
	 * @param callback function run when an NPC can be seen
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setNpcCanBeSeenId(Function<OnNpcCanBeSeen, TerminateReturn> callback, Collection<Integer> npcIds)
	{
		return registerFunction(callback, EventType.ON_NPC_CAN_BE_SEEN, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Enables creature-see tracking and registers an ON_CREATURE_SEE callback for the given NPCs.
	 * @param callback action run when a creature is seen
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setCreatureSeeId(Consumer<OnCreatureSee> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			Npc.addCreatureSeeId(id);
		}
		
		return registerConsumer(callback, EventType.ON_CREATURE_SEE, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Enables creature-see tracking and registers an ON_CREATURE_SEE callback for the given NPCs.
	 * @param callback action run when a creature is seen
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setCreatureSeeId(Consumer<OnCreatureSee> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			Npc.addCreatureSeeId(id);
		}
		
		return registerConsumer(callback, EventType.ON_CREATURE_SEE, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_ATTACKABLE_FACTION_CALL callback for the given NPCs.
	 * @param callback action run on the faction call
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setAttackableFactionCallId(Consumer<OnAttackableFactionCall> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_ATTACKABLE_FACTION_CALL, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_ATTACKABLE_FACTION_CALL callback for the given NPCs.
	 * @param callback action run on the faction call
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setAttackableFactionCallId(Consumer<OnAttackableFactionCall> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_ATTACKABLE_FACTION_CALL, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_ATTACKABLE_ATTACK callback for the given NPCs, logging any that do not exist.
	 * @param callback action run when an attackable is attacked
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setAttackableAttackId(Consumer<OnAttackableAttack> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addAttackId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_ATTACKABLE_ATTACK, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_ATTACKABLE_ATTACK callback for the given NPCs, logging any that do not exist.
	 * @param callback action run when an attackable is attacked
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setAttackableAttackId(Consumer<OnAttackableAttack> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addAttackId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_ATTACKABLE_ATTACK, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_ATTACKABLE_AGGRO_RANGE_ENTER callback for the given NPCs.
	 * @param callback action run on aggro range entry
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setAttackableAggroRangeEnterId(Consumer<OnAttackableAggroRangeEnter> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_ATTACKABLE_AGGRO_RANGE_ENTER, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_ATTACKABLE_AGGRO_RANGE_ENTER callback for the given NPCs.
	 * @param callback action run on aggro range entry
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setAttackableAggroRangeEnterId(Consumer<OnAttackableAggroRangeEnter> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_ATTACKABLE_AGGRO_RANGE_ENTER, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_PLAYER_SKILL_LEARN callback for the given NPCs.
	 * @param callback action run when a player learns a skill
	 * @param npcIds NPC identifiers that trigger the event
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setPlayerSkillLearnId(Consumer<OnPlayerSkillLearn> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_SKILL_LEARN, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_PLAYER_SKILL_LEARN callback for the given NPCs.
	 * @param callback action run when a player learns a skill
	 * @param npcIds NPC identifiers that trigger the event
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setPlayerSkillLearnId(Consumer<OnPlayerSkillLearn> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_SKILL_LEARN, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_PLAYER_SUMMON_SPAWN callback for the given NPCs.
	 * @param callback action run when a summon spawns
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setPlayerSummonSpawnId(Consumer<OnPlayerSummonSpawn> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_SUMMON_SPAWN, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_PLAYER_SUMMON_SPAWN callback for the given NPCs.
	 * @param callback action run when a summon spawns
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setPlayerSummonSpawnId(Consumer<OnPlayerSummonSpawn> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_SUMMON_SPAWN, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_PLAYER_SUMMON_TALK callback for the given NPCs.
	 * @param callback action run when a summon talks
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setPlayerSummonTalkId(Consumer<OnPlayerSummonTalk> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_SUMMON_TALK, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_PLAYER_SUMMON_TALK callback for the given NPCs.
	 * @param callback action run when a summon talks
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setPlayerSummonTalkId(Consumer<OnPlayerSummonSpawn> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_SUMMON_TALK, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers a global ON_PLAYER_LOGIN callback.
	 * @param callback action run when a player logs in
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setPlayerLoginId(Consumer<OnPlayerLogin> callback)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_LOGIN, ListenerRegisterType.GLOBAL);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers a global ON_PLAYER_LOGOUT callback.
	 * @param callback action run when a player logs out
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setPlayerLogoutId(Consumer<OnPlayerLogout> callback)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_LOGOUT, ListenerRegisterType.GLOBAL);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_CREATURE_ZONE_ENTER callback for the given zones.
	 * @param callback action run when a creature enters a zone
	 * @param npcIds zone identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setCreatureZoneEnterId(Consumer<OnCreatureZoneEnter> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_ZONE_ENTER, ListenerRegisterType.ZONE, npcIds);
	}
	
	/**
	 * Registers an ON_CREATURE_ZONE_ENTER callback for the given zones.
	 * @param callback action run when a creature enters a zone
	 * @param npcIds zone identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setCreatureZoneEnterId(Consumer<OnCreatureZoneEnter> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_ZONE_ENTER, ListenerRegisterType.ZONE, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_CREATURE_ZONE_EXIT callback for the given zones.
	 * @param callback action run when a creature exits a zone
	 * @param npcIds zone identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setCreatureZoneExitId(Consumer<OnCreatureZoneExit> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_ZONE_EXIT, ListenerRegisterType.ZONE, npcIds);
	}
	
	/**
	 * Registers an ON_CREATURE_ZONE_EXIT callback for the given zones.
	 * @param callback action run when a creature exits a zone
	 * @param npcIds zone identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setCreatureZoneExitId(Consumer<OnCreatureZoneExit> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_ZONE_EXIT, ListenerRegisterType.ZONE, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_TRAP_ACTION callback for the given NPCs.
	 * @param callback action run on the trap action
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setTrapActionId(Consumer<OnTrapAction> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_TRAP_ACTION, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Registers an ON_TRAP_ACTION callback for the given NPCs.
	 * @param callback action run on the trap action
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setTrapActionId(Consumer<OnTrapAction> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_TRAP_ACTION, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_ITEM_BYPASS_EVENT callback for the given items.
	 * @param callback action run on the item bypass event
	 * @param npcIds item identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setItemBypassEventId(Consumer<OnItemBypassEvent> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_ITEM_BYPASS_EVENT, ListenerRegisterType.ITEM, npcIds);
	}
	
	/**
	 * Registers an ON_ITEM_BYPASS_EVENT callback for the given items.
	 * @param callback action run on the item bypass event
	 * @param npcIds item identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setItemBypassEventId(Consumer<OnItemBypassEvent> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_ITEM_BYPASS_EVENT, ListenerRegisterType.ITEM, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_ITEM_TALK callback for the given items.
	 * @param callback action run on the item talk event
	 * @param npcIds item identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setItemTalkId(Consumer<OnItemTalk> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_ITEM_TALK, ListenerRegisterType.ITEM, npcIds);
	}
	
	/**
	 * Registers an ON_ITEM_TALK callback for the given items.
	 * @param callback action run on the item talk event
	 * @param npcIds item identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setItemTalkId(Consumer<OnItemTalk> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_ITEM_TALK, ListenerRegisterType.ITEM, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers a global ON_OLYMPIAD_MATCH_RESULT callback.
	 * @param callback action run on an Olympiad match result
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setOlympiadMatchResult(Consumer<OnOlympiadMatchResult> callback)
	{
		return registerConsumer(callback, EventType.ON_OLYMPIAD_MATCH_RESULT, ListenerRegisterType.OLYMPIAD);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_CASTLE_SIEGE_START callback for the given castles.
	 * @param callback action run when a siege starts
	 * @param castleIds castle identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setCastleSiegeStartId(Consumer<OnCastleSiegeStart> callback, int... castleIds)
	{
		return registerConsumer(callback, EventType.ON_CASTLE_SIEGE_START, ListenerRegisterType.CASTLE, castleIds);
	}
	
	/**
	 * Registers an ON_CASTLE_SIEGE_START callback for the given castles.
	 * @param callback action run when a siege starts
	 * @param castleIds castle identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setCastleSiegeStartId(Consumer<OnCastleSiegeStart> callback, Collection<Integer> castleIds)
	{
		return registerConsumer(callback, EventType.ON_CASTLE_SIEGE_START, ListenerRegisterType.CASTLE, castleIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_CASTLE_SIEGE_OWNER_CHANGE callback for the given castles.
	 * @param callback action run when castle ownership changes
	 * @param castleIds castle identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setCastleSiegeOwnerChangeId(Consumer<OnCastleSiegeOwnerChange> callback, int... castleIds)
	{
		return registerConsumer(callback, EventType.ON_CASTLE_SIEGE_OWNER_CHANGE, ListenerRegisterType.CASTLE, castleIds);
	}
	
	/**
	 * Registers an ON_CASTLE_SIEGE_OWNER_CHANGE callback for the given castles.
	 * @param callback action run when castle ownership changes
	 * @param castleIds castle identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setCastleSiegeOwnerChangeId(Consumer<OnCastleSiegeOwnerChange> callback, Collection<Integer> castleIds)
	{
		return registerConsumer(callback, EventType.ON_CASTLE_SIEGE_OWNER_CHANGE, ListenerRegisterType.CASTLE, castleIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_CASTLE_SIEGE_FINISH callback for the given castles.
	 * @param callback action run when a siege ends
	 * @param castleIds castle identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setCastleSiegeFinishId(Consumer<OnCastleSiegeFinish> callback, int... castleIds)
	{
		return registerConsumer(callback, EventType.ON_CASTLE_SIEGE_FINISH, ListenerRegisterType.CASTLE, castleIds);
	}
	
	/**
	 * Registers an ON_CASTLE_SIEGE_FINISH callback for the given castles.
	 * @param callback action run when a siege ends
	 * @param castleIds castle identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setCastleSiegeFinishId(Consumer<OnCastleSiegeFinish> callback, Collection<Integer> castleIds)
	{
		return registerConsumer(callback, EventType.ON_CASTLE_SIEGE_FINISH, ListenerRegisterType.CASTLE, castleIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers a global ON_PLAYER_PROFESSION_CHANGE callback.
	 * @param callback action run on profession change
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setPlayerProfessionChangeId(Consumer<OnPlayerProfessionChange> callback)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_PROFESSION_CHANGE, ListenerRegisterType.GLOBAL);
	}
	
	/**
	 * Registers a global ON_PLAYER_PROFESSION_CANCEL callback.
	 * @param callback action run on profession cancellation
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setPlayerProfessionCancelId(Consumer<OnPlayerProfessionCancel> callback)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_PROFESSION_CANCEL, ListenerRegisterType.GLOBAL);
	}
	
	/**
	 * Registers an ON_INSTANCE_CREATED callback for the given instance templates.
	 * @param callback action run when an instance is created
	 * @param templateIds instance template identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setInstanceCreatedId(Consumer<OnInstanceCreated> callback, int... templateIds)
	{
		return registerConsumer(callback, EventType.ON_INSTANCE_CREATED, ListenerRegisterType.INSTANCE, templateIds);
	}
	
	/**
	 * Registers an ON_INSTANCE_CREATED callback for the given instance templates.
	 * @param callback action run when an instance is created
	 * @param templateIds instance template identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setInstanceCreatedId(Consumer<OnInstanceCreated> callback, Collection<Integer> templateIds)
	{
		return registerConsumer(callback, EventType.ON_INSTANCE_CREATED, ListenerRegisterType.INSTANCE, templateIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_INSTANCE_DESTROY callback for the given instance templates.
	 * @param callback action run when an instance is destroyed
	 * @param templateIds instance template identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setInstanceDestroyId(Consumer<OnInstanceDestroy> callback, int... templateIds)
	{
		return registerConsumer(callback, EventType.ON_INSTANCE_DESTROY, ListenerRegisterType.INSTANCE, templateIds);
	}
	
	/**
	 * Registers an ON_INSTANCE_DESTROY callback for the given instance templates.
	 * @param callback action run when an instance is destroyed
	 * @param templateIds instance template identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setInstanceDestroyId(Consumer<OnInstanceDestroy> callback, Collection<Integer> templateIds)
	{
		return registerConsumer(callback, EventType.ON_INSTANCE_DESTROY, ListenerRegisterType.INSTANCE, templateIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_INSTANCE_ENTER callback for the given instance templates.
	 * @param callback action run when a player enters an instance
	 * @param templateIds instance template identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setInstanceEnterId(Consumer<OnInstanceEnter> callback, int... templateIds)
	{
		return registerConsumer(callback, EventType.ON_INSTANCE_ENTER, ListenerRegisterType.INSTANCE, templateIds);
	}
	
	/**
	 * Registers an ON_INSTANCE_ENTER callback for the given instance templates.
	 * @param callback action run when a player enters an instance
	 * @param templateIds instance template identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setInstanceEnterId(Consumer<OnInstanceEnter> callback, Collection<Integer> templateIds)
	{
		return registerConsumer(callback, EventType.ON_INSTANCE_ENTER, ListenerRegisterType.INSTANCE, templateIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_INSTANCE_LEAVE callback for the given instance templates.
	 * @param callback action run when a player leaves an instance
	 * @param templateIds instance template identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setInstanceLeaveId(Consumer<OnInstanceLeave> callback, int... templateIds)
	{
		return registerConsumer(callback, EventType.ON_INSTANCE_LEAVE, ListenerRegisterType.INSTANCE, templateIds);
	}
	
	/**
	 * Registers an ON_INSTANCE_LEAVE callback for the given instance templates.
	 * @param callback action run when a player leaves an instance
	 * @param templateIds instance template identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setInstanceLeaveId(Consumer<OnInstanceLeave> callback, Collection<Integer> templateIds)
	{
		return registerConsumer(callback, EventType.ON_INSTANCE_LEAVE, ListenerRegisterType.INSTANCE, templateIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Registers an ON_INSTANCE_STATUS_CHANGE callback for the given instance templates.
	 * @param callback action run when instance status changes
	 * @param templateIds instance template identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setInstanceStatusChangeId(Consumer<OnInstanceStatusChange> callback, int... templateIds)
	{
		return registerConsumer(callback, EventType.ON_INSTANCE_STATUS_CHANGE, ListenerRegisterType.INSTANCE, templateIds);
	}
	
	/**
	 * Registers an ON_INSTANCE_STATUS_CHANGE callback for the given instance templates.
	 * @param callback action run when instance status changes
	 * @param templateIds instance template identifiers to monitor
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> setInstanceStatusChangeId(Consumer<OnInstanceStatusChange> callback, Collection<Integer> templateIds)
	{
		return registerConsumer(callback, EventType.ON_INSTANCE_STATUS_CHANGE, ListenerRegisterType.INSTANCE, templateIds);
	}
	
	// --------------------------------------------------------------------------------------------------
	// --------------------------------Default listener register methods---------------------------------
	// --------------------------------------------------------------------------------------------------
	
	/**
	 * Builds and registers a ConsumerEventListener for the given event and NPCs.
	 * @param callback action run when the event occurs
	 * @param type event category to listen for
	 * @param registerType registration scope
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> registerConsumer(Consumer<? extends IBaseEvent> callback, EventType type, ListenerRegisterType registerType, int... npcIds)
	{
		return registerListener(container -> new ConsumerEventListener(container, type, callback, this), registerType, npcIds);
	}
	
	/**
	 * Builds and registers a ConsumerEventListener for the given event and NPCs.
	 * @param callback action run when the event occurs
	 * @param type event category to listen for
	 * @param registerType registration scope
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> registerConsumer(Consumer<? extends IBaseEvent> callback, EventType type, ListenerRegisterType registerType, Collection<Integer> npcIds)
	{
		return registerListener(container -> new ConsumerEventListener(container, type, callback, this), registerType, npcIds);
	}
	
	/**
	 * Builds and registers a FunctionEventListener for the given event and NPCs.
	 * @param callback function run when the event occurs
	 * @param type event category to listen for
	 * @param registerType registration scope
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> registerFunction(Function<? extends IBaseEvent, ? extends AbstractEventReturn> callback, EventType type, ListenerRegisterType registerType, int... npcIds)
	{
		return registerListener(container -> new FunctionEventListener(container, type, callback, this), registerType, npcIds);
	}
	
	/**
	 * Builds and registers a FunctionEventListener for the given event and NPCs.
	 * @param callback function run when the event occurs
	 * @param type event category to listen for
	 * @param registerType registration scope
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> registerFunction(Function<? extends IBaseEvent, ? extends AbstractEventReturn> callback, EventType type, ListenerRegisterType registerType, Collection<Integer> npcIds)
	{
		return registerListener(container -> new FunctionEventListener(container, type, callback, this), registerType, npcIds);
	}
	
	/**
	 * Builds and registers a RunnableEventListener for the given event and NPCs.
	 * @param callback task run when the event occurs
	 * @param type event category that triggers execution
	 * @param registerType registration scope
	 * @param npcIds NPC identifiers to target
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> registerRunnable(Runnable callback, EventType type, ListenerRegisterType registerType, int... npcIds)
	{
		return registerListener(container -> new RunnableEventListener(container, type, callback, this), registerType, npcIds);
	}
	
	/**
	 * Registers a runnable task as an event listener for a collection of NPCs.
	 * @param callback task to execute when triggered
	 * @param type event type that triggers the callback
	 * @param registerType registration type used
	 * @param npcIds collection of NPC IDs affected by the listener
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> registerRunnable(Runnable callback, EventType type, ListenerRegisterType registerType, Collection<Integer> npcIds)
	{
		return registerListener(container -> new RunnableEventListener(container, type, callback, this), registerType, npcIds);
	}
	
	/**
	 * Registers an annotation-based listener for the given event and NPCs.
	 * @param callback method executed as the callback
	 * @param type event type to respond to
	 * @param registerType registration category
	 * @param priority execution order priority
	 * @param npcIds NPC IDs the listener applies to
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> registerAnnotation(Method callback, EventType type, ListenerRegisterType registerType, int priority, int... npcIds)
	{
		return registerListener(container -> new AnnotationEventListener(container, type, callback, this, priority), registerType, npcIds);
	}
	
	/**
	 * Registers an annotation-based listener for the given event and a collection of NPCs.
	 * @param callback method executed when the event occurs
	 * @param type event type to respond to
	 * @param registerType registration category
	 * @param priority execution order of the listener
	 * @param npcIds collection of NPC IDs this applies to
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> registerAnnotation(Method callback, EventType type, ListenerRegisterType registerType, int priority, Collection<Integer> npcIds)
	{
		return registerListener(container -> new AnnotationEventListener(container, type, callback, this, priority), registerType, npcIds);
	}
	
	/**
	 * Registers a dummy listener for the given event type and NPCs.
	 * @param type event type to register the dummy listener for
	 * @param registerType registration type used
	 * @param npcIds NPC IDs the listeners apply to
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> registerDummy(EventType type, ListenerRegisterType registerType, int... npcIds)
	{
		return registerListener(container -> new DummyEventListener(container, type, this), registerType, npcIds);
	}
	
	/**
	 * Registers a dummy listener for the given event type and a collection of NPCs.
	 * @param type event type associated with the dummy listener
	 * @param registerType registration type used
	 * @param npcIds collection of target NPC IDs
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> registerDummy(EventType type, ListenerRegisterType registerType, Collection<Integer> npcIds)
	{
		return registerListener(container -> new DummyEventListener(container, type, this), registerType, npcIds);
	}
	
	// --------------------------------------------------------------------------------------------------
	// --------------------------------------Register methods--------------------------------------------
	// --------------------------------------------------------------------------------------------------
	
	/**
	 * Registers a listener built from the given action for the supplied IDs, handling NPC, zone, item, castle, fortress, instance, and global scopes.
	 * @param action function mapping a listeners container to a listener
	 * @param registerType registration category
	 * @param ids identifiers to register; if empty, global types are used
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> registerListener(Function<ListenersContainer, AbstractEventListener> action, ListenerRegisterType registerType, int... ids)
	{
		final List<AbstractEventListener> listeners = new ArrayList<>(ids.length > 0 ? ids.length : 1);
		if (ids.length > 0)
		{
			for (int id : ids)
			{
				switch (registerType)
				{
					case NPC:
					{
						final NpcTemplate template = NpcData.getInstance().getTemplate(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case ZONE:
					{
						final ZoneType template = ZoneManager.getInstance().getZoneById(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case ITEM:
					{
						final ItemTemplate template = ItemData.getInstance().getTemplate(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case CASTLE:
					{
						final Castle template = CastleManager.getInstance().getCastleById(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case FORTRESS:
					{
						final Fort template = FortManager.getInstance().getFortById(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case INSTANCE:
					{
						final InstanceTemplate template = InstanceManager.getInstance().getInstanceTemplate(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					default:
					{
						LOGGER.warning(getClass().getSimpleName() + ": Unhandled register type: " + registerType);
					}
				}
				
				_registeredIds.computeIfAbsent(registerType, _ -> ConcurrentHashMap.newKeySet()).add(id);
			}
		}
		else
		{
			switch (registerType)
			{
				case OLYMPIAD:
				{
					final Olympiad template = Olympiad.getInstance();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL: // Global Listener
				{
					final ListenersContainer template = Containers.Global();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL_NPCS: // Global Npcs Listener
				{
					final ListenersContainer template = Containers.Npcs();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL_MONSTERS: // Global Monsters Listener
				{
					final ListenersContainer template = Containers.Monsters();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL_PLAYERS: // Global Players Listener
				{
					final ListenersContainer template = Containers.Players();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
			}
		}
		
		_listeners.addAll(listeners);
		return listeners;
	}
	
	/**
	 * Registers a listener built from the given action for a collection of IDs across the supported scopes.
	 * @param action function mapping a container to a listener
	 * @param registerType registration category
	 * @param ids collection of identifiers for specific templates
	 * @return registered listeners
	 */
	protected List<AbstractEventListener> registerListener(Function<ListenersContainer, AbstractEventListener> action, ListenerRegisterType registerType, Collection<Integer> ids)
	{
		final List<AbstractEventListener> listeners = new ArrayList<>(!ids.isEmpty() ? ids.size() : 1);
		if (!ids.isEmpty())
		{
			for (int id : ids)
			{
				switch (registerType)
				{
					case NPC:
					{
						final NpcTemplate template = NpcData.getInstance().getTemplate(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case ZONE:
					{
						final ZoneType template = ZoneManager.getInstance().getZoneById(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case ITEM:
					{
						final ItemTemplate template = ItemData.getInstance().getTemplate(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case CASTLE:
					{
						final Castle template = CastleManager.getInstance().getCastleById(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case FORTRESS:
					{
						final Fort template = FortManager.getInstance().getFortById(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case INSTANCE:
					{
						final InstanceTemplate template = InstanceManager.getInstance().getInstanceTemplate(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					default:
					{
						LOGGER.warning(getClass().getSimpleName() + ": Unhandled register type: " + registerType);
					}
				}
			}
			
			_registeredIds.computeIfAbsent(registerType, _ -> ConcurrentHashMap.newKeySet()).addAll(ids);
		}
		else
		{
			switch (registerType)
			{
				case OLYMPIAD:
				{
					final Olympiad template = Olympiad.getInstance();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL: // Global Listener
				{
					final ListenersContainer template = Containers.Global();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL_NPCS: // Global Npcs Listener
				{
					final ListenersContainer template = Containers.Npcs();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL_MONSTERS: // Global Monsters Listener
				{
					final ListenersContainer template = Containers.Monsters();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL_PLAYERS: // Global Players Listener
				{
					final ListenersContainer template = Containers.Players();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
			}
		}
		
		_listeners.addAll(listeners);
		return listeners;
	}
	
	/**
	 * Returns the registered IDs for the given listener type, or an empty set if none.
	 * @param type listener registration category to query
	 * @return set of registered IDs
	 */
	public Set<Integer> getRegisteredIds(ListenerRegisterType type)
	{
		return _registeredIds.getOrDefault(type, Collections.emptySet());
	}
	
	/**
	 * Returns the queue of active listeners.
	 * @return queue of registered listeners
	 */
	public Queue<AbstractEventListener> getListeners()
	{
		return _listeners;
	}
	
	/**
	 * Called when a spawn is activated.
	 * @param template the spawn template containing the new spawn data
	 */
	
	public void onSpawnActivate(SpawnTemplate template)
	{
	}
	
	/**
	 * Called when a spawn is deactivated.
	 * @param template the spawn template that was deactivated
	 */
	public void onSpawnDeactivate(SpawnTemplate template)
	{
	}
	
	/**
	 * Called when an NPC is spawned.
	 * @param template the spawn template used to create the NPC
	 * @param group the spawn group the NPC belongs to
	 * @param npc the newly created NPC
	 */
	public void onSpawnNpc(SpawnTemplate template, SpawnGroup group, Npc npc)
	{
	}
	
	/**
	 * Called when an NPC is spawned or despawned.
	 * @param template the spawn template used for the spawn
	 * @param group the spawn group containing the NPC
	 * @param npc the NPC being spawned or despawned
	 */
	public void onSpawnDespawnNpc(SpawnTemplate template, SpawnGroup group, Npc npc)
	{
	}
	
	/**
	 * Called when a template-spawned NPC dies.
	 * @param template the spawn template used to create the NPC
	 * @param group the spawn group the NPC belonged to
	 * @param npc the NPC that died
	 * @param killer the creature that killed the NPC
	 */
	public void onSpawnNpcDeath(SpawnTemplate template, SpawnGroup group, Npc npc, Creature killer)
	{
	}
	
	/**
	 * Shows on-screen text to the player for the given duration, skipping players that are simulating talking.
	 * @param player the player receiving the message
	 * @param text the message content to display
	 * @param time the display duration in milliseconds
	 */
	
	public static void showOnScreenMsg(Player player, String text, int time)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		player.sendPacket(new ExShowScreenMessage(text, time));
	}
	
	/**
	 * Shows an NPC-string on-screen message at the given position, skipping players that are simulating talking.
	 * @param player the player receiving the message
	 * @param npcString the NPC string id identifying the content
	 * @param position the screen position for the message
	 * @param time the display duration
	 * @param params arguments included in the message
	 */
	public static void showOnScreenMsg(Player player, NpcStringId npcString, int position, int time, String... params)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		player.sendPacket(new ExShowScreenMessage(npcString, position, time, params));
	}
	
	/**
	 * Shows an NPC-string on-screen message with an optional effect, skipping players that are simulating talking.
	 * @param player the player receiving the message
	 * @param npcString the NPC string id identifying the text
	 * @param position the screen position for the message
	 * @param time the display duration
	 * @param showEffect whether to play a visual effect
	 * @param params arguments used to format the message
	 */
	public static void showOnScreenMsg(Player player, NpcStringId npcString, int position, int time, boolean showEffect, String... params)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		player.sendPacket(new ExShowScreenMessage(npcString, position, time, showEffect, params));
	}
	
	/**
	 * Shows a system-message on-screen at the given position, skipping players that are simulating talking.
	 * @param player the player receiving the message
	 * @param systemMsg the identifier for the message type
	 * @param position the screen position for the message
	 * @param time the display duration in milliseconds
	 * @param params arguments inserted into the message template
	 */
	public static void showOnScreenMsg(Player player, SystemMessageId systemMsg, int position, int time, String... params)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		player.sendPacket(new ExShowScreenMessage(systemMsg, position, time, params));
	}
	
	/**
	 * Spawns an NPC at the given positionable location.
	 * @param npcId the identifier of the NPC to spawn
	 * @param pos the position where the NPC appears
	 * @return the newly created NPC
	 */
	public static Npc addSpawn(int npcId, IPositionable pos)
	{
		return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), false, 0, false, 0);
	}
	
	/**
	 * Spawns an NPC at the given location, optionally with random offset and despawn delay, linked to a summoner.
	 * @param summoner the NPC summoning the entity
	 * @param npcId the identifier of the NPC to spawn
	 * @param pos the position with coordinates and heading
	 * @param randomOffset whether to apply a random position offset
	 * @param despawnDelay milliseconds before the entity despawns
	 * @return the newly created NPC
	 */
	public static Npc addSpawn(Npc summoner, int npcId, IPositionable pos, boolean randomOffset, long despawnDelay)
	{
		return addSpawn(summoner, npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), randomOffset, despawnDelay, false, 0);
	}
	
	/**
	 * Spawns an NPC at the given positionable location, optionally as a summon.
	 * @param npcId the identifier of the NPC type
	 * @param pos the position with coordinates
	 * @param isSummonSpawn whether this is a summon spawn
	 * @return the newly created NPC
	 */
	public static Npc addSpawn(int npcId, IPositionable pos, boolean isSummonSpawn)
	{
		return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), false, 0, isSummonSpawn, 0);
	}
	
	/**
	 * Spawns an NPC at the given positionable location with optional random offset and despawn delay.
	 * @param npcId the identifier of the NPC to spawn
	 * @param pos the position with coordinates
	 * @param randomOffset whether to apply a random position variance
	 * @param despawnDelay milliseconds before the NPC despawns
	 * @return the newly created NPC
	 */
	public static Npc addSpawn(int npcId, IPositionable pos, boolean randomOffset, long despawnDelay)
	{
		return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), randomOffset, despawnDelay, false, 0);
	}
	
	/**
	 * Spawns an NPC at the given positionable location with optional offset, despawn delay, and summon flag.
	 * @param npcId the identifier of the NPC type
	 * @param pos the position with coordinates
	 * @param randomOffset whether to apply a random position offset
	 * @param despawnDelay milliseconds before the NPC despawns
	 * @param isSummonSpawn whether the spawn was created by a summon
	 * @return the newly created spawn
	 */
	public static Npc addSpawn(int npcId, IPositionable pos, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), randomOffset, despawnDelay, isSummonSpawn, 0);
	}
	
	/**
	 * Spawns an NPC at the given positionable location for a specific instance, linked to a summoner.
	 * @param summoner the NPC summoning the entity
	 * @param npcId the identifier of the NPC type
	 * @param pos the position with coordinates
	 * @param randomOffset whether to apply a random position offset
	 * @param instanceId the identifier of the NPC instance
	 * @return the newly created NPC
	 */
	public static Npc addSpawn(Npc summoner, int npcId, IPositionable pos, boolean randomOffset, int instanceId)
	{
		return addSpawn(summoner, npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), randomOffset, 0, false, instanceId);
	}
	
	/**
	 * Spawns an NPC at the given positionable location with optional offset, despawn delay, summon flag, and instance.
	 * @param npcId the identifier of the NPC type
	 * @param pos the position with coordinates
	 * @param randomOffset whether to apply a random position offset
	 * @param despawnDelay milliseconds before the NPC despawns
	 * @param isSummonSpawn whether this is a summoned entity
	 * @param instanceId the identifier of the world instance
	 * @return the created NPC
	 */
	public static Npc addSpawn(int npcId, IPositionable pos, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, int instanceId)
	{
		return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), randomOffset, despawnDelay, isSummonSpawn, instanceId);
	}
	
	/**
	 * Spawns an NPC at the given coordinates with optional random offset and despawn delay.
	 * @param npcId the identifier of the NPC type to spawn
	 * @param x the X coordinate of the spawn location
	 * @param y the Y coordinate of the spawn location
	 * @param z the Z coordinate of the spawn location
	 * @param heading the direction the NPC faces
	 * @param randomOffset whether to add random variation to the coordinates
	 * @param despawnDelay milliseconds before the NPC despawns
	 * @return the newly created NPC
	 */
	public static Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay)
	{
		return addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay, false, 0);
	}
	
	/**
	 * Spawns an NPC at the given coordinates with optional offset, despawn delay, and summon flag.
	 * @param npcId the identifier of the NPC type
	 * @param x the X coordinate of the spawn point
	 * @param y the Y coordinate of the spawn point
	 * @param z the Z coordinate of the spawn point
	 * @param heading the direction the NPC faces
	 * @param randomOffset whether to add random variation to the coordinates
	 * @param despawnDelay milliseconds before the NPC despawns
	 * @param isSummonSpawn whether the spawn was created by a summon
	 * @return the newly created NPC
	 */
	public static Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		return addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay, isSummonSpawn, 0);
	}
	
	/**
	 * Spawns an NPC at the given coordinates with optional offset, despawn delay, summon flag, and instance.
	 * @param npcId the identifier of the NPC type to spawn
	 * @param x the X coordinate of the spawn location
	 * @param y the Y coordinate of the spawn location
	 * @param z the Z coordinate of the spawn location
	 * @param heading the direction the NPC faces
	 * @param randomOffset whether to add random variation to the coordinates
	 * @param despawnDelay milliseconds before the NPC despawns
	 * @param isSummonSpawn whether the NPC was created via a summon
	 * @param instanceId the identifier of the NPC instance
	 * @return the newly created NPC
	 */
	public static Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, int instanceId)
	{
		return addSpawn(null, npcId, x, y, z, heading, randomOffset, despawnDelay, isSummonSpawn, instanceId);
	}
	
	/**
	 * Spawns and broadcasts an NPC at the given coordinates, applying offset, despawn timer, summon flag, and instance, optionally linked to a summoner.
	 * @param summoner the owning NPC, or null
	 * @param npcId the identifier of the NPC type to spawn
	 * @param xValue the X coordinate of the spawn location
	 * @param yValue the Y coordinate of the spawn location
	 * @param zValue the Z coordinate of the spawn location
	 * @param heading the direction the NPC should face
	 * @param randomOffset whether to apply a random offset to X and Y
	 * @param despawnDelay milliseconds before the NPC despawns
	 * @param isSummonSpawn whether to treat the NPC as a summon
	 * @param instance the instance id for the spawn
	 * @return the spawned NPC, or null if the spawn failed
	 */
	public static Npc addSpawn(Npc summoner, int npcId, int xValue, int yValue, int zValue, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, int instance)
	{
		try
		{
			if ((xValue == 0) && (yValue == 0))
			{
				LOGGER.severe("addSpawn(): invalid spawn coordinates for NPC #" + npcId + "!");
				return null;
			}
			
			int x = xValue;
			int y = yValue;
			if (randomOffset)
			{
				int offset = Rnd.get(50, 100);
				if (Rnd.nextBoolean())
				{
					offset *= -1;
				}
				
				x += offset;
				offset = Rnd.get(50, 100);
				if (Rnd.nextBoolean())
				{
					offset *= -1;
				}
				
				y += offset;
			}
			
			final Spawn spawn = new Spawn(npcId);
			spawn.setInstanceId(instance);
			spawn.setHeading(heading);
			spawn.setXYZ(x, y, zValue);
			spawn.stopRespawn();
			
			final Npc npc = spawn.doSpawn(isSummonSpawn);
			if (despawnDelay > 0)
			{
				npc.scheduleDespawn(despawnDelay);
			}
			
			if (summoner != null)
			{
				summoner.addSummonedNpc(npc);
			}
			
			// Retain monster original position if ENABLE_RANDOM_MONSTER_SPAWNS is enabled.
			if (RandomSpawnsConfig.ENABLE_RANDOM_MONSTER_SPAWNS && !randomOffset && npc.isMonster())
			{
				spawn.setXYZ(x, y, zValue);
				npc.setXYZ(x, y, zValue);
				if (heading > -1)
				{
					npc.setHeading(heading);
				}
			}
			
			// Fixes invisible NPCs spawned by script.
			npc.broadcastInfo();
			
			return npc;
		}
		catch (Exception e)
		{
			LOGGER.warning("Could not spawn NPC #" + npcId + "; error: " + e.getMessage());
		}
		
		return null;
	}
	
	/**
	 * Creates and spawns an invincible trap with full HP and MP at the given coordinates.
	 * @param trapId the identifier of the NPC template used as a trap
	 * @param x the X coordinate of the spawn location
	 * @param y the Y coordinate of the spawn location
	 * @param z the Z coordinate of the spawn location
	 * @param heading the direction the trap faces
	 * @param instanceId the identifier of the trap instance
	 * @return the newly spawned trap
	 */
	public Trap addTrap(int trapId, int x, int y, int z, int heading, int instanceId)
	{
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(trapId);
		final Trap trap = new Trap(npcTemplate, instanceId, -1);
		trap.setCurrentHp(trap.getMaxHp());
		trap.setCurrentMp(trap.getMaxMp());
		trap.setInvul(true);
		trap.setHeading(heading);
		trap.spawnMe(x, y, z);
		return trap;
	}
	
	/**
	 * Spawns a minion for the given master monster.
	 * @param master the monster acting as the minion master
	 * @param minionId the identifier of the minion type to spawn
	 * @return the newly created minion
	 */
	public Npc addMinion(Monster master, int minionId)
	{
		return MinionList.spawnMinion(master, minionId);
	}
	
	/**
	 * Returns the total count of the given item in the player inventory.
	 * @param player the player to check
	 * @param itemId the identifier of the item
	 * @return the total number of items found
	 */
	public static long getQuestItemsCount(Player player, int itemId)
	{
		return player.getInventory().getInventoryItemCount(itemId, -1);
	}
	
	/**
	 * Returns the combined count of the given items in the player inventory, capped at {@code Long.MAX_VALUE}.
	 * @param player the player whose inventory is checked
	 * @param itemIds the identifiers of the items to count
	 * @return the total sum of counts for all matching items
	 */
	public long getQuestItemsCount(Player player, int... itemIds)
	{
		long count = 0;
		for (Item item : player.getInventory().getItems())
		{
			if (item == null)
			{
				continue;
			}
			
			for (int itemId : itemIds)
			{
				if (item.getId() == itemId)
				{
					final long itemCount = item.getCount();
					if (itemCount > (Long.MAX_VALUE - count))
					{
						return Long.MAX_VALUE;
					}
					
					count += itemCount;
				}
			}
		}
		
		return count;
	}
	
	/**
	 * Checks whether the player has the given item, verifying the required count.
	 * @param player the player to check
	 * @param item the item to look for
	 * @return {@code true} if the player has the item
	 */
	protected static boolean hasItem(Player player, ItemHolder item)
	{
		return hasItem(player, item, true);
	}
	
	/**
	 * Checks whether the player has the given item, optionally verifying the required count.
	 * @param player the player to check
	 * @param item the required item
	 * @param checkCount whether to verify the required amount
	 * @return {@code true} if the requirements are met
	 */
	protected static boolean hasItem(Player player, ItemHolder item, boolean checkCount)
	{
		if (item == null)
		{
			return false;
		}
		
		if (checkCount)
		{
			return getQuestItemsCount(player, item.getId()) >= item.getCount();
		}
		
		return hasQuestItems(player, item.getId());
	}
	
	/**
	 * Checks whether the player has every item in the list, optionally verifying counts; returns false for an empty list.
	 * @param player the player to check
	 * @param checkCount whether to verify item quantities
	 * @param itemList the items to validate
	 * @return {@code true} if all items are present
	 */
	protected static boolean hasAllItems(Player player, boolean checkCount, ItemHolder... itemList)
	{
		if ((itemList == null) || (itemList.length == 0))
		{
			return false;
		}
		
		for (ItemHolder item : itemList)
		{
			if (!hasItem(player, item, checkCount))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks whether the given item exists in the player inventory.
	 * @param player the player to check
	 * @param itemId the identifier of the item to find
	 * @return {@code true} if the item is found
	 */
	public static boolean hasQuestItems(Player player, int itemId)
	{
		return player.getInventory().getItemByItemId(itemId) != null;
	}
	
	/**
	 * Checks whether the player has all the given items; returns false for an empty array.
	 * @param player the player to check
	 * @param itemIds the identifiers of the required items
	 * @return {@code true} if all items are found
	 */
	public static boolean hasQuestItems(Player player, int... itemIds)
	{
		if ((itemIds == null) || (itemIds.length == 0))
		{
			return false;
		}
		
		final PlayerInventory inv = player.getInventory();
		for (int itemId : itemIds)
		{
			if (inv.getItemByItemId(itemId) == null)
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks whether the player has any of the given items in their inventory.
	 * @param player the player whose inventory is checked
	 * @param itemIds the identifiers of the items to look for
	 * @return {@code true} if at least one item exists
	 */
	public boolean hasAtLeastOneQuestItem(Player player, int... itemIds)
	{
		final PlayerInventory inv = player.getInventory();
		for (int itemId : itemIds)
		{
			if (inv.getItemByItemId(itemId) != null)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks whether the player owns any of the given items across inventory, warehouse, pet, servitors, mail attachments, and active auctions.
	 * @param player the player to check
	 * @param itemIds the identifiers of the items to look for
	 * @return {@code true} if at least one item is found
	 */
	public boolean ownsAtLeastOneItem(Player player, int... itemIds)
	{
		// Inventory.
		final PlayerInventory inventory = player.getInventory();
		for (int itemId : itemIds)
		{
			if (inventory.getItemByItemId(itemId) != null)
			{
				return true;
			}
		}
		
		// Warehouse.
		final PlayerWarehouse warehouse = player.getWarehouse();
		for (int itemId : itemIds)
		{
			if (warehouse.getItemByItemId(itemId) != null)
			{
				return true;
			}
		}
		
		// Pet.
		if (player.hasPet())
		{
			final PetInventory petInventory = player.getPet().getInventory();
			if (petInventory != null)
			{
				for (int itemId : itemIds)
				{
					if (petInventory.getItemByItemId(itemId) != null)
					{
						return true;
					}
				}
			}
		}
		
		// Summons.
		if (player.hasServitors())
		{
			for (Summon summon : player.getServitors().values())
			{
				final PetInventory summonInventory = summon.getInventory();
				if (summonInventory != null)
				{
					for (int itemId : itemIds)
					{
						if (summonInventory.getItemByItemId(itemId) != null)
						{
							return true;
						}
					}
				}
			}
		}
		
		// Mail attachments.
		if (GeneralConfig.ALLOW_MAIL)
		{
			final int objectId = player.getObjectId();
			final List<MailMessage> inbox = MailManager.getInstance().getInbox(objectId);
			for (int itemId : itemIds)
			{
				for (MailMessage message : inbox)
				{
					final Mail mail = message.getAttachments();
					if ((mail != null) && (mail.getItemByItemId(itemId) != null))
					{
						return true;
					}
				}
			}
			
			final List<MailMessage> outbox = MailManager.getInstance().getOutbox(objectId);
			for (int itemId : itemIds)
			{
				for (MailMessage message : outbox)
				{
					final Mail mail = message.getAttachments();
					if ((mail != null) && (mail.getItemByItemId(itemId) != null))
					{
						return true;
					}
				}
			}
		}
		
		// Item auctions.
		for (int itemId : itemIds)
		{
			if (ItemCommissionManager.getInstance().hasCommissionedItemId(player, itemId))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns the enchant level of the given item in the player inventory, or 0 if not found.
	 * @param player the player to check
	 * @param itemId the identifier of the item
	 * @return the enchant level
	 */
	public static int getEnchantLevel(Player player, int itemId)
	{
		final Item enchantedItem = player.getInventory().getItemByItemId(itemId);
		if (enchantedItem == null)
		{
			return 0;
		}
		
		return enchantedItem.getEnchantLevel();
	}
	
	/**
	 * Gives Adena to the player, optionally applying server reward rates.
	 * @param player the player receiving the Adena
	 * @param count the amount of Adena to give
	 * @param applyRates whether to apply reward rate logic instead of a direct grant
	 */
	public void giveAdena(Player player, long count, boolean applyRates)
	{
		if (applyRates)
		{
			rewardItems(player, Inventory.ADENA_ID, count);
		}
		else
		{
			giveItems(player, Inventory.ADENA_ID, count);
		}
	}
	
	/**
	 * Rewards the player with the item described by the given holder.
	 * @param player the player receiving the items
	 * @param holder the holder containing the reward data
	 */
	public static void rewardItems(Player player, ItemHolder holder)
	{
		rewardItems(player, holder.getId(), holder.getCount());
	}
	
	/**
	 * Rewards the player with an item, applying type-based reward multipliers; skips simulated talking and non-positive counts.
	 * @param player the player receiving the items
	 * @param itemId the identifier of the item to give
	 * @param countValue the base amount to reward
	 */
	public static void rewardItems(Player player, int itemId, long countValue)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		if (countValue <= 0)
		{
			return;
		}
		
		final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
		if (item == null)
		{
			return;
		}
		
		long count = countValue;
		try
		{
			if (itemId == Inventory.ADENA_ID)
			{
				count *= RatesConfig.RATE_QUEST_REWARD_ADENA;
			}
			else if (RatesConfig.RATE_QUEST_REWARD_USE_MULTIPLIERS)
			{
				if (item instanceof EtcItem)
				{
					switch (((EtcItem) item).getItemType())
					{
						case POTION:
						{
							count *= RatesConfig.RATE_QUEST_REWARD_POTION;
							break;
						}
						case ENCHT_WP:
						case ENCHT_AM:
						case SCROLL:
						{
							count *= RatesConfig.RATE_QUEST_REWARD_SCROLL;
							break;
						}
						case RECIPE:
						{
							count *= RatesConfig.RATE_QUEST_REWARD_RECIPE;
							break;
						}
						case MATERIAL:
						{
							count *= RatesConfig.RATE_QUEST_REWARD_MATERIAL;
							break;
						}
						default:
						{
							count *= RatesConfig.RATE_QUEST_REWARD;
						}
					}
				}
			}
			else
			{
				count *= RatesConfig.RATE_QUEST_REWARD;
			}
		}
		catch (Exception e)
		{
			count = Long.MAX_VALUE;
		}
		
		// Add items to player's inventory.
		final Item itemInstance = player.getInventory().addItem(ItemProcessType.QUEST, itemId, count, player, player.getTarget());
		if (itemInstance == null)
		{
			return;
		}
		
		sendItemGetMessage(player, itemInstance, count);
	}
	
	/**
	 * Sends the appropriate item- or Adena-obtained message and refreshes the player weight and Adena packets.
	 * @param player the player receiving the message
	 * @param item the item being rewarded
	 * @param count the amount of the reward to display
	 */
	private static void sendItemGetMessage(Player player, Item item, long count)
	{
		// If item for reward is gold, send message of gold reward to client.
		if (item.getId() == Inventory.ADENA_ID)
		{
			final SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_ADENA_2);
			smsg.addLong(count);
			player.sendPacket(smsg);
		}
		// Otherwise, send message of object reward to client.
		else if (count > 1)
		{
			final SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
			smsg.addItemName(item);
			smsg.addLong(count);
			player.sendPacket(smsg);
		}
		else
		{
			final SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_2);
			smsg.addItemName(item);
			player.sendPacket(smsg);
		}
		
		// send packets
		player.sendPacket(new ExUserInfoInvenWeight(player));
		player.sendPacket(new ExAdenaInvenCount(player));
	}
	
	/**
	 * Gives a specific amount of an item to a player.
	 * @param player the player who will receive the items
	 * @param itemId the identifier of the item to give
	 * @param count the number of items to provide
	 */
	public static void giveItems(Player player, int itemId, long count)
	{
		giveItems(player, itemId, count, 0, false);
	}
	
	/**
	 * Gives a specific amount of an item to a player with an optional sound effect.
	 * @param player the player who will receive the items
	 * @param itemId the identifier of the item type
	 * @param count the number of items to give
	 * @param playSound true to play a sound effect, false to remain silent
	 */
	public static void giveItems(Player player, int itemId, long count, boolean playSound)
	{
		giveItems(player, itemId, count, 0, playSound);
	}
	
	/**
	 * Gives items to a player using the id and count from an item holder.
	 * @param player the player who will receive the items
	 * @param holder the holder containing the item data
	 */
	protected void giveItems(Player player, ItemHolder holder)
	{
		giveItems(player, holder.getId(), holder.getCount());
	}
	
	/**
	 * Gives a specific amount of an item to a player, applying quest multipliers, enchant level, and optional sound.
	 * @param player the player receiving the items
	 * @param itemId the identifier of the item template
	 * @param count the amount of items to give
	 * @param enchantlevel the enchantment level to apply (ignored for adena)
	 * @param playSound whether to play a sound effect on receipt
	 */
	public static void giveItems(Player player, int itemId, long count, int enchantlevel, boolean playSound)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		if (count <= 0)
		{
			return;
		}
		
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		if (template == null)
		{
			return;
		}
		
		// Apply quest item drop amount multiplier configuration.
		long finalCount = count;
		if (template.isQuestItem())
		{
			finalCount = (long) (count * RatesConfig.QUEST_ITEM_DROP_AMOUNT_MULTIPLIER);
		}
		
		// Add items to player's inventory.
		final Item item = player.getInventory().addItem(ItemProcessType.QUEST, itemId, finalCount, player, player.getTarget());
		if (item == null)
		{
			return;
		}
		
		// Set enchant level for item if that item is not adena.
		if ((enchantlevel > 0) && (itemId != Inventory.ADENA_ID))
		{
			item.setEnchantLevel(enchantlevel);
		}
		
		if (playSound)
		{
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		
		sendItemGetMessage(player, item, finalCount);
	}
	
	/**
	 * Gives a specific amount of an item to a player with an optional attribute assignment.
	 * @param player the player receiving the items
	 * @param itemId the identifier of the item template
	 * @param count the amount of the item to give
	 * @param attributeType the type of attribute to apply
	 * @param attributeValue the value of the attribute to set
	 */
	public static void giveItems(Player player, int itemId, long count, AttributeType attributeType, int attributeValue)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		if (count <= 0)
		{
			return;
		}
		
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		if (template == null)
		{
			return;
		}
		
		// Apply quest item drop amount multiplier configuration.
		long finalCount = count;
		if (template.isQuestItem())
		{
			finalCount = (long) (count * RatesConfig.QUEST_ITEM_DROP_AMOUNT_MULTIPLIER);
		}
		
		// Add items to player's inventory.
		final Item item = player.getInventory().addItem(ItemProcessType.QUEST, itemId, finalCount, player, player.getTarget());
		if (item == null)
		{
			return;
		}
		
		// Set value for given attribute type.
		if ((attributeType != null) && (attributeValue > 0))
		{
			item.setAttribute(new AttributeHolder(attributeType, attributeValue), true);
			if (item.isEquipped())
			{
				player.getStat().recalculateStats(true);
			}
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(item);
			player.sendInventoryUpdate(iu);
		}
		
		sendItemGetMessage(player, item, finalCount);
	}
	
	/**
	 * Gives a random amount of an item to a player based on a drop chance, capped by a limit.
	 * @param player the player who will receive the item
	 * @param itemId the identifier of the item to give
	 * @param amountToGive the maximum quantity of the item to give
	 * @param limit the upper bound for the held amount
	 * @param dropChance the success rate between 0.0 and 1.0
	 * @param playSound whether to play a sound effect when the item is given
	 * @return {@code true} if the item was given, {@code false} otherwise
	 */
	public static boolean giveItemRandomly(Player player, int itemId, long amountToGive, long limit, double dropChance, boolean playSound)
	{
		return giveItemRandomly(player, null, itemId, amountToGive, amountToGive, limit, dropChance, playSound);
	}
	
	/**
	 * Gives a random amount of an item from an npc to a player based on a drop chance, capped by a limit.
	 * @param player the player who will receive the item
	 * @param npc the npc that provides the item
	 * @param itemId the identifier of the item to give
	 * @param amountToGive the maximum amount of the item to give
	 * @param limit the upper bound for the held amount
	 * @param dropChance the probability of the drop occurring
	 * @param playSound whether to play a sound effect when the item is given
	 * @return {@code true} if the item was given, {@code false} otherwise
	 */
	public static boolean giveItemRandomly(Player player, Npc npc, int itemId, long amountToGive, long limit, double dropChance, boolean playSound)
	{
		return giveItemRandomly(player, npc, itemId, amountToGive, amountToGive, limit, dropChance, playSound);
	}
	
	/**
	 * Gives a random quest-item amount to a player, applying champion multipliers and respecting the held limit.
	 * @param player the player who will receive the item
	 * @param npc the npc associated with the reward
	 * @param itemId the identifier of the item to give
	 * @param minAmount the minimum quantity to award
	 * @param maxAmount the maximum quantity to award
	 * @param limit the maximum total a player can hold, or 0 for no limit
	 * @param dropChance the probability between 0.0 and 1.0
	 * @param playSound whether to play a sound effect on receipt
	 * @return {@code true} if an item was given or the limit was reached, {@code false} otherwise
	 */
	public static boolean giveItemRandomly(Player player, Npc npc, int itemId, long minAmount, long maxAmount, long limit, double dropChance, boolean playSound)
	{
		if (player.isSimulatingTalking())
		{
			return false;
		}
		
		final long currentCount = getQuestItemsCount(player, itemId);
		if ((limit > 0) && (currentCount >= limit))
		{
			return true;
		}
		
		long minAmountWithBonus = (long) (minAmount * RatesConfig.QUEST_ITEM_DROP_AMOUNT_MULTIPLIER);
		long maxAmountWithBonus = (long) (maxAmount * RatesConfig.QUEST_ITEM_DROP_AMOUNT_MULTIPLIER);
		double dropChanceWithBonus = dropChance;
		if ((npc != null) && ChampionMonstersConfig.CHAMPION_ENABLE && npc.isChampion())
		{
			if ((itemId == Inventory.ADENA_ID) || (itemId == Inventory.ANCIENT_ADENA_ID))
			{
				dropChanceWithBonus *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_CHANCE;
				minAmountWithBonus *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT;
				maxAmountWithBonus *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT;
			}
			else
			{
				dropChanceWithBonus *= ChampionMonstersConfig.CHAMPION_REWARDS_CHANCE;
				minAmountWithBonus *= ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT;
				maxAmountWithBonus *= ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT;
			}
		}
		
		long amountToGive = (minAmountWithBonus == maxAmountWithBonus) ? minAmountWithBonus : Rnd.get(minAmountWithBonus, maxAmountWithBonus);
		final double random = Rnd.nextDouble();
		
		// Inventory slot check (almost useless for non-stacking items).
		if ((dropChanceWithBonus >= random) && (amountToGive > 0) && player.getInventory().validateCapacityByItemId(itemId))
		{
			if ((limit > 0) && ((currentCount + amountToGive) > limit))
			{
				amountToGive = limit - currentCount;
			}
			
			// Give the item to player.
			if (player.addItem(ItemProcessType.QUEST, itemId, amountToGive, npc, true) != null)
			{
				// Limit reached (if there is no limit, this block doesn't execute).
				if ((currentCount + amountToGive) == limit)
				{
					if (playSound)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
					
					return true;
				}
				
				if (playSound)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				// If there is no limit, return true every time an item is given.
				if (limit <= 0)
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Removes a quantity of an item from a player inventory, unequipping it first if needed.
	 * @param player the player who will lose the items
	 * @param itemId the identifier of the item to remove
	 * @param amountValue the quantity to take, clamped to the held count
	 * @return {@code true} if the items were removed, {@code false} otherwise
	 */
	public static boolean takeItems(Player player, int itemId, long amountValue)
	{
		if (player.isSimulatingTalking())
		{
			return false;
		}
		
		// Get object item from player's inventory list.
		final Item item = player.getInventory().getItemByItemId(itemId);
		if (item == null)
		{
			return false;
		}
		
		// Tests on count value in order not to have negative value.
		long amount = amountValue;
		if ((amountValue < 0) || (amountValue > item.getCount()))
		{
			amount = item.getCount();
		}
		
		// Destroy the quantity of items wanted.
		if (item.isEquipped())
		{
			final InventoryUpdate iu = new InventoryUpdate();
			for (Item itm : player.getInventory().unEquipItemInBodySlotAndRecord(item.getTemplate().getBodyPart()))
			{
				iu.addModifiedItem(itm);
			}
			
			// Sent on its own, the removal of the same item would replace the unequip and leave it shown as equipped.
			player.flushInventoryUpdate();
			player.sendPacket(iu);
			player.broadcastUserInfo();
		}
		
		return player.destroyItemByItemId(ItemProcessType.QUEST, itemId, amount, player, true);
	}
	
	/**
	 * Removes items from a player based on an item holder.
	 * @param player the player who will lose the items
	 * @param holder the holder containing the item id and count to remove
	 * @return {@code true} if the items were removed, {@code false} otherwise
	 */
	protected static boolean takeItem(Player player, ItemHolder holder)
	{
		if (holder == null)
		{
			return false;
		}
		
		return takeItems(player, holder.getId(), holder.getCount());
	}
	
	/**
	 * Removes a list of items from a player only if all are present.
	 * @param player the player who will lose the items
	 * @param itemList the items to remove
	 * @return {@code true} if all items were removed, {@code false} otherwise
	 */
	protected static boolean takeAllItems(Player player, ItemHolder... itemList)
	{
		if (player.isSimulatingTalking())
		{
			return false;
		}
		
		if ((itemList == null) || (itemList.length == 0))
		{
			return false;
		}
		
		// First check if the player has all items to avoid taking half the items from the list.
		if (!hasAllItems(player, true, itemList))
		{
			return false;
		}
		
		for (ItemHolder item : itemList)
		{
			// This should never be false, but just in case.
			if (!takeItem(player, item))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Removes a quantity of each of several items from a player inventory.
	 * @param player the player who will lose the items
	 * @param amount the quantity of each item to remove
	 * @param itemIds the ids of the items to take
	 * @return {@code true} if all items were removed, {@code false} otherwise
	 */
	public static boolean takeItems(Player player, int amount, int... itemIds)
	{
		if (player.isSimulatingTalking())
		{
			return false;
		}
		
		boolean check = true;
		if (itemIds != null)
		{
			for (int item : itemIds)
			{
				check &= takeItems(player, item, amount);
			}
		}
		
		return check;
	}
	
	/**
	 * Plays a sound effect for all players in an instance.
	 * @param world the instance where the sound will be played
	 * @param sound the name of the sound to play
	 */
	public static void playSound(Instance world, String sound)
	{
		world.broadcastPacket(new PlaySound(sound));
	}
	
	/**
	 * Plays a sound for a player by sound name.
	 * @param player the player who will hear the sound
	 * @param sound the name of the sound to play
	 */
	public static void playSound(Player player, String sound)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		player.sendPacket(QuestSound.getSound(sound));
	}
	
	/**
	 * Plays a quest sound for a player.
	 * @param player the player who will hear the sound
	 * @param sound the quest sound to play
	 */
	public static void playSound(Player player, QuestSound sound)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		player.sendPacket(sound.getPacket());
	}
	
	/**
	 * Adds experience and skill points to a player, applying premium and quest rates and granting PC Cafe points.
	 * @param player the player receiving the rewards
	 * @param exp the base amount of experience to add
	 * @param sp the base amount of skill points to add
	 */
	public static void addExpAndSp(Player player, long exp, int sp)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		long addExp = exp;
		int addSp = sp;
		
		// Premium rates
		if (player.hasPremiumStatus())
		{
			addExp *= PremiumSystemConfig.PREMIUM_RATE_QUEST_XP;
			addSp *= PremiumSystemConfig.PREMIUM_RATE_QUEST_SP;
		}
		
		final long rewardExp = (long) (addExp * RatesConfig.RATE_QUEST_REWARD_XP);
		player.addExpAndSp(rewardExp, (int) (addSp * RatesConfig.RATE_QUEST_REWARD_SP));
		PcCafePointsManager.getInstance().givePcCafePoint(player, rewardExp);
	}
	
	/**
	 * Adds faction points to a player scaled by the quest reward rate.
	 * @param player the player receiving the points
	 * @param faction the faction being updated
	 * @param points the base number of points to add
	 */
	public static void addFactionPoints(Player player, Faction faction, int points)
	{
		player.addFactionPoints(faction, (int) (points * RatesConfig.RATE_QUEST_REWARD_FP));
	}
	
	/**
	 * Generates a random integer.
	 * @param max the upper limit (exclusive)
	 * @return a random integer between 0 and max
	 */
	public static int getRandom(int max)
	{
		return Rnd.get(max);
	}
	
	/**
	 * Generates a random integer within a range.
	 * @param min the minimum value of the range
	 * @param max the maximum value of the range
	 * @return a random integer between min and max
	 */
	public static int getRandom(int min, int max)
	{
		return Rnd.get(min, max);
	}
	
	/**
	 * Generates a random long.
	 * @param max the upper limit (exclusive)
	 * @return a random long between 0 and max
	 */
	public static long getRandom(long max)
	{
		return Rnd.get(max);
	}
	
	/**
	 * Generates a random long within a range.
	 * @param min the minimum possible value
	 * @param max the maximum possible value
	 * @return a random long between min and max
	 */
	public static long getRandom(long min, long max)
	{
		return Rnd.get(min, max);
	}
	
	/**
	 * Generates a random boolean value.
	 * @return a randomly selected boolean value
	 */
	public static boolean getRandomBoolean()
	{
		return Rnd.nextBoolean();
	}
	
	/**
	 * Selects a random element from the provided array.
	 * @param <T> the type of elements in the array
	 * @param array the array of elements to choose from
	 * @return a random element, or null if the array is empty
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getRandomEntry(T... array)
	{
		if (array.length == 0)
		{
			return null;
		}
		
		return array[Rnd.get(array.length)];
	}
	
	/**
	 * Selects a random element from the provided list.
	 * @param <T> the type of elements in the list
	 * @param list the list to select an item from
	 * @return a random element, or null if the list is empty
	 */
	public static <T> T getRandomEntry(List<T> list)
	{
		if (list.isEmpty())
		{
			return null;
		}
		
		return list.get(Rnd.get(list.size()));
	}
	
	/**
	 * Selects a random integer from the provided array.
	 * @param array the array of integers to choose from
	 * @return a randomly selected integer from the array
	 */
	public static int getRandomEntry(int... array)
	{
		return array[Rnd.get(array.length)];
	}
	
	/**
	 * Retrieves the item id from a player equipment slot.
	 * @param player the player to check
	 * @param slot the index of the equipment slot
	 * @return the id of the item in that slot, or 0 if empty
	 */
	public static int getItemEquipped(Player player, int slot)
	{
		return player.getInventory().getPaperdollItemId(slot);
	}
	
	/**
	 * Retrieves the current number of game ticks.
	 * @return the current count of game ticks
	 */
	public static int getGameTicks()
	{
		return GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	/**
	 * Executes an action for a player and optionally their party or command channel members.
	 * @param player the primary player to perform the action on
	 * @param npc the npc associated with the action
	 * @param isSummon whether the action involves a summon
	 * @param includeParty whether party members should receive the action
	 * @param includeCommandChannel whether command channel members should receive the action
	 */
	public void executeForEachPlayer(Player player, Npc npc, boolean isSummon, boolean includeParty, boolean includeCommandChannel)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		if ((includeParty || includeCommandChannel) && player.isInParty())
		{
			final Party party = player.getParty();
			if (includeCommandChannel && party.isInCommandChannel())
			{
				party.getCommandChannel().forEachMember(member ->
				{
					actionForEachPlayer(member, npc, isSummon);
					return true;
				});
			}
			else if (includeParty)
			{
				party.forEachMember(member ->
				{
					actionForEachPlayer(member, npc, isSummon);
					return true;
				});
			}
		}
		else
		{
			actionForEachPlayer(player, npc, isSummon);
		}
	}
	
	/**
	 * Executes an action for each player involved, intended to be overridden by quest scripts.
	 * @param player the player the action targets
	 * @param npc the npc associated with the action
	 * @param isSummon whether the action involves a summon
	 */
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		// To be overridden in quest scripts.
	}
	
	/**
	 * Opens a door within an instance if it exists and is closed.
	 * @param doorId the identifier of the door
	 * @param instanceId the identifier of the instance
	 */
	public void openDoor(int doorId, int instanceId)
	{
		final Door door = getDoor(doorId, instanceId);
		if (door == null)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": called openDoor(" + doorId + ", " + instanceId + "); but door was not found!", new NullPointerException());
		}
		else if (!door.isOpen())
		{
			door.openMe();
		}
	}
	
	/**
	 * Closes a door within an instance if it exists and is open.
	 * @param doorId the identifier of the door
	 * @param instanceId the identifier of the instance
	 */
	public void closeDoor(int doorId, int instanceId)
	{
		final Door door = getDoor(doorId, instanceId);
		if (door == null)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": called closeDoor(" + doorId + ", " + instanceId + "); but door was not found!", new NullPointerException());
		}
		else if (door.isOpen())
		{
			door.closeMe();
		}
	}
	
	/**
	 * Retrieves a door, preferring an instance-scoped lookup and falling back to global data.
	 * @param doorId the identifier of the door
	 * @param instanceId the identifier of the instance
	 * @return the door if found, or null
	 */
	public Door getDoor(int doorId, int instanceId)
	{
		Door door = null;
		final Instance instance = InstanceManager.getInstance().getInstance(instanceId);
		if (instance != null)
		{
			door = instance.getDoor(doorId);
		}
		else
		{
			door = DoorData.getInstance().getDoor(doorId);
		}
		
		return door;
	}
	
	/**
	 * Adds a default attack desire for an npc against a playable target.
	 * @param npc the npc that will perform the action
	 * @param playable the playable entity that becomes the target
	 */
	protected void addAttackPlayerDesire(Npc npc, Playable playable)
	{
		addAttackPlayerDesire(npc, playable, 999);
	}
	
	/**
	 * Adds a specific attack desire value for an npc against a target and sets its intention to attack.
	 * @param npc the npc that will perform the action
	 * @param target the playable entity being targeted
	 * @param desire the amount of desire to add to the target
	 */
	protected void addAttackPlayerDesire(Npc npc, Playable target, int desire)
	{
		if (npc.isAttackable())
		{
			npc.asAttackable().addDamageHate(target, 0, desire);
		}
		
		npc.setRunning();
		npc.getAI().setIntentionAttack(target);
	}
	
	/**
	 * Sets an npc to running and orders its AI to attack the target.
	 * @param npc the npc instance to update
	 * @param target the creature the npc should attack
	 */
	protected void addAttackDesire(Npc npc, Creature target)
	{
		npc.setRunning();
		npc.getAI().setIntentionAttack(target);
	}
	
	/**
	 * Orders an npc AI to move toward a location.
	 * @param npc the npc that will perform the move
	 * @param loc the target location for the move
	 * @param desire the priority weight of this movement goal
	 */
	protected void addMoveToDesire(Npc npc, Location loc, int desire)
	{
		npc.getAI().setIntentionMoveTo(loc);
	}
	
	/**
	 * Targets a playable and casts a skill from a skill holder.
	 * @param npc the npc performing the action
	 * @param target the playable entity receiving the skill
	 * @param skill the holder containing the skill data
	 */
	protected void castSkill(Npc npc, Playable target, SkillHolder skill)
	{
		npc.setTarget(target);
		npc.doCast(skill.getSkill());
	}
	
	/**
	 * Targets a playable and casts a skill.
	 * @param npc the npc performing the action
	 * @param target the playable entity being targeted
	 * @param skill the skill to be cast
	 */
	protected void castSkill(Npc npc, Playable target, Skill skill)
	{
		npc.setTarget(target);
		npc.doCast(skill);
	}
	
	/**
	 * Adds a skill-cast desire for an npc using a skill holder.
	 * @param npc the npc that will perform the action
	 * @param target the target of the cast
	 * @param skill the holder containing the skill data
	 * @param desire the priority value for this cast
	 */
	protected void addSkillCastDesire(Npc npc, WorldObject target, SkillHolder skill, int desire)
	{
		addSkillCastDesire(npc, target, skill.getSkill(), desire);
	}
	
	/**
	 * Adds a skill-cast desire for an npc, updating hate and triggering the cast.
	 * @param npc the npc performing the action
	 * @param target the target of the cast
	 * @param skill the skill to be cast
	 * @param desire the amount of hate or desire to add
	 */
	protected void addSkillCastDesire(Npc npc, WorldObject target, Skill skill, int desire)
	{
		if (npc.isAttackable() && (target != null) && target.isCreature())
		{
			npc.asAttackable().addDamageHate(target.asCreature(), 0, desire);
		}
		
		npc.setTarget(target != null ? target : npc);
		npc.doCast(skill);
	}
	
	/**
	 * Sends a special camera effect to a player.
	 * @param player the player who will receive the effect
	 * @param creature the creature the camera focuses on
	 * @param force the force value for the camera movement
	 * @param angle1 the first camera angle
	 * @param angle2 the second camera angle
	 * @param time the time duration for the effect
	 * @param range the distance range for the camera
	 * @param duration the total duration of the effect
	 * @param relYaw the relative yaw value
	 * @param relPitch the relative pitch value
	 * @param isWide whether the camera should be wide
	 * @param relAngle the relative angle for positioning
	 */
	public static void specialCamera(Player player, Creature creature, int force, int angle1, int angle2, int time, int range, int duration, int relYaw, int relPitch, int isWide, int relAngle)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		player.sendPacket(new SpecialCamera(creature, force, angle1, angle2, time, range, duration, relYaw, relPitch, isWide, relAngle));
	}
	
	/**
	 * Sends a special camera effect to a player relative to the player.
	 * @param player the player who will receive the effect
	 * @param creature the creature the camera focuses on
	 * @param force the strength of the camera movement
	 * @param angle1 the first camera angle
	 * @param angle2 the second camera angle
	 * @param time the starting time for the effect
	 * @param duration how long the effect lasts
	 * @param relYaw the relative yaw value
	 * @param relPitch the relative pitch value
	 * @param isWide whether the view should be wide
	 * @param relAngle the relative angle for the effect
	 */
	public static void specialCameraEx(Player player, Creature creature, int force, int angle1, int angle2, int time, int duration, int relYaw, int relPitch, int isWide, int relAngle)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		player.sendPacket(new SpecialCamera(creature, player, force, angle1, angle2, time, duration, relYaw, relPitch, isWide, relAngle));
	}
	
	/**
	 * Sends a special camera effect to a player with an extra packet parameter.
	 * @param player the player who will receive the effect
	 * @param creature the creature the camera focuses on
	 * @param force the force of the camera movement
	 * @param angle1 the first camera angle
	 * @param angle2 the second camera angle
	 * @param time the time duration for the transition
	 * @param range the distance range of the camera
	 * @param duration the total duration of the effect
	 * @param relYaw the relative yaw value
	 * @param relPitch the relative pitch value
	 * @param isWide whether the camera should be wide
	 * @param relAngle the relative angle for the camera
	 * @param unk an unknown parameter used by the packet
	 */
	public static void specialCamera3(Player player, Creature creature, int force, int angle1, int angle2, int time, int range, int duration, int relYaw, int relPitch, int isWide, int relAngle, int unk)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		player.sendPacket(new SpecialCamera(creature, force, angle1, angle2, time, range, duration, relYaw, relPitch, isWide, relAngle, unk));
	}
	
	/**
	 * Broadcasts a special camera effect to all players in an instance.
	 * @param world the instance where the effect occurs
	 * @param creature the creature associated with the effect
	 * @param force the force value for the effect
	 * @param angle1 the first camera angle
	 * @param angle2 the second camera angle
	 * @param time the time duration for the effect
	 * @param range the range of the effect
	 * @param duration the total duration of the movement
	 * @param relYaw the relative yaw value
	 * @param relPitch the relative pitch value
	 * @param isWide whether the view is wide
	 * @param relAngle the relative angle parameter
	 * @param unk an unknown additional packet parameter
	 */
	public static void specialCamera(Instance world, Creature creature, int force, int angle1, int angle2, int time, int range, int duration, int relYaw, int relPitch, int isWide, int relAngle, int unk)
	{
		world.broadcastPacket(new SpecialCamera(creature, force, angle1, angle2, time, range, duration, relYaw, relPitch, isWide, relAngle, unk));
	}
	
	/**
	 * Adds a marker to a player radar.
	 * @param player the player who will receive the marker
	 * @param x the x coordinate of the marker
	 * @param y the y coordinate of the marker
	 * @param z the z coordinate of the marker
	 */
	public static void addRadar(Player player, int x, int y, int z)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		player.getRadar().addMarker(x, y, z);
	}
	
	/**
	 * Removes a marker from a player radar.
	 * @param player the player whose radar will be updated
	 * @param x the x coordinate of the marker to remove
	 * @param y the y coordinate of the marker to remove
	 * @param z the z coordinate of the marker to remove
	 */
	public void removeRadar(Player player, int x, int y, int z)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		player.getRadar().removeMarker(x, y, z);
	}
	
	/**
	 * Removes all markers from a player radar.
	 * @param player the player whose radar should be cleared
	 */
	public void clearRadar(Player player)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		player.getRadar().removeAllMarkers();
	}
	
	/**
	 * Starts playing a movie for a player.
	 * @param player the player who will watch the movie
	 * @param movie the movie to be played
	 */
	public static void playMovie(Player player, Movie movie)
	{
		if (player.isSimulatingTalking())
		{
			return;
		}
		
		new MovieHolder(Arrays.asList(player), movie);
	}
	
	/**
	 * Starts a movie for a list of players.
	 * @param players the players who will watch the movie
	 * @param movie the movie to be played
	 */
	public void playMovie(List<Player> players, Movie movie)
	{
		new MovieHolder(players, movie);
	}
	
	/**
	 * Starts a movie for a set of players.
	 * @param players the players who will watch the movie
	 * @param movie the movie to be played
	 */
	public void playMovie(Set<Player> players, Movie movie)
	{
		new MovieHolder(new ArrayList<>(players), movie);
	}
	
	/**
	 * Plays a movie for all valid players currently in an instance.
	 * @param instance the instance containing the players
	 * @param movie the movie to be played
	 */
	public void playMovie(Instance instance, Movie movie)
	{
		if (instance != null)
		{
			for (Player player : instance.getPlayers())
			{
				if ((player != null) && (player.getInstanceWorld() == instance))
				{
					playMovie(player, movie);
				}
			}
		}
	}
	
	/**
	 * Grants story quest buffs to a player when the reward is enabled in configuration.
	 * @param player the player who will receive the buffs
	 */
	public void giveStoryBuffReward(Player player)
	{
		if (GeneralConfig.ENABLE_STORY_QUEST_BUFF_REWARD)
		{
			for (SkillHolder holder : STORY_QUEST_BUFFS)
			{
				SkillCaster.triggerCast(player, player, holder.getSkill());
			}
		}
	}
	
	/**
	 * Retrieves the data associated with this quest.
	 * @return the new-quest object containing the quest details
	 */
	public NewQuest getQuestData()
	{
		return _questData;
	}
	
	/**
	 * Gives quest rewards to a player, including items, level-up, experience, and skill points.
	 * @param player the player who will receive the rewards
	 */
	public void rewardPlayer(Player player)
	{
		final NewQuestReward reward = _questData.getRewards();
		final List<ItemHolder> rewardItems = reward.getItems();
		if ((rewardItems != null) && !rewardItems.isEmpty())
		{
			for (ItemHolder item : rewardItems)
			{
				rewardItems(player, item);
			}
		}
		
		final int rewardLevel = reward.getLevel();
		if (rewardLevel > 0)
		{
			final long playerExp = player.getExp();
			final long targetExp = ExperienceData.getInstance().getExpForLevel(rewardLevel);
			if (playerExp < targetExp)
			{
				player.addExpAndSp(targetExp - playerExp, 0);
				player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
				player.broadcastUserInfo();
			}
		}
		
		final long rewardExp = reward.getExp();
		if (rewardExp > 0)
		{
			player.getStat().addExp(rewardExp);
			player.broadcastUserInfo();
		}
		
		final long rewardSp = reward.getSp();
		if (rewardSp > 0)
		{
			player.getStat().addSp(rewardSp);
			player.broadcastUserInfo();
		}
	}
	
	/**
	 * Teleports a player to a quest location if no state restrictions apply.
	 * @param player the player to be moved
	 * @param loc the destination for the teleport
	 * @return {@code true} if the teleport succeeded, {@code false} if it failed due to a restriction or null location
	 */
	public boolean teleportToQuestLocation(Player player, ILocational loc)
	{
		if (loc == null)
		{
			return false;
		}
		
		if (player.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_WHILE_DEAD);
			return false;
		}
		
		// Players should not be able to teleport if in a special location.
		if ((player.getMovieHolder() != null) || player.isFishing() || player.isInInstance() || player.isOnEvent() || player.isInOlympiadMode() || player.getObserverMode().isActive() || player.isInTraingCamp() || player.isInTimedHuntingZone())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_RIGHT_NOW);
			return false;
		}
		
		// Teleport in combat configuration.
		if (!PlayerConfig.TELEPORT_WHILE_PLAYER_IN_COMBAT && (player.isInCombat() || player.isCastingNow()))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_WHILE_IN_COMBAT);
			return false;
		}
		
		// Karma related configurations.
		if ((!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT || !PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_GK) && (player.getReputation() < 0))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_RIGHT_NOW);
			return false;
		}
		
		// Cannot escape effect.
		if (player.isAffected(EffectFlag.CANNOT_ESCAPE))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_RIGHT_NOW);
			return false;
		}
		
		player.abortCast();
		player.stopMove(null);
		player.teleToLocation(loc);
		return true;
	}
	
	/**
	 * Sets the type of the quest data.
	 * @param type the new type to assign to the quest data
	 */
	public void setType(int type)
	{
		_questData.setType(type);
	}
	
	/**
	 * Sends a quest acceptance dialog to a player after a 2000 ms delay.
	 * @param player the player who will receive the dialog
	 */
	public void sendAcceptDialog(Player player)
	{
		ThreadPool.schedule(() -> player.sendPacket(new ExQuestDialog(getId(), QuestDialogType.ACCEPT)), 2000);
	}
	
	/**
	 * Sends a quest completion dialog to a player after a 2000 ms delay.
	 * @param player the player who will receive the dialog
	 */
	public void sendEndDialog(Player player)
	{
		ThreadPool.schedule(() -> player.sendPacket(new ExQuestDialog(getId(), QuestDialogType.END)), 2000);
	}
}
