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
package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.holders.PetDataHolder;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.PetData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.instance.Servitor;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.PetItemList;

/**
 * This class manages the database operations for summon information.<br>
 * It handles the persistence and retrieval of data related to {@code Summon} entities.
 * @author Nyaran, Mobius
 */
public class SummonInfoTable
{
	private static final Logger LOGGER = Logger.getLogger(SummonInfoTable.class.getName());
	
	private static final Map<Integer, Integer> _pets = new ConcurrentHashMap<>();
	private static final Map<Integer, Set<Integer>> _servitors = new ConcurrentHashMap<>();
	
	// SQL
	private static final String INIT_PET = "SELECT ownerId, item_obj_id FROM pets WHERE restore = 'true'";
	private static final String INIT_SUMMONS = "SELECT ownerId, summonId FROM character_summons";
	private static final String LOAD_SUMMON = "SELECT summonSkillId, summonId, curHp, curMp, time FROM character_summons WHERE ownerId = ?";
	private static final String REMOVE_SUMMON = "DELETE FROM character_summons WHERE ownerId = ? and summonId = ?";
	private static final String REMOVE_ALL_SUMMONS = "DELETE FROM character_summons WHERE ownerId = ?";
	private static final String SAVE_SUMMON = "REPLACE INTO character_summons (ownerId,summonId,summonSkillId,curHp,curMp,time) VALUES (?,?,?,?,?,?)";
	
	/**
	 * Retrieves the current collection of pets.<br>
	 * This method returns a {@code Map} containing pet data.
	 * @return A {@code Map<Integer, Integer>} representing all pets.
	 */
	public Map<Integer, Integer> getPets()
	{
		return _pets;
	}
	
	/**
	 * Retrieves the current collection of servitors.<br>
	 * The data is stored in a {@code Map} where keys are unique identifiers.
	 * @return A {@code Map} containing sets of servitor IDs.
	 */
	public Map<Integer, Set<Integer>> getServitors()
	{
		return _servitors;
	}
	
	/**
	 * Initializes the summon data from the database.<br>
	 * This method loads saved servitors and pets based on the current configuration.<br>
	 * It populates internal maps if the respective restore options are enabled.
	 */
	public void init()
	{
		if (PlayerConfig.RESTORE_SERVITOR_ON_RECONNECT)
		{
			try (Connection con = DatabaseFactory.getConnection();
				Statement s = con.createStatement();
				ResultSet rs = s.executeQuery(INIT_SUMMONS))
			{
				while (rs.next())
				{
					_servitors.computeIfAbsent(rs.getInt("ownerId"), _ -> ConcurrentHashMap.newKeySet()).add(rs.getInt("summonId"));
				}
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Error while loading saved servitor: " + e);
			}
		}
		
		if (PlayerConfig.RESTORE_PET_ON_RECONNECT)
		{
			try (Connection con = DatabaseFactory.getConnection();
				Statement s = con.createStatement();
				ResultSet rs = s.executeQuery(INIT_PET))
			{
				while (rs.next())
				{
					_pets.put(rs.getInt("ownerId"), rs.getInt("item_obj_id"));
				}
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Error while loading saved pet: " + e);
			}
		}
	}
	
	/**
	 * Removes a specific servitor from a player's active list.<br>
	 * This method updates the internal cache and the database.
	 * @param player The {@code Player} who owns the servitor.
	 * @param summonObjectId The unique identifier of the servitor to remove.
	 */
	public void removeServitor(Player player, int summonObjectId)
	{
		final int ownerObjectId = player.getObjectId();
		_servitors.computeIfPresent(ownerObjectId, (_, v) ->
		{
			v.remove(summonObjectId);
			return !v.isEmpty() ? v : null;
		});
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(REMOVE_SUMMON))
		{
			ps.setInt(1, ownerObjectId);
			ps.setInt(2, summonObjectId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Summon cannot be removed: " + e);
		}
	}
	
	/**
	 * Restores a pet for the specified {@code Player}.<br>
	 * This method spawns the pet based on the summoning item in the player's inventory.<br>
	 * It initializes the pet stats and sets it to follow the owner.
	 * @param player The {@code Player} who owns the pet.
	 */
	public void restorePet(Player player)
	{
		final Integer controlObjectId = _pets.get(player.getObjectId());
		if (controlObjectId == null)
		{
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(controlObjectId);
		if (item == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Null pet summoning item for: " + player);
			return;
		}
		
		final PetDataHolder petData = PetData.getInstance().getPetDataHolderByItemId(item.getId());
		if (petData == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Null pet data for: " + player + " and summoning item: " + item);
			return;
		}
		
		final int npcId = petData.getNpcId();
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcId);
		if (npcTemplate == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Null pet NPC template for: " + player + " and pet Id:" + npcId);
			return;
		}
		
		final Pet pet = Pet.spawnPet(npcTemplate, player, item);
		if (pet == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Null pet instance for: " + player + " and pet NPC template:" + npcTemplate);
			return;
		}
		
		pet.setShowSummonAnimation(true);
		pet.setTitle(player.getName());
		
		final boolean petRespawned = pet.isRespawned();
		if (!petRespawned)
		{
			pet.setCurrentHp(pet.getMaxHp());
			pet.setCurrentMp(pet.getMaxMp());
			pet.getStat().setExp(pet.getExpForThisLevel());
			pet.setCurrentFed(pet.getMaxFed());
		}
		
		pet.setRunning();
		
		if (!petRespawned)
		{
			pet.storeMe();
		}
		
		item.setEnchantLevel(pet.getLevel());
		player.setPet(pet);
		pet.spawnMe(player.getX() + 50, player.getY() + 100, player.getZ());
		pet.startFeed();
		pet.setFollowStatus(true);
		pet.getOwner().sendPacket(new PetItemList(pet.getInventory().getItems()));
		pet.broadcastStatusUpdate();
	}
	
	/**
	 * Restores the servitors for a specific player from the database.<br>
	 * This method updates the health, mana, and remaining life time of active servitors.<br>
	 * It ensures that only valid skills are applied to the player.
	 * @param player The {@code Player} whose servitors need to be restored.
	 */
	public void restoreServitor(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_SUMMON))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				Skill skill;
				while (rs.next())
				{
					final int summonObjId = rs.getInt("summonId");
					final int skillId = rs.getInt("summonSkillId");
					removeServitor(player, summonObjId);
					skill = SkillData.getInstance().getSkill(skillId, player.getSkillLevel(skillId));
					if (skill == null)
					{
						continue;
					}
					
					skill.applyEffects(player, player);
					
					if (player.hasServitors())
					{
						Servitor servitor = null;
						for (Summon summon : player.getServitors().values())
						{
							if (summon instanceof Servitor)
							{
								final Servitor s = summon.asServitor();
								if (s.getReferenceSkill() == skillId)
								{
									servitor = s;
									break;
								}
							}
						}
						
						if (servitor != null)
						{
							final int curHp = rs.getInt("curHp");
							final int curMp = rs.getInt("curMp");
							final int time = rs.getInt("time");
							servitor.setCurrentHp(curHp);
							servitor.setCurrentMp(curMp);
							servitor.setLifeTimeRemaining(time);
						}
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Servitor cannot be restored: " + e);
		}
	}
	
	/**
	 * Saves the current state of a {@code Servitor} to the database.<br>
	 * This method updates the owner, object ID, skills, health, mana, and remaining life time.<br>
	 * It also adds the summon to the internal tracking map if it is not already present.
	 * @param summon The {@code Servitor} instance to be saved.
	 */
	public void saveSummon(Servitor summon)
	{
		if (summon == null)
		{
			return;
		}
		
		final Player summonOwner = summon.getOwner();
		final int summonOwnerObjectId = summonOwner.getObjectId();
		final int summonObjectId = summon.getObjectId();
		_servitors.computeIfAbsent(summonOwnerObjectId, _ -> ConcurrentHashMap.newKeySet()).add(summonObjectId);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SAVE_SUMMON))
		{
			ps.setInt(1, summonOwnerObjectId);
			ps.setInt(2, summonObjectId);
			ps.setInt(3, summon.getReferenceSkill());
			ps.setInt(4, (int) Math.round(summon.getCurrentHp()));
			ps.setInt(5, (int) Math.round(summon.getCurrentMp()));
			ps.setInt(6, Math.max(0, summon.getLifeTimeRemaining()));
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to store summon: " + summon + " from " + summonOwner + ", error: " + e);
		}
	}
	
	/**
	 * Removes a character from the internal tracking maps.<br>
	 * This method also deletes all associated summons from the database.
	 * @param objectId The unique identifier of the character to remove.
	 */
	public void removeCharacter(int objectId)
	{
		_pets.remove(objectId);
		_servitors.remove(objectId);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(REMOVE_ALL_SUMMONS))
		{
			ps.setInt(1, objectId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Summons cannot be removed for charId " + objectId + ": " + e);
		}
	}
	
	/**
	 * Provides access to the singleton instance of {@code SummonInfoTable}.<br>
	 * Use this method to get the global table for summon information.
	 * @return The singleton instance of {@code SummonInfoTable}.
	 */
	public static SummonInfoTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SummonInfoTable INSTANCE = new SummonInfoTable();
	}
}
