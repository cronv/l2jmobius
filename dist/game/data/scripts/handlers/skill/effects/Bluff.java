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
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.serverpackets.StartRotation;
import org.l2jmobius.gameserver.network.serverpackets.StopRotation;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code Bluff} effect logic.<br>
 * It manages how the bluff status affects a creature in the game world.
 * @author decad
 */
public class Bluff extends AbstractEffect
{
	private final int _chance;
	
	/**
	 * Creates a new {@code Bluff} effect instance.<br>
	 * This constructor initializes the effect using provided statistics.<br>
	 * It sets the success chance from the {@code StatSet}.
	 * @param params The {@code StatSet} containing configuration values.
	 */
	public Bluff(StatSet params)
	{
		_chance = params.getInt("chance", 100);
	}
	
	/**
	 * Determines if the {@code Bluff} effect succeeds.<br>
	 * It calculates the success probability based on the current chance.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @return {@code true} if the effect succeeds, otherwise {@code false}.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcProbability(_chance, effector, effected, skill);
	}
	
	/**
	 * Checks if the effect happens instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant visual effect for a skill.<br>
	 * This method handles rotation changes for the target creature.<br>
	 * It prevents specific NPCs and raid entities from rotating.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		// Headquarters NPC should not rotate.
		if ((effected.getId() == 35062) || effected.isRaid() || effected.isRaidMinion())
		{
			return;
		}
		
		final int objectId = effected.getObjectId();
		final int effectorHeading = effector.getHeading();
		effected.broadcastPacket(new StartRotation(objectId, effected.getHeading(), 1, 65535));
		effected.broadcastPacket(new StopRotation(objectId, effectorHeading, 65535));
		effected.setHeading(effectorHeading);
	}
}
