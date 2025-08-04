# Plugin Minecraft Civilization

Plugin Java Minecraft reproduisant le jeu Civilization 6 dans un environnement Minecraft multijoueur. Ce plugin transforme Minecraft en un jeu de stratégie où les joueurs construisent des empires, gèrent des villages, commercent et se font la guerre.

## 🏗️ Architecture Technique

### Technologies Utilisées
- **Java 21** - Langage principal (mise à jour depuis Java 17)
- **Paper API 1.21.8** - API Minecraft moderne pour les performances optimales (mise à jour depuis 1.18.2)
- **JsonDB** - Base de données JSON pour la persistence
- **Maven** - Gestionnaire de dépendances

### Structure du Projet

```
src/main/java/TestJava/testjava/
├── TestJava.java              # Point d'entrée principal du plugin
├── Config.java                # Configuration centralisée
├── models/                    # Modèles de données (POJOs avec annotations JsonDB)
├── repositories/              # Couche d'accès aux données (pattern Repository)
├── services/                  # Logique métier encapsulée
├── commands/                  # Handlers de commandes joueur
├── threads/                   # Tâches asynchrones et simulation
├── helpers/                   # Utilitaires et fonctions d'aide
└── classes/                   # Classes personnalisées
```

## 🎯 Concepts Fondamentaux

### Empires et Villages
- **Empire** : Appartient à un joueur, peut être en guerre, possède des juridictions
- **Village** : Centre de civilisation avec population, garnison, armée et points de prospérité
- **Protection territoriale** : Chaque village protège un rayon défini par `Config.VILLAGE_PROTECTION_RADIUS`

### Systèmes de Jeu

#### 1. Système de Villages
- Création avec une cloche (`Material.BELL`)
- Conquête avec un bloc de diamant (`Material.DIAMOND_BLOCK`)
- Gestion de la population via les lits
- Points de prospérité liés à l'alimentation

#### 2. Système de Guerre
- Déclaration de guerre entre empires
- Placement de TNT uniquement en territoire ennemi pendant la guerre
- Threads dédiés pour la gestion des conflits

#### 3. Système de Commerce
- Marché mondial avec prix dynamiques
- Calculs basés sur la juridiction
- Ressources définies dans `resources.json`

#### 4. Système de Villageois
- IA pour la recherche de nourriture
- Reproduction automatique
- Gestion de l'alimentation et de la satisfaction

## 📁 Détail des Couches

### Models (Modèles de Données)
Utilisation de **JsonDB** avec annotations pour la persistence :

```java
@Document(collection = "villages", schemaVersion = "1.0")
public class VillageModel {
    @Id
    private String id;
    // Propriétés avec getters/setters
}
```

**Modèles principaux :**
- `EmpireModel` : Gestion des empires et guerres
- `VillageModel` : Villages avec population et stats
- `VillagerModel` : Villageois individuels avec IA
- `BuildingModel` : Bâtiments avec coûts et niveaux
- `ResourceModel` : Ressources et économie
- `DelegationModel` : Système de délégation/commerce
- `EatableModel` : Nourriture et agriculture
- `WarBlockModel` : Blocs liés aux guerres

### Repositories (Accès aux Données)
Pattern Repository pour l'abstraction de la persistence :

```java
public class VillageRepository {
    public static void update(VillageModel village) {
        TestJava.database.upsert(village);
    }
    
    public static VillageModel get(String id) {
        return TestJava.database.findById(id, VillageModel.class);
    }
    
    public static Collection<VillageModel> getAll() {
        return TestJava.database.findAll(VillageModel.class);
    }
}
```

### Services (Logique Métier)
Encapsulation de la logique complexe :

- `BlockProtectionService` : Protection territoriale et validations
- `VillageService` : Gestion des villages (création, conquête)
- `PlayerService` : Gestion des joueurs et empires
- `EntityService` : Gestion des entités personnalisées
- `VillagerService` : IA des villageois
- `WarBlockService` : Gestion des conflits
- `ItemService` & `InventoryService` : Gestion des objets

### Helpers (Utilitaires)
Classes d'aide avec logique spécialisée :

- `Colorize` : Formatage des messages avec couleurs
- `CustomName` : Gestion des noms personnalisés d'entités
- `EatableHelper` : Utilitaires pour la nourriture
- `JuridictionHelper` : Calculs économiques et territoriaux
- `ResourceHelper` : **Recherche intelligente de ressources avec suggestions** ✨

### Commands (Interface Joueur)
Implémentation de `CommandExecutor` pour les interactions :

- `/village <nom>` : Informations sur un village
- `/war <village>` : Déclaration de guerre
- `/market buy/sell <ressource> <quantité>` : Commerce
- `/build <type>` : Construction de bâtiments
- `/delegation <joueur>` : Envoi de délégations

### Threads (Simulation & IA)
Tâches schedulées pour la simulation du monde :

- `VillagerSpawnThread` : Génération de villageois (1 min)
- `VillagerEatThread` : Consommation de nourriture (5 min)
- `VillagerGoEatThread` : IA de recherche de nourriture (2 min)
- `DefenderThread` : IA de défense (5 sec)
- `TraderThread` : IA de commerce (1 min)
- `LocustThread` : Événements de sauterelles (1 sec)
- `DailyBuildingCostThread` : Coûts quotidiens (20 min)
- `WarThread` : Gestion des guerres (dynamique)

## 🎮 Événements Gérés

La classe principale `TestJava` implémente `Listener` et gère :

### Événements de Blocs
- `BlockPlaceEvent` : Villages, conquêtes, défenseurs, TNT
- `BlockBreakEvent` : Protection territoriale, destruction de centres
- `BlockGrowEvent` : Agriculture automatique

### Événements d'Entités
- `EntitySpawnEvent` : Contrôle des spawns (villageois, golems)
- `EntityDeathEvent` : Gestion des morts (villageois, ennemis)
- `EntityDamageEvent` : Protection et logique de combat
- `EntityPickupItemEvent` : IA de ramassage de nourriture

### Événements de Joueurs
- `PlayerJoinEvent` : Création d'empire automatique
- `PlayerRespawnEvent` : Téléportation au village

## 🔧 Configuration

### Config.java
```java
public class Config {
    public static final Integer VILLAGE_PROTECTION_RADIUS = 256;
    public static final Integer VILLAGE_CONSTRUCTION_RADIUS = 128;
    public static final Material VILLAGE_CENTER_TYPE = Material.BELL;
    public static final Material CONQUER_TYPE = Material.DIAMOND_BLOCK;
    public static final String BED_TYPE = "_BED";
    public static final String DEFAULT_VILLAGE_ID = "village";
}
```

### resources.json
Définit les ressources commerciales avec leur rareté et quantités.

## 🚀 Guide de Développement pour IA

### Règles de Développement

1. **TOUJOURS mettre à jour ce README** quand vous ajoutez des fonctionnalités
2. **Respecter l'architecture** : Model → Repository → Service → Command/Event
3. **Utiliser les services existants** avant de créer de nouveaux
4. **Gérer les erreurs** et valider les inputs utilisateur
5. **Documenter les nouvelles commandes** dans plugin.yml
6. **Tester les threads** et éviter les fuites mémoire

### Workflow Recommandé

1. **Analyser le besoin** : Quelle fonctionnalité implémenter ?
2. **Vérifier l'existant** : Services/repositories disponibles ?
3. **Créer/Modifier les modèles** si nécessaire (avec @Document)
4. **Étendre les repositories** pour les nouveaux accès données
5. **Implémenter la logique** dans les services
6. **Créer les commandes** ou gérer les événements
7. **Tester minutieusement** en jeu
8. **Mettre à jour la documentation**

### Patterns à Respecter

#### Création d'un Nouveau Modèle
```java
@Document(collection = "nouveau", schemaVersion = "1.0")
public class NouveauModel {
    @Id
    private String id;
    // Propriétés avec getters/setters
}
```

#### Repository Associé
```java
public class NouveauRepository {
    public static void update(NouveauModel model) {
        TestJava.database.upsert(model);
    }
    
    public static NouveauModel get(String id) {
        return TestJava.database.findById(id, NouveauModel.class);
    }
    
    // Autres méthodes selon les besoins
}
```

#### Service pour la Logique
```java
public class NouveauService {
    public void faireQuelqueChose(Player player) {
        // Validation
        VillageModel village = VillageRepository.getCurrentVillageConstructibleIfOwn(player);
        if (village == null) {
            player.sendMessage(ChatColor.RED + "Vous devez être dans votre village");
            return;
        }
        
        // Logique métier
        // ...
        
        // Sauvegarde
        NouveauRepository.update(nouveau);
    }
}
```

### Points d'Attention

- **Performance** : Les threads tournent en permanence, optimiser les requêtes
- **Concurrence** : Utiliser des collections thread-safe si nécessaire
- **Mémoire** : Nettoyer les HashMap et collections temporaires
- **Base de données** : JsonDB n'est pas relationnelle, dénormaliser si besoin
- **Événements** : Attention aux cascades d'événements infinies
- **Migration API** : Depuis Paper 1.21.8, les attributs `GENERIC_*` sont remplacés par leurs équivalents sans préfixe (ex: `GENERIC_MOVEMENT_SPEED` → `MOVEMENT_SPEED`)

### Tests & Debug

- Utiliser `Bukkit.getLogger().info()` pour débugger
- Tester en multijoueur pour valider la concurrence
- Vérifier les performances avec `/timings` de Paper
- Valider la persistence en redémarrant le serveur

## 📊 Métriques & Performance

- **Threads actifs** : ~8 threads permanents
- **Fréquence DB** : Accès fréquents, optimiser les requêtes
- **Événements** : ~15 handlers d'événements critiques
- **Commandes** : 8 commandes joueur principales

## 🔄 Migrations API

### Migration vers Paper 1.21.8 (Janvier 2025)

**Changements critiques appliqués :**

#### Attributs d'Entités ✅ CORRIGÉ
Les attributs `GENERIC_*` ont été renommés pour correspondre aux noms vanilla de Minecraft :

| Ancien (1.18.2) | Nouveau (1.21.8) |
|------------------|-------------------|
| `GENERIC_MOVEMENT_SPEED` | `MOVEMENT_SPEED` |
| `GENERIC_FOLLOW_RANGE` | `FOLLOW_RANGE` |
| `GENERIC_KNOCKBACK_RESISTANCE` | `KNOCKBACK_RESISTANCE` |
| `GENERIC_ATTACK_DAMAGE` | `ATTACK_DAMAGE` |
| `GENERIC_ATTACK_SPEED` | `ATTACK_SPEED` |

**Fichiers modifiés :**
- `EntityService.java` : Lignes 69-71, 97
- `DelegationCommand.java` : Lignes 58-59

#### Player.getDisplayName() ✅ CORRIGÉ
`Player.getDisplayName()` est déprécié, remplacé par `Player.getName()` :

**Changement :**
```java
// Ancien
player.getDisplayName()

// Nouveau
player.getName()
```

**Fichiers modifiés :**
- `BlockProtectionService.java`
- `PlayerService.java` 
- `VillageService.java`
- `EntityService.java`
- `WarBlockService.java`
- `VillageRepository.java`
- `DefenderThread.java`
- Toutes les commandes (`WarCommand`, `DelegationCommand`, `RenameCommand`)

#### APIs Dépréciées (Non-Critiques) ⚠️ DOCUMENTÉ
Ces APIs fonctionnent encore mais sont marquées comme dépréciées :

**ChatColor** → Adventure API (Component)
- Impacte : Tous les fichiers utilisant `ChatColor.RED`, etc.
- Migration future recommandée vers Adventure API

**broadcastMessage(String)** → Adventure API  
- Impacte : Messages serveur globaux
- Migration future vers Adventure API

**getCustomName()/setCustomName(String)** → Adventure API
- Impacte : Noms personnalisés d'entités
- Migration future vers Adventure API

**LivingEntity.setSwimming(boolean)**
- Déprécié dans EntityService.java ligne 207

#### Java Version
- **Ancien** : Java 17
- **Nouveau** : Java 21

#### API Paper
- **Ancienne** : Paper API 1.18.2-R0.1-SNAPSHOT
- **Nouvelle** : Paper API 1.21.8-R0.1-SNAPSHOT

#### Configuration Build ✅ AJOUTÉ
**Maven Shade Plugin** ajouté pour inclure les dépendances externes :

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.6.0</version>
    <!-- Configuration pour relocating JsonDB -->
</plugin>
```

**Changements de scope :**
- Paper API : `compile` → `provided` (fourni par le serveur)
- JsonDB : `compile` (inclus dans le JAR final)

**Problème résolu :** `NoClassDefFoundError: io/jsondb/JsonDBTemplate`

#### Gestion Robuste des Mondes ✅ AJOUTÉ
**Problème résolu :** `NullPointerException` quand le monde spécifique n'existe pas

```java
// Vérification et fallback pour le monde
if (TestJava.world == null) {
    getLogger().warning("Le monde '" + worldName + "' n'existe pas...");
    TestJava.world = Bukkit.getWorlds().get(0); // Monde principal
}
```

**Améliorations :**
- Fallback automatique vers le monde principal si `nouveaumonde2` n'existe pas
- Gestion sécurisée des entités avec vérifications null
- Logs informatifs pour diagnostiquer les problèmes de chargement
- Méthode utilitaire `tryLoadSpecificWorld()` pour rechargement dynamique

**Fichiers modifiés :**
- `TestJava.java` : Logique de fallback et gestion d'erreurs
- `CustomName.java` : Vérifications de sécurité pour éviter les NPE

#### Gestion Robuste des Commandes ✅ AJOUTÉ
**Problème résolu :** `ArrayIndexOutOfBoundsException` dans toutes les commandes

**Correction appliquée à toutes les commandes :**
```java
// Vérification systématique des arguments
if (args.length == 0) {
    sender.sendMessage(ChatColor.RED + "Usage: /command <arguments>");
    return true;
}
```

**Commandes corrigées :**
- `/village <villageName>` : Vérification argument obligatoire
- `/war <villageName>` : Vérification argument obligatoire  
- `/delegation <pseudo>` : Vérification argument obligatoire
- `/marketprice <buy|sell> <resource>` : Vérification 2+ arguments
- `/market <buy|sell> <resource> <quantity>` : Vérification 3+ arguments
- `/money <playerName>` : Gestion optionnelle/obligatoire
- `/rename <newName>` : Vérification argument obligatoire

**Comportement amélioré :**
- Messages d'aide automatiques au lieu de crashes
- Validation robuste avant traitement
- Expérience utilisateur plus fluide

#### Système de Suggestions de Ressources ✅ AJOUTÉ
**Problème résolu :** Difficulté à découvrir les noms exacts des ressources

**Nouvelle fonctionnalité intelligente :**
```java
// Recherche en 3 étapes dans ResourceHelper
1. Correspondance exacte (insensible à la casse)
2. Recherche avec contains()  
3. Suggestions basées sur la distance de Levenshtein
```

**Exemple d'amélioration :**
```
Avant:
/marketprice buy diamond
→ "Impossible de trouver la ressource 'diamond'"

Après:
/marketprice buy diamond
→ "Impossible de trouver la ressource 'diamond'"
→ "Ressources similaires : DIAMOND_BLOCK, REDSTONE_BLOCK, EMERALD_BLOCK"
→ "Ressources disponibles : COAL_BLOCK, COBBLESTONE, DIAMOND_BLOCK, ..."
```

**Fonctionnalités :**
- **Suggestions intelligentes** : Top 3 des ressources les plus proches
- **Liste complète** : Affichage de toutes les ressources disponibles
- **Recherche flexible** : Correspondance exacte, partielle, ou par similarité
- **Algorithme de Levenshtein** : Distance de chaînes pour classer les suggestions

**Commandes améliorées :**
- `/marketprice <buy|sell> <resource>` : Suggestions automatiques
- `/market <buy|sell> <resource> <quantity>` : Suggestions automatiques

**Fichiers ajoutés :**
- `ResourceHelper.java` : Classe utilitaire centralisée avec algorithme intelligent

**Impact utilisateur :**
- ✅ **Découverte facile** des ressources disponibles
- ✅ **Correction guidée** des erreurs de frappe
- ✅ **Expérience intuitive** pour les nouveaux joueurs

#### Sécurité & Affichage des Suggestions ✅ CORRIGÉ

**Problème de sécurité résolu :**
- **Ancien système dangereux** : Correspondances partielles exécutaient automatiquement les commandes
- **Nouveau système sécurisé** : Seules les correspondances **EXACTES** permettent l'exécution

**Exemple sécurisé :**
```java
// ✅ SÉCURISÉ : Seule correspondance exacte
/marketprice buy DIAMOND_BLOCK → EXECUTE (correspondance exacte)
/marketprice buy diamond_block → EXECUTE (insensible à la casse)

// ✅ SÉCURISÉ : Correspondance partielle = suggestions uniquement
/marketprice buy diamond → SUGGESTIONS uniquement (pas d'exécution)
/marketprice buy gold → SUGGESTIONS uniquement (pas d'exécution)
```

**Bug d'affichage corrigé :**
- **Problème** : `return false` dans les commandes déclenchait l'affichage automatique d'usage de Bukkit
- **Solution** : `return true` pour préserver nos messages de suggestions intelligentes
- **Résultat** : Les suggestions et la liste complète s'affichent correctement

**Fichiers corrigés :**
- `MarketPriceCommand.java` : Retour corrigé pour affichage correct
- `MarketCommand.java` : Retour corrigé pour affichage correct
- `ResourceHelper.java` : Logique de recherche strictement sécurisée

#### Initialisation Automatique des Ressources ✅ AJOUTÉ
**Problème critique résolu :** Les ressources n'étaient jamais chargées depuis `resources.json` vers la base de données JsonDB

**Symptôme identifié :**
```
[DEBUG] Nombre de ressources: 0
```

**Cause racine :**
- ✅ Fichier `resources.json` existait avec toutes les ressources
- ✅ Collection `ResourceModel` était créée dans JsonDB
- ❌ **Aucun code ne chargeait** les données depuis le fichier vers la base

**Solution implémentée :**
```java
// Nouveau service d'initialisation automatique
ResourceInitializationService.initializeResourcesIfEmpty();
```

**Fonctionnalités du service :**
- **Chargement automatique** : Lecture de `resources.json` au démarrage
- **Parsing intelligent** : Support du format JSON multi-lignes
- **Prévention de doublons** : Chargement seulement si la collection est vide
- **Logs détaillés** : Suivi du processus de chargement
- **Gestion d'erreurs** : Traitement robuste des erreurs de parsing

**Workflow du chargement :**
1. **Vérification** : Collection ResourceModel vide ?
2. **Lecture** : Fichier `resources.json` depuis les ressources
3. **Parsing** : Chaque ligne JSON → objet `ResourceModel`
4. **Sauvegarde** : Insertion dans la base de données JsonDB
5. **Confirmation** : Logs de succès

**Logs attendus au démarrage :**
```
[TestJava] Chargement des ressources depuis resources.json...
[TestJava] Ressource chargée: DIAMOND_BLOCK (quantité: 1)
[TestJava] Ressource chargée: EMERALD_BLOCK (quantité: 3)
...
[TestJava] ✅ 14 ressources chargées avec succès!
```

**Fichiers ajoutés :**
- `ResourceInitializationService.java` : Service d'initialisation complet

**Fichiers modifiés :**
- `TestJava.java` : Appel de l'initialisation au démarrage

**Impact :**
- ✅ **Commandes market fonctionnelles** dès le premier démarrage
- ✅ **Suggestions intelligentes** avec vraies ressources
- ✅ **Base de données cohérente** avec les ressources définies
- ✅ **Maintenance simplifiée** : modification de `resources.json` = auto-sync

#### Correction Affichage Commande Village ✅ CORRIGÉ
**Problème résolu :** Caractères carrés indésirables dans l'affichage de `/village`

**Cause :** Utilisation de `\r\n` (retour chariot + nouvelle ligne) au lieu de `\n` simple
- Minecraft n'affiche pas correctement les caractères `\r`
- Ces caractères apparaissaient comme des carrés dans l'interface

**Solution :**
```java
// Avant (affichait des carrés)
ChatColor.GOLD + village.getId() + "\r\n" +

// Après (affichage propre)
ChatColor.GOLD + village.getId() + "\n" +
```

**Fichier corrigé :**
- `VillageCommand.java` : Remplacement de tous les `\r\n` par `\n`

**Résultat :**
- ✅ **Affichage propre** de la commande `/village`
- ✅ **Plus de carrés indésirables** dans les statistiques
- ✅ **Interface utilisateur améliorée**

### Futures Migrations

Pour les prochaines mises à jour d'API :
1. **Toujours vérifier** les breaking changes dans la documentation Paper
2. **Utiliser** `mvn clean compile` pour identifier les erreurs
3. **Documenter** les changements dans cette section
4. **Tester** en jeu après correction

---

## ⚠️ Important pour les IA Développeuses

**CE README DOIT ÊTRE MAINTENU À JOUR À CHAQUE MODIFICATION DU CODE.**

Avant toute contribution :
1. Lisez entièrement ce README
2. Analysez l'architecture existante  
3. Suivez les patterns établis
4. Testez vos modifications
5. Mettez à jour cette documentation

Ce plugin est un écosystème complexe où chaque modification peut impacter d'autres systèmes. La cohérence architecturale est cruciale pour maintenir la stabilité et les performances.
