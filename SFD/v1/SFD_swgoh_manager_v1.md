# Spécification Fonctionnelle Détaillée — SWGOH Manager

**Projet :** swgoh-manager
**Dépôt :** https://github.com/Xalkinn/swgoh-manager
**Version :** 1.0
**Date :** Août 2026

---

## 1. Introduction

### 1.1 Objet du document

Cette Spécification Fonctionnelle Détaillée (SFD) décrit les fonctionnalités de l'application **SWGOH Manager**, une application web de suivi et de pilotage de guilde pour *Star Wars: Galaxy of Heroes*. Elle sert de référence pour comprendre le périmètre fonctionnel actuel de l'application.

### 1.2 Périmètre

SWGOH Manager centralise les données d'une guilde (roster des joueurs, omicrons, objectifs de progression) extraites via l'API swgoh-comlink (au travers du client [swgoh-comlink-java](https://github.com/Xalkinn/comlink)), les stocke en base MySQL, et les restitue via une interface web (dashboard, suivi joueurs, objectifs, omicrons).

### 1.3 Table des modules

| Trigramme | Module                        |
|-----------|--------------------------------|
| DASH      | Tableau de bord                |
| GSTJOU    | Gestion des joueurs             |
| GSTOBJ    | Gestion des objectifs           |
| SUIOMI    | Suivi des omicrons              |
| GSTEXT    | Gestion des extractions         |
| GSTBAT    | Gestion des batchs              |
| ACT       | Actions diverses                |
| ENF       | Exigences non fonctionnelles    |

---

## 2. Architecture technique

| Composant       | Technologie                          |
|------------------|---------------------------------------|
| Langage          | Java 21                               |
| Framework        | Spring Boot 4.1 (Web MVC, Data JDBC)  |
| Vues             | Thymeleaf                             |
| Base de données  | MySQL                                 |
| Build            | Maven                                 |
| Source de données| API swgoh-comlink, via le client [swgoh-comlink-java](https://github.com/Xalkinn/comlink) |

---

## 3. Tableau de bord (DASH)

### 3.1 Description

Page d'accueil de l'application (`/`). Elle donne une vue synthétique de l'état de la guilde et du système au moment de la connexion.

### 3.2 Exigences fonctionnelles

| ID | Exigence |
|----|----------|
| EX_DASH_01 | Asur la page d'accueil, les statistiques globales de la guilde (nombre de joueurs suivis, nombre d'omicrons, etc.). |
| EX_DASH_02 | Le système doit afficher l'état du système (santé de l'application). |
| EX_DASH_03 | Le système doit afficher la progression de l'extraction de données en cours, lorsqu'une extraction est active. |
| EX_DASH_04 | Le système doit afficher les informations relatives à la dernière actualisation des données. |
| EX_DASH_05 | Le système doit afficher la liste des derniers personnages montés en rang/niveau par les membres de la guilde. |
| EX_DASH_06 | Le système doit afficher l'historique des dernières extractions effectuées. |
| EX_DASH_07 | Le système doit exposer un point d'accès API (`GET /api/extraction`) retournant au format JSON l'état de l'extraction en cours, pour permettre un rafraîchissement dynamique côté client sans recharger la page. |

---

## 4. Gestion des joueurs (GSTJOU)

### 4.1 Description

Page `/joueurs`. Permet de consulter la liste des joueurs de la guilde et l'évolution du roster d'un joueur donné entre deux extractions de données.

### 4.2 Exigences fonctionnelles

| ID | Exigence |
|----|----------|
| EX_GSTJOU_01 | Le système doit afficher la liste de tous les joueurs de la guilde, triée par nom. |
| EX_GSTJOU_02 | Le système doit permettre de sélectionner un joueur afin de consulter l'évolution de son roster. |
| EX_GSTJOU_03 | Par défaut, le système doit comparer automatiquement la dernière extraction disponible à l'extraction précédente pour le joueur sélectionné (recherche « classique »). |
| EX_GSTJOU_04 | Le système doit permettre à l'utilisateur de sélectionner manuellement deux extractions (ancienne et nouvelle) afin de comparer l'évolution du roster du joueur entre ces deux points dans le temps (recherche « avancée »). |
| EX_GSTJOU_05 | Le système doit afficher la liste des extractions disponibles pour permettre la sélection manuelle. |

---

## 5. Gestion des objectifs (GSTOBJ)

### 5.1 Description

Page `/objectifs`. Permet de définir des objectifs de progression par personnage et de suivre l'avancement de chaque joueur de la guilde vis-à-vis de ces objectifs.

### 5.2 Exigences fonctionnelles

| ID | Exigence |
|----|----------|
| EX_GSTOBJ_01 | Le système doit afficher la liste des personnages disponibles pouvant faire l'objet d'un objectif. |
| EX_GSTOBJ_02 | Le système doit permettre de créer un nouvel objectif associé à un personnage. |
| EX_GSTOBJ_03 | Le système doit permettre de supprimer un objectif existant. |
| EX_GSTOBJ_04 | Le système doit afficher la liste des objectifs déjà définis. |
| EX_GSTOBJ_05 | Le système doit permettre de consulter, pour un personnage donné, un comparatif de la progression de chaque joueur de la guilde vis-à-vis de l'objectif défini pour ce personnage. |

---

## 6. Suivi des omicrons (SUIOMI)

### 6.1 Description

Page `/omicrons`. Donne une vue des compétences omicron débloquées au sein de la guilde.

### 6.2 Exigences fonctionnelles

| ID | Exigence |
|----|----------|
| EX_SUIOMI_01 | Le système doit afficher la liste des compétences omicron débloquées au sein de la guilde. |

---

## 7. Gestion des extractions (GSTEXT)

### 7.1 Description

Processus d'extraction des données de guilde depuis l'API swgoh-comlink et de conservation d'un historique des extractions successives, utilisé comme socle par les autres modules (dashboard, joueurs).

### 7.2 Exigences fonctionnelles

| ID | Exigence |
|----|----------|
| EX_GSTEXT_01 | Le système doit permettre d'extraire les données de la guilde (joueurs, personnages, reliques, omicrons) depuis l'API swgoh-comlink via le client swgoh-comlink-java. |
| EX_GSTEXT_02 | Le système doit conserver chaque extraction en base de données (identifiant, date de fin) afin de constituer un historique consultable. |
| EX_GSTEXT_03 | Le système doit permettre de consulter, à tout instant, l'état d'avancement (progression) d'une extraction en cours. |
| EX_GSTEXT_04 | Le système doit permettre d'identifier la dernière et l'avant-dernière extraction disponibles pour un joueur donné, afin d'alimenter les comparatifs automatiques du module GSTJOU. |

---

## 8. Gestion des batchs (GSTBAT)

### 8.1 Description

Traitements asynchrones déclenchables manuellement pour analyser ou mettre à jour les données de la guilde en tâche de fond (`/actions/batch-omicron`, `/actions/batch-roster`).

### 8.2 Exigences fonctionnelles

| ID | Exigence |
|----|----------|
| EX_GSTBAT_01 | Le système doit permettre de déclencher manuellement un batch d'analyse des omicrons de la guilde. |
| EX_GSTBAT_02 | Le système doit empêcher le lancement d'un nouveau batch omicron si un batch est déjà en cours d'exécution. |
| EX_GSTBAT_03 | Le système doit permettre de déclencher manuellement un batch de mise à jour du roster complet de la guilde. |
| EX_GSTBAT_04 | Le système doit permettre de consulter le statut d'exécution des batchs (en cours / terminé). |

---

## 9. Actions diverses (ACT)

### 9.1 Description

Actions complémentaires accessibles depuis l'interface (`/actions/export`, `/actions/refresh`). *Ces deux actions sont actuellement présentes dans le contrôleur mais non implémentées fonctionnellement (stubs).*

### 9.2 Exigences fonctionnelles

| ID | Exigence | Statut |
|----|----------|--------|
| EX_ACT_01 | Le système doit permettre d'exporter les données de la guilde au format CSV. | À implémenter |
| EX_ACT_02 | Le système doit permettre de déclencher un rafraîchissement manuel des données depuis l'interface. | À implémenter |

---

## 10. Exigences non fonctionnelles (ENF)

| ID | Exigence |
|----|----------|
| EX_ENF_01 | L'application doit être développée en Java 21 avec le framework Spring Boot. |
| EX_ENF_02 | Les données doivent être persistées dans une base MySQL. |
| EX_ENF_03 | Les identifiants de connexion à la base de données ne doivent pas être stockés en clair dans le dépôt Git ; ils doivent être externalisés via des variables d'environnement. |
| EX_ENF_04 | L'application doit dépendre du client swgoh-comlink-java pour toute communication avec l'API swgoh-comlink. |

---

## 11. Annexe — Glossaire des trigrammes

- **DASH** : Tableau de bord
- **GSTJOU** : Gestion des joueurs
- **GSTOBJ** : Gestion des objectifs
- **SUIOMI** : Suivi des omicrons
- **GSTEXT** : Gestion des extractions
- **GSTBAT** : Gestion des batchs
- **ACT** : Actions diverses
- **ENF** : Exigences non fonctionnelles