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
package handlers.skill.targets.affectobject;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.IAffectObjectHandler;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectObject;

/**
 * Handles skill effects that target friendly entities.<br>
 * This class identifies and applies actions to {@code Player} or {@code Creature} objects belonging to the same group.<br>
 * It ensures that only allies are affected by the specific skill logic.
 * @author Nik
 */
public class Friend implements IAffectObjectHandler
{
	/**
	 * Checks if the {@code target} is considered a friend of the {@code creature}.<br>
	 * This method evaluates various relationships such as parties, clans, and alliances.<br>
	 * It also handles specific conditions for siege, pvp zones, and npc interactions.
	 * @param creature The entity performing the action.
	 * @param target The entity being targeted.
	 * @return {@code true} if the target is a friend, {@code false} otherwise.
	 */
	@Override
	public boolean checkAffectedObject(Creature creature, Creature target)
	{
		if (creature == target)
		{
			return true;
		}
		
		final Player player = creature.asPlayer();
		final Player targetPlayer = target.asPlayer();
		if (player != null)
		{
			if (targetPlayer != null)
			{
				// Same player.
				if (player == targetPlayer)
				{
					return true;
				}
				
				if (PlayerConfig.ALT_COMMAND_CHANNEL_FRIENDS)
				{
					final CommandChannel playerCC = player.getCommandChannel();
					final CommandChannel targetCC = targetPlayer.getCommandChannel();
					if ((playerCC != null) && (targetCC != null) && (playerCC.getLeaderObjectId() == targetCC.getLeaderObjectId()))
					{
						return true;
					}
				}
				
				// Party (command channel doesn't make you friends).
				final Party party = player.getParty();
				final Party targetParty = targetPlayer.getParty();
				if ((party != null) && (targetParty != null) && (party.getLeaderObjectId() == targetParty.getLeaderObjectId()))
				{
					return true;
				}
				
				// Arena.
				if (creature.isInsideZone(ZoneId.PVP) && target.isInsideZone(ZoneId.PVP))
				{
					return false;
				}
				
				// Duel.
				if (player.isInDuel() && targetPlayer.isInDuel() && (player.getDuelId() == targetPlayer.getDuelId()))
				{
					return false;
				}
				
				// Olympiad.
				if (player.isInOlympiadMode() && targetPlayer.isInOlympiadMode() && (player.getOlympiadGameId() == targetPlayer.getOlympiadGameId()))
				{
					return false;
				}
				
				// Clan.
				final Clan clan = player.getClan();
				final Clan targetClan = targetPlayer.getClan();
				if (clan != null)
				{
					if (clan == targetClan)
					{
						return true;
					}
					
					// War
					if ((targetClan != null) && clan.isAtWarWith(targetClan) && targetClan.isAtWarWith(clan))
					{
						return false;
					}
				}
				
				// Alliance.
				final int playerAllyId = player.getAllyId();
				if ((playerAllyId != 0) && (playerAllyId == targetPlayer.getAllyId()))
				{
					return true;
				}
				
				// Siege.
				if (target.isInsideZone(ZoneId.SIEGE))
				{
					// Players in the same siege side at the same castle are considered friends.
					return player.isSiegeFriend(targetPlayer);
				}
				
				// By default any neutral non-flagged player is considered a friend.
				return (targetPlayer.getPvpFlag() == 0) && (targetPlayer.getReputation() >= 0);
			}
			
			// By default any npc that is not mob is considered friend.
			return !target.isMonster() && !target.isAutoAttackable(player);
		}
		
		return !target.isAutoAttackable(creature);
	}
	
	/**
	 * Retrieves the type of affect object associated with this handler.<br>
	 * This method identifies which {@code AffectObject} category is being handled.
	 * @return The {@code AffectObject} enum value representing the target type.
	 */
	@Override
	public Enum<AffectObject> getAffectObjectType()
	{
		return AffectObject.FRIEND;
	}
}
