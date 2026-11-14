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
package org.l2jmobius.gameserver.entity.actor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.AttackableAI;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.cache.RelationCache;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.PvpConfig;
import org.l2jmobius.gameserver.config.custom.BossAnnouncementsConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.holders.AccessLevel;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.TransformData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Position;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.entity.actor.holders.creature.Hit;
import org.l2jmobius.gameserver.entity.actor.holders.creature.IgnoreSkillHolder;
import org.l2jmobius.gameserver.entity.actor.holders.creature.TimeStamp;
import org.l2jmobius.gameserver.entity.actor.instance.FriendlyNpc;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.instance.Trap;
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.entity.actor.status.CreatureStatus;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneRegion;
import org.l2jmobius.gameserver.entity.zone.type.WaterZone;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.geoengine.pathfinding.GeoLocation;
import org.l2jmobius.gameserver.geoengine.pathfinding.PathFinding;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttack;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttackAvoid;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttacked;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageDealt;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageReceived;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureKilled;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSee;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillFinishCast;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillUse;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureTeleport;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureTeleported;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableFactionCall;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.DamageReturn;
import org.l2jmobius.gameserver.mechanics.events.returns.LocationReturn;
import org.l2jmobius.gameserver.mechanics.options.OptionSkillHolder;
import org.l2jmobius.gameserver.mechanics.options.OptionSkillType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffFinishTask;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.SkillCastingType;
import org.l2jmobius.gameserver.mechanics.skill.SkillChannelized;
import org.l2jmobius.gameserver.mechanics.skill.SkillChannelizer;
import org.l2jmobius.gameserver.mechanics.skill.enums.BasicProperty;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.BasicPropertyResist;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.MoveType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.StatusUpdateType;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.Attack;
import org.l2jmobius.gameserver.network.serverpackets.ChangeMoveType;
import org.l2jmobius.gameserver.network.serverpackets.ChangeWaitType;
import org.l2jmobius.gameserver.network.serverpackets.ExTeleportToLocationActivate;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerInfo;
import org.l2jmobius.gameserver.network.serverpackets.MoveToLocation;
import org.l2jmobius.gameserver.network.serverpackets.MoveToPawn;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.Revive;
import org.l2jmobius.gameserver.network.serverpackets.ServerObjectInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.TeleportToLocation;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanagers.CreatureAttackTaskManager;
import org.l2jmobius.gameserver.taskmanagers.CreatureSeeTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.taskmanagers.MovementTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class serves as the base class for all character objects in the game world, such as players and NPCs.<br>
 * It handles core logic for entities including {@code Playable}, {@code Npc}, {@code Trap}, and {@code StaticObject}.<br>
 * Each instance maintains a link to a {@code CreatureTemplate} to manage shared static properties efficiently.
 */
public abstract class Creature extends WorldObject
{
	public static final Logger LOGGER = Logger.getLogger(Creature.class.getName());
	
	private final Set<Creature> _attackByList = ConcurrentHashMap.newKeySet(1);
	
	private boolean _isDead = false;
	private boolean _isImmobilized = false;
	private boolean _isOverloaded = false; // The char is carrying too much.
	private boolean _isPendingRevive = false;
	private boolean _isRunning = isPlayer();
	protected boolean _showSummonAnimation = false;
	protected boolean _isTeleporting = false;
	private boolean _isInvul = false;
	private boolean _isUndying = false;
	private boolean _isFlying = false;
	
	private boolean _blockActions = false;
	private final Map<Integer, AtomicInteger> _blockActionsAllowedSkills = new ConcurrentHashMap<>();
	
	private CreatureStat _stat;
	private CreatureStatus _status;
	private CreatureTemplate _template; // The link on the CreatureTemplate object containing generic and static properties of this Creature type (ex : Max HP, Speed...)
	private String _title;
	
	public static final double MAX_HP_BAR_PX = 352.0;
	
	private double _hpUpdateIncCheck = .0;
	private double _hpUpdateDecCheck = .0;
	private double _hpUpdateInterval = .0;
	
	private int _reputation = 0;
	
	/** Map containing all skills of this character. */
	private final ConcurrentSkipListMap<Integer, Skill> _skills = new ConcurrentSkipListMap<>();
	/** Map containing the skill reuse time stamps. */
	private final Map<Long, TimeStamp> _reuseTimeStampsSkills = new ConcurrentHashMap<>();
	/** Map containing the item reuse time stamps. */
	private final Map<Integer, TimeStamp> _reuseTimeStampsItems = new ConcurrentHashMap<>();
	/** Map containing all the disabled skills. */
	private final Map<Long, Long> _disabledSkills = new ConcurrentHashMap<>();
	private boolean _allSkillsDisabled;
	
	private final byte[] _zones = new byte[ZoneId.getZoneCount()];
	protected final Location _lastZoneValidateLocation = new Location(super.getX(), super.getY(), super.getZ());
	
	private final StampedLock _attackLock = new StampedLock();
	
	private Team _team = Team.NONE;
	
	private boolean _lethalable = true;
	
	private final Map<Integer, OptionSkillHolder> _triggerSkills = new ConcurrentHashMap<>(1);
	
	private final Map<Integer, IgnoreSkillHolder> _ignoreSkillEffects = new ConcurrentHashMap<>(1);
	/** Creatures effect list. */
	private final EffectList _effectList = new EffectList(this);
	/** The creature that summons this character. */
	private Creature _summoner = null;
	
	/** Map of summoned NPCs by this creature. */
	private Map<Integer, Npc> _summonedNpcs = null;
	
	private Creature _recallCreature = null;
	
	private SkillChannelizer _channelizer = null;
	
	private SkillChannelized _channelized = null;
	
	private final BuffFinishTask _buffFinishTask = new BuffFinishTask();
	
	private Transform _transform = null;
	
	/** Movement data of this Creature */
	protected MoveData _move;
	private boolean _cursorKeyMovement = false;
	/** Distance a path end point may sit from its target and still count as having reached it, two geo cells. */
	private static final int PATH_ENDPOINT_TOLERANCE = 32;
	
	/** This creature's target. */
	private WorldObject _target;
	
	// Set by the start of attack, in game ticks.
	private volatile long _attackEndTime;
	private volatile long _disableRangedAttackEndTime;
	
	private volatile CreatureAI _ai = null;
	
	/** Future Skill Cast */
	protected Map<SkillCastingType, SkillCaster> _skillCasters = new ConcurrentHashMap<>();
	
	private final AtomicInteger _abnormalShieldBlocks = new AtomicInteger();
	private final Map<Integer, Double> _multipliedAbnormalTimes = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _addedAbnormalTimes = new ConcurrentHashMap<>();
	
	private final Map<Integer, RelationCache> _knownRelations = new ConcurrentHashMap<>();
	
	private Set<Creature> _seenCreatures = null;
	private int _seenCreatureRange = PlayerConfig.ALT_PARTY_RANGE;
	
	private final Map<StatusUpdateType, Long> _statusUpdates = new ConcurrentHashMap<>();
	
	/** A map holding info about basic property mesmerizing system. */
	private final Map<BasicProperty, BasicPropertyResist> _basicPropertyResists = new ConcurrentHashMap<>(1);
	
	/** A set containing the shot types currently charged. */
	private Set<ShotType> _chargedShots = EnumSet.noneOf(ShotType.class);
	
	/** A list containing the dropped items of this fake player. */
	private final List<Item> _fakePlayerDrops = new CopyOnWriteArrayList<>();
	
	private OnCreatureAttack _onCreatureAttack = null;
	private OnCreatureAttacked _onCreatureAttacked = null;
	private OnCreatureAttackAvoid _onCreatureAttackAvoid = null;
	public OnCreatureSkillFinishCast onCreatureSkillFinishCast = null;
	public OnCreatureSkillUse onCreatureSkillUse = null;
	
	/**
	 * Creates a new {@code Creature} instance using the provided template.<br>
	 * This constructor automatically assigns a unique ID from the {@code IdManager}.
	 * @param template The {@code CreatureTemplate} used to initialize the creature.
	 */
	public Creature(CreatureTemplate template)
	{
		this(IdManager.getInstance().getNextId(), template);
	}
	
	/**
	 * Creates a new {@code Creature} instance using a specific object ID and template.<br>
	 * This constructor initializes the creature's stats, status, and skills based on the provided {@code CreatureTemplate}.<br>
	 * It also sets the instance type to {@code InstanceType.Creature} and makes the entity invulnerable by default.
	 * @param objectId The unique identifier for this creature.
	 * @param template The {@code CreatureTemplate} used to define the creature's properties.
	 */
	public Creature(int objectId, CreatureTemplate template)
	{
		super(objectId);
		if (template == null)
		{
			throw new NullPointerException("Template is null!");
		}
		
		setInstanceType(InstanceType.Creature);
		
		// Set its template to the new Creature.
		_template = template;
		initCharStat();
		initCharStatus();
		
		if (isNpc())
		{
			// Copy the skills of the Npc from its template to the Creature Instance.
			// The skills list can be affected by spell effects so it's necessary to make a copy.
			// to avoid that a spell affecting a Npc, affects others Npc of the same type too.
			for (Skill skill : template.getSkills().values())
			{
				addSkill(skill);
			}
		}
		else if (isSummon())
		{
			// Copy the skills of the Summon from its template to the Creature Instance.
			// The skills list can be affected by spell effects so it's necessary to make a copy.
			// to avoid that a spell affecting a Summon, affects others Summon of the same type too.
			for (Skill skill : template.getSkills().values())
			{
				addSkill(skill);
			}
		}
		
		setInvul(true);
	}
	
	/**
	 * Retrieves the list of active effects for this creature.
	 * @return the {@code EffectList} containing all current effects.
	 */
	public EffectList getEffectList()
	{
		return _effectList;
	}
	
	/**
	 * Retrieves the inventory associated with this entity.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Inventory} object or {@code null}.
	 */
	public Inventory getInventory()
	{
		return null;
	}
	
	/**
	 * Removes a specific quantity of an item from the inventory based on its ID.<br>
	 * This method handles the destruction process and optional messaging.
	 * @param process The type of process used to destroy the item.
	 * @param itemId The unique identifier of the item to remove.
	 * @param count The amount of the item to destroy.
	 * @param reference The {@code WorldObject} associated with this action.
	 * @param sendMessage Whether to send a confirmation message to the player.
	 * @return {@code true} if the destruction was successful, {@code false} otherwise.
	 */
	public boolean destroyItemByItemId(ItemProcessType process, int itemId, long count, WorldObject reference, boolean sendMessage)
	{
		// Default: NPCs consume virtual items for their skills.
		// TODO: should be logged if even happens.. should be false
		return true;
	}
	
	/**
	 * Removes a specific quantity of an item from the inventory.<br>
	 * This method handles the destruction logic based on the provided process type.
	 * @param process The type of action being performed to destroy the item.
	 * @param objectId The unique identifier of the item to be removed.
	 * @param count The amount of the item to remove.
	 * @param reference The {@code WorldObject} associated with this action.
	 * @param sendMessage Whether to notify the player about the destruction.
	 * @return {@code true} if the operation was successful, {@code false} otherwise.
	 */
	public boolean destroyItem(ItemProcessType process, int objectId, long count, WorldObject reference, boolean sendMessage)
	{
		// Default: NPCs consume virtual items for their skills.
		// TODO: should be logged if even happens.. should be false
		return true;
	}
	
	/**
	 * Checks if the current entity is located within a specific zone.<br>
	 * This method evaluates the zone type against the current world instance and internal zone data.
	 * @param zone The {@code ZoneId} to check against.
	 * @return {@code true} if the entity is inside the specified zone, {@code false} otherwise.
	 */
	@Override
	public boolean isInsideZone(ZoneId zone)
	{
		final Instance instance = getInstanceWorld();
		switch (zone)
		{
			case PVP:
			{
				if ((instance != null) && instance.isPvP())
				{
					return true;
				}
				
				return (_zones[ZoneId.PVP.ordinal()] > 0) && (_zones[ZoneId.PEACE.ordinal()] == 0) && (_zones[ZoneId.NO_PVP.ordinal()] == 0);
			}
			case PEACE:
			{
				if ((instance != null) && instance.isPvP())
				{
					return false;
				}
				break;
			}
		}
		
		return _zones[zone.ordinal()] > 0;
	}
	
	/**
	 * Updates the status of whether this entity is inside a specific zone.<br>
	 * This method increments or decrements the counter for the given {@code ZoneId}.
	 * @param zone The {@code ZoneId} to update.
	 * @param state The new state to set. Use {@code true} to enter and {@code false} to exit.
	 */
	public void setInsideZone(ZoneId zone, boolean state)
	{
		synchronized (_zones)
		{
			if (state)
			{
				_zones[zone.ordinal()]++;
			}
			else if (_zones[zone.ordinal()] > 0)
			{
				_zones[zone.ordinal()]--;
			}
		}
	}
	
	/**
	 * Checks if the creature is currently in a transformed state.<br>
	 * It returns {@code true} if a transformation exists.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if transformed, {@code false} otherwise.
	 */
	public boolean isTransformed()
	{
		return _transform != null;
	}
	
	/**
	 * Changes the current state of the entity to a specific transformation.<br>
	 * This method checks for valid conditions before applying the change.<br>
	 * It handles restrictions like siege rules and world boundaries.
	 * @param id The unique identifier of the {@code Transform} to apply.
	 * @param addSkills Whether to include skills during the transformation.
	 * @return {@code true} if the transformation was successful, {@code false} otherwise.
	 */
	public boolean transform(int id, boolean addSkills)
	{
		final Transform transform = TransformData.getInstance().getTransform(id);
		if (transform != null)
		{
			if (transform.isFlying() && (getX() > World.GRACIA_MAX_X))
			{
				return false;
			}
			
			if (!FeatureConfig.ALLOW_MOUNTS_DURING_SIEGE && transform.isRiding() && isInsideZone(ZoneId.SIEGE))
			{
				return false;
			}
			
			_transform = transform;
			transform.onTransform(this, addSkills);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Reverts the current transformation of the creature.<br>
	 * This method clears the active {@code Transform} object.<br>
	 * If the entity is a player, it recalculates their stats and updates their user information.
	 */
	public void untransform()
	{
		if (_transform != null)
		{
			_transform.onUntransform(this);
			_transform = null;
		}
		
		// Mobius: Tempfix for untransform not showing stats.
		// Resend UserInfo to player.
		if (isPlayer())
		{
			getStat().recalculateStats(true);
			asPlayer().updateUserInfo();
		}
	}
	
	/**
	 * Retrieves the current transformation of this creature.<br>
	 * This method returns the {@code Transform} object associated with the entity.
	 * @return The current {@code Transform} instance.
	 */
	public Transform getTransformation()
	{
		return _transform;
	}
	
	/**
	 * Retrieves the unique identifier for the current transformation.<br>
	 * Returns {@code 0} if no transformation is active.
	 * @return The {@code int} ID of the transformation.
	 */
	public int getTransformationId()
	{
		if (_transform == null)
		{
			return 0;
		}
		
		return _transform.getId();
	}
	
	/**
	 * Retrieves the display identifier for the current transformation.<br>
	 * This method returns {@code 0} if there is no active visual transformation.
	 * @return The unique display ID of the transformation or {@code 0}.
	 */
	public int getTransformationDisplayId()
	{
		if ((_transform == null) || !_transform.isVisual())
		{
			return 0;
		}
		
		return _transform.getDisplayId();
	}
	
	/**
	 * Gets the collision radius of the entity.<br>
	 * This value is used to determine the physical size for hit detection.<br>
	 * It returns the transformed radius if a transformation is active.<br>
	 * Otherwise, it returns the default radius from the template.
	 * @return The current collision radius as a {@code float}.
	 */
	public float getCollisionRadius()
	{
		if (_transform == null)
		{
			return _template.getCollisionRadius();
		}
		
		return _transform.getCollisionRadius(this, _template.getCollisionRadius());
	}
	
	/**
	 * Retrieves the collision height of the entity.<br>
	 * It uses the transformation height if one exists.<br>
	 * Otherwise, it returns the default template height.
	 * @return The current collision height as a {@code float}.
	 */
	public float getCollisionHeight()
	{
		if (_transform == null)
		{
			return _template.getCollisionHeight();
		}
		
		return _transform.getCollisionHeight(this, _template.getCollisionHeight());
	}
	
	/**
	 * Checks if the entity has Game Master privileges.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the entity is a Game Master, otherwise {@code false}.
	 */
	public boolean isGM()
	{
		return false;
	}
	
	/**
	 * Retrieves the current access level of this entity.<br>
	 * This method returns {@code null} for all entities.
	 * @return the {@code AccessLevel} of the entity or {@code null}.
	 */
	public AccessLevel getAccessLevel()
	{
		return null;
	}
	
	/**
	 * Initializes the internal values used for updating character status bars.<br>
	 * This method calculates intervals based on the maximum health of the creature.
	 */
	protected void initCharStatusUpdateValues()
	{
		_hpUpdateIncCheck = _stat.getMaxHp();
		_hpUpdateInterval = _hpUpdateIncCheck / MAX_HP_BAR_PX;
		_hpUpdateDecCheck = _hpUpdateIncCheck - _hpUpdateInterval;
	}
	
	/**
	 * Handles the logic for when a creature decays or respawns.<br>
	 * It checks if the player should be disconnected after death based on {@code PlayerConfig}.<br>
	 * If not disconnected, it calls {@code decayMe()} and resets the creature's state.<br>
	 * This includes removing the entity from zones and re-enabling its AI.
	 */
	public void onDecay()
	{
		if (isPlayer())
		{
			final Player player = asPlayer();
			if (player.isInTimedHuntingZone())
			{
				player.stopTimedHuntingZoneTask();
				abortCast();
				stopMove(null);
				teleToLocation(MapRegionData.getInstance().getTeleToLocation(this, TeleportWhereType.TOWN));
				setInstance(null);
			}
			else if (PlayerConfig.DISCONNECT_AFTER_DEATH && player.isOnline())
			{
				Disconnection.of(player).storeAndDeleteWith(new SystemMessage(SystemMessageId.SIXTY_MIN_HAVE_PASSED_AFTER_THE_DEATH_OF_YOUR_CHARACTER_SO_YOU_WERE_DISCONNECTED_FROM_THE_GAME));
			}
		}
		else
		{
			decayMe();
			final ZoneRegion region = ZoneManager.getInstance().getRegion(this);
			if (region != null)
			{
				region.removeFromZones(this);
			}
			
			// Removes itself from the summoned list.
			if (_summoner != null)
			{
				_summoner.removeSummonedNpc(getObjectId());
			}
			
			// Enable AI.
			_disabledAI = false;
			
			_onCreatureAttack = null;
			_onCreatureAttacked = null;
			_onCreatureAttackAvoid = null;
			onCreatureSkillFinishCast = null;
			onCreatureSkillUse = null;
		}
	}
	
	/**
	 * This method is called when the entity is first created in the world.<br>
	 * It starts the buff finish task and revalidates the current zone.<br>
	 * It also handles special spawn announcements for bosses and raid monsters.
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		_buffFinishTask.start();
		
		revalidateZone(true);
		
		// Custom boss announcements configuration.
		if (this instanceof GrandBoss)
		{
			if (BossAnnouncementsConfig.GRANDBOSS_SPAWN_ANNOUNCEMENTS && (!isInInstance() || BossAnnouncementsConfig.GRANDBOSS_INSTANCE_ANNOUNCEMENTS) && !isMinion() && !isRaidMinion())
			{
				final int id = getId();
				final String name = NpcData.getInstance().getTemplate(id).getName();
				if ((name != null) && !BossAnnouncementsConfig.RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS.contains(id))
				{
					World.broadcastToAllOnlinePlayers(name + " has spawned!");
					World.broadcastToAllOnlinePlayersOnScreen(name + " has spawned!");
				}
			}
		}
		else if (isRaid() && BossAnnouncementsConfig.RAIDBOSS_SPAWN_ANNOUNCEMENTS && (!isInInstance() || BossAnnouncementsConfig.RAIDBOSS_INSTANCE_ANNOUNCEMENTS) && !isMinion() && !isRaidMinion())
		{
			final int id = getId();
			final String name = NpcData.getInstance().getTemplate(id).getName();
			if ((name != null) && !BossAnnouncementsConfig.RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS.contains(id))
			{
				World.broadcastToAllOnlinePlayers(name + " has spawned!");
				World.broadcastToAllOnlinePlayersOnScreen(name + " has spawned!");
			}
		}
	}
	
	/**
	 * This method is called when a creature finishes teleporting.<br>
	 * It spawns the creature at its current coordinates.<br>
	 * It updates the {@code _isTeleporting} state to {@code false}.<br>
	 * It notifies any listeners for the {@code ON_CREATURE_TELEPORTED} event.
	 */
	public synchronized void onTeleported()
	{
		if (!_isTeleporting)
		{
			return;
		}
		
		spawnMe(getX(), getY(), getZ());
		setTeleporting(false);
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_TELEPORTED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnCreatureTeleported(this), this);
		}
	}
	
	/**
	 * Adds a {@code Creature} to the list of entities currently being attacked.<br>
	 * This method updates the internal tracking of active targets.
	 * @param creature The {@code Creature} to add to the attack list.
	 */
	public void addAttackerToAttackByList(Creature creature)
	{
		// DS: moved to Attackable.
	}
	
	/**
	 * Sends a packet to all relevant clients.<br>
	 * This method handles the distribution of data to multiple players.
	 * @param packet The {@code ServerPacket} object to be sent.
	 */
	public void broadcastPacket(ServerPacket packet)
	{
		broadcastPacket(packet, true);
	}
	
	/**
	 * Sends a packet to all players in the world.<br>
	 * This method handles both global broadcasting and visibility checks.
	 * @param packet The {@code ServerPacket} to be sent.
	 * @param includeSelf Determines if the sender should also receive the packet.
	 */
	public void broadcastPacket(ServerPacket packet, boolean includeSelf)
	{
		packet.sendInBroadcast();
		
		World.forEachVisibleObject(this, Player.class, player ->
		{
			if (isVisibleFor(player))
			{
				player.sendPacket(packet);
			}
		});
	}
	
	// TODO: Add target logic.
	/**
	 * Sends a skill packet to all nearby players.<br>
	 * This method handles the broadcasting of a {@code ServerPacket} to observers.<br>
	 * It uses the {@code target} object to determine the location for the broadcast.
	 * @param packet The {@code ServerPacket} containing the skill data to send.
	 * @param target The {@code WorldObject} used as the origin point for the broadcast.
	 */
	public void broadcastSkillPacket(ServerPacket packet, WorldObject target)
	{
		broadcastPacket(packet);
	}
	
	// TODO: Add targets logic.
	/**
	 * Sends a skill packet to all specified targets.<br>
	 * This method handles the distribution of {@code ServerPacket} data.
	 * @param packet The {@code ServerPacket} to be sent.
	 * @param targets A {@code Collection} of {@code WorldObject} entities to receive the packet.
	 */
	public void broadcastSkillPacket(ServerPacket packet, Collection<WorldObject> targets)
	{
		broadcastPacket(packet);
	}
	
	/**
	 * Sends a notification to all nearby players about the movement.<br>
	 * This method updates the visual position of the entity for others.
	 */
	public void broadcastMoveToLocation()
	{
		broadcastMoveToLocation(false);
	}
	
	/**
	 * Sends a movement packet to nearby clients.<br>
	 * This method handles different logic for playable characters and non-playable creatures.<br>
	 * It updates the last broadcast time to prevent spamming packets too frequently.
	 * @param force If {@code true}, the move is broadcasted immediately regardless of the time elapsed.
	 */
	public void broadcastMoveToLocation(boolean force)
	{
		final MoveData move = _move;
		if (move == null)
		{
			return;
		}
		
		// Broadcast MoveToLocation when forced or once per second.
		final int gameTicks = GameTimeTaskManager.getInstance().getGameTicks();
		if (!force && !_cursorKeyMovement && (move.moveTimestamp > 0) && ((gameTicks - move.lastBroadcastTime) < 10))
		{
			return;
		}
		
		move.lastBroadcastTime = gameTicks;
		
		if (isPlayable())
		{
			broadcastPacket(new MoveToLocation(this));
		}
		else
		{
			final CreatureAI ai = hasAI() ? getAI() : null;
			
			// The client animates the run towards this pawn, so it has to be what the actor is really walking to.
			// The selected target is not, it stays on whoever the npc cast at last.
			WorldObject target = null;
			if (ai != null)
			{
				final Intention intention = ai.getIntention();
				if ((intention == Intention.ATTACK) || (intention == Intention.FOLLOW))
				{
					final WorldObject pawn = ai.getTarget();
					target = (pawn != null) ? pawn : _target;
				}
			}
			
			if (target != null)
			{
				if ((target != this) && !isOnGeodataPath(move))
				{
					broadcastPacket(new MoveToPawn(this, target, getAI().getClientMovingToPawnOffset()));
				}
				else
				{
					broadcastPacket(new MoveToLocation(this));
				}
			}
			else
			{
				final WorldRegion region = getWorldRegion();
				if (((region != null) && region.areNeighborsActive()))
				{
					broadcastPacket(new MoveToLocation(this));
				}
			}
		}
	}
	
	/**
	 * Sends a social action packet to nearby players.<br>
	 * This method checks if the entity is playable before broadcasting.<br>
	 * If not playable, it broadcasts to neighbors if the region is active.
	 * @param id The unique identifier for the social action.
	 */
	public void broadcastSocialAction(int id)
	{
		if (isPlayable())
		{
			broadcastPacket(new SocialAction(getObjectId(), id));
		}
		else
		{
			final WorldRegion region = getWorldRegion();
			if ((region != null) && region.areNeighborsActive())
			{
				broadcastPacket(new SocialAction(getObjectId(), id));
			}
		}
	}
	
	/**
	 * Checks if the current health requires a visual update.<br>
	 * This method determines if the HP change is significant enough to trigger an update.<br>
	 * It also handles special cases for very low or maximum health values.
	 * @return {@code true} if the health needs to be updated, {@code false} otherwise.
	 */
	protected boolean needHpUpdate()
	{
		final double currentHp = _status.getCurrentHp();
		final double maxHp = _stat.getMaxHp();
		if ((currentHp <= 1.0) || (maxHp < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentHp <= _hpUpdateDecCheck) || (currentHp >= _hpUpdateIncCheck))
		{
			if (currentHp == maxHp)
			{
				_hpUpdateIncCheck = currentHp + 1;
				_hpUpdateDecCheck = currentHp - _hpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentHp / _hpUpdateInterval;
				int intMulti = (int) doubleMulti;
				_hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti - 1 : intMulti);
				_hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Sends a status update to all nearby players.<br>
	 * This method notifies others about the current state of the entity.<br>
	 * It is used to synchronize visual information across the network.
	 */
	public void broadcastStatusUpdate()
	{
		broadcastStatusUpdate(null);
	}
	
	/**
	 * Sends a status update packet to relevant observers.<br>
	 * This method updates values like HP and MP for the current creature.<br>
	 * If a {@code caster} is provided, it broadcasts the update to everyone.<br>
	 * Otherwise, it sends the update only to players targeting this object.
	 * @param caster The creature that performed the action, or {@code null}.
	 */
	public void broadcastStatusUpdate(Creature caster)
	{
		final StatusUpdate su = new StatusUpdate(this);
		
		// HP
		su.addUpdate(StatusUpdateType.MAX_HP, _stat.getMaxHp());
		su.addUpdate(StatusUpdateType.CUR_HP, (long) _status.getCurrentHp());
		
		// MP
		computeStatusUpdate(su, StatusUpdateType.MAX_MP);
		computeStatusUpdate(su, StatusUpdateType.CUR_MP);
		
		// Skill was involved.
		if (caster != null)
		{
			su.addCaster(caster);
			broadcastPacket(su);
			return;
		}
		
		// HP-MP regeneration.
		su.sendInBroadcast();
		World.forEachVisibleObject(this, Player.class, player ->
		{
			if (isVisibleFor(player) && (player.getTarget() == this))
			{
				player.sendPacket(su);
			}
		});
	}
	
	/**
	 * Sends a text message to the player.<br>
	 * This method handles the delivery of a {@code String} message.
	 * @param text The content of the message to be sent.
	 */
	public void sendMessage(String text)
	{
		// default implementation
	}
	
	/**
	 * Teleports this creature to a specific location.<br>
	 * This method handles all necessary logic such as canceling actions, updating coordinates, and notifying listeners.
	 * @param xValue The target X coordinate.
	 * @param yValue The target Y coordinate.
	 * @param zValue The target Z coordinate.
	 * @param headingValue The target heading value.
	 * @param instanceValue The target {@code Instance} to move the creature into.
	 */
	public void teleToLocation(int xValue, int yValue, int zValue, int headingValue, Instance instanceValue)
	{
		// Prevent teleporting for players that disconnected unexpectedly.
		if (isPlayer() && !asPlayer().isOnline())
		{
			return;
		}
		
		int x = xValue;
		int y = yValue;
		int z = _isFlying ? zValue : GeoEngine.getInstance().getHeight(x, y, zValue);
		int heading = headingValue;
		Instance instance = instanceValue;
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_TELEPORT, this))
		{
			final LocationReturn term = EventDispatcher.getInstance().notifyEvent(new OnCreatureTeleport(this, x, y, z, heading, instance), this, LocationReturn.class);
			if (term != null)
			{
				if (term.terminate())
				{
					return;
				}
				else if (term.overrideLocation())
				{
					x = term.getX();
					y = term.getY();
					z = term.getZ();
					heading = term.getHeading();
					instance = term.getInstance();
				}
			}
		}
		
		// Prepare creature for teleport.
		if (_isPendingRevive)
		{
			doRevive();
		}
		
		// Abort any client actions, casting and remove target.
		sendPacket(ActionFailed.get(SkillCastingType.NORMAL));
		sendPacket(ActionFailed.get(SkillCastingType.NORMAL_SECOND));
		if (isMoving())
		{
			stopMove(null);
		}
		
		abortCast();
		setTarget(null);
		
		setTeleporting(true);
		
		getAI().setIntentionActive();
		
		// Remove the object from its old location.
		decayMe();
		
		// Adjust position a bit.
		z += 5;
		
		// Send teleport packet where needed.
		broadcastPacket(new TeleportToLocation(this, x, y, z, heading));
		
		// Change instance world.
		if (getInstanceWorld() != instance)
		{
			setInstance(instance);
		}
		
		// Set the x,y,z position of the WorldObject and if necessary modify its _worldRegion.
		setXYZ(x, y, z);
		
		// Also adjust heading.
		if (heading != 0)
		{
			setHeading(heading);
		}
		
		// Send teleport finished packet to player.
		sendPacket(new ExTeleportToLocationActivate(this));
		
		// Allow recall of the detached characters.
		if (isPlayer())
		{
			final Player player = asPlayer();
			final GameClient client = player.getClient();
			if ((client != null) && client.isDetached())
			{
				onTeleported();
			}
		}
		else
		{
			onTeleported();
		}
		
		revalidateZone(true);
	}
	
	/**
	 * Moves the entity to a specific set of coordinates.<br>
	 * This method updates the position of the actor in the world.
	 * @param x The target X coordinate.
	 * @param y The target Y coordinate.
	 * @param z The target Z coordinate.
	 */
	public void teleToLocation(int x, int y, int z)
	{
		teleToLocation(x, y, z, 0, getInstanceWorld());
	}
	
	/**
	 * Moves an entity to a specific set of coordinates.<br>
	 * This method updates the position of the provided {@code Instance}.
	 * @param x The target X coordinate.
	 * @param y The target Y coordinate.
	 * @param z The target Z coordinate.
	 * @param instance The {@code Instance} to move.
	 */
	public void teleToLocation(int x, int y, int z, Instance instance)
	{
		teleToLocation(x, y, z, 0, instance);
	}
	
	/**
	 * Moves the entity to a specific set of coordinates.<br>
	 * This method updates the position and rotation of the actor.
	 * @param x The target X coordinate.
	 * @param y The target Y coordinate.
	 * @param z The target Z coordinate.
	 * @param heading The target rotation angle.
	 */
	public void teleToLocation(int x, int y, int z, int heading)
	{
		teleToLocation(x, y, z, heading, getInstanceWorld());
	}
	
	/**
	 * Moves the entity to a specific set of coordinates.<br>
	 * The method updates the position and rotation of the actor.<br>
	 * It can optionally apply a random offset to the final location.
	 * @param x The target X coordinate.
	 * @param y The target Y coordinate.
	 * @param z The target Z coordinate.
	 * @param heading The direction the entity should face.
	 * @param randomOffset If {@code true}, a random offset is applied to the coordinates.
	 */
	public void teleToLocation(int x, int y, int z, int heading, boolean randomOffset)
	{
		teleToLocation(x, y, z, heading, (randomOffset) ? PlayerConfig.MAX_OFFSET_ON_TELEPORT : 0, getInstanceWorld());
	}
	
	/**
	 * Teleports an entity to a specific set of coordinates.<br>
	 * This method updates the position and rotation of the target.
	 * @param x The destination X coordinate.
	 * @param y The destination Y coordinate.
	 * @param z The destination Z coordinate.
	 * @param heading The direction the entity should face.
	 * @param randomOffset Whether to apply a random position offset.
	 * @param instance The {@code Instance} where the teleport occurs.
	 */
	public void teleToLocation(int x, int y, int z, int heading, boolean randomOffset, Instance instance)
	{
		teleToLocation(x, y, z, heading, (randomOffset) ? PlayerConfig.MAX_OFFSET_ON_TELEPORT : 0, instance);
	}
	
	/**
	 * Moves the entity to a specific set of coordinates.<br>
	 * This method updates the position and rotation of the character.
	 * @param x The target X coordinate.
	 * @param y The target Y coordinate.
	 * @param z The target Z coordinate.
	 * @param heading The direction the entity should face.
	 * @param randomOffset A value to add a small random variation to the position.
	 */
	public void teleToLocation(int x, int y, int z, int heading, int randomOffset)
	{
		teleToLocation(x, y, z, heading, randomOffset, getInstanceWorld());
	}
	
	/**
	 * Teleports an entity to a specific location.<br>
	 * This method applies a random offset if enabled or requested.<br>
	 * It ensures the final destination is valid and reachable.
	 * @param xValue The target X coordinate.
	 * @param yValue The target Y coordinate.
	 * @param zValue The target Z coordinate.
	 * @param heading The direction to face after teleporting.
	 * @param randomOffset The maximum range for the random position offset.
	 * @param instance The entity being moved.
	 */
	public void teleToLocation(int xValue, int yValue, int zValue, int heading, int randomOffset, Instance instance)
	{
		int x = xValue;
		int y = yValue;
		int z = zValue;
		
		if (PlayerConfig.OFFSET_ON_TELEPORT_ENABLED || (randomOffset > 0))
		{
			x = xValue + Rnd.get(-randomOffset, randomOffset);
			y = yValue + Rnd.get(-randomOffset, randomOffset);
			
			int count = 0;
			final float collision = Math.min(getCollisionRadius() * 2, randomOffset);
			while ((count++ < 100) && ((LocationUtil.calculateDistance(xValue, yValue, zValue, x, y, z, true, false) < collision) || !GeoEngine.getInstance().canSeeTarget(xValue, yValue, zValue, x, y, z, instance) || !GeoEngine.getInstance().canMoveToTarget(xValue, yValue, zValue, x, y, z, instance)))
			{
				x = xValue + Rnd.get(-randomOffset, randomOffset);
				y = yValue + Rnd.get(-randomOffset, randomOffset);
			}
			
			if (!_isFlying)
			{
				final Location validLocation = GeoEngine.getInstance().getValidLocation(xValue, yValue, z, x, y, z, instance);
				x = validLocation.getX();
				y = validLocation.getY();
				z = validLocation.getZ();
			}
		}
		
		teleToLocation(x, y, z, heading, instance);
	}
	
	/**
	 * Teleports the entity to a specific location.<br>
	 * This method uses the coordinates and heading from the provided {@code ILocational} object.
	 * @param loc The destination location data.
	 */
	public void teleToLocation(ILocational loc)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading());
	}
	
	/**
	 * Moves an entity to a specific location.<br>
	 * This method updates the coordinates and heading of the {@code Instance}.
	 * @param loc The destination coordinates provided by {@code ILocational}.
	 * @param instance The {@code Instance} to be moved.
	 */
	public void teleToLocation(ILocational loc, Instance instance)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), instance);
	}
	
	/**
	 * Teleports the entity to a specific location.<br>
	 * It uses the coordinates and heading from the provided {@code ILocational} object.<br>
	 * A random offset is applied during the teleportation process.
	 * @param loc The destination location data.
	 * @param randomOffset The amount of randomness to apply to the final position.
	 */
	public void teleToLocation(ILocational loc, int randomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset);
	}
	
	/**
	 * Moves an entity to a specific location.<br>
	 * The method applies a random offset to the target coordinates.
	 * @param loc The destination {@code ILocational} object.
	 * @param randomOffset The amount of randomness to apply to the position.
	 * @param instance The {@code Instance} where the teleport occurs.
	 */
	public void teleToLocation(ILocational loc, int randomOffset, Instance instance)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset, instance);
	}
	
	/**
	 * Teleports the entity to a specific location.<br>
	 * It uses the coordinates and heading from the provided {@code ILocational} object.<br>
	 * If {@code randomOffset} is {@code true}, it applies a random offset based on the configuration.
	 * @param loc The destination location data.
	 * @param randomOffset Whether to apply a random position offset during teleportation.
	 */
	public void teleToLocation(ILocational loc, boolean randomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), (randomOffset) ? PlayerConfig.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	/**
	 * Moves an entity to a specific location.<br>
	 * This method updates the coordinates and heading of the target.
	 * @param loc The destination {@code ILocational} object.
	 * @param randomOffset Whether to apply a random offset to the final position.
	 * @param instance The {@code Instance} where the teleport occurs.
	 */
	public void teleToLocation(ILocational loc, boolean randomOffset, Instance instance)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset, instance);
	}
	
	/**
	 * Teleports the entity to a specific location.<br>
	 * This method handles the movement logic based on the provided type.
	 * @param teleportWhere The destination type for the teleport.
	 */
	public void teleToLocation(TeleportWhereType teleportWhere)
	{
		teleToLocation(teleportWhere, getInstanceWorld());
	}
	
	/**
	 * Teleports the actor to a specific location based on the provided type.<br>
	 * This method handles the movement logic for the entity within an {@code Instance}.
	 * @param teleportWhere The type of teleportation to be performed.
	 * @param instance The {@code Instance} where the teleportation occurs.
	 */
	public void teleToLocation(TeleportWhereType teleportWhere, Instance instance)
	{
		teleToLocation(MapRegionData.getInstance().getTeleToLocation(this, teleportWhere), true, instance);
	}
	
	/**
	 * Executes an automatic attack against a specific target.<br>
	 * This method validates the attack conditions including distance, line of sight, and weapon requirements.<br>
	 * It handles different weapon types such as bows, crossbows, and melee weapons.<br>
	 * If the attack is successful, it updates the character's heading and manages attack timers.
	 * @param target The {@code Creature} to be attacked.
	 */
	public void doAutoAttack(Creature target)
	{
		final long stamp = _attackLock.tryWriteLock();
		if (stamp == 0)
		{
			return;
		}
		
		try
		{
			if ((target == null) || (!isPlayable() && isAttackDisabled()) || !target.isTargetable())
			{
				return;
			}
			
			if (!isAlikeDead())
			{
				if (((isNpc() && target.isAlikeDead() && !(target.isPlayer() && target.asPlayer().isFakeDeathSeenBy(this))) || !isInSurroundingRegion(target)) || (isPlayer() && target.isDead()))
				{
					getAI().setIntentionActive();
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if ((_transform != null) && !_transform.canAttack())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			
			final Player player = asPlayer();
			if (player != null)
			{
				if (player.getObserverMode().isActive())
				{
					sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_FUNCTION_IN_THE_SPECTATOR_MODE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				else if (player.isSiegeFriend(target))
				{
					sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				// Checking if target has moved to peace zone.
				else if (target.isInsidePeaceZone(this))
				{
					getAI().setIntentionActive();
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				// Events.
				else if (player.isOnEvent() && !player.isOnSoloEvent() && target.isPlayable() && (player.getTeam() == target.asPlayer().getTeam()))
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (isInsidePeaceZone(this, target))
			{
				getAI().setIntentionActive();
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			stopEffectsOnAction();
			
			// GeoData Los Check here (or dz > 1000).
			if (!GeoEngine.getInstance().canSeeTarget(this, target))
			{
				sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
				getAI().setIntentionActive();
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Get the active weapon item corresponding to the active weapon instance (always equipped in the right hand).
			final Weapon weaponItem = getActiveWeaponItem();
			final WeaponType weaponType = getAttackType();
			
			// BOW and CROSSBOW checks.
			if (weaponItem != null)
			{
				if (!weaponItem.isAttackWeapon() && !isGM())
				{
					if (weaponItem.getItemType() == WeaponType.FISHINGROD)
					{
						sendPacket(SystemMessageId.YOU_CANNOT_ATTACK_WHILE_FISHING);
					}
					else
					{
						sendPacket(SystemMessageId.YOU_CANNOT_ATTACK_WITH_THIS_WEAPON);
					}
					
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				// Ranged weapon checks.
				if (weaponItem.getItemType().isRanged())
				{
					// Check if bow delay is still active.
					if (_disableRangedAttackEndTime > System.nanoTime())
					{
						if (isPlayer())
						{
							ThreadPool.schedule(() -> getAI().notifyActionReadyToAct(), 300);
							sendPacket(ActionFailed.STATIC_PACKET);
						}
						return;
					}
					
					// Check for MP
					if (isPlayer())
					{
						// Checking if target has moved to peace zone - only for player-bow attacks at the moment.
						// Other melee is checked in movement code and for offensive spells a check is done every time.
						if (target.isInsidePeaceZone(this))
						{
							getAI().setIntentionActive();
							sendPacket(SystemMessageId.YOU_CANNOT_ATTACK_IN_A_PEACEFUL_ZONE);
							sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						
						// Check if player has enough MP to shoot.
						int mpConsume = weaponItem.getMpConsume();
						final int reducedMp = weaponItem.getReducedMpConsume();
						if ((reducedMp > 0) && (Rnd.get(100) < weaponItem.getReducedMpConsumeChance()))
						{
							mpConsume = reducedMp;
						}
						
						mpConsume = isAffected(EffectFlag.CHEAPSHOT) ? 0 : mpConsume;
						if (_status.getCurrentMp() < mpConsume)
						{
							// If Player doesn't have enough MP, stop the attack.
							ThreadPool.schedule(() -> getAI().notifyActionReadyToAct(), 1000);
							sendPacket(SystemMessageId.NOT_ENOUGH_MP);
							sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						
						// If Player have enough MP, the bow consumes it.
						if (mpConsume > 0)
						{
							_status.reduceMp(mpConsume);
						}
					}
				}
			}
			
			// Mobius: Do not move when attack is launched.
			if (isMoving())
			{
				stopMove(getLocation());
			}
			
			final WeaponType attackType = getAttackType();
			final boolean isTwoHanded = (weaponItem != null) && (weaponItem.getBodyPart() == BodyPart.LR_HAND);
			final int timeAtk = Formulas.calculateTimeBetweenAttacks(_stat.getPAtkSpd());
			final int timeToHit = Formulas.calculateTimeToHit(timeAtk, weaponType, isTwoHanded, false);
			final long currentTime = System.nanoTime();
			_attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(timeAtk);
			
			// Precaution. It has happened in the past. Probably impossible to happen now, but will not risk it.
			if (_attackEndTime < currentTime)
			{
				_attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(Integer.MAX_VALUE);
			}
			
			// Make sure that char is facing selected target.
			// also works: setHeading(Util.convertDegreeToClientHeading(Util.calculateAngleFrom(this, target)));
			setHeading(LocationUtil.calculateHeadingFrom(this, target));
			
			// Always try to charge soulshots.
			if (!isChargedShot(ShotType.SOULSHOTS) && !isChargedShot(ShotType.BLESSED_SOULSHOTS))
			{
				rechargeShots(true, false, false);
			}
			
			// Get the Attack Reuse Delay of the Weapon.
			final Attack attack = generateAttackTargetData(target, weaponItem, attackType);
			boolean crossbow = false;
			switch (attackType)
			{
				case CROSSBOW:
				case TWOHANDCROSSBOW:
				{
					crossbow = true;
					// fallthrough
				}
				case BOW:
				{
					// Old method.
					final int reuse = Formulas.calculateReuseTime(this, weaponItem);
					
					// Try to do what is expected by having more attack speed.
					// final int reuse = (int) (Formulas.calculateReuseTime(this, weaponItem) / (Math.max(1, _stat.getAttackSpeedMultiplier() - 1)));
					
					// Consume ammunition.
					final Inventory inventory = getInventory();
					if (inventory != null)
					{
						inventory.reduceAmmunitionCount(crossbow ? EtcItemType.BOLT : EtcItemType.ARROW);
					}
					
					// Check if the Creature is a Player.
					if (isPlayer())
					{
						if (crossbow)
						{
							sendPacket(SystemMessageId.YOUR_CROSSBOW_IS_PREPARING_TO_FIRE);
						}
						
						sendPacket(new SetupGauge(getObjectId(), SetupGauge.RED, reuse));
					}
					
					// Calculate and set the disable delay of the bow in function of the Attack Speed.
					_disableRangedAttackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(reuse);
					
					// Precaution. It happened in the past for _attackEndTime. Will not risk it.
					if (_disableRangedAttackEndTime < currentTime)
					{
						_disableRangedAttackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(Integer.MAX_VALUE);
					}
					
					CreatureAttackTaskManager.getInstance().onHitTimeNotDual(this, weaponItem, attack, timeToHit, timeAtk);
					break;
				}
				case FIST:
				{
					if (!isPlayer())
					{
						CreatureAttackTaskManager.getInstance().onHitTimeNotDual(this, weaponItem, attack, timeToHit, timeAtk);
						break;
					}
					// fallthrough
				}
				case DUAL:
				case DUALFIST:
				case DUALBLUNT:
				case DUALDAGGER:
				{
					final int delayForSecondAttack = Formulas.calculateTimeToHit(timeAtk, weaponType, isTwoHanded, true) - timeToHit;
					CreatureAttackTaskManager.getInstance().onFirstHitTimeForDual(this, weaponItem, attack, timeToHit, timeAtk, delayForSecondAttack);
					break;
				}
				default:
				{
					CreatureAttackTaskManager.getInstance().onHitTimeNotDual(this, weaponItem, attack, timeToHit, timeAtk);
					break;
				}
			}
			
			// If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack.
			// to the Creature AND to all Player in the _KnownPlayers of the Creature.
			if (attack.hasHits())
			{
				broadcastPacket(attack);
			}
			
			// Flag the attacker if it's a Player outside a PvP area.
			if ((player != null) && !player.isInsideZone(ZoneId.PVP) && (player != target)) // Prevent players from flagging in PvP Zones.
			{
				AttackStanceTaskManager.getInstance().addAttackStanceTask(player);
				final boolean isPlayerSummonTarget = (target == player.getPet()) || player.getServitors().containsValue(target);
				if (!isPlayerSummonTarget)
				{
					if (((target.isRaid() && PvpConfig.FLAG_PLAYER_ON_RAID_ATTACK) || (target.isChampion() && PvpConfig.FLAG_PLAYER_ON_CHAMPION_ATTACK)))
					{
						player.updatePvPStatus();
					}
					else
					{
						player.updatePvPStatus(target);
					}
				}
			}
			else if (PvpConfig.FLAG_PLAYER_ON_SUMMON_PET_RAID_CHAMPION_ATTACK && (isSummon() || isPet()))
			{
				final Player owner = isSummon() ? asSummon().getOwner() : asPet().getOwner();
				if ((owner != null) && !owner.isInsideZone(ZoneId.PVP) && (owner != target))
				{
					final boolean isOwnerSummonTarget = (target == owner.getPet()) || owner.getServitors().containsValue(target);
					if (!isOwnerSummonTarget && ((target.isRaid() && PvpConfig.FLAG_PLAYER_ON_RAID_ATTACK) || (target.isChampion() && PvpConfig.FLAG_PLAYER_ON_CHAMPION_ATTACK)))
					{
						AttackStanceTaskManager.getInstance().addAttackStanceTask(owner);
						owner.updatePvPStatus();
					}
				}
			}
			
			if (isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE && (target.isPlayable() || target.isFakePlayer()))
			{
				final Npc npc = asNpc();
				if (!npc.isScriptValue(1))
				{
					npc.setScriptValue(1); // in combat
					broadcastInfo(); // update flag status
					ScriptManager.getInstance().getScript("PvpFlaggingStopTask").notifyEvent("FLAG_CHECK", npc, null);
				}
			}
		}
		finally
		{
			_attackLock.unlockWrite(stamp);
		}
	}
	
	/**
	 * Creates an {@code Attack} object based on the provided target and weapon.<br>
	 * This method calculates hits for dual weapons and handles additional targets.<br>
	 * It also checks for maximum simultaneous attacks based on character stats.
	 * @param target The primary {@code Creature} to attack.
	 * @param weapon The {@code Weapon} being used for the attack.
	 * @param weaponType The {@code WeaponType} of the equipped weapon.
	 * @return A new {@code Attack} object containing all calculated hits.
	 */
	private Attack generateAttackTargetData(Creature target, Weapon weapon, WeaponType weaponType)
	{
		final boolean isDual = (WeaponType.DUAL == weaponType) || (WeaponType.DUALBLUNT == weaponType) || (WeaponType.DUALDAGGER == weaponType) || (WeaponType.DUALFIST == weaponType);
		final Attack attack = new Attack(this, target);
		boolean shotConsumed = false;
		
		// Calculate the main target hit.
		Hit hit = generateHit(target, weapon, shotConsumed, isDual);
		attack.addHit(hit);
		shotConsumed = hit.isShotUsed();
		
		// Second hit for the dual attack.
		if (isDual)
		{
			hit = generateHit(target, weapon, shotConsumed, isDual);
			attack.addHit(hit);
			shotConsumed = hit.isShotUsed();
		}
		
		// H5 Changes: without Polearm Mastery (skill 216) max simultaneous attacks is 3 (1 by default + 2 in skill 3599).
		int attackCountMax = (int) _stat.getValue(Stat.ATTACK_COUNT_MAX, 1);
		if ((attackCountMax > 1) && (_stat.getValue(Stat.PHYSICAL_POLEARM_TARGET_SINGLE, 0) <= 0))
		{
			final double headingAngle = LocationUtil.convertHeadingToDegree(getHeading());
			final int maxRadius = _stat.getPhysicalAttackRadius();
			final int physicalAttackAngle = _stat.getPhysicalAttackAngle();
			for (Creature obj : World.getVisibleObjectsInRange(this, Creature.class, maxRadius))
			{
				// Skip main target.
				if (obj == target)
				{
					continue;
				}
				
				// Skip dead or fake dead target.
				if (obj.isAlikeDead())
				{
					continue;
				}
				
				// Check if target is auto attackable.
				if (!obj.isAutoAttackable(this))
				{
					continue;
				}
				
				// Check if target is within attack angle.
				if (Math.abs(calculateDirectionTo(obj) - headingAngle) > physicalAttackAngle)
				{
					continue;
				}
				
				// Launch a simple attack against the additional target.
				hit = generateHit(obj, weapon, shotConsumed, false);
				attack.addHit(hit);
				shotConsumed = hit.isShotUsed();
				
				// Side targets flag the attacker as well.
				final Player attackerPlayer = asPlayer();
				if ((attackerPlayer != null) && !attackerPlayer.isInsideZone(ZoneId.PVP) && (attackerPlayer != obj) && (obj != attackerPlayer.getPet()) && !attackerPlayer.getServitors().containsValue(obj))
				{
					attackerPlayer.updatePvPStatus(obj);
				}
				
				if (--attackCountMax <= 0)
				{
					break;
				}
			}
		}
		
		return attack;
	}
	
	/**
	 * Calculates the result of an attack against a target.<br>
	 * This method determines if the attack hits, calculates damage, and checks for critical strikes.<br>
	 * It also handles special mechanics like soulshots and half-damage modifiers.
	 * @param target The {@code Creature} being attacked.
	 * @param weapon The {@code Weapon} used to perform the attack.
	 * @param shotConsumedValue Whether the shot should be considered consumed.
	 * @param halfDamage Whether the resulting damage should be halved.
	 * @return A new {@code Hit} object containing all calculation results.
	 */
	private Hit generateHit(Creature target, Weapon weapon, boolean shotConsumedValue, boolean halfDamage)
	{
		int damage = 0;
		byte shld = 0;
		boolean crit = false;
		boolean shotConsumed = shotConsumedValue;
		boolean shotBlessed = false;
		final boolean miss = Formulas.calcHitMiss(this, target);
		if (!shotConsumed)
		{
			if (isChargedShot(ShotType.BLESSED_SOULSHOTS))
			{
				shotBlessed = true;
				shotConsumed = !miss && unchargeShot(ShotType.BLESSED_SOULSHOTS);
			}
			else
			{
				shotConsumed = !miss && unchargeShot(ShotType.SOULSHOTS);
			}
		}
		
		final int ssGrade = (shotConsumed && (weapon != null)) ? weapon.getItemGrade().ordinal() : 0;
		
		// Check if hit isn't missed.
		if (!miss)
		{
			crit = Formulas.calcCrit(_stat.getCriticalHit(), this, target, null);
			shld = Formulas.calcShldUse(this, target, true, crit);
			damage = (int) Formulas.calcAutoAttackDamage(this, target, shld, crit, shotConsumed, shotBlessed);
			if (halfDamage)
			{
				damage /= 2;
			}
		}
		
		return new Hit(target, damage, miss, crit, shld, shotConsumed, ssGrade);
	}
	
	/**
	 * Executes the casting logic for a specific skill.<br>
	 * This method handles the internal processing required to trigger a {@code Skill}.
	 * @param skill The {@code Skill} object to be cast.
	 */
	public void doCast(Skill skill)
	{
		doCast(skill, null, false, false);
	}
	
	/**
	 * Executes the casting of a specific skill.<br>
	 * This method checks if the actor is allowed to cast based on movement and status.<br>
	 * It determines the correct casting type and attempts to initiate the action.
	 * @param skill The {@code Skill} to be cast.
	 * @param item The {@code Item} required for the skill, if any.
	 * @param ctrlPressed Whether the {@code Ctrl} key is currently held down.
	 * @param shiftPressed Whether the {@code Shift} key is currently held down.
	 */
	public synchronized void doCast(Skill skill, Item item, boolean ctrlPressed, boolean shiftPressed)
	{
		// Attackables cannot cast while moving.
		if (isAttackable() && isMoving())
		{
			return;
		}
		
		// Get proper casting type.
		SkillCastingType castingType = SkillCastingType.NORMAL;
		if (skill.canDoubleCast() && isAffected(EffectFlag.DOUBLE_CAST) && isCastingNow(castingType))
		{
			castingType = SkillCastingType.NORMAL_SECOND;
		}
		
		// Try casting the skill.
		final SkillCaster skillCaster = SkillCaster.castSkill(this, _target, skill, item, castingType, ctrlPressed, shiftPressed);
		if ((skillCaster == null) && isPlayer())
		{
			// Skill casting failed, notify player.
			sendPacket(ActionFailed.get(castingType));
			getAI().setIntentionActive();
		}
	}
	
	/**
	 * Retrieves the timestamps for items that have been reused.
	 * @return a {@code Map} where the key is the item ID and the value is its {@code TimeStamp}.
	 */
	public Map<Integer, TimeStamp> getItemReuseTimeStamps()
	{
		return _reuseTimeStampsItems;
	}
	
	/**
	 * Adds an {@code Item} to the list of items with a specific timestamp.<br>
	 * This method helps track when an item was added or updated.
	 * @param item The {@code Item} object to be added.
	 * @param reuse The value used to determine if the timestamp should be reused.
	 */
	public void addTimeStampItem(Item item, long reuse)
	{
		addTimeStampItem(item, reuse, -1);
	}
	
	/**
	 * Adds a new {@code TimeStamp} entry for an item to the internal tracking map.<br>
	 * This method uses the unique object ID of the {@code Item} as the key.
	 * @param item The {@code Item} instance to track.
	 * @param reuse The time value used for calculating reuse.
	 * @param systime The current system time to record.
	 */
	public void addTimeStampItem(Item item, long reuse, long systime)
	{
		_reuseTimeStampsItems.put(item.getObjectId(), new TimeStamp(item, reuse, systime));
	}
	
	/**
	 * Calculates the remaining time until an item can be reused.<br>
	 * This method checks the internal timestamp for a specific object.<br>
	 * It returns the remaining duration in milliseconds.
	 * @param itemObjId The unique identifier of the item object.
	 * @return The remaining reuse time in milliseconds, or -1 if no reuse stamp exists.
	 */
	public long getItemRemainingReuseTime(int itemObjId)
	{
		final TimeStamp reuseStamp = _reuseTimeStampsItems.get(itemObjId);
		return reuseStamp != null ? reuseStamp.getRemaining() : -1;
	}
	
	/**
	 * Retrieves the remaining delay for a specific reuse group.<br>
	 * It checks all active timestamps to find the one matching the provided {@code group}.<br>
	 * If no valid timestamp is found, it returns -1.
	 * @param group The unique identifier of the reuse group to check.
	 * @return The number of milliseconds remaining until the action can be reused, or -1 if not applicable.
	 */
	public long getReuseDelayOnGroup(int group)
	{
		if ((group > 0) && !_reuseTimeStampsItems.isEmpty())
		{
			final long currentTime = System.currentTimeMillis();
			for (TimeStamp ts : _reuseTimeStampsItems.values())
			{
				if (ts.getSharedReuseGroup() == group)
				{
					final long stamp = ts.getStamp();
					if (currentTime < stamp)
					{
						return Math.max(stamp - currentTime, 0);
					}
				}
			}
		}
		
		return -1;
	}
	
	/**
	 * Clears all stored item reuse timestamps.<br>
	 * This resets the cooldown tracking for item use.
	 */
	public void resetTimeStampsItems()
	{
		_reuseTimeStampsItems.clear();
	}
	
	/**
	 * Retrieves the timestamps for skill reuses.<br>
	 * This method returns a map where the key is the skill ID and the value is the {@code TimeStamp}.
	 * @return A {@code Map} containing skill IDs and their corresponding reuse timestamps.
	 */
	public Map<Long, TimeStamp> getSkillReuseTimeStamps()
	{
		return _reuseTimeStampsSkills;
	}
	
	/**
	 * Adds a timestamp to a specific {@code Skill}.<br>
	 * This method helps track when a skill was last used.
	 * @param skill The {@code Skill} object to update.
	 * @param reuse The amount of time the skill must wait before being reused.
	 */
	public void addTimeStamp(Skill skill, long reuse)
	{
		addTimeStamp(skill, reuse, -1);
	}
	
	/**
	 * Adds a timestamp for a specific skill to the reuse tracking map.<br>
	 * This method records when a skill can be used again based on the provided time values.
	 * @param skill The {@code Skill} object to be updated.
	 * @param reuse The cooldown or reuse duration for the skill.
	 * @param systime The current system time to use as a reference point.
	 */
	public void addTimeStamp(Skill skill, long reuse, long systime)
	{
		_reuseTimeStampsSkills.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse, systime));
	}
	
	/**
	 * Removes a skill from the reuse time stamp tracking map.<br>
	 * This method uses the hash code of the {@code Skill} to identify it.
	 * @param skill The {@code Skill} object to be removed.
	 */
	public void removeTimeStamp(Skill skill)
	{
		_reuseTimeStampsSkills.remove(skill.getReuseHashCode());
	}
	
	/**
	 * Clears all stored skill reuse timestamps.<br>
	 * This resets the cooldown tracking for skills.
	 */
	public void resetTimeStamps()
	{
		_reuseTimeStampsSkills.clear();
	}
	
	/**
	 * Retrieves the remaining time until a skill can be reused.<br>
	 * It uses the provided {@code hashCode} to look up the specific skill.<br>
	 * Returns -1 if no reuse time is recorded.
	 * @param hashCode The unique identifier for the skill.
	 * @return The remaining time in milliseconds, or -1 if not found.
	 */
	public long getSkillRemainingReuseTime(long hashCode)
	{
		final TimeStamp reuseStamp = _reuseTimeStampsSkills.get(hashCode);
		return reuseStamp != null ? reuseStamp.getRemaining() : -1;
	}
	
	/**
	 * Checks if a skill is currently on cooldown.<br>
	 * It verifies if the skill's reuse time has not passed yet.
	 * @param hashCode The unique identifier for the skill.
	 * @return {@code true} if the skill is still on cooldown, {@code false} otherwise.
	 */
	public boolean hasSkillReuse(long hashCode)
	{
		final TimeStamp reuseStamp = _reuseTimeStampsSkills.get(hashCode);
		return (reuseStamp != null) && reuseStamp.hasNotPassed();
	}
	
	/**
	 * Retrieves the last time a specific skill was used.<br>
	 * This method uses the provided hash code to look up the timestamp.
	 * @param hashCode The unique identifier for the skill.
	 * @return The {@code TimeStamp} of the last usage, or {@code null} if not found.
	 */
	public synchronized TimeStamp getSkillReuseTimeStamp(long hashCode)
	{
		return _reuseTimeStampsSkills.get(hashCode);
	}
	
	/**
	 * Retrieves the list of skills that are currently disabled.
	 * @return a {@code Map<Long, Long>} containing the IDs and counts of disabled skills.
	 */
	public Map<Long, Long> getDisabledSkills()
	{
		return _disabledSkills;
	}
	
	/**
	 * Enables a specific skill for the actor.<br>
	 * This method removes the skill from the {@code _disabledSkills} set.<br>
	 * If the provided {@code Skill} is {@code null}, the method returns immediately.
	 * @param skill The {@code Skill} object to enable.
	 */
	public void enableSkill(Skill skill)
	{
		if (skill == null)
		{
			return;
		}
		
		_disabledSkills.remove(skill.getReuseHashCode());
	}
	
	/**
	 * Disables a specific skill for the actor.<br>
	 * The skill will remain disabled until the specified time passes.<br>
	 * If {@code delay} is less than or equal to 0, the skill is disabled permanently.
	 * @param skill The {@code Skill} object to be disabled.
	 * @param delay The amount of time in milliseconds before the skill becomes available again.
	 */
	public void disableSkill(Skill skill, long delay)
	{
		if (skill == null)
		{
			return;
		}
		
		_disabledSkills.put(skill.getReuseHashCode(), delay > 0 ? System.currentTimeMillis() + delay : Long.MAX_VALUE);
	}
	
	/**
	 * Clears all skills that are currently disabled.<br>
	 * This method resets the {@code _disabledSkills} collection to an empty state.
	 */
	public void resetDisabledSkills()
	{
		_disabledSkills.clear();
	}
	
	/**
	 * Checks if a specific skill is currently disabled for the actor.<br>
	 * This method evaluates global flags, active effects, and cooldown timers.
	 * @param skill The {@code Skill} object to check.
	 * @return {@code true} if the skill cannot be used, {@code false} otherwise.
	 */
	public boolean isSkillDisabled(Skill skill)
	{
		if (skill == null)
		{
			return false;
		}
		
		if (_allSkillsDisabled || (!skill.canCastWhileDisabled() && isAllSkillsDisabled()))
		{
			return true;
		}
		
		if (isAffected(EffectFlag.CONDITIONAL_BLOCK_ACTIONS) && !isBlockedActionsAllowedSkill(skill))
		{
			return true;
		}
		
		final long hashCode = skill.getReuseHashCode();
		if (hasSkillReuse(hashCode))
		{
			return true;
		}
		
		if (_disabledSkills.isEmpty())
		{
			return false;
		}
		
		final Long stamp = _disabledSkills.get(hashCode);
		if (stamp == null)
		{
			return false;
		}
		
		if (stamp < System.currentTimeMillis())
		{
			_disabledSkills.remove(hashCode);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Sets the {@code _allSkillsDisabled} flag to {@code true}.<br>
	 * This prevents the actor from using any skills.
	 */
	public void disableAllSkills()
	{
		_allSkillsDisabled = true;
	}
	
	/**
	 * Enables all skills for the actor.<br>
	 * This method sets the {@code _allSkillsDisabled} flag to {@code false}.
	 */
	public void enableAllSkills()
	{
		_allSkillsDisabled = false;
	}
	
	/**
	 * Processes the death of this {@code Creature}.<br>
	 * It sets the health to {@code 0} and marks the entity as dead.<br>
	 * This method also handles rewards, stops active actions, and notifies relevant systems.
	 * @param killer The {@code Creature} that caused the death.
	 * @return {@code true} if the creature was successfully killed, or {@code false} if it was already dead.
	 */
	public boolean doDie(Creature killer)
	{
		// Killing is only possible one time.
		synchronized (this)
		{
			if (_isDead)
			{
				return false;
			}
			
			// Now reset currentHp to zero.
			setCurrentHp(0);
			setDead(true);
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_DEATH, this))
		{
			EventDispatcher.getInstance().notifyEvent(new OnCreatureDeath(killer, this), this);
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_KILLED, killer))
		{
			EventDispatcher.getInstance().notifyEvent(new OnCreatureKilled(killer, this), killer);
		}
		
		abortAttack();
		abortCast();
		
		// Calculate rewards for main damage dealer.
		final Creature mainDamageDealer = isMonster() ? asMonster().getMainDamageDealer() : null;
		calculateRewards(mainDamageDealer != null ? mainDamageDealer : killer);
		
		// Set target to null and cancel Attack or Cast.
		setTarget(null);
		
		// Stop movement
		stopMove(null);
		
		// Stop HP/MP/CP Regeneration task.
		_status.stopHpMpRegeneration();
		
		if (isAttackable())
		{
			final Spawn spawn = asNpc().getSpawn();
			if ((spawn != null) && spawn.isRespawnEnabled())
			{
				stopAllEffects();
			}
			else
			{
				_effectList.stopAllEffectsWithoutExclusions(true, true);
			}
			
			// Clan help range aggro on kill.
			if ((killer != null) && killer.isPlayable() && !killer.asPlayer().isGM())
			{
				final NpcTemplate template = asAttackable().getTemplate();
				final Set<Integer> clans = template.getClans();
				if ((clans != null) && !clans.isEmpty())
				{
					World.forEachVisibleObjectInRange(this, Attackable.class, template.getClanHelpRange(), called ->
					{
						// Don't call dead npcs, npcs without ai or npcs which are too far away.
						if (called.isDead() || !called.hasAI() || (Math.abs(killer.getZ() - called.getZ()) > 600))
						{
							return;
						}
						
						// Don't call npcs spawned within the last 7 seconds.
						if ((System.currentTimeMillis() - called.getSpawnTime()) < 7000)
						{
							return;
						}
						
						// Don't call npcs who are already doing some action (e.g. attacking, casting).
						final CreatureAI ai = called.getAI();
						final Intention intention = ai.getIntention();
						if ((intention != Intention.IDLE) && (intention != Intention.ACTIVE))
						{
							return;
						}
						
						// Don't call npcs who aren't in the same clan.
						if (!template.isClan(called.getTemplate().getClans()))
						{
							return;
						}
						
						// By default, when a faction member calls for help, attack the caller's attacker.
						ai.notifyActionAggression(killer, 1);
						
						if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_FACTION_CALL, called))
						{
							EventDispatcher.getInstance().notifyEventAsync(new OnAttackableFactionCall(called, asAttackable(), killer.asPlayer(), killer.isSummon()), called);
						}
					});
				}
			}
		}
		else
		{
			stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform.
		broadcastStatusUpdate();
		
		// Notify Creature AI
		if (hasAI())
		{
			getAI().notifyActionDeath();
		}
		
		ZoneManager.getInstance().getRegion(this).onDeath(this);
		
		getAttackByList().clear();
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		
		// Custom boss announcements configuration.
		if (this instanceof GrandBoss)
		{
			if (BossAnnouncementsConfig.GRANDBOSS_DEFEAT_ANNOUNCEMENTS && (!isInInstance() || BossAnnouncementsConfig.GRANDBOSS_INSTANCE_ANNOUNCEMENTS) && !isMinion() && !isRaidMinion())
			{
				final int id = getId();
				final String name = NpcData.getInstance().getTemplate(id).getName();
				if ((name != null) && !BossAnnouncementsConfig.RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS.contains(id))
				{
					World.broadcastToAllOnlinePlayers(name + " has been defeated!");
					World.broadcastToAllOnlinePlayersOnScreen(name + " has been defeated!");
				}
			}
		}
		else if (isRaid() && BossAnnouncementsConfig.RAIDBOSS_DEFEAT_ANNOUNCEMENTS && (!isInInstance() || BossAnnouncementsConfig.RAIDBOSS_INSTANCE_ANNOUNCEMENTS) && !isMinion() && !isRaidMinion())
		{
			final int id = getId();
			final String name = NpcData.getInstance().getTemplate(id).getName();
			if ((name != null) && !BossAnnouncementsConfig.RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS.contains(id))
			{
				World.broadcastToAllOnlinePlayers(name + " has been defeated!");
				World.broadcastToAllOnlinePlayersOnScreen(name + " has been defeated!");
			}
		}
		
		return true;
	}
	
	/**
	 * Reduces the current activity of the entity.<br>
	 * This method clears active targets and resets the AI intention to idle if applicable.<br>
	 * It then calls the parent class implementation.
	 * @return {@code true} if the decay was successful, {@code false} otherwise.
	 */
	@Override
	public boolean decayMe()
	{
		if (hasAI())
		{
			final CreatureAI ai = getAI();
			if (isAttackable())
			{
				getAttackByList().clear();
				asAttackable().clearAggroList();
				ai.setIntentionIdle();
			}
			
			ai.stopAITask();
		}
		
		return super.decayMe();
	}
	
	/**
	 * Removes this entity from the game world.<br>
	 * Stops all active AI tasks and effects.<br>
	 * Clears summoner references and seen creatures.
	 * @return {@code true} if the removal was successful.
	 */
	public boolean deleteMe()
	{
		if (hasAI())
		{
			getAI().stopAITask();
		}
		
		// Removes itself from the summoned list.
		if (_summoner != null)
		{
			_summoner.removeSummonedNpc(getObjectId());
		}
		
		// Remove all active, passive and option effects, do not broadcast changes.
		_effectList.stopAllEffectsWithoutExclusions(false, false);
		
		// Forget all seen creatures.
		if (_seenCreatures != null)
		{
			CreatureSeeTaskManager.getInstance().remove(this);
			_seenCreatures.clear();
		}
		
		// Cancel the BuffFinishTask related to this creature.
		_buffFinishTask.stop();
		
		// Set world region to null.
		setWorldRegion(null);
		
		return true;
	}
	
	/**
	 * Calculates and distributes rewards for defeating a specific creature.<br>
	 * This method handles the logic for loot and experience gains.
	 * @param killer The {@code Creature} that defeated the target.
	 */
	protected void calculateRewards(Creature killer)
	{
	}
	
	/**
	 * Resurrects the entity if it is currently dead.<br>
	 * This method resets the death status and restores health, mana, or CP based on configuration values.<br>
	 * It also broadcasts a {@code Revive} packet to other players.<br>
	 * If the entity is currently teleporting, the revive process is marked as pending.
	 */
	public void doRevive()
	{
		synchronized (this)
		{
			if (!_isDead)
			{
				return;
			}
			
			if (_isTeleporting)
			{
				setIsPendingRevive(true);
				return;
			}
			
			setIsPendingRevive(false);
			setDead(false);
		}
		
		if (PlayerConfig.RESPAWN_RESTORE_CP > 0)
		{
			final double restoreCp = _stat.getMaxCp() * PlayerConfig.RESPAWN_RESTORE_CP;
			if (_status.getCurrentCp() < restoreCp)
			{
				_status.setCurrentCp(restoreCp);
			}
		}
		
		if (PlayerConfig.RESPAWN_RESTORE_HP > 0)
		{
			final double restoreHp = _stat.getMaxHp() * PlayerConfig.RESPAWN_RESTORE_HP;
			if (_status.getCurrentHp() < restoreHp)
			{
				_status.setCurrentHp(restoreHp);
			}
		}
		
		if (PlayerConfig.RESPAWN_RESTORE_MP > 0)
		{
			final double restoreMp = _stat.getMaxMp() * PlayerConfig.RESPAWN_RESTORE_MP;
			if (_status.getCurrentMp() < restoreMp)
			{
				_status.setCurrentMp(restoreMp);
			}
		}
		
		// Start broadcast status.
		broadcastPacket(new Revive(this));
		ZoneManager.getInstance().getRegion(this).onRevive(this);
	}
	
	/**
	 * Revives the entity using a specific power level.<br>
	 * This method handles the logic for bringing an actor back to life.
	 * @param revivePower The amount of power used for the revival process.
	 */
	public void doRevive(double revivePower)
	{
		doRevive();
	}
	
	/**
	 * Retrieves the artificial intelligence for this creature.<br>
	 * This method initializes the {@code CreatureAI} if it is currently null.<br>
	 * It uses a synchronized block to ensure thread safety during initialization.
	 * @return The {@code CreatureAI} instance associated with this creature.
	 */
	public CreatureAI getAI()
	{
		CreatureAI ai = _ai;
		if (ai == null)
		{
			synchronized (this)
			{
				ai = _ai;
				if (ai == null)
				{
					_ai = ai = initAI();
				}
			}
		}
		
		return ai;
	}
	
	/**
	 * Initializes the artificial intelligence for this creature.<br>
	 * This method creates and returns a new {@code CreatureAI} instance.
	 * @return A new {@code CreatureAI} object associated with this creature.
	 */
	protected CreatureAI initAI()
	{
		return new CreatureAI(this);
	}
	
	/**
	 * Removes the current AI from this entity.<br>
	 * This method is skipped if the entity is a walker.<br>
	 * It sets the internal AI reference to {@code null}.
	 */
	public void detachAI()
	{
		if (isWalker())
		{
			return;
		}
		
		setAI(null);
	}
	
	/**
	 * Updates the AI behavior of this creature.<br>
	 * This method replaces the current {@code CreatureAI} with a new one.<br>
	 * If the old AI was an instance of {@code AttackableAI}, its task will be stopped automatically.
	 * @param newAI The new {@code CreatureAI} to assign to this creature.
	 */
	public void setAI(CreatureAI newAI)
	{
		final CreatureAI oldAI = _ai;
		if ((oldAI != null) && (oldAI != newAI) && (oldAI instanceof AttackableAI))
		{
			oldAI.stopAITask();
		}
		
		_ai = newAI;
	}
	
	/**
	 * Checks if the entity has an active AI component.<br>
	 * Returns {@code true} if the internal AI object is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the entity has AI, {@code false} otherwise.
	 */
	public boolean hasAI()
	{
		return _ai != null;
	}
	
	/**
	 * Checks if the current entity is participating in a raid.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the entity is in a raid, otherwise {@code false}.
	 */
	public boolean isRaid()
	{
		return false;
	}
	
	/**
	 * Checks if the current entity is a minion.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the entity is a minion, otherwise {@code false}.
	 */
	public boolean isMinion()
	{
		return false;
	}
	
	/**
	 * Checks if the current creature is a raid minion.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the creature is a raid minion, otherwise {@code false}.
	 */
	public boolean isRaidMinion()
	{
		return false;
	}
	
	/**
	 * Retrieves the list of creatures that are currently attacking this entity.
	 * @return a {@code Set} containing all {@code Creature} objects in the attack list.
	 */
	public Set<Creature> getAttackByList()
	{
		return _attackByList;
	}
	
	/**
	 * Checks if the entity is currently under a control block effect.<br>
	 * This method returns {@code true} if the {@code BLOCK_CONTROL} flag is active.
	 * @return {@code true} if control is blocked, {@code false} otherwise.
	 */
	public boolean isControlBlocked()
	{
		return isAffected(EffectFlag.BLOCK_CONTROL);
	}
	
	/**
	 * Checks if all skills are currently disabled for this entity.<br>
	 * This returns {@code true} if the {@code _allSkillsDisabled} flag is set or if there are active block actions.
	 * @return {@code true} if no skills can be used, {@code false} otherwise.
	 */
	public boolean isAllSkillsDisabled()
	{
		return _allSkillsDisabled || hasBlockActions();
	}
	
	/**
	 * Checks if the actor is currently unable to perform attacks.<br>
	 * This returns {@code true} if the actor is attacking or disabled.
	 * @return {@code true} if the attack is disabled, {@code false} otherwise.
	 */
	public boolean isAttackDisabled()
	{
		return isAttackingNow() || isDisabled();
	}
	
	/**
	 * Checks if the actor's AI is currently disabled.<br>
	 * This returns {@code true} if any disabling conditions are met.<br>
	 * These conditions include being dead, having muted physical attacks, or performing block actions.
	 * @return {@code true} if the actor is disabled, {@code false} otherwise.
	 */
	public boolean isDisabled()
	{
		return _disabledAI || isAlikeDead() || isPhysicalAttackMuted() || hasBlockActions();
	}
	
	/**
	 * Checks if the creature is currently under a confusion effect.<br>
	 * This method returns {@code true} if the {@code EffectFlag.CONFUSED} status is active.
	 * @return {@code true} if confused, {@code false} otherwise.
	 */
	public boolean isConfused()
	{
		return isAffected(EffectFlag.CONFUSED);
	}
	
	/**
	 * Checks if the entity is currently dead.
	 * @return {@code true} if the entity is dead, {@code false} otherwise.
	 */
	public boolean isAlikeDead()
	{
		return _isDead;
	}
	
	/**
	 * Checks if the creature is currently dead.
	 * @return {@code true} if the creature is dead, {@code false} otherwise.
	 */
	public boolean isDead()
	{
		return _isDead;
	}
	
	/**
	 * Updates the death status of the creature.<br>
	 * Sets the internal {@code _isDead} flag to the provided value.
	 * @param value The new death state to apply. Use {@code true} if the creature is dead and {@code false} otherwise.
	 */
	public void setDead(boolean value)
	{
		_isDead = value;
	}
	
	/**
	 * Checks if the entity is currently immobilized.<br>
	 * This status prevents the entity from moving freely.
	 * @return {@code true} if the entity is immobilized, {@code false} otherwise.
	 */
	public boolean isImmobilized()
	{
		return _isImmobilized;
	}
	
	/**
	 * Sets whether the entity is currently immobilized.<br>
	 * Use {@code true} to prevent movement.<br>
	 * Use {@code false} to allow normal movement.
	 * @param value The immobilization state to set.
	 */
	public void setImmobilized(boolean value)
	{
		_isImmobilized = value;
	}
	
	/**
	 * Checks if the creature is currently muted.<br>
	 * This method returns {@code true} if the {@code EffectFlag.MUTED} flag is active.
	 * @return {@code true} if the entity is muted, {@code false} otherwise.
	 */
	public boolean isMuted()
	{
		return isAffected(EffectFlag.MUTED);
	}
	
	/**
	 * Checks if the entity is currently under a physical mute effect.<br>
	 * This method returns {@code true} if the {@code PSYCHICAL_MUTED} flag is active.
	 * @return {@code true} if the entity is physically muted, {@code false} otherwise.
	 */
	public boolean isPhysicalMuted()
	{
		return isAffected(EffectFlag.PSYCHICAL_MUTED);
	}
	
	/**
	 * Checks if the entity is currently affected by a physical attack mute.<br>
	 * This method returns {@code true} if the {@code PSYCHICAL_ATTACK_MUTED} flag is active.
	 * @return {@code true} if physical attacks are muted, {@code false} otherwise.
	 */
	public boolean isPhysicalAttackMuted()
	{
		return isAffected(EffectFlag.PSYCHICAL_ATTACK_MUTED);
	}
	
	/**
	 * Checks if the actor's movement is currently restricted.<br>
	 * This method returns {@code true} if the actor is blocked, rooted, overloaded, immobilized, dead, or teleporting.
	 * @return {@code true} if movement is disabled, {@code false} otherwise.
	 */
	public boolean isMovementDisabled()
	{
		// check for isTeleporting to prevent teleport cheating (if appear packet not received)
		return hasBlockActions() || isRooted() || _isOverloaded || _isImmobilized || isAlikeDead() || _isTeleporting;
	}
	
	/**
	 * Checks if the entity is currently in an overloaded state.
	 * @return {@code true} if the entity is overloaded, {@code false} otherwise.
	 */
	public boolean isOverloaded()
	{
		return _isOverloaded;
	}
	
	/**
	 * Sets whether the entity is currently overloaded.<br>
	 * This updates the {@code _isOverloaded} state of the creature.
	 * @param value The new overload status to set.
	 */
	public void setOverloaded(boolean value)
	{
		_isOverloaded = value;
	}
	
	/**
	 * Checks if the entity is currently waiting to be revived.<br>
	 * This returns {@code true} only if the entity is dead and has a pending revive status.
	 * @return {@code true} if the entity is dead and awaiting revival, {@code false} otherwise.
	 */
	public boolean isPendingRevive()
	{
		return _isDead && _isPendingRevive;
	}
	
	/**
	 * Sets whether the entity is currently waiting to be revived.<br>
	 * This flag determines if a revive action is pending for this object.
	 * @param value The new status of the pending revive state.
	 */
	public void setIsPendingRevive(boolean value)
	{
		_isPendingRevive = value;
	}
	
	/**
	 * Checks if the entity is currently disarmed.<br>
	 * This method returns {@code true} if the {@code EffectFlag.DISARMED} effect is active.
	 * @return {@code true} if the entity is disarmed, {@code false} otherwise.
	 */
	public boolean isDisarmed()
	{
		return isAffected(EffectFlag.DISARMED);
	}
	
	/**
	 * Retrieves the pet associated with this entity.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Summon} object representing the pet, or {@code null} if no pet exists.
	 */
	public Summon getPet()
	{
		return null;
	}
	
	/**
	 * Retrieves the list of servitors associated with this entity.<br>
	 * This method currently returns an empty map.
	 * @return a {@code Map<Integer, Summon>} containing the servitors.
	 */
	public Map<Integer, Summon> getServitors()
	{
		return Collections.emptyMap();
	}
	
	/**
	 * Retrieves a {@code Summon} associated with the specified object ID.<br>
	 * This method currently returns {@code null}.
	 * @param objectId The unique identifier of the object.
	 * @return The {@code Summon} object or {@code null}.
	 */
	public Summon getServitor(int objectId)
	{
		return null;
	}
	
	/**
	 * Checks if the actor currently has a summon active.<br>
	 * This returns {@code true} if there is a pet or any servitors.
	 * @return {@code true} if a summon exists, {@code false} otherwise.
	 */
	public boolean hasSummon()
	{
		return (getPet() != null) || !getServitors().isEmpty();
	}
	
	/**
	 * Checks if the entity currently has a pet.<br>
	 * This method returns {@code true} if a pet is linked to this creature.<br>
	 * It returns {@code false} if no pet exists.
	 * @return {@code true} if there is a pet, otherwise {@code false}.
	 */
	public boolean hasPet()
	{
		return getPet() != null;
	}
	
	/**
	 * Checks if a specific object is currently a servitor.<br>
	 * This method looks up the {@code objectId} in the internal servitor collection.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if the object is a servitor, {@code false} otherwise.
	 */
	public boolean hasServitor(int objectId)
	{
		return getServitors().containsKey(objectId);
	}
	
	/**
	 * Checks if the creature has any servitors.<br>
	 * It returns {@code true} if the servitor list is not empty.<br>
	 * It returns {@code false} if there are no servitors.
	 * @return {@code true} if servitors exist, {@code false} otherwise.
	 */
	public boolean hasServitors()
	{
		return !getServitors().isEmpty();
	}
	
	/**
	 * Removes a specific servitor from the list of active servitors.<br>
	 * This method uses the unique identifier to locate and delete the entry.
	 * @param objectId The unique ID of the servitor to be removed.
	 */
	public void removeServitor(int objectId)
	{
		getServitors().remove(objectId);
	}
	
	/**
	 * Checks if the creature is currently affected by a rooted status.<br>
	 * This method returns {@code true} if the root effect is active.
	 * @return {@code true} if the creature is rooted, {@code false} otherwise.
	 */
	public boolean isRooted()
	{
		return isAffected(EffectFlag.ROOTED);
	}
	
	/**
	 * Checks if the actor is currently running.
	 * @return {@code true} if the actor is running, {@code false} otherwise.
	 */
	public boolean isRunning()
	{
		return _isRunning;
	}
	
	/**
	 * Updates the running state of this entity.<br>
	 * This method synchronizes the internal {@code _isRunning} flag with the provided value.<br>
	 * It also broadcasts necessary updates to nearby players based on the entity type.
	 * @param value The new running state to set.
	 */
	private void setRunning(boolean value)
	{
		if (_isRunning == value)
		{
			return;
		}
		
		_isRunning = value;
		final double runSpeed = _stat.getRunSpeed();
		if (runSpeed != 0)
		{
			broadcastPacket(new ChangeMoveType(this));
		}
		
		if (isPlayer())
		{
			asPlayer().broadcastUserInfo();
		}
		else if (isSummon())
		{
			broadcastStatusUpdate();
		}
		else if (isNpc())
		{
			World.forEachVisibleObject(this, Player.class, player ->
			{
				if (!isVisibleFor(player))
				{
					return;
				}
				
				if (isFakePlayer())
				{
					player.sendPacket(new FakePlayerInfo(asNpc()));
				}
				else if (runSpeed == 0)
				{
					player.sendPacket(new ServerObjectInfo(asNpc(), player));
				}
				else
				{
					player.sendPacket(new NpcInfo(asNpc()));
				}
			});
		}
	}
	
	/**
	 * Sets the running state of the actor to {@code true}.<br>
	 * This method is used to initialize the active status.
	 */
	public void setRunning()
	{
		setRunning(true);
	}
	
	/**
	 * Checks if the entity is currently prevented from performing actions.<br>
	 * This returns {@code true} if the entity is blocked by a status or flag.
	 * @return {@code true} if actions are blocked, {@code false} otherwise.
	 */
	public boolean hasBlockActions()
	{
		return _blockActions || isAffected(EffectFlag.BLOCK_ACTIONS) || isAffected(EffectFlag.CONDITIONAL_BLOCK_ACTIONS);
	}
	
	/**
	 * Sets whether actions for this entity are currently blocked.<br>
	 * Use {@code true} to prevent the entity from performing any actions.<br>
	 * Use {@code false} to allow normal behavior.
	 * @param blockActions The boolean state to set for blocking actions.
	 */
	public void setBlockActions(boolean blockActions)
	{
		_blockActions = blockActions;
	}
	
	/**
	 * Checks if the entity has been betrayed.<br>
	 * This method returns {@code true} if the {@code EffectFlag.BETRAYED} flag is active.
	 * @return {@code true} if the entity is betrayed, {@code false} otherwise.
	 */
	public boolean isBetrayed()
	{
		return isAffected(EffectFlag.BETRAYED);
	}
	
	/**
	 * Checks if the entity is currently in the process of teleporting.<br>
	 * This state is used to determine if movement or other actions should be restricted.
	 * @return {@code true} if the entity is teleporting, {@code false} otherwise.
	 */
	public boolean isTeleporting()
	{
		return _isTeleporting;
	}
	
	/**
	 * Sets whether the entity is currently in the process of teleporting.<br>
	 * Use {@code true} to indicate a teleport is happening.<br>
	 * Use {@code false} to indicate the entity is not teleporting.
	 * @param value The teleportation state to set.
	 */
	public void setTeleporting(boolean value)
	{
		_isTeleporting = value;
	}
	
	/**
	 * Sets whether the entity is currently invulnerable.<br>
	 * Use {@code true} to make the entity immune to damage.<br>
	 * Use {@code false} to allow the entity to take damage.
	 * @param value The invulnerability state to set.
	 */
	public void setInvul(boolean value)
	{
		_isInvul = value;
	}
	
	/**
	 * Checks if the entity is currently invulnerable.<br>
	 * This returns {@code true} if the entity is marked as invulnerable or is currently teleporting.
	 * @return {@code true} if the entity cannot be damaged, {@code false} otherwise.
	 */
	@Override
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting;
	}
	
	/**
	 * Sets whether the entity is currently in an undying state.<br>
	 * This determines if the entity can be killed or destroyed.
	 * @param undying The status to set for the undying property.
	 */
	public void setUndying(boolean undying)
	{
		_isUndying = undying;
	}
	
	/**
	 * Checks if the entity is currently in an undying state.<br>
	 * This returns {@code true} if the entity is immortal, has a specific effect, or is in a special zone.
	 * @return {@code true} if the entity cannot die, {@code false} otherwise.
	 */
	public boolean isUndying()
	{
		return _isUndying || isInvul() || isAffected(EffectFlag.IGNORE_DEATH) || isInsideZone(ZoneId.UNDYING);
	}
	
	/**
	 * Checks if the entity's health points are currently blocked.<br>
	 * This returns {@code true} if the entity is invincible or has the {@code HP_BLOCK} effect.
	 * @return {@code true} if health is blocked, {@code false} otherwise.
	 */
	public boolean isHpBlocked()
	{
		return isInvul() || isAffected(EffectFlag.HP_BLOCK);
	}
	
	/**
	 * Checks if the creature's mana pool is currently blocked.<br>
	 * This returns {@code true} if the entity is invincible or has the {@code MP_BLOCK} effect.
	 * @return {@code true} if mana is blocked, {@code false} otherwise.
	 */
	public boolean isMpBlocked()
	{
		return isInvul() || isAffected(EffectFlag.MP_BLOCK);
	}
	
	/**
	 * Checks if the entity currently has a buff block effect.<br>
	 * This method returns {@code true} if the {@code BUFF_BLOCK} flag is active.
	 * @return {@code true} if the buff is blocked, {@code false} otherwise.
	 */
	public boolean isBuffBlocked()
	{
		return isAffected(EffectFlag.BUFF_BLOCK);
	}
	
	/**
	 * Checks if the entity is currently immune to debuffs.<br>
	 * This returns {@code true} if the entity is invincible or has the {@code DEBUFF_BLOCK} flag.
	 * @return {@code true} if debuffs are blocked, {@code false} otherwise.
	 */
	public boolean isDebuffBlocked()
	{
		return isInvul() || isAffected(EffectFlag.DEBUFF_BLOCK);
	}
	
	/**
	 * Checks if the current creature is an undead type.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the creature is undead, otherwise {@code false}.
	 */
	public boolean isUndead()
	{
		return false;
	}
	
	/**
	 * Checks if the entity is currently prevented from being resurrected.<br>
	 * This method returns {@code true} if the {@code BLOCK_RESURRECTION} flag is active.
	 * @return {@code true} if resurrection is blocked, {@code false} otherwise.
	 */
	public boolean isResurrectionBlocked()
	{
		return isAffected(EffectFlag.BLOCK_RESURRECTION);
	}
	
	/**
	 * Checks if the current entity is currently in a flying state.
	 * @return {@code true} if the entity is flying, otherwise {@code false}.
	 */
	public boolean isFlying()
	{
		return _isFlying;
	}
	
	/**
	 * Sets whether the entity is currently in flight.<br>
	 * Use {@code true} to enable flying and {@code false} to disable it.
	 * @param mode The flight state to apply.
	 */
	public void setFlying(boolean mode)
	{
		_isFlying = mode;
	}
	
	/**
	 * Retrieves the current statistics of the creature.<br>
	 * This method returns the {@code CreatureStat} object associated with this entity.
	 * @return the {@code CreatureStat} of the creature.
	 */
	public CreatureStat getStat()
	{
		return _stat;
	}
	
	/**
	 * Initializes the {@code _stat} object for this creature.<br>
	 * This method creates a new instance of {@code CreatureStat}.
	 */
	public void initCharStat()
	{
		_stat = new CreatureStat(this);
	}
	
	/**
	 * Updates the current stat of the creature.<br>
	 * This method replaces the existing {@code _stat} with a new {@code CreatureStat}.
	 * @param value The new {@code CreatureStat} to assign.
	 */
	public void setStat(CreatureStat value)
	{
		_stat = value;
	}
	
	/**
	 * Retrieves the current status of the creature.<br>
	 * This method returns the {@code CreatureStatus} object associated with this entity.
	 * @return the current {@code CreatureStatus}
	 */
	public CreatureStatus getStatus()
	{
		return _status;
	}
	
	/**
	 * Initializes the {@code _status} field for this creature.<br>
	 * This method creates a new instance of {@code CreatureStatus}.
	 */
	public void initCharStatus()
	{
		_status = new CreatureStatus(this);
	}
	
	/**
	 * Updates the current status of the creature.<br>
	 * This method sets the {@code _status} field to the provided value.
	 * @param value The new {@code CreatureStatus} to assign.
	 */
	public void setStatus(CreatureStatus value)
	{
		_status = value;
	}
	
	/**
	 * Retrieves the template associated with this creature.<br>
	 * This provides access to the base data for the entity.
	 * @return the {@code CreatureTemplate} of this creature.
	 */
	public CreatureTemplate getTemplate()
	{
		return _template;
	}
	
	/**
	 * Sets the {@code CreatureTemplate} for this creature.<br>
	 * This method updates the internal template reference.
	 * @param template The {@code CreatureTemplate} to assign.
	 */
	protected void setTemplate(CreatureTemplate template)
	{
		_template = template;
	}
	
	/**
	 * Retrieves the display title for this entity.<br>
	 * This method handles logic for monster levels, aggression tags, and champion titles.<br>
	 * It also returns the owner's name if the entity is a trap.
	 * @return The formatted title string or an empty string if no title exists.
	 */
	public String getTitle()
	{
		// Custom level titles
		if (isMonster() && (NpcConfig.SHOW_NPC_LEVEL || NpcConfig.SHOW_NPC_AGGRESSION))
		{
			String t1 = "";
			if (NpcConfig.SHOW_NPC_LEVEL)
			{
				t1 += "Lv " + getLevel();
			}
			
			String t2 = "";
			if (NpcConfig.SHOW_NPC_AGGRESSION)
			{
				if (!t1.isEmpty())
				{
					t2 += " ";
				}
				
				final Monster monster = asMonster();
				if (monster.isAggressive())
				{
					t2 += "[A]"; // Aggressive.
				}
				
				final NpcTemplate template = monster.getTemplate();
				if ((template.getClans() != null) && (template.getClanHelpRange() > 0))
				{
					t2 += "[G]"; // Group.
				}
			}
			
			t1 += t2;
			if ((_title != null) && !_title.isEmpty())
			{
				t1 += " " + _title;
			}
			
			return isChampion() ? ChampionMonstersConfig.CHAMP_TITLE + " " + t1 : t1;
		}
		
		// Champion titles
		if (isChampion())
		{
			return ChampionMonstersConfig.CHAMP_TITLE;
		}
		
		// Set trap title
		if (isTrap())
		{
			final Player owner = ((Trap) this).getOwner();
			if (owner != null)
			{
				_title = owner.getName();
			}
		}
		
		return _title != null ? _title : "";
	}
	
	/**
	 * Sets the title for this creature.<br>
	 * If the provided {@code value} is {@code null}, it sets the title to an empty string.<br>
	 * For players, the title is truncated to 20 characters if it exceeds that length.
	 * @param value The new title to set.
	 */
	public void setTitle(String value)
	{
		if (value == null)
		{
			_title = "";
		}
		else
		{
			_title = isPlayer() && (value.length() > 21) ? value.substring(0, 20) : value;
		}
	}
	
	/**
	 * Sets the movement state of the creature to walking.<br>
	 * This method stops the running state and enables walking behavior.
	 */
	public void setWalking()
	{
		setRunning(false);
	}
	
	/**
	 * Initiates the fake death state for a player.<br>
	 * This method stops all current actions like attacking, casting, and moving.<br>
	 * It also notifies the AI system and broadcasts the status change to other players.
	 */
	public void startFakeDeath()
	{
		if (!isPlayer())
		{
			return;
		}
		
		// Aborts any attacks/casts if fake dead.
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyActionFakeDeath();
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
		
		// Remove target from those that have the untargetable creature on target.
		if (PlayerConfig.FAKE_DEATH_UNTARGET)
		{
			World.forEachVisibleObject(this, Creature.class, c ->
			{
				if (c.getTarget() == this)
				{
					c.setTarget(null);
				}
			});
		}
	}
	
	/**
	 * Starts the paralyze effect on the entity.<br>
	 * This method cancels any active attacks or casts.<br>
	 * It also stops movement and notifies the {@code AI} that actions are blocked.
	 */
	public void startParalyze()
	{
		// Aborts any attacks/casts if paralyzed.
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyActionBlocked();
	}
	
	/**
	 * Stops all active effects currently applied to this entity.<br>
	 * This method clears the entire effect list immediately.
	 */
	public void stopAllEffects()
	{
		_effectList.stopAllEffects(true);
	}
	
	/**
	 * Stops all active effects on the entity.<br>
	 * Effects that are marked to last through death will remain active.
	 */
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		_effectList.stopAllEffectsExceptThoseThatLastThroughDeath();
	}
	
	/**
	 * Stops specific skill effects for the actor.<br>
	 * This method removes active effects based on a type and ID.
	 * @param type The {@code SkillFinishType} of the effect to stop.
	 * @param skillId The unique identifier for the skill.
	 */
	public void stopSkillEffects(SkillFinishType type, int skillId)
	{
		_effectList.stopSkillEffects(type, skillId);
	}
	
	/**
	 * Stops all active effects for a specific skill.<br>
	 * This method removes the skill from the current effect list.
	 * @param skill The {@code Skill} object to stop.
	 */
	public void stopSkillEffects(Skill skill)
	{
		_effectList.stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
	}
	
	/**
	 * Stops all active effects that match the specified flag.<br>
	 * This method updates the internal effect list of the actor.
	 * @param effectFlag The {@code EffectFlag} to identify which effects should be stopped.
	 */
	public void stopEffects(EffectFlag effectFlag)
	{
		_effectList.stopEffects(effectFlag);
	}
	
	/**
	 * Stops all active effects that are tied to a specific action.<br>
	 * This method clears the current state of ongoing actions for the entity.
	 */
	public void stopEffectsOnAction()
	{
		_effectList.stopEffectsOnAction();
	}
	
	/**
	 * Stops all active effects that are triggered by taking damage.<br>
	 * This method updates the internal {@code EffectList}.
	 */
	public void stopEffectsOnDamage()
	{
		_effectList.stopEffectsOnDamage();
	}
	
	/**
	 * Stops the fake death state for this entity.<br>
	 * If {@code removeEffects} is {@code true}, it also removes the fake death effects.<br>
	 * This method updates the character status and broadcasts a revive packet.
	 * @param removeEffects Determines whether to clear the fake death visual effects.
	 */
	public void stopFakeDeath(boolean removeEffects)
	{
		if (removeEffects)
		{
			stopEffects(EffectFlag.FAKE_DEATH);
		}
		
		// If this is a player instance, start the grace period for this character (grace from mobs only)!
		if (isPlayer())
		{
			asPlayer().setRecentFakeDeath(true);
		}
		
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH));
		
		// TODO: Temporary hack where players see a fall damage (FD) state on moving targets; teleporting to an FD user causes them to fall again for that client even if they are standing, possibly due to incorrect data in the CharInfo packet.
		broadcastPacket(new Revive(this));
	}
	
	/**
	 * Stops the stunning state for the entity.<br>
	 * If {@code removeEffects} is {@code true}, it also removes stun effects from the effect list.<br>
	 * Notifies the AI to think if the entity is not a player.
	 * @param removeEffects Determines whether to remove active stun effects.
	 */
	public void stopStunning(boolean removeEffects)
	{
		if (removeEffects)
		{
			_effectList.stopEffects(AbnormalType.STUN);
		}
		
		if (!isPlayer())
		{
			getAI().notifyActionThink();
		}
	}
	
	/**
	 * Stops the current transformation of the entity.<br>
	 * This method handles visual updates and AI notifications.
	 * @param removeEffects Set to {@code true} to also remove related status effects.
	 */
	public void stopTransformation(boolean removeEffects)
	{
		if (removeEffects && !_effectList.stopEffects(AbnormalType.TRANSFORM))
		{
			_effectList.stopEffects(AbnormalType.CHANGEBODY);
		}
		
		if (_transform != null)
		{
			untransform();
		}
		
		if (!isPlayer())
		{
			getAI().notifyActionThink();
		}
		
		updateAbnormalVisualEffects();
	}
	
	/**
	 * Updates the visual effects for abnormal states.<br>
	 * This method handles the rendering of special effects for entities.
	 */
	public void updateAbnormalVisualEffects()
	{
		// overridden
	}
	
	/**
	 * Updates the visual icons for active effects.<br>
	 * This method refreshes the display of status effects on the entity.<br>
	 * It ensures that all current effects are correctly represented.
	 */
	public void updateEffectIcons()
	{
		updateEffectIcons(false);
	}
	
	/**
	 * Updates the visual icons for active effects. <br>
	 * This method refreshes the display of status effects.
	 * @param partyOnly Set to {@code true} to update only for the current party.
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		// overridden
	}
	
	/**
	 * Checks if the current entity is affected by a specific skill.<br>
	 * This method uses the unique ID of the provided {@code SkillHolder}.
	 * @param skill The {@code SkillHolder} object to check against.
	 * @return {@code true} if the entity is affected, {@code false} otherwise.
	 */
	public boolean isAffectedBySkill(SkillHolder skill)
	{
		return isAffectedBySkill(skill.getSkillId());
	}
	
	/**
	 * Checks if the entity is currently affected by a specific skill.<br>
	 * This method queries the internal effect list for the given {@code skillId}.
	 * @param skillId The unique identifier of the skill to check.
	 * @return {@code true} if the entity is affected, otherwise {@code false}.
	 */
	public boolean isAffectedBySkill(int skillId)
	{
		return _effectList.isAffectedBySkill(skillId);
	}
	
	/**
	 * Retrieves the level of a skill associated with a specific buff.<br>
	 * It checks if the buff exists for the given {@code skillId}.<br>
	 * If no buff is found, it returns {@code 0}.
	 * @param skillId The unique identifier of the skill to check.
	 * @return The level of the skill or {@code 0} if not found.
	 */
	public int getAffectedSkillLevel(int skillId)
	{
		final BuffInfo info = _effectList.getBuffInfoBySkillId(skillId);
		return info == null ? 0 : info.getSkill().getLevel();
	}
	
	/**
	 * This class groups all movement data.
	 */
	public static class MoveData
	{
		// When we retrieve x/y/z we use GameTimeControl.getGameTicks().
		// If we are moving, but move timestamp==gameticks, we don't need to recalculate position.
		public int moveStartTime;
		public int moveTimestamp; // Last movement update.
		public int xDestination;
		public int yDestination;
		public int zDestination;
		public double xAccurate; // Otherwise there would be rounding errors.
		public double yAccurate;
		public double zAccurate;
		public int heading;
		
		public boolean disregardingGeodata;
		public int onGeodataPathIndex;
		public List<GeoLocation> geoPath;
		public int geoPathAccurateTx;
		public int geoPathAccurateTy;
		public int geoPathGtx;
		public int geoPathGty;
		
		public int lastBroadcastTime;
	}
	
	/**
	 * Sends updated statistics to relevant clients.<br>
	 * This method handles broadcasting changes for players, NPCs, and summons.<br>
	 * It also updates movement states if the speed becomes zero.
	 * @param changed A {@code Set} of {@code Stat} objects that have been modified.
	 */
	public void broadcastModifiedStats(Set<Stat> changed)
	{
		if (!isSpawned())
		{
			return;
		}
		
		if ((changed == null) || changed.isEmpty())
		{
			return;
		}
		
		// Don't broadcast modified stats on login.
		final boolean isPlayerLocal = isPlayer();
		if (isPlayerLocal && !asPlayer().isOnline())
		{
			return;
		}
		
		// If this creature was previously moving, but now due to stat change can no longer move, broadcast StopMove packet.
		if (isMoving() && (getMoveSpeed() <= 0))
		{
			stopMove(null);
		}
		
		if (isSummon())
		{
			final Summon summon = asSummon();
			if (summon.getOwner() != null)
			{
				summon.updateAndBroadcastStatus(1);
			}
		}
		else if (isPlayerLocal)
		{
			final Player player = asPlayer();
			final UserInfo info = new UserInfo(player, false);
			info.addComponentType(UserInfoType.SLOTS, UserInfoType.ENCHANTLEVEL);
			
			boolean updateWeight = false;
			for (Stat stat : changed)
			{
				switch (stat)
				{
					case MOVE_SPEED:
					case RUN_SPEED:
					case WALK_SPEED:
					case SWIM_RUN_SPEED:
					case SWIM_WALK_SPEED:
					case FLY_RUN_SPEED:
					case FLY_WALK_SPEED:
					{
						info.addComponentType(UserInfoType.MULTIPLIER);
						break;
					}
					case PHYSICAL_ATTACK_SPEED:
					{
						info.addComponentType(UserInfoType.MULTIPLIER, UserInfoType.STATS);
						break;
					}
					case PHYSICAL_ATTACK:
					case PHYSICAL_DEFENCE:
					case EVASION_RATE:
					case ACCURACY_COMBAT:
					case CRITICAL_RATE:
					case CRITICAL_RATE_SKILL:
					case MAGIC_CRITICAL_RATE:
					case MAGIC_EVASION_RATE:
					case ACCURACY_MAGIC:
					case MAGIC_ATTACK:
					case MAGIC_ATTACK_SPEED:
					case MAGICAL_DEFENCE:
					{
						info.addComponentType(UserInfoType.STATS);
						break;
					}
					case MAX_CP:
					{
						info.addComponentType(UserInfoType.MAX_HPCPMP);
						break;
					}
					case MAX_HP:
					{
						info.addComponentType(UserInfoType.MAX_HPCPMP);
						break;
					}
					case MAX_MP:
					{
						info.addComponentType(UserInfoType.MAX_HPCPMP);
						break;
					}
					case STAT_STR:
					case STAT_CON:
					case STAT_DEX:
					case STAT_INT:
					case STAT_WIT:
					case STAT_MEN:
					{
						info.addComponentType(UserInfoType.BASE_STATS);
						updateWeight = true;
						break;
					}
					case FIRE_RES:
					case WATER_RES:
					case WIND_RES:
					case EARTH_RES:
					case HOLY_RES:
					case DARK_RES:
					{
						info.addComponentType(UserInfoType.ELEMENTALS);
						break;
					}
					case FIRE_POWER:
					case WATER_POWER:
					case WIND_POWER:
					case EARTH_POWER:
					case HOLY_POWER:
					case DARK_POWER:
					{
						info.addComponentType(UserInfoType.ATK_ELEMENTAL);
						break;
					}
					case WEIGHT_LIMIT:
					case WEIGHT_PENALTY:
					{
						updateWeight = true;
						break;
					}
				}
			}
			
			if (updateWeight)
			{
				player.refreshOverloaded(true);
			}
			
			sendPacket(info);
			
			player.broadcastCharInfo();
			
			if (hasServitors() && hasAbnormalType(AbnormalType.ABILITY_CHANGE))
			{
				getServitors().values().forEach(Summon::broadcastStatusUpdate);
			}
		}
		else if (isNpc())
		{
			World.forEachVisibleObject(this, Player.class, player ->
			{
				if (!isVisibleFor(player))
				{
					return;
				}
				
				if (isFakePlayer())
				{
					player.sendPacket(new FakePlayerInfo(asNpc()));
				}
				else if (getRunSpeed() == 0)
				{
					player.sendPacket(new ServerObjectInfo(asNpc(), player));
				}
				else
				{
					player.sendPacket(new NpcInfo(asNpc()));
				}
			});
		}
	}
	
	/**
	 * Retrieves the destination X coordinate of the actor.<br>
	 * If a movement is in progress, it returns the target X value from {@code MoveData}.<br>
	 * Otherwise, it returns the current X position.
	 * @return The destination or current X coordinate as an {@code int}.
	 */
	public int getXdestination()
	{
		final MoveData move = _move;
		if (move != null)
		{
			return move.xDestination;
		}
		
		return getX();
	}
	
	/**
	 * Retrieves the destination Y coordinate of the actor.<br>
	 * If a movement is in progress, it returns the target Y value from {@code MoveData}.<br>
	 * Otherwise, it returns the current Y position.
	 * @return The destination or current Y coordinate as an {@code int}.
	 */
	public int getYdestination()
	{
		final MoveData move = _move;
		if (move != null)
		{
			return move.yDestination;
		}
		
		return getY();
	}
	
	/**
	 * Retrieves the destination Z coordinate for movement.<br>
	 * If no active move data exists, it returns the current Z coordinate.
	 * @return the destination or current Z coordinate as an {@code int}.
	 */
	public int getZdestination()
	{
		final MoveData move = _move;
		if (move != null)
		{
			return move.zDestination;
		}
		
		return getZ();
	}
	
	/**
	 * Checks if the creature is currently in a state of combat.<br>
	 * This method returns {@code true} if the entity has an active AI and is auto-attacking.
	 * @return {@code true} if the entity is attacking, {@code false} otherwise.
	 */
	public boolean isInCombat()
	{
		return hasAI() && getAI().isAutoAttacking();
	}
	
	/**
	 * Checks if the entity is currently in a state of movement.<br>
	 * It returns {@code true} if the internal move object is not {@code null}.
	 * @return {@code true} if moving, {@code false} otherwise.
	 */
	public boolean isMoving()
	{
		return _move != null;
	}
	
	/**
	 * Checks if the current movement is following a predefined geodata path.<br>
	 * This method verifies the {@code MoveData} to see if it aligns with specific paths.
	 * @return {@code true} if the move is on a geodata path, {@code false} otherwise.
	 */
	public boolean isOnGeodataPath()
	{
		final MoveData move = _move;
		if (move == null)
		{
			return false;
		}
		
		return isOnGeodataPath(move);
	}
	
	/**
	 * Checks if the movement is currently on a valid geodata path.<br>
	 * It returns {@code false} if the index is invalid or at the end of the path.
	 * @param move The {@code MoveData} object containing the current path information.
	 * @return {@code true} if the movement is still on the path, {@code false} otherwise.
	 */
	public boolean isOnGeodataPath(MoveData move)
	{
		if (move.onGeodataPathIndex == -1)
		{
			return false;
		}
		
		if (move.onGeodataPathIndex == (move.geoPath.size() - 1))
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Retrieves the current geographic path for movement.<br>
	 * This method returns the path data if a move is currently in progress.<br>
	 * It returns {@code null} if no move is active.
	 * @return A {@code List} of {@code GeoLocation} objects representing the path, or {@code null}.
	 */
	public List<GeoLocation> getGeoPath()
	{
		final MoveData move = _move;
		if (move != null)
		{
			return move.geoPath;
		}
		
		return null;
	}
	
	/**
	 * Checks if the entity is currently casting a skill.<br>
	 * It returns {@code true} if there are any active skill casters.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if casting is in progress, {@code false} otherwise.
	 */
	public boolean isCastingNow()
	{
		return !_skillCasters.isEmpty();
	}
	
	/**
	 * Checks if the actor is currently casting a specific skill type.<br>
	 * It looks for the {@code skillCastingType} in the active casting set.
	 * @param skillCastingType The type of skill to check.
	 * @return {@code true} if the actor is casting this skill, otherwise {@code false}.
	 */
	public boolean isCastingNow(SkillCastingType skillCastingType)
	{
		return _skillCasters.containsKey(skillCastingType);
	}
	
	/**
	 * Checks if any active skill caster matches the provided criteria.<br>
	 * It iterates through all current {@code SkillCaster} objects.
	 * @param filter A {@code Predicate} used to test each {@code SkillCaster}.
	 * @return {@code true} if at least one caster matches the filter, otherwise {@code false}.
	 */
	public boolean isCastingNow(Predicate<SkillCaster> filter)
	{
		for (SkillCaster skillCaster : _skillCasters.values())
		{
			if (filter.test(skillCaster))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the entity is currently performing any offensive action.<br>
	 * This includes normal attacks, ranged attacks, or casting spells.
	 * @return {@code true} if the entity is attacking or casting, {@code false} otherwise.
	 */
	public boolean isAttackingOrCastingNow()
	{
		return isAttackingNow() || isRangeAttackingNow() || isCastingNow();
	}
	
	/**
	 * Checks if the entity is currently in an attacking state.<br>
	 * It compares the current system time against the stored attack end time.
	 * @return {@code true} if the attack has not finished yet, {@code false} otherwise.
	 */
	public boolean isAttackingNow()
	{
		return _attackEndTime > System.nanoTime();
	}
	
	/**
	 * Checks if the entity is currently allowed to perform ranged attacks.<br>
	 * It compares the current system time against the disabled end time.
	 * @return {@code true} if ranged attacking is enabled, {@code false} otherwise.
	 */
	public boolean isRangeAttackingNow()
	{
		return _disableRangedAttackEndTime > System.nanoTime();
	}
	
	/**
	 * Stops the current attack action for this creature.<br>
	 * This method checks if an attack is currently in progress.<br>
	 * If true, it calls {@code CreatureAttackTaskManager#abortAttack} and sends a failure packet.
	 */
	public void abortAttack()
	{
		if (isAttackingNow())
		{
			CreatureAttackTaskManager.getInstance().abortAttack(this);
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Stops all active skill casting for this entity.<br>
	 * It iterates through every {@code SkillCaster} and calls {@code stopCasting}.<br>
	 * If the entity is a player, it also clears any queued skills.
	 */
	public void abortAllSkillCasters()
	{
		for (SkillCaster skillCaster : getSkillCasters())
		{
			skillCaster.stopCasting(true);
			if (isPlayer())
			{
				asPlayer().setQueuedSkill(null, null, false, false);
			}
		}
	}
	
	/**
	 * Checks if the current cast should be cancelled.<br>
	 * This method determines if a skill is of a normal type.
	 * @return {@code true} if the cast is aborted, {@code false} otherwise.
	 */
	public boolean abortCast()
	{
		return abortCast(SkillCaster::isAnyNormalType);
	}
	
	/**
	 * Attempts to interrupt the skill casting of a target. <br>
	 * It searches for a {@code SkillCaster} that matches the provided filter.<br>
	 * If found, it stops the cast and clears any queued skills if the caster is a player.
	 * @param filter A {@code Predicate} used to identify the specific {@code SkillCaster} to interrupt.
	 * @return {@code true} if a valid {@code SkillCaster} was found and interrupted, otherwise {@code false}.
	 */
	public boolean abortCast(Predicate<SkillCaster> filter)
	{
		final SkillCaster skillCaster = getSkillCaster(SkillCaster::canAbortCast, filter);
		if (skillCaster != null)
		{
			skillCaster.stopCasting(true);
			if (isPlayer())
			{
				asPlayer().setQueuedSkill(null, null, false, false);
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Updates the current position of the creature based on its movement data.<br>
	 * This method calculates the new coordinates and handles collision checks with obstacles.<br>
	 * It also manages AI intentions and teleports the entity if it moves between ground layers.
	 * @return {@code true} if the creature has arrived at its destination or was stopped by an obstacle, otherwise {@code false}.
	 */
	public boolean updatePosition()
	{
		if (!isSpawned())
		{
			_move = null;
			return true;
		}
		
		// Get movement data
		final MoveData move = _move;
		if (move == null)
		{
			return true;
		}
		
		// Check if this is the first update.
		if (move.moveTimestamp == 0)
		{
			move.moveTimestamp = move.moveStartTime;
			move.xAccurate = getX();
			move.yAccurate = getY();
		}
		
		// Check if the position has already been calculated.
		final int gameTicks = GameTimeTaskManager.getInstance().getGameTicks();
		if (move.moveTimestamp == gameTicks)
		{
			return false;
		}
		
		final int zPrev = getZ(); // The z coordinate may be modified by coordinate synchronizations.
		double dx = move.xDestination - move.xAccurate;
		double dy = move.yDestination - move.yAccurate;
		double dz = move.zDestination - zPrev; // Z coordinate will follow client values.
		
		// Distance from destination.
		double delta = (dx * dx) + (dy * dy);
		final boolean isFloating = _isFlying || (isInsideZone(ZoneId.WATER) && !isInsideZone(ZoneId.CASTLE));
		if (!isFloating && (delta < 10000) && ((dz * dz) > 2500)) // Close enough, allows error between client and server geodata if it cannot be avoided.
		{
			delta = Math.sqrt(delta);
		}
		else
		{
			delta = Math.sqrt(delta + (dz * dz));
		}
		
		// Target collision should be subtracted from current distance.
		final double collision;
		final WorldObject target = _target;
		if ((target != null) && target.isCreature() && hasAI() && (getAI().getIntention() == Intention.ATTACK))
		{
			collision = target.asCreature().getCollisionRadius();
		}
		else
		{
			collision = getCollisionRadius();
		}
		
		delta = Math.max(0.00001, delta - collision);
		
		double distFraction = Double.MAX_VALUE;
		if (delta > 1)
		{
			final double distPassed = (_stat.getMoveSpeed() * (gameTicks - move.moveTimestamp)) / GameTimeTaskManager.TICKS_PER_SECOND;
			distFraction = distPassed / delta;
		}
		
		final boolean arrived = distFraction > 1;
		if (arrived)
		{
			// Set the position of the Creature to the destination.
			super.setXYZ(move.xDestination, move.yDestination, move.zDestination);
		}
		else
		{
			final int newZ = zPrev + (int) ((dz * distFraction) + 0.5);
			move.xAccurate += dx * distFraction;
			move.yAccurate += dy * distFraction;
			
			// Prevent attackables teleporting to another ground layer while moving.
			if (isAttackable() && !isFloating && (Math.abs(newZ - zPrev) > 300))
			{
				final Spawn spawn = asAttackable().getSpawn();
				if (spawn != null)
				{
					teleToLocation(spawn, getInstanceWorld());
					getAttackByList().clear();
					asAttackable().clearAggroList();
					getAI().setIntentionIdle();
				}
				
				return true;
			}
			
			// Set the position of the Creature to estimated after parcial move.
			super.setXYZ((int) move.xAccurate, (int) move.yAccurate, newZ);
		}
		
		revalidateZone(false);
		
		// Set the timer of last position update to now.
		move.moveTimestamp = gameTicks;
		
		// Broadcast MoveToLocation on arrived.
		if (arrived && !isOnGeodataPath())
		{
			broadcastMoveToLocation(true);
		}
		else if (isAttackable() && (target != null)) // Attackable with target.
		{
			broadcastMoveToLocation();
		}
		
		return arrived;
	}
	
	/**
	 * Revalidates the remaining movement against the doors and fences of the surroundings.<br>
	 * Pathfinding routes around them, but a direct move can still be aimed through one, and these objects are only known once their region is reached.<br>
	 * The destination is shortened to the last free point, so the creature walks up to the obstacle and stops in front of it.
	 */
	public void revalidateMoveDestination()
	{
		final MoveData move = _move;
		if (move == null)
		{
			return;
		}
		
		final int curX = getX();
		final int curY = getY();
		final int curZ = getZ();
		
		// A straight segment check cannot see a door standing uphill, its interpolated height passes below the collision band of the door.<br>
		// Pathfinding walks cell by cell with the height of each cell, so it is asked instead.
		// The target is held within reach of the path buffers, so that a path ending short means an obstacle and not merely a target too far away.
		final int dx = move.xDestination - curX;
		final int dy = move.yDestination - curY;
		final double distance = Math.sqrt((dx * dx) + (dy * dy));
		final int reach = PathFinding.getInstance().getMaxPathDistance();
		final int targetX;
		final int targetY;
		final int targetZ;
		if (distance > reach)
		{
			final double ratio = reach / distance;
			targetX = curX + (int) (dx * ratio);
			targetY = curY + (int) (dy * ratio);
			targetZ = curZ + (int) ((move.zDestination - curZ) * ratio);
		}
		else
		{
			targetX = move.xDestination;
			targetY = move.yDestination;
			targetZ = move.zDestination;
		}
		
		final List<GeoLocation> path = PathFinding.getInstance().findPath(curX, curY, curZ, targetX, targetY, targetZ, getInstanceWorld(), isPlayable());
		if ((path == null) || path.isEmpty())
		{
			return;
		}
		
		// A path reaching its target lands on the target cell, while a blocked one stops far short of it.
		final GeoLocation lastPoint = path.get(path.size() - 1);
		final int x = lastPoint.getX();
		final int y = lastPoint.getY();
		if ((Math.abs(x - targetX) <= PATH_ENDPOINT_TOLERANCE) && (Math.abs(y - targetY) <= PATH_ENDPOINT_TOLERANCE))
		{
			return;
		}
		
		// The path stops short, so the movement ends at the furthest reachable point.
		move.onGeodataPathIndex = -1; // Set not on geodata path.
		if ((x == curX) && (y == curY))
		{
			// Standing in front of the obstacle already, there is nothing left to walk.
			if (hasAI())
			{
				if (getAI().isFollowing())
				{
					getAI().stopFollow();
				}
				
				getAI().setIntentionIdle();
			}
			
			stopMove(null);
			return;
		}
		
		move.xDestination = x;
		move.yDestination = y;
		move.zDestination = lastPoint.getZ();
		broadcastMoveToLocation(true);
	}
	
	/**
	 * Updates the current zone status for this entity.<br>
	 * This method checks if the entity has moved far enough to require a new zone check.<br>
	 * If {@code force} is {@code true}, it skips the distance check and validates immediately.
	 * @param force Set to {@code true} to bypass the distance threshold check.
	 */
	public void revalidateZone(boolean force)
	{
		// This function is called too often from movement code.
		if (!force && (calculateDistance3D(_lastZoneValidateLocation) < (isNpc() && !isInCombat() ? NpcConfig.MAX_DRIFT_RANGE : 100)))
		{
			return;
		}
		
		_lastZoneValidateLocation.setXYZ(this);
		
		final ZoneRegion region = ZoneManager.getInstance().getRegion(this);
		if (region != null)
		{
			region.revalidateZones(this);
		}
		else // Precaution. Moved at invalid region?
		{
			World.disposeOutOfBoundsObject(this);
		}
	}
	
	/**
	 * Stops the movement of the creature.<br>
	 * This method clears the current movement data.<br>
	 * It updates the position and heading based on the provided {@code Location}.<br>
	 * A {@code StopMove} packet is sent to all nearby clients.
	 * @param loc The new {@code Location} to set for the creature.
	 */
	public void stopMove(Location loc)
	{
		// Delete movement data of the Creature.
		_move = null;
		
		// All data are contained in a Location object.
		if (loc != null)
		{
			setXYZ(loc.getX(), loc.getY(), loc.getZ());
			setHeading(loc.getHeading());
			revalidateZone(true);
		}
		
		broadcastPacket(new StopMove(this));
	}
	
	/**
	 * Checks if the summon animation should be displayed.
	 * @return {@code true} if the animation is enabled, {@code false} otherwise.
	 */
	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}
	
	/**
	 * Sets whether the summon animation should be displayed.<br>
	 * This updates the internal state for summoning effects.
	 * @param showSummonAnimation The boolean value to set for showing the animation.
	 */
	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}
	
	/**
	 * Sets the current target for this entity.<br>
	 * If the provided {@code WorldObject} is {@code null} or not spawned, the target is cleared.
	 * @param object The {@code WorldObject} to set as the new target.
	 */
	public void setTarget(WorldObject object)
	{
		if ((object != null) && !object.isSpawned())
		{
			_target = null;
			return;
		}
		
		_target = object;
	}
	
	/**
	 * Retrieves the unique identifier of the current target.<br>
	 * If there is no active target, it returns {@code 0}.
	 * @return The {@code int} object ID of the target or {@code 0} if null.
	 */
	public int getTargetId()
	{
		if (_target != null)
		{
			return _target.getObjectId();
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the current target of this actor.
	 * @return the {@code WorldObject} that is currently being targeted, or {@code null} if there is no target.
	 */
	public WorldObject getTarget()
	{
		return _target;
	}
	
	// Called from AIAccessor only.
	
	/**
	 * Moves the creature to a specific set of coordinates.<br>
	 * This method calculates the path and handles movement logic including water, flying, and geodata checks.<br>
	 * It also manages movement offsets and notifies the AI when the destination is reached.
	 * @param xValue The target X coordinate.
	 * @param yValue The target Y coordinate.
	 * @param zValue The target Z coordinate.
	 * @param offsetValue The distance to stop from the target location.
	 */
	public void moveToLocation(int xValue, int yValue, int zValue, int offsetValue)
	{
		// Get the Move Speed of the Creature.
		final double speed = _stat.getMoveSpeed();
		if ((speed <= 0) || isMovementDisabled())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		int x = xValue;
		int y = yValue;
		int z = zValue;
		int offset = offsetValue;
		
		// Get current position of the Creature.
		final int curX = getX();
		final int curY = getY();
		final int curZ = getZ();
		
		// Calculate distance (dx,dy) between current position and destination.
		// TODO: improve Z axis move/follow support when dx,dy are small compared to dz
		double dx = (x - curX);
		double dy = (y - curY);
		double dz = (z - curZ);
		double distance = Math.sqrt((dx * dx) + (dy * dy));
		
		final boolean verticalMovementOnly = _isFlying && (distance == 0) && (dz != 0);
		if (verticalMovementOnly)
		{
			distance = Math.abs(dz);
		}
		
		// Make water move short and use no geodata checks for swimming chars distance in a click can easily be over 3000.
		final boolean isInWater = isInsideZone(ZoneId.WATER) && !isInsideZone(ZoneId.CASTLE);
		if (isInWater && (distance > 700))
		{
			final double divider = 700 / distance;
			x = curX + (int) (divider * dx);
			y = curY + (int) (divider * dy);
			z = curZ + (int) (divider * dz);
			dx = (x - curX);
			dy = (y - curY);
			dz = (z - curZ);
			distance = Math.sqrt((dx * dx) + (dy * dy));
		}
		
		// @formatter:off
		// Define movement angles needed.
		// ^
		// |    X (x,y)
		// |   /
		// |  / distance
		// | /
		// The angle is X.
		// (curx,cury)
		// @formatter:on
		
		double cos;
		double sin;
		
		// Check if a movement offset is defined or no distance to go through.
		if ((offset > 0) || (distance < 1))
		{
			// Approximation for moving closer when Z coordinates are different; TODO: handle Z axis movement better.
			offset -= Math.abs(dz);
			if (offset < 5)
			{
				offset = 5;
			}
			
			// If no distance to go through, the movement is canceled.
			if ((distance < 1) || ((distance - offset) <= 0))
			{
				// Notify the AI that the Creature is arrived at destination.
				getAI().notifyActionArrived();
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Calculate movement angles needed.
			sin = dy / distance;
			cos = dx / distance;
			distance -= (offset - 5); // Due to rounding error, we have to move a bit closer to be in range.
			
			// Calculate the new destination with offset included.
			x = curX + (int) (distance * cos);
			y = curY + (int) (distance * sin);
		}
		else
		{
			// Calculate movement angles needed.
			sin = dy / distance;
			cos = dx / distance;
		}
		
		// Create and Init a MoveData object.
		final MoveData move = new MoveData();
		
		// GEODATA MOVEMENT CHECKS AND PATHFINDING
		final WorldRegion region = getWorldRegion();
		move.disregardingGeodata = (region == null) || !region.areNeighborsActive();
		move.onGeodataPathIndex = -1; // Initialize not on geodata path.
		if (!move.disregardingGeodata && !_isFlying && !isInWater && !isVehicle() && !_cursorKeyMovement)
		{
			final boolean isInVehicle = isPlayer() && (asPlayer().getVehicle() != null);
			if (isInVehicle)
			{
				move.disregardingGeodata = true;
			}
			
			// Movement checks.
			if ((GeoEngineConfig.PATHFINDING > 0) && !(this instanceof FriendlyNpc))
			{
				int originalX = x;
				int originalY = y;
				final int originalZ = z;
				final double originalDistance = distance;
				final int gtx = (originalX - World.WORLD_X_MIN) >> 4;
				final int gty = (originalY - World.WORLD_Y_MIN) >> 4;
				final MoveData currentMove = _move;
				if ((currentMove != null) && isOnGeodataPath(currentMove))
				{
					if ((gtx == currentMove.geoPathGtx) && (gty == currentMove.geoPathGty))
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					
					currentMove.onGeodataPathIndex = -1; // Set not on geodata path.
				}
				
				// Support for player attack with direct movement. Tested at retail on May 11th 2023.
				final boolean directMove = isPlayer() && hasAI() && (getAI().getIntention() == Intention.ATTACK);
				
				// Check if destination is a water zone.
				final boolean isDestinationInWater = isPlayer() && (ZoneManager.getInstance().getZone(x, y, z, WaterZone.class) != null);
				
				if (directMove //
					|| (!isInVehicle && !isDestinationInWater // Not in vehicle and destination not in water.
						&& !(isPlayer() && (distance > 3000)) // Should be able to click far away and move.
						&& !(isMonster() && (Math.abs(dz) > 100) && GeoEngine.getInstance().canSeeTarget(this, _target)) // Monsters can move on ledges.
						&& !(((curZ - z) > 300) && (distance < 300)))) // Prohibit correcting destination if character wants to fall.
				{
					// Location is different if destination wasn't reached (or just z coord is different).
					final Location destination = GeoEngine.getInstance().getValidLocation(curX, curY, curZ, x, y, z, getInstanceWorld());
					x = destination.getX();
					y = destination.getY();
					if (!isPlayer())
					{
						z = destination.getZ();
					}
					
					dx = x - curX;
					dy = y - curY;
					dz = z - curZ;
					distance = verticalMovementOnly ? (dz * dz) : Math.sqrt((dx * dx) + (dy * dy));
				}
				
				// Pathfinding checks.
				if (!directMove && ((originalDistance - distance) > 30) && !isControlBlocked() && !isInVehicle)
				{
					// Path calculation -- overrides previous movement check.
					move.geoPath = PathFinding.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ, getInstanceWorld(), isPlayer());
					final boolean found = (move.geoPath != null) && (move.geoPath.size() > 1);
					
					if (found)
					{
						// A partial path ends at the closest reachable point, so aim for that point instead of the requested target.
						final GeoLocation lastPoint = move.geoPath.get(move.geoPath.size() - 1);
						if ((Math.abs(lastPoint.getX() - originalX) > 16) || (Math.abs(lastPoint.getY() - originalY) > 16))
						{
							originalX = lastPoint.getX();
							originalY = lastPoint.getY();
						}
						
						move.onGeodataPathIndex = 0; // On first segment.
						move.geoPathGtx = gtx;
						move.geoPathGty = gty;
						move.geoPathAccurateTx = originalX;
						move.geoPathAccurateTy = originalY;
						x = move.geoPath.get(move.onGeodataPathIndex).getX();
						y = move.geoPath.get(move.onGeodataPathIndex).getY();
						z = move.geoPath.get(move.onGeodataPathIndex).getZ();
						dx = x - curX;
						dy = y - curY;
						dz = z - curZ;
						distance = verticalMovementOnly ? (dz * dz) : Math.sqrt((dx * dx) + (dy * dy));
						sin = dy / distance;
						cos = dx / distance;
					}
					else // No path found.
					{
						// When no move path was found, use direct movement. Tested at retail on October 21st 2024.
						// if (isPlayer() && !_isFlying && !isInWater)
						// {
						// sendPacket(ActionFailed.STATIC_PACKET);
						// return;
						// }
						
						move.disregardingGeodata = true;
						x = originalX;
						y = originalY;
						z = originalZ;
						distance = originalDistance;
					}
				}
				
				// Verify destination when using mouse movement and no path is found.
				if (isPlayable() && !_cursorKeyMovement && (move.geoPath == null) //
					&& !isInVehicle // Not in vehicle.
					&& !(((curZ - z) > 300) && (distance < 300))) // Prohibit correcting destination if character wants to fall.
				{
					final Location destination = GeoEngine.getInstance().getValidLocation(curX, curY, curZ, x, y, z, getInstanceWorld());
					x = destination.getX();
					y = destination.getY();
					z = destination.getZ();
					dx = x - curX;
					dy = y - curY;
					dz = z - curZ;
					distance = verticalMovementOnly ? (dz * dz) : Math.sqrt((dx * dx) + (dy * dy));
				}
			}
			
			// If no distance to go through, the movement is canceled.
			if ((distance < 1) && ((GeoEngineConfig.PATHFINDING > 0) || isPlayable()))
			{
				// An attack or cast already in flight has to survive this, cancelling it is what strands melee.
				final Intention intention = hasAI() ? getAI().getIntention() : null;
				final boolean pendingTargetAction = (intention == Intention.ATTACK) || (intention == Intention.CAST);
				if (isSummon() && !pendingTargetAction)
				{
					// Do not break following owner.
					if (getAI().getTarget() != asPlayer())
					{
						asSummon().setFollowStatus(false);
						getAI().setIntentionIdle();
					}
				}
				else if (!pendingTargetAction)
				{
					getAI().setIntentionIdle();
				}
				
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// Apply Z distance for flying or swimming for correct timing calculations.
		if ((_isFlying || isInWater) && !verticalMovementOnly)
		{
			distance = Math.sqrt((distance * distance) + (dz * dz));
		}
		
		// Calculate the number of ticks between the current position and the destination.
		final int ticksToMove = (int) ((GameTimeTaskManager.TICKS_PER_SECOND * distance) / speed);
		move.xDestination = x;
		move.yDestination = y;
		move.zDestination = z; // This is what was requested from client.
		
		// Calculate and set the heading of the Creature.
		move.heading = 0; // Initial value for coordinate sync.
		
		// Does not break heading on vertical movements.
		if (!verticalMovementOnly)
		{
			setHeading(LocationUtil.calculateHeadingFrom(cos, sin));
		}
		
		move.moveStartTime = GameTimeTaskManager.getInstance().getGameTicks();
		
		// Set the Creature _move object to MoveData object.
		_move = move;
		
		// Add the Creature to moving objects of the MovementTaskManager.
		// The MovementTaskManager manages object movement.
		MovementTaskManager.getInstance().registerMovingObject(this);
		
		// Create a task to notify the AI that Creature arrives at a check point of the movement.
		if ((ticksToMove * GameTimeTaskManager.MILLIS_IN_TICK) > 3000)
		{
			ThreadPool.schedule(() -> getAI().notifyActionArrivedRevalidate(), 2000);
		}
		
		// The Event.ARRIVED will be sent when the character will actually arrive to destination by MovementTaskManager.
	}
	
	/**
	 * Updates the current movement data to the next point on the path.<br>
	 * Checks if the creature is allowed to move and calculates the new destination.<br>
	 * Registers the actor with the {@code MovementTaskManager} for processing.<br>
	 * Returns {@code true} if the move was successful, otherwise returns {@code false}.
	 * @return {@code true} if the movement was updated successfully; {@code false} otherwise.
	 */
	public boolean moveToNextRoutePoint()
	{
		final MoveData move = _move;
		if (move == null)
		{
			return false;
		}
		
		if (!isOnGeodataPath(move))
		{
			// Cancel the move action.
			_move = null;
			return false;
		}
		
		// Get the Move Speed of the Creature.
		final double speed = _stat.getMoveSpeed();
		if ((speed <= 0) || isMovementDisabled())
		{
			// Cancel the move action.
			_move = null;
			return false;
		}
		
		// Get current position of the Creature.
		final int curX = getX();
		final int curY = getY();
		
		// Create and Init a MoveData object.
		final MoveData newMove = new MoveData();
		
		// Update MoveData object
		newMove.onGeodataPathIndex = move.onGeodataPathIndex + 1; // next segment
		newMove.geoPath = move.geoPath;
		newMove.geoPathGtx = move.geoPathGtx;
		newMove.geoPathGty = move.geoPathGty;
		newMove.geoPathAccurateTx = move.geoPathAccurateTx;
		newMove.geoPathAccurateTy = move.geoPathAccurateTy;
		if (move.onGeodataPathIndex == (move.geoPath.size() - 2))
		{
			newMove.xDestination = move.geoPathAccurateTx;
			newMove.yDestination = move.geoPathAccurateTy;
			newMove.zDestination = move.geoPath.get(newMove.onGeodataPathIndex).getZ();
		}
		else
		{
			newMove.xDestination = move.geoPath.get(newMove.onGeodataPathIndex).getX();
			newMove.yDestination = move.geoPath.get(newMove.onGeodataPathIndex).getY();
			newMove.zDestination = move.geoPath.get(newMove.onGeodataPathIndex).getZ();
		}
		
		// Calculate and set the heading of the Creature.
		final double ndx = newMove.xDestination - curX;
		final double ndy = newMove.yDestination - curY;
		final double distance = Math.sqrt((ndx * ndx) + (ndy * ndy));
		if (distance != 0)
		{
			setHeading(LocationUtil.calculateHeadingFrom(curX, curY, newMove.xDestination, newMove.yDestination));
		}
		
		// Calculate the number of ticks between the current position and the destination.
		final int ticksToMove = (int) ((GameTimeTaskManager.TICKS_PER_SECOND * distance) / speed);
		newMove.heading = 0; // Initial value for coordinate sync.
		newMove.moveStartTime = GameTimeTaskManager.getInstance().getGameTicks();
		
		// Set the Creature _move object to MoveData object.
		_move = newMove;
		
		// Add the Creature to moving objects of the MovementTaskManager.
		// The MovementTaskManager manages object movement.
		MovementTaskManager.getInstance().registerMovingObject(this);
		
		// Create a task to notify the AI that Creature arrives at a check point of the movement.
		if ((ticksToMove * GameTimeTaskManager.MILLIS_IN_TICK) > 3000)
		{
			ThreadPool.schedule(() -> getAI().notifyActionArrivedRevalidate(), 2000);
		}
		
		// The Event.ARRIVED will be sent when the character will actually arrive to destination by MovementTaskManager.
		
		// Send a Server->Client packet MoveToLocation to the actor and all Player in its known players.
		broadcastMoveToLocation(true);
		return true;
	}
	
	/**
	 * Checks if the provided {@code heading} is valid for movement.<br>
	 * This method updates the current move data if the {@code heading} differs from the stored value.<br>
	 * It returns {@code true} if the movement is allowed or if it is an initial value.
	 * @param heading The new direction to validate.
	 * @return {@code true} if the heading is valid, {@code false} otherwise.
	 */
	public boolean validateMovementHeading(int heading)
	{
		final MoveData move = _move;
		if (move == null)
		{
			return true;
		}
		
		boolean result = true;
		if (move.heading != heading)
		{
			result = (move.heading == 0); // initial value or false
			move.heading = heading;
		}
		
		return result;
	}
	
	/**
	 * Checks if a location is within a specific 2D distance.<br>
	 * This method ignores the {@code Z} coordinate during calculation.
	 * @param loc The {@code ILocational} object containing coordinates.
	 * @param radius The maximum distance to check against.
	 * @return {@code true} if the location is within the radius, {@code false} otherwise.
	 */
	public boolean isInsideRadius2D(ILocational loc, int radius)
	{
		return isInsideRadius2D(loc.getX(), loc.getY(), loc.getZ(), radius);
	}
	
	/**
	 * Checks if a point is within a specific distance from the origin.<br>
	 * This method uses 2D coordinates for the calculation.
	 * @param x The x coordinate of the point.
	 * @param y The y coordinate of the point.
	 * @param z The z coordinate of the point.
	 * @param radius The maximum distance to check against.
	 * @return {@code true} if the point is inside the radius, {@code false} otherwise.
	 */
	public boolean isInsideRadius2D(int x, int y, int z, int radius)
	{
		return calculateDistance2D(x, y, z) < radius;
	}
	
	/**
	 * Checks if a location is within a specific 3D distance.<br>
	 * This method calculates the distance between two points in 3D space.<br>
	 * It returns {@code true} if the point is inside the given radius.
	 * @param loc The {@code ILocational} object containing the coordinates.
	 * @param radius The maximum distance to check against.
	 * @return {@code true} if the location is within the radius, otherwise {@code false}.
	 */
	public boolean isInsideRadius3D(ILocational loc, int radius)
	{
		return isInsideRadius3D(loc.getX(), loc.getY(), loc.getZ(), radius);
	}
	
	/**
	 * Checks if a point is within a specific distance from the origin.<br>
	 * This method uses 3D coordinates to perform the calculation.
	 * @param x The x coordinate of the point.
	 * @param y The y coordinate of the point.
	 * @param z The z coordinate of the point.
	 * @param radius The maximum distance allowed.
	 * @return {@code true} if the point is inside the radius, otherwise {@code false}.
	 */
	public boolean isInsideRadius3D(int x, int y, int z, int radius)
	{
		return calculateDistance3D(x, y, z) < radius;
	}
	
	/**
	 * Checks if the actor can use a specific type of ammunition.<br>
	 * This method currently always returns {@code true}.
	 * @param type The {@code EtcItemType} to check.
	 * @return {@code true} if the ammunition is valid for use.
	 */
	protected boolean checkAndEquipAmmunition(EtcItemType type)
	{
		return true;
	}
	
	/**
	 * This is a dummy method for adding experience and skill points.<br>
	 * It is intended to be overridden by {@code Player} or pet classes.<br>
	 * The method does not perform any actions in this base class.
	 * @param addToExp The amount of experience to add.
	 * @param addToSp The amount of skill points to add.
	 */
	public synchronized void addExpAndSp(double addToExp, double addToSp)
	{
		// Dummy method (overridden by players and pets).
	}
	
	/**
	 * <b><u>Overridden in</u>:</b>
	 * <li>Player</li>
	 * @return the active weapon instance (always equipped in the right hand).
	 */
	public abstract Item getActiveWeaponInstance();
	
	/**
	 * <b><u>Overridden in</u>:</b>
	 * <li>Player</li>
	 * @return the active weapon item (always equipped in the right hand).
	 */
	public abstract Weapon getActiveWeaponItem();
	
	/**
	 * <b><u>Overridden in</u>:</b>
	 * <li>Player</li>
	 * @return the secondary weapon instance (always equipped in the left hand).
	 */
	public abstract Item getSecondaryWeaponInstance();
	
	/**
	 * <b><u>Overridden in</u>:</b>
	 * <li>Player</li>
	 * @return the secondary {@link ItemTemplate} item (always equipped in the left hand).
	 */
	public abstract ItemTemplate getSecondaryWeaponItem();
	
	/**
	 * Handles the logic for when an attack hits a target but is not a dual-hit.<br>
	 * This method processes individual hits from an {@code Attack}.<br>
	 * It checks if targets are valid and within range before applying damage.<br>
	 * It also handles specific logic for ranged weapons and visibility.
	 * @param weapon The {@code Weapon} used to perform the attack.
	 * @param attack The {@code Attack} object containing hit data.
	 * @param hitTime The time at which the hit occurred.
	 * @param attackTime The total time of the attack.
	 */
	public void onHitTimeNotDual(Weapon weapon, Attack attack, int hitTime, int attackTime)
	{
		if (_isDead)
		{
			getAI().notifyActionCancel();
			return;
		}
		
		for (Hit hit : attack.getHits())
		{
			final Creature target = hit.getTarget().asCreature();
			if ((target == null) || target.isDead() || !isInSurroundingRegion(target))
			{
				continue;
			}
			
			if (hit.isMiss())
			{
				notifyAttackAvoid(target, false);
			}
			else
			{
				// Avoid arrows dealing damage when the target hides behind something.
				if ((weapon != null) && weapon.getItemType().isRanged() && !GeoEngine.getInstance().canSeeTarget(this, target))
				{
					if (target.isPlayer())
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_AVOIDED_C1_S_ATTACK);
						sm.addString(getName());
						target.sendPacket(sm);
					}
					
					if (isPlayer())
					{
						sendPacket(SystemMessageId.YOU_HAVE_MISSED);
					}
					continue;
				}
				
				onHitTarget(target, weapon, hit);
			}
		}
		
		CreatureAttackTaskManager.getInstance().onAttackFinish(this, attack, attackTime - hitTime);
	}
	
	/**
	 * Handles the logic for the first hit of a dual attack.<br>
	 * It notifies the task manager and processes the primary hit result.<br>
	 * If the target is invalid or out of range, the action is cancelled.
	 * @param weapon The {@code Weapon} used to perform the attack.
	 * @param attack The {@code Attack} object containing hit data.
	 * @param hitTime The time at which the hit occurred.
	 * @param attackTime The total duration of the attack.
	 * @param delayForSecondAttack The delay before the second part of the dual attack.
	 */
	public void onFirstHitTimeForDual(Weapon weapon, Attack attack, int hitTime, int attackTime, int delayForSecondAttack)
	{
		if (_isDead)
		{
			getAI().notifyActionCancel();
			return;
		}
		
		CreatureAttackTaskManager.getInstance().onSecondHitTimeForDual(this, weapon, attack, hitTime, attackTime, delayForSecondAttack);
		
		// First dual attack is the first hit only.
		final Hit hit = attack.getHits().get(0);
		final Creature target = hit.getTarget().asCreature();
		if ((target == null) || target.isDead() || !isInSurroundingRegion(target))
		{
			getAI().notifyActionCancel();
			return;
		}
		
		if (hit.isMiss())
		{
			notifyAttackAvoid(target, false);
		}
		else
		{
			onHitTarget(target, weapon, hit);
		}
	}
	
	/**
	 * Handles the logic for the second hit of a dual attack.<br>
	 * This method processes all hits in an {@code Attack} except for the first one.<br>
	 * It updates the target status and notifies the task manager when finished.
	 * @param weapon The {@code Weapon} used to perform the attack.
	 * @param attack The {@code Attack} object containing hit data.
	 * @param hitTime1 The time of the first hit.
	 * @param hitTime2 The time of the second hit.
	 * @param attackTime The total time for the attack sequence.
	 */
	public void onSecondHitTimeForDual(Weapon weapon, Attack attack, int hitTime1, int hitTime2, int attackTime)
	{
		if (_isDead)
		{
			getAI().notifyActionCancel();
			return;
		}
		
		// Second dual attack is the remaining hits (first hit not included).
		final List<Hit> attackHits = attack.getHits();
		for (int i = 1; i < attackHits.size(); i++)
		{
			final Hit hit = attackHits.get(i);
			final Creature target = hit.getTarget().asCreature();
			if ((target == null) || target.isDead() || !isInSurroundingRegion(target))
			{
				continue;
			}
			
			if (hit.isMiss())
			{
				notifyAttackAvoid(target, false);
			}
			else
			{
				onHitTarget(target, weapon, hit);
			}
		}
		
		CreatureAttackTaskManager.getInstance().onAttackFinish(this, attack, attackTime - (hitTime1 + hitTime2));
	}
	
	/**
	 * Handles the logic when a creature successfully hits a target.<br>
	 * This method reduces the target's health and notifies relevant scripts.<br>
	 * It also checks for triggered skills and applies weapon special effects.<br>
	 * Finally, it handles specific mechanics like cursed weapon effects.
	 * @param target The {@code Creature} being attacked.
	 * @param weapon The {@code Weapon} used to perform the attack.
	 * @param hit The {@code Hit} object containing damage and critical status information.
	 */
	public void onHitTarget(Creature target, Weapon weapon, Hit hit)
	{
		// Reduce targets HP.
		final boolean hitCritical = hit.isCritical();
		doAttack(hit.getDamage(), target, null, false, false, hitCritical, false);
		
		// Notify to scripts when the attack has been done.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_ATTACK, this))
		{
			if (_onCreatureAttack == null)
			{
				_onCreatureAttack = new OnCreatureAttack();
			}
			
			_onCreatureAttack.setAttacker(this);
			_onCreatureAttack.setTarget(target);
			_onCreatureAttack.setSkill(null);
			EventDispatcher.getInstance().notifyEvent(_onCreatureAttack, this);
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_ATTACKED, target))
		{
			if (_onCreatureAttacked == null)
			{
				_onCreatureAttacked = new OnCreatureAttacked();
			}
			
			_onCreatureAttacked.setAttacker(this);
			_onCreatureAttacked.setTarget(target);
			_onCreatureAttacked.setSkill(null);
			EventDispatcher.getInstance().notifyEvent(_onCreatureAttacked, target);
		}
		
		if (!_triggerSkills.isEmpty())
		{
			for (OptionSkillHolder holder : _triggerSkills.values())
			{
				final OptionSkillType skillType = holder.getSkillType();
				if (((!hitCritical && (skillType == OptionSkillType.ATTACK)) || ((skillType == OptionSkillType.CRITICAL) && hitCritical)) && (Rnd.get(100) < holder.getChance()))
				{
					SkillCaster.triggerCast(this, target, holder.getSkill(), null, false);
				}
			}
		}
		
		// Launch weapon Special ability effect if available.
		if (hitCritical && (weapon != null))
		{
			weapon.applyConditionalSkills(this, target, null, ItemSkillType.ON_CRITICAL_SKILL);
		}
		
		// If hit by a cursed weapon, CP is reduced to 0.
		// If a cursed weapon is hit by a Hero, CP is reduced to 0.
		if (isPlayer() && !target.isHpBlocked() && (asPlayer().isCursedWeaponEquipped() || (asPlayer().isHero() && target.isPlayer() && target.asPlayer().isCursedWeaponEquipped())))
		{
			target.setCurrentCp(0);
		}
	}
	
	/**
	 * Handles logic after an {@code Attack} has finished.<br>
	 * This method recharges auto-soulshot tasks if a hit was successful.<br>
	 * It also notifies the AI that the character is ready for the next action.
	 * @param attack The {@code Attack} object that just completed.
	 */
	public void onAttackFinish(Attack attack)
	{
		// Recharge any active auto-soulshot tasks for current creature after the attack has successfully hit.
		for (Hit hit : attack.getHits())
		{
			if (!hit.isMiss())
			{
				rechargeShots(true, false, false);
				break;
			}
		}
		
		// Notify that this character is ready to act for the next attack.
		getAI().notifyActionReadyToAct();
	}
	
	/**
	 * Stops the current attack if the creature is already attacking.<br>
	 * This method calls {@code abortAttack()} to cancel the action.<br>
	 * If the actor is a player, it sends a failure system message.
	 */
	public void breakAttack()
	{
		if (isAttackingNow())
		{
			// Abort the attack of the Creature and send Server->Client ActionFailed packet.
			abortAttack();
			if (isPlayer())
			{
				// Send a system message.
				sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
			}
		}
	}
	
	/**
	 * Interrupts the current magic skill casting process.<br>
	 * This method identifies if a normal type magic skill is being cast.<br>
	 * It stops the casting and sends an interruption message to the player if applicable.
	 */
	public void breakCast()
	{
		// Break only one skill at a time while casting.
		final SkillCaster skillCaster = getSkillCaster(SkillCaster::isAnyNormalType);
		if ((skillCaster != null) && skillCaster.getSkill().isMagic())
		{
			// Abort the cast of the Creature and send Server->Client MagicSkillCanceled/ActionFailed packet.
			skillCaster.stopCasting(true);
			
			if (isPlayer())
			{
				// Send a system message.
				sendPacket(SystemMessageId.YOUR_CASTING_HAS_BEEN_INTERRUPTED);
			}
		}
	}
	
	/**
	 * Handles the logic for a forced attack action initiated by a player.<br>
	 * This method checks if the attack is valid based on zone rules, olympiad status, and target state.<br>
	 * If all conditions are met, it notifies the AI to begin attacking the target.
	 * @param player The {@code Player} who is attempting to perform the forced attack.
	 */
	@Override
	public void onForcedAttack(Player player)
	{
		if (isInsidePeaceZone(player))
		{
			// If Creature or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed.
			player.sendPacket(SystemMessageId.YOU_CANNOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInOlympiadMode())
		{
			final WorldObject playerTarget = player.getTarget();
			if ((playerTarget != null) && playerTarget.isPlayable())
			{
				final Player target = playerTarget.asPlayer();
				if (target.isInOlympiadMode() && (!player.isOlympiadStart() || (player.getOlympiadGameId() != target.getOlympiadGameId())))
				{
					// If Player is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed.
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}
		
		final WorldObject forcedAttackTarget = player.getTarget();
		if ((forcedAttackTarget != null) && !forcedAttackTarget.canBeAttacked() && !player.getAccessLevel().allowPeaceAttack())
		{
			// If target is not attackable, send a Server->Client packet ActionFailed.
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isConfused())
		{
			// If target is confused, send a Server->Client packet ActionFailed.
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// GeoData Los Check or dz > 1000.
		// if (!GeoEngine.getInstance().canSeeTarget(player, this))
		// {
		// player.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
		// player.sendPacket(ActionFailed.STATIC_PACKET);
		// return;
		// }
		
		if (player.getBlockCheckerArena() != -1)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Notify AI with ATTACK.
		player.getAI().setIntentionAttack(this);
	}
	
	/**
	 * Checks if the attacker is currently in a peace zone.<br>
	 * This method determines if combat should be restricted based on location.
	 * @param attacker The {@code WorldObject} performing the attack.
	 * @return {@code true} if the area is a peace zone, {@code false} otherwise.
	 */
	public boolean isInsidePeaceZone(WorldObject attacker)
	{
		return isInsidePeaceZone(attacker, this);
	}
	
	/**
	 * Checks if the attack is allowed based on peace zone rules.<br>
	 * This method verifies if either the {@code attacker} or the {@code target} are in a protected area.<br>
	 * It also considers player reputation and access levels to determine the result.
	 * @param attacker The object initiating the attack.
	 * @param target The object being attacked.
	 * @return {@code true} if the action is permitted within peace zones, {@code false} otherwise.
	 */
	public boolean isInsidePeaceZone(WorldObject attacker, WorldObject target)
	{
		final Instance instanceWorld = getInstanceWorld();
		if ((target == null) || !((target.isPlayable() || target.isFakePlayer()) && attacker.isPlayable()) || ((instanceWorld != null) && instanceWorld.isPvP()))
		{
			return false;
		}
		
		final Player attackerPlayer = attacker.asPlayer();
		if (PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE)
		{
			// Allows red to be attacked and red to attack flagged players.
			final Player targetPlayer = target.asPlayer();
			if ((targetPlayer != null) && (targetPlayer.getReputation() < 0))
			{
				return false;
			}
			
			if ((attackerPlayer != null) && (attackerPlayer.getReputation() < 0) && (targetPlayer != null) && (targetPlayer.getPvpFlag() > 0))
			{
				return false;
			}
		}
		
		if ((attackerPlayer != null) && attackerPlayer.getAccessLevel().allowPeaceAttack())
		{
			return false;
		}
		
		return (target.isInsideZone(ZoneId.PEACE) || attacker.isInsideZone(ZoneId.PEACE) || target.isInsideZone(ZoneId.NO_PVP) || attacker.isInsideZone(ZoneId.NO_PVP));
	}
	
	/**
	 * Checks if the current entity is located in an active region.<br>
	 * This method verifies that the {@code WorldRegion} is not {@code null}.<br>
	 * It also checks if the region's active state is {@code true}.
	 * @return {@code true} if the entity is in an active region, {@code false} otherwise.
	 */
	public boolean isInActiveRegion()
	{
		final WorldRegion region = getWorldRegion();
		return ((region != null) && (region.isActive()));
	}
	
	/**
	 * Checks if the current entity is part of a party.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the entity is in a party, otherwise {@code false}.
	 */
	public boolean isInParty()
	{
		return false;
	}
	
	/**
	 * Retrieves the {@code Party} associated with this creature.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Party} object or {@code null} if no party exists.
	 */
	public Party getParty()
	{
		return null;
	}
	
	/**
	 * Adds a new {@code Skill} to the actor.<br>
	 * If the skill already exists, it replaces the old one and removes its effects.<br>
	 * It also handles special logic for maintaining sublevels during level increases.
	 * @param skill The {@code Skill} object to add.
	 * @return The {@code Skill} that was replaced, or {@code null} if no replacement occurred.
	 */
	public Skill addSkill(Skill skill)
	{
		Skill oldSkill = null;
		Skill newSkill = skill;
		if (newSkill != null)
		{
			// Mobius: Keep sublevel on skill level increase.
			final Integer newSkillId = newSkill.getId();
			final Skill existingSkill = _skills.get(newSkillId);
			if ((existingSkill != null) && (existingSkill.getSubLevel() > 0) && (newSkill.getSubLevel() == 0) && (existingSkill.getLevel() < newSkill.getLevel()))
			{
				newSkill = SkillData.getInstance().getSkill(newSkillId, newSkill.getLevel(), existingSkill.getSubLevel());
			}
			
			// Replace oldSkill by newSkill or Add the newSkill.
			oldSkill = _skills.put(newSkillId, newSkill);
			
			// If an old skill has been replaced, remove all its Func objects.
			if (oldSkill != null)
			{
				// Stop all effects of that skill.
				if (oldSkill.isPassive())
				{
					_effectList.stopSkillEffects(SkillFinishType.REMOVED, oldSkill);
				}
				
				_stat.recalculateStats(true);
			}
			
			if (newSkill.isPassive())
			{
				newSkill.applyEffects(this, this, false, true, false, 0, null);
			}
		}
		
		return oldSkill;
	}
	
	/**
	 * Removes a specific skill from the actor.<br>
	 * This method handles the removal logic based on the provided {@code Skill} object.
	 * @param skill The {@code Skill} object to be removed.
	 * @param cancelEffect Set to {@code true} to stop any active effects of the skill, or {@code false} otherwise.
	 * @return The removed {@code Skill} object, or {@code null} if the input was {@code null}.
	 */
	public Skill removeSkill(Skill skill, boolean cancelEffect)
	{
		return (skill != null) ? removeSkill(skill.getId(), cancelEffect) : null;
	}
	
	/**
	 * Removes a specific skill from the actor based on its unique identifier.<br>
	 * This method searches for the skill matching {@code skillId}.<br>
	 * It returns the removed {@code Skill} object.
	 * @param skillId The unique ID of the skill to be removed.
	 * @return The {@code Skill} object that was removed, or {@code null} if not found.
	 */
	public Skill removeSkill(int skillId)
	{
		return removeSkill(skillId, true);
	}
	
	/**
	 * Removes a specific skill from the creature.<br>
	 * This method also handles stopping active casts and effects based on the provided flags.
	 * @param skillId The unique identifier of the skill to remove.
	 * @param cancelEffect If {@code true}, it will stop all active effects for this skill.
	 * @return The {@code Skill} object that was removed, or {@code null} if no such skill existed.
	 */
	public Skill removeSkill(int skillId, boolean cancelEffect)
	{
		// Remove the skill from the Creature _skills.
		final Skill oldSkill = _skills.remove(skillId);
		
		// Remove all its Func objects from the Creature calculator set.
		if (oldSkill != null)
		{
			// Stop casting if this skill is used right now.
			abortCast(s -> s.getSkill().getId() == skillId);
			
			// Stop effects.
			if (cancelEffect || oldSkill.isToggle() || oldSkill.isPassive())
			{
				stopSkillEffects(SkillFinishType.REMOVED, oldSkill.getId());
				_stat.recalculateStats(true);
			}
		}
		
		return oldSkill;
	}
	
	/**
	 * Retrieves all skills associated with this entity.<br>
	 * This method returns a collection of {@code Skill} objects.
	 * @return A {@code Collection} containing all {@code Skill} instances.
	 */
	public Collection<Skill> getAllSkills()
	{
		return _skills.values();
	}
	
	/**
	 * Removes all skills currently associated with this entity.<br>
	 * If the entity is a player, it uses the {@code Player#removeSkill} method.<br>
	 * Otherwise, it removes each skill using the internal {@code removeSkill} method.
	 */
	public void removeAllSkills()
	{
		if (isPlayer())
		{
			final Player player = asPlayer();
			while (!_skills.isEmpty())
			{
				player.removeSkill(_skills.firstEntry().getValue());
			}
		}
		else
		{
			while (!_skills.isEmpty())
			{
				removeSkill(_skills.firstEntry().getValue(), true);
			}
		}
	}
	
	/**
	 * Retrieves the collection of skills for this entity.
	 * @return a {@code Map} where the key is an {@code Integer} and the value is a {@code Skill}.
	 */
	public Map<Integer, Skill> getSkills()
	{
		return _skills;
	}
	
	/**
	 * Retrieves the level of a specific skill.<br>
	 * This method checks if the actor knows the skill associated with {@code skillId}.<br>
	 * If the skill is not known, it returns {@code 0}.
	 * @param skillId The unique identifier for the skill.
	 * @return The current level of the skill or {@code 0} if unknown.
	 */
	public int getSkillLevel(int skillId)
	{
		final Skill skill = getKnownSkill(skillId);
		return (skill == null) ? 0 : skill.getLevel();
	}
	
	/**
	 * Retrieves a specific skill from the list of known skills.<br>
	 * This method looks up the skill using the provided unique identifier.
	 * @param skillId The unique ID of the skill to retrieve.
	 * @return The {@code Skill} object if found, or {@code null} if it does not exist.
	 */
	public Skill getKnownSkill(int skillId)
	{
		return _skills.get(skillId);
	}
	
	/**
	 * Retrieves the total number of active buffs.<br>
	 * This method calls {@code getBuffCount()} on the internal effect list.
	 * @return The current count of buffs as an {@code int}.
	 */
	public int getBuffCount()
	{
		return _effectList.getBuffCount();
	}
	
	/**
	 * Retrieves the current number of dance effects active on this entity.<br>
	 * This value is fetched from the internal {@code EffectList}.
	 * @return The total count of dance effects as an {@code int}.
	 */
	public int getDanceCount()
	{
		return _effectList.getDanceCount();
	}
	
	// Quest event ON_SPELL_FNISHED
	/**
	 * Notifies the system that a specific skill from a quest has finished.<br>
	 * This method is used to trigger events after a skill execution.
	 * @param skill The {@code Skill} object that was just completed.
	 * @param target The {@code WorldObject} that was targeted by the skill.
	 */
	public void notifyQuestEventSkillFinished(Skill skill, WorldObject target)
	{
	}
	
	/**
	 * Calculates the level modifier for the current entity.<br>
	 * This value depends on whether the entity is currently transformed.<br>
	 * If no transformation exists or it is a stance, it uses a base calculation.<br>
	 * Otherwise, it retrieves the modifier from the active {@code Transform} object.
	 * @return The calculated level modifier as a {@code double}.
	 */
	public double getLevelMod()
	{
		if ((_transform == null) || _transform.isStance())
		{
			// Untested: (lvl + 89 + unk5,5forSkill4.0Else * odyssey_lvl_mod) / 100; odyssey_lvl_mod = (lvl-99) min 0.
			return (getLevel() + 89) / 100d;
		}
		
		return _transform.getLevelMod(this);
	}
	
	private boolean _disabledAI = false;
	
	/**
	 * Retrieves the Player vs Player (PvP) status flag.<br>
	 * This method returns a {@code byte} value representing the current PvP state.
	 * @return The current PvP flag as a {@code byte}.
	 */
	public byte getPvpFlag()
	{
		return 0;
	}
	
	/**
	 * Updates the PvP flag for the character.<br>
	 * This method allows changing whether a player can participate in PvP.
	 * @param value The new value to set for the PvP status.
	 */
	public void updatePvPFlag(int value)
	{
		// Overridden in Player
	}
	
	/**
	 * Calculates a random damage multiplier based on the character's stats.<br>
	 * It uses the {@code RANDOM_DAMAGE} value to determine the range.<br>
	 * The result is returned as a {@code double}.
	 * @return A random multiplier for calculating damage.
	 */
	public double getRandomDamageMultiplier()
	{
		final int random = (int) _stat.getValue(Stat.RANDOM_DAMAGE);
		return (1 + ((double) Rnd.get(-random, random) / 100));
	}
	
	/**
	 * Retrieves the timestamp when the current attack is scheduled to end.
	 * @return The end time of the attack as a {@code long}.
	 */
	public long getAttackEndTime()
	{
		return _attackEndTime;
	}
	
	/**
	 * Retrieves the timestamp when ranged attacks are disabled.<br>
	 * This value is used to determine if an actor can perform long-distance strikes.
	 * @return The end time as a {@code long}.
	 */
	public long getRangedAttackEndTime()
	{
		return _disableRangedAttackEndTime;
	}
	
	/**
	 * Not Implemented.
	 * @return
	 */
	public abstract int getLevel();
	
	/**
	 * Retrieves the current accuracy value of the creature.<br>
	 * This value is used to determine hit success rates in combat.
	 * @return The accuracy as an {@code int}.
	 */
	public int getAccuracy()
	{
		return _stat.getAccuracy();
	}
	
	/**
	 * Retrieves the current magic accuracy of the creature.<br>
	 * This value is used to determine how effectively spells hit a target.
	 * @return The magic accuracy as an {@code int}.
	 */
	public int getMagicAccuracy()
	{
		return _stat.getMagicAccuracy();
	}
	
	/**
	 * Retrieves the current magic evasion rate for this entity.<br>
	 * This value is fetched from the internal {@code _stat} object.
	 * @return The magic evasion rate as an {@code int}.
	 */
	public int getMagicEvasionRate()
	{
		return _stat.getMagicEvasionRate();
	}
	
	/**
	 * Retrieves the current attack speed multiplier.<br>
	 * This value is used to calculate how fast an actor performs attacks.
	 * @return the {@code double} value of the attack speed multiplier.
	 */
	public double getAttackSpeedMultiplier()
	{
		return _stat.getAttackSpeedMultiplier();
	}
	
	/**
	 * Calculates the critical damage value.<br>
	 * This method retrieves the damage based on the provided initial value.
	 * @param init The initial damage value to use for calculation.
	 * @return The calculated critical damage as a {@code double}.
	 */
	public double getCriticalDmg(int init)
	{
		return _stat.getCriticalDmg(init);
	}
	
	/**
	 * Retrieves the current critical hit value.<br>
	 * This method returns the value from the internal {@code _stat} object.
	 * @return The integer value of the critical hit.
	 */
	public int getCriticalHit()
	{
		return _stat.getCriticalHit();
	}
	
	/**
	 * Retrieves the skill critical rate for the player.<br>
	 * This value is obtained from the internal {@code _stat} object.
	 * @return The current skill critical rate as an {@code int}.
	 */
	public int getPSkillCriticalRate()
	{
		return _stat.getPSkillCriticalRate();
	}
	
	/**
	 * Retrieves the current evasion rate of the creature.<br>
	 * This value is fetched from the internal {@code _stat} object.
	 * @return The evasion rate as an {@code int}.
	 */
	public int getEvasionRate()
	{
		return _stat.getEvasionRate();
	}
	
	/**
	 * Calculates the magical attack range for a specific skill.<br>
	 * This method retrieves the distance value from the character's statistics.
	 * @param skill The {@code Skill} object to check the range for.
	 * @return The integer range of the magical attack.
	 */
	public int getMagicalAttackRange(Skill skill)
	{
		return _stat.getMagicalAttackRange(skill);
	}
	
	/**
	 * Retrieves the maximum CP value for this creature.<br>
	 * This value is obtained from the internal {@code _stat} object.
	 * @return The maximum CP as an {@code int}.
	 */
	public int getMaxCp()
	{
		return _stat.getMaxCp();
	}
	
	/**
	 * Retrieves the maximum amount of CP that can be recovered.<br>
	 * This value is fetched from the internal {@code _stat} object.
	 * @return The maximum recoverable CP as an {@code int}.
	 */
	public int getMaxRecoverableCp()
	{
		return _stat.getMaxRecoverableCp();
	}
	
	/**
	 * Retrieves the Magic Attack value of the creature.<br>
	 * This value is obtained from the internal {@code _stat} object.
	 * @return The current magic attack as an {@code int}.
	 */
	public int getMAtk()
	{
		return _stat.getMAtk();
	}
	
	/**
	 * Retrieves the magic attack bonus provided by the weapon.<br>
	 * This value is fetched from the internal {@code _stat} object.
	 * @return The total magic attack bonus as an {@code int}.
	 */
	public int getWeaponBonusMAtk()
	{
		return _stat.getWeaponBonusMAtk();
	}
	
	/**
	 * Retrieves the magic attack speed of the creature.<br>
	 * This value is obtained from the internal {@code _stat} object.
	 * @return The current magic attack speed as an {@code int}.
	 */
	public int getMAtkSpd()
	{
		return _stat.getMAtkSpd();
	}
	
	/**
	 * Retrieves the maximum mana points for this creature.<br>
	 * This value is obtained from the internal {@code _stat} object.
	 * @return The maximum amount of mana points as an {@code int}.
	 */
	public int getMaxMp()
	{
		return _stat.getMaxMp();
	}
	
	/**
	 * Retrieves the maximum amount of MP that can be recovered.<br>
	 * This value is fetched from the internal {@code _stat} object.
	 * @return the maximum recoverable MP as an {@code int}.
	 */
	public int getMaxRecoverableMp()
	{
		return _stat.getMaxRecoverableMp();
	}
	
	/**
	 * Retrieves the maximum health points of the creature.<br>
	 * This value is obtained from the internal statistics object.
	 * @return the maximum health as a {@code long}.
	 */
	public long getMaxHp()
	{
		return _stat.getMaxHp();
	}
	
	/**
	 * Calculates the maximum amount of health points that can be recovered.<br>
	 * This value is retrieved from the internal statistics object.
	 * @return The maximum recoverable {@code long} value for HP.
	 */
	public long getMaxRecoverableHp()
	{
		return _stat.getMaxRecoverableHp();
	}
	
	/**
	 * Retrieves the critical hit rate of the actor.<br>
	 * This value is obtained from the internal {@code _stat} object.
	 * @return The current critical hit rate as an {@code int}.
	 */
	public int getMCriticalHit()
	{
		return _stat.getMCriticalHit();
	}
	
	/**
	 * Retrieves the Magic Defense value of the creature.<br>
	 * This value is obtained from the internal {@code _stat} object.
	 * @return the current magic defense as an {@code int}.
	 */
	public int getMDef()
	{
		return _stat.getMDef();
	}
	
	/**
	 * Retrieves the physical attack value of the creature.<br>
	 * This value is obtained from the internal {@code _stat} object.
	 * @return The current physical attack power as an {@code int}.
	 */
	public int getPAtk()
	{
		return _stat.getPAtk();
	}
	
	/**
	 * Retrieves the weapon bonus attack power.<br>
	 * This value is obtained from the {@code _stat} object.
	 * @return The total weapon bonus attack power as an {@code int}.
	 */
	public int getWeaponBonusPAtk()
	{
		return _stat.getWeaponBonusPAtk();
	}
	
	/**
	 * Retrieves the physical attack speed of the actor.<br>
	 * This value is obtained from the internal {@code _stat} object.
	 * @return The current physical attack speed as an {@code int}.
	 */
	public int getPAtkSpd()
	{
		return _stat.getPAtkSpd();
	}
	
	/**
	 * Retrieves the physical defense value of the creature.<br>
	 * This value is used to calculate damage reduction from physical attacks.
	 * @return The current {@code int} value for physical defense.
	 */
	public int getPDef()
	{
		return _stat.getPDef();
	}
	
	/**
	 * Retrieves the physical attack range of the creature.<br>
	 * This value is obtained from the internal statistics.
	 * @return The physical attack range as an {@code int}.
	 */
	public int getPhysicalAttackRange()
	{
		return _stat.getPhysicalAttackRange();
	}
	
	/**
	 * Retrieves the current movement speed multiplier.<br>
	 * This value is used to scale the base movement speed of the actor.
	 * @return the current movement speed multiplier as a {@code double}.
	 */
	public double getMovementSpeedMultiplier()
	{
		return _stat.getMovementSpeedMultiplier();
	}
	
	/**
	 * Retrieves the current running speed of the creature.<br>
	 * This value is fetched from the internal {@code _stat} object.
	 * @return The run speed as a {@code double}.
	 */
	public double getRunSpeed()
	{
		return _stat.getRunSpeed();
	}
	
	/**
	 * Retrieves the current walking speed of the creature.<br>
	 * This value is fetched from the internal {@code _stat} object.
	 * @return The walk speed as a {@code double}.
	 */
	public double getWalkSpeed()
	{
		return _stat.getWalkSpeed();
	}
	
	/**
	 * Retrieves the current swim and run speed of the creature.<br>
	 * This value is fetched from the internal {@code _stat} object.
	 * @return The speed as a {@code double}.
	 */
	public double getSwimRunSpeed()
	{
		return _stat.getSwimRunSpeed();
	}
	
	/**
	 * Retrieves the movement speed for swimming and walking.<br>
	 * This value is fetched from the internal statistics of the actor.
	 * @return The current swim and walk speed as a {@code double}.
	 */
	public double getSwimWalkSpeed()
	{
		return _stat.getSwimWalkSpeed();
	}
	
	/**
	 * Retrieves the current movement speed of the creature.<br>
	 * This value is fetched from the internal {@code _stat} object.
	 * @return The move speed as a {@code double}.
	 */
	public double getMoveSpeed()
	{
		return _stat.getMoveSpeed();
	}
	
	/**
	 * Retrieves the shield defense value of the creature.<br>
	 * This value is obtained from the internal {@code _stat} object.
	 * @return The current shield defense as an {@code int}.
	 */
	public int getShldDef()
	{
		return _stat.getShldDef();
	}
	
	/**
	 * Retrieves the strength statistic of the creature.<br>
	 * This value is obtained from the internal {@code _stat} object.
	 * @return The current strength value as an {@code int}.
	 */
	public int getSTR()
	{
		return _stat.getSTR();
	}
	
	/**
	 * Retrieves the current dexterity value of the creature.<br>
	 * This method accesses the internal statistics of the entity.
	 * @return The integer value for {@code DEX}.
	 */
	public int getDEX()
	{
		return _stat.getDEX();
	}
	
	/**
	 * Retrieves the current CON statistic of the creature.<br>
	 * This value is fetched from the internal {@code _stat} object.
	 * @return The integer value of the CON stat.
	 */
	public int getCON()
	{
		return _stat.getCON();
	}
	
	/**
	 * Retrieves the integer value of the character's status.<br>
	 * This method accesses the underlying {@code _stat} object.
	 * @return The current integer value for this status.
	 */
	public int getINT()
	{
		return _stat.getINT();
	}
	
	/**
	 * Retrieves the Wisdom value of the creature.<br>
	 * This method returns the current {@code WIT} stat from the internal statistics.
	 * @return The integer value of the Wisdom statistic.
	 */
	public int getWIT()
	{
		return _stat.getWIT();
	}
	
	/**
	 * Retrieves the Maximum Energy value for this creature.<br>
	 * This method accesses the underlying {@code _stat} object to get the value.
	 * @return The maximum energy as an {@code int}.
	 */
	public int getMEN()
	{
		return _stat.getMEN();
	}
	
	/**
	 * Retrieves the luck value of the creature.<br>
	 * This method returns the current {@code LUC} stat from the internal statistics object.
	 * @return The integer value of the luck stat.
	 */
	public int getLUC()
	{
		return _stat.getLUC();
	}
	
	/**
	 * Retrieves the Charisma statistic of the creature.<br>
	 * This value is obtained from the internal {@code _stat} object.
	 * @return The current Charisma value as an {@code int}.
	 */
	public int getCHA()
	{
		return _stat.getCHA();
	}
	
	// Status - NEED TO REMOVE ONCE CREATURESTATUS IS COMPLETE
	/**
	 * Registers a new listener for status changes.<br>
	 * This method adds the provided {@code Creature} to the internal status update list.
	 * @param object The {@code Creature} that will receive status updates.
	 */
	public void addStatusListener(Creature object)
	{
		_status.addStatusListener(object);
	}
	
	/**
	 * Executes an attack on a target creature.<br>
	 * This method calculates final damage based on various modifiers like immobilization and rear attacks.<br>
	 * It also handles mechanics such as counterattacks, life/mana absorption, and damage reflection.<br>
	 * Finally, it applies the damage to the target and may break their current actions.
	 * @param damageValue The base amount of damage to be dealt.
	 * @param target The {@code Creature} receiving the attack.
	 * @param skill The {@code Skill} being used for this attack, or {@code null}.
	 * @param isDOT Whether the damage should be applied as a Damage Over Time effect.
	 * @param directlyToHp Whether the damage should bypass defenses to hit health directly.
	 * @param critical Whether the attack should be treated as a critical hit.
	 * @param reflect Whether the attack should be reflected back at the attacker.
	 */
	public void doAttack(double damageValue, Creature target, Skill skill, boolean isDOT, boolean directlyToHp, boolean critical, boolean reflect)
	{
		// Check if fake players should aggro each other.
		if (isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AGGRO_FPC && target.isFakePlayer())
		{
			return;
		}
		
		// Start attack stance and notify being attacked.
		if (target.hasAI())
		{
			final CreatureAI targetAI = target.getAI();
			targetAI.clientStartAutoAttack();
			targetAI.notifyActionAttacked(this);
		}
		
		getAI().clientStartAutoAttack();
		
		// ImmobileDamageBonus and ImmobileDamageResist effect bonuses.
		double damage = damageValue;
		if (target.hasBlockActions() || target.isRooted() || target.isImmobilized())
		{
			damage *= _stat.getMul(Stat.IMMOBILE_DAMAGE_BONUS, 1);
			damage *= Math.max(0.22, target.getStat().getMul(Stat.IMMOBILE_DAMAGE_RESIST, 1));
		}
		
		if (!reflect && !isDOT)
		{
			// RearDamage effect bonus.
			damage *= _stat.getPositionTypeValue(Stat.REAR_DAMAGE_RATE, Position.getPosition(this, target));
			
			// Counterattacks happen before damage received.
			if (!target.isDead() && (skill != null))
			{
				Formulas.calcCounterAttack(this, target, skill, true);
				
				// Shield Deflect Magic: Reflect all damage on caster.
				if (skill.isMagic() && (target.getStat().getValue(Stat.VENGEANCE_SKILL_MAGIC_DAMAGE, 0) > Rnd.get(100)))
				{
					reduceCurrentHp(damage, target, skill, isDOT, directlyToHp, critical, true);
					return;
				}
			}
		}
		
		// Absorb HP from the damage inflicted.
		final boolean isPvP = isPlayable() && (target.isPlayable() || target.isFakePlayer());
		if ((!isPvP || PvpConfig.VAMPIRIC_ATTACK_AFFECTS_PVP) && !target.isInvul())
		{
			if ((skill == null) || PlayerConfig.VAMPIRIC_ATTACK_WORKS_WITH_SKILLS)
			{
				final double absorbHpPercent = getStat().getValue(Stat.ABSORB_DAMAGE_PERCENT, 0);
				if ((absorbHpPercent > 0) && (Rnd.nextDouble() < _stat.getValue(Stat.ABSORB_DAMAGE_CHANCE)))
				{
					final double currentHp = _status.getCurrentHp();
					int absorbDamage = (int) Math.min(absorbHpPercent * damage, _stat.getMaxRecoverableHp() - currentHp);
					absorbDamage = Math.min(absorbDamage, (int) target.getCurrentHp());
					absorbDamage *= target.getStat().getValue(Stat.ABSORB_DAMAGE_DEFENCE, 1);
					if (absorbDamage > 0)
					{
						setCurrentHp(currentHp + absorbDamage);
					}
				}
			}
		}
		
		// Absorb MP from the damage inflicted.
		if (!isPvP || PvpConfig.MP_VAMPIRIC_ATTACK_AFFECTS_PVP)
		{
			if ((skill != null) || PlayerConfig.MP_VAMPIRIC_ATTACK_WORKS_WITH_MELEE)
			{
				final double absorbMpPercent = _stat.getValue(Stat.ABSORB_MANA_DAMAGE_PERCENT, 0);
				if ((absorbMpPercent > 0) && (Rnd.nextDouble() < _stat.getValue(Stat.ABSORB_MANA_DAMAGE_CHANCE)))
				{
					final double currentMp = _status.getCurrentMp();
					int absorbDamage = (int) Math.min(absorbMpPercent * damage, _stat.getMaxRecoverableMp() - currentMp);
					absorbDamage = Math.min(absorbDamage, (int) target.getCurrentMp());
					if (absorbDamage > 0)
					{
						setCurrentMp(currentMp + absorbDamage);
					}
				}
			}
		}
		
		// Target receives the damage.
		target.reduceCurrentHp(damage, this, skill, isDOT, directlyToHp, critical, reflect);
		
		// Check if damage should be reflected or absorbed. When killing blow is made, the target doesn't reflect (vamp too?).
		if (!reflect && !isDOT && !target.isDead() && !target.isHpBlocked())
		{
			int reflectedDamage = 0;
			
			// Reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary.
			final double reflectPercent = Math.min(target.getStat().getValue(Stat.REFLECT_DAMAGE_PERCENT, 0) - getStat().getValue(Stat.REFLECT_DAMAGE_PERCENT_DEFENSE, 0), target.isPlayer() ? PlayerConfig.PLAYER_REFLECT_PERCENT_LIMIT : PlayerConfig.NON_PLAYER_REFLECT_PERCENT_LIMIT);
			if (reflectPercent > 0)
			{
				reflectedDamage = (int) ((reflectPercent / 100.) * damage);
				reflectedDamage = (int) Math.min(reflectedDamage, target.getMaxHp());
				
				// Reflected damage is limited by P.Def/M.Def.
				if ((skill != null) && skill.isMagic())
				{
					reflectedDamage = (int) Math.min(reflectedDamage, target.getStat().getMDef() * 1.5);
				}
				else
				{
					reflectedDamage = Math.min(reflectedDamage, target.getStat().getPDef());
				}
			}
			
			if (reflectedDamage > 0)
			{
				target.doAttack(reflectedDamage, this, skill, isDOT, directlyToHp, critical, true);
			}
		}
		
		// Break casting of target during attack.
		if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
		{
			target.breakAttack();
			target.breakCast();
		}
	}
	
	/**
	 * Reduces the current health points of this creature.<br>
	 * This method handles damage calculation and application.
	 * @param amount The amount of damage to subtract from the health.
	 * @param attacker The {@code Creature} that initiated the attack.
	 * @param skill The {@code Skill} used to perform the attack.
	 */
	public void reduceCurrentHp(double amount, Creature attacker, Skill skill)
	{
		reduceCurrentHp(amount, attacker, skill, false, false, false, false);
	}
	
	/**
	 * Reduces the current health points of this creature based on an attack.<br>
	 * This method handles damage calculations including caps, PvP/PvE modifiers, and champion scaling.<br>
	 * It also triggers relevant events for damage dealt and received.
	 * @param amountValue The raw amount of damage to apply.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param skill The {@code Skill} used to perform the attack.
	 * @param isDOT Whether the damage is a Damage Over Time effect.
	 * @param directlyToHp Whether the damage should be applied directly to health.
	 * @param critical Whether the hit is a critical strike.
	 * @param reflect Whether the damage should be reflected back to the attacker.
	 */
	public void reduceCurrentHp(double amountValue, Creature attacker, Skill skill, boolean isDOT, boolean directlyToHp, boolean critical, boolean reflect)
	{
		double amount = amountValue;
		
		// Auto attacks make you stand up.
		if (isPlayer() && asPlayer().isFakeDeath() && PlayerConfig.FAKE_DEATH_DAMAGE_STAND && (amount > 0))
		{
			stopFakeDeath(true);
		}
		
		// Notify of this attack only if there is an attacking creature.
		// Events are built per hit: this creature can be damaged by many attackers on different threads at once, so a cached event would get its attacker swapped mid-dispatch.
		if ((attacker != null) && EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_DAMAGE_DEALT, attacker))
		{
			EventDispatcher.getInstance().notifyEvent(new OnCreatureDamageDealt(attacker, this, amount, skill, critical, isDOT, reflect), attacker);
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_DAMAGE_RECEIVED, this))
		{
			final DamageReturn term = EventDispatcher.getInstance().notifyEvent(new OnCreatureDamageReceived(attacker, this, amount, skill, critical, isDOT, reflect), this, DamageReturn.class);
			if (term != null)
			{
				if (term.terminate())
				{
					return;
				}
				else if (term.override())
				{
					amount = term.getDamage();
				}
			}
		}
		
		// Damage cap should not affect gm characters.
		if ((attacker != null) && !attacker.isGM())
		{
			final double damageCap = _stat.getValue(Stat.DAMAGE_LIMIT);
			if (damageCap > 0)
			{
				amount = Math.min(amount, damageCap);
			}
		}
		
		// Cursed Weapon Defense - Damage.
		if (isNpc())
		{
			final int npcId = getId();
			
			// Zariche Treasure Chests 24370 / Akamanah Treasure Chests 24371
			if ((npcId == 24370) || (npcId == 24371))
			{
				if ((attacker != null) && attacker.isPlayer())
				{
					final Player pAttacker = attacker.asPlayer();
					
					// Check if the internal player variable is the owner of the Cursed Sword, which works regardless of XML.
					final int cwId = pAttacker.getCursedWeaponEquippedId();
					boolean isRealHolder = ((cwId == 8190) || (cwId == 8689));
					
					if (isRealHolder)
					{
						// Cursed Sword holder: limit 200.
						amount = Math.min(amount, 200);
					}
					else
					{
						// Regular player: limit 20.
						amount = Math.min(amount, 20);
					}
				}
				else
				{
					// Pets/Summons: limit 20.
					amount = Math.min(amount, 20);
				}
			}
			// Priests of Purification: 24366/24369
			else if ((npcId >= 24366) && (npcId <= 24369))
			{
				if ((attacker != null) && attacker.isPlayer())
				{
					final Player pAttacker = attacker.asPlayer();
					
					final int cwId = pAttacker.getCursedWeaponEquippedId();
					boolean isRealHolder = ((cwId == 8190) || (cwId == 8689));
					
					if (isRealHolder)
					{
						// Cursed Sword holder: limit 1500.
						amount = Math.min(amount, 1500);
					}
					else
					{
						// Regular player: total block.
						return;
					}
				}
				else
				{
					// Pets/Summons: total block.
					return;
				}
			}
		}
		
		// Calculate PvP/PvE damage received. It is a post-attack stat.
		if (attacker != null)
		{
			if (attacker.isPlayable())
			{
				amount *= (100 + Math.max(_stat.getValue(Stat.PVP_DAMAGE_TAKEN), -80)) / 100;
			}
			else
			{
				amount *= (100 + Math.max(_stat.getValue(Stat.PVE_DAMAGE_TAKEN), -80)) / 100;
			}
			
			if (attacker.isRaid() || attacker.isRaidMinion())
			{
				amount *= (100 + Math.max(_stat.getValue(Stat.PVE_DAMAGE_TAKEN_RAID), -80)) / 100;
			}
			else if (attacker.isMonster())
			{
				amount *= (100 + Math.max(_stat.getValue(Stat.PVE_DAMAGE_TAKEN_MONSTER), -80)) / 100;
			}
		}
		
		if (ChampionMonstersConfig.CHAMPION_ENABLE && isChampion() && (ChampionMonstersConfig.CHAMPION_HP != 0))
		{
			_status.reduceHp(amount / ChampionMonstersConfig.CHAMPION_HP, attacker, (skill == null) || !skill.isToggle(), isDOT, false);
		}
		else if (isPlayer())
		{
			final Player player = asPlayer();
			player.addDamageTaken(attacker, skill != null ? skill.getDisplayId() : 0, amount, isDOT, reflect);
			if (!isDOT && (skill != null) && (skill.getCastRange() > 0) && (attacker != null) && !GeoEngine.getInstance().canSeeTarget(attacker, this))
			{
				amount = 0;
			}
			
			player.getStatus().reduceHp(amount, attacker, skill, (skill == null) || !skill.isToggle(), isDOT, false, directlyToHp);
		}
		else
		{
			_status.reduceHp(amount, attacker, (skill == null) || !skill.isToggle(), isDOT, false);
		}
		
		if (attacker != null)
		{
			attacker.sendDamageMessage(this, skill, (int) amount, critical, false);
		}
	}
	
	/**
	 * Reduces the current MP of the creature.<br>
	 * This method updates the internal status by subtracting a specific value.
	 * @param amount The amount of MP to remove.
	 */
	public void reduceCurrentMp(double amount)
	{
		_status.reduceMp(amount);
	}
	
	/**
	 * Removes a specific listener from the status update system.<br>
	 * This prevents the provided {@code Creature} from receiving further status updates.
	 * @param object The {@code Creature} to remove as a listener.
	 */
	@Override
	public void removeStatusListener(Creature object)
	{
		_status.removeStatusListener(object);
	}
	
	/**
	 * Stops the regeneration of health points and mana points.<br>
	 * This method updates the internal status to prevent recovery.
	 */
	protected void stopHpMpRegeneration()
	{
		_status.stopHpMpRegeneration();
	}
	
	/**
	 * Retrieves the current CP value of the actor.<br>
	 * This method accesses the internal status to get the numeric value.
	 * @return The current CP as a {@code double}.
	 */
	public double getCurrentCp()
	{
		return _status.getCurrentCp();
	}
	
	/**
	 * Calculates the current CP percentage of the creature.<br>
	 * This value is based on the ratio between current and maximum CP.
	 * @return The current CP as a percentage integer.
	 */
	public int getCurrentCpPercent()
	{
		return (int) ((_status.getCurrentCp() * 100) / _stat.getMaxCp());
	}
	
	/**
	 * Updates the current CP value of the actor.<br>
	 * This method sets the new CP value in the status object.
	 * @param newCp The new CP value to set.
	 */
	public void setCurrentCp(double newCp)
	{
		_status.setCurrentCp(newCp);
	}
	
	/**
	 * Updates the current CP value of the actor.<br>
	 * This method also handles whether to notify other players.
	 * @param newCp The new CP value to set.
	 * @param broadcast Set to {@code true} to send a message to nearby players, or {@code false} to hide the change.
	 */
	public void setCurrentCp(double newCp, boolean broadcast)
	{
		_status.setCurrentCp(newCp, broadcast);
	}
	
	/**
	 * Retrieves the current health points of the creature.<br>
	 * This value is fetched from the internal status object.
	 * @return The current health as a {@code double}.
	 */
	public double getCurrentHp()
	{
		return _status.getCurrentHp();
	}
	
	/**
	 * Calculates the current health percentage of the creature.<br>
	 * This value is returned as an integer between {@code 0} and {@code 100}.
	 * @return The current health percentage.
	 */
	public int getCurrentHpPercent()
	{
		return (int) ((_status.getCurrentHp() * 100) / _stat.getMaxHp());
	}
	
	/**
	 * Updates the current health points of the actor.<br>
	 * This method sets the {@code newHp} value into the status object.
	 * @param newHp The new health value to set.
	 */
	public void setCurrentHp(double newHp)
	{
		_status.setCurrentHp(newHp);
	}
	
	/**
	 * Updates the current health points of the actor.<br>
	 * This method also handles sending a notification to other players if requested.
	 * @param newHp The new health value to set.
	 * @param broadcast Set to {@code true} to notify others, or {@code false} to keep it silent.
	 */
	public void setCurrentHp(double newHp, boolean broadcast)
	{
		_status.setCurrentHp(newHp, broadcast);
	}
	
	/**
	 * Updates the current health points and mana points of the actor.<br>
	 * This method sets both values simultaneously to the provided amounts.
	 * @param newHp The new value for health points.
	 * @param newMp The new value for mana points.
	 */
	public void setCurrentHpMp(double newHp, double newMp)
	{
		_status.setCurrentHpMp(newHp, newMp);
	}
	
	/**
	 * Retrieves the current Mana Points of the creature.<br>
	 * This value is fetched from the internal status object.
	 * @return The current amount of {@code double} mana points.
	 */
	public double getCurrentMp()
	{
		return _status.getCurrentMp();
	}
	
	/**
	 * Calculates the current Mana Points as a percentage.<br>
	 * This value is based on the ratio of current MP to maximum MP.
	 * @return The current MP percentage as an {@code int}.
	 */
	public int getCurrentMpPercent()
	{
		return (int) ((_status.getCurrentMp() * 100) / _stat.getMaxMp());
	}
	
	/**
	 * Updates the current mana points of the actor.<br>
	 * This method sets the {@code newMp} value into the status object.
	 * @param newMp The new amount of mana to set.
	 */
	public void setCurrentMp(double newMp)
	{
		_status.setCurrentMp(newMp);
	}
	
	/**
	 * Updates the current mana points of the actor.<br>
	 * This method sets the new {@code double} value for mana.
	 * @param newMp The new amount of mana to set.
	 * @param broadcast Whether to send a network packet to other players.
	 */
	public void setCurrentMp(double newMp, boolean broadcast)
	{
		_status.setCurrentMp(newMp, broadcast);
	}
	
	/**
	 * Restores the entity to its full health and mana values.<br>
	 * A stat recalculation that is still pending runs first, so the values come from the final maximums.
	 */
	public void fullRestore()
	{
		// Template passives and fresh effects are pumped by a delayed task, filling before it lands would use the old maximums.
		_stat.runPendingRecalculation();
		_status.setCurrentHpMp(getMaxHp(), getMaxMp());
	}
	
	/**
	 * Calculates the maximum weight load for a player or pet.<br>
	 * Returns {@code 0} if the entity is not a player or a pet.
	 * @return The calculated weight limit as an {@code int}.
	 */
	public int getMaxLoad()
	{
		if (isPlayer() || isPet())
		{
			// Weight Limit = (CON Modifier*69000) * Skills.
			// Source http://l2p.bravehost.com/weightlimit.html (May 2007)
			final double baseLoad = Math.floor(BaseStat.CON.calcBonus(this) * 69000 * PlayerConfig.ALT_WEIGHT_LIMIT);
			return (int) _stat.getValue(Stat.WEIGHT_LIMIT, baseLoad);
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the weight penalty for the current entity.<br>
	 * This value is only returned if the entity is a player or a pet.<br>
	 * For all other entities, it returns {@code 0}.
	 * @return The weight penalty value as an {@code int}.
	 */
	public int getBonusWeightPenalty()
	{
		if (isPlayer() || isPet())
		{
			return (int) _stat.getValue(Stat.WEIGHT_PENALTY, 1);
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the current weight load of the entity.<br>
	 * This method checks if the entity is a player or a pet.<br>
	 * If it is, it returns the total weight from the {@code Inventory}.<br>
	 * Otherwise, it returns 0.
	 * @return The total weight as an {@code int}, or 0 if not applicable.
	 */
	public int getCurrentLoad()
	{
		if (isPlayer() || isPet())
		{
			final Inventory inventory = getInventory();
			return (inventory != null) ? inventory.getTotalWeight() : 0;
		}
		
		return 0;
	}
	
	/**
	 * Checks if the current creature is a champion.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the creature is a champion, otherwise {@code false}.
	 */
	public boolean isChampion()
	{
		return false;
	}
	
	/**
	 * Sends a damage message to the target creature.<br>
	 * This method handles the visual and text feedback for an attack.
	 * @param target The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} used to perform the attack.
	 * @param damage The amount of damage dealt.
	 * @param crit Whether the hit was a critical strike.
	 * @param miss Whether the attack failed to hit the target.
	 */
	public void sendDamageMessage(Creature target, Skill skill, int damage, boolean crit, boolean miss)
	{
	}
	
	/**
	 * Retrieves the attack element of the creature.<br>
	 * This method returns the {@code AttributeType} associated with its attacks.
	 * @return The {@code AttributeType} of the attack element.
	 */
	public AttributeType getAttackElement()
	{
		return _stat.getAttackElement();
	}
	
	/**
	 * Retrieves the value of a specific attack element.<br>
	 * This method looks up the attribute provided by the caller.
	 * @param attackAttribute The type of attack attribute to check.
	 * @return The integer value associated with the given attribute.
	 */
	public int getAttackElementValue(AttributeType attackAttribute)
	{
		return _stat.getAttackElementValue(attackAttribute);
	}
	
	/**
	 * Retrieves the defense value for a specific element type.<br>
	 * This method looks up the value from the internal statistics.
	 * @param defenseAttribute The {@code AttributeType} to check.
	 * @return The integer defense value for the given attribute.
	 */
	public int getDefenseElementValue(AttributeType defenseAttribute)
	{
		return _stat.getDefenseElementValue(defenseAttribute);
	}
	
	/**
	 * Enables or disables the core AI for this entity.<br>
	 * Setting {@code true} will turn off the AI logic.<br>
	 * Setting {@code false} will keep the AI active.
	 * @param value The state to set for the AI status.
	 */
	public void disableCoreAI(boolean value)
	{
		_disabledAI = value;
	}
	
	/**
	 * Checks if the core AI for this entity is currently disabled.<br>
	 * This status determines whether the creature follows its standard behaviors.
	 * @return {@code true} if the AI is disabled, {@code false} otherwise.
	 */
	public boolean isCoreAIDisabled()
	{
		return _disabledAI;
	}
	
	/**
	 * Checks if the raid curse can be applied to the actor.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the curse is successfully given, otherwise {@code false}.
	 */
	public boolean giveRaidCurse()
	{
		return false;
	}
	
	/**
	 * Checks if the entity is currently affected by a specific effect.<br>
	 * This method queries the internal {@code EffectList}.
	 * @param flag The {@code EffectFlag} to check for.
	 * @return {@code true} if the effect is active, {@code false} otherwise.
	 */
	public boolean isAffected(EffectFlag flag)
	{
		return _effectList.isAffected(flag);
	}
	
	/**
	 * Retrieves the team associated with this creature.
	 * @return the {@code Team} object.
	 */
	public Team getTeam()
	{
		return _team;
	}
	
	/**
	 * Sets the team for this creature.<br>
	 * This method updates the internal {@code _team} field.
	 * @param team The {@code Team} to assign.
	 */
	public void setTeam(Team team)
	{
		_team = team;
	}
	
	/**
	 * Sets whether the entity can be killed.<br>
	 * Use {@code true} to allow the entity to die.<br>
	 * Use {@code false} to make the entity invincible.
	 * @param value The lethality status of the entity.
	 */
	public void setLethalable(boolean value)
	{
		_lethalable = value;
	}
	
	/**
	 * Checks if the entity can be killed.<br>
	 * Returns {@code true} if the entity is lethalable.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating if the entity can be killed.
	 */
	public boolean isLethalable()
	{
		return _lethalable;
	}
	
	/**
	 * Checks if the actor has any trigger skills assigned.<br>
	 * Returns {@code true} if the skill set is not empty.<br>
	 * Returns {@code false} if there are no trigger skills.
	 * @return a boolean indicating if trigger skills exist.
	 */
	public boolean hasTriggerSkills()
	{
		return !_triggerSkills.isEmpty();
	}
	
	/**
	 * Retrieves the map of skills that are triggered by this entity.<br>
	 * The keys represent the skill IDs.
	 * @return a {@code Map<Integer, OptionSkillHolder>} containing all trigger skills.
	 */
	public Map<Integer, OptionSkillHolder> getTriggerSkills()
	{
		return _triggerSkills;
	}
	
	/**
	 * Adds a skill to the list of trigger skills.<br>
	 * This method maps the skill ID to its corresponding {@code OptionSkillHolder}.
	 * @param holder The {@code OptionSkillHolder} containing the skill data.
	 */
	public void addTriggerSkill(OptionSkillHolder holder)
	{
		getTriggerSkills().put(holder.getSkill().getId(), holder);
	}
	
	/**
	 * Removes a specific skill from the list of trigger skills.<br>
	 * This method identifies the skill using its unique ID from the provided holder.
	 * @param holder The {@code OptionSkillHolder} containing the skill to be removed.
	 */
	public void removeTriggerSkill(OptionSkillHolder holder)
	{
		getTriggerSkills().remove(holder.getSkill().getId());
	}
	
	/**
	 * Checks if the entity is allowed to be revived.<br>
	 * This method currently always returns {@code true}.
	 * @return {@code true} if the entity can be revived, otherwise {@code false}.
	 */
	public boolean canRevive()
	{
		return true;
	}
	
	/**
	 * Sets whether the entity is allowed to be revived.<br>
	 * Use {@code true} to enable revival and {@code false} to disable it.
	 * @param value The boolean state for revival permission.
	 */
	public void setCanRevive(boolean value)
	{
	}
	
	/**
	 * Checks if the sweep ability is currently active.<br>
	 * This method always returns {@code false}.
	 * @return {@code true} if the sweep is active, otherwise {@code false}.
	 */
	public boolean isSweepActive()
	{
		return false;
	}
	
	/**
	 * Retrieves the unique identifier for the clan.<br>
	 * This method returns a default value of {@code 0}.
	 * @return The integer ID of the clan.
	 */
	public int getClanId()
	{
		return 0;
	}
	
	/**
	 * Retrieves the {@code Clan} associated with this entity.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Clan} object or {@code null} if no clan exists.
	 */
	public Clan getClan()
	{
		return null;
	}
	
	/**
	 * Checks if the current actor is a member of the academy.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the actor is an academy member, otherwise {@code false}.
	 */
	public boolean isAcademyMember()
	{
		return false;
	}
	
	/**
	 * Retrieves the type of pledge associated with the entity.<br>
	 * This method currently returns a default value of {@code 0}.
	 * @return The integer representation of the pledge type.
	 */
	public int getPledgeType()
	{
		return 0;
	}
	
	/**
	 * Retrieves the unique identifier for the ally. <br>
	 * This method currently returns a default value of {@code 0}.
	 * @return The integer ID of the ally.
	 */
	public int getAllyId()
	{
		return 0;
	}
	
	/**
	 * Notifies listeners that an attack was avoided by a specific creature.<br>
	 * This method triggers the {@code ON_CREATURE_ATTACK_AVOID} event.
	 * @param target The {@code Creature} that avoided the attack.
	 * @param isDot A boolean indicating if the attack was a damage-over-time effect.
	 */
	public void notifyAttackAvoid(Creature target, boolean isDot)
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_ATTACK_AVOID, target))
		{
			if (_onCreatureAttackAvoid == null)
			{
				_onCreatureAttackAvoid = new OnCreatureAttackAvoid();
			}
			
			_onCreatureAttackAvoid.setAttacker(this);
			_onCreatureAttackAvoid.setTarget(target);
			_onCreatureAttackAvoid.setDamageOverTime(isDot);
			EventDispatcher.getInstance().notifyEvent(_onCreatureAttackAvoid, target);
		}
	}
	
	/**
	 * Retrieves the current attack type of the actor.<br>
	 * It checks for an active weapon first.<br>
	 * If no weapon is held, it uses the base attack type from the template or transformation.
	 * @return The {@code WeaponType} of the current weapon or the default base attack type.
	 */
	public WeaponType getAttackType()
	{
		final Weapon weapon = getActiveWeaponItem();
		if (weapon != null)
		{
			return weapon.getItemType();
		}
		
		final WeaponType defaultWeaponType = _template.getBaseAttackType();
		if (_transform == null)
		{
			return defaultWeaponType;
		}
		
		return _transform.getBaseAttackType(this, defaultWeaponType);
	}
	
	/**
	 * Checks if this entity belongs to a specific category.<br>
	 * This method queries the {@code CategoryData} instance.
	 * @param type The {@code CategoryType} to check against.
	 * @return {@code true} if the entity is in the category, {@code false} otherwise.
	 */
	public boolean isInCategory(CategoryType type)
	{
		return CategoryData.getInstance().isInCategory(type, getId());
	}
	
	/**
	 * Checks if the current entity belongs to any of the provided categories.<br>
	 * It iterates through each {@code CategoryType} in the input array.
	 * @param types The list of {@code CategoryType} values to check against.
	 * @return {@code true} if the entity is in at least one category, otherwise {@code false}.
	 */
	public boolean isInOneOfCategory(CategoryType... types)
	{
		for (CategoryType type : types)
		{
			if (CategoryData.getInstance().isInCategory(type, getId()))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves the summoner associated with this creature.
	 * @return the {@code Creature} that summoned this entity or {@code null} if none exists.
	 */
	public Creature getSummoner()
	{
		return _summoner;
	}
	
	/**
	 * Sets the {@code Creature} that is summoning this entity.<br>
	 * This updates the internal {@code _summoner} field.
	 * @param summoner The {@code Creature} acting as the summoner.
	 */
	public void setSummoner(Creature summoner)
	{
		_summoner = summoner;
	}
	
	/**
	 * Adds a new {@code Npc} to the list of summoned entities.<br>
	 * This method links the {@code Npc} to this actor as its summoner.
	 * @param npc The {@code Npc} instance to be added.
	 */
	public void addSummonedNpc(Npc npc)
	{
		if (_summonedNpcs == null)
		{
			synchronized (this)
			{
				if (_summonedNpcs == null)
				{
					_summonedNpcs = new ConcurrentHashMap<>();
				}
			}
		}
		
		_summonedNpcs.put(npc.getObjectId(), npc);
		
		npc.setSummoner(this);
	}
	
	/**
	 * Removes a specific NPC from the list of summoned NPCs.<br>
	 * This method checks if any summoned NPCs exist before attempting removal.
	 * @param objectId The unique identifier of the NPC to remove.
	 */
	public void removeSummonedNpc(int objectId)
	{
		if (_summonedNpcs != null)
		{
			_summonedNpcs.remove(objectId);
		}
	}
	
	/**
	 * Retrieves all NPCs that have been summoned by this actor.<br>
	 * Returns an empty list if no NPCs are currently summoned.
	 * @return a {@code Collection} of {@code Npc} objects.
	 */
	public Collection<Npc> getSummonedNpcs()
	{
		return _summonedNpcs != null ? _summonedNpcs.values() : Collections.emptyList();
	}
	
	/**
	 * Retrieves a specific {@code Npc} based on its unique identifier.<br>
	 * This method checks the internal collection of summoned NPCs.<br>
	 * It returns {@code null} if no NPC is found or if the list is empty.
	 * @param objectId The unique ID of the NPC to retrieve.
	 * @return The {@code Npc} instance associated with the ID, or {@code null}.
	 */
	public Npc getSummonedNpc(int objectId)
	{
		if (_summonedNpcs != null)
		{
			return _summonedNpcs.get(objectId);
		}
		
		return null;
	}
	
	/**
	 * Retrieves the total number of NPCs currently summoned by this actor.<br>
	 * It returns {@code 0} if no NPCs have been summoned.
	 * @return The count of summoned NPCs as an {@code int}.
	 */
	public int getSummonedNpcCount()
	{
		return _summonedNpcs != null ? _summonedNpcs.size() : 0;
	}
	
	/**
	 * Clears the list of currently summoned NPCs.<br>
	 * This method removes all entities from the {@code _summonedNpcs} collection.<br>
	 * It checks if the collection is not {@code null} before clearing it.
	 */
	public void resetSummonedNpcs()
	{
		if (_summonedNpcs != null)
		{
			_summonedNpcs.clear();
		}
	}
	
	/**
	 * Retrieves the creature associated with a recall point.<br>
	 * This method returns the {@code Creature} object stored in the internal field.
	 * @return The {@code Creature} linked to the recall, or {@code null} if none exists.
	 */
	public Creature getRecallCreature()
	{
		return _recallCreature;
	}
	
	/**
	 * Sets the creature used for the recall action.<br>
	 * This defines which {@code Creature} entity is targeted when a recall is triggered.
	 * @param recallCreature The {@code Creature} to be set as the recall target.
	 */
	public void setRecallCreature(Creature recallCreature)
	{
		_recallCreature = recallCreature;
	}
	
	/**
	 * Checks if the current entity is a creature.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the entity is a creature.
	 */
	@Override
	public boolean isCreature()
	{
		return true;
	}
	
	/**
	 * Converts the current object into a {@code Creature}.<br>
	 * This method returns the current instance.
	 * @return the current {@code Creature} instance.
	 */
	@Override
	public Creature asCreature()
	{
		return this;
	}
	
	/**
	 * Retrieves the minimum distance to a shop.<br>
	 * This method currently returns a default value of {@code 0}.
	 * @return The minimum distance as an {@code int}.
	 */
	public int getMinShopDistance()
	{
		return 0;
	}
	
	/**
	 * Retrieves all entities that can cast skills.
	 * @return a {@code Collection} of {@code SkillCaster} objects.
	 */
	public Collection<SkillCaster> getSkillCasters()
	{
		return _skillCasters.values();
	}
	
	/**
	 * Registers a new {@code SkillCaster} with a specific casting type.<br>
	 * This method stores the caster in the internal map.
	 * @param castingType The category of the casting type.
	 * @param skillCaster The {@code SkillCaster} instance to add.
	 * @return The previous {@code SkillCaster} associated with this type, or {@code null}.
	 */
	public SkillCaster addSkillCaster(SkillCastingType castingType, SkillCaster skillCaster)
	{
		return _skillCasters.put(castingType, skillCaster);
	}
	
	/**
	 * Removes a {@code SkillCaster} from the internal collection based on its casting type.<br>
	 * This method identifies the caster associated with the provided {@code castingType}.
	 * @param castingType The type of skill casting to identify the caster for removal.
	 * @return The {@code SkillCaster} that was removed, or {@code null} if no such caster existed.
	 */
	public SkillCaster removeSkillCaster(SkillCastingType castingType)
	{
		return _skillCasters.remove(castingType);
	}
	
	/**
	 * Retrieves a list of {@code SkillCaster} objects that match specific criteria.<br>
	 * This method combines multiple filters to narrow down the results.
	 * @param filterValue The primary {@code Predicate} used to filter the casters.
	 * @param filters A variable number of additional {@code Predicate} objects to apply.
	 * @return A {@code List} containing all {@code SkillCaster} objects that pass every filter.
	 */
	@SafeVarargs
	public final List<SkillCaster> getSkillCasters(Predicate<SkillCaster> filterValue, Predicate<SkillCaster>... filters)
	{
		Predicate<SkillCaster> filter = filterValue;
		for (Predicate<SkillCaster> additionalFilter : filters)
		{
			filter = filter.and(additionalFilter);
		}
		
		final List<SkillCaster> result = new ArrayList<>();
		for (SkillCaster skillCaster : _skillCasters.values())
		{
			if (filter.test(skillCaster))
			{
				result.add(skillCaster);
			}
		}
		
		return result;
	}
	
	/**
	 * Finds a {@code SkillCaster} that matches all provided criteria.<br>
	 * It combines the primary filter with any additional filters provided.<br>
	 * The method returns the first matching result found in the collection.
	 * @param filterValue The primary {@code Predicate} used to filter the {@code SkillCaster}.
	 * @param filters A variable number of additional {@code Predicate} objects to apply.
	 * @return The first {@code SkillCaster} that matches all conditions, or {@code null} if none are found.
	 */
	@SafeVarargs
	public final SkillCaster getSkillCaster(Predicate<SkillCaster> filterValue, Predicate<SkillCaster>... filters)
	{
		Predicate<SkillCaster> filter = filterValue;
		for (Predicate<SkillCaster> additionalFilter : filters)
		{
			filter = filter.and(additionalFilter);
		}
		
		for (SkillCaster skillCaster : _skillCasters.values())
		{
			if (filter.test(skillCaster))
			{
				return skillCaster;
			}
		}
		
		return null;
	}
	
	/**
	 * Checks if the entity is currently performing a channeling action.<br>
	 * This method verifies if the {@code _channelizer} object exists and is active.
	 * @return {@code true} if the entity is channeling, otherwise {@code false}.
	 */
	public boolean isChanneling()
	{
		return (_channelizer != null) && _channelizer.isChanneling();
	}
	
	/**
	 * Retrieves the {@code SkillChannelizer} for this entity.<br>
	 * If it does not exist, a new one is created and assigned.
	 * @return The {@code SkillChannelizer} instance.
	 */
	public SkillChannelizer getSkillChannelizer()
	{
		if (_channelizer == null)
		{
			_channelizer = new SkillChannelizer(this);
		}
		
		return _channelizer;
	}
	
	/**
	 * Checks if the current entity is not restricted to a specific channel.<br>
	 * This method returns {@code true} if the entity can move freely between channels.<br>
	 * It returns {@code false} if the entity is locked to a single channel.
	 * @return {@code true} if the entity is not channelized, otherwise {@code false}.
	 */
	public boolean isChannelized()
	{
		return (_channelized != null) && !_channelized.isChannelized();
	}
	
	/**
	 * Retrieves the {@code SkillChannelized} object for this entity.<br>
	 * This method creates a new instance if it does not already exist.
	 * @return The current {@code SkillChannelized} object.
	 */
	public SkillChannelized getSkillChannelized()
	{
		if (_channelized == null)
		{
			_channelized = new SkillChannelized();
		}
		
		return _channelized;
	}
	
	/**
	 * Adds a skill to the list of ignored effects.<br>
	 * This method tracks how many instances of a specific skill are being ignored.<br>
	 * If the skill is already tracked, it increases the instance count.<br>
	 * Otherwise, it creates a new entry for the skill.
	 * @param holder The {@code SkillHolder} containing the skill to ignore.
	 */
	public void addIgnoreSkillEffects(SkillHolder holder)
	{
		final Integer skillId = holder.getSkillId();
		final Map<Integer, IgnoreSkillHolder> ignoreSkills = getIgnoreSkillEffects();
		final IgnoreSkillHolder ignoreSkillHolder = ignoreSkills.get(skillId);
		if (ignoreSkillHolder != null)
		{
			ignoreSkillHolder.increaseInstances();
			return;
		}
		
		ignoreSkills.put(skillId, new IgnoreSkillHolder(holder));
	}
	
	/**
	 * Removes the ignore effect for a specific skill from the current holder.<br>
	 * This happens when the instance count for that skill reaches zero.
	 * @param holder The {@code SkillHolder} containing the skill to check.
	 */
	public void removeIgnoreSkillEffects(SkillHolder holder)
	{
		final Integer skillId = holder.getSkillId();
		final IgnoreSkillHolder ignoreSkillHolder = getIgnoreSkillEffects().get(skillId);
		if ((ignoreSkillHolder != null) && (ignoreSkillHolder.decreaseInstances() < 1))
		{
			getIgnoreSkillEffects().remove(skillId);
		}
	}
	
	/**
	 * Checks if the effects of a specific skill should be ignored.<br>
	 * This method verifies if the {@code skillId} is present in the ignore list.<br>
	 * It also checks if the {@code skillLevel} matches the required level for ignoring.
	 * @param skillId The unique identifier of the skill to check.
	 * @param skillLevel The current level of the skill being evaluated.
	 * @return {@code true} if the skill effects are ignored, otherwise {@code false}.
	 */
	public boolean isIgnoringSkillEffects(int skillId, int skillLevel)
	{
		if (!_ignoreSkillEffects.isEmpty())
		{
			final SkillHolder holder = getIgnoreSkillEffects().get(skillId);
			return ((holder != null) && ((holder.getSkillLevel() < 1) || (holder.getSkillLevel() == skillLevel)));
		}
		
		return false;
	}
	
	/**
	 * Retrieves the map of skills that are currently being ignored.<br>
	 * This map contains {@code IgnoreSkillHolder} objects for each skill ID.
	 * @return a {@code Map} where keys are skill IDs and values are their corresponding holders.
	 */
	private Map<Integer, IgnoreSkillHolder> getIgnoreSkillEffects()
	{
		return _ignoreSkillEffects;
	}
	
	/**
	 * Retrieves the collection of listeners for a specific event type.<br>
	 * This method combines listeners from the object, its template, and global containers.<br>
	 * If multiple sources have listeners, they are merged into a single list.
	 * @param type The {@code EventType} to filter listeners by.
	 * @return A {@code Collection} of {@code AbstractEventListener} objects.
	 */
	@Override
	public Collection<AbstractEventListener> getListeners(EventType type)
	{
		final Collection<AbstractEventListener> objectListeners = super.getListeners(type);
		final Collection<AbstractEventListener> templateListeners = _template.getListeners(type);
		final Collection<AbstractEventListener> globalListeners = isMonster() ? Containers.Monsters().getListeners(type) : isNpc() ? Containers.Npcs().getListeners(type) : isPlayer() ? Containers.Players().getListeners(type) : Collections.emptyList();
		
		// Avoid creating a new object.
		final boolean objEmpty = objectListeners.isEmpty();
		final boolean tplEmpty = templateListeners.isEmpty();
		final boolean glbEmpty = globalListeners.isEmpty();
		if (objEmpty && tplEmpty && glbEmpty)
		{
			return Collections.emptyList();
		}
		else if (!objEmpty && tplEmpty && glbEmpty)
		{
			return objectListeners;
		}
		else if (!tplEmpty && objEmpty && glbEmpty)
		{
			return templateListeners;
		}
		else if (!glbEmpty && objEmpty && tplEmpty)
		{
			return globalListeners;
		}
		
		final Collection<AbstractEventListener> allListeners = new ArrayList<>(objectListeners.size() + templateListeners.size() + globalListeners.size());
		allListeners.addAll(objectListeners);
		allListeners.addAll(templateListeners);
		allListeners.addAll(globalListeners);
		return allListeners;
	}
	
	/**
	 * Retrieves the race of this creature.<br>
	 * This value is obtained from the underlying {@code CreatureTemplate}.
	 * @return the {@code Race} of the creature.
	 */
	public Race getRace()
	{
		return _template.getRace();
	}
	
	/**
	 * Updates the 3D coordinates of this entity.<br>
	 * This method validates that the new location is not {@code 0, 0}.<br>
	 * It also ensures the new region exists and updates zone memberships.
	 * @param newX The new X coordinate.
	 * @param newY The new Y coordinate.
	 * @param newZ The new Z coordinate.
	 */
	@Override
	public void setXYZ(int newX, int newY, int newZ)
	{
		// 0, 0 is not a valid location.
		if ((newX == 0) && (newY == 0))
		{
			return;
		}
		
		// Mobius: Prevent moving to nonexistent regions.
		final ZoneRegion newZoneRegion = ZoneManager.getInstance().getRegion(newX, newY);
		if (newZoneRegion == null)
		{
			return;
		}
		
		final ZoneRegion oldZoneRegion = ZoneManager.getInstance().getRegion(this);
		if (oldZoneRegion != newZoneRegion)
		{
			oldZoneRegion.removeFromZones(this);
			newZoneRegion.revalidateZones(this);
		}
		
		super.setXYZ(newX, newY, newZ);
	}
	
	/**
	 * Retrieves the map of all known relations for this entity.<br>
	 * The keys are unique identifiers and the values are {@code RelationCache} objects.
	 * @return a {@code Map<Integer, RelationCache>} containing the known relations.
	 */
	public Map<Integer, RelationCache> getKnownRelations()
	{
		return _knownRelations;
	}
	
	/**
	 * Checks if this entity can be targeted by other actors.<br>
	 * It verifies the base targetable status and ensures no untargetable effects are active.
	 * @return {@code true} if the entity is targetable, {@code false} otherwise.
	 */
	@Override
	public boolean isTargetable()
	{
		return super.isTargetable() && !isAffected(EffectFlag.UNTARGETABLE);
	}
	
	/**
	 * Checks if the entity has targeting disabled.<br>
	 * This method returns {@code true} if the {@code TARGETING_DISABLED} flag is active.
	 * @return {@code true} if targeting is disabled, {@code false} otherwise.
	 */
	public boolean isTargetingDisabled()
	{
		return isAffected(EffectFlag.TARGETING_DISABLED);
	}
	
	/**
	 * Checks if the entity is currently prevented from escaping.<br>
	 * This method returns {@code true} if the {@code CANNOT_ESCAPE} flag is active or it is inside a {@code NO_ESCAPE} zone.
	 * @return {@code true} if the entity cannot escape, {@code false} otherwise.
	 */
	public boolean cannotEscape()
	{
		return isAffected(EffectFlag.CANNOT_ESCAPE) || isInsideZone(ZoneId.NO_ESCAPE);
	}
	
	/**
	 * Sets the number of times an abnormal shield blocks attacks.<br>
	 * This value determines how many hits the shield can absorb.
	 * @param times The number of blocks allowed for the abnormal shield.
	 */
	public void setAbnormalShieldBlocks(int times)
	{
		_abnormalShieldBlocks.set(times);
	}
	
	/**
	 * Retrieves the number of blocks provided by abnormal shields.<br>
	 * This value is stored in an {@code AtomicInteger}.
	 * @return The total count of abnormal shield blocks.
	 */
	public int getAbnormalShieldBlocks()
	{
		return _abnormalShieldBlocks.get();
	}
	
	/**
	 * Reduces the count of abnormal shield blocks by one.<br>
	 * This method updates the internal counter and retrieves the new value.
	 * @return The updated number of abnormal shield blocks.
	 */
	public int decrementAbnormalShieldBlocks()
	{
		return _abnormalShieldBlocks.decrementAndGet();
	}
	
	/**
	 * Updates the accumulated abnormal time for a specific skill.<br>
	 * If the skill already has an existing value, it multiplies that value by the new {@code time}.<br>
	 * Otherwise, it sets the initial value to {@code time}.
	 * @param skillId The unique identifier of the skill.
	 * @param time The multiplier or base time value to apply.
	 */
	public void addMultipliedAbnormalTime(int skillId, double time)
	{
		final double oldTime = _multipliedAbnormalTimes.getOrDefault(skillId, 0d);
		if (oldTime != 0d)
		{
			_multipliedAbnormalTimes.put(skillId, oldTime * time);
		}
		else
		{
			_multipliedAbnormalTimes.put(skillId, time);
		}
	}
	
	/**
	 * Adds a specific amount of abnormal time to a skill.<br>
	 * This method updates the total time for the given {@code skillId}.<br>
	 * If the skill already has an existing time, it adds the new value to it.
	 * @param skillId The unique identifier for the skill.
	 * @param time The amount of time to add to the skill.
	 */
	public void addAddedAbnormalTime(int skillId, int time)
	{
		final int oldTime = _addedAbnormalTimes.getOrDefault(skillId, 0);
		if (oldTime != 0)
		{
			_addedAbnormalTimes.put(skillId, oldTime + time);
		}
		else
		{
			_addedAbnormalTimes.put(skillId, time);
		}
	}
	
	/**
	 * Updates the multiplier for abnormal times associated with a specific skill.<br>
	 * If the new {@code time} matches the existing value, the entry is removed.<br>
	 * Otherwise, it recalculates the multiplier based on the previous value.
	 * @param skillId The unique identifier of the skill to update.
	 * @param time The current abnormal time value for the skill.
	 */
	public void removeMultipliedAbnormalTime(int skillId, double time)
	{
		final double oldTime = _multipliedAbnormalTimes.getOrDefault(skillId, 0d);
		if (oldTime == time)
		{
			_multipliedAbnormalTimes.remove(skillId);
		}
		else
		{
			_multipliedAbnormalTimes.put(skillId, oldTime / time);
		}
	}
	
	/**
	 * Removes or updates an abnormal time value for a specific skill.<br>
	 * If the provided {@code time} matches the current stored value, the entry is removed.<br>
	 * Otherwise, the stored value is decreased by the provided {@code time}.
	 * @param skillId The unique identifier of the skill.
	 * @param time The amount of time to subtract or check against.
	 */
	public void removeAddedAbnormalTime(int skillId, int time)
	{
		final int oldTime = _addedAbnormalTimes.getOrDefault(skillId, 0);
		if (oldTime == time)
		{
			_addedAbnormalTimes.remove(skillId);
		}
		else
		{
			_addedAbnormalTimes.put(skillId, oldTime - time);
		}
	}
	
	/**
	 * Retrieves the multiplied abnormal time for a specific skill.<br>
	 * It looks up the value using the provided {@code skillId}.<br>
	 * Returns {@code 0d} if the skill is not found.
	 * @param skillId The unique identifier of the skill.
	 * @return The multiplied abnormal time as a {@code double}.
	 */
	public double getMultipliedAbnormalTime(int skillId)
	{
		return _multipliedAbnormalTimes.getOrDefault(skillId, 0d);
	}
	
	/**
	 * Retrieves the total abnormal time added for a specific skill.<br>
	 * It looks up the value using the provided {@code skillId}.<br>
	 * If no data exists for the ID, it returns {@code 0}.
	 * @param skillId The unique identifier of the skill.
	 * @return The amount of abnormal time associated with the skill.
	 */
	public int getAddedAbnormalTime(int skillId)
	{
		return _addedAbnormalTimes.getOrDefault(skillId, 0);
	}
	
	/**
	 * Checks if the entity has a specific type of abnormal effect.<br>
	 * This method queries the internal {@code EffectList}.
	 * @param abnormalType The {@code AbnormalType} to check for.
	 * @return {@code true} if the effect exists, {@code false} otherwise.
	 */
	public boolean hasAbnormalType(AbnormalType abnormalType)
	{
		return _effectList.hasAbnormalType(abnormalType);
	}
	
	/**
	 * Adds a skill to the list of allowed actions for blocking.<br>
	 * This method increments the count for the specified {@code skillId}.
	 * @param skillId The unique identifier of the skill.
	 */
	public void addBlockActionsAllowedSkill(Integer skillId)
	{
		_blockActionsAllowedSkills.computeIfAbsent(skillId, _ -> new AtomicInteger()).incrementAndGet();
	}
	
	/**
	 * Removes a skill from the allowed actions list.<br>
	 * This method decrements the count for the specified {@code skillId}.<br>
	 * If the count reaches {@code 0}, the skill is removed from the internal map.
	 * @param skillId The unique identifier of the skill to update.
	 */
	public void removeBlockActionsAllowedSkill(Integer skillId)
	{
		_blockActionsAllowedSkills.computeIfPresent(skillId, (_, v) -> v.decrementAndGet() != 0 ? v : null);
	}
	
	/**
	 * Checks if a specific skill is allowed to perform blocked actions.<br>
	 * This method looks up the skill ID in the internal block list.
	 * @param skill The {@code Skill} object to check.
	 * @return {@code true} if the skill is allowed, {@code false} otherwise.
	 */
	public boolean isBlockedActionsAllowedSkill(Skill skill)
	{
		return _blockActionsAllowedSkills.containsKey(skill.getId());
	}
	
	/**
	 * Initializes the internal tracking for creatures that this entity can see.<br>
	 * It sets the {@code _seenCreatureRange} based on the NPC template if applicable.<br>
	 * This method also registers the entity with the {@code CreatureSeeTaskManager}.
	 */
	protected void initSeenCreatures()
	{
		if (_seenCreatures == null)
		{
			synchronized (this)
			{
				if (_seenCreatures == null)
				{
					if (isNpc())
					{
						final NpcTemplate template = asNpc().getTemplate();
						if (template != null)
						{
							final int aggroRange = template.getAggroRange();
							if (aggroRange > 0)
							{
								_seenCreatureRange = aggroRange;
							}
						}
					}
					
					_seenCreatures = ConcurrentHashMap.newKeySet(1);
				}
			}
		}
		
		CreatureSeeTaskManager.getInstance().add(this);
	}
	
	/**
	 * Updates the list of creatures currently visible to this entity.<br>
	 * It checks for nearby {@code Creature} objects within a specific range.<br>
	 * If a new visible creature is found, it triggers an {@code ON_CREATURE_SEE} event.
	 */
	public void updateSeenCreatures()
	{
		if ((_seenCreatures == null) || _isDead || !isSpawned())
		{
			return;
		}
		
		// Check if region and its neighbors are active.
		final WorldRegion region = getWorldRegion();
		if ((region == null) || !region.areNeighborsActive())
		{
			return;
		}
		
		World.forEachVisibleObjectInRange(this, Creature.class, _seenCreatureRange, creature ->
		{
			if (!creature.isInvisible() && _seenCreatures.add(creature) && EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_SEE, this))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnCreatureSee(this, creature), this);
			}
		});
	}
	
	/**
	 * Removes a specific creature from the list of seen creatures.<br>
	 * This method updates the internal tracking of encountered entities.
	 * @param worldObject The {@code WorldObject} to be removed.
	 */
	public void removeSeenCreature(WorldObject worldObject)
	{
		if (_seenCreatures == null)
		{
			return;
		}
		
		_seenCreatures.remove(worldObject);
	}
	
	/**
	 * Retrieves the current movement type of the creature.<br>
	 * It checks if the entity is moving and whether it is currently running.
	 * @return the {@code MoveType} representing the current state.
	 */
	public MoveType getMoveType()
	{
		if (!isMoving())
		{
			return MoveType.STANDING;
		}
		
		return _isRunning ? MoveType.RUNNING : MoveType.WALKING;
	}
	
	/**
	 * Updates the status of a specific type for this entity.<br>
	 * It checks if the new value differs from the current stored value.<br>
	 * If the value has changed, it adds the update to the {@code StatusUpdate} object.
	 * @param su The {@code StatusUpdate} object to store the change in.
	 * @param type The {@code StatusUpdateType} representing the status being updated.
	 */
	protected void computeStatusUpdate(StatusUpdate su, StatusUpdateType type)
	{
		final long newValue = type.getValue(this);
		_statusUpdates.compute(type, (_, oldValue) ->
		{
			if ((oldValue == null) || (oldValue != newValue))
			{
				su.addUpdate(type, newValue);
				return newValue;
			}
			
			return oldValue;
		});
	}
	
	/**
	 * Adds a specific status update value to the internal collection.<br>
	 * This method maps the provided {@code StatusUpdateType} to its corresponding value for this object.
	 * @param type The {@code StatusUpdateType} to be added.
	 */
	protected void addStatusUpdateValue(StatusUpdateType type)
	{
		_statusUpdates.put(type, type.getValue(this));
	}
	
	/**
	 * Initializes the cache for status update values.<br>
	 * This method registers specific types like {@code MAX_HP} and {@code CUR_MP}.<br>
	 * It ensures that these attributes are tracked during updates.
	 */
	protected void initStatusUpdateCache()
	{
		addStatusUpdateValue(StatusUpdateType.MAX_HP);
		addStatusUpdateValue(StatusUpdateType.MAX_MP);
		addStatusUpdateValue(StatusUpdateType.CUR_HP);
		addStatusUpdateValue(StatusUpdateType.CUR_MP);
	}
	
	/**
	 * Checks if the entity has basic property resistance.<br>
	 * This method currently always returns {@code true}.
	 * @return {@code true} if the entity has basic property resistance.
	 */
	public boolean hasBasicPropertyResist()
	{
		return true;
	}
	
	/**
	 * Retrieves the resistance value for a specific basic property.<br>
	 * If no resistance exists for the given {@code basicProperty}, a new one is created.
	 * @param basicProperty The type of basic property to check.
	 * @return The {@code BasicPropertyResist} associated with the property.
	 */
	public BasicPropertyResist getBasicPropertyResist(BasicProperty basicProperty)
	{
		return _basicPropertyResists.computeIfAbsent(basicProperty, _ -> new BasicPropertyResist());
	}
	
	/**
	 * Retrieves the current reputation value of the actor.
	 * @return The current {@code int} reputation score.
	 */
	public int getReputation()
	{
		return _reputation;
	}
	
	/**
	 * Updates the current reputation value of the actor.<br>
	 * This method sets the internal {@code _reputation} field to a new value.
	 * @param reputation The new reputation value to assign.
	 */
	public void setReputation(int reputation)
	{
		_reputation = reputation;
	}
	
	/**
	 * Checks if a specific shot type is currently charged.<br>
	 * This method looks up the {@code ShotType} in the internal collection of charged shots.
	 * @param type The {@code ShotType} to check.
	 * @return {@code true} if the shot is charged, otherwise {@code false}.
	 */
	public boolean isChargedShot(ShotType type)
	{
		return _chargedShots.contains(type);
	}
	
	/**
	 * Adds a specific shot type to the current charge list.<br>
	 * This method tracks which shots are currently being charged by the actor.
	 * @param type The {@code ShotType} to add to the collection.
	 * @return {@code true} if the shot was successfully added, and {@code false} otherwise.
	 */
	public boolean chargeShot(ShotType type)
	{
		return _chargedShots.add(type);
	}
	
	/**
	 * Removes a specific shot type from the list of charged shots.<br>
	 * This method checks if the {@code ShotType} exists in the internal collection.
	 * @param type The {@code ShotType} to be removed.
	 * @return {@code true} if the shot was successfully removed, and {@code false} otherwise.
	 */
	public boolean unchargeShot(ShotType type)
	{
		return _chargedShots.remove(type);
	}
	
	/**
	 * Clears all currently charged shots.<br>
	 * This method resets the {@code _chargedShots} set to an empty state.
	 */
	public void unchargeAllShots()
	{
		_chargedShots = EnumSet.noneOf(ShotType.class);
	}
	
	/**
	 * This is a dummy method for recharging shots.<br>
	 * It should be overridden by subclasses to implement specific logic.
	 * @param physical Set to {@code true} if the shot is of physical type.
	 * @param magic Set to {@code true} if the shot is of magic type.
	 * @param fish Set to {@code true} if the shot is of fish type.
	 */
	public void rechargeShots(boolean physical, boolean magic, boolean fish)
	{
		// Dummy method to be overriden.
	}
	
	/**
	 * Sets whether the cursor follows movement keys.<br>
	 * Use {@code true} to enable this behavior.<br>
	 * Use {@code false} to disable this behavior.
	 * @param value The boolean state for cursor key movement.
	 */
	public void setCursorKeyMovement(boolean value)
	{
		_cursorKeyMovement = value;
	}
	
	/**
	 * Checks if the current input is a cursor key movement.<br>
	 * This helps identify when a player is moving using arrow keys.
	 * @return {@code true} if the input is a cursor key movement, {@code false} otherwise.
	 */
	public boolean isCursorKeyMovement()
	{
		return _cursorKeyMovement;
	}
	
	/**
	 * Updates the coordinates of the entity and synchronizes them with move data.<br>
	 * This method sets the {@code x}, {@code y}, and {@code z} values.<br>
	 * It also updates the accurate coordinates in the {@code MoveData} object if it exists.
	 * @param x The new X coordinate.
	 * @param y The new Y coordinate.
	 * @param z The new Z coordinate.
	 */
	public void setSyncedXYZ(int x, int y, int z)
	{
		setXYZ(x, y, z);
		
		final MoveData move = _move;
		if (move != null)
		{
			move.xAccurate = x;
			move.yAccurate = y;
		}
	}
	
	/**
	 * Retrieves the list of items dropped by fake players.<br>
	 * This method returns the internal collection of {@code Item} objects.
	 * @return a {@code List} of {@code Item} objects.
	 */
	public List<Item> getFakePlayerDrops()
	{
		return _fakePlayerDrops;
	}
	
	/**
	 * Adds a new buff information object to the finish task list.<br>
	 * This method ensures that the buff will be processed for completion.
	 * @param info The {@code BuffInfo} object to be added.
	 */
	public void addBuffInfoTime(BuffInfo info)
	{
		_buffFinishTask.addBuffInfo(info);
	}
	
	/**
	 * Removes the specific buff information from the finish task scheduler.<br>
	 * This method ensures that a buff no longer triggers its completion logic.
	 * @param info The {@code BuffInfo} object to be removed.
	 */
	public void removeBuffInfoTime(BuffInfo info)
	{
		_buffFinishTask.removeBuffInfo(info);
	}
}
