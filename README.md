# 🌾 DanaRent

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Platform](https://img.shields.io/badge/Platform-Paper%201.21.1-blue.svg)](https://papermc.io/)
[![Build Status](https://img.shields.io/badge/Tests-85%20Passing-brightgreen.svg)]()
[![Database](https://img.shields.io/badge/Database-SQLite%20HikariCP-lightgrey.svg)]()
[![Economy](https://img.shields.io/badge/Economy-Vault%20API-yellow.svg)]()

> **DanaRent** est un plugin de location de parcelles (fermes agricoles et carrières minières) sécurisé et performant pour serveurs **Minecraft Paper 1.21.1 Skyblock** (intégré nativement avec **SuperiorSkyblock2** et **Vault**).

---

## 📖 Sommaire
- [Présentation](#-présentation)
- [Fonctionnalités Clés](#-fonctionnalités-clés)
  - [Types de Parcelles](#1-types-de-parcelles-spécialisés)
  - [Sécurité & Confinement](#2-sécurité--confinement-spatial)
  - [Économie & Séquestre (Escrow)](#3-économie--système-descrow-sécurisé)
  - [Gestion & GUIs](#4-interfaces-graphiques-ergonomiques-guis)
  - [Quotas & Permissions](#5-quotas-et-feature-toggles)
- [Commandes & Permissions](#-commandes--permissions)
- [Configuration (100% YAML & MiniMessage)](#-configuration-et-personnalisation)
- [Architecture & Développement](#-architecture--développement)
- [Prérequis & Installation](#-prérequis--installation)

---

## 🌟 Présentation

Dans un univers Skyblock, les fermes automatisées et générateurs de minerais représentent un investissement colossal en temps et en ressources. **DanaRent** permet aux propriétaires d'îles de monétiser leurs infrastructures en les louant temporairement à d'autres joueurs, tout en garantissant :
- **Une protection totale de l'île hôte** (aucun vol de blocs, d'eau, de lave ou de coffres).
- **Une autonomie complète pour le locataire** (pas besoin d'être ajouté en membre/coop sur l'île).
- **Une économie équilibrée** (système de séquestre avec remboursement au prorata et taxe serveur anti-inflation).

---

## ⚡ Fonctionnalités Clés

### 1. Types de Parcelles Spécialisés
* **`FARM` (Fermes Agricoles)** :
  * Dédié aux cultures végétales (blé, carottes, pommes de terre, betteraves, canne à sucre, verrues du nether, etc.).
  * **Auto-replant intelligent** : les cultures mûres récoltées sont instantanément replantées au stade 0 (la graine nécessaire est automatiquement déduite des drops).
  * Protection contre le piétinement des terres labourées (`FARMLAND`) et blocage de la casse des cultures immatures.
* **`MINING` (Carrières Minières)** :
  * Conçu pour les générateurs de cobblestone, de basalte et de minerais.
  * Whitelist stricte de minerais configurables pour éviter l'extraction de blocs structurels de l'île.
  * Scanner dynamique de composition des générateurs affiché dans le lore de location.

### 2. Sécurité & Confinement Spatial
* **Bypass Sécurisé SuperiorSkyblock2** : Les locataires peuvent récolter/miner au sein de leur parcelle sans aucun droit d'accès au reste de l'île.
* **Confinement Spatial (Tampon 1 bloc)** : Si un locataire s'éloigne de sa parcelle louée pendant la session, il reçoit un avertissement sonore/textuel et est téléporté en douceur au centre de sa parcelle.
* **Protection Stricte** :
  * Interdiction d'ouverture des conteneurs (coffres, fours, shulkers, etc.).
  * Interdiction de pose de blocs (`BlockPlaceEvent`), de déversement de fluides (seaux d'eau/lave) ou d'utilisation d'outils destructeurs.
  * Blocage des explosions, pistons et interactions externes traversant la parcelle.

### 3. Économie & Système d'Escrow Sécurisé
* **Compte Séquestre (Escrow)** : Lors de la location, le montant total du loyer est prélevé au locataire via **Vault** et verrouillé dans le séquestre du plugin.
* **Prorata en cas d'annulation anticipée** :
  * Si le locataire résilie son contrat avant la fin, il est instantanément remboursé à 100% du temps restant non consommé.
  * Le propriétaire perçoit le montant correspondant à la durée réellement utilisée.
* **Taxe Serveur (Money Sink)** :
  * Prélèvement configurable d'un pourcentage de taxe sur les revenus générés (Option A - Money Sink).
  * La taxe n'est appliquée que sur la part consommée (le locataire récupère toujours 100% de son prorata non consommé).
  * Traçage complet dans l'historique des transactions financières SQLite.
* **Caisse de Collecte** : Les gains des propriétaires sont conservés en lieu sûr et récupérables via la commande `/farm collect` ou via l'interface `/farm manage`.

### 4. Interfaces Graphiques Ergonomiques (GUIs)
* **Catalogue Public (`/champs` ou `/farm list`)** :
  * Navigation fluide paginée.
  * Filtrage par type (`TOUS`, `FARM`, `MINING`).
  * Informations complètes sur chaque offre : prix, durée, propriétaire, composition des blocs, statut de disponibilité.
* **Panneau de Gestion Propriétaire (`/farm manage`)** :
  * Visualisation de toutes les parcelles de l'île.
  * Bascule d'activation/mise en location en un clic.
  * Modification du prix et renommage.
  * Collecte instantanée des revenus accumulés.
* **Écran de Confirmation** :
  * Récapitulatif clair avant tout paiement (durée, coût total, localisation, taxe appliquée).

### 5. Quotas et Feature Toggles
* **Quotas Hiérarchiques (LuckPerms & fallback config)** :
  * Quota de création de parcelles par île : `danarent.limit.create.<N>`.
  * Quota de locations simultanées par joueur : `danarent.limit.rent.<N>`.
* **Feature Toggles Dynamiques** :
  * Possibilité de désactiver temporairement un type (`types.farm.enabled: false`, `types.mining.enabled: false`).
  * Bloque la création et suspend élégamment l'accès dans le catalogue avec un indicateur dédié.

---

## 🎮 Commandes & Permissions

### Commandes Joueurs
| Commande | Description | Permission |
| :--- | :--- | :--- |
| `/farm wand` | Obtient la baguette de sélection de zone 3D | `danarent.wand` |
| `/farm define <farm\|mining> <prix> [durée]` | Crée une parcelle sur la sélection active | `danarent.create` |
| `/farm list` (ou `/champs`) | Ouvre le catalogue public des parcelles | `danarent.catalog` |
| `/farm manage` | Ouvre le panneau de gestion de ses parcelles | `danarent.manage` |
| `/farm collect` | Récupère tous les loyers accumulés | `danarent.collect` |
| `/farm cancel [plotId]` | Résilie une location active (avec remboursement au prorata) | `danarent.cancel` |

### Commandes Administrateurs
| Commande | Description | Permission |
| :--- | :--- | :--- |
| `/danarent reload` | Recharge `config.yml`, `messages.yml` et les GUIs | `danarent.admin.reload` |
| `/danarent listall [type]` | Liste toutes les parcelles du serveur et leur statut | `danarent.admin.list` |
| `/danarent info <id>` | Affiche les données détaillées d'une parcelle et son contrat | `danarent.admin.info` |
| `/danarent delete <id>` | Supprime de force une parcelle et clôture le contrat | `danarent.admin.delete` |

---

## ⚙️ Configuration et Personnalisation

Le plugin respecte le principe strict du **100% YAML externalisé** et utilise le moteur de formatage **Adventure MiniMessage** (aucun code couleur `§` ou `&` archaïque).

Exemple d'aperçu de `config.yml` :
```yaml
database:
  file: "danarent.db"
  pool:
    maximum-pool-size: 10
    minimum-idle: 2

types:
  farm:
    enabled: true
  mining:
    enabled: true

economy:
  tax:
    enabled: true
    percentage: 5.0 # 5% prélevés en money sink
    mode: "SINK"

limits:
  creation:
    default: 3 # Parcelles max par île par défaut
  active-rentals:
    default: 1 # Locations simultanées max par joueur par défaut
```

Tous les messages, alertes de confinement, préfixes et lores d'inventaires sont éditables à chaud dans `messages.yml` et `guis/*.yml`.

---

## 🏗️ Architecture & Développement

Le projet est développé selon la méthodologie **TDD (Test-Driven Development)** et suit les règles d'architecture les plus rigoureuses pour l'écosystème Paper :
* **Asynchronisme & Tick Thread** : Toutes les écritures et lectures SQLite transitent par un pool HikariCP en tâche asynchrone, sans jamais geler la boucle principale du serveur.
* **Zéro Fuite Mémoire** : Aucune référence directe persistante d'objets `Player` (uniquement des identifiants `UUID`).
* **Suite de Tests Complète** :
  * **85 tests unitaires** exécutés via **JUnit 5**, **MockBukkit 4.3.0** et **AssertJ**.
  * Base de données de test SQLite in-memory (`jdbc:sqlite::memory:`).
  * Mocks dédiés pour l'API SuperiorSkyblock2 et Vault Economy.

---

## 📦 Prérequis & Installation

### Prérequis
1. **Serveur** : Paper 1.21.1 ou version supérieure compatible.
2. **Java** : JDK 21+.
3. **Dépendances obligatoires** :
   * [SuperiorSkyblock2](https://github.com/BG-Software-LLC/SuperiorSkyblock2)
   * [Vault](https://www.spigotmc.org/resources/vault.34315/) + un plugin d'économie compatible (ex: *ExcellentEconomy*, *EssentialsX*).

### Installation
1. Téléchargez le fichier `DanaRent-1.0.0-SNAPSHOT.jar` (disponible dans les [Releases](https://github.com/DanaKube-Mc/DanaRent/releases)).
2. Déposez-le dans le dossier `plugins/` de votre serveur Minecraft.
3. Démarrez ou redémarrez votre serveur.
4. Personnalisez les configurations générées dans `plugins/DanaRent/` si nécessaire.

### Compilation depuis les sources
```bash
git clone https://github.com/DanaKube-Mc/DanaRent.git
cd DanaRent
mvn clean package
```
Le fichier JAR ombré (*shaded*) contenant toutes les dépendances SQLite et HikariCP sera généré sous `target/DanaRent-1.0.0-SNAPSHOT.jar`.

---

## 📄 Licence
Ce projet est développé pour le réseau **DanaKube**. Tous droits réservés.
