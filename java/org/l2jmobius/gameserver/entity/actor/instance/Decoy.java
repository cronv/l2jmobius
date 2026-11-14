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

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExCharInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;

/**
 * Represents a decoy entity used to distract other actors.<br>
 * This {@code Decoy} class handles the behavior of an NPC that acts as a target for enemies.<br>
 * It inherits from {@code Creature} and manages its own lifecycle within the game world.
 */
public class Decoy extends Creature
{
	private final Player _owner;
	private Future<?> _decoyLifeTask;
	private Future<?> _hateSpam;
	private ScheduledFuture<?> _skillTask;
	
	/**
	 * Creates a new {@code Decoy} instance.<br>
	 * This constructor initializes the decoy with default aggressive behavior.
	 * @param template The {@code NpcTemplate} used to define the decoy's appearance and stats.
	 * @param owner The {@code Player} who summoned this decoy.
	 * @param totalLifeTime The duration in milliseconds before the decoy expires.
	 */
	public Decoy(NpcTemplate template, Player owner, int totalLifeTime)
	{
		this(template, owner, totalLifeTime, true);
	}
	
	/**
	 * Creates a new {@code Decoy} entity based on the provided template.<br>
	 * This constructor initializes the decoy's owner and sets its initial position.<br>
	 * It schedules an automatic removal task after the specified lifetime.<br>
	 * If aggressive is {@code true}, it also starts a periodic hate spam task.
	 * @param template The {@code NpcTemplate} used to define the decoy's properties.
	 * @param owner The {@code Player} who summoned this decoy.
	 * @param totalLifeTime The duration in milliseconds before the decoy is removed.
	 * @param aggressive Determines if the decoy should actively attack nearby enemies.
	 */
	public Decoy(NpcTemplate template, Player owner, int totalLifeTime, boolean aggressive)
	{
		super(template);
		setInstanceType(InstanceType.Decoy);
		
		_owner = owner;
		setXYZInvisible(owner.getX(), owner.getY(), owner.getZ());
		setInvul(false);
		
		_decoyLifeTask = ThreadPool.schedule(this::unSummon, totalLifeTime);
		
		if (aggressive)
		{
			final int hateSpamSkillId = 5272;
			final int skilllevel = Math.min(getTemplate().getDisplayId() - 13070, SkillData.getInstance().getMaxLevel(hateSpamSkillId));
			_hateSpam = ThreadPool.scheduleAtFixedRate(new HateSpam(this, SkillData.getInstance().getSkill(hateSpamSkillId, skilllevel)), 2000, 5000);
		}
		
		final SkillHolder skill = template.getParameters().getSkillHolder("decoy_skill");
		if (skill != null)
		{
			// Trigger cast instantly (?)...
			ThreadPool.schedule(() ->
			{
				final Skill decoySkill = skill.getSkill();
				doCast(decoySkill); // (?)
				
				final long castTime = (long) (template.getParameters().getFloat("cast_time", 5) * 1000) - 100;
				final long skillDelay = (long) (template.getParameters().getFloat("skill_delay", 2) * 1000);
				_skillTask = ThreadPool.scheduleAtFixedRate(() ->
				{
					if ((isDead() || !isSpawned()) && (_skillTask != null))
					{
						_skillTask.cancel(false);
						_skillTask = null;
						return;
					}
					
					doCast(decoySkill);
				}, castTime, skillDelay);
			}, 100); // ...presumably after spawnMe is called by SummonNpc effect.
		}
	}
	
	/**
	 * Handles the death logic for this decoy.<br>
	 * It cancels active tasks and starts the decay process.
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
		
		if (_hateSpam != null)
		{
			_hateSpam.cancel(true);
			_hateSpam = null;
		}
		
		unSummon();
		DecayTaskManager.getInstance().add(this);
		return true;
	}
	
	private static class HateSpam implements Runnable
	{
		private final Decoy _player;
		private final Skill _skill;
		
		HateSpam(Decoy player, Skill hate)
		{
			_player = player;
			_skill = hate;
		}
		
		@Override
		public void run()
		{
			try
			{
				_player.setTarget(_player);
				_player.doCast(_skill);
			}
			catch (Throwable e)
			{
				LOGGER.log(Level.SEVERE, "Decoy Error: ", e);
			}
		}
	}
	
	/**
	 * Removes the decoy from the game world.<br>
	 * This method cancels all active tasks associated with the decoy.<br>
	 * It also removes the entity from its current zone and starts the decay process.
	 */
	public synchronized void unSummon()
	{
		if (_skillTask != null)
		{
			_skillTask.cancel(false);
			_skillTask = null;
		}
		
		if (_hateSpam != null)
		{
			_hateSpam.cancel(true);
			_hateSpam = null;
		}
		
		if (isSpawned() && !isDead())
		{
			ZoneManager.getInstance().getRegion(this).removeFromZones(this);
			decayMe();
		}
		
		if (_decoyLifeTask != null)
		{
			_decoyLifeTask.cancel(false);
			_decoyLifeTask = null;
		}
	}
	
	/**
	 * This method is called when the {@code Decoy} instance is first created.<br>
	 * It initializes the entity and sends a {@code CharInfo} packet to the client.
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		sendPacket(new ExCharInfo(this, false));
	}
	
	/**
	 * Updates the visual state of abnormal effects for nearby players.<br>
	 * This method checks if a {@code Player} can see this entity.<br>
	 * It sends a {@code CharInfo} packet to update visibility status.
	 */
	@Override
	public void updateAbnormalVisualEffects()
	{
		World.forEachVisibleObject(this, Player.class, player ->
		{
			if (isVisibleFor(player))
			{
				player.sendPacket(new ExCharInfo(this, isInvisible() && player.isGM()));
			}
		});
	}
	
	/**
	 * Stops the decay process for this {@code Decoy}.<br>
	 * This method cancels the task managed by {@code DecayTaskManager}.
	 */
	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancel(this);
	}
	
	/**
	 * This method is called when the decoy starts to decay.<br>
	 * It removes the decoy from the game world by calling {@code deleteMe}.
	 */
	@Override
	public void onDecay()
	{
		deleteMe(_owner);
	}
	
	/**
	 * Checks if the owner of this decoy can be auto-attacked by a specific creature.<br>
	 * This method delegates the check to the {@code Player} who owns this decoy.
	 * @param attacker The {@code Creature} attempting to perform an auto-attack.
	 * @return {@code true} if the owner is attackable, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return _owner.isAutoAttackable(attacker);
	}
	
	/**
	 * Retrieves the current active weapon instance for this decoy.<br>
	 * This method currently returns {@code null}.
	 * @return the active {@code Item} instance or {@code null} if none exists.
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the weapon item currently held by this decoy.<br>
	 * This method returns {@code null} if no weapon is equipped.
	 * @return the {@code Weapon} object or {@code null}.
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon instance for this decoy.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Item} representing the secondary weapon or {@code null}
	 */
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon item for this decoy.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Weapon} object representing the secondary weapon, or {@code null}.
	 */
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	/**
	 * Retrieves the unique identifier for this decoy.<br>
	 * This value is obtained from the associated {@code NpcTemplate}.
	 * @return the {@code int} ID of the template.
	 */
	@Override
	public int getId()
	{
		return getTemplate().getId();
	}
	
	/**
	 * Retrieves the level of this decoy.<br>
	 * This value is obtained from the associated {@code NpcTemplate}.
	 * @return The level as an {@code int}.
	 */
	@Override
	public int getLevel()
	{
		return getTemplate().getLevel();
	}
	
	/**
	 * Removes this decoy from the game world.<br>
	 * This method triggers the decay process for the entity.
	 * @param owner The {@code Player} who owns this decoy.
	 */
	public void deleteMe(Player owner)
	{
		decayMe();
	}
	
	/**
	 * Retrieves the {@code Player} who owns this decoy.
	 * @return the {@code Player} owner of this decoy.
	 */
	public Player getOwner()
	{
		return _owner;
	}
	
	/**
	 * Retrieves the {@code Player} who owns this decoy.
	 * @return the {@code Player} owner of this decoy.
	 */
	@Override
	public Player asPlayer()
	{
		return _owner;
	}
	
	/**
	 * Retrieves the {@code NpcTemplate} associated with this decoy.<br>
	 * This method calls the parent class to get the base template data.
	 * @return the {@code NpcTemplate} for this entity.
	 */
	@Override
	public NpcTemplate getTemplate()
	{
		return (NpcTemplate) super.getTemplate();
	}
	
	/**
	 * Sends information about this decoy to a specific {@code Player}.<br>
	 * This method updates the visibility status of the decoy for the recipient.
	 * @param player The {@code Player} who will receive the update.
	 */
	@Override
	public void sendInfo(Player player)
	{
		player.sendPacket(new ExCharInfo(this, isInvisible() && player.isGM()));
	}
	
	/**
	 * Sends a {@code ServerPacket} to the owner of this decoy.<br>
	 * This method checks if the {@code _owner} is not {@code null} before sending.
	 * @param packet The {@code ServerPacket} to be sent.
	 */
	@Override
	public void sendPacket(ServerPacket packet)
	{
		if (_owner != null)
		{
			_owner.sendPacket(packet);
		}
	}
	
	/**
	 * Sends a system message to the owner of this decoy.<br>
	 * This method checks if the {@code _owner} is not {@code null} before sending.
	 * @param id The unique identifier for the system message to be sent.
	 */
	@Override
	public void sendPacket(SystemMessageId id)
	{
		if (_owner != null)
		{
			_owner.sendPacket(id);
		}
	}
}
