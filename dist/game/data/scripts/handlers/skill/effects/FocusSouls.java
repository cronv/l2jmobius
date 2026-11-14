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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code FocusSouls} skill effect.<br>
 * It manages how the effect interacts with entities and modifies game stats.
 * @author nBd, Adry_85
 */
public class FocusSouls extends AbstractEffect
{
	private final int _charge;
	
	/**
	 * Creates a new instance of the {@code FocusSouls} effect.<br>
	 * It initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public FocusSouls(StatSet params)
	{
		_charge = params.getInt("charge", 0);
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
	 * Applies the instant effect of this skill to a target.<br>
	 * It increases the soul count of the {@code effected} creature if they are a {@code Player}.<br>
	 * The amount added depends on the current charge and maximum souls.
	 * @param effector The creature who performs the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayer() || effected.isAlikeDead())
		{
			return;
		}
		
		final Player target = effected.asPlayer();
		final int maxSouls = (int) target.getStat().getValue(Stat.MAX_SOULS, 0);
		if (maxSouls > 0)
		{
			final int amount = _charge;
			final int chargedSouls = target.getChargedSouls();
			if ((chargedSouls < maxSouls))
			{
				final int count = ((chargedSouls + amount) <= maxSouls) ? amount : (maxSouls - chargedSouls);
				target.increaseSouls(count);
			}
			else
			{
				target.sendPacket(SystemMessageId.SOUL_CANNOT_BE_INCREASED_ANYMORE);
			}
		}
	}
}
