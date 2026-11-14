/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.skill.effects;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.instance.Defender;
import org.l2jmobius.gameserver.entity.actor.instance.FortCommander;
import org.l2jmobius.gameserver.entity.actor.instance.SiegeFlag;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code Fear} status effect.<br>
 * It manages how affected creatures behave when under this debuff.
 * @author littlecrow
 */
public class Fear extends AbstractEffect
{
	private static final int FEAR_RANGE = 500;
	
	/**
	 * Creates a new instance of the {@code Fear} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values for this effect.
	 */
	public Fear(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * It returns the value associated with {@code EffectFlag.FEAR}.
	 * @return The bitmask representing the fear effect flag.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.FEAR.getMask();
	}
	
	/**
	 * Checks if the fear effect can be applied to a target.<br>
	 * It returns {@code false} if the target is {@code null} or a raid monster.<br>
	 * The target must be a player, a summon, or an attackable creature that is not a siege object.
	 * @param effector The creature casting the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		if ((effected == null) || effected.isRaid())
		{
			return false;
		}
		
		return effected.isPlayer() || effected.isSummon() || (effected.isAttackable() //
			&& !((effected instanceof Defender) || (effected instanceof FortCommander) //
				|| (effected instanceof SiegeFlag) || (effected.getTemplate().getRace() == Race.SIEGE_WEAPON)));
	}
	
	/**
	 * Returns the duration of the effect in ticks.<br>
	 * This value determines how many game cycles the effect lasts.
	 * @return The number of ticks for this effect.
	 */
	@Override
	public int getTicks()
	{
		return 5;
	}
	
	/**
	 * This method is called periodically while the effect is active.<br>
	 * It triggers the specific action associated with this fear effect.<br>
	 * The method always returns {@code false}.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature currently affected by the skill.
	 * @param skill The skill that triggered this effect.
	 * @param item The item used to cast the skill, if any.
	 * @return Always returns {@code false}.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		fearAction(null, effected);
		return false;
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It triggers the specific logic for the fear action.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger this effect.
	 * @param item The item involved in the skill execution, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		fearAction(effector, effected);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It handles logic for the {@code effected} creature after the effect expires.<br>
	 * If the target is not a player, it notifies the AI to think about its next action.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that was affected by the skill.
	 * @param skill The {@code Skill} used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (!effected.isPlayer())
		{
			effected.getAI().notifyActionThink();
		}
	}
	
	/**
	 * This method handles the movement logic for a creature under the fear effect.<br>
	 * It calculates a destination based on the distance and angle between two creatures.<br>
	 * The {@code effected} creature is then commanded to move toward that location.
	 * @param effector The creature causing the fear effect.
	 * @param effected The creature currently affected by the fear.
	 */
	private void fearAction(Creature effector, Creature effected)
	{
		final double radians = Math.toRadians((effector != null) ? LocationUtil.calculateAngleFrom(effector, effected) : LocationUtil.convertHeadingToDegree(effected.getHeading()));
		
		final int posX = (int) (effected.getX() + (FEAR_RANGE * Math.cos(radians)));
		final int posY = (int) (effected.getY() + (FEAR_RANGE * Math.sin(radians)));
		final int posZ = effected.getZ();
		
		final Location destination = GeoEngine.getInstance().getValidLocation(effected.getX(), effected.getY(), effected.getZ(), posX, posY, posZ, effected.getInstanceWorld());
		effected.getAI().setIntentionMoveTo(destination);
	}
}
