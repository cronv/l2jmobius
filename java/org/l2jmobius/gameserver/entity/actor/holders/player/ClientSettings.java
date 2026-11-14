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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class stores the configuration settings provided by the client.<br>
 * It manages various preferences and options for a {@code Player}.
 * @author Index
 */
public class ClientSettings
{
	private final Player _player;
	private boolean _announceDisabled;
	private boolean _partyRequestRestrictedFromOthers;
	private boolean _partyRequestRestrictedFromClan;
	private boolean _partyRequestRestrictedFromFriends;
	private boolean _friendRequestRestrictedFromOthers;
	private boolean _friendRequestRestrictedFromClan;
	private int _partyContributionType;
	
	/**
	 * Creates a new {@code ClientSettings} instance for a specific player.<br>
	 * This constructor loads the player's settings from their saved variables.<br>
	 * It initializes all restriction flags and contribution types.
	 * @param player The {@code Player} object whose settings are being loaded.
	 */
	public ClientSettings(Player player)
	{
		_player = player;
		
		final String variable = _player.getVariables().getString(PlayerVariables.CLIENT_SETTINGS, "");
		final StatSet settings = variable.isEmpty() ? new StatSet() : new StatSet(Arrays.stream(variable.split(",")).map(entry -> entry.split("=")).collect(Collectors.toMap(entry -> entry[0].replace("{", "").replace(" ", ""), entry -> entry[1].replace("}", "").replace(" ", ""))));
		_announceDisabled = settings.getBoolean("ANNOUNCE_DISABLED", false);
		_partyRequestRestrictedFromOthers = settings.getBoolean("PARTY_REQUEST_RESTRICTED_FROM_OTHERS", false);
		_partyRequestRestrictedFromClan = settings.getBoolean("PARTY_REQUEST_RESTRICTED_FROM_CLAN", false);
		_partyRequestRestrictedFromFriends = settings.getBoolean("PARTY_REQUEST_RESTRICTED_FROM_FRIENDS", false);
		_friendRequestRestrictedFromOthers = settings.getBoolean("FRIENDS_REQUEST_RESTRICTED_FROM_OTHERS", false);
		_friendRequestRestrictedFromClan = settings.getBoolean("FRIENDS_REQUEST_RESTRICTED_FROM_CLAN", false);
		_partyContributionType = settings.getInt("PARTY_CONTRIBUTION_TYPE", 0);
	}
	
	/**
	 * Saves the current client settings to the player's variables.<br>
	 * This method updates the {@code PlayerVariables.CLIENT_SETTINGS} variable.<br>
	 * It uses a {@code StatSet} to bundle all restriction and contribution types.
	 */
	public void storeSettings()
	{
		final StatSet settings = new StatSet();
		settings.set("ANNOUNCE_DISABLED", _announceDisabled);
		settings.set("PARTY_REQUEST_RESTRICTED_FROM_OTHERS", _partyRequestRestrictedFromOthers);
		settings.set("PARTY_REQUEST_RESTRICTED_FROM_CLAN", _partyRequestRestrictedFromClan);
		settings.set("PARTY_REQUEST_RESTRICTED_FROM_FRIENDS", _partyRequestRestrictedFromFriends);
		settings.set("FRIENDS_REQUEST_RESTRICTED_FROM_OTHERS", _friendRequestRestrictedFromOthers);
		settings.set("FRIENDS_REQUEST_RESTRICTED_FROM_CLAN", _friendRequestRestrictedFromClan);
		settings.set("PARTY_CONTRIBUTION_TYPE", _partyContributionType);
		_player.getVariables().set(PlayerVariables.CLIENT_SETTINGS, settings.getSet());
	}
	
	/**
	 * Checks if the announcement feature is currently turned off.<br>
	 * This method returns the status of the {@code _announceDisabled} flag.
	 * @return {@code true} if announcements are disabled, {@code false} otherwise.
	 */
	public boolean isAnnounceDisabled()
	{
		return _announceDisabled;
	}
	
	/**
	 * Sets whether announcements are enabled or disabled.<br>
	 * This method updates the {@code _announceDisabled} state.<br>
	 * It also calls {@code storeSettings} to save the change.
	 * @param enabled The boolean value to set for announcement status.
	 */
	public void setAnnounceEnabled(boolean enabled)
	{
		_announceDisabled = enabled;
		storeSettings();
	}
	
	/**
	 * Checks if the player has restricted party requests from other players.<br>
	 * This setting determines who can send a request to join the party.
	 * @return {@code true} if requests are restricted, {@code false} otherwise.
	 */
	public boolean isPartyRequestRestrictedFromOthers()
	{
		return _partyRequestRestrictedFromOthers;
	}
	
	/**
	 * Sets whether other players can send party requests.<br>
	 * This updates the {@code _partyRequestRestrictedFromOthers} flag.
	 * @param partyRequestRestrictedFromOthers The new restriction status. Set to {@code true} to block requests from others, or {@code false} to allow them.
	 */
	public void setPartyRequestRestrictedFromOthers(boolean partyRequestRestrictedFromOthers)
	{
		_partyRequestRestrictedFromOthers = partyRequestRestrictedFromOthers;
	}
	
	/**
	 * Checks if the player has restricted party requests from clan members.<br>
	 * This returns {@code true} if the restriction is active.
	 * @return {@code true} if restricted, {@code false} otherwise.
	 */
	public boolean isPartyRequestRestrictedFromClan()
	{
		return _partyRequestRestrictedFromClan;
	}
	
	/**
	 * Sets whether players can send party requests to members of their own clan.<br>
	 * Use {@code true} to block requests from clan members.<br>
	 * Use {@code false} to allow requests from clan members.
	 * @param partyRequestRestrictedFromClan The restriction status for clan members.
	 */
	public void setPartyRequestRestrictedFromClan(boolean partyRequestRestrictedFromClan)
	{
		_partyRequestRestrictedFromClan = partyRequestRestrictedFromClan;
	}
	
	/**
	 * Checks if the player has restricted party requests from friends.<br>
	 * This returns {@code true} if the restriction is active.<br>
	 * It returns {@code false} if the restriction is disabled.
	 * @return The current status of the friend party request restriction.
	 */
	public boolean isPartyRequestRestrictedFromFriends()
	{
		return _partyRequestRestrictedFromFriends;
	}
	
	/**
	 * Sets whether friends are allowed to send party requests.<br>
	 * Use {@code true} to block requests from friends.<br>
	 * Use {@code false} to allow requests from friends.
	 * @param partyRequestRestrictedFromFriends The restriction status for friend party requests.
	 */
	public void setPartyRequestRestrictedFromFriends(boolean partyRequestRestrictedFromFriends)
	{
		_partyRequestRestrictedFromFriends = partyRequestRestrictedFromFriends;
	}
	
	/**
	 * Checks if the player has restricted friend requests from other players.<br>
	 * This setting determines who can send a friend request to this user.
	 * @return {@code true} if requests are restricted, {@code false} otherwise.
	 */
	public boolean isFriendRequestRestrictedFromOthers()
	{
		return _friendRequestRestrictedFromOthers;
	}
	
	/**
	 * Updates the privacy setting for receiving friend requests.<br>
	 * This determines if other players can send friend requests to this user.
	 * @param friendRequestRestrictedFromOthers Set to {@code true} to block requests from others, or {@code false} to allow them.
	 */
	public void setFriendRequestRestrictedFromOthers(boolean friendRequestRestrictedFromOthers)
	{
		_friendRequestRestrictedFromOthers = friendRequestRestrictedFromOthers;
	}
	
	/**
	 * Checks if friend requests are restricted from clan members.<br>
	 * This method returns the current restriction status for the player.
	 * @return {@code true} if restrictions are active, {@code false} otherwise.
	 */
	public boolean isFriendRequestRestrictedFromClan()
	{
		return _friendRequestRestrictedFromClan;
	}
	
	/**
	 * Sets whether friend requests are restricted from clan members.<br>
	 * This updates the {@code _friendRequestRestrictedFromClan} field.
	 * @param friendRequestRestrictedFromClan The new restriction status. Set to {@code true} to restrict or {@code false} to allow.
	 */
	public void setFriendRequestRestrictionFromClan(boolean friendRequestRestrictedFromClan)
	{
		_friendRequestRestrictedFromClan = friendRequestRestrictedFromClan;
	}
	
	/**
	 * Retrieves the current party contribution type.<br>
	 * This value determines how a player contributes to their party.
	 * @return the {@code int} value representing the contribution type.
	 */
	public int getPartyContributionType()
	{
		return _partyContributionType;
	}
	
	/**
	 * Sets the type of contribution for a party.<br>
	 * This method updates the internal value and saves the settings.
	 * @param partyContributionType The new contribution type to assign.
	 */
	public void setPartyContributionType(int partyContributionType)
	{
		_partyContributionType = partyContributionType;
		storeSettings();
	}
}
