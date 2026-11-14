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
package org.l2jmobius.gameserver.entity.actor;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.ai.PlayerAI;
import org.l2jmobius.gameserver.ai.SummonAI;
import org.l2jmobius.gameserver.cache.RelationCache;
import org.l2jmobius.gameserver.communitybbs.BB.Forum;
import org.l2jmobius.gameserver.communitybbs.Manager.ForumsBBSManager;
import org.l2jmobius.gameserver.config.CeremonyOfChaosConfig;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.HuntPassConfig;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.PvpConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.config.custom.FreeMountsConfig;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.config.custom.PrivateStoreRangeConfig;
import org.l2jmobius.gameserver.config.custom.PvpAnnounceConfig;
import org.l2jmobius.gameserver.config.custom.PvpRewardItemConfig;
import org.l2jmobius.gameserver.config.custom.PvpTitleColorConfig;
import org.l2jmobius.gameserver.config.custom.RebirthConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.holders.AccessLevel;
import org.l2jmobius.gameserver.data.holders.PetDataHolder;
import org.l2jmobius.gameserver.data.holders.PetLevelData;
import org.l2jmobius.gameserver.data.holders.PreparedMultisellListHolder;
import org.l2jmobius.gameserver.data.holders.SellBuffHolder;
import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.data.holders.TrainingHolder;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.sql.OfflinePlayTable;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.data.sql.SummonEffectTable;
import org.l2jmobius.gameserver.data.sql.SummonInfoTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.ExperienceLossData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.data.xml.NewQuestData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.NpcNameLocalisationData;
import org.l2jmobius.gameserver.data.xml.PetData;
import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.data.xml.SendMessageLocalisationData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.data.xml.SymbolSealData;
import org.l2jmobius.gameserver.data.xml.TimedHuntingZoneData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.enums.player.AdminTeleportType;
import org.l2jmobius.gameserver.entity.actor.enums.player.BonusExpType;
import org.l2jmobius.gameserver.entity.actor.enums.player.GroupType;
import org.l2jmobius.gameserver.entity.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerAction;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.actor.enums.player.Sex;
import org.l2jmobius.gameserver.entity.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.entity.actor.enums.player.SubclassInfoType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.entity.actor.holders.creature.TimeStamp;
import org.l2jmobius.gameserver.entity.actor.holders.player.ArenaParticipantsHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.Attendance;
import org.l2jmobius.gameserver.entity.actor.holders.player.AutoPlaySettingsHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.AutoUseSettingsHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.entity.actor.holders.player.ChallengePoint;
import org.l2jmobius.gameserver.entity.actor.holders.player.ClientSettings;
import org.l2jmobius.gameserver.entity.actor.holders.player.CollectionList;
import org.l2jmobius.gameserver.entity.actor.holders.player.ContactList;
import org.l2jmobius.gameserver.entity.actor.holders.player.CrossEventProgress;
import org.l2jmobius.gameserver.entity.actor.holders.player.CubicList;
import org.l2jmobius.gameserver.entity.actor.holders.player.CursedWeapon;
import org.l2jmobius.gameserver.entity.actor.holders.player.DamageTakenHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.DeathPenalty;
import org.l2jmobius.gameserver.entity.actor.holders.player.Duel;
import org.l2jmobius.gameserver.entity.actor.holders.player.Friends;
import org.l2jmobius.gameserver.entity.actor.holders.player.HennaList;
import org.l2jmobius.gameserver.entity.actor.holders.player.HuntPass;
import org.l2jmobius.gameserver.entity.actor.holders.player.Macro;
import org.l2jmobius.gameserver.entity.actor.holders.player.MacroList;
import org.l2jmobius.gameserver.entity.actor.holders.player.MovieHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.ObserverMode;
import org.l2jmobius.gameserver.entity.actor.holders.player.OfflineMode;
import org.l2jmobius.gameserver.entity.actor.holders.player.PremiumItemList;
import org.l2jmobius.gameserver.entity.actor.holders.player.Radar;
import org.l2jmobius.gameserver.entity.actor.holders.player.RankingHistory;
import org.l2jmobius.gameserver.entity.actor.holders.player.RankingHistoryDataHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.RecipeBook;
import org.l2jmobius.gameserver.entity.actor.holders.player.Recommendations;
import org.l2jmobius.gameserver.entity.actor.holders.player.RelicList;
import org.l2jmobius.gameserver.entity.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.entity.actor.holders.player.Shortcuts;
import org.l2jmobius.gameserver.entity.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.TeleportBookmarkList;
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;
import org.l2jmobius.gameserver.entity.actor.instance.Boat;
import org.l2jmobius.gameserver.entity.actor.instance.ControlTower;
import org.l2jmobius.gameserver.entity.actor.instance.Defender;
import org.l2jmobius.gameserver.entity.actor.instance.Doppelganger;
import org.l2jmobius.gameserver.entity.actor.instance.FriendlyMob;
import org.l2jmobius.gameserver.entity.actor.instance.Guard;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.instance.Shuttle;
import org.l2jmobius.gameserver.entity.actor.instance.TamedBeast;
import org.l2jmobius.gameserver.entity.actor.instance.Trap;
import org.l2jmobius.gameserver.entity.actor.request.AbstractRequest;
import org.l2jmobius.gameserver.entity.actor.request.AutoPeelRequest;
import org.l2jmobius.gameserver.entity.actor.request.SayuneRequest;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.actor.status.PlayerStatus;
import org.l2jmobius.gameserver.entity.actor.tasks.player.DismountTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.FameTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.InventoryEnableTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.PetFeedTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.RentPetTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.ResetChargesTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.ResetSoulsTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.SitDownTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.StandUpTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.TeleportWatchdogTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.WarnUserTakeBreakTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.WaterTask;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.clan.ClanPrivileges;
import org.l2jmobius.gameserver.entity.clan.ClanWar;
import org.l2jmobius.gameserver.entity.clan.enums.ClanWarState;
import org.l2jmobius.gameserver.entity.cubic.Cubic;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.entity.groups.PartyMessageType;
import org.l2jmobius.gameserver.entity.groups.matching.MatchingRoom;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.BroochJewel;
import org.l2jmobius.gameserver.entity.item.enums.ItemGrade;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.ActionType;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerFreight;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerRefund;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerWarehouse;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneRegion;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.WaterZone;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.CoupleManager;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.HandysBlockCheckerManager;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.managers.MatchingRoomManager;
import org.l2jmobius.gameserver.managers.MentorManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.managers.RebirthManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.managers.SellBuffsManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.managers.games.AerialCleftManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayableExpChanged;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerAbilityPointsChanged;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerFameChanged;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerItemEquip;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerMenteeStatus;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerMentorStatus;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerPKChanged;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerProfessionCancel;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerProfessionChange;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerPvPChanged;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerPvPKill;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerReputationChanged;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSubChange;
import org.l2jmobius.gameserver.mechanics.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.fishing.Fishing;
import org.l2jmobius.gameserver.mechanics.herobook.HeroBookInfoHolder;
import org.l2jmobius.gameserver.mechanics.herobook.HeroBookManager;
import org.l2jmobius.gameserver.mechanics.homunculus.HomunculusList;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateiArena;
import org.l2jmobius.gameserver.mechanics.olympiad.Hero;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadFightHistory;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameManager;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;
import org.l2jmobius.gameserver.mechanics.script.Faction;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestDialogType;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.QuestTimer;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.NewQuest;
import org.l2jmobius.gameserver.mechanics.script.timers.TimerHolder;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.CastleSide;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.mechanics.skill.AmmunitionSkillList;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.MountEnabledSkillList;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.SkillCastingType;
import org.l2jmobius.gameserver.mechanics.skill.enums.NextActionType;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillUseHolder;
import org.l2jmobius.gameserver.mechanics.skill.replacement.SkillReplacementList;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.MoveType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCTeam;
import org.l2jmobius.gameserver.mechanics.variables.AccountVariables;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;
import org.l2jmobius.gameserver.network.enums.PartySmallWindowUpdateType;
import org.l2jmobius.gameserver.network.enums.StatusUpdateType;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.holders.ClientHardwareInfoHolder;
import org.l2jmobius.gameserver.network.holders.RequestPartner;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.AbstractHtmlPacket;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillList;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ChairSit;
import org.l2jmobius.gameserver.network.serverpackets.ChangeWaitType;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExAbnormalStatusUpdateFromTarget;
import org.l2jmobius.gameserver.network.serverpackets.ExAdenaInvenCount;
import org.l2jmobius.gameserver.network.serverpackets.ExAlterSkillRequest;
import org.l2jmobius.gameserver.network.serverpackets.ExAutoSoulShot;
import org.l2jmobius.gameserver.network.serverpackets.ExBrPremiumState;
import org.l2jmobius.gameserver.network.serverpackets.ExCharInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExDamagePopUp;
import org.l2jmobius.gameserver.network.serverpackets.ExDieInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExGetOnAirShip;
import org.l2jmobius.gameserver.network.serverpackets.ExMagicAttackInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExPledgeCoinInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExPledgeCount;
import org.l2jmobius.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;
import org.l2jmobius.gameserver.network.serverpackets.ExQuestItemList;
import org.l2jmobius.gameserver.network.serverpackets.ExSetCompassZoneCode;
import org.l2jmobius.gameserver.network.serverpackets.ExStartScenePlayer;
import org.l2jmobius.gameserver.network.serverpackets.ExStopScenePlayer;
import org.l2jmobius.gameserver.network.serverpackets.ExStorageMaxCount;
import org.l2jmobius.gameserver.network.serverpackets.ExSubjobInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExUseSharedGroupItem;
import org.l2jmobius.gameserver.network.serverpackets.ExUserBoostStat;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoAbnormalVisualEffect;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoAddMDefend;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoAddPDefend;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoEquipSlot;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoInvenWeight;
import org.l2jmobius.gameserver.network.serverpackets.ExUserViewInfoParameter;
import org.l2jmobius.gameserver.network.serverpackets.ExVitalityEffectInfo;
import org.l2jmobius.gameserver.network.serverpackets.GetOnVehicle;
import org.l2jmobius.gameserver.network.serverpackets.HennaInfo;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;
import org.l2jmobius.gameserver.network.serverpackets.NicknameChanged;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreListBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreListSell;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreManageListBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgSell;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopMsg;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopSellList;
import org.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import org.l2jmobius.gameserver.network.serverpackets.Ride;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutInit;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutRegister;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.network.serverpackets.SkillList;
import org.l2jmobius.gameserver.network.serverpackets.Snoop;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.TargetSelected;
import org.l2jmobius.gameserver.network.serverpackets.TargetUnselected;
import org.l2jmobius.gameserver.network.serverpackets.TradeDone;
import org.l2jmobius.gameserver.network.serverpackets.TradeOtherDone;
import org.l2jmobius.gameserver.network.serverpackets.TradeStart;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.network.serverpackets.ability.ExAcquireAPSkillList;
import org.l2jmobius.gameserver.network.serverpackets.autopeel.ExReadyItemAutoPeel;
import org.l2jmobius.gameserver.network.serverpackets.autopeel.ExStopItemAutoPeel;
import org.l2jmobius.gameserver.network.serverpackets.autoplay.ExActivateAutoShortcut;
import org.l2jmobius.gameserver.network.serverpackets.autoplay.ExAutoPlaySettingSend;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExResponseCommissionInfo;
import org.l2jmobius.gameserver.network.serverpackets.friend.FriendStatus;
import org.l2jmobius.gameserver.network.serverpackets.huntingzones.TimeRestrictFieldDieLimitTime;
import org.l2jmobius.gameserver.network.serverpackets.huntingzones.TimedHuntingZoneExit;
import org.l2jmobius.gameserver.network.serverpackets.limitshop.ExBloodyCoinCount;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestDialog;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestNotificationAll;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsCollectionInfo;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsExchangeList;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsList;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.taskmanagers.ItemsAutoDestroyTaskManager;
import org.l2jmobius.gameserver.taskmanagers.PlayerAutoSaveTaskManager;
import org.l2jmobius.gameserver.taskmanagers.PvpFlagTaskManager;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class represents all player characters in the world.<br>
 * There is always a client-thread connected to this (except if a player-store is activated upon logout).
 */
public class Player extends Playable
{
	// Character Skill SQL String Definitions:
	private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level,skill_sub_level FROM character_skills WHERE charId=? AND class_index=?";
	private static final String UPDATE_CHARACTER_SKILL_LEVEL = "UPDATE character_skills SET skill_level=?, skill_sub_level=?  WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String ADD_NEW_SKILLS = "REPLACE INTO character_skills (charId,skill_id,skill_level,skill_sub_level,class_index) VALUES (?,?,?,?,?)";
	private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE charId=? AND class_index=?";
	
	// Character Skill Save SQL String Definitions:
	private static final String ADD_SKILL_SAVE = "REPLACE INTO character_skills_save (charId,skill_id,skill_level,skill_sub_level,remaining_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,skill_sub_level,remaining_time, reuse_delay, systime, restore_type FROM character_skills_save WHERE charId=? AND class_index=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE charId=? AND class_index=?";
	
	// Character Item Reuse Time String Definition:
	private static final String ADD_ITEM_REUSE_SAVE = "INSERT INTO character_item_reuse_save (charId,itemId,itemObjId,reuseDelay,systime) VALUES (?,?,?,?,?)";
	private static final String RESTORE_ITEM_REUSE_SAVE = "SELECT charId,itemId,itemObjId,reuseDelay,systime FROM character_item_reuse_save WHERE charId=?";
	private static final String DELETE_ITEM_REUSE_SAVE = "DELETE FROM character_item_reuse_save WHERE charId=?";
	
	// Character Character SQL String Definitions:
	private static final String INSERT_CHARACTER = "INSERT INTO characters (account_name,charId,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,reputation,fame,raidbossPoints,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,title_color,online,clan_privs,wantspeace,base_class,nobless,power_grade,vitality_points,createDate,lastAccess,kills,deaths) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,reputation=?,fame=?,raidbossPoints=?,pvpkills=?,pkkills=?,clanid=?,race=?,classid=?,deletetime=?,title=?,title_color=?,online=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,nobless=?,power_grade=?,subpledge=?,lvl_joined_academy=?,apprentice=?,sponsor=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,bookmarkslot=?,vitality_points=?,language=?,faction=?,pccafe_points=?,kills=?,deaths=? WHERE charId=?";
	private static final String UPDATE_CHARACTER_ACCESS = "UPDATE characters SET accesslevel = ? WHERE charId = ?";
	private static final String RESTORE_CHARACTER = "SELECT * FROM characters WHERE charId=?";
	
	// Character Subclass SQL String Definitions:
	private static final String RESTORE_CHAR_SUBCLASSES = "SELECT class_id,exp,sp,level,vitality_points,class_index,dual_class FROM character_subclasses WHERE charId=? ORDER BY class_index ASC";
	private static final String ADD_CHAR_SUBCLASS = "INSERT INTO character_subclasses (charId,class_id,exp,sp,level,vitality_points,class_index,dual_class) VALUES (?,?,?,?,?,?,?,?)";
	private static final String UPDATE_CHAR_SUBCLASS = "UPDATE character_subclasses SET exp=?,sp=?,level=?,vitality_points=?,class_id=?,dual_class=? WHERE charId=? AND class_index =?";
	private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE charId=? AND class_index=?";
	
	// Character Henna SQL String Definitions:
	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?";
	
	// Character Shortcut SQL String Definitions:
	private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE charId=? AND class_index=?";
	
	public static final String NEWBIE_KEY = "NEWBIE";
	
	public static final int ID_NONE = -1;
	
	public static final int REQUEST_TIMEOUT = 15;
	
	private int _pcCafePoints = 0;
	
	private GameClient _client;
	private String _ip = "N/A";
	
	private String _conquestName;
	private final String _accountName;
	private long _deleteTimer;
	private Calendar _createDate = Calendar.getInstance();
	
	private String _lang = null;
	private String _htmlPrefix = "";
	
	private volatile boolean _isOnline = false;
	private final OfflineMode _offlineMode = new OfflineMode(this);
	private boolean _enteredWorld = false;
	private long _onlineTime;
	private long _onlineBeginTime;
	private long _lastAccess;
	private long _uptime;
	
	private final InventoryUpdate _inventoryUpdate = new InventoryUpdate();
	private ScheduledFuture<?> _inventoryUpdateTask;
	private ScheduledFuture<?> _itemListTask;
	private ScheduledFuture<?> _adenaAndWeightTask;
	private ScheduledFuture<?> _skillListTask;
	private ScheduledFuture<?> _storageCountTask;
	private ScheduledFuture<?> _userBoostStatTask;
	private ScheduledFuture<?> _abnormalVisualEffectTask;
	private ScheduledFuture<?> _effectUserInfoUpdateTask;
	private ScheduledFuture<?> _updateAndBroadcastStatusTask;
	private ScheduledFuture<?> _broadcastCharInfoTask;
	private ScheduledFuture<?> _broadcastStatusUpdateTask;
	
	private boolean _subclassLock = false;
	protected int _baseClass;
	protected int _activeClass;
	protected int _classIndex = 0;
	private boolean _isDeathKnight = false;
	private boolean _isShineMaker = false;
	private boolean _isWarg = false;
	private boolean _isSamurai = false;
	
	/** Data for mounted pets. */
	private int _controlItemId;
	private PetDataHolder _data;
	private PetLevelData _leveldata;
	private int _curFeed;
	protected Future<?> _mountFeedTask;
	private ScheduledFuture<?> _dismountTask;
	private boolean _petItems = false;
	
	/** The list of sub-classes this character has. */
	private final Map<Integer, SubClassHolder> _subClasses = new ConcurrentHashMap<>();
	
	private static final String ORIGINAL_CLASS_VAR = "OriginalClass";
	
	private final PlayerAppearance _appearance;
	
	/** The Experience of the Player before the last Death Penalty */
	private long _expBeforeDeath;
	private final Object _expSpLock = new Object();
	
	/** The number of player killed during a PvP (the player killed was PvP Flagged) */
	private int _pvpKills;
	
	/** The PK counter of the Player (= Number of non PvP Flagged player killed) */
	private int _pkKills;
	
	/** The total kill counter of the Player */
	private int _totalKills = 0;
	
	/** The total death counter of the Player */
	private int _totalDeaths = 0;
	
	/** The PvP Flag state of the Player (0=White, 1=Purple) */
	private byte _pvpFlag;
	
	private final Deque<DamageTakenHolder> _lastDamageTaken = new ArrayDeque<>(32);
	
	/** The Fame of this Player */
	private int _fame;
	private ScheduledFuture<?> _fameTask;
	
	/** The Raidboss points of this Player */
	private int _raidbossPoints;
	
	private ScheduledFuture<?> _teleportWatchdog;
	
	/** The Siege state of the Player */
	private byte _siegeState = 0;
	
	/** The id of castle/fort which the Player is registered for siege */
	private int _siegeSide = 0;
	
	private int _curWeightPenalty = 0;
	
	private int _lastCompassZone; // The last compass zone update send to the client.
	
	private final ContactList _contactList = new ContactList(this);
	
	private int _bookmarkslot = 0; // The Teleport Bookmark Slot
	
	private final TeleportBookmarkList _teleportBookmarks = new TeleportBookmarkList(this);
	
	private boolean _canFeed;
	private boolean _isInSiege;
	private boolean _isInHideoutSiege = false;
	
	/** Olympiad */
	private boolean _inOlympiadMode = false;
	private boolean _olympiadStart = false;
	private int _olympiadGameId = -1;
	private int _olympiadSide = -1;
	
	/** Duel */
	private boolean _isInDuel = false;
	private boolean _startingDuel = false;
	private int _duelState = Duel.DUELSTATE_NODUEL;
	private int _duelId = 0;
	private SystemMessageId _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
	
	/** Boat and AirShip */
	private Vehicle _vehicle = null;
	private Location _inVehiclePosition;
	
	private MountType _mountType = MountType.NONE;
	private int _mountNpcId;
	private int _mountLevel;
	/** Store object used to summon the strider you are mounting **/
	private int _mountObjectID = 0;
	
	private AdminTeleportType _teleportType = AdminTeleportType.NORMAL;
	
	private boolean _inCrystallize;
	private volatile boolean _isCrafting;
	
	/** The recipe book and manufacture shop of the Player */
	private final RecipeBook _recipeBook = new RecipeBook(this);
	
	private final PremiumItemList _premiumItemList = new PremiumItemList(this);
	
	/** True if the Player is sitting */
	private boolean _waitTypeSitting;
	private boolean _sittingInProgress;
	private int _throneId;
	
	/** Location before entering Observer Mode */
	private Location _lastLoc;
	private final ObserverMode _observerMode = new ObserverMode(this);
	private boolean _freeLook = false;
	
	/** Stored from last ValidatePosition **/
	private final Location _lastServerPosition = new Location(0, 0, 0);
	
	private final AtomicBoolean _blinkActive = new AtomicBoolean();
	
	private final Recommendations _recommendations = new Recommendations(this);
	
	private final PlayerInventory _inventory = new PlayerInventory(this);
	private final PlayerFreight _freight = new PlayerFreight(this);
	private final PlayerWarehouse _warehouse = new PlayerWarehouse(this);
	private PlayerRefund _refund;
	private PrivateStoreType _privateStoreType = PrivateStoreType.NONE;
	private TradeList _activeTradeList;
	private ItemContainer _activeWarehouse;
	private String _storeName = "";
	private TradeList _sellList;
	private TradeList _buyList;
	
	// Multisell
	private PreparedMultisellListHolder _currentMultiSell = null;
	
	private int _nobleLevel = 0;
	private boolean _hero = false;
	private boolean _trueHero = false;
	
	/** Premium System */
	private boolean _premiumStatus = false;
	
	/** Faction System */
	private boolean _isGood = false;
	private boolean _isEvil = false;
	
	/** The Npc corresponding to the last Folk which one the player talked. */
	private Npc _lastFolkNpc = null;
	
	/** Last NPC Id talked on a quest */
	private int _questNpcObject = 0;
	
	/** Used for simulating Quest onTalk */
	private boolean _simulatedTalking = false;
	
	/** The table containing all Quests began by the Player */
	private final Map<String, QuestState> _quests = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
	
	/** The list containing all shortcuts of this player. */
	private final Shortcuts _shortcuts = new Shortcuts(this);
	
	/** The list containing all macros of this player. */
	private final MacroList _macros = new MacroList(this);
	
	private final Set<Player> _snoopListener = ConcurrentHashMap.newKeySet();
	private final Set<Player> _snoopedPlayer = ConcurrentHashMap.newKeySet();
	
	private final HennaList _hennas = new HennaList(this);
	
	private static final String SYMBOL_POINTS_VAR = "SYMBOL_POINTS";
	
	/** The Pet of the Player */
	private Pet _pet = null;
	/** Servitors of the Player */
	private final Map<Integer, Summon> _servitors = new ConcurrentHashMap<>(1);
	/** The Agathions of the Player */
	private final int[] _agathionIds = new int[5];
	private int _lastAgathionSlot = 0;
	
	// Apparently, a Player CAN have both a summon AND a tamed beast at the same time!!
	// After Freya players can control more than one tamed beast.
	private final Set<TamedBeast> _tamedBeast = ConcurrentHashMap.newKeySet();
	
	// client radar
	private final Radar _radar;
	
	private MatchingRoom _matchingRoom;
	
	private ScheduledFuture<?> _taskWarnUserTakeBreak;
	
	// Clan related attributes
	/** The Clan Identifier of the Player */
	private int _clanId;
	
	/** The Clan object of the Player */
	private Clan _clan;
	
	/** Apprentice and Sponsor IDs */
	private int _apprentice = 0;
	private int _sponsor = 0;
	
	private long _clanJoinExpiryTime;
	private long _clanCreateExpiryTime;
	
	private int _powerGrade = 0;
	private ClanPrivileges _clanPrivileges = new ClanPrivileges();
	
	/** Player's pledge class (knight, Baron, etc.) */
	private int _pledgeClass = 0;
	private int _pledgeType = 0;
	
	/** Level at which the player joined the clan as an academy member */
	private int _lvlJoinedAcademy = 0;
	
	private int _wantsPeace = 0;
	
	// charges
	private final AtomicInteger _charges = new AtomicInteger();
	private ScheduledFuture<?> _chargeTask = null;
	
	// Absorbed Souls
	private int _souls = 0;
	private ScheduledFuture<?> _soulTask = null;
	
	// WorldPosition used by TARGET_SIGNET_GROUND
	private Location _currentSkillWorldPosition;
	
	private AccessLevel _accessLevel;
	
	private boolean _messageRefusal = false; // message refusal mode
	
	private boolean _silenceMode = false; // silence mode
	private List<Integer> _silenceModeExcluded; // silence mode
	private boolean _dietMode = false; // ignore weight penalty
	private boolean _tradeRefusal = false; // Trade refusal
	private boolean _exchangeRefusal = false; // Exchange refusal
	
	private Party _party;
	
	// This is needed to find the inviting player for Party response.
	// There can only be one active party request at once.
	private Player _activeRequester;
	private long _requestExpireTime = 0;
	private final RequestPartner _request = new RequestPartner(this);
	
	// Used for protection after teleport.
	private long _spawnProtectEndTime = 0;
	private long _teleportProtectEndTime = 0;
	
	private final Map<Integer, ExResponseCommissionInfo> _lastCommissionInfos = new ConcurrentHashMap<>();
	
	// Protects a char from aggro mobs when getting up from fake death.
	private long _recentFakeDeathEndTime = 0;
	private final Set<Integer> _fakeDeathWatchers = ConcurrentHashMap.newKeySet();
	
	/** The fists Weapon of the Player (used when no weapon is equipped) */
	private Weapon _fistsWeaponItem;
	
	private final Map<Integer, String> _chars = new ConcurrentSkipListMap<>();
	
	// private byte _updateKnownCounter = 0;
	
	private int _createItemLevel;
	private int _createCommonItemLevel;
	private ItemGrade _crystallizeGrade = ItemGrade.NONE;
	
	private final Map<Class<? extends AbstractRequest>, AbstractRequest> _requests = new ConcurrentHashMap<>();
	
	protected boolean _inventoryDisable = false;
	/** Player's cubics. */
	private final CubicList _cubics = new CubicList(this);
	/** Active shots. */
	protected Set<Integer> _activeSoulShots = ConcurrentHashMap.newKeySet();
	/** Active Brooch Jewels **/
	private BroochJewel _activeRubyJewel = null;
	private BroochJewel _activeSapphireJewel = null;
	
	private int _lastAmmunitionId = 0;
	
	/** Event parameters */
	private boolean _isRegisteredOnEvent = false;
	private boolean _isOnSoloEvent = false;
	private boolean _isOnEvent = false;
	
	private byte _handysBlockCheckerEventArena = -1;
	private KrateiArena _krateiArena = null;
	private int _ucKills = 0;
	private int _ucDeaths = 0;
	private int _ucState = 0;
	public static final int UC_STATE_NONE = 0;
	public static final int UC_STATE_POINT = 1;
	public static final int UC_STATE_ARENA = 2;
	
	/** New race ticket. */
	private final int[] _raceTickets = new int[2];
	
	private final BlockList _blockList = new BlockList(this);
	
	private final Map<Integer, Skill> _transformSkills = new ConcurrentHashMap<>();
	private ScheduledFuture<?> _taskRentPet;
	private ScheduledFuture<?> _taskWater;
	
	/** Last Html Npcs, 0 = last html was not bound to an npc */
	private final int[] _htmlActionOriginObjectIds = new int[HtmlActionScope.values().length];
	/**
	 * Origin of the last incoming html action request.<br>
	 * This can be used for htmls continuing the conversation with an npc.
	 */
	private int _lastHtmlActionOriginObjId;
	
	/** Bypass validations */
	@SuppressWarnings("unchecked")
	private final List<String>[] _htmlActionCaches = new List[HtmlActionScope.values().length];
	
	private Forum _forumMail;
	private Forum _forumMemo;
	
	/** Skills queued because a skill is already in progress */
	private SkillUseHolder _queuedSkill;
	private boolean _alterSkillActive = false;
	
	private int _cursedWeaponEquippedId = 0;
	private boolean _combatFlagEquippedId = false;
	
	private boolean _canRevive = true;
	private int _reviveRequested = 0;
	private double _revivePower = 0;
	private int _reviveHpPercent = 0;
	private int _reviveMpPercent = 0;
	private int _reviveCpPercent = 0;
	private boolean _revivePet = false;
	
	private double _cpUpdateIncCheck = .0;
	private double _cpUpdateDecCheck = .0;
	private double _cpUpdateInterval = .0;
	private double _mpUpdateIncCheck = .0;
	private double _mpUpdateDecCheck = .0;
	private double _mpUpdateInterval = .0;
	
	private double _originalCp = .0;
	private double _originalHp = .0;
	private double _originalMp = .0;
	
	/** Char Coords from Client */
	private int _clientX;
	private int _clientY;
	private int _clientZ;
	private int _clientHeading;
	
	// During fall validations will be disabled for 1000 ms.
	private static final int FALLING_VALIDATION_DELAY = 1000;
	private volatile long _fallingTimestamp = 0;
	private volatile int _fallingDamage = 0;
	private Future<?> _fallingDamageTask = null;
	
	private int _multiSocialTarget = 0;
	private int _multiSociaAction = 0;
	
	private MovieHolder _movieHolder = null;
	
	private String _adminConfirmCmd = null;
	
	private volatile long _lastItemAuctionInfoRequest = 0;
	
	private long _pvpFlagLasts;
	
	private long _notMoveUntil = 0;
	
	/** Map containing all custom skills of this player. */
	private Map<Integer, Skill> _customSkills = null;
	private volatile SkillReplacementList _skillReplacementList;
	
	private final OlympiadFightHistory _olympiadFightHistory = new OlympiadFightHistory(this);
	
	private volatile int _actionMask;
	
	private int _questZoneId = -1;
	
	private final Fishing _fishing = new Fishing(this);
	
	/**
	 * Updates the last recorded timestamp for the {@code pvpFlag}.<br>
	 * This value is used to track when the PvP status was last modified.
	 * @param time The new timestamp to set for the PvP flag.
	 */
	public void setPvpFlagLasts(long time)
	{
		_pvpFlagLasts = time;
	}
	
	/**
	 * Retrieves the last time the {@code pvpFlag} was updated.<br>
	 * This value is returned as a {@code long} timestamp.
	 * @return The last update time for the PvP flag.
	 */
	public long getPvpFlagLasts()
	{
		return _pvpFlagLasts;
	}
	
	/**
	 * Enables the PvP flag for this player.<br>
	 * This method sets the status to {@code 1}.<br>
	 * It also registers this player with the {@code PvpFlagTaskManager}.
	 */
	public void startPvPFlag()
	{
		updatePvPFlag(1);
		PvpFlagTaskManager.getInstance().add(this);
	}
	
	/**
	 * Stops the PvP registration task for this player.<br>
	 * This method removes the current player from the {@code PvpFlagTaskManager}.
	 */
	public void stopPvpRegTask()
	{
		PvpFlagTaskManager.getInstance().remove(this);
	}
	
	/**
	 * Stops the PvP registration task.<br>
	 * Sets the PvP flag value to {@code 0}.
	 */
	public void stopPvPFlag()
	{
		stopPvpRegTask();
		updatePvPFlag(0);
	}
	
	// Wedding
	private boolean _married = false;
	private int _partnerId = 0;
	private int _coupleId = 0;
	private boolean _engagerequest = false;
	private int _engageid = 0;
	private boolean _marryrequest = false;
	private boolean _marryaccepted = false;
	
	// Training Camp
	private static final String TRAINING_CAMP_VAR = "TRAINING_CAMP";
	private static final String TRAINING_CAMP_DURATION = "TRAINING_CAMP_DURATION";
	
	// Shared dualclass skills.
	private static final String KNOWN_DUAL_SKILLS_VAR = "KNOWN_DUAL_SKILLS";
	private static final int[] DUAL_CLASS_SKILLS =
	{
		19222, // Dignity of the Exalted
		19223, // Belief of the Exalted
		19224, // Blessing of the Exalted
		19225, // Summon Battle Potion
		19226, // Favor of the Exalted
		19229, // Fate of the Exalted
		19290, // Vitality of the Exalted
		31009, // Elements of the Exalted
		34498, // Conquest Ability Fire Source
		34499, // Conquest Ability Life Source
		34502, // Conquest Ability Flame Spark
		34501, // Conquest Ability Fire Totem
		34500, // Conquest Ability Battle Soul
		34495, // Conquest Ability Primordial Fire Source
		62240, // Tenacity Reward (Cave of Trials buff)
	};
	
	// Save responder name for log it.
	private String _lastPetitionGmName = null;
	
	private boolean _hasCharmOfCourage = false;
	private ScheduledFuture<?> _charmOfCourageTask;
	
	private final Set<Integer> _whisperers = ConcurrentHashMap.newKeySet();
	
	private final Attendance _attendance = new Attendance(this);
	
	private final AutoPlaySettingsHolder _autoPlaySettings = new AutoPlaySettingsHolder();
	private final AutoUseSettingsHolder _autoUseSettings = new AutoUseSettingsHolder();
	private final AtomicBoolean _autoPlaying = new AtomicBoolean();
	private boolean _resumedAutoPlay = false;
	
	private ScheduledFuture<?> _timedHuntingZoneTask = null;
	
	private final HomunculusList _homunculusList = new HomunculusList(this);
	
	private final CollectionList _collections = new CollectionList(this);
	
	private final CrossEventProgress _crossEventProgress = new CrossEventProgress(this);
	
	private final RelicList _relics = new RelicList(this);
	private final DeathPenalty _deathPenalty = new DeathPenalty(this);
	private HeroBookInfoHolder _heroBookInfo = null;
	
	private final HuntPass _huntPass;
	
	private final ChallengePoint _challengePoints;
	
	private final RankingHistory _rankingHistory;
	
	private final List<QuestTimer> _questTimers = new ArrayList<>();
	private final List<TimerHolder<?>> _timerHolders = new ArrayList<>();
	
	// Selling buffs system
	private boolean _isSellingBuffs = false;
	private List<SellBuffHolder> _sellingBuffs = null;
	
	/**
	 * Checks if the player is currently selling buffs.
	 * @return {@code true} if the player is selling buffs, {@code false} otherwise.
	 */
	public boolean isSellingBuffs()
	{
		return _isSellingBuffs;
	}
	
	/**
	 * Updates the status of whether selling buffs is enabled.<br>
	 * This method sets the internal flag to {@code true} or {@code false}.
	 * @param value The new state for selling buffs.
	 */
	public void setSellingBuffs(boolean value)
	{
		_isSellingBuffs = value;
	}
	
	/**
	 * Retrieves the list of active selling buffs.<br>
	 * This method ensures that a non-null {@code List} is returned.
	 * @return a {@code List} of {@code SellBuffHolder} objects.
	 */
	public List<SellBuffHolder> getSellingBuffs()
	{
		if (_sellingBuffs == null)
		{
			_sellingBuffs = new ArrayList<>();
		}
		
		return _sellingBuffs;
	}
	
	// Player client settings
	private ClientSettings _clientSettings;
	
	/**
	 * Retrieves the settings for the current client.<br>
	 * If the settings have not been initialized, they will be created now.
	 * @return the {@code ClientSettings} object associated with this player.
	 */
	public ClientSettings getClientSettings()
	{
		if (_clientSettings == null)
		{
			_clientSettings = new ClientSettings(this);
		}
		
		return _clientSettings;
	}
	
	/**
	 * Creates a new {@code Player} instance based on the provided template.<br>
	 * This method initializes the character data and saves it to the database.<br>
	 * It returns {@code null} if the database operation fails.
	 * @param template The {@code PlayerTemplate} used to define the character's base stats.
	 * @param accountName The name of the account this player belongs to.
	 * @param name The display name of the character.
	 * @param app The {@code PlayerAppearance} data for the character.
	 * @return A new {@code Player} object if successful, or {@code null} otherwise.
	 */
	public static Player create(PlayerTemplate template, String accountName, String name, PlayerAppearance app)
	{
		// Create a new Player with an account name.
		final Player player = new Player(template, accountName, app);
		
		// Set the name of the Player.
		player.setName(name);
		
		// Set access level.
		player.setAccessLevel(0, false, false);
		
		// Set Character's create time.
		player.setCreateDate(Calendar.getInstance());
		
		// Set the base class ID to that of the actual class ID.
		player.setBaseClass(player.getPlayerClass());
		
		// Give 20 recommendations.
		player.getRecommendations().setLeft(20);
		
		// Add the player in the characters table of the database.
		if (player.createDb())
		{
			PlayerInfoTable.getInstance().addName(player);
			return player;
		}
		
		return null;
	}
	
	/**
	 * Retrieves the name of the player.<br>
	 * If names are hidden during an olympiad, it returns the class name instead.
	 * @return The name of the player as a {@code String}.
	 */
	@Override
	public String getName()
	{
		if (OlympiadConfig.OLYMPIAD_HIDE_NAMES && isInOlympiadMode())
		{
			return ClassListData.getInstance().getClass(getPlayerClass()).getClassName();
		}
		
		return _name;
	}
	
	/**
	 * Retrieves the name of the current conquest.
	 * @return The {@code String} representing the conquest name.
	 */
	public String getConquestName()
	{
		return _conquestName;
	}
	
	/**
	 * Sets the name of the current conquest for the player.<br>
	 * This method updates the {@code PlayerVariables} and saves them to the database.<br>
	 * If the provided value is {@code null}, it removes the conquest name from the variables.
	 * @param conquestName The name of the conquest to set.
	 */
	public void setConquestName(String conquestName)
	{
		_conquestName = conquestName;
		
		final PlayerVariables variables = getVariables();
		if (conquestName == null)
		{
			variables.remove(PlayerVariables.CONQUEST_NAME);
		}
		else
		{
			variables.set(PlayerVariables.CONQUEST_NAME, conquestName);
		}
		
		variables.storeMe();
	}
	
	/**
	 * Retrieves the name of the account associated with this player.<br>
	 * It returns the local {@code _accountName} if no client is connected.<br>
	 * Otherwise, it retrieves the name from the active {@code _client}.
	 * @return The account name as a {@code String}.
	 */
	public String getAccountName()
	{
		return _client == null ? _accountName : _client.getAccountName();
	}
	
	/**
	 * Retrieves the account name associated with this player.<br>
	 * This method returns the {@code String} value stored in the internal account name field.
	 * @return The account name of the player as a {@code String}.
	 */
	public String getAccountNamePlayer()
	{
		return _accountName;
	}
	
	/**
	 * Retrieves the characters associated with this account.
	 * @return a {@code Map<Integer, String>} where keys are character IDs and values are names.
	 */
	public Map<Integer, String> getAccountChars()
	{
		return _chars;
	}
	
	/**
	 * Calculates the relationship status between this player and a target {@code Player}.<br>
	 * This method checks for various connections such as clan membership, party status, and siege states.<br>
	 * It returns a bitmask of all applicable relations based on the current game state.
	 * @param target The {@code Player} to check the relationship against.
	 * @return A {@code long} value representing the combined relation flags.
	 */
	public long getRelation(Player target)
	{
		final Clan clan = getClan();
		final Party party = getParty();
		final Clan targetClan = target.getClan();
		long result = 0;
		if (clan != null)
		{
			result |= RelationChanged.RELATION_CLAN_MEMBER;
			if (clan == targetClan)
			{
				result |= RelationChanged.RELATION_CLAN_MATE;
			}
			
			if (getAllyId() != 0)
			{
				result |= RelationChanged.RELATION_ALLY_MEMBER;
			}
		}
		
		if (isClanLeader())
		{
			result |= RelationChanged.RELATION_LEADER;
		}
		
		if ((party != null) && (party == target.getParty()))
		{
			result |= RelationChanged.RELATION_HAS_PARTY;
			final List<Player> members = party.getMembers();
			final int memberCount = members.size();
			for (int i = 0; i < memberCount; i++)
			{
				if (members.get(i) != this)
				{
					continue;
				}
				
				switch (i)
				{
					case 0:
					{
						result |= RelationChanged.RELATION_PARTYLEADER; // 0x10
						break;
					}
					case 1:
					{
						result |= RelationChanged.RELATION_PARTY4; // 0x8
						break;
					}
					case 2:
					{
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY2 + RelationChanged.RELATION_PARTY1; // 0x7
						break;
					}
					case 3:
					{
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY2; // 0x6
						break;
					}
					case 4:
					{
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY1; // 0x5
						break;
					}
					case 5:
					{
						result |= RelationChanged.RELATION_PARTY3; // 0x4
						break;
					}
					case 6:
					{
						result |= RelationChanged.RELATION_PARTY2 + RelationChanged.RELATION_PARTY1; // 0x3
						break;
					}
					case 7:
					{
						result |= RelationChanged.RELATION_PARTY2; // 0x2
						break;
					}
					case 8:
					{
						result |= RelationChanged.RELATION_PARTY1; // 0x1
						break;
					}
				}
			}
		}
		
		// Registered attackers and defenders are neutral outside the battlefield.
		if ((_siegeState != 0) && isInsideZone(ZoneId.SIEGE))
		{
			result |= RelationChanged.RELATION_INSIEGE;
			if (getSiegeState() != target.getSiegeState())
			{
				result |= RelationChanged.RELATION_ENEMY;
			}
			else
			{
				result |= RelationChanged.RELATION_ALLY;
			}
			
			if (_siegeState == 1)
			{
				result |= RelationChanged.RELATION_ATTACKER;
			}
		}
		
		if ((clan != null) && (targetClan != null))
		{
			final ClanWar war = clan.getWarWith(targetClan.getId());
			if (war != null)
			{
				switch (war.getState())
				{
					case DECLARATION:
					case BLOOD_DECLARATION:
					{
						if (war.getAttackerClanId() != target.getClanId())
						{
							result |= RelationChanged.RELATION_DECLARED_WAR;
						}
						break;
					}
					case MUTUAL:
					{
						result |= RelationChanged.RELATION_DECLARED_WAR;
						result |= RelationChanged.RELATION_MUTUAL_WAR;
						break;
					}
				}
			}
		}
		
		if (_handysBlockCheckerEventArena != -1)
		{
			result |= RelationChanged.RELATION_INSIEGE;
			final ArenaParticipantsHolder holder = HandysBlockCheckerManager.getInstance().getHolder(getBlockCheckerArena());
			if (holder.getPlayerTeam(this) == 0)
			{
				result |= RelationChanged.RELATION_ENEMY;
			}
			else
			{
				result |= RelationChanged.RELATION_ALLY;
			}
			
			result |= RelationChanged.RELATION_ATTACKER;
		}
		
		return result;
	}
	
	/**
	 * Loads a {@code Player} object from the database using its unique identifier.<br>
	 * This method retrieves and restores all data associated with the given ID.
	 * @param objectId The unique database ID of the player to load.
	 * @return The loaded {@code Player} instance.
	 */
	public static Player load(int objectId)
	{
		return restore(objectId);
	}
	
	/**
	 * Initializes the update intervals and check values for CP and MP.<br>
	 * These values are used to calculate how often status updates occur based on maximum stats.
	 */
	private void initPcStatusUpdateValues()
	{
		final int maxCp = getMaxCp();
		_cpUpdateInterval = maxCp / 352.0;
		_cpUpdateIncCheck = maxCp;
		_cpUpdateDecCheck = maxCp - _cpUpdateInterval;
		
		final int maxMp = getMaxMp();
		_mpUpdateInterval = maxMp / 352.0;
		_mpUpdateIncCheck = maxMp;
		_mpUpdateDecCheck = maxMp - _mpUpdateInterval;
	}
	
	/**
	 * Initializes a new {@code Player} instance with the provided data.<br>
	 * This constructor sets up the basic player properties and internal systems.
	 * @param objectId The unique identifier for the player object.
	 * @param template The {@code PlayerTemplate} containing base data.
	 * @param accountName The name of the associated account.
	 * @param app The {@code PlayerAppearance} used to define the character's look.
	 */
	private Player(int objectId, PlayerTemplate template, String accountName, PlayerAppearance app)
	{
		super(objectId, template);
		setInstanceType(InstanceType.Player);
		initCharStatusUpdateValues();
		initPcStatusUpdateValues();
		
		for (int i = 0; i < _htmlActionCaches.length; ++i)
		{
			_htmlActionCaches[i] = new ArrayList<>();
		}
		
		_conquestName = getVariables().getString(PlayerVariables.CONQUEST_NAME, null);
		
		_accountName = accountName;
		app.setOwner(this);
		_appearance = app;
		
		_huntPass = HuntPassConfig.ENABLE_HUNT_PASS ? new HuntPass(this) : null;
		
		// Create an AI.
		getAI();
		
		// Create a Radar object.
		_radar = new Radar(this);
		_challengePoints = new ChallengePoint(this);
		_rankingHistory = new RankingHistory(this);
	}
	
	/**
	 * Creates a new {@code Player} instance with a generated unique ID.<br>
	 * This constructor uses the provided template and appearance data.
	 * @param template The {@code PlayerTemplate} containing base character data.
	 * @param accountName The name of the user account.
	 * @param app The {@code PlayerAppearance} defining the visual look.
	 */
	private Player(PlayerTemplate template, String accountName, PlayerAppearance app)
	{
		this(IdManager.getInstance().getNextId(), template, accountName, app);
	}
	
	/**
	 * Retrieves the statistics for the player.<br>
	 * This method calls the parent class to get the {@code PlayerStat}.
	 * @return the {@code PlayerStat} object.
	 */
	@Override
	public PlayerStat getStat()
	{
		return (PlayerStat) super.getStat();
	}
	
	/**
	 * Initializes the character statistics for this player.<br>
	 * This method creates a new {@code PlayerStat} object and assigns it to the current instance.
	 */
	@Override
	public void initCharStat()
	{
		setStat(new PlayerStat(this));
	}
	
	/**
	 * Retrieves the current status of the player.<br>
	 * This method calls the {@code getStatus()} method from the parent class.
	 * @return the {@code PlayerStatus} object for this player.
	 */
	@Override
	public PlayerStatus getStatus()
	{
		return (PlayerStatus) super.getStatus();
	}
	
	/**
	 * Initializes the {@code PlayerStatus} for this character.<br>
	 * This method creates a new instance of {@code PlayerStatus} using the current object.<br>
	 * It is used to set up the status data when the character is loaded.
	 */
	@Override
	public void initCharStatus()
	{
		setStatus(new PlayerStatus(this));
	}
	
	/**
	 * Retrieves the current appearance of the player.<br>
	 * This method returns the {@code PlayerAppearance} object associated with this character.
	 * @return the {@code PlayerAppearance} of the player.
	 */
	public PlayerAppearance getAppearance()
	{
		return _appearance;
	}
	
	/**
	 * Checks if the hair accessory feature is currently enabled.<br>
	 * This method retrieves the value from the player variables.<br>
	 * It returns {@code true} by default if no value is set.
	 * @return {@code true} if hair accessories are enabled, {@code false} otherwise.
	 */
	public boolean isHairAccessoryEnabled()
	{
		return getVariables().getBoolean(PlayerVariables.HAIR_ACCESSORY_VARIABLE_NAME, true);
	}
	
	/**
	 * Enables or disables the hair accessory feature for the player.<br>
	 * This updates the {@code HAIR_ACCESSORY_VARIABLE_NAME} in the player variables.
	 * @param enabled The state to set for the hair accessory.
	 */
	public void setHairAccessoryEnabled(boolean enabled)
	{
		getVariables().set(PlayerVariables.HAIR_ACCESSORY_VARIABLE_NAME, enabled);
	}
	
	/**
	 * Retrieves the base template for this player.<br>
	 * It returns the template of the original class if it exists.<br>
	 * Otherwise, it returns the template for the base class.
	 * @return the {@code PlayerTemplate} associated with the player's class.
	 */
	public PlayerTemplate getBaseTemplate()
	{
		final PlayerClass originalClass = getOriginalClass();
		if (originalClass != null)
		{
			return PlayerTemplateData.getInstance().getTemplate(originalClass.getId());
		}
		
		return PlayerTemplateData.getInstance().getTemplate(_baseClass);
	}
	
	/**
	 * Retrieves the original class of the player.<br>
	 * This method looks up the value stored in the {@code ORIGINAL_CLASS_VAR}.<br>
	 * It returns a {@code PlayerClass} object or {@code null} if not found.
	 * @return The {@code PlayerClass} representing the player's original class.
	 */
	public PlayerClass getOriginalClass()
	{
		return getVariables().getEnum(ORIGINAL_CLASS_VAR, PlayerClass.class, null);
	}
	
	/**
	 * Updates the original class of the player.<br>
	 * This method stores the provided {@code PlayerClass} in the internal variables map.
	 * @param newClass The {@code PlayerClass} to be set as the original class.
	 */
	public void setOriginalClass(PlayerClass newClass)
	{
		getVariables().set(ORIGINAL_CLASS_VAR, newClass);
	}
	
	/**
	 * Removes the {@code ORIGINAL_CLASS_VAR} from the variables map.<br>
	 * This method resets the original class reference for the actor.
	 */
	public void resetOriginalClass()
	{
		getVariables().remove(ORIGINAL_CLASS_VAR);
	}
	
	/**
	 * Retrieves the current {@code HuntPass} object.
	 * @return the {@code HuntPass} associated with this entity.
	 */
	public HuntPass getHuntPass()
	{
		return _huntPass;
	}
	
	/**
	 * Retrieves the history of ranking data.<br>
	 * This method returns a collection of {@code RankingHistoryDataHolder} objects.
	 * @return A {@code Collection} containing the ranking history data.
	 */
	public Collection<RankingHistoryDataHolder> getRankingHistoryData()
	{
		return _rankingHistory.getData();
	}
	
	/**
	 * Retrieves the current amount of challenge points.
	 * @return the current {@code ChallengePoint} value.
	 */
	public ChallengePoint getChallengeInfo()
	{
		return _challengePoints;
	}
	
	/**
	 * Retrieves the template associated with this player.<br>
	 * This method returns a {@code PlayerTemplate} object.
	 * @return the {@code PlayerTemplate} for this entity
	 */
	@Override
	public PlayerTemplate getTemplate()
	{
		return (PlayerTemplate) super.getTemplate();
	}
	
	/**
	 * Updates the template for this player.<br>
	 * This method retrieves and sets the data from a {@code PlayerTemplate}.
	 * @param newclass The class identifier used to find the new template.
	 */
	public void setTemplate(PlayerClass newclass)
	{
		super.setTemplate(PlayerTemplateData.getInstance().getTemplate(newclass));
	}
	
	/**
	 * Initializes the artificial intelligence for this player.<br>
	 * This method creates and returns a new {@code PlayerAI} instance.
	 * @return the initialized {@code CreatureAI} object.
	 */
	@Override
	protected CreatureAI initAI()
	{
		return new PlayerAI(this);
	}
	
	/**
	 * Retrieves the current level of the player.<br>
	 * This method fetches the level from the {@code PlayerStat} object.
	 * @return The integer value representing the player's level.
	 */
	@Override
	public int getLevel()
	{
		return getStat().getLevel();
	}
	
	/**
	 * Sets the base class for this actor.<br>
	 * This value determines the primary template used for the entity.
	 * @param baseClass The {@code int} ID of the base class to set.
	 */
	public void setBaseClass(int baseClass)
	{
		_baseClass = baseClass;
	}
	
	/**
	 * Sets the base class ID for the player.<br>
	 * This method updates the internal {@code _baseClass} field.
	 * @param playerClass The {@code PlayerClass} object containing the new ID.
	 */
	public void setBaseClass(PlayerClass playerClass)
	{
		_baseClass = playerClass.getId();
	}
	
	/**
	 * Checks if the player is currently in a store mode.<br>
	 * This method returns {@code true} if the private store type is not {@code NONE}.
	 * @return {@code true} if the player is in a store; {@code false} otherwise.
	 */
	public boolean isInStoreMode()
	{
		return _privateStoreType != PrivateStoreType.NONE;
	}
	
	/**
	 * Checks if the player is currently in a store mode.<br>
	 * This includes buy, sell, or package sell modes.
	 * @return {@code true} if the player is in any of these modes, {@code false} otherwise.
	 */
	public boolean isInStoreSellOrBuyMode()
	{
		return (_privateStoreType == PrivateStoreType.BUY) || (_privateStoreType == PrivateStoreType.SELL) || (_privateStoreType == PrivateStoreType.PACKAGE_SELL);
	}
	
	/**
	 * Checks if the player is currently in the crafting state.
	 * @return {@code true} if the player is crafting, {@code false} otherwise.
	 */
	public boolean isCrafting()
	{
		return _isCrafting;
	}
	
	/**
	 * Updates the crafting status of the actor.<br>
	 * This method sets whether the character is currently engaged in crafting.
	 * @param isCrafting The new crafting state to set. Use {@code true} for active and {@code false} for inactive.
	 */
	public void setCrafting(boolean isCrafting)
	{
		_isCrafting = isCrafting;
	}
	
	/**
	 * Retrieves the recipe book of this player.
	 * @return the {@code RecipeBook} of this player.
	 */
	public RecipeBook getRecipeBook()
	{
		return _recipeBook;
	}
	
	/**
	 * Retrieves the ID of the last quest NPC object.<br>
	 * This value is used to track the most recent interaction with a quest NPC.
	 * @return The unique identifier for the last quest NPC object.
	 */
	public int getLastQuestNpcObject()
	{
		return _questNpcObject;
	}
	
	/**
	 * Updates the ID of the last quest NPC object.<br>
	 * This value is used to track progress in quests.
	 * @param npcId The unique identifier for the NPC object.
	 */
	public void setLastQuestNpcObject(int npcId)
	{
		_questNpcObject = npcId;
	}
	
	/**
	 * Checks if the actor is currently simulating talking.<br>
	 * This is used to determine if the character is in a fake conversation state.
	 * @return {@code true} if simulation is active, {@code false} otherwise.
	 */
	public boolean isSimulatingTalking()
	{
		return _simulatedTalking;
	}
	
	/**
	 * Enables or disables the simulated talking feature.<br>
	 * This setting controls whether the actor appears to be speaking.
	 * @param value The {@code boolean} state to set for simulated talking.
	 */
	public void setSimulatedTalking(boolean value)
	{
		_simulatedTalking = value;
	}
	
	/**
	 * Retrieves the current state of a specific quest.<br>
	 * This method looks up the quest in the internal collection.
	 * @param quest The unique identifier for the quest.
	 * @return The {@code QuestState} associated with the provided quest, or {@code null} if not found.
	 */
	public QuestState getQuestState(String quest)
	{
		return _quests.get(quest);
	}
	
	/**
	 * Updates the quest state for a specific quest.<br>
	 * This method adds or replaces the {@code QuestState} in the internal quest map.
	 * @param qs The {@code QuestState} object to be stored.
	 */
	public void setQuestState(QuestState qs)
	{
		_quests.put(qs.getQuestName(), qs);
	}
	
	/**
	 * Checks if the actor has a specific quest in their progress.
	 * @param quest The unique identifier of the quest to check.
	 * @return {@code true} if the quest exists, otherwise {@code false}.
	 */
	public boolean hasQuestState(String quest)
	{
		return _quests.containsKey(quest);
	}
	
	/**
	 * Checks if any of the provided quest IDs have been completed.<br>
	 * It iterates through all active quest states for the actor.
	 * @param questIds A {@code List<Integer>} containing the IDs to check.
	 * @return {@code true} if at least one quest in the list is finished, otherwise {@code false}.
	 */
	public boolean hasAnyCompletedQuestStates(List<Integer> questIds)
	{
		for (QuestState questState : _quests.values())
		{
			if (questIds.contains(questState.getQuest().getId()) && questState.isCompleted())
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Removes a specific quest from the player's active quests.<br>
	 * This method updates the internal {@code _quests} collection.
	 * @param quest The unique identifier of the quest to remove.
	 */
	public void delQuestState(String quest)
	{
		_quests.remove(quest);
	}
	
	/**
	 * Retrieves all quest states for the current actor.<br>
	 * This method returns a collection of {@code QuestState} objects.
	 * @return A {@code Collection} containing all {@code QuestState} values.
	 */
	public Collection<QuestState> getAllQuestStates()
	{
		return _quests.values();
	}
	
	/**
	 * Retrieves all quests that are currently active.<br>
	 * This method filters out quests that have not started or have an ID of 1 or less.
	 * @return A {@code Collection} of {@code Quest} objects.
	 */
	public Collection<Quest> getAllActiveQuests()
	{
		final List<Quest> activeQuests = new ArrayList<>();
		for (QuestState questState : _quests.values())
		{
			if (!questState.isStarted())
			{
				continue;
			}
			
			final Quest quest = questState.getQuest();
			if ((quest == null) || (quest.getId() <= 1))
			{
				continue;
			}
			
			activeQuests.add(quest);
		}
		
		return activeQuests;
	}
	
	/**
	 * Processes a specific event for a given script.<br>
	 * This method checks if the player is near the correct NPC to trigger the action.<br>
	 * If the conditions are met, it notifies the quest of the event.
	 * @param scriptName The name of the script to process.
	 * @param event The specific event identifier to trigger.
	 */
	public void processScriptEvent(String scriptName, String event)
	{
		final Quest quest = ScriptManager.getInstance().getScript(scriptName);
		if ((quest == null) || (event == null) || event.isEmpty())
		{
			return;
		}
		
		final Npc target = _lastFolkNpc;
		if ((target != null) && isInsideRadius3D(target, Npc.INTERACTION_DISTANCE))
		{
			quest.notifyEvent(event, target, this);
		}
		else if (_questNpcObject > 0)
		{
			final WorldObject object = World.findObject(getLastQuestNpcObject());
			if ((object != null) && object.isNpc() && isInsideRadius3D(object, Npc.INTERACTION_DISTANCE))
			{
				final Npc npc = object.asNpc();
				quest.notifyEvent(event, npc, this);
			}
		}
	}
	
	/** List of all QuestState instance that needs to be notified of this Player's or its pet's death */
	private Set<QuestState> _notifyQuestOfDeathList;
	
	/**
	 * Adds a quest to the list of quests that notify the player upon death.<br>
	 * This method checks if the {@code QuestState} is already present before adding it.<br>
	 * It returns early if the provided {@code QuestState} is {@code null}.
	 * @param qs The {@code QuestState} to be added.
	 */
	public void addNotifyQuestOfDeath(QuestState qs)
	{
		if (qs == null)
		{
			return;
		}
		
		final Set<QuestState> notifyQuestOfDeath = getNotifyQuestOfDeath();
		if (!notifyQuestOfDeath.contains(qs))
		{
			notifyQuestOfDeath.add(qs);
		}
	}
	
	/**
	 * Removes a specific quest state from the notification list.<br>
	 * This method checks if both the {@code QuestState} and the internal list are not {@code null}.<br>
	 * If they exist, it removes the provided {@code QuestState} object.
	 * @param qs The {@code QuestState} to be removed from the notification list.
	 */
	public void removeNotifyQuestOfDeath(QuestState qs)
	{
		if ((qs == null) || (_notifyQuestOfDeathList == null))
		{
			return;
		}
		
		_notifyQuestOfDeathList.remove(qs);
	}
	
	/**
	 * Retrieves the set of quest states that should be notified upon death.<br>
	 * This method ensures the internal list is initialized before returning it.
	 * @return a {@code Set} of {@code QuestState} objects.
	 */
	public Set<QuestState> getNotifyQuestOfDeath()
	{
		if (_notifyQuestOfDeathList == null)
		{
			synchronized (this)
			{
				if (_notifyQuestOfDeathList == null)
				{
					_notifyQuestOfDeathList = ConcurrentHashMap.newKeySet();
				}
			}
		}
		
		return _notifyQuestOfDeathList;
	}
	
	/**
	 * Checks if the list of quests to notify on death is empty.<br>
	 * It returns {@code true} if the list is {@code null} or contains no items.
	 * @return {@code true} if there are no quest notifications, {@code false} otherwise.
	 */
	public boolean isNotifyQuestOfDeathEmpty()
	{
		return (_notifyQuestOfDeathList == null) || _notifyQuestOfDeathList.isEmpty();
	}
	
	/**
	 * Sends a notification to the player about available quests.<br>
	 * It checks for new quests that meet the starting conditions.<br>
	 * If a valid quest is found, it sends an acceptance dialog.
	 */
	public void sendQuestList()
	{
		sendPacket(new ExQuestNotificationAll(this));
		for (NewQuest newQuest : NewQuestData.getInstance().getQuests())
		{
			if (newQuest.getQuestType() != 2)
			{
				final Quest quest = ScriptManager.getInstance().getQuest(newQuest.getId());
				if (quest != null)
				{
					final QuestState questState = getQuestState(quest.getName());
					if ((questState == null) && quest.canStartQuest(this) && !newQuest.getConditions().getSpecificStart())
					{
						sendPacket(new ExQuestDialog(quest.getId(), QuestDialogType.ACCEPT));
						break; // Only send first dialog.
					}
				}
			}
		}
	}
	
	/**
	 * Restores the player's shortcut settings.<br>
	 * This method calls {@code restoreMe()} on the {@code _shortcuts} object.
	 */
	private void restoreShortcuts()
	{
		_shortcuts.restoreMe();
	}
	
	/**
	 * Retrieves all shortcuts associated with the actor.<br>
	 * This method returns a collection of {@code Shortcut} objects.
	 * @return A {@code Collection} containing all {@code Shortcut} items.
	 */
	public Collection<Shortcut> getAllShortcuts()
	{
		return _shortcuts.getAllShortcuts();
	}
	
	/**
	 * Retrieves a specific shortcut based on its position.<br>
	 * This method looks up the item in the provided slot and page.
	 * @param slot The index of the shortcut slot.
	 * @param page The page number where the shortcut is located.
	 * @return The {@code Shortcut} object found at the specified location.
	 */
	public Shortcut getShortcut(int slot, int page)
	{
		return _shortcuts.getShortcut(slot, page);
	}
	
	/**
	 * Adds a new shortcut to the current list.<br>
	 * This method registers the provided {@code Shortcut} object.
	 * @param shortcut The {@code Shortcut} to be registered.
	 */
	public void registerShortcut(Shortcut shortcut)
	{
		_shortcuts.registerShortcut(shortcut);
	}
	
	/**
	 * Updates the shortcut information for a specific skill.<br>
	 * This method synchronizes the skill data with the player's shortcuts.
	 * @param skillId The unique identifier of the skill.
	 * @param skillLevel The current level of the skill.
	 * @param skillSubLevel The sub-level associated with the skill.
	 */
	public void updateShortcuts(int skillId, int skillLevel, int skillSubLevel)
	{
		_shortcuts.updateShortcuts(skillId, skillLevel, skillSubLevel);
	}
	
	/**
	 * Removes a shortcut from the specified location.<br>
	 * This method updates the internal shortcuts list for the player.
	 * @param slot The index of the shortcut slot to clear.
	 * @param page The page number where the shortcut is located.
	 */
	public void deleteShortcut(int slot, int page)
	{
		_shortcuts.deleteShortcut(slot, page);
	}
	
	/**
	 * Adds a new macro to the internal registry.<br>
	 * This allows the system to recognize and execute the provided {@code Macro}.
	 * @param macro The {@code Macro} object to register.
	 */
	public void registerMacro(Macro macro)
	{
		_macros.registerMacro(macro);
	}
	
	/**
	 * Removes a specific macro from the player's list.<br>
	 * This method uses the unique identifier to locate and delete the entry.
	 * @param id The unique {@code int} identifier of the macro to be removed.
	 */
	public void deleteMacro(int id)
	{
		_macros.deleteMacro(id);
	}
	
	/**
	 * Retrieves the list of macros associated with this actor.
	 * @return a {@code MacroList} containing all current macros.
	 */
	public MacroList getMacros()
	{
		return _macros;
	}
	
	/**
	 * Updates the current siege state of the player.<br>
	 * This method sets the internal {@code _siegeState} variable.
	 * @param siegeState The new siege state value to apply.
	 */
	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}
	
	/**
	 * Retrieves the current siege state of the player.
	 * @return The {@code byte} value representing the siege state.
	 */
	public byte getSiegeState()
	{
		return _siegeState;
	}
	
	/**
	 * Sets the side of the siege for this actor.<br>
	 * This updates the {@code _siegeSide} field with a new value.
	 * @param value The integer representing the siege side.
	 */
	public void setSiegeSide(int value)
	{
		_siegeSide = value;
	}
	
	/**
	 * Checks if the actor is registered on a specific siege field.<br>
	 * It returns {@code true} if the side matches or falls within a neutral range.
	 * @param value The siege side identifier to check against.
	 * @return {@code true} if the actor belongs to the specified side; {@code false} otherwise.
	 */
	public boolean isRegisteredOnThisSiegeField(int value)
	{
		return (_siegeSide == value) || ((_siegeSide >= 81) && (_siegeSide <= 89));
	}
	
	/**
	 * Retrieves the current siege side of the player.<br>
	 * This value indicates which team or faction the player belongs to during a siege event.
	 * @return The integer representing the siege side.
	 */
	public int getSiegeSide()
	{
		return _siegeSide;
	}
	
	/**
	 * Checks if the target is an ally during a siege.<br>
	 * This method verifies that both entities are in the same siege zone and share the same side.<br>
	 * It also validates specific victory conditions for attackers.
	 * @param target The {@code WorldObject} to check against.
	 * @return {@code true} if the target is a friend, {@code false} otherwise.
	 */
	public boolean isSiegeFriend(WorldObject target)
	{
		// If i'm natural or not in siege zone, not friends.
		if ((_siegeState == 0) || !isInsideZone(ZoneId.SIEGE))
		{
			return false;
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(_siegeSide);
		if (castle == null)
		{
			return false;
		}
		
		// If target isn't a player, is self.
		final Player targetPlayer = target.asPlayer();
		if ((targetPlayer == null) || (targetPlayer == this))
		{
			return false;
		}
		
		// If target isn't on same siege or not on same state, not friends.
		final int targetSiegeState = targetPlayer.getSiegeState();
		if ((targetPlayer.getSiegeSide() != _siegeSide) || (_siegeState != targetSiegeState))
		{
			return false;
		}
		
		if (_siegeState == 1)
		{
			// Check first castle mid victory.
			if (!castle.isFirstMidVictory() && (_siegeState == targetSiegeState))
			{
				return true;
			}
			
			// Attackers are considered friends only if castle has no owner.
			return castle.getOwner() == null;
		}
		
		// Both are defenders, friends.
		return true;
	}
	
	/**
	 * Sets the player's PvP flag status.<br>
	 * This method updates the {@code _pvpFlag} field with a new value.
	 * @param pvpFlag The new integer value for the PvP flag.
	 */
	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = (byte) pvpFlag;
	}
	
	/**
	 * Retrieves the current PvP flag status.<br>
	 * This value indicates if the player is currently in a PvP state.
	 * @return The {@code byte} value of the PvP flag.
	 */
	@Override
	public byte getPvpFlag()
	{
		return _pvpFlag;
	}
	
	/**
	 * Updates the PvP flag for this player.<br>
	 * This method synchronizes the change with other players and summons.<br>
	 * It broadcasts the new status to nearby visible objects.
	 * @param value The new PvP flag value to set.
	 */
	@Override
	public void updatePvPFlag(int value)
	{
		if (_pvpFlag == value)
		{
			return;
		}
		
		setPvpFlag(value);
		
		final StatusUpdate su = new StatusUpdate(this);
		computeStatusUpdate(su, StatusUpdateType.PVP_FLAG);
		if (su.hasUpdates())
		{
			broadcastPacket(su);
			sendPacket(su);
		}
		
		// If this player has a pet update the pets pvp flag as well.
		if (hasSummon())
		{
			final RelationChanged rc = new RelationChanged();
			final Summon pet = _pet;
			if (pet != null)
			{
				rc.addRelation(pet, getRelation(this), false);
			}
			
			if (hasServitors())
			{
				getServitors().values().forEach(s -> rc.addRelation(s, getRelation(this), false));
			}
			
			sendPacket(rc);
		}
		
		World.forEachVisibleObject(this, Player.class, player ->
		{
			if (!isVisibleFor(player))
			{
				return;
			}
			
			final long relation = getRelation(player);
			final boolean isAutoAttackable = isAutoAttackable(player);
			final int objectId = player.getObjectId();
			final RelationCache oldrelation = getKnownRelations().get(objectId);
			if ((oldrelation == null) || (oldrelation.getRelation() != relation) || (oldrelation.isAutoAttackable() != isAutoAttackable))
			{
				final RelationChanged rc = new RelationChanged();
				rc.addRelation(this, relation, isAutoAttackable);
				if (hasSummon())
				{
					final Summon pet = _pet;
					if (pet != null)
					{
						rc.addRelation(pet, relation, isAutoAttackable);
					}
					
					if (hasServitors())
					{
						getServitors().values().forEach(s -> rc.addRelation(s, relation, isAutoAttackable));
					}
				}
				
				player.sendPacket(rc);
				getKnownRelations().put(objectId, new RelationCache(relation, isAutoAttackable));
			}
		});
	}
	
	/**
	 * Updates the current zone status for this entity.<br>
	 * This method checks if the entity has moved far enough to require a new validation.<br>
	 * It also handles specific logic for water states and special zones like Siege or PVP.
	 * @param force If {@code true}, the validation is performed immediately regardless of distance.
	 */
	@Override
	public void revalidateZone(boolean force)
	{
		// Cannot validate if not in a world region (happens during teleport).
		if (getWorldRegion() == null)
		{
			return;
		}
		
		// This function is called too often from movement code.
		if (!force && (calculateDistance3D(_lastZoneValidateLocation) < 100))
		{
			return;
		}
		
		_lastZoneValidateLocation.setXYZ(this);
		
		ZoneManager.getInstance().getRegion(this).revalidateZones(this);
		
		if (GeneralConfig.ALLOW_WATER)
		{
			checkWaterState();
		}
		
		if (!isInsideZone(ZoneId.PEACE) && !_autoUseSettings.isEmpty())
		{
			AutoUseTaskManager.getInstance().startAutoUseTask(this);
		}
		
		if (isInsideZone(ZoneId.ALTERED))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.ALTEREDZONE)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.ALTEREDZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.ALTEREDZONE));
		}
		else if (isInsideZone(ZoneId.SIEGE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE));
		}
		else if (isInsideZone(ZoneId.PVP))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.PVPZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE));
		}
		else if (isInsideZone(ZoneId.PEACE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE));
		}
		else if (isInsideZone(ZoneId.NO_PVP))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.NOPVPZONE)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.NOPVPZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.NOPVPZONE));
		}
		else
		{
			if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE)
			{
				return;
			}
			
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE)
			{
				updatePvPStatus();
			}
			
			_lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE));
		}
	}
	
	/**
	 * Retrieves the current item level for creation.<br>
	 * This value determines the level requirement for items.
	 * @return The integer value of the create item level.
	 */
	public int getCreateItemLevel()
	{
		return _createItemLevel;
	}
	
	/**
	 * Sets the required level to create an item.<br>
	 * This value determines the minimum level needed for creation.
	 * @param createItemLevel The new level requirement for creating items.
	 */
	public void setCreateItemLevel(int createItemLevel)
	{
		_createItemLevel = createItemLevel;
	}
	
	/**
	 * Retrieves the level required to create a common item.
	 * @return The {@code int} value of the creation level.
	 */
	public int getCreateCommonItemLevel()
	{
		return _createCommonItemLevel;
	}
	
	/**
	 * Sets the level required to create common items.<br>
	 * This value determines the minimum level for common item creation.
	 * @param createCommonItemLevel The required level for creating common items.
	 */
	public void setCreateCommonItemLevel(int createCommonItemLevel)
	{
		_createCommonItemLevel = createCommonItemLevel;
	}
	
	/**
	 * Retrieves the current crystallize grade of the player.
	 * @return the {@code ItemGrade} value representing the crystallization level.
	 */
	public ItemGrade getCrystallizeGrade()
	{
		return _crystallizeGrade;
	}
	
	/**
	 * Sets the crystallization grade for the player.<br>
	 * If the provided value is {@code null}, it defaults to {@code ItemGrade.NONE}.
	 * @param crystallizeGrade The new {@code ItemGrade} to assign.
	 */
	public void setCrystallizeGrade(ItemGrade crystallizeGrade)
	{
		_crystallizeGrade = crystallizeGrade != null ? crystallizeGrade : ItemGrade.NONE;
	}
	
	/**
	 * Retrieves the total number of player kills.
	 * @return The current count of {@code pkKills}.
	 */
	public int getPkKills()
	{
		return _pkKills;
	}
	
	/**
	 * Updates the player's total number of PK kills.<br>
	 * This method notifies listeners if the kill count changes.
	 * @param pkKills The new number of PK kills to set.
	 */
	public void setPkKills(int pkKills)
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PK_CHANGED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPKChanged(this, _pkKills, pkKills), this);
		}
		
		_pkKills = pkKills;
		if (_pkKills > FeatureConfig.PK_PENALTY_LIST_MINIMUM_COUNT)
		{
			World.addPkPlayer(this);
		}
	}
	
	/**
	 * Retrieves the total number of kills for this character.
	 * @return The total kill count as an {@code int}.
	 */
	public int getTotalKills()
	{
		return _totalKills;
	}
	
	/**
	 * Retrieves the total number of deaths for this character.
	 * @return the total death count as an {@code int}.
	 */
	public int getTotalDeaths()
	{
		return _totalDeaths;
	}
	
	/**
	 * Updates the total number of kills for the player.<br>
	 * This method sets the {@code _totalKills} field to a new value.
	 * @param value The new total kill count to assign.
	 */
	public void setTotalKills(int value)
	{
		_totalKills = value;
	}
	
	/**
	 * Updates the total number of deaths for the player.<br>
	 * This method sets the {@code _totalDeaths} field to a new value.
	 * @param value The new total death count to assign.
	 */
	public void setTotalDeaths(int value)
	{
		_totalDeaths = value;
	}
	
	/**
	 * Retrieves the current delete timer value.<br>
	 * This value represents the time remaining before an entity is removed.
	 * @return The current {@code long} value of the delete timer.
	 */
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}
	
	/**
	 * Sets the time remaining before this actor is deleted.<br>
	 * This value is stored in the {@code _deleteTimer} field.
	 * @param deleteTimer The amount of time until deletion occurs.
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}
	
	/**
	 * Retrieves the {@code Recommendations} of this player.
	 * @return the {@code Recommendations} of this player.
	 */
	public Recommendations getRecommendations()
	{
		return _recommendations;
	}
	
	/**
	 * Sets the experience value to be restored after death.<br>
	 * This value determines how much {@code exp} a player keeps when they die.
	 * @param exp The amount of experience to store before death.
	 */
	public void setExpBeforeDeath(long exp)
	{
		_expBeforeDeath = exp;
	}
	
	/**
	 * Retrieves the experience amount held by the player before their last death.<br>
	 * This value is used to track progress lost during gameplay.
	 * @return The experience value as a {@code long}.
	 */
	public long getExpBeforeDeath()
	{
		return _expBeforeDeath;
	}
	
	/**
	 * Sets the starting reputation value for the character.<br>
	 * This method updates the internal reputation state.
	 * @param reputation The new reputation value to assign.
	 */
	public void setInitialReputation(int reputation)
	{
		super.setReputation(reputation);
	}
	
	/**
	 * Updates the reputation value of the player.<br>
	 * This method checks for limits defined in {@code PvpConfig}.<br>
	 * It also notifies listeners and updates visible guards if reputation becomes negative.
	 * @param value The new reputation value to set.
	 */
	@Override
	public void setReputation(int value)
	{
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_REPUTATION_CHANGED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerReputationChanged(this, getReputation(), value), this);
		}
		
		int reputation = value;
		if (reputation > PvpConfig.MAX_REPUTATION) // Max count of positive reputation
		{
			reputation = PvpConfig.MAX_REPUTATION;
		}
		
		final int currentReputation = getReputation();
		if (currentReputation == reputation)
		{
			return;
		}
		
		if ((currentReputation >= 0) && (reputation < 0))
		{
			World.forEachVisibleObject(this, Guard.class, object ->
			{
				final CreatureAI ai = object.getAI();
				if (ai.getIntention() == Intention.IDLE)
				{
					ai.setIntentionActive();
				}
			});
		}
		
		super.setReputation(reputation);
		
		sendPacket(new SystemMessage(SystemMessageId.YOUR_REPUTATION_HAS_BEEN_CHANGED_TO_S1).addInt(currentReputation));
		broadcastReputation();
		
		if (currentReputation >= 0)
		{
			World.removePkPlayer(this);
		}
	}
	
	/**
	 * Retrieves the current weight penalty for the player.<br>
	 * This value returns {@code 0} if the player is in diet mode.<br>
	 * Otherwise, it returns the stored {@code _curWeightPenalty}.
	 * @return The current weight penalty as an {@code int}.
	 */
	public int getWeightPenalty()
	{
		return _dietMode ? 0 : _curWeightPenalty;
	}
	
	/**
	 * Updates the overloaded status based on current load.<br>
	 * This method recalculates weight penalties and applies skills accordingly.<br>
	 * If changes occur, it optionally sends a packet to clients.
	 * @param broadcast Whether to notify other players of the status change.
	 */
	public void refreshOverloaded(boolean broadcast)
	{
		final int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			final long weightproc = (((getCurrentLoad() - getBonusWeightPenalty()) * 1000L) / maxLoad);
			int newWeightPenalty;
			if ((weightproc < 500) || _dietMode)
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if (weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}
			
			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if ((newWeightPenalty > 0) && !_dietMode)
				{
					addSkill(SkillData.getInstance().getSkill(CommonSkill.WEIGHT_PENALTY.getId(), newWeightPenalty));
					setOverloaded(getCurrentLoad() > maxLoad);
				}
				else
				{
					removeSkill(getKnownSkill(4270), false, true);
					setOverloaded(false);
				}
				
				if (broadcast)
				{
					sendPacket(new EtcStatusUpdate(this));
					broadcastUserInfo();
				}
			}
		}
	}
	
	/**
	 * Equips or unequips an item for the player.<br>
	 * Validates that the item is owned by the player and located in a valid slot.<br>
	 * Handles mana consumption, shot recharging, and event notifications upon equipping.<br>
	 * Updates inventory limits and broadcasts information to other players.
	 * @param item The {@code Item} to be equipped or unequipped.
	 * @param abortAttack Whether to cancel the current attack if {@code true}.
	 */
	public void useEquippableItem(Item item, boolean abortAttack)
	{
		// Check if the item is null.
		if (item == null)
		{
			return;
		}
		
		// Check if the item is owned by this player.
		if (item.getOwnerId() != getObjectId())
		{
			return;
		}
		
		// Check if the item is in the inventory.
		final ItemLocation itemLocation = item.getItemLocation();
		if ((itemLocation != ItemLocation.INVENTORY) && (itemLocation != ItemLocation.PAPERDOLL))
		{
			return;
		}
		
		// Equip or unEquip.
		Collection<Item> items = null;
		final boolean isEquiped = item.isEquipped();
		final Item isFirstSlotAgathion = getInventory().getPaperdollItem(Inventory.PAPERDOLL_AGATHION1);
		SystemMessage sm = null;
		if (isEquiped)
		{
			final BodyPart bodyPart = BodyPart.fromItem(item);
			if (item.isEnchanted())
			{
				final int enchantLevel = item.getEnchantLevel();
				if (bodyPart == BodyPart.AGATHION)
				{
					sm = new SystemMessage(SystemMessageId.S1_S2_S_POWER_WAS_SEALED);
					sm.addInt(enchantLevel);
					sm.addItemName(item);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_S2_UNEQUIPPED);
					sm.addInt(enchantLevel);
					sm.addItemName(item);
				}
			}
			else
			{
				if (bodyPart == BodyPart.AGATHION)
				{
					sm = new SystemMessage(SystemMessageId.S1_S_POWER_WAS_SEALED);
					sm.addItemName(item);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_UNEQUIPPED);
					sm.addItemName(item);
				}
			}
			
			sendPacket(sm);
			
			// We can't unequip talisman by body slot, and a virtual item only ever leaves the slot it was applied to.
			if ((bodyPart == BodyPart.DECO) || (bodyPart == BodyPart.BROOCH_JEWEL) || (bodyPart == BodyPart.AGATHION) || (bodyPart == BodyPart.ARTIFACT) || item.isVirtual())
			{
				items = _inventory.unEquipItemInSlotAndRecord(item.getLocationSlot());
			}
			else
			{
				items = _inventory.unEquipItemInBodySlotAndRecord(bodyPart);
			}
		}
		else
		{
			items = _inventory.equipItemAndRecord(item);
			if (item.isEquipped())
			{
				final BodyPart bodyPart = item.getTemplate().getBodyPart();
				if (item.isEnchanted())
				{
					final int enchantLevel = item.getEnchantLevel();
					if (item.isArmor() && (bodyPart == BodyPart.AGATHION))
					{
						if (isFirstSlotAgathion != null)
						{
							sm = new SystemMessage(SystemMessageId.S1_S2_IS_SUMMONED_AS_A_SECONDARY_AGATHION);
							sm.addInt(enchantLevel);
							sm.addItemName(item);
							
							final SystemMessage sm2 = new SystemMessage(SystemMessageId.ONLY_SECONDARY_SKILLS_OF_S1_S2_ARE_AVAILABLE);
							sm2.addInt(enchantLevel);
							sm2.addItemName(item);
							sendPacket(sm2);
						}
						else
						{
							sm = new SystemMessage(SystemMessageId.S1_S2_IS_SUMMONED_AS_A_PRIMARY_AGATHION);
							sm.addInt(enchantLevel);
							sm.addItemName(item);
							
							final SystemMessage sm2 = new SystemMessage(SystemMessageId.S1_S2_S_POWER_WAS_UNLOCKED_THEREBY_ACTIVATING_ALL_ITS_ABILITIES);
							sm2.addInt(enchantLevel);
							sm2.addItemName(item);
							sendPacket(sm2);
						}
					}
					else
					{
						sm = new SystemMessage(SystemMessageId.S1_S2_EQUIPPED);
						sm.addInt(enchantLevel);
						sm.addItemName(item);
					}
				}
				else
				{
					if (item.isArmor() && (bodyPart == BodyPart.AGATHION))
					{
						if (isFirstSlotAgathion != null)
						{
							sm = new SystemMessage(SystemMessageId.S1_IS_SUMMONED_AS_A_SECONDARY_AGATHION);
							sm.addItemName(item);
							
							final SystemMessage sm2 = new SystemMessage(SystemMessageId.ONLY_S1_S_UNIQUE_ABILITY_BECOMES_ACTIVE);
							sm2.addItemName(item);
							sendPacket(sm2);
						}
						else
						{
							sm = new SystemMessage(SystemMessageId.S1_IS_SUMMONED_AS_A_PRIMARY_AGATHION);
							sm.addItemName(item);
							
							final SystemMessage sm2 = new SystemMessage(SystemMessageId.S1_S_POWER_WAS_UNLOCKED_THEREBY_ACTIVATING_ALL_ITS_ABILITIES);
							sm2.addItemName(item);
							sendPacket(sm2);
						}
					}
					else
					{
						sm = new SystemMessage(SystemMessageId.S1_EQUIPPED);
						sm.addItemName(item);
					}
				}
				
				sendPacket(sm);
				
				// Consume mana - will start a task if required; returns if item is not a shadow item.
				item.decreaseMana(false);
				
				if ((bodyPart == BodyPart.R_HAND) || (bodyPart == BodyPart.LR_HAND))
				{
					rechargeShots(true, true, false);
				}
				
				// Notify to scripts.
				final ItemTemplate itemTemplate = item.getTemplate();
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_EQUIP, itemTemplate))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemEquip(this, item), itemTemplate);
				}
			}
			else
			{
				sendPacket(SystemMessageId.NO_EQUIPMENT_SLOT_AVAILABLE);
			}
		}
		
		broadcastUserInfo();
		ThreadPool.schedule(() -> sendPacket(new ExUserInfoEquipSlot(this)), 100);
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addItems(items);
		sendInventoryUpdate(iu);
		
		if (abortAttack)
		{
			abortAttack();
		}
	}
	
	/**
	 * Retrieves the total number of player versus player kills.<br>
	 * This value represents the count of opponents defeated in combat.
	 * @return The total number of {@code pvpKills}.
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}
	
	/**
	 * Updates the total number of player versus player kills.<br>
	 * This method notifies listeners if the value changes.
	 * @param pvpKills The new count of {@code pvpKills} to set.
	 */
	public void setPvpKills(int pvpKills)
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PVP_CHANGED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPvPChanged(this, _pvpKills, pvpKills), this);
		}
		
		_pvpKills = pvpKills;
	}
	
	/**
	 * Retrieves the current fame value of the actor.
	 * @return The current {@code int} fame value.
	 */
	public int getFame()
	{
		return _fame;
	}
	
	/**
	 * Updates the player's fame value.<br>
	 * This method ensures the value stays between 0 and {@code PlayerConfig.MAX_PERSONAL_FAME_POINTS}.<br>
	 * It also triggers an event if the fame value changes.
	 * @param fame The new fame value to set for the player.
	 */
	public void setFame(int fame)
	{
		int newFame = fame;
		if (fame > PlayerConfig.MAX_PERSONAL_FAME_POINTS)
		{
			newFame = PlayerConfig.MAX_PERSONAL_FAME_POINTS;
		}
		else if (fame < 0)
		{
			newFame = 0;
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_FAME_CHANGED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerFameChanged(this, _fame, newFame), this);
		}
		
		_fame = newFame;
	}
	
	/**
	 * Retrieves the current raid boss points for the player.
	 * @return The total number of raid boss points as an {@code int}.
	 */
	public int getRaidbossPoints()
	{
		return _raidbossPoints;
	}
	
	/**
	 * Sets the current raid boss points for the player.<br>
	 * This method ensures the value stays between {@code 0} and {@code 2000000000}.
	 * @param points The number of points to assign.
	 */
	public void setRaidbossPoints(int points)
	{
		int value = points;
		if (points > 2000000000) // Close to integer max value (2147483647).
		{
			value = 2000000000;
		}
		else if (points < 0)
		{
			value = 0;
		}
		
		_raidbossPoints = value;
	}
	
	/**
	 * Increases the current raid boss points for the player.<br>
	 * This method updates the internal point total by adding a specific amount.
	 * @param increasePoints The number of points to add to the current total.
	 */
	public void increaseRaidbossPoints(int increasePoints)
	{
		setRaidbossPoints(_raidbossPoints + increasePoints);
	}
	
	/**
	 * Retrieves the current clan contribution points for the player.<br>
	 * It fetches the value from the {@code PlayerVariables} storage.<br>
	 * Returns 0 if no contribution has been recorded.
	 * @return The total number of clan contribution points as an {@code int}.
	 */
	public int getClanContribution()
	{
		return getVariables().getInt(PlayerVariables.CLAN_CONTRIBUTION, 0);
	}
	
	/**
	 * Retrieves the total contribution points for the clan.<br>
	 * This value is stored in the {@code PlayerVariables} map.
	 * @return The total number of clan contribution points as an {@code int}.
	 */
	public int getClanContributionTotal()
	{
		return getVariables().getInt(PlayerVariables.CLAN_CONTRIBUTION_TOTAL, 0);
	}
	
	/**
	 * Retrieves the previous clan contribution value.<br>
	 * This method fetches the integer stored in {@code PlayerVariables.CLAN_CONTRIBUTION_PREVIOUS}.<br>
	 * It returns a default value of {@code 0} if no value is found.
	 * @return The previous clan contribution as an {@code int}.
	 */
	public int getClanContributionPrevious()
	{
		return getVariables().getInt(PlayerVariables.CLAN_CONTRIBUTION_PREVIOUS, 0);
	}
	
	/**
	 * Retrieves the total clan contribution from the previous period.<br>
	 * This value is stored in the player's variables.
	 * @return The total contribution as an {@code int}.
	 */
	public int getClanContributionTotalPrevious()
	{
		return getVariables().getInt(PlayerVariables.CLAN_CONTRIBUTION_TOTAL_PREVIOUS, 0);
	}
	
	/**
	 * Increases the clan contribution score of the player.<br>
	 * The amount is added to both current and total contribution variables.<br>
	 * The value is capped at {@code FeatureConfig.CLAN_CONTRIBUTION_REQUIRED}.<br>
	 * A system message is sent to notify the player of the increase.
	 * @param amount The amount to add to the clan contribution.
	 */
	public void increaseClanContribution(int amount)
	{
		if ((amount <= 0) || (getClanContribution() >= FeatureConfig.CLAN_CONTRIBUTION_REQUIRED))
		{
			return;
		}
		
		getVariables().set(PlayerVariables.CLAN_CONTRIBUTION, Math.min(FeatureConfig.CLAN_CONTRIBUTION_REQUIRED, getClanContribution() + amount));
		getVariables().set(PlayerVariables.CLAN_CONTRIBUTION_TOTAL, Math.min(100000000, getClanContributionTotal() + amount));
		
		sendPacket(new SystemMessage(SystemMessageId.YOUR_CONTRIBUTION_SCORE_HAS_INCREASED_BY_S1).addInt(amount));
	}
	
	/**
	 * Retrieves the character class of the player.<br>
	 * This method fetches the class information from the player template.
	 * @return the {@code PlayerClass} of the current player.
	 */
	public PlayerClass getPlayerClass()
	{
		return getTemplate().getPlayerClass();
	}
	
	/**
	 * Updates the player's class to a new one.<br>
	 * This method handles all logic for class transfers including rewards and notifications.<br>
	 * It also updates the player's stats and skills in the background.
	 * @param id The {@code int} identifier of the new class.
	 */
	public void setPlayerClass(int id)
	{
		if (_subclassLock)
		{
			return;
		}
		
		_subclassLock = true;
		
		try
		{
			if ((getLvlJoinedAcademy() != 0) && (_clan != null) && CategoryData.getInstance().isInCategory(CategoryType.SIXTH_CLASS_GROUP, id))
			{
				if (_lvlJoinedAcademy <= 16)
				{
					_clan.addReputationScore(FeatureConfig.JOIN_ACADEMY_MAX_REP_SCORE);
				}
				else if (_lvlJoinedAcademy >= 39)
				{
					_clan.addReputationScore(FeatureConfig.JOIN_ACADEMY_MIN_REP_SCORE);
				}
				else
				{
					_clan.addReputationScore((FeatureConfig.JOIN_ACADEMY_MAX_REP_SCORE - ((_lvlJoinedAcademy - 16) * 20)));
				}
				
				setLvlJoinedAcademy(0);
				
				// Oust pledge member from the academy, cuz he has awakened.
				final SystemMessage msg = new SystemMessage(SystemMessageId.S1_IS_DISMISSED_FROM_THE_CLAN);
				msg.addPcName(this);
				_clan.broadcastToOnlineMembers(msg);
				_clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(_name));
				_clan.removeClanMember(getObjectId(), 0);
				sendPacket(SystemMessageId.CONGRATULATIONS_YOU_WILL_NOW_GRADUATE_FROM_THE_CLAN_ACADEMY_AND_LEAVE_YOUR_CURRENT_CLAN_YOU_CAN_NOW_JOIN_A_CLAN_WITHOUT_BEING_SUBJECT_TO_ANY_PENALTIES);
				
				// receive graduation gift
				_inventory.addItem(ItemProcessType.REWARD, 8181, 1, this, null); // give academy circlet
			}
			
			if (isSubClassActive())
			{
				getSubClasses().get(_classIndex).setPlayerClass(id);
			}
			
			setTarget(this);
			broadcastSkillPacket(new MagicSkillUse(this, 5103, 1, 0, 0), this);
			setClassTemplate(id);
			if (isInCategory(CategoryType.FOURTH_CLASS_GROUP))
			{
				sendPacket(SystemMessageId.CONGRATULATIONS_YOU_VE_COMPLETED_YOUR_THIRD_CLASS_TRANSFER_QUEST);
			}
			else
			{
				sendPacket(SystemMessageId.CONGRATULATIONS_YOU_VE_COMPLETED_THE_CLASS_CHANGE);
			}
			
			// Remove class permitted hennas.
			for (int slot = 1; slot < 5; slot++)
			{
				final Henna henna = _hennas.get(slot);
				if ((henna != null) && !henna.isAllowedClass(getPlayerClass()))
				{
					_hennas.remove(slot);
				}
			}
			
			// Update class icon in party and clan.
			if (isInParty())
			{
				_party.broadcastPacket(new PartySmallWindowUpdate(this, true));
			}
			
			if (_clan != null)
			{
				_clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
			}
			
			sendPacket(new ExSubjobInfo(this, SubclassInfoType.CLASS_CHANGED));
			
			// Add AutoGet skills and normal skills and/or learnByFS depending on configurations.
			rewardSkills();
			
			if (!isGM() && PlayerConfig.DECREASE_SKILL_LEVEL)
			{
				checkPlayerSkills();
			}
			
			_friends.notifyStatus(FriendStatus.MODE_CLASS);
		}
		finally
		{
			_subclassLock = false;
			
			PlayerInfoTable.getInstance().setClassId(getObjectId(), id);
			
			ThreadPool.schedule(() ->
			{
				getInventory().applyItemSkills();
				getStat().recalculateStats(false);
				updateAbnormalVisualEffects();
				sendSkillList();
			}, 100);
		}
	}
	
	/**
	 * Checks if the class is currently undergoing a change.<br>
	 * This method returns the state of the {@code _subclassLock}.
	 * @return {@code true} if the class is changing, {@code false} otherwise.
	 */
	public boolean isChangingClass()
	{
		return _subclassLock;
	}
	
	/**
	 * Retrieves the current experience points of the actor.<br>
	 * This method fetches the value from the {@code PlayerStat} object.
	 * @return The total experience as a {@code long}.
	 */
	public long getExp()
	{
		return getStat().getExp();
	}
	
	/**
	 * Sets the current fists weapon for the character.<br>
	 * This updates the {@code _fistsWeaponItem} field with the provided item.
	 * @param weaponItem The {@code Weapon} object to be set as the fists weapon.
	 */
	public void setFistsWeaponItem(Weapon weaponItem)
	{
		_fistsWeaponItem = weaponItem;
	}
	
	/**
	 * Retrieves the current fists weapon item.<br>
	 * This method returns the {@code Weapon} object associated with the character's fists.
	 * @return the {@code Weapon} item used as a fists weapon
	 */
	public Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}
	
	/**
	 * Retrieves the correct fist weapon based on a character class ID.<br>
	 * This method maps specific ranges of {@code classId} to their corresponding {@code Weapon} templates.<br>
	 * It returns a default weapon if the provided ID does not match any known range.
	 * @param classId The unique identifier for the character class.
	 * @return The {@code Weapon} object associated with the given class.
	 */
	public Weapon findFistsWeaponItem(int classId)
	{
		final Weapon weaponItem;
		
		if ((classId >= 0x00) && (classId <= 0x09))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(246 /* Human Fighter Fist */);
		}
		else if ((classId >= 0x0a) && (classId <= 0x11))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(251 /* Human Mystic Fist */);
		}
		else if ((classId >= 0x12) && (classId <= 0x18))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(244 /* Elven Fighter Fist */);
		}
		else if ((classId >= 0x19) && (classId <= 0x1e))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(249 /* Elven Mystic Fist */);
		}
		else if ((classId >= 0x1f) && (classId <= 0x25))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(245 /* Dark Fighter Fist */);
		}
		else if ((classId >= 0x26) && (classId <= 0x2b))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(250 /* Dark Mystic Fist */);
		}
		else if ((classId >= 0x2c) && (classId <= 0x30))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(248 /* Orc Fighter Fist */);
		}
		else if ((classId >= 0x31) && (classId <= 0x34))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(252 /* Orc Shaman Fist */);
		}
		else if ((classId >= 0x35) && (classId <= 0x39))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(247 /* Dwarven Fighter Fist */);
		}
		else // Default fist item to avoid NPEs.
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(246 /* Human Fighter Fist */);
		}
		
		return weaponItem;
	}
	
	/**
	 * Updates and grants skills to the player based on current configuration.<br>
	 * This method handles auto-learning, race skills, and special rewards.<br>
	 * It also checks for skill level decreases and item restrictions.
	 */
	public void rewardSkills()
	{
		// Give all normal skills if activated Auto-Learn is activated, included AutoGet skills.
		if (PlayerConfig.AUTO_LEARN_SKILLS)
		{
			giveAvailableSkills(PlayerConfig.AUTO_LEARN_FS_SKILLS, PlayerConfig.AUTO_LEARN_FP_SKILLS, true, PlayerConfig.AUTO_LEARN_SKILLS_WITHOUT_ITEMS);
		}
		else
		{
			giveAvailableAutoGetSkills();
		}
		
		if (PlayerConfig.DECREASE_SKILL_LEVEL && !isGM())
		{
			checkPlayerSkills();
		}
		
		for (SkillLearn skill : SkillTreeData.getInstance().getRaceSkillTree(getRace()))
		{
			if (getLevel() >= skill.getGetLevel())
			{
				addSkill(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()), true);
			}
		}
		
		if (FreeMountsConfig.ENABLE_FREE_STRIDER)
		{
			final Skill skill = SkillData.getInstance().getSkill(49991, 1);
			if (skill != null)
			{
				addSkill(skill, false);
			}
		}
		
		if (FreeMountsConfig.ENABLE_FREE_WYVERN)
		{
			final Skill skill = SkillData.getInstance().getSkill(49992, 1);
			if (skill != null)
			{
				addSkill(skill, false);
			}
		}
		
		if (RebirthConfig.REBIRTH_ALLOW_REBIRTH)
		{
			RebirthManager.getInstance().grantRebirthSkills(this);
		}
		
		checkItemRestriction();
		sendSkillList();
		restoreAutoShortcutVisual();
	}
	
	/**
	 * Grants temporary skills based on the actor's status.<br>
	 * This includes noble, hero, and clan-related skills.<br>
	 * It also handles rebirth skills and reloads passive effects from equipped items.
	 */
	public void regiveTemporarySkills()
	{
		// Do not call this on enterworld or char load.
		
		// Add noble skills if noble.
		if (_nobleLevel > 0)
		{
			setNobleLevel(_nobleLevel);
		}
		
		// Add Hero skills if hero.
		if (_hero)
		{
			setHero(true);
		}
		
		// Add clan skills.
		if (_clan != null)
		{
			_clan.addSkillEffects(this);
			
			if ((_clan.getLevel() >= SiegeManager.getInstance().getSiegeClanMinLevel()) && isClanLeader())
			{
				SiegeManager.getInstance().addSiegeSkills(this);
			}
			
			if (_clan.getCastleId() > 0)
			{
				final Castle castle = CastleManager.getInstance().getCastleByOwner(_clan);
				if (castle != null)
				{
					castle.giveResidentialSkills(this);
				}
			}
			
			if (_clan.getFortId() > 0)
			{
				final Fort fort = FortManager.getInstance().getFortByOwner(_clan);
				if (fort != null)
				{
					fort.giveResidentialSkills(this);
				}
			}
		}
		
		if (RebirthConfig.REBIRTH_ALLOW_REBIRTH)
		{
			RebirthManager.getInstance().grantRebirthSkills(this);
		}
		
		// Reload passive skills from armors / jewels / weapons.
		_inventory.reloadEquippedItems();
	}
	
	/**
	 * Calculates and updates the available skills for the player.<br>
	 * This method checks various conditions to determine which skills can be learned.<br>
	 * It automatically adds new skills to the character and handles shortcut updates.
	 * @param includeByFs Whether to include skills based on FS.
	 * @param includeByFp Whether to include skills based on FP.
	 * @param includeAutoGet Whether to include auto-get skills.
	 * @param includeRequiredItems Whether to check for required items.
	 * @return The number of new skills learned by the player.
	 */
	public int giveAvailableSkills(boolean includeByFs, boolean includeByFp, boolean includeAutoGet, boolean includeRequiredItems)
	{
		int skillCounter = 0;
		
		// Get available skills.
		final Collection<Skill> skills = SkillTreeData.getInstance().getAllAvailableSkills(this, getTemplate().getPlayerClass(), includeByFs, includeByFp, includeAutoGet, includeRequiredItems);
		final List<Skill> skillsForStore = new ArrayList<>();
		for (Skill skill : skills)
		{
			final int skillId = skill.getId();
			final Skill oldSkill = getKnownSkill(skillId);
			if (oldSkill == skill)
			{
				continue;
			}
			
			if (getSkillLevel(skillId) == 0)
			{
				skillCounter++;
			}
			
			// Fix when learning toggle skills.
			if (skill.isToggle() && !skill.isNecessaryToggle() && isAffectedBySkill(skillId))
			{
				stopSkillEffects(SkillFinishType.REMOVED, skillId);
			}
			
			// Mobius: Keep sublevel on skill level increase.
			final int skillLevel = skill.getLevel();
			final int currentSubLevel = skill.getSubLevel();
			if (oldSkill != null)
			{
				final int oldSubLevel = oldSkill.getSubLevel();
				if ((oldSubLevel > 0) && (currentSubLevel == 0) && (oldSkill.getLevel() < skillLevel))
				{
					skill = SkillData.getInstance().getSkill(skillId, skillLevel, oldSubLevel);
				}
			}
			
			addSkill(skill, false);
			skillsForStore.add(skill);
			
			if (PlayerConfig.AUTO_LEARN_SKILLS)
			{
				updateShortcuts(skillId, skillLevel, currentSubLevel);
			}
		}
		
		storeSkills(skillsForStore, -1);
		
		if (PlayerConfig.AUTO_LEARN_SKILLS && (skillCounter > 0))
		{
			// Sending ShortcutInit breaks auto use shortcuts.
			// sendPacket(new ShortcutInit(this));
			
			sendMessage("You have learned " + skillCounter + " new skills.");
		}
		
		restoreAutoShortcutVisual();
		
		return skillCounter;
	}
	
	/**
	 * Retrieves and applies all available auto-get skills to the actor.<br>
	 * This method identifies the highest level of each available skill.<br>
	 * It then adds these skills to the actor's current skill set.
	 */
	public void giveAvailableAutoGetSkills()
	{
		// Get available skills.
		final List<SkillLearn> autoGetSkills = SkillTreeData.getInstance().getAvailableAutoGetSkills(this);
		final SkillData st = SkillData.getInstance();
		
		// Keep only the highest available level per skill. When several levels of the same auto-get skill become available at once (e.g. after a level jump),
		// the unordered skill list could otherwise leave a lower level applied, since adding a skill simply overwrites the previous level regardless of value.
		final Map<Integer, SkillLearn> highestLevelById = new HashMap<>();
		for (SkillLearn s : autoGetSkills)
		{
			final int skillId = s.getSkillId();
			final SkillLearn current = highestLevelById.get(skillId);
			if ((current == null) || (s.getSkillLevel() > current.getSkillLevel()))
			{
				highestLevelById.put(skillId, s);
			}
		}
		
		Skill skill;
		for (SkillLearn s : highestLevelById.values())
		{
			skill = st.getSkill(s.getSkillId(), s.getSkillLevel());
			if (skill != null)
			{
				addSkill(skill, true);
			}
			else
			{
				LOGGER.warning("Skipping null auto-get skill for " + this);
			}
		}
	}
	
	/**
	 * Sets the experience points for the character.<br>
	 * The value is clamped to a minimum of {@code 0}.
	 * @param exp The new experience value to set.
	 */
	public void setExp(long exp)
	{
		getStat().setExp(Math.max(0, exp));
	}
	
	/**
	 * Retrieves the race of the player.<br>
	 * This method checks for an original class, active subclasses, or template data.
	 * @return the {@code Race} of the player.
	 */
	@Override
	public Race getRace()
	{
		final PlayerClass originalClass = getOriginalClass();
		if (originalClass != null)
		{
			return originalClass.getRace();
		}
		
		if (!isSubClassActive())
		{
			return getTemplate().getRace();
		}
		
		return PlayerTemplateData.getInstance().getTemplate(_baseClass).getRace();
	}
	
	/**
	 * Retrieves the current {@code Radar} object.
	 * @return the {@code Radar} instance.
	 */
	public Radar getRadar()
	{
		return _radar;
	}
	
	/**
	 * Retrieves the current SP value of the actor.<br>
	 * This method fetches the SP from the {@code PlayerStat} object.
	 * @return The current SP as a {@code long}.
	 */
	public long getSp()
	{
		return getStat().getSp();
	}
	
	/**
	 * Sets the current SP value for the actor.<br>
	 * The value is clamped to a minimum of {@code 0}.
	 * @param sp The new SP value to set.
	 */
	public void setSp(long sp)
	{
		super.getStat().setSp(Math.max(0, sp));
	}
	
	/**
	 * Checks if the player is the lord of a specific castle.<br>
	 * This method verifies if the player leads the clan that owns the castle.
	 * @param castleId The unique identifier of the castle to check.
	 * @return {@code true} if the player is the lord, otherwise {@code false}.
	 */
	public boolean isCastleLord(int castleId)
	{
		// Player has clan and is the clan leader, check the castle info.
		if ((_clan != null) && (_clan.getLeader().getPlayer() == this))
		{
			// If the clan has a castle and it is actually the queried castle, return true.
			final Castle castle = CastleManager.getInstance().getCastleByOwner(_clan);
			if ((castle != null) && (castle == CastleManager.getInstance().getCastleById(castleId)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves the unique identifier of the player's clan.
	 * @return The {@code int} ID of the clan.
	 */
	@Override
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * Retrieves the unique identifier for the clan crest.<br>
	 * Returns {@code 0} if the actor is not part of a clan.
	 * @return The integer ID of the clan crest.
	 */
	public int getClanCrestId()
	{
		if (_clan != null)
		{
			return _clan.getCrestId();
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the unique identifier for the large clan crest.<br>
	 * This value is only returned if the player belongs to a clan with an active castle or hideout.<br>
	 * If no such location exists, it returns {@code 0}.
	 * @return The {@code int} ID of the large clan crest.
	 */
	public int getClanCrestLargeId()
	{
		if ((_clan != null) && ((_clan.getCastleId() != 0) || (_clan.getHideoutId() != 0)))
		{
			return _clan.getCrestLargeId();
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the expiration time for joining a clan.<br>
	 * This value represents the timestamp when the join permission expires.
	 * @return The expiry time as a {@code long}.
	 */
	public long getClanJoinExpiryTime()
	{
		return _clanJoinExpiryTime;
	}
	
	/**
	 * Sets the expiration time for joining a clan.<br>
	 * This value determines how long a join request remains valid.
	 * @param time The duration in milliseconds until the join request expires.
	 */
	public void setClanJoinExpiryTime(long time)
	{
		_clanJoinExpiryTime = time;
	}
	
	/**
	 * Retrieves the expiration time for clan creation.<br>
	 * This value represents when the ability to create a new clan expires.
	 * @return The expiration time as a {@code long}.
	 */
	public long getClanCreateExpiryTime()
	{
		return _clanCreateExpiryTime;
	}
	
	/**
	 * Sets the expiration time for clan creation.<br>
	 * This value determines how long a player can remain in the process of creating a clan.
	 * @param time The expiration time in milliseconds.
	 */
	public void setClanCreateExpiryTime(long time)
	{
		_clanCreateExpiryTime = time;
	}
	
	/**
	 * Updates the total online time for the player.<br>
	 * This method also resets the start time of the current session to the current system time.
	 * @param time The new online time value in milliseconds.
	 */
	public void setOnlineTime(long time)
	{
		_onlineTime = time;
		_onlineBeginTime = System.currentTimeMillis();
	}
	
	/**
	 * Calculates the total time the player has been online.<br>
	 * This value is returned in milliseconds.
	 * @return The elapsed time since the player logged in as an {@code int}.
	 */
	public int getOnlineTimeMillis()
	{
		return (int) (System.currentTimeMillis() - _onlineBeginTime);
	}
	
	/**
	 * Retrieves the inventory of the player.<br>
	 * This method returns the {@code PlayerInventory} object associated with this character.
	 * @return The {@code PlayerInventory} instance.
	 */
	@Override
	public PlayerInventory getInventory()
	{
		return _inventory;
	}
	
	/**
	 * Removes a specific shortcut from the player's list.<br>
	 * This method uses the provided {@code objectId} to identify which shortcut to delete.
	 * @param objectId The unique identifier of the object to remove from shortcuts.
	 */
	public void removeItemFromShortcut(int objectId)
	{
		_shortcuts.deleteShortcutByObjectId(objectId);
	}
	
	/**
	 * Checks if the character is currently in a sitting state.
	 * @return {@code true} if the character is sitting, {@code false} otherwise.
	 */
	public boolean isSitting()
	{
		return _waitTypeSitting;
	}
	
	/**
	 * Sets whether the actor is currently in a sitting state.<br>
	 * This updates the internal {@code _waitTypeSitting} variable.
	 * @param value The boolean value to set for the sitting state.
	 */
	public void setSitting(boolean value)
	{
		_waitTypeSitting = value;
	}
	
	/**
	 * Retrieves the id of the throne the player sits on.
	 * @return The static object id of the throne, or 0 when the player does not sit on one.
	 */
	public int getThroneId()
	{
		return _throneId;
	}
	
	/**
	 * Sets the throne the player sits on.
	 * @param throneId The static object id of the throne, or 0 when leaving it.
	 */
	public void setThroneId(int throneId)
	{
		_throneId = throneId;
	}
	
	/**
	 * Updates the current sitting status of the actor.<br>
	 * Sets the internal {@code _sittingInProgress} flag to the provided value.
	 * @param value The new sitting state to apply.
	 */
	public void setSittingProgress(boolean value)
	{
		_sittingInProgress = value;
	}
	
	/**
	 * Changes the actor's state to a sitting position.<br>
	 * This method updates the current animation and status of the entity.
	 */
	public void sitDown()
	{
		sitDown(true);
	}
	
	/**
	 * Causes the actor to enter a sitting state.<br>
	 * This method checks if the actor is already sitting or performing restricted actions.<br>
	 * It updates the character status and notifies other players of the change.
	 * @param checkCast Determines whether to prevent sitting while currently casting a spell.
	 */
	public void sitDown(boolean checkCast)
	{
		if (_sittingInProgress)
		{
			return;
		}
		
		if (checkCast && isCastingNow())
		{
			sendMessage("Cannot sit while casting.");
			return;
		}
		
		if (!_waitTypeSitting && !isAttackDisabled() && !isControlBlocked() && !isImmobilized() && !isFishing())
		{
			breakAttack();
			setSitting(true);
			setSittingProgress(true);
			getAI().setIntentionRest();
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
			
			// Schedule a sit down task to wait for the animation to finish.
			ThreadPool.schedule(new SitDownTask(this), 2500);
		}
	}
	
	/**
	 * Causes the actor to stand up from a sitting position.<br>
	 * This method checks if the actor is currently sitting and eligible to stand.<br>
	 * It updates the wait type and schedules an animation task.
	 */
	public void standUp()
	{
		if (_sittingInProgress)
		{
			return;
		}
		
		if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead())
		{
			_throneId = 0;
			setSittingProgress(true);
			if (getEffectList().isAffected(EffectFlag.RELAXING))
			{
				stopEffects(EffectFlag.RELAXING);
			}
			
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			
			// Schedule a stand up task to wait for the animation to finish.
			ThreadPool.schedule(new StandUpTask(this), 2500);
		}
	}
	
	/**
	 * Retrieves the warehouse associated with this player.
	 * @return the {@code PlayerWarehouse} object.
	 */
	public PlayerWarehouse getWarehouse()
	{
		return _warehouse;
	}
	
	/**
	 * Retrieves the freight associated with the player.
	 * @return the {@code PlayerFreight} object.
	 */
	public PlayerFreight getFreight()
	{
		return _freight;
	}
	
	/**
	 * Checks if the player is eligible to receive a refund.<br>
	 * This method verifies that a refund exists and that refunds are enabled in {@code GeneralConfig}.
	 * @return {@code true} if a refund is available, {@code false} otherwise.
	 */
	public boolean hasRefund()
	{
		return (_refund != null) && (_refund.getSize() > 0) && GeneralConfig.ALLOW_REFUND;
	}
	
	/**
	 * Retrieves the {@code PlayerRefund} object for this player.<br>
	 * If the refund object does not exist, it creates a new one.
	 * @return The {@code PlayerRefund} associated with this player.
	 */
	public PlayerRefund getRefund()
	{
		if (_refund == null)
		{
			_refund = new PlayerRefund(this);
		}
		
		return _refund;
	}
	
	/**
	 * Removes the current refund object from the system.<br>
	 * This method deletes the {@code _refund} entity if it exists.<br>
	 * It then sets the {@code _refund} reference to {@code null}.
	 */
	public void clearRefund()
	{
		if (_refund != null)
		{
			_refund.deleteMe();
		}
		
		_refund = null;
	}
	
	/**
	 * Retrieves the current amount of Adena held by the player.<br>
	 * This value is fetched from the {@code Inventory} object.
	 * @return The total amount of Adena as a {@code long}.
	 */
	public long getAdena()
	{
		return _inventory.getAdena();
	}
	
	/**
	 * Retrieves the amount of Ancient Adena held by the player.<br>
	 * This value is fetched from the internal inventory system.
	 * @return the current amount of {@code AncientAdena} as a {@code long}.
	 */
	public long getAncientAdena()
	{
		return _inventory.getAncientAdena();
	}
	
	/**
	 * Retrieves the current number of beauty tickets owned by the player.<br>
	 * This value is fetched from the {@code Inventory} object.
	 * @return The total count of beauty tickets as a {@code long}.
	 */
	public long getBeautyTickets()
	{
		return _inventory.getBeautyTickets();
	}
	
	/**
	 * Adds a specific amount of adena to the player's inventory.<br>
	 * The amount is capped by the maximum allowed limit defined in {@code PlayerConfig}.<br>
	 * It handles sending system messages and updating the client UI based on the result.
	 * @param process The type of process that triggered this adena addition.
	 * @param count The amount of adena to add.
	 * @param reference The object associated with this transaction.
	 * @param sendMessage Whether to send a confirmation or failure message to the player.
	 */
	public void addAdena(ItemProcessType process, long count, WorldObject reference, boolean sendMessage)
	{
		final long currentAdena = _inventory.getAdena();
		final long limitRemaining = PlayerConfig.MAX_ADENA - currentAdena;
		final long amountToAdd = count;
		
		if (count > limitRemaining)
		{
			count = limitRemaining;
		}
		
		if (sendMessage)
		{
			if ((count == 0) && (amountToAdd > 0))
			{
				// Failed adding adena.
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_ACQUIRE_S1_ADENA);
				sm.addLong(amountToAdd);
				sendPacket(sm);
			}
			else
			{
				// Inform player how much adena they received.
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_ADENA_2);
				sm.addLong(count);
				sendPacket(sm);
			}
		}
		
		final long adena = _inventory.getAdena();
		if (count > 0)
		{
			_inventory.addAdena(process, count, this, reference);
			
			// Send update packet.
			if (count == adena)
			{
				sendItemList();
			}
			else
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(_inventory.getAdenaInstance());
				sendInventoryUpdate(iu);
			}
		}
		
		// If the player has reached the adena limit, inform them.
		if ((adena == PlayerConfig.MAX_ADENA) && sendMessage)
		{
			sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_YOUR_OUT_OF_POCKET_ADENA_LIMIT);
		}
	}
	
	/**
	 * Reduces the amount of adena from the player's inventory.<br>
	 * Checks if the player has enough currency before proceeding.<br>
	 * Sends a notification message if requested.
	 * @param process The type of process that is consuming the adena.
	 * @param count The amount of adena to remove.
	 * @param reference The object associated with this transaction.
	 * @param sendMessage Whether to send a confirmation or error message to the player.
	 * @return {@code true} if the reduction was successful, {@code false} otherwise.
	 */
	public boolean reduceAdena(ItemProcessType process, long count, WorldObject reference, boolean sendMessage)
	{
		if (count > _inventory.getAdena())
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
			}
			
			return false;
		}
		
		if (count > 0)
		{
			// The destroyed item is kept, spending the whole stack clears the inventory adena instance.
			final Item adenaItem = _inventory.destroyItemByItemId(process, Inventory.ADENA_ID, count, this, reference);
			if (adenaItem == null)
			{
				return false;
			}
			
			// Send update packet.
			final InventoryUpdate iu = new InventoryUpdate();
			if (adenaItem.getCount() > 0)
			{
				iu.addModifiedItem(adenaItem);
			}
			else
			{
				iu.addRemovedItem(adenaItem);
			}
			
			sendInventoryUpdate(iu);
			
			if (sendMessage)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_SPENT_S1_ADENA);
				sm.addLong(count);
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Reduces the number of beauty tickets in the inventory.<br>
	 * Checks if the requested amount is available before processing.<br>
	 * Sends a notification message to the player if {@code sendMessage} is {@code true}.
	 * @param process The type of process being performed.
	 * @param count The number of beauty tickets to remove.
	 * @param reference The object used as a reference for the transaction.
	 * @param sendMessage Whether to send a confirmation message to the player.
	 * @return {@code true} if the reduction was successful, {@code false} otherwise.
	 */
	public boolean reduceBeautyTickets(ItemProcessType process, long count, WorldObject reference, boolean sendMessage)
	{
		if (count > _inventory.getBeautyTickets())
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		if (count > 0)
		{
			if (!_inventory.reduceBeautyTickets(process, count, this, reference))
			{
				return false;
			}
			
			// Send update packet.
			final Item beautyTickets = _inventory.getBeautyTicketsInstance();
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(beautyTickets);
			sendInventoryUpdate(iu);
			
			if (sendMessage)
			{
				if (count > 1)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_X_S2_DISAPPEARED);
					sm.addItemName(Inventory.BEAUTY_TICKET_ID);
					sm.addLong(count);
					sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
					sm.addItemName(Inventory.BEAUTY_TICKET_ID);
					sendPacket(sm);
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Adds a specific amount of Ancient Adena to the player's inventory.<br>
	 * This method handles both the internal data update and optional notifications.<br>
	 * If {@code sendMessage} is {@code true}, it sends a system message to the player.<br>
	 * It also triggers an inventory update packet if the count is greater than {@code 0}.
	 * @param process The type of process that triggered this addition.
	 * @param count The amount of Ancient Adena to add.
	 * @param reference The object used as a reference for the transaction.
	 * @param sendMessage Whether to send a system message to the player.
	 */
	public void addAncientAdena(ItemProcessType process, long count, WorldObject reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
			sm.addItemName(Inventory.ANCIENT_ADENA_ID);
			sm.addLong(count);
			sendPacket(sm);
		}
		
		if (count > 0)
		{
			_inventory.addAncientAdena(process, count, this, reference);
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(_inventory.getAncientAdenaInstance());
			sendInventoryUpdate(iu);
		}
	}
	
	/**
	 * Reduces the amount of Ancient Adena in the player's inventory.<br>
	 * Checks if the player has enough currency before processing the reduction.<br>
	 * Updates the inventory and sends a notification to the client if required.
	 * @param process The type of process being performed.
	 * @param count The amount of Ancient Adena to remove.
	 * @param reference A reference object for the transaction.
	 * @param sendMessage Whether to send a system message to the player.
	 * @return {@code true} if the reduction was successful, {@code false} otherwise.
	 */
	public boolean reduceAncientAdena(ItemProcessType process, long count, WorldObject reference, boolean sendMessage)
	{
		if (count > _inventory.getAncientAdena())
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
			}
			
			return false;
		}
		
		if (count > 0)
		{
			// Keep the item reference, paying the exact amount destroys the stack.
			final Item ancientAdenaItem = _inventory.getAncientAdenaInstance();
			if (!_inventory.reduceAncientAdena(process, count, this, reference))
			{
				return false;
			}
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(ancientAdenaItem);
			sendInventoryUpdate(iu);
			
			if (sendMessage)
			{
				if (count > 1)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_X_S2_DISAPPEARED);
					sm.addItemName(Inventory.ANCIENT_ADENA_ID);
					sm.addLong(count);
					sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
					sm.addItemName(Inventory.ANCIENT_ADENA_ID);
					sendPacket(sm);
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Adds an {@code Item} to the inventory based on the specified {@code ItemProcessType}.<br>
	 * This method handles system messages, capacity checks, and special item logic.
	 * @param process The type of process used to add the item.
	 * @param item The {@code Item} object to be added.
	 * @param reference The {@code WorldObject} providing context for the addition.
	 * @param sendMessage Whether to send a notification message to the player.
	 */
	public void addItem(ItemProcessType process, Item item, WorldObject reference, boolean sendMessage)
	{
		final long itemCount = item.getCount();
		if (itemCount > 0)
		{
			// Sends message to client if requested.
			if (sendMessage)
			{
				if (itemCount > 1)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
					sm.addItemName(item);
					sm.addLong(itemCount);
					sendPacket(sm);
				}
				else if (item.isEnchanted())
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_S2);
					sm.addInt(item.getEnchantLevel());
					sm.addItemName(item);
					sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1);
					sm.addItemName(item);
					sendPacket(sm);
				}
			}
			
			// Add the item to inventory.
			final Item newitem = _inventory.addItem(process, item, this, reference);
			
			// If over capacity, drop the item.
			if (!isGM() && !_inventory.validateCapacity(0, item.isQuestItem(), item.isArtifactItem()) && newitem.isDropable() && (!newitem.isStackable() || (newitem.getLastChange() != Item.MODIFIED)))
			{
				dropItem(ItemProcessType.DROP, newitem, null, true, true);
			}
			else if (CursedWeaponsManager.getInstance().isCursed(newitem.getId()))
			{
				CursedWeaponsManager.getInstance().activate(this, newitem);
			}
			else if (FortSiegeManager.getInstance().isCombat(item.getId()) && FortSiegeManager.getInstance().activateCombatFlag(this, item))
			{
				final Fort fort = FortManager.getInstance().getFort(this);
				fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.C1_HAS_ACQUIRED_THE_FLAG), _name);
			}
		}
	}
	
	/**
	 * Adds an item to the player's inventory.<br>
	 * This method handles the logic for processing the addition.
	 * @param process The type of process used to add the item.
	 * @param itemId The unique identifier of the item.
	 * @param count The amount of the item to add.
	 * @param reference A {@code WorldObject} reference for the source or target.
	 * @param sendMessage Whether to send a notification message to the player.
	 * @return The resulting {@code Item} object after addition.
	 */
	public Item addItem(ItemProcessType process, int itemId, long count, WorldObject reference, boolean sendMessage)
	{
		return addItem(process, itemId, count, -1, reference, sendMessage);
	}
	
	/**
	 * Adds an item to the player's inventory based on the provided details.<br>
	 * This method handles messages, auto-use for herbs, and enchantment levels.<br>
	 * If the inventory is full, the item may be dropped on the ground.
	 * @param process The type of process used to add the item.
	 * @param itemId The unique identifier of the item template.
	 * @param count The amount of the item to add.
	 * @param enchantLevel The enchantment level to apply to the new item.
	 * @param reference A reference object for the inventory operation.
	 * @param sendMessage Whether to send a system message to the player upon receiving the item.
	 * @return The created {@code Item} object, or {@code null} if the addition failed.
	 */
	public Item addItem(ItemProcessType process, int itemId, long count, int enchantLevel, WorldObject reference, boolean sendMessage)
	{
		if (count > 0)
		{
			final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
			if (item == null)
			{
				LOGGER.severe("Item doesn't exist so cannot be added. Item ID: " + itemId);
				return null;
			}
			
			// Sends message to client if requested.
			final boolean hasExImmediateEffect = item.hasExImmediateEffect();
			if (sendMessage && ((!isCastingNow() && hasExImmediateEffect) || !hasExImmediateEffect))
			{
				if (count > 1)
				{
					if ((process == ItemProcessType.SWEEP) || (process == ItemProcessType.QUEST))
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
						sm.addItemName(itemId);
						sm.addLong(count);
						sendPacket(sm);
					}
					else
					{
						final SystemMessage sm;
						if (enchantLevel > 0)
						{
							sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_S2_X_S3);
							sm.addInt(enchantLevel);
						}
						else
						{
							sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
						}
						
						sm.addItemName(itemId);
						sm.addLong(count);
						sendPacket(sm);
					}
				}
				else if ((process == ItemProcessType.SWEEP) || (process == ItemProcessType.QUEST))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_2);
					sm.addItemName(itemId);
					sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1);
					sm.addItemName(itemId);
					sendPacket(sm);
				}
			}
			
			// Auto-use herbs.
			if (hasExImmediateEffect && item.isEtcItem())
			{
				for (SkillHolder skillHolder : item.getAllSkills())
				{
					SkillCaster.triggerCast(this, null, skillHolder.getSkill(), null, false);
				}
				
				broadcastInfo();
			}
			else
			{
				// Add the item to inventory.
				final Item createdItem = _inventory.addItem(process, itemId, count, this, reference);
				if (enchantLevel > -1)
				{
					createdItem.setEnchantLevel(enchantLevel);
					
					// Update with an InventoryUpdate that has the new enchant value.
					final InventoryUpdate playerIU = new InventoryUpdate();
					if (createdItem.isStackable() && (createdItem.getCount() > count))
					{
						playerIU.addModifiedItem(createdItem);
					}
					else
					{
						playerIU.addNewItem(createdItem);
					}
					
					sendInventoryUpdate(playerIU);
				}
				
				// If over capacity, drop the item.
				if (!isGM() && !_inventory.validateCapacity(0, item.isQuestItem(), item.isArtifactItem()) && createdItem.isDropable() && (!createdItem.isStackable() || (createdItem.getLastChange() != Item.MODIFIED)))
				{
					dropItem(ItemProcessType.DROP, createdItem, null, true);
				}
				else if (CursedWeaponsManager.getInstance().isCursed(createdItem.getId()))
				{
					CursedWeaponsManager.getInstance().activate(this, createdItem);
				}
				
				return createdItem;
			}
		}
		
		return null;
	}
	
	/**
	 * Adds an item to the specified object.<br>
	 * This method handles the logic for processing the addition.<br>
	 * It can optionally send a message to the player.
	 * @param process The type of process used to add the item.
	 * @param item The {@code ItemHolder} containing the item data.
	 * @param reference The {@code WorldObject} that will receive the item.
	 * @param sendMessage A boolean indicating if a message should be sent.
	 */
	public void addItem(ItemProcessType process, ItemHolder item, WorldObject reference, boolean sendMessage)
	{
		addItem(process, item.getId(), item.getCount(), reference, sendMessage);
	}
	
	/**
	 * Removes an {@code Item} from the world.<br>
	 * This method handles the destruction logic based on the provided process type.
	 * @param process The type of process used to destroy the item.
	 * @param item The {@code Item} object to be removed.
	 * @param reference The {@code WorldObject} that owns or holds the item.
	 * @param sendMessage Whether to send a confirmation message to the player.
	 * @return {@code true} if the item was successfully destroyed, {@code false} otherwise.
	 */
	public boolean destroyItem(ItemProcessType process, Item item, WorldObject reference, boolean sendMessage)
	{
		return destroyItem(process, item, item.getCount(), reference, sendMessage);
	}
	
	/**
	 * Removes a specific quantity of an item from the inventory.<br>
	 * This method handles the removal logic and updates the client.<br>
	 * It returns {@code false} if the item could not be destroyed.
	 * @param process The type of process used to destroy the item.
	 * @param item The {@code Item} object to be removed.
	 * @param count The amount of the item to remove.
	 * @param reference The {@code WorldObject} associated with this action.
	 * @param sendMessage Whether to send a confirmation message to the player.
	 * @return {@code true} if the destruction was successful, {@code false} otherwise.
	 */
	public boolean destroyItem(ItemProcessType process, Item item, long count, WorldObject reference, boolean sendMessage)
	{
		final Item destoyedItem = _inventory.destroyItem(process, item, count, this, reference);
		if (destoyedItem == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		// Send inventory update packet.
		final InventoryUpdate playerIU = new InventoryUpdate();
		if (destoyedItem.isStackable() && (destoyedItem.getCount() > 0))
		{
			playerIU.addModifiedItem(destoyedItem);
		}
		else
		{
			playerIU.addRemovedItem(destoyedItem);
		}
		
		sendInventoryUpdate(playerIU);
		
		// Sends message to client if requested.
		if (sendMessage)
		{
			if (count > 1)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_X_S2_DISAPPEARED);
				sm.addItemName(destoyedItem);
				sm.addLong(count);
				sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(destoyedItem);
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Removes a specific quantity of an item from the inventory.<br>
	 * This method checks if the item exists and has enough count before destruction.<br>
	 * It sends an error message to the player if the operation fails due to insufficient quantity.
	 * @param process The type of process used for destroying the item.
	 * @param objectId The unique identifier of the item in the inventory.
	 * @param count The amount of the item to remove.
	 * @param reference The {@code WorldObject} associated with this action.
	 * @param sendMessage Whether to send a system message if the destruction fails.
	 * @return {@code true} if the item was successfully destroyed, {@code false} otherwise.
	 */
	@Override
	public boolean destroyItem(ItemProcessType process, int objectId, long count, WorldObject reference, boolean sendMessage)
	{
		final Item item = _inventory.getItemByObjectId(objectId);
		if ((item == null) || (item.getCount() < count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		return destroyItem(process, item, count, reference, sendMessage);
	}
	
	/**
	 * Removes a specific quantity of an item from the inventory.<br>
	 * This method handles both stackable and non-stackable items.<br>
	 * It also manages special cases like Adena currency.<br>
	 * Returns true if the destruction was successful.
	 * @param process The type of process that triggered the destruction.
	 * @param itemId The unique identifier of the item to remove.
	 * @param count The amount of the item to destroy.
	 * @param reference The object used as a reference for the action.
	 * @param sendMessage Whether to notify the player with a system message.
	 * @return {@code true} if the item was successfully removed, {@code false} otherwise.
	 */
	@Override
	public boolean destroyItemByItemId(ItemProcessType process, int itemId, long count, WorldObject reference, boolean sendMessage)
	{
		if (itemId == Inventory.ADENA_ID)
		{
			return reduceAdena(process, count, reference, sendMessage);
		}
		
		final Item item = _inventory.getItemByItemId(itemId);
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		final boolean isStackable = item.isStackable();
		final long itemCount = isStackable ? item.getCount() : _inventory.getInventoryItemCount(itemId, -1);
		final long removeCount = count < 0 ? itemCount : count;
		if ((removeCount <= 0) || (itemCount < removeCount) || (_inventory.destroyItemByItemId(process, itemId, removeCount, this, reference) == null))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		// Send inventory update packet.
		final InventoryUpdate playerIU = new InventoryUpdate();
		if (isStackable && (itemCount > 0) && (itemCount != removeCount))
		{
			playerIU.addModifiedItem(item);
		}
		else
		{
			playerIU.addRemovedItem(item);
		}
		
		sendInventoryUpdate(playerIU);
		
		// Einhasad coin UI update.
		if (item.getId() == Inventory.EINHASAD_COIN_ID)
		{
			sendPacket(new ExBloodyCoinCount(this));
		}
		
		// Sends message to client if requested.
		if (sendMessage)
		{
			if (removeCount > 1)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_X_S2_DISAPPEARED);
				sm.addItemName(itemId);
				sm.addLong(removeCount);
				sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(itemId);
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Transfers an item from the current inventory to a target inventory.<br>
	 * This method validates the source item and handles network updates for both inventories.
	 * @param process The type of transfer process being performed.
	 * @param objectId The unique identifier of the item to move.
	 * @param count The quantity of the item to transfer.
	 * @param target The {@code Inventory} where the item will be moved.
	 * @param reference A {@code WorldObject} used as a reference for the transaction.
	 * @return The resulting {@code Item} after the transfer, or {@code null} if the operation failed.
	 */
	public Item transferItem(ItemProcessType process, int objectId, long count, Inventory target, WorldObject reference)
	{
		final Item oldItem = checkItemManipulation(objectId, count, "transfer");
		if (oldItem == null)
		{
			return null;
		}
		
		final Item newItem = _inventory.transferItem(process, objectId, count, target, this, reference);
		if (newItem == null)
		{
			return null;
		}
		
		// Send inventory update packet.
		final InventoryUpdate playerIU = new InventoryUpdate();
		if ((oldItem.getCount() > 0) && (oldItem != newItem))
		{
			playerIU.addModifiedItem(oldItem);
		}
		else
		{
			playerIU.addRemovedItem(oldItem);
		}
		
		sendInventoryUpdate(playerIU);
		
		// Send target update packet.
		if (target instanceof PlayerInventory)
		{
			final Player targetPlayer = ((PlayerInventory) target).getOwner();
			final InventoryUpdate targetIU = new InventoryUpdate();
			if (newItem.getCount() > count)
			{
				targetIU.addModifiedItem(newItem);
			}
			else
			{
				targetIU.addNewItem(newItem);
			}
			
			targetPlayer.sendInventoryUpdate(targetIU);
		}
		
		// Einhasad coin UI update.
		if (newItem.getId() == Inventory.EINHASAD_COIN_ID)
		{
			sendPacket(new ExBloodyCoinCount(this));
		}
		
		return newItem;
	}
	
	/**
	 * Exchanges items based on a specific process and cost.<br>
	 * This method checks for inventory space and weight limits before processing the trade.<br>
	 * It removes the cost item and adds the reward item if successful.
	 * @param process The type of process to perform during the exchange.
	 * @param reference The {@code WorldObject} associated with the transaction.
	 * @param coinId The unique identifier for the currency or item being spent.
	 * @param cost The amount of the cost item to be removed.
	 * @param rewardId The unique identifier for the item to be received.
	 * @param count The quantity of the reward item to be added.
	 * @param sendMessage Whether to send a system message to the player on failure.
	 * @return {@code true} if the exchange was successful, {@code false} otherwise.
	 */
	public boolean exchangeItemsById(ItemProcessType process, WorldObject reference, int coinId, long cost, int rewardId, long count, boolean sendMessage)
	{
		if (!_inventory.validateCapacityByItemId(rewardId, count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			}
			
			return false;
		}
		
		if (!_inventory.validateWeightByItemId(rewardId, count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.WEIGHT_LIMIT_IS_EXCEEDED);
			}
			
			return false;
		}
		
		if (destroyItemByItemId(process, coinId, cost, reference, sendMessage))
		{
			addItem(process, rewardId, count, reference, sendMessage);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Drops an item from the inventory into the world.<br>
	 * This method handles spawning the item, applying auto-destroy rules, and updating the client.
	 * @param process The type of process used for dropping the item.
	 * @param item The {@code Item} object to be dropped.
	 * @param reference The {@code WorldObject} that serves as a spatial reference for the drop.
	 * @param sendMessage Whether to send a confirmation message to the player.
	 * @param protectItem Whether to apply retail-style drop protection to the item.
	 * @return {@code true} if the item was successfully dropped, {@code false} otherwise.
	 */
	public boolean dropItem(ItemProcessType process, Item item, WorldObject reference, boolean sendMessage, boolean protectItem)
	{
		final Item droppedItem = _inventory.dropItem(process, item, this, reference);
		if (droppedItem == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		droppedItem.dropMe(this, (getX() + Rnd.get(50)) - 25, (getY() + Rnd.get(50)) - 25, getZ() + 20);
		if ((GeneralConfig.AUTODESTROY_ITEM_AFTER > 0) && GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM && !GeneralConfig.LIST_PROTECTED_ITEMS.contains(droppedItem.getId()) && ((droppedItem.isEquipable() && GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM) || !droppedItem.isEquipable()))
		{
			ItemsAutoDestroyTaskManager.getInstance().addItem(droppedItem);
		}
		
		// Protection against auto destroy dropped item.
		if (GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM)
		{
			droppedItem.setProtected(droppedItem.isEquipable() && (!droppedItem.isEquipable() || !GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM));
		}
		else
		{
			droppedItem.setProtected(true);
		}
		
		// Retail drop protection.
		if (protectItem)
		{
			droppedItem.getDropProtection().protect(this);
		}
		
		// Send inventory update packet.
		final InventoryUpdate playerIU = new InventoryUpdate();
		playerIU.addItem(droppedItem);
		sendInventoryUpdate(playerIU);
		
		// Sends message to client if requested.
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_DROPPED_S1);
			sm.addItemName(droppedItem);
			sendPacket(sm);
		}
		
		// Einhasad coin UI update.
		if (item.getId() == Inventory.EINHASAD_COIN_ID)
		{
			sendPacket(new ExBloodyCoinCount(this));
		}
		
		return true;
	}
	
	/**
	 * Drops an item into the world based on the specified process.<br>
	 * This method handles the logic for placing the item and optionally sending a message.
	 * @param process The type of process used to drop the item.
	 * @param item The {@code Item} object to be dropped.
	 * @param reference The {@code WorldObject} that serves as the reference point.
	 * @param sendMessage A boolean indicating whether a message should be sent to the player.
	 * @return {@code true} if the item was successfully dropped, {@code false} otherwise.
	 */
	public boolean dropItem(ItemProcessType process, Item item, WorldObject reference, boolean sendMessage)
	{
		return dropItem(process, item, reference, sendMessage, false);
	}
	
	/**
	 * Drops an item from the inventory into the game world.<br>
	 * This method handles position placement, auto-destruction logic, and protection status.<br>
	 * It also updates the player's inventory and sends notifications if requested.
	 * @param process The type of process used for dropping the item.
	 * @param objectId The unique identifier of the item to drop.
	 * @param count The quantity of the item to drop.
	 * @param x The X coordinate where the item will land.
	 * @param y The Y coordinate where the item will land.
	 * @param z The Z coordinate where the item will land.
	 * @param reference A reference object used during the drop process.
	 * @param sendMessage Whether to send a system message to the player upon dropping.
	 * @param protectItem Whether to apply specific drop protection to the item.
	 * @return The dropped {@code Item} object, or {@code null} if the drop failed.
	 */
	public Item dropItem(ItemProcessType process, int objectId, long count, int x, int y, int z, WorldObject reference, boolean sendMessage, boolean protectItem)
	{
		final Item invitem = _inventory.getItemByObjectId(objectId);
		final Item item = _inventory.dropItem(process, objectId, count, this, reference);
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return null;
		}
		
		item.dropMe(this, x, y, z);
		if ((GeneralConfig.AUTODESTROY_ITEM_AFTER > 0) && GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM && !GeneralConfig.LIST_PROTECTED_ITEMS.contains(item.getId()) && ((item.isEquipable() && GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable()))
		{
			ItemsAutoDestroyTaskManager.getInstance().addItem(item);
		}
		
		if (GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM)
		{
			item.setProtected(item.isEquipable() && (!item.isEquipable() || !GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM));
		}
		else
		{
			item.setProtected(true);
		}
		
		// Retail drop protection.
		if (protectItem)
		{
			item.getDropProtection().protect(this);
		}
		
		// Send inventory update packet.
		final InventoryUpdate playerIU = new InventoryUpdate();
		playerIU.addItem(invitem);
		sendInventoryUpdate(playerIU);
		
		// Sends message to client if requested.
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}
		
		// Einhasad coin UI update.
		if (item.getId() == Inventory.EINHASAD_COIN_ID)
		{
			sendPacket(new ExBloodyCoinCount(this));
		}
		
		return item;
	}
	
	/**
	 * Validates if an action can be performed on a specific item.<br>
	 * This method checks for ownership, stackability, and current status.<br>
	 * It also prevents actions on active pets, mounts, or items being processed.
	 * @param objectId The unique identifier of the item to check.
	 * @param count The quantity of the item involved in the action.
	 * @param action A string describing the type of action being performed.
	 * @return The {@code Item} if the action is valid, or {@code null} if it fails any checks.
	 */
	public Item checkItemManipulation(int objectId, long count, String action)
	{
		// TODO: if we remove objects that are not visible from the World, we'll have to remove this check
		if (World.findObject(objectId) == null)
		{
			LOGGER.finest(getObjectId() + ": player tried to " + action + " item not available in World");
			return null;
		}
		
		final Item item = _inventory.getItemByObjectId(objectId);
		if ((item == null) || (item.getOwnerId() != getObjectId()))
		{
			LOGGER.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return null;
		}
		
		if ((count < 0) || ((count > 1) && !item.isStackable()))
		{
			LOGGER.finest(getObjectId() + ": player tried to " + action + " item with invalid count: " + count);
			return null;
		}
		
		if (count > item.getCount())
		{
			LOGGER.finest(getObjectId() + ": player tried to " + action + " more items than he owns");
			return null;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting.
		if (((_pet != null) && (_pet.getControlObjectId() == objectId)) || (_mountObjectID == objectId))
		{
			return null;
		}
		
		if (isProcessingItem(objectId))
		{
			return null;
		}
		
		// We cannot put a Weapon with Augmentation in WH while casting (Possible Exploit).
		if (item.isAugmented() && isCastingNow())
		{
			return null;
		}
		
		return item;
	}
	
	/**
	 * Checks if the player is currently under spawn protection.<br>
	 * This returns {@code true} if the protection period has not expired.<br>
	 * It compares the current system time against the stored end time.
	 * @return {@code true} if protected, {@code false} otherwise.
	 */
	public boolean isSpawnProtected()
	{
		return (_spawnProtectEndTime != 0) && (_spawnProtectEndTime > System.currentTimeMillis());
	}
	
	/**
	 * Checks if the player is currently protected from teleportation.<br>
	 * This returns {@code true} if the protection period has not expired.<br>
	 * It compares the current system time against the stored end time.
	 * @return {@code true} if the player is still protected, {@code false} otherwise.
	 */
	public boolean isTeleportProtected()
	{
		return (_teleportProtectEndTime != 0) && (_teleportProtectEndTime > System.currentTimeMillis());
	}
	
	/**
	 * Enables or disables the spawn protection for the player.<br>
	 * If {@code true}, it sets a protection period based on the configuration.<br>
	 * If {@code false}, it removes any active protection.
	 * @param protect The boolean value to set the protection status.
	 */
	public void setSpawnProtection(boolean protect)
	{
		_spawnProtectEndTime = protect ? System.currentTimeMillis() + (PlayerConfig.PLAYER_SPAWN_PROTECTION * 1000) : 0;
	}
	
	/**
	 * Enables or disables teleport protection for the player.<br>
	 * If {@code true}, it sets a protection timer based on the configuration.<br>
	 * If {@code false}, it removes any active protection.
	 * @param protect The boolean value to set the protection status.
	 */
	public void setTeleportProtection(boolean protect)
	{
		_teleportProtectEndTime = protect ? System.currentTimeMillis() + (PlayerConfig.PLAYER_TELEPORT_PROTECTION * 1000) : 0;
	}
	
	/**
	 * Sets the expiration time for the recent fake death protection.<br>
	 * If {@code protect} is {@code true}, it calculates a new end time based on configuration.<br>
	 * If {@code protect} is {@code false}, it sets the end time to {@code 0}.
	 * @param protect Determines whether to apply the fake death protection period.
	 */
	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeTaskManager.getInstance().getGameTicks() + (PlayerConfig.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeTaskManager.TICKS_PER_SECOND) : 0;
	}
	
	/**
	 * Checks if the character is currently in a fake death state.<br>
	 * It compares the last fake death end time against the current game ticks.
	 * @return {@code true} if the fake death period has not ended yet, {@code false} otherwise.
	 */
	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	/**
	 * Checks if the character is currently under a fake death effect.<br>
	 * This method verifies if the {@code EffectFlag.FAKE_DEATH} flag is active.
	 * @return {@code true} if the character is in a fake death state, {@code false} otherwise.
	 */
	public boolean isFakeDeath()
	{
		return isAffected(EffectFlag.FAKE_DEATH);
	}
	
	/**
	 * Returns the object ids of the monsters that saw through the current fake death and keep attacking.
	 * @return The live set of monster object ids, refilled each time fake death starts.
	 */
	public Set<Integer> getFakeDeathWatchers()
	{
		return _fakeDeathWatchers;
	}
	
	/**
	 * Checks if the given creature saw through the current fake death of this player.
	 * @param creature The creature to check.
	 * @return {@code true} if this player is faking death and the creature keeps attacking regardless.
	 */
	public boolean isFakeDeathSeenBy(Creature creature)
	{
		return isFakeDeath() && _fakeDeathWatchers.contains(creature.getObjectId());
	}
	
	/**
	 * Checks if the actor is currently in a dead state.<br>
	 * This includes both standard death and fake death states.
	 * @return {@code true} if the actor is dead or faking death, {@code false} otherwise.
	 */
	@Override
	public boolean isAlikeDead()
	{
		return super.isAlikeDead() || isFakeDeath();
	}
	
	/**
	 * Retrieves the current {@code GameClient} instance.<br>
	 * This method provides access to the client associated with this object.
	 * @return The current {@code GameClient} object.
	 */
	public GameClient getClient()
	{
		return _client;
	}
	
	/**
	 * Sets the {@code GameClient} associated with this actor.<br>
	 * This method also updates the internal IP address if a valid one exists in the provided client.
	 * @param client The {@code GameClient} to assign.
	 */
	public void setClient(GameClient client)
	{
		_client = client;
		if (_client != null)
		{
			final String ip = _client.getIp();
			if (ip != null)
			{
				_ip = ip;
			}
		}
	}
	
	/**
	 * Retrieves the IP address of the current actor.<br>
	 * This method returns the value stored in the internal {@code _ip} field.
	 * @return The IP address as a {@code String}.
	 */
	public String getIPAddress()
	{
		return _ip;
	}
	
	/**
	 * Retrieves the current world position of the skill.<br>
	 * This method returns the {@code Location} where the skill is currently active.
	 * @return the current {@code Location} of the skill.
	 */
	public Location getCurrentSkillWorldPosition()
	{
		return _currentSkillWorldPosition;
	}
	
	/**
	 * Updates the current world position of a skill.<br>
	 * This method stores the {@code Location} provided in the {@code _currentSkillWorldPosition} field.
	 * @param worldPosition The new {@code Location} to set for the skill.
	 */
	public void setCurrentSkillWorldPosition(Location worldPosition)
	{
		_currentSkillWorldPosition = worldPosition;
	}
	
	/**
	 * Enables a specific skill for the actor.<br>
	 * This method also handles optional timestamp removal.
	 * @param skill The {@code Skill} object to enable.
	 * @param removeTimeStamp Set to {@code true} to remove the skill's timestamp, or {@code false} to keep it.
	 */
	public void enableSkill(Skill skill, boolean removeTimeStamp)
	{
		super.enableSkill(skill);
		if (removeTimeStamp)
		{
			removeTimeStamp(skill);
		}
	}
	
	/**
	 * Activates a specific skill for the actor.<br>
	 * This method updates the internal state to allow the skill to be used.
	 * @param skill The {@code Skill} object to enable.
	 */
	@Override
	public void enableSkill(Skill skill)
	{
		enableSkill(skill, true);
	}
	
	/**
	 * Checks if the current CP value requires an update.<br>
	 * It determines if the CP has moved outside of its current update range.<br>
	 * This helps manage how frequently the CP bar is refreshed for the player.
	 * @return {@code true} if the CP needs to be updated, {@code false} otherwise.
	 */
	private boolean needCpUpdate()
	{
		final double currentCp = getCurrentCp();
		if ((currentCp <= 1.0) || (getMaxCp() < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentCp <= _cpUpdateDecCheck) || (currentCp >= _cpUpdateIncCheck))
		{
			if (currentCp == getMaxCp())
			{
				_cpUpdateIncCheck = currentCp + 1;
				_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentCp / _cpUpdateInterval;
				int intMulti = (int) doubleMulti;
				_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti - 1 : intMulti);
				_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if the current MP value requires an update to the UI bar.<br>
	 * It determines if the mana level has crossed a specific threshold.<br>
	 * This helps optimize how often the mana bar is refreshed for the player.
	 * @return {@code true} if the mana bar needs to be updated, {@code false} otherwise.
	 */
	private boolean needMpUpdate()
	{
		final double currentMp = getCurrentMp();
		if ((currentMp <= 1.0) || (getMaxMp() < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentMp <= _mpUpdateDecCheck) || (currentMp >= _mpUpdateIncCheck))
		{
			if (currentMp == getMaxMp())
			{
				_mpUpdateIncCheck = currentMp + 1;
				_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentMp / _mpUpdateInterval;
				int intMulti = (int) doubleMulti;
				_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti - 1 : intMulti);
				_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Schedules a task to broadcast the status updates of this entity.<br>
	 * It calculates changes for stats like HP, MP, and CP.<br>
	 * The update is sent to relevant players based on visibility or party membership.
	 * @param caster The {@code Creature} that performed the action causing the update.
	 */
	@Override
	public void broadcastStatusUpdate(Creature caster)
	{
		if (_broadcastStatusUpdateTask == null)
		{
			_broadcastStatusUpdateTask = ThreadPool.schedule(() ->
			{
				final StatusUpdate su = new StatusUpdate(this);
				computeStatusUpdate(su, StatusUpdateType.LEVEL);
				computeStatusUpdate(su, StatusUpdateType.MAX_HP);
				computeStatusUpdate(su, StatusUpdateType.CUR_HP);
				computeStatusUpdate(su, StatusUpdateType.MAX_MP);
				computeStatusUpdate(su, StatusUpdateType.CUR_MP);
				computeStatusUpdate(su, StatusUpdateType.MAX_CP);
				computeStatusUpdate(su, StatusUpdateType.CUR_CP);
				
				final Party party = getParty();
				final boolean needCpUpdate = needCpUpdate();
				final boolean needHpUpdate = needHpUpdate();
				final boolean needMpUpdate = needMpUpdate();
				
				if (su.hasUpdates())
				{
					// Skill was involved.
					if (caster != null)
					{
						su.addCaster(caster);
						broadcastPacket(su);
					}
					else if (needCpUpdate || needHpUpdate || needMpUpdate) // CP-HP-MP regeneration.
					{
						su.sendInBroadcast();
						sendPacket(su);
						
						World.forEachVisibleObject(this, Player.class, player ->
						{
							if (isVisibleFor(player) && ((player.getTarget() == this) || ((party != null) && (player.getParty() == party))))
							{
								player.sendPacket(su);
							}
						});
					}
				}
				
				// Check if a party is in progress and party window update is useful.
				if ((party != null) && (needCpUpdate || needHpUpdate || needMpUpdate))
				{
					final PartySmallWindowUpdate partyWindow = new PartySmallWindowUpdate(this, false);
					if (needCpUpdate)
					{
						partyWindow.addComponentType(PartySmallWindowUpdateType.CURRENT_CP);
						partyWindow.addComponentType(PartySmallWindowUpdateType.MAX_CP);
					}
					
					if (needHpUpdate)
					{
						partyWindow.addComponentType(PartySmallWindowUpdateType.CURRENT_HP);
						partyWindow.addComponentType(PartySmallWindowUpdateType.MAX_HP);
					}
					
					if (needMpUpdate)
					{
						partyWindow.addComponentType(PartySmallWindowUpdateType.CURRENT_MP);
						partyWindow.addComponentType(PartySmallWindowUpdateType.MAX_MP);
					}
					
					party.broadcastToPartyMembers(this, partyWindow);
				}
				
				if (_inOlympiadMode && _olympiadStart && (needCpUpdate || needHpUpdate))
				{
					final OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(getOlympiadGameId());
					if ((game != null) && game.isBattleStarted())
					{
						game.getStadium().broadcastStatusUpdate(this);
					}
				}
				
				// In duel MP updated only with CP or HP.
				if (_isInDuel && (needCpUpdate || needHpUpdate))
				{
					DuelManager.getInstance().broadcastToOppositTeam(this, new ExDuelUpdateUserInfo(this));
				}
				
				_broadcastStatusUpdateTask = null;
			}, 50);
		}
	}
	
	/**
	 * Updates and broadcasts information about the user.<br>
	 * This method calls {@code updateUserInfo()} to refresh local data.<br>
	 * It then calls {@code broadcastCharInfo()} to notify known players.
	 */
	public void broadcastUserInfo()
	{
		// Send user info to the current player.
		updateUserInfo();
		
		// Broadcast char info to known players.
		broadcastCharInfo();
	}
	
	/**
	 * Updates the user information for the current player.<br>
	 * This method sends a {@code UserInfo} packet to the client.
	 */
	public void updateUserInfo()
	{
		sendPacket(new UserInfo(this));
		sendPacket(new ExUserViewInfoParameter(this));
		sendPacket(new ExUserInfoAddPDefend(this));
		sendPacket(new ExUserInfoAddMDefend(this));
	}
	
	/**
	 * Schedules a trailing update for user information changed by effects.<br>
	 * Repeated effect changes within the delay are combined into one update.
	 */
	public void scheduleEffectUserInfoUpdate()
	{
		if (_effectUserInfoUpdateTask == null)
		{
			_effectUserInfoUpdateTask = ThreadPool.schedule(() ->
			{
				updateUserInfo();
				_effectUserInfoUpdateTask = null;
			}, 3000);
		}
	}
	
	/**
	 * Sends user information to the current player.<br>
	 * This method also broadcasts character information to all known players.
	 * @param types The types of user information components to include in the packet.
	 */
	public void broadcastUserInfo(UserInfoType... types)
	{
		// Send user info to the current player.
		final UserInfo ui = new UserInfo(this, false);
		ui.addComponentType(types);
		sendPacket(ui);
		
		// Broadcast char info to all known players.
		broadcastCharInfo();
	}
	
	/**
	 * Sends the character information to all nearby players.<br>
	 * This method schedules a task to broadcast {@code CharInfo} packets.<br>
	 * It also updates and synchronizes player relations with visible entities.
	 */
	public void broadcastCharInfo()
	{
		// Client is disconnected.
		if (isOnlineInt() == 0)
		{
			return;
		}
		
		if (_broadcastCharInfoTask == null)
		{
			_broadcastCharInfoTask = ThreadPool.schedule(() ->
			{
				if (!_freeLook)
				{
					final ExCharInfo charInfo = new ExCharInfo(this, false);
					charInfo.sendInBroadcast();
					World.forEachVisibleObject(this, Player.class, player ->
					{
						if (isVisibleFor(player))
						{
							if (isInvisible() && isGM())
							{
								player.sendPacket(new ExCharInfo(this, true));
							}
							else
							{
								player.sendPacket(charInfo);
							}
							
							// Update relation.
							final long relation = getRelation(player);
							final boolean isAutoAttackable = isAutoAttackable(player);
							final int objectId = player.getObjectId();
							final RelationCache oldrelation = getKnownRelations().get(objectId);
							if ((oldrelation == null) || (oldrelation.getRelation() != relation) || (oldrelation.isAutoAttackable() != isAutoAttackable))
							{
								final RelationChanged rc = new RelationChanged();
								rc.addRelation(this, relation, isAutoAttackable);
								if (hasSummon())
								{
									final Summon pet = getPet();
									if (pet != null)
									{
										rc.addRelation(pet, relation, isAutoAttackable);
									}
									
									if (hasServitors())
									{
										getServitors().values().forEach(s -> rc.addRelation(s, relation, isAutoAttackable));
									}
								}
								
								player.sendPacket(rc);
								getKnownRelations().put(objectId, new RelationCache(relation, isAutoAttackable));
							}
						}
					});
				}
				
				_broadcastCharInfoTask = null;
			}, 100);
		}
	}
	
	/**
	 * Sends the clan information to this player.<br>
	 * Updates all other players with the current title information.
	 */
	public void broadcastTitleInfo()
	{
		// Send a Server->Client packet UserInfo to this Player.
		broadcastUserInfo(UserInfoType.CLAN);
		
		// Send a Server->Client packet TitleUpdate to all known players.
		broadcastPacket(new NicknameChanged(this));
	}
	
	/**
	 * Sends a {@code ServerPacket} to all visible players in the world.<br>
	 * This method ensures that only nearby players receive the packet.<br>
	 * It also allows for an optional local copy to be sent to the caller.
	 * @param packet The {@code ServerPacket} to be broadcasted.
	 * @param includeSelf Whether to send the packet back to the sender as well.
	 */
	@Override
	public void broadcastPacket(ServerPacket packet, boolean includeSelf)
	{
		if (packet instanceof ExCharInfo)
		{
			new IllegalArgumentException("CharInfo is being send via broadcastPacket. Do NOT do that! Use broadcastCharInfo() instead.");
		}
		
		packet.sendInBroadcast();
		
		if (includeSelf)
		{
			sendPacket(packet);
		}
		
		World.forEachVisibleObject(this, Player.class, player ->
		{
			if (!isVisibleFor(player))
			{
				return;
			}
			
			player.sendPacket(packet);
		});
	}
	
	/**
	 * Retrieves the unique identifier for the player's ally group.<br>
	 * This method checks if the player belongs to a {@code Clan}.<br>
	 * If no clan exists, it returns 0.
	 * @return The integer ID of the ally group or 0 if none exists.
	 */
	@Override
	public int getAllyId()
	{
		return _clan == null ? 0 : _clan.getAllyId();
	}
	
	/**
	 * Retrieves the unique identifier for the ally crest.<br>
	 * This method checks if the actor has an ally first.<br>
	 * If no ally exists, it returns {@code 0}.<br>
	 * Otherwise, it returns the ID from the {@code _clan} object.
	 * @return The integer ID of the ally crest or {@code 0} if none is found.
	 */
	public int getAllyCrestId()
	{
		return getAllyId() == 0 ? 0 : _clan.getAllyCrestId();
	}
	
	/**
	 * Sends a network packet to the connected client.<br>
	 * This method checks if the {@code _client} is not {@code null} before sending.
	 * @param packet The {@code ServerPacket} to be sent to the client.
	 */
	@Override
	public void sendPacket(ServerPacket packet)
	{
		if (_client != null)
		{
			_client.sendPacket(packet);
		}
	}
	
	/**
	 * Sends a system message to the player.<br>
	 * This method creates a new {@code SystemMessage} using the provided ID.<br>
	 * It then transmits that message to the client.
	 * @param id The unique identifier for the system message to be sent.
	 */
	@Override
	public void sendPacket(SystemMessageId id)
	{
		sendPacket(new SystemMessage(id));
	}
	
	/**
	 * Performs an interaction with the specified {@code Creature}.<br>
	 * If the target is a {@code Player}, it opens the appropriate store menu.<br>
	 * If the target is not a player, it triggers the target's action method.
	 * @param target The {@code Creature} to interact with.
	 */
	public void doInteract(Creature target)
	{
		if (target == null)
		{
			return;
		}
		
		if (target.isPlayer())
		{
			final Player targetPlayer = target.asPlayer();
			sendPacket(ActionFailed.STATIC_PACKET);
			
			final PrivateStoreType targetStoreType = targetPlayer.getPrivateStoreType();
			if ((targetStoreType == PrivateStoreType.SELL) || (targetStoreType == PrivateStoreType.PACKAGE_SELL))
			{
				if (_isSellingBuffs)
				{
					SellBuffsManager.getInstance().sendBuffMenu(this, targetPlayer, 0);
				}
				else
				{
					sendPacket(new PrivateStoreListSell(this, targetPlayer));
				}
			}
			else if (targetStoreType == PrivateStoreType.BUY)
			{
				sendPacket(new PrivateStoreListBuy(this, targetPlayer));
			}
			else if (targetStoreType == PrivateStoreType.MANUFACTURE)
			{
				sendPacket(new RecipeShopSellList(this, targetPlayer));
			}
		}
		else // _interactTarget=null should never happen but one never knows ^^;
		{
			target.onAction(this);
		}
	}
	
	/**
	 * Automatically handles the looting process for a specific item.<br>
	 * It distributes items to party members if applicable.<br>
	 * Otherwise, it adds the item or adena directly to the inventory.
	 * @param target The {@code Attackable} entity that dropped the loot.
	 * @param itemId The unique identifier of the item to be looted.
	 * @param itemCount The amount of the item to receive.
	 */
	public void doAutoLoot(Attackable target, int itemId, long itemCount)
	{
		if (isInParty() && !ItemData.getInstance().getTemplate(itemId).hasExImmediateEffect())
		{
			_party.distributeItem(this, itemId, itemCount, false, target);
		}
		else if (itemId == Inventory.ADENA_ID)
		{
			addAdena(ItemProcessType.LOOT, itemCount, target, true);
		}
		else
		{
			addItem(ItemProcessType.LOOT, itemId, itemCount, target, true);
		}
	}
	
	/**
	 * Automatically collects an item from a target.<br>
	 * This method handles the logic for picking up loot.
	 * @param target The {@code Attackable} entity that dropped the item.
	 * @param item The {@code ItemHolder} containing the item data to be looted.
	 */
	public void doAutoLoot(Attackable target, ItemHolder item)
	{
		doAutoLoot(target, item.getId(), item.getCount());
	}
	
	/**
	 * Attempts to pick up a {@code WorldObject} from the game world.<br>
	 * This method validates if the object is an item and checks for various restrictions like inventory capacity or trade status.<br>
	 * If successful, it adds the item to the player's inventory and handles special logic such as party distribution or auto-use effects.
	 * @param object The {@code WorldObject} that the player is trying to pick up.
	 */
	@Override
	public void doPickupItem(WorldObject object)
	{
		// Wyvern riders cannot pick up items.
		if (_mountType == MountType.WYVERN)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (isAlikeDead() || isFakeDeath())
		{
			return;
		}
		
		if (getActiveTradeList() != null)
		{
			sendPacket(SystemMessageId.YOU_CANNOT_PICK_UP_OR_USE_ITEMS_WHILE_TRADING);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Set the AI Intention to IDLE.
		getAI().setIntentionIdle();
		
		// Check if the WorldObject to pick up is a Item.
		if (!object.isItem())
		{
			// Do not try to pickup anything that is not an item :)
			LOGGER.warning(this + " trying to pickup wrong target." + getTarget());
			return;
		}
		
		final Item target = (Item) object;
		
		// Send a Server->Client packet ActionFailed to this Player.
		sendPacket(ActionFailed.STATIC_PACKET);
		
		// Send a Server->Client packet StopMove to this Player.
		sendPacket(new StopMove(this));
		SystemMessage smsg = null;
		synchronized (target)
		{
			// Check if the target to pick up is visible.
			if (!target.isSpawned())
			{
				// Send a Server->Client packet ActionFailed to this Player.
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!target.getDropProtection().tryPickUp(this))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}
			
			if (((isInParty() && (_party.getDistributionType() == PartyDistributionType.FINDERS_KEEPERS)) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
				return;
			}
			
			// The bag has to carry the weight as well, not only the slot.
			if (((isInParty() && (_party.getDistributionType() == PartyDistributionType.FINDERS_KEEPERS)) || !isInParty()) && !_inventory.validateWeight(target.getTemplate().getWeight() * target.getCount()))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.WEIGHT_LIMIT_IS_EXCEEDED);
				return;
			}
			
			if (isInvul() && !isGM())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}
			
			final int ownerId = target.getOwnerId();
			if ((ownerId != 0) && (ownerId != getObjectId()) && !isInLooterParty(ownerId))
			{
				if (target.getId() == Inventory.ADENA_ID)
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1_ADENA);
					smsg.addLong(target.getCount());
				}
				else
				{
					final long targetCount = target.getCount();
					if (targetCount > 1)
					{
						smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S2_S1_S);
						smsg.addItemName(target);
						smsg.addLong(targetCount);
					}
					else
					{
						smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
						smsg.addItemName(target);
					}
				}
				
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(smsg);
				return;
			}
			
			// You can pickup only 1 combat flag.
			if (FortSiegeManager.getInstance().isCombat(target.getId()) && !FortSiegeManager.getInstance().checkIfCanPickup(this))
			{
				return;
			}
			
			if ((target.getItemLootShedule() != null) && ((ownerId == getObjectId()) || isInLooterParty(ownerId)))
			{
				target.resetOwnerTimer();
			}
			
			// Remove the Item from the world and send server->client GetItem packets.
			target.pickupMe(this);
			if (GeneralConfig.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}
		}
		
		// Auto use herbs - pick up.
		final int targetId = target.getId();
		if (target.getTemplate().hasExImmediateEffect())
		{
			final IItemHandler handler = ItemHandler.getInstance().getHandler(target.getEtcItem());
			if (handler == null)
			{
				LOGGER.warning("No item handler registered for item ID: " + targetId + ".");
			}
			else
			{
				handler.onItemUse(this, target, false);
			}
			
			ItemManager.destroyItem(ItemProcessType.NONE, target, this, null);
		}
		// Cursed Weapons are not distributed.
		else if (CursedWeaponsManager.getInstance().isCursed(targetId))
		{
			addItem(ItemProcessType.PICKUP, target, null, true);
		}
		else if (FortSiegeManager.getInstance().isCombat(targetId))
		{
			addItem(ItemProcessType.PICKUP, target, null, true);
		}
		else
		{
			// If item is instance of ArmorType or WeaponType broadcast an "Attention" system message.
			if ((target.getItemType() instanceof ArmorType) || (target.getItemType() instanceof WeaponType))
			{
				if (target.isEnchanted())
				{
					smsg = new SystemMessage(SystemMessageId.ATTENTION_C1_HAS_PICKED_UP_S2_S3);
					smsg.addPcName(this);
					smsg.addInt(target.getEnchantLevel());
					smsg.addItemName(targetId);
					broadcastPacket(smsg);
				}
				else
				{
					smsg = new SystemMessage(SystemMessageId.ATTENTION_C1_HAS_PICKED_UP_S2);
					smsg.addPcName(this);
					smsg.addItemName(targetId);
					broadcastPacket(smsg);
				}
			}
			
			// Check if a Party is in progress.
			if (isInParty())
			{
				_party.distributeItem(this, target);
			}
			else if ((targetId == Inventory.ADENA_ID) && (_inventory.getAdenaInstance() != null))
			{
				addAdena(ItemProcessType.PICKUP, target.getCount(), null, true);
				ItemManager.destroyItem(ItemProcessType.PICKUP, target, this, null);
			}
			else
			{
				addItem(ItemProcessType.PICKUP, target, null, true);
			}
		}
	}
	
	/**
	 * Executes an automatic attack against the specified target.<br>
	 * This method calls the parent class logic to perform the attack.<br>
	 * It also resets the fake death status and updates PvP status if applicable.
	 * @param target The {@code Creature} to be attacked.
	 */
	@Override
	public void doAutoAttack(Creature target)
	{
		// Wyvern riders cannot attack monsters.
		if ((_mountType == MountType.WYVERN) && target.isMonster())
		{
			getAI().setIntentionActive();
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		super.doAutoAttack(target);
		setRecentFakeDeath(false);
		if (target.isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE)
		{
			updatePvPStatus();
		}
	}
	
	/**
	 * Executes the casting logic for a specific skill.<br>
	 * This method calls the parent class cast logic and resets the fake death status.
	 * @param skill The {@code Skill} object to be cast.
	 */
	@Override
	public void doCast(Skill skill)
	{
		super.doCast(skill);
		setRecentFakeDeath(false);
	}
	
	/**
	 * Checks if the player is allowed to open a private store.<br>
	 * This method validates distance requirements and various status conditions.<br>
	 * It ensures the player is not in an invalid state like casting or riding a mount.
	 * @return {@code true} if the player can open a store, {@code false} otherwise.
	 */
	public boolean canOpenPrivateStore()
	{
		if ((PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_NPC > 0) || (PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_PLAYER > 0))
		{
			if (World.getFirstVisibleObjectInRange(this, Creature.class, 1000, creature -> (creature.getMinShopDistance() > 0) && LocationUtil.checkIfInRange(creature.getMinShopDistance(), this, creature, true)) != null)
			{
				sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE));
				return false;
			}
		}
		
		return !_isSellingBuffs && !isAlikeDead() && !_inOlympiadMode && !isMounted() && !isInsideZone(ZoneId.NO_STORE) && !isCastingNow();
	}
	
	/**
	 * Retrieves the minimum distance for a shop based on the current sitting state.<br>
	 * It returns {@code PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_PLAYER} if the actor is waiting to sit.<br>
	 * Otherwise, it returns {@code 0}.
	 * @return The minimum shop distance as an {@code int}.
	 */
	@Override
	public int getMinShopDistance()
	{
		return _waitTypeSitting ? PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_PLAYER : 0;
	}
	
	/**
	 * Attempts to open a private buy store for the player.<br>
	 * Checks if the player is allowed to open a store based on their current state.<br>
	 * If successful, it sets the store type and sends the necessary packets.<br>
	 * If unsuccessful, it sends an error message or a failure packet.
	 */
	public void tryOpenPrivateBuyStore()
	{
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death).
		if (canOpenPrivateStore())
		{
			if ((_privateStoreType == PrivateStoreType.BUY) || (_privateStoreType == PrivateStoreType.BUY_MANAGE))
			{
				setPrivateStoreType(PrivateStoreType.NONE);
			}
			
			if (_privateStoreType == PrivateStoreType.NONE)
			{
				if (_waitTypeSitting)
				{
					standUp();
				}
				
				setPrivateStoreType(PrivateStoreType.BUY_MANAGE);
				sendPacket(new PrivateStoreManageListBuy(1, this));
				sendPacket(new PrivateStoreManageListBuy(2, this));
			}
		}
		else
		{
			if (isInsideZone(ZoneId.NO_STORE))
			{
				sendPacket(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE);
			}
			
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Retrieves the current multi-sell list holder.
	 * @return the {@code PreparedMultisellListHolder} object.
	 */
	public PreparedMultisellListHolder getMultiSell()
	{
		return _currentMultiSell;
	}
	
	/**
	 * Sets the current multi-sell list for the player.<br>
	 * This method updates the {@code _currentMultiSell} field with the provided data.
	 * @param list The {@code PreparedMultisellListHolder} to set as the current list.
	 */
	public void setMultiSell(PreparedMultisellListHolder list)
	{
		_currentMultiSell = list;
	}
	
	/**
	 * Sets the current target for this actor.<br>
	 * This method validates if the {@code worldObject} is reachable, visible, and valid to be targeted.<br>
	 * It handles status listener registration and sends necessary packets to update the client.
	 * @param worldObject The {@code WorldObject} to set as the new target.
	 */
	@Override
	public void setTarget(WorldObject worldObject)
	{
		WorldObject newTarget = worldObject;
		if (newTarget != null)
		{
			final boolean isInParty = (newTarget.isPlayer() && isInParty() && _party.containsPlayer(newTarget.asPlayer()));
			
			// Prevents /target exploiting.
			if (!isInParty && (Math.abs(newTarget.getZ() - getZ()) > 3000))
			{
				newTarget = null;
			}
			
			// Check if the new target is visible.
			if ((newTarget != null) && !isInParty && !newTarget.isSpawned())
			{
				newTarget = null;
			}
			
			// Vehicles cannot be targeted.
			if (!isGM() && (newTarget instanceof Vehicle))
			{
				newTarget = null;
			}
		}
		
		// Get the current target.
		final WorldObject oldTarget = getTarget();
		if (oldTarget != null)
		{
			if (oldTarget.equals(newTarget)) // no target change?
			{
				// Validate location of the target.
				if ((newTarget != null) && (newTarget.getObjectId() != getObjectId()))
				{
					sendPacket(new ValidateLocation(newTarget));
				}
				return;
			}
			
			// Remove the target from the status listener.
			oldTarget.removeStatusListener(this);
		}
		
		if ((newTarget != null) && newTarget.isCreature())
		{
			final Creature target = newTarget.asCreature();
			final int newTargetObjectId = newTarget.getObjectId();
			final int objectId = getObjectId();
			
			// Validate location of the new target.
			if (newTargetObjectId != objectId)
			{
				sendPacket(new ValidateLocation(target));
			}
			
			// Show the client his new target.
			sendPacket(new MyTargetSelected(this, target));
			
			// Register target to listen for hp changes.
			target.addStatusListener(this);
			
			// Send max/current hp.
			final StatusUpdate su = new StatusUpdate(target);
			su.addUpdate(StatusUpdateType.MAX_HP, target.getMaxHp());
			su.addUpdate(StatusUpdateType.CUR_HP, (long) target.getCurrentHp());
			sendPacket(su);
			
			// To others the new target, and not yourself!
			World.broadcastToVisiblePlayers(this, new TargetSelected(objectId, newTargetObjectId, getX(), getY(), getZ()));
			
			// Send buffs
			sendPacket(new ExAbnormalStatusUpdateFromTarget(target));
		}
		
		// Target was removed?
		if ((newTarget == null) && (getTarget() != null))
		{
			broadcastPacket(new TargetUnselected(this));
		}
		
		// Target the new WorldObject (add the target to the Player _target, _knownObject and Player to _KnownObject of the WorldObject).
		super.setTarget(newTarget);
	}
	
	/**
	 * Retrieves the item currently equipped in the right hand.<br>
	 * This method looks up the specific slot in the {@code Inventory}.
	 * @return the {@code Item} object located in the right hand slot, or {@code null} if empty.
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		return _inventory.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}
	
	/**
	 * Retrieves the current active weapon item.<br>
	 * If no weapon is equipped, it returns the default fists weapon.
	 * @return The {@code Weapon} template of the currently held weapon or the fists weapon.
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		final Item weapon = getActiveWeaponInstance();
		if (weapon == null)
		{
			return _fistsWeaponItem;
		}
		
		return (Weapon) weapon.getTemplate();
	}
	
	/**
	 * Retrieves the chest armor item from the player's inventory.<br>
	 * This method looks specifically at the {@code PAPERDOLL_CHEST} slot.
	 * @return the {@code Item} located in the chest armor slot, or {@code null} if empty.
	 */
	public Item getChestArmorInstance()
	{
		return _inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
	}
	
	/**
	 * Retrieves the armor item currently equipped on the legs.<br>
	 * This method looks up the specific slot in the {@code Inventory}.
	 * @return The {@code Item} object for the leg armor, or {@code null} if empty.
	 */
	public Item getLegsArmorInstance()
	{
		return _inventory.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
	}
	
	/**
	 * Retrieves the template of the chest armor currently worn by the actor.<br>
	 * This method returns {@code null} if no chest armor is equipped.
	 * @return The {@code Armor} template of the active chest piece, or {@code null}.
	 */
	public Armor getActiveChestArmorItem()
	{
		final Item armor = getChestArmorInstance();
		if (armor == null)
		{
			return null;
		}
		
		return (Armor) armor.getTemplate();
	}
	
	/**
	 * Retrieves the {@code Armor} template for the current leg armor.<br>
	 * This method returns {@code null} if no leg armor is equipped.
	 * @return The {@code Armor} template of the active leg armor, or {@code null}.
	 */
	public Armor getActiveLegsArmorItem()
	{
		final Item legs = getLegsArmorInstance();
		if (legs == null)
		{
			return null;
		}
		
		return (Armor) legs.getTemplate();
	}
	
	/**
	 * Checks if the character is currently wearing heavy armor.<br>
	 * This method verifies both chest and leg armor types.<br>
	 * It also checks for full body armor pieces.
	 * @return {@code true} if the character is wearing heavy armor, {@code false} otherwise.
	 */
	public boolean isWearingHeavyArmor()
	{
		final Item armor = getChestArmorInstance();
		if (armor == null)
		{
			return false;
		}
		
		final ItemType armorType = armor.getItemType();
		final Item legs = getLegsArmorInstance();
		if ((legs != null) && (legs.getItemType() == ArmorType.HEAVY) && (armorType == ArmorType.HEAVY))
		{
			return true;
		}
		
		return (_inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR) && (armorType == ArmorType.HEAVY);
	}
	
	/**
	 * Checks if the character is currently wearing light armor.<br>
	 * This method verifies both chest and leg armor types.<br>
	 * It also considers the body part of the equipped item.
	 * @return {@code true} if the character is wearing light armor, {@code false} otherwise.
	 */
	public boolean isWearingLightArmor()
	{
		final Item armor = getChestArmorInstance();
		if (armor == null)
		{
			return false;
		}
		
		final ItemType armorType = armor.getItemType();
		final Item legs = getLegsArmorInstance();
		if ((legs != null) && (legs.getItemType() == ArmorType.LIGHT) && (armorType == ArmorType.LIGHT))
		{
			return true;
		}
		
		return (_inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR) && (armorType == ArmorType.LIGHT);
	}
	
	/**
	 * Checks if the character is currently wearing magic armor.<br>
	 * This method verifies both chest and leg armor types.<br>
	 * It also checks for full body armor pieces.
	 * @return {@code true} if magic armor is detected, {@code false} otherwise.
	 */
	public boolean isWearingMagicArmor()
	{
		final Item armor = getChestArmorInstance();
		if (armor == null)
		{
			return false;
		}
		
		final ItemType armorType = armor.getItemType();
		final Item legs = getLegsArmorInstance();
		if ((legs != null) && (legs.getItemType() == ArmorType.MAGIC) && (armorType == ArmorType.MAGIC))
		{
			return true;
		}
		
		return (_inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR) && (armorType == ArmorType.MAGIC);
	}
	
	/**
	 * Checks if the character is currently married.
	 * @return {@code true} if the character is married, {@code false} otherwise.
	 */
	public boolean isMarried()
	{
		return _married;
	}
	
	/**
	 * Updates the marital status of the actor.<br>
	 * Sets the internal {@code _married} flag to the provided value.
	 * @param value The new marital status to set.
	 */
	public void setMarried(boolean value)
	{
		_married = value;
	}
	
	/**
	 * Checks if the current actor has an active engage request.
	 * @return {@code true} if there is an engage request, {@code false} otherwise.
	 */
	public boolean isEngageRequest()
	{
		return _engagerequest;
	}
	
	/**
	 * Updates the engagement request status and ID.<br>
	 * This method sets the internal state for a specific player's engagement.
	 * @param state The new boolean state of the engagement request.
	 * @param playerid The unique identifier for the player involved in the request.
	 */
	public void setEngageRequest(boolean state, int playerid)
	{
		_engagerequest = state;
		_engageid = playerid;
	}
	
	/**
	 * Updates the marriage request status.<br>
	 * Sets the {@code _marryrequest} field to the specified boolean value.
	 * @param value The new status for the marriage request.
	 */
	public void setMarryRequest(boolean value)
	{
		_marryrequest = value;
	}
	
	/**
	 * Checks if there is a pending marriage request.
	 * @return {@code true} if a marriage request exists, {@code false} otherwise.
	 */
	public boolean isMarryRequest()
	{
		return _marryrequest;
	}
	
	/**
	 * Updates the marriage acceptance status.<br>
	 * Sets the internal {@code _marryaccepted} flag to the provided value.
	 * @param value The new status to set as {@code true} or {@code false}.
	 */
	public void setMarryAccepted(boolean value)
	{
		_marryaccepted = value;
	}
	
	/**
	 * Checks if the marriage proposal has been accepted.<br>
	 * Returns {@code true} if the status is accepted.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating the acceptance status.
	 */
	public boolean isMarryAccepted()
	{
		return _marryaccepted;
	}
	
	/**
	 * Retrieves the unique engagement identifier for this actor.
	 * @return The current {@code int} value of the engage ID.
	 */
	public int getEngageId()
	{
		return _engageid;
	}
	
	/**
	 * Retrieves the unique identifier of the partner.
	 * @return the {@code int} ID of the partner.
	 */
	public int getPartnerId()
	{
		return _partnerId;
	}
	
	/**
	 * Sets the unique identifier for the partner.<br>
	 * This value is stored in the {@code _partnerId} field.
	 * @param partnerid The unique ID of the partner to assign.
	 */
	public void setPartnerId(int partnerid)
	{
		_partnerId = partnerid;
	}
	
	/**
	 * Retrieves the unique identifier for the player's couple. <br>
	 * This value identifies which couple a character belongs to.
	 * @return The {@code int} ID of the couple.
	 */
	public int getCoupleId()
	{
		return _coupleId;
	}
	
	/**
	 * Sets the unique identifier for a couple.<br>
	 * This value is stored in the {@code _coupleId} field.
	 * @param coupleId The unique ID of the couple to assign.
	 */
	public void setCoupleId(int coupleId)
	{
		_coupleId = coupleId;
	}
	
	/**
	 * Processes the response to an engagement request.<br>
	 * This method updates the request status and notifies the target player.
	 * @param answer The response value where {@code 1} means accepted and any other value means denied.
	 */
	public void engageAnswer(int answer)
	{
		if (!_engagerequest || (_engageid == 0))
		{
			return;
		}
		
		final Player ptarget = World.getPlayer(_engageid);
		setEngageRequest(false, 0);
		if (ptarget != null)
		{
			if (answer == 1)
			{
				CoupleManager.getInstance().createCouple(ptarget, Player.this);
				ptarget.sendMessage("Request to Engage has been >ACCEPTED<");
			}
			else
			{
				ptarget.sendMessage("Request to Engage has been >DENIED<!");
			}
		}
	}
	
	/**
	 * Retrieves the item currently equipped in the left hand.<br>
	 * This method looks up the item from the {@code Paperdoll} slot.
	 * @return the {@code Item} object in the left hand, or {@code null} if empty.
	 */
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}
	
	/**
	 * Retrieves the template of the item equipped in the left hand.<br>
	 * This method checks the paperdoll for an item at the {@code PAPERDOLL_LHAND} slot.<br>
	 * It returns {@code null} if no item is equipped.
	 * @return The {@code ItemTemplate} of the left-hand item, or {@code null}.
	 */
	@Override
	public ItemTemplate getSecondaryWeaponItem()
	{
		final Item item = _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (item != null)
		{
			return item.getTemplate();
		}
		
		return null;
	}
	
	/**
	 * Handles the logic for when a {@code Creature} dies.<br>
	 * This method processes rewards, announcements, and status updates based on the killer.<br>
	 * It also handles item drops, pet deactivation, and reputation changes.
	 * @param killer The {@code Creature} that caused the death.
	 * @return {@code true} if the death was processed successfully, {@code false} otherwise.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		AerialCleftManager.getInstance().onKill(killer, this);
		
		final KrateiArena arena = getKrateiArena();
		if (arena != null)
		{
			arena.setRespawnTask(this);
			if ((killer != null) && killer.isPlayable())
			{
				final Player player = killer.asPlayer();
				if (player != null)
				{
					final KrateiArena killerArena = player.getKrateiArena();
					if (killerArena != null)
					{
						killerArena.addPoints(player, true);
					}
				}
			}
		}
		
		// Stop auto peel.
		if (hasRequest(AutoPeelRequest.class))
		{
			sendPacket(new ExStopItemAutoPeel(true));
			sendPacket(new ExReadyItemAutoPeel(false, 0));
			removeRequest(AutoPeelRequest.class);
		}
		
		// Add Tranquil Soul effect.
		SkillCaster.triggerCast(this, this, CommonSkill.TRANQUIL_SOUL.getSkill());
		
		Collection<Item> droppedItems = null;
		
		if (killer != null)
		{
			final Player pk = killer.asPlayer();
			final boolean fpcKill = killer.isFakePlayer();
			if ((pk != null) || fpcKill)
			{
				final Party party = getParty();
				if ((party != null) && (party.getUCState() instanceof UCTeam))
				{
					((UCTeam) party.getUCState()).onKill(this, pk);
				}
				
				if (pk != null)
				{
					if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PVP_KILL, this))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPvPKill(pk, this), this);
					}
					
					setTotalDeaths(getTotalDeaths() + 1);
					
					// pvp/pk item rewards
					if (!(PvpRewardItemConfig.DISABLE_REWARDS_IN_INSTANCES && (getInstanceId() != 0)) && //
						!(PvpRewardItemConfig.DISABLE_REWARDS_IN_PVP_ZONES && isInsideZone(ZoneId.PVP)))
					{
						// pvp
						if (PvpRewardItemConfig.REWARD_PVP_ITEM && (_pvpFlag != 0))
						{
							pk.addItem(ItemProcessType.REWARD, PvpRewardItemConfig.REWARD_PVP_ITEM_ID, PvpRewardItemConfig.REWARD_PVP_ITEM_AMOUNT, this, PvpRewardItemConfig.REWARD_PVP_ITEM_MESSAGE);
						}
						
						// pk
						if (PvpRewardItemConfig.REWARD_PK_ITEM && (_pvpFlag == 0))
						{
							pk.addItem(ItemProcessType.REWARD, PvpRewardItemConfig.REWARD_PK_ITEM_ID, PvpRewardItemConfig.REWARD_PK_ITEM_AMOUNT, this, PvpRewardItemConfig.REWARD_PK_ITEM_MESSAGE);
						}
					}
				}
				
				// announce pvp/pk
				if (PvpAnnounceConfig.ANNOUNCE_PK_PVP && (((pk != null) && !pk.isGM()) || fpcKill))
				{
					String msg = "";
					if (_pvpFlag == 0)
					{
						msg = PvpAnnounceConfig.ANNOUNCE_PK_MSG.replace("$killer", pk != null ? pk.getName() : killer.getName()).replace("$target", _name);
						if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.S1_3);
							sm.addString(msg);
							World.broadcastToAllOnlinePlayers(sm);
						}
						else
						{
							World.broadcastToAllOnlinePlayers(msg, false);
						}
					}
					else if (_pvpFlag != 0)
					{
						msg = PvpAnnounceConfig.ANNOUNCE_PVP_MSG.replace("$killer", killer.getName()).replace("$target", _name);
						if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.S1_3);
							sm.addString(msg);
							World.broadcastToAllOnlinePlayers(sm);
						}
						else
						{
							World.broadcastToAllOnlinePlayers(msg, false);
						}
					}
				}
				
				if (fpcKill && FakePlayersConfig.FAKE_PLAYER_KILL_KARMA && (_pvpFlag == 0) && (getReputation() >= 0))
				{
					killer.setReputation(killer.getReputation() - 150);
				}
			}
			
			broadcastStatusUpdate();
			
			// Clear resurrect xp calculation.
			setExpBeforeDeath(0);
			
			// Calculate Shilen's Breath debuff level. It must happen right before death, because buffs aren't applied on dead characters.
			_deathPenalty.calculate(killer);
			
			// Kill the Player.
			if (!super.doDie(killer))
			{
				return false;
			}
			
			// Issues drop of Cursed Weapon.
			if (isCursedWeaponEquipped())
			{
				CursedWeaponsManager.getInstance().drop(_cursedWeaponEquippedId, killer);
			}
			else if (_combatFlagEquippedId)
			{
				final Fort fort = FortManager.getInstance().getFort(this);
				if (fort != null)
				{
					FortSiegeManager.getInstance().dropCombatFlag(this, fort.getResidenceId());
				}
				else
				{
					final BodyPart bodyPart = BodyPart.fromItem(_inventory.getItemByItemId(9819));
					_inventory.unEquipItemInBodySlot(bodyPart);
					destroyItem(ItemProcessType.DESTROY, _inventory.getItemByItemId(9819), null, true);
				}
			}
			else
			{
				final boolean insidePvpZone = isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE);
				if ((pk == null) || !pk.isCursedWeaponEquipped())
				{
					// Only-PvE areas and, when enabled, peace zones (towns): no item drop and no XP loss on death.
					final boolean peaceZoneNoPenalty = PlayerConfig.PEACE_ZONE_DEATH_NO_PENALTY && isInsideZone(ZoneId.PEACE);
					if (!isInsideZone(ZoneId.ONLY_PVE) && !peaceZoneNoPenalty)
					{
						droppedItems = onDieDropItem(killer); // Check if any item should be dropped.
					}
					
					if (!insidePvpZone && (pk != null))
					{
						final Clan pkClan = pk.getClan();
						if ((pkClan != null) && (_clan != null) && !isAcademyMember() && !(pk.isAcademyMember()))
						{
							final ClanWar clanWar = _clan.getWarWith(pkClan.getId());
							if ((clanWar != null) && AntiFeedManager.getInstance().check(killer, this))
							{
								pk.increaseClanContribution(FeatureConfig.CLAN_CONTRIBUTION_REWARD_FOR_ENEMY);
								clanWar.onKill(pk, this);
							}
						}
					}
					
					// Should not penalize player when lucky, in a PvP zone or event.
					if (!isLucky() && !insidePvpZone && !isOnEvent() && !isInsideZone(ZoneId.ONLY_PVE) && !peaceZoneNoPenalty)
					{
						calculateDeathExpPenalty(killer);
					}
				}
			}
		}
		
		sendPacket(new ExDieInfo(droppedItems == null ? Collections.emptyList() : droppedItems, _lastDamageTaken));
		
		if (isMounted())
		{
			stopFeed();
		}
		
		// synchronized (this)
		// {
		if (isFakeDeath())
		{
			stopFakeDeath(true);
		}
		
		// }
		
		// Unsummon Cubics
		if (!_cubics.isEmpty())
		{
			_cubics.getCubics().values().forEach(Cubic::deactivate);
			_cubics.clear();
		}
		
		for (Npc npc : getSummonedNpcs())
		{
			if (npc instanceof Doppelganger)
			{
				npc.deleteMe();
			}
		}
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		
		if (_agathionIds[0] != 0)
		{
			setAgathionId(0);
		}
		
		if (hasServitors())
		{
			getServitors().values().forEach(servitor ->
			{
				if (servitor.isBetrayed())
				{
					sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
					return;
				}
				
				servitor.cancelAction();
			});
		}
		
		stopRentPet();
		stopWaterTask();
		
		AntiFeedManager.getInstance().setLastDeathTime(getObjectId());
		
		// FIXME: Karma reduction tempfix.
		if (getReputation() < 0)
		{
			final int reputation = getReputation();
			final int newRep = reputation - (reputation / 4);
			setReputation(newRep < -20 ? newRep : 0);
		}
		
		if (isInTimedHuntingZone())
		{
			DecayTaskManager.getInstance().add(this);
			sendPacket(new TimeRestrictFieldDieLimitTime());
		}
		else if (PlayerConfig.DISCONNECT_AFTER_DEATH)
		{
			DecayTaskManager.getInstance().add(this);
		}
		
		return true;
	}
	
	/**
	 * Records a new instance of damage received by this entity.<br>
	 * It prevents self-inflicted damage from being added to the history.<br>
	 * The internal list is limited to the 20 most recent entries.
	 * @param attacker The {@code Creature} that dealt the damage.
	 * @param skillId The unique identifier for the skill used.
	 * @param damage The amount of damage dealt.
	 * @param isDOT Whether the damage is from a Damage Over Time effect.
	 * @param reflect Whether the damage was reflected by the target.
	 */
	public void addDamageTaken(Creature attacker, int skillId, double damage, boolean isDOT, boolean reflect)
	{
		if (attacker == this)
		{
			return;
		}
		
		synchronized (_lastDamageTaken)
		{
			_lastDamageTaken.add(new DamageTakenHolder(attacker, skillId, damage, isDOT, reflect));
			if (_lastDamageTaken.size() > 20)
			{
				_lastDamageTaken.removeFirst();
			}
		}
	}
	
	/**
	 * Resets the history of damage taken by this actor.<br>
	 * This method clears all entries from the {@code _lastDamageTaken} collection.
	 */
	public void clearDamageTaken()
	{
		synchronized (_lastDamageTaken)
		{
			_lastDamageTaken.clear();
		}
	}
	
	/**
	 * Determines the items to be dropped upon death based on the killer.<br>
	 * This method checks for events, clan wars, and karma status before calculating drop rates.<br>
	 * It filters out non-droppable items like quest items or shadow items.
	 * @param killer The {@code Creature} that killed the entity.
	 * @return A {@code Collection} of {@code Item} objects dropped by the entity.
	 */
	private Collection<Item> onDieDropItem(Creature killer)
	{
		final List<Item> droppedItems = new ArrayList<>();
		if (isOnEvent() || (killer == null))
		{
			return droppedItems;
		}
		
		final Player pk = killer.asPlayer();
		final int reputation = getReputation();
		if ((reputation >= 0) && (pk != null) && (pk.getClan() != null) && (getClan() != null) && (pk.getClan().isAtWarWith(_clanId)
		
		// || _clan.isAtWarWith(pk.getClanId())
		))
		{
			return droppedItems;
		}
		
		if ((!isInsideZone(ZoneId.PVP) || (pk == null)) && (!isGM() || PvpConfig.KARMA_DROP_GM))
		{
			boolean isKarmaDrop = false;
			final int pkLimit = PvpConfig.KARMA_PK_LIMIT;
			int dropEquip = 0;
			int dropEquipWeapon = 0;
			int dropItem = 0;
			int dropLimit = 0;
			int dropPercent = 0;
			if ((reputation < 0) && (_pkKills >= pkLimit))
			{
				isKarmaDrop = true;
				dropPercent = RatesConfig.KARMA_RATE_DROP;
				dropEquip = RatesConfig.KARMA_RATE_DROP_EQUIP;
				dropEquipWeapon = RatesConfig.KARMA_RATE_DROP_EQUIP_WEAPON;
				dropItem = RatesConfig.KARMA_RATE_DROP_ITEM;
				dropLimit = RatesConfig.KARMA_DROP_LIMIT;
			}
			else if (killer.isNpc() && (getLevel() > 4))
			{
				dropPercent = RatesConfig.PLAYER_RATE_DROP;
				dropEquip = RatesConfig.PLAYER_RATE_DROP_EQUIP;
				dropEquipWeapon = RatesConfig.PLAYER_RATE_DROP_EQUIP_WEAPON;
				dropItem = RatesConfig.PLAYER_RATE_DROP_ITEM;
				dropLimit = RatesConfig.PLAYER_DROP_LIMIT;
			}
			
			if ((dropPercent > 0) && (Rnd.get(100) < dropPercent))
			{
				int dropCount = 0;
				int itemDropPercent = 0;
				for (Item itemDrop : _inventory.getItems())
				{
					// Don't drop
					if (itemDrop.isShadowItem() || // do not drop Shadow Items
						itemDrop.isTimeLimitedItem() || // do not drop Time Limited Items
						!itemDrop.isDropable() || (itemDrop.getId() == Inventory.ADENA_ID) || // Adena
						(itemDrop.getTemplate().getType2() == ItemTemplate.TYPE2_QUEST) || // Quest Items
						((_pet != null) && (_pet.getControlObjectId() == itemDrop.getId())) || // Control Item of active pet
						(Arrays.binarySearch(PvpConfig.KARMA_LIST_NONDROPPABLE_ITEMS, itemDrop.getId()) >= 0) || // Item listed in the non droppable item list
						(Arrays.binarySearch(PvpConfig.KARMA_LIST_NONDROPPABLE_PET_ITEMS, itemDrop.getId()) >= 0 // Item listed in the non droppable pet item list
						))
					{
						continue;
					}
					
					if (itemDrop.isEquipped())
					{
						// Set proper chance according to Item type of equipped Item.
						itemDropPercent = itemDrop.getTemplate().getType2() == ItemTemplate.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
					}
					else
					{
						itemDropPercent = dropItem; // Item in inventory
					}
					
					// NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
					if (Rnd.get(100) < itemDropPercent)
					{
						if (itemDrop.isEquipped())
						{
							_inventory.unEquipItemInSlot(itemDrop.getLocationSlot());
						}
						
						dropItem(ItemProcessType.DEATH, itemDrop, killer, true);
						droppedItems.add(itemDrop);
						sendItemList();
						
						final String name = _name;
						final int itemDropId = itemDrop.getId();
						final long itemCount = itemDrop.getCount();
						if (isKarmaDrop)
						{
							LOGGER.warning(name + " has karma and dropped id = " + itemDropId + ", count = " + itemCount);
						}
						else
						{
							LOGGER.warning(name + " dropped id = " + itemDropId + ", count = " + itemCount);
						}
						
						if (++dropCount >= dropLimit)
						{
							break;
						}
					}
				}
			}
		}
		
		return droppedItems;
	}
	
	/**
	 * Handles the logic triggered when this actor kills a target.<br>
	 * It updates statistics such as reputation, PK kills, and siege counts.<br>
	 * The method checks for specific conditions like Olympiad mode or PvP zones.
	 * @param target The {@code Playable} entity that was killed.
	 */
	public void onPlayerKill(Playable target)
	{
		if ((target == null) || !target.isPlayable())
		{
			return;
		}
		
		// Avoid nulls && check if player != killedPlayer.
		final Player killedPlayer = target.asPlayer();
		if ((killedPlayer == null) || (this == killedPlayer))
		{
			return;
		}
		
		// Cursed weapons progress - Add killedPlayer to version Prelude of War 2019
		if (isCursedWeaponEquipped() && target.isPlayer())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquippedId, killedPlayer);
			return;
		}
		
		// Olympiad support
		if (isInOlympiadMode() || killedPlayer.isInOlympiadMode())
		{
			return;
		}
		
		// Duel support
		if (isInDuel() && killedPlayer.isInDuel())
		{
			return;
		}
		
		// If both players are in SIEGE zone just increase siege kills/deaths.
		final boolean targetIsPlayer = target.isPlayer();
		if (targetIsPlayer && isInsideZone(ZoneId.SIEGE) && killedPlayer.isInsideZone(ZoneId.SIEGE))
		{
			if (!isSiegeFriend(killedPlayer))
			{
				final Clan targetClan = killedPlayer.getClan();
				if ((_clan != null) && (targetClan != null))
				{
					_clan.addSiegeKill();
					targetClan.addSiegeDeath();
				}
			}
			return;
		}
		
		// Do nothing when in PVP zone.
		if (isInsideZone(ZoneId.PVP) || target.isInsideZone(ZoneId.PVP))
		{
			return;
		}
		
		if (checkIfPvP(killedPlayer))
		{
			// Check if player should get + rep.
			if (killedPlayer.getReputation() < 0)
			{
				final int levelDiff = killedPlayer.getLevel() - getLevel();
				final int myReputation = getReputation();
				if ((myReputation >= 0) && (levelDiff < 11) && (levelDiff > -11)) // TODO: Time check, same player can't be killed again in 8 hours
				{
					setReputation(myReputation + PvpConfig.REPUTATION_INCREASE);
				}
			}
			
			if (targetIsPlayer)
			{
				setPvpKills(_pvpKills + 1);
				setTotalKills(getTotalKills() + 1);
				updatePvpTitleAndColor(true);
			}
		}
		else if ((getReputation() > 0) && (_pkKills == 0))
		{
			setReputation(0);
			if (targetIsPlayer)
			{
				setTotalKills(getTotalKills() + 1);
				setPkKills(getPkKills() + 1);
			}
		}
		else // Calculate new karma and increase pk count
		{
			if (FactionSystemConfig.FACTION_SYSTEM_ENABLED)
			{
				if ((_isGood && killedPlayer.isGood()) || (_isEvil && killedPlayer.isEvil()))
				{
					setReputation(getReputation() - Formulas.calculateKarmaGain(getPkKills(), target.isSummon()));
					if (targetIsPlayer)
					{
						setPkKills(getPkKills() + 1);
						setTotalKills(getTotalKills() + 1);
					}
				}
			}
			else
			{
				setReputation(getReputation() - Formulas.calculateKarmaGain(getPkKills(), target.isSummon()));
				if (targetIsPlayer)
				{
					setPkKills(getPkKills() + 1);
					setTotalKills(getTotalKills() + 1);
					
					// Teleport to Prison.
					if ((_pkKills >= 10) && (_pkKills < 35))
					{
						teleToLocation(61414, -42632, -2992);
						getVariables().set(PlayerVariables.PRISON_WAIT_TIME, System.currentTimeMillis() + 18000000);
					}
					else if ((_pkKills >= 35) && (_pkKills < 40))
					{
						teleToLocation(59147, -42547, -3000);
						getVariables().set(PlayerVariables.PRISON_WAIT_TIME, System.currentTimeMillis() + 14400000);
					}
					else if (_pkKills >= 40)
					{
						teleToLocation(58969, -44995, -2992);
						getVariables().set(PlayerVariables.PRISON_WAIT_TIME, System.currentTimeMillis() + 10800000);
					}
					
					// Einhasad debuffs.
					if (_pkKills > 9)
					{
						SkillCaster.triggerCast(this, this, CommonSkill.EINHASAD_OVERSEEING_4.getSkill());
						SkillCaster.triggerCast(this, this, CommonSkill.EINHASAD_CHAINS.getSkill());
					}
					else if (_pkKills > 7)
					{
						SkillCaster.triggerCast(this, this, CommonSkill.EINHASAD_OVERSEEING_3.getSkill());
					}
					else if (_pkKills > 5)
					{
						SkillCaster.triggerCast(this, this, CommonSkill.EINHASAD_OVERSEEING_2.getSkill());
					}
					else if (_pkKills > 3)
					{
						SkillCaster.triggerCast(this, this, CommonSkill.EINHASAD_OVERSEEING_1.getSkill());
					}
				}
			}
		}
		
		broadcastUserInfo(UserInfoType.SOCIAL);
		checkItemRestriction();
	}
	
	/**
	 * Updates the player title and name color based on their total PvP kills.<br>
	 * This method checks if the PvP color system is enabled and the faction system is disabled.<br>
	 * It applies specific titles and colors defined in {@code PvpTitleColorConfig}.
	 * @param broadcastInfo If {@code true}, it broadcasts the new title information to other players.
	 */
	public void updatePvpTitleAndColor(boolean broadcastInfo)
	{
		if (PvpTitleColorConfig.PVP_COLOR_SYSTEM_ENABLED && !FactionSystemConfig.FACTION_SYSTEM_ENABLED) // Faction system uses title colors.
		{
			if ((_pvpKills >= PvpTitleColorConfig.PVP_AMOUNT1) && (_pvpKills < PvpTitleColorConfig.PVP_AMOUNT2))
			{
				setTitle("\u00AE " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT1 + " \u00AE");
				_appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT1);
			}
			else if ((_pvpKills >= PvpTitleColorConfig.PVP_AMOUNT2) && (_pvpKills < PvpTitleColorConfig.PVP_AMOUNT3))
			{
				setTitle("\u00AE " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT2 + " \u00AE");
				_appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT2);
			}
			else if ((_pvpKills >= PvpTitleColorConfig.PVP_AMOUNT3) && (_pvpKills < PvpTitleColorConfig.PVP_AMOUNT4))
			{
				setTitle("\u00AE " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT3 + " \u00AE");
				_appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT3);
			}
			else if ((_pvpKills >= PvpTitleColorConfig.PVP_AMOUNT4) && (_pvpKills < PvpTitleColorConfig.PVP_AMOUNT5))
			{
				setTitle("\u00AE " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT4 + " \u00AE");
				_appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT4);
			}
			else if (_pvpKills >= PvpTitleColorConfig.PVP_AMOUNT5)
			{
				setTitle("\u00AE " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT5 + " \u00AE");
				_appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT5);
			}
			
			if (broadcastInfo)
			{
				broadcastTitleInfo();
			}
		}
	}
	
	/**
	 * Updates the current PvP status of the player.<br>
	 * This method checks if the player is in a {@code ZoneId.PVP} zone.<br>
	 * If not, it sets the next expiration time based on {@code PvpConfig.PVP_NORMAL_TIME}.<br>
	 * It also starts the PvP flag if it is currently set to {@code 0}.
	 */
	public void updatePvPStatus()
	{
		if (isInsideZone(ZoneId.PVP))
		{
			return;
		}
		
		setPvpFlagLasts(System.currentTimeMillis() + PvpConfig.PVP_NORMAL_TIME);
		if (_pvpFlag == 0)
		{
			startPvPFlag();
		}
	}
	
	/**
	 * Updates the {@code PvP} status for a specific creature.<br>
	 * This method checks if the target is eligible for {@code PvP} based on zone, faction, and duel status.<br>
	 * If valid, it sets the expiration time and starts the {@code PvP} flag if it is currently inactive.
	 * @param target The {@code Creature} to update.
	 */
	public void updatePvPStatus(Creature target)
	{
		final Player targetPlayer = target.asPlayer();
		if (targetPlayer == null)
		{
			return;
		}
		
		if (this == targetPlayer)
		{
			return;
		}
		
		if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && target.isPlayer() && ((isGood() && targetPlayer.isEvil()) || (isEvil() && targetPlayer.isGood())))
		{
			return;
		}
		
		if (_isInDuel && (targetPlayer.getDuelId() == getDuelId()))
		{
			return;
		}
		
		if ((!isInsideZone(ZoneId.PVP) || !target.isInsideZone(ZoneId.PVP)) && (targetPlayer.getReputation() >= 0))
		{
			if (checkIfPvP(targetPlayer))
			{
				setPvpFlagLasts(System.currentTimeMillis() + PvpConfig.PVP_PVP_TIME);
			}
			else
			{
				setPvpFlagLasts(System.currentTimeMillis() + PvpConfig.PVP_NORMAL_TIME);
			}
			
			if (_pvpFlag == 0)
			{
				startPvPFlag();
			}
		}
	}
	
	/**
	 * Checks if the player currently has the lucky status.<br>
	 * This returns {@code true} if the level is 9 or lower and the character has the {@code LUCKY} skill effect.
	 * @return {@code true} if the conditions are met, {@code false} otherwise.
	 */
	public boolean isLucky()
	{
		return (getLevel() <= 9) && isAffectedBySkill(CommonSkill.LUCKY.getId());
	}
	
	/**
	 * Restores a portion of the experience lost after death.<br>
	 * This method calculates the difference between current and previous experience.<br>
	 * It then adds a percentage of that difference back to the player's stats.
	 * @param restorePercent The percentage of lost experience to recover.
	 */
	public void restoreExp(double restorePercent)
	{
		if (_expBeforeDeath > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp(Math.round(((_expBeforeDeath - getExp()) * restorePercent) / 100));
			setExpBeforeDeath(0);
		}
	}
	
	/**
	 * Calculates and applies the experience penalty for a creature's death.<br>
	 * This method determines the percentage of experience lost based on the level and the type of killer.<br>
	 * It also accounts for specific stats, karma status, and war conditions to adjust the final amount.<br>
	 * The resulting loss is capped at 10% of the current level's total experience.
	 * @param killer The {@code Creature} that killed the entity.
	 */
	public void calculateDeathExpPenalty(Creature killer)
	{
		final int level = getLevel();
		double percentLost = ExperienceLossData.getInstance().getPercentLost(level);
		
		// Calculate reduce exp lost stats.
		if (killer != null)
		{
			if (killer.isRaid())
			{
				percentLost *= getStat().getValue(Stat.REDUCE_EXP_LOST_BY_RAID, 1);
			}
			else if (killer.isMonster())
			{
				percentLost *= getStat().getValue(Stat.REDUCE_EXP_LOST_BY_MOB, 1);
			}
			else if (killer.isPlayable())
			{
				percentLost *= getStat().getValue(Stat.REDUCE_EXP_LOST_BY_PVP, 1);
			}
		}
		
		// Calculate karma loss configuration.
		if (getReputation() < 0)
		{
			percentLost *= RatesConfig.RATE_KARMA_EXP_LOST;
		}
		
		// Get the total exp for the current player level.
		long currentLevelExp = 0;
		if (!isOnEvent())
		{
			final int maxLevel = ExperienceData.getInstance().getMaxLevel();
			if (level < maxLevel)
			{
				currentLevelExp = getStat().getExpForLevel(level + 1) - getStat().getExpForLevel(level);
			}
			else
			{
				currentLevelExp = getStat().getExpForLevel(maxLevel) - getStat().getExpForLevel(maxLevel - 1);
			}
		}
		
		// Calculate the experience loss.
		long lostExp = 0;
		if (currentLevelExp > 0)
		{
			lostExp = Math.round((currentLevelExp * percentLost) / 100);
			
			// Capping loss to 10% of current level experience.
			lostExp = Math.min(lostExp, currentLevelExp / 10);
			
			// Reduce experience lost for war kills.
			if ((killer != null) && killer.isPlayable() && atWarWith(killer.asPlayer()))
			{
				lostExp /= 4;
			}
		}
		
		setExpBeforeDeath(getExp());
		getStat().removeExp(lostExp);
	}
	
	/**
	 * Stops all active background tasks and timers for this actor.<br>
	 * This method cleans up various periodic updates such as HP regeneration, pet data, and task cycles.
	 */
	public void stopAllTimers()
	{
		stopHpMpRegeneration();
		stopWarnUserTakeBreak();
		stopWaterTask();
		stopFeed();
		clearPetData();
		storePetFood(_mountNpcId);
		stopRentPet();
		stopPvpRegTask();
		stopSoulTask();
		stopChargeTask();
		stopFameTask();
		_recommendations.stopGiveTask();
	}
	
	/**
	 * Retrieves the current pet associated with this actor.
	 * @return the {@code Pet} object or {@code null} if no pet exists.
	 */
	@Override
	public Pet getPet()
	{
		return _pet;
	}
	
	/**
	 * Retrieves the map of all active servitors.<br>
	 * The keys represent the unique IDs of the servitors.
	 * @return a {@code Map<Integer, Summon>} containing the servitors.
	 */
	@Override
	public Map<Integer, Summon> getServitors()
	{
		return _servitors;
	}
	
	/**
	 * Retrieves a random servitor from the current collection.<br>
	 * This method returns {@code null} if no servitors are available.
	 * @return A {@code Summon} object or {@code null}.
	 */
	public Summon getAnyServitor()
	{
		return getServitors().values().stream().findAny().orElse(null);
	}
	
	/**
	 * Retrieves the first available servitor from the list.<br>
	 * This method returns {@code null} if no servitors exist.
	 * @return The first {@code Summon} object found, or {@code null}.
	 */
	public Summon getFirstServitor()
	{
		final Map<Integer, Summon> servitors = getServitors();
		if (servitors.isEmpty())
		{
			return null;
		}
		
		return servitors.values().iterator().next();
	}
	
	/**
	 * Retrieves a specific {@code Summon} based on its unique ID.<br>
	 * This method looks up the servitor in the internal collection.
	 * @param objectId The unique identifier of the servitor to find.
	 * @return The {@code Summon} object associated with the provided ID, or {@code null} if not found.
	 */
	@Override
	public Summon getServitor(int objectId)
	{
		return getServitors().get(objectId);
	}
	
	/**
	 * Retrieves a list of all active summons for the player.<br>
	 * This includes all servitors and the primary pet.
	 * @return A {@code List} containing all {@code Summon} objects.
	 */
	public List<Summon> getServitorsAndPets()
	{
		final List<Summon> summons = new ArrayList<>();
		summons.addAll(getServitors().values());
		if (_pet != null)
		{
			summons.add(_pet);
		}
		
		return summons;
	}
	
	/**
	 * Retrieves the first {@code Trap} found among the summoned NPCs.<br>
	 * This method iterates through all summoned NPCs to find one that is a trap.<br>
	 * It returns {@code null} if no such NPC exists.
	 * @return The first {@code Trap} object found, or {@code null}.
	 */
	public Trap getTrap()
	{
		for (Npc npc : getSummonedNpcs())
		{
			if (npc.isTrap())
			{
				return (Trap) npc;
			}
		}
		
		return null;
	}
	
	/**
	 * Sets the current {@code Pet} for this actor.<br>
	 * This method updates the internal {@code _pet} field.
	 * @param pet The {@code Pet} object to assign.
	 */
	public void setPet(Pet pet)
	{
		_pet = pet;
	}
	
	/**
	 * Adds a new {@code Summon} to the internal collection.<br>
	 * This method stores the {@code Summon} using its unique object ID as the key.
	 * @param servitor The {@code Summon} instance to be added.
	 */
	public void addServitor(Summon servitor)
	{
		_servitors.put(servitor.getObjectId(), servitor);
	}
	
	/**
	 * Retrieves the set of tamed beasts owned by the player.
	 * @return a {@code Set} containing all {@code TamedBeast} objects.
	 */
	public Set<TamedBeast> getTrainedBeasts()
	{
		return _tamedBeast;
	}
	
	/**
	 * Adds a new {@code TamedBeast} to the internal collection.<br>
	 * This method registers the beast so it can be managed by the owner.
	 * @param tamedBeast The {@code TamedBeast} object to add.
	 */
	public void addTrainedBeast(TamedBeast tamedBeast)
	{
		_tamedBeast.add(tamedBeast);
	}
	
	/**
	 * Retrieves the current {@code RequestPartner} object.
	 * @return the {@code RequestPartner} associated with this entity.
	 */
	public RequestPartner getRequest()
	{
		return _request;
	}
	
	/**
	 * Sets the current active requester for this actor.<br>
	 * This method updates the {@code _activeRequester} field.
	 * @param requester The {@code Player} who is making the request.
	 */
	public void setActiveRequester(Player requester)
	{
		_activeRequester = requester;
	}
	
	/**
	 * Retrieves the current active requester.<br>
	 * This method checks if the request has expired and clears it if necessary.
	 * @return the {@code Player} who made the active request, or {@code null} if none exists.
	 */
	public Player getActiveRequester()
	{
		final Player requester = _activeRequester;
		if ((requester != null) && requester.isRequestExpired() && (_activeTradeList == null))
		{
			_activeRequester = null;
		}
		
		return _activeRequester;
	}
	
	/**
	 * Checks if the actor is currently processing a request.<br>
	 * It returns {@code true} if there is an active requester or if the request has not expired yet.
	 * @return {@code true} if a request is being processed, {@code false} otherwise.
	 */
	public boolean isProcessingRequest()
	{
		return (getActiveRequester() != null) || (_requestExpireTime > GameTimeTaskManager.getInstance().getGameTicks());
	}
	
	/**
	 * Checks if the player is currently involved in a transaction.<br>
	 * This includes active requests, trade lists, or pending request times.
	 * @return {@code true} if any transaction activity is detected, {@code false} otherwise.
	 */
	public boolean isProcessingTransaction()
	{
		return (getActiveRequester() != null) || (_activeTradeList != null) || (_requestExpireTime > GameTimeTaskManager.getInstance().getGameTicks());
	}
	
	/**
	 * Sets the expiration time for the current request.<br>
	 * This method calculates the deadline based on the current game ticks.<br>
	 * It uses the {@code REQUEST_TIMEOUT} constant to determine how long the request is valid.
	 */
	public void blockRequest()
	{
		_requestExpireTime = GameTimeTaskManager.getInstance().getGameTicks() + (REQUEST_TIMEOUT * GameTimeTaskManager.TICKS_PER_SECOND);
	}
	
	/**
	 * Handles a transaction request from another player.<br>
	 * This method sets the expiration time for the current request.<br>
	 * It also registers this actor as the active requester for the {@code partner}.
	 * @param partner The {@code Player} who initiated the transaction.
	 */
	public void onTransactionRequest(Player partner)
	{
		_requestExpireTime = GameTimeTaskManager.getInstance().getGameTicks() + (REQUEST_TIMEOUT * GameTimeTaskManager.TICKS_PER_SECOND);
		partner.setActiveRequester(this);
	}
	
	/**
	 * Checks if the current request has expired.<br>
	 * It compares the expiration time against the current game ticks.
	 * @return {@code true} if the request is expired, {@code false} otherwise.
	 */
	public boolean isRequestExpired()
	{
		return _requestExpireTime <= GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	/**
	 * Resets the request expiration time to {@code 0}.<br>
	 * This method is called when a transaction response is received.
	 */
	public void onTransactionResponse()
	{
		_requestExpireTime = 0;
	}
	
	/**
	 * Sets the current active warehouse for the player.<br>
	 * This updates the {@code _activeWarehouse} field with the provided {@code ItemContainer}.
	 * @param warehouse The {@code ItemContainer} to set as active.
	 */
	public void setActiveWarehouse(ItemContainer warehouse)
	{
		_activeWarehouse = warehouse;
	}
	
	/**
	 * Retrieves the currently active warehouse for the player.
	 * @return the {@code ItemContainer} of the active warehouse.
	 */
	public ItemContainer getActiveWarehouse()
	{
		return _activeWarehouse;
	}
	
	/**
	 * Sets the current active list of trades.<br>
	 * This updates the {@code _activeTradeList} field with the provided {@code TradeList}.
	 * @param tradeList The new {@code TradeList} to set as active.
	 */
	public void setActiveTradeList(TradeList tradeList)
	{
		_activeTradeList = tradeList;
	}
	
	/**
	 * Retrieves the list of currently active trades.
	 * @return a {@code TradeList} containing all active trade entries.
	 */
	public TradeList getActiveTradeList()
	{
		return _activeTradeList;
	}
	
	/**
	 * This method is called when a trade starts.<br>
	 * It initializes the {@code TradeList} for the current session.<br>
	 * It sends a system message and a packet to notify the player.
	 * @param partner The {@code Player} that the character is starting a trade with.
	 */
	public void onTradeStart(Player partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);
		
		final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_BEGIN_TRADING_WITH_C1);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(new TradeStart(1, this));
		sendPacket(new TradeStart(2, this));
	}
	
	/**
	 * This method is called when a trade has been confirmed.<br>
	 * It sends a confirmation message to the player.<br>
	 * It also notifies the client that the trade with the other party is finished.
	 * @param partner The {@code Player} who was involved in the trade.
	 */
	public void onTradeConfirm(Player partner)
	{
		final SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_CONFIRMED_THE_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(TradeOtherDone.STATIC_PACKET);
	}
	
	/**
	 * Handles the logic when a trade is cancelled.<br>
	 * It clears the active trade list and notifies the partner.<br>
	 * A system message is sent to inform the player of the cancellation.
	 * @param partner The {@code Player} who was involved in the trade.
	 */
	public void onTradeCancel(Player partner)
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
		_activeTradeList.lock();
		_activeTradeList = null;
		sendPacket(new TradeDone(0));
		final SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_CANCELLED_THE_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
	}
	
	/**
	 * Handles the completion of a trade between players.<br>
	 * It clears the active trade list and sends a notification packet.<br>
	 * If {@code successfull} is {@code true}, it also sends a success message.
	 * @param successfull Indicates whether the trade was completed successfully.
	 */
	public void onTradeFinish(boolean successfull)
	{
		_activeTradeList = null;
		sendPacket(new TradeDone(1));
		if (successfull)
		{
			sendPacket(SystemMessageId.YOUR_TRADE_WAS_SUCCESSFUL);
		}
	}
	
	/**
	 * Initiates a trade session with another player.<br>
	 * This method notifies both participants that the trade has begun.
	 * @param partner The {@code Player} to start trading with.
	 */
	public void startTrade(Player partner)
	{
		onTradeStart(partner);
		partner.onTradeStart(this);
	}
	
	/**
	 * Cancels the current active trade for this player.<br>
	 * It notifies the trading partner to cancel their trade as well.<br>
	 * This method ensures that both parties are informed of the cancellation.
	 */
	public void cancelActiveTrade()
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
		final Player partner = _activeTradeList.getPartner();
		if (partner != null)
		{
			partner.onTradeCancel(this);
		}
		
		onTradeCancel(this);
	}
	
	/**
	 * Retrieves the name of the store.
	 * @return The {@code String} name of the store.
	 */
	public String getStoreName()
	{
		return _storeName;
	}
	
	/**
	 * Sets the store name for the player.<br>
	 * If the provided {@code name} is {@code null}, it defaults to an empty string.
	 * @param name The name of the store to set.
	 */
	public void setStoreName(String name)
	{
		_storeName = name == null ? "" : name;
	}
	
	/**
	 * Retrieves the list of items currently being sold.<br>
	 * If the list does not exist, it creates a new {@code TradeList}.
	 * @return a {@code TradeList} containing the sell items.
	 */
	public TradeList getSellList()
	{
		if (_sellList == null)
		{
			_sellList = new TradeList(this);
		}
		
		return _sellList;
	}
	
	/**
	 * Retrieves the list of items currently being bought.<br>
	 * If the list does not exist, it creates a new {@code TradeList}.
	 * @return a {@code TradeList} containing the buy items.
	 */
	public TradeList getBuyList()
	{
		if (_buyList == null)
		{
			_buyList = new TradeList(this);
		}
		
		return _buyList;
	}
	
	/**
	 * Sets the type of private store for this player.<br>
	 * This method also handles removing the trader if the store is set to {@code NONE}.
	 * @param privateStoreType The new {@code PrivateStoreType} to assign.
	 */
	public void setPrivateStoreType(PrivateStoreType privateStoreType)
	{
		_privateStoreType = privateStoreType;
		if (OfflineTradeConfig.OFFLINE_DISCONNECT_FINISHED && (privateStoreType == PrivateStoreType.NONE) && _offlineMode.isEnabled())
		{
			OfflineTraderTable.getInstance().removeTrader(getObjectId());
			Disconnection.of(this).storeAndDelete();
		}
	}
	
	/**
	 * Retrieves the current type of the private store.
	 * @return the {@code PrivateStoreType} associated with this entity.
	 */
	public PrivateStoreType getPrivateStoreType()
	{
		return _privateStoreType;
	}
	
	/**
	 * Sets the {@code Clan} associated with this character.<br>
	 * If the provided {@code Clan} is {@code null}, all clan-related data is reset.<br>
	 * This method also verifies that the character is a member of the specified {@code Clan}.
	 * @param clan The {@code Clan} to assign to this character.
	 */
	public void setClan(Clan clan)
	{
		_clan = clan;
		if (clan == null)
		{
			setTitle("");
			_clanId = 0;
			_clanPrivileges = new ClanPrivileges();
			_pledgeType = 0;
			_powerGrade = 0;
			_lvlJoinedAcademy = 0;
			_apprentice = 0;
			_sponsor = 0;
			_activeWarehouse = null;
			PlayerInfoTable.getInstance().removeClanId(getObjectId());
			return;
		}
		
		if (!clan.isMember(getObjectId()))
		{
			// Char has been kicked from clan.
			setClan(null);
			return;
		}
		
		_clanId = clan.getId();
		PlayerInfoTable.getInstance().setClanId(getObjectId(), _clanId);
	}
	
	/**
	 * Retrieves the {@code Clan} associated with this actor.
	 * @return the current {@code Clan} object.
	 */
	@Override
	public Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * Checks if the current player is the leader of their clan.<br>
	 * It returns {@code false} if the player is not in a clan.<br>
	 * Otherwise, it compares the player's ID to the clan leader ID.
	 * @return {@code true} if the player is the clan leader, {@code false} otherwise.
	 */
	public boolean isClanLeader()
	{
		if (_clan == null)
		{
			return false;
		}
		
		return getObjectId() == _clan.getLeaderId();
	}
	
	/**
	 * Checks if the character has ammunition and equips it to the left hand.<br>
	 * This method handles both arrows and bolts based on the provided {@code EtcItemType}.<br>
	 * It updates the inventory and sends a notification to the client if an item is equipped.
	 * @param type The type of ammunition to check for, such as {@code EtcItemType.ARROW} or {@code EtcItemType.BOLT}.
	 * @return {@code true} if ammunition is already equipped or was successfully equipped; {@code false} otherwise.
	 */
	@Override
	protected boolean checkAndEquipAmmunition(EtcItemType type)
	{
		Item ammunition = null;
		final Weapon weapon = getActiveWeaponItem();
		if (type == EtcItemType.ARROW)
		{
			ammunition = _inventory.findArrowForBow(weapon);
		}
		else if (type == EtcItemType.BOLT)
		{
			ammunition = _inventory.findBoltForCrossBow(weapon);
		}
		
		if (ammunition != null)
		{
			addAmmunitionSkills(ammunition);
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(ammunition);
			sendInventoryUpdate(iu);
			return true;
		}
		
		removeAmmunitionSkills();
		return false;
	}
	
	/**
	 * Updates the active skills based on a new ammunition item.<br>
	 * This method removes old skills and adds passive skills from the provided {@code Item}.<br>
	 * It also triggers a skill list update if any new passive skills are added.
	 * @param ammunition The {@code Item} containing the new ammunition data.
	 */
	private void addAmmunitionSkills(Item ammunition)
	{
		final int currentAmmunitionId = ammunition.getId();
		if (_lastAmmunitionId == currentAmmunitionId)
		{
			return;
		}
		
		removeAmmunitionSkills();
		_lastAmmunitionId = currentAmmunitionId;
		
		final List<ItemSkillHolder> skills = ammunition.getTemplate().getAllSkills();
		if (skills == null)
		{
			return;
		}
		
		boolean sendSkillList = false;
		for (ItemSkillHolder holder : skills)
		{
			if (!isAffectedBySkill(holder))
			{
				final Skill skill = holder.getSkill();
				if (skill.isPassive())
				{
					addSkill(skill);
					sendSkillList = true;
				}
			}
		}
		
		if (sendSkillList)
		{
			sendSkillList();
		}
	}
	
	/**
	 * Removes all ammunition skills from the actor.<br>
	 * This method checks if {@code _lastAmmunitionId} is non-zero before proceeding.<br>
	 * It iterates through {@code AmmunitionSkillList} and removes each skill.<br>
	 * If any skill is removed, it triggers a {@code sendSkillList()} update.
	 */
	public void removeAmmunitionSkills()
	{
		if (_lastAmmunitionId == 0)
		{
			return;
		}
		
		_lastAmmunitionId = 0;
		
		boolean sendSkillList = false;
		for (Integer skillId : AmmunitionSkillList.values())
		{
			if (removeSkill(skillId.intValue(), true) != null)
			{
				sendSkillList = true;
			}
		}
		
		if (sendSkillList)
		{
			sendSkillList();
		}
	}
	
	/**
	 * Checks if the player is allowed to equip a specific item.<br>
	 * This method enforces class-specific restrictions for Death Knights, Shine Makers, and Samurai.<br>
	 * It also handles special cases for fishing rods and specific weapon IDs.
	 * @param item The {@code Item} to check for equipment eligibility.
	 * @return {@code true} if the player can equip the item, {@code false} otherwise.
	 */
	public boolean canUseEquipment(Item item)
	{
		if (item.isArmor())
		{
			// Prevent equip shields for Death Knight, Shine Maker or Samurai players.
			if (item.getItemType() == ArmorType.SHIELD)
			{
				final int classId = getPlayerClass().getId();
				if (CategoryData.getInstance().isInCategory(CategoryType.DEATH_KNIGHT_ALL_CLASS, classId) || CategoryData.getInstance().isInCategory(CategoryType.SHINE_MAKER_ALL_CLASS, classId) || isSamurai())
				{
					return false;
				}
			}
		}
		else if (item.isWeapon())
		{
			// Allow equipping Sword Cursed - version Prelude of War 2019
			final int itemId = item.getId();
			if ((itemId == 8190) || (itemId == 8689))
			{
				return true;
			}
			
			// Fishing rods are enabled for all players.
			final ItemType itemType = item.getItemType();
			if (itemType == WeaponType.FISHINGROD)
			{
				return item.isEquipable();
			}
			
			// Prevent Death Knight players to equip other weapons than One Hand Swords.
			final int classId = getPlayerClass().getId();
			if (CategoryData.getInstance().isInCategory(CategoryType.DEATH_KNIGHT_ALL_CLASS, classId))
			{
				if ((itemType != WeaponType.SWORD) || (item.getTemplate().getBodyPart() == BodyPart.LR_HAND))
				{
					return false;
				}
			}
			// Prevent Shine Maker players to equip other weapons than Two Hand Swords or Bows.
			else if (CategoryData.getInstance().isInCategory(CategoryType.SHINE_MAKER_ALL_CLASS, classId))
			{
				if ((itemType != WeaponType.BOW) && ((itemType != WeaponType.SWORD) || (item.getTemplate().getBodyPart() != BodyPart.LR_HAND)))
				{
					return false;
				}
			}
			// Prevent Samurai players to equip other weapons than Swords.
			else if (CategoryData.getInstance().isInCategory(CategoryType.SAMURAI_ALL_CLASS, classId))
			{
				if (itemType != WeaponType.SWORD)
				{
					return false;
				}
			}
		}
		
		return item.isEquipable();
	}
	
	/**
	 * This method checks all equipped items on the player.<br>
	 * It removes any equipment that cannot be used by the character.<br>
	 * The removed items are automatically unequipped from their body parts.<br>
	 * A system message is sent to notify the player of the action.
	 */
	public void disarmUnusableEquipment()
	{
		final InventoryUpdate iu = new InventoryUpdate();
		final PlayerInventory inventory = getInventory();
		for (Item equipped : inventory.getPaperdollItems())
		{
			if ((equipped != null) && !canUseEquipment(equipped))
			{
				final BodyPart bodyPart = BodyPart.fromItem(equipped);
				inventory.unEquipItemInBodySlot(bodyPart);
				iu.addModifiedItem(equipped);
				
				final SystemMessage sm;
				if (equipped.isEnchanted())
				{
					sm = new SystemMessage(SystemMessageId.S1_S2_UNEQUIPPED);
					sm.addInt(equipped.getEnchantLevel());
					sm.addItemName(equipped);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_UNEQUIPPED);
					sm.addItemName(equipped);
				}
				
				sendPacket(sm);
			}
		}
		
		if (!iu.getItems().isEmpty())
		{
			sendInventoryUpdate(iu);
			broadcastUserInfo();
		}
	}
	
	/**
	 * Removes the weapon from the right hand slot.<br>
	 * Returns {@code true} if the weapon was successfully disarmed or if no weapon was equipped.<br>
	 * Returns {@code false} if the weapon cannot be removed due to restrictions like curses, combat flags, or force equip status.
	 * @return {@code true} if successful; {@code false} otherwise.
	 */
	public boolean disarmWeapons()
	{
		// If there is no weapon to disarm then return true.
		final Item wpn = _inventory.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
		{
			return true;
		}
		
		// Don't allow disarming a cursed weapon.
		if (isCursedWeaponEquipped())
		{
			return false;
		}
		
		// Don't allow disarming a Combat Flag or Territory Ward.
		if (_combatFlagEquippedId)
		{
			return false;
		}
		
		// Don't allow disarming if the weapon is force equip.
		if (wpn.getWeaponItem().isForceEquip())
		{
			return false;
		}
		
		final Collection<Item> unequipped = _inventory.unEquipItemInBodySlotAndRecord(wpn.getTemplate().getBodyPart());
		final InventoryUpdate iu = new InventoryUpdate();
		for (Item itm : unequipped)
		{
			iu.addModifiedItem(itm);
		}
		
		sendInventoryUpdate(iu);
		abortAttack();
		broadcastUserInfo();
		
		// This can be 0 if the user pressed the right mousebutton twice very fast.
		if (!unequipped.isEmpty())
		{
			final SystemMessage sm;
			final Item unequippedItem = unequipped.iterator().next();
			if (unequippedItem.isEnchanted())
			{
				sm = new SystemMessage(SystemMessageId.S1_S2_UNEQUIPPED);
				sm.addInt(unequippedItem.getEnchantLevel());
				sm.addItemName(unequippedItem);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_UNEQUIPPED);
				sm.addItemName(unequippedItem);
			}
			
			sendPacket(sm);
		}
		
		return true;
	}
	
	/**
	 * Removes the shield from the left hand slot.<br>
	 * This method handles unequipping the item and updating the inventory.<br>
	 * It also cancels any current attacks and broadcasts updated user information.
	 * @return {@code true} if the operation was successful.
	 */
	public boolean disarmShield()
	{
		final Item sld = _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (sld != null)
		{
			final Collection<Item> unequipped = _inventory.unEquipItemInBodySlotAndRecord(sld.getTemplate().getBodyPart());
			final InventoryUpdate iu = new InventoryUpdate();
			for (Item itm : unequipped)
			{
				iu.addModifiedItem(itm);
			}
			
			sendInventoryUpdate(iu);
			
			abortAttack();
			broadcastUserInfo();
			
			// This can be 0 if the user pressed the right mousebutton twice very fast.
			if (!unequipped.isEmpty())
			{
				SystemMessage sm = null;
				final Item unequippedItem = unequipped.iterator().next();
				if (unequippedItem.isEnchanted())
				{
					sm = new SystemMessage(SystemMessageId.S1_S2_UNEQUIPPED);
					sm.addInt(unequippedItem.getEnchantLevel());
					sm.addItemName(unequippedItem);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_UNEQUIPPED);
					sm.addItemName(unequippedItem);
				}
				
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Attempts to mount the provided {@code Summon} pet.<br>
	 * Checks for siege restrictions and player status before mounting.<br>
	 * Updates the actor's state and broadcasts the ride packet.
	 * @param pet The {@code Summon} object to be mounted.
	 * @return {@code true} if the mount was successful, {@code false} otherwise.
	 */
	public boolean mount(Summon pet)
	{
		// If mounts (excluding wyvern) are disallowed during siege, check if player is in a siege zone.
		if (!FeatureConfig.ALLOW_MOUNTS_DURING_SIEGE && isInsideZone(ZoneId.SIEGE))
		{
			final MountType type = MountType.findByNpcId(pet.getId());
			
			// Only allow mounting if the pet is a Strider and the player is the castle lord of the current siege castle.
			final Castle castle = CastleManager.getInstance().getCastle(this);
			final boolean isAllowed = (type == MountType.STRIDER) && (castle != null) && isClanLeader() && (getClanId() == castle.getOwnerId());
			
			if (!isAllowed)
			{
				sendMessage("You cannot mount during a siege.");
				return false;
			}
		}
		
		if (!disarmWeapons() || !disarmShield() || isTransformed())
		{
			return false;
		}
		
		getEffectList().stopAllToggles();
		final int petId = pet.getId();
		setMount(petId, pet.getLevel());
		setMountObjectID(pet.getControlObjectId());
		clearPetData();
		startFeed(petId);
		broadcastPacket(new Ride(this));
		
		// Notify self and others about speed change.
		broadcastUserInfo();
		
		pet.unSummon(this);
		return true;
	}
	
	/**
	 * Mounts a specific NPC as a ride for the player.<br>
	 * This method checks if the player is ready to mount before proceeding.<br>
	 * It updates the player's status and broadcasts the change to others.
	 * @param npcId The unique identifier of the NPC to be used as a mount.
	 * @param controlItemObjId The object ID for the item that controls the mount.
	 * @param useFood Whether to start feeding the mount automatically.
	 * @return {@code true} if the mounting process was successful, {@code false} otherwise.
	 */
	public boolean mount(int npcId, int controlItemObjId, boolean useFood)
	{
		if (!disarmWeapons() || !disarmShield() || isTransformed())
		{
			return false;
		}
		
		getEffectList().stopAllToggles();
		setMount(npcId, getLevel());
		clearPetData();
		setMountObjectID(controlItemObjId);
		broadcastPacket(new Ride(this));
		
		// Notify self and others about speed change.
		broadcastUserInfo();
		if (useFood)
		{
			startFeed(npcId);
		}
		
		return true;
	}
	
	/**
	 * Attempts to mount the player on a provided {@code Summon} pet.<br>
	 * This method checks if the player and the pet meet all requirements for mounting.<br>
	 * If the player is already mounted, it will attempt to dismount them instead.
	 * @param pet The {@code Summon} object that the player wants to ride.
	 * @return {@code true} if the mount or dismount action was successful, otherwise {@code false}.
	 */
	public boolean mountPlayer(Summon pet)
	{
		if ((pet != null) && pet.isMountable() && !isMounted() && !isBetrayed())
		{
			if (isDead())
			{
				// A strider cannot be ridden when dead.
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOU_CANNOT_USE_A_MOUNT_WHILE_DEAD);
				return false;
			}
			else if (pet.isDead())
			{
				// A dead strider cannot be ridden.
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOU_CANNOT_USE_A_DEAD_MOUNT);
				return false;
			}
			else if (pet.isInCombat() || pet.isRooted())
			{
				// A strider in battle cannot be ridden.
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOU_CANNOT_USE_A_MOUNT_THAT_IS_IN_BATTLE);
				return false;
			}
			else if (isInCombat())
			{
				// A strider cannot be ridden while in battle.
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_MOUNT_CANNOT_BE_RIDDEN_WHILE_IN_BATTLE);
				return false;
			}
			else if (_waitTypeSitting)
			{
				// A strider can be ridden only when standing.
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_MOUNT_CAN_BE_RIDDEN_ONLY_WHEN_STANDING);
				return false;
			}
			else if (isFishing())
			{
				// You can't mount, dismount, break and drop items while fishing.
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_2);
				return false;
			}
			else if (isTransformed() || isCursedWeaponEquipped())
			{
				// No message needed, player while transformed doesn't have mount action.
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			else if (_inventory.getItemByItemId(9819) != null)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				
				// FIXME: Wrong Message
				sendMessage("You cannot mount a steed while holding a flag.");
				return false;
			}
			else if (pet.isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_HUNGRY_MOUNT_CANNOT_BE_MOUNTED_OR_DISMOUNTED);
				return false;
			}
			else if (!LocationUtil.checkIfInRange(200, this, pet, true))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOU_ARE_TOO_FAR_AWAY_FROM_YOUR_MOUNT_TO_RIDE);
				return false;
			}
			else if (!pet.isDead() && !isMounted())
			{
				mount(pet);
			}
		}
		else if (isRentedPet())
		{
			stopRentPet();
		}
		else if (isMounted())
		{
			if ((_mountType == MountType.WYVERN) && isInsideZone(ZoneId.NO_LANDING) && !isGM())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOU_ARE_NOT_ALLOWED_TO_DISMOUNT_IN_THIS_LOCATION);
				return false;
			}
			else if (isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_HUNGRY_MOUNT_CANNOT_BE_MOUNTED_OR_DISMOUNTED);
				return false;
			}
			else
			{
				dismount();
			}
		}
		
		return true;
	}
	
	/**
	 * Attempts to dismount the player from their current mount.<br>
	 * Checks if the location is valid for dismounting.<br>
	 * Sends a failure packet and returns {@code false} if conditions are not met.<br>
	 * Updates the player state and broadcasts the change on success.
	 * @return {@code true} if the player successfully dismounted, or {@code false} otherwise.
	 */
	public boolean dismount()
	{
		if (ZoneManager.getInstance().getZone(getX(), getY(), getZ() - 300, WaterZone.class) == null)
		{
			final int z = getZ();
			if (!isGM() && !isInWater() && (z > 10000))
			{
				sendPacket(SystemMessageId.YOU_ARE_NOT_ALLOWED_TO_DISMOUNT_IN_THIS_LOCATION);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			// Skip elevation check only in NoLandingZone.
			if (!isInsideZone(ZoneId.NO_LANDING) && ((GeoEngine.getInstance().getHeight(getX(), getY(), z) + 300) < z))
			{
				sendPacket(SystemMessageId.YOU_CANNOT_DISMOUNT_FROM_THIS_ELEVATION);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		else
		{
			ThreadPool.schedule(() ->
			{
				if (isInWater())
				{
					broadcastUserInfo();
				}
			}, 1500);
		}
		
		final boolean wasFlying = isFlying();
		sendPacket(new SetupGauge(3, 0, 0));
		final int petId = _mountNpcId;
		setMount(0, 0);
		stopFeed();
		clearPetData();
		if (wasFlying)
		{
			removeSkill(CommonSkill.WYVERN_BREATH.getSkill());
		}
		
		broadcastPacket(new Ride(this));
		setMountObjectID(0);
		storePetFood(petId);
		
		// Notify self and others about speed change.
		broadcastUserInfo();
		
		return true;
	}
	
	/**
	 * Sets the current uptime for the actor.<br>
	 * This updates the internal {@code _uptime} field with the provided value.
	 * @param time The new uptime value to set.
	 */
	public void setUptime(long time)
	{
		_uptime = time;
	}
	
	/**
	 * Calculates the total time the server has been running.<br>
	 * It returns the difference between the current system time and the start time.
	 * @return The uptime in milliseconds as a {@code long}.
	 */
	public long getUptime()
	{
		return System.currentTimeMillis() - _uptime;
	}
	
	/**
	 * Checks if the actor is currently invulnerable.<br>
	 * This method returns {@code true} if the actor is invulnerable or protected from teleportation.
	 * @return {@code true} if the actor cannot be damaged or moved, {@code false} otherwise.
	 */
	@Override
	public boolean isInvul()
	{
		return super.isInvul() || isTeleportProtected();
	}
	
	/**
	 * Checks if the actor is currently in a party.<br>
	 * Returns {@code true} if the actor belongs to a party.<br>
	 * Returns {@code false} if the actor is not in a party.
	 * @return {@code true} if in a party, {@code false} otherwise.
	 */
	@Override
	public boolean isInParty()
	{
		return _party != null;
	}
	
	/**
	 * Sets the current {@code Party} for this actor.<br>
	 * This method updates the internal {@code _party} field.
	 * @param party The {@code Party} object to assign.
	 */
	public void setParty(Party party)
	{
		_party = party;
	}
	
	/**
	 * Adds the current actor to a specific party.<br>
	 * This method updates the internal {@code _party} field.<br>
	 * It also adds this actor as a member to the provided {@code Party}.
	 * @param party The {@code Party} object to join.
	 */
	public void joinParty(Party party)
	{
		if (party != null)
		{
			// First set the party otherwise this wouldn't be considered.
			// as in a party into the Creature.updateEffectIcons() call.
			_party = party;
			party.addPartyMember(this);
		}
	}
	
	/**
	 * Removes the actor from their current party.<br>
	 * This method sets the internal {@code _party} reference to {@code null}.<br>
	 * It notifies the party that the member has disconnected.
	 */
	public void leaveParty()
	{
		if (isInParty())
		{
			_party.removePartyMember(this, PartyMessageType.DISCONNECTED);
			_party = null;
		}
	}
	
	/**
	 * Retrieves the {@code Party} associated with this actor.
	 * @return the current {@code Party} object.
	 */
	@Override
	public Party getParty()
	{
		return _party;
	}
	
	/**
	 * Checks if the player is currently in a command channel.<br>
	 * This method returns {@code true} only if the player is in a party and that party is in a command channel.
	 * @return {@code true} if the player is in a command channel, {@code false} otherwise.
	 */
	public boolean isInCommandChannel()
	{
		return isInParty() && _party.isInCommandChannel();
	}
	
	/**
	 * Retrieves the current command channel for this actor.<br>
	 * This method returns {@code null} if the actor is not currently in a command channel.
	 * @return the {@code CommandChannel} object or {@code null}.
	 */
	public CommandChannel getCommandChannel()
	{
		return (isInCommandChannel()) ? _party.getCommandChannel() : null;
	}
	
	/**
	 * Checks if the actor has Game Master privileges.<br>
	 * This method queries the current access level of the entity.
	 * @return {@code true} if the actor is a Game Master, {@code false} otherwise.
	 */
	@Override
	public boolean isGM()
	{
		return _accessLevel.isGm();
	}
	
	/**
	 * Updates the access level and visual appearance of the player.<br>
	 * This method also handles optional broadcasting and database synchronization.
	 * @param level The numeric ID of the access level to assign.
	 * @param broadcast Whether to send a global message when the level is changed.
	 * @param updateInDb Whether to save the new access level to the database.
	 */
	public void setAccessLevel(int level, boolean broadcast, boolean updateInDb)
	{
		AccessLevel accessLevel = AdminData.getInstance().getAccessLevel(level);
		if (accessLevel == null)
		{
			LOGGER.warning("Can't find access level " + level + " for " + this);
			accessLevel = AdminData.getInstance().getAccessLevel(0);
		}
		
		if ((accessLevel.getLevel() == 0) && (GeneralConfig.DEFAULT_ACCESS_LEVEL > 0))
		{
			accessLevel = AdminData.getInstance().getAccessLevel(GeneralConfig.DEFAULT_ACCESS_LEVEL);
			if (accessLevel == null)
			{
				LOGGER.warning("Config's default access level (" + GeneralConfig.DEFAULT_ACCESS_LEVEL + ") is not defined, defaulting to 0!");
				accessLevel = AdminData.getInstance().getAccessLevel(0);
				GeneralConfig.DEFAULT_ACCESS_LEVEL = 0;
			}
		}
		
		_accessLevel = accessLevel;
		_appearance.setNameColor(_accessLevel.getNameColor());
		_appearance.setTitleColor(_accessLevel.getTitleColor());
		if (broadcast)
		{
			broadcastUserInfo();
		}
		
		if (updateInDb)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_CHARACTER_ACCESS))
			{
				ps.setInt(1, accessLevel.getLevel());
				ps.setInt(2, getObjectId());
				ps.executeUpdate();
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, "Failed to update character's accesslevel in db: " + this, e);
			}
		}
		
		PlayerInfoTable.getInstance().addName(this);
		
		if (accessLevel == null)
		{
			LOGGER.warning("Tryed to set unregistered access level " + level + " for " + this + ". Setting access level without privileges!");
		}
		else if (level > 0)
		{
			LOGGER.warning(_accessLevel.getName() + " access level set for character " + _name + "! Just a warning to be careful ;)");
		}
	}
	
	/**
	 * Updates the access level for the current account.<br>
	 * This method notifies the {@code LoginServerThread} of the new permission level.
	 * @param level The new integer value representing the access level.
	 */
	public void setAccountAccesslevel(int level)
	{
		LoginServerThread.getInstance().sendAccessLevel(getAccountName(), level);
	}
	
	/**
	 * Retrieves the current access level of the actor.
	 * @return the {@code AccessLevel} of this actor.
	 */
	@Override
	public AccessLevel getAccessLevel()
	{
		return _accessLevel;
	}
	
	/**
	 * Schedules a task to update the player status.<br>
	 * This method refreshes overloaded and expertise penalty values.<br>
	 * It then broadcasts updated information to all relevant players.
	 */
	public void updateAndBroadcastStatus()
	{
		if (_updateAndBroadcastStatusTask == null)
		{
			_updateAndBroadcastStatusTask = ThreadPool.schedule(() ->
			{
				refreshOverloaded(true);
				
				// Send a Server->Client packet UserInfo to this Player and CharInfo to all known players (broadcast).
				broadcastUserInfo();
				
				_updateAndBroadcastStatusTask = null;
			}, 50);
		}
	}
	
	/**
	 * Sends a broadcast to all visible players regarding reputation status.<br>
	 * This method updates the {@code RelationChanged} packet for nearby entities.<br>
	 * It checks if the current relation or auto-attackable status has changed since the last update.
	 */
	public void broadcastReputation()
	{
		broadcastUserInfo(UserInfoType.SOCIAL);
		
		World.forEachVisibleObject(this, Player.class, player ->
		{
			if (!isVisibleFor(player))
			{
				return;
			}
			
			final long relation = getRelation(player);
			final boolean isAutoAttackable = isAutoAttackable(player);
			final int objectId = player.getObjectId();
			final RelationCache oldrelation = getKnownRelations().get(objectId);
			if ((oldrelation == null) || (oldrelation.getRelation() != relation) || (oldrelation.isAutoAttackable() != isAutoAttackable))
			{
				final RelationChanged rc = new RelationChanged();
				rc.addRelation(this, relation, isAutoAttackable);
				if (hasSummon())
				{
					if (_pet != null)
					{
						rc.addRelation(_pet, relation, isAutoAttackable);
					}
					
					if (hasServitors())
					{
						getServitors().values().forEach(s -> rc.addRelation(s, relation, isAutoAttackable));
					}
				}
				
				player.sendPacket(rc);
				getKnownRelations().put(objectId, new RelationCache(relation, isAutoAttackable));
			}
		});
	}
	
	/**
	 * Updates the online status of the player.<br>
	 * This method also optionally updates the database if requested.
	 * @param isOnline The new online status to set.
	 * @param updateInDb Whether to save the change to the database.
	 */
	public void setOnlineStatus(boolean isOnline, boolean updateInDb)
	{
		if (_isOnline != isOnline)
		{
			_isOnline = isOnline;
		}
		
		// Update the characters table of the database with online status and lastAccess (called when login and logout).
		if (updateInDb)
		{
			updateOnlineStatus();
		}
	}
	
	/**
	 * Updates the online status of the character in the database.<br>
	 * It sets the {@code online} flag and updates the {@code lastAccess} timestamp.<br>
	 * The operation uses the current object ID to identify the correct record.
	 */
	public void updateOnlineStatus()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE charId=?"))
		{
			statement.setInt(1, isOnlineInt());
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed updating character online status.", e);
		}
	}
	
	/**
	 * Creates a new character entry in the database.<br>
	 * This method inserts all player statistics and appearance data into the table.<br>
	 * It returns {@code true} if the insertion is successful.<br>
	 * It returns {@code false} if an error occurs during the database operation.
	 * @return {@code true} if the character was created successfully, otherwise {@code false}.
	 */
	private boolean createDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_CHARACTER))
		{
			statement.setString(1, _accountName);
			statement.setInt(2, getObjectId());
			statement.setString(3, _name);
			statement.setInt(4, getLevel());
			statement.setInt(5, (int) getMaxHp());
			statement.setDouble(6, getCurrentHp());
			statement.setInt(7, getMaxCp());
			statement.setDouble(8, getCurrentCp());
			statement.setInt(9, getMaxMp());
			statement.setDouble(10, getCurrentMp());
			statement.setInt(11, _appearance.getFace());
			statement.setInt(12, _appearance.getHairStyle());
			statement.setInt(13, _appearance.getHairColor());
			statement.setInt(14, _appearance.isFemale() ? 1 : 0);
			statement.setLong(15, getExp());
			statement.setLong(16, getSp());
			statement.setInt(17, getReputation());
			statement.setInt(18, _fame);
			statement.setInt(19, _raidbossPoints);
			statement.setInt(20, _pvpKills);
			statement.setInt(21, _pkKills);
			statement.setInt(22, _clanId);
			statement.setInt(23, getRace().ordinal());
			statement.setInt(24, getPlayerClass().getId());
			statement.setLong(25, _deleteTimer);
			statement.setInt(26, _createItemLevel > 0 ? 1 : 0);
			statement.setString(27, getTitle());
			statement.setInt(28, _appearance.getTitleColor());
			statement.setInt(29, isOnlineInt());
			statement.setInt(30, _clanPrivileges.getMask());
			statement.setInt(31, _wantsPeace);
			statement.setInt(32, _baseClass);
			statement.setInt(33, _nobleLevel);
			statement.setLong(34, 0);
			statement.setInt(35, PlayerStat.MIN_VITALITY_POINTS);
			statement.setDate(36, new Date(_createDate.getTimeInMillis()));
			statement.setLong(37, System.currentTimeMillis());
			statement.setInt(38, getTotalKills());
			statement.setInt(39, getTotalDeaths());
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not insert char data: " + e.getMessage(), e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * This method reconstructs a {@code Player} object from the database.<br>
	 * It loads all character data including stats, inventory, and skills.<br>
	 * The process uses the provided unique identifier to fetch records.
	 * @param objectId The unique ID of the player to restore.
	 * @return The fully populated {@code Player} object or {@code null} if not found.
	 */
	private static Player restore(int objectId)
	{
		Player player = null;
		double currentCp = 0;
		double currentHp = 0;
		double currentMp = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHARACTER))
		{
			// Retrieve the Player from the characters table of the database.
			statement.setInt(1, objectId);
			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					final int activeClassId = rset.getInt("classid");
					final boolean female = rset.getInt("sex") != Sex.MALE.ordinal();
					final PlayerTemplate template = PlayerTemplateData.getInstance().getTemplate(activeClassId);
					final PlayerAppearance app = new PlayerAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), female);
					player = new Player(objectId, template, rset.getString("account_name"), app);
					player.setName(rset.getString("char_name"));
					player.setLastAccess(rset.getLong("lastAccess"));
					
					final PlayerStat stat = player.getStat();
					stat.setExp(rset.getLong("exp"));
					player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
					stat.setLevel(rset.getInt("level"));
					stat.setSp(rset.getLong("sp"));
					
					player.setWantsPeace(rset.getInt("wantspeace"));
					
					player.setHeading(rset.getInt("heading"));
					
					player.setInitialReputation(rset.getInt("reputation"));
					player.setFame(rset.getInt("fame"));
					player.setRaidbossPoints(rset.getInt("raidbossPoints"));
					player.setPvpKills(rset.getInt("pvpkills"));
					player.setPkKills(rset.getInt("pkkills"));
					player.setOnlineTime(rset.getLong("onlinetime"));
					final int nobleLevel = rset.getInt("nobless");
					player.setNobleLevel(nobleLevel);
					
					final int factionId = rset.getInt("faction");
					if (factionId == 1)
					{
						player.setGood();
					}
					
					if (factionId == 2)
					{
						player.setEvil();
					}
					
					player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
					if (player.getClanJoinExpiryTime() < System.currentTimeMillis())
					{
						player.setClanJoinExpiryTime(0);
					}
					
					player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
					if (player.getClanCreateExpiryTime() < System.currentTimeMillis())
					{
						player.setClanCreateExpiryTime(0);
					}
					
					player.setPcCafePoints(rset.getInt("pccafe_points"));
					
					final int clanId = rset.getInt("clanid");
					player.setPowerGrade(rset.getInt("power_grade"));
					stat.setVitalityPoints(rset.getInt("vitality_points"));
					player.setPledgeType(rset.getInt("subpledge"));
					// player.setApprentice(rset.getInt("apprentice"));
					
					Clan clan = null;
					if (clanId > 0)
					{
						clan = ClanTable.getInstance().getClan(clanId);
						player.setClan(clan);
						if ((clan != null) && clan.isMember(objectId))
						{
							if (clan.getLeaderId() != player.getObjectId())
							{
								if (player.getPowerGrade() == 0)
								{
									player.setPowerGrade(5);
								}
								
								player.setClanPrivileges(clan.getRankPrivs(player.getPowerGrade()));
							}
							else
							{
								player.getClanPrivileges().enableAll();
								player.setPowerGrade(1);
							}
							
							player.setPledgeClass(ClanMember.calculatePledgeClass(player));
						}
					}
					
					if (clan == null)
					{
						if (nobleLevel > 0)
						{
							player.setPledgeClass(5);
						}
						
						if (player.isHero())
						{
							player.setPledgeClass(8);
						}
						
						player.getClanPrivileges().disableAll();
					}
					
					player.setTotalDeaths(rset.getInt("deaths"));
					player.setTotalKills(rset.getInt("kills"));
					player.setDeleteTimer(rset.getLong("deletetime"));
					player.setTitle(rset.getString("title"));
					player.setAccessLevel(rset.getInt("accesslevel"), false, false);
					final int titleColor = rset.getInt("title_color");
					if (titleColor != PlayerAppearance.DEFAULT_TITLE_COLOR)
					{
						player.getAppearance().setTitleColor(titleColor);
					}
					
					player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
					player.setUptime(System.currentTimeMillis());
					
					currentHp = rset.getDouble("curHp");
					currentCp = rset.getDouble("curCp");
					currentMp = rset.getDouble("curMp");
					player.setClassIndex(0);
					try
					{
						player.setBaseClass(rset.getInt("base_class"));
					}
					catch (Exception e)
					{
						player.setBaseClass(activeClassId);
						LOGGER.log(Level.WARNING, "Exception during player.setBaseClass for player: " + player + " base class: " + rset.getInt("base_class"), e);
					}
					
					// Restore Subclass Data (cannot be done earlier in function).
					if (restoreSubClassData(player) && (activeClassId != player.getBaseClass()))
					{
						for (SubClassHolder subClass : player.getSubClasses().values())
						{
							if (subClass.getId() == activeClassId)
							{
								player.setClassIndex(subClass.getClassIndex());
							}
						}
					}
					
					if ((player.getClassIndex() == 0) && (activeClassId != player.getBaseClass()))
					{
						// A subclass is in use but does not exist in the database, indicating a possible restart-while-modify subclass cheat attempt.
						// Switching to use base class.
						player.setPlayerClass(player.getBaseClass());
						LOGGER.warning(player + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
					}
					else
					{
						player._activeClass = activeClassId;
					}
					
					if (CategoryData.getInstance().isInCategory(CategoryType.DEATH_KNIGHT_ALL_CLASS, player.getBaseTemplate().getPlayerClass().getId()))
					{
						player._isDeathKnight = true;
					}
					else if (CategoryData.getInstance().isInCategory(CategoryType.SHINE_MAKER_ALL_CLASS, player.getBaseTemplate().getPlayerClass().getId()))
					{
						player._isShineMaker = true;
					}
					else if (CategoryData.getInstance().isInCategory(CategoryType.WARG_ALL_CLASS, player.getBaseTemplate().getPlayerClass().getId()))
					{
						player._isWarg = true;
					}
					else if (CategoryData.getInstance().isInCategory(CategoryType.SAMURAI_ALL_CLASS, player.getBaseTemplate().getPlayerClass().getId()))
					{
						player._isSamurai = true;
					}
					
					player.setApprentice(rset.getInt("apprentice"));
					player.setSponsor(rset.getInt("sponsor"));
					player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
					
					// Set Hero status if it applies.
					player.setHero(Hero.getInstance().isHero(objectId));
					
					CursedWeaponsManager.getInstance().checkPlayer(player);
					
					// Set the x,y,z position of the Player and make it invisible.
					final int x = rset.getInt("x");
					final int y = rset.getInt("y");
					final int z = GeoEngine.getInstance().getHeight(x, y, rset.getInt("z"));
					player.setXYZInvisible(x, y, z);
					player.setLastServerPosition(x, y, z);
					
					// Set Teleport Bookmark Slot.
					player.setBookMarkSlot(rset.getInt("BookmarkSlot"));
					
					player.getCreateDate().setTime(rset.getDate("createDate"));
					
					// Language
					player.setLang(rset.getString("language"));
					
					// Retrieve the name and ID of the other characters assigned to this account.
					try (PreparedStatement stmt = con.prepareStatement("SELECT charId, char_name FROM characters WHERE account_name=? AND charId<>? AND deletetime=0"))
					{
						stmt.setString(1, player._accountName);
						stmt.setInt(2, objectId);
						try (ResultSet chars = stmt.executeQuery())
						{
							while (chars.next())
							{
								player._chars.put(chars.getInt("charId"), chars.getString("char_name"));
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed loading character.", e);
		}
		
		if (player == null)
		{
			return null;
		}
		
		// Retrieve from the database all items of this Player and add them to _inventory.
		player.getInventory().restore();
		player.getWarehouse().restore();
		player.getFreight().restore();
		player.restoreItemReuse();
		
		// Retrieve from the database all skills of this Player and add them to _skills.
		player.restoreSkills();
		
		// Retrieve from the database all macros of this Player and add them to _macros.
		player.getMacros().restoreMe();
		
		// Retrieve from the database all henna of this Player and add them to _hennas.
		player.getHennaList().restore();
		
		// Retrieve from the database all teleport bookmark of this Player and add them to _teleportBookmarks.
		player.getTeleportBookmarkList().restore();
		
		// Retrieve from the database the recipe book of this Player.
		player.getRecipeBook().restore(true);
		
		// Restore Recipe Shop list.
		if (PlayerConfig.STORE_RECIPE_SHOPLIST)
		{
			player.getRecipeBook().restoreShop();
		}
		
		// Restore collections.
		final CollectionList collectionList = player.getCollectionList();
		collectionList.restore();
		collectionList.restoreBonuses();
		collectionList.restoreFavorites();
		
		player.getChallengeInfo().restoreChallengePoints();
		
		// Cross event.
		player.getCrossEventProgress().restore();
		
		// Restore Relics.
		final RelicList relicList = player.getRelicList();
		relicList.restore();
		relicList.restoreActiveInfo();
		player.sendPacket(new ExRelicsList(player)); // Update confirmed relic list relics count.
		player.sendPacket(new ExRelicsExchangeList(player)); // Update relic exchange/confirm list.
		
		// Restore Relic collections.
		relicList.restoreCollections();
		relicList.restoreCollectionBonuses();
		player.sendPacket(new ExRelicsCollectionInfo(player));
		
		// Load Premium Item List.
		player.getPremiumItemList().restore();
		
		// Restore items in pet inventory.
		player.restorePetInventoryItems();
		
		// Reward auto-get skills and all available skills if auto-learn skills is true.
		player.rewardSkills();
		
		// Retrieve from the database all shortcuts of this Player and add them to _shortcuts.
		player.restoreShortcuts();
		
		// Initialize status update cache.
		player.initStatusUpdateCache();
		
		// Restore current Cp, HP and MP values.
		player.setCurrentCp(currentCp);
		player.setCurrentHp(currentHp);
		player.setCurrentMp(currentMp);
		
		player.setOriginalCpHpMp(currentCp, currentHp, currentMp);
		if (currentHp < 0.5)
		{
			player.setDead(true);
			player.stopHpMpRegeneration();
		}
		
		// Restore pet if exists in the world.
		player.setPet(World.getPet(player.getObjectId()));
		final Summon pet = player.getPet();
		if (pet != null)
		{
			pet.setOwner(player);
		}
		
		if (player.hasServitors())
		{
			for (Summon summon : player.getServitors().values())
			{
				summon.setOwner(player);
			}
		}
		
		// CoC Monthly winner. (True Hero)
		final int trueHeroId = GlobalVariablesManager.getInstance().getInt(GlobalVariablesManager.COC_TRUE_HERO, 0);
		if (trueHeroId == player.getObjectId())
		{
			if (!GlobalVariablesManager.getInstance().getBoolean(GlobalVariablesManager.COC_TRUE_HERO_REWARDED, true))
			{
				GlobalVariablesManager.getInstance().set(GlobalVariablesManager.COC_TRUE_HERO_REWARDED, true);
				
				for (ItemHolder reward : CeremonyOfChaosConfig.COC_MONTHLY_WINNER_REWARDS)
				{
					player.addItem(ItemProcessType.REWARD, reward.getId(), reward.getCount(), player, true);
				}
				
				player.setFame(player.getFame() + CeremonyOfChaosConfig.COC_MONTHLY_WINNER_FAME);
				
				player.sendMessage("You have been rewarded with " + CeremonyOfChaosConfig.COC_MONTHLY_WINNER_FAME + " fame points.");
				final Clan clan = player.getClan();
				if (clan != null)
				{
					clan.addReputationScore(CeremonyOfChaosConfig.COC_MONTHLY_WINNER_CLAN_REPUTATION);
				}
			}
			
			player.setTrueHero(true);
		}
		
		player.getHomunculusList().restore();
		
		// Recalculate all stats.
		player.getStat().recalculateStats(false);
		
		// Update the overloaded status of the Player.
		player.refreshOverloaded(false);
		
		player.getFriends().restore();
		
		player.getRecommendations().restore();
		player.getRecommendations().startGiveTask();
		
		player.setOnlineStatus(true, false);
		
		PlayerAutoSaveTaskManager.getInstance().add(player);
		
		return player;
	}
	
	/**
	 * Sets the number of symbol seal points for the player.<br>
	 * The value is capped at {@code PlayerConfig.MAX_SYMBOL_SEAL_POINTS}.
	 * @param value The amount of symbol seal points to set.
	 */
	public void setSymbolSealPoints(int value)
	{
		getVariables().set(SYMBOL_POINTS_VAR, Math.min(value, PlayerConfig.MAX_SYMBOL_SEAL_POINTS));
	}
	
	/**
	 * Retrieves the current number of symbol seal points.<br>
	 * This value is stored in the player's variables.
	 * @return The integer value of the symbol seal points.
	 */
	public int getSymbolSealPoints()
	{
		return getVariables().getInt(SYMBOL_POINTS_VAR, 0);
	}
	
	/**
	 * Removes symbol seal skills from the player.<br>
	 * This method checks if the player belongs to a specific class group.<br>
	 * If they do, it removes up to three unique skills associated with that class.
	 */
	public void removeSymbolSealSkills()
	{
		final int classId = getPlayerClass().getId();
		if (CategoryData.getInstance().isInCategory(CategoryType.SIXTH_CLASS_GROUP, classId))
		{
			// Remove basic symbol seal skills.
			for (int i = 0; i < 3; i++)
			{
				final Skill skill = SymbolSealData.getInstance().getSkill(classId, i);
				if (skill != null)
				{
					removeSkill(skill);
				}
			}
			
			// Get dye levels from account variables.
			final int dyeLevelSlot1 = getAccountVariables().getInt(AccountVariables.DYE_LEVEL_FOR_SLOT_ + 1, 0);
			final int dyeLevelSlot2 = getAccountVariables().getInt(AccountVariables.DYE_LEVEL_FOR_SLOT_ + 2, 0);
			final int dyeLevelSlot3 = getAccountVariables().getInt(AccountVariables.DYE_LEVEL_FOR_SLOT_ + 3, 0);
			final int dyeHiddenLevelSlot1 = getAccountVariables().getInt(AccountVariables.DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ + 1, 0);
			final int dyeHiddenLevelSlot2 = getAccountVariables().getInt(AccountVariables.DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ + 2, 0);
			final int dyeHiddenLevelSlot3 = getAccountVariables().getInt(AccountVariables.DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ + 3, 0);
			
			// Calculate buff level.
			final int symbolSealBuffLevel = dyeHiddenLevelSlot1 + dyeHiddenLevelSlot2 + dyeHiddenLevelSlot3;
			
			// Store skills in a map.
			final Map<Integer, Integer> skillToLevelMap = Map.of( //
				37073, dyeLevelSlot1, // Seal - Giant's Power
				37074, dyeLevelSlot2, // Seal - Giant's Wisdom
				37075, dyeLevelSlot3, // Seal - Giant's Might
				37066, dyeHiddenLevelSlot1, // Seal Heritage - Giant's Power
				37067, dyeHiddenLevelSlot2, // Seal Heritage - Giant's Wisdom
				37068, dyeHiddenLevelSlot3, // Seal Heritage - Giant's Might
				37072, symbolSealBuffLevel // Giant's Seal
			);
			
			// Remove skills only if the corresponding dye level is greater than 0.
			skillToLevelMap.forEach((skillId, level) ->
			{
				if (level > 0)
				{
					final Skill skill = SkillData.getInstance().getSkill(skillId, level);
					if (skill != null)
					{
						removeSkill(skill);
					}
				}
			});
		}
	}
	
	/**
	 * Updates the symbol seal skills for the player.<br>
	 * This method checks if the player belongs to a specific class group.<br>
	 * It removes existing skills and adds new ones based on available slots.
	 */
	public void updateSymbolSealSkills()
	{
		final int classId = getPlayerClass().getId();
		if (CategoryData.getInstance().isInCategory(CategoryType.SIXTH_CLASS_GROUP, classId))
		{
			removeSymbolSealSkills();
			if (getSymbolSealPoints() > 0)
			{
				final int usedSlots;
				switch (_hennas.getEmptySlots())
				{
					case 0:
					{
						usedSlots = 3;
						break;
					}
					case 1:
					{
						usedSlots = 2;
						break;
					}
					case 2:
					{
						usedSlots = 1;
						break;
					}
					default:
					{
						usedSlots = 0;
						break;
					}
				}
				
				for (int i = 0; i < usedSlots; i++)
				{
					final Skill skill = SymbolSealData.getInstance().getSkill(classId, i);
					if (skill != null)
					{
						addSkill(skill);
					}
				}
				
				// Get dye levels from account variables.
				final int dyeLevelSlot1 = getAccountVariables().getInt(AccountVariables.DYE_LEVEL_FOR_SLOT_ + 1, 0);
				final int dyeLevelSlot2 = getAccountVariables().getInt(AccountVariables.DYE_LEVEL_FOR_SLOT_ + 2, 0);
				final int dyeLevelSlot3 = getAccountVariables().getInt(AccountVariables.DYE_LEVEL_FOR_SLOT_ + 3, 0);
				final int dyeHiddenLevelSlot1 = getAccountVariables().getInt(AccountVariables.DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ + 1, 0);
				final int dyeHiddenLevelSlot2 = getAccountVariables().getInt(AccountVariables.DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ + 2, 0);
				final int dyeHiddenLevelSlot3 = getAccountVariables().getInt(AccountVariables.DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ + 3, 0);
				
				// Calculate buff level.
				final int symbolSealBuffLevel = dyeHiddenLevelSlot1 + dyeHiddenLevelSlot2 + dyeHiddenLevelSlot3;
				
				// Store skills in a map.
				final Map<Integer, Integer> skillToLevelMap = Map.of( //
					37073, dyeLevelSlot1, // Seal - Giant's Power
					37074, dyeLevelSlot2, // Seal - Giant's Wisdom
					37075, dyeLevelSlot3, // Seal - Giant's Might
					37066, dyeHiddenLevelSlot1, // Seal Heritage - Giant's Power
					37067, dyeHiddenLevelSlot2, // Seal Heritage - Giant's Wisdom
					37068, dyeHiddenLevelSlot3, // Seal Heritage - Giant's Might
					37072, symbolSealBuffLevel // Giant's Seal
				);
				
				// Add skills based on dye levels.
				skillToLevelMap.forEach((skillId, level) ->
				{
					if (level > 0)
					{
						final Skill skill = SkillData.getInstance().getSkill(skillId, level);
						if (skill != null)
						{
							addSkill(skill);
						}
					}
				});
				
				// Remove skills with level 0.
				getSkillList().stream().filter(skill -> skillToLevelMap.containsKey(skill.getId()) && (skillToLevelMap.get(skill.getId()) == 0)).forEach(this::removeSkill);
			}
		}
	}
	
	/**
	 * Retrieves the mail forum associated with this actor.<br>
	 * It creates a new forum if one does not already exist.
	 * @return the {@code Forum} object for the mail system.
	 */
	public Forum getMail()
	{
		if (_forumMail == null)
		{
			setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(_name));
			
			if (_forumMail == null)
			{
				ForumsBBSManager.getInstance().createNewForum(_name, ForumsBBSManager.getInstance().getForumByName("MailRoot"), Forum.MAIL, Forum.OWNERONLY, getObjectId());
				setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(_name));
			}
		}
		
		return _forumMail;
	}
	
	/**
	 * Sets the current {@code Forum} for mail. <br>
	 * This method updates the internal {@code _forumMail} field.
	 * @param forum The {@code Forum} object to assign.
	 */
	public void setMail(Forum forum)
	{
		_forumMail = forum;
	}
	
	/**
	 * Retrieves the memo forum associated with this account.<br>
	 * If the memo does not exist, it will be created automatically.
	 * @return the {@code Forum} object for the user's memo.
	 */
	public Forum getMemo()
	{
		if (_forumMemo == null)
		{
			setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			
			if (_forumMemo == null)
			{
				ForumsBBSManager.getInstance().createNewForum(_accountName, ForumsBBSManager.getInstance().getForumByName("MemoRoot"), Forum.MEMO, Forum.OWNERONLY, getObjectId());
				setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			}
		}
		
		return _forumMemo;
	}
	
	/**
	 * Sets the memo for the current forum.<br>
	 * This method updates the {@code _forumMemo} field with the provided {@code Forum} object.
	 * @param forum The {@code Forum} object to set as the memo.
	 */
	public void setMemo(Forum forum)
	{
		_forumMemo = forum;
	}
	
	/**
	 * Restores the subclass data for a specific {@code Player} from the database.<br>
	 * It populates the player's subclass collection with information like level and experience.<br>
	 * If an error occurs during the database operation, it logs a warning message.
	 * @param player The {@code Player} object whose subclasses need to be restored.
	 * @return Always returns {@code true}.
	 */
	private static boolean restoreSubClassData(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_SUBCLASSES))
		{
			statement.setInt(1, player.getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final SubClassHolder subClass = new SubClassHolder();
					subClass.setPlayerClass(rset.getInt("class_id"));
					subClass.setDualClassActive(rset.getBoolean("dual_class"));
					subClass.setVitalityPoints(rset.getInt("vitality_points"));
					subClass.setLevel(rset.getInt("level"));
					subClass.setExp(rset.getLong("exp"));
					subClass.setSp(rset.getLong("sp"));
					subClass.setClassIndex(rset.getInt("class_index"));
					
					// Enforce the correct indexing of _subClasses against their class indexes.
					player.getSubClasses().put(subClass.getClassIndex(), subClass);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore classes for " + player.getName() + ": " + e.getMessage(), e);
		}
		
		return true;
	}
	
	/**
	 * Retrieves the {@code PremiumItemList} of this player.
	 * @return the {@code PremiumItemList} of this player.
	 */
	public PremiumItemList getPremiumItemList()
	{
		return _premiumItemList;
	}
	
	/**
	 * Saves the current character data to the database.<br>
	 * This method handles base stats, sub-stats, and items.<br>
	 * It also updates inventory, warehouse, and freight information.
	 * @param storeActiveEffects Determines whether active effects should be saved when {@code true}.
	 */
	public synchronized void store(boolean storeActiveEffects)
	{
		storeCharBase();
		storeCharSub();
		storeEffect(storeActiveEffects);
		storeItemReuseDelay();
		if (PlayerConfig.STORE_RECIPE_SHOPLIST)
		{
			_recipeBook.storeShop();
		}
		
		_rankingHistory.store();
		
		// Store collections.
		_collections.store();
		_collections.storeFavorites();
		
		_challengePoints.storeChallengePoints();
		
		_crossEventProgress.store();
		
		final PlayerVariables vars = getScript(PlayerVariables.class);
		if (vars != null)
		{
			vars.storeMe();
		}
		
		final AccountVariables aVars = getScript(AccountVariables.class);
		if (aVars != null)
		{
			aVars.storeMe();
		}
		
		getInventory().updateDatabase();
		getWarehouse().updateDatabase();
		getFreight().updateDatabase();
		
		if (_huntPass != null)
		{
			_huntPass.store();
		}
	}
	
	/**
	 * Saves the current state of the actor to the database.<br>
	 * This method calls {@code store(true)} to ensure all data is persisted.
	 */
	@Override
	public void storeMe()
	{
		store(true);
	}
	
	/**
	 * Saves the character's base statistics and appearance to the database.<br>
	 * This method updates various fields including experience, level, and health.<br>
	 * It also records location data and clan information.<br>
	 * Any errors during the database update are logged as a warning.
	 */
	private void storeCharBase()
	{
		// Get the exp, level, and sp of base class to store in base table.
		final long exp = getStat().getBaseExp();
		final int level = getStat().getBaseLevel();
		final long sp = getStat().getBaseSp();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_CHARACTER))
		{
			statement.setInt(1, level);
			statement.setInt(2, (int) getMaxHp());
			statement.setDouble(3, getCurrentHp());
			statement.setInt(4, getMaxCp());
			statement.setDouble(5, getCurrentCp());
			statement.setInt(6, getMaxMp());
			statement.setDouble(7, getCurrentMp());
			statement.setInt(8, _appearance.getFace());
			statement.setInt(9, _appearance.getHairStyle());
			statement.setInt(10, _appearance.getHairColor());
			statement.setInt(11, _appearance.isFemale() ? 1 : 0);
			statement.setInt(12, getHeading());
			statement.setInt(13, _lastLoc != null ? _lastLoc.getX() : getX());
			statement.setInt(14, _lastLoc != null ? _lastLoc.getY() : getY());
			statement.setInt(15, _lastLoc != null ? _lastLoc.getZ() : getZ());
			statement.setLong(16, exp);
			statement.setLong(17, _expBeforeDeath);
			statement.setLong(18, sp);
			statement.setInt(19, getReputation());
			statement.setInt(20, _fame);
			statement.setInt(21, _raidbossPoints);
			statement.setInt(22, _pvpKills);
			statement.setInt(23, _pkKills);
			statement.setInt(24, _clanId);
			statement.setInt(25, getRace().ordinal());
			statement.setInt(26, getPlayerClass().getId());
			statement.setLong(27, _deleteTimer);
			statement.setString(28, getTitle());
			statement.setInt(29, _appearance.getTitleColor());
			statement.setInt(30, isOnlineInt());
			statement.setInt(31, _clanPrivileges.getMask());
			statement.setInt(32, _wantsPeace);
			statement.setInt(33, _baseClass);
			long totalOnlineTime = _onlineTime;
			if (_onlineBeginTime > 0)
			{
				totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000;
			}
			
			statement.setLong(34, _offlineMode.getShopStartTime() > 0 ? _onlineTime : totalOnlineTime);
			statement.setInt(35, _nobleLevel);
			statement.setInt(36, _powerGrade);
			statement.setInt(37, _pledgeType);
			statement.setInt(38, _lvlJoinedAcademy);
			statement.setLong(39, _apprentice);
			statement.setLong(40, _sponsor);
			statement.setLong(41, _clanJoinExpiryTime);
			statement.setLong(42, _clanCreateExpiryTime);
			statement.setString(43, _name);
			statement.setInt(44, _bookmarkslot);
			statement.setInt(45, getStat().getBaseVitalityPoints());
			statement.setString(46, _lang);
			int factionId = 0;
			if (_isGood)
			{
				factionId = 1;
			}
			
			if (_isEvil)
			{
				factionId = 2;
			}
			
			statement.setInt(47, factionId);
			statement.setInt(48, _pcCafePoints);
			statement.setInt(49, getTotalKills());
			statement.setInt(50, getTotalDeaths());
			statement.setInt(51, getObjectId());
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store char base data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	/**
	 * Saves the current sub-class data to the database.<br>
	 * This method updates experience, skill points, level, and other stats.<br>
	 * It uses a batch update to process all active sub-classes for the character.
	 */
	private void storeCharSub()
	{
		if (getTotalSubClasses() <= 0)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_SUBCLASS))
		{
			for (SubClassHolder subClass : getSubClasses().values())
			{
				statement.setLong(1, subClass.getExp());
				statement.setLong(2, subClass.getSp());
				statement.setInt(3, subClass.getLevel());
				statement.setInt(4, subClass.getVitalityPoints());
				statement.setInt(5, subClass.getId());
				statement.setBoolean(6, subClass.isDualClass());
				statement.setInt(7, getObjectId());
				statement.setInt(8, subClass.getClassIndex());
				statement.addBatch();
			}
			
			statement.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store sub class data for " + _name + ": " + e.getMessage(), e);
		}
	}
	
	/**
	 * Saves the current character effects and skill reuse data to the database.<br>
	 * This method handles both active buffs and pending skill reuses.<br>
	 * It prevents duplicate entries by clearing existing records before saving.
	 * @param storeEffects Determines whether active buff effects should be saved.
	 */
	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!PlayerConfig.STORE_SKILL_COOLTIME)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Delete all current stored effects for char to avoid dupe.
			final int objectId = getObjectId();
			try (PreparedStatement delete = con.prepareStatement(DELETE_SKILL_SAVE))
			{
				delete.setInt(1, objectId);
				delete.setInt(2, _classIndex);
				delete.execute();
			}
			
			int buffIndex = 0;
			final List<Long> storedSkills = new ArrayList<>();
			final long currentTime = System.currentTimeMillis();
			
			// Store all effect data along with calculated remaining.
			// reuse delays for matching skills. 'restore_type'= 0.
			try (PreparedStatement statement = con.prepareStatement(ADD_SKILL_SAVE))
			{
				if (storeEffects)
				{
					for (BuffInfo info : getEffectList().getEffects())
					{
						if (info == null)
						{
							continue;
						}
						
						final Skill skill = info.getSkill();
						
						// Do not store those effects.
						if (skill.isDeleteAbnormalOnLeave())
						{
							continue;
						}
						
						// Do not save heals.
						if (skill.getAbnormalType() == AbnormalType.LIFE_FORCE_OTHERS)
						{
							continue;
						}
						
						// Toggles are skipped, unless they are necessary to be always on.
						if (!PlayerConfig.ALT_STORE_TOGGLES && (skill.isToggle() && !skill.isNecessaryToggle()))
						{
							continue;
						}
						
						if (skill.isMentoring())
						{
							continue;
						}
						
						// Dances and songs are not kept in retail.
						if (skill.isDance() && !PlayerConfig.ALT_STORE_DANCES)
						{
							continue;
						}
						
						final long reuseHashCode = skill.getReuseHashCode();
						if (storedSkills.contains(reuseHashCode))
						{
							continue;
						}
						
						storedSkills.add(reuseHashCode);
						
						statement.setInt(1, objectId);
						statement.setInt(2, skill.getId());
						statement.setInt(3, skill.getLevel());
						statement.setInt(4, skill.getSubLevel());
						statement.setInt(5, info.getTime());
						
						final TimeStamp t = getSkillReuseTimeStamp(reuseHashCode);
						final long tStamp = (t != null) ? t.getStamp() : 0;
						final boolean tValid = (t != null) && (currentTime < tStamp);
						statement.setLong(6, (t != null) && tValid ? t.getReuse() : 0);
						statement.setDouble(7, tValid ? tStamp : 0);
						statement.setInt(8, 0); // Store type 0, active buffs/debuffs.
						statement.setInt(9, _classIndex);
						statement.setInt(10, ++buffIndex);
						statement.addBatch();
					}
				}
				
				// Skills under reuse.
				for (Entry<Long, TimeStamp> ts : getSkillReuseTimeStamps().entrySet())
				{
					final long hash = ts.getKey();
					if (storedSkills.contains(hash))
					{
						continue;
					}
					
					final TimeStamp t = ts.getValue();
					if (t == null)
					{
						continue;
					}
					
					final long tStamp = t.getStamp();
					if (currentTime < tStamp)
					{
						storedSkills.add(hash);
						
						statement.setInt(1, objectId);
						statement.setInt(2, t.getSkillId());
						statement.setInt(3, t.getSkillLevel());
						statement.setInt(4, t.getSkillSubLevel());
						statement.setInt(5, -1);
						statement.setLong(6, t.getReuse());
						statement.setDouble(7, tStamp);
						statement.setInt(8, 1); // Restore type 1, skill reuse.
						statement.setInt(9, _classIndex);
						statement.setInt(10, ++buffIndex);
						statement.addBatch();
					}
				}
				
				statement.executeBatch();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store char effect data: ", e);
		}
	}
	
	/**
	 * Saves the current item reuse delay data to the database.<br>
	 * This method clears existing records for the character and adds valid timestamps.<br>
	 * It ensures that only items with a future timestamp are persisted.
	 */
	private void storeItemReuseDelay()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement(DELETE_ITEM_REUSE_SAVE);
			PreparedStatement ps2 = con.prepareStatement(ADD_ITEM_REUSE_SAVE))
		{
			final int objectId = getObjectId();
			ps1.setInt(1, objectId);
			ps1.execute();
			
			final long currentTime = System.currentTimeMillis();
			for (TimeStamp ts : getItemReuseTimeStamps().values())
			{
				if (ts == null)
				{
					continue;
				}
				
				final long tsStamp = ts.getStamp();
				if (currentTime < tsStamp)
				{
					ps2.setInt(1, objectId);
					ps2.setInt(2, ts.getItemId());
					ps2.setInt(3, ts.getItemObjectId());
					ps2.setLong(4, ts.getReuse());
					ps2.setDouble(5, tsStamp);
					ps2.addBatch();
				}
			}
			
			ps2.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store char item reuse data: ", e);
		}
	}
	
	/**
	 * Checks if the player is currently online.
	 * @return {@code true} if the player is online, {@code false} otherwise.
	 */
	public boolean isOnline()
	{
		return _isOnline;
	}
	
	/**
	 * Checks the current online status of the player.<br>
	 * Returns a value based on whether the client is connected or detached.
	 * @return {@code 1} if the player is online and connected, {@code 2} if the player is online but detached, and {@code 0} otherwise.
	 */
	public int isOnlineInt()
	{
		if (_isOnline && (_client != null))
		{
			return _client.isDetached() ? 2 : 1;
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the {@code OfflineMode} of this player.
	 * @return the {@code OfflineMode} of this player.
	 */
	public OfflineMode getOfflineMode()
	{
		return _offlineMode;
	}
	
	/**
	 * Sets the internal flag to indicate that the player has entered the world.<br>
	 * This updates the {@code _enteredWorld} boolean value to {@code true}.
	 */
	public void setEnteredWorld()
	{
		_enteredWorld = true;
	}
	
	/**
	 * Checks if the player has successfully entered the game world.<br>
	 * Returns {@code true} if the player is currently in the world.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating the entry status.
	 */
	public boolean hasEnteredWorld()
	{
		return _enteredWorld;
	}
	
	/**
	 * Adds a new {@code Skill} to the actor.<br>
	 * This method also registers the skill as a custom skill.
	 * @param newSkill The {@code Skill} object to be added.
	 * @return The successfully added {@code Skill} object.
	 */
	@Override
	public Skill addSkill(Skill newSkill)
	{
		addCustomSkill(newSkill);
		return super.addSkill(newSkill);
	}
	
	/**
	 * Adds a new {@code Skill} to the player.<br>
	 * This method updates the internal skill list and calculation sets.<br>
	 * If {@code store} is {@code true}, it also saves the change to the database.
	 * @param newSkill The {@code Skill} object to be added.
	 * @param store Whether to persist the new skill in the database.
	 * @return The {@code Skill} that was previously held at this position, or {@code null}.
	 */
	public Skill addSkill(Skill newSkill, boolean store)
	{
		// Add a skill to the Player _skills and its Func objects to the calculator set of the Player.
		final Skill oldSkill = addSkill(newSkill);
		
		// Add or update a Player skill in the character_skills table of the database.
		if (store)
		{
			storeSkill(newSkill, oldSkill, -1);
			final Integer newSkillId = newSkill.getId();
			if (ArrayUtil.contains(DUAL_CLASS_SKILLS, newSkillId))
			{
				final Map<Integer, Integer> knownSkills = getVariables().getIntegerMap(KNOWN_DUAL_SKILLS_VAR);
				knownSkills.put(newSkillId, newSkill.getLevel());
				getVariables().setIntegerMap(KNOWN_DUAL_SKILLS_VAR, knownSkills);
			}
		}
		
		return oldSkill;
	}
	
	/**
	 * Removes a specific skill from the actor.<br>
	 * This method handles custom skills and determines whether to save the change.
	 * @param skill The {@code Skill} object to be removed.
	 * @param store A boolean indicating if the removal should be saved to the database.
	 * @return The {@code Skill} object that was removed.
	 */
	@Override
	public Skill removeSkill(Skill skill, boolean store)
	{
		removeCustomSkill(skill);
		return store ? removeSkill(skill) : super.removeSkill(skill, true);
	}
	
	/**
	 * Removes a specific skill from the actor.<br>
	 * This method handles custom skills and determines whether to save the change or cancel effects.
	 * @param skill The {@code Skill} object to be removed.
	 * @param store If {@code true}, it uses the internal removal logic; otherwise, it calls the superclass method.
	 * @param cancelEffect Determines if the effect of the skill should be cancelled upon removal.
	 * @return The resulting {@code Skill} object after the removal process.
	 */
	public Skill removeSkill(Skill skill, boolean store, boolean cancelEffect)
	{
		removeCustomSkill(skill);
		return store ? removeSkill(skill) : super.removeSkill(skill, cancelEffect);
	}
	
	/**
	 * Removes a specific skill from the character.<br>
	 * This method updates the database and removes associated shortcuts.
	 * @param skill The {@code Skill} object to be removed.
	 * @return The {@code Skill} that was removed, or {@code null} if no skill was found.
	 */
	public Skill removeSkill(Skill skill)
	{
		removeCustomSkill(skill);
		
		// Remove a skill from the Creature and its stats.
		final Skill oldSkill = super.removeSkill(skill, true);
		if (oldSkill != null)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR))
			{
				// Remove or update a Player skill from the character_skills table of the database.
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, _classIndex);
				statement.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Error could not delete skill: " + e.getMessage(), e);
			}
		}
		
		if ((getTransformationId() > 0) || isCursedWeaponEquipped())
		{
			return oldSkill;
		}
		
		if (skill != null)
		{
			for (Shortcut sc : _shortcuts.getAllShortcuts())
			{
				final int skillId = skill.getId();
				if ((sc != null) && (sc.getId() == skillId) && (sc.getType() == ShortcutType.SKILL) && !((skillId >= 3080) && (skillId <= 3259)))
				{
					deleteShortcut(sc.getSlot(), sc.getPage());
				}
			}
		}
		
		return oldSkill;
	}
	
	/**
	 * Updates the character's skill data in the database.<br>
	 * It handles both updating an existing skill and adding a new one.<br>
	 * If {@code oldSkill} is provided, it updates the level of that specific skill.<br>
	 * If {@code oldSkill} is {@code null}, it adds the {@code newSkill} to the character.
	 * @param newSkill The {@code Skill} object containing the new level and sub-level data.
	 * @param oldSkill The existing {@code Skill} object to be updated, or {@code null} if adding a new skill.
	 * @param newClassIndex The index of the class for which the skill is being stored.
	 */
	private void storeSkill(Skill newSkill, Skill oldSkill, int newClassIndex)
	{
		final int classIndex = (newClassIndex > -1) ? newClassIndex : _classIndex;
		try (Connection con = DatabaseFactory.getConnection())
		{
			if ((oldSkill != null) && (newSkill != null))
			{
				try (PreparedStatement ps = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL))
				{
					ps.setInt(1, newSkill.getLevel());
					ps.setInt(2, newSkill.getSubLevel());
					ps.setInt(3, oldSkill.getId());
					ps.setInt(4, getObjectId());
					ps.setInt(5, classIndex);
					ps.execute();
				}
			}
			else if (newSkill != null)
			{
				try (PreparedStatement ps = con.prepareStatement(ADD_NEW_SKILLS))
				{
					ps.setInt(1, getObjectId());
					ps.setInt(2, newSkill.getId());
					ps.setInt(3, newSkill.getLevel());
					ps.setInt(4, newSkill.getSubLevel());
					ps.setInt(5, classIndex);
					ps.execute();
				}
			}
			// else
			// {
			// LOGGER.warning("Could not store new skill, it's null!");
			// }
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error could not store char skills: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Saves a list of new {@code Skill} objects to the database.<br>
	 * This method uses a batch process to update the character's skills.<br>
	 * It identifies the correct class index using the provided value or the default.
	 * @param newSkills The list of {@code Skill} objects to be saved.
	 * @param newClassIndex The index of the class for these skills, or -1 to use the current one.
	 */
	private void storeSkills(List<Skill> newSkills, int newClassIndex)
	{
		if (newSkills.isEmpty())
		{
			return;
		}
		
		final int classIndex = (newClassIndex > -1) ? newClassIndex : _classIndex;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(ADD_NEW_SKILLS))
		{
			for (Skill addSkill : newSkills)
			{
				ps.setInt(1, getObjectId());
				ps.setInt(2, addSkill.getId());
				ps.setInt(3, addSkill.getLevel());
				ps.setInt(4, addSkill.getSubLevel());
				ps.setInt(5, classIndex);
				ps.addBatch();
			}
			
			ps.executeBatch();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Error could not store char skills: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Loads the player's skills from the database.<br>
	 * It populates the {@code Skill} objects for the character.<br>
	 * This method also validates skills against the skill tree and restores dual class skills.
	 */
	private void restoreSkills()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR))
		{
			// Retrieve all skills of this Player from the database.
			final int objectId = getObjectId();
			statement.setInt(1, objectId);
			statement.setInt(2, _classIndex);
			final List<Skill> outdatedSkills = new ArrayList<>();
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final int id = rset.getInt("skill_id");
					final int level = rset.getInt("skill_level");
					final int subLevel = rset.getInt("skill_sub_level");
					
					// Create a Skill object for each record.
					Skill skill = SkillData.getInstance().getSkillQuietly(id, level, subLevel);
					if (skill == null)
					{
						// The stored level is no longer defined, fall back to the max level.
						final int maxLevel = SkillData.getInstance().getMaxLevel(id);
						final int lookupLevel = ((maxLevel > 0) && (level > maxLevel)) ? maxLevel : level;
						skill = SkillData.getInstance().getSkillQuietly(id, lookupLevel, subLevel);
						
						// The stored sublevel is no longer defined, fall back to the base sublevel.
						if ((skill == null) && (subLevel != 0))
						{
							skill = SkillData.getInstance().getSkillQuietly(id, lookupLevel, 0);
						}
					}
					
					if (skill == null)
					{
						LOGGER.warning("Skipped null skill Id: " + id + " Level: " + level + " while restoring player skills for playerObjId: " + objectId);
						continue;
					}
					
					// The stored level or sublevel is no longer defined, repair the record to avoid the same lookup failing on every restore.
					if ((skill.getLevel() != level) || (skill.getSubLevel() != subLevel))
					{
						outdatedSkills.add(skill);
					}
					
					// Add the Skill object to the Creature _skills and its Func objects to the calculator set of the Creature.
					addSkill(skill);
					
					if (GeneralConfig.SKILL_CHECK_ENABLE && (!isGM() || GeneralConfig.SKILL_CHECK_GM) && !SkillTreeData.getInstance().isSkillAllowed(this, skill))
					{
						PunishmentManager.handleIllegalPlayerAction(this, "Player " + _name + " has invalid skill " + skill.getName() + " (" + skill.getId() + "/" + skill.getLevel() + "), class:" + ClassListData.getInstance().getClass(getPlayerClass()).getClassName(), IllegalActionPunishmentType.BROADCAST);
						if (GeneralConfig.SKILL_CHECK_REMOVE)
						{
							removeSkill(skill);
						}
					}
				}
			}
			
			// Store the repaired records, so the missing levels are not looked up again.
			if (!outdatedSkills.isEmpty())
			{
				try (PreparedStatement ps = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL))
				{
					for (Skill skill : outdatedSkills)
					{
						ps.setInt(1, skill.getLevel());
						ps.setInt(2, skill.getSubLevel());
						ps.setInt(3, skill.getId());
						ps.setInt(4, objectId);
						ps.setInt(5, _classIndex);
						ps.addBatch();
					}
					
					ps.executeBatch();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore character " + this + " skills: " + e.getMessage(), e);
		}
		
		// Learn known dualclass skills.
		restoreDualSkills();
		
		// Check ability skill count.
		int count = 0;
		for (SkillLearn sk : SkillTreeData.getInstance().getAbilitySkillTree().values())
		{
			final Skill knownSkill = getKnownSkill(sk.getSkillId());
			if ((knownSkill != null) && (knownSkill.getLevel() == sk.getSkillLevel()))
			{
				count++;
			}
		}
		
		// Too many ability skills. Remove them all.
		if ((count > (PlayerConfig.PLAYER_MAXIMUM_LEVEL - 85)) || (count > getAbilityPointsUsed()))
		{
			for (SkillLearn sk : SkillTreeData.getInstance().getAbilitySkillTree().values())
			{
				final Skill knownSkill = getKnownSkill(sk.getSkillId());
				if ((knownSkill != null) && (knownSkill.getLevel() == sk.getSkillLevel()))
				{
					removeSkill(knownSkill, false);
				}
			}
		}
	}
	
	/**
	 * Restores the dual skills for the character.<br>
	 * This method checks if dual classing is active and adds known skills to the player.<br>
	 * It retrieves skills from the {@code KNOWN_DUAL_SKILLS_VAR} map.
	 */
	public void restoreDualSkills()
	{
		if (isDualClassActive() || !isSubClassActive())
		{
			final Map<Integer, Integer> knownSkills = getVariables().getIntegerMap(KNOWN_DUAL_SKILLS_VAR);
			if (knownSkills.isEmpty())
			{
				return;
			}
			
			for (Entry<Integer, Integer> entry : knownSkills.entrySet())
			{
				final Skill skill = SkillData.getInstance().getSkill(entry.getKey(), entry.getValue());
				
				// removeSkill(skill);
				addSkill(skill, true);
			}
		}
	}
	
	/**
	 * Restores active skill effects for the character from the database.<br>
	 * This method handles skills that were still in effect or under reuse delay upon logout.<br>
	 * It applies the remaining time and updates the timestamps for each valid skill.
	 */
	@Override
	public void restoreEffects()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_SKILL_SAVE))
		{
			final int objectId = getObjectId();
			statement.setInt(1, objectId);
			statement.setInt(2, _classIndex);
			try (ResultSet rset = statement.executeQuery())
			{
				final long currentTime = System.currentTimeMillis();
				while (rset.next())
				{
					final Skill skill = SkillData.getInstance().getSkill(rset.getInt("skill_id"), rset.getInt("skill_level"), rset.getInt("skill_sub_level"));
					if (skill == null)
					{
						continue;
					}
					
					final int remainingTime = rset.getInt("remaining_time");
					final long reuseDelay = rset.getLong("reuse_delay");
					final long systime = rset.getLong("systime");
					final int restoreType = rset.getInt("restore_type");
					final long time = systime - currentTime;
					if (time > 10)
					{
						disableSkill(skill, time);
						addTimeStamp(skill, reuseDelay, systime);
					}
					
					// Restore Type 1 The remaining skills lost effect upon logout but were still under a high reuse delay.
					if (restoreType > 0)
					{
						continue;
					}
					
					// Restore Type 0 These skill were still in effect on the character upon logout.
					// Some of which were self casted and might still have had a long reuse delay which also is restored.
					skill.applyEffects(this, this, false, remainingTime);
				}
			}
			
			// Remove previously restored skills.
			try (PreparedStatement delete = con.prepareStatement(DELETE_SKILL_SAVE))
			{
				delete.setInt(1, objectId);
				delete.setInt(2, _classIndex);
				delete.executeUpdate();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Restores the item reuse delay data from the database.<br>
	 * This method updates items in the inventory or warehouse with their saved cooldowns.<br>
	 * It also clears the temporary save records for this object after processing.
	 */
	private void restoreItemReuse()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_ITEM_REUSE_SAVE);
			PreparedStatement delete = con.prepareStatement(DELETE_ITEM_REUSE_SAVE))
		{
			final int objectId = getObjectId();
			statement.setInt(1, objectId);
			try (ResultSet rset = statement.executeQuery())
			{
				int itemId;
				long reuseDelay;
				long systime;
				boolean isInInventory;
				long remainingTime;
				final long currentTime = System.currentTimeMillis();
				while (rset.next())
				{
					itemId = rset.getInt("itemId");
					reuseDelay = rset.getLong("reuseDelay");
					systime = rset.getLong("systime");
					isInInventory = true;
					
					// Using item Id
					Item item = _inventory.getItemByItemId(itemId);
					if (item == null)
					{
						item = getWarehouse().getItemByItemId(itemId);
						isInInventory = false;
					}
					
					if ((item != null) && (item.getId() == itemId) && (item.getReuseDelay() > 0))
					{
						remainingTime = systime - currentTime;
						if (remainingTime > 10)
						{
							addTimeStampItem(item, reuseDelay, systime);
							if (isInInventory && item.isEtcItem())
							{
								final int group = item.getSharedReuseGroup();
								if (group > 0)
								{
									sendPacket(new ExUseSharedGroupItem(itemId, group, (int) remainingTime, (int) reuseDelay));
								}
							}
						}
					}
				}
			}
			
			// Delete item reuse.
			delete.setInt(1, objectId);
			delete.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore " + this + " Item Reuse data: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the hennas of this player.
	 * @return the {@code HennaList} of this player.
	 */
	public HennaList getHennaList()
	{
		return _hennas;
	}
	
	/**
	 * Checks if the actor belongs to a specific category.<br>
	 * This method determines if the entity is part of the {@code SIXTH_CLASS_GROUP}.
	 * @return {@code true} if the actor is in the specified category, {@code false} otherwise.
	 */
	@Override
	public boolean hasBasicPropertyResist()
	{
		return isInCategory(CategoryType.SIXTH_CLASS_GROUP);
	}
	
	/**
	 * Saves the current state of the player to the database.<br>
	 * This method calls {@code storeMe()} and {@code getRecommendations().store()}.<br>
	 * If {@code GeneralConfig.UPDATE_ITEMS_ON_CHAR_STORE} is true, it also updates the inventory, warehouse, and freight databases.
	 */
	public void autoSave()
	{
		storeMe();
		_recommendations.store();
		
		if (GeneralConfig.UPDATE_ITEMS_ON_CHAR_STORE)
		{
			getInventory().updateDatabase();
			getWarehouse().updateDatabase();
			getFreight().updateDatabase();
		}
	}
	
	/**
	 * Checks if the player is allowed to log out at this moment.<br>
	 * This method verifies several conditions such as active item requests, class changes, and event registrations.<br>
	 * It returns {@code false} if any restriction prevents the logout.
	 * @return {@code true} if the player can log out, or {@code false} otherwise.
	 */
	public boolean canLogout()
	{
		if (hasItemRequest())
		{
			return false;
		}
		
		if (isSubclassLocked())
		{
			LOGGER.warning("Player " + _name + " tried to restart/logout during class change.");
			return false;
		}
		
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(this) && !(isGM() && GeneralConfig.GM_RESTART_FIGHTING))
		{
			return false;
		}
		
		if (isRegisteredOnEvent())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Determines if the specified attacker is allowed to auto-attack this entity.<br>
	 * This method checks various conditions including visibility, targetable status, and friendship.<br>
	 * It also evaluates party membership, clan relations, siege rules, and zone restrictions.
	 * @param attacker The {@code Creature} attempting to perform the attack.
	 * @return {@code true} if the attack is permitted; {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker == null)
		{
			return false;
		}
		
		// Invisible or untargetable players should not be attackable.
		if (isInvisible() || isAffected(EffectFlag.UNTARGETABLE))
		{
			return false;
		}
		
		// Check if the attacker isn't the Player Pet.
		final int objectId = attacker.getObjectId();
		if ((attacker == this) || (attacker == _pet) || attacker.hasServitor(objectId))
		{
			return false;
		}
		
		// Friendly mobs do not attack players.
		if (attacker instanceof FriendlyMob)
		{
			return false;
		}
		
		// Check if the attacker is a Monster.
		if (attacker.isMonster())
		{
			return true;
		}
		
		// is AutoAttackable if both players are in the same duel and the duel is still going on
		final boolean isPlayable = attacker.isPlayable();
		if (isPlayable && (_duelState == Duel.DUELSTATE_DUELLING) && (getDuelId() == attacker.asPlayer().getDuelId()))
		{
			return true;
		}
		
		// Check if the attacker is not in the same party. NOTE: Party checks goes before oly checks in order to prevent patry member autoattack at oly.
		if (isInParty() && _party.getMembers().contains(attacker))
		{
			return false;
		}
		
		// Check if the attacker is in olympia and olympia start.
		if (attacker.isPlayer() && attacker.asPlayer().isInOlympiadMode())
		{
			return _inOlympiadMode && _olympiadStart && (attacker.asPlayer().getOlympiadGameId() == getOlympiadGameId());
		}
		
		// Check if the attacker is in an event.
		if (isOnEvent())
		{
			return isOnSoloEvent() || (getTeam() != attacker.getTeam());
		}
		
		// Check if the attacker is a Playable.
		if (isPlayable)
		{
			if (isInsideZone(ZoneId.PEACE) || isInsideZone(ZoneId.NO_PVP))
			{
				return false;
			}
			
			// Same Command Channel are friends.
			if (PlayerConfig.ALT_COMMAND_CHANNEL_FRIENDS && (isInParty() && (getParty().getCommandChannel() != null) && attacker.isInParty() && (attacker.getParty().getCommandChannel() != null) && (getParty().getCommandChannel() == attacker.getParty().getCommandChannel())))
			{
				return false;
			}
			
			// Get Player
			final Player attackerPlayer = attacker.asPlayer();
			final Clan clan = getClan();
			final Clan attackerClan = attackerPlayer.getClan();
			if ((clan != null) && (attackerClan != null))
			{
				if (clan != attackerClan)
				{
					final Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
					if (siege != null)
					{
						// Check if a siege is in progress and if attacker and the Player aren't in the Defender clan.
						if (siege.checkIsDefender(attackerClan) && siege.checkIsDefender(clan))
						{
							return false;
						}
						
						// Check if a siege is in progress and if attacker and the Player aren't in the Attacker clan.
						if (siege.checkIsAttacker(attackerClan) && siege.checkIsAttacker(clan))
						{
							// If first mid victory is achieved, attackers can attack attackers.
							final Castle castle = CastleManager.getInstance().getCastleById(_siegeSide);
							return (castle != null) && castle.isFirstMidVictory();
						}
					}
				}
				
				// Check if clan is at war.
				if ((getWantsPeace() == 0) && (attackerPlayer.getWantsPeace() == 0) && !isAcademyMember())
				{
					final ClanWar war = attackerClan.getWarWith(getClanId());
					if (war != null)
					{
						final ClanWarState warState = war.getState();
						if ((warState == ClanWarState.MUTUAL) || (((warState == ClanWarState.BLOOD_DECLARATION) || (warState == ClanWarState.DECLARATION)) && (war.getAttackerClanId() == clan.getId())))
						{
							return true;
						}
					}
				}
			}
			
			// Check if the Player is in an arena, but NOT siege zone. NOTE: This check comes before clan/ally checks, but after party checks.
			// This is done because in arenas, clan/ally members can autoattack if they are not in party.
			if ((isInsideZone(ZoneId.PVP) && attackerPlayer.isInsideZone(ZoneId.PVP)) && !(isInsideZone(ZoneId.SIEGE) && attackerPlayer.isInsideZone(ZoneId.SIEGE)))
			{
				return true;
			}
			
			// Check if the attacker is not in the same clan.
			if ((clan != null) && clan.isMember(objectId))
			{
				return false;
			}
			
			// Check if the attacker is not in the same ally.
			if (attacker.isPlayer() && (getAllyId() != 0) && (getAllyId() == attackerPlayer.getAllyId()))
			{
				return false;
			}
			
			// Now check again if the Player is in pvp zone, but this time at siege PvP zone, applying clan/ally checks.
			if (isInsideZone(ZoneId.PVP) && attackerPlayer.isInsideZone(ZoneId.PVP) && isInsideZone(ZoneId.SIEGE) && attackerPlayer.isInsideZone(ZoneId.SIEGE))
			{
				return true;
			}
			
			if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && ((isGood() && attackerPlayer.isEvil()) || (isEvil() && attackerPlayer.isGood())))
			{
				return true;
			}
		}
		
		if ((attacker instanceof Defender) && (_clan != null))
		{
			final Siege siege = SiegeManager.getInstance().getSiege(this);
			return (siege != null) && siege.checkIsAttacker(_clan);
		}
		
		if (attacker instanceof Guard)
		{
			if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_GUARDS_ENABLED && ((_isGood && attacker.asNpc().getTemplate().isClan(FactionSystemConfig.FACTION_EVIL_TEAM_NAME)) || (_isEvil && attacker.asNpc().getTemplate().isClan(FactionSystemConfig.FACTION_GOOD_TEAM_NAME))))
			{
				return true;
			}
			
			return (getReputation() < 0); // Guards attack only PK players.
		}
		
		// Check if the Player has Karma.
		if ((getReputation() < 0) || (_pvpFlag > 0))
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Validates if a skill can be used by the actor.<br>
	 * This method checks various conditions such as player state, cooldowns, and requirements.<br>
	 * It handles logic for toggle skills, stance-based magic, and target validation.<br>
	 * If all conditions are met, it notifies the AI to begin casting.
	 * @param skill The {@code Skill} object to be used.
	 * @param item The {@code Item} being used with the skill, or {@code null}.
	 * @param forceUse Whether to force the use of the skill.
	 * @param dontMove Whether to prevent movement during the action.
	 * @return {@code true} if the skill can be cast; {@code false} otherwise.
	 */
	@Override
	public boolean useMagic(Skill skill, Item item, boolean forceUse, boolean dontMove)
	{
		if (skill == null)
		{
			return false;
		}
		
		Skill usedSkill = skill;
		final SkillReplacementList replacementList = getSkillReplacementListIfPresent();
		if ((replacementList != null) && replacementList.hasReplacement(usedSkill.getId()))
		{
			usedSkill = replacementList.resolveSkill(usedSkill);
		}
		
		// Passive skills cannot be used.
		if (usedSkill.isPassive())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Formal Wear allows no skills and no skill casting items.
		final Item formalWear = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if ((formalWear != null) && (formalWear.getTemplate().getBodyPart() == BodyPart.ALLDRESS))
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_SKILL_CASTING_ITEMS_WHILE_WEARING_FORMAL_WEAR);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// If Alternate rule Karma punishment is set to true, forbid skill Return to player with Karma.
		if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && (getReputation() < 0) && usedSkill.hasEffectType(EffectType.TELEPORT))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Players mounted on pets cannot use any toggle skills.
		if (usedSkill.isToggle() && isMounted())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Wyvern riders cannot use character skills, only the Wyvern Breath and Wyvern Aegis, items such as potions still work.
		if ((item == null) && (_mountType == MountType.WYVERN))
		{
			final int skillId = usedSkill.getId();
			if ((skillId != CommonSkill.WYVERN_BREATH.getId()) && (skillId != CommonSkill.WYVERN_AEGIS.getId()))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Riders use basic attacks but no physical skills, items such as potions still work.
		if ((item == null) && usedSkill.isPhysical() && isMounted())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Nor offensive skills, except the ones of the mount.
		if (isMounted() && usedSkill.hasNegativeEffect() && (!isGM() || GeneralConfig.GM_SKILL_RESTRICTION))
		{
			final int skillId = usedSkill.getId();
			if (!MountEnabledSkillList.contains(skillId))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
				sm.addSkillName(skillId);
				sendPacket(sm);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Support for wizard skills with stances (Fire, Water, Wind, Earth).
		final Skill attachedSkill = usedSkill.getAttachedSkill(this);
		if (attachedSkill != null)
		{
			usedSkill = attachedSkill;
		}
		
		// Alter skills
		if (_alterSkillActive)
		{
			sendPacket(new ExAlterSkillRequest(null, -1, -1, -1));
			_alterSkillActive = false;
		}
		
		final int alternateRangedSkillId = usedSkill.getAlternateRangedSkillId();
		if (alternateRangedSkillId > 0)
		{
			final Weapon weapon = getActiveWeaponItem();
			if (weapon != null)
			{
				if (weapon.getItemType().isRanged())
				{
					usedSkill = SkillData.getInstance().getSkill(alternateRangedSkillId, usedSkill.getLevel(), usedSkill.getSubLevel());
				}
				else
				{
					final int alternateMeleeSkillId = usedSkill.getAlternateMeleeSkillId();
					if (alternateMeleeSkillId > 0)
					{
						usedSkill = SkillData.getInstance().getSkill(alternateMeleeSkillId, usedSkill.getLevel(), usedSkill.getSubLevel());
					}
				}
			}
		}
		
		final int alternateEnemySkill = usedSkill.getAlternateEnemySkillId();
		if (alternateEnemySkill > 0)
		{
			final int alternateAllySkillId = usedSkill.getAlternateAllySkillId();
			usedSkill = SkillData.getInstance().getSkill(alternateEnemySkill, usedSkill.getLevel(), usedSkill.getSubLevel());
			if (alternateAllySkillId > 0)
			{
				final WorldObject target = getTarget();
				if ((target != null) && target.isPlayer())
				{
					final Player targetPlayer = target.asPlayer();
					if ((targetPlayer == this) || (((getParty() != null) && (getParty() == targetPlayer.getParty())) || ((getClan() != null) && (getClan() == targetPlayer.getClan())) || ((getAllyId() != 0) && (getAllyId() == targetPlayer.getAllyId()))))
					{
						usedSkill = SkillData.getInstance().getSkill(alternateAllySkillId, usedSkill.getLevel(), usedSkill.getSubLevel());
					}
				}
			}
		}
		
		// ************************************* Check Player State *******************************************
		
		// Abnormal effects(ex : Stun, Sleep...) are checked in Creature useMagic().
		if (!usedSkill.canCastWhileDisabled() && (isControlBlocked() || hasBlockActions()))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the player is dead.
		if (isDead())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if fishing and trying to use non-fishing skills.
		if (isFishing() && !usedSkill.hasEffectType(EffectType.FISHING, EffectType.FISHING_START))
		{
			sendPacket(SystemMessageId.ONLY_FISHING_SKILLS_MAY_BE_USED_AT_THIS_TIME);
			return false;
		}
		
		if (_observerMode.isActive())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_FUNCTION_IN_THE_SPECTATOR_MODE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (isSkillDisabled(usedSkill))
		{
			final SystemMessage sm;
			final long usedSkillHashCode = usedSkill.getReuseHashCode();
			if (hasSkillReuse(usedSkillHashCode))
			{
				final int remainingTime = (int) (TimeUtil.roundUpToSecond(getSkillRemainingReuseTime(usedSkillHashCode)) / 1000);
				final int hours = remainingTime / 3600;
				final int minutes = (remainingTime % 3600) / 60;
				final int seconds = (remainingTime % 60);
				if (hours > 0)
				{
					sm = new SystemMessage(SystemMessageId.S1_WILL_BE_AVAILABLE_AGAIN_IN_S2_H_S3_MIN_S4_SEC);
					sm.addSkillName(usedSkill);
					sm.addInt(hours);
					sm.addInt(minutes);
				}
				else if (minutes > 0)
				{
					sm = new SystemMessage(SystemMessageId.S1_WILL_BE_AVAILABLE_AGAIN_IN_S2_MIN_S3_SEC);
					sm.addSkillName(usedSkill);
					sm.addInt(minutes);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_WILL_BE_AVAILABLE_AGAIN_IN_S2_SEC);
					sm.addSkillName(usedSkill);
				}
				
				sm.addInt(seconds);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_IS_NOT_AVAILABLE_AT_THIS_TIME_BEING_PREPARED_FOR_REUSE);
				sm.addSkillName(usedSkill);
			}
			
			// Don't send packet for Raise/Focus Shield if Final Supreme Defense is active.
			if ((getEffectList().getBuffInfoBySkillId(10017 /* Supreme Defense */) == null) && ((usedSkill.getId() == 10020 /* Focus Shield */) || (usedSkill.getId() == 10021 /* Raise Shield */)))
			{
				sendPacket(sm);
			}
			
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster is sitting.
		if (_waitTypeSitting)
		{
			sendPacket(SystemMessageId.YOU_CANNOT_MOVE_WHILE_SITTING);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the skill type is toggle and disable it, unless the toggle is necessary to be on.
		if (usedSkill.isToggle())
		{
			final int usedSkillId = usedSkill.getId();
			if (isAffectedBySkill(usedSkillId))
			{
				if (!usedSkill.isNecessaryToggle())
				{
					stopSkillEffects(SkillFinishType.REMOVED, usedSkillId);
				}
				
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			final int toggleGroupId = usedSkill.getToggleGroupId();
			if (toggleGroupId > 0)
			{
				getEffectList().stopAllTogglesOfGroup(toggleGroupId);
			}
		}
		
		// Check if the player uses "Fake Death" skill.
		// Note: do not check this before TOGGLE reset
		if (isFakeDeath())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Create and set a WorldObject containing the target of the skill.
		final WorldObject target = usedSkill.getTarget(this, forceUse, dontMove, true);
		final Location worldPosition = _currentSkillWorldPosition;
		if ((usedSkill.getTargetType() == TargetType.GROUND) && (worldPosition == null))
		{
			if (usedSkill.getAffectScope() == AffectScope.FAN_PB)
			{
				if (isInsideZone(ZoneId.PEACE))
				{
					sendPacket(SystemMessageId.YOU_CANNOT_ATTACK_IN_A_PEACEFUL_ZONE);
				}
				else if (getCurrentMp() < usedSkill.getMpConsume())
				{
					sendPacket(SystemMessageId.NOT_ENOUGH_MP);
				}
				else if (usedSkill.checkCondition(this, target, true))
				{
					sendPacket(new MagicSkillUse(this, this, usedSkill.getDisplayId(), usedSkill.getDisplayLevel(), 0, 0, usedSkill.getReuseDelayGroup(), -1, SkillCastingType.NORMAL, true));
				}
			}
			
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check the validity of the target.
		if (target == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if all casting conditions are completed.
		if (!usedSkill.checkCondition(this, target, true))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			
			// Upon failed conditions, next action is called.
			final NextActionType nextAction = usedSkill.getNextAction();
			if ((nextAction != NextActionType.NONE) && (target != this) && target.isAutoAttackable(this))
			{
				final Intention nextIntention = getAI().getNextIntention();
				if ((nextIntention == null) || (nextIntention != Intention.MOVE_TO))
				{
					if (nextAction == NextActionType.ATTACK)
					{
						getAI().setIntentionAttack(target);
					}
					else if (nextAction == NextActionType.CAST)
					{
						getAI().setIntentionCast(usedSkill, target, item, false, false);
					}
				}
			}
			
			return false;
		}
		
		final boolean doubleCast = isAffected(EffectFlag.DOUBLE_CAST) && usedSkill.canDoubleCast();
		
		// If a skill is currently being used, queue this one if this is not the same.
		// In case of double casting, check if both slots are occupied, then queue skill.
		if ((!doubleCast && (isAttackingNow() || isCastingNow(SkillCaster::isAnyNormalType))) || (isCastingNow(s -> s.getCastingType() == SkillCastingType.NORMAL) && isCastingNow(s -> s.getCastingType() == SkillCastingType.NORMAL_SECOND)))
		{
			// Do not queue skill if called by an item.
			if (item == null)
			{
				// Create a new SkillUseHolder object and queue it in the player _queuedSkill.
				setQueuedSkill(usedSkill, item, forceUse, dontMove);
			}
			
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (_queuedSkill != null)
		{
			setQueuedSkill(null, null, false, false);
		}
		
		// Notify the AI with CAST and target.
		getAI().setIntentionCast(usedSkill, target, item, forceUse, dontMove);
		return true;
	}
	
	/**
	 * Checks if a specific player is part of the current party.<br>
	 * This method verifies membership within both standard and command channels.
	 * @param looterId The unique identifier of the player to check.
	 * @return {@code true} if the player is in the party, {@code false} otherwise.
	 */
	public boolean isInLooterParty(int looterId)
	{
		final Player looter = World.getPlayer(looterId);
		
		// If Player is in a CommandChannel.
		if (isInParty() && _party.isInCommandChannel() && (looter != null))
		{
			return _party.getCommandChannel().getMembers().contains(looter);
		}
		
		if (isInParty() && (looter != null))
		{
			return _party.getMembers().contains(looter);
		}
		
		return false;
	}
	
	/**
	 * Checks if the player belongs to a mage class.<br>
	 * This method calls {@code getPlayerClass()} and verifies its type.
	 * @return {@code true} if the character is a mage, {@code false} otherwise.
	 */
	public boolean isMageClass()
	{
		return getPlayerClass().isMage();
	}
	
	/**
	 * Checks if the character is a Death Knight.
	 * @return {@code true} if the character is a Death Knight, {@code false} otherwise.
	 */
	public boolean isDeathKnight()
	{
		return _isDeathKnight;
	}
	
	/**
	 * Sets whether the player is a Death Knight.<br>
	 * Use {@code true} to enable this status.<br>
	 * Use {@code false} to disable this status.
	 * @param value The boolean state to set for the Death Knight status.
	 */
	public void setDeathKnight(boolean value)
	{
		_isDeathKnight = value;
	}
	
	/**
	 * Checks if the player is currently a shine maker.<br>
	 * This method returns {@code true} if the condition is met.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if the player is a shine maker, {@code false} otherwise.
	 */
	public boolean isShineMaker()
	{
		return _isShineMaker;
	}
	
	/**
	 * Sets whether the player is currently a shine maker.<br>
	 * This updates the {@code _isShineMaker} field.
	 * @param value The boolean state to set for the shine maker status.
	 */
	public void setShineMaker(boolean value)
	{
		_isShineMaker = value;
	}
	
	/**
	 * Checks if the current actor is a warg.
	 * @return {@code true} if the actor is a warg, {@code false} otherwise.
	 */
	public boolean isWarg()
	{
		return _isWarg;
	}
	
	/**
	 * Sets the {@code isWarg} status for the player.<br>
	 * This determines if the character is currently in a state of war.
	 * @param value The boolean value to set for the war status.
	 */
	public void setWarg(boolean value)
	{
		_isWarg = value;
	}
	
	/**
	 * Checks if the player is a samurai.
	 * @return {@code true} if the player is a samurai, {@code false} otherwise.
	 */
	public boolean isSamurai()
	{
		return _isSamurai;
	}
	
	/**
	 * Sets whether the player is currently a samurai.<br>
	 * This updates the {@code _isSamurai} flag for the character.
	 * @param value The boolean state to set for the samurai status.
	 */
	public void setSamurai(boolean value)
	{
		_isSamurai = value;
	}
	
	/**
	 * Checks if the player is currently riding a mount.<br>
	 * Returns {@code true} if the player has a mount equipped.<br>
	 * Returns {@code false} if the player is not on a mount.
	 * @return {@code true} if mounted, {@code false} otherwise.
	 */
	public boolean isMounted()
	{
		return _mountType != MountType.NONE;
	}
	
	/**
	 * Checks if the character is currently in a state where landing is prohibited.<br>
	 * This happens in no landing zones or during active sieges for non-owners.
	 * @return {@code true} if landing is restricted, {@code false} otherwise.
	 */
	public boolean checkLandingState()
	{
		// Check if char is in a no landing zone.
		if (isInsideZone(ZoneId.NO_LANDING))
		{
			return true;
		}
		else
		// If this is a castle that is currently being sieged, and the rider is NOT a castle owner.
		// he cannot land.
		// Castle owner is the leader of the clan that owns the castle where the pc is.
		if (isInsideZone(ZoneId.SIEGE) && !((getClan() != null) && (CastleManager.getInstance().getCastle(this) == CastleManager.getInstance().getCastleByOwner(getClan())) && (this == getClan().getLeader().getPlayer())))
		{
			return true;
		}
		
		return false;
	}
	
	// Returns false if the change of mount type fails.
	/**
	 * Sets the mount information for the player.<br>
	 * This method updates the mount type, NPC ID, and level.<br>
	 * It also adjusts flying status and skills based on the mount type.
	 * @param npcId The unique identifier of the mount NPC.
	 * @param npcLevel The level of the mount NPC.
	 */
	public void setMount(int npcId, int npcLevel)
	{
		final MountType type = MountType.findByNpcId(npcId);
		switch (type)
		{
			case NONE: // None
			{
				setFlying(false);
				break;
			}
			case STRIDER: // Strider
			{
				if (_nobleLevel > 0)
				{
					addSkill(CommonSkill.STRIDER_SIEGE_ASSAULT.getSkill(), false);
				}
				break;
			}
			case WYVERN: // Wyvern
			{
				setFlying(true);
				break;
			}
		}
		
		_mountType = type;
		_mountNpcId = npcId;
		_mountLevel = npcLevel;
	}
	
	/**
	 * Retrieves the current mount type of the player.
	 * @return the {@code MountType} of the player.
	 */
	public MountType getMountType()
	{
		return _mountType;
	}
	
	/**
	 * Stops all active effects on the actor.<br>
	 * This method calls the superclass implementation to clear effects.<br>
	 * It then updates and broadcasts the current status of the actor.
	 */
	@Override
	public void stopAllEffects()
	{
		super.stopAllEffects();
		updateAndBroadcastStatus();
	}
	
	/**
	 * Stops all active effects on the actor.<br>
	 * Effects that are designed to last through death will remain active.<br>
	 * This method is typically called when an actor dies or changes state.
	 */
	@Override
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		super.stopAllEffectsExceptThoseThatLastThroughDeath();
		updateAndBroadcastStatus();
	}
	
	/**
	 * Retrieves the {@code CubicList} of this player.
	 * @return the {@code CubicList} of this player.
	 */
	public CubicList getCubicList()
	{
		return _cubics;
	}
	
	/**
	 * Schedules a task to update abnormal visual effects.<br>
	 * This method sends an {@code ExUserInfoAbnormalVisualEffect} packet to the client.<br>
	 * It also broadcasts the character information and clears the internal task reference.
	 */
	@Override
	public void updateAbnormalVisualEffects()
	{
		if (_abnormalVisualEffectTask == null)
		{
			_abnormalVisualEffectTask = ThreadPool.schedule(() ->
			{
				sendPacket(new ExUserInfoAbnormalVisualEffect(this));
				broadcastCharInfo();
				_abnormalVisualEffectTask = null;
			}, 50);
		}
	}
	
	/**
	 * Updates the inventory blocking status of the player.<br>
	 * If {@code true}, it schedules a task to re-enable the inventory after a delay.
	 * @param value The new blocking status to set.
	 */
	public void setInventoryBlockingStatus(boolean value)
	{
		_inventoryDisable = value;
		if (value)
		{
			ThreadPool.schedule(new InventoryEnableTask(this), 1500);
		}
	}
	
	/**
	 * Checks if the inventory system is currently disabled.
	 * @return {@code true} if the inventory is disabled, {@code false} otherwise.
	 */
	public boolean isInventoryDisabled()
	{
		return _inventoryDisable;
	}
	
	/**
	 * Retrieves the enchantment level of the currently active weapon.<br>
	 * It returns {@code 0} if no weapon is equipped.<br>
	 * The result is capped at a maximum value of {@code 127}.
	 * @return The enchantment level as an {@code int}.
	 */
	public int getEnchantEffect()
	{
		final Item wpn = getActiveWeaponInstance();
		if (wpn == null)
		{
			return 0;
		}
		
		return Math.min(127, wpn.getEnchantLevel());
	}
	
	/**
	 * Sets the most recent {@code Npc} encountered as a folk NPC.<br>
	 * This updates the internal reference for tracking interactions.
	 * @param folkNpc The {@code Npc} object to store as the last folk NPC.
	 */
	public void setLastFolkNPC(Npc folkNpc)
	{
		_lastFolkNpc = folkNpc;
	}
	
	/**
	 * Retrieves the last {@code Npc} that was interacted with by this player.<br>
	 * This is useful for tracking recent interactions.
	 * @return the last {@code Npc} object or {@code null} if none exists.
	 */
	public Npc getLastFolkNPC()
	{
		return _lastFolkNpc;
	}
	
	/**
	 * Adds a specific item to the list of active soul shots.<br>
	 * This method registers an {@code itemId} for use in soul shot mechanics.
	 * @param itemId The unique identifier of the item to add.
	 */
	public void addAutoSoulShot(int itemId)
	{
		_activeSoulShots.add(itemId);
	}
	
	/**
	 * Removes a specific auto soul shot from the active list.<br>
	 * This method checks if the item exists in the current collection.
	 * @param itemId The unique identifier of the item to remove.
	 * @return {@code true} if the item was successfully removed, {@code false} otherwise.
	 */
	public boolean removeAutoSoulShot(int itemId)
	{
		return _activeSoulShots.remove(itemId);
	}
	
	/**
	 * Retrieves the set of active soul shots.
	 * @return a {@code Set<Integer>} containing the IDs of current soul shots.
	 */
	public Set<Integer> getAutoSoulShot()
	{
		return _activeSoulShots;
	}
	
	/**
	 * Refreshes the active soul shots for the character.<br>
	 * This method checks the inventory for valid items based on the provided types.<br>
	 * It triggers the appropriate action for each matching item found.
	 * @param physical Set to {@code true} to process soul shots.
	 * @param magic Set to {@code true} to process spirit shots.
	 * @param fish Set to {@code true} to process fishing shots.
	 */
	@Override
	public void rechargeShots(boolean physical, boolean magic, boolean fish)
	{
		for (int itemId : _activeSoulShots)
		{
			final Item item = _inventory.getItemByItemId(itemId);
			if (item == null)
			{
				removeAutoSoulShot(itemId);
				continue;
			}
			
			final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
			if (handler == null)
			{
				continue;
			}
			
			final ActionType defaultAction = item.getTemplate().getDefaultAction();
			if ((magic && (defaultAction == ActionType.SPIRITSHOT)) || (physical && (defaultAction == ActionType.SOULSHOT)) || (fish && (defaultAction == ActionType.FISHINGSHOT)))
			{
				handler.onItemUse(this, item, false);
			}
		}
	}
	
	/**
	 * Disables auto-shot for all active soul shots of a specific crystal level.<br>
	 * This method iterates through {@code _activeSoulShots}.<br>
	 * It checks if the item's crystal type matches the provided {@code crystalType}.<br>
	 * If it matches, the {@code disableAutoShot} method is called for that item.
	 * @param crystalType The level of the crystal type to filter by.
	 */
	public void disableAutoShotByCrystalType(int crystalType)
	{
		for (int itemId : _activeSoulShots)
		{
			if (ItemData.getInstance().getTemplate(itemId).getCrystalType().getLevel() == crystalType)
			{
				disableAutoShot(itemId);
			}
		}
	}
	
	/**
	 * Disables the automatic use of a specific item.<br>
	 * This method removes the item from the active soul shots list.<br>
	 * It also sends a notification packet to the player.
	 * @param itemId The unique identifier of the item to disable.
	 * @return {@code true} if the auto-shot was successfully disabled, {@code false} otherwise.
	 */
	public boolean disableAutoShot(int itemId)
	{
		if (_activeSoulShots.contains(itemId))
		{
			removeAutoSoulShot(itemId);
			sendPacket(new ExAutoSoulShot(itemId, false, 0));
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_DEACTIVATED);
			sm.addItemName(itemId);
			sendPacket(sm);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Disables all currently active automatic soul shots.<br>
	 * This method sends a packet to the client for each active item.<br>
	 * It also clears the internal list of active soul shots.
	 */
	public void disableAutoShotsAll()
	{
		for (int itemId : _activeSoulShots)
		{
			sendPacket(new ExAutoSoulShot(itemId, false, 0));
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_DEACTIVATED);
			sm.addItemName(itemId);
			sendPacket(sm);
		}
		
		_activeSoulShots.clear();
	}
	
	/**
	 * Retrieves the currently active ruby jewel.
	 * @return the {@code BroochJewel} object representing the active ruby jewel.
	 */
	public BroochJewel getActiveRubyJewel()
	{
		return _activeRubyJewel;
	}
	
	/**
	 * Sets the active {@code BroochJewel} for the player.<br>
	 * This method updates the internal reference to the current ruby jewel.
	 * @param jewel The {@code BroochJewel} object to set as active.
	 */
	public void setActiveRubyJewel(BroochJewel jewel)
	{
		_activeRubyJewel = jewel;
	}
	
	/**
	 * Retrieves the currently active sapphire jewel.
	 * @return the {@code BroochJewel} object representing the active sapphire.
	 */
	public BroochJewel getActiveSapphireJewel()
	{
		return _activeSapphireJewel;
	}
	
	/**
	 * Sets the currently active {@code SapphireJewel}.<br>
	 * This method updates the internal state of the player's active jewel.
	 * @param jewel The {@code BroochJewel} to set as active.
	 */
	public void setActiveSapphireJewel(BroochJewel jewel)
	{
		_activeSapphireJewel = jewel;
	}
	
	/**
	 * Updates the active Ruby and Sapphire jewels for the player.<br>
	 * This method checks the inventory for equipped {@code BroochJewel} items.<br>
	 * It sets the active jewel based on the highest priority found in the list.
	 */
	public void updateActiveBroochJewel()
	{
		final BroochJewel[] broochJewels = BroochJewel.values();
		
		// Update active Ruby jewel.
		setActiveRubyJewel(null);
		for (int i = broochJewels.length - 1; i > 0; i--)
		{
			final BroochJewel jewel = broochJewels[i];
			if (jewel.isRuby() && _inventory.isItemEquipped(jewel.getItemId()))
			{
				setActiveRubyJewel(jewel);
				break;
			}
		}
		
		// Update active Sapphire jewel.
		setActiveSapphireJewel(null);
		for (int i = broochJewels.length - 1; i > 0; i--)
		{
			final BroochJewel jewel = broochJewels[i];
			if (jewel.isSapphire() && _inventory.isItemEquipped(jewel.getItemId()))
			{
				setActiveSapphireJewel(jewel);
				break;
			}
		}
	}
	
	/**
	 * Retrieves the current clan privileges for this character.
	 * @return the {@code ClanPrivileges} object containing all permissions.
	 */
	public ClanPrivileges getClanPrivileges()
	{
		return _clanPrivileges;
	}
	
	/**
	 * Updates the privileges for a specific clan.<br>
	 * This method clones the provided {@code ClanPrivileges} object to set the new values.
	 * @param clanPrivileges The new {@code ClanPrivileges} to apply.
	 */
	public void setClanPrivileges(ClanPrivileges clanPrivileges)
	{
		_clanPrivileges = clanPrivileges.clone();
	}
	
	/**
	 * Checks if the actor has the required permissions.<br>
	 * It compares the provided {@code ClanAccess} against the current privileges.
	 * @param access The {@code ClanAccess} level to check.
	 * @return {@code true} if the actor has sufficient access, {@code false} otherwise.
	 */
	public boolean hasAccess(ClanAccess access)
	{
		return _clanPrivileges.hasMinimumPrivileges(access);
	}
	
	// baron etc
	/**
	 * Sets the pledge class ID for the player.<br>
	 * This method also triggers a check for item restrictions based on the new class.
	 * @param id The unique identifier of the pledge class to set.
	 */
	public void setPledgeClass(int id)
	{
		_pledgeClass = id;
		checkItemRestriction();
	}
	
	/**
	 * Retrieves the current pledge class of the player.
	 * @return the {@code int} value representing the pledge class.
	 */
	public int getPledgeClass()
	{
		return _pledgeClass;
	}
	
	/**
	 * Sets the pledge type for the player.<br>
	 * This updates the internal {@code _pledgeType} field with the provided value.
	 * @param typeId The unique identifier for the pledge type.
	 */
	public void setPledgeType(int typeId)
	{
		if (_clan != null)
		{
			_clan.createSubPledge(typeId);
		}
		
		_pledgeType = typeId;
	}
	
	/**
	 * Retrieves the current pledge type of the player.
	 * @return The integer value representing the {@code pledgeType}.
	 */
	@Override
	public int getPledgeType()
	{
		return _pledgeType;
	}
	
	/**
	 * Retrieves the current apprentice value.
	 * @return the {@code int} value of the apprentice.
	 */
	public int getApprentice()
	{
		return _apprentice;
	}
	
	/**
	 * Sets the unique identifier for the apprentice.<br>
	 * This updates the {@code _apprentice} field with the provided value.
	 * @param apprenticeId The unique ID of the apprentice to set.
	 */
	public void setApprentice(int apprenticeId)
	{
		_apprentice = apprenticeId;
	}
	
	/**
	 * Retrieves the sponsor ID of the actor.
	 * @return the {@code int} value representing the sponsor.
	 */
	public int getSponsor()
	{
		return _sponsor;
	}
	
	/**
	 * Sets the unique identifier for the player's sponsor.<br>
	 * This updates the {@code _sponsor} field with the provided value.
	 * @param sponsorId The unique ID of the sponsor to assign.
	 */
	public void setSponsor(int sponsorId)
	{
		_sponsor = sponsorId;
	}
	
	/**
	 * Retrieves the current bookmark slot index.
	 * @return The integer value of the {@code _bookmarkslot}.
	 */
	public int getBookMarkSlot()
	{
		return _bookmarkslot;
	}
	
	/**
	 * Updates the current bookmark slot for the player.<br>
	 * This method also sends a packet to synchronize the change with the client.
	 * @param slot The new {@code int} value for the bookmark slot.
	 */
	public void setBookMarkSlot(int slot)
	{
		_bookmarkslot = slot;
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	/**
	 * Sends a local system message to the player.<br>
	 * The {@code message} is localized before being sent.
	 * @param message The raw text content of the message to display.
	 */
	@Override
	public void sendMessage(String message)
	{
		sendPacket(new SystemMessage(SendMessageLocalisationData.getLocalisation(this, message)));
	}
	
	/**
	 * Sends a system message to the player.<br>
	 * This method handles localization based on the current configuration.
	 * @param message The text content to be displayed.
	 */
	public void sendSysMessage(String message)
	{
		if (GeneralConfig.GM_STARTUP_BUILDER_HIDE)
		{
			sendPacket(new CreatureSay(null, ChatType.GENERAL, "SYS", SendMessageLocalisationData.getLocalisation(this, message)));
		}
		else
		{
			sendMessage(message);
		}
	}
	
	/**
	 * Sets the visibility status of the player.<br>
	 * This method is only available to Game Masters.<br>
	 * It toggles silence, invulnerability, and invisibility based on the provided value.
	 * @param hide The desired visibility state.
	 * @return {@code true} if the status was successfully changed, or {@code false} otherwise.
	 */
	public boolean setHiding(boolean hide)
	{
		if (!isGM())
		{
			return false;
		}
		
		final boolean enteredWorld = hasEnteredWorld();
		if (enteredWorld)
		{
			final boolean invisible = isInvisible();
			if (invisible && hide)
			{
				// Already hiding.
				return false;
			}
			
			if (!invisible && !hide)
			{
				// Already visible.
				return false;
			}
		}
		
		setSilenceMode(hide);
		setInvul(hide);
		setInvisible(hide);
		
		final Pet pet = getPet();
		if (pet != null)
		{
			pet.setInvisible(hide);
		}
		
		for (Summon servitor : getServitors().values())
		{
			servitor.setInvisible(hide);
		}
		
		if (hide && enteredWorld)
		{
			World.forEachVisibleObject(this, Player.class, player ->
			{
				if (player.getTarget() == this)
				{
					player.setTarget(null);
				}
			});
		}
		
		broadcastUserInfo();
		sendPacket(new ExUserInfoAbnormalVisualEffect(this));
		return true;
	}
	
	/**
	 * Retrieves the {@code ObserverMode} of this player.
	 * @return the {@code ObserverMode} of this player.
	 */
	public ObserverMode getObserverMode()
	{
		return _observerMode;
	}
	
	/**
	 * Updates the last known location of the actor.<br>
	 * This method creates a new {@code Location} object using the current coordinates.
	 */
	public void setLastLocation()
	{
		_lastLoc = new Location(getX(), getY(), getZ());
	}
	
	/**
	 * Removes the last recorded location of the actor.<br>
	 * This method sets the {@code _lastLoc} variable to {@code null}.
	 */
	public void unsetLastLocation()
	{
		_lastLoc = null;
	}
	
	/**
	 * Sets the side of the olympiad for this actor.<br>
	 * This updates the {@code _olympiadSide} field with the provided value.
	 * @param i The integer representing the olympiad side.
	 */
	public void setOlympiadSide(int i)
	{
		_olympiadSide = i;
	}
	
	/**
	 * Retrieves the side of the olympiad for the current actor.
	 * @return The integer value representing the olympiad side.
	 */
	public int getOlympiadSide()
	{
		return _olympiadSide;
	}
	
	/**
	 * Sets the unique identifier for the Olympiad game.<br>
	 * This value identifies which specific game instance the player is participating in.
	 * @param id The {@code int} ID of the Olympiad game.
	 */
	public void setOlympiadGameId(int id)
	{
		_olympiadGameId = id;
	}
	
	/**
	 * Retrieves the unique identifier for the current Olympiad game.
	 * @return The {@code int} ID of the Olympiad game.
	 */
	public int getOlympiadGameId()
	{
		return _olympiadGameId;
	}
	
	/**
	 * Retrieves the most recent location of the actor.<br>
	 * This method returns the {@code Location} stored in the internal cache.
	 * @return the last known {@code Location} object.
	 */
	public Location getLastLocation()
	{
		return _lastLoc;
	}
	
	/**
	 * Retrieves the current teleport mode for the administrator.
	 * @return the {@code AdminTeleportType} of the current session.
	 */
	public AdminTeleportType getTeleMode()
	{
		return _teleportType;
	}
	
	/**
	 * Sets the teleport mode for the actor.<br>
	 * This method updates the {@code _teleportType} field.
	 * @param type The new {@code AdminTeleportType} to apply.
	 */
	public void setTeleMode(AdminTeleportType type)
	{
		_teleportType = type;
	}
	
	/**
	 * Updates the race ticket count for a specific index.<br>
	 * This method modifies the internal {@code _raceTickets} collection.
	 * @param i The index of the race ticket to update.
	 * @param value The new value to assign to the ticket at the specified index.
	 */
	public void setRaceTicket(int i, int value)
	{
		_raceTickets[i] = value;
	}
	
	/**
	 * Retrieves the number of race tickets for a specific index.
	 * @param i The index of the ticket to retrieve.
	 * @return The integer value of the race ticket at the given index.
	 */
	public int getRaceTicket(int i)
	{
		return _raceTickets[i];
	}
	
	/**
	 * Checks if the message refusal status is active.
	 * @return {@code true} if the message was refused, {@code false} otherwise.
	 */
	public boolean getMessageRefusal()
	{
		return _messageRefusal;
	}
	
	/**
	 * Updates the message refusal status of the actor.<br>
	 * This method sets the {@code _messageRefusal} field to the provided value.<br>
	 * It also sends an {@code EtcStatusUpdate} packet to synchronize the state.
	 * @param mode The new boolean value for the message refusal status.
	 */
	public void setMessageRefusal(boolean mode)
	{
		_messageRefusal = mode;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Sets the current diet mode for the actor.<br>
	 * This method updates the {@code _dietMode} field.
	 * @param mode The boolean value to set for the diet mode.
	 */
	public void setDietMode(boolean mode)
	{
		_dietMode = mode;
	}
	
	/**
	 * Checks if the player is currently in diet mode.
	 * @return {@code true} if diet mode is active, {@code false} otherwise.
	 */
	public boolean getDietMode()
	{
		return _dietMode;
	}
	
	/**
	 * Sets whether the player currently refuses to trade.<br>
	 * Use {@code true} to enable refusal and {@code false} to disable it.
	 * @param mode The boolean value to set for trade refusal.
	 */
	public void setTradeRefusal(boolean mode)
	{
		_tradeRefusal = mode;
	}
	
	/**
	 * Checks if the player has refused a trade.
	 * @return {@code true} if the trade was refused, {@code false} otherwise.
	 */
	public boolean getTradeRefusal()
	{
		return _tradeRefusal;
	}
	
	/**
	 * Sets whether the player is currently refusing to trade.<br>
	 * This updates the {@code _exchangeRefusal} status.
	 * @param mode The boolean value to set for exchange refusal.
	 */
	public void setExchangeRefusal(boolean mode)
	{
		_exchangeRefusal = mode;
	}
	
	/**
	 * Checks if the player has refused an exchange.
	 * @return {@code true} if the exchange was refused, {@code false} otherwise.
	 */
	public boolean getExchangeRefusal()
	{
		return _exchangeRefusal;
	}
	
	/**
	 * Retrieves the current list of blocked entities.<br>
	 * This method returns the {@code BlockList} object associated with this actor.
	 * @return The {@code BlockList} containing all blocked entries.
	 */
	public BlockList getBlockList()
	{
		return _blockList;
	}
	
	/**
	 * Checks if the specified player is currently blocked.<br>
	 * This returns {@code true} if all players are blocked or if this specific player is in the block list.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is blocked, {@code false} otherwise.
	 */
	public boolean isBlocking(Player player)
	{
		return _blockList.isBlockAll() || _blockList.isInBlockList(player);
	}
	
	/**
	 * Checks if the player is allowed to perform actions.<br>
	 * It verifies that the global block list is inactive and the specific player is not blocked.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the player is not blocked, {@code false} otherwise.
	 */
	public boolean isNotBlocking(Player player)
	{
		return !_blockList.isBlockAll() && !_blockList.isInBlockList(player);
	}
	
	/**
	 * Checks if the specified {@code Player} has blocked this entity.<br>
	 * It returns {@code true} if all players are blocked or if the specific player is in the block list.
	 * @param player The {@code Player} to check for a block status.
	 * @return {@code true} if the player is blocking this entity, {@code false} otherwise.
	 */
	public boolean isBlocked(Player player)
	{
		return player.getBlockList().isBlockAll() || player.getBlockList().isInBlockList(this);
	}
	
	/**
	 * Checks if the player is allowed to interact with this entity.<br>
	 * It returns {@code true} if the player is not blocked by a global block or a specific list.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the interaction is permitted, {@code false} otherwise.
	 */
	public boolean isNotBlocked(Player player)
	{
		return !player.getBlockList().isBlockAll() && !player.getBlockList().isInBlockList(this);
	}
	
	/**
	 * Updates the hero status of the player.<br>
	 * This method manages the addition or removal of skills based on the {@code hero} value.<br>
	 * It also synchronizes the skill list with the client.
	 * @param hero The boolean flag indicating if the character is a hero.
	 */
	public void setHero(boolean hero)
	{
		if (hero && (_baseClass == _activeClass))
		{
			for (Skill skill : SkillTreeData.getInstance().getHeroSkillTree())
			{
				addSkill(skill, false); // Don't persist hero skills into database.
			}
		}
		else
		{
			for (Skill skill : SkillTreeData.getInstance().getHeroSkillTree())
			{
				removeSkill(skill, false, true); // Just remove skills from non-hero players.
			}
		}
		
		_hero = hero;
		setPledgeClass(ClanMember.calculatePledgeClass(this));
		sendSkillList();
	}
	
	/**
	 * Sets whether the player is currently in Olympiad mode.<br>
	 * This flag determines if specific Olympiad rules apply to the actor.
	 * @param value The boolean state to set for {@code _inOlympiadMode}.
	 */
	public void setInOlympiadMode(boolean value)
	{
		_inOlympiadMode = value;
	}
	
	/**
	 * Sets whether the olympiad has started.<br>
	 * This updates the {@code _olympiadStart} status.
	 * @param value The boolean state to set for the olympiad start.
	 */
	public void setOlympiadStart(boolean value)
	{
		_olympiadStart = value;
	}
	
	/**
	 * Checks if the Olympiad event has currently started.
	 * @return {@code true} if the Olympiad is active, {@code false} otherwise.
	 */
	public boolean isOlympiadStart()
	{
		return _olympiadStart;
	}
	
	/**
	 * Checks if the player is currently in Olympiad mode.
	 * @return {@code true} if the player is in Olympiad mode, {@code false} otherwise.
	 */
	public boolean isInOlympiadMode()
	{
		return _inOlympiadMode;
	}
	
	/**
	 * Checks if the current actor is a hero.
	 * @return {@code true} if the actor is a hero, {@code false} otherwise.
	 */
	public boolean isHero()
	{
		return _hero;
	}
	
	/**
	 * Checks if the player has the legend status.<br>
	 * This method retrieves the value from {@code PlayerVariables}.<br>
	 * It returns {@code false} if the variable is not set.
	 * @return {@code true} if the player is a legend, otherwise {@code false}.
	 */
	public boolean isLegend()
	{
		return getVariables().getBoolean(PlayerVariables.IS_LEGEND, false);
	}
	
	/**
	 * Sets whether the player has the legend status.<br>
	 * This updates the {@code IS_LEGEND} variable in the player's variables map.
	 * @param legend The boolean value to set for the legend status.
	 */
	public void setLegend(boolean legend)
	{
		getVariables().set(PlayerVariables.IS_LEGEND, legend);
	}
	
	/**
	 * Checks if the player is currently in a duel.
	 * @return {@code true} if the player is in a duel, {@code false} otherwise.
	 */
	public boolean isInDuel()
	{
		return _isInDuel;
	}
	
	/**
	 * Sets the {@code _startingDuel} flag to {@code true}.<br>
	 * This method initializes the state for a starting duel.
	 */
	public void setStartingDuel()
	{
		_startingDuel = true;
	}
	
	/**
	 * Checks whether this player is in the process of starting a duel.
	 * @return {@code true} if a duel is starting, {@code false} otherwise.
	 */
	public boolean isStartingDuel()
	{
		return _startingDuel;
	}
	
	/**
	 * Retrieves the unique identifier for the current duel.
	 * @return the {@code int} value of the duel ID.
	 */
	public int getDuelId()
	{
		return _duelId;
	}
	
	/**
	 * Updates the current duel state of the player.<br>
	 * This method sets the internal {@code _duelState} variable to a new value.
	 * @param mode The integer representing the new duel mode.
	 */
	public void setDuelState(int mode)
	{
		_duelState = mode;
	}
	
	/**
	 * Retrieves the current state of the duel.
	 * @return The integer value representing the current {@code duelState}.
	 */
	public int getDuelState()
	{
		return _duelState;
	}
	
	/**
	 * Sets the duel status for the player.<br>
	 * If {@code duelId} is greater than {@code 0}, the player enters a duel state.<br>
	 * If {@code duelId} is {@code 0} or less, the player exits the duel and resumes normal behavior.
	 * @param duelId The unique identifier for the duel.
	 */
	public void setInDuel(int duelId)
	{
		if (duelId > 0)
		{
			_isInDuel = true;
			_duelState = Duel.DUELSTATE_DUELLING;
			_duelId = duelId;
		}
		else
		{
			if (_duelState == Duel.DUELSTATE_DEAD)
			{
				enableAllSkills();
				getStatus().startHpMpRegeneration();
			}
			
			_isInDuel = false;
			_duelState = Duel.DUELSTATE_NODUEL;
			_duelId = 0;
		}
		
		_startingDuel = false;
	}
	
	/**
	 * Retrieves the message sent when a duel cannot be initiated.<br>
	 * This method creates a new {@code SystemMessage} containing the reason for the failure.<br>
	 * It also updates the internal state to the default no-opponent message.
	 * @return A {@code SystemMessage} object containing the duel rejection text.
	 */
	public SystemMessage getNoDuelReason()
	{
		final SystemMessage sm = new SystemMessage(_noDuelReason);
		sm.addPcName(this);
		_noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
		return sm;
	}
	
	/**
	 * Checks if the character is eligible to start a duel.<br>
	 * This method verifies various conditions such as health, combat status, and location.<br>
	 * It updates {@code _noDuelReason} if the check fails.
	 * @return {@code true} if the character can duel, {@code false} otherwise.
	 */
	public boolean canDuel()
	{
		if (isInCombat() || isJailed())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_AS_THEY_ARE_CURRENTLY_IN_BATTLE;
			return false;
		}
		
		if (isDead() || isAlikeDead() || ((getCurrentHp() < (getMaxHp() / 2)) || (getCurrentMp() < (getMaxMp() / 2))))
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_AS_THEIR_HP_MP_50;
			return false;
		}
		
		if (_isInDuel || _startingDuel)
		{
			_noDuelReason = SystemMessageId.C1_IS_ALREADY_IN_A_DUEL;
			return false;
		}
		
		if (_inOlympiadMode || isRegisteredOnEvent())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_AS_THEY_ARE_CURRENTLY_IN_BATTLE;
			return false;
		}
		
		if (isCursedWeaponEquipped())
		{
			_noDuelReason = SystemMessageId.C1_IS_IN_A_CHAOTIC_OR_PURPLE_STATE_AND_CANNOT_PARTICIPATE_IN_A_DUEL;
			return false;
		}
		
		if (_privateStoreType != PrivateStoreType.NONE)
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_AS_THEY_ARE_CURRENTLY_IN_A_PRIVATE_STORE_OR_MANUFACTURE;
			return false;
		}
		
		if (isMounted() || isInBoat())
		{
			_noDuelReason = SystemMessageId.C1_IS_RIDING_A_BOAT_FENRIR_OR_STRIDER_AND_THEREFORE_CANNOT_DUEL;
			return false;
		}
		
		if (isFishing())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_AS_THEY_ARE_CURRENTLY_FISHING;
			return false;
		}
		
		if (isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.PEACE) || isInsideZone(ZoneId.SIEGE) || isInsideZone(ZoneId.NO_PVP))
		{
			_noDuelReason = SystemMessageId.C1_IS_IN_AN_AREA_WHERE_DUEL_IS_NOT_ALLOWED_AND_YOU_CANNOT_APPLY_FOR_A_DUEL;
			return false;
		}
		
		return true;
	}
	
	/**
	 * Retrieves the current noble level of the player.
	 * @return The integer value representing the noble level.
	 */
	public int getNobleLevel()
	{
		return _nobleLevel;
	}
	
	/**
	 * Updates the noble level of the character.<br>
	 * This method manages the acquisition or removal of noble skills based on the provided value.<br>
	 * It also synchronizes the skill list with the client.
	 * @param level The new noble level to set. Use 0 to remove all noble skills.
	 */
	public void setNobleLevel(int level)
	{
		if (level != 0)
		{
			SkillTreeData.getInstance().getNobleSkillAutoGetTree().forEach(skill -> addSkill(skill, false));
		}
		else
		{
			SkillTreeData.getInstance().getNobleSkillTree().forEach(skill -> removeSkill(skill, false, true));
		}
		
		_nobleLevel = level;
		setPledgeClass(ClanMember.calculatePledgeClass(this));
		sendSkillList();
	}
	
	/**
	 * Sets the level at which the character joined the academy.<br>
	 * This value is stored in the {@code _lvlJoinedAcademy} field.
	 * @param lvl The level to set for joining the academy.
	 */
	public void setLvlJoinedAcademy(int lvl)
	{
		_lvlJoinedAcademy = lvl;
	}
	
	/**
	 * Retrieves the level at which the character joined the academy.
	 * @return the academy join level as an {@code int}.
	 */
	public int getLvlJoinedAcademy()
	{
		return _lvlJoinedAcademy;
	}
	
	/**
	 * Checks if the player is a member of an academy.<br>
	 * It returns {@code true} if the level joined in the academy is greater than {@code 0}.
	 * @return {@code true} if the player is an academy member, {@code false} otherwise.
	 */
	@Override
	public boolean isAcademyMember()
	{
		return _lvlJoinedAcademy > 0;
	}
	
	/**
	 * Sets the team for this actor.<br>
	 * This method updates the team and broadcasts the new status to all related entities.
	 * @param team The {@code Team} object to assign.
	 */
	@Override
	public void setTeam(Team team)
	{
		super.setTeam(team);
		broadcastUserInfo();
		if ((GeneralConfig.BLUE_TEAM_ABNORMAL_EFFECT != null) || (GeneralConfig.RED_TEAM_ABNORMAL_EFFECT != null))
		{
			sendPacket(new ExUserInfoAbnormalVisualEffect(this));
		}
		
		if (_pet != null)
		{
			_pet.broadcastStatusUpdate();
		}
		
		if (hasServitors())
		{
			getServitors().values().forEach(Summon::broadcastStatusUpdate);
		}
	}
	
	/**
	 * Updates the peace preference for the player.<br>
	 * This value determines if the character seeks peaceful interactions.
	 * @param wantsPeace The new peace status to set.
	 */
	public void setWantsPeace(int wantsPeace)
	{
		_wantsPeace = wantsPeace;
	}
	
	/**
	 * Checks if the player has requested a state of peace.<br>
	 * This value determines how the character interacts with others.
	 * @return the current peace status as an {@code int}.
	 */
	public int getWantsPeace()
	{
		return _wantsPeace;
	}
	
	/**
	 * Sends the list of available skills to the client.<br>
	 * This method updates the user interface with current skill data.
	 */
	public void sendSkillList()
	{
		sendSkillList(0);
	}
	
	/**
	 * Sends the current list of skills to the client.<br>
	 * This method schedules a task to gather all available skills and their properties.<br>
	 * It also sends an {@code AcquireSkillList} packet to update the UI.
	 * @param lastLearnedSkillId The ID of the last skill learned by the player. Use 0 if none.
	 */
	public void sendSkillList(int lastLearnedSkillId)
	{
		if (_skillListTask == null)
		{
			_skillListTask = ThreadPool.schedule(() ->
			{
				boolean isDisabled = false;
				final SkillList skillList = new SkillList();
				final Transform transformation = getTransformation();
				final boolean visualOnlyTransform = (transformation != null) && transformation.isVisualOnly();
				final SkillReplacementList replacementList = getSkillReplacementListIfPresent();
				for (Skill skill : getSkillList())
				{
					if (_clan != null)
					{
						isDisabled = skill.isClanSkill() && (_clan.getReputationScore() <= 0);
					}
					
					if (visualOnlyTransform && (replacementList != null))
					{
						final Skill resolvedSkill = replacementList.resolveSkill(skill);
						skillList.addSkill(resolvedSkill.getDisplayId(), resolvedSkill.getReuseDelayGroup(), resolvedSkill.getDisplayLevel(), resolvedSkill.getSubLevel(), resolvedSkill.isPassive(), isDisabled, resolvedSkill.isEnchantable());
						continue;
					}
					
					// Consider skill replacements.
					final int skillDisplayId = skill.getDisplayId();
					final int originalSkillId = getOriginalSkill(skill.getId());
					if (originalSkillId != skillDisplayId)
					{
						final Skill originalSkill = SkillData.getInstance().getSkill(originalSkillId, skill.getLevel(), skill.getSubLevel());
						skillList.addSkill(originalSkill.getDisplayId(), originalSkill.getReuseDelayGroup(), originalSkill.getDisplayLevel(), originalSkill.getSubLevel(), originalSkill.isPassive(), isDisabled, originalSkill.isEnchantable());
					}
					else
					{
						skillList.addSkill(skillDisplayId, skill.getReuseDelayGroup(), skill.getDisplayLevel(), skill.getSubLevel(), skill.isPassive(), isDisabled, skill.isEnchantable());
					}
				}
				
				if (lastLearnedSkillId > 0)
				{
					skillList.setLastLearnedSkillId(lastLearnedSkillId);
				}
				
				sendPacket(skillList);
				sendPacket(new AcquireSkillList(this));
				if (visualOnlyTransform)
				{
					sendResolvedSkillShortcuts();
				}
				
				restoreAutoShortcutVisual();
				_skillListTask = null;
			}, 300);
		}
	}
	
	/**
	 * Sends the shortcuts of replaced skills under their final IDs.<br>
	 * The saved shortcuts are left alone, so nothing has to be undone but the packets.
	 */
	private void sendResolvedSkillShortcuts()
	{
		final SkillReplacementList replacementList = getSkillReplacementListIfPresent();
		if (replacementList == null)
		{
			return;
		}
		
		for (Shortcut shortcut : getAllShortcuts())
		{
			if (shortcut.getType() != ShortcutType.SKILL)
			{
				continue;
			}
			
			final Skill knownSkill = getKnownSkill(shortcut.getId());
			if (knownSkill == null)
			{
				continue;
			}
			
			final Skill resolvedSkill = replacementList.resolveSkill(knownSkill);
			if (resolvedSkill.getId() == knownSkill.getId())
			{
				continue;
			}
			
			final Shortcut resolvedShortcut = new Shortcut(shortcut.getSlot(), shortcut.getPage(), ShortcutType.SKILL, resolvedSkill.getDisplayId(), resolvedSkill.getDisplayLevel(), resolvedSkill.getSubLevel(), shortcut.getCharacterType());
			resolvedShortcut.setAutoUse(shortcut.isAutoUse());
			resolvedShortcut.setSharedReuseGroup(resolvedSkill.getReuseDelayGroup());
			sendPacket(new ShortcutRegister(resolvedShortcut, this));
		}
	}
	
	/**
	 * Sends the saved skill shortcuts back out, undoing the final IDs a visual transform put on the bar.
	 */
	public void restoreSkillShortcuts()
	{
		for (Shortcut shortcut : getAllShortcuts())
		{
			if (shortcut.getType() == ShortcutType.SKILL)
			{
				sendPacket(new ShortcutRegister(shortcut, this));
			}
		}
	}
	
	/**
	 * Schedules a task to send the maximum storage count packet.<br>
	 * This method ensures that the {@code ExStorageMaxCount} packet is sent after a delay of {@code 300} milliseconds.<br>
	 * It checks if the {@code _storageCountTask} is null before scheduling.
	 */
	public void sendStorageMaxCount()
	{
		if (_storageCountTask == null)
		{
			_storageCountTask = ThreadPool.schedule(() ->
			{
				sendPacket(new ExStorageMaxCount(this));
				_storageCountTask = null;
			}, 300);
		}
	}
	
	/**
	 * Schedules a task to send user boost statistics to the client.<br>
	 * This method sends packets for vitality, buffs, and passive bonuses.<br>
	 * It also sends the vitality effect information after a delay of 300 milliseconds.
	 */
	public void sendUserBoostStat()
	{
		if (_userBoostStatTask == null)
		{
			_userBoostStatTask = ThreadPool.schedule(() ->
			{
				sendPacket(new ExUserBoostStat(this, BonusExpType.VITALITY));
				sendPacket(new ExUserBoostStat(this, BonusExpType.BUFFS));
				sendPacket(new ExUserBoostStat(this, BonusExpType.PASSIVE));
				sendPacket(new ExVitalityEffectInfo(this));
				_userBoostStatTask = null;
			}, 300);
		}
	}
	
	/**
	 * Adds a new sub-class to the player character.<br>
	 * This method checks if the player has reached the maximum limit before adding.<br>
	 * It saves the new class data to the database and initializes the skill tree.
	 * @param classId The unique identifier for the class being added.
	 * @param classIndex The index position of the sub-class.
	 * @param isDualClass Whether this sub-class should be treated as a dual class.
	 * @return {@code true} if the sub-class was successfully added, {@code false} otherwise.
	 */
	public boolean addSubClass(int classId, int classIndex, boolean isDualClass)
	{
		if (_subclassLock)
		{
			return false;
		}
		
		_subclassLock = true;
		
		try
		{
			if ((getTotalSubClasses() == PlayerConfig.MAX_SUBCLASS) || (classIndex == 0))
			{
				return false;
			}
			
			final Map<Integer, SubClassHolder> subclasses = getSubClasses();
			if (subclasses.containsKey(classIndex))
			{
				return false;
			}
			
			// Note: Never change _classIndex in any method other than setActiveClass().
			
			final SubClassHolder newClass = new SubClassHolder();
			newClass.setPlayerClass(classId);
			newClass.setClassIndex(classIndex);
			newClass.setVitalityPoints(PlayerStat.MAX_VITALITY_POINTS);
			if (isDualClass)
			{
				newClass.setDualClassActive(true);
				newClass.setExp(ExperienceData.getInstance().getExpForLevel(PlayerConfig.BASE_DUALCLASS_LEVEL));
				newClass.setLevel(PlayerConfig.BASE_DUALCLASS_LEVEL);
			}
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(ADD_CHAR_SUBCLASS))
			{
				// Store the basic info about this new sub-class.
				statement.setInt(1, getObjectId());
				statement.setInt(2, newClass.getId());
				statement.setLong(3, newClass.getExp());
				statement.setLong(4, newClass.getSp());
				statement.setInt(5, newClass.getLevel());
				statement.setInt(6, newClass.getVitalityPoints());
				statement.setInt(7, newClass.getClassIndex());
				statement.setBoolean(8, newClass.isDualClass());
				statement.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "WARNING: Could not add character sub class for " + _name + ": " + e.getMessage(), e);
				return false;
			}
			
			// Commit after database INSERT in case exception is thrown.
			subclasses.put(newClass.getClassIndex(), newClass);
			
			final PlayerClass subTemplate = PlayerClass.getPlayerClass(classId);
			final Map<Long, SkillLearn> skillTree = SkillTreeData.getInstance().getCompleteClassSkillTree(subTemplate);
			final Map<Integer, Skill> prevSkillList = new HashMap<>();
			for (SkillLearn skillInfo : skillTree.values())
			{
				if ((skillInfo.getSkillId() == CommonSkill.DIVINE_INSPIRATION.getId()) && !PlayerConfig.AUTO_LEARN_DIVINE_INSPIRATION)
				{
					continue;
				}
				
				if ((skillInfo.getSkillId() > 11399) && (skillInfo.getSkillId() < 11405) && !PlayerConfig.AUTO_LEARN_FP_SKILLS)
				{
					continue;
				}
				
				if (skillInfo.getGetLevel() <= newClass.getLevel())
				{
					final Skill prevSkill = prevSkillList.get(skillInfo.getSkillId());
					final Skill newSkill = SkillData.getInstance().getSkill(skillInfo.getSkillId(), skillInfo.getSkillLevel());
					if (((prevSkill != null) && (prevSkill.getLevel() > newSkill.getLevel())) || SkillTreeData.getInstance().isRemoveSkill(subTemplate, skillInfo.getSkillId()))
					{
						continue;
					}
					
					prevSkillList.put(newSkill.getId(), newSkill);
					storeSkill(newSkill, prevSkill, classIndex);
				}
			}
			
			return true;
		}
		finally
		{
			_subclassLock = false;
			getStat().recalculateStats(false);
			updateAbnormalVisualEffects();
			
			// Version Prelude of War 2019.
			// Checks if the player owns a Cursed Weapon and restores skills/references immediately after a subclass change or transformation update.
			for (CursedWeapon cw : CursedWeaponsManager.getInstance().getCursedWeapons())
			{
				if (cw.getPlayerId() == getObjectId())
				{
					// Update player reference in the Cursed Weapon Manager.
					cw.setPlayer(this);
					setCursedWeaponEquippedId(cw.getItemId());
					
					// Apply skills immediately.
					cw.giveSkill();
					
					// Schedule a secondary check/apply to ensure skills persist after the class change animation/packet flow ends.
					ThreadPool.schedule(() ->
					{
						if (isOnline())
						{
							cw.giveSkill();
							sendSkillList();
						}
					}, 1000);
					
					break;
				}
			}
			
			sendSkillList();
		}
	}
	
	/**
	 * Removes an existing sub-class and replaces it with a new one.<br>
	 * This method handles the removal of skills, shortcuts, and database records.<br>
	 * It also cleans up dual-class specific variables if applicable.
	 * @param classIndex The index of the sub-class to be removed.
	 * @param newClassId The ID of the new sub-class to add.
	 * @param isDualClass Whether the new sub-class should be treated as a dual-class.
	 * @return {@code true} if the modification was successful, {@code false} otherwise.
	 */
	public boolean modifySubClass(int classIndex, int newClassId, boolean isDualClass)
	{
		final SubClassHolder subClass = getSubClasses().get(classIndex);
		if (subClass == null)
		{
			return false;
		}
		
		// Notify to scripts before class is removed.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PROFESSION_CANCEL, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerProfessionCancel(this, subClass.getId()), this);
		}
		
		if (subClass.isDualClass())
		{
			getVariables().remove(PlayerVariables.ABILITY_POINTS_DUAL_CLASS_SKILLS_A);
			getVariables().remove(PlayerVariables.ABILITY_POINTS_DUAL_CLASS_SKILLS_B);
			getVariables().remove(PlayerVariables.ABILITY_POINTS_USED_DUAL_CLASS_A);
			getVariables().remove(PlayerVariables.ABILITY_POINTS_USED_DUAL_CLASS_B);
			int revelationSkill = getVariables().getInt(PlayerVariables.REVELATION_SKILL_1_DUAL_CLASS, 0);
			if (revelationSkill != 0)
			{
				removeSkill(revelationSkill);
			}
			
			revelationSkill = getVariables().getInt(PlayerVariables.REVELATION_SKILL_2_DUAL_CLASS, 0);
			if (revelationSkill != 0)
			{
				removeSkill(revelationSkill);
			}
		}
		
		// Remove after stats are recalculated.
		getSubClasses().remove(classIndex);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement deleteHennas = con.prepareStatement(DELETE_CHAR_HENNA);
			PreparedStatement deleteShortcuts = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
			PreparedStatement deleteSkillReuse = con.prepareStatement(DELETE_SKILL_SAVE);
			PreparedStatement deleteSkills = con.prepareStatement(DELETE_CHAR_SKILLS);
			PreparedStatement deleteSubclass = con.prepareStatement(DELETE_CHAR_SUBCLASS))
		{
			final int objectId = getObjectId();
			
			// Remove class permitted hennas.
			for (int slot = 1; slot < 5; slot++)
			{
				final Henna henna = _hennas.get(slot);
				if (henna != null)
				{
					deleteHennas.setInt(1, objectId);
					deleteHennas.setInt(2, slot);
					deleteHennas.setInt(3, classIndex);
					deleteHennas.execute();
				}
			}
			
			// Remove all shortcuts info stored for this sub-class.
			deleteShortcuts.setInt(1, objectId);
			deleteShortcuts.setInt(2, classIndex);
			deleteShortcuts.execute();
			
			// Remove all effects info stored for this sub-class.
			deleteSkillReuse.setInt(1, objectId);
			deleteSkillReuse.setInt(2, classIndex);
			deleteSkillReuse.execute();
			
			// Remove all skill info stored for this sub-class.
			deleteSkills.setInt(1, objectId);
			deleteSkills.setInt(2, classIndex);
			deleteSkills.execute();
			
			// Remove all basic info stored about this sub-class.
			deleteSubclass.setInt(1, objectId);
			deleteSubclass.setInt(2, classIndex);
			deleteSubclass.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not modify sub class for " + _name + " to class index " + classIndex + ": " + e.getMessage(), e);
			return false;
		}
		
		return addSubClass(newClassId, classIndex, isDualClass);
	}
	
	/**
	 * Checks if the current actor belongs to a subclass.<br>
	 * It returns {@code true} if the {@code _classIndex} is greater than {@code 0}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the actor is a subclass, {@code false} otherwise.
	 */
	public boolean isSubClassActive()
	{
		return _classIndex > 0;
	}
	
	/**
	 * Checks if the character belongs to the Sixth Class group.
	 * @return {@code true} if the class is in the Sixth Class category, {@code false} otherwise.
	 */
	public boolean isAwakenedClass()
	{
		return isInCategory(CategoryType.SIXTH_CLASS_GROUP);
	}
	
	/**
	 * Activates the dual class for a specific sub-class.<br>
	 * This method updates the status of the sub-class at the given index.
	 * @param classIndex The index of the sub-class to modify.
	 */
	public void setDualClassActive(int classIndex)
	{
		if (isSubClassActive())
		{
			getSubClasses().get(_classIndex).setDualClassActive(true);
		}
	}
	
	/**
	 * Checks if the current character has a dual class active.<br>
	 * This method verifies that the subclass system is enabled and that the specific class index corresponds to a dual class.
	 * @return {@code true} if the dual class is active, {@code false} otherwise.
	 */
	public boolean isDualClassActive()
	{
		if (!isSubClassActive())
		{
			return false;
		}
		
		if (_subClasses.isEmpty())
		{
			return false;
		}
		
		final SubClassHolder subClass = _subClasses.get(_classIndex);
		if (subClass == null)
		{
			return false;
		}
		
		return subClass.isDualClass();
	}
	
	/**
	 * Checks if the actor has any dual classes.<br>
	 * It iterates through all sub-classes to find a match.
	 * @return {@code true} if at least one dual class exists, otherwise {@code false}.
	 */
	public boolean hasDualClass()
	{
		for (SubClassHolder subClass : _subClasses.values())
		{
			if (subClass.isDualClass())
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves the first dual class found in the list of subclasses.<br>
	 * This method searches through all available subclasses.<br>
	 * It returns the first {@code SubClassHolder} that satisfies the {@code isDualClass()} condition.<br>
	 * Returns {@code null} if no dual class is found.
	 * @return The first {@code SubClassHolder} representing a dual class, or {@code null}.
	 */
	public SubClassHolder getDualClass()
	{
		for (SubClassHolder subClass : _subClasses.values())
		{
			if (subClass.isDualClass())
			{
				return subClass;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the map of all registered sub-classes.<br>
	 * The keys are unique identifiers for each sub-class.
	 * @return a {@code Map<Integer, SubClassHolder>} containing the sub-classes.
	 */
	public Map<Integer, SubClassHolder> getSubClasses()
	{
		return _subClasses;
	}
	
	/**
	 * Calculates the total number of subclasses available.<br>
	 * This method retrieves all subclasses and returns their count.
	 * @return The total number of subclasses as an {@code int}.
	 */
	public int getTotalSubClasses()
	{
		return getSubClasses().size();
	}
	
	/**
	 * Retrieves the base class ID of the actor.<br>
	 * This value identifies the primary template used for this entity.
	 * @return The integer ID of the base class.
	 */
	public int getBaseClass()
	{
		return _baseClass;
	}
	
	/**
	 * Retrieves the ID of the currently active class.
	 * @return the {@code int} value representing the active class.
	 */
	public int getActiveClass()
	{
		return _activeClass;
	}
	
	/**
	 * Retrieves the unique index of the class. <br>
	 * This value identifies the specific class type in the system.
	 * @return The integer index of the class.
	 */
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	/**
	 * Sets the internal index for the character class.<br>
	 * This value is used to identify which class a player belongs to.
	 * @param classIndex The new index value to assign to the class.
	 */
	protected void setClassIndex(int classIndex)
	{
		_classIndex = classIndex;
	}
	
	/**
	 * Sets the active class for the player based on the provided ID.<br>
	 * This method retrieves the corresponding {@code PlayerTemplate}.<br>
	 * It also notifies any listeners of a profession change.
	 * @param classId The unique identifier for the character class.
	 */
	private void setClassTemplate(int classId)
	{
		_activeClass = classId;
		
		final PlayerTemplate pcTemplate = PlayerTemplateData.getInstance().getTemplate(classId);
		if (pcTemplate == null)
		{
			LOGGER.severe("Missing template for classId: " + classId);
			throw new Error();
		}
		
		// Set the template of the Player.
		setTemplate(pcTemplate);
		
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PROFESSION_CHANGE, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerProfessionChange(this, pcTemplate, isSubClassActive()), this);
		}
	}
	
	/**
	 * Tells whether a skill belongs to the character rather than to its current class.
	 * @param skillId The skill to check.
	 * @return {@code true} for hero and noblesse skills.
	 */
	private static boolean isCharacterWideSkill(int skillId)
	{
		final SkillTreeData skillTrees = SkillTreeData.getInstance();
		for (Skill skill : skillTrees.getHeroSkillTree())
		{
			if ((skill != null) && (skill.getId() == skillId))
			{
				return true;
			}
		}
		
		for (Skill skill : skillTrees.getNobleSkillTree())
		{
			if ((skill != null) && (skill.getId() == skillId))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Changes the character to a new subclass based on the provided index.<br>
	 * This method handles all necessary resets including skills, effects, and stats.<br>
	 * It prevents switching if the character is currently transformed.
	 * @param classIndex The index of the subclass to switch to.
	 */
	public void setActiveClass(int classIndex)
	{
		if (_subclassLock)
		{
			return;
		}
		
		_subclassLock = true;
		
		try
		{
			// Cannot switch or change subclasses while transformed.
			if (isTransformed())
			{
				return;
			}
			
			// Remove active item skills before saving char to database.
			// Because worn items may differ the next time this class is chosen.
			for (Item item : _inventory.getPaperdollItems(Item::isAugmented))
			{
				if ((item != null) && item.isEquipped())
				{
					item.getAugmentation().removeBonus(this);
				}
			}
			
			// Abort any kind of cast.
			abortCast();
			
			if (isChannelized())
			{
				getSkillChannelized().abortChannelization();
			}
			
			// 1. Call store() before modifying _classIndex to avoid skill effects rollover.
			// 2. Register the correct _classId against applied 'classIndex'.
			store(PlayerConfig.SUBCLASS_STORE_SKILL_COOLTIME);
			
			if (_sellingBuffs != null)
			{
				_sellingBuffs.clear();
			}
			
			// Hero and noblesse skills belong to the character, not to the class, so their reuse survives the switch.
			final Map<Long, TimeStamp> keptReuse = new HashMap<>();
			for (Entry<Long, TimeStamp> entry : getSkillReuseTimeStamps().entrySet())
			{
				if (isCharacterWideSkill(entry.getValue().getSkillId()))
				{
					keptReuse.put(entry.getKey(), entry.getValue());
				}
			}
			
			resetTimeStamps();
			getSkillReuseTimeStamps().putAll(keptReuse);
			
			// Clear charges.
			_charges.set(0);
			stopChargeTask();
			
			if (hasServitors())
			{
				getServitors().values().forEach(s -> s.unSummon(this));
			}
			
			if (classIndex == 0)
			{
				setClassTemplate(_baseClass);
			}
			else
			{
				try
				{
					setClassTemplate(getSubClasses().get(classIndex).getId());
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "Could not switch " + _name + "'s sub class to class index " + classIndex + ": " + e.getMessage(), e);
					return;
				}
			}
			
			_classIndex = classIndex;
			if (isInParty())
			{
				_party.recalculatePartyLevel();
			}
			
			// Update the character's change in class status.
			// 1. Remove any active cubics from the player.
			// 2. Renovate the characters table in the database with the new class info, storing also buff/effect data.
			// 3. Remove all existing skills.
			// 4. Restore all the learned skills for the current class from the database.
			// 5. Restore effect/buff data for the new class.
			// 6. Restore henna data for the class, applying the new stat modifiers while removing existing ones.
			// 7. Reset HP/MP/CP stats and send Server->Client character status packet to reflect changes.
			// 8. Restore shortcut data related to this class.
			// 9. Resend a class change animation effect to broadcast to all nearby players.
			_autoUseSettings.getAutoSkills().clear();
			_autoUseSettings.getAutoBuffs().clear();
			for (Skill oldSkill : getAllSkills())
			{
				removeSkill(oldSkill, false, true);
			}
			
			// stopAllEffectsExceptThoseThatLastThroughDeath();
			final EffectList effectList = getEffectList();
			effectList.stopEffects(info -> !info.getSkill().isStayAfterDeath(), true, false);
			
			// stopAllEffects();
			effectList.stopEffects(info ->
			{
				final Skill skill = info.getSkill();
				return !skill.isNecessaryToggle() && !skill.isIrreplaceableBuff();
			}, true, false);
			
			// In controversy with isNecessaryToggle above, new class rewarded skills should be rewarded bellow.
			effectList.stopAllToggles();
			
			// Update abnormal visual effects.
			sendPacket(new ExUserInfoAbnormalVisualEffect(this));
			_cubics.stopAll();
			
			_recipeBook.restore(false);
			
			restoreSkills();
			rewardSkills();
			regiveTemporarySkills();
			getInventory().applyItemSkills();
			getHomunculusList().restore();
			_relics.restoreCollectionBonuses();
			_collections.restoreBonuses();
			
			// Prevents some issues when changing between subclases that shares skills.
			resetDisabledSkills();
			
			restoreEffects();
			
			sendPacket(new EtcStatusUpdate(this));
			
			_hennas.restore();
			sendPacket(new HennaInfo(this));
			
			final long maxHp = getMaxHp();
			if (getCurrentHp() > maxHp)
			{
				setCurrentHp(maxHp);
			}
			
			final int maxMp = getMaxMp();
			if (getCurrentMp() > maxMp)
			{
				setCurrentMp(maxMp);
			}
			
			final int maxCp = getMaxCp();
			if (getCurrentCp() > maxCp)
			{
				setCurrentCp(maxCp);
			}
			
			refreshOverloaded(true);
			broadcastUserInfo();
			
			// Clear resurrect xp calculation.
			setExpBeforeDeath(0);
			
			_shortcuts.restoreMe();
			sendPacket(new ShortcutInit(this));
			broadcastPacket(new SocialAction(getObjectId(), SocialAction.LEVEL_UP));
			sendPacket(new SkillCoolTime(this));
			sendStorageMaxCount();
			
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SUB_CHANGE, this))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSubChange(this), this);
			}
		}
		finally
		{
			_subclassLock = false;
			getStat().recalculateStats(false);
			updateAbnormalVisualEffects();
			
			// Version Prelude of War 2019.
			// Checks if the player owns a Cursed Weapon and restores skills/references immediately after a subclass change or transformation update.
			for (CursedWeapon cw : CursedWeaponsManager.getInstance().getCursedWeapons())
			{
				if (cw.getPlayerId() == getObjectId())
				{
					// Update player reference in the Cursed Weapon Manager.
					cw.setPlayer(this);
					setCursedWeaponEquippedId(cw.getItemId());
					
					// Apply skills immediately.
					cw.giveSkill();
					
					// Schedule a secondary check/apply to ensure skills persist after the class change animation/packet flow ends.
					ThreadPool.schedule(() ->
					{
						if (isOnline())
						{
							cw.giveSkill();
							sendSkillList();
						}
					}, 1000);
					
					break;
				}
			}
			
			sendSkillList();
		}
	}
	
	/**
	 * Checks if the subclass lock is currently active.<br>
	 * This method returns the current state of the {@code _subclassLock} variable.
	 * @return {@code true} if the subclass is locked, and {@code false} otherwise.
	 */
	public boolean isSubclassLocked()
	{
		return _subclassLock;
	}
	
	/**
	 * Stops the task that warns users to take a break.<br>
	 * This method cancels the active {@code _taskWarnUserTakeBreak} if it exists.<br>
	 * It then sets the task reference to {@code null}.
	 */
	public void stopWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak != null)
		{
			_taskWarnUserTakeBreak.cancel(true);
			_taskWarnUserTakeBreak = null;
		}
	}
	
	/**
	 * Starts a recurring task to remind users to take a break.<br>
	 * This method schedules the {@code WarnUserTakeBreakTask} every 1 hour.<br>
	 * It ensures that only one instance of the task is running at a time.
	 */
	public void startWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak == null)
		{
			_taskWarnUserTakeBreak = ThreadPool.scheduleAtFixedRate(new WarnUserTakeBreakTask(this), 3600000, 3600000);
		}
	}
	
	/**
	 * Stops the active pet rental task.<br>
	 * This method handles the logic for when a rented pet expires.<br>
	 * It teleports the player to a town if they are in a flying zone and then cancels the {@code _taskRentPet} task.
	 */
	public void stopRentPet()
	{
		if (_taskRentPet != null)
		{
			// If the rent of a wyvern expires while over a flying zone, tp to down before unmounting.
			if (checkLandingState() && (_mountType == MountType.WYVERN))
			{
				teleToLocation(TeleportWhereType.TOWN);
			}
			
			if (dismount()) // This should always be true now, since we teleported already.
			{
				_taskRentPet.cancel(true);
				_taskRentPet = null;
			}
		}
	}
	
	/**
	 * Starts a recurring task to handle pet rentals.<br>
	 * This method schedules the {@code RentPetTask} using the {@code ThreadPool}.<br>
	 * The task repeats every {@code seconds} amount of time.
	 * @param seconds The interval in seconds between each task execution.
	 */
	public void startRentPet(int seconds)
	{
		if (_taskRentPet == null)
		{
			_taskRentPet = ThreadPool.scheduleAtFixedRate(new RentPetTask(this), seconds * 1000, seconds * 1000);
		}
	}
	
	/**
	 * Checks if the pet is currently being rented.<br>
	 * Returns {@code true} if a rental task exists.<br>
	 * Returns {@code false} otherwise.
	 * @return {@code true} if the pet is rented, {@code false} otherwise.
	 */
	public boolean isRentedPet()
	{
		return _taskRentPet != null;
	}
	
	/**
	 * Stops the active water task for this player.<br>
	 * This method cancels the {@code _taskWater} scheduled task if it exists.<br>
	 * It also clears the reference and sends a {@code SetupGauge} packet to update the UI.
	 */
	public void stopWaterTask()
	{
		if (_taskWater != null)
		{
			_taskWater.cancel(false);
			_taskWater = null;
			sendPacket(new SetupGauge(getObjectId(), 2, 0));
		}
	}
	
	/**
	 * Starts the underwater breathing task for the player.<br>
	 * This method calculates the maximum time a player can stay underwater based on their {@code BREATH} stat.<br>
	 * It then sends a {@code SetupGauge} packet and schedules a repeating {@code WaterTask}.
	 */
	public void startWaterTask()
	{
		if (isDead() || (_taskWater != null))
		{
			return;
		}
		
		// Base time in water is 60 seconds (60000 milliseconds).
		final int baseTimeInWater = 60000;
		
		// Calculate the total stat as a percentage of the base time.
		final double breathPercentage = getStat().getValue(Stat.BREATH, getBaseTemplate().getBaseBreath()) / 100d;
		
		// Apply the percentage to the base time.
		final int timeInWater = (int) (baseTimeInWater * breathPercentage);
		
		// Send the setup gauge packet with the calculated time.
		sendPacket(new SetupGauge(getObjectId(), 2, timeInWater));
		
		// Schedule the water task.
		_taskWater = ThreadPool.scheduleAtFixedRate(new WaterTask(this), timeInWater, 1000);
	}
	
	/**
	 * Checks if the actor is currently in water.<br>
	 * This method returns {@code true} if a water task exists.<br>
	 * It returns {@code false} if no water task is present.
	 * @return {@code true} if the actor is in water, {@code false} otherwise.
	 */
	public boolean isInWater()
	{
		return _taskWater != null;
	}
	
	/**
	 * Checks if the player is currently in a water zone.<br>
	 * Starts the water task if the player is in {@code ZoneId.WATER}.<br>
	 * Stops the water task if the player is not in a water zone.
	 */
	public void checkWaterState()
	{
		if (isInsideZone(ZoneId.WATER))
		{
			startWaterTask();
		}
		else
		{
			stopWaterTask();
		}
	}
	
	/**
	 * Handles the logic when a player enters the game world.<br>
	 * This method initializes various states such as skills, buffs, and zone positions.<br>
	 * It also notifies friends and triggers relevant events for scripts and mentors.
	 */
	public void onPlayerEnter()
	{
		startWarnUserTakeBreak();
		
		if (isGM() && !GeneralConfig.GM_STARTUP_BUILDER_HIDE)
		{
			// Bleah, see L2J custom below.
			if (isInvul())
			{
				sendMessage("Entering world in Invulnerable mode.");
			}
			
			if (isInvisible())
			{
				sendMessage("Entering world in Invisible mode.");
			}
			
			if (_silenceMode)
			{
				sendMessage("Entering world in Silence mode.");
			}
		}
		
		// Apply item skills.
		_inventory.applyItemSkills();
		
		// Buff and status icons.
		if (PlayerConfig.STORE_SKILL_COOLTIME)
		{
			restoreEffects();
		}
		
		if ((_pkKills > FeatureConfig.PK_PENALTY_LIST_MINIMUM_COUNT) && (getReputation() < 0))
		{
			World.addPkPlayer(this);
		}
		
		revalidateZone(true);
		
		_friends.notifyStatus(FriendStatus.MODE_ONLINE);
		if (!isGM() && PlayerConfig.DECREASE_SKILL_LEVEL)
		{
			checkPlayerSkills();
		}
		
		try
		{
			final SayuneRequest sayune = getRequest(SayuneRequest.class);
			if (sayune != null)
			{
				sayune.onLogout();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			for (ZoneType zone : ZoneManager.getInstance().getZones(this))
			{
				zone.onPlayerLoginInside(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "", e);
		}
		
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_LOGIN, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerLogin(this), this);
		}
		
		if (isMentee())
		{
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_MENTEE_STATUS, this))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerMenteeStatus(this, true), this);
			}
		}
		else if (isMentor() && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_MENTOR_STATUS, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerMentorStatus(this, true), this);
		}
		
		// TODO : Need to fix that hack!
		if (!isDead())
		{
			// Run on a separate thread to give time to above events to be notified.
			ThreadPool.schedule(() ->
			{
				setCurrentHp(_originalHp);
				setCurrentMp(_originalMp);
				setCurrentCp(_originalCp);
			}, 300);
		}
	}
	
	/**
	 * Restores the player's current health, mana, and chaos points.<br>
	 * This method schedules a task to set these values to their maximum amounts.<br>
	 * It provides a small delay to allow for stat recalculations.
	 */
	@Override
	public void fullRestore()
	{
		ThreadPool.schedule(() ->
		{
			setCurrentHp(getMaxHp());
			setCurrentMp(getMaxMp());
			setCurrentCp(getMaxCp());
		}, 100 /* Compensate for recalculateStats task delay. */);
	}
	
	/**
	 * Retrieves the timestamp of the last access.<br>
	 * This value represents when the actor was last active.
	 * @return The {@code long} value of the last access time.
	 */
	public long getLastAccess()
	{
		return _lastAccess;
	}
	
	/**
	 * Updates the timestamp of the last access.<br>
	 * This method sets the internal {@code _lastAccess} field.
	 * @param lastAccess The new timestamp to set for the last access.
	 */
	protected void setLastAccess(long lastAccess)
	{
		_lastAccess = lastAccess;
	}
	
	/**
	 * Resurrects the actor and resets its status.<br>
	 * This method cancels decay tasks and updates the player's state.<br>
	 * It also teleports summons to the player if they are in a peace zone.<br>
	 * Finally, it notifies the current instance of the revival.
	 */
	@Override
	public void doRevive()
	{
		super.doRevive();
		
		// Stop decay task.
		DecayTaskManager.getInstance().cancel(this);
		
		sendPacket(new EtcStatusUpdate(this));
		_revivePet = false;
		_reviveRequested = 0;
		_revivePower = 0;
		
		// Teleport summons to player.
		if (isInsideZone(ZoneId.PEACE) && hasSummon())
		{
			final Pet pet = getPet();
			if (pet != null)
			{
				pet.teleToLocation(this, true);
			}
			
			for (Summon summon : getServitors().values())
			{
				if (!summon.isInsideZone(ZoneId.SIEGE))
				{
					summon.teleToLocation(this, true);
				}
			}
		}
		
		if (isMounted())
		{
			startFeed(_mountNpcId);
		}
		
		// Notify instance
		final Instance instance = getInstanceWorld();
		if (instance != null)
		{
			instance.doRevive(this);
		}
		
		// Stop Tranquil Soul effect.
		if (isAffectedBySkill(CommonSkill.TRANQUIL_SOUL.getId()))
		{
			final EffectList effectList = getEffectList();
			effectList.stopSkillEffects(SkillFinishType.REMOVED, CommonSkill.TRANQUIL_SOUL.getSkill());
			effectList.stopAbnormalVisualEffect(AbnormalVisualEffect.H_DEATH_EFFECT_AVE);
		}
		else
		{
			getEffectList().updateEffectList();
		}
		
		clearDamageTaken();
	}
	
	/**
	 * Revives the player and restores experience.<br>
	 * This method calls {@code doRevive()} before applying the power.
	 * @param revivePower The amount of experience to restore.
	 */
	@Override
	public void doRevive(double revivePower)
	{
		doRevive();
		restoreExp(revivePower);
	}
	
	/**
	 * Requests a resurrection for the player or their pet.<br>
	 * This method checks if resurrection is allowed and calculates restoration values.<br>
	 * It handles specific logic for pets, owners, and special items like Charm of Courage.
	 * @param reviver The {@code Player} who is attempting to perform the resurrection.
	 * @param isPet A boolean indicating if the target being resurrected is a pet.
	 * @param power The power level used to calculate experience restoration.
	 * @param reviveHp The percentage of health to restore.
	 * @param reviveMp The percentage of mana to restore.
	 * @param reviveCp The percentage of cp to restore.
	 */
	public void reviveRequest(Player reviver, boolean isPet, int power, int reviveHp, int reviveMp, int reviveCp)
	{
		if (isResurrectionBlocked())
		{
			return;
		}
		
		if (_reviveRequested == 1)
		{
			if (_revivePet == isPet)
			{
				reviver.sendPacket(SystemMessageId.RESURRECTION_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
			}
			else if (isPet)
			{
				reviver.sendPacket(SystemMessageId.YOU_CANNOT_RESURRECT_A_PET_WHILE_ITS_MASTER_IS_BEING_RESURRECTED); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
			}
			else
			{
				reviver.sendPacket(SystemMessageId.THE_PET_CANNOT_HELP_ITS_MASTER_WHILE_BEING_RESURRECTED); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
			}
			return;
		}
		
		if ((isPet && (_pet != null) && _pet.isDead()) || (!isPet && isDead()))
		{
			_reviveRequested = 1;
			_revivePower = Formulas.calculateSkillResurrectRestorePercent(power, reviver);
			_reviveHpPercent = reviveHp;
			_reviveMpPercent = reviveMp;
			_reviveCpPercent = reviveCp;
			_revivePet = isPet;
			if (hasCharmOfCourage())
			{
				final ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.YOUR_CHARM_OF_COURAGE_IS_TRYING_TO_RESURRECT_YOU_WOULD_YOU_LIKE_TO_RESURRECT_NOW.getId());
				dlg.addTime(60000);
				sendPacket(dlg);
				return;
			}
			
			final long restoreExp = Math.round(((_expBeforeDeath - getExp()) * _revivePower) / 100);
			final ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.C1_IS_ATTEMPTING_TO_DO_A_RESURRECTION_THAT_RESTORES_S2_S3_XP_ACCEPT.getId());
			final SystemMessage dlgSm = dlg.getSystemMessage();
			dlgSm.addPcName(reviver);
			dlgSm.addLong(restoreExp);
			dlgSm.addInt(power);
			sendPacket(dlg);
		}
	}
	
	/**
	 * Processes the resurrection of a player or their pet.<br>
	 * This method checks if a revive is requested and valid.<br>
	 * It restores health, mana, and cp based on defined percentages.
	 * @param answer The response value to determine if the revive action should proceed.
	 */
	public void reviveAnswer(int answer)
	{
		if ((_reviveRequested != 1) || (!isDead() && !_revivePet) || (_revivePet && (_pet != null) && !_pet.isDead()))
		{
			return;
		}
		
		if (answer == 1)
		{
			if (!_revivePet)
			{
				if (_revivePower != 0)
				{
					doRevive(_revivePower);
				}
				else
				{
					doRevive();
				}
			}
			else if (_pet != null)
			{
				if (_revivePower != 0)
				{
					_pet.doRevive(_revivePower);
				}
				else
				{
					_pet.doRevive();
				}
			}
		}
		
		_reviveRequested = 0;
		_revivePower = 0;
		
		// Support for specific HP/MP/CP percentage restored.
		final Creature effected = _revivePet ? _pet : this;
		if (effected == null)
		{
			_reviveHpPercent = 0;
			_reviveMpPercent = 0;
			_reviveCpPercent = 0;
			return;
		}
		
		if (_reviveHpPercent > 0)
		{
			// ResurrectionHpBonus effect bonus.
			final double percent = Math.min(100, _reviveHpPercent + effected.getStat().getValue(Stat.RESURRECTION_HP_BONUS, 0));
			final double amount = (effected.getMaxHp() * percent) / 100;
			if (amount > 0)
			{
				effected.setCurrentHp(amount, true);
			}
			
			_reviveHpPercent = 0;
		}
		
		if (_reviveMpPercent > 0)
		{
			final double amount = (effected.getMaxMp() * _reviveMpPercent) / 100;
			if (amount > 0)
			{
				effected.setCurrentMp(amount, true);
			}
			
			_reviveMpPercent = 0;
		}
		
		if (_reviveCpPercent > 0)
		{
			final double amount = (effected.getMaxCp() * _reviveCpPercent) / 100;
			if (amount > 0)
			{
				effected.setCurrentCp(amount, true);
			}
			
			_reviveCpPercent = 0;
		}
	}
	
	/**
	 * Checks if a revive has been requested.
	 * @return {@code true} if the revive request is active, {@code false} otherwise.
	 */
	public boolean isReviveRequested()
	{
		return (_reviveRequested == 1);
	}
	
	/**
	 * Checks if the pet is currently in the process of being revived.
	 * @return {@code true} if the pet is reviving, {@code false} otherwise.
	 */
	public boolean isRevivingPet()
	{
		return _revivePet;
	}
	
	/**
	 * Resets the revival request status.<br>
	 * Sets {@code _reviveRequested} and {@code _revivePower} to {@code 0}.
	 */
	public void removeReviving()
	{
		_reviveRequested = 0;
		_revivePower = 0;
	}
	
	/**
	 * Handles the logic for when an action request is processed.<br>
	 * This method removes spawn and teleport protections from the player.<br>
	 * It also checks if any pets or servitors need to be restored based on configuration.
	 */
	public void onActionRequest()
	{
		if (isSpawnProtected())
		{
			setSpawnProtection(false);
			if (!isInsideZone(ZoneId.PEACE))
			{
				sendPacket(SystemMessageId.YOU_ARE_NO_LONGER_PROTECTED_FROM_AGGRESSIVE_MONSTERS);
			}
			
			final int objectId = getObjectId();
			if (PlayerConfig.RESTORE_SERVITOR_ON_RECONNECT && !hasSummon() && SummonInfoTable.getInstance().getServitors().containsKey(objectId))
			{
				SummonInfoTable.getInstance().restoreServitor(this);
			}
			
			if (PlayerConfig.RESTORE_PET_ON_RECONNECT && !hasSummon() && SummonInfoTable.getInstance().getPets().containsKey(objectId))
			{
				SummonInfoTable.getInstance().restorePet(this);
			}
		}
		
		if (isTeleportProtected())
		{
			setTeleportProtection(false);
			if (!isInsideZone(ZoneId.PEACE))
			{
				sendMessage("Teleport spawn protection ended.");
			}
		}
	}
	
	/**
	 * Teleports the actor to a specific location.<br>
	 * This method handles vehicle dismounting and height adjustments during teleportation.
	 * @param loc The destination {@code ILocational} object.
	 * @param allowRandomOffset Whether to apply a random offset to the destination coordinates.
	 */
	@Override
	public void teleToLocation(ILocational loc, boolean allowRandomOffset)
	{
		if ((_vehicle != null) && !_vehicle.isTeleporting())
		{
			setVehicle(null);
		}
		
		if (isFlyingMounted() && (loc.getZ() < -1005))
		{
			super.teleToLocation(loc.getX(), loc.getY(), -1005, loc.getHeading());
		}
		
		super.teleToLocation(loc, allowRandomOffset);
	}
	
	/**
	 * This method is called when the player is teleported.<br>
	 * It updates the last known position and revalidates the current zone.<br>
	 * It handles pet movements, clears tamed beasts, and manages auto-play tasks.<br>
	 * Finally, it broadcasts the new information to nearby players.
	 */
	@Override
	public void onTeleported()
	{
		// Stop auto peel.
		if (hasRequest(AutoPeelRequest.class))
		{
			sendPacket(new ExStopItemAutoPeel(true));
			sendPacket(new ExReadyItemAutoPeel(false, 0));
			removeRequest(AutoPeelRequest.class);
		}
		
		super.onTeleported();
		
		if (isInAirShip())
		{
			getAirShip().sendInfo(this);
		}
		else // Update last player position upon teleport.
		{
			setLastServerPosition(getX(), getY(), getZ());
		}
		
		// Force a revalidation.
		revalidateZone(true);
		
		checkItemRestriction();
		
		if ((PlayerConfig.PLAYER_TELEPORT_PROTECTION > 0) && !_inOlympiadMode)
		{
			setTeleportProtection(true);
		}
		
		// Trained beast is lost after teleport.
		for (TamedBeast tamedBeast : _tamedBeast)
		{
			tamedBeast.deleteMe();
		}
		
		_tamedBeast.clear();
		
		// Modify the position of the pet if necessary.
		if (_pet != null)
		{
			_pet.setFollowStatus(false);
			_pet.teleToLocation(getLocation(), false);
			((SummonAI) _pet.getAI()).setStartFollowController(true);
			_pet.setFollowStatus(true);
			_pet.setInstance(getInstanceWorld());
			_pet.updateAndBroadcastStatus(0);
		}
		
		getServitors().values().forEach(s ->
		{
			s.setFollowStatus(false);
			s.teleToLocation(getLocation(), false);
			((SummonAI) s.getAI()).setStartFollowController(true);
			s.setFollowStatus(true);
			s.setInstance(getInstanceWorld());
			s.updateAndBroadcastStatus(0);
		});
		
		// Show movie if available.
		if (_movieHolder != null)
		{
			sendPacket(new ExStartScenePlayer(_movieHolder.getMovie()));
		}
		
		// Close time limited zone window.
		final TimedHuntingZoneHolder holder = getTimedHuntingZone();
		if ((holder != null) && !isInsideZone(ZoneId.TIMED_HUNTING))
		{
			sendPacket(new TimedHuntingZoneExit(holder.getZoneId()));
			stopTimedHuntingZoneTask();
		}
		
		// Stop auto play.
		AutoPlayTaskManager.getInstance().stopAutoPlay(this);
		AutoUseTaskManager.getInstance().stopAutoUseTask(this);
		sendPacket(new ExAutoPlaySettingSend(_autoPlaySettings.getOptions(), false, _autoPlaySettings.doPickup(), _autoPlaySettings.getNextTargetMode(), _autoPlaySettings.isShortRange(), _autoPlaySettings.getAutoPotionPercent(), _autoPlaySettings.isRespectfulHunting(), _autoPlaySettings.getMacroIndex()));
		restoreAutoShortcutVisual();
		
		// Send info to nearby players.
		broadcastInfo();
	}
	
	/**
	 * Sets whether the character is currently in the process of teleporting.<br>
	 * This state helps manage movement and interaction logic during transitions.
	 * @param teleport The {@code boolean} value to set for the teleporting state.
	 */
	@Override
	public void setTeleporting(boolean teleport)
	{
		setTeleporting(teleport, true);
	}
	
	/**
	 * Updates the teleporting status of the actor.<br>
	 * This method also manages the {@code TeleportWatchdogTask} if enabled.
	 * @param teleport Set to {@code true} to start teleporting or {@code false} to stop it.
	 * @param useWatchDog Set to {@code true} to enable the watchdog timer logic.
	 */
	public void setTeleporting(boolean teleport, boolean useWatchDog)
	{
		super.setTeleporting(teleport);
		if (!useWatchDog)
		{
			return;
		}
		
		if (teleport)
		{
			if ((_teleportWatchdog == null) && (PlayerConfig.TELEPORT_WATCHDOG_TIMEOUT > 0))
			{
				synchronized (this)
				{
					if (_teleportWatchdog == null)
					{
						_teleportWatchdog = ThreadPool.schedule(new TeleportWatchdogTask(this), PlayerConfig.TELEPORT_WATCHDOG_TIMEOUT * 1000);
					}
				}
			}
		}
		else if (_teleportWatchdog != null)
		{
			_teleportWatchdog.cancel(false);
			_teleportWatchdog = null;
		}
	}
	
	/**
	 * Updates the last known server position of the actor.<br>
	 * This method sets the {@code x}, {@code y}, and {@code z} coordinates.
	 * @param x The new X coordinate.
	 * @param y The new Y coordinate.
	 * @param z The new Z coordinate.
	 */
	public void setLastServerPosition(int x, int y, int z)
	{
		_lastServerPosition.setXYZ(x, y, z);
	}
	
	/**
	 * Retrieves the last known position of the server.<br>
	 * This method returns the coordinates stored in {@code _lastServerPosition}.
	 * @return the last recorded {@code Location} object.
	 */
	public Location getLastServerPosition()
	{
		return _lastServerPosition;
	}
	
	/**
	 * Sets whether the blink effect is currently active.<br>
	 * This updates the {@code _blinkActive} state for the actor.
	 * @param value The boolean state to set for the blink effect.
	 */
	public void setBlinkActive(boolean value)
	{
		_blinkActive.set(value);
	}
	
	/**
	 * Checks if the blink effect is currently active.
	 * @return {@code true} if the blink is active, {@code false} otherwise.
	 */
	public boolean isBlinkActive()
	{
		return _blinkActive.get();
	}
	
	/**
	 * Adds experience and skill points to the character.<br>
	 * This method updates the player's stats safely using a lock.
	 * @param addToExp The amount of experience to add.
	 * @param addToSp The amount of skill points to add.
	 */
	@Override
	public void addExpAndSp(double addToExp, double addToSp)
	{
		synchronized (_expSpLock)
		{
			getStat().addExpAndSp(addToExp, addToSp, false);
		}
	}
	
	/**
	 * Updates the experience and skill points for the character.<br>
	 * This method handles the calculation based on the provided values.
	 * @param addToExp The amount of experience to add.
	 * @param addToSp The amount of skill points to add.
	 * @param useVitality Whether to use vitality for the calculation.
	 */
	public void addExpAndSp(double addToExp, double addToSp, boolean useVitality)
	{
		synchronized (_expSpLock)
		{
			getStat().addExpAndSp(addToExp, addToSp, useVitality);
		}
	}
	
	/**
	 * Removes experience and skill points from the player.<br>
	 * This method updates the {@code PlayerStat} of the actor.
	 * @param removeExp The amount of experience to subtract.
	 * @param removeSp The amount of skill points to subtract.
	 */
	public void removeExpAndSp(long removeExp, long removeSp)
	{
		getStat().removeExpAndSp(removeExp, removeSp, true);
	}
	
	/**
	 * Removes experience and skill points from the character.<br>
	 * This method updates the player's stats based on the provided values.<br>
	 * It can optionally send a notification message to the player.
	 * @param removeExp The amount of experience to subtract.
	 * @param removeSp The amount of skill points to subtract.
	 * @param sendMessage Whether to notify the player about this change.
	 */
	public void removeExpAndSp(long removeExp, long removeSp, boolean sendMessage)
	{
		getStat().removeExpAndSp(removeExp, removeSp, sendMessage);
	}
	
	/**
	 * Reduces the current health points of this entity based on an attack.<br>
	 * It also notifies all tamed beasts that their owner has been attacked.
	 * @param value The amount of damage to subtract from the health.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param skill The {@code Skill} used for the attack.
	 * @param isDOT Whether the damage is a Damage Over Time effect.
	 * @param directlyToHp Whether the damage applies directly to health points.
	 * @param critical Whether the hit is a critical strike.
	 * @param reflect Whether the damage should be reflected back to the attacker.
	 */
	@Override
	public void reduceCurrentHp(double value, Creature attacker, Skill skill, boolean isDOT, boolean directlyToHp, boolean critical, boolean reflect)
	{
		super.reduceCurrentHp(value, attacker, skill, isDOT, directlyToHp, critical, reflect);
		
		// Notify the tamed beast of attacks.
		for (TamedBeast tamedBeast : _tamedBeast)
		{
			tamedBeast.onOwnerGotAttacked(attacker);
		}
	}
	
	/**
	 * Sends a snoop message to all registered listeners.<br>
	 * This method creates a new {@code Snoop} object and broadcasts it along with the provided packet.
	 * @param type The category of the snoop message.
	 * @param name The name associated with the snoop.
	 * @param text The content of the snoop message.
	 * @param cs The {@code CreatureSay} packet to be sent to listeners.
	 */
	public void broadcastSnoop(ChatType type, String name, String text, CreatureSay cs)
	{
		if (_snoopListener.isEmpty())
		{
			return;
		}
		
		final Snoop sn = new Snoop(getObjectId(), _name, type, name, text);
		for (Player pci : _snoopListener)
		{
			if (pci != null)
			{
				pci.sendPacket(cs);
				pci.sendPacket(sn);
			}
		}
	}
	
	/**
	 * Adds a {@code Player} to the snoop listener list.<br>
	 * This method ensures that the player is only added if they are not already present.
	 * @param pci The {@code Player} object to add.
	 */
	public void addSnooper(Player pci)
	{
		if (!_snoopListener.contains(pci))
		{
			_snoopListener.add(pci);
		}
	}
	
	/**
	 * Removes the player from the snoop listener.<br>
	 * This stops the system from tracking this specific player's actions.
	 * @param pci The {@code Player} object to remove.
	 */
	public void removeSnooper(Player pci)
	{
		_snoopListener.remove(pci);
	}
	
	/**
	 * Adds a player to the list of snooped players.<br>
	 * This method checks if the {@code Player} is already present before adding it.
	 * @param pci The {@code Player} object to be added.
	 */
	public void addSnooped(Player pci)
	{
		if (!_snoopedPlayer.contains(pci))
		{
			_snoopedPlayer.add(pci);
		}
	}
	
	/**
	 * Removes a player from the list of snooped players.<br>
	 * This method updates the internal tracking for {@code _snoopedPlayer}.
	 * @param pci The {@code Player} to remove from the snooping list.
	 */
	public void removeSnooped(Player pci)
	{
		_snoopedPlayer.remove(pci);
	}
	
	/**
	 * Adds a specific action to the HTML action cache for a given scope.<br>
	 * This method stores the {@code action} string in the internal map.
	 * @param scope The {@code HtmlActionScope} that defines where the action belongs.
	 * @param action The name of the action to be added.
	 */
	public void addHtmlAction(HtmlActionScope scope, String action)
	{
		synchronized (_htmlActionCaches)
		{
			_htmlActionCaches[scope.ordinal()].add(action);
		}
	}
	
	/**
	 * Clears the cached HTML actions for a specific scope.<br>
	 * This method removes all entries from the internal cache associated with the provided {@code HtmlActionScope}.
	 * @param scope The {@code HtmlActionScope} used to identify which cache to clear.
	 */
	public void clearHtmlActions(HtmlActionScope scope)
	{
		synchronized (_htmlActionCaches)
		{
			_htmlActionCaches[scope.ordinal()].clear();
		}
	}
	
	/**
	 * Sets the origin object ID for an HTML action within a specific scope.<br>
	 * This method updates the internal mapping for the provided {@code HtmlActionScope}.<br>
	 * It ensures that the {@code npcObjId} is not less than {@code 0}.
	 * @param scope The {@code HtmlActionScope} to update.
	 * @param npcObjId The unique identifier of the NPC object.
	 */
	public void setHtmlActionOriginObjectId(HtmlActionScope scope, int npcObjId)
	{
		if (npcObjId < 0)
		{
			throw new IllegalArgumentException();
		}
		
		_htmlActionOriginObjectIds[scope.ordinal()] = npcObjId;
	}
	
	/**
	 * Retrieves the ID of the last object that triggered an HTML action.<br>
	 * This value is used to track the origin of specific UI interactions.
	 * @return the {@code int} identifier of the last source object.
	 */
	public int getLastHtmlActionOriginId()
	{
		return _lastHtmlActionOriginObjId;
	}
	
	/**
	 * Checks if the given action is valid based on a collection of cached actions.<br>
	 * It compares the {@code action} against each entry in {@code actionIter}.<br>
	 * The method returns {@code true} if a match is found or if the action starts with a known prefix.
	 * @param actionIter The collection of allowed HTML actions to check against.
	 * @param action The specific action string to validate.
	 * @return {@code true} if the action is valid, otherwise {@code false}.
	 */
	private boolean validateHtmlAction(Iterable<String> actionIter, String action)
	{
		for (String cachedAction : actionIter)
		{
			final int length = cachedAction.length();
			if (cachedAction.charAt(length - 1) == AbstractHtmlPacket.VAR_PARAM_START_CHAR)
			{
				if (action.startsWith(cachedAction.substring(0, length - 1).trim()))
				{
					return true;
				}
			}
			else if (cachedAction.equals(action))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the provided action matches a cached HTML action.<br>
	 * It compares the input string against all stored actions.
	 * @param action The action string to validate.
	 * @return The object ID of the matching origin, or -1 if no match is found.
	 */
	public int validateHtmlAction(String action)
	{
		synchronized (_htmlActionCaches)
		{
			for (int i = 0; i < _htmlActionCaches.length; ++i)
			{
				if (validateHtmlAction(_htmlActionCaches[i], action))
				{
					_lastHtmlActionOriginObjId = _htmlActionOriginObjectIds[i];
					return _lastHtmlActionOriginObjId;
				}
			}
		}
		
		return -1;
	}
	
	/**
	 * Checks if an action can be performed on a specific item.<br>
	 * It verifies ownership, current usage status, and special restrictions.
	 * @param objectId The unique identifier of the item to check.
	 * @param itemProcessType The type of operation being attempted on the item.
	 * @return {@code true} if the action is allowed, {@code false} otherwise.
	 */
	public boolean validateItemManipulation(int objectId, ItemProcessType itemProcessType)
	{
		final Item item = _inventory.getItemByObjectId(objectId);
		final int myId = getObjectId();
		if ((item == null) || (item.getOwnerId() != myId))
		{
			LOGGER.finest(myId + ": player tried to " + itemProcessType + " item he is not owner of.");
			return false;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting.
		if (((_pet != null) && (_pet.getControlObjectId() == objectId)) || (_mountObjectID == objectId))
		{
			return false;
		}
		
		if (isProcessingItem(objectId))
		{
			return false;
		}
		
		if (CursedWeaponsManager.getInstance().isCursed(item.getId()))
		{
			// Can not trade a cursed weapon.
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if the actor is currently inside a boat.<br>
	 * This method returns {@code true} if the vehicle is not {@code null} and is a boat.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the actor is in a boat, {@code false} otherwise.
	 */
	public boolean isInBoat()
	{
		return (_vehicle != null) && _vehicle.isBoat();
	}
	
	/**
	 * Retrieves the {@code Boat} associated with this actor.<br>
	 * This method casts the internal vehicle object to a {@code Boat}.
	 * @return the {@code Boat} instance.
	 */
	public Boat getBoat()
	{
		return (Boat) _vehicle;
	}
	
	/**
	 * Checks if the current actor is currently on an airship.<br>
	 * This method verifies that a vehicle exists and is of the airship type.
	 * @return {@code true} if the actor is on an airship, {@code false} otherwise.
	 */
	public boolean isInAirShip()
	{
		return (_vehicle != null) && _vehicle.isAirShip();
	}
	
	/**
	 * Retrieves the {@code AirShip} associated with this entity.<br>
	 * This method casts the internal vehicle object to an {@code AirShip}.
	 * @return the {@code AirShip} instance.
	 */
	public AirShip getAirShip()
	{
		return (AirShip) _vehicle;
	}
	
	/**
	 * Checks if the current vehicle is a {@code Shuttle}.
	 * @return {@code true} if the actor is currently in a shuttle, {@code false} otherwise.
	 */
	public boolean isInShuttle()
	{
		return _vehicle instanceof Shuttle;
	}
	
	/**
	 * Retrieves the {@code Shuttle} object associated with this entity.<br>
	 * This method casts the internal vehicle to a {@code Shuttle}.
	 * @return the {@code Shuttle} instance.
	 */
	public Shuttle getShuttle()
	{
		return (Shuttle) _vehicle;
	}
	
	/**
	 * Retrieves the current vehicle associated with this actor.
	 * @return the {@code Vehicle} object, or {@code null} if no vehicle is mounted.
	 */
	public Vehicle getVehicle()
	{
		return _vehicle;
	}
	
	/**
	 * Sets the current {@code Vehicle} for this actor.<br>
	 * If the new value is {@code null}, it removes this actor from the previous vehicle.
	 * @param v The {@code Vehicle} to assign.
	 */
	public void setVehicle(Vehicle v)
	{
		if ((v == null) && (_vehicle != null))
		{
			_vehicle.removePassenger(this);
		}
		
		_vehicle = v;
	}
	
	/**
	 * Checks if the actor is currently inside a vehicle.<br>
	 * Returns {@code true} if the actor is in a vehicle.<br>
	 * Returns {@code false} otherwise.
	 * @return {@code true} if the actor is in a vehicle, {@code false} otherwise.
	 */
	public boolean isInVehicle()
	{
		return _vehicle != null;
	}
	
	/**
	 * Sets whether the actor is currently in a crystallized state.<br>
	 * This updates the {@code _inCrystallize} flag for the entity.
	 * @param inCrystallize The new crystallization status to set.
	 */
	public void setInCrystallize(boolean inCrystallize)
	{
		_inCrystallize = inCrystallize;
	}
	
	/**
	 * Checks if the character is currently in a crystallized state.
	 * @return {@code true} if the character is crystallized, {@code false} otherwise.
	 */
	public boolean isInCrystallize()
	{
		return _inCrystallize;
	}
	
	/**
	 * Retrieves the current position of the actor while inside a vehicle.
	 * @return the {@code Location} of the vehicle.
	 */
	public Location getInVehiclePosition()
	{
		return _inVehiclePosition;
	}
	
	/**
	 * Sets the current position of the actor while it is inside a vehicle.<br>
	 * This updates the {@code _inVehiclePosition} field with the provided coordinates.
	 * @param pt The new {@code Location} to set for the vehicle position.
	 */
	public void setInVehiclePosition(Location pt)
	{
		_inVehiclePosition = pt;
	}
	
	/**
	 * Performs a full cleanup of the player's state.<br>
	 * This method handles logout logic including database updates and stopping active tasks.<br>
	 * It removes the player from various world systems like parties, clans, and matching rooms.<br>
	 * All inventory items and variables are saved before the player is removed from the world.
	 * @return {@code true} if the deletion process was successful.
	 */
	@Override
	public boolean deleteMe()
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_LOGOUT, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerLogout(this), this);
		}
		
		if (getReputation() < 0)
		{
			World.removePkPlayer(this);
		}
		
		try
		{
			for (ZoneType zone : ZoneManager.getInstance().getZones(this))
			{
				zone.onPlayerLogoutInside(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).
		try
		{
			if (!_isOnline)
			{
				LOGGER.log(Level.SEVERE, "deleteMe() called on offline character " + this, new RuntimeException());
			}
			
			setOnlineStatus(false, true);
			PlayerInfoTable.getInstance().setLastAccess(getObjectId(), System.currentTimeMillis());
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			if (GeneralConfig.ENABLE_BLOCK_CHECKER_EVENT && (_handysBlockCheckerEventArena != -1))
			{
				HandysBlockCheckerManager.getInstance().onDisconnect(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			if (_krateiArena != null)
			{
				_krateiArena.removePlayer(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			AerialCleftManager.getInstance().onLogout(this);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			_isOnline = false;
			_offlineMode.setPlayRunning(false);
			abortAttack();
			abortCast();
			stopMove(null);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Remove combat flag.
		try
		{
			if (_inventory.getItemByItemId(9819) != null)
			{
				final Fort fort = FortManager.getInstance().getFort(this);
				if (fort != null)
				{
					FortSiegeManager.getInstance().dropCombatFlag(this, fort.getResidenceId());
				}
				else
				{
					final BodyPart bodyPart = BodyPart.fromItem(_inventory.getItemByItemId(9819));
					_inventory.unEquipItemInBodySlot(bodyPart);
					destroyItem(ItemProcessType.DESTROY, _inventory.getItemByItemId(9819), null, true);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			if (_matchingRoom != null)
			{
				_matchingRoom.deleteMember(this, false);
			}
			
			MatchingRoomManager.getInstance().removeFromWaitingList(this);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			if (isFlying())
			{
				removeSkill(SkillData.getInstance().getSkill(CommonSkill.WYVERN_BREATH.getId(), 1));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Exit timed hunting zone.
		if (isInTimedHuntingZone())
		{
			teleToLocation(TeleportWhereType.TOWN);
			storeCharBase();
		}
		
		// Make sure player variables are stored.
		getVariables().saveNow();
		
		// Make sure account variables are stored.
		getAccountVariables().saveNow();
		
		// Recommendations must be saved before task (timer) is canceled.
		try
		{
			_recommendations.store();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Make sure relic/doll data is stored.
		try
		{
			_relics.store();
			_relics.storeCollections();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Stop the HP/MP/CP Regeneration task (scheduled tasks).
		try
		{
			stopAllTimers();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			setTeleporting(false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Cancel Attak or Cast.
		try
		{
			setTarget(null);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		
		// Stop all toggles.
		getEffectList().stopAllToggles();
		
		// Remove from world regions zones.
		final ZoneRegion region = ZoneManager.getInstance().getRegion(this);
		if (region != null)
		{
			region.removeFromZones(this);
		}
		
		// If a Party is in progress, leave it (and festival party).
		if (isInParty())
		{
			try
			{
				leaveParty();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		_cubics.stopAll();
		
		// Remove the Player from the world.
		try
		{
			decayMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		if (OlympiadManager.getInstance().isRegistered(this) || (getOlympiadGameId() != -1))
		{
			OlympiadManager.getInstance().removeDisconnectedCompetitor(this);
		}
		
		// If the Player has Pet, unsummon it.
		if (hasSummon())
		{
			try
			{
				Summon pet = _pet;
				if (pet != null)
				{
					pet.setRestoreSummon(true);
					pet.unSummon(this);
					
					// Dead pet wasn't unsummoned, broadcast npcinfo changes (pet will be without owner name - means owner offline).
					pet = _pet;
					if (pet != null)
					{
						pet.broadcastNpcInfo(0);
					}
				}
				
				getServitors().values().forEach(s ->
				{
					s.setRestoreSummon(true);
					s.unSummon(this);
				});
				
				// Servitors have stored their effects to the database during unSummon, so the in-memory cache for this player can be released now.
				SummonEffectTable.getInstance().removeServitorEffects(getObjectId());
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			} // Returns pet to control item.
		}
		
		if (_clan != null)
		{
			// Set the status for pledge member list to OFFLINE.
			try
			{
				final ClanMember clanMember = _clan.getClanMember(getObjectId());
				if (clanMember != null)
				{
					clanMember.setClanContribution(getClanContribution());
					clanMember.setClanContributionTotal(getClanContributionTotal());
					clanMember.setPlayer(null);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		if (getActiveRequester() != null)
		{
			// Deals with sudden exit in the middle of transaction.
			setActiveRequester(null);
			cancelActiveTrade();
		}
		
		// If the Player is a GM, remove it from the GM List.
		if (isGM())
		{
			try
			{
				AdminData.getInstance().deleteGm(this);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		try
		{
			// Check if the Player is in observer mode to set its position to its position.
			// before entering in observer mode.
			if (_observerMode.isActive())
			{
				setLocationInvisible(_lastLoc);
			}
			
			if (_vehicle != null)
			{
				_vehicle.oustPlayer(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Remove player from instance.
		final Instance inst = getInstanceWorld();
		if (inst != null)
		{
			try
			{
				inst.onPlayerLogout(this);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		try
		{
			_cubics.stopAll();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Update database with items in its inventory and remove them from the world.
		try
		{
			_inventory.deleteMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Update database with items in its warehouse and remove them from the world.
		try
		{
			getWarehouse().deleteMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			_freight.deleteMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			clearRefund();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		if (isCursedWeaponEquipped())
		{
			try
			{
				CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquippedId).setPlayer(null);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		if (_clanId > 0)
		{
			_clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
			_clan.broadcastToOnlineMembers(new ExPledgeCount(_clan));
			// ClanTable.getInstance().getClan(getClanId()).broadcastToOnlineMembers(new PledgeShowMemberListAdd(this));
		}
		
		for (Player player : _snoopedPlayer)
		{
			player.removeSnooper(this);
		}
		
		for (Player player : _snoopListener)
		{
			player.removeSnooped(this);
		}
		
		// Notify to scripts.
		if (isMentee())
		{
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_MENTEE_STATUS, this))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerMenteeStatus(this, false), this);
			}
		}
		else if (isMentor() && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_MENTOR_STATUS, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerMentorStatus(this, false), this);
		}
		
		try
		{
			_friends.notifyStatus(FriendStatus.MODE_OFFLINE);
			_blockList.playerLogout();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on deleteMe() notifyFriends: " + e.getMessage(), e);
		}
		
		// Stop all passives and augment options.
		getEffectList().stopAllPassives(false, false);
		getEffectList().stopAllOptions(false, false);
		
		PlayerAutoSaveTaskManager.getInstance().remove(this);
		
		return super.deleteMe();
	}
	
	/**
	 * Retrieves the maximum inventory capacity for the player.<br>
	 * This value depends on the player's race and GM status.<br>
	 * It also includes bonuses from the {@code Stat.INVENTORY_NORMAL} stat.
	 * @return the total inventory limit as an {@code int}.
	 */
	public int getInventoryLimit()
	{
		int ivlim;
		if (isGM())
		{
			ivlim = PlayerConfig.INVENTORY_MAXIMUM_GM;
		}
		else if (getRace() == Race.DWARF)
		{
			ivlim = PlayerConfig.INVENTORY_MAXIMUM_DWARF;
		}
		else
		{
			ivlim = PlayerConfig.INVENTORY_MAXIMUM_NO_DWARF;
		}
		
		ivlim += (int) getStat().getValue(Stat.INVENTORY_NORMAL, 0);
		return ivlim;
	}
	
	/**
	 * Retrieves the total warehouse slot limit for the player.<br>
	 * The base limit depends on whether the character is a {@code DWARF}.<br>
	 * It also adds any bonus slots from the {@code STORAGE_PRIVATE} stat.
	 * @return The total number of available warehouse slots as an {@code int}.
	 */
	public int getWareHouseLimit()
	{
		int whlim;
		if (getRace() == Race.DWARF)
		{
			whlim = PlayerConfig.WAREHOUSE_SLOTS_DWARF;
		}
		else
		{
			whlim = PlayerConfig.WAREHOUSE_SLOTS_NO_DWARF;
		}
		
		whlim += (int) getStat().getValue(Stat.STORAGE_PRIVATE, 0);
		return whlim;
	}
	
	/**
	 * Retrieves the maximum number of items a player can sell in a private store.<br>
	 * This value depends on the player's race and their current trade sell stat.
	 * @return The total calculated limit for private store sales.
	 */
	public int getPrivateSellStoreLimit()
	{
		int pslim;
		if (getRace() == Race.DWARF)
		{
			pslim = PlayerConfig.MAX_PVTSTORESELL_SLOTS_DWARF;
		}
		else
		{
			pslim = PlayerConfig.MAX_PVTSTORESELL_SLOTS_OTHER;
		}
		
		pslim += (int) getStat().getValue(Stat.TRADE_SELL, 0);
		return pslim;
	}
	
	/**
	 * Retrieves the maximum number of items a player can buy from a private store.<br>
	 * This value depends on the player's race and their current trade buy stat.
	 * @return The total calculated purchase limit as an {@code int}.
	 */
	public int getPrivateBuyStoreLimit()
	{
		int pblim;
		if (getRace() == Race.DWARF)
		{
			pblim = PlayerConfig.MAX_PVTSTOREBUY_SLOTS_DWARF;
		}
		else
		{
			pblim = PlayerConfig.MAX_PVTSTOREBUY_SLOTS_OTHER;
		}
		
		pblim += (int) getStat().getValue(Stat.TRADE_BUY, 0);
		return pblim;
	}
	
	/**
	 * Retrieves the unique identifier of the mount NPC.<br>
	 * This value identifies which specific mount is currently being used.
	 * @return The {@code int} ID of the mount NPC.
	 */
	public int getMountNpcId()
	{
		return _mountNpcId;
	}
	
	/**
	 * Retrieves the P. Atk. of the ridden mount, the rider's worn items do not count.<br>
	 * A rider more than 5 levels below the mount loses 50% plus 5% per additional level.
	 * @return the ridden P. Atk., or 0 when not riding a mount with ride stats.
	 */
	public double getRidePAtk()
	{
		final PetLevelData data = getRideData();
		if ((data == null) || (data.getPAtkOnRide() <= 0))
		{
			return 0;
		}
		
		final int levelGap = getMountLevel() - getLevel();
		return (levelGap > 5) ? data.getPAtkOnRide() * Math.max(0, 0.5 - (0.05 * (levelGap - 6))) : data.getPAtkOnRide();
	}
	
	/**
	 * Retrieves the M. Atk. of the ridden mount, the rider's worn items do not count.
	 * @return the ridden M. Atk., or 0 when not riding a mount with ride stats.
	 */
	public double getRideMAtk()
	{
		final PetLevelData data = getRideData();
		return (data == null) ? 0 : data.getMAtkOnRide();
	}
	
	/**
	 * Retrieves the attack speed of the ridden mount, the rider's worn items do not count.
	 * @return the ridden attack speed, or 0 when not riding a mount with ride stats.
	 */
	public double getRideAttackSpeed()
	{
		final PetLevelData data = getRideData();
		return (data == null) ? 0 : data.getAttackSpeedOnRide();
	}
	
	/**
	 * Retrieves the level data of the ridden mount.
	 * @return the {@code PetLevelData} of the mount, or {@code null} when not riding.
	 */
	private PetLevelData getRideData()
	{
		return isMounted() ? PetData.getInstance().getPetLevelData(getMountNpcId(), getMountLevel()) : null;
	}
	
	/**
	 * Retrieves the current level of the player's mount.
	 * @return The integer value representing the mount level.
	 */
	public int getMountLevel()
	{
		return _mountLevel;
	}
	
	/**
	 * Updates the unique identifier for the current mount.<br>
	 * This method sets the {@code _mountObjectID} field to a new value.
	 * @param newID The new ID to assign to the mount.
	 */
	public void setMountObjectID(int newID)
	{
		_mountObjectID = newID;
	}
	
	/**
	 * Retrieves the unique identifier of the current mount.<br>
	 * This value represents the {@code objectId} of the mount being ridden.
	 * @return The {@code int} ID of the mount.
	 */
	public int getMountObjectID()
	{
		return _mountObjectID;
	}
	
	/**
	 * Retrieves the skill currently waiting in the queue.<br>
	 * This method returns the {@code SkillUseHolder} object for the next skill to be executed.
	 * @return the current {@code SkillUseHolder} or {@code null} if no skill is queued.
	 */
	public SkillUseHolder getQueuedSkill()
	{
		return _queuedSkill;
	}
	
	/**
	 * Sets the skill that is currently queued for use.<br>
	 * If {@code queuedSkill} is {@code null}, it clears the current queue.<br>
	 * Otherwise, it creates a new {@code SkillUseHolder} with the provided details.
	 * @param queuedSkill The {@code Skill} to be executed next.
	 * @param item The {@code Item} used to trigger the skill.
	 * @param ctrlPressed Whether the {@code Ctrl} key was held during activation.
	 * @param shiftPressed Whether the {@code Shift} key was held during activation.
	 */
	public void setQueuedSkill(Skill queuedSkill, Item item, boolean ctrlPressed, boolean shiftPressed)
	{
		if (queuedSkill == null)
		{
			_queuedSkill = null;
			return;
		}
		
		_queuedSkill = new SkillUseHolder(queuedSkill, item, ctrlPressed, shiftPressed);
	}
	
	/**
	 * Checks if the alter skill is currently active.
	 * @return {@code true} if the skill is active, {@code false} otherwise.
	 */
	public boolean isAlterSkillActive()
	{
		return _alterSkillActive;
	}
	
	/**
	 * Sets whether the alter skill is currently active.<br>
	 * This updates the {@code _alterSkillActive} state for the actor.
	 * @param alterSkillActive The new status to set as {@code true} or {@code false}.
	 */
	public void setAlterSkillActive(boolean alterSkillActive)
	{
		_alterSkillActive = alterSkillActive;
	}
	
	/**
	 * Checks if the actor is currently under a jail punishment.<br>
	 * This method verifies punishments for the character, account, IP address, and hardware ID.
	 * @return {@code true} if any of these identifiers are jailed, {@code false} otherwise.
	 */
	public boolean isJailed()
	{
		if (PunishmentManager.getInstance().hasPunishment(getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.JAIL) || PunishmentManager.getInstance().hasPunishment(getAccountName(), PunishmentAffect.ACCOUNT, PunishmentType.JAIL) || PunishmentManager.getInstance().hasPunishment(getIPAddress(), PunishmentAffect.IP, PunishmentType.JAIL))
		{
			return true;
		}
		
		if (_client == null)
		{
			return false;
		}
		
		final ClientHardwareInfoHolder hwInfo = _client.getHardwareInfo();
		return (hwInfo != null) && PunishmentManager.getInstance().hasPunishment(hwInfo.getMacAddress(), PunishmentAffect.HWID, PunishmentType.JAIL);
	}
	
	/**
	 * Checks if the player is currently restricted from using chat.<br>
	 * This method verifies punishments for the character, account, IP address, and hardware ID.
	 * @return {@code true} if any of these identifiers are banned from chatting, {@code false} otherwise.
	 */
	public boolean isChatBanned()
	{
		if (PunishmentManager.getInstance().hasPunishment(getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN) || PunishmentManager.getInstance().hasPunishment(getAccountName(), PunishmentAffect.ACCOUNT, PunishmentType.CHAT_BAN) || PunishmentManager.getInstance().hasPunishment(getIPAddress(), PunishmentAffect.IP, PunishmentType.CHAT_BAN))
		{
			return true;
		}
		
		if (_client == null)
		{
			return false;
		}
		
		final ClientHardwareInfoHolder hwInfo = _client.getHardwareInfo();
		return (hwInfo != null) && PunishmentManager.getInstance().hasPunishment(hwInfo.getMacAddress(), PunishmentAffect.HWID, PunishmentType.CHAT_BAN);
	}
	
	/**
	 * Starts a recurring task to manage the player's fame.<br>
	 * This method checks if the fame system is enabled and if the player meets the level requirements.<br>
	 * If valid, it schedules a {@code FameTask} using the provided delay and rate.
	 * @param delay The initial delay in milliseconds before the task starts.
	 * @param fameFixRate The rate at which the fame fix is applied.
	 */
	public void startFameTask(long delay, int fameFixRate)
	{
		if (!PlayerConfig.FAME_SYSTEM_ENABLED)
		{
			return;
		}
		
		if ((getLevel() < 40) || (getPlayerClass().level() < 2))
		{
			return;
		}
		
		if (_fameTask == null)
		{
			_fameTask = ThreadPool.scheduleAtFixedRate(new FameTask(this, fameFixRate), delay, delay);
		}
	}
	
	/**
	 * Stops the active fame task for this player.<br>
	 * This method cancels the {@code _fameTask} if it is currently running.<br>
	 * It then sets the {@code _fameTask} reference to {@code null}.
	 */
	public void stopFameTask()
	{
		if (_fameTask != null)
		{
			_fameTask.cancel(false);
			_fameTask = null;
		}
	}
	
	/**
	 * Retrieves the current power grade of the actor.
	 * @return the {@code int} value representing the power grade.
	 */
	public int getPowerGrade()
	{
		return _powerGrade;
	}
	
	/**
	 * Sets the current power grade for the actor.<br>
	 * This value determines the character's power level.
	 * @param power The new power grade to assign.
	 */
	public void setPowerGrade(int power)
	{
		_powerGrade = power;
	}
	
	/**
	 * Checks if the character has a cursed weapon equipped.<br>
	 * Returns {@code true} if a cursed weapon is currently active.<br>
	 * Returns {@code false} otherwise.
	 * @return {@code true} if a cursed weapon is equipped, {@code false} otherwise.
	 */
	public boolean isCursedWeaponEquipped()
	{
		return _cursedWeaponEquippedId != 0;
	}
	
	/**
	 * Sets the unique identifier for the currently equipped cursed weapon.<br>
	 * This updates the {@code _cursedWeaponEquippedId} field.
	 * @param value The ID of the cursed weapon to equip.
	 */
	public void setCursedWeaponEquippedId(int value)
	{
		_cursedWeaponEquippedId = value;
	}
	
	/**
	 * Retrieves the unique identifier of the currently equipped cursed weapon.
	 * @return The {@code int} ID of the cursed weapon.
	 */
	public int getCursedWeaponEquippedId()
	{
		return _cursedWeaponEquippedId;
	}
	
	/**
	 * Checks if a combat flag is currently equipped.
	 * @return {@code true} if the flag ID is not {@code null}, otherwise {@code false}.
	 */
	public boolean isCombatFlagEquipped()
	{
		return _combatFlagEquippedId;
	}
	
	/**
	 * Sets the ID of the equipped combat flag.<br>
	 * This updates the {@code _combatFlagEquippedId} field.
	 * @param value The unique identifier for the combat flag.
	 */
	public void setCombatFlagEquipped(boolean value)
	{
		_combatFlagEquippedId = value;
	}
	
	/**
	 * Retrieves the current number of charged souls.
	 * @return The total count of {@code _souls}.
	 */
	public int getChargedSouls()
	{
		return _souls;
	}
	
	/**
	 * Increases the current soul count of the player.<br>
	 * Sends a system message to notify the player of the change.<br>
	 * Restarts the soul task and updates the status packet.
	 * @param count The amount of souls to add.
	 */
	public void increaseSouls(int count)
	{
		_souls += count;
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_SOUL_COUNT_HAS_INCREASED_BY_S1_IT_IS_NOW_AT_S2);
		sm.addInt(count);
		sm.addInt(_souls);
		sendPacket(sm);
		restartSoulTask();
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Reduces the current amount of souls by a specific value.<br>
	 * Ensures that the soul count does not drop below {@code 0}.<br>
	 * Updates the soul task status and sends a status update packet.
	 * @param count The number of souls to remove from the character.
	 * @return Always returns {@code true} after processing the reduction.
	 */
	public boolean decreaseSouls(int count)
	{
		_souls -= count;
		if (_souls < 0)
		{
			_souls = 0;
		}
		
		if (_souls == 0)
		{
			stopSoulTask();
		}
		else
		{
			restartSoulTask();
		}
		
		sendPacket(new EtcStatusUpdate(this));
		return true;
	}
	
	/**
	 * Resets the current soul count to {@code 0}.<br>
	 * Stops any active soul tasks.<br>
	 * Sends an {@code EtcStatusUpdate} packet to update the client.
	 */
	public void clearSouls()
	{
		_souls = 0;
		stopSoulTask();
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Restarts the soul task for this entity.<br>
	 * It cancels any existing {@code _soulTask} if it is not {@code null}.<br>
	 * A new {@code ResetSoulsTask} is then scheduled to run after 600,000 milliseconds.
	 */
	private void restartSoulTask()
	{
		if (_soulTask != null)
		{
			_soulTask.cancel(false);
			_soulTask = null;
		}
		
		_soulTask = ThreadPool.schedule(new ResetSoulsTask(this), 600000);
	}
	
	/**
	 * Stops the active soul task for this entity.<br>
	 * This method cancels the {@code _soulTask} if it is not {@code null}.<br>
	 * It then sets the {@code _soulTask} reference to {@code null}.
	 */
	public void stopSoulTask()
	{
		if (_soulTask != null)
		{
			_soulTask.cancel(false);
			_soulTask = null;
		}
	}
	
	/**
	 * Retrieves the {@code DeathPenalty} of this player.
	 * @return the {@code DeathPenalty} of this player.
	 */
	public DeathPenalty getDeathPenalty()
	{
		return _deathPenalty;
	}
	
	/**
	 * Sends a damage message to the target creature.<br>
	 * Handles logic for missed attacks, critical hits, and blocked damage.<br>
	 * Updates Olympiad progress if applicable.
	 * @param target The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} used to perform the attack.
	 * @param damage The amount of damage dealt.
	 * @param crit Whether the hit was a critical strike.
	 * @param miss Whether the attack failed to hit the target.
	 */
	@Override
	public void sendDamageMessage(Creature target, Skill skill, int damage, boolean crit, boolean miss)
	{
		final int selfId = getObjectId();
		final int targetId = target.getObjectId();
		
		// Check if hit is missed.
		if (miss)
		{
			sendPacket(new ExDamagePopUp(selfId, targetId, 0, ExDamagePopUp.DODGE, skill));
			// target.sendPacket(new ExDamagePopUp(selfId, targetId, 0, ExDamagePopUp.DODGE));
			
			if (skill == null)
			{
				if (target.isPlayer())
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_EVADED_C2_S_ATTACK);
					sm.addPcName(target.asPlayer());
					sm.addString(getName());
					target.sendPacket(sm);
				}
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_ATTACK_WENT_ASTRAY);
				sm.addPcName(this);
				sendPacket(sm);
			}
			else
			{
				sendPacket(new ExMagicAttackInfo(selfId, targetId, ExMagicAttackInfo.EVADED));
			}
			return;
		}
		
		// Check if hit is critical.
		if (crit)
		{
			final boolean isMagic = (skill != null) && skill.isMagic();
			if (!isMagic)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_LANDED_A_CRITICAL_HIT);
				sm.addPcName(this);
				sendPacket(sm);
			}
			else
			{
				sendPacket(SystemMessageId.M_CRITICAL);
			}
			
			if (skill != null)
			{
				if (isMagic)
				{
					sendPacket(new ExMagicAttackInfo(selfId, targetId, ExMagicAttackInfo.M_CRITICAL));
				}
				else if (skill.isPhysical())
				{
					sendPacket(new ExMagicAttackInfo(selfId, targetId, ExMagicAttackInfo.P_CRITICAL));
				}
				else
				{
					sendPacket(new ExMagicAttackInfo(selfId, targetId, ExMagicAttackInfo.CRITICAL));
				}
			}
		}
		
		if (isInOlympiadMode() && target.isPlayer() && target.asPlayer().isInOlympiadMode() && (target.asPlayer().getOlympiadGameId() == getOlympiadGameId()))
		{
			OlympiadGameManager.getInstance().notifyCompetitorDamage(this, damage);
		}
		
		final boolean isHpBlocked = target.isHpBlocked();
		final boolean isNpc = target.isNpc();
		final boolean isPlayer = target.isPlayer();
		final boolean isInvul = target.isInvul();
		if ((isHpBlocked && !isNpc) || (isPlayer && target.isAffected(EffectFlag.DUELIST_FURY) && !isAffected(EffectFlag.FACEOFF)) || isInvul)
		{
			if (skill == null)
			{
				sendPacket(new ExDamagePopUp(selfId, targetId, 0, ExDamagePopUp.IMMUNE, skill));
				// target.sendPacket(new ExDamagePopUp(selfId, targetId, 0, ExDamagePopUp.IMMUNE));
			}
			else
			{
				sendPacket(new ExMagicAttackInfo(selfId, targetId, ExMagicAttackInfo.RESISTED));
				// target.sendPacket(new ExMagicAttackInfo(selfId, targetId, ExMagicAttackInfo.RESISTED));
			}
			
			sendPacket(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
			if (skill != null)
			{
				sendPacket(new ExMagicAttackInfo(getObjectId(), target.getObjectId(), ExMagicAttackInfo.BLOCKED));
			}
		}
		else if (target.isDoor() || (target instanceof ControlTower))
		{
			sendPacket(new ExDamagePopUp(selfId, targetId, damage, ExDamagePopUp.NORMAL_ATTACK, skill));
			target.sendPacket(new ExDamagePopUp(selfId, targetId, damage, ExDamagePopUp.NORMAL_ATTACK, skill));
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_VE_HIT_FOR_S1_DAMAGE);
			sm.addInt(damage);
			sendPacket(sm);
		}
		else if (this != target)
		{
			if (crit)
			{
				sendPacket(new ExDamagePopUp(selfId, targetId, damage, ExDamagePopUp.CRITICAL, skill));
				target.sendPacket(new ExDamagePopUp(selfId, targetId, damage, ExDamagePopUp.CRITICAL, skill));
			}
			else if (skill != null)
			{
				sendPacket(new ExDamagePopUp(selfId, targetId, damage, ExDamagePopUp.SKILL_HIT, skill));
				target.sendPacket(new ExDamagePopUp(selfId, targetId, damage, ExDamagePopUp.SKILL_HIT, skill));
			}
			else
			{
				sendPacket(new ExDamagePopUp(selfId, targetId, damage, ExDamagePopUp.NORMAL_ATTACK, skill));
				target.sendPacket(new ExDamagePopUp(selfId, targetId, damage, ExDamagePopUp.NORMAL_ATTACK, skill));
			}
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_DEALT_S3_DAMAGE_TO_C2);
			sm.addPcName(this);
			
			// Localisation related.
			String targetName = target.getName();
			if (MultilingualSupportConfig.MULTILANG_ENABLE && isNpc)
			{
				final String[] localisation = NpcNameLocalisationData.getInstance().getLocalisation(_lang, target.getId());
				if (localisation != null)
				{
					targetName = localisation[0];
				}
			}
			
			sm.addString(targetName);
			sm.addInt(damage);
			sm.addPopup(targetId, selfId, -damage);
			sendPacket(sm);
		}
	}
	
	/**
	 * Sets the unique identifier for the Agathion.<br>
	 * This value links the player to a specific NPC.
	 * @param npcId The {@code int} ID of the Agathion.
	 */
	public void setAgathionId(int npcId)
	{
		_agathionIds[0] = npcId;
	}
	
	/**
	 * Sets the unique identifier for the Agathion in a specific slot.
	 * @param index The {@code int} agathion slot index.
	 * @param npcId The {@code int} ID of the Agathion.
	 */
	public void setAgathionId(int index, int npcId)
	{
		if ((index >= 0) && (index < _agathionIds.length))
		{
			_agathionIds[index] = npcId;
		}
	}
	
	/**
	 * Retrieves the unique identifier for the Agathion.
	 * @return the {@code int} ID of the Agathion.
	 */
	public int getAgathionId()
	{
		return _agathionIds[0];
	}
	
	/**
	 * Retrieves the unique identifier for the Agathion in a specific slot.
	 * @param index The {@code int} agathion slot index.
	 * @return the {@code int} ID of the Agathion.
	 */
	public int getAgathionId(int index)
	{
		if ((index >= 0) && (index < _agathionIds.length))
		{
			return _agathionIds[index];
		}
		
		return 0;
	}
	
	/**
	 * Sets the agathion slot index that was last unequipped.
	 * @param slot The {@code int} agathion slot index.
	 */
	public void setLastAgathionSlot(int slot)
	{
		_lastAgathionSlot = slot;
	}
	
	/**
	 * Retrieves the agathion slot index that was last unequipped.
	 * @return the {@code int} agathion slot index.
	 */
	public int getLastAgathionSlot()
	{
		return _lastAgathionSlot;
	}
	
	/**
	 * Retrieves the current vitality points for the actor.<br>
	 * This value is fetched from the {@code PlayerStat} object.
	 * @return The number of vitality points as an {@code int}.
	 */
	public int getVitalityPoints()
	{
		return getStat().getVitalityPoints();
	}
	
	/**
	 * Updates the vitality points for the character.<br>
	 * This method modifies the underlying {@code PlayerStat} object.
	 * @param points The new amount of vitality points to set.
	 * @param quiet If {@code true}, the change will not trigger any notifications.
	 */
	public void setVitalityPoints(int points, boolean quiet)
	{
		getStat().setVitalityPoints(points, quiet);
	}
	
	/**
	 * Updates the vitality points for the character.<br>
	 * This method modifies the current vitality values based on the provided parameters.
	 * @param points The amount of vitality points to add or subtract.
	 * @param useRates Whether to apply specific rate multipliers to the points.
	 * @param quiet Whether to suppress any visual feedback or messages during the update.
	 */
	public void updateVitalityPoints(int points, boolean useRates, boolean quiet)
	{
		if ((_huntPass != null) && _huntPass.toggleSayha())
		{
			return;
		}
		
		getStat().updateVitalityPoints(points, useRates, quiet);
	}
	
	/**
	 * Checks all paperdoll slots for valid equipment.<br>
	 * Unequips items that no longer meet their required conditions.<br>
	 * Sends a notification to the player if an item is removed.
	 */
	public void checkItemRestriction()
	{
		for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
		{
			final Item equippedItem = _inventory.getPaperdollItem(i);
			if ((equippedItem != null) && !equippedItem.getTemplate().checkCondition(this, this, false))
			{
				_inventory.unEquipItemInSlot(i);
				
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(equippedItem);
				sendInventoryUpdate(iu);
				
				SystemMessage sm = null;
				if (equippedItem.getTemplate().getBodyPart() == BodyPart.BACK)
				{
					sendPacket(SystemMessageId.YOUR_CLOAK_HAS_BEEN_UNEQUIPPED_BECAUSE_YOUR_ARMOR_SET_IS_NO_LONGER_COMPLETE);
					return;
				}
				
				if (equippedItem.isEnchanted())
				{
					sm = new SystemMessage(SystemMessageId.S1_S2_UNEQUIPPED);
					sm.addInt(equippedItem.getEnchantLevel());
					sm.addItemName(equippedItem);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_UNEQUIPPED);
					sm.addItemName(equippedItem);
				}
				
				sendPacket(sm);
			}
		}
	}
	
	/**
	 * Adds a new transformation skill to the actor.<br>
	 * This method stores the {@code Skill} object using its unique ID as the key.
	 * @param skill The {@code Skill} object to be added.
	 */
	public void addTransformSkill(Skill skill)
	{
		_transformSkills.put(skill.getId(), skill);
	}
	
	/**
	 * Checks if the actor possesses a specific transform skill.<br>
	 * It verifies if the provided {@code Skill} object matches the one stored in the internal map.
	 * @param skill The {@code Skill} to check for.
	 * @return {@code true} if the actor has this transform skill, {@code false} otherwise.
	 */
	public boolean hasTransformSkill(Skill skill)
	{
		return _transformSkills.get(skill.getId()) == skill;
	}
	
	/**
	 * Checks if the actor has any transformation skills.<br>
	 * Returns {@code true} if the {@code transformSkills} list is not empty.<br>
	 * Returns {@code false} if no transformation skills are available.
	 * @return a boolean indicating if transformation skills exist.
	 */
	public boolean hasTransformSkills()
	{
		return !_transformSkills.isEmpty();
	}
	
	/**
	 * Retrieves all skills associated with transformations.
	 * @return a {@code Collection} of {@code Skill} objects.
	 */
	public Collection<Skill> getAllTransformSkills()
	{
		return _transformSkills.values();
	}
	
	/**
	 * Removes all active transform skills from the actor.<br>
	 * This method clears the {@code _transformSkills} collection.
	 */
	public void removeAllTransformSkills()
	{
		_transformSkills.clear();
	}
	
	/**
	 * Retrieves a specific skill based on its unique identifier.<br>
	 * This method checks for transform skills before falling back to the base class.
	 * @param skillId The unique ID of the skill to retrieve.
	 * @return The {@code Skill} object associated with the provided ID, or {@code null} if not found.
	 */
	@Override
	public Skill getKnownSkill(int skillId)
	{
		return !_transformSkills.isEmpty() ? _transformSkills.getOrDefault(skillId, super.getKnownSkill(skillId)) : super.getKnownSkill(skillId);
	}
	
	/**
	 * Retrieves the list of skills available to the player.<br>
	 * This method filters skills based on transformation status and class types.<br>
	 * It also excludes blocked or hidden alchemy skills.
	 * @return a {@code Collection} of {@code Skill} objects.
	 */
	public Collection<Skill> getSkillList()
	{
		Collection<Skill> currentSkills = getAllSkills();
		if (isTransformed() && !_transformSkills.isEmpty())
		{
			// Include transformation skills and those skills that are allowed during transformation.
			final List<Skill> filteredSkills = new ArrayList<>();
			for (Skill skill : currentSkills)
			{
				if (!skill.allowOnTransform())
				{
					continue;
				}
				
				filteredSkills.add(skill);
			}
			
			currentSkills = filteredSkills;
			
			// Revelation skills.
			if (isDualClassActive())
			{
				int revelationSkill = getVariables().getInt(PlayerVariables.REVELATION_SKILL_1_DUAL_CLASS, 0);
				if (revelationSkill != 0)
				{
					addSkill(SkillData.getInstance().getSkill(revelationSkill, 1), false);
				}
				
				revelationSkill = getVariables().getInt(PlayerVariables.REVELATION_SKILL_2_DUAL_CLASS, 0);
				if (revelationSkill != 0)
				{
					addSkill(SkillData.getInstance().getSkill(revelationSkill, 1), false);
				}
			}
			else if (!isSubClassActive())
			{
				int revelationSkill = getVariables().getInt(PlayerVariables.REVELATION_SKILL_1_MAIN_CLASS, 0);
				if (revelationSkill != 0)
				{
					addSkill(SkillData.getInstance().getSkill(revelationSkill, 1), false);
				}
				
				revelationSkill = getVariables().getInt(PlayerVariables.REVELATION_SKILL_2_MAIN_CLASS, 0);
				if (revelationSkill != 0)
				{
					addSkill(SkillData.getInstance().getSkill(revelationSkill, 1), false);
				}
			}
			
			// Include transformation skills.
			currentSkills.addAll(_transformSkills.values());
		}
		
		final List<Skill> finalSkills = new ArrayList<>();
		for (Skill skill : currentSkills)
		{
			if ((skill == null) || skill.isBlockActionUseSkill() || SkillTreeData.getInstance().isAlchemySkill(skill.getId(), skill.getLevel()) || !skill.isDisplayInList())
			{
				continue;
			}
			
			finalSkills.add(skill);
		}
		
		return finalSkills;
	}
	
	/**
	 * Starts the feeding process for a pet or mount.<br>
	 * This method checks if the actor is mounted and has a valid target.<br>
	 * It initializes the feed gauge and schedules a periodic task to consume food.
	 * @param npcId The unique identifier of the NPC to be fed.
	 */
	protected void startFeed(int npcId)
	{
		_canFeed = npcId > 0;
		if (!isMounted())
		{
			return;
		}
		
		if (hasPet())
		{
			setCurrentFeed(_pet.getCurrentFed());
			_controlItemId = _pet.getControlObjectId();
			final int feedConsume = getFeedConsume();
			sendPacket(new SetupGauge(3, (_curFeed * 10000) / feedConsume, (getMaxFeed() * 10000) / feedConsume));
			if (!isDead())
			{
				_mountFeedTask = ThreadPool.scheduleAtFixedRate(new PetFeedTask(this), 10000, 10000);
			}
		}
		else if (_canFeed)
		{
			setCurrentFeed(getMaxFeed());
			final int feedConsume = getFeedConsume();
			sendPacket(new SetupGauge(3, (_curFeed * 10000) / feedConsume, (getMaxFeed() * 10000) / feedConsume));
			if (!isDead())
			{
				_mountFeedTask = ThreadPool.scheduleAtFixedRate(new PetFeedTask(this), 10000, 10000);
			}
		}
	}
	
	/**
	 * Stops the active mount feed task.<br>
	 * This method cancels the {@code _mountFeedTask} if it is currently running.<br>
	 * It then sets the task reference to {@code null}.
	 */
	public void stopFeed()
	{
		if (_mountFeedTask != null)
		{
			_mountFeedTask.cancel(false);
			_mountFeedTask = null;
		}
	}
	
	/**
	 * Resets the internal data of the player.<br>
	 * This method sets the {@code _data} field to {@code null}.
	 */
	private void clearPetData()
	{
		_data = null;
	}
	
	/**
	 * Retrieves the data for a specific pet based on its unique identifier.<br>
	 * This method fetches information from the {@code PetData} instance.
	 * @param npcId The unique identification number of the NPC pet.
	 * @return The {@code PetDataHolder} associated with the provided {@code npcId}.
	 */
	public PetDataHolder getPetData(int npcId)
	{
		if (_data == null)
		{
			_data = PetData.getInstance().getPetData(npcId);
		}
		
		return _data;
	}
	
	/**
	 * Retrieves the level data for a specific pet.<br>
	 * This method fetches data from {@code PetData} if it is not already cached.
	 * @param npcId The unique identifier of the NPC.
	 * @return The {@code PetLevelData} associated with the given ID.
	 */
	private PetLevelData getPetLevelData(int npcId)
	{
		if (_leveldata == null)
		{
			_leveldata = PetData.getInstance().getPetData(npcId).getPetLevelData(getMountLevel());
		}
		
		return _leveldata;
	}
	
	/**
	 * Retrieves the current feed value.
	 * @return The current {@code int} value of the feed.
	 */
	public int getCurrentFeed()
	{
		return _curFeed;
	}
	
	/**
	 * Retrieves the current feed consumption rate for the pet.<br>
	 * It checks if the pet is currently attacking to determine the correct value.<br>
	 * If attacking, it returns the battle feed rate.<br>
	 * Otherwise, it returns the normal feed rate.
	 * @return The integer value of the required feed consumption.
	 */
	public int getFeedConsume()
	{
		// Fighting while riding uses the battle food rate.
		if (isInCombat())
		{
			return getPetLevelData(_mountNpcId).getPetFeedBattleOnRide();
		}
		
		return getPetLevelData(_mountNpcId).getPetFeedNormalOnRide();
	}
	
	/**
	 * Sets the current feed level for the actor.<br>
	 * This method updates the {@code _curFeed} value based on the provided amount.<br>
	 * It also sends a packet to update the gauge and broadcasts info if the hunger state changes.
	 * @param num The new feed amount to set.
	 */
	public void setCurrentFeed(int num)
	{
		final boolean lastHungryState = isHungry();
		final int maxFeed = getMaxFeed();
		final int feedConsume = getFeedConsume();
		_curFeed = num > maxFeed ? maxFeed : num;
		sendPacket(new SetupGauge(3, (_curFeed * 10000) / feedConsume, (maxFeed * 10000) / feedConsume));
		
		// Broadcast move speed change when strider becomes hungry / full.
		if (lastHungryState != isHungry())
		{
			broadcastUserInfo();
		}
	}
	
	/**
	 * Retrieves the maximum feed amount for the current mount.<br>
	 * This value is fetched from the pet data associated with the mount NPC ID.
	 * @return the maximum feed value as an {@code int}
	 */
	private int getMaxFeed()
	{
		return getPetLevelData(_mountNpcId).getPetMaxFeed();
	}
	
	/**
	 * Checks if the current pet is hungry.<br>
	 * This method evaluates the pet's feed level against its maximum capacity.<br>
	 * It also verifies that the pet exists and can be fed.
	 * @return {@code true} if the pet is hungry, {@code false} otherwise.
	 */
	public boolean isHungry()
	{
		return isMounted() && _canFeed && (_curFeed < ((getPetData(_mountNpcId).getHungryLimit() / 100f) * getPetLevelData(_mountNpcId).getPetMaxFeed()));
	}
	
	/**
	 * Schedules a task to dismount the player after a specific time.<br>
	 * The delay is converted from seconds to milliseconds.
	 * @param delay The number of seconds to wait before dismounting.
	 */
	public void enteredNoLanding(int delay)
	{
		_dismountTask = ThreadPool.schedule(new DismountTask(this), delay * 1000);
	}
	
	/**
	 * Cancels the active dismount task if one exists.<br>
	 * Sets the {@code _dismountTask} reference to {@code null}.
	 */
	public void exitedNoLanding()
	{
		if (_dismountTask != null)
		{
			_dismountTask.cancel(true);
			_dismountTask = null;
		}
	}
	
	/**
	 * Saves the current feeding status of a pet to the database.<br>
	 * This method updates the {@code fed} column for a specific item object ID.<br>
	 * It resets the internal {@code _controlItemId} to {@code 0} after a successful update.
	 * @param petId The unique identifier of the pet being updated.
	 */
	public void storePetFood(int petId)
	{
		if ((_controlItemId != 0) && (petId != 0))
		{
			String req;
			req = "UPDATE pets SET fed=? WHERE item_obj_id = ?";
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(req))
			{
				statement.setInt(1, _curFeed);
				statement.setInt(2, _controlItemId);
				statement.executeUpdate();
				_controlItemId = 0;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Failed to store Pet [NpcId: " + petId + "] data", e);
			}
		}
	}
	
	/**
	 * Sets whether the player is currently in a siege. <br>
	 * This updates the {@code _isInSiege} status flag.
	 * @param value The new siege status to set.
	 */
	public void setInSiege(boolean value)
	{
		_isInSiege = value;
	}
	
	/**
	 * Checks if the player is currently in a siege state.
	 * @return {@code true} if the player is in a siege, {@code false} otherwise.
	 */
	public boolean isInSiege()
	{
		return _isInSiege;
	}
	
	/**
	 * Updates the status of whether the character is currently in a hideout siege.<br>
	 * This method sets the {@code _isInHideoutSiege} flag to either {@code true} or {@code false}.
	 * @param isInHideoutSiege The new siege status to apply.
	 */
	public void setInHideoutSiege(boolean isInHideoutSiege)
	{
		_isInHideoutSiege = isInHideoutSiege;
	}
	
	/**
	 * Checks if the player is currently participating in a hideout siege.
	 * @return {@code true} if the player is in a hideout siege, {@code false} otherwise.
	 */
	public boolean isInHideoutSiege()
	{
		return _isInHideoutSiege;
	}
	
	/**
	 * Checks if the character is currently riding a flying mount.<br>
	 * This method returns {@code true} if the transformation exists and is in flight.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if the character is on a flying mount, {@code false} otherwise.
	 */
	public boolean isFlyingMounted()
	{
		final Transform transform = getTransformation();
		return (transform != null) && transform.isFlying();
	}
	
	/**
	 * Sets whether the player is currently in free look mode.<br>
	 * This determines if the camera can be rotated independently of character movement.
	 * @param value The {@code boolean} state to set for free look.
	 */
	public void setFreeLook(boolean value)
	{
		_freeLook = value;
	}
	
	/**
	 * Checks if the player is currently using free look mode.
	 * @return {@code true} if free look is active, {@code false} otherwise.
	 */
	public boolean isFreeLook()
	{
		return _freeLook;
	}
	
	/**
	 * Retrieves the current number of charges.
	 * @return the total count of {@code charges}
	 */
	public int getCharges()
	{
		return _charges.get();
	}
	
	/**
	 * Updates the current charge count for the actor.<br>
	 * This method restarts the associated charge task.<br>
	 * The new value is stored in the {@code _charges} field.
	 * @param count The new number of charges to set.
	 */
	public void setCharges(int count)
	{
		restartChargeTask();
		_charges.set(count);
	}
	
	/**
	 * Reduces the current number of charges by a specific amount.<br>
	 * This method checks if enough charges are available before proceeding.<br>
	 * It updates the charge task status and sends a status update packet.
	 * @param count The number of charges to remove.
	 * @return {@code true} if the charges were successfully decreased, {@code false} otherwise.
	 */
	public boolean decreaseCharges(int count)
	{
		if (_charges.get() < count)
		{
			return false;
		}
		
		// Charge clear task should be reset every time a charge is decreased and stopped when charges become 0.
		if (_charges.addAndGet(-count) == 0)
		{
			stopChargeTask();
		}
		else
		{
			restartChargeTask();
		}
		
		sendPacket(new EtcStatusUpdate(this));
		return true;
	}
	
	/**
	 * Resets the current charges to {@code 0}.<br>
	 * Sends an {@code EtcStatusUpdate} packet to update the client.
	 */
	public void clearCharges()
	{
		_charges.set(0);
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Cancels the existing charge task if it is currently running.<br>
	 * Schedules a new {@code ResetChargesTask} to run after 600000 milliseconds.
	 */
	private void restartChargeTask()
	{
		if (_chargeTask != null)
		{
			_chargeTask.cancel(false);
			_chargeTask = null;
		}
		
		_chargeTask = ThreadPool.schedule(new ResetChargesTask(this), 600000);
	}
	
	/**
	 * Stops the active charge task for this actor.<br>
	 * It cancels the {@code _chargeTask} if it is not {@code null}.<br>
	 * The {@code _chargeTask} reference is then set to {@code null}.
	 */
	public void stopChargeTask()
	{
		if (_chargeTask != null)
		{
			_chargeTask.cancel(false);
			_chargeTask = null;
		}
	}
	
	/**
	 * Retrieves the teleport bookmarks of this player.
	 * @return the {@code TeleportBookmarkList} of this player.
	 */
	public TeleportBookmarkList getTeleportBookmarkList()
	{
		return _teleportBookmarks;
	}
	
	/**
	 * Sends updated information about this entity to a specific {@code Player}.<br>
	 * This method updates the player's view of the entity's position and visibility.<br>
	 * It also synchronizes relationship data for both entities and their summons.<br>
	 * Finally, it sends relevant packets based on the current private store type.
	 * @param player The {@code Player} receiving the information.
	 */
	@Override
	public void sendInfo(Player player)
	{
		final boolean invisToTarget = isInvisible() && player.isGM();
		if (isInBoat())
		{
			setXYZ(getBoat().getLocation());
			player.sendPacket(new ExCharInfo(this, invisToTarget));
			player.sendPacket(new GetOnVehicle(getObjectId(), getBoat().getObjectId(), _inVehiclePosition));
		}
		else if (isInAirShip())
		{
			setXYZ(getAirShip().getLocation());
			player.sendPacket(new ExCharInfo(this, invisToTarget));
			player.sendPacket(new ExGetOnAirShip(this, getAirShip()));
		}
		else
		{
			player.sendPacket(new ExCharInfo(this, invisToTarget));
		}
		
		// Show a late arrival the player seated on the throne.
		if ((_throneId > 0) && _waitTypeSitting)
		{
			player.sendPacket(new ChairSit(this, _throneId));
		}
		
		final long relation1 = getRelation(player);
		final RelationChanged rc1 = new RelationChanged();
		final boolean insidePeace = isInsideZone(ZoneId.PEACE);
		final boolean insideNoPvp = isInsideZone(ZoneId.NO_PVP);
		rc1.addRelation(this, relation1, !insidePeace || !insideNoPvp);
		if (hasSummon())
		{
			if (_pet != null)
			{
				rc1.addRelation(_pet, relation1, !insidePeace || !insideNoPvp);
			}
			
			if (hasServitors())
			{
				getServitors().values().forEach(s -> rc1.addRelation(s, relation1, !insidePeace || !insideNoPvp));
			}
		}
		
		player.sendPacket(rc1);
		
		final long relation2 = player.getRelation(this);
		final RelationChanged rc2 = new RelationChanged();
		final boolean playerInsidePeace = player.isInsideZone(ZoneId.PEACE);
		rc2.addRelation(player, relation2, !playerInsidePeace);
		if (player.hasSummon())
		{
			if (_pet != null)
			{
				rc2.addRelation(_pet, relation2, !playerInsidePeace);
			}
			
			if (hasServitors())
			{
				getServitors().values().forEach(s -> rc2.addRelation(s, relation2, !playerInsidePeace));
			}
		}
		
		sendPacket(rc2);
		
		switch (_privateStoreType)
		{
			case SELL:
			{
				player.sendPacket(new PrivateStoreMsgSell(this));
				break;
			}
			case PACKAGE_SELL:
			{
				player.sendPacket(new ExPrivateStoreSetWholeMsg(this));
				break;
			}
			case BUY:
			{
				player.sendPacket(new PrivateStoreMsgBuy(this));
				break;
			}
			case MANUFACTURE:
			{
				player.sendPacket(new RecipeShopMsg(this));
				break;
			}
		}
		
		// Required for showing mount transformations to players that just entered the game.
		if (isTransformed())
		{
			player.sendPacket(new ExCharInfo(this, invisToTarget));
		}
	}
	
	/**
	 * Starts playing a movie for the character.<br>
	 * This method stops current movement and attacks before starting the scene.<br>
	 * It ensures that the movie only starts if one is not already active.
	 * @param holder The {@code MovieHolder} containing the movie data to be played.
	 */
	public void playMovie(MovieHolder holder)
	{
		if (_movieHolder != null)
		{
			return;
		}
		
		abortAttack();
		
		// abortCast(); Confirmed in retail, playing a movie does not abort cast.
		stopMove(null);
		setMovieHolder(holder);
		if (!isTeleporting())
		{
			sendPacket(new ExStartScenePlayer(holder.getMovie()));
		}
	}
	
	/**
	 * Stops the currently playing movie for the player.<br>
	 * This method sends a stop packet and clears the {@code movieHolder}.
	 */
	public void stopMovie()
	{
		sendPacket(new ExStopScenePlayer(_movieHolder.getMovie()));
		setMovieHolder(null);
	}
	
	/**
	 * Checks if the character is allowed to enchant skills.<br>
	 * This method verifies several conditions such as transformation status and current actions.<br>
	 * It returns {@code false} if the character is locked, transformed, casting, or in a vehicle.
	 * @return {@code true} if all conditions are met; {@code false} otherwise.
	 */
	public boolean isAllowedToEnchantSkills()
	{
		if (isSubclassLocked())
		{
			return false;
		}
		
		if (isTransformed())
		{
			return false;
		}
		
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(this))
		{
			return false;
		}
		
		if (isCastingNow())
		{
			return false;
		}
		
		if (isInBoat() || isInAirShip())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Sets the creation date for the actor.<br>
	 * This method updates the {@code _createDate} field with the provided value.
	 * @param createDate The {@code Calendar} object representing the date of creation.
	 */
	public void setCreateDate(Calendar createDate)
	{
		_createDate = createDate;
	}
	
	/**
	 * Retrieves the date when this object was created.
	 * @return the {@code Calendar} object representing the creation date.
	 */
	public Calendar getCreateDate()
	{
		return _createDate;
	}
	
	/**
	 * Checks if today is the character's birthday.<br>
	 * It handles leap year logic for characters created on February 29.<br>
	 * Returns a value based on how many days away the next birthday is.
	 * @return The number of days until the next birthday, or 0 if it is today, or -1 if no match is found.
	 */
	public int checkBirthDay()
	{
		final Calendar now = Calendar.getInstance();
		
		// "Characters with a February 29 creation date will receive a gift on February 28."
		if ((_createDate.get(Calendar.DAY_OF_MONTH) == 29) && (_createDate.get(Calendar.MONTH) == 1))
		{
			_createDate.add(Calendar.HOUR_OF_DAY, -24);
		}
		
		if ((now.get(Calendar.MONTH) == _createDate.get(Calendar.MONTH)) && (now.get(Calendar.DAY_OF_MONTH) == _createDate.get(Calendar.DAY_OF_MONTH)) && (now.get(Calendar.YEAR) != _createDate.get(Calendar.YEAR)))
		{
			return 0;
		}
		
		int i;
		for (i = 1; i < 6; i++)
		{
			now.add(Calendar.HOUR_OF_DAY, 24);
			if ((now.get(Calendar.MONTH) == _createDate.get(Calendar.MONTH)) && (now.get(Calendar.DAY_OF_MONTH) == _createDate.get(Calendar.DAY_OF_MONTH)) && (now.get(Calendar.YEAR) != _createDate.get(Calendar.YEAR)))
			{
				return i;
			}
		}
		
		return -1;
	}
	
	/**
	 * Calculates the age of the player in years.<br>
	 * This method compares the current system time with the creation date.<br>
	 * It returns the total number of full years passed since the account was created.
	 * @return The age of the player as an {@code int}.
	 */
	public int getBirthdays()
	{
		long time = (System.currentTimeMillis() - _createDate.getTimeInMillis()) / 1000;
		time /= TimeUnit.DAYS.toMillis(365);
		return (int) time;
	}
	
	/**
	 * list of character friends
	 */
	private final Friends _friends = new Friends(this);
	
	/**
	 * Retrieves the {@code Friends} of this player.
	 * @return the {@code Friends} of this player.
	 */
	public Friends getFriends()
	{
		return _friends;
	}
	
	/**
	 * Checks if the silence mode is currently active.
	 * @return {@code true} if silence mode is enabled, {@code false} otherwise.
	 */
	public boolean isSilenceMode()
	{
		return _silenceMode;
	}
	
	/**
	 * Checks if the player is currently affected by silence mode.<br>
	 * This method accounts for specific exclusions defined in the configuration.
	 * @param playerObjId The unique identifier of the player to check.
	 * @return {@code true} if the player is silenced, {@code false} otherwise.
	 */
	public boolean isSilenceMode(int playerObjId)
	{
		if (PlayerConfig.SILENCE_MODE_EXCLUDE && _silenceMode && (_silenceModeExcluded != null))
		{
			return !_silenceModeExcluded.contains(playerObjId);
		}
		
		return _silenceMode;
	}
	
	/**
	 * Enables or disables silence mode for the actor.<br>
	 * This method also clears any exceptions from the silence mode.<br>
	 * It sends a status update packet to the client.
	 * @param mode The boolean value to set for silence mode.
	 */
	public void setSilenceMode(boolean mode)
	{
		_silenceMode = mode;
		if (_silenceModeExcluded != null)
		{
			_silenceModeExcluded.clear(); // Clear the excluded list on each setSilenceMode.
		}
		
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Adds a player to the list of entities excluded from silence mode.<br>
	 * This ensures that the specified player will not be affected by silence mechanics.
	 * @param playerObjId The unique identifier of the player to exclude.
	 */
	public void addSilenceModeExcluded(int playerObjId)
	{
		if (_silenceModeExcluded == null)
		{
			_silenceModeExcluded = new ArrayList<>(1);
		}
		
		_silenceModeExcluded.add(playerObjId);
	}
	
	/**
	 * Gets the collision radius of the actor.<br>
	 * This value is used to determine the physical size for hit detection.<br>
	 * It accounts for mounts, transformations, and gender-specific templates.
	 * @return The calculated collision radius as a {@code float}.
	 */
	@Override
	public float getCollisionRadius()
	{
		if (isMounted() && (_mountNpcId > 0))
		{
			return NpcData.getInstance().getTemplate(getMountNpcId()).getFCollisionRadius();
		}
		
		final float defaultCollisionRadius = _appearance.isFemale() ? getBaseTemplate().getFCollisionRadiusFemale() : getBaseTemplate().getFCollisionRadius();
		final Transform transform = getTransformation();
		if (transform == null)
		{
			return defaultCollisionRadius;
		}
		
		return transform.getCollisionRadius(this, defaultCollisionRadius);
	}
	
	/**
	 * Retrieves the collision height of the actor.<br>
	 * This value is used to determine how high an entity occupies in the world.<br>
	 * It accounts for mounts, transformations, and gender-specific templates.
	 * @return The calculated collision height as a {@code float}.
	 */
	@Override
	public float getCollisionHeight()
	{
		if (isMounted() && (_mountNpcId > 0))
		{
			return NpcData.getInstance().getTemplate(getMountNpcId()).getFCollisionHeight();
		}
		
		final float defaultCollisionHeight = _appearance.isFemale() ? getBaseTemplate().getFCollisionHeightFemale() : getBaseTemplate().getFCollisionHeight();
		final Transform transform = getTransformation();
		if (transform == null)
		{
			return defaultCollisionHeight;
		}
		
		return transform.getCollisionHeight(this, defaultCollisionHeight);
	}
	
	/**
	 * Retrieves the current X coordinate of the client.
	 * @return the {@code int} value representing the X position.
	 */
	public int getClientX()
	{
		return _clientX;
	}
	
	/**
	 * Retrieves the current Y coordinate of the client.
	 * @return the {@code int} value representing the Y position.
	 */
	public int getClientY()
	{
		return _clientY;
	}
	
	/**
	 * Retrieves the current Z coordinate of the client.
	 * @return the {@code int} value representing the Z position.
	 */
	public int getClientZ()
	{
		return _clientZ;
	}
	
	/**
	 * Retrieves the current heading of the client.
	 * @return The integer value representing the client's heading.
	 */
	public int getClientHeading()
	{
		return _clientHeading;
	}
	
	/**
	 * Sets the client X coordinate for the player.<br>
	 * This updates the internal {@code _clientX} field.
	 * @param value The new X coordinate to set.
	 */
	public void setClientX(int value)
	{
		_clientX = value;
	}
	
	/**
	 * Sets the client Y coordinate for the player.<br>
	 * This updates the internal {@code _clientY} field.
	 * @param value The new Y coordinate to set.
	 */
	public void setClientY(int value)
	{
		_clientY = value;
	}
	
	/**
	 * Sets the Z coordinate of the client.<br>
	 * This updates the vertical position in the game world.
	 * @param value The new {@code int} value for the Z coordinate.
	 */
	public void setClientZ(int value)
	{
		_clientZ = value;
	}
	
	/**
	 * Sets the current heading for the client.<br>
	 * This updates the {@code _clientHeading} field with a new value.
	 * @param value The new heading value to set.
	 */
	public void setClientHeading(int value)
	{
		_clientHeading = value;
	}
	
	/**
	 * Checks if the actor is currently in a falling state.<br>
	 * This method evaluates the current height and environment to determine if fall damage should be applied.<br>
	 * It returns {@code true} if the actor is actively falling based on the internal timestamp.
	 * @param z The target Z coordinate to compare against the current position.
	 * @return {@code true} if the actor is currently falling, {@code false} otherwise.
	 */
	public boolean isFalling(int z)
	{
		if (isDead() || isFlying() || isFlyingMounted() || isInsideZone(ZoneId.WATER))
		{
			return false;
		}
		
		if ((_fallingTimestamp != 0) && (System.currentTimeMillis() < _fallingTimestamp))
		{
			return true;
		}
		
		final int deltaZ = getZ() - z;
		if (deltaZ <= getBaseTemplate().getSafeFallHeight())
		{
			_fallingTimestamp = 0;
			return false;
		}
		
		// If there is no geodata loaded for the place we are, client Z correction might cause falling damage.
		if (!GeoEngine.getInstance().hasGeo(getX(), getY()))
		{
			_fallingTimestamp = 0;
			return false;
		}
		
		if (_fallingDamage == 0)
		{
			_fallingDamage = (int) Formulas.calcFallDam(this, deltaZ);
		}
		
		if (_fallingDamageTask != null)
		{
			_fallingDamageTask.cancel(true);
		}
		
		_fallingDamageTask = ThreadPool.schedule(() ->
		{
			if ((_fallingDamage > 0) && !isInvul())
			{
				reduceCurrentHp(Math.min(_fallingDamage, getCurrentHp() - 1), this, null, false, true, false, false);
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_VE_RECEIVED_S1_DAMAGE_FROM_FALLING);
				sm.addInt(_fallingDamage);
				sendPacket(sm);
			}
			
			_fallingDamage = 0;
			_fallingDamageTask = null;
		}, 1500);
		
		// Prevent falling under ground.
		sendPacket(new ValidateLocation(this));
		setFalling();
		
		return false;
	}
	
	/**
	 * Sets the timestamp for when the actor is considered to be falling.<br>
	 * This method updates {@code _fallingTimestamp} using the current system time.<br>
	 * It adds a delay defined by {@code FALLING_VALIDATION_DELAY}.
	 */
	public void setFalling()
	{
		_fallingTimestamp = System.currentTimeMillis() + FALLING_VALIDATION_DELAY;
	}
	
	/**
	 * Retrieves the current movie holder instance.
	 * @return the {@code MovieHolder} object.
	 */
	public MovieHolder getMovieHolder()
	{
		return _movieHolder;
	}
	
	/**
	 * Sets the {@code MovieHolder} for this actor.<br>
	 * This method updates the internal reference to a {@code MovieHolder} object.
	 * @param movie The {@code MovieHolder} to be assigned.
	 */
	public void setMovieHolder(MovieHolder movie)
	{
		_movieHolder = movie;
	}
	
	/**
	 * Updates the timestamp for the last item auction request.<br>
	 * This method sets {@code _lastItemAuctionInfoRequest} to the current system time.
	 */
	public void updateLastItemAuctionRequest()
	{
		_lastItemAuctionInfoRequest = System.currentTimeMillis();
	}
	
	/**
	 * Checks if the item auction polling is currently active.<br>
	 * It returns {@code true} if less than 2000 milliseconds have passed since the last request.<br>
	 * This prevents the system from requesting auction data too frequently.
	 * @return {@code true} if the poll is still active, {@code false} otherwise.
	 */
	public boolean isItemAuctionPolling()
	{
		return (System.currentTimeMillis() - _lastItemAuctionInfoRequest) < 2000;
	}
	
	/**
	 * Checks if the character is currently unable to move.<br>
	 * This returns {@code true} if movement is restricted by a status, a movie, or fishing.
	 * @return {@code true} if movement is disabled, {@code false} otherwise.
	 */
	@Override
	public boolean isMovementDisabled()
	{
		return super.isMovementDisabled() || (_movieHolder != null) || _fishing.isFishing();
	}
	
	/**
	 * Retrieves the HTML prefix for the current actor.<br>
	 * This method returns an empty {@code String} if multilingual support is disabled.
	 * @return The HTML prefix string.
	 */
	public String getHtmlPrefix()
	{
		if (!MultilingualSupportConfig.MULTILANG_ENABLE)
		{
			return "";
		}
		
		return _htmlPrefix;
	}
	
	/**
	 * Retrieves the current language of the actor.
	 * @return The language code as a {@code String}.
	 */
	public String getLang()
	{
		return _lang;
	}
	
	/**
	 * Sets the language for the actor.<br>
	 * This method checks if multilingual support is enabled and allowed.<br>
	 * It updates the internal language and HTML prefix accordingly.
	 * @param lang The language code to set.
	 * @return {@code true} if the requested language was successfully set, otherwise {@code false}.
	 */
	public boolean setLang(String lang)
	{
		boolean result = false;
		if (MultilingualSupportConfig.MULTILANG_ENABLE)
		{
			if (MultilingualSupportConfig.MULTILANG_ALLOWED.contains(lang))
			{
				_lang = lang;
				result = true;
			}
			else
			{
				_lang = MultilingualSupportConfig.MULTILANG_DEFAULT;
			}
			
			_htmlPrefix = _lang.equals("en") ? "" : "data/lang/" + _lang + "/";
		}
		else
		{
			_lang = null;
			_htmlPrefix = "";
		}
		
		return result;
	}
	
	/**
	 * Retrieves the current amount of PC Cafe points.
	 * @return The number of {@code pcCafePoints} owned by the player.
	 */
	public int getPcCafePoints()
	{
		return _pcCafePoints;
	}
	
	/**
	 * Updates the current PC Cafe points for the player.<br>
	 * This method ensures the value does not exceed {@code PremiumSystemConfig.PC_CAFE_MAX_POINTS}.
	 * @param count The number of points to set.
	 */
	public void setPcCafePoints(int count)
	{
		_pcCafePoints = count < PremiumSystemConfig.PC_CAFE_MAX_POINTS ? count : PremiumSystemConfig.PC_CAFE_MAX_POINTS;
	}
	
	/**
	 * Retrieves the current amount of honor coins for the player.<br>
	 * It looks up the value associated with the key {@code HONOR_COINS}.<br>
	 * If no value is found, it returns {@code 0}.
	 * @return The number of honor coins as a {@code long}.
	 */
	public long getHonorCoins()
	{
		return getVariables().getLong("HONOR_COINS", 0);
	}
	
	/**
	 * Sets the number of honor coins for the player.<br>
	 * This method updates the {@code HONOR_COINS} variable.<br>
	 * It also sends a packet to update the client information.
	 * @param value The new amount of honor coins to set.
	 */
	public void setHonorCoins(long value)
	{
		getVariables().set("HONOR_COINS", value);
		sendPacket(new ExPledgeCoinInfo(this));
	}
	
	/**
	 * Validates the current skill levels of the player.<br>
	 * This method checks if any skills exceed the allowed limits defined in {@code SkillTreeData}.<br>
	 * If a skill is too high, it calls {@code deacreaseSkillLevel} to lower it.
	 */
	public void checkPlayerSkills()
	{
		SkillLearn learn;
		for (Entry<Integer, Skill> e : getSkills().entrySet())
		{
			final Integer skillKey = e.getKey();
			final Skill skillValue = e.getValue();
			learn = SkillTreeData.getInstance().getClassSkill(skillKey, skillValue.getLevel() % 100, getPlayerClass());
			if (learn != null)
			{
				final int levelDiff = skillKey == CommonSkill.EXPERTISE.getId() ? 0 : 9;
				if (getLevel() < (learn.getGetLevel() - levelDiff))
				{
					deacreaseSkillLevel(skillValue, levelDiff);
				}
			}
		}
	}
	
	/**
	 * Reduces the level of a specific skill.<br>
	 * This method finds the next lowest valid skill level based on the player's current level.<br>
	 * If no lower level is available, the skill is removed from the character.
	 * @param skill The {@code Skill} object to be modified.
	 * @param levelDiff The amount of levels to subtract from the requirement check.
	 */
	private void deacreaseSkillLevel(Skill skill, int levelDiff)
	{
		int nextLevel = -1;
		final Map<Long, SkillLearn> skillTree = SkillTreeData.getInstance().getCompleteClassSkillTree(getPlayerClass());
		for (SkillLearn sl : skillTree.values())
		{
			final int slSkillLevel = sl.getSkillLevel();
			if ((sl.getSkillId() == skill.getId()) && (nextLevel < slSkillLevel) && (getLevel() >= (sl.getGetLevel() - levelDiff)))
			{
				nextLevel = slSkillLevel; // next possible skill level
			}
		}
		
		if (nextLevel == -1)
		{
			LOGGER.info("Removing skill " + skill + " from " + this);
			removeSkill(skill, true); // there is no lower skill
		}
		else
		{
			LOGGER.info("Decreasing skill " + skill + " to " + nextLevel + " for " + this);
			addSkill(SkillData.getInstance().getSkill(skill.getId(), nextLevel), true); // replace with lower one
		}
	}
	
	/**
	 * Checks if the actor is currently able to perform a social action.<br>
	 * This method verifies that the actor is not busy, alive, or restricted by status effects.<br>
	 * It also ensures the actor's AI intention is set to {@code IDLE}.
	 * @return {@code true} if all conditions for a social action are met; {@code false} otherwise.
	 */
	public boolean canMakeSocialAction()
	{
		return ((_privateStoreType == PrivateStoreType.NONE) && (getActiveRequester() == null) && !isAlikeDead() && !isAllSkillsDisabled() && !isCastingNow() && (getAI().getIntention() == Intention.IDLE));
	}
	
	/**
	 * Sets the multi-social action and its corresponding target.<br>
	 * This method updates the internal state for social interactions.
	 * @param id The unique identifier for the social action.
	 * @param targetId The unique identifier for the target of the action.
	 */
	public void setMultiSocialAction(int id, int targetId)
	{
		_multiSociaAction = id;
		_multiSocialTarget = targetId;
	}
	
	/**
	 * Retrieves the current multi-social action value.
	 * @return The integer value of {@code _multiSociaAction}.
	 */
	public int getMultiSociaAction()
	{
		return _multiSociaAction;
	}
	
	/**
	 * Retrieves the current multi-social target value.
	 * @return The integer value of {@code _multiSocialTarget}.
	 */
	public int getMultiSocialTarget()
	{
		return _multiSocialTarget;
	}
	
	/**
	 * Retrieves the maximum number of quest items allowed in an inventory.<br>
	 * This value is pulled from the {@code PlayerConfig} class.
	 * @return the maximum limit for quest items as an {@code int}.
	 */
	public int getQuestInventoryLimit()
	{
		return PlayerConfig.INVENTORY_MAXIMUM_QUEST_ITEMS;
	}
	
	/**
	 * Retrieves the capacity of the dedicated Artifact inventory tab.<br>
	 * This value is pulled from the {@code PlayerConfig} class.
	 * @return the maximum limit for Artifacts as an {@code int}.
	 */
	public int getArtifactInventoryLimit()
	{
		return PlayerConfig.INVENTORY_MAXIMUM_ARTIFACT_ITEMS;
	}
	
	/**
	 * Checks if the current actor is allowed to attack a specific {@code Creature}.<br>
	 * This method evaluates various conditions such as target attackability, zone types, and party status.<br>
	 * It also considers clan relations, duel states, and PvP flags to determine eligibility.
	 * @param creature The {@code Creature} that is being checked for a valid attack.
	 * @return {@code true} if the attack is permitted, otherwise {@code false}.
	 */
	public boolean canAttackCreature(Creature creature)
	{
		if (creature.isAttackable())
		{
			return true;
		}
		else if (creature.isPlayable())
		{
			if (creature.isInsideZone(ZoneId.PVP) && !creature.isInsideZone(ZoneId.SIEGE))
			{
				return true;
			}
			
			final Player target = creature.isSummon() ? creature.asSummon().getOwner() : creature.asPlayer();
			if (isInDuel() && target.isInDuel() && (target.getDuelId() == getDuelId()))
			{
				return true;
			}
			else if (isInParty() && target.isInParty())
			{
				final Party selfParty = getParty();
				final Party targetParty = target.getParty();
				if (selfParty == targetParty)
				{
					return false;
				}
				
				final CommandChannel selfChannel = selfParty.getCommandChannel();
				final CommandChannel targetChannel = targetParty.getCommandChannel();
				if (((selfChannel != null) || (targetChannel != null)) && (selfChannel == targetChannel))
				{
					return false;
				}
			}
			else if ((getClan() != null) && (target.getClan() != null))
			{
				final int clanId = getClanId();
				final Clan targetClan = target.getClan();
				final int targetClanId = target.getClanId();
				if (clanId == targetClanId)
				{
					return false;
				}
				
				final int myAllyId = getAllyId();
				final int targetAllyId = target.getAllyId();
				if (((myAllyId > 0) || (targetAllyId > 0)) && (myAllyId == targetAllyId))
				{
					return false;
				}
				
				if (getClan().isAtWarWith(targetClanId) && targetClan.isAtWarWith(clanId))
				{
					return true;
				}
			}
			else if ((getClan() == null) || (target.getClan() == null))
			{
				if ((target.getPvpFlag() == 0) && (target.getReputation() >= 0))
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if the inventory is nearly full.<br>
	 * It returns {@code true} if the current size is less than or equal to 90% of the limit.
	 * @param includeQuestInv Determines whether to count quest items in the total size.
	 * @return {@code true} if the inventory is under 90% capacity, {@code false} otherwise.
	 */
	public boolean isInventoryUnder90(boolean includeQuestInv)
	{
		return (includeQuestInv ? _inventory.getSize() : _inventory.getNonQuestSize()) <= (getInventoryLimit() * 0.9);
	}
	
	/**
	 * Checks if the current inventory is less than 80% full.<br>
	 * This method calculates the capacity based on the {@code includeQuestInv} flag.
	 * @param includeQuestInv Determines whether to count quest items in the size calculation.
	 * @return {@code true} if the inventory is under 80% capacity, {@code false} otherwise.
	 */
	public boolean isInventoryUnder80(boolean includeQuestInv)
	{
		return (includeQuestInv ? _inventory.getSize() : _inventory.getNonQuestSize()) <= (getInventoryLimit() * 0.8);
	}
	
	/**
	 * Checks if the actor has any pet items.
	 * @return {@code true} if there are pet items, {@code false} otherwise.
	 */
	public boolean havePetInvItems()
	{
		return _petItems;
	}
	
	/**
	 * Updates the pet inventory status.<br>
	 * Sets whether the pet currently has items.
	 * @param haveit The new status to set. Use {@code true} if they have items and {@code false} otherwise.
	 */
	public void setPetInvItems(boolean haveit)
	{
		_petItems = haveit;
	}
	
	/**
	 * Checks the database to see if the pet has any items.<br>
	 * Updates the internal state of the pet inventory.<br>
	 * Logs an error if the database query fails.
	 */
	private void restorePetInventoryItems()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT object_id FROM `items` WHERE `owner_id`=? AND (`loc`='PET' OR `loc`='PET_EQUIP') LIMIT 1;"))
		{
			statement.setInt(1, getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				setPetInvItems(rset.next() && (rset.getInt("object_id") > 0));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not check Items in Pet Inventory for playerId: " + getObjectId(), e);
		}
	}
	
	/**
	 * Retrieves the command used for admin confirmation.<br>
	 * This value is stored in the {@code _adminConfirmCmd} field.
	 * @return The string representing the admin confirmation command.
	 */
	public String getAdminConfirmCmd()
	{
		return _adminConfirmCmd;
	}
	
	/**
	 * Sets the command required for administrator confirmation.<br>
	 * This value is used to verify actions performed by administrators.
	 * @param adminConfirmCmd The {@code String} representing the confirmation command.
	 */
	public void setAdminConfirmCmd(String adminConfirmCmd)
	{
		_adminConfirmCmd = adminConfirmCmd;
	}
	
	/**
	 * Updates the premium status of the player.<br>
	 * Sets the {@code _premiumStatus} field to the provided value.
	 * @param premiumStatus The new premium status to set. Use {@code true} for premium or {@code false} for non-premium.
	 */
	public void setPremiumStatus(boolean premiumStatus)
	{
		_premiumStatus = premiumStatus;
		sendPacket(new ExBrPremiumState(this));
	}
	
	/**
	 * Checks if the player currently has an active premium status.<br>
	 * This depends on both the global configuration and the individual player state.
	 * @return {@code true} if the premium system is enabled and the player is a premium member, {@code false} otherwise.
	 */
	public boolean hasPremiumStatus()
	{
		return PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && _premiumStatus;
	}
	
	/**
	 * Sets the name of the last Game Master who handled a petition.<br>
	 * This value is stored in the {@code _lastPetitionGmName} field.
	 * @param gmName The name of the Game Master to store.
	 */
	public void setLastPetitionGmName(String gmName)
	{
		_lastPetitionGmName = gmName;
	}
	
	/**
	 * Retrieves the name of the last GM who handled a petition.<br>
	 * This value is stored in the {@code _lastPetitionGmName} field.
	 * @return The name of the last petition GM as a {@code String}.
	 */
	public String getLastPetitionGmName()
	{
		return _lastPetitionGmName;
	}
	
	/**
	 * Retrieves the current list of contacts.
	 * @return a {@code ContactList} containing all contacts.
	 */
	public ContactList getContactList()
	{
		return _contactList;
	}
	
	/**
	 * Returns the timestamp when the actor is allowed to move again.<br>
	 * This value represents a point in time used for movement restrictions.
	 * @return The {@code long} timestamp of the next allowed movement.
	 */
	public long getNotMoveUntil()
	{
		return _notMoveUntil;
	}
	
	/**
	 * Updates the timestamp for when movement is restricted.<br>
	 * This method sets {@code _notMoveUntil} to the current time plus a configured delay.<br>
	 * It uses the value defined in {@code PlayerConfig.PLAYER_MOVEMENT_BLOCK_TIME}.
	 */
	public void updateNotMoveUntil()
	{
		_notMoveUntil = System.currentTimeMillis() + PlayerConfig.PLAYER_MOVEMENT_BLOCK_TIME;
	}
	
	/**
	 * Checks if the current actor is a player.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the entity is a player.
	 */
	@Override
	public boolean isPlayer()
	{
		return true;
	}
	
	/**
	 * Converts the current actor into a {@code Player} object.<br>
	 * This method returns the current instance cast as a {@code Player}.
	 * @return the current {@code Player} instance.
	 */
	@Override
	public Player asPlayer()
	{
		return this;
	}
	
	/**
	 * Retrieves a specific custom skill from the internal collection.<br>
	 * This method looks up the skill using the provided unique identifier.
	 * @param skillId The unique ID of the custom skill to retrieve.
	 * @return The {@code Skill} object if found, or {@code null} if it does not exist.
	 */
	public Skill getCustomSkill(int skillId)
	{
		return (_customSkills != null) ? _customSkills.get(skillId) : null;
	}
	
	/**
	 * Adds a unique custom skill to the internal collection.<br>
	 * This method checks if the {@code Skill} object is not {@code null}.<br>
	 * It also ensures the display ID does not match the base ID.<br>
	 * The skill is stored in a {@code ConcurrentSkipListMap} using its display ID as the key.
	 * @param skill The {@code Skill} object to be added.
	 */
	private void addCustomSkill(Skill skill)
	{
		if ((skill != null) && (skill.getDisplayId() != skill.getId()))
		{
			if (_customSkills == null)
			{
				_customSkills = new ConcurrentSkipListMap<>();
			}
			
			_customSkills.put(skill.getDisplayId(), skill);
		}
	}
	
	/**
	 * Removes a specific custom skill from the internal collection.<br>
	 * This method checks if the {@code Skill} object is not {@code null}.<br>
	 * It also verifies that the skill has a different display ID than its unique ID.<br>
	 * If these conditions are met, it removes the skill using its display ID.
	 * @param skill The {@code Skill} object to be removed.
	 */
	private void removeCustomSkill(Skill skill)
	{
		if ((skill != null) && (_customSkills != null) && (skill.getDisplayId() != skill.getId()))
		{
			_customSkills.remove(skill.getDisplayId());
		}
	}
	
	/**
	 * Registers a replacement for an existing skill.<br>
	 * The skill stays known, only what it resolves to changes, and only while the rule lives.
	 * @param originalId The ID of the skill to be replaced.
	 * @param replacementId The ID of the new skill that replaces the original one.
	 */
	public void addReplacedSkill(int originalId, int replacementId)
	{
		getSkillReplacementList().registerReplacement(originalId, replacementId, -1, SkillReplacementList.DEFAULT_PRIORITY);
	}
	
	/**
	 * Removes every replacement registered for one skill.<br>
	 * What the skill resolved to is simply no longer reachable.
	 * @param originalId The unique identifier of the skill to be restored.
	 */
	public void removeReplacedSkill(int originalId)
	{
		final SkillReplacementList manager = _skillReplacementList;
		if (manager != null)
		{
			manager.unregisterReplacements(originalId);
		}
	}
	
	/**
	 * Finds the final skill ID after all replacements are applied.<br>
	 * This method follows the chain of IDs until it finds a terminal value.
	 * @param originalId The starting skill ID to check for replacements.
	 * @return The final replaced skill ID.
	 */
	public int getReplacementSkill(int originalId)
	{
		final SkillReplacementList manager = _skillReplacementList;
		return manager == null ? originalId : manager.resolveSkillId(originalId);
	}
	
	/**
	 * Retrieves the base skill ID from a chain of replacements.<br>
	 * This method follows the replacement path until it finds the final original ID.
	 * @param replacementId The starting skill ID to trace back from.
	 * @return The final original skill ID.
	 */
	public int getOriginalSkill(int replacementId)
	{
		final SkillReplacementList manager = _skillReplacementList;
		return manager == null ? replacementId : manager.resolveOriginalSkillId(replacementId);
	}
	
	/**
	 * Retrieves the set of skills that have been replaced.
	 * @return a {@code Collection<Integer>} containing the IDs of the replaced skills.
	 */
	public Collection<Integer> getReplacedSkills()
	{
		final SkillReplacementList manager = _skillReplacementList;
		return manager == null ? List.of() : manager.getReplacedSkills();
	}
	
	/**
	 * Retrieves the skill replacement manager, creating it on first use.
	 * @return The {@code SkillReplacementList} of this player.
	 */
	public SkillReplacementList getSkillReplacementList()
	{
		SkillReplacementList manager = _skillReplacementList;
		if (manager == null)
		{
			synchronized (this)
			{
				manager = _skillReplacementList;
				if (manager == null)
				{
					manager = new SkillReplacementList(this);
					_skillReplacementList = manager;
				}
			}
		}
		
		return manager;
	}
	
	/**
	 * Retrieves the skill replacement manager without creating one.
	 * @return The {@code SkillReplacementList} of this player, or {@code null} when it has none yet.
	 */
	public SkillReplacementList getSkillReplacementListIfPresent()
	{
		return _skillReplacementList;
	}
	
	/**
	 * Retrieves the history of olympiad fights for this player.
	 * @return the {@code OlympiadFightHistory} object.
	 */
	public OlympiadFightHistory getOlympiadFightHistory()
	{
		return _olympiadFightHistory;
	}
	
	/**
	 * Checks if the character is allowed to be revived.
	 * @return {@code true} if revival is possible, {@code false} otherwise.
	 */
	@Override
	public boolean canRevive()
	{
		return _canRevive;
	}
	
	/**
	 * Sets whether the actor can be revived.<br>
	 * Use {@code true} to allow revival and {@code false} to prevent it.
	 * @param value The revive status to set.
	 */
	@Override
	public void setCanRevive(boolean value)
	{
		_canRevive = value;
	}
	
	/**
	 * Checks if the actor is currently involved in an event.<br>
	 * This returns {@code true} if the actor is registered for an event or is actively participating in one.
	 * @return {@code true} if the actor is registered or on an event, {@code false} otherwise.
	 */
	public boolean isRegisteredOnEvent()
	{
		return _isRegisteredOnEvent || _isOnEvent;
	}
	
	/**
	 * Sets whether the actor is registered to receive events.<br>
	 * This updates the internal {@code _isRegisteredOnEvent} flag.
	 * @param value The registration status to set. Use {@code true} to register or {@code false} to unregister.
	 */
	public void setRegisteredOnEvent(boolean value)
	{
		_isRegisteredOnEvent = value;
	}
	
	/**
	 * Checks if the current actor is currently participating in an event.
	 * @return {@code true} if the actor is on an event, {@code false} otherwise.
	 */
	@Override
	public boolean isOnEvent()
	{
		return _isOnEvent;
	}
	
	/**
	 * Sets whether the actor is currently processing an event.<br>
	 * Use {@code true} to enable event processing.<br>
	 * Use {@code false} to disable event processing.
	 * @param value The boolean state to set for event processing.
	 */
	public void setOnEvent(boolean value)
	{
		_isOnEvent = value;
	}
	
	/**
	 * Checks if the solo event is currently active.
	 * @return {@code true} if the solo event is enabled, {@code false} otherwise.
	 */
	public boolean isOnSoloEvent()
	{
		return _isOnSoloEvent;
	}
	
	/**
	 * Sets whether the solo event is currently active.<br>
	 * This updates the {@code _isOnSoloEvent} flag for the actor.
	 * @param value The boolean state to set for the solo event.
	 */
	public void setOnSoloEvent(boolean value)
	{
		_isOnSoloEvent = value;
	}
	
	/**
	 * Checks if the player is currently exempt from death penalties.<br>
	 * This returns {@code true} if a specific event is active or if the player has a protection effect.
	 * @return {@code true} if the player is blocked from death penalty, {@code false} otherwise.
	 */
	public boolean isBlockedFromDeathPenalty()
	{
		return _isOnEvent || isAffected(EffectFlag.PROTECT_DEATH_PENALTY);
	}
	
	/**
	 * Sets the specific arena for the block checker event.<br>
	 * This value determines which area is used for checking blocks.
	 * @param arena The {@code byte} value representing the target arena.
	 */
	public void setBlockCheckerArena(byte arena)
	{
		_handysBlockCheckerEventArena = arena;
	}
	
	/**
	 * Retrieves the arena identifier for the handy block checker event.
	 * @return The {@code int} value of the event arena.
	 */
	public int getBlockCheckerArena()
	{
		return _handysBlockCheckerEventArena;
	}
	
	/**
	 * Retrieves the current {@code KrateiArena} instance.
	 * @return The {@code KrateiArena} object.
	 */
	public KrateiArena getKrateiArena()
	{
		return _krateiArena;
	}
	
	/**
	 * Sets the current {@code KrateiArena} for this actor.<br>
	 * This method updates the internal arena reference.
	 * @param arena The {@code KrateiArena} to set.
	 */
	public void setKrateiArena(KrateiArena arena)
	{
		_krateiArena = arena;
	}
	
	/**
	 * Retrieves the total number of UCK kills.
	 * @return The current count of {@code _ucKills}.
	 */
	public int getUCKills()
	{
		return _ucKills;
	}
	
	/**
	 * Increments the unique kill count for the player.<br>
	 * This method updates the {@code _ucKills} counter.
	 */
	public void addKillCountUC()
	{
		_ucKills++;
	}
	
	/**
	 * Retrieves the total number of deaths for the user.
	 * @return The current death count as an {@code int}.
	 */
	public int getUCDeaths()
	{
		return _ucDeaths;
	}
	
	/**
	 * Increments the total death count for this character.<br>
	 * This method updates the {@code _ucDeaths} counter.
	 */
	public void addDeathCountUC()
	{
		_ucDeaths++;
	}
	
	/**
	 * Resets the user combat statistics to their default values.<br>
	 * This method sets both {@code _ucDeaths} and {@code _ucKills} to {@code 0}.
	 */
	public void cleanUCStats()
	{
		_ucDeaths = 0;
		_ucKills = 0;
	}
	
	/**
	 * Updates the current {@code UCState} for the player.<br>
	 * This method also updates the registration status based on whether the state is not {@code UC_STATE_NONE}.
	 * @param state The new state to set for the player.
	 */
	public void setUCState(int state)
	{
		setRegisteredOnEvent(state != UC_STATE_NONE);
		_ucState = state;
	}
	
	/**
	 * Retrieves the current state of the user character.
	 * @return The integer value representing the {@code _ucState}.
	 */
	public int getUCState()
	{
		return _ucState;
	}
	
	/**
	 * Sets the original character stats for the player.<br>
	 * These values are used as a baseline for calculations.
	 * @param cp The original character power value.
	 * @param hp The original health points value.
	 * @param mp The original mana points value.
	 */
	public void setOriginalCpHpMp(double cp, double hp, double mp)
	{
		_originalCp = cp;
		_originalHp = hp;
		_originalMp = mp;
	}
	
	/**
	 * Checks if the player has any variables assigned.<br>
	 * It returns {@code true} if the variable map is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if variables exist, {@code false} otherwise.
	 */
	public boolean hasVariables()
	{
		return getScript(PlayerVariables.class) != null;
	}
	
	/**
	 * Retrieves the variables associated with this player.<br>
	 * If no variables exist, a new {@code PlayerVariables} object is created and added.
	 * @return the {@code PlayerVariables} instance for this player.
	 */
	public PlayerVariables getVariables()
	{
		final PlayerVariables vars = getScript(PlayerVariables.class);
		return vars != null ? vars : addScript(new PlayerVariables(getObjectId()));
	}
	
	/**
	 * Checks if the account has any variables assigned.<br>
	 * This method retrieves the variable map from {@code AccountVariables}.<br>
	 * It returns {@code true} if the map is not {@code null}.
	 * @return {@code true} if variables exist, {@code false} otherwise.
	 */
	public boolean hasAccountVariables()
	{
		return getScript(AccountVariables.class) != null;
	}
	
	/**
	 * Retrieves the variables associated with the account.<br>
	 * It returns the existing {@code AccountVariables} if found.<br>
	 * If none exist, it creates and adds a new one using the current account name.
	 * @return The {@code AccountVariables} object for this account.
	 */
	public AccountVariables getAccountVariables()
	{
		final AccountVariables vars = getScript(AccountVariables.class);
		return vars != null ? vars : addScript(new AccountVariables(getAccountName()));
	}
	
	/**
	 * Retrieves the unique identifier for the player's class.<br>
	 * This method calls {@code getId()} on the current {@code PlayerClass}.
	 * @return the integer ID of the player's class.
	 */
	@Override
	public int getId()
	{
		return getPlayerClass().getId();
	}
	
	/**
	 * Checks if the current character is under a party ban.<br>
	 * This method queries the {@code PunishmentManager} for active restrictions.
	 * @return {@code true} if the character is banned from parties, {@code false} otherwise.
	 */
	public boolean isPartyBanned()
	{
		return PunishmentManager.getInstance().hasPunishment(getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.PARTY_BAN);
	}
	
	/**
	 * Adds a new action to the player.<br>
	 * This method updates the internal action mask if the action is not already present.
	 * @param act The {@code PlayerAction} to add.
	 * @return {@code true} if the action was successfully added, or {@code false} if it already existed.
	 */
	public boolean addAction(PlayerAction act)
	{
		if (!hasAction(act))
		{
			_actionMask |= act.getMask();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Removes a specific action from the player.<br>
	 * This method updates the internal {@code _actionMask}.
	 * @param act The {@code PlayerAction} to be removed.
	 * @return {@code true} if the action was successfully removed, or {@code false} if it did not exist.
	 */
	public boolean removeAction(PlayerAction act)
	{
		if (hasAction(act))
		{
			_actionMask &= ~act.getMask();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if the player has a specific action allowed.<br>
	 * It compares the {@code PlayerAction} mask against the current action mask.
	 * @param act The {@code PlayerAction} to check.
	 * @return {@code true} if all actions in the mask are allowed, {@code false} otherwise.
	 */
	public boolean hasAction(PlayerAction act)
	{
		final long mask = act.getMask();
		return (_actionMask & mask) == mask;
	}
	
	/**
	 * Sets whether the character possesses the Charm of Courage.<br>
	 * This updates the {@code _hasCharmOfCourage} flag.
	 * @param value The boolean state to set for the Charm of Courage.
	 */
	public void setCharmOfCourage(boolean value)
	{
		_hasCharmOfCourage = value;
		if (!value && (_charmOfCourageTask != null))
		{
			_charmOfCourageTask.cancel(false);
			_charmOfCourageTask = null;
		}
	}
	
	/**
	 * Starts the Charm of Courage state for a limited time.<br>
	 * When the time runs out the state ends and the client status icon is updated.
	 * @param time The duration in milliseconds.
	 */
	public void startCharmOfCourage(long time)
	{
		setCharmOfCourage(false);
		_hasCharmOfCourage = true;
		_charmOfCourageTask = ThreadPool.schedule(() ->
		{
			_charmOfCourageTask = null;
			_hasCharmOfCourage = false;
			sendPacket(new EtcStatusUpdate(this));
		}, time);
	}
	
	/**
	 * Checks if the character possesses the Charm of Courage.
	 * @return {@code true} if the charm is active, {@code false} otherwise.
	 */
	public boolean hasCharmOfCourage()
	{
		return _hasCharmOfCourage;
	}
	
	/**
	 * Checks if the current state is valid.<br>
	 * Returns {@code true} if the status is good.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean value representing the validity of the state.
	 */
	public boolean isGood()
	{
		return _isGood;
	}
	
	/**
	 * Checks if the actor is classified as evil.
	 * @return {@code true} if the actor is evil, {@code false} otherwise.
	 */
	public boolean isEvil()
	{
		return _isEvil;
	}
	
	/**
	 * Sets the actor's alignment to good.<br>
	 * This method sets {@code _isGood} to {@code true}.<br>
	 * It also sets {@code _isEvil} to {@code false}.
	 */
	public void setGood()
	{
		_isGood = true;
		_isEvil = false;
	}
	
	/**
	 * Sets the actor's alignment to evil.<br>
	 * This method updates the {@code _isGood} and {@code _isEvil} flags.
	 */
	public void setEvil()
	{
		_isGood = false;
		_isEvil = true;
	}
	
	/**
	 * Checks if the current actor is at war with a specific target.<br>
	 * This method verifies clan relations between two non-academy members.<br>
	 * It returns {@code false} if either player is not in a clan or is an academy member.
	 * @param target The {@code Playable} entity to check against.
	 * @return {@code true} if the clans are at war, {@code false} otherwise.
	 */
	public boolean atWarWith(Playable target)
	{
		if (target == null)
		{
			return false;
		}
		
		if ((_clan != null) && !isAcademyMember()) // Current player
		{
			final Clan targetClan = target.getClan();
			if ((targetClan != null) && !target.isAcademyMember()) // Target player
			{
				return _clan.isAtWarWith(targetClan);
			}
		}
		
		return false;
	}
	
	/**
	 * Updates the visual hair identifier for the character.<br>
	 * This method stores the provided {@code hairId} in the character variables.
	 * @param hairId The unique identifier of the hair to display.
	 */
	public void setVisualHair(int hairId)
	{
		getVariables().set("visualHairId", hairId);
	}
	
	/**
	 * Sets the visual hair color for the character.<br>
	 * This updates the {@code visualHairColorId} variable in the actor's variables map.
	 * @param colorId The unique identifier for the desired hair color.
	 */
	public void setVisualHairColor(int colorId)
	{
		getVariables().set("visualHairColorId", colorId);
	}
	
	/**
	 * Updates the visual face identifier for the character.<br>
	 * This value is stored in the character's variables.
	 * @param faceId The unique ID of the face to display.
	 */
	public void setVisualFace(int faceId)
	{
		getVariables().set("visualFaceId", faceId);
	}
	
	/**
	 * Retrieves the unique identifier for the character's visual hair.<br>
	 * It checks if the player belongs to a specific class category first.<br>
	 * If so, it returns the style from {@code PlayerAppearance}.<br>
	 * Otherwise, it looks for a custom value in the variables map or defaults to the appearance style.
	 * @return The integer ID of the current hair style.
	 */
	public int getVisualHair()
	{
		if (CategoryData.getInstance().isInCategory(CategoryType.DEATH_KNIGHT_ALL_CLASS, getPlayerClass().getId()))
		{
			return _appearance.getHairStyle();
		}
		
		return getVariables().getInt("visualHairId", _appearance.getHairStyle());
	}
	
	/**
	 * Retrieves the visual hair color ID for the character.<br>
	 * It checks if the player belongs to a specific class category.<br>
	 * If they do, it returns the default hair color from {@code PlayerAppearance}.<br>
	 * Otherwise, it looks for a custom variable named {@code visualHairColorId} and falls back to the default if not found.
	 * @return The integer ID of the current hair color.
	 */
	public int getVisualHairColor()
	{
		if (CategoryData.getInstance().isInCategory(CategoryType.DEATH_KNIGHT_ALL_CLASS, getPlayerClass().getId()))
		{
			return _appearance.getHairColor();
		}
		
		return getVariables().getInt("visualHairColorId", _appearance.getHairColor());
	}
	
	/**
	 * Retrieves the visual face ID for the character.<br>
	 * It checks for a custom variable named {@code visualFaceId}.<br>
	 * If that variable is not set, it returns the default face from {@code PlayerAppearance}.
	 * @return The integer ID of the current visual face.
	 */
	public int getVisualFace()
	{
		if (CategoryData.getInstance().isInCategory(CategoryType.DEATH_KNIGHT_ALL_CLASS, getPlayerClass().getId()))
		{
			return _appearance.getFace();
		}
		
		return getVariables().getInt("visualFaceId", _appearance.getFace());
	}
	
	/**
	 * Checks if the current actor has mentor status.<br>
	 * This method queries the {@code MentorManager} to verify permissions.
	 * @return {@code true} if the actor is a mentor, {@code false} otherwise.
	 */
	public boolean isMentor()
	{
		return MentorManager.getInstance().isMentor(getObjectId());
	}
	
	/**
	 * Checks if the current actor is currently a mentee.<br>
	 * This method queries the {@code MentorManager} to verify status.
	 * @return {@code true} if the actor is a mentee, {@code false} otherwise.
	 */
	public boolean isMentee()
	{
		return MentorManager.getInstance().isMentee(getObjectId());
	}
	
	/**
	 * Retrieves the current number of ability points for the character.<br>
	 * It checks if dual class is active to determine which variable to use.<br>
	 * Returns 0 if no value is found.
	 * @return The total amount of available ability points as an {@code int}.
	 */
	public int getAbilityPoints()
	{
		// Grand Crusade: 1 point per level after 84
		return Math.max(0, getLevel() - 84);
	}
	
	/**
	 * Retrieves the current count of Exalted Contribution Certificates.<br>
	 * This method checks the player inventory for item {@code 83670}.<br>
	 * It returns the quantity if found, otherwise it returns {@code 0}.
	 * @return The number of honor items held by the player.
	 */
	public int getHonorPoints()
	{
		final Item honorItem = getInventory().getItemByItemId(83670 /* Exalted Contribution Certificate - I */);
		return (int) (honorItem != null ? honorItem.getCount() : 0);
	}
	
	/**
	 * Retrieves the total number of honor points used by the player.<br>
	 * This value depends on whether the player is using a dual class or a main class.<br>
	 * It checks the active ability preset to determine which variable to return.
	 * @return The amount of honor points used as an {@code int}.
	 */
	public int getHonorPointsUsed()
	{
		if (getAbilityPreset() == 0)
		{
			if (isDualClassActive())
			{
				return getVariables().getInt(PlayerVariables.HONOR_POINTS_USED_DUAL_CLASS_A, 0);
			}
			
			return getVariables().getInt(PlayerVariables.HONOR_POINTS_USED_MAIN_CLASS_A, 0);
		}
		
		if (isDualClassActive())
		{
			return getVariables().getInt(PlayerVariables.HONOR_POINTS_USED_DUAL_CLASS_B, 0);
		}
		
		return getVariables().getInt(PlayerVariables.HONOR_POINTS_USED_MAIN_CLASS_B, 0);
	}
	
	/**
	 * Updates the number of honor points used by the player.<br>
	 * This method also handles skill data cleanup based on the provided flag.
	 * @param points The new amount of honor points used.
	 * @param deleteSkills Set to {@code true} to remove associated skills from variables.
	 */
	public void setHonorPointsUsed(int points, boolean deleteSkills)
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ABILITY_POINTS_CHANGED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerAbilityPointsChanged(this, getAbilityPointsUsed(), points), this);
		}
		
		if (getAbilityPreset() == 0)
		{
			if (isDualClassActive())
			{
				getVariables().set(PlayerVariables.HONOR_POINTS_USED_DUAL_CLASS_A, points);
				if (deleteSkills)
				{
					getVariables().remove(PlayerVariables.ABILITY_POINTS_DUAL_CLASS_SKILLS_A);
				}
			}
			else
			{
				getVariables().set(PlayerVariables.HONOR_POINTS_USED_MAIN_CLASS_A, points);
				if (deleteSkills)
				{
					getVariables().remove(PlayerVariables.ABILITY_POINTS_MAIN_CLASS_SKILLS_A);
				}
			}
		}
		else
		{
			if (isDualClassActive())
			{
				getVariables().set(PlayerVariables.HONOR_POINTS_USED_DUAL_CLASS_B, points);
				if (deleteSkills)
				{
					getVariables().remove(PlayerVariables.ABILITY_POINTS_DUAL_CLASS_SKILLS_B);
				}
			}
			else
			{
				getVariables().set(PlayerVariables.HONOR_POINTS_USED_MAIN_CLASS_B, points);
				if (deleteSkills)
				{
					getVariables().remove(PlayerVariables.ABILITY_POINTS_MAIN_CLASS_SKILLS_B);
				}
			}
		}
	}
	
	/**
	 * Retrieves the total number of ability points currently in use.<br>
	 * It checks if the player is a dual class to determine which variable to read.
	 * @return The amount of used ability points as an {@code int}.
	 */
	public int getAbilityPointsUsed()
	{
		if (getAbilityPreset() == 0)
		{
			if (isDualClassActive())
			{
				return getVariables().getInt(PlayerVariables.ABILITY_POINTS_USED_DUAL_CLASS_A, 0);
			}
			
			return getVariables().getInt(PlayerVariables.ABILITY_POINTS_USED_MAIN_CLASS_A, 0);
		}
		
		if (isDualClassActive())
		{
			return getVariables().getInt(PlayerVariables.ABILITY_POINTS_USED_DUAL_CLASS_B, 0);
		}
		
		return getVariables().getInt(PlayerVariables.ABILITY_POINTS_USED_MAIN_CLASS_B, 0);
	}
	
	/**
	 * Updates the number of ability points used by the player.<br>
	 * This method also handles skill deletion based on the provided flag.
	 * @param points The new amount of ability points used.
	 * @param deleteSkills Set to {@code true} to remove associated skills from the variables.
	 */
	public void setAbilityPointsUsed(int points, boolean deleteSkills)
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ABILITY_POINTS_CHANGED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerAbilityPointsChanged(this, getAbilityPointsUsed(), points), this);
		}
		
		if (getAbilityPreset() == 0)
		{
			if (isDualClassActive())
			{
				getVariables().set(PlayerVariables.ABILITY_POINTS_USED_DUAL_CLASS_A, points);
				if (deleteSkills)
				{
					getVariables().remove(PlayerVariables.ABILITY_POINTS_DUAL_CLASS_SKILLS_A);
				}
			}
			else
			{
				getVariables().set(PlayerVariables.ABILITY_POINTS_USED_MAIN_CLASS_A, points);
				if (deleteSkills)
				{
					getVariables().remove(PlayerVariables.ABILITY_POINTS_MAIN_CLASS_SKILLS_A);
				}
			}
		}
		else
		{
			if (isDualClassActive())
			{
				getVariables().set(PlayerVariables.ABILITY_POINTS_USED_DUAL_CLASS_B, points);
				if (deleteSkills)
				{
					getVariables().remove(PlayerVariables.ABILITY_POINTS_DUAL_CLASS_SKILLS_B);
				}
			}
			else
			{
				getVariables().set(PlayerVariables.ABILITY_POINTS_USED_MAIN_CLASS_B, points);
				if (deleteSkills)
				{
					getVariables().remove(PlayerVariables.ABILITY_POINTS_MAIN_CLASS_SKILLS_B);
				}
			}
		}
	}
	
	/**
	 * This method restores the ability skills for the character.<br>
	 * It removes current skills and reloads them from saved variables.<br>
	 * The process includes updating stats and broadcasting information to other players.
	 */
	public void restoreAbilitySkills()
	{
		for (SkillLearn sk : SkillTreeData.getInstance().getAbilitySkillTree().values())
		{
			final Skill skill = getKnownSkill(sk.getSkillId());
			if (skill != null)
			{
				removeSkill(skill);
				getEffectList().stopSkillEffects(SkillFinishType.SILENT, skill); // TODO: Check if retail shows system message.
			}
		}
		
		String skillsData = "";
		if (getAbilityPreset() == 0)
		{
			if (isDualClassActive())
			{
				skillsData = getVariables().getString(PlayerVariables.ABILITY_POINTS_DUAL_CLASS_SKILLS_A, "");
			}
			else
			{
				skillsData = getVariables().getString(PlayerVariables.ABILITY_POINTS_MAIN_CLASS_SKILLS_A, "");
			}
		}
		else
		{
			if (isDualClassActive())
			{
				skillsData = getVariables().getString(PlayerVariables.ABILITY_POINTS_DUAL_CLASS_SKILLS_B, "");
			}
			else
			{
				skillsData = getVariables().getString(PlayerVariables.ABILITY_POINTS_MAIN_CLASS_SKILLS_B, "");
			}
		}
		
		for (String skillPair : skillsData.split(","))
		{
			final String[] skillInfo = skillPair.split("-");
			if (skillInfo.length == 2)
			{
				final int skillId = Integer.parseInt(skillInfo[0]);
				final int skillLevel = Integer.parseInt(skillInfo[1]);
				final SkillLearn raceskill = SkillTreeData.getInstance().getAbilitySkill(skillId, skillLevel);
				if (raceskill != null)
				{
					addSkill(SkillData.getInstance().getSkill(skillId, skillLevel), false);
				}
			}
		}
		
		sendPacket(new ExAcquireAPSkillList(this));
		
		ThreadPool.schedule(() ->
		{
			sendSkillList();
			getStat().recalculateStats(false);
			broadcastUserInfo();
		}, 100);
	}
	
	/**
	 * Retrieves the current ability preset value.<br>
	 * This method fetches the integer stored in {@code PlayerVariables.ABILITY_PRESET}.<br>
	 * It returns a default value of {@code 0} if no preset is set.
	 * @return The current ability preset as an {@code int}.
	 */
	public int getAbilityPreset()
	{
		return getVariables().getInt(PlayerVariables.ABILITY_PRESET, 0);
	}
	
	/**
	 * Sets the ability preset for the player.<br>
	 * This updates the {@code PlayerVariables.ABILITY_PRESET} variable.
	 * @param preset The new preset value to apply.
	 */
	public void setAbilityPreset(int preset)
	{
		getVariables().set(PlayerVariables.ABILITY_PRESET, preset);
	}
	
	/**
	 * Retrieves the total world chat points for the player.<br>
	 * This value is calculated based on daily limits and character stats.
	 * @return The total number of world chat points as an {@code int}.
	 */
	public int getWorldChatPoints()
	{
		return (int) ((GeneralConfig.WORLD_CHAT_POINTS_PER_DAY + getStat().getAdd(Stat.WORLD_CHAT_POINTS, 0)) * getStat().getMul(Stat.WORLD_CHAT_POINTS, 1));
	}
	
	/**
	 * Retrieves the current world chat usage value.<br>
	 * This method fetches the integer stored in the {@code PlayerVariables} variable.
	 * @return The current world chat usage as an {@code int}.
	 */
	public int getWorldChatUsed()
	{
		return getVariables().getInt(PlayerVariables.WORLD_CHAT_VARIABLE_NAME, 0);
	}
	
	/**
	 * Updates the number of times world chat has been used.<br>
	 * This method stores the value in the player's variables.
	 * @param timesUsed The total count of world chat uses.
	 */
	public void setWorldChatUsed(int timesUsed)
	{
		getVariables().set(PlayerVariables.WORLD_CHAT_VARIABLE_NAME, timesUsed);
	}
	
	/**
	 * Retrieves the side of the castle associated with the player.<br>
	 * Returns {@code CastleSide.NEUTRAL} if the player has no clan or a valid castle is not found.
	 * @return The {@code CastleSide} of the player's castle.
	 */
	public CastleSide getPlayerSide()
	{
		if (_clan == null)
		{
			return CastleSide.NEUTRAL;
		}
		
		final int castleId = _clan.getCastleId();
		if (castleId == 0)
		{
			return CastleSide.NEUTRAL;
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(castleId);
		if (castle == null)
		{
			return CastleSide.NEUTRAL;
		}
		
		return castle.getSide();
	}
	
	/**
	 * Checks if the player belongs to the dark side.<br>
	 * This method compares the current side with {@code CastleSide#DARK}.
	 * @return {@code true} if the player is on the dark side, {@code false} otherwise.
	 */
	public boolean isOnDarkSide()
	{
		return getPlayerSide() == CastleSide.DARK;
	}
	
	/**
	 * Checks if the player belongs to the light side.<br>
	 * This method compares the current side with {@code CastleSide#LIGHT}.
	 * @return {@code true} if the player is on the light side, {@code false} otherwise.
	 */
	public boolean isOnLightSide()
	{
		return getPlayerSide() == CastleSide.LIGHT;
	}
	
	/**
	 * Retrieves the maximum summon points for the player.<br>
	 * This method fetches the value from the {@code Stat} object.<br>
	 * It returns a default of {@code 0} if no value is found.
	 * @return The current maximum summon points as an {@code int}.
	 */
	public int getMaxSummonPoints()
	{
		return (int) getStat().getValue(Stat.MAX_SUMMON_POINTS, 0);
	}
	
	/**
	 * Calculates the total sum of points for all active summons.<br>
	 * It iterates through every {@code Summon} in the servitors collection.
	 * @return The total number of summon points as an {@code int}.
	 */
	public int getSummonPoints()
	{
		int totalPoints = 0;
		for (Summon summon : getServitors().values())
		{
			totalPoints += summon.getSummonPoints();
		}
		
		return totalPoints;
	}
	
	/**
	 * Adds a new request to the internal collection.<br>
	 * This method checks if the request is valid and not already present.
	 * @param request The {@code AbstractRequest} object to be added.
	 * @return {@code true} if the request was successfully added, otherwise {@code false}.
	 */
	public boolean addRequest(AbstractRequest request)
	{
		return canRequest(request) && (_requests.putIfAbsent(request.getClass(), request) == null);
	}
	
	/**
	 * Checks if the given request is compatible with all existing requests.<br>
	 * It iterates through all active requests in the internal collection.<br>
	 * If any request cannot work with the new one, it returns {@code false}.<br>
	 * Otherwise, it returns {@code true}.
	 * @param request The {@code AbstractRequest} to validate.
	 * @return {@code true} if compatible with all requests, {@code false} otherwise.
	 */
	public boolean canRequest(AbstractRequest request)
	{
		for (AbstractRequest r : _requests.values())
		{
			if (!request.canWorkWith(r))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Removes all requests of a specific type from the internal request map.<br>
	 * This method checks if any requests were actually removed.
	 * @param clazz The {@code Class} type of the request to be removed.
	 * @return {@code true} if at least one request was removed, otherwise {@code false}.
	 */
	public boolean removeRequest(Class<? extends AbstractRequest> clazz)
	{
		return _requests.remove(clazz) != null;
	}
	
	/**
	 * Retrieves a specific request from the internal request map.<br>
	 * The method uses the provided class to find and cast the correct object.
	 * @param <T> The {@code AbstractRequest} subtype to retrieve.
	 * @param requestClass The {@code Class} type of the request to retrieve.
	 * @return The requested object cast to type {@code T}.
	 */
	public <T extends AbstractRequest> T getRequest(Class<T> requestClass)
	{
		return requestClass.cast(_requests.get(requestClass));
	}
	
	/**
	 * Checks if there are any pending requests.<br>
	 * Returns {@code true} if the request list is not empty.<br>
	 * Returns {@code false} if there are no requests.
	 * @return a boolean indicating if requests exist.
	 */
	public boolean hasRequests()
	{
		return !_requests.isEmpty();
	}
	
	/**
	 * Checks if there are any pending item requests.<br>
	 * It iterates through all active requests to find an item request.
	 * @return {@code true} if at least one item request exists, otherwise {@code false}.
	 */
	public boolean hasItemRequest()
	{
		for (AbstractRequest request : _requests.values())
		{
			if (request.isItemRequest())
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if a specific request type is currently active.<br>
	 * It first looks through the provided list of classes.<br>
	 * If none are found, it checks the primary {@code requestClass}.
	 * @param requestClass The primary class to check for an active request.
	 * @param classes A variable number of additional classes to check.
	 * @return {@code true} if any of the provided classes have an active request; {@code false} otherwise.
	 */
	@SafeVarargs
	public final boolean hasRequest(Class<? extends AbstractRequest> requestClass, Class<? extends AbstractRequest>... classes)
	{
		for (Class<? extends AbstractRequest> clazz : classes)
		{
			if (_requests.containsKey(clazz))
			{
				return true;
			}
		}
		
		return _requests.containsKey(requestClass);
	}
	
	/**
	 * Checks if a specific object is currently being used by any active request.<br>
	 * It iterates through all pending requests to find a match for the given ID.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if the object is in use, otherwise {@code false}.
	 */
	public boolean isProcessingItem(int objectId)
	{
		for (AbstractRequest request : _requests.values())
		{
			if (request.isUsing(objectId))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Removes all active requests associated with a specific object.<br>
	 * This method checks the internal request map and deletes entries where the request is currently being used by {@code objectId}.
	 * @param objectId The unique identifier of the object whose requests should be removed.
	 */
	public void removeRequestsThatProcessesItem(int objectId)
	{
		_requests.values().removeIf(req -> req.isUsing(objectId));
	}
	
	/**
	 * Retrieves the current amount of prime points for the player.<br>
	 * This value is fetched from the account variables.<br>
	 * It returns 0 if no points are found.
	 * @return The number of prime points as an {@code int}.
	 */
	public int getPrimePoints()
	{
		return getAccountVariables().getInt("PRIME_POINTS", 0);
	}
	
	/**
	 * Updates the prime points for the account.<br>
	 * This method ensures that the value is never less than {@code 0}.<br>
	 * The change is saved immediately to the database.
	 * @param points The number of prime points to set.
	 */
	public void setPrimePoints(int points)
	{
		// Immediate store upon change.
		final AccountVariables vars = getAccountVariables();
		vars.set("PRIME_POINTS", Math.max(points, 0));
		vars.storeMe();
	}
	
	/**
	 * Checks if the actor can receive experience based on its current state.<br>
	 * It returns a {@code TerminateReturn} object with all flags set to {@code false} if the actor is dead.<br>
	 * Otherwise, it returns an object with all flags set to {@code true}.
	 * @return A {@code TerminateReturn} indicating whether experience processing should continue.
	 */
	private TerminateReturn onExperienceReceived()
	{
		if (isDead())
		{
			return new TerminateReturn(false, false, false);
		}
		
		return new TerminateReturn(true, true, true);
	}
	
	/**
	 * Disables the experience gain for the player.<br>
	 * This method adds a listener to handle changes in playable experience.<br>
	 * It ensures that {@code onExperienceReceived()} is triggered when experience changes.
	 */
	public void disableExpGain()
	{
		addListener(new FunctionEventListener(this, EventType.ON_PLAYABLE_EXP_CHANGED, (OnPlayableExpChanged _) -> onExperienceReceived(), this));
	}
	
	/**
	 * Enables experience gain for the actor.<br>
	 * This method removes any listeners that prevent the owner from gaining experience.
	 */
	public void enableExpGain()
	{
		removeListenerIf(EventType.ON_PLAYABLE_EXP_CHANGED, listener -> listener.getOwner() == this);
	}
	
	/**
	 * Retrieves the most recent commission information for all players.<br>
	 * The data is mapped by player ID.
	 * @return A {@code Map} where keys are player IDs and values are {@code ExResponseCommissionInfo} objects.
	 */
	public Map<Integer, ExResponseCommissionInfo> getLastCommissionInfos()
	{
		return _lastCommissionInfos;
	}
	
	/**
	 * Retrieves the set of whisperer IDs.
	 * @return a {@code Set<Integer>} containing all whisperer identifiers.
	 */
	public Set<Integer> getWhisperers()
	{
		return _whisperers;
	}
	
	/**
	 * Retrieves the current matching room for this actor.
	 * @return the {@code MatchingRoom} object associated with this entity.
	 */
	public MatchingRoom getMatchingRoom()
	{
		return _matchingRoom;
	}
	
	/**
	 * Sets the current {@code MatchingRoom} for this actor.<br>
	 * This updates the internal {@code _matchingRoom} field.
	 * @param matchingRoom The {@code MatchingRoom} to assign.
	 */
	public void setMatchingRoom(MatchingRoom matchingRoom)
	{
		_matchingRoom = matchingRoom;
	}
	
	/**
	 * Checks if the player is currently in a matching room.<br>
	 * Returns {@code true} if the internal room reference is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the player is in a matching room, {@code false} otherwise.
	 */
	public boolean isInMatchingRoom()
	{
		return _matchingRoom != null;
	}
	
	/**
	 * Retrieves the total number of vitality items used by the player.<br>
	 * This value is stored in a specific player variable.
	 * @return The count of vitality items used as an {@code int}.
	 */
	public int getVitalityItemsUsed()
	{
		return getVariables().getInt(PlayerVariables.VITALITY_ITEMS_USED_VARIABLE_NAME, 0);
	}
	
	/**
	 * Updates the number of vitality items used by the player.<br>
	 * This method stores the value in the {@code PlayerVariables} collection.
	 * @param used The total count of vitality items that have been consumed.
	 */
	public void setVitalityItemsUsed(int used)
	{
		final PlayerVariables vars = getVariables();
		vars.set(PlayerVariables.VITALITY_ITEMS_USED_VARIABLE_NAME, used);
		vars.storeMe();
	}
	
	/**
	 * Checks if the entity is visible to a specific {@code Player}.<br>
	 * The entity is visible if it passes the base visibility check.<br>
	 * It is also visible if both entities belong to the same party.
	 * @param player The {@code Player} checking for visibility.
	 * @return {@code true} if the entity is visible, {@code false} otherwise.
	 */
	@Override
	public boolean isVisibleFor(Player player)
	{
		return (super.isVisibleFor(player) || ((player.getParty() != null) && (player.getParty() == getParty())));
	}
	
	/**
	 * Sets the unique identifier for the quest zone.<br>
	 * This value determines which specific quest area the actor is currently in.
	 * @param id The {@code int} ID of the quest zone to set.
	 */
	public void setQuestZoneId(int id)
	{
		_questZoneId = id;
	}
	
	/**
	 * Retrieves the unique identifier for the current quest zone.<br>
	 * This value identifies which specific area a player is currently in for quest purposes.
	 * @return The {@code int} ID of the quest zone.
	 */
	public int getQuestZoneId()
	{
		return _questZoneId;
	}
	
	/**
	 * Updates the inventory data for the player.<br>
	 * This method cancels any existing update task and schedules a new one.<br>
	 * It sends the updated packet to the client after a short delay.
	 * @param iu The {@code InventoryUpdate} object containing the new item entries.
	 */
	public void sendInventoryUpdate(InventoryUpdate iu)
	{
		if (_inventoryUpdateTask != null)
		{
			_inventoryUpdateTask.cancel(false);
		}
		
		_inventoryUpdate.putAll(iu.getItemEntries());
		
		_inventoryUpdateTask = ThreadPool.schedule(() ->
		{
			sendPacket(_inventoryUpdate);
			
			updateAdenaAndWeight();
		}, 100);
	}
	
	/**
	 * Sends the pending inventory update now instead of waiting for the scheduled task.
	 */
	public void flushInventoryUpdate()
	{
		if (_inventoryUpdateTask != null)
		{
			_inventoryUpdateTask.cancel(false);
			_inventoryUpdateTask = null;
		}
		
		sendPacket(_inventoryUpdate);
		updateAdenaAndWeight();
	}
	
	/**
	 * Sends the item list and quest items to the client.<br>
	 * This method schedules a task to run after {@code 250} milliseconds.<br>
	 * It sends multiple {@code ItemList} and {@code ExQuestItemList} packets.<br>
	 * Finally, it updates the player's adena and weight.
	 */
	public void sendItemList()
	{
		if (_itemListTask != null)
		{
			_itemListTask.cancel(false);
		}
		
		_itemListTask = ThreadPool.schedule(() ->
		{
			sendPacket(new ItemList(1, this));
			sendPacket(new ItemList(2, this));
			sendPacket(new ExQuestItemList(1, this));
			sendPacket(new ExQuestItemList(2, this));
			
			updateAdenaAndWeight();
		}, 250);
	}
	
	/**
	 * Updates the player's adena and weight information.<br>
	 * This method cancels any existing task and schedules a new one.<br>
	 * It sends packets to update the inventory count and weight every 800 milliseconds.
	 */
	public void updateAdenaAndWeight()
	{
		if (_adenaAndWeightTask != null)
		{
			_adenaAndWeightTask.cancel(false);
		}
		
		_adenaAndWeightTask = ThreadPool.schedule(() ->
		{
			sendPacket(new ExAdenaInvenCount(this));
			sendPacket(new ExUserInfoInvenWeight(this));
		}, 800);
	}
	
	/**
	 * Retrieves the current fishing status.
	 * @return the {@code Fishing} object representing the player's fishing state.
	 */
	public Fishing getFishing()
	{
		return _fishing;
	}
	
	/**
	 * Checks if the actor is currently engaged in fishing.
	 * @return {@code true} if the actor is fishing, {@code false} otherwise.
	 */
	public boolean isFishing()
	{
		return _fishing.isFishing();
	}
	
	/**
	 * Retrieves the current movement type of the actor.<br>
	 * This method checks if the actor is currently sitting.<br>
	 * If not sitting, it returns the base movement type from the parent class.
	 * @return The {@code MoveType} representing the current state.
	 */
	@Override
	public MoveType getMoveType()
	{
		if (_waitTypeSitting)
		{
			return MoveType.SITTING;
		}
		
		return super.getMoveType();
	}
	
	/**
	 * Retrieves the current group type of the player.<br>
	 * It checks if the player is in a party or a command channel.<br>
	 * Returns {@code GroupType.NONE} if the player is not in any group.
	 * @return The current {@code GroupType} of the actor.
	 */
	public GroupType getGroupType()
	{
		return isInParty() ? (_party.isInCommandChannel() ? GroupType.COMMAND_CHANNEL : GroupType.PARTY) : GroupType.NONE;
	}
	
	/**
	 * Stops all active background tasks associated with this entity.<br>
	 * This method cancels various scheduled tasks such as mount feeds, fame updates, and quest timers.<br>
	 * It also clears the internal collections of timer holders and schedules.
	 */
	public void stopAllTasks()
	{
		if ((_mountFeedTask != null) && !_mountFeedTask.isDone() && !_mountFeedTask.isCancelled())
		{
			_mountFeedTask.cancel(false);
			_mountFeedTask = null;
		}
		
		if ((_dismountTask != null) && !_dismountTask.isDone() && !_dismountTask.isCancelled())
		{
			_dismountTask.cancel(false);
			_dismountTask = null;
		}
		
		if ((_fameTask != null) && !_fameTask.isDone() && !_fameTask.isCancelled())
		{
			_fameTask.cancel(false);
			_fameTask = null;
		}
		
		if ((_teleportWatchdog != null) && !_teleportWatchdog.isDone() && !_teleportWatchdog.isCancelled())
		{
			_teleportWatchdog.cancel(false);
			_teleportWatchdog = null;
		}
		
		_recommendations.stopGiveTask();
		
		if ((_chargeTask != null) && !_chargeTask.isDone() && !_chargeTask.isCancelled())
		{
			_chargeTask.cancel(false);
			_chargeTask = null;
		}
		
		if ((_soulTask != null) && !_soulTask.isDone() && !_soulTask.isCancelled())
		{
			_soulTask.cancel(false);
			_soulTask = null;
		}
		
		if ((_taskRentPet != null) && !_taskRentPet.isDone() && !_taskRentPet.isCancelled())
		{
			_taskRentPet.cancel(false);
			_taskRentPet = null;
		}
		
		if ((_taskWater != null) && !_taskWater.isDone() && !_taskWater.isCancelled())
		{
			_taskWater.cancel(false);
			_taskWater = null;
		}
		
		if ((_fallingDamageTask != null) && !_fallingDamageTask.isDone() && !_fallingDamageTask.isCancelled())
		{
			_fallingDamageTask.cancel(false);
			_fallingDamageTask = null;
		}
		
		if ((_timedHuntingZoneTask != null) && !_timedHuntingZoneTask.isDone() && !_timedHuntingZoneTask.isCancelled())
		{
			_timedHuntingZoneTask.cancel(false);
			_timedHuntingZoneTask = null;
		}
		
		if ((_taskWarnUserTakeBreak != null) && !_taskWarnUserTakeBreak.isDone() && !_taskWarnUserTakeBreak.isCancelled())
		{
			_taskWarnUserTakeBreak.cancel(false);
			_taskWarnUserTakeBreak = null;
		}
		
		_hennas.stopSchedules();
		
		synchronized (_questTimers)
		{
			for (QuestTimer timer : _questTimers)
			{
				timer.cancelTask();
			}
			
			_questTimers.clear();
		}
		
		synchronized (_timerHolders)
		{
			for (TimerHolder<?> timer : _timerHolders)
			{
				timer.cancelTask();
			}
			
			_timerHolders.clear();
		}
	}
	
	/**
	 * Adds a new {@code QuestTimer} to the internal list of active timers.<br>
	 * This method ensures thread safety by using a synchronized block.
	 * @param questTimer The {@code QuestTimer} object to be added.
	 */
	public void addQuestTimer(QuestTimer questTimer)
	{
		synchronized (_questTimers)
		{
			_questTimers.add(questTimer);
		}
	}
	
	/**
	 * Removes a specific quest timer from the active timers list.<br>
	 * This method ensures thread safety by using a synchronized block.
	 * @param questTimer The {@code QuestTimer} object to be removed.
	 */
	public void removeQuestTimer(QuestTimer questTimer)
	{
		synchronized (_questTimers)
		{
			_questTimers.remove(questTimer);
		}
	}
	
	/**
	 * Adds a new {@code TimerHolder} to the internal collection.<br>
	 * This method ensures thread safety during the addition process.
	 * @param timer The {@code TimerHolder} instance to be added.
	 */
	public void addTimerHolder(TimerHolder<?> timer)
	{
		synchronized (_timerHolders)
		{
			_timerHolders.add(timer);
		}
	}
	
	/**
	 * Removes a specific timer from the internal collection.<br>
	 * This method ensures that the {@code TimerHolder} is no longer tracked by the system.
	 * @param timer The {@code TimerHolder} instance to be removed.
	 */
	public void removeTimerHolder(TimerHolder<?> timer)
	{
		synchronized (_timerHolders)
		{
			_timerHolders.remove(timer);
		}
	}
	
	/**
	 * Checks if the character has the true hero status.
	 * @return {@code true} if the character is a true hero, {@code false} otherwise.
	 */
	public boolean isTrueHero()
	{
		return _trueHero;
	}
	
	/**
	 * Sets whether the character is considered a true hero.<br>
	 * This updates the {@code _trueHero} status field.
	 * @param value The boolean value to set for the true hero status.
	 */
	public void setTrueHero(boolean value)
	{
		_trueHero = value;
	}
	
	/**
	 * Retrieves the number of points associated with a specific faction.<br>
	 * This method looks up the integer value stored in the player variables.<br>
	 * If no value exists, it returns {@code 0}.
	 * @param faction The {@code Faction} to check for points.
	 * @return The current point count for the given faction.
	 */
	public int getFactionPoints(Faction faction)
	{
		return getVariables().getInt(faction.toString(), 0);
	}
	
	/**
	 * Retrieves the current level of a specific {@code Faction}.<br>
	 * This method compares the player's faction points against the required points for each level.
	 * @param faction The {@code Faction} to check.
	 * @return The integer level of the provided {@code Faction}.
	 */
	public int getFactionLevel(Faction faction)
	{
		final int currentPoints = getFactionPoints(faction);
		for (int i = 0; i < faction.getLevelCount(); i++)
		{
			if (currentPoints <= faction.getPointsOfLevel(i))
			{
				return i;
			}
		}
		
		return 0;
	}
	
	/**
	 * Calculates the progress toward the next level for a specific faction.<br>
	 * It returns a value between 0.0 and 1.0 based on current points.
	 * @param faction The {@code Faction} to check the progress for.
	 * @return The progress as a {@code float}.
	 */
	public float getFactionProgress(Faction faction)
	{
		final int currentLevel = getFactionLevel(faction);
		final int currentLevelPoints = getFactionPoints(faction);
		final int previousLevelPoints = faction.getPointsOfLevel(currentLevel - 1);
		final int nextLevelPoints = faction.getPointsOfLevel(currentLevel + 1);
		return (float) (currentLevelPoints - previousLevelPoints) / (nextLevelPoints - previousLevelPoints);
	}
	
	/**
	 * Adds a specific amount of points to a faction.<br>
	 * This method updates the player's variables and checks for level increases.<br>
	 * It also sends a notification message to the player.
	 * @param faction The {@code Faction} object to modify.
	 * @param count The number of points to add.
	 */
	public void addFactionPoints(Faction faction, int count)
	{
		final int currentPoints = getFactionPoints(faction);
		final int oldLevel = getFactionLevel(faction);
		if ((currentPoints + count) < faction.getPointsOfLevel(faction.getLevelCount() - 1))
		{
			getVariables().set(faction.toString(), currentPoints + count);
			sendPacket(new SystemMessage(SystemMessageId.YOU_OBTAINED_S1_AMITY_POINTS_FOR_S2).addInt(count).addFactionName(faction.getId()));
		}
		else
		{
			getVariables().set(faction.toString(), faction.getPointsOfLevel(faction.getLevelCount() - 1));
		}
		
		if (oldLevel < getFactionLevel(faction))
		{
			sendPacket(new SystemMessage(SystemMessageId.THE_S1_FACTION_LEVEL_HAS_INCREASED_OPEN_THE_FACTIONS_WINDOW_TO_LEARN_MORE).addFactionName(faction.getId()));
		}
	}
	
	/**
	 * Initializes the cache for status updates.<br>
	 * This method sets up the default values to be tracked for this entity.<br>
	 * It calls the parent class initialization and adds specific types like {@code LEVEL} and {@code CP}.
	 */
	@Override
	protected void initStatusUpdateCache()
	{
		super.initStatusUpdateCache();
		addStatusUpdateValue(StatusUpdateType.LEVEL);
		addStatusUpdateValue(StatusUpdateType.MAX_CP);
		addStatusUpdateValue(StatusUpdateType.CUR_CP);
	}
	
	/**
	 * Attempts to trigger a lucky event based on the player's luck stat.<br>
	 * It checks if a random roll is successful and ensures the skill is not on cooldown.<br>
	 * If successful, it triggers the {@code LUCKY_CLOVER} skill and sends a system message.
	 * @return {@code true} if the lucky event was triggered, {@code false} otherwise.
	 */
	public boolean tryLuck()
	{
		if (((Rnd.nextDouble() * 100) < (BaseStat.LUC.getValue(getLUC()) * RatesConfig.LUCKY_CHANCE_MULTIPLIER)) && !hasSkillReuse(CommonSkill.LUCKY_CLOVER.getSkill().getReuseHashCode()))
		{
			SkillCaster.triggerCast(this, this, CommonSkill.LUCKY_CLOVER.getSkill());
			sendPacket(SystemMessageId.LADY_LUCK_SMILES_UPON_YOU);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Retrieves the training camp information for the player.<br>
	 * This method fetches data from the account variables.<br>
	 * It returns a {@code TrainingHolder} object if the data exists.<br>
	 * If no information is found, it returns {@code null}.
	 * @return A {@code TrainingHolder} containing the camp details or {@code null}.
	 */
	public TrainingHolder getTraingCampInfo()
	{
		final String info = getAccountVariables().getString(TRAINING_CAMP_VAR, null);
		if (info == null)
		{
			return null;
		}
		
		return new TrainingHolder(Integer.parseInt(info.split(";")[0]), Integer.parseInt(info.split(";")[1]), Integer.parseInt(info.split(";")[2]), Long.parseLong(info.split(";")[3]), Long.parseLong(info.split(";")[4]));
	}
	
	/**
	 * Updates the training camp information for the player.<br>
	 * This method saves the training camp details into the account variables.
	 * @param holder The {@code TrainingHolder} object containing the camp data.
	 */
	public void setTraingCampInfo(TrainingHolder holder)
	{
		getAccountVariables().set(TRAINING_CAMP_VAR, holder.getObjectId() + ";" + holder.getClassIndex() + ";" + holder.getLevel() + ";" + holder.getStartTime() + ";" + holder.getEndTime());
	}
	
	/**
	 * Removes the training camp information from the account variables.<br>
	 * This method clears the data associated with {@code TRAINING_CAMP_VAR}.
	 */
	public void removeTraingCampInfo()
	{
		getAccountVariables().remove(TRAINING_CAMP_VAR);
	}
	
	/**
	 * Retrieves the duration of the training camp.<br>
	 * This value is stored in the account variables.<br>
	 * It returns {@code 0} if no duration is set.
	 * @return The training camp duration as a {@code long}.
	 */
	public long getTraingCampDuration()
	{
		return getAccountVariables().getLong(TRAINING_CAMP_DURATION, 0);
	}
	
	/**
	 * Sets the duration for the training camp.<br>
	 * This value is stored in the account variables.
	 * @param duration The length of time for the training camp.
	 */
	public void setTraingCampDuration(long duration)
	{
		getAccountVariables().set(TRAINING_CAMP_DURATION, duration);
	}
	
	/**
	 * Resets the training camp duration for the player.<br>
	 * This method removes the {@code TRAINING_CAMP_DURATION} variable from the account variables.
	 */
	public void resetTraingCampDuration()
	{
		getAccountVariables().remove(TRAINING_CAMP_DURATION);
	}
	
	/**
	 * Checks if the player is currently inside a training camp.<br>
	 * It verifies that the training information exists and has not expired.
	 * @return {@code true} if the player is in an active training camp, {@code false} otherwise.
	 */
	public boolean isInTraingCamp()
	{
		final TrainingHolder trainingHolder = getTraingCampInfo();
		return (trainingHolder != null) && (trainingHolder.getEndTime() > System.currentTimeMillis());
	}
	
	/**
	 * Retrieves the {@code Attendance} of this player.
	 * @return the {@code Attendance} of this player.
	 */
	public Attendance getAttendance()
	{
		return _attendance;
	}
	
	/**
	 * Retrieves the current auto-play settings.
	 * @return the {@code AutoPlaySettingsHolder} object containing the settings.
	 */
	public AutoPlaySettingsHolder getAutoPlaySettings()
	{
		return _autoPlaySettings;
	}
	
	/**
	 * Retrieves the current auto-use settings for this actor.
	 * @return the {@code AutoUseSettingsHolder} containing the configuration.
	 */
	public AutoUseSettingsHolder getAutoUseSettings()
	{
		return _autoUseSettings;
	}
	
	/**
	 * Sets whether the actor is currently in auto-play mode.<br>
	 * If {@code false}, it may remove the actor from offline play if configured.
	 * @param value The boolean state to set for auto-playing.
	 */
	public void setAutoPlaying(boolean value)
	{
		_autoPlaying.set(value);
		
		if (!value && _offlineMode.isPlayRunning() && OfflinePlayConfig.RESTORE_AUTO_PLAY_OFFLINERS)
		{
			OfflinePlayTable.getInstance().removeOfflinePlay(this);
		}
	}
	
	/**
	 * Checks if the actor is currently in auto-play mode.
	 * @return {@code true} if auto-playing, {@code false} otherwise.
	 */
	public boolean isAutoPlaying()
	{
		return _autoPlaying.get();
	}
	
	/**
	 * Sets whether the auto-play feature should resume.<br>
	 * This updates the {@code _resumedAutoPlay} state.
	 * @param value The boolean value to set for resuming auto-play.
	 */
	public void setResumedAutoPlay(boolean value)
	{
		_resumedAutoPlay = value;
	}
	
	/**
	 * Checks if the auto-play feature has been resumed.
	 * @return {@code true} if auto-play is currently resumed, {@code false} otherwise.
	 */
	public boolean hasResumedAutoPlay()
	{
		return _resumedAutoPlay;
	}
	
	/**
	 * Restores the auto-play settings for the player.<br>
	 * This method checks if auto-play is enabled in {@code GeneralConfig}.<br>
	 * It retrieves saved variables and updates the current {@code AutoPlaySettings}.<br>
	 * If active, it starts the {@code AutoPlayTaskManager} task.
	 */
	public void restoreAutoSettings()
	{
		if (!GeneralConfig.ENABLE_AUTO_PLAY || !getVariables().contains(PlayerVariables.AUTO_USE_SETTINGS))
		{
			return;
		}
		
		final List<Integer> settings = getVariables().getIntegerList(PlayerVariables.AUTO_USE_SETTINGS);
		if (settings.isEmpty())
		{
			return;
		}
		
		final int options = settings.get(0);
		final boolean active = GeneralConfig.RESUME_AUTO_PLAY && (settings.get(1) == 1);
		final boolean pickUp = settings.get(2) == 1;
		final int nextTargetMode = settings.get(3);
		final boolean shortRange = settings.get(4) == 1;
		final int potionPercent = settings.get(5);
		final boolean respectfulHunting = settings.get(6) == 1;
		final int macroIndex = settings.size() < 9 ? 0 : settings.get(8);
		
		final AutoPlaySettingsHolder autoPlaySettings = getAutoPlaySettings();
		autoPlaySettings.setAutoPotionPercent(potionPercent);
		autoPlaySettings.setOptions(options);
		autoPlaySettings.setPickup(pickUp);
		autoPlaySettings.setNextTargetMode(nextTargetMode);
		autoPlaySettings.setShortRange(shortRange);
		autoPlaySettings.setRespectfulHunting(respectfulHunting);
		autoPlaySettings.setMacroIndex(macroIndex);
		
		sendPacket(new ExAutoPlaySettingSend(options, active, pickUp, nextTargetMode, shortRange, potionPercent, respectfulHunting, macroIndex));
		
		if (active)
		{
			AutoPlayTaskManager.getInstance().startAutoPlay(this);
		}
		
		_resumedAutoPlay = true;
	}
	
	/**
	 * Restores the auto-use status for shortcuts.<br>
	 * This method checks if the {@code AUTO_USE_SHORTCUTS} variable is enabled.<br>
	 * It then updates all valid skills, actions, and items to have their auto-use property set to {@code true}.
	 */
	public void restoreAutoShortcutVisual()
	{
		if (!getVariables().contains(PlayerVariables.AUTO_USE_SHORTCUTS))
		{
			return;
		}
		
		final List<Integer> positions = getVariables().getIntegerList(PlayerVariables.AUTO_USE_SHORTCUTS);
		for (Shortcut shortcut : getAllShortcuts())
		{
			final Integer position = shortcut.getSlot() + (shortcut.getPage() * Shortcuts.MAX_SHORTCUTS_PER_BAR);
			if (!positions.contains(position))
			{
				continue;
			}
			
			final ShortcutType shortcutType = shortcut.getType();
			if (shortcutType == ShortcutType.SKILL)
			{
				final Skill knownSkill = getKnownSkill(shortcut.getId());
				if (knownSkill != null)
				{
					shortcut.setAutoUse(true);
				}
			}
			else if (shortcutType == ShortcutType.ACTION)
			{
				shortcut.setAutoUse(true);
			}
			else
			{
				final Item item = getInventory().getItemByObjectId(shortcut.getId());
				if (item != null)
				{
					shortcut.setAutoUse(true);
				}
			}
		}
	}
	
	/**
	 * Restores the auto-use status for all shortcuts.<br>
	 * This method checks if the {@code AUTO_USE_SHORTCUTS} variable is enabled.<br>
	 * It then updates each shortcut to be active based on its type and position.<br>
	 * Shortcuts are registered with the {@code AutoUseTaskManager} as actions, skills, or items.
	 */
	public void restoreAutoShortcuts()
	{
		if (!getVariables().contains(PlayerVariables.AUTO_USE_SHORTCUTS))
		{
			return;
		}
		
		final List<Integer> positions = getVariables().getIntegerList(PlayerVariables.AUTO_USE_SHORTCUTS);
		for (Shortcut shortcut : getAllShortcuts())
		{
			final Integer position = shortcut.getSlot() + (shortcut.getPage() * Shortcuts.MAX_SHORTCUTS_PER_BAR);
			if (!positions.contains(position))
			{
				continue;
			}
			
			if (shortcut.getType() == ShortcutType.ACTION)
			{
				shortcut.setAutoUse(true);
				AutoUseTaskManager.getInstance().addAutoAction(this, shortcut.getId());
				continue;
			}
			
			final Skill knownSkill = getKnownSkill(shortcut.getId());
			if (knownSkill != null)
			{
				shortcut.setAutoUse(true);
				if (knownSkill.hasNegativeEffect())
				{
					AutoUseTaskManager.getInstance().addAutoSkill(this, shortcut.getId());
				}
				else
				{
					AutoUseTaskManager.getInstance().addAutoBuff(this, shortcut.getId());
				}
			}
			else
			{
				final Item item = getInventory().getItemByObjectId(shortcut.getId());
				if (item != null)
				{
					shortcut.setAutoUse(true);
					if (item.isPotion())
					{
						AutoUseTaskManager.getInstance().setAutoPotionItem(this, item.getId());
					}
					else
					{
						AutoUseTaskManager.getInstance().addAutoSupplyItem(this, item.getId());
					}
				}
			}
		}
	}
	
	/**
	 * Adds a shortcut to the auto-use list.<br>
	 * This method updates the player's saved variables for shortcuts.<br>
	 * It checks if the shortcut exists and enables it if found.
	 * @param slot The index of the shortcut slot.
	 * @param page The page number where the shortcut is located.
	 */
	public synchronized void addAutoShortcut(int slot, int page)
	{
		final List<Integer> positions = getVariables().getIntegerList(PlayerVariables.AUTO_USE_SHORTCUTS);
		final Shortcut usedShortcut = getShortcut(slot, page);
		if (usedShortcut == null)
		{
			final Integer position = slot + (page * Shortcuts.MAX_SHORTCUTS_PER_BAR);
			positions.remove(position);
		}
		else
		{
			for (Shortcut shortcut : getAllShortcuts())
			{
				if ((usedShortcut.getId() == shortcut.getId()) && (usedShortcut.getType() == shortcut.getType()))
				{
					shortcut.setAutoUse(true);
					sendPacket(new ExActivateAutoShortcut(shortcut, true));
					final Integer position = shortcut.getSlot() + (shortcut.getPage() * Shortcuts.MAX_SHORTCUTS_PER_BAR);
					if (!positions.contains(position))
					{
						positions.add(position);
					}
				}
			}
		}
		
		getVariables().setIntegerList(PlayerVariables.AUTO_USE_SHORTCUTS, positions);
	}
	
	/**
	 * Removes the auto-use status from a specific shortcut.<br>
	 * This method updates the player variables and sends a packet to the client.
	 * @param slot The index of the shortcut slot.
	 * @param page The page number where the shortcut is located.
	 */
	public synchronized void removeAutoShortcut(int slot, int page)
	{
		if (!getVariables().contains(PlayerVariables.AUTO_USE_SHORTCUTS))
		{
			return;
		}
		
		final List<Integer> positions = getVariables().getIntegerList(PlayerVariables.AUTO_USE_SHORTCUTS);
		final Shortcut usedShortcut = getShortcut(slot, page);
		if (usedShortcut == null)
		{
			final Integer position = slot + (page * Shortcuts.MAX_SHORTCUTS_PER_BAR);
			positions.remove(position);
		}
		else
		{
			for (Shortcut shortcut : getAllShortcuts())
			{
				if ((usedShortcut.getId() == shortcut.getId()) && (usedShortcut.getType() == shortcut.getType()))
				{
					shortcut.setAutoUse(false);
					sendPacket(new ExActivateAutoShortcut(shortcut, false));
					final Integer position = shortcut.getSlot() + (shortcut.getPage() * Shortcuts.MAX_SHORTCUTS_PER_BAR);
					positions.remove(position);
				}
			}
		}
		
		getVariables().setIntegerList(PlayerVariables.AUTO_USE_SHORTCUTS, positions);
	}
	
	/**
	 * Checks if the player is currently in a timed hunting zone.<br>
	 * This method retrieves the last hunting zone ID from {@code PlayerVariables}.<br>
	 * It returns {@code true} if the ID is greater than {@code 0}.
	 * @return {@code true} if the player is in a valid hunting zone, {@code false} otherwise.
	 */
	public boolean isInTimedHuntingZone()
	{
		return getVariables().getInt(PlayerVariables.LAST_HUNTING_ZONE_ID, 0) > 0;
	}
	
	/**
	 * Checks if the player is currently in a timed hunting zone.<br>
	 * It compares the provided {@code zoneId} with the last recorded hunting zone.
	 * @param zoneId The unique identifier of the zone to check.
	 * @return {@code true} if the zone matches, otherwise {@code false}.
	 */
	public boolean isInTimedHuntingZone(int zoneId)
	{
		return getVariables().getInt(PlayerVariables.LAST_HUNTING_ZONE_ID, 0) == zoneId;
	}
	
	/**
	 * Retrieves the hunting zone data based on the player's last visited zone.<br>
	 * It looks up the ID from {@code PlayerVariables}.
	 * @return the {@code TimedHuntingZoneHolder} for the current zone or {@code null} if not found.
	 */
	public TimedHuntingZoneHolder getTimedHuntingZone()
	{
		return TimedHuntingZoneData.getInstance().getHuntingZone(getVariables().getInt(PlayerVariables.LAST_HUNTING_ZONE_ID, 0));
	}
	
	/**
	 * Starts a recurring task to manage the time for a specific hunting zone.<br>
	 * This method stops any existing task before scheduling a new one.<br>
	 * It handles countdown messages and teleports the player when time expires.
	 * @param zoneId The unique identifier of the hunting zone.
	 */
	public void startTimedHuntingZone(int zoneId)
	{
		// Stop previous task.
		stopTimedHuntingZoneTask();
		
		_timedHuntingZoneTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			if (isInTimedHuntingZone(zoneId))
			{
				final long time = getTimedHuntingZoneRemainingTime(zoneId);
				if (time > 0)
				{
					if (time < 300000)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.THE_TIME_FOR_HUNTING_IN_THIS_ZONE_EXPIRES_IN_S1_MIN_PLEASE_ADD_MORE_TIME);
						sm.addLong(time / 60000);
						sendPacket(sm);
					}
					
					getVariables().set(PlayerVariables.HUNTING_ZONE_TIME + zoneId, time - 60000);
				}
				else
				{
					if (_timedHuntingZoneTask != null)
					{
						_timedHuntingZoneTask.cancel(false);
						_timedHuntingZoneTask = null;
					}
					
					abortCast();
					stopMove(null);
					teleToLocation(MapRegionData.getInstance().getTeleToLocation(this, TeleportWhereType.TOWN));
					sendPacket(SystemMessageId.THE_HUNTING_ZONE_S_USE_TIME_HAS_EXPIRED_SO_YOU_WERE_MOVED_OUTSIDE);
					setInstance(null);
				}
			}
		}, 60000, 60000);
	}
	
	/**
	 * Stops the active timed hunting zone task.<br>
	 * This method checks if {@code _timedHuntingZoneTask} is currently running.<br>
	 * If it is active, it cancels the task and sets the reference to {@code null}.
	 */
	public void stopTimedHuntingZoneTask()
	{
		if ((_timedHuntingZoneTask != null) && !_timedHuntingZoneTask.isCancelled() && !_timedHuntingZoneTask.isDone())
		{
			_timedHuntingZoneTask.cancel(true);
			_timedHuntingZoneTask = null;
		}
	}
	
	/**
	 * Retrieves the remaining time for a specific hunting zone.<br>
	 * This method checks the player variables to find the duration.<br>
	 * It ensures that the returned value is never less than {@code 0}.
	 * @param zoneId The unique identifier for the hunting zone.
	 * @return The number of seconds remaining in the zone.
	 */
	public int getTimedHuntingZoneRemainingTime(int zoneId)
	{
		return Math.max(getVariables().getInt(PlayerVariables.HUNTING_ZONE_TIME + zoneId, 0), 0);
	}
	
	/**
	 * Retrieves the initial entry time for a specific timed hunting zone.<br>
	 * This method looks up the value associated with the {@code zoneId}.<br>
	 * It ensures that the returned value is never less than {@code 0}.
	 * @param zoneId The unique identifier of the hunting zone.
	 * @return The entry time as a {@code long} value.
	 */
	public long getTimedHuntingZoneInitialEntry(int zoneId)
	{
		return Math.max(getVariables().getLong(PlayerVariables.HUNTING_ZONE_ENTRY + zoneId, 0), 0);
	}
	
	/**
	 * Retrieves the list of homunculi associated with this player.
	 * @return a {@code HomunculusList} containing all current homunculi.
	 */
	public HomunculusList getHomunculusList()
	{
		return _homunculusList;
	}
	
	/**
	 * Calculates the number of available homunculus slots for the player.<br>
	 * It compares the maximum allowed count against the current opened count.
	 * @return The total number of remaining homunculus slots.
	 */
	public int getAvailableHomunculusSlotCount()
	{
		return Math.min(PlayerConfig.MAX_HOMUNCULUS_COUNT, getVariables().getInt(PlayerVariables.HOMUNCULUS_OPENED_SLOT_COUNT, PlayerConfig.START_HOMUNCULUS_COUNT));
	}
	
	/**
	 * Calculates and updates the number of opened homunculus slots.<br>
	 * This method checks the available slot count and current list size.<br>
	 * It then updates {@code PlayerVariables.HOMUNCULUS_OPENED_SLOT_COUNT} based on specific logic.
	 */
	public void calculateHomunculusSlots()
	{
		final int slotCount = getAvailableHomunculusSlotCount();
		if ((slotCount == 0) || (slotCount == 1) || (slotCount == 2))
		{
			final int listSize = _homunculusList.size();
			if ((listSize != 0) && (listSize < 2))
			{
				if (getVariables().getInt(PlayerVariables.HOMUNCULUS_CREATION_TIME, 0) >= 0)
				{
					getVariables().set(PlayerVariables.HOMUNCULUS_OPENED_SLOT_COUNT, listSize + 1);
				}
				else
				{
					getVariables().set(PlayerVariables.HOMUNCULUS_OPENED_SLOT_COUNT, listSize);
				}
			}
			else
			{
				getVariables().set(PlayerVariables.HOMUNCULUS_OPENED_SLOT_COUNT, 3);
			}
		}
	}
	
	/**
	 * Retrieves the {@code CollectionList} of this player.
	 * @return the {@code CollectionList} of this player.
	 */
	public CollectionList getCollectionList()
	{
		return _collections;
	}
	
	/**
	 * Retrieves the current PvP streak of the player.<br>
	 * This value is stored in the {@code PlayerVariables} collection.<br>
	 * It returns a default value of {@code 0} if no streak exists.
	 * @return The current integer value of the PvP streak.
	 */
	public int getPvpStreak()
	{
		return getVariables().getInt(PlayerVariables.PVP_STREAK, 0);
	}
	
	/**
	 * Increases the player's current PvP streak by 1.<br>
	 * This method updates the {@code PVP_STREAK} variable in the player's variables map.
	 */
	public void increasePvpStreak()
	{
		getVariables().set(PlayerVariables.PVP_STREAK, getPvpStreak() + 1);
	}
	
	/**
	 * Removes the current {@code PVP_STREAK} from the player's variables.<br>
	 * This resets the count of consecutive PvP victories for the character.
	 */
	public void removePvpStreak()
	{
		getVariables().remove(PlayerVariables.PVP_STREAK);
	}
	
	/**
	 * Retrieves the {@code CrossEventProgress} of this player.
	 * @return the {@code CrossEventProgress} of this player.
	 */
	public CrossEventProgress getCrossEventProgress()
	{
		return _crossEventProgress;
	}
	
	/**
	 * Retrieves the current hero book progress for the player.<br>
	 * This method initializes the data if it is currently {@code null}.<br>
	 * It also applies any level effects to the character.
	 * @return a {@code HeroBookInfoHolder} containing the player's progress.
	 */
	public HeroBookInfoHolder getHeroBookProgress()
	{
		if (_heroBookInfo == null)
		{
			_heroBookInfo = HeroBookManager.getCurrentPlayerProgress(this);
			HeroBookManager.applyLevelEffects(this);
		}
		
		return _heroBookInfo;
	}
	
	/**
	 * Retrieves the progress of a hero book for a specific category.<br>
	 * This method calls {@code HeroBookManager.getCurrentPlayerProgress}.
	 * @param category The unique identifier for the hero book category.
	 * @return A {@code HeroBookInfoHolder} containing the player's current progress.
	 */
	public HeroBookInfoHolder getHeroBookProgress(byte category)
	{
		return HeroBookManager.getCurrentPlayerProgress(this, category);
	}
	
	/**
	 * Updates the progress of a hero book for a specific category.<br>
	 * This method saves the current player's progress to the database.
	 * @param holder The {@code HeroBookInfoHolder} containing the book data.
	 * @param category The integer ID representing the book category.
	 */
	public void updateHeroBookProgress(HeroBookInfoHolder holder, int category)
	{
		HeroBookManager.saveCurrentPlayerProgress(this, holder, category);
	}
	
	/**
	 * Updates the internal hero book information.<br>
	 * This method sets the {@code _heroBookInfo} field to the provided value.
	 * @param holder The {@code HeroBookInfoHolder} object containing the new data.
	 */
	public void updateHeroBookProgress(HeroBookInfoHolder holder)
	{
		_heroBookInfo = holder;
	}
	
	/**
	 * Retrieves the {@code RelicList} of this player.
	 * @return the {@code RelicList} of this player.
	 */
	public RelicList getRelicList()
	{
		return _relics;
	}
}
