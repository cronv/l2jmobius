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
package ai.bosses.Valakas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.npc.DropGroupHolder;
import org.l2jmobius.gameserver.entity.actor.holders.npc.DropHolder;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Manages the loot distribution logic for the {@code Valakas} boss.<br>
 * This class handles specific auto-loot rules for players on a {@code CommandChannel}.
 * @author Notorion
 */
public class ValakasLootManager extends Script
{
	private static final Logger LOGGER = Logger.getLogger(ValakasLootManager.class.getName());
	
	private static final int VALAKAS_ID = 29415;
	
	// CONFIGURATION: Minimum number of players in the CC required to be eligible for the drop, 30 is a safety margin.
	private static final int MIN_CC_MEMBERS = 30;
	
	private static final List<DropGroupHolder> _storedDrops = new ArrayList<>();
	private static boolean _dropsLoaded = false;
	
	private final Map<Integer, Long> _damageMap = new ConcurrentHashMap<>();
	
	/**
	 * Initializes the {@code ValakasLootManager} script.<br>
	 * Registers necessary IDs for attack, kill, and spawn events.<br>
	 * Loads drop data and scans for existing bosses.
	 */
	public ValakasLootManager()
	{
		addAttackId(VALAKAS_ID);
		addKillId(VALAKAS_ID);
		addSpawnId(VALAKAS_ID);
		
		loadAndHijackDrops();
		scanForExistingValakas();
		LOGGER.info("ValakasLootManager: Loaded. Target: " + VALAKAS_ID);
	}
	
	/**
	 * Searches the world for an active Valakas boss.<br>
	 * If a living boss is found, it triggers the drop hijacking logic.
	 */
	private void scanForExistingValakas()
	{
		for (WorldObject obj : World.getVisibleObjects())
		{
			if ((obj != null) && obj.isNpc() && (obj.getId() == VALAKAS_ID) && !((Npc) obj).isDead())
			{
				loadAndHijackDrops();
				return;
			}
		}
	}
	
	/**
	 * This method is called when a new {@code Npc} enters the world.<br>
	 * It initializes the loot manager and clears previous damage data.
	 * @param npc The {@code Npc} instance that has just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		loadAndHijackDrops();
		_damageMap.clear();
	}
	
	/**
	 * Loads the loot data for the Valakas boss.<br>
	 * It captures the drops from the {@code NpcTemplate} and removes them from the game world.<br>
	 * This ensures that items are handled by this manager instead of dropping on the ground.
	 */
	private void loadAndHijackDrops()
	{
		if (_dropsLoaded && !_storedDrops.isEmpty())
		{
			return;
		}
		
		final NpcTemplate template = NpcData.getInstance().getTemplate(VALAKAS_ID);
		if (template == null)
		{
			LOGGER.warning("ValakasLootManager: Template " + VALAKAS_ID + " not found.");
			return;
		}
		
		final List<DropGroupHolder> originalDrops = template.getDropGroups();
		if ((originalDrops != null) && !originalDrops.isEmpty())
		{
			synchronized (_storedDrops)
			{
				_storedDrops.clear();
				_storedDrops.addAll(originalDrops);
				_dropsLoaded = true;
			}
			
			// Drop removed from XML so the server does not drop it on the ground.
			template.removeDropGroups();
			LOGGER.info("ValakasLootManager: Drops successfully captured.");
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * It updates the total damage dealt by the attacker.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt in this instance.
	 * @param isSummon Indicates if the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (attacker == null)
		{
			return;
		}
		
		_damageMap.merge(attacker.getObjectId(), (long) damage, Long::sum);
	}
	
	/**
	 * Handles the logic when an {@code Npc} is killed.<br>
	 * This method determines the winner based on damage ranking and Command Channel requirements.<br>
	 * It rewards the leader of a valid Command Channel with the loot.
	 * @param npc The {@code Npc} that was defeated.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (_damageMap.isEmpty())
		{
			return;
		}
		
		// 1. Get Damage Ranking (Top -> Bottom).
		final List<Player> ranking = calculateRanking();
		
		Player validWinner = null;
		
		// 2. Command Channel (CC) Filter.
		for (Player candidate : ranking)
		{
			Party party = candidate.getParty();
			
			// Check if there is a Party and if there is a Command Channel.
			if ((party != null) && (party.getCommandChannel() != null))
			{
				final CommandChannel cc = party.getCommandChannel();
				
				// Check Minimum Size of the CC.
				final int memberCount = cc.getMemberCount();
				if (memberCount >= MIN_CC_MEMBERS)
				{
					validWinner = candidate; // Valid Winner.
					break;
				}
				
				// LOGGER.info("Leader's CC " + candidate.getName() + " ignored. Members: " + memberCount + " (Minimum: " + MIN_CC_MEMBERS + ")");
			}
		}
		
		if (validWinner != null)
		{
			final CommandChannel cc = validWinner.getParty().getCommandChannel();
			final Player ccLeader = cc.getLeader(); // The drop always goes to the CC Leader.
			
			final String ccLeaderName = ccLeader.getName();
			LOGGER.info("Valakas auto-loot: " + ccLeaderName + " (Membros: " + cc.getMemberCount() + ")");
			
			// 3. Official Message on Screen.
			sendScreenMessage(npc, ccLeaderName);
			
			// 4. Assign items directly to the Command Channel (CC) Leader's inventory.
			giveAutoLootDrops(npc, ccLeader);
		}
		else
		{
			// LOGGER.info("No valid Command Channel (CC) with " + MIN_CC_MEMBERS + "+ members inflicted sufficient damage.");
		}
		
		_damageMap.clear();
	}
	
	/**
	 * Sends a screen message to players who participated in the fight.<br>
	 * It broadcasts that the winner's command channel has looting rights.<br>
	 * The message is sent to all damage dealers and nearby spectators.
	 * @param npc The {@code Npc} instance used to determine nearby players.
	 * @param winnerName The name of the player who won the fight.
	 */
	private void sendScreenMessage(Npc npc, String winnerName)
	{
		// Message: "# $s1's Command Channel has looting rights.".
		ExShowScreenMessage screenMsg = new ExShowScreenMessage(NpcStringId.S1_S_COMMAND_CHANNEL_HAS_LOOTING_RIGHTS, ExShowScreenMessage.TOP_CENTER, 10000, true, winnerName);
		
		// Broadcast message to all players who inflicted damage.
		for (Integer playerId : _damageMap.keySet())
		{
			final Player p = World.getPlayer(playerId);
			if ((p != null) && p.isOnline())
			{
				p.sendPacket(screenMsg);
			}
		}
		
		// Broadcast to nearby spectators.
		if (npc != null)
		{
			World.forEachVisibleObject(npc, Player.class, p ->
			{
				if (!_damageMap.containsKey(p.getObjectId()))
				{
					p.sendPacket(screenMsg);
				}
			});
		}
	}
	
	/**
	 * Distributes loot items from a defeated {@code Npc} to the specified {@code Player}.<br>
	 * This method checks all stored drop groups and calculates probabilities for each item.<br>
	 * Items are added directly to the owner's inventory if they pass the random chance check.
	 * @param npc The {@code Npc} that was killed and contained the loot.
	 * @param owner The {@code Player} who will receive the auto-loot items.
	 */
	private void giveAutoLootDrops(Npc npc, Player owner)
	{
		if (_storedDrops.isEmpty())
		{
			return;
		}
		
		int totalGiven = 0;
		for (DropGroupHolder group : _storedDrops)
		{
			// Probability of drop assigned to the group.
			final double groupChance = group.getChance();
			if ((groupChance > 0) && (Rnd.get(100.0) > groupChance))
			{
				continue;
			}
			
			for (DropHolder drop : group.getDropList())
			{
				// Probability of item drop.
				if (Rnd.get(100.0) < drop.getChance())
				{
					long count = Rnd.get(drop.getMin(), drop.getMax());
					
					// Assign directly to player’s inventory.
					owner.addItem(ItemProcessType.LOOT, drop.getItemId(), count, npc, true);
					totalGiven++;
				}
			}
		}
		
		LOGGER.info("Items successfully assigned to Command Channel (CC) Leader (" + owner.getName() + "): " + totalGiven);
	}
	
	/**
	 * Calculates the ranking of players based on total group damage.<br>
	 * It aggregates damage for each party and identifies the ultimate leader.<br>
	 * The resulting list is sorted from highest to lowest damage.
	 * @return A {@code List} of {@code Player} objects ranked by their group's total damage.
	 */
	private List<Player> calculateRanking()
	{
		final Map<Integer, Long> groupDamage = new ConcurrentHashMap<>();
		final Map<Integer, Player> leaderObjects = new ConcurrentHashMap<>();
		
		// Aggregate damage per unit, defined by Party Leader.
		for (Entry<Integer, Long> entry : _damageMap.entrySet())
		{
			final Player player = World.getPlayer(entry.getKey());
			if ((player == null) || !player.isOnline())
			{
				continue;
			}
			
			final Player leader = getUltimateLeader(player);
			final int objectId = leader.getObjectId();
			groupDamage.merge(objectId, entry.getValue(), Long::sum);
			leaderObjects.putIfAbsent(objectId, leader);
		}
		
		if (groupDamage.isEmpty())
		{
			return new ArrayList<>();
		}
		
		final List<Entry<Integer, Long>> sortedGroups = new ArrayList<>(groupDamage.entrySet());
		sortedGroups.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
		
		final List<Player> rankedLeaders = new ArrayList<>();
		for (Entry<Integer, Long> entry : sortedGroups)
		{
			rankedLeaders.add(leaderObjects.get(entry.getKey()));
		}
		
		return rankedLeaders;
	}
	
	/**
	 * Finds the leader of a {@code Player} to aggregate damage.<br>
	 * Returns the party leader if the {@code Player} is in a party.<br>
	 * Otherwise, it returns the {@code Player} themselves.
	 * @param player The {@code Player} to check for leadership.
	 * @return The leader of the party or the {@code Player} if no party exists.
	 */
	private Player getUltimateLeader(Player player)
	{
		// Retrieve Party Leader to aggregate damage.
		final Party party = player.getParty();
		if (party != null)
		{
			return party.getLeader();
		}
		
		return player;
	}
	
	/**
	 * This is the entry point for the {@code ValakasLootManager} class.<br>
	 * It initializes a new instance of the manager to start the loot system.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new ValakasLootManager();
	}
}
