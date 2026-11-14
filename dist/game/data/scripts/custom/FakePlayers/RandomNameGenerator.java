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

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;

/**
 * Builds a random, thematically fitting character name from a {@link Race}, a mage flag and a sex flag.<br>
 * A name is assembled as {@code prefix + middle + suffix}, where the prefix pool is chosen by race, the middle pool by the mage flag and the suffix pool by the sex flag.<br>
 * Every generated name is stored so the same name is never handed out twice; call {@link #release(String)} when a name is no longer in use to make it available again.
 * @author Mobius
 */
public class RandomNameGenerator
{
	// @formatter:off
	// Race flavored name starts.
	private static final Map<Race, String[]> RACE_PREFIXES = new EnumMap<>(Race.class);
	static
	{
		RACE_PREFIXES.put(Race.HUMAN, new String[]{"Al", "Bran", "Cor", "Dun", "El", "Gar", "Hal", "Mar", "Ric", "Ser", "Ton", "Wil"});
		RACE_PREFIXES.put(Race.ELF, new String[]{"Ael", "Cael", "Elu", "Fael", "Gal", "Ith", "Lae", "Mith", "Syl", "Thal", "Vaen"});
		RACE_PREFIXES.put(Race.DARK_ELF, new String[]{"Dro", "Kar", "Mor", "Nyx", "Sha", "Vel", "Xar", "Zeph", "Grim", "Nok"});
		RACE_PREFIXES.put(Race.ORC, new String[]{"Bruk", "Dro", "Gor", "Grok", "Krul", "Mog", "Nar", "Rok", "Thok", "Ugra", "Zug"});
		RACE_PREFIXES.put(Race.DWARF, new String[]{"Bal", "Dor", "Dur", "Gim", "Grom", "Kaz", "Mor", "Nal", "Thra", "Bof"});
		RACE_PREFIXES.put(Race.KAMAEL, new String[]{"Ash", "Cyr", "Dae", "Kal", "Ryu", "Sae", "Tae", "Vin", "Zar", "Kyo"});
		RACE_PREFIXES.put(Race.ERTHEIA, new String[]{"Aei", "Eli", "Ily", "Lun", "Mio", "Nia", "Rae", "Sei", "Uli", "Yui"});
		RACE_PREFIXES.put(Race.SYLPH, new String[]{"Aer", "Bre", "Cirr", "Fen", "Gale", "Nim", "Sky", "Vent", "Zephy", "Aura"});
		RACE_PREFIXES.put(Race.HIGH_ELF, new String[]{"Aur", "Cael", "Elar", "Hel", "Ith", "Lor", "Sil", "Val", "Aeth", "Cel"});
	}
	
	// Fallback prefixes for any race without a dedicated pool.
	private static final String[] DEFAULT_PREFIXES = {"Ander", "Bel", "Cael", "Dun", "Eryn", "Fael", "Gor", "Hal", "Kel", "Var"};
	
	// Mystical connectors for mage classes.
	private static final String[] MAGE_MIDDLES = {"a", "e", "ith", "yr", "ael", "wyn", "ora", "une", "is", "el"};
	
	// Martial connectors for fighter classes.
	private static final String[] FIGHTER_MIDDLES = {"ar", "or", "gan", "dric", "mund", "gar", "ric", "don", "vak", "bek"};
	
	// Soft endings for female names.
	private static final String[] FEMALE_SUFFIXES = {"a", "ia", "wen", "elle", "ara", "yn", "is", "na", "lith", "ea"};
	
	// Hard endings for male names.
	private static final String[] MALE_SUFFIXES = {"us", "or", "an", "ric", "dor", "in", "ok", "ar", "gar", "th"};
	// @formatter:on
	
	// Names already handed out, so the same one is never generated twice.
	private static final Set<String> GENERATED_NAMES = ConcurrentHashMap.newKeySet();
	
	// Random tries before falling back to a numeric suffix when a pool is nearly exhausted.
	private static final int MAX_RANDOM_TRIES = 100;
	
	/**
	 * Generates a unique random name for the given race, mage flag and sex.
	 * @param race the race that selects the prefix pool
	 * @param isMage {@code true} to use mystical middles, {@code false} for martial ones
	 * @param female {@code true} for a female name, {@code false} for a male name
	 * @return a capitalized random name that has not been generated before
	 */
	public static String generateName(Race race, boolean isMage, boolean female)
	{
		final String[] prefixes = RACE_PREFIXES.getOrDefault(race, DEFAULT_PREFIXES);
		final String[] middles = isMage ? MAGE_MIDDLES : FIGHTER_MIDDLES;
		final String[] suffixes = female ? FEMALE_SUFFIXES : MALE_SUFFIXES;
		String name = null;
		for (int i = 0; i < MAX_RANDOM_TRIES; i++)
		{
			final String raw = prefixes[Rnd.get(prefixes.length)] + middles[Rnd.get(middles.length)] + suffixes[Rnd.get(suffixes.length)];
			name = raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
			if (GENERATED_NAMES.add(name))
			{
				return name;
			}
		}
		
		// Pool nearly exhausted, append an incrementing number until unique.
		int counter = 2;
		String candidate;
		do
		{
			candidate = name + counter++;
		}
		while (!GENERATED_NAMES.add(candidate));
		return candidate;
	}
	
	/**
	 * Generates a unique random name using the race and mage flag of a {@link PlayerClass}.
	 * @param playerClass the class whose race and mage flag drive the name
	 * @param female {@code true} for a female name, {@code false} for a male name
	 * @return a capitalized random name that has not been generated before
	 */
	public static String generateName(PlayerClass playerClass, boolean female)
	{
		return generateName(playerClass.getRace(), playerClass.isMage(), female);
	}
	
	/**
	 * Generates a unique random name for the given race and mage flag, with a randomly chosen sex.
	 * @param race the race that selects the prefix pool
	 * @param isMage {@code true} to use mystical middles, {@code false} for martial ones
	 * @return a capitalized random name that has not been generated before
	 */
	public static String generateName(Race race, boolean isMage)
	{
		return generateName(race, isMage, Rnd.nextBoolean());
	}
	
	/**
	 * Releases a previously generated name so it can be handed out again.
	 * @param name the name to free
	 * @return {@code true} if the name was stored and is now available again
	 */
	public static boolean release(String name)
	{
		return GENERATED_NAMES.remove(name);
	}
}
