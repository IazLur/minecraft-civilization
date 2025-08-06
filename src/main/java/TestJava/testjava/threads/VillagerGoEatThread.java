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

        String queryHungry = String.format("/.[food<'%s']", MAX_FOOD);
        Collection<VillagerModel> hungryVillagers = TestJava.database.find(queryHungry, VillagerModel.class);
        
        String queryFull = String.format("/.[food>='%s']", FULL_FOOD);
        Collection<VillagerModel> fullVillagers = TestJava.database.find(queryFull, VillagerModel.class);

        // Compteurs par village
        Map<String, VillageStats> villageStatsMap = new HashMap<>();
        
        // Compter les villageois rassasiés (nourriture >= 20) qui ne consomment que des points de nourriture
        for (VillagerModel villager : fullVillagers) {
            villageStatsMap.putIfAbsent(villager.getVillageName(), new VillageStats());
            villageStatsMap.get(villager.getVillageName()).rassasies++;
        }
        
        for (VillagerModel villager : hungryVillagers) {
            Bukkit.getLogger().info("Testing food for " + villager.getId());
            
            // Initialiser les statistiques du village si nécessaire
            villageStatsMap.putIfAbsent(villager.getVillageName(), new VillageStats());
            
            Bukkit.getScheduler().runTask(TestJava.plugin, () -> 
                handleHungryVillager(villager, villageEatablesMap, villageStatsMap.get(villager.getVillageName()))
            );
        }
        
        // Attendre que toutes les tâches se terminent et afficher les résultats
        Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> displayDistributionResults(villageStatsMap), 100L);
    }

    private HashMap<String, Collection<EatableModel>> prepareEatablesMap(Collection<EatableModel> eatables) {
        HashMap<String, Collection<EatableModel>> map = new HashMap<>();
        for (EatableModel eatable : eatables) {
            map.computeIfAbsent(eatable.getVillage(), k -> new ArrayList<>()).add(eatable);
        }
        return map;
    }

    private void handleHungryVillager(VillagerModel villager, HashMap<String, Collection<EatableModel>> villageEatablesMap, VillageStats stats) {
        Villager eVillager = fetchEntityVillager(villager.getId(), villager);

        if(eVillager == null) {
            // Villageois fantôme détecté - nettoyage automatique
            handleGhostVillager(villager);
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
        UUID uuid = UUID.randomUUID();

        Location loc = new Location(TestJava.world, targetEatable.getX(), targetEatable.getY(), targetEatable.getZ());
        Block block = loc.getBlock();

        final int[] attempts = {0};
        final double[] increasedDistance = {2.0};

        TestJava.threads.put(uuid, Bukkit.getScheduler().scheduleSyncRepeatingTask(TestJava.plugin, () -> {
            attempts[0] += 1;

            if (attempts[0] % 3 == 0) {
                increasedDistance[0] += 1.0;
            }

            performScheduledTask(eVillager, villager, targetEatable, block, loc, uuid, increasedDistance[0], villageEatablesMap);
        }, delay, 10));
    }

    private void performScheduledTask(Villager eVillager, VillagerModel villager, EatableModel targetEatable, Block block, Location loc, UUID uuid, double increasedDistance, HashMap<String, Collection<EatableModel>> villageEatablesMap) {
        if (eVillager.isSleeping()) {
            eVillager.wakeup();
        }

        eVillager.getPathfinder().moveTo(loc, MOVE_SPEED);

        if (isFoodGone(block)) {
            handleFoodGone(targetEatable, uuid, villager, villageEatablesMap);
            return;
        }

        if (eVillager.getLocation().distance(loc) <= increasedDistance) {
            handleEating(villager, block, targetEatable, uuid);
        }
    }

    private boolean isFoodGone(Block block) {
        return !(block.getBlockData() instanceof Ageable age) || age.getAge() != age.getMaximumAge();
    }

    private void handleFoodGone(EatableModel targetEatable, UUID uuid, VillagerModel villager, HashMap<String, Collection<EatableModel>> villageEatablesMap) {
        targetedEatables.remove(targetEatable.getId());
        // Note: On ne relance pas handleHungryVillager ici pour éviter les boucles infinies
        // et parce que le villageois sera traité au prochain cycle
        cancelTask(uuid);
    }

    private void handleEating(VillagerModel villager, Block block, EatableModel targetEatable, UUID uuid) {
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
        cancelTask(uuid);
    }

    private void cancelTask(UUID uuid) {
        Bukkit.getScheduler().cancelTask(TestJava.threads.get(uuid));
        TestJava.threads.remove(uuid);
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
            
            // Envoyer le message de distribution seulement au propriétaire
            owner.sendMessage("Distribution de nourriture à " + Colorize.name(villageName));
            owner.sendMessage("Villageois rassasiés: " + Colorize.name(stats.rassasies + " villageois"));
            owner.sendMessage("Villageois autosuffisants: " + Colorize.name(stats.autosuffisants + " villageois"));
            owner.sendMessage("Villageois clients: " + Colorize.name(stats.clients + " villageois"));
            owner.sendMessage("Villageois voleurs: " + Colorize.name(stats.voleurs + " villageois"));
            owner.sendMessage("Villageois affamés: " + Colorize.name(stats.affames + " villageois"));
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[VillagerGoEat] Erreur envoi message: " + e.getMessage());
        }
    }

    /**
     * Gère les villageois fantômes (en DB mais pas dans le monde)
     */
    private void handleGhostVillager(VillagerModel villager) {
        try {
            // Log détaillé pour diagnostic
            Bukkit.getLogger().warning("[GhostVillager] Villageois fantôme détecté: " + 
                                     villager.getId() + " dans village " + villager.getVillageName());
            
            // Supprime le villageois de la base de données
            VillagerRepository.remove(villager.getId());
            
            // Met à jour la population du village
            VillageModel village = VillageRepository.get(villager.getVillageName());
            if (village != null && village.getPopulation() > 0) {
                village.setPopulation(village.getPopulation() - 1);
                VillageRepository.update(village);
                
                Bukkit.getLogger().info("[GhostVillager] Population de " + village.getId() + 
                                       " mise à jour: " + village.getPopulation());
            }
            
            // Broadcast informatif
            Bukkit.getServer().broadcastMessage(
                ChatColor.GRAY + "🧹 Nettoyage automatique: villageois fantôme supprimé de " + 
                Colorize.name(villager.getVillageName())
            );
            
            Bukkit.getLogger().info("[GhostVillager] ✅ Villageois fantôme " + villager.getId() + 
                                   " supprimé avec succès");
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[GhostVillager] Erreur lors du nettoyage de " + 
                                     villager.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}