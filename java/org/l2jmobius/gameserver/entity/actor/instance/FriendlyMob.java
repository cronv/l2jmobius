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

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

/**
 * This class represents friendly mobs located within the game world.<br>
 * These entities are always in an aggressive state and only attack players with a {@code karma} value greater than {@code 0}.
 */
public class FriendlyMob extends Attackable
{
	/**
	 * Creates a new {@code FriendlyMob} instance.<br>
	 * This constructor initializes the mob using the provided template.<br>
	 * It sets the {@code InstanceType} to {@code InstanceType.FriendlyMob}.
	 * @param template The {@code NpcTemplate} used to define the mob's properties.
	 */
	public FriendlyMob(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.FriendlyMob);
	}
	
	/**
	 * Checks if this mob will automatically attack a specific creature.<br>
	 * It determines aggression based on the attacker's reputation.
	 * @param attacker The {@code Creature} attempting to initiate an attack.
	 * @return {@code true} if the mob should attack, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker.isPlayer())
		{
			return attacker.asPlayer().getReputation() < 0;
		}
		
		return super.isAutoAttackable(attacker);
	}
	
	/**
	 * Checks if this entity behaves aggressively.<br>
	 * This method always returns {@code true} for friendly mobs.
	 * @return {@code true} if the mob is aggressive.
	 */
	@Override
	public boolean isAggressive()
	{
		return true;
	}
}
