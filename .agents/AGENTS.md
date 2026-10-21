# Directives et Règles du Projet DanaRent

<!-- SECTION: MINECRAFT_DEVELOPMENT -->
## Stack Minecraft & Java
- **API** : Paper 1.21.1 (`io.papermc.paper:paper-api`)
- **Java** : JDK 21
- **Build Tool** : Maven (`pom.xml`)
- **Base de Données** : SQLite via HikariCP
- **Dépendances tierces** : SuperiorSkyblock2, Vault (ExcellentEconomy)

## Règles Spécifiques au Développement de Plugins Minecraft
- **Thread Principal (Tick Thread)** : NE JAMAIS bloquer le thread principal avec des opérations d'I/O lentes (requêtes SQL, lectures/écritures de fichiers lourdes). Toutes les requêtes en base de données doivent s'exécuter de manière asynchrone (`Bukkit.getAsyncScheduler()` ou `CompletableFuture`) avec synchronisation sécurisée sur le tick thread si nécessaire.
- **Gestion de la Mémoire** : Ne stockez JAMAIS d'instances d'objets `Player` de manière statique ou persistante dans des collections (risque majeur de fuite de mémoire). Référencez toujours les joueurs par leur `UUID`. Nettoyez scrupuleusement toutes les collections, tâches et connexions dans `onDisable()`.
- **Événements & Listeners** : Enregistrez tous les écouteurs d'événements (`Listener`) dans la méthode `onEnable()` de la classe principale en utilisant `getServer().getPluginManager().registerEvents(...)`.
- **Zéro Hardcoding (100% YAML)** : Aucun message, préfixe, titre de menu, lore d'item, symbole ou configuration ne doit être codé en dur dans les classes Java. Tout doit être externalisé dans les fichiers YAML (`config.yml`, `messages.yml`, `guis/*.yml`).
- **Adventure MiniMessage** : Tous les composants textuels doivent être parsés avec `MiniMessage.miniMessage().deserialize(...)`. Les anciens codes de couleur avec `§` ou `&` sont strictement proscrits.
<!-- END_SECTION: MINECRAFT_DEVELOPMENT -->

<!-- SECTION: TDD_METHODOLOGY -->
## Méthodologie Test-Driven Development (TDD)
- **Cycle TDD** :
  1. **Red** : Écriture du test unitaire décrivant l'exigence fonctionnelle ou le cas limite (le test doit échouer).
  2. **Green** : Écriture de l'implémentation minimale pour faire passer le test.
  3. **Refactor** : Nettoyage et optimisation du code tout en conservant les tests au vert.
- **Tests Unitaires & Mocks** :
  - Utilisation de **MockBukkit 1.21** pour simuler l'environnement Paper.
  - SQLite in-memory (`jdbc:sqlite::memory:`) pour tester les couches d'accès aux données.
  - Fakes/Mocks pour Vault (`Economy`) et SuperiorSkyblock2 (`SuperiorSkyblockAPI`, `Island`).
<!-- END_SECTION: TDD_METHODOLOGY -->

<!-- SECTION: GITHUB_CONVENTIONS -->
## Conventions de Versioning et GitHub
- **Branches** :
  - `main` : Branche de production stable. Aucun commit direct non revu.
  - `feature/<nom-module>` : Développement d'un module ou d'une fonctionnalité (ex: `feature/module-01-core-db`).
  - `bugfix/<description>` : Correction de bugs.
  - `chore/<description>` : Maintenance, mise à jour des dépendances, outillage.
- **Format des Commits (Conventional Commits)** :
  - Format : `<type>(<scope>): <description>`
  - Types autorisés : `feat`, `fix`, `test`, `refactor`, `style`, `docs`, `chore`, `perf`.
  - Exemples :
    - `feat(escrow): add prorata calculation and transaction logging`
    - `test(database): verify table migration and async plot queries`
<!-- END_SECTION: GITHUB_CONVENTIONS -->
