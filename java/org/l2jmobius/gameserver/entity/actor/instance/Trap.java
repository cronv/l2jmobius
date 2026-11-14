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
package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.TrapAction;
import org.l2jmobius.gameserver.entity.actor.tasks.npc.trap.TrapTask;
import org.l2jmobius.gameserver.entity.actor.tasks.npc.trap.TrapTriggerTask;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnTrapAction;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExDamagePopUp;
import org.l2jmobius.gameserver.network.serverpackets.ExMagicAttackInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;

/**
 * Represents an instance of a trap in the game world.<br>
 * This class handles the behavior and logic for interactive traps that affect players or creatures.
 * @author nBd
 */
public class Trap extends Npc
{
	private static final int TICK = 1000; // 1s
	private boolean _hasLifeTime;
	private boolean _isInArena = false;
	private boolean _isTriggered;
	private final int _lifeTime;
	private Player _owner;
	private final Set<Integer> _playersWhoDetectedMe = new HashSet<>();
	private final SkillHolder _skill;
	private int _remainingTime;
	
	// Tasks
	private ScheduledFuture<?> _trapTask = null;
	
	/**
	 * Creates a new {@code Trap} instance using a template.<br>
	 * This constructor initializes the trap with specific properties and life time.
	 * @param template The {@code NpcTemplate} used to define the trap's base properties.
	 * @param instanceId The unique identifier for this specific trap instance.
	 * @param lifeTime The duration in milliseconds before the trap expires. Use a negative value for infinite life.
	 */
	public Trap(NpcTemplate template, int instanceId, int lifeTime)
	{
		super(template);
		setInstanceType(InstanceType.Trap);
		setInstanceById(instanceId);
		setName(template.getName());
		setInvul(false);
		_owner = null;
		_isTriggered = false;
		_skill = getParameters().getObject("trap_skill", SkillHolder.class);
		_hasLifeTime = lifeTime >= 0;
		_lifeTime = lifeTime != 0 ? lifeTime : 30000;
		_remainingTime = _lifeTime;
		if (_skill != null)
		{
			_trapTask = ThreadPool.scheduleAtFixedRate(new TrapTask(this), TICK, TICK);
		}
	}
	
	/**
	 * Creates a new {@code Trap} instance owned by a specific player.<br>
	 * This constructor initializes the trap using the provided template and lifetime.<br>
	 * It also sets the {@code Player} as the owner of this trap.
	 * @param template The {@code NpcTemplate} used to define the trap's properties.
	 * @param owner The {@code Player} who owns this trap.
	 * @param lifeTime The duration in milliseconds before the trap expires.
	 */
	public Trap(NpcTemplate template, Player owner, int lifeTime)
	{
		this(template, owner.getInstanceId(), lifeTime);
		_owner = owner;
	}
	
	/**
	 * Sends a {@code ServerPacket} to all visible players.<br>
	 * It checks if the player can see this object or if the trap is already triggered.
	 * @param packet The {@code ServerPacket} to be sent to the players.
	 * @param includeSelf Determines whether to include the owner of the trap in the broadcast.
	 */
	@Override
	public void broadcastPacket(ServerPacket packet, boolean includeSelf)
	{
		World.forEachVisibleObject(this, Player.class, player ->
		{
			if (_isTriggered || canBeSeen(player))
			{
				player.sendPacket(packet);
			}
		});
	}
	
	/**
	 * Checks if a specific {@code Creature} is allowed to see this trap.<br>
	 * This method evaluates visibility based on ownership, party status, and game modes.<br>
	 * It returns {@code true} if the creature can see the trap and {@code false} otherwise.
	 * @param creature The {@code Creature} to check for visibility.
	 * @return {@code true} if the creature can see the trap; {@code false} otherwise.
	 */
	public boolean canBeSeen(Creature creature)
	{
		if ((creature != null) && _playersWhoDetectedMe.contains(creature.getObjectId()))
		{
			return true;
		}
		
		if ((_owner == null) || (creature == null))
		{
			return false;
		}
		
		if (creature == _owner)
		{
			return true;
		}
		
		if (creature.isPlayer())
		{
			// observers can't see trap
			if (creature.asPlayer().getObserverMode().isActive())
			{
				return false;
			}
			
			// olympiad competitors can't see trap
			if (_owner.isInOlympiadMode() && creature.asPlayer().isInOlympiadMode() && (creature.asPlayer().getOlympiadSide() != _owner.getOlympiadSide()))
			{
				return false;
			}
		}
		
		if (_isInArena)
		{
			return true;
		}
		
		if (_owner.isInParty() && creature.isInParty() && (_owner.getParty().getLeaderObjectId() == creature.getParty().getLeaderObjectId()))
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Removes this trap from the game world.<br>
	 * This method clears the owner reference before calling the parent deletion logic.
	 * @return {@code true} if the removal was successful, {@code false} otherwise.
	 */
	@Override
	public boolean deleteMe()
	{
		_owner = null;
		return super.deleteMe();
	}
	
	/**
	 * Retrieves the {@code Player} who owns this trap.
	 * @return the {@code Player} owner of the trap.
	 */
	@Override
	public Player asPlayer()
	{
		return _owner;
	}
	
	/**
	 * Retrieves the weapon currently being used by this trap.<br>
	 * This method returns {@code null} if no weapon is active.
	 * @return the current {@code Weapon} item or {@code null}.
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	/**
	 * Retrieves the reputation of the owner.<br>
	 * If there is no owner, it returns {@code 0}.
	 * @return The reputation value as an {@code int}.
	 */
	@Override
	public int getReputation()
	{
		return _owner != null ? _owner.getReputation() : 0;
	}
	
	/**
	 * Retrieves the {@code Player} who owns this trap.
	 * @return the {@code Player} owner of the trap, or {@code null} if there is no owner.
	 */
	public Player getOwner()
	{
		return _owner;
	}
	
	/**
	 * Retrieves the PvP flag of the trap owner.<br>
	 * If the trap has no owner, it returns {@code 0}.
	 * @return The PvP flag as a {@code byte}.
	 */
	@Override
	public byte getPvpFlag()
	{
		return _owner != null ? _owner.getPvpFlag() : 0;
	}
	
	/**
	 * Retrieves the secondary weapon instance for this trap.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Item} instance of the secondary weapon, or {@code null} if none exists.
	 */
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon item for this trap.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Weapon} object representing the secondary weapon, or {@code null}.
	 */
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	/**
	 * Retrieves the {@code Skill} associated with this trap.<br>
	 * Returns {@code null} if no skill is assigned to the trap.
	 * @return the {@code Skill} object or {@code null}.
	 */
	public Skill getSkill()
	{
		if (_skill == null)
		{
			return null;
		}
		
		return _skill.getSkill();
	}
	
	/**
	 * Checks if the trap can be attacked by a specific creature.<br>
	 * This method returns {@code true} if the attacker cannot see the trap.<br>
	 * It is used to determine if an entity is eligible for auto-attack.
	 * @param attacker The {@code Creature} attempting to attack the trap.
	 * @return {@code true} if the creature cannot see the trap, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return !canBeSeen(attacker);
	}
	
	/**
	 * Checks if the current entity is a trap.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if this instance is a trap.
	 */
	@Override
	public boolean isTrap()
	{
		return true;
	}
	
	/**
	 * Checks if the trap has been activated.
	 * @return {@code true} if the trap is triggered, {@code false} otherwise.
	 */
	public boolean isTriggered()
	{
		return _isTriggered;
	}
	
	/**
	 * This method is called when the trap instance is first created.<br>
	 * It initializes the internal state of the trap.<br>
	 * It checks if the trap is located in a {@code ZoneId.PVP} area.<br>
	 * It clears the list of players who have detected the trap.
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_isInArena = isInsideZone(ZoneId.PVP) && !isInsideZone(ZoneId.SIEGE);
		_playersWhoDetectedMe.clear();
	}
	
	/**
	 * Executes an attack on a specific target.<br>
	 * This method processes the damage and sends a message to the target.
	 * @param damage The amount of damage to deal.
	 * @param target The {@code Creature} receiving the attack.
	 * @param skill The {@code Skill} being used for the attack.
	 * @param isDOT Whether the damage is a Damage Over Time effect.
	 * @param directlyToHp Whether the damage applies directly to health points.
	 * @param critical Whether the attack deals critical damage.
	 * @param reflect Whether the attack can be reflected by the target.
	 */
	@Override
	public void doAttack(double damage, Creature target, Skill skill, boolean isDOT, boolean directlyToHp, boolean critical, boolean reflect)
	{
		super.doAttack(damage, target, skill, isDOT, directlyToHp, critical, reflect);
		sendDamageMessage(target, skill, (int) damage, critical, false);
	}
	
	/**
	 * Sends a damage notification to the owner of the trap.<br>
	 * This method handles olympiad notifications and block messages.<br>
	 * It sends a system message showing the damage dealt to the target.
	 * @param target The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} used to perform the attack.
	 * @param damage The amount of damage dealt.
	 * @param crit Whether the hit was a critical strike.
	 * @param miss Whether the attack missed the target.
	 */
	@Override
	public void sendDamageMessage(Creature target, Skill skill, int damage, boolean crit, boolean miss)
	{
		if (miss || (_owner == null))
		{
			sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.DODGE, skill));
			
			// target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.DODGE));
			return;
		}
		
		if (_owner.isInOlympiadMode() && target.isPlayer() && target.asPlayer().isInOlympiadMode() && (target.asPlayer().getOlympiadGameId() == _owner.getOlympiadGameId()))
		{
			OlympiadGameManager.getInstance().notifyCompetitorDamage(getOwner(), damage);
		}
		
		if (target.isHpBlocked() && !target.isNpc())
		{
			final int objectId = getObjectId();
			final int targetObjectId = target.getObjectId();
			if (skill == null)
			{
				sendPacket(new ExDamagePopUp(objectId, targetObjectId, 0, ExDamagePopUp.IMMUNE, skill));
				// target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.IMMUNE));
			}
			else
			{
				sendPacket(new ExMagicAttackInfo(objectId, targetObjectId, ExMagicAttackInfo.RESISTED));
				// target.sendPacket(new ExMagicAttackInfo(getObjectId(), target.getObjectId(), ExMagicAttackInfo.RESISTED));
			}
			
			_owner.sendPacket(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
		}
		else
		{
			final int objectId = getObjectId();
			final int targetObjectId = target.getObjectId();
			if (crit)
			{
				sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.CRITICAL, skill));
				target.sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.CRITICAL, skill));
			}
			else if (skill != null)
			{
				sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.SKILL_HIT, skill));
				target.sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.SKILL_HIT, skill));
			}
			else
			{
				sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.NORMAL_ATTACK, skill));
				target.sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.NORMAL_ATTACK, skill));
			}
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_DEALT_S3_DAMAGE_TO_C2);
			sm.addString(getName());
			sm.addString(target.getName());
			sm.addInt(damage);
			sm.addPopup(targetObjectId, objectId, (damage * -1));
			_owner.sendPacket(sm);
		}
	}
	
	/**
	 * Sends information about this trap to a specific player.<br>
	 * This packet is sent if the trap is already triggered or if the player can see it.
	 * @param player The {@code Player} who will receive the trap information.
	 */
	@Override
	public void sendInfo(Player player)
	{
		if (_isTriggered || canBeSeen(player))
		{
			player.sendPacket(new NpcInfo(this));
		}
	}
	
	/**
	 * Sets the creature that has detected this trap.<br>
	 * This method updates the list of detectors and notifies relevant scripts.<br>
	 * It also sends information to the detector if it is a playable character.
	 * @param detector The {@code Creature} that triggered or detected the trap.
	 */
	public void setDetected(Creature detector)
	{
		if (_isInArena)
		{
			if (detector.isPlayable())
			{
				sendInfo(detector.asPlayer());
			}
			return;
		}
		
		if ((_owner != null) && (_owner.getPvpFlag() == 0) && (_owner.getReputation() >= 0))
		{
			return;
		}
		
		_playersWhoDetectedMe.add(detector.getObjectId());
		
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_TRAP_ACTION, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnTrapAction(this, detector, TrapAction.TRAP_DETECTED), this);
		}
		
		if (detector.isPlayable())
		{
			sendInfo(detector.asPlayer());
		}
	}
	
	/**
	 * Stops the decay process for this trap.<br>
	 * This method cancels the scheduled task in {@code DecayTaskManager}.
	 */
	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancel(this);
	}
	
	/**
	 * Activates the trap on a specific creature.<br>
	 * This method sets the trap state to triggered and notifies listeners.<br>
	 * It also schedules a new {@code TrapTriggerTask}.
	 * @param target The {@code Creature} that triggered the trap.
	 */
	public void triggerTrap(Creature target)
	{
		if (_trapTask != null)
		{
			_trapTask.cancel(true);
			_trapTask = null;
		}
		
		_isTriggered = true;
		broadcastPacket(new NpcInfo(this));
		setTarget(target);
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_TRAP_ACTION, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnTrapAction(this, target, TrapAction.TRAP_TRIGGERED), this);
		}
		
		ThreadPool.schedule(new TrapTriggerTask(this), 500);
	}
	
	/**
	 * Removes the trap from the game world.<br>
	 * This method cancels any active {@code TrapTask}.<br>
	 * It also clears the owner and deletes the entity if it is currently spawned.
	 */
	public void unSummon()
	{
		if (_trapTask != null)
		{
			_trapTask.cancel(true);
			_trapTask = null;
		}
		
		_owner = null;
		if (isSpawned() && !isDead())
		{
			ZoneManager.getInstance().getRegion(this).removeFromZones(this);
			deleteMe();
		}
	}
	
	/**
	 * Checks if this trap has a limited life time.<br>
	 * Returns {@code true} if the trap is set to expire after a certain duration.<br>
	 * Returns {@code false} if the trap persists indefinitely.
	 * @return {@code true} if the trap has a life time, {@code false} otherwise.
	 */
	public boolean hasLifeTime()
	{
		return _hasLifeTime;
	}
	
	/**
	 * Sets whether this trap has a limited lifetime.<br>
	 * Use {@code true} to enable the lifetime property.<br>
	 * Use {@code false} to disable it.
	 * @param value The boolean value to set for the lifetime status.
	 */
	public void setHasLifeTime(boolean value)
	{
		_hasLifeTime = value;
	}
	
	/**
	 * Gets the time remaining for this trap.<br>
	 * This value is used to determine how long the trap stays active.
	 * @return The amount of time left as an {@code int}.
	 */
	public int getRemainingTime()
	{
		return _remainingTime;
	}
	
	/**
	 * Sets the remaining life time of the trap.<br>
	 * This updates the internal {@code _remainingTime} variable.
	 * @param time The new amount of time remaining.
	 */
	public void setRemainingTime(int time)
	{
		_remainingTime = time;
	}
	
	/**
	 * Retrieves the remaining life time of this trap.<br>
	 * The value represents how many ticks or seconds the trap will remain active.
	 * @return the current life time as an {@code int}.
	 */
	public int getLifeTime()
	{
		return _lifeTime;
	}
}
