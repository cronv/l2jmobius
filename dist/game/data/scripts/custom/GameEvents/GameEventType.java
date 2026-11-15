package custom.GameEvents;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import custom.GameEvents.events.TeamVsTeamEvent;

/**
 * Каталог типов ивентов. Новый ивент = константа + класс-наследник AbstractGameEvent.
 * НЕ называть EventType — это имя занято ядром (mechanics/events/EventType).
 */
public enum GameEventType
{
	TEAM_VS_TEAM("Team vs Team", TeamVsTeamEvent::new),
	// LAST_MAN_STANDING("Last Man Standing", LastManStandingEvent::new),
	// CAPTURE_THE_FLAG("Capture the Flag", CaptureTheFlagEvent::new),
	;

	// Индекс строится один раз при загрузке класса: lookup без исключений на valueOf.
	private static final Map<String, GameEventType> BY_NAME = new HashMap<>();
	static
	{
		for (GameEventType type : values())
		{
			BY_NAME.put(type.name().toLowerCase(Locale.ROOT), type);
		}
	}

	private final String _displayName;
	private final Supplier<? extends AbstractGameEvent> _factory;

	GameEventType(String displayName, Supplier<? extends AbstractGameEvent> factory)
	{
		_displayName = displayName;
		_factory = factory;
	}

	public String getDisplayName()
	{
		return _displayName;
	}

	public AbstractGameEvent create()
	{
		return _factory.get();
	}

	/** Безопасный разбор bypass-параметра (клиент может прислать что угодно). */
	public static GameEventType fromName(String name)
	{
		return (name == null) ? null : BY_NAME.get(name.toLowerCase(Locale.ROOT));
	}
}
