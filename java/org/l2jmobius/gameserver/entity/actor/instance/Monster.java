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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.holders.npc.MinionList;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class represents the base logic for all monster entities in the game.<br>
 * It manages behaviors for various types including Minions, RaidBosses, and GrandBosses.
 */
public class Monster extends Attackable
{
	protected boolean _enableMinions = true;
	
	private Monster _master = null;
	private MinionList _minionList = null;
	
	/**
	 * Creates a new {@code Monster} instance using the provided template.<br>
	 * This constructor sets the instance type to {@code InstanceType.Monster}.<br>
	 * It also enables auto-attack functionality for the monster.
	 * @param template The {@code NpcTemplate} used to initialize the monster properties.
	 */
	public Monster(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Monster);
		setAutoAttackable(true);
	}
	
	/**
	 * Determines if the monster can be automatically attacked by a specific creature.<br>
	 * This method checks various conditions including fake player settings, aggression levels, and entity types.
	 * @param attacker The {@code Creature} attempting to attack this monster.
	 * @return {@code true} if the monster is auto-attackable by the provided {@code Creature}, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (isFakePlayer())
		{
			return FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE || isInCombat() || attacker.isMonster() || (getScriptValue() > 0);
		}
		
		// Check if the Monster target is aggressive.
		if (NpcConfig.GUARD_ATTACK_AGGRO_MOB && getTemplate().isAggressive() && (attacker instanceof Guard))
		{
			return true;
		}
		
		if (attacker.isMonster())
		{
			return attacker.isFakePlayer();
		}
		
		// Anything considers monsters friendly except Players, Attackables (Guards, Friendly NPC), Traps and EffectPoints.
		if (!attacker.isPlayable() && !attacker.isAttackable() && !(attacker instanceof Trap) && !(attacker instanceof EffectPoint))
		{
			return false;
		}
		
		return super.isAutoAttackable(attacker);
	}
	
	/**
	 * Checks if the monster will attack players.<br>
	 * It returns {@code true} if the template is aggressive and no passive effects are active.
	 * @return {@code true} if the monster is currently aggressive, {@code false} otherwise.
	 */
	@Override
	public boolean isAggressive()
	{
		return getTemplate().isAggressive() && !isAffected(EffectFlag.PASSIVE);
	}
	
	/**
	 * This method is called when the monster is first created in the world.<br>
	 * It initializes the monster's state and links it to its master if one exists.<br>
	 * It also triggers any dynamic script-based minion spawning logic.
	 */
	@Override
	public void onSpawn()
	{
		if (!isTeleporting() && (_master != null))
		{
			setRandomWalking(false);
			setIsRaidMinion(_master.isRaid());
			_master.getMinionList().onMinionSpawn(this);
		}
		
		// Dynamic script-based minions spawned here, after all preparations.
		super.onSpawn();
	}
	
	/**
	 * This method is called when the monster is teleported.<br>
	 * It ensures that all minions are notified of the master's new location.
	 */
	@Override
	public void onTeleported()
	{
		super.onTeleported();
		
		if (hasMinions())
		{
			getMinionList().onMasterTeleported();
		}
	}
	
	/**
	 * Removes this monster from the game world.<br>
	 * This method notifies minions and masters before deletion.
	 * @return {@code true} if the monster was successfully deleted, {@code false} otherwise.
	 */
	@Override
	public boolean deleteMe()
	{
		if (hasMinions())
		{
			getMinionList().onMasterDie(true);
		}
		
		if (_master != null)
		{
			_master.getMinionList().onMinionDie(this, 0);
		}
		
		return super.deleteMe();
	}
	
	/**
	 * Retrieves the leader of this monster.<br>
	 * This method returns the {@code Monster} object assigned as the master.
	 * @return the {@code Monster} leader or {@code null} if no leader exists.
	 */
	@Override
	public Monster getLeader()
	{
		return _master;
	}
	
	/**
	 * Sets the leader for this {@code Monster}.<br>
	 * The {@code _master} field is updated to the provided value.
	 * @param leader The {@code Monster} to be set as the leader.
	 */
	public void setLeader(Monster leader)
	{
		_master = leader;
	}
	
	/**
	 * Enables or disables the minion behavior for this monster.<br>
	 * Setting {@code true} allows the monster to spawn minions.<br>
	 * Setting {@code false} prevents the monster from spawning minions.
	 * @param value The boolean state to set for minion activation.
	 */
	public void enableMinions(boolean value)
	{
		_enableMinions = value;
	}
	
	/**
	 * Checks if the monster has any minions.<br>
	 * It returns {@code true} if the {@code _minionList} is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if there are minions, {@code false} otherwise.
	 */
	public boolean hasMinions()
	{
		return _minionList != null;
	}
	
	/**
	 * Retrieves the list of minions associated with this monster.<br>
	 * If the list does not exist, it will be created automatically.
	 * @return the {@code MinionList} for this monster.
	 */
	public MinionList getMinionList()
	{
		if (_minionList == null)
		{
			synchronized (this)
			{
				if (_minionList == null)
				{
					_minionList = new MinionList(this);
				}
			}
		}
		
		return _minionList;
	}
	
	/**
	 * Checks if the current entity is a monster.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the entity is a monster.
	 */
	@Override
	public boolean isMonster()
	{
		return true;
	}
	
	/**
	 * Casts the current object as a {@code Monster}.<br>
	 * This method returns the current instance.
	 * @return the current {@code Monster} instance.
	 */
	@Override
	public Monster asMonster()
	{
		return this;
	}
	
	/**
	 * Checks if the monster is a walker.<br>
	 * If this monster has a master, it returns the master's walker status.<br>
	 * Otherwise, it returns the default walker status from the parent class.
	 * @return {@code true} if the entity is a walker, {@code false} otherwise.
	 */
	@Override
	public boolean isWalker()
	{
		return ((_master == null) ? super.isWalker() : _master.isWalker());
	}
	
	/**
	 * Checks if the monster can receive a raid curse.<br>
	 * If this is a raid minion, it delegates the check to its master.<br>
	 * Otherwise, it uses the default behavior from the parent class.
	 * @return {@code true} if the curse can be applied, {@code false} otherwise.
	 */
	@Override
	public boolean giveRaidCurse()
	{
		return (isRaidMinion() && (_master != null)) ? _master.giveRaidCurse() : super.giveRaidCurse();
	}
	
	/**
	 * Executes a skill cast for the monster.<br>
	 * This method prevents monsters from casting positive effects on players.<br>
	 * It calls the base class implementation if the conditions are met.
	 * @param skill The {@code Skill} to be cast.
	 * @param item The {@code Item} used in the cast.
	 * @param ctrlPressed The state of the {@code true} or {@code false} control key.
	 * @param shiftPressed The state of the {@code true} or {@code false} shift key.
	 */
	@Override
	public synchronized void doCast(Skill skill, Item item, boolean ctrlPressed, boolean shiftPressed)
	{
		// Might need some exceptions here, but it will prevent the monster buffing player bug.
		if (!skill.hasNegativeEffect() && (getTarget() != null) && getTarget().isPlayer())
		{
			abortAllSkillCasters();
			return;
		}
		
		super.doCast(skill, item, ctrlPressed, shiftPressed);
	}
}
