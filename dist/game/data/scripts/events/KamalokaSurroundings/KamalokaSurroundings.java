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
package events.KamalokaSurroundings;

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Kamaloka Surroundings (Rim Kamaloka) event: the Pathfinder Worker on the town event pedestals.<br>
 * The dungeon itself is handled by {@link events.KamalokaSurroundings.KamalokaSurroundingsInstance}.
 * @author Atronic
 */
public class KamalokaSurroundings extends LongTimeEvent
{
	private static final int PATHFINDER_WORKER = 34520;
	
	// @formatter:off
	private static final String[] RANK_LABELS = {"<font color=\"ff6666\">1st</font>", "<font color=\"ffcc33\">2nd</font>", "<font color=\"ffcc33\">3rd</font>"};
	private static final String[] NO_ENTRY = {"-", "-"};
	// @formatter:on
	
	private KamalokaSurroundings()
	{
		addStartNpc(PATHFINDER_WORKER);
		addFirstTalkId(PATHFINDER_WORKER);
		addTalkId(PATHFINDER_WORKER);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "34520.htm";
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("stage_"))
		{
			final int stage;
			try
			{
				stage = Integer.parseInt(event.substring("stage_".length()));
			}
			catch (NumberFormatException e)
			{
				return null;
			}
			
			if ((stage < 0) || (stage >= KamalokaSurroundingsInstance.STAGE_LEVELS.length))
			{
				return null;
			}
			
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setHtml(getHtm(player, "34520-05.htm"));
			html.replace("%level%", String.valueOf(KamalokaSurroundingsInstance.STAGE_LEVELS[stage]));
			html.replace("%stage%", String.valueOf(stage));
			player.sendPacket(html);
			return null;
		}
		
		switch (event)
		{
			case "34520.htm":
			case "34520-01.htm":
			case "34520-02.htm":
			case "34520-03.htm":
			{
				return event;
			}
			case "ranking":
			{
				final List<String[]> ranking = KamalokaSurroundingsInstance.getRanking();
				final int rankingSize = ranking.size();
				final StringBuilder rows = new StringBuilder();
				for (int i = 0; i < 3; i++)
				{
					final String[] entry = i < rankingSize ? ranking.get(i) : NO_ENTRY;
					rows.append("<tr><td width=40 align=center>").append(RANK_LABELS[i]).append("</td><td width=140 align=center>").append(entry[0]).append("</td><td width=90 align=center>").append(entry[1]).append("</td></tr>");
				}
				
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setHtml(getHtm(player, "34520-04.htm"));
				html.replace("%rows%", rows.toString());
				player.sendPacket(html);
				return null;
			}
			default:
			{
				break;
			}
		}
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new KamalokaSurroundings();
	}
}
