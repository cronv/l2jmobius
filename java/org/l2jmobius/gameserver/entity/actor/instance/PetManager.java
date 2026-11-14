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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.PetDataHolder;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.PetData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.stat.PetStat;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillLaunched;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the lifecycle and interactions of pets within a player instance.<br>
 * This class handles pet spawning, data persistence, and general pet behavior logic.
 */
public class PetManager extends Merchant
{
	/**
	 * Creates a new {@code PetManager} instance using the provided template.<br>
	 * This constructor initializes the manager with a specific {@code InstanceType}.
	 * @param template The {@code NpcTemplate} used to define the manager's properties.
	 */
	public PetManager(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.PetManager);
	}
	
	/**
	 * Retrieves the file path for a specific pet manager HTML template.<br>
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
		
		return "data/html/petmanager/" + pom + ".htm";
	}
	
	/**
	 * Displays the pet manager interface to a specific player.<br>
	 * This method determines the correct HTML file based on the NPC ID and summon status.<br>
	 * It sends the formatted packet to the {@code Player}.
	 * @param player The {@code Player} who will view the chat window.
	 */
	@Override
	public void showChatWindow(Player player)
	{
		final int id = getId();
		String filename = "data/html/petmanager/" + id + ".htm";
		if ((id == 36478) && player.hasSummon())
		{
			filename = "data/html/petmanager/restore-unsummonpet.htm";
		}
		
		final int objectId = getObjectId();
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(objectId));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	/**
	 * Processes bypass commands sent by a {@code Player}.<br>
	 * It handles specific actions like exchange, evolve, and restore.<br>
	 * If the command is not recognized, it calls the superclass method.
	 * @param player The {@code Player} who sent the command.
	 * @param command The string content of the bypass command.
	 */
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("exchange"))
		{
			final String[] params = command.split(" ");
			final int val = Integer.parseInt(params[1]);
			switch (val)
			{
				case 1:
				{
					exchange(player, 7585, 6650);
					break;
				}
				case 2:
				{
					exchange(player, 7583, 6648);
					break;
				}
				case 3:
				{
					exchange(player, 7584, 6649);
					break;
				}
			}
		}
		else if (command.startsWith("evolve"))
		{
			final String[] params = command.split(" ");
			final int val = Integer.parseInt(params[1]);
			boolean ok = false;
			switch (val)
			{
				// Info evolve(player, "curent pet summon item", "new pet summon item", "level required to evolve")
				// To ignore evolve just put value 0 where do you like example: evolve(player, 0, 9882, 55);
				case 1:
				{
					ok = doEvolve(player, this, 2375, 9882, 55);
					break;
				}
				case 2:
				{
					ok = doEvolve(player, this, 9882, 10426, 70);
					break;
				}
				case 3:
				{
					ok = doEvolve(player, this, 6648, 10311, 55);
					break;
				}
				case 4:
				{
					ok = doEvolve(player, this, 6650, 10313, 55);
					break;
				}
				case 5:
				{
					ok = doEvolve(player, this, 6649, 10312, 55);
					break;
				}
			}
			
			if (!ok)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, "data/html/petmanager/evolve_no.htm");
				player.sendPacket(html);
			}
		}
		else if (command.startsWith("restore"))
		{
			final String[] params = command.split(" ");
			final int val = Integer.parseInt(params[1]);
			boolean ok = false;
			switch (val)
			{
				// Info evolve(player, "curent pet summon item", "new pet summon item", "level required to evolve")
				case 1:
				{
					ok = doRestore(player, this, 10307, 9882, 55);
					break;
				}
				case 2:
				{
					ok = doRestore(player, this, 10611, 10426, 70);
					break;
				}
				case 3:
				{
					ok = doRestore(player, this, 10308, 4422, 55);
					break;
				}
				case 4:
				{
					ok = doRestore(player, this, 10309, 4423, 55);
					break;
				}
				case 5:
				{
					ok = doRestore(player, this, 10310, 4424, 55);
					break;
				}
			}
			
			if (!ok)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, "data/html/petmanager/restore_no.htm");
				player.sendPacket(html);
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	/**
	 * This method handles the exchange of items for a {@code Player}.<br>
	 * It removes one item of type {@code itemIdtake} and gives one item of type {@code itemIdgive}.<br>
	 * If the removal fails, it sends an error message to the player.<br>
	 * If successful, it sends a success message to the player.
	 * @param player The {@code Player} who is performing the exchange.
	 * @param itemIdtake The unique ID of the item to be removed from the inventory.
	 * @param itemIdgive The unique ID of the item to be given to the player.
	 */
	public void exchange(Player player, int itemIdtake, int itemIdgive)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		if (player.destroyItemByItemId(null, itemIdtake, 1, this, true))
		{
			player.addItem(ItemProcessType.NONE, itemIdgive, 1, this, true);
			html.setFile(player, "data/html/petmanager/" + getId() + ".htm");
			player.sendPacket(html);
		}
		else
		{
			html.setFile(player, "data/html/petmanager/exchange_no.htm");
			player.sendPacket(html);
		}
	}
	
	/**
	 * Handles the evolution process of a player's pet.<br>
	 * Checks if the player meets all requirements to evolve their current pet into a new form.<br>
	 * If successful, it replaces the old pet with a new one and preserves its name and experience.
	 * @param player The {@code Player} who owns the pet.
	 * @param npc The {@code Npc} performing the evolution.
	 * @param itemIdtake The item ID of the current pet.
	 * @param itemIdgive The item ID of the new evolved pet.
	 * @param petminLevel The minimum level required for evolution.
	 * @return {@code true} if the evolution was successful, {@code false} otherwise.
	 */
	private boolean doEvolve(Player player, Npc npc, int itemIdtake, int itemIdgive, int petminLevel)
	{
		if ((itemIdtake == 0) || (itemIdgive == 0) || (petminLevel == 0))
		{
			return false;
		}
		
		final Summon pet = player.getPet();
		if (pet == null)
		{
			return false;
		}
		
		final Pet currentPet = pet.asPet();
		if (currentPet.isAlikeDead())
		{
			PunishmentManager.handleIllegalPlayerAction(player, player + " tried to use death pet exploit!", GeneralConfig.DEFAULT_PUNISH);
			return false;
		}
		
		Item item = null;
		final PetDataHolder oldData = PetData.getInstance().getPetDataHolderByItemId(itemIdtake);
		if (oldData == null)
		{
			return false;
		}
		
		final int oldnpcID = oldData.getNpcId();
		final PetStat currentPetStat = currentPet.getStat();
		if ((currentPetStat.getLevel() < petminLevel) || (currentPet.getId() != oldnpcID))
		{
			return false;
		}
		
		final PetDataHolder petData = PetData.getInstance().getPetDataHolderByItemId(itemIdgive);
		if (petData == null)
		{
			return false;
		}
		
		final int npcID = petData.getNpcId();
		if (npcID == 0)
		{
			return false;
		}
		
		long petexp = currentPetStat.getExp();
		final String oldname = currentPet.getName();
		final int oldX = currentPet.getX();
		final int oldY = currentPet.getY();
		final int oldZ = currentPet.getZ();
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcID);
		currentPet.unSummon(player);
		
		// Deleting old pet item.
		currentPet.destroyControlItem(player, true);
		item = player.getInventory().addItem(ItemProcessType.RESTORE, itemIdgive, 1, player, npc);
		
		// Summoning new pet
		final Pet petSummon = Pet.spawnPet(npcTemplate, player, item);
		if (petSummon == null)
		{
			return false;
		}
		
		// Fix for non-linear baby pet exp.
		final PetStat petSummonStat = petSummon.getStat();
		final long _minimumexp = petSummonStat.getExpForLevel(petminLevel);
		if (petexp < _minimumexp)
		{
			petexp = _minimumexp;
		}
		
		petSummonStat.addExp(petexp);
		petSummon.setCurrentHp(petSummon.getMaxHp());
		petSummon.setCurrentMp(petSummon.getMaxMp());
		petSummon.setCurrentFed(petSummon.getMaxFed());
		petSummon.setTitle(player.getName());
		petSummon.setName(oldname);
		petSummon.setRunning();
		petSummon.storeMe();
		
		player.setPet(petSummon);
		
		player.sendPacket(new MagicSkillUse(npc, 2046, 1, 1000, 600000));
		player.sendPacket(SystemMessageId.SUMMONING_YOUR_PET);
		petSummon.spawnMe(oldX, oldY, oldZ);
		petSummon.startFeed();
		item.setEnchantLevel(petSummon.getLevel());
		
		ThreadPool.schedule(new EvolveFinalizer(player, petSummon), 900);
		if (petSummon.getCurrentFed() <= 0)
		{
			ThreadPool.schedule(new EvolveFeedWait(player, petSummon), 60000);
		}
		else
		{
			petSummon.startFeed();
		}
		
		return true;
	}
	
	/**
	 * Restores a pet by replacing an old item with a new one.<br>
	 * This method removes the old pet from the database and inventory.<br>
	 * It then spawns a new pet with stats based on the previous level.
	 * @param player The {@code Player} who owns the pet.
	 * @param npc The {@code Npc} performing the restoration.
	 * @param itemIdtake The item ID of the old pet to be removed.
	 * @param itemIdgive The item ID of the new pet to be given.
	 * @param petminLevel The minimum level for the restored pet.
	 * @return {@code true} if the restoration was successful, {@code false} otherwise.
	 */
	private boolean doRestore(Player player, Npc npc, int itemIdtake, int itemIdgive, int petminLevel)
	{
		if ((itemIdtake == 0) || (itemIdgive == 0) || (petminLevel == 0))
		{
			return false;
		}
		
		final Item item = player.getInventory().getItemByItemId(itemIdtake);
		if (item == null)
		{
			return false;
		}
		
		int oldpetlvl = item.getEnchantLevel();
		if (oldpetlvl < petminLevel)
		{
			oldpetlvl = petminLevel;
		}
		
		final PetDataHolder oldData = PetData.getInstance().getPetDataHolderByItemId(itemIdtake);
		if (oldData == null)
		{
			return false;
		}
		
		final PetDataHolder petData = PetData.getInstance().getPetDataHolderByItemId(itemIdgive);
		if (petData == null)
		{
			return false;
		}
		
		final int npcId = petData.getNpcId();
		if (npcId == 0)
		{
			return false;
		}
		
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcId);
		
		// Deleting old pet item.
		final PlayerInventory playerInventory = player.getInventory();
		final Item removedItem = playerInventory.destroyItem(ItemProcessType.RESTORE, item, player, npc);
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(removedItem);
		player.sendPacket(sm);
		
		// Give new pet item.
		final Item addedItem = playerInventory.addItem(ItemProcessType.RESTORE, itemIdgive, 1, player, npc);
		
		// Summoning new pet
		final Pet petSummon = Pet.spawnPet(npcTemplate, player, addedItem);
		if (petSummon == null)
		{
			return false;
		}
		
		final PetStat petSummonStat = petSummon.getStat();
		final long _maxexp = petSummonStat.getExpForLevel(oldpetlvl);
		petSummonStat.addExp(_maxexp);
		petSummon.setCurrentHp(petSummon.getMaxHp());
		petSummon.setCurrentMp(petSummon.getMaxMp());
		petSummon.setCurrentFed(petSummon.getMaxFed());
		petSummon.setTitle(player.getName());
		petSummon.setRunning();
		petSummon.storeMe();
		
		player.setPet(petSummon);
		
		player.sendPacket(new MagicSkillUse(npc, 2046, 1, 1000, 600000));
		player.sendPacket(SystemMessageId.SUMMONING_YOUR_PET);
		petSummon.spawnMe(player.getX(), player.getY(), player.getZ());
		petSummon.startFeed();
		addedItem.setEnchantLevel(petSummon.getLevel());
		
		// Inventory update
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addRemovedItem(removedItem);
		player.sendInventoryUpdate(iu);
		
		player.broadcastUserInfo();
		
		ThreadPool.schedule(new EvolveFinalizer(player, petSummon), 900);
		if (petSummon.getCurrentFed() <= 0)
		{
			ThreadPool.schedule(new EvolveFeedWait(player, petSummon), 60000);
		}
		else
		{
			petSummon.startFeed();
		}
		
		// Pet control item no longer exists, delete the pet from the db.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?"))
		{
			ps.setInt(1, removedItem.getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			// Ignore.
		}
		
		return true;
	}
	
	private class EvolveFeedWait implements Runnable
	{
		private final Player _player;
		private final Pet _petSummon;
		
		EvolveFeedWait(Player player, Pet petSummon)
		{
			_player = player;
			_petSummon = petSummon;
		}
		
		@Override
		public void run()
		{
			try
			{
				if (_petSummon.getCurrentFed() <= 0)
				{
					_petSummon.unSummon(_player);
				}
				else
				{
					_petSummon.startFeed();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "run: ", e);
			}
		}
	}
	
	private class EvolveFinalizer implements Runnable
	{
		private final Player _player;
		private final Pet _petSummon;
		
		EvolveFinalizer(Player player, Pet petSummon)
		{
			_player = player;
			_petSummon = petSummon;
		}
		
		@Override
		public void run()
		{
			try
			{
				_player.sendPacket(new MagicSkillLaunched(_player, 2046, 1));
				_petSummon.setFollowStatus(true);
				_petSummon.setShowSummonAnimation(false);
			}
			catch (Throwable e)
			{
				LOGGER.log(Level.WARNING, "run: ", e);
			}
		}
	}
}
