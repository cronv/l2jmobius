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
 * This class handles the {@code NotFriend} affect object logic based on Gracia Final retail behavior.<br>
 * It determines target hostility by checking player status, zone types, and faction relationships.
 * @author Nik
 */
public class NotFriend implements IAffectObjectHandler
{
	/**
	 * Determines if the {@code target} is considered a non-friend of the {@code creature}.<br>
	 * This method checks various conditions like party status, clan relations, and zone types.<br>
	 * It returns {@code true} if the target should be treated as an enemy.
	 * @param creature The entity performing the action.
	 * @param target The entity being targeted by the action.
	 * @return {@code true} if the target is a non-friend, {@code false} otherwise.
	 */
	@Override
	public boolean checkAffectedObject(Creature creature, Creature target)
	{
		if (creature == target)
		{
			return false;
		}
		
		final Player player = creature.asPlayer();
		final Player targetPlayer = target.asPlayer();
		if ((player != null) && (targetPlayer != null))
		{
			// Same player.
			if (player == targetPlayer)
			{
				return false;
			}
			
			// Auto play.
			if (player.isAutoPlaying())
			{
				final int targetMode = player.getAutoPlaySettings().getNextTargetMode();
				if ((targetMode == 1 /* Monster */) || (targetMode == 3 /* NPC */))
				{
					return false;
				}
			}
			
			// Peace Zone.
			if (target.isInsidePeaceZone(player) && !player.getAccessLevel().allowPeaceAttack())
			{
				return false;
			}
			
			if (PlayerConfig.ALT_COMMAND_CHANNEL_FRIENDS)
			{
				final CommandChannel playerCC = player.getCommandChannel();
				final CommandChannel targetCC = targetPlayer.getCommandChannel();
				if ((playerCC != null) && (targetCC != null) && (playerCC.getLeaderObjectId() == targetCC.getLeaderObjectId()))
				{
					return false;
				}
			}
			
			// Party (command channel doesn't make you friends).
			final Party party = player.getParty();
			final Party targetParty = targetPlayer.getParty();
			if ((party != null) && (targetParty != null) && (party.getLeaderObjectId() == targetParty.getLeaderObjectId()))
			{
				return false;
			}
			
			// Events.
			if (player.isOnEvent() && !player.isOnSoloEvent() && (player.getTeam() == target.getTeam()))
			{
				return false;
			}
			
			// Olympiad observer.
			if (targetPlayer.getObserverMode().isActive())
			{
				return false;
			}
			
			// Siege.
			if (target.isInsideZone(ZoneId.SIEGE))
			{
				// Players in the same siege side at the same castle are considered friends.
				return !player.isSiegeFriend(targetPlayer);
			}
			
			// Arena.
			if (creature.isInsideZone(ZoneId.PVP) && !creature.isInsideZone(ZoneId.SIEGE) && target.isInsideZone(ZoneId.PVP) && !target.isInsideZone(ZoneId.SIEGE))
			{
				return true;
			}
			
			// Duel.
			if (player.isInDuel() && targetPlayer.isInDuel() && (player.getDuelId() == targetPlayer.getDuelId()))
			{
				return true;
			}
			
			// Olympiad.
			if (player.isInOlympiadMode() && targetPlayer.isInOlympiadMode() && (player.getOlympiadGameId() == targetPlayer.getOlympiadGameId()))
			{
				return true;
			}
			
			// Clan.
			final Clan clan = player.getClan();
			final Clan targetClan = targetPlayer.getClan();
			if (clan != null)
			{
				if (clan == targetClan)
				{
					return false;
				}
				
				// War
				if ((targetClan != null) && clan.isAtWarWith(targetClan) && targetClan.isAtWarWith(clan))
				{
					return true;
				}
			}
			
			// Alliance.
			final int playerAllyId = player.getAllyId();
			if ((playerAllyId != 0) && (playerAllyId == targetPlayer.getAllyId()))
			{
				return false;
			}
			
			// Auto play target mode check.
			if (player.isAutoPlaying() && ((targetPlayer.getPvpFlag() == 0) || (targetPlayer.getReputation() > -1)))
			{
				final int targetMode = player.getAutoPlaySettings().getNextTargetMode();
				if ((targetMode != 0 /* Any Target */) && (targetMode != 2 /* Characters */))
				{
					return false;
				}
			}
			
			// At this point summon should be prevented from attacking friendly targets.
			if (creature.isSummon() && (target == creature.getTarget()))
			{
				return true;
			}
			
			// By default any flagged/PK player is considered enemy.
			return (targetPlayer.getPvpFlag() > 0) || (targetPlayer.getReputation() < 0);
		}
		
		return target.isAutoAttackable(creature);
	}
	
	/**
	 * Retrieves the specific type of affect object associated with this handler.<br>
	 * This method identifies the category used for target filtering.
	 * @return The {@code AffectObject} enum value representing the not friend type.
	 */
	@Override
	public Enum<AffectObject> getAffectObjectType()
	{
		return AffectObject.NOT_FRIEND;
	}
}
