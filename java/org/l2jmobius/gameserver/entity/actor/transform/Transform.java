/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.entity.actor.transform;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.enums.player.Sex;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.itemcontainer.InventoryBlockType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerTransform;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.serverpackets.ExBasicActionList;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoEquipSlot;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents the transformation state of an actor in the game world.<br>
 * It handles data related to shape-shifting, including visual changes and status effects. This class is used by {@code Creature} and {@code Player} entities to manage their current form.
 * @author UnAfraid
 */
public class Transform
{
	private final int _id;
	private final int _displayId;
	private final TransformType _type;
	private final boolean _canSwim;
	private final int _spawnHeight;
	private final boolean _canAttack;
	private final boolean _isVisual;
	private final String _name;
	private final String _title;
	
	private TransformTemplate _maleTemplate;
	private TransformTemplate _femaleTemplate;
	
	/**
	 * Creates a new {@code Transform} instance using the provided data.<br>
	 * This constructor initializes all internal fields from a {@code StatSet}.<br>
	 * It sets values for IDs, types, and various boolean flags.
	 * @param set The {@code StatSet} containing the configuration data.
	 */
	public Transform(StatSet set)
	{
		_id = set.getInt("id");
		_displayId = set.getInt("displayId", _id);
		_type = set.getEnum("type", TransformType.class, TransformType.COMBAT);
		_canSwim = set.getInt("can_swim", 0) == 1;
		_canAttack = set.getInt("normal_attackable", 1) == 1;
		_isVisual = set.getInt("is_visual", 1) == 1;
		_spawnHeight = set.getInt("spawn_height", 0);
		_name = set.getString("setName", null);
		_title = set.getString("setTitle", null);
	}
	
	/**
	 * Retrieves the unique identifier for this {@code Transform}.
	 * @return The internal ID of the transform.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the unique display identifier for this {@code Transform}.<br>
	 * This ID is used to distinguish between different visual forms.
	 * @return The integer value of the display ID.
	 */
	public int getDisplayId()
	{
		return _displayId;
	}
	
	/**
	 * Retrieves the type of this transform.
	 * @return the {@code TransformType} associated with this transform.
	 */
	public TransformType getType()
	{
		return _type;
	}
	
	/**
	 * Checks if the transform allows swimming.
	 * @return {@code true} if swimming is enabled, {@code false} otherwise.
	 */
	public boolean canSwim()
	{
		return isVisualOnly() || _canSwim;
	}
	
	/**
	 * Checks if the transform allows attacking.
	 * @return {@code true} if attacking is enabled, {@code false} otherwise.
	 */
	public boolean canAttack()
	{
		return isVisualOnly() || _canAttack;
	}
	
	/**
	 * Checks if this transform is purely visual.<br>
	 * Returns {@code true} if the transform does not affect gameplay mechanics.<br>
	 * Returns {@code false} if it has functional effects.
	 * @return {@code true} if visual, {@code false} otherwise.
	 */
	public boolean isVisual()
	{
		return _isVisual;
	}
	
	/**
	 * Retrieves the height at which this transform spawns.
	 * @return the spawn height as an {@code int}.
	 */
	public int getSpawnHeight()
	{
		return _spawnHeight;
	}
	
	/**
	 * Retrieves the name of the {@code Transform}.
	 * @return The name as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the title of the {@code Transform}.
	 * @return The title as a {@code String}.
	 */
	public String getTitle()
	{
		return _title;
	}
	
	/**
	 * Retrieves the correct {@code TransformTemplate} based on the gender of the creature.<br>
	 * It checks if the {@code Creature} is a player or an NPC to determine the sex.<br>
	 * If no valid template is found, it returns {@code null}.
	 * @param creature The {@code Creature} used to determine the required template.
	 * @return The appropriate {@code TransformTemplate} for the creature's gender, or {@code null}.
	 */
	public TransformTemplate getTemplate(Creature creature)
	{
		if (creature.isPlayer())
		{
			return (creature.asPlayer().getAppearance().isFemale() ? _femaleTemplate : _maleTemplate);
		}
		else if (creature.isNpc())
		{
			return creature.asNpc().getTemplate().getSex() == Sex.FEMALE ? _femaleTemplate : _maleTemplate;
		}
		
		return null;
	}
	
	/**
	 * Sets the appearance template for a specific gender.<br>
	 * This method assigns the provided {@code TransformTemplate} to either the male or female slot.
	 * @param male Set to {@code true} to assign the template to the male slot, or {@code false} for the female slot.
	 * @param template The {@code TransformTemplate} to be assigned.
	 */
	public void setTemplate(boolean male, TransformTemplate template)
	{
		if (male)
		{
			_maleTemplate = template;
		}
		else
		{
			_femaleTemplate = template;
		}
	}
	
	/**
	 * Checks if the transform is a stance.<br>
	 * This returns {@code true} if the type is {@code MODE_CHANGE}.
	 * @return {@code true} if it is a stance, {@code false} otherwise.
	 */
	public boolean isStance()
	{
		return (_type == TransformType.MODE_CHANGE) || isVisualOnly();
	}
	
	/**
	 * Checks whether this transform changes nothing but appearance.<br>
	 * Such a transform leaves the stats, the skills and the action list of the creature alone.
	 * @return {@code true} when the transform is visual only.
	 */
	public boolean isVisualOnly()
	{
		return _type == TransformType.VISUAL_ONLY;
	}
	
	/**
	 * Checks if the transform is of a combat type.
	 * @return {@code true} if the type is {@code TransformType.COMBAT}, {@code false} otherwise.
	 */
	public boolean isCombat()
	{
		return _type == TransformType.COMBAT;
	}
	
	/**
	 * Checks if the transform type is set to non-combat.
	 * @return {@code true} if the type is {@code TransformType.NON_COMBAT}, {@code false} otherwise.
	 */
	public boolean isNonCombat()
	{
		return _type == TransformType.NON_COMBAT;
	}
	
	/**
	 * Checks if the transform type is set to flying.
	 * @return {@code true} if the transform is a flying type, {@code false} otherwise.
	 */
	public boolean isFlying()
	{
		return _type == TransformType.FLYING;
	}
	
	/**
	 * Checks if the transform type is cursed.
	 * @return {@code true} if the transform is a cursed type, {@code false} otherwise.
	 */
	public boolean isCursed()
	{
		return _type == TransformType.CURSED;
	}
	
	/**
	 * Checks if the current transform is in riding mode.<br>
	 * This method compares the {@code _type} field against {@code TransformType#RIDING_MODE}.
	 * @return {@code true} if the transform is a riding type, {@code false} otherwise.
	 */
	public boolean isRiding()
	{
		return _type == TransformType.RIDING_MODE;
	}
	
	/**
	 * Checks if the transform type is set to pure statistics.<br>
	 * This determines if the transformation provides only stat bonuses.
	 * @return {@code true} if the type is {@code PURE_STAT}, {@code false} otherwise.
	 */
	public boolean isPureStats()
	{
		return _type == TransformType.PURE_STAT;
	}
	
	/**
	 * Checks if the current transform allows for weapon statistics.<br>
	 * This is true if the {@code TransformType} is either {@code COMBAT} or {@code MODE_CHANGE}.
	 * @return {@code true} if weapon stats can be used, {@code false} otherwise.
	 */
	public boolean canUseWeaponStats()
	{
		return (_type == TransformType.COMBAT) || (_type == TransformType.MODE_CHANGE) || isVisualOnly();
	}
	
	/**
	 * Retrieves the collision height for a specific {@code Creature}.<br>
	 * It checks the transform template associated with the creature.<br>
	 * If no specific height is defined in the template, it returns the provided default value.
	 * @param creature The {@code Creature} to check for a transform template.
	 * @param defaultCollisionHeight The height to return if no template value exists.
	 * @return The collision height as a {@code float}.
	 */
	public float getCollisionHeight(Creature creature, float defaultCollisionHeight)
	{
		if (isVisualOnly())
		{
			return defaultCollisionHeight;
		}
		
		final TransformTemplate template = getTemplate(creature);
		if ((template != null) && (template.getCollisionHeight() != null))
		{
			return template.getCollisionHeight();
		}
		
		return defaultCollisionHeight;
	}
	
	/**
	 * Retrieves the collision radius for a specific {@code Creature}.<br>
	 * It checks the associated {@code TransformTemplate} for a defined value.<br>
	 * If no value is found in the template, it returns the provided default.
	 * @param creature The {@code Creature} to check for a transform.
	 * @param defaultCollisionRadius The radius to return if no specific value exists.
	 * @return The collision radius as a {@code float}.
	 */
	public float getCollisionRadius(Creature creature, float defaultCollisionRadius)
	{
		if (isVisualOnly())
		{
			return defaultCollisionRadius;
		}
		
		final TransformTemplate template = getTemplate(creature);
		if ((template != null) && (template.getCollisionRadius() != null))
		{
			return template.getCollisionRadius();
		}
		
		return defaultCollisionRadius;
	}
	
	/**
	 * Handles the logic for transforming a {@code Creature}.<br>
	 * It cancels current actions and updates the creature's state.<br>
	 * If the creature is a {@code Player}, it handles skills, appearance, and inventory blocks.
	 * @param creature The {@code Creature} to transform.
	 * @param addSkills Whether to grant new skills during transformation.
	 */
	public void onTransform(Creature creature, boolean addSkills)
	{
		// Abort attacking and casting.
		if ((_type != TransformType.MODE_CHANGE) && !isVisualOnly()) // Wargs and visual transforms should continue what they were doing.
		{
			creature.abortAttack();
			creature.abortCast();
		}
		
		// Get off the strider or something else if character is mounted.
		final Player player = creature.asPlayer();
		if (creature.isPlayer() && player.isMounted() && !isVisualOnly())
		{
			player.dismount();
		}
		
		final TransformTemplate template = getTemplate(creature);
		if (template != null)
		{
			// Start flying.
			if (isFlying())
			{
				creature.setFlying(true);
			}
			
			// Get player a bit higher so he doesn't drops underground after transformation happens.
			if (!isVisualOnly())
			{
				creature.setXYZ(creature.getX(), creature.getY(), (int) (creature.getZ() + getCollisionHeight(creature, 0)));
			}
			if (creature.isPlayer())
			{
				final PlayerAppearance appearance = player.getAppearance();
				if (_name != null)
				{
					appearance.setVisibleName(_name);
				}
				
				if (_title != null)
				{
					appearance.setVisibleTitle(_title);
				}
				
				if (addSkills && !isVisualOnly())
				{
					// Add common skills.
					for (SkillHolder h : template.getSkills())
					{
						player.addTransformSkill(h.getSkill());
					}
					
					// Add skills depending on level.
					for (AdditionalSkillHolder h : template.getAdditionalSkills())
					{
						if (player.getLevel() >= h.getMinLevel())
						{
							player.addTransformSkill(h.getSkill());
						}
					}
					
					// Add collection skills.
					for (SkillLearn s : SkillTreeData.getInstance().getCollectSkillTree().values())
					{
						final Skill skill = player.getKnownSkill(s.getSkillId());
						if (skill != null)
						{
							player.addTransformSkill(skill);
						}
					}
				}
				
				// Set inventory blocks if needed.
				final List<AdditionalItemHolder> additionalItems = isVisualOnly() ? List.of() : template.getAdditionalItems();
				if (!additionalItems.isEmpty())
				{
					final List<Integer> allowed = new ArrayList<>();
					final List<Integer> notAllowed = new ArrayList<>();
					for (AdditionalItemHolder holder : additionalItems)
					{
						if (holder.isAllowedToUse())
						{
							allowed.add(holder.getId());
						}
						else
						{
							notAllowed.add(holder.getId());
						}
					}
					
					if (!allowed.isEmpty())
					{
						player.getInventory().setInventoryBlock(allowed, InventoryBlockType.WHITELIST);
					}
					
					if (!notAllowed.isEmpty())
					{
						player.getInventory().setInventoryBlock(notAllowed, InventoryBlockType.BLACKLIST);
					}
				}
				
				// Send basic action list.
				if (!isVisualOnly() && template.hasBasicActionList())
				{
					player.sendPacket(new ExBasicActionList(template.getBasicActionList()));
				}
				
				if ((_type != TransformType.MODE_CHANGE) && !isVisualOnly())
				{
					player.getEffectList().stopAllToggles();
				}
				
				if (isVisualOnly())
				{
					player.sendSkillList();
				}
				else
				{
					ThreadPool.schedule(() ->
					{
						player.sendSkillList();
						player.sendPacket(new SkillCoolTime(player));
					}, 1000);
				}
				
				player.broadcastUserInfo();
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_TRANSFORM, player))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerTransform(player, getId()), player);
				}
			}
			else
			{
				creature.broadcastInfo();
			}
			
			// I don't know why, but you need to broadcast this to trigger the transformation client-side.
			// Usually should be sent naturally after applying effect, but sometimes is sent before that... i just do not know...
			creature.updateAbnormalVisualEffects();
		}
	}
	
	/**
	 * Handles the logic when a {@code Creature} reverts from a transformation.<br>
	 * This method stops active attacks and casting for the creature.<br>
	 * It also removes transformation-specific effects, skills, and visual titles if applicable.
	 * @param creature The {@code Creature} that is undergoing untransformation.
	 */
	public void onUntransform(Creature creature)
	{
		// Abort attacking and casting.
		if (!isVisualOnly())
		{
			creature.abortAttack();
			creature.abortCast();
		}
		
		final TransformTemplate template = getTemplate(creature);
		if (template != null)
		{
			// Stop flying.
			if (isFlying())
			{
				creature.setFlying(false);
			}
			
			if (creature.isPlayer())
			{
				final Player player = creature.asPlayer();
				final PlayerAppearance appearance = player.getAppearance();
				if (_name != null)
				{
					appearance.setVisibleName(null);
				}
				
				if (_title != null)
				{
					appearance.setVisibleTitle(null);
				}
				
				if (isVisualOnly())
				{
					// The shortcuts carry final IDs while transformed, so the saved ones go back out.
					player.restoreSkillShortcuts();
					player.sendSkillList();
				}
				else
				{
					// Remove transformation skills.
					player.removeAllTransformSkills();
				}
				
				// Remove inventory blocks if needed.
				if (!isVisualOnly() && !template.getAdditionalItems().isEmpty())
				{
					player.getInventory().unblock();
				}
				
				if (!isVisualOnly())
				{
					player.sendPacket(ExBasicActionList.STATIC_PACKET);
				}
				
				final EffectList effectList = player.getEffectList();
				if (!isVisualOnly() && !effectList.stopEffects(AbnormalType.TRANSFORM))
				{
					effectList.stopEffects(AbnormalType.CHANGEBODY);
				}
				
				if (!isVisualOnly())
				{
					ThreadPool.schedule(() ->
					{
						player.sendSkillList();
						player.sendPacket(new SkillCoolTime(player));
					}, 1000);
				}
				
				player.broadcastUserInfo();
				if (!isVisualOnly())
				{
					player.sendPacket(new ExUserInfoEquipSlot(player));
				}
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_TRANSFORM, player))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerTransform(player, 0), player);
				}
			}
			else
			{
				creature.broadcastInfo();
			}
		}
	}
	
	/**
	 * This method handles skill updates when a {@code Player} levels up.<br>
	 * It checks the transform template for additional skills.<br>
	 * New skills are added if the player meets the required level requirements.
	 * @param player The {@code Player} who leveled up.
	 */
	public void onLevelUp(Player player)
	{
		if (isVisualOnly())
		{
			return;
		}
		
		// Add skills depending on level.
		final TransformTemplate template = getTemplate(player);
		if ((template != null) && !template.getAdditionalSkills().isEmpty())
		{
			for (AdditionalSkillHolder holder : template.getAdditionalSkills())
			{
				if ((player.getLevel() >= holder.getMinLevel()) && (player.getSkillLevel(holder.getSkillId()) < holder.getSkillLevel()))
				{
					player.addTransformSkill(holder.getSkill());
				}
			}
		}
	}
	
	/**
	 * Retrieves the base attack type for a specific creature.<br>
	 * It checks the transform template associated with the {@code Creature}.<br>
	 * If no valid weapon type is found in the template, it returns the provided default.
	 * @param creature The {@code Creature} to check for its transform data.
	 * @param defaultAttackType The {@code WeaponType} to return if no specific type is defined.
	 * @return The {@code WeaponType} of the base attack or the {@code defaultAttackType}.
	 */
	public WeaponType getBaseAttackType(Creature creature, WeaponType defaultAttackType)
	{
		if (isVisualOnly())
		{
			return defaultAttackType;
		}
		
		final TransformTemplate template = getTemplate(creature);
		if (template != null)
		{
			final WeaponType weaponType = template.getBaseAttackType();
			if (weaponType != null)
			{
				return weaponType;
			}
		}
		
		return defaultAttackType;
	}
	
	/**
	 * Retrieves a specific stat value for a given {@code Creature}.<br>
	 * It checks the transform template and level-specific data.<br>
	 * If no data is found, it returns the provided default value.
	 * @param creature The {@code Creature} to check stats for.
	 * @param stat The {@code Stat} type to retrieve.
	 * @param defaultValue The value to return if the stat is not found.
	 * @return The calculated stat value or the {@code defaultValue}.
	 */
	public double getStats(Creature creature, Stat stat, double defaultValue)
	{
		if (isVisualOnly())
		{
			return defaultValue;
		}
		
		double val = defaultValue;
		final TransformTemplate template = getTemplate(creature);
		if (template != null)
		{
			val = template.getStats(stat, defaultValue);
			final TransformLevelData data = template.getData(creature.getLevel());
			if (data != null)
			{
				val = data.getStats(stat, defaultValue);
			}
		}
		
		return val;
	}
	
	/**
	 * Retrieves the defense value for a specific equipment slot.<br>
	 * It checks if a {@code TransformTemplate} exists for the player.<br>
	 * If a template is found, it returns the modified defense value.<br>
	 * Otherwise, it returns the default base defense from the player's template.
	 * @param player The {@code Player} instance to check.
	 * @param slot The equipment slot index.
	 * @return The calculated defense value for the given slot.
	 */
	public int getBaseDefBySlot(Player player, int slot)
	{
		final int defaultValue = player.getTemplate().getBaseDefBySlot(slot);
		if (isVisualOnly())
		{
			return defaultValue;
		}
		
		final TransformTemplate template = getTemplate(player);
		return template == null ? defaultValue : template.getDefense(slot, defaultValue);
	}
	
	/**
	 * Calculates the level modifier for a specific creature.<br>
	 * This method retrieves the modifier from the transform template based on the creature's current level.<br>
	 * It returns 1.0 if no specific data is found.
	 * @param creature The {@code Creature} to check for modifiers.
	 * @return The level modifier as a {@code double}.
	 */
	public double getLevelMod(Creature creature)
	{
		if (isVisualOnly())
		{
			return 1;
		}
		
		double val = 1;
		final TransformTemplate template = getTemplate(creature);
		if (template != null)
		{
			final TransformLevelData data = template.getData(creature.getLevel());
			if (data != null)
			{
				val = data.getLevelMod();
			}
		}
		
		return val;
	}
}
