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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.data.xml.ActionData;
import org.l2jmobius.gameserver.data.xml.PetSkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.holders.player.AutoUseSettingsHolder;
import org.l2jmobius.gameserver.entity.actor.instance.Guard;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.actor.transform.TransformTemplate;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.ActionUserHandler;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemUse;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.AttachSkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.replacement.SkillReplacementList;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Manages the background tasks for automatic item and skill usage.<br>
 * This class handles the logic for players who have enabled auto-use features.<br>
 * It ensures that items, skills, and buffs are triggered correctly based on configured settings.
 * @author Mobius
 */
public class AutoUseTaskManager
{
	private static final Set<Set<Player>> POOLS = ConcurrentHashMap.newKeySet();
	private static final Set<Player> AUTO_CASTING = ConcurrentHashMap.newKeySet();
	private static final Set<Player> ADVANCING_PLAYERS = ConcurrentHashMap.newKeySet();
	private static final int POOL_SIZE = 200;
	private static final int TASK_DELAY = 300;
	private static final int REUSE_MARGIN_TIME = 3;
	
	/**
	 * Initializes a new instance of the {@code AutoUseTaskManager}.<br>
	 * This constructor is used to set up the manager for handling auto-use tasks.
	 */
	protected AutoUseTaskManager()
	{
	}
	
	private class AutoUse implements Runnable
	{
		private final Set<Player> _players;
		
		public AutoUse(Set<Player> players)
		{
			_players = players;
		}
		
		@Override
		public void run()
		{
			if (_players.isEmpty())
			{
				return;
			}
			
			for (Player player : _players)
			{
				if (player.getAutoUseSettings().isEmpty() || !player.isOnline() || (player.getOfflineMode().isEnabled() && !player.getOfflineMode().isPlayRunning()))
				{
					stopAutoUseTask(player);
					continue;
				}
				
				if (player.isSitting() || player.hasBlockActions() || player.isControlBlocked() || player.isAlikeDead() || player.isMounted())
				{
					continue;
				}
				
				final Transform transform = player.getTransformation();
				if ((transform != null) && transform.isRiding())
				{
					continue;
				}
				
				final boolean isInPeaceZone = player.isInsideZone(ZoneId.PEACE) || player.isInsideZone(ZoneId.SAYUNE);
				
				if (GeneralConfig.ENABLE_AUTO_ITEM && !isInPeaceZone)
				{
					final AutoUseSettingsHolder autoUseSettings = player.getAutoUseSettings();
					ITEMS: for (Integer itemId : autoUseSettings.getAutoSupplyItems())
					{
						if (player.isTeleporting())
						{
							break ITEMS;
						}
						
						final Item item = player.getInventory().getItemByItemId(itemId.intValue());
						if (item == null)
						{
							autoUseSettings.getAutoSupplyItems().remove(itemId);
							continue ITEMS;
						}
						
						if (item.isOlyRestrictedItem() && player.isInOlympiadMode())
						{
							autoUseSettings.getAutoSupplyItems().remove(itemId);
							continue ITEMS;
						}
						
						final ItemTemplate template = item.getTemplate();
						if ((template == null) || !template.checkCondition(player, player, false))
						{
							continue ITEMS;
						}
						
						final List<ItemSkillHolder> skills = template.getSkills(ItemSkillType.NORMAL);
						if (skills != null)
						{
							for (ItemSkillHolder itemSkillHolder : skills)
							{
								final Skill skill = itemSkillHolder.getSkill();
								if (player.isAffectedBySkill(skill.getId()) || player.hasSkillReuse(skill.getReuseHashCode()) || !skill.checkCondition(player, player, false))
								{
									continue ITEMS;
								}
							}
						}
						
						final int reuseDelay = item.getReuseDelay();
						if ((reuseDelay <= 0) || (player.getItemRemainingReuseTime(item.getObjectId()) <= 0))
						{
							final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
							if ((handler != null) && handler.onItemUse(player, item, false))
							{
								if (reuseDelay > 0)
								{
									player.addTimeStampItem(item, reuseDelay);
								}
								
								// Notify events.
								if (EventDispatcher.getInstance().hasListener(EventType.ON_ITEM_USE, template))
								{
									EventDispatcher.getInstance().notifyEventAsync(new OnItemUse(player, item), template);
								}
							}
						}
					}
				}
				
				if (GeneralConfig.ENABLE_AUTO_POTION && !isInPeaceZone && (player.getCurrentHpPercent() < player.getAutoPlaySettings().getAutoPotionPercent()))
				{
					final int itemId = player.getAutoUseSettings().getAutoPotionItem();
					if (itemId > 0)
					{
						final Item item = player.getInventory().getItemByItemId(itemId);
						if (item == null)
						{
							player.getAutoUseSettings().setAutoPotionItem(0);
						}
						else
						{
							final int reuseDelay = item.getReuseDelay();
							if ((reuseDelay <= 0) || (player.getItemRemainingReuseTime(item.getObjectId()) <= 0))
							{
								final EtcItem etcItem = item.getEtcItem();
								final IItemHandler handler = ItemHandler.getInstance().getHandler(etcItem);
								if ((handler != null) && handler.onItemUse(player, item, false))
								{
									if (reuseDelay > 0)
									{
										player.addTimeStampItem(item, reuseDelay);
									}
									
									// Notify events.
									final ItemTemplate itemTemplate = item.getTemplate();
									if (EventDispatcher.getInstance().hasListener(EventType.ON_ITEM_USE, itemTemplate))
									{
										EventDispatcher.getInstance().notifyEventAsync(new OnItemUse(player, item), itemTemplate);
									}
								}
							}
						}
					}
				}
				
				if (GeneralConfig.ENABLE_AUTO_SKILL)
				{
					BUFFS: for (Integer skillId : player.getAutoUseSettings().getAutoBuffs())
					{
						// Fixes start area issue.
						if (isInPeaceZone)
						{
							break BUFFS;
						}
						
						// Already casting.
						if (player.isCastingNow())
						{
							break BUFFS;
						}
						
						// Attacking.
						if (player.isAttackingNow())
						{
							break BUFFS;
						}
						
						// Player is teleporting.
						if (player.isTeleporting())
						{
							break BUFFS;
						}
						
						Playable pet = null;
						Skill skill = player.getKnownSkill(skillId.intValue());
						if (skill == null)
						{
							if (player.hasServitors())
							{
								SUMMON_SEARCH: for (Summon summon : player.getServitors().values())
								{
									skill = summon.getKnownSkill(skillId.intValue());
									if (skill != null)
									{
										pet = summon;
										break SUMMON_SEARCH;
									}
								}
							}
							
							if ((skill == null) && player.hasPet())
							{
								pet = player.getPet();
								skill = pet.getKnownSkill(skillId.intValue());
							}
							
							if (skill == null)
							{
								player.getAutoUseSettings().getAutoBuffs().remove(skillId);
								continue BUFFS;
							}
						}
						
						// Buff use check.
						final WorldObject target = player.getTarget();
						if (!canCastBuff(player, target, skill))
						{
							continue BUFFS;
						}
						
						ATTACH_SEARCH: for (AttachSkillHolder holder : skill.getAttachSkills())
						{
							if (player.isAffectedBySkill(holder.getRequiredSkillId()))
							{
								skill = holder.getSkill();
								break ATTACH_SEARCH;
							}
						}
						
						// Playable target cast.
						final Playable caster = pet != null ? pet : player;
						if ((target != null) && (target.isPlayable()))
						{
							final Player targetPlayer = target.asPlayer();
							if (((targetPlayer.getPvpFlag() == 0) && (targetPlayer.getReputation() >= 0)) || (targetPlayer.getParty() == caster.getParty()))
							{
								caster.doCast(skill);
							}
							else
							{
								if (!caster.getEffectList().isAffectedBySkill(skill.getId()))
								{
									final WorldObject savedTarget = target;
									caster.setTarget(caster);
									caster.doCast(skill);
									caster.setTarget(savedTarget);
								}
							}
						}
						else // Target self, cast and re-target.
						{
							final WorldObject savedTarget = target;
							caster.setTarget(caster);
							caster.doCast(skill);
							caster.setTarget(savedTarget);
						}
					}
					
					// Continue when auto play is not enabled.
					if (!player.isAutoPlaying())
					{
						continue;
					}
					
					// A cast completion advances the rotation on its own thread, so a pool pass never selects a second skill under it.
					if (ADVANCING_PLAYERS.add(player))
					{
						try
						{
							final int count = player.getAutoUseSettings().getAutoSkills().size();
							SKILLS: for (int i = 0; i < count; i++)
							{
								// Already casting.
								if (player.isCastingNow())
								{
									break SKILLS;
								}
								
								// Player is teleporting.
								if (player.isTeleporting())
								{
									break SKILLS;
								}
								
								// Acquire next skill.
								Playable pet = null;
								final WorldObject target = player.getTarget();
								final Integer skillId = player.getAutoUseSettings().getNextSkillId();
								Skill skill = player.getKnownSkill(skillId);
								if (skill == null)
								{
									if (player.hasServitors())
									{
										SUMMON_SEARCH: for (Summon summon : player.getServitors().values())
										{
											skill = summon.getKnownSkill(skillId.intValue());
											if (skill == null)
											{
												skill = PetSkillData.getInstance().getKnownSkill(summon, skillId);
											}
											
											if (skill != null)
											{
												pet = summon;
												pet.setTarget(target);
												break SUMMON_SEARCH;
											}
										}
									}
									
									if ((skill == null) && player.hasPet())
									{
										pet = player.getPet();
										skill = pet.getKnownSkill(skillId);
										if (skill == null)
										{
											skill = PetSkillData.getInstance().getKnownSkill(pet.asSummon(), skillId);
										}
										
										if (pet.isSkillDisabled(skill))
										{
											player.getAutoUseSettings().incrementSkillOrder();
											break SKILLS;
										}
									}
									
									if (skill == null)
									{
										final AutoUseSettingsHolder autoUseSettings = player.getAutoUseSettings();
										autoUseSettings.getAutoSkills().remove(skillId);
										autoUseSettings.resetSkillOrder();
										break SKILLS;
									}
								}
								
								// Casting on self stops movement.
								if (target == player)
								{
									break SKILLS;
								}
								
								// Check negative effect skill target.
								if ((target == null) || target.asCreature().isDead())
								{
									// Remove queued skill.
									if (player.getQueuedSkill() != null)
									{
										player.setQueuedSkill(null, null, false, false);
									}
									break SKILLS;
								}
								
								// Peace zone and auto attackable checks.
								if (target.isInsideZone(ZoneId.PEACE) || !target.isAutoAttackable(player))
								{
									break SKILLS;
								}
								
								// Do not attack guards.
								if (target instanceof Guard)
								{
									final int targetMode = player.getAutoPlaySettings().getNextTargetMode();
									if ((targetMode != 3 /* NPC */) && (targetMode != 0 /* Any Target */))
									{
										break SKILLS;
									}
								}
								
								// Increment skill order.
								player.getAutoUseSettings().incrementSkillOrder();
								
								// Skill use check, against the replacement useMagic will swap in when one is active, since that is the skill whose reuse and conditions apply.
								final Playable caster = pet != null ? pet : player;
								final SkillReplacementList replacementList = player.getSkillReplacementListIfPresent();
								if (!canUseMagic(caster, target, ((pet != null) || (replacementList == null)) ? skill : replacementList.resolveSkill(skill)))
								{
									continue SKILLS;
								}
								
								// Use the skill.
								if (pet != null)
								{
									caster.useMagic(skill, null, true, false);
									break SKILLS;
								}
								
								// A player cast is marked so its completion continues the rotation without waiting for the next pass, unless it neither started nor queued.
								AUTO_CASTING.add(player);
								if (!player.useMagic(skill, null, true, false) && (player.getQueuedSkill() == null))
								{
									AUTO_CASTING.remove(player);
								}
								
								break SKILLS;
							}
						}
						finally
						{
							ADVANCING_PLAYERS.remove(player);
						}
					}
					
					ACTIONS: for (Integer actionId : player.getAutoUseSettings().getAutoActions())
					{
						final BuffInfo info = player.getEffectList().getFirstBuffInfoByAbnormalType(AbnormalType.BOT_PENALTY);
						if (info != null)
						{
							for (AbstractEffect effect : info.getEffects())
							{
								if (!effect.checkCondition(actionId))
								{
									player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_SO_YOUR_ACTIONS_HAVE_BEEN_RESTRICTED);
									break ACTIONS;
								}
							}
						}
						
						// Do not allow to do some action if player is transformed.
						if (transform != null)
						{
							final TransformTemplate transformTemplate = transform.getTemplate(player);
							final int[] allowedActions = transformTemplate.getBasicActionList();
							if ((allowedActions == null) || (Arrays.binarySearch(allowedActions, actionId) < 0))
							{
								continue ACTIONS;
							}
						}
						
						final ActionDataHolder actionHolder = ActionData.getInstance().getActionData(actionId);
						if (actionHolder != null)
						{
							final IActionUserHandler actionHandler = ActionUserHandler.getInstance().getHandler(actionHolder.getHandler());
							if (actionHandler != null)
							{
								if (!actionHandler.isPetAction())
								{
									actionHandler.onAction(player, actionHolder, false, false);
								}
								else
								{
									final Summon summon = player.getAnyServitor();
									if ((summon != null) && !summon.isAlikeDead())
									{
										final Skill skill = summon.getKnownSkill(actionHolder.getOptionId());
										if ((skill != null) && !canSummonCastSkill(player, summon, skill))
										{
											continue ACTIONS;
										}
										
										actionHandler.onAction(player, actionHolder, false, false);
									}
								}
							}
						}
					}
				}
			}
		}
		
		private boolean canCastBuff(Player player, WorldObject target, Skill skill)
		{
			// Summon check.
			if ((skill.getAffectScope() == AffectScope.SUMMON_EXCEPT_MASTER) || (skill.getTargetType() == TargetType.SUMMON))
			{
				if (!player.hasServitors())
				{
					return false;
				}
				
				int occurrences = 0;
				for (Summon servitor : player.getServitors().values())
				{
					if (servitor.isAffectedBySkill(skill.getId()))
					{
						occurrences++;
					}
				}
				
				if (occurrences == player.getServitors().size())
				{
					return false;
				}
			}
			
			if ((target != null) && target.isCreature() && target.asCreature().isAlikeDead() && (skill.getTargetType() != TargetType.SELF) && (skill.getTargetType() != TargetType.NPC_BODY) && (skill.getTargetType() != TargetType.PC_BODY))
			{
				return false;
			}
			
			final Playable playableTarget = (target == null) || !target.isPlayable() || (skill.getTargetType() == TargetType.SELF) ? player : target.asPlayable();
			if ((player != playableTarget) && (player.calculateDistance3D(playableTarget) > skill.getCastRange()))
			{
				return false;
			}
			
			if (!canUseMagic(player, playableTarget, skill))
			{
				return false;
			}
			
			final BuffInfo buffInfo = playableTarget.getEffectList().getBuffInfoBySkillId(skill.getId());
			final BuffInfo abnormalBuffInfo = playableTarget.getEffectList().getFirstBuffInfoByAbnormalType(skill.getAbnormalType());
			if (abnormalBuffInfo != null)
			{
				if (buffInfo != null)
				{
					return (abnormalBuffInfo.getSkill().getId() == buffInfo.getSkill().getId()) && ((buffInfo.getTime() <= REUSE_MARGIN_TIME) || (buffInfo.getSkill().getLevel() < skill.getLevel()));
				}
				
				return (abnormalBuffInfo.getSkill().getAbnormalLevel() < skill.getAbnormalLevel()) || abnormalBuffInfo.isAbnormalType(AbnormalType.NONE);
			}
			
			return buffInfo == null;
		}
		
		private boolean canUseMagic(Playable playable, WorldObject target, Skill skill)
		{
			if ((skill.getItemConsumeCount() > 0) && (playable.getInventory().getInventoryItemCount(skill.getItemConsumeId(), -1) < skill.getItemConsumeCount()))
			{
				return false;
			}
			
			if (playable.isPlayer() && (playable.asPlayer().getCharges() < skill.getChargeConsumeCount()))
			{
				return false;
			}
			
			final int mpConsume = skill.getMpInitialConsume() + skill.getMpConsume();
			if ((mpConsume > 0) && (playable.getCurrentMp() < mpConsume))
			{
				return false;
			}
			
			for (AttachSkillHolder holder : skill.getAttachSkills())
			{
				if (playable.isAffectedBySkill(holder.getRequiredSkillId()) //
					&& (playable.hasSkillReuse(holder.getSkill().getReuseHashCode()) || playable.isAffectedBySkill(holder)))
				{
					return false;
				}
			}
			
			return !playable.isSkillDisabled(skill) && skill.checkCondition(playable, target, false);
		}
		
		private boolean canSummonCastSkill(Player player, Summon summon, Skill skill)
		{
			if (skill.hasNegativeEffect() && (player.getTarget() == null))
			{
				return false;
			}
			
			final int mpConsume = skill.getMpConsume() + skill.getMpInitialConsume();
			if ((((mpConsume != 0) && (mpConsume > (int) (summon.getCurrentMp()))) || ((skill.getHpConsume() != 0) && (skill.getHpConsume() > (int) (summon.getCurrentHp())))))
			{
				return false;
			}
			
			if (summon.isSkillDisabled(skill))
			{
				return false;
			}
			
			if (((player.getTarget() != null) && !skill.checkCondition(summon, player.getTarget(), false)) || ((player.getTarget() == null) && !skill.checkCondition(summon, player, false)))
			{
				return false;
			}
			
			if ((skill.getItemConsumeCount() > 0) && (summon.getInventory().getInventoryItemCount(skill.getItemConsumeId(), -1) < skill.getItemConsumeCount()))
			{
				return false;
			}
			
			if (skill.getTargetType().equals(TargetType.SELF) || skill.getTargetType().equals(TargetType.SUMMON))
			{
				final BuffInfo summonInfo = summon.getEffectList().getBuffInfoBySkillId(skill.getId());
				return (summonInfo != null) && (summonInfo.getTime() >= REUSE_MARGIN_TIME);
			}
			
			if ((skill.getEffects(EffectScope.GENERAL) != null) && skill.getEffects(EffectScope.GENERAL).stream().anyMatch(a -> a.getEffectType().equals(EffectType.MANAHEAL_BY_LEVEL)) && (player.getCurrentMpPercent() > 80))
			{
				return false;
			}
			
			final BuffInfo buffInfo = player.getEffectList().getBuffInfoBySkillId(skill.getId());
			final BuffInfo abnormalBuffInfo = player.getEffectList().getFirstBuffInfoByAbnormalType(skill.getAbnormalType());
			if (abnormalBuffInfo != null)
			{
				if (buffInfo != null)
				{
					return (abnormalBuffInfo.getSkill().getId() == buffInfo.getSkill().getId()) && ((buffInfo.getTime() <= REUSE_MARGIN_TIME) || (buffInfo.getSkill().getLevel() < skill.getLevel()));
				}
				
				return (abnormalBuffInfo.getSkill().getAbnormalLevel() < skill.getAbnormalLevel()) || abnormalBuffInfo.isAbnormalType(AbnormalType.NONE);
			}
			
			return true;
		}
	}
	
	/**
	 * Starts the automatic item use task for a specific player.<br>
	 * This method adds the {@code Player} to an existing pool or creates a new one.<br>
	 * It schedules the task to run at a fixed rate using a thread pool.
	 * @param player The {@code Player} for whom the auto-use task will be started.
	 */
	public synchronized void startAutoUseTask(Player player)
	{
		for (Set<Player> pool : POOLS)
		{
			if (pool.contains(player))
			{
				return;
			}
		}
		
		for (Set<Player> pool : POOLS)
		{
			if (pool.size() < POOL_SIZE)
			{
				pool.add(player);
				return;
			}
		}
		
		final Set<Player> pool = ConcurrentHashMap.newKeySet(POOL_SIZE);
		pool.add(player);
		ThreadPool.schedulePriorityTaskAtFixedRate(new AutoUse(pool), TASK_DELAY, TASK_DELAY);
		POOLS.add(pool);
	}
	
	/**
	 * Continues the auto-skill rotation as soon as a marked cast completes, or picks a new target when the cast finished off the current one.
	 * @param player the player whose cast completed
	 * @param aborted whether the cast was interrupted
	 */
	public void onAutoSkillCastFinished(Player player, boolean aborted)
	{
		// Only a marked cast continues the rotation, and an interrupted one is left to the next pass.
		if (!AUTO_CASTING.remove(player) || aborted || !player.isAutoPlaying())
		{
			return;
		}
		
		final WorldObject target = player.getTarget();
		if ((target == null) || target.asCreature().isDead())
		{
			AutoPlayTaskManager.getInstance().pickNextTarget(player);
		}
		else
		{
			ThreadPool.execute(new AutoUse(Set.of(player)));
		}
	}
	
	/**
	 * Stops the auto-use task for a specific player.<br>
	 * This method resets the skill order in the {@code AutoUseSettingsHolder}.<br>
	 * It also removes the {@code Player} from all active task pools.
	 * @param player The {@code Player} whose auto-use task should be stopped.
	 */
	public void stopAutoUseTask(Player player)
	{
		// A cast still in flight must not continue a rotation that was stopped.
		AUTO_CASTING.remove(player);
		final AutoUseSettingsHolder autoUseSettings = player.getAutoUseSettings();
		autoUseSettings.resetSkillOrder();
		if (autoUseSettings.isEmpty() || !player.isOnline() || (player.getOfflineMode().isEnabled() && !player.getOfflineMode().isPlayRunning()))
		{
			for (Set<Player> pool : POOLS)
			{
				if (pool.remove(player))
				{
					return;
				}
			}
		}
	}
	
	/**
	 * Adds a specific item to the auto-supply list for a player.<br>
	 * This method also starts the auto-use task for the player.
	 * @param player The {@code Player} who will receive the auto-supply item.
	 * @param itemId The unique identifier of the item to add.
	 */
	public void addAutoSupplyItem(Player player, int itemId)
	{
		player.getAutoUseSettings().getAutoSupplyItems().add(itemId);
		startAutoUseTask(player);
	}
	
	/**
	 * Removes a specific item from the player's auto-supply list.<br>
	 * This method also stops the active auto-use task for the {@code Player}.
	 * @param player The {@code Player} who owns the auto-supply settings.
	 * @param itemId The unique identifier of the item to remove.
	 */
	public void removeAutoSupplyItem(Player player, int itemId)
	{
		player.getAutoUseSettings().getAutoSupplyItems().remove(itemId);
		stopAutoUseTask(player);
	}
	
	/**
	 * Sets the specific item used for auto-potions for a player.<br>
	 * This method updates the {@code AutoUseSettingsHolder} and starts the task.
	 * @param player The {@code Player} instance to update.
	 * @param itemId The unique identifier of the potion item.
	 */
	public void setAutoPotionItem(Player player, int itemId)
	{
		player.getAutoUseSettings().setAutoPotionItem(itemId);
		startAutoUseTask(player);
	}
	
	/**
	 * Removes the auto-potion item from the player's settings.<br>
	 * This method also stops the active auto-use task for the specified player.
	 * @param player The {@code Player} whose auto-potion item will be removed.
	 */
	public void removeAutoPotionItem(Player player)
	{
		player.getAutoUseSettings().setAutoPotionItem(0);
		stopAutoUseTask(player);
	}
	
	/**
	 * Adds a specific skill to the player's auto-buff list.<br>
	 * This method also starts the auto-use task for the player.
	 * @param player The {@code Player} object to modify.
	 * @param skillId The unique identifier of the skill to add.
	 */
	public void addAutoBuff(Player player, int skillId)
	{
		player.getAutoUseSettings().getAutoBuffs().add(skillId);
		startAutoUseTask(player);
	}
	
	/**
	 * Removes a specific buff from the player's auto-buff settings.<br>
	 * This method also stops the active auto-use task for the player.
	 * @param player The {@code Player} instance to modify.
	 * @param skillId The unique identifier of the skill to remove.
	 */
	public void removeAutoBuff(Player player, int skillId)
	{
		player.getAutoUseSettings().getAutoBuffs().remove(skillId);
		stopAutoUseTask(player);
	}
	
	/**
	 * Adds a specific skill to the player's auto-use list.<br>
	 * This method also starts the auto-use task for the {@code Player}.
	 * @param player The {@code Player} who will receive the new skill.
	 * @param skillId The unique identifier of the skill to add.
	 */
	public void addAutoSkill(Player player, Integer skillId)
	{
		player.getAutoUseSettings().getAutoSkills().add(skillId);
		startAutoUseTask(player);
	}
	
	/**
	 * Removes a specific skill from the player's auto-use settings.<br>
	 * This method also stops the active auto-use task for the player.
	 * @param player The {@code Player} whose skills are being modified.
	 * @param skillId The unique identifier of the skill to remove.
	 */
	public void removeAutoSkill(Player player, Integer skillId)
	{
		player.getAutoUseSettings().getAutoSkills().remove(skillId);
		stopAutoUseTask(player);
	}
	
	/**
	 * Adds a specific action to the player's auto-use settings.<br>
	 * This method also starts the auto-use task for the player.
	 * @param player The {@code Player} who will perform the action.
	 * @param actionId The unique identifier for the action to add.
	 */
	public void addAutoAction(Player player, int actionId)
	{
		player.getAutoUseSettings().getAutoActions().add(actionId);
		startAutoUseTask(player);
	}
	
	/**
	 * Removes a specific auto action from the player's settings.<br>
	 * This method also stops the active auto use task for the player.
	 * @param player The {@code Player} whose actions are being modified.
	 * @param actionId The unique identifier of the action to remove.
	 */
	public void removeAutoAction(Player player, int actionId)
	{
		player.getAutoUseSettings().getAutoActions().remove(actionId);
		stopAutoUseTask(player);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code AutoUseTaskManager}.<br>
	 * Use this method to access the manager from anywhere in your code.
	 * @return The active {@code AutoUseTaskManager} instance.
	 */
	public static AutoUseTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AutoUseTaskManager INSTANCE = new AutoUseTaskManager();
	}
}
