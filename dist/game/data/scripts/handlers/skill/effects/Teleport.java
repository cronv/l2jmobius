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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.games.AerialCleftManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateiArena;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the teleportation logic for skill effects.<br>
 * It manages moving a {@code Creature} to a new {@code Location}.
 * @author Adry_85
 */
public class Teleport extends AbstractEffect
{
	private final Location _loc;
	
	/**
	 * Creates a new {@code Teleport} effect.<br>
	 * This constructor initializes the destination coordinates from the provided data.
	 * @param params The {@code StatSet} containing "x", "y", and "z" coordinate values.
	 */
	public Teleport(StatSet params)
	{
		_loc = new Location(params.getInt("x", 0), params.getInt("y", 0), params.getInt("z", 0));
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this teleport effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.TELEPORT;
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
	 * Executes an instant teleport effect.<br>
	 * This method moves the {@code effected} creature to a specific location.<br>
	 * It handles special logic for players in arenas or specific states.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the teleportation.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayer())
		{
			final Player player = effected.asPlayer();
			if (!AerialCleftManager.getInstance().canEscape(player.getObjectId()))
			{
				return;
			}
			
			final KrateiArena arena = player.getKrateiArena();
			if (arena != null)
			{
				arena.removePlayer(player);
			}
			else if (player.getUCState() != Player.UC_STATE_NONE)
			{
				return;
			}
		}
		
		effected.teleToLocation(_loc, true, null);
	}
}
