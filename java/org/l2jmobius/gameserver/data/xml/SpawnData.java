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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.holders.npc.ChanceLocation;
import org.l2jmobius.gameserver.entity.actor.holders.npc.MinionHolder;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.spawns.SpawnGroup;
import org.l2jmobius.gameserver.entity.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.entity.zone.ZoneForm;
import org.l2jmobius.gameserver.entity.zone.form.ZoneCuboid;
import org.l2jmobius.gameserver.entity.zone.form.ZoneCylinder;
import org.l2jmobius.gameserver.entity.zone.form.ZoneNPoly;
import org.l2jmobius.gameserver.entity.zone.type.BannedSpawnTerritory;
import org.l2jmobius.gameserver.entity.zone.type.SpawnTerritory;
import org.l2jmobius.gameserver.interfaces.IParameterized;
import org.l2jmobius.gameserver.interfaces.ITerritorized;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and parsing of spawn data from XML files.<br>
 * It provides the necessary logic to initialize {@code SpawnTemplate} objects into the game world.
 * @author UnAfraid, Mobius
 */
public class SpawnData implements IXmlReader
{
	protected static final Logger LOGGER = Logger.getLogger(SpawnData.class.getName());
	
	private static final String OTHER_XML_FOLDER = "data/spawns/Others";
	private static final Pattern COUNT_PATTERN = Pattern.compile("count=\"(\\d+)\"");
	
	private final Collection<SpawnTemplate> _spawnTemplates = ConcurrentHashMap.newKeySet();
	
	/**
	 * Initializes a new instance of the {@code SpawnData} class.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected SpawnData()
	{
		load();
	}
	
	/**
	 * Loads all spawn data from the configuration files.<br>
	 * This method reads the {@code data/spawns} directory.<br>
	 * It populates the internal collection of spawn templates.
	 */
	@Override
	public void load()
	{
		parseDatapackDirectory("data/spawns", true);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _spawnTemplates.stream().flatMap(c -> c.getGroups().stream()).flatMap(c -> c.getSpawns().stream()).count() + " spawns");
	}
	
	/**
	 * Parses the provided {@code Document} to populate the internal spawn templates.<br>
	 * It iterates through all {@code spawn} nodes within the {@code list} element.<br>
	 * Any errors encountered during parsing are logged as warnings.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "spawn", spawnNode ->
		{
			try
			{
				parseSpawn(spawnNode, file, _spawnTemplates);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while processing spawn in file: " + file.getAbsolutePath(), e);
			}
		}));
	}
	
	/**
	 * Parses a spawn node from an XML file and adds it to the collection.<br>
	 * This method creates a new {@code SpawnTemplate} based on the provided node.<br>
	 * It processes nested elements like territories, groups, and NPCs.
	 * @param spawnsNode The {@code Node} containing the spawn data.
	 * @param file The {@code File} where the XML is located.
	 * @param spawns The {@code Collection} of {@code SpawnTemplate} objects to populate.
	 */
	public void parseSpawn(Node spawnsNode, File file, Collection<SpawnTemplate> spawns)
	{
		final SpawnTemplate spawnTemplate = new SpawnTemplate(new StatSet(parseAttributes(spawnsNode)), file);
		SpawnGroup defaultGroup = null;
		for (Node innerNode = spawnsNode.getFirstChild(); innerNode != null; innerNode = innerNode.getNextSibling())
		{
			switch (innerNode.getNodeName())
			{
				case "territories":
				{
					parseTerritories(innerNode, spawnTemplate.getFile(), spawnTemplate);
					break;
				}
				case "group":
				{
					parseGroup(innerNode, spawnTemplate);
					break;
				}
				case "npc":
				{
					if (defaultGroup == null)
					{
						defaultGroup = new SpawnGroup(StatSet.EMPTY_STATSET);
					}
					
					parseNpc(innerNode, spawnTemplate, defaultGroup);
					break;
				}
				case "parameters":
				{
					parseParameters(innerNode, spawnTemplate);
					break;
				}
			}
		}
		
		// Add the default group if it was populated.
		if (defaultGroup != null)
		{
			spawnTemplate.addGroup(defaultGroup);
		}
		
		spawns.add(spawnTemplate);
	}
	
	/**
	 * Parses territory data from an XML node.<br>
	 * This method identifies different shapes like Cuboid, NPoly, and Cylinder.<br>
	 * It adds the resulting territories or banned territories to the provided template.
	 * @param innerNode The {@code Node} containing the territory definitions.
	 * @param file The {@code File} source of the XML data.
	 * @param spawnTemplate The {@code ITerritorized} template to update with new data.
	 */
	private void parseTerritories(Node innerNode, File file, ITerritorized spawnTemplate)
	{
		forEach(innerNode, IXmlReader::isNode, territoryNode ->
		{
			final NamedNodeMap territoryAttrs = territoryNode.getAttributes();
			final String name = parseString(territoryAttrs, "name", file.getName() + "_" + (spawnTemplate.getTerritories().size() + 1));
			final int minZ = parseInteger(territoryAttrs, "minZ");
			final int maxZ = parseInteger(territoryAttrs, "maxZ");
			final List<Integer> xNodes = new ArrayList<>();
			final List<Integer> yNodes = new ArrayList<>();
			
			// Iterate over each "node" element within the territory.
			forEach(territoryNode, "node", node ->
			{
				final NamedNodeMap nodeAttrs = node.getAttributes();
				xNodes.add(parseInteger(nodeAttrs, "x"));
				yNodes.add(parseInteger(nodeAttrs, "y"));
			});
			
			final int[] x = xNodes.stream().mapToInt(Integer::valueOf).toArray();
			final int[] y = yNodes.stream().mapToInt(Integer::valueOf).toArray();
			
			// Determine the ZoneForm based on the "shape" attribute.
			ZoneForm zoneForm = null;
			final String zoneShape = parseString(territoryAttrs, "shape", "NPoly");
			switch (zoneShape)
			{
				case "Cuboid":
				{
					zoneForm = new ZoneCuboid(x[0], x[1], y[0], y[1], minZ, maxZ);
					break;
				}
				case "NPoly":
				{
					zoneForm = new ZoneNPoly(x, y, minZ, maxZ);
					break;
				}
				case "Cylinder":
				{
					final int zoneRad = Integer.parseInt(territoryAttrs.getNamedItem("rad").getNodeValue());
					zoneForm = new ZoneCylinder(x[0], y[0], minZ, maxZ, zoneRad);
					break;
				}
			}
			
			// Add the territory or banned territory to spawnTemplate.
			switch (territoryNode.getNodeName())
			{
				case "territory":
				{
					spawnTemplate.addTerritory(new SpawnTerritory(name, zoneForm));
					break;
				}
				case "banned_territory":
				{
					spawnTemplate.addBannedTerritory(new BannedSpawnTerritory(name, zoneForm));
					break;
				}
			}
		});
	}
	
	/**
	 * Parses a group node from an XML file.<br>
	 * This method creates a new {@code SpawnGroup} based on attributes.<br>
	 * It then processes child nodes to define territories and NPCs.<br>
	 * Finally, it adds the completed group to the provided {@code SpawnTemplate}.
	 * @param n The current XML node representing the group.
	 * @param spawnTemplate The template where the new group will be added.
	 */
	private void parseGroup(Node n, SpawnTemplate spawnTemplate)
	{
		// Initialize SpawnGroup with parsed attributes.
		final SpawnGroup group = new SpawnGroup(new StatSet(parseAttributes(n)));
		
		// Iterate over each child element within the "group" node.
		forEach(n, IXmlReader::isNode, npcNode ->
		{
			switch (npcNode.getNodeName())
			{
				case "territories":
				{
					parseTerritories(npcNode, spawnTemplate.getFile(), group);
					break;
				}
				case "npc":
				{
					parseNpc(npcNode, spawnTemplate, group);
					break;
				}
			}
		});
		
		// Add the group to the spawnTemplate.
		spawnTemplate.addGroup(group);
	}
	
	/**
	 * Parses an NPC node from XML and creates a corresponding {@code NpcSpawnTemplate}.<br>
	 * It validates the NPC ID and type before parsing nested elements like parameters, minions, or locations.<br>
	 * The resulting template is added to the provided {@code SpawnGroup} if all checks pass.
	 * @param n The XML node containing the NPC data.
	 * @param spawnTemplate The parent template associated with this spawn.
	 * @param group The group where the new NPC template will be stored.
	 */
	private void parseNpc(Node n, SpawnTemplate spawnTemplate, SpawnGroup group)
	{
		// Initialize the NpcSpawnTemplate.
		final NpcSpawnTemplate npcTemplate = new NpcSpawnTemplate(spawnTemplate, group, new StatSet(parseAttributes(n)));
		final int npcId = npcTemplate.getId();
		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		
		if (template == null)
		{
			// Log a warning for invalid NPC IDs outside the fake player range [80000, 89999].
			// This hardcoded check provides a resource-constrained solution to prevent unnecessary logging.
			if (FakePlayersConfig.FAKE_PLAYERS_ENABLED || ((npcId < 80000) && (npcId > 89999)))
			{
				LOGGER.warning(getClass().getSimpleName() + ": Requested spawn for non-existing NPC: " + npcId + " in file: " + spawnTemplate.getFile().getName());
			}
			return;
		}
		
		// Validate NPC type.
		if (template.isType("Servitor") || template.isType("Pet"))
		{
			LOGGER.warning(getClass().getSimpleName() + ": Requested spawn for " + template.getType() + " " + template.getName() + "(" + npcId + ") in file: " + spawnTemplate.getFile().getName());
			return;
		}
		
		if (!FakePlayersConfig.FAKE_PLAYERS_ENABLED && template.isFakePlayer())
		{
			return;
		}
		
		// Iterate through child elements of the NPC node.
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			final String childName = d.getNodeName();
			if ("parameters".equalsIgnoreCase(childName))
			{
				parseParameters(d, npcTemplate);
			}
			else if ("minions".equalsIgnoreCase(childName))
			{
				parseMinions(d, npcTemplate);
			}
			else if ("locations".equalsIgnoreCase(childName))
			{
				parseLocations(d, npcTemplate);
			}
		}
		
		// Add NPC template to the group.
		group.addSpawn(npcTemplate);
	}
	
	/**
	 * Parses specific configuration nodes into a {@code StatSet}.<br>
	 * It extracts parameters, skills, locations, and minions from the XML.<br>
	 * The resulting data is then applied to the provided {@code npcTemplate}.
	 * @param node The {@code Node} containing the parameter definitions.
	 * @param npcTemplate The {@code IParameterized} template to receive the parsed parameters.
	 */
	private void parseParameters(Node node, IParameterized<StatSet> npcTemplate)
	{
		final Map<String, Object> parameters = new HashMap<>();
		for (Node parameterNode = node.getFirstChild(); parameterNode != null; parameterNode = parameterNode.getNextSibling())
		{
			NamedNodeMap attributes = parameterNode.getAttributes();
			switch (parameterNode.getNodeName().toLowerCase())
			{
				case "param":
				{
					parameters.put(parseString(attributes, "name"), parseString(attributes, "value"));
					break;
				}
				case "skill":
				{
					parameters.put(parseString(attributes, "name"), new SkillHolder(parseInteger(attributes, "id"), parseInteger(attributes, "level")));
					break;
				}
				case "location":
				{
					parameters.put(parseString(attributes, "name"), new Location(parseInteger(attributes, "x"), parseInteger(attributes, "y"), parseInteger(attributes, "z"), parseInteger(attributes, "heading", 0)));
					break;
				}
				case "minions":
				{
					final List<MinionHolder> minions = new ArrayList<>(1);
					for (Node minionNode = parameterNode.getFirstChild(); minionNode != null; minionNode = minionNode.getNextSibling())
					{
						if (minionNode.getNodeName().equalsIgnoreCase("npc"))
						{
							attributes = minionNode.getAttributes();
							minions.add(new MinionHolder(parseInteger(attributes, "id"), parseInteger(attributes, "count"), parseInteger(attributes, "max", 0), parseInteger(attributes, "respawnTime"), parseInteger(attributes, "weightPoint", 0)));
						}
					}
					
					if (!minions.isEmpty())
					{
						parameters.put(parseString(parameterNode.getAttributes(), "name"), minions);
					}
					break;
				}
			}
		}
		
		// Set the parameters on npcTemplate, or use EMPTY_STATSET if no parameters were parsed.
		npcTemplate.setParameters(!parameters.isEmpty() ? new StatSet(Collections.unmodifiableMap(parameters)) : StatSet.EMPTY_STATSET);
	}
	
	/**
	 * Parses the minion data from an XML node.<br>
	 * This method adds new {@code MinionHolder} objects to the {@code NpcSpawnTemplate}.
	 * @param n The XML {@code Node} containing minion information.
	 * @param npcTemplate The {@code NpcSpawnTemplate} to update with minions.
	 */
	private void parseMinions(Node n, NpcSpawnTemplate npcTemplate)
	{
		forEach(n, "minion", minionNode -> npcTemplate.addMinion(new MinionHolder(new StatSet(parseAttributes(minionNode)))));
	}
	
	/**
	 * Parses location data from an XML node.<br>
	 * It extracts coordinates and chances to create new spawn locations.<br>
	 * These locations are then added to the provided {@code NpcSpawnTemplate}.
	 * @param n The XML {@code Node} containing the location elements.
	 * @param npcTemplate The {@code NpcSpawnTemplate} to store the parsed locations in.
	 */
	private void parseLocations(Node n, NpcSpawnTemplate npcTemplate)
	{
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if ("location".equalsIgnoreCase(d.getNodeName()))
			{
				final NamedNodeMap attrs = d.getAttributes();
				final int x = parseInteger(attrs, "x");
				final int y = parseInteger(attrs, "y");
				final int z = parseInteger(attrs, "z");
				final int heading = parseInteger(attrs, "heading", 0);
				final double chance = parseDouble(attrs, "chance");
				npcTemplate.addSpawnLocation(new ChanceLocation(x, y, z, heading, chance));
			}
		}
	}
	
	/**
	 * Initializes the spawn templates in the game world.<br>
	 * This method checks if spawning is enabled in {@code DevelopmentConfig}.<br>
	 * It activates all templates that are set to spawn by default.<br>
	 * The initialization can be performed synchronously or via a thread pool depending on {@code ThreadConfig}.
	 */
	public void init()
	{
		if (DevelopmentConfig.NO_SPAWNS)
		{
			return;
		}
		
		final String simpleName = getClass().getSimpleName();
		LOGGER.info(simpleName + ": Initializing spawns...");
		if (ThreadConfig.THREADS_FOR_LOADING)
		{
			final Collection<ScheduledFuture<?>> jobs = ConcurrentHashMap.newKeySet();
			for (SpawnTemplate template : _spawnTemplates)
			{
				if (template.isSpawningByDefault())
				{
					jobs.add(ThreadPool.schedule(() ->
					{
						template.spawnAll(null);
						template.notifyActivate();
					}, 0));
				}
			}
			while (!jobs.isEmpty())
			{
				for (ScheduledFuture<?> job : jobs)
				{
					if ((job == null) || job.isDone() || job.isCancelled())
					{
						jobs.remove(job);
					}
				}
			}
		}
		else
		{
			for (SpawnTemplate template : _spawnTemplates)
			{
				if (template.isSpawningByDefault())
				{
					template.spawnAll(null);
					template.notifyActivate();
				}
			}
		}
		
		LOGGER.info(simpleName + ": All spawns has been initialized!");
	}
	
	/**
	 * Removes all active spawns from the current instance.<br>
	 * This method iterates through every {@code SpawnTemplate} in the collection.<br>
	 * It calls the {@code despawnAll} method on each individual template.
	 */
	public void despawnAll()
	{
		final String simpleName = getClass().getSimpleName();
		LOGGER.info(simpleName + ": Removing all spawns...");
		_spawnTemplates.forEach(SpawnTemplate::despawnAll);
		LOGGER.info(simpleName + ": All spawns has been removed!");
	}
	
	/**
	 * Retrieves all loaded spawn templates.
	 * @return a {@code Collection} of {@code SpawnTemplate} objects.
	 */
	public Collection<SpawnTemplate> getSpawns()
	{
		return _spawnTemplates;
	}
	
	/**
	 * Retrieves a list of {@code SpawnTemplate} objects that match a specific condition.<br>
	 * This method filters the internal collection based on the provided {@code Predicate}.
	 * @param condition The filter used to determine which {@code SpawnTemplate} objects to include.
	 * @return A {@code List} containing all templates that satisfy the {@code condition}.
	 */
	public List<SpawnTemplate> getSpawns(Predicate<SpawnTemplate> condition)
	{
		final List<SpawnTemplate> result = new ArrayList<>();
		for (SpawnTemplate spawnTemplate : _spawnTemplates)
		{
			if (condition.test(spawnTemplate))
			{
				result.add(spawnTemplate);
			}
		}
		
		return result;
	}
	
	/**
	 * Finds a {@code SpawnTemplate} based on its unique name.<br>
	 * The search is case-insensitive.<br>
	 * Returns {@code null} if no match is found.
	 * @param name The name of the spawn to look for.
	 * @return The matching {@code SpawnTemplate} or {@code null}.
	 */
	public SpawnTemplate getSpawnByName(String name)
	{
		for (SpawnTemplate spawn : _spawnTemplates)
		{
			final String spawnName = spawn.getName();
			if ((spawnName != null) && spawnName.equalsIgnoreCase(name))
			{
				return spawn;
			}
		}
		
		return null;
	}
	
	/**
	 * Finds a {@code SpawnGroup} based on its name.<br>
	 * This method searches through all loaded templates to find a matching group.<br>
	 * The search is case-insensitive.
	 * @param name The name of the group to look for.
	 * @return The matching {@code SpawnGroup} or {@code null} if not found.
	 */
	public SpawnGroup getSpawnGroupByName(String name)
	{
		for (SpawnTemplate spawnTemplate : _spawnTemplates)
		{
			for (SpawnGroup group : spawnTemplate.getGroups())
			{
				final String groupName = group.getName();
				if ((groupName != null) && groupName.equalsIgnoreCase(name))
				{
					return group;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a list of {@code NpcSpawnTemplate} objects based on a specific filter.<br>
	 * This method iterates through all spawn groups and checks each template against the provided condition.
	 * @param condition A {@code Predicate} used to filter the {@code NpcSpawnTemplate} results.
	 * @return A {@code List} containing all templates that match the given condition.
	 */
	public List<NpcSpawnTemplate> getNpcSpawns(Predicate<NpcSpawnTemplate> condition)
	{
		final List<NpcSpawnTemplate> result = new ArrayList<>();
		for (SpawnTemplate template : _spawnTemplates)
		{
			for (SpawnGroup group : template.getGroups())
			{
				for (NpcSpawnTemplate spawn : group.getSpawns())
				{
					if (condition.test(spawn))
					{
						result.add(spawn);
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Adds a new {@code Spawn} to the global spawn table.<br>
	 * This method also creates or updates an XML file containing the spawn data.<br>
	 * The file is saved based on the coordinates of the spawn location.
	 * @param spawn The {@code Spawn} object to be added and saved.
	 */
	public synchronized void addNewSpawn(Spawn spawn)
	{
		SpawnTable.getInstance().addSpawn(spawn);
		
		// Create output directory if it doesn't exist.
		final File outputDirectory = new File(OTHER_XML_FOLDER);
		if (!outputDirectory.exists())
		{
			boolean result = false;
			try
			{
				outputDirectory.mkdir();
				result = true;
			}
			catch (SecurityException se)
			{
				// Ignore.
			}
			
			if (result)
			{
				LOGGER.info(getClass().getSimpleName() + ": Created directory: " + OTHER_XML_FOLDER);
			}
		}
		
		// XML file for spawn.
		final int spawnXVal = spawn.getX();
		final int spawnYVal = spawn.getY();
		final int x = ((spawnXVal - World.WORLD_X_MIN) >> 15) + World.TILE_X_MIN;
		final int y = ((spawnYVal - World.WORLD_Y_MIN) >> 15) + World.TILE_Y_MIN;
		final File spawnFile = new File(OTHER_XML_FOLDER + "/" + x + "_" + y + ".xml");
		
		// Write info to XML.
		final int spawnIdVal = spawn.getId();
		final int spawnHeadingVal = spawn.getHeading();
		final String spawnId = String.valueOf(spawnIdVal);
		final int amount = spawn.getAmount();
		final String spawnCount = String.valueOf(amount);
		final String spawnX = String.valueOf(spawnXVal);
		final String spawnY = String.valueOf(spawnYVal);
		final String spawnZ = String.valueOf(spawn.getZ());
		final String spawnHeading = String.valueOf(spawnHeadingVal);
		final String spawnDelay = String.valueOf(spawn.getRespawnDelay() / 1000);
		if (spawnFile.exists()) // Update.
		{
			final File tempFile = new File(spawnFile.getAbsolutePath().substring(ServerConfig.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/') + ".tmp");
			try
			{
				try (BufferedReader reader = new BufferedReader(new FileReader(spawnFile));
					BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile)))
				{
					String currentLine;
					while ((currentLine = reader.readLine()) != null)
					{
						if (currentLine.contains("</group>"))
						{
							final NpcTemplate template = NpcData.getInstance().getTemplate(spawnIdVal);
							final String title = template.getTitle();
							final String name = title.isEmpty() ? template.getName() : template.getName() + " - " + title;
							writer.write("			<npc id=\"" + spawnId + (amount > 1 ? "\" count=\"" + spawnCount : "") + "\" x=\"" + spawnX + "\" y=\"" + spawnY + "\" z=\"" + spawnZ + (spawnHeadingVal > 0 ? "\" heading=\"" + spawnHeading : "") + "\" respawnTime=\"" + spawnDelay + "sec\" /> <!-- " + name + " -->" + System.lineSeparator());
							writer.write(currentLine + System.lineSeparator());
							continue;
						}
						
						writer.write(currentLine + System.lineSeparator());
					}
				}
				spawnFile.delete();
				tempFile.renameTo(spawnFile);
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Could not store spawn in the spawn XML files: " + e);
			}
		}
		else // New file.
		{
			try
			{
				final NpcTemplate template = NpcData.getInstance().getTemplate(spawnIdVal);
				final String title = template.getTitle();
				final String name = title.isEmpty() ? template.getName() : template.getName() + " - " + title;
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(spawnFile)))
				{
					writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
					writer.write("<list xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../../xsd/spawns.xsd\">" + System.lineSeparator());
					writer.write("	<spawn name=\"" + x + "_" + y + "\">" + System.lineSeparator());
					writer.write("		<group>" + System.lineSeparator());
					writer.write("			<npc id=\"" + spawnId + (amount > 1 ? "\" count=\"" + spawnCount : "") + "\" x=\"" + spawnX + "\" y=\"" + spawnY + "\" z=\"" + spawnZ + (spawnHeadingVal > 0 ? "\" heading=\"" + spawnHeading : "") + "\" respawnTime=\"" + spawnDelay + "sec\" /> <!-- " + name + " -->" + System.lineSeparator());
					writer.write("		</group>" + System.lineSeparator());
					writer.write("	</spawn>" + System.lineSeparator());
					writer.write("</list>" + System.lineSeparator());
				}
				LOGGER.info(getClass().getSimpleName() + ": Created file: " + OTHER_XML_FOLDER + "/" + x + "_" + y + ".xml");
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Spawn " + spawn + " could not be added to the spawn XML files: " + e);
			}
		}
	}
	
	/**
	 * Removes a specific {@code Spawn} from the server data.<br>
	 * This method deletes the entry from the {@code SpawnTable}.<br>
	 * It also updates the corresponding XML file on the disk: a fixed point loses its element, a territory spawn loses one of the count of its npc line.<br>
	 * If the resulting XML file is empty, it will be deleted automatically.
	 * @param spawn The {@code Spawn} object to be removed.
	 */
	public synchronized void deleteSpawn(Spawn spawn)
	{
		SpawnTable.getInstance().removeSpawn(spawn);
		editSpawnFile(spawn, false);
	}
	
	/**
	 * Removes the group of a {@code Spawn} from the server data.<br>
	 * This method deletes the entry from the {@code SpawnTable}.<br>
	 * It also updates the corresponding XML file on the disk: the whole npc line goes, or the group element when the group has territories of its own.<br>
	 * The other npcs of the group are the business of the caller.
	 * @param spawn The {@code Spawn} of the npc that names the group.
	 */
	public synchronized void deleteGroup(Spawn spawn)
	{
		SpawnTable.getInstance().removeSpawn(spawn);
		editSpawnFile(spawn, true);
	}
	
	/**
	 * Rewrites the spawn file of a {@code Spawn} without it.
	 * @param spawn The {@code Spawn} object to be removed.
	 * @param wholeGroup {@code true} to remove its whole npc line or group, {@code false} to remove only one npc.
	 */
	private void editSpawnFile(Spawn spawn, boolean wholeGroup)
	{
		final int x = ((spawn.getX() - World.WORLD_X_MIN) >> 15) + World.TILE_X_MIN;
		final int y = ((spawn.getY() - World.WORLD_Y_MIN) >> 15) + World.TILE_Y_MIN;
		final NpcSpawnTemplate npcSpawnTemplate = spawn.getNpcSpawnTemplate();
		final SpawnTemplate npcSpawnInnerTemplate = npcSpawnTemplate != null ? npcSpawnTemplate.getSpawnTemplate() : null;
		final File spawnFile = npcSpawnInnerTemplate != null ? npcSpawnInnerTemplate.getFile() : new File(OTHER_XML_FOLDER + "/" + x + "_" + y + ".xml");
		final File tempFile = new File(spawnFile.getAbsolutePath().substring(ServerConfig.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/') + ".tmp");
		try
		{
			boolean lastLineFound = false; // Used to check for empty file.
			int lineCount = 0;
			try (BufferedReader reader = new BufferedReader(new FileReader(spawnFile));
				BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile)))
			{
				boolean found = false; // In XML you can have more than one spawn with same coords.
				boolean isMultiLine = false; // In case spawn has more stats.
				String currentLine;
				
				final SpawnGroup group = npcSpawnTemplate != null ? npcSpawnTemplate.getGroup() : null;
				List<SpawnTerritory> territories = group != null ? group.getTerritories() : Collections.emptyList();
				if (territories.isEmpty() && (npcSpawnInnerTemplate != null))
				{
					territories = npcSpawnInnerTemplate.getTerritories();
				}
				
				if (territories.isEmpty())
				{
					final String spawnId = String.valueOf(spawn.getId());
					final Location spawnLoc = npcSpawnTemplate != null ? npcSpawnTemplate.getSpawnLocation() : null;
					final String spawnX = String.valueOf(spawnLoc != null ? spawnLoc.getX() : spawn.getX());
					final String spawnY = String.valueOf(spawnLoc != null ? spawnLoc.getY() : spawn.getY());
					final String spawnZ = String.valueOf(spawnLoc != null ? spawnLoc.getZ() : spawn.getZ());
					
					while ((currentLine = reader.readLine()) != null)
					{
						if (!found)
						{
							if (isMultiLine)
							{
								if (currentLine.contains("</npc>"))
								{
									found = true;
								}
								continue;
							}
							
							if (currentLine.contains(spawnId) && currentLine.contains(spawnX) && currentLine.contains(spawnY) && currentLine.contains(spawnZ))
							{
								if (!currentLine.contains("/>") && !currentLine.contains("</npc>"))
								{
									isMultiLine = true;
								}
								else
								{
									found = true;
								}
								continue;
							}
						}
						
						writer.write(currentLine + System.lineSeparator());
						if (currentLine.contains("</list>"))
						{
							lastLineFound = true;
						}
						
						if (!lastLineFound)
						{
							lineCount++;
						}
					}
				}
				else if (wholeGroup && (group != null) && !group.getTerritories().isEmpty())
				{
					// A group with territories of its own: the whole group element goes.
					final List<String> block = new ArrayList<>();
					boolean inGroup = false;
					boolean ours = false;
					while ((currentLine = reader.readLine()) != null)
					{
						if (!found)
						{
							if (currentLine.contains("<group"))
							{
								inGroup = true;
							}
							
							if (inGroup)
							{
								block.add(currentLine);
								if (!ours)
								{
									ours = namesTerritory(currentLine, territories);
								}
								
								if (currentLine.contains("</group>"))
								{
									inGroup = false;
									if (ours)
									{
										found = true;
									}
									else
									{
										for (String kept : block)
										{
											writer.write(kept + System.lineSeparator());
											lineCount++;
										}
									}
									
									block.clear();
								}
								continue;
							}
						}
						
						writer.write(currentLine + System.lineSeparator());
						if (currentLine.contains("</list>"))
						{
							lastLineFound = true;
						}
						
						if (!lastLineFound)
						{
							lineCount++;
						}
					}
				}
				else
				{
					// A territory spawn keeps its block: the npc line loses one of its count, or goes when none is left or the whole line is asked.
					final String npcId = "id=\"" + spawn.getId() + "\"";
					final String spawnX = "x=\"" + spawn.getX() + "\"";
					final String spawnY = "y=\"" + spawn.getY() + "\"";
					boolean inBlock = false;
					while ((currentLine = reader.readLine()) != null)
					{
						if (!found)
						{
							if (isMultiLine)
							{
								if (currentLine.contains("</npc>"))
								{
									found = true;
								}
								continue;
							}
							
							if (!inBlock)
							{
								inBlock = namesTerritory(currentLine, territories);
							}
							else if (currentLine.contains("</spawn>"))
							{
								inBlock = false; // the npc was not in this block after all
							}
							else if (currentLine.contains("<npc ") && currentLine.contains(npcId) && (!currentLine.contains("x=\"") || (currentLine.contains(spawnX) && currentLine.contains(spawnY))))
							{
								final int count = parseCount(currentLine);
								if (!wholeGroup && (count > 1))
								{
									currentLine = currentLine.replace("count=\"" + count + "\"", "count=\"" + (count - 1) + "\"");
									found = true;
								}
								else
								{
									if (currentLine.contains("/>"))
									{
										found = true;
									}
									else
									{
										isMultiLine = true;
									}
									
									continue;
								}
							}
						}
						
						writer.write(currentLine + System.lineSeparator());
						if (currentLine.contains("</list>"))
						{
							lastLineFound = true;
						}
						
						if (!lastLineFound)
						{
							lineCount++;
						}
					}
				}
			}
			spawnFile.delete();
			tempFile.renameTo(spawnFile);
			
			// Delete empty file.
			if (lineCount < 8)
			{
				LOGGER.info(getClass().getSimpleName() + ": Deleted empty file: " + spawnFile.getAbsolutePath().substring(ServerConfig.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/'));
				spawnFile.delete();
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Spawn " + spawn + " could not be removed from the spawn XML files: " + e);
		}
	}
	
	/**
	 * Tells whether a line of a spawn file names one of the given territories.
	 * @param line The line of the file.
	 * @param territories The territories to look for.
	 * @return {@code true} when the line quotes the name of one of them.
	 */
	private static boolean namesTerritory(String line, List<SpawnTerritory> territories)
	{
		for (SpawnTerritory territory : territories)
		{
			if (line.contains('"' + territory.getName() + '"'))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Reads the count attribute of an npc line of a spawn file.
	 * @param line The line of the {@code <npc>} element.
	 * @return The count, or {@code 1} when the line has none.
	 */
	private static int parseCount(String line)
	{
		final Matcher matcher = COUNT_PATTERN.matcher(line);
		return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
	}
	
	/**
	 * Retrieves the singleton instance of {@code SpawnData}.<br>
	 * This method provides a global access point to the spawn data manager.
	 * @return The singleton instance of {@code SpawnData}.
	 */
	public static SpawnData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SpawnData INSTANCE = new SpawnData();
	}
}
