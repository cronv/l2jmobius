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

import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

/**
 * This class manages the behavior and logic for all Grand Bosses.<br>
 * It serves as a specialized entity type for high-level boss encounters.
 */
public class GrandBoss extends Monster
{
	private boolean _useRaidCurse = true;
	
	/**
	 * Creates a new {@code GrandBoss} instance.<br>
	 * This constructor initializes the boss with specific raid properties.<br>
	 * It sets the {@code InstanceType} to {@code GrandBoss}.<br>
	 * It marks the entity as a raid and makes it non-lethalable.
	 * @param template The {@code NpcTemplate} used to create this boss.
	 */
	public GrandBoss(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.GrandBoss);
		setIsRaid(true);
		setLethalable(false);
	}
	
	/**
	 * This method is called when the {@code GrandBoss} instance is first created.<br>
	 * It disables random walking for this entity.<br>
	 * It also calls the {@code onSpawn()} method of the parent class.
	 */
	@Override
	public void onSpawn()
	{
		setRandomWalking(false);
		super.onSpawn();
	}
	
	/**
	 * Calculates the vitality points for a specific entity.<br>
	 * This method returns the negative value of the base calculation.
	 * @param level The current level of the entity.
	 * @param exp The experience points of the entity.
	 * @param isBoss A boolean indicating if the entity is a boss.
	 * @return The calculated vitality points as an {@code int}.
	 */
	@Override
	public int getVitalityPoints(int level, double exp, boolean isBoss)
	{
		return -super.getVitalityPoints(level, exp, isBoss);
	}
	
	/**
	 * Checks if the vitality rate should be applied.<br>
	 * This method returns a {@code boolean} value.
	 * @return {@code true} if the vitality rate is active, otherwise {@code false}.
	 */
	@Override
	public boolean useVitalityRate()
	{
		return false;
	}
	
	/**
	 * Sets whether the boss applies a raid curse.<br>
	 * Use {@code true} to enable the curse effect.<br>
	 * Use {@code false} to disable the curse effect.
	 * @param value The boolean state to set for the raid curse.
	 */
	public void setUseRaidCurse(boolean value)
	{
		_useRaidCurse = value;
	}
	
	/**
	 * Checks if the raid curse is currently active.<br>
	 * This method returns the current state of the {@code _useRaidCurse} flag.
	 * @return {@code true} if the raid curse is enabled, {@code false} otherwise.
	 */
	@Override
	public boolean giveRaidCurse()
	{
		return _useRaidCurse;
	}
}
