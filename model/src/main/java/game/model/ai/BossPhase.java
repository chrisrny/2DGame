package game.model.ai;

import game.model.combat.Telegraph;
import game.model.entity.Boss;
import game.model.world.World;
import java.util.Optional;

/**
 * Une phase du boss (pattern State) : ce qu'il fait tant qu'il est dans cette phase.
 *
 * <p>Le boss délègue chaque pas de jeu à sa phase actuelle, et change de phase selon ses points
 * de vie. Chaque phase garde son propre état (attaque en cours, minuteurs).</p>
 */
public interface BossPhase {

    /**
     * Fait agir le boss pendant un pas de jeu.
     *
     * @param boss  le boss
     * @param world le monde, pour le joueur, les murs et les cibles
     * @param dt    durée du pas, en secondes
     */
    void update(Boss boss, World world, float dt);

    /**
     * L'attaque en train d'être annoncée, s'il y en a une.
     *
     * @param boss le boss, pour les annonces qui le suivent (sa charge)
     * @return l'annonce en cours, ou rien entre deux attaques
     */
    Optional<Telegraph> telegraph(Boss boss);

    /** @return {@code true} pendant une charge : le seul moment où le boss blesse au contact */
    boolean isCharging();
}
