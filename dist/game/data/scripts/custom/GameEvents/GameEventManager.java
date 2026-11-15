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

/**
 * Singleton-реестр. EnumMap = массив под капотом: O(1).
 * ВАЖНО: циклы ивентов стартуют в start() ПОСЛЕ публикации INSTANCE,
 * иначе openRegistration() внутри конструктора получит getInstance() == null.
 */
public final class GameEventManager
{
	private final EnumMap<GameEventType, AbstractGameEvent> _events = new EnumMap<>(GameEventType.class);
	private final IGameEvent[] _sortedByName; // сортировка A...Z один раз при старте
	private final List<GameEventListener> _listeners = new CopyOnWriteArrayList<>();
	private final AtomicLong _revision = new AtomicLong(); // версия для кэша доски
	private volatile boolean _started;

    private static final Logger LOGGER = Logger.getLogger(GameEventManager.class.getName());
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm");

	private GameEventManager()
	{
		for (GameEventType type : GameEventType.values())
		{
			_events.put(type, type.create());
		}

		_sortedByName = _events.values().toArray(new IGameEvent[0]);
		Arrays.sort(_sortedByName, Comparator.comparing(IGameEvent::getDisplayName, String.CASE_INSENSITIVE_ORDER));

		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout event) -> onPlayerLogout(event), this));

		// Циклы здесь НЕ стартуем — INSTANCE ещё не присвоен (reentrancy в <clinit>).
	}

	/** ScriptEngine вызывает main у скриптов датапака. */
	public static void main(String[] args)
	{
		getInstance().start(); // INSTANCE уже опубликован — обратные вызовы безопасны
	}

	/** Запуск циклов после полной инициализации. Идемпотентен. */
    private void start()
    {
        if (_started)
        {
            return;
        }
        _started = true;

        for (AbstractGameEvent event : _events.values())
        {
            event.scheduleFirstCycle();
        }

        LOGGER.info("GameEvents: Initialized " + _events.size() + " event(s).");
        for (AbstractGameEvent event : _events.values())
        {
            LOGGER.info("GameEvents: " + event.getDisplayName() + " [" + event.getType() + "] state=" + event.getState() //
                + ", next start " + LOG_TIME.format(Instant.ofEpochMilli(event.getNextStartMillis()).atZone(ZoneId.systemDefault())) //
                + ", participants " + event.getParticipantCount() + "/" + event.getMaxParticipants());
        }
    }

	/** Все ивенты, отсортированные A...Z. Read-only по соглашению. */
	public IGameEvent[] getEventsSorted()
	{
		return _sortedByName;
	}

	public IGameEvent getEvent(GameEventType type)
	{
		return _events.get(type);
	}

	/** Все "действующие" ивенты через bool active. */
	public List<IGameEvent> getActiveEvents()
	{
		final List<IGameEvent> active = new ArrayList<>(4);
		for (AbstractGameEvent event : _events.values())
		{
			if (event.isActive())
			{
				active.add(event);
			}
		}
		return active;
	}

	/** Замена дефолтной реализации типа своей (DI для сторонних dev). */
	public void replaceEvent(GameEventType type, AbstractGameEvent implementation)
	{
		final AbstractGameEvent old = _events.put(type, implementation);
		if (old != null)
		{
			old.shutdown();
		}
		implementation.scheduleFirstCycle();
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

	/** Listener ядра: чистим регистрации вышедших игроков (только на этапе регистрации). */
	public void onPlayerLogout(OnPlayerLogout event)
	{
		final int objectId = event.getPlayer().getObjectId();
		for (AbstractGameEvent gameEvent : _events.values())
		{
			if (gameEvent.getState() == EventState.REGISTERING)
			{
				gameEvent.removeParticipant(objectId);
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
