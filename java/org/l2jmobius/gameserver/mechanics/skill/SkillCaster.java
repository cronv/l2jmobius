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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PvpConfig;
import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.xml.ActionData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.SiegeFlag;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.ActionType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillFinishCast;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillUse;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSkillSee;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.options.OptionSkillHolder;
import org.l2jmobius.gameserver.mechanics.options.OptionSkillType;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.mechanics.skill.enums.NextActionType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillUseHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.StatusUpdateType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExMagicSkillUseGround;
import org.l2jmobius.gameserver.network.serverpackets.ExRotation;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillCanceled;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillLaunched;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.MoveToPawn;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Handles the execution and lifecycle of skill casting for entities.<br>
 * This class manages the timing, logic, and progression of skills from initiation to completion.<br>
 * It ensures that skill mechanics are processed correctly within the game world.
 * @author Nik, Mobius
 */
public class SkillCaster implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(SkillCaster.class.getName());
	
	private static final Set<AbnormalType> DISABLE_TYPES = EnumSet.of(AbnormalType.SLEEP, AbnormalType.ROOT_PHYSICALLY, AbnormalType.ROOT_MAGICALLY, AbnormalType.PARALYZE);
	
	private final Creature _caster;
	private final WorldObject _target;
	private final Skill _skill;
	private final Item _item;
	private final SkillCastingType _castingType;
	private final boolean _shiftPressed;
	private int _hitTime;
	private int _cancelTime;
	private int _coolTime;
	private Collection<WorldObject> _targets;
	private ScheduledFuture<?> _task;
	private int _phase;
	
	/**
	 * Initializes a new instance of the {@code SkillCaster} class.<br>
	 * This constructor sets up all necessary data for executing a skill.<br>
	 * It also calculates the required timing based on the provided parameters.
	 * @param caster The {@code Creature} performing the action.
	 * @param target The {@code WorldObject} being targeted by the skill.
	 * @param skill The {@code Skill} to be cast.
	 * @param item The {@code Item} used for the skill, if any.
	 * @param castingType The {@code SkillCastingType} of the action.
	 * @param ctrlPressed Indicates if the {@code true} value for the control key is active.
	 * @param shiftPressed Indicates if the {@code true} value for the shift key is active.
	 * @param castTime The time in milliseconds required to complete the cast.
	 */
	private SkillCaster(Creature caster, WorldObject target, Skill skill, Item item, SkillCastingType castingType, boolean ctrlPressed, boolean shiftPressed, int castTime)
	{
		Objects.requireNonNull(caster);
		Objects.requireNonNull(skill);
		Objects.requireNonNull(castingType);
		
		_caster = caster;
		_target = target;
		_skill = skill;
		_item = item;
		_castingType = castingType;
		_shiftPressed = shiftPressed;
		
		calcSkillTiming(caster, skill, castTime);
	}
	
	/**
	 * Initiates the casting process for a specific skill.<br>
	 * This method checks if the caster is allowed to use the skill based on game rules.<br>
	 * It returns a {@code SkillCaster} object to manage the casting lifecycle.
	 * @param caster The {@code Creature} performing the action.
	 * @param target The {@code WorldObject} being targeted by the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param item The {@code Item} used for the skill, if any.
	 * @param castingType The {@code SkillCastingType} of the action.
	 * @param ctrlPressed Whether the {@code Ctrl} key is held down.
	 * @param shiftPressed Whether the {@code Shift} key is held down.
	 * @return A new {@code SkillCaster} instance or {@code null} if casting is blocked.
	 */
	public static SkillCaster castSkill(Creature caster, WorldObject target, Skill skill, Item item, SkillCastingType castingType, boolean ctrlPressed, boolean shiftPressed)
	{
		// Prevent players from attacking before the Olympiad countdown ends.
		if (caster.isPlayer() && skill.hasNegativeEffect())
		{
			final Player player = caster.asPlayer();
			if (player.isInOlympiadMode() && !player.isOlympiadStart())
			{
				return null;
			}
		}
		
		return castSkill(caster, target, skill, item, castingType, ctrlPressed, shiftPressed, -1);
	}
	
	/**
	 * Initiates the casting process for a specific skill.<br>
	 * This method validates all requirements such as range and target conditions.<br>
	 * If successful, it returns a {@code SkillCaster} object to manage the execution.
	 * @param caster The {@code Creature} performing the action.
	 * @param worldObject The {@code WorldObject} being targeted or interacted with.
	 * @param skill The {@code Skill} to be cast.
	 * @param item The {@code Item} used for the skill, if any.
	 * @param castingType The {@code SkillCastingType} of the action.
	 * @param ctrlPressed Whether the {@code true} value for the control key is active.
	 * @param shiftPressed Whether the {@code true} value for the shift key is active.
	 * @param castTime The time in milliseconds required to complete the cast.
	 * @return A new {@code SkillCaster} instance or {@code null} if conditions are not met.
	 */
	public static SkillCaster castSkill(Creature caster, WorldObject worldObject, Skill skill, Item item, SkillCastingType castingType, boolean ctrlPressed, boolean shiftPressed, int castTime)
	{
		if ((caster == null) || (skill == null) || (castingType == null))
		{
			return null;
		}
		
		if (!checkUseConditions(caster, skill, castingType))
		{
			return null;
		}
		
		// Check true aiming target of the skill.
		final WorldObject target = skill.getTarget(caster, worldObject, ctrlPressed, shiftPressed, false);
		if (target == null)
		{
			return null;
		}
		
		// You should not heal/buff monsters without pressing the ctrl button.
		if (caster.isPlayer() && (target.isMonster() && !target.isFakePlayer()) && (skill.getEffectPoint() > 0) && !ctrlPressed)
		{
			caster.sendPacket(SystemMessageId.INVALID_TARGET);
			return null;
		}
		
		// Siege headquarters cannot be healed or buffed.
		if ((target instanceof SiegeFlag) && (skill.getEffectPoint() > 0))
		{
			caster.sendPacket(SystemMessageId.INVALID_TARGET);
			return null;
		}
		
		final int castRange = skill.getCastRange();
		if ((castRange > 0) && !LocationUtil.checkIfInRange(castRange + (int) caster.getStat().getValue(Stat.MAGIC_ATTACK_RANGE, 0), caster, target, false))
		{
			return null;
		}
		
		// Schedule a thread that will execute 500ms before casting time is over (for animation issues and retail handling).
		final SkillCaster skillCaster = new SkillCaster(caster, target, skill, item, castingType, ctrlPressed, shiftPressed, castTime);
		skillCaster.run();
		return skillCaster;
	}
	
	@Override
	public void run()
	{
		final boolean instantCast = (_castingType == SkillCastingType.SIMULTANEOUS) || _skill.isAbnormalInstant() || _skill.isWithoutAction() || _skill.isToggle();
		
		// Skills with instant cast are never launched.
		if (instantCast)
		{
			triggerCast(_caster, _target, _skill, _item, false);
			return;
		}
		
		long nextTaskDelay = 0;
		boolean hasNextPhase = false;
		switch (_phase++)
		{
			case 0: // Start skill casting.
			{
				hasNextPhase = startCasting();
				nextTaskDelay = _hitTime;
				break;
			}
			case 1: // Launch the skill.
			{
				hasNextPhase = launchSkill();
				nextTaskDelay = _cancelTime;
				break;
			}
			case 2: // Finish launching and apply effects.
			{
				hasNextPhase = finishSkill();
				nextTaskDelay = _coolTime;
				break;
			}
		}
		
		// Reschedule next task if we have such.
		if (hasNextPhase)
		{
			_task = ThreadPool.schedule(this, nextTaskDelay);
		}
		else
		{
			// Stop casting if there is no next phase.
			stopCasting(false);
		}
	}
	
	/**
	 * Initiates the casting process for a skill.<br>
	 * Validates requirements such as mana, fame, and clan reputation.<br>
	 * Handles resource consumption and updates the caster state.<br>
	 * Sends necessary packets to notify clients of the start of the action.
	 * @return {@code true} if the casting started successfully, {@code false} otherwise.
	 */
	public boolean startCasting()
	{
		final Creature caster = _caster;
		final WorldObject target = _target;
		
		if ((caster == null) || (target == null))
		{
			return false;
		}
		
		_coolTime = Formulas.calcAtkSpd(caster, _skill, _skill.getCoolTime()); // TODO Get proper formula of this.
		final int displayedCastTime = _hitTime + _cancelTime; // For client purposes, it must be displayed to player the skill casting time + launch time.
		final boolean instantCast = (_castingType == SkillCastingType.SIMULTANEOUS) || _skill.isAbnormalInstant() || _skill.isWithoutAction();
		
		// Add this SkillCaster to the creature so it can be marked as casting.
		if (!instantCast)
		{
			caster.addSkillCaster(_castingType, this);
		}
		
		// Disable the skill during the re-use delay and create a task EnableSkill with Medium priority to enable it at the end of the re-use delay.
		int reuseDelay = caster.getStat().getReuseTime(_skill);
		if (caster.isPlayable())
		{
			reuseDelay = (int) (reuseDelay * ClassBalanceConfig.SKILL_REUSE_MULTIPLIERS[caster.asPlayer().getPlayerClass().getId()]);
		}
		
		if (reuseDelay > 10)
		{
			// Skill mastery doesn't affect static skills / A2 and item skills on reuse.
			if (Formulas.calcSkillMastery(caster, _skill) && !_skill.isStatic() && (_skill.getReferenceItemId() == 0) && (_skill.getOperateType() == SkillOperateType.A1))
			{
				reuseDelay = 100;
				caster.sendPacket(SystemMessageId.A_SKILL_IS_READY_TO_BE_USED_AGAIN);
			}
			
			if (reuseDelay > 1000)
			{
				caster.addTimeStamp(_skill, reuseDelay);
			}
			else
			{
				caster.disableSkill(_skill, reuseDelay);
			}
		}
		
		// Stop movement when casting. Except instant cast.
		if (!instantCast)
		{
			caster.getAI().clientStopMoving(null);
			
			// Also replace other intentions with idle. (Mainly done for MOVE_TO).
			if (caster.isPlayer() && !_skill.hasNegativeEffect())
			{
				caster.getAI().setIntentionIdle();
			}
		}
		
		// Reduce talisman mana on skill use.
		if ((_skill.getReferenceItemId() > 0) && (ItemData.getInstance().getTemplate(_skill.getReferenceItemId()).getBodyPart() == BodyPart.DECO))
		{
			for (Item item : caster.getInventory().getItems())
			{
				if (item.isEquipped() && (item.getId() == _skill.getReferenceItemId()))
				{
					item.decreaseMana(false, item.useSkillDisTime());
					break;
				}
			}
		}
		
		if (target != caster)
		{
			// Face the target
			caster.setHeading(LocationUtil.calculateHeadingFrom(caster, target));
			caster.broadcastPacket(new ExRotation(caster.getObjectId(), caster.getHeading())); // TODO: Not sent in retail. Probably moveToPawn is enough
			
			// Send MoveToPawn packet to trigger Blue Bubbles on target become Red, but don't do it while (double) casting, because that will screw up animation... some fucked up stuff, right?
			if (caster.isPlayer() && !caster.isCastingNow() && target.isCreature())
			{
				caster.sendPacket(new MoveToPawn(caster, target, (int) caster.calculateDistance2D(target)));
				caster.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
		
		// Stop effects since we started casting (except for skills without action). It should be sent before casting bar and mana consume.
		if (!_skill.isWithoutAction())
		{
			caster.stopEffectsOnAction();
		}
		
		// Consume skill initial MP needed for cast. Retail sends it regardless if > 0 or not.
		final int initmpcons = caster.getStat().getMpInitialConsume(_skill);
		if (initmpcons > 0)
		{
			if (initmpcons > caster.getCurrentMp())
			{
				caster.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
				return false;
			}
			
			caster.getStatus().reduceMp(initmpcons);
			final StatusUpdate su = new StatusUpdate(caster);
			su.addUpdate(StatusUpdateType.CUR_MP, (int) caster.getCurrentMp());
			caster.sendPacket(su);
		}
		
		// Send a packet starting the casting.
		final int actionId = caster.isSummon() ? ActionData.getInstance().getSkillActionId(_skill.getId()) : -1;
		if (!_skill.isNotBroadcastable())
		{
			caster.broadcastSkillPacket(new MagicSkillUse(caster, target, _skill.getDisplayId(), _skill.getDisplayLevel(), displayedCastTime, reuseDelay, _skill.getReuseDelayGroup(), actionId, _castingType), target);
			if (caster.isPlayer() && (_skill.getTargetType() == TargetType.GROUND) && (_skill.getAffectScope() == AffectScope.FAN_PB))
			{
				final Player player = caster.asPlayer();
				final Location worldPosition = player.getCurrentSkillWorldPosition();
				if (worldPosition != null)
				{
					final Location location = new Location(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), worldPosition.getHeading());
					ThreadPool.schedule(() -> player.broadcastPacket(new ExMagicSkillUseGround(player.getObjectId(), _skill.getDisplayId(), location)), 100);
				}
			}
		}
		
		if (caster.isPlayer() && !instantCast)
		{
			// Send a system message to the player.
			if (!_skill.isHidingMessages())
			{
				caster.sendPacket(_skill.getId() != 2046 ? new SystemMessage(SystemMessageId.YOU_HAVE_USED_S1).addSkillName(_skill) : new SystemMessage(SystemMessageId.SUMMONING_YOUR_PET));
			}
			
			// Show the gauge bar for casting.
			caster.sendPacket(new SetupGauge(caster.getObjectId(), SetupGauge.BLUE, displayedCastTime));
		}
		
		// Consume reagent item.
		if ((_skill.getItemConsumeId() > 0) && (_skill.getItemConsumeCount() > 0) && (caster.getInventory() != null))
		{
			// Get the Item consumed by the spell.
			final Item requiredItem = caster.getInventory().getItemByItemId(_skill.getItemConsumeId());
			if (_skill.hasNegativeEffect() || (requiredItem.getTemplate().getDefaultAction() == ActionType.NONE)) // Non reagent items are removed at finishSkill or item handler.
			{
				caster.destroyItem(ItemProcessType.NONE, requiredItem.getObjectId(), _skill.getItemConsumeCount(), caster, false);
			}
		}
		
		if (caster.isPlayer())
		{
			final Player player = caster.asPlayer();
			
			// Consume fame points.
			if (_skill.getFamePointConsume() > 0)
			{
				if (player.getFame() < _skill.getFamePointConsume())
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_REPUTATION_POINTS);
					return false;
				}
				
				player.setFame(player.getFame() - _skill.getFamePointConsume());
				
				final SystemMessage msg = new SystemMessage(SystemMessageId.S1_FAME_HAS_BEEN_CONSUMED);
				msg.addInt(_skill.getFamePointConsume());
				player.sendPacket(msg);
			}
			
			// Consume clan reputation points.
			if (_skill.getClanRepConsume() > 0)
			{
				final Clan clan = player.getClan();
				if ((clan == null) || (clan.getReputationScore() < _skill.getClanRepConsume()))
				{
					player.sendPacket(SystemMessageId.THE_CLAN_REPUTATION_IS_TOO_LOW);
					return false;
				}
				
				clan.takeReputationScore(_skill.getClanRepConsume());
				
				final SystemMessage msg = new SystemMessage(SystemMessageId.S1_CLAN_REPUTATION_POINTS_SPENT);
				msg.addInt(_skill.getClanRepConsume());
				player.sendPacket(msg);
			}
		}
		
		// Trigger any skill cast start effects.
		if (target.isCreature())
		{
			// Tempfix for the delayed TriggerSkillByDualRange effect skill.
			final List<AbstractEffect> effects = _skill.getEffects(EffectScope.GENERAL);
			if ((effects != null) && !effects.isEmpty())
			{
				for (AbstractEffect effect : effects)
				{
					if (effect.getEffectType() == EffectType.DUAL_RANGE)
					{
						effect.instant(caster, target.asCreature(), _skill, null);
						return false;
					}
				}
			}
			
			_skill.applyEffectScope(EffectScope.START, new BuffInfo(caster, target.asCreature(), _skill, false, _item, null), true, false);
		}
		
		// Start channeling if skill is channeling.
		if (_skill.isChanneling())
		{
			caster.getSkillChannelizer().startChanneling(_skill);
		}
		
		return true;
	}
	
	/**
	 * Executes the launch phase of a skill.<br>
	 * This method validates the distance between the caster and target.<br>
	 * It identifies all affected targets and handles flying types.<br>
	 * Finally, it broadcasts the skill animation to other players.
	 * @return {@code true} if the skill was launched successfully, or {@code false} otherwise.
	 */
	public boolean launchSkill()
	{
		final Creature caster = _caster;
		final WorldObject target = _target;
		
		if ((caster == null) || (target == null))
		{
			return false;
		}
		
		// The cast was allowed to start at the bonus range, so the launch has to reach that far as well.
		final int effectRange = _skill.getEffectRange();
		if ((effectRange > 0) && !LocationUtil.checkIfInRange(effectRange + (int) caster.getStat().getValue(Stat.MAGIC_ATTACK_RANGE, 0), caster, target, true))
		{
			if (caster.isPlayer())
			{
				caster.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_CANCELLED);
			}
			
			return false;
		}
		
		// Gather list of affected targets by this skill.
		_targets = _skill.getTargetsAffected(caster, target);
		
		// Finish flying by setting the target location after picking targets. Packet is sent before MagicSkillLaunched.
		if (_skill.isFlyType())
		{
			handleSkillFly(caster, target);
		}
		
		// Display animation of launching skill upon targets.
		if (!_skill.isNotBroadcastable())
		{
			caster.broadcastSkillPacket(new MagicSkillLaunched(caster, _skill.getDisplayId(), _skill.getDisplayLevel(), _castingType, _targets), _targets);
		}
		
		return true;
	}
	
	/**
	 * Completes the skill casting process and applies its effects.<br>
	 * This method checks for required resources like MP, HP, souls, or charges.<br>
	 * It consumes these resources and notifies listeners of the finished cast.<br>
	 * Finally, it triggers the skill logic and updates the caster's state.
	 * @return {@code true} if the skill was successfully completed; {@code false} otherwise.
	 */
	public boolean finishSkill()
	{
		final Creature caster = _caster;
		final WorldObject target = _target;
		
		if ((caster == null) || (target == null))
		{
			return false;
		}
		
		if (_targets == null)
		{
			_targets = Collections.singletonList(target);
		}
		
		final StatusUpdate su = new StatusUpdate(caster);
		
		// Consume the required MP or stop casting if not enough.
		final double mpConsume = _skill.getMpConsume() > 0 ? caster.getStat().getMpConsume(_skill) : 0;
		if (mpConsume > 0)
		{
			if (mpConsume > caster.getCurrentMp())
			{
				caster.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
				return false;
			}
			
			caster.getStatus().reduceMp(mpConsume);
			su.addUpdate(StatusUpdateType.CUR_MP, (int) caster.getCurrentMp());
		}
		
		// Consume the required HP or stop casting if not enough.
		final double consumeHp = _skill.getHpConsume();
		if (consumeHp > 0)
		{
			if (consumeHp >= caster.getCurrentHp())
			{
				caster.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
				return false;
			}
			
			caster.getStatus().reduceHp(consumeHp, caster, true);
			su.addUpdate(StatusUpdateType.CUR_HP, (int) caster.getCurrentHp());
		}
		
		// Send HP/MP consumption packet if any attribute is set.
		if (su.hasUpdates())
		{
			caster.sendPacket(su);
		}
		
		if (caster.isPlayer())
		{
			// Consume Souls if necessary.
			final int maxSoulConsumeCount = _skill.getMaxSoulConsumeCount();
			if ((maxSoulConsumeCount > 0) && !caster.asPlayer().decreaseSouls(maxSoulConsumeCount))
			{
				return false;
			}
			
			// Consume charges if necessary.
			final int chargeConsumeCount = _skill.getChargeConsumeCount();
			if ((chargeConsumeCount > 0) && !caster.asPlayer().decreaseCharges(chargeConsumeCount))
			{
				return false;
			}
		}
		
		// Consume skill reduced item on success.
		final int itemConsumeCount = _skill.getItemConsumeCount();
		if ((_item != null) && (_item.getTemplate().getDefaultAction() == ActionType.SKILL_REDUCE_ON_SKILL_SUCCESS) && (itemConsumeCount > 0) && (_skill.getItemConsumeCount() > 0) && !caster.destroyItem(ItemProcessType.NONE, _item.getObjectId(), itemConsumeCount, target, true))
		{
			return false;
		}
		
		// Notify skill is casted.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_SKILL_FINISH_CAST, caster))
		{
			if (caster.onCreatureSkillFinishCast == null)
			{
				caster.onCreatureSkillFinishCast = new OnCreatureSkillFinishCast();
			}
			
			caster.onCreatureSkillFinishCast.setCaster(caster);
			caster.onCreatureSkillFinishCast.setTarget(target);
			caster.onCreatureSkillFinishCast.setSkill(_skill);
			caster.onCreatureSkillFinishCast.setSimultaneously(_skill.isWithoutAction());
			EventDispatcher.getInstance().notifyEvent(caster.onCreatureSkillFinishCast, caster);
		}
		
		// Call the skill's effects and AI interraction and stuff.
		callSkill(caster, target, _targets, _skill, _item);
		
		// Start attack stance.
		if (!_skill.isWithoutAction() && _skill.hasNegativeEffect() && (_skill.getTargetType() != TargetType.DOOR_TREASURE))
		{
			caster.getAI().clientStartAutoAttack();
		}
		
		// Notify DP Scripts.
		caster.notifyQuestEventSkillFinished(_skill, target);
		
		// On each repeat recharge shots before cast.
		caster.rechargeShots(_skill.useSoulShot(), _skill.useSpiritShot(), false);
		
		// Reset current skill world position.
		if (caster.isPlayer() && (_skill.getTargetType() == TargetType.GROUND) && ((_skill.getAffectScope() == AffectScope.FAN_PB) || (_skill.getAffectScope() == AffectScope.FAN)))
		{
			caster.asPlayer().setCurrentSkillWorldPosition(null);
		}
		
		return true;
	}
	
	/**
	 * Executes the logic for a specific skill.<br>
	 * This method handles effect calculations, target filtering, and PvP status updates.<br>
	 * It also manages special interactions like raid boss protections and trigger skills.
	 * @param caster The creature performing the skill.
	 * @param target The primary object targeted by the skill.
	 * @param targets A collection of additional objects affected by the skill.
	 * @param skill The {@code Skill} being executed.
	 * @param item The {@code Item} used to cast the skill.
	 */
	public static void callSkill(Creature caster, WorldObject target, Collection<WorldObject> targets, Skill skill, Item item)
	{
		// Launch the magic skill in order to calculate its effects.
		try
		{
			// Disabled characters should not be able to finish negative effect skills.
			if (skill.hasNegativeEffect() && caster.isDisabled())
			{
				return;
			}
			
			// Check if the toggle skill effects are already in progress on the Creature.
			if (skill.isToggle() && caster.isAffectedBySkill(skill.getId()))
			{
				return;
			}
			
			if ((targets == null) || targets.isEmpty())
			{
				if (skill.hasEffects(EffectScope.SELF))
				{
					skill.activateSkill(caster, item, Collections.emptyList());
				}
				return;
			}
			
			final Collection<WorldObject> skillTargets = new ArrayList<>(targets);
			
			// Block RaidBoss from receiving heals/buffs from players and their pet/summon owner.
			if (GrandBossConfig.DISABLE_RAIDBOSS_HEAL_FROM_PLAYERS || GrandBossConfig.DISABLE_RAIDBOSS_BUFF_FROM_PLAYERS)
			{
				final Player actingPlayer = caster.isPlayer() ? caster.asPlayer() : (caster.isSummon() ? caster.asSummon().getOwner() : (caster.isPet() ? caster.asPet().getOwner() : null));
				if (actingPlayer != null)
				{
					final boolean isHealSkill = skill.hasEffectType(EffectType.HEAL, EffectType.CPHEAL, EffectType.MANAHEAL_BY_LEVEL, EffectType.MANAHEAL_PERCENT, EffectType.REBALANCE_HP);
					final boolean isBuffSkill = !isHealSkill && !skill.hasNegativeEffect() && (skill.isContinuous() || skill.getBuffType().isBuff() || skill.getBuffType().isDance() || skill.getBuffType().isToggle() || skill.hasEffectType(EffectType.NOBLESSE_BLESSING, EffectType.CHARM_OF_LUCK, EffectType.RELAXING));
					final boolean blockRaidBoss = (isHealSkill && GrandBossConfig.DISABLE_RAIDBOSS_HEAL_FROM_PLAYERS) || (isBuffSkill && GrandBossConfig.DISABLE_RAIDBOSS_BUFF_FROM_PLAYERS);
					
					if (blockRaidBoss)
					{
						skillTargets.removeIf(obj -> obj.isCreature() && obj.asCreature().isRaid());
						if (skillTargets.isEmpty())
						{
							return;
						}
					}
				}
			}
			
			// Initial checks
			for (WorldObject obj : skillTargets)
			{
				if ((obj == null) || !obj.isCreature())
				{
					continue;
				}
				
				final Creature creature = obj.asCreature();
				
				// Check raid monster/minion attack and check buffing characters who attack raid monsters. Raid is still affected by skills.
				if (!NpcConfig.RAID_DISABLE_CURSE && creature.isRaid() && creature.giveRaidCurse() && (caster.getLevel() >= (creature.getLevel() + 9)) && (skill.hasNegativeEffect() || ((creature.getTarget() == caster) && creature.asAttackable().getAggroList().containsKey(caster))))
				{
					// Skills such as Summon Battle Scar too can trigger magic silence.
					final CommonSkill curse = skill.hasNegativeEffect() ? CommonSkill.RAID_CURSE2 : CommonSkill.RAID_CURSE;
					final Skill curseSkill = curse.getSkill();
					if (curseSkill != null)
					{
						curseSkill.applyEffects(creature, caster);
					}
				}
				
				// Static skills not trigger any chance skills.
				if (!skill.isStatic())
				{
					final Weapon activeWeapon = caster.getActiveWeaponItem();
					
					// Launch weapon Special ability skill effect if available.
					if ((activeWeapon != null) && !creature.isDead())
					{
						activeWeapon.applyConditionalSkills(caster, creature, skill, ItemSkillType.ON_MAGIC_SKILL);
					}
					
					if (caster.hasTriggerSkills())
					{
						for (OptionSkillHolder holder : caster.getTriggerSkills().values())
						{
							if (((skill.isMagic() && (holder.getSkillType() == OptionSkillType.MAGIC)) || (skill.isPhysical() && (holder.getSkillType() == OptionSkillType.ATTACK))) && (Rnd.get(100) < holder.getChance()))
							{
								triggerCast(caster, creature, holder.getSkill(), null, false);
							}
						}
					}
				}
			}
			
			// A sleep, hold or paralysis cannot land again on a flagged player already under it, such a cast does not flag the caster.
			List<WorldObject> alreadyDisabled = null;
			if (DISABLE_TYPES.contains(skill.getAbnormalType()))
			{
				for (WorldObject obj : skillTargets)
				{
					if (obj.isPlayer() && (obj.asPlayer().getPvpFlag() > 0) && obj.asPlayer().getEffectList().hasAbnormalType(skill.getAbnormalType()))
					{
						if (alreadyDisabled == null)
						{
							alreadyDisabled = new ArrayList<>();
						}
						
						alreadyDisabled.add(obj);
					}
				}
			}
			
			// Launch the magic skill and calculate its effects.
			skill.activateSkill(caster, item, skillTargets);
			
			// Flag on Raid/Champion through offensive skills (player or owner of summon/pet).
			if ((skill.getEffectPoint() <= 0) && skill.hasNegativeEffect() && (PvpConfig.FLAG_PLAYER_ON_RAID_ATTACK || PvpConfig.FLAG_PLAYER_ON_CHAMPION_ATTACK))
			{
				final Player actingPlayer;
				if (caster.isPlayer())
				{
					actingPlayer = caster.asPlayer();
				}
				else if (PvpConfig.FLAG_PLAYER_ON_SUMMON_PET_RAID_CHAMPION_ATTACK && (caster.isSummon() || caster.isPet()))
				{
					actingPlayer = caster.isSummon() ? caster.asSummon().getOwner() : caster.asPet().getOwner();
				}
				else
				{
					actingPlayer = null;
				}
				
				if (actingPlayer != null)
				{
					for (WorldObject skillTarget : skillTargets)
					{
						if (skillTarget.isCreature())
						{
							final Creature targetCreature = skillTarget.asCreature();
							if (((targetCreature.isRaid() && PvpConfig.FLAG_PLAYER_ON_RAID_ATTACK) || (targetCreature.isChampion() && PvpConfig.FLAG_PLAYER_ON_CHAMPION_ATTACK)))
							{
								actingPlayer.updatePvPStatus();
								break;
							}
						}
					}
				}
			}
			
			final Player player = caster.asPlayer();
			if (player != null)
			{
				for (WorldObject obj : skillTargets)
				{
					if (!obj.isCreature())
					{
						continue;
					}
					
					if (skill.hasNegativeEffect())
					{
						if (obj.isPlayable())
						{
							// Update pvpflag.
							if ((alreadyDisabled == null) || !alreadyDisabled.contains(obj))
							{
								player.updatePvPStatus(obj.asCreature());
							}
							
							if (obj.isSummon())
							{
								obj.asSummon().updateAndBroadcastStatus(1);
							}
						}
						else if (obj.isAttackable())
						{
							// Add hate to the attackable, and put it in the attack list.
							obj.asAttackable().addDamageHate(caster, 0, -skill.getEffectPoint());
							obj.asCreature().addAttackerToAttackByList(caster);
							
							// Summoning a servitor should not renew your own PvP flag time.
							if (obj.isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE && (!obj.isServitor() || (obj.getObjectId() != player.getFirstServitor().getObjectId())))
							{
								player.updatePvPStatus();
							}
						}
						
						// Notify target AI about the attack.
						if (obj.asCreature().hasAI() && !skill.hasEffectType(EffectType.HATE))
						{
							obj.asCreature().getAI().notifyActionAttacked(caster);
						}
					}
					// Self casting should not increase PvP time.
					else if ((obj != player) && (!obj.isSummon() || ((obj.asSummon()).getOwner() != player)))
					{
						// Supporting monsters or players results in pvpflag.
						if (((skill.getEffectPoint() > 0) && obj.isMonster()) //
							|| (obj.isPlayable() && (((obj.asPlayer().getPvpFlag() > 0) && !(PvpConfig.SUPPORT_FLAGGED_ALLY_NO_FLAG && ((player.isInParty() && player.getParty().containsPlayer(obj.asPlayer())) || ((player.getClanId() > 0) && (player.getClanId() == obj.asPlayer().getClanId())) || player.isInSiege()))) //
								|| (obj.asCreature().getReputation() < 0) //
							)))
						{
							// Consider fake player PvP status.
							if (!obj.isFakePlayer() //
								|| (obj.isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE && (!obj.asNpc().isScriptValue(0) || (obj.asNpc().getReputation() < 0))))
							{
								player.updatePvPStatus();
							}
						}
					}
				}
				
				// Mobs in range 1000 see spell.
				World.forEachVisibleObjectInRange(player, Npc.class, 1000, npc ->
				{
					if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SKILL_SEE, npc))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillSee(npc, player, skill, caster.isSummon(), skillTargets), npc);
					}
					
					// On Skill See logic.
					if (npc.isAttackable() && !npc.isFakePlayer())
					{
						final Attackable attackable = npc.asAttackable();
						if ((skill.getEffectPoint() > 0) && attackable.hasAI() && (attackable.getAI().getIntention() == Intention.ATTACK))
						{
							final WorldObject npcTarget = attackable.getTarget();
							for (WorldObject skillTarget : skillTargets)
							{
								if ((npcTarget == skillTarget) || (npc == skillTarget))
								{
									final Creature originalCaster = caster.isSummon() ? caster : player;
									attackable.addDamageHate(originalCaster, 0, (long) (((skill.getEffectPoint() * 150) / (attackable.getLevel() + 7)) * attackable.getHateRatio(originalCaster)));
								}
							}
						}
						
						// Players which are 9 levels above a Raid Boss and cast a skill nearby, are silenced with the Raid Curse skill.
						// Static skills (recipe books, potions and other item skills) are not casting.
						if (!NpcConfig.RAID_DISABLE_CURSE && !skill.isStatic() && attackable.isRaid() && attackable.giveRaidCurse() && attackable.isInCombat() && ((player.getLevel() - attackable.getLevel()) > 8))
						{
							final CommonSkill curse = skill.hasNegativeEffect() ? CommonSkill.RAID_CURSE2 : CommonSkill.RAID_CURSE;
							curse.getSkill().applyEffects(attackable, player);
						}
					}
				});
			}
			else if (caster.isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE) // Fake player attacks player.
			{
				if (target.isPlayable() || target.isFakePlayer())
				{
					final Npc npc = caster.asNpc();
					if (!npc.isScriptValue(1))
					{
						npc.setScriptValue(1); // in combat
						npc.broadcastInfo(); // update flag status
						ScriptManager.getInstance().getScript("PvpFlaggingStopTask").notifyEvent("FLAG_CHECK", npc, null);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, caster + " callSkill() failed.", e);
		}
	}
	
	/**
	 * Stops the current skill casting process.<br>
	 * This method cancels any active tasks and removes the casting state from the caster.<br>
	 * If {@code aborted} is {@code true}, it broadcasts a cancellation packet to the client.<br>
	 * It also handles subsequent actions like launching queued skills or initiating attacks.
	 * @param aborted Determines whether the cast was interrupted or completed successfully.
	 */
	public void stopCasting(boolean aborted)
	{
		// Cancel the task and unset it.
		if (_task != null)
		{
			_task.cancel(false);
			_task = null;
		}
		
		final Creature caster = _caster;
		final WorldObject target = _target;
		if (caster == null)
		{
			return;
		}
		
		caster.removeSkillCaster(_castingType);
		
		if (caster.isChanneling())
		{
			caster.getSkillChannelizer().stopChanneling();
		}
		
		// If aborted, broadcast casting aborted.
		if (aborted)
		{
			caster.broadcastPacket(new MagicSkillCanceled(caster.getObjectId())); // Broadcast packet to stop animations client-side.
			caster.sendPacket(ActionFailed.get(_castingType)); // Send an "action failed" packet to the caster.
		}
		
		// If there is a queued skill, launch it and wipe the queue.
		if (caster.isPlayer())
		{
			final Player currPlayer = caster.asPlayer();
			final SkillUseHolder queuedSkill = currPlayer.getQueuedSkill();
			if (queuedSkill != null)
			{
				ThreadPool.execute(() ->
				{
					currPlayer.setQueuedSkill(null, null, false, false);
					currPlayer.useMagic(queuedSkill.getSkill(), queuedSkill.getItem(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed());
				});
				
				return;
			}
		}
		
		// Attack target after skill use.
		final CreatureAI casterAI = caster.getAI();
		final NextActionType nextAction = _skill.getNextAction();
		if ((nextAction != NextActionType.NONE) && (casterAI.getNextIntention() == null))
		{
			if ((nextAction == NextActionType.ATTACK) && (target != null) && (target != caster) && target.isAutoAttackable(caster) && !_shiftPressed)
			{
				if (!caster.isPlayer() || !caster.asPlayer().isAutoPlaying())
				{
					casterAI.setIntentionAttack(target);
				}
				else // Auto play handles the attacking.
				{
					casterAI.notifyActionFinishCasting();
				}
			}
			else if ((nextAction == NextActionType.CAST) && (target != null) && (target != caster) && target.isAutoAttackable(caster))
			{
				casterAI.setIntentionCast(_skill, target, _item, false, false);
			}
			else
			{
				casterAI.notifyActionFinishCasting();
			}
		}
		else
		{
			casterAI.notifyActionFinishCasting();
		}
		
		// The rotation continues only once the next action above is settled, so it never races the caster AI.
		if (caster.isPlayer())
		{
			AutoUseTaskManager.getInstance().onAutoSkillCastFinished(caster.asPlayer(), aborted);
		}
	}
	
	/**
	 * Calculates the timing values for a skill cast.<br>
	 * This method determines the hit time, cancel time, and cooldown based on creature stats.<br>
	 * It handles different logic for channeling skills versus standard skills.
	 * @param creature The {@code Creature} performing the action.
	 * @param skill The {@code Skill} being used.
	 * @param castTime The base cast time of the skill.
	 */
	private void calcSkillTiming(Creature creature, Skill skill, int castTime)
	{
		final double timeFactor = Formulas.calcSkillTimeFactor(creature, skill);
		final double cancelTime = Formulas.calcSkillCancelTime(creature, skill);
		if (skill.getOperateType().isChanneling())
		{
			_hitTime = (int) Math.max(skill.getHitTime() - cancelTime, 0);
			_cancelTime = 2866;
		}
		else
		{
			if (castTime > -1)
			{
				_hitTime = (int) Math.max((castTime - cancelTime) / timeFactor, 0);
			}
			else
			{
				_hitTime = (int) Math.max((skill.getHitTime() - cancelTime) / timeFactor, 0);
			}
			
			_cancelTime = (int) cancelTime;
		}
		
		_coolTime = (int) (skill.getCoolTime() / timeFactor); // cooltimeMillis / timeFactor
	}
	
	/**
	 * Starts the casting process for a specific {@code Skill}.<br>
	 * This method initiates the skill execution from one {@code Creature} to another.
	 * @param creature The {@code Creature} who is performing the action.
	 * @param target The {@code Creature} who is receiving the action.
	 * @param skill The {@code Skill} being cast.
	 */
	public static void triggerCast(Creature creature, Creature target, Skill skill)
	{
		triggerCast(creature, target, skill, null, true);
	}
	
	/**
	 * Triggers a cast action for a specific creature.<br>
	 * This method adds the casting information to either the {@code target} or the {@code creature}.<br>
	 * It checks if the {@code target} is {@code null} to decide which entity receives the trigger.
	 * @param creature The creature performing the cast action.
	 * @param target The object being targeted by the skill.
	 * @param skill The {@code Skill} being used for the cast.
	 * @param item The {@code Item} associated with the skill.
	 * @param ignoreTargetType A boolean flag indicating whether to ignore the target type.
	 */
	public static void triggerCast(Creature creature, WorldObject target, Skill skill, Item item, boolean ignoreTargetType)
	{
		if (creature == null)
		{
			return;
		}
		
		if (target == null)
		{
			creature.addTriggerCast(new TriggerCastInfo(creature, target, skill, item, ignoreTargetType));
		}
		else
		{
			target.addTriggerCast(new TriggerCastInfo(creature, target, skill, item, ignoreTargetType));
		}
	}
	
	/**
	 * Triggers the execution of a skill based on the provided information.<br>
	 * This method validates conditions and handles target selection before activating the skill.<br>
	 * It also manages reuse delays, broadcasts packets, and notifies event listeners.
	 * @param info The {@code TriggerCastInfo} object containing all necessary data for the cast.
	 */
	public static void triggerCast(TriggerCastInfo info)
	{
		final Creature creature = info.getCreature();
		final WorldObject target = info.getTarget();
		final Skill skill = info.getSkill();
		final Item item = info.getItem();
		final boolean ignoreTargetType = info.isIgnoreTargetType();
		
		try
		{
			if ((creature == null) || (skill == null))
			{
				return;
			}
			
			if (skill.checkCondition(creature, target, true))
			{
				if (creature.isSkillDisabled(skill))
				{
					return;
				}
				
				final int reuseDelay = skill.getReuseDelay();
				if (reuseDelay > 0)
				{
					creature.disableSkill(skill, reuseDelay);
				}
				
				WorldObject currentTarget = target;
				if (!ignoreTargetType)
				{
					final WorldObject objTarget = skill.getTarget(creature, false, false, false);
					
					// Avoid triggering skills on invalid targets.
					if (objTarget == null)
					{
						return;
					}
					
					if (objTarget.isCreature())
					{
						currentTarget = objTarget;
					}
				}
				
				final List<WorldObject> targets = skill.getTargetsAffected(creature, currentTarget);
				if (!skill.isNotBroadcastable() && !creature.isChanneling())
				{
					if ((targets.size() > 1) && skill.hasNegativeEffect())
					{
						// The client draws an instant skill's hit only on the MagicSkillUse target and ignores the MagicSkillLaunched list, so a hostile area trigger needs one packet per affected target.
						for (WorldObject affected : targets)
						{
							creature.broadcastSkillPacket(new MagicSkillUse(creature, affected, skill.getDisplayId(), skill.getLevel(), 0, 0), targets);
						}
					}
					else
					{
						creature.broadcastSkillPacket(new MagicSkillUse(creature, currentTarget, skill.getDisplayId(), skill.getLevel(), 0, 0), targets);
					}
				}
				
				// Triggered casts keep the caster's shots charged.
				// Otherwise only the first trigger gets the shot bonus and the next real cast starts without a shot.
				final boolean blessedSpiritshot = creature.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
				final boolean spiritshot = creature.isChargedShot(ShotType.SPIRITSHOTS);
				final boolean blessedSoulshot = creature.isChargedShot(ShotType.BLESSED_SOULSHOTS);
				final boolean soulshot = creature.isChargedShot(ShotType.SOULSHOTS);
				
				// Launch the magic skill and calculate its effects.
				skill.activateSkill(creature, item, targets);
				
				if (blessedSpiritshot)
				{
					creature.chargeShot(ShotType.BLESSED_SPIRITSHOTS);
				}
				
				if (spiritshot)
				{
					creature.chargeShot(ShotType.SPIRITSHOTS);
				}
				
				if (blessedSoulshot)
				{
					creature.chargeShot(ShotType.BLESSED_SOULSHOTS);
				}
				
				if (soulshot)
				{
					creature.chargeShot(ShotType.SOULSHOTS);
				}
				
				// Notify skill is casted.
				if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_SKILL_FINISH_CAST, creature))
				{
					if (creature.onCreatureSkillFinishCast == null)
					{
						creature.onCreatureSkillFinishCast = new OnCreatureSkillFinishCast();
					}
					
					creature.onCreatureSkillFinishCast.setCaster(creature);
					creature.onCreatureSkillFinishCast.setTarget(target);
					creature.onCreatureSkillFinishCast.setSkill(skill);
					creature.onCreatureSkillFinishCast.setSimultaneously(skill.isWithoutAction());
					EventDispatcher.getInstance().notifyEvent(creature.onCreatureSkillFinishCast, creature);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed simultaneous cast: ", e);
		}
	}
	
	/**
	 * Retrieves the {@code Skill} associated with this caster.
	 * @return the current {@code Skill} object.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Retrieves the {@code Creature} that is currently casting the skill.
	 * @return the {@code Creature} performing the cast
	 */
	public Creature getCaster()
	{
		return _caster;
	}
	
	/**
	 * Retrieves the current target of the skill.
	 * @return the {@code WorldObject} being targeted by this skill.
	 */
	public WorldObject getTarget()
	{
		return _target;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this skill cast.
	 * @return the {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Checks if the current skill casting can be cancelled.<br>
	 * If a target has already been assigned, the skill is considered launched and cannot be aborted.
	 * @return {@code true} if the cast can still be aborted, {@code false} otherwise.
	 */
	public boolean canAbortCast()
	{
		return getCaster().getTarget() == null; // When targets are allocated, that means skill is already launched, therefore cannot be aborted.
	}
	
	/**
	 * Retrieves the current casting type of the skill.
	 * @return the {@code SkillCastingType} associated with this cast.
	 */
	public SkillCastingType getCastingType()
	{
		return _castingType;
	}
	
	/**
	 * Checks if the current casting type is normal.
	 * @return {@code true} if the type is {@code SkillCastingType.NORMAL}, otherwise {@code false}.
	 */
	public boolean isNormalFirstType()
	{
		return _castingType == SkillCastingType.NORMAL;
	}
	
	/**
	 * Checks if the current casting type is set to {@code NORMAL_SECOND}.
	 * @return {@code true} if the type matches, otherwise {@code false}.
	 */
	public boolean isNormalSecondType()
	{
		return _castingType == SkillCastingType.NORMAL_SECOND;
	}
	
	/**
	 * Checks if the current casting type is a normal variation.<br>
	 * This returns {@code true} for both {@code NORMAL} and {@code NORMAL_SECOND} types.
	 * @return {@code true} if the skill is cast normally, {@code false} otherwise.
	 */
	public boolean isAnyNormalType()
	{
		return (_castingType == SkillCastingType.NORMAL) || (_castingType == SkillCastingType.NORMAL_SECOND);
	}
	
	/**
	 * Returns a string representation of the {@code SkillCaster} object.<br>
	 * This includes details about the caster, skill, target, and casting type.
	 * @return A string containing the object's data.
	 */
	@Override
	public String toString()
	{
		return super.toString() + " [caster: " + _caster + " skill: " + _skill + " target: " + _target + " type: " + _castingType + "]";
	}
	
	/**
	 * Checks if the {@code caster} meets all requirements to use a specific {@code skill}.<br>
	 * This method validates conditions like cooldowns and other restrictions.
	 * @param caster The {@code Creature} attempting to perform the action.
	 * @param skill The {@code Skill} being checked for usage.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	public static boolean checkUseConditions(Creature caster, Skill skill)
	{
		return checkUseConditions(caster, skill, SkillCastingType.NORMAL);
	}
	
	/**
	 * Validates if a {@code Creature} can use a specific {@code Skill}.<br>
	 * This method checks various requirements including MP, HP, and item consumption.<br>
	 * It also verifies status effects like mutes, weapon restrictions, and zone limitations.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being attempted.
	 * @param castingType The {@code SkillCastingType} of the action.
	 * @return {@code true} if all conditions are met, {@code false} otherwise.
	 */
	public static boolean checkUseConditions(Creature caster, Skill skill, SkillCastingType castingType)
	{
		if (caster == null)
		{
			return false;
		}
		
		if ((skill == null) || caster.isSkillDisabled(skill) || (skill.isFlyType() && caster.isMovementDisabled()))
		{
			caster.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_SKILL_USE, caster))
		{
			if (caster.onCreatureSkillUse == null)
			{
				caster.onCreatureSkillUse = new OnCreatureSkillUse();
			}
			
			caster.onCreatureSkillUse.setCaster(caster);
			caster.onCreatureSkillUse.setSkill(skill);
			caster.onCreatureSkillUse.setSimultaneously(skill.isWithoutAction());
			final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(caster.onCreatureSkillUse, caster, TerminateReturn.class);
			if ((term != null) && term.terminate())
			{
				caster.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Check if creature is already casting.
		if ((castingType != null) && caster.isCastingNow(castingType))
		{
			caster.sendPacket(ActionFailed.get(castingType));
			return false;
		}
		
		// Check if the caster has enough MP.
		if (caster.getCurrentMp() < (caster.getStat().getMpConsume(skill) + caster.getStat().getMpInitialConsume(skill)))
		{
			caster.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
			caster.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster has enough HP.
		if (caster.getCurrentHp() <= skill.getHpConsume())
		{
			caster.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
			caster.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Skill mute checks.
		if (!skill.isStatic())
		{
			// Check if the skill is a magic spell and if the Creature is not muted.
			if (skill.isMagic())
			{
				if (caster.isMuted())
				{
					caster.sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else if (caster.isPhysicalMuted()) // Check if the skill is physical and if the Creature is not physical_muted.
			{
				caster.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Check if the caster's weapon is limited to use only its own skills.
		final Weapon weapon = caster.getActiveWeaponItem();
		if ((weapon != null) && weapon.useWeaponSkillsOnly() && !caster.isGM())
		{
			final List<ItemSkillHolder> weaponSkills = weapon.getSkills(ItemSkillType.NORMAL);
			if (weaponSkills != null)
			{
				boolean hasSkill = false;
				for (ItemSkillHolder holder : weaponSkills)
				{
					if (holder.getSkillId() == skill.getId())
					{
						hasSkill = true;
						break;
					}
				}
				
				if (!hasSkill)
				{
					caster.sendPacket(SystemMessageId.THAT_WEAPON_CANNOT_USE_ANY_OTHER_SKILL_EXCEPT_THE_WEAPON_S_SKILL);
					return false;
				}
			}
		}
		
		// Check if a spell consumes an item.
		if ((skill.getItemConsumeId() > 0) && (skill.getItemConsumeCount() > 0) && (caster.getInventory() != null))
		{
			// Get the Item consumed by the spell.
			final Item requiredItem = caster.getInventory().getItemByItemId(skill.getItemConsumeId());
			if ((requiredItem == null) || (requiredItem.getCount() < skill.getItemConsumeCount()))
			{
				if (skill.hasEffectType(EffectType.SUMMON))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.SUMMONING_A_SERVITOR_COSTS_S2_S1);
					sm.addItemName(skill.getItemConsumeId());
					sm.addInt(skill.getItemConsumeCount());
					caster.sendPacket(sm);
				}
				else
				{
					caster.sendPacket(new SystemMessage(SystemMessageId.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL));
				}
				
				return false;
			}
		}
		
		if (caster.isPlayer())
		{
			final Player player = caster.asPlayer();
			if (player.getObserverMode().isActive())
			{
				return false;
			}
			
			if (player.isInOlympiadMode() && skill.isBlockedInOlympiad())
			{
				player.sendPacket(SystemMessageId.THE_SKILL_CANNOT_BE_USED_IN_THE_OLYMPIAD);
				return false;
			}
			
			if (player.isInsideZone(ZoneId.SAYUNE))
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_USE_SKILLS_IN_THE_CORRESPONDING_REGION);
				return false;
			}
			
			// Check if not in AirShip.
			if (player.isInAirShip() && !skill.hasEffectType(EffectType.REFUEL_AIRSHIP))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
				sm.addSkillName(skill);
				player.sendPacket(sm);
				return false;
			}
			
			if (player.getFame() < skill.getFamePointConsume())
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_REPUTATION_POINTS);
				return false;
			}
			
			// Consume clan reputation points.
			if (skill.getClanRepConsume() > 0)
			{
				final Clan clan = player.getClan();
				if ((clan == null) || (clan.getReputationScore() < skill.getClanRepConsume()))
				{
					player.sendPacket(SystemMessageId.THE_CLAN_REPUTATION_IS_TOO_LOW);
					return false;
				}
			}
			
			// Check for skill reuse (fixes macro right click press exploit).
			if (caster.hasSkillReuse(skill.getReuseHashCode()))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_NOT_AVAILABLE_AT_THIS_TIME_BEING_PREPARED_FOR_REUSE);
				sm.addSkillName(skill);
				caster.sendPacket(sm);
				return false;
			}
			
			// Events.
			if (player.isOnEvent())
			{
				if (skill.hasEffectType(EffectType.TELEPORT)) // Disable teleport skills.
				{
					player.sendMessage("You cannot use " + skill.getName() + " while attending an event.");
					return false;
				}
				
				// Commented because AOE skills like Warlord's [Thunder Storm] (44) and debuffs like Overlord's [Seal of Binding] (1208) do not work when targeting an enemy from the other team.
				// if (skill.hasNegativeEffect() && !player.isOnSoloEvent())
				// {
				// final WorldObject target = player.getTarget();
				// if ((target != null) && target.isPlayable() && (player.getTeam() == target.asPlayer().getTeam()))
				// {
				// return false;
				// }
				// }
			}
		}
		
		return true;
	}
	
	/**
	 * Handles the movement logic for skills that cause a creature to fly or teleport.<br>
	 * It calculates the destination coordinates based on specific skill types like DA1 through DA5.<br>
	 * The method updates the {@code Creature} position and broadcasts the flight packet to clients.
	 * @param creature The {@code Creature} performing the action.
	 * @param target The {@code WorldObject} that serves as the reference for the movement calculation.
	 */
	private void handleSkillFly(Creature creature, WorldObject target)
	{
		int x = 0;
		int y = 0;
		int z = 0;
		FlyType flyType = FlyType.CHARGE;
		switch (_skill.getOperateType())
		{
			case DA1:
			case DA2:
			{
				if (creature == target)
				{
					final double course = Math.toRadians(180);
					final double radian = Math.toRadians(LocationUtil.convertHeadingToDegree(creature.getHeading()));
					x = target.getX() + (int) (Math.cos(Math.PI + radian + course) * _skill.getCastRange());
					y = target.getY() + (int) (Math.sin(Math.PI + radian + course) * _skill.getCastRange());
					z = target.getZ();
				}
				else
				{
					x = target.getX();
					y = target.getY();
					z = target.getZ();
				}
				break;
			}
			case DA3:
			{
				flyType = FlyType.WARP_BACK;
				final double radian = Math.toRadians(LocationUtil.convertHeadingToDegree(creature.getHeading()));
				x = creature.getX() + (int) (Math.cos(Math.PI + radian) * _skill.getCastRange());
				y = creature.getY() + (int) (Math.sin(Math.PI + radian) * _skill.getCastRange());
				z = creature.getZ();
				break;
			}
			case DA4:
			case DA5:
			{
				final double course = _skill.getOperateType() == SkillOperateType.DA4 ? Math.toRadians(270) : Math.toRadians(90);
				final double radian = Math.toRadians(LocationUtil.convertHeadingToDegree(target.getHeading()));
				double nRadius = creature.getCollisionRadius();
				if (target.isCreature())
				{
					nRadius += target.asCreature().getCollisionRadius();
				}
				
				x = target.getX() + (int) (Math.cos(Math.PI + radian + course) * nRadius);
				y = target.getY() + (int) (Math.sin(Math.PI + radian + course) * nRadius);
				z = target.getZ();
				break;
			}
			case DA6:
			{
				final int creatureX = creature.getX();
				final int creatureY = creature.getY();
				final int dx = target.getX() - creatureX;
				final int dy = target.getY() - creatureY;
				final double distance = Math.sqrt((dx * dx) + (dy * dy));
				if (distance > 1)
				{
					final double range = _skill.getCastRange();
					final double ratio = range / distance;
					x = creatureX + (int) (dx * ratio);
					y = creatureY + (int) (dy * ratio);
					z = creature.getZ();
				}
				else
				{
					x = creatureX;
					y = creatureY;
					z = creature.getZ();
				}
				break;
			}
		}
		
		final Location destination = creature.isFlying() ? new Location(x, y, z) : GeoEngine.getInstance().getValidLocation(creature.getX(), creature.getY(), creature.getZ(), x, y, z, creature.getInstanceWorld());
		
		creature.getAI().setIntentionIdle();
		creature.broadcastPacket(new FlyToLocation(creature, destination, flyType, 0, 0, 333));
		creature.setXYZ(destination);
		creature.revalidateZone(true);
	}
}
