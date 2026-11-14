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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.ArenaParticipantsHolder;
import org.l2jmobius.gameserver.entity.actor.instance.Block;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.HandysBlockCheckerManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for the {@code FlipBlock} skill effect.<br>
 * This class manages how blocks are flipped or manipulated in the game world.
 * @author Mobius
 */
public class FlipBlock extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code FlipBlock} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public FlipBlock(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect occurs instantly.<br>
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
	 * This method checks if the target is a {@code Block} and updates its color based on the player's team.<br>
	 * It only applies changes if the player belongs to an arena with specific team requirements.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} or object receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Block block = effected instanceof Block ? (Block) effected : null;
		final Player player = effector.isPlayer() ? effector.asPlayer() : null;
		if ((block == null) || (player == null))
		{
			return;
		}
		
		final int arena = player.getBlockCheckerArena();
		if (arena != -1)
		{
			final ArenaParticipantsHolder holder = HandysBlockCheckerManager.getInstance().getHolder(arena);
			if (holder == null)
			{
				return;
			}
			
			final int team = holder.getPlayerTeam(player);
			final int color = block.getColorEffect();
			if ((team == 0) && (color == 0x00))
			{
				block.changeColor(player, holder, team);
			}
			else if ((team == 1) && (color == 0x53))
			{
				block.changeColor(player, holder, team);
			}
		}
	}
}
