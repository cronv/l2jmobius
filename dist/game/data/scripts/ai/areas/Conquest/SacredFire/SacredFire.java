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
package ai.areas.Conquest.SacredFire;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.ConquestConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Guard;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.Id;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.OnServerStart;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSummonSacredFire;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemUse;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.dethroneability.ExHolyFireNotify;

/**
 * Handles the artificial intelligence for the Sacred Fire in the Conquest system.<br>
 * This class manages the behavior and logic of the {@code SacredFire} entity.
 * @author CostyKiller, -K
 */
public class SacredFire extends Script
{
	private static final boolean DEBUG = false; // Set it true to display related messages into server console.
	
	// NPCs
	private static final int SACRED_FIRE = 34658;
	private static final int FIRE_POWER_ZEALOT = 34663;
	private static final int FLAME_POWER_GUARD = 34664;
	
	// Items
	private static final int SACRED_FIRE_SUMMON_SCROLL = 82614;
	private static final int FLAME_GUARDIAN_SCROLL = 82619;
	private static final int FLAME_ESSENCE = 82618;
	
	// NPC vars
	private static final long LIFETIME = 1500000; // 25 Minutes is equivalent to 1500000 Milliseconds
	private static final long SACRED_FIRE_COUNT_UPDATE_TIME = 60000; // 60 Seconds is equivalent to 60000 Milliseconds
	private static final int SPAWNED = 1; // Spawned effect
	private static final int DESTROYED = 2; // Destroyed effect
	
	// SQL Queries
	private static final String DELETE_QUERY = "DELETE FROM character_variables WHERE var LIKE ?";
	
	// Steal attempt requirements
	private static final int REQUIRED_PERSONAL_POINTS = 900;
	private static final int REQUIRED_SP = 400000;
	private static final int STEAL_CHANCE = 50; // 50%
	
	// Skills
	private static final SkillHolder ESSENCE_OF_FIRE_ENERGY_BUFF = new SkillHolder(34492, 1);
	private static final SkillHolder FLAME_GUARDIAN_ENERGY_BUFF = new SkillHolder(34493, 1);
	
	// Zones
	private static final ZoneType FIRE_SOURCE_COMMON_AREA_ZONE = ZoneManager.getInstance().getZoneByName("common_area_fire_source");
	private static final ZoneType CONQUEST_ZONE = ZoneManager.getInstance().getZoneByName("conquest");
	
	// Sacred Fire states are: 0 for summoning the flame, 1 for summoning in progress, 2 for completion and reward, and 3 for extinguished.
	private static final int SUMMONING_STATE = 1;
	private static final int REWARDING_STATE = 2;
	private static final int EXTINGUISHED_STATE = 3;
	
	// Tasks
	protected ScheduledFuture<?> _scheduledSuccessfulDefenseRewardTask;
	protected ScheduledFuture<?> _scheduledSacredFireCountSetupTask;
	protected Map<Integer, ScheduledFuture<?>> _scheduledSuccessfulDefenseRewardTasksList = new ConcurrentHashMap<>();
	protected Map<Integer, ScheduledFuture<?>> _scheduledSacredFireCountSetupTasksList = new ConcurrentHashMap<>();
	
	/**
	 * Initializes the {@code SacredFire} manager.<br>
	 * Registers all necessary NPCs and dialogue IDs.<br>
	 * Sets up zone exit listeners for conquest areas.
	 */
	private SacredFire()
	{
		addStartNpc(SACRED_FIRE);
		addFirstTalkId(SACRED_FIRE, FIRE_POWER_ZEALOT);
		addTalkId(SACRED_FIRE);
		addExitZoneId(CONQUEST_ZONE.getId());
	}
	
	/**
	 * Handles specific interactions for Sacred Fire objects.<br>
	 * This method processes events like stealing, upgrading, and protecting the fire.<br>
	 * It checks player requirements and updates variables or rewards accordingly.
	 * @param event The name of the event to process.
	 * @param npc The {@code Npc} object involved in the interaction.
	 * @param player The {@code Player} performing the action.
	 * @return The HTML text to display, or {@code null} if no specific UI is required.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final long personalConquestPoints = player.getVariables().getLong(PlayerVariables.CONQUEST_PERSONAL_POINTS, 0);
		switch (event)
		{
			case "34658.html":
			case "34658-01.html":
			case "34658-02.html":
			case "34658-03.html":
			case "34658-31.html":
			{
				htmltext = event;
				break;
			}
			case "attemptSteal":
			{
				if (npc.getVariables().getInt("OWNER_OID", 0) == player.getObjectId())
				{
					player.sendMessage("You can't steal your own Sacred Fire.");
				}
				else if (!((personalConquestPoints > REQUIRED_PERSONAL_POINTS) && (player.getSp() > REQUIRED_SP)))
				{
					player.sendMessage("You can't steal this Sacred Fire, because requirements are not met.");
				}
				else
				{
					// - Each attempt costs 900 Conquest points and 400,000 SP.
					// - The price is paid even if the attempt ends in a failure.
					player.getVariables().set(PlayerVariables.CONQUEST_PERSONAL_POINTS, player.getVariables().getLong(PlayerVariables.CONQUEST_PERSONAL_POINTS) - REQUIRED_PERSONAL_POINTS);
					player.setSp(player.getSp() - REQUIRED_SP);
					
					// Messages.
					final SystemMessage sm = new SystemMessage(SystemMessageId.PERSONAL_CONQUEST_POINTS_S1);
					sm.addLong(REQUIRED_PERSONAL_POINTS);
					final SystemMessage sm2 = new SystemMessage(SystemMessageId.YOUR_SP_HAS_DECREASED_BY_S1);
					sm2.addLong(REQUIRED_SP);
					player.sendPacket(sm);
					player.sendPacket(sm2);
					
					if (getRandom(100) < STEAL_CHANCE)
					{
						final int npcObjectId = npc.getObjectId();
						final ScheduledFuture<?> defenseRewardTask = _scheduledSuccessfulDefenseRewardTasksList.get(npcObjectId);
						if ((defenseRewardTask != null) && !defenseRewardTask.isCancelled())
						{
							defenseRewardTask.cancel(false);
							_scheduledSuccessfulDefenseRewardTasksList.remove(npcObjectId);
						}
						
						final ScheduledFuture<?> countSetupTask = _scheduledSacredFireCountSetupTasksList.get(npcObjectId);
						if ((countSetupTask != null) && !countSetupTask.isCancelled())
						{
							countSetupTask.cancel(false);
							_scheduledSacredFireCountSetupTasksList.remove(npcObjectId);
						}
						
						npc.setDisplayEffect(DESTROYED);
						npc.decayMe();
						player.sendMessage("You have stolen this Sacred Fire.");
						
						// TODO: check if stealing the sacred fire makes it disappear or spawns it for the stealer.
						// spawnSacredFire(player);
						
						// TODO: Check with increased range if more guardians attack.
						World.forEachVisibleObjectInRange(player, Guard.class, 1000, guard ->
						{
							if ((guard.getId() == FLAME_POWER_GUARD) && !guard.isDead() && !guard.isDecayed() && !guard.isInCombat() && npc.getEffectList().isAffectedBySkill(FLAME_GUARDIAN_ENERGY_BUFF.getSkillId()))
							{
								addAttackPlayerDesire(guard, player);
							}
						});
						
						// Get the previous owner objectId from world and update Sacred Fire vars.
						final Player previousOwner = World.getPlayer(npc.getVariables().getInt("OWNER_OID", 0));
						
						// Set previous owner variable of the stolen Sacred Fire state to 3 (flame extinguished).
						previousOwner.getVariables().set("SACRED_FIRE_SLOT_" + npc.getVariables().getInt("SLOT", 0), EXTINGUISHED_STATE);
						
						// Update previous owner current Sacred Fires count.
						previousOwner.getVariables().set(PlayerVariables.CONQUEST_SACRED_FIRE_SLOT_COUNT, previousOwner.getVariables().getInt(PlayerVariables.CONQUEST_SACRED_FIRE_SLOT_COUNT, 0) - 1);
						previousOwner.getVariables().remove("SACRED_FIRE_SLOT_" + npc.getVariables().getInt("SLOT", 0));
						
						if (player.isInventoryUnder90(false))
						{
							if (!ConquestConfig.CONQUEST_SACRED_FIRE_REWARDS.isEmpty())
							{
								// Common items rewards.
								giveItems(player, getRandomEntry(ConquestConfig.CONQUEST_SACRED_FIRE_REWARDS));
							}
						}
						else
						{
							player.sendPacket(SystemMessageId.YOUR_INVENTORY_S_WEIGHT_SLOT_LIMIT_HAS_BEEN_EXCEEDED_SO_YOU_CANNOT_RECEIVE_THE_REWARD_PLEASE_FREE_UP_SOME_SPACE_AND_TRY_AGAIN);
						}
					}
					else
					{
						player.sendMessage("You have failed to steal this Sacred Fire.");
					}
				}
				break;
			}
			case "increaseHp":
			{
				if (npc.getVariables().getInt("OWNER_OID", 0) != player.getObjectId())
				{
					player.sendMessage("You can't upgrade this Sacred Fire.");
				}
				else if (npc.getVariables().hasVariable("INCREASED_HP") && npc.getVariables().getBoolean("INCREASED_HP"))
				{
					htmltext = "34658-21.html"; // Already used flame essence.
				}
				else if (takeItems(player, FLAME_ESSENCE, 1))
				{
					npc.setDisplayEffect(SPAWNED);
					npc.getVariables().set("INCREASED_HP", true);
					ESSENCE_OF_FIRE_ENERGY_BUFF.getSkill().applyEffects(npc, npc);
					htmltext = "34658-20.html"; // Sacred Fire buff.
				}
				else
				{
					player.sendMessage("You don't have the required items.");
				}
				break;
			}
			case "preventSteal":
			{
				if (npc.getVariables().getInt("OWNER_OID", 0) != player.getObjectId())
				{
					player.sendMessage("You can't protect this Sacred Fire.");
				}
				else if (npc.getVariables().hasVariable("PROTECTED") && npc.getVariables().getBoolean("PROTECTED", false))
				{
					htmltext = "34658-31.html"; // Already used protect fire.
				}
				else if (takeItems(player, FLAME_GUARDIAN_SCROLL, 1))
				{
					npc.setDisplayEffect(SPAWNED);
					npc.getVariables().set("PROTECTED", true);
					FLAME_GUARDIAN_ENERGY_BUFF.getSkill().applyEffects(npc, npc);
					htmltext = "34658-30.html";
				}
				else
				{
					player.sendMessage("You don't have the required items.");
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Determines the dialogue file to display when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * It returns the HTML filename associated with the unique ID of the {@code Npc}.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the path to the dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This method handles logic when a {@code Creature} leaves a specific {@code ZoneType}.<br>
	 * It checks if the creature is a {@code Player}.<br>
	 * If the player is not in the conquest zone, it decays their sacred fires after 5 seconds.<br>
	 * All related variables and scheduled tasks are cleared for the player.
	 * @param creature The {@code Creature} that exited the zone.
	 * @param zone The type of {@code ZoneType} being exited.
	 */
	@Override
	public void onExitZone(Creature creature, ZoneType zone)
	{
		if (!creature.isPlayer())
		{
			return;
		}
		
		final Player player = creature.asPlayer();
		ThreadPool.schedule(() ->
		{
			// Player teleported inside Conquest.
			if (player.isInsideZone(ZoneId.CONQUEST))
			{
				return;
			}
			
			// Decay spawned sacred fires.
			for (int i = ConquestConfig.CONQUEST_MAX_SACRED_FIRE_SLOTS_COUNT; i >= 1; i--)
			{
				final WorldObject sacredFire = World.findObject(player.getVariables().getInt("SACRED_FIRE_SLOT_" + i + "_OID", 0));
				if (sacredFire != null)
				{
					sacredFire.asNpc().setDisplayEffect(DESTROYED);
					sacredFire.asNpc().decayMe();
					
					for (Entry<Integer, ScheduledFuture<?>> entry : _scheduledSuccessfulDefenseRewardTasksList.entrySet())
					{
						final ScheduledFuture<?> defenseRewardTask = entry.getValue();
						if ((defenseRewardTask != null) && !defenseRewardTask.isCancelled() && !defenseRewardTask.isDone())
						{
							defenseRewardTask.cancel(false);
						}
					}
					
					_scheduledSuccessfulDefenseRewardTasksList.clear();
					
					for (Entry<Integer, ScheduledFuture<?>> entry : _scheduledSacredFireCountSetupTasksList.entrySet())
					{
						final ScheduledFuture<?> countSetupTask = entry.getValue();
						if ((countSetupTask != null) && !countSetupTask.isCancelled() && !countSetupTask.isDone())
						{
							countSetupTask.cancel(false);
						}
					}
					
					_scheduledSacredFireCountSetupTasksList.clear();
					
					if (DEBUG)
					{
						LOGGER.info("Sacred Fire Decay. -> " + player.getObjectId() + " - " + "SACRED_FIRE_SLOT_" + i);
					}
				}
				
				// Remove Sacred Fire progress if the player exits the conquest zone
				player.getVariables().remove("SACRED_FIRE_SLOT_" + i);
				player.getVariables().remove("SACRED_FIRE_SLOT_" + i + "_SUMMON_TIME");
				player.getVariables().remove("SACRED_FIRE_SLOT_" + i + "_OID");
				player.getVariables().remove(PlayerVariables.CONQUEST_SACRED_FIRE_SLOT_COUNT);
				player.getVariables().storeMe();
			}
			
			// Custom message.
			player.sendMessage("Sacred Fire progress was lost, since you exit the conquest zone");
			if (DEBUG)
			{
				LOGGER.info("Sacred Fire Vars have been reset, since you exit the conquest zone");
			}
		}, 5000);
	}
	
	/**
	 * Handles the logic when a player uses an item.<br>
	 * It checks if conquest is available before spawning Sacred Fire.<br>
	 * If requirements are not met, it returns the item to the player.
	 * @param event The {@code OnItemUse} event containing the player and item details.
	 */
	@RegisterEvent(EventType.ON_ITEM_USE)
	@RegisterType(ListenerRegisterType.ITEM)
	@Id(SACRED_FIRE_SUMMON_SCROLL)
	public void onItemUse(OnItemUse event)
	{
		final Player player = event.getPlayer();
		if (!GlobalVariablesManager.getInstance().getBoolean("CONQUEST_AVAILABLE", false))
		{
			player.sendMessage("You can't summon Sacred Fires if you're not in proper zone and conquest is not available.");
			player.getInventory().addItem(ItemProcessType.REFUND, SACRED_FIRE_SUMMON_SCROLL, 1, player, player);
			return;
		}
		
		spawnSacredFire(player);
	}
	
	/**
	 * Spawns a {@code SacredFire} entity for the specified {@code Player}.<br>
	 * This method checks if the {@code Player} is in the correct zone and has the required transformation.<br>
	 * It validates available slots before spawning the fire and scheduling reward tasks.<br>
	 * If requirements are not met, it sends a message and refunds the scroll to the {@code Player}.
	 * @param player The {@code Player} attempting to summon the Sacred Fire.
	 */
	private void spawnSacredFire(Player player)
	{
		// Sacred Fire can only be summoned in the common area of the Fire Source in Conquest's Fire Area (after entering the area your character will turn into a rabbit).
		// Check zone and transformation (rabbit transform id is 105).
		if (FIRE_SOURCE_COMMON_AREA_ZONE.isInsideZone(player) && (player.getTransformationId() == 105))
		{
			if (player.getVariables().getInt(PlayerVariables.CONQUEST_SACRED_FIRE_SLOT_COUNT, 0) < ConquestConfig.CONQUEST_MAX_SACRED_FIRE_SLOTS_COUNT)
			{
				// Get Sacred Fire slot.
				int sacredFireSlot = ConquestConfig.CONQUEST_MAX_SACRED_FIRE_SLOTS_COUNT;
				for (int i = sacredFireSlot; i >= 1; i--)
				{
					if ((player.getVariables().getInt("SACRED_FIRE_SLOT_" + i, 0) == 0) || (player.getVariables().getInt("SACRED_FIRE_SLOT_" + i, 0) == 3))
					{
						sacredFireSlot = i;
					}
				}
				
				if (DEBUG)
				{
					LOGGER.info("Sacred Fire Script -> " + player.getName() + " (" + player.getObjectId() + ") - " + " Sacred Fire Slots: " + sacredFireSlot);
				}
				
				final Npc sacredFire = addSpawn(SACRED_FIRE, player, true, LIFETIME, false, player.getInstanceId());
				sacredFire.setDisplayEffect(SPAWNED);
				sacredFire.getVariables().set("OWNER_OID", player.getObjectId());
				sacredFire.getVariables().set("SLOT", sacredFireSlot);
				sacredFire.getVariables().set("SUMMON_TIME", System.currentTimeMillis());
				
				// Send Flame notify packet
				player.sendPacket(new ExHolyFireNotify(2));
				
				checkSummonSacredFireListener(player);
				
				// Update owner current Sacred Fires count.
				player.getVariables().set(PlayerVariables.CONQUEST_SACRED_FIRE_SLOT_COUNT, player.getVariables().getInt(PlayerVariables.CONQUEST_SACRED_FIRE_SLOT_COUNT, 0) + 1);
				
				// Set Sacred Fire state to 1 (summoning).
				player.getVariables().set("SACRED_FIRE_SLOT_" + sacredFire.getVariables().getInt("SLOT", 0), SUMMONING_STATE);
				player.getVariables().set("SACRED_FIRE_SLOT_" + sacredFire.getVariables().getInt("SLOT", 0) + "_OID", sacredFire.getObjectId());
				player.getVariables().set("SACRED_FIRE_SLOT_" + sacredFire.getVariables().getInt("SLOT", 0) + "_SUMMON_TIME", sacredFire.getVariables().getLong("SUMMON_TIME", 0));
				player.getVariables().storeMe();
				
				// Tasks
				_scheduledSuccessfulDefenseRewardTask = ThreadPool.schedule(() ->
				{
					// Rewards
					if (player.isInventoryUnder90(false))
					{
						// Set Sacred Fire state to 2 (summoning flame is finished get reward).
						// Send Flame notify packet
						player.sendPacket(new ExHolyFireNotify(2));
						player.getVariables().set("SACRED_FIRE_SLOT_" + sacredFire.getVariables().getInt("SLOT", 0), REWARDING_STATE);
						player.getVariables().storeMe();
						final SystemMessage sm = new SystemMessage(SystemMessageId.FIRE_SOURCE_POINTS_S1_PERSONAL_CONQUEST_POINTS_S2_SERVER_CONQUEST_POINTS_S3);
						
						// Points rewards.
						if (ConquestConfig.CONQUEST_SACRED_FIRE_REWARD_FIRE_SOURCE_POINTS != 0)
						{
							player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_EXP, player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_EXP, 0) + ConquestConfig.CONQUEST_SACRED_FIRE_REWARD_FIRE_SOURCE_POINTS);
						}
						
						// Message update.
						sm.addInt(ConquestConfig.CONQUEST_SACRED_FIRE_REWARD_FIRE_SOURCE_POINTS != 0 ? ConquestConfig.CONQUEST_SACRED_FIRE_REWARD_FIRE_SOURCE_POINTS : 0);
						if (ConquestConfig.CONQUEST_SACRED_FIRE_REWARD_PERSONAL_POINTS != 0)
						{
							player.getVariables().set(PlayerVariables.CONQUEST_PERSONAL_POINTS, player.getVariables().getLong(PlayerVariables.CONQUEST_PERSONAL_POINTS, 0) + ConquestConfig.CONQUEST_SACRED_FIRE_REWARD_PERSONAL_POINTS);
						}
						
						// Message update.
						sm.addLong(ConquestConfig.CONQUEST_SACRED_FIRE_REWARD_PERSONAL_POINTS != 0 ? ConquestConfig.CONQUEST_SACRED_FIRE_REWARD_PERSONAL_POINTS : 0);
						if (ConquestConfig.CONQUEST_SACRED_FIRE_REWARD_SERVER_POINTS != 0)
						{
							GlobalVariablesManager.getInstance().set("CONQUEST_SERVER_POINTS", GlobalVariablesManager.getInstance().getLong("CONQUEST_SERVER_POINTS", 0) + ConquestConfig.CONQUEST_SACRED_FIRE_REWARD_SERVER_POINTS);
						}
						
						// Message update.
						sm.addLong(ConquestConfig.CONQUEST_SACRED_FIRE_REWARD_SERVER_POINTS != 0 ? ConquestConfig.CONQUEST_SACRED_FIRE_REWARD_SERVER_POINTS : 0);
						player.sendPacket(sm);
						
						// Common items rewards.
						if (!ConquestConfig.CONQUEST_SACRED_FIRE_REWARDS.isEmpty())
						{
							for (ItemHolder reward : ConquestConfig.CONQUEST_SACRED_FIRE_REWARDS)
							{
								player.addItem(ItemProcessType.REWARD, reward.getId(), reward.getCount(), player, true);
							}
						}
					}
					else
					{
						// Set Sacred Fire state to 3 (flame is extinguished).
						// Send Flame notify packet
						player.sendPacket(new ExHolyFireNotify(2));
						player.getVariables().set("SACRED_FIRE_SLOT_" + sacredFire.getVariables().getInt("SLOT", 0), EXTINGUISHED_STATE);
						player.getVariables().storeMe();
						player.sendPacket(SystemMessageId.YOUR_INVENTORY_S_WEIGHT_SLOT_LIMIT_HAS_BEEN_EXCEEDED_SO_YOU_CANNOT_RECEIVE_THE_REWARD_PLEASE_FREE_UP_SOME_SPACE_AND_TRY_AGAIN);
					}
					
					if (DEBUG)
					{
						LOGGER.info("Sacred Fire Reward Task. -> " + player.getObjectId() + " - " + "SACRED_FIRE_SLOT_" + sacredFire.getVariables().getInt("SLOT", 0));
					}
					
					_scheduledSacredFireCountSetupTask = ThreadPool.schedule(() ->
					{
						// Update owner current Sacred Fires count.
						player.getVariables().set(PlayerVariables.CONQUEST_SACRED_FIRE_SLOT_COUNT, player.getVariables().getInt(PlayerVariables.CONQUEST_SACRED_FIRE_SLOT_COUNT, 0) - 1);
						
						// Remove previous vars.
						player.getVariables().remove("SACRED_FIRE_SLOT_" + sacredFire.getVariables().getInt("SLOT", 0));
						player.getVariables().remove("SACRED_FIRE_SLOT_" + sacredFire.getVariables().getInt("SLOT", 0) + "_SUMMON_TIME");
						player.getVariables().remove("SACRED_FIRE_SLOT_" + sacredFire.getVariables().getInt("SLOT", 0) + "_OID");
						player.getVariables().storeMe();
						
						if (DEBUG)
						{
							LOGGER.info("Sacred Fire Count Setup Task. -> " + player.getObjectId() + " - " + "SACRED_FIRE_SLOT_" + sacredFire.getVariables().getInt("SLOT", 0));
						}
					}, SACRED_FIRE_COUNT_UPDATE_TIME);
					
					_scheduledSacredFireCountSetupTasksList.put(sacredFire.getObjectId(), _scheduledSacredFireCountSetupTask); // Store the current task for bulk usage on clean.
				}, LIFETIME);
				
				_scheduledSuccessfulDefenseRewardTasksList.put(sacredFire.getObjectId(), _scheduledSuccessfulDefenseRewardTask); // Store the current task for bulk usage on clean.
			}
			else
			{
				player.sendMessage("You already have max number of Sacred Fires.");
				player.getInventory().addItem(ItemProcessType.REFUND, SACRED_FIRE_SUMMON_SCROLL, 1, player, player);
			}
		}
		else
		{
			player.sendMessage("You can't summon Sacred Fires here.");
			player.getInventory().addItem(ItemProcessType.REFUND, SACRED_FIRE_SUMMON_SCROLL, 1, player, player);
		}
	}
	
	/**
	 * Clears the sacred fire variables from the database.<br>
	 * This method executes a {@code DELETE} query to reset all related slots.<br>
	 * It logs a warning if the database operation fails.
	 */
	private void removeSacredFireVars()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(DELETE_QUERY))
		{
			st.setString(1, "SACRED_FIRE_SLOT_" + "%");
			st.execute();
			LOGGER.info("Sacred Fire Vars have been reset.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't delete Sacred Fire variables.", e);
		}
	}
	
	/**
	 * This method is called when the server starts.<br>
	 * It clears all existing variables related to {@code SacredFire}.
	 * @param event The {@code OnServerStart} event object.
	 */
	@RegisterEvent(EventType.ON_SERVER_START)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onServerStart(OnServerStart event)
	{
		removeSacredFireVars();
	}
	
	/**
	 * Checks if there is a listener for the sacred fire summon event.<br>
	 * It notifies all registered listeners asynchronously if one exists.
	 * @param player The {@code Player} who triggered the action.
	 */
	private void checkSummonSacredFireListener(Player player)
	{
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SUMMON_SACRED_FIRE))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSummonSacredFire(player, SACRED_FIRE));
		}
	}
	
	/**
	 * This is the entry point for the {@code SacredFire} class.<br>
	 * It initializes a new instance of the {@code SacredFire} object.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new SacredFire();
	}
}
