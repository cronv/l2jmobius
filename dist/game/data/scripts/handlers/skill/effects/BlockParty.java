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
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code BlockParty} skill effect.<br>
 * It manages how the effect interacts with entities and applies relevant mechanics.
 * @author BiggBoss
 */
public class BlockParty extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code BlockParty} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public BlockParty(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * It returns {@code true} only if the target is not {@code null}.<br>
	 * The target must also be a player character.
	 * @param effector The creature casting the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effected != null) && effected.isPlayer();
	}
	
	/**
	 * Handles the logic when this effect begins.<br>
	 * It applies a party ban punishment to the target creature.
	 * @param effector The {@code Creature} who triggered the effect.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} used to trigger the effect.
	 * @param item The {@code Item} associated with the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		PunishmentManager.getInstance().startPunishment(new PunishmentTask(0, effected.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.PARTY_BAN, 0, "Party banned by bot report", "system", true));
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the party ban punishment from the target creature.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who was affected by the skill.
	 * @param skill The {@code Skill} used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		PunishmentManager.getInstance().stopPunishment(effected.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.PARTY_BAN);
	}
}
