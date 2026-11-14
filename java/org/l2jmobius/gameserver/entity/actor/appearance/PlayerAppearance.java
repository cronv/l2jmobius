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
package org.l2jmobius.gameserver.entity.actor.appearance;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.Sex;

/**
 * Represents the visual appearance of a {@code Player}.<br>
 * It manages data such as gender, hair style, and other cosmetic attributes.<br>
 * This class is used to determine how a player character looks in the game world.
 */
public class PlayerAppearance
{
	public static final int DEFAULT_TITLE_COLOR = 0xECF9A2;
	
	private Player _owner;
	
	private byte _face;
	
	private byte _hairColor;
	
	private byte _hairStyle;
	
	private boolean _isFemale;
	
	/** The current visible name of this player, not necessarily the real one */
	private String _visibleName;
	
	/** The current visible title of this player, not necessarily the real one */
	private String _visibleTitle;
	
	/** The default name color is 0xFFFFFF. */
	private int _nameColor = 0xFFFFFF;
	
	/** The default title color is 0xECF9A2. */
	private int _titleColor = DEFAULT_TITLE_COLOR;
	
	private int _visibleClanId = -1;
	private int _visibleClanCrestId = -1;
	private int _visibleClanLargeCrestId = -1;
	private int _visibleAllyId = -1;
	private int _visibleAllyCrestId = -1;
	
	/**
	 * Creates a new {@code PlayerAppearance} object.<br>
	 * This constructor initializes the basic visual traits of a player.
	 * @param face The face type identifier.
	 * @param hColor The hair color value.
	 * @param hStyle The hair style identifier.
	 * @param isFemale Set to {@code true} if the character is female, or {@code false} otherwise.
	 */
	public PlayerAppearance(byte face, byte hColor, byte hStyle, boolean isFemale)
	{
		_face = face;
		_hairColor = hColor;
		_hairStyle = hStyle;
		_isFemale = isFemale;
	}
	
	/**
	 * Sets the name that other players see for this character.<br>
	 * This name can be different from the actual account name.
	 * @param visibleName The new name to display for the player.
	 */
	public void setVisibleName(String visibleName)
	{
		_visibleName = visibleName;
	}
	
	/**
	 * Retrieves the name that is displayed to other players.<br>
	 * If no custom visible name is set, it returns the owner's real name.
	 * @return The {@code String} representing the visible name.
	 */
	public String getVisibleName()
	{
		if (_visibleName == null)
		{
			return _owner.getName();
		}
		
		return _visibleName;
	}
	
	/**
	 * Sets the title that is displayed for the player.<br>
	 * This value can be different from the actual title.
	 * @param visibleTitle The new title string to display.
	 */
	public void setVisibleTitle(String visibleTitle)
	{
		_visibleTitle = visibleTitle;
	}
	
	/**
	 * Retrieves the title currently displayed for the player.<br>
	 * If {@code _visibleTitle} is {@code null}, it returns the owner's default title.<br>
	 * Otherwise, it returns the custom visible title.
	 * @return The string representing the visible title of the player.
	 */
	public String getVisibleTitle()
	{
		if (_visibleTitle == null)
		{
			return _owner.getTitle();
		}
		
		return _visibleTitle;
	}
	
	/**
	 * Retrieves the current face ID of the player.<br>
	 * This value determines which facial appearance is displayed.
	 * @return The {@code byte} value representing the face.
	 */
	public byte getFace()
	{
		return _face;
	}
	
	/**
	 * Updates the face type of the player appearance.<br>
	 * The provided {@code int} value is cast to a {@code byte}.
	 * @param value The new face value to set.
	 */
	public void setFace(int value)
	{
		_face = (byte) value;
	}
	
	/**
	 * Retrieves the current hair color of the player.<br>
	 * The value is stored as a {@code byte}.
	 * @return the hair color as a {@code byte}
	 */
	public byte getHairColor()
	{
		return _hairColor;
	}
	
	/**
	 * Sets the hair color for the player.<br>
	 * The {@code value} is cast to a {@code byte}.
	 * @param value The new hair color value.
	 */
	public void setHairColor(int value)
	{
		_hairColor = (byte) value;
	}
	
	/**
	 * Retrieves the current hair style of the player.<br>
	 * This value is stored as a {@code byte}.
	 * @return The current hair style ID.
	 */
	public byte getHairStyle()
	{
		return _hairStyle;
	}
	
	/**
	 * Updates the hair style of the player.<br>
	 * The new style is stored as a {@code byte}.
	 * @param value The new hair style identifier.
	 */
	public void setHairStyle(int value)
	{
		_hairStyle = (byte) value;
	}
	
	/**
	 * Checks if the player character is female.
	 * @return {@code true} if the character is female, {@code false} otherwise.
	 */
	public boolean isFemale()
	{
		return _isFemale;
	}
	
	/**
	 * Sets the gender of the player to female.<br>
	 * This updates the {@code _isFemale} field to {@code true}.
	 */
	public void setFemale()
	{
		_isFemale = true;
	}
	
	/**
	 * Sets the player's gender to male.<br>
	 * This method updates the {@code _isFemale} field to {@code false}.
	 */
	public void setMale()
	{
		_isFemale = false;
	}
	
	/**
	 * Retrieves the sex type of the player.<br>
	 * This method checks if the character is female or male.
	 * @return the {@code Sex} value representing the gender.
	 */
	public Sex getSexType()
	{
		return _isFemale ? Sex.FEMALE : Sex.MALE;
	}
	
	/**
	 * Retrieves the current color of the player's name.<br>
	 * This value is used to determine how the name appears in the game world.
	 * @return The {@code int} color value of the name.
	 */
	public int getNameColor()
	{
		return _nameColor;
	}
	
	/**
	 * Sets the color of the player's name.<br>
	 * The value must be a non-negative integer.<br>
	 * If {@code nameColor} is less than 0, no changes are made.
	 * @param nameColor The new color value for the name.
	 */
	public void setNameColor(int nameColor)
	{
		if (nameColor < 0)
		{
			return;
		}
		
		_nameColor = nameColor;
	}
	
	/**
	 * Sets the name color using RGB values.<br>
	 * This method converts individual colors into a single integer.
	 * @param red The amount of red in the color.
	 * @param green The amount of green in the color.
	 * @param blue The amount of blue in the color.
	 */
	public void setNameColor(int red, int green, int blue)
	{
		_nameColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
	}
	
	/**
	 * Retrieves the current color of the player's title.<br>
	 * This value is used to determine how the title text is rendered.
	 * @return The {@code int} color value of the title.
	 */
	public int getTitleColor()
	{
		return _titleColor;
	}
	
	/**
	 * Sets the color of the player's visible title.<br>
	 * The value must be a non-negative integer.<br>
	 * If the provided {@code titleColor} is less than 0, no changes are made.
	 * @param titleColor The new color value for the title.
	 */
	public void setTitleColor(int titleColor)
	{
		if (titleColor < 0)
		{
			return;
		}
		
		_titleColor = titleColor;
	}
	
	/**
	 * Sets the color of the player title using RGB values.<br>
	 * This method converts individual red, green, and blue components into a single integer color.
	 * @param red The red component of the color.
	 * @param green The green component of the color.
	 * @param blue The blue component of the color.
	 */
	public void setTitleColor(int red, int green, int blue)
	{
		_titleColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
	}
	
	/**
	 * Sets the {@code Player} who owns this appearance.<br>
	 * This updates the internal {@code _owner} field.
	 * @param owner The {@code Player} object to set as the owner.
	 */
	public void setOwner(Player owner)
	{
		_owner = owner;
	}
	
	/**
	 * Retrieves the {@code Player} that owns this appearance.
	 * @return the {@code Player} owner of this appearance.
	 */
	public Player getOwner()
	{
		return _owner;
	}
	
	/**
	 * Retrieves the ID of the clan currently visible to other players.<br>
	 * It returns {@code _visibleClanId} if it is not {@code -1}.<br>
	 * If {@code _visibleClanId} is {@code -1}, it checks if the owner has a cursed weapon equipped.<br>
	 * If a cursed weapon is equipped, it returns {@code 0}.<br>
	 * Otherwise, it returns the clan ID of the {@code Player} owner.
	 * @return The visible clan ID as an {@code int}.
	 */
	public int getVisibleClanId()
	{
		return _visibleClanId != -1 ? _visibleClanId : _owner.isCursedWeaponEquipped() ? 0 : _owner.getClanId();
	}
	
	/**
	 * Retrieves the ID of the clan crest currently visible for the player.<br>
	 * It returns {@code _visibleClanCrestId} if it is not {@code -1}.<br>
	 * If no custom crest is set, it checks if the owner has a cursed weapon equipped.<br>
	 * If a cursed weapon is equipped, it returns {@code 0}.<br>
	 * Otherwise, it returns the default clan crest ID from the owner.
	 * @return The integer ID of the visible clan crest.
	 */
	public int getVisibleClanCrestId()
	{
		return _visibleClanCrestId != -1 ? _visibleClanCrestId : _owner.isCursedWeaponEquipped() ? 0 : _owner.getClanCrestId();
	}
	
	/**
	 * Retrieves the ID of the large clan crest currently visible for the player.<br>
	 * It returns {@code _visibleClanLargeCrestId} if it is not set to -1.<br>
	 * If it is -1, it checks if the owner has a cursed weapon equipped and returns 0 if true.<br>
	 * Otherwise, it returns the clan crest large ID from the owner.
	 * @return The integer ID of the visible large clan crest.
	 */
	public int getVisibleClanLargeCrestId()
	{
		return _visibleClanLargeCrestId != -1 ? _visibleClanLargeCrestId : _owner.isCursedWeaponEquipped() ? 0 : _owner.getClanCrestLargeId();
	}
	
	/**
	 * Retrieves the ID of the visible ally.<br>
	 * It returns {@code _visibleAllyId} if it is not {@code -1}.<br>
	 * If {@code _visibleAllyId} is {@code -1}, it checks if the owner has a cursed weapon equipped.<br>
	 * If a cursed weapon is equipped, it returns {@code 0}.<br>
	 * Otherwise, it returns the ally ID from the {@code Player} owner.
	 * @return The visible ally ID as an {@code int}.
	 */
	public int getVisibleAllyId()
	{
		return _visibleAllyId != -1 ? _visibleAllyId : _owner.isCursedWeaponEquipped() ? 0 : _owner.getAllyId();
	}
	
	/**
	 * Retrieves the ID of the visible ally crest.<br>
	 * It returns the value stored in {@code _visibleAllyCrestId} if it is not {@code -1}.<br>
	 * If no custom crest is set, it checks if the owner has a cursed weapon equipped or is null.<br>
	 * Otherwise, it returns the ally crest ID from the {@code Player} owner.
	 * @return The integer ID of the visible ally crest.
	 */
	public int getVisibleAllyCrestId()
	{
		return _visibleAllyCrestId != -1 ? _visibleAllyCrestId : (_owner == null) || _owner.isCursedWeaponEquipped() ? 0 : _owner.getAllyCrestId();
	}
	
	/**
	 * Updates the visible clan and ally information for the player.<br>
	 * This method sets all relevant IDs for clans, crests, and allies.
	 * @param clanId The unique identifier for the visible clan.
	 * @param clanCrestId The unique identifier for the visible clan crest.
	 * @param clanLargeCrestId The unique identifier for the large visible clan crest.
	 * @param allyId The unique identifier for the visible ally.
	 * @param allyCrestId The unique identifier for the visible ally crest.
	 */
	public void setVisibleClanData(int clanId, int clanCrestId, int clanLargeCrestId, int allyId, int allyCrestId)
	{
		_visibleClanId = clanId;
		_visibleClanCrestId = clanCrestId;
		_visibleClanLargeCrestId = clanLargeCrestId;
		_visibleAllyId = allyId;
		_visibleAllyCrestId = allyCrestId;
	}
}
