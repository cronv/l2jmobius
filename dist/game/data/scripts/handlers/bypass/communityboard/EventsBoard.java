package handlers.bypass.communityboard;

import java.time.LocalDate;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;

import custom.GameEvents.EventState;
import custom.GameEvents.GameEventManager;
import custom.GameEvents.GameEventType;
import custom.GameEvents.IGameEvent;
import custom.GameEvents.schedule.IEventSchedule;

/**
 * Adapter движка к Community Board.
 * Список кэшируется и пересобирается только при смене revision или дня (Сегодня/Завтра).
 */
public class EventsBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsevents",
		"_bbsevent"
	};

	private static final String NAV_PATH = "data/html/CommunityBoard/Custom/navigation.html";
	private static final String LIST_PATH = "data/html/CommunityBoard/Custom/events/list.html";
	private static final String VIEW_PATH = "data/html/CommunityBoard/Custom/events/view.html";

	private volatile String _cachedList;
	private volatile long _cachedRevision = -1;
	private volatile long _cachedDay = -1;

    public EventsBoard()
    {
        LOG.info("EventsBoard: Community Board section 'Events' registered (commands: _bbsevents, _bbsevent).");
    }

	@Override
	public boolean onCommand(String command, Player player)
	{
		if (command.equals("_bbsevents"))
		{
			sendList(player);
			return true;
		}

		// "_bbsevent <view|reg|unreg> <TYPE>"
		final int space1 = command.indexOf(' ');
		if (space1 < 0)
		{
			sendList(player);
			return true;
		}
		final int space2 = command.indexOf(' ', space1 + 1);
		if (space2 < 0)
		{
			return false;
		}

		final String action = command.substring(space1 + 1, space2);
		final GameEventType type = GameEventType.fromName(command.substring(space2 + 1));
		if (type == null)
		{
			return false;
		}

		final IGameEvent event = GameEventManager.getInstance().getEvent(type);
		switch (action)
		{
			case "reg":
			{
				event.register(player);
				break;
			}
			case "unreg":
			{
				event.unregister(player);
				break;
			}
			case "view":
			{
				break;
			}
			default:
			{
				return false;
			}
		}

		sendView(player, event);
		return true;
	}

	private void sendList(Player player)
	{
		final GameEventManager manager = GameEventManager.getInstance();
		final long now = System.currentTimeMillis();
		final long revision = manager.getRevision();
		final long day = LocalDate.now().toEpochDay();
		String html = _cachedList;
		if ((html == null) || (revision != _cachedRevision) || (day != _cachedDay))
		{
			final StringBuilder rows = new StringBuilder(512);
			for (IGameEvent event : manager.getEventsSorted())
			{
				rows.append("<tr>");
				rows.append("<td width=190><a action=\"bypass _bbsevent view ").append(event.getType().name()).append("\">").append(event.getDisplayName()).append("</a></td>");
				rows.append("<td width=110>").append(event.getState().getTitle()).append("</td>");
				rows.append("<td width=70 align=center>").append(event.getParticipantCount()).append('/').append(event.getMaxParticipants()).append("</td>");
				rows.append("<td width=170>").append(nextCell(event, now)).append("</td>");
				rows.append("</tr>");
			}
			html = HtmCache.getInstance().getHtm(player, LIST_PATH).replace("%events%", rows.toString()).replace("%navigation%", HtmCache.getInstance().getHtm(player, NAV_PATH));
			_cachedList = html;
			_cachedRevision = revision;
			_cachedDay = day;
		}
		CommunityBoardHandler.separateAndSend(html, player);
	}

	private void sendView(Player player, IGameEvent event)
	{
		final String typeName = event.getType().name();
		final String button;
		if (event.getState() == EventState.REGISTERING)
		{
			button = event.isRegistered(player.getObjectId()) //
				? "<button value=\"Unregister\" action=\"bypass _bbsevent unreg " + typeName + "\" width=150 height=30 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" //
				: "<button value=\"Register\" action=\"bypass _bbsevent reg " + typeName + "\" width=150 height=30 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";
		}
		else
		{
			button = "<font color=\"999999\">" + event.getState().getTitle() + "</font>";
		}

		String html = HtmCache.getInstance().getHtm(player, VIEW_PATH);
		html = html.replace("%navigation%", HtmCache.getInstance().getHtm(player, NAV_PATH));
		html = html.replace("%name%", event.getDisplayName());
		html = html.replace("%status%", event.getState().getTitle());
		html = html.replace("%count%", event.getParticipantCount() + " / " + event.getMaxParticipants());
		html = html.replace("%levels%", event.getMinLevel() + " - " + event.getMaxLevel());
		html = html.replace("%next%", nextCell(event, System.currentTimeMillis()));
		html = html.replace("%button%", button);
		CommunityBoardHandler.separateAndSend(html, player);
	}

	/** Ячейка "следующий ивент": числа жёлтым через color=LEVEL. */
	private static String nextCell(IGameEvent event, long now)
	{
		if (event.getState() == EventState.RUNNING)
		{
			return "<font color=\"LEVEL\">Идёт сейчас</font>";
		}
		final IEventSchedule.NextEventTime next = event.getSchedule().describe(event.getNextStartMillis(), now);
		return next.label() + " <font color=\"LEVEL\">" + next.time() + "</font>";
	}

	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
