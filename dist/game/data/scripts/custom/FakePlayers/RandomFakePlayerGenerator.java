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
package custom.FakePlayers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemGrade;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Builds a random fake player as a Monster type NPC template and registers it with {@link NpcData}, returning its npc id to spawn.<br>
 * A random playable {@link PlayerClass} drives the race, mage flag and sex; the level drives the {@link ItemGrade} of the gear ({@link #gradeForLevel(int)}).<br>
 * Name comes from {@link RandomNameGenerator}, equipment from {@link RandomEquipmentGenerator}, base stats from {@link PlayerTemplateData} and skills from {@link SkillTreeData}.<br>
 * Because a fake player's appearance is fixed on its template at load, each generated player is a fresh single use template with its own id.
 * @author Mobius
 */
public class RandomFakePlayerGenerator
{
	// Runtime npc ids start high to avoid clashing with datapack templates; nextFreeId() skips any that are taken.
	private static final int NPC_ID_BASE = 8000000;
	private static final AtomicInteger NEXT_ID = new AtomicInteger(NPC_ID_BASE);
	
	// Strider mount npc ids (Star Strider, Strider of Dusk) and their movement speed while mounted.
	private static final int[] STRIDER_IDS =
	{
		12527,
		12528
	};
	private static final int STRIDER_WALK_SPEED = 80;
	private static final int STRIDER_RUN_SPEED = 170;
	
	// Appearance bounds (face 0-2, hair style 0-4, hair color 0-3).
	private static final int FACE_COUNT = 3;
	private static final int HAIR_STYLE_COUNT = 5;
	private static final int HAIR_COLOR_COUNT = 4;
	
	// Private sell store sign shown above a shop fake player.
	private static final String[] SHOP_MESSAGES =
	{
		"WTS spoil mats, cheap!",
		"Buying all crystals, fast trade",
		"WTS soulshots, best price",
		"Mats and recipes, come look",
		"WTS keys and jewels, pm me",
		"Cheap D grade, no scam"
	};
	
	// Playable classes that have a race and a loaded template.
	private static final List<PlayerClass> ELIGIBLE_CLASSES = new ArrayList<>();
	
	// Classes that must never be generated. Add PlayerClass entries here to ban them.
	private static final Set<PlayerClass> BANNED_CLASSES = EnumSet.of(PlayerClass.SIGEL_KNIGHT, PlayerClass.TYRR_WARRIOR, PlayerClass.OTHELL_ROGUE, PlayerClass.YUL_ARCHER, PlayerClass.FEOH_WIZARD, PlayerClass.ISS_ENCHANTER, PlayerClass.WYNN_SUMMONER, PlayerClass.AEORE_HEALER);
	
	/**
	 * Collects the eligible classes on first use.
	 */
	private static synchronized void initialize()
	{
		if (!ELIGIBLE_CLASSES.isEmpty())
		{
			return;
		}
		
		final PlayerTemplateData templateData = PlayerTemplateData.getInstance();
		for (PlayerClass playerClass : PlayerClass.values())
		{
			if ((playerClass.getRace() != null) && !BANNED_CLASSES.contains(playerClass) && (templateData.getTemplate(playerClass) != null))
			{
				ELIGIBLE_CLASSES.add(playerClass);
			}
		}
	}
	
	/**
	 * Generates a random fake player of a random class and level.
	 * @return the registered npc id to spawn
	 */
	public static int generate()
	{
		return generate(false);
	}
	
	/**
	 * Generates a random fake player of a random class and level, optionally mounted on a strider.
	 * @param mounted {@code true} to ride a strider
	 * @return the registered npc id to spawn
	 */
	public static int generate(boolean mounted)
	{
		initialize();
		
		final PlayerClass playerClass = ELIGIBLE_CLASSES.get(Rnd.get(ELIGIBLE_CLASSES.size()));
		return generate(playerClass, Rnd.get(1, maxLevel()), mounted);
	}
	
	/**
	 * Generates a random fake player of the given class and level.
	 * @param playerClass the class to build
	 * @param level the character level, which also selects the gear grade
	 * @return the registered npc id to spawn
	 */
	public static int generate(PlayerClass playerClass, int level)
	{
		return generate(playerClass, level, false);
	}
	
	/**
	 * Generates a random fake player of the given class and level, optionally mounted on a strider.
	 * @param playerClass the class to build
	 * @param level the character level, which also selects the gear grade
	 * @param mounted {@code true} to ride a strider
	 * @return the registered npc id to spawn
	 */
	public static int generate(PlayerClass playerClass, int level, boolean mounted)
	{
		return generate(playerClass, level, mounted, null);
	}
	
	/**
	 * Generates a random Dwarf fake player sitting in a private sell store with a random store message.
	 * @return the registered npc id to spawn
	 */
	public static int generateShop()
	{
		return generateShop(SHOP_MESSAGES[Rnd.get(SHOP_MESSAGES.length)]);
	}
	
	/**
	 * Generates a random Dwarf fake player sitting in a private sell store showing the given message.
	 * @param message the private store sell message shown above the shop
	 * @return the registered npc id to spawn
	 */
	public static int generateShop(String message)
	{
		initialize();
		
		final List<PlayerClass> dwarves = new ArrayList<>();
		for (PlayerClass playerClass : ELIGIBLE_CLASSES)
		{
			if (playerClass.getRace() == Race.DWARF)
			{
				dwarves.add(playerClass);
			}
		}
		if (dwarves.isEmpty())
		{
			return generate(); // No dwarf class available, fall back to a normal fake player.
		}
		
		return generate(dwarves.get(Rnd.get(dwarves.size())), Rnd.get(1, maxLevel()), false, message);
	}
	
	/**
	 * Generates a random fake player of the given class and level, optionally mounted on a strider or sitting in a private sell store.
	 * @param playerClass the class to build
	 * @param level the character level, which also selects the gear grade
	 * @param mounted {@code true} to ride a strider
	 * @param shopMessage the private store sell message when the fake player is a shop, or {@code null} for a normal fake player
	 * @return the registered npc id to spawn
	 */
	private static int generate(PlayerClass playerClass, int level, boolean mounted, String shopMessage)
	{
		initialize();
		
		// Levels above the server maximum have no player stat table entry, so they are capped.
		final int cappedLevel = Math.min(level, maxLevel());
		final Race race = playerClass.getRace();
		final boolean isMage = playerClass.isMage();
		final boolean female = (race == Race.ERTHEIA) || Rnd.nextBoolean(); // Ertheia is a female only race.
		final int npcId = nextFreeId();
		
		final StatSet set = new StatSet();
		set.set("id", npcId);
		set.set("level", cappedLevel);
		set.set("type", "Monster");
		set.set("name", RandomNameGenerator.generateName(race, isMage, female));
		set.set("title", "");
		set.set("race", race.name());
		set.set("sex", female ? "FEMALE" : "MALE");
		set.set("face", Rnd.get(FACE_COUNT));
		set.set("hair", Rnd.get(HAIR_STYLE_COUNT));
		set.set("hairColor", Rnd.get(HAIR_COLOR_COUNT));
		set.set("attackable", true);
		set.set("targetable", true);
		set.set("talkable", false);
		set.set("randomWalk", false);
		set.set("randomAnimation", false); // Player-rendered fakes must not emit NPC social animations; it crashes the client.
		set.set("fakePlayer", true);
		set.set("classId", playerClass.getId());
		
		// Ride a random strider when requested (e.g. a route with "Strider" in its name), moving at strider speed.
		if (mounted)
		{
			set.set("mountType", MountType.STRIDER.ordinal());
			set.set("mountNpcId", STRIDER_IDS[Rnd.get(STRIDER_IDS.length)]);
			set.set("baseWalkSpd", STRIDER_WALK_SPEED);
			set.set("baseRunSpd", STRIDER_RUN_SPEED);
		}
		
		// Sit in a private sell store when a store message is given.
		if (shopMessage != null)
		{
			set.set("sitting", true);
			set.set("privateStoreType", 1); // PrivateStoreType.SELL
			set.set("privateStoreMessage", shopMessage);
		}
		
		applyEquipment(set, gradeForLevel(cappedLevel), playerClass);
		applyCombatAi(set, isMage);
		applyStats(set, playerClass, cappedLevel, female);
		
		final NpcTemplate template = new NpcTemplate(set);
		template.setSkills(buildSkills(playerClass, cappedLevel));
		template.setAISkillLists(null); // Never null! XML parsed templates always set this and AttackableAI.thinkAttack throws (silently) without it.
		NpcData.getInstance().addTemplate(template);
		return npcId;
	}
	
	/**
	 * Maps a character level to the gear grade it should wear.
	 * @param level the character level
	 * @return the matching {@link ItemGrade}
	 */
	public static ItemGrade gradeForLevel(int level)
	{
		if (level < 20)
		{
			return ItemGrade.NONE;
		}
		
		if (level < 40)
		{
			return ItemGrade.D;
		}
		
		if (level < 52)
		{
			return ItemGrade.C;
		}
		
		if (level < 61)
		{
			return ItemGrade.B;
		}
		
		if (level < 76)
		{
			return ItemGrade.A;
		}
		
		if (level < 85)
		{
			return ItemGrade.S;
		}
		
		return ItemGrade.R;
	}
	
	/**
	 * Sets the AI type and physical attack range from the weapon that {@link #applyEquipment} chose: bow and crossbow users become archers that keep distance, magic weapon users cast as mages, and everything else fights in melee.<br>
	 * The class mage flag is only a fallback for the rare case where no weapon was equipped.
	 * @param set the template StatSet being built, after {@link #applyEquipment}
	 * @param isMage the class mage flag, used only when there is no weapon
	 */
	private static void applyCombatAi(StatSet set, boolean isMage)
	{
		String aiType = isMage ? "MAGE" : "FIGHTER";
		int attackRange = 40;
		String attackType = "FIST";
		if (ItemData.getInstance().getTemplate(set.getInt("equipRHand", 0)) instanceof Weapon weapon)
		{
			final WeaponType weaponType = weapon.getItemType();
			attackRange = weapon.getBaseAttackRange();
			attackType = weaponType.name();
			if (weaponType.isRanged())
			{
				aiType = "ARCHER";
			}
			else if (weapon.isMagicWeapon())
			{
				aiType = "MAGE";
			}
			else
			{
				aiType = "FIGHTER";
			}
		}
		
		set.set("aiType", aiType);
		set.set("baseAtkRange", attackRange);
		set.set("baseAtkType", attackType);
	}
	
	/**
	 * Fills the paperdoll StatSet keys from a generated grade appropriate loadout.
	 * @param set the template StatSet being built
	 * @param grade the gear grade
	 * @param playerClass the class the gear is for
	 */
	private static void applyEquipment(StatSet set, ItemGrade grade, PlayerClass playerClass)
	{
		final ItemData itemData = ItemData.getInstance();
		for (int itemId : RandomEquipmentGenerator.generateEquipment(grade, playerClass))
		{
			final ItemTemplate item = itemData.getTemplate(itemId);
			if (item == null)
			{
				continue;
			}
			
			if (item.isWeapon())
			{
				set.set("equipRHand", itemId);
				continue;
			}
			
			if ((item instanceof Armor armor) && (armor.getItemType() == ArmorType.SHIELD))
			{
				set.set("equipLHand", itemId);
				continue;
			}
			
			final String slotKey = armorSlotKey(item.getBodyPart());
			if (slotKey != null)
			{
				set.set(slotKey, itemId);
			}
		}
	}
	
	/**
	 * Copies base stats from the player template into the NPC StatSet.
	 * @param set the template StatSet being built
	 * @param playerClass the class whose template supplies the stats
	 * @param level the character level for the per level HP and MP tables
	 * @param female whether to use the female collision size
	 */
	private static void applyStats(StatSet set, PlayerClass playerClass, int level, boolean female)
	{
		final PlayerTemplate template = PlayerTemplateData.getInstance().getTemplate(playerClass);
		if (template == null)
		{
			return;
		}
		
		set.set("baseSTR", template.getBaseSTR());
		set.set("baseCON", template.getBaseCON());
		set.set("baseDEX", template.getBaseDEX());
		set.set("baseINT", template.getBaseINT());
		set.set("baseWIT", template.getBaseWIT());
		set.set("baseMEN", template.getBaseMEN());
		set.set("baseHpMax", template.getBaseHpMax(level));
		set.set("baseMpMax", template.getBaseMpMax(level));
		set.set("baseHpReg", (float) template.getBaseHpRegen(level));
		set.set("baseMpReg", (float) template.getBaseMpRegen(level));
		set.set("basePAtk", template.getBasePAtk());
		set.set("baseMAtk", template.getBaseMAtk());
		set.set("basePDef", template.getBasePDef());
		set.set("baseMDef", template.getBaseMDef());
		set.set("basePAtkSpd", template.getBasePAtkSpd());
		set.set("baseCritRate", template.getBaseCritRate());
		set.set("collision_radius", female ? template.getFCollisionRadiusFemale() : template.getFCollisionRadius());
		set.set("collision_height", female ? template.getFCollisionHeightFemale() : template.getFCollisionHeight());
	}
	
	/**
	 * Builds the skill map the class would know at the given level.<br>
	 * It walks the complete class skill tree, keeps every skill whose required level is met, and picks the highest level of each skill.
	 * @param playerClass the class whose skills to grant
	 * @param level the character level
	 * @return a skill id to {@link Skill} map for {@link NpcTemplate#setSkills(Map)}
	 */
	private static Map<Integer, Skill> buildSkills(PlayerClass playerClass, int level)
	{
		final Map<Integer, Integer> bestLevels = new HashMap<>();
		for (SkillLearn learn : SkillTreeData.getInstance().getCompleteClassSkillTree(playerClass).values())
		{
			if (learn.getGetLevel() <= level)
			{
				bestLevels.merge(learn.getSkillId(), learn.getSkillLevel(), Math::max);
			}
		}
		
		final SkillData skillData = SkillData.getInstance();
		final Map<Integer, Skill> skills = new HashMap<>();
		for (Entry<Integer, Integer> entry : bestLevels.entrySet())
		{
			final Skill skill = skillData.getSkill(entry.getKey().intValue(), entry.getValue().intValue());
			if (skill != null)
			{
				skills.put(skill.getId(), skill);
			}
		}
		
		return skills;
	}
	
	/**
	 * Maps a body part to the fake player paperdoll StatSet key, or {@code null} when it is not a worn armor slot.
	 * @param bodyPart the item body part
	 * @return the StatSet key or {@code null}
	 */
	private static String armorSlotKey(BodyPart bodyPart)
	{
		switch (bodyPart)
		{
			case HEAD:
			{
				return "equipHead";
			}
			case CHEST:
			case FULL_ARMOR:
			{
				return "equipChest";
			}
			case LEGS:
			{
				return "equipLegs";
			}
			case GLOVES:
			{
				return "equipGloves";
			}
			case FEET:
			{
				return "equipFeet";
			}
			default:
			{
				return null;
			}
		}
	}
	
	/**
	 * Returns the highest character level the server has player stat tables for.
	 * @return the level cap
	 */
	private static int maxLevel()
	{
		return ExperienceData.getInstance().getMaxLevel() - 1;
	}
	
	/**
	 * Returns the next npc id not already used by a template.
	 * @return a free npc id
	 */
	private static int nextFreeId()
	{
		final NpcData npcData = NpcData.getInstance();
		int id = NEXT_ID.getAndIncrement();
		while (npcData.getTemplate(id) != null)
		{
			id = NEXT_ID.getAndIncrement();
		}
		
		return id;
	}
}
