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
 * This class handles the {@code EnlargeAbnormalSlot} skill effect.<br>
 * It manages the logic for increasing abnormal slot capacity on a target.
 * @author Zoey76
 */
public class EnlargeAbnormalSlot extends AbstractEffect
{
	private final int _slots;
	
	/**
	 * Creates a new instance of the {@code EnlargeAbnormalSlot} effect.<br>
	 * This constructor initializes the effect using provided statistics.
	 * @param params A {@code StatSet} containing the configuration values.
	 */
	public EnlargeAbnormalSlot(StatSet params)
	{
		_slots = params.getInt("slots", 0);
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * It verifies that both creatures are not {@code null}.<br>
	 * It also ensures that the {@code effected} creature is a player.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effector != null) && (effected != null) && effected.isPlayer();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It increases the maximum buff count of the {@code effected} creature by a specific amount.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.getStat().setMaxBuffCount(effected.getStat().getMaxBuffCount() + _slots);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It reduces the maximum buff count of the {@code effected} creature.<br>
	 * The reduction amount is based on the {@code _slots} value.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The skill used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.getStat().setMaxBuffCount(Math.max(0, effected.getStat().getMaxBuffCount() - _slots));
	}
}
