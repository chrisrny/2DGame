package game.model.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Bus d'événements du jeu (pattern Observer) : le modèle publie, la vue écoute.
 *
 * <p>Publier ne prévient personne tout de suite : l'événement est mis en file. Les écouteurs
 * sont prévenus quand on appelle {@link #flush()}, une fois à la fin de chaque pas de jeu.
 * Un événement publié pendant un {@code flush} est distribué au {@code flush} suivant : on
 * évite ainsi les boucles et les bugs d'ordre quand un écouteur publie à son tour.</p>
 */
public final class EventBus {

    private final List<Consumer<GameEvent>> listeners = new ArrayList<>();
    private List<GameEvent> pending = new ArrayList<>();

    /**
     * Met un événement en file ; il sera distribué au prochain {@link #flush()}.
     *
     * @param event l'événement à publier
     */
    public void publish(GameEvent event) {
        pending.add(Objects.requireNonNull(event, "event"));
    }

    /**
     * Abonne un écouteur à tous les événements.
     *
     * @param listener l'écouteur à prévenir
     */
    public void subscribe(Consumer<GameEvent> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Désabonne un écouteur, par exemple quand un écran est fermé.
     *
     * @param listener l'écouteur à retirer
     */
    public void unsubscribe(Consumer<GameEvent> listener) {
        listeners.remove(listener);
    }

    /**
     * Distribue à tous les écouteurs les événements en file, dans l'ordre de publication.
     */
    public void flush() {
        List<GameEvent> toDeliver = pending;
        pending = new ArrayList<>();
        for (GameEvent event : toDeliver) {
            for (Consumer<GameEvent> listener : List.copyOf(listeners)) {
                listener.accept(event);
            }
        }
    }

    /**
     * Nombre d'événements en attente de distribution.
     *
     * @return le nombre d'événements publiés depuis le dernier {@link #flush()}
     */
    public int pendingCount() {
        return pending.size();
    }
}
