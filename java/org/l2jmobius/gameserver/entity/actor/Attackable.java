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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.AttackableAI;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.RaidDropAnnounceData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.enums.npc.DropType;
import org.l2jmobius.gameserver.entity.actor.holders.npc.AggroInfo;
import org.l2jmobius.gameserver.entity.actor.holders.npc.DamageDoneInfo;
import org.l2jmobius.gameserver.entity.actor.holders.npc.LimitedDropHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.HuntPass;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.actor.status.AttackableStatus;
import org.l2jmobius.gameserver.entity.actor.tasks.attackable.CommandChannelTimer;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.managers.events.EventDropManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableAggroRangeEnter;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableAttack;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableKill;
import org.l2jmobius.gameserver.mechanics.olympiad.Hero;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.siege.manor.Seed;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExMagicAttackInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExRaidDropItemAnnounce;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.WalkingTaskManager;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents an entity that can be attacked by other actors.<br>
 * This class serves as the base for monsters and other hostile NPCs in the game world.<br>
 * It handles core behaviors such as aggression, health management, and combat logic.
 */
public class Attackable extends Npc
{
	// Long range guard, cast on self when hit from beyond this distance.
	private static final int LONG_RANGE_GUARD_DISTANCE = 150;
	
	// Raid
	private boolean _isRaid = false;
	private boolean _isRaidMinion = false;
	
	//
	private boolean _champion = false;
	private final Map<Creature, AggroInfo> _aggroList = new ConcurrentHashMap<>();
	private boolean _canReturnToSpawnPoint = true;
	private boolean _seeThroughSilentMove = false;
	
	// Manor
	private boolean _seeded = false;
	private Seed _seed = null;
	private int _seederObjId = 0;
	private final AtomicReference<ItemHolder> _harvestItem = new AtomicReference<>();
	
	// Spoil
	private int _spoilerObjectId;
	private boolean _plundered = false;
	private final AtomicReference<Collection<ItemHolder>> _sweepItems = new AtomicReference<>();
	
	// Over-hit
	private boolean _overhit;
	private double _overhitDamage;
	private Creature _overhitAttacker;
	
	// Command channel
	private CommandChannel _firstCommandChannelAttacked = null;
	private CommandChannelTimer _commandChannelTimer = null;
	private long _commandChannelLastAttack = 0;
	
	// Misc
	private boolean _mustGiveExpSp;
	private long _spawnTime = System.currentTimeMillis();
	
	/**
	 * Creates a new {@code Attackable} instance using the provided template.<br>
	 * This constructor initializes the entity as an attackable type.<br>
	 * It sets the invulnerability status to {@code false}.<br>
	 * It enables the requirement to reward experience and SP.
	 * @param template The {@code NpcTemplate} used to initialize this entity.
	 */
	public Attackable(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Attackable);
		setInvul(false);
		_mustGiveExpSp = true;
	}
	
	/**
	 * Retrieves the current status of the attackable entity.<br>
	 * This method returns the {@code AttackableStatus} from the parent class.
	 * @return The current {@code AttackableStatus}.
	 */
	@Override
	public AttackableStatus getStatus()
	{
		return (AttackableStatus) super.getStatus();
	}
	
	/**
	 * Initializes the status of this {@code Attackable} entity.<br>
	 * This method creates and sets a new {@code AttackableStatus} instance.
	 */
	@Override
	public void initCharStatus()
	{
		setStatus(new AttackableStatus(this));
	}
	
	/**
	 * Initializes the artificial intelligence for this entity.<br>
	 * This method creates and returns a new {@code AttackableAI} instance.
	 * @return the initialized {@code CreatureAI} object.
	 */
	@Override
	protected CreatureAI initAI()
	{
		return new AttackableAI(this);
	}
	
	/**
	 * Retrieves the current list of creatures that have aggressive behavior toward this entity.<br>
	 * The map uses {@code Creature} as the key and {@code AggroInfo} as the value.
	 * @return A {@code Map} containing all active aggro data.
	 */
	public Map<Creature, AggroInfo> getAggroList()
	{
		return _aggroList;
	}
	
	/**
	 * Checks if the entity is allowed to return to its original spawn point.<br>
	 * This value determines whether the actor can reset its position.
	 * @return {@code true} if the entity can return to its spawn point, {@code false} otherwise.
	 */
	public boolean canReturnToSpawnPoint()
	{
		return _canReturnToSpawnPoint;
	}
	
	/**
	 * Sets whether the entity is allowed to return to its original spawn point.<br>
	 * Use {@code true} to enable this behavior.<br>
	 * Use {@code false} to disable this behavior.
	 * @param value The boolean value to set for returning to the spawn point.
	 */
	public void setCanReturnToSpawnPoint(boolean value)
	{
		_canReturnToSpawnPoint = value;
	}
	
	/**
	 * Checks if the entity can see through silent moves.<br>
	 * This determines if stealth effects are ignored by this actor.
	 * @return {@code true} if the entity can see through silent moves, {@code false} otherwise.
	 */
	public boolean canSeeThroughSilentMove()
	{
		return _seeThroughSilentMove;
	}
	
	/**
	 * Sets whether the entity can see through silent moves.<br>
	 * This determines if a move performed silently is still detectable by this actor.
	 * @param value The boolean value to set for {@code seeThroughSilentMove}.
	 */
	public void setSeeThroughSilentMove(boolean value)
	{
		_seeThroughSilentMove = value;
	}
	
	/**
	 * Executes a specific magic skill for this actor.<br>
	 * This method checks if the conditions are met before casting.<br>
	 * If a valid target is found, it sets the AI intention to cast at that target.
	 * @param skill The {@code Skill} object to be used.
	 */
	public void useMagic(Skill skill)
	{
		if (!SkillCaster.checkUseConditions(this, skill))
		{
			return;
		}
		
		final WorldObject target = skill.getTarget(this, false, false, false);
		if (target != null)
		{
			getAI().setIntentionCast(skill, target);
		}
	}
	
	/**
	 * Reduces the current health points of this entity based on an incoming attack.<br>
	 * This method handles damage calculation, aggro updates, and raid-specific mechanics.<br>
	 * It also triggers minion assistance if the target is a monster with minions.
	 * @param value The amount of damage to subtract from the current HP.
	 * @param attacker The {@code Creature} that initiated the attack.
	 * @param skill The {@code Skill} used to perform the attack.
	 * @param isDOT Whether the damage is part of a Damage Over Time effect.
	 * @param directlyToHp Whether the damage should be applied directly to HP.
	 * @param critical Whether the hit is a critical strike.
	 * @param reflect Whether the damage should be reflected back to the attacker.
	 */
	@Override
	public void reduceCurrentHp(double value, Creature attacker, Skill skill, boolean isDOT, boolean directlyToHp, boolean critical, boolean reflect)
	{
		if (_isRaid && !isMinion() && (attacker != null))
		{
			final Party attackerParty = attacker.getParty();
			if ((attackerParty != null) && attackerParty.isInCommandChannel() && attackerParty.getCommandChannel().meetRaidWarCondition(this))
			{
				if (_firstCommandChannelAttacked == null) // looting right isn't set
				{
					synchronized (this)
					{
						if (_firstCommandChannelAttacked == null)
						{
							_firstCommandChannelAttacked = attackerParty.getCommandChannel();
							if (_firstCommandChannelAttacked != null)
							{
								_commandChannelTimer = new CommandChannelTimer(this);
								_commandChannelLastAttack = System.currentTimeMillis();
								ThreadPool.schedule(_commandChannelTimer, 10000); // check for last attack
								_firstCommandChannelAttacked.broadcastPacket(new ExShowScreenMessage(NpcStringId.S1_S_COMMAND_CHANNEL_HAS_LOOTING_RIGHTS, ExShowScreenMessage.TOP_CENTER, 3000, _firstCommandChannelAttacked.getLeader().getName()));
							}
						}
					}
				}
				else if (attackerParty.getCommandChannel().equals(_firstCommandChannelAttacked)) // is in same channel
				{
					_commandChannelLastAttack = System.currentTimeMillis(); // update last attack time
				}
			}
		}
		
		// Add damage and hate to the attacker AggroInfo of the Attackable _aggroList.
		if (attacker != null)
		{
			addDamage(attacker, (int) value, skill);
			
			// Check Raidboss attack. Character will be petrified if attacking a raid that's more than 8 levels lower. In retail you deal damage to raid before curse.
			if (_isRaid && giveRaidCurse() && !NpcConfig.RAID_DISABLE_CURSE && (attacker.getLevel() > (getLevel() + 8)))
			{
				final Skill raidCurse = CommonSkill.RAID_CURSE2.getSkill();
				if (raidCurse != null)
				{
					raidCurse.applyEffects(this, attacker);
				}
			}
		}
		
		// If this Attackable is a Monster and it has spawned minions, call its minions to battle.
		if (isMonster())
		{
			Monster master = asMonster();
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(this, attacker);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(this, attacker);
			}
		}
		
		// Reduce the current HP of the Attackable and launch the doDie Task if necessary.
		super.reduceCurrentHp(value, attacker, skill, isDOT, directlyToHp, critical, reflect);
	}
	
	/**
	 * Sets whether the entity must reward experience and SP.<br>
	 * This flag determines if rewards are guaranteed upon death.
	 * @param value The boolean value to set for mandatory rewards.
	 */
	public synchronized void setMustRewardExpSp(boolean value)
	{
		_mustGiveExpSp = value;
	}
	
	/**
	 * Checks if the entity is required to grant experience and SP.<br>
	 * This check returns {@code false} if the entity is a fake player.
	 * @return {@code true} if rewards are mandatory, {@code false} otherwise.
	 */
	public synchronized boolean getMustRewardExpSP()
	{
		return _mustGiveExpSp && !isFakePlayer();
	}
	
	/**
	 * Handles the logic for when a creature dies.<br>
	 * It processes death events and notifies relevant systems.<br>
	 * This method is called to trigger corpse removal and reward distribution.
	 * @param killer The {@code Creature} that dealt the killing blow.
	 * @return {@code true} if the death was successful, {@code false} otherwise.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		// Kill the Npc (the corpse disappeared after 7 seconds).
		if (!super.doDie(killer))
		{
			return false;
		}
		
		// Notify to scripts.
		if (killer != null)
		{
			final Player player = killer.asPlayer();
			if (player != null)
			{
				if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_KILL, this))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnAttackableKill(player, this, killer.isSummon()), this);
				}
			}
		}
		
		// Notify to minions if there are.
		if (isMonster())
		{
			final Monster mob = asMonster();
			final Monster leader = mob.getLeader();
			if ((leader != null) && leader.hasMinions())
			{
				final int npcId = getId();
				final int respawnTime = NpcConfig.MINIONS_RESPAWN_TIME.containsKey(npcId) ? NpcConfig.MINIONS_RESPAWN_TIME.get(npcId) * 1000 : -1;
				leader.getMinionList().onMinionDie(mob, respawnTime);
			}
			
			if (mob.hasMinions())
			{
				mob.getMinionList().onMasterDie(false);
			}
		}
		
		return true;
	}
	
	private static class PartyContainer
	{
		public Party party;
		public long damage;
		
		public PartyContainer(Party party, long damage)
		{
			this.party = party;
			this.damage = damage;
		}
	}
	
	/**
	 * Calculates and distributes rewards to players after a creature is defeated.<br>
	 * This method identifies the main damage dealers and handles loot distribution.<br>
	 * It also processes raid boss points and experience rewards based on damage dealt.
	 * @param lastAttacker The {@code Creature} that dealt the final blow.
	 */
	@Override
	protected void calculateRewards(Creature lastAttacker)
	{
		try
		{
			if (_aggroList.isEmpty())
			{
				return;
			}
			
			// NOTE: Concurrent-safe map is used because while iterating to verify all conditions sometimes an entry must be removed.
			final Map<Player, DamageDoneInfo> rewards = new ConcurrentHashMap<>();
			
			Player maxDealer = null;
			long maxDamage = 0;
			long totalDamage = 0;
			
			// While Iterating over This Map Removing Object is Not Allowed.
			// Go through the _aggroList of the Attackable.
			for (AggroInfo info : _aggroList.values())
			{
				// Get the Creature corresponding to this attacker.
				final Player attacker = info.getAttacker().asPlayer();
				if (attacker == null)
				{
					continue;
				}
				
				// Get damages done by this attacker.
				final long damage = info.getDamage();
				
				// Prevent unwanted behavior
				if (damage > 1)
				{
					// Check if damage dealer isn't too far from this (killed monster).
					if (calculateDistance3D(attacker) > PlayerConfig.ALT_PARTY_RANGE)
					{
						continue;
					}
					
					totalDamage += damage;
					
					// Calculate real damages (Summoners should get own damage plus summon's damage).
					final DamageDoneInfo reward = rewards.computeIfAbsent(attacker, DamageDoneInfo::new);
					reward.addDamage(damage);
					
					final long rewardDamage = reward.getDamage();
					if (rewardDamage > maxDamage)
					{
						maxDealer = attacker;
						maxDamage = rewardDamage;
					}
				}
			}
			
			final List<PartyContainer> damagingParties = new ArrayList<>();
			for (AggroInfo info : _aggroList.values())
			{
				final Creature attacker = info.getAttacker();
				if (attacker == null)
				{
					continue;
				}
				
				long totalMemberDamage = 0;
				final Party party = attacker.getParty();
				if (party == null)
				{
					continue;
				}
				
				PartyContainer found = null;
				for (int i = 0, damagingPartySize = damagingParties.size(); i < damagingPartySize; i++)
				{
					final PartyContainer p = damagingParties.get(i);
					if (p.party == party)
					{
						found = p;
						break;
					}
				}
				
				final PartyContainer container = found != null ? found : new PartyContainer(party, 0L);
				final List<Player> members = party.getMembers();
				for (Player e : members)
				{
					final AggroInfo memberAggro = _aggroList.get(e);
					if (memberAggro == null)
					{
						continue;
					}
					
					final long memberDamage = memberAggro.getDamage();
					if (memberDamage > 1)
					{
						totalMemberDamage += memberDamage;
					}
				}
				
				container.damage = totalMemberDamage;
				
				if (found == null)
				{
					damagingParties.add(container);
				}
			}
			
			final PartyContainer mostDamageParty;
			damagingParties.sort(Comparator.comparingLong(c -> -c.damage));
			mostDamageParty = !damagingParties.isEmpty() ? damagingParties.get(0) : null;
			
			// Calculate raidboss points
			if (_isRaid && !_isRaidMinion)
			{
				final Player player = (maxDealer != null) && maxDealer.isOnline() ? maxDealer : lastAttacker.asPlayer();
				broadcastPacket(new SystemMessage(SystemMessageId.CONGRATULATIONS_YOUR_RAID_WAS_SUCCESSFUL));
				final int raidbossPoints = (int) (getTemplate().getRaidPoints() * RatesConfig.RATE_RAIDBOSS_POINTS);
				final Party party = player.getParty();
				if (party != null)
				{
					final CommandChannel command = party.getCommandChannel();
					final List<Player> members = new ArrayList<>();
					if (command != null)
					{
						for (Player p : command.getMembers())
						{
							if (p.calculateDistance3D(this) < PlayerConfig.ALT_PARTY_RANGE)
							{
								members.add(p);
							}
						}
					}
					else
					{
						for (Player p : player.getParty().getMembers())
						{
							if (p.calculateDistance3D(this) < PlayerConfig.ALT_PARTY_RANGE)
							{
								members.add(p);
							}
						}
					}
					
					members.forEach(p ->
					{
						final int points = (int) (Math.max(raidbossPoints / members.size(), 1) * p.getStat().getValue(Stat.BONUS_RAID_POINTS, 1));
						p.increaseRaidbossPoints(points);
						p.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_RAID_POINT_S).addInt(points));
						p.increaseClanContribution(FeatureConfig.CLAN_CONTRIBUTION_REWARD_FOR_RAID);
						if (p.getNobleLevel() > 0)
						{
							Hero.getInstance().setRBkilled(p.getObjectId(), getId());
						}
					});
				}
				else
				{
					final int points = (int) (Math.max(raidbossPoints, 1) * player.getStat().getValue(Stat.BONUS_RAID_POINTS, 1));
					player.increaseRaidbossPoints(points);
					player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_RAID_POINT_S).addInt(points));
					player.increaseClanContribution(FeatureConfig.CLAN_CONTRIBUTION_REWARD_FOR_RAID);
					if (player.getNobleLevel() > 0)
					{
						Hero.getInstance().setRBkilled(player.getObjectId(), getId());
					}
				}
			}
			
			if ((mostDamageParty != null) && (mostDamageParty.damage > maxDamage))
			{
				Player leader = mostDamageParty.party.getLeader();
				doItemDrop(leader);
				EventDropManager.getInstance().doEventDrop(leader, this);
			}
			else
			{
				doItemDrop((maxDealer != null) && maxDealer.isOnline() ? maxDealer : lastAttacker);
				EventDropManager.getInstance().doEventDrop(lastAttacker, this);
			}
			
			if (!getMustRewardExpSP())
			{
				return;
			}
			
			if (!rewards.isEmpty())
			{
				for (DamageDoneInfo reward : rewards.values())
				{
					if (reward == null)
					{
						continue;
					}
					
					// Attacker to be rewarded.
					final Player attacker = reward.getAttacker();
					
					// Total amount of damage done.
					final long damage = reward.getDamage();
					
					// Get party
					final Party attackerParty = attacker.getParty();
					
					// Penalty applied to the attacker's XP.
					// If this attacker have servitor, get Exp Penalty applied for the servitor.
					float penalty = 1;
					
					for (Summon summon : attacker.getServitors().values())
					{
						final float expMultiplier = summon.asServitor().getExpMultiplier();
						if (expMultiplier > 1)
						{
							penalty = expMultiplier;
							break;
						}
					}
					
					// If there's NO party in progress.
					if (attackerParty == null)
					{
						// Calculate Exp and SP rewards.
						if (isInSurroundingRegion(attacker))
						{
							// Calculate the difference of level between this attacker (player or servitor owner) and the Attackable.
							// mob = 24, atk = 10, diff = -14 (full xp)
							// mob = 24, atk = 28, diff = 4 (some xp)
							// mob = 24, atk = 50, diff = 26 (no xp)
							final double[] expSp = calculateExpAndSp(attacker.getLevel(), damage, totalDamage);
							double exp = expSp[0];
							double sp = expSp[1];
							if (ChampionMonstersConfig.CHAMPION_ENABLE && _champion)
							{
								exp *= ChampionMonstersConfig.CHAMPION_REWARDS_EXP_SP;
								sp *= ChampionMonstersConfig.CHAMPION_REWARDS_EXP_SP;
							}
							
							exp *= penalty;
							
							// Check for an over-hit enabled strike.
							final Creature overhitAttacker = _overhitAttacker;
							if (_overhit && (overhitAttacker != null))
							{
								final Player player = overhitAttacker.asPlayer();
								if ((player != null) && (attacker == player))
								{
									attacker.sendPacket(SystemMessageId.OVER_HIT);
									attacker.sendPacket(new ExMagicAttackInfo(overhitAttacker.getObjectId(), getObjectId(), ExMagicAttackInfo.OVERHIT));
									exp += calculateOverhitExp(exp);
								}
							}
							
							// Distribute the Exp and SP between the Player and its Summon.
							if (!attacker.isDead())
							{
								final PlayerStat attackerStat = attacker.getStat();
								final int playerClassId = attacker.getPlayerClass().getId();
								exp = attackerStat.getValue(Stat.EXPSP_RATE, exp) * ClassBalanceConfig.EXP_AMOUNT_MULTIPLIERS[playerClassId];
								sp = attackerStat.getValue(Stat.EXPSP_RATE, sp) * ClassBalanceConfig.SP_AMOUNT_MULTIPLIERS[playerClassId];
								
								// Premium rates
								if (attacker.hasPremiumStatus())
								{
									exp *= PremiumSystemConfig.PREMIUM_RATE_XP;
									sp *= PremiumSystemConfig.PREMIUM_RATE_SP;
								}
								
								attacker.addExpAndSp(exp, sp, useVitalityRate());
								if (exp > 0)
								{
									if (useVitalityRate())
									{
										attacker.updateVitalityPoints(getVitalityPoints(attacker.getLevel(), exp, _isRaid), true, false);
										PcCafePointsManager.getInstance().givePcCafePoint(attacker, exp);
									}
									
									final int symbolSealPoints = attacker.getSymbolSealPoints();
									if (symbolSealPoints > 0)
									{
										attacker.setSymbolSealPoints(symbolSealPoints - PlayerConfig.CONSUME_SYMBOL_SEAL_POINTS);
										if (attacker.getSymbolSealPoints() == 0)
										{
											attacker.removeSymbolSealSkills();
										}
									}
									
									final HuntPass huntPass = attacker.getHuntPass();
									if (huntPass != null)
									{
										attacker.getHuntPass().addPassPoint();
									}
								}
							}
						}
					}
					else
					{
						// Share with party members.
						long partyDmg = 0;
						double partyMul = 1;
						int partyLvl = 0;
						
						// Get all Creature that can be rewarded in the party.
						final List<Player> rewardedMembers = new ArrayList<>();
						
						// Go through all Player in the party.
						final List<Player> groupMembers = attackerParty.isInCommandChannel() ? attackerParty.getCommandChannel().getMembers() : attackerParty.getMembers();
						for (Player partyPlayer : groupMembers)
						{
							if ((partyPlayer == null) || partyPlayer.isDead())
							{
								continue;
							}
							
							// Get the RewardInfo of this Player from Attackable rewards.
							final DamageDoneInfo reward2 = rewards.get(partyPlayer);
							
							// If the Player is in the Attackable rewards add its damages to party damages.
							if (reward2 != null)
							{
								if (calculateDistance3D(partyPlayer) < PlayerConfig.ALT_PARTY_RANGE)
								{
									partyDmg += reward2.getDamage(); // Add Player damages to party damages.
									rewardedMembers.add(partyPlayer);
									
									if (partyPlayer.getLevel() > partyLvl)
									{
										if (attackerParty.isInCommandChannel())
										{
											partyLvl = attackerParty.getCommandChannel().getLevel();
										}
										else
										{
											partyLvl = partyPlayer.getLevel();
										}
									}
								}
								
								rewards.remove(partyPlayer); // Remove the Player from the Attackable rewards.
							}
							else if (calculateDistance3D(partyPlayer) < PlayerConfig.ALT_PARTY_RANGE)
							{
								rewardedMembers.add(partyPlayer);
								if (partyPlayer.getLevel() > partyLvl)
								{
									if (attackerParty.isInCommandChannel())
									{
										partyLvl = attackerParty.getCommandChannel().getLevel();
									}
									else
									{
										partyLvl = partyPlayer.getLevel();
									}
								}
							}
						}
						
						// If the party didn't killed this Attackable alone.
						if (partyDmg < totalDamage)
						{
							partyMul = ((double) partyDmg / totalDamage);
						}
						
						// Calculate Exp and SP rewards.
						final double[] expSp = calculateExpAndSp(partyLvl, partyDmg, totalDamage);
						double exp = expSp[0];
						double sp = expSp[1];
						if (ChampionMonstersConfig.CHAMPION_ENABLE && _champion)
						{
							exp *= ChampionMonstersConfig.CHAMPION_REWARDS_EXP_SP;
							sp *= ChampionMonstersConfig.CHAMPION_REWARDS_EXP_SP;
						}
						
						exp *= partyMul;
						sp *= partyMul;
						
						// Check for an over-hit enabled strike.
						// (When in party, the over-hit exp bonus is given to the whole party and splitted proportionally through the party members).
						final Creature overhitAttacker = _overhitAttacker;
						if (_overhit && (overhitAttacker != null))
						{
							final Player player = overhitAttacker.asPlayer();
							if ((player != null) && (attacker == player))
							{
								attacker.sendPacket(SystemMessageId.OVER_HIT);
								attacker.sendPacket(new ExMagicAttackInfo(overhitAttacker.getObjectId(), getObjectId(), ExMagicAttackInfo.OVERHIT));
								exp += calculateOverhitExp(exp);
							}
						}
						
						// Distribute Experience and SP rewards to Player Party members in the known area of the last attacker.
						if (partyDmg > 0)
						{
							attackerParty.distributeXpAndSp(exp, sp, rewardedMembers, partyLvl, this);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "", e);
		}
	}
	
	/**
	 * Adds a specific creature to the list of entities that have attacked this actor.<br>
	 * This method ensures that the same {@code Creature} is not added more than once.<br>
	 * It ignores {@code null} values and prevents adding the actor to its own list.
	 * @param creature The {@code Creature} to be added to the attack list.
	 */
	@Override
	public void addAttackerToAttackByList(Creature creature)
	{
		if ((creature == null) || (creature == this))
		{
			return;
		}
		
		final Set<Creature> attackByList = getAttackByList();
		if (attackByList.contains(creature))
		{
			return;
		}
		
		attackByList.add(creature);
	}
	
	/**
	 * Retrieves the creature that has dealt the most damage to this entity.<br>
	 * It checks the {@code _aggroList} for the highest damage value.<br>
	 * The attacker must also be within the {@code PlayerConfig.ALT_PARTY_RANGE}.
	 * @return the {@code Creature} dealing the most damage, or {@code null} if no valid attacker is found.
	 */
	public Creature getMainDamageDealer()
	{
		if (_aggroList.isEmpty())
		{
			return null;
		}
		
		long damage = 0;
		Creature damageDealer = null;
		for (AggroInfo info : _aggroList.values())
		{
			if (info == null)
			{
				continue;
			}
			
			final long infoDamage = info.getDamage();
			if (infoDamage <= damage)
			{
				continue;
			}
			
			final Creature infoAttacker = info.getAttacker();
			if (calculateDistance3D(infoAttacker) < PlayerConfig.ALT_PARTY_RANGE)
			{
				damage = infoDamage;
				damageDealer = infoAttacker;
			}
		}
		
		return damageDealer;
	}
	
	/**
	 * Applies damage from an attacker to this creature.<br>
	 * This method updates the AI state and calculates hate values based on the damage dealt.<br>
	 * It also triggers relevant events if the attacker is a player.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param damage The amount of damage to apply.
	 * @param skill The {@code Skill} used for the attack, or {@code null} if no skill was used.
	 */
	public void addDamage(Creature attacker, int damage, Skill skill)
	{
		if (attacker == null)
		{
			return;
		}
		
		// Notify the Attackable AI with ATTACKED.
		if (!isDead())
		{
			try
			{
				// If monster is on walk - stop it.
				if (isWalker() && !isCoreAIDisabled() && WalkingTaskManager.getInstance().isOnWalk(this))
				{
					WalkingTaskManager.getInstance().stopMoving(this, false, true);
				}
				
				final CreatureAI ai = getAI();
				if (ai instanceof AttackableAI)
				{
					// The faction call shares the hate of this hit, so the damage travels with the notify.
					((AttackableAI) ai).notifyActionAttacked(attacker, damage);
				}
				else
				{
					ai.notifyActionAttacked(attacker);
				}
				
				// After the ATTACKED notify, with the damage of the hit deciding whether the guard reacts.
				tryLongRangeGuard(attacker, damage);
				
				// Calculate the amount of hate this attackable receives from this attack.
				long hateValue = (long) ((((long) damage * 100) / (getLevel() + 7)) * getHateRatio(attacker));
				if (skill == null)
				{
					hateValue *= attacker.getStat().getMul(Stat.HATE_ATTACK, 1);
				}
				
				addDamageHate(attacker, damage, (int) hateValue);
				
				final Player player = attacker.asPlayer();
				if ((player != null) && EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_ATTACK, this))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAttack(player, this, damage, skill, attacker.isSummon()), this);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "", e);
			}
		}
	}
	
	/**
	 * Casts NPC Ultimate Defense on self when hit from range, declared by the AI parameter LongRangeGuardRate.<br>
	 * When the attacker is further away than {@code LONG_RANGE_GUARD_DISTANCE}, the skill is cast with LongRangeGuardRate percent chance,<br>
	 * unless the hit dealt no damage, which is what every skill retail excludes from the reaction has in common.<br>
	 * When the attacker is within that distance, the effect is dispelled instead.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param damage The amount of damage the hit dealt.
	 */
	private void tryLongRangeGuard(Creature attacker, int damage)
	{
		if (!NpcConfig.LONG_RANGE_GUARD)
		{
			return;
		}
		
		// Every skill retail leaves out of the reaction deals no damage, so the damage of the hit identifies them at runtime.
		if (damage <= 0)
		{
			return;
		}
		
		final int guardRate = getTemplate().getParameters().getInt("LongRangeGuardRate", -1);
		if (guardRate <= 0)
		{
			return;
		}
		
		if (calculateDistance2D(attacker) > LONG_RANGE_GUARD_DISTANCE)
		{
			if (!isAffectedBySkill(CommonSkill.NPC_ULTIMATE_DEFENSE.getId()) && (Rnd.get(100) < guardRate))
			{
				// Null where the chronicle does not carry the skill, which leaves the reaction inert rather than throwing.
				final Skill guardSkill = CommonSkill.NPC_ULTIMATE_DEFENSE.getSkill();
				if (guardSkill != null)
				{
					// Triggered, so the reaction does not interrupt what the monster is doing.
					SkillCaster.triggerCast(this, this, guardSkill);
				}
			}
		}
		else if (isAffectedBySkill(CommonSkill.NPC_ULTIMATE_DEFENSE.getId()))
		{
			stopSkillEffects(SkillFinishType.REMOVED, CommonSkill.NPC_ULTIMATE_DEFENSE.getId());
		}
	}
	
	/**
	 * Calculates the hate multiplier declared by the AI parameters SetHateGroup, SetHateOccupation and SetHateRace,<br>
	 * which increase the hate received from players of a matching class category, class or race.
	 * @param attacker the creature generating hate
	 * @return the hate multiplier for this attacker
	 */
	public double getHateRatio(Creature attacker)
	{
		if (!attacker.isPlayer())
		{
			return 1;
		}
		
		final StatSet params = getTemplate().getParameters();
		if (params.isEmpty())
		{
			return 1;
		}
		
		final Player player = attacker.asPlayer();
		int ratio = 0;
		final CategoryType group = params.getEnum("SetHateGroup", CategoryType.class, null);
		if ((group != null) && player.isInCategory(group))
		{
			ratio += params.getInt("SetHateGroupRatio", 0);
		}
		
		if (player.getPlayerClass().getId() == params.getInt("SetHateOccupation", -1))
		{
			ratio += params.getInt("SetHateOccupationRatio", 0);
		}
		
		if (player.getRace() == params.getEnum("SetHateRace", Race.class, null))
		{
			ratio += params.getInt("SetHateRaceRatio", 0);
		}
		
		return 1 + (ratio / 100.0);
	}
	
	/**
	 * Adds damage and aggro value to a specific creature.<br>
	 * This method updates the hate level of an attacker.<br>
	 * It handles special cases for fake players and summons.<br>
	 * It also ensures the AI intention becomes active if aggro is gained.
	 * @param creature The {@code Creature} that dealt the damage.
	 * @param damage The amount of damage dealt to this entity.
	 * @param aggroValue The amount of hate or aggro to add.
	 */
	public void addDamageHate(Creature creature, long damage, long aggroValue)
	{
		Creature attacker = creature;
		if ((attacker == null) || (attacker == this))
		{
			return;
		}
		
		// Check if fake players should aggro each other.
		if (isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AGGRO_FPC && attacker.isFakePlayer())
		{
			return;
		}
		
		// A mounted fake player dismounts when attacked (no effect if it is not mounted).
		if (isFakePlayer())
		{
			dismount();
		}
		
		Player targetPlayer = attacker.asPlayer();
		final Creature summoner = attacker.getSummoner();
		if (attacker.isNpc() && (summoner != null) && summoner.isPlayer() && !attacker.isTargetable())
		{
			targetPlayer = summoner.asPlayer();
			attacker = summoner;
		}
		
		// Get the AggroInfo of the attacker Creature from the _aggroList of the Attackable.
		final AggroInfo ai = _aggroList.computeIfAbsent(attacker, AggroInfo::new);
		ai.addDamage(damage);
		
		// Traps does not cause aggro.
		// Making this hack because not possible to determine if damage made by trap.
		// So just check for triggered trap here.
		long aggro = aggroValue;
		if ((targetPlayer == null) || (targetPlayer.getTrap() == null) || !targetPlayer.getTrap().isTriggered())
		{
			ai.addHate(aggro);
		}
		
		if ((targetPlayer != null) && (aggro == 0))
		{
			addDamageHate(attacker, 0, 1);
			
			// Set the intention to the Attackable to ACTIVE.
			if (getAI().getIntention() == Intention.IDLE)
			{
				getAI().setIntentionActive();
			}
			
			// Notify to scripts
			if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_AGGRO_RANGE_ENTER, this))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAggroRangeEnter(this, targetPlayer, attacker.isSummon()), this);
			}
		}
		else if ((targetPlayer == null) && (aggro == 0))
		{
			aggro = 1;
			ai.addHate(1);
		}
		
		// Set the intention to the Attackable to ACTIVE.
		if ((aggro != 0) && (getAI().getIntention() == Intention.IDLE))
		{
			getAI().setIntentionActive();
		}
	}
	
	/**
	 * Reduces the hate value of a specific target or all targets.<br>
	 * If the target is {@code null}, it applies the change to every creature in the aggro list.<br>
	 * This method also updates the global aggression state based on the resulting hate levels.
	 * @param target The {@code Creature} whose hate should be reduced, or {@code null} to affect all targets.
	 * @param amount The amount of hate to add to the target(s).
	 */
	public void reduceHate(Creature target, long amount)
	{
		if (target == null) // whole aggrolist
		{
			final Creature mostHated = getMostHated();
			if (mostHated == null) // makes target passive for a moment more
			{
				((AttackableAI) getAI()).setGlobalAggro(-25);
				return;
			}
			
			for (AggroInfo ai : _aggroList.values())
			{
				ai.addHate(amount);
			}
			
			if (getHating(mostHated) >= 0)
			{
				((AttackableAI) getAI()).setGlobalAggro(-25);
				clearAggroList();
				getAI().setIntentionActive();
				if (!isFakePlayer())
				{
					setWalking();
				}
			}
			return;
		}
		
		final AggroInfo ai = _aggroList.get(target);
		if (ai == null)
		{
			return;
		}
		
		ai.addHate(amount);
		if ((ai.getHate() >= 0) && (getMostHated() == null))
		{
			((AttackableAI) getAI()).setGlobalAggro(-25);
			clearAggroList();
			getAI().setIntentionActive();
			if (!isFakePlayer())
			{
				setWalking();
			}
		}
	}
	
	/**
	 * Removes the hate value for a specific creature.<br>
	 * This method checks if the {@code target} exists in the aggro list.<br>
	 * If found, it calls {@code stopHate()} on that entry.
	 * @param target The {@code Creature} to remove from the hate list.
	 */
	public void stopHating(Creature target)
	{
		if (target == null)
		{
			return;
		}
		
		final AggroInfo ai = _aggroList.get(target);
		if (ai != null)
		{
			ai.stopHate();
		}
	}
	
	/**
	 * Finds the creature that currently has the highest aggro level.<br>
	 * This method checks the internal {@code _aggroList}.<br>
	 * It returns {@code null} if no enemies are present or if this entity is dead.
	 * @return The {@code Creature} with the most hate, or {@code null} if none exist.
	 */
	public Creature getMostHated()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		Creature mostHated = null;
		long maxHate = 0;
		
		// While Interacting over This Map Removing Object is Not Allowed.
		// Go through the aggroList of the Attackable.
		for (AggroInfo ai : _aggroList.values())
		{
			if (ai == null)
			{
				continue;
			}
			
			if (ai.checkHate(this) > maxHate)
			{
				mostHated = ai.getAttacker();
				maxHate = ai.getHate();
			}
		}
		
		return mostHated;
	}
	
	/**
	 * Retrieves the two creatures that have caused the most aggro.<br>
	 * It checks the current {@code _aggroList} to find these targets.<br>
	 * If no valid aggro exists, it returns {@code null}.
	 * @return A {@code List} containing the top two hated {@code Creature} objects.
	 */
	public List<Creature> get2MostHated()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		Creature mostHated = null;
		Creature secondMostHated = null;
		long maxHate = 0;
		final List<Creature> result = new ArrayList<>();
		
		// While iterating over this map removing objects is not allowed.
		// Go through the aggroList of the Attackable.
		for (AggroInfo ai : _aggroList.values())
		{
			if (ai.checkHate(this) > maxHate)
			{
				secondMostHated = mostHated;
				mostHated = ai.getAttacker();
				maxHate = ai.getHate();
			}
		}
		
		result.add(mostHated);
		
		if (getAttackByList().contains(secondMostHated))
		{
			result.add(secondMostHated);
		}
		else
		{
			result.add(null);
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of creatures that currently have hate against this entity.<br>
	 * This method filters the aggro list to include only those who meet the hate criteria.<br>
	 * It returns {@code null} if there are no active aggressors or if the entity is dead.
	 * @return A {@code List} of {@code Creature} objects that have hate, or {@code null}.
	 */
	public List<Creature> getHateList()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		final List<Creature> result = new ArrayList<>();
		for (AggroInfo ai : _aggroList.values())
		{
			ai.checkHate(this);
			
			result.add(ai.getAttacker());
		}
		
		return result;
	}
	
	/**
	 * Retrieves the current hate value for a specific target.<br>
	 * This method checks if the target is valid and currently attacking.<br>
	 * It returns 0 if the target is null, not in the aggro list, or has special protections.
	 * @param target The {@code Creature} to check for hate values.
	 * @return The current hate value as a {@code long}.
	 */
	public long getHating(Creature target)
	{
		if (_aggroList.isEmpty() || (target == null))
		{
			return 0;
		}
		
		final AggroInfo ai = _aggroList.get(target);
		if (ai == null)
		{
			return 0;
		}
		
		final Creature aiAttacker = ai.getAttacker();
		if (aiAttacker.isPlayer())
		{
			final Player act = aiAttacker.asPlayer();
			if (act.isInvisible() || act.isInvul() || act.isSpawnProtected())
			{
				// Remove Object Should Use This Method and Can be Blocked While Interacting.
				_aggroList.remove(target);
				return 0;
			}
		}
		
		if (!aiAttacker.isSpawned() || aiAttacker.isInvisible())
		{
			_aggroList.remove(target);
			return 0;
		}
		
		if (aiAttacker.isAlikeDead() && !(aiAttacker.isPlayer() && aiAttacker.asPlayer().isFakeDeathSeenBy(this)))
		{
			ai.stopHate();
			return 0;
		}
		
		return ai.getHate();
	}
	
	/**
	 * Handles the logic for dropping items when a creature dies.<br>
	 * It uses the template of the current entity to determine loot.<br>
	 * The {@code mainDamageDealer} is used to calculate specific rewards.
	 * @param mainDamageDealer The {@code Creature} that dealt the most damage.
	 */
	public void doItemDrop(Creature mainDamageDealer)
	{
		doItemDrop(getTemplate(), mainDamageDealer);
	}
	
	/**
	 * Handles the logic for dropping items when an NPC dies.<br>
	 * It determines which items to drop based on the {@code NpcTemplate}.<br>
	 * The method checks if the {@code mainDamageDealer} is a player or a fake player.<br>
	 * It also handles auto-loot mechanics and raid boss notifications.
	 * @param npcTemplate The template containing the loot data for the NPC.
	 * @param mainDamageDealer The creature that dealt the primary damage to the NPC.
	 */
	public void doItemDrop(NpcTemplate npcTemplate, Creature mainDamageDealer)
	{
		if (mainDamageDealer == null)
		{
			return;
		}
		
		final Player player = mainDamageDealer.asPlayer();
		
		// Don't drop anything if the last attacker or owner isn't Player.
		if (player == null)
		{
			// Unless its a fake player and they can drop items.
			if (mainDamageDealer.isFakePlayer() && FakePlayersConfig.FAKE_PLAYER_CAN_DROP_ITEMS)
			{
				final Collection<ItemHolder> deathItems = npcTemplate.calculateDrops(DropType.DROP, this, mainDamageDealer);
				if (deathItems != null)
				{
					for (ItemHolder drop : deathItems)
					{
						final ItemTemplate item = ItemData.getInstance().getTemplate(drop.getId());
						
						// Check if the autoLoot mode is active.
						if (PlayerConfig.AUTO_LOOT_ITEM_IDS.contains(item.getId()) || isFlying() || (!item.hasExImmediateEffect() && ((!_isRaid && PlayerConfig.AUTO_LOOT) || (_isRaid && PlayerConfig.AUTO_LOOT_RAIDS))))
						{
							// Do nothing.
						}
						else if (PlayerConfig.AUTO_LOOT_HERBS && item.hasExImmediateEffect())
						{
							for (SkillHolder skillHolder : item.getAllSkills())
							{
								SkillCaster.triggerCast(mainDamageDealer, null, skillHolder.getSkill(), null, false);
							}
							
							mainDamageDealer.broadcastInfo(); // ? check if this is necessary
						}
						else
						{
							final Item droppedItem = dropItem(mainDamageDealer, drop); // Drop the item on the ground.
							if (FakePlayersConfig.FAKE_PLAYER_CAN_PICKUP)
							{
								mainDamageDealer.getFakePlayerDrops().add(droppedItem);
							}
						}
					}
					
					deathItems.clear();
				}
			}
			return;
		}
		
		CursedWeaponsManager.getInstance().checkDrop(this, player);
		if (isSpoiled() && !_plundered)
		{
			_sweepItems.set(npcTemplate.calculateDrops(DropType.SPOIL, this, player));
		}
		
		final Collection<ItemHolder> deathItems = npcTemplate.calculateDrops(DropType.DROP, this, player);
		if (deathItems != null)
		{
			List<Integer> announceItems = null;
			for (ItemHolder drop : deathItems)
			{
				final ItemTemplate item = ItemData.getInstance().getTemplate(drop.getId());
				final Integer itemId = item.getId();
				
				// Check if the autoLoot mode is active.
				if (PlayerConfig.AUTO_LOOT_ITEM_IDS.contains(itemId) || isFlying() || (!item.hasExImmediateEffect() && ((!_isRaid && PlayerConfig.AUTO_LOOT) || (_isRaid && PlayerConfig.AUTO_LOOT_RAIDS))) || (item.hasExImmediateEffect() && PlayerConfig.AUTO_LOOT_HERBS))
				{
					player.doAutoLoot(this, drop); // Give the item(s) to the Player that has killed the Attackable.
				}
				else
				{
					dropItem(player, drop); // Drop the item on the ground.
				}
				
				// Broadcast message if RaidBoss was defeated.
				if (_isRaid && !_isRaidMinion)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.C1_DIED_AND_DROPPED_S2_X_S3);
					sm.addString(getName());
					sm.addItemName(item);
					sm.addLong(drop.getCount());
					broadcastPacket(sm);
					if (RaidDropAnnounceData.getInstance().isAnnounce(itemId))
					{
						if (announceItems == null)
						{
							announceItems = new ArrayList<>(3);
						}
						
						if (announceItems.size() < 3)
						{
							announceItems.add(itemId);
						}
					}
				}
			}
			
			if (announceItems != null)
			{
				World.broadcastToAllOnlinePlayers(new ExRaidDropItemAnnounce(player.getName(), getId(), announceItems));
			}
			
			deathItems.clear();
		}
		
		// Items that can be obtained with a daily limit.
		final Collection<LimitedDropHolder> limitedDropList = npcTemplate.getLimitedDropList();
		if (limitedDropList != null)
		{
			for (LimitedDropHolder holder : limitedDropList)
			{
				final Player member = Quest.getRandomPartyMember(player);
				int playerMaxLevel = holder.getMaxPlayerLevel() + RatesConfig.LIMITED_DROP_MAX_LEVEL_INCREASE;
				int dailyLimit = holder.getDailyLimit();
				double itemChance = holder.getChance();
				final Integer holderItemId = holder.getItemId();
				
				if (RatesConfig.LIMITED_DROP_DAILY_LIMIT_BY_ID.get(holderItemId) != null)
				{
					dailyLimit *= RatesConfig.LIMITED_DROP_DAILY_LIMIT_BY_ID.get(holderItemId);
				}
				
				if (RatesConfig.LIMITED_DROP_CHANCE_BY_ID.get(holderItemId) != null)
				{
					itemChance *= RatesConfig.LIMITED_DROP_CHANCE_BY_ID.get(holderItemId);
				}
				
				if ((member.getLevel() >= holder.getMinPlayerLevel()) && (member.getLevel() <= playerMaxLevel) && ((Rnd.get(100d) < itemChance) && ((member.getParty() == null) || member.isInsideRadius3D(this, PlayerConfig.ALT_PARTY_RANGE))))
				{
					final String variableName = PlayerVariables.LIMITED_DROP_COUNT_VAR + holderItemId;
					final PlayerVariables variables = member.getVariables();
					final int count = variables.getInt(variableName, 0);
					// final int dailyLimit = holder.getDailyLimit();
					if (count < dailyLimit)
					{
						variables.set(variableName, count + 1);
						member.addItem(ItemProcessType.DROP, holderItemId, Rnd.get(holder.getMinDropAmount(), holder.getMaxDropAmount()), this, true);
					}
					else
					{
						if (count == dailyLimit)
						{
							variables.set(variableName, count + 1);
							member.sendPacket(SystemMessageId.YOU_EXCEEDED_THE_LIMIT_AND_CANNOT_COMPLETE_THE_TASK);
						}
						
						member.sendMessage("You obtained all available " + ItemData.getInstance().getTemplate(holderItemId).getName() + " for this day!");
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the weapon currently being used by the actor.<br>
	 * This method returns {@code null} if no weapon is active.
	 * @return the current {@code Item} being held as a weapon, or {@code null}.
	 */
	public Item getActiveWeapon()
	{
		return null;
	}
	
	/**
	 * Checks if a specific creature is currently in the aggro list.<br>
	 * This method verifies that the {@code Creature} object is not {@code null}.<br>
	 * It returns {@code true} if the creature exists in the internal aggro map.
	 * @param creature The {@code Creature} to check for aggro status.
	 * @return {@code true} if the creature is in the aggro list, otherwise {@code false}.
	 */
	public boolean isInAggroList(Creature creature)
	{
		return (creature != null) && _aggroList.containsKey(creature);
	}
	
	/**
	 * Clears the current list of aggressive entities.<br>
	 * This method also resets all overhit related values to their default states.
	 */
	public void clearAggroList()
	{
		_aggroList.clear();
		
		// clear overhit values
		_overhit = false;
		_overhitDamage = 0;
		_overhitAttacker = null;
	}
	
	/**
	 * Checks if the sweep action is currently active.<br>
	 * It returns {@code true} if there are items in the sweep list.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if a sweep is active, {@code false} otherwise.
	 */
	@Override
	public boolean isSweepActive()
	{
		return _sweepItems.get() != null;
	}
	
	/**
	 * Retrieves the list of items obtained from a sweep action.<br>
	 * This method converts {@code ItemHolder} objects into {@code ItemTemplate} objects.<br>
	 * It returns an empty list if no sweep items are present.
	 * @return A {@code List} of {@code ItemTemplate} objects.
	 */
	public List<ItemTemplate> getSpoilLootItems()
	{
		final Collection<ItemHolder> sweepItems = _sweepItems.get();
		final List<ItemTemplate> lootItems = new ArrayList<>();
		if (sweepItems != null)
		{
			for (ItemHolder item : sweepItems)
			{
				lootItems.add(ItemData.getInstance().getTemplate(item.getId()));
			}
		}
		
		return lootItems;
	}
	
	/**
	 * Retrieves the collection of items gathered during a sweep.<br>
	 * This method clears the internal storage after retrieval.
	 * @return A {@code Collection} of {@code ItemHolder} objects or {@code null}.
	 */
	public Collection<ItemHolder> takeSweep()
	{
		return _sweepItems.getAndSet(null);
	}
	
	/**
	 * Retrieves and clears the current harvest item.<br>
	 * This method returns the {@code ItemHolder} object if one exists.<br>
	 * It sets the internal reference to {@code null} after retrieval.
	 * @return the {@code ItemHolder} that was being harvested, or {@code null} if none exist.
	 */
	public ItemHolder takeHarvest()
	{
		return _harvestItem.getAndSet(null);
	}
	
	/**
	 * Checks if a corpse is too old to be interacted with.<br>
	 * This method verifies the remaining decay time of an entity.<br>
	 * It may send a system message to the {@code attacker} if requested.
	 * @param attacker The {@code Player} attempting to interact with the corpse.
	 * @param remainingTime The threshold for the decay time.
	 * @param sendMessage Whether to notify the player that the corpse is too old.
	 * @return {@code true} if the corpse is considered too old, {@code false} otherwise.
	 */
	public boolean isOldCorpse(Player attacker, int remainingTime, boolean sendMessage)
	{
		if (isDead() && (DecayTaskManager.getInstance().getRemainingTime(this) < remainingTime))
		{
			if (sendMessage && (attacker != null))
			{
				attacker.sendPacket(SystemMessageId.THE_CORPSE_IS_TOO_OLD_THE_SKILL_CANNOT_BE_USED);
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if the given {@code Player} has priority rights over a spoiler.<br>
	 * This verifies if the player is the owner or part of the looter party.<br>
	 * If they lack rights and {@code sendMessage} is {@code true}, a system message is sent.
	 * @param sweeper The {@code Player} to check for ownership rights.
	 * @param sendMessage Whether to send a notification if the player lacks rights.
	 * @return {@code true} if the player has priority rights, {@code false} otherwise.
	 */
	public boolean checkSpoilOwner(Player sweeper, boolean sendMessage)
	{
		if ((sweeper.getObjectId() != _spoilerObjectId) && !sweeper.isInLooterParty(_spoilerObjectId))
		{
			if (sendMessage)
			{
				sweeper.sendPacket(SystemMessageId.THERE_ARE_NO_PRIORITY_RIGHTS_ON_A_SWEEPER);
			}
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * Enables or disables the overhit mechanic.<br>
	 * This method updates the {@code _overhit} flag for the actor.
	 * @param status The boolean value to set for overhit status.
	 */
	public void overhitEnabled(boolean status)
	{
		_overhit = status;
	}
	
	/**
	 * Calculates and sets the over-hit damage values for a creature.<br>
	 * This method determines how much extra damage was dealt beyond the remaining health.<br>
	 * If the strike does not kill the creature, all over-hit values are cleared.
	 * @param attacker The {@code Creature} that performed the attack.
	 * @param damage The total amount of damage dealt by the attack.
	 */
	public void setOverhitValues(Creature attacker, double damage)
	{
		// Calculate the over-hit damage.
		// Ex: mob had 10 HP left, over-hit skill did 50 damage total, over-hit damage is 40.
		final double overhitDmg = -(getCurrentHp() - damage);
		if (overhitDmg < 0)
		{
			// We didn't killed the mob with the over-hit strike. (it wasn't really an over-hit strike).
			// Let's just clear all the over-hit related values.
			overhitEnabled(false);
			_overhitDamage = 0;
			_overhitAttacker = null;
			return;
		}
		
		overhitEnabled(true);
		_overhitDamage = overhitDmg;
		_overhitAttacker = attacker;
	}
	
	/**
	 * Retrieves the {@code Creature} that caused an overhit.
	 * @return the {@code Creature} who performed the overhit or {@code null}.
	 */
	public Creature getOverhitAttacker()
	{
		return _overhitAttacker;
	}
	
	/**
	 * Retrieves the current overhit damage value.<br>
	 * This value is used to calculate extra damage dealt during an attack.
	 * @return The {@code double} value of the overhit damage.
	 */
	public double getOverhitDamage()
	{
		return _overhitDamage;
	}
	
	/**
	 * Checks if the current attack is an overhit.
	 * @return {@code true} if the attack is an overhit, {@code false} otherwise.
	 */
	public boolean isOverhit()
	{
		return _overhit;
	}
	
	/**
	 * Calculates the experience and SP rewards for a character.<br>
	 * This method determines rewards based on the level difference between the character and the monster.<br>
	 * It applies specific multipliers if the character is high level and significantly stronger than the target.
	 * @param charLevel The level of the character receiving the reward.
	 * @param damage The amount of damage dealt by the character.
	 * @param totalDamage The total damage received by the monster.
	 * @return A {@code double[]} array containing the calculated experience and SP values.
	 */
	private double[] calculateExpAndSp(int charLevel, long damage, long totalDamage)
	{
		final int levelDiff = charLevel - getLevel();
		double xp = 0;
		double sp = 0;
		if ((levelDiff < RatesConfig.MONSTER_EXP_MAX_LEVEL_DIFFERENCE) && (levelDiff > -RatesConfig.MONSTER_EXP_MAX_LEVEL_DIFFERENCE))
		{
			xp = Math.max(0, (getExpReward(charLevel) * damage) / totalDamage);
			sp = Math.max(0, (getSpReward(charLevel) * damage) / totalDamage);
			if ((charLevel > 84) && (levelDiff <= -3))
			{
				double mul;
				switch (levelDiff)
				{
					case -3:
					{
						mul = 0.97;
						break;
					}
					case -4:
					{
						mul = 0.67;
						break;
					}
					case -5:
					{
						mul = 0.42;
						break;
					}
					case -6:
					{
						mul = 0.25;
						break;
					}
					case -7:
					{
						mul = 0.15;
						break;
					}
					case -8:
					{
						mul = 0.09;
						break;
					}
					case -9:
					{
						mul = 0.05;
						break;
					}
					case -10:
					{
						mul = 0.03;
						break;
					}
					default:
					{
						mul = 1;
						break;
					}
				}
				
				xp *= mul;
				sp *= mul;
			}
		}
		
		return new double[]
		{
			xp,
			sp
		};
	}
	
	/**
	 * Calculates the experience bonus based on over-hit damage.<br>
	 * This method determines a percentage of the total extra damage dealt relative to maximum health.<br>
	 * The resulting bonus is capped at 25% of the base experience.
	 * @param exp The base amount of experience to calculate the bonus from.
	 * @return The calculated over-hit experience bonus as a {@code double}.
	 */
	public double calculateOverhitExp(double exp)
	{
		// Get the percentage based on the total of extra (over-hit) damage done relative to the total (maximum) ammount of HP on the Attackable.
		double overhitPercentage = ((_overhitDamage * 100) / getMaxHp());
		
		// Over-hit damage percentages are limited to 25% max.
		if (overhitPercentage > 25)
		{
			overhitPercentage = 25;
		}
		
		// Get the overhit exp bonus according to the above over-hit damage percentage.
		// (1/1 basis - 13% of over-hit damage, 13% of extra exp is given, and so on...).
		return (overhitPercentage / 100) * exp;
	}
	
	/**
	 * Checks if the entity is currently capable of being attacked.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the entity can be attacked.
	 */
	@Override
	public boolean canBeAttacked()
	{
		return true;
	}
	
	/**
	 * This method is called when the actor is spawned into the world.<br>
	 * It resets various internal states such as aggro lists and rewards.<br>
	 * It also initializes the AI behavior based on the current region status.
	 */
	@Override
	public void onSpawn()
	{
		// Keep track of the spawn time.
		_spawnTime = System.currentTimeMillis();
		
		super.onSpawn();
		
		// Clear mob spoil, seed.
		setSpoilerObjectId(0);
		
		// Clear all aggro list and overhit.
		clearAggroList();
		
		// Clear Harvester reward
		_harvestItem.set(null);
		_sweepItems.set(null);
		_plundered = false;
		
		// fake players
		if (isFakePlayer())
		{
			getFakePlayerDrops().clear(); // Clear existing fake player drops.
			setReputation(0); // reset reputation
			setScriptValue(0); // remove pvp flag
			setRunning(); // don't walk
		}
		else
		{
			setWalking();
		}
		
		// Clear mod Seeded stat
		_seeded = false;
		_seed = null;
		_seederObjId = 0;
		
		// Check the region where this mob is, do not activate the AI if region is inactive.
		if (hasAI())
		{
			// Roll the aggro timeout again, the AI object survives a respawn.
			if (getAI() instanceof AttackableAI)
			{
				((AttackableAI) getAI()).rollGlobalAggro();
			}
			
			// Set the intention of the Attackable to ACTIVE.
			final CreatureAI ai = getAI();
			ai.setIntentionActive();
			
			// Check the region where this mob is, do not activate the AI if region is inactive.
			if (!isInActiveRegion())
			{
				ai.stopAITask();
			}
		}
	}
	
	/**
	 * @return the time in milliseconds when this attackable was spawned.
	 */
	public long getSpawnTime()
	{
		return _spawnTime;
	}
	
	/**
	 * This method is called when an entity respawns.<br>
	 * It resets the champion state and determines if the monster becomes a champion.<br>
	 * It also updates the team status based on the champion configuration.<br>
	 * Finally, it calls the superclass implementation to reset other NPC states.
	 */
	@Override
	public void onRespawn()
	{
		// Reset champion state
		_champion = false;
		
		// Set champion on next spawn.
		final int level = getLevel();
		if (ChampionMonstersConfig.CHAMPION_ENABLE && isMonster() && !isQuestMonster() && !getTemplate().isUndying() && !_isRaid && !_isRaidMinion && (ChampionMonstersConfig.CHAMPION_FREQUENCY > 0) && (level >= ChampionMonstersConfig.CHAMP_MIN_LEVEL) && (level <= ChampionMonstersConfig.CHAMP_MAX_LEVEL) && (ChampionMonstersConfig.CHAMPION_ENABLE_IN_INSTANCES || (getInstanceId() == 0)))
		{
			if (Rnd.get(100) < ChampionMonstersConfig.CHAMPION_FREQUENCY)
			{
				_champion = true;
			}
			
			if (ChampionMonstersConfig.SHOW_CHAMPION_AURA)
			{
				setTeam(_champion ? Team.RED : Team.NONE, false);
			}
		}
		
		// Reset the rest of NPC related states.
		super.onRespawn();
	}
	
	/**
	 * Checks if the current entity is a spoiler.<br>
	 * Returns {@code true} if the internal ID is not {@code 0}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if spoiled, {@code false} otherwise.
	 */
	public boolean isSpoiled()
	{
		return _spoilerObjectId != 0;
	}
	
	/**
	 * Retrieves the unique identifier for the spoiler object.<br>
	 * This value is used to identify specific spoiler entities in the game world.
	 * @return The {@code int} ID of the spoiler object.
	 */
	public int getSpoilerObjectId()
	{
		return _spoilerObjectId;
	}
	
	/**
	 * Sets the unique identifier for a spoiler object.<br>
	 * This value is used to link the entity to specific spoiler data.
	 * @param spoilerObjectId The {@code int} ID of the spoiler object.
	 */
	public void setSpoilerObjectId(int spoilerObjectId)
	{
		_spoilerObjectId = spoilerObjectId;
	}
	
	/**
	 * Marks the entity as plundered by a specific player.<br>
	 * This method updates the spoiler status and calculates the loot for the {@code Player}.
	 * @param player The {@code Player} who plundered the entity.
	 */
	public void setPlundered(Player player)
	{
		_plundered = true;
		_spoilerObjectId = player.getObjectId();
		_sweepItems.set(getTemplate().calculateDrops(DropType.SPOIL, this, player));
	}
	
	/**
	 * Sets the harvest item based on a specific {@code Player}.<br>
	 * This method checks if the provided {@code seeder} matches the required seed.<br>
	 * It calculates the drop count based on skills and level differences.<br>
	 * The resulting {@code ItemHolder} is then stored in the harvest item field.
	 * @param seeder The {@code Player} used to determine the harvest rewards.
	 */
	public void setSeeded(Player seeder)
	{
		if ((_seed != null) && (_seederObjId == seeder.getObjectId()))
		{
			_seeded = true;
			int count = 1;
			for (int skillId : getTemplate().getSkills().keySet())
			{
				switch (skillId)
				{
					case 4303: // Strong type x2
					{
						count *= 2;
						break;
					}
					case 4304: // Strong type x3
					{
						count *= 3;
						break;
					}
					case 4305: // Strong type x4
					{
						count *= 4;
						break;
					}
					case 4306: // Strong type x5
					{
						count *= 5;
						break;
					}
					case 4307: // Strong type x6
					{
						count *= 6;
						break;
					}
					case 4308: // Strong type x7
					{
						count *= 7;
						break;
					}
					case 4309: // Strong type x8
					{
						count *= 8;
						break;
					}
					case 4310: // Strong type x9
					{
						count *= 9;
						break;
					}
				}
			}
			
			// hi-level mobs bonus
			final int diff = getLevel() - _seed.getLevel() - 5;
			if (diff > 0)
			{
				count += diff;
			}
			
			_harvestItem.set(new ItemHolder(_seed.getCropId(), count * RatesConfig.RATE_DROP_MANOR));
		}
	}
	
	/**
	 * Sets the initial random seed for this entity.<br>
	 * This operation only occurs if a seed has not been set yet.
	 * @param seed The {@code Seed} value to use for randomization.
	 * @param seeder The {@code Player} who provided the seed.
	 */
	public void setSeeded(Seed seed, Player seeder)
	{
		if (!_seeded)
		{
			_seed = seed;
			_seederObjId = seeder.getObjectId();
		}
	}
	
	/**
	 * Retrieves the unique identifier for the seeder object.<br>
	 * This ID is used to identify specific seeding entities.
	 * @return The {@code int} value of the seeder object ID.
	 */
	public int getSeederId()
	{
		return _seederObjId;
	}
	
	/**
	 * Retrieves the current seed value.<br>
	 * This value is used for random number generation.
	 * @return the {@code Seed} object.
	 */
	public Seed getSeed()
	{
		return _seed;
	}
	
	/**
	 * Checks if the entity has been seeded.<br>
	 * This is used to determine if random values are being generated from a fixed seed.
	 * @return {@code true} if the entity is seeded, {@code false} otherwise.
	 */
	public boolean isSeeded()
	{
		return _seeded;
	}
	
	/**
	 * Check if the server allows Random Animation.
	 */
	// This is located here because Monster and FriendlyMob both extend this class. The other non-pc instances extend either Npc or Monster.
	/**
	 * Checks if the actor should play a random animation.<br>
	 * This depends on the global configuration and whether random animations are enabled.<br>
	 * It returns {@code false} if the actor is an instance of {@code GrandBoss}.
	 * @return {@code true} if random animations are allowed for this actor, {@code false} otherwise.
	 */
	@Override
	public boolean hasRandomAnimation()
	{
		return ((GeneralConfig.MAX_MONSTER_ANIMATION > 0) && isRandomAnimationEnabled() && !(this instanceof GrandBoss));
	}
	
	/**
	 * Sets the timer for the command channel.<br>
	 * This method updates the {@code _commandChannelTimer} field with a new value.
	 * @param commandChannelTimer The new {@code CommandChannelTimer} to assign.
	 */
	public void setCommandChannelTimer(CommandChannelTimer commandChannelTimer)
	{
		_commandChannelTimer = commandChannelTimer;
	}
	
	/**
	 * Retrieves the current timer for the command channel.
	 * @return the {@code CommandChannelTimer} instance.
	 */
	public CommandChannelTimer getCommandChannelTimer()
	{
		return _commandChannelTimer;
	}
	
	/**
	 * Retrieves the first {@code CommandChannel} that was attacked.<br>
	 * This value is used to track initial combat interactions.
	 * @return The {@code CommandChannel} of the first attacker, or {@code null} if none exists.
	 */
	public CommandChannel getFirstCommandChannelAttacked()
	{
		return _firstCommandChannelAttacked;
	}
	
	/**
	 * Sets the first command channel that was attacked.<br>
	 * This value is used to track the initial target of an attack.
	 * @param firstCommandChannelAttacked The {@code CommandChannel} object representing the first attacked channel.
	 */
	public void setFirstCommandChannelAttacked(CommandChannel firstCommandChannelAttacked)
	{
		_firstCommandChannelAttacked = firstCommandChannelAttacked;
	}
	
	/**
	 * Retrieves the timestamp of the last attack from the command channel.<br>
	 * This value is used to track recent combat activity.
	 * @return The {@code long} timestamp of the last attack.
	 */
	public long getCommandChannelLastAttack()
	{
		return _commandChannelLastAttack;
	}
	
	/**
	 * Updates the timestamp of the last attack on a specific command channel.<br>
	 * This value is used to track recent combat activity.
	 * @param channelLastAttack The timestamp of the last attack.
	 */
	public void setCommandChannelLastAttack(long channelLastAttack)
	{
		_commandChannelLastAttack = channelLastAttack;
	}
	
	/**
	 * Resets the aggro list and moves the entity back to its spawn point.<br>
	 * This method clears all current targets.<br>
	 * If the entity has an active AI, it sets the intention to move to the spawn location.
	 */
	public void returnHome()
	{
		clearAggroList();
		
		if (hasAI() && (getSpawn() != null))
		{
			getAI().setIntentionMoveTo(getSpawn().getLocation());
		}
	}
	
	/*
	 * Return vitality points decrease (if positive) or increase (if negative) based on damage. Maximum for damage = maxHp.
	 */
	/**
	 * Calculates the vitality points to be consumed based on character stats.<br>
	 * This method returns a negative value representing the amount of vitality lost.<br>
	 * It checks if the level and experience rewards are valid before calculating.
	 * @param level The current level of the entity.
	 * @param exp The experience reward to be processed.
	 * @param isBoss A boolean indicating if the entity is a boss.
	 * @return The negative vitality points value or 0 if conditions are not met.
	 */
	public int getVitalityPoints(int level, double exp, boolean isBoss)
	{
		final int currentLevel = getLevel();
		if ((currentLevel <= 0) || (getExpReward(level) <= 0) || (isBoss && (NpcConfig.VITALITY_CONSUME_BY_BOSS == 0)))
		{
			return 0;
		}
		
		int points;
		if (level < 85)
		{
			points = Math.max((int) ((exp / 1000) * Math.max(level - currentLevel, 1)), 1);
		}
		else
		{
			points = Math.max((int) ((exp / (isBoss ? NpcConfig.VITALITY_CONSUME_BY_BOSS : NpcConfig.VITALITY_CONSUME_BY_MOB)) * Math.max(level - currentLevel, 1)), 1);
		}
		
		return -points;
	}
	
	/*
	 * True if vitality rate for exp and sp should be applied
	 */
	/**
	 * Checks if the vitality rate should be applied to this entity.<br>
	 * This depends on whether the entity is a champion and the global configuration.
	 * @return {@code true} if the vitality rate is active, {@code false} otherwise.
	 */
	public boolean useVitalityRate()
	{
		return !_champion || ChampionMonstersConfig.CHAMPION_ENABLE_VITALITY;
	}
	
	/**
	 * Checks if the current entity is part of a raid.
	 * @return {@code true} if it is a raid, {@code false} otherwise.
	 */
	@Override
	public boolean isRaid()
	{
		return _isRaid;
	}
	
	/**
	 * Sets whether the actor is currently in a raid.<br>
	 * Use {@code true} to indicate the actor is part of a raid.<br>
	 * Use {@code false} to indicate it is not.
	 * @param isRaid The raid status to set.
	 */
	public void setIsRaid(boolean isRaid)
	{
		_isRaid = isRaid;
	}
	
	/**
	 * Sets whether the actor is considered a raid minion.<br>
	 * This updates both the {@code _isRaid} and {@code _isRaidMinion} flags.
	 * @param value The boolean value to set for the raid minion status.
	 */
	public void setIsRaidMinion(boolean value)
	{
		_isRaid = value;
		_isRaidMinion = value;
	}
	
	/**
	 * Checks if the current actor is a raid minion.<br>
	 * This is used to identify specific types of monsters during raids.
	 * @return {@code true} if the actor is a raid minion, {@code false} otherwise.
	 */
	@Override
	public boolean isRaidMinion()
	{
		return _isRaidMinion;
	}
	
	/**
	 * Checks if this actor is a minion.<br>
	 * A minion is defined as any entity that has a leader.
	 * @return {@code true} if the entity has a leader, {@code false} otherwise.
	 */
	@Override
	public boolean isMinion()
	{
		return getLeader() != null;
	}
	
	/**
	 * Retrieves the leader of the current group.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Attackable} leader or {@code null} if no leader exists.
	 */
	public Attackable getLeader()
	{
		return null;
	}
	
	/**
	 * Checks if the current actor is a champion.
	 * @return {@code true} if the actor is a champion, {@code false} otherwise.
	 */
	@Override
	public boolean isChampion()
	{
		return _champion;
	}
	
	/**
	 * Checks if the entity can be attacked by others.<br>
	 * This method currently always returns {@code true}.
	 * @return {@code true} if the entity is attackable.
	 */
	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	/**
	 * Converts the current object into an {@code Attackable} type.<br>
	 * This method returns the current instance to allow for method chaining.
	 * @return The current instance cast as {@code Attackable}.
	 */
	@Override
	public Attackable asAttackable()
	{
		return this;
	}
	
	/**
	 * Sets the current target for this actor.<br>
	 * If {@code null} is provided, it clears the current target and resets aggression.<br>
	 * This method updates the internal state of the AI and aggro list.
	 * @param object The {@code WorldObject} to set as the new target.
	 */
	@Override
	public void setTarget(WorldObject object)
	{
		if (isDead())
		{
			return;
		}
		
		if (object == null)
		{
			final WorldObject target = getTarget();
			if (target != null)
			{
				_aggroList.remove(target);
			}
			
			final CreatureAI ai = getAI();
			if (_aggroList.isEmpty())
			{
				if (ai instanceof AttackableAI)
				{
					((AttackableAI) ai).setGlobalAggro(-25);
				}
				
				if (!isFakePlayer())
				{
					setWalking();
				}
				
				clearAggroList();
			}
			
			ai.setIntentionActive();
		}
		
		super.setTarget(object);
	}
}
