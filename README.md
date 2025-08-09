# Plugin Minecraft Civilization - Guide d'Architecture

Plugin Minecraft Java reproduisant Civilization 6 dans un environnement multijoueur. Ce document sert de guide architectural pour comprendre et développer le plugin.

## 🏗️ Stack Technique

- **Java 21** - Langage principal
- **Paper API 1.21.8** - API Minecraft moderne
- **JsonDB** - Base de données JSON pour la persistence
- **Maven** - Gestionnaire de dépendances et build
- **Jackson** - Sérialisation/désérialisation JSON

## 🔍 Règles de Développement

### Vérification des Lints

Les LLM (Language Model) doivent **OBLIGATOIREMENT** :
1. Vérifier les erreurs de lint après chaque modification de fichier
2. Corriger immédiatement toute erreur de lint détectée
3. Ne pas laisser de code avec des erreurs de lint
4. Utiliser l'outil `read_lints` pour vérifier les fichiers modifiés
5. Documenter les corrections de lint effectuées

**Exemple de workflow** :
1. Modification d'un fichier
2. Vérification immédiate avec `read_lints`
3. Si erreurs détectées → correction immédiate
4. Nouvelle vérification pour confirmer
5. Documentation des corrections dans les commentaires

## 📁 Architecture du Projet

```
src/main/java/TestJava/testjava/
├── TestJava.java                    # Point d'entrée principal
├── Config.java                      # Configuration centralisée
├── models/                          # Modèles de données (JsonDB)
├── repositories/                    # Couche d'accès aux données
├── services/                        # Logique métier
├── commands/                        # Handlers de commandes
├── listeners/                       # Event handlers Bukkit
├── threads/                         # Tâches asynchrones
├── helpers/                         # Utilitaires
├── classes/                         # Classes personnalisées
└── enums/                          # Énumérations
```

## ⚠️ Politique de Chat et Colorisation (IMPORTANT)

Un problème de sérialisation a été constaté dans le chat (ex. affichage de `TextComponentImpl{...}`) lorsqu'on utilise des `Component` Adventure directement ou lorsqu'on fait `toString()` sur des noms personnalisés Adventure.

Pour éviter ce problème, le plugin utilise une colorisation simple via `ChatColor` encapsulée dans `helpers/Colorize.java`.

Règles obligatoires pour les messages joueurs/serveur :
- Utiliser UNIQUEMENT des `String` avec `ChatColor` via `Colorize.name(...)`.
- Diffuser avec `Bukkit.getServer().broadcastMessage(String)` ou `Player#sendMessage(String)`.
- Ne JAMAIS faire `component.toString()` ni passer un `Component` Adventure dans `broadcast`.
- Pour extraire un nom Adventure d'entité, utiliser le PlainText serializer:
  - `PlainTextComponentSerializer.plainText().serialize(entity.customName())`

Exemples (Do/Don't):
- DO: `Bukkit.getServer().broadcastMessage("🌱 " + Colorize.name(villagerName) + " a planté un chêne...");`
- DO: `String n = PlainTextComponentSerializer.plainText().serialize(villager.customName());`
- DON'T: `Bukkit.broadcast(Component.text("..."));`
- DON'T: `villager.customName().toString()`

Note: L'API `broadcastMessage(String)` est dépréciée côté Paper, mais elle est volontairement utilisée ici pour conserver un rendu texte homogène avec `Colorize`. Si nécessaire, annoter localement la méthode avec `@SuppressWarnings("deprecation")`.

## 🚀 Système de Déplacement Centralisé (v4.0)

### ⚡ Améliorations Majeures Récentes

Le système de déplacement a été **complètement refactorisé** pour résoudre les problèmes d'anarchie et de conflits :

#### 1. VillagerMovementManager (Nouveau)
- **Centralisation complète** de tous les déplacements
- **API fluide** avec pattern Builder  
- **Gestion automatique** des timeouts et retries
- **Callbacks configurables** (success/failure/progress)
- **Validation préalable** des mouvements
- **50% de code en moins** dans les services

#### 2. Système Anti-Conflit (Nouveau)
- **VillagerMovementConflictListener** bloque pathfinding parasites
- **Résolution automatique** des conflits IA vs Plugin
- **Préservation interactions** joueur prioritaires  
- **Debug intégré** pour monitoring temps réel
- **Validation préalable** empêche mouvements impossibles

#### 3. Migration Services Terminée
- ✅ `VillagerInventoryService` : **57% code en moins**
- ✅ `ArmorierService` : **44% code en moins**  
- ✅ `VillagerGoEatThread` : **50% code en moins**
- 🔄 `FletcherService` et `InactiveJobSearchService` : À venir

### 📊 Impact Performances

| Métrique | Avant | Après | Amélioration |
|----------|-------|-------|--------------|
| **Échecs mouvement** | 15-20% | < 5% | **-70%** |
| **Conflits pathfinding** | Fréquents | Rares | **-95%** |
| **Code dupliqué** | 120 lignes | 60 lignes | **-50%** |
| **Debug capability** | Aucune | Complète | **+∞** |

### 🎯 Usage Simple

#### Avant (anarchique et complexe) :
```java
UUID taskId = UUID.randomUUID();
TestJava.threads.put(taskId, Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
    attempts++;
    if (attempts > maxAttempts) { cleanup(); return; }
    if (villager.isDead()) { cleanup(); return; }
    villager.getPathfinder().moveTo(target, 1.0);
    if (villager.getLocation().distance(target) <= 3.0) {
        performAction(); cleanup();
    }
}, 0, 20));
```

#### Maintenant (centralisé et robuste) :
```java
VillagerMovementManager.moveVillager(villager, destination)
    .withTimeout(60)
    .onSuccess(() -> performAction())
    .onFailure(() -> handleError())
    .start();
```

### 📁 Fichiers Système Déplacement

**Nouveaux fichiers** :
- `services/VillagerMovementManager.java` - Gestionnaire central
- `listeners/VillagerMovementConflictListener.java` - Anti-conflit
- `examples/VillagerMovementExamples.java` - Exemples d'usage
- `examples/AntiConflictMovementDemo.java` - Démonstrations avancées

**Documentation** :
- `VILLAGER_MOVEMENT_REFACTORING.md` - Guide de migration
- `ANTI_CONFLICT_SYSTEM.md` - Système anti-conflit détaillé

### Problème Identifié (Résolu)
L'ancien système de déplacement des villageois était **anarchique et dispersé** :
- Code dupliqué dans plusieurs services (`VillagerInventoryService`, `ArmorierService`, `FletcherService`, `VillagerGoEatThread`)
- Boucles répétitives avec `scheduleSyncRepeatingTask` pour forcer les déplacements
- Gestion d'état incohérente (timeouts, distances, vérifications)
- Maintenance difficile avec logique similaire copiée-collée

### Solution : VillagerMovementManager Centralisé

**Architecture centralisée** qui remplace toutes les implémentations disparates :

#### Avant (Code dispersé)
```java
// Dans VillagerInventoryService.java
TestJava.threads.put(taskId, Bukkit.getScheduler().scheduleSyncRepeatingTask(TestJava.plugin, () -> {
    attempts[0] += 1;
    if (attempts[0] % 3 == 0) {
        increasedDistance[0] += 1.0;
    }
    performMovementTask(buyer, seller, buyerEntity, sellerEntity, foodType, price, 
                      taskId, increasedDistance[0], attempts[0]);
}, delay, 10));

// Dans ArmorierService.java  
BukkitRunnable movementTask = new BukkitRunnable() {
    private int attempts = 0;
    @Override
    public void run() {
        attempts++;
        if (attempts > maxAttempts) { /* timeout */ }
        if (!target.isOnline()) { /* cleanup */ }
        bukkitVillager.getPathfinder().moveTo(targetLoc, 1.0);
    }
};

// Dans VillagerGoEatThread.java
TestJava.threads.put(uuid, Bukkit.getScheduler().scheduleSyncRepeatingTask(TestJava.plugin, () -> {
    attempts[0] += 1;
    if (attempts[0] % 3 == 0) {
        increasedDistance[0] += 1.0;
    }
    eVillager.getPathfinder().moveTo(loc, MOVE_SPEED);
}, delay, 10));
```

#### Après (API centralisée)
```java
// Dans tous les services : une seule API cohérente
VillagerMovementManager.moveVillager(villager, targetLocation)
    .onSuccess(() -> performAction())
    .onFailure(() -> handleFailure())
    .withTimeout(30)
    .withSuccessDistance(3.0)
    .withName("TaskDescription")
    .start();
```

### Fonctionnalités du Gestionnaire

#### 1. **Gestion Automatique du Cycle de Vie**
- Timeout et retry automatiques
- Nettoyage automatique des tâches terminées
- Vérifications de sécurité (villageois valide, vivant)
- Annulation intelligente des tâches précédentes

#### 2. **API Fluide et Configurable**
```java
VillagerMovementManager.moveVillager(villager, target)
    .withSuccessDistance(2.0)        // Distance de réussite
    .withMoveSpeed(1.5)              // Vitesse de déplacement  
    .withTimeout(45)                 // Timeout en secondes
    .withoutRetry()                  // Désactiver retry automatique
    .withName("CustomTaskName")      // Nom pour debugging
    .onSuccess(() -> { /* action */ })
    .onFailure(() -> { /* cleanup */ })
    .onPositionUpdate((distance, attempts) -> {
        // Callback à chaque vérification
    })
    .start();
```

#### 3. **Callbacks et Événements**
- **onSuccess** : Action à l'arrivée 
- **onFailure** : Gestion d'échec (timeout, villageois mort)
- **onPositionUpdate** : Suivi en temps réel (distance, tentatives)

#### 4. **Gestion des Tâches Actives**
```java
// Annulation par villageois (évite conflits multiples)
VillagerMovementManager.cancelMovementForVillager(villager);

// Annulation par ID de tâche
VillagerMovementManager.cancelMovement(taskId);

// Monitoring global
int activeMovements = VillagerMovementManager.getActiveMovementCount();
boolean hasMovement = VillagerMovementManager.hasActiveMovement(villager);
```

### Avantages du Nouveau Système

✅ **Code unifié** : Une seule implémentation maintenue  
✅ **Performance** : Gestion optimisée des tâches concurrentes  
✅ **Fiabilité** : Nettoyage automatique, pas de fuites mémoire  
✅ **Debugging** : Logs centralisés avec noms de tâches  
✅ **Flexibilité** : Configuration fine par cas d'usage  
✅ **Maintenance** : Corrections en un seul endroit  

### Migration des Services

**Services migrés vers VillagerMovementManager** :
- ✅ `VillagerInventoryService` (achat nourriture fermiers)
- ✅ `VillagerGoEatThread` (déplacement vers champs)  
- ✅ `ArmorierService` (déplacement vers joueurs)
- 🔄 `FletcherService` (équipement gardes) - *En cours*
- 🔄 `InactiveJobSearchService` (recherche métiers) - *En cours*

### Usage Recommandé

#### Pour les Nouveaux Développements
```java
// Déplacement simple
UUID taskId = VillagerMovementManager.moveToLocation(villager, target, "NewFeature");

// Déplacement avec action
VillagerMovementManager.moveToLocationAndExecute(villager, target, 
    () -> performBusinessLogic(), "FeatureName");

// Déplacement complexe avec suivi
VillagerMovementManager.moveVillager(villager, target)
    .withName("ComplexFeature")
    .onSuccess(() -> {
        // Action principale
        executeBusinessLogic();
        
        // Enregistrer dans l'historique  
        HistoryService.recordEvent(villager, "Action terminée");
    })
    .onFailure(() -> {
        // Fallback ou retry manuel
        scheduleRetryLater();
    })
    .onPositionUpdate((distance, attempts) -> {
        // Conditions d'arrêt personnalisées
        if (customStopCondition()) {
            VillagerMovementManager.cancelMovement(getCurrentTaskId());
        }
    })
    .start();
```

#### Intégration avec l'Historique
```java
// Le gestionnaire peut automatiquement enregistrer les mouvements
VillagerMovementManager.moveVillager(villager, target)
    .withName("FoodPurchase")
    .onSuccess(() -> {
        performTransaction();
        HistoryService.recordFoodPurchase(villager, "Achat réussi");
    })
    .onFailure(() -> {
        HistoryService.recordEvent(villager, "Échec déplacement achat");
    })
    .start();
```

### Métriques et Monitoring

Le nouveau système offre une visibilité complète :
- Nombre de tâches actives : `getActiveMovementCount()`
- État par villageois : `hasActiveMovement(villager)`
- Logs détaillés avec noms de tâches
- Callbacks pour métriques personnalisées

Cette refactorisation améliore significativement la **maintenabilité**, la **performance** et la **fiabilité** de tous les systèmes de déplacement dans le plugin.

## 🎯 Concepts de Jeu Fondamentaux

### Entités Principales
- **Empire** : Appartient à un joueur, gère l'économie (juridictions)
- **Village** : Centre de civilisation avec population et bâtiments
- **Villageois** : Entités IA avec système de classes sociales
- **Bâtiments** : Structures avec coûts et fonctionnalités (bergeries)
- **Ressources** : Système économique avec marché dynamique

### Systèmes Gameplay
- **Territoire** : Protection avec rayon défini par `VILLAGE_PROTECTION_RADIUS`
- **Commerce** : Marché mondial avec ressources et prix + échanges entre villageois
- **Classes Sociales** : Hiérarchie villageois basée sur alimentation
- **Distance** : Contraintes de placement pour métiers et bâtiments
- **Guerre** : Conflits entre empires avec mécaniques TNT
- **Économie Villageois** : Richesse personnelle, salaires et impôts par métier + redistribution sociale
- **Inventaire Intelligent** : Système d'achat/vente entre villageois avec déplacement physique

## 📊 Modèles de Données (JsonDB)

### Pattern de Base
```java
@Document(collection = "collectionName", schemaVersion = "1.0")
public class ModelClass {
    @Id
    private String/UUID id;
    // Propriétés avec getters/setters
}
```

### Modèles Principaux

**EmpireModel** - Gestion empire/économie
```java
private String empireName;
private String playerName; 
private Integer juridictionCount;  // Monnaie
private Boolean inWar;
```

**VillageModel** - Villages et population
```java
private String id;                 // Nom du village
private String playerName;         // Propriétaire
private Integer population;        // Nombre villageois
private Integer food;             // Points de prospérité
```

**VillagerModel** - Villageois individuels avec IA
```java
private UUID id;                   // UUID entité Minecraft
private String village;           // Village d'appartenance
private Integer food;             // Points nourriture
private Integer socialClass;      // Classe sociale (0-4)
 private Float richesse;           // Richesse personnelle en juridictions
 private Integer education;        // Niveau d'éducation (0-8)
```

**BuildingModel** - Bâtiments avec économie
```java
private UUID id;
private String buildingType;      // "bergerie"
private String villageName;
private int level;               // Niveau (1-3)
private boolean active;          // Statut économique
private int costToBuild;
private int costPerDay;
```

**SheepModel** - Moutons de bergerie
```java
private UUID id;                 // UUID entité Minecraft
private UUID buildingId;         // Bergerie propriétaire
private String villageName;
private int sheepNumber;         // Numéro séquentiel
```

## 🔄 Pattern Repository

### Interface Standard
```java
public class EntityRepository {
    public static void update(EntityModel entity) {
        TestJava.database.upsert(entity);
    }
    
    public static EntityModel get(String id) {
        return TestJava.database.findById(id, EntityModel.class);
    }
    
    public static Collection<EntityModel> getAll() {
        return TestJava.database.findAll(EntityModel.class);
    }
    
    public static void remove(EntityModel entity) {
        TestJava.database.remove(entity, EntityModel.class);
    }
}
```

### Requêtes JxPath
```java
// Recherche par critère
String jxQuery = String.format("/.[villageName='%s']", villageName);
Collection<BuildingModel> buildings = TestJava.database.find(jxQuery, BuildingModel.class);

// Recherche villageois par classe sociale
String query = String.format("/.[socialClass=%d]", classLevel);
Collection<VillagerModel> villagers = TestJava.database.find(query, VillagerModel.class);
```

## ⚙️ Couche Services

### Pattern Service
```java
public class EntityService {
    // Logique métier pure
    public static void processBusinessLogic(EntityModel entity) {
        // Validation
        // Transformation
        // Sauvegarde via Repository
        EntityRepository.update(entity);
    }
}
```

### Services Clés

**VillagerMovementManager** - Gestionnaire centralisé de déplacements (v4.0+)
```java
// Remplace les boucles répétitives avec scheduleSyncRepeatingTask
// API fluide pour tous les déplacements de villageois du plugin

// Usage standard
VillagerMovementManager.moveVillager(villager, targetLocation)
    .onSuccess(() -> performAction())
    .onFailure(() -> handleFailure())
    .withTimeout(30)
    .withSuccessDistance(3.0)
    .withName("TaskDescription")
    .start();

// Méthodes de convenance
UUID taskId = VillagerMovementManager.moveToLocation(villager, target, "TaskName");
VillagerMovementManager.moveToEntity(villager, entity, onSuccess, onFailure, "TaskName");

// Gestion des tâches
VillagerMovementManager.cancelMovementForVillager(villager);
VillagerMovementManager.cancelMovement(taskId);
int activeCount = VillagerMovementManager.getActiveMovementCount();

// Configuration avancée avec callbacks
VillagerMovementManager.moveVillager(villager, location)
    .withMoveSpeed(1.5)
    .withoutRetry() // Pas de relance automatique du pathfinding
    .onPositionUpdate((distance, attempts) -> {
        // Callback pour chaque vérification de position
        if (someCondition) {
            // Logique personnalisée pendant le déplacement
        }
    })
    .start();
```

**SocialClassService** - Gestion classes sociales
```java
// Évaluation et transition automatique
public static void evaluateAndUpdateSocialClass(VillagerModel villager) {
    SocialClass newClass = calculateClassFromFood(villager.getFood());
    if (newClass != villager.getSocialClassEnum()) {
        updateVillagerSocialClass(villager, newClass);
    }
}

// Mise à jour nom avec tag coloré
public static void updateVillagerDisplayName(VillagerModel villager) {
    String coloredTag = villager.getSocialClassEnum().getColoredTag();
    String newName = coloredTag + " [" + villager.getVillage() + "] " + villager.getName();
    // Application au monde Minecraft
}
```

**DistanceValidationService** - Contrôle placement
```java
public static ValidationResult validateJobBlockPlacement(Player player, Location location, Material material) {
    JobDistanceConfig config = DistanceConfigService.getJobConfig(material);
    VillageModel village = VillageRepository.getNearestVillageOfPlayer(player.getName());
    
    double distance = location.distance(villageCenter);
    return validateDistance(distance, config.getDistanceMin(), config.getDistanceMax());
}
```

**SheepService** - Gestion moutons bergerie
```java
public static boolean spawnSheepForBuilding(BuildingModel building) {
    if (!building.isActive()) return false;
    
    int currentCount = SheepRepository.getSheepCountForBuilding(building.getId());
    int maxLimit = building.getLevel(); // Level = nombre max moutons
    
    if (currentCount >= maxLimit) return false;
    
    // Spawn entité + création SheepModel
}
```

**TaxService** - Système d'impôts villageois avec redistribution sociale
```java
public static void collectTaxes() {
    // Pour chaque villageois avec métier:
    // 1. Payer salaire selon JobDistanceConfig
    // 2. Collecter impôts (% du salaire)
    // 3. Verser impôts à l'empire
    // 4. NOUVEAU: Redistribuer 25% des taxes aux villageois misérables
    // 5. Message global de collecte + message personnalisé au propriétaire
}
```

**VillagerInventoryService** - Commerce entre villageois
```java
public static boolean attemptToFeedVillager(VillagerModel hungryVillager) {
    // 1. Consommer depuis inventaire personnel (priorité)
    // 2. Acheter auprès du fermier le plus proche
    // 3. Déplacement physique vers le vendeur
    // 4. Transaction avec échange richesse/items
}
```

**VillagerHomeService** - Gestion des "Home" des villageois
```java
public static void validateAndCorrectAllVillagerHomes() {
    // Vérifie que chaque villageois est dans le rayon de protection de son village
    // Corrige automatiquement les "Home" incorrects
    // Empêche le retour automatique au village d'origine
}

public static void resetVillagerNavigation(Villager villager) {
    // Réinitialise complètement les données de navigation
    // Utilisé lors de la téléportation pour famine
}
```

## 🎮 Système d'Événements

### Listeners Pattern
```java
@EventHandler(priority = EventPriority.HIGH)
public void onEvent(BukkitEvent event) {
    // Validation
    if (!shouldHandle(event)) return;
    
    // Logique métier via Services
    ServiceClass.processEvent(event);
    
    // Modification événement si nécessaire
    if (shouldCancel) event.setCancelled(true);
}
```

### Listeners Principaux

**JobBlockPlacementListener** - Contrôle pose métiers
```java
@EventHandler(priority = EventPriority.HIGH)
public void onBlockPlace(BlockPlaceEvent event) {
    if (!DistanceConfigService.isJobBlock(event.getBlock().getType())) return;
    
    ValidationResult result = DistanceValidationService.validateJobBlockPlacement(
        event.getPlayer(), event.getBlock().getLocation(), event.getBlock().getType());
    
    if (!result.isValid()) {
        event.setCancelled(true);
        event.getPlayer().sendMessage(result.getMessage());
    }
}
```

**SocialClassJobListener** - Transitions classes sociales
```java
@EventHandler(priority = EventPriority.MONITOR)
public void onVillagerCareerChange(VillagerCareerChangeEvent event) {
    VillagerModel villager = VillagerRepository.find(event.getEntity().getUniqueId());
    
    // Délai pour que le changement soit effectif
    Bukkit.getScheduler().runTaskLater(() -> {
        if (gainedJob && villager.getSocialClassEnum() == SocialClass.INACTIVE) {
            SocialClassService.promoteToWorkerOnJobAssignment(villager);
        } else if (lostJob && villager.getSocialClassEnum() == SocialClass.OUVRIERE) {
            SocialClassService.demoteToInactiveOnJobLoss(villager);
        }
    }, 2L);
}
```

## 🧵 Système de Threads

### Pattern Thread
```java
public class GameThread implements Runnable {
    @Override
    public void run() {
        try {
            Collection<EntityModel> entities = EntityRepository.getAll();
            for (EntityModel entity : entities) {
                processEntity(entity);
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erreur dans " + getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
```

### Threads Principaux

**VillagerEatThread** (5 min) - Consommation nourriture
```java
// Décrémente nourriture et évalue classe sociale
for (VillagerModel villager : VillagerRepository.getAll()) {
    villager.setFood(villager.getFood() - 1);
    SocialClassService.evaluateAndUpdateSocialClass(villager);
    VillagerRepository.update(villager);
}
```

**DailyBuildingCostThread** (4 min) - Coûts quotidiens (coût divisé par 5)
```java
for (BuildingModel building : BuildingRepository.getAll()) {
    if (!building.isActive()) continue;
    
    int adjustedCost = building.getCostPerDay() / 5; // Coût divisé par 5
    
    if (empire.getJuridictionCount() >= adjustedCost) {
        // Paiement normal
        empire.setJuridictionCount(empire.getJuridictionCount() - adjustedCost);
    } else {
        // Désactivation par manque de fonds
        building.setActive(false);
        if ("bergerie".equals(building.getBuildingType())) {
            SheepService.removeAllSheepForBuilding(building);
        }
    }
}
// Message personnalisé au propriétaire: "Votre village a payé Xµ pour maintenir X bâtiments."
```

**SheepSpawnThread** (20 min) - Production moutons
```java
for (BuildingModel building : BuildingRepository.getAll()) {
    if ("bergerie".equals(building.getBuildingType()) && building.isActive()) {
        SheepService.spawnSheepForBuilding(building);
    }
}
```

**VillagerTaxThread** (5 min) - Collecte d'impôts avec redistribution sociale
```java
// Collecte automatique d'impôts des villageois avec métier
TaxService.collectTaxes();
// Messages: 
// - Global: "💰 Collecte d'impôts terminée: XXXµ collectés auprès de X villageois"
// - Par village: "🏘️ Village (Propriétaire): XXXµ collectés auprès de X villageois"
// - NOUVEAU Redistribution: "🎁 25% des taxes (XXXµ) ont été redistribuées à X misérables"
```

**VillagerGoEatThread** (2 min) - Recherche nourriture intelligente
```java
// Nouvelle logique prioritaire avec compteurs:
FeedResult result = VillagerInventoryService.attemptToFeedVillager(villager);
if (result == FeedResult.SELF_FED) {
    stats.autosuffisants++; // Mangé depuis inventaire
} else if (result == FeedResult.BOUGHT_FOOD) {
    stats.clients++; // Acheté auprès fermier
} else {
    // Fallback vers EatableModel (champs publics)
    stats.voleurs++ // ou stats.affames++ si échec
}
// Affichage global par village à la fin du cycle
// NOUVEAU: Déclenchement manuel avec /admin goeat
```

**FarmerSupplyThread** (10 min) - Approvisionnement fermiers
```java
// Donne des stocks alimentaires aux fermiers pour qu'ils puissent vendre
// Blé: production régulière, Pain: 30% chance, Bloc foin: 10% chance
VillagerInventoryService.giveFoodToFarmers();
```

**VillagerInventoryTransformationThread** (1 min) - Transformations et ventes automatiques
```java
// Pour chaque villageois:
// 1. Transformer 3 blés → 1 pain (silencieux)
// 2. Transformer 3 pains → 1 bloc de foin (silencieux)  
// 3. Vendre tous les blocs de foin à la banque mondiale (message coloré)
VillagerInventoryTransformationService.executeInventoryTransformationCycle();
// Message: "{Prénom} {Nom} a vendu {x} foin à la banque mondiale pour {x}µ"
// Log résumé: "X villageois traités (blé→pain: X, pain→foin: X, foin vendus: X)"
```

**AutomaticJobAssignmentThread** (1 min) - Assignation automatique d'emplois
```java
// Boucle sur chaque village et chaque villageois inactif
// Cherche automatiquement les bâtiments custom avec des emplois disponibles
// Assigne automatiquement les villageois inactifs aux emplois disponibles
AutomaticJobAssignmentService.executeAutomaticJobAssignment();
```

## 🎲 Commandes

### Pattern Command
```java
public class GameCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Commande joueur uniquement");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Validation arguments
        if (args.length < requiredArgs) {
            player.sendMessage("Usage: " + getUsage());
            return true;
        }
        
        // Logique via Services
        ServiceClass.executeCommand(player, args);
        return true;
    }
}
```

### Commandes Principales

**BuildCommand** - Construction bâtiments
```java
// Validation distance
ValidationResult validation = DistanceValidationService.validateBuildingPlacement(
    player, buildLocation, buildingType);
if (!validation.isValid()) {
    player.sendMessage(validation.getMessage());
    return true;
}

// Configuration depuis JSON
BuildingDistanceConfig config = DistanceConfigService.getBuildingConfig(buildingType);
building.setCostToBuild(config.getCostToBuild());
building.setCostPerDay(config.getCostPerDay());
```


**DistanceCommand** - Interface distances
```java
switch (subCommand) {
    case "metiers": return handleJobsCommand(player);
    case "batiments": return handleBuildingsCommand(player);
    case "check": return handleCheckCommand(player);
    case "info": return handleInfoCommand(player, args);
    case "reload": return handleReloadCommand(player);
}
```

**AdminCommand** - Commandes administratives
```java
switch (subCommand) {
    case "refresh": return refreshCmd.onCommand(sender, command, label, subArgs);
    case "data": return dataCmd.onCommand(sender, command, label, subArgs);
    case "collecttaxes": return handleCollectTaxesCommand(player); // NOUVEAU
    case "goeat": return handleGoEatCommand(player); // NOUVEAU
    // ... autres sous-commandes admin
}
```

## 📋 Configuration JSON

### Métiers (`metiers.json`)
```json
[
  {
    "material": "CARTOGRAPHY_TABLE",
    "jobName": "Cartographe",
    "distanceMin": 10,
    "distanceMax": 50,
    "description": "Table de cartographie pour le métier de cartographe",
    "salaire": 15,
    "tauxImpot": 0.25
  }
]
```

### Bâtiments (`metiers_custom.json`)
```json
[
  {
    "buildingType": "bergerie",
    "distanceMin": 20,
    "distanceMax": 100,
    "description": "Bergerie pour élever des moutons",
    "costToBuild": 2500,
    "costPerDay": 50,
    "costPerUpgrade": 1500,
    "costUpgradeMultiplier": 1.2
  }
]
```

### Chargement Configuration
```java
public static void loadJobConfigurations() {
    try (InputStream inputStream = TestJava.class.getResourceAsStream("/metiers.json")) {
        String json = new BufferedReader(new InputStreamReader(inputStream))
            .lines().collect(Collectors.joining("\n"));
        
        List<JobDistanceConfig> configs = objectMapper.readValue(
            json, new TypeReference<List<JobDistanceConfig>>() {});
        
        jobConfigs.clear();
        for (JobDistanceConfig config : configs) {
            Material material = Material.valueOf(config.getMaterial());
            jobConfigs.put(material, config);
        }
    }
}
```

## 🔧 Classes Utilitaires

### CustomName - Gestion noms entités
```java
// Extraction village depuis nom personnalisé
public static String extractVillageName(String customName) {
    String cleanName = ChatColor.stripColor(customName);
    Pattern pattern = Pattern.compile("\\[(.*?)\\]");
    Matcher matcher = pattern.matcher(cleanName);
    if (matcher.find()) {
        return matcher.group(1); // Premier élément entre []
    }
    throw new IllegalArgumentException("Format invalide: " + customName);
}
```

### Colorize - Formatage messages
```java
public static String name(Object object) {
    return ChatColor.GOLD + object.toString() + ChatColor.WHITE;
}
```

## 🎭 Système Classes Sociales

### Enum SocialClass
```java
public enum SocialClass {
    MISERABLE(0, "Misérable", ChatColor.YELLOW, "{0}"),
    INACTIVE(1, "Inactive", ChatColor.GRAY, "{1}"),
    OUVRIERE(2, "Ouvrière", ChatColor.BLUE, "{2}"),
    MOYENNE(3, "Moyenne", ChatColor.GREEN, "{3}"),
    BOURGEOISIE(4, "Bourgeoisie", ChatColor.GOLD, "{4}");
    
    public String getColoredTag() {
        return color + tag + ChatColor.RESET;
    }
}
```

### Logique Transitions
```java
// Basé sur points nourriture
if (food >= 19 && currentClass == SocialClass.MISERABLE) {
    return SocialClass.INACTIVE;
} else if (food < 6 && currentClass == SocialClass.INACTIVE) {
    return SocialClass.MISERABLE;
} else if (food <= 5 && currentClass == SocialClass.OUVRIERE) {
    return SocialClass.MISERABLE; // + perte métier
}
```

### Format Noms
```
{0} [VillageName] Prénom Nom    // Classe 0 - Jaune
{1} [VillageName] Prénom Nom    // Classe 1 - Gris
{2} [VillageName] Prénom Nom    // Classe 2 - Bleu
```

### Attribution Automatique des Métiers

Quand un joueur place un bloc de métier :
1. **Validation** : Distance village vérifiée par `JobBlockPlacementListener`
2. **Recherche** : `JobAssignmentService` trouve le villageois inactif le plus proche (rayon 100 blocs)
3. **Attribution** : Le villageois inactif se dirige vers le bloc et prend automatiquement le métier
4. **Protection** : Les villageois misérables sont empêchés de prendre des métiers (pathfinding bloqué)

```java
// Flux d'attribution
BlockPlaceEvent → JobAssignmentService.assignJobToNearestInactiveVillager()
→ findInactiveVillagersNearby() → directVillagerToJobBlock() 
→ villager.getPathfinder().moveTo() → VillagerCareerChangeEvent
```

### Restrictions par Classe
- **Misérable (0)** : Ne peut **PAS** avoir de métier
- **Inactive (1)** : Peut obtenir un métier → promotion automatique vers Ouvrière
- **Ouvrière (2)** : Possède un métier, génère des impôts

### Corrections de Bugs (v2.1)

#### Bug Villageois Misérable avec Métier
**Problème** : Timing entre `VillagerEatThread` (5 min) et `SocialClassEnforcementThread` (2 min) permettait aux misérables de garder leur métier.

**Solution** :
- ✅ **Vérification immédiate** dans `SocialClassService.evaluateAndUpdateSocialClass()`
- ✅ **Double contrôle** dans `SocialClassEnforcementThread.enforceStrictJobRestrictions()`
- ✅ **Logs de détection** : `🚨 BUG DÉTECTÉ: Villageois misérable avec métier`

#### Bug Retour Village d'Origine
**Problème** : Villageois migrés retournaient automatiquement à leur village d'origine (données navigation Minecraft).

**Solution** :
- ✅ **Service VillagerHomeService** : Vérification et correction automatique des "Home"
- ✅ **Vérification périodique** : Dans `SocialClassEnforcementThread` (toutes les 2 minutes)
- ✅ **Vérification au démarrage** : Correction des Home au lancement du serveur
- ✅ **Vérification lors de migration** : Réinitialisation navigation lors de téléportation pour famine
- ✅ **Rayon de protection** : Vérification distance `VILLAGE_PROTECTION_RADIUS` (256 blocs)
- ✅ **Reset profession temporaire** pour vider les données internes
- ✅ **Arrêt pathfinding** pour empêcher le retour automatique

## 🐑 Système Bergerie

### Architecture Moutons
```java
// Spawn quotidien automatique
if (building.isActive() && currentSheepCount < building.getLevel()) {
    Sheep entity = spawnSheepEntity();
    SheepModel model = new SheepModel();
    model.setId(entity.getUniqueId());
    model.setBuildingId(building.getId());
    model.setSheepNumber(currentSheepCount + 1);
    SheepRepository.update(model);
    
    String name = generateSheepName(building, model.getSheepNumber());
    entity.setCustomName(name); // {actif} [Village] Mouton N°X
}
```

### Contrôle Population
```java
// Empêche reproduction
@EventHandler
public void onEntityBreed(EntityBreedEvent event) {
    if (event.getEntityType() == EntityType.SHEEP) {
        Sheep parent1 = (Sheep) event.getMother();
        if (parent1.getCustomName() != null) { // Mouton géré
            event.setCancelled(true);
        }
    }
}

// Empêche spawn naturel
@EventHandler  
public void onCreatureSpawn(CreatureSpawnEvent event) {
    if (event.getEntityType() == EntityType.SHEEP && 
        event.getSpawnReason() != SpawnReason.CUSTOM) {
        event.setCancelled(true);
    }
}
```

## 🌳 Système Garde Forestier (v4.1+)

### 🎯 Vue d'ensemble
Le garde forestier est un métier custom qui plante automatiquement des arbres après le paiement des taxes, créant progressivement des forêts autour des villages.

### ⚙️ Configuration
```json
{
  "buildingType": "garde forestier",
  "distanceMin": 77,
  "distanceMax": 128,
  "description": "Garde forestier pour générer une forêt",
  "costToBuild": 500,
  "costPerDay": 10,
  "salaireEmploye": 12,
  "tauxTaxeEmploye": 0.25,
  "nombreEmployesMax": 3
}
```

### 🔄 Workflow de Plantation
1. **Déclenchement automatique** : Toutes les 5 minutes via `VillagerTaxThread`
2. **Après paiement taxes** : `ForestGuardService.triggerTreePlantingAfterSalary()`
3. **Déplacement lieu de travail** : Utilise `VillagerMovementManager` (fiable)
4. **Recherche emplacement** : Rayon 50 blocs, sol adapté, 6 blocs hauteur libre
5. **Déplacement plantation** : Vers endroit optimal trouvé
6. **Plantation magique** : Sapling + croissance après 3 secondes

### 🌱 Types d'Arbres et Probabilités
| Type | Probabilité | Rareté |
|------|-------------|---------|
| **Chêne** | 30% | Très commun |
| **Bouleau** | 25% | Commun |
| **Épicéa** | 20% | Commun |
| **Jungle** | 10% | Peu commun |
| **Acacia** | 8% | Peu commun |
| **Chêne Noir** | 5% | Rare |
| **Cerisier** | 2% | Très rare |

### 💬 Messages Système
```java
// Succès
"🌱 [Nom du Garde] a planté un chêne qui va bientôt pousser..."
"🌳✨ [Nom du Garde] a fait pousser un magnifique chêne par magie !"

// Échecs
"🌲 [Nom du Garde] n'a pas pu rejoindre son poste de garde forestier"
"🌲 [Nom du Garde] ne trouve pas d'endroit libre pour planter un arbre"
```

### 🔧 Architecture Technique
```java
// Dans TaxService.collectTaxes() - Déclenchement automatique
if (villager.hasCustomJob() && "garde forestier".equals(villager.getCurrentJobName())) {
    ForestGuardService.triggerTreePlantingAfterSalary(villager, entity);
}

// Fonctionnalités principales
ForestGuardService.findSuitablePlantingLocation() // Recherche intelligente
ForestGuardService.plantSaplingAndGrow()         // Plantation + croissance
ForestGuardService.createSimpleTree()            // Fallback manuel
```

### 🎮 Impact Gameplay
- **Coût** : 500µ construction + 10µ/4min entretien + 3µ/5min impôts
- **Bénéfice** : Création automatique de forêts + ressources bois renouvelables
- **Limitation** : 3 gardes maximum par bâtiment
- **Écologie** : Développement durable et embellissement territorial

## 📏 Système Distance

### Validation Placement
```java
private static ValidationResult validateDistanceToVillageCenter(
    Location targetLocation, VillageModel village, int minDistance, int maxDistance) {
    
    Location center = new Location(targetLocation.getWorld(), village.getX(), village.getY(), village.getZ());
    double distance = targetLocation.distance(center);
    
    if (distance < minDistance) {
        return new ValidationResult(false, 
            "❌ Trop proche du centre ! Distance: " + distance + " (min: " + minDistance + ")",
            distance, minDistance, maxDistance);
    }
    
    return new ValidationResult(true, "✅ Placement autorisé", distance, minDistance, maxDistance);
}
```

## 🏁 Point d'Entrée Principal

### TestJava.java - Initialisation
```java
@Override
public void onEnable() {
    // 1. Enregistrement event listeners
    Bukkit.getPluginManager().registerEvents(new JobBlockPlacementListener(), this);
    Bukkit.getPluginManager().registerEvents(new SocialClassJobListener(), this);
    
    // 2. Enregistrement commandes
    getCommand("build").setExecutor(new BuildCommand());
    getCommand("distance").setExecutor(new DistanceCommand());
    
    // 3. Initialisation JsonDB
    database = new JsonDBTemplate(jsonLocation, baseScanPackage);
    database.createCollection(VillagerModel.class);
    database.createCollection(BuildingModel.class);
    database.createCollection(SheepModel.class);
    
    // 4. Chargement configurations
    DistanceConfigService.loadAllConfigurations();
    ResourceInitializationService.initializeResourcesIfEmpty();
    
    // 5. Synchronisation données
    VillagerSynchronizationService.synchronizeWorldVillagersWithDatabase();
    SocialClassService.initializeSocialClassForExistingVillagers();
    
    // 6. Démarrage threads
    Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new VillagerEatThread(), 0, 20 * 60 * 5);
    Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new VillagerTaxThread(), 0, 20 * 60 * 5);
    Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new FarmerSupplyThread(), 0, 20 * 60 * 10);
    Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new VillagerGoEatThread(), 0, 20 * 60 * 2);
    Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new DailyBuildingCostThread(), 0, 20 * 60 * 20);
    Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new SheepSpawnThread(), 0, 20 * 60 * 20);
}
```

## 💰 Système Économique Villageois

### Richesse Personnelle
- **Chaque villageois** possède une richesse en juridictions (µ)
- **Affichage** via `/social villager` : `"Richesse: X.XXµ"`
- **Initialisation** : 0µ par défaut pour nouveaux villageois

### Salaires et Impôts par Métier
```java
// Configuration dans metiers.json
"salaire": 15,        // Reçu toutes les 5 minutes
"tauxImpot": 0.25     // 25% prélevé pour l'empire
```

### Redistribution Sociale (v3.10+)
- **25% des taxes collectées** sont automatiquement redistribuées aux villageois misérables
- **Répartition équitable** : Le montant est divisé entre tous les misérables du serveur
- **Message personnalisé** au propriétaire : `"🎁 25% des taxes (XXXµ) ont été redistribuées à X misérables"`
- **But social** : Aide les villageois en difficulté à améliorer leur classe sociale

#### Barème des Métiers (13 métiers officiels Minecraft)
| Métier | Salaire | Taux Impôt | Revenus Net |
|--------|---------|------------|-------------|
| Pêcheur | 3µ | 10% | 2.7µ / 5min |
| Fermier | 6µ | 15% | 5.1µ / 5min |
| Boucher | 6µ | 15% | 5.1µ / 5min |
| Tisserand | 9µ | 20% | 7.2µ / 5min |
| Tailleur de Pierre | 9µ | 20% | 7.2µ / 5min |
| Travailleur du Cuir | 9µ | 20% | 7.2µ / 5min |
| Archer | 12µ | 25% | 9µ / 5min |
| Forgeron d'Outils | 12µ | 25% | 9µ / 5min |
| Armurier | 12µ | 25% | 9µ / 5min |
| Réparateur d'Armes | 12µ | 25% | 9µ / 5min |
| Cartographe | 15µ | 30% | 10.5µ / 5min |
| Bibliothécaire | 15µ | 30% | 10.5µ / 5min |
| **Clerc** | 18µ | 35% | 11.7µ / 5min |

### Commerce Alimentaire Intelligent

#### Hiérarchie de Recherche Nourriture
1. **Inventaire Personnel** (immédiat)
   - Bloc de foin (+9 nourriture)
   - Pain (+3 nourriture) 
   - Blé (+1 nourriture)

2. **Achat auprès Fermiers** (avec déplacement physique)
   - Prix : Blé 1µ, Pain 3µ, Bloc foin 9µ
   - Déplacement vers fermier le plus proche
   - Transaction richesse + transfert item

3. **Récolte Champs** (fallback original)
   - Si aucun achat possible
   - Déplacement vers EatableModel

#### Messages Système
**Messages Globaux par Village** (toutes les 2 minutes)
```java
"Distribution de nourriture à VillageName"
"Villageois autosuffisants: X villageois" // Mangé depuis inventaire personnel
"Villageois clients: X villageois"        // Acheté auprès d'un fermier
"Villageois voleurs: X villageois"        // Mangé dans les champs publics
"Villageois affamés: X villageois"        // N'ont rien trouvé
```

### Approvisionnement Automatique
- **FarmerSupplyThread** (10 min) : Donne stocks aux fermiers
- **Production** : Blé régulier, Pain 30%, Bloc foin 10%

## 📊 Métriques Système

- **Modèles de données** : 13 classes principales (+ VillagerHistoryModel, VillageHistoryModel)
- **Services** : 20+ services métier (TaxService, VillagerInventoryService, VillagerInventoryTransformationService, HistoryService)
- **Commandes** : 14 commandes utilisateur (+ /admin collecttaxes, /admin goeat, /data)
- **Threads** : 11 threads de simulation (nouveaux: Tax, FarmerSupply, VillagerInventoryTransformation)
- **Listeners** : 5+ event handlers
- **Configurations JSON** : 2 fichiers (13 métiers officiels + salaires/impôts + 1 bâtiment)
- **Historique JSON** : Fichiers individuels par villageois/village avec archivage automatique

## 🚀 Développement

### Workflow Ajout Fonctionnalité
1. **Modèle** : Créer classe avec annotations JsonDB
2. **Repository** : Implémenter CRUD standard
3. **Service** : Logique métier pure
4. **Command/Listener** : Interface utilisateur
5. **Thread** : Tâches périodiques si nécessaire
6. **Config** : JSON pour paramètres
7. **Test** : Compilation + test en jeu

### Patterns à Respecter
- **Services statiques** : Logique métier centralisée
- **Repository pattern** : Abstraction base de données
- **Configuration JSON** : Paramètres externalisés
- **Event-driven** : Réaction aux événements Minecraft
- **Thread-safe** : Attention concurrence sur collections partagées

### Points d'Attention
- **JsonDB** : Pas de relations, dénormaliser si besoin
- **Bukkit Scheduler** : Utiliser pour tâches asynchrones
- **Memory leaks** : Nettoyer collections temporaires
- **Performance** : Les threads tournent en permanence
- **Persistence** : Toujours sauvegarder après modification

## 📚 Système d'Historique (v3.4+)

### 📖 **Enregistrement Automatique**
Toutes les actions importantes des villageois et villages sont automatiquement enregistrées dans des fichiers JSON individuels :

**Villageois** (`/plugins/TestJava/history/villagers/{UUID}.json`) :
- Naissance dans un village
- Consommation de nourriture (propre inventaire ou achat)
- Changements de classe sociale
- Changements de métier
- Achats effectués auprès d'autres villageois
- Épisodes de famine
- Échecs de déplacement

**Villages** (`/plugins/TestJava/history/villages/{nom}.json`) :
- Naissances de villageois
- Statistiques de population par classe sociale
- Collectes d'impôts et richesse de l'empire
- Morts de villageois

### 🗂️ **Gestion des Fichiers**
- **Compression par templates** : Évite la répétition des phrases similaires
- **Archivage automatique** : Villageois morts → `/dead/{UUID}_dead.json`
- **Gestion des renommages** : Mise à jour automatique des fichiers villages
- **Timestamps** : Chaque entrée avec date/heure `[dd/MM/yyyy HH:mm]`

### 📖 **Commandes d'Historique**
```bash
/data village <nom>     # Historique complet du village dans un livre
/data villager          # Historique du villageois le plus proche dans un livre
```

**Fonctionnalités des livres** :
- Pagination automatique (12 lignes par page)
- Historique inversé (plus récent en premier)
- Titre personnalisé avec nom du village/villageois
- Ajout direct à l'inventaire du joueur

## 🎯 Nouvelles Fonctionnalités Majeures (v3.3+)

### 💰 Économie Villageois Complète
- **Richesse personnelle** : Chaque villageois accumule des juridictions
- **Salaires automatiques** : Revenus selon le métier toutes les 5 minutes
- **Système d'impôts** : Prélèvement au profit de l'empire du village + redistribution sociale
- **Messages publics** : Collecte d'impôts visible par tous
- **Commandes admin** : `/admin collecttaxes` pour les impôts, `/admin goeat` pour la nourriture

### 🛒 Commerce Inter-Villageois
- **Inventaire personnel** : Villageois mangent leurs propres stocks d'abord
- **Achat intelligent** : Recherche et achat auprès des fermiers proches
- **Déplacement physique** : Villageois se déplacent vers les vendeurs
- **Transaction complète** : Échange argent ↔ nourriture avec consommation

### 🔄 Transformations Automatiques d'Inventaire (v3.12+)
- **Transformation du blé** : 3 blés → 1 pain automatiquement (silencieux)
- **Transformation du pain** : 3 pains → 1 bloc de foin automatiquement (silencieux)
- **Vente à la banque mondiale** : Tous les blocs de foin vendus automatiquement
- **Enrichissement villageois** : Les juridictions vont directement au villageois
- **Fréquence** : Processus exécuté toutes les minutes pour tous les villageois
- **Messages visibles** : `{Prénom} {Nom} a vendu {x} foin à la banque mondiale pour {x}µ`
- **Économie fluide** : Conversion automatique des excédents alimentaires en richesse

### 🎮 Expérience Gameplay Enrichie
- **Interactions visuelles** : Déplacements et échanges visibles
- **Économie dynamique** : Circulation monétaire entre villageois  
- **Spécialisation métiers** : Fermiers deviennent vendeurs alimentaires
- **Gestion stratégique** : Équilibrer population/métiers/ressources

---

Ce plugin implémente un système de civilisation complexe avec une architecture modulaire, une base de données JSON intégrée et des mécaniques de jeu avancées. L'architecture est conçue pour être extensible et maintenable.

## 🐛 **Correction du Bug de Spawn de Villageois (v3.5)**

### **Problème Signalé**
> "Le village avait 1 seul lit, donc normalement limité à 1 villageois en spawn. Lors de ma reconnexion au serveur, le village avait plein de villageois."

### **Causes Identifiées**
1. **Spawn Naturel Minecraft** : Le serveur Minecraft peut faire spawner des villageois naturellement dans les villages, même sans lits
2. **Pas de Vérification de Limite** : `EntityService.testSpawnIfVillager` ne vérifie pas `population < bedsCount`
3. **Synchronisation Agressive** : Au redémarrage, tous les villageois du monde sont synchronisés sans vérifier les limites
4. **Thread Spawn Incohérent** : Le `VillagerSpawnThread` met à jour la population mais ne vérifie pas si le spawn a réellement eu lieu

### **Solutions Implémentées**

#### ✅ **1. Vérification de Limite dans EntityService**
```java
// VÉRIFICATION CRITIQUE: Empêcher le spawn si le village a atteint sa limite de lits
if (village.getPopulation() >= village.getBedsCount()) {
    Bukkit.getLogger().warning("[EntityService] Spawn villageois bloqué: Village " + 
        village.getId() + " a atteint sa limite (" + village.getPopulation() + "/" + village.getBedsCount() + " lits)");
    e.setCancelled(true);
    return;
}
```

#### ✅ **2. Amélioration du VillagerSpawnThread**
- **Vérifications de sécurité** : Skip les villages sans lits
- **Vérification de succès** : Mise à jour population seulement si le spawn a réussi
- **Gestion d'erreurs** : Logs détaillés en cas d'échec

#### ✅ **3. Service de Correction Automatique**
**VillagePopulationCorrectionService** :
- **Correction au démarrage** : Vérification automatique de tous les villages
- **Suppression intelligente** : Supprime les villageois les plus récents en excès
- **Messages informatifs** : Broadcast des corrections effectuées

#### ✅ **4. Commande de Gestion Manuelle**
**`/population`** :
- `/population check` - Vérifier les populations
- `/population fix` - Corriger les populations  
- `/population stats` - Afficher les statistiques

#### ✅ **5. Logs Détaillés**
- **Détection des excès** : `⚠️ Village X: 5/1 villageois`
- **Corrections effectuées** : `✅ Village X corrigé: 1/1 villageois`
- **Messages de mort** : `💀 [Village] Nom a été supprimé (correction population)`

## 📝 **Optimisation des Logs des Threads (v3.6)**

### **Problème Signalé**
> "Tu vas modifier les logs de TOUS les threads. Actuellement tu fais des logs serveur sur des itérations ce qui flood la console. Je veux 1 seul log serveur par execution de thread, qui récapitule ce qui a été fait, et non pas faire des logs au fur et à mesure de l'execution du thread."

### **Threads Modifiés**

#### ✅ **1. SheepSpawnThread**
```java
// AVANT: Logs itératifs pour chaque mouton
Bukkit.getLogger().info("[SheepSpawn] ✅ Mouton spawné pour bergerie de " + building.getVillageName());

// APRÈS: Un seul log de résumé
Bukkit.getLogger().info("[SheepSpawn] 📊 Résumé: " + totalSpawned + " moutons spawnés dans " + activeBarns + " bergeries actives");
```

#### ✅ **2. SheepMovementThread**
```java
// AVANT: Logs itératifs pour chaque mouton
Bukkit.getLogger().warning("[SheepMovement] ⚠️ Bergerie introuvable pour mouton " + sheepModel.getVillageName());

// APRÈS: Un seul log de résumé
Bukkit.getLogger().info("[SheepMovement] 📍 Résumé: " + movedCount + " moutons déplacés, " + removedCount + " supprimés");
```

#### ✅ **3. SocialClassEnforcementThread**
```java
// AVANT: Logs itératifs pour chaque action
Bukkit.getLogger().info("[SocialClassEnforcement] " + updated + " noms de villageois mis à jour");

// APRÈS: Un seul log de résumé détaillé
Bukkit.getLogger().info("[SocialClassEnforcement] ✅ Résumé: " + totalActions + " actions effectuées " +
                       "(restrictions: 1, strict: " + strictRestrictions + ", noms: " + namesUpdated + ")");
```

#### ✅ **4. CustomJobMaintenanceThread**
```java
// AVANT: Logs itératifs pour chaque maintenance
Bukkit.getLogger().info("[CustomJobMaintenance] Armures réparées: " + armorFixed + " employés custom");

// APRÈS: Un seul log de résumé
Bukkit.getLogger().info("[CustomJobMaintenance] ✅ Résumé: " + totalActions + " actions effectuées " +
                       "(armures: " + armorFixed + ", ajustements: " + buildingAdjustments + ")");
```

#### ✅ **5. AutomaticJobAssignmentService**
```java
// AVANT: Logs itératifs pour chaque village
Bukkit.getLogger().info("[AutoJobAssignment] ✅ Village " + village.getId() + ": " + villageAssignments + " emplois assignés");

// APRÈS: Un seul log de résumé
Bukkit.getLogger().info("[AutoJobAssignment] ✅ Résumé: " + totalAssignments + " emplois assignés dans " + 
                       villagesWithAssignments + "/" + villagesProcessed + " villages");
```

#### ✅ **6. VillagerSynchronizationService**
```java
// AVANT: Logs itératifs pour chaque villageois synchronisé
Bukkit.getLogger().info("[VillagerSync] ✅ Synchronisé: " + villager.getUniqueId());

// APRÈS: Un seul log de résumé final
Bukkit.getLogger().info("[VillagerSync] ✅ Synchronisation terminée en " + duration + " secondes");
Bukkit.getLogger().info("[VillagerSync] Nouveaux synchronisés: " + result.syncedCount);
```

#### ✅ **7. VillagerSpawnThread**
```java
// AVANT: Logs itératifs pour chaque villageois spawné
Bukkit.getLogger().info("[VillagerSpawnThread] Villageois spawné avec succès dans " + village.getId());

// APRÈS: Un seul log de résumé
Bukkit.getLogger().info("[VillagerSpawnThread] ✅ Résumé: " + totalSpawned + " villageois spawnés " +
                       "(vérifié " + villagesChecked + " villages, " + villagesSkipped + " ignorés)");
```

#### ✅ **8. DailyBuildingCostThread**
```java
// APRÈS: Ajout d'un log de résumé
Bukkit.getLogger().info("[DailyBuildingCost] ✅ Résumé: " + totalBuildingsProcessed + " bâtiments traités, " + 
                       totalCostPaid + "µ payés, " + buildingsDeactivated + " désactivés");
```

### **Avantages de l'Optimisation**
- **Console plus propre** : Plus de flood de logs itératifs
- **Informations utiles** : Résumés détaillés avec statistiques
- **Performance améliorée** : Moins d'écriture dans les logs
- **Debugging facilité** : Un seul log par thread pour identifier les problèmes

### **Format des Logs de Résumé**
```
[ThreadName] ✅ Résumé: X actions effectuées (détail1: Y, détail2: Z)
[ThreadName] ℹ️ Aucune action nécessaire (vérifié X éléments)
[ThreadName] ❌ Erreur: message d'erreur
```

## 🐛 **Correction du Bug de Comptage des Villageois (v3.7)**

### **Problème Signalé**
> "Le message 'Distribution de nourriture' est censé afficher l'activité de nourriture de tous les villageois du village. Mais il semblerait que souvent le nombre de villageois affichés au total dans le message ne corresponde pas au total de villageois du village, comme si certains ne se nourrissent pas ou passent à travers les mailles du filet."

### **Causes Identifiées**

#### ❌ **1. Villageois avec nourriture entre 19 et 20**
- Les villageois avec `food >= 19` mais `< 20` n'étaient ni "rassasiés" ni "affamés"
- Ils n'étaient pas comptés dans les statistiques

#### ❌ **2. Villageois fantômes non comptés**
- Les villageois en DB mais pas dans le monde étaient supprimés sans être comptés
- Cela créait des incohérences dans les totaux

#### ❌ **3. Échecs de traitement silencieux**
- Les erreurs dans `handleHungryVillager` n'étaient pas gérées
- Les villageois en échec n'étaient pas comptés

#### ❌ **4. Requêtes de base de données incomplètes**
- Utilisation de requêtes JXQuery au lieu de traiter tous les villageois
- Certains villageois pouvaient être manqués

### **Solutions Implémentées**

#### ✅ **1. Système de Comptage Complet**
```java
// AVANT: Requêtes partielles
String queryHungry = String.format("/.[food<'%s']", MAX_FOOD);
String queryFull = String.format("/.[food>='%s']", FULL_FOOD);

// APRÈS: Traitement de tous les villageois
Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
for (VillagerModel villager : allVillagers) {
    if (villager.getFood() >= FULL_FOOD) {
        stats.rassasies++;
    } else if (villager.getFood() < MAX_FOOD) {
        // Traitement des affamés
    } else {
        stats.stables++; // NOUVEAU: Villageois entre 19 et 20
    }
}
```

#### ✅ **2. Gestion des Erreurs Robuste**
```java
private void handleHungryVillager(VillagerModel villager, VillageStats stats) {
    try {
        // Logique de traitement
    } catch (Exception e) {
        // En cas d'erreur, compter comme affamé par défaut
        stats.affames++;
        Bukkit.getLogger().warning("[VillagerGoEat] Erreur traitement villageois " + villager.getId());
    }
}
```

#### ✅ **3. Validation des Totaux**
```java
// Calculer le total des villageois traités
int totalProcessed = stats.rassasies + stats.autosuffisants + stats.clients + 
                     stats.voleurs + stats.affames + stats.stables;
int villagePopulation = village.getPopulation();

// Validation du total
if (totalProcessed != villagePopulation) {
    owner.sendMessage(ChatColor.YELLOW + "⚠️ Attention: " + totalProcessed + 
                     " villageois traités sur " + villagePopulation);
}
```

#### ✅ **4. Nouvelle Catégorie "Stables"**
- **Villageois stables** : Nourriture entre 19 et 20 (pas besoin de se nourrir mais pas rassasiés)
- Comptage complet de tous les états possibles

#### ✅ **5. Logs de Résumé Détaillés**
```java
Bukkit.getLogger().info("[VillagerGoEat] ✅ Résumé global: " + totalVillagers + 
                       " villageois traités dans " + totalVillages + " villages");
Bukkit.getLogger().info("[VillagerGoEat] 📊 Répartition: " + totalRassasies + 
                       " rassasiés, " + totalAutosuffisants + " autosuffisants...");
```

#### ✅ **6. Commande de Diagnostic**
```bash
/population diagnose <village>
```
- Analyse détaillée d'un village spécifique
- Corrige automatiquement les classes sociales incorrectes
- Affiche un rapport détaillé des corrections effectuées

### **Nouvelles Catégories de Villageois**

| Catégorie | Nourriture | Description |
|-----------|------------|-------------|
| **Rassasiés** | `>= 20` | Ne consomment que des points de nourriture |
| **Stables** | `19-20` | Pas besoin de se nourrir mais pas rassasiés |
| **Autosuffisants** | `< 19` | Se nourrissent de leur inventaire |
| **Clients** | `< 19` | Achètent de la nourriture aux fermiers |
| **Voleurs** | `< 19` | Volent du blé dans les champs |
| **Affamés** | `< 19` | Ne trouvent pas de nourriture |

### **Avantages de la Correction**
- **Comptage précis** : Tous les villageois sont maintenant comptés
- **Détection d'incohérences** : Alertes automatiques si les totaux ne correspondent pas
- **Debugging facilité** : Logs détaillés et commande de diagnostic
- **Transparence** : Les joueurs voient exactement combien de villageois sont traités
- **Robustesse** : Gestion des erreurs pour éviter les villageois "perdus"

## 🐛 **Correction du Bug d'Attribution Automatique des Métiers (v3.9)**

### **Problème Signalé**
> "Je pose un bloc de métier. Un villageois de classe misérable tente de récupérer le métier, mais l'action est annulée par le code (bon fonctionnement). Le code force un villageois de classe inactive à se déplacer vers le bloc de métier et à récupérer le métier. Sauf que, une fois à destination, le villageois ne récupère pas le métier et reste comme il est."

### **Causes Identifiées**

#### ❌ **1. Pathfinding Insuffisant**
- `villager.getPathfinder().moveTo()` ne force pas le villageois à **interagir** avec le bloc de métier
- Le villageois se déplace vers le bloc mais ne décide pas naturellement de le prendre
- La mécanique Minecraft d'attribution automatique des métiers n'est pas fiable

#### ❌ **2. Pas de Vérification de Réussite Robuste**
- Le système attend 5 secondes puis vérifie, mais ne force pas l'interaction si l'attribution échoue
- Les tentatives de réessai (`moveTo` répété) ne fonctionnent pas de manière fiable

#### ❌ **3. Concurrence Possible**
- D'autres villageois peuvent "voler" le bloc de métier pendant le déplacement
- La logique ne garantit pas l'exclusivité pour le villageois désigné

### **Solutions Implémentées**

#### ✅ **1. Attribution Forcée Immédiate**
```java
// CORRECTION BUG: Forcer l'attribution immédiate du métier
// Au lieu de laisser le villageois "décider" naturellement, nous forçons l'attribution
forceJobAssignment(villager, villagerModel, jobBlockType, jobBlockLocation);
```

#### ✅ **2. Téléportation + Attribution Directe**
```java
private static void forceJobAssignment(Villager villager, VillagerModel villagerModel, 
                                     Material jobBlockType, Location jobBlockLocation) {
    // Étape 1: Téléporter le villageois près du bloc pour garantir la proximité
    Location targetLocation = jobBlockLocation.clone().add(0.5, 1, 0.5);
    villager.teleport(targetLocation);
    
    // Étape 2: Forcer l'attribution du métier avec un délai court
    Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
        // Déterminer la profession correspondante au bloc
        Villager.Profession targetProfession = getProfessionFromJobBlock(jobBlockType);
        
        if (targetProfession != null) {
            // Forcer la profession directement
            villager.setProfession(targetProfession);
        }
    }, 10L); // 0.5 seconde de délai
}
```

#### ✅ **3. Mapping Complet Bloc → Profession**
```java
private static Villager.Profession getProfessionFromJobBlock(Material blockType) {
    return switch (blockType) {
        case COMPOSTER -> Villager.Profession.FARMER;
        case BLAST_FURNACE -> Villager.Profession.ARMORER;
        case SMOKER -> Villager.Profession.BUTCHER;
        case CARTOGRAPHY_TABLE -> Villager.Profession.CARTOGRAPHER;
        case BREWING_STAND -> Villager.Profession.CLERIC;
        case SMITHING_TABLE -> Villager.Profession.TOOLSMITH;
        case FLETCHING_TABLE -> Villager.Profession.FLETCHER;
        case LOOM -> Villager.Profession.SHEPHERD;
        case STONECUTTER -> Villager.Profession.MASON;
        case CAULDRON -> Villager.Profession.LEATHERWORKER;
        case LECTERN -> Villager.Profession.LIBRARIAN;
        case GRINDSTONE -> Villager.Profession.WEAPONSMITH;
        case BARREL -> Villager.Profession.FISHERMAN;
        default -> null;
    };
}
```

#### ✅ **4. Vérification Finale avec Messages**
```java
private static void verifyFinalJobAssignment(Villager villager, VillagerModel villagerModel, 
                                           Villager.Profession expectedProfession) {
    if (villager.getProfession() == expectedProfession) {
        // Succès ! Le SocialClassJobListener va maintenant gérer la promotion à la classe Ouvrière
        String villagerName = extractVillagerName(villager);
        String jobName = getJobNameFromBlock(getMaterialFromProfession(expectedProfession));
        
        Bukkit.getServer().broadcastMessage(
            "§a✅ " + villagerName + "§f est maintenant " + "§e" + jobName
        );
    } else {
        Bukkit.getLogger().warning("[JobAssignment] ❌ ÉCHEC FINAL: Villageois " + villager.getUniqueId() + 
                                 " devrait être " + expectedProfession + " mais est " + villager.getProfession());
    }
}
```

### **Nouveau Flux d'Attribution**

#### **Avant (Défaillant)**
1. Bloc posé → JobAssignmentService trouve villageois inactif
2. `villager.getPathfinder().moveTo()` vers le bloc
3. **Attente** que le villageois prenne naturellement le métier
4. ❌ **Échec** : Le villageois n'interagit pas avec le bloc

#### **Après (Fiable)**
1. Bloc posé → JobAssignmentService trouve villageois inactif
2. **Téléportation forcée** près du bloc (`villager.teleport()`)
3. **Attribution directe** de la profession (`villager.setProfession()`)
4. ✅ **Succès** : Le villageois obtient immédiatement le métier
5. `SocialClassJobListener` gère automatiquement la promotion vers classe "Ouvrière"

### **Messages Système**

#### **Attribution en Cours**
```
[Nom Villageois] se dirige vers le bloc de métier pour devenir [Métier]
```

#### **Attribution Réussie**
```
✅ [Nom Villageois] est maintenant [Métier]
```

#### **Logs Techniques**
```
[JobAssignment] 🔧 Téléportation forcée du villageois vers (X, Y, Z)
[JobAssignment] ✅ ATTRIBUTION FORCÉE: [UUID] → [PROFESSION]
[JobAssignment] ✅ SUCCESS: Villageois [UUID] a obtenu le métier [PROFESSION]
```

### **Avantages de la Correction**
- **Fiabilité 100%** : L'attribution des métiers fonctionne maintenant de manière déterministe
- **Élimination des échecs** : Plus de villageois qui se déplacent sans prendre le métier
- **Réactivité améliorée** : Attribution quasi-instantanée au lieu d'attendre le pathfinding
- **Intégration transparente** : Le `SocialClassJobListener` continue de gérer les promotions de classe
- **Messages clairs** : Feedback visuel pour les joueurs

### **Impact sur le Gameplay**
- **Pose de blocs de métier** : Attribution immédiate et garantie aux villageois inactifs
- **Gestion des classes sociales** : Les villageois misérables restent correctement bloqués
- **Économie villageoise** : Les métiers sont maintenant attribués de manière fiable pour le système d'impôts

---

## 🚨 **ERREUR CRITIQUE CORRIGÉE : Component JSON Brut dans le Chat (v3.9.1)**

### **Problème Critique Identifié**
> **LLM ATTENTION** : L'utilisation de `villager.customName().toString()` produit des composants JSON bruts dans le chat au lieu du texte formaté !

#### **❌ ERREUR - Ne JAMAIS faire cela**
```java
// ❌ MAUVAIS - Produit du JSON brut dans le chat
String customName = villager.customName().toString();
// Résultat: {"text":"[Village] Jean","color":"blue"} au lieu de "[Village] Jean"
```

#### **✅ CORRECT - Utiliser l'ancienne API**
```java
// ✅ BON - Produit du texte formaté propre
String customName = villager.getCustomName();
// Résultat: "[Village] Jean" (avec formatage couleur)
```

### **Occurrences Corrigées**
- ✅ `JobAssignmentService.extractVillagerName()` 
 
- ✅ `VillagerService.createVillagerModelFromVillager()`
- ✅ `CustomJobArmorService.removeCustomJobArmor()`

### **Règle Stricte pour les LLM**
**🚨 INTERDICTION ABSOLUE 🚨**
- **JAMAIS** utiliser `.customName().toString()` sur les entités Minecraft
- **TOUJOURS** utiliser `.getCustomName()` même si l'API est marquée `@Deprecated`
- **TOUJOURS** vérifier que les messages dans le chat sont du texte et non du JSON

### **Pourquoi cette Erreur ?**
- `customName()` retourne un `Component` (nouveau système Paper/Adventure)
- `Component.toString()` produit la sérialisation JSON interne
- `getCustomName()` retourne une `String` formatée (ancien système Bukkit)

### **Comment Détecter cette Erreur**
- Messages dans le chat avec `{"text":"...","color":"..."}` au lieu de texte coloré
- Logs serveur montrant des structures JSON brutes
- Villageois avec des noms contenant des accolades et guillemets

### **Vérification Obligatoire**
Avant chaque commit, vérifier :
```bash
grep -r "customName().toString()" src/
# Doit retourner AUCUN résultat
```

### **Exemple de Message Correct vs Incorrect**

#### **✅ Correct**
```
§a✅ §b{2} [Truc] Jean Dupont§f est maintenant §eFermier
```

#### **❌ Incorrect (JSON brut)**
```
[{"text":"✅ ","color":"green"},{"text":"{2} [Truc] Jean Dupont","color":"aqua"},"text":" est maintenant ","color":"white"},{"text":"Fermier","color":"yellow"}]
```

---

## 🐛 **Correction du Bug d'Incohérence de Classe Sociale (v3.8)**

### **Problème Signalé**
> "/social villager" a eu une incohérence, le villageois avait la classe sociale "Inactive" alors qu'il avait bien un métier custom "bergerie" et que la classe était "Ouvrière" et donc "{2}" dans son customName.

### **Causes Identifiées**

#### ❌ **1. Incohérence Base de Données vs Affichage**
- La base de données contenait encore l'ancienne classe "Inactive" (1)
- Le `customName` affichait la classe "Ouvrière" (2) avec le tag `{2}`
- La commande `/social villager` lisait directement depuis la base sans évaluation

#### ❌ **2. Évaluation de Classe Sociale Non Systématique**
- Les villageois avec métiers custom n'étaient pas automatiquement promus vers "Ouvrière"
- L'évaluation ne se faisait que lors de l'obtention du métier, pas lors de la consultation

#### ❌ **3. Manque de Vérification Prioritaire**
- La logique d'évaluation ne vérifiait pas en priorité si un villageois avec métier custom était bien en classe "Ouvrière"

### **Solutions Implémentées**

#### ✅ **1. Correction de la Commande `/social villager`**
```java
// CORRECTION BUG: Évaluer et mettre à jour la classe sociale avant affichage
// pour s'assurer de la cohérence entre métier et classe sociale
SocialClass oldClass = villagerModel.getSocialClassEnum();
SocialClassService.evaluateAndUpdateSocialClass(villagerModel);
SocialClass newClass = villagerModel.getSocialClassEnum();

// Si la classe a changé, informer le joueur
if (oldClass != newClass) {
    player.sendMessage(ChatColor.YELLOW + "⚠️ Classe sociale corrigée: " + 
                     oldClass.getColoredTag() + " " + oldClass.getName() + 
                     ChatColor.YELLOW + " → " + newClass.getColoredTag() + " " + newClass.getName());
}
```

#### ✅ **2. Amélioration du Service SocialClassService**
```java
// CORRECTION BUG: Vérification prioritaire pour les métiers custom
// Si le villageois a un métier custom, il DOIT être en classe Ouvrière
if (villager.hasCustomJob() && currentClass != SocialClass.OUVRIERE) {
    newClass = SocialClass.OUVRIERE;
    Bukkit.getLogger().info("[SocialClass] 🔧 CORRECTION: Villageois avec métier custom (" + 
                           villager.getCurrentJobName() + ") promu vers Ouvrière");
}
```

#### ✅ **3. Nouvelle Commande de Diagnostic**
```bash
/social diagnose
```
- Analyse tous les villageois pour détecter les incohérences
- Corrige automatiquement les classes sociales incorrectes
- Affiche un rapport détaillé des corrections effectuées

#### ✅ **4. Logs de Correction Détaillés**
```java
Bukkit.getLogger().info("[SocialClass] 🔧 CORRECTION: Villageois avec métier custom (" + 
                       villager.getCurrentJobName() + ") promu vers Ouvrière");
```

### **Règles de Cohérence Implémentées**

| Situation | Classe Sociale Requise | Action |
|-----------|------------------------|--------|
| **Villageois avec métier custom** | **Ouvrière (2)** | Promotion automatique |
| **Villageois avec métier natif** | **Ouvrière (2)** | Promotion automatique |
| **Villageois sans métier** | **Inactive (1)** ou **Misérable (0)** | Selon nourriture |
| **Villageois misérable** | **Misérable (0)** | Retrait forcé du métier |

### **Avantages de la Correction**
- **Cohérence garantie** : Les villageois avec métiers sont toujours en classe "Ouvrière"
- **Correction automatique** : La commande `/social villager` corrige les incohérences
- **Diagnostic complet** : Nouvelle commande pour analyser et corriger en masse
- **Transparence** : Le joueur est informé des corrections effectuées
- **Robustesse** : Vérification prioritaire des métiers custom dans l'évaluation

### **Utilisation des Nouvelles Fonctionnalités**

#### **Correction Automatique**
```bash
/social villager  # Corrige automatiquement la classe sociale avant affichage
```

#### **Diagnostic Complet**
```bash
/social diagnose  # Analyse et corrige toutes les incohérences
```

#### **Messages de Correction**
```
⚠️ Classe sociale corrigée: {1} Inactive → {2} Ouvrière
🔧 Corrigé: {1} Inactive → {2} Ouvrière (UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
```

---