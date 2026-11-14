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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.config.custom.PrivateStoreRangeConfig;
import org.l2jmobius.gameserver.config.custom.PvpAnnounceConfig;
import org.l2jmobius.gameserver.config.custom.PvpRewardItemConfig;
import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.data.xml.DynamicExpRateData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.entity.actor.enums.npc.RaidBossStatus;
import org.l2jmobius.gameserver.entity.actor.enums.player.GroupType;
import org.l2jmobius.gameserver.entity.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.entity.actor.instance.Fisherman;
import org.l2jmobius.gameserver.entity.actor.instance.Merchant;
import org.l2jmobius.gameserver.entity.actor.instance.Teleporter;
import org.l2jmobius.gameserver.entity.actor.instance.Warehouse;
import org.l2jmobius.gameserver.entity.actor.stat.NpcStat;
import org.l2jmobius.gameserver.entity.actor.status.NpcStatus;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.entity.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.entity.zone.type.TaxZone;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.DatabaseSpawnManager;
import org.l2jmobius.gameserver.managers.FakePlayerChatManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcCanBeSeen;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcDespawn;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcEventReceived;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSkillFinished;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSpawn;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcTeleport;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.script.QuestTimer;
import org.l2jmobius.gameserver.mechanics.script.timers.TimerHolder;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.CastleTaxType;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.variables.NpcVariables;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeNpcState;
import org.l2jmobius.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;
import org.l2jmobius.gameserver.network.serverpackets.ExShowChannelingEffect;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerInfo;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfoAbnormalVisualEffect;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgSell;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopMsg;
import org.l2jmobius.gameserver.network.serverpackets.Ride;
import org.l2jmobius.gameserver.network.serverpackets.ServerObjectInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.ItemsAutoDestroyTaskManager;
import org.l2jmobius.gameserver.taskmanagers.WalkingTaskManager;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class represents a Non-Player-Creature in the world.<br>
 * It can be a monster or a friendly creature.<br>
 * It uses a template to fetch some static values.
 */
public class Npc extends Creature
{
	/** The interaction distance of the Npc(is used as offset in MovetoLocation method) */
	public static final int INTERACTION_DISTANCE = 250;
	/** Maximum distance where the drop may appear given this NPC position. */
	public static final int RANDOM_ITEM_DROP_LIMIT = 70;
	/** Ids of NPCs that see creatures through the OnCreatureSee event. */
	private static final Set<Integer> CREATURE_SEE_IDS = ConcurrentHashMap.newKeySet();
	/** The Spawn object that manage this Npc */
	private Spawn _spawn;
	/** The flag to specify if this Npc is busy */
	private boolean _isBusy = false;
	/** True if endDecayTask has already been called */
	private volatile boolean _isDecayed = false;
	/** True if this Npc is autoattackable **/
	private boolean _isAutoAttackable = false;
	/** Time of last social packet broadcast */
	private long _lastSocialBroadcast = 0;
	/** Minimum interval between social packets */
	private static final int MINIMUM_SOCIAL_INTERVAL = 6000;
	/** Time of next random animation */
	private long _nextRandomAnimationTime = 0;
	/** Support for random animation switching */
	private boolean _isRandomAnimationEnabled = true;
	private boolean _isRandomWalkingEnabled = true;
	private boolean _isWalker = false;
	private boolean _isTalkable = getTemplate().isTalkable();
	private final boolean _isQuestMonster = getTemplate().isQuestMonster();
	private final boolean _isFakePlayer = getTemplate().isFakePlayer();
	private boolean _dismounted = false; // A mounted fake player that has been dismounted at runtime.
	
	private int _currentLHandId; // Normally this shouldn't change from the template, but there exist exceptions.
	private int _currentRHandId; // Normally this shouldn't change from the template, but there exist exceptions.
	private int _currentEnchant; // Normally this shouldn't change from the template, but there exist exceptions.
	private float _currentCollisionHeight; // Used for npc grow effect skills.
	private float _currentCollisionRadius; // Used for npc grow effect skills.
	
	private int _soulshotamount = 0;
	private int _spiritshotamount = 0;
	private int _displayEffect = 0;
	
	private int _killingBlowWeaponId;
	
	private int _cloneObjId; // Used in NpcInfo packet to clone the specified player.
	private int _clanId; // Used in NpcInfo packet to show the specified clan.
	
	private NpcStringId _titleString;
	private NpcStringId _nameString;
	
	private StatSet _params;
	private volatile int _scriptValue = 0;
	private RaidBossStatus _raidStatus;
	
	/** Contains information about local tax payments. */
	private TaxZone _taxZone = null;
	
	private final List<QuestTimer> _questTimers = new ArrayList<>();
	private final List<TimerHolder<?>> _timerHolders = new ArrayList<>();
	
	private final Set<Player> _talkedPlayers = ConcurrentHashMap.newKeySet(0);
	
	/**
	 * Creates a new {@code Npc} instance based on the provided template.<br>
	 * This constructor initializes the NPC's stats, status, and equipment.<br>
	 * It also sets up collision data and flying properties.
	 * @param template The {@code NpcTemplate} used to define the NPC's properties.
	 */
	public Npc(NpcTemplate template)
	{
		// Call the Creature constructor to set the _template of the Creature, copy skills from template to object.
		// and link _calculators to NPC_STD_CALCULATOR.
		super(template);
		setInstanceType(InstanceType.Npc);
		initCharStatusUpdateValues();
		setTargetable(getTemplate().isTargetable());
		
		// Initialize the "current" equipment.
		_currentLHandId = getTemplate().getLHandId();
		_currentRHandId = getTemplate().getRHandId();
		_currentEnchant = NpcConfig.ENABLE_RANDOM_ENCHANT_EFFECT ? Rnd.get(4, 21) : getTemplate().getWeaponEnchant();
		
		// Initialize the "current" collisions.
		_currentCollisionHeight = getTemplate().getFCollisionHeight();
		_currentCollisionRadius = getTemplate().getFCollisionRadius();
		setFlying(template.isFlying());
		initStatusUpdateCache();
	}
	
	/**
	 * Triggers a social action animation for nearby players.<br>
	 * This method broadcasts the animation to all known players.<br>
	 * It ensures that actions are only sent after a minimum time interval has passed.
	 * @param animationId The unique identifier of the animation to play.
	 */
	public void onRandomAnimation(int animationId)
	{
		// Send a packet SocialAction to all Player in the _KnownPlayers of the Npc.
		final long now = System.currentTimeMillis();
		if ((now - _lastSocialBroadcast) > MINIMUM_SOCIAL_INTERVAL)
		{
			_lastSocialBroadcast = now;
			broadcastSocialAction(animationId);
		}
	}
	
	/**
	 * Retrieves the time the next random animation is due.
	 * @return The next random animation time in milliseconds.
	 */
	public long getNextRandomAnimationTime()
	{
		return _nextRandomAnimationTime;
	}
	
	/**
	 * Sets the time the next random animation is due.
	 * @param time The next random animation time in milliseconds.
	 */
	public void setNextRandomAnimationTime(long time)
	{
		_nextRandomAnimationTime = time;
	}
	
	/**
	 * Checks if the NPC is allowed to play random animations.<br>
	 * This depends on the server configuration and the AI type.
	 * @return {@code true} if random animations are enabled, {@code false} otherwise.
	 */
	public boolean hasRandomAnimation()
	{
		return ((GeneralConfig.MAX_NPC_ANIMATION > 0) && _isRandomAnimationEnabled && (getAiType() != AIType.CORPSE) && !isFakePlayer());
	}
	
	/**
	 * Enables or disables random animations for this NPC.<br>
	 * Sets the {@code _isRandomAnimationEnabled} flag to the provided {@code value}.
	 * @param value The boolean state to set for random animations.
	 */
	public void setRandomAnimation(boolean value)
	{
		_isRandomAnimationEnabled = value;
	}
	
	/**
	 * Checks if the random animation feature is currently active.<br>
	 * This determines whether the NPC will play random animations.
	 * @return {@code true} if enabled, {@code false} otherwise.
	 */
	public boolean isRandomAnimationEnabled()
	{
		return _isRandomAnimationEnabled;
	}
	
	/**
	 * Enables or disables the random walking behavior for this NPC.<br>
	 * Set to {@code true} to allow the NPC to move randomly.<br>
	 * Set to {@code false} to keep the NPC stationary.
	 * @param enabled The state of the random walking feature.
	 */
	public void setRandomWalking(boolean enabled)
	{
		_isRandomWalkingEnabled = enabled;
	}
	
	/**
	 * Checks if the random walking feature is currently active.<br>
	 * This determines whether the NPC will move randomly around its area.
	 * @return {@code true} if random walking is enabled, {@code false} otherwise.
	 */
	public boolean isRandomWalkingEnabled()
	{
		return _isRandomWalkingEnabled;
	}
	
	/**
	 * Retrieves the statistics for this NPC.<br>
	 * This method returns the {@code NpcStat} object associated with the entity.
	 * @return the {@code NpcStat} of the NPC.
	 */
	@Override
	public NpcStat getStat()
	{
		return (NpcStat) super.getStat();
	}
	
	/**
	 * Initializes the character statistics for this {@code Npc}.<br>
	 * This method creates and sets a new {@code NpcStat} object.
	 */
	@Override
	public void initCharStat()
	{
		setStat(new NpcStat(this));
	}
	
	/**
	 * Retrieves the current status of this {@code Npc}.<br>
	 * This method returns the status inherited from the parent class.
	 * @return the current {@code NpcStatus} of the entity.
	 */
	@Override
	public NpcStatus getStatus()
	{
		return (NpcStatus) super.getStatus();
	}
	
	/**
	 * Initializes the status of this {@code Npc}.<br>
	 * This method creates a new {@code NpcStatus} object and assigns it to the current instance.
	 */
	@Override
	public void initCharStatus()
	{
		setStatus(new NpcStatus(this));
	}
	
	/**
	 * Retrieves the {@code NpcTemplate} associated with this NPC.<br>
	 * This template contains the base data for the entity.
	 * @return The {@code NpcTemplate} of this NPC.
	 */
	@Override
	public NpcTemplate getTemplate()
	{
		return (NpcTemplate) super.getTemplate();
	}
	
	/**
	 * Retrieves the unique identifier for this {@code Npc}.<br>
	 * This value is obtained from the associated {@code NpcTemplate}.
	 * @return The unique integer ID of the NPC.
	 */
	@Override
	public int getId()
	{
		return getTemplate().getId();
	}
	
	/**
	 * Checks if the NPC can be attacked by players.<br>
	 * This method returns a value based on the {@code NpcConfig} settings.
	 * @return {@code true} if the NPC is attackable, {@code false} otherwise.
	 */
	@Override
	public boolean canBeAttacked()
	{
		return NpcConfig.ALT_ATTACKABLE_NPCS;
	}
	
	/**
	 * Retrieves the level of this {@code Npc}.<br>
	 * This value is obtained from the associated {@code NpcTemplate}.
	 * @return The current level as an {@code int}.
	 */
	@Override
	public int getLevel()
	{
		return getTemplate().getLevel();
	}
	
	/**
	 * Checks if the NPC will attack players automatically.<br>
	 * This method returns {@code false} by default.
	 * @return {@code true} if the NPC is aggressive, {@code false} otherwise.
	 */
	public boolean isAggressive()
	{
		return false;
	}
	
	/**
	 * Gets the distance at which this NPC will notice and attack players.<br>
	 * This value is retrieved from the associated {@code NpcTemplate}.
	 * @return The aggro range as an {@code int}.
	 */
	public int getAggroRange()
	{
		return getTemplate().getAggroRange();
	}
	
	/**
	 * Checks if the specified {@code Npc} belongs to the same clan as this entity.<br>
	 * This method compares the clan data from both templates.
	 * @param npc The {@code Npc} to check against.
	 * @return {@code true} if they are in the same clan, {@code false} otherwise.
	 */
	public boolean isInMyClan(Npc npc)
	{
		return getTemplate().isClan(npc.getTemplate().getClans());
	}
	
	/**
	 * Checks if the entity belongs to the undead race.<br>
	 * This method retrieves the race from the {@code NpcTemplate}.
	 * @return {@code true} if the entity is an undead creature, {@code false} otherwise.
	 */
	@Override
	public boolean isUndead()
	{
		return getTemplate().getRace() == Race.UNDEAD;
	}
	
	/**
	 * Updates the visual effects for abnormal entities.<br>
	 * This method iterates through all visible players to send relevant packets.<br>
	 * It handles specific logic for fake players and stationary objects.
	 */
	@Override
	public void updateAbnormalVisualEffects()
	{
		World.forEachVisibleObject(this, Player.class, player ->
		{
			if (!isVisibleFor(player))
			{
				return;
			}
			
			if (_isFakePlayer)
			{
				player.sendPacket(new FakePlayerInfo(this));
				sendPrivateStoreMessage(player);
			}
			else if (getRunSpeed() == 0)
			{
				player.sendPacket(new ServerObjectInfo(this, player));
			}
			else
			{
				player.sendPacket(new NpcInfoAbnormalVisualEffect(this));
			}
		});
	}
	
	/**
	 * Sends the private store sign message of a fake player to a specific player.
	 * @param player The player to send the store message to.
	 */
	private void sendPrivateStoreMessage(Player player)
	{
		if (!_isFakePlayer)
		{
			return;
		}
		
		final FakePlayerHolder fakePlayerInfo = getTemplate().getFakePlayerInfo();
		final int storeType = fakePlayerInfo.getPrivateStoreType();
		if (storeType <= 0)
		{
			return;
		}
		
		final String message = fakePlayerInfo.getPrivateStoreMessage();
		if (message.isEmpty())
		{
			return;
		}
		
		switch (storeType)
		{
			case 1: // Sell
			{
				player.sendPacket(new PrivateStoreMsgSell(getObjectId(), message));
				break;
			}
			case 3: // Buy
			{
				player.sendPacket(new PrivateStoreMsgBuy(getObjectId(), message));
				break;
			}
			case 5: // Manufacture
			{
				player.sendPacket(new RecipeShopMsg(getObjectId(), message));
				break;
			}
			case 8: // Package Sell
			{
				player.sendPacket(new ExPrivateStoreSetWholeMsg(getObjectId(), message));
				break;
			}
		}
	}
	
	/**
	 * Checks if the specified creature can perform an auto-attack on this entity.<br>
	 * This method evaluates conditions such as clan membership and NPC hatred.
	 * @param attacker The {@code Creature} attempting to initiate the attack.
	 * @return {@code true} if the attack is permitted, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker == null)
		{
			return false;
		}
		
		// Summons can attack NPCs.
		if (attacker.isSummon())
		{
			return true;
		}
		
		if (!isTargetable())
		{
			return false;
		}
		
		if (attacker.isAttackable())
		{
			if (isInMyClan(attacker.asNpc()))
			{
				return false;
			}
			
			// Chaos NPCs attack everything except clan.
			if (((NpcTemplate) attacker.getTemplate()).isChaos())
			{
				return true;
			}
			
			// Usually attackables attack everything they hate.
			return attacker.asAttackable().getHating(this) > 0;
		}
		
		return _isAutoAttackable;
	}
	
	/**
	 * Sets whether the entity can be targeted for auto-attacks.<br>
	 * This determines if other creatures will automatically attack this actor.
	 * @param flag The boolean value to set for auto-attack status.
	 */
	public void setAutoAttackable(boolean flag)
	{
		_isAutoAttackable = flag;
	}
	
	/**
	 * Retrieves the ID of the item held in the left hand.<br>
	 * This method returns the current {@code int} identifier for the left-hand equipment.
	 * @return The unique identifier of the left-hand item.
	 */
	public int getLeftHandItem()
	{
		return _currentLHandId;
	}
	
	/**
	 * Retrieves the ID of the item held in the right hand.
	 * @return The {@code int} identifier of the right-hand item.
	 */
	public int getRightHandItem()
	{
		return _currentRHandId;
	}
	
	/**
	 * Retrieves the current enchantment effect of the NPC.<br>
	 * This value represents the specific visual or functional effect applied to the entity.
	 * @return The integer ID of the current enchantment effect.
	 */
	public int getEnchantEffect()
	{
		return _currentEnchant;
	}
	
	/**
	 * Checks if the actor is currently busy.
	 * @return {@code true} if the actor is busy, {@code false} otherwise.
	 */
	public boolean isBusy()
	{
		return _isBusy;
	}
	
	/**
	 * Updates the busy status of this actor.<br>
	 * Sets the internal state to {@code true} or {@code false}.
	 * @param isBusy The new busy status to apply.
	 */
	public void setBusy(boolean isBusy)
	{
		_isBusy = isBusy;
	}
	
	/**
	 * Checks if the current entity is a warehouse.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the entity is a warehouse, otherwise {@code false}.
	 */
	public boolean isWarehouse()
	{
		return false;
	}
	
	/**
	 * Checks if a {@code Player} can target this entity.<br>
	 * It verifies if the player's controls are blocked.<br>
	 * It also checks if the player has a locked target on another entity.
	 * @param player The {@code Player} attempting to target this entity.
	 * @return {@code true} if the player can target this entity, otherwise {@code false}.
	 */
	public boolean canTarget(Player player)
	{
		if (player.isControlBlocked())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (player.isLockedTarget() && (player.getLockedTarget() != this))
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CHANGE_ENMITY);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if the player is allowed to interact with this entity.<br>
	 * It verifies the player's state and distance from the target.
	 * @param player The {@code Player} attempting to perform the interaction.
	 * @return {@code true} if the interaction is permitted, {@code false} otherwise.
	 */
	public boolean canInteract(Player player)
	{
		if (player.isCastingNow())
		{
			return false;
		}
		else if (player.isDead() || player.isFakeDeath())
		{
			return false;
		}
		else if (player.isSitting() && (getId() != 34200)) // Mystic Tavern Globe requires player sitting.
		{
			return false;
		}
		else if (player.isInStoreMode())
		{
			return false;
		}
		else if (!isInsideRadius3D(player, INTERACTION_DISTANCE))
		{
			return false;
		}
		else if (player.getInstanceWorld() != getInstanceWorld())
		{
			return false;
		}
		else if (_isBusy)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Sets the tax zone for this entity.<br>
	 * This value is only applied if the entity is not currently in an instance.<br>
	 * If the provided {@code zone} is {@code null}, it will be set to {@code null}.
	 * @param zone The {@code TaxZone} to assign.
	 */
	public void setTaxZone(TaxZone zone)
	{
		_taxZone = ((zone != null) && !isInInstance()) ? zone : null;
	}
	
	/**
	 * Retrieves the {@code Castle} associated with the current tax zone.<br>
	 * It returns {@code null} if no tax zone is defined.
	 * @return The {@code Castle} object or {@code null}.
	 */
	public Castle getTaxCastle()
	{
		return (_taxZone != null) ? _taxZone.getCastle() : null;
	}
	
	/**
	 * Retrieves the tax rate for a specific type from the current castle.<br>
	 * If no castle is active, it returns {@code 0}.
	 * @param type The category of tax to calculate.
	 * @return The tax rate as a double value between {@code 0} and {@code 1}.
	 */
	public double getCastleTaxRate(CastleTaxType type)
	{
		final Castle castle = getTaxCastle();
		return (castle != null) ? (castle.getTaxPercent(type) / 100.0) : 0;
	}
	
	/**
	 * Processes a tax payment for the castle.<br>
	 * This method adds the specified amount to the treasury if a {@code Castle} exists.
	 * @param amount The amount of currency to be paid.
	 */
	public void handleTaxPayment(long amount)
	{
		final Castle taxCastle = getTaxCastle();
		if (taxCastle != null)
		{
			taxCastle.addToTreasury(amount);
		}
	}
	
	/**
	 * Retrieves the nearest castle for this entity.<br>
	 * It uses the {@code CastleManager} to perform the search.
	 * @return the nearest {@code Castle} object or {@code null} if none is found.
	 */
	public Castle getCastle()
	{
		return CastleManager.getInstance().findNearestCastle(this);
	}
	
	/**
	 * Retrieves the {@code ClanHall} associated with this NPC.<br>
	 * It first checks for a hall linked to any active {@code ClanHallZone}.<br>
	 * If no zone is found, it falls back to searching by the NPC ID.
	 * @return the {@code ClanHall} object or {@code null} if not found.
	 */
	public ClanHall getClanHall()
	{
		final int id = getId();
		if (id == 33360) // Provisional Hall Manager
		{
			for (ZoneType zone : ZoneManager.getInstance().getZones(this))
			{
				if (zone instanceof ClanHallZone)
				{
					final ClanHall clanHall = ClanHallData.getInstance().getClanHallById(((ClanHallZone) zone).getResidenceId());
					if (clanHall != null)
					{
						return clanHall;
					}
				}
			}
		}
		
		return ClanHallData.getInstance().getClanHallByNpcId(id);
	}
	
	/**
	 * Finds the nearest {@code Castle} to this actor.<br>
	 * It searches within a specific range defined by the distance.
	 * @param maxDistance The maximum distance to search for a castle.
	 * @return The nearest {@code Castle} found, or {@code null} if none exist nearby.
	 */
	public Castle getCastle(long maxDistance)
	{
		return CastleManager.getInstance().findNearestCastle(this, maxDistance);
	}
	
	/**
	 * Retrieves the nearest fort for this entity.<br>
	 * It uses the {@code FortManager} to perform the search.
	 * @return the nearest {@code Fort} object or {@code null} if none is found.
	 */
	public Fort getFort()
	{
		return FortManager.getInstance().findNearestFort(this);
	}
	
	/**
	 * Finds the nearest fort within a specific range.<br>
	 * This method uses {@code FortManager} to locate the closest fort relative to this actor.
	 * @param maxDistance The maximum distance to search for a fort.
	 * @return The nearest {@code Fort} found, or {@code null} if none exist in range.
	 */
	public Fort getFort(long maxDistance)
	{
		return FortManager.getInstance().findNearestFort(this, maxDistance);
	}
	
	/**
	 * Handles the interaction when a player uses a bypass command on an NPC.<br>
	 * This method checks if the {@code player} can interact with this entity.<br>
	 * It then executes the corresponding logic from the {@code BypassHandler}.<br>
	 * If no handler is found, it logs the unknown command to the server console.
	 * @param player The {@code Player} who initiated the interaction.
	 * @param command The specific bypass string provided by the player.
	 */
	public void onBypassFeedback(Player player, String command)
	{
		if (canInteract(player))
		{
			final IBypassHandler handler = BypassHandler.getInstance().getHandler(command);
			if (handler != null)
			{
				handler.onCommand(command, player, this);
			}
			else
			{
				LOGGER.info(getClass().getSimpleName() + ": Unknown NPC bypass: \"" + command + "\" NpcId: " + getId());
			}
		}
	}
	
	/**
	 * Retrieves the current active weapon instance for the actor.<br>
	 * This method returns {@code null} if no weapon is currently equipped.
	 * @return the active {@code Item} instance or {@code null}.
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the weapon currently held by the actor.<br>
	 * This method returns {@code null} if no weapon is equipped.
	 * @return the current {@code Weapon} object or {@code null}.
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon instance for the actor.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Item} representing the secondary weapon or {@code null} if none exists.
	 */
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon item for the actor.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Weapon} object representing the secondary weapon.
	 */
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	/**
	 * Retrieves the file path for an NPC's HTML response.<br>
	 * It constructs a path based on the provided ID and value.<br>
	 * If no valid file is found, it returns the default message path.
	 * @param npcId The unique identifier of the NPC.
	 * @param value An additional value used to differentiate the HTML file.
	 * @param player The player interacting with the NPC.
	 * @return The relative path to the HTML file as a {@code String}.
	 */
	public String getHtmlPath(int npcId, int value, Player player)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		
		final String temp = "data/html/default/" + pom + ".htm";
		if (GeneralConfig.HTM_CACHE)
		{
			// If not running lazy cache the file must be in the cache or it does not exist.
			if (HtmCache.getInstance().contains(temp))
			{
				return temp;
			}
		}
		else
		{
			final File file = new File(ServerConfig.DATAPACK_ROOT, temp);
			if (file.isFile())
			{
				final String lowerCaseName = file.getName().toLowerCase();
				if (lowerCaseName.endsWith(".htm") || lowerCaseName.endsWith(".html"))
				{
					return temp;
				}
			}
		}
		
		// If the file is not found, the standard message "I have nothing to say to you" is returned.
		return "data/html/npcdefault.htm";
	}
	
	/**
	 * Opens the chat window for a specific player.<br>
	 * This method displays the standard chat interface to the user.
	 * @param player The {@code Player} who will see the chat window.
	 */
	public void showChatWindow(Player player)
	{
		if (_talkedPlayers.contains(player))
		{
			return;
		}
		
		_talkedPlayers.add(player);
		ThreadPool.schedule(() -> _talkedPlayers.remove(player), 1000);
		
		showChatWindow(player, 0);
	}
	
	/**
	 * Checks if a PK deny chat window can be displayed for the player.<br>
	 * It attempts to load an HTML file based on the provided {@code type}.<br>
	 * If the file exists, it sends the message to the {@code Player}.
	 * @param player The {@code Player} who will receive the chat window.
	 * @param type The category of the PK deny message.
	 * @return {@code true} if the window was successfully sent, {@code false} otherwise.
	 */
	private boolean showPkDenyChatWindow(Player player, String type)
	{
		String html = HtmCache.getInstance().getHtm(player, "data/html/" + type + "/" + getId() + "-pk.htm");
		if (html != null)
		{
			final int objectId = getObjectId();
			html = html.replace("%objectId%", String.valueOf(objectId));
			player.sendPacket(new NpcHtmlMessage(objectId, html));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Displays the chat window for an NPC to a specific player.<br>
	 * This method handles logic for reputation checks and selects the correct HTML file based on the NPC ID.<br>
	 * It sends the resulting {@code NpcHtmlMessage} packet to the player.
	 * @param player The {@code Player} who will view the chat window.
	 * @param value The page index or specific identifier for the content to display.
	 */
	public void showChatWindow(Player player, int value)
	{
		if (!_isTalkable)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.getReputation() < 0)
		{
			if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (this instanceof Merchant))
			{
				if (showPkDenyChatWindow(player, "merchant"))
				{
					return;
				}
			}
			else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && (this instanceof Teleporter))
			{
				if (showPkDenyChatWindow(player, "teleporter"))
				{
					return;
				}
			}
			else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && (this instanceof Warehouse))
			{
				if (showPkDenyChatWindow(player, "warehouse"))
				{
					return;
				}
			}
			else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (this instanceof Fisherman))
			{
				if (showPkDenyChatWindow(player, "fisherman"))
				{
					return;
				}
			}
		}
		
		if (getTemplate().isType("Auctioneer") && (value == 0))
		{
			return;
		}
		
		final int npcId = getTemplate().getId();
		String filename;
		switch (npcId)
		{
			case 31690:
			case 31769:
			case 31770:
			case 31771:
			case 31772:
			{
				if (player.isHero() || (player.getNobleLevel() > 0))
				{
					filename = Olympiad.OLYMPIAD_HTML_PATH + "hero_main.htm";
				}
				else
				{
					filename = (getHtmlPath(npcId, value, player));
				}
				break;
			}
			case 30298: // Blacksmith Pinter
			{
				if (player.isAcademyMember())
				{
					filename = (getHtmlPath(npcId, 1, player));
				}
				else
				{
					filename = (getHtmlPath(npcId, value, player));
				}
				break;
			}
			default:
			{
				if (((npcId >= 31093) && (npcId <= 31094)) || ((npcId >= 31172) && (npcId <= 31201)) || ((npcId >= 31239) && (npcId <= 31254)))
				{
					return;
				}
				
				// Get the text of the selected HTML file in function of the npcId and of the page number.
				filename = (getHtmlPath(npcId, value, player));
				break;
			}
		}
		
		// Send a Server->Client NpcHtmlMessage containing the text of the Npc to the Player.
		final int objectId = getObjectId();
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		html.setFile(player, filename);
		html.replace("%npcname%", getName());
		html.replace("%objectId%", String.valueOf(objectId));
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet.
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Displays an HTML window to a specific player.<br>
	 * This method sends an {@code NpcHtmlMessage} containing the specified file content.<br>
	 * It also sends a static packet to prevent the client from waiting for further actions.
	 * @param player The {@code Player} who will see the chat window.
	 * @param filename The name of the file to display in the window.
	 */
	public void showChatWindow(Player player, String filename)
	{
		// Send a Server->Client NpcHtmlMessage containing the text of the Npc to the Player.
		final int objectId = getObjectId();
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(objectId));
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet.
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Calculates the experience reward for a specific level.<br>
	 * This method checks if dynamic experience rates are enabled.<br>
	 * If disabled, it uses the global experience rate from {@code RatesConfig}.
	 * @param level The character level to calculate the reward for.
	 * @return The calculated experience value as a {@code double}.
	 */
	public double getExpReward(int level)
	{
		if (DynamicExpRateData.getInstance().isEnabled())
		{
			return getTemplate().getExp() * DynamicExpRateData.getInstance().getDynamicExpRate(level);
		}
		
		final Instance instance = getInstanceWorld();
		final float rateMul = instance != null ? instance.getExpRate() : RatesConfig.RATE_XP;
		return getTemplate().getExp() * rateMul;
	}
	
	/**
	 * Calculates the SP reward for a specific level.<br>
	 * This method checks if dynamic experience rates are enabled.<br>
	 * It applies the correct multiplier based on the character's level or global settings.
	 * @param level The level of the character to calculate the reward for.
	 * @return The calculated SP reward as a double.
	 */
	public double getSpReward(int level)
	{
		if (DynamicExpRateData.getInstance().isEnabled())
		{
			return getTemplate().getSP() * DynamicExpRateData.getInstance().getDynamicSpRate(level);
		}
		
		final Instance instance = getInstanceWorld();
		final float rateMul = instance != null ? instance.getSPRate() : RatesConfig.RATE_SP;
		return getTemplate().getSP() * rateMul;
	}
	
	/**
	 * Restores MP to the given creature over time, based on this NPC's mana reward settings.
	 * @param creature the creature receiving the mana reward
	 */
	private void rewardMana(Creature creature)
	{
		final NpcTemplate template = getTemplate();
		final int ticks = template.getManaRewardTicks();
		final double tickValue = template.getManaRewardType() == StatModifierType.PER ? (creature.getMaxMp() * (template.getManaRewardValue() / 100d)) / ticks : template.getManaRewardValue() / ticks;
		final AtomicInteger count = new AtomicInteger(ticks);
		final ScheduledFuture<?>[] task = new ScheduledFuture<?>[1];
		task[0] = ThreadPool.scheduleAtFixedRate(() ->
		{
			if ((count.decrementAndGet() <= 0) || (creature.isPlayer() && !creature.asPlayer().isOnline()))
			{
				task[0].cancel(false);
				return;
			}
			
			creature.setCurrentMp(creature.getCurrentMp() + tickValue);
		}, PlayerConfig.EFFECT_TICK_RATIO, PlayerConfig.EFFECT_TICK_RATIO);
	}
	
	/**
	 * Handles the logic for when a creature dies.<br>
	 * It resets weapon data and processes rewards or penalties based on the killer.<br>
	 * This method also handles corpse decay and notifies spawn templates.
	 * @param killer The {@code Creature} that killed this entity.
	 * @return {@code true} if the death was successful, otherwise {@code false}.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		// Normally this wouldn't really be needed, but for those few exceptions,
		// we do need to reset the weapons back to the initial template weapon.
		final NpcTemplate template = getTemplate();
		_currentLHandId = template.getLHandId();
		_currentRHandId = template.getRHandId();
		_currentCollisionHeight = template.getFCollisionHeight();
		_currentCollisionRadius = template.getFCollisionRadius();
		
		final Weapon weapon = (killer != null) ? killer.getActiveWeaponItem() : null;
		_killingBlowWeaponId = (weapon != null) ? weapon.getId() : 0;
		if (_isFakePlayer && (killer != null) && killer.isPlayable())
		{
			final Player player = killer.asPlayer();
			if (isScriptValue(0) && (getReputation() >= 0))
			{
				if (FakePlayersConfig.FAKE_PLAYER_KILL_KARMA)
				{
					player.setReputation(player.getReputation() - Formulas.calculateKarmaGain(player.getPkKills(), killer.isSummon()));
					player.setPkKills(player.getPkKills() + 1);
					player.broadcastUserInfo(UserInfoType.SOCIAL);
					player.checkItemRestriction();
					
					// pk item rewards
					if (PvpRewardItemConfig.REWARD_PK_ITEM)
					{
						if (!(PvpRewardItemConfig.DISABLE_REWARDS_IN_INSTANCES && (getInstanceId() != 0)) && //
							!(PvpRewardItemConfig.DISABLE_REWARDS_IN_PVP_ZONES && isInsideZone(ZoneId.PVP)))
						{
							player.addItem(ItemProcessType.REWARD, PvpRewardItemConfig.REWARD_PK_ITEM_ID, PvpRewardItemConfig.REWARD_PK_ITEM_AMOUNT, this, PvpRewardItemConfig.REWARD_PK_ITEM_MESSAGE);
						}
					}
					
					// announce pk
					if (PvpAnnounceConfig.ANNOUNCE_PK_PVP && !player.isGM())
					{
						final String msg = PvpAnnounceConfig.ANNOUNCE_PK_MSG.replace("$killer", player.getName()).replace("$target", getName());
						if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.S1_3);
							sm.addString(msg);
							World.broadcastToAllOnlinePlayers(sm);
						}
						else
						{
							World.broadcastToAllOnlinePlayers(msg, false);
						}
					}
				}
			}
			else if (FakePlayersConfig.FAKE_PLAYER_KILL_PVP)
			{
				player.setPvpKills(player.getPvpKills() + 1);
				player.setTotalKills(player.getTotalKills() + 1);
				player.broadcastUserInfo(UserInfoType.SOCIAL);
				
				// pvp item rewards
				if (PvpRewardItemConfig.REWARD_PVP_ITEM)
				{
					if (!(PvpRewardItemConfig.DISABLE_REWARDS_IN_INSTANCES && (getInstanceId() != 0)) && //
						!(PvpRewardItemConfig.DISABLE_REWARDS_IN_PVP_ZONES && isInsideZone(ZoneId.PVP)))
					{
						player.addItem(ItemProcessType.REWARD, PvpRewardItemConfig.REWARD_PVP_ITEM_ID, PvpRewardItemConfig.REWARD_PVP_ITEM_AMOUNT, this, PvpRewardItemConfig.REWARD_PVP_ITEM_MESSAGE);
					}
				}
				
				// announce pvp
				if (PvpAnnounceConfig.ANNOUNCE_PK_PVP && !player.isGM())
				{
					final String msg = PvpAnnounceConfig.ANNOUNCE_PVP_MSG.replace("$killer", player.getName()).replace("$target", getName());
					if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_3);
						sm.addString(msg);
						World.broadcastToAllOnlinePlayers(sm);
					}
					else
					{
						World.broadcastToAllOnlinePlayers(msg, false);
					}
				}
			}
		}
		
		DecayTaskManager.getInstance().add(this);
		
		if (_spawn != null)
		{
			final NpcSpawnTemplate npcTemplate = _spawn.getNpcSpawnTemplate();
			if (npcTemplate != null)
			{
				npcTemplate.notifyNpcDeath(this, killer);
			}
		}
		
		// Apply mana rewards.
		if ((template.getManaRewardValue() > 0) && (killer != null) && killer.isPlayable())
		{
			final Player killerPlayer = killer.asPlayer();
			rewardMana(killerPlayer);
			for (Summon summon : killerPlayer.getServitors().values())
			{
				rewardMana(summon);
			}
			
			final GroupType group = template.getManaRewardGroup();
			if (group != GroupType.NONE)
			{
				final Party party = killerPlayer.getParty();
				if (party != null)
				{
					final List<Player> members = (group == GroupType.COMMAND_CHANNEL) && party.isInCommandChannel() ? party.getCommandChannel().getMembers() : party.getMembers();
					for (Player member : members)
					{
						if ((member != killerPlayer) && (member.calculateDistance3D(getX(), getY(), getZ()) <= PlayerConfig.ALT_PARTY_RANGE))
						{
							rewardMana(member);
							for (Summon summon : member.getServitors().values())
							{
								rewardMana(summon);
							}
						}
					}
				}
			}
		}
		
		DatabaseSpawnManager.getInstance().updateStatus(this, true);
		return true;
	}
	
	/**
	 * Sets the {@code Spawn} object for this entity.<br>
	 * This method updates the internal {@code _spawn} field.
	 * @param spawn The {@code Spawn} object to set.
	 */
	public void setSpawn(Spawn spawn)
	{
		_spawn = spawn;
	}
	
	/**
	 * This method is called when the NPC is spawned into the world.<br>
	 * It initializes internal variables like shot amounts and animation flags.<br>
	 * It also handles event notifications and updates the walking manager.
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		// Recharge shots
		_soulshotamount = getTemplate().getSoulShot();
		_spiritshotamount = getTemplate().getSpiritShot();
		_killingBlowWeaponId = 0;
		_isRandomAnimationEnabled = getTemplate().isRandomAnimationEnabled();
		_isRandomWalkingEnabled = !WalkingTaskManager.getInstance().isTargeted(this) && getTemplate().isRandomWalkEnabled();
		if (isTeleporting())
		{
			if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_TELEPORT, this))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnNpcTeleport(this), this);
			}
		}
		else if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SPAWN, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcSpawn(this), this);
		}
		
		if (!isTeleporting())
		{
			WalkingTaskManager.getInstance().onSpawn(this);
		}
		
		if (isInsideZone(ZoneId.TAX) && (getCastle() != null) && (NpcConfig.SHOW_CREST_WITHOUT_QUEST || getCastle().getShowNpcCrest()) && (getCastle().getOwnerId() != 0))
		{
			setClanId(getCastle().getOwnerId());
		}
		
		if (CREATURE_SEE_IDS.contains(getId()))
		{
			initSeenCreatures();
		}
		
		// Fake players say random lines.
		if (_isFakePlayer)
		{
			FakePlayerChatManager.getInstance().scheduleRandomChat(this);
		}
	}
	
	/**
	 * Adds a specific creature ID to the global visibility list.<br>
	 * This allows the system to recognize the creature for sight-related logic.
	 * @param id The unique identifier of the creature to add.
	 */
	public static void addCreatureSeeId(int id)
	{
		CREATURE_SEE_IDS.add(id);
	}
	
	/**
	 * This method is called when an NPC respawns.<br>
	 * It resets the NPC to its default alive state.<br>
	 * All effects, stats, and variables are cleared or restored.
	 */
	public void onRespawn()
	{
		// Make it alive.
		setDead(false);
		
		// Stop all effects and recalculate stats without broadcasting.
		getEffectList().stopAllEffects(false);
		
		// Reset decay info.
		setDecayed(false);
		
		// Fully heal NPC.
		fullRestore();
		
		// Clear script variables.
		if (hasVariables())
		{
			getVariables().getSet().clear();
		}
		
		// Reset targetable state.
		setTargetable(getTemplate().isTargetable());
		
		// Reset summoner
		setSummoner(null);
		
		// Reset summoned list.
		resetSummonedNpcs();
		
		// Reset NpcStringId for name.
		_nameString = null;
		
		// Reset NpcStringId for title.
		_titleString = null;
		
		// Reset parameters
		_params = null;
	}
	
	/**
	 * Handles the logic for when an {@code Npc} decays or despawns.<br>
	 * This method marks the entity as decayed and removes it from the world.<br>
	 * It also updates spawn counters, notifies managers, and clears active timers.
	 */
	@Override
	public void onDecay()
	{
		if (_isDecayed)
		{
			return;
		}
		
		setDecayed(true);
		
		// Remove the Npc from the world when the decay task is launched.
		super.onDecay();
		
		// Decrease its spawn counter.
		if ((_spawn != null) && !DatabaseSpawnManager.getInstance().isDefined(getId()))
		{
			_spawn.decreaseCount(this);
		}
		
		// Notify Walking Manager.
		WalkingTaskManager.getInstance().onDeath(this);
		
		// Notify DP scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_DESPAWN, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcDespawn(this), this);
		}
		
		// Remove from instance world.
		final Instance instance = getInstanceWorld();
		if (instance != null)
		{
			instance.removeNpc(this);
		}
		
		// Stop all timers.
		stopQuestTimers();
		stopTimerHolders();
		
		// Clear script value.
		_scriptValue = 0;
	}
	
	/**
	 * Removes this entity from the game world.<br>
	 * This method handles cleanup tasks like decaying and removing zone registrations.
	 * @return {@code true} if the deletion was successful, or {@code false} otherwise.
	 */
	@Override
	public boolean deleteMe()
	{
		try
		{
			onDecay();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed decayMe().", e);
		}
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		
		ZoneManager.getInstance().getRegion(this).removeFromZones(this);
		
		return super.deleteMe();
	}
	
	/**
	 * Retrieves the current spawn information.
	 * @return the {@code Spawn} object.
	 */
	public Spawn getSpawn()
	{
		return _spawn;
	}
	
	/**
	 * Checks if the entity has decayed.
	 * @return {@code true} if the entity is decayed, {@code false} otherwise.
	 */
	public boolean isDecayed()
	{
		return _isDecayed;
	}
	
	/**
	 * Sets whether the entity has decayed.<br>
	 * This updates the {@code _isDecayed} state of the actor.
	 * @param decayed The decay status to set. Use {@code true} if decayed and {@code false} otherwise.
	 */
	public void setDecayed(boolean decayed)
	{
		_isDecayed = decayed;
	}
	
	/**
	 * Stops the decay process for this entity.<br>
	 * It cancels the active task in {@code DecayTaskManager}.<br>
	 * It then triggers the {@code onDecay()} method if the entity is not already decayed.
	 */
	public void endDecayTask()
	{
		if (!_isDecayed)
		{
			DecayTaskManager.getInstance().cancel(this);
			onDecay();
		}
	}
	
	// Two functions to change the appearance of the equipped weapons on the NPC.
	// This is only useful for a few NPCs and is most likely going to be called from AI.
	/**
	 * Sets the unique identifier for the item held in the left hand.<br>
	 * This method updates the internal ID and broadcasts the change to other players.
	 * @param newWeaponId The {@code int} ID of the new weapon or item.
	 */
	public void setLHandId(int newWeaponId)
	{
		_currentLHandId = newWeaponId;
		broadcastInfo();
	}
	
	/**
	 * Sets the unique identifier for the weapon held in the right hand.<br>
	 * This method updates the internal ID and broadcasts the change to other clients.
	 * @param newWeaponId The {@code int} ID of the new weapon to equip.
	 */
	public void setRHandId(int newWeaponId)
	{
		_currentRHandId = newWeaponId;
		broadcastInfo();
	}
	
	/**
	 * Updates the weapon IDs for both hands.<br>
	 * This method sets the new identifiers and broadcasts the change.
	 * @param newLWeaponId The ID of the item in the left hand.
	 * @param newRWeaponId The ID of the item in the right hand.
	 */
	public void setLRHandId(int newLWeaponId, int newRWeaponId)
	{
		_currentRHandId = newRWeaponId;
		_currentLHandId = newLWeaponId;
		broadcastInfo();
	}
	
	/**
	 * Updates the current enchantment value of the NPC.<br>
	 * This method also broadcasts the updated information to all players.
	 * @param newEnchantValue The new integer value for the enchantment.
	 */
	public void setEnchant(int newEnchantValue)
	{
		_currentEnchant = newEnchantValue;
		broadcastInfo();
	}
	
	/**
	 * Checks if the name of this NPC should be displayed.<br>
	 * This value is retrieved from the associated {@code NpcTemplate}.
	 * @return {@code true} if the name is visible, {@code false} otherwise.
	 */
	public boolean isShowName()
	{
		return getTemplate().isShowName();
	}
	
	/**
	 * Sets the collision height for this entity.<br>
	 * This value determines how high an object is considered to be for physical interactions.
	 * @param height The new collision height value.
	 */
	public void setCollisionHeight(float height)
	{
		_currentCollisionHeight = height;
	}
	
	/**
	 * Sets the collision radius for this entity.<br>
	 * This value determines how much space the object occupies during physical interactions.
	 * @param radius The size of the collision area as a {@code float}.
	 */
	public void setCollisionRadius(float radius)
	{
		_currentCollisionRadius = radius;
	}
	
	/**
	 * Returns the current height of the collision box.<br>
	 * This value is used to determine where the entity touches the ground.
	 * @return The current collision height as a {@code float}.
	 */
	@Override
	public float getCollisionHeight()
	{
		return _currentCollisionHeight;
	}
	
	/**
	 * Gets the current collision radius of this entity.<br>
	 * This value determines the physical size used for collision detection.
	 * @return The current collision radius as a {@code float}.
	 */
	@Override
	public float getCollisionRadius()
	{
		return _currentCollisionRadius;
	}
	
	/**
	 * Sends relevant information about this entity to the specified {@code Player}.<br>
	 * This method checks if the player can see the entity before sending packets.<br>
	 * It handles different packet types based on whether the entity is a fake player or an NPC.
	 * @param player The {@code Player} who will receive the information.
	 */
	@Override
	public void sendInfo(Player player)
	{
		if (isVisibleFor(player))
		{
			if (_isFakePlayer)
			{
				player.sendPacket(new FakePlayerInfo(this));
				sendPrivateStoreMessage(player);
			}
			else if (getRunSpeed() == 0)
			{
				player.sendPacket(new ServerObjectInfo(this, player));
			}
			else
			{
				player.sendPacket(new NpcInfo(this));
			}
		}
	}
	
	/**
	 * Schedules the removal of this entity after a specific time.<br>
	 * The {@code deleteMe()} method will be called if the entity has not already decayed.
	 * @param delay The amount of time to wait before despawning in milliseconds.
	 */
	public void scheduleDespawn(long delay)
	{
		ThreadPool.schedule(() ->
		{
			if (!_isDecayed)
			{
				deleteMe();
			}
		}, delay);
	}
	
	/**
	 * Notifies the system that an NPC has finished using a specific skill.<br>
	 * This method checks if there is a valid {@code target} and if listeners are registered.<br>
	 * It then dispatches the event asynchronously to all relevant listeners.
	 * @param skill The {@code Skill} that was just completed.
	 * @param target The {@code WorldObject} that was affected by the skill.
	 */
	@Override
	public void notifyQuestEventSkillFinished(Skill skill, WorldObject target)
	{
		if ((target != null) && EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SKILL_FINISHED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillFinished(this, target.asPlayer(), skill), this);
		}
	}
	
	/**
	 * Checks if the NPC is currently unable to move.<br>
	 * This returns {@code true} if movement is disabled by the parent class, if the template forbids movement, or if the AI type is a corpse.
	 * @return {@code true} if movement is restricted; {@code false} otherwise.
	 */
	@Override
	public boolean isMovementDisabled()
	{
		return super.isMovementDisabled() || !getTemplate().canMove() || (getAiType() == AIType.CORPSE);
	}
	
	/**
	 * Retrieves the artificial intelligence type for this NPC.<br>
	 * This value is fetched from the associated {@code NpcTemplate}.
	 * @return the {@code AIType} of the NPC.
	 */
	public AIType getAiType()
	{
		return getTemplate().getAIType();
	}
	
	/**
	 * Updates the visual display effect for this NPC.<br>
	 * This method sends a packet to notify clients of the change.
	 * @param value The new effect ID to apply.
	 */
	public void setDisplayEffect(int value)
	{
		if (value != _displayEffect)
		{
			_displayEffect = value;
			broadcastPacket(new ExChangeNpcState(getObjectId(), value));
		}
	}
	
	/**
	 * Checks if the current display effect matches a specific value.
	 * @param value The {@code int} value to compare against.
	 * @return {@code true} if the effect matches, otherwise {@code false}.
	 */
	public boolean hasDisplayEffect(int value)
	{
		return _displayEffect == value;
	}
	
	/**
	 * Retrieves the current display effect for this NPC.<br>
	 * This value determines which visual effect is shown to players.
	 * @return The integer ID of the display effect.
	 */
	public int getDisplayEffect()
	{
		return _displayEffect;
	}
	
	/**
	 * Retrieves the current color effect for this NPC.<br>
	 * This method returns a default value of {@code 0}.
	 * @return The integer value representing the color effect.
	 */
	public int getColorEffect()
	{
		return 0;
	}
	
	/**
	 * Checks if the current actor is a non-player character.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the entity is an NPC.
	 */
	@Override
	public boolean isNpc()
	{
		return true;
	}
	
	/**
	 * Converts the current object into an {@code Npc}.<br>
	 * This method returns the current instance.
	 * @return the current {@code Npc} instance.
	 */
	@Override
	public Npc asNpc()
	{
		return this;
	}
	
	/**
	 * Sets the team for this entity.<br>
	 * If {@code broadcast} is {@code true}, it sends a notification to other players.
	 * @param team The {@code Team} to assign to this entity.
	 * @param broadcast Whether to announce the change to others.
	 */
	public void setTeam(Team team, boolean broadcast)
	{
		super.setTeam(team);
		if (broadcast)
		{
			broadcastInfo();
		}
	}
	
	/**
	 * Sets the team for this entity.<br>
	 * This method updates the team and broadcasts the new information.
	 * @param team The {@code Team} to assign to this entity.
	 */
	@Override
	public void setTeam(Team team)
	{
		super.setTeam(team);
		broadcastInfo();
	}
	
	/**
	 * Checks if the NPC is configured to walk.<br>
	 * Returns {@code true} if walking is enabled.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating if the entity is a walker.
	 */
	@Override
	public boolean isWalker()
	{
		return _isWalker;
	}
	
	/**
	 * Sets the entity as a walker.<br>
	 * This enables walking behavior for the {@code Npc}.
	 */
	public void setWalker()
	{
		_isWalker = true;
	}
	
	/**
	 * Recharges shots for the entity based on the provided types.<br>
	 * This method handles both fake players and normal entities.<br>
	 * It checks for available shot amounts and random chances before charging.
	 * @param physical Determines if {@code ShotType.SOULSHOTS} should be charged.
	 * @param magic Determines if {@code ShotType.SPIRITSHOTS} should be charged.
	 * @param fish This parameter is currently unused in the logic.
	 */
	@Override
	public void rechargeShots(boolean physical, boolean magic, boolean fish)
	{
		if (_isFakePlayer && FakePlayersConfig.FAKE_PLAYER_USE_SHOTS)
		{
			if (physical)
			{
				broadcastPacket(new MagicSkillUse(this, this, 9193, 1, 0, 0));
				chargeShot(ShotType.SOULSHOTS);
			}
			
			if (magic)
			{
				broadcastPacket(new MagicSkillUse(this, this, 9195, 1, 0, 0));
				chargeShot(ShotType.SPIRITSHOTS);
			}
		}
		else
		{
			if (physical && (_soulshotamount > 0))
			{
				if (Rnd.get(100) > getTemplate().getSoulShotChance())
				{
					return;
				}
				
				_soulshotamount--;
				broadcastPacket(new MagicSkillUse(this, this, 2154, 1, 0, 0));
				chargeShot(ShotType.SOULSHOTS);
			}
			
			if (magic && (_spiritshotamount > 0))
			{
				if (Rnd.get(100) > getTemplate().getSpiritShotChance())
				{
					return;
				}
				
				_spiritshotamount--;
				broadcastPacket(new MagicSkillUse(this, this, 2159, 1, 0, 0));
				chargeShot(ShotType.SPIRITSHOTS);
			}
		}
	}
	
	/**
	 * Retrieves the current value of the script.
	 * @return The integer value associated with the script.
	 */
	public int getScriptValue()
	{
		return _scriptValue;
	}
	
	/**
	 * Sets the internal script value for this entity.<br>
	 * This value is used by scripts to store and retrieve data.
	 * @param value The new integer value to assign to the script variable.
	 */
	public void setScriptValue(int value)
	{
		_scriptValue = value;
	}
	
	/**
	 * Checks if the provided {@code int} matches the internal script value.
	 * @param value The integer to compare against the script value.
	 * @return {@code true} if the values match, {@code false} otherwise.
	 */
	public boolean isScriptValue(int value)
	{
		return _scriptValue == value;
	}
	
	/**
	 * Checks if the given {@code Npc} belongs to the same spawn group.<br>
	 * It compares the names of the spawn templates for both entities.
	 * @param npc The {@code Npc} to check.
	 * @return {@code true} if they share the same spawn group name, {@code false} otherwise.
	 */
	public boolean isInMySpawnGroup(Npc npc)
	{
		return getSpawn().getNpcSpawnTemplate().getSpawnTemplate().getName().equals(npc.getSpawn().getNpcSpawnTemplate().getSpawnTemplate().getName());
	}
	
	/**
	 * Checks if the current location matches the spawn point.<br>
	 * It compares the current {@code x} and {@code y} coordinates with the {@code _spawn} object.
	 * @return {@code true} if the entity is at its spawn location, {@code false} otherwise.
	 */
	public boolean staysInSpawnLoc()
	{
		return ((_spawn != null) && (_spawn.getX() == getX()) && (_spawn.getY() == getY()));
	}
	
	/**
	 * Checks if this NPC has any variables assigned to it.<br>
	 * It looks for a non-null instance of {@code NpcVariables}.
	 * @return {@code true} if the NPC has variables, {@code false} otherwise.
	 */
	public boolean hasVariables()
	{
		return getScript(NpcVariables.class) != null;
	}
	
	/**
	 * Retrieves the variables associated with this {@code Npc}.<br>
	 * If no variables exist, a new {@code NpcVariables} instance is created.
	 * @return the {@code NpcVariables} object.
	 */
	public NpcVariables getVariables()
	{
		final NpcVariables vars = getScript(NpcVariables.class);
		return vars != null ? vars : addScript(new NpcVariables());
	}
	
	/**
	 * Sends a specific event to all {@code Npc} objects within a certain range.<br>
	 * This method identifies nearby NPCs that have the required listener and notifies them asynchronously.
	 * @param eventName The name of the event to broadcast.
	 * @param radius The distance from this object to search for targets.
	 * @param reference An additional object used as a reference for the event data.
	 */
	public void broadcastEvent(String eventName, int radius, WorldObject reference)
	{
		World.forEachVisibleObjectInRange(this, Npc.class, radius, obj ->
		{
			if (obj.hasListener(EventType.ON_NPC_EVENT_RECEIVED))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnNpcEventReceived(eventName, this, obj, reference), obj);
			}
		});
	}
	
	/**
	 * Sends a script event to a specific receiver.<br>
	 * This method notifies the listener if one exists for the NPC event type.
	 * @param eventName The name of the event to trigger.
	 * @param receiver The {@code WorldObject} that will receive the event.
	 * @param reference An additional {@code WorldObject} used as a reference for the event.
	 */
	public void sendScriptEvent(String eventName, WorldObject receiver, WorldObject reference)
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_EVENT_RECEIVED, receiver))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcEventReceived(eventName, this, receiver.asNpc(), reference), receiver);
		}
	}
	
	/**
	 * Calculates a random location within a specified range.<br>
	 * It uses the current coordinates as the center point.<br>
	 * If the input values are invalid, it returns the current position.
	 * @param radiusMin The minimum distance from the center.
	 * @param radiusMax The maximum distance from the center.
	 * @return A new {@code Location} object at the random coordinates.
	 */
	public Location getPointInRange(int radiusMin, int radiusMax)
	{
		if ((radiusMax == 0) || (radiusMax < radiusMin))
		{
			return new Location(getX(), getY(), getZ());
		}
		
		final int radius = Rnd.get(radiusMin, radiusMax);
		final double angle = Rnd.nextDouble() * 2 * Math.PI;
		return new Location((int) (getX() + (radius * Math.cos(angle))), (int) (getY() + (radius * Math.sin(angle))), getZ());
	}
	
	/**
	 * Creates and drops an item at the current location.<br>
	 * The method handles stackable items and randomizes the drop position.<br>
	 * It also registers the item for auto-destruction if configured.
	 * @param creature The {@code Creature} that dropped the item.
	 * @param itemId The unique identifier of the item to drop.
	 * @param itemCount The number of items to drop.
	 * @return The created {@code Item} object, or {@code null} if creation fails.
	 */
	public Item dropItem(Creature creature, int itemId, long itemCount)
	{
		Item item = null;
		for (int i = 0; i < itemCount; i++)
		{
			if (ItemData.getInstance().getTemplate(itemId) == null)
			{
				LOGGER.severe("Item doesn't exist so cannot be dropped. Item ID: " + itemId + " Quest: " + getName());
				return null;
			}
			
			item = ItemManager.createItem(ItemProcessType.LOOT, itemId, itemCount, creature, this);
			if (item == null)
			{
				return null;
			}
			
			if (creature != null)
			{
				item.getDropProtection().protect(creature);
			}
			
			// Randomize drop position.
			final int newX = (getX() + Rnd.get((RANDOM_ITEM_DROP_LIMIT * 2) + 1)) - RANDOM_ITEM_DROP_LIMIT;
			final int newY = (getY() + Rnd.get((RANDOM_ITEM_DROP_LIMIT * 2) + 1)) - RANDOM_ITEM_DROP_LIMIT;
			final int newZ = getZ() + 20;
			
			item.dropMe(this, newX, newY, newZ);
			
			// Add drop to auto destroy item task.
			if (!GeneralConfig.LIST_PROTECTED_ITEMS.contains(itemId) && (((GeneralConfig.AUTODESTROY_ITEM_AFTER > 0) && !item.getTemplate().hasExImmediateEffect()) || ((GeneralConfig.HERB_AUTO_DESTROY_TIME > 0) && item.getTemplate().hasExImmediateEffect())))
			{
				ItemsAutoDestroyTaskManager.getInstance().addItem(item);
			}
			
			item.setProtected(false);
			
			// If stackable, end loop as entire count is included in 1 instance of item.
			if (item.isStackable() || !GeneralConfig.MULTIPLE_ITEM_DROP)
			{
				break;
			}
		}
		
		return item;
	}
	
	/**
	 * Drops an item for a specific creature.<br>
	 * This method handles the creation of the dropped object in the world.
	 * @param creature The {@code Creature} that will drop the item.
	 * @param item The {@code ItemHolder} containing the item data to be dropped.
	 * @return The resulting {@code Item} object created in the world.
	 */
	public Item dropItem(Creature creature, ItemHolder item)
	{
		return dropItem(creature, item.getId(), item.getCount());
	}
	
	/**
	 * Retrieves the name of this {@code Npc}.<br>
	 * This value is obtained from the associated {@code NpcTemplate}.
	 * @return The name of the {@code Npc} as a {@code String}.
	 */
	@Override
	public String getName()
	{
		return getTemplate().getName();
	}
	
	/**
	 * Checks if the NPC is visible to a specific player.<br>
	 * This method evaluates visibility based on configured rules and events.
	 * @param player The {@code Player} checking for visibility.
	 * @return {@code true} if the NPC can be seen by the player, {@code false} otherwise.
	 */
	@Override
	public boolean isVisibleFor(Player player)
	{
		if (hasListener(EventType.ON_NPC_CAN_BE_SEEN))
		{
			final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnNpcCanBeSeen(this, player), this, TerminateReturn.class);
			if (term != null)
			{
				return term.terminate();
			}
		}
		
		return super.isVisibleFor(player);
	}
	
	/**
	 * Sets whether the NPC can be talked to.<br>
	 * Use {@code true} to allow interaction and {@code false} to disable it.
	 * @param value The talkable status to set.
	 */
	public void setTalkable(boolean value)
	{
		_isTalkable = value;
	}
	
	/**
	 * Checks if the NPC can be interacted with via dialogue.<br>
	 * Returns {@code true} if the entity is talkable, and {@code false} otherwise.
	 * @return a boolean value representing the talkable status.
	 */
	public boolean isTalkable()
	{
		return _isTalkable;
	}
	
	/**
	 * Checks if the current NPC is a quest monster.<br>
	 * This is used to identify monsters that are part of specific quests.
	 * @return {@code true} if this is a quest monster, {@code false} otherwise.
	 */
	public boolean isQuestMonster()
	{
		return _isQuestMonster;
	}
	
	/**
	 * Sets the unique identifier for the weapon that deals the killing blow.<br>
	 * This value is used to track which item finished off a target.
	 * @param weaponId The {@code int} ID of the weapon to be set.
	 */
	public void setKillingBlowWeapon(int weaponId)
	{
		_killingBlowWeaponId = weaponId;
	}
	
	/**
	 * Retrieves the ID of the weapon used to deliver the killing blow.
	 * @return the {@code int} identifier of the weapon.
	 */
	public int getKillingBlowWeapon()
	{
		return _killingBlowWeaponId;
	}
	
	/**
	 * Retrieves the minimum distance for a shop range.<br>
	 * This value is fetched from {@code PrivateStoreRangeConfig}.
	 * @return The minimum shop distance as an {@code int}.
	 */
	@Override
	public int getMinShopDistance()
	{
		return PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_NPC;
	}
	
	/**
	 * Checks if the current actor is a fake player.<br>
	 * This is used to identify non-real players in the game world.
	 * @return {@code true} if the actor is a fake player, {@code false} otherwise.
	 */
	@Override
	public boolean isFakePlayer()
	{
		return _isFakePlayer;
	}
	
	/**
	 * Checks whether a mounted fake player has been dismounted at runtime.
	 * @return {@code true} if the fake player is no longer showing its mount.
	 */
	public boolean isDismounted()
	{
		return _dismounted;
	}
	
	/**
	 * Dismounts a mounted fake player: hides its mount and reveals its weapon, then broadcasts the change.<br>
	 * Has no effect if the fake player is not currently mounted or is already dismounted.
	 */
	public void dismount()
	{
		if (_dismounted || !_isFakePlayer || (getTemplate().getFakePlayerInfo() == null) || (getTemplate().getFakePlayerInfo().getMountType() == 0))
		{
			return;
		}
		
		_dismounted = true;
		broadcastPacket(new Ride(this)); // Tell nearby clients to remove the strider, then refresh the char info.
		broadcastInfo();
	}
	
	/**
	 * Re-mounts a dismounted fake player, showing its mount again.<br>
	 * Has no effect unless the fake player was dismounted at runtime.
	 */
	public void remount()
	{
		if (!_dismounted || !_isFakePlayer)
		{
			return;
		}
		
		_dismounted = false;
		broadcastPacket(new Ride(this)); // Tell nearby clients to show the strider again, then refresh the char info.
		broadcastInfo();
	}
	
	/**
	 * Retrieves the unique identifier for the cloned object.<br>
	 * This value is used to distinguish between original and cloned entities.
	 * @return the {@code int} ID of the clone object.
	 */
	public int getCloneObjId()
	{
		return _cloneObjId;
	}
	
	/**
	 * Sets the unique identifier for a cloned object.<br>
	 * This value is used to track objects created from a template.
	 * @param cloneObjId The new ID to assign to the cloned object.
	 */
	public void setCloneObjId(int cloneObjId)
	{
		_cloneObjId = cloneObjId;
	}
	
	/**
	 * Retrieves the unique identifier for the clan.
	 * @return the {@code int} value of the clan ID.
	 */
	@Override
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * Sets the unique identifier for the clan.<br>
	 * This updates the internal {@code _clanId} field.
	 * @param clanObjId The ID of the clan to assign.
	 */
	public void setClanId(int clanObjId)
	{
		_clanId = clanObjId;
	}
	
	/**
	 * Sends a broadcast message to all visible players.<br>
	 * The message is sent using the specified {@code ChatType}.
	 * @param chatType The type of chat to be used for the broadcast.
	 * @param text The content of the message to display.
	 */
	public void broadcastSay(ChatType chatType, String text)
	{
		World.broadcastToVisiblePlayers(this, new NpcSay(this, chatType, text));
	}
	
	/**
	 * Broadcasts a specific NPC message to nearby or all visible players.<br>
	 * This method handles the logic for different {@code ChatType} categories.<br>
	 * It automatically determines the broadcast range based on the provided type.
	 * @param chatType The category of the chat message to be sent.
	 * @param npcStringId The unique identifier for the NPC text string.
	 * @param parameters Optional strings to be inserted into the message template.
	 */
	public void broadcastSay(ChatType chatType, NpcStringId npcStringId, String... parameters)
	{
		final NpcSay npcSay = new NpcSay(this, chatType, npcStringId);
		if (parameters != null)
		{
			for (String parameter : parameters)
			{
				if (parameter != null)
				{
					npcSay.addStringParameter(parameter);
				}
			}
		}
		
		switch (chatType)
		{
			case NPC_GENERAL:
			{
				World.broadcastToVisiblePlayersInRange(this, npcSay, 1250);
				break;
			}
			default:
			{
				World.broadcastToVisiblePlayers(this, npcSay);
				break;
			}
		}
	}
	
	/**
	 * Sends a chat message to players within a specific area.<br>
	 * The message is displayed based on the provided {@code ChatType}.<br>
	 * This method uses the current actor's location as the center point.
	 * @param chatType The type of chat message to send.
	 * @param text The content of the message to display.
	 * @param radius The distance in units from the actor to reach other players.
	 */
	public void broadcastSay(ChatType chatType, String text, int radius)
	{
		World.broadcastToVisiblePlayersInRange(this, new NpcSay(this, chatType, text), radius);
	}
	
	/**
	 * Sends a broadcast message to all players within a specific range.<br>
	 * The message is sent by this {@code Npc}.
	 * @param chatType The type of the chat message to be sent.
	 * @param npcStringId The unique identifier for the text string to display.
	 * @param radius The distance from the NPC within which players can see the message.
	 */
	public void broadcastSay(ChatType chatType, NpcStringId npcStringId, int radius)
	{
		World.broadcastToVisiblePlayersInRange(this, new NpcSay(this, chatType, npcStringId), radius);
	}
	
	/**
	 * Retrieves the statistics parameters for this NPC.<br>
	 * It merges parameters from the template and the spawn template if available.
	 * @return a {@code StatSet} containing the merged parameters.
	 */
	public StatSet getParameters()
	{
		if (_params != null)
		{
			return _params;
		}
		
		if (_spawn != null) // Minions doesn't have Spawn object bound.
		{
			final NpcSpawnTemplate npcSpawnTemplate = _spawn.getNpcSpawnTemplate();
			final StatSet spawnParams = (npcSpawnTemplate != null) ? npcSpawnTemplate.getParameters() : null;
			if ((spawnParams != null) && !spawnParams.isEmpty())
			{
				final StatSet templateParams = getTemplate().getParameters();
				if ((templateParams != null) && !templateParams.getSet().isEmpty())
				{
					final StatSet set = new StatSet();
					set.merge(templateParams);
					set.merge(spawnParams);
					_params = set;
					return set;
				}
				
				_params = spawnParams;
				return _params;
			}
		}
		
		_params = getTemplate().getParameters();
		return _params;
	}
	
	/**
	 * Retrieves the list of long range skills for this NPC.<br>
	 * This method fetches skills from the {@code NpcTemplate}.
	 * @return a {@code List} of {@code Skill} objects.
	 */
	public List<Skill> getLongRangeSkills()
	{
		return getTemplate().getAISkills(AISkillScope.LONG_RANGE);
	}
	
	/**
	 * Retrieves the list of short range skills for this NPC.<br>
	 * This method fetches skills from the {@code NpcTemplate}.
	 * @return a {@code List} of {@code Skill} objects.
	 */
	public List<Skill> getShortRangeSkills()
	{
		return getTemplate().getAISkills(AISkillScope.SHORT_RANGE);
	}
	
	/**
	 * Checks if the NPC should use a skill based on a random chance.<br>
	 * It compares a random number against the range defined in the template.
	 * @return {@code true} if the skill is triggered, {@code false} otherwise.
	 */
	public boolean hasSkillChance()
	{
		return Rnd.get(100) < Rnd.get(getTemplate().getMinSkillChance(), getTemplate().getMaxSkillChance());
	}
	
	/**
	 * Retrieves the display name of the NPC.<br>
	 * This returns the string identifier associated with this entity.
	 * @return The {@code NpcStringId} representing the name.
	 */
	public NpcStringId getNameString()
	{
		return _nameString;
	}
	
	/**
	 * Retrieves the title string associated with this NPC.<br>
	 * This value is used to display a specific name or label for the entity.
	 * @return The {@code NpcStringId} of the title string.
	 */
	public NpcStringId getTitleString()
	{
		return _titleString;
	}
	
	/**
	 * Sets the display name string for this NPC.<br>
	 * This updates the internal {@code _nameString} field.
	 * @param nameString The new {@code NpcStringId} to assign to the NPC.
	 */
	public void setNameString(NpcStringId nameString)
	{
		_nameString = nameString;
	}
	
	/**
	 * Sets the title string for this NPC.<br>
	 * This updates the {@code _titleString} field with the provided value.
	 * @param titleString The new {@code NpcStringId} to set as the title.
	 */
	public void setTitleString(NpcStringId titleString)
	{
		_titleString = titleString;
	}
	
	/**
	 * Sends a channeling effect packet to the client.<br>
	 * This method notifies players of an active channeling action.
	 * @param target The {@code Creature} receiving the effect.
	 * @param state The integer state of the channeling effect.
	 */
	public void sendChannelingEffect(Creature target, int state)
	{
		broadcastPacket(new ExShowChannelingEffect(this, target, state));
	}
	
	/**
	 * Updates the current raid boss status.<br>
	 * This method sets the {@code _raidStatus} field to the provided value.
	 * @param status The new {@code RaidBossStatus} to apply.
	 */
	public void setDBStatus(RaidBossStatus status)
	{
		_raidStatus = status;
	}
	
	/**
	 * Retrieves the current status of the raid boss.<br>
	 * This method returns the value stored in the {@code _raidStatus} field.
	 * @return the current {@code RaidBossStatus} of this entity.
	 */
	public RaidBossStatus getDBStatus()
	{
		return _raidStatus;
	}
	
	/**
	 * Adds a new {@code QuestTimer} to the internal list of active timers.<br>
	 * This method ensures thread safety by using a synchronized block.
	 * @param questTimer The {@code QuestTimer} object to be added.
	 */
	public void addQuestTimer(QuestTimer questTimer)
	{
		synchronized (_questTimers)
		{
			_questTimers.add(questTimer);
		}
	}
	
	/**
	 * Removes a specific quest timer from the internal tracking list.<br>
	 * This method ensures thread safety by using a synchronized block.
	 * @param questTimer The {@code QuestTimer} object to be removed.
	 */
	public void removeQuestTimer(QuestTimer questTimer)
	{
		synchronized (_questTimers)
		{
			_questTimers.remove(questTimer);
		}
	}
	
	/**
	 * Stops all active quest timers for this entity.<br>
	 * This method cancels every task in the {@code _questTimers} list.<br>
	 * It then clears the {@code _questTimers} collection.
	 */
	public void stopQuestTimers()
	{
		synchronized (_questTimers)
		{
			for (QuestTimer timer : _questTimers)
			{
				timer.cancelTask();
			}
			
			_questTimers.clear();
		}
	}
	
	/**
	 * Adds a new {@code TimerHolder} to the internal collection.<br>
	 * This method ensures thread safety during the addition process.
	 * @param timer The {@code TimerHolder} object to be added.
	 */
	public void addTimerHolder(TimerHolder<?> timer)
	{
		synchronized (_timerHolders)
		{
			_timerHolders.add(timer);
		}
	}
	
	/**
	 * Removes a specific timer from the internal collection.<br>
	 * This method ensures thread safety by using a synchronized block.
	 * @param timer The {@code TimerHolder<?>} object to be removed.
	 */
	public void removeTimerHolder(TimerHolder<?> timer)
	{
		synchronized (_timerHolders)
		{
			_timerHolders.remove(timer);
		}
	}
	
	/**
	 * Stops all active timers held by this object.<br>
	 * This method cancels every task in the {@code _timerHolders} collection.<br>
	 * It then clears the collection to remove all references.
	 */
	public void stopTimerHolders()
	{
		synchronized (_timerHolders)
		{
			for (TimerHolder<?> timer : _timerHolders)
			{
				timer.cancelTask();
			}
			
			_timerHolders.clear();
		}
	}
	
	/**
	 * Returns a string representation of the {@code Npc}.<br>
	 * This includes the class name, name, ID, and object ID.
	 * @return A formatted string describing the current entity.
	 */
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(':');
		sb.append(getName());
		sb.append('(');
		sb.append(getId());
		sb.append(")[");
		sb.append(getObjectId());
		sb.append(']');
		return sb.toString();
	}
}
