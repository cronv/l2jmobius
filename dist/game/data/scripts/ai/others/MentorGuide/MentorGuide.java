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
package ai.others.MentorGuide;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.Mentee;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.Mail;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.managers.MentorManager;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLevelChanged;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerMenteeAdd;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerMenteeLeft;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerMenteeRemove;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerMenteeStatus;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerMentorStatus;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerProfessionChange;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.mentoring.ExMentorList;

/**
 * This class handles the logic for the {@code MentorGuide} AI.<br>
 * It manages interactions and behaviors related to the mentoring system.
 * @author Gnacik, UnAfraid, Mobius
 */
public class MentorGuide extends Script implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(MentorGuide.class.getName());
	
	// NPCs
	private static final int MENTOR_GUIDE = 33587;
	
	// Items
	private static final int MENTEE_CERT = 33800;
	private static final int MENTEE_MARK = 33804;
	private static final int GRADUTION_BOX = 81348;
	private static final int MENTEE_HEADPHONE = 34759;
	private static final int DIPLOMA = 33805;
	
	// Skills
	private static final SkillHolder[] MENTEE_BUFFS =
	{
		new SkillHolder(9233, 1), // Mentor's Guidance
	};
	
	// Skills
	private static final SkillHolder[] MENTEE_BUFFS_WITHOUT_MENTOR_ONLINE =
	{
		new SkillHolder(62075, 1), // Fever Melody - Mentor
		new SkillHolder(62076, 1), // Drumbeat of Serenity - Mentor
		new SkillHolder(62077, 1), // Fantasia Harmony - Mentor
	};
	protected static final SkillHolder[] MENTOR_BUFFS =
	{
		new SkillHolder(9256, 1), // Mentee's Appreciation
	};
	private static final SkillHolder MENTEE_MENTOR_SUMMON = new SkillHolder(9379, 1); // Mentee's Mentor Summon
	private static final SkillHolder MENTOR_ART_OF_SEDUCTION = new SkillHolder(18594, 1); // Mentor's Art of Seduction
	// Misc
	private static final int MAX_LEVEL = 105;
	private static final String LEVEL_UP_TITLE = "Mentee coin from Mentee leveling";
	private static final String LEVEL_UP_BODY = "Your mentee %s has reached level %d, so you are receiving some Mentee Coin. After Mentee Coin has successfully been removed and placed into your inventory please be sure to delete this letter. If your mailbox is full when any future letters are sent to you cannot be delivered and you will not receive these items.";
	private static final String MENTEE_ADDED_TITLE = "Congratulations on becoming a mentee.";
	private static final String MENTEE_ADDED_BODY = "Greetings. This is the Mentor Guide.\n\nYou will experience a world of unlimited adventures with your mentor, Exciting, isn't it?\n\nWhen you graduate from mentee status (at level 105), you will receive a graduation box.";
	private static final String MENTEE_GRADUATE_TITLE = "Congratulations on your graduation";
	private static final String MENTEE_GRADUATE_BODY = "Greetings! This is the Mentor Guide.\nCongratulations!  Did you enjoy the time with a mentor? Here is a Mentee Certificate for graduating.\n\nFind me in town, and I'll give you a Diploma if you show me your Mentee Certificatee. You'll also get a small graduation gift!\n\nNow, on to your next Adventure!";
	private static final String MENTEE_REWARD_LV_VAR = "MENTEE_REWARD_LV";
	private static final Map<Integer, Integer> MENTEE_COINS = new HashMap<>();
	
	/**
	 * Loads the mentor coin data from the data file.<br>
	 * This method parses {@code data/scripts/ai/others/MentorGuide/MentorCoins.xml}.<br>
	 * It logs the total number of loaded items to the console.
	 */
	@Override
	public void load()
	{
		parseDatapackFile("data/scripts/ai/others/MentorGuide/MentorCoins.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + MENTEE_COINS.size() + " mentee coins");
	}
	
	/**
	 * Parses the provided {@code Document} to populate the mentee coin rewards.<br>
	 * It looks for a {@code list} node containing {@code mentee} entries.<br>
	 * The method maps levels to coins based on the attributes found in the XML.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("mentee".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attributes = d.getAttributes();
						final int level = parseInteger(attributes, "level");
						final int coins = parseInteger(attributes, "coins");
						MENTEE_COINS.put(level, coins);
					}
				}
			}
		}
	}
	
	/**
	 * Private constructor for the {@code MentorGuide} class.<br>
	 * It initializes the necessary IDs and NPCs.<br>
	 * It then calls the {@code load()} method to prepare the data.
	 */
	private MentorGuide()
	{
		addFirstTalkId(MENTOR_GUIDE);
		addStartNpc(MENTOR_GUIDE);
		addTalkId(MENTOR_GUIDE);
		load();
	}
	
	/**
	 * Handles specific events triggered by an {@code Npc}.<br>
	 * It processes item exchanges and buff removals for mentees.
	 * @param event The name or content of the event to process.
	 * @param npc The {@code Npc} triggering the event.
	 * @param player The {@code Player} involved in the interaction.
	 * @return The HTML text to display or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		if (event.equalsIgnoreCase("exchange"))
		{
			if (hasQuestItems(player, MENTEE_CERT) && (player.getLevel() >= MAX_LEVEL) && player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
			{
				takeItems(player, MENTEE_CERT, 1);
				giveItems(player, DIPLOMA, 40);
				return null;
			}
			
			htmltext = "33587-04.htm";
		}
		else if (event.startsWith("REMOVE_BUFFS"))
		{
			final String[] params = event.split(" ");
			if (StringUtil.isNumeric(params[1]))
			{
				final int objectId = Integer.parseInt(params[1]);
				MentorManager.getInstance().getMentees(objectId).stream().filter(Objects::nonNull).filter(Mentee::isOnline).forEach(mentee ->
				{
					final Player menteePlayer = mentee.getPlayer();
					if (menteePlayer != null)
					{
						for (SkillHolder holder : MENTEE_BUFFS)
						{
							menteePlayer.stopSkillEffects(holder.getSkill());
						}
					}
					
					mentee.sendPacket(new ExMentorList(menteePlayer));
				});
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Returns the dialogue file for a player's first interaction with an {@code Npc}.<br>
	 * This method is used to trigger the initial conversation between a mentor and a mentee.
	 * @param npc The {@code Npc} entity that is being spoken to.
	 * @param player The {@code Player} entity who is initiating the conversation.
	 * @return A {@code String} representing the name of the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "33587-01.htm";
	}
	
	/**
	 * This method is called when a new mentee is added to a mentor.<br>
	 * It applies initial buffs to both the mentor and the mentee.<br>
	 * It updates the mentor's list and handles skill assignments.<br>
	 * Finally, it sends a reward mail to the new mentee.
	 * @param event The {@code OnPlayerMenteeAdd} event containing the mentor and mentee data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_MENTEE_ADD)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onMenteeAdded(OnPlayerMenteeAdd event)
	{
		// Starting buffs for Mentor
		for (SkillHolder sk : MENTOR_BUFFS)
		{
			sk.getSkill().applyEffects(event.getMentor(), event.getMentor());
		}
		
		// Starting buffs for Mentee when mentor is online
		for (SkillHolder sk : MENTEE_BUFFS)
		{
			sk.getSkill().applyEffects(event.getMentee(), event.getMentee());
		}
		
		// Starting buffs for Mentee
		for (SkillHolder sk : MENTEE_BUFFS_WITHOUT_MENTOR_ONLINE)
		{
			sk.getSkill().applyEffects(event.getMentee(), event.getMentee());
		}
		
		// Update mentor list
		final Player mentor = event.getMentor();
		mentor.sendPacket(new ExMentorList(mentor));
		
		// Add the mentee skill
		handleMenteeSkills(event.getMentee());
		
		// Give mentor's buffs only if he didn't had them.
		handleMentorSkills(mentor);
		
		// Send mail with the headphone
		sendMail(event.getMentee().getObjectId(), MENTEE_ADDED_TITLE, MENTEE_ADDED_BODY, MENTEE_HEADPHONE, 1);
	}
	
	/**
	 * Handles the logic when a mentee's online status changes.<br>
	 * This method applies or removes mentoring buffs based on whether the mentor is online.<br>
	 * It also sends system messages and updates the mentor list for both parties.
	 * @param event The {@code OnPlayerMenteeStatus} event containing the mentee information.
	 */
	@RegisterEvent(EventType.ON_PLAYER_MENTEE_STATUS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerMenteeStatus(OnPlayerMenteeStatus event)
	{
		final Player player = event.getMentee();
		if (event.isMenteeOnline())
		{
			final Mentee mentor = MentorManager.getInstance().getMentor(player.getObjectId());
			if (mentor != null)
			{
				// Starting buffs for Mentee
				for (SkillHolder sk : MENTEE_BUFFS_WITHOUT_MENTOR_ONLINE)
				{
					sk.getSkill().applyEffects(player, player);
				}
				
				if (mentor.isOnline())
				{
					// @formatter:off
					final long mentorBuffs = mentor.getPlayer().getEffectList().getEffects()
						.stream()
						.map(BuffInfo::getSkill)
						.filter(Skill::isMentoring)
						.count();
					// @formatter:on
					
					if (mentorBuffs != MENTOR_BUFFS.length)
					{
						// Starting buffs for Mentor
						for (SkillHolder sk : MENTOR_BUFFS)
						{
							sk.getSkill().applyEffects(mentor.getPlayer(), mentor.getPlayer());
						}
					}
					
					// Starting buffs for Mentee
					for (SkillHolder sk : MENTEE_BUFFS)
					{
						sk.getSkill().applyEffects(player, player);
					}
					
					// Add the mentee skill
					handleMenteeSkills(player);
					
					mentor.sendPacket(new SystemMessage(SystemMessageId.YOUR_MENTEE_S1_IS_ONLINE).addString(player.getName()));
					mentor.sendPacket(new ExMentorList(mentor.getPlayer()));
				}
			}
			
			player.sendPacket(new ExMentorList(player));
		}
		else
		{
			final int objectId = player.getObjectId();
			final Mentee mentor = MentorManager.getInstance().getMentor(objectId);
			if ((mentor != null) && mentor.isOnline())
			{
				if (MentorManager.getInstance().isAllMenteesOffline(mentor.getObjectId(), objectId))
				{
					MentorManager.getInstance().cancelAllMentoringBuffs(mentor.getPlayer());
				}
				
				mentor.sendPacket(new SystemMessage(SystemMessageId.YOUR_MENTEE_S1_HAS_LOGGED_OUT).addString(player.getName()));
				mentor.sendPacket(new ExMentorList(mentor.getPlayer()));
			}
		}
	}
	
	/**
	 * Handles the logic when a player's mentor status changes.<br>
	 * This method manages buff applications for both mentors and mentees based on online status.<br>
	 * It also handles system messages and updates the mentor list for affected players.
	 * @param event The {@code OnPlayerMentorStatus} event containing the mentor details.
	 */
	@RegisterEvent(EventType.ON_PLAYER_MENTOR_STATUS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerMentorStatus(OnPlayerMentorStatus event)
	{
		final Player player = event.getMentor();
		if (event.isMentorOnline())
		{
			// stop buffs removal task
			final int objectId = player.getObjectId();
			cancelQuestTimer("REMOVE_BUFFS " + objectId, null, null);
			MentorManager.getInstance().getMentees(objectId).stream().filter(Objects::nonNull).filter(Mentee::isOnline).forEach(mentee ->
			{
				// @formatter:off
				final long menteeBuffs = mentee.getPlayer().getEffectList().getEffects()
					.stream()
					.map(BuffInfo::getSkill)
					.filter(Skill::isMentoring)
					.count();
				// @formatter:on
				
				if (menteeBuffs != MENTEE_BUFFS.length)
				{
					// Starting buffs for Mentee
					for (SkillHolder sk : MENTEE_BUFFS)
					{
						sk.getSkill().applyEffects(mentee.getPlayer(), mentee.getPlayer());
					}
				}
				
				mentee.sendPacket(new SystemMessage(SystemMessageId.YOUR_MENTOR_S1_IS_ONLINE).addString(player.getName()));
				mentee.sendPacket(new ExMentorList(mentee.getPlayer()));
			});
				
			if (MentorManager.getInstance().hasOnlineMentees(objectId))
			{
				// Starting buffs for Mentor
				for (SkillHolder sk : MENTOR_BUFFS)
				{
					sk.getSkill().applyEffects(player, player);
				}
			}
			
			// Give mentor's buffs only if he didn't had them.
			handleMentorSkills(player);
			
			player.sendPacket(new ExMentorList(player));
		}
		else
		{
			final int objectId = player.getObjectId();
			startQuestTimer("REMOVE_BUFFS " + objectId, 5 * 60 * 1000, null, null);
			MentorManager.getInstance().getMentees(objectId).stream().filter(Objects::nonNull).filter(Mentee::isOnline).forEach(mentee ->
			{
				mentee.sendPacket(new SystemMessage(SystemMessageId.YOUR_MENTOR_S1_HAS_LOGGED_OUT).addString(player.getName()));
				mentee.sendPacket(new ExMentorList(mentee.getPlayer()));
			});
		}
	}
	
	/**
	 * This method handles logic when a player changes their profession.<br>
	 * It updates skills for both mentors and mentees based on the new profession.<br>
	 * It also checks if a mentee qualifies for graduate rewards.
	 * @param event The {@code OnPlayerProfessionChange} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_PROFESSION_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onProfessionChange(OnPlayerProfessionChange event)
	{
		final Player player = event.getPlayer();
		if (player.isMentor())
		{
			// Give mentor's buffs only if he didn't had them.
			handleMentorSkills(player);
			return;
		}
		
		// Not a mentee
		if (!player.isMentee())
		{
			return;
		}
		
		handleMenteeSkills(player);
	}
	
	/**
	 * This method is called when a player's level increases.<br>
	 * It checks if the player is a mentee and handles rewards or graduation.<br>
	 * If the player exceeds {@code MAX_LEVEL}, they are treated as a graduate.<br>
	 * Otherwise, it notifies the mentor of the progress.
	 * @param event The {@code OnPlayerLevelChanged} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LEVEL_CHANGED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onLevelIncreased(OnPlayerLevelChanged event)
	{
		final Player player = event.getPlayer();
		
		// Not a mentee
		if (!player.isMentee())
		{
			return;
		}
		
		checkLevelForReward(player); // Checking level to send a mail if is necessary
		if (player.getLevel() >= MAX_LEVEL)
		{
			handleGraduateMentee(player);
		}
		else
		{
			final Mentee mentor = MentorManager.getInstance().getMentor(player.getObjectId());
			if ((mentor != null) && mentor.isOnline())
			{
				mentor.sendPacket(new ExMentorList(mentor.getPlayer()));
			}
		}
	}
	
	/**
	 * Handles logic when a mentee leaves their mentor.<br>
	 * This method removes the mentee skill from the player.<br>
	 * It also checks if the mentor has any remaining mentees to remove mentor skills.<br>
	 * Finally, it updates the UI for both players.
	 * @param event The {@code OnPlayerMenteeLeft} event containing the involved players.
	 */
	@RegisterEvent(EventType.ON_PLAYER_MENTEE_LEFT)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onMenteeLeft(OnPlayerMenteeLeft event)
	{
		final Player player = event.getMentee();
		final Player mentor = event.getMentor().getPlayer();
		
		// Remove the mentee skills
		player.removeSkill(MENTEE_MENTOR_SUMMON.getSkill(), true);
		
		// If player does not have any mentees anymore remove mentor skills.
		if ((mentor != null) && (MentorManager.getInstance().getMentees(mentor.getObjectId()) == null))
		{
			mentor.removeSkill(MENTOR_ART_OF_SEDUCTION.getSkill(), true);
			
			// Clear the mentee
			mentor.sendPacket(new ExMentorList(mentor));
		}
		
		// Clear mentee status
		player.sendPacket(new ExMentorList(player));
	}
	
	/**
	 * Handles the logic when a mentee is removed from a mentor.<br>
	 * It removes specific skills from the player and updates their status.<br>
	 * If the mentor has no mentees left, it also removes the mentor's harmony skills.<br>
	 * Finally, it refreshes the mentor's list of mentees.
	 * @param event The {@code OnPlayerMenteeRemove} event containing the mentor and mentee data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_MENTEE_REMOVE)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onMenteeRemove(OnPlayerMenteeRemove event)
	{
		final Mentee mentee = event.getMentee();
		final Player mentor = event.getMentor();
		final Player player = mentee.getPlayer();
		if (player != null)
		{
			// Remove the mentee skills
			player.removeSkill(MENTEE_MENTOR_SUMMON.getSkill(), true);
			
			// Clear mentee status
			player.sendPacket(new ExMentorList(player));
		}
		
		// If player does not have any mentees anymore remove mentor skills.
		if (MentorManager.getInstance().getMentees(mentor.getObjectId()) == null)
		{
			mentor.removeSkill(MENTOR_ART_OF_SEDUCTION.getSkill(), true);
		}
		
		// Remove mentee from the list
		mentor.sendPacket(new ExMentorList(mentor));
	}
	
	/**
	 * Checks if the {@code Player} has a specific mentee skill.<br>
	 * Adds the skill to the player if it is currently missing.
	 * @param player The {@code Player} object to check and update.
	 */
	private void handleMenteeSkills(Player player)
	{
		// Give mentee's buffs only if he didn't had them.
		if (player.getKnownSkill(MENTEE_MENTOR_SUMMON.getSkillId()) == null)
		{
			// Add the mentee skills
			player.addSkill(MENTEE_MENTOR_SUMMON.getSkill(), false);
		}
	}
	
	/**
	 * This method grants specific mentor skills to a {@code Player}.<br>
	 * It checks if the player already knows the skills before adding them.<br>
	 * The skills are added only if they are currently missing from the player.
	 * @param player The {@code Player} who will receive the mentor skills.
	 */
	private void handleMentorSkills(Player player)
	{
		// Give mentor's buffs only if he didn't had them.
		if (player.getKnownSkill(MENTOR_ART_OF_SEDUCTION.getSkillId()) == null)
		{
			// Add the mentor skills
			player.addSkill(MENTOR_ART_OF_SEDUCTION.getSkill(), false);
		}
	}
	
	/**
	 * Handles the logic when a mentee character graduates.<br>
	 * This method removes mentoring buffs and ends the mentor relationship.<br>
	 * It applies penalties to the mentor and sends graduation rewards to the player.<br>
	 * The process also updates the status for both players in the game world.
	 * @param player The {@code Player} who has reached graduation status.
	 */
	private void handleGraduateMentee(Player player)
	{
		MentorManager.getInstance().cancelAllMentoringBuffs(player);
		final int objectId = player.getObjectId();
		final Mentee mentor = MentorManager.getInstance().getMentor(objectId);
		if (mentor != null)
		{
			final int mentorObjectId = mentor.getObjectId();
			MentorManager.getInstance().setPenalty(mentorObjectId, PlayerConfig.MENTOR_PENALTY_FOR_MENTEE_COMPLETE);
			MentorManager.getInstance().deleteMentor(mentorObjectId, objectId);
			if (mentor.isOnline())
			{
				mentor.sendPacket(new SystemMessage(SystemMessageId.S1_HAS_ACHIEVED_LV_105_AND_THE_MENTOR_MENTEE_RELATIONSHIP_HAS_ENDED_YOU_WILL_BE_ABLE_TO_BECOME_ANOTHER_CHARACTER_S_MENTOR_AFTER_ONE_DAY).addPcName(player));
				if (MentorManager.getInstance().isAllMenteesOffline(mentorObjectId, objectId))
				{
					MentorManager.getInstance().cancelAllMentoringBuffs(mentor.getPlayer());
				}
				
				mentor.sendPacket(new ExMentorList(mentor.getPlayer()));
			}
			
			// Remove the mentee skills
			player.removeSkill(MENTEE_MENTOR_SUMMON.getSkill(), true);
			
			// Clear mentee status
			player.sendPacket(new ExMentorList(player));
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NO_LONGER_S1_S_MENTEE_AS_YOU_HAVE_ACHIEVED_LV_105_YOU_DO_NOT_NEED_A_MENTOR_ANY_LONGER).addPcName(player));
			sendMail(objectId, MENTEE_GRADUATE_TITLE, MENTEE_GRADUATE_BODY, GRADUTION_BOX, 1);
		}
	}
	
	/**
	 * Checks if the {@code Player} is eligible for a level-based reward.<br>
	 * It verifies that the current level exists in the rewards map.<br>
	 * It also ensures the player has not already received a reward for this level.<br>
	 * If all conditions are met, it sends a mail to the mentor with the reward amount.
	 * @param player The {@code Player} object to check for rewards.
	 */
	private void checkLevelForReward(Player player)
	{
		final int playerLevel = player.getLevel();
		if (!MENTEE_COINS.containsKey(playerLevel))
		{
			return;
		}
		
		final Mentee mentor = MentorManager.getInstance().getMentor(player.getObjectId());
		if (mentor == null)
		{
			return;
		}
		
		final int amount = MENTEE_COINS.get(playerLevel);
		if ((amount > 0) && (player.getVariables().getInt(MENTEE_REWARD_LV_VAR, 0) < playerLevel))
		{
			player.getVariables().set(MENTEE_REWARD_LV_VAR, playerLevel);
			sendMail(mentor.getObjectId(), LEVEL_UP_TITLE, String.format(LEVEL_UP_BODY, player.getName(), playerLevel), MENTEE_MARK, amount);
		}
	}
	
	/**
	 * Sends a reward mail to a player.<br>
	 * This method creates a {@code MailMessage} with specific content and attachments.<br>
	 * It then uses the {@code MailManager} to deliver the message.
	 * @param objectId The unique identifier for the recipient.
	 * @param title The subject line of the mail.
	 * @param body The main text content of the mail.
	 * @param itemId The ID of the item to be included as a reward.
	 * @param amount The quantity of the item to include in the mail.
	 */
	private void sendMail(int objectId, String title, String body, int itemId, long amount)
	{
		final MailMessage msg = new MailMessage(objectId, title, body, MailType.MENTOR_NPC);
		final Mail attachments = msg.createAttachments();
		attachments.addItem(ItemProcessType.REWARD, itemId, amount, null, null);
		MailManager.getInstance().sendMessage(msg);
	}
	
	/**
	 * This is the entry point for the {@code MentorGuide} application.<br>
	 * It initializes a new instance of the {@code MentorGuide} class.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new MentorGuide();
	}
}
