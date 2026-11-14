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

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.stat.PlayableStat;
import org.l2jmobius.gameserver.entity.actor.status.PlayableStatus;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanWar;
import org.l2jmobius.gameserver.entity.clan.enums.ClanWarState;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class represents all playable characters within the game world.<br>
 * It serves as a base for entities such as {@code Player} and {@code Summon}.
 */
public abstract class Playable extends Creature
{
	private Creature _lockedTarget = null;
	private Player transferDmgTo = null;
	
	/**
	 * Creates a new {@code Playable} instance using an ID and a template.<br>
	 * This constructor initializes the entity as a playable character.<br>
	 * It sets the {@code InstanceType} to {@code Playable}.<br>
	 * It ensures that invulnerability is set to {@code false}.
	 * @param objectId The unique identifier for the object.
	 * @param template The {@code CreatureTemplate} used to define the character properties.
	 */
	public Playable(int objectId, CreatureTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.Playable);
		setInvul(false);
	}
	
	/**
	 * Creates a new {@code Playable} instance using the provided template.<br>
	 * This constructor initializes the entity as a playable character type.<br>
	 * It sets the invulnerability status to {@code false}.
	 * @param template The {@code CreatureTemplate} used to define the properties of the new character.
	 */
	public Playable(CreatureTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Playable);
		setInvul(false);
	}
	
	/**
	 * Retrieves the {@code PlayableStat} for this entity.<br>
	 * This method casts the base stat to a {@code PlayableStat}.
	 * @return the {@code PlayableStat} associated with this object.
	 */
	@Override
	public PlayableStat getStat()
	{
		return (PlayableStat) super.getStat();
	}
	
	/**
	 * Initializes the {@code PlayableStat} for this character.<br>
	 * This method creates and sets a new instance of {@code PlayableStat}.
	 */
	@Override
	public void initCharStat()
	{
		setStat(new PlayableStat(this));
	}
	
	/**
	 * Retrieves the current status of the playable character.<br>
	 * This method returns a {@code PlayableStatus} object.
	 * @return The current {@code PlayableStatus}.
	 */
	@Override
	public PlayableStatus getStatus()
	{
		return (PlayableStatus) super.getStatus();
	}
	
	/**
	 * Initializes the status of the character.<br>
	 * This method creates a new {@code PlayableStatus} instance for this object.
	 */
	@Override
	public void initCharStatus()
	{
		setStatus(new PlayableStatus(this));
	}
	
	/**
	 * Handles the logic for when a {@code Playable} entity dies.<br>
	 * It updates health, stops actions, and notifies relevant systems like quests and instances.<br>
	 * This method ensures that death only occurs once.
	 * @param killer The {@code Creature} responsible for the death, or {@code null}.
	 * @return {@code true} if the entity died successfully, {@code false} otherwise.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_DEATH, this))
		{
			final TerminateReturn returnBack = EventDispatcher.getInstance().notifyEvent(new OnCreatureDeath(killer, this), this, TerminateReturn.class);
			if ((returnBack != null) && returnBack.terminate())
			{
				return false;
			}
		}
		
		// Killing is only possible one time.
		synchronized (this)
		{
			if (isDead())
			{
				return false;
			}
			
			// Now reset currentHp to zero.
			setCurrentHp(0);
			setDead(true);
		}
		
		// Set target to null and cancel Attack or Cast.
		setTarget(null);
		
		// Abort casting after target has been cancelled.
		abortAttack();
		abortCast();
		
		// Stop movement.
		stopMove(null);
		
		// Stop HP/MP/CP Regeneration task.
		getStatus().stopHpMpRegeneration();
		
		boolean deleteBuffs = true;
		if (isNoblesseBlessedAffected())
		{
			stopEffects(EffectFlag.NOBLESS_BLESSING);
			deleteBuffs = false;
		}
		
		if (isResurrectSpecialAffected())
		{
			stopEffects(EffectFlag.RESURRECTION_SPECIAL);
			deleteBuffs = false;
		}
		
		final Player player = asPlayer();
		if (isPlayer())
		{
			if (player.hasCharmOfCourage() && player.isInSiege())
			{
				// The charm lasts through death until its time runs out (Kamael notes).
				player.reviveRequest(player, false, 0, 0, 0, 0);
			}
		}
		
		if (deleteBuffs)
		{
			stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		else
		{
			// Buffs are kept, but Lucky Charm is lost as normal.
			getEffectList().stopEffects(AbnormalType.REDUCE_DROP_PENALTY);
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform.
		broadcastStatusUpdate();
		
		ZoneManager.getInstance().getRegion(this).onDeath(this);
		
		// Notify Quest of Playable's death.
		if (!player.isNotifyQuestOfDeathEmpty())
		{
			for (QuestState qs : player.getNotifyQuestOfDeath())
			{
				qs.getQuest().onDeath(killer == null ? this : killer, this, qs);
			}
		}
		
		// Notify instance.
		if (isPlayer())
		{
			final Instance instance = getInstanceWorld();
			if (instance != null)
			{
				instance.onDeath(player);
			}
		}
		
		if (killer != null)
		{
			final Player killerPlayer = killer.asPlayer();
			if (killerPlayer != null)
			{
				killerPlayer.onPlayerKill(this);
			}
		}
		
		// Notify Creature AI.
		getAI().notifyActionDeath();
		return true;
	}
	
	/**
	 * Checks if the target player can be attacked in PvP.<br>
	 * This method evaluates conditions like reputation, flags, and clan wars.<br>
	 * It returns {@code true} if the target is valid for PvP combat.
	 * @param target The {@code Player} to check against.
	 * @return {@code true} if PvP is allowed, {@code false} otherwise.
	 */
	public boolean checkIfPvP(Player target)
	{
		final Player player = asPlayer();
		if ((player == null) //
			|| (target == null) //
			|| (player == target) //
			|| (target.getReputation() < 0) //
			|| (target.getPvpFlag() > 0) //
			|| target.isOnDarkSide())
		{
			return true;
		}
		else if (player.isInParty() && player.getParty().containsPlayer(target))
		{
			return false;
		}
		
		final Clan playerClan = player.getClan();
		if ((playerClan != null) && !player.isAcademyMember() && !target.isAcademyMember())
		{
			final ClanWar war = playerClan.getWarWith(target.getClanId());
			return (war != null) && (war.getState() == ClanWarState.MUTUAL);
		}
		
		return false;
	}
	
	/**
	 * Checks if the actor can be targeted for an attack.<br>
	 * This method currently always returns {@code true}.
	 * @return {@code true} if the actor is targetable.
	 */
	@Override
	public boolean canBeAttacked()
	{
		return true;
	}
	
	// Support for Noblesse Blessing skill, where buffs are retained after resurrect.
	/**
	 * Checks if the character is currently affected by a Noblesse Blessing.<br>
	 * This method returns {@code true} if the specific effect flag is active.
	 * @return {@code true} if the blessing is active, {@code false} otherwise.
	 */
	public boolean isNoblesseBlessedAffected()
	{
		return isAffected(EffectFlag.NOBLESS_BLESSING);
	}
	
	/**
	 * Checks if the character is affected by a special resurrection effect.<br>
	 * This method verifies the presence of the {@code RESURRECTION_SPECIAL} flag.
	 * @return {@code true} if the effect is active, {@code false} otherwise.
	 */
	public boolean isResurrectSpecialAffected()
	{
		return isAffected(EffectFlag.RESURRECTION_SPECIAL);
	}
	
	/**
	 * Checks if the character is affected by the silent moving effect.<br>
	 * This method returns {@code true} if the {@code EffectFlag.SILENT_MOVE} flag is active.
	 * @return {@code true} if the effect is active, {@code false} otherwise.
	 */
	public boolean isSilentMovingAffected()
	{
		return isAffected(EffectFlag.SILENT_MOVE);
	}
	
	/**
	 * Checks if the character is currently affected by a protection blessing.<br>
	 * This method returns {@code true} if the {@code EffectFlag.PROTECTION_BLESSING} is active.
	 * @return {@code true} if the effect is active, {@code false} otherwise.
	 */
	public boolean isProtectionBlessingAffected()
	{
		return isAffected(EffectFlag.PROTECTION_BLESSING);
	}
	
	/**
	 * Updates the visual icons for active effects.<br>
	 * This method refreshes the display of status effects on the character.
	 * @param partyOnly Set to {@code true} to update only for party members.
	 */
	@Override
	public void updateEffectIcons(boolean partyOnly)
	{
		getEffectList().updateEffectIcons(partyOnly);
	}
	
	/**
	 * Checks if the character currently has a locked target.<br>
	 * It returns {@code true} if the internal target is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if there is a locked target, {@code false} otherwise.
	 */
	public boolean isLockedTarget()
	{
		return _lockedTarget != null;
	}
	
	/**
	 * Retrieves the current locked target of this entity.<br>
	 * This method returns the {@code Creature} that is currently being targeted.
	 * @return the {@code Creature} object representing the locked target, or {@code null} if none exists.
	 */
	public Creature getLockedTarget()
	{
		return _lockedTarget;
	}
	
	/**
	 * Sets the specific target that this character is currently locked onto.<br>
	 * This value will be used by {@code getLockedTarget}.
	 * @param creature The {@code Creature} to set as the locked target.
	 */
	public void setLockedTarget(Creature creature)
	{
		_lockedTarget = creature;
	}
	
	/**
	 * Sets the target player for damage transfer.<br>
	 * This method updates the {@code transferDmgTo} field.
	 * @param val The {@code Player} who will receive the transferred damage.
	 */
	public void setTransferDamageTo(Player val)
	{
		transferDmgTo = val;
	}
	
	/**
	 * Retrieves the {@code Player} who receives transferred damage.<br>
	 * This value is set via the {@code setTransferDamageTo} method.
	 * @return the {@code Player} receiving the damage, or {@code null} if none.
	 */
	public Player getTransferingDamageTo()
	{
		return transferDmgTo;
	}
	
	public abstract void doPickupItem(WorldObject object);
	
	public abstract boolean useMagic(Skill skill, Item item, boolean forceUse, boolean dontMove);
	
	public abstract void storeMe();
	
	public abstract void storeEffect(boolean storeEffects);
	
	public abstract void restoreEffects();
	
	/**
	 * Checks if the character is currently participating in an event.<br>
	 * This method always returns {@code false} at this time.
	 * @return {@code true} if the character is on an event, otherwise {@code false}.
	 */
	public boolean isOnEvent()
	{
		return false;
	}
	
	/**
	 * Checks if the entity is a playable character.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the entity is playable.
	 */
	@Override
	public boolean isPlayable()
	{
		return true;
	}
	
	/**
	 * Converts the current object into a {@code Playable} instance.<br>
	 * This method returns the current object itself.
	 * @return the current object cast as {@code Playable}
	 */
	@Override
	public Playable asPlayable()
	{
		return this;
	}
}
