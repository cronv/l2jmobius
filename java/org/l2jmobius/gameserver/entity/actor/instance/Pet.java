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
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.holders.PetDataHolder;
import org.l2jmobius.gameserver.data.holders.PetEffect;
import org.l2jmobius.gameserver.data.holders.PetLevelData;
import org.l2jmobius.gameserver.data.sql.PetEffectTable;
import org.l2jmobius.gameserver.data.sql.SummonInfoTable;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.PetData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.stat.PetStat;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PetInventory;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeNpcState;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PetInventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PetItemList;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;

/**
 * Represents a pet entity that follows and assists its owner.<br>
 * This class handles the specific behaviors, stats, and inventory management for pets in the game world.
 */
public class Pet extends Summon
{
	protected static final Logger LOGGER_PET = Logger.getLogger(Pet.class.getName());
	
	private static final String ADD_SKILL_SAVE = "INSERT INTO character_pet_skills_save (petObjItemId,skill_id,skill_level,skill_sub_level,remaining_time,buff_index) VALUES (?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT petObjItemId,skill_id,skill_level,skill_sub_level,remaining_time,buff_index FROM character_pet_skills_save WHERE petObjItemId=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_pet_skills_save WHERE petObjItemId=?";
	
	private static final int SIN_EATER_ID = 12564;
	
	int _curFed;
	final PetInventory _inventory;
	private final int _controlObjectId;
	private boolean _respawned;
	private final boolean _mountable;
	private Future<?> _feedTask;
	private PetDataHolder _data;
	private PetLevelData _leveldata;
	
	/** The Experience before the last Death Penalty */
	private long _expBeforeDeath = 0;
	private int _curWeightPenalty = 0;
	
	/**
	 * Retrieves the level data for this pet.<br>
	 * It fetches the data from {@code PetData} if it is not already loaded.
	 * @return the {@code PetLevelData} associated with the current pet level.
	 */
	public PetLevelData getPetLevelData()
	{
		if (_leveldata == null)
		{
			_leveldata = PetData.getInstance().getPetLevelData(getTemplate().getId(), getStat().getLevel());
		}
		
		return _leveldata;
	}
	
	/**
	 * Retrieves the data holder for this pet.<br>
	 * It fetches the data from {@code PetData} if it is not already loaded.
	 * @return the {@code PetDataHolder} associated with this pet.
	 */
	public PetDataHolder getPetData()
	{
		if (_data == null)
		{
			_data = PetData.getInstance().getPetData(getTemplate().getId());
		}
		
		return _data;
	}
	
	/**
	 * Updates the level data for this pet.<br>
	 * This method sets the internal {@code _leveldata} field to the provided value.
	 * @param value The new {@code PetLevelData} to assign to the pet.
	 */
	public void setPetData(PetLevelData value)
	{
		_leveldata = value;
	}
	
	/**
	 * Manage Feeding Task.<br>
	 * Feed or kill the pet depending on hunger level.<br>
	 * If pet has food in inventory and feed level drops below 55% then consume food from inventory.<br>
	 * Send a broadcastStatusUpdate packet for this Pet
	 */
	class FeedTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				final Summon pet = getOwner().getPet();
				if ((getOwner() == null) || (pet == null) || (pet.getObjectId() != getObjectId()))
				{
					stopFeed();
					return;
				}
				else if (_curFed > getFeedConsume())
				{
					setCurrentFed(_curFed - getFeedConsume());
				}
				else
				{
					setCurrentFed(0);
				}
				
				broadcastStatusUpdate();
				
				if (isUncontrollable())
				{
					if (Rnd.get(100) < 30)
					{
						// Owl Monk remove PK.
						if ((getTemplate().getId() == 16050) && (getOwner() != null))
						{
							getOwner().setPkKills(Math.max(0, getOwner().getPkKills() - Rnd.get(1, 6)));
						}
						
						sendPacket(SystemMessageId.YOUR_PET_IS_TIRED_OF_STARVING_AND_HAS_LEFT_YOU);
						unSummon(getOwner());
						stopFeed();
						return;
					}
					
					sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
					return;
				}
				
				final Set<Integer> foodIds = getPetData().getFood();
				if (foodIds.isEmpty())
				{
					if (isHungry())
					{
						sendPacket(SystemMessageId.YOUR_PET_IS_VERY_HUNGRY_PLEASE_BE_CAREFUL);
					}
					return;
				}
				
				// Try to find food in pet inventory.
				Item food = null;
				for (int id : foodIds)
				{
					food = _inventory.getItemByItemId(id);
					if (food != null)
					{
						break;
					}
				}
				
				// Pet is hungry and food is available - auto feed.
				if ((food != null) && isHungry())
				{
					final IItemHandler handler = ItemHandler.getInstance().getHandler(food.getEtcItem());
					if (handler != null)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_GUARDIAN_WAS_HUNGRY_SO_IT_HAS_EATEN_S1);
						sm.addItemName(food.getId());
						sendPacket(sm);
						handler.onItemUse(Pet.this, food, false);
						sendPacket(new PetItemList(getInventory().getItems()));
					}
				}
				
				// Notify player if pet uncontrollable due to starvation.
				if (isUncontrollable())
				{
					sendPacket(SystemMessageId.YOUR_PET_IS_STARVING_AND_WILL_NOT_OBEY_UNTIL_IT_GETS_IT_S_FOOD_FEED_YOUR_PET);
				}
			}
			catch (Exception e)
			{
				LOGGER_PET.log(Level.SEVERE, "Pet [ObjectId: " + getObjectId() + "] a feed task error has occurred", e);
			}
		}
		
		private int getFeedConsume()
		{
			// If pet is attacking.
			if (isAttackingNow())
			{
				return getPetLevelData().getPetFeedBattle();
			}
			
			return getPetLevelData().getPetFeedNormal();
		}
	}
	
	/**
	 * Creates and spawns a new {@code Pet} instance for a specific player.<br>
	 * This method removes any existing pet from the owner before spawning the new one.<br>
	 * It handles level synchronization based on the provided {@code NpcTemplate}.
	 * @param template The {@code NpcTemplate} defining the pet's properties.
	 * @param owner The {@code Player} who will own the spawned pet.
	 * @param control The {@code Item} used to trigger the summoning process.
	 * @return The newly created {@code Pet} instance, or {@code null} if spawning failed.
	 */
	public static synchronized Pet spawnPet(NpcTemplate template, Player owner, Item control)
	{
		final int objectId = owner.getObjectId();
		final Pet existingPet = World.getPet(objectId);
		if (existingPet != null) // Owner has a pet listed in world.
		{
			existingPet.unSummon(owner);
		}
		
		final PetDataHolder data = PetData.getInstance().getPetData(template.getId());
		final Pet pet = restore(control, template, owner);
		
		// Add the pet instance to world.
		if (pet != null)
		{
			pet.setTitle(owner.getName());
			if (data.isSynchLevel() && (pet.getLevel() != owner.getLevel()))
			{
				final int availableLevel = Math.min(data.getMaxLevel(), owner.getLevel());
				pet.getStat().setLevel(availableLevel);
				pet.getStat().setExp(pet.getStat().getExpForLevel(availableLevel));
			}
			
			World.addPet(objectId, pet);
		}
		
		return pet;
	}
	
	/**
	 * Creates a new {@code Pet} instance using the provided template and owner.<br>
	 * This constructor automatically determines the pet level based on the template ID.
	 * @param template The {@code NpcTemplate} used to define the pet's properties.
	 * @param owner The {@code Player} who owns this pet.
	 * @param control The {@code Item} used to control the pet.
	 */
	public Pet(NpcTemplate template, Player owner, Item control)
	{
		this(template, owner, control, template.getDisplayId() == SIN_EATER_ID ? owner.getLevel() : template.getLevel());
	}
	
	/**
	 * Creates a new {@code Pet} instance with specific attributes.<br>
	 * This constructor initializes the pet's stats, inventory, and mountability.
	 * @param template The {@code NpcTemplate} used to define the pet type.
	 * @param owner The {@code Player} who owns this pet.
	 * @param control The {@code Item} used to control the pet.
	 * @param level The initial level of the pet.
	 */
	public Pet(NpcTemplate template, Player owner, Item control, int level)
	{
		super(template, owner);
		setInstanceType(InstanceType.Pet);
		
		_controlObjectId = control.getObjectId();
		getStat().setLevel(Math.max(level, PetData.getInstance().getPetMinLevel(template.getId())));
		_inventory = new PetInventory(this);
		_inventory.restore();
		
		final int npcId = template.getId();
		_mountable = PetData.isMountable(npcId);
		getPetData();
		getPetLevelData();
	}
	
	/**
	 * Retrieves the current statistics for this pet.<br>
	 * This method calls the parent class to get the {@code PetStat} object.
	 * @return the {@code PetStat} of the pet.
	 */
	@Override
	public PetStat getStat()
	{
		return (PetStat) super.getStat();
	}
	
	/**
	 * Initializes the statistics for this pet.<br>
	 * This method creates a new {@code PetStat} object and assigns it to the current instance.
	 */
	@Override
	public void initCharStat()
	{
		setStat(new PetStat(this));
	}
	
	/**
	 * Checks if the pet has been respawned.
	 * @return {@code true} if the pet is currently in a respawned state, {@code false} otherwise.
	 */
	public boolean isRespawned()
	{
		return _respawned;
	}
	
	/**
	 * Retrieves the type of the summon.<br>
	 * This method returns a constant value representing the summon category.
	 * @return The integer value for the summon type.
	 */
	@Override
	public int getSummonType()
	{
		return 2;
	}
	
	/**
	 * Retrieves the unique identifier of the control object.<br>
	 * This ID is used to identify the item or entity controlling this pet.
	 * @return The {@code int} value of the control object ID.
	 */
	@Override
	public int getControlObjectId()
	{
		return _controlObjectId;
	}
	
	/**
	 * Retrieves the item used to control this pet.<br>
	 * It looks up the item in the owner's inventory using the internal object ID.
	 * @return the {@code Item} associated with the control action or {@code null}.
	 */
	public Item getControlItem()
	{
		return getOwner().getInventory().getItemByObjectId(_controlObjectId);
	}
	
	/**
	 * Retrieves the current fed amount.
	 * @return The current fed value as an {@code int}.
	 */
	public int getCurrentFed()
	{
		return _curFed;
	}
	
	/**
	 * Updates the current fed value for this pet.<br>
	 * This method checks if the new value is within the allowed maximum.<br>
	 * It also sends a packet to update the state if the value changes from zero.
	 * @param num The new fed value to set.
	 */
	public void setCurrentFed(int num)
	{
		if (num <= 0)
		{
			sendPacket(new ExChangeNpcState(getObjectId(), 0x64));
		}
		else if ((_curFed <= 0) && (num > 0))
		{
			sendPacket(new ExChangeNpcState(getObjectId(), 0x65));
		}
		
		_curFed = num > getMaxFed() ? getMaxFed() : num;
	}
	
	/**
	 * Retrieves the weapon currently equipped in the right hand.<br>
	 * It searches the {@code PetInventory} for an item located at {@code ItemLocation.PET_EQUIP}.<br>
	 * The item must have a body part of {@code BodyPart.R_HAND} or {@code BodyPart.LR_HAND}.
	 * @return the {@code Item} instance if found, or {@code null} otherwise.
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		if (_inventory != null)
		{
			for (Item item : _inventory.getItems())
			{
				if (item.getItemLocation() == ItemLocation.PET_EQUIP)
				{
					final BodyPart bodyPart = item.getTemplate().getBodyPart();
					if ((bodyPart == BodyPart.R_HAND) || (bodyPart == BodyPart.LR_HAND))
					{
						return item;
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the template of the currently active weapon.<br>
	 * This method returns {@code null} if no weapon is equipped.
	 * @return the {@code Weapon} template of the active weapon or {@code null}.
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		final Item weapon = getActiveWeaponInstance();
		if (weapon == null)
		{
			return null;
		}
		
		return (Weapon) weapon.getTemplate();
	}
	
	/**
	 * Retrieves the secondary weapon instance for this entity.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Item} representing the secondary weapon, or {@code null}.
	 */
	@Override
	public Item getSecondaryWeaponInstance()
	{
		// temporary? unavailable
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon item for the entity.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Weapon} object representing the secondary weapon.
	 */
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		// temporary? unavailable
		return null;
	}
	
	/**
	 * Retrieves the inventory associated with this pet.
	 * @return the {@code PetInventory} object.
	 */
	@Override
	public PetInventory getInventory()
	{
		return _inventory;
	}
	
	/**
	 * Removes a specific quantity of an item from the pet inventory.<br>
	 * This method handles the destruction logic and sends updates to the client if requested.
	 * @param process The type of item process being performed.
	 * @param objectId The unique identifier of the item instance.
	 * @param count The amount of items to remove.
	 * @param reference The {@code WorldObject} associated with this action.
	 * @param sendMessage Whether to send a confirmation message to the player.
	 * @return {@code true} if the item was successfully destroyed, {@code false} otherwise.
	 */
	@Override
	public boolean destroyItem(ItemProcessType process, int objectId, long count, WorldObject reference, boolean sendMessage)
	{
		final Item item = _inventory.destroyItem(process, objectId, count, getOwner(), reference);
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		// Send Pet inventory update packet.
		final PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		sendPacket(petIU);
		
		if (sendMessage)
		{
			if (count > 1)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_X_S2_DISAPPEARED);
				sm.addItemName(item.getId());
				sm.addLong(count);
				sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(item.getId());
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Removes a specific quantity of an item from the inventory based on its unique ID.<br>
	 * This method handles both the removal logic and sending necessary update packets to the client.
	 * @param process The type of process used for destroying the item.
	 * @param itemId The unique identifier of the item to be removed.
	 * @param count The amount of the item to remove.
	 * @param reference The {@code WorldObject} associated with this action.
	 * @param sendMessage Whether to send a confirmation or error message to the player.
	 * @return {@code true} if the item was successfully removed, {@code false} otherwise.
	 */
	@Override
	public boolean destroyItemByItemId(ItemProcessType process, int itemId, long count, WorldObject reference, boolean sendMessage)
	{
		final Item item = _inventory.destroyItemByItemId(process, itemId, count, getOwner(), reference);
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		// Send Pet inventory update packet.
		final PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		sendPacket(petIU);
		
		if (sendMessage)
		{
			if (count > 1)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_X_S2_DISAPPEARED);
				sm.addItemName(item.getId());
				sm.addLong(count);
				sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(item.getId());
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Attempts to pick up a {@code WorldObject} from the ground.<br>
	 * This method validates if the object is an item and checks for various restrictions like cursed status or inventory capacity.<br>
	 * If successful, the item is added to the pet's inventory or distributed to the party.
	 * @param object The {@code WorldObject} that the pet will attempt to pick up.
	 */
	@Override
	public void doPickupItem(WorldObject object)
	{
		if (isDead())
		{
			return;
		}
		
		getAI().setIntentionIdle();
		broadcastPacket(new StopMove(this));
		if (!object.isItem())
		{
			// Do not try to pickup anything that is not an item :)
			LOGGER_PET.warning(this + " trying to pickup wrong target." + object);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Item target = (Item) object;
		final int targetId = target.getId();
		
		// Cursed weapons
		if (CursedWeaponsManager.getInstance().isCursed(targetId))
		{
			final SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
			smsg.addItemName(targetId);
			sendPacket(smsg);
			return;
		}
		else if (FortSiegeManager.getInstance().isCombat(targetId))
		{
			return;
		}
		
		final boolean follow = getFollowStatus();
		SystemMessage smsg = null;
		synchronized (target)
		{
			// Check if the target to pick up is visible.
			if (!target.isSpawned())
			{
				// Send a Server->Client packet ActionFailed to this Player.
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!target.getDropProtection().tryPickUp(this))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}
			
			if (((isInParty() && (getParty().getDistributionType() == PartyDistributionType.FINDERS_KEEPERS)) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOUR_PET_S_INVENTORY_IS_FULL_REMOVE_SOMETHING_FROM_IT_AND_TRY_AGAIN);
				return;
			}
			
			final int ownerId = target.getOwnerId();
			final int objectId = getOwner().getObjectId();
			if ((ownerId != 0) && (ownerId != objectId) && !getOwner().isInLooterParty(ownerId))
			{
				if (targetId == Inventory.ADENA_ID)
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1_ADENA);
					smsg.addLong(target.getCount());
				}
				else if (target.getCount() > 1)
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S2_S1_S);
					smsg.addItemName(target);
					smsg.addLong(target.getCount());
				}
				else
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
					smsg.addItemName(target);
				}
				
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(smsg);
				return;
			}
			
			if ((target.getItemLootShedule() != null) && ((ownerId == objectId) || getOwner().isInLooterParty(ownerId)))
			{
				target.resetOwnerTimer();
			}
			
			// Remove from the ground!
			target.pickupMe(this);
			
			if (GeneralConfig.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}
		}
		
		// Herbs
		if (target.getTemplate().hasExImmediateEffect())
		{
			final IItemHandler handler = ItemHandler.getInstance().getHandler(target.getEtcItem());
			if (handler == null)
			{
				LOGGER.warning("No item handler registered for item ID: " + targetId + ".");
			}
			else
			{
				handler.onItemUse(this, target, false);
			}
			
			ItemManager.destroyItem(ItemProcessType.NONE, target, getOwner(), null);
			broadcastStatusUpdate();
		}
		else
		{
			if (targetId == Inventory.ADENA_ID)
			{
				smsg = new SystemMessage(SystemMessageId.YOUR_PET_HAS_PICKED_UP_S1_ADENA);
				smsg.addLong(target.getCount());
				sendPacket(smsg);
			}
			else if (target.isEnchanted())
			{
				smsg = new SystemMessage(SystemMessageId.YOUR_PET_HAS_PICKED_UP_S1_S2);
				smsg.addInt(target.getEnchantLevel());
				smsg.addItemName(target);
				sendPacket(smsg);
			}
			else if (target.getCount() > 1)
			{
				smsg = new SystemMessage(SystemMessageId.YOUR_PET_HAS_PICKED_UP_S1_X_S2);
				smsg.addLong(target.getCount());
				smsg.addItemName(target);
				sendPacket(smsg);
			}
			else
			{
				smsg = new SystemMessage(SystemMessageId.YOUR_PET_HAS_PICKED_UP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
			}
			
			// If owner is in party and it is not finders keepers, distribute the item instead of stealing it -.-
			if (getOwner().isInParty() && (getOwner().getParty().getDistributionType() != PartyDistributionType.FINDERS_KEEPERS))
			{
				getOwner().getParty().distributeItem(getOwner(), target);
			}
			else
			{
				final Item item = _inventory.addItem(ItemProcessType.PICKUP, target, getOwner(), this);
				if (item != null)
				{
					// Picked up items are stored at once, so the pet keeps them through a server crash.
					item.updateDatabase(true);
					
					getOwner().sendPacket(new PetItemList(getInventory().getItems()));
				}
			}
		}
		
		getAI().setIntentionIdle();
		
		if (follow)
		{
			followOwner();
		}
	}
	
	/**
	 * Removes this pet from the game world and database.<br>
	 * Transfers all items to the owner before deletion.<br>
	 * Cleans up associated effects and summon information.
	 * @param owner The {@code Player} who owns this pet.
	 */
	@Override
	public void deleteMe(Player owner)
	{
		_inventory.transferItemsToOwner();
		super.deleteMe(owner);
		destroyControlItem(owner, false); // this should also delete the pet from the db
		SummonInfoTable.getInstance().getPets().remove(getOwner().getObjectId());
		
		// Release the per-pet effect cache so it does not accumulate forever after pet removal.
		PetEffectTable.getInstance().removePetEffects(_controlObjectId);
	}
	
	/**
	 * Handles the logic for when this pet dies.<br>
	 * It applies death penalties and notifies the owner.<br>
	 * The body is added to the decay task manager.
	 * @param killer The {@code Creature} that killed this pet.
	 * @return {@code true} if the death was successful, {@code false} otherwise.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		final Player owner = getOwner();
		if ((owner != null) && !owner.isInDuel() && (!isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE)))
		{
			deathPenalty();
		}
		
		if (!super.doDie(killer, true))
		{
			return false;
		}
		
		stopFeed();
		sendPacket(SystemMessageId.THE_PET_HAS_DIED_IF_YOU_DO_NOT_RESURRECT_IT_WITHIN_24_H_THE_PET_S_BODY_AND_ITS_SUMMONING_ITEM_WILL_DISAPPEAR);
		DecayTaskManager.getInstance().add(this);
		
		// Do not decrease exp if is in duel, arena.
		return true;
	}
	
	/**
	 * Revives the pet and restores its active status.<br>
	 * This method removes the reviving state from the owner.<br>
	 * It cancels any decay tasks and starts the feeding process.<br>
	 * If the pet is not hungry, it sets the pet to a running state.<br>
	 * Finally, it activates the AI intentions.
	 */
	@Override
	public void doRevive()
	{
		getOwner().removeReviving();
		
		super.doRevive();
		
		// stopDecay
		DecayTaskManager.getInstance().cancel(this);
		startFeed();
		if (!isHungry())
		{
			setRunning();
		}
		
		getAI().setIntentionActive();
	}
	
	/**
	 * Restores the pet and its lost experience.<br>
	 * The amount of experience restored depends on the {@code revivePower}.
	 * @param revivePower The power value used to calculate the restoration amount.
	 */
	@Override
	public void doRevive(double revivePower)
	{
		// Restore the pet's lost experience,.
		// depending on the % return of the skill used (based on its power).
		restoreExp(revivePower);
		doRevive();
	}
	
	/**
	 * Transfers an item from the current inventory to a target inventory.<br>
	 * This method handles the logic for moving items and sends necessary update packets.
	 * @param process The type of item process being performed.
	 * @param objectId The unique identifier of the item to move.
	 * @param count The amount of the item to transfer.
	 * @param target The inventory where the item will be moved to.
	 * @param actor The player performing the action.
	 * @param reference The world object associated with this action.
	 * @return The resulting {@code Item} after the transfer, or {@code null} if the transfer failed.
	 */
	public Item transferItem(ItemProcessType process, int objectId, long count, Inventory target, Player actor, WorldObject reference)
	{
		final Item oldItem = _inventory.getItemByObjectId(objectId);
		final Item playerOldItem = target.getItemByItemId(oldItem.getId());
		final Item newItem = _inventory.transferItem(process, objectId, count, target, actor, reference);
		if (newItem == null)
		{
			return null;
		}
		
		// Send inventory update packet.
		final PetInventoryUpdate petIU = new PetInventoryUpdate();
		if ((oldItem.getCount() > 0) && (oldItem != newItem))
		{
			petIU.addModifiedItem(oldItem);
		}
		else
		{
			petIU.addRemovedItem(oldItem);
		}
		
		sendPacket(petIU);
		
		// Send target update packet.
		if ((playerOldItem != null) && newItem.isStackable())
		{
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(newItem);
			sendInventoryUpdate(iu);
		}
		
		return newItem;
	}
	
	/**
	 * Removes the pet from the world and deletes its control item.<br>
	 * This method handles inventory updates based on whether the pet is evolving.<br>
	 * It also removes the pet record from the database.
	 * @param owner The {@code Player} who owns the pet.
	 * @param evolve A boolean indicating if the pet is currently evolving.
	 */
	public void destroyControlItem(Player owner, boolean evolve)
	{
		// Remove the pet instance from world.
		World.removePet(owner.getObjectId());
		
		// Delete from inventory.
		try
		{
			Item removedItem;
			if (evolve)
			{
				removedItem = owner.getInventory().destroyItem(ItemProcessType.FEE, _controlObjectId, 1, getOwner(), this);
			}
			else
			{
				removedItem = owner.getInventory().destroyItem(ItemProcessType.DESTROY, _controlObjectId, 1, getOwner(), this);
				if (removedItem != null)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
					sm.addItemName(removedItem);
					owner.sendPacket(sm);
				}
			}
			
			if (removedItem == null)
			{
				LOGGER.warning("Couldn't destroy pet control item for " + owner + " pet: " + this + " evolve: " + evolve);
			}
			else
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addRemovedItem(removedItem);
				
				owner.sendInventoryUpdate(iu);
				owner.broadcastUserInfo();
			}
		}
		catch (Exception e)
		{
			LOGGER_PET.log(Level.WARNING, "Error while destroying control item: " + e.getMessage(), e);
		}
		
		// Pet control item no longer exists, delete the pet from the db.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id = ?"))
		{
			statement.setInt(1, _controlObjectId);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER_PET.log(Level.SEVERE, "Failed to delete Pet [ObjectId: " + getObjectId() + "]", e);
		}
	}
	
	/**
	 * Removes all items from the pet inventory.<br>
	 * Each item is dropped at the current location of the pet.<br>
	 * Any errors during the dropping process are logged as warnings.
	 */
	public void dropAllItems()
	{
		try
		{
			for (Item item : _inventory.getItems())
			{
				dropItemHere(item);
			}
		}
		catch (Exception e)
		{
			LOGGER_PET.log(Level.WARNING, "Pet Drop Error: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Drops a specific item from the inventory.<br>
	 * The item is placed on the ground at the current location.<br>
	 * If {@code protect} is {@code true}, it applies drop protection to the owner.
	 * @param item The {@code Item} object to be dropped.
	 * @param protect Whether to apply drop protection to the owner.
	 */
	public void dropItemHere(Item item, boolean protect)
	{
		final Item dropit = _inventory.dropItem(ItemProcessType.DROP, item.getObjectId(), item.getCount(), getOwner(), this);
		if (dropit != null)
		{
			if (protect)
			{
				dropit.getDropProtection().protect(getOwner());
			}
			
			LOGGER_PET.finer("Item id to drop: " + dropit.getId() + " amount: " + dropit.getCount());
			dropit.dropMe(this, getX(), getY(), getZ() + 100);
		}
	}
	
	/**
	 * Drops an item at the current location.<br>
	 * This method handles the placement of a {@code Item} in the world.
	 * @param dropit The {@code Item} to be dropped.
	 */
	public void dropItemHere(Item dropit)
	{
		dropItemHere(dropit, false);
	}
	
	/**
	 * Checks if this entity can be mounted by a player.
	 * @return {@code true} if the entity is mountable, {@code false} otherwise.
	 */
	@Override
	public boolean isMountable()
	{
		return _mountable;
	}
	
	/**
	 * Restores a {@code Pet} instance from the database using its unique identifier.<br>
	 * If no data exists, it creates a new pet based on the provided template.<br>
	 * It handles loading stats like experience, health, and current feeding status.
	 * @param control The {@code Item} used to identify the specific pet in the database.
	 * @param template The {@code NpcTemplate} defining the base pet type.
	 * @param owner The {@code Player} who owns this pet.
	 * @return The restored {@code Pet} object, or a new one if no data was found.
	 */
	private static Pet restore(Item control, NpcTemplate template, Player owner)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT item_obj_id, name, level, curHp, curMp, exp, sp, fed FROM pets WHERE item_obj_id=?"))
		{
			Pet pet;
			statement.setInt(1, control.getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				if (!rset.next())
				{
					return new Pet(template, owner, control);
				}
				
				pet = new Pet(template, owner, control, rset.getInt("level"));
				pet._respawned = true;
				pet.setName(rset.getString("name"));
				
				long exp = rset.getLong("exp");
				final PetLevelData info = PetData.getInstance().getPetLevelData(pet.getId(), pet.getLevel());
				
				// DS: update experience based by level.
				// Avoiding pet delevels due to exp per level values changed.
				if ((info != null) && (exp < info.getPetMaxExp()))
				{
					exp = info.getPetMaxExp();
				}
				
				pet.getStat().setExp(exp);
				pet.getStat().setSp(rset.getInt("sp"));
				
				pet.getStatus().setCurrentHp(rset.getInt("curHp"));
				pet.getStatus().setCurrentMp(rset.getInt("curMp"));
				pet.getStatus().setCurrentCp(pet.getMaxCp());
				if (rset.getDouble("curHp") < 1)
				{
					pet.setDead(true);
					pet.stopHpMpRegeneration();
				}
				
				pet.setCurrentFed(rset.getInt("fed"));
			}
			
			return pet;
		}
		catch (Exception e)
		{
			LOGGER_PET.log(Level.WARNING, "Could not restore pet data for owner: " + owner + " - " + e.getMessage(), e);
		}
		
		return null;
	}
	
	/**
	 * Sets whether the summon should be restored.<br>
	 * This updates the {@code _restoreSummon} field.
	 * @param value The boolean value to set for restoration.
	 */
	@Override
	public void setRestoreSummon(boolean value)
	{
		_restoreSummon = value;
	}
	
	/**
	 * Stops the active effects of a specific skill for this pet.<br>
	 * This method removes the effect from the {@code PetEffectTable}.
	 * @param type The category of the skill finish type.
	 * @param skillId The unique identifier of the skill to stop.
	 */
	@Override
	public void stopSkillEffects(SkillFinishType type, int skillId)
	{
		super.stopSkillEffects(type, skillId);
		final Collection<PetEffect> effects = PetEffectTable.getInstance().getPetEffectsFor(_controlObjectId);
		if ((effects == null) || effects.isEmpty())
		{
			return;
		}
		
		for (PetEffect effect : effects)
		{
			if (effect.getSkill().getId() == skillId)
			{
				effects.remove(effect);
			}
		}
	}
	
	/**
	 * Saves the current pet data to the database.<br>
	 * This method handles both new pet creation and updates for existing pets.<br>
	 * It also synchronizes the enchantment level of the control item with the pet level.
	 */
	@Override
	public void storeMe()
	{
		if (_controlObjectId == 0)
		{
			// This is a summon, not a pet, don't store anything.
			return;
		}
		
		if (!PlayerConfig.RESTORE_PET_ON_RECONNECT)
		{
			_restoreSummon = false;
		}
		
		String req;
		if (!_respawned)
		{
			req = "INSERT INTO pets (name,level,curHp,curMp,exp,sp,fed,ownerId,restore,item_obj_id) VALUES (?,?,?,?,?,?,?,?,?,?)";
		}
		else
		{
			req = "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,fed=?,ownerId=?,restore=? WHERE item_obj_id = ?";
		}
		
		final int level = getStat().getLevel();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(req))
		{
			final String name = getName();
			final long exp = getStat().getExp();
			final long sp = getStat().getSp();
			statement.setString(1, name);
			statement.setInt(2, level);
			statement.setDouble(3, getStatus().getCurrentHp());
			statement.setDouble(4, getStatus().getCurrentMp());
			statement.setLong(5, exp);
			statement.setLong(6, sp);
			statement.setInt(7, _curFed);
			final int objectId = getOwner().getObjectId();
			statement.setInt(8, objectId);
			statement.setString(9, String.valueOf(_restoreSummon)); // True restores pet on login.
			statement.setInt(10, _controlObjectId);
			statement.executeUpdate();
			
			_respawned = true;
			if (_restoreSummon)
			{
				SummonInfoTable.getInstance().getPets().put(objectId, getControlObjectId());
			}
			else
			{
				SummonInfoTable.getInstance().getPets().remove(objectId);
			}
		}
		catch (Exception e)
		{
			LOGGER_PET.log(Level.SEVERE, "Failed to store Pet [ObjectId: " + getObjectId() + "] data", e);
		}
		
		final Item itemInst = getControlItem();
		if ((itemInst != null) && (itemInst.getEnchantLevel() != level))
		{
			itemInst.setEnchantLevel(level);
			itemInst.updateDatabase();
		}
	}
	
	/**
	 * Saves the current effects of a summon to the database.<br>
	 * This method clears existing records before adding new ones to prevent duplicates.<br>
	 * It filters out skills based on specific configuration rules and types.
	 * @param storeEffects Determines whether the effect data should be saved to the database.
	 */
	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!PlayerConfig.SUMMON_STORE_SKILL_COOLTIME)
		{
			return;
		}
		
		final PetEffectTable petEffectTable = PetEffectTable.getInstance();
		
		// Clear cached effect list so the upcoming inserts do not duplicate state from a prior save.
		petEffectTable.clearPetEffects(_controlObjectId);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement(DELETE_SKILL_SAVE);
			PreparedStatement ps2 = con.prepareStatement(ADD_SKILL_SAVE))
		{
			// Delete all current stored effects for summon to avoid dupe.
			ps1.setInt(1, _controlObjectId);
			ps1.execute();
			
			int buffIndex = 0;
			
			final Set<Long> storedSkills = new HashSet<>();
			
			// Store all effect data along with calculated remaining.
			if (storeEffects)
			{
				for (BuffInfo info : getEffectList().getEffects())
				{
					if (info == null)
					{
						continue;
					}
					
					final Skill skill = info.getSkill();
					
					// Do not store those effects.
					if (skill.isDeleteAbnormalOnLeave() || !skill.isSharedWithSummon())
					{
						continue;
					}
					
					// Do not save heals.
					if (skill.getAbnormalType() == AbnormalType.LIFE_FORCE_OTHERS)
					{
						continue;
					}
					
					// Toggles are skipped, unless they are necessary to be always on.
					if (skill.isToggle() && !skill.isNecessaryToggle())
					{
						continue;
					}
					
					// Dances and songs are not kept in retail.
					if (skill.isDance() && !PlayerConfig.ALT_STORE_DANCES)
					{
						continue;
					}
					
					if (!storedSkills.add(skill.getReuseHashCode()))
					{
						continue;
					}
					
					ps2.setInt(1, _controlObjectId);
					ps2.setInt(2, skill.getId());
					ps2.setInt(3, skill.getLevel());
					ps2.setInt(4, skill.getSubLevel());
					final int infoTime = info.getTime();
					ps2.setInt(5, infoTime);
					ps2.setInt(6, ++buffIndex);
					ps2.addBatch();
					
					petEffectTable.addPetEffect(_controlObjectId, new PetEffect(skill, infoTime));
				}
				
				ps2.executeBatch();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store pet effect data: ", e);
		}
	}
	
	/**
	 * Restores active effects for the pet from the database.<br>
	 * This method checks if any saved skills exist for the {@code _controlObjectId}.<br>
	 * It then applies these effects to the current instance.
	 */
	@Override
	public void restoreEffects()
	{
		final PetEffectTable petEffectTable = PetEffectTable.getInstance();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement(RESTORE_SKILL_SAVE);
			PreparedStatement ps2 = con.prepareStatement(DELETE_SKILL_SAVE))
		{
			// Only hit the database when the cache has nothing for this pet.
			if (!petEffectTable.hasPetEffects(_controlObjectId))
			{
				ps1.setInt(1, _controlObjectId);
				try (ResultSet rset = ps1.executeQuery())
				{
					while (rset.next())
					{
						final Skill skill = SkillData.getInstance().getSkill(rset.getInt("skill_id"), rset.getInt("skill_level"), rset.getInt("skill_sub_level"));
						if (skill == null)
						{
							continue;
						}
						
						if (skill.hasEffects(EffectScope.GENERAL))
						{
							final int effectCurrentTime = rset.getInt("remaining_time");
							petEffectTable.addPetEffect(_controlObjectId, new PetEffect(skill, effectCurrentTime));
						}
					}
				}
			}
			
			ps2.setInt(1, _controlObjectId);
			ps2.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
		finally
		{
			final Collection<PetEffect> effects = petEffectTable.getPetEffectsFor(_controlObjectId);
			if (effects != null)
			{
				for (PetEffect se : effects)
				{
					if (se != null)
					{
						se.getSkill().applyEffects(this, this, false, se.getEffectCurrentTime());
					}
				}
			}
		}
	}
	
	/**
	 * Stops the active feeding task for this pet.<br>
	 * This method cancels the {@code _feedTask} if it is currently running.<br>
	 * It sets the {@code _feedTask} reference to {@code null}.
	 */
	public synchronized void stopFeed()
	{
		if (_feedTask != null)
		{
			_feedTask.cancel(false);
			_feedTask = null;
		}
	}
	
	/**
	 * Starts the periodic feeding task for the pet.<br>
	 * This method stops any existing feed task before starting a new one.<br>
	 * It only schedules the task if the pet is alive and belongs to its owner.
	 */
	public synchronized void startFeed()
	{
		// Stop feeding task if it is active.
		stopFeed();
		if (!isDead() && (getOwner().getPet() == this))
		{
			_feedTask = ThreadPool.scheduleAtFixedRate(new FeedTask(), 10000, 10000);
		}
	}
	
	/**
	 * Removes the summon from the game world.<br>
	 * This method stops all active effects and cleans up resources.<br>
	 * It also removes the pet from the owner's list if it is still alive.
	 * @param owner The {@code Player} who owns this summon.
	 */
	@Override
	public synchronized void unSummon(Player owner)
	{
		stopFeed();
		stopHpMpRegeneration();
		super.unSummon(owner);
		
		// Pet effects were just persisted to the database by storeEffect during super.unSummon, so the in-memory cache can be released.
		PetEffectTable.getInstance().removePetEffects(_controlObjectId);
		
		if (!isDead())
		{
			if (_inventory != null)
			{
				_inventory.deleteMe();
			}
			
			World.removePet(owner.getObjectId());
		}
	}
	
	/**
	 * Restores a portion of the experience lost upon death.<br>
	 * This method calculates the difference between current and previous experience.<br>
	 * It then adds a percentage of that difference back to the pet's stats.
	 * @param restorePercent The percentage of lost experience to recover.
	 */
	public void restoreExp(double restorePercent)
	{
		if (_expBeforeDeath > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp(Math.round(((_expBeforeDeath - getStat().getExp()) * restorePercent) / 100));
			_expBeforeDeath = 0;
		}
	}
	
	/**
	 * Calculates and applies an experience penalty to the pet upon death.<br>
	 * The amount lost depends on the current level of the pet.<br>
	 * It updates the internal {@code _expBeforeDeath} value before modifying the stats.
	 */
	private void deathPenalty()
	{
		final int level = getStat().getLevel();
		// Pets lose 70% less experience than the base penalty.
		final double percentLost = ((-0.07 * level) + 6.5) * 0.3;
		
		// Calculate the Experience loss.
		final long lostExp = Math.round(((getStat().getExpForLevel(level + 1) - getStat().getExpForLevel(level)) * percentLost) / 100);
		
		// Get the Experience before applying penalty.
		_expBeforeDeath = getStat().getExp();
		
		// Set the new Experience value of the Pet.
		getStat().addExp(-lostExp);
	}
	
	/**
	 * Adds experience and skill points to the pet.<br>
	 * This method applies specific rates based on the pet type.
	 * @param addToExp The amount of experience to add.
	 * @param addToSp The amount of skill points to add.
	 */
	@Override
	public synchronized void addExpAndSp(double addToExp, double addToSp)
	{
		if (getId() == SIN_EATER_ID)
		{
			getStat().addExpAndSp(addToExp * RatesConfig.SINEATER_XP_RATE);
		}
		else
		{
			getStat().addExpAndSp(addToExp * RatesConfig.PET_XP_RATE);
		}
	}
	
	/**
	 * Retrieves the experience required to reach the current level.<br>
	 * This method checks if the pet has reached the maximum level defined in {@code ExperienceData}.<br>
	 * If the max level is reached, it returns 0.<br>
	 * Otherwise, it fetches the value from the pet's stats.
	 * @return The experience required for the current level as a {@code long}.
	 */
	@Override
	public long getExpForThisLevel()
	{
		final int level = getLevel();
		if (level >= ExperienceData.getInstance().getMaxPetLevel())
		{
			return 0;
		}
		
		return getStat().getExpForLevel(level);
	}
	
	/**
	 * Calculates the experience required to reach the next level.<br>
	 * It checks if the pet is already at the maximum possible level.<br>
	 * If it is not at max level, it retrieves the value from {@code PetStat}.
	 * @return The amount of experience needed for the next level or 0 if at max level.
	 */
	@Override
	public long getExpForNextLevel()
	{
		final int level = getLevel();
		if (level >= (ExperienceData.getInstance().getMaxPetLevel() - 1))
		{
			return 0;
		}
		
		return getStat().getExpForLevel(level + 1);
	}
	
	/**
	 * Retrieves the current level of the pet.<br>
	 * This method fetches the level from the {@code PetStat} object.
	 * @return The current level as an {@code int}.
	 */
	@Override
	public int getLevel()
	{
		return getStat().getLevel();
	}
	
	/**
	 * Retrieves the maximum amount of feed that can be given.<br>
	 * This value is obtained from the {@code PetStat} object.
	 * @return The maximum feed count as an {@code int}.
	 */
	public int getMaxFed()
	{
		return getStat().getMaxFeed();
	}
	
	/**
	 * Retrieves the critical hit value for this pet.<br>
	 * This method fetches the data from the current {@code PetStat}.
	 * @return The integer value of the critical hit.
	 */
	@Override
	public int getCriticalHit()
	{
		return getStat().getCriticalHit();
	}
	
	/**
	 * Retrieves the Magic Attack value of the pet.<br>
	 * This method fetches the value from the current {@code PetStat}.
	 * @return The magic attack value as an {@code int}.
	 */
	@Override
	public int getMAtk()
	{
		return getStat().getMAtk();
	}
	
	/**
	 * Retrieves the Magic Defense value of this pet.<br>
	 * This value is obtained from the {@code PetStat} object.
	 * @return The magic defense value as an {@code int}.
	 */
	@Override
	public int getMDef()
	{
		return getStat().getMDef();
	}
	
	/**
	 * Retrieves the current skill level for a specific skill.<br>
	 * This method checks if the skill is known by the pet first.<br>
	 * If the skill is unknown, it returns {@code 0}.<br>
	 * Otherwise, it calculates the level based on the pet's overall level.
	 * @param skillId The unique identifier of the skill to check.
	 * @return The calculated skill level as an {@code int}.
	 */
	@Override
	public int getSkillLevel(int skillId)
	{
		if (getKnownSkill(skillId) == null)
		{
			return 0;
		}
		
		final int level = getLevel();
		return level > 70 ? 7 + ((level - 70) / 5) : level / 10;
	}
	
	/**
	 * Updates the owner of the current pet.<br>
	 * This method changes the owner to the provided {@code Player}.<br>
	 * It also updates the pet's registration in the {@code World}.
	 * @param owner The new {@code Player} who will own this pet.
	 */
	public void updateRefOwner(Player owner)
	{
		final int oldOwnerId = getOwner().getObjectId();
		setOwner(owner);
		World.removePet(oldOwnerId);
		World.addPet(oldOwnerId, this);
	}
	
	/**
	 * Retrieves the maximum inventory size for a pet.<br>
	 * This value is pulled from the {@code NpcConfig}.
	 * @return the maximum number of items a pet can carry.
	 */
	public int getInventoryLimit()
	{
		return NpcConfig.INVENTORY_MAXIMUM_PET;
	}
	
	/**
	 * Updates the overloaded status and weight penalties of the pet.<br>
	 * This method checks the current load against the maximum capacity.<br>
	 * It applies specific skills based on the percentage of weight exceeded.<br>
	 * The calculation is skipped if the owner has diet mode enabled.
	 */
	public void refreshOverloaded()
	{
		if ((getInventory() == null) || (getOwner() == null))
		{
			return;
		}
		
		final int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			final long weightproc = (((getCurrentLoad() - getBonusWeightPenalty()) * 1000L) / maxLoad);
			int newWeightPenalty;
			if ((weightproc < 500) || getOwner().getDietMode())
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if (weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}
			
			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if (newWeightPenalty > 0)
				{
					addSkill(SkillData.getInstance().getSkill(4270, newWeightPenalty));
					setOverloaded(getCurrentLoad() >= maxLoad);
				}
				else
				{
					removeSkill(getKnownSkill(4270), true);
					setOverloaded(false);
				}
			}
		}
	}
	
	/**
	 * Updates the current status and sends a notification to all nearby players.<br>
	 * This method also refreshes overloaded data if the inventory and owner are valid.
	 * @param value The new status value to apply.
	 */
	@Override
	public void updateAndBroadcastStatus(int value)
	{
		if ((getInventory() != null) && (getOwner() != null))
		{
			refreshOverloaded();
		}
		
		super.updateAndBroadcastStatus(value);
	}
	
	/**
	 * Checks if the pet is currently hungry.<br>
	 * This method compares the current fed amount against the maximum feed limit.
	 * @return {@code true} if the pet needs food, {@code false} otherwise.
	 */
	@Override
	public boolean isHungry()
	{
		return _curFed < ((getPetData().getHungryLimit() / 100f) * getPetLevelData().getPetMaxFeed());
	}
	
	/**
	 * Checks if the pet is currently uncontrollable.<br>
	 * This occurs when the current fed value is less than or equal to {@code 0}.
	 * @return {@code true} if the pet cannot be controlled, {@code false} otherwise.
	 */
	public boolean isUncontrollable()
	{
		return _curFed <= 0;
	}
	
	/**
	 * Retrieves the ID of the item held in the right hand.<br>
	 * Returns {@code 0} if no item is equipped.
	 * @return The unique identifier of the weapon or {@code 0}.
	 */
	@Override
	public int getWeapon()
	{
		final Item weapon = _inventory.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (weapon != null)
		{
			return weapon.getId();
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the ID of the item equipped in the chest slot.<br>
	 * Returns {@code 0} if no item is equipped.
	 * @return The unique identifier of the chest item or {@code 0}.
	 */
	@Override
	public int getArmor()
	{
		final Item weapon = _inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if (weapon != null)
		{
			return weapon.getId();
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the ID of the item equipped in the neck slot.<br>
	 * Returns {@code 0} if no item is found in that slot.
	 * @return The unique identifier of the neck item or {@code 0}.
	 */
	public int getJewel()
	{
		final Item weapon = _inventory.getPaperdollItem(Inventory.PAPERDOLL_NECK);
		if (weapon != null)
		{
			return weapon.getId();
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the number of soul shots per hit for this pet.<br>
	 * This value is fetched from the {@code PetLevelData}.
	 * @return The amount of soul shots per hit as a {@code short}.
	 */
	@Override
	public short getSoulShotsPerHit()
	{
		return getPetLevelData().getPetSoulShot();
	}
	
	/**
	 * Retrieves the number of spirit shots per hit for this pet.<br>
	 * This value is fetched from the {@code PetLevelData}.
	 * @return The amount of spirit shots per hit as a {@code short}.
	 */
	@Override
	public short getSpiritShotsPerHit()
	{
		return getPetLevelData().getPetSpiritShot();
	}
	
	/**
	 * Sets the name of the pet.<br>
	 * This method also updates the custom type of the control item if it exists.
	 * @param name The new name to assign to the pet.
	 */
	@Override
	public void setName(String name)
	{
		final Item controlItem = getControlItem();
		if (controlItem != null)
		{
			if (controlItem.getCustomType2() == (name == null ? 1 : 0))
			{
				// Name not set yet.
				controlItem.setCustomType2(name != null ? 1 : 0);
				controlItem.updateDatabase();
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(controlItem);
				sendInventoryUpdate(iu);
			}
		}
		else
		{
			LOGGER.warning("Pet control item null, for pet: " + toString());
		}
		
		super.setName(name);
	}
	
	/**
	 * Checks if a specific item can be used as food.<br>
	 * This method verifies if the {@code itemId} exists in the allowed food list.
	 * @param itemId The unique identifier of the item to check.
	 * @return {@code true} if the item is valid food, {@code false} otherwise.
	 */
	public boolean canEatFoodId(int itemId)
	{
		return _data.getFood().contains(itemId);
	}
	
	/**
	 * Checks if the current entity is a pet.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the entity is a pet.
	 */
	@Override
	public boolean isPet()
	{
		return true;
	}
	
	/**
	 * Converts the current object into a {@code Pet} instance.<br>
	 * This method returns the current instance as a {@code Pet}.
	 * @return the current instance cast to {@code Pet}
	 */
	@Override
	public Pet asPet()
	{
		return this;
	}
	
	/**
	 * Retrieves the current running speed of the pet.<br>
	 * This value is adjusted based on whether the pet is currently uncontrollable.
	 * @return The calculated run speed as a {@code double}.
	 */
	@Override
	public double getRunSpeed()
	{
		return super.getRunSpeed() * (isUncontrollable() ? 0.5d : 1.0d);
	}
	
	/**
	 * Returns the current walking speed of the pet.<br>
	 * The speed is reduced by {@code 0.5} if the pet is uncontrollable.
	 * @return the calculated walk speed as a {@code double}.
	 */
	@Override
	public double getWalkSpeed()
	{
		return super.getWalkSpeed() * (isUncontrollable() ? 0.5d : 1.0d);
	}
	
	/**
	 * Gets the current movement speed multiplier for this entity.<br>
	 * This value is adjusted based on whether the pet is currently uncontrollable.
	 * @return The final movement speed multiplier as a {@code double}.
	 */
	@Override
	public double getMovementSpeedMultiplier()
	{
		return super.getMovementSpeedMultiplier() * (isUncontrollable() ? 0.5d : 1.0d);
	}
	
	/**
	 * Retrieves the current movement speed of the pet.<br>
	 * This value depends on whether the pet is running or walking.<br>
	 * It also accounts for different speeds when in water zones.
	 * @return The current movement speed as a {@code double}.
	 */
	@Override
	public double getMoveSpeed()
	{
		if (isInsideZone(ZoneId.WATER))
		{
			return isRunning() ? getSwimRunSpeed() : getSwimWalkSpeed();
		}
		
		return isRunning() ? getRunSpeed() : getWalkSpeed();
	}
}
