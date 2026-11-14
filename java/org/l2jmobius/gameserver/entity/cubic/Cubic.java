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
package org.l2jmobius.gameserver.entity.cubic;

import java.util.Comparator;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.CubicTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExDamagePopUp;
import org.l2jmobius.gameserver.network.serverpackets.ExMagicAttackInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoCubic;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a cubic entity within the game world.<br>
 * This class handles the behavior and logic for {@code Cubic} type creatures.
 * @author UnAfraid, Mobius
 */
public class Cubic extends Creature
{
	private final Player _owner;
	private final Player _caster;
	private final CubicTemplate _template;
	private ScheduledFuture<?> _skillUseTask;
	private ScheduledFuture<?> _expireTask;
	
	/**
	 * Creates a new {@code Cubic} instance.<br>
	 * This constructor initializes the cubic with its owner and caster.<br>
	 * It also sets the instance type to {@code InstanceType.Cubic}.<br>
	 * The cubic is activated immediately after creation.
	 * @param owner The player who owns the cubic.
	 * @param caster The player who cast the cubic, or the owner if null.
	 * @param template The {@code CubicTemplate} used to define the cubic properties.
	 */
	public Cubic(Player owner, Player caster, CubicTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Cubic);
		_owner = owner;
		_caster = caster == null ? owner : caster;
		_template = template;
		activate();
	}
	
	/**
	 * Starts the cubic's active lifecycle.<br>
	 * Schedules the {@code readyToUseSkill} task at a fixed rate based on the template delay.<br>
	 * Schedules the {@code deactivate} task to run after the template duration expires.
	 */
	private void activate()
	{
		_skillUseTask = ThreadPool.scheduleAtFixedRate(this::readyToUseSkill, 0, _template.getDelay() * 1000);
		_expireTask = ThreadPool.schedule(this::deactivate, _template.getDuration() * 1000);
	}
	
	/**
	 * Stops all active tasks for this cubic.<br>
	 * Cancels the skill use and expiration tasks.<br>
	 * Removes the cubic from the owner's list.<br>
	 * Updates the owner with the new status.
	 */
	public void deactivate()
	{
		if ((_skillUseTask != null) && !_skillUseTask.isDone())
		{
			_skillUseTask.cancel(true);
		}
		
		_skillUseTask = null;
		if ((_expireTask != null) && !_expireTask.isDone())
		{
			_expireTask.cancel(true);
		}
		
		_expireTask = null;
		_owner.getCubicList().remove(_template.getId());
		_owner.sendPacket(new ExUserInfoCubic(_owner));
		_owner.broadcastCharInfo();
	}
	
	/**
	 * Determines the appropriate action based on the target type of the template.<br>
	 * It checks if the cubic should attack a target, use a skill, heal, or act toward the master.
	 */
	private void readyToUseSkill()
	{
		switch (_template.getTargetType())
		{
			case TARGET:
			{
				actionToCurrentTarget();
				break;
			}
			case BY_SKILL:
			{
				actionToTargetBySkill();
				break;
			}
			case HEAL:
			{
				actionHeal();
				break;
			}
			case MASTER:
			{
				actionToMaster();
				break;
			}
		}
	}
	
	/**
	 * Selects a {@code CubicSkill} based on weighted probabilities.<br>
	 * It iterates through all skills in the template and picks one using their trigger rates.<br>
	 * If no skill is selected, it returns {@code null}.
	 * @return The selected {@code CubicSkill} or {@code null} if none were triggered.
	 */
	private CubicSkill chooseSkill()
	{
		final double random = Rnd.nextDouble() * 100;
		double commulativeChance = 0;
		for (CubicSkill cubicSkill : _template.getCubicSkills())
		{
			if ((commulativeChance += cubicSkill.getTriggerRate()) > random)
			{
				return cubicSkill;
			}
		}
		
		return null;
	}
	
	/**
	 * Executes the selected skill on the current target.<br>
	 * This method retrieves the chosen {@code CubicSkill}.<br>
	 * It then identifies the target from the {@code _owner}.<br>
	 * If both are valid, it attempts to use the skill.
	 */
	private void actionToCurrentTarget()
	{
		final CubicSkill skill = chooseSkill();
		final WorldObject target = _owner.getTarget();
		if ((skill != null) && (target != null))
		{
			tryToUseSkill(target, skill);
		}
	}
	
	/**
	 * Executes the selected cubic skill based on its target type.<br>
	 * It determines whether to use a skill on a target, heal, or affect the master.<br>
	 * The method calls {@code chooseSkill()} to retrieve the active skill.
	 */
	private void actionToTargetBySkill()
	{
		final CubicSkill skill = chooseSkill();
		if (skill != null)
		{
			switch (skill.getTargetType())
			{
				case TARGET:
				{
					final WorldObject target = _owner.getTarget();
					if (target != null)
					{
						tryToUseSkill(target, skill);
					}
					break;
				}
				case HEAL:
				{
					actionHeal();
					break;
				}
				case MASTER:
				{
					tryToUseSkill(_owner, skill);
					break;
				}
			}
		}
	}
	
	/**
	 * Executes the healing logic for the cubic entity.<br>
	 * It selects a random skill from the template based on trigger rates.<br>
	 * The method identifies the healthiest target in range to receive the heal.<br>
	 * It ensures that only living targets are healed.
	 */
	private void actionHeal()
	{
		final double random = Rnd.nextDouble() * 100;
		double commulativeChance = 0;
		for (CubicSkill cubicSkill : _template.getCubicSkills())
		{
			if ((commulativeChance += cubicSkill.getTriggerRate()) > random)
			{
				final Skill skill = cubicSkill.getSkill();
				if ((skill != null) && (Rnd.get(100) < cubicSkill.getSuccessRate()))
				{
					final Party party = _owner.getParty();
					Stream<Creature> stream;
					if (party != null)
					{
						stream = World.getVisibleObjectsInRange(_owner, Creature.class, PlayerConfig.ALT_PARTY_RANGE, c -> (c.getParty() == party) && _template.validateConditions(this, _owner, c) && cubicSkill.validateConditions(this, _owner, c)).stream();
					}
					else
					{
						stream = _owner.getServitorsAndPets().stream().filter(summon -> _template.validateConditions(this, _owner, summon) && cubicSkill.validateConditions(this, _owner, summon)).map(Creature.class::cast);
					}
					
					if (_template.validateConditions(this, _owner, _owner) && cubicSkill.validateConditions(this, _owner, _owner))
					{
						stream = Stream.concat(stream, Stream.of(_owner));
					}
					
					final Creature target = stream.sorted(Comparator.comparingInt(Creature::getCurrentHpPercent)).findFirst().orElse(null);
					if ((target != null) && (!target.isDead())) // Life Cubic should not try to heal dead targets.
					{
						if (Rnd.nextDouble() > (target.getCurrentHp() / target.getMaxHp()))
						{
							activateCubicSkill(skill, target);
						}
						break;
					}
				}
			}
		}
	}
	
	/**
	 * Executes an action directed towards the master of the {@code Cubic}.<br>
	 * It selects a random {@code CubicSkill} and attempts to use it on the owner.
	 */
	private void actionToMaster()
	{
		final CubicSkill skill = chooseSkill();
		if (skill != null)
		{
			tryToUseSkill(_owner, skill);
		}
	}
	
	/**
	 * Attempts to execute a specific {@code CubicSkill} on a target.<br>
	 * This method validates the conditions for using the skill.<br>
	 * It also checks if the success rate is met before activation.
	 * @param worldObject The initial object targeted by the action.
	 * @param cubicSkill The specific {@code CubicSkill} to be used.
	 */
	private void tryToUseSkill(WorldObject worldObject, CubicSkill cubicSkill)
	{
		WorldObject target = worldObject;
		final Skill skill = cubicSkill.getSkill();
		if ((_template.getTargetType() != CubicTargetType.MASTER) && !((_template.getTargetType() == CubicTargetType.BY_SKILL) && (cubicSkill.getTargetType() == CubicTargetType.MASTER)))
		{
			target = skill.getTarget(_owner, target, false, false, false);
		}
		
		if (target != null)
		{
			if (target.isDoor() && !cubicSkill.canUseOnStaticObjects())
			{
				return;
			}
			
			if (_template.validateConditions(this, _owner, target) && cubicSkill.validateConditions(this, _owner, target) && (Rnd.get(100) < cubicSkill.getSuccessRate()))
			{
				activateCubicSkill(skill, target);
			}
		}
	}
	
	/**
	 * Activates a specific {@code Skill} on a {@code WorldObject} target.<br>
	 * This method checks if the skill is ready for reuse before execution.<br>
	 * It broadcasts the skill packet and updates the owner's cooldown timer.
	 * @param skill The {@code Skill} to be activated.
	 * @param target The {@code WorldObject} that will receive the skill effect.
	 */
	private void activateCubicSkill(Skill skill, WorldObject target)
	{
		final int reuseDelay = skill.getReuseDelay();
		if (!_owner.hasSkillReuse(skill.getReuseHashCode()))
		{
			_caster.broadcastSkillPacket(new MagicSkillUse(_owner, target, skill.getDisplayId(), skill.getDisplayLevel(), skill.getHitTime(), reuseDelay), target);
			skill.activateSkill(this, target);
			_owner.addTimeStamp(skill, reuseDelay);
		}
	}
	
	/**
	 * Sends a damage notification to the owner of this cubic.<br>
	 * It handles olympiad notifications and block messages.<br>
	 * If the attack misses, no message is sent.
	 * @param target The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} used to perform the action.
	 * @param damage The amount of damage dealt.
	 * @param crit Whether the hit was a critical strike.
	 * @param miss Whether the attack failed to hit the target.
	 */
	@Override
	public void sendDamageMessage(Creature target, Skill skill, int damage, boolean crit, boolean miss)
	{
		if (miss || (_owner == null))
		{
			sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.DODGE, skill));
			
			// target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.DODGE));
			return;
		}
		
		if (_owner.isInOlympiadMode() && target.isPlayer() && target.asPlayer().isInOlympiadMode() && (target.asPlayer().getOlympiadGameId() == _owner.getOlympiadGameId()))
		{
			OlympiadGameManager.getInstance().notifyCompetitorDamage(_owner, damage);
		}
		
		if (target.isHpBlocked() && !target.isNpc())
		{
			if (skill == null)
			{
				sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.IMMUNE, skill));
				// target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.IMMUNE));
			}
			else
			{
				sendPacket(new ExMagicAttackInfo(getObjectId(), target.getObjectId(), ExMagicAttackInfo.RESISTED));
				// target.sendPacket(new ExMagicAttackInfo(getObjectId(), target.getObjectId(), ExMagicAttackInfo.RESISTED));
			}
			
			_owner.sendPacket(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
		}
		else
		{
			final int objectId = getObjectId();
			final int targetObjectId = target.getObjectId();
			if (crit)
			{
				sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.CRITICAL, skill));
				target.sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.CRITICAL, skill));
			}
			else if (skill != null)
			{
				sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.SKILL_HIT, skill));
				target.sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.SKILL_HIT, skill));
			}
			else
			{
				sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.NORMAL_ATTACK, skill));
				target.sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.NORMAL_ATTACK, skill));
			}
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_DEALT_S3_DAMAGE_TO_C2);
			sm.addString(getName());
			sm.addString(target.getName());
			sm.addInt(damage);
			sm.addPopup(targetObjectId, _owner.getObjectId(), (damage * -1));
			_owner.sendPacket(sm);
		}
	}
	
	/**
	 * Sends a {@code ServerPacket} to the owner of this cubic.<br>
	 * This method checks if the owner is not {@code null} before sending.
	 * @param packet The {@code ServerPacket} to be sent.
	 */
	@Override
	public void sendPacket(ServerPacket packet)
	{
		if (_owner != null)
		{
			_owner.sendPacket(packet);
		}
	}
	
	/**
	 * Returns the {@code Player} that owns this cubic.
	 * @return the owner of the cubic as a {@code Player} object.
	 */
	@Override
	public Player asPlayer()
	{
		return _owner;
	}
	
	/**
	 * Retrieves the creature that cast this cubic.
	 * @return the {@code Creature} who performed the casting.
	 */
	public Creature getCaster()
	{
		return _caster;
	}
	
	/**
	 * Checks if the cubic was cast by someone other than its owner.<br>
	 * This is useful for determining if a cubic belongs to another player.
	 * @return {@code true} if the caster is not the owner, {@code false} otherwise.
	 */
	public boolean isGivenByOther()
	{
		return _caster != _owner;
	}
	
	/**
	 * Returns the name of the owner.<br>
	 * This method retrieves the {@code String} name from the associated {@code Player}.
	 * @return The name of the owner as a {@code String}.
	 */
	@Override
	public String getName()
	{
		return _owner.getName();
	}
	
	/**
	 * Retrieves the level of the owner.<br>
	 * This method returns the {@code int} value from the owner's level property.
	 * @return The current level of the owner.
	 */
	@Override
	public int getLevel()
	{
		return _owner.getLevel();
	}
	
	/**
	 * Retrieves the X coordinate of the owner.<br>
	 * This value is obtained from the {@code Player} object.
	 * @return the X coordinate as an {@code int}.
	 */
	@Override
	public int getX()
	{
		return _owner.getX();
	}
	
	/**
	 * Retrieves the Y coordinate of the owner.
	 * @return the Y coordinate as an {@code int}.
	 */
	@Override
	public int getY()
	{
		return _owner.getY();
	}
	
	/**
	 * Retrieves the Z coordinate of the owner.<br>
	 * This value represents the height of the {@code Player}.
	 * @return the Z coordinate as an {@code int}.
	 */
	@Override
	public int getZ()
	{
		return _owner.getZ();
	}
	
	/**
	 * Retrieves the heading of the owner.<br>
	 * This value is obtained from the {@code Player} object.
	 * @return The current heading as an {@code int}.
	 */
	@Override
	public int getHeading()
	{
		return _owner.getHeading();
	}
	
	/**
	 * Retrieves the unique instance identifier of the owner.<br>
	 * This value is obtained from the {@code Player} object that owns this cubic.
	 * @return The unique ID of the owner's instance.
	 */
	@Override
	public int getInstanceId()
	{
		return _owner.getInstanceId();
	}
	
	/**
	 * Checks if the owner of this cubic is currently inside an instance.
	 * @return {@code true} if the owner is in an instance, {@code false} otherwise.
	 */
	@Override
	public boolean isInInstance()
	{
		return _owner.isInInstance();
	}
	
	/**
	 * Retrieves the {@code Instance} where the owner of this cubic is located.
	 * @return the {@code Instance} associated with the owner.
	 */
	@Override
	public Instance getInstanceWorld()
	{
		return _owner.getInstanceWorld();
	}
	
	/**
	 * Retrieves the current location of the owner.
	 * @return the {@code Location} of the owner.
	 */
	@Override
	public Location getLocation()
	{
		return _owner.getLocation();
	}
	
	/**
	 * Calculates a random damage multiplier based on the owner's stats.<br>
	 * It uses the {@code Stat.RANDOM_DAMAGE} value to determine the range.<br>
	 * The result is returned as a {@code double}.
	 * @return A random damage multiplier.
	 */
	@Override
	public double getRandomDamageMultiplier()
	{
		final int random = (int) _owner.getStat().getValue(Stat.RANDOM_DAMAGE);
		return (1 + ((double) Rnd.get(-random, random) / 100));
	}
	
	/**
	 * Retrieves the magic accuracy of the owner.<br>
	 * This value is taken directly from the {@code Player} object.
	 * @return the magic accuracy as an {@code int}.
	 */
	@Override
	public int getMagicAccuracy()
	{
		return _owner.getMagicAccuracy();
	}
	
	/**
	 * Retrieves the {@code CubicTemplate} associated with this object.
	 * @return The {@code CubicTemplate} used by this instance.
	 */
	@Override
	public CubicTemplate getTemplate()
	{
		return _template;
	}
	
	/**
	 * Retrieves the unique identifier for this cubic.<br>
	 * This value is obtained from the associated {@code CubicTemplate}.
	 * @return The integer ID of the cubic.
	 */
	@Override
	public int getId()
	{
		return _template.getId();
	}
	
	/**
	 * Retrieves the base physical attack value.<br>
	 * This value is obtained from the {@code CubicTemplate}.
	 * @return The base physical attack as an {@code int}.
	 */
	@Override
	public int getPAtk()
	{
		return _template.getBasePAtk();
	}
	
	/**
	 * Retrieves the base magic attack value.<br>
	 * This value is taken from the {@code CubicTemplate}.
	 * @return the base magic attack as an {@code int}.
	 */
	@Override
	public int getMAtk()
	{
		return _template.getBaseMAtk();
	}
	
	/**
	 * Retrieves the currently active weapon instance.<br>
	 * This method returns {@code null} if no weapon is currently active.
	 * @return The active {@code Item} instance or {@code null}.
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the weapon currently being used by this entity.<br>
	 * This method returns {@code null} if no weapon is active.
	 * @return the current {@code Weapon} object or {@code null}.
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon instance for this object.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Item} representing the secondary weapon, or {@code null}.
	 */
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the template for the secondary weapon.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code ItemTemplate} of the secondary weapon or {@code null}.
	 */
	@Override
	public ItemTemplate getSecondaryWeaponItem()
	{
		return null;
	}
	
	/**
	 * Checks if the specified creature can be attacked by this cubic.<br>
	 * This method currently always returns {@code false}.
	 * @param attacker The {@code Creature} attempting to attack.
	 * @return {@code false} regardless of the input.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}
	
	/**
	 * Spawns the cubic entity into the game world.
	 * @return {@code true} if the spawning was successful.
	 */
	@Override
	public boolean spawnMe()
	{
		return true;
	}
	
	/**
	 * This method is called when the {@code Cubic} object is first created.<br>
	 * It initializes the initial state of the entity.
	 */
	@Override
	public void onSpawn()
	{
	}
	
	/**
	 * Removes this object from the game world.<br>
	 * This method is used to clean up the entity.
	 * @return {@code true} if the deletion was successful.
	 */
	@Override
	public boolean deleteMe()
	{
		return true;
	}
	
	/**
	 * This method handles the decay logic for the cubic. <br>
	 * It currently always returns {@code true}.
	 * @return {@code true} if the decay was successful.
	 */
	@Override
	public boolean decayMe()
	{
		return true;
	}
	
	/**
	 * This method is called when the {@code Cubic} entity decays.<br>
	 * It handles the logic for removing the object from the world.
	 */
	@Override
	public void onDecay()
	{
	}
	
	/**
	 * This method is called when the entity is teleported.<br>
	 * It handles any necessary logic updates after a position change.
	 */
	@Override
	public synchronized void onTeleported()
	{
	}
	
	/**
	 * Sends information to the specified {@code Player}.<br>
	 * This method handles the network communication for the player.
	 * @param player The {@code Player} who will receive the information.
	 */
	@Override
	public void sendInfo(Player player)
	{
	}
	
	/**
	 * Checks if the owner of this cubic is currently invincible.<br>
	 * This method returns the invulnerability status of the {@code Player} who owns it.
	 * @return {@code true} if the owner is invincible, {@code false} otherwise.
	 */
	@Override
	public boolean isInvul()
	{
		return _owner.isInvul();
	}
	
	/**
	 * Checks if the cubic can be targeted by other entities.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if targetable, otherwise {@code false}.
	 */
	@Override
	public boolean isTargetable()
	{
		return false;
	}
	
	/**
	 * Checks if the cubic entity is immortal.<br>
	 * This method currently always returns {@code true}.
	 * @return {@code true} if the entity cannot die.
	 */
	@Override
	public boolean isUndying()
	{
		return true;
	}
	
	/**
	 * Checks if the current entity is a player.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the entity is a player.
	 */
	@Override
	public boolean isPlayer()
	{
		return true;
	}
	
	/**
	 * Checks if the current object is a cubic.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the object is a cubic.
	 */
	@Override
	public boolean isCubic()
	{
		return true;
	}
}
