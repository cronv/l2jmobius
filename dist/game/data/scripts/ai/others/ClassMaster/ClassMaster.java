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
package ai.others.ClassMaster;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerBecomeNoblesse;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerBypass;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLevelChanged;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerPressTutorialMark;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerProfessionChange;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.TutorialCloseHtml;
import org.l2jmobius.gameserver.network.serverpackets.TutorialShowQuestionMark;

/**
 * This class handles the artificial intelligence for the {@code ClassMaster} NPC.<br>
 * It manages interactions related to player classes and skill progression.
 * @author Nik, Mobius
 */
public class ClassMaster extends Script implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ClassMaster.class.getName());
	
	// NPCs
	private static final List<Integer> CLASS_MASTERS = new ArrayList<>();
	static
	{
		CLASS_MASTERS.add(31756); // Mr. Cat
		CLASS_MASTERS.add(31757); // Queen of Hearts
	}
	
	// Misc
	private boolean _isEnabled;
	private boolean _spawnClassMasters;
	private boolean _showPopupWindow;
	private final List<ClassChangeData> _classChangeData = new ArrayList<>();
	
	/**
	 * Initializes a new instance of the {@code ClassMaster} class.<br>
	 * This constructor calls the {@code load()} method to prepare data.<br>
	 * It also registers the necessary NPCs and talk IDs for class masters.
	 */
	public ClassMaster()
	{
		load();
		addStartNpc(CLASS_MASTERS);
		addTalkId(CLASS_MASTERS);
		addFirstTalkId(CLASS_MASTERS);
	}
	
	/**
	 * Loads the class change data from the configuration file.<br>
	 * This method clears existing data and parses {@code config/ClassMaster.xml}.<br>
	 * It logs the total number of loaded options to the console.
	 */
	@Override
	public void load()
	{
		_classChangeData.clear();
		parseDatapackFile("config/ClassMaster.xml");
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _classChangeData.size() + " class change options.");
	}
	
	/**
	 * Checks if the system is currently in validation mode.<br>
	 * This method always returns {@code false}.
	 * @return {@code true} if validating, otherwise {@code false}.
	 */
	@Override
	public boolean isValidating()
	{
		return false;
	}
	
	/**
	 * Parses the provided {@code Document} to load class change configuration data.<br>
	 * This method reads settings from an XML file and populates internal lists.<br>
	 * It checks for enabled status before processing any options.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node cm = n.getFirstChild(); cm != null; cm = cm.getNextSibling())
				{
					attrs = cm.getAttributes();
					if ("classMaster".equals(cm.getNodeName()))
					{
						_isEnabled = parseBoolean(attrs, "classChangeEnabled", false);
						if (!_isEnabled)
						{
							return;
						}
						
						_spawnClassMasters = parseBoolean(attrs, "spawnClassMasters", true);
						_showPopupWindow = parseBoolean(attrs, "showPopupWindow", false);
						for (Node c = cm.getFirstChild(); c != null; c = c.getNextSibling())
						{
							attrs = c.getAttributes();
							if ("classChangeOption".equals(c.getNodeName()))
							{
								final List<CategoryType> appliedCategories = new ArrayList<>();
								final List<ItemHolder> requiredItems = new ArrayList<>();
								final List<ItemHolder> rewardedItems = new ArrayList<>();
								boolean setNoble = false;
								boolean setHero = false;
								final String optionName = parseString(attrs, "name", "");
								for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
								{
									attrs = b.getAttributes();
									if ("appliesTo".equals(b.getNodeName()))
									{
										for (Node r = b.getFirstChild(); r != null; r = r.getNextSibling())
										{
											attrs = r.getAttributes();
											if ("category".equals(r.getNodeName()))
											{
												final CategoryType category = CategoryType.findByName(r.getTextContent().trim());
												if (category == null)
												{
													LOGGER.severe(getClass().getSimpleName() + ": Incorrect category type: " + r.getNodeValue());
													continue;
												}
												
												appliedCategories.add(category);
											}
										}
									}
									
									if ("rewards".equals(b.getNodeName()))
									{
										for (Node r = b.getFirstChild(); r != null; r = r.getNextSibling())
										{
											attrs = r.getAttributes();
											if ("item".equals(r.getNodeName()))
											{
												final int itemId = parseInteger(attrs, "id");
												final int count = parseInteger(attrs, "count", 1);
												rewardedItems.add(new ItemHolder(itemId, count));
											}
											else if ("setNoble".equals(r.getNodeName()))
											{
												setNoble = true;
											}
											else if ("setHero".equals(r.getNodeName()))
											{
												setHero = true;
											}
										}
									}
									else if ("conditions".equals(b.getNodeName()))
									{
										for (Node r = b.getFirstChild(); r != null; r = r.getNextSibling())
										{
											attrs = r.getAttributes();
											if ("item".equals(r.getNodeName()))
											{
												final int itemId = parseInteger(attrs, "id");
												final int count = parseInteger(attrs, "count", 1);
												requiredItems.add(new ItemHolder(itemId, count));
											}
										}
									}
								}
								
								if (appliedCategories.isEmpty())
								{
									LOGGER.warning(getClass().getSimpleName() + ": Class change option: " + optionName + " has no categories to be applied on. Skipping!");
									continue;
								}
								
								final ClassChangeData classChangeData = new ClassChangeData(optionName, appliedCategories);
								classChangeData.setItemsRequired(requiredItems);
								classChangeData.setItemsRewarded(rewardedItems);
								classChangeData.setRewardHero(setHero);
								classChangeData.setRewardNoblesse(setNoble);
								
								_classChangeData.add(classChangeData);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * This method is called when a spawn template is activated.<br>
	 * It checks if the {@code _spawnClassMasters} flag is enabled.<br>
	 * If true, it triggers the spawning of all relevant masters.
	 * @param template The {@code SpawnTemplate} being activated.
	 */
	@Override
	public void onSpawnActivate(SpawnTemplate template)
	{
		if (_spawnClassMasters)
		{
			template.spawnAllIncludingNotDefault(null);
		}
	}
	
	/**
	 * This method is called when a spawn is deactivated.<br>
	 * It removes all entities associated with the provided {@code SpawnTemplate}.
	 * @param template The {@code SpawnTemplate} to be processed.
	 */
	@Override
	public void onSpawnDeactivate(SpawnTemplate template)
	{
		template.despawnAll();
	}
	
	/**
	 * Handles the logic when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method returns the name of the HTML file to display during this interaction.
	 * @param npc The {@code Npc} entity that is being spoken to.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the filename of the HTML content to show.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "test_server_helper001.html";
	}
	
	/**
	 * Handles various events triggered by the Class Master NPC.<br>
	 * It processes actions like class changes, skill learning, and clan level updates.<br>
	 * The method returns an HTML string to be displayed to the player based on the event type.
	 * @param eventValue The identifier for the specific action or event to perform.
	 * @param npc The {@code Npc} instance interacting with the player.
	 * @param player The {@code Player} instance performing the action.
	 * @return A {@code String} containing HTML content, or {@code null} if the action is invalid.
	 */
	@Override
	public String onEvent(String eventValue, Npc npc, Player player)
	{
		if (!_isEnabled)
		{
			return null;
		}
		
		String htmltext = null;
		String event = eventValue;
		final StringTokenizer st = new StringTokenizer(event);
		event = st.nextToken();
		switch (event)
		{
			case "buyitems":
			{
				htmltext = npc.getId() == CLASS_MASTERS.get(0) ? "test_server_helper001a.html" : "test_server_helper001b.html";
				break;
			}
			case "setnoble":
			{
				if (player.getNobleLevel() > 0)
				{
					htmltext = "test_server_helper025b.html";
				}
				else if (player.getLevel() < 75)
				{
					htmltext = "test_server_helper025a.html";
				}
				else
				{
					player.setNobleLevel(1);
					checkNobleListener(player);
					player.broadcastUserInfo();
					
					// TODO: SetOneTimeQuestFlag(talker, 10385, 1);
					htmltext = "test_server_helper025.html";
				}
				break;
			}
			case "firstclass":
			{
				htmltext = getFirstOccupationChangeHtml(player);
				break;
			}
			case "secondclass":
			{
				htmltext = getSecondOccupationChangeHtml(player);
				break;
			}
			case "thirdclass":
			{
				if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && (player.getLevel() > 75))
				{
					if (changeToNextClass(player))
					{
						player.sendPacket(new PlaySound("ItemSound.quest_fanfare_2"));
						player.broadcastUserInfo();
						htmltext = "test_server_helper021.html";
					}
				}
				else if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
				{
					htmltext = "test_server_helper011.html";
				}
				else if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
				{
					htmltext = "test_server_helper011a.html";
				}
				else
				{
					htmltext = "test_server_helper024.html";
				}
				break;
			}
			case "awaken":
			{
				if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) && (player.getLevel() > 84))
				{
					if (changeToNextClass(player))
					{
						player.sendPacket(new PlaySound("ItemSound.quest_fanfare_2"));
						player.broadcastUserInfo();
						player.store(false); // Save player cause if server crashes before this char is saved, he will lose class and the money payed for class change.
						htmltext = "test_server_helper021.html";
					}
				}
				else if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
				{
					htmltext = "test_server_helper011a.html";
				}
				else
				{
					htmltext = "test_server_helper011b.html";
				}
				break;
			}
			case "setclass":
			{
				if (!st.hasMoreTokens())
				{
					return null;
				}
				
				final int classId = Integer.parseInt(st.nextToken());
				boolean canChange = false;
				if ((player.isInCategory(CategoryType.SECOND_CLASS_GROUP) || player.isInCategory(CategoryType.FIRST_CLASS_GROUP)) && (player.getLevel() >= 40)) // In retail you can skip first occupation.
				{
					canChange = CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, classId) || (player.isInCategory(CategoryType.FIRST_CLASS_GROUP) && CategoryData.getInstance().isInCategory(CategoryType.SECOND_CLASS_GROUP, classId));
				}
				else if (player.isInCategory(CategoryType.FIRST_CLASS_GROUP) && (player.getLevel() >= 20))
				{
					canChange = CategoryData.getInstance().isInCategory(CategoryType.SECOND_CLASS_GROUP, classId);
				}
				else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && (player.getLevel() >= 76))
				{
					canChange = CategoryData.getInstance().isInCategory(CategoryType.FOURTH_CLASS_GROUP, classId);
				}
				else if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) && (player.getLevel() >= 85)) // 9
				{
					canChange = CategoryData.getInstance().isInCategory(CategoryType.SIXTH_CLASS_GROUP, classId); // 11
				}
				
				// Validate that the requested class belongs to the player's own class tree, preventing bypass exploits.
				final PlayerClass targetClass = PlayerClass.getPlayerClass(classId);
				if (canChange && (targetClass != null) && targetClass.equalsOrChildOf(player.getPlayerClass()))
				{
					int classDataIndex = -1;
					if (st.hasMoreTokens())
					{
						classDataIndex = Integer.parseInt(st.nextToken());
					}
					
					if (checkIfClassChangeHasOptions(player) && (classDataIndex == -1))
					{
						htmltext = getHtm(player, "cc_options.html");
						htmltext = htmltext.replace("%name%", ClassListData.getInstance().getClass(classId).getClassName()); // getEscapedClientCode());
						htmltext = htmltext.replace("%options%", getClassChangeOptions(player, classId));
						return htmltext;
					}
					
					final ClassChangeData data = getClassChangeData(classDataIndex);
					if (data != null)
					{
						// Required items.
						if (!data.getItemsRequired().isEmpty())
						{
							for (ItemHolder ri : data.getItemsRequired())
							{
								if (player.getInventory().getInventoryItemCount(ri.getId(), -1) < ri.getCount())
								{
									player.sendMessage("You do not have enough items.");
									return null; // No class change if payment failed.
								}
							}
							
							for (ItemHolder ri : data.getItemsRequired())
							{
								player.destroyItemByItemId(ItemProcessType.FEE, ri.getId(), ri.getCount(), npc, true);
							}
						}
						
						// Give possible rewards.
						if (!data.getItemsRewarded().isEmpty())
						{
							for (ItemHolder ri : data.getItemsRewarded())
							{
								giveItems(player, ri);
							}
						}
						
						// Give possible nobless status reward.
						if (data.isRewardNoblesse())
						{
							player.setNobleLevel(1);
							checkNobleListener(player);
						}
						
						// Give possible hero status reward.
						if (data.isRewardHero())
						{
							player.setHero(true);
						}
					}
					
					player.setPlayerClass(classId);
					if (player.isSubClassActive())
					{
						player.getSubClasses().get(player.getClassIndex()).setPlayerClass(player.getActiveClass());
					}
					else
					{
						player.setBaseClass(player.getActiveClass());
					}
					
					if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
					{
						SkillTreeData.getInstance().cleanSkillUponChangeClass(player);
						for (SkillLearn skill : SkillTreeData.getInstance().getRaceSkillTree(player.getRace()))
						{
							player.addSkill(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()), true);
						}
					}
					
					if (PlayerConfig.AUTO_LEARN_SKILLS)
					{
						player.giveAvailableSkills(PlayerConfig.AUTO_LEARN_FS_SKILLS, PlayerConfig.AUTO_LEARN_FP_SKILLS, true, PlayerConfig.AUTO_LEARN_SKILLS_WITHOUT_ITEMS);
					}
					
					player.store(false); // Save player cause if server crashes before this char is saved, he will lose class and the money payed for class change.
					player.broadcastUserInfo();
					player.sendSkillList();
					player.sendPacket(new PlaySound("ItemSound.quest_fanfare_2"));
					return "test_server_helper021.html";
				}
				break;
			}
			case "clanlevel":
			{
				htmltext = player.isClanLeader() ? "test_server_helper022.html" : "pl014.html";
				break;
			}
			case "learnskills":
			{
				// Retail class master only lets you learn all third class skills.
				if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
				{
					htmltext = "test_server_helper001_failed.html";
				}
				else
				{
					player.giveAvailableSkills(true, true, true, true);
				}
				break;
			}
			case "clanlevelup":
			{
				final Clan clan = player.getClan();
				if ((clan == null) || !player.isClanLeader())
				{
					htmltext = "pl014.html";
					break;
				}
				
				if (clan.getLevel() >= 10)
				{
					htmltext = "test_server_helper022a.html";
				}
				else
				{
					clan.changeLevel(clan.getLevel() + 1);
					clan.broadcastClanStatus();
					htmltext = "test_server_helper022c.html";
				}
				break;
			}
			case "test_server_helper001.html":
			{
				if (CLASS_MASTERS.contains(npc.getId()))
				{
					htmltext = event;
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Determines the correct HTML file path for a player's occupation change.<br>
	 * This method checks the {@code Player} category, race, level, and class type.<br>
	 * It returns the filename of the helper guide based on these attributes.
	 * @param player The {@code Player} object to check for occupation data.
	 * @return A {@code String} containing the name of the HTML file or {@code null}.
	 */
	private String getFirstOccupationChangeHtml(Player player)
	{
		String htmltext = null;
		if (player.isInCategory(CategoryType.FIRST_CLASS_GROUP))
		{
			if (player.getRace() == Race.ERTHEIA)
			{
				htmltext = "test_server_helper027a.html";
			}
			else if (player.getLevel() < 20)
			{
				htmltext = "test_server_helper027.html";
			}
			else
			{
				switch (player.getPlayerClass())
				{
					case FIGHTER:
					{
						htmltext = "test_server_helper026a.html";
						break;
					}
					case MAGE:
					{
						htmltext = "test_server_helper026b.html";
						break;
					}
					case ELVEN_FIGHTER:
					{
						htmltext = "test_server_helper026c.html";
						break;
					}
					case ELVEN_MAGE:
					{
						htmltext = "test_server_helper026d.html";
						break;
					}
					case DARK_FIGHTER:
					{
						htmltext = "test_server_helper026e.html";
						break;
					}
					case DARK_MAGE:
					{
						htmltext = "test_server_helper026f.html";
						break;
					}
					case ORC_FIGHTER:
					{
						htmltext = "test_server_helper026g.html";
						break;
					}
					case ORC_MAGE:
					{
						htmltext = "test_server_helper026h.html";
						break;
					}
					case DWARVEN_FIGHTER:
					{
						htmltext = "test_server_helper026i.html";
						break;
					}
					case MALE_SOLDIER:
					{
						htmltext = "test_server_helper026j.html";
						break;
					}
					case FEMALE_SOLDIER:
					{
						htmltext = "test_server_helper026k.html";
						break;
					}
					case DEATH_SOLDIER:
					{
						htmltext = "test_server_helper026l.html";
						break;
					}
					case DWARF_MAKER:
					{
						htmltext = "test_server_helper026m.html";
						break;
					}
					case WARG_0:
					{
						htmltext = "test_server_helper026n.html";
						break;
					}
				}
			}
		}
		else if (player.isInCategory(CategoryType.SECOND_CLASS_GROUP))
		{
			htmltext = "test_server_helper028.html";
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
		{
			htmltext = "test_server_helper010.html";
		}
		else if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			htmltext = "test_server_helper011.html";
		}
		else if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
		{
			htmltext = "test_server_helper011a.html";
		}
		
		return htmltext;
	}
	
	/**
	 * Determines the appropriate HTML file for a player's second occupation change.<br>
	 * It checks the {@code Player} category and level to select the correct helper file.<br>
	 * The method returns specific filenames based on the current {@code PlayerClass}.
	 * @param player The {@code Player} object used to check class and level requirements.
	 * @return A {@code String} containing the name of the HTML file to be displayed.
	 */
	private String getSecondOccupationChangeHtml(Player player)
	{
		String htmltext = null;
		if (player.isInCategory(CategoryType.SECOND_CLASS_GROUP) || player.isInCategory(CategoryType.FIRST_CLASS_GROUP))
		{
			if (player.getLevel() < 40)
			{
				htmltext = "test_server_helper023.html";
			}
			else
			{
				switch (player.getPlayerClass())
				{
					case FIGHTER:
					{
						htmltext = "test_server_helper012.html";
						break;
					}
					case WARRIOR:
					{
						htmltext = "test_server_helper012a.html";
						break;
					}
					case KNIGHT:
					{
						htmltext = "test_server_helper012b.html";
						break;
					}
					case ROGUE:
					{
						htmltext = "test_server_helper012c.html";
						break;
					}
					case DEATH_SOLDIER:
					{
						htmltext = "test_server_helper012d.html";
						break;
					}
					case DEATH_WARRIOR:
					{
						htmltext = "test_server_helper012e.html";
						break;
					}
					case MAGE:
					{
						htmltext = "test_server_helper013.html";
						break;
					}
					case WIZARD:
					{
						htmltext = "test_server_helper013a.html";
						break;
					}
					case CLERIC:
					{
						htmltext = "test_server_helper013b.html";
						break;
					}
					case ELVEN_FIGHTER:
					{
						htmltext = "test_server_helper014.html";
						break;
					}
					case ELVEN_KNIGHT:
					{
						htmltext = "test_server_helper014a.html";
						break;
					}
					case ELVEN_SCOUT:
					{
						htmltext = "test_server_helper014b.html";
						break;
					}
					case ELVEN_MAGE:
					{
						htmltext = "test_server_helper015.html";
						break;
					}
					case ELVEN_WIZARD:
					{
						htmltext = "test_server_helper015a.html";
						break;
					}
					case ORACLE:
					{
						htmltext = "test_server_helper015b.html";
						break;
					}
					case DARK_FIGHTER:
					{
						htmltext = "test_server_helper016.html";
						break;
					}
					case PALUS_KNIGHT:
					{
						htmltext = "test_server_helper016a.html";
						break;
					}
					case ASSASSIN:
					{
						htmltext = "test_server_helper016b.html";
						break;
					}
					case DARK_MAGE:
					{
						htmltext = "test_server_helper017.html";
						break;
					}
					case DARK_WIZARD:
					{
						htmltext = "test_server_helper017a.html";
						break;
					}
					case SHILLIEN_ORACLE:
					{
						htmltext = "test_server_helper017b.html";
						break;
					}
					case ORC_FIGHTER:
					{
						htmltext = "test_server_helper018.html";
						break;
					}
					case ORC_RAIDER:
					{
						htmltext = "test_server_helper018a.html";
						break;
					}
					case ORC_MONK:
					{
						htmltext = "test_server_helper018b.html";
						break;
					}
					case ORC_MAGE:
					case ORC_SHAMAN:
					{
						htmltext = "test_server_helper019.html";
						break;
					}
					case DWARVEN_FIGHTER:
					{
						htmltext = "test_server_helper020.html";
						break;
					}
					case ARTISAN:
					{
						htmltext = "test_server_helper020b.html";
						break;
					}
					case SCAVENGER:
					{
						htmltext = "test_server_helper020a.html";
						break;
					}
					case MALE_SOLDIER:
					case TROOPER:
					{
						htmltext = "test_server_helper020c.html";
						break;
					}
					case FEMALE_SOLDIER:
					case WARDER:
					{
						htmltext = "test_server_helper020d.html";
						break;
					}
					case ERTHEIA_FIGHTER:
					{
						htmltext = "test_server_helper020e.html";
						break;
					}
					case ERTHEIA_WIZARD:
					{
						htmltext = "test_server_helper020f.html";
						break;
					}
					case EARTH_MAKER:
					{
						htmltext = "test_server_helper020g.html";
						break;
					}
					case WARG_1:
					{
						htmltext = "test_server_helper020h.html";
						break;
					}
				}
			}
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
		{
			htmltext = "test_server_helper010.html";
		}
		else if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			htmltext = "test_server_helper011.html";
		}
		else if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
		{
			htmltext = "test_server_helper011a.html";
		}
		else
		{
			htmltext = "test_server_helper029.html";
		}
		
		return htmltext;
	}
	
	/**
	 * Attempts to transition a player to their next available class.<br>
	 * This method checks requirements, handles item payments, and updates the player's class data.<br>
	 * It returns {@code true} if the change was successful or {@code false} otherwise.
	 * @param player The {@code Player} object to perform the class change on.
	 * @return {@code true} if the class change succeeded, {@code false} if it failed.
	 */
	private boolean changeToNextClass(Player player)
	{
		PlayerClass newClass = null;
		final PlayerClass playerClass = player.getPlayerClass();
		if (playerClass == PlayerClass.FEMALE_SOUL_HOUND)
		{
			newClass = PlayerClass.FEOH_SOUL_HOUND; // Fix for Female Soulhounds
		}
		else
		{
			newClass = Arrays.stream(PlayerClass.values()).filter(cid -> playerClass == cid.getParent()).findAny().orElse(null);
		}
		
		if (newClass == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": No new classId found for " + player);
			return false;
		}
		else if (newClass == playerClass)
		{
			LOGGER.warning(getClass().getSimpleName() + ": New classId found for " + player + " is exactly the same as the one he currently is!");
			return false;
		}
		else if (checkIfClassChangeHasOptions(player))
		{
			String html = getHtm(player, "cc_options.html");
			html = html.replace("%name%", ClassListData.getInstance().getClass(newClass.getId()).getClassName()); // getEscapedClientCode());
			html = html.replace("%options%", getClassChangeOptions(player, newClass.getId()));
			showResult(player, html);
			return false;
		}
		else
		{
			ClassChangeData data = null;
			for (ClassChangeData ccd : _classChangeData)
			{
				if (ccd.isInCategory(player))
				{
					data = ccd;
					break;
				}
			}
			
			if (data != null)
			{
				// Required items.
				if (!data.getItemsRequired().isEmpty())
				{
					for (ItemHolder ri : data.getItemsRequired())
					{
						if (player.getInventory().getInventoryItemCount(ri.getId(), -1) < ri.getCount())
						{
							player.sendMessage("You do not have enough items.");
							return false; // No class change if payment failed.
						}
					}
					
					for (ItemHolder ri : data.getItemsRequired())
					{
						player.destroyItemByItemId(ItemProcessType.FEE, ri.getId(), ri.getCount(), player, true);
					}
				}
				
				// Give possible rewards.
				if (!data.getItemsRewarded().isEmpty())
				{
					for (ItemHolder ri : data.getItemsRewarded())
					{
						giveItems(player, ri);
					}
				}
				
				// Give possible nobless status reward.
				if (data.isRewardNoblesse())
				{
					player.setNobleLevel(1);
					checkNobleListener(player);
				}
				
				// Give possible hero status reward.
				if (data.isRewardHero())
				{
					player.setHero(true);
				}
			}
			
			player.setPlayerClass(newClass.getId());
			if (player.isSubClassActive())
			{
				player.getSubClasses().get(player.getClassIndex()).setPlayerClass(player.getActiveClass());
			}
			else
			{
				player.setBaseClass(player.getActiveClass());
			}
			
			if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
			{
				SkillTreeData.getInstance().cleanSkillUponChangeClass(player);
				for (SkillLearn skill : SkillTreeData.getInstance().getRaceSkillTree(player.getRace()))
				{
					player.addSkill(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()), true);
				}
			}
			
			if (PlayerConfig.AUTO_LEARN_SKILLS)
			{
				player.giveAvailableSkills(PlayerConfig.AUTO_LEARN_FS_SKILLS, PlayerConfig.AUTO_LEARN_FP_SKILLS, true, PlayerConfig.AUTO_LEARN_SKILLS_WITHOUT_ITEMS);
			}
			
			player.store(false); // Save player cause if server crashes before this char is saved, he will lose class and the money payed for class change.
			player.broadcastUserInfo();
			player.sendSkillList();
			return true;
		}
	}
	
	/**
	 * Displays a tutorial popup to the player.<br>
	 * This method checks if the player meets specific level and category requirements.<br>
	 * If the conditions are met, it sends a {@code TutorialShowQuestionMark} packet.
	 * @param player The {@code Player} who will receive the popup.
	 */
	private void showPopupWindow(Player player)
	{
		if (!_showPopupWindow)
		{
			return;
		}
		
		// @formatter:off
		if ((player.isInCategory(CategoryType.FIRST_CLASS_GROUP) && (player.getLevel() >= 20)) ||
			((player.isInCategory(CategoryType.SECOND_CLASS_GROUP) || player.isInCategory(CategoryType.FIRST_CLASS_GROUP)) && (player.getLevel() >= 40)) ||
			(player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && (player.getLevel() >= 76)) ||
			(player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) && (player.getLevel() >= 85)))
		// @formatter:on
		{
			player.sendPacket(new TutorialShowQuestionMark(102, 0));
		}
	}
	
	/**
	 * Handles the logic when a player clicks on a specific tutorial mark.<br>
	 * It checks if the player meets the level and category requirements to show class change information.<br>
	 * If valid, it displays the appropriate HTML content to the player.
	 * @param event The {@code OnPlayerPressTutorialMark} event containing the player and mark ID.
	 */
	@RegisterEvent(EventType.ON_PLAYER_PRESS_TUTORIAL_MARK)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerPressTutorialMark(OnPlayerPressTutorialMark event)
	{
		if (!_showPopupWindow || (event.getMarkId() != 102))
		{
			return;
		}
		
		String html = null;
		final Player player = event.getPlayer();
		if ((player.isInCategory(CategoryType.SECOND_CLASS_GROUP) || player.isInCategory(CategoryType.FIRST_CLASS_GROUP)) && (player.getLevel() >= 40)) // In retail you can skip first occupation.
		{
			html = getHtm(player, getSecondOccupationChangeHtml(player));
		}
		else if (player.isInCategory(CategoryType.FIRST_CLASS_GROUP) && (player.getLevel() >= 20))
		{
			html = getHtm(player, getFirstOccupationChangeHtml(player));
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && (player.getLevel() >= 76))
		{
			html = getHtm(player, "qm_thirdclass.html");
		}
		else if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) && (player.getLevel() >= 85)) // 9
		{
			html = getHtm(player, "qm_awaken.html");
		}
		
		if (html != null)
		{
			showResult(player, html);
			// player.sendPacket(new TutorialShowHtml(html));
		}
	}
	
	/**
	 * Handles the logic when a player executes a bypass command.<br>
	 * This method checks if the command starts with {@code Script ClassMaster }.<br>
	 * If it matches, it processes the event and sends a result to the player.
	 * @param event The {@code OnPlayerBypass} event containing the player and their command.
	 */
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerBypass(OnPlayerBypass event)
	{
		if (event.getCommand().startsWith("Script ClassMaster "))
		{
			final String html = onEvent(event.getCommand().substring(18), null, event.getPlayer());
			event.getPlayer().sendPacket(TutorialCloseHtml.STATIC_PACKET);
			showResult(event.getPlayer(), html);
		}
	}
	
	/**
	 * Handles the logic when a player changes their profession.<br>
	 * This method triggers a popup window for the affected player.
	 * @param event The {@code OnPlayerProfessionChange} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_PROFESSION_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerProfessionChange(OnPlayerProfessionChange event)
	{
		showPopupWindow(event.getPlayer());
	}
	
	/**
	 * This method is called when a player's level changes.<br>
	 * It triggers the display of a popup window for the affected player.
	 * @param event The {@code OnPlayerLevelChanged} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LEVEL_CHANGED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLevelChanged(OnPlayerLevelChanged event)
	{
		showPopupWindow(event.getPlayer());
	}
	
	/**
	 * Handles the logic when a player logs into the game.<br>
	 * This method triggers a popup window for the player.
	 * @param event The {@code OnPlayerLogin} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		showPopupWindow(event.getPlayer());
	}
	
	/**
	 * Generates an HTML string representing the available class change options.<br>
	 * It filters options based on the player's current categories.<br>
	 * The output includes requirements and rewards for each valid option.
	 * @param player The {@code Player} object to check for category eligibility.
	 * @param selectedClassId The {@code int} ID of the class currently being viewed.
	 * @return A formatted HTML string containing the list of options.
	 */
	private String getClassChangeOptions(Player player, int selectedClassId)
	{
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < _classChangeData.size(); i++)
		{
			final ClassChangeData option = getClassChangeData(i);
			if ((option == null) || option.getCategories().stream().noneMatch(player::isInCategory))
			{
				continue;
			}
			
			sb.append("<tr><td><img src=L2UI_CT1.ChatBalloon_DF_TopCenter width=276 height=1 /></td></tr>");
			sb.append("<tr><td><table bgcolor=3f3f3f width=100%>");
			sb.append("<tr><td align=center><a action=\"bypass -h Script ClassMaster setclass " + selectedClassId + " " + i + "\">" + option.getName() + ":</a></td></tr>");
			sb.append("<tr><td><table width=276>");
			sb.append("<tr><td>Requirements:</td></tr>");
			if (option.getItemsRequired().isEmpty())
			{
				sb.append("<tr><td><font color=LEVEL>Free</font></td></tr>");
			}
			else
			{
				option.getItemsRequired().forEach(ih -> sb.append("<tr><td><font color=\"LEVEL\">" + ih.getCount() + "</font></td><td>" + ItemData.getInstance().getTemplate(ih.getId()).getName() + "</td><td width=30></td></tr>"));
			}
			sb.append("<tr><td>Rewards:</td></tr>");
			if (option.getItemsRewarded().isEmpty())
			{
				if (option.isRewardNoblesse())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Noblesse status.</font></td></tr>");
				}
				
				if (option.isRewardHero())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Hero status.</font></td></tr>");
				}
				
				if (!option.isRewardNoblesse() && !option.isRewardHero())
				{
					sb.append("<tr><td><font color=LEVEL>none</font></td></tr>");
				}
			}
			else
			{
				option.getItemsRewarded().forEach(ih -> sb.append("<tr><td><font color=\"LEVEL\">" + ih.getCount() + "</font></td><td>" + ItemData.getInstance().getTemplate(ih.getId()).getName() + "</td><td width=30></td></tr>"));
				if (option.isRewardNoblesse())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Noblesse status.</font></td></tr>");
				}
				
				if (option.isRewardHero())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Hero status.</font></td></tr>");
				}
			}
			
			sb.append("</table></td></tr>");
			sb.append("</table></td></tr>");
			sb.append("<tr><td><img src=L2UI_CT1.ChatBalloon_DF_TopCenter width=276 height=1 /></td></tr>");
		}
		
		return sb.toString();
	}
	
	private static class ClassChangeData
	{
		private final String _name;
		private final List<CategoryType> _appliedCategories;
		private boolean _rewardNoblesse;
		private boolean _rewardHero;
		private List<ItemHolder> _itemsRequired;
		private List<ItemHolder> _itemsRewarded;
		
		public ClassChangeData(String name, List<CategoryType> appliedCategories)
		{
			_name = name;
			_appliedCategories = appliedCategories != null ? appliedCategories : Collections.emptyList();
		}
		
		public String getName()
		{
			return _name;
		}
		
		public List<CategoryType> getCategories()
		{
			return _appliedCategories != null ? _appliedCategories : Collections.emptyList();
		}
		
		public boolean isInCategory(Player player)
		{
			if (_appliedCategories != null)
			{
				for (CategoryType category : _appliedCategories)
				{
					if (player.isInCategory(category))
					{
						return true;
					}
				}
			}
			
			return false;
		}
		
		public boolean isRewardNoblesse()
		{
			return _rewardNoblesse;
		}
		
		public void setRewardNoblesse(boolean rewardNoblesse)
		{
			_rewardNoblesse = rewardNoblesse;
		}
		
		public boolean isRewardHero()
		{
			return _rewardHero;
		}
		
		public void setRewardHero(boolean rewardHero)
		{
			_rewardHero = rewardHero;
		}
		
		void setItemsRequired(List<ItemHolder> itemsRequired)
		{
			_itemsRequired = itemsRequired;
		}
		
		public List<ItemHolder> getItemsRequired()
		{
			return _itemsRequired != null ? _itemsRequired : Collections.emptyList();
		}
		
		void setItemsRewarded(List<ItemHolder> itemsRewarded)
		{
			_itemsRewarded = itemsRewarded;
		}
		
		public List<ItemHolder> getItemsRewarded()
		{
			return _itemsRewarded != null ? _itemsRewarded : Collections.emptyList();
		}
	}
	
	/**
	 * Determines if the player should see a selection menu for class changes.<br>
	 * This happens if there are specific item requirements or multiple rewards available.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if options should be shown, {@code false} otherwise.
	 */
	private boolean checkIfClassChangeHasOptions(Player player)
	{
		boolean showOptions = false;
		
		// Check if there are requirements.
		for (ClassChangeData ccd : _classChangeData)
		{
			if (!ccd.getItemsRequired().isEmpty() && ccd.isInCategory(player))
			{
				showOptions = true;
				break;
			}
		}
		
		if (!showOptions)
		{
			// Check if there is more than 1 reward to chose.
			int count = 0;
			for (ClassChangeData ccd : _classChangeData)
			{
				if (!ccd.getItemsRewarded().isEmpty() && ccd.isInCategory(player))
				{
					count++;
				}
			}
			
			if (count > 1)
			{
				showOptions = true;
			}
		}
		
		return showOptions;
	}
	
	/**
	 * Retrieves the class change data for a specific position.<br>
	 * It checks if the provided {@code index} is within the valid range of the list.
	 * @param index The position of the data to retrieve.
	 * @return The {@code ClassChangeData} at the specified index, or {@code null} if it is out of bounds.
	 */
	private ClassChangeData getClassChangeData(int index)
	{
		if ((index >= 0) && (index < _classChangeData.size()))
		{
			return _classChangeData.get(index);
		}
		
		return null;
	}
	
	/**
	 * Checks if there is a listener for the player becoming a noblesse.<br>
	 * If a listener exists, it notifies the {@code EventDispatcher} asynchronously.
	 * @param player The {@code Player} object to check.
	 */
	private void checkNobleListener(Player player)
	{
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_BECOME_NOBLESSE))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerBecomeNoblesse(player));
		}
	}
	
	/**
	 * This is the main entry point for the {@code ClassMaster} application.<br>
	 * It initializes a new instance of the {@code ClassMaster} class.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new ClassMaster();
	}
}
