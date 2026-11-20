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
    private volatile long _cachedTime = 0;

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

	@Override
    public String[] getCommandList()
    {
        return COMMANDS;
    }

	private void sendList(Player player)
	{
		final GameEventManager manager = GameEventManager.getInstance();
		final long now = System.currentTimeMillis();
		final long revision = manager.getRevision();
		final long day = LocalDate.now().toEpochDay();
		String html = _cachedList;
		final boolean forceRefresh = (now - _cachedTime) > 1000;
        if ((html == null) || (day != _cachedDay) || forceRefresh)
		{
			final StringBuilder rows = new StringBuilder(512);
            for (IGameEvent event : manager.getEventsSorted())
            {
                rows.append("<tr>");
                rows.append("<td width=190><a action=\"bypass _bbsevent view ").append(event.getType().name()).append("\">").append(event.getDisplayName()).append("</a></td>");
                rows.append("<td width=110>").append(event.getState().getTitle()).append("</td>");
                rows.append("<td width=80 align=center>").append(countCell(event)).append("</td>");
                rows.append("<td width=160>").append(nextCell(event, now)).append("</td>");
                rows.append("</tr>");
            }
			html = HtmCache.getInstance().getHtm(player, LIST_PATH).replace("%events%", rows.toString()).replace("%navigation%", HtmCache.getInstance().getHtm(player, NAV_PATH));
			_cachedList = html;
			_cachedRevision = revision;
			_cachedDay = day;
            _cachedTime = now;
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
		html = html.replace("%count%", countCell(event));  // ← используем тот же countCell
        html = html.replace("%min%", String.valueOf(event.getMinParticipants()));
        html = html.replace("%levels%", event.getMinLevel() + " - " + event.getMaxLevel());
		html = html.replace("%next%", nextCell(event, System.currentTimeMillis()));
		html = html.replace("%button%", button);
		CommunityBoardHandler.separateAndSend(html, player);
	}

    /** Ячейка "следующий ивент". */
    private static String nextCell(IGameEvent event, long now)
    {
        final EventState state = event.getState();
        if (state == EventState.RUNNING)
        {
            return "<font color=\"LEVEL\">Идёт сейчас</font>";
        }
        if (state == EventState.REGISTERING)
        {
            return "<font color=\"LEVEL\">Сейчас</font>";
        }
        final long nextStart = event.getNextStartMillis();
        if (nextStart <= 0)
        {
            return "Не запланирован";
        }
        final IEventSchedule schedule = event.getSchedule();
        if (schedule != null)
        {
            final IEventSchedule.NextEventTime next = schedule.describe(nextStart, now);
            return next.label() + " <font color=\"LEVEL\">" + next.time() + "</font>";
        }
        // Cron-расписание (adapter): форматируем напрямую.
        return formatCronTime(nextStart, now);
    }

    /** Форматирование времени для cron-ивентов: Сегодня/Завтра/день недели + HH:mm. */
    private static String formatCronTime(long nextMillis, long now)
    {
        final java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        final java.time.LocalDate nextDate = java.time.Instant.ofEpochMilli(nextMillis).atZone(zone).toLocalDate();
        final java.time.LocalDate today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        final long diff = nextDate.toEpochDay() - today.toEpochDay();
        final String time = java.time.Instant.ofEpochMilli(nextMillis).atZone(zone).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        final String label;
        if (diff <= 0)
        {
            label = "Сегодня";
        }
        else if (diff == 1)
        {
            label = "Завтра";
        }
        else if (diff < 7)
        {
            label = switch (nextDate.getDayOfWeek())
            {
                case MONDAY -> "Понедельник";
                case TUESDAY -> "Вторник";
                case WEDNESDAY -> "Среда";
                case THURSDAY -> "Четверг";
                case FRIDAY -> "Пятница";
                case SATURDAY -> "Суббота";
                case SUNDAY -> "Воскресенье";
            };
        }
        else
        {
            label = nextDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM"));
        }
        return label + " <font color=\"LEVEL\">" + time + "</font>";
    }

    /**
     * Счётчик игроков с цветовой индикацией достаточности.
     * Красный + подсказка если < min, золотой если >= min.
     */
    private static String countCell(IGameEvent event)
    {
        final int count = event.getParticipantCount();
        final int min = event.getMinParticipants();
        final int max = event.getMaxParticipants();

        if (count < min)
        {
            // Мало игроков — красный + подсказка минимума
            return "<font color=\"FF4444\">" + count + "</font>/" + max + " <font color=\"999999\">(мин. " + min + ")</font>";
        }
        // Достаточно — золото
        return "<font color=\"LEVEL\">" + count + "</font>/" + max;
    }
}
