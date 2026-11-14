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

import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code Hide} effect.<br>
 * It manages how a creature becomes invisible to other entities in the world.
 * @author ZaKaX, nBd
 */
public class Hide extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code Hide} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public Hide(StatSet params)
	{
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It makes a {@code Player} invisible and resets their AI intention.<br>
	 * It also clears targets and stops actions for nearby enemies.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to apply this effect.
	 * @param item The {@code Item} associated with the effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayer())
		{
			final Player player = effected.asPlayer();
			player.setInvisible(true);
			
			final CreatureAI playerAI = player.getAI();
			if (playerAI.getNextIntention() == Intention.ATTACK)
			{
				playerAI.setIntentionIdle();
			}
			
			World.forEachVisibleObject(player, Creature.class, target ->
			{
				if ((target != null) && (target.getTarget() == player))
				{
					if (target.isAttackable())
					{
						target.asAttackable().clearAggroList();
					}
					target.setTarget(null);
					target.abortAttack();
					target.abortCast();
					target.getAI().setIntentionIdle();
				}
			});
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It handles logic for removing effects from the target.<br>
	 * Specifically, it makes a {@code Player} visible if they were hidden.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that was affected by the skill.
	 * @param skill The {@code Skill} used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isPlayer())
		{
			final Player player = effected.asPlayer();
			if (!player.getObserverMode().isActive())
			{
				player.setInvisible(false);
			}
		}
	}
}
