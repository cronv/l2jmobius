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

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
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
 * Handles the logic for pulling a target creature toward a specific location.<br>
 * This effect calculates the movement of an entity based on a destination point.
 * @author fruit
 */
public class PullToTarget extends AbstractEffect
{
	private final int _speed;
	private final int _delay;
	private final int _animationSpeed;
	private final FlyType _type;
	private final int _radius;
	
	/**
	 * Initializes a new {@code PullToTarget} effect.<br>
	 * This constructor extracts configuration values from the provided {@code StatSet}.<br>
	 * It sets properties like speed, delay, and radius for the movement effect.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public PullToTarget(StatSet params)
	{
		_speed = params.getInt("speed", 0);
		_delay = params.getInt("delay", _speed);
		_animationSpeed = params.getInt("animationSpeed", 0);
		_type = params.getEnum("type", FlyType.class, FlyType.WARP_FORWARD); // type 9
		_radius = params.getInt("radius", 100);
	}
	
	/**
	 * Determines if the effect successfully applies to a target.<br>
	 * It calculates the success probability based on the provided {@code Skill}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used for the calculation.
	 * @return {@code true} if the effect succeeds, otherwise {@code false}.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcProbability(100, effector, effected, skill);
	}
	
	/**
	 * Checks if the effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of a skill.<br>
	 * This method pulls nearby creatures toward the target.
	 * @param effector The creature who performs the action.
	 * @param effected The primary target of the skill.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		// Check if the skill target is valid.
		if (effected == null)
		{
			return;
		}
		
		World.forEachVisibleObjectInRange(effected, Creature.class, _radius, nearby ->
		{
			if ((nearby != effected) && (nearby != effector))
			{
				flyToTarget(nearby, effected);
			}
		});
	}
	
	/**
	 * Moves a {@code Creature} to the location of another target.<br>
	 * This method checks if the movement is valid before updating coordinates.<br>
	 * It prevents moving raid mobs, non-attackable NPCs, or debuff-blocked characters.
	 * @param creature The entity that will be moved.
	 * @param flyToTarget The destination entity to move toward.
	 */
	private void flyToTarget(Creature creature, Creature flyToTarget)
	{
		// Prevent pulling raids and town NPCs.
		if (creature.isRaid() || (creature.isNpc() && !creature.isAttackable()))
		{
			return;
		}
		
		// Prevent pulling debuff blocked characters.
		if (creature.isDebuffBlocked())
		{
			return;
		}
		
		// In retail, you get debuff, but you are not even moved if there is obstacle. You are still disabled from using skills and moving though.
		final int targetX = flyToTarget.getX();
		final int targetY = flyToTarget.getY();
		final int targetZ = flyToTarget.getZ();
		
		if (GeoEngine.getInstance().canMoveToTarget(creature.getX(), creature.getY(), creature.getZ(), targetX, targetY, targetZ, flyToTarget.getInstanceWorld()))
		{
			creature.broadcastPacket(new FlyToLocation(creature, flyToTarget, _type, _speed, _delay, _animationSpeed));
			creature.setXYZ(targetX, targetY, GeoEngine.getInstance().getHeight(targetX, targetY, targetZ) + 20);
			creature.broadcastPacket(new ValidateLocation(creature), false);
			creature.revalidateZone(true);
		}
	}
}
