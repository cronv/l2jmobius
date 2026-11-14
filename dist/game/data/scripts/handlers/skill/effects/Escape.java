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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.games.AerialCleftManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateiArena;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code Escape} skill effect.<br>
 * It defines how a creature reacts when triggered by this specific mechanic.
 * @author Adry_85
 */
public class Escape extends AbstractEffect
{
	private final TeleportWhereType _escapeType;
	
	/**
	 * Creates a new {@code Escape} effect.<br>
	 * This constructor initializes the escape type from the provided stats.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public Escape(StatSet params)
	{
		_escapeType = params.getEnum("escapeType", TeleportWhereType.class, null);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return the {@code EffectType} for this escape effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.TELEPORT;
	}
	
	/**
	 * Checks if the effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Checks if the {@code Skill} can be activated.<br>
	 * This method verifies that the {@code effected} creature is not currently blocked from escaping.<br>
	 * It returns {@code true} only if the base conditions are met and escape is allowed.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being attempted.
	 * @return {@code true} if the skill can start, {@code false} otherwise.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		// While affected by escape blocking effect you cannot use Blink or Scroll of Escape.
		return super.canStart(effector, effected, skill) && !effected.cannotEscape();
	}
	
	/**
	 * Executes the instant effect for a skill.<br>
	 * This method handles teleportation and arena removal logic.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (_escapeType == null)
		{
			return;
		}
		
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
		
		if (effected.isInInstance() && effected.asPlayer().isInTimedHuntingZone())
		{
			effected.teleToLocation(effected.asPlayer().getTimedHuntingZone().getEnterLocation(), effected.getInstanceId());
		}
		else
		{
			effected.teleToLocation(_escapeType, null);
		}
	}
}
