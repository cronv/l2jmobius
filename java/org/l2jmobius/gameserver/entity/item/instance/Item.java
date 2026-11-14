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
package org.l2jmobius.gameserver.entity.item.instance;

import static org.l2jmobius.gameserver.entity.itemcontainer.Inventory.ADENA_ID;
import static org.l2jmobius.gameserver.entity.itemcontainer.Inventory.MAX_ADENA;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.TransmogConfig;
import org.l2jmobius.gameserver.data.holders.ArmorSet;
import org.l2jmobius.gameserver.data.holders.PreparedMultisellListHolder;
import org.l2jmobius.gameserver.data.xml.AgathionData;
import org.l2jmobius.gameserver.data.xml.AppearanceItemData;
import org.l2jmobius.gameserver.data.xml.ArmorSetData;
import org.l2jmobius.gameserver.data.xml.EnchantItemOptionsData;
import org.l2jmobius.gameserver.data.xml.EnsoulData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.request.AutoPeelRequest;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.appearance.AppearanceStone;
import org.l2jmobius.gameserver.entity.item.appearance.AppearanceType;
import org.l2jmobius.gameserver.entity.item.enchant.attribute.AttributeHolder;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.entity.item.holders.AgathionSkillHolder;
import org.l2jmobius.gameserver.entity.item.holders.ArmorsetSkillHolder;
import org.l2jmobius.gameserver.entity.item.holders.DropProtection;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.managers.SiegeGuardManager;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerAugment;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerItemDrop;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerItemPickup;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemAttributeAdd;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemBypassEvent;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemEnchantAdd;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemTalk;
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;
import org.l2jmobius.gameserver.mechanics.options.EnchantOptions;
import org.l2jmobius.gameserver.mechanics.options.Options;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillConditionScope;
import org.l2jmobius.gameserver.mechanics.variables.ItemVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.DropItem;
import org.l2jmobius.gameserver.network.serverpackets.GetItem;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.network.serverpackets.SpawnItem;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.ItemAppearanceTaskManager;
import org.l2jmobius.gameserver.taskmanagers.ItemLifeTimeTaskManager;
import org.l2jmobius.gameserver.taskmanagers.ItemManaTaskManager;
import org.l2jmobius.gameserver.util.GMAudit;

/**
 * This class represents an individual instance of an item within the game world.<br>
 * It manages all properties, behaviors, and state changes for items held by players or dropped on the ground.
 */
public class Item extends WorldObject
{
	private static final Logger LOGGER = Logger.getLogger(Item.class.getName());
	private static final Logger LOG_ITEMS = Logger.getLogger("item");
	
	/** Owner */
	private int _ownerId;
	private Player _owner;
	
	/** ID of who dropped the item last, used for knownlist */
	private int _dropperObjectId = 0;
	
	/** Quantity of the item */
	private long _count = 1;
	/** Initial Quantity of the item */
	private long _initCount;
	/** Remaining time (in miliseconds) */
	private long _time;
	/** Quantity of the item can decrease */
	private boolean _decrease = false;
	
	/** ID of the item */
	private final int _itemId;
	
	/** ItemTemplate associated to the item */
	private final ItemTemplate _itemTemplate;
	
	/** Location of the item : Inventory, PaperDoll, WareHouse */
	private ItemLocation _loc;
	
	/** Slot where item is stored : Paperdoll slot, inventory order ... */
	private int _locData;
	
	/** Level of enchantment of the item */
	private int _enchantLevel;
	
	/** Wear Item */
	private boolean _wear;
	
	/** Augmented Item */
	private AugmentationInstance _augmentation = null;
	
	/** Shadow item */
	private int _mana = -1;
	private boolean _consumingMana = false;
	
	/** Custom item types (used loto, race tickets) */
	private int _type1;
	private int _type2;
	
	private long _dropTime;
	
	private boolean _published = false;
	
	private boolean _protected;
	
	public static final int UNCHANGED = 0;
	public static final int ADDED = 1;
	public static final int REMOVED = 3;
	public static final int MODIFIED = 2;
	
	// @formatter:off
	public static final int[] DEFAULT_ENCHANT_OPTIONS = { 0, 0, 0 };
	// @formatter:on
	
	private int _lastChange = 2; // 1 ??, 2 modified, 3 removed
	private boolean _existsInDb; // If a record exists in DB.
	private boolean _storedInDb; // If DB data is up-to-date.
	
	private final ReentrantLock _dbLock = new ReentrantLock();
	
	private Map<AttributeType, AttributeHolder> _elementals = null;
	
	private ScheduledFuture<?> _itemLootShedule = null;
	
	private final DropProtection _dropProtection = new DropProtection();
	
	private final List<Options> _enchantOptions = new ArrayList<>();
	private final EnsoulOption[] _ensoulOptions = new EnsoulOption[2];
	private final EnsoulOption[] _ensoulSpecialOptions = new EnsoulOption[1];
	
	private boolean _isVirtual = false;
	private boolean _damaged = false;
	
	/**
	 * Creates a new {@code Item} instance using specific IDs.<br>
	 * This constructor initializes the item properties from the template data.<br>
	 * It validates that the provided {@code itemId} is valid.
	 * @param objectId The unique identifier for the object in the world.
	 * @param itemId The unique identifier for the item type.
	 */
	public Item(int objectId, int itemId)
	{
		super(objectId);
		setInstanceType(InstanceType.Item);
		_itemId = itemId;
		_itemTemplate = ItemData.getInstance().getTemplate(itemId);
		if ((_itemId == 0) || (_itemTemplate == null))
		{
			throw new IllegalArgumentException();
		}
		
		super.setName(_itemTemplate.getName());
		_loc = ItemLocation.VOID;
		_type1 = 0;
		_type2 = 0;
		_dropTime = 0;
		_mana = _itemTemplate.getDuration();
		final long time = _itemTemplate.getTime();
		_time = time == -1 ? -1 : System.currentTimeMillis() + (time * 60 * 1000);
		scheduleLifeTimeTask();
		scheduleVisualLifeTime();
	}
	
	/**
	 * Creates a new instance of an {@code Item}.<br>
	 * This constructor initializes the item using a specific template.<br>
	 * It sets the name, location, and lifetime based on the provided data.
	 * @param objectId The unique identifier for this specific object.
	 * @param itemTemplate The {@code ItemTemplate} containing the base properties of the item.
	 */
	public Item(int objectId, ItemTemplate itemTemplate)
	{
		super(objectId);
		setInstanceType(InstanceType.Item);
		_itemId = itemTemplate.getId();
		_itemTemplate = itemTemplate;
		if (_itemId == 0)
		{
			throw new IllegalArgumentException();
		}
		
		super.setName(_itemTemplate.getName());
		_loc = ItemLocation.VOID;
		_mana = _itemTemplate.getDuration();
		final long time = _itemTemplate.getTime();
		_time = time == -1 ? -1 : System.currentTimeMillis() + (time * 60 * 1000);
		scheduleLifeTimeTask();
		scheduleVisualLifeTime();
	}
	
	/**
	 * Creates a new {@code Item} instance from a database result set.<br>
	 * This constructor populates all fields including count, owner ID, and location data.<br>
	 * It also initializes life time tasks and restores attributes if the item is equipable.
	 * @param rs The {@code ResultSet} containing the item data.
	 * @throws SQLException If a database access error occurs.
	 */
	public Item(ResultSet rs) throws SQLException
	{
		this(rs.getInt("object_id"), ItemData.getInstance().getTemplate(rs.getInt("item_id")));
		_count = rs.getLong("count");
		_ownerId = rs.getInt("owner_id");
		_loc = ItemLocation.valueOf(rs.getString("loc"));
		_locData = rs.getInt("loc_data");
		_enchantLevel = rs.getInt("enchant_level");
		_type1 = rs.getInt("custom_type1");
		_type2 = rs.getInt("custom_type2");
		_mana = rs.getInt("mana_left");
		_time = rs.getLong("time");
		scheduleLifeTimeTask();
		scheduleVisualLifeTime();
		
		_existsInDb = true;
		_storedInDb = true;
		
		if (isEquipable())
		{
			restoreAttributes();
			restoreSpecialAbilities();
		}
		
		_isVirtual = getVariables().getBoolean(ItemVariables.VIRTUAL, false);
		_damaged = getVariables().getBoolean(ItemVariables.DAMAGED, false);
	}
	
	/**
	 * Creates a new {@code Item} instance using the next available unique ID.<br>
	 * This constructor automatically generates an {@code objectId}.
	 * @param itemId The unique identifier for the item type.
	 */
	public Item(int itemId)
	{
		this(IdManager.getInstance().getNextId(), itemId);
	}
	
	/**
	 * Allows a {@code Creature} to pick up this item.<br>
	 * This method handles the removal of the item from the world.<br>
	 * It also clears any active multisell states for players.<br>
	 * Finally, it notifies relevant scripts and systems.
	 * @param creature The {@code Creature} picking up the item.
	 */
	public void pickupMe(Creature creature)
	{
		// Prevent dropping and picking up items while using MaintainEnchantment multisells.
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			final PreparedMultisellListHolder multisell = player.getMultiSell();
			if ((multisell != null) && multisell.isMaintainEnchantment())
			{
				player.setMultiSell(null);
			}
		}
		
		// Create a server->client GetItem packet to pick up the Item.
		creature.broadcastPacket(new GetItem(this, creature.getObjectId()));
		
		final WorldRegion oldregion = getWorldRegion();
		synchronized (this)
		{
			setSpawned(false);
		}
		
		// If this item is a mercenary ticket, remove the spawns!
		final Castle castle = CastleManager.getInstance().getCastle(this);
		if ((castle != null) && (SiegeGuardManager.getInstance().getSiegeGuardByItem(castle.getResidenceId(), getId()) != null))
		{
			SiegeGuardManager.getInstance().removeTicket(this);
			ItemsOnGroundManager.getInstance().removeObject(this);
		}
		
		// Outside of synchronized to avoid deadlocks.
		// Remove the Item from the world.
		World.removeVisibleObject(this, oldregion);
		setWorldRegion(null);
		
		// Notify to scripts
		final ItemTemplate template = getTemplate();
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_PICKUP, template))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemPickup(player, this), template);
			}
		}
	}
	
	/**
	 * Sets the owner identifier for this item.<br>
	 * This method also handles logging and GM audit actions based on the configuration.
	 * @param process The type of process being performed.
	 * @param ownerId The new unique identifier for the owner.
	 * @param creator The player who initiated the action.
	 * @param reference An object or string used as a reference for logging purposes.
	 */
	public void setOwnerId(ItemProcessType process, int ownerId, Player creator, Object reference)
	{
		setOwnerId(ownerId);
		
		if ((GeneralConfig.LOG_ITEMS && ((!GeneralConfig.LOG_ITEMS_SMALL_LOG) && (!GeneralConfig.LOG_ITEMS_IDS_ONLY))) || (GeneralConfig.LOG_ITEMS_SMALL_LOG && (_itemTemplate.isEquipable() || (_itemTemplate.getId() == ADENA_ID))) || (GeneralConfig.LOG_ITEMS_IDS_ONLY && GeneralConfig.LOG_ITEMS_IDS_LIST.contains(_itemTemplate.getId())))
		{
			final String itemTemplateName = _itemTemplate.getName();
			if (_enchantLevel > 0)
			{
				LOG_ITEMS.info(StringUtil.concat("SETOWNER:", String.valueOf(process), ", item ", String.valueOf(getObjectId()), ":+", String.valueOf(_enchantLevel), " ", itemTemplateName, "(", String.valueOf(_count), "), ", String.valueOf(creator), ", ", String.valueOf(reference)));
			}
			else
			{
				LOG_ITEMS.info(StringUtil.concat("SETOWNER:", String.valueOf(process), ", item ", String.valueOf(getObjectId()), ":", itemTemplateName, "(", String.valueOf(_count), "), ", String.valueOf(creator), ", ", String.valueOf(reference)));
			}
		}
		
		if ((creator != null) && creator.isGM() && GeneralConfig.GMAUDIT)
		{
			final String targetName = (creator.getTarget() != null ? creator.getTarget().getName() : "no-target");
			String referenceName = "no-reference";
			if (reference instanceof WorldObject)
			{
				final WorldObject worldObj = (WorldObject) reference;
				referenceName = (worldObj.getName() != null ? worldObj.getName() : "no-name");
			}
			else if (reference instanceof String)
			{
				referenceName = (String) reference;
			}
			
			final String name = getName();
			GMAudit.logAction(creator.toString(), StringUtil.concat(String.valueOf(process), "(id: ", String.valueOf(_itemId), " name: ", name, ")"), targetName, StringUtil.concat("Object referencing this action is: ", referenceName));
		}
	}
	
	/**
	 * Sets the unique identifier for the current owner of this item.<br>
	 * This method updates the {@code _ownerId} and handles skill transfers.<br>
	 * It clears skills from the previous owner and assigns them to the new one.
	 * @param ownerId The new unique identifier for the item owner.
	 */
	public void setOwnerId(int ownerId)
	{
		if (ownerId == _ownerId)
		{
			return;
		}
		
		// Remove any inventory skills from the old owner.
		removeSkillsFromOwner();
		
		_owner = null;
		_ownerId = ownerId;
		_storedInDb = false;
		
		// Give any inventory skills to the new owner only if the item is in inventory.
		// else the skills will be given when location is set to inventory.
		giveSkillsToOwner();
	}
	
	/**
	 * Retrieves the unique identifier of the owner for this item.
	 * @return The {@code int} value representing the owner ID.
	 */
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	/**
	 * Updates the location of this item.<br>
	 * This method sets the {@code ItemLocation} for the current instance.
	 * @param loc The new {@code ItemLocation} to assign.
	 */
	public void setItemLocation(ItemLocation loc)
	{
		setItemLocation(loc, 0, true);
	}
	
	/**
	 * Updates the location of this item.<br>
	 * This method sets both the {@code ItemLocation} and the associated {@code locData}.
	 * @param loc The new {@code ItemLocation} for the item.
	 * @param locData The data value associated with the new location.
	 */
	public void setItemLocation(ItemLocation loc, int locData)
	{
		setItemLocation(loc, locData, true);
	}
	
	/**
	 * Updates the current location and data for this item.<br>
	 * This method handles moving the item between different containers or slots.<br>
	 * If {@code checkSkills} is {@code true}, it manages the transfer of skills between owners.
	 * @param loc The new {@code ItemLocation} to assign to the item.
	 * @param locData The data associated with the new location slot.
	 * @param checkSkills Whether to perform skill transfers during the move.
	 */
	public void setItemLocation(ItemLocation loc, int locData, boolean checkSkills)
	{
		if ((loc == _loc) && (locData == _locData))
		{
			return;
		}
		
		if (checkSkills)
		{
			// Remove any inventory skills from the old owner.
			removeSkillsFromOwner();
		}
		
		_loc = loc;
		_locData = locData;
		_storedInDb = false;
		
		if (checkSkills)
		{
			// Give any inventory skills to the new owner only if the item is in inventory.
			// else the skills will be given when location is set to inventory.
			giveSkillsToOwner();
		}
	}
	
	/**
	 * Retrieves the current location of this item.
	 * @return the {@code ItemLocation} of the item.
	 */
	public ItemLocation getItemLocation()
	{
		return _loc;
	}
	
	/**
	 * Updates the quantity of this item.<br>
	 * If the provided {@code count} is less than {@code -1}, it defaults to {@code 0}.<br>
	 * This method resets the {@code _storedInDb} flag to {@code false}.
	 * @param count The new amount for the item.
	 */
	public void setCount(long count)
	{
		if (_count == count)
		{
			return;
		}
		
		_count = count >= -1 ? count : 0;
		_storedInDb = false;
	}
	
	/**
	 * Retrieves the current quantity of this item.
	 * @return The number of items in the stack as a {@code long}.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * Updates the current count of this item.<br>
	 * This method handles overflow limits and ensures the count does not drop below {@code 0}.<br>
	 * It also logs the change if specific configuration settings are enabled.
	 * @param process The type of process that triggered the change.
	 * @param count The amount to add or subtract from the current count.
	 * @param creator The player who performed the action.
	 * @param reference An object providing additional context for the transaction.
	 */
	public void changeCount(ItemProcessType process, long count, Player creator, Object reference)
	{
		if (count == 0)
		{
			return;
		}
		
		final long old = _count;
		final long max = _itemId == ADENA_ID ? MAX_ADENA : Long.MAX_VALUE;
		if ((count > 0) && (_count > (max - count)))
		{
			setCount(max);
		}
		else
		{
			setCount(_count + count);
		}
		
		if (_count < 0)
		{
			setCount(0);
		}
		
		_storedInDb = false;
		
		if ((process != null) && (process != ItemProcessType.NONE))
		{
			if ((GeneralConfig.LOG_ITEMS && ((!GeneralConfig.LOG_ITEMS_SMALL_LOG) && (!GeneralConfig.LOG_ITEMS_IDS_ONLY))) || (GeneralConfig.LOG_ITEMS_SMALL_LOG && (_itemTemplate.isEquipable() || (_itemTemplate.getId() == ADENA_ID))) || (GeneralConfig.LOG_ITEMS_IDS_ONLY && GeneralConfig.LOG_ITEMS_IDS_LIST.contains(_itemTemplate.getId())))
			{
				final String itemTemplateName = _itemTemplate.getName();
				if (_enchantLevel > 0)
				{
					LOG_ITEMS.info(StringUtil.concat("CHANGE:", String.valueOf(process), ", item ", String.valueOf(getObjectId()), ":+", String.valueOf(_enchantLevel), " ", itemTemplateName, "(", String.valueOf(_count), "), PrevCount(", String.valueOf(old), "), ", String.valueOf(creator), ", ", String.valueOf(reference)));
				}
				else
				{
					LOG_ITEMS.info(StringUtil.concat("CHANGE:", String.valueOf(process), ", item ", String.valueOf(getObjectId()), ":", itemTemplateName, "(", String.valueOf(_count), "), PrevCount(", String.valueOf(old), "), ", String.valueOf(creator), ", ", String.valueOf(reference)));
				}
			}
			
			if ((creator != null) && creator.isGM() && GeneralConfig.GMAUDIT)
			{
				final String targetName = (creator.getTarget() != null ? creator.getTarget().getName() : "no-target");
				String referenceName = "no-reference";
				if (reference instanceof WorldObject)
				{
					referenceName = ((WorldObject) reference).getName() != null ? ((WorldObject) reference).getName() : "no-name";
				}
				else if (reference instanceof String)
				{
					referenceName = (String) reference;
				}
				
				final String name = getName();
				GMAudit.logAction(creator.toString(), StringUtil.concat(String.valueOf(process), "(id: ", String.valueOf(_itemId), " objId: ", String.valueOf(getObjectId()), " name: ", name, " count: ", String.valueOf(count), ")"), targetName, StringUtil.concat("Object referencing this action is: ", referenceName));
			}
		}
	}
	
	/**
	 * Checks if the item can be enchanted.<br>
	 * This depends on the item template and its current location.<br>
	 * The item must be in an {@code Inventory} or {@code Paperdoll} to return {@code true}.
	 * @return {@code true} if the item is enchantable, {@code false} otherwise.
	 */
	public boolean isEnchantable()
	{
		if ((_loc == ItemLocation.INVENTORY) || (_loc == ItemLocation.PAPERDOLL))
		{
			return _itemTemplate.isEnchantable();
		}
		
		return false;
	}
	
	/**
	 * Checks if the item can be equipped by a character.<br>
	 * This method returns {@code true} if the item is linked to a specific body part.<br>
	 * It returns {@code false} if the item has no associated body part.
	 * @return {@code true} if the item is equipable, otherwise {@code false}.
	 */
	public boolean isEquipable()
	{
		return _itemTemplate.getBodyPart() != BodyPart.NONE;
	}
	
	/**
	 * Checks if the item is currently equipped by a character.<br>
	 * It returns {@code true} if the location is either {@code PAPERDOLL} or {@code PET_EQUIP}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the item is equipped, {@code false} otherwise.
	 */
	public boolean isEquipped()
	{
		return (_loc == ItemLocation.PAPERDOLL) || (_loc == ItemLocation.PET_EQUIP);
	}
	
	/**
	 * Retrieves the specific slot data for this item's location.<br>
	 * This value is used to identify where an item is placed within a container.
	 * @return The integer value of the location slot.
	 */
	public int getLocationSlot()
	{
		return _locData;
	}
	
	/**
	 * Retrieves the base template for this item.<br>
	 * This provides access to the static data of the {@code ItemTemplate}.
	 * @return the {@code ItemTemplate} associated with this instance.
	 */
	public ItemTemplate getTemplate()
	{
		return _itemTemplate;
	}
	
	/**
	 * Retrieves the first custom type value for this item.<br>
	 * This value is used to categorize items with unique properties.
	 * @return The integer value of {@code customType1}.
	 */
	public int getCustomType1()
	{
		return _type1;
	}
	
	/**
	 * Retrieves the second custom type value for this item.<br>
	 * This value is used to store additional metadata not covered by standard fields.
	 * @return The integer value of {@code _type2}.
	 */
	public int getCustomType2()
	{
		return _type2;
	}
	
	/**
	 * Updates the first custom type value for this item.<br>
	 * This method sets the {@code _type1} field to a new integer value.
	 * @param newtype The new value to assign to the first custom type.
	 */
	public void setCustomType1(int newtype)
	{
		_type1 = newtype;
	}
	
	/**
	 * Updates the custom type 2 value for this item.<br>
	 * This method sets the internal {@code _type2} field to a new integer value.
	 * @param newtype The new value to assign to custom type 2.
	 */
	public void setCustomType2(int newtype)
	{
		_type2 = newtype;
	}
	
	/**
	 * Sets the drop time for this item.<br>
	 * This value determines when the item will be dropped.
	 * @param time The new drop time to set.
	 */
	public void setDropTime(long time)
	{
		_dropTime = time;
	}
	
	/**
	 * Retrieves the time when this item was dropped.<br>
	 * This value is returned as a {@code long}.
	 * @return The drop time of the item.
	 */
	public long getDropTime()
	{
		return _dropTime;
	}
	
	/**
	 * Retrieves the type of the item.<br>
	 * This value is obtained from the associated {@code ItemTemplate}.
	 * @return the {@code ItemType} of this item.
	 */
	public ItemType getItemType()
	{
		return _itemTemplate.getItemType();
	}
	
	/**
	 * Retrieves the unique identifier for this item.<br>
	 * This value corresponds to the {@code itemId} defined in the data files.
	 * @return The {@code int} ID of the item.
	 */
	@Override
	public int getId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the display ID of the item template.<br>
	 * This value is used to identify how the item appears in the game UI.
	 * @return the {@code int} display ID from the {@code ItemTemplate}.
	 */
	public int getDisplayId()
	{
		return _itemTemplate.getDisplayId();
	}
	
	/**
	 * Checks if the item belongs to the {@code EtcItem} category.<br>
	 * This method verifies the type of the underlying {@code ItemTemplate}.
	 * @return {@code true} if the item is an {@code EtcItem}, otherwise {@code false}.
	 */
	public boolean isEtcItem()
	{
		return _itemTemplate instanceof EtcItem;
	}
	
	/**
	 * Checks if the item is a weapon.<br>
	 * This method returns {@code true} if the template is an instance of {@code Weapon}.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if this item is a weapon, {@code false} otherwise.
	 */
	public boolean isWeapon()
	{
		return _itemTemplate instanceof Weapon;
	}
	
	/**
	 * Checks if the item is an armor type.<br>
	 * This method verifies if the {@code ItemTemplate} belongs to the {@code Armor} class.
	 * @return {@code true} if the item is armor, {@code false} otherwise.
	 */
	public boolean isArmor()
	{
		return _itemTemplate instanceof Armor;
	}
	
	/**
	 * Retrieves the {@code EtcItem} data from the item template.<br>
	 * This method checks if the current template is an instance of {@code EtcItem}.<br>
	 * It returns {@code null} if the template does not match this type.
	 * @return the {@code EtcItem} object or {@code null} if it does not exist.
	 */
	public EtcItem getEtcItem()
	{
		if (_itemTemplate instanceof EtcItem)
		{
			return (EtcItem) _itemTemplate;
		}
		
		return null;
	}
	
	/**
	 * Retrieves the {@code Weapon} data from the item template.<br>
	 * This method checks if the current template is an instance of {@code Weapon}.<br>
	 * It returns {@code null} if the item is not a weapon.
	 * @return the {@code Weapon} object or {@code null} if it does not exist.
	 */
	public Weapon getWeaponItem()
	{
		if (_itemTemplate instanceof Weapon)
		{
			return (Weapon) _itemTemplate;
		}
		
		return null;
	}
	
	/**
	 * Retrieves the {@code Armor} template from this item.<br>
	 * This method checks if the underlying template is an instance of {@code Armor}.<br>
	 * It returns {@code null} if the item is not armor.
	 * @return the {@code Armor} template, or {@code null} if it does not exist.
	 */
	public Armor getArmorItem()
	{
		if (_itemTemplate instanceof Armor)
		{
			return (Armor) _itemTemplate;
		}
		
		return null;
	}
	
	/**
	 * Retrieves the number of crystals required for an enchantment.<br>
	 * This value is determined by the current {@code _enchantLevel}.
	 * @return The total crystal count as an {@code int}.
	 */
	public int getCrystalCount()
	{
		return _itemTemplate.getCrystalCount(_enchantLevel);
	}
	
	/**
	 * Retrieves the base reference price for this item.<br>
	 * This value is obtained from the associated {@code ItemTemplate}.
	 * @return The reference price as a {@code long}.
	 */
	public long getReferencePrice()
	{
		return _itemTemplate.getReferencePrice();
	}
	
	/**
	 * Retrieves the display name of the item.<br>
	 * This value is fetched from the associated {@code ItemTemplate}.
	 * @return The name of the item as a {@code String}.
	 */
	public String getItemName()
	{
		return _itemTemplate.getName();
	}
	
	/**
	 * Retrieves the reuse delay for this item.<br>
	 * This value is fetched from the associated {@code ItemTemplate}.
	 * @return The reuse delay as an {@code int}.
	 */
	public int getReuseDelay()
	{
		return _itemTemplate.getReuseDelay();
	}
	
	/**
	 * Retrieves the shared reuse group for this item.<br>
	 * This value is obtained from the underlying {@code ItemTemplate}.
	 * @return the shared reuse group as an {@code int}.
	 */
	public int getSharedReuseGroup()
	{
		return _itemTemplate.getSharedReuseGroup();
	}
	
	/**
	 * Retrieves the timestamp of the last modification.<br>
	 * This value is used to track when the item was last updated.
	 * @return The integer representing the last change time.
	 */
	public int getLastChange()
	{
		return _lastChange;
	}
	
	/**
	 * Updates the timestamp of the last change for this item.<br>
	 * This value is used to track when the item was last modified.
	 * @param lastChange The new timestamp value to set.
	 */
	public void setLastChange(int lastChange)
	{
		_lastChange = lastChange;
	}
	
	/**
	 * Checks if this item can be stacked with other items.<br>
	 * This value is retrieved from the {@code ItemTemplate}.
	 * @return {@code true} if the item is stackable, {@code false} otherwise.
	 */
	public boolean isStackable()
	{
		return _itemTemplate.isStackable();
	}
	
	/**
	 * Checks if the item can be dropped by a player.<br>
	 * This method validates several conditions including template settings and transmog status.<br>
	 * It also considers augmentation rules and visual ID requirements.
	 * @return {@code true} if the item is dropable, {@code false} otherwise.
	 */
	public boolean isDropable()
	{
		if (_isVirtual)
		{
			return false;
		}
		
		if (!_itemTemplate.isDropable())
		{
			return false;
		}
		
		if (isEquipable() && (getTransmogId() > 0))
		{
			return false;
		}
		
		if (isAugmented())
		{
			return PlayerConfig.ALT_ALLOW_AUGMENT_TRADE;
		}
		
		return getVisualId() == 0;
	}
	
	/**
	 * Checks if the item can be destroyed.<br>
	 * This method verifies the template properties and augmentation status.
	 * @return {@code true} if the item is destroyable, {@code false} otherwise.
	 */
	public boolean isDestroyable()
	{
		if (!_itemTemplate.isDestroyable())
		{
			return false;
		}
		
		if (isAugmented())
		{
			return PlayerConfig.ALT_ALLOW_AUGMENT_DESTROY;
		}
		
		return true;
	}
	
	/**
	 * Checks if the item can be traded by a player.<br>
	 * This method verifies the template settings and specific item properties.<br>
	 * It considers transmog status and augmentation rules.
	 * @return {@code true} if the item is tradeable, {@code false} otherwise.
	 */
	public boolean isTradeable()
	{
		if (_isVirtual)
		{
			return false;
		}
		
		if (!_itemTemplate.isTradeable())
		{
			return false;
		}
		
		if (isEquipable() && (getTransmogId() > 0))
		{
			return false;
		}
		
		if (isAugmented())
		{
			return PlayerConfig.ALT_ALLOW_AUGMENT_TRADE;
		}
		
		return true;
	}
	
	/**
	 * Checks if the item can be sold to a vendor.<br>
	 * This method verifies the template settings and specific item properties.<br>
	 * It considers transmog status and augmentation rules.
	 * @return {@code true} if the item is sellable, {@code false} otherwise.
	 */
	public boolean isSellable()
	{
		if (_isVirtual)
		{
			return false;
		}
		
		if (!_itemTemplate.isSellable())
		{
			return false;
		}
		
		if (isEquipable() && (getTransmogId() > 0))
		{
			return false;
		}
		
		if (isAugmented())
		{
			return PlayerConfig.ALT_ALLOW_AUGMENT_TRADE;
		}
		
		return true;
	}
	
	/**
	 * Checks if the item can be deposited into a warehouse.<br>
	 * It verifies the item template properties and current equipment status.<br>
	 * It also checks tradeability based on whether it is a shadow item.
	 * @param isPrivateWareHouse The status of the private warehouse.
	 * @return {@code true} if the item can be deposited, {@code false} otherwise.
	 */
	public boolean isDepositable(boolean isPrivateWareHouse)
	{
		if (!_itemTemplate.isDepositable() || isEquipped())
		{
			return false;
		}
		
		if (!isPrivateWareHouse && (!isTradeable() || isShadowItem()))
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if the item is a potion.<br>
	 * This method returns {@code true} if the item template is defined as a potion.
	 * @return {@code true} if it is a potion, {@code false} otherwise.
	 */
	public boolean isPotion()
	{
		return _itemTemplate.isPotion();
	}
	
	/**
	 * Checks if the item is an elixir.<br>
	 * This method returns {@code true} if the template defines it as an elixir.
	 * @return {@code true} if the item is an elixir, {@code false} otherwise.
	 */
	public boolean isElixir()
	{
		return _itemTemplate.isElixir();
	}
	
	/**
	 * Checks if the item is a scroll.<br>
	 * This method returns {@code true} if the template defines it as a scroll.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if the item is a scroll, {@code false} otherwise.
	 */
	public boolean isScroll()
	{
		return _itemTemplate.isScroll();
	}
	
	/**
	 * Checks if the item is classified as a hero item.<br>
	 * This method retrieves the status from the {@code ItemTemplate}.
	 * @return {@code true} if it is a hero item, {@code false} otherwise.
	 */
	public boolean isHeroItem()
	{
		return _itemTemplate.isHeroItem();
	}
	
	/**
	 * Checks if the item is considered a common item.<br>
	 * This method returns the value from the underlying {@code ItemTemplate}.
	 * @return {@code true} if the item is common, {@code false} otherwise.
	 */
	public boolean isCommonItem()
	{
		return _itemTemplate.isCommon();
	}
	
	/**
	 * Checks if the item is allowed to be used in PvP.<br>
	 * This method returns the value from the {@code ItemTemplate}.
	 * @return {@code true} if it is a PvP item, {@code false} otherwise.
	 */
	public boolean isPvp()
	{
		return _itemTemplate.isPvpItem();
	}
	
	/**
	 * Checks if the item is restricted for use in Olympiad.<br>
	 * This method returns {@code true} if the item cannot be used in an Olympiad.
	 * @return {@code true} if the item is restricted, {@code false} otherwise.
	 */
	public boolean isOlyRestrictedItem()
	{
		return _itemTemplate.isOlyRestrictedItem();
	}
	
	/**
	 * Checks if the item can be used or interacted with by a specific player.<br>
	 * This method validates various conditions such as equipment status, quest restrictions,<br>
	 * and current actions like casting skills.
	 * @param player The {@code Player} attempting to use the item.
	 * @param allowAdena Whether to permit interactions with {@code ADENA_ID}.
	 * @param allowNonTradeable Whether to permit interactions with non-tradeable items.
	 * @return {@code true} if the item is available for use; {@code false} otherwise.
	 */
	public boolean isAvailable(Player player, boolean allowAdena, boolean allowNonTradeable)
	{
		final Summon pet = player.getPet();
		
		return ((!isEquipped()) // Not equipped
			&& (_itemTemplate.getType2() != ItemTemplate.TYPE2_QUEST) // Not Quest Item
			&& ((_itemTemplate.getType2() != ItemTemplate.TYPE2_MONEY) || (_itemTemplate.getType1() != ItemTemplate.TYPE1_SHIELD_ARMOR)) // not money, not shield
			&& ((pet == null) || (getObjectId() != pet.getControlObjectId())) // Not Control item of currently summoned pet.
			&& !(player.isProcessingItem(getObjectId())) // Not momentarily used enchant scroll.
			&& (allowAdena || (_itemId != ADENA_ID)) // Not Adena
			&& (!player.isCastingNow(s -> s.getSkill().getItemConsumeId() != _itemId)) && (allowNonTradeable || (isTradeable() && (!((_itemTemplate.getItemType() == EtcItemType.PET_COLLAR) && player.havePetInvItems())))));
	}
	
	/**
	 * Retrieves the current enchantment level of this item.<br>
	 * This value represents how many times the item has been enchanted.
	 * @return The current enchantment level as an {@code int}.
	 */
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	/**
	 * Checks if the item has an active enchantment.<br>
	 * It returns {@code true} if the enchantment level is greater than {@code 0}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if enchanted, {@code false} otherwise.
	 */
	public boolean isEnchanted()
	{
		return _enchantLevel > 0;
	}
	
	/**
	 * Sets the enchantment level for this item.<br>
	 * This method updates the internal level and recalculates stats.<br>
	 * It also clears any cached armor set data if a player is holding it.
	 * @param level The new enchantment level to apply.
	 */
	public void setEnchantLevel(int level)
	{
		final int newLevel = Math.max(0, level);
		if (_enchantLevel == newLevel)
		{
			return;
		}
		
		clearEnchantStats();
		
		// Agathion skills.
		final Player player = asPlayer();
		if (isEquipped() && (_itemTemplate.getBodyPart() == BodyPart.AGATHION))
		{
			final AgathionSkillHolder agathionSkills = AgathionData.getInstance().getSkills(getId());
			if (agathionSkills != null)
			{
				boolean update = false;
				
				// Remove old skills.
				for (Skill skill : agathionSkills.getMainSkills(_enchantLevel))
				{
					player.removeSkill(skill, false, skill.isPassive());
					update = true;
				}
				
				for (Skill skill : agathionSkills.getSubSkills(_enchantLevel))
				{
					player.removeSkill(skill, false, skill.isPassive());
					update = true;
				}
				
				// Add new skills.
				if (getLocationSlot() == Inventory.PAPERDOLL_AGATHION1)
				{
					for (Skill skill : agathionSkills.getMainSkills(newLevel))
					{
						if (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, player, player))
						{
							continue;
						}
						
						player.addSkill(skill, false);
						update = true;
					}
				}
				
				for (Skill skill : agathionSkills.getSubSkills(newLevel))
				{
					if (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, player, player))
					{
						continue;
					}
					
					player.addSkill(skill, false);
					update = true;
				}
				
				if (update)
				{
					player.sendSkillList();
				}
			}
		}
		
		_enchantLevel = newLevel;
		applyEnchantStats();
		_storedInDb = false;
		
		if (player != null)
		{
			player.getInventory().getPaperdollCache().clearArmorSetEnchant();
			
			// Notify to Scripts
			if (EventDispatcher.getInstance().hasListener(EventType.ON_ITEM_ENCHANT_ADD))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnItemEnchantAdd(player, this));
			}
		}
	}
	
	/**
	 * Checks if the item has an augmentation.<br>
	 * Returns {@code true} if the internal augmentation field is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if augmented, {@code false} otherwise.
	 */
	public boolean isAugmented()
	{
		return _augmentation != null;
	}
	
	/**
	 * Retrieves the current augmentation of this item.
	 * @return the {@code AugmentationInstance} associated with this item.
	 */
	public AugmentationInstance getAugmentation()
	{
		return _augmentation;
	}
	
	/**
	 * Sets the augmentation for this item.<br>
	 * This method removes any existing augmentation before applying the new one.<br>
	 * It optionally updates the database and notifies relevant scripts.
	 * @param augmentation The {@code AugmentationInstance} to apply to the item.
	 * @param updateDatabase Set to {@code true} to persist changes to the database.
	 * @return {@code true} if the operation was successful.
	 */
	public boolean setAugmentation(AugmentationInstance augmentation, boolean updateDatabase)
	{
		// Remove previous augmentation.
		if (_augmentation != null)
		{
			if (isEquipped())
			{
				_augmentation.removeBonus(asPlayer());
			}
			
			removeAugmentation();
		}
		
		_augmentation = augmentation;
		if (updateDatabase)
		{
			updateItemOptions();
		}
		
		// Notify to scripts.
		final ItemTemplate template = getTemplate();
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_AUGMENT, template))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerAugment(asPlayer(), this, augmentation, true), template);
		}
		
		return true;
	}
	
	/**
	 * Removes the augmentation from this item.<br>
	 * This method clears the {@code _augmentation} field and deletes the entry from the database.<br>
	 * It also notifies any active listeners about the removal.
	 */
	public void removeAugmentation()
	{
		if (_augmentation == null)
		{
			return;
		}
		
		// Copy augmentation before removing it.
		final AugmentationInstance augment = _augmentation;
		_augmentation = null;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM item_augmentations WHERE itemId = ?"))
		{
			ps.setInt(1, getObjectId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Item could not remove augmentation for " + this + " from DB: ", e);
		}
		
		// Notify to scripts.
		final ItemTemplate template = getTemplate();
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_AUGMENT, template))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerAugment(asPlayer(), this, augment, false), template);
		}
	}
	
	/**
	 * This method restores the item's unique attributes from the database.<br>
	 * It loads augmentation data such as mineral IDs and options.<br>
	 * It also retrieves and applies elemental attributes to the instance.
	 */
	public void restoreAttributes()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement("SELECT mineralId,option1,option2,option3 FROM item_augmentations WHERE itemId=?");
			PreparedStatement ps2 = con.prepareStatement("SELECT elemType,elemValue FROM item_elementals WHERE itemId=?"))
		{
			final int objectId = getObjectId();
			ps1.setInt(1, objectId);
			try (ResultSet rs = ps1.executeQuery())
			{
				if (rs.next())
				{
					final int mineralId = rs.getInt("mineralId");
					final int option1 = rs.getInt("option1");
					final int option2 = rs.getInt("option2");
					final int option3 = rs.getInt("option3");
					if ((option1 > 0) || (option2 > 0) || (option3 > 0))
					{
						_augmentation = new AugmentationInstance(mineralId, option1, option2, option3);
					}
				}
			}
			
			ps2.setInt(1, objectId);
			try (ResultSet rs = ps2.executeQuery())
			{
				while (rs.next())
				{
					final byte attributeType = rs.getByte(1);
					final int attributeValue = rs.getInt(2);
					if ((attributeType != -1) && (attributeValue != -1))
					{
						applyAttribute(new AttributeHolder(AttributeType.findByClientId(attributeType), attributeValue));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Item could not restore augmentation and elemental data for " + this + " from DB: ", e);
		}
	}
	
	/**
	 * Updates the item options using a database connection.<br>
	 * This method refreshes the attributes of the current {@code Item}.<br>
	 * It handles any necessary data synchronization with the database.
	 */
	public void updateItemOptions()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			updateItemOptions(con);
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Item could not update atributes for " + this + " from DB:", e);
		}
	}
	
	/**
	 * Updates the item augmentations in the database.<br>
	 * This method saves the current augmentation data to the {@code item_augmentations} table.<br>
	 * It uses a {@code REPLACE INTO} statement to ensure the record is updated or inserted.
	 * @param con The active {@code Connection} used to execute the SQL query.
	 */
	private void updateItemOptions(Connection con)
	{
		try (PreparedStatement ps = con.prepareStatement("REPLACE INTO item_augmentations VALUES(?,?,?,?,?)"))
		{
			ps.setInt(1, getObjectId());
			if (_augmentation != null)
			{
				ps.setInt(2, _augmentation.getMineralId());
				ps.setInt(3, _augmentation.getOption1Id());
				ps.setInt(4, _augmentation.getOption2Id());
				ps.setInt(5, _augmentation.getOption3Id());
			}
			else
			{
				ps.setInt(2, 0);
				ps.setInt(3, 0);
				ps.setInt(4, 0);
				ps.setInt(5, 0);
			}
			
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Item could not update atributes for " + this + " from DB: ", e);
		}
	}
	
	/**
	 * Updates the elemental properties of the item.<br>
	 * This method synchronizes data with the database.
	 */
	public void updateItemElementals()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			updateItemElements(con);
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Item could not update elementals for " + this + " from DB: ", e);
		}
	}
	
	/**
	 * Updates the elemental attributes of an item in the database.<br>
	 * This method removes existing entries and inserts new ones based on current values.<br>
	 * It handles {@code SQLException} by logging a severe error message.
	 * @param con The {@code Connection} object used to execute SQL queries.
	 */
	private void updateItemElements(Connection con)
	{
		try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_elementals WHERE itemId = ?"))
		{
			ps.setInt(1, getObjectId());
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Item could not update elementals for " + this + " from DB: ", e);
		}
		
		if (_elementals == null)
		{
			return;
		}
		
		try (PreparedStatement ps = con.prepareStatement("INSERT INTO item_elementals VALUES(?,?,?)"))
		{
			for (AttributeHolder attribute : _elementals.values())
			{
				ps.setInt(1, getObjectId());
				ps.setByte(2, attribute.getType().getClientId());
				ps.setInt(3, attribute.getValue());
				ps.executeUpdate();
				ps.clearParameters();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Item could not update elementals for " + this + " from DB: ", e);
		}
	}
	
	/**
	 * Retrieves the collection of attributes for this item.<br>
	 * Returns {@code null} if no attributes exist.
	 * @return a {@code Collection} of {@code AttributeHolder} objects or {@code null}.
	 */
	public Collection<AttributeHolder> getAttributes()
	{
		return _elementals != null ? _elementals.values() : null;
	}
	
	/**
	 * Checks if the item has any elemental attributes.<br>
	 * Returns {@code true} if the attribute list is not empty.<br>
	 * Returns {@code false} otherwise.
	 * @return {@code true} if attributes exist, {@code false} otherwise.
	 */
	public boolean hasAttributes()
	{
		return (_elementals != null) && !_elementals.isEmpty();
	}
	
	/**
	 * Retrieves a specific elemental attribute from the item.<br>
	 * This method returns {@code null} if no attributes are present.
	 * @param type The {@code AttributeType} to look up.
	 * @return The corresponding {@code AttributeHolder} or {@code null}.
	 */
	public AttributeHolder getAttribute(AttributeType type)
	{
		return _elementals != null ? _elementals.get(type) : null;
	}
	
	/**
	 * Retrieves the primary attack attribute for a weapon.<br>
	 * This method checks if the item is a weapon and returns its first available attribute.<br>
	 * If no attributes are found, it may return an elemental attribute.<br>
	 * Returns {@code null} if the item is not a weapon or has no attributes.
	 * @return The first {@code AttributeHolder} associated with the weapon, or {@code null}.
	 */
	public AttributeHolder getAttackAttribute()
	{
		if (isWeapon())
		{
			final Collection<AttributeHolder> templateAttributes = _itemTemplate.getAttributes();
			if (templateAttributes != null)
			{
				if (!templateAttributes.isEmpty())
				{
					return templateAttributes.iterator().next();
				}
			}
			else if ((_elementals != null) && !_elementals.isEmpty())
			{
				return _elementals.values().iterator().next();
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the type of attack attribute for this item.<br>
	 * It returns {@code AttributeType.NONE} if no attribute is found.
	 * @return The {@code AttributeType} associated with the item's attack attribute.
	 */
	public AttributeType getAttackAttributeType()
	{
		final AttributeHolder holder = getAttackAttribute();
		return holder != null ? holder.getType() : AttributeType.NONE;
	}
	
	/**
	 * Retrieves the current attack attribute power of this item.<br>
	 * It returns the value from the {@code AttributeHolder}.<br>
	 * If no attribute exists, it returns {@code 0}.
	 * @return The integer value of the attack attribute power.
	 */
	public int getAttackAttributePower()
	{
		final AttributeHolder holder = getAttackAttribute();
		return holder != null ? holder.getValue() : 0;
	}
	
	/**
	 * Retrieves the defense value for a specific attribute type.<br>
	 * This method checks if the item is armor before looking up the value.<br>
	 * It returns {@code 0} if no valid attribute is found.
	 * @param element The {@code AttributeType} to check for.
	 * @return The integer value of the defense attribute.
	 */
	public int getDefenceAttribute(AttributeType element)
	{
		if (isArmor())
		{
			if (_itemTemplate.getAttributes() != null)
			{
				final AttributeHolder attribute = _itemTemplate.getAttribute(element);
				if (attribute != null)
				{
					return attribute.getValue();
				}
			}
			else if (_elementals != null)
			{
				final AttributeHolder attribute = getAttribute(element);
				if (attribute != null)
				{
					return attribute.getValue();
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Updates the internal collection of attributes.<br>
	 * This method adds a new {@code AttributeHolder} if it does not exist.<br>
	 * If an attribute of the same type already exists, its value is updated.
	 * @param holder The {@code AttributeHolder} to be processed.
	 */
	private synchronized void applyAttribute(AttributeHolder holder)
	{
		if (_elementals == null)
		{
			_elementals = new LinkedHashMap<>(3);
			_elementals.put(holder.getType(), holder);
		}
		else
		{
			final AttributeType type = holder.getType();
			final AttributeHolder attribute = getAttribute(type);
			if (attribute != null)
			{
				attribute.setValue(holder.getValue());
			}
			else
			{
				_elementals.put(type, holder);
			}
		}
		
		// Notify to Scripts
		if (EventDispatcher.getInstance().hasListener(EventType.ON_ITEM_ATTRIBUTE_ADD))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnItemAttributeAdd(asPlayer(), this));
		}
	}
	
	/**
	 * Updates the attributes for a specific {@code AttributeHolder}.<br>
	 * This method applies changes to the holder immediately.<br>
	 * If {@code updateDatabase} is {@code true}, it also refreshes the item elementals.
	 * @param holder The {@code AttributeHolder} to modify.
	 * @param updateDatabase Set to {@code true} to persist changes to the database.
	 */
	public void setAttribute(AttributeHolder holder, boolean updateDatabase)
	{
		applyAttribute(holder);
		if (updateDatabase)
		{
			updateItemElementals();
		}
	}
	
	/**
	 * Removes a specific elemental attribute from the item.<br>
	 * This method updates both the internal memory and the database.<br>
	 * If the attribute does not exist, the method returns immediately.
	 * @param type The {@code AttributeType} to be removed.
	 */
	public void clearAttribute(AttributeType type)
	{
		if ((_elementals == null) || (getAttribute(type) == null))
		{
			return;
		}
		
		synchronized (_elementals)
		{
			_elementals.remove(type);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM item_elementals WHERE itemId = ? AND elemType = ?"))
		{
			ps.setInt(1, getObjectId());
			ps.setByte(2, type.getClientId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Item could not remove elemental enchant for " + this + " from DB: ", e);
		}
	}
	
	/**
	 * Removes all elemental attributes from the item.<br>
	 * This method clears the internal list of elementals.<br>
	 * It also deletes all associated records from the database.
	 */
	public void clearAllAttributes()
	{
		if (_elementals == null)
		{
			return;
		}
		
		synchronized (_elementals)
		{
			_elementals.clear();
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM item_elementals WHERE itemId = ?"))
		{
			ps.setInt(1, getObjectId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Item could not remove all elemental enchant for " + this + " from DB: ", e);
		}
	}
	
	/**
	 * Checks if the item is currently considered a shadow item.<br>
	 * This method returns {@code true} if the mana value is zero or greater.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if the item is a shadow item, {@code false} otherwise.
	 */
	public boolean isShadowItem()
	{
		return _mana >= 0;
	}
	
	/**
	 * Retrieves the current mana value of the item.<br>
	 * This method returns the amount stored in the {@code _mana} field.
	 * @return The current mana as an {@code int}.
	 */
	public int getMana()
	{
		return _mana;
	}
	
	/**
	 * Reduces the mana of the entity.<br>
	 * This method handles the deduction logic for mana usage.
	 * @param resetConsumingMana Determines if the consuming mana should be reset to {@code true} or {@code false}.
	 */
	public void decreaseMana(boolean resetConsumingMana)
	{
		decreaseMana(resetConsumingMana, 1);
	}
	
	/**
	 * Reduces the mana of a shadow item by a specific amount.<br>
	 * This method handles item destruction if mana reaches {@code 0}.<br>
	 * It also sends system messages to the player regarding remaining mana.
	 * @param resetConsumingMana Set to {@code true} to stop the automatic mana consumption task.
	 * @param count The amount of mana to subtract from the item.
	 */
	public void decreaseMana(boolean resetConsumingMana, int count)
	{
		if (!isShadowItem())
		{
			return;
		}
		
		if ((_mana - count) >= 0)
		{
			_mana -= count;
		}
		else
		{
			_mana = 0;
		}
		
		if (_storedInDb)
		{
			_storedInDb = false;
		}
		
		if (resetConsumingMana)
		{
			_consumingMana = false;
		}
		
		final Player player = asPlayer();
		if (player == null)
		{
			return;
		}
		
		SystemMessage sm;
		switch (_mana)
		{
			case 10:
			{
				sm = new SystemMessage(SystemMessageId.S1_S_REMAINING_MANA_IS_NOW_10);
				sm.addItemName(_itemTemplate);
				player.sendPacket(sm);
				break;
			}
			case 5:
			{
				sm = new SystemMessage(SystemMessageId.S1_S_REMAINING_MANA_IS_NOW_5);
				sm.addItemName(_itemTemplate);
				player.sendPacket(sm);
				break;
			}
			case 1:
			{
				sm = new SystemMessage(SystemMessageId.S1_S_REMAINING_MANA_IS_NOW_1_IT_WILL_DISAPPEAR_SOON);
				sm.addItemName(_itemTemplate);
				player.sendPacket(sm);
				break;
			}
		}
		
		if (_mana == 0) // The life time has expired.
		{
			sm = new SystemMessage(SystemMessageId.S1_S_REMAINING_MANA_IS_NOW_0_AND_THE_ITEM_HAS_DISAPPEARED);
			sm.addItemName(_itemTemplate);
			player.sendPacket(sm);
			
			// Unequip.
			final PlayerInventory playerInventory = player.getInventory();
			if (isEquipped())
			{
				final InventoryUpdate iu = new InventoryUpdate();
				for (Item item : playerInventory.unEquipItemInSlotAndRecord(getLocationSlot()))
				{
					iu.addModifiedItem(item);
				}
				
				player.sendInventoryUpdate(iu);
				player.broadcastUserInfo();
			}
			
			if (_loc != ItemLocation.WAREHOUSE)
			{
				// Destroy.
				playerInventory.destroyItem(ItemProcessType.DESTROY, this, player, null);
				
				// Send update.
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addRemovedItem(this);
				player.sendInventoryUpdate(iu);
			}
			else
			{
				player.getWarehouse().destroyItem(ItemProcessType.DESTROY, this, player, null);
			}
			
			// Delete from world. (redundant - already removed by destroyItem)
			// World.removeObject(this);
		}
		else
		{
			// Reschedule if still equipped.
			if (!_consumingMana && isEquipped())
			{
				scheduleConsumeManaTask();
			}
			
			if (_loc != ItemLocation.WAREHOUSE)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(this);
				player.sendInventoryUpdate(iu);
			}
		}
	}
	
	/**
	 * Schedules a task to consume mana for this item.<br>
	 * This method checks if the consumption process is already active.<br>
	 * If not, it sets the status and adds the item to the {@code ItemManaTaskManager}.
	 */
	public void scheduleConsumeManaTask()
	{
		if (_consumingMana)
		{
			return;
		}
		
		_consumingMana = true;
		ItemManaTaskManager.getInstance().add(this);
	}
	
	/**
	 * Checks if the item can be attacked by a specific creature.<br>
	 * This method currently always returns {@code false}.
	 * @param attacker The {@code Creature} attempting to attack the item.
	 * @return {@code false} regardless of the input.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}
	
	/**
	 * Updates the current item information in the database.<br>
	 * This method saves the state of the {@code Item} instance to the persistent storage.
	 */
	public void updateDatabase()
	{
		updateDatabase(false);
	}
	
	/**
	 * Updates the current item data in the database.<br>
	 * This method handles inserting, updating, or removing records based on the item state.<br>
	 * It respects the {@code GeneralConfig.LAZY_ITEMS_UPDATE} setting unless forced.
	 * @param force If {@code true}, the update will occur regardless of lazy loading settings.
	 */
	public void updateDatabase(boolean force)
	{
		_dbLock.lock();
		
		try
		{
			if (_existsInDb)
			{
				if ((_ownerId == 0) || (_loc == ItemLocation.VOID) || (_loc == ItemLocation.REFUND) || ((_count == 0) && (_loc != ItemLocation.LEASE)))
				{
					removeFromDb();
				}
				else if (!GeneralConfig.LAZY_ITEMS_UPDATE || force)
				{
					updateInDb();
				}
			}
			else
			{
				if ((_ownerId == 0) || (_loc == ItemLocation.VOID) || (_loc == ItemLocation.REFUND) || ((_count == 0) && (_loc != ItemLocation.LEASE)))
				{
					return;
				}
				
				insertIntoDb();
			}
		}
		finally
		{
			_dbLock.unlock();
		}
	}
	
	/**
	 * Drops the item into the world at a specific location.<br>
	 * This method handles position validation and updates the visible object list.<br>
	 * It also manages ownership and triggers relevant events if dropped by a player.
	 * @param dropper The {@code Creature} that drops the item, or {@code null} for a global drop.
	 * @param locX The target X coordinate.
	 * @param locY The target Y coordinate.
	 * @param locZ The target Z coordinate.
	 */
	public void dropMe(Creature dropper, int locX, int locY, int locZ)
	{
		int x = locX;
		int y = locY;
		int z = locZ;
		
		if (dropper != null)
		{
			final Instance instance = dropper.getInstanceWorld();
			final Location dropDest = GeoEngine.getInstance().getValidLocation(dropper.getX(), dropper.getY(), dropper.getZ(), x, y, z, instance);
			x = dropDest.getX();
			y = dropDest.getY();
			z = dropDest.getZ();
			setInstance(instance); // Inherit instancezone when dropped in visible world.
		}
		else
		{
			setInstance(null); // No dropper? Make it a global item...
		}
		
		// Set the x,y,z position of the Item dropped and update its world region.
		setSpawned(true);
		setXYZ(x, y, z);
		
		setDropTime(System.currentTimeMillis());
		setDropperObjectId(dropper != null ? dropper.getObjectId() : 0); // Set the dropper Id for the knownlist packets in sendInfo.
		
		// Add the Item dropped in the world as a visible object.
		final WorldRegion region = getWorldRegion();
		region.addVisibleObject(this);
		World.addVisibleObject(this, region);
		if (GeneralConfig.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance().save(this);
		}
		
		setDropperObjectId(0); // Set the dropper Id back to 0 so it no longer shows the drop packet.
		
		if ((dropper != null) && dropper.isPlayer())
		{
			_owner = null;
			
			// Notify to scripts
			final ItemTemplate template = getTemplate();
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_DROP, template))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDrop(dropper.asPlayer(), this, new Location(x, y, z)), template);
			}
		}
	}
	
	/**
	 * Updates the current item data in the database.<br>
	 * This method saves properties like owner ID, count, and location.<br>
	 * It also updates augmentation and elemental data if they exist.
	 */
	private void updateInDb()
	{
		if (!_existsInDb || _wear || _storedInDb)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE items SET owner_id=?,count=?,loc=?,loc_data=?,enchant_level=?,custom_type1=?,custom_type2=?,mana_left=?,time=? WHERE object_id = ?"))
		{
			ps.setInt(1, _ownerId);
			ps.setLong(2, _count);
			ps.setString(3, _loc.name());
			ps.setInt(4, _locData);
			ps.setInt(5, _enchantLevel);
			ps.setInt(6, _type1);
			ps.setInt(7, _type2);
			ps.setInt(8, _mana);
			ps.setLong(9, _time);
			ps.setInt(10, getObjectId());
			ps.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;
			
			if (_augmentation != null)
			{
				updateItemOptions(con);
			}
			
			if (_elementals != null)
			{
				updateItemElements(con);
			}
			
			updateSpecialAbilities(con);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Item could not update " + this + " in DB: Reason: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Saves the current item data to the database.<br>
	 * This method checks if the item is already stored or currently worn before inserting.<br>
	 * It updates related options and elements if they exist.
	 */
	private void insertIntoDb()
	{
		final int objectId = getObjectId();
		if (_existsInDb || (objectId == 0) || _wear)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,object_id,custom_type1,custom_type2,mana_left,time) VALUES (?,?,?,?,?,?,?,?,?,?,?)"))
		{
			ps.setInt(1, _ownerId);
			ps.setInt(2, _itemId);
			ps.setLong(3, _count);
			ps.setString(4, _loc.name());
			ps.setInt(5, _locData);
			ps.setInt(6, _enchantLevel);
			ps.setInt(7, objectId);
			ps.setInt(8, _type1);
			ps.setInt(9, _type2);
			ps.setInt(10, _mana);
			ps.setLong(11, _time);
			
			ps.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;
			
			if (_augmentation != null)
			{
				updateItemOptions(con);
			}
			
			if (_elementals != null)
			{
				updateItemElements(con);
			}
			
			updateSpecialAbilities(con);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Item could not insert " + this + " into DB: Reason: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes the item data from the database.<br>
	 * This method deletes records from multiple tables associated with this object.<br>
	 * It only executes if the item exists in the database and is not currently worn.
	 */
	private void removeFromDb()
	{
		if (!_existsInDb || _wear)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			final int objectId = getObjectId();
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM items WHERE object_id = ?"))
			{
				ps.setInt(1, objectId);
				ps.executeUpdate();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_augmentations WHERE itemId = ?"))
			{
				ps.setInt(1, objectId);
				ps.executeUpdate();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_elementals WHERE itemId = ?"))
			{
				ps.setInt(1, objectId);
				ps.executeUpdate();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_special_abilities WHERE objectId = ?"))
			{
				ps.setInt(1, objectId);
				ps.executeUpdate();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_variables WHERE id = ?"))
			{
				ps.setInt(1, objectId);
				ps.executeUpdate();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Item could not delete " + this + " in DB ", e);
		}
		finally
		{
			_existsInDb = false;
			_storedInDb = false;
		}
	}
	
	/**
	 * Cancels the active loot schedule for this item.<br>
	 * This method sets the {@code _itemLootShedule} to {@code null}.
	 */
	public void resetOwnerTimer()
	{
		if (_itemLootShedule != null)
		{
			_itemLootShedule.cancel(true);
			_itemLootShedule = null;
		}
	}
	
	/**
	 * Sets the scheduled task for item looting.<br>
	 * This method assigns a {@code ScheduledFuture<?>} to the internal schedule variable.
	 * @param sf The {@code ScheduledFuture<?>} representing the loot schedule.
	 */
	public void setItemLootShedule(ScheduledFuture<?> sf)
	{
		_itemLootShedule = sf;
	}
	
	/**
	 * Retrieves the scheduled task for item looting.<br>
	 * This returns a {@code ScheduledFuture} object representing the loot process.
	 * @return The {@code ScheduledFuture} for the item loot schedule.
	 */
	public ScheduledFuture<?> getItemLootShedule()
	{
		return _itemLootShedule;
	}
	
	/**
	 * Sets whether the item is protected.<br>
	 * Use {@code true} to protect the item from being moved or modified.<br>
	 * Use {@code false} to allow normal interactions.
	 * @param isProtected The protection status of the item.
	 */
	public void setProtected(boolean isProtected)
	{
		_protected = isProtected;
	}
	
	/**
	 * Checks if the item is currently protected.<br>
	 * This status determines if the item can be moved or interacted with by others.
	 * @return {@code true} if the item is protected, {@code false} otherwise.
	 */
	public boolean isProtected()
	{
		return _protected;
	}
	
	/**
	 * Checks if the item can be used or interacted with based on specific conditions.<br>
	 * It verifies requirements like player status and template constraints.<br>
	 * Items in pet locations are always considered available.
	 * @return {@code true} if the item meets all requirements, {@code false} otherwise.
	 */
	public boolean isAvailable()
	{
		if (!_itemTemplate.isConditionAttached())
		{
			return true;
		}
		
		if ((_loc == ItemLocation.PET) || (_loc == ItemLocation.PET_EQUIP))
		{
			return true;
		}
		
		final Player player = asPlayer();
		if (player != null)
		{
			for (Condition condition : _itemTemplate.getConditions())
			{
				if (condition == null)
				{
					continue;
				}
				
				if (!condition.testImpl(player, player, null, _itemTemplate))
				{
					return false;
				}
			}
			
			if (player.hasRequest(AutoPeelRequest.class))
			{
				final EtcItem etcItem = getEtcItem();
				if ((etcItem != null) && (etcItem.getExtractableItems() != null))
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Updates the internal state for decreasing the item count.<br>
	 * This method sets the {@code _decrease} flag based on the provided value.
	 * @param decrease The boolean value to set for the decrease operation.
	 */
	public void setCountDecrease(boolean decrease)
	{
		_decrease = decrease;
	}
	
	/**
	 * Checks if the item count is currently decreasing.
	 * @return {@code true} if the count is decreasing, {@code false} otherwise.
	 */
	public boolean getCountDecrease()
	{
		return _decrease;
	}
	
	/**
	 * Sets the initial count for this item.<br>
	 * This value is used during the initialization process.
	 * @param initCount The number of items to set as the initial count.
	 */
	public void setInitCount(int initCount)
	{
		_initCount = initCount;
	}
	
	/**
	 * Retrieves the initial count of this item.<br>
	 * This value represents the quantity at the time of initialization.
	 * @return the initial count as a {@code long}.
	 */
	public long getInitCount()
	{
		return _initCount;
	}
	
	/**
	 * Resets the item count to its initial value.<br>
	 * This method is only called if the {@code _decrease} flag is true.<br>
	 * It uses the stored {@code _initCount} value.
	 */
	public void restoreInitCount()
	{
		if (_decrease)
		{
			setCount(_initCount);
		}
	}
	
	/**
	 * Checks if the item has a time limit.<br>
	 * Returns {@code true} if the time value is greater than {@code 0}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the item is time limited, {@code false} otherwise.
	 */
	public boolean isTimeLimitedItem()
	{
		return _time > 0;
	}
	
	/**
	 * Retrieves the current time of the item.
	 * @return the current time as a {@code long} value.
	 */
	public long getTime()
	{
		return _time;
	}
	
	/**
	 * Calculates the time remaining for this item.<br>
	 * It subtracts the current system time from the expiration timestamp.
	 * @return The remaining time in milliseconds as a {@code long}.
	 */
	public long getRemainingTime()
	{
		return _time - System.currentTimeMillis();
	}
	
	/**
	 * Handles the expiration of an item.<br>
	 * This method removes the item from the player's inventory or warehouse.<br>
	 * It also sends a notification to the player that the item has expired.
	 */
	public void endOfLife()
	{
		final Player player = asPlayer();
		if (player == null)
		{
			return;
		}
		
		final PlayerInventory playerInventory = player.getInventory();
		if (isEquipped())
		{
			final InventoryUpdate iu = new InventoryUpdate();
			for (Item item : playerInventory.unEquipItemInSlotAndRecord(getLocationSlot()))
			{
				iu.addModifiedItem(item);
			}
			
			player.sendInventoryUpdate(iu);
		}
		
		if (_loc != ItemLocation.WAREHOUSE)
		{
			// Destroy.
			playerInventory.destroyItem(ItemProcessType.DESTROY, this, player, null);
			
			// Send update.
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addRemovedItem(this);
			player.sendInventoryUpdate(iu);
		}
		else
		{
			player.getWarehouse().destroyItem(ItemProcessType.DESTROY, this, player, null);
		}
		
		player.sendPacket(new SystemMessage(SystemMessageId.S1_HAS_EXPIRED).addItemName(_itemId));
		
		// Delete from world. (redundant - already removed by destroyItem)
		// World.removeObject(this);
	}
	
	/**
	 * Schedules the expiration task for this item.<br>
	 * This method checks if the item has a limited lifetime.<br>
	 * If the time has expired, it calls {@code endOfLife()}.<br>
	 * Otherwise, it adds the item to the {@code ItemLifeTimeTaskManager} for tracking.
	 */
	public void scheduleLifeTimeTask()
	{
		if (!isTimeLimitedItem())
		{
			return;
		}
		
		if (getRemainingTime() <= 0)
		{
			endOfLife();
		}
		else
		{
			ItemLifeTimeTaskManager.getInstance().add(this, getTime());
		}
	}
	
	/**
	 * Sets the unique identifier for the dropper object.<br>
	 * This value is used to link the item to a specific dropper entity.
	 * @param id The {@code int} ID of the dropper object.
	 */
	public void setDropperObjectId(int id)
	{
		_dropperObjectId = id;
	}
	
	/**
	 * Sends the necessary network packet to a {@code Player}.<br>
	 * This method determines whether to send a {@code DropItem} or {@code SpawnItem} packet.<br>
	 * The choice depends on whether the item was dropped by another object.
	 * @param player The {@code Player} who will receive the information.
	 */
	@Override
	public void sendInfo(Player player)
	{
		if (_dropperObjectId != 0)
		{
			player.sendPacket(new DropItem(this, _dropperObjectId));
		}
		else
		{
			player.sendPacket(new SpawnItem(this));
		}
	}
	
	/**
	 * Retrieves the current drop protection status.<br>
	 * This value determines if the item can be dropped by the owner.
	 * @return the {@code DropProtection} status of this item.
	 */
	public DropProtection getDropProtection()
	{
		return _dropProtection;
	}
	
	/**
	 * Checks if the item has been published.
	 * @return {@code true} if the item is published, {@code false} otherwise.
	 */
	public boolean isPublished()
	{
		return _published;
	}
	
	/**
	 * Updates the publication status of this item.<br>
	 * Sets the {@code _published} flag to {@code true}.
	 */
	public void publish()
	{
		_published = true;
	}
	
	/**
	 * Removes the item from the world if dropped items are set to save in the configuration.<br>
	 * This method then calls the parent {@code decayMe()} logic.
	 * @return {@code true} if the item was successfully decayed, and {@code false} otherwise.
	 */
	@Override
	public boolean decayMe()
	{
		if (GeneralConfig.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance().removeObject(this);
		}
		
		return super.decayMe();
	}
	
	/**
	 * Checks if the item is designated as a quest item.<br>
	 * This method retrieves the status from the {@code ItemTemplate}.
	 * @return {@code true} if it is a quest item, {@code false} otherwise.
	 */
	public boolean isQuestItem()
	{
		return _itemTemplate.isQuestItem();
	}
	
	/**
	 * Checks if the item is an Artifact (card or book), which the client displays in its own dedicated inventory tab.<br>
	 * This method retrieves the status from the {@code ItemTemplate}.
	 * @return {@code true} if it is an Artifact, {@code false} otherwise.
	 */
	public boolean isArtifactItem()
	{
		return _itemTemplate.isArtifactItem();
	}
	
	/**
	 * Checks if the item can be affected by elements.<br>
	 * This check only applies if the item is in an {@code Inventory} or on a {@code Paperdoll}.<br>
	 * It returns the value from the associated {@code ItemTemplate}.
	 * @return {@code true} if the item is elementable and in a valid location, {@code false} otherwise.
	 */
	public boolean isElementable()
	{
		if ((_loc == ItemLocation.INVENTORY) || (_loc == ItemLocation.PAPERDOLL))
		{
			return _itemTemplate.isElementable();
		}
		
		return false;
	}
	
	/**
	 * Checks if the item can be transported as freight.<br>
	 * This method returns the value from the associated {@code ItemTemplate}.
	 * @return {@code true} if the item is freightable, {@code false} otherwise.
	 */
	public boolean isFreightable()
	{
		return _itemTemplate.isFreightable();
	}
	
	/**
	 * Retrieves the skill cooldown time for this item.<br>
	 * This value is fetched from the {@code ItemTemplate}.
	 * @return The cooldown time as an {@code int}.
	 */
	public int useSkillDisTime()
	{
		return _itemTemplate.useSkillDisTime();
	}
	
	/**
	 * Retrieves the enchantment level of this item.<br>
	 * This method checks if the owner is in Olympiad mode.<br>
	 * If so, it caps the level based on {@code OlympiadConfig} limits.
	 * @return The effective enchantment level as an {@code int}.
	 */
	public int getOlyEnchantLevel()
	{
		final Player player = asPlayer();
		int enchant = _enchantLevel;
		
		if (player == null)
		{
			return enchant;
		}
		
		if (player.isInOlympiadMode())
		{
			if (_itemTemplate.isWeapon())
			{
				if ((OlympiadConfig.OLYMPIAD_WEAPON_ENCHANT_LIMIT >= 0) && (enchant > OlympiadConfig.OLYMPIAD_WEAPON_ENCHANT_LIMIT))
				{
					enchant = OlympiadConfig.OLYMPIAD_WEAPON_ENCHANT_LIMIT;
				}
			}
			else
			{
				if ((OlympiadConfig.OLYMPIAD_ARMOR_ENCHANT_LIMIT >= 0) && (enchant > OlympiadConfig.OLYMPIAD_ARMOR_ENCHANT_LIMIT))
				{
					enchant = OlympiadConfig.OLYMPIAD_ARMOR_ENCHANT_LIMIT;
				}
			}
		}
		
		return enchant;
	}
	
	/**
	 * Checks if the item provides passive skills.<br>
	 * This is true only for enchantment attribute runes located in an inventory.<br>
	 * The item must have a valid owner and contain normal skills.
	 * @return {@code true} if the item has passive skills, {@code false} otherwise.
	 */
	public boolean hasPassiveSkills()
	{
		final ItemType itemType = _itemTemplate.getItemType();
		return ((itemType == EtcItemType.ENCHT_ATTR_RUNE) || (itemType == EtcItemType.ENCHT_ATTRT_RUNE_SELECT)) && (_loc == ItemLocation.INVENTORY) && (_ownerId > 0) && (_itemTemplate.getSkills(ItemSkillType.NORMAL) != null);
	}
	
	/**
	 * Applies skills from this item to the owner.<br>
	 * This method checks if the item is equipped or has passive skills.<br>
	 * It then adds normal and enchantment passive skills to the {@code Player}.<br>
	 * If a skill already exists, it updates the level if necessary.
	 */
	public void giveSkillsToOwner()
	{
		if (!isEquipped() && !hasPassiveSkills())
		{
			return;
		}
		
		final Player player = asPlayer();
		if (player == null)
		{
			return;
		}
		
		_itemTemplate.forEachSkill(ItemSkillType.NORMAL, holder ->
		{
			final Skill skill = holder.getSkill();
			if (skill.isPassive())
			{
				final int skillId = skill.getId();
				if (player.isAffectedBySkill(skillId))
				{
					final int oldSkillLevel = player.getSkillLevel(skillId);
					if (oldSkillLevel < skill.getLevel())
					{
						player.removeSkill(skill, false); // Make sure previous skill effects exit properly.
						player.addSkill(skill, false);
					}
				}
				else
				{
					player.addSkill(skill, false);
				}
			}
		});
		
		_itemTemplate.forEachSkill(ItemSkillType.ON_ENCHANT, holder ->
		{
			final Skill skill = holder.getSkill();
			if (skill.isPassive() && (getEnchantLevel() >= holder.getValue()))
			{
				player.removeSkill(skill, false); // Make sure previous skill effects exit properly.
				player.addSkill(skill, false);
			}
		});
	}
	
	/**
	 * Removes skills from the owner of this item.<br>
	 * This method checks for passive skills provided by the item template.<br>
	 * It removes skills if they no longer meet the required conditions or levels.<br>
	 * Finally, it refreshes the inventory skills for the player.
	 */
	public void removeSkillsFromOwner()
	{
		if (!hasPassiveSkills())
		{
			return;
		}
		
		final Player player = asPlayer();
		if (player != null)
		{
			_itemTemplate.forEachSkill(ItemSkillType.NORMAL, holder ->
			{
				final Skill skill = holder.getSkill();
				if (skill.isPassive())
				{
					final int skillLevel = player.getSkillLevel(holder.getSkillId());
					if (((skillLevel > 0) && (skillLevel <= holder.getSkillLevel())) || (!skill.checkConditions(SkillConditionScope.PASSIVE, player, player)))
					{
						player.removeSkill(skill, false, skill.isPassive());
					}
				}
				else
				{
					player.removeSkill(skill, false, skill.isPassive());
				}
			});
			
			player.getInventory().applyItemSkills();
		}
	}
	
	/**
	 * Checks if the current object is an item.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the object is an item.
	 */
	@Override
	public boolean isItem()
	{
		return true;
	}
	
	/**
	 * Retrieves the {@code Player} who owns this item.<br>
	 * If the owner is not currently loaded, it attempts to fetch them using the {@code _ownerId}.
	 * @return the {@code Player} object owning the item, or {@code null} if no owner exists.
	 */
	@Override
	public Player asPlayer()
	{
		if ((_owner == null) && (_ownerId != 0))
		{
			_owner = World.getPlayer(_ownerId);
		}
		
		return _owner;
	}
	
	/**
	 * Retrieves the reuse delay for this item.<br>
	 * This value is fetched from the associated {@code ItemTemplate}.
	 * @return the reuse delay as an {@code int}.
	 */
	public int getEquipReuseDelay()
	{
		return _itemTemplate.getEquipReuseDelay();
	}
	
	/**
	 * Handles feedback when a player interacts with an item via a bypass command.<br>
	 * This method parses the {@code command} to identify specific scripts or events.<br>
	 * It triggers the appropriate event based on whether an event name is provided.
	 * @param player The {@code Player} who triggered the interaction.
	 * @param command The raw command string starting with "Script".
	 */
	public void onBypassFeedback(Player player, String command)
	{
		if (!command.startsWith("Script"))
		{
			return;
		}
		
		final String questName = command.substring(6);
		String event = null;
		final int idx = questName.indexOf(' ');
		if (idx > 0)
		{
			event = questName.substring(idx).trim();
		}
		
		if (event != null)
		{
			final ItemTemplate template = getTemplate();
			if (EventDispatcher.getInstance().hasListener(EventType.ON_ITEM_BYPASS_EVENT, template))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnItemBypassEvent(this, player, event), template);
			}
		}
		else
		{
			final ItemTemplate template = getTemplate();
			if (EventDispatcher.getInstance().hasListener(EventType.ON_ITEM_TALK, template))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnItemTalk(this, player), template);
			}
		}
	}
	
	/**
	 * Retrieves the enchantment options for this item.<br>
	 * It returns specific options if they are defined in the data.<br>
	 * If no specific options exist, it returns the default values.
	 * @return an {@code int[]} array containing the enchantment options.
	 */
	public int[] getEnchantOptions()
	{
		final EnchantOptions op = EnchantItemOptionsData.getInstance().getOptions(this);
		if (op != null)
		{
			return op.getOptions();
		}
		
		return DEFAULT_ENCHANT_OPTIONS;
	}
	
	/**
	 * Retrieves the list of special abilities for this item.<br>
	 * It filters out any {@code null} entries from the internal collection.
	 * @return A {@code Collection} of {@code EnsoulOption} objects.
	 */
	public Collection<EnsoulOption> getSpecialAbilities()
	{
		final List<EnsoulOption> result = new ArrayList<>();
		for (EnsoulOption ensoulOption : _ensoulOptions)
		{
			if (ensoulOption != null)
			{
				result.add(ensoulOption);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a specific special ability based on its position in the list.
	 * @param index The position of the ability to retrieve.
	 * @return The {@code EnsoulOption} at the specified index.
	 */
	public EnsoulOption getSpecialAbility(int index)
	{
		return _ensoulOptions[index];
	}
	
	/**
	 * Retrieves the list of special abilities from an ensoulment.<br>
	 * This method filters out any {@code null} values from the internal collection.
	 * @return A {@code Collection} of {@code EnsoulOption} objects.
	 */
	public Collection<EnsoulOption> getAdditionalSpecialAbilities()
	{
		final List<EnsoulOption> result = new ArrayList<>();
		for (EnsoulOption ensoulSpecialOption : _ensoulSpecialOptions)
		{
			if (ensoulSpecialOption != null)
			{
				result.add(ensoulSpecialOption);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a specific special ability from the ensoul options list.
	 * @param index The position of the option in the internal list.
	 * @return The {@code EnsoulOption} at the specified index.
	 */
	public EnsoulOption getAdditionalSpecialAbility(int index)
	{
		return _ensoulSpecialOptions[index];
	}
	
	/**
	 * Adds a special ability to the item at a specific position.<br>
	 * This method handles both regular and special types of abilities.<br>
	 * It replaces any existing option at the chosen position.<br>
	 * If {@code updateInDB} is {@code true}, it saves the changes to the database.
	 * @param option The {@code EnsoulOption} to add to the item.
	 * @param position The index of the slot where the ability should be placed.
	 * @param type The category of the ability, where {@code 1} is regular and {@code 2} is special.
	 * @param updateInDB Whether to persist the changes in the database immediately.
	 */
	public void addSpecialAbility(EnsoulOption option, int position, int type, boolean updateInDB)
	{
		if ((type == 1) && ((position < 0) || (position > 1))) // two first slots
		{
			return;
		}
		
		if ((type == 2) && (position != 0)) // third slot
		{
			return;
		}
		
		if (type == 1) // Adding regular ability
		{
			final EnsoulOption oldOption = _ensoulOptions[position];
			if (oldOption != null)
			{
				removeSpecialAbility(oldOption);
			}
			
			if (position < _itemTemplate.getEnsoulSlots())
			{
				_ensoulOptions[position] = option;
			}
		}
		else if (type == 2) // Adding special ability
		{
			final EnsoulOption oldOption = _ensoulSpecialOptions[position];
			if (oldOption != null)
			{
				removeSpecialAbility(oldOption);
			}
			
			if (position < _itemTemplate.getSpecialEnsoulSlots())
			{
				_ensoulSpecialOptions[position] = option;
			}
		}
		
		if (updateInDB)
		{
			updateSpecialAbilities();
		}
	}
	
	/**
	 * Removes a special ability from the item based on its position and type.<br>
	 * This method handles different logic depending on whether the {@code type} is 1 or 2.<br>
	 * It also manages internal array cleanup for ensoul options.
	 * @param position The index of the option to remove.
	 * @param type The category of the special ability.
	 */
	public void removeSpecialAbility(int position, int type)
	{
		if (type == 1)
		{
			final EnsoulOption option = _ensoulOptions[position];
			if (option != null)
			{
				removeSpecialAbility(option);
				_ensoulOptions[position] = null;
				
				// Rearrange.
				if (position == 0)
				{
					final EnsoulOption secondEnsoul = _ensoulOptions[1];
					if (secondEnsoul != null)
					{
						removeSpecialAbility(secondEnsoul);
						_ensoulOptions[1] = null;
						addSpecialAbility(secondEnsoul, 0, type, true);
					}
				}
			}
		}
		else if (type == 2)
		{
			final EnsoulOption option = _ensoulSpecialOptions[position];
			if (option != null)
			{
				removeSpecialAbility(option);
				_ensoulSpecialOptions[position] = null;
			}
		}
	}
	
	/**
	 * Removes all special abilities from the item.<br>
	 * This method iterates through both standard and special ensoul options.<br>
	 * It calls {@code clearSpecialAbility} for each option found.
	 */
	public void clearSpecialAbilities()
	{
		for (EnsoulOption ensoulOption : _ensoulOptions)
		{
			clearSpecialAbility(ensoulOption);
		}
		
		for (EnsoulOption ensoulSpecialOption : _ensoulSpecialOptions)
		{
			clearSpecialAbility(ensoulSpecialOption);
		}
	}
	
	/**
	 * Checks if the item is currently equipped.<br>
	 * Iterates through all {@code EnsoulOption} and {@code EnsoulSpecialOption} objects.<br>
	 * Executes the {@code applySpecialAbility} method for each option found.
	 */
	public void applySpecialAbilities()
	{
		if (!isEquipped())
		{
			return;
		}
		
		for (EnsoulOption ensoulOption : _ensoulOptions)
		{
			applySpecialAbility(ensoulOption);
		}
		
		for (EnsoulOption ensoulSpecialOption : _ensoulSpecialOptions)
		{
			applySpecialAbility(ensoulSpecialOption);
		}
	}
	
	/**
	 * Removes a specific special ability from the item database.<br>
	 * This method also removes the associated skill from the player if one exists.<br>
	 * It handles potential errors by logging a warning message.
	 * @param option The {@code EnsoulOption} containing the ability to remove.
	 */
	private void removeSpecialAbility(EnsoulOption option)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM item_special_abilities WHERE objectId = ? AND optionId = ?"))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, option.getId());
			ps.execute();
			
			final Skill skill = option.getSkill();
			if (skill != null)
			{
				final Player player = asPlayer();
				if (player != null)
				{
					player.removeSkill(skill.getId());
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Item could not remove special ability for " + this, e);
		}
	}
	
	/**
	 * Applies a special ability to the player based on the provided option.<br>
	 * This method checks if the {@code EnsoulOption} contains a skill.<br>
	 * If it does, it adds that skill to the player if they do not already have the correct level.
	 * @param option The {@code EnsoulOption} containing the ability data.
	 */
	private void applySpecialAbility(EnsoulOption option)
	{
		if (option == null)
		{
			return;
		}
		
		final Skill skill = option.getSkill();
		if (skill != null)
		{
			final Player player = asPlayer();
			if ((player != null) && (player.getSkillLevel(skill.getId()) != skill.getLevel()))
			{
				player.addSkill(skill, false);
			}
		}
	}
	
	/**
	 * Removes a special ability from the player associated with this item.<br>
	 * This method checks if the {@code EnsoulOption} contains a skill.<br>
	 * If a skill exists, it is removed from the player's skill list.
	 * @param option The {@code EnsoulOption} containing the skill to be cleared.
	 */
	private void clearSpecialAbility(EnsoulOption option)
	{
		if (option == null)
		{
			return;
		}
		
		final Skill skill = option.getSkill();
		if (skill != null)
		{
			final Player player = asPlayer();
			if (player != null)
			{
				player.removeSkill(skill, false, true);
			}
		}
	}
	
	/**
	 * This method loads special abilities from the database.<br>
	 * It retrieves data based on the current {@code objectId}.<br>
	 * Each valid option is added to the item using {@code addSpecialAbility}.
	 */
	private void restoreSpecialAbilities()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM item_special_abilities WHERE objectId = ? ORDER BY position"))
		{
			ps.setInt(1, getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int optionId = rs.getInt("optionId");
					final int type = rs.getInt("type");
					final int position = rs.getInt("position");
					final EnsoulOption option = EnsoulData.getInstance().getOption(optionId);
					if (option != null)
					{
						addSpecialAbility(option, position, type, false);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Item could not restore special abilities for " + this, e);
		}
	}
	
	/**
	 * Updates the special abilities of this item in the database.<br>
	 * This method handles the persistence of unique item traits.
	 */
	public void updateSpecialAbilities()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			updateSpecialAbilities(con);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Item could not update item special abilities", e);
		}
	}
	
	/**
	 * Updates the special abilities for this item in the database.<br>
	 * It inserts or updates both regular and special ensoul options.<br>
	 * The method handles {@code SQLException} by logging a warning.
	 * @param con The {@code Connection} object used to execute the SQL query.
	 */
	private void updateSpecialAbilities(Connection con)
	{
		try (PreparedStatement ps = con.prepareStatement("INSERT INTO item_special_abilities (`objectId`, `type`, `optionId`, `position`) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE type = ?, optionId = ?, position = ?"))
		{
			final int objectId = getObjectId();
			ps.setInt(1, objectId);
			for (int i = 0; i < _ensoulOptions.length; i++)
			{
				if (_ensoulOptions[i] == null)
				{
					continue;
				}
				
				ps.setInt(2, 1); // regular options
				final int optionId = _ensoulOptions[i].getId();
				ps.setInt(3, optionId);
				ps.setInt(4, i);
				
				ps.setInt(5, 1); // regular options
				ps.setInt(6, optionId);
				ps.setInt(7, i);
				ps.execute();
			}
			
			for (int i = 0; i < _ensoulSpecialOptions.length; i++)
			{
				if (_ensoulSpecialOptions[i] == null)
				{
					continue;
				}
				
				ps.setInt(2, 2); // special options
				final int optionId = _ensoulSpecialOptions[i].getId();
				ps.setInt(3, optionId);
				ps.setInt(4, i);
				
				ps.setInt(5, 2); // special options
				ps.setInt(6, optionId);
				ps.setInt(7, i);
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Item could not update item special abilities", e);
		}
	}
	
	/**
	 * Removes all enchantment options from the item.<br>
	 * If the owner is not a {@code Player}, it clears the internal list immediately.<br>
	 * Otherwise, it removes each option from the player before clearing the list.
	 */
	public void clearEnchantStats()
	{
		final Player player = asPlayer();
		if (player == null)
		{
			_enchantOptions.clear();
			return;
		}
		
		for (Options op : _enchantOptions)
		{
			op.remove(player);
		}
		
		_enchantOptions.clear();
	}
	
	/**
	 * Applies the enchantment statistics of this item to the owner.<br>
	 * This method checks if the item is equipped and has valid enchant options.<br>
	 * It iterates through all active enchant IDs and applies their effects to the {@code Player}.
	 */
	public void applyEnchantStats()
	{
		final Player player = asPlayer();
		if (!isEquipped() || (player == null) || (getEnchantOptions() == DEFAULT_ENCHANT_OPTIONS))
		{
			return;
		}
		
		for (int id : getEnchantOptions())
		{
			final Options options = OptionData.getInstance().getOptions(id);
			if (options != null)
			{
				options.apply(player);
				_enchantOptions.add(options);
			}
			else if (id != 0)
			{
				LOGGER.info("Item applyEnchantStats could not find option " + id + " " + this + " " + player);
			}
		}
	}
	
	/**
	 * Sets the heading value for this item.<br>
	 * This updates the internal {@code heading} property.
	 * @param heading The new heading value to assign.
	 */
	@Override
	public void setHeading(int heading)
	{
	}
	
	/**
	 * Stops all active tasks associated with this item.<br>
	 * This method removes the item from {@code ItemLifeTimeTaskManager}.<br>
	 * It also removes the item from {@code ItemAppearanceTaskManager}.
	 */
	public void stopAllTasks()
	{
		ItemLifeTimeTaskManager.getInstance().remove(this);
		ItemAppearanceTaskManager.getInstance().remove(this);
	}
	
	/**
	 * Retrieves the variables associated with this item.<br>
	 * If no variables exist, a new {@code ItemVariables} object is created and added.
	 * @return the {@code ItemVariables} instance for this item.
	 */
	public ItemVariables getVariables()
	{
		final ItemVariables vars = getScript(ItemVariables.class);
		return vars != null ? vars : addScript(new ItemVariables(getObjectId()));
	}
	
	/**
	 * Retrieves the unique visual identifier for this item.<br>
	 * This method checks if the item has a specific appearance based on player race.<br>
	 * It returns 0 if the visual requirements are not met.
	 * @return The visual ID of the item as an {@code int}.
	 */
	public int getVisualId()
	{
		final int visualId = getVariables().getInt(ItemVariables.VISUAL_ID, 0);
		if (visualId > 0)
		{
			final int appearanceStoneId = getVariables().getInt(ItemVariables.VISUAL_APPEARANCE_STONE_ID, 0);
			if (appearanceStoneId > 0)
			{
				final AppearanceStone stone = AppearanceItemData.getInstance().getStone(appearanceStoneId);
				if (stone != null)
				{
					final Player player = asPlayer();
					if (player != null)
					{
						final Race playerRace = player.getRace();
						if (!stone.getRaces().isEmpty() && !stone.getRaces().contains(playerRace))
						{
							return 0;
						}
						
						if (!stone.getRacesNot().isEmpty() && stone.getRacesNot().contains(playerRace))
						{
							return 0;
						}
					}
				}
			}
		}
		
		return visualId;
	}
	
	/**
	 * Sets the unique visual identifier for this item.<br>
	 * If {@code 0} is provided, it removes any active visual lifetime tasks.
	 * @param visualId The new visual ID to assign.
	 */
	public void setVisualId(int visualId)
	{
		getVariables().set(ItemVariables.VISUAL_ID, visualId);
		
		// When removed, cancel existing lifetime task.
		if (visualId == 0)
		{
			ItemAppearanceTaskManager.getInstance().remove(this);
			onVisualLifeTimeEnd();
		}
	}
	
	/**
	 * Retrieves the unique identifier for the visual appearance stone.<br>
	 * This value is stored in the item variables.<br>
	 * It returns {@code 0} if no specific ID is set.
	 * @return The integer ID of the appearance stone.
	 */
	public int getAppearanceStoneId()
	{
		return getVariables().getInt(ItemVariables.VISUAL_APPEARANCE_STONE_ID, 0);
	}
	
	/**
	 * Retrieves the remaining visual lifetime of the item.<br>
	 * This value is used to determine how long a visual effect or appearance lasts.
	 * @return The visual lifetime as a {@code long}.
	 */
	public long getVisualLifeTime()
	{
		return getVariables().getLong(ItemVariables.VISUAL_APPEARANCE_LIFE_TIME, 0);
	}
	
	/**
	 * Schedules the visual lifetime for this item.<br>
	 * This method removes the item from the current task manager.<br>
	 * It checks if the remaining visual life is greater than {@code 0}.<br>
	 * If time remains, it adds the item back to the task manager with its end time.<br>
	 * Otherwise, it triggers the {@code onVisualLifeTimeEnd} event.
	 */
	public void scheduleVisualLifeTime()
	{
		ItemAppearanceTaskManager.getInstance().remove(this);
		final long visualLifeTime = getVisualLifeTime();
		if (visualLifeTime > 0)
		{
			if ((visualLifeTime - System.currentTimeMillis()) > 0)
			{
				ItemAppearanceTaskManager.getInstance().add(this, visualLifeTime);
			}
			else
			{
				onVisualLifeTimeEnd();
			}
		}
	}
	
	/**
	 * Handles the logic when a visual effect's lifetime expires.<br>
	 * It removes temporary visual variables and saves the item state.<br>
	 * If the owner is a {@code Player}, it updates their appearance and sends a notification.
	 */
	public void onVisualLifeTimeEnd()
	{
		removeVisualSetSkills();
		
		final ItemVariables vars = getVariables();
		vars.remove(ItemVariables.VISUAL_ID);
		vars.remove(ItemVariables.VISUAL_APPEARANCE_STONE_ID);
		vars.remove(ItemVariables.VISUAL_APPEARANCE_LIFE_TIME);
		vars.storeMe();
		
		final Player player = asPlayer();
		if (player != null)
		{
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(this);
			player.broadcastUserInfo(UserInfoType.APPAREANCE);
			player.sendInventoryUpdate(iu);
			
			if (isEnchanted())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.S1_S2_THE_ITEM_S_TEMPORARY_APPEARANCE_HAS_BEEN_RESET).addInt(_enchantLevel).addItemName(this));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.S1_THE_ITEM_S_TEMPORARY_APPEARANCE_HAS_BEEN_RESET).addItemName(this));
			}
		}
	}
	
	/**
	 * Checks if the item is a virtual object.<br>
	 * Virtual items do not have a physical location in the game world.
	 * @return {@code true} if the item is virtual, {@code false} otherwise.
	 */
	public boolean isVirtual()
	{
		return _isVirtual;
	}
	
	/**
	 * Sets whether the item is considered a virtual object.<br>
	 * If {@code false}, it removes the virtual flag from the item variables.<br>
	 * If {@code true}, it sets the virtual flag to {@code true}.
	 * @param virtual The virtual status to apply.
	 */
	public void setVirtual(boolean virtual)
	{
		_isVirtual = virtual;
		
		final ItemVariables variables = getVariables();
		if (!virtual)
		{
			variables.remove(ItemVariables.VIRTUAL);
		}
		else
		{
			variables.set(ItemVariables.VIRTUAL, true);
		}
		
		variables.storeMe();
	}
	
	public boolean isDamaged()
	{
		return _damaged;
	}
	
	public void setDamaged(boolean damaged)
	{
		_damaged = damaged;
		
		final ItemVariables vars = getVariables();
		if (!damaged)
		{
			vars.remove(ItemVariables.DAMAGED);
		}
		else
		{
			vars.set(ItemVariables.DAMAGED, true);
		}
		
		vars.storeMe();
	}
	
	/**
	 * Removes visual set skills from the player if an armor set is incomplete.<br>
	 * This method checks if the equipped item belongs to a fixed appearance stone.<br>
	 * It verifies if the current number of pieces meets the minimum requirement.<br>
	 * If the requirement is not met, it removes the associated skills from the {@code Player}.
	 */
	public void removeVisualSetSkills()
	{
		if (!isEquipped())
		{
			return;
		}
		
		final int appearanceStoneId = getAppearanceStoneId();
		if (appearanceStoneId > 0)
		{
			final AppearanceStone stone = AppearanceItemData.getInstance().getStone(appearanceStoneId);
			if ((stone != null) && (stone.getType() == AppearanceType.FIXED))
			{
				final Player player = asPlayer();
				if (player != null)
				{
					boolean update = false;
					for (ArmorSet armorSet : ArmorSetData.getInstance().getSets(stone.getVisualId()))
					{
						if ((armorSet.getPieceCount(player, Item::getVisualId) - 1 /* not removed yet */) < armorSet.getMinimumPieces())
						{
							for (ArmorsetSkillHolder holder : armorSet.getSkills())
							{
								final Skill skill = holder.getSkill();
								if (skill != null)
								{
									player.removeSkill(skill, false, skill.isPassive());
									update = true;
								}
							}
						}
					}
					
					if (update)
					{
						player.sendSkillList();
					}
				}
			}
		}
	}
	
	/**
	 * Applies visual set skills to the player.<br>
	 * This method checks if the equipped item belongs to a valid armor set.<br>
	 * It grants skills to the player if they meet the required piece count and level requirements.<br>
	 * Active skills are updated with appropriate reuse delays and timestamps.
	 */
	public void applyVisualSetSkills()
	{
		if (!isEquipped())
		{
			return;
		}
		
		final int appearanceStoneId = getAppearanceStoneId();
		if (appearanceStoneId > 0)
		{
			final AppearanceStone stone = AppearanceItemData.getInstance().getStone(appearanceStoneId);
			if ((stone != null) && (stone.getType() == AppearanceType.FIXED))
			{
				final Player player = asPlayer();
				if (player != null)
				{
					boolean update = false;
					boolean updateTimeStamp = false;
					for (ArmorSet armorSet : ArmorSetData.getInstance().getSets(stone.getVisualId()))
					{
						if (armorSet.getPieceCount(player, Item::getVisualId) >= armorSet.getMinimumPieces())
						{
							for (ArmorsetSkillHolder holder : armorSet.getSkills())
							{
								if (player.getSkillLevel(holder.getSkillId()) >= holder.getSkillLevel())
								{
									continue;
								}
								
								final Skill skill = holder.getSkill();
								if ((skill == null) || (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, player, player)))
								{
									continue;
								}
								
								player.addSkill(skill, false);
								update = true;
								
								if (skill.isActive())
								{
									if (!player.hasSkillReuse(skill.getReuseHashCode()))
									{
										final int equipDelay = getEquipReuseDelay();
										if (equipDelay > 0)
										{
											player.addTimeStamp(skill, equipDelay);
											player.disableSkill(skill, equipDelay);
										}
									}
									
									// Active, non offensive, skills start with reuse on equip.
									if (!skill.hasNegativeEffect() && !skill.isTransformation() && (PlayerConfig.ARMOR_SET_EQUIP_ACTIVE_SKILL_REUSE > 0) && player.hasEnteredWorld())
									{
										final int reuseDelay = skill.getReuseDelay();
										player.addTimeStamp(skill, reuseDelay > 0 ? reuseDelay : PlayerConfig.ARMOR_SET_EQUIP_ACTIVE_SKILL_REUSE);
									}
									
									updateTimeStamp = true;
								}
							}
						}
					}
					
					if (updateTimeStamp)
					{
						player.sendPacket(new SkillCoolTime(player));
					}
					
					if (update)
					{
						player.sendSkillList();
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the unique identifier for a transmog item.<br>
	 * This method checks if transmogs are enabled in {@code TransmogConfig}.<br>
	 * If disabled, it returns {@code 0}.<br>
	 * Otherwise, it returns the value stored under {@code ItemVariables.TRANSMOG_ID}.
	 * @return The transmog ID as an {@code int}.
	 */
	public int getTransmogId()
	{
		if (!TransmogConfig.ENABLE_TRANSMOG)
		{
			return 0;
		}
		
		return getVariables().getInt(ItemVariables.TRANSMOG_ID, 0);
	}
	
	/**
	 * Sets the unique identifier for a transmog effect.<br>
	 * This method updates the {@code TRANSMOG_ID} variable and saves it to the database.
	 * @param transmogId The new ID to assign to this item.
	 */
	public void setTransmogId(int transmogId)
	{
		final ItemVariables variables = getVariables();
		variables.set(ItemVariables.TRANSMOG_ID, transmogId);
		variables.storeMe();
	}
	
	/**
	 * Removes the transmog identifier from this item.<br>
	 * This method updates the internal variables and saves the changes to the database.
	 */
	public void removeTransmog()
	{
		getVariables().remove(ItemVariables.TRANSMOG_ID);
		getVariables().storeMe();
	}
	
	/**
	 * Returns a string representation of the item.<br>
	 * This includes the template name and the unique object ID.
	 * @return A string containing the item details.
	 */
	@Override
	public String toString()
	{
		return StringUtil.concat(_itemTemplate.toString(), "[", String.valueOf(getObjectId()), "]");
	}
}
