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
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the teleportation of a {@code Player} or a {@code Party} to a summoned {@code Npc}.<br>
 * This class implements the logic for moving entities to a specific NPC location as part of a skill effect.
 * @author Nik
 */
public class TeleportToNpc extends AbstractEffect
{
	private final int _npcId;
	private final boolean _party;
	
	/**
	 * This constructor initializes the teleport effect.<br>
	 * It extracts the required data from a {@code StatSet}.<br>
	 * The values are used to determine the destination and target type.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public TeleportToNpc(StatSet params)
	{
		_npcId = params.getInt("npcId");
		_party = params.getBoolean("party", false);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.TELEPORT_TO_TARGET;
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
	 * Executes the teleport effect immediately.<br>
	 * It moves the target to a summoned NPC location.<br>
	 * This method handles both individual and party teleportation.
	 * @param effector The {@code Creature} who cast the skill.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		ILocational teleLocation = null;
		for (Npc npc : effector.getSummonedNpcs())
		{
			if (npc.getId() == _npcId)
			{
				teleLocation = npc;
			}
		}
		
		if (teleLocation != null)
		{
			final Party party = effected.getParty();
			if (_party && (party != null))
			{
				for (Player member : party.getMembers())
				{
					teleport(member, teleLocation);
				}
			}
			else
			{
				teleport(effected, teleLocation);
			}
		}
	}
	
	/**
	 * Moves a {@code Creature} to a specific {@code ILocational} coordinate.<br>
	 * This method checks if the target is within a certain radius before moving.<br>
	 * It handles packet broadcasting and state resets during the teleport.
	 * @param effected The {@code Creature} that will be moved.
	 * @param location The destination {@code ILocational} for the teleport.
	 */
	private void teleport(Creature effected, ILocational location)
	{
		if (effected.isInsideRadius2D(location, 900))
		{
			effected.getAI().setIntentionIdle();
			effected.broadcastPacket(new FlyToLocation(effected, location, FlyType.DUMMY));
			effected.abortAttack();
			effected.abortCast();
			effected.setXYZ(location);
			effected.broadcastPacket(new ValidateLocation(effected));
			effected.revalidateZone(true);
		}
		else
		{
			effected.teleToLocation(location);
		}
	}
}
