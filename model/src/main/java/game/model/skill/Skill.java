package game.model.skill;

import game.model.entity.Player;
import game.model.world.World;
import java.util.Objects;

/** Une compétence active du joueur, avec son temps de recharge. */
public abstract class Skill {

    private final String name;
    private final float cooldown;
    private float cooldownLeft;
    private float timeSinceActivation = Float.POSITIVE_INFINITY;

    /**
     * Crée une compétence prête à l'emploi.
     *
     * @param name nom affiché au joueur
     * @param cooldown temps de recharge, en secondes, strictement positif
     */
    protected Skill(String name, float cooldown) {
        this.name = Objects.requireNonNull(name, "name");
        if (cooldown <= 0f) {
            throw new IllegalArgumentException("Le temps de recharge doit être positif");
        }
        this.cooldown = cooldown;
    }

    /**
     * Essaie d'utiliser la compétence : refusée tant que la recharge n'est pas finie, sinon l'effet
     * se produit et la recharge repart.
     *
     * @param player le joueur qui utilise la compétence
     * @param world le monde où se produit l'effet
     * @return {@code true} si la compétence a été utilisée
     */
    public final boolean tryActivate(Player player, World world) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(world, "world");
        if (!isReady()) {
            return false;
        }
        activate(player, world);
        cooldownLeft = cooldown;
        timeSinceActivation = 0f;
        return true;
    }

    /**
     * L'effet propre à la compétence. Appelé seulement quand elle est rechargée.
     *
     * @param player le joueur qui utilise la compétence
     * @param world le monde où se produit l'effet
     */
    protected abstract void activate(Player player, World world);

    /**
     * Fait avancer la recharge d'un pas.
     *
     * @param dt durée du pas, en secondes
     */
    public void update(float dt) {
        cooldownLeft = Math.max(0f, cooldownLeft - dt);
        timeSinceActivation += dt;
    }

    /** @return {@code true} si la compétence peut être utilisée maintenant */
    public boolean isReady() {
        return cooldownLeft <= 0f;
    }

    /** @return la part de recharge restante : 1 juste après utilisation, 0 quand elle est prête */
    public float cooldownRatio() {
        return cooldownLeft / cooldown;
    }

    /** @return le temps écoulé depuis la dernière utilisation, en secondes (infini si jamais utilisée) */
    public float timeSinceActivation() {
        return timeSinceActivation;
    }

    /** @return le temps de recharge restant, en secondes */
    public float cooldownLeft() {
        return cooldownLeft;
    }

    /** @return le temps de recharge complet, en secondes */
    public float cooldown() {
        return cooldown;
    }

    /** @return le nom affiché au joueur */
    public String name() {
        return name;
    }
}
