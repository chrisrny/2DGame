# Game Design Document — Kagaribi

> **Version 1 — 19/10/2026.** Mise à jour du GDD v0 du 01/10/2026 (Imran) d'après le jeu
> réellement implémenté. Les valeurs ci-dessous sont celles du code (constantes du module
> `model`). Les écarts avec la v0 sont listés en [section 10](#10-écarts-avec-le-gdd-v0).

**Un héros chibi traverse un quartier japonais en flammes, la nuit. Il survit à deux étages
de vagues d'ennemis, choisit une relique à chaque niveau gagné, puis affronte l'Oni du quartier,
un boss en deux phases.**

## Sommaire

1. [Concept](#1-concept)
2. [Boucle de jeu](#2-boucle-de-jeu)
3. [Personnage et combat](#3-personnage-et-combat)
4. [Ennemis et boss](#4-ennemis-et-boss)
5. [Reliques](#5-reliques)
6. [Contrôles](#6-contrôles)
7. [Interface, écrans et options](#7-interface-écrans-et-options)
8. [Direction artistique et son](#8-direction-artistique-et-son)
9. [Périmètre](#9-périmètre)
10. [Écarts avec le GDD v0](#10-écarts-avec-le-gdd-v0)

## 1. Concept

Jeu d'action PvE en vagues, inspiré de *Cinderia* : on en garde le combat (vagues d'ennemis,
compétences, reliques, boss). Il n'y a pas de progression d'une partie à l'autre : chaque run
se joue d'une traite, et tout repart de zéro à la suivante.

| Élément | Choix |
|---|---|
| Nom | **Kagaribi** (篝火, « feu de veille ») |
| Genre | Action en vagues, solo, temps réel |
| Vue | De dessus, caméra qui suit le joueur sur une grande carte |
| Ambiance | Quartier japonais de nuit, en flammes ; personnages chibi |
| Durée d'une run | 2 à 4 minutes (2 étages + boss) |
| Plateforme | Desktop (macOS, Windows, Linux), clavier + souris ou manette |
| Technique | Java 21, libGDX, résolution logique 640 × 360 (fenêtre 1280 × 720) |

## 2. Boucle de jeu

Une run se joue sur **une seule carte de 1920 × 1080** pixels (canaux, ponts, torii, ruines,
piliers), avec 16 points d'apparition. On ne marche pas dans l'eau des canaux, mais les
projectiles et les sorts passent au-dessus (les Archers peuvent tirer par-dessus).
La run se découpe en 3 étages :

| Étage | Contenu | Pour finir l'étage | Multiplicateur ennemis |
|---|---|---|---|
| 1 | Vague 1 : 4 Rôdeurs · Vague 2 : 3 Rôdeurs + 2 Archers · Vague 3 : 4 Rôdeurs + 3 Archers | 16 ennemis tués | × 1 |
| 2 | Vague 1 : 4 Rôdeurs + 1 Archer + 1 Chargeur · Vague 2 : 4 R + 3 A + 2 C · Vague 3 : 5 R + 3 A + 3 C | 26 ennemis tués | × 1,2 |
| 3 | L'Oni du quartier (boss), dans l'arène centrale | Tuer le boss | × 1 |

- **Vagues** : la vague suivante apparaît 1,5 à 2 s après la mort du dernier ennemi, juste hors
  de l'écran : entre 380 et 560 px du joueur, toute la vague du même côté (± 75°), sur une case
  libre d'où l'on peut le rejoindre. Faute de place, elle utilise les points d'apparition fixes
  (à au moins 200 px). Après la dernière vague, elle revient tant que l'étage n'est pas
  fini.
- **Entre deux étages** : bandeau d'annonce de 2 s et **soin de 30 % des PV max**.
- **Progression** : chaque ennemi tué rapporte 1 XP. Il faut 5 XP pour le niveau 2, puis
  3 de plus à chaque niveau (5, 8, 11…). **À chaque niveau gagné, le jeu se fige et propose
  1 relique parmi 3** tirées au hasard parmi celles que le joueur n'a pas encore.
- **Victoire** : le boss tombe à 0 PV. **Défaite** : le joueur tombe à 0 PV. L'écran de fin
  arrive 1,5 s plus tard, avec le bilan de la run.
- **Difficulté** : multiplie les PV et les dégâts des ennemis (voir [section 7](#7-interface-écrans-et-options)).
- Chaque run est tirée d'une graine aléatoire : même graine, mêmes apparitions et mêmes
  reliques proposées.

## 3. Personnage et combat

Un seul héros, une attaque de base, une esquive et deux compétences.

| Statistique | Valeur |
|---|---|
| PV max | 100 |
| Vitesse | 120 px/s |
| Attaque de base | 12 dégâts, zone de 36 px devant le joueur, recharge 0,4 s |
| Coup critique | 10 % de chance, dégâts × 2 |
| Recul infligé | 300 px/s |
| Esquive | 48 px en 0,2 s, **invulnérable** pendant l'esquive, recharge 0,6 s |
| Invincibilité après un coup reçu | 0,8 s (le héros clignote) |

| Compétence | Effet | Dégâts | Recharge |
|---|---|---|---|
| **Onde de choc** | Zone autour du joueur, rayon 96 px ; repousse (400 px/s) et étourdit 0,5 s | 28 | 5 s |
| **Éventail de lames** | 7 lames en éventail de 60° vers la visée, 380 px/s ; chaque lame traverse 2 ennemis | 9 par lame | 2,5 s |

Règles :

- Les dégâts du joueur = dégâts de base modifiés par les reliques, × 2 en cas de critique.
- On ne peut pas attaquer ni lancer de compétence pendant une esquive, un recul ou la mort.
- Les ennemis ne se chevauchent pas entre eux (ils s'écartent) et contournent les murs grâce à
  une grille de navigation.

## 4. Ennemis et boss

Trois ennemis aux comportements opposés, plus un boss qui change de comportement à mi-vie.
**Lisibilité : toute attaque ennemie dangereuse est annoncée avant de frapper.**

| Ennemi | Comportement | PV | Dégâts | Vitesse |
|---|---|---|---|---|
| **Rôdeur** | Fonce sur le joueur, frappe au contact | 30 | 10 (contact) | 70 px/s |
| **Archer** | Reste entre 120 et 180 px du joueur, tire toutes les 1,5 s s'il le voit (portée 260 px) | 20 | 8 (flèche, 200 px/s) ; 4 au contact | 55 px/s |
| **Chargeur** | S'approche ; à moins de 160 px avec un chemin dégagé, **se fige 0,6 s (annonce)**, charge en ligne droite à 300 px/s, puis reste étourdi 1 s (plus tôt s'il percute un mur) | 40 | 15 (pendant la charge seulement) | 60 px/s |

Les dégâts de contact ne se répètent pas plus d'une fois toutes les 0,6 s par ennemi.

### Boss — l'Oni du quartier

800 PV, 55 px/s, insensible au recul. Arène fixe au centre de la carte.

- **Phase 1 (plus de 50 % des PV)** : poursuit le joueur pendant 1 s, puis alterne :
  - **Frappe au sol** : un cercle de 60 px apparaît sous le joueur et se remplit pendant
    **0,8 s**, puis inflige 20 dégâts dans la zone ;
  - **Charge** : comme le Chargeur (annonce, charge à 400 px/s, 20 dégâts, étourdi après).
- **Phase 2 (50 % des PV et moins)** : mêmes attaques, plus rapides et plus larges (pause
  0,6 s, cercle de 72 px annoncé 0,7 s, charge à 470 px/s). **Chaque frappe libère 8 braises
  en cercle** (6 dégâts chacune).

## 5. Reliques

8 reliques : 5 modifient une statistique, 3 ont un effet spécial. Une relique déjà possédée
n'est plus proposée.

| Relique | Effet |
|---|---|
| Lame affûtée | +20 % de dégâts |
| Bottes de cendre | +15 % de vitesse |
| Cœur de braise | +20 PV max (et les PV gagnés sont donnés tout de suite) |
| Plume d'ombre | −20 % de recharge d'esquive |
| Œil du chasseur | +10 % de chance de critique |
| Croc vampire | Chaque coup qui touche soigne 1 PV, sorts compris (chaque ennemi pris dans l'Onde de choc, chaque lame de l'Éventail) |
| Pas de braise | L'esquive laisse 3 zones de feu (rayon 12 px, 1,5 s) qui infligent 3 dégâts toutes les 0,5 s |
| Écho | 1 attaque sur 4 frappe une seconde fois, 0,15 s plus tard |

## 6. Contrôles

| Action | Clavier + souris | Manette (Xbox) |
|---|---|---|
| Se déplacer | Z Q S D ou flèches | Stick gauche ou croix |
| Viser | Souris | Stick droit (relâché : direction du déplacement), avec aide à la visée |
| Attaque de base | Clic gauche | X ou RT |
| Esquive | Espace | A ou LT |
| Onde de choc | Clic droit | RB ou Y |
| Éventail de lames | E | LB ou B |
| Pause | Échap | Start |
| Afficher / masquer la mini-carte | M | Back |
| Triche de démo : victoire / défaite | F9 / F10 | — |

Clavier et manette marchent en même temps : la visée suit le dernier périphérique utilisé.

## 7. Interface, écrans et options

- **HUD** (en haut à gauche) : portrait du héros, barre de vie (la vie perdue reste visible un
  instant), barre d'expérience et niveau, jauge d'esquive, les deux compétences avec leur
  recharge, reliques possédées. Voile rouge sur les bords quand la vie est basse.
- **Plaque d'étage** (en haut à droite) : numéro de l'étage, ennemis vaincus et jauge ; au
  début de chaque étage, une annonce avec son nom.
- **Mini-carte** (en haut à droite, sous la plaque d'étage) : toute la carte en petit, le héros,
  les ennemis et la zone visible à l'écran. Se masque avec M (Back à la manette).
- **Barre du boss** (en haut au centre) pendant le combat final, avec un repère au seuil de la
  phase 2.
- **Chiffres de dégâts** au-dessus des cibles, critiques en plus gros.
- **Écrans** : titre, partie, pause (reprendre, options, quitter), options, choix de relique,
  victoire, défaite.
- **Options** :
  - chiffres de dégâts oui / non, **à effet immédiat** (démonstration demandée en soutenance) ;
  - difficulté Facile / Normale / Difficile = × 0,75 / × 1 / × 1,25 sur les PV et les dégâts
    des ennemis, appliquée à partir de l'étage suivant.

## 8. Direction artistique et son

- **Décor** : quartier des plaisirs japonais la nuit, en flammes ; canaux, ponts, torii,
  lanternes. Éclairage dynamique (nuit, lumières qui vacillent), braises et fumée.
- **Personnages** : chibi (grosse tête, petit corps), sprites animés pour le
  héros, les trois ennemis et le boss.
- **Couleurs de lecture** : rouge = danger (zones d'attaque annoncées, ennemis),
  or / orange = récompenses et feu, cyan = magie et compétences.
- **Lisibilité avant tout** : chaque attaque ennemie est annoncée par une forme au sol ou un
  temps d'arrêt ; secousse d'écran quand une compétence frappe, et bref arrêt sur image pour
  l'Onde de choc.
- **Son** : trois musiques qui bouclent sans couture : le quartier (1 min 30, en deux parties),
  le combat contre le boss, et un thème calme pour le titre et les écrans de fin ; une vingtaine
  d'effets (coups,
  critiques, esquive, compétences, annonces du Chargeur et du boss, montée de niveau,
  victoire, défaite).

## 9. Périmètre

| Priorité | Contenu | État |
|---|---|---|
| MVP | 1 personnage (attaque, esquive, 2 compétences), 3 ennemis, boss à 2 phases, étages et vagues, 8 reliques, HUD, chiffres de dégâts, écrans titre / options / pause / relique / fin, tests + couverture, Javadoc, UML | Fait |
| Bonus | Sprites chibi et animations | Fait |
| Bonus | Sons et musique | Fait |
| Bonus | Effets visuels : secousse d'écran, particules, éclairage | Fait |
| Bonus | Jouabilité à la manette | Fait |
| Bonus | Amélioration permanente entre les runs (sauvegardée) | Coupé |

## 10. Écarts avec le GDD v0

Le passage des salles à une grande carte a été décidé en octobre ; l'amélioration permanente
entre les runs a été coupée au profit de l'équilibrage d'une run complète.

| GDD v0 | Jeu actuel |
|---|---|
| 3 salles fermées par des portes, 4 arènes tirées au hasard | Une grande carte 1920 × 1080, 2 étages de vagues puis le boss |
| 1 relique après chaque salle | 1 relique à chaque niveau gagné (XP par ennemi tué) |
| Compétence « Lame lancée » (1 projectile) | « Éventail de lames » (7 lames qui traversent 2 ennemis) |
| Onde de choc sur A | Onde de choc sur clic droit |
| Reliques Sablier et Vampirisme (soin par ennemi tué) | Plume d'ombre (recharge d'esquive) et Croc vampire (soin par coup) |
| Écho : la Lame lancée tire 2 projectiles | Écho : 1 attaque sur 4 frappe deux fois |
| Boss phase 2 : cercle de projectiles + invocation de 2 Rôdeurs | Phase 2 : attaques plus rapides + 8 braises après chaque frappe, pas d'invocation |
| Difficile × 1,5 | Difficile × 1,25 |
| Amélioration permanente entre les runs | Coupée |
| Pause automatique si la manette est débranchée | Non implémentée |
