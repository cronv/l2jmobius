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
package org.l2jmobius.tools.editor.npc.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.tools.editor.npc.model.NpcDefinition;
import org.l2jmobius.tools.editor.npc.model.NpcField;
import org.l2jmobius.tools.editor.npc.model.NpcField.ValueKind;
import org.l2jmobius.tools.editor.npc.model.NpcIndex;
import org.l2jmobius.tools.editor.npc.model.NpcMinionEntry;
import org.l2jmobius.tools.editor.npc.model.NpcMinionSet;
import org.l2jmobius.tools.editor.npc.model.NpcParameterEntry;
import org.l2jmobius.tools.editor.npc.model.NpcParameterSkillEntry;
import org.l2jmobius.tools.editor.npc.model.NpcProjectLayout;
import org.l2jmobius.tools.editor.npc.model.NpcRecord;
import org.l2jmobius.tools.editor.npc.model.NpcReferenceSection;
import org.l2jmobius.tools.editor.npc.model.NpcSkillEntry;
import org.l2jmobius.tools.editor.npc.model.NpcSkillInfo;
import org.l2jmobius.tools.editor.npc.model.NpcStructuredData;
import org.l2jmobius.tools.editor.npc.util.NpcXmlUtil;
import org.l2jmobius.tools.editor.service.FileSnapshot;
import org.l2jmobius.tools.editor.service.FileSnapshotCache;

/**
 * Loads one NPC definition and performs surgical updates without reconstructing the complete NPC node. <br>
 * Drop lists remain untouched; scalar fields and supported repeated NPC structures are edited in place.
 * @author BazookaRpm
 */
public final class NpcDefinitionXmlEditor
{
	private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
	private static final Pattern INTEGER = Pattern.compile("[-+]?\\d+");
	private static final Pattern DECIMAL = Pattern.compile("[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?");
	
	public NpcDefinition load(NpcRecord record) throws Exception
	{
		final FileSnapshot snapshot = FileSnapshotCache.get(record.getFile());
		final Document document = NpcXmlUtil.parse(snapshot.getText());
		final Element npc = findNpc(document, record.getId());
		if (npc == null)
		{
			throw new IllegalArgumentException("NPC " + record.getId() + " was not found in " + record.getFile() + ".");
		}
		
		final List<NpcField> fields = new ArrayList<>();
		loadRootAttributes(npc, fields);
		loadScalarChildren(npc, "", fields);
		final NpcStructuredData structuredData = loadStructuredData(npc);
		final List<NpcReferenceSection> references = loadReferences(npc);
		return new NpcDefinition(record, fields, structuredData, references, snapshot.getSha256());
	}
	
	public void validate(NpcDefinition definition, NpcProjectLayout layout) throws Exception
	{
		validateInternal(definition, layout, null);
	}
	
	public void validate(NpcDefinition definition, NpcIndex index) throws Exception
	{
		validateInternal(definition, index.getLayout(), index);
	}
	
	private void validateInternal(NpcDefinition definition, NpcProjectLayout layout, NpcIndex index) throws Exception
	{
		final FileSnapshot original = FileSnapshotCache.readFresh(definition.getNpc().getFile());
		if (!original.getSha256().equals(definition.getSourceSha256()))
		{
			throw new IllegalStateException("NPC XML changed on disk after it was loaded. Reload the NPC before validating.");
		}
		
		for (NpcField field : definition.getFields())
		{
			validateValue(field);
		}
		
		validateStructuredData(definition.getStructuredData(), index);
		final String candidate = buildCandidate(definition, original.getText());
		NpcXmlUtil.validate(candidate, layout.getNpcSchema());
	}
	
	public Path save(NpcDefinition definition, NpcProjectLayout layout) throws Exception
	{
		return saveInternal(definition, layout, null);
	}
	
	public Path save(NpcDefinition definition, NpcIndex index) throws Exception
	{
		return saveInternal(definition, index.getLayout(), index);
	}
	
	private Path saveInternal(NpcDefinition definition, NpcProjectLayout layout, NpcIndex index) throws Exception
	{
		if (!definition.isDirty())
		{
			return null;
		}
		
		final Path file = definition.getNpc().getFile();
		final FileSnapshot original = FileSnapshotCache.readFresh(file);
		if (!original.getSha256().equals(definition.getSourceSha256()))
		{
			throw new IllegalStateException("NPC XML changed on disk after it was loaded. Reload the NPC before saving.");
		}
		
		for (NpcField field : definition.getFields())
		{
			if (field.isChanged())
			{
				validateValue(field);
			}
		}
		
		validateStructuredData(definition.getStructuredData(), index);
		
		final String updated = buildCandidate(definition, original.getText());
		NpcXmlUtil.validate(updated, layout.getNpcSchema());
		
		final Path backup = createBackup(file, layout);
		writeAtomic(file, updated, original.hasBom());
		final FileSnapshot saved = FileSnapshotCache.readFresh(file);
		NpcXmlUtil.validate(saved.getText(), layout.getNpcSchema());
		definition.setSourceSha256(saved.getSha256());
		return backup;
	}
	
	private String buildCandidate(NpcDefinition definition, String source)
	{
		final ElementSpan npcSpan = findNpcSpan(source, definition.getNpc().getId());
		String npcBlock = source.substring(npcSpan._start, npcSpan._end);
		for (NpcField field : definition.getFields())
		{
			if (field.isChanged())
			{
				npcBlock = replaceField(npcBlock, field);
			}
		}
		
		if (definition.getStructuredData().isDirty())
		{
			npcBlock = applyStructuredChanges(npcBlock, definition.getStructuredData());
		}
		
		return source.substring(0, npcSpan._start) + npcBlock + source.substring(npcSpan._end);
	}
	
	private Element findNpc(Document document, int npcId)
	{
		final Element root = document.getDocumentElement();
		for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ((node.getNodeType() == Node.ELEMENT_NODE) && "npc".equals(node.getNodeName()))
			{
				final Element npc = (Element) node;
				if (npc.hasAttribute("id") && (Integer.parseInt(npc.getAttribute("id")) == npcId))
				{
					return npc;
				}
			}
		}
		
		return null;
	}
	
	private void loadRootAttributes(Element npc, List<NpcField> fields)
	{
		final NamedNodeMap attributes = npc.getAttributes();
		for (int index = 0; index < attributes.getLength(); index++)
		{
			final Node attribute = attributes.item(index);
			final String name = attribute.getNodeName();
			if ("id".equals(name))
			{
				continue;
			}
			
			fields.add(new NpcField("@" + name, "General", "Identity", humanize(name), "", name, infer(attribute.getNodeValue()), false, attribute.getNodeValue()));
		}
	}
	
	private void loadScalarChildren(Element parent, String parentPath, List<NpcField> fields)
	{
		final Map<String, Integer> siblingCounts = countElementChildren(parent);
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if (node.getNodeType() != Node.ELEMENT_NODE)
			{
				continue;
			}
			
			final Element element = (Element) node;
			final String tag = element.getTagName();
			if ("dropLists".equals(tag) || "skillList".equals(tag) || "parameters".equals(tag) || (siblingCounts.get(tag) > 1))
			{
				continue;
			}
			
			final String path = parentPath.isEmpty() ? tag : parentPath + "/" + tag;
			final String tab = tabFor(path);
			final String group = groupFor(path);
			final NamedNodeMap attributes = element.getAttributes();
			for (int index = 0; index < attributes.getLength(); index++)
			{
				final Node attribute = attributes.item(index);
				final String key = path + "@" + attribute.getNodeName();
				fields.add(new NpcField(key, tab, group, humanize(attribute.getNodeName()), path, attribute.getNodeName(), infer(attribute.getNodeValue()), false, attribute.getNodeValue()));
			}
			
			if (!hasElementChildren(element))
			{
				final String text = element.getTextContent() == null ? "" : element.getTextContent().trim();
				if (!text.isEmpty())
				{
					fields.add(new NpcField(path + "#text", tab, group, humanize(tag), path, null, infer(text), false, text));
				}
			}
			else
			{
				loadScalarChildren(element, path, fields);
			}
		}
	}
	
	private Map<String, Integer> countElementChildren(Element parent)
	{
		final Map<String, Integer> counts = new HashMap<>();
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				counts.merge(node.getNodeName(), 1, Integer::sum);
			}
		}
		
		return counts;
	}
	
	private boolean hasElementChildren(Element element)
	{
		for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				return true;
			}
		}
		
		return false;
	}
	
	private NpcStructuredData loadStructuredData(Element npc)
	{
		final Element skillList = directChild(npc, "skillList");
		final Element parameters = directChild(npc, "parameters");
		final NpcStructuredData data = new NpcStructuredData(skillList != null, parameters != null);
		if (skillList != null)
		{
			int skillIndex = 0;
			for (Element skill : directChildren(skillList, "skill"))
			{
				data.addLoadedSkill(new NpcSkillEntry(skillIndex++, parseRequiredInt(skill, "id"), parseRequiredInt(skill, "level")));
			}
		}
		
		if (parameters != null)
		{
			int parameterIndex = 0;
			int parameterSkillIndex = 0;
			int minionSetIndex = 0;
			for (Node node = parameters.getFirstChild(); node != null; node = node.getNextSibling())
			{
				if (node.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				
				final Element element = (Element) node;
				if ("param".equals(element.getTagName()))
				{
					data.addLoadedParameter(new NpcParameterEntry(parameterIndex++, element.getAttribute("name"), element.getAttribute("value")));
				}
				else if ("skill".equals(element.getTagName()))
				{
					data.addLoadedParameterSkill(new NpcParameterSkillEntry(parameterSkillIndex++, element.getAttribute("name"), parseRequiredInt(element, "id"), parseRequiredInt(element, "level")));
				}
				else if ("minions".equals(element.getTagName()))
				{
					final NpcMinionSet minionSet = new NpcMinionSet(minionSetIndex++, element.getAttribute("name"));
					int minionIndex = 0;
					for (Element minion : directChildren(element, "npc"))
					{
						minionSet.addLoadedMinion(new NpcMinionEntry(minionIndex++, parseRequiredInt(minion, "id"), parseRequiredInt(minion, "count"), parseOptionalInt(minion, "max"), parseRequiredInt(minion, "respawnTime"), parseOptionalInt(minion, "weightPoint")));
					}
					
					data.addLoadedMinionSet(minionSet);
				}
			}
		}
		
		return data;
	}
	
	private int parseRequiredInt(Element element, String attribute)
	{
		if (!element.hasAttribute(attribute))
		{
			throw new IllegalArgumentException("Missing required " + element.getTagName() + " attribute " + attribute + ".");
		}
		
		try
		{
			return Integer.parseInt(element.getAttribute(attribute));
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Invalid " + element.getTagName() + " attribute " + attribute + ": " + element.getAttribute(attribute), e);
		}
	}
	
	private Integer parseOptionalInt(Element element, String attribute)
	{
		if (!element.hasAttribute(attribute) || element.getAttribute(attribute).isBlank())
		{
			return null;
		}
		
		return Integer.valueOf(parseRequiredInt(element, attribute));
	}
	
	private List<NpcReferenceSection> loadReferences(Element npc)
	{
		final List<NpcReferenceSection> sections = new ArrayList<>();
		final Element ai = directChild(npc, "ai");
		if (ai != null)
		{
			final Element clanList = directChild(ai, "clanList");
			if (clanList != null)
			{
				final NpcReferenceSection clans = new NpcReferenceSection("AI Clans", "Clan");
				final NpcReferenceSection ignoredNpcs = new NpcReferenceSection("AI Ignored NPCs", "NPC");
				for (Node node = clanList.getFirstChild(); node != null; node = node.getNextSibling())
				{
					if (node.getNodeType() != Node.ELEMENT_NODE)
					{
						continue;
					}
					
					final Element element = (Element) node;
					if ("clan".equals(element.getTagName()))
					{
						clans.addRow(element.getTextContent().trim());
					}
					else if ("ignoreNpcId".equals(element.getTagName()))
					{
						ignoredNpcs.addRow(element.getTextContent().trim());
					}
				}
				
				if (!clans.getRows().isEmpty())
				{
					sections.add(clans);
				}
				
				if (!ignoredNpcs.getRows().isEmpty())
				{
					sections.add(ignoredNpcs);
				}
			}
		}
		
		return sections;
	}
	
	private Element directChild(Element parent, String name)
	{
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ((node.getNodeType() == Node.ELEMENT_NODE) && name.equals(node.getNodeName()))
			{
				return (Element) node;
			}
		}
		
		return null;
	}
	
	private List<Element> directChildren(Element parent, String name)
	{
		final List<Element> result = new ArrayList<>();
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ((node.getNodeType() == Node.ELEMENT_NODE) && name.equals(node.getNodeName()))
			{
				result.add((Element) node);
			}
		}
		
		return result;
	}
	
	private ValueKind infer(String value)
	{
		if ((value != null) && ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)))
		{
			return ValueKind.BOOLEAN;
		}
		
		if ((value != null) && INTEGER.matcher(value.trim()).matches())
		{
			return ValueKind.INTEGER;
		}
		
		if ((value != null) && DECIMAL.matcher(value.trim()).matches())
		{
			return ValueKind.DECIMAL;
		}
		
		return ValueKind.TEXT;
	}
	
	private String tabFor(String path)
	{
		if (path.startsWith("stats/vitals") || "stats".equals(path))
		{
			return "Stats";
		}
		
		if (path.startsWith("stats/attack") || path.startsWith("stats/defence"))
		{
			return "Combat";
		}
		
		if (path.startsWith("stats/abnormalResist") || path.startsWith("stats/attribute"))
		{
			return "Elements";
		}
		
		if (path.startsWith("stats/speed") || path.startsWith("stats/hitTime"))
		{
			return "Movement";
		}
		
		if (path.startsWith("status") || path.startsWith("ai"))
		{
			return "AI / Aggro";
		}
		
		if (path.startsWith("collision"))
		{
			return "Collision";
		}
		
		if (path.startsWith("acquire") || path.startsWith("mpReward"))
		{
			return "Rewards";
		}
		
		if (path.startsWith("equipment") || path.startsWith("shots"))
		{
			return "Equipment / Shots";
		}
		
		if (path.startsWith("race") || path.startsWith("sex") || path.startsWith("corpseTime") || path.startsWith("exCrtEffect"))
		{
			return "General";
		}
		
		return "Advanced";
	}
	
	private String groupFor(String path)
	{
		if (path.isEmpty() || "race".equals(path) || "sex".equals(path))
		{
			return "Identity";
		}
		
		if ("corpseTime".equals(path) || "exCrtEffect".equals(path) || "sNpcPropHpRate".equals(path))
		{
			return "Runtime";
		}
		
		if ("stats".equals(path))
		{
			return "Attributes";
		}
		
		if ("stats/hitTime".equals(path))
		{
			return "Timing";
		}
		
		if ("stats/attribute/attack".equals(path))
		{
			return "Element Attack";
		}
		
		if ("stats/attribute/defence".equals(path))
		{
			return "Element Defence";
		}
		
		if ("equipment".equals(path))
		{
			return "Gear";
		}
		
		if ("acquire".equals(path))
		{
			return "Acquire";
		}
		
		if ("mpReward".equals(path))
		{
			return "MP Reward";
		}
		
		if ("shots".equals(path))
		{
			return "Shots";
		}
		
		if ("status".equals(path))
		{
			return "Status";
		}
		
		if ("ai".equals(path))
		{
			return "AI";
		}
		
		final int slash = path.lastIndexOf('/');
		return humanize(slash < 0 ? "Details" : path.substring(slash + 1));
	}
	
	private String humanize(String value)
	{
		if ((value == null) || value.isEmpty())
		{
			return "Value";
		}
		
		final StringBuilder builder = new StringBuilder();
		for (int index = 0; index < value.length(); index++)
		{
			final char character = value.charAt(index);
			if ((index > 0) && Character.isUpperCase(character) && Character.isLowerCase(value.charAt(index - 1)))
			{
				builder.append(' ');
			}
			builder.append(character);
		}
		
		if (builder.length() > 0)
		{
			builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
		}
		
		return builder.toString();
	}
	
	private void validateValue(NpcField field)
	{
		final String value = field.getValue().trim();
		try
		{
			switch (field.getKind())
			{
				case BOOLEAN:
				{
					if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value))
					{
						throw new IllegalArgumentException(field.getLabel() + " must be true or false.");
					}
					break;
				}
				case INTEGER:
				{
					Long.parseLong(value);
					break;
				}
				case DECIMAL:
				{
					new BigDecimal(value);
					break;
				}
				case TEXT:
				{
					break;
				}
				default:
				{
					throw new IllegalArgumentException(field.getLabel() + " has an unknown field kind: " + field.getKind() + ".");
				}
			}
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException(field.getLabel() + " has an invalid numeric value: " + value, e);
		}
	}
	
	private void validateStructuredData(NpcStructuredData data, NpcIndex index)
	{
		if (data.hasSkillList())
		{
			if (data.getSkills().isEmpty())
			{
				throw new IllegalArgumentException("Skill List must contain at least one skill.");
			}
			
			for (NpcSkillEntry skill : data.getSkills())
			{
				if ((skill.getId() < 0) || (skill.getLevel() < 0))
				{
					throw new IllegalArgumentException("NPC skills require non-negative id and level values.");
				}
				
				if ((index != null) && (skill.isNew() || (skill.getId() != skill.getOriginalId()) || skill.isChanged()))
				{
					final NpcSkillInfo info = index.getSkill(skill.getId());
					if (info == null)
					{
						throw new IllegalArgumentException("Skill " + skill.getId() + " does not exist in the active chronicle's data/stats/skills XML.");
					}
					
					if (!info.supportsLevel(skill.getLevel()))
					{
						throw new IllegalArgumentException("Skill " + skill.getId() + " (" + info.getName() + ") supports levels " + info.getMinLevel() + "-" + info.getMaxLevel() + ", not level " + skill.getLevel() + ".");
					}
				}
			}
		}
		
		if (!data.hasParameters())
		{
			return;
		}
		
		if (data.getActiveParameterChildCount() == 0)
		{
			throw new IllegalArgumentException("Parameters must contain at least one param, skill, or minions entry.");
		}
		
		for (NpcParameterEntry parameter : data.getParameters())
		{
			if ((parameter.getName() == null) || parameter.getName().isBlank())
			{
				throw new IllegalArgumentException("Parameter name cannot be empty.");
			}
			
			if (parameter.getValue() == null)
			{
				throw new IllegalArgumentException("Parameter " + parameter.getName() + " requires a value.");
			}
		}
		
		for (NpcParameterSkillEntry skill : data.getParameterSkills())
		{
			if ((skill.getName() == null) || skill.getName().isBlank())
			{
				throw new IllegalArgumentException("Parameter skill name cannot be empty.");
			}
			
			if ((skill.getId() <= 0) || (skill.getLevel() <= 0))
			{
				throw new IllegalArgumentException("Parameter skill " + skill.getName() + " requires positive id and level values.");
			}
			
			if ((index != null) && (skill.isNew() || (skill.getId() != skill.getOriginalId())) && (index.getSkill(skill.getId()) == null))
			{
				throw new IllegalArgumentException("Parameter skill id " + skill.getId() + " does not exist in the active chronicle's data/stats/skills XML.");
			}
		}
		
		for (NpcMinionSet minionSet : data.getMinionSets())
		{
			if ((minionSet.getName() == null) || minionSet.getName().isBlank())
			{
				throw new IllegalArgumentException("Minion set name cannot be empty.");
			}
			
			if (minionSet.getMinions().isEmpty())
			{
				throw new IllegalArgumentException("Minion set " + minionSet.getName() + " must contain at least one NPC.");
			}
			
			for (NpcMinionEntry minion : minionSet.getMinions())
			{
				if (minion.getId() <= 0)
				{
					throw new IllegalArgumentException("Minion NPC id must be positive.");
				}
				
				if (minion.getCount() <= 0)
				{
					throw new IllegalArgumentException("Minion " + minion.getId() + " count must be positive.");
				}
				
				if ((minion.getMax() != null) && (minion.getMax().intValue() <= 0))
				{
					throw new IllegalArgumentException("Minion " + minion.getId() + " max must be positive when present.");
				}
				
				if (minion.getRespawnTime() < 0)
				{
					throw new IllegalArgumentException("Minion " + minion.getId() + " respawnTime cannot be negative.");
				}
				
				if ((index != null) && (minion.isNew() || (minion.getId() != minion.getOriginalId())) && (index.getNpc(minion.getId()) == null))
				{
					throw new IllegalArgumentException("Minion NPC " + minion.getId() + " does not exist in the active NPC datapack. Existing historical references may be preserved, but a new NPC ID must resolve.");
				}
			}
		}
	}
	
	private String applyStructuredChanges(String npcBlock, NpcStructuredData data)
	{
		String result = npcBlock;
		if (data.hasSkillList() && hasSkillChanges(data))
		{
			result = updateSkillList(result, data);
		}
		
		if (data.hasParameters() && hasParameterChanges(data))
		{
			result = updateParameters(result, data);
		}
		
		return result;
	}
	
	private boolean hasSkillChanges(NpcStructuredData data)
	{
		for (NpcSkillEntry skill : data.getAllSkills())
		{
			if (skill.isChanged())
			{
				return true;
			}
		}
		
		return false;
	}
	
	private boolean hasParameterChanges(NpcStructuredData data)
	{
		for (NpcParameterEntry parameter : data.getAllParameters())
		{
			if (parameter.isChanged())
			{
				return true;
			}
		}
		
		for (NpcParameterSkillEntry skill : data.getAllParameterSkills())
		{
			if (skill.isChanged())
			{
				return true;
			}
		}
		
		for (NpcMinionSet minionSet : data.getAllMinionSets())
		{
			if (minionSet.isChanged())
			{
				return true;
			}
		}
		
		return false;
	}
	
	private String updateSkillList(String npcBlock, NpcStructuredData data)
	{
		final ElementSpan skillListSpan = findElementSpan(npcBlock, "skillList");
		if (skillListSpan == null)
		{
			throw new IllegalStateException("The existing skillList could not be located while saving.");
		}
		
		String block = npcBlock.substring(skillListSpan._start, skillListSpan._end);
		final List<NpcSkillEntry> allSkills = data.getAllSkills();
		for (int index = allSkills.size() - 1; index >= 0; index--)
		{
			final NpcSkillEntry skill = allSkills.get(index);
			if (skill.isNew() || !skill.isChanged())
			{
				continue;
			}
			
			final ElementSpan span = findNthDirectChild(block, "skill", skill.getOriginalIndex());
			if (span == null)
			{
				throw new IllegalStateException("Existing skillList entry " + skill.getOriginalIndex() + " could not be located.");
			}
			
			if (skill.isRemoved())
			{
				block = removeElement(block, span);
			}
			else
			{
				block = replaceElementAttributes(block, span, new String[][]
				{
					{
						"id",
						Integer.toString(skill.getId())
					},
					{
						"level",
						Integer.toString(skill.getLevel())
					}
				});
			}
		}
		
		for (NpcSkillEntry skill : allSkills)
		{
			if (skill.isNew() && !skill.isRemoved())
			{
				block = appendDirectChild(block, "<skill id=\"" + skill.getId() + "\" level=\"" + skill.getLevel() + "\" />");
			}
		}
		
		return npcBlock.substring(0, skillListSpan._start) + block + npcBlock.substring(skillListSpan._end);
	}
	
	private String updateParameters(String npcBlock, NpcStructuredData data)
	{
		final ElementSpan parametersSpan = findElementSpan(npcBlock, "parameters");
		if (parametersSpan == null)
		{
			throw new IllegalStateException("The existing parameters block could not be located while saving.");
		}
		
		String block = npcBlock.substring(parametersSpan._start, parametersSpan._end);
		
		final List<NpcParameterEntry> parameters = data.getAllParameters();
		for (int index = parameters.size() - 1; index >= 0; index--)
		{
			final NpcParameterEntry parameter = parameters.get(index);
			if (parameter.isNew() || !parameter.isChanged())
			{
				continue;
			}
			
			final ElementSpan span = findNthDirectChild(block, "param", parameter.getOriginalIndex());
			if (span == null)
			{
				throw new IllegalStateException("Existing parameter entry " + parameter.getOriginalIndex() + " could not be located.");
			}
			
			if (parameter.isRemoved())
			{
				block = removeElement(block, span);
			}
			else
			{
				block = replaceElementAttributes(block, span, new String[][]
				{
					{
						"name",
						parameter.getName()
					},
					{
						"value",
						parameter.getValue()
					}
				});
			}
		}
		
		final List<NpcParameterSkillEntry> parameterSkills = data.getAllParameterSkills();
		for (int index = parameterSkills.size() - 1; index >= 0; index--)
		{
			final NpcParameterSkillEntry skill = parameterSkills.get(index);
			if (skill.isNew() || !skill.isChanged())
			{
				continue;
			}
			
			final ElementSpan span = findNthDirectChild(block, "skill", skill.getOriginalIndex());
			if (span == null)
			{
				throw new IllegalStateException("Existing parameter skill entry " + skill.getOriginalIndex() + " could not be located.");
			}
			
			if (skill.isRemoved())
			{
				block = removeElement(block, span);
			}
			else
			{
				block = replaceElementAttributes(block, span, new String[][]
				{
					{
						"name",
						skill.getName()
					},
					{
						"id",
						Integer.toString(skill.getId())
					},
					{
						"level",
						Integer.toString(skill.getLevel())
					}
				});
			}
		}
		
		final List<NpcMinionSet> minionSets = data.getAllMinionSets();
		for (int index = minionSets.size() - 1; index >= 0; index--)
		{
			final NpcMinionSet minionSet = minionSets.get(index);
			if (minionSet.isNew() || !minionSet.isChanged())
			{
				continue;
			}
			
			final ElementSpan span = findNthDirectChild(block, "minions", minionSet.getOriginalIndex());
			if (span == null)
			{
				throw new IllegalStateException("Existing minion set " + minionSet.getOriginalIndex() + " could not be located.");
			}
			
			if (minionSet.isRemoved())
			{
				block = removeElement(block, span);
			}
			else
			{
				String setBlock = block.substring(span._start, span._end);
				setBlock = updateMinionSet(setBlock, minionSet);
				block = block.substring(0, span._start) + setBlock + block.substring(span._end);
			}
		}
		
		for (NpcParameterEntry parameter : parameters)
		{
			if (parameter.isNew() && !parameter.isRemoved())
			{
				block = appendDirectChild(block, "<param name=\"" + escapeAttribute(parameter.getName(), '"') + "\" value=\"" + escapeAttribute(parameter.getValue(), '"') + "\" />");
			}
		}
		
		for (NpcParameterSkillEntry skill : parameterSkills)
		{
			if (skill.isNew() && !skill.isRemoved())
			{
				block = appendDirectChild(block, "<skill name=\"" + escapeAttribute(skill.getName(), '"') + "\" id=\"" + skill.getId() + "\" level=\"" + skill.getLevel() + "\" />");
			}
		}
		
		for (NpcMinionSet minionSet : minionSets)
		{
			if (minionSet.isNew() && !minionSet.isRemoved())
			{
				block = appendDirectChild(block, renderMinionSet(minionSet));
			}
		}
		
		return npcBlock.substring(0, parametersSpan._start) + block + npcBlock.substring(parametersSpan._end);
	}
	
	private String updateMinionSet(String setBlock, NpcMinionSet minionSet)
	{
		ElementSpan root = rootElementSpan(setBlock);
		setBlock = replaceElementAttributes(setBlock, root, new String[][]
		{
			{
				"name",
				minionSet.getName()
			}
		});
		final List<NpcMinionEntry> minions = minionSet.getAllMinions();
		for (int index = minions.size() - 1; index >= 0; index--)
		{
			final NpcMinionEntry minion = minions.get(index);
			if (minion.isNew() || !minion.isChanged())
			{
				continue;
			}
			
			final ElementSpan span = findNthDirectChild(setBlock, "npc", minion.getOriginalIndex());
			if (span == null)
			{
				throw new IllegalStateException("Existing minion entry " + minion.getOriginalIndex() + " could not be located.");
			}
			
			if (minion.isRemoved())
			{
				setBlock = removeElement(setBlock, span);
			}
			else
			{
				setBlock = replaceElementAttributes(setBlock, span, minionAttributes(minion));
			}
		}
		
		for (NpcMinionEntry minion : minions)
		{
			if (minion.isNew() && !minion.isRemoved())
			{
				setBlock = appendDirectChild(setBlock, renderMinion(minion));
			}
		}
		
		return setBlock;
	}
	
	private String[][] minionAttributes(NpcMinionEntry minion)
	{
		return new String[][]
		{
			{
				"id",
				Integer.toString(minion.getId())
			},
			{
				"count",
				Integer.toString(minion.getCount())
			},
			{
				"max",
				minion.getMax() == null ? null : minion.getMax().toString()
			},
			{
				"respawnTime",
				Integer.toString(minion.getRespawnTime())
			},
			{
				"weightPoint",
				minion.getWeightPoint() == null ? null : minion.getWeightPoint().toString()
			}
		};
	}
	
	private String renderMinionSet(NpcMinionSet minionSet)
	{
		final StringBuilder builder = new StringBuilder();
		builder.append("<minions name=\"").append(escapeAttribute(minionSet.getName(), '"')).append("\">");
		for (NpcMinionEntry minion : minionSet.getMinions())
		{
			builder.append('\n').append('\t').append(renderMinion(minion));
		}
		builder.append('\n').append("</minions>");
		return builder.toString();
	}
	
	private String renderMinion(NpcMinionEntry minion)
	{
		final StringBuilder builder = new StringBuilder();
		builder.append("<npc id=\"").append(minion.getId()).append("\" count=\"").append(minion.getCount()).append('"');
		if (minion.getMax() != null)
		{
			builder.append(" max=\"").append(minion.getMax()).append('"');
		}
		builder.append(" respawnTime=\"").append(minion.getRespawnTime()).append('"');
		if (minion.getWeightPoint() != null)
		{
			builder.append(" weightPoint=\"").append(minion.getWeightPoint()).append('"');
		}
		builder.append(" />");
		return builder.toString();
	}
	
	private ElementSpan findNthDirectChild(String block, String tag, int ordinal)
	{
		final ElementSpan root = rootElementSpan(block);
		if ((root == null) || (ordinal < 0))
		{
			return null;
		}
		
		int current = 0;
		int position = root._contentStart;
		int depth = 0;
		while ((position >= 0) && (position < root._contentEnd))
		{
			final int start = block.indexOf('<', position);
			if ((start < 0) || (start >= root._contentEnd))
			{
				return null;
			}
			
			if (block.startsWith("<!--", start))
			{
				final int end = block.indexOf("-->", start + 4);
				position = end < 0 ? root._contentEnd : end + 3;
				continue;
			}
			
			if (block.startsWith("<?", start))
			{
				final int end = block.indexOf("?>", start + 2);
				position = end < 0 ? root._contentEnd : end + 2;
				continue;
			}
			
			if (block.startsWith("<!", start))
			{
				final int end = block.indexOf('>', start + 2);
				position = end < 0 ? root._contentEnd : end + 1;
				continue;
			}
			
			final Tag parsed = parseTag(block, start);
			if (parsed == null)
			{
				return null;
			}
			
			if (parsed._closing)
			{
				if (depth > 0)
				{
					depth--;
				}
			}
			else
			{
				if ((depth == 0) && tag.equals(parsed._name))
				{
					if (current == ordinal)
					{
						return parseElementAt(block, start, root._contentEnd);
					}
					
					current++;
				}
				
				if (!parsed._selfClosing)
				{
					depth++;
				}
			}
			
			position = parsed._end;
		}
		
		return null;
	}
	
	private ElementSpan rootElementSpan(String block)
	{
		final int start = block.indexOf('<');
		return start < 0 ? null : parseElementAt(block, start, block.length());
	}
	
	private String replaceElementAttributes(String text, ElementSpan span, String[][] attributes)
	{
		final String element = text.substring(span._start, span._end);
		final ElementSpan local = rootElementSpan(element);
		String opening = element.substring(local._openStart, local._openEnd);
		for (String[] attribute : attributes)
		{
			opening = updateOpeningAttribute(opening, attribute[0], attribute[1]);
		}
		
		final String updatedElement = opening + element.substring(local._openEnd);
		return text.substring(0, span._start) + updatedElement + text.substring(span._end);
	}
	
	private String updateOpeningAttribute(String opening, String attribute, String value)
	{
		final Pattern pattern = Pattern.compile("\\s+\\b" + Pattern.quote(attribute) + "\\s*=\\s*([\"'])(.*?)\\1", Pattern.DOTALL);
		final Matcher matcher = pattern.matcher(opening);
		if (matcher.find())
		{
			if (value == null)
			{
				return opening.substring(0, matcher.start()) + opening.substring(matcher.end());
			}
			
			final char quote = matcher.group(1).charAt(0);
			final String replacement = " " + attribute + "=" + quote + escapeAttribute(value, quote) + quote;
			return opening.substring(0, matcher.start()) + replacement + opening.substring(matcher.end());
		}
		
		if (value == null)
		{
			return opening;
		}
		
		int insert = opening.lastIndexOf('>');
		if (insert < 0)
		{
			throw new IllegalStateException("Malformed XML opening tag while adding attribute " + attribute + ".");
		}
		
		int slash = insert - 1;
		while ((slash >= 0) && Character.isWhitespace(opening.charAt(slash)))
		{
			slash--;
		}
		
		if ((slash >= 0) && (opening.charAt(slash) == '/'))
		{
			insert = slash;
		}
		
		return opening.substring(0, insert) + " " + attribute + "=\"" + escapeAttribute(value, '"') + "\"" + opening.substring(insert);
	}
	
	private String removeElement(String text, ElementSpan span)
	{
		return text.substring(0, span._start) + text.substring(span._end);
	}
	
	private String appendDirectChild(String block, String markup)
	{
		final ElementSpan root = rootElementSpan(block);
		if ((root == null) || (root._contentStart < 0))
		{
			throw new IllegalStateException("Cannot append to a self-closing or malformed XML container.");
		}
		
		final String eol = detectEol(block);
		int lineStart = block.lastIndexOf('\n', Math.max(0, root._contentEnd - 1));
		lineStart = lineStart < 0 ? 0 : lineStart + 1;
		final String beforeClose = block.substring(lineStart, root._contentEnd);
		final boolean closingOnIndentedLine = beforeClose.trim().isEmpty();
		final String parentIndent = closingOnIndentedLine ? beforeClose : "";
		String childIndent = null;
		final ElementSpan firstChild = firstDirectChild(block, root);
		if (firstChild != null)
		{
			int childLineStart = block.lastIndexOf('\n', Math.max(0, firstChild._start - 1));
			childLineStart = childLineStart < 0 ? 0 : childLineStart + 1;
			final String candidate = block.substring(childLineStart, firstChild._start);
			if (candidate.trim().isEmpty())
			{
				childIndent = candidate;
			}
		}
		
		if (childIndent == null)
		{
			childIndent = parentIndent + "\t";
		}
		
		final String normalizedMarkup = markup.replace("\r\n", "\n").replace('\r', '\n').replace("\n", eol + childIndent);
		if (closingOnIndentedLine)
		{
			return block.substring(0, lineStart) + childIndent + normalizedMarkup + eol + block.substring(lineStart);
		}
		
		return block.substring(0, root._contentEnd) + eol + childIndent + normalizedMarkup + eol + parentIndent + block.substring(root._contentEnd);
	}
	
	private ElementSpan firstDirectChild(String text, ElementSpan parent)
	{
		if (parent._contentStart < 0)
		{
			return null;
		}
		
		int position = parent._contentStart;
		int depth = 0;
		while (position < parent._contentEnd)
		{
			final int start = text.indexOf('<', position);
			if ((start < 0) || (start >= parent._contentEnd))
			{
				return null;
			}
			
			if (text.startsWith("<!--", start))
			{
				final int end = text.indexOf("-->", start + 4);
				position = end < 0 ? parent._contentEnd : end + 3;
				continue;
			}
			
			final Tag tag = parseTag(text, start);
			if (tag == null)
			{
				return null;
			}
			
			if (tag._closing)
			{
				if (depth > 0)
				{
					depth--;
				}
			}
			else
			{
				if (depth == 0)
				{
					return parseElementAt(text, start, parent._contentEnd);
				}
				
				if (!tag._selfClosing)
				{
					depth++;
				}
			}
			
			position = tag._end;
		}
		
		return null;
	}
	
	private String detectEol(String text)
	{
		return text.contains("\r\n") ? "\r\n" : "\n";
	}
	
	private String replaceField(String npcBlock, NpcField field)
	{
		final ElementSpan span = field.getElementPath().isEmpty() ? findRootSpan(npcBlock) : findElementSpan(npcBlock, field.getElementPath());
		if (span == null)
		{
			throw new IllegalStateException("Could not locate existing XML field path " + field.getElementPath() + ".");
		}
		
		if (field.getAttribute() != null)
		{
			return replaceAttribute(npcBlock, span, field.getAttribute(), field.getValue());
		}
		
		return replaceText(npcBlock, span, field.getValue());
	}
	
	private String replaceAttribute(String text, ElementSpan span, String attribute, String value)
	{
		final String opening = text.substring(span._openStart, span._openEnd);
		final Pattern pattern = Pattern.compile("(\\b" + Pattern.quote(attribute) + "\\s*=\\s*)([\"'])(.*?)(\\2)", Pattern.DOTALL);
		final Matcher matcher = pattern.matcher(opening);
		if (!matcher.find())
		{
			throw new IllegalStateException("Existing attribute " + attribute + " was not found while saving.");
		}
		
		final String quote = matcher.group(2);
		final String escaped = escapeAttribute(value, quote.charAt(0));
		final String replacement = matcher.group(1) + quote + escaped + quote;
		final String updatedOpening = opening.substring(0, matcher.start()) + replacement + opening.substring(matcher.end());
		return text.substring(0, span._openStart) + updatedOpening + text.substring(span._openEnd);
	}
	
	private String replaceText(String text, ElementSpan span, String value)
	{
		if (span._contentStart < 0)
		{
			throw new IllegalStateException("Cannot edit text content of a self-closing XML element.");
		}
		
		final String oldContent = text.substring(span._contentStart, span._contentEnd);
		int leading = 0;
		while ((leading < oldContent.length()) && Character.isWhitespace(oldContent.charAt(leading)))
		{
			leading++;
		}
		
		int trailing = oldContent.length();
		while ((trailing > leading) && Character.isWhitespace(oldContent.charAt(trailing - 1)))
		{
			trailing--;
		}
		
		final String replacement = oldContent.substring(0, leading) + escapeText(value) + oldContent.substring(trailing);
		return text.substring(0, span._contentStart) + replacement + text.substring(span._contentEnd);
	}
	
	private ElementSpan findNpcSpan(String text, int npcId)
	{
		final Pattern pattern = Pattern.compile("<npc\\b[^>]*\\bid\\s*=\\s*[\"']" + npcId + "[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
		final Matcher matcher = pattern.matcher(text);
		if (!matcher.find())
		{
			throw new IllegalStateException("Could not locate NPC " + npcId + " in its XML file.");
		}
		
		return parseElementAt(text, matcher.start(), text.length());
	}
	
	private ElementSpan findRootSpan(String npcBlock)
	{
		final int start = npcBlock.indexOf("<npc");
		return start < 0 ? null : parseElementAt(npcBlock, start, npcBlock.length());
	}
	
	private ElementSpan findElementSpan(String npcBlock, String path)
	{
		ElementSpan current = findRootSpan(npcBlock);
		if (current == null)
		{
			return null;
		}
		
		for (String tag : path.split("/"))
		{
			current = findDirectChild(npcBlock, current, tag);
			if (current == null)
			{
				return null;
			}
		}
		
		return current;
	}
	
	private ElementSpan findDirectChild(String text, ElementSpan parent, String target)
	{
		if (parent._contentStart < 0)
		{
			return null;
		}
		
		int position = parent._contentStart;
		int depth = 0;
		while (position < parent._contentEnd)
		{
			final int start = text.indexOf('<', position);
			if ((start < 0) || (start >= parent._contentEnd))
			{
				return null;
			}
			
			if (text.startsWith("<!--", start))
			{
				final int end = text.indexOf("-->", start + 4);
				position = end < 0 ? parent._contentEnd : end + 3;
				continue;
			}
			
			if (text.startsWith("<?", start))
			{
				final int end = text.indexOf("?>", start + 2);
				position = end < 0 ? parent._contentEnd : end + 2;
				continue;
			}
			
			if (text.startsWith("<!", start))
			{
				final int end = text.indexOf('>', start + 2);
				position = end < 0 ? parent._contentEnd : end + 1;
				continue;
			}
			
			final Tag tag = parseTag(text, start);
			if (tag == null)
			{
				return null;
			}
			
			if (tag._closing)
			{
				if (depth > 0)
				{
					depth--;
				}
			}
			else
			{
				if ((depth == 0) && target.equals(tag._name))
				{
					return parseElementAt(text, start, parent._contentEnd);
				}
				
				if (!tag._selfClosing)
				{
					depth++;
				}
			}
			
			position = tag._end;
		}
		
		return null;
	}
	
	private ElementSpan parseElementAt(String text, int start, int limit)
	{
		final Tag opening = parseTag(text, start);
		if ((opening == null) || opening._closing)
		{
			return null;
		}
		
		if (opening._selfClosing)
		{
			return new ElementSpan(start, opening._end, -1, -1, opening._end);
		}
		
		int depth = 1;
		int position = opening._end;
		while ((position < limit) && (depth > 0))
		{
			final int next = text.indexOf('<', position);
			if ((next < 0) || (next >= limit))
			{
				break;
			}
			
			if (text.startsWith("<!--", next))
			{
				final int end = text.indexOf("-->", next + 4);
				position = end < 0 ? limit : end + 3;
				continue;
			}
			
			final Tag tag = parseTag(text, next);
			if (tag == null)
			{
				break;
			}
			
			if (tag._closing)
			{
				depth--;
				if (depth == 0)
				{
					return new ElementSpan(start, opening._end, opening._end, next, tag._end);
				}
			}
			else if (!tag._selfClosing)
			{
				depth++;
			}
			
			position = tag._end;
		}
		
		throw new IllegalStateException("Malformed XML element beginning at offset " + start + ".");
	}
	
	private Tag parseTag(String text, int start)
	{
		if ((start < 0) || (start >= text.length()) || (text.charAt(start) != '<'))
		{
			return null;
		}
		
		int position = start + 1;
		boolean closing = false;
		if ((position < text.length()) && (text.charAt(position) == '/'))
		{
			closing = true;
			position++;
		}
		while ((position < text.length()) && Character.isWhitespace(text.charAt(position)))
		{
			position++;
		}
		
		final int nameStart = position;
		while ((position < text.length()) && (Character.isLetterOrDigit(text.charAt(position)) || (text.charAt(position) == '_') || (text.charAt(position) == '-') || (text.charAt(position) == ':')))
		{
			position++;
		}
		
		if (position == nameStart)
		{
			return null;
		}
		
		final String name = text.substring(nameStart, position);
		char quote = 0;
		while (position < text.length())
		{
			final char character = text.charAt(position);
			if (quote != 0)
			{
				if (character == quote)
				{
					quote = 0;
				}
			}
			else if ((character == '\'') || (character == '"'))
			{
				quote = character;
			}
			else if (character == '>')
			{
				int check = position - 1;
				while ((check > start) && Character.isWhitespace(text.charAt(check)))
				{
					check--;
				}
				
				return new Tag(name, closing, !closing && (text.charAt(check) == '/'), position + 1);
			}
			
			position++;
		}
		
		return null;
	}
	
	private String escapeAttribute(String value, char quote)
	{
		String result = value.replace("&", "&amp;").replace("<", "&lt;");
		if (quote == '"')
		{
			result = result.replace("\"", "&quot;");
		}
		else
		{
			result = result.replace("'", "&apos;");
		}
		
		return result;
	}
	
	private String escapeText(String value)
	{
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
	
	private Path createBackup(Path source, NpcProjectLayout layout) throws IOException
	{
		final String projectName = safeName(layout.getSelectedRoot().getFileName() == null ? "project" : layout.getSelectedRoot().getFileName().toString());
		final Path backupRoot = Path.of(System.getProperty("user.home"), ".l2jmobius-npc-editor", "backups", projectName, BACKUP_FORMAT.format(LocalDateTime.now()));
		Path relative;
		try
		{
			relative = layout.getSelectedRoot().relativize(source).normalize();
			if (relative.isAbsolute() || relative.startsWith(".."))
			{
				relative = source.getFileName();
			}
		}
		catch (IllegalArgumentException e)
		{
			relative = source.getFileName();
		}
		
		final Path backup = backupRoot.resolve(relative == null ? source.getFileName() : relative).normalize();
		if (!backup.startsWith(backupRoot))
		{
			throw new IOException("Refusing to create a backup outside the backup root.");
		}
		
		Files.createDirectories(backup.getParent());
		Files.copy(source, backup, StandardCopyOption.COPY_ATTRIBUTES);
		return backup;
	}
	
	private String safeName(String value)
	{
		return value.replaceAll("[^A-Za-z0-9._-]", "_");
	}
	
	private void writeAtomic(Path target, String text, boolean bom) throws IOException
	{
		final byte[] body = text.getBytes(StandardCharsets.UTF_8);
		final byte[] output;
		if (bom)
		{
			output = new byte[body.length + 3];
			output[0] = (byte) 0xEF;
			output[1] = (byte) 0xBB;
			output[2] = (byte) 0xBF;
			System.arraycopy(body, 0, output, 3, body.length);
		}
		else
		{
			output = body;
		}
		
		final Path temp = Files.createTempFile(target.getParent(), target.getFileName().toString() + ".", ".npceditor.tmp");
		Files.write(temp, output);
		try
		{
			Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (AtomicMoveNotSupportedException e)
		{
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}
	
	/**
	 * Tracks source offsets for one XML element during surgical editing.
	 */
	private static final class ElementSpan
	{
		private final int _start;
		private final int _openStart;
		private final int _openEnd;
		private final int _contentStart;
		private final int _contentEnd;
		private final int _end;
		
		private ElementSpan(int start, int openEnd, int contentStart, int contentEnd, int end)
		{
			_start = start;
			_openStart = start;
			_openEnd = openEnd;
			_contentStart = contentStart;
			_contentEnd = contentEnd;
			_end = end;
		}
	}
	
	/**
	 * Represents one parsed XML tag and its structural flags during span discovery.
	 */
	private static final class Tag
	{
		private final String _name;
		private final boolean _closing;
		private final boolean _selfClosing;
		private final int _end;
		
		private Tag(String name, boolean closing, boolean selfClosing, int end)
		{
			_name = name;
			_closing = closing;
			_selfClosing = selfClosing;
			_end = end;
		}
	}
}
