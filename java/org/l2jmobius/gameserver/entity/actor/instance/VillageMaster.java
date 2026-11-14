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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowCreatePledge;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillLaunched;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a village master NPC that provides various services to players.<br>
 * This class handles interactions such as skill learning, pledge creation, and clan management.
 */
public class VillageMaster extends Folk
{
	/**
	 * Creates a new instance of a {@code VillageMaster}.<br>
	 * This constructor initializes the actor using the provided template.<br>
	 * It sets the {@code InstanceType} to {@code VillageMaster}.
	 * @param template The {@code NpcTemplate} used to create this master.
	 */
	public VillageMaster(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.VillageMaster);
	}
	
	/**
	 * Checks if the creature can be attacked automatically by a specific attacker.<br>
	 * This method returns {@code true} if the attacker is a monster.<br>
	 * Otherwise, it uses the default behavior from the parent class.
	 * @param attacker The {@code Creature} attempting to perform an auto-attack.
	 * @return {@code true} if the attack is allowed, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker.isMonster())
		{
			return true;
		}
		
		return super.isAutoAttackable(attacker);
	}
	
	/**
	 * Retrieves the file path for a specific village master HTML template.<br>
	 * The path is constructed based on the provided NPC ID and value.
	 * @param npcId The unique identifier of the NPC.
	 * @param value An additional value used to differentiate templates; if 0, only the NPC ID is used.
	 * @param player The {@code Player} object associated with the request.
	 * @return A {@code String} representing the full path to the .htm file.
	 */
	@Override
	public String getHtmlPath(int npcId, int value, Player player)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		
		return "data/html/villagemaster/" + pom + ".htm";
	}
	
	/**
	 * Processes custom bypass commands for clan and pledge management.<br>
	 * This method handles actions like creating clans, academies, and alliances.<br>
	 * It also manages leader changes and skill information for the player.
	 * @param player The {@code Player} who sent the command.
	 * @param command The raw string containing the command and its parameters.
	 */
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final String[] commandStr = command.split(" ");
		final String actualCommand = commandStr[0]; // Get actual command
		String cmdParams = "";
		if (commandStr.length >= 2)
		{
			cmdParams = commandStr[1];
		}
		
		if (actualCommand.equalsIgnoreCase("create_clan"))
		{
			player.sendPacket(ExShowCreatePledge.STATIC_PACKET);
		}
		else if (actualCommand.equalsIgnoreCase("create_ally"))
		{
			if (cmdParams.isEmpty())
			{
				player.sendPacket(SystemMessageId.INCORRECT_ALLIANCE_NAME_PLEASE_TRY_AGAIN);
				return;
			}
			
			final Clan clan = player.getClan();
			if (clan == null)
			{
				player.sendPacket(SystemMessageId.ONLY_CLAN_LEADERS_MAY_CREATE_ALLIANCES);
			}
			else
			{
				clan.createAlly(player, cmdParams);
			}
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_ally"))
		{
			player.getClan().dissolveAlly(player);
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_clan"))
		{
			dissolveClan(player);
		}
		else if (actualCommand.equalsIgnoreCase("change_clan_leader"))
		{
			if (cmdParams.isEmpty())
			{
				player.sendPacket(SystemMessageId.PLEASE_ENTER_YOUR_CLAN_NAME);
				return;
			}
			
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}
			
			if (player.getName().equalsIgnoreCase(cmdParams))
			{
				return;
			}
			
			final Clan clan = player.getClan();
			final ClanMember member = clan.getClanMember(cmdParams);
			if (member == null)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
				sm.addString(cmdParams);
				player.sendPacket(sm);
				return;
			}
			
			if (!member.isOnline())
			{
				player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_CURRENTLY_ONLINE);
				return;
			}
			
			// To avoid clans with null clan leader, academy members shouldn't be eligible for clan leader.
			if (member.getPlayer().isAcademyMember())
			{
				player.sendPacket(SystemMessageId.THAT_PRIVILEGE_CANNOT_BE_GRANTED_TO_A_CLAN_ACADEMY_MEMBER);
				return;
			}
			
			if (PlayerConfig.ALT_CLAN_LEADER_INSTANT_ACTIVATION)
			{
				clan.setNewLeader(member);
			}
			else
			{
				final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
				if (clan.getNewLeaderId() == 0)
				{
					clan.setNewLeaderId(member.getObjectId(), true);
					msg.setFile(player, "data/scripts/village_master/ClanMaster/9000-07-success.htm");
				}
				else
				{
					msg.setFile(player, "data/scripts/village_master/ClanMaster/9000-07-in-progress.htm");
				}
				
				player.sendPacket(msg);
			}
		}
		else if (actualCommand.equalsIgnoreCase("cancel_clan_leader_change"))
		{
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}
			
			final Clan clan = player.getClan();
			final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
			if (clan.getNewLeaderId() != 0)
			{
				clan.setNewLeaderId(0, true);
				msg.setFile(player, "data/scripts/village_master/ClanMaster/9000-07-canceled.htm");
			}
			else
			{
				msg.setHtml("<html><body>You don't have clan leader delegation applications submitted yet!</body></html>");
			}
			
			player.sendPacket(msg);
		}
		else if (actualCommand.equalsIgnoreCase("recover_clan"))
		{
			recoverClan(player);
		}
		else if (actualCommand.equalsIgnoreCase("increase_clan_level"))
		{
			if (player.getClan().levelUpClan(player))
			{
				player.broadcastSkillPacket(new MagicSkillUse(player, 5103, 1, 0, 0), player);
				player.broadcastSkillPacket(new MagicSkillLaunched(player, 5103, 1), player);
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	/**
	 * Initiates the dissolution process for the player's clan.<br>
	 * Validates that the {@code Player} is a leader and meets all requirements.<br>
	 * Sets an expiry time for the dissolution based on {@code PlayerConfig}.<br>
	 * Applies an experience penalty to the {@code Player} and schedules removal from {@code ClanTable}.
	 * @param player The {@code Player} who is attempting to dissolve their clan.
	 */
	private void dissolveClan(Player player)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final Clan clan = player.getClan();
		if (clan.getAllyId() != 0)
		{
			player.sendPacket(SystemMessageId.THE_SELECTED_CLAN_HAS_APPLIED_FOR_DISBANDMENT_2);
			return;
		}
		
		if (clan.isAtWar())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_A_CLAN_WHILE_ENGAGED_IN_A_WAR);
			return;
		}
		
		if ((clan.getCastleId() != 0) || (clan.getHideoutId() != 0) || (clan.getFortId() != 0))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DISBAND_THE_CLAN_THAT_OWNS_A_CLAN_HALL_CASTLE);
			return;
		}
		
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (SiegeManager.getInstance().checkIsRegistered(clan, castle.getResidenceId()))
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_A_CLAN_DURING_A_SIEGE_OR_WHILE_PROTECTING_A_CASTLE);
				return;
			}
		}
		
		for (Fort fort : FortManager.getInstance().getForts())
		{
			if (FortSiegeManager.getInstance().checkIsRegistered(clan, fort.getResidenceId()))
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_A_CLAN_DURING_A_SIEGE_OR_WHILE_PROTECTING_A_CASTLE);
				return;
			}
		}
		
		if (player.isInsideZone(ZoneId.SIEGE))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_A_CLAN_DURING_A_SIEGE_OR_WHILE_PROTECTING_A_CASTLE);
			return;
		}
		
		if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_THE_DISSOLUTION_OF_S1_CLAN);
			sm.addString(clan.getName());
			player.sendPacket(sm);
			return;
		}
		
		clan.setDissolvingExpiryTime(System.currentTimeMillis() + (PlayerConfig.ALT_CLAN_DISSOLVE_DAYS * 86400000)); // 24*60*60*1000 = 86400000
		clan.updateClanInDB();
		
		// The clan leader should take the XP penalty of a full death.
		player.calculateDeathExpPenalty(null);
		ClanTable.getInstance().scheduleRemoveClan(clan.getId());
	}
	
	/**
	 * Cancels the dissolution process for a clan.<br>
	 * This method checks if the {@code Player} is the leader of the clan.<br>
	 * If authorized, it resets the expiry time and updates the database.
	 * @param player The {@code Player} who is attempting to recover the clan.
	 */
	private void recoverClan(Player player)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final Clan clan = player.getClan();
		if (clan.getDissolvingExpiryTime() == 0)
		{
			player.sendPacket(SystemMessageId.THERE_ARE_NO_REQUESTS_TO_DISPERSE);
			return;
		}
		
		clan.setDissolvingExpiryTime(0);
		clan.updateClanInDB();
	}
}
