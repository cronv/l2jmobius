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
package handlers.chat.commands.voiced;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.config.custom.WeddingConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerAction;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.managers.CoupleManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;

/**
 * Handles voiced commands related to the wedding system.<br>
 * This class processes specific voice inputs to trigger wedding-related actions and effects.
 * @author evill33t
 */
public class Wedding implements IVoicedCommandHandler
{
	static final Logger LOGGER = Logger.getLogger(Wedding.class.getName());
	private static final String[] _voicedCommands =
	{
		"divorce",
		"engage",
		"gotolove"
	};
	
	/**
	 * Processes voiced commands for wedding-related actions.<br>
	 * This method checks the {@code command} string and calls the appropriate logic.
	 * @param command The name of the voiced command to execute.
	 * @param activeChar The {@code Player} who sent the command.
	 * @param params Additional parameters provided with the command.
	 * @return {@code true} if the command was successfully processed, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (command.startsWith("engage"))
		{
			return engage(activeChar);
		}
		else if (command.startsWith("divorce"))
		{
			return divorce(activeChar);
		}
		else if (command.startsWith("gotolove"))
		{
			return goToLove(activeChar);
		}
		
		return false;
	}
	
	/**
	 * Ends the current marriage or engagement for the specified player.<br>
	 * This method handles adena costs and notifies the partner if they are online.<br>
	 * It removes the couple data from the {@code CoupleManager}.
	 * @param activeChar The {@code Player} who is initiating the divorce.
	 * @return {@code true} if the divorce was successful, or {@code false} if the player has no partner.
	 */
	public boolean divorce(Player activeChar)
	{
		final int partnerId = activeChar.getPartnerId();
		if (partnerId == 0)
		{
			return false;
		}
		
		final int coupleId = activeChar.getCoupleId();
		long adenaAmount = 0;
		if (activeChar.isMarried())
		{
			activeChar.sendMessage("You are now divorced.");
			adenaAmount = (activeChar.getAdena() / 100) * WeddingConfig.WEDDING_DIVORCE_COSTS;
			activeChar.getInventory().reduceAdena(ItemProcessType.FEE, adenaAmount, activeChar, null);
		}
		else
		{
			activeChar.sendMessage("You have broken up as a couple.");
		}
		
		final Player partner = World.getPlayer(partnerId);
		if (partner != null)
		{
			partner.setPartnerId(0);
			if (partner.isMarried())
			{
				partner.sendMessage("Your spouse has decided to divorce you.");
			}
			else
			{
				partner.sendMessage("Your fiance has decided to break the engagement with you.");
			}
			
			// give adena
			if (adenaAmount > 0)
			{
				partner.addAdena(ItemProcessType.REFUND, adenaAmount, null, false);
			}
		}
		
		CoupleManager.getInstance().deleteCouple(coupleId);
		return true;
	}
	
	/**
	 * Sends an engagement request to the targeted player.<br>
	 * Validates that both players are friends and not already in a relationship.<br>
	 * If successful, it sends a confirmation dialog to the target.
	 * @param activeChar The {@code Player} who is initiating the engagement request.
	 * @return {@code true} if the request was successfully sent; {@code false} otherwise.
	 */
	public boolean engage(Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		if (target == null)
		{
			activeChar.sendMessage("You have no one targeted.");
			return false;
		}
		else if (!target.isPlayer())
		{
			activeChar.sendMessage("You can only ask another player to engage you.");
			return false;
		}
		else if (activeChar.getPartnerId() != 0)
		{
			activeChar.sendMessage("You are already engaged.");
			if (WeddingConfig.WEDDING_PUNISH_INFIDELITY)
			{
				activeChar.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.BIG_HEAD); // Give player a Big Head.
				
				// Lets recycle the sevensigns debuffs.
				int skillId;
				int skillLevel = 1;
				if (activeChar.getLevel() > 40)
				{
					skillLevel = 2;
				}
				
				if (activeChar.isMageClass())
				{
					skillId = 4362;
				}
				else
				{
					skillId = 4361;
				}
				
				final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
				if (!activeChar.isAffectedBySkill(skillId))
				{
					skill.applyEffects(activeChar, activeChar);
				}
			}
			
			return false;
		}
		
		final Player ptarget = target.asPlayer();
		
		// check if player target himself
		final int activeCharObjectId = activeChar.getObjectId();
		if (ptarget.getObjectId() == activeCharObjectId)
		{
			activeChar.sendMessage("Is there something wrong with you, are you trying to go out with youself?");
			return false;
		}
		
		if (ptarget.isMarried())
		{
			activeChar.sendMessage("Player already married.");
			return false;
		}
		
		if (ptarget.isEngageRequest())
		{
			activeChar.sendMessage("Player already asked by someone else.");
			return false;
		}
		
		if (ptarget.getPartnerId() != 0)
		{
			activeChar.sendMessage("Player already engaged with someone else.");
			return false;
		}
		
		if ((ptarget.getAppearance().isFemale() == activeChar.getAppearance().isFemale()) && !WeddingConfig.WEDDING_SAMESEX)
		{
			activeChar.sendMessage("Gay marriage is not allowed on this server!");
			return false;
		}
		
		// check if target has player on friendlist
		boolean foundOnFriendList = false;
		int objectId;
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=?");
			statement.setInt(1, ptarget.getObjectId());
			final ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				objectId = rset.getInt("friendId");
				if (objectId == activeCharObjectId)
				{
					foundOnFriendList = true;
				}
			}
			
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.warning("could not read friend data:" + e);
		}
		
		if (!foundOnFriendList)
		{
			activeChar.sendMessage("The player you want to ask is not on your friends list, you must first be on each others friends list before you choose to engage.");
			return false;
		}
		
		ptarget.setEngageRequest(true, activeCharObjectId);
		ptarget.addAction(PlayerAction.USER_ENGAGE);
		
		final ConfirmDlg dlg = new ConfirmDlg(activeChar.getName() + " is asking to engage you. Do you want to start a new relationship?");
		dlg.addTime(15 * 1000);
		ptarget.sendPacket(dlg);
		return true;
	}
	
	/**
	 * Teleports the player to their married partner.<br>
	 * Checks all requirements like marriage status and online presence.<br>
	 * Deducts the required fee from the player's Adena.<br>
	 * Starts a teleport animation and moves the player after a delay.
	 * @param activeChar The {@code Player} who is initiating the teleport.
	 * @return {@code true} if the teleport was successfully started, {@code false} otherwise.
	 */
	public boolean goToLove(Player activeChar)
	{
		final int teleportTimer = WeddingConfig.WEDDING_TELEPORT_DURATION * 1000;
		
		if (!WeddingConfig.WEDDING_TELEPORT)
		{
			activeChar.sendMessage("The 'Go to Love' teleport feature has been disabled.");
			return false;
		}
		
		if (!activeChar.isMarried())
		{
			activeChar.sendMessage("You're not married.");
			return false;
		}
		
		final int partnerId = activeChar.getPartnerId();
		if (partnerId == 0)
		{
			activeChar.sendMessage("Couldn't find your fiance in the Database - Inform a Gamemaster.");
			LOGGER.severe("Married but couldn't find parter for " + activeChar.getName());
			return false;
		}
		
		if (activeChar.isCombatFlagEquipped())
		{
			activeChar.sendMessage("While you are holding a Combat Flag or Territory Ward you can't go to your love!");
			return false;
		}
		
		if (activeChar.isCursedWeaponEquipped())
		{
			activeChar.sendMessage("While you are holding a Cursed Weapon you can't go to your love!");
			return false;
		}
		
		if (activeChar.isJailed())
		{
			activeChar.sendMessage("You are in Jail!");
			return false;
		}
		
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("You are in the Olympiad now.");
			return false;
		}
		
		if (activeChar.isRegisteredOnEvent())
		{
			activeChar.sendMessage("You are registered in an event.");
			return false;
		}
		
		if (activeChar.isInDuel())
		{
			activeChar.sendMessage("You are in a duel!");
			return false;
		}
		
		if (activeChar.getObserverMode().isActive())
		{
			activeChar.sendMessage("You are in the observation.");
			return false;
		}
		
		final Siege activeSiege = SiegeManager.getInstance().getSiege(activeChar);
		if ((activeSiege != null) && activeSiege.isInProgress())
		{
			activeChar.sendMessage("You are in a siege, you cannot go to your partner.");
			return false;
		}
		
		if (activeChar.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
		{
			activeChar.sendMessage("You are in area which blocks summoning.");
			return false;
		}
		
		final Player partner = World.getPlayer(partnerId);
		
		if ((partner == null) || !partner.isOnline())
		{
			activeChar.sendMessage("Your partner is not online.");
			return false;
		}
		
		if (activeChar.getInstanceId() != partner.getInstanceId())
		{
			activeChar.sendMessage("Your partner is in another World!");
			return false;
		}
		
		if (partner.isJailed())
		{
			activeChar.sendMessage("Your partner is in Jail.");
			return false;
		}
		
		if (partner.isCursedWeaponEquipped())
		{
			activeChar.sendMessage("Your partner is holding a Cursed Weapon and you can't go to your love!");
			return false;
		}
		
		if (partner.isInOlympiadMode())
		{
			activeChar.sendMessage("Your partner is in the Olympiad now.");
			return false;
		}
		
		if (partner.isRegisteredOnEvent())
		{
			activeChar.sendMessage("Your partner is registered in an event.");
			return false;
		}
		
		if (partner.isInDuel())
		{
			activeChar.sendMessage("Your partner is in a duel.");
			return false;
		}
		
		if (partner.getObserverMode().isActive())
		{
			activeChar.sendMessage("Your partner is in the observation.");
			return false;
		}
		
		final Siege partnerSiege = SiegeManager.getInstance().getSiege(partner);
		if ((partnerSiege != null) && partnerSiege.isInProgress())
		{
			activeChar.sendMessage("Your partner is in a siege, you cannot go to your partner.");
			return false;
		}
		
		if (partner.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
		{
			activeChar.sendMessage("Your partner is in area which blocks summoning.");
			return false;
		}
		
		if (!activeChar.reduceAdena(ItemProcessType.FEE, WeddingConfig.WEDDING_TELEPORT_PRICE, null, true))
		{
			return false; // Player already informed by system message inside reduceAdena.
		}
		
		String formattedTime = TimeUtil.formatDuration(teleportTimer);
		activeChar.sendMessage("You will be teleported to your partner in " + formattedTime + ".");
		
		activeChar.getAI().setIntentionIdle();
		
		// SoE Animation section.
		activeChar.setTarget(activeChar);
		activeChar.disableAllSkills();
		
		activeChar.broadcastSkillPacket(new MagicSkillUse(activeChar, 1050, 1, teleportTimer, 0), activeChar);
		activeChar.sendPacket(new SetupGauge(activeChar.getObjectId(), 0, teleportTimer));
		// End SoE Animation section.
		
		final EscapeFinalizer ef = new EscapeFinalizer(activeChar, partner.getLocation());
		
		// continue execution later
		// activeChar.setSkillCast(ThreadPool.schedule(ef, teleportTimer));
		// activeChar.forceIsCasting(GameTimeTaskManager.getInstance().getGameTicks() + (teleportTimer / GameTimeTaskManager.MILLIS_IN_TICK));
		ThreadPool.schedule(ef, teleportTimer);
		return true;
	}
	
	private static class EscapeFinalizer implements Runnable
	{
		private final Player _player;
		private final Location _partnerLoc;
		
		EscapeFinalizer(Player activeChar, Location loc)
		{
			_player = activeChar;
			_partnerLoc = loc;
		}
		
		@Override
		public void run()
		{
			if (_player.isDead())
			{
				return;
			}
			
			if ((SiegeManager.getInstance().getSiege(_partnerLoc) != null) && SiegeManager.getInstance().getSiege(_partnerLoc).isInProgress())
			{
				_player.sendMessage("Your partner is in siege, you can't go to your partner.");
				return;
			}
			
			_player.enableAllSkills();
			
			try
			{
				_player.teleToLocation(_partnerLoc);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "", e);
			}
		}
	}
	
	/**
	 * Retrieves the list of available voiced commands.<br>
	 * This method returns all valid strings for wedding actions.
	 * @return a {@code String[]} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return _voicedCommands;
	}
}
