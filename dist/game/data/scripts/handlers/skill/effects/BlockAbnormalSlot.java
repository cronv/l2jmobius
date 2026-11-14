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

import java.util.EnumSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class implements the {@code Block Buff Slot} effect.<br>
 * It prevents the application of certain types of abnormal effects to a target.
 * @author Zoey76
 */
public class BlockAbnormalSlot extends AbstractEffect
{
	private final Set<AbnormalType> _blockAbnormalSlots = EnumSet.noneOf(AbnormalType.class);
	
	/**
	 * Creates a new {@code BlockAbnormalSlot} effect.<br>
	 * This constructor initializes the blocked slots from the provided parameters.<br>
	 * It parses the "slot" string to identify which {@code AbnormalType} values to block.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public BlockAbnormalSlot(StatSet params)
	{
		for (String slot : params.getString("slot").split(";"))
		{
			if (!slot.isEmpty())
			{
				_blockAbnormalSlots.add(Enum.valueOf(AbnormalType.class, slot));
			}
		}
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It adds blocked abnormal types to the {@code effected} creature.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} used to trigger this effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.getEffectList().addBlockedAbnormalTypes(_blockAbnormalSlots);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the blocked abnormal types from the {@code effected} creature.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.getEffectList().removeBlockedAbnormalTypes(_blockAbnormalSlots);
	}
}
