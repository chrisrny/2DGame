package game.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.ai.BossFirstPhase;
import game.model.ai.ChargerBehavior;
import game.model.combat.Hit;
import game.model.combat.Telegraph;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.event.EventBus;
import game.model.world.FakeWorld;
import game.model.world.GameMap;
import game.model.world.MapLayout;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests du boss et de sa phase 1 : chaque attaque est annoncée avant de frapper. */
class BossTest {

    private static final Vec2 DEPART = new Vec2(500f, 500f);

    // 10 s de jeu au plus pour attendre un événement : bien plus qu'un cycle d'attaques.
    private static final int PAS_MAX = 600;

    /** Cible minimale qui compte les dégâts reçus, et que le test peut déplacer. */
    private static final class Cible implements Damageable {
        private Vec2 position;
        private int degatsRecus;

        Cible(Vec2 position) {
            this.position = position;
        }

        @Override
        public boolean receive(Hit hit) {
            degatsRecus += hit.damage();
            return true;
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public Circle hitbox() {
            return new Circle(position, 6f);
        }

        @Override
        public Faction faction() {
            return Faction.PLAYER;
        }
    }

    private final FakeWorld monde = FakeWorld.empty();
    private final Boss boss = new Boss(DEPART);
    // Assez loin pour que le boss ne soit pas sur lui quand il lance sa charge.
    private final Cible joueur = new Cible(DEPART.add(new Vec2(300f, 0f)));

    BossTest() {
        monde.setPlayerPosition(joueur.position);
        monde.addTarget(joueur);
    }

    private Optional<Telegraph> annonce() {
        return boss.currentAttackTelegraph();
    }

    // Avance pas à pas jusqu'à ce qu'une annonce commence.
    private Telegraph avancerJusquAUneAnnonce() {
        for (int i = 0; i < PAS_MAX && annonce().isEmpty(); i++) {
            boss.update(monde, FixedStep.DT);
        }
        return annonce().orElseThrow();
    }

    // Avance tant que l'annonce dure, en vérifiant qu'aucun dégât ne tombe pendant ce temps.
    // Renvoie la durée de l'annonce, en secondes.
    private float avancerPendantLAnnonceSansDegats() {
        int degatsAvant = joueur.degatsRecus;
        int pas = 0;
        while (annonce().isPresent() && pas < PAS_MAX) {
            assertEquals(degatsAvant, joueur.degatsRecus, "aucun dégât pendant l'annonce");
            assertEquals(0, boss.contactDamage(), "pas de contact blessant pendant l'annonce");
            boss.update(monde, FixedStep.DT);
            pas++;
        }
        return pas * FixedStep.DT;
    }

    @Test
    void laFrappeAuSolTombeApresSonAnnonceEtJamaisAvant() {
        Telegraph frappe = avancerJusquAUneAnnonce();
        assertEquals(BossFirstPhase.SLAM_RADIUS, frappe.zone().radius());
        assertEquals(joueur.position, frappe.zone().center(), "le cercle s'affiche sur le joueur");

        float duree = avancerPendantLAnnonceSansDegats();

        assertEquals(BossFirstPhase.SLAM_WIND_UP, duree, FixedStep.DT);
        assertEquals(BossFirstPhase.SLAM_DAMAGE, joueur.degatsRecus);
    }

    @Test
    void leJoueurQuiSortDuCercleATempsNePrendRien() {
        avancerJusquAUneAnnonce();
        // Le cercle reste là où était le joueur : il s'écarte de 100 u, hors du rayon de 60.
        joueur.position = joueur.position.add(new Vec2(0f, 100f));
        monde.setPlayerPosition(joueur.position);

        avancerPendantLAnnonceSansDegats();

        assertEquals(0, joueur.degatsRecus);
    }

    @Test
    void laChargeSuitLaFrappeEtNeBlesseQuApresSonAnnonce() {
        avancerJusquAUneAnnonce();
        avancerPendantLAnnonceSansDegats();

        // L'attaque suivante est la charge : son annonce est le boss lui-même.
        Telegraph charge = avancerJusquAUneAnnonce();
        assertEquals(Boss.RADIUS, charge.zone().radius());
        float duree = avancerPendantLAnnonceSansDegats();

        assertEquals(ChargerBehavior.WIND_UP_DURATION, duree, FixedStep.DT);
        assertEquals(Boss.CHARGE_DAMAGE, boss.contactDamage(), "la charge blesse une fois annoncée");
    }

    @Test
    void laChargeVaPlusViteQueCelleDuChargeur() {
        avancerJusquAUneAnnonce();
        avancerPendantLAnnonceSansDegats();
        avancerJusquAUneAnnonce();
        avancerPendantLAnnonceSansDegats();

        Vec2 avant = boss.position();
        boss.update(monde, FixedStep.DT);

        float vitesse = boss.position().distanceTo(avant) / FixedStep.DT;
        assertEquals(BossFirstPhase.CHARGE_SPEED, vitesse, 1f);
        assertTrue(vitesse > ChargerBehavior.CHARGE_SPEED);
    }

    @Test
    void lePassageEnPhase2ArriveAMoitieDesPv() {
        boss.receive(new Hit(Boss.MAX_HP / 2 - 1, Faction.PLAYER, false, Vec2.ZERO));
        boss.update(monde, FixedStep.DT);
        assertEquals(1, boss.phaseNumber(), "encore en phase 1 au-dessus de la moitié");

        boss.receive(new Hit(1, Faction.PLAYER, false, Vec2.ZERO));
        boss.update(monde, FixedStep.DT);
        assertEquals(2, boss.phaseNumber(), "phase 2 à la moitié des PV");
    }

    @Test
    void leBossNeReculePasSousLesCoups() {
        boss.receive(new Hit(1, Faction.PLAYER, false, new Vec2(-1000f, 0f)));
        Vec2 avant = boss.position();
        boss.update(monde, FixedStep.DT);

        // Il n'a fait que marcher vers le joueur, à sa vitesse.
        assertTrue(boss.position().distanceTo(avant) <= Boss.SPEED * FixedStep.DT + 0.001f);
        assertTrue(boss.position().x() > avant.x(), "il avance vers le joueur, il ne recule pas");
    }

    @Test
    void leBossSeBatDansUneSalleDeTest() {
        Rect arene = new Rect(0f, 0f, 600f, 400f);
        List<Rect> murs = List.of(new Rect(0f, 0f, 600f, 16f), new Rect(0f, 384f, 600f, 16f),
                new Rect(0f, 16f, 16f, 368f), new Rect(584f, 16f, 16f, 368f));
        Player vraiJoueur = new Player(new Vec2(300f, 200f), PlayerStats.defaults(), new Random(1));
        GameMap salle = new GameMap(new MapLayout(arene, murs, List.of(new Vec2(50f, 50f))), vraiJoueur,
                new EventBus(), new Random(1));
        Boss gardien = new Boss(new Vec2(150f, 200f));
        salle.addEnemy(gardien);

        // Le joueur ne bouge pas : rien ne le touche avant la fin de la première annonce.
        float premierCoup = -1f;
        for (int i = 1; i <= PAS_MAX && vraiJoueur.isAlive(); i++) {
            salle.update(PlayerIntent.NONE, FixedStep.DT);
            if (premierCoup < 0f && vraiJoueur.hp() < vraiJoueur.maxHp()) {
                premierCoup = i * FixedStep.DT;
            }
        }

        float premiereAnnonceFinie = BossFirstPhase.PAUSE_DURATION + BossFirstPhase.SLAM_WIND_UP;
        assertEquals(premiereAnnonceFinie, premierCoup, FixedStep.DT, "la frappe tombe après son annonce");
        assertTrue(vraiJoueur.hp() <= vraiJoueur.maxHp() - BossFirstPhase.SLAM_DAMAGE - Boss.CHARGE_DAMAGE,
                "la frappe puis la charge ont touché le joueur immobile");
    }

    @Test
    void leBossEstInsensibleALEtourdissement() {
        boss.receive(new Hit(10, Faction.PLAYER, false, Vec2.ZERO, 2f));
        assertEquals(Boss.MAX_HP - 10, boss.hp());
        assertTrue(!boss.isStunned(), "l'Onde de choc ne fige pas le boss");
    }

    @Test
    void laChargeAnticipeLeJoueurQuiSEcarte() {
        avancerJusquAUneAnnonce();
        avancerPendantLAnnonceSansDegats();
        avancerJusquAUneAnnonce();

        // Pendant l'annonce de la charge, le joueur s'écarte vers le haut à sa vitesse de marche.
        Vec2 vitesse = new Vec2(0f, 120f);
        for (int i = 0; i < PAS_MAX && annonce().isPresent(); i++) {
            joueur.position = joueur.position.add(vitesse.scale(FixedStep.DT));
            monde.setPlayerPosition(joueur.position);
            boss.update(monde, FixedStep.DT);
        }

        Vec2 avant = boss.position();
        boss.update(monde, FixedStep.DT);
        Vec2 direction = boss.position().sub(avant).normalized();

        // Il vise 70 % du chemin vers là où sera le joueur quand la charge l'atteindra.
        float trajet = avant.distanceTo(joueur.position) / BossFirstPhase.CHARGE_SPEED;
        Vec2 visee = joueur.position.add(vitesse.scale(BossFirstPhase.CHARGE_LEAD * trajet));
        Vec2 attendue = visee.sub(avant).normalized();
        assertEquals(attendue.x(), direction.x(), 0.02f);
        assertEquals(attendue.y(), direction.y(), 0.02f);
        Vec2 versLeJoueur = joueur.position.sub(avant).normalized();
        assertTrue(direction.y() > versLeJoueur.y() + 0.1f, "il fonce devant le joueur, pas sur lui");
    }
}
