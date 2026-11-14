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

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * Represents a defensive entity within an instance.<br>
 * This class handles the logic for entities that protect specific locations or objectives.<br>
 * It extends {@code Attackable} to allow interaction with other actors.
 */
public class Defender extends Attackable
{
	private Castle _castle = null; // the castle which the instance should defend
	private Fort _fort = null; // the fortress which the instance should defend
	
	/**
	 * Creates a new {@code Defender} instance.<br>
	 * This constructor initializes the defender using a provided template.<br>
	 * It sets the {@code InstanceType} to {@code Defender}.
	 * @param template The {@code NpcTemplate} used to create this defender.
	 */
	public Defender(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Defender);
	}
	
	/**
	 * Applies damage to this entity from an attacker.<br>
	 * This method also triggers a hate increase for nearby {@code Defender} objects.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param damage The amount of damage to apply.
	 * @param skill The {@code Skill} used to perform the attack.
	 */
	@Override
	public void addDamage(Creature attacker, int damage, Skill skill)
	{
		super.addDamage(attacker, damage, skill);
		World.forEachVisibleObjectInRange(this, Defender.class, 500, defender -> defender.addDamageHate(attacker, 0, 10));
	}
	
	/**
	 * Checks if the defender can be attacked by a specific creature.<br>
	 * This method validates if the attacker is a playable player and if they are an enemy during an active siege.
	 * @param attacker The {@code Creature} attempting to perform the attack.
	 * @return {@code true} if the attacker is allowed to hit this defender, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		// Attackable during siege by all except defenders.
		if (!attacker.isPlayable())
		{
			return false;
		}
		
		final Player player = attacker.asPlayer();
		
		// Check if siege is in progress.
		if (((_fort != null) && _fort.getZone().isActive()) || ((_castle != null) && _castle.getZone().isActive()))
		{
			final int activeSiegeId = (_fort != null) ? _fort.getResidenceId() : _castle.getResidenceId();
			
			// Check if player is an enemy of this defender npc.
			if ((player != null) && (((player.getSiegeState() == 2) && !player.isRegisteredOnThisSiegeField(activeSiegeId)) || (player.getSiegeState() == 1) || (player.getSiegeState() == 0)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the defender uses random animations.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if random animations are enabled, otherwise {@code false}.
	 */
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
	
	/**
	 * Moves the defender back to its original spawn point.<br>
	 * This method checks if the entity is too far from its spawn location.<br>
	 * If it is outside the allowed radius, it clears aggro and moves home.
	 */
	@Override
	public void returnHome()
	{
		if (getWalkSpeed() <= 0)
		{
			return;
		}
		
		final Spawn spawn = getSpawn();
		if (spawn == null)
		{
			return;
		}
		
		if (!isInsideRadius2D(spawn, 40))
		{
			clearAggroList();
			
			if (hasAI())
			{
				getAI().setIntentionMoveTo(spawn.getLocation());
			}
		}
	}
	
	/**
	 * This method is called when the {@code Defender} instance is first created.<br>
	 * It initializes the associated {@code Fort} and {@code Castle} based on current coordinates.<br>
	 * It logs a warning if the entity spawns outside of any valid zone.
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		final int x = getX();
		final int y = getY();
		final int z = getZ();
		
		_fort = FortManager.getInstance().getFort(x, y, z);
		_castle = CastleManager.getInstance().getCastle(x, y, z);
		if ((_fort == null) && (_castle == null))
		{
			LOGGER.warning("Defender spawned outside of Fortress or Castle zone!" + this);
		}
	}
	
	/**
	 * Handles actions performed by a {@code Player} on this defender.<br>
	 * It checks if the player can target the entity and updates their AI intentions.<br>
	 * If the interaction is successful, it sets the attack or interact intention based on distance.
	 * @param player The {@code Player} performing the action.
	 * @param interact Whether the player is attempting to interact with the defender.
	 */
	@Override
	public void onAction(Player player, boolean interact)
	{
		if (!canTarget(player))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
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
			// This max heigth difference might need some tweaking.
			if (isAutoAttackable(player) && !isAlikeDead() && (Math.abs(player.getZ() - getZ()) < 600))
			{
				player.getAI().setIntentionAttack(this);
			}
			
			// Notify the Player AI with INTERACT.
			if (!isAutoAttackable(player) && !canInteract(player))
			{
				player.getAI().setIntentionInteract(this);
			}
		}
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet.
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Executes a magic skill and updates the current target.<br>
	 * This method finds the nearest {@code Defender} or eligible {@code Player} with the lowest health.<br>
	 * It then sets this entity to attack that specific target.<br>
	 * Finally, it calls the superclass {@code useMagic} method.
	 * @param skill The {@code Skill} to be used by the defender.
	 */
	@Override
	public void useMagic(Skill skill)
	{
		if (!skill.hasNegativeEffect())
		{
			Creature target = this;
			double lowestHpValue = Double.MAX_VALUE;
			for (Creature nearby : World.getVisibleObjectsInRange(this, Creature.class, skill.getCastRange()))
			{
				if ((nearby == null) || nearby.isDead() || !GeoEngine.getInstance().canSeeTarget(this, nearby))
				{
					continue;
				}
				
				if (nearby instanceof Defender)
				{
					final double targetHp = nearby.getCurrentHp();
					if (lowestHpValue > targetHp)
					{
						target = nearby;
						lowestHpValue = targetHp;
					}
				}
				else if (nearby.isPlayer())
				{
					final Player player = nearby.asPlayer();
					if ((player.getSiegeState() == 2) && !player.isRegisteredOnThisSiegeField(getScriptValue()))
					{
						final double targetHp = nearby.getCurrentHp();
						if (lowestHpValue > targetHp)
						{
							target = nearby;
							lowestHpValue = targetHp;
						}
					}
				}
			}
			
			setTarget(target);
		}
		
		super.useMagic(skill);
	}
	
	/**
	 * Increases the hate level of a {@code Creature} based on damage dealt.<br>
	 * This method updates how much an attacker is targeted by the defender.<br>
	 * It handles specific logic to prevent unnecessary hate during siege events.
	 * @param attacker The {@code Creature} that dealt the damage.
	 * @param damage The amount of damage dealt to the defender.
	 * @param aggro The amount of hate to add to the attacker.
	 */
	@Override
	public void addDamageHate(Creature attacker, long damage, long aggro)
	{
		if (attacker == null)
		{
			return;
		}
		
		if (!(attacker instanceof Defender))
		{
			if ((damage == 0) && (aggro <= 1) && (attacker.isPlayable()))
			{
				final Player player = attacker.asPlayer();
				
				// Check if siege is in progress.
				if (((_fort != null) && _fort.getZone().isActive()) || ((_castle != null) && _castle.getZone().isActive()))
				{
					final int activeSiegeId = (_fort != null) ? _fort.getResidenceId() : _castle.getResidenceId();
					
					// Do not add hate on defenders.
					if ((player.getSiegeState() == 2) && player.isRegisteredOnThisSiegeField(activeSiegeId))
					{
						return;
					}
				}
			}
			
			super.addDamageHate(attacker, damage, aggro);
		}
	}
}
