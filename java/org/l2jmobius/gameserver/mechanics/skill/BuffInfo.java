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
package org.l2jmobius.gameserver.mechanics.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectTaskInfo;
import org.l2jmobius.gameserver.mechanics.effects.EffectTickTask;
import org.l2jmobius.gameserver.mechanics.options.Options;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * This class serves as a complex DTO that holds all information for a specific buff, debuff, or dance/song.<br>
 * It contains the set of effects issued by a skill.
 * @author Zoey76, Mobius
 */
public class BuffInfo
{
	// Data
	/** Data. */
	private final int _effectorObjectId;
	private final Creature _effector;
	private final Creature _effected;
	private final Skill _skill;
	/** The effects. */
	private final List<AbstractEffect> _effects = new ArrayList<>(1);
	
	// Tasks
	/** Effect tasks for ticks. */
	private Map<AbstractEffect, EffectTaskInfo> _tasks;
	
	// Time and ticks
	/** Abnormal time. */
	private int _abnormalTime;
	/** The game ticks at the start of this effect. */
	private int _periodStartTicks;
	
	// Misc
	/** If {@code true} then this effect has been cancelled. */
	private volatile SkillFinishType _finishType = SkillFinishType.NORMAL;
	/** If {@code true} then this effect is in use (or has been stop because an Herb took place). */
	private volatile boolean _isInUse = true;
	private final boolean _hideStartMessage;
	private final Item _item;
	private final Options _option;
	
	/**
	 * Creates a new {@code BuffInfo} object to store skill effect data.<br>
	 * This constructor initializes all necessary fields for a buff or debuff.
	 * @param effector The {@code Creature} that casts the skill.
	 * @param effected The {@code Creature} that receives the effect.
	 * @param skill The {@code Skill} being executed.
	 * @param hideStartMessage Set to {@code true} to suppress the initial message.
	 * @param item The {@code Item} used for the skill, if any.
	 * @param option The {@code Options} associated with this effect.
	 */
	public BuffInfo(Creature effector, Creature effected, Skill skill, boolean hideStartMessage, Item item, Options option)
	{
		_effectorObjectId = (effector != null) ? effector.getObjectId() : 0;
		_effector = effector;
		_effected = effected;
		_skill = skill;
		_abnormalTime = Formulas.calcEffectAbnormalTime(effector, effected, skill);
		_periodStartTicks = GameTimeTaskManager.getInstance().getGameTicks();
		_hideStartMessage = hideStartMessage;
		_item = item;
		_option = option;
	}
	
	/**
	 * Retrieves the list of effects associated with this buff.
	 * @return a {@code List} containing all {@code AbstractEffect} objects.
	 */
	public List<AbstractEffect> getEffects()
	{
		return _effects;
	}
	
	/**
	 * Adds a new effect to the list of effects.<br>
	 * This method stores the provided {@code AbstractEffect} in the internal collection.
	 * @param effect The {@code AbstractEffect} to be added.
	 */
	public void addEffect(AbstractEffect effect)
	{
		_effects.add(effect);
	}
	
	/**
	 * Adds a task to the internal collection.<br>
	 * This method maps an {@code AbstractEffect} to its corresponding {@code EffectTaskInfo}.<br>
	 * It ensures that the {@code _tasks} map is initialized before adding the entry.
	 * @param effect The {@code AbstractEffect} to be added.
	 * @param effectTaskInfo The {@code EffectTaskInfo} associated with the effect.
	 */
	private void addTask(AbstractEffect effect, EffectTaskInfo effectTaskInfo)
	{
		if (_tasks == null)
		{
			synchronized (this)
			{
				if (_tasks == null)
				{
					_tasks = new ConcurrentHashMap<>();
				}
			}
		}
		
		_tasks.put(effect, effectTaskInfo);
	}
	
	/**
	 * Retrieves the {@code EffectTaskInfo} associated with a specific effect.<br>
	 * It looks up the task in the internal map of tasks.<br>
	 * Returns {@code null} if no task exists or if the map is null.
	 * @param effect The {@code AbstractEffect} to look up.
	 * @return The corresponding {@code EffectTaskInfo} or {@code null}.
	 */
	private EffectTaskInfo getEffectTask(AbstractEffect effect)
	{
		return (_tasks == null) ? null : _tasks.get(effect);
	}
	
	/**
	 * Retrieves the {@code Skill} associated with this buff.
	 * @return the {@code Skill} object.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Retrieves the current abnormal time value.<br>
	 * This value represents the duration of an abnormal state.
	 * @return The current {@code int} value for abnormal time.
	 */
	public int getAbnormalTime()
	{
		return _abnormalTime;
	}
	
	/**
	 * Sets the duration of the abnormal state.<br>
	 * This updates the {@code _abnormalTime} field for this buff.
	 * @param abnormalTime The new time value to set.
	 */
	public void setAbnormalTime(int abnormalTime)
	{
		_abnormalTime = abnormalTime;
	}
	
	/**
	 * Retrieves the starting tick of the current period.<br>
	 * This value represents when the periodic effect began.
	 * @return the number of ticks since the start of the period.
	 */
	public int getPeriodStartTicks()
	{
		return _periodStartTicks;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this buff.
	 * @return the {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the {@code Options} associated with this buff.
	 * @return the {@code Options} object.
	 */
	public Options getOption()
	{
		return _option;
	}
	
	/**
	 * Calculates the remaining time for the effect.<br>
	 * This method subtracts the elapsed seconds from the total abnormal time.
	 * @return The remaining time in seconds as an {@code int}.
	 */
	public int getTime()
	{
		return _abnormalTime - ((GameTimeTaskManager.getInstance().getGameTicks() - _periodStartTicks) / GameTimeTaskManager.TICKS_PER_SECOND);
	}
	
	/**
	 * Checks if the buff has been marked for removal.<br>
	 * This method returns {@code true} if the finish type is set to {@code SkillFinishType.REMOVED}.
	 * @return {@code true} if the buff is removed, {@code false} otherwise.
	 */
	public boolean isRemoved()
	{
		return _finishType == SkillFinishType.REMOVED;
	}
	
	/**
	 * Sets the completion type for this buff.<br>
	 * This determines how the effects are handled when the skill finishes.
	 * @param type The {@code SkillFinishType} to apply.
	 */
	public void setFinishType(SkillFinishType type)
	{
		_finishType = type;
	}
	
	/**
	 * Checks if the buff is currently active.<br>
	 * Returns {@code true} if the buff is in use.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating the current status of the buff.
	 */
	public boolean isInUse()
	{
		return _isInUse;
	}
	
	/**
	 * Updates the usage status of this buff.<br>
	 * This method sets whether the effect is currently active or not.<br>
	 * It also handles sending system messages to the player if applicable.
	 * @param value The new status to set as {@code true} for in use or {@code false} for not in use.
	 */
	public void setInUse(boolean value)
	{
		_isInUse = value;
		
		// Send message that the effect is applied or removed.
		if ((_skill != null) && !_skill.isHidingMessages() && _effected.isPlayer())
		{
			if (value)
			{
				if (!_hideStartMessage && !_skill.isAura() && isDisplayedForEffected())
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_USED_S1);
					sm.addSkillName(_skill);
					_effected.sendPacket(sm);
				}
			}
			else
			{
				final SystemMessage sm = new SystemMessage(_skill.isToggle() ? SystemMessageId.S1_HAS_BEEN_ABORTED : SystemMessageId.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED);
				sm.addSkillName(_skill);
				_effected.sendPacket(sm);
			}
		}
	}
	
	/**
	 * Retrieves the unique identifier of the creature that cast the effect.<br>
	 * This ID corresponds to the {@code effector}.
	 * @return The {@code int} object ID of the effector.
	 */
	public int getEffectorObjectId()
	{
		return _effectorObjectId;
	}
	
	/**
	 * Retrieves the creature that cast the effect.<br>
	 * This returns the {@code Creature} responsible for the buff or debuff.
	 * @return the {@code Creature} acting as the effector.
	 */
	public Creature getEffector()
	{
		return _effector;
	}
	
	/**
	 * Retrieves the creature that is affected by this buff.
	 * @return the {@code Creature} instance receiving the effect.
	 */
	public Creature getEffected()
	{
		return _effected;
	}
	
	/**
	 * Stops all effects associated with this {@code BuffInfo}.<br>
	 * It updates the finish type and removes the buff from the target.<br>
	 * This method also triggers the completion of all active effects.
	 * @param type The {@code SkillFinishType} used to determine how the effects end.
	 */
	public void stopAllEffects(SkillFinishType type)
	{
		setFinishType(type);
		
		// Remove this buff info from BuffFinishTask.
		_effected.removeBuffInfoTime(this);
		finishEffects();
	}
	
	/**
	 * Initializes all effects associated with this buff.<br>
	 * Sends a system message to the player if the effect is visible.<br>
	 * Schedules periodic tasks for continuous effects.<br>
	 * Handles initial logic such as starting effects and managing abnormal time.
	 */
	public void initializeEffects()
	{
		if ((_effected == null) || (_skill == null))
		{
			return;
		}
		
		// When effects are initialized, they have successfully landed.
		if (!_hideStartMessage && _effected.isPlayer() && (_effected.asPlayer().hasEnteredWorld() || PlayerConfig.SHOW_EFFECT_MESSAGES_ON_LOGIN) && !_skill.isHidingMessages() && !_skill.isAura() && isDisplayedForEffected())
		{
			final SystemMessage sm = new SystemMessage(_skill.isToggle() ? SystemMessageId.YOU_HAVE_USED_S1 : SystemMessageId.YOU_FEEL_THE_S1_EFFECT);
			sm.addSkillName(_skill);
			_effected.sendPacket(sm);
		}
		
		// Creates a task that will stop all the effects.
		if (_abnormalTime > 0)
		{
			_effected.addBuffInfoTime(this);
		}
		
		for (AbstractEffect effect : _effects)
		{
			final boolean dead = _effected.isDead();
			if (effect.isInstant() || (dead && !_skill.isPassive() && !_skill.isStayAfterDeath()))
			{
				continue;
			}
			
			// Call on start.
			effect.onStart(_effector, _effected, _skill, _item);
			
			// Do not add continuous effect if target just died from the initial effect, otherwise they'll be ticked forever.
			if (dead && !_skill.isPassive())
			{
				continue;
			}
			
			// If it's a continuous effect, if has ticks schedule a task with period, otherwise schedule a simple task to end it.
			final int ticks = effect.getTicks();
			if (ticks > 0)
			{
				// The task for the effect ticks.
				final EffectTickTask effectTask = new EffectTickTask(this, effect);
				final ScheduledFuture<?> scheduledFuture = ThreadPool.scheduleAtFixedRate(effectTask, ticks * PlayerConfig.EFFECT_TICK_RATIO, ticks * PlayerConfig.EFFECT_TICK_RATIO);
				
				// Adds the task for ticking.
				addTask(effect, new EffectTaskInfo(effectTask, scheduledFuture));
			}
		}
	}
	
	/**
	 * Handles the logic for a single effect tick.<br>
	 * This method checks if the effect is currently in use and triggers its action time.<br>
	 * If the action finishes and the skill is a toggle, it cancels any remaining scheduled tasks.
	 * @param effect The {@code AbstractEffect} instance to process.
	 */
	public void onTick(AbstractEffect effect)
	{
		boolean continueForever = false;
		
		// If the effect is in use, allow it to affect the effected.
		if (_isInUse)
		{
			// Callback for on action time event.
			continueForever = effect.onActionTime(_effector, _effected, _skill, _item);
		}
		
		if (!continueForever && _skill.isToggle())
		{
			final EffectTaskInfo task = getEffectTask(effect);
			if (task != null)
			{
				final ScheduledFuture<?> schedule = task.getScheduledFuture();
				if ((schedule != null) && !schedule.isCancelled() && !schedule.isDone())
				{
					schedule.cancel(false); // Stop further ticks without interrupting the thread, which may be this tick itself.
				}
				
				_effected.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, _skill); // Remove the buff from the effect list.
			}
		}
	}
	
	/**
	 * Finalizes all active effects associated with this buff.<br>
	 * This method cancels any pending scheduled tasks.<br>
	 * It also triggers the {@code onExit} logic for every effect.<br>
	 * Finally, it sends a system message to the player if required.
	 */
	public void finishEffects()
	{
		// Cancels the ticking task.
		if (_tasks != null)
		{
			for (EffectTaskInfo effectTask : _tasks.values())
			{
				final ScheduledFuture<?> schedule = effectTask.getScheduledFuture();
				if ((schedule != null) && !schedule.isCancelled() && !schedule.isDone())
				{
					schedule.cancel(false); // Stop further ticks without interrupting the thread, which may be this tick itself.
				}
			}
		}
		
		// Notify on exit.
		for (AbstractEffect effect : _effects)
		{
			// Instant effects shouldn't call onExit(..).
			// if (!effect.isInstant())
			// {
			effect.onExit(_effector, _effected, _skill);
			// }
		}
		
		// Set the proper system message.
		if ((_skill != null) && !(_effected.isSummon() && !_effected.asSummon().getOwner().hasSummon()) && !_skill.isHidingMessages())
		{
			SystemMessageId smId = null;
			if ((_finishType == SkillFinishType.SILENT) || !isDisplayedForEffected())
			{
				// smId is null.
			}
			else if (_skill.isToggle())
			{
				smId = SystemMessageId.S1_HAS_BEEN_ABORTED;
			}
			else if (_finishType == SkillFinishType.REMOVED)
			{
				smId = SystemMessageId.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED;
			}
			else if (!_skill.isPassive())
			{
				smId = SystemMessageId.S1_HAS_WORN_OFF;
			}
			
			if (smId != null)
			{
				final Player player = _effected.asPlayer();
				if ((player != null) && player.isOnline())
				{
					final SystemMessage sm = new SystemMessage(smId);
					sm.addSkillName(_skill);
					_effected.sendPacket(sm);
				}
			}
		}
	}
	
	/**
	 * Resets the abnormal time for this buff.<br>
	 * This method updates the {@code _abnormalTime} value if it is greater than {@code 0}.<br>
	 * It also refreshes the start ticks and updates the buff information on the affected creature.
	 * @param abnormalTime The new value to set for the abnormal time.
	 */
	public void resetAbnormalTime(int abnormalTime)
	{
		if (_abnormalTime > 0)
		{
			_periodStartTicks = GameTimeTaskManager.getInstance().getGameTicks();
			_abnormalTime = abnormalTime;
			_effected.removeBuffInfoTime(this);
			_effected.addBuffInfoTime(this);
		}
	}
	
	/**
	 * Checks if the skill associated with this buff info matches a specific abnormal type.<br>
	 * It compares the {@code AbnormalType} of the skill to the provided parameter.
	 * @param type The {@code AbnormalType} to check against.
	 * @return {@code true} if the types match, otherwise {@code false}.
	 */
	public boolean isAbnormalType(AbnormalType type)
	{
		return _skill.getAbnormalType() == type;
	}
	
	/**
	 * Checks if the effect should be displayed to the affected target.<br>
	 * This logic determines visibility based on skill properties and targets.
	 * @return {@code true} if the effect is visible, {@code false} otherwise.
	 */
	public boolean isDisplayedForEffected()
	{
		return !_skill.isSelfContinuous() || (_effected == _effector) || !_skill.hasEffects(EffectScope.SELF);
	}
}
