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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.PartyMessageType;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Mail;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.Earthquake;
import org.l2jmobius.gameserver.network.serverpackets.ExCursedWeaponLocation;
import org.l2jmobius.gameserver.network.serverpackets.ExRedSky;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;

/**
 * Represents a cursed weapon held by a {@code Player}.<br>
 * This class manages the unique effects and behaviors associated with cursed items.<br>
 * It handles specific mechanics such as environmental changes or special skills.
 * @author Notorion
 */
public class CursedWeapon
{
	private static final Logger LOGGER = Logger.getLogger(CursedWeapon.class.getName());
	
	// _name is the name of the cursed weapon associated with its ID.
	private final String _name;
	// _itemId is the Item ID of the cursed weapon.
	private final int _itemId;
	// _skillId is the skills ID.
	private final int _skillId;
	
	// The drop rate is defined directly by cursedweapon.java at 100% for the CW model version 2019.
	// private int _dropRate;
	private int _duration;
	private int _durationLost;
	private int _disapearChance;
	private int _stageKills;
	
	// This should be false unless if the cursed weapon is dropped, in that case it would be true.
	private boolean _isDropped = false;
	
	// This sets the cursed weapon status to true only if a player has the cursed weapon, otherwise this should be false.
	private boolean _isActivated = false;
	private boolean _rewardSent = false;
	private ScheduledFuture<?> _removeTask;
	private int _nbKills = 0;
	long _endTime = 0;
	private int _playerId = 0;
	protected Player _player = null;
	private Item _item = null;
	private int _playerReputation = 0;
	private int _playerPkKills = 0;
	protected int transformationId = 0;
	private long _lastKillTime = 0;
	
	/**
	 * Creates a new instance of a {@code CursedWeapon}.<br>
	 * This constructor initializes the weapon with its unique properties.
	 * @param itemId The unique identifier for the item.
	 * @param skillId The ID of the skill associated with this cursed weapon.
	 * @param name The display name of the cursed weapon.
	 */
	public CursedWeapon(int itemId, int skillId, String name)
	{
		_name = name;
		_itemId = itemId;
		_skillId = skillId;
	}
	
	/**
	 * Handles the removal and cleanup of a cursed weapon.<br>
	 * If the player is online, it removes the item from their inventory and restores stats.<br>
	 * If the player is offline, it deletes the item and updates character data in the database.<br>
	 * It also cleans up any active tasks and resets internal state variables.
	 */
	public void endOfLife()
	{
		if (_isActivated)
		{
			// Allows the Defense script to end and the reward to be sent to the player.
			if (System.currentTimeMillis() >= (_endTime - 60000))
			{
				sendEventReward();
			}
			
			if ((_player != null) && _player.isOnline())
			{
				final Calendar now = Calendar.getInstance();
				final int h = now.get(Calendar.HOUR_OF_DAY);
				
				// 1. Clear combat status and restore the player's original points.
				_player.abortAttack();
				_player.setReputation(_playerReputation);
				_player.setPkKills(_playerPkKills);
				_player.setCursedWeaponEquippedId(0);
				
				// 2. Remove all skills (This will automatically remove the Abnormal Visual Effect).
				removeSkill();
				
				// 3. Unequip the weapon from the hand, save the player and destroy the item in the inventory.
				_player.getInventory().unEquipItemInBodySlot(BodyPart.LR_HAND);
				_player.storeMe();
				_player.getInventory().destroyItemByItemId(ItemProcessType.NONE, _itemId, 1, _player, null);
				
				// 4. Send the update packet to the client.
				_player.sendItemList();
				_player.broadcastUserInfo();
				
				// 5. Check the event time and apply death only AFTER the player is 100% normal.
				if (h >= 23)
				{
					if (!_player.isDead())
					{
						_player.doDie(null);
					}
				}
			}
			else
			{
				// Remove from OFFLINE player (Via Database).
				try (Connection con = DatabaseFactory.getConnection())
				{
					// 1. Delete the Item.
					try (PreparedStatement del = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?"))
					{
						del.setInt(1, _playerId);
						del.setInt(2, _itemId);
						if (del.executeUpdate() != 1)
						{
							LOGGER.warning("Error while deleting itemId " + _itemId + " from userId " + _playerId);
						}
					}
					
					// 2. Restore Reputation and PK.
					try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET reputation=?, pkkills=? WHERE charId=?"))
					{
						ps.setInt(1, _playerReputation);
						ps.setInt(2, _playerPkKills);
						ps.setInt(3, _playerId);
						if (ps.executeUpdate() != 1)
						{
							LOGGER.warning("Error while updating karma & pkkills for userId " + _playerId);
						}
					}
					
					// 3. Clear Offline Skills.
					try (PreparedStatement delSkills = con.prepareStatement("DELETE FROM character_skills WHERE charId=? AND skill_id IN (35398, 35400, 35399, 35401)"))
					{
						delSkills.setInt(1, _playerId);
						delSkills.executeUpdate();
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "Could not delete : " + e.getMessage(), e);
				}
			}
		}
		else
		{
			// Either this cursed weapon is in the inventory of someone who has another cursed weapon equipped, or this cursed weapon is on the ground.
			if ((_player != null) && (_player.getInventory().getItemByItemId(_itemId) != null))
			{
				// Destroy.
				_player.getInventory().destroyItemByItemId(ItemProcessType.NONE, _itemId, 1, _player, null);
				_player.sendItemList();
				_player.broadcastUserInfo();
			}
			// Is dropped on the ground.
			else if (_item != null)
			{
				_item.decayMe();
				// LOGGER.info(_name + " item has been removed from World.");
			}
		}
		
		// Delete infos from table if any.
		CursedWeaponsManager.removeFromDb(_itemId);
		
		// Message to indicate the disappearance of the Cursed Sword.
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
		sm.addItemName(_itemId);
		broadcastToWorldExceptConquest(sm);
		updateMapIcon();
		
		// Reset state.
		cancelTask();
		_isActivated = false;
		_isDropped = false;
		_endTime = 0;
		_player = null;
		_playerId = 0;
		_playerReputation = 0;
		_playerPkKills = 0;
		_item = null;
		_nbKills = 0;
		
		// Reset the timer so the next holder starts at 0.
		_lastKillTime = 0;
	}
	
	/**
	 * Cancels the scheduled removal task.<br>
	 * This method checks if {@code _removeTask} is not {@code null}.<br>
	 * If it exists, it calls {@code cancel(true)} and sets the reference to {@code null}.
	 */
	private void cancelTask()
	{
		if (_removeTask != null)
		{
			_removeTask.cancel(true);
			_removeTask = null;
		}
	}
	
	private class RemoveTask implements Runnable
	{
		protected RemoveTask()
		{
		}
		
		@Override
		public void run()
		{
			if (System.currentTimeMillis() >= _endTime)
			{
				endOfLife();
			}
		}
	}
	
	/**
	 * Handles the logic for dropping an item when a target is defeated.<br>
	 * This method is called to process loot for a specific {@code Player}.
	 * @param attackable The entity that was defeated.
	 * @param player The player who received the drop.
	 */
	private void dropIt(Attackable attackable, Player player)
	{
		dropIt(attackable, player, null, true);
	}
	
	// Function to clear cursed sword holder outside event time (23h/18h).
	/**
	 * Removes the cursed weapon effect from a player without dropping the item.<br>
	 * This method cleans up all associated skills, reputation changes, and visual effects.<br>
	 * It handles both online players and offline database records.
	 */
	public void forceClearWithoutDrop()
	{
		cancelTask();
		
		if (_player != null)
		{
			if (_player.getInventory().getItemByItemId(_itemId) != null)
			{
				_player.getInventory().unEquipItemInBodySlot(BodyPart.LR_HAND);
				_player.getInventory().destroyItemByItemId(ItemProcessType.NONE, _itemId, 1, _player, null);
			}
			
			_player.setReputation(_playerReputation);
			_player.setPkKills(_playerPkKills);
			_player.setCursedWeaponEquippedId(0);
			removeSkill();
			_player.getAppearance().setVisibleName(null);
			_player.abortAttack();
			_player.sendItemList();
			_player.broadcastUserInfo();
			_player.sendPacket(new UserInfo(_player));
		}
		else if (_playerId > 0)
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET reputation=?, pkkills=? WHERE charId=?"))
				{
					ps.setInt(1, _playerReputation);
					ps.setInt(2, _playerPkKills);
					ps.setInt(3, _playerId);
					ps.executeUpdate();
				}
				
				try (PreparedStatement delSkills = con.prepareStatement("DELETE FROM character_skills WHERE charId=? AND skill_id IN (35398,35400,35399,35401)"))
				{
					delSkills.setInt(1, _playerId);
					delSkills.executeUpdate();
				}
				
				try (PreparedStatement del = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?"))
				{
					del.setInt(1, _playerId);
					del.setInt(2, _itemId);
					del.executeUpdate();
				}
			}
			catch (Exception e)
			{
				LOGGER.warning("forceClearWithoutDrop: DB cleanup error: " + e.getMessage());
			}
		}
		
		CursedWeaponsManager.removeFromDb(_itemId);
		
		_isActivated = false;
		_isDropped = false;
		_endTime = 0;
		_player = null;
		_playerId = 0;
		_playerReputation = 0;
		_playerPkKills = 0;
		_item = null;
		_nbKills = 0;
		_rewardSent = false;
	}
	
	/**
	 * Handles the logic for dropping a cursed weapon.<br>
	 * It determines whether to drop the item from an {@code Attackable} or from a {@code Player}.<br>
	 * If dropped by a monster, it triggers world effects like RedSky and Earthquake.<br>
	 * If dropped by a player death, it updates reputation and removes skills.
	 * @param attackable The entity that drops the item.
	 * @param player The player who interacts with or receives the drop.
	 * @param killer The creature responsible for the kill.
	 * @param fromMonster A boolean indicating if the source is a monster.
	 */
	private void dropIt(Attackable attackable, Player player, Creature killer, boolean fromMonster)
	{
		// Locks physical drop (Cursed Sword).
		final Calendar now = Calendar.getInstance();
		final int h = now.get(Calendar.HOUR_OF_DAY);
		
		// 1. After 23h (h >= 23)
		// 2. Before 18h (h < 18) - To prevent dropping in the morning/afternoon.
		if ((h >= 23) || (h < 18))
		{
			forceClearWithoutDrop();
			return;
		}
		
		if (fromMonster)
		{
			_isActivated = false;
			_item = attackable.dropItem(player, _itemId, 1);
			_item.setDropTime(0);
			
			// Only execute effects if the item REALLY dropped.
			if (_item != null)
			{
				// Visual Effects.
				broadcastToWorldExceptConquest(new ExRedSky(10));
				broadcastToWorldExceptConquest(new Earthquake(_item.getX(), _item.getY(), _item.getZ(), 14, 3));
			}
		}
		else
		{
			// Player drop.
			if (_player == null)
			{
				return;
			}
			
			// Visual death effect (souls) of the player carrying the cursed sword (Always executed on death/drop).
			final int soulSkillId = (_itemId == 8190) ? 35402 : 35403;
			final NpcTemplate template = NpcData.getInstance().getTemplate(18919);
			if (template != null)
			{
				final Npc soulsDummy = new Npc(template);
				
				// Define location based on who died (Player).
				final int x = fromMonster ? attackable.getX() : _player.getX();
				final int y = fromMonster ? attackable.getY() : _player.getY();
				final int z = fromMonster ? attackable.getZ() : _player.getZ();
				soulsDummy.setInstance(fromMonster ? attackable.getInstanceWorld() : _player.getInstanceWorld());
				soulsDummy.spawnMe(x, y, z);
				ThreadPool.schedule(() ->
				{
					if (soulsDummy.isSpawned())
					{
						soulsDummy.broadcastPacket(new MagicSkillUse(soulsDummy, soulsDummy, soulSkillId, 1, 1000, 0));
					}
				}, 300);
				ThreadPool.schedule(soulsDummy::deleteMe, 5000);
			}
			
			_isActivated = false;
			_item = _player.getInventory().getItemByItemId(_itemId);
			_player.dropItem(ItemProcessType.DEATH, _item, killer, true);
			_player.setReputation(_playerReputation);
			_player.setPkKills(_playerPkKills);
			_player.setCursedWeaponEquippedId(0);
			
			// 1. Remove CursedWeapon Skills.
			removeSkill();
			
			// 2. Removal of buffs.
			if (_itemId == 8190)
			{
				_player.removeSkill(35398); // Soul of Sword - Zariche - Status
				_player.removeSkill(35400); // Demonic Sword Zariche - Level
			}
			else if (_itemId == 8689)
			{
				_player.removeSkill(35399); // Soul of Sword - Akamanah - Status
				_player.removeSkill(35401); // Blood Sword Akamanah - Level
			}
			
			_player.stopAllEffectsExceptThoseThatLastThroughDeath();
			_player.abortAttack();
			_player.sendPacket(new UserInfo(_player));
			_player.broadcastUserInfo();
			if (_item != null)
			{
				broadcastToWorldExceptConquest(new ExRedSky(10));
				broadcastToWorldExceptConquest(new Earthquake(_item.getX(), _item.getY(), _item.getZ(), 14, 3));
			}
		}
		
		_isDropped = true;
		_nbKills = 0;
		updateMapIcon();
		if (_item != null)
		{
			final NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
			final long currentTotal = getCurrentReward();
			final long guaranteed = 5_000_000_000L;
			long changed = currentTotal - guaranteed;
			if (changed < 0)
			{
				changed = 0;
			}
			
			// Message id 1815 $s2 has appeared in $s1; the Treasure Chest contains $s2 adena with a fixed reward of $s3 and an additional reward of $s4, which will be given to the last owner at 23:59.
			final SystemMessage sm = new SystemMessage(SystemMessageId.S2_HAS_APPEARED_IN_S1_THE_TREASURE_CHEST_CONTAINS_S2_ADENA_FIXED_REWARD_S3_ADDITIONAL_REWARD_S4_THE_ADENA_WILL_BE_GIVEN_TO_THE_LAST_OWNER_AT_23_59);
			sm.addZoneName(_item.getX(), _item.getY(), _item.getZ()); // $s1
			sm.addItemName(_itemId); // $s2
			
			// Reward.
			// Bonus ($s3) - Guaranteed ($s4).
			sm.addString(nf.format(changed)); // $s3 (Bonus)
			sm.addString(nf.format(guaranteed)); // $s4 (Guaranteed)
			broadcastToWorldExceptConquest(sm);
		}
		
		if (_removeTask != null)
		{
			_removeTask.cancel(true);
			_removeTask = null;
		}
		
		// Schedule to disappear in 10 minutes (600,000 ms).
		_removeTask = ThreadPool.schedule(this::endOfLife, 600000);
	}
	
	/**
	 * This method handles the logic when a player logs into the server with a cursed weapon.<br>
	 * It applies transformations and skills to the player.<br>
	 * It also sends system messages to announce the login and show remaining usage time.
	 */
	public void cursedOnLogin()
	{
		// Re-applies skills to the player.
		int skillIdToApply = 0;
		if (_itemId == 8190)
		{
			skillIdToApply = 35398;
		}
		else if (_itemId == 8689)
		{
			skillIdToApply = 35399;
		}
		
		if (skillIdToApply > 0)
		{
			int level = getLevel(); // Uses getLevel() method to ensure the correct level.
			final int maxLvl = SkillData.getInstance().getMaxLevel(skillIdToApply);
			if (level > maxLvl)
			{
				level = maxLvl;
			}
			
			final Skill skill = SkillData.getInstance().getSkill(skillIdToApply, level);
			if (skill != null)
			{
				_player.addSkill(skill, false);
				skill.applyEffects(_player, _player);
				_player.sendSkillList();
			}
		}
		
		final NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
		final long currentTotal = getCurrentReward();
		final long guaranteed = 5_000_000_000L;
		long changed = currentTotal - guaranteed;
		if (changed < 0)
		{
			changed = 0;
		}
		
		// Login message id 1817: The owner of $s2 is in $s1; the Treasure Chest contains $s2 adena, with a fixed reward of $s3 and an additional reward of $s4, to be given to the last owner at 23:59.
		final SystemMessage sm = new SystemMessage(SystemMessageId.THE_S2_S_OWNER_IS_IN_S1_THE_TREASURE_CHEST_CONTAINS_S2_ADENA_FIXED_REWARD_S3_ADDITIONAL_REWARD_S4_THE_ADENA_WILL_BE_GIVEN_TO_THE_LAST_OWNER_AT_23_59);
		final int playerX = _player.getX();
		final int playerY = _player.getY();
		final int playerZ = _player.getZ();
		sm.addZoneName(playerX, playerY, playerZ); // $s1
		final int cursedWeaponEquippedId = _player.getCursedWeaponEquippedId();
		sm.addItemName(cursedWeaponEquippedId); // $s2
		
		// Reward Visual.
		sm.addString(nf.format(changed)); // $s3 (Bonus)
		sm.addString(nf.format(guaranteed)); // $s4 (Guaranteed)
		broadcastToWorldExceptConquest(sm);
		_player.getAppearance().setVisibleName(_name);
		broadcastToWorldExceptConquest(new ExRedSky(10));
		broadcastToWorldExceptConquest(new Earthquake(playerX, playerY, playerZ, 14, 3));
		
		// Time message.
		final CursedWeapon cw = CursedWeaponsManager.getInstance().getCursedWeapon(cursedWeaponEquippedId);
		final SystemMessage msg = new SystemMessage(SystemMessageId.S1_S_REMAINING_TIME_S2_MIN_TYPE_CURSEDSWORD_TO_CHECK_OTHER_INFORMATION);
		final int timeLeft = (int) (cw.getTimeLeft() / 60000);
		msg.addItemName(cursedWeaponEquippedId);
		msg.addInt(timeLeft);
		_player.sendPacket(msg);
		
		// Update visual to ensure.
		_player.broadcastUserInfo();
	}
	
	// Rebinds all CursedWeapon skills for the player.
	// Note: The visual transformation is handled automatically by the Status Skill effects (35398/35399).
	/**
	 * Grants specific skills to the player based on their current progress.<br>
	 * The skill level is calculated using {@code _nbKills} and {@code _stageKills}.<br>
	 * It also adds specific transform skills like {@code VOID_BURST} and {@code VOID_FLOW}.<br>
	 * Finally, it sends the updated skill list to the player.
	 */
	public void giveSkill()
	{
		// Zariche: 35398 (Status/Visual) + 35400 (Level).
		// Akamanah: 35399 (Status/Visual) + 35401 (Level).
		int statSkillId = 0;
		int levelSkillId = 0;
		if (_itemId == 8190) // Zariche
		{
			statSkillId = 35398;
			levelSkillId = 35400;
		}
		else if (_itemId == 8689) // Akamanah
		{
			statSkillId = 35399;
			levelSkillId = 35401;
		}
		
		if (statSkillId == 0)
		{
			return;
		}
		
		// Remove any CW skill the player has, avoiding duplication.
		_player.removeSkill(35398);
		_player.removeSkill(35399);
		_player.removeSkill(35400);
		_player.removeSkill(35401);
		for (int id : new int[]
		{
			35398,
			35399,
			35400,
			35401
		})
		{
			_player.stopSkillEffects(SkillFinishType.REMOVED, id);
		}
		
		// Applies the skill status.
		final Skill statSkill = SkillData.getInstance().getSkill(statSkillId, 1);
		if (statSkill != null)
		{
			_player.addSkill(statSkill, true); // true = save on character
			
			// Forces immediate effect application (including visual transformation).
			statSkill.applyEffects(_player, _player);
		}
		
		// Applies the level skill (Based on Kills).
		int currentLevel = getLevel(); // Gets the current kills calculation.
		
		// Ensures it does not exceed the maximum level.
		final int maxLvl = SkillData.getInstance().getMaxLevel(levelSkillId);
		if (currentLevel > maxLvl)
		{
			currentLevel = maxLvl;
		}
		
		final Skill levelSkill = SkillData.getInstance().getSkill(levelSkillId, currentLevel);
		if (levelSkill != null)
		{
			_player.addSkill(levelSkill, true);
			levelSkill.applyEffects(_player, _player);
		}
		
		_player.addTransformSkill(CommonSkill.VOID_BURST.getSkill());
		_player.addTransformSkill(CommonSkill.VOID_FLOW.getSkill());
		_player.sendSkillList();
		_player.broadcastUserInfo();
	}
	
	// List of all Cursed Weapon skills (Zariche and Akamanah).
	// 35398: Zariche Stat.
	// 35400: Zariche Level.
	// 35399: Akamanah Stat.
	// 35401: Akamanah Level.
	/**
	 * Removes specific cursed skills and the current skill from the player.<br>
	 * This method stops all associated effects and updates the player's information.
	 */
	public void removeSkill()
	{
		final int[] allCursedSkills =
		{
			35398,
			35400,
			35399,
			35401
		};
		for (int skillId : allCursedSkills)
		{
			_player.removeSkill(skillId);
			final Skill skill = SkillData.getInstance().getSkill(skillId, 1);
			if (skill != null)
			{
				_player.stopSkillEffects(skill);
			}
		}
		
		if (_skillId > 0)
		{
			_player.removeSkill(_skillId);
			final Skill skill = SkillData.getInstance().getSkill(_skillId, 1);
			if (skill != null)
			{
				_player.stopSkillEffects(skill);
			}
		}
		
		_player.getAppearance().setVisibleName(null);
		_player.sendSkillList();
		_player.broadcastUserInfo();
		_player.sendPacket(new UserInfo(_player));
	}
	
	/**
	 * Re-activates the cursed weapon status.<br>
	 * Sets the {@code _isActivated} flag to {@code true}.<br>
	 * Checks if the duration has expired and calls {@code endOfLife} if necessary.<br>
	 * Schedules a new task to remove the effect based on the remaining time.
	 */
	public void reActivate()
	{
		_isActivated = true;
		if ((_endTime - System.currentTimeMillis()) <= 0)
		{
			endOfLife();
			return;
		}
		
		// Schedule cleanup without dropping with minimum delay, as the player may still be offline during restoration.
		final Calendar now = Calendar.getInstance();
		final int h = now.get(Calendar.HOUR_OF_DAY);
		
		// Same release logic already present in activate().
		if ((h >= 23) || (h < 18))
		{
			// If player is GM or has the panel flag, skip forced removal after 2s.
			if ((_player != null) && (_player.isGM() || _player.getVariables().getBoolean("CW_GM_AUTH", false)))
			{
				// GM or tester is authorized outside event time.
			}
			else
			{
				// Schedule with 2s delay to ensure player object is loaded.
				ThreadPool.schedule(() ->
				{
					if (_player != null)
					{
						forceClearWithoutDrop();
					}
					else
					{
						// Player offline: cleanup only via database.
						CursedWeaponsManager.removeFromDb(_itemId);
						_isActivated = false;
						_isDropped = false;
						_endTime = 0;
						_playerId = 0;
						_playerReputation = 0;
						_playerPkKills = 0;
						_item = null;
						_nbKills = 0;
						_rewardSent = false;
					}
				}, 2000);
				return;
			}
		}
		
		// Event time: original logic.
		_removeTask = ThreadPool.scheduleAtFixedRate(new RemoveTask(), _durationLost * 12000, _durationLost * 12000);
	}
	
	/**
	 * Checks if the item can be dropped by a specific entity.<br>
	 * It prevents certain mobs or chests from dropping the sword.<br>
	 * If allowed and the event is active, it triggers the drop and schedules its removal.
	 * @param attackable The {@code Attackable} entity involved in the action.
	 * @param player The {@code Player} who triggered the event.
	 * @return {@code true} if the item dropped successfully, {@code false} otherwise.
	 */
	public boolean checkDrop(Attackable attackable, Player player)
	{
		// Prevents Priests (24366/24368) or Treasure Chests (24370/24371) from dropping the Sword.
		final int mobId = attackable.getId();
		if ((mobId == 24366) || (mobId == 24367) || (mobId == 24368) || (mobId == 24369) || (mobId == 24370) || (mobId == 24371))
		{
			return false;
		}
		
		if (CursedWeaponsManager.getInstance().isEventActive())
		{
			dropIt(attackable, player);
			if (_removeTask != null)
			{
				_removeTask.cancel(true);
			}
			
			// Schedule to disappear in 10 minutes (600,000 ms).
			_removeTask = ThreadPool.schedule(this::endOfLife, 600000);
			_endTime = System.currentTimeMillis() + (_duration * 60000);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Activates the cursed weapon for a specific player.<br>
	 * The method ensures the player is unmounted before proceeding.<br>
	 * It updates player stats, applies transformations, and equips the item.<br>
	 * A global announcement is sent to notify other players of the activation.
	 * @param player The {@code Player} who will equip the cursed weapon.
	 * @param item The {@code Item} being activated by the player.
	 */
	public void activate(Player player, Item item)
	{
		// Authorization allows a GM to apply the Cursed Sword on themselves or a player outside of event hours or between 18:00 and 23:59, provided it does not have the "CW_GM_AUTH" variable placed by admin_cw_add.
		// Executes the time lock.
		if (!player.isGM() && !player.getVariables().getBoolean("CW_GM_AUTH", false))
		{
			final Calendar now = Calendar.getInstance();
			final int h = now.get(Calendar.HOUR_OF_DAY);
			final int m = now.get(Calendar.MINUTE);
			final int s = now.get(Calendar.SECOND);
			if ((h >= 23) || (h < 18) || ((h == 22) && ((m == 59) || ((m == 58) && (s >= 50)))))
			{
				// Destroys the item.
				if (item != null)
				{
					try
					{
						player.getInventory().destroyItemByItemId(ItemProcessType.NONE, item.getId(), 1, player, null);
					}
					catch (Exception e)
					{
					}
				}
				
				_isActivated = false;
				return;
			}
		}
		
		_rewardSent = false;
		_rewardSent = false;
		cancelTask();
		
		// If mounted, dismounts.
		if (player.isMounted() && !player.dismount())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1).addItemName(item));
			player.dropItem(ItemProcessType.DROP, item, null, true);
			return;
		}
		
		_isActivated = true;
		
		// If the sword was forced by GM, it skips drop and starts with time 0.
		if (_endTime == 0)
		{
			_endTime = System.currentTimeMillis() + (_duration * 60000);
		}
		
		// Start inactivity timer at the exact moment the cursed sword is equipped.
		_lastKillTime = System.currentTimeMillis();
		
		_player = player;
		_playerId = _player.getObjectId();
		_playerReputation = _player.getReputation();
		_playerPkKills = _player.getPkKills();
		saveData();
		_player.setCursedWeaponEquippedId(_itemId);
		_player.setReputation(-9999999);
		_player.setPkKills(0);
		if (_player.isInParty())
		{
			_player.getParty().removePartyMember(_player, PartyMessageType.EXPELLED);
		}
		
		_item = item;
		
		// Auto-Equip.
		// Clears hands to ensure no occupied slot error occurs.
		_player.getInventory().unEquipItemInBodySlot(BodyPart.LR_HAND);
		_player.getInventory().unEquipItemInBodySlot(BodyPart.L_HAND);
		_player.getInventory().unEquipItemInBodySlot(BodyPart.R_HAND);
		_player.getInventory().equipItem(_item);
		int skillIdToApply = 0;
		if (_itemId == 8190)
		{
			skillIdToApply = 35398;
		}
		else if (_itemId == 8689)
		{
			skillIdToApply = 35399;
		}
		
		int level = 1 + (_nbKills / _stageKills);
		final int maxLvl = SkillData.getInstance().getMaxLevel(skillIdToApply);
		if (level > maxLvl)
		{
			level = maxLvl;
		}
		
		// Apply Effects.
		if (skillIdToApply > 0)
		{
			final Skill skill = SkillData.getInstance().getSkill(skillIdToApply, level);
			if (skill != null)
			{
				_player.addSkill(skill, false);
				skill.applyEffects(_player, _player);
				_player.sendSkillList();
			}
		}
		
		_player.getAppearance().setVisibleName(_name);
		
		// Since _nbKills starts at 0, getLevel() will return 1.
		// The player starts with Level 1 Buff applied.
		giveSkill();
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_EQUIPPED);
		sm.addItemName(_item);
		_player.sendPacket(sm);
		final NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
		final long currentTotal = getCurrentReward();
		final long guaranteed = 5_000_000_000L;
		long changed = currentTotal - guaranteed;
		if (changed < 0)
		{
			changed = 0;
		}
		
		// Sends Message 1816 ($s1=Zone, $s2=Item, $s3=Guaranteed, $s4=Changed)
		// The $s2's owner has appeared in $s1. The Treasure Chest contains $s2 adena. Fixed reward: $s3, additional reward: $s4. The adena will be given to the last owner at 23:59.
		final SystemMessage msg = new SystemMessage(SystemMessageId.THE_S2_S_OWNER_HAS_APPEARED_IN_S1_THE_TREASURE_CHEST_CONTAINS_S2_ADENA_FIXED_REWARD_S3_ADDITIONAL_REWARD_S4_THE_ADENA_WILL_BE_GIVEN_TO_THE_LAST_OWNER_AT_23_59);
		msg.addZoneName(_player.getX(), _player.getY(), _player.getZ());
		msg.addItemName(_item);
		msg.addString(nf.format(changed)); // $s3 (Bonus)
		msg.addString(nf.format(guaranteed)); // $s4 (Guaranteed)
		CursedWeaponsManager.getInstance().broadcastToWorldExceptConquest(msg);
		_player.sendItemList();
		_player.broadcastUserInfo();
		
		// Possession visual upon acquiring the Cursed Sword (Temporary Abnormal).
		final AbnormalVisualEffect visualEffect = AbnormalVisualEffect.INIT_TRANSFORM;
		_player.getEffectList().startAbnormalVisualEffect(visualEffect);
		
		// 3 second effect (3000ms).
		ThreadPool.schedule(() ->
		{
			if ((_player != null) && _player.isOnline())
			{
				_player.getEffectList().stopAbnormalVisualEffect(visualEffect);
			}
		}, 3000);
		
		// Purge ghost icons from the client's map cache.
		purgeGhostMapIcons();
		
		// Recovery HP CP MP.
		ThreadPool.schedule(() ->
		{
			if ((_player != null) && _player.isOnline() && !_player.isDead())
			{
				_player.setCurrentHpMp(_player.getMaxHp(), _player.getMaxMp());
				_player.setCurrentCp(_player.getMaxCp());
				_player.broadcastStatusUpdate();
			}
		}, 500);
	}
	
	// Purge ghost icons from the client's map cache.
	// Sends the swords to opposite poles to break Icon and then passes an empty list to finalize cleanup.
	/**
	 * Removes ghost icons from the world map.<br>
	 * This method clears old location data by broadcasting empty lists.<br>
	 * It then updates the current icon to its correct position.
	 */
	public void purgeGhostMapIcons()
	{
		// 1. Send icons to opposite poles to tear Icon.
		final List<ExCursedWeaponLocation.CursedWeaponInfo> splitList = new ArrayList<>();
		final Location locZ = new Location(100000, 100000, 0);
		final Location locA = new Location(-100000, -100000, 0);
		splitList.add(new ExCursedWeaponLocation.CursedWeaponInfo(locZ, 8190, 1, 0L));
		splitList.add(new ExCursedWeaponLocation.CursedWeaponInfo(locA, 8689, 1, 0L));
		broadcastToWorldExceptConquest(new ExCursedWeaponLocation(splitList));
		
		// 2. Pass an empty list to clear the dirty map history.
		broadcastToWorldExceptConquest(new ExCursedWeaponLocation(Collections.emptyList()));
		
		// 3. Immediately update the radar with the real coordinates.
		updateMapIcon();
	}
	
	/**
	 * Saves the current weapon data to the database.<br>
	 * This method removes old records for the item and inserts new ones if active.<br>
	 * It handles {@code SQLException} by logging a severe error.
	 */
	public void saveData()
	{
		// LOGGER.info("[CW-DEBUG] Saving Cursed Weapon data " + _name + " (ID: " + _itemId + ")...");
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement del = con.prepareStatement("DELETE FROM cursed_weapons WHERE itemId = ?");
			PreparedStatement ps = con.prepareStatement("INSERT INTO cursed_weapons (itemId, charId, playerReputation, playerPkKills, nbKills, endTime) VALUES (?, ?, ?, ?, ?, ?)"))
		{
			// Delete previous data.
			del.setInt(1, _itemId);
			del.executeUpdate();
			if (_isActivated)
			{
				ps.setInt(1, _itemId);
				ps.setInt(2, _playerId);
				ps.setInt(3, _playerReputation);
				ps.setInt(4, _playerPkKills);
				ps.setInt(5, _nbKills);
				ps.setLong(6, _endTime);
				ps.executeUpdate();
				// LOGGER.info("[CW-DEBUG] SAVE SUCCESS! Weapon saved for PlayerOID: " + _playerId + " with endTime: " + _endTime);
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "CursedWeapon: Failed to save data.", e);
		}
	}
	
	/**
	 * Handles the logic for dropping the item after a kill.<br>
	 * It checks if the item should disappear based on {@code _disapearChance}.<br>
	 * If it does not disappear, it drops the item and resets the player stats.
	 * @param killer The {@code Creature} that killed the target.
	 */
	public void dropIt(Creature killer)
	{
		if (Rnd.get(100) <= _disapearChance)
		{
			endOfLife();
		}
		else
		{
			// At the end of the event, clear player reference.
			dropIt(null, null, killer, false);
			
			if (_player != null)
			{
				_player.setReputation(_playerReputation);
				_player.setPkKills(_playerPkKills);
				_player.setCursedWeaponEquippedId(0);
				removeSkill();
				_player.abortAttack();
				_player.broadcastUserInfo();
			}
		}
	}
	
	// Inactivity timer (2 hours without a kill).
	/**
	 * Retrieves the timestamp of the last kill.<br>
	 * This value is stored as a {@code long}.
	 * @return The time of the last kill.
	 */
	public long getLastKillTime()
	{
		return _lastKillTime;
	}
	
	/**
	 * Updates the timestamp of the last kill.<br>
	 * This method sets the internal {@code _lastKillTime} field to the provided value.
	 * @param time The new timestamp for the last kill.
	 */
	public void setLastKillTime(long time)
	{
		_lastKillTime = time;
	}
	
	// Compatibility method (kept in case some old script calls without target).
	/**
	 * Increments the total number of kills for this cursed weapon.<br>
	 * Updates the player's PK kills and refreshes their user information if they are online.<br>
	 * Checks if a new skill should be granted based on the current kill count.<br>
	 * Reduces the remaining time-to-live of the weapon and saves the data.
	 */
	public void increaseKills()
	{
		increaseKills(null);
	}
	
	/**
	 * Increases the internal kill points for the weapon based on the victim.<br>
	 * This method checks the level and status of the {@code Player} to determine point values.<br>
	 * It prevents point gains if both players belong to the same clan.<br>
	 * If points are earned, it updates the last kill time and saves the data.
	 * @param victim The {@code Player} that was killed by the weapon owner.
	 */
	public void increaseKills(Player victim)
	{
		// Starts at 0. Thus, if killing level 1, it remains 0.
		int points = 0;
		if (victim != null)
		{
			// If both have clan and it's the same ID, cancel point gain.
			final int clanId = _player.getClanId();
			final int victimClanId = victim.getClanId();
			if ((clanId > 0) && (victimClanId > 0) && (clanId == victimClanId))
			{
				_player.sendMessage("Cursed Weapon: Killing clan members does not grant power.");
				return;
			}
			
			final int lvl = victim.getLevel();
			
			// Points Table - Prelude of War 2019.
			if (lvl >= 111)
			{
				points = 700;
			}
			else if (lvl >= 106)
			{
				points = 500;
			}
			else if (lvl >= 100)
			{
				points = 100;
			}
			else if (lvl >= 85)
			{
				points = 10;
			}
			else if (lvl >= 21)
			{
				points = 1;
				// If lower than 21, points remain 0.
			}
			
			if (victim.isCursedWeaponEquipped())
			{
				points = 1000;
			}
		}
		
		// Only executes if gained any points.
		if (points > 0)
		{
			// Timer to remove Sword Cursed - Player must make a kill during the event to extend the time.
			_lastKillTime = System.currentTimeMillis();
			
			// Sums internal CW points.
			_nbKills += points;
			if ((_player != null) && _player.isOnline())
			{
				// Sums +1 to the player's Visual PK.
				_player.setPkKills(_player.getPkKills() + 1);
				
				// Checks if leveled up.
				giveSkill();
				// updateMapIcon();
			}
			
			saveData();
		}
	}
	
	/**
	 * Sets the probability that the item will disappear.<br>
	 * This value is used to determine if the item vanishes after use.
	 * @param disapearChance The chance percentage for the item to disappear.
	 */
	public void setDisapearChance(int disapearChance)
	{
		_disapearChance = disapearChance;
	}
	
	// No longer by chance rate, during the event it is 100%.
	/**
	 * Sets the probability of an item dropping.<br>
	 * This value determines how often a drop occurs.
	 * @param dropRate The new drop rate value.
	 */
	public void setDropRate(int dropRate)
	{
		// _dropRate = dropRate;
	}
	
	/**
	 * Sets the active duration for this object.<br>
	 * The value is stored in the internal {@code _duration} field.
	 * @param duration The new duration value to set.
	 */
	public void setDuration(int duration)
	{
		_duration = duration;
	}
	
	/**
	 * Sets the amount of time lost for this object.<br>
	 * This value is stored in the {@code _durationLost} field.
	 * @param durationLost The amount of time to be lost.
	 */
	public void setDurationLost(int durationLost)
	{
		_durationLost = durationLost;
	}
	
	/**
	 * Sets the number of kills required for the current stage.<br>
	 * This updates the {@code _stageKills} field.
	 * @param stageKills The new number of kills to set.
	 */
	public void setStageKills(int stageKills)
	{
		_stageKills = stageKills;
	}
	
	/**
	 * Sets the total number of kills for this object.<br>
	 * This updates the internal {@code _nbKills} field.
	 * @param nbKills The new number of kills to set.
	 */
	public void setNbKills(int nbKills)
	{
		_nbKills = nbKills;
	}
	
	/**
	 * Sets the unique identifier for the player.<br>
	 * This value is stored in the internal {@code _playerId} field.
	 * @param playerId The unique ID of the player to set.
	 */
	public void setPlayerId(int playerId)
	{
		_playerId = playerId;
	}
	
	/**
	 * Updates the reputation value for a specific player.<br>
	 * This method sets the internal {@code _playerReputation} field.
	 * @param playerReputation The new reputation value to assign to the player.
	 */
	public void setPlayerReputation(int playerReputation)
	{
		_playerReputation = playerReputation;
	}
	
	/**
	 * Updates the number of Player Kill (PK) kills for the player.<br>
	 * This method sets the {@code _playerPkKills} field to a new value.
	 * @param playerPkKills The new number of PK kills to assign.
	 */
	public void setPlayerPkKills(int playerPkKills)
	{
		_playerPkKills = playerPkKills;
	}
	
	/**
	 * Updates the activation status of this object.<br>
	 * Sets the internal state to {@code true} or {@code false}.
	 * @param isActivated The new activation status.
	 */
	public void setActivated(boolean isActivated)
	{
		_isActivated = isActivated;
	}
	
	/**
	 * Updates the dropped status of the object.<br>
	 * Sets the internal {@code _isDropped} flag to the provided value.
	 * @param isDropped The new drop status to set.
	 */
	public void setDropped(boolean isDropped)
	{
		_isDropped = isDropped;
	}
	
	/**
	 * Sets the expiration time for this object.<br>
	 * This value is stored in the {@code _endTime} field.
	 * @param endTime The timestamp representing when the effect ends.
	 */
	public void setEndTime(long endTime)
	{
		_endTime = endTime;
	}
	
	/**
	 * Sets the {@code Player} associated with this object.<br>
	 * This method updates the internal {@code _player} field.
	 * @param player The {@code Player} to set.
	 */
	public void setPlayer(Player player)
	{
		_player = player;
	}
	
	/**
	 * Sets the {@code Item} associated with this object.<br>
	 * This method updates the internal {@code _item} field.
	 * @param item The {@code Item} to set.
	 */
	public void setItem(Item item)
	{
		_item = item;
	}
	
	/**
	 * Checks if the cursed weapon is currently active.
	 * @return {@code true} if activated, {@code false} otherwise.
	 */
	public boolean isActivated()
	{
		return _isActivated;
	}
	
	/**
	 * Checks if the item has been dropped.
	 * @return {@code true} if the item is currently dropped, {@code false} otherwise.
	 */
	public boolean isDropped()
	{
		return _isDropped;
	}
	
	/**
	 * Retrieves the end time of the current effect.
	 * @return The end time as a {@code long}.
	 */
	public long getEndTime()
	{
		return _endTime;
	}
	
	/**
	 * Retrieves the name of the cursed weapon.
	 * @return The {@code String} name of the weapon.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the unique identifier for this item.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the unique identifier for the skill.
	 * @return The {@code int} value of the skill ID.
	 */
	public int getSkillId()
	{
		return _skillId;
	}
	
	/**
	 * Retrieves the unique identifier of the player.
	 * @return The {@code int} ID of the player.
	 */
	public int getPlayerId()
	{
		return _playerId;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this object.
	 * @return the current {@code Player} instance.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the current reputation score of the player.<br>
	 * This value is used to determine how other entities perceive the player.
	 * @return The current {@code int} reputation value.
	 */
	public int getPlayerReputation()
	{
		return _playerReputation;
	}
	
	/**
	 * Retrieves the total number of Player Kill (PK) kills for the player.
	 * @return The current count of {@code playerPkKills}.
	 */
	public int getPlayerPkKills()
	{
		return _playerPkKills;
	}
	
	/**
	 * Retrieves the current number of kills.
	 * @return the total count of kills as an {@code int}.
	 */
	public int getNbKills()
	{
		return _nbKills;
	}
	
	/**
	 * Retrieves the current number of stage kills.
	 * @return The total count of stage kills as an {@code int}.
	 */
	public int getStageKills()
	{
		return _stageKills;
	}
	
	/**
	 * Checks if the cursed weapon is currently active.<br>
	 * It returns {@code true} if the weapon is either activated or dropped.
	 * @return {@code true} if the weapon is active, {@code false} otherwise.
	 */
	public boolean isActive()
	{
		return _isActivated || _isDropped;
	}
	
	// Adena Table:
	// Level 1: 5,000,000,000 (5 bi base)
	// Level 2: 5,200,000,000 (5 bi + 200 mi)
	// Level 3: 5,500,000,000 (5 bi + 500 mi)
	// Level 4: 7,000,000,000 (5 bi + 2 bi)
	// Level 5: 10,000,000,000 (5 bi + 5 bi).
	/**
	 * Retrieves the current level of the skill.<br>
	 * This value is calculated based on the amount of adena rewarded.<br>
	 * It returns a level between {@code 1} and {@code 5} depending on the reward threshold.
	 * @return The current skill level as an {@code int}.
	 */
	public int getLevel()
	{
		// Calculates based on current prize (adena).
		final long currentReward = getCurrentReward();
		
		// Thresholds based on Adena (not kills!).
		if (currentReward >= 10_000_000_000L) // 10 bi
		{
			return 5;
		}
		
		if (currentReward >= 7_000_000_000L) // 7 bi
		{
			return 4;
		}
		
		if (currentReward >= 5_500_000_000L) // 5.5 bi
		{
			return 3;
		}
		
		if (currentReward >= 5_200_000_000L) // 5.2 bi
		{
			return 2;
		}
		
		return 1; // 5 bi base
	}
	
	/**
	 * Calculates the remaining time for the cursed weapon effect.<br>
	 * It subtracts the current system time from the end time.
	 * @return The number of milliseconds left before the effect expires.
	 */
	public long getTimeLeft()
	{
		return _endTime - System.currentTimeMillis();
	}
	
	/**
	 * Teleports a player to a specific location based on the weapon status.<br>
	 * If active, the player moves to the owner.<br>
	 * If dropped, the player moves to the item location.<br>
	 * Otherwise, the player receives an error message.
	 * @param player The {@code Player} to teleport.
	 */
	public void goTo(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		if (_isActivated && (_player != null))
		{
			// Go to player holding the weapon.
			player.teleToLocation(_player.getLocation(), true);
		}
		else if (_isDropped && (_item != null))
		{
			// Go to item on the ground.
			player.teleToLocation(_item.getLocation(), true);
		}
		else
		{
			player.sendMessage(_name + " isn't in the World.");
		}
	}
	
	/**
	 * Retrieves the current location of the associated entity.<br>
	 * It returns the {@code Location} of the player if active.<br>
	 * If dropped, it returns the {@code Location} of the item.<br>
	 * Returns {@code null} if neither condition is met.
	 * @return the {@code Location} object or {@code null}.
	 */
	public Location getWorldPosition()
	{
		if (_isActivated && (_player != null))
		{
			return _player.getLocation();
		}
		
		if (_isDropped && (_item != null))
		{
			return _item.getLocation();
		}
		
		return null;
	}
	
	// Sends map update packet to all players.
	// Used when Adena/Reward changes.
	/**
	 * Updates the map icons for cursed weapons in the world.<br>
	 * This method checks if the event is active before processing.<br>
	 * It sends a list of active and dropped weapon locations to all online players.<br>
	 * If no weapons are found, it clears existing icons from the player screens.
	 */
	private void updateMapIcon()
	{
		if (CursedWeaponsManager.getInstance() == null)
		{
			return;
		}
		
		// Verification; Event Mode Inactive.
		if (!CursedWeaponsManager.getInstance().isEventActive())
		{
			// Sends to ALL players (without distinction).
			for (Player player : World.getPlayers())
			{
				if ((player != null) && player.isOnline())
				{
					clearSinglePlayerScreen(player);
				}
			}
			return;
		}
		
		// Mode: Event ACTIVE (18:00 - 23:00)
		// Builds list of active CWs in the world.
		final List<ExCursedWeaponLocation.CursedWeaponInfo> listToSend = new ArrayList<>();
		for (CursedWeapon cw : CursedWeaponsManager.getInstance().getCursedWeapons())
		{
			// Only adds CWs that are:
			// Activated (in someone's hand) OR
			// Dropped (on the ground).
			if (cw.isActive() && ((cw.getPlayerId() > 0) || cw.isDropped()))
			{
				final Location loc = cw.getWorldPosition();
				if (loc != null)
				{
					listToSend.add(new ExCursedWeaponLocation.CursedWeaponInfo(loc, // Location
						cw.getItemId(), cw.isActivated() ? 1 : 0, // 1 = equipped, 0 = on ground
						cw.getStageReward()));
				}
			}
		}
		
		// With the CW list for normal world.
		final ExCursedWeaponLocation realPacket = new ExCursedWeaponLocation(listToSend);
		
		// Verification for Conquest during the event.
		for (Player player : World.getPlayers())
		{
			if ((player == null) || !player.isOnline())
			{
				continue;
			}
			
			final boolean isInConquest = player.isInsideZone(ZoneId.CONQUEST);
			if (isInConquest)
			{
				clearSinglePlayerScreen(player);
			}
			else
			{
				if (!listToSend.isEmpty())
				{
					player.sendPacket(realPacket);
				}
				else
				{
					clearSinglePlayerScreen(player);
				}
			}
		}
	}
	
	/**
	 * Retrieves the reward amount based on the current stage level.<br>
	 * The value depends on the integer returned by {@code getLevel()}.
	 * @return the reward amount as a {@code long}.
	 */
	public long getStageReward()
	{
		final int level = getLevel();
		switch (level)
		{
			case 5:
			{
				return 10_000_000_000L;
			}
			case 4:
			{
				return 7_000_000_000L;
			}
			case 3:
			{
				return 5_500_000_000L;
			}
			case 2:
			{
				return 5_200_000_000L;
			}
			default:
			{
				return 5_000_000_000L;
			}
		}
	}
	
	/**
	 * Calculates the total reward amount for the player.<br>
	 * This includes a fixed base reward and a bonus based on kills.<br>
	 * The bonus is capped at a maximum value of {@code 5000000000}.
	 * @return The total calculated reward as a {@code long}.
	 */
	public long getCurrentReward()
	{
		final long baseReward = 5_000_000_000L; // 5 Billion Base
		
		// 180,000 points (max) * 27,777 = ~5 Billion bonus.
		final long adenaPerPoint = 27_777L;
		long currentBonus = _nbKills * adenaPerPoint;
		final long maxBonus = 5_000_000_000L;
		if (currentBonus > maxBonus)
		{
			currentBonus = maxBonus;
		}
		
		return baseReward + currentBonus;
	}
	
	/**
	 * Retrieves the current duration of the cursed weapon.
	 * @return The duration as a {@code long}.
	 */
	public long getDuration()
	{
		return _duration;
	}
	
	// Final reward function.
	/**
	 * Sends a reward to the player who holds the cursed weapon.<br>
	 * This method calculates the total amount based on the item level.<br>
	 * It sends a mail with the currency and a special box to the owner.<br>
	 * A world broadcast is also sent to announce the reward.
	 */
	public void sendEventReward()
	{
		if (_rewardSent)
		{
			return; // If already sent, aborts to avoid duplication.
		}
		
		if (_playerId == 0)
		{
			return;
		}
		
		_rewardSent = true; // Marks as reward sent
		long finalReward = 5_000_000_000L; // Base 5bi
		final int level = getLevel();
		
		// Adds the exact bonus according to the final level.
		switch (level)
		{
			case 2:
			{
				finalReward += 200_000_000L;
				break;
			}
			case 3:
			{
				finalReward += 500_000_000L;
				break;
			}
			case 4:
			{
				finalReward += 2_000_000_000L;
				break;
			}
			case 5:
			{
				finalReward += 5_000_000_000L;
				break;
			}
			default:
			{
				break; // Level 1 = Only the base
			}
		}
		
		final int rewardBoxId = (_itemId == 8190) ? 80903 : 80904;
		
		// Unofficial text.
		final String mailSubject = "Cursed Weapon Event Reward";
		final String mailBody = "Congratulations! You are the final possessor of " + _name + ". Here is your reward.";
		
		// Optional message 2.
		// String mailBody = "Congratulations! You are the final possessor of " + _name +
		// ". Level: " + level + " | Kills: " + _nbKills;
		final MailMessage msg = new MailMessage(-1, _playerId, false, mailSubject, mailBody, 0);
		final Mail attachments = msg.createAttachments();
		
		// Delivery: 'finalReward' (Fixed Table), not the map visual value.
		attachments.addItem(ItemProcessType.REWARD, 57, finalReward, null, null);
		attachments.addItem(ItemProcessType.REWARD, rewardBoxId, 1, null, null);
		MailManager.getInstance().sendMessage(msg);
		final NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1);
		sm.addString(nf.format(finalReward) + " Adena has been given to the owner of " + _name);
		broadcastToWorldExceptConquest(sm);
	}
	
	/**
	 * Sends a packet to all online players in the world.<br>
	 * Players currently inside the {@code ZoneId.CONQUEST} zone will not receive it.<br>
	 * If a player is found in the conquest zone, their screen is cleared instead.
	 * @param packet The {@code ServerPacket} to be sent to the players.
	 */
	public void broadcastToWorldExceptConquest(ServerPacket packet)
	{
		for (Player player : World.getPlayers())
		{
			if ((player != null) && player.isOnline())
			{
				if (player.isInsideZone(ZoneId.CONQUEST))
				{
					clearSinglePlayerScreen(player);
				}
				else
				{
					player.sendPacket(packet);
				}
			}
		}
	}
	
	/**
	 * Clears the single player screen for a specific user.<br>
	 * This method removes any active cursed weapon effects from the {@code Player}.
	 * @param player The {@code Player} whose screen needs to be cleared.
	 */
	public void clearSinglePlayerScreen(Player player)
	{
		CursedWeaponsManager.getInstance().clearSinglePlayerScreen(player);
	}
}
