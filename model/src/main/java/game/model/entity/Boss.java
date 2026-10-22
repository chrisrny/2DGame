package game.model.entity;

import game.model.ai.BossFirstPhase;
import game.model.ai.BossPhase;
import game.model.ai.BossSecondPhase;
import game.model.ai.BossSummons;
import game.model.combat.Hit;
import game.model.combat.Telegraph;
import game.model.commun.Vec2;
import game.model.world.World;
import java.util.List;
import java.util.Optional;

/**
 * Le boss de l'étage 3, l'Oni du quartier : il change de comportement selon ses points de vie.
 *
 * <p>Ce qu'il fait est délégué à sa phase actuelle ({@link BossPhase}, pattern State). Il
 * commence en {@link BossFirstPhase} et passe en phase 2 à {@link #SECOND_PHASE_THRESHOLD} de
 * ses points de vie. Toutes ses attaques sont annoncées : la vue dessine
 * {@link #currentAttackTelegraph()} avant chaque coup.</p>
 *
 * <p>Pendant tout le combat, il appelle aussi des Rôdeurs en renfort ({@link BossSummons}),
 * annoncés par {@link #summonTelegraphs()}.</p>
 *
 * <p>Comme le Chargeur, il ne blesse au contact que pendant sa charge.</p>
 */
public final class Boss extends Enemy {

    /** Points de vie maximum. */
    public static final int MAX_HP = 800;

    /** Rayon de la zone de collision : plus du double d'un Rôdeur. */
    public static final float RADIUS = 20f;

    /** Vitesse de marche, en unités par seconde : un peu plus lent qu'un Rôdeur, on peut le distancer. */
    public static final float SPEED = 55f;

    /** Dégâts infligés au joueur touché pendant la charge. */
    public static final int CHARGE_DAMAGE = 20;

    /** Part des points de vie à laquelle le boss passe en phase 2. */
    public static final float SECOND_PHASE_THRESHOLD = 0.5f;

    private BossPhase phase = new BossFirstPhase();
    private int phaseNumber = 1;
    private final BossSummons summons = new BossSummons();

    /**
     * Crée le boss aux valeurs de référence.
     *
     * @param position position d'apparition
     */
    public Boss(Vec2 position) {
        this(position, 1f);
    }

    /**
     * Crée le boss renforcé (ou affaibli) par le multiplicateur du stage et de la difficulté.
     *
     * @param position   position d'apparition
     * @param multiplier multiplicateur des points de vie et des dégâts
     */
    public Boss(Vec2 position, float multiplier) {
        // Le comportement passé à Enemy renvoie vers la phase du boss : la Strategy des
        // ennemis est ici branchée sur le State du boss.
        super(position, MAX_HP, RADIUS, SPEED, CHARGE_DAMAGE, (self, world, dt) -> ((Boss) self).act(world, dt),
                multiplier);
    }

    private void act(World world, float dt) {
        if (phaseNumber == 1 && hp() <= maxHp() * SECOND_PHASE_THRESHOLD) {
            // Changement d'état (pattern State) : la phase 2 reprend le même cycle, plus vite et
            // plus fort, et chaque frappe projette un anneau de braises.
            phase = new BossSecondPhase();
            phaseNumber = 2;
        }
        phase.update(this, world, dt);
        // Les renforts arrivent pendant tout le combat, quelle que soit la phase.
        summons.update(this, world, dt);
    }

    /**
     * Applique un coup, sans le recul : un boss repoussé à chaque coup serait figé par un joueur
     * qui frappe en boucle, et n'attaquerait plus jamais.
     *
     * @param hit le coup reçu
     * @return {@code true} si le coup a été appliqué
     */
    @Override
    public boolean receive(Hit hit) {
        return super.receive(new Hit(hit.damage(), hit.source(), hit.critical(), Vec2.ZERO));
    }

    /**
     * Dégâts de contact : ceux de la charge pendant qu'il charge, aucun le reste du temps.
     *
     * @return les dégâts infligés au joueur qui le touche maintenant
     */
    @Override
    public int contactDamage() {
        return phase.isCharging() ? super.contactDamage() : 0;
    }

    /**
     * L'attaque que le boss est en train d'annoncer, pour que la vue la dessine.
     *
     * @return l'annonce en cours, ou rien entre deux attaques
     */
    public Optional<Telegraph> currentAttackTelegraph() {
        return phase.telegraph(this);
    }

    /**
     * Les renforts que le boss est en train d'invoquer, pour que la vue dessine leur point
     * d'arrivée avant qu'ils apparaissent.
     *
     * @return une annonce par Rôdeur à venir, vide hors invocation
     * @see BossSummons
     */
    public List<Telegraph> summonTelegraphs() {
        return summons.telegraphs();
    }

    /** @return le nombre de Rôdeurs qu'il a invoqués encore en vie */
    public int summonedAlive() {
        return summons.alive();
    }

    /** @return la phase actuelle du boss : 1, puis 2 à partir de la moitié de ses points de vie */
    public int phaseNumber() {
        return phaseNumber;
    }
}
