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
package handlers.skill.effects;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This effect pulls an affected target back toward the effector.<br>
 * It is used to handle knockback or pulling mechanics in skills.
 * @author Nik, Atronic
 */
public class PullBack extends AbstractEffect
{
	private final int _speed;
	private final int _delay;
	private final int _animationSpeed;
	private final FlyType _type;
	
	/**
	 * Creates a new {@code PullBack} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets properties like speed, delay, and animation type.
	 * @param params The {@code StatSet} containing the configuration values for this effect.
	 */
	public PullBack(StatSet params)
	{
		_speed = params.getInt("speed", 0);
		_delay = params.getInt("delay", _speed);
		_animationSpeed = params.getInt("animationSpeed", 0);
		_type = params.getEnum("type", FlyType.class, FlyType.WARP_FORWARD); // type 9
	}
	
	/**
	 * Calculates the success probability of the pull back effect.<br>
	 * It uses {@code Formulas.calcProbability} to determine if the action succeeds.
	 * @param effector The creature performing the action.
	 * @param effected The creature being pulled.
	 * @param skill The skill used to trigger the effect.
	 * @return {@code true} if the success roll is successful, otherwise {@code false}.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcProbability(100, effector, effected, skill);
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the pull back effect immediately.<br>
	 * This method checks if the target is valid and can move to the effector's location.<br>
	 * If successful, it moves the {@code effected} creature to a valid destination.
	 * @param effector The creature performing the action.
	 * @param effected The creature being pulled back.
	 * @param skill The skill used to trigger this effect.
	 * @param item The item associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		// Prevent pulling raids, town NPCs and anything a script pinned in place.
		if ((effected == null) || effected.isRaid() || (effected.isNpc() && !effected.isAttackable()) || effected.isImmobilized())
		{
			return;
		}
		
		// Prevent pulling debuff blocked characters.
		if ((effected.isDebuffBlocked()))
		{
			return;
		}
		
		// In retail, you get debuff, but you are not even moved if there is obstacle. You are still disabled from using skills and moving though.
		final int ex = effected.getX();
		final int ey = effected.getY();
		final int ez = effected.getZ();
		final int ex2 = effector.getX();
		final int ey2 = effector.getY();
		final int ez2 = effector.getZ();
		final Instance world = effected.getInstanceWorld();
		
		if (GeoEngine.getInstance().canMoveToTarget(ex, ey, ez, ex2, ey2, ez2, world))
		{
			final Location dest = GeoEngine.getInstance().getValidLocation(ex, ey, ez, ex2, ey2, ez2 + 20, world);
			effected.broadcastPacket(new FlyToLocation(effected, dest, _type, _speed, _delay, _animationSpeed));
			effected.setXYZ(dest);
			effected.broadcastPacket(new ValidateLocation(effected), false);
			effected.revalidateZone(true);
		}
	}
}
