package game.model.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** Tests du bus d'événements. */
class EventBusTest {

    @Test
    void publierNePrevientPersonneAvantFlush() {
        EventBus bus = new EventBus();
        List<GameEvent> received = new ArrayList<>();
        bus.subscribe(received::add);

        bus.publish(new GameEvent.StageCleared(1));

        assertTrue(received.isEmpty());
        assertEquals(1, bus.pendingCount());
    }

    @Test
    void flushDistribueLesEvenementsDansLOrdre() {
        EventBus bus = new EventBus();
        List<GameEvent> received = new ArrayList<>();
        bus.subscribe(received::add);

        bus.publish(new GameEvent.StageCleared(1));
        bus.publish(new GameEvent.PlayerDied());
        bus.flush();

        assertEquals(List.of(new GameEvent.StageCleared(1), new GameEvent.PlayerDied()), received);
        assertEquals(0, bus.pendingCount());
    }

    @Test
    void unEvenementPublieDansUnEcouteurAttendLeFlushSuivant() {
        EventBus bus = new EventBus();
        List<GameEvent> received = new ArrayList<>();
        bus.subscribe(event -> {
            received.add(event);
            if (event instanceof GameEvent.StageCleared) {
                bus.publish(new GameEvent.RelicChosen("Lame affûtée"));
            }
        });

        bus.publish(new GameEvent.StageCleared(1));
        bus.flush();
        assertEquals(1, received.size());

        bus.flush();
        assertEquals(new GameEvent.RelicChosen("Lame affûtée"), received.get(1));
    }

    @Test
    void unEcouteurDesabonneNeRecoitPlusRien() {
        EventBus bus = new EventBus();
        List<GameEvent> received = new ArrayList<>();
        Consumer<GameEvent> listener = received::add;
        bus.subscribe(listener);
        bus.unsubscribe(listener);

        bus.publish(new GameEvent.PlayerDied());
        bus.flush();

        assertTrue(received.isEmpty());
    }
}
