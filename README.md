# Plugin Minecraft Civilization - Guide d'Architecture

Plugin Minecraft Java reproduisant Civilization 6 dans un environnement multijoueur. Ce document sert de guide architectural pour comprendre et développer le plugin.

## 🏗️ Stack Technique

- **Java 21** - Langage principal
- **Paper API 1.21.8** - API Minecraft moderne
- **JsonDB** - Base de données JSON pour la persistence
- **Maven** - Gestionnaire de dépendances et build
- **Jackson** - Sérialisation/désérialisation JSON

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
- **Économie Villageois** : Richesse personnelle, salaires et impôts par métier
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

**TaxService** - Système d'impôts villageois
```java
public static void collectTaxes() {
    // Pour chaque villageois avec métier:
    // 1. Payer salaire selon JobDistanceConfig
    // 2. Collecter impôts (% du salaire)
    // 3. Verser impôts à l'empire
    // 4. Message global de collecte
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

**VillagerTaxThread** (5 min) - Collecte d'impôts
```java
// Collecte automatique d'impôts des villageois avec métier
TaxService.collectTaxes();
// Message: "💰 Collecte d'impôts terminée: XXXµ collectés auprès de X villageois"
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
```

**FarmerSupplyThread** (10 min) - Approvisionnement fermiers
```java
// Donne des stocks alimentaires aux fermiers pour qu'ils puissent vendre
// Blé: production régulière, Pain: 30% chance, Bloc foin: 10% chance
VillagerInventoryService.giveFoodToFarmers();
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
- ✅ **Réinitialisation navigation** : `resetVillagerHome()` avant téléportation
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
- **Services** : 19+ services métier (TaxService, VillagerInventoryService, HistoryService)
- **Commandes** : 14 commandes utilisateur (+ /data)
- **Threads** : 10 threads de simulation (nouveaux: Tax, FarmerSupply)
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
- **Système d'impôts** : Prélèvement au profit de l'empire du village
- **Messages publics** : Collecte d'impôts visible par tous

### 🛒 Commerce Inter-Villageois
- **Inventaire personnel** : Villageois mangent leurs propres stocks d'abord
- **Achat intelligent** : Recherche et achat auprès des fermiers proches
- **Déplacement physique** : Villageois se déplacent vers les vendeurs
- **Transaction complète** : Échange argent ↔ nourriture avec consommation

### 🎮 Expérience Gameplay Enrichie
- **Interactions visuelles** : Déplacements et échanges visibles
- **Économie dynamique** : Circulation monétaire entre villageois  
- **Spécialisation métiers** : Fermiers deviennent vendeurs alimentaires
- **Gestion stratégique** : Équilibrer population/métiers/ressources

---

Ce plugin implémente un système de civilisation complexe avec une architecture modulaire, une base de données JSON intégrée et des mécaniques de jeu avancées. L'architecture est conçue pour être extensible et maintenable.

**🔄 Auto-Update README Policy** : Ce document est automatiquement maintenu à jour selon `.readme-update-policy.md`