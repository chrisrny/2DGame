package game.core.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import game.model.combat.Projectile;
import game.model.commun.Faction;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Charger;
import game.model.entity.Enemy;
import game.model.entity.GiantProwler;
import game.model.world.GameMap;

/**
 * Dessin provisoire de la map en formes simples : sol sombre, murs gris, Rôdeurs en disques
 * rouges, Archers en disques orange, Chargeurs en disques jaunes, boss provisoire en grand disque
 * pourpre, projectiles en petits points
 * (orange pour les ennemis, cyan pour le joueur). Les sprites le remplaceront en phase 3.
 */
public final class WorldRenderer {

    /** Durée d'une phase de clignotement pendant l'annonce du Chargeur, en secondes. */
    private static final float WIND_UP_BLINK = 0.08f;

    /**
     * Dessine la map. Le {@link ShapeRenderer} doit déjà être ouvert en mode {@code Filled}.
     *
     * @param shapes le pinceau de formes
     * @param map    la map à dessiner
     */
    public void draw(ShapeRenderer shapes, GameMap map) {
        shapes.setColor(0.07f, 0.06f, 0.09f, 1f);
        rect(shapes, map.arena());
        shapes.setColor(0.35f, 0.34f, 0.38f, 1f);
        for (Rect wall : map.walls()) {
            rect(shapes, wall);
        }
        for (Enemy enemy : map.enemies()) {
            if (enemy instanceof Charger charger) {
                setChargerColor(shapes, charger);
            } else if (enemy instanceof GiantProwler) {
                shapes.setColor(0.62f, 0.12f, 0.38f, 1f);
            } else if (enemy instanceof Archer) {
                shapes.setColor(1f, 0.55f, 0.15f, 1f);
            } else {
                shapes.setColor(0.86f, 0.25f, 0.25f, 1f);
            }
            Vec2 p = enemy.position();
            shapes.circle(p.x(), p.y(), enemy.hitbox().radius(), 20);
        }
        for (Projectile projectile : map.projectiles()) {
            if (projectile.faction() == Faction.ENEMY) {
                shapes.setColor(1f, 0.7f, 0.3f, 1f);
            } else {
                shapes.setColor(0.35f, 0.95f, 0.95f, 1f);
            }
            Vec2 p = projectile.position();
            shapes.circle(p.x(), p.y(), projectile.hitbox().radius(), 8);
        }
    }

    /**
     * Chargeur : jaune ; il clignote en blanc pendant l'annonce pour que le joueur voie venir
     * la charge, et devient terne quand il est étourdi. Le clignotement est calculé à partir
     * du temps de phase donné par le modèle : la vue ne garde aucun minuteur à elle.
     */
    private static void setChargerColor(ShapeRenderer shapes, Charger charger) {
        if (charger.isWindingUp() && (int) (charger.phaseTime() / WIND_UP_BLINK) % 2 == 0) {
            shapes.setColor(1f, 1f, 1f, 1f);
        } else if (charger.isStunned()) {
            shapes.setColor(0.55f, 0.5f, 0.2f, 1f);
        } else {
            shapes.setColor(1f, 0.88f, 0.2f, 1f);
        }
    }

    private static void rect(ShapeRenderer shapes, Rect r) {
        shapes.rect(r.x(), r.y(), r.width(), r.height());
    }
}
