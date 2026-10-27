package game.core.hud;

import com.badlogic.gdx.utils.Pool;

/** Un chiffre de dégâts qui flotte au-dessus d'une cible touchée. */
final class FloatingNumber implements Pool.Poolable {

    float x;
    float y;
    String text;
    boolean critical;
    boolean onPlayer;
    float age;

    /**
     * Prépare le chiffre pour un nouveau coup.
     *
     * @param x position horizontale de départ, dans le monde
     * @param y position verticale de départ, dans le monde
     * @param damage dégâts infligés
     * @param critical {@code true} pour un coup critique
     * @param onPlayer {@code true} si c'est le joueur qui a été touché
     */
    void init(float x, float y, int damage, boolean critical, boolean onPlayer) {
        this.x = x;
        this.y = y;
        this.text = critical ? damage + "!" : String.valueOf(damage);
        this.critical = critical;
        this.onPlayer = onPlayer;
        this.age = 0f;
    }

    @Override
    public void reset() {
        x = 0f;
        y = 0f;
        text = null;
        critical = false;
        onPlayer = false;
        age = 0f;
    }
}
