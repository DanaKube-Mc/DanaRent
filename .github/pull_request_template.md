## 📝 Description

<!-- Décrivez brièvement les modifications apportées par cette PR -->

## 🔗 Issue liée

Fixes # <!-- ou Closes # -->

## 🛠️ Type de changement

- [ ] `feat`: Nouvelle fonctionnalité
- [ ] `fix`: Correction de bug
- [ ] `refactor`: Refonte de code sans changement de fonctionnalité
- [ ] `test`: Ajout ou correction de tests unitaires
- [ ] `docs`: Documentation ou wiki
- [ ] `chore`: Tâche de maintenance ou outillage

## ✅ Checklist de Qualité (Directives DanaRent)

- [ ] Le code respecte le standard **Java 21** et **Paper 1.21.1**.
- [ ] **Zéro I/O bloquant** sur le tick thread principal (requêtes DB asynchrones).
- [ ] **Zéro fuite mémoire** : pas d'objets `Player` persistants (stockage par `UUID`).
- [ ] **Zéro hardcoding** : tous les textes/menus sont externalisés en YAML.
- [ ] **MiniMessage** utilisé pour tous les composants textuels (aucun code `§` ou `&`).
- [ ] **TDD / Tests unitaires** : les tests `mvn test` passent à 100% sans régression.
