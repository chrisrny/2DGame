package game.model.commun;

/**
 * Statistiques du joueur. Ce sont les valeurs de base ; les reliques produisent
 * de nouvelles statistiques à partir de celles-ci, sans jamais les modifier.
 *
 * <p>Les valeurs par défaut sont un point de départ, ajustées pendant l'équilibrage (P2-X1).</p>
 *
 * @param speed          vitesse de déplacement, en unités par seconde
 * @param maxHp          points de vie maximum
 * @param attackDamage   dégâts de l'attaque de base
 * @param attackRange    portée de l'attaque de base, en unités
 * @param attackCooldown temps minimum entre deux attaques, en secondes
 * @param critChance     probabilité d'un coup critique, entre 0 et 1
 * @param dodgeDistance  distance parcourue pendant une esquive, en unités
 * @param dodgeDuration  durée d'une esquive, en secondes
 * @param dodgeCooldown  temps de recharge de l'esquive, en secondes
 */
public record PlayerStats(
        float speed,
        int maxHp,
        int attackDamage,
        float attackRange,
        float attackCooldown,
        float critChance,
        float dodgeDistance,
        float dodgeDuration,
        float dodgeCooldown) {

    /**
     * Vérifie que les statistiques sont cohérentes.
     *
     * @throws IllegalArgumentException si une valeur est hors de son intervalle
     */
    public PlayerStats {
        if (speed < 0f || maxHp <= 0 || attackDamage < 0 || attackRange < 0f
                || attackCooldown < 0f || dodgeDistance < 0f || dodgeDuration < 0f || dodgeCooldown < 0f) {
            throw new IllegalArgumentException("Statistiques du joueur invalides");
        }
        if (critChance < 0f || critChance > 1f) {
            throw new IllegalArgumentException("La chance de critique doit être entre 0 et 1");
        }
    }

    /**
     * Statistiques de départ du joueur.
     *
     * @return les valeurs par défaut
     */
    public static PlayerStats defaults() {
        return new PlayerStats(120f, 100, 10, 28f, 0.4f, 0.10f, 48f, 0.2f, 0.8f);
    }
}
