package TestJava.testjava.services;

import TestJava.testjava.Config;
import TestJava.testjava.TestJava;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Pillager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service pour gérer la logique du métier natif "Cartographe"
 * Les cartographes alertent le propriétaire du village lors d'intrusions de mercenaires ou joueurs adverses
 */
public class CartographeService {
    
    // Cache des positions précédentes pour détecter les entrées/sorties
    private static final Map<UUID, Location> previousPlayerLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, Location> previousMercenaryLocations = new ConcurrentHashMap<>();
    
    // Délai minimum entre les alertes pour éviter le spam (en millisecondes)
    private static final long ALERT_COOLDOWN = 10000; // 10 secondes
    private static final Map<String, Long> lastAlertTimes = new ConcurrentHashMap<>();
    
    /**
     * Vérifie si un village a des cartographes actifs
     */
    public static boolean hasActiveCartographer(VillageModel village) {
        if (village == null) return false;
        
        try {
            // Utiliser le repository standard et filtrer par village
            Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
            
            for (VillagerModel villager : allVillagers) {
                if (village.getId().equals(villager.getVillageName()) && villager.hasNativeJob()) {
                    // Vérifier si l'entité Minecraft existe et est un cartographe
                    if (TestJava.world != null) {
                        for (org.bukkit.entity.Entity entity : TestJava.world.getEntities()) {
                            if (entity instanceof Villager bukkit_villager && 
                                entity.getUniqueId().equals(villager.getId()) &&
                                bukkit_villager.getProfession() == Villager.Profession.CARTOGRAPHER) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CartographeService] Erreur lors de la vérification des cartographes: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Traite le mouvement d'un joueur et détecte les intrusions
     */
    public static void handlePlayerMovement(Player player) {
        if (player == null) return;
        
        Location currentLocation = player.getLocation();
        Location previousLocation = previousPlayerLocations.get(player.getUniqueId());
        
        // Mettre à jour la position actuelle
        previousPlayerLocations.put(player.getUniqueId(), currentLocation.clone());
        
        if (previousLocation == null) {
            return; // Premier mouvement enregistré
        }
        
        // Vérifier les transitions entre villages
        checkVillageTransition(player, previousLocation, currentLocation, false);
    }
    
    /**
     * Traite le mouvement d'un mercenaire et détecte les intrusions
     */
    public static void handleMercenaryMovement(org.bukkit.entity.Entity mercenary) {
        if (mercenary == null || !isMercenary(mercenary)) return;
        
        Location currentLocation = mercenary.getLocation();
        Location previousLocation = previousMercenaryLocations.get(mercenary.getUniqueId());
        
        // Mettre à jour la position actuelle
        previousMercenaryLocations.put(mercenary.getUniqueId(), currentLocation.clone());
        
        if (previousLocation == null) {
            return; // Premier mouvement enregistré
        }
        
        // Vérifier les transitions entre villages
        checkVillageTransition(mercenary, previousLocation, currentLocation, true);
    }
    
    /**
     * Vérifie si une entité est un mercenaire
     */
    private static boolean isMercenary(org.bukkit.entity.Entity entity) {
        if (!entity.isCustomNameVisible()) {
            return false;
        }
        
        // Utiliser customName() pour éviter l'avertissement de dépréciation
        String customName = null;
        if (entity.customName() != null) {
            customName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(entity.customName());
        }
        
        if (customName == null) {
            return false;
        }
        
        return (entity instanceof Zombie && customName.contains("Mercenaire")) ||
               (entity instanceof Skeleton && customName.contains("[") && customName.contains("]")) ||
               (entity instanceof Pillager && customName.contains("[") && customName.contains("]"));
    }
    
    /**
     * Vérifie les transitions entre villages et déclenche les alertes
     */
    private static void checkVillageTransition(Object entity, Location previousLocation, Location currentLocation, boolean isMercenary) {
        VillageModel previousVillage = getVillageAtLocation(previousLocation);
        VillageModel currentVillage = getVillageAtLocation(currentLocation);
        
        // Pas de changement de zone
        if (Objects.equals(previousVillage, currentVillage)) {
            return;
        }
        
        String entityName;
        String entityType;
        
        if (entity instanceof Player player) {
            entityName = player.getName();
            entityType = "Joueur";
        } else if (entity instanceof org.bukkit.entity.Entity bukkit_entity) {
            entityName = extractEntityName(bukkit_entity);
            entityType = "Mercenaire";
        } else {
            return;
        }
        
        // Entrée dans un village
        if (previousVillage == null && currentVillage != null) {
            handleVillageEntry(currentVillage, entityName, entityType, entity);
        }
        // Sortie d'un village
        else if (previousVillage != null && currentVillage == null) {
            handleVillageExit(previousVillage, entityName, entityType, entity);
        }
        // Passage direct d'un village à un autre
        else if (previousVillage != null && currentVillage != null && !previousVillage.getId().equals(currentVillage.getId())) {
            handleVillageExit(previousVillage, entityName, entityType, entity);
            handleVillageEntry(currentVillage, entityName, entityType, entity);
        }
    }
    
    /**
     * Gère l'entrée dans un village
     */
    private static void handleVillageEntry(VillageModel village, String entityName, String entityType, Object entity) {
        // Vérifier si le village a des cartographes
        if (!hasActiveCartographer(village)) {
            return;
        }
        
        // Vérifier si c'est un adversaire
        if (!isAdversary(village, entity)) {
            return;
        }
        
        // Vérifier le cooldown des alertes
        String alertKey = village.getId() + ":" + entityName + ":entry";
        if (!canSendAlert(alertKey)) {
            return;
        }
        
        // Envoyer l'alerte
        sendIntrusionAlert(village, entityName, entityType, true);
        updateAlertCooldown(alertKey);
    }
    
    /**
     * Gère la sortie d'un village
     */
    private static void handleVillageExit(VillageModel village, String entityName, String entityType, Object entity) {
        // Vérifier si le village a des cartographes
        if (!hasActiveCartographer(village)) {
            return;
        }
        
        // Vérifier si c'est un adversaire
        if (!isAdversary(village, entity)) {
            return;
        }
        
        // Vérifier le cooldown des alertes
        String alertKey = village.getId() + ":" + entityName + ":exit";
        if (!canSendAlert(alertKey)) {
            return;
        }
        
        // Envoyer l'alerte
        sendIntrusionAlert(village, entityName, entityType, false);
        updateAlertCooldown(alertKey);
    }
    
    /**
     * Vérifie si une entité est un adversaire pour le village
     */
    private static boolean isAdversary(VillageModel village, Object entity) {
        if (entity instanceof Player player) {
            // Un joueur est adversaire s'il n'est pas le propriétaire du village
            return !village.getPlayerName().equals(player.getName());
        } else {
            // Les mercenaires sont toujours considérés comme adversaires
            return true;
        }
    }
    
    /**
     * Envoie une alerte d'intrusion au propriétaire du village
     */
    private static void sendIntrusionAlert(VillageModel village, String entityName, String entityType, boolean isEntry) {
        Player owner = Bukkit.getPlayer(village.getPlayerName());
        if (owner == null || !owner.isOnline()) {
            return; // Propriétaire pas connecté
        }
        
        String action = isEntry ? "est entré dans" : "a quitté";
        String emoji = isEntry ? "🚨" : "✅";
        
        // Utiliser un format simple sans codes de couleur compliqués
        String message = emoji + " ALERTE CARTOGRAPHE\n" +
                        entityType + " " + entityName + 
                        " " + action + " les frontières de " + village.getId() + " !";
        
        owner.sendMessage(message);
        
        // Log pour debug
        Bukkit.getLogger().info("[CartographeService] Alerte envoyée à " + owner.getName() + 
                               ": " + entityType + " " + entityName + " " + action + " " + village.getId());
    }
    
    /**
     * Obtient le village à une location donnée
     */
    private static VillageModel getVillageAtLocation(Location location) {
        try {
            Collection<VillageModel> allVillages = VillageRepository.getAll();
            
            for (VillageModel village : allVillages) {
                Location villageCenter = new Location(location.getWorld(), village.getX(), village.getY(), village.getZ());
                double distance = location.distance(villageCenter);
                
                if (distance <= Config.VILLAGE_PROTECTION_RADIUS) {
                    return village;
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CartographeService] Erreur lors de la recherche de village: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extrait le nom d'une entité mercenaire
     */
    private static String extractEntityName(org.bukkit.entity.Entity entity) {
        if (entity.customName() != null) {
            String customName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(entity.customName());
            // Format: [Village] Nom ou [Village] Type
            int bracketEnd = customName.indexOf(']');
            if (bracketEnd != -1 && bracketEnd + 2 < customName.length()) {
                return customName.substring(bracketEnd + 2).trim();
            }
            return customName;
        }
        return "Entité inconnue";
    }
    
    /**
     * Vérifie si une alerte peut être envoyée (respecte le cooldown)
     */
    private static boolean canSendAlert(String alertKey) {
        Long lastAlert = lastAlertTimes.get(alertKey);
        if (lastAlert == null) {
            return true;
        }
        
        return (System.currentTimeMillis() - lastAlert) >= ALERT_COOLDOWN;
    }
    
    /**
     * Met à jour le temps de la dernière alerte
     */
    private static void updateAlertCooldown(String alertKey) {
        lastAlertTimes.put(alertKey, System.currentTimeMillis());
    }
    
    /**
     * Nettoie les données de mouvement obsolètes
     */
    public static void cleanupOldMovementData() {
        try {
            // Nettoyer les joueurs déconnectés
            Set<UUID> playersToRemove = new HashSet<>();
            for (UUID playerId : previousPlayerLocations.keySet()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline()) {
                    playersToRemove.add(playerId);
                }
            }
            playersToRemove.forEach(previousPlayerLocations::remove);
            
            // Nettoyer les mercenaires morts ou disparus
            Set<UUID> mercenariesToRemove = new HashSet<>();
            if (TestJava.world != null) {
                Set<UUID> activeMercenaries = new HashSet<>();
                for (org.bukkit.entity.Entity entity : TestJava.world.getEntities()) {
                    if (isMercenary(entity)) {
                        activeMercenaries.add(entity.getUniqueId());
                    }
                }
                
                for (UUID mercenaryId : previousMercenaryLocations.keySet()) {
                    if (!activeMercenaries.contains(mercenaryId)) {
                        mercenariesToRemove.add(mercenaryId);
                    }
                }
            }
            mercenariesToRemove.forEach(previousMercenaryLocations::remove);
            
            // Nettoyer les alertes anciennes
            long cutoffTime = System.currentTimeMillis() - (ALERT_COOLDOWN * 2);
            lastAlertTimes.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CartographeService] Erreur lors du nettoyage: " + e.getMessage());
        }
    }
    
    /**
     * Obtient les statistiques du service pour debug
     */
    public static String getDebugStats() {
        return String.format("CartographeService - Joueurs trackés: %d, Mercenaires trackés: %d, Alertes actives: %d",
            previousPlayerLocations.size(),
            previousMercenaryLocations.size(),
            lastAlertTimes.size());
    }
}
