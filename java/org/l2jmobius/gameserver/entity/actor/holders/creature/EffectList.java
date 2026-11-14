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
package org.l2jmobius.gameserver.entity.actor.holders.creature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameManager;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillBuffType;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.serverpackets.AbnormalStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExAbnormalStatusUpdateFromTarget;
import org.l2jmobius.gameserver.network.serverpackets.PartySpelled;
import org.l2jmobius.gameserver.network.serverpackets.ShortBuffStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadSpelledInfo;

/**
 * Holds all the buff information affecting a {@code Creature}.<br>
 * Manages logic for adding, removing, replacing, or deactivating buffs.<br>
 * Uses maps with skill IDs as keys to optimize lookups and avoid unnecessary iterations.
 * @author Zoey76, Mobius
 */
public class EffectList
{
	private static final Logger LOGGER = Logger.getLogger(EffectList.class.getName());
	/** Queue containing all effects from buffs for this effect list. */
	private final Queue<BuffInfo> _actives = new ConcurrentLinkedQueue<>();
	/** List containing all passives for this effect list. They bypass most of the actions and they are not included in most operations. */
	private final Set<BuffInfo> _passives = ConcurrentHashMap.newKeySet();
	/** List containing all options for this effect list. They bypass most of the actions and they are not included in most operations. */
	private final Set<BuffInfo> _options = ConcurrentHashMap.newKeySet();
	/** Map containing the all stacked effect in progress for each {@code AbnormalType}. */
	private Set<AbnormalType> _stackedEffects = EnumSet.noneOf(AbnormalType.class);
	/** Set containing all {@code AbnormalType}s that shouldn't be added to this creature effect list. */
	private final Set<AbnormalType> _blockedAbnormalTypes = EnumSet.noneOf(AbnormalType.class);
	/** Set containing all abnormal visual effects this creature currently displays. */
	private Set<AbnormalVisualEffect> _abnormalVisualEffects = EnumSet.noneOf(AbnormalVisualEffect.class);
	/** Short buff skill ID. */
	private BuffInfo _shortBuff = null;
	/** Count of specific types of buffs. */
	private final AtomicInteger _buffCount = new AtomicInteger();
	private final AtomicInteger _triggerBuffCount = new AtomicInteger();
	private final AtomicInteger _danceCount = new AtomicInteger();
	private final AtomicInteger _toggleCount = new AtomicInteger();
	private final AtomicInteger _debuffCount = new AtomicInteger();
	/** If {@code true} this effect list has buffs removed on any action. */
	private final AtomicInteger _hasBuffsRemovedOnAnyAction = new AtomicInteger();
	/** If {@code true} this effect list has buffs removed on damage. */
	private final AtomicInteger _hasBuffsRemovedOnDamage = new AtomicInteger();
	/** Effect flags. */
	private long _effectFlags;
	/** The owner of this effect list. */
	private final Creature _owner;
	/** Hidden buffs count, prevents iterations. */
	private final AtomicInteger _hiddenBuffs = new AtomicInteger();
	/** Delay task **/
	private ScheduledFuture<?> _updateEffectIconTask;
	private final AtomicBoolean _updateAbnormalStatus = new AtomicBoolean();
	
	/**
	 * Creates a new {@code EffectList} for a specific creature.<br>
	 * This constructor initializes the list with the provided {@code Creature} as its owner.
	 * @param owner The {@code Creature} that will own this effect list.
	 */
	public EffectList(Creature owner)
	{
		_owner = owner;
	}
	
	/**
	 * Retrieves the set of passive effects for this entity.<br>
	 * The returned set is unmodifiable to prevent direct changes.
	 * @return a {@code Set} of {@code BuffInfo} objects representing passive effects.
	 */
	public Set<BuffInfo> getPassives()
	{
		return Collections.unmodifiableSet(_passives);
	}
	
	/**
	 * Retrieves the set of active options for this creature.<br>
	 * The returned set is unmodifiable to prevent direct changes.
	 * @return a {@code Set} of {@code BuffInfo} objects representing the current options.
	 */
	public Set<BuffInfo> getOptions()
	{
		return Collections.unmodifiableSet(_options);
	}
	
	/**
	 * Retrieves the collection of active effects.<br>
	 * This method returns an unmodifiable view of the internal effect list.
	 * @return a {@code Collection} of {@code BuffInfo} objects.
	 */
	public Collection<BuffInfo> getEffects()
	{
		return Collections.unmodifiableCollection(_actives);
	}
	
	/**
	 * Retrieves a list of all active buffs for the creature.<br>
	 * This method filters the active effects to include only those with a buff type.
	 * @return A {@code List} containing all {@code BuffInfo} objects that are classified as buffs.
	 */
	public List<BuffInfo> getBuffs()
	{
		final List<BuffInfo> result = new ArrayList<>();
		for (BuffInfo info : _actives)
		{
			if (info.getSkill().getBuffType().isBuff())
			{
				result.add(info);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of all active dance effects.<br>
	 * This method filters the active buffs to find those with a dance type.
	 * @return A {@code List} of {@code BuffInfo} objects representing dances.
	 */
	public List<BuffInfo> getDances()
	{
		final List<BuffInfo> result = new ArrayList<>();
		for (BuffInfo info : _actives)
		{
			if (info.getSkill().getBuffType().isDance())
			{
				result.add(info);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of all active debuffs.<br>
	 * This method filters the active effects to find those with a debuff type.
	 * @return A {@code List} containing all {@code BuffInfo} objects that are classified as debuffs.
	 */
	public List<BuffInfo> getDebuffs()
	{
		final List<BuffInfo> result = new ArrayList<>();
		for (BuffInfo info : _actives)
		{
			if (info.getSkill().getBuffType().isDebuff())
			{
				result.add(info);
			}
		}
		
		return result;
	}
	
	/**
	 * Checks if the creature is currently affected by a specific skill.<br>
	 * This method searches through both active and passive buffs.
	 * @param skillId The unique identifier of the skill to check.
	 * @return {@code true} if the skill is found, otherwise {@code false}.
	 */
	public boolean isAffectedBySkill(int skillId)
	{
		for (BuffInfo info : _actives)
		{
			if (info.getSkill().getId() == skillId)
			{
				return true;
			}
		}
		
		for (BuffInfo info : _passives)
		{
			if (info.getSkill().getId() == skillId)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves the {@code BuffInfo} associated with a specific skill ID.<br>
	 * This method searches through both active and passive buffs.<br>
	 * It returns the first matching buff found or {@code null} if no match exists.
	 * @param skillId The unique identifier of the skill to search for.
	 * @return The {@code BuffInfo} object, or {@code null} if not found.
	 */
	public BuffInfo getBuffInfoBySkillId(int skillId)
	{
		for (BuffInfo info : _actives)
		{
			if (info.getSkill().getId() == skillId)
			{
				return info;
			}
		}
		
		for (BuffInfo info : _passives)
		{
			if (info.getSkill().getId() == skillId)
			{
				return info;
			}
		}
		
		return null;
	}
	
	/**
	 * Checks if the creature has a specific abnormal type.<br>
	 * This method looks for the {@code AbnormalType} in the current effects.
	 * @param type The {@code AbnormalType} to check for.
	 * @return {@code true} if the type is present, otherwise {@code false}.
	 */
	public boolean hasAbnormalType(AbnormalType type)
	{
		return _stackedEffects.contains(type);
	}
	
	/**
	 * Checks if any of the provided types exist in the stacked effects.<br>
	 * It iterates through all current abnormal types to find a match.
	 * @param types The collection of {@code AbnormalType} values to check.
	 * @return {@code true} if at least one matching type is found, otherwise {@code false}.
	 */
	public boolean hasAbnormalType(Collection<AbnormalType> types)
	{
		for (AbnormalType abnormalType : _stackedEffects)
		{
			if (types.contains(abnormalType))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the creature has a specific {@code AbnormalType} that matches a given condition.<br>
	 * This method first verifies if any active buff is of the requested type.<br>
	 * It then iterates through all active buffs to see if any satisfy both the type and the {@code filter}.
	 * @param type The {@code AbnormalType} to search for.
	 * @param filter A {@code Predicate} used to further filter the matching {@code BuffInfo} objects.
	 * @return {@code true} if a matching buff is found, otherwise {@code false}.
	 */
	public boolean hasAbnormalType(AbnormalType type, Predicate<BuffInfo> filter)
	{
		if (hasAbnormalType(type))
		{
			for (BuffInfo info : _actives)
			{
				if (info.isAbnormalType(type) && filter.test(info))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves the first active buff that matches a specific abnormal type.<br>
	 * This method checks if the creature is affected by the given {@code AbnormalType}.<br>
	 * It iterates through all active buffs and returns the first one it finds.<br>
	 * Returns {@code null} if no matching buff exists.
	 * @param type The {@code AbnormalType} to search for.
	 * @return The first matching {@code BuffInfo} or {@code null}.
	 */
	public BuffInfo getFirstBuffInfoByAbnormalType(AbnormalType type)
	{
		if (hasAbnormalType(type))
		{
			for (BuffInfo info : _actives)
			{
				if (info.isAbnormalType(type))
				{
					return info;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Adds specific types of abnormal effects to the blocked list.<br>
	 * These types will be ignored by the system during processing.
	 * @param blockedAbnormalTypes The set of {@code AbnormalType} values to block.
	 */
	public void addBlockedAbnormalTypes(Set<AbnormalType> blockedAbnormalTypes)
	{
		_blockedAbnormalTypes.addAll(blockedAbnormalTypes);
	}
	
	/**
	 * Removes specific abnormal types from the blocked list.<br>
	 * This method updates the internal set of restricted {@code AbnormalType} values.
	 * @param blockedBuffSlots The set of {@code AbnormalType} values to remove.
	 * @return {@code true} if at least one element was removed, {@code false} otherwise.
	 */
	public boolean removeBlockedAbnormalTypes(Set<AbnormalType> blockedBuffSlots)
	{
		return _blockedAbnormalTypes.removeAll(blockedBuffSlots);
	}
	
	/**
	 * Retrieves the set of blocked abnormal types.<br>
	 * This method returns an unmodifiable view of the internal collection.
	 * @return a {@code Set} containing all {@code AbnormalType} entries that are currently blocked.
	 */
	public Set<AbnormalType> getBlockedAbnormalTypes()
	{
		return Collections.unmodifiableSet(_blockedAbnormalTypes);
	}
	
	/**
	 * Updates the short buff status for a player.<br>
	 * This method sends a packet to the player to notify them of changes.<br>
	 * If the provided {@code BuffInfo} is {@code null}, it resets the short buff.<br>
	 * Otherwise, it sends the updated skill details and remaining time.
	 * @param info The {@code BuffInfo} object containing the new status data.
	 */
	public void shortBuffStatusUpdate(BuffInfo info)
	{
		if (_owner.isPlayer())
		{
			_shortBuff = info;
			if (info == null)
			{
				_owner.sendPacket(ShortBuffStatusUpdate.RESET_SHORT_BUFF);
			}
			else
			{
				_owner.sendPacket(new ShortBuffStatusUpdate(info.getSkill().getId(), info.getSkill().getLevel(), info.getSkill().getSubLevel(), info.getTime()));
			}
		}
	}
	
	/**
	 * Retrieves the current number of active buffs.<br>
	 * This calculation subtracts hidden buffs from the total buff count.<br>
	 * It returns 0 if there are no active buffs.
	 * @return The total number of visible active buffs as an {@code int}.
	 */
	public int getBuffCount()
	{
		return !_actives.isEmpty() ? (_buffCount.get() - _hiddenBuffs.get()) : 0;
	}
	
	/**
	 * Retrieves the current number of active dances.<br>
	 * This method returns the value stored in the {@code _danceCount} variable.
	 * @return The total count of dances as an {@code int}.
	 */
	public int getDanceCount()
	{
		return _danceCount.get();
	}
	
	/**
	 * Returns the total number of triggered buffs.<br>
	 * This count represents how many active effects have been triggered.
	 * @return The current count of triggered buffs as an {@code int}.
	 */
	public int getTriggeredBuffCount()
	{
		return _triggerBuffCount.get();
	}
	
	/**
	 * Retrieves the current number of active toggle effects.<br>
	 * This count is stored in an {@code AtomicInteger}.
	 * @return The total number of toggle effects currently active.
	 */
	public int getToggleCount()
	{
		return _toggleCount.get();
	}
	
	/**
	 * Retrieves the current number of active debuffs.<br>
	 * This method returns the count stored in the {@code _debuffCount} variable.
	 * @return The total number of debuffs currently affecting the creature.
	 */
	public int getDebuffCount()
	{
		return _debuffCount.get();
	}
	
	/**
	 * Retrieves the total number of hidden buffs currently active.<br>
	 * This count includes effects that are not visible to other players.
	 * @return The number of hidden buffs as an {@code int}.
	 */
	public int getHiddenBuffsCount()
	{
		return _hiddenBuffs.get();
	}
	
	/**
	 * Stops all active effects on the creature.<br>
	 * This method removes effects that are not marked as irreplaceable.
	 * @param broadcast Set to {@code true} to notify other players of these changes.
	 */
	public void stopAllEffects(boolean broadcast)
	{
		stopEffects(b -> !b.getSkill().isIrreplaceableBuff(), true, broadcast);
	}
	
	/**
	 * Stops all active effects on the creature.<br>
	 * It preserves effects that are configured to stay after death.<br>
	 * This method checks the {@code isStayAfterDeath} property of each skill.
	 */
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		stopEffects(info -> !info.getSkill().isStayAfterDeath(), true, true);
	}
	
	/**
	 * Stops all active effects for the creature without any exclusions.<br>
	 * This method removes all buffs from the active, passive, and option lists.<br>
	 * It optionally updates stats and broadcasts changes to other players.
	 * @param update Determines if the creature's stats and effect flags should be updated.
	 * @param broadcast Determines if the change should be sent to other clients.
	 */
	public void stopAllEffectsWithoutExclusions(boolean update, boolean broadcast)
	{
		for (BuffInfo info : _actives)
		{
			remove(info);
		}
		
		for (BuffInfo info : _passives)
		{
			remove(info);
		}
		
		for (BuffInfo info : _options)
		{
			remove(info);
		}
		
		// Update stats, effect flags and icons.
		if (update)
		{
			updateEffectList(broadcast);
		}
	}
	
	/**
	 * Stops all active toggle effects for the creature.<br>
	 * This method checks if any toggle buffs are currently active.<br>
	 * It removes them if they are not marked as irreplaceable.
	 */
	public void stopAllToggles()
	{
		if (_toggleCount.get() > 0)
		{
			stopEffects(b ->
			{
				final Skill bSkill = b.getSkill();
				return bSkill.isToggle() && !bSkill.isIrreplaceableBuff();
			}, true, true);
		}
	}
	
	/**
	 * Stops all active effects belonging to a specific toggle group.<br>
	 * This method checks if any toggles are currently active.<br>
	 * It then removes effects that match the provided {@code toggleGroup}.
	 * @param toggleGroup The unique identifier for the group of toggles to stop.
	 */
	public void stopAllTogglesOfGroup(int toggleGroup)
	{
		if (_toggleCount.get() > 0)
		{
			stopEffects(b ->
			{
				final Skill bSkill = b.getSkill();
				return bSkill.isToggle() && (bSkill.getToggleGroupId() == toggleGroup);
			}, true, true);
		}
	}
	
	/**
	 * Removes all passive effects from the creature.<br>
	 * If {@code update} is {@code true}, it refreshes the stats and icons.<br>
	 * If {@code broadcast} is {@code true}, these changes are sent to other players.
	 * @param update Whether to refresh the internal effect list and statistics.
	 * @param broadcast Whether to notify other clients of the change.
	 */
	public void stopAllPassives(boolean update, boolean broadcast)
	{
		if (!_passives.isEmpty())
		{
			_passives.forEach(this::remove);
			
			// Update stats, effect flags and icons.
			if (update)
			{
				updateEffectList(broadcast);
			}
		}
	}
	
	/**
	 * Removes all active options from the creature.<br>
	 * If {@code update} is {@code true}, it refreshes the stats and icons.
	 * @param update Determines if the effect list should be updated after removal.
	 * @param broadcast Determines if the update should be sent to other players.
	 */
	public void stopAllOptions(boolean update, boolean broadcast)
	{
		if (!_options.isEmpty())
		{
			_options.forEach(this::remove);
			
			// Update stats, effect flags and icons.
			if (update)
			{
				updateEffectList(broadcast);
			}
		}
	}
	
	/**
	 * Stops all active effects that match the specified {@code EffectFlag}.<br>
	 * This method checks if the current entity is affected by the flag.<br>
	 * If a match is found, it removes the corresponding buff and updates the effect list.
	 * @param effectFlag The {@code EffectFlag} used to identify which effects to remove.
	 */
	public void stopEffects(EffectFlag effectFlag)
	{
		if (isAffected(effectFlag) && !_actives.isEmpty())
		{
			boolean update = false;
			for (BuffInfo info : _actives)
			{
				for (AbstractEffect effect : info.getEffects())
				{
					if ((effect != null) && ((effect.getEffectFlags() & effectFlag.getMask()) != 0))
					{
						remove(info);
						update = true;
					}
				}
			}
			
			// Update stats, effect flags and icons.
			if (update)
			{
				updateEffectList(true);
			}
		}
	}
	
	/**
	 * Stops the effects of a specific skill for the creature.<br>
	 * This method finds the buff information using {@code skillId}.<br>
	 * If the effect exists, it is removed based on the provided {@code type}.
	 * @param type The category or type of the skill finish to process.
	 * @param skillId The unique identifier of the skill to stop.
	 */
	public void stopSkillEffects(SkillFinishType type, int skillId)
	{
		final BuffInfo info = getBuffInfoBySkillId(skillId);
		if (info != null)
		{
			remove(info, type, true, true);
		}
	}
	
	/**
	 * Stops the effects of a specific skill based on the provided type.<br>
	 * This method removes active instances of the skill from the creature.
	 * @param type The category or condition for stopping the effect.
	 * @param skill The {@code Skill} object containing the ID to be stopped.
	 */
	public void stopSkillEffects(SkillFinishType type, Skill skill)
	{
		stopSkillEffects(type, skill.getId());
	}
	
	/**
	 * Stops all active effects that match a specific {@code AbnormalType}.<br>
	 * This method checks if the creature is currently affected by the given type.<br>
	 * If it is, it removes those effects and returns {@code true}.<br>
	 * Otherwise, it returns {@code false}.
	 * @param type The {@code AbnormalType} to filter for when stopping effects.
	 * @return {@code true} if any matching effects were stopped, otherwise {@code false}.
	 */
	public boolean stopEffects(AbnormalType type)
	{
		if (hasAbnormalType(type))
		{
			stopEffects(i -> i.isAbnormalType(type), true, true);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Stops all active effects that match the specified abnormal types.<br>
	 * This method checks if any of the provided types are currently active.<br>
	 * If they are, it removes them from the creature.
	 * @param types The collection of {@code AbnormalType} to stop.
	 * @return {@code true} if any matching effects were stopped, otherwise {@code false}.
	 */
	public boolean stopEffects(Collection<AbnormalType> types)
	{
		if (hasAbnormalType(types))
		{
			stopEffects(i -> types.contains(i.getSkill().getAbnormalType()), true, true);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Removes active effects based on a specific filter.<br>
	 * This method checks all active buffs against the provided {@code Predicate}.<br>
	 * It optionally updates stats and broadcasts changes to other players.
	 * @param filter The condition used to determine which {@code BuffInfo} objects to remove.
	 * @param update Whether to refresh the entity's statistics and effect flags after removal.
	 * @param broadcast Whether to send a network packet to notify other players of the change.
	 */
	public void stopEffects(Predicate<BuffInfo> filter, boolean update, boolean broadcast)
	{
		if (!_actives.isEmpty())
		{
			for (BuffInfo info : _actives)
			{
				if (filter.test(info))
				{
					remove(info);
				}
			}
			
			// Update stats, effect flags and icons.
			if (update)
			{
				updateEffectList(broadcast);
			}
		}
	}
	
	/**
	 * Stops specific effects that are configured to be removed on any action.<br>
	 * This method checks if there are any buffs marked for removal and clears them.
	 */
	public void stopEffectsOnAction()
	{
		if (_hasBuffsRemovedOnAnyAction.get() > 0)
		{
			stopEffects(info -> info.getSkill().isRemovedOnAnyActionExceptMove(), true, true);
		}
	}
	
	/**
	 * Stops all effects that are configured to be removed when the creature takes damage.<br>
	 * This method checks if any buffs have the {@code isRemovedOnDamage} property set to {@code true}.<br>
	 * It then removes those specific effects from the creature.
	 */
	public void stopEffectsOnDamage()
	{
		if (_hasBuffsRemovedOnDamage.get() > 0)
		{
			stopEffects(info -> info.getSkill().isRemovedOnDamage(), true, true);
		}
	}
	
	/**
	 * Checks if the current number of active effects exceeds the allowed limits.<br>
	 * It evaluates specific types like triggers, dances, debuffs, and buffs.
	 * @param buffTypes The types of buffs to check for limit violations.
	 * @return {@code true} if any of the specified types exceed their maximum limit; {@code false} otherwise.
	 */
	private boolean isLimitExceeded(SkillBuffType... buffTypes)
	{
		for (SkillBuffType buffType : buffTypes)
		{
			switch (buffType)
			{
				case TRIGGER:
				{
					if (_triggerBuffCount.get() > PlayerConfig.TRIGGERED_BUFFS_MAX_AMOUNT)
					{
						return true;
					}
					break;
				}
				case DANCE:
				{
					if (_danceCount.get() > PlayerConfig.DANCES_MAX_AMOUNT)
					{
						return true;
					}
					break;
				}
				// case TOGGLE: Do toggles have limit?
				case DEBUFF:
				{
					if (_debuffCount.get() > 24)
					{
						return true;
					}
					break;
				}
				case BUFF:
				{
					if (getBuffCount() > _owner.getStat().getMaxBuffCount())
					{
						return true;
					}
					break;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Updates the internal counters for various buff types and properties.<br>
	 * This method increments or decrements counts based on the {@code increase} flag.<br>
	 * It handles hidden buffs, action-based removals, damage-based removals, and specific skill types.
	 * @param info The {@code BuffInfo} object to process.
	 * @param increase A boolean indicating whether to increment or decrement the counts.
	 * @return The new count of the specific buff type, or 0 if no specific type matches.
	 */
	private int increaseDecreaseCount(BuffInfo info, boolean increase)
	{
		// If it's a hidden buff, manage hidden buff count.
		if (!info.isInUse())
		{
			if (increase)
			{
				_hiddenBuffs.incrementAndGet();
			}
			else
			{
				_hiddenBuffs.decrementAndGet();
			}
		}
		
		// Update flag for skills being removed on action or damage.
		if (info.getSkill().isRemovedOnAnyActionExceptMove())
		{
			if (increase)
			{
				_hasBuffsRemovedOnAnyAction.incrementAndGet();
			}
			else
			{
				_hasBuffsRemovedOnAnyAction.decrementAndGet();
			}
		}
		
		if (info.getSkill().isRemovedOnDamage())
		{
			if (increase)
			{
				_hasBuffsRemovedOnDamage.incrementAndGet();
			}
			else
			{
				_hasBuffsRemovedOnDamage.decrementAndGet();
			}
		}
		
		// Increase specific buff count.
		switch (info.getSkill().getBuffType())
		{
			case TRIGGER:
			{
				return increase ? _triggerBuffCount.incrementAndGet() : _triggerBuffCount.decrementAndGet();
			}
			case DANCE:
			{
				return increase ? _danceCount.incrementAndGet() : _danceCount.decrementAndGet();
			}
			case TOGGLE:
			{
				return increase ? _toggleCount.incrementAndGet() : _toggleCount.decrementAndGet();
			}
			case DEBUFF:
			{
				return increase ? _debuffCount.incrementAndGet() : _debuffCount.decrementAndGet();
			}
			case BUFF:
			{
				return increase ? _buffCount.incrementAndGet() : _buffCount.decrementAndGet();
			}
		}
		
		return 0;
	}
	
	/**
	 * Removes a specific buff from the creature.<br>
	 * This method handles the removal logic for {@code BuffInfo}.
	 * @param info The {@code BuffInfo} object to be removed.
	 */
	private void remove(BuffInfo info)
	{
		remove(info, SkillFinishType.REMOVED, false, false);
	}
	
	/**
	 * Removes a specific buff from the owner.<br>
	 * This method handles options, passives, and active effects differently based on their type.<br>
	 * It can also trigger an update of the effect list if requested.
	 * @param info The {@code BuffInfo} object to be removed.
	 * @param type The {@code SkillFinishType} indicating how the buff ends.
	 * @param update Whether to refresh the effect list after removal.
	 * @param broadcast Whether to send the update to other players.
	 */
	public void remove(BuffInfo info, SkillFinishType type, boolean update, boolean broadcast)
	{
		if (info == null)
		{
			return;
		}
		
		if (info.getOption() != null)
		{
			// Remove separately if it is an option.
			removeOption(info, type);
		}
		else if (info.getSkill().isPassive())
		{
			// Remove Passive effect.
			removePassive(info, type);
		}
		else
		{
			// Remove active effect.
			removeActive(info, type);
			if (_owner.isNpc()) // Fix for all NPC debuff animations removed.
			{
				updateEffectList(broadcast);
			}
		}
		
		// Update stats, effect flags and icons.
		if (update)
		{
			updateEffectList(broadcast);
		}
	}
	
	/**
	 * Removes an active buff from the creature.<br>
	 * This method stops all effects associated with the buff.<br>
	 * It also updates the buff count and handles short buff status.
	 * @param info The {@code BuffInfo} object to be removed.
	 * @param type The {@code SkillFinishType} indicating how the skill ended.
	 */
	private void removeActive(BuffInfo info, SkillFinishType type)
	{
		if (!_actives.isEmpty())
		{
			// Removes the buff from the given effect list.
			// Already removed means already stopped, stopping again would strip a newer buff of the same skill.
			if (!_actives.remove(info))
			{
				return;
			}
			
			// Remove short buff.
			if (info == _shortBuff)
			{
				shortBuffStatusUpdate(null);
			}
			
			// Stop the buff effects.
			info.stopAllEffects(type);
			
			// Decrease specific buff count.
			increaseDecreaseCount(info, false);
			info.getSkill().applyEffectScope(EffectScope.END, info, true, false);
		}
	}
	
	/**
	 * Removes a specific passive effect from the creature.<br>
	 * This method also stops all effects associated with the provided {@code SkillFinishType}.
	 * @param info The {@code BuffInfo} object to be removed.
	 * @param type The {@code SkillFinishType} used to stop related effects.
	 */
	private void removePassive(BuffInfo info, SkillFinishType type)
	{
		if (!_passives.isEmpty())
		{
			_passives.remove(info);
			info.stopAllEffects(type);
		}
	}
	
	/**
	 * Removes a specific buff from the options list.<br>
	 * This method also stops all effects associated with the provided {@code SkillFinishType}.
	 * @param info The {@code BuffInfo} object to be removed.
	 * @param type The {@code SkillFinishType} used to stop the effects.
	 */
	private void removeOption(BuffInfo info, SkillFinishType type)
	{
		if (!_options.isEmpty())
		{
			_options.remove(info);
			info.stopAllEffects(type);
		}
	}
	
	/**
	 * Adds a new buff or effect to the creature.<br>
	 * This method checks if the target is alive before applying active skills.<br>
	 * It categorizes the {@code BuffInfo} as an option, passive, or active effect.<br>
	 * The internal effect list is updated after the addition.
	 * @param info The {@code BuffInfo} object to be added.
	 */
	public void add(BuffInfo info)
	{
		if (info == null)
		{
			return;
		}
		
		final Skill skill = info.getSkill();
		
		// Prevent adding and initializing buffs/effects on dead creatures.
		if (info.getEffected().isDead() && (skill != null) && !skill.isPassive() && !skill.isStayAfterDeath())
		{
			return;
		}
		
		if (skill == null)
		{
			// Only options are without skills.
			addOption(info);
		}
		else if (skill.isPassive())
		{
			// Passive effects are treated specially.
			addPassive(info);
		}
		else
		{
			// Add active effect
			addActive(info);
		}
		
		// Update stats, effect flags and icons.
		updateEffectList(true);
	}
	
	/**
	 * Adds an active buff to the creature.<br>
	 * This method validates if the buff can be applied based on death status, blocked types, and stacking rules.<br>
	 * It handles overriding existing effects of the same abnormal type or skill ID.<br>
	 * Finally, it updates the buff count and initializes the effect.
	 * @param info The {@code BuffInfo} object containing the details of the buff to add.
	 */
	private void addActive(BuffInfo info)
	{
		final Skill skill = info.getSkill();
		final Creature effected = info.getEffected();
		final Creature effector = info.getEffector();
		
		// Cannot add active buff to dead creature. Even in retail if you are dead with Lv. 3 Shillien's Breath, it will disappear instead of going 1 level down.
		if (effected.isDead() && !skill.isStayAfterDeath())
		{
			return;
		}
		
		final AbnormalType abnormalType = skill.getAbnormalType();
		if ((_blockedAbnormalTypes != null) && _blockedAbnormalTypes.contains(abnormalType))
		{
			return;
		}
		
		// Fix for stacking trigger skills.
		if (skill.isTriggeredSkill())
		{
			final BuffInfo triggerInfo = effected.getEffectList().getBuffInfoBySkillId(skill.getId());
			if ((triggerInfo != null) && (triggerInfo.getSkill().getLevel() >= skill.getLevel()))
			{
				return;
			}
		}
		
		if (effector != null)
		{
			// Check for debuffs against target.
			if ((effector != effected) && skill.hasNegativeEffect())
			{
				// Check if effected is debuff blocked.
				if ((effected.isDebuffBlocked() || (effector.isGM() && !effector.getAccessLevel().canGiveDamage())))
				{
					return;
				}
				
				if (effector.isPlayer() && effected.isPlayer() && effected.isAffected(EffectFlag.DUELIST_FURY) && !effector.isAffected(EffectFlag.DUELIST_FURY))
				{
					return;
				}
			}
			
			// Check if buff skills are blocked.
			if (effected.isBuffBlocked() && !skill.hasNegativeEffect())
			{
				return;
			}
		}
		
		// Servitor gets buffs only from its summoner. (Age of Magic 439)
		final boolean isDebuff = skill.isDebuff();
		final boolean isServitor = effected.isServitor();
		if (!isDebuff && isServitor && !info.getEffector().hasServitor(effected.getObjectId()))
		{
			return;
		}
		
		// Manage effect stacking.
		if (hasAbnormalType(abnormalType))
		{
			for (BuffInfo existingInfo : _actives)
			{
				final Skill existingSkill = existingInfo.getSkill();
				
				// Check if existing effect should be removed due to stack.
				// Effects with no abnormal don't stack if their ID is the same. Effects of the same abnormal type don't stack.
				if ((abnormalType.isNone() && (existingSkill.getId() == skill.getId())) || (!abnormalType.isNone() && (existingSkill.getAbnormalType() == abnormalType)))
				{
					// Check if there is subordination abnormal. Skills with subordination abnormal stack with each other, unless the caster is the same.
					if (!skill.getSubordinationAbnormalType().isNone() && (skill.getSubordinationAbnormalType() == existingSkill.getSubordinationAbnormalType()) //
						&& ((info.getEffectorObjectId() == 0) || (existingInfo.getEffectorObjectId() == 0) || (info.getEffectorObjectId() != existingInfo.getEffectorObjectId())))
					{
						continue;
					}
					
					// The effect we are adding overrides the existing effect. Delete or disable the existing effect.
					if (skill.getAbnormalLevel() >= existingSkill.getAbnormalLevel())
					{
						// If it is an herb, set as not in use the lesser buff, unless it is the same skill.
						if ((skill.isAbnormalInstant() || existingSkill.isIrreplaceableBuff()) && (skill.getId() != existingSkill.getId()))
						{
							existingInfo.setInUse(false);
							_hiddenBuffs.incrementAndGet();
						}
						else
						{
							// Remove effect that gets overridden.
							remove(existingInfo);
						}
					}
					else if (skill.isIrreplaceableBuff()) // The effect we try to add should be hidden.
					{
						info.setInUse(false);
					}
					else // The effect we try to add should be overridden.
					{
						return;
					}
				}
			}
		}
		
		// Increase buff count.
		increaseDecreaseCount(info, true);
		
		// Check if any effect limit is exceeded.
		final SkillBuffType[] values = SkillBuffType.values();
		if (isLimitExceeded(values))
		{
			// Check for each category.
			for (BuffInfo existingInfo : _actives)
			{
				if (existingInfo.isInUse() && !skill.is7Signs() && isLimitExceeded(existingInfo.getSkill().getBuffType()))
				{
					remove(existingInfo);
				}
				
				// Break further loops if there is no any other limit exceeding.
				if (!isLimitExceeded(values))
				{
					break;
				}
			}
		}
		
		// After removing old buff (same ID) or stacked buff (same abnormal type),
		// Add the buff to the end of the effect list.
		_actives.add(info);
		
		// Initialize effects.
		info.initializeEffects();
	}
	
	/**
	 * Adds a passive effect to the creature.<br>
	 * This method removes any existing passive with the same skill ID before adding the new one.<br>
	 * It also initializes the effects for the provided {@code BuffInfo}.
	 * @param info The {@code BuffInfo} object containing the passive data.
	 */
	private void addPassive(BuffInfo info)
	{
		final Skill skill = info.getSkill();
		
		// Passive effects don't need stack type!
		if (!skill.getAbnormalType().isNone())
		{
			LOGGER.warning("Passive " + skill + " with abnormal type: " + skill.getAbnormalType() + "!");
		}
		
		// Remove previous passives of this id.
		for (BuffInfo b : _passives)
		{
			if ((b != null) && (b.getSkill().getId() == skill.getId()))
			{
				b.setInUse(false);
				_passives.remove(b);
			}
		}
		
		_passives.add(info);
		
		// Initialize effects.
		info.initializeEffects();
	}
	
	/**
	 * Adds a new option to the current set of options.<br>
	 * This method removes any existing options with the same ID before adding the new one.<br>
	 * It also initializes the effects for the provided {@code BuffInfo}.
	 * @param info The {@code BuffInfo} object containing the option to be added.
	 */
	private void addOption(BuffInfo info)
	{
		if (info.getOption() != null)
		{
			// Remove previous options of this id.
			for (BuffInfo b : _options)
			{
				if ((b != null) && (b.getOption().getId() == info.getOption().getId()))
				{
					b.setInUse(false);
					_options.remove(b);
				}
			}
			
			_options.add(info);
			
			// Initialize effects.
			info.initializeEffects();
		}
	}
	
	/**
	 * Updates the visual effect icons for the owner of this object.<br>
	 * This method schedules a task to broadcast buff and status information.<br>
	 * It handles updates for players, summons, parties, and olympiad observers.
	 * @param partyOnly If {@code true}, only sends updates to members of the same party.
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		if (!partyOnly)
		{
			_updateAbnormalStatus.compareAndSet(false, true);
		}
		
		if (_updateEffectIconTask == null)
		{
			_updateEffectIconTask = ThreadPool.schedule(() ->
			{
				final Player player = _owner.asPlayer();
				if (player != null)
				{
					final Party party = player.getParty();
					final AbnormalStatusUpdate asu = (_owner.isPlayer() && _updateAbnormalStatus.get()) ? new AbnormalStatusUpdate() : null;
					final PartySpelled ps = ((party != null) || _owner.isSummon()) ? new PartySpelled(_owner) : null;
					final ExOlympiadSpelledInfo os = (player.isInOlympiadMode() && player.isOlympiadStart()) ? new ExOlympiadSpelledInfo(player) : null;
					if (!_actives.isEmpty())
					{
						for (BuffInfo info : _actives)
						{
							if ((info != null) && info.isInUse())
							{
								final boolean isHealingPotionSkill = info.getSkill().isHealingPotionSkill();
								if (isHealingPotionSkill)
								{
									shortBuffStatusUpdate(info);
								}
								else if (info.isDisplayedForEffected())
								{
									if (asu != null)
									{
										asu.addSkill(info);
									}
									
									final boolean isToggle = info.getSkill().isToggle();
									if ((ps != null) && !isToggle)
									{
										ps.addSkill(info);
									}
									if (os != null)
									{
										os.addSkill(info);
									}
								}
							}
						}
					}
					
					// Send icon update for player buff bar.
					if (asu != null)
					{
						_owner.sendPacket(asu);
					}
					
					// Player or summon is in party. Broadcast packet to everyone in the party.
					if (ps != null)
					{
						if (party != null)
						{
							party.broadcastPacket(ps);
						}
						else // Not in party, then it is a summon info for its owner.
						{
							player.sendPacket(ps);
						}
					}
					
					if (os != null)
					{
						final OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(player.getOlympiadGameId());
						if ((game != null) && game.isBattleStarted())
						{
							game.getStadium().broadcastPacket(os);
						}
					}
				}
				
				// Update effect icons for everyone targeting this owner.
				final ExAbnormalStatusUpdateFromTarget upd = new ExAbnormalStatusUpdateFromTarget(_owner);
				for (Creature creature : _owner.getStatus().getStatusListener())
				{
					if ((creature != null) && creature.isPlayer())
					{
						creature.sendPacket(upd);
					}
				}
				
				if (_owner.isPlayer() && (_owner.getTarget() == _owner))
				{
					_owner.sendPacket(upd);
				}
				
				_updateAbnormalStatus.set(false);
				_updateEffectIconTask = null;
			}, 300);
		}
	}
	
	/**
	 * Retrieves the set of current abnormal visual effects.<br>
	 * This method returns all active {@code AbnormalVisualEffect} objects for this entity.
	 * @return a {@code Set} containing the current {@code AbnormalVisualEffect} instances.
	 */
	public Set<AbnormalVisualEffect> getCurrentAbnormalVisualEffects()
	{
		return _abnormalVisualEffects;
	}
	
	/**
	 * Checks if the creature has a specific abnormal visual effect.<br>
	 * This method verifies if the provided {@code AbnormalVisualEffect} is present in the current list.
	 * @param ave The {@code AbnormalVisualEffect} to check for.
	 * @return {@code true} if the effect exists, otherwise {@code false}.
	 */
	public boolean hasAbnormalVisualEffect(AbnormalVisualEffect ave)
	{
		return _abnormalVisualEffects.contains(ave);
	}
	
	/**
	 * Starts one or more abnormal visual effects for the owner.<br>
	 * This method adds each provided effect to the internal collection.<br>
	 * It then triggers an update on the {@code _owner}.
	 * @param aves The array of {@code AbnormalVisualEffect} objects to start.
	 */
	public void startAbnormalVisualEffect(AbnormalVisualEffect... aves)
	{
		for (AbnormalVisualEffect ave : aves)
		{
			_abnormalVisualEffects.add(ave);
		}
		
		_owner.updateAbnormalVisualEffects();
	}
	
	/**
	 * Stops specific abnormal visual effects for the owner.<br>
	 * This method removes the provided effects from the internal collection.<br>
	 * It then updates the owner's abnormal visual effects state.
	 * @param aves The array of {@code AbnormalVisualEffect} objects to remove.
	 */
	public void stopAbnormalVisualEffect(AbnormalVisualEffect... aves)
	{
		for (AbnormalVisualEffect ave : aves)
		{
			_abnormalVisualEffects.remove(ave);
		}
		
		_owner.updateAbnormalVisualEffects();
	}
	
	/**
	 * Updates the internal list of active effects for the creature.<br>
	 * This method refreshes the current status of all buffs and debuffs.<br>
	 * It synchronizes the data with the underlying effect system.
	 */
	public void updateEffectList()
	{
		updateEffectList(true);
	}
	
	/**
	 * Updates the effect flags and recalculates stats for the owner.<br>
	 * This method processes active and passive buffs to determine current effects.<br>
	 * It also handles hidden buff logic and abnormal visual effects.
	 * @param broadcast Determines if changes should be sent to the client.
	 */
	private void updateEffectList(boolean broadcast)
	{
		// Create new empty flags.
		long flags = 0;
		final Set<AbnormalType> abnormalTypeFlags = EnumSet.noneOf(AbnormalType.class);
		final Set<AbnormalVisualEffect> abnormalVisualEffectFlags = EnumSet.noneOf(AbnormalVisualEffect.class);
		final Set<BuffInfo> unhideBuffs = new HashSet<>();
		
		// Recalculate new flags
		for (BuffInfo info : _actives)
		{
			if ((info != null) && info.isDisplayedForEffected())
			{
				final Skill skill = info.getSkill();
				final AbnormalType skillAbnormalType = skill.getAbnormalType();
				
				// Handle hidden buffs. Check if there was such abnormal before so we can continue.
				if ((_hiddenBuffs.get() > 0) && _stackedEffects.contains(skillAbnormalType))
				{
					// If incoming buff is not hidden, remove any hidden buffs with its abnormal type.
					if (info.isInUse())
					{
						unhideBuffs.removeIf(b -> b.isAbnormalType(skillAbnormalType));
					}
					// If this incoming buff is hidden and its first of its abnormal, or it removes any previous hidden buff with the same or lower abnormal level and add this instead.
					else if (!abnormalTypeFlags.contains(skillAbnormalType) || unhideBuffs.removeIf(b -> (b.isAbnormalType(skillAbnormalType)) && (b.getSkill().getAbnormalLevel() <= skill.getAbnormalLevel())))
					{
						unhideBuffs.add(info);
					}
				}
				
				// Add the EffectType flag.
				for (AbstractEffect e : info.getEffects())
				{
					flags |= e.getEffectFlags();
				}
				
				// Add the AbnormalType flag.
				abnormalTypeFlags.add(skillAbnormalType);
				
				// Add AbnormalVisualEffect flag.
				if (skill.hasAbnormalVisualEffects())
				{
					for (AbnormalVisualEffect ave : skill.getAbnormalVisualEffects())
					{
						abnormalVisualEffectFlags.add(ave);
						_abnormalVisualEffects.add(ave);
					}
					
					if (broadcast)
					{
						_owner.updateAbnormalVisualEffects();
					}
				}
			}
		}
		
		// Add passive effect flags.
		for (BuffInfo info : _passives)
		{
			if (info != null)
			{
				// Add the EffectType flag.
				for (AbstractEffect e : info.getEffects())
				{
					flags |= e.getEffectFlags();
				}
				
				// Add AbnormalVisualEffect flag.
				final Skill skill = info.getSkill();
				if (skill.hasAbnormalVisualEffects())
				{
					for (AbnormalVisualEffect ave : skill.getAbnormalVisualEffects())
					{
						abnormalVisualEffectFlags.add(ave);
						_abnormalVisualEffects.add(ave);
					}
					
					if (broadcast)
					{
						_owner.updateAbnormalVisualEffects();
					}
				}
			}
		}
		
		// Replace the old flags with the new flags.
		_effectFlags = flags;
		_stackedEffects = abnormalTypeFlags;
		
		// Unhide the selected buffs.
		unhideBuffs.forEach(b ->
		{
			b.setInUse(true);
			_hiddenBuffs.decrementAndGet();
		});
		
		// Recalculate all stats
		_owner.getStat().recalculateStats(broadcast);
		if (broadcast && _owner.isPlayer())
		{
			_owner.asPlayer().scheduleEffectUserInfoUpdate();
		}
		
		if (broadcast)
		{
			// Check if there is change in AbnormalVisualEffect.
			if (!abnormalVisualEffectFlags.equals(_abnormalVisualEffects))
			{
				_abnormalVisualEffects = abnormalVisualEffectFlags;
				_owner.updateAbnormalVisualEffects();
			}
			
			// Send updates to the client.
			updateEffectIcons(false);
		}
	}
	
	/**
	 * Checks if a specific effect flag is currently active.<br>
	 * This method compares the internal bitmask against the provided {@code EffectFlag}.
	 * @param flag The {@code EffectFlag} to check for.
	 * @return {@code true} if the flag is set, otherwise {@code false}.
	 */
	public boolean isAffected(EffectFlag flag)
	{
		return (_effectFlags & flag.getMask()) != 0;
	}
}
