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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.status.SiegeFlagStatus;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.siege.Siegable;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClan;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a siege flag used during fort siege events.<br>
 * This {@code Npc} handles the logic for capturing and holding territory.<br>
 * It interacts with {@code SiegeManager} to update the status of various siege points.
 */
public class SiegeFlag extends Npc
{
	private final Clan _clan;
	private Siegable _siege;
	private final boolean _isAdvanced;
	private boolean _canTalk;
	
	/**
	 * Creates a new {@code SiegeFlag} instance for a specific player.<br>
	 * This constructor initializes the flag with its associated clan and siege data.<br>
	 * It also sets whether the flag is considered advanced.
	 * @param player The {@code Player} who owns or interacts with the flag.
	 * @param template The {@code NpcTemplate} used to define the flag's appearance.
	 * @param advanced A boolean indicating if the flag has advanced properties.
	 */
	public SiegeFlag(Player player, NpcTemplate template, boolean advanced)
	{
		super(template);
		setInstanceType(InstanceType.SiegeFlag);
		
		_clan = player.getClan();
		_canTalk = true;
		final int x = player.getX();
		final int y = player.getY();
		final int z = player.getZ();
		_siege = SiegeManager.getInstance().getSiege(x, y, z);
		if (_siege == null)
		{
			_siege = FortSiegeManager.getInstance().getSiege(x, y, z);
		}
		
		if ((_clan == null) || (_siege == null))
		{
			throw new NullPointerException(getClass().getSimpleName() + ": Initialization failed.");
		}
		
		final SiegeClan sc = _siege.getAttackerClan(_clan);
		if (sc == null)
		{
			throw new NullPointerException(getClass().getSimpleName() + ": Cannot find siege clan.");
		}
		
		sc.addFlag(this);
		_isAdvanced = advanced;
		getStatus();
		setInvul(false);
	}
	
	/**
	 * Checks if the {@code SiegeFlag} is currently vulnerable to damage.<br>
	 * It returns {@code false} if the entity is invincible.
	 * @return {@code true} if the flag can be attacked, otherwise {@code false}.
	 */
	@Override
	public boolean canBeAttacked()
	{
		return !isInvul();
	}
	
	/**
	 * Checks if the {@code SiegeFlag} can be targeted by an auto-attack. <br>
	 * This method returns {@code true} if the flag is not currently invincible.
	 * @param attacker The {@code Creature} attempting to perform the attack.
	 * @return {@code true} if the flag can be attacked, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return !isInvul();
	}
	
	/**
	 * Handles the logic when this flag is killed by another creature.<br>
	 * It removes the flag from the associated siege clan if applicable.
	 * @param killer The {@code Creature} that dealt the killing blow.
	 * @return {@code true} if the death was successful, {@code false} otherwise.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if ((_siege != null) && (_clan != null))
		{
			final SiegeClan sc = _siege.getAttackerClan(_clan);
			if (sc != null)
			{
				sc.removeFlag(this);
			}
		}
		
		return true;
	}
	
	/**
	 * This method is called when a {@code Player} performs a forced attack.<br>
	 * It triggers the standard action logic for the player interaction.
	 * @param player The {@code Player} who initiated the forced attack.
	 */
	@Override
	public void onForcedAttack(Player player)
	{
		onAction(player);
	}
	
	/**
	 * Handles actions performed by a {@code Player} on this siege flag.<br>
	 * It updates the player's target if they are valid.<br>
	 * If the player interacts, it triggers an attack intention based on distance and reachability.
	 * @param player The {@code Player} performing the action.
	 * @param interact Whether the player is currently interacting with the flag.
	 */
	@Override
	public void onAction(Player player, boolean interact)
	{
		if ((player == null) || !canTarget(player))
		{
			return;
		}
		
		// Check if the Player already target the Npc.
		if (this != player.getTarget())
		{
			// Set the target of the Player player.
			player.setTarget(this);
		}
		else if (interact)
		{
			if (isAutoAttackable(player) && (Math.abs(player.getZ() - getZ()) < 100))
			{
				player.getAI().setIntentionAttack(this);
			}
			else
			{
				// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet.
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}
	
	/**
	 * Checks if this flag belongs to an advanced headquarter.<br>
	 * This method returns the value of the {@code _isAdvanced} field.
	 * @return {@code true} if it is an advanced headquarter, {@code false} otherwise.
	 */
	public boolean isAdvancedHeadquarter()
	{
		return _isAdvanced;
	}
	
	/**
	 * Retrieves the current status of the siege flag.<br>
	 * This method returns a {@code SiegeFlagStatus} value.
	 * @return The current {@code SiegeFlagStatus}.
	 */
	@Override
	public SiegeFlagStatus getStatus()
	{
		return (SiegeFlagStatus) super.getStatus();
	}
	
	/**
	 * Initializes the status of this {@code SiegeFlag}.<br>
	 * It creates a new {@code SiegeFlagStatus} object for the current instance.
	 */
	@Override
	public void initCharStatus()
	{
		setStatus(new SiegeFlagStatus(this));
	}
	
	/**
	 * Reduces the current health of this {@code SiegeFlag}.<br>
	 * It also sends a warning to clan members if a siege is in progress.
	 * @param damage The amount of damage to subtract from health.
	 * @param attacker The {@code Creature} that initiated the attack.
	 * @param skill The {@code Skill} used to perform the attack.
	 */
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, skill);
		if (canTalk() && (((getCastle() != null) && getCastle().getSiege().isInProgress()) || ((getFort() != null) && getFort().getSiege().isInProgress())) && (_clan != null))
		{
			// Send warning to owners of headquarters that theirs base is under attack.
			_clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.YOUR_BASE_IS_BEING_ATTACKED));
			setCanTalk(false);
			ThreadPool.schedule(new ScheduleTalkTask(), 20000);
		}
	}
	
	private class ScheduleTalkTask implements Runnable
	{
		public ScheduleTalkTask()
		{
		}
		
		@Override
		public void run()
		{
			setCanTalk(true);
		}
	}
	
	/**
	 * Sets whether the {@code SiegeFlag} is allowed to talk.<br>
	 * Use {@code true} to enable communication.<br>
	 * Use {@code false} to disable communication.
	 * @param value The new status for talking.
	 */
	void setCanTalk(boolean value)
	{
		_canTalk = value;
	}
	
	/**
	 * Checks if the flag is allowed to talk.<br>
	 * Returns the current state of {@code _canTalk}.
	 * @return {@code true} if talking is enabled, {@code false} otherwise.
	 */
	private boolean canTalk()
	{
		return _canTalk;
	}
}
