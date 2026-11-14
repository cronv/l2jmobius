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
package org.l2jmobius.gameserver.network.holders;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;

/**
 * This class stores the data required to send character information to the client.<br>
 * It is primarily used for populating the character selection screen.
 */
public class CharacterInfoHolder
{
	private String _name;
	private int _objectId = 0;
	private long _exp = 0;
	private long _sp = 0;
	private int _clanId = 0;
	private int _race = 0;
	private int _classId = 0;
	private int _baseClassId = 0;
	private long _deleteTimer = 0;
	private long _lastAccess = 0;
	private int _face = 0;
	private int _hairStyle = 0;
	private int _hairColor = 0;
	private int _sex = 0;
	private int _level = 1;
	private int _maxHp = 0;
	private double _currentHp = 0;
	private int _maxMp = 0;
	private double _currentMp = 0;
	private final int[][] _paperdoll;
	private int _reputation = 0;
	private int _pkKills = 0;
	private int _pvpKills = 0;
	private AugmentationInstance _augmentation;
	private int _x = 0;
	private int _y = 0;
	private int _z = 0;
	private String _htmlPrefix = null;
	private boolean _isGood = false;
	private boolean _isEvil = false;
	private int _vitalityPoints = 0;
	private int _accessLevel = 0;
	private boolean _isNoble;
	private final PlayerVariables _vars;
	
	/**
	 * Creates a new {@code CharacterInfoHolder} instance.<br>
	 * This constructor initializes the object ID and character name.<br>
	 * It also restores the visible inventory and player variables based on the provided ID.
	 * @param objectId The unique identifier for the character.
	 * @param name The display name of the character.
	 */
	public CharacterInfoHolder(int objectId, String name)
	{
		setObjectId(objectId);
		_name = name;
		_paperdoll = PlayerInventory.restoreVisibleInventory(objectId);
		_vars = new PlayerVariables(_objectId);
	}
	
	/**
	 * Retrieves the unique identifier for this object.
	 * @return The {@code int} value of the object ID.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Sets the unique identifier for the character.<br>
	 * This value is stored in the {@code _objectId} field.
	 * @param objectId The new unique ID to assign to the character.
	 */
	public void setObjectId(int objectId)
	{
		_objectId = objectId;
	}
	
	/**
	 * Retrieves the current access level of the character.<br>
	 * This value is stored in the {@code _accessLevel} field.
	 * @return The integer value representing the character's access level.
	 */
	public int getAccessLevel()
	{
		return _accessLevel;
	}
	
	/**
	 * Sets the access level for the character.<br>
	 * This updates the internal {@code _accessLevel} field.
	 * @param level The new integer value for the access level.
	 */
	public void setAccessLevel(int level)
	{
		_accessLevel = level;
	}
	
	/**
	 * Checks if the character is considered good.
	 * @return {@code true} if the character is good, {@code false} otherwise.
	 */
	public boolean isGood()
	{
		return _isGood;
	}
	
	/**
	 * Sets the character's alignment to good.<br>
	 * This method sets {@code _isGood} to {@code true}.<br>
	 * It also sets {@code _isEvil} to {@code false}.
	 */
	public void setGood()
	{
		_isGood = true;
		_isEvil = false;
	}
	
	/**
	 * Checks if the character is classified as evil.
	 * @return {@code true} if the character is evil, {@code false} otherwise.
	 */
	public boolean isEvil()
	{
		return _isEvil;
	}
	
	/**
	 * Sets the character's alignment to evil.<br>
	 * This method updates {@code _isGood} to {@code false}.<br>
	 * It also sets {@code _isEvil} to {@code true}.
	 */
	public void setEvil()
	{
		_isGood = false;
		_isEvil = true;
	}
	
	/**
	 * Retrieves the unique identifier for the character's clan.
	 * @return The {@code int} value of the clan ID.
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * Sets the unique identifier for the character's clan.<br>
	 * This updates the {@code _clanId} field within the {@code CharacterInfoHolder}.
	 * @param clanId The new clan ID to assign.
	 */
	public void setClanId(int clanId)
	{
		_clanId = clanId;
	}
	
	/**
	 * Retrieves the unique identifier for the character's class.<br>
	 * This value represents the current job or profession of the character.
	 * @return the {@code int} value of the class ID.
	 */
	public int getClassId()
	{
		return _classId;
	}
	
	/**
	 * Retrieves the base class identifier for the character.<br>
	 * This value represents the primary class type before any modifications.
	 * @return the {@code int} value of the base class ID.
	 */
	public int getBaseClassId()
	{
		return _baseClassId;
	}
	
	/**
	 * Sets the unique identifier for the character's class.<br>
	 * This value is used to determine which class a character belongs to.
	 * @param classId The {@code int} ID of the character class.
	 */
	public void setClassId(int classId)
	{
		_classId = classId;
	}
	
	/**
	 * Sets the base class identifier for the character.<br>
	 * This value determines the primary class of the character.
	 * @param baseClassId The unique identifier for the base class.
	 */
	public void setBaseClassId(int baseClassId)
	{
		// Shinemaker
		if ((baseClassId >= 231) && (baseClassId <= 235))
		{
			_baseClassId = 231;
		}
		// Warg
		if ((baseClassId >= 255) && (baseClassId <= 259))
		{
			_baseClassId = 255;
		}
		// Samurai
		else if ((baseClassId >= 264) && (baseClassId <= 268))
		{
			_baseClassId = 264;
		}
		// Other Classes
		else
		{
			_baseClassId = baseClassId;
		}
	}
	
	/**
	 * Retrieves the current health points of the character.<br>
	 * This value represents the current {@code double} amount of HP.
	 * @return The current health points as a {@code double}.
	 */
	public double getCurrentHp()
	{
		return _currentHp;
	}
	
	/**
	 * Updates the current health points of the character.<br>
	 * This method sets the {@code _currentHp} field to a new value.
	 * @param currentHp The new health value to set.
	 */
	public void setCurrentHp(double currentHp)
	{
		_currentHp = currentHp;
	}
	
	/**
	 * Retrieves the current mana points of the character.
	 * @return the current {@code double} value of mana.
	 */
	public double getCurrentMp()
	{
		return _currentMp;
	}
	
	/**
	 * Updates the current mana points of the character.<br>
	 * This method sets the {@code _currentMp} field to a new value.
	 * @param currentMp The new amount of mana points to set.
	 */
	public void setCurrentMp(double currentMp)
	{
		_currentMp = currentMp;
	}
	
	/**
	 * Retrieves the current delete timer for the character.<br>
	 * This value is used to determine when a character should be removed.
	 * @return The {@code long} value of the delete timer.
	 */
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}
	
	/**
	 * Sets the deletion timer for the character.<br>
	 * This value determines when the character will be removed from the system.
	 * @param deleteTimer The time in milliseconds until the character is deleted.
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}
	
	/**
	 * Retrieves the timestamp of the last time the character was accessed.
	 * @return The {@code long} value representing the last access time.
	 */
	public long getLastAccess()
	{
		return _lastAccess;
	}
	
	/**
	 * Updates the timestamp of the last time the character was accessed.<br>
	 * This value is stored in the {@code _lastAccess} field.
	 * @param lastAccess The new timestamp to set for the last access.
	 */
	public void setLastAccess(long lastAccess)
	{
		_lastAccess = lastAccess;
	}
	
	/**
	 * Retrieves the current experience points of the character.
	 * @return the total amount of {@code exp} as a {@code long}.
	 */
	public long getExp()
	{
		return _exp;
	}
	
	/**
	 * Sets the experience points for the character.<br>
	 * This updates the internal {@code _exp} value.
	 * @param exp The new experience value to set.
	 */
	public void setExp(long exp)
	{
		_exp = exp;
	}
	
	/**
	 * Retrieves the face identifier for the character.<br>
	 * It checks the {@code visualFaceId} variable first.<br>
	 * If that variable is not set, it returns the default {@code _face} value.
	 * @return The integer ID of the character's face.
	 */
	public int getFace()
	{
		if (CategoryData.getInstance().isInCategory(CategoryType.DEATH_KNIGHT_ALL_CLASS, _classId))
		{
			return _face;
		}
		
		return _vars.getInt("visualFaceId", _face);
	}
	
	/**
	 * Sets the character's face ID.<br>
	 * This updates the internal {@code _face} value.
	 * @param face The new face identifier to assign.
	 */
	public void setFace(int face)
	{
		_face = face;
	}
	
	/**
	 * Retrieves the character's hair color.<br>
	 * It checks for a custom variable first.<br>
	 * If no variable exists, it returns the default value.
	 * @return The integer ID of the hair color.
	 */
	public int getHairColor()
	{
		if (CategoryData.getInstance().isInCategory(CategoryType.DEATH_KNIGHT_ALL_CLASS, _classId))
		{
			return _hairColor;
		}
		
		return _vars.getInt("visualHairColorId", _hairColor);
	}
	
	/**
	 * Sets the color of the character's hair.<br>
	 * This updates the {@code _hairColor} field in the {@code CharacterInfoHolder}.
	 * @param hairColor The new color value for the hair.
	 */
	public void setHairColor(int hairColor)
	{
		_hairColor = hairColor;
	}
	
	/**
	 * Retrieves the current hairstyle ID for the character.<br>
	 * It first checks for a value in the {@code visualHairId} variable.<br>
	 * If that variable is not set, it returns the default {@code _hairStyle} value.
	 * @return The integer ID of the character's hair style.
	 */
	public int getHairStyle()
	{
		if (CategoryData.getInstance().isInCategory(CategoryType.DEATH_KNIGHT_ALL_CLASS, _classId))
		{
			return _hairStyle;
		}
		
		return _vars.getInt("visualHairId", _hairStyle);
	}
	
	/**
	 * Sets the hairstyle for the character.<br>
	 * This updates the internal {@code _hairStyle} value.
	 * @param hairStyle The new hairstyle ID to assign.
	 */
	public void setHairStyle(int hairStyle)
	{
		_hairStyle = hairStyle;
	}
	
	/**
	 * Retrieves the object ID for a specific paperdoll slot.<br>
	 * This method looks up the first value in the 2D array for the given index.
	 * @param slot The index of the paperdoll slot to check.
	 * @return The integer object ID associated with the specified slot.
	 */
	public int getPaperdollObjectId(int slot)
	{
		return _paperdoll[slot][0];
	}
	
	/**
	 * Retrieves the item ID from a specific paperdoll slot.<br>
	 * This method accesses the second element of the array at the given index.
	 * @param slot The index of the paperdoll slot to check.
	 * @return The integer ID of the item in that slot.
	 */
	public int getPaperdollItemId(int slot)
	{
		return _paperdoll[slot][1];
	}
	
	/**
	 * Retrieves the visual identifier for an item in a specific paperdoll slot.<br>
	 * This value is used to determine how the item appears on the character model.
	 * @param slot The index of the paperdoll slot to check.
	 * @return The visual ID of the item at the specified slot.
	 */
	public int getPaperdollItemVisualId(int slot)
	{
		return _paperdoll[slot][3];
	}
	
	/**
	 * Retrieves the current level of the character.
	 * @return The integer value representing the character's level.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Sets the character level.<br>
	 * This updates the {@code _level} field of the {@code CharacterInfoHolder}.
	 * @param level The new level to assign.
	 */
	public void setLevel(int level)
	{
		_level = level;
	}
	
	/**
	 * Retrieves the maximum health points of the character.<br>
	 * This value represents the upper limit of {@code currentHp}.
	 * @return The maximum health as an {@code int}.
	 */
	public int getMaxHp()
	{
		return _maxHp;
	}
	
	/**
	 * Sets the maximum health points for the character.<br>
	 * This updates the {@code _maxHp} field in the {@code CharacterInfoHolder}.
	 * @param maxHp The new value for the maximum health.
	 */
	public void setMaxHp(int maxHp)
	{
		_maxHp = maxHp;
	}
	
	/**
	 * Retrieves the maximum mana points of the character.
	 * @return The value of {@code _maxMp}.
	 */
	public int getMaxMp()
	{
		return _maxMp;
	}
	
	/**
	 * Sets the maximum mana points for the character.<br>
	 * This updates the {@code _maxMp} field in the {@code CharacterInfoHolder}.
	 * @param maxMp The new maximum mana value.
	 */
	public void setMaxMp(int maxMp)
	{
		_maxMp = maxMp;
	}
	
	/**
	 * Retrieves the name of the character.
	 * @return The {@code String} name of the character.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Sets the character name.<br>
	 * This updates the {@code _name} field of the {@code CharacterInfoHolder}.
	 * @param name The new name to assign to the character.
	 */
	public void setName(String name)
	{
		_name = name;
	}
	
	/**
	 * Retrieves the race of the character.
	 * @return the {@code int} value representing the character's race.
	 */
	public int getRace()
	{
		return _race;
	}
	
	/**
	 * Sets the character's race.<br>
	 * This updates the internal {@code _race} value.
	 * @param race The new race identifier to set.
	 */
	public void setRace(int race)
	{
		_race = race;
	}
	
	/**
	 * Retrieves the sex of the character.
	 * @return The sex value as an {@code int}.
	 */
	public int getSex()
	{
		return _sex;
	}
	
	/**
	 * Sets the gender of the character.<br>
	 * This updates the internal {@code _sex} value.
	 * @param sex The gender identifier to set.
	 */
	public void setSex(int sex)
	{
		_sex = sex;
	}
	
	/**
	 * Retrieves the current amount of silver points.
	 * @return The total {@code sp} value as a {@code long}.
	 */
	public long getSp()
	{
		return _sp;
	}
	
	/**
	 * Sets the amount of silver points for the character.<br>
	 * This updates the {@code _sp} field.
	 * @param sp The new amount of silver points to set.
	 */
	public void setSp(long sp)
	{
		_sp = sp;
	}
	
	/**
	 * Retrieves the effect value for a specific equipment slot.<br>
	 * This method accesses the third index of the {@code _paperdoll} array.
	 * @param slot The index of the equipment slot to check.
	 * @return The integer value representing the enchantment effect.
	 */
	public int getEnchantEffect(int slot)
	{
		return _paperdoll[slot][2];
	}
	
	/**
	 * Updates the character's reputation value.<br>
	 * This method sets the internal {@code _reputation} field.
	 * @param reputation The new reputation value to assign.
	 */
	public void setReputation(int reputation)
	{
		_reputation = reputation;
	}
	
	/**
	 * Retrieves the current reputation of the character.
	 * @return The {@code int} value representing the character's reputation.
	 */
	public int getReputation()
	{
		return _reputation;
	}
	
	/**
	 * Sets the {@code AugmentationInstance} for this character.<br>
	 * This updates the internal augmentation data.
	 * @param augmentation The {@code AugmentationInstance} to set.
	 */
	public void setAugmentation(AugmentationInstance augmentation)
	{
		_augmentation = augmentation;
	}
	
	/**
	 * Retrieves the current augmentation instance.<br>
	 * This returns the {@code AugmentationInstance} associated with this character.
	 * @return the {@code AugmentationInstance} object.
	 */
	public AugmentationInstance getAugmentation()
	{
		return _augmentation;
	}
	
	/**
	 * Sets the number of Player Kill (PK) kills for the character.<br>
	 * This updates the internal {@code _pkKills} value.
	 * @param pkKills The new number of PK kills to set.
	 */
	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}
	
	/**
	 * Retrieves the total number of player kills.<br>
	 * This value represents the character's PK kill count.
	 * @return The current number of {@code pkKills}.
	 */
	public int getPkKills()
	{
		return _pkKills;
	}
	
	/**
	 * Updates the player's total number of PvP kills.<br>
	 * This value is stored in the {@code CharacterInfoHolder}.
	 * @param pvpKills The new count of PvP kills to set.
	 */
	public void setPvPKills(int pvpKills)
	{
		_pvpKills = pvpKills;
	}
	
	/**
	 * Retrieves the total number of PvP kills for the character.
	 * @return The count of {@code pvkKills}.
	 */
	public int getPvPKills()
	{
		return _pvpKills;
	}
	
	/**
	 * Retrieves the current {@code x} coordinate.
	 * @return The value of {@code _x}.
	 */
	public int getX()
	{
		return _x;
	}
	
	/**
	 * Retrieves the current Y coordinate of the character.
	 * @return The {@code int} value representing the vertical position.
	 */
	public int getY()
	{
		return _y;
	}
	
	/**
	 * Retrieves the vertical coordinate of the character.<br>
	 * This value represents the {@code z} position in the game world.
	 * @return The current {@code z} coordinate as an {@code int}.
	 */
	public int getZ()
	{
		return _z;
	}
	
	/**
	 * Sets the {@code x} coordinate for the character.<br>
	 * This updates the internal value of {@code _x}.
	 * @param x The new {@code x} coordinate to set.
	 */
	public void setX(int x)
	{
		_x = x;
	}
	
	/**
	 * Sets the vertical coordinate of the character.<br>
	 * This updates the {@code _y} field.
	 * @param y The new vertical coordinate value.
	 */
	public void setY(int y)
	{
		_y = y;
	}
	
	/**
	 * Sets the vertical coordinate of the character.<br>
	 * This updates the {@code _z} field.
	 * @param z The new vertical coordinate value.
	 */
	public void setZ(int z)
	{
		_z = z;
	}
	
	/**
	 * Retrieves the HTML prefix for the character.<br>
	 * This value is used to format the character's display information.
	 * @return The {@code String} representing the HTML prefix or {@code null}.
	 */
	public String getHtmlPrefix()
	{
		return _htmlPrefix;
	}
	
	/**
	 * Sets the HTML prefix for the character.<br>
	 * This value is used when displaying character information to the client.
	 * @param s The {@code String} prefix to set.
	 */
	public void setHtmlPrefix(String s)
	{
		_htmlPrefix = s;
	}
	
	/**
	 * Updates the vitality points for the character.<br>
	 * This method sets the internal value to the provided amount.
	 * @param points The new number of vitality points to set.
	 */
	public void setVitalityPoints(int points)
	{
		_vitalityPoints = points;
	}
	
	/**
	 * Retrieves the current vitality points of the character.<br>
	 * This value is stored in the {@code _vitalityPoints} field.
	 * @return The number of vitality points as an {@code int}.
	 */
	public int getVitalityPoints()
	{
		return _vitalityPoints;
	}
	
	/**
	 * Checks if the hair accessory feature is currently enabled.<br>
	 * This method retrieves the value from {@code PlayerVariables}.<br>
	 * It defaults to {@code true} if no value is set.
	 * @return {@code true} if hair accessories are enabled, {@code false} otherwise.
	 */
	public boolean isHairAccessoryEnabled()
	{
		return _vars.getBoolean(PlayerVariables.HAIR_ACCESSORY_VARIABLE_NAME, true);
	}
	
	/**
	 * Retrieves the total number of vitality items used by the player.<br>
	 * This value is stored in the {@code PlayerVariables}.
	 * @return The count of vitality items used as an {@code int}.
	 */
	public int getVitalityItemsUsed()
	{
		return _vars.getInt(PlayerVariables.VITALITY_ITEMS_USED_VARIABLE_NAME, 0);
	}
	
	/**
	 * Checks if the character has noble status.
	 * @return {@code true} if the character is noble, {@code false} otherwise.
	 */
	public boolean isNoble()
	{
		return _isNoble;
	}
	
	/**
	 * Sets the nobility status of the character.<br>
	 * This updates the {@code _isNoble} field.
	 * @param noble The new nobility status to set. Use {@code true} for noble or {@code false} for non-noble.
	 */
	public void setNoble(boolean noble)
	{
		_isNoble = noble;
	}
}
