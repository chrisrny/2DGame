package game.core.render;

import game.model.ai.ChargerBehavior;
import game.model.combat.Telegraph;
import game.model.commun.Vec2;
import game.model.entity.Boss;
import java.util.Objects;
import java.util.Optional;

/**
 * Déduit la pose du boss à chaque image, à partir de ce que le modèle montre déjà.
 *
 * <p>Le modèle ne dit pas « le boss lève sa massue » : la vue le lit dans ses signes extérieurs.
 * Une annonce plus large que le boss, c'est le coup de massue qui se prépare ; une annonce sur le
 * boss lui-même, c'est l'élan de la charge ; des dégâts au contact, c'est la charge. Les poses qui
 * suivent un geste (impact, étourdissement, sursaut) durent un temps fixe, gardé ici.</p>
 *
 * <p>Cette classe ne dessine rien et ne dépend pas de libGDX : {@link BossSprite} lui demande
 * quoi afficher.</p>
 */
public final class BossPoseTracker {

    /** Ce que fait le boss à l'écran. */
    public enum Pose {
        /** Immobile. */
        IDLE,
        /** Il avance vers le joueur. */
        WALK,
        /** Massue levée pendant l'annonce du coup : le geste suit l'avancement de l'annonce. */
        SLAM_RAISE,
        /** La massue vient de frapper le sol. */
        SLAM_IMPACT,
        /** Il prend son élan avant la charge. */
        CHARGE_WIND_UP,
        /** Il charge. */
        CHARGE,
        /** Sonné après la charge. */
        STUNNED,
        /** Sursaut après un coup reçu, ou au passage en phase 2. */
        FLINCH
    }

    /** Durée de l'animation d'impact, en secondes. */
    public static final float IMPACT_DURATION = 0.32f;

    /** Durée du sursaut après un coup reçu, en secondes. */
    public static final float FLINCH_DURATION = 0.16f;

    /** Durée du flash blanc après un coup reçu, en secondes. */
    public static final float FLASH_DURATION = 0.1f;

    /** Écart horizontal minimal avec le joueur pour se retourner, en unités (évite le clignotement). */
    private static final float FACE_DEAD_ZONE = 8f;

    /** Déplacement minimal entre deux images pour considérer qu'il marche, en unités. */
    private static final float MOVE_EPSILON = 0.05f;

    /**
     * Temps pendant lequel il reste « en marche » après son dernier mouvement, en secondes. À plus
     * de 60 images par seconde, certaines images arrivent sans pas du modèle : sans ce délai, la
     * marche serait coupée une image sur deux.
     */
    private static final float WALK_GRACE = 0.12f;

    private Pose pose = Pose.IDLE;
    private float poseTime;
    private Vec2 position;
    private int lastHp = -1;
    private int lastPhase = 1;
    private boolean wasSlamming;
    private boolean wasCharging;
    private float slamProgress;
    private Vec2 slamCenter = Vec2.ZERO;
    private float impactLeft;
    private float stunLeft;
    private float flinchLeft;
    private float flashLeft;
    private float walkLeft;
    private boolean facingLeft;
    private Vec2 impactNow;
    private boolean phaseChangedNow;

    /**
     * Lit l'état du boss et choisit sa pose. À appeler une fois par image tant que la partie avance.
     *
     * @param boss   le boss
     * @param player la position du joueur, vers qui il se tourne
     * @param delta  temps écoulé depuis l'image précédente, en secondes
     */
    public void update(Boss boss, Vec2 player, float delta) {
        Objects.requireNonNull(boss, "boss");
        Objects.requireNonNull(player, "player");
        Vec2 previous = position == null ? boss.position() : position;
        position = boss.position();
        float dx = position.x() - previous.x();
        boolean moved = position.distanceTo(previous) > MOVE_EPSILON;

        impactLeft = Math.max(0f, impactLeft - delta);
        stunLeft = Math.max(0f, stunLeft - delta);
        flinchLeft = Math.max(0f, flinchLeft - delta);
        flashLeft = Math.max(0f, flashLeft - delta);
        walkLeft = moved ? WALK_GRACE : Math.max(0f, walkLeft - delta);
        impactNow = null;
        phaseChangedNow = false;

        Optional<Telegraph> telegraph = boss.currentAttackTelegraph();
        // L'annonce du coup de massue est bien plus large que le boss ; celle de la charge, c'est lui.
        boolean slamming = telegraph.isPresent() && telegraph.get().zone().radius() > boss.hitbox().radius() + 1f;
        boolean windingUp = telegraph.isPresent() && !slamming;
        boolean charging = boss.contactDamage() > 0;

        if (slamming) {
            slamProgress = telegraph.get().progress();
            slamCenter = telegraph.get().zone().center();
            if (!wasSlamming) {
                // Il se tourne une fois vers sa cible et ne bouge plus pendant le geste.
                facingLeft = slamCenter.x() < position.x();
            }
        } else if (wasSlamming) {
            impactLeft = IMPACT_DURATION;
            impactNow = slamCenter;
        }
        if (wasCharging && !charging) {
            stunLeft = ChargerBehavior.STUN_DURATION;
        }
        if (lastHp >= 0 && boss.hp() < lastHp) {
            flashLeft = FLASH_DURATION;
            flinchLeft = FLINCH_DURATION;
        }
        if (boss.phaseNumber() != lastPhase) {
            phaseChangedNow = true;
            flinchLeft = FLINCH_DURATION * 2;
        }
        wasSlamming = slamming;
        wasCharging = charging;
        lastHp = boss.hp();
        lastPhase = boss.phaseNumber();

        if (charging && moved && Math.abs(dx) > MOVE_EPSILON) {
            facingLeft = dx < 0f;                       // en charge, il regarde où il fonce
        } else if (!slamming && impactLeft <= 0f && !charging && stunLeft <= 0f) {
            // Pendant le geste, l'impact et l'étourdissement, il garde son sens.
            float toPlayer = player.x() - position.x();
            if (Math.abs(toPlayer) > FACE_DEAD_ZONE) {
                facingLeft = toPlayer < 0f;
            }
        }

        Pose next;
        if (slamming) {
            next = Pose.SLAM_RAISE;
        } else if (impactLeft > 0f) {
            next = Pose.SLAM_IMPACT;
        } else if (windingUp) {
            next = Pose.CHARGE_WIND_UP;
        } else if (charging) {
            next = Pose.CHARGE;
        } else if (stunLeft > 0f) {
            next = Pose.STUNNED;
        } else if (flinchLeft > 0f) {
            next = Pose.FLINCH;
        } else {
            next = walkLeft > 0f ? Pose.WALK : Pose.IDLE;
        }
        if (next != pose) {
            pose = next;
            poseTime = 0f;
        } else {
            poseTime += delta;
        }
    }

    /** @return la pose à afficher */
    public Pose pose() {
        return pose;
    }

    /** @return le temps passé dans la pose actuelle, en secondes */
    public float poseTime() {
        return poseTime;
    }

    /** @return l'avancement de l'annonce du coup de massue, de 0 à 1 */
    public float slamProgress() {
        return slamProgress;
    }

    /** @return {@code true} si le boss regarde à gauche (image retournée) */
    public boolean facingLeft() {
        return facingLeft;
    }

    /** @return {@code true} pendant le flash blanc d'un coup reçu */
    public boolean flashing() {
        return flashLeft > 0f;
    }

    /** @return la dernière position connue du boss, ou {@code null} avant la première mise à jour */
    public Vec2 position() {
        return position;
    }

    /** @return le centre du coup de massue s'il vient de frapper à cette image, sinon vide */
    public Optional<Vec2> impactNow() {
        return Optional.ofNullable(impactNow);
    }

    /** @return {@code true} si le boss vient de passer à la phase suivante à cette image */
    public boolean phaseChangedNow() {
        return phaseChangedNow;
    }
}
