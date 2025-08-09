package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.BuildingRepository;
import TestJava.testjava.helpers.Colorize;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Villager;
import org.bukkit.Chunk;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service pour gérer les gardes forestiers - plantation automatique d'arbres
 * Appelé après le paiement des taxes dans TaxService
 */
public class ForestGuardService {
    
    // Types de saplings disponibles avec leurs poids de probabilité
    private static final Map<Material, Integer> SAPLING_TYPES = Map.of(
        Material.OAK_SAPLING, 30,      // Le plus commun
        Material.BIRCH_SAPLING, 25,
        Material.SPRUCE_SAPLING, 20,
        Material.JUNGLE_SAPLING, 10,
        Material.ACACIA_SAPLING, 8,
        Material.DARK_OAK_SAPLING, 5,
        Material.CHERRY_SAPLING, 2     // Le plus rare
    );
    
    // Rayon de recherche autour du bâtiment garde forestier
    private static final int SEARCH_RADIUS = 50;
    
    // Hauteur minimum d'espace libre au-dessus du sapling pour la croissance
    private static final int MIN_HEIGHT_CLEARANCE = 6;
    
    /**
     * Déclenche la plantation d'arbre après le paiement des taxes
     * 
     * @param villagerModel Le modèle du garde forestier
     * @param villagerEntity L'entité du garde forestier
     */
    public static void triggerTreePlantingAfterSalary(VillagerModel villagerModel, Villager villagerEntity) {
        Bukkit.getLogger().info("[ForestGuardService] DEBUG - triggerTreePlantingAfterSalary appelé pour " + villagerModel.getId());
        
        if (villagerModel == null || villagerEntity == null) {
            Bukkit.getLogger().warning("[ForestGuardService] Garde forestier ou modèle villageois null");
            return;
        }
        
        String villageName = villagerModel.getVillageName();
        if (villageName == null) {
            Bukkit.getLogger().warning("[ForestGuardService] Village introuvable pour le garde forestier: " + villagerModel.getId());
            return;
        }
        
        if (TestJava.world == null) {
            Bukkit.getLogger().warning("[ForestGuardService] Monde introuvable");
            return;
        }
        
        // Trouver le bâtiment garde forestier associé
        BuildingModel forestGuardBuilding = findForestGuardBuilding(villageName);
        if (forestGuardBuilding == null) {
            Bukkit.getLogger().warning("[ForestGuardService] Aucun bâtiment garde forestier trouvé dans le village " + villageName);
            return;
        }
        
        Bukkit.getLogger().info("[ForestGuardService] DEBUG - Bâtiment garde forestier trouvé: " + forestGuardBuilding.getId() + " à (" + forestGuardBuilding.getX() + "," + forestGuardBuilding.getY() + "," + forestGuardBuilding.getZ() + ")");
        
        // Déplacer le garde vers le bâtiment puis planter un arbre
        moveToWorkplaceAndPlantTree(villagerEntity, villagerModel, forestGuardBuilding);
    }
    
    /**
     * Trouve le bâtiment garde forestier d'un village
     */
    private static BuildingModel findForestGuardBuilding(String villageName) {
        Collection<BuildingModel> buildings = BuildingRepository.getAll();
        
        Bukkit.getLogger().info("[ForestGuardService] DEBUG - Recherche bâtiment garde_forestier pour village: " + villageName);
        Bukkit.getLogger().info("[ForestGuardService] DEBUG - Nombre total de bâtiments: " + buildings.size());
        
        for (BuildingModel building : buildings) {
            Bukkit.getLogger().info("[ForestGuardService] DEBUG - Bâtiment trouvé: " + building.getBuildingType() + " village: " + building.getVillageName() + " actif: " + building.isActive());
            
            if ("garde_forestier".equals(building.getBuildingType()) && 
                villageName.equals(building.getVillageName()) &&
                building.isActive()) {
                return building;
            }
        }
        
        return null;
    }
    
    /**
     * Déplace le garde forestier vers son lieu de travail et plante un arbre
     */
    @SuppressWarnings("deprecation")
    private static void moveToWorkplaceAndPlantTree(Villager villagerEntity, VillagerModel villagerModel, BuildingModel building) {
        Location buildingLocation = new Location(TestJava.world, building.getX(), building.getY(), building.getZ());
        // Charger le chunk de destination pour éviter les soucis de pathfinding sur longue distance
        try {
            Chunk destChunk = buildingLocation.getChunk();
            if (!destChunk.isLoaded()) destChunk.load(true);
        } catch (Exception ignored) {}
        final String guardDisplayName;
        if (villagerEntity.customName() != null) {
            guardDisplayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText()
                    .serialize(villagerEntity.customName());
        } else {
            guardDisplayName = "Garde Forestier";
        }
        
        // Debug logs pour diagnostiquer le problème
        Bukkit.getLogger().info("[ForestGuardService] DEBUG - Garde forestier " + villagerModel.getId());
        Bukkit.getLogger().info("[ForestGuardService] DEBUG - Villager valide: " + (villagerEntity != null && !villagerEntity.isDead() && villagerEntity.isValid()));
        Bukkit.getLogger().info("[ForestGuardService] DEBUG - Position actuelle: " + villagerEntity.getLocation());
        Bukkit.getLogger().info("[ForestGuardService] DEBUG - Destination: " + buildingLocation);
        Bukkit.getLogger().info("[ForestGuardService] DEBUG - Distance: " + villagerEntity.getLocation().distance(buildingLocation));
        Bukkit.getLogger().info("[ForestGuardService] DEBUG - Même monde: " + (villagerEntity.getWorld() == buildingLocation.getWorld()));
        
        // Logs de debug avant l'appel au VillagerMovementManager
        Bukkit.getLogger().info("[ForestGuardService] DEBUG - Tentative de création de tâche de déplacement");
        Bukkit.getLogger().info("[ForestGuardService] DEBUG - Villageois valide avant déplacement: " + (villagerEntity != null && !villagerEntity.isDead() && villagerEntity.isValid()));
        Bukkit.getLogger().info("[ForestGuardService] DEBUG - Destination valide: " + (buildingLocation != null && buildingLocation.getWorld() != null));
        
        // Utiliser le système centralisé de déplacement
        UUID taskId = VillagerMovementManager.moveVillager(villagerEntity, buildingLocation)
            .withSuccessDistance(3.5)
            .withMoveSpeed(1.2)
            .withTimeout(60)
            .onSuccess(() -> {
                Bukkit.getLogger().info("[ForestGuardService] DEBUG - Succès du déplacement vers lieu de travail");
                // Une fois arrivé au lieu de travail, chercher un endroit pour planter
                findPlantingLocationAndPlant(villagerEntity, villagerModel, buildingLocation, guardDisplayName);
            })
            .onFailure(() -> {
                Bukkit.getLogger().warning("[ForestGuardService] DEBUG - Échec du déplacement vers lieu de travail - tentative de plantation directe à proximité");
                // Fallback: planter quand même près du lieu de travail
                plantNearWorkplaceDirectly(buildingLocation, guardDisplayName);
                Bukkit.getServer().broadcastMessage("🌲 " + Colorize.name(guardDisplayName) + " n'a pas pu rejoindre son poste, mais a planté un arbre à proximité.");
            })
            .onPositionUpdate((distance, attempts) -> {
                // Log périodique pour suivre le déplacement
                if (attempts % 10 == 0) { // Log toutes les 10 tentatives (environ 10 secondes)
                    Bukkit.getLogger().info("[ForestGuardService] DEBUG - Déplacement en cours: distance=" + String.format("%.2f", distance) + ", tentatives=" + attempts);
                }
            })
            .withName("ForestGuard-MoveToWorkplace")
            .start();
            
        if (taskId != null) {
            Bukkit.getLogger().info("[ForestGuardService] DEBUG - Tâche de déplacement créée avec ID: " + taskId);
        } else {
            Bukkit.getLogger().warning("[ForestGuardService] DEBUG - Échec de création de la tâche de déplacement - validation échouée");
        }
            
        Bukkit.getLogger().info("[ForestGuardService] Garde forestier " + villagerModel.getId() + 
                               " se dirige vers son lieu de travail pour planter un arbre");
    }
    
    /**
     * Trouve un endroit libre autour du lieu de travail et plante un arbre
     */
    @SuppressWarnings("deprecation")
    private static void findPlantingLocationAndPlant(Villager villagerEntity, VillagerModel villagerModel, 
                                                    Location workplaceLocation, String guardName) {
        
        Location plantingSpot = findSuitablePlantingLocation(workplaceLocation);
        
        if (plantingSpot == null) {
            Bukkit.getServer().broadcastMessage("🌲 " + Colorize.name(guardName) + " ne trouve pas d'endroit libre pour planter un arbre");
            return;
        }
        
        // Assurer que le chunk de plantation est chargé
        try {
            Chunk plantChunk = plantingSpot.getChunk();
            if (!plantChunk.isLoaded()) plantChunk.load(true);
        } catch (Exception ignored) {}
        
        // Déplacer le garde vers l'endroit de plantation
        VillagerMovementManager.moveVillager(villagerEntity, plantingSpot)
            .withSuccessDistance(2.0)
            .withTimeout(45)
            .onSuccess(() -> {
                // Planter le sapling
                plantSaplingAndGrow(villagerEntity, plantingSpot, guardName);
            })
            .onFailure(() -> {
                // Fallback: planter même si le garde n'a pas pu s'y rendre
                Bukkit.getLogger().warning("[ForestGuardService] DEBUG - Échec d'accès à l'endroit de plantation - plantation forcée");
                plantSaplingAndGrow(null, plantingSpot, guardName);
                Bukkit.getServer().broadcastMessage("🌲 " + Colorize.name(guardName) + " n'a pas pu atteindre l'endroit, mais a planté un arbre à distance.");
            })
            .withName("ForestGuard-MoveToPlantingSpot")
            .start();
    }
    
    // NOUVEAU: Plantation directe autour du lieu de travail sans déplacement du garde
    @SuppressWarnings("deprecation")
    private static void plantNearWorkplaceDirectly(Location workplaceLocation, String guardName) {
        Location spot = findSuitablePlantingLocation(workplaceLocation);
        if (spot != null) {
            try {
                Chunk c = spot.getChunk();
                if (!c.isLoaded()) c.load(true);
            } catch (Exception ignored) {}
            plantSaplingAndGrow(null, spot, guardName);
        } else {
            Bukkit.getServer().broadcastMessage("🌲 " + Colorize.name(guardName) + " n'a trouvé aucun terrain approprié pour planter près du poste.");
        }
    }
    
    // Recréation des utilitaires terrain/plantation manquants
    private static Location findSuitablePlantingLocation(Location centerLocation) {
        List<Location> candidateLocations = new ArrayList<>();
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                if (Math.abs(x) < 5 && Math.abs(z) < 5) continue;
                Location checkLocation = centerLocation.clone().add(x, 0, z);
                if (isSuitableForPlanting(checkLocation)) {
                    candidateLocations.add(checkLocation);
                }
            }
        }
        if (candidateLocations.isEmpty()) return null;
        return candidateLocations.get(ThreadLocalRandom.current().nextInt(candidateLocations.size()));
    }

    private static boolean isSuitableForPlanting(Location location) {
        if (TestJava.world == null) return false;
        Location surfaceLocation = findSolidSurface(location);
        if (surfaceLocation == null) return false;
        Block groundBlock = TestJava.world.getBlockAt(surfaceLocation);
        Block plantingBlock = groundBlock.getRelative(0, 1, 0);
        if (!isValidGroundForTrees(groundBlock.getType())) return false;
        if (plantingBlock.getType() != Material.AIR) return false;
        for (int y = 1; y <= MIN_HEIGHT_CLEARANCE; y++) {
            Block checkBlock = groundBlock.getRelative(0, y, 0);
            if (checkBlock.getType() != Material.AIR) return false;
        }
        return !hasNearbyTrees(surfaceLocation, 8);
    }

    private static Location findSolidSurface(Location location) {
        if (TestJava.world == null) return null;
        Location checkLocation = location.clone();
        checkLocation.setY(TestJava.world.getMaxHeight() - 1);
        for (int y = TestJava.world.getMaxHeight() - 1; y >= TestJava.world.getMinHeight(); y--) {
            checkLocation.setY(y);
            Block block = TestJava.world.getBlockAt(checkLocation);
            if (block.getType().isSolid() && !block.getType().name().contains("LEAVES")) {
                return checkLocation.clone();
            }
        }
        return null;
    }

    private static boolean isValidGroundForTrees(Material material) {
        return material == Material.GRASS_BLOCK ||
               material == Material.DIRT ||
               material == Material.COARSE_DIRT ||
               material == Material.PODZOL ||
               material == Material.MYCELIUM ||
               material == Material.ROOTED_DIRT;
    }

    private static boolean hasNearbyTrees(Location location, int radius) {
        if (TestJava.world == null) return false;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location checkLocation = location.clone().add(x, y, z);
                    Material material = TestJava.world.getBlockAt(checkLocation).getType();
                    if (material.name().contains("_LOG") || material.name().contains("_WOOD")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Plante un sapling puis force la pousse en décalant le garde APRÈS la pousse.
     * Avant la pousse, applique de la "poudre d'os" simulée sur deux blocs d'herbe adjacents.
     */
    @SuppressWarnings("deprecation")
    private static void plantSaplingAndGrow(Villager villagerEntity, Location plantingLocation, String guardName) {
        if (TestJava.world == null) return;
        
        // S'assurer que le chunk est chargé
        try {
            Chunk c = plantingLocation.getChunk();
            if (!c.isLoaded()) c.load(true);
        } catch (Exception ignored) {}
        
        // Choisir un type de sapling aléatoire selon les probabilités
        Material saplingType = selectRandomSapling();
        
        // Trouver la surface solide
        Location surfaceLocation = findSolidSurface(plantingLocation);
        if (surfaceLocation == null) {
            Bukkit.getServer().broadcastMessage("🌲 " + Colorize.name(guardName) + " ne peut pas planter sur ce terrain");
            return;
        }
        
        // Charger le chunk de la surface si nécessaire
        try {
            Chunk sc = surfaceLocation.getChunk();
            if (!sc.isLoaded()) sc.load(true);
        } catch (Exception ignored) {}
        
        Block groundBlock = TestJava.world.getBlockAt(surfaceLocation);
        Block candidate = groundBlock.getRelative(0, 1, 0);
        Block finalPlantingBlock = null;
        
        // Remplacer explicitement l'air au-dessus du sol par le sapling demandé
        if (candidate.getType() == Material.AIR && isValidGroundForTrees(groundBlock.getType())) {
            candidate.setType(saplingType);
            finalPlantingBlock = candidate;
        } else {
            // Si pas d'air immédiatement, tenter de trouver la prochaine case libre verticale (sécurité)
            for (int dy = 1; dy <= 2; dy++) {
                Block alt = groundBlock.getRelative(0, dy, 0);
                if (alt.getType() == Material.AIR) {
                    alt.setType(saplingType);
                    finalPlantingBlock = alt;
                    break;
                }
            }
        }
        
        if (finalPlantingBlock == null) {
            Bukkit.getServer().broadcastMessage("🌲 " + Colorize.name(guardName) + " n'a pas trouvé d'espace libre pour planter.");
            return;
        }
        
        // NOUVEAU: Simuler l'effet de poudre d'os sur deux blocs d'herbe adjacents avant la pousse
        applyBonemealOnNearbyGrassBlocks(groundBlock, 2);

        String saplingName = saplingType.name().toLowerCase().replace("_sapling", "").replace("_", " ");
        Bukkit.getServer().broadcastMessage("🌱 " + Colorize.name(guardName) + " a planté un " + Colorize.name(saplingName) + " qui va bientôt pousser...");
        Block blockForGrowth = finalPlantingBlock;
        // Répéter l'application d'engrais jusqu'à croissance complète ou fallback
        final int[] taskIdHolder = new int[1];
        final int maxAttempts = 12;
        final int[] attempts = new int[]{0};
        taskIdHolder[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(TestJava.plugin, () -> {
            try {
                if (blockForGrowth.getType() != saplingType) {
                    // L'arbre a poussé : décaler le garde maintenant si nécessaire
                    if (villagerEntity != null && villagerEntity.isValid() && villagerEntity.getWorld() == plantingLocation.getWorld()) {
                        try {
                            double dist = villagerEntity.getLocation().distance(plantingLocation);
                            if (dist < 2.5) {
                                Location safe = findSafeOffsetSpot(plantingLocation, 3, 6);
                                if (safe != null) {
                                    try {
                                        Chunk c2 = safe.getChunk();
                                        if (!c2.isLoaded()) c2.load(true);
                                    } catch (Exception ignored) {}
                                    villagerEntity.teleport(safe);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    String treeName = saplingType.name().toLowerCase().replace("_sapling", "").replace("_", " ");
                    Bukkit.getServer().broadcastMessage("🌳✨ " + Colorize.name(guardName) + " a fait pousser un magnifique " + Colorize.name(treeName) + " par magie !");
                    Bukkit.getScheduler().cancelTask(taskIdHolder[0]);
                    return;
                }
                blockForGrowth.applyBoneMeal(org.bukkit.block.BlockFace.UP);
                attempts[0]++;
                if (attempts[0] >= maxAttempts) {
                    Bukkit.getScheduler().cancelTask(taskIdHolder[0]);
                    createSimpleTree(blockForGrowth, saplingType, guardName);
                    // Après création manuelle de l'arbre, décaler le garde
                    if (villagerEntity != null && villagerEntity.isValid() && villagerEntity.getWorld() == plantingLocation.getWorld()) {
                        try {
                            double dist = villagerEntity.getLocation().distance(plantingLocation);
                            if (dist < 2.5) {
                                Location safe = findSafeOffsetSpot(plantingLocation, 3, 6);
                                if (safe != null) {
                                    try {
                                        Chunk c2 = safe.getChunk();
                                        if (!c2.isLoaded()) c2.load(true);
                                    } catch (Exception ignored) {}
                                    villagerEntity.teleport(safe);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ex) {
                Bukkit.getScheduler().cancelTask(taskIdHolder[0]);
                createSimpleTree(blockForGrowth, saplingType, guardName);
                // Après création manuelle (suite à exception), décaler le garde
                if (villagerEntity != null && villagerEntity.isValid() && villagerEntity.getWorld() == plantingLocation.getWorld()) {
                    try {
                        double dist = villagerEntity.getLocation().distance(plantingLocation);
                        if (dist < 2.5) {
                            Location safe = findSafeOffsetSpot(plantingLocation, 3, 6);
                            if (safe != null) {
                                try {
                                    Chunk c2 = safe.getChunk();
                                    if (!c2.isLoaded()) c2.load(true);
                                } catch (Exception ignored) {}
                                villagerEntity.teleport(safe);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }, 0L, 5L);
    }
    
    /**
     * Sélectionne un type de sapling aléatoire selon les probabilités
     */
    private static Material selectRandomSapling() {
        int totalWeight = SAPLING_TYPES.values().stream().mapToInt(Integer::intValue).sum();
        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
        
        int currentWeight = 0;
        for (Map.Entry<Material, Integer> entry : SAPLING_TYPES.entrySet()) {
            currentWeight += entry.getValue();
            if (randomWeight < currentWeight) {
                return entry.getKey();
            }
        }
        
        // Fallback (ne devrait jamais arriver)
        return Material.OAK_SAPLING;
    }
    
    /**
     * Crée un petit arbre manuellement si la croissance automatique échoue
     */
    @SuppressWarnings("deprecation")
    private static void createSimpleTree(Block saplingBlock, Material saplingType, String guardName) {
        Location location = saplingBlock.getLocation();
        
        // Déterminer les matériaux de log et de feuilles
        Material logType = getLogTypeFromSapling(saplingType);
        Material leavesType = getLeavesTypeFromSapling(saplingType);
        
        // Créer un tronc de 4-6 blocs de hauteur
        int treeHeight = ThreadLocalRandom.current().nextInt(4, 7);
        
        for (int y = 0; y < treeHeight; y++) {
            Block logBlock = TestJava.world.getBlockAt(location.clone().add(0, y, 0));
            logBlock.setType(logType);
        }
        
        // Ajouter des feuilles au sommet
        Location topLocation = location.clone().add(0, treeHeight - 1, 0);
        addLeavesLayer(topLocation, leavesType, 2); // Couche supérieure
        addLeavesLayer(topLocation.clone().add(0, -1, 0), leavesType, 2); // Couche du milieu
        addLeavesLayer(topLocation.clone().add(0, -2, 0), leavesType, 1); // Couche inférieure
        
        String treeName = saplingType.name().toLowerCase().replace("_sapling", "").replace("_", " ");
        Bukkit.getServer().broadcastMessage("🌳✨ " + Colorize.name(guardName) + " a fait pousser un " + Colorize.name(treeName) + " par magie !");
    }
    
    /**
     * Ajoute une couche de feuilles
     */
    private static void addLeavesLayer(Location center, Material leavesType, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Éviter les coins pour un aspect plus naturel
                if (Math.abs(x) == radius && Math.abs(z) == radius) continue;
                
                Location leavesLocation = center.clone().add(x, 0, z);
                Block leavesBlock = TestJava.world.getBlockAt(leavesLocation);
                
                if (leavesBlock.getType() == Material.AIR) {
                    leavesBlock.setType(leavesType);
                }
            }
        }
    }
    
    /**
     * Obtient le type de log correspondant au sapling
     */
    private static Material getLogTypeFromSapling(Material saplingType) {
        return switch (saplingType) {
            case OAK_SAPLING -> Material.OAK_LOG;
            case BIRCH_SAPLING -> Material.BIRCH_LOG;
            case SPRUCE_SAPLING -> Material.SPRUCE_LOG;
            case JUNGLE_SAPLING -> Material.JUNGLE_LOG;
            case ACACIA_SAPLING -> Material.ACACIA_LOG;
            case DARK_OAK_SAPLING -> Material.DARK_OAK_LOG;
            case CHERRY_SAPLING -> Material.CHERRY_LOG;
            default -> Material.OAK_LOG;
        };
    }
    
    /**
     * Obtient le type de feuilles correspondant au sapling
     */
    private static Material getLeavesTypeFromSapling(Material saplingType) {
        return switch (saplingType) {
            case OAK_SAPLING -> Material.OAK_LEAVES;
            case BIRCH_SAPLING -> Material.BIRCH_LEAVES;
            case SPRUCE_SAPLING -> Material.SPRUCE_LEAVES;
            case JUNGLE_SAPLING -> Material.JUNGLE_LEAVES;
            case ACACIA_SAPLING -> Material.ACACIA_LEAVES;
            case DARK_OAK_SAPLING -> Material.DARK_OAK_LEAVES;
            case CHERRY_SAPLING -> Material.CHERRY_LEAVES;
            default -> Material.OAK_LEAVES;
        };
    }

    /**
     * Trouve un emplacement sûr à quelques blocs de la zone donnée
     */
    private static Location findSafeOffsetSpot(Location origin, int minRadius, int maxRadius) {
        if (origin == null || origin.getWorld() == null) return null;
        int[] radii = new int[Math.max(0, maxRadius - minRadius + 1)];
        for (int i = 0; i < radii.length; i++) radii[i] = minRadius + i;
        int[][] dirs = new int[][] { {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1} };
        for (int r : radii) {
            for (int[] d : dirs) {
                Location candidateXZ = origin.clone().add(d[0]*r, 0, d[1]*r);
                Location ground = findSolidSurface(candidateXZ);
                if (ground == null) continue;
                Block base = TestJava.world.getBlockAt(ground);
                Block head = base.getRelative(0, 1, 0);
                Block head2 = base.getRelative(0, 2, 0);
                if (head.getType() == Material.AIR && head2.getType() == Material.AIR) {
                    return ground.clone().add(0.5, 1.0, 0.5);
                }
            }
        }
        return null;
    }

    /**
     * Applique de la poudre d'os sur jusqu'à "count" blocs d'herbe adjacents (cardinaux) au pied du sapling.
     */
    private static void applyBonemealOnNearbyGrassBlocks(Block groundBlockUnderSapling, int count) {
        if (groundBlockUnderSapling == null) return;
        List<Block> candidates = new ArrayList<>();
        int[][] dirs = new int[][] { {1,0}, {-1,0}, {0,1}, {0,-1} };
        for (int[] d : dirs) {
            Block neighbor = groundBlockUnderSapling.getRelative(d[0], 0, d[1]);
            if (neighbor.getType() == Material.GRASS_BLOCK) {
                Block above = neighbor.getRelative(0, 1, 0);
                if (above.getType() == Material.AIR) {
                    candidates.add(neighbor);
                }
            }
        }
        // Mélanger pour variété, puis appliquer à count blocs
        Collections.shuffle(candidates, ThreadLocalRandom.current());
        int applied = 0;
        for (Block b : candidates) {
            try {
                b.applyBoneMeal(org.bukkit.block.BlockFace.UP);
                applied++;
                if (applied >= count) break;
            } catch (Exception ignored) {}
        }
    }
}
