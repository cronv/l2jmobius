package custom.GameEvents.events;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.ScriptManager;

import custom.GameEvents.EventState;
import custom.GameEvents.GameEventType;
import custom.GameEvents.IGameEvent;
import custom.GameEvents.schedule.IEventSchedule;
import custom.events.TeamVsTeam.TvT;

/**
 * Bridge: оборачивает оригинальный TvT под контракт IGameEvent.
 * Расписание читает из config.xml оригинала (SchedulingPattern).
 * Не добавляет зависимостей в TvT.java.
 */
public class TvTAdapter implements IGameEvent
{
	private static final String SCRIPT_NAME = "TvT";
	private static final String CONFIG_PATH = "data/scripts/custom/events/TeamVsTeam/config.xml";

	private final List<SchedulingPattern> _patterns = new ArrayList<>(2);

	public TvTAdapter()
	{
		loadSchedule();
	}

	private void loadSchedule()
	{
		try
		{
			final File file = new File(CONFIG_PATH);
			if (!file.exists())
			{
				return;
			}
			final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
			final NodeList schedules = doc.getElementsByTagName("schedule");
			for (int i = 0; i < schedules.getLength(); i++)
			{
				final Element el = (Element) schedules.item(i);
				final String pattern = el.getAttribute("pattern");
				if ((pattern != null) && !pattern.isBlank())
				{
					_patterns.add(new SchedulingPattern(pattern));
				}
			}
		}
		catch (Exception e)
		{
			// Конфиг не загружен — ивент только по GM-команде.
		}
	}

	private TvT getOriginal()
	{
		return (TvT) ScriptManager.getInstance().getScript(SCRIPT_NAME);
	}

	@Override
	public GameEventType getType()
	{
		return GameEventType.TEAM_VS_TEAM;
	}

	@Override
	public String getDisplayName()
	{
		return GameEventType.TEAM_VS_TEAM.getDisplayName();
	}

	@Override
	public EventState getState()
	{
		final TvT tvt = getOriginal();
		if (tvt == null)
		{
			return EventState.INACTIVE;
		}
		return switch (tvt.getEventState())
		{
			case PARTICIPATING -> EventState.REGISTERING;
			case STARTING, STARTED -> EventState.RUNNING;
			default -> EventState.INACTIVE;
		};
	}

	@Override
	public boolean isActive()
	{
		final EventState state = getState();
		return (state == EventState.REGISTERING) || (state == EventState.RUNNING);
	}

	@Override
	public int getParticipantCount()
	{
		final TvT tvt = getOriginal();
        if ((tvt == null) || (tvt.getEventState() == TvT.EventState.INACTIVE))
        {
            return 0;
        }
        return tvt.getRegisteredCount();
	}

	@Override
	public int getMinParticipants()
	{
		final TvT tvt = getOriginal();
		return (tvt != null) ? tvt.getMinimumParticipants() : 4;
	}

	@Override
	public int getMaxParticipants()
	{
		final TvT tvt = getOriginal();
		return (tvt != null) ? tvt.getMaximumParticipants() : 24;
	}

	@Override
	public int getMinLevel()
	{
		final TvT tvt = getOriginal();
		return (tvt != null) ? tvt.getMinimumLevel() : 85;
	}

	@Override
	public int getMaxLevel()
	{
		final TvT tvt = getOriginal();
		return (tvt != null) ? tvt.getMaximumLevel() : 200;
	}

	@Override
	public boolean isRegistered(int objectId)
	{
		final TvT tvt = getOriginal();
        if ((tvt == null) || (tvt.getEventState() == TvT.EventState.INACTIVE))
        {
            return false;
        }
        final Player player = World.getInstance().getPlayer(objectId);
        return (player != null) && tvt.isPlayerRegistered(player);
	}

	@Override
	public boolean register(Player player)
	{
		final TvT tvt = getOriginal();
		if (tvt == null)
		{
			player.sendMessage("TvT event is not loaded.");
			return false;
		}
		if (tvt.getEventState() != TvT.EventState.PARTICIPATING)
		{
			player.sendMessage("Registration is not open.");
			return false;
		}
		tvt.eventBypass(player, "Participate");
		return tvt.isPlayerRegistered(player);
	}

	@Override
	public boolean unregister(Player player)
	{
		final TvT tvt = getOriginal();
		if (tvt == null)
		{
			return false;
		}
		tvt.eventBypass(player, "CancelParticipation");
		return !tvt.isPlayerRegistered(player);
	}

	@Override
	public IEventSchedule getSchedule()
	{
		// Cron-расписание не совместимо с sealed IEventSchedule.
		// EventsBoard форматирует время напрямую через getNextStartMillis().
		return null;
	}

	@Override
	public long getNextStartMillis()
	{
		if (_patterns.isEmpty())
		{
			return 0; // Нет расписания — только GM-запуск.
		}
		final long now = System.currentTimeMillis();
		long earliest = Long.MAX_VALUE;
		for (SchedulingPattern pattern : _patterns)
		{
			final long next = pattern.next(now);
			if ((next > 0) && (next < earliest))
			{
				earliest = next;
			}
		}
		return (earliest == Long.MAX_VALUE) ? 0 : earliest;
	}
}
