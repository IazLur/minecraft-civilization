package TestJava.testjava.services;

import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour corriger les populations de villages qui dépassent leur limite de lits
 */
public class VillagePopulationCorrectionService {
    
    /**
     * Corrige automatiquement tous les villages qui ont trop de villageois
     * @return Nombre de villages corrigés
     */
    public static int correctAllVillagePopulations() {
        Bukkit.getLogger().info("[PopulationCorrection] ===============================================");
        Bukkit.getLogger().info("[PopulationCorrection] Démarrage de la correction des populations...");
        
        int correctedVillages = 0;
        
        try {
            Collection<VillageModel> allVillages = VillageRepository.getAll();
            
            for (VillageModel village : allVillages) {
                if (correctVillagePopulation(village)) {
                    correctedVillages++;
                }
            }
            
            Bukkit.getLogger().info("[PopulationCorrection] ✅ Correction terminée: " + correctedVillages + " villages corrigés");
            Bukkit.getLogger().info("[PopulationCorrection] ===============================================");
            
            if (correctedVillages > 0) {
                // Broadcast informatif
                Bukkit.getServer().broadcastMessage(
                    ChatColor.YELLOW + "🔧 Correction automatique: " + ChatColor.AQUA + correctedVillages + 
                    ChatColor.YELLOW + " villages ont été corrigés (population > lits)"
                );
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[PopulationCorrection] ❌ Erreur lors de la correction: " + e.getMessage());
            e.printStackTrace();
        }
        
        return correctedVillages;
    }
    
    /**
     * Corrige la population d'un village spécifique
     * @param village Le village à corriger
     * @return true si le village a été corrigé
     */
    public static boolean correctVillagePopulation(VillageModel village) {
        try {
            // Vérifie si le village a des lits
            if (village.getBedsCount() == null || village.getBedsCount() <= 0) {
                Bukkit.getLogger().warning("[PopulationCorrection] Village " + village.getId() + " n'a pas de lits");
                return false;
            }
            
            // Vérifie si la population dépasse la limite
            if (village.getPopulation() <= village.getBedsCount()) {
                return false; // Pas de correction nécessaire
            }
            
            int excessVillagers = village.getPopulation() - village.getBedsCount();
            Bukkit.getLogger().warning("[PopulationCorrection] Village " + village.getId() + 
                " a " + village.getPopulation() + " villageois pour " + village.getBedsCount() + 
                " lits (excès: " + excessVillagers + ")");
            
            // Récupère tous les villageois du village
            Collection<VillagerModel> villageVillagers = VillagerRepository.getAll().stream()
                .filter(v -> village.getId().equals(v.getVillageName()))
                .collect(Collectors.toList());
            
            if (villageVillagers.size() < excessVillagers) {
                Bukkit.getLogger().warning("[PopulationCorrection] Villageois en base insuffisants pour " + village.getId());
                return false;
            }
            
            // Supprime les villageois en excès (les plus récents d'abord)
            List<VillagerModel> villagersToRemove = villageVillagers.stream()
                .sorted((v1, v2) -> v2.getId().compareTo(v1.getId())) // Plus récents en premier
                .limit(excessVillagers)
                .collect(Collectors.toList());
            
            // Supprime les villageois du monde et de la base
            for (VillagerModel villagerToRemove : villagersToRemove) {
                removeVillagerFromWorld(villagerToRemove);
                VillagerRepository.remove(villagerToRemove.getId());
                
                Bukkit.getLogger().info("[PopulationCorrection] Villageois supprimé: " + 
                    villagerToRemove.getId() + " de " + village.getId());
            }
            
            // Met à jour la population du village
            village.setPopulation(village.getBedsCount());
            VillageRepository.update(village);
            
            Bukkit.getLogger().info("[PopulationCorrection] ✅ Village " + village.getId() + 
                " corrigé: " + village.getBedsCount() + " villageois pour " + village.getBedsCount() + " lits");
            
            return true;
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[PopulationCorrection] ❌ Erreur correction " + village.getId() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Supprime un villageois du monde Minecraft
     */
    private static void removeVillagerFromWorld(VillagerModel villagerModel) {
        try {
            for (Entity entity : Bukkit.getWorlds().get(0).getEntities()) {
                if (entity instanceof Villager villager && 
                    villager.getUniqueId().equals(villagerModel.getId())) {
                    
                    // Message de mort pour le broadcast
                    String customName = villager.getCustomName();
                    if (customName != null) {
                        Bukkit.getServer().broadcastMessage(
                            ChatColor.RED + "💀 " + ChatColor.stripColor(customName) + 
                            " a été supprimé (correction population)"
                        );
                    }
                    
                    // Supprime l'entité
                    villager.remove();
                    break;
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[PopulationCorrection] Impossible de supprimer villageois du monde: " + e.getMessage());
        }
    }
    
    /**
     * Vérifie et affiche les statistiques de population
     */
    public static void displayPopulationStatistics() {
        Bukkit.getLogger().info("[PopulationCorrection] ===============================================");
        Bukkit.getLogger().info("[PopulationCorrection] Statistiques des populations:");
        
        Collection<VillageModel> allVillages = VillageRepository.getAll();
        int totalVillages = 0;
        int villagesWithExcess = 0;
        
        for (VillageModel village : allVillages) {
            totalVillages++;
            
            if (village.getBedsCount() != null && village.getBedsCount() > 0) {
                if (village.getPopulation() > village.getBedsCount()) {
                    villagesWithExcess++;
                    Bukkit.getLogger().warning("[PopulationCorrection] ⚠️ " + village.getId() + 
                        ": " + village.getPopulation() + "/" + village.getBedsCount() + " villageois");
                } else {
                    Bukkit.getLogger().info("[PopulationCorrection] ✅ " + village.getId() + 
                        ": " + village.getPopulation() + "/" + village.getBedsCount() + " villageois");
                }
            } else {
                Bukkit.getLogger().warning("[PopulationCorrection] ❌ " + village.getId() + ": Pas de lits");
            }
        }
        
        Bukkit.getLogger().info("[PopulationCorrection] Total villages: " + totalVillages);
        Bukkit.getLogger().info("[PopulationCorrection] Villages avec excès: " + villagesWithExcess);
        Bukkit.getLogger().info("[PopulationCorrection] ===============================================");
    }
}
