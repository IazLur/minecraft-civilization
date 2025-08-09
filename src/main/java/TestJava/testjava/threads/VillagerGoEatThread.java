package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.EatableModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.EatableRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.services.SocialClassService;
import TestJava.testjava.services.VillagerInventoryService;
import TestJava.testjava.services.VillagerInventoryService.FeedResult;
import TestJava.testjava.services.HistoryService;
import TestJava.testjava.services.VillagerMovementManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.*;

public class VillagerGoEatThread implements Runnable {

    private static final int MIN_DELAY = 20;
    private static final int RANGE_DELAY = 20 * 10;
    private static final int MAX_FOOD = 19;
    private static final int FULL_FOOD = 20;
    private static final double MOVE_SPEED = 1D;

    private final Set<UUID> targetedEatables = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void run() {
        Collection<EatableModel> eatables = EatableRepository.getAll();
        HashMap<String, Collection<EatableModel>> villageEatablesMap = prepareEatablesMap(eatables);

        // Récupérer TOUS les villageois pour un comptage complet
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        
        // Compteurs par village
        Map<String, VillageStats> villageStatsMap = new HashMap<>();
        
        // Initialiser les statistiques pour tous les villages
        for (VillagerModel villager : allVillagers) {
            villageStatsMap.putIfAbsent(villager.getVillageName(), new VillageStats());
        }
        
        // Traiter chaque villageois individuellement
        for (VillagerModel villager : allVillagers) {
            VillageStats stats = villageStatsMap.get(villager.getVillageName());
            
            // Catégoriser le villageois selon sa nourriture
            if (villager.getFood() >= FULL_FOOD) {
                // Villageois rassasiés (nourriture >= 20) - ne consomment que des points
                stats.rassasies++;
            } else if (villager.getFood() < MAX_FOOD) {
                // Villageois affamés (nourriture < 19) - besoin de se nourrir
                Bukkit.getScheduler().runTask(TestJava.plugin, () -> 
                    handleHungryVillager(villager, villageEatablesMap, stats)
                );
            } else {
                // Villageois avec nourriture entre 19 et 20 - pas besoin de se nourrir mais pas rassasiés
                stats.stables++;
            }
        }
        
        // Attendre que toutes les tâches se terminent et afficher les résultats
        Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
            displayDistributionResults(villageStatsMap);
            
            // Log de résumé global
            int totalVillagers = allVillagers.size();
            int totalVillages = villageStatsMap.size();
            int totalRassasies = villageStatsMap.values().stream().mapToInt(s -> s.rassasies).sum();
            int totalAutosuffisants = villageStatsMap.values().stream().mapToInt(s -> s.autosuffisants).sum();
            int totalClients = villageStatsMap.values().stream().mapToInt(s -> s.clients).sum();
            int totalVoleurs = villageStatsMap.values().stream().mapToInt(s -> s.voleurs).sum();
            int totalAffames = villageStatsMap.values().stream().mapToInt(s -> s.affames).sum();
            int totalStables = villageStatsMap.values().stream().mapToInt(s -> s.stables).sum();
            
            Bukkit.getLogger().info("[VillagerGoEat] ✅ Résumé global: " + totalVillagers + " villageois traités dans " + totalVillages + " villages");
            Bukkit.getLogger().info("[VillagerGoEat] 📊 Répartition: " + totalRassasies + " rassasiés, " + totalAutosuffisants + " autosuffisants, " + 
                                   totalClients + " clients, " + totalVoleurs + " voleurs, " + totalAffames + " affamés, " + totalStables + " stables");
        }, 100L);
    }

    private HashMap<String, Collection<EatableModel>> prepareEatablesMap(Collection<EatableModel> eatables) {
        HashMap<String, Collection<EatableModel>> map = new HashMap<>();
        for (EatableModel eatable : eatables) {
            map.computeIfAbsent(eatable.getVillage(), k -> new ArrayList<>()).add(eatable);
        }
        return map;
    }

    private void handleHungryVillager(VillagerModel villager, HashMap<String, Collection<EatableModel>> villageEatablesMap, VillageStats stats) {
        try {
            Villager eVillager = fetchEntityVillager(villager.getId(), villager);

            if(eVillager == null) {
                // Villageois fantôme détecté - nettoyage automatique
                handleGhostVillager(villager);
                // Ne pas compter les villageois fantômes dans les statistiques
                return;
            }

            // NOUVELLE LOGIQUE : Priorité à l'inventaire et aux achats avant d'aller aux champs
            FeedResult feedResult = VillagerInventoryService.attemptToFeedVillager(villager);
            if (feedResult == FeedResult.SELF_FED) {
                stats.autosuffisants++;
                return;
            } else if (feedResult == FeedResult.BOUGHT_FOOD) {
                stats.clients++;
                return;
            }

            // LOGIQUE ORIGINALE : En dernier recours, aller manger dans les champs
            EatableModel targetEatable = findEatable(villager, villageEatablesMap);

            if (targetEatable != null) {
                targetedEatables.add(targetEatable.getId());
                stats.voleurs++;
                moveVillagerToFood(eVillager, villager, targetEatable, villageEatablesMap);
            } else {
                stats.affames++;
            }
            
        } catch (Exception e) {
            // En cas d'erreur, compter comme affamé par défaut
            stats.affames++;
            Bukkit.getLogger().warning("[VillagerGoEat] Erreur traitement villageois " + villager.getId() + ": " + e.getMessage());
        }
    }

    private Villager fetchEntityVillager(UUID uuid, VillagerModel villager) {
        Entity entity = TestJava.plugin.getServer().getEntity(uuid);

        if (entity == null) {
            Location location = VillageRepository.getBellLocation(VillageRepository.get(villager.getVillageName()));
            World world = location.getWorld();

            // Récupère les coordonnées du chunk central
            int chunkX = location.getChunk().getX();
            int chunkZ = location.getChunk().getZ();

            // Boucle pour charger les chunks dans un carré de 3x3
            for (int x = chunkX - 1; x <= chunkX + 1; x++) {
                for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
                    Chunk chunk = world.getChunkAt(x, z);

                    if (!chunk.isLoaded()) {
                        boolean success = chunk.load(true);

                        if (success) {
                            entity = TestJava.plugin.getServer().getEntity(uuid);
                            if (entity != null) {
                                break;
                            }
                        } else {
                            Bukkit.getServer().getLogger().warning("Impossible de charger le chunk en [" + x + ", " + z + "].");
                        }
                    }
                }
                if (entity != null) {
                    break;
                }
            }
        }

        return (entity instanceof Villager) ? (Villager) entity : null;
    }

    private boolean eatableExistsInWorld(EatableModel eatable) {
        Location loc = new Location(TestJava.world, eatable.getX(), eatable.getY(), eatable.getZ());
        Block block = loc.getBlock();
        boolean exists = (block.getBlockData() instanceof Ageable age) && (age.getMaximumAge() == age.getAge());

        if (block.getBlockData().getMaterial() != Material.WHEAT) {
            EatableRepository.remove(eatable);
            return false;
        }

        return exists;
    }

    private EatableModel findEatable(VillagerModel villager, HashMap<String, Collection<EatableModel>> villageEatablesMap) {
        Villager eVillager = fetchEntityVillager(villager.getId(), villager);
        if (eVillager == null) {
            // Villageois fantôme - sera géré par handleHungryVillager
            return null;
        }

        Location villagerLocation = eVillager.getLocation();
        Collection<EatableModel> eatables = villageEatablesMap.getOrDefault(villager.getVillageName(), Collections.emptyList());

        return eatables.stream().filter(this::eatableExistsInWorld).filter(eatable -> !targetedEatables.contains(eatable.getId())).min(Comparator.comparingDouble(eatable -> calculateDistance(villagerLocation, eatable))).orElse(null);
    }

    private double calculateDistance(Location villagerLocation, EatableModel eatable) {
        Location eatableLocation = new Location(TestJava.world, eatable.getX(), eatable.getY(), eatable.getZ());
        return villagerLocation.distance(eatableLocation);
    }

    private void moveVillagerToFood(Villager eVillager, VillagerModel villager, EatableModel targetEatable, HashMap<String, Collection<EatableModel>> villageEatablesMap) {
        Random rand = new Random();
        int delay = MIN_DELAY + rand.nextInt(RANGE_DELAY);
        
        Location targetLocation = new Location(TestJava.world, targetEatable.getX(), targetEatable.getY(), targetEatable.getZ());
        
        // Attendre un délai aléatoire avant de commencer le déplacement
        Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
            
            // Vérifications de sécurité avant de commencer
            if (eVillager == null || eVillager.isDead() || !eVillager.isValid()) {
                targetedEatables.remove(targetEatable.getId());
                return;
            }
            
            // Vérifier si la nourriture existe encore
            Block block = targetLocation.getBlock();
            if (isFoodGone(block)) {
                handleFoodGone(targetEatable, villager, villageEatablesMap);
                return;
            }
            
            // Utiliser le gestionnaire centralisé pour le déplacement
            VillagerMovementManager.moveVillager(eVillager, targetLocation)
                .withSuccessDistance(2.0) // Distance de base, peut augmenter si nécessaire
                .withMoveSpeed(MOVE_SPEED)
                .withTimeout(30) // 30 secondes maximum
                .withName("VillagerEating_" + villager.getId())
                .onSuccess(() -> {
                    // Manger la nourriture à l'arrivée
                    Block currentBlock = targetLocation.getBlock();
                    if (!isFoodGone(currentBlock)) {
                        handleEating(villager, currentBlock, targetEatable);
                    } else {
                        handleFoodGone(targetEatable, villager, villageEatablesMap);
                    }
                })
                .onFailure(() -> {
                    // Échec du déplacement, libérer la nourriture
                    targetedEatables.remove(targetEatable.getId());
                })
                .onPositionUpdate((distance, attempts) -> {
                    // Vérifier à chaque tentative si la nourriture existe encore
                    Block currentBlock = targetLocation.getBlock();
                    if (isFoodGone(currentBlock)) {
                        // La nourriture a disparu, on va laisser le callback onFailure gérer
                        handleFoodGone(targetEatable, villager, villageEatablesMap);
                    }
                })
                .start();
                
        }, delay);
    }

    private boolean isFoodGone(Block block) {
        return !(block.getBlockData() instanceof Ageable age) || age.getAge() != age.getMaximumAge();
    }

    private void handleFoodGone(EatableModel targetEatable, VillagerModel villager, HashMap<String, Collection<EatableModel>> villageEatablesMap) {
        targetedEatables.remove(targetEatable.getId());
        // Note: On ne relance pas handleHungryVillager ici pour éviter les boucles infinies
        // et parce que le villageois sera traité au prochain cycle
    }

    private void handleEating(VillagerModel villager, Block block, EatableModel targetEatable) {
        Ageable age = (Ageable) block.getBlockData();
        age.setAge(1);
        block.setBlockData(age);

        villager.setFood(villager.getFood() + 1);
        VillagerRepository.update(villager);

        // Enregistrer la consommation dans l'historique (vol de blé)
        HistoryService.recordFoodConsumption(villager, "blé");

        // Évaluation de la classe sociale après l'alimentation
        SocialClassService.evaluateAndUpdateSocialClass(villager);

        VillageModel village = VillageRepository.get(villager.getVillageName());
        if (villager.getFood() >= MAX_FOOD) {
            village.setProsperityPoints(village.getProsperityPoints() + 1);
        }

        targetedEatables.remove(targetEatable.getId());

        VillageRepository.update(village);
        EatableRepository.remove(targetEatable);
    }

    /**
     * Classe pour stocker les statistiques de distribution par village
     */
    private static class VillageStats {
        public int autosuffisants = 0;
        public int clients = 0;
        public int voleurs = 0;
        public int affames = 0;
        public int rassasies = 0; // NOUVEAU: villageois avec 20+ nourriture
        public int stables = 0; // NOUVEAU: villageois avec nourriture entre 19 et 20
    }
    
    /**
     * Affiche les résultats de distribution de nourriture par village
     */
    private void displayDistributionResults(Map<String, VillageStats> villageStatsMap) {
        for (Map.Entry<String, VillageStats> entry : villageStatsMap.entrySet()) {
            String villageName = entry.getKey();
            VillageStats stats = entry.getValue();
            
            // CORRECTION BUG: Envoyer le message seulement au propriétaire du village
            sendDistributionMessageToVillageOwner(villageName, stats);
        }
    }
    
    /**
     * Envoie le message de distribution seulement au propriétaire du village
     */
    private void sendDistributionMessageToVillageOwner(String villageName, VillageStats stats) {
        try {
            VillageModel village = VillageRepository.get(villageName);
            if (village == null) {
                return;
            }
            
            // Trouver le joueur propriétaire
            org.bukkit.entity.Player owner = Bukkit.getPlayerExact(village.getPlayerName());
            if (owner == null || !owner.isOnline()) {
                return; // Propriétaire pas connecté, pas de message
            }
            
            // Calculer le total des villageois traités
            int totalProcessed = stats.rassasies + stats.autosuffisants + stats.clients + stats.voleurs + stats.affames + stats.stables;
            int villagePopulation = village.getPopulation();
            
            // Envoyer le message de distribution seulement au propriétaire
            owner.sendMessage(org.bukkit.ChatColor.GREEN + "Distribution de nourriture à " + villageName);
            owner.sendMessage("Villageois suffisants: " + Colorize.name((stats.voleurs + stats.rassasies + stats.stables + stats.autosuffisants + stats.clients) + " villageois"));
            owner.sendMessage("Villageois affamés: " + Colorize.name(stats.affames + " villageois"));
            owner.sendMessage(Colorize.name("Il y a eu " + stats.voleurs + " vols de blé."));
            owner.sendMessage(Colorize.name("Il y a eu " + stats.clients + " achats de nourriture."));
            
            // Validation du total
            if (totalProcessed != villagePopulation) {
                owner.sendMessage(org.bukkit.ChatColor.YELLOW + "⚠️ Attention: " + totalProcessed + " villageois traités sur " + villagePopulation + " (différence: " + (villagePopulation - totalProcessed) + ")");
                Bukkit.getLogger().warning("[VillagerGoEat] Incohérence détectée pour " + villageName + ": " + totalProcessed + " traités sur " + villagePopulation + " villageois");
            } else {
                owner.sendMessage(org.bukkit.ChatColor.GREEN + "✅ Total: " + totalProcessed + "/" + villagePopulation + " villageois traités");
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[VillagerGoEat] Erreur envoi message: " + e.getMessage());
        }
    }

    /**
     * Gère les villageois fantômes (en DB mais pas dans le monde)
     */
    private void handleGhostVillager(VillagerModel villager) {
        try {
            // Supprime le villageois de la base de données
            VillagerRepository.remove(villager.getId());
            
            // Met à jour la population du village
            VillageModel village = VillageRepository.get(villager.getVillageName());
            if (village != null && village.getPopulation() > 0) {
                village.setPopulation(village.getPopulation() - 1);
                VillageRepository.update(village);
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[GhostVillager] Erreur lors du nettoyage de " + 
                                     villager.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}