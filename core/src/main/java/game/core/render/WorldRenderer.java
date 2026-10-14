package game.core.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import game.model.combat.Projectile;
import game.model.commun.Faction;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Enemy;
import game.model.world.GameMap;

/**
 * Dessin provisoire de la map en formes simples : sol sombre, murs gris, Rôdeurs en disques
 * rouges, Archers en disques orange, projectiles en petits points (orange pour les ennemis,
 * cyan pour le joueur). Les sprites le remplaceront en phase 3.
 */
public final class WorldRenderer {

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
            if (enemy instanceof Archer) {
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

    private static void rect(ShapeRenderer shapes, Rect r) {
        shapes.rect(r.x(), r.y(), r.width(), r.height());
    }
}
