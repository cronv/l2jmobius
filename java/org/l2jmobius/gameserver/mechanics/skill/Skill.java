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
package org.l2jmobius.gameserver.mechanics.skill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.AffectScopeHandler;
import org.l2jmobius.gameserver.handler.IAffectScopeHandler;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.enums.BasicProperty;
import org.l2jmobius.gameserver.mechanics.skill.enums.NextActionType;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.AttachSkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectObject;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.mechanics.stats.BasicPropertyResist;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.TraitType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a skill in the game world and handles its execution logic.<br>
 * This class manages how skills interact with {@code Creature} entities and apply effects.<br>
 * It serves as the primary data model for all skill mechanics.
 */
public class Skill
{
	private static final Logger LOGGER = Logger.getLogger(Skill.class.getName());
	
	/** Skill ID. */
	private final int _id;
	/** Skill level. */
	private final int _level;
	/** Skill sub level. */
	private final int _subLevel;
	/** Custom skill ID displayed by the client. */
	private final int _displayId;
	/** Custom skill level displayed by the client. */
	private final int _displayLevel;
	/** Skill client's name. */
	private final String _name;
	/** Operative type: passive, active, toggle. */
	private final SkillOperateType _operateType;
	private final int _magic;
	private final TraitType _traitType;
	private final boolean _staticReuse;
	/** MP consumption. */
	private final int _mpConsume;
	/** Initial MP consumption. */
	private final int _mpInitialConsume;
	/** MP consumption per channeling. */
	private final int _mpPerChanneling;
	/** HP consumption. */
	private final int _hpConsume;
	/** Amount of items consumed by this skill from caster. */
	private final int _itemConsumeCount;
	/** Id of item consumed by this skill from caster. */
	private final int _itemConsumeId;
	/** Fame points consumed by this skill from caster */
	private final int _famePointConsume;
	/** Clan points consumed by this skill from caster's clan */
	private final int _clanRepConsume;
	/** Cast range: how far can be the target. */
	private final int _castRange;
	/** Effect range: how far the skill affect the target. */
	private final int _effectRange;
	/** Abnormal instant, used for herbs mostly. */
	private final boolean _isAbnormalInstant;
	/** Abnormal level, global effect level. */
	private final int _abnormalLevel;
	/** Abnormal type: global effect "group". */
	private final AbnormalType _abnormalType;
	/** Abnormal type: local effect "group". */
	private final AbnormalType _subordinationAbnormalType;
	/** Abnormal time: global effect duration time. */
	private final int _abnormalTime;
	/** Abnormal visual effect: the visual effect displayed ingame. */
	private Set<AbnormalVisualEffect> _abnormalVisualEffects;
	/** If {@code true} this skill's effect should stay after death. */
	private final boolean _stayAfterDeath;
	/** If {@code true} this skill's effect recovery HP/MP or CP from herb. */
	private final boolean _isRecoveryHerb;
	
	private final int _refId;
	
	// All times in milliseconds.
	private final int _hitTime;
	private final double _hitCancelTime;
	private final int _coolTime;
	private final long _reuseHashCode;
	private final int _reuseDelay;
	private final int _reuseDelayGroup;
	
	private final int _magicLevel;
	private final int _lvlBonusRate;
	private final int _activateRate;
	private final int _minChance;
	private final int _maxChance;
	
	// Effecting area of the skill, in radius.
	// The radius center varies according to the _targetType:
	// "caster" if targetType = AURA/PARTY/CLAN or "target" if targetType = AREA
	private final TargetType _targetType;
	private final AffectScope _affectScope;
	private final AffectObject _affectObject;
	private final int _affectRange;
	private final int[] _fanRange = new int[4]; // unk;startDegree;fanAffectRange;fanAffectAngle
	private final int[] _affectLimit = new int[3]; // TODO: Third value is unknown... find it out!
	private final int[] _affectHeight = new int[2];
	
	private final NextActionType _nextAction;
	
	private final boolean _removedOnAnyActionExceptMove;
	private final boolean _removedOnDamage;
	private final boolean _removedOnUnequipWeapon;
	
	private final boolean _blockedInOlympiad;
	
	private final AttributeType _attributeType;
	private final int _attributeValue;
	
	private final BasicProperty _basicProperty;
	
	private final int _minPledgeClass;
	private final int _soulMaxConsume;
	private final int _chargeConsume;
	
	private final boolean _isTriggeredSkill; // If true the skill will take activation buff slot instead of a normal buff slot.
	private final int _effectPoint;
	
	private final Map<SkillConditionScope, List<ISkillCondition>> _conditionLists = new EnumMap<>(SkillConditionScope.class);
	private final Map<EffectScope, List<AbstractEffect>> _effectLists = new EnumMap<>(EffectScope.class);
	
	private final boolean _isDebuff;
	
	private final boolean _isSuicideAttack;
	private final boolean _canBeDispelled;
	
	private final boolean _excludedFromCheck;
	private final boolean _withoutAction;
	
	private final String _icon;
	
	private volatile Byte[] _effectTypes;
	
	// Channeling data
	private final int _channelingSkillId;
	private final long _channelingStart;
	private final long _channelingTickInterval;
	
	// Mentoring
	private final boolean _isMentoring;
	
	// Stance skill IDs
	private final int _doubleCastSkill;
	
	private final boolean _canDoubleCast;
	private final boolean _canCastWhileDisabled;
	private final boolean _isSharedWithSummon;
	private final boolean _isNecessaryToggle;
	private final boolean _deleteAbnormalOnLeave;
	private final boolean _irreplaceableBuff; // Stays after death, on subclass change, cannot be canceled.
	private final boolean _ignoreDebuffResist; // Debuff that Debuff Resistance cannot stop.
	private final boolean _blockActionUseSkill; // Blocks the use skill client action and is not showed on skill list.
	
	private final int _toggleGroupId;
	private final int _attachToggleGroupId;
	private final List<AttachSkillHolder> _attachSkills;
	private final Set<AbnormalType> _abnormalResists;
	
	private final double _magicCriticalRate;
	private final SkillBuffType _buffType;
	private final boolean _displayInList;
	private final boolean _isHidingMessages;
	
	private final int _alternateRangedSkillId;
	private final int _alternateMeleeSkillId;
	
	private final int _alternateEnemySkillId;
	private final int _alternateAllySkillId;
	
	/**
	 * Creates a new {@code Skill} instance using the provided data set.<br>
	 * This constructor initializes all skill properties such as costs, ranges, and effects.<br>
	 * It also parses complex strings for fan ranges, affect limits, and heights.
	 * @param set The {@code StatSet} containing the raw skill configuration data.
	 */
	public Skill(StatSet set)
	{
		_id = set.getInt(".id");
		_level = set.getInt(".level");
		_subLevel = set.getInt(".subLevel", 0);
		_refId = set.getInt(".referenceId", 0);
		_displayId = set.getInt(".displayId", _id);
		_displayLevel = set.getInt(".displayLevel", _level);
		_name = set.getString(".name", "");
		_operateType = set.getEnum("operateType", SkillOperateType.class);
		_magic = set.getInt("isMagic", 0);
		_traitType = set.getEnum("trait", TraitType.class, TraitType.NONE);
		_staticReuse = set.getBoolean("staticReuse", false);
		_mpConsume = set.getInt("mpConsume", 0);
		_mpInitialConsume = set.getInt("mpInitialConsume", 0);
		_mpPerChanneling = set.getInt("mpPerChanneling", _mpConsume);
		_hpConsume = set.getInt("hpConsume", 0);
		_itemConsumeCount = set.getInt("itemConsumeCount", 0);
		_itemConsumeId = set.getInt("itemConsumeId", 0);
		_famePointConsume = set.getInt("famePointConsume", 0);
		_clanRepConsume = set.getInt("clanRepConsume", 0);
		_castRange = set.getInt("castRange", -1);
		_effectRange = set.getInt("effectRange", -1);
		_abnormalLevel = set.getInt("abnormalLevel", 0);
		_abnormalType = set.getEnum("abnormalType", AbnormalType.class, AbnormalType.NONE);
		_subordinationAbnormalType = set.getEnum("subordinationAbnormalType", AbnormalType.class, AbnormalType.NONE);
		int abnormalTime = set.getInt("abnormalTime", 0);
		if (PlayerConfig.ENABLE_MODIFY_SKILL_DURATION && PlayerConfig.SKILL_DURATION_LIST.containsKey(_id) && (_operateType != SkillOperateType.T))
		{
			if ((_level < 100) || (_level > 140))
			{
				abnormalTime = PlayerConfig.SKILL_DURATION_LIST.get(_id);
			}
			else if ((_level >= 100) && (_level < 140))
			{
				abnormalTime += PlayerConfig.SKILL_DURATION_LIST.get(_id);
			}
		}
		
		_abnormalTime = abnormalTime;
		_isAbnormalInstant = set.getBoolean("abnormalInstant", false);
		parseAbnormalVisualEffect(set.getString("abnormalVisualEffect", null));
		_stayAfterDeath = set.getBoolean("stayAfterDeath", false);
		_hitTime = set.getInt("hitTime", 0);
		_hitCancelTime = set.getDouble("hitCancelTime", 0);
		_coolTime = set.getInt("coolTime", 0);
		_isDebuff = set.getBoolean("isDebuff", false);
		_isRecoveryHerb = set.getBoolean("isRecoveryHerb", false);
		if (PlayerConfig.ENABLE_MODIFY_SKILL_REUSE && PlayerConfig.SKILL_REUSE_LIST.containsKey(_id))
		{
			_reuseDelay = PlayerConfig.SKILL_REUSE_LIST.get(_id);
		}
		else
		{
			_reuseDelay = set.getInt("reuseDelay", 0);
		}
		
		_reuseDelayGroup = set.getInt("reuseDelayGroup", -1);
		_reuseHashCode = SkillData.getSkillHashCode(_reuseDelayGroup > 0 ? _reuseDelayGroup : _id, _level, _subLevel);
		_targetType = set.getEnum("targetType", TargetType.class, TargetType.SELF);
		_affectScope = set.getEnum("affectScope", AffectScope.class, AffectScope.SINGLE);
		_affectObject = set.getEnum("affectObject", AffectObject.class, AffectObject.ALL);
		_affectRange = set.getInt("affectRange", 0);
		
		final String fanRange = set.getString("fanRange", null);
		if (fanRange != null)
		{
			try
			{
				final String[] valuesSplit = fanRange.split(";");
				_fanRange[0] = Integer.parseInt(valuesSplit[0]);
				_fanRange[1] = Integer.parseInt(valuesSplit[1]);
				_fanRange[2] = Integer.parseInt(valuesSplit[2]);
				_fanRange[3] = Integer.parseInt(valuesSplit[3]);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("SkillId: " + _id + " invalid fanRange value: " + fanRange + ", \"unk;startDegree;fanAffectRange;fanAffectAngle\" required");
			}
		}
		
		final String affectLimit = set.getString("affectLimit", null);
		if (affectLimit != null)
		{
			try
			{
				final String[] valuesSplit = affectLimit.split("-");
				_affectLimit[0] = Integer.parseInt(valuesSplit[0]);
				_affectLimit[1] = Integer.parseInt(valuesSplit[1]);
				if (valuesSplit.length > 2)
				{
					_affectLimit[2] = Integer.parseInt(valuesSplit[2]);
				}
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("SkillId: " + _id + " invalid affectLimit value: " + affectLimit + ", \"minAffected-additionalRandom\" required");
			}
		}
		
		final String affectHeight = set.getString("affectHeight", null);
		if (affectHeight != null)
		{
			try
			{
				final String[] valuesSplit = affectHeight.split(";");
				_affectHeight[0] = Integer.parseInt(valuesSplit[0]);
				_affectHeight[1] = Integer.parseInt(valuesSplit[1]);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("SkillId: " + _id + " invalid affectHeight value: " + affectHeight + ", \"minHeight-maxHeight\" required");
			}
			
			if (_affectHeight[0] > _affectHeight[1])
			{
				throw new IllegalArgumentException("SkillId: " + _id + " invalid affectHeight value: " + affectHeight + ", \"minHeight-maxHeight\" required, minHeight is higher than maxHeight!");
			}
		}
		
		_magicLevel = set.getInt("magicLevel", 0);
		_lvlBonusRate = set.getInt("lvlBonusRate", 0);
		_activateRate = set.getInt("activateRate", -1);
		_minChance = set.getInt("minChance", PlayerConfig.MIN_ABNORMAL_STATE_SUCCESS_RATE);
		_maxChance = set.getInt("maxChance", PlayerConfig.MAX_ABNORMAL_STATE_SUCCESS_RATE);
		_nextAction = set.getEnum("nextAction", NextActionType.class, NextActionType.NONE);
		_removedOnAnyActionExceptMove = set.getBoolean("removedOnAnyActionExceptMove", false);
		_removedOnDamage = set.getBoolean("removedOnDamage", false);
		_removedOnUnequipWeapon = set.getBoolean("removedOnUnequipWeapon", false);
		_blockedInOlympiad = set.getBoolean("blockedInOlympiad", false);
		_attributeType = set.getEnum("attributeType", AttributeType.class, AttributeType.NONE);
		_attributeValue = set.getInt("attributeValue", 0);
		_basicProperty = set.getEnum("basicProperty", BasicProperty.class, BasicProperty.NONE);
		_isSuicideAttack = set.getBoolean("isSuicideAttack", false);
		_minPledgeClass = set.getInt("minPledgeClass", 0);
		_soulMaxConsume = set.getInt("soulMaxConsumeCount", 0);
		_chargeConsume = set.getInt("chargeConsume", 0);
		_isTriggeredSkill = set.getBoolean("isTriggeredSkill", false);
		_effectPoint = set.getInt("effectPoint", 0);
		_canBeDispelled = set.getBoolean("canBeDispelled", true);
		_excludedFromCheck = set.getBoolean("excludedFromCheck", false);
		_withoutAction = set.getBoolean("withoutAction", false);
		_icon = set.getString("icon", "icon.skill0000");
		_channelingSkillId = set.getInt("channelingSkillId", 0);
		_channelingTickInterval = (long) set.getFloat("channelingTickInterval", 2000f) * 1000;
		_channelingStart = (long) (set.getFloat("channelingStart", 0f) * 1000);
		_isMentoring = set.getBoolean("isMentoring", false);
		_doubleCastSkill = set.getInt("doubleCastSkill", 0);
		_canDoubleCast = set.getBoolean("canDoubleCast", false);
		_canCastWhileDisabled = set.getBoolean("canCastWhileDisabled", false);
		_isSharedWithSummon = set.getBoolean("isSharedWithSummon", true);
		_isNecessaryToggle = set.getBoolean("isNecessaryToggle", false);
		_deleteAbnormalOnLeave = set.getBoolean("deleteAbnormalOnLeave", false);
		_irreplaceableBuff = set.getBoolean("irreplaceableBuff", false);
		_ignoreDebuffResist = set.getBoolean("ignoreDebuffResist", false);
		_blockActionUseSkill = set.getBoolean("blockActionUseSkill", false);
		_toggleGroupId = set.getInt("toggleGroupId", -1);
		_attachToggleGroupId = set.getInt("attachToggleGroupId", -1);
		_attachSkills = set.getList("attachSkillList", StatSet.class, Collections.emptyList()).stream().map(AttachSkillHolder::fromStatSet).toList();
		
		final String abnormalResist = set.getString("abnormalResists", null);
		if (abnormalResist != null)
		{
			final String[] abnormalResistStrings = abnormalResist.split(";");
			if (abnormalResistStrings.length > 0)
			{
				_abnormalResists = new HashSet<>(abnormalResistStrings.length);
				for (String s : abnormalResistStrings)
				{
					try
					{
						_abnormalResists.add(AbnormalType.valueOf(s));
					}
					catch (Exception e)
					{
						LOGGER.log(Level.WARNING, "Skill ID[" + _id + "] Expected AbnormalType for abnormalResists but found " + s, e);
					}
				}
			}
			else
			{
				_abnormalResists = Collections.emptySet();
			}
		}
		else
		{
			_abnormalResists = Collections.emptySet();
		}
		
		_magicCriticalRate = set.getDouble("magicCriticalRate", 0);
		_buffType = _isTriggeredSkill ? SkillBuffType.TRIGGER : isToggle() ? SkillBuffType.TOGGLE : isDance() ? SkillBuffType.DANCE : _isDebuff ? SkillBuffType.DEBUFF : !isHealingPotionSkill() ? SkillBuffType.BUFF : SkillBuffType.NONE;
		_displayInList = set.getBoolean("displayInList", true);
		_isHidingMessages = set.getBoolean("isHidingMessages", false);
		
		_alternateRangedSkillId = set.getInt("alternateRangedSkillId", 0);
		_alternateMeleeSkillId = set.getInt("alternateMeleeSkillId", 0);
		
		_alternateEnemySkillId = set.getInt("alternateEnemySkillId", 0);
		_alternateAllySkillId = set.getInt("alternateAllySkillId", 0);
	}
	
	/**
	 * Retrieves the trait type associated with this skill.
	 * @return the {@code TraitType} of the skill.
	 */
	public TraitType getTraitType()
	{
		return _traitType;
	}
	
	/**
	 * Retrieves the attribute type associated with this skill.<br>
	 * This value determines which specific attribute is affected by the skill.
	 * @return the {@code AttributeType} of the skill.
	 */
	public AttributeType getAttributeType()
	{
		return _attributeType;
	}
	
	/**
	 * Retrieves the numerical value of the attribute. <br>
	 * This value corresponds to the {@code AttributeType} defined for this skill.
	 * @return The current integer value of the attribute.
	 */
	public int getAttributeValue()
	{
		return _attributeValue;
	}
	
	/**
	 * Checks if the skill has an Area of Effect (AOE).<br>
	 * This method returns {@code true} for various scope types like {@code FAN}, {@code RANGE}, or {@code SQUARE}.<br>
	 * It returns {@code false} if the skill is not an AOE.
	 * @return {@code true} if the skill is an AOE, otherwise {@code false}.
	 */
	public boolean isAOE()
	{
		switch (_affectScope)
		{
			case FAN:
			case FAN_PB:
			case POINT_BLANK:
			case RANGE:
			case RING_RANGE:
			case SQUARE:
			case SQUARE_PB:
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the skill is a suicide attack.<br>
	 * A suicide attack typically harms the user as well as the target.
	 * @return {@code true} if it is a suicide attack, {@code false} otherwise.
	 */
	public boolean isSuicideAttack()
	{
		return _isSuicideAttack;
	}
	
	/**
	 * Checks if the skill can be used while a creature is transformed.<br>
	 * This method returns {@code true} if the skill is passive.
	 * @return {@code true} if the skill is allowed during transformation, {@code false} otherwise.
	 */
	public boolean allowOnTransform()
	{
		return isPassive();
	}
	
	/**
	 * Checks if the skill is an abnormal instant effect.<br>
	 * This determines if the abnormal status applies immediately.
	 * @return {@code true} if it is an instant effect, {@code false} otherwise.
	 */
	public boolean isAbnormalInstant()
	{
		return _isAbnormalInstant;
	}
	
	/**
	 * Retrieves the {@code AbnormalType} associated with this skill.<br>
	 * This value determines how abnormal effects are handled for the skill.
	 * @return the {@code AbnormalType} of the skill.
	 */
	public AbnormalType getAbnormalType()
	{
		return _abnormalType;
	}
	
	/**
	 * Retrieves the abnormal type associated with subordination.<br>
	 * This value defines how a specific subordinate effect behaves.
	 * @return the {@code AbnormalType} for this skill's subordination.
	 */
	public AbnormalType getSubordinationAbnormalType()
	{
		return _subordinationAbnormalType;
	}
	
	/**
	 * Retrieves the current level of the abnormal effect.<br>
	 * This value represents the intensity or tier of the status.
	 * @return The integer value of the {@code abnormalLevel}.
	 */
	public int getAbnormalLevel()
	{
		return _abnormalLevel;
	}
	
	/**
	 * Retrieves the duration of the abnormal effect.<br>
	 * This value represents how long the effect lasts in time units.
	 * @return The total time for the abnormal effect as an {@code int}.
	 */
	public int getAbnormalTime()
	{
		return _abnormalTime;
	}
	
	/**
	 * Retrieves the set of visual effects associated with abnormal actions.<br>
	 * Returns an empty {@code Set} if no effects are defined.
	 * @return A {@code Set} of {@code AbnormalVisualEffect} objects.
	 */
	public Set<AbnormalVisualEffect> getAbnormalVisualEffects()
	{
		return (_abnormalVisualEffects != null) ? _abnormalVisualEffects : Collections.emptySet();
	}
	
	/**
	 * Checks if the skill has any abnormal visual effects.<br>
	 * It returns {@code true} if the effect list is not {@code null} and contains items.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if there are abnormal visual effects, {@code false} otherwise.
	 */
	public boolean hasAbnormalVisualEffects()
	{
		return (_abnormalVisualEffects != null) && !_abnormalVisualEffects.isEmpty();
	}
	
	/**
	 * Retrieves the magic level of the skill.<br>
	 * This value determines how the skill interacts with magic-based mechanics.
	 * @return the current {@code int} magic level.
	 */
	public int getMagicLevel()
	{
		return _magicLevel;
	}
	
	/**
	 * Retrieves the level bonus rate for this skill.<br>
	 * This value determines how much the skill scales with the character's level.
	 * @return the current level bonus rate as an {@code int}.
	 */
	public int getLvlBonusRate()
	{
		return _lvlBonusRate;
	}
	
	/**
	 * Retrieves the activation rate for this skill.<br>
	 * This value determines how often the skill triggers successfully.
	 * @return The current {@code int} activation rate.
	 */
	public int getActivateRate()
	{
		return _activateRate;
	}
	
	/**
	 * Retrieves the minimum chance value for this skill.<br>
	 * This value represents the lower bound of the activation probability.
	 * @return The minimum chance as an {@code int}.
	 */
	public int getMinChance()
	{
		return _minChance;
	}
	
	/**
	 * Retrieves the maximum activation chance for this skill.<br>
	 * This value represents the upper limit of the random chance range.
	 * @return the maximum chance as an {@code int}.
	 */
	public int getMaxChance()
	{
		return _maxChance;
	}
	
	/**
	 * Checks if the skill effect is removed by any action.<br>
	 * This check excludes movement actions from triggering removal.
	 * @return {@code true} if the effect is removed on actions other than moving, {@code false} otherwise.
	 */
	public boolean isRemovedOnAnyActionExceptMove()
	{
		return _removedOnAnyActionExceptMove;
	}
	
	/**
	 * Checks if the skill effect is removed when damage is taken.<br>
	 * Returns {@code true} if the effect should be cleared upon receiving damage.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating if the effect is removed on damage.
	 */
	public boolean isRemovedOnDamage()
	{
		return _removedOnDamage;
	}
	
	/**
	 * Checks if the skill effect should be removed when a weapon is unequipped.
	 * @return {@code true} if the effect is removed on unequipping, {@code false} otherwise.
	 */
	public boolean isRemovedOnUnequipWeapon()
	{
		return _removedOnUnequipWeapon;
	}
	
	/**
	 * Checks if the skill is restricted during an Olympiad.<br>
	 * Returns {@code true} if the skill cannot be used in an Olympiad.<br>
	 * Returns {@code false} if the skill is allowed to be used.
	 * @return a boolean indicating if the skill is blocked in an Olympiad.
	 */
	public boolean isBlockedInOlympiad()
	{
		return _blockedInOlympiad;
	}
	
	/**
	 * Retrieves the unique identifier for the channeling skill.<br>
	 * This value is used to identify specific skills that require continuous casting.
	 * @return the {@code int} ID of the channeling skill.
	 */
	public int getChannelingSkillId()
	{
		return _channelingSkillId;
	}
	
	/**
	 * Retrieves the next action to be performed.<br>
	 * This value determines what happens after the current skill action finishes.
	 * @return the {@code NextActionType} of the next action.
	 */
	public NextActionType getNextAction()
	{
		return _nextAction;
	}
	
	/**
	 * Retrieves the maximum distance for casting this skill.
	 * @return The cast range as an {@code int}.
	 */
	public int getCastRange()
	{
		return _castRange;
	}
	
	/**
	 * Retrieves the range of the effect.<br>
	 * This value determines how far the skill's impact can reach.
	 * @return the effect range as an {@code int}.
	 */
	public int getEffectRange()
	{
		return _effectRange;
	}
	
	/**
	 * Returns the amount of health consumed by this skill.<br>
	 * This value represents the HP cost for the user.
	 * @return The {@code int} value of the HP consumption.
	 */
	public int getHpConsume()
	{
		return _hpConsume;
	}
	
	/**
	 * Retrieves the unique identifier for this skill.
	 * @return The {@code int} ID of the skill.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Checks if the skill is classified as a debuff.
	 * @return {@code true} if it is a debuff, {@code false} otherwise.
	 */
	public boolean isDebuff()
	{
		return _isDebuff;
	}
	
	/**
	 * Checks if the skill is classified as a recovery herb.
	 * @return {@code true} if it is a recovery herb, {@code false} otherwise.
	 */
	public boolean isRecoveryHerb()
	{
		return _isRecoveryHerb;
	}
	
	/**
	 * Retrieves the unique identifier used for displaying this skill.<br>
	 * This ID is typically used in the user interface.
	 * @return the {@code int} display ID of the skill.
	 */
	public int getDisplayId()
	{
		return _displayId;
	}
	
	/**
	 * Retrieves the display level of the skill.<br>
	 * This value is used for showing the level to the user.
	 * @return The current display level as an {@code int}.
	 */
	public int getDisplayLevel()
	{
		return _displayLevel;
	}
	
	/**
	 * Retrieves the basic property of this skill.
	 * @return the {@code BasicProperty} object.
	 */
	public BasicProperty getBasicProperty()
	{
		return _basicProperty;
	}
	
	/**
	 * Retrieves the number of items consumed by this skill.
	 * @return The total count of items used.
	 */
	public int getItemConsumeCount()
	{
		return _itemConsumeCount;
	}
	
	/**
	 * Retrieves the unique identifier for the item consumed by this skill.
	 * @return the {@code int} ID of the required item.
	 */
	public int getItemConsumeId()
	{
		return _itemConsumeId;
	}
	
	/**
	 * Retrieves the amount of fame points consumed by this skill.<br>
	 * This value is used to determine the cost when a player activates the skill.
	 * @return The number of fame points required for activation.
	 */
	public int getFamePointConsume()
	{
		return _famePointConsume;
	}
	
	/**
	 * Retrieves the amount of clan reputation consumed by this skill.<br>
	 * This value determines how much reputation is deducted from the player.
	 * @return the {@code int} value representing the clan reputation cost.
	 */
	public int getClanRepConsume()
	{
		return _clanRepConsume;
	}
	
	/**
	 * Retrieves the current level of the skill.<br>
	 * This value represents the progression level assigned to this {@code Skill}.
	 * @return The integer level of the skill.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the current sub-level of the skill.<br>
	 * This value represents a specific subdivision within the skill's hierarchy.
	 * @return The integer value of the {@code _subLevel}.
	 */
	public int getSubLevel()
	{
		return _subLevel;
	}
	
	/**
	 * Retrieves the magic type of the skill.<br>
	 * This value identifies how the skill interacts with magic mechanics.
	 * @return the {@code int} value representing the magic type.
	 */
	public int getMagicType()
	{
		return _magic;
	}
	
	/**
	 * Checks if the skill is a physical attack.<br>
	 * Returns {@code true} if the magic level is {@code 0}.<br>
	 * Returns {@code false} otherwise.
	 * @return {@code true} if the skill is physical, {@code false} otherwise.
	 */
	public boolean isPhysical()
	{
		return _magic == 0;
	}
	
	/**
	 * Checks if the skill is classified as a magic type.
	 * @return {@code true} if the skill is magic, {@code false} otherwise.
	 */
	public boolean isMagic()
	{
		return _magic == 1;
	}
	
	/**
	 * Checks if the skill is a static type.<br>
	 * This method returns {@code true} if the magic level is equal to {@code 2}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the skill is static, {@code false} otherwise.
	 */
	public boolean isStatic()
	{
		return _magic == 2;
	}
	
	/**
	 * Checks if the skill is classified as a dance.<br>
	 * This method returns {@code true} if the magic level is equal to {@code 3}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the skill is a dance, {@code false} otherwise.
	 */
	public boolean isDance()
	{
		return _magic == 3;
	}
	
	/**
	 * Checks if the skill allows for static reuse.<br>
	 * This determines whether a skill can be reused immediately.
	 * @return {@code true} if static reuse is enabled, {@code false} otherwise.
	 */
	public boolean isStaticReuse()
	{
		return _staticReuse;
	}
	
	/**
	 * Retrieves the amount of mana consumed by this skill.
	 * @return the {@code int} value representing the mana cost.
	 */
	public int getMpConsume()
	{
		return _mpConsume;
	}
	
	/**
	 * Retrieves the initial mana cost for this skill.<br>
	 * This value represents the base amount of {@code mp} consumed when the skill is first cast.
	 * @return The initial mana consumption as an {@code int}.
	 */
	public int getMpInitialConsume()
	{
		return _mpInitialConsume;
	}
	
	/**
	 * Retrieves the amount of mana consumed per channeling tick.<br>
	 * This value determines how much {@code mp} is deducted during a continuous skill cast.
	 * @return The mana cost per channeling interval as an {@code int}.
	 */
	public int getMpPerChanneling()
	{
		return _mpPerChanneling;
	}
	
	/**
	 * Retrieves the name of the skill.
	 * @return The {@code String} name of the skill.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Gets the cooldown time for reusing this skill.<br>
	 * This value determines how long a player must wait before using the skill again.
	 * @return The reuse delay in milliseconds.
	 */
	public int getReuseDelay()
	{
		return _reuseDelay;
	}
	
	/**
	 * Retrieves the reuse delay group for this skill.<br>
	 * This value determines which category of cooldown applies to the skill.
	 * @return The integer ID of the reuse delay group.
	 */
	public int getReuseDelayGroup()
	{
		return _reuseDelayGroup;
	}
	
	/**
	 * Retrieves the unique hash code for skill reuse.<br>
	 * This value is used to identify specific reuse mechanics.
	 * @return The {@code long} hash code of the reuse mechanic.
	 */
	public long getReuseHashCode()
	{
		return _reuseHashCode;
	}
	
	/**
	 * Retrieves the hit time for this skill.<br>
	 * This value represents how long it takes for the skill to land.
	 * @return the hit time as an {@code int}.
	 */
	public int getHitTime()
	{
		return _hitTime;
	}
	
	/**
	 * Retrieves the time required to cancel a hit.<br>
	 * This value determines how long an action can be interrupted.
	 * @return The {@code double} value representing the hit cancel time.
	 */
	public double getHitCancelTime()
	{
		return _hitCancelTime;
	}
	
	/**
	 * Retrieves the current cooldown time for this skill.
	 * @return The cooldown value as an {@code int}.
	 */
	public int getCoolTime()
	{
		return _coolTime;
	}
	
	/**
	 * Retrieves the target type for this skill.<br>
	 * This indicates what kind of entity can be targeted by the skill.
	 * @return the {@code TargetType} of the skill.
	 */
	public TargetType getTargetType()
	{
		return _targetType;
	}
	
	/**
	 * Retrieves the scope of the effect for this skill.<br>
	 * This defines which targets or areas are affected by the skill.
	 * @return the {@code AffectScope} associated with this skill.
	 */
	public AffectScope getAffectScope()
	{
		return _affectScope;
	}
	
	/**
	 * Retrieves the {@code AffectObject} associated with this skill.<br>
	 * This object contains specific data regarding how the effect is applied.
	 * @return the {@code AffectObject} instance.
	 */
	public AffectObject getAffectObject()
	{
		return _affectObject;
	}
	
	/**
	 * Retrieves the range of the effect. <br>
	 * This value determines how far the skill's impact can reach.
	 * @return The effect range as an {@code int}.
	 */
	public int getAffectRange()
	{
		return _affectRange;
	}
	
	/**
	 * Retrieves the range of the fan effect.
	 * @return an {@code int[]} containing the fan range values.
	 */
	public int[] getFanRange()
	{
		return _fanRange;
	}
	
	/**
	 * Retrieves the current limit for effects.<br>
	 * This method calculates a value based on the defined limits.<br>
	 * It adds a random value to the base limit if any are set.
	 * @return The calculated effect limit as an {@code int}.
	 */
	public int getAffectLimit()
	{
		if ((_affectLimit[0] > 0) || (_affectLimit[1] > 0))
		{
			return (_affectLimit[0] + Rnd.get(_affectLimit[1]));
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the minimum height for an effect.<br>
	 * This value is used to determine vertical positioning.
	 * @return The minimum height as an {@code int}.
	 */
	public int getAffectHeightMin()
	{
		return _affectHeight[0];
	}
	
	/**
	 * Retrieves the maximum height for an effect.<br>
	 * This value is stored in the {@code _affectHeight} array.
	 * @return The maximum height as an {@code int}.
	 */
	public int getAffectHeightMax()
	{
		return _affectHeight[1];
	}
	
	/**
	 * Checks if the current operation type is active.<br>
	 * This method delegates the check to the internal {@code _operateType} object.
	 * @return {@code true} if the operation is active, {@code false} otherwise.
	 */
	public boolean isActive()
	{
		return _operateType.isActive();
	}
	
	/**
	 * Checks if the skill is a passive type.<br>
	 * This method returns {@code true} if the skill does not require activation.<br>
	 * It returns {@code false} if the skill is an active ability.
	 * @return {@code true} if the skill is passive, {@code false} otherwise.
	 */
	public boolean isPassive()
	{
		return _operateType.isPassive();
	}
	
	/**
	 * Checks if the skill operation type is a toggle.<br>
	 * This method returns {@code true} if the skill can be toggled on or off.
	 * @return {@code true} if it is a toggle, {@code false} otherwise.
	 */
	public boolean isToggle()
	{
		return _operateType.isToggle();
	}
	
	/**
	 * Checks if the skill operates as an aura.<br>
	 * An aura effect typically stays active around a creature.
	 * @return {@code true} if the skill is an aura, {@code false} otherwise.
	 */
	public boolean isAura()
	{
		return _operateType.isAura();
	}
	
	/**
	 * Checks if messages for this skill should be hidden.<br>
	 * This returns {@code true} if either the local flag or the operation type requires hiding messages.
	 * @return {@code true} if messages are hidden, {@code false} otherwise.
	 */
	public boolean isHidingMessages()
	{
		return _isHidingMessages || _operateType.isHidingMessages();
	}
	
	/**
	 * Checks if the operation type prevents broadcasting.<br>
	 * This method returns {@code true} if the action should not be sent to other clients.
	 * @return {@code true} if the operation is not broadcastable, {@code false} otherwise.
	 */
	public boolean isNotBroadcastable()
	{
		return _operateType.isNotBroadcastable();
	}
	
	/**
	 * Checks if the skill has a continuous effect.<br>
	 * This returns {@code true} if either the operation type or the self-continuous property is active.
	 * @return {@code true} if the skill is continuous, otherwise {@code false}.
	 */
	public boolean isContinuous()
	{
		return _operateType.isContinuous() || isSelfContinuous();
	}
	
	/**
	 * Checks if the skill operation type allows for flying.<br>
	 * This method returns {@code true} if the action can be performed while flying.
	 * @return {@code true} if it is a fly type, {@code false} otherwise.
	 */
	public boolean isFlyType()
	{
		return _operateType.isFlyType();
	}
	
	/**
	 * Checks if the skill's operation type is self-continuous.<br>
	 * This determines if the effect applies to the caster continuously.
	 * @return {@code true} if the operation is self-continuous, {@code false} otherwise.
	 */
	public boolean isSelfContinuous()
	{
		return _operateType.isSelfContinuous();
	}
	
	/**
	 * Checks if the skill requires channeling.<br>
	 * This method returns {@code true} if the skill's operation type is a channeling action.
	 * @return {@code true} if the skill is a channeling skill, {@code false} otherwise.
	 */
	public boolean isChanneling()
	{
		return _operateType.isChanneling();
	}
	
	/**
	 * Checks if the skill is a triggered skill.<br>
	 * This returns {@code true} if the skill can be activated automatically.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the skill is triggered, {@code false} otherwise.
	 */
	public boolean isTriggeredSkill()
	{
		return _isTriggeredSkill;
	}
	
	/**
	 * Checks if the skill belongs to a synergy type.<br>
	 * This method returns {@code true} if the skill is part of a synergy.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if it is a synergy skill, {@code false} otherwise.
	 */
	public boolean isSynergySkill()
	{
		return _operateType.isSynergy();
	}
	
	/**
	 * Retrieves the operation type of the skill.<br>
	 * This value determines how the skill behaves during execution.
	 * @return the {@code SkillOperateType} of this skill.
	 */
	public SkillOperateType getOperateType()
	{
		return _operateType;
	}
	
	/**
	 * Checks if the skill results in a transformation.<br>
	 * This returns {@code true} if the {@code abnormalType} is either {@code TRANSFORM} or {@code CHANGEBODY}.
	 * @return {@code true} if the skill is a transformation, {@code false} otherwise.
	 */
	public boolean isTransformation()
	{
		return (_abnormalType == AbnormalType.TRANSFORM) || (_abnormalType == AbnormalType.CHANGEBODY);
	}
	
	/**
	 * Retrieves the current effect point value.<br>
	 * This value represents the specific point assigned to the effect.
	 * @return the current {@code int} value of the effect point.
	 */
	public int getEffectPoint()
	{
		return _effectPoint;
	}
	
	/**
	 * Checks if the skill contains specific physical attack effects.<br>
	 * It verifies if the skill includes {@code PHYSICAL_ATTACK} or {@code PHYSICAL_ATTACK_HP_LINK}.
	 * @return {@code true} if any of these effect types are present, otherwise {@code false}.
	 */
	public boolean useSoulShot()
	{
		return hasEffectType(EffectType.PHYSICAL_ATTACK, EffectType.PHYSICAL_ATTACK_HP_LINK);
	}
	
	/**
	 * Checks if the skill is a spirit shot.<br>
	 * This method returns {@code true} if the magic level is equal to {@code 1}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the skill is a spirit shot, {@code false} otherwise.
	 */
	public boolean useSpiritShot()
	{
		return _magic == 1;
	}
	
	/**
	 * Checks if the skill includes a fishing effect.<br>
	 * This method returns {@code true} if the skill is associated with {@code EffectType.FISHING}.
	 * @return {@code true} if the skill has a fishing effect, otherwise {@code false}.
	 */
	public boolean useFishShot()
	{
		return hasEffectType(EffectType.FISHING);
	}
	
	/**
	 * Retrieves the minimum pledge class required for this skill.<br>
	 * This value determines if a player meets the necessary requirements to use it.
	 * @return the minimum pledge class level as an {@code int}.
	 */
	public int getMinPledgeClass()
	{
		return _minPledgeClass;
	}
	
	/**
	 * Checks if the skill is classified as a hero skill.<br>
	 * This method queries the {@code SkillTreeData} based on the current ID and level.
	 * @return {@code true} if it is a hero skill, {@code false} otherwise.
	 */
	public boolean isHeroSkill()
	{
		return SkillTreeData.getInstance().isHeroSkill(_id, _level);
	}
	
	/**
	 * Checks if the skill is a GMS skill.<br>
	 * This method verifies the skill status based on its ID and level.
	 * @return {@code true} if it is a GMS skill, {@code false} otherwise.
	 */
	public boolean isGMSkill()
	{
		return SkillTreeData.getInstance().isGMSkill(_id, _level);
	}
	
	/**
	 * Checks if the skill belongs to the 7 Signs group.<br>
	 * This is determined by comparing the {@code id}.
	 * @return {@code true} if the ID is between 4361 and 4366 inclusive, otherwise {@code false}.
	 */
	public boolean is7Signs()
	{
		return (_id > 4360) && (_id < 4367);
	}
	
	/**
	 * Checks if the skill provides health recovery.<br>
	 * This method returns {@code true} if the {@code AbnormalType} is set to {@code HP_RECOVER}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if this is a healing skill, {@code false} otherwise.
	 */
	public boolean isHealingPotionSkill()
	{
		return _abnormalType == AbnormalType.HP_RECOVER;
	}
	
	/**
	 * Returns the maximum number of souls that can be consumed.<br>
	 * This value is retrieved from the {@code _soulMaxConsume} field.
	 * @return The maximum soul consumption count as an {@code int}.
	 */
	public int getMaxSoulConsumeCount()
	{
		return _soulMaxConsume;
	}
	
	/**
	 * Retrieves the amount of charge consumed by this skill.
	 * @return the {@code int} value representing the charge consumption.
	 */
	public int getChargeConsumeCount()
	{
		return _chargeConsume;
	}
	
	/**
	 * Checks if the skill effect persists after the owner dies.<br>
	 * This returns {@code true} if any of the stay-after-death conditions are met.
	 * @return {@code true} if the effect remains active, {@code false} otherwise.
	 */
	public boolean isStayAfterDeath()
	{
		return _stayAfterDeath || _irreplaceableBuff || _isNecessaryToggle;
	}
	
	/**
	 * Checks if the skill has a negative effect.<br>
	 * This method returns {@code true} if the internal effect point is less than {@code 0}.
	 * @return {@code true} if the effect is negative, otherwise {@code false}.
	 */
	public boolean hasNegativeEffect()
	{
		return _effectPoint < 0;
	}
	
	/**
	 * Retrieves the ID of the alternate ranged skill.<br>
	 * This value is used to determine which secondary skill can be triggered.
	 * @return the {@code int} identifier for the alternate ranged skill.
	 */
	public int getAlternateRangedSkillId()
	{
		return _alternateRangedSkillId;
	}
	
	/**
	 * Retrieves the ID of the alternate melee skill.<br>
	 * This value is used to determine which secondary skill can be triggered.
	 * @return the {@code int} identifier for the alternate melee skill.
	 */
	public int getAlternateMeleeSkillId()
	{
		return _alternateMeleeSkillId;
	}
	
	/**
	 * Retrieves the ID of the alternate skill used against enemies.<br>
	 * This value is returned when an enemy performs a specific counter action.
	 * @return the {@code int} identifier for the alternate enemy skill.
	 */
	public int getAlternateEnemySkillId()
	{
		return _alternateEnemySkillId;
	}
	
	/**
	 * Retrieves the ID of the alternate ally skill.<br>
	 * This value is used to determine which secondary skill is triggered for allies.
	 * @return the {@code int} ID of the alternate ally skill.
	 */
	public int getAlternateAllySkillId()
	{
		return _alternateAllySkillId;
	}
	
	/**
	 * Checks if the {@code creature} can use a skill on the specified {@code object}.<br>
	 * It validates the general and the target conditions of the skill.<br>
	 * If requirements are not met, it may send a system message to the {@code creature}.
	 * @param creature The entity attempting to use the skill.
	 * @param object The target of the skill.
	 * @param sendMessage Whether to notify the {@code creature} if the skill fails.
	 * @return {@code true} if all conditions are met, {@code false} otherwise.
	 */
	public boolean checkCondition(Creature creature, WorldObject object, boolean sendMessage)
	{
		if (creature.isFakePlayer() || (creature.isGM() && !GeneralConfig.GM_SKILL_RESTRICTION))
		{
			return true;
		}
		
		if (!checkConditions(SkillConditionScope.GENERAL, creature, object) || !checkConditions(SkillConditionScope.TARGET, creature, object))
		{
			if (sendMessage && !((creature == object) && hasNegativeEffect())) // Self targeted negative effect skills should not send a message.
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
				sm.addSkillName(_id);
				creature.sendPacket(sm);
			}
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * Finds the target {@code WorldObject} for a specific {@code Creature}.<br>
	 * This method determines which object will be affected by an action.
	 * @param creature The {@code Creature} performing the action.
	 * @param forceUse Whether to ignore normal restrictions and force the use of the target.
	 * @param dontMove Whether to prevent the actor from moving toward the target.
	 * @param sendMessage Whether to send a message to the player regarding the target selection.
	 * @return The {@code WorldObject} selected as the target, or {@code null} if no target is found.
	 */
	public WorldObject getTarget(Creature creature, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		return getTarget(creature, creature.getTarget(), forceUse, dontMove, sendMessage);
	}
	
	/**
	 * Determines the target for a specific skill based on its target type.<br>
	 * This method uses the appropriate {@code TargetHandler} to find the valid {@code WorldObject}.<br>
	 * If no handler is found, it sends an error message to the {@code Creature}.
	 * @param creature The {@code Creature} attempting to use the skill.
	 * @param seletedTarget The currently selected {@code WorldObject} target.
	 * @param forceUse Whether to ignore certain restrictions when forcing the use of the skill.
	 * @param dontMove Whether the {@code Creature} should remain stationary during the process.
	 * @param sendMessage Whether to send a confirmation message to the user.
	 * @return The resulting {@code WorldObject} target, or {@code null} if no target is found.
	 */
	public WorldObject getTarget(Creature creature, WorldObject seletedTarget, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		final ITargetTypeHandler handler = TargetHandler.getInstance().getHandler(getTargetType());
		if (handler != null)
		{
			try
			{
				return handler.getTarget(creature, seletedTarget, this, forceUse, dontMove, sendMessage);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception in Skill.getTarget(): " + e.getMessage(), e);
			}
		}
		
		creature.sendMessage("Target type of skill " + this + " is not currently handled.");
		return null;
	}
	
	/**
	 * Determines which {@code WorldObject} instances are affected by the skill.<br>
	 * This method uses the appropriate {@code IAffectScopeHandler} based on the skill's scope.<br>
	 * It also ensures that monsters cannot apply positive effects to playable characters.
	 * @param creature The {@code Creature} casting the skill.
	 * @param target The primary {@code WorldObject} targeted by the skill.
	 * @return A list of affected {@code WorldObject} instances, or {@code null} if no handler is found or the target is invalid.
	 */
	public List<WorldObject> getTargetsAffected(Creature creature, WorldObject target)
	{
		if (target == null)
		{
			return null;
		}
		
		final IAffectScopeHandler handler = AffectScopeHandler.getInstance().getHandler(getAffectScope());
		if (handler != null)
		{
			try
			{
				final List<WorldObject> result = new ArrayList<>();
				handler.forEachAffected(creature, target, this, result::add);
				
				// Prevent monsters buffing playables.
				if ((creature != null) && creature.isMonster() && !hasNegativeEffect())
				{
					result.removeIf(wo -> wo.isPlayable());
				}
				
				return result;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception in Skill.getTargetsAffected(): " + e.getMessage(), e);
			}
		}
		
		if (creature != null)
		{
			creature.sendMessage("Target affect scope of skill " + this + " is not currently handled.");
		}
		
		return null;
	}
	
	/**
	 * Executes an action for all objects affected by the skill's scope.<br>
	 * This method uses the {@code AffectScopeHandler} to determine which targets are impacted.<br>
	 * If no handler is found, a message is sent to the {@code creature}.
	 * @param creature The {@code Creature} performing the action.
	 * @param target The primary {@code WorldObject} being targeted.
	 * @param action The {@code Consumer} defining what to do with each affected object.
	 */
	public void forEachTargetAffected(Creature creature, WorldObject target, Consumer<? super WorldObject> action)
	{
		if (target == null)
		{
			return;
		}
		
		final IAffectScopeHandler handler = AffectScopeHandler.getInstance().getHandler(getAffectScope());
		if (handler != null)
		{
			try
			{
				handler.forEachAffected(creature, target, this, action);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception in Skill.forEachTargetAffected(): " + e.getMessage(), e);
			}
		}
		else
		{
			creature.sendMessage("Target affect scope of skill " + this + " is not currently handled.");
		}
	}
	
	/**
	 * Adds a specific effect to an existing scope.<br>
	 * This method stores the {@code AbstractEffect} inside the provided {@code EffectScope}.<br>
	 * If the scope does not exist yet, it will be created automatically.
	 * @param effectScope The category or scope where the effect should be stored.
	 * @param effect The specific effect to add to the list.
	 */
	public void addEffect(EffectScope effectScope, AbstractEffect effect)
	{
		_effectLists.computeIfAbsent(effectScope, _ -> new ArrayList<>()).add(effect);
	}
	
	/**
	 * Retrieves the list of effects associated with a specific scope.<br>
	 * This method looks up the {@code AbstractEffect} objects based on the provided {@code EffectScope}.
	 * @param effectScope The scope used to identify which effects to return.
	 * @return A {@code List} of {@code AbstractEffect} objects matching the given scope.
	 */
	public List<AbstractEffect> getEffects(EffectScope effectScope)
	{
		return _effectLists.get(effectScope);
	}
	
	/**
	 * Checks if there are any active effects for a specific scope.<br>
	 * It returns {@code true} if the list of effects is not empty.<br>
	 * It returns {@code false} if no effects exist or the list is {@code null}.
	 * @param effectScope The {@code EffectScope} to check for active effects.
	 * @return {@code true} if effects are present, otherwise {@code false}.
	 */
	public boolean hasEffects(EffectScope effectScope)
	{
		final List<AbstractEffect> effects = _effectLists.get(effectScope);
		return (effects != null) && !effects.isEmpty();
	}
	
	/**
	 * Applies effects to a target based on the provided scope and information.<br>
	 * This method processes both instant and continuous effects depending on the boolean flags.<br>
	 * It checks for success before applying any changes to the creatures.
	 * @param effectScope The scope used to determine which effects should be applied.
	 * @param info The data containing the effector, effected creature, and other context.
	 * @param applyInstantEffects Set to {@code true} to allow the application of instant effects.
	 * @param addContinuousEffects Set to {@code true} to allow the addition of continuous effects.
	 */
	public void applyEffectScope(EffectScope effectScope, BuffInfo info, boolean applyInstantEffects, boolean addContinuousEffects)
	{
		if ((effectScope != null) && hasEffects(effectScope))
		{
			for (AbstractEffect effect : getEffects(effectScope))
			{
				if (effect.isInstant())
				{
					if (applyInstantEffects)
					{
						final Creature effector = info.getEffector();
						final Creature effected = info.getEffected();
						if (effect.calcSuccess(effector, effected, this))
						{
							effect.instant(effector, effected, this, info.getItem());
						}
					}
				}
				else if (addContinuousEffects)
				{
					final Creature effector = info.getEffector();
					final Creature effected = info.getEffected();
					if (applyInstantEffects)
					{
						effect.continuousInstant(effector, effected, this, info.getItem());
					}
					
					if (effect.canStart(effector, effected, this))
					{
						info.addEffect(effect);
					}
					
					// Tempfix for hp/mp regeneration.
					// TODO: Find where regen stops and make a proper fix
					if (effected.isPlayer() && !hasNegativeEffect())
					{
						effected.asPlayer().getStatus().startHpMpRegeneration();
					}
				}
			}
		}
	}
	
	/**
	 * Applies the effects of a skill from one creature to another.<br>
	 * This method handles the logic for transferring status changes or buffs.
	 * @param effector The {@code Creature} that is casting or initiating the effect.
	 * @param effected The {@code Creature} that will receive the effect.
	 */
	public void applyEffects(Creature effector, Creature effected)
	{
		applyEffects(effector, effected, false, false, true, 0, null);
	}
	
	/**
	 * Applies the effects of a skill to a target.<br>
	 * This method processes the interaction between an effector and an affected creature.<br>
	 * It uses the provided {@code Item} to determine specific effect parameters.
	 * @param effector The {@code Creature} that is casting or triggering the effect.
	 * @param effected The {@code Creature} that receives the effect.
	 * @param item The {@code Item} used to trigger the effect.
	 */
	public void applyEffects(Creature effector, Creature effected, Item item)
	{
		applyEffects(effector, effected, false, false, true, 0, item);
	}
	
	/**
	 * Applies the effects of a skill from one creature to another.<br>
	 * This method handles both immediate and delayed effect logic.
	 * @param effector The {@code Creature} that is casting or triggering the effect.
	 * @param effected The {@code Creature} that will receive the effect.
	 * @param instant A boolean indicating if the effect should happen immediately.
	 * @param abnormalTime The duration of the abnormal effect in milliseconds.
	 */
	public void applyEffects(Creature effector, Creature effected, boolean instant, int abnormalTime)
	{
		applyEffects(effector, effected, false, false, instant, abnormalTime, null);
	}
	
	/**
	 * Applies the effects of a skill to a target creature.<br>
	 * This method handles different scopes such as self, passive, and continuous effects.<br>
	 * It also manages special logic for aura resets and buff sharing with summons.
	 * @param effector The {@code Creature} who is casting or initiating the effect.
	 * @param effected The {@code Creature} who will receive the effect.
	 * @param self Whether the effect is being applied to the caster themselves.
	 * @param passive Whether the effect is a passive buff.
	 * @param instant Whether the effect should be applied instantly rather than as a duration.
	 * @param abnormalTime The amount of time for abnormal effects, if applicable.
	 * @param item The {@code Item} associated with this effect application.
	 */
	public void applyEffects(Creature effector, Creature effected, boolean self, boolean passive, boolean instant, int abnormalTime, Item item)
	{
		// Null targets cannot receive any effects.
		if (effected == null)
		{
			return;
		}
		
		if (effected.isIgnoringSkillEffects(_id, _level))
		{
			return;
		}
		
		final int skillId = effector.isPlayer() ? effector.asPlayer().getReplacementSkill(_id) : _id;
		boolean addContinuousEffects = !passive && (_operateType.isToggle() || (_operateType.isContinuous() && Formulas.calcEffectSuccess(effector, effected, this)));
		if (!self && !passive)
		{
			final BuffInfo info;
			if (skillId != _id)
			{
				info = new BuffInfo(effector, effected, SkillData.getInstance().getSkill(skillId, getLevel(), getSubLevel()), !instant, item, null);
			}
			else
			{
				info = new BuffInfo(effector, effected, this, !instant, item, null);
			}
			
			if (addContinuousEffects && (abnormalTime > 0))
			{
				info.setAbnormalTime(abnormalTime);
			}
			
			applyEffectScope(EffectScope.GENERAL, info, instant, addContinuousEffects);
			
			final boolean effectorIsPlayable = effector.isPlayable();
			final EffectScope pvpOrPveEffectScope = effectorIsPlayable && effected.isAttackable() ? EffectScope.PVE : effectorIsPlayable && effected.isPlayable() ? EffectScope.PVP : null;
			applyEffectScope(pvpOrPveEffectScope, info, instant, addContinuousEffects);
			if (addContinuousEffects)
			{
				// Aura skills reset the abnormal time.
				final EffectList effectedEffectList = effected.getEffectList();
				final BuffInfo existingInfo = _operateType.isAura() ? effectedEffectList.getBuffInfoBySkillId(skillId) : null;
				if (existingInfo != null)
				{
					existingInfo.resetAbnormalTime(info.getAbnormalTime());
				}
				else
				{
					effectedEffectList.add(info);
				}
				
				// Check for mesmerizing debuffs and increase resist level.
				if (_isDebuff && (_basicProperty != BasicProperty.NONE) && effected.hasBasicPropertyResist())
				{
					final BasicPropertyResist resist = effected.getBasicPropertyResist(_basicProperty);
					resist.increaseResistLevel();
				}
			}
			
			// Support for buff sharing feature including healing herbs.
			if (_isSharedWithSummon && effected.isPlayer() && effected.hasServitors() && !isTransformation() && ((addContinuousEffects && isContinuous() && !_isDebuff) || _isRecoveryHerb))
			{
				effected.getServitors().values().forEach(s -> applyEffects(effector, s, _isRecoveryHerb, 0));
			}
		}
		
		if (self)
		{
			addContinuousEffects = !passive && (_operateType.isToggle() || (_operateType.isSelfContinuous() && Formulas.calcEffectSuccess(effector, effector, this)));
			
			final BuffInfo info;
			if (skillId != _id)
			{
				info = new BuffInfo(effector, effected, SkillData.getInstance().getSkill(skillId, getLevel(), getSubLevel()), !instant, item, null);
			}
			else
			{
				info = new BuffInfo(effector, effector, this, !instant, item, null);
			}
			
			if (addContinuousEffects && (abnormalTime > 0))
			{
				info.setAbnormalTime(abnormalTime);
			}
			
			applyEffectScope(EffectScope.SELF, info, instant, addContinuousEffects);
			if (addContinuousEffects)
			{
				// Aura skills reset the abnormal time.
				final BuffInfo existingInfo = _operateType.isAura() ? effector.getEffectList().getBuffInfoBySkillId(skillId) : null;
				if (existingInfo != null)
				{
					existingInfo.resetAbnormalTime(info.getAbnormalTime());
				}
				else
				{
					info.getEffector().getEffectList().add(info);
				}
			}
			
			// Support for buff sharing feature.
			// Avoiding Servitor Share since it's implementation already "shares" the effect.
			if (addContinuousEffects && _isSharedWithSummon)
			{
				if (effector.isPlayer() && isContinuous() && !_isDebuff && effector.hasServitors())
				{
					effector.getServitors().values().forEach(s -> applyEffects(effector, s, false, 0));
				}
			}
		}
		
		if (passive)
		{
			final BuffInfo info;
			if (skillId != _id)
			{
				info = new BuffInfo(effector, effected, SkillData.getInstance().getSkill(skillId, getLevel(), getSubLevel()), true, item, null);
			}
			else
			{
				info = new BuffInfo(effector, effector, this, true, item, null);
			}
			
			applyEffectScope(EffectScope.GENERAL, info, false, true);
			effector.getEffectList().add(info);
		}
	}
	
	/**
	 * Applies channeling effects to a target creature.<br>
	 * This method checks if the {@code effected} target is {@code null}.<br>
	 * If the target exists, it applies the effect using the {@code CHANNELING} scope.
	 * @param effector The {@code Creature} casting or initiating the effect.
	 * @param effected The {@code Creature} receiving the effect.
	 */
	public void applyChannelingEffects(Creature effector, Creature effected)
	{
		// Null targets cannot receive any effects.
		if (effected == null)
		{
			return;
		}
		
		final BuffInfo info = new BuffInfo(effector, effected, this, false, null, null);
		applyEffectScope(EffectScope.CHANNELING, info, true, true);
	}
	
	/**
	 * Activates a skill for a specific caster on a group of targets.<br>
	 * This method processes the skill logic and applies effects to all objects in the collection.
	 * @param caster The {@code Creature} who is performing the skill action.
	 * @param targets A {@code Collection} of {@code WorldObject} entities that will be affected by the skill.
	 */
	public void activateSkill(Creature caster, Collection<WorldObject> targets)
	{
		activateSkill(caster, null, targets);
	}
	
	/**
	 * Activates a skill and applies its effects to the specified targets.<br>
	 * This method handles reflection logic, self-effects, and special shot types.<br>
	 * It also processes suicide attacks if the skill is configured for them.
	 * @param caster The {@code Creature} who is performing the skill action.
	 * @param item The {@code Item} used to activate the skill.
	 * @param targets A {@code Collection} of {@code WorldObject} entities affected by the skill.
	 */
	public void activateSkill(Creature caster, Item item, Collection<WorldObject> targets)
	{
		// The bar above a creature refreshes only for whoever has it selected, so an area hit has to announce the rest.
		final boolean areaHit = targets.size() > 1;
		for (WorldObject target : targets)
		{
			if (!target.isCreature() || (target.isSummon() && !isSharedWithSummon()))
			{
				continue;
			}
			
			final Creature skillTarget = target.asCreature();
			final double healthBefore = skillTarget.getCurrentHp();
			if (Formulas.calcBuffDebuffReflection(skillTarget, this))
			{
				// If skill is reflected instant effects should be casted on target and continuous effects on caster.
				applyEffects(skillTarget, caster, false, 0);
				
				final BuffInfo info = new BuffInfo(caster, skillTarget, this, false, item, null);
				applyEffectScope(EffectScope.GENERAL, info, true, false);
				
				final boolean isPlayable = caster.isPlayable();
				final boolean isAttackable = skillTarget.isAttackable();
				final EffectScope pvpOrPveEffectScope = isPlayable && isAttackable ? EffectScope.PVE : isPlayable && skillTarget.isPlayable() ? EffectScope.PVP : null;
				applyEffectScope(pvpOrPveEffectScope, info, true, false);
			}
			else
			{
				applyEffects(caster, skillTarget, item);
			}
			
			if (areaHit && (skillTarget.getCurrentHp() != healthBefore))
			{
				skillTarget.broadcastStatusUpdate(caster);
			}
		}
		
		// Self Effect.
		if (hasEffects(EffectScope.SELF))
		{
			if (caster.isAffectedBySkill(_id))
			{
				caster.stopSkillEffects(SkillFinishType.REMOVED, _id);
			}
			
			applyEffects(caster, caster, true, false, true, 0, item);
		}
		
		if (!caster.isCubic())
		{
			if (useSpiritShot())
			{
				caster.unchargeShot(caster.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS);
			}
			else if (useSoulShot())
			{
				caster.unchargeShot(caster.isChargedShot(ShotType.BLESSED_SOULSHOTS) ? ShotType.BLESSED_SOULSHOTS : ShotType.SOULSHOTS);
			}
		}
		
		if (_isSuicideAttack)
		{
			caster.doDie(caster);
		}
	}
	
	/**
	 * Activates a skill using the default effect. <br>
	 * This method triggers the skill logic for a specific {@code Creature} and {@code WorldObject}.
	 * @param caster The {@code Creature} who is casting the skill.
	 * @param target The {@code WorldObject} that is being targeted by the skill.
	 */
	public void activateSkill(Creature caster, WorldObject target)
	{
		activateSkill(caster, target, null);
	}
	
	/**
	 * Activates the skill and applies its effects to the target and caster.<br>
	 * This method handles reflection logic, self-effects, and special shot types.<br>
	 * It also manages suicide attack mechanics if applicable.
	 * @param caster The {@code Creature} performing the skill action.
	 * @param target The {@code WorldObject} being targeted by the skill.
	 * @param item The {@code Item} used to activate the skill.
	 */
	public void activateSkill(Creature caster, WorldObject target, Item item)
	{
		if (target.isCreature() && (!target.isSummon() || isSharedWithSummon()))
		{
			final Creature skillTarget = target.asCreature();
			if (Formulas.calcBuffDebuffReflection(skillTarget, this))
			{
				// If skill is reflected instant effects should be casted on target and continuous effects on caster.
				applyEffects(skillTarget, caster, false, 0);
				
				final BuffInfo info = new BuffInfo(caster, skillTarget, this, false, item, null);
				applyEffectScope(EffectScope.GENERAL, info, true, false);
				
				final boolean isPlayable = caster.isPlayable();
				final boolean isAttackable = skillTarget.isAttackable();
				final EffectScope pvpOrPveEffectScope = isPlayable && isAttackable ? EffectScope.PVE : isPlayable && skillTarget.isPlayable() ? EffectScope.PVP : null;
				applyEffectScope(pvpOrPveEffectScope, info, true, false);
			}
			else
			{
				applyEffects(caster, skillTarget, item);
			}
		}
		
		// Self Effect.
		if (hasEffects(EffectScope.SELF))
		{
			if (caster.isAffectedBySkill(_id))
			{
				caster.stopSkillEffects(SkillFinishType.REMOVED, _id);
			}
			
			applyEffects(caster, caster, true, false, true, 0, item);
		}
		
		if (!caster.isCubic())
		{
			if (useSpiritShot())
			{
				caster.unchargeShot(caster.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS);
			}
			else if (useSoulShot())
			{
				caster.unchargeShot(caster.isChargedShot(ShotType.BLESSED_SOULSHOTS) ? ShotType.BLESSED_SOULSHOTS : ShotType.SOULSHOTS);
			}
		}
		
		if (_isSuicideAttack)
		{
			caster.doDie(caster);
		}
	}
	
	/**
	 * Adds a specific skill condition to a defined scope.<br>
	 * This method stores the {@code ISkillCondition} inside the list associated with the {@code SkillConditionScope}.
	 * @param skillConditionScope The scope where the condition will be added.
	 * @param skillCondition The condition to add to the specified scope.
	 */
	public void addCondition(SkillConditionScope skillConditionScope, ISkillCondition skillCondition)
	{
		_conditionLists.computeIfAbsent(skillConditionScope, _ -> new ArrayList<>()).add(skillCondition);
	}
	
	/**
	 * Checks if all required conditions for a skill are met.<br>
	 * It validates the requirements based on the provided {@code SkillConditionScope}.<br>
	 * If no conditions exist for the scope, it returns {@code true}.
	 * @param skillConditionScope The specific scope used to determine which conditions to check.
	 * @param caster The creature casting the skill.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if all conditions are satisfied, otherwise {@code false}.
	 */
	public boolean checkConditions(SkillConditionScope skillConditionScope, Creature caster, WorldObject target)
	{
		final List<ISkillCondition> conditions = _conditionLists.get(skillConditionScope);
		if (conditions == null)
		{
			return true;
		}
		
		for (ISkillCondition condition : conditions)
		{
			if (!condition.canUse(caster, this, target))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Returns a string representation of the skill.<br>
	 * This includes the name, ID, level, and sub-level.
	 * @return A formatted string describing the skill.
	 */
	@Override
	public String toString()
	{
		return "Skill " + _name + "(" + _id + "," + _level + "," + _subLevel + ")";
	}
	
	/**
	 * Retrieves the unique reference identifier for this skill.<br>
	 * This ID is used to distinguish between different versions of a skill.
	 * @return The {@code int} value of the reference item ID.
	 */
	public int getReferenceItemId()
	{
		return _refId;
	}
	
	/**
	 * Checks if the skill effect can be removed by a dispel action.
	 * @return {@code true} if the effect is dispellable, {@code false} otherwise.
	 */
	public boolean canBeDispelled()
	{
		return _canBeDispelled;
	}
	
	/**
	 * Checks if the skill can be stolen by other players.<br>
	 * This method evaluates several conditions including whether the skill is passive, a toggle, or a debuff.<br>
	 * It also verifies specific flags like hero skills and game master skills.
	 * @return {@code true} if the skill meets all requirements to be stolen, {@code false} otherwise.
	 */
	public boolean canBeStolen()
	{
		return !isPassive() && !isToggle() && !_isDebuff && !_irreplaceableBuff && !isHeroSkill() && !isGMSkill() && !(isStatic() && (getId() != CommonSkill.CARAVANS_SECRET_MEDICINE.getId())) && _canBeDispelled;
	}
	
	/**
	 * Checks if the skill belongs to a clan.<br>
	 * It verifies this based on the skill ID and level.
	 * @return {@code true} if it is a clan skill, {@code false} otherwise.
	 */
	public boolean isClanSkill()
	{
		return SkillTreeData.getInstance().isClanSkill(_id, _level);
	}
	
	/**
	 * Checks if this skill is excluded from specific validation checks.<br>
	 * Returns {@code true} if the skill should be skipped during processing.<br>
	 * Returns {@code false} if the skill should be checked normally.
	 * @return a boolean indicating the exclusion status.
	 */
	public boolean isExcludedFromCheck()
	{
		return _excludedFromCheck;
	}
	
	/**
	 * Checks if the skill is performed without an action.<br>
	 * This indicates whether the skill can be triggered automatically.
	 * @return {@code true} if the skill has no associated action, {@code false} otherwise.
	 */
	public boolean isWithoutAction()
	{
		return _withoutAction;
	}
	
	/**
	 * Parses a semicolon-separated string of visual effects.<br>
	 * It converts each name into an {@code AbnormalVisualEffect} object.<br>
	 * Valid effects are stored in the internal collection.<br>
	 * Invalid effect names will trigger a warning log.
	 * @param abnormalVisualEffects A string containing effect names separated by semicolons.
	 */
	private void parseAbnormalVisualEffect(String abnormalVisualEffects)
	{
		if (abnormalVisualEffects != null)
		{
			final String[] data = abnormalVisualEffects.split(";");
			final Set<AbnormalVisualEffect> aves = new HashSet<>(1);
			for (String aveString : data)
			{
				final AbnormalVisualEffect ave = AbnormalVisualEffect.findByName(aveString);
				if (ave != null)
				{
					aves.add(ave);
				}
				else
				{
					LOGGER.warning("Invalid AbnormalVisualEffect(" + this + ") found for Skill(" + aveString + ")");
				}
			}
			
			if (!aves.isEmpty())
			{
				_abnormalVisualEffects = aves;
			}
		}
	}
	
	/**
	 * Checks if the skill contains a specific effect type or any of the provided types.<br>
	 * This method searches through all effects associated with the skill.
	 * @param effectType The primary {@code EffectType} to check for.
	 * @param effectTypes A variable number of additional {@code EffectType} values to check.
	 * @return {@code true} if any of the requested types are found, otherwise {@code false}.
	 */
	public boolean hasEffectType(EffectType effectType, EffectType... effectTypes)
	{
		if (_effectTypes == null)
		{
			synchronized (this)
			{
				if (_effectTypes == null)
				{
					final Set<Byte> effectTypesSet = new HashSet<>();
					for (List<AbstractEffect> effectList : _effectLists.values())
					{
						if (effectList != null)
						{
							for (AbstractEffect effect : effectList)
							{
								effectTypesSet.add((byte) effect.getEffectType().ordinal());
							}
						}
					}
					
					final Byte[] effectTypesArray = effectTypesSet.toArray(new Byte[0]);
					Arrays.sort(effectTypesArray);
					_effectTypes = effectTypesArray;
				}
			}
		}
		
		if (Arrays.binarySearch(_effectTypes, (byte) effectType.ordinal()) >= 0)
		{
			return true;
		}
		
		for (EffectType type : effectTypes)
		{
			if (Arrays.binarySearch(_effectTypes, (byte) type.ordinal()) >= 0)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the skill contains specific effects for a given scope.<br>
	 * This method returns {@code true} if any of the provided effect types match.<br>
	 * It ignores cases where the scope already has active effects.
	 * @param effectScope The scope to check for effects.
	 * @param effectType The primary effect type to look for.
	 * @param effectTypes A variable number of additional effect types to check.
	 * @return {@code true} if a matching effect is found, otherwise {@code false}.
	 */
	public boolean hasEffectType(EffectScope effectScope, EffectType effectType, EffectType... effectTypes)
	{
		if (hasEffects(effectScope))
		{
			return false;
		}
		
		for (AbstractEffect effect : _effectLists.get(effectScope))
		{
			final EffectType currentType = effect.getEffectType();
			if (effectType == currentType)
			{
				return true;
			}
			
			for (EffectType type : effectTypes)
			{
				if (type == currentType)
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves the icon associated with this skill.<br>
	 * This method returns the internal icon identifier.
	 * @return The {@code String} representing the skill icon.
	 */
	public String getIcon()
	{
		return _icon;
	}
	
	/**
	 * Returns the interval between channeling ticks.<br>
	 * This value determines how often a channeled skill updates.
	 * @return The tick interval as a {@code long}.
	 */
	public long getChannelingTickInterval()
	{
		return _channelingTickInterval;
	}
	
	/**
	 * Gets the initial delay for a channeling skill.<br>
	 * This value represents the time before the channeling starts.
	 * @return The initial delay as a {@code long}.
	 */
	public long getChannelingTickInitialDelay()
	{
		return _channelingStart;
	}
	
	/**
	 * Checks if the skill is currently in a mentoring state.
	 * @return {@code true} if the skill is mentoring, {@code false} otherwise.
	 */
	public boolean isMentoring()
	{
		return _isMentoring;
	}
	
	/**
	 * Retrieves the skill attached to a specific toggle group for a creature.<br>
	 * This method checks if the {@code Creature} has an active effect matching the required toggle group ID.<br>
	 * It then returns the corresponding {@code Skill} based on the current level and sub-level.<br>
	 * Returns {@code null} if no valid attached skill is found.
	 * @param creature The {@code Creature} to check for active effects.
	 * @return The associated {@code Skill} object, or {@code null} if none exists.
	 */
	public Skill getAttachedSkill(Creature creature)
	{
		// If character is double casting, return double cast skill.
		if ((_doubleCastSkill > 0) && creature.isAffected(EffectFlag.DOUBLE_CAST))
		{
			return SkillData.getInstance().getSkill(getDoubleCastSkill(), getLevel(), getSubLevel());
		}
		
		// Default toggle group ID, assume nothing attached.
		if ((_attachToggleGroupId <= 0) || (_attachSkills == null))
		{
			return null;
		}
		
		int toggleSkillId = 0;
		for (BuffInfo info : creature.getEffectList().getEffects())
		{
			final Skill infoSkill = info.getSkill();
			if (infoSkill.getToggleGroupId() == _attachToggleGroupId)
			{
				toggleSkillId = infoSkill.getId();
				break;
			}
		}
		
		// No active toggles with this toggle group ID found.
		if (toggleSkillId == 0)
		{
			return null;
		}
		
		AttachSkillHolder attachedSkill = null;
		for (AttachSkillHolder ash : _attachSkills)
		{
			if (ash.getRequiredSkillId() == toggleSkillId)
			{
				attachedSkill = ash;
				break;
			}
		}
		
		// No attached skills for this toggle found.
		if (attachedSkill == null)
		{
			return null;
		}
		
		return SkillData.getInstance().getSkill(attachedSkill.getSkillId(), Math.min(SkillData.getInstance().getMaxLevel(attachedSkill.getSkillId()), _level), _subLevel);
	}
	
	/**
	 * Checks if the skill can be cast twice in a row.
	 * @return {@code true} if double casting is allowed, {@code false} otherwise.
	 */
	public boolean canDoubleCast()
	{
		return _canDoubleCast;
	}
	
	/**
	 * Retrieves the double cast skill status.<br>
	 * This value indicates if the skill can be cast twice.
	 * @return the {@code int} value of the double cast skill.
	 */
	public int getDoubleCastSkill()
	{
		return _doubleCastSkill;
	}
	
	/**
	 * Checks if the skill can be cast while the character is disabled.
	 * @return {@code true} if casting is allowed while disabled, {@code false} otherwise.
	 */
	public boolean canCastWhileDisabled()
	{
		return _canCastWhileDisabled;
	}
	
	/**
	 * Checks if the skill can be shared with a summon.<br>
	 * Returns {@code true} if it is shared, and {@code false} otherwise.
	 * @return The share status of the skill.
	 */
	public boolean isSharedWithSummon()
	{
		return _isSharedWithSummon;
	}
	
	/**
	 * Checks if the skill requires a toggle to be active.<br>
	 * Returns {@code true} if the skill must be toggled on or off.<br>
	 * Returns {@code false} otherwise.
	 * @return the status of the necessary toggle.
	 */
	public boolean isNecessaryToggle()
	{
		return _isNecessaryToggle;
	}
	
	/**
	 * Checks if the abnormal effect should be removed when a player leaves.<br>
	 * Returns {@code true} if the effect is deleted on leave.<br>
	 * Returns {@code false} otherwise.
	 * @return The status of deletion on leave.
	 */
	public boolean isDeleteAbnormalOnLeave()
	{
		return _deleteAbnormalOnLeave;
	}
	
	/**
	 * Checks if the skill provides an irreplaceable buff.<br>
	 * This determines if the effect cannot be overwritten by other buffs.
	 * @return {@code true} if the buff is irreplaceable, {@code false} otherwise.
	 */
	public boolean isIrreplaceableBuff()
	{
		return _irreplaceableBuff;
	}
	
	/**
	 * Checks if Debuff Resistance cannot stop this debuff when it always lands.
	 * @return {@code true} if the debuff ignores Debuff Resistance, {@code false} otherwise.
	 */
	public boolean isIgnoreDebuffResist()
	{
		return _ignoreDebuffResist;
	}
	
	/**
	 * Checks if this skill should be shown in the list.
	 * @return {@code true} if it is displayed, {@code false} otherwise.
	 */
	public boolean isDisplayInList()
	{
		return _displayInList;
	}
	
	/**
	 * Checks if the skill prevents other actions from being performed.<br>
	 * This determines if the action is considered a blocking move.
	 * @return {@code true} if the skill blocks other actions, {@code false} otherwise.
	 */
	public boolean isBlockActionUseSkill()
	{
		return _blockActionUseSkill;
	}
	
	/**
	 * Retrieves the unique identifier for the toggle group.<br>
	 * This ID determines which skills are linked together in a toggle system.
	 * @return The {@code int} value of the toggle group ID.
	 */
	public int getToggleGroupId()
	{
		return _toggleGroupId;
	}
	
	/**
	 * Retrieves the unique identifier for the attach toggle group.<br>
	 * This ID determines which skills are grouped together for toggling.
	 * @return the {@code int} value of the attach toggle group ID.
	 */
	public int getAttachToggleGroupId()
	{
		return _attachToggleGroupId;
	}
	
	/**
	 * Retrieves the list of skills attached to this object.
	 * @return a {@code List} of {@code AttachSkillHolder} objects.
	 */
	public List<AttachSkillHolder> getAttachSkills()
	{
		return _attachSkills;
	}
	
	/**
	 * Retrieves the set of abnormal resistances.<br>
	 * This method returns all {@code AbnormalType} values that are resisted.
	 * @return a {@code Set} containing the {@code AbnormalType} resists.
	 */
	public Set<AbnormalType> getAbnormalResists()
	{
		return _abnormalResists;
	}
	
	/**
	 * Retrieves the magic critical rate for this skill.<br>
	 * This value determines how often a magic attack triggers a critical hit.
	 * @return The current {@code double} value of the magic critical rate.
	 */
	public double getMagicCriticalRate()
	{
		return _magicCriticalRate;
	}
	
	/**
	 * Retrieves the type of the skill buff.<br>
	 * This value determines how the buff behaves in the game.
	 * @return the {@code SkillBuffType} associated with this skill.
	 */
	public SkillBuffType getBuffType()
	{
		return _buffType;
	}
	
	/**
	 * Checks if this skill can be used for enchanting.<br>
	 * It queries the {@code EnchantSkillGroupsData} instance to verify eligibility.
	 * @return {@code true} if the skill is enchantable, {@code false} otherwise.
	 */
	public boolean isEnchantable()
	{
		return EnchantSkillGroupsData.getInstance().isEnchantable(this);
	}
}
