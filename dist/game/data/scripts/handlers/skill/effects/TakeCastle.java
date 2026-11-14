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
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.CastleSide;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code TakeCastle} skill effect.<br>
 * It manages the transition of castle ownership between different sides.
 * @author Adry_85, St3eT
 */
public class TakeCastle extends AbstractEffect
{
	private final CastleSide _side;
	
	/**
	 * Creates a new {@code TakeCastle} effect.<br>
	 * This constructor initializes the castle side from the provided parameters.
	 * @param params The {@code StatSet} containing the required configuration data.
	 */
	public TakeCastle(StatSet params)
	{
		_side = params.getEnum("side", CastleSide.class);
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of taking a castle.<br>
	 * This method checks if the {@code effector} is a player.<br>
	 * It then engraves the castle side onto the {@code effected} creature.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} involved in the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer())
		{
			return;
		}
		
		final Castle castle = CastleManager.getInstance().getCastle(effector);
		castle.engrave(effector.getClan(), effected, _side);
	}
}
