package game.core.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import game.model.combat.Projectile;
import game.model.combat.Telegraph;
import game.model.commun.Faction;
import game.model.entity.Boss;
import game.model.entity.Charger;
import game.model.entity.Enemy;
import game.model.entity.Player;
import game.model.event.EventBus;
import game.model.event.GameEvent;
import game.model.player.state.PlayerStateKind;
import game.model.world.Run;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Le son de la partie : effets et musique ({@code assets/sfx}, {@code assets/music}). */
public final class GameAudio implements Disposable {

    /** Les effets, avec leur volume. */
    enum Sfx {
        SLASH("slash", 0.45f), HIT("hit", 0.55f), CRIT("crit", 0.7f), ENEMY_DEATH("enemy_death", 0.55f),
        PLAYER_HURT("player_hurt", 0.8f), DODGE("dodge", 0.45f), SHOCKWAVE("shockwave", 0.75f),
        BLADE_FAN("blade_fan", 0.6f), EMBER_SPIT("ember_spit", 0.35f), CHARGER_WINDUP("charger_windup", 0.45f),
        CHARGER_CHARGE("charger_charge", 0.5f), BOSS_SLAM("boss_slam", 1f), BOSS_WINDUP("boss_windup", 0.35f),
        BOSS_ROAR("boss_roar", 0.9f), BOSS_DEATH("boss_death", 1f), LEVEL_UP("level_up", 0.7f),
        RELIC_PICK("relic_pick", 0.7f), STAGE_START("stage_start", 0.8f);

        private final String file;
        private final float volume;

        Sfx(String file, float volume) {
            this.file = file;
            this.volume = volume;
        }
    }

    /** Délai minimal entre deux lectures d'un même son, en secondes. */
    private static final float REPEAT_GAP = 0.06f;

    /** Variation de hauteur au hasard, de part et d'autre de la hauteur normale. */
    private static final float PITCH_SPREAD = 0.07f;

    /** Volume de la musique, et durée du fondu entre deux musiques, en secondes. */
    private static final float MUSIC_VOLUME = 0.45f;
    private static final float CROSSFADE = 1.5f;

    private final Map<Sfx, Sound> sounds = new EnumMap<>(Sfx.class);
    private final Map<Sfx, Float> lastPlayed = new EnumMap<>(Sfx.class);
    private final Music quarter;
    private final Music bossMusic;
    private final Random random = new Random();
    private float clock;
    private float fade = 1f;
    private Music current;
    private Music previous;

    // Ce que l'on a vu à l'image précédente.
    private PlayerStateKind lastState;
    private float lastSkill1 = Float.POSITIVE_INFINITY;
    private float lastSkill2 = Float.POSITIVE_INFINITY;
    private boolean wasChoosing;
    private float lastIntro;
    private boolean bossSeen;
    private boolean bossDead;
    private boolean slamming;
    private boolean bossWinding;
    private boolean bossCharging;
    private int bossPhase = 1;
    private boolean ended;
    private final Set<Projectile> knownProjectiles = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Enemy, Integer> chargerStates = new IdentityHashMap<>();

    /**
     * Charge les sons et s'abonne aux événements de la partie.
     *
     * @param events le bus d'événements du modèle
     */
    public GameAudio(EventBus events) {
        for (Sfx sfx : Sfx.values()) {
            sounds.put(sfx, Gdx.audio.newSound(Gdx.files.internal("sfx/" + sfx.file + ".ogg")));
        }
        quarter = Gdx.audio.newMusic(Gdx.files.internal("music/quarter.ogg"));
        bossMusic = Gdx.audio.newMusic(Gdx.files.internal("music/boss.ogg"));
        quarter.setLooping(true);
        bossMusic.setLooping(true);
        Objects.requireNonNull(events, "events").subscribe(this::onEvent);
    }

    private void onEvent(GameEvent event) {
        if (event instanceof GameEvent.EntityHit hit) {
            if (hit.target() == Faction.PLAYER) {
                play(Sfx.PLAYER_HURT);
            } else {
                play(hit.critical() ? Sfx.CRIT : Sfx.HIT);
            }
        } else if (event instanceof GameEvent.EntityDied died && died.faction() == Faction.ENEMY) {
            play(Sfx.ENEMY_DEATH);
        }
    }

    /**
     * Joue un son, sauf s'il vient déjà de partir.
     *
     * @param sfx le son
     */
    void play(Sfx sfx) {
        Float last = lastPlayed.get(sfx);
        if (last != null && clock - last < REPEAT_GAP) {
            return;
        }
        lastPlayed.put(sfx, clock);
        float pitch = 1f + (random.nextFloat() * 2f - 1f) * PITCH_SPREAD;
        sounds.get(sfx).play(sfx.volume, pitch, 0f);
    }

    /**
     * Compare la partie à l'image précédente pour jouer les sons de ce qui commence, et règle la
     * musique. À appeler une fois par image.
     *
     * @param run la partie
     * @param delta temps écoulé depuis l'image précédente, en secondes
     */
    public void update(Run run, float delta) {
        clock += delta;
        Player player = run.player();
        PlayerStateKind state = player.stateKind();
        if (state != lastState) {
            if (state == PlayerStateKind.ATTACKING) {
                play(Sfx.SLASH);
            } else if (state == PlayerStateKind.DODGING) {
                play(Sfx.DODGE);
            }
            lastState = state;
        }
        float since1 = player.skill1().timeSinceActivation();
        if (since1 < lastSkill1) {
            play(Sfx.SHOCKWAVE);
        }
        lastSkill1 = since1;
        float since2 = player.skill2().map(skill -> skill.timeSinceActivation()).orElse(Float.POSITIVE_INFINITY);
        if (since2 < lastSkill2) {
            play(Sfx.BLADE_FAN);
        }
        lastSkill2 = since2;

        listenEnemies(run);
        listenBoss(run);

        if (run.isChoosingRelic() != wasChoosing) {
            play(run.isChoosingRelic() ? Sfx.LEVEL_UP : Sfx.RELIC_PICK);
            wasChoosing = run.isChoosingRelic();
        }
        if (run.stageIntroLeft() > lastIntro + 0.5f) {
            play(Sfx.STAGE_START);                          // un nouvel étage s'annonce
        }
        lastIntro = run.stageIntroLeft();
        if (run.isOver() && !ended) {
            // La musique s'éteint ; le jingle de victoire ou de défaite part avec l'écran de fin.
            ended = true;
            switchTo(null);
        }
        if (!ended) {
            switchTo(run.isBossStage() && !bossDead ? bossMusic : quarter);
        }
        updateMusic(delta);
    }

    private void listenEnemies(Run run) {
        List<Projectile> projectiles = run.map().projectiles();
        for (Projectile projectile : projectiles) {
            if (projectile.faction() == Faction.ENEMY && !knownProjectiles.contains(projectile)) {
                play(Sfx.EMBER_SPIT);
            }
        }
        knownProjectiles.clear();
        knownProjectiles.addAll(projectiles);

        Set<Enemy> alive = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Enemy enemy : run.map().enemies()) {
            if (enemy instanceof Charger charger) {
                alive.add(charger);
                int now = charger.isWindingUp() ? 1 : charger.isCharging() ? 2 : 0;
                Integer before = chargerStates.put(charger, now);
                if (before != null && before != now) {
                    if (now == 1) {
                        play(Sfx.CHARGER_WINDUP);
                    } else if (now == 2) {
                        play(Sfx.CHARGER_CHARGE);
                    }
                }
            }
        }
        chargerStates.keySet().retainAll(alive);
    }

    private void listenBoss(Run run) {
        Optional<Boss> found = run.boss();
        if (found.isEmpty()) {
            return;
        }
        Boss boss = found.get();
        if (!boss.isAlive()) {
            if (bossSeen && !bossDead) {
                play(Sfx.BOSS_DEATH);
            }
            bossDead = true;
            return;
        }
        bossSeen = true;
        Optional<Telegraph> telegraph = boss.currentAttackTelegraph();
        boolean slam = telegraph.isPresent() && telegraph.get().zone().radius() > boss.hitbox().radius() + 1f;
        boolean winding = telegraph.isPresent() && !slam;
        if (slam && !slamming) {
            play(Sfx.BOSS_WINDUP);
        } else if (!slam && slamming) {
            play(Sfx.BOSS_SLAM);
        }
        if (winding && !bossWinding) {
            play(Sfx.CHARGER_WINDUP);
        }
        boolean charging = boss.contactDamage() > 0;
        if (charging && !bossCharging) {
            play(Sfx.CHARGER_CHARGE);
        }
        slamming = slam;
        bossWinding = winding;
        bossCharging = charging;
        if (boss.phaseNumber() != bossPhase) {
            bossPhase = boss.phaseNumber();
            play(Sfx.BOSS_ROAR);
        }
    }

    // Fondu enchaîné : l'ancienne musique baisse pendant que la nouvelle monte.
    private void switchTo(Music next) {
        if (next == current) {
            return;
        }
        if (previous != null) {
            previous.stop();
        }
        previous = current;
        current = next;
        fade = 0f;
        if (current != null) {
            current.setVolume(0f);
            current.play();
        }
    }

    private void updateMusic(float delta) {
        fade = Math.min(1f, fade + delta / CROSSFADE);
        if (current != null) {
            current.setVolume(MUSIC_VOLUME * fade);
        }
        if (previous != null) {
            previous.setVolume(MUSIC_VOLUME * (1f - fade));
            if (fade >= 1f) {
                previous.stop();
                previous = null;
            }
        }
    }

    /** Coupe la musique, par exemple quand on quitte la partie. */
    public void stop() {
        quarter.stop();
        bossMusic.stop();
    }

    @Override
    public void dispose() {
        stop();
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        quarter.dispose();
        bossMusic.dispose();
    }
}
