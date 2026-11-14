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
package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.AbstractZoneSettings;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.FortSiege;
import org.l2jmobius.gameserver.mechanics.siege.Siegable;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Represents a specific type of zone designated for siege warfare.<br>
 * This class handles the unique mechanics and rules associated with {@code Siegable} structures.
 * @author durgus, Skache
 */
public class SiegeZone extends ZoneType
{
	private static final int DISMOUNT_DELAY = 5;
	
	/**
	 * Creates a new {@code SiegeZone} instance using the provided unique identifier.<br>
	 * This constructor initializes the zone and loads its associated settings.<br>
	 * If no settings are found for the zone name, it creates a new {@code Settings} object.
	 * @param id The unique identification number for this siege zone.
	 */
	public SiegeZone(int id)
	{
		super(id);
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new Settings();
		}
		
		setSettings(settings);
	}
	
	public class Settings extends AbstractZoneSettings
	{
		private int _siegableId = -1;
		private Siegable _siege = null;
		private boolean _isActiveSiege = false;
		
		protected Settings()
		{
		}
		
		public int getSiegeableId()
		{
			return _siegableId;
		}
		
		protected void setSiegeableId(int id)
		{
			_siegableId = id;
		}
		
		public Siegable getSiege()
		{
			return _siege;
		}
		
		public void setSiege(Siegable s)
		{
			_siege = s;
		}
		
		public boolean isActiveSiege()
		{
			return _isActiveSiege;
		}
		
		public void setActiveSiege(boolean value)
		{
			_isActiveSiege = value;
		}
		
		@Override
		public void clear()
		{
			_siegableId = -1;
			_siege = null;
			_isActiveSiege = false;
		}
	}
	
	/**
	 * Retrieves the configuration settings for this zone.<br>
	 * This method returns a {@code Settings} object containing specific rules and properties.
	 * @return The {@code Settings} instance associated with this zone.
	 */
	@Override
	public Settings getSettings()
	{
		return (Settings) super.getSettings();
	}
	
	/**
	 * Sets a specific configuration parameter for this zone.<br>
	 * This method handles special logic for {@code castleId} and {@code fortId}.<br>
	 * It updates the siegeable ID in the zone settings.
	 * @param name The name of the parameter to set.
	 * @param value The string value associated with the parameter.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("castleId"))
		{
			if (getSettings().getSiegeableId() != -1)
			{
				throw new IllegalArgumentException("Siege object already defined!");
			}
			
			getSettings().setSiegeableId(Integer.parseInt(value));
		}
		else if (name.equals("fortId"))
		{
			if (getSettings().getSiegeableId() != -1)
			{
				throw new IllegalArgumentException("Siege object already defined!");
			}
			
			getSettings().setSiegeableId(Integer.parseInt(value));
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It updates the creature's zone status and handles siege-specific logic.<br>
	 * If the entity is a {@code Player}, it manages fame tasks and mount restrictions.<br>
	 * The method also sends a notification packet to the player upon entry.
	 * @param creature The {@code Creature} that has entered the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (!getSettings().isActiveSiege())
		{
			return;
		}
		
		creature.setInsideZone(ZoneId.PVP, true);
		creature.setInsideZone(ZoneId.SIEGE, true);
		creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true); // FIXME: Custom ?
		
		if (!creature.isPlayer())
		{
			return;
		}
		
		final Player player = creature.asPlayer();
		final int siegeableId = getSettings().getSiegeableId();
		if (player.isRegisteredOnThisSiegeField(siegeableId))
		{
			player.setInSiege(true); // in siege
			final Siegable siege = getSettings().getSiege();
			if (siege.giveFame() && (siege.getFameFrequency() > 0))
			{
				player.startFameTask(siege.getFameFrequency() * 1000, siege.getFameAmount());
			}
		}
		
		creature.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
		player.broadcastUserInfo(); // Siege marks show only inside the battlefield.
		
		if (FeatureConfig.ALLOW_MOUNTS_DURING_SIEGE)
		{
			return;
		}
		
		if (player.isGM())
		{
			player.sendMessage("You have entered a siege zone. GM dismount restrictions are ignored.");
			return;
		}
		
		// Check if wyvern riding is disallowed during siege and player is currently mounted on a wyvern.
		final Castle castle = CastleManager.getInstance().getCastleById(siegeableId);
		final boolean isCastleLord = (castle != null) && player.isClanLeader() && (player.getClanId() == castle.getOwnerId());
		if (player.getMountType() == MountType.WYVERN)
		{
			// Only allow castle lord to ride wyvern inside siege zone.
			if (!isCastleLord)
			{
				player.sendPacket(SystemMessageId.THIS_AREA_CANNOT_BE_ENTERED_WHILE_MOUNTED_ATOP_OF_A_WYVERN_YOU_WILL_BE_DISMOUNTED_FROM_YOUR_WYVERN_IF_YOU_DO_NOT_LEAVE);
				player.enteredNoLanding(DISMOUNT_DELAY);
			}
		}
		// If normal mounts (excluding wyvern) are disallowed during siege and player is mounted on such a mount.
		else if (player.isMounted() && (player.getMountType() != MountType.WYVERN))
		{
			// Allow castle lord to be mounted on a strider during siege, disallow others.
			// This is because, on retail servers, the inside of a castle is a no-fly zone during a siege.
			// However, to be able to **summon and ride a wyvern**, the castle lord must first be mounted on a strider.
			// So, the castle lord is permitted to mount a strider during the siege, even when mounts are generally disallowed.
			// This enables the castle lord to meet the precondition to summon their wyvern inside the siege zone.
			final boolean isCastleLordOnStrider = (player.getMountType() == MountType.STRIDER) && isCastleLord;
			if (!isCastleLordOnStrider)
			{
				// Dismount the player if they are not allowed to be mounted in siege zone.
				player.dismount();
			}
		}
		
		final Transform transform = player.getTransformation();
		if ((transform != null) && transform.isRiding())
		{
			player.untransform();
		}
	}
	
	/**
	 * Handles logic when a {@code Creature} leaves the zone.<br>
	 * Updates zone flags and combat statuses for players.<br>
	 * Cleans up siege-related effects and items if applicable.
	 * @param creature The {@code Creature} that is exiting the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.PVP, false);
		creature.setInsideZone(ZoneId.SIEGE, false);
		creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false); // FIXME: Custom ?
		if (getSettings().isActiveSiege() && creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
			player.broadcastUserInfo(); // Siege marks show only inside the battlefield.
			if (player.getMountType() == MountType.WYVERN)
			{
				player.exitedNoLanding();
			}
			
			// Neutrals leaving the battlefield turn purple for 10 seconds, registered characters keep their siege status.
			if (!player.isInSiege() && (player.getPvpFlag() == 0))
			{
				player.setPvpFlagLasts(System.currentTimeMillis() + 10000);
				player.startPvPFlag();
			}
		}
		
		if (!creature.isPlayer())
		{
			return;
		}
		
		final Player player = creature.asPlayer();
		player.stopFameTask();
		player.setInSiege(false);
		
		if (!(getSettings().getSiege() instanceof FortSiege) || (player.getInventory().getItemByItemId(9819) == null))
		{
			return;
		}
		
		// Drop combat flag.
		final Fort fort = FortManager.getInstance().getFortById(getSettings().getSiegeableId());
		if (fort != null)
		{
			FortSiegeManager.getInstance().dropCombatFlag(player, fort.getResidenceId());
		}
		else
		{
			final Item item = player.getInventory().getItemByItemId(9819);
			final BodyPart bodyPart = BodyPart.fromItem(item);
			player.getInventory().unEquipItemInBodySlot(bodyPart);
			player.destroyItem(ItemProcessType.DESTROY, item, null, true);
		}
		
		if (player.hasServitors())
		{
			player.getServitors().values().forEach(servitor ->
			{
				if (servitor.getRace() == Race.SIEGE_WEAPON)
				{
					servitor.abortAttack();
					servitor.abortCast();
					servitor.stopAllEffects();
					servitor.unSummon(player);
				}
			});
		}
	}
	
	/**
	 * Handles logic when a {@code Creature} dies inside the zone.<br>
	 * This method applies specific debuffs to players registered on the current siege field.<br>
	 * It checks if the siege is active before applying any effects.
	 * @param creature The {@code Creature} that died inside the zone.
	 */
	@Override
	public void onDieInside(Creature creature)
	{
		// Debuff participants only if they die inside siege zone.
		if (!getSettings().isActiveSiege() || !creature.isPlayer() || !creature.asPlayer().isRegisteredOnThisSiegeField(getSettings().getSiegeableId()))
		{
			return;
		}
		
		int level = 1;
		final BuffInfo info = creature.getEffectList().getBuffInfoBySkillId(5660);
		if (info != null)
		{
			level = Math.min(level + info.getSkill().getLevel(), 5);
		}
		
		final Skill skill = SkillData.getInstance().getSkill(5660, level);
		if (skill != null)
		{
			skill.applyEffects(creature, creature);
		}
	}
	
	/**
	 * Handles logic when a {@code Player} logs out from this zone.<br>
	 * It checks if the player belongs to the correct clan for the siegeable area.<br>
	 * If the player is not part of the allowed clan, they are teleported to a town.
	 * @param player The {@code Player} who is logging out.
	 */
	@Override
	public void onPlayerLogoutInside(Player player)
	{
		if (player.getClanId() != getSettings().getSiegeableId())
		{
			player.teleToLocation(TeleportWhereType.TOWN);
		}
	}
	
	/**
	 * Updates the status of all characters currently inside this zone.<br>
	 * If active siege is enabled, it triggers the {@code onEnter} method for each creature.<br>
	 * Otherwise, it removes various zone flags and sends a notification to players.
	 */
	public void updateZoneStatusForCharactersInside()
	{
		if (getSettings().isActiveSiege())
		{
			for (Creature creature : getCharactersInside())
			{
				if (creature != null)
				{
					onEnter(creature);
				}
			}
		}
		else
		{
			Player player;
			for (Creature creature : getCharactersInside())
			{
				if (creature == null)
				{
					continue;
				}
				
				creature.setInsideZone(ZoneId.PVP, false);
				creature.setInsideZone(ZoneId.SIEGE, false);
				creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
				
				if (creature.isPlayer())
				{
					player = creature.asPlayer();
					creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
					player.stopFameTask();
					if (player.getMountType() == MountType.WYVERN)
					{
						player.exitedNoLanding();
					}
				}
			}
		}
	}
	
	/**
	 * Sends a text message to all players currently inside this zone.<br>
	 * The method iterates through the list of players and calls {@code sendMessage}.
	 * @param message The text content to be sent to each player.
	 */
	public void announceToPlayers(String message)
	{
		for (Player player : getPlayersInside())
		{
			if (player != null)
			{
				player.sendMessage(message);
			}
		}
	}
	
	/**
	 * Retrieves the unique identifier for the siege object.<br>
	 * This value is fetched from the zone settings.
	 * @return the {@code int} ID of the siegeable object.
	 */
	public int getSiegeObjectId()
	{
		return getSettings().getSiegeableId();
	}
	
	/**
	 * Checks if the siege zone is currently active.<br>
	 * This method retrieves the status from the {@code AbstractZoneSettings}.
	 * @return {@code true} if the siege is active, {@code false} otherwise.
	 */
	public boolean isActive()
	{
		return getSettings().isActiveSiege();
	}
	
	/**
	 * Sets whether the siege zone is currently active.<br>
	 * This updates the status in the {@code AbstractZoneSettings}.
	 * @param value The new active state to set. Use {@code true} to enable or {@code false} to disable.
	 */
	public void setActive(boolean value)
	{
		getSettings().setActiveSiege(value);
	}
	
	/**
	 * Sets the {@code Siegable} instance for this zone.<br>
	 * This updates the siege object associated with the zone settings.
	 * @param siege The {@code Siegable} object to assign to the zone.
	 */
	public void setSiegeInstance(Siegable siege)
	{
		getSettings().setSiege(siege);
	}
	
	/**
	 * Removes all players from the zone who do not belong to the specified clan.<br>
	 * Players who are not part of the {@code owningClanId} will be teleported to a town.
	 * @param owningClanId The ID of the clan allowed to remain in the zone.
	 */
	public void banishForeigners(int owningClanId)
	{
		for (Player temp : getPlayersInside())
		{
			if (temp.getClanId() == owningClanId)
			{
				continue;
			}
			
			temp.teleToLocation(TeleportWhereType.TOWN);
		}
	}
}
