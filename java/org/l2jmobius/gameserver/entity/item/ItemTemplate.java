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
package org.l2jmobius.gameserver.entity.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.CustomDepositableItemsConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.item.enchant.attribute.AttributeHolder;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemGrade;
import org.l2jmobius.gameserver.entity.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.entity.item.holders.ExtractableProduct;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.entity.item.type.ActionType;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.entity.item.type.MaterialType;
import org.l2jmobius.gameserver.mechanics.commission.CommissionItemType;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncAdd;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncSet;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncTemplate;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class contains all information concerning an item such as weapons or armor.<br>
 * It serves as the base class for {@code Armor}, {@code EtcItem}, and {@code Weapon}.
 */
public abstract class ItemTemplate extends ListenersContainer
{
	protected static final Logger LOGGER = Logger.getLogger(ItemTemplate.class.getName());
	
	public static final int TYPE1_WEAPON_RING_EARRING_NECKLACE = 0;
	public static final int TYPE1_SHIELD_ARMOR = 1;
	public static final int TYPE1_ITEM_QUESTITEM_ADENA = 4;
	
	public static final int TYPE2_WEAPON = 0;
	public static final int TYPE2_SHIELD_ARMOR = 1;
	public static final int TYPE2_ACCESSORY = 2;
	public static final int TYPE2_QUEST = 3;
	public static final int TYPE2_MONEY = 4;
	public static final int TYPE2_OTHER = 5;
	
	private int _itemId;
	private int _displayId;
	private String _name;
	private String _additionalName;
	private String _icon;
	private int _weight;
	private boolean _stackable;
	private MaterialType _materialType;
	private CrystalType _crystalType;
	private int _equipReuseDelay;
	private int _duration;
	private long _time;
	private int _autoDestroyTime;
	private BodyPart _bodyPart;
	private long _referencePrice;
	private int _crystalCount;
	private boolean _sellable;
	private boolean _dropable;
	private boolean _destroyable;
	private boolean _tradeable;
	private boolean _depositable;
	private boolean _enchantable;
	private int _enchantLimit;
	private int _ensoulNormalSlots;
	private int _ensoulSpecialSlots;
	private boolean _elementable;
	private boolean _questUsableItem;
	private boolean _questItem;
	private boolean _freightable;
	private boolean _allowSelfResurrection;
	private boolean _isOlyRestricted;
	private boolean _isEventRestricted;
	private boolean _forNpc;
	private boolean _common;
	private boolean _heroItem;
	private boolean _pvpItem;
	private boolean _immediateEffect;
	private boolean _exImmediateEffect;
	private int _defaultEnchantLevel;
	private ActionType _defaultAction;
	
	protected int _type1; // Needed for item list (inventory).
	protected int _type2; // Different lists for armor, weapon, etc.
	private Map<AttributeType, AttributeHolder> _elementals = null;
	protected Map<Stat, FuncTemplate> _funcTemplates;
	protected List<Condition> _preConditions;
	private List<ItemSkillHolder> _skills;
	
	private int _useSkillDisTime;
	protected int _reuseDelay;
	private int _sharedReuseGroup;
	
	private CommissionItemType _commissionItemType;
	
	private boolean _isAppearanceable;
	private boolean _isBlessed;
	
	private int _artifactSlot;
	
	/**
	 * Creates a new {@code ItemTemplate} using the provided statistics.<br>
	 * This constructor initializes the template with a specific {@code StatSet}.
	 * @param set The {@code StatSet} containing the item's attributes.
	 */
	protected ItemTemplate(StatSet set)
	{
		set(set);
	}
	
	/**
	 * Initializes the item properties from a {@code StatSet}.<br>
	 * This method populates all internal fields like IDs, names, and weights.<br>
	 * It also handles specific logic for quest items and depositable status.
	 * @param set The {@code StatSet} containing the data to apply.
	 */
	public void set(StatSet set)
	{
		_itemId = set.getInt("item_id");
		_displayId = set.getInt("displayId", _itemId);
		_name = set.getString("name");
		_additionalName = set.getString("additionalName", null);
		_icon = set.getString("icon", null);
		_weight = set.getInt("weight", 0);
		_materialType = set.getEnum("material", MaterialType.class, MaterialType.STEEL);
		_equipReuseDelay = set.getInt("equip_reuse_delay", 0) * 1000;
		_duration = set.getInt("duration", -1);
		_time = set.getInt("time", -1);
		_autoDestroyTime = set.getInt("auto_destroy_time", -1) * 1000;
		_bodyPart = BodyPart.fromName(set.getString("bodypart", "none"));
		_referencePrice = set.getLong("price", 0);
		_crystalType = set.getEnum("crystal_type", CrystalType.class, CrystalType.NONE);
		_crystalCount = set.getInt("crystal_count", 0);
		_stackable = set.getBoolean("is_stackable", false);
		_sellable = set.getBoolean("is_sellable", true);
		_dropable = set.getBoolean("is_dropable", true);
		_destroyable = set.getBoolean("is_destroyable", true);
		_tradeable = set.getBoolean("is_tradable", true);
		_questUsableItem = set.getBoolean("is_questusable", false);
		_questItem = _questUsableItem || set.getBoolean("is_questitem", false);
		
		if (CustomDepositableItemsConfig.CUSTOM_DEPOSITABLE_ENABLED)
		{
			_depositable = !_questItem || CustomDepositableItemsConfig.CUSTOM_DEPOSITABLE_QUEST_ITEMS;
		}
		else
		{
			_depositable = set.getBoolean("is_depositable", true);
		}
		
		_ensoulNormalSlots = set.getInt("ensoulNormalSlots", 0);
		_ensoulSpecialSlots = set.getInt("ensoulSpecialSlots", 0);
		
		_elementable = set.getBoolean("element_enabled", false);
		_enchantable = set.getBoolean("enchant_enabled", false);
		_enchantLimit = set.getInt("enchant_limit", 0);
		_freightable = set.getBoolean("is_freightable", false);
		_allowSelfResurrection = set.getBoolean("allow_self_resurrection", false);
		_isOlyRestricted = set.getBoolean("is_oly_restricted", false);
		_isEventRestricted = set.getBoolean("is_event_restricted", false);
		_forNpc = set.getBoolean("for_npc", false);
		_isAppearanceable = set.getBoolean("isAppearanceable", false);
		_isBlessed = set.getBoolean("blessed", false);
		_artifactSlot = set.getInt("artifactSlot", 0);
		_immediateEffect = set.getBoolean("immediate_effect", false);
		_exImmediateEffect = set.getBoolean("ex_immediate_effect", false);
		_defaultAction = set.getEnum("default_action", ActionType.class, ActionType.NONE);
		_useSkillDisTime = set.getInt("useSkillDisTime", 0);
		_defaultEnchantLevel = set.getInt("enchanted", 0);
		_reuseDelay = set.getInt("reuse_delay", 0);
		_sharedReuseGroup = set.getInt("shared_reuse_group", 0);
		_commissionItemType = set.getEnum("commissionItemType", CommissionItemType.class, CommissionItemType.OTHER_ITEM);
		_common = (((_itemId >= 11605) && (_itemId <= 12361)) || ((_itemId >= 20639) && (_itemId <= 20653)));
		_heroItem = ((_itemId >= 6611) && (_itemId <= 6621)) || ((_itemId >= 9388) && (_itemId <= 9390)) || (_itemId == 6842);
		_pvpItem = ((_itemId >= 10667) && (_itemId <= 10835)) || ((_itemId >= 12852) && (_itemId <= 12977)) || ((_itemId >= 14363) && (_itemId <= 14525)) || (_itemId == 14528) || (_itemId == 14529) || (_itemId == 14558) || ((_itemId >= 15913) && (_itemId <= 16024)) || ((_itemId >= 16134) && (_itemId <= 16147)) || (_itemId == 16149) || (_itemId == 16151) || (_itemId == 16153) || (_itemId == 16155) || (_itemId == 16157) || (_itemId == 16159) || ((_itemId >= 16168) && (_itemId <= 16176)) || ((_itemId >= 16179) && (_itemId <= 16220)) || ((_itemId >= 21932) && (_itemId <= 21934)) || (_itemId == 21938) || ((_itemId >= 21952) && (_itemId <= 21954)) || (_itemId == 21958) || ((_itemId >= 21970) && (_itemId <= 21972));
	}
	
	/**
	 * Returns the itemType.
	 * @return Enum
	 */
	public abstract ItemType getItemType();
	
	/**
	 * Checks if the item is an {@code Etc} type.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the item is an {@code Etc} item, otherwise {@code false}.
	 */
	public boolean isEtcItem()
	{
		return false;
	}
	
	/**
	 * Checks if the item is an armor type.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the item is armor, otherwise {@code false}.
	 */
	public boolean isArmor()
	{
		return false;
	}
	
	/**
	 * Checks if the item is a weapon.<br>
	 * This method returns {@code false} for all items currently.
	 * @return {@code true} if the item is a weapon, otherwise {@code false}.
	 */
	public boolean isWeapon()
	{
		return false;
	}
	
	/**
	 * Checks if the item is classified as a magic weapon.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the item is a magic weapon, otherwise {@code false}.
	 */
	public boolean isMagicWeapon()
	{
		return false;
	}
	
	/**
	 * Retrieves the reuse delay for this equipment.<br>
	 * This value determines how long a player must wait before reusing the item.
	 * @return The reuse delay as an {@code int}.
	 */
	public int getEquipReuseDelay()
	{
		return _equipReuseDelay;
	}
	
	/**
	 * Retrieves the total duration of the item.<br>
	 * This value represents how long the effect lasts in seconds.
	 * @return The duration as an {@code int}.
	 */
	public int getDuration()
	{
		return _duration;
	}
	
	/**
	 * Retrieves the current time value of the item.<br>
	 * This value is stored in the internal {@code _time} field.
	 * @return The current time as a {@code long}.
	 */
	public long getTime()
	{
		return _time;
	}
	
	/**
	 * Returns the time in seconds before an item automatically destroys.<br>
	 * This value determines how long the item remains active.
	 * @return The auto destroy time as an {@code int}.
	 */
	public int getAutoDestroyTime()
	{
		return _autoDestroyTime;
	}
	
	/**
	 * Retrieves the unique identifier for this item.<br>
	 * This value corresponds to the internal ID used by the system.
	 * @return The {@code int} ID of the item.
	 */
	public int getId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the unique identifier used for display purposes.<br>
	 * This ID is different from the internal database ID.
	 * @return the {@code int} value of the display ID.
	 */
	public int getDisplayId()
	{
		return _displayId;
	}
	
	public abstract int getItemMask();
	
	/**
	 * Retrieves the material type of this item.<br>
	 * This value determines how the item behaves as a crafting material.
	 * @return the {@code MaterialType} of the item.
	 */
	public MaterialType getMaterialType()
	{
		return _materialType;
	}
	
	/**
	 * Retrieves the secondary type of the item.<br>
	 * This value is used to categorize items into specific groups.
	 * @return the {@code int} value representing the second item type.
	 */
	public int getType2()
	{
		return _type2;
	}
	
	/**
	 * Retrieves the weight of the item.<br>
	 * This value represents how heavy the item is in the game world.
	 * @return The weight as an {@code int}.
	 */
	public int getWeight()
	{
		return _weight;
	}
	
	/**
	 * Checks if the item can be used to create crystals.<br>
	 * This returns {@code true} only if a valid {@code CrystalType} is assigned.<br>
	 * It also requires the {@code crystalCount} to be greater than {@code 0}.
	 * @return {@code true} if the item is crystallizable, {@code false} otherwise.
	 */
	public boolean isCrystallizable()
	{
		return (_crystalType != CrystalType.NONE) && (_crystalCount > 0);
	}
	
	/**
	 * Retrieves the grade of the item.<br>
	 * This method returns the {@code ItemGrade} based on the internal crystal type.
	 * @return the current {@code ItemGrade} of this item.
	 */
	public ItemGrade getItemGrade()
	{
		return ItemGrade.valueOf(_crystalType);
	}
	
	/**
	 * Retrieves the crystal type associated with this item.
	 * @return the {@code CrystalType} of the item.
	 */
	public CrystalType getCrystalType()
	{
		return _crystalType;
	}
	
	/**
	 * Retrieves the unique identifier for the crystal type.<br>
	 * This value is obtained from the {@code CrystalType} associated with this item.
	 * @return The {@code int} ID of the crystal.
	 */
	public int getCrystalItemId()
	{
		return _crystalType.getCrystalId();
	}
	
	/**
	 * Retrieves the simplified crystal type for this item.<br>
	 * It maps specific high-level types like {@code S80} or {@code R95} to their base categories.
	 * @return The simplified {@code CrystalType}.
	 */
	public CrystalType getCrystalTypePlus()
	{
		switch (_crystalType)
		{
			case S80:
			case S84:
			{
				return CrystalType.S;
			}
			case R95:
			case R99:
			case R110:
			{
				return CrystalType.R;
			}
			case L:
			{
				return CrystalType.L;
			}
			default:
			{
				return _crystalType;
			}
		}
	}
	
	/**
	 * Retrieves the total number of crystals associated with this item.
	 * @return the current {@code crystalCount} as an {@code int}.
	 */
	public int getCrystalCount()
	{
		return _crystalCount;
	}
	
	/**
	 * Calculates the total crystal count for an item based on its enchantment level.<br>
	 * This method applies specific bonuses depending on the item type and {@code enchantLevel}.<br>
	 * It returns the base {@code _crystalCount} plus any applicable bonuses.
	 * @param enchantLevel The current enchantment level of the item.
	 * @return The total calculated crystal count as an {@code int}.
	 */
	public int getCrystalCount(int enchantLevel)
	{
		if (enchantLevel > 3)
		{
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
				{
					return _crystalCount + (_crystalType.getCrystalEnchantBonusArmor() * ((3 * enchantLevel) - 6));
				}
				case TYPE2_WEAPON:
				{
					return _crystalCount + (_crystalType.getCrystalEnchantBonusWeapon() * ((2 * enchantLevel) - 3));
				}
				default:
				{
					return _crystalCount;
				}
			}
		}
		else if (enchantLevel > 0)
		{
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
				{
					return _crystalCount + (_crystalType.getCrystalEnchantBonusArmor() * enchantLevel);
				}
				case TYPE2_WEAPON:
				{
					return _crystalCount + (_crystalType.getCrystalEnchantBonusWeapon() * enchantLevel);
				}
				default:
				{
					return _crystalCount;
				}
			}
		}
		else
		{
			return _crystalCount;
		}
	}
	
	/**
	 * Retrieves the display name of the item.
	 * @return The {@code String} name of the item.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the secondary name of the item.<br>
	 * This value is used to display an extra name in the game UI.
	 * @return The additional name as a {@code String}.
	 */
	public String getAdditionalName()
	{
		return _additionalName;
	}
	
	/**
	 * Retrieves the collection of attributes for this item.<br>
	 * Returns {@code null} if no attributes are present.
	 * @return a {@code Collection} of {@code AttributeHolder} objects or {@code null}.
	 */
	public Collection<AttributeHolder> getAttributes()
	{
		return _elementals != null ? _elementals.values() : null;
	}
	
	/**
	 * Retrieves the {@code AttributeHolder} for a specific attribute type.<br>
	 * This method searches the internal map of attributes.<br>
	 * It returns {@code null} if no attributes are defined or if the type is missing.
	 * @param type The {@code AttributeType} to look up.
	 * @return The corresponding {@code AttributeHolder}, or {@code null} if not found.
	 */
	public AttributeHolder getAttribute(AttributeType type)
	{
		return _elementals != null ? _elementals.get(type) : null;
	}
	
	/**
	 * Sets the attributes for this item.<br>
	 * This method updates an existing attribute or adds a new one to the internal collection.
	 * @param holder The {@code AttributeHolder} containing the type and value to set.
	 */
	public void setAttributes(AttributeHolder holder)
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
	}
	
	/**
	 * Retrieves the body part associated with this item.<br>
	 * This indicates where on the character the item is equipped.
	 * @return the {@code BodyPart} of the item.
	 */
	public BodyPart getBodyPart()
	{
		return _bodyPart;
	}
	
	/**
	 * Retrieves the primary type identifier for this item.<br>
	 * This value corresponds to the {@code type1} field in the database.
	 * @return The integer value of the item's primary type.
	 */
	public int getType1()
	{
		return _type1;
	}
	
	/**
	 * Checks if this item can be stacked.<br>
	 * Returns {@code true} if multiple units of the item can occupy a single slot.<br>
	 * Returns {@code false} if each unit must occupy its own slot.
	 * @return the stackable status of the item.
	 */
	public boolean isStackable()
	{
		return _stackable;
	}
	
	/**
	 * Checks if the item can be equipped by a character.<br>
	 * This returns {@code true} if the item has a valid body part and is not an {@code EtcItemType}.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if the item is equipable, {@code false} otherwise.
	 */
	public boolean isEquipable()
	{
		return (_bodyPart != BodyPart.NONE) && !(getItemType() instanceof EtcItemType);
	}
	
	/**
	 * Retrieves the base reference price of the item.<br>
	 * This value is used as a standard for calculating other costs.
	 * @return The {@code long} value representing the reference price.
	 */
	public long getReferencePrice()
	{
		return _referencePrice;
	}
	
	/**
	 * Checks if the item can be sold.<br>
	 * Returns {@code true} if it is sellable, and {@code false} otherwise.
	 * @return a boolean value indicating sellability.
	 */
	public boolean isSellable()
	{
		return _sellable;
	}
	
	/**
	 * Checks if the item can be dropped by a player.
	 * @return {@code true} if the item is dropable, {@code false} otherwise.
	 */
	public boolean isDropable()
	{
		return _dropable;
	}
	
	/**
	 * Checks if the item can be destroyed.<br>
	 * Returns {@code true} if the item is destroyable.<br>
	 * Returns {@code false} if the item cannot be destroyed.
	 * @return a boolean indicating if the item is destroyable
	 */
	public boolean isDestroyable()
	{
		return _destroyable;
	}
	
	/**
	 * Checks if the item can be traded.<br>
	 * Returns {@code true} if trading is allowed.<br>
	 * Returns {@code false} if trading is restricted.
	 * @return a boolean indicating tradeability.
	 */
	public boolean isTradeable()
	{
		return _tradeable;
	}
	
	/**
	 * Checks if the item can be deposited.<br>
	 * Returns {@code true} if it is depositable, otherwise returns {@code false}.
	 * @return a boolean indicating if the item is depositable.
	 */
	public boolean isDepositable()
	{
		return _depositable;
	}
	
	/**
	 * Checks if the item can be enchanted.<br>
	 * This method verifies that the item is not on the {@code PlayerConfig.ENCHANT_BLACKLIST}.<br>
	 * It also checks the internal {@code _enchantable} flag.
	 * @return {@code true} if the item can be enchanted, {@code false} otherwise.
	 */
	public boolean isEnchantable()
	{
		return (Arrays.binarySearch(PlayerConfig.ENCHANT_BLACKLIST, _itemId) < 0) && _enchantable;
	}
	
	/**
	 * Retrieves the maximum enchantment level for this item.<br>
	 * Returns {@code 0} if no specific limit is set.
	 * @return The maximum number of enchantments allowed.
	 */
	public int getEnchantLimit()
	{
		return _enchantLimit > 0 ? _enchantLimit : 0;
	}
	
	/**
	 * Retrieves the number of normal slots available for ensouling.<br>
	 * This value represents the standard capacity for this item type.
	 * @return the number of {@code int} slots.
	 */
	public int getEnsoulSlots()
	{
		return _ensoulNormalSlots;
	}
	
	/**
	 * Retrieves the number of special slots available for ensouling.<br>
	 * This value is used to determine how many unique items can be bound to an object.
	 * @return The total count of {@code _ensoulSpecialSlots}.
	 */
	public int getSpecialEnsoulSlots()
	{
		return _ensoulSpecialSlots;
	}
	
	/**
	 * Checks if the item can be affected by elemental properties.<br>
	 * Returns {@code true} if the item is elementable.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating if the item is elementable.
	 */
	public boolean isElementable()
	{
		return _elementable;
	}
	
	/**
	 * Checks if the item is classified as a common item.
	 * @return {@code true} if the item is common, {@code false} otherwise.
	 */
	public boolean isCommon()
	{
		return _common;
	}
	
	/**
	 * Checks if the item is classified as a hero item.
	 * @return {@code true} if it is a hero item, {@code false} otherwise.
	 */
	public boolean isHeroItem()
	{
		return _heroItem;
	}
	
	/**
	 * Checks if the item is designated for Player vs Player combat.
	 * @return {@code true} if it is a PvP item, {@code false} otherwise.
	 */
	public boolean isPvpItem()
	{
		return _pvpItem;
	}
	
	/**
	 * Checks if the item is a potion.<br>
	 * This method returns {@code true} if the item type matches {@code EtcItemType.POTION}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if this is a potion, {@code false} otherwise.
	 */
	public boolean isPotion()
	{
		return getItemType() == EtcItemType.POTION;
	}
	
	/**
	 * Checks if the item is an elixir.<br>
	 * This method returns {@code true} if the item type matches {@code EtcItemType.ELIXIR}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if this is an elixir, {@code false} otherwise.
	 */
	public boolean isElixir()
	{
		return getItemType() == EtcItemType.ELIXIR;
	}
	
	/**
	 * Checks if the item is a scroll.<br>
	 * This method returns {@code true} if the item type matches {@code EtcItemType.SCROLL}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the item is a scroll, {@code false} otherwise.
	 */
	public boolean isScroll()
	{
		return getItemType() == EtcItemType.SCROLL;
	}
	
	/**
	 * Adds a new {@code FuncTemplate} to the item.<br>
	 * This method maps the template to its specific {@code Stat}.<br>
	 * It also applies attributes based on the template value.<br>
	 * A warning is logged if a duplicate stat is found.
	 * @param template The {@code FuncTemplate} to be added.
	 */
	public void addFunctionTemplate(FuncTemplate template)
	{
		switch (template.getStat())
		{
			case FIRE_RES:
			case FIRE_POWER:
			{
				setAttributes(new AttributeHolder(AttributeType.FIRE, (int) template.getValue()));
				break;
			}
			case WATER_RES:
			case WATER_POWER:
			{
				setAttributes(new AttributeHolder(AttributeType.WATER, (int) template.getValue()));
				break;
			}
			case WIND_RES:
			case WIND_POWER:
			{
				setAttributes(new AttributeHolder(AttributeType.WIND, (int) template.getValue()));
				break;
			}
			case EARTH_RES:
			case EARTH_POWER:
			{
				setAttributes(new AttributeHolder(AttributeType.EARTH, (int) template.getValue()));
				break;
			}
			case HOLY_RES:
			case HOLY_POWER:
			{
				setAttributes(new AttributeHolder(AttributeType.HOLY, (int) template.getValue()));
				break;
			}
			case DARK_RES:
			case DARK_POWER:
			{
				setAttributes(new AttributeHolder(AttributeType.DARK, (int) template.getValue()));
				break;
			}
		}
		
		if (_funcTemplates == null)
		{
			_funcTemplates = new EnumMap<>(Stat.class);
		}
		
		final Stat stat = template.getStat();
		if (_funcTemplates.put(stat, template) != null)
		{
			LOGGER.warning("Item with id " + _itemId + " has 2 func templates with same stat: " + stat);
		}
	}
	
	/**
	 * Adds a new condition to the list of requirements.<br>
	 * This method ensures that the {@code Condition} is checked before an action can occur.
	 * @param c The {@code Condition} to add to the requirements.
	 */
	public void attachCondition(Condition c)
	{
		if (_preConditions == null)
		{
			_preConditions = new ArrayList<>();
		}
		
		_preConditions.add(c);
	}
	
	/**
	 * Retrieves the list of conditions required for this item.
	 * @return a {@code List} of {@code Condition} objects.
	 */
	public List<Condition> getConditions()
	{
		return _preConditions;
	}
	
	/**
	 * Checks if the item contains any skills.<br>
	 * Returns {@code true} if the internal skill list is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the item has skills, {@code false} otherwise.
	 */
	public boolean hasSkills()
	{
		return _skills != null;
	}
	
	/**
	 * Retrieves all skills associated with this item.
	 * @return a {@code List} of {@code ItemSkillHolder} objects.
	 */
	public List<ItemSkillHolder> getAllSkills()
	{
		return _skills;
	}
	
	/**
	 * Retrieves a list of skills that match the given criteria.<br>
	 * This method filters the internal skill collection based on the provided {@code Predicate}.<br>
	 * It returns {@code null} if no skills are associated with this item.
	 * @param condition The filter to apply to each {@code ItemSkillHolder}.
	 * @return A list of matching {@code ItemSkillHolder} objects, or {@code null} if the skill set is empty.
	 */
	public List<ItemSkillHolder> getSkills(Predicate<ItemSkillHolder> condition)
	{
		if (_skills == null)
		{
			return null;
		}
		
		final List<ItemSkillHolder> result = new ArrayList<>();
		for (ItemSkillHolder skill : _skills)
		{
			if (condition.test(skill))
			{
				result.add(skill);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of skills that match the specified type.<br>
	 * This method filters the internal skill collection based on the provided {@code ItemSkillType}.<br>
	 * It returns {@code null} if no skills are defined for this item.
	 * @param type The {@code ItemSkillType} to filter by.
	 * @return A list of {@code ItemSkillHolder} objects matching the type, or {@code null} if none exist.
	 */
	public List<ItemSkillHolder> getSkills(ItemSkillType type)
	{
		if (_skills == null)
		{
			return null;
		}
		
		final List<ItemSkillHolder> result = new ArrayList<>();
		for (ItemSkillHolder skill : _skills)
		{
			if (skill.getType() == type)
			{
				result.add(skill);
			}
		}
		
		return result;
	}
	
	/**
	 * Iterates through all skills of a specific type.<br>
	 * Performs the given action for each matching skill found.
	 * @param type The {@code ItemSkillType} to filter by.
	 * @param action The {@code Consumer} to apply to each {@code ItemSkillHolder}.
	 */
	public void forEachSkill(ItemSkillType type, Consumer<ItemSkillHolder> action)
	{
		if (_skills != null)
		{
			for (ItemSkillHolder skill : _skills)
			{
				if (skill.getType() == type)
				{
					action.accept(skill);
				}
			}
		}
	}
	
	/**
	 * Adds a new skill to the item.<br>
	 * This method initializes the internal list if it is {@code null}.<br>
	 * The provided {@code ItemSkillHolder} is then added to the collection.
	 * @param holder The {@code ItemSkillHolder} to be added.
	 */
	public void addSkill(ItemSkillHolder holder)
	{
		// Agathion skills managed by AgathionData.
		// if ((getBodyPart() == SLOT_AGATHION) && (holder.getType() != ItemSkillType.ON_EQUIP) && (holder.getType() != ItemSkillType.ON_UNEQUIP))
		// {
		// LOGGER.warning("Remove from agathion " + _itemId + " " + holder + "!");
		// return;
		// }
		
		if (_skills == null)
		{
			_skills = new ArrayList<>();
		}
		
		_skills.add(holder);
	}
	
	/**
	 * Checks if a {@code Creature} can use or equip the item based on various restrictions.<br>
	 * This method validates GM permissions, Olympiad rules, event restrictions, and custom conditions.
	 * @param creature The {@code Creature} attempting to perform the action.
	 * @param object The {@code WorldObject} associated with the item.
	 * @param sendMessage Whether to send a descriptive message to the player if a condition fails.
	 * @return {@code true} if all conditions are met, {@code false} otherwise.
	 */
	public boolean checkCondition(Creature creature, WorldObject object, boolean sendMessage)
	{
		if (creature.isGM() && !GeneralConfig.GM_ITEM_RESTRICTION)
		{
			return true;
		}
		
		// Don't allow hero equipment and restricted items during Olympiad.
		if (creature.isPlayer())
		{
			if ((isOlyRestrictedItem() || _heroItem) && (creature.isPlayer() && creature.asPlayer().isInOlympiadMode()))
			{
				if (isEquipable())
				{
					creature.sendPacket(SystemMessageId.THE_ITEM_CANNOT_BE_EQUIPPED_IN_THE_OLYMPIAD);
				}
				else
				{
					creature.sendPacket(SystemMessageId.THE_ITEM_CANNOT_BE_USED_IN_THE_OLYMPIAD);
				}
				
				return false;
			}
			
			if (_isEventRestricted && creature.asPlayer().isOnEvent())
			{
				creature.sendMessage("You cannot use this item in the event.");
				return false;
			}
		}
		
		if (!isConditionAttached())
		{
			return true;
		}
		
		final Creature target = object.isCreature() ? object.asCreature() : null;
		for (Condition preCondition : _preConditions)
		{
			if (preCondition == null)
			{
				continue;
			}
			
			if (!preCondition.test(creature, target, null, null))
			{
				if (creature.isSummon())
				{
					creature.sendPacket(SystemMessageId.THE_PET_CANNOT_USE_THIS_ITEM);
					return false;
				}
				
				if (sendMessage)
				{
					final String msg = preCondition.getMessage();
					final int msgId = preCondition.getMessageId();
					if (msg != null)
					{
						creature.sendMessage(msg);
					}
					else if (msgId != 0)
					{
						final SystemMessage sm = new SystemMessage(msgId);
						if (preCondition.isAddName())
						{
							sm.addItemName(_itemId);
						}
						
						creature.sendPacket(sm);
					}
				}
				
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if there are any pre-conditions attached to this item.<br>
	 * It returns {@code true} if the {@code _preConditions} list is not {@code null} and contains items.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if conditions exist, {@code false} otherwise.
	 */
	public boolean isConditionAttached()
	{
		return (_preConditions != null) && !_preConditions.isEmpty();
	}
	
	/**
	 * Checks if the item can be used for quest purposes.
	 * @return {@code true} if the item is usable in a quest, {@code false} otherwise.
	 */
	public boolean isQuestUsableItem()
	{
		return _questUsableItem;
	}
	
	/**
	 * Checks if the item is designated as a quest item.
	 * @return {@code true} if it is a quest item, {@code false} otherwise.
	 */
	public boolean isQuestItem()
	{
		return _questItem;
	}
	
	/**
	 * Checks if the item is an Artifact (card or book), which the client displays in its own dedicated inventory tab.
	 * @return {@code true} if it is an Artifact, {@code false} otherwise.
	 */
	public boolean isArtifactItem()
	{
		return (_bodyPart == BodyPart.ARTIFACT);
	}
	
	/**
	 * Checks if the item can be transported as freight.
	 * @return {@code true} if the item is freightable, {@code false} otherwise.
	 */
	public boolean isFreightable()
	{
		return _freightable;
	}
	
	/**
	 * Checks if the item allows for self-resurrection.
	 * @return {@code true} if resurrection is allowed, {@code false} otherwise.
	 */
	public boolean isAllowSelfResurrection()
	{
		return _allowSelfResurrection;
	}
	
	/**
	 * Checks if the item is restricted during an Olympiad.<br>
	 * This method returns {@code true} if the item is marked as restricted in the configuration.<br>
	 * It also checks the global restriction flag for this specific item type.
	 * @return {@code true} if the item cannot be used or traded in an Olympiad, {@code false} otherwise.
	 */
	public boolean isOlyRestrictedItem()
	{
		return _isOlyRestricted || OlympiadConfig.OLYMPIAD_RESTRICTED_ITEMS.contains(_itemId);
	}
	
	/**
	 * Checks if the item is restricted for use in specific events.<br>
	 * Returns {@code true} if the item is restricted.<br>
	 * Returns {@code false} if the item is not restricted.
	 * @return a boolean value indicating restriction status.
	 */
	public boolean isEventRestrictedItem()
	{
		return _isEventRestricted;
	}
	
	/**
	 * Checks if this item is intended for NPC use.<br>
	 * Returns {@code true} if the item is for NPCs.<br>
	 * Returns {@code false} if it is not.
	 * @return a boolean value indicating if the item is for an NPC.
	 */
	public boolean isForNpc()
	{
		return _forNpc;
	}
	
	/**
	 * Checks if the item can be displayed in the appearance system.<br>
	 * Returns {@code true} if it is appearanceable, otherwise returns {@code false}.
	 * @return a boolean indicating if the item is appearanceable.
	 */
	public boolean isAppearanceable()
	{
		return _isAppearanceable;
	}
	
	/**
	 * Checks if the item has a blessed status.<br>
	 * This property determines if the item can be traded or sold normally.
	 * @return {@code true} if the item is blessed, {@code false} otherwise.
	 */
	public boolean isBlessed()
	{
		return _isBlessed;
	}
	
	/**
	 * Retrieves the specific slot assigned to an artifact.<br>
	 * This value identifies where the item can be placed in the equipment system.
	 * @return The integer ID of the artifact slot.
	 */
	public int getArtifactSlot()
	{
		return _artifactSlot;
	}
	
	/**
	 * Checks if the item has an immediate effect.<br>
	 * This value determines if the effect triggers instantly.
	 * @return {@code true} if the effect is immediate, {@code false} otherwise.
	 */
	public boolean hasExImmediateEffect()
	{
		return _exImmediateEffect;
	}
	
	/**
	 * Checks if the item has an immediate effect.<br>
	 * This determines if the effect happens instantly upon use.
	 * @return {@code true} if the effect is immediate, {@code false} otherwise.
	 */
	public boolean hasImmediateEffect()
	{
		return _immediateEffect;
	}
	
	/**
	 * Retrieves the default action for this item.<br>
	 * This value is used when no specific action is defined.
	 * @return the {@code ActionType} of the default action
	 */
	public ActionType getDefaultAction()
	{
		return _defaultAction;
	}
	
	/**
	 * Returns the skill cooldown time for this item.<br>
	 * This value determines how long a player must wait before using a skill again.
	 * @return The cooldown time as an {@code int}.
	 */
	public int useSkillDisTime()
	{
		return _useSkillDisTime;
	}
	
	/**
	 * Gets the reuse delay for this item.<br>
	 * This value determines how long a player must wait before using the item again.
	 * @return The reuse delay as an {@code int}.
	 */
	public int getReuseDelay()
	{
		return _reuseDelay;
	}
	
	/**
	 * Retrieves the shared reuse group for this item.<br>
	 * This value determines how the item behaves during reuse actions.
	 * @return the {@code int} value of the shared reuse group.
	 */
	public int getSharedReuseGroup()
	{
		return _sharedReuseGroup;
	}
	
	/**
	 * Retrieves the commission item type for this item.<br>
	 * This value determines how the item behaves within the commission system.
	 * @return the {@code CommissionItemType} of this item.
	 */
	public CommissionItemType getCommissionItemType()
	{
		return _commissionItemType;
	}
	
	/**
	 * Retrieves the icon associated with this item.
	 * @return The {@code String} representing the icon.
	 */
	public String getIcon()
	{
		return _icon;
	}
	
	/**
	 * Retrieves the default enchantment level for this item.<br>
	 * This value is used when no specific level is defined.
	 * @return The default enchantment level as an {@code int}.
	 */
	public int getDefaultEnchantLevel()
	{
		return _defaultEnchantLevel;
	}
	
	/**
	 * Checks if the item is a pet collar.<br>
	 * This method returns {@code true} if the item type matches {@code EtcItemType.PET_COLLAR}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the item is a pet collar, {@code false} otherwise.
	 */
	public boolean isPetItem()
	{
		return getItemType() == EtcItemType.PET_COLLAR;
	}
	
	/**
	 * Adds a new {@code ExtractableProduct} to the list of items.<br>
	 * This method registers an item that can be extracted from another object.
	 * @param extractableProduct The {@code ExtractableProduct} to add.
	 */
	public void addCapsuledItem(ExtractableProduct extractableProduct)
	{
	}
	
	/**
	 * Retrieves the value of a specific statistic for this item.<br>
	 * It checks if a functional template exists for the given {@code Stat}.<br>
	 * If no valid template is found, it returns the provided default value.
	 * @param stat The {@code Stat} type to retrieve.
	 * @param defaultValue The value to return if the statistic is not defined.
	 * @return The numeric value of the statistic or the {@code defaultValue}.
	 */
	public double getStats(Stat stat, double defaultValue)
	{
		if (_funcTemplates != null)
		{
			final FuncTemplate template = _funcTemplates.get(stat);
			if ((template != null) && ((template.getFunctionClass() == FuncAdd.class) || (template.getFunctionClass() == FuncSet.class)))
			{
				return template.getValue();
			}
		}
		
		return defaultValue;
	}
	
	/**
	 * Returns a string representation of the item.<br>
	 * This includes the name and the {@code _itemId}.
	 * @return A formatted string containing the name and ID.
	 */
	@Override
	public String toString()
	{
		return StringUtil.concat(_name, "(", String.valueOf(_itemId), ")");
	}
}
