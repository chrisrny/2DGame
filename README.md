# Kagaribi — 2D Game (B-OOP-301)

Action-roguelite en vue de dessus, en Java 21 avec [libGDX](https://libgdx.com/).
Un héros chibi traverse un quartier japonais en flammes, sur une grande carte : deux étages de
vagues d'ennemis, une relique à choisir (1 parmi 3) à chaque niveau gagné, puis le Gardien, un
boss en deux phases. Jouable au clavier et à la souris ou à la manette.

Documentation : [GDD](docs/GDD.md) · [Architecture et design patterns](docs/ARCHITECTURE.md) ·
[Diagrammes UML](docs/uml/)

## Prérequis

- Java 21 (`java -version`)
- Rien d'autre : le wrapper Gradle (`./gradlew`) télécharge Gradle et les dépendances.

## Commandes

| Action | Commande |
|---|---|
| Lancer le jeu | `./gradlew lwjgl3:run` |
| Compiler tout le projet | `./gradlew build` |
| Lancer les tests du modèle + rapport de couverture | `./gradlew model:test` |
| Lancer les tests et afficher la couverture | `./scripts/run_tests.sh` |
| Générer la Javadoc | `./gradlew javadoc` |
| Construire un jar exécutable | `./gradlew lwjgl3:jar` |
| Lancer le jar (déploiement : seul Java 21 est nécessaire) | `java -jar lwjgl3/build/libs/2dgame-0.1.0.jar` |

- Rapport de couverture : `model/build/reports/jacoco/test/html/index.html`
- Javadoc : `<module>/build/docs/javadoc/index.html`
- Jar : `lwjgl3/build/libs/`

## Démo

| Clavier et souris | Manette (Xbox / PlayStation) | Action |
|---|---|---|
| Z Q S D ou flèches | Stick gauche ou croix | Se déplacer |
| Souris | Stick droit (sinon sens de la marche, avec aide à la visée) | Viser |
| Clic gauche | X / □ ou gâchette droite | Attaque de base |
| Clic droit | RB / R1 ou Y / △ | Onde de choc |
| E | LB / L1 ou B / ○ | Éventail de lames |
| Espace | A / ✕ ou gâchette gauche | Esquive |
| Échap | Start | Pause : reprendre, options ou quitter |
| M | Back | Afficher ou masquer la mini-carte |
| 1 2 3 ou clic | Croix puis A / ✕ | Choisir une relique |
| **F9** | — | Triche de démo : victoire immédiate |
| **F10** | — | Triche de démo : défaite immédiate |

La manette est détectée toute seule, branchée avant ou pendant la partie : on passe du clavier à
la manette sans rien régler, et les touches affichées (menus, HUD, cartes) suivent.

Pour la soutenance :

- **Réglage à effet immédiat** : en partie, Échap → Options → « Chiffres de dégâts : non » →
  Échap → Reprendre. Les chiffres de dégâts ne s'affichent plus, sans relancer.
- **Fins de partie** : F9 ou F10 en pleine partie. L'écran de victoire ou de défaite arrive
  1,5 s plus tard ; Entrée relance une partie, Échap revient au titre.
- **Difficulté** : Options → Difficulté (facile ×0,75, normale ×1, difficile ×1,25 sur les PV
  et les dégâts des ennemis), appliquée à partir du stage suivant.

## Architecture

Le projet est découpé en trois modules Gradle :

- **`model`** contient toutes les règles du jeu, en Java pur. Il ne dépend pas de libGDX :
  un `import com.badlogic…` dans ce module ne compile pas. La séparation modèle / vue est
  donc garantie par le build, et le modèle se teste sans ouvrir de fenêtre.
- **`core`** affiche le modèle et lui transmet les intentions du joueur. Il ne décide jamais des règles.
- **`lwjgl3`** est le lanceur desktop.

À chaque image : lecture des entrées → avancée du modèle à pas fixe (1/60 s) → dessin.

```
.
├── settings.gradle          déclaration des 3 modules
├── build.gradle             configuration commune (Java 21, encodage, Javadoc)
├── gradle.properties        versions (libGDX, LWJGL, JUnit)
├── gradlew, gradlew.bat, gradle/wrapper/
├── assets/                  sprites, sons, polices
│
├── model/                   règles du jeu, Java pur, AUCUN import libGDX
│   ├── build.gradle         JUnit 5 + JaCoCo
│   └── src/
│       ├── main/java/game/model/
│       │   ├── commun/        vecteur 2D, configuration, intention du joueur
│       │   ├── event/         événements et bus d'événements (Observer)
│       │   ├── entity/        joueur, ennemis, boss
│       │   ├── player/state/  états du joueur (State)
│       │   ├── combat/        coups, dégâts, collisions
│       │   ├── skill/         compétences et recharges
│       │   ├── relic/         reliques et tirage (Factory)
│       │   ├── ai/            comportements des ennemis (Strategy)
│       │   └── world/         carte, étages, vagues, run
│       └── test/java/game/model/   même arborescence
│
├── core/                    libGDX : affiche et écoute, ne décide rien
│   ├── build.gradle         dépend de :model
│   └── src/main/java/game/core/
│       ├── Main.java        gestion des écrans (extends Game)
│       ├── screen/          titre, partie, pause, options, fin
│       ├── render/          dessin des entités
│       ├── input/           clavier / souris / manette → intention du joueur
│       └── hud/             points de vie, recharges, chiffres de dégâts
│
└── lwjgl3/                  lanceur desktop uniquement
    └── src/main/java/game/lwjgl3/
        ├── Lwjgl3Launcher.java
        └── StartupHelper.java
```

## Conventions

- `main` est toujours jouable ; on n'y pousse jamais directement.
- Une branche par tâche, nommée d'après son identifiant : `feat/P1-A4-esquive`, `fix/P1-B4-coin-mur`.
- Commits au format [Conventional Commits](https://www.conventionalcommits.org/fr/) :
  `feat(player): ajoute l'esquive avec invincibilité`.
- Chaque fusion passe par une pull request relue par l'autre membre du binôme.
- Code, commentaires et Javadoc en français.
