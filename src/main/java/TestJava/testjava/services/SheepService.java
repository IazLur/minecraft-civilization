package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.models.SheepModel;
import TestJava.testjava.repositories.SheepRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;

import java.util.Collection;

public class SheepService {

    /**
     * Crée et spawn un nouveau mouton pour une bergerie
     * CONDITION : Il faut au moins 1 employé par mouton
     */
    public static boolean spawnSheepForBuilding(BuildingModel building) {
        if (!building.getBuildingType().equals("bergerie") || !building.isActive()) {
            return false;
        }

        int currentSheepCount = SheepRepository.getSheepCountForBuilding(building.getId());
        int maxSheep = building.getLevel(); // Niveau 1 = 1 mouton max, etc.

        // NOUVELLE LOGIQUE : Vérifier qu'il y a au moins 1 employé par mouton
        int currentEmployees = CustomJobAssignmentService.countBuildingEmployees(building);
        int requiredEmployeesForNextSheep = currentSheepCount + 1; // On veut spawner 1 mouton de plus
        
        if (currentEmployees < requiredEmployeesForNextSheep) {
            Bukkit.getLogger().info("[SheepService] ❌ Pas assez d'employés pour spawner un mouton: " + 
                                   currentEmployees + " employés pour " + requiredEmployeesForNextSheep + " moutons requis");
            return false;
        }

        if (currentSheepCount >= maxSheep) {
            return false; // Limite atteinte
        }

        try {
            Location spawnLoc = new Location(TestJava.world, building.getX() + 1, building.getY() + 1, building.getZ() + 1);
            Sheep sheep = (Sheep) TestJava.world.spawnEntity(spawnLoc, EntityType.SHEEP);
            
            int sheepNumber = SheepRepository.getNextSheepNumberForVillage(building.getVillageName());
            String customName = formatSheepName(building, sheepNumber, true);
            sheep.setCustomName(customName);
            sheep.setCustomNameVisible(true);

            // Empêcher la reproduction
            sheep.setAgeLock(true);
            sheep.setAdult();

            // Sauvegarder en base
            SheepModel sheepModel = new SheepModel(
                sheep.getUniqueId(),
                building.getId(),
                building.getVillageName(),
                sheepNumber,
                spawnLoc.getBlockX(),
                spawnLoc.getBlockY(),
                spawnLoc.getBlockZ(),
                spawnLoc.getWorld().getName()
            );
            SheepRepository.update(sheepModel);

            Bukkit.getLogger().info("[SheepService] ✅ Mouton spawné pour bergerie " + building.getVillageName() + " (N°" + sheepNumber + ")");
            return true;

        } catch (Exception e) {
            Bukkit.getLogger().severe("[SheepService] ❌ Erreur lors du spawn de mouton: " + e.getMessage());
            return false;
        }
    }

    /**
     * Supprime tous les moutons d'une bergerie (entités + base de données)
     */
    public static void removeAllSheepForBuilding(BuildingModel building) {
        Collection<SheepModel> sheep = SheepRepository.getSheepForBuilding(building.getId());
        
        for (SheepModel sheepModel : sheep) {
            // Supprimer l'entité du monde
            if (TestJava.world != null) {
                TestJava.world.getEntities().stream()
                    .filter(entity -> entity.getUniqueId().equals(sheepModel.getId()))
                    .findFirst()
                    .ifPresent(entity -> {
                        entity.remove();
                        Bukkit.getLogger().info("[SheepService] ✅ Mouton supprimé: " + sheepModel.getVillageName() + " N°" + sheepModel.getSheepNumber());
                    });
            }
        }

        // Supprimer de la base de données
        SheepRepository.removeAllSheepForBuilding(building.getId());
    }

    /**
     * Met à jour le nom de tous les moutons d'une bergerie
     */
    public static void updateSheepNamesForBuilding(BuildingModel building) {
        Collection<SheepModel> sheep = SheepRepository.getSheepForBuilding(building.getId());
        
        for (SheepModel sheepModel : sheep) {
            if (TestJava.world != null) {
                TestJava.world.getEntities().stream()
                    .filter(entity -> entity.getUniqueId().equals(sheepModel.getId()))
                    .filter(entity -> entity instanceof Sheep)
                    .findFirst()
                    .ifPresent(entity -> {
                        String newName = formatSheepName(building, sheepModel.getSheepNumber(), building.isActive());
                        entity.setCustomName(newName);
                    });
            }
        }
    }

    /**
     * Formate le nom d'un mouton selon le statut de la bergerie
     */
    public static String formatSheepName(BuildingModel building, int sheepNumber, boolean isActive) {
        ChatColor statusColor = isActive ? ChatColor.GREEN : ChatColor.RED;
        String status = isActive ? "actif" : "inactif";
        
        return statusColor + "{" + status + "} " + 
               ChatColor.BLUE + "[" + building.getVillageName() + "] " + 
               ChatColor.WHITE + "Mouton N°" + sheepNumber;
    }

    /**
     * Déplace un mouton vers sa bergerie
     */
    public static void moveSheepToBuilding(SheepModel sheepModel, BuildingModel building) {
        if (TestJava.world == null) return;

        TestJava.world.getEntities().stream()
            .filter(entity -> entity.getUniqueId().equals(sheepModel.getId()))
            .filter(entity -> entity instanceof Sheep)
            .findFirst()
            .ifPresent(entity -> {
                Sheep sheep = (Sheep) entity;
                Location buildingLoc = new Location(TestJava.world, building.getX(), building.getY() + 1, building.getZ());
                Location sheepLoc = sheep.getLocation();
                
                // Si le mouton est trop loin (plus de 10 blocs), le téléporter
                if (sheepLoc.distance(buildingLoc) > 10) {
                    Location newLoc = buildingLoc.clone().add(
                        (Math.random() - 0.5) * 6, // X aléatoire dans un rayon de 3 blocs
                        0,
                        (Math.random() - 0.5) * 6  // Z aléatoire dans un rayon de 3 blocs
                    );
                    sheep.teleport(newLoc);
                    Bukkit.getLogger().info("[SheepService] 📍 Mouton téléporté vers bergerie: " + sheepModel.getVillageName() + " N°" + sheepModel.getSheepNumber());
                }
            });
    }

    /**
     * Supprime tous les moutons naturels (sans customName)
     */
    public static void removeNaturalSheep() {
        if (TestJava.world == null) return;

        TestJava.world.getEntitiesByClass(Sheep.class).stream()
            .filter(sheep -> sheep.getCustomName() == null || sheep.getCustomName().trim().isEmpty())
            .forEach(sheep -> {
                sheep.remove();
                Bukkit.getLogger().info("[SheepService] 🗑️ Mouton naturel supprimé à " + sheep.getLocation());
            });
    }
}