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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * This class manages all Castle Siege Artefacts.<br>
 * It handles the behavior and logic for these specific entities within the game world.
 */
public class Artefact extends Npc
{
	/**
	 * Creates a new {@code Artefact} instance.<br>
	 * This constructor initializes the object using the provided template.<br>
	 * It also sets the specific instance type to {@code InstanceType.Artefact}.
	 * @param template The {@code NpcTemplate} used to configure this artefact.
	 */
	public Artefact(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Artefact);
	}
	
	/**
	 * This method is called when the {@code Artefact} instance is spawned.<br>
	 * It registers the current object with its associated castle.
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		getCastle().registerArtefact(this);
	}
	
	/**
	 * Checks if the current object is an artefact.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if this instance is an artefact.
	 */
	@Override
	public boolean isArtefact()
	{
		return true;
	}
	
	/**
	 * Checks if the creature can be targeted for auto-attacks.<br>
	 * This method currently always returns {@code false}.
	 * @param attacker The {@code Creature} attempting to attack.
	 * @return {@code false} because this entity cannot be auto-attacked.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}
	
	/**
	 * Checks if this entity can be targeted for an attack.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the entity can be attacked, otherwise {@code false}.
	 */
	@Override
	public boolean canBeAttacked()
	{
		return false;
	}
	
	/**
	 * Handles an attack initiated by a player against this artefact.<br>
	 * This method sends an {@code ActionFailed} packet to the {@code Player}.<br>
	 * It prevents the client from waiting for further responses.
	 * @param player The {@code Player} who attempted the attack.
	 */
	@Override
	public void onForcedAttack(Player player)
	{
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet.
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Reduces the current health of this entity.<br>
	 * This method applies damage from a specific attack.
	 * @param damage The amount of health to subtract.
	 * @param attacker The {@code Creature} that initiated the attack.
	 * @param skill The {@code Skill} used to perform the action.
	 */
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, Skill skill)
	{
	}
	
	/**
	 * Reduces the current health of this entity based on various combat parameters.<br>
	 * This method handles complex damage calculations including dots and reflections.
	 * @param value The amount of damage to subtract from the health.
	 * @param attacker The {@code Creature} that initiated the attack.
	 * @param skill The {@code Skill} used to perform the action.
	 * @param isDOT Whether this damage should be applied as a Damage Over Time effect.
	 * @param directlyToHp Whether the damage bypasses armor and goes directly to health.
	 * @param critical Whether the hit should be treated as a critical strike.
	 * @param reflect Whether the damage should be reflected back to the attacker.
	 */
	@Override
	public void reduceCurrentHp(double value, Creature attacker, Skill skill, boolean isDOT, boolean directlyToHp, boolean critical, boolean reflect)
	{
	}
}
