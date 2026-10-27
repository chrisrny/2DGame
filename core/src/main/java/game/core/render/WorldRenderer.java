package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import game.model.combat.Telegraph;
import game.model.commun.Circle;
import game.model.entity.Boss;
import game.model.world.GameMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * Annonces des attaques du boss et de ses invocations, dessinées en formes par-dessus la scène : la
 * zone en rouge transparent, et un disque plus vif qui grandit avec l'avancement donné par le
 * modèle, jusqu'à la remplir quand le coup part (ou quand le Rôdeur invoqué arrive).
 */
public final class WorldRenderer {

    /** Opacité d'une annonce d'attaque : assez pour la voir, pas assez pour cacher ce qu'il y a dessous. */
    private static final float TELEGRAPH_ALPHA = 0.3f;

    /**
     * Dessine les annonces d'attaque en cours. Ouvre et ferme lui-même le {@link ShapeRenderer}.
     *
     * @param shapes le pinceau de formes, réglé sur la caméra du monde
     * @param map la map, qui contient le boss
     */
    public void drawTelegraphs(ShapeRenderer shapes, GameMap map) {
        List<Telegraph> telegraphs = map.enemies().stream()
                .filter(Boss.class::isInstance)
                .map(Boss.class::cast)
                // Ses attaques et, en phase 2, l'arrivée des Rôdeurs qu'il invoque.
                .flatMap(boss -> Stream.concat(boss.currentAttackTelegraph().stream(),
                        boss.summonTelegraphs().stream()))
                .toList();
        if (telegraphs.isEmpty()) {
            return;
        }
        // La transparence a besoin du mélange de couleurs, activé le temps des annonces seulement.
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0.1f, 0.1f, TELEGRAPH_ALPHA);
        for (Telegraph telegraph : telegraphs) {
            Circle zone = telegraph.zone();
            shapes.circle(zone.center().x(), zone.center().y(), zone.radius(), 40);
            shapes.circle(zone.center().x(), zone.center().y(), zone.radius() * telegraph.progress(), 40);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
