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
 * This class implements the {@code Immobile} buff effect.<br>
 * It prevents a {@code Creature} from moving while the effect is active.
 * @author mkizub
 */
public class BlockMove extends AbstractEffect
{
	/**
	 * Creates a new instance of {@code BlockMove}.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public BlockMove(StatSet params)
	{
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It applies an immobilization status to the target creature.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} used to trigger this effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.setImmobilized(true);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the immobilization status from the target creature.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.setImmobilized(false);
	}
}
