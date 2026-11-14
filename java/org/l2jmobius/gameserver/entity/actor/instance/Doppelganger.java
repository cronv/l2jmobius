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

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.DoppelgangerAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameManager;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.ExDamagePopUp;
import org.l2jmobius.gameserver.network.serverpackets.ExMagicAttackInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a {@code Doppelganger} entity within the game world.<br>
 * This class handles the behavior and logic for this specific type of hostile actor.<br>
 * It utilizes {@code DoppelgangerAI} to manage its actions and interactions.
 * @author Nik, Enryu
 */
public class Doppelganger extends Attackable
{
	private boolean _copySummonerEffects = true;
	private ScheduledFuture<?> _attackTask = null;
	private Creature _attackTarget = null;
	
	/**
	 * Creates a new {@code Doppelganger} instance based on a template.<br>
	 * This constructor initializes the entity with properties from its owner.<br>
	 * It sets the summoner, clan, and world instance to match the owner.<br>
	 * The entity is spawned at a random nearby location.
	 * @param template The {@code NpcTemplate} used to define the doppelganger.
	 * @param owner The {@code Player} who summons this doppelganger.
	 */
	public Doppelganger(NpcTemplate template, Player owner)
	{
		super(template);
		
		setSummoner(owner);
		setCloneObjId(owner.getObjectId());
		setClanId(owner.getClanId());
		setInstance(owner.getInstanceWorld()); // Set instance to same as owner.
		setXYZInvisible(owner.getX() + Rnd.get(-100, 100), owner.getY() + Rnd.get(-100, 100), owner.getZ());
		((DoppelgangerAI) getAI()).setStartFollowController(true);
		followSummoner(true);
	}
	
	/**
	 * Initializes the artificial intelligence for this creature.<br>
	 * This method creates and returns a new instance of {@code DoppelgangerAI}.
	 * @return the initialized {@code CreatureAI} object.
	 */
	@Override
	protected CreatureAI initAI()
	{
		return new DoppelgangerAI(this);
	}
	
	/**
	 * This method is called when the {@code Doppelganger} is first created.<br>
	 * It initializes the entity by calling the parent class method.<br>
	 * If enabled, it copies abnormal effects from the summoner to this entity.
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		if (_copySummonerEffects && (getSummoner() != null))
		{
			final Creature summoner = getSummoner();
			for (BuffInfo summonerInfo : summoner.getEffectList().getEffects())
			{
				if (summonerInfo.getAbnormalTime() > 0)
				{
					final BuffInfo info = new BuffInfo(summoner, this, summonerInfo.getSkill(), false, null, null);
					info.setAbnormalTime(summonerInfo.getAbnormalTime());
					getEffectList().add(info);
				}
			}
		}
	}
	
	/**
	 * Updates the behavior of the doppelganger regarding its summoner.<br>
	 * If {@code true}, it sets the intention to follow the summoner if currently idle or active.<br>
	 * If {@code false}, it stops following and sets the intention to idle.
	 * @param followSummoner The boolean flag determining whether to follow the summoner.
	 */
	public void followSummoner(boolean followSummoner)
	{
		if (followSummoner)
		{
			final Intention intention = getAI().getIntention();
			if ((intention == Intention.IDLE) || (intention == Intention.ACTIVE))
			{
				setRunning();
				getAI().setIntentionFollow(getSummoner());
			}
		}
		else if (getAI().getIntention() == Intention.FOLLOW)
		{
			getAI().setIntentionIdle();
		}
	}
	
	/**
	 * Sets whether the doppelganger copies effects from its summoner.<br>
	 * This updates the internal {@code _copySummonerEffects} flag.
	 * @param copySummonerEffects The boolean value to set for copying effects.
	 */
	public void setCopySummonerEffects(boolean copySummonerEffects)
	{
		_copySummonerEffects = copySummonerEffects;
	}
	
	/**
	 * Stops the current attack task for this entity.<br>
	 * It cancels the {@code _attackTask} if it is currently running.<br>
	 * It also sets both {@code _attackTask} and {@code _attackTarget} to {@code null}.
	 */
	public void stopAttackTask()
	{
		if ((_attackTask != null) && !_attackTask.isCancelled() && !_attackTask.isDone())
		{
			_attackTask.cancel(false);
			_attackTask = null;
			_attackTarget = null;
		}
	}
	
	/**
	 * Starts a recurring task to perform combat actions against a specific target.<br>
	 * This method stops any existing attack tasks before starting a new one.<br>
	 * It schedules the {@code thinkCombat} method to run every 1000 milliseconds.
	 * @param target The {@code Creature} that this entity will attack.
	 */
	public void startAttackTask(Creature target)
	{
		stopAttackTask();
		_attackTarget = target;
		_attackTask = ThreadPool.scheduleAtFixedRate(this::thinkCombat, 1000, 1000);
	}
	
	/**
	 * Handles the combat logic for the {@code Doppelganger}.<br>
	 * It checks if there is a valid target to attack.<br>
	 * If no target exists, it stops the current attack task.<br>
	 * Otherwise, it performs an auto-attack on the target.
	 */
	private void thinkCombat()
	{
		final Creature target = _attackTarget;
		if (target == null)
		{
			stopAttackTask();
			return;
		}
		
		doAutoAttack(target);
	}
	
	/**
	 * Retrieves the {@code pvpFlag} from the summoner.<br>
	 * If no summoner exists, it returns {@code 0}.
	 * @return The {@code byte} flag of the summoner.
	 */
	@Override
	public byte getPvpFlag()
	{
		return getSummoner() != null ? getSummoner().getPvpFlag() : 0;
	}
	
	/**
	 * Retrieves the team of the summoner.<br>
	 * If no summoner exists, it returns {@code Team.NONE}.
	 * @return the {@code Team} of the owner or {@code Team.NONE} if null.
	 */
	@Override
	public Team getTeam()
	{
		return getSummoner() != null ? getSummoner().getTeam() : Team.NONE;
	}
	
	/**
	 * Checks if this entity can be attacked by a specific creature.<br>
	 * It delegates the check to the summoner if one exists.<br>
	 * Otherwise, it uses the default behavior of the {@code Attackable} class.
	 * @param attacker The {@code Creature} attempting to perform an attack.
	 * @return {@code true} if the entity can be attacked, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		final Creature summoner = getSummoner();
		if (summoner != null)
		{
			return summoner.isAutoAttackable(attacker);
		}
		
		return super.isAutoAttackable(attacker);
	}
	
	/**
	 * Executes an attack on a specific target.<br>
	 * This method processes the base attack logic and sends a damage message to the recipient.
	 * @param damage The amount of damage to deal.
	 * @param target The {@code Creature} receiving the attack.
	 * @param skill The {@code Skill} being used for the attack.
	 * @param isDOT Whether the attack is a Damage Over Time effect.
	 * @param directlyToHp Whether the damage should bypass armor and go directly to health.
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
	 * Sends a damage notification message to the target.<br>
	 * This method handles special cases like blocked attacks and olympiad notifications.<br>
	 * It prevents duplicate messages if the target is the owner.
	 * @param target The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} used to perform the attack.
	 * @param damage The amount of damage dealt.
	 * @param crit Whether the hit was a critical strike.
	 * @param miss Whether the attack missed the target.
	 */
	@Override
	public void sendDamageMessage(Creature target, Skill skill, int damage, boolean crit, boolean miss)
	{
		final int objectId = getObjectId();
		final int targetObjectId = target.getObjectId();
		if (miss || (getSummoner() == null) || !getSummoner().isPlayer())
		{
			sendPacket(new ExDamagePopUp(objectId, targetObjectId, 0, ExDamagePopUp.DODGE, skill));
			// target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.DODGE));
			return;
		}
		
		// Prevents the double spam of system messages, if the target is the owning player.
		if (targetObjectId != getSummoner().getObjectId())
		{
			final Player player = asPlayer();
			if (player.isInOlympiadMode() && (target.isPlayer()) && target.asPlayer().isInOlympiadMode() && (target.asPlayer().getOlympiadGameId() == player.getOlympiadGameId()))
			{
				OlympiadGameManager.getInstance().notifyCompetitorDamage(getSummoner().asPlayer(), damage);
			}
			
			final SystemMessage sm;
			if ((target.isHpBlocked() && !target.isNpc()) || (target.isPlayer() && target.isAffected(EffectFlag.DUELIST_FURY) && !player.isAffected(EffectFlag.FACEOFF)))
			{
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
				
				sm = new SystemMessage(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
			}
			else
			{
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
				
				sm = new SystemMessage(SystemMessageId.C1_HAS_DEALT_S3_DAMAGE_TO_C2);
				sm.addNpcName(this);
				sm.addString(target.getName());
				sm.addInt(damage);
				sm.addPopup(targetObjectId, objectId, (damage * -1));
			}
			
			sendPacket(sm);
		}
	}
	
	/**
	 * Reduces the current health of this entity based on the provided damage.<br>
	 * It also sends a system message to the summoner if the target is hit.
	 * @param damage The amount of damage to subtract from the health.
	 * @param attacker The {@code Creature} that initiated the attack.
	 * @param skill The {@code Skill} used to perform the action.
	 */
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, skill);
		
		if ((getSummoner() != null) && getSummoner().isPlayer() && (attacker != null) && !isDead() && !isHpBlocked())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RECEIVED_S3_DAMAGE_FROM_C2);
			sm.addNpcName(this);
			sm.addString(attacker.getName());
			sm.addInt((int) damage);
			sm.addPopup(getObjectId(), attacker.getObjectId(), (int) -damage);
			sendPacket(sm);
		}
	}
	
	/**
	 * Converts this entity into a {@code Player} object.<br>
	 * It returns the summoner if one exists.<br>
	 * Otherwise, it falls back to the parent class implementation.
	 * @return The {@code Player} associated with this entity or its summoner.
	 */
	@Override
	public Player asPlayer()
	{
		return getSummoner() != null ? getSummoner().asPlayer() : super.asPlayer();
	}
	
	/**
	 * Removes this entity from the game world.<br>
	 * This method stops any active attack tasks before deletion.
	 * @return {@code true} if the entity was successfully deleted, or {@code false} otherwise.
	 */
	@Override
	public boolean deleteMe()
	{
		stopAttackTask();
		return super.deleteMe();
	}
	
	/**
	 * Removes this entity from the game world once its time is up.<br>
	 * The clone is told to go before the world drops it, or the client keeps drawing it where it stood.
	 */
	@Override
	public void onDecay()
	{
		if (!isDecayed())
		{
			final DeleteObject deletePacket = new DeleteObject(this);
			broadcastPacket(deletePacket);
			
			// A summoner standing too far to be reached by the broadcast still has the clone on screen.
			final Player summoner = getSummoner() != null ? getSummoner().asPlayer() : null;
			if (summoner != null)
			{
				summoner.sendPacket(deletePacket);
			}
		}
		
		super.onDecay();
	}
	
	/**
	 * This method is called when the owner of this {@code Doppelganger} is teleported.<br>
	 * It removes the {@code Doppelganger} from the game world.
	 */
	@Override
	public void onTeleported()
	{
		deleteMe(); // In retail, doppelgangers disappear when summoner teleports.
	}
	
	/**
	 * Sends a {@code ServerPacket} to the summoner of this entity.<br>
	 * This method checks if a summoner exists before attempting to send.<br>
	 * If no summoner is found, nothing happens.
	 * @param packet The {@code ServerPacket} to be sent to the summoner.
	 */
	@Override
	public void sendPacket(ServerPacket packet)
	{
		if (getSummoner() != null)
		{
			getSummoner().sendPacket(packet);
		}
	}
	
	/**
	 * Sends a system message to the summoner of this {@code Doppelganger}.<br>
	 * This method checks if a summoner exists before sending.
	 * @param id The unique identifier for the system message to be sent.
	 */
	@Override
	public void sendPacket(SystemMessageId id)
	{
		if (getSummoner() != null)
		{
			getSummoner().sendPacket(id);
		}
	}
	
	/**
	 * Returns a string representation of the {@code Doppelganger}.<br>
	 * This includes the base information and the owner's name.
	 * @return A formatted string containing the entity details.
	 */
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append('(');
		sb.append(getId());
		sb.append(") Summoner: ");
		sb.append(getSummoner());
		return sb.toString();
	}
}
