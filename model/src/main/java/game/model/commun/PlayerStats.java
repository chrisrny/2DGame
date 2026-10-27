package game.model.commun;

/**
 * Statistiques du joueur. Ce sont les valeurs de base ; les reliques produisent de nouvelles
 * statistiques à partir de celles-ci, sans jamais les modifier.
 *
 * @param speed vitesse de déplacement, en unités par seconde
 * @param maxHp points de vie maximum
 * @param attackDamage dégâts de l'attaque de base
 * @param attackRange portée de l'attaque de base, en unités
 * @param attackCooldown temps minimum entre deux attaques, en secondes
 * @param critChance probabilité d'un coup critique, entre 0 et 1
 * @param dodgeDistance distance parcourue pendant une esquive, en unités
 * @param dodgeDuration durée d'une esquive, en secondes
 * @param dodgeCooldown temps de recharge de l'esquive, en secondes
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

    /** Statistiques de départ du joueur. */
    public static PlayerStats defaults() {
        return new PlayerStats(120f, 100, 12, 36f, 0.4f, 0.10f, 48f, 0.2f, 0.6f);
    }

    /**
     * Copie avec une autre vitesse. Les reliques s'en servent pour produire de nouvelles
     * statistiques sans modifier celles-ci.
     *
     * @param value la nouvelle vitesse, en unités par seconde
     * @return une copie avec cette vitesse
     */
    public PlayerStats withSpeed(float value) {
        return new PlayerStats(value, maxHp, attackDamage, attackRange, attackCooldown, critChance,
                dodgeDistance, dodgeDuration, dodgeCooldown);
    }

    /**
     * Copie avec d'autres points de vie maximum.
     *
     * @param value les nouveaux points de vie maximum
     * @return une copie avec ces points de vie maximum
     */
    public PlayerStats withMaxHp(int value) {
        return new PlayerStats(speed, value, attackDamage, attackRange, attackCooldown, critChance,
                dodgeDistance, dodgeDuration, dodgeCooldown);
    }

    /**
     * Copie avec d'autres dégâts d'attaque.
     *
     * @param value les nouveaux dégâts
     * @return une copie avec ces dégâts
     */
    public PlayerStats withAttackDamage(int value) {
        return new PlayerStats(speed, maxHp, value, attackRange, attackCooldown, critChance,
                dodgeDistance, dodgeDuration, dodgeCooldown);
    }

    /**
     * Copie avec une autre chance de critique.
     *
     * @param value la nouvelle chance de critique, entre 0 et 1
     * @return une copie avec cette chance de critique
     */
    public PlayerStats withCritChance(float value) {
        return new PlayerStats(speed, maxHp, attackDamage, attackRange, attackCooldown, value,
                dodgeDistance, dodgeDuration, dodgeCooldown);
    }

    /**
     * Copie avec une autre recharge d'esquive.
     *
     * @param value la nouvelle recharge d'esquive, en secondes
     * @return une copie avec cette recharge
     */
    public PlayerStats withDodgeCooldown(float value) {
        return new PlayerStats(speed, maxHp, attackDamage, attackRange, attackCooldown, critChance,
                dodgeDistance, dodgeDuration, value);
    }
}
