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
package org.l2jmobius.gameserver.entity.actor.status;

import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.xml.NpcNameLocalisationData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.actor.holders.player.Duel;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerCheatDeath;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Represents the status and state information for a {@code Player} entity.<br>
 * This class handles player-specific mechanics such as stats, buffs, and active states.
 */
public class PlayerStatus extends PlayableStatus
{
	private double _currentCp = 0; // Current CP of the Player
	
	/**
	 * Creates a new {@code PlayerStatus} instance for a specific player.<br>
	 * This constructor initializes the status using the provided {@code Player} object.
	 * @param player The {@code Player} to associate with this status.
	 */
	public PlayerStatus(Player player)
	{
		super(player);
	}
	
	/**
	 * Reduces the current CP of the player.<br>
	 * If the reduction amount exceeds the current CP, it sets the CP to 0.
	 * @param value The amount of CP to subtract.
	 */
	@Override
	public void reduceCp(int value)
	{
		if (_currentCp > value)
		{
			setCurrentCp(_currentCp - value);
		}
		else
		{
			setCurrentCp(0);
		}
	}
	
	/**
	 * Reduces the current health of the player.<br>
	 * This method handles basic damage calculation and updates the status.
	 * @param value The amount of health to subtract.
	 * @param attacker The {@code Creature} that dealt the damage.
	 */
	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, null, true, false, false, false);
	}
	
	/**
	 * Reduces the health points of the entity.<br>
	 * This method handles various damage types and states.
	 * @param value The amount of health to subtract.
	 * @param attacker The {@code Creature} that caused the damage.
	 * @param awake Whether the target is currently awake.
	 * @param isDOT Whether the damage comes from a Damage Over Time effect.
	 * @param isHPConsumption Whether the reduction is due to HP consumption.
	 */
	@Override
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption)
	{
		reduceHp(value, attacker, null, awake, isDOT, isHPConsumption, false);
	}
	
	/**
	 * Reduces the current HP of the player based on the provided value.<br>
	 * This method handles complex logic including damage transfer to summons, mana shields, and party members.<br>
	 * It also manages status effects like stun breaks, standing up from sitting, and CP consumption.<br>
	 * If the player's HP reaches zero, it triggers death mechanics or duel defeat conditions.
	 * @param value The amount of damage to subtract from the current HP.
	 * @param attacker The {@code Creature} that initiated the attack.
	 * @param skill The {@code Skill} used to perform the attack.
	 * @param awake Whether the player is currently in an awake state.
	 * @param isDOT Whether the damage is from a Damage Over Time effect.
	 * @param isHPConsumption Whether the HP reduction is for consumption purposes.
	 * @param ignoreCP Whether to bypass the CP (Combat Points) protection system.
	 */
	public void reduceHp(double value, Creature attacker, Skill skill, boolean awake, boolean isDOT, boolean isHPConsumption, boolean ignoreCP)
	{
		final Player player = getActiveChar();
		if (player.isDead())
		{
			return;
		}
		
		// If OFFLINE_MODE_NO_DAMAGE is enabled and player is offline and he is in store/craft mode, no damage is taken.
		if (OfflineTradeConfig.OFFLINE_MODE_NO_DAMAGE && (player.getClient() != null) && player.getClient().isDetached() && ((OfflineTradeConfig.OFFLINE_TRADE_ENABLE && ((player.getPrivateStoreType() == PrivateStoreType.SELL) || (player.getPrivateStoreType() == PrivateStoreType.BUY))) || (OfflineTradeConfig.OFFLINE_CRAFT_ENABLE && (player.isCrafting() || (player.getPrivateStoreType() == PrivateStoreType.MANUFACTURE)))))
		{
			return;
		}
		
		if (player.isHpBlocked() && !(isDOT || isHPConsumption))
		{
			return;
		}
		
		if (player.isAffected(EffectFlag.DUELIST_FURY) && !attacker.isAffected(EffectFlag.FACEOFF))
		{
			return;
		}
		
		if (!isHPConsumption)
		{
			if (awake)
			{
				player.stopEffectsOnDamage();
			}
			
			// Attacked players in craft/shops stand up.
			if (player.isCrafting() || player.isInStoreMode())
			{
				player.setPrivateStoreType(PrivateStoreType.NONE);
				player.standUp();
				player.broadcastUserInfo();
			}
			else if (player.isSitting())
			{
				player.standUp();
			}
			
			if (!isDOT)
			{
				if (Formulas.calcStunBreak(player))
				{
					player.stopStunning(true);
				}
				
				if (Formulas.calcRealTargetBreak())
				{
					player.getEffectList().stopEffects(AbnormalType.REAL_TARGET);
				}
				
				// Engraving the holy relic restarts when the clan leader is attacked.
				if ((attacker != null) && (attacker != player))
				{
					final SkillCaster sealOfRuler = player.getSkillCaster(caster -> caster.getSkill().getId() == CommonSkill.SEAL_OF_RULER.getId());
					if (sealOfRuler != null)
					{
						sealOfRuler.stopCasting(true);
						player.sendPacket(SystemMessageId.YOUR_CASTING_HAS_BEEN_INTERRUPTED);
					}
				}
			}
		}
		
		double amount = value;
		int fullValue = (int) amount;
		int tDmg = 0;
		int mpDam = 0;
		if ((attacker != null) && (attacker != player))
		{
			final Player attackerPlayer = attacker.asPlayer();
			if (attackerPlayer != null)
			{
				if (attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage())
				{
					return;
				}
				
				if (player.isInDuel())
				{
					if (player.getDuelState() == Duel.DUELSTATE_DEAD)
					{
						return;
					}
					else if (player.getDuelState() == Duel.DUELSTATE_WINNER)
					{
						return;
					}
					
					// Cancel duel if player got hit by another player, that is not part of the duel.
					if (attackerPlayer.getDuelId() != player.getDuelId())
					{
						player.setDuelState(Duel.DUELSTATE_INTERRUPTED);
					}
				}
			}
			
			// Check and calculate transfered damage.
			final PlayerStat stat = player.getStat();
			final Summon summon = player.getFirstServitor();
			if ((summon != null) && LocationUtil.checkIfInRange(1000, player, summon, true))
			{
				tDmg = ((int) amount * (int) stat.getValue(Stat.TRANSFER_DAMAGE_SUMMON_PERCENT, 0)) / 100;
				
				// A share the servitor cannot take without dying goes fully to the summoner.
				if ((tDmg > 0) && (tDmg < summon.getCurrentHp()))
				{
					// The share is already final, the damage taken modifiers of reduceCurrentHp would push it past the cap and kill the servitor.
					summon.getStatus().reduceHp(tDmg, attacker);
					amount -= tDmg;
					fullValue = (int) amount; // Reduce the announced value here as player will get a message about summon damage.
				}
			}
			
			mpDam = ((int) amount * (int) stat.getValue(Stat.MANA_SHIELD_PERCENT, 0)) / 100;
			if (mpDam > 0)
			{
				mpDam = (int) (amount - mpDam);
				if (mpDam > player.getCurrentMp())
				{
					player.sendPacket(SystemMessageId.MP_HAS_REACHED_0_THE_MANA_ARMOR_HAS_DISAPPEARED);
					player.stopSkillEffects(SkillFinishType.REMOVED, 1556);
					amount = mpDam - player.getCurrentMp();
					player.setCurrentMp(0);
				}
				else
				{
					player.reduceCurrentMp(mpDam);
					final SystemMessage smsg = new SystemMessage(SystemMessageId.DUE_TO_THE_MANA_ARMOR_EFFECT_YOU_LOSE_S1_MP_INSTEAD_OF_HP);
					smsg.addInt(mpDam);
					player.sendPacket(smsg);
					return;
				}
			}
			
			final Player caster = player.getTransferingDamageTo();
			if ((caster != null) && (player.getParty() != null) && LocationUtil.checkIfInRange(1000, player, caster, true) && !caster.isDead() && (player != caster) && player.getParty().getMembers().contains(caster))
			{
				int transferDmg = 0;
				transferDmg = ((int) amount * (int) stat.getValue(Stat.TRANSFER_DAMAGE_TO_PLAYER, 0)) / 100;
				transferDmg = Math.min((int) caster.getCurrentHp() - 1, transferDmg);
				if (transferDmg > 0)
				{
					int membersInRange = 0;
					for (Player member : caster.getParty().getMembers())
					{
						if (LocationUtil.checkIfInRange(1000, member, caster, false) && (member != caster))
						{
							membersInRange++;
						}
					}
					
					int casterHpDamage = transferDmg;
					if ((attacker.isPlayable() || attacker.isFakePlayer()) && (caster.getCurrentCp() > 0))
					{
						final int cpDamage = (int) Math.min(caster.getCurrentCp(), transferDmg);
						caster.getStatus().reduceCp(cpDamage);
						casterHpDamage -= cpDamage;
					}
					
					if (membersInRange > 0)
					{
						// CP was taken above and the share is capped, so the rest goes straight to HP without the damage taken modifiers.
						caster.getStatus().reduceHp(casterHpDamage / membersInRange, attacker, null, true, false, false, true);
						amount -= transferDmg;
						fullValue = (int) amount;
					}
				}
			}
			
			if (!(ignoreCP || attacker.hasAbnormalType(AbnormalType.GHOSTLY_WHISPERS)) && (attacker.isPlayable() || attacker.isFakePlayer()))
			{
				if (_currentCp >= amount)
				{
					setCurrentCp(_currentCp - amount); // Set Cp to diff of Cp vs value.
					amount = 0; // No need to subtract anything from Hp.
				}
				else
				{
					amount -= _currentCp; // Get diff from value vs Cp; will apply diff to Hp.
					setCurrentCp(0, false); // Set Cp to 0
				}
			}
			
			if ((fullValue > 0) && !isDOT)
			{
				// Send a System Message to the Player.
				SystemMessage smsg = new SystemMessage(SystemMessageId.C1_HAS_RECEIVED_S3_DAMAGE_FROM_C2);
				smsg.addString(player.getName());
				
				// Localisation related.
				String targetName = attacker.getName();
				if (MultilingualSupportConfig.MULTILANG_ENABLE && attacker.isNpc())
				{
					final String[] localisation = NpcNameLocalisationData.getInstance().getLocalisation(player.getLang(), attacker.getId());
					if (localisation != null)
					{
						targetName = localisation[0];
					}
				}
				
				smsg.addString(targetName);
				smsg.addInt(fullValue);
				smsg.addPopup(player.getObjectId(), attacker.getObjectId(), -fullValue);
				player.sendPacket(smsg);
				
				if ((tDmg > 0) && (summon != null) && (attackerPlayer != null))
				{
					smsg = new SystemMessage(SystemMessageId.YOU_VE_DEALT_S1_DAMAGE_TO_YOUR_TARGET_AND_S2_DAMAGE_TO_THEIR_SERVITOR);
					smsg.addInt(fullValue);
					smsg.addInt(tDmg);
					attackerPlayer.sendPacket(smsg);
				}
			}
		}
		
		if (amount > 0)
		{
			double newHp = Math.max(getCurrentHp() - amount, player.isUndying() ? 1 : 0);
			if (newHp <= 0)
			{
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CHEAT_DEATH, player) && !player.hasAbnormalType(AbnormalType.BLOCK_RESURRECTION) && !player.hasAbnormalType(AbnormalType.RESIST_CHEAT_DEATH))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerCheatDeath(player), player);
					return;
				}
				
				if (player.isInDuel())
				{
					player.disableAllSkills();
					stopHpMpRegeneration();
					if (attacker != null)
					{
						attacker.getAI().setIntentionActive();
						attacker.sendPacket(ActionFailed.STATIC_PACKET);
					}
					
					// Let the DuelManager know of his defeat.
					DuelManager.getInstance().onPlayerDefeat(player);
					newHp = 1;
				}
				else
				{
					newHp = 0;
				}
			}
			
			setCurrentHp(newHp);
		}
		
		if ((player.getCurrentHp() < 0.5) && !isHPConsumption && !player.isUndying())
		{
			player.abortAttack();
			player.abortCast();
			
			if (player.isInOlympiadMode())
			{
				stopHpMpRegeneration();
				player.setDead(true);
				player.setIsPendingRevive(true);
				final Summon pet = player.getPet();
				if (pet != null)
				{
					pet.getAI().setIntentionIdle();
				}
				
				player.getServitors().values().forEach(s -> s.getAI().setIntentionIdle());
				return;
			}
			
			player.doDie(attacker);
		}
	}
	
	/**
	 * Retrieves the current CP value of the player.<br>
	 * This method returns the {@code double} value stored in {@code _currentCp}.
	 * @return The current CP amount.
	 */
	@Override
	public double getCurrentCp()
	{
		return _currentCp;
	}
	
	/**
	 * Updates the current CP of the player.<br>
	 * This method sets the value to {@code newCp}.
	 * @param newCp The new CP value to set.
	 */
	@Override
	public void setCurrentCp(double newCp)
	{
		setCurrentCp(newCp, true);
	}
	
	/**
	 * Sets the current CP value for the player.<br>
	 * This method updates the internal CP and manages regeneration flags.<br>
	 * It also handles broadcasting the status update to other players if requested.
	 * @param value The new CP value to set.
	 * @param broadcastPacket Whether to send a status update packet to other players.
	 */
	@Override
	public void setCurrentCp(double value, boolean broadcastPacket)
	{
		// Get the Max CP of the Creature.
		final int currentCp = (int) _currentCp;
		final Player player = getActiveChar();
		final int maxCp = player.getStat().getMaxCp();
		
		synchronized (this)
		{
			if (player.isDead())
			{
				return;
			}
			
			final double newCp = Math.max(0, value);
			if (newCp >= maxCp)
			{
				// Set the RegenActive flag to false.
				_currentCp = maxCp;
				_flagsRegenActive &= ~REGEN_FLAG_CP;
				
				// Stop the HP/MP/CP Regeneration task.
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true.
				_currentCp = newCp;
				_flagsRegenActive |= REGEN_FLAG_CP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority.
				startHpMpRegeneration();
			}
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform.
		if ((currentCp != _currentCp) && broadcastPacket)
		{
			player.broadcastStatusUpdate();
		}
	}
	
	/**
	 * Updates the current status of the player based on regeneration rates.<br>
	 * This method checks if {@code CP}, {@code HP}, and {@code MP} are below their maximum recoverable limits.<br>
	 * It adds the appropriate regeneration values to each stat if they are not full.<br>
	 * Finally, it broadcasts the updated status to the client.
	 */
	@Override
	protected void doRegeneration()
	{
		final Player player = getActiveChar();
		final PlayerStat stat = player.getStat();
		
		// Modify the current CP of the Creature and broadcast Server->Client packet StatusUpdate.
		if (_currentCp < stat.getMaxRecoverableCp())
		{
			setCurrentCp(_currentCp + stat.getValue(Stat.REGENERATE_CP_RATE), false);
		}
		
		// Modify the current HP of the Creature and broadcast Server->Client packet StatusUpdate.
		final double currentHp = getCurrentHp();
		if (currentHp < stat.getMaxRecoverableHp())
		{
			setCurrentHp(currentHp + stat.getValue(Stat.REGENERATE_HP_RATE), false);
		}
		
		// Modify the current MP of the Creature and broadcast Server->Client packet StatusUpdate.
		final double currentMp = getCurrentMp();
		if (currentMp < stat.getMaxRecoverableMp())
		{
			setCurrentMp(currentMp + stat.getValue(Stat.REGENERATE_MP_RATE), false);
		}
		
		player.broadcastStatusUpdate(); // Send the StatusUpdate packet.
	}
	
	/**
	 * Retrieves the current active {@code Player} character.<br>
	 * This method converts the internal actor to a {@code Player} type.
	 * @return The active {@code Player} instance.
	 */
	@Override
	public Player getActiveChar()
	{
		return super.getActiveChar().asPlayer();
	}
}
