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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
public class DailyBuildingCostThread implements Runnable {

    @Override
    public void run() {
        Collection<BuildingModel> allBuildings = BuildingRepository.getAll();
        
        // Statistiques par village pour le message personnalisé
        Map<String, VillageCostStats> villageStats = new HashMap<>();

        for (BuildingModel building : allBuildings) {
            VillageModel village = VillageRepository.get(building.getVillageName());

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

            // Calculer le coût divisé par 5 (nouvelle fréquence : 4 min au lieu de 20 min)
            int adjustedCost = building.getCostPerDay() / 5;

            if (empire.getJuridictionCount() >= adjustedCost) {
                // Payer les coûts normalement
                empire.setJuridictionCount(empire.getJuridictionCount() - adjustedCost);
                EmpireRepository.update(empire);
                
                // Statistiques pour le message personnalisé
                String villageName = village.getId();
                villageStats.putIfAbsent(villageName, new VillageCostStats());
                VillageCostStats stats = villageStats.get(villageName);
                stats.totalCost += adjustedCost;
                stats.buildingCount++;
                stats.villageName = villageName;
                stats.ownerName = village.getPlayerName();
                
            } else {
                // Pas assez d'argent : désactiver le bâtiment au lieu de le détruire
                building.setActive(false);
                BuildingRepository.update(building);

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
        public String villageName;
        public String ownerName;
        public int totalCost = 0;
        public int buildingCount = 0;
    }
}