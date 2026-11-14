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
package org.l2jmobius.gameserver.entity.actor.templates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.NpcStatMultipliersConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.entity.actor.enums.npc.DropType;
import org.l2jmobius.gameserver.entity.actor.enums.player.GroupType;
import org.l2jmobius.gameserver.entity.actor.enums.player.Sex;
import org.l2jmobius.gameserver.entity.actor.holders.npc.DropGroupHolder;
import org.l2jmobius.gameserver.entity.actor.holders.npc.DropHolder;
import org.l2jmobius.gameserver.entity.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.entity.actor.holders.npc.LimitedDropHolder;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.MathUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents the static data and configuration for a non-player character (NPC).<br>
 * This class serves as a blueprint used to initialize {@code Creature} instances in the game world.
 * @author NosBit, Nobius
 */
public class NpcTemplate extends CreatureTemplate
{
	private static final Logger LOGGER = Logger.getLogger(NpcTemplate.class.getName());
	
	private int _id;
	private int _displayId;
	private int _level;
	private String _type;
	private String _name;
	private boolean _usingServerSideName;
	private String _title;
	private boolean _usingServerSideTitle;
	// Empty rather than null, since templates built in code never reach the setter the xml path calls.
	private StatSet _parameters = StatSet.EMPTY_STATSET;
	private Sex _sex;
	private int _chestId;
	private int _rhandId;
	private int _lhandId;
	private int _weaponEnchant;
	private double _exp;
	private double _sp;
	private double _raidPoints;
	private boolean _unique;
	private boolean _attackable;
	private boolean _targetable;
	private boolean _talkable;
	private boolean _isQuestMonster;
	private boolean _undying;
	private boolean _showName;
	private boolean _randomWalk;
	private boolean _randomAnimation;
	private boolean _flying;
	private boolean _fakePlayer;
	private FakePlayerHolder _fakePlayerInfo;
	private boolean _canMove;
	private boolean _noSleepMode;
	private boolean _passableDoor;
	private boolean _hasSummoner;
	private boolean _canBeSown;
	private boolean _canBeCrt;
	private boolean _isDeathPenalty;
	private int _corpseTime;
	private AIType _aiType;
	private int _aggroRange;
	private int _clanHelpRange;
	private boolean _isChaos;
	private boolean _isAggressive;
	private int _soulShot;
	private int _spiritShot;
	private int _soulShotChance;
	private int _spiritShotChance;
	private int _minSkillChance;
	private int _maxSkillChance;
	private double _hitTimeFactor;
	private double _hitTimeFactorSkill;
	private int _baseAttackAngle;
	private Map<Integer, Skill> _skills;
	private Map<AISkillScope, List<Skill>> _aiSkillLists;
	private Set<Integer> _clans;
	private Set<Integer> _ignoreClanNpcIds;
	private List<DropGroupHolder> _dropGroups;
	private List<DropGroupHolder> _dropGroupRolls;
	private List<DropHolder> _dropListDeath;
	private List<DropHolder> _dropListSpoil;
	private List<LimitedDropHolder> _dropListLimited;
	private float _collisionRadiusGrown;
	private float _collisionHeightGrown;
	private int _manaRewardValue;
	private StatModifierType _manaRewardType;
	private int _manaRewardTicks;
	private GroupType _manaRewardGroup;
	
	/**
	 * Creates a new {@code NpcTemplate} using the provided statistics.<br>
	 * This constructor initializes the template with a specific {@code StatSet}.
	 * @param set The {@code StatSet} containing the initial attributes for this NPC.
	 */
	public NpcTemplate(StatSet set)
	{
		super(set);
	}
	
	/**
	 * Initializes the template data from a {@code StatSet}.<br>
	 * This method populates all internal fields like IDs, names, and stats.<br>
	 * It also applies specific multipliers based on the NPC type.
	 * @param set The {@code StatSet} containing the configuration data.
	 */
	@Override
	public void set(StatSet set)
	{
		super.set(set);
		_id = set.getInt("id");
		_displayId = set.getInt("displayId", _id);
		_level = set.getInt("level", 85);
		_type = set.getString("type", "Folk");
		_name = set.getString("name", "");
		_usingServerSideName = set.getBoolean("usingServerSideName", false);
		_title = set.getString("title", "");
		_usingServerSideTitle = set.getBoolean("usingServerSideTitle", false);
		setRace(set.getEnum("race", Race.class, Race.NONE));
		_sex = set.getEnum("sex", Sex.class, Sex.ETC);
		_chestId = set.getInt("chestId", 0);
		if ((_chestId > 0) && (ItemData.getInstance().getTemplate(_chestId) == null))
		{
			LOGGER.warning("NpcTemplate " + _id + ": Could not find item for chestId with id " + _chestId + ".");
		}
		
		_rhandId = set.getInt("rhandId", 0);
		if ((_rhandId > 0) && (ItemData.getInstance().getTemplate(_rhandId) == null))
		{
			LOGGER.warning("NpcTemplate " + _id + ": Could not find item for rhandId with id " + _rhandId + ".");
		}
		
		_lhandId = set.getInt("lhandId", 0);
		if ((_lhandId > 0) && (ItemData.getInstance().getTemplate(_lhandId) == null))
		{
			LOGGER.warning("NpcTemplate " + _id + ": Could not find item for lhandId with id " + _lhandId + ".");
		}
		
		_weaponEnchant = set.getInt("weaponEnchant", 0);
		_exp = set.getDouble("exp", 0);
		_sp = set.getDouble("sp", 0);
		_raidPoints = set.getDouble("raidPoints", 0);
		_unique = set.getBoolean("unique", false);
		_attackable = set.getBoolean("attackable", true);
		_targetable = set.getBoolean("targetable", true);
		_talkable = set.getBoolean("talkable", true);
		_isQuestMonster = _title.contains("Quest");
		_undying = set.getBoolean("undying", !_type.equals("Monster") && !_type.equals("RaidBoss") && !_type.equals("GrandBoss"));
		_showName = set.getBoolean("showName", true);
		_randomWalk = set.getBoolean("randomWalk", !_type.equals("Guard"));
		_randomAnimation = set.getBoolean("randomAnimation", true);
		_flying = set.getBoolean("flying", false);
		_fakePlayer = set.getBoolean("fakePlayer", false);
		if (_fakePlayer)
		{
			_fakePlayerInfo = new FakePlayerHolder(set);
			
			// Check if a character with the same name already exists.
			if (PlayerInfoTable.getInstance().getIdByName(_name) > 0)
			{
				LOGGER.info(getClass().getSimpleName() + ": Fake player id [" + _id + "] conflict. A real player with name [" + _name + "] already exists.");
			}
		}
		
		_canMove = (set.getDouble("baseWalkSpd", 1d) <= 0.1) || set.getBoolean("canMove", true);
		_noSleepMode = set.getBoolean("noSleepMode", false);
		_passableDoor = set.getBoolean("passableDoor", false);
		_hasSummoner = set.getBoolean("hasSummoner", false);
		_canBeSown = set.getBoolean("canBeSown", false);
		_canBeCrt = set.getBoolean("exCrtEffect", true);
		_isDeathPenalty = set.getBoolean("isDeathPenalty", false);
		_corpseTime = set.getInt("corpseTime", NpcConfig.DEFAULT_CORPSE_TIME);
		_aiType = set.getEnum("aiType", AIType.class, AIType.FIGHTER);
		_aggroRange = set.getInt("aggroRange", 0);
		_clanHelpRange = set.getInt("clanHelpRange", 0);
		_isChaos = set.getBoolean("isChaos", false);
		_isAggressive = set.getBoolean("isAggressive", false);
		_soulShot = set.getInt("soulShot", 0);
		_spiritShot = set.getInt("spiritShot", 0);
		_soulShotChance = set.getInt("soulShotChance", 0);
		_spiritShotChance = set.getInt("spiritShotChance", 0);
		_minSkillChance = set.getInt("minSkillChance", 7);
		_maxSkillChance = set.getInt("maxSkillChance", 15);
		_hitTimeFactor = set.getInt("hitTime", 100) / 100d;
		_hitTimeFactorSkill = set.getInt("hitTimeSkill", 100) / 100d;
		_baseAttackAngle = set.getInt("width", 120);
		_collisionRadiusGrown = set.getFloat("collisionRadiusGrown", 0);
		_collisionHeightGrown = set.getFloat("collisionHeightGrown", 0);
		_manaRewardValue = set.getInt("manaRewardValue", 0);
		_manaRewardType = set.getEnum("manaRewardType", StatModifierType.class, StatModifierType.DIFF);
		_manaRewardTicks = set.getInt("manaRewardTicks", 0);
		_manaRewardGroup = set.getEnum("manaRewardGroup", GroupType.class, GroupType.NONE);
		if (NpcStatMultipliersConfig.ENABLE_NPC_STAT_MULTIPLIERS) // Custom NPC Stat Multipliers
		{
			switch (_type)
			{
				case "Monster":
				{
					_baseValues.put(Stat.MAX_HP, getBaseHpMax() * NpcStatMultipliersConfig.MONSTER_HP_MULTIPLIER);
					_baseValues.put(Stat.MAX_MP, getBaseMpMax() * NpcStatMultipliersConfig.MONSTER_MP_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_ATTACK, getBasePAtk() * NpcStatMultipliersConfig.MONSTER_PATK_MULTIPLIER);
					_baseValues.put(Stat.MAGIC_ATTACK, getBaseMAtk() * NpcStatMultipliersConfig.MONSTER_MATK_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_DEFENCE, getBasePDef() * NpcStatMultipliersConfig.MONSTER_PDEF_MULTIPLIER);
					_baseValues.put(Stat.MAGICAL_DEFENCE, getBaseMDef() * NpcStatMultipliersConfig.MONSTER_MDEF_MULTIPLIER);
					_aggroRange *= NpcStatMultipliersConfig.MONSTER_AGRRO_RANGE_MULTIPLIER;
					_clanHelpRange *= NpcStatMultipliersConfig.MONSTER_CLAN_HELP_RANGE_MULTIPLIER;
					break;
				}
				case "RaidBoss":
				case "GrandBoss":
				{
					_baseValues.put(Stat.MAX_HP, getBaseHpMax() * NpcStatMultipliersConfig.RAIDBOSS_HP_MULTIPLIER);
					_baseValues.put(Stat.MAX_MP, getBaseMpMax() * NpcStatMultipliersConfig.RAIDBOSS_MP_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_ATTACK, getBasePAtk() * NpcStatMultipliersConfig.RAIDBOSS_PATK_MULTIPLIER);
					_baseValues.put(Stat.MAGIC_ATTACK, getBaseMAtk() * NpcStatMultipliersConfig.RAIDBOSS_MATK_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_DEFENCE, getBasePDef() * NpcStatMultipliersConfig.RAIDBOSS_PDEF_MULTIPLIER);
					_baseValues.put(Stat.MAGICAL_DEFENCE, getBaseMDef() * NpcStatMultipliersConfig.RAIDBOSS_MDEF_MULTIPLIER);
					_aggroRange *= NpcStatMultipliersConfig.RAIDBOSS_AGRRO_RANGE_MULTIPLIER;
					_clanHelpRange *= NpcStatMultipliersConfig.RAIDBOSS_CLAN_HELP_RANGE_MULTIPLIER;
					break;
				}
				case "Guard":
				{
					_baseValues.put(Stat.MAX_HP, getBaseHpMax() * NpcStatMultipliersConfig.GUARD_HP_MULTIPLIER);
					_baseValues.put(Stat.MAX_MP, getBaseMpMax() * NpcStatMultipliersConfig.GUARD_MP_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_ATTACK, getBasePAtk() * NpcStatMultipliersConfig.GUARD_PATK_MULTIPLIER);
					_baseValues.put(Stat.MAGIC_ATTACK, getBaseMAtk() * NpcStatMultipliersConfig.GUARD_MATK_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_DEFENCE, getBasePDef() * NpcStatMultipliersConfig.GUARD_PDEF_MULTIPLIER);
					_baseValues.put(Stat.MAGICAL_DEFENCE, getBaseMDef() * NpcStatMultipliersConfig.GUARD_MDEF_MULTIPLIER);
					_aggroRange *= NpcStatMultipliersConfig.GUARD_AGRRO_RANGE_MULTIPLIER;
					_clanHelpRange *= NpcStatMultipliersConfig.GUARD_CLAN_HELP_RANGE_MULTIPLIER;
					break;
				}
				case "Defender":
				{
					_baseValues.put(Stat.MAX_HP, getBaseHpMax() * NpcStatMultipliersConfig.DEFENDER_HP_MULTIPLIER);
					_baseValues.put(Stat.MAX_MP, getBaseMpMax() * NpcStatMultipliersConfig.DEFENDER_MP_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_ATTACK, getBasePAtk() * NpcStatMultipliersConfig.DEFENDER_PATK_MULTIPLIER);
					_baseValues.put(Stat.MAGIC_ATTACK, getBaseMAtk() * NpcStatMultipliersConfig.DEFENDER_MATK_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_DEFENCE, getBasePDef() * NpcStatMultipliersConfig.DEFENDER_PDEF_MULTIPLIER);
					_baseValues.put(Stat.MAGICAL_DEFENCE, getBaseMDef() * NpcStatMultipliersConfig.DEFENDER_MDEF_MULTIPLIER);
					_aggroRange *= NpcStatMultipliersConfig.DEFENDER_AGRRO_RANGE_MULTIPLIER;
					_clanHelpRange *= NpcStatMultipliersConfig.DEFENDER_CLAN_HELP_RANGE_MULTIPLIER;
					break;
				}
			}
		}
	}
	
	/**
	 * Retrieves the unique identifier for this template.<br>
	 * This value is used to identify the specific NPC in the game data.
	 * @return The {@code int} ID of the template.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the unique identifier used for display purposes.<br>
	 * This ID is often different from the internal database ID.
	 * @return the {@code int} value of the display ID.
	 */
	public int getDisplayId()
	{
		return _displayId;
	}
	
	/**
	 * Retrieves the level of the template.
	 * @return the current level as an {@code int}.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the type of the {@code NpcTemplate}.<br>
	 * This value identifies the category or classification of the NPC.
	 * @return The string representation of the NPC type.
	 */
	public String getType()
	{
		return _type;
	}
	
	/**
	 * Checks if the current template matches a specific type.<br>
	 * The comparison is case-insensitive.
	 * @param type The string type to compare against.
	 * @return {@code true} if the types match, {@code false} otherwise.
	 */
	public boolean isType(String type)
	{
		return _type.equalsIgnoreCase(type);
	}
	
	/**
	 * Retrieves the name of the template.
	 * @return The {@code String} name of this template.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Checks if the name is provided by the server.<br>
	 * Returns {@code true} if the server-side name is being used.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating if the server-side name is active.
	 */
	public boolean isUsingServerSideName()
	{
		return _usingServerSideName;
	}
	
	/**
	 * Retrieves the title of the NPC template.<br>
	 * This value is used to display a specific name or label for the entity.
	 * @return The {@code String} representing the title.
	 */
	public String getTitle()
	{
		return _title;
	}
	
	/**
	 * Checks if the title is managed by the server.<br>
	 * Returns {@code true} if the server side title is active.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating if the server-side title is being used.
	 */
	public boolean isUsingServerSideTitle()
	{
		return _usingServerSideTitle;
	}
	
	/**
	 * Retrieves the current set of parameters for this template.<br>
	 * This method returns the internal {@code StatSet} object.
	 * @return the {@code StatSet} containing the template parameters.
	 */
	public StatSet getParameters()
	{
		return _parameters;
	}
	
	/**
	 * Updates the internal parameters of this template.<br>
	 * This method assigns a new {@code StatSet} to the private field.
	 * @param set The {@code StatSet} containing the new parameters.
	 */
	public void setParameters(StatSet set)
	{
		_parameters = set;
	}
	
	/**
	 * Retrieves the sex of the entity.<br>
	 * This returns the {@code Sex} value associated with this template.
	 * @return the {@code Sex} of the entity.
	 */
	public Sex getSex()
	{
		return _sex;
	}
	
	/**
	 * Retrieves the unique identifier for the chest. <br>
	 * This value is used to identify specific chest types in the game.
	 * @return the {@code int} ID of the chest.
	 */
	public int getChestId()
	{
		return _chestId;
	}
	
	/**
	 * Retrieves the ID of the right-hand item.<br>
	 * This value is used to determine which object the entity holds in its right hand.
	 * @return The {@code int} identifier for the right-hand item.
	 */
	public int getRHandId()
	{
		return _rhandId;
	}
	
	/**
	 * Retrieves the unique identifier for the left-hand item.<br>
	 * This value is used to determine which object is held in the left hand.
	 * @return The {@code int} ID of the left-hand item.
	 */
	public int getLHandId()
	{
		return _lhandId;
	}
	
	/**
	 * Retrieves the enchantment level of the weapon.<br>
	 * This value represents the current enchantment status for the template.
	 * @return The integer value of the weapon enchantment.
	 */
	public int getWeaponEnchant()
	{
		return _weaponEnchant;
	}
	
	/**
	 * Retrieves the experience points for this template.
	 * @return the amount of {@code double} experience.
	 */
	public double getExp()
	{
		return _exp;
	}
	
	/**
	 * Retrieves the Soul Point value.<br>
	 * This method returns the current {@code SP} for the entity.
	 * @return The amount of Soul Points as a {@code double}.
	 */
	public double getSP()
	{
		return _sp;
	}
	
	/**
	 * Retrieves the raid points for this template.
	 * @return the current amount of {@code raidPoints}.
	 */
	public double getRaidPoints()
	{
		return _raidPoints;
	}
	
	/**
	 * Checks if this NPC template is unique.<br>
	 * Returns {@code true} if the entity is a unique monster.<br>
	 * Returns {@code false} otherwise.
	 * @return the uniqueness status of the template.
	 */
	public boolean isUnique()
	{
		return _unique;
	}
	
	/**
	 * Checks if this entity can be attacked by other actors.<br>
	 * Returns {@code true} if the entity is attackable.<br>
	 * Returns {@code false} if the entity cannot be attacked.
	 * @return a boolean indicating whether the entity is attackable.
	 */
	public boolean isAttackable()
	{
		return _attackable;
	}
	
	/**
	 * Checks if this entity can be targeted by other actors.<br>
	 * Returns {@code true} if the entity is targetable.<br>
	 * Returns {@code false} if the entity cannot be targeted.
	 * @return a boolean indicating whether the entity is targetable.
	 */
	public boolean isTargetable()
	{
		return _targetable;
	}
	
	/**
	 * Checks if the entity can be interacted with via dialogue.<br>
	 * Returns {@code true} if the entity is talkable.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating if the entity is talkable.
	 */
	public boolean isTalkable()
	{
		return _talkable;
	}
	
	/**
	 * Checks if this NPC template is designated as a quest monster.<br>
	 * This property determines if the entity belongs to specific quest requirements.
	 * @return {@code true} if it is a quest monster, {@code false} otherwise.
	 */
	public boolean isQuestMonster()
	{
		return _isQuestMonster;
	}
	
	/**
	 * Checks if the entity is an undying creature.<br>
	 * Undying creatures cannot be killed by normal means.
	 * @return {@code true} if the entity is undying, {@code false} otherwise.
	 */
	public boolean isUndying()
	{
		return _undying;
	}
	
	/**
	 * Checks if the name of this entity should be displayed.<br>
	 * Returns {@code true} if the name is visible.<br>
	 * Returns {@code false} if the name is hidden.
	 * @return a boolean indicating whether the name is shown.
	 */
	public boolean isShowName()
	{
		return _showName;
	}
	
	/**
	 * Checks if the random walk behavior is active for this NPC.<br>
	 * This determines if the entity moves in a random direction.
	 * @return {@code true} if random walk is enabled, {@code false} otherwise.
	 */
	public boolean isRandomWalkEnabled()
	{
		return _randomWalk && (getParameters().getInt("MoveArounding", 1) != 0);
	}
	
	/**
	 * Checks if random animations are enabled for this template.
	 * @return {@code true} if random animations are active, {@code false} otherwise.
	 */
	public boolean isRandomAnimationEnabled()
	{
		return _randomAnimation;
	}
	
	/**
	 * Checks if the entity is currently flying.
	 * @return {@code true} if the entity is flying, {@code false} otherwise.
	 */
	public boolean isFlying()
	{
		return _flying;
	}
	
	/**
	 * Checks if this entity is a fake player.<br>
	 * This is used to identify non-player actors that mimic player behavior.
	 * @return {@code true} if the entity is a fake player, {@code false} otherwise.
	 */
	public boolean isFakePlayer()
	{
		return _fakePlayer;
	}
	
	/**
	 * Retrieves the information for this fake player.<br>
	 * This method returns the {@code FakePlayerHolder} associated with this entity.
	 * @return the {@code FakePlayerHolder} object.
	 */
	public FakePlayerHolder getFakePlayerInfo()
	{
		return _fakePlayerInfo;
	}
	
	/**
	 * Checks if the entity is allowed to move.
	 * @return {@code true} if movement is enabled, {@code false} otherwise.
	 */
	public boolean canMove()
	{
		return _canMove;
	}
	
	/**
	 * Checks if the entity is in no sleep mode.<br>
	 * Returns {@code true} if the mode is active.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating the status of the no sleep mode.
	 */
	public boolean isNoSleepMode()
	{
		return _noSleepMode;
	}
	
	/**
	 * Checks if the door can be passed through by entities.
	 * @return {@code true} if the door is passable, {@code false} otherwise.
	 */
	public boolean isPassableDoor()
	{
		return _passableDoor;
	}
	
	/**
	 * Checks if the entity has a summoner.
	 * @return {@code true} if there is a summoner, {@code false} otherwise.
	 */
	public boolean hasSummoner()
	{
		return _hasSummoner;
	}
	
	/**
	 * Checks if the NPC can be sown.<br>
	 * This method returns the value of the {@code _canBeSown} flag.
	 * @return {@code true} if the NPC can be sown, {@code false} otherwise.
	 */
	public boolean canBeSown()
	{
		return _canBeSown;
	}
	
	/**
	 * Checks if the entity can be a Champion Monster.<br>
	 * This method returns {@code true} if the configuration allows it.
	 * @return {@code true} if it can be a champion monster, {@code false} otherwise.
	 */
	public boolean canBeCrt()
	{
		return _canBeCrt;
	}
	
	/**
	 * Checks if this NPC template results in a death penalty.<br>
	 * This is used to determine if players lose items or experience upon death.
	 * @return {@code true} if the NPC causes a death penalty, {@code false} otherwise.
	 */
	public boolean isDeathPenalty()
	{
		return _isDeathPenalty;
	}
	
	/**
	 * Retrieves the time remaining before a corpse disappears.<br>
	 * This value is used to determine how long an entity stays on the ground.
	 * @return The remaining time in seconds as an {@code int}.
	 */
	public int getCorpseTime()
	{
		return _corpseTime;
	}
	
	/**
	 * Retrieves the artificial intelligence type of this template.<br>
	 * This determines how the NPC behaves in the game world.
	 * @return the {@code AIType} associated with this entity.
	 */
	public AIType getAIType()
	{
		return _aiType;
	}
	
	/**
	 * Gets the distance at which an NPC will notice a player.<br>
	 * This value determines the detection radius for aggressive behavior.
	 * @return The aggro range as an {@code int}.
	 */
	public int getAggroRange()
	{
		return _aggroRange;
	}
	
	/**
	 * Retrieves the maximum range for clan help actions.<br>
	 * This value determines how far a player can assist others in their clan.
	 * @return The integer value of the clan help range.
	 */
	public int getClanHelpRange()
	{
		return _clanHelpRange;
	}
	
	/**
	 * Checks if the NPC template is marked as a chaos monster.<br>
	 * This property determines specific behaviors for chaotic entities.
	 * @return {@code true} if the entity is a chaos monster, {@code false} otherwise.
	 */
	public boolean isChaos()
	{
		return _isChaos;
	}
	
	/**
	 * Checks if the NPC template is aggressive.<br>
	 * Returns {@code true} if the entity attacks players automatically.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating the aggression status.
	 */
	public boolean isAggressive()
	{
		return _isAggressive;
	}
	
	/**
	 * Retrieves the current soul shot value.<br>
	 * This method returns the amount of {@code soulShot} associated with this template.
	 * @return The integer value of the soul shot.
	 */
	public int getSoulShot()
	{
		return _soulShot;
	}
	
	/**
	 * Retrieves the current value of the spirit shot.
	 * @return The {@code int} value for spirit shot.
	 */
	public int getSpiritShot()
	{
		return _spiritShot;
	}
	
	/**
	 * Retrieves the chance for a soul shot to occur.<br>
	 * This value is used to determine if a special attack triggers.
	 * @return The current soul shot chance as an {@code int}.
	 */
	public int getSoulShotChance()
	{
		return _soulShotChance;
	}
	
	/**
	 * Retrieves the chance for a spirit shot to occur.<br>
	 * This value is used to determine if a spirit shot effect triggers.
	 * @return The current spirit shot chance as an {@code int}.
	 */
	public int getSpiritShotChance()
	{
		return _spiritShotChance;
	}
	
	/**
	 * Retrieves the minimum chance for a skill to be used.<br>
	 * This value is stored in the {@code _minSkillChance} field.
	 * @return The minimum skill chance as an {@code int}.
	 */
	public int getMinSkillChance()
	{
		return _minSkillChance;
	}
	
	/**
	 * Retrieves the maximum skill chance for this template.<br>
	 * This value represents the upper limit of the skill activation probability.
	 * @return The maximum skill chance as an {@code int}.
	 */
	public int getMaxSkillChance()
	{
		return _maxSkillChance;
	}
	
	/**
	 * Retrieves the hit time factor for this template.<br>
	 * This value determines how quickly an entity can perform attacks.
	 * @return the current {@code double} value of the hit time factor.
	 */
	public double getHitTimeFactor()
	{
		return _hitTimeFactor;
	}
	
	/**
	 * Retrieves the hit time factor for skills.<br>
	 * This value determines how quickly a skill's animation or execution occurs.
	 * @return The current {@code double} value of the hit time factor.
	 */
	public double getHitTimeFactorSkill()
	{
		return _hitTimeFactorSkill;
	}
	
	/**
	 * Retrieves the base attack angle for this template.<br>
	 * This value is used to determine the default direction of an attack.
	 * @return the base attack angle as an {@code int}.
	 */
	public int getBaseAttackAngle()
	{
		return _baseAttackAngle;
	}
	
	/**
	 * Retrieves the list of skills associated with this template.
	 * @return A {@code Map} where the key is an {@code Integer} ID and the value is a {@code Skill} object.
	 */
	@Override
	public Map<Integer, Skill> getSkills()
	{
		return _skills;
	}
	
	/**
	 * Sets the collection of skills for this template.<br>
	 * If the provided map is {@code null}, an empty map will be used instead.<br>
	 * The internal map is made unmodifiable to ensure data integrity.
	 * @param skills A map where the key is an {@code Integer} and the value is a {@code Skill}.
	 */
	public void setSkills(Map<Integer, Skill> skills)
	{
		_skills = skills != null ? Collections.unmodifiableMap(skills) : Collections.emptyMap();
	}
	
	/**
	 * Retrieves the list of skills for a specific AI scope.<br>
	 * This method returns an empty {@code List} if no skills are found for the given scope.
	 * @param aiSkillScope The {@code AISkillScope} to filter the skills by.
	 * @return A {@code List} of {@code Skill} objects matching the scope.
	 */
	public List<Skill> getAISkills(AISkillScope aiSkillScope)
	{
		return _aiSkillLists.getOrDefault(aiSkillScope, Collections.emptyList());
	}
	
	/**
	 * Sets the list of skills for different AI scopes.<br>
	 * This method assigns a map where each {@code AISkillScope} is linked to a {@code List} of {@code Skill} objects.<br>
	 * If the provided map is {@code null}, it will be initialized as an empty map.
	 * @param aiSkillLists The map containing skills for various AI scopes.
	 */
	public void setAISkillLists(Map<AISkillScope, List<Skill>> aiSkillLists)
	{
		_aiSkillLists = aiSkillLists != null ? Collections.unmodifiableMap(aiSkillLists) : Collections.emptyMap();
	}
	
	/**
	 * Retrieves the set of clans associated with this template.
	 * @return a {@code Set<Integer>} containing the clan IDs.
	 */
	public Set<Integer> getClans()
	{
		return _clans;
	}
	
	/**
	 * Retrieves the amount of MP granted as a mana reward when this NPC dies.
	 * @return The mana reward value, 0 when this NPC gives no reward.
	 */
	public int getManaRewardValue()
	{
		return _manaRewardValue;
	}
	
	/**
	 * Retrieves how the mana reward value is interpreted.<br>
	 * {@code PER} grants a percentage of the receiver's maximum MP, {@code DIFF} a fixed amount.
	 * @return The {@code StatModifierType} of the mana reward.
	 */
	public StatModifierType getManaRewardType()
	{
		return _manaRewardType;
	}
	
	/**
	 * Retrieves the number of ticks over which the mana reward is granted.
	 * @return The mana reward tick count.
	 */
	public int getManaRewardTicks()
	{
		return _manaRewardTicks;
	}
	
	/**
	 * Retrieves which group of players receives the mana reward.<br>
	 * {@code NONE} rewards the killer alone, {@code PARTY} the party, {@code COMMAND_CHANNEL} the command channel.
	 * @return The {@code GroupType} of the mana reward.
	 */
	public GroupType getManaRewardGroup()
	{
		return _manaRewardGroup;
	}
	
	/**
	 * Sets the collection of clans associated with this template.<br>
	 * If the provided {@code Set<Integer>} is {@code null}, the internal field will be set to {@code null}.<br>
	 * The input set is wrapped in an unmodifiable view to prevent external changes.
	 * @param clans The set of clan IDs to assign.
	 */
	public void setClans(Set<Integer> clans)
	{
		_clans = clans != null ? Collections.unmodifiableSet(clans) : null;
	}
	
	/**
	 * Checks if the NPC belongs to any of the specified clans.<br>
	 * It compares the clan IDs against a set of allowed clans.
	 * @param clanName The primary name of the clan to check.
	 * @param clanNames A variable number of additional clan names to check.
	 * @return {@code true} if any provided clan matches an allowed clan, otherwise {@code false}.
	 */
	public boolean isClan(String clanName, String... clanNames)
	{
		// Using local variable for the sake of reloading since it can be turned to null.
		final Set<Integer> clans = _clans;
		if (clans == null)
		{
			return false;
		}
		
		int clanId = NpcData.getInstance().getGenericClanId();
		if (clans.contains(clanId))
		{
			return true;
		}
		
		clanId = NpcData.getInstance().getClanId(clanName);
		if (clans.contains(clanId))
		{
			return true;
		}
		
		for (String name : clanNames)
		{
			clanId = NpcData.getInstance().getClanId(name);
			if (clans.contains(clanId))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the NPC belongs to any of the specified clans.<br>
	 * It compares the provided {@code Set<Integer>} against the internal clan list.
	 * @param clans A set of clan IDs to check against.
	 * @return {@code true} if a match is found, otherwise {@code false}.
	 */
	public boolean isClan(Set<Integer> clans)
	{
		// Using local variable for the sake of reloading since it can be turned to null.
		final Set<Integer> clanSet = _clans;
		if ((clanSet == null) || (clans == null))
		{
			return false;
		}
		
		final int clanId = NpcData.getInstance().getGenericClanId();
		if (clanSet.contains(clanId))
		{
			return true;
		}
		
		for (Integer id : clans)
		{
			if (clanSet.contains(id))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves the set of NPC IDs that should be ignored by clans.<br>
	 * These IDs are used to filter specific NPCs from clan-related logic.
	 * @return a {@code Set<Integer>} containing the ignored NPC IDs.
	 */
	public Set<Integer> getIgnoreClanNpcIds()
	{
		return _ignoreClanNpcIds;
	}
	
	/**
	 * Checks if the list of ignored clan NPC IDs is defined.<br>
	 * Returns {@code true} if the list exists.<br>
	 * Returns {@code false} if the list is {@code null}.
	 * @return {@code true} if there are ignored clan NPC IDs, otherwise {@code false}.
	 */
	public boolean hasIgnoreClanNpcIds()
	{
		return _ignoreClanNpcIds != null;
	}
	
	/**
	 * Sets the list of NPC IDs that should be ignored by clan mechanics.<br>
	 * This method updates the internal {@code _ignoreClanNpcIds} collection.<br>
	 * If the provided set is {@code null}, the internal field will also be set to {@code null}.
	 * @param ignoreClanNpcIds A {@code Set} of {@code Integer} IDs to exclude from clan logic.
	 */
	public void setIgnoreClanNpcIds(Set<Integer> ignoreClanNpcIds)
	{
		_ignoreClanNpcIds = ignoreClanNpcIds != null ? Collections.unmodifiableSet(ignoreClanNpcIds) : null;
	}
	
	/**
	 * Clears the drop groups associated with this template.<br>
	 * Sets the {@code _dropGroups} field to {@code null}.
	 */
	public void removeDropGroups()
	{
		_dropGroups = null;
		_dropGroupRolls = null;
	}
	
	/**
	 * Clears all drop lists for death, spoil, and limited events.<br>
	 * This method sets {@code _dropListDeath}, {@code _dropListSpoil}, and {@code _dropListLimited} to {@code null}.
	 */
	public void removeDrops()
	{
		_dropListDeath = null;
		_dropListSpoil = null;
		_dropListLimited = null;
	}
	
	/**
	 * Sets the list of drop groups for this template.<br>
	 * This method updates the {@code _dropGroups} field with the provided collection.
	 * @param groups The list of {@code DropGroupHolder} objects to set.
	 */
	public void setDropGroups(List<DropGroupHolder> groups)
	{
		_dropGroups = groups;
		_dropGroupRolls = groups != null ? expandRolls(groups) : null;
	}
	
	/**
	 * Expands the drop groups whose chance exceeds 100 into full rolls of 100 plus a last roll at the rest.<br>
	 * Retail keeps such chances as several rolls of the same group, each roll picking one of its items.
	 * @param groups The list of {@code DropGroupHolder} objects as the template declares them.
	 * @return The groups to roll on a kill.
	 */
	private static List<DropGroupHolder> expandRolls(List<DropGroupHolder> groups)
	{
		final List<DropGroupHolder> rolls = new ArrayList<>(groups.size());
		for (DropGroupHolder group : groups)
		{
			if (group.getChance() <= 100)
			{
				rolls.add(group);
				continue;
			}
			
			for (double chance = group.getChance(); chance > 0; chance -= 100)
			{
				final DropGroupHolder roll = new DropGroupHolder(Math.min(chance, 100));
				for (DropHolder drop : group.getDropList())
				{
					roll.addDrop(drop);
				}
				
				rolls.add(roll);
			}
		}
		
		return rolls;
	}
	
	/**
	 * Adds a new drop to the death loot list.<br>
	 * This method ensures that the {@code _dropListDeath} list is initialized before adding the item.
	 * @param dropHolder The {@code DropHolder} object to be added to the loot list.
	 */
	public void addDrop(DropHolder dropHolder)
	{
		if (_dropListDeath == null)
		{
			_dropListDeath = new ArrayList<>(1);
		}
		
		_dropListDeath.add(dropHolder);
	}
	
	/**
	 * Adds a specific drop to the spoil list.<br>
	 * This method ensures the internal list is initialized before adding the {@code DropHolder}.
	 * @param dropHolder The {@code DropHolder} object to be added.
	 */
	public void addSpoil(DropHolder dropHolder)
	{
		if (_dropListSpoil == null)
		{
			_dropListSpoil = new ArrayList<>(1);
		}
		
		_dropListSpoil.add(dropHolder);
	}
	
	/**
	 * Sets the list of limited drops for this template.<br>
	 * This method updates the {@code _dropListLimited} field with the provided collection.
	 * @param dropListLimited The list of {@code LimitedDropHolder} objects to set.
	 */
	public void setLimitedDropList(List<LimitedDropHolder> dropListLimited)
	{
		_dropListLimited = dropListLimited;
	}
	
	/**
	 * Retrieves the list of drop groups for this template.
	 * @return a {@code List} of {@code DropGroupHolder} objects.
	 */
	public List<DropGroupHolder> getDropGroups()
	{
		return _dropGroups;
	}
	
	/**
	 * Retrieves the list of items that can be dropped by this NPC.<br>
	 * This method returns the internal collection of {@code DropHolder} objects.
	 * @return a {@code List} of {@code DropHolder} objects.
	 */
	public List<DropHolder> getDropList()
	{
		return _dropListDeath;
	}
	
	/**
	 * Retrieves the list of items that this NPC drops.<br>
	 * This method returns the internal collection of {@code DropHolder} objects.
	 * @return a {@code List} of {@code DropHolder} objects.
	 */
	public List<DropHolder> getSpoilList()
	{
		return _dropListSpoil;
	}
	
	/**
	 * Retrieves the list of limited drops for this template.
	 * @return a {@code Collection} of {@code LimitedDropHolder} objects.
	 */
	public Collection<LimitedDropHolder> getLimitedDropList()
	{
		return _dropListLimited;
	}
	
	/**
	 * Calculates the items dropped by a creature upon death or spoil.<br>
	 * This method determines which items are awarded based on the specified {@code DropType}.<br>
	 * It handles both grouped and ungrouped loot logic.
	 * @param dropType The type of drop to calculate, such as {@code DROP} or {@code SPOIL}.
	 * @param victim The creature that died.
	 * @param killer The creature that killed the victim.
	 * @return A list of {@code ItemHolder} objects containing the dropped items, or {@code null} if no drops occur.
	 */
	public List<ItemHolder> calculateDrops(DropType dropType, Creature victim, Creature killer)
	{
		if (dropType == DropType.DROP)
		{
			// calculate group drops
			List<ItemHolder> groupDrops = null;
			if (_dropGroups != null)
			{
				groupDrops = calculateGroupDrops(victim, killer);
			}
			
			// calculate ungrouped drops
			List<ItemHolder> ungroupedDrops = null;
			if (_dropListDeath != null)
			{
				ungroupedDrops = calculateUngroupedDrops(dropType, victim, killer);
			}
			
			// return results
			if ((groupDrops != null) && (ungroupedDrops != null))
			{
				groupDrops.addAll(ungroupedDrops);
				ungroupedDrops.clear();
				return groupDrops;
			}
			
			if (groupDrops != null)
			{
				return groupDrops;
			}
			
			if (ungroupedDrops != null)
			{
				return ungroupedDrops;
			}
		}
		else if ((dropType == DropType.SPOIL) && (_dropListSpoil != null))
		{
			return calculateUngroupedDrops(dropType, victim, killer);
		}
		
		// no drops
		return null;
	}
	
	/**
	 * Calculates the list of items to drop based on the victim and killer.<br>
	 * This method handles level differences, raid status, and premium bonuses.<br>
	 * It also processes champion rewards and custom drop rates.
	 * @param victim The {@code Creature} that was killed.
	 * @param killer The {@code Creature} that performed the kill.
	 * @return A {@code List} of {@code ItemHolder} objects to be dropped.
	 */
	private List<ItemHolder> calculateGroupDrops(Creature victim, Creature killer)
	{
		// level difference calculations
		final int levelDifference = victim.getLevel() - killer.getLevel();
		final double levelGapChanceToDropAdena = MathUtil.scaleToRange(levelDifference, -RatesConfig.DROP_ADENA_MAX_LEVEL_DIFFERENCE, -RatesConfig.DROP_ADENA_MIN_LEVEL_DIFFERENCE, RatesConfig.DROP_ADENA_MIN_LEVEL_GAP_CHANCE, 100d);
		final double levelGapChanceToDrop = MathUtil.scaleToRange(levelDifference, -RatesConfig.DROP_ITEM_MAX_LEVEL_DIFFERENCE, -RatesConfig.DROP_ITEM_MIN_LEVEL_DIFFERENCE, RatesConfig.DROP_ITEM_MIN_LEVEL_GAP_CHANCE, 100d);
		
		List<ItemHolder> calculatedDrops = null;
		int dropOccurrenceCounter = victim.isRaid() ? RatesConfig.DROP_MAX_OCCURRENCES_RAIDBOSS : RatesConfig.DROP_MAX_OCCURRENCES_NORMAL;
		if (dropOccurrenceCounter > 0)
		{
			final Player player = killer.asPlayer();
			List<ItemHolder> randomDrops = null;
			ItemHolder cachedItem = null;
			double totalChance; // total group chance is 100
			for (DropGroupHolder group : _dropGroupRolls)
			{
				totalChance = 0;
				GROUP_DROP: for (DropHolder dropItem : group.getDropList())
				{
					final int itemId = dropItem.getItemId();
					final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
					final boolean champion = victim.isChampion();
					
					// chance
					double rateChance = 1;
					if (RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId) != null)
					{
						rateChance *= RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId);
						if (champion && (itemId == Inventory.ADENA_ID))
						{
							rateChance *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_CHANCE;
						}
						
						if ((itemId == Inventory.ADENA_ID) && (rateChance > 100))
						{
							rateChance = 100;
						}
					}
					else if (item.hasExImmediateEffect())
					{
						rateChance *= RatesConfig.RATE_HERB_DROP_CHANCE_MULTIPLIER;
					}
					else if (victim.isRaid())
					{
						rateChance *= RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
					}
					else
					{
						rateChance *= RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_CHANCE : 1);
					}
					
					// premium chance
					if (player != null)
					{
						if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
						{
							if (PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId) != null)
							{
								rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId);
							}
							else if (item.hasExImmediateEffect())
							{
								// TODO: Premium herb chance? :)
							}
							else if (victim.isRaid())
							{
								// TODO: Premium raid chance? :)
							}
							else
							{
								rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE;
							}
						}
						
						// bonus drop rate effect
						rateChance *= player.getStat().getMul(Stat.BONUS_DROP_RATE, 1);
					}
					
					// only use total chance on x1, custom rates break this logic because total chance is more than 100%
					if (rateChance == 1)
					{
						totalChance += dropItem.getChance();
					}
					else
					{
						totalChance = dropItem.getChance();
					}
					
					final double groupItemChance = totalChance * (group.getChance() / 100) * rateChance;
					
					// Check if maximum drop occurrences have been reached, as items with a 100% drop chance without server rate multipliers drop normally.
					if ((dropOccurrenceCounter == 0) && (groupItemChance < 100) && (randomDrops != null) && (calculatedDrops != null))
					{
						if ((rateChance == 1) && !randomDrops.isEmpty()) // custom rates break this logic because total chance is more than 100%
						{
							// remove highest chance item (temporarily if no other item replaces it)
							cachedItem = randomDrops.remove(0);
							calculatedDrops.remove(cachedItem);
						}
						
						dropOccurrenceCounter = 1;
					}
					
					// check level gap that may prevent to drop item
					if ((Rnd.nextDouble() * 100) > (dropItem.getItemId() == Inventory.ADENA_ID ? levelGapChanceToDropAdena : levelGapChanceToDrop))
					{
						continue GROUP_DROP;
					}
					
					// calculate chances
					final ItemHolder drop = calculateGroupDrop(group, dropItem, victim, killer, groupItemChance);
					if (drop == null)
					{
						continue GROUP_DROP;
					}
					
					// create lists
					if (randomDrops == null)
					{
						randomDrops = new ArrayList<>(dropOccurrenceCounter);
					}
					
					if (calculatedDrops == null)
					{
						calculatedDrops = new ArrayList<>(dropOccurrenceCounter);
					}
					
					// finally
					final Float itemChance = RatesConfig.RATE_DROP_CHANCE_BY_ID.get(dropItem.getItemId());
					if (itemChance != null)
					{
						if ((groupItemChance * itemChance) < 100)
						{
							dropOccurrenceCounter--;
							if (rateChance == 1) // custom rates break this logic because total chance is more than 100%
							{
								randomDrops.add(drop);
							}
						}
					}
					else if (groupItemChance < 100)
					{
						dropOccurrenceCounter--;
						if (rateChance == 1) // custom rates break this logic because total chance is more than 100%
						{
							randomDrops.add(drop);
						}
					}
					
					calculatedDrops.add(drop);
					
					// no more drops from this group, only use on x1, custom rates break this logic because total chance is more than 100%
					if (rateChance == 1)
					{
						break GROUP_DROP;
					}
				}
			}
			
			// add temporarily removed item when not replaced
			if ((dropOccurrenceCounter > 0) && (cachedItem != null) && (calculatedDrops != null))
			{
				calculatedDrops.add(cachedItem);
			}
			
			// clear random drops
			if (randomDrops != null)
			{
				randomDrops.clear();
				randomDrops = null;
			}
			
			// champion extra drop
			if (victim.isChampion() && (Rnd.get(100) < (victim.getLevel() < killer.getLevel() ? ChampionMonstersConfig.CHAMPION_REWARD_LOWER_LEVEL_ITEM_CHANCE : ChampionMonstersConfig.CHAMPION_REWARD_HIGHER_LEVEL_ITEM_CHANCE)))
			{
				// create list
				if (calculatedDrops == null)
				{
					calculatedDrops = new ArrayList<>();
				}
				
				if (!calculatedDrops.containsAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS))
				{
					calculatedDrops.addAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS);
				}
			}
		}
		
		return calculatedDrops;
	}
	
	/**
	 * Calculates the list of items dropped by a victim based on the drop type.<br>
	 * This method considers level differences, drop rates, and champion rewards.<br>
	 * It handles both spoil and death drop logic.
	 * @param dropType The type of drop to calculate.
	 * @param victim The creature that died.
	 * @param killer The creature that killed the victim.
	 * @return A list of {@code ItemHolder} objects representing the calculated drops.
	 */
	private List<ItemHolder> calculateUngroupedDrops(DropType dropType, Creature victim, Creature killer)
	{
		final List<DropHolder> dropList = dropType == DropType.SPOIL ? _dropListSpoil : _dropListDeath;
		
		// level difference calculations
		final int levelDifference = victim.getLevel() - killer.getLevel();
		final double levelGapChanceToDropAdena = MathUtil.scaleToRange(levelDifference, -RatesConfig.DROP_ADENA_MAX_LEVEL_DIFFERENCE, -RatesConfig.DROP_ADENA_MIN_LEVEL_DIFFERENCE, RatesConfig.DROP_ADENA_MIN_LEVEL_GAP_CHANCE, 100d);
		final double levelGapChanceToDrop = MathUtil.scaleToRange(levelDifference, -RatesConfig.DROP_ITEM_MAX_LEVEL_DIFFERENCE, -RatesConfig.DROP_ITEM_MIN_LEVEL_DIFFERENCE, RatesConfig.DROP_ITEM_MIN_LEVEL_GAP_CHANCE, 100d);
		
		int dropOccurrenceCounter = victim.isRaid() ? RatesConfig.DROP_MAX_OCCURRENCES_RAIDBOSS : RatesConfig.DROP_MAX_OCCURRENCES_NORMAL;
		List<ItemHolder> calculatedDrops = null;
		List<ItemHolder> randomDrops = null;
		ItemHolder cachedItem = null;
		if (dropOccurrenceCounter > 0)
		{
			for (DropHolder dropItem : dropList)
			{
				// Check if maximum drop occurrences have been reached, as items with a 100% drop chance without server rate multipliers drop normally.
				final double itemChanceValue = dropItem.getChance();
				if ((dropOccurrenceCounter == 0) && (itemChanceValue < 100) && (randomDrops != null) && (calculatedDrops != null))
				{
					// remove highest chance item (temporarily if no other item replaces it)
					cachedItem = randomDrops.remove(0);
					calculatedDrops.remove(cachedItem);
					dropOccurrenceCounter = 1;
				}
				
				// check level gap that may prevent to drop item
				final int itemId = dropItem.getItemId();
				if ((Rnd.nextDouble() * 100) > (itemId == Inventory.ADENA_ID ? levelGapChanceToDropAdena : levelGapChanceToDrop))
				{
					continue;
				}
				
				// calculate chances
				final ItemHolder drop = calculateUngroupedDrop(dropItem, victim, killer);
				if (drop == null)
				{
					continue;
				}
				
				// create lists
				if (randomDrops == null)
				{
					randomDrops = new ArrayList<>(dropOccurrenceCounter);
				}
				
				if (calculatedDrops == null)
				{
					calculatedDrops = new ArrayList<>(dropOccurrenceCounter);
				}
				
				// finally
				final Float itemChance = RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId);
				if (itemChance != null)
				{
					if ((itemChanceValue * itemChance) < 100)
					{
						dropOccurrenceCounter--;
						randomDrops.add(drop);
					}
				}
				else if (itemChanceValue < 100)
				{
					dropOccurrenceCounter--;
					randomDrops.add(drop);
				}
				
				calculatedDrops.add(drop);
			}
		}
		
		// add temporarily removed item when not replaced
		if ((dropOccurrenceCounter > 0) && (cachedItem != null) && (calculatedDrops != null))
		{
			calculatedDrops.add(cachedItem);
		}
		
		// clear random drops
		if (randomDrops != null)
		{
			randomDrops.clear();
			randomDrops = null;
		}
		
		// champion extra drop
		if (victim.isChampion() && (Rnd.get(100) < (victim.getLevel() < killer.getLevel() ? ChampionMonstersConfig.CHAMPION_REWARD_LOWER_LEVEL_ITEM_CHANCE : ChampionMonstersConfig.CHAMPION_REWARD_HIGHER_LEVEL_ITEM_CHANCE)))
		{
			// create list
			if (calculatedDrops == null)
			{
				calculatedDrops = new ArrayList<>();
			}
			
			if (!calculatedDrops.containsAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS))
			{
				calculatedDrops.addAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS);
			}
		}
		
		return calculatedDrops;
	}
	
	/**
	 * Calculates the quantity of an item to drop based on various multipliers.<br>
	 * It checks for champion status, raid status, and premium player bonuses.<br>
	 * The method returns a new {@code ItemHolder} if the random chance succeeds.<br>
	 * If the roll fails, it returns {@code null}.
	 * @param group The holder containing the drop group data.
	 * @param dropItem The specific item to be dropped.
	 * @param victim The creature that was killed.
	 * @param killer The creature that performed the kill.
	 * @param chance The probability percentage for the item to drop.
	 * @return A new {@code ItemHolder} with the calculated amount, or {@code null}.
	 */
	private ItemHolder calculateGroupDrop(DropGroupHolder group, DropHolder dropItem, Creature victim, Creature killer, double chance)
	{
		final int itemId = dropItem.getItemId();
		final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
		final boolean champion = victim.isChampion();
		
		// calculate if item will drop
		if ((Rnd.nextDouble() * 100) < chance)
		{
			// amount is calculated after chance returned success
			double rateAmount = 1;
			final Float rateDropById = RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId);
			if (rateDropById != null)
			{
				rateAmount *= rateDropById;
				if (champion && (itemId == Inventory.ADENA_ID))
				{
					rateAmount *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT;
				}
			}
			else if (item.hasExImmediateEffect())
			{
				rateAmount *= RatesConfig.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
			}
			else if (victim.isRaid())
			{
				rateAmount *= RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
			}
			else
			{
				rateAmount *= RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT : 1);
			}
			
			// premium amount
			final Player player = killer.asPlayer();
			if (player != null)
			{
				if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
				{
					final Float premiumRateDropById = PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId);
					if (premiumRateDropById != null)
					{
						rateAmount *= premiumRateDropById;
					}
					else if (item.hasExImmediateEffect())
					{
						// TODO: Premium herb amount? :)
					}
					else if (victim.isRaid())
					{
						// TODO: Premium raid amount? :)
					}
					else
					{
						rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT;
					}
				}
				
				// bonus drop amount effect
				final PlayerStat stat = player.getStat();
				rateAmount *= stat.getMul(Stat.BONUS_DROP_AMOUNT, 1);
				if (itemId == Inventory.ADENA_ID)
				{
					rateAmount *= stat.getMul(Stat.BONUS_DROP_ADENA, 1);
				}
			}
			
			// finally
			return new ItemHolder(itemId, (long) (Rnd.get(dropItem.getMin(), dropItem.getMax()) * rateAmount));
		}
		
		return null;
	}
	
	/**
	 * Calculates the dropped item and its quantity based on various loot rules.<br>
	 * This method handles different drop types such as {@code DROP}, {@code LUCKY}, and {@code SPOIL}.<br>
	 * It applies multipliers for champion status, raid status, premium accounts, and player stats.
	 * @param dropItem The holder containing the item data to be dropped.
	 * @param victim The creature that was killed.
	 * @param killer The creature that performed the kill.
	 * @return A new {@code ItemHolder} if the drop is successful, otherwise {@code null}.
	 */
	private ItemHolder calculateUngroupedDrop(DropHolder dropItem, Creature victim, Creature killer)
	{
		switch (dropItem.getDropType())
		{
			case DROP:
			{
				final int itemId = dropItem.getItemId();
				final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
				final boolean champion = victim.isChampion();
				
				// chance
				double rateChance = 1;
				if (RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId) != null)
				{
					rateChance *= RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId);
					if (champion && (itemId == Inventory.ADENA_ID))
					{
						rateChance *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_CHANCE;
					}
					
					if ((itemId == Inventory.ADENA_ID) && (rateChance > 100))
					{
						rateChance = 100;
					}
				}
				else if (item.hasExImmediateEffect())
				{
					rateChance *= RatesConfig.RATE_HERB_DROP_CHANCE_MULTIPLIER;
				}
				else if (victim.isRaid())
				{
					rateChance *= RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
				}
				else
				{
					rateChance *= RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_CHANCE : 1);
				}
				
				// premium chance
				final Player player = killer.asPlayer();
				if (player != null)
				{
					if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
					{
						if (PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId) != null)
						{
							rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId);
						}
						else if (item.hasExImmediateEffect())
						{
							// TODO: Premium herb chance? :)
						}
						else if (victim.isRaid())
						{
							// TODO: Premium raid chance? :)
						}
						else
						{
							rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE;
						}
					}
					
					// bonus drop rate effect
					rateChance *= player.getStat().getMul(Stat.BONUS_DROP_RATE, 1);
				}
				
				// calculate if item will drop
				if ((Rnd.nextDouble() * 100) < (dropItem.getChance() * rateChance))
				{
					// amount is calculated after chance returned success
					double rateAmount = 1;
					if (RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId) != null)
					{
						rateAmount *= RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId);
						if (champion && (itemId == Inventory.ADENA_ID))
						{
							rateAmount *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT;
						}
					}
					else if (item.hasExImmediateEffect())
					{
						rateAmount *= RatesConfig.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
					}
					else if (victim.isRaid())
					{
						rateAmount *= RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
					}
					else
					{
						rateAmount *= RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT : 1);
					}
					
					// premium amount
					if (player != null)
					{
						if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
						{
							if (PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId) != null)
							{
								rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId);
							}
							else if (item.hasExImmediateEffect())
							{
								// TODO: Premium herb amount? :)
							}
							else if (victim.isRaid())
							{
								// TODO: Premium raid amount? :)
							}
							else
							{
								rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT;
							}
						}
						
						// bonus drop amount effect
						final PlayerStat stat = player.getStat();
						rateAmount *= stat.getMul(Stat.BONUS_DROP_AMOUNT, 1);
						if (itemId == Inventory.ADENA_ID)
						{
							rateAmount *= stat.getMul(Stat.BONUS_DROP_ADENA, 1);
						}
					}
					
					// finally
					return new ItemHolder(itemId, (long) (Rnd.get(dropItem.getMin(), dropItem.getMax()) * rateAmount));
				}
				break;
			}
			case LUCKY:
			{
				// try chance before luck
				if ((Rnd.nextDouble() * 100) < dropItem.getChance())
				{
					final Player player = killer.asPlayer();
					if ((player != null) && player.tryLuck())
					{
						return new ItemHolder(dropItem.getItemId(), Rnd.get(dropItem.getMin(), dropItem.getMax()));
					}
				}
				break;
			}
			case SPOIL:
			{
				// chance
				double rateChance = RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER;
				
				// premium chance
				final Player player = killer.asPlayer();
				if (player != null)
				{
					if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
					{
						rateChance *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_CHANCE;
					}
					
					// bonus spoil rate effect
					rateChance *= player.getStat().getMul(Stat.BONUS_SPOIL_RATE, 1);
				}
				
				// calculate if item will be rewarded
				if ((Rnd.nextDouble() * 100) < (dropItem.getChance() * rateChance))
				{
					// amount is calculated after chance returned success
					double rateAmount = RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER;
					
					// premium amount
					if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && (player != null) && player.hasPremiumStatus())
					{
						rateAmount *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_AMOUNT;
					}
					
					// finally
					return new ItemHolder(dropItem.getItemId(), (long) (Rnd.get(dropItem.getMin(), dropItem.getMax()) * rateAmount));
				}
				break;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the collision radius of the entity after growth.<br>
	 * This value is used to determine the physical size for hit detection.
	 * @return The grown collision radius as a {@code float}.
	 */
	public float getCollisionRadiusGrown()
	{
		return _collisionRadiusGrown;
	}
	
	/**
	 * Returns the collision height of the entity after growth.<br>
	 * This value is used to determine how high an object occupies in the world.
	 * @return The grown collision height as a {@code float}.
	 */
	public float getCollisionHeightGrown()
	{
		return _collisionHeightGrown;
	}
	
	/**
	 * Checks if a class is compatible with another class or interface.<br>
	 * It verifies if {@code subValue} implements the interface {@code clazz}.<br>
	 * It also checks if {@code subValue} is the same class as {@code clazz} or inherits from it.
	 * @param subValue The class to check.
	 * @param clazz The target class or interface to compare against.
	 * @return {@code true} if the classes are compatible, otherwise {@code false}.
	 */
	public static boolean isAssignableTo(Class<?> subValue, Class<?> clazz)
	{
		// If clazz represents an interface.
		if (clazz.isInterface())
		{
			// Check if obj implements the clazz interface.
			for (Class<?> interface1 : subValue.getInterfaces())
			{
				if (clazz.getName().equals(interface1.getName()))
				{
					return true;
				}
			}
		}
		else
		{
			Class<?> sub = subValue;
			do
			{
				if (sub.getName().equals(clazz.getName()))
				{
					return true;
				}
				
				sub = sub.getSuperclass();
			}
			while (sub != null);
		}
		
		return false;
	}
	
	/**
	 * Checks if an object can be assigned to a specific class.<br>
	 * This method verifies the inheritance relationship between two types.
	 * @param obj The object to check.
	 * @param clazz The target class to compare against.
	 * @return {@code true} if the object is assignable to the class, {@code false} otherwise.
	 */
	public static boolean isAssignableTo(Object obj, Class<?> clazz)
	{
		return isAssignableTo(obj.getClass(), clazz);
	}
}
