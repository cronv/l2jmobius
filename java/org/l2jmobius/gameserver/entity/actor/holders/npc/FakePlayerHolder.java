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
package org.l2jmobius.gameserver.entity.actor.holders.npc;

import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class acts as a container for data related to fake players in the game world.<br>
 * It stores information such as {@code PlayerClass} and other attributes defined in {@code FakePlayerData}.
 * @author Mobius
 */
public class FakePlayerHolder
{
	private final PlayerClass _playerClass;
	private final int _hair;
	private final int _hairColor;
	private final int _face;
	private final int _nameColor;
	private final int _titleColor;
	private final int _equipHead;
	private final int _equipRHand;
	private final int _equipLHand;
	private final int _equipGloves;
	private final int _equipChest;
	private final int _equipLegs;
	private final int _equipFeet;
	private final int _equipCloak;
	private final int _equipHair;
	private final int _equipHair2;
	private final int _agathionId;
	private final int _weaponEnchantLevel;
	private final int _armorEnchantLevel;
	private final boolean _fishing;
	private final int _baitLocationX;
	private final int _baitLocationY;
	private final int _baitLocationZ;
	private final int _recommends;
	private final int _nobleLevel;
	private final boolean _hero;
	private final int _clanId;
	private final int _pledgeStatus;
	private final boolean _isSitting;
	private final int _privateStoreType;
	private final String _privateStoreMessage;
	private final boolean _talkable;
	private final int _mountType;
	private final int _mountNpcId;
	
	/**
	 * Creates a new {@code FakePlayerHolder} instance using the provided data.<br>
	 * This constructor initializes all player attributes from a {@code StatSet}.<br>
	 * It also registers the fake player's name and talkable status in {@code FakePlayerData}.
	 * @param set The {@code StatSet} containing the data for the fake player.
	 */
	public FakePlayerHolder(StatSet set)
	{
		_playerClass = PlayerClass.getPlayerClass(set.getInt("classId", 1));
		_hair = set.getInt("hair", 1);
		_hairColor = set.getInt("hairColor", 1);
		_face = set.getInt("face", 1);
		_nameColor = set.getInt("nameColor", 0xFFFFFF);
		_titleColor = set.getInt("titleColor", 0xECF9A2);
		_equipHead = set.getInt("equipHead", 0);
		_equipRHand = set.getInt("equipRHand", 0); // Or dual hand.
		_equipLHand = set.getInt("equipLHand", 0);
		_equipGloves = set.getInt("equipGloves", 0);
		_equipChest = set.getInt("equipChest", 0);
		_equipLegs = set.getInt("equipLegs", 0);
		_equipFeet = set.getInt("equipFeet", 0);
		_equipCloak = set.getInt("equipCloak", 0);
		_equipHair = set.getInt("equipHair", 0);
		_equipHair2 = set.getInt("equipHair2", 0);
		_agathionId = set.getInt("agathionId", 0);
		_weaponEnchantLevel = set.getInt("weaponEnchantLevel", 0);
		_armorEnchantLevel = set.getInt("armorEnchantLevel", 0);
		_fishing = set.getBoolean("fishing", false);
		_baitLocationX = set.getInt("baitLocationX", 0);
		_baitLocationY = set.getInt("baitLocationY", 0);
		_baitLocationZ = set.getInt("baitLocationZ", 0);
		_recommends = set.getInt("recommends", 0);
		_nobleLevel = set.getInt("nobleLevel", 0);
		_hero = set.getBoolean("hero", false);
		_clanId = set.getInt("clanId", 0);
		_pledgeStatus = set.getInt("pledgeStatus", 0);
		_isSitting = set.getBoolean("sitting", false);
		_privateStoreType = set.getInt("privateStoreType", 0);
		_privateStoreMessage = set.getString("privateStoreMessage", "");
		_talkable = set.getBoolean("fakePlayerTalkable", true);
		_mountType = set.getInt("mountType", 0);
		_mountNpcId = set.getInt("mountNpcId", 0);
		
		// Populate FakePlayerData mappings.
		final String name = set.getString("name", "");
		FakePlayerData.getInstance().addFakePlayerId(name, set.getInt("id", 0)); // Map name to npcId.
		final String lowercaseName = name.toLowerCase();
		FakePlayerData.getInstance().addFakePlayerName(lowercaseName, name); // Map lowercase name to original name.
		if (_talkable)
		{
			FakePlayerData.getInstance().addTalkableFakePlayerName(lowercaseName);
		}
	}
	
	/**
	 * Retrieves the character class of the fake player.
	 * @return the {@code PlayerClass} of the entity.
	 */
	public PlayerClass getPlayerClass()
	{
		return _playerClass;
	}
	
	/**
	 * Retrieves the mount type of the fake player.
	 * @return the mount type as a {@code MountType} ordinal (0 none, 1 strider, 2 wyvern, 3 wolf).
	 */
	public int getMountType()
	{
		return _mountType;
	}
	
	/**
	 * Retrieves the mount npc id of the fake player.
	 * @return the mount npc id, or {@code 0} when not mounted.
	 */
	public int getMountNpcId()
	{
		return _mountNpcId;
	}
	
	/**
	 * Retrieves the hair ID of the fake player.<br>
	 * This value represents the specific hairstyle used by the character.
	 * @return the hair ID as an {@code int}.
	 */
	public int getHair()
	{
		return _hair;
	}
	
	/**
	 * Retrieves the color of the hair for this fake player.
	 * @return the {@code int} value representing the hair color.
	 */
	public int getHairColor()
	{
		return _hairColor;
	}
	
	/**
	 * Retrieves the face ID of the fake player.<br>
	 * This value determines which facial appearance is used for the character.
	 * @return the {@code int} face ID.
	 */
	public int getFace()
	{
		return _face;
	}
	
	/**
	 * Retrieves the color code for the player name.<br>
	 * This value is used to determine how the name appears in the game.
	 * @return the integer color code of the name.
	 */
	public int getNameColor()
	{
		return _nameColor;
	}
	
	/**
	 * Retrieves the color value for the player title.<br>
	 * This value is used to determine how the title text appears.
	 * @return the {@code int} color code of the title.
	 */
	public int getTitleColor()
	{
		return _titleColor;
	}
	
	/**
	 * Retrieves the head equipment ID for the fake player.<br>
	 * This value represents the specific item worn on the head slot.
	 * @return the {@code int} identifier of the head equipment.
	 */
	public int getEquipHead()
	{
		return _equipHead;
	}
	
	/**
	 * Retrieves the ID of the equipment held in the right hand.<br>
	 * This value represents the specific item equipped by the fake player.
	 * @return the {@code int} ID of the right-hand equipment.
	 */
	public int getEquipRHand()
	{
		return _equipRHand;
	}
	
	/**
	 * Retrieves the ID of the item equipped in the left hand.
	 * @return the {@code int} identifier for the left-hand equipment.
	 */
	public int getEquipLHand()
	{
		return _equipLHand;
	}
	
	/**
	 * Retrieves the ID of the gloves equipped by the fake player.
	 * @return the integer ID of the gloves.
	 */
	public int getEquipGloves()
	{
		return _equipGloves;
	}
	
	/**
	 * Retrieves the ID of the chest armor equipped by the fake player.
	 * @return the integer ID of the chest equipment.
	 */
	public int getEquipChest()
	{
		return _equipChest;
	}
	
	/**
	 * Retrieves the ID of the legs equipment.<br>
	 * This value represents the specific item worn on the legs.
	 * @return the integer ID of the legs equipment.
	 */
	public int getEquipLegs()
	{
		return _equipLegs;
	}
	
	/**
	 * Retrieves the ID of the footwear equipped by the fake player.
	 * @return the {@code int} identifier for the feet equipment.
	 */
	public int getEquipFeet()
	{
		return _equipFeet;
	}
	
	/**
	 * Retrieves the ID of the cloak equipped by the fake player.
	 * @return The {@code int} identifier for the cloak.
	 */
	public int getEquipCloak()
	{
		return _equipCloak;
	}
	
	/**
	 * Retrieves the ID of the equipped hair.<br>
	 * This value represents the specific hair item currently worn by the player.
	 * @return The integer ID of the equipped hair.
	 */
	public int getEquipHair()
	{
		return _equipHair;
	}
	
	/**
	 * Retrieves the second equipped hair ID.<br>
	 * This value represents the secondary hairstyle for a fake player.
	 * @return the {@code int} ID of the second equipped hair.
	 */
	public int getEquipHair2()
	{
		return _equipHair2;
	}
	
	/**
	 * Retrieves the unique identifier for the Agathion.<br>
	 * This value represents the specific Agathion associated with this fake player.
	 * @return the {@code int} ID of the Agathion.
	 */
	public int getAgathionId()
	{
		return _agathionId;
	}
	
	/**
	 * Retrieves the current enchantment level of the weapon.<br>
	 * This value represents how many times the weapon has been enchanted.
	 * @return The integer level of the weapon enchantment.
	 */
	public int getWeaponEnchantLevel()
	{
		return _weaponEnchantLevel;
	}
	
	/**
	 * Retrieves the current armor enchantment level.<br>
	 * This value represents how many times the armor has been enchanted.
	 * @return The integer level of the armor enchantment.
	 */
	public int getArmorEnchantLevel()
	{
		return _armorEnchantLevel;
	}
	
	/**
	 * Checks if the fake player is currently fishing.
	 * @return {@code true} if the player is fishing, {@code false} otherwise.
	 */
	public boolean isFishing()
	{
		return _fishing;
	}
	
	/**
	 * Retrieves the X coordinate of the bait location.<br>
	 * This value is used to determine where the fishing bait is placed.
	 * @return The {@code int} value representing the X coordinate.
	 */
	public int getBaitLocationX()
	{
		return _baitLocationX;
	}
	
	/**
	 * Retrieves the Y coordinate of the bait location.<br>
	 * This value represents the vertical position in the game world.
	 * @return The Y coordinate as an {@code int}.
	 */
	public int getBaitLocationY()
	{
		return _baitLocationY;
	}
	
	/**
	 * Retrieves the Z coordinate of the bait location.<br>
	 * This value represents the height in the game world.
	 * @return The {@code int} value of the Z coordinate.
	 */
	public int getBaitLocationZ()
	{
		return _baitLocationZ;
	}
	
	/**
	 * Retrieves the number of recommendations for this fake player.
	 * @return The total count of recommendations as an {@code int}.
	 */
	public int getRecommends()
	{
		return _recommends;
	}
	
	/**
	 * Retrieves the noble level of the fake player.<br>
	 * This value represents the current nobility rank.
	 * @return the {@code int} noble level.
	 */
	public int getNobleLevel()
	{
		return _nobleLevel;
	}
	
	/**
	 * Checks if the fake player is a hero.
	 * @return {@code true} if the character is a hero, {@code false} otherwise.
	 */
	public boolean isHero()
	{
		return _hero;
	}
	
	/**
	 * Retrieves the unique identifier for the player's clan.<br>
	 * This value is used to identify which group the player belongs to.
	 * @return The {@code int} ID of the clan.
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * Retrieves the current pledge status of the fake player.<br>
	 * This value represents the specific pledge level or state.
	 * @return the {@code int} value representing the pledge status.
	 */
	public int getPledgeStatus()
	{
		return _pledgeStatus;
	}
	
	/**
	 * Checks if the fake player is currently in a sitting position.
	 * @return {@code true} if the player is sitting, {@code false} otherwise.
	 */
	public boolean isSitting()
	{
		return _isSitting;
	}
	
	/**
	 * Retrieves the type of the private store.<br>
	 * This value identifies how the private store is configured.
	 * @return the {@code int} value representing the private store type.
	 */
	public int getPrivateStoreType()
	{
		return _privateStoreType;
	}
	
	/**
	 * Retrieves the message associated with the private store.<br>
	 * This string is used to display information about a specific shop.
	 * @return The {@code String} containing the private store message.
	 */
	public String getPrivateStoreMessage()
	{
		return _privateStoreMessage;
	}
	
	/**
	 * Checks if the fake player can be interacted with via dialogue.
	 * @return {@code true} if the entity is talkable, {@code false} otherwise.
	 */
	public boolean isTalkable()
	{
		return _talkable;
	}
}
