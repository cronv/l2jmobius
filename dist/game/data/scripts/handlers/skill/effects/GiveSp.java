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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Instant effect that grants SP to the effector when both it and the effected are alive players.
 * @author Adry_85
 */
public class GiveSp extends AbstractEffect
{
	private final int _sp;
	
	/**
	 * Creates a {@code GiveSp} effect reading the SP amount from the skill parameters.
	 * @param params The {@code StatSet} of skill parameters.
	 */
	public GiveSp(StatSet params)
	{
		_sp = params.getInt("sp", 0);
	}
	
	/**
	 * Indicates whether this effect applies instantly.
	 * @return {@code true} always.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Grants the configured SP to the effector when both it and the effected are players and the effected is alive.
	 * @param effector The {@code Creature} applying the effect.
	 * @param effected The {@code Creature} the effect is applied to.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} involved, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer() || !effected.isPlayer() || effected.isAlikeDead())
		{
			return;
		}
		
		effector.asPlayer().addExpAndSp(0, _sp);
	}
}
