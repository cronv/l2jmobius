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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.CursedWeapon;
import org.l2jmobius.gameserver.entity.actor.instance.Defender;
import org.l2jmobius.gameserver.entity.actor.instance.FeedableBeast;
import org.l2jmobius.gameserver.entity.actor.instance.FortCommander;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.instance.Guard;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExActivatedCursedTreasureBoxLocation;
import org.l2jmobius.gameserver.network.serverpackets.ExCursedWeaponList;
import org.l2jmobius.gameserver.network.serverpackets.ExCursedWeaponLocation;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the logic and data for cursed weapons within the game world.<br>
 * This class handles loading configurations and tracking active {@code CursedWeapon} instances.
 * @author Micht, Notorion
 */
public class CursedWeaponsManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CursedWeaponsManager.class.getName());
	
	private final Map<Integer, CursedWeapon> _cursedWeapons = new HashMap<>();
	
	// NPCs.
	public static final int ZARICHE_BOX_NPC_ID = 24370;
	public static final int AKAMANAH_BOX_NPC_ID = 24371;
	
	public static final int ZARICHE_WEAPON_ID = 1;
	public static final int AKAMANAH_WEAPON_ID = 2;
	
	private final List<Location> _zaricheBoxLocs = new CopyOnWriteArrayList<>();
	private final List<Location> _akamanahBoxLocs = new CopyOnWriteArrayList<>();
	
	// Time Lock.
	private boolean _isEventActive = false;
	
	/**
	 * Initializes the {@code CursedWeaponsManager}.<br>
	 * This constructor calls the {@code load()} method to prepare the manager.<br>
	 * It also schedules a recurring task to check the event status every 60000ms.
	 */
	protected CursedWeaponsManager()
	{
		load();
		
		// Checks the time every 1 minute (60000ms).
		ThreadPool.scheduleAtFixedRate(this::checkEventStatus, 1000, 60000);
	}
	
	/**
	 * Checks if the current event is active.
	 * @return {@code true} if the event is running, {@code false} otherwise.
	 */
	public boolean isEventActive()
	{
		return _isEventActive;
	}
	
	/**
	 * Checks the current time to determine if the event should be active.<br>
	 * It verifies that the day is either {@code Monday} or {@code Thursday}.<br>
	 * The event starts between {@code 18} and {@code 23} hours.<br>
	 * It automatically ends the event and clears visuals outside of these times.
	 */
	public void checkEventStatus()
	{
		final Calendar now = Calendar.getInstance();
		final int day = now.get(Calendar.DAY_OF_WEEK);
		final int hour = now.get(Calendar.HOUR_OF_DAY);
		final int minute = now.get(Calendar.MINUTE);
		
		// 1. Week day verification: Monday and Thursday.
		if ((day != Calendar.MONDAY) && (day != Calendar.THURSDAY))
		{
			if (_isEventActive)
			{
				endAllWeapons();
				_isEventActive = false;
				clearEventVisuals();
			}
			return;
		}
		
		// 2. Event time.
		if ((hour >= 18) && (hour < 23))
		{
			if (!_isEventActive)
			{
				_isEventActive = true;
				LOGGER.info("Cursed Weapons Defense Event Started.");
			}
		}
		// 3. Deactivate and clean outside 18h/23h schedule.
		else
		{
			if (_isEventActive)
			{
				endAllWeapons();
				_isEventActive = false;
				clearEventVisuals();
				LOGGER.info("Cursed Weapons Event Ended (Time Schedule).");
			}
			// Emergency redundancy (23:01).
			else if ((hour == 23) && (minute == 1))
			{
				endAllWeapons();
				clearEventVisuals();
			}
		}
	}
	
	// Clean cursed sword icons and treasure chest icons.
	/**
	 * Removes all active event visuals from the game world.<br>
	 * This method clears the internal location lists for Zariche and Akamanah boxes.<br>
	 * It broadcasts updates to reset the visual state of cursed weapons and treasure boxes.
	 */
	public void clearEventVisuals()
	{
		final List<ExCursedWeaponLocation.CursedWeaponInfo> splitList = new ArrayList<>();
		final Location locZ = new Location(100000, 100000, 0);
		final Location locA = new Location(-100000, -100000, 0);
		splitList.add(new ExCursedWeaponLocation.CursedWeaponInfo(locZ, 8190, 1, 0L));
		splitList.add(new ExCursedWeaponLocation.CursedWeaponInfo(locA, 8689, 1, 0L));
		broadcastToWorldExceptConquest(new ExCursedWeaponLocation(splitList));
		
		_zaricheBoxLocs.clear();
		_akamanahBoxLocs.clear();
		broadcastToWorldExceptConquest(new ExActivatedCursedTreasureBoxLocation(8190, _zaricheBoxLocs));
		broadcastToWorldExceptConquest(new ExActivatedCursedTreasureBoxLocation(8689, _akamanahBoxLocs));
		broadcastToWorldExceptConquest(new ExCursedWeaponLocation(Collections.emptyList()));
	}
	
	/**
	 * This method ends all currently active cursed weapons.<br>
	 * It iterates through every {@code CursedWeapon} in the collection.<br>
	 * If a weapon is active, it calls its {@code endOfLife()} method to remove it from the world.
	 */
	private void endAllWeapons()
	{
		for (CursedWeapon cw : _cursedWeapons.values())
		{
			// End active Sword Weapons so they don't remain in the world.
			if (cw.isActive())
			{
				cw.endOfLife();
			}
		}
	}
	
	/**
	 * Adds a new location for a specific treasure box to the active event.<br>
	 * This method checks if the {@code npcId} matches known chest types.<br>
	 * It updates the internal list and broadcasts the change to the world.
	 * @param npcId The unique identifier of the NPC box.
	 * @param loc The {@code Location} where the box is placed.
	 */
	public void addBoxLocation(int npcId, Location loc)
	{
		if (!_isEventActive)
		{
			return;
		}
		
		// Zariche Treasure Chests (24370).
		if (npcId == ZARICHE_BOX_NPC_ID)
		{
			if (!_zaricheBoxLocs.contains(loc))
			{
				_zaricheBoxLocs.add(loc);
				broadcastToWorldExceptConquest(new ExActivatedCursedTreasureBoxLocation(npcId, _zaricheBoxLocs));
			}
		}
		// Akamanah Treasure Chests (24371).
		else if (npcId == AKAMANAH_BOX_NPC_ID)
		{
			if (!_akamanahBoxLocs.contains(loc))
			{
				_akamanahBoxLocs.add(loc);
				broadcastToWorldExceptConquest(new ExActivatedCursedTreasureBoxLocation(npcId, _akamanahBoxLocs));
			}
		}
	}
	
	/**
	 * Removes a specific location for a treasure box from the active list.<br>
	 * This method updates the internal storage and broadcasts the change to the world.
	 * @param npcId The unique identifier of the NPC associated with the box.
	 * @param loc The {@code Location} object representing where the box was removed.
	 */
	public void removeBoxLocation(int npcId, Location loc)
	{
		// Zariche Treasure Chests (24370).
		if (npcId == ZARICHE_BOX_NPC_ID)
		{
			_zaricheBoxLocs.remove(loc);
			broadcastToWorldExceptConquest(new ExActivatedCursedTreasureBoxLocation(npcId, _zaricheBoxLocs));
		}
		// Akamanah Treasure Chests (24371).
		else if (npcId == AKAMANAH_BOX_NPC_ID)
		{
			_akamanahBoxLocs.remove(loc);
			broadcastToWorldExceptConquest(new ExActivatedCursedTreasureBoxLocation(npcId, _akamanahBoxLocs));
		}
	}
	
	/**
	 * Retrieves the list of locations for Zariche boxes.<br>
	 * This method returns an empty {@code List} if the event is not active.
	 * @return A {@code List} of {@code Location} objects.
	 */
	public List<Location> getZaricheBoxLocs()
	{
		if (!_isEventActive)
		{
			return java.util.Collections.emptyList();
		}
		
		return _zaricheBoxLocs;
	}
	
	/**
	 * Retrieves the list of locations for Akamanah boxes.<br>
	 * Returns an empty {@code List} if the event is not active.
	 * @return a {@code List} of {@code Location} objects.
	 */
	public List<Location> getAkamanahBoxLocs()
	{
		if (!_isEventActive)
		{
			return java.util.Collections.emptyList();
		}
		
		return _akamanahBoxLocs;
	}
	
	/**
	 * Loads the cursed weapons data from the configuration files.<br>
	 * This method checks if cursed weapons are enabled in {@code GeneralConfig}.<br>
	 * It parses the XML file and initializes the internal weapon list.
	 */
	@Override
	public void load()
	{
		if (!GeneralConfig.ALLOW_CURSED_WEAPONS)
		{
			return;
		}
		
		parseDatapackFile("data/CursedWeapons.xml");
		restore();
		controlPlayers();
		// LOGGER.info(getClass().getSimpleName() + ": Loaded " + _cursedWeapons.size() + " cursed weapons.");
	}
	
	/**
	 * Updates the kill count for a specific cursed weapon.<br>
	 * This method checks if the item exists before updating its data.
	 * @param itemId The unique identifier of the cursed weapon.
	 * @param victim The {@code Player} who was killed by the weapon.
	 */
	public void increaseKills(int itemId, Player victim)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);
		if (cw != null)
		{
			cw.increaseKills(victim);
		}
	}
	
	/**
	 * Parses the provided {@code Document} to load cursed weapons.<br>
	 * It reads item data from the XML structure and stores them in a map.
	 * @param document The {@code Document} object containing the weapon data.
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
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
						final int skillId = Integer.parseInt(attrs.getNamedItem("skillId").getNodeValue());
						final String name = attrs.getNamedItem("name").getNodeValue();
						final CursedWeapon cw = new CursedWeapon(id, skillId, name);
						int val;
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							// No longer by chance rate, during the event it is 100% configured in cursedweapon.java.
							if ("dropRate".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								cw.setDropRate(val);
							}
							else if ("duration".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								cw.setDuration(val);
							}
							else if ("durationLost".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								cw.setDurationLost(val);
							}
							else if ("disapearChance".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								cw.setDisapearChance(val);
							}
							else if ("stageKills".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								cw.setStageKills(val);
							}
						}
						
						_cursedWeapons.put(id, cw);
					}
				}
			}
		}
	}
	
	/**
	 * Restores cursed weapon data from the database.<br>
	 * It updates existing objects in the {@code _cursedWeapons} map.<br>
	 * This method handles connection management and error logging.
	 */
	private void restore()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT itemId, charId, playerReputation, playerPkKills, nbKills, endTime FROM cursed_weapons"))
		{
			// LOGGER.info("[CW-DEBUG] private void restore in manage");
			// Restore data for each cursed weapon from the database.
			CursedWeapon cw;
			while (rs.next())
			{
				cw = _cursedWeapons.get(rs.getInt("itemId"));
				cw.setPlayerId(rs.getInt("charId"));
				cw.setPlayerReputation(rs.getInt("playerReputation"));
				cw.setPlayerPkKills(rs.getInt("playerPkKills"));
				cw.setNbKills(rs.getInt("nbKills"));
				cw.setEndTime(rs.getLong("endTime"));
				cw.reActivate();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not restore CursedWeapons data: ", e);
		}
	}
	
	/**
	 * Checks for cursed weapons held by players that should not have them.<br>
	 * It removes the item from the player's inventory if a mismatch is found.<br>
	 * The method also restores the player's original reputation and PK count.<br>
	 * Finally, it cleans up the corrupted data from the database.
	 */
	private void controlPlayers()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT owner_id FROM items WHERE item_id=?"))
		{
			// Remove the weapon from those who shouldn't have it.
			for (CursedWeapon cw : _cursedWeapons.values())
			{
				if (cw.isActivated())
				{
					continue;
				}
				
				final int itemId = cw.getItemId();
				ps.setInt(1, itemId);
				try (ResultSet rset = ps.executeQuery())
				{
					if (rset.next())
					{
						final int playerId = rset.getInt("owner_id");
						LOGGER.info("PROBLEM : Player " + playerId + " owns the cursed weapon " + itemId + " but he shouldn't.");
						
						// Deletes the item from inventory.
						try (PreparedStatement delete = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?"))
						{
							delete.setInt(1, playerId);
							delete.setInt(2, itemId);
							delete.executeUpdate();
						}
						catch (Exception e)
						{
							LOGGER.warning("Error while deleting cursed weapon " + itemId + " from userId " + playerId);
						}
						
						// Restore Karma and PK.
						try (PreparedStatement update = con.prepareStatement("UPDATE characters SET reputation=?, pkkills=? WHERE charId=?"))
						{
							update.setInt(1, cw.getPlayerReputation());
							update.setInt(2, cw.getPlayerPkKills());
							update.setInt(3, playerId);
							update.executeUpdate();
						}
						
						// Remove skills from this specific player (in case the weapon still existed).
						try (PreparedStatement delSkills = con.prepareStatement("DELETE FROM character_skills WHERE charId=? AND skill_id IN (35398, 35400, 35399, 35401)"))
						{
							delSkills.setInt(1, playerId);
							delSkills.executeUpdate();
						}
						
						removeFromDb(itemId);
					}
				}
			}
			
			try (PreparedStatement psClean = con.prepareStatement("DELETE FROM character_skills WHERE skill_id IN (35398, 35399, 35400, 35401) " + "AND charId NOT IN (SELECT owner_id FROM items WHERE item_id IN (8190, 8689))"))
			{
				final int cleaned = psClean.executeUpdate();
				if (cleaned > 0)
				{
					// LOGGER.info("CursedWeaponsManager: Cleaned orphaned skills from " + cleaned + " players.");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not check CursedWeapons data: ", e);
		}
	}
	
	/**
	 * Checks if a cursed weapon should drop from an entity.<br>
	 * This method ignores specific types like {@code Defender} or {@code GrandBoss}.<br>
	 * It iterates through all active cursed weapons to see if any conditions are met.
	 * @param attackable The entity being attacked.
	 * @param player The player who performed the action.
	 */
	public synchronized void checkDrop(Attackable attackable, Player player)
	{
		// Original block list.
		if ((attackable instanceof Defender) || (attackable instanceof Guard) || (attackable instanceof GrandBoss) || (attackable instanceof FeedableBeast) || (attackable instanceof FortCommander))
		{
			return;
		}
		
		// Block after 23h.
		final Calendar now = Calendar.getInstance();
		final int h = now.get(Calendar.HOUR_OF_DAY);
		final int m = now.get(Calendar.MINUTE);
		final int s = now.get(Calendar.SECOND);
		
		// Logic for absolute block for possession of cursed swords.
		// 1. (h >= 23) -> Blocks 23:00, 23:01, 00:00...
		// 2. (h < 18) -> Blocks morning/afternoon.
		// 3. (h == 22 && m == 59) -> Blocks entire 22:59.
		// 4. (h == 22 && m == 58 && s >= 50) -> Blocks the final 10s.
		if ((h >= 23) || (h < 18) || ((h == 22) && ((m == 59) || ((m == 58) && (s >= 50)))))
		{
			return; // Cancels drop.
		}
		
		// 1. Time Lock (Controlled by checkEventStatus).
		if (!_isEventActive)
		{
			return;
		}
		
		// 3. Blocks and rules (Level, Instance, Zone).
		if (attackable.getLevel() < 95)
		{
			return;
		}
		
		if (attackable.getInstanceId() > 0)
		{
			return;
		}
		
		// 4. Drop block in Conquest.
		final int x = attackable.getX();
		final int y = attackable.getY();
		for (ZoneType zone : ZoneManager.getInstance().getZones(x, y, attackable.getZ()))
		{
			final String zoneName = zone.getName();
			if ((zoneName != null) && zoneName.equalsIgnoreCase("Conquest"))
			{
				return; // In Conquest area: Does not drop.
			}
		}
		
		// 3. Random drop between Cursed Swords.
		final List<CursedWeapon> shuffledWeapons = new ArrayList<>(_cursedWeapons.values());
		Collections.shuffle(shuffledWeapons);
		
		// Current region of the monster.
		final int currentRegionId = MapRegionData.getInstance().getMapRegionLocId(x, y);
		for (CursedWeapon cw : shuffledWeapons)
		{
			if (cw.isActive())
			{
				continue;
			}
			
			// Region block.
			boolean regionOccupied = false;
			for (CursedWeapon activeCw : _cursedWeapons.values())
			{
				// Checks active Cursed Swords.
				if (activeCw.isActive() && (activeCw.getWorldPosition() != null))
				{
					// Region of the active Sword.
					final int activeRegionId = MapRegionData.getInstance().getMapRegionLocId(activeCw.getWorldPosition().getX(), activeCw.getWorldPosition().getY());
					
					// If the region is the same, prohibits the drop.
					if (currentRegionId == activeRegionId)
					{
						regionOccupied = true;
						break;
					}
				}
			}
			
			if (regionOccupied)
			{
				continue; // Region occupied, tries the next sword.
			}
			
			// If the region is free, tries the real drop (Chance %).
			if (cw.checkDrop(attackable, player))
			{
				break;
			}
		}
	}
	
	/**
	 * Activates a cursed weapon for a specific player.<br>
	 * This method handles logic for equipping the item.<br>
	 * If the player already has a cursed weapon, it updates the old one and expires the new one.<br>
	 * Otherwise, it binds the new cursed weapon to the {@code Player}.
	 * @param player The {@code Player} who is interacting with the item.
	 * @param item The {@code Item} being activated.
	 */
	public void activate(Player player, Item item)
	{
		final CursedWeapon cw = _cursedWeapons.get(item.getId());
		if (player.isCursedWeaponEquipped())
		{
			final CursedWeapon cw2 = _cursedWeapons.get(player.getCursedWeaponEquippedId());
			
			// Update the kill count based on the stage kills.
			cw2.setNbKills(cw2.getStageKills() - 1);
			cw2.increaseKills();
			cw.setPlayer(player); // Necessary to identify the inventory location.
			cw.endOfLife(); // Expire the weapon and clean up.
		}
		else
		{
			cw.activate(player, item);
		}
	}
	
	/**
	 * Causes a cursed weapon to drop for the specified killer.<br>
	 * This method retrieves the {@code CursedWeapon} by its ID and triggers the drop logic.
	 * @param itemId The unique identifier of the item to drop.
	 * @param killer The {@code Creature} that killed the target and will receive the drop.
	 */
	public void drop(int itemId, Creature killer)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);
		cw.dropIt(killer);
	}
	
	/**
	 * Increases the kill count for a specific cursed weapon.<br>
	 * This method retrieves the {@code CursedWeapon} associated with the provided ID.<br>
	 * It then calls the internal increase logic on that object.
	 * @param itemId The unique identifier of the item to update.
	 */
	public void increaseKills(int itemId)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);
		cw.increaseKills();
	}
	
	/**
	 * Retrieves the current level of a cursed weapon.<br>
	 * This method looks up the weapon by its unique ID.
	 * @param itemId The unique identifier of the cursed weapon.
	 * @return The integer level of the requested cursed weapon.
	 */
	public int getLevel(int itemId)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);
		return cw.getLevel();
	}
	
	/**
	 * Sends a system message to all online players.<br>
	 * This method uses {@code World.broadcastToAllOnlinePlayers} to deliver the content.
	 * @param sm The {@code SystemMessage} object containing the text to display.
	 */
	public static void announce(SystemMessage sm)
	{
		World.broadcastToAllOnlinePlayers(sm);
	}
	
	/**
	 * Validates and updates the status of cursed weapons for a specific player.<br>
	 * This method checks if the {@code Player} owns an active {@code CursedWeapon}.<br>
	 * If active, it refreshes the weapon data and sends a usage time message to the player.
	 * @param player The {@code Player} object to check.
	 */
	public void checkPlayer(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		if (player.isInsideZone(ZoneId.CONQUEST))
		{
			clearSinglePlayerScreen(player);
		}
		
		if (!_isEventActive)
		{
			// Check if player has a registered cursed sword and clear without drop.
			for (CursedWeapon cw : _cursedWeapons.values())
			{
				if (cw.isActivated() && (player.getObjectId() == cw.getPlayerId()))
				{
					// Bind player so forceClearWithoutDrop() works properly.
					cw.setPlayer(player);
					cw.setItem(player.getInventory().getItemByItemId(cw.getItemId()));
					
					// Direct cleanup: no drop, no endOfLife(), no risk of re-drop.
					cw.forceClearWithoutDrop();
					
					clearEventVisuals();
					return;
				}
			}
			
			// No sword: clear only visual packets.
			clearSinglePlayerScreen(player);
			return;
		}
		
		// Event ACTIVE: logic.
		for (CursedWeapon cw : _cursedWeapons.values())
		{
			if (cw.isActivated() && (player.getObjectId() == cw.getPlayerId()))
			{
				cw.setPlayer(player);
				cw.setItem(player.getInventory().getItemByItemId(cw.getItemId()));
				cw.giveSkill();
				player.setCursedWeaponEquippedId(cw.getItemId());
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S_REMAINING_TIME_S2_MIN_TYPE_CURSEDSWORD_TO_CHECK_OTHER_INFORMATION);
				sm.addString(cw.getName());
				sm.addInt((int) ((cw.getEndTime() - System.currentTimeMillis()) / 60000));
				player.sendPacket(sm);
			}
		}
	}
	
	/**
	 * Checks if a player owns an active cursed weapon.<br>
	 * It searches through all managed weapons to find a match for the given ID.
	 * @param ownerId The unique identifier of the player to check.
	 * @return The {@code int} item ID of the owned weapon, or -1 if none is found.
	 */
	public int checkOwnsWeaponId(int ownerId)
	{
		for (CursedWeapon cw : _cursedWeapons.values())
		{
			if (cw.isActivated() && (ownerId == cw.getPlayerId()))
			{
				return cw.getItemId();
			}
		}
		
		return -1;
	}
	
	/**
	 * Removes a specific item from the database.<br>
	 * This method deletes the entry associated with the provided {@code itemId}.<br>
	 * It handles any {@code SQLException} by logging an error message.
	 * @param itemId The unique identifier of the item to remove.
	 */
	public static void removeFromDb(int itemId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM cursed_weapons WHERE itemId = ?"))
		{
			// LOGGER.info("[CW-DEBUG] public static void removeFromDb ");
			ps.setInt(1, itemId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Failed to remove data: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Saves all current cursed weapon data to the database.<br>
	 * This method iterates through every {@code CursedWeapon} in the manager.<br>
	 * It calls the {@code saveData()} method on each individual weapon.
	 */
	public void saveData()
	{
		for (CursedWeapon cw : _cursedWeapons.values())
		{
			// LOGGER.info("[CW-DEBUG] savedata manage");
			cw.saveData();
		}
	}
	
	/**
	 * Checks if a specific item is currently cursed.<br>
	 * This method looks up the {@code itemId} in the internal map.
	 * @param itemId The unique identifier of the item to check.
	 * @return {@code true} if the item is cursed, otherwise {@code false}.
	 */
	public boolean isCursed(int itemId)
	{
		return _cursedWeapons.containsKey(itemId);
	}
	
	/**
	 * Retrieves all cursed weapons currently managed by this system.
	 * @return A {@code Collection} of {@code CursedWeapon} objects.
	 */
	public Collection<CursedWeapon> getCursedWeapons()
	{
		return _cursedWeapons.values();
	}
	
	/**
	 * Retrieves the unique identifiers for all cursed weapons.<br>
	 * This method returns a set of {@code Integer} values from the internal map.
	 * @return A {@code Set} containing the IDs of all registered cursed weapons.
	 */
	public Set<Integer> getCursedWeaponsIds()
	{
		return _cursedWeapons.keySet();
	}
	
	/**
	 * Retrieves a {@code CursedWeapon} based on the provided item ID.<br>
	 * This method looks up the weapon in the internal map.<br>
	 * It returns {@code null} if no cursed weapon is found for that ID.
	 * @param itemId The unique identifier of the item to look up.
	 * @return The corresponding {@code CursedWeapon} object or {@code null}.
	 */
	public CursedWeapon getCursedWeapon(int itemId)
	{
		return _cursedWeapons.get(itemId);
	}
	
	/**
	 * Grants the passive skill to a specific cursed weapon.<br>
	 * This method retrieves the {@code CursedWeapon} by its ID and applies the skill.<br>
	 * Any errors during the process are caught and ignored.
	 * @param itemId The unique identifier of the item to receive the passive skill.
	 */
	public void givePassive(int itemId)
	{
		try
		{
			_cursedWeapons.get(itemId).giveSkill();
		}
		catch (Exception e)
		{
		}
	}
	
	/**
	 * Sends a packet to all online players in the world.<br>
	 * Players currently inside the {@code ZoneId.CONQUEST} zone will not receive it.<br>
	 * If a player is in the conquest zone, their screen is cleared instead.
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
	 * Stops the current event immediately.<br>
	 * This method shuts down all active weapons and clears visual effects.<br>
	 * It sets the internal state to {@code false}.
	 */
	public void endEventForced()
	{
		endAllWeapons();
		_isEventActive = false;
		clearEventVisuals();
	}
	
	/**
	 * Clears the cursed weapon information from the player's screen.<br>
	 * This method sends empty packets to reset active lists and locations.<br>
	 * It ensures that all visual data for cursed weapons is removed.
	 * @param player The {@code Player} whose screen needs to be cleared.
	 */
	public void clearSinglePlayerScreen(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		final Set<Integer> emptyActiveList = new HashSet<>();
		player.sendPacket(new ExCursedWeaponList(emptyActiveList));
		
		final List<ExCursedWeaponLocation.CursedWeaponInfo> splitList = new ArrayList<>();
		final Location locZ = new Location(100000, 100000, 0);
		final Location locA = new Location(-100000, -100000, 0);
		splitList.add(new ExCursedWeaponLocation.CursedWeaponInfo(locZ, 8190, 1, 0L));
		splitList.add(new ExCursedWeaponLocation.CursedWeaponInfo(locA, 8689, 1, 0L));
		player.sendPacket(new ExCursedWeaponLocation(splitList));
		
		player.sendPacket(new ExCursedWeaponLocation(Collections.emptyList()));
		player.sendPacket(new ExActivatedCursedTreasureBoxLocation(8190, Collections.emptyList()));
		player.sendPacket(new ExActivatedCursedTreasureBoxLocation(8689, Collections.emptyList()));
	}
	
	/**
	 * Retrieves the singleton instance of the {@code CursedWeaponsManager}.<br>
	 * Use this method to access the manager from anywhere in the code.
	 * @return The active {@code CursedWeaponsManager} instance.
	 */
	public static CursedWeaponsManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CursedWeaponsManager INSTANCE = new CursedWeaponsManager();
	}
}
