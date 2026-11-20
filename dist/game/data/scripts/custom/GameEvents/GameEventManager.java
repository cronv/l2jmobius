package custom.GameEvents;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

public final class GameEventManager
{
	private static final Logger LOGGER = Logger.getLogger(GameEventManager.class.getName());
	private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm");

	private final EnumMap<GameEventType, IGameEvent> _events = new EnumMap<>(GameEventType.class);
	private final IGameEvent[] _sortedByName;
	private final List<GameEventListener> _listeners = new CopyOnWriteArrayList<>();
	private final AtomicLong _revision = new AtomicLong();
	private volatile boolean _started;

	private GameEventManager()
	{
		for (GameEventType type : GameEventType.values())
		{
			_events.put(type, type.create());
		}

		_sortedByName = _events.values().toArray(new IGameEvent[0]);
		Arrays.sort(_sortedByName, Comparator.comparing(IGameEvent::getDisplayName, String.CASE_INSENSITIVE_ORDER));

		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout event) -> onPlayerLogout(event), this));
	}

	public static void main(String[] args)
	{
		getInstance().start();
	}

	private void start()
	{
		if (_started)
		{
			return;
		}
		_started = true;

		addListener(new GameEventListener()
		{
			@Override
			public void onStateChanged(IGameEvent event, EventState oldState, EventState newState)
			{
				LOGGER.info("GameEvents: " + event.getDisplayName() + " " + oldState + " -> " + newState + " (participants: " + event.getParticipantCount() + ")");
			}
		});

		for (IGameEvent event : _events.values())
		{
			// Только AbstractGameEvent имеет собственный lifecycle.
			// Adapter-ивенты (TvTAdapter) управляются оригиналом через config.xml.
			if (event instanceof AbstractGameEvent abstractEvent)
			{
				abstractEvent.scheduleFirstCycle();
			}
		}

		LOGGER.info("GameEvents: Initialized " + _events.size() + " event(s).");
		for (IGameEvent event : _events.values())
		{
			final long nextStart = event.getNextStartMillis();
			final String nextStr = (nextStart > 0) ? LOG_TIME.format(Instant.ofEpochMilli(nextStart).atZone(ZoneId.systemDefault())) : "config.xml";
			LOGGER.info("GameEvents: " + event.getDisplayName() + " [" + event.getType() + "] state=" + event.getState() + ", next start " + nextStr + ", participants " + event.getParticipantCount() + "/" + event.getMaxParticipants());
		}
	}

	public IGameEvent[] getEventsSorted()
	{
		return _sortedByName;
	}

	public IGameEvent getEvent(GameEventType type)
	{
		return _events.get(type);
	}

	public List<IGameEvent> getActiveEvents()
	{
		final List<IGameEvent> active = new ArrayList<>(4);
		for (IGameEvent event : _events.values())
		{
			if (event.isActive())
			{
				active.add(event);
			}
		}
		return active;
	}

	public void replaceEvent(GameEventType type, IGameEvent implementation)
	{
		final IGameEvent old = _events.put(type, implementation);
		if (old instanceof AbstractGameEvent abstractOld)
		{
			abstractOld.shutdown();
		}
		if (implementation instanceof AbstractGameEvent abstractNew)
		{
			abstractNew.scheduleFirstCycle();
		}
	}

	public long getRevision()
	{
		return _revision.get();
	}

	public void addListener(GameEventListener listener)
	{
		_listeners.add(listener);
	}

	public void removeListener(GameEventListener listener)
	{
		_listeners.remove(listener);
	}

	void fireStateChanged(IGameEvent event, EventState oldState, EventState newState)
	{
		_revision.incrementAndGet();
		for (GameEventListener listener : _listeners)
		{
			listener.onStateChanged(event, oldState, newState);
		}
	}

	void fireParticipantsChanged(IGameEvent event)
	{
		_revision.incrementAndGet();
		for (GameEventListener listener : _listeners)
		{
			listener.onParticipantsChanged(event);
		}
	}

	public void onPlayerLogout(OnPlayerLogout event)
	{
		final int objectId = event.getPlayer().getObjectId();
		for (IGameEvent gameEvent : _events.values())
		{
			if ((gameEvent.getState() == EventState.REGISTERING) && (gameEvent instanceof AbstractGameEvent abstractEvent))
			{
				abstractEvent.removeParticipant(objectId);
			}
		}
	}

	public static GameEventManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		private static final GameEventManager INSTANCE = new GameEventManager();
	}
}
