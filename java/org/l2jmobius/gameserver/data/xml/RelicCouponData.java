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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.holders.RelicCouponHolder;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;

/**
 * This class handles the loading and storage of relic coupon data from XML files.<br>
 * It provides the necessary information for creating {@code RelicCouponHolder} objects.
 * @author Brado
 */
public class RelicCouponData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RelicCouponData.class.getName());
	
	private static final Map<Integer, RelicCouponHolder> RELIC_COUPONS = new HashMap<>();
	private static final Map<Integer, LinkedHashMap<Integer, Long>> CACHED_CHANCES = new LinkedHashMap<>();
	
	/**
	 * This is the default constructor for {@code RelicCouponData}.<br>
	 * It initializes the data by calling the {@code load()} method.
	 */
	protected RelicCouponData()
	{
		load();
	}
	
	/**
	 * Loads the relic coupon data from the XML file.<br>
	 * This method clears existing data before parsing the new values.<br>
	 * It logs a message if any coupons are successfully loaded.
	 */
	@Override
	public void load()
	{
		RELIC_COUPONS.clear();
		parseDatapackFile("data/RelicCouponData.xml");
		cacheChances();
		if (!RELIC_COUPONS.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + RELIC_COUPONS.size() + " relic coupon data.");
		}
	}
	
	/**
	 * Parses a {@code Document} to load relic coupon data.<br>
	 * This method reads the XML structure and populates the internal map.<br>
	 * It validates that each item ID exists in the system.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "coupon", couponNode ->
		{
			final Element couponElement = (Element) couponNode;
			final int itemId = Integer.parseInt(couponElement.getAttribute("itemId"));
			if (ItemData.getInstance().getTemplate(itemId) == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Could not find coupon with item id " + itemId + ".");
				return;
			}
			
			final int summonCount = couponElement.hasAttribute("summonCount") ? Integer.parseInt(couponElement.getAttribute("summonCount")) : 1;
			if (couponElement.hasAttribute("relicId"))
			{
				final int relicId = Integer.parseInt(couponElement.getAttribute("relicId"));
				RELIC_COUPONS.put(itemId, new RelicCouponHolder(itemId, relicId, summonCount));
			}
			else
			{
				final Map<RelicGrade, Integer> groups = new HashMap<>();
				final Set<Integer> disabledRelicIds = new HashSet<>();
				final Map<Integer, Integer> chanceRolls = new HashMap<>();
				
				// Parse <disabledRelics> (renamed from <disabledDolls> to match the XML).
				forEach(couponNode, "disabledRelics", disabledRelicsNode ->
				{
					forEach(disabledRelicsNode, "disabled", disabledNode ->
					{
						final int id = Integer.parseInt(((Element) disabledNode).getAttribute("id"));
						disabledRelicIds.add(id);
					});
				});
				
				forEach(couponNode, "chanceGroups", groupChanceNode ->
				{
					forEach(groupChanceNode, "group", groupNode ->
					{
						final Element groupElement = (Element) groupNode;
						final RelicGrade grade = RelicGrade.valueOf(groupElement.getAttribute("grade"));
						final int chance = Integer.parseInt(groupElement.getAttribute("chance"));
						groups.put(grade, chance);
					});
				});
				
				if (!groups.isEmpty())
				{
					RELIC_COUPONS.put(itemId, new RelicCouponHolder(itemId, summonCount, groups, disabledRelicIds));
				}
				else
				{
					forEach(couponNode, "chanceRollGroups", chanceRollGroupsNode ->
					{
						forEach(chanceRollGroupsNode, "chanceRoll", chanceRollNode ->
						{
							final Element chanceRollElement = (Element) chanceRollNode;
							final int dollId = Integer.parseInt(chanceRollElement.getAttribute("dollId"));
							final int chance = Integer.parseInt(chanceRollElement.getAttribute("chance"));
							chanceRolls.put(dollId, chance);
						});
					});
					
					if (!chanceRolls.isEmpty())
					{
						RELIC_COUPONS.put(itemId, new RelicCouponHolder(itemId, summonCount, chanceRolls));
					}
				}
			}
		}));
	}
	
	/**
	 * Retrieves a {@code RelicCouponHolder} based on the provided item ID.<br>
	 * This method looks up the coupon data in the internal cache.
	 * @param itemId The unique identifier of the coupon item.
	 * @return The {@code RelicCouponHolder} associated with the ID, or {@code null} if not found.
	 */
	public RelicCouponHolder getCouponFromCouponItemId(int itemId)
	{
		return RELIC_COUPONS.get(itemId);
	}
	
	/**
	 * Retrieves the {@code RelicId} associated with a specific coupon item.<br>
	 * This method looks up the coupon in the internal map.<br>
	 * It returns {@code 0} if no valid coupon is found.
	 * @param itemId The unique identifier of the coupon item.
	 * @return The integer ID of the relic or {@code 0} if not found.
	 */
	public int getRelicIdByCouponItemId(int itemId)
	{
		return RELIC_COUPONS.get(itemId) != null ? RELIC_COUPONS.get(itemId).getRelicId() : 0;
	}
	
	/**
	 * Determines the relic ID for a given coupon.<br>
	 * It first checks if a specific relic is already assigned to the {@code RelicCouponHolder}.<br>
	 * If not, it performs weighted rolls based on grades or chance rolls.
	 * @param coupon The {@code RelicCouponHolder} object containing the coupon data.
	 * @return The resulting relic ID as an {@code int}, or 0 if no relic can be determined.
	 */
	private int getRelicIdFromSummon(RelicCouponHolder coupon)
	{
		final int couponRelicId = coupon.getRelicId();
		if (couponRelicId != 0)
		{
			return couponRelicId;
		}
		
		final Map<RelicGrade, Integer> grades = new HashMap<>();
		grades.putAll(coupon.getCouponRelicGrades());
		if (!grades.isEmpty())
		{
			// Step 1: weighted grade roll using chanceGroups percentages
			int totalGradeChance = 0;
			for (int c : grades.values())
			{
				totalGradeChance += c;
			}
			int gradeRng = Rnd.get(totalGradeChance);
			RelicGrade rolledGrade = null;
			for (Entry<RelicGrade, Integer> entry : grades.entrySet())
			{
				gradeRng -= entry.getValue();
				if (gradeRng < 0)
				{
					rolledGrade = entry.getKey();
					break;
				}
			}
			if (rolledGrade == null)
			{
				rolledGrade = grades.keySet().iterator().next();
			}
			
			// Step 2: weighted relic roll within the rolled grade using summonChance
			final List<RelicDataHolder> relicsByGrade = RelicData.getInstance().getRelicsByGrade(rolledGrade).stream().filter(r -> !coupon.getDisabledIds().contains(r.getRelicId())).toList();
			final long totalWeight = relicsByGrade.stream().mapToLong(RelicDataHolder::getSummonChance).sum();
			if (totalWeight <= 0)
			{
				LOGGER.warning("No valid relics available for summoning with coupon " + coupon.getItemId());
				return 0;
			}
			
			long relicRng = Rnd.get(totalWeight);
			for (RelicDataHolder relic : relicsByGrade)
			{
				relicRng -= relic.getSummonChance();
				if (relicRng < 0)
				{
					return relic.getRelicId();
				}
			}
			return relicsByGrade.get(relicsByGrade.size() - 1).getRelicId();
		}
		
		final Map<Integer, Integer> chanceRolls = coupon.getChanceRolls();
		if (!chanceRolls.isEmpty())
		{
			int totalRollChance = 0;
			for (int c : chanceRolls.values())
			{
				totalRollChance += c;
			}
			int rollRng = Rnd.get(totalRollChance);
			for (Entry<Integer, Integer> entry : chanceRolls.entrySet())
			{
				rollRng -= entry.getValue();
				if (rollRng < 0)
				{
					return entry.getKey();
				}
			}
			return chanceRolls.keySet().iterator().next();
		}
		
		return 0;
	}
	
	/**
	 * Clears and repopulates the {@code CACHED_CHANCES} map.<br>
	 * This method calculates summon chances for all items in {@code RELIC_COUPONS}.<br>
	 * It processes relic grades, weights, and custom rolls to determine final probabilities.
	 */
	private void cacheChances()
	{
		CACHED_CHANCES.clear();
		for (RelicCouponHolder holder : RELIC_COUPONS.values())
		{
			final Map<Integer, Long> results = new HashMap<>();
			final int holderRelicId = holder.getRelicId();
			if (holderRelicId != 0)
			{
				results.put(holderRelicId, 10_000_000_000L);
			}
			
			final Map<RelicGrade, Integer> grades = new HashMap<>();
			grades.putAll(holder.getCouponRelicGrades());
			if (!grades.isEmpty())
			{
				for (Entry<RelicGrade, Integer> entry : grades.entrySet())
				{
					final List<RelicDataHolder> gradePool = RelicData.getInstance().getRelicsByGrade(entry.getKey()).stream().filter(r -> !holder.getDisabledIds().contains(r.getRelicId())).toList();
					final long totalWeight = gradePool.stream().mapToLong(RelicDataHolder::getSummonChance).sum();
					if (totalWeight <= 0)
					{
						continue;
					}
					for (RelicDataHolder rh : gradePool)
					{
						// Divide before multiplying to avoid long overflow:
						// gradeChance * (sc * 10^10 / totalWeight) / 100
						final long chance = (entry.getValue() * ((rh.getSummonChance() * 10_000_000_000L) / totalWeight)) / 100L;
						results.put(rh.getRelicId(), chance);
					}
				}
			}
			
			final Map<Integer, Integer> holderChanceRolls = holder.getChanceRolls();
			if (!holderChanceRolls.isEmpty())
			{
				for (Entry<Integer, Integer> entry : holderChanceRolls.entrySet())
				{
					results.put(entry.getKey(), entry.getValue() * 10_000_000L);
				}
			}
			
			final LinkedHashMap<Integer, Long> sortedRelics = results.entrySet().stream().sorted(Map.Entry.<Integer, Long> comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, _) -> e1, LinkedHashMap::new));
			CACHED_CHANCES.put(holder.getItemId(), sortedRelics);
		}
	}
	
	/**
	 * Retrieves the cached chances for a specific item.<br>
	 * This method looks up the data in the internal cache.
	 * @param itemId The unique identifier of the item to look up.
	 * @return A {@code Map<Integer, Long>} containing the chances or {@code null} if not found.
	 */
	public Map<Integer, Long> getCachedChances(int itemId)
	{
		return CACHED_CHANCES.get(itemId);
	}
	
	/**
	 * Generates a list of relic IDs based on a coupon.<br>
	 * It determines the number of relics to summon from the {@code RelicCouponHolder}.<br>
	 * Each relic is selected using the internal summoning logic.
	 * @param coupon The {@code RelicCouponHolder} containing the summon rules.
	 * @return A {@code List} of integers representing the obtained relic IDs.
	 */
	public List<Integer> generateSummonRelics(RelicCouponHolder coupon)
	{
		final List<Integer> relics = new ArrayList<>();
		for (int i = 1; i <= coupon.getRelicSummonCount(); i++)
		{
			final int obtainedRelicId = getRelicIdFromSummon(coupon);
			relics.add(obtainedRelicId);
		}
		return relics;
	}
	
	/**
	 * Provides access to the singleton instance of {@code RelicCouponData}.<br>
	 * Use this method to retrieve the global data holder.
	 * @return The single instance of {@code RelicCouponData}.
	 */
	public static RelicCouponData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RelicCouponData INSTANCE = new RelicCouponData();
	}
}
