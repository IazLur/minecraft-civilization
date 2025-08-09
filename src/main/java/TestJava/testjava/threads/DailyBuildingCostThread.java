package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.BuildingRepository;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.services.CustomJobAssignmentService;
import TestJava.testjava.services.SheepService;
import TestJava.testjava.services.NativeJobLevelService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import java.util.Set;
import java.util.HashSet;
import org.bukkit.entity.Villager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
public class DailyBuildingCostThread implements Runnable {

    @Override
    public void run() {
        Collection<BuildingModel> allBuildings = BuildingRepository.getAll();
        
        // Statistiques par village pour le message personnalisé
        Map<String, VillageCostStats> villageStats = new HashMap<>();
        int totalBuildingsProcessed = 0;
        int totalCostPaid = 0;
        int buildingsDeactivated = 0;
        Set<String> villagesWithSchool = new HashSet<>();

        for (BuildingModel building : allBuildings) {
            VillageModel village = VillageRepository.get(building.getVillageName());
            // Enregistre les villages disposant d'une école active
            if ("ecole".equals(building.getBuildingType()) && building.isActive()) {
                villagesWithSchool.add(building.getVillageName());
            }

            if(village == null) {
                continue; // passez à la prochaine itération
            }

            EmpireModel empire = EmpireRepository.getForPlayer(village.getPlayerName());

            if(empire == null) {
                continue; // passez à la prochaine itération
            }

            // Si le bâtiment est inactif, pas de coût
            if (!building.isActive()) {
                continue;
            }

            totalBuildingsProcessed++;
            
            // Calculer le coût divisé par 5 (nouvelle fréquence : 4 min au lieu de 20 min)
            int adjustedCost = building.getCostPerDay() / 5;

            if (empire.getJuridictionCount() >= adjustedCost) {
                // Payer les coûts normalement
                empire.setJuridictionCount(empire.getJuridictionCount() - adjustedCost);
                EmpireRepository.update(empire);
                totalCostPaid += adjustedCost;
                
                // Statistiques pour le message personnalisé
                String villageName = village.getId();
                villageStats.putIfAbsent(villageName, new VillageCostStats());
                VillageCostStats stats = villageStats.get(villageName);
                stats.totalCost += adjustedCost;
                stats.buildingCount++;
                stats.ownerName = village.getPlayerName();
                
            } else {
                // Pas assez d'argent : désactiver le bâtiment au lieu de le détruire
                building.setActive(false);
                BuildingRepository.update(building);
                buildingsDeactivated++;

                // Gérer la désactivation du bâtiment custom
                if ("bergerie".equals(building.getBuildingType())) {
                    // Retirer tous les employés
                    CustomJobAssignmentService.adjustBuildingEmployees(building);
                    
                    // Tuer tous les moutons
                    SheepService.removeAllSheepForBuilding(building);
                    SheepService.updateSheepNamesForBuilding(building); // Mettre à jour les noms restants
                    
                    Bukkit.getServer().broadcastMessage(Colorize.name(building.getBuildingType()) + " de " + Colorize.name(village.getId()) +
                            " s'est désactivée par manque de fonds. Tous les employés et moutons ont été licenciés/supprimés.");
                } else {
                    // Pour les autres bâtiments custom, retirer les employés
                    CustomJobAssignmentService.adjustBuildingEmployees(building);
                    
                    Bukkit.getServer().broadcastMessage(Colorize.name(building.getBuildingType()) + " de " + Colorize.name(village.getId()) +
                            " s'est désactivée par manque de fonds. Tous les employés ont été licenciés.");
                }

                // Mettre à jour l'ArmorStand pour montrer le statut inactif
                Location loc = new Location(TestJava.world, building.getX(), building.getY() + 1, building.getZ());
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                    if (entity instanceof ArmorStand && entity.getLocation().distance(loc) < 1.0) {
                        ArmorStand armorStand = (ArmorStand) entity;
                        String newName = ChatColor.RED + "{inactif} " + 
                                        ChatColor.BLUE + "[" + village.getId() + "] " + 
                                        ChatColor.WHITE + building.getBuildingType();
                        armorStand.setCustomName(newName);
                    }
                }
            }
        }

        // === Traitement de l'éducation ===
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        for (VillagerModel villager : allVillagers) {
            if (!villagesWithSchool.contains(villager.getVillageName())) continue;
            if (villager.getSocialClassEnum().getLevel() < 1) continue;
            int currentEducation = villager.getEducation();
            if (currentEducation >= 8) continue;

            float currentRichesse = villager.getRichesse();
            float costEducation = currentEducation * 20f;
            if (currentRichesse >= costEducation) {
                villager.setRichesse(currentRichesse - costEducation);
                villager.setEducation(currentEducation + 1);
                VillagerRepository.update(villager);

                String displayName;
                Entity entity = TestJava.world != null ? TestJava.world.getEntity(villager.getId()) : null;
                if (entity instanceof Villager villagerEntity && villagerEntity.getCustomName() != null) {
                    displayName = ChatColor.stripColor(villagerEntity.getCustomName());
                } else {
                    displayName = "Un villageois";
                }
                Bukkit.getServer().broadcastMessage(displayName + " a gagné un niveau d'éducation (niveau " + villager.getEducation() + ")");
                
                // NOUVEAU : Si le villageois a un métier natif, mettre à jour son niveau selon l'éducation
                if (villager.hasNativeJob()) {
                    NativeJobLevelService.applyEducationToNativeJobLevel(villager);
                }
            }
        }
        
        // === Coût journalier des gardes squelettes ===
        Collection<VillageModel> allVillages = VillageRepository.getAll();
        for (VillageModel village : allVillages) {
            EmpireModel empire = EmpireRepository.getForPlayer(village.getPlayerName());
            if (empire == null) continue;
            int skeletonPaid = 0;
            for (Entity entity : TestJava.world.getEntities()) {
                if (entity instanceof org.bukkit.entity.Skeleton && entity.isCustomNameVisible()) {
                    String customName = entity.getCustomName();
                    if (customName != null && customName.contains(village.getId())) {
                        String[] nameParts = customName.replace(ChatColor.COLOR_CHAR + "", "").split(" ");
                        String prenom = nameParts.length > 0 ? nameParts[0] : "Garde";
                        String nom = nameParts.length > 1 ? nameParts[1] : "Squelette";
                        if (empire.getJuridictionCount() >= 1) {
                            empire.setJuridictionCount(empire.getJuridictionCount() - 1);
                            EmpireRepository.update(empire);
                            skeletonPaid++;
                        } else {
                            entity.remove();
                            Bukkit.getServer().broadcastMessage(
                                ChatColor.RED + prenom + " " + nom + ChatColor.GRAY + " sans salaire a déserté " +
                                ChatColor.YELLOW + village.getId()
                            );
                        }
                    }
                }
            }
            // Message au propriétaire si paiement effectué
            if (skeletonPaid > 0) {
                Player owner = Bukkit.getPlayer(village.getPlayerName());
                if (owner != null && owner.isOnline()) {
                    owner.sendMessage(ChatColor.AQUA + "Votre village a payé " + ChatColor.YELLOW + skeletonPaid + "µ" + ChatColor.AQUA + " aux gardes squelettes");
                }
            }
        }

        // Un seul log de résumé
        if (totalBuildingsProcessed > 0) {
            Bukkit.getLogger().info("[DailyBuildingCost] ✅ Résumé: " + totalBuildingsProcessed + " bâtiments traités, " + 
                                   totalCostPaid + "µ payés, " + buildingsDeactivated + " désactivés");
        }
        
        // Afficher les messages personnalisés aux propriétaires des villages
        for (VillageCostStats stats : villageStats.values()) {
            if (stats.totalCost > 0) {
                Player owner = Bukkit.getPlayer(stats.ownerName);
                if (owner != null && owner.isOnline()) {
                    owner.sendMessage(ChatColor.GREEN + "Votre village a payé " + 
                                    ChatColor.YELLOW + stats.totalCost + "µ" + 
                                    ChatColor.GREEN + " pour maintenir " + 
                                    ChatColor.YELLOW + stats.buildingCount + 
                                    ChatColor.GREEN + " bâtiments.");
                }
            }
        }
    }
    
    /**
     * Classe pour les statistiques de coûts par village
     */
    private static class VillageCostStats {
        public String ownerName;
        public int totalCost = 0;
        public int buildingCount = 0;
    }
}